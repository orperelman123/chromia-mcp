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

    // Runner threads abandoned by timed-out calls that have not yet terminated.
    // A tight loop in user test code does not poll interrupts and Thread.stop is
    // unsafe (the Rell API exposes no cancellation hook), so an abandoned runner
    // spins until its loop ends - each one pinning a core meanwhile. The count is
    // surfaced in the result notes; daemon threads never block JVM shutdown.
    private val leakedRunners = java.util.concurrent.atomic.AtomicInteger()

    private fun newRunnerExecutor() =
        // One dedicated thread per call: an unstoppable runaway runner must not
        // poison a shared pool, and queueing follow-up work on the same thread
        // guarantees it runs only after the runaway task finishes.
        java.util.concurrent.Executors.newSingleThreadExecutor { r ->
            Thread(r, "rell-test-runner").apply { isDaemon = true }
        }

    private val TEST_MODULE_REGEX = Regex("""(^|\n)\s*@test\s+module\b""")

    /** True when the (masked) source declares a `@test module`. */
    internal fun isTestModuleSource(content: String): Boolean =
        TEST_MODULE_REGEX.containsMatchIn(maskRellSource(content, maskStrings = true))

    /**
     * Rell module name for a source path: path separators become dots. A file
     * named module.rell - or any file whose CONTENT has no module header - belongs
     * to its DIRECTORY module (tests/module.rell -> tests, app/entities.rell
     * without a `module;` header -> app), per the compiler's
     * C_ModuleUtils.getModuleInfo (`tail == module.rell || ast.header == null`).
     * Deriving "app.entities" for a header-less file made the compiler throw
     * "Module 'app.entities' not found" - a false red on the recommended layout
     * (audit 2026-09-01). Root-level directory files map to "" (the root module).
     */
    internal fun moduleNameForPath(path: String, content: String? = null): String {
        val segments = path.replace('\\', '/').removeSuffix(".rell")
            .split('/')
            .filter { it.isNotEmpty() && it != "." }
        val directoryFile = segments.isNotEmpty() &&
            (segments.last() == "module" || (content != null && !hasModuleHeader(content)))
        val effective = if (directoryFile) segments.dropLast(1) else segments
        return effective.joinToString(".")
    }

    private val HEADER_KEYWORD_MODIFIERS = setOf("abstract", "mutable", "override")

    /**
     * True when the source starts (after comments/whitespace) with a module
     * header: `[modifiers] module ;` where a modifier is abstract/mutable/override
     * or an annotation such as @test or @mount('x') - mirroring the compiler
     * grammar (rootParser: optional(moduleHeader), moduleHeader: modifiers MODULE SEMI).
     */
    internal fun hasModuleHeader(content: String): Boolean {
        val masked = maskRellSource(content, maskStrings = true)
        var i = 0
        fun skipWs() { while (i < masked.length && masked[i].isWhitespace()) i++ }
        fun readWord(): String {
            val start = i
            while (i < masked.length && (masked[i].isLetterOrDigit() || masked[i] == '_')) i++
            return masked.substring(start, i)
        }
        skipWs()
        while (i < masked.length) {
            when {
                masked[i] == '@' -> {
                    i++
                    if (readWord().isEmpty()) return false
                    skipWs()
                    if (i < masked.length && masked[i] == '(') {
                        var depth = 0
                        while (i < masked.length) {
                            when (masked[i]) { '(' -> depth++; ')' -> depth-- }
                            i++
                            if (depth == 0) break
                        }
                        if (depth != 0) return false
                    }
                }
                masked[i].isLetter() || masked[i] == '_' -> {
                    val word = readWord()
                    if (word == "module") {
                        skipWs()
                        return i < masked.length && masked[i] == ';'
                    }
                    if (word !in HEADER_KEYWORD_MODIFIERS) return false
                }
                else -> return false
            }
            skipWs()
        }
        return false
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

    fun run(
        files: Map<String, String>,
        databaseUrl: String? = System.getenv(DATABASE_URL_ENV),
        /** module name -> module_args, e.g. {"lib.ft4.core.accounts": {"rate_limit": {...}}}. */
        moduleArgs: Map<String, Map<String, kotlinx.serialization.json.JsonElement>> = emptyMap(),
        timeoutSeconds: Long = EXECUTION_TIMEOUT_SECONDS
    ): Result {
        require(files.isNotEmpty()) { "Provide a non-empty `files` map" }
        files.keys.forEach { relPath ->
            require(!relPath.contains("..") && !Path.of(relPath).isAbsolute) { "Path must be relative without '..': $relPath" }
            require(relPath.endsWith(".rell")) { "Only .rell files are supported: $relPath" }
        }
        // Same case-insensitive clobbering hazard as rell_check (QA finding).
        val collisions = files.keys.groupBy { it.lowercase().replace('\\', '/') }.filterValues { it.size > 1 }
        require(collisions.isEmpty()) {
            "Case-insensitive path collision: ${collisions.values.first()} - most file systems treat these as the same file; rename one."
        }

        // Detect @test on comment/string-masked source: `@test // note` + newline +
        // `module;` is a valid header, and "@test module" inside a comment or string
        // must not classify a file as a test module.
        val testModules = files
            .filterValues { isTestModuleSource(it) }
            .map { (path, content) -> moduleNameForPath(path, content) }
            .distinct()
        require(testModules.isNotEmpty()) {
            "No @test modules found. Mark test files with `@test module;` and name test functions test_*."
        }
        require(testModules.none { it.isEmpty() }) {
            "A root-level module.rell test module has no name - put test files in a subdirectory (e.g. tests/module.rell) or name the file after the module."
        }

        val tempDir = Files.createTempDirectory("rell-tests")
        var cleanupDeferred = false
        return try {
            files.forEach { (relPath, content) ->
                val target = tempDir.resolve(relPath).normalize()
                require(target.startsWith(tempDir)) { "Path escapes source root: $relPath" }
                Files.createDirectories(target.parent)
                Files.writeString(target, RellCheck.stripBom(content))
            }
            // Vendored FT4 sources for `import lib.ft4.*` - see RellLibs. With the
            // lib present, app modules must be scoped to the user's own files.
            // A header-less sibling of a @test module.rell resolves to the test
            // module's name - subtract so it is never passed as an app module.
            val appModules = if (RellLibs.needsFt4(files)) {
                RellLibs.provisionFt4(tempDir)
                RellLibs.userAppModules(files) - testModules.toSet()
            } else {
                (RellLibs.userAppModules(files) - testModules.toSet()).ifEmpty { null }
            }
            val outcome = execute(tempDir, appModules, testModules, databaseUrl, moduleArgs, timeoutSeconds)
            cleanupDeferred = outcome.cleanupDeferred
            outcome.result
        } finally {
            // On timeout the abandoned runner may still be reading the temp dir;
            // its deletion is queued behind the runaway task instead (see execute).
            if (!cleanupDeferred) runCatching { tempDir.toFile().deleteRecursively() }
        }
    }

    private data class ExecuteOutcome(val result: Result, val cleanupDeferred: Boolean)

    /** Converts JSON module args to the Gtv map the Rell compiler expects. */
    private fun toGtvArgs(
        moduleArgs: Map<String, Map<String, kotlinx.serialization.json.JsonElement>>
    ): Map<String, Map<String, net.postchain.gtv.Gtv>> =
        moduleArgs.mapValues { (_, args) -> args.mapValues { (_, v) -> jsonToGtv(v) } }

    private fun jsonToGtv(element: kotlinx.serialization.json.JsonElement): net.postchain.gtv.Gtv = when (element) {
        is kotlinx.serialization.json.JsonNull -> net.postchain.gtv.GtvNull
        is kotlinx.serialization.json.JsonPrimitive ->
            when {
                element.isString -> net.postchain.gtv.GtvFactory.gtv(element.content)
                element.content == "true" || element.content == "false" ->
                    net.postchain.gtv.GtvFactory.gtv(element.content.toBoolean())
                else -> element.content.toLongOrNull()?.let { net.postchain.gtv.GtvFactory.gtv(it) }
                    ?: net.postchain.gtv.GtvFactory.gtv(element.content)
            }
        is kotlinx.serialization.json.JsonArray -> net.postchain.gtv.GtvFactory.gtv(element.map { jsonToGtv(it) })
        is kotlinx.serialization.json.JsonObject ->
            net.postchain.gtv.GtvFactory.gtv(element.mapValues { (_, v) -> jsonToGtv(v) })
    }

    private fun execute(
        sourceDir: Path,
        appModules: List<String>?,
        testModules: List<String>,
        databaseUrl: String?,
        moduleArgs: Map<String, Map<String, kotlinx.serialization.json.JsonElement>> = emptyMap(),
        timeoutSeconds: Long = EXECUTION_TIMEOUT_SECONDS
    ): ExecuteOutcome {
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
                    // Real FT4 tests need module_args (lib.ft4.core.accounts etc.);
                    // without them an authenticated operation cannot be exercised.
                    .apply { if (moduleArgs.isNotEmpty()) moduleArgs(toGtvArgs(moduleArgs)) }
                    .build()
            )
            .cliEnv(quietEnv)
            .databaseUrl(databaseUrl)
            .printTestCases(false)
            .onTestCaseFinished { collected.add(it) }
            .build()

        // User test code executes in-process; bound it so an infinite loop in a
        // test returns a clear failure instead of hanging the tool call forever.
        val executor = newRunnerExecutor()
        val future = executor.submit {
            RellApiRunTests.runTests(config, sourceDir.toFile(), appModules, testModules)
        }
        var timedOut = false
        try {
            future.get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.TimeoutException) {
            timedOut = true
            future.cancel(true)
            leakedRunners.incrementAndGet()
            // Single-thread executor: this runs only after the runaway task ends
            // (if ever) - release the leak counter and delete the temp dir it may
            // still be reading. Daemon thread, so JVM shutdown is never blocked.
            executor.submit {
                leakedRunners.decrementAndGet()
                runCatching { sourceDir.toFile().deleteRecursively() }
            }
            executor.shutdown()
            val finished = collected.size
            val leaked = leakedRunners.get()
            return ExecuteOutcome(
                Result(
                    ok = false,
                    total = finished,
                    passed = collected.count { it.res.error == null },
                    failed = finished - collected.count { it.res.error == null },
                    cases = collected.map { r ->
                        val error = r.res.error
                        CaseResult(r.case.name, error == null, error?.message)
                    },
                    notes = "Test execution exceeded ${timeoutSeconds}s and was abandoned - " +
                        "check for an infinite loop or unbounded work in a test. $finished case(s) finished before the timeout." +
                        " $leaked abandoned runner thread(s) from timed-out calls may still be executing " +
                        "(daemon threads; they cannot be stopped safely and each pins a core until their loop ends)."
                ),
                cleanupDeferred = true
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
        } finally {
            if (!timedOut) executor.shutdownNow()
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
            val leaked = leakedRunners.get()
            if (leaked > 0) {
                append(" $leaked abandoned runner thread(s) from earlier timed-out calls are still executing.")
            }
        }
        return ExecuteOutcome(
            Result(failed == 0 && cases.isNotEmpty(), cases.size, cases.size - failed, failed, cases, notes),
            cleanupDeferred = false
        )
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
