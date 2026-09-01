package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.domain.NetworkResult
import org.chromia.tools.CheckDappProject
import org.chromia.tools.CheckDappProjectStrategy
import org.chromia.tools.CheckFt4ImportsStrategy
import org.chromia.tools.ChrAggregatesStrategy
import org.chromia.tools.ChromiaRellPracticesHelp
import org.chromia.tools.DappScaffold
import org.chromia.tools.Ft4ImportCheck
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RellCheck
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RellSecurityCheckStrategy
import org.chromia.tools.ToolExecutor
import org.chromia.tools.summarizeChrAggregates
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the 2026-09-01 probe findings batch:
 * - check_dapp_project works without a yaml (default chromia.yml, noted)
 * - chromia_help topic aliases (security / best_practices / best-practices)
 * - rell_check FT4-version-mismatch hint on "Module 'lib.ft4.*' not found"
 * - allowAdminModules escape hatch (check_dapp_project, check_ft4_imports,
 *   rell_security_check)
 * - test-surface HIGH findings downgrade to MEDIUM with a -test-surface suffix
 * - get_chr_aggregates summarized default + full:true (808KB responses)
 */
class ProbeImprovementsRegressionTest {

    private val executor = ToolExecutor(RecordingRepository(), PromptManager())

    private fun textOf(result: io.modelcontextprotocol.kotlin.sdk.CallToolResult): String =
        (result.content.first() as TextContent).text!!

    // ---- item 2: optional yaml -----------------------------------------

    @Test
    fun checkDappProjectWorksWithoutYamlAndSaysSo() = runBlocking {
        val main = DappScaffold.files("hello").getValue("src/main.rell")
        val result = CheckDappProjectStrategy().execute(
            CallToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("rell", buildJsonObject { put("src/main.rell", main) })
                }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = result.structuredContent!!
        assertEquals(true, payload["ok"]!!.jsonPrimitive.content.toBoolean(), payload.toString())
        val notes = payload["notes"]!!.jsonPrimitive.content
        assertTrue(
            notes.contains("used a default chromia.yml (rellVersion ${DappScaffold.RELL_VERSION})"),
            notes
        )
    }

    @Test
    fun checkDappProjectWithYamlIsUnchangedAndNotesNoDefault() {
        val scaffold = DappScaffold.files("hello")
        val result = CheckDappProject.check(
            scaffold.getValue("chromia.yml"),
            mapOf("src/main.rell" to scaffold.getValue("src/main.rell"))
        )
        assertTrue(result.ok, result.errors.toString())
        assertFalse(result.notes.joinToString(" ").contains("default chromia.yml"), result.notes.toString())
    }

    @Test
    fun checkDappProjectSchemaNoLongerRequiresYaml() {
        val schema = McpTools.checkDappProjectTool().inputSchema
        assertEquals(listOf("rell"), schema.required)
        assertTrue("allowAdminModules" in schema.properties.keys, schema.properties.keys.toString())
    }

    // ---- item 3: help topic aliases ------------------------------------

    @Test
    fun helpTopicAliasesResolveToRellPractices() = runBlocking {
        listOf("security", "best_practices", "best-practices").forEach { alias ->
            val result = executor.executeTool(
                CallToolRequest(
                    name = "chromia_help",
                    arguments = buildJsonObject { put("topic", alias) }
                )
            )
            assertTrue(result.isError != true, alias)
            assertEquals(ChromiaRellPracticesHelp.toJson(), result.structuredContent, alias)
        }
    }

    // ---- item 4: FT4 version mismatch hint -----------------------------

    @Test
    fun missingFt4ModuleErrorGetsVersionMismatchHint() {
        // lib.ft4.test.utils existed before FT4 1.1.0r and is gone in the
        // vendored tree, so the compile fails with "Module ... not found".
        val result = RellCheck.check(
            mapOf("main.rell" to "module;\nimport lib.ft4.test.utils;\n"),
            null
        )
        assertFalse(result.ok)
        assertTrue(
            result.errors.any { it.text.contains("lib.ft4.test.utils") },
            result.errors.toString()
        )
        assertTrue(result.notes.contains("vendors FT4 v1.1.0r"), result.notes)
        assertTrue(result.notes.contains("older or newer FT4"), result.notes)
    }

