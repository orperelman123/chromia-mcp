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

    // Cap on captured print()/log() output (chars). Without our own printers the
    // dependency default is Rt_OutPrinter = System.out.println, which corrupts
    // the JSON-RPC stream in --stdio mode - possibly mid-frame from the runner
    // thread (audit F1). Captured output is surfaced in the result instead.
    const val MAX_PRINT_CAPTURE_CHARS = 16_384

    // Refuse new runs once this many abandoned runner threads are still spinning:
    // each pins a core, and beyond a few the server is effectively degraded.
    internal const val MAX_LEAKED_RUNNERS = 4

    /** Thread-safe print()/log() sink, bounded so a print loop cannot grow the heap. */
    internal class BoundedPrinter(private val maxChars: Int) : net.postchain.rell.base.runtime.Rt_Printer {
        private val buffer = StringBuilder()
        var truncated = false
            private set

        @Synchronized
        override fun print(str: String) {
            if (truncated) return
            val remaining = maxChars - buffer.length
            if (str.length > remaining) {
                // Only now was output actually dropped; an exact fit used to be
                // reported as truncated too (audit round 4 minor).
                buffer.append(str, 0, remaining.coerceAtLeast(0))
                truncated = true
            } else {
                buffer.append(str)
                if (str.length < remaining) buffer.append('\n')
            }
        }

        @Synchronized
        fun text(): String = buffer.toString().trimEnd('\n')
    }

    // Runner threads abandoned by timed-out calls that have not yet terminated.
    // A tight loop in user test code does not poll interrupts and Thread.stop is
    // unsafe (the Rell API exposes no cancellation hook), so an abandoned runner
    // spins until its loop ends - each one pinning a core meanwhile. The count is
    // surfaced in the result notes; daemon threads never block JVM shutdown.
    internal val leakedRunners = java.util.concurrent.atomic.AtomicInteger()

    // Database-backed runs share one schema in CHROMIA_TEST_DATABASE_URL: two
    // concurrent runs see each other's chain tables and fail with "Missing
    // metadata entities for existing tables: c0.<entity>" (e2e finding
    // 2026-09-01). Serialize them; fair so queued calls run in arrival order.
    // Released only after the runner task truly ends - on timeout the release
    // is queued behind the runaway task on its own executor (see execute).
    internal val dbRunPermit = java.util.concurrent.Semaphore(1, true)

    /**
     * The caller thread was interrupted while the runner may still own the shared
     * test database and temp dir; both are released behind the runner (audit
     * round 4 F5). [run] must NOT delete the temp dir on this exception.
     */
    internal class RunAbandonedOnInterruptException(message: String) : InterruptedException(message)

    /** Test seam: replaces RellApiRunTests.runTests on the runner thread when set. */
    internal var runnerOverrideForTests: (() -> Unit)? = null

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
        val notes: String,
        /** print()/log() output captured from the tests (capped), "" when none. */
        val prints: String = ""
    )

    fun run(
        files: Map<String, String>,
        databaseUrl: String? = System.getenv(DATABASE_URL_ENV),
        /** module name -> module_args, e.g. {"lib.ft4.core.accounts": {"rate_limit": {...}}}. */
        moduleArgs: Map<String, Map<String, kotlinx.serialization.json.JsonElement>> = emptyMap(),
        timeoutSeconds: Long = EXECUTION_TIMEOUT_SECONDS
    ): Result {
        require(files.isNotEmpty()) { "Provide a non-empty `files` map" }
        RellCheck.requireTotalSizeWithinCap(files)
        files.keys.forEach { relPath ->
            require(!relPath.contains("..") && !Path.of(relPath).isAbsolute) { "Path must be relative without '..': $relPath" }
            require(relPath.endsWith(".rell")) { "Only .rell files are supported: $relPath" }
        }
        // Same case-insensitive clobbering hazard as rell_check (QA finding).
        val collisions = files.keys.groupBy { it.lowercase().replace('\\', '/') }.filterValues { it.size > 1 }
        require(collisions.isEmpty()) {
            "Case-insensitive path collision: ${collisions.values.first()} - most file systems treat these as the same file; rename one."
        }
        // Scaffold-shaped keys (src/test/main_test.rell) otherwise derive module
        // src.test.main_test, whose `import main;` fails with a misleading
        // "Module 'main' not found" (audit 2026-09-01). Paths are relative to the
        // Rell source root - normalize the ./ and src/ prefixes away, like rell_check.
        val sources = RellCheck.normalizeSourceRoots(files)

        // Detect @test on comment/string-masked source: `@test // note` + newline +
        // `module;` is a valid header, and "@test module" inside a comment or string
        // must not classify a file as a test module.
        val testModules = sources
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
            sources.forEach { (relPath, content) ->
                val target = tempDir.resolve(relPath).normalize()
                require(target.startsWith(tempDir)) { "Path escapes source root: $relPath" }
                Files.createDirectories(target.parent)
                Files.writeString(target, RellCheck.stripBom(content))
            }
            // Vendored FT4 sources for `import lib.ft4.*` - see RellLibs. With the
            // lib present, app modules must be scoped to the user's own files.
            // A header-less sibling of a @test module.rell resolves to the test
            // module's name - subtract so it is never passed as an app module.
            val appModules = if (RellLibs.needsFt4(sources)) {
                RellLibs.provisionFt4(tempDir)
                RellLibs.userAppModules(sources) - testModules.toSet()
            } else {
                (RellLibs.userAppModules(sources) - testModules.toSet()).ifEmpty { null }
            }
            val outcome = execute(tempDir, appModules, testModules, databaseUrl, moduleArgs, timeoutSeconds)
            cleanupDeferred = outcome.cleanupDeferred
            outcome.result
        } catch (e: RunAbandonedOnInterruptException) {
            cleanupDeferred = true // temp deletion is queued behind the abandoned runner
            throw e
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
        val leakedBefore = leakedRunners.get()
        if (leakedBefore >= MAX_LEAKED_RUNNERS) {
            throw IllegalStateException(
                "$leakedBefore abandoned test runner thread(s) from timed-out calls are still executing - " +
                    "each pins a core and cannot be stopped safely. The server needs a restart; " +
                    "fix the runaway loop in the test code before re-running."
            )
        }
        // Capture compiler/runner messages so a test-compile failure reports
        // file/line diagnostics instead of a bare "Compilation failed".
        val messages = java.util.concurrent.CopyOnWriteArrayList<String>()
        val quietEnv = PrinterRellCliEnv({ messages.add(it) }, { messages.add(it) })
        // Rell print()/log() must never reach System.out (the dependency default):
        // in --stdio mode that is the JSON-RPC stream (audit F1). Capture instead.
        val printer = BoundedPrinter(MAX_PRINT_CAPTURE_CHARS)
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
            .outPrinter(printer)
            .logPrinter(printer)
            .databaseUrl(databaseUrl)
            .printTestCases(false)
            .onTestCaseFinished { collected.add(it) }
            .build()

        // The shared test database admits one run at a time (see dbRunPermit).
        val usesDb = databaseUrl != null
        if (usesDb && !dbRunPermit.tryAcquire(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)) {
            throw IllegalStateException(
                "The test database ($DATABASE_URL_ENV) is still in use by another run_rell_tests call " +
                    "after ${timeoutSeconds}s (database-backed runs share one schema and are serialized). " +
                    "Retry shortly."
            )
        }
        // User test code executes in-process; bound it so an infinite loop in a
        // test returns a clear failure instead of hanging the tool call forever.
        val executor = newRunnerExecutor()
        val future = executor.submit {
            runnerOverrideForTests?.invoke()
                ?: RellApiRunTests.runTests(config, sourceDir.toFile(), appModules, testModules)
        }
        var runnerAbandoned = false
        try {
            future.get(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            // Interruption of the CALLER is not proof the runner stopped: releasing
            // the DB permit here resurrected the schema-collision e9c41ea fixed
            // (audit round 4 F5). Same deferred-release path as a timeout.
            runnerAbandoned = true
            future.cancel(true)
            leakedRunners.incrementAndGet()
            executor.submit {
                leakedRunners.decrementAndGet()
                runCatching { sourceDir.toFile().deleteRecursively() }
                if (usesDb) dbRunPermit.release()
            }
            executor.shutdown()
            Thread.currentThread().interrupt()
            throw RunAbandonedOnInterruptException(
                "run_rell_tests was interrupted; the runner may still be executing - " +
                    "its database permit and temp files are released when it finishes."
            )
        } catch (e: java.util.concurrent.TimeoutException) {
            runnerAbandoned = true
            future.cancel(true)
            leakedRunners.incrementAndGet()
            // Single-thread executor: this runs only after the runaway task ends
            // (if ever) - release the leak counter and delete the temp dir it may
            // still be reading. Daemon thread, so JVM shutdown is never blocked.
            executor.submit {
                leakedRunners.decrementAndGet()
                runCatching { sourceDir.toFile().deleteRecursively() }
                // The runaway may have kept using the database until now.
                if (usesDb) dbRunPermit.release()
            }
            executor.shutdown()
            // Snapshot once: the runner thread may still be appending, and mixing
            // live reads made total/passed/failed mutually inconsistent (audit).
            val snapshot = collected.toList()
            val finished = snapshot.size
            val passed = snapshot.count { it.res.error == null }
            val leaked = leakedRunners.get()
            return ExecuteOutcome(
                Result(
                    ok = false,
                    total = finished,
                    passed = passed,
                    failed = finished - passed,
                    cases = snapshot.map { r ->
                        val error = r.res.error
                        CaseResult(r.case.name, error == null, error?.message)
                    },
                    notes = "Test execution exceeded ${timeoutSeconds}s and was abandoned - " +
                        "check for an infinite loop or unbounded work in a test. $finished case(s) finished before the timeout." +
                        " $leaked abandoned runner thread(s) from timed-out calls may still be executing " +
                        "(daemon threads; they cannot be stopped safely and each pins a core until their loop ends)." +
                        printsNote(printer),
                    prints = printer.text()
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
            if (!runnerAbandoned) {
                executor.shutdownNow()
                if (usesDb) dbRunPermit.release()
            }
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
            if (cases.isEmpty()) {
                // ok=false with total=0 and no explanation left agents guessing
                // (audit 2026-09-01): the @test module was found, but nothing ran.
                append(" 0 test functions found - test functions must be named test_*.")
            }
            if (dbLimited > 0) {
                append(" $dbLimited failure(s) are environmental (dbRequired=true): the test touches entities/objects and needs PostgreSQL via $DATABASE_URL_ENV.")
            } else if (databaseUrl == null) {
                append(" No $DATABASE_URL_ENV set - tests touching entities/database fail without PostgreSQL; pure-logic tests are unaffected.")
            }
            val leaked = leakedRunners.get()
            if (leaked > 0) {
                append(" $leaked abandoned runner thread(s) from earlier timed-out calls are still executing.")
            }
            append(printsNote(printer))
        }
        return ExecuteOutcome(
            Result(failed == 0 && cases.isNotEmpty(), cases.size, cases.size - failed, failed, cases, notes, printer.text()),
            cleanupDeferred = false
        )
    }

    private fun printsNote(printer: BoundedPrinter): String = when {
        printer.truncated ->
            " Captured print()/log() output was truncated at $MAX_PRINT_CAPTURE_CHARS chars (see `prints`)."
        printer.text().isNotEmpty() -> " print()/log() output captured in `prints`."
        else -> ""
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
        if (prints.isNotEmpty()) put("prints", prints)
        put("notes", notes)
    }
}
