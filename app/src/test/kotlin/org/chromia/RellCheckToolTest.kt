package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.RellCheckStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RellCheckToolTest {

    private val repo = RecordingRepository()

    private fun run(arguments: kotlinx.serialization.json.JsonObject) = runBlocking {
        RellCheckStrategy().execute(
            CallToolRequest(name = "rell_check", arguments = arguments),
            repo
        )
    }

    @Test
    fun validSingleModuleCompilesOk() {
        val result = run(
            buildJsonObject {
                put(
                    "source",
                    """
                    module;
                    entity book { key isbn: text; title: text; }
                    operation add_book(isbn: text, title: text) {
                        create book(isbn, title);
                    }
                    query get_books() = book @* {} ( .isbn, .title );
                    """.trimIndent()
                )
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean())
        assertEquals(0, structured.getValue("errors").jsonArray.size)
    }

    @Test
    fun brokenSourceReportsErrorWithLineNumber() {
        val result = run(
            buildJsonObject {
                put(
                    "source",
                    """
                    module;
                    query broken() = unknown_symbol_xyz;
                    """.trimIndent()
                )
            }
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        assertEquals(false, structured.getValue("ok").jsonPrimitive.content.toBoolean())
        val errors = structured.getValue("errors").jsonArray
        assertTrue(errors.isNotEmpty(), "expected at least one error diagnostic")
        val first = errors[0].jsonObject
        assertEquals("ERROR", first.getValue("severity").jsonPrimitive.content)
        assertNotNull(first["line"], "error should carry a line number: $first")
        assertEquals("2", first.getValue("line").jsonPrimitive.content)
    }

    @Test
    fun multiFileProjectCompiles() {
        val result = run(
            buildJsonObject {
                put(
                    "files",
                    buildJsonObject {
                        put("main.rell", "module;\nimport lib.util;\nquery version_of() = util.lib_version();")
                        put("lib/util.rell", "module;\nfunction lib_version(): text = \"1.0\";")
                    }
                )
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean())
    }

    @Test
    fun pathTraversalIsRejected() {
        val result = run(
            buildJsonObject {
                put("files", buildJsonObject { put("../evil.rell", "module;") })
            }
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(result.isError == true || text.contains("'..'") || text.contains("failed"), text)
        assertFalse(text.contains("\"ok\": true"), text)
    }

    @Test
    fun missingInputReturnsGuidance() {
        val result = run(buildJsonObject { put("modules", buildJsonArray { add("main") }) })
        assertTrue(result.isError == true)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("source") && text.contains("files"), text)
    }
}