    @Test
    fun nonFt4MissingModuleGetsNoMismatchHint() {
        // FT4 provisioned, but the missing module is the user's own - no hint.
        val result = RellCheck.check(
            mapOf("main.rell" to "module;\nimport lib.ft4.assets;\nimport nope;\n"),
            null
        )
        assertFalse(result.ok)
        assertFalse(result.notes.contains("vendors FT4"), result.notes)
    }

    @Test
    fun noFt4InvolvedMeansNoMismatchHint() {
        val result = RellCheck.check(mapOf("main.rell" to "module;\nimport nope;\n"), null)
        assertFalse(result.ok)
        assertFalse(result.notes.contains("vendors FT4"), result.notes)
    }

    // ---- item 5: allowAdminModules escape hatch ------------------------

    @Test
    fun ft4ImportCheckDefaultStillErrorsOnAdminModules() {
        val result = Ft4ImportCheck.scan("import lib.ft4.admin;")
        assertFalse(result.ok)
        assertTrue(result.errors.any { it.contains("lib.ft4.admin") }, result.errors.toString())
    }

    @Test
    fun ft4ImportCheckAllowAdminModulesDowngradesToWarnings() {
        val result = Ft4ImportCheck.scan("import lib.ft4.admin;", allowAdminModules = true)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(
            result.warnings.any {
                it.contains("forbidden FT4 production module lib.ft4.admin") &&
                    it.contains("(allowed by allowAdminModules)")
            },
            result.warnings.toString()
        )
        // The hit is still reported for tooling that inspects hits.
        assertTrue(result.hits.any { it.module == "lib.ft4.admin" })
    }

    @Test
    fun securityCheckAllowAdminModulesDowngradesBannedFindings() {
        val files = mapOf("main.rell" to "module;\nimport lib.ft4.admin;\n")
        val default = RellSecurityCheck.analyze(files)
        assertFalse(default.ok)
        val critical = default.findings.single { it.rule == "banned-module" }
        assertEquals("CRITICAL", critical.severity)

        val allowed = RellSecurityCheck.analyze(files, allowAdminModules = true)
        assertTrue(allowed.ok, allowed.findings.toString())
        val downgraded = allowed.findings.single { it.rule == "banned-module" }
        assertEquals("MEDIUM", downgraded.severity)
        assertTrue(downgraded.text.endsWith("(allowed by allowAdminModules)"), downgraded.text)
        assertTrue(allowed.notes.contains("allowAdminModules=true"), allowed.notes)
    }

    @Test
    fun securityCheckStrategyAcceptsAllowAdminModules() = runBlocking {
        val result = RellSecurityCheckStrategy().execute(
            CallToolRequest(
                name = "rell_security_check",
                arguments = buildJsonObject {
                    put("source", "module;\nimport lib.ft4.admin;\n")
                    put("allowAdminModules", true)
                }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = result.structuredContent!!
        assertEquals(true, payload["ok"]!!.jsonPrimitive.content.toBoolean(), payload.toString())
        val findings = payload["findings"]!!.jsonArray
        assertTrue(
            findings.any { it.jsonObject["severity"]!!.jsonPrimitive.content == "MEDIUM" },
            payload.toString()
        )
    }

    @Test
    fun checkDappProjectAllowAdminModulesTurnsErrorsIntoWarnings() = runBlocking {
        val rell = buildJsonObject { put("main.rell", "module;\nimport lib.ft4.admin;\n") }

        val default = CheckDappProjectStrategy().execute(
            CallToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject { put("rell", rell) }
            ),
            RecordingRepository()
        )
        assertEquals(
            false,
            default.structuredContent!!["ok"]!!.jsonPrimitive.content.toBoolean(),
            default.structuredContent.toString()
        )

        val allowed = CheckDappProjectStrategy().execute(
            CallToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("rell", rell)
                    put("allowAdminModules", true)
                }
            ),
            RecordingRepository()
        )
        val payload = allowed.structuredContent!!
        assertEquals(true, payload["ok"]!!.jsonPrimitive.content.toBoolean(), payload.toString())
        val warnings = payload["warnings"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(warnings.any { it.contains("(allowed by allowAdminModules)") }, warnings.toString())
    }

