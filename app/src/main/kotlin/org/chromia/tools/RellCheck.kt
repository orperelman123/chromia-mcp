package org.chromia.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.postchain.rell.api.base.PrinterRellCliEnv
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliException
import java.nio.file.Files
import java.nio.file.Path

/**
 * In-process Rell compilation for the `rell_check` tool: write the given
 * sources to a temp dir, compile with the same compiler the Chromia CLI
 * embeds, and return structured diagnostics (file/line/column/text) so an
 * agent can iterate write -> compile -> fix without installing `chr`.
 */
object RellCheck {

    /**
     * "main.rell(3:9) ERROR: ..." / "lib/util.rell(1:1) Warning: ...".
     * The compiler prints "Warning:" in mixed case - matching only WARNING left
     * every warning unparsed (file=null), so they lost their file/line structure
     * and dodged the vendored-FT4 suppression (ft4-demo, real-world round 1).
     */
    private val MESSAGE_REGEX =
        Regex("""^(.+?)\((\d+):(\d+)\)\s+(ERROR|WARNING)[:\s]\s*(.*)$""", RegexOption.IGNORE_CASE)

    data class Diagnostic(
        val file: String?,
        val line: Int?,
        val column: Int?,
        val severity: String,
        val text: String,
        val raw: String
    )

    data class Result(
        val ok: Boolean,
        val modules: List<String>,
        val errors: List<Diagnostic>,
        val warnings: List<Diagnostic>,
        val notes: String
    )

    fun check(files: Map<String, String>, modules: List<String>?): Result {
        validateFileMap(files)
        // Scaffold-shaped keys (src/main.rell) otherwise become modules named
        // src.main, and the compiler answers "Module 'main' not found" - steering
        // agents to the wrong fix (audit 2026-09-01). Paths are relative to the
        // Rell source root, so a leading ./ or src/ project prefix is dropped.
        val sources = normalizeSourceRoots(files)
        val tempDir = Files.createTempDirectory("rell-check")
        return try {
            sources.forEach { (relPath, content) ->
                val target = tempDir.resolve(relPath).normalize()
                require(target.startsWith(tempDir)) { "Path escapes source root: $relPath" }
                Files.createDirectories(target.parent)
                Files.writeString(target, stripBom(content))
            }
            // Vendored FT4 sources let `import lib.ft4.*` compile without chr install.
            // With a vendored lib present the module list must be explicit (the user's
            // app modules) so library modules compile only when imported.
            // CRITICAL (audit 2026-08-31): an EMPTY module list means "compile
            // nothing" to the compiler, which reports ok=true with zero modules - a
            // false green on broken code. A root module.rell resolves to the empty
            // module name and is filtered out, so the FT4 branch hit exactly that.
            // Fall back to null (= all modules) whenever the scoped list is empty.
            // ...unless the user submitted their OWN lib/ft4 tree: provisioning
            // used to truncate-overwrite those files, silently substituting a
            // mixed-version tree (audit F2). Compile exactly what was sent and
            // surface a note so errors are correctly attributed.
            val submittedFt4 = RellLibs.submittedFt4FileCount(sources)
            val provisionedFt4 = submittedFt4 == 0 && RellLibs.needsFt4(sources)
            if (provisionedFt4) RellLibs.provisionFt4(tempDir)
            // Test modules are not app modules: without passing them explicitly a
            // project of only @test files compiled nothing and reported ok=true.
            val testModules = sources.filterValues { RunRellTests.isTestModuleSource(it) }
                .map { (path, content) -> RunRellTests.moduleNameForPath(path, content) }
                .filter { it.isNotEmpty() }
                .distinct()
            // A header-less sibling of a @test module.rell resolves to the test
            // module's name - subtract so it is never passed as an app module.
            val effectiveModules = modules
                ?: (RellLibs.userAppModules(sources) - testModules.toSet()).ifEmpty { null }
            val result = compile(tempDir, effectiveModules, testModules)
            val annotated = if (submittedFt4 > 0) {
                result.copy(notes = result.notes + " " + RellLibs.submittedFt4Note(submittedFt4))
            } else {
                // Warnings INSIDE the provisioned FT4 library are pinned-library
                // noise the agent cannot act on: a one-line FT4 dapp came back
                // with ~30 lib/ft4 nullability warnings drowning its own output
                // (ft4-demo, real-world round 1). Errors in lib/ft4 still
                // surface - they indicate real problems (e.g. a missing
                // dependency the library needs).
                suppressVendoredFt4Warnings(result)
            }
            appendFt4VersionMismatchHint(annotated, ft4Involved = provisionedFt4 || submittedFt4 > 0)
        } finally {
            runCatching { tempDir.toFile().deleteRecursively() }
        }
    }

