package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.RagStore
import org.chromia.tools.ToolExecutor
import org.chromia.tools.callToolRequest
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

/**
 * THE EXECUTOR ITSELF: dispatch, unknown names, and the shape of a failure.
 *
 * Every test here used to be handed a recording double of our own
 * `ChromiaRepository`. Most of them never wanted a repository at all - they are
 * about the executor's own behaviour and throw or answer before any call is made
 * - and the two that did want one were reading back a body the test had written
 * a line earlier. Both kinds are gone:
 *
 *  - the executor-only tests now take [McpTestSupport.offlineRepository], the
 *    production repository pointed at a closed loopback port, so "this never
 *    reaches the network" is enforced by the operating system rather than
 *    assumed;
 *  - the two that are genuinely about a repository answer now run against the
 *    real explorer through [LiveChromia].
 *
 * `get_network_stats` was this file's stock example tool. It was retired from
 * production on 2026-09-07 (the explorer answers INTERNAL_ERROR for
 * `dashboardData` on every selection set, docs/UPSTREAM.md #3a), so the tests
 * that only needed *a* tool now use one that still exists.
 */
class ToolExecutorTest {

    private val authSegment = TextSegment.from(
        "FT4 authentication uses auth descriptors and require_mandatory_flags on the main descriptor.",
        Metadata.from("file_name", "ft4-auth.md")
    )
    private val rellSegment = TextSegment.from(
        "Rell compiler pipeline is S_ then C_ passes then R_ then RR_ then Rt.",
        Metadata.from("file_name", "rell-compiler.md")
    )

    // A REAL RagStore over a two-segment index: real query(), real fetchById().
    // This used to override query(), so these executor tests never ran our own
    // retrieval at all (see TestDocsIndex).
    private val fixtureStore = TestDocsIndex.store(authSegment, rellSegment)

    /**
     * The production repository at a closed port unless a test says otherwise:
     * a tool that is supposed to answer without leaving the process gets a real
     * `ConnectException` if it ever does.
     */
    private fun executor(
        repo: ChromiaRepositoryImpl = McpTestSupport.offlineRepository()
    ): ToolExecutor {
        val unusedRag = CompletableDeferred(RagStore(loadFromRegistry = false))
        return ToolExecutor(repo, PromptManager(), unusedRag)
    }

    private fun ragExecutor(): ToolExecutor =
        ToolExecutor(McpTestSupport.offlineRepository(), PromptManager(), CompletableDeferred(fixtureStore))

    @Test
    fun executeToolUnknownNameReturnsErrorText() = runBlocking {
        val result = executor().executeTool(
            callToolRequest(
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

    /**
     * DISPATCH, PROVEN BY THE THIRD PARTY'S ANSWER.
     *
     * The claim is the executor's: a known tool name reaches that tool's
     * strategy and its answer comes back whole. It used to be checked by
     * pre-loading a recorder with `{"countAllAccounts": 9}` and finding a 9,
     * which is true of any dispatch table that returns what it was handed.
     * Here the number is the explorer's, and the tool is one that still exists.
     */
    @Test
    fun executeToolDispatchesAKnownToolToTheLiveExplorer() = runBlocking {
        LiveChromia.requireLive("dispatches get_total_rewards_paid through the executor to the live explorer")
        val result = executor(LiveChromia.repository()).executeTool(
            callToolRequest(
                name = "get_total_rewards_paid",
                arguments = buildJsonObject { put("network", LiveChromia.EXPLORER_NETWORK) }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        if (result.isError == true) {
            // The explorer refused. Dispatch still happened, and the proof is
            // that the message is the one THIS tool's strategy writes and no
            // other - an unknown name would have said "Unknown tool".
            assertTrue(
                text.startsWith("Failed to get total rewards paid:"),
                "the failure must come from the get_total_rewards_paid strategy: $text"
            )
            return@runBlocking
        }
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals(payload, result.structuredContent)
        val total = payload.getValue("data").jsonObject
            .getValue("totalRewardsPaid").jsonPrimitive.content
        assertTrue(total.all { it.isDigit() }, "the explorer answers a decimal string: $total")
        assertTrue(total.toBigInteger() > BigInteger.ZERO, "mainnet has paid rewards: $total")
    }

    /**
     * A REAL REFUSAL, THROUGH THE EXECUTOR.
     *
     * This used to pre-load a recorder with the string "explorer HTTP 503" and
     * then find it in the output, which proves string concatenation. The
     * explorer really does refuse `network=testnet` with an HTTP 400, every
     * time, and it is documented in docs/UPSTREAM.md #9 - so the error text,
     * the `isError` flag, the structured `error` and the upstream verdict are
     * all produced by a failure nobody in this repository wrote.
     */
    @Test
    fun executeToolExplorerRefusalSetsIsErrorAndKeepsTheExplorersWords() = runBlocking {
        LiveChromia.requireLive("takes the explorer's real HTTP 400 for network=testnet through the executor")
        val result = executor(LiveChromia.repository()).executeTool(
            callToolRequest(
                name = "get_all_assets",
                arguments = buildJsonObject { put("network", "testnet") }
            )
        )
        val text = (result.content.first() as TextContent).text!!
        assertEquals(
            true, result.isError,
            "the explorer refuses network=testnet; a refusal must not come back as a success: $result"
        )
        assertTrue(
            text.startsWith("Failed to get all assets:"),
            "the executor must keep the strategy's own framing: $text"
        )
        assertTrue(text.contains("400"), "the explorer's own status must survive: $text")
        assertNotNull(result.structuredContent)
        assertEquals(text, result.structuredContent!!["error"]!!.jsonPrimitive.content)
        assertEquals(
            "explorer_testnet_400",
            result.structuredContent!!["upstream_rule"]?.jsonPrimitive?.content,
            "a refusal the server can classify as upstream must say so inline: $text"
        )
    }

    @Test
    fun executeToolWrapsStrategyException() = runBlocking {
        val result = executor().executeTool(
            callToolRequest(
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
        // Retired 2026-09-07 because the explorer will not serve them; a tool
        // that advertises data nobody can fetch is the same fake as a recorded
        // body that pretends it can.
        assertTrue("get_network_stats" !in names)
        assertTrue("get_transactions_by_cluster" !in names)
        assertTrue("get_blockchains_transactions" !in names)
        assertTrue("get_node_unavailability" !in names)
    }

    @Test
    fun executeToolBlankRidIsMissingRequiredParameterAndIsError() = runBlocking {
        val result = executor().executeTool(
            callToolRequest(
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
            callToolRequest(
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
            callToolRequest(
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
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            )
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        val hits = structured["results"]!!.jsonArray
        // Two segments, and the real BGE-small embedder scores any two short English
        // sentences above the store's 0.6 retrieval floor, so a two-segment fixture
        // index returns both. The claim this test makes is which one LEADS.
        assertEquals(2, hits.size)
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
            callToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            )
        )
        assertTrue(docs.isError != true)
        val hits = docs.structuredContent!!["hits"]!!.jsonArray
        // Two segments, and the real BGE-small embedder scores any two short English
        // sentences above the store's 0.6 retrieval floor, so a two-segment fixture
        // index returns both. The claim this test makes is which one LEADS.
        assertEquals(2, hits.size)
        val id = hits.first().jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(segmentId(authSegment), id)
        assertEquals(authSegment.text(), hits.first().jsonObject["text"]!!.jsonPrimitive.content)
        assertTrue(docs.structuredContent!!["text"]!!.jsonPrimitive.content.contains(id))

        val fetch = executor.executeTool(
            callToolRequest(
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
            callToolRequest(
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
