package org.chromia.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.postchain.rell.api.base.PrinterRellCliEnv
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.gtx.RellApiRunTests
import net.postchain.rell.base.utils.UnitTestCaseResult
import java.nio.file.Files
import java.nio.file.Path

/**
 * In-process Rell test execution for the `run_rell_tests` tool - completes the
 * agent loop: rell_check (compiles) -> rell_security_check (secure) ->
 * run_rell_tests (behavior verified). Uses the same embedded runner the Chromia
 * CLI wraps. Tests that touch entities need PostgreSQL: set
 * CHROMIA_TEST_DATABASE_URL (jdbc:postgresql://...) - without it, database-less
 * tests still run and entity tests fail with a clear message.
 */
object RunRellTests {

    const val DATABASE_URL_ENV = "CHROMIA_TEST_DATABASE_URL"
    const val EXECUTION_TIMEOUT_SECONDS = 90L

    // Daemon threads so a runaway test (infinite loop) can never block JVM shutdown.
    private val runnerExecutor = java.util.concurrent.Executors.newCachedThreadPool { r ->
        Thread(r, "rell-test-runner").apply { isDaemon = true }
    }

    private val TEST_MODULE_REGEX = Regex("""(^|\n)\s*@test\s+module\b""")

    /** True when the (masked) source declares a `@test module`. */
    internal fun isTestModuleSource(content: String): Boolean =
        TEST_MODULE_REGEX.containsMatchIn(maskRellSource(content, maskStrings = true))

    /**
     * Rell module name for a source path: path separators become dots, and a file
     * named module.rell belongs to its DIRECTORY module (tests/module.rell -> tests),
     * per the Rell module rules. Root module.rell maps to "" (rejected by run()).
     */
    internal fun moduleNameForPath(path: String): String {
        val segments = path.replace('\\', '/').removeSuffix(".rell")
            .split('/')
            .filter { it.isNotEmpty() && it != "." }
        val effective = if (segments.isNotEmpty() && segments.last() == "module") segments.dropLast(1) else segments
        return effective.joinToString(".")
    }

    data class CaseResult(
        val name: String,
        val ok: Boolean,
        val error: String?,
        /** Failure is environmental (no PostgreSQL configured), not a logic failure. */
        val dbRequired: Boolean = false
    )

    private fun isDbRequiredError(error: String?): Boolean =
        error != null && (error.contains("No database connection") || error.contains("database features require a database URL"))

    data class Result(
        val ok: Boolean,
        val total: Int,
        val passed: Int,
        val failed: Int,
        val cases: List<CaseResult>,
        val notes: String
    )

    fun run(files: Map<String, String>, databaseUrl: String? = System.getenv(DATABASE_URL_ENV)): Result {
        require(files.isNotEmpty()) { "Provide a non-empty `files` map" }
        files.keys.forEach { relPath ->
            require(!relPath.contains("..") && !Path.of(relPath).isAbsolute) { "Path must be relative without '..': $relPath" }
            require(relPath.endsWith(".rell")) { "Only .rell files are supported: $relPath" }
        }

        // Detect @test on comment/string-masked source: `@test // note` + newline +
        // `module;` is a valid header, and "@test module" inside a comment or string
        // must not classify a file as a test module.
        val testModules = files
            .filterValues { isTestModuleSource(it) }
            .keys.map { moduleNameForPath(it) }
        require(testModules.isNotEmpty()) {
            "No @test modules found. Mark test files with `@test module;` and name test functions test_*."
        }
        require(testModules.none { it.isEmpty() }) {
            "A root-level module.rell test module has no name - put test files in a subdirectory (e.g. tests/module.rell) or name the file after the module."
        }

        val tempDir = Files.createTempDirectory("rell-tests")
        return try {
            files.forEach { (relPath, content) ->
                val target = tempDir.resolve(relPath).normalize()
                require(target.startsWith(tempDir)) { "Path escapes source root: $relPath" }
                Files.createDirectories(target.parent)
                Files.writeString(target, content)
            }
            // Vendored FT4 sources for `import lib.ft4.*` - see RellLibs. With the
            // lib present, app modules must be scoped to the user's own files.
            val appModules = if (RellLibs.needsFt4(files)) {
                RellLibs.provisionFt4(tempDir)
                RellLibs.userAppModules(files)
            } else {
                RellLibs.userAppModules(files).ifEmpty { null }
            }
            execute(tempDir, appModules, testModules, databaseUrl)
        } finally {
            runCatching { tempDir.toFile().deleteRecursively() }
        }
    }

