package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.ChrBuildHelp
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChromiaHelpGatewayTest {

    private val executor = ToolExecutor(RecordingRepository(), PromptManager())

    private fun call(arguments: kotlinx.serialization.json.JsonObject) = runBlocking {
        executor.executeTool(callToolRequest(name = "chromia_help", arguments = arguments))
    }

    @Test
    fun noTopicReturnsFullTopicIndex() {
        val result = call(buildJsonObject {})
        assertTrue(result.isError != true)
        val topics = result.structuredContent!!.getValue("topics").jsonArray.map { it.jsonPrimitive.content }
        assertEquals(McpTools.HELP_TOOL_NAMES, topics.toSet())
    }

    @Test
    fun topicDelegatesToUnderlyingHelpTool() {
        val result = call(buildJsonObject { put("topic", "chr_build_help") })
        assertTrue(result.isError != true)
        assertEquals(ChrBuildHelp.toJson(), result.structuredContent)
    }

    @Test
    fun shortTopicSpellingIsAccepted() {
        val result = call(buildJsonObject { put("topic", "chr_build") })
        assertEquals(ChrBuildHelp.toJson(), result.structuredContent)
    }

    @Test
    fun unknownTopicReturnsIndexWithGuidance() {
        val result = call(buildJsonObject { put("topic", "no_such_topic") })
        assertTrue(result.isError != true)
        val notes = result.structuredContent!!.getValue("notes").jsonPrimitive.content
        assertTrue(notes.contains("no_such_topic"), notes)
    }

    @Test
    fun compactModeDropsHelpToolsButKeepsGatewayAndEverythingElse() {
        val full = McpTools.allTools(compact = false).map { it.name }.toSet()
        val compact = McpTools.allTools(compact = true).map { it.name }.toSet()
        assertTrue("chromia_help" in compact)
        assertTrue(McpTools.HELP_TOOL_NAMES.none { it in compact })
        assertEquals(full - McpTools.HELP_TOOL_NAMES, compact)
        assertTrue(McpTools.HELP_TOOL_NAMES.all { it in full })
    }

    @Test
    fun compactModeEnvFlagParsing() {
        assertTrue(McpTools.compactToolsMode(mapOf("CHROMIA_MCP_COMPACT_TOOLS" to "true")))
        assertTrue(McpTools.compactToolsMode(mapOf("CHROMIA_MCP_COMPACT_TOOLS" to "TRUE")))
        assertFalse(McpTools.compactToolsMode(mapOf("CHROMIA_MCP_COMPACT_TOOLS" to "false")))
        assertFalse(McpTools.compactToolsMode(emptyMap()))
    }

    @Test
    fun disabledToolsEnvDropsAdvertisement() {
        assertEquals(
            setOf("rell_check", "run_rell_tests"),
            McpTools.disabledTools(mapOf("CHROMIA_MCP_DISABLE_TOOLS" to "rell_check, run_rell_tests,"))
        )
        assertEquals(emptySet<String>(), McpTools.disabledTools(emptyMap()))
        val names = McpTools.allTools(compact = true, disabled = setOf("rell_check", "run_rell_tests")).map { it.name }
        assertFalse("rell_check" in names)
        assertFalse("run_rell_tests" in names)
        assertTrue("rell_security_check" in names)
        assertTrue("chromia_help" in names)
    }
}
