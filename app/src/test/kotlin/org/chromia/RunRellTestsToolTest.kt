package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.RunRellTestsStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RunRellTestsToolTest {

    private val repo = RecordingRepository()

    private fun run(arguments: kotlinx.serialization.json.JsonObject) = runBlocking {
        RunRellTestsStrategy().execute(
            CallToolRequest(name = "run_rell_tests", arguments = arguments),
            repo
        )
    }

    @Test
    fun passingAndFailingCasesAreReported() {
        val result = run(
            buildJsonObject {
                put(
                    "files",
                    buildJsonObject {
                        put("lib.rell", "module;\nfunction double(x: integer): integer = x * 2;")
                        put(
                            "lib_test.rell",
                            """
                            @test module;
                            import lib;
                            function test_double_ok() { assert_equals(lib.double(2), 4); }
                            function test_double_broken() { assert_equals(lib.double(2), 5); }
                            """.trimIndent()
                        )
                    }
                )
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals("2", structured.getValue("total").jsonPrimitive.content)
        assertEquals("1", structured.getValue("passed").jsonPrimitive.content)
        assertEquals("1", structured.getValue("failed").jsonPrimitive.content)
        assertEquals(false, structured.getValue("ok").jsonPrimitive.content.toBoolean())
        val cases = structured.getValue("cases").jsonArray.map { it.jsonObject }
        val broken = cases.first { it.getValue("name").jsonPrimitive.content.contains("broken") }
        assertEquals("false", broken.getValue("ok").jsonPrimitive.content)
        assertTrue(broken.containsKey("error"), broken.toString())
    }

    @Test
    fun allPassingReportsOk() {
        val result = run(
            buildJsonObject {
                put(
                    "files",
                    buildJsonObject {
                        put(
                            "math_test.rell",
                            "@test module;\nfunction test_math() { assert_equals(2 + 2, 4); }"
                        )
                    }
                )
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean(), structured.toString())
    }

    @Test
    fun missingTestModuleGivesGuidance() {
        val result = run(
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", "module;") })
            }
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(result.isError == true)
        assertTrue(text.contains("@test module"), text)
    }
}