    private fun execute(sourceDir: Path, appModules: List<String>?, testModules: List<String>, databaseUrl: String?): Result {
        // Capture compiler/runner messages so a test-compile failure reports
        // file/line diagnostics instead of a bare "Compilation failed".
        val messages = java.util.concurrent.CopyOnWriteArrayList<String>()
        val quietEnv = PrinterRellCliEnv({ messages.add(it) }, { messages.add(it) })
        // Written by the runner thread, read by the caller thread on timeout.
        val collected = java.util.concurrent.CopyOnWriteArrayList<UnitTestCaseResult>()
        val config = RellApiRunTests.Config.Builder()
            .compileConfig(
                RellApiCompile.Config.Builder()
                    .cliEnv(quietEnv)
                    .moduleArgsMissingError(false)
                    .build()
            )
            .cliEnv(quietEnv)
            .databaseUrl(databaseUrl)
            .printTestCases(false)
            .onTestCaseFinished { collected.add(it) }
            .build()

        // User test code executes in-process; bound it so an infinite loop in a
        // test returns a clear failure instead of hanging the tool call forever.
        val future = runnerExecutor.submit {
            RellApiRunTests.runTests(config, sourceDir.toFile(), appModules, testModules)
        }
        try {
            future.get(EXECUTION_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.TimeoutException) {
            future.cancel(true)
            val finished = collected.size
            return Result(
                ok = false,
                total = finished,
                passed = collected.count { it.res.error == null },
                failed = finished - collected.count { it.res.error == null },
                cases = collected.map { r ->
                    val error = r.res.error
                    CaseResult(r.case.name, error == null, error?.message)
                },
                notes = "Test execution exceeded ${EXECUTION_TIMEOUT_SECONDS}s and was abandoned - " +
                    "check for an infinite loop or unbounded work in a test. $finished case(s) finished before the timeout."
            )
        } catch (e: java.util.concurrent.ExecutionException) {
            val cause = e.cause
            if (cause is net.postchain.rell.api.base.RellCliException) {
                val diagnostics = messages.filter { it.isNotBlank() }.joinToString("\n")
                throw IllegalArgumentException(
                    "Rell test sources do not compile:\n$diagnostics".trimEnd()
                )
            }
            throw cause ?: e
        }

        val cases = collected.map { r ->
            val error = r.res.error
            val message = error?.message ?: error?.let { it::class.simpleName }
            CaseResult(r.case.name, error == null, message, dbRequired = isDbRequiredError(message))
        }
        val failed = cases.count { !it.ok }
        val dbLimited = cases.count { it.dbRequired }
        val notes = buildString {
            append("Ran ${cases.size} test(s) in ${testModules.size} test module(s): ${cases.size - failed} passed, $failed failed.")
            if (dbLimited > 0) {
                append(" $dbLimited failure(s) are environmental (dbRequired=true): the test touches entities/objects and needs PostgreSQL via $DATABASE_URL_ENV.")
            } else if (databaseUrl == null) {
                append(" No $DATABASE_URL_ENV set - tests touching entities/database fail without PostgreSQL; pure-logic tests are unaffected.")
            }
        }
        return Result(failed == 0 && cases.isNotEmpty(), cases.size, cases.size - failed, failed, cases, notes)
    }

    fun Result.toJson(): JsonObject = buildJsonObject {
        put("ok", ok)
        put("total", total)
        put("passed", passed)
        put("failed", failed)
        put(
            "cases",
            buildJsonArray {
                cases.forEach { c ->
                    add(
                        buildJsonObject {
                            put("name", c.name)
                            put("ok", c.ok)
                            c.error?.let { put("error", it) }
                            if (c.dbRequired) put("dbRequired", true)
                        }
                    )
                }
            }
        )
        put("notes", notes)
    }
}
