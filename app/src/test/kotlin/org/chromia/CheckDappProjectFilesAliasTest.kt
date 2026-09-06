package org.chromia

import org.chromia.tools.propertiesOrEmpty

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
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
 * (1) check_dapp_project accepts `files` (noted),
 * (2) the disabled-tool refusal that points at check_dapp_project states the
 *     parameter shape, not just the tool name.
 *
 * AUDIT F10 (2026-09-06) settled which way round that is. `files` is the name
 * eight tools already used, so it is the CANONICAL one everywhere code is
 * taken; `rell` and `source` are accepted with no warning and no preference.
 * The note this used to append - "`files` was accepted as an alias for the
 * `rell` parameter - prefer `rell` in future calls" - was the one message in
 * the server that actively pushed an agent AWAY from the majority spelling,
 * and it is gone. What is pinned below is the new contract: both spellings
 * work, neither is nagged about, and `files` wins when both are sent.
 */
class CheckDappProjectFilesAliasTest {

    private val scaffold = DappScaffold.files("alias_demo")
    private val yaml = scaffold.getValue("chromia.yml")
    private val main = scaffold.getValue("src/main.rell")

    private fun call(args: kotlinx.serialization.json.JsonObject) = runBlocking {
        ToolExecutor(RecordingRepository(), PromptManager())
            .executeTool(callToolRequest(name = "check_dapp_project", arguments = args))
    }

    @Test
    fun filesMapIsAcceptedWithNoNagAboutIt() {
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
        assertFalse(notes.contains("prefer `rell`"), "the canonical name must not be nagged about: $notes")
        assertFalse(notes.contains("accepted as an alias"), notes)
    }

    @Test
    fun theCanonicalNameWinsWhenBothParametersArePresent() {
        val broken = main + "\nquery broken() = no_such_symbol;\n"
        val result = call(
            buildJsonObject {
                put("yaml", yaml)
                put("files", buildJsonObject { put("src/main.rell", main) })
                put("rell", buildJsonObject { put("src/main.rell", broken) })
            }
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        // The valid `files` content was checked, not the broken `rell` content:
        // one canonical name, and it is the one that decides.
        assertTrue(structured["ok"]!!.jsonPrimitive.content.toBoolean(), structured.toString())
        val notes = structured["notes"]!!.jsonPrimitive.content
        assertFalse(notes.contains("alias"), notes)
    }

    @Test
    fun missingBothParametersNamesTheCanonicalNameAndItsAliases() {
        val result = call(buildJsonObject { put("yaml", yaml) })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Missing required parameter: files"), text)
        assertTrue(text.contains("`rell` and `source` are accepted as aliases"), text)
        assertTrue(text.contains("path -> source"), text)
    }

    @Test
    fun theCanonicalNameIsDeclaredFirstAndIsTheRequiredOne() {
        val tool = McpTools.checkDappProjectTool()
        assertNotNull(tool.inputSchema.propertiesOrEmpty["files"], "schema must declare `files`")
        assertNotNull(tool.inputSchema.propertiesOrEmpty["rell"], "and keep `rell` as an accepted alias")
        // AUDIT F10: canonical first, so an agent skimming the schema meets the
        // one name that works everywhere before any alias.
        val order = tool.inputSchema.propertiesOrEmpty.keys.toList()
        assertTrue(order.indexOf("files") < order.indexOf("rell"), order.toString())
        assertEquals(listOf("files"), tool.inputSchema.required)
    }

    @Test
    fun disabledToolRefusalStatesTheParameterShape() {
        val disabled = setOf("rell_check", "rell_security_check", "run_rell_tests")
        disabled.forEach { name ->
            val message = McpTools.disabledToolRefusal(name, disabled)!!
            assertTrue(message.contains("check_dapp_project"), message)
            assertTrue(message.contains("pass your sources as `files`"), message)
            assertTrue(message.contains("map of path -> source or a single source string"), message)
            assertTrue(message.contains("`rell` and `source` are accepted as aliases"), message)
        }
    }
}
