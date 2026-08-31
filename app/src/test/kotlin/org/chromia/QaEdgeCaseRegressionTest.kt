package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.AssetTopHoldersStrategy
import org.chromia.tools.AllTransactionsStrategy
import org.chromia.tools.DappScaffold
import org.chromia.tools.RellCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.ScaffoldDappStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Regressions for the adversarial QA edge-case findings (2026-08-31). */
class QaEdgeCaseRegressionTest {

    private val repo = RecordingRepository()

    private fun call(strategy: org.chromia.tools.ToolStrategy, args: kotlinx.serialization.json.JsonObject) =
        runBlocking { strategy.execute(CallToolRequest(name = "t", arguments = args), repo) }

    /** Real agent path: the executor converts validation failures into tool errors. */
    private fun callViaExecutor(tool: String, args: kotlinx.serialization.json.JsonObject) = runBlocking {
        org.chromia.tools.ToolExecutor(repo, org.chromia.tools.PromptManager())
            .executeTool(CallToolRequest(name = tool, arguments = args))
    }

    // 1. scaffold_dapp must never silently rename
    @Test
    fun invalidScaffoldNameProducesWarning() {
        val json = DappScaffold.toJson("my-dapp")
        assertEquals("hello", json.getValue("name").jsonPrimitive.content)
        val warnings = json.getValue("warnings").jsonArray.map { it.jsonPrimitive.content }
        assertTrue(warnings.any { it.contains("my-dapp") }, warnings.toString())
    }

    @Test
    fun unknownTemplateProducesWarning() {
        val json = DappScaffold.toJson("notes", template = "nonexistent")
        val warnings = json.getValue("warnings").jsonArray.map { it.jsonPrimitive.content }
        assertTrue(warnings.any { it.contains("nonexistent") }, warnings.toString())
    }

    @Test
    fun validNameHasNoWarnings() {
        val json = DappScaffold.toJson("notes_app", template = "ft4")
        assertEquals("notes_app", json.getValue("name").jsonPrimitive.content)
        assertTrue(json.getValue("warnings").jsonArray.isEmpty())
    }

    @Test
    fun scaffoldStrategySurfacesWarnings() {
        val result = call(ScaffoldDappStrategy(), buildJsonObject { put("name", "My Dapp!") })
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("warnings"), text.take(200))
    }

    // 2. BOM must not break valid sources
    @Test
    fun bomPrefixedSourceCompiles() {
        val bom = "﻿"
        val result = RellCheck.check(mapOf("main.rell" to bom + "module;\nquery ping() = \"pong\";"), null)
        assertTrue(result.ok, "BOM-prefixed source must compile: ${result.errors}")
    }

    @Test
    fun stripBomOnlyRemovesLeadingMarker() {
        assertEquals("module;", RellCheck.stripBom("﻿module;"))
        assertEquals("a﻿b", RellCheck.stripBom("a﻿b"))
    }

    // 3. Case-insensitive path collisions must be explicit
    @Test
    fun caseCollisionIsRejectedWithClearMessage() {
        val e = runCatching {
            RellCheck.check(mapOf("a.rell" to "module;", "A.rell" to "module;"), null)
        }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException, "expected validation error, got $e")
        assertTrue(e!!.message!!.contains("collision"), e.message!!)
    }

    @Test
    fun caseCollisionRejectedInTestRunner() {
        val e = runCatching {
            RunRellTests.run(mapOf("t.rell" to "@test module;", "T.rell" to "@test module;"), databaseUrl = null)
        }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException, "expected validation error, got $e")
        assertTrue(e!!.message!!.contains("collision"), e.message!!)
    }

    // 5. Invalid pagination/time ranges fail locally, not upstream
    @Test
    fun negativeLimitIsRejectedLocally() {
        val result = callViaExecutor("get_asset_top_holders", buildJsonObject { put("assetId", "x"); put("limit", -1) })
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must be a positive integer"), text.take(200))
        assertEquals(null, repo.lastCall, "must not reach the repository")
    }

    @Test
    fun zeroLimitIsRejectedLocally() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("limit", 0) })
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must be a positive integer"), text.take(200))
    }

    @Test
    fun invertedTimeRangeIsRejectedLocally() {
        val result = callViaExecutor(
            "get_all_transactions",
            buildJsonObject { put("timestampFrom", "2000000000000"); put("timestampTo", "1000000000000") }
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("must not be later than"), text.take(200))
    }

    @Test
    fun validPaginationStillPassesThrough() {
        val result = call(AllTransactionsStrategy(), buildJsonObject { put("limit", 5); put("offset", 0) })
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    // 6. ISO time windows were never validated: requireOrderedTimestamps only
    // tried toLongOrNull, so "ISO format" (the documented schema) was a no-op.
    @Test
    fun invertedIsoTimeRangeIsRejectedLocally() {
        val result = callViaExecutor(
            "get_all_transactions",
            buildJsonObject { put("timestampFrom", "2025-06-01T00:00:00Z"); put("timestampTo", "2024-01-01T00:00:00Z") }
        )
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("must not be later than"), text.take(200))
    }

    @Test
    fun orderedIsoTimeRangePassesThrough() {
        val result = call(
            AllTransactionsStrategy(),
            buildJsonObject { put("timestampFrom", "2024-01-01"); put("timestampTo", "2025-06-01T12:30:00Z") }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    @Test
    fun malformedTimestampsDoNotThrowLocally() {
        val result = call(
            AllTransactionsStrategy(),
            buildJsonObject { put("timestampFrom", "not-a-date"); put("timestampTo", "also-bad") }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    @Test
    fun mixedEpochAndIsoTimestampsAreNotRejected() {
        val result = call(
            AllTransactionsStrategy(),
            buildJsonObject { put("timestampFrom", "1700000000000"); put("timestampTo", "2020-01-01T00:00:00Z") }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
    }

    // 7. Malformed pagination used to be silently dropped by extractInt,
    // hiding the agent's mistake behind unpaginated results.
    @Test
    fun nonNumericLimitIsRejectedWithClearError() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("limit", "twenty") })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must be an integer"), text.take(200))
    }

    @Test
    fun nonNumericOffsetIsRejectedWithClearError() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("offset", "abc") })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("offset must be an integer"), text.take(200))
    }

    @Test
    fun outOfIntRangeLimitIsRejectedWithClearError() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("limit", 99_999_999_999L) })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit is out of range"), text.take(200))
    }

    @Test
    fun absurdlyLargeLimitIsRejectedWithClearError() {
        val result = callViaExecutor("get_all_transactions", buildJsonObject { put("limit", 1_000_000) })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("limit must not exceed"), text.take(200))
    }
}
