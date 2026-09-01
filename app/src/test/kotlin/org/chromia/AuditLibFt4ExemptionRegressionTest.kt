package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
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
 */
class AuditLibFt4ExemptionRegressionTest {

    private val repo = RecordingRepository()

    /** Minimal compilable stand-in for a chr-installed FT4 tree, mirroring the
     *  two v1.1.0r files that legitimately contain forbidden names. */
    private val libTree = mapOf(
        "src/lib/ft4/admin/module.rell" to "module;\n",
        "src/lib/ft4/core/accounts/strategies/open/module.rell" to
            "module;\noperation ras_open(main_ad: text) { require(main_ad != \"\"); }\n",
        "src/lib/ft4/test/core/assets.rell" to "module;\nimport lib.ft4.admin;\n"
    )

    private val cleanApp = "module;\nquery greet() = \"hi\";\n"

    private val yaml = DappScaffold.files("hello_dapp").getValue("chromia.yml")

    // ---- F2: check_dapp_project ------------------------------------------

    @Test
    fun submittedFt4TreeIsNotFlaggedByCheckDappProject() {
        val result = CheckDappProject.check(yaml, libTree + ("src/main.rell" to cleanApp))
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
            libTree + ("src/main.rell" to "module;\nimport lib.ft4.admin;\n")
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
            CallToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("yaml", yaml)
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
            CallToolRequest(
                name = "rell_security_check",
                arguments = buildJsonObject {
                    put(
                        "files",
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
        assertEquals(0, structured.getValue("findings").jsonArray.size, structured.toString())
        assertTrue(structured.getValue("notes").jsonPrimitive.content.contains("vendored-library"), structured.toString())
    }

    // ---- F2: check_ft4_imports -------------------------------------------

    @Test
    fun ft4ImportScanExemptsLibTreeAndNotesIt() {
        val result = Ft4ImportCheck.scanFiles(libTree + ("src/main.rell" to cleanApp))
        assertTrue(result.ok, result.errors.toString())
        assertEquals(3, result.exemptedLibFiles)

        val bad = Ft4ImportCheck.scanFiles(
            libTree + ("src/main.rell" to "module;\nimport lib.ft4.admin;\n")
        )
        assertFalse(bad.ok)
        assertTrue(bad.errors.any { it.contains("src/main.rell") }, bad.errors.toString())
    }

    @Test
    fun checkFt4ImportsStrategyNotesExemption() = runBlocking {
        val result = CheckFt4ImportsStrategy().execute(
            CallToolRequest(
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

    // ---- F1: compile notes survive the compile-gate error paths -----------

    @Test
    fun securityCheckCompileGateKeepsSubmittedTreeNote() = runBlocking {
        val result = RellSecurityCheckStrategy().execute(
            CallToolRequest(
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
