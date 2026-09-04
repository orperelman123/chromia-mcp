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
import org.junit.jupiter.api.Assertions.assertFalse
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

    // Reality audit D7: an only-@test submission used to report "Compiled 0
    // module(s) successfully" although the test modules did compile.
    @Test
    fun onlyTestModuleSubmissionCountsTestModulesInNotes() {
        val result = RellCheck.check(
            mapOf("my_test.rell" to "@test module;\nfunction test_nothing() {}"),
            null
        )
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.notes.contains("1 @test module(s)"), result.notes)
        assertFalse(result.notes.contains("0 module(s)"), result.notes)
    }

    @Test
    fun mixedSubmissionCountsAppAndTestModulesSeparately() {
        val result = RellCheck.check(
            mapOf(
                "main.rell" to "module;\nquery ping() = \"pong\";",
                "my_test.rell" to "@test module;\nfunction test_nothing() {}"
            ),
            null
        )
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.notes.contains("1 module(s) and 1 @test module(s)"), result.notes)
    }

    @Test
    fun plainSubmissionNotesStayUnchanged() {
        val result = RellCheck.check(mapOf("main.rell" to "module;\nquery ping() = \"pong\";"), null)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.notes.contains("Compiled 1 module(s) successfully"), result.notes)
        assertFalse(result.notes.contains("@test"), result.notes)
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

    // DX audit 2026-09-04 (P6): rell_security_check / rell_check with `source` =
    // the stablecoin TEST module alone filed it as main.rell, so `import main;`
    // resolved to itself and 20 "Unknown name: 'main.x'" errors described a file
    // that was never sent. The test module must be placed as a test module and
    // the note must name the module it imports and how to pass it.
    @Test
    fun testModuleAsSingleSourceIsPlacedAsATestModuleAndTheMissingImportIsNamed() {
        val test = DappScaffold.files("peg", template = "stablecoin").getValue("src/test/main_test.rell")
        for (tool in listOf("rell_check", "rell_security_check")) {
            val result = callViaExecutor(tool, buildJsonObject { put("source", test) })
            val text = (result.content.first() as TextContent).text!!
            assertFalse(text.contains("Unknown name: 'main."), "$tool: the test file must not be compiled AS main: ${text.take(400)}")
            assertTrue(text.contains("`source` is a @test module, placed at test/main_test.rell."), "$tool: ${text.take(600)}")
            assertTrue(text.contains("It imports main - not submitted"), "$tool: ${text.take(600)}")
            assertTrue(text.contains("Pass `files` with the app module(s) AND the test file"), "$tool: ${text.take(600)}")
        }
        // An app module as `source` is unchanged: main.rell, no note.
        val ok = callViaExecutor("rell_check", buildJsonObject { put("source", "module;\nquery ping() = \"pong\";") })
        val okText = (ok.content.first() as TextContent).text!!
        assertTrue(okText.contains("\"ok\":true"), okText)
        assertFalse(okText.contains("@test module, placed"), okText)
        // A self-contained test module compiles alone and says so only on failure.
        val alone = callViaExecutor("rell_check", buildJsonObject { put("source", "@test module;\nfunction test_x() { assert_true(true); }") })
        val aloneText = (alone.content.first() as TextContent).text!!
        assertTrue(aloneText.contains("\"ok\":true"), aloneText)
    }

    // DX audit 2026-09-04 (P9): `require(true, "oops);` is reported as a syntax
    // error at the `(` - true for the parser, useless for the agent. The note
    // must say what the line most likely is.
    @Test
    fun unterminatedStringLiteralIsNamedInTheNotes() {
        val result = RellCheck.check(mapOf("main.rell" to "module;\noperation x() {\n    require(true, \"oops);\n}\n"), null)
        assertFalse(result.ok)
        assertEquals(3, result.errors.first().line)
        assertTrue(result.notes.contains("Line 3 of main.rell has an odd number of double quotes - most likely an unterminated string literal"), result.notes)
        // A genuine syntax error on a line with balanced quotes gets no such guess.
        val plain = RellCheck.check(mapOf("main.rell" to "module;\noperation x() {\n    require(true, \"oops\")\n}\n"), null)
        assertFalse(plain.ok)
        assertFalse(plain.notes.contains("unterminated string literal"), plain.notes)
    }
}
