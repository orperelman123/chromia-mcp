package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.CheckDappProject
import org.chromia.tools.CheckDappProjectStrategy
import org.chromia.tools.CheckFt4ImportsStrategy
import org.chromia.tools.DappScaffold
import org.chromia.tools.Ft4ImportCheck
import org.chromia.tools.RellLibs
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RellSecurityCheckStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the 2026-09-01 audit follow-up round:
 *
 * F2 - a submitted lib/ft4 tree tripped forbidden-module CRITICALs against
 *      FT4's OWN library files: v1.1.0r itself contains `operation ras_open(`
 *      (lib/ft4/core/accounts/strategies/open/module.rell) and
 *      `import lib.ft4.admin;` (lib/ft4/test/core/assets.rell), so the exact
 *      submission flow the skip-provisioning fix legitimized reported ok=false
 *      with errors pointing INTO the library. Vendored-library files are now
 *      exempt from Ft4ImportCheck and the banned-module/strategy security
 *      rules; the user's APP files remain fully scanned.
 *
 * F1 - the security check's compile gate and check_dapp_project dropped
 *      compile.notes, losing the "Using your submitted lib/ft4 sources" note
 *      and misattributing errors to the vendored tree.
 *
 * Hash gate (agent-experience round security enhancement): the F2 exemption is
 * no longer granted on the path alone - a submitted lib/ft4 file is exempt
 * ONLY if its content matches the vendored FT4 v1.1.0r copy at the same
 * relative path (line endings normalized). A differing file (foreign fork,
 * patched, or planted) is scanned like app code, with a note saying why.
 */
class AuditLibFt4ExemptionRegressionTest {

    private val repo = RecordingRepository()

    /** The genuine vendored FT4 v1.1.0r tree, keyed the way a chr-installed
     *  project submits it (src/lib/ft4/...). Bit-identical content is what
     *  earns the scanning exemption under the hash gate. */
    private val libTree: Map<String, String> =
        RellLibs.vendoredFt4Files().mapKeys { (path, _) -> "src/$path" }

    /** The crosschain modules import lib.iccf - a separate library that is not
     *  vendored - so compiling them (and their dependents) standalone fails for
     *  reasons unrelated to the exemption under test. Compile-gated tests use
     *  the tree without that closure; scanner-only tests use the full tree. */
    private val compilableLibTree: Map<String, String> = libTree.filterKeys { path ->
        !path.startsWith("src/lib/ft4/external/crosschain/") &&
            !path.startsWith("src/lib/ft4/crosschain/") &&
            !path.startsWith("src/lib/ft4/external/admin/crosschain/") &&
            !path.startsWith("src/lib/ft4/admin/crosschain/") &&
            path != "src/lib/ft4/test/core/assets.rell" &&
            path != "src/lib/ft4/test/core/module.rell"
    }

    private val vendoredVersion = RellLibs.vendoredFt4Files().getValue("lib/ft4/version.rell")

    /** A vendored file with a planted unauthenticated mutation appended -
     *  compiles, but must be scanned and flagged, never exempted. */
    private val plantedVersion = vendoredVersion +
        "\nentity backdoor_log { name; }\n" +
        "operation ras_backdoor(dest: text) { create backdoor_log(name = dest); }\n"

    private val cleanApp = "module;\nquery greet() = \"hi\";\n"

    private val yaml = DappScaffold.files("hello_dapp").getValue("chromia.yml")

    // ---- F2: check_dapp_project ------------------------------------------

    @Test
    fun submittedFt4TreeIsNotFlaggedByCheckDappProject() {
        val result = CheckDappProject.check(yaml, compilableLibTree + ("src/main.rell" to cleanApp))
        assertTrue(result.ok, result.errors.toString())
        assertTrue(
            result.notes.any { it.contains("vendored-library") },
            "expected the exemption note, got: ${result.notes}"
        )
        // F1: the compile note must also survive into the project result.
        assertTrue(
            result.notes.any { it.contains("submitted lib/ft4") },
            "expected the submitted-tree compile note, got: ${result.notes}"
        )
    }

    @Test
    fun appFileImportingAdminIsStillFlaggedByCheckDappProject() {
        val result = CheckDappProject.check(
            yaml,
            compilableLibTree + ("src/main.rell" to "module;\nimport lib.ft4.admin;\n")
        )
        assertFalse(result.ok)
        assertTrue(
            result.errors.any { it.startsWith("main.rell") && it.contains("forbidden FT4 production module") },
            "the APP import must still be flagged: ${result.errors}"
        )
        // No finding may point into the library tree.
        assertTrue(
            result.errors.none { it.contains("lib/ft4/") },
            "no error may point into lib/ft4: ${result.errors}"
        )
    }

