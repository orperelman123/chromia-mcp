package org.chromia.tools

import java.nio.file.Files
import java.nio.file.Path

/**
 * Vendored Rell libraries for in-process compilation. FT4 sources are the
 * pinned production release (see AGENTS.md pins), packed from
 * gitlab.com/chromaway/ft4-lib tag v1.1.0r (`rell/src/lib`): `ft4` plus its
 * sibling libraries `iccf` and `iccf_test` (FT4's crosschain/test-helper
 * modules import lib.iccf, which `chr` resolves via chromia.yml libs - without
 * it, any dapp touching lib.ft4.test.core failed with "Module 'lib.iccf' not
 * found"; real-world round 2 D1). So agents can rell_check / run_rell_tests
 * real FT4 dapp code without `chr install`.
 */
object RellLibs {
    const val FT4_VERSION = "v1.1.0r"
    private const val FT4_RESOURCE = "rell-libs/ft4-v1.1.0r.zip"
    private val FT4_IMPORT_REGEX = Regex("""\blib\.(ft4|iccf)\b""")

    /** Top-level lib/ directories the vendored zip provides. */
    val VENDORED_LIB_ROOTS = setOf("ft4", "iccf", "iccf_test")

    fun needsFt4(files: Map<String, String>): Boolean =
        // Masked source: a `lib.ft4` mention in a comment or string is not an import.
        files.values.any { FT4_IMPORT_REGEX.containsMatchIn(maskRellSource(it, maskStrings = true)) }

    /**
     * Files the user submitted under lib/ft4/ (source-root-normalized paths).
     * When ANY are present the vendored zip must NOT be provisioned: it used to
     * truncate-overwrite the user's files, so an agent submitting its own
     * chr-installed FT4 got results computed against a silently substituted
     * mixed-version tree (audit F2). Compile with exactly what was sent instead
     * and say so via [submittedVendoredNote].
     */
    /**
     * True for a submitted file that lives under the vendored-library root
     * lib/ft4/ (same ./ and src/ prefix normalization as RellCheck source
     * roots). Such files are FT4's own code, so the forbidden-import and
     * banned-module scanners must not report findings INSIDE them: FT4 v1.1.0r
     * itself contains `operation ras_open(` and `import lib.ft4.admin;`, so a
     * chr-installed tree submitted whole tripped CRITICALs against the library
     * (audit F2 follow-up). The user's app files stay fully scanned.
     *
     * The path alone is NOT enough to skip scanning - the scanners also require
     * [matchesVendoredFt4]: a lib/ft4 file whose content differs from the
     * vendored copy could be a foreign fork, a patched library, or planted code
     * hiding under the exempt path, so it is scanned like app code (security
     * enhancement, agent-experience round). Provisioning-skip decisions still
     * use the path alone: whatever the user submitted under lib/ft4/ is the
     * compile truth, matching or not.
     */
    fun isSubmittedFt4Path(path: String): Boolean =
        normalizeFt4Path(path).startsWith("lib/ft4/")

    /**
     * True for a submitted file under any lib/ root the server vendors
     * (lib/ft4, lib/iccf, lib/iccf_test). Such paths get the hash-gated
     * scanning exemption: identical-to-vendored files are library code the
     * user does not own; differing ones are scanned like app code.
     */
    fun isVendoredLibraryPath(path: String): Boolean {
        val normalized = normalizeFt4Path(path)
        if (!normalized.startsWith("lib/")) return false
        val root = normalized.removePrefix("lib/").substringBefore('/')
        return root in VENDORED_LIB_ROOTS && normalized.length > "lib/$root/".length
    }

    /**
     * True for a submitted lib/ file with NO vendored counterpart root
     * (lib/ft3, lib/icmf, ...). There is nothing to hash-compare against, so
     * scanners skip these as third-party library code the user does not own -
     * chromunity's vendored lib/ft3 produced findings inside library code
     * (real-world round 2 D5). The note says how to opt back in.
     */
    fun isThirdPartyLibPath(path: String): Boolean =
        isVendoredLibPath(path) && !isVendoredLibraryPath(path)

    fun thirdPartyLibNote(count: Int): String =
        "$count file(s) under lib/ skipped as third-party library code (no vendored copy to " +
            "verify against); submit them outside lib/ to have them scanned."

