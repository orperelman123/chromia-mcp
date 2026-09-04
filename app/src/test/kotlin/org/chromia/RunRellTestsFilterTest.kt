package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.RunRellTests
import org.chromia.tools.RunRellTestsStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `tests` - the `chr test --tests` filter, exposed through the tool and the
 * runner. Round 11 (2026-09-04): the Rell runner has had `testPatterns` all
 * along (glob on the function name, `module:function`, or the module) and this
 * server never passed it, so an agent chasing ONE red case re-ran the whole
 * suite every time - and this project's own template gate ran 60 mutants x a
 * full suite each, 2148 of the suite's 2380 seconds.
 */
class RunRellTestsFilterTest {

    private val repo = RecordingRepository()

    private val files = mapOf(
        "lib.rell" to "module;\nfunction double(x: integer): integer = x * 2;",
        "lib_test.rell" to """
            @test module;
            import lib;
            function test_alpha() { assert_equals(lib.double(2), 4); }
            function test_beta() { assert_equals(lib.double(3), 6); }
            function test_gamma_broken() { assert_equals(lib.double(2), 5); }
        """.trimIndent()
    )

    private fun names(result: RunRellTests.Result) = result.cases.map { it.name.substringAfterLast(':') }.toSet()

    @Test
    fun noFilterRunsEverything() {
        val result = RunRellTests.run(files, databaseUrl = null)
        assertEquals(setOf("test_alpha", "test_beta", "test_gamma_broken"), names(result))
        assertFalse(result.ok)
    }

    @Test
    fun exactNameRunsOnlyThatCase() {
        val result = RunRellTests.run(files, databaseUrl = null, tests = listOf("test_alpha"))
        assertEquals(setOf("test_alpha"), names(result))
        assertTrue(result.ok, result.notes)
        assertTrue(result.notes.contains("tests=[test_alpha]"), result.notes)
    }

    @Test
    fun globAndSeveralPatternsUnion() {
        val result = RunRellTests.run(files, databaseUrl = null, tests = listOf("*beta", "test_gamma*"))
        assertEquals(setOf("test_beta", "test_gamma_broken"), names(result))
        assertFalse(result.ok)
        assertEquals(1, result.failed)
    }

    @Test
    fun qualifiedModuleColonNameMatches() {
        val result = RunRellTests.run(files, databaseUrl = null, tests = listOf("lib_test:test_beta"))
        assertEquals(setOf("test_beta"), names(result))
    }

    /** A filter that matches nothing is not a green run - it names the pattern and what it could have matched. */
    @Test
    fun unmatchedFilterIsRedAndListsTheTestFunctions() {
        val result = RunRellTests.run(files, databaseUrl = null, tests = listOf("test_delta"))
        assertFalse(result.ok)
        assertEquals(0, result.total)
        assertTrue(result.notes.contains("no test function matched tests=[test_delta]"), result.notes)
        assertTrue(result.notes.contains("test_alpha"), result.notes)
        assertTrue(result.notes.contains("test_gamma_broken"), result.notes)
        assertFalse(result.notes.contains("must be named test_*"), "the generic no-tests hint is wrong when a filter is the reason: ${result.notes}")
    }

    @Test
    fun blankPatternsAreRejected() {
        val e = runCatching { RunRellTests.run(files, databaseUrl = null, tests = listOf("test_alpha", " ")) }.exceptionOrNull()
        assertTrue(e is IllegalArgumentException, "$e")
        assertTrue(e!!.message!!.contains("tests"), e.message)
    }

    private fun call(arguments: JsonObject) = runBlocking {
        RunRellTestsStrategy().execute(CallToolRequest(name = "run_rell_tests", arguments = arguments), repo)
    }

    private fun filesJson() = buildJsonObject { files.forEach { (k, v) -> put(k, v) } }

    @Test
    fun toolAcceptsAnArrayOfPatterns() {
        val result = call(
            buildJsonObject {
                put("files", filesJson())
                put("tests", buildJsonArray { add("test_alpha"); add("test_beta") })
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals("2", structured.getValue("total").jsonPrimitive.content)
        assertEquals("true", structured.getValue("ok").jsonPrimitive.content)
    }

    /** Agents pass a single name as a string (and chr takes a comma list); accept both rather than fail the call. */
    @Test
    fun toolAcceptsACommaSeparatedString() {
        val result = call(
            buildJsonObject {
                put("files", filesJson())
                put("tests", "test_alpha, test_gamma*")
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val cases = result.structuredContent!!.getValue("cases").jsonArray.map { it.jsonObject.getValue("name").jsonPrimitive.content.substringAfterLast(':') }
        assertEquals(setOf("test_alpha", "test_gamma_broken"), cases.toSet())
    }

    @Test
    fun toolRejectsNonStringPatternsByPosition() {
        val result = call(
            buildJsonObject {
                put("files", filesJson())
                put("tests", buildJsonArray { add("test_alpha"); add(7) })
            }
        )
        assertTrue(result.isError == true)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("tests[1]"), text)
    }

    @Test
    fun toolTreatsAnEmptyArrayAsNoFilter() {
        val result = call(
            buildJsonObject {
                put("files", filesJson())
                put("tests", buildJsonArray { })
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        assertEquals("3", result.structuredContent!!.getValue("total").jsonPrimitive.content)
    }
}
