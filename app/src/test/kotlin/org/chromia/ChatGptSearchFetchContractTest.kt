package org.chromia

import org.chromia.tools.propertiesOrEmpty

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.callToolRequest
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ChatGPT's deep-research / connector contract for the two tools it calls by
 * name, pinned in one place.
 *
 * As OpenAI documents it:
 *  - `search` takes `{ "query": string }` and answers `{ "results": [ { "id",
 *    "title", "url" } ] }`.
 *  - `fetch` takes `{ "id": string }` - the id `search` handed out - and answers
 *    `{ "id", "title", "text", "url", "metadata"? }`.
 *  - Both answers must be present BOTH as `structuredContent` and as a JSON
 *    string in the first `content` block, because clients read one or the other.
 *
 * Everything about how those payloads are produced is covered by
 * SearchFetchToolsTest; this test only holds the wire shape still, so a change
 * to the docs tools that would silently break the ChatGPT connector fails here
 * rather than in someone's ChatGPT window.
 */
class ChatGptSearchFetchContractTest {

    private fun executor() = ToolExecutor(
        RecordingRepository(),
        PromptManager(),
        ragStoreFactory = { McpTestSupport.fixtureRagStore() }
    )

    /** The JSON string in `content[0]` must be the same object as structuredContent. */
    private fun contentJson(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test
    fun searchAnswersResultsOfIdTitleUrlInBothChannels() = runBlocking {
        val result = executor().executeTool(
            callToolRequest(name = "search", arguments = buildJsonObject { put("query", "FT4 authentication") })
        )
        assertEquals(false, result.isError == true)

        val structured = result.structuredContent
            ?: error("search must return structuredContent - ChatGPT reads it")
        val text = (result.content.first() as TextContent).text
        assertEquals(structured, contentJson(text), "content[0] must be structuredContent as a JSON string")

        assertEquals(setOf("results"), structured.keys)
        val results = structured.getValue("results").jsonArray
        assertTrue(results.isNotEmpty(), "fixture store must return a hit")
        results.forEach { hit ->
            val o = hit.jsonObject
            assertEquals(
                setOf("id", "title", "url"),
                o.keys,
                "a search result is exactly {id,title,url} - extra keys break strict connectors"
            )
            o.values.forEach { v -> assertTrue(v.jsonPrimitive.isString, "every field is a string: $o") }
        }
        assertEquals(segmentId(McpTestSupport.AUTH_SEGMENT), results.first().jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun fetchAnswersIdTitleTextUrlInBothChannels() = runBlocking {
        val executor = executor()
        val search = executor.executeTool(
            callToolRequest(name = "search", arguments = buildJsonObject { put("query", "FT4 authentication") })
        )
        val id = search.structuredContent!!.getValue("results").jsonArray
            .first().jsonObject.getValue("id").jsonPrimitive.content

        val result = executor.executeTool(
            callToolRequest(name = "fetch", arguments = buildJsonObject { put("id", id) })
        )
        assertEquals(false, result.isError == true)

        val structured = result.structuredContent
            ?: error("fetch must return structuredContent - ChatGPT reads it")
        val text = (result.content.first() as TextContent).text
        assertEquals(structured, contentJson(text), "content[0] must be structuredContent as a JSON string")

        // metadata is optional in the contract; this server does not emit it.
        assertEquals(setOf("id", "title", "text", "url"), structured.keys)
        assertEquals(id, structured.getValue("id").jsonPrimitive.content, "fetch echoes the id search handed out")
        assertTrue(structured.getValue("text").jsonPrimitive.content.contains("FT4 authentication"))
        assertTrue(structured.getValue("title").jsonPrimitive.content.isNotBlank())
        assertTrue(structured.getValue("url").jsonPrimitive.content.isNotBlank())
    }

    /**
     * An id ChatGPT invents (or one from a different index) must be a clean
     * tool-level error carrying the id back, not a protocol error and not a
     * fuzzy neighbour presented as the requested document.
     */
    @Test
    fun fetchOfAnUnknownIdIsACleanToolErrorNamingTheId() = runBlocking {
        val result = executor().executeTool(
            callToolRequest(name = "fetch", arguments = buildJsonObject { put("id", "not-an-id") })
        )
        assertEquals(true, result.isError)
        val structured = result.structuredContent!!
        assertEquals(structured, contentJson((result.content.first() as TextContent).text))
        assertEquals("not-an-id", structured.getValue("id").jsonPrimitive.content)
        assertTrue(structured.containsKey("error"))
        assertTrue("text" !in structured && "title" !in structured)
    }

    /** The advertised schemas are the other half of the contract ChatGPT reads. */
    @Test
    fun advertisedSchemasMatchTheContract() {
        val tools = org.chromia.tools.McpTools.allTools().associateBy { it.name }

        val search = tools.getValue("search")
        assertEquals(listOf("query"), search.inputSchema.required)
        assertTrue(search.inputSchema.propertiesOrEmpty.containsKey("query"))
        assertEquals(listOf("results"), search.outputSchema!!.required)

        val fetch = tools.getValue("fetch")
        assertEquals(listOf("id"), fetch.inputSchema.required)
        assertTrue(fetch.inputSchema.propertiesOrEmpty.containsKey("id"))
        assertEquals(listOf("id"), fetch.outputSchema!!.required)
        assertTrue(fetch.outputSchema!!.propertiesOrEmpty.keys.containsAll(setOf("id", "title", "text", "url")))
    }
}
