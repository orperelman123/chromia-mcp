package org.chromia.tools

import java.nio.file.Files
import java.nio.file.Path

/**
 * Vendored Rell libraries for in-process compilation. FT4 sources are the
 * pinned production release (see AGENTS.md pins), packed at build time from
 * gitlab.com/chromaway/ft4-lib tag v1.1.0r (`rell/src/lib/ft4`), so agents can
 * rell_check / run_rell_tests real FT4 dapp code without `chr install`.
 */
object RellLibs {
    const val FT4_VERSION = "v1.1.0r"
    private const val FT4_RESOURCE = "rell-libs/ft4-v1.1.0r.zip"
    private val FT4_IMPORT_REGEX = Regex("""\blib\.ft4\b""")

    fun needsFt4(files: Map<String, String>): Boolean =
        // Masked source: a `lib.ft4` mention in a comment or string is not an import.
        files.values.any { FT4_IMPORT_REGEX.containsMatchIn(maskRellSource(it, maskStrings = true)) }

    /**
     * Files the user submitted under lib/ft4/ (source-root-normalized paths).
     * When ANY are present the vendored zip must NOT be provisioned: it used to
     * truncate-overwrite the user's files, so an agent submitting its own
     * chr-installed FT4 got results computed against a silently substituted
     * mixed-version tree (audit F2). Compile with exactly what was sent instead
     * and say so via [submittedFt4Note].
     */
    /**
     * True for a submitted file that lives under the vendored-library root
     * lib/ft4/ (same ./ and src/ prefix normalization as RellCheck source
     * roots). Such files are FT4's own code, so the forbidden-import and
     * banned-module scanners must not report findings INSIDE them: FT4 v1.1.0r
     * itself contains `operation ras_open(` and `import lib.ft4.admin;`, so a
     * chr-installed tree submitted whole tripped CRITICALs against the library
     * (audit F2 follow-up). The user's app files stay fully scanned.
     */
    fun isSubmittedFt4Path(path: String): Boolean =
        path.trim().replace('\\', '/').removePrefix("./").removePrefix("src/").startsWith("lib/ft4/")

    fun submittedFt4FileCount(files: Map<String, String>): Int =
        files.keys.count { isSubmittedFt4Path(it) }

    fun submittedFt4Note(count: Int): String =
        "Using your submitted lib/ft4 sources ($count file(s)) instead of the vendored FT4 $FT4_VERSION."

    fun exemptedFt4Note(count: Int): String =
        "$count vendored-library file(s) under lib/ft4/ excluded from import/security scanning; " +
            "your app files are still fully scanned."

    /** Unpacks the vendored FT4 sources (entries under lib/ft4/...) into [root]. */
    fun provisionFt4(root: Path) {
        val stream = javaClass.classLoader.getResourceAsStream(FT4_RESOURCE)
            ?: error("Vendored FT4 sources missing from classpath: $FT4_RESOURCE")
        java.util.zip.ZipInputStream(stream).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = root.resolve(entry.name).normalize()
                require(target.startsWith(root)) { "Zip entry escapes extraction root: ${entry.name}" }
                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    Files.createDirectories(target.parent)
                    Files.newOutputStream(target).use { out -> zip.copyTo(out) }
                }
            }
        }
    }

    /**
     * App (non-test) module names for the user's files - passed explicitly to the
     * compiler so vendored library modules compile only when imported, instead of
     * `null` ("all modules") sweeping the whole vendored tree in.
     */
    fun userAppModules(files: Map<String, String>): List<String> =
        files.filterValues { !RunRellTests.isTestModuleSource(it) }
            .map { (path, content) -> RunRellTests.moduleNameForPath(path, content) }
            .filter { it.isNotEmpty() }
            .distinct()
}