    @Test
    fun checkDappProjectStrategyCarriesNotesThrough() = runBlocking {
        val result = CheckDappProjectStrategy().execute(
            callToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("yaml", yaml)
                    put(
                        "rell",
                        buildJsonObject {
                            compilableLibTree.forEach { (path, content) -> put(path, content) }
                            put("src/main.rell", cleanApp)
                        }
                    )
                }
            ),
            repo
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
        val notes = structured.getValue("notes").jsonPrimitive.content
        assertTrue(notes.contains("vendored-library"), notes)
        assertTrue(notes.contains("submitted lib/ft4"), notes)
    }

    // ---- F2: rell_security_check -----------------------------------------

    @Test
    fun securityCheckExemptsSubmittedFt4TreeButScansAppFiles() {
        val result = RellSecurityCheck.analyze(libTree + ("src/main.rell" to cleanApp))
        assertTrue(result.ok, result.findings.toString())
        assertTrue(result.findings.isEmpty(), result.findings.toString())
        assertTrue(result.notes.contains("vendored-library"), result.notes)

        val bad = RellSecurityCheck.analyze(
            libTree + ("src/main.rell" to "module;\nimport lib.ft4.admin;\n")
        )
        assertFalse(bad.ok)
        assertTrue(
            bad.findings.any { it.severity == "CRITICAL" && it.rule == "banned-module" && it.file == "src/main.rell" },
            bad.findings.toString()
        )
        assertTrue(
            bad.findings.none { RellLibs.isSubmittedFt4Path(it.file) },
            "no finding may point into lib/ft4: ${bad.findings}"
        )
    }

    @Test
    fun securityCheckStrategyOkOnSubmittedFt4Tree() = runBlocking {
        val result = RellSecurityCheckStrategy().execute(
            callToolRequest(
                name = "rell_security_check",
                arguments = buildJsonObject {
                    put(
                        "files",
                        buildJsonObject {
                            compilableLibTree.forEach { (path, content) -> put(path, content) }
                            put("src/main.rell", cleanApp)
                        }
                    )
                }
            ),
            repo
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
        assertEquals(0, structured.getValue("findings").jsonArray.size, structured.toString())
        assertTrue(structured.getValue("notes").jsonPrimitive.content.contains("vendored-library"), structured.toString())
    }

    // ---- F2: check_ft4_imports -------------------------------------------

    @Test
    fun ft4ImportScanExemptsLibTreeAndNotesIt() {
        val result = Ft4ImportCheck.scanFiles(libTree + ("src/main.rell" to cleanApp))
        assertTrue(result.ok, result.errors.toString())
        assertEquals(libTree.size, result.exemptedLibFiles)

        val bad = Ft4ImportCheck.scanFiles(
            libTree + ("src/main.rell" to "module;\nimport lib.ft4.admin;\n")
        )
        assertFalse(bad.ok)
        assertTrue(bad.errors.any { it.contains("src/main.rell") }, bad.errors.toString())
    }

    @Test
    fun checkFt4ImportsStrategyNotesExemption() = runBlocking {
        val result = CheckFt4ImportsStrategy().execute(
            callToolRequest(
                name = "check_ft4_imports",
                arguments = buildJsonObject {
                    put(
                        "rell",
                        buildJsonObject {
                            libTree.forEach { (path, content) -> put(path, content) }
                            put("src/main.rell", cleanApp)
                        }
                    )
                }
            ),
            repo
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
        assertTrue(structured.getValue("notes").jsonPrimitive.content.contains("vendored-library"), structured.toString())
    }

    // ---- Hash gate: exemption requires bit-identical vendored content ------

    @Test
    fun crlfVendoredFileStillExempt() {
        // CRLF vs LF must never defeat the content match (Windows editors).
        val crlf = vendoredVersion.replace("\n", "\r\n")
        val result = RellSecurityCheck.analyze(
            mapOf("src/lib/ft4/version.rell" to crlf, "src/main.rell" to cleanApp)
        )
        assertTrue(result.ok, result.findings.toString())
        assertTrue(result.findings.isEmpty(), result.findings.toString())
        assertTrue(result.notes.contains("vendored-library"), result.notes)
        assertFalse(result.notes.contains("differs"), result.notes)
    }

    @Test
    fun plantedLibFt4FileIsScannedAndFlaggedBySecurityCheck() {
        val result = RellSecurityCheck.analyze(
            mapOf("src/lib/ft4/version.rell" to plantedVersion, "src/main.rell" to cleanApp)
        )
        assertFalse(result.ok, result.notes)
        assertTrue(
            result.findings.any {
                it.severity == "HIGH" && it.rule == "unauthenticated-mutation" &&
                    it.file == "src/lib/ft4/version.rell"
            },
            "the planted mutation must be flagged despite the lib/ft4 path: ${result.findings}"
        )
        assertTrue(
            result.notes.contains(
                "lib/ft4/version.rell differs from vendored FT4 ${RellLibs.FT4_VERSION} - scanned as user code"
            ),
            result.notes
        )
        // Nothing matched the vendored copy, so no exemption note.
        assertFalse(result.notes.contains("vendored-library"), result.notes)
    }

    @Test
    fun mixedSubmissionScansOnlyTheModifiedFile() {
        val vendoredAdmin = RellLibs.vendoredFt4Files().getValue("lib/ft4/admin/module.rell")
        val result = RellSecurityCheck.analyze(
            mapOf(
                "src/lib/ft4/admin/module.rell" to vendoredAdmin,
                "src/lib/ft4/version.rell" to plantedVersion,
                "src/main.rell" to cleanApp
            )
        )
        assertFalse(result.ok)
        assertTrue(
            result.findings.isNotEmpty() && result.findings.all { it.file == "src/lib/ft4/version.rell" },
            "only the modified file may produce findings: ${result.findings}"
        )
        assertTrue(result.notes.contains("1 vendored-library file(s)"), result.notes)
        assertTrue(result.notes.contains("lib/ft4/version.rell differs"), result.notes)
        assertFalse(result.notes.contains("admin/module.rell differs"), result.notes)
    }

    @Test
    fun ft4ImportScanFlagsModifiedLibFileWithNote() {
        val forked = vendoredVersion + "\nimport lib.ft4.admin;\n"
        val result = Ft4ImportCheck.scanFiles(
            mapOf(
                "src/lib/ft4/admin/module.rell" to
                    RellLibs.vendoredFt4Files().getValue("lib/ft4/admin/module.rell"),
                "src/lib/ft4/version.rell" to forked,
                "src/main.rell" to cleanApp
            )
        )
        assertFalse(result.ok)
        assertEquals(1, result.exemptedLibFiles)
        assertTrue(
            result.errors.any { it.contains("src/lib/ft4/version.rell") && it.contains("forbidden") },
            "the forked lib file's forbidden import must be flagged: ${result.errors}"
        )
        assertTrue(
            result.warnings.any { it.contains("lib/ft4/version.rell differs from vendored FT4") },
            "the differs-note must explain why the lib file was scanned: ${result.warnings}"
        )
    }

    @Test
    fun checkDappProjectScansPlantedLibFile() {
        val result = CheckDappProject.check(
            yaml,
            compilableLibTree + ("src/lib/ft4/version.rell" to plantedVersion) + ("src/main.rell" to cleanApp)
        )
        assertFalse(result.ok, "planted lib/ft4 mutation must fail the gate: ${result.notes}")
        assertTrue(
            result.errors.any { it.contains("lib/ft4/version.rell") && it.contains("unauthenticated-mutation") },
            "the planted operation must surface as a blocking finding: ${result.errors}"
        )
        assertTrue(
            result.notes.any { it.contains("lib/ft4/version.rell differs from vendored FT4") },
            "the differs-note must be carried: ${result.notes}"
        )
        // The untouched rest of the tree stays exempt.
        assertTrue(
            result.notes.any { it.contains("vendored-library") },
            "identical files must still be exempt: ${result.notes}"
        )
    }

    // ---- F1: compile notes survive the compile-gate error paths -----------

    @Test
    fun securityCheckCompileGateKeepsSubmittedTreeNote() = runBlocking {
        val result = RellSecurityCheckStrategy().execute(
            callToolRequest(
                name = "rell_security_check",
                arguments = buildJsonObject {
                    put(
                        "files",
                        buildJsonObject {
                            put("main.rell", "module;\nthis does not compile\n")
                            put("lib/ft4/module.rell", "module;\n")
                        }
                    )
                }
            ),
            repo
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(false, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
        val notes = structured.getValue("notes").jsonPrimitive.content
        assertTrue(notes.contains("fix rell_check errors first"), notes)
        assertTrue(notes.contains("submitted lib/ft4"), notes)
    }

    @Test
    fun checkDappProjectKeepsCompileNotesOnFailure() {
        val result = CheckDappProject.check(
            yaml,
            mapOf(
                "main.rell" to "module;\nthis does not compile\n",
                "lib/ft4/module.rell" to "module;\n"
            )
        )
        assertFalse(result.ok)
        assertTrue(
            result.notes.any { it.contains("submitted lib/ft4") },
            "compile notes must survive: ${result.notes}"
        )
    }
}