    @Test
    fun checkFt4ImportsStrategyAcceptsAllowAdminModules() = runBlocking {
        val result = CheckFt4ImportsStrategy().execute(
            CallToolRequest(
                name = "check_ft4_imports",
                arguments = buildJsonObject {
                    put("rell", "import lib.ft4.admin;")
                    put("allowAdminModules", true)
                }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val payload = result.structuredContent!!
        assertEquals(true, payload["ok"]!!.jsonPrimitive.content.toBoolean(), payload.toString())
        assertEquals(0, payload["errors"]!!.jsonArray.size, payload.toString())
    }

    @Test
    fun scaffoldPolicyIsUntouchedByTheEscapeHatch() {
        // The forbidden list itself and the scaffold templates never relax.
        assertTrue("lib.ft4.admin" in DappScaffold.forbiddenModules)
        DappScaffold.files("hello", "ft4").values.forEach { content ->
            assertFalse(content.contains("import lib.ft4.admin;"), "scaffold must never import admin")
        }
    }

    // ---- item 6: test-surface findings downgrade -----------------------

    private val mutatingOp = "operation seed(name: text) { create user(name = name); }"

    @Test
    fun helperMutationInTestOnlyModuleIsMediumTestSurface() {
        val files = mapOf(
            "main.rell" to "module;\nentity user { name: text; }\n",
            "fixtures.rell" to "module;\nimport main;\n$mutatingOp\n",
            "main_test.rell" to "@test module;\nimport fixtures;\nfunction test_x() {}\n"
        )
        val result = RellSecurityCheck.analyze(files)
        val finding = result.findings.single { it.rule.startsWith("unauthenticated-mutation") }
        assertEquals("MEDIUM", finding.severity, result.findings.toString())
        assertEquals("unauthenticated-mutation-test-surface", finding.rule)
        assertEquals("fixtures.rell", finding.file)
        assertTrue(result.ok, "test-surface MEDIUM must not block: ${result.findings}")
        assertTrue(result.notes.contains("-test-surface"), result.notes)
    }

    @Test
    fun sameMutationInAppModuleStaysHigh() {
        val files = mapOf(
            "main.rell" to "module;\nentity user { name: text; }\n$mutatingOp\n"
        )
        val result = RellSecurityCheck.analyze(files)
        val finding = result.findings.single { it.rule.startsWith("unauthenticated-mutation") }
        assertEquals("HIGH", finding.severity)
        assertEquals("unauthenticated-mutation", finding.rule)
        assertFalse(result.ok)
    }

    @Test
    fun helperImportedByAppAndTestStaysHigh() {
        val files = mapOf(
            "main.rell" to "module;\nimport helper;\nentity user { name: text; }\n",
            "helper.rell" to "module;\n$mutatingOp\n",
            "main_test.rell" to "@test module;\nimport helper;\nfunction test_x() {}\n"
        )
        val result = RellSecurityCheck.analyze(files)
        val finding = result.findings.single { it.rule.startsWith("unauthenticated-mutation") }
        assertEquals("HIGH", finding.severity, result.findings.toString())
    }

    @Test
    fun testsDirectoryPathIsTestSurface() {
        val files = mapOf(
            "main.rell" to "module;\nentity user { name: text; }\n",
            "tests/helper.rell" to "module;\nimport main;\n$mutatingOp\n"
        )
        val result = RellSecurityCheck.analyze(files)
        val finding = result.findings.single { it.rule.startsWith("unauthenticated-mutation") }
        assertEquals("MEDIUM", finding.severity, result.findings.toString())
        assertEquals("unauthenticated-mutation-test-surface", finding.rule)
    }

    @Test
    fun criticalInTestModuleStaysCritical() {
        val files = mapOf(
            "main_test.rell" to "@test module;\nimport lib.ft4.admin;\nfunction test_x() {}\n"
        )
        val result = RellSecurityCheck.analyze(files)
        val finding = result.findings.single { it.rule == "banned-module" }
        assertEquals("CRITICAL", finding.severity)
        assertFalse(result.ok)
    }

    // ---- item 7: get_chr_aggregates size control -----------------------

    private fun bigAggregates(entries: Int) = buildJsonObject {
        put(
            "chrAggregates",
            buildJsonObject {
                put(
                    "groupedDeposits",
                    buildJsonArray {
                        repeat(entries) { i ->
                            add(
                                buildJsonObject {
                                    put("address", "0x$i")
                                    put("networkId", "1")
                                    put("total", "$i")
                                }
                            )
                        }
                    }
                )
                put("groupedWithdrawals", buildJsonArray {})
                put(
                    "totals",
                    buildJsonObject {
                        put("depositsTotal", "100")
                        put("withdrawalsTotal", "0")
                    }
                )
            }
        )
    }

    @Test
    fun summarizeCapsLongArraysAndNotes() {
        val summarized = summarizeChrAggregates(bigAggregates(60))
        val aggregates = summarized["chrAggregates"]!!.jsonObject
        assertEquals(50, aggregates["groupedDeposits"]!!.jsonArray.size)
        // Totals and small arrays are untouched.
        assertEquals("100", aggregates["totals"]!!.jsonObject["depositsTotal"]!!.jsonPrimitive.content)
        assertEquals(0, aggregates["groupedWithdrawals"]!!.jsonArray.size)
        val note = summarized["note"]!!.jsonPrimitive.content
        assertTrue(note.contains("chrAggregates.groupedDeposits"), note)
        assertTrue(note.contains("first 50 of 60"), note)
        assertTrue(note.contains("full:true"), note)
    }

    @Test
    fun summarizeLeavesSmallResponsesUntouched() {
        val small = bigAggregates(3)
        assertEquals(small, summarizeChrAggregates(small))
    }

    @Test
    fun chrAggregatesDefaultIsSummarized() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(bigAggregates(60))
        val result = ChrAggregatesStrategy().execute(
            CallToolRequest(name = "get_chr_aggregates", arguments = buildJsonObject {}),
            repo
        )
        assertTrue(result.isError != true)
        val payload = result.structuredContent!!
        assertEquals(
            50,
            payload["chrAggregates"]!!.jsonObject["groupedDeposits"]!!.jsonArray.size,
            payload.toString()
        )
        assertTrue(payload["note"]!!.jsonPrimitive.content.contains("full:true"), payload.toString())
        assertTrue(textOf(result).contains("full:true"))
    }

    @Test
    fun chrAggregatesFullTrueReturnsUncappedShape() = runBlocking {
        val repo = RecordingRepository()
        val data = bigAggregates(60)
        repo.next = NetworkResult.Success(data)
        val result = ChrAggregatesStrategy().execute(
            CallToolRequest(
                name = "get_chr_aggregates",
                arguments = buildJsonObject { put("full", true) }
            ),
            repo
        )
        assertTrue(result.isError != true)
        // Exactly the old behavior: the repository payload, uncapped, no note.
        assertEquals(data, result.structuredContent)
    }

    @Test
    fun chrAggregatesSchemaDocumentsFullFlag() {
        val schema = McpTools.getChrAggregatesTool().inputSchema
        assertTrue("full" in schema.properties.keys, schema.properties.keys.toString())
        assertTrue(McpTools.getChrAggregatesTool().description!!.contains("full:true"))
    }
}
