package org.chromia

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.ChrBuildHelp
import org.chromia.tools.ChromiaHelpStrategy
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.callToolRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Audit F9(b): `chromia_help{topic}` was all-or-nothing, and one topic
 * (chr_create_rell_dapp_help) cost 109,152 B / 27,288 tokens - four times what
 * compact mode saved in total; all 31 topics together were 785,687 B / 196,422
 * tokens. A topic now answers with its table of contents, `section` returns one
 * section, and `section:"all"` is still the whole payload so nothing is lost.
 */
class ChromiaHelpSectionTest {

    private val executor = ToolExecutor(McpTestSupport.offlineRepository(), PromptManager())

    /**
     * Measured bound on one section. Every section this server offers is under it;
     * the test prints the worst so the number stays honest.
     */
    private val sectionBoundBytes = 8_000

    private fun call(arguments: JsonObject): JsonObject {
        val result = runBlocking {
            executor.executeTool(callToolRequest(name = "chromia_help", arguments = arguments))
        }
        assertTrue(result.isError != true, "chromia_help $arguments returned an error")
        return checkNotNull(result.structuredContent) { "chromia_help $arguments returned no structured content" }
    }

    private fun toc(topic: String): JsonObject = call(buildJsonObject { put("topic", topic) })

    private fun section(topic: String, section: String): JsonObject =
        call(buildJsonObject { put("topic", topic); put("section", section) })

    @Test
    fun aTopicAnswersWithItsTableOfContentsNotThePayload() {
        val index = toc("chr_build_help")
        assertEquals("chr_build_help", index.getValue("topic").jsonPrimitive.content)
        val sections = index.getValue("sections").jsonArray
        assertTrue(sections.isNotEmpty(), "no sections listed for chr_build_help")
        sections.forEach { entry ->
            assertTrue(entry.jsonObject.containsKey("section"))
            assertTrue(entry.jsonObject.getValue("bytes").jsonPrimitive.content.toInt() >= 0)
        }
        val whole = ChrBuildHelp.toJson().toString().toByteArray().size
        assertEquals(whole, index.getValue("bytes").jsonPrimitive.content.toInt())
        assertTrue(
            index.toString().toByteArray().size < whole,
            "the table of contents is not cheaper than the payload it indexes"
        )
        assertTrue(index.getValue("notes").jsonPrimitive.content.contains("section"))
    }

    @Test
    fun sectionAllIsStillTheWholePayload() {
        val result = runBlocking {
            executor.executeTool(
                callToolRequest(
                    name = "chromia_help",
                    arguments = buildJsonObject { put("topic", "chr_build_help"); put("section", "all") }
                )
            )
        }
        assertEquals(ChrBuildHelp.toJson(), result.structuredContent)
    }

    @Test
    fun aNamedSectionReturnsThatSectionOnly() {
        val index = toc("chr_build_help")
        val first = index.getValue("sections").jsonArray.first().jsonObject
            .getValue("section").jsonPrimitive.content
        val payload = section("chr_build_help", first)
        assertEquals(first, payload.getValue("section").jsonPrimitive.content)
        assertTrue(payload.containsKey(first), "the section payload does not carry the section it names")
        assertTrue(
            payload.toString().toByteArray().size <= ChrBuildHelp.toJson().toString().toByteArray().size,
            "one section costs more than the whole topic"
        )
    }

    @Test
    fun unknownSectionReturnsTheTableOfContentsWithGuidance() {
        val result = section("chr_build_help", "no_such_section")
        assertTrue(result.getValue("notes").jsonPrimitive.content.contains("no_such_section"))
        assertTrue(result.containsKey("sections"))
    }

    /** Every section of every topic is under the measured bound, and reachable. */
    @Test
    fun everySectionOfEveryTopicIsUnderTheBound() {
        var worst = 0
        var worstName = ""
        var totalToc = 0
        McpTools.HELP_TOOL_NAMES.sorted().forEach { topic ->
            val index = toc(topic)
            totalToc += index.toString().toByteArray().size
            val sections = index.getValue("sections").jsonArray
            assertTrue(sections.isNotEmpty(), "$topic lists no sections")
            sections.forEach { entry ->
                val name = entry.jsonObject.getValue("section").jsonPrimitive.content
                val bytes = entry.jsonObject.getValue("bytes").jsonPrimitive.content.toInt()
                if (bytes > worst) {
                    worst = bytes
                    worstName = "$topic/$name"
                }
                // a listed section must actually resolve
                val payload = section(topic, name)
                assertEquals(name, payload.getValue("section").jsonPrimitive.content)
            }
        }
        println("help sections: worst=$worstName ${worst}B; all 31 tables of contents together=${totalToc}B")
        assertTrue(
            worst <= sectionBoundBytes,
            "$worstName is $worst B, over the $sectionBoundBytes B section bound"
        )
    }

    /** The gateway's own splitting rule: a big object is indexed by its sub-keys. */
    @Test
    fun oversizeSectionsAreSplitIntoSubSections() {
        val split = McpTools.HELP_TOOL_NAMES.any { topic ->
            toc(topic).getValue("sections").jsonArray.any {
                it.jsonObject.getValue("section").jsonPrimitive.content.contains('.')
            }
        }
        assertTrue(split, "no topic was split at all - ${ChromiaHelpStrategy.SECTION_SPLIT_BYTES} B is not doing anything")
    }
}