    /**
     * Windows editors (Notepad, some VS Code configs) prefix files with U+FEFF.
     * The Rell lexer rejects it as an unexpected token, so a perfectly valid file
     * fails with a cryptic syntax error (QA finding). Strip it before compiling.
     */
    internal fun stripBom(content: String): String = content.removePrefix("﻿")

    /**
     * Drops a leading ./ and src/ project prefix - file paths are relative to
     * the Rell SOURCE ROOT, but agents feed scaffold_dapp output (keyed
     * src/main.rell) verbatim, deriving module src.main and a misleading
     * "Module 'main' not found" (audit 2026-09-01). Same normalization as
     * [CheckDappProject]. Two inputs collapsing to one path is an error, like
     * the case-insensitive collision check.
     *
     * Backslash separators are normalized to / first: a Windows-style key like
     * src\main.rell used to dodge the prefix stripping and, on Linux, produced
     * a literal file named 'src\main.rell' while the module name derived to
     * src.main - a platform-dependent phantom module (audit F4).
     */
    internal fun normalizeSourceRoot(path: String): String =
        path.trim().replace('\\', '/').removePrefix("./").removePrefix("src/")

    internal fun normalizeSourceRoots(files: Map<String, String>): LinkedHashMap<String, String> {
        val collisions = files.keys
            .groupBy { normalizeSourceRoot(it).lowercase().replace('\\', '/') }
            .filterValues { it.size > 1 }
        require(collisions.isEmpty()) {
            "Paths ${collisions.values.first()} resolve to the same file after the src/ prefix is " +
                "normalized away (paths are relative to the Rell source root) - rename one so both can be used."
        }
        val out = linkedMapOf<String, String>()
        files.forEach { (path, content) -> out[normalizeSourceRoot(path)] = content }
        return out
    }

    /**
     * Total-input ceiling for the three Rell tools (rell_check,
     * rell_security_check via its compile gate, run_rell_tests): unbounded
     * submissions have no legitimate use through an MCP tool call and feed
     * quadratic scanners downstream (audit F3).
     */
    internal const val MAX_TOTAL_SOURCE_CHARS = 2 * 1024 * 1024

    internal fun requireTotalSizeWithinCap(files: Map<String, String>) {
        var total = 0L
        files.values.forEach { total += it.length }
        require(total <= MAX_TOTAL_SOURCE_CHARS) {
            "Total Rell source size ($total chars across ${files.size} file(s)) exceeds the " +
                "$MAX_TOTAL_SOURCE_CHARS-char (~2 MB) limit - submit a smaller project."
        }
    }

    private fun validateFileMap(files: Map<String, String>) {
        require(files.isNotEmpty()) { "Provide `source` or a non-empty `files` map" }
        requireTotalSizeWithinCap(files)
        files.keys.forEach { relPath ->
            require(relPath.isNotBlank()) { "Empty file path" }
            require(!relPath.contains("..")) { "Path must not contain '..': $relPath" }
            require(!Path.of(relPath).isAbsolute) { "Path must be relative: $relPath" }
            require(relPath.endsWith(".rell")) { "Only .rell files are compiled: $relPath" }
        }
        // Case-insensitive filesystems (Windows, default macOS) map a.rell and A.rell
        // to ONE file - the second write silently clobbers the first and produces a
        // misleading "module not found" (QA finding). Reject the collision explicitly.
        val collisions = files.keys.groupBy { it.lowercase().replace('\\', '/') }.filterValues { it.size > 1 }
        require(collisions.isEmpty()) {
            "Case-insensitive path collision: ${collisions.values.first()} - most file systems treat these as the same file; rename one."
        }
    }

    /** "Module 'lib.ft4.<something>' not found" - the FT4-version-mismatch signature. */
    private val FT4_MODULE_NOT_FOUND_REGEX = Regex("""Module '(lib\.ft4[^']*)' not found""")

