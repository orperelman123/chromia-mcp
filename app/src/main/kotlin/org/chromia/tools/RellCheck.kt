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

    /** "main.rell(3:9) ERROR: ..." / "lib/util.rell(1:1) WARNING: ..." */
    private val MESSAGE_REGEX = Regex("""^(.+?)\((\d+):(\d+)\)\s+(ERROR|WARNING)[:\s]\s*(.*)$""")

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
            if (RellLibs.needsFt4(sources)) RellLibs.provisionFt4(tempDir)
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
            compile(tempDir, effectiveModules, testModules)
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
     */
    internal fun normalizeSourceRoot(path: String): String =
        path.trim().removePrefix("./").removePrefix("src/")

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

    private fun validateFileMap(files: Map<String, String>) {
        require(files.isNotEmpty()) { "Provide `source` or a non-empty `files` map" }
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
            Diagnostic(file, line.toIntOrNull(), col.toIntOrNull(), severity, text, raw)
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
