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

    private val TEST_MODULE_REGEX = Regex("""^\s*@test\s+module\b""", RegexOption.MULTILINE)

    data class CaseResult(val name: String, val ok: Boolean, val error: String?)

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

        val testModules = files.filterValues { TEST_MODULE_REGEX.containsMatchIn(it) }
            .keys.map { it.removeSuffix(".rell").replace('/', '.').replace('\\', '.') }
        require(testModules.isNotEmpty()) {
            "No @test modules found. Mark test files with `@test module;` and name test functions test_*."
        }

        val tempDir = Files.createTempDirectory("rell-tests")
        return try {
            files.forEach { (relPath, content) ->
                val target = tempDir.resolve(relPath).normalize()
                require(target.startsWith(tempDir)) { "Path escapes source root: $relPath" }
                Files.createDirectories(target.parent)
                Files.writeString(target, content)
            }
            execute(tempDir, testModules, databaseUrl)
        } finally {
            runCatching { tempDir.toFile().deleteRecursively() }
        }
    }

    private fun execute(sourceDir: Path, testModules: List<String>, databaseUrl: String?): Result {
        val quietEnv = PrinterRellCliEnv({ }, { })
        val collected = mutableListOf<UnitTestCaseResult>()
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

        RellApiRunTests.runTests(config, sourceDir.toFile(), null, testModules)

        val cases = collected.map { r ->
            val error = r.res.error
            CaseResult(r.case.name, error == null, error?.message ?: error?.let { it::class.simpleName })
        }
        val failed = cases.count { !it.ok }
        val notes = buildString {
            append("Ran ${cases.size} test(s) in ${testModules.size} test module(s): ${cases.size - failed} passed, $failed failed.")
            if (databaseUrl == null) {
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
                        }
                    )
                }
            }
        )
        put("notes", notes)
    }
}
