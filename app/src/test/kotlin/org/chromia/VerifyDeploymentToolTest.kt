package org.chromia

import org.chromia.tools.propertiesOrEmpty

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.put
import net.postchain.common.BlockchainRid
import net.postchain.common.toHex
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.NetworkResult
import org.chromia.domain.exceptions.NetworkConfigurationException
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.VerifyDeployment
import org.chromia.tools.VerifyDeploymentStrategy
import org.chromia.tools.WriteDeploymentConfig
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
            .executeTool(callToolRequest(name = "verify_deployment", arguments = args))
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
    fun registeredChainThatIsStillStartingIsNotCalledAWrongBrid() {
        // First real deploy (2026-09-04): ~5 minutes of "check the BRID and
        // network" for a chain the Directory already listed on node6-8.
        repo.nextHeight = NetworkResult.Error(
            "currentBlockHeight: 404 Not Found  Can't find blockchain with blockchainRID: $upperBrid from https://node8.testnet.chromia.com"
        )
        repo.next = NetworkResult.Success(buildJsonObject {
            put("data", buildJsonArray { add(JsonPrimitive("https://node6.testnet.chromia.com")); add(JsonPrimitive("https://node8.testnet.chromia.com")) })
        })
        val result = call(buildJsonObject { put("brid", hexBrid); put("network", "testnet"); put("waitMs", 0) })
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        assertFalse(s["live"]!!.jsonPrimitive.boolean)
        assertTrue(s["registered"]!!.jsonPrimitive.boolean, s.toString())
        assertEquals(listOf("https://node6.testnet.chromia.com", "https://node8.testnet.chromia.com"), s["hostedOn"]!!.jsonArray.map { it.jsonPrimitive.content })
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.startsWith("Chain is REGISTERED but not serving yet: the Directory chain lists it on https://node6"), notes)
        assertTrue(notes.contains("re-run verify_deployment in 1-2 minutes"), notes)
        assertFalse(notes.contains("check the BRID and network"), "a registered chain must not be diagnosed as a wrong BRID: $notes")
        // The question went to the testnet Directory chain, keyed by the raw BRID bytes.
        val dir = repo.lastDapp!!
        assertEquals(WriteDeploymentConfig.TESTNET_DIRECTORY_BRID, dir.brid.uppercase())
        assertEquals("cm_get_blockchain_api_urls", dir.query)
        assertEquals("testnet", dir.network)
        assertEquals(upperBrid, (dir.arguments["blockchain_rid"] as ByteArray).toHex().uppercase())

        // The Directory answering "no such chain" confirms the wrong-BRID reading.
        repo.next = NetworkResult.Success(buildJsonObject { put("data", buildJsonArray { }) })
        val unknown = call(buildJsonObject { put("brid", hexBrid); put("network", "testnet"); put("waitMs", 0) }).structuredContent!!
        assertFalse(unknown["registered"]!!.jsonPrimitive.boolean, unknown.toString())
        val unknownNotes = unknown["notes"]!!.jsonPrimitive.content
        assertTrue(unknownNotes.contains("check the BRID and network"), unknownNotes)
        assertTrue(unknownNotes.contains("The Directory chain lists no API URLs for this BRID on \"testnet\""), unknownNotes)

        // A raw node URL has no Directory to ask: the old hint, no query.
        repo.lastDapp = null
        val raw = call(buildJsonObject { put("brid", hexBrid); put("network", "https://node.example:7740"); put("waitMs", 0) }).structuredContent!!
        assertNull(raw["registered"], raw.toString())
        assertNull(repo.lastDapp, "no Directory chain is known for a raw node URL")
        assertTrue(VerifyDeployment.isUnknownChain("HTTP 404 Not Found"))
        assertFalse(VerifyDeployment.isUnknownChain("Connection refused"))
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

    // ---- overall deadline (D1, live probe 2026-09-02) ------------------------
    // A mainnet chain hosted in a non-system cluster left the probe running
    // past the hosting platform's 60s proxy write timeout: the agent got a
    // closed socket (UND_ERR_SOCKET) instead of an answer. The tool's TOTAL
    // work must stay bounded well under that ceiling.

    private fun callWithDeadline(deadlineMs: Long, args: kotlinx.serialization.json.JsonObject) =
        runBlocking {
            VerifyDeploymentStrategy(deadlineMs = deadlineMs)
                .execute(callToolRequest(name = "verify_deployment", arguments = args), repo)
        }

    @Test
    fun deadlineClampAndEnvParsingAreBoundedUnderTheProxyTimeout() {
        assertTrue(
            VerifyDeployment.MAX_DEADLINE_MS < 60_000L,
            "the deadline cap must stay well under the 60s proxy write timeout"
        )
        assertEquals(VerifyDeployment.DEFAULT_DEADLINE_MS, VerifyDeployment.clampDeadlineMs(null))
        assertEquals(VerifyDeployment.MIN_DEADLINE_MS, VerifyDeployment.clampDeadlineMs(0))
        assertEquals(VerifyDeployment.MAX_DEADLINE_MS, VerifyDeployment.clampDeadlineMs(999_999))
        assertEquals(5_000L, VerifyDeployment.clampDeadlineMs(5_000))

        assertEquals(VerifyDeployment.DEFAULT_DEADLINE_MS, VerifyDeployment.configuredDeadlineMs(null))
        assertEquals(VerifyDeployment.DEFAULT_DEADLINE_MS, VerifyDeployment.configuredDeadlineMs("soon"))
        assertEquals(VerifyDeployment.MAX_DEADLINE_MS, VerifyDeployment.configuredDeadlineMs("999999"))
        assertEquals(15_000L, VerifyDeployment.configuredDeadlineMs(" 15000 "))
    }

    @Test
    fun timeoutHintNamesClusterCauseAndNodeUrlEscapeHatch() {
        val hint = VerifyDeployment.timeoutHint("mainnet", 20_000)
        assertTrue(hint.contains("timed out"), hint)
        assertTrue(hint.contains("cluster"), hint)
        assertTrue(hint.contains("\"mainnet\""), hint)
        assertTrue(hint.contains("node URL as `network`"), hint)
    }

    @Test
    fun bogusBridAnswersAgreeOnTheActionableCore() {
        // A bogus BRID legitimately yields EITHER answer, depending on
        // upstream node health (live-verified 2026-09-02): a healthy node
        // 404s an unknown BRID in <1s, but postchain-client's TryNextOnError
        // only surfaces that 404 after crawling every pool endpoint, so with
        // any degraded endpoint the deadline fires first and the caller gets
        // the timeout hint. The e2e sweep accepts either - this test pins the
        // shared actionable core both must carry so tool and sweep cannot
        // drift apart: re-check the BRID, or verify via the dapp's own node
        // URL as `network`.
        val unknownChain = VerifyDeployment.failureHint(
            "Can't find blockchain with blockchainRID: ${"AB".repeat(32)}", "mainnet"
        )
        val timedOut = VerifyDeployment.timeoutHint("mainnet", 20_000)
        listOf(unknownChain, timedOut).forEach { hint ->
            assertTrue(hint.contains("check the BRID"), hint)
            assertTrue(hint.contains("node URL as `network`"), hint)
        }
        // And the sweep's tag line: the strategy prefixes failureHint answers
        // with "Height probe failed:" while the deadline path starts with
        // "Height probe timed out:" - the sweep greps for either prefix.
        assertTrue(timedOut.startsWith("Height probe timed out:"), timedOut)
    }

    @Test
    fun hangingFirstProbeReturnsBoundedTimedOutResultInsteadOfHanging() {
        repo.heightDelaysMs.add(60_000L) // signer discovery / height read that never answers in time
        repo.nextHeight = NetworkResult.Success(1L)
        val startNanos = System.nanoTime()
        val result = callWithDeadline(
            300,
            buildJsonObject { put("brid", hexBrid); put("network", "mainnet"); put("waitMs", 0) }
        )
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(elapsedMs < 10_000, "answered in ${elapsedMs}ms - must be bounded, never hang")
        assertTrue(result.isError != true, "a timed-out probe is a normal answer, not a tool error")
        val s = result.structuredContent!!
        assertFalse(s["live"]!!.jsonPrimitive.boolean)
        assertEquals(upperBrid, s["brid"]!!.jsonPrimitive.content)
        assertFalse(s["heightProgressing"]!!.jsonPrimitive.boolean)
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("timed out"), notes)
        assertTrue(notes.contains("cluster"), notes)
        assertTrue(notes.contains("node URL as `network`"), notes)
    }

    @Test
    fun deadlineIsEnforcedAcrossAttemptsNotPerAttempt() {
        // First probe answers instantly; the SECOND outlives what is left of
        // the shared budget - it must be abandoned, not given a fresh one.
        repo.heightDelaysMs.addAll(listOf(0L, 60_000L))
        repo.heightQueue.addAll(listOf(NetworkResult.Success(7L), NetworkResult.Success(9L)))
        val startNanos = System.nanoTime()
        val result = callWithDeadline(400, buildJsonObject { put("brid", hexBrid); put("waitMs", 0) })
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(elapsedMs < 10_000, "answered in ${elapsedMs}ms - must be bounded, never hang")
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean, "the first probe answered - the chain IS live")
        assertEquals(7L, s["blockHeight"]!!.jsonPrimitive.long)
        assertFalse(s["heightProgressing"]!!.jsonPrimitive.boolean)
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("Second height probe skipped"), notes)
        assertTrue(notes.contains("deadline"), notes)
    }

    @Test
    fun hangingSmokeQueryIsSkippedAtTheDeadlineChainStaysLive() {
        repo.heightQueue.addAll(listOf(NetworkResult.Success(1L), NetworkResult.Success(2L)))
        repo.dappDelayMs = 60_000L
        val result = callWithDeadline(
            400,
            buildJsonObject { put("brid", hexBrid); put("waitMs", 0); put("query", "hello_world") }
        )
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean)
        assertNull(s["queryResult"])
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("Smoke query 'hello_world' skipped"), notes)
        assertTrue(notes.contains("deadline"), notes)
    }

    @Test
    fun waitBetweenReadsIsClampedToTheRemainingDeadline() {
        var waited = -1L
        val strategy = VerifyDeploymentStrategy(delayFn = { waited = it }, deadlineMs = 500)
        repo.heightQueue.addAll(listOf(NetworkResult.Success(1L), NetworkResult.Success(2L)))
        runBlocking {
            strategy.execute(
                callToolRequest(
                    name = "verify_deployment",
                    arguments = buildJsonObject { put("brid", hexBrid); put("waitMs", 10_000) }
                ),
                repo
            )
        }
        assertTrue(waited in 0L..500L, "wait ${waited}ms must not exceed the remaining deadline")
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
            .forEach { assertNotNull(tool.inputSchema.propertiesOrEmpty[it], "inputSchema missing $it") }
        val out = tool.outputSchema!!
        listOf("live", "brid", "blockHeight", "heightProgressing", "queryResult", "notes")
            .forEach { assertNotNull(out.propertiesOrEmpty[it], "outputSchema missing $it") }
    }
}