    /**
     * When FT4 was involved (vendored tree provisioned, or the user submitted
     * their own lib/ft4) and an error says a lib.ft4 module does not exist, the
     * real cause is usually a version mismatch, not a typo: the server vendors
     * FT4 [RellLibs.FT4_VERSION], and the user's project may pin an older or
     * newer FT4 whose module set differs (real case: lib.ft4.test.utils existed
     * before 1.1.0r and is gone in it). Without the hint the agent chases a
     * nonexistent import bug in its own code.
     */
    internal fun appendFt4VersionMismatchHint(result: Result, ft4Involved: Boolean): Result {
        if (!ft4Involved) return result
        val missing = result.errors.firstNotNullOfOrNull { FT4_MODULE_NOT_FOUND_REGEX.find(it.text) }
            ?: return result
        return result.copy(
            notes = result.notes +
                " Note: this server vendors FT4 ${RellLibs.FT4_VERSION}; '${missing.groupValues[1]}' not being" +
                " found can mean your project pins an older or newer FT4 whose module set differs" +
                " (e.g. lib.ft4.test.utils existed before 1.1.0r). Check your FT4 tagOrBranch pin."
        )
    }

    /** Drops warnings located in provisioned lib/ft4 files; says so in notes. */
    internal fun suppressVendoredFt4Warnings(result: Result): Result {
        val (vendored, user) = result.warnings.partition {
            it.file?.replace('\\', '/')?.startsWith("lib/ft4/") == true
        }
        if (vendored.isEmpty()) return result
        return result.copy(
            warnings = user,
            notes = result.notes +
                " ${vendored.size} compiler warning(s) inside vendored FT4 ${RellLibs.FT4_VERSION} suppressed."
        )
    }

    private fun compile(sourceDir: Path, modules: List<String>?, testModules: List<String> = emptyList()): Result {
        val captured = mutableListOf<String>()
        val cliEnv = PrinterRellCliEnv({ captured.add(it) }, { captured.add(it) })
        val config = RellApiCompile.Config.Builder()
            .cliEnv(cliEnv)
            .quiet(false)
            // Agents check sources without chromia.yml module args; missing args
            // must not fail the compile.
            .moduleArgsMissingError(false)
            .build()

        val compiledModules = mutableListOf<String>()
        val failure = runCatching {
            val app = RellApiCompile.compileApp(config, sourceDir.toFile(), modules, testModules)
            app.modules
                .filter { !it.test && !it.abstract && !it.external }
                .forEach { compiledModules.add(it.name.toString()) }
        }.exceptionOrNull()

        if (failure != null && failure !is RellCliException && failure !is IllegalArgumentException) {
            throw failure
        }

        val diagnostics = captured
            .filter { it.isNotBlank() && !it.startsWith("Errors:") }
            .map { parseMessage(it) }
        // A compiler exception with no parsed ERROR line still means failure.
        val errors = diagnostics.filter { it.severity == "ERROR" }.toMutableList()
        if (failure != null && errors.isEmpty()) {
            errors.add(Diagnostic(null, null, null, "ERROR", failure.message ?: "Compilation failed", failure.message ?: ""))
        }
        val warnings = diagnostics.filter { it.severity == "WARNING" }

        val ok = failure == null
        val notes = if (ok) {
            "Compiled ${compiledModules.size} module(s) successfully with Rell ${rellVersion()}."
        } else {
            "Compilation failed with ${errors.size} error(s). Fix the first error and re-run; later errors often cascade."
        }
        return Result(ok, compiledModules, errors, warnings, notes)
    }

    private fun parseMessage(raw: String): Diagnostic {
        val match = MESSAGE_REGEX.find(raw.trim())
        return if (match != null) {
            val (file, line, col, severity, text) = match.destructured
            Diagnostic(file, line.toIntOrNull(), col.toIntOrNull(), severity.uppercase(), text, raw)
        } else {
            val severity = if (raw.contains("ERROR", ignoreCase = true)) "ERROR" else "WARNING"
            Diagnostic(null, null, null, severity, raw, raw)
        }
    }

    private fun rellVersion(): String = runCatching {
        net.postchain.rell.base.utils.RellVersions.VERSION.toString()
    }.getOrDefault("unknown")

    fun Result.toJson(): JsonObject = buildJsonObject {
        put("ok", ok)
        put("modules", buildJsonArray { modules.forEach { add(JsonPrimitive(it)) } })
        put("errors", diagnosticsJson(errors))
        put("warnings", diagnosticsJson(warnings))
        put("notes", notes)
    }

    private fun diagnosticsJson(items: List<Diagnostic>) = buildJsonArray {
        items.forEach { d ->
            add(
                buildJsonObject {
                    d.file?.let { put("file", it) }
                    d.line?.let { put("line", it) }
                    d.column?.let { put("column", it) }
                    put("severity", d.severity)
                    put("text", d.text)
                }
            )
        }
    }
}