    private fun normalizeFt4Path(path: String): String =
        path.trim().replace('\\', '/').removePrefix("./").removePrefix("src/")

    /**
     * The vendored FT4 sources by zip-entry path (lib/ft4/...), line endings
     * normalized to LF. ~0.5 MB, loaded once on first content comparison.
     */
    private val vendoredFt4Contents: Map<String, String> by lazy {
        val stream = javaClass.classLoader.getResourceAsStream(FT4_RESOURCE)
            ?: error("Vendored FT4 sources missing from classpath: $FT4_RESOURCE")
        buildMap {
            java.util.zip.ZipInputStream(stream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    put(
                        entry.name.replace('\\', '/'),
                        normalizeLineEndings(zip.readBytes().toString(Charsets.UTF_8))
                    )
                }
            }
        }
    }

    /** CRLF/CR vs LF must never defeat a content match. */
    private fun normalizeLineEndings(s: String): String =
        s.replace("\r\n", "\n").replace('\r', '\n')

    /** Exposed for tests that need a genuine vendored tree to submit. */
    internal fun vendoredFt4Files(): Map<String, String> = vendoredFt4Contents

    /**
     * True when the submitted lib/ft4 file is identical (modulo line endings)
     * to the vendored FT4 [FT4_VERSION] file at the same source-root-relative
     * path. Only such files earn the scanning exemption; a path with no
     * vendored counterpart, or with different content, does not.
     */
    fun matchesVendoredFt4(path: String, content: String): Boolean {
        val vendored = vendoredFt4Contents[normalizeFt4Path(path)] ?: return false
        return vendored == normalizeLineEndings(content)
    }

    /** Why a submitted lib/ft4 file is being scanned like app code. */
    fun modifiedFt4Note(path: String): String =
        "${normalizeFt4Path(path)} differs from vendored FT4 $FT4_VERSION - scanned as user code."

    /**
     * Files submitted under a vendored lib root (lib/ft4, lib/iccf,
     * lib/iccf_test). When ANY are present the vendored zip must NOT be
     * provisioned - it would truncate-overwrite the user's files (audit F2);
     * compile with exactly what was sent and say so via [submittedVendoredNote].
     */
    fun submittedVendoredLibFileCount(files: Map<String, String>): Int =
        files.keys.count { isVendoredLibraryPath(it) }

    fun submittedVendoredNote(files: Map<String, String>): String {
        val roots = files.keys.filter { isVendoredLibraryPath(it) }
            .map { normalizeFt4Path(it).removePrefix("lib/").substringBefore('/') }
            .distinct()
            .sorted()
        val count = submittedVendoredLibFileCount(files)
        val where = roots.joinToString(" + ") { "lib/$it" }
        return "Using your submitted $where sources ($count file(s)) instead of the vendored FT4 $FT4_VERSION."
    }

    fun exemptedFt4Note(count: Int): String =
        "$count vendored-library file(s) under lib/ excluded from import/security scanning; " +
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
     *
     * Submitted files under lib/ (the `chr install` vendoring root) are library
     * code too and get the same treatment: compiled only when imported. `chr
     * build` compiles the modules reachable from the configured entry module, so
     * a vendored library shipping ALTERNATIVE modules (lib.icmf's receiver vs
     * metadata_receiver both mount `__icmf_message`) builds fine for chr but
     * false-redded here with a mount-name conflict when every submitted module
     * was force-compiled (price-oracle, real-world round 1). The existing
     * ifEmpty-null fallback at the call sites keeps lib-only submissions (e.g.
     * checking a library project itself) compiling everything, as before.
     */
    fun userAppModules(files: Map<String, String>): List<String> =
        files.filterValues { !RunRellTests.isTestModuleSource(it) }
            .filterKeys { !isVendoredLibPath(it) }
            .map { (path, content) -> RunRellTests.moduleNameForPath(path, content) }
            .filter { it.isNotEmpty() }
            .distinct()

    /** lib/... after the same ./ and src/ prefix normalization as source roots. */
    fun isVendoredLibPath(path: String): Boolean =
        normalizeFt4Path(path).startsWith("lib/")
}
