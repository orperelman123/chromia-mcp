package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.put
import net.postchain.common.BlockchainRid
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.NetworkResult
import org.chromia.domain.exceptions.NetworkConfigurationException
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.VerifyDeployment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * verify_deployment: prove a deployment works with no keys - chain known on
 * the network, block height progression (bounded wait, never hangs), optional
 * dapp smoke query. Unit-level only: the repository fake and the
 * BlockchainHeightClient seam replace all network I/O.
 */
class VerifyDeploymentToolTest {

    private val hexBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val upperBrid = hexBrid.uppercase()

    // ---- parseBrid -----------------------------------------------------------

    @Test
    fun parseBridAcceptsBareHexAndRellAndPrefixForms() {
        assertEquals(upperBrid, VerifyDeployment.parseBrid(hexBrid))
        assertEquals(upperBrid, VerifyDeployment.parseBrid(upperBrid))
        assertEquals(upperBrid, VerifyDeployment.parseBrid("x\"$hexBrid\""))
        assertEquals(upperBrid, VerifyDeployment.parseBrid("X\"$upperBrid\""))
        assertEquals(upperBrid, VerifyDeployment.parseBrid("0x$hexBrid"))
        assertEquals(upperBrid, VerifyDeployment.parseBrid("  $hexBrid  "))
    }

    @Test
    fun parseBridRejectsWrongLengthAndNonHex() {
        listOf("abc", hexBrid.dropLast(1), hexBrid + "0", "z".repeat(64), "").forEach { bad ->
            val error = assertThrows(IllegalArgumentException::class.java) {
                VerifyDeployment.parseBrid(bad)
            }
            assertTrue(error.message!!.contains("64-character hex"), error.message)
            assertTrue(error.message!!.contains("chr deployment create"), error.message)
        }
    }

    // ---- failureHint ---------------------------------------------------------

    @Test
    fun unknownChainMessagesGetABridNetworkHint() {
        listOf(
            "Can't find blockchain with blockchainRID: $upperBrid",
            "HTTP 404 Not Found",
            "unknown blockchain rid"
        ).forEach { msg ->
            val hint = VerifyDeployment.failureHint(msg, "testnet")
            assertTrue(hint.contains("not on this network"), hint)
            assertTrue(hint.contains("check the BRID and network"), hint)
            // Regression (live probe 2026-09-02): a mainnet dapp chain hosted in a
            // non-system cluster (e.g. AllianceGames, cluster "pink") 404s on the
            // predefined system nodes - the hint must not claim the chain is off
            // the network, and must name the node-URL escape hatch.
            assertTrue(hint.contains("cluster"), hint)
            assertTrue(hint.contains("node URL as `network`"), hint)
        }
    }

    @Test
    fun unreachableMessagesGetANetworkHint() {
        listOf(
            "Connection refused: node0.testnet.chromia.com",
            "java.net.UnknownHostException: no-such-node",
            "connect timed out"
        ).forEach { msg ->
            val hint = VerifyDeployment.failureHint(msg, "testnet")
            assertTrue(hint.contains("could not be reached"), hint)
        }
    }

    @Test
    fun otherMessagesPointAtTranslateError() {
        val hint = VerifyDeployment.failureHint("something exploded", "mainnet")
        assertTrue(hint.contains("translate_error"), hint)
    }

    @Test
    fun waitClampIsBounded() {
        assertEquals(VerifyDeployment.DEFAULT_WAIT_MS, VerifyDeployment.clampWaitMs(null))
        assertEquals(0L, VerifyDeployment.clampWaitMs(-5))
        assertEquals(VerifyDeployment.MAX_WAIT_MS, VerifyDeployment.clampWaitMs(999_999))
        assertEquals(1234L, VerifyDeployment.clampWaitMs(1234))
    }

    // ---- strategy via executor ----------------------------------------------

    private val repo = RecordingRepository()

    private fun call(args: kotlinx.serialization.json.JsonObject) = runBlocking {
        ToolExecutor(repo, PromptManager())
            .executeTool(CallToolRequest(name = "verify_deployment", arguments = args))
    }

