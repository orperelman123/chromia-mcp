package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.McpJson
import io.modelcontextprotocol.kotlin.sdk.types.Prompt
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.serialization.builtins.ListSerializer
import org.chromia.tools.McpPrompts
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Audit F9, first-contact cost. Measured on the base commit with the audit's own
 * metric (the byte length of the compact JSON of the `tools` array):
 *
 * | | full | compact |
 * |---|---|---|
 * | tools | 74 | 43 |
 * | tools/list | 115,232 B | 89,627 B |
 * | prompts/list | -32601 (unimplemented) | same |
 * | resources/list | 590 B | same |
 * | first contact | 115,822 B (~29.0k tok) | 90,217 B (~22.6k tok) |
 *
 * Compact mode's entire benefit was dropping the 31 *_help schemas - 22% - while
 * the 15 largest schemas were byte-identical in both modes. This test measures
 * the same numbers on every run and fails above the bound, so the saving cannot
 * quietly evaporate again.
 */
class ContextBudgetTest {

    /**
     * resources/list, measured on the wire (audit section 1). Three static
     * resources, untouched by this work; counted so "first contact" is the whole
     * cost an agent pays before its first useful call, not just tools/list.
     */
    private val resourcesListBytes = 590

    /** 12k tokens at the audit's 4-bytes-per-token convention. */
    private val firstContactBoundBytes = 48_000

    private fun toolBytes(tools: List<Tool>): Int =
        McpJson.encodeToString(ListSerializer(Tool.serializer()), tools).toByteArray().size

    private fun promptBytes(prompts: List<Prompt>): Int =
        McpJson.encodeToString(ListSerializer(Prompt.serializer()), prompts).toByteArray().size

    private fun prompts(compact: Boolean): List<Prompt> =
        McpPrompts.catalogue(PromptManager()).map { entry ->
            val prompt = entry.toPrompt()
            if (compact) prompt.copy(description = App.compactPromptDescription(prompt.description.orEmpty()))
            else prompt
        }

    @Test
    fun compactFirstContactFitsTwelveThousandTokens() {
        val tools = McpTools.allTools(compact = true)
        val toolsBytes = toolBytes(tools)
        val promptsBytes = promptBytes(prompts(compact = true))
        val total = toolsBytes + promptsBytes + resourcesListBytes

        println(
            "compact first contact: tools=${tools.size} toolsList=${toolsBytes}B " +
                "promptsList=${promptsBytes}B resourcesList=${resourcesListBytes}B " +
                "total=${total}B (~${total / 4} tok)"
        )
        assertTrue(
            toolsBytes <= McpTools.COMPACT_TOOLS_BYTES,
            "compact tools/list is $toolsBytes B, over the ${McpTools.COMPACT_TOOLS_BYTES} B bound"
        )
        assertTrue(
            total <= firstContactBoundBytes,
            "compact first contact is $total B (~${total / 4} tok), over the $firstContactBoundBytes B bound"
        )
    }

    @Test
    fun fullCatalogStaysUnderItsBound() {
        val tools = McpTools.allTools(compact = false)
        val toolsBytes = toolBytes(tools)
        val promptsBytes = promptBytes(prompts(compact = false))
        val total = toolsBytes + promptsBytes + resourcesListBytes

        println(
            "full first contact: tools=${tools.size} toolsList=${toolsBytes}B " +
                "promptsList=${promptsBytes}B resourcesList=${resourcesListBytes}B " +
                "total=${total}B (~${total / 4} tok)"
        )
        assertTrue(
            toolsBytes <= McpTools.FULL_TOOLS_BYTES,
            "full tools/list is $toolsBytes B, over the ${McpTools.FULL_TOOLS_BYTES} B bound"
        )
    }

    /**
     * The point of compact mode. Before this work it saved 22% and left the 15
     * biggest schemas untouched; a compact catalog that is not at least a third
     * of the full one is not a compact catalog.
     */
    @Test
    fun compactModeSavesMostOfTheCatalog() {
        val full = toolBytes(McpTools.allTools(compact = false))
        val compact = toolBytes(McpTools.allTools(compact = true))
        val saving = 100 - (compact * 100 / full)
        println("compact saving: $compact of $full B ($saving%)")
        assertTrue(saving >= 65, "compact mode saves only $saving% ($compact of $full B); audit baseline was 22%")
    }

    /** Compact mode hides no tool but the *_help gateway covers, and adds none. */
    @Test
    fun compactModeDropsOnlyHelpToolSchemas() {
        val full = McpTools.allTools(compact = false).map { it.name }.toSet()
        val compact = McpTools.allTools(compact = true).map { it.name }.toSet()
        assertEquals(McpTools.HELP_TOOL_NAMES, full - compact)
        assertTrue((compact - full).isEmpty(), "compact mode invented tools: ${compact - full}")
        assertTrue("describe_tool" in compact, "describe_tool must survive compact mode - it is the way back to the prose")
        assertTrue("chromia_help" in compact)
    }

    /** Every advertised description is under the F9 bound, in both modes. */
    @Test
    fun everyAdvertisedDescriptionIsUnderTheBound() {
        McpTools.allTools(compact = false).forEach { tool ->
            val bytes = tool.description.orEmpty().toByteArray().size
            assertTrue(
                bytes <= McpTools.MAX_DESCRIPTION_BYTES,
                "${tool.name} description is $bytes B, over the ${McpTools.MAX_DESCRIPTION_BYTES} B bound"
            )
        }
        McpTools.allTools(compact = true).forEach { tool ->
            val bytes = tool.description.orEmpty().toByteArray().size
            assertTrue(
                bytes <= McpTools.COMPACT_HEADLINE_BYTES,
                "${tool.name} compact headline is $bytes B, over the ${McpTools.COMPACT_HEADLINE_BYTES} B bound"
            )
            assertTrue(tool.description.orEmpty().isNotBlank(), "${tool.name} has an empty compact headline")
        }
    }

    /** A compact headline is a PREFIX of the advertised description, never a rewrite. */
    @Test
    fun compactHeadlineIsAPrefixOfTheRealDescription() {
        val full = McpTools.allTools(compact = false).associate { it.name to it.description.orEmpty().trim() }
        McpTools.allTools(compact = true).forEach { tool ->
            val headline = tool.description.orEmpty().removeSuffix(" ...")
            assertTrue(
                full.getValue(tool.name).startsWith(headline),
                "${tool.name} compact headline is not a prefix of its description: $headline"
            )
        }
    }
}
