package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Live-probe friction fix (2026-09-02): an agent porting a rell_check call to
 * check_dapp_project sent {files:{...}} - rell_check's parameter name - and
 * hit "Missing required parameter: rell". The two fixes under test:
 * (1) check_dapp_project accepts `files` as an alias for `rell` (noted),
 * (2) the disabled-tool refusal that points at check_dapp_project states the
 *     parameter shape, not just the tool name.
 */
class CheckDappProjectFilesAliasTest {

    private val scaffold = DappScaffold.files("alias_demo")
    private val yaml = scaffold.getValue("chromia.yml")
    private val main = scaffold.getValue("src/main.rell")

    private fun call(args: kotlinx.serialization.json.JsonObject) = runBlocking {
        ToolExecutor(RecordingRepository(), PromptManager())
            .executeTool(CallToolRequest(name = "check_dapp_project", arguments = args))
    }

    @Test
    fun filesMapIsAcceptedAsAliasForRellWithANote() {
        val result = call(
            buildJsonObject {
                put("yaml", yaml)
                put("files", buildJsonObject { put("src/main.rell", main) })
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!
        assertTrue(structured["ok"]!!.jsonPrimitive.content.toBoolean(), structured.toString())
        val notes = structured["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("`files` was accepted as an alias"), notes)
        assertTrue(notes.contains("prefer `rell`"), notes)
    }

    @Test
    fun rellWinsWhenBothParametersArePresent() {
        val broken = main + "\nquery broken() = no_such_symbol;\n"
        val result = call(
            buildJsonObject {
                put("yaml", yaml)
                put("rell", buildJsonObject { put("src/main.rell", main) })
                put("files", buildJsonObject { put("src/main.rell", broken) })
            }
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        // The valid `rell` content was checked, not the broken `files` content.
        assertTrue(structured["ok"]!!.jsonPrimitive.content.toBoolean(), structured.toString())
        val notes = structured["notes"]!!.jsonPrimitive.content
        assertFalse(notes.contains("alias"), notes)
    }

    @Test
    fun missingBothParametersNamesTheAliasInTheError() {
        val result = call(buildJsonObject { put("yaml", yaml) })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Missing required parameter: rell"), text)
        assertTrue(text.contains("`files` is accepted as an alias"), text)
        assertTrue(text.contains("path -> source"), text)
    }

    @Test
    fun filesAliasIsDeclaredInTheSchema() {
        val tool = McpTools.checkDappProjectTool()
        val files = tool.inputSchema.properties["files"]
        assertNotNull(files, "schema must declare the `files` alias")
        // `rell` stays the required parameter; the alias is optional.
        assertEquals(listOf("rell"), tool.inputSchema.required)
    }

    @Test
    fun disabledToolRefusalStatesTheParameterShape() {
        val disabled = setOf("rell_check", "rell_security_check", "run_rell_tests")
        disabled.forEach { name ->
            val message = McpTools.disabledToolRefusal(name, disabled)!!
            assertTrue(message.contains("check_dapp_project"), message)
            assertTrue(message.contains("pass your sources as `rell`"), message)
            assertTrue(message.contains("map of path -> source or a single source string"), message)
            assertTrue(message.contains("`files` map is accepted as an alias"), message)
        }
    }
}