    @Test
    fun progressingChainIsLiveWithLatestHeight() {
        repo.heightQueue.addAll(listOf(NetworkResult.Success(41L), NetworkResult.Success(43L)))
        val result = call(
            buildJsonObject {
                put("brid", hexBrid)
                put("network", "testnet")
                put("waitMs", 0)
            }
        )
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean)
        assertEquals(upperBrid, s["brid"]!!.jsonPrimitive.content)
        assertEquals(43L, s["blockHeight"]!!.jsonPrimitive.long)
        assertTrue(s["heightProgressing"]!!.jsonPrimitive.boolean)
        assertEquals(2, repo.heightCalls)
        assertEquals("testnet", repo.lastHeightNetwork)
        assertEquals(upperBrid, repo.lastHeightBrid.orEmpty().uppercase())
        // No smoke query requested: none must run.
        assertNull(repo.lastDapp)
        assertNull(s["queryResult"])
    }

    @Test
    fun idleChainIsLiveButNotProgressingWithHonestNote() {
        repo.heightQueue.addAll(listOf(NetworkResult.Success(41L), NetworkResult.Success(41L)))
        val result = call(buildJsonObject { put("brid", hexBrid); put("waitMs", 0) })
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean)
        assertFalse(s["heightProgressing"]!!.jsonPrimitive.boolean)
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("idle"), notes)
        assertTrue(notes.contains("not a failure"), notes)
    }

    @Test
    fun unknownBridReportsNotOnThisNetwork() {
        repo.nextHeight = NetworkResult.Error("Can't find blockchain with blockchainRID: $upperBrid")
        val result = call(buildJsonObject { put("brid", hexBrid); put("waitMs", 0) })
        assertTrue(result.isError != true) // the verification completed; the answer is "not live"
        val s = result.structuredContent!!
        assertFalse(s["live"]!!.jsonPrimitive.boolean)
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("not on this network"), notes)
        assertTrue(notes.contains("check the BRID and network"), notes)
        assertEquals(1, repo.heightCalls, "no second probe after a failed first one")
    }

    @Test
    fun unreachableNodeReportsUpstreamHint() {
        repo.nextHeight = NetworkResult.Error("Connection refused: nope.example:7740")
        val result = call(
            buildJsonObject { put("brid", hexBrid); put("network", "https://nope.example:7740"); put("waitMs", 0) }
        )
        val s = result.structuredContent!!
        assertFalse(s["live"]!!.jsonPrimitive.boolean)
        assertTrue(s["notes"]!!.jsonPrimitive.content.contains("could not be reached"))
        assertEquals("https://nope.example:7740", repo.lastHeightNetwork)
    }

    @Test
    fun smokeQueryRunsThroughTheDappQueryPlumbing() {
        repo.heightQueue.addAll(listOf(NetworkResult.Success(1L), NetworkResult.Success(2L)))
        repo.next = NetworkResult.Success(buildJsonObject { put("greeting", "Hello World!") })
        val result = call(
            buildJsonObject {
                put("brid", hexBrid)
                put("waitMs", 0)
                put("query", "hello_world")
                put("arguments", buildJsonObject { put("name", "or") })
            }
        )
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean)
        assertEquals(
            "Hello World!",
            s["queryResult"]!!.jsonObject["greeting"]!!.jsonPrimitive.content
        )
        val dapp = repo.lastDapp!!
        assertEquals("hello_world", dapp.query)
        assertEquals(mapOf<String, Any?>("name" to "or"), dapp.arguments)
    }

    @Test
    fun failedSmokeQueryKeepsChainLiveAndExplains() {
        repo.heightQueue.addAll(listOf(NetworkResult.Success(1L), NetworkResult.Success(2L)))
        repo.next = NetworkResult.Error("Unknown query: no_such_query")
        val result = call(
            buildJsonObject { put("brid", hexBrid); put("waitMs", 0); put("query", "no_such_query") }
        )
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean)
        assertNull(s["queryResult"])
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("Smoke query 'no_such_query' failed"), notes)
        assertTrue(notes.contains("rell.get_app_structure"), notes)
    }

    @Test
    fun invalidBridAndWaitAreValidationErrors() {
        val badBrid = call(buildJsonObject { put("brid", "abc") })
        assertEquals(true, badBrid.isError)
        assertTrue((badBrid.content.first() as TextContent).text!!.contains("64-character hex"))

        val badWait = call(buildJsonObject { put("brid", hexBrid); put("waitMs", "soon") })
        assertEquals(true, badWait.isError)
        assertTrue((badWait.content.first() as TextContent).text!!.contains("waitMs"))

        val missing = call(buildJsonObject { })
        assertEquals(true, missing.isError)
        assertTrue(
            (missing.content.first() as TextContent).text!!.contains("Missing required parameter: brid")
        )
    }

    @Test
    fun networkDefaultsToTestnet() {
        repo.heightQueue.addAll(listOf(NetworkResult.Success(1L), NetworkResult.Success(2L)))
        call(buildJsonObject { put("brid", hexBrid); put("waitMs", 0) })
        assertEquals("testnet", repo.lastHeightNetwork)
    }

    // ---- PostchainClientService height plumbing ------------------------------

    private val rid = BlockchainRid.buildFromHex(hexBrid)

    @Test
    fun heightSeamReceivesResolvedPredefinedUrls() {
        var seen: List<String>? = null
        val service = PostchainClientService(
            ChromiaConfig(),
            clientFactory = null,
            heightClient = { urls, brid ->
                seen = urls
                assertEquals(upperBrid, brid.toHex().uppercase())
                77L
            }
        )
        val result = service.currentBlockHeight("testnet", rid)
        assertEquals(77L, (result as NetworkResult.Success).data)
        assertTrue(seen!!.any { it.contains("node0.testnet.chromia.com") }, seen.toString())
    }

    @Test
    fun customNodeUrlResolvesToSingleUrl() {
        var seen: List<String>? = null
        val service = PostchainClientService(
            ChromiaConfig(),
            clientFactory = null,
            heightClient = { urls, _ -> seen = urls; 5L }
        )
        service.currentBlockHeight("https://mynode.example:7740/", rid)
        assertEquals(listOf("https://mynode.example:7740"), seen)
    }

    @Test
    fun unknownNetworkNameIsConfigurationError() {
        val service = PostchainClientService(
            ChromiaConfig(),
            clientFactory = null,
            heightClient = { _, _ -> error("must not be called") }
        )
        val result = service.currentBlockHeight("not-a-real-network", rid)
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("not-a-real-network"))
        assertTrue(error.cause?.cause is NetworkConfigurationException)
    }

    @Test
    fun heightClientFailureBecomesErrorWithRawMessage() {
        val service = PostchainClientService(
            ChromiaConfig(),
            clientFactory = null,
            heightClient = { _, _ ->
                throw RuntimeException("Can't find blockchain with blockchainRID: $upperBrid")
            }
        )
        val result = service.currentBlockHeight("testnet", rid)
        assertTrue(result is NetworkResult.Error)
        val message = (result as NetworkResult.Error).message
        assertTrue(message.contains("Can't find blockchain"), message)
        // The raw message is exactly what VerifyDeployment.failureHint classifies.
        assertTrue(VerifyDeployment.failureHint(message, "testnet").contains("not on this network"))
    }

    // ---- schema + compact mode ----------------------------------------------

    @Test
    fun advertisedInFullAndCompactMode() {
        val full = McpTools.allTools(compact = false).map { it.name }
        val compact = McpTools.allTools(compact = true).map { it.name }
        assertTrue("verify_deployment" in full)
        assertTrue(
            "verify_deployment" in compact,
            "verify_deployment is cheap+high-value: compact mode must keep it"
        )
    }

    @Test
    fun toolSchemaDeclaresBridRequiredAndOutputShape() {
        val tool = McpTools.verifyDeploymentTool()
        assertEquals("verify_deployment", tool.name)
        assertEquals(listOf("brid"), tool.inputSchema.required)
        listOf("brid", "network", "query", "arguments", "waitMs")
            .forEach { assertNotNull(tool.inputSchema.properties[it], "inputSchema missing $it") }
        val out = tool.outputSchema!!
        listOf("live", "brid", "blockHeight", "heightProgressing", "queryResult", "notes")
            .forEach { assertNotNull(out.properties[it], "outputSchema missing $it") }
    }
}
