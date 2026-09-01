package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.domain.NetworkResult
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToolExecutorTest {

    private val authSegment = TextSegment.from(
        "FT4 authentication uses auth descriptors and require_mandatory_flags on the main descriptor.",
        Metadata.from("file_name", "ft4-auth.md")
    )
    private val rellSegment = TextSegment.from(
        "Rell compiler pipeline is S_ then C_ passes then R_ then RR_ then Rt.",
        Metadata.from("file_name", "rell-compiler.md")
    )

    private val fixtureStore = object : RagStore(loadFromRegistry = false) {
        override fun query(query: String): List<TextSegment>? {
            val hits = listOf(authSegment, rellSegment).filter { segment ->
                segment.text().contains(query, ignoreCase = true) ||
                    (segment.metadata()?.getString("file_name")?.contains(query, ignoreCase = true) == true)
            }
            return hits.ifEmpty { null }?.also { rememberQueryHits(it) }
        }
    }

    private fun executor(repo: RecordingRepository = RecordingRepository()): ToolExecutor {
        val unusedRag = CompletableDeferred(RagStore(loadFromRegistry = false))
        return ToolExecutor(repo, PromptManager(), unusedRag)
    }

    private fun ragExecutor(): ToolExecutor =
        ToolExecutor(RecordingRepository(), PromptManager(), CompletableDeferred(fixtureStore))

    @Test
    fun executeToolUnknownNameReturnsErrorText() = runBlocking {
        val result = executor().executeTool(
            CallToolRequest(
                name = "not_a_real_tool",
                arguments = buildJsonObject { put("network", "mainnet") }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals("Unknown tool: not_a_real_tool", text)
        assertEquals(true, result.isError)
        assertNotNull(result.structuredContent)
        assertEquals(text, result.structuredContent!!["error"]!!.jsonPrimitive.content)
    }

    @Test
    fun executeToolDispatchesKnownStrategy() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Success(
            buildJsonObject { put("countAllAccounts", 9) }
        )
        val result = executor(repo).executeTool(
            CallToolRequest(
                name = "get_network_stats",
                arguments = buildJsonObject { put("network", "testnet") }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals("testnet", repo.lastNetwork)
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("9", payload["countAllAccounts"]!!.jsonPrimitive.content)
        assertTrue(result.isError != true)
        assertEquals(payload, result.structuredContent)
        assertEquals("9", result.structuredContent!!["countAllAccounts"]!!.jsonPrimitive.content)
    }

    @Test
    fun executeToolRepositoryErrorSetsIsError() = runBlocking {
        val repo = RecordingRepository()
        repo.next = NetworkResult.Error("explorer HTTP 503")
        val result = executor(repo).executeTool(
            CallToolRequest(
                name = "get_network_stats",
                arguments = buildJsonObject { put("network", "mainnet") }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Failed to get network stats"))
        assertTrue(text.contains("explorer HTTP 503"))
        assertEquals(true, result.isError)
        assertNotNull(result.structuredContent)
        assertEquals(text, result.structuredContent!!["error"]!!.jsonPrimitive.content)
    }

    @Test
    fun executeToolWrapsStrategyException() = runBlocking {
        val result = executor().executeTool(
            CallToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject { put("query", "rell.get_app_structure") }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.startsWith("Tool execution failed:"))
        assertTrue(text.contains("blockchainRid"))
        assertEquals(true, result.isError)
        assertNotNull(result.structuredContent)
        assertEquals(text, result.structuredContent!!["error"]!!.jsonPrimitive.content)
        assertTrue(result.structuredContent!!["error"]!!.jsonPrimitive.content.contains("Missing required parameter"))
    }

    @Test
    fun registeredToolNamesMatchMcpTools() {
        val names = executor().registeredToolNames()
        assertEquals(McpTools.allTools().map { it.name }.toSet(), names)
        assertTrue("get_prompts" in names)
        assertTrue("chromia_dapp_query" in names)
        assertTrue("translate_error" in names)
        assertTrue("not_a_real_tool" !in names)
    }

    @Test
    fun executeToolBlankRidIsMissingRequiredParameterAndIsError() = runBlocking {
        val result = executor().executeTool(
            CallToolRequest(
                name = "get_blockchain_details",
                arguments = buildJsonObject {
                    put("rid", "   ")
                    put("network", "mainnet")
                }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Missing required parameter"))
        assertTrue(text.contains("rid"))
        assertEquals(true, result.isError)
    }

    @Test
    fun executeToolBlankAssetIdIsMissingRequiredParameterAndIsError() = runBlocking {
        val result = executor().executeTool(
            CallToolRequest(
                name = "get_asset_top_holders",
                arguments = buildJsonObject {
                    put("assetId", "")
                    put("network", "mainnet")
                }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Missing required parameter"))
        assertTrue(text.contains("assetId"))
        assertEquals(true, result.isError)
    }

    @Test
    fun executeToolWhitespaceAssetIdIsMissingRequiredParameterAndIsError() = runBlocking {
        val result = executor().executeTool(
            CallToolRequest(
                name = "get_asset_distribution",
                arguments = buildJsonObject {
                    put("assetId", " \t")
                    put("network", "mainnet")
                }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Missing required parameter"))
        assertTrue(text.contains("assetId"))
        assertEquals(true, result.isError)
    }

    @Test
    fun executeToolSearchReturnsStableIdsAndStructuredContent() = runBlocking {
        val result = ragExecutor().executeTool(
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            )
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val hits = structured["results"]!!.jsonArray
        assertEquals(1, hits.size)
        val hit = hits.first().jsonObject
        assertEquals(segmentId(authSegment), hit["id"]!!.jsonPrimitive.content)
        assertEquals("ft4-auth.md", hit["title"]!!.jsonPrimitive.content)
        assertTrue(hit["url"]!!.jsonPrimitive.content.contains("ft4-auth.md"))
        assertTrue("metadata" !in structured)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
    }

    @Test
    fun executeToolFetchDocsThenFetchHitsExactSegment() = runBlocking {
        val executor = ragExecutor()
        val docs = executor.executeTool(
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            )
        )
        assertTrue(docs.isError != true)
        val hits = docs.structuredContent!!["hits"]!!.jsonArray
        assertEquals(1, hits.size)
        val id = hits.first().jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(segmentId(authSegment), id)
        assertEquals(authSegment.text(), hits.first().jsonObject["text"]!!.jsonPrimitive.content)
        assertTrue(docs.structuredContent!!["text"]!!.jsonPrimitive.content.contains(id))

        val fetch = executor.executeTool(
            CallToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", id) }
            )
        )
        assertTrue(fetch.isError != true)
        val structured = fetch.structuredContent!!
        assertEquals(id, structured["id"]!!.jsonPrimitive.content)
        assertEquals("ft4-auth.md", structured["title"]!!.jsonPrimitive.content)
        assertTrue(structured["text"]!!.jsonPrimitive.content.contains("FT4 authentication"))
        assertTrue("error" !in structured)
        assertTrue("metadata" !in structured)
        val payload = Json.parseToJsonElement((fetch.content.first() as TextContent).text!!).jsonObject
        assertEquals(structured, payload)
    }

    @Test
    fun executeToolFetchUnknownIdReturnsErrorShape() = runBlocking {
        val result = ragExecutor().executeTool(
            CallToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "missing-doc") }
            )
        )
        assertEquals(true, result.isError)
        val structured = result.structuredContent!!
        assertEquals("missing-doc", structured["id"]!!.jsonPrimitive.content)
        assertTrue(structured["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertTrue("title" !in structured)
        assertTrue("text" !in structured)
        assertTrue("url" !in structured)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(structured, payload)
    }
}
