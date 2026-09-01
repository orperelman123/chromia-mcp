package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Combined read-only in-memory Chromia dapp project check.
 * Runs [ChromiaYmlValidator] on a chromia.yml string and [Ft4ImportCheck]
 * on one or more .rell file contents. Does not write to disk, run chr,
 * generate keys, or send signed transactions.
 */
object CheckDappProject {
    const val YAML_PATH = "chromia.yml"

    private val LINE_REF = Regex("""^line (\d+): (.*)$""")

    /** "line 3: msg" -> "main.rell:3: msg", matching the compile/security format. */
    private fun locate(label: String, finding: String): String =
        LINE_REF.matchEntire(finding)
            ?.let { "$label:${it.groupValues[1]}: ${it.groupValues[2]}" }
            ?: "$label: $finding"

    data class Result(
        val ok: Boolean,
        val errors: List<String>,
        val warnings: List<String>,
        val notes: List<String> = emptyList()
    ) {
        fun toJson() = buildJsonObject {
            put("ok", ok)
            put("errors", buildJsonArray { errors.forEach { add(JsonPrimitive(it)) } })
            put("warnings", buildJsonArray { warnings.forEach { add(JsonPrimitive(it)) } })
            put("notes", notes.joinToString(" "))
        }
    }

    /**
     * @param compile also compile the sources (rell_check) and run the security
     * pass, so `ok:true` means "this project actually builds and is not obviously
     * insecure" - not merely "the yml parses". Disable only for pure-text checks.
     */
    fun check(yaml: String, rellFiles: Map<String, String>, compile: Boolean = true): Result {
        val yml = ChromiaYmlValidator.validate(yaml)
        val errors = mutableListOf<String>()
        val warnings = yml.warnings.map { "$YAML_PATH: $it" }.toMutableList()
        val notes = mutableListOf<String>()
        yml.errors.forEach { errors += "$YAML_PATH: $it" }
        if (rellFiles.isEmpty()) {
            errors += "missing .rell file contents"
        }
        var exemptedLibFiles = 0
        rellFiles.forEach { (path, content) ->
            // FT4's own library files contain `operation ras_open(` and
            // `import lib.ft4.admin;` - scanning a submitted lib/ft4 tree
            // reported forbidden-module errors pointing INTO the library (audit
            // F2 follow-up). Skip vendored-library files; app files stay scanned.
            if (RellLibs.isSubmittedFt4Path(path)) {
                exemptedLibFiles++
                return@forEach
            }
            // Same normalized path and "<path>:<line>: ..." shape as the compile
            // and security findings below - FT4 findings used to say
            // "src/main.rell: line 3: ..." for the same file (audit 2026-09-01).
            val label = path.trim().removePrefix("./").removePrefix("src/").ifEmpty { "rell" }
            val one = Ft4ImportCheck.scan(content)
            one.errors.forEach { err -> errors += locate(label, err) }
            one.warnings.forEach { warn -> warnings += locate(label, warn) }
        }
        if (exemptedLibFiles > 0) {
            notes += RellLibs.exemptedFt4Note(exemptedLibFiles)
        }

        if (compile && rellFiles.isNotEmpty()) {
            // A project check that never compiles can report ok:true on code that
            // does not parse. Compile, then security-scan when it builds.
            val normalized = rellFiles.keys.filter { it.trim().endsWith(".rell") }
                .groupBy { it.trim().removePrefix("./").removePrefix("src/") }
            // src/main.rell and main.rell normalize to the same compile path - one
            // would silently clobber the other (audit 2026-09-01).
            val collisions = normalized.filterValues { it.size > 1 }
            if (collisions.isNotEmpty()) {
                errors += "rell: paths ${collisions.values.first()} resolve to the same file after " +
                    "normalization - rename one so both can be checked."
            }
            val compilable = normalized.mapValues { (_, paths) -> rellFiles.getValue(paths.first()) }
            if (compilable.isEmpty()) {
                // Skipping the gate silently reported ok=true on unaudited code
                // (audit 2026-09-01): rell input was supplied but nothing is
                // compilable, which must be an error, not a pass.
                errors += "rell: no compilable .rell files - key each source by a path ending in .rell " +
                    "(e.g. {\"main.rell\": \"module; ...\"}) so the compile and security checks can run."
            } else if (collisions.isEmpty()) {
                runCatching { RellCheck.check(compilable, null) }.fold(
                    onSuccess = { result ->
                        // Compile notes carry load-bearing context (e.g. "Using your
                        // submitted lib/ft4 sources ...") that was silently dropped
                        // (audit F1 follow-up).
                        if (result.notes.isNotBlank()) notes += result.notes
                        result.errors.forEach { d ->
                            val where = listOfNotNull(d.file, d.line?.toString()).joinToString(":")
                            errors += if (where.isEmpty()) "rell: ${d.text}" else "$where: ${d.text}"
                        }
                        result.warnings.forEach { d ->
                            val where = listOfNotNull(d.file, d.line?.toString()).joinToString(":")
                            warnings += if (where.isEmpty()) "rell: ${d.text}" else "$where: ${d.text}"
                        }
                        if (result.ok) {
                            val sec = RellSecurityCheck.analyze(compilable)
                            sec.findings.forEach { f ->
                                val line = "${f.file}:${f.line}: [${f.severity}] ${f.rule} - ${f.text}. Fix: ${f.fix}"
                                if (f.severity == "CRITICAL" || f.severity == "HIGH") errors += line else warnings += line
                            }
                        }
                    },
                    onFailure = { e -> errors += "rell: compile check failed: ${e.message}" }
                )
            }
        }
        return Result(ok = errors.isEmpty(), errors = errors, warnings = warnings, notes = notes)
    }
}
