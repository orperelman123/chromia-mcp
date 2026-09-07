package org.chromia

import org.chromia.tools.propertiesOrEmpty

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.domain.NetworkResult
import org.chromia.tools.CHR_AGGREGATES_ARRAY_CAP
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

    /**
     * The production repository pointed at a closed loopback port. Every claim
     * from here down to item 6 is local - the Rell compiler, the FT4 import
     * scan, the security check, the scaffold, the tool schemas - so the
     * repository is only a required parameter of `execute(request, repository)`.
     * It used to be a `RecordingRepository`, a double of our own
     * `ChromiaRepository`; the real implementation aimed at a dead port is
     * strictly better in that job, because a tool that quietly reached for the
     * network would now fail with a real ConnectException instead of being
     * handed an invented answer. Item 7 is a different matter and goes live.
     */
    private val executor = ToolExecutor(McpTestSupport.offlineRepository(), PromptManager())

    private fun textOf(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): String =
        (result.content.first() as TextContent).text!!

    // ---- item 2: optional yaml -----------------------------------------

    @Test
    fun checkDappProjectWorksWithoutYamlAndSaysSo() = runBlocking {
        val main = DappScaffold.files("hello").getValue("src/main.rell")
        val result = CheckDappProjectStrategy().execute(
            callToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("rell", buildJsonObject { put("src/main.rell", main) })
                }
            ),
            McpTestSupport.offlineRepository()
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
        // The point of this pin is that `yaml` is NOT required. AUDIT F10 made
        // `files` the canonical Rell-sources name ("`files` for Rell sources" -
        // the spelling eight other code-taking tools already use) with `rell`
        // still read as an alias, so the one required input is now `files`.
        assertEquals(listOf("files"), schema.required)
        assertTrue("rell" in schema.propertiesOrEmpty.keys, schema.propertiesOrEmpty.keys.toString())
        assertTrue("allowAdminModules" in schema.propertiesOrEmpty.keys, schema.propertiesOrEmpty.keys.toString())
    }

    // ---- item 3: help topic aliases ------------------------------------

    @Test
    fun helpTopicAliasesResolveToRellPractices() = runBlocking {
        listOf("security", "best_practices", "best-practices").forEach { alias ->
            val result = executor.executeTool(
                callToolRequest(
                    name = "chromia_help",
                    arguments = buildJsonObject { put("topic", alias); put("section", "all") }
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
            callToolRequest(
                name = "rell_security_check",
                arguments = buildJsonObject {
                    put("source", "module;\nimport lib.ft4.admin;\n")
                    put("allowAdminModules", true)
                }
            ),
            McpTestSupport.offlineRepository()
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
            callToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject { put("rell", rell) }
            ),
            McpTestSupport.offlineRepository()
        )
        assertEquals(
            false,
            default.structuredContent!!["ok"]!!.jsonPrimitive.content.toBoolean(),
            default.structuredContent.toString()
        )

        val allowed = CheckDappProjectStrategy().execute(
            callToolRequest(
                name = "check_dapp_project",
                arguments = buildJsonObject {
                    put("rell", rell)
                    put("allowAdminModules", true)
                }
            ),
            McpTestSupport.offlineRepository()
        )
        val payload = allowed.structuredContent!!
        assertEquals(true, payload["ok"]!!.jsonPrimitive.content.toBoolean(), payload.toString())
        val warnings = payload["warnings"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue(warnings.any { it.contains("(allowed by allowAdminModules)") }, warnings.toString())
    }

    @Test
    fun checkFt4ImportsStrategyAcceptsAllowAdminModules() = runBlocking {
        val result = CheckFt4ImportsStrategy().execute(
            callToolRequest(
                name = "check_ft4_imports",
                arguments = buildJsonObject {
                    put("rell", "import lib.ft4.admin;")
                    put("allowAdminModules", true)
                }
            ),
            McpTestSupport.offlineRepository()
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
        // Round 2 D4 updated the policy: @test modules are EXEMPT from the
        // banned-module scan (FT4's own test helpers import lib.ft4.admin, and
        // exercising admin modules is what test code does) - with a note. The
        // same import in a NON-test module stays CRITICAL.
        val files = mapOf(
            "main_test.rell" to "@test module;\nimport lib.ft4.admin;\nfunction test_x() {}\n"
        )
        val result = RellSecurityCheck.analyze(files)
        assertTrue(result.findings.none { it.rule == "banned-module" }, result.findings.toString())
        assertTrue(result.notes.contains("@test module"), result.notes)

        val production = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nimport lib.ft4.admin;\n")
        )
        val finding = production.findings.single { it.rule == "banned-module" }
        assertEquals("CRITICAL", finding.severity)
        assertFalse(production.ok)
    }

    // ---- item 7: get_chr_aggregates size control -----------------------
    //
    // These tests used to run on `bigAggregates(60)` - a JsonObject the test
    // built to look like the explorer, handed to a RecordingRepository which
    // handed it straight back. Every one of them was therefore a statement about
    // a shape the test had invented, and none of them could notice that the
    // explorer's real response had changed, shrunk below the cap (which would
    // make the whole summarizer dead weight), or stopped being served.
    //
    // The mainnet explorer serves the real thing. On 2026-09-07 `chrAggregates`
    // answered with 6576 grouped deposits - the several-hundred-KB response the
    // cap exists for - so the cap, the note and full:true are all asserted
    // against the payload that motivated them. `includeGroupedWithdrawals:false`
    // is the explorer's own way of producing a SMALL real response, which is
    // what the untouched-response test needs; nothing here is constructed.

    /**
     * Things only the explorer can say. Its refusal is upstream's, and it must
     * arrive as a real error carrying upstream's own words - never as a success
     * with a missing envelope, which would be ours.
     */
    private val upstreamMarkers = listOf(
        "internal_error", "http 4", "http 5", "bad request", "service unavailable",
        "gateway", "timeout", "timed out", "connection reset", "connection refused",
        "connection closed", "no route to host", "unknownhost", "request timeout"
    )

    /**
     * The `data.chrAggregates` object the explorer served, or null when the
     * explorer refused. A refusal must carry upstream's words; a success whose
     * `data` envelope is missing is a swallowed failure dressed as an answer and
     * fails here.
     */
    private fun assertLiveChrAggregates(payload: JsonObject?, errorText: String?): JsonObject? {
        if (errorText != null) {
            assertTrue(
                upstreamMarkers.any { errorText.lowercase().contains(it) },
                "get_chr_aggregates failed and nothing in the message is an upstream signature, " +
                    "so the failure is ours: $errorText"
            )
            return null
        }
        val data = payload!!["data"]
        assertTrue(
            data != null && data is JsonObject,
            "a get_chr_aggregates success with no `data` envelope is a swallowed upstream failure: $payload"
        )
        val aggregates = (data as JsonObject)["chrAggregates"]
        assertTrue(
            aggregates is JsonObject,
            "the explorer answered without a chrAggregates object - either it renamed the field or " +
                "the query is reading the wrong one: $data"
        )
        return aggregates as JsonObject
    }

    /** The tool's structured payload when it succeeded, its error text when it did not. */
    private fun assertLiveChrAggregatesTool(
        result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
    ): JsonObject? = assertLiveChrAggregates(
        result.structuredContent,
        if (result.isError == true) textOf(result) else null
    )

    /**
     * THE CAP, THE NOTE AND THE DEFAULT, ON THE REAL RESPONSE.
     *
     * One live call proves all three: the arrays past the cap are cut to exactly
     * [CHR_AGGREGATES_ARRAY_CAP], an array under it is left alone, the totals are
     * copied through untouched, and the note names the truncated path, the real
     * total and the escape hatch. The "of N" in the note is the explorer's own
     * count, so the assertion that N is past the cap is the assertion that this
     * tool still has a size problem worth solving.
     */
    @Test
    fun summarizeCapsLongArraysAndNotes() = runBlocking {
        LiveChromia.requireLive("reads the real mainnet CHR deposit aggregates - the response the cap exists for")
        val result = ChrAggregatesStrategy().execute(
            callToolRequest(
                name = "get_chr_aggregates",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("includeTotals", true)
                    put("includeGroupedDeposits", true)
                    // The withdrawals breakdown is the small array in this
                    // response; keeping it out of the selection is how the
                    // explorer itself supplies an under-the-cap array.
                    put("includeGroupedWithdrawals", false)
                }
            ),
            LiveChromia.repository()
        )
        val aggregates = assertLiveChrAggregatesTool(result) ?: return@runBlocking
        val deposits = aggregates.getValue("groupedDeposits").jsonArray
        assertEquals(
            CHR_AGGREGATES_ARRAY_CAP, deposits.size,
            "the default response must be capped at $CHR_AGGREGATES_ARRAY_CAP entries: ${deposits.size}"
        )
        // An array under the cap is untouched, and the totals are copied verbatim.
        assertEquals(0, aggregates.getValue("groupedWithdrawals").jsonArray.size)
        assertTrue(
            aggregates.getValue("totals").jsonObject
                .getValue("depositsTotal").jsonPrimitive.content.toLong() > 0,
            "mainnet has CHR deposits: $aggregates"
        )
        val note = result.structuredContent!!.getValue("note").jsonPrimitive.content
        assertTrue(note.contains("chrAggregates.groupedDeposits"), note)
        assertTrue(note.contains("full:true"), note)
        val total = Regex("""first $CHR_AGGREGATES_ARRAY_CAP of (\d+) entries""").find(note)
        assertTrue(total != null, "the note must say how many entries the explorer really sent: $note")
        assertTrue(
            total!!.groupValues[1].toInt() > CHR_AGGREGATES_ARRAY_CAP,
            "the real response must still be past the cap, or the summarizer is dead weight: $note"
        )
        assertTrue(textOf(result).contains("full:true"))
    }

    /**
     * A REAL response with nothing to cap comes back byte-for-byte.
     *
     * `includeGroupedDeposits:false, includeGroupedWithdrawals:false` is a real
     * explorer answer - two empty arrays and the totals - so the identity
     * comparison is made against something the explorer produced rather than
     * against a three-entry object the test wrote.
     */
    @Test
    fun summarizeLeavesSmallResponsesUntouched() = runBlocking {
        LiveChromia.requireLive("reads the totals-only CHR aggregates - a real response with nothing to cap")
        val result = LiveChromia.repository().getChrAggregates(
            LiveChromia.EXPLORER_NETWORK,
            includeTotals = true,
            includeGroupedDeposits = false,
            includeGroupedWithdrawals = false
        )
        val small = (result as? NetworkResult.Success)?.data
        val aggregates = assertLiveChrAggregates(
            small,
            (result as? NetworkResult.Error)?.message
        ) ?: return@runBlocking
        assertTrue(
            aggregates.getValue("groupedDeposits").jsonArray.size <= CHR_AGGREGATES_ARRAY_CAP,
            "this selection is supposed to be the small one: $aggregates"
        )
        assertTrue(
            aggregates.getValue("totals").jsonObject
                .getValue("withdrawalsTotal").jsonPrimitive.content.toLong() > 0
        )
        assertEquals(small, summarizeChrAggregates(small!!), "nothing was over the cap; nothing may change")
    }

    /**
     * full:true is the escape hatch: the repository's payload, uncapped, no note.
     * The same live query the capped test makes, so "uncapped" is measured
     * against the explorer's real count rather than against a constant.
     */
    @Test
    fun chrAggregatesFullTrueReturnsUncappedShape() = runBlocking {
        LiveChromia.requireLive("calls get_chr_aggregates with full:true against the live explorer")
        val result = ChrAggregatesStrategy().execute(
            callToolRequest(
                name = "get_chr_aggregates",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("includeTotals", true)
                    put("includeGroupedDeposits", true)
                    put("includeGroupedWithdrawals", false)
                    put("full", true)
                }
            ),
            LiveChromia.repository()
        )
        val aggregates = assertLiveChrAggregatesTool(result) ?: return@runBlocking
        assertTrue(
            aggregates.getValue("groupedDeposits").jsonArray.size > CHR_AGGREGATES_ARRAY_CAP,
            "full:true must not cap: ${aggregates.getValue("groupedDeposits").jsonArray.size}"
        )
        assertFalse(
            result.structuredContent!!.containsKey("note"),
            "the uncapped response has nothing to note: ${result.structuredContent!!.keys}"
        )
        assertEquals(
            setOf("data"), result.structuredContent!!.keys,
            "full:true is the repository payload and nothing else: ${result.structuredContent!!.keys}"
        )
    }

    @Test
    fun chrAggregatesSchemaDocumentsFullFlag() {
        val schema = McpTools.getChrAggregatesTool().inputSchema
        assertTrue("full" in schema.propertiesOrEmpty.keys, schema.propertiesOrEmpty.keys.toString())
        assertTrue(McpTools.getChrAggregatesTool().description!!.contains("full:true"))
    }
}
