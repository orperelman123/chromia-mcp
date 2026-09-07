package org.chromia

import org.chromia.tools.propertiesOrEmpty

import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.postchain.common.BlockchainRid
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.ChromiaRepository
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
 * dapp smoke query.
 *
 * ZERO DOUBLES (2026-09-07). Every behavioural test here used to be driven by
 * `RecordingRepository`, a double of our own `ChromiaRepository`: the test told
 * it what the chain's height was, how long the probe took, and what the dapp
 * query answered, and then asked verify_deployment what it had concluded. That
 * is the tool restating the test. The recorder is deleted; what stands in its
 * place is one of three REAL things, chosen per test and said in the test:
 *
 *  - **the live testnet**, through the production repository
 *    ([LiveChromia.repository]) - the Economy Chain for the live path, and
 *    [LiveChromia.unknownChainRid] for the chain-not-found path, whose wording
 *    comes from the node rather than from us;
 *  - **a real closed port** ([McpTestSupport.offlineRepository]) for the
 *    validation errors raised before any probe, and for a node that genuinely
 *    is not there - the failure is the operating system's;
 *  - **a real budget too small for a real probe** for the deadline paths. No
 *    latency is injected anywhere: a 100ms deadline against the live network,
 *    or a wait longer than the whole budget, is exactly what an agent on a slow
 *    link sees, and it is the tool's own clock that decides.
 *
 * The live tests share ONE repository per class (see the companion below): the
 * first contact with a node builds the postchain client, which does signer
 * discovery over the network and costs seconds; the deadline tests deliberately
 * build their own COLD repository so that real cost lands inside the budget
 * they are testing.
 */
class VerifyDeploymentToolTest {

    private val hexBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    private val upperBrid = hexBrid.uppercase()

    private companion object {
        /**
         * ONE production repository for the whole class, warmed once.
         *
         * Warming is a real height read of the real Economy Chain: it builds the
         * cached postchain client (signer discovery over the network, measured
         * 1.4-4.7s cold on 2026-09-07) so that a later test with a 4s budget is
         * measuring the tool's deadline arithmetic rather than the JVM's first
         * TLS handshake. Tests that WANT the cold cost inside their budget build
         * their own repository and say so.
         */
        val liveRepository by lazy {
            LiveChromia.repository().also { repository ->
                runBlocking {
                    repository.getBlockchainHeight(LiveChromia.NETWORK, LiveChromia.economyChainRid)
                }
            }
        }
    }

    private fun verify(repository: ChromiaRepository, args: JsonObject) = runBlocking {
        ToolExecutor(repository, PromptManager())
            .executeTool(callToolRequest(name = "verify_deployment", arguments = args))
    }

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
    //
    // These four are pure functions of a string, so the string is the input, not
    // a stand-in for a collaborator - there is nothing here to double. The one
    // thing a typed-out message cannot prove is that the classifier still
    // matches what the NODE says; that is
    // [aBridThatIsNotOnTheNetworkProducesTheMessageFailureHintClassifies],
    // which takes the wording from the live testnet.

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

    // ---- the tool against the live testnet -----------------------------------

    /**
     * THE LIVE PATH, END TO END.
     *
     * Replaces three recorder-driven tests at once:
     *
     *   progressingChainIsLiveWithLatestHeight - the live half of it (live:true,
     *       the canonical upper-case BRID, a real height, and no smoke query
     *       unless one was asked for). Its OTHER half is deleted below.
     *   idleChainIsLiveButNotProgressingWithHonestNote - the idle note, which is
     *       what the real Economy Chain actually produces: measured 2026-09-07,
     *       its height did not move in 20 seconds, because a dapp chain builds
     *       blocks from transactions, not from a timer.
     *   networkDefaultsToTestnet - asserted where it can fail, in the answer:
     *       no `network` was passed and the chain was found on "testnet".
     *
     * Both height answers are legitimate for a real chain, so both are accepted
     * and each one is checked for the honest note that belongs to it. Only one
     * of them can be produced on demand, which is the subject of the deletion
     * note below.
     */
    @Test
    fun liveEconomyChainIsLiveIdleAndDefaultsToTestnet() {
        LiveChromia.requireLive("verifies the live testnet Economy Chain through verify_deployment")
        val result = verify(
            liveRepository,
            buildJsonObject { put("brid", LiveChromia.ECONOMY_CHAIN_BRID_HEX); put("waitMs", 0) }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean, s.toString())
        assertEquals(LiveChromia.ECONOMY_CHAIN_BRID_HEX.uppercase(), s["brid"]!!.jsonPrimitive.content)
        assertTrue(s["blockHeight"]!!.jsonPrimitive.long > 0L, "a live chain has blocks: $s")
        // No smoke query requested: none must run.
        assertNull(s["queryResult"], s.toString())
        val notes = s["notes"]!!.jsonPrimitive.content
        // No `network` was passed - the default carried it to testnet, where the
        // Economy Chain lives, and the answer says so.
        assertTrue(notes.contains("known on \"testnet\""), notes)
        if (s["heightProgressing"]!!.jsonPrimitive.boolean) {
            // A transaction landed between the two reads - rare on testnet, and
            // still a correct answer: the reported height is the later read.
            assertTrue(notes.contains("known on \"testnet\" at height"), notes)
        } else {
            assertTrue(notes.contains("idle"), notes)
            assertTrue(notes.contains("not a failure"), notes)
        }
    }

    // DELETED 2026-09-07 (zero-doubles): progressingChainIsLiveWithLatestHeight
    // asserted the heightProgressing:true branch - that a height which ADVANCED
    // between the two reads is reported as progressing, with the SECOND (later)
    // reading as blockHeight. It did that by queueing 41 then 43 on the recorder.
    //
    // No real input can produce it inside the tool's own limits: the wait between
    // the two reads is capped at VerifyDeployment.MAX_WAIT_MS (10s), and no chain
    // this suite can reach builds a block that often. Measured 2026-09-07 through
    // the production repository: the testnet Economy Chain and the testnet
    // Directory chain did not advance at all over 20s of polling, and the mainnet
    // Directory chain advanced once in ~30s. A local Postchain node cannot stand
    // in either - the production repository reaches every chain through
    // StandardChromiaClient, which resolves the chain via a Directory chain a
    // standalone node does not have (verified: every read against a local
    // local_chain_up node answers "404 Not Found"), so verify_deployment cannot
    // be pointed at one at all.
    //
    // Now unverified: the `progressing = secondHeight > firstHeight` branch of
    // VerifyDeploymentStrategy - heightProgressing:true, and blockHeight
    // reporting the LATER of the two readings. The live test above covers the
    // idle branch and would catch the two reads collapsing into one; the
    // e2e sweep against a chain with real traffic is where the progressing
    // branch could still be pinned.

    /**
     * THE CHAIN-NOT-FOUND PATH, ANSWERED BY THE NETWORK.
     *
     * Replaces unknownBridReportsNotOnThisNetwork and the unknown half of
     * registeredChainThatIsStillStartingIsNotCalledAWrongBrid, both of which
     * used to type out the node's sentence and hand back an empty `data` array
     * as the Directory's answer. Here the node says whatever it says today
     * (2026-09-07: "Unknown blockchain 0x0123...") and the live testnet
     * Directory chain is really asked whether it knows the BRID - it answers
     * `{"data":[]}`, which is what turns a wrong-BRID diagnosis from a guess
     * into a verdict.
     *
     * A BRID nothing serves legitimately produces EITHER the unknown-chain
     * answer or the deadline answer (see bogusBridAnswersAgreeOnTheActionableCore
     * for why); both must carry the same actionable core, and only the first can
     * carry a Directory verdict.
     */
    @Test
    fun liveUnknownBridIsToldApartFromARegisteredChainByTheDirectory() {
        LiveChromia.requireLive("asks the live testnet Directory chain about an undeployed BRID")
        val result = verify(
            liveRepository,
            buildJsonObject {
                put("brid", LiveChromia.unknownChainRid.toHex())
                put("network", LiveChromia.NETWORK)
                put("waitMs", 0)
            }
        )
        assertTrue(result.isError != true, "the verification completed; the answer is \"not live\"")
        val s = result.structuredContent!!
        assertFalse(s["live"]!!.jsonPrimitive.boolean, s.toString())
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("check the BRID"), notes)
        assertTrue(notes.contains("node URL as `network`"), notes)
        if (!notes.contains("timed out")) {
            assertTrue(notes.contains("not on this network"), notes)
            val registered = s["registered"]
            assertNotNull(
                registered,
                "an unknown-chain answer on a NAMED network must be taken to the Directory chain, " +
                    "and its verdict reported: $s"
            )
            assertFalse(
                registered!!.jsonPrimitive.boolean,
                "the Directory answered and lists no API URLs for this BRID: $s"
            )
            assertTrue(
                notes.contains("The Directory chain lists no API URLs for this BRID on \"testnet\""),
                notes
            )
        }
        assertTrue(VerifyDeployment.isUnknownChain("HTTP 404 Not Found"))
        assertFalse(VerifyDeployment.isUnknownChain("Connection refused"))
    }

    // DELETED 2026-09-07 (zero-doubles): the REGISTERED-but-not-serving half of
    // registeredChainThatIsStillStartingIsNotCalledAWrongBrid asserted that when
    // the node answers "unknown blockchain" for a BRID the Directory DOES list,
    // the tool says "Chain is REGISTERED but not serving yet ... re-run
    // verify_deployment in 1-2 minutes" instead of "check the BRID and network".
    // The recorder produced that by pairing a typed-out 404 with a typed-out list
    // of two node URLs.
    //
    // No real input can produce it: it needs a chain that is in the Directory and
    // simultaneously not yet answering on its nodes - the ~5 minute window right
    // after `chr deployment create`, which no test can conjure and no existing
    // chain sits in. Probing the live testnet confirms the two halves separately
    // (2026-09-07): the Directory answers three API URLs for the Economy Chain,
    // and those nodes serve it, so the pair cannot be made to disagree.
    //
    // Now unverified: VerifyDeployment.startingHint and the `hosts != null &&
    // hosts.isNotEmpty()` branch of VerifyDeploymentStrategy (registered:true,
    // hostedOn, the "re-run in 1-2 minutes" wording). The Directory IS really
    // asked in the live test above, and its negative answer is verified there;
    // only the positive-answer branch is uncovered. It stays reachable through a
    // real deployment - the deploy journey that produced it is the place to
    // re-pin it.

    /**
     * A RAW NODE URL HAS NO DIRECTORY TO ASK.
     *
     * The Directory question only exists for the named networks, so a `network`
     * that is a node URL must produce no `registered` verdict at all - the tool
     * must not invent one. Live, with the unknown BRID: the node answers
     * "Unknown blockchain", which is exactly the answer that WOULD trigger the
     * Directory question on "testnet".
     */
    @Test
    fun liveRawNodeUrlGetsNoRegisteredVerdictBecauseThereIsNoDirectoryToAsk() {
        LiveChromia.requireLive("verifies an undeployed BRID against a raw testnet node URL")
        val result = verify(
            liveRepository,
            buildJsonObject {
                put("brid", LiveChromia.unknownChainRid.toHex())
                put("network", WriteDeploymentConfig.TESTNET_URL)
                put("waitMs", 0)
            }
        )
        val s = result.structuredContent!!
        assertFalse(s["live"]!!.jsonPrimitive.boolean, s.toString())
        assertNull(s["registered"], "no Directory chain is known for a raw node URL: $s")
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("node URL as `network`"), notes)
        assertFalse(
            notes.contains("The Directory chain lists"),
            "nothing was asked, so nothing may be reported: $notes"
        )
    }

    /**
     * A NODE THAT REALLY IS NOT THERE.
     *
     * The offline repository is the production one pointed at 127.0.0.1:1, so
     * this is a real socket refused by the operating system. Its wording is the
     * client's ("503 Client Error: Connection Refused ... Connection refused:
     * getsockopt"), and failureHint has to classify THAT, not a sentence a test
     * wrote. It must also not mistake it for an unknown chain: nothing is asked
     * of the Directory, so there is no `registered` verdict.
     */
    @Test
    fun offlineNodeIsReportedAsUnreachableWithNoDirectoryVerdict() {
        val result = verify(
            McpTestSupport.offlineRepository(),
            buildJsonObject { put("brid", hexBrid); put("waitMs", 0) }
        )
        val s = result.structuredContent!!
        assertFalse(s["live"]!!.jsonPrimitive.boolean, s.toString())
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("could not be reached"), notes)
        assertNull(s["registered"], "a refused connection is not a Directory question: $s")
    }

    /**
     * THE SMOKE QUERY, AND ITS ARGUMENTS, BOUND BY THE CHAIN.
     *
     * Replaces smokeQueryRunsThroughTheDappQueryPlumbing, which asserted that
     * the recorder had been handed `mapOf("name" to "or")` - a restatement of
     * the call the test had just made.
     *
     * Here the first verification asks the live Economy Chain for its CHR asset
     * and the second passes that asset's symbol back as a nested filter
     * argument. Rell BINDS the filter: a symbol that did not arrive would come
     * back as every asset (or as a query that cannot bind at all), so exactly
     * one row carrying that symbol is a statement only the chain can make.
     */
    @Test
    fun liveSmokeQueryAndItsArgumentsAreBoundByTheChain() {
        LiveChromia.requireLive("runs verify_deployment's smoke query against the live Economy Chain")
        val asset = verify(
            liveRepository,
            buildJsonObject {
                put("brid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                put("network", LiveChromia.NETWORK)
                put("waitMs", 0)
                put("query", "get_chr_asset")
            }
        )
        assertTrue(asset.isError != true, (asset.content.first() as TextContent).text)
        val assetJson = asset.structuredContent!!
        assertTrue(assetJson["live"]!!.jsonPrimitive.boolean, assetJson.toString())
        val symbol = assetJson["queryResult"]!!.jsonObject["symbol"]!!.jsonPrimitive.content
        assertTrue(symbol.isNotBlank(), assetJson.toString())

        val filtered = verify(
            liveRepository,
            buildJsonObject {
                put("brid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                put("network", LiveChromia.NETWORK)
                put("waitMs", 0)
                put("query", "ft4.get_assets_filtered")
                put(
                    "arguments",
                    buildJsonObject {
                        put(
                            "asset_filter",
                            buildJsonObject {
                                put("ids", JsonNull)
                                put("name", JsonNull)
                                put("symbol", symbol)
                                put("type", JsonNull)
                            }
                        )
                        put("page_size", 2)
                        put("page_cursor", JsonNull)
                    }
                )
            }
        )
        assertTrue(filtered.isError != true, (filtered.content.first() as TextContent).text)
        val rows = filtered.structuredContent!!["queryResult"]!!.jsonObject["data"]!!.jsonArray
        assertEquals(
            1, rows.size,
            "the nested symbol filter had to reach the chain and bind for exactly one asset to " +
                "come back: ${filtered.structuredContent}"
        )
        assertEquals(symbol, rows[0].jsonObject["symbol"]!!.jsonPrimitive.content)
    }

    /**
     * A smoke query the chain refuses does not make the chain not-live. The
     * refusal is the node's own ("Unknown query: no_such_query", 2026-09-07).
     */
    @Test
    fun liveFailedSmokeQueryKeepsTheChainLiveAndExplains() {
        LiveChromia.requireLive("sends a query the live Economy Chain does not have")
        val result = verify(
            liveRepository,
            buildJsonObject {
                put("brid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                put("network", LiveChromia.NETWORK)
                put("waitMs", 0)
                put("query", "no_such_query")
            }
        )
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean, s.toString())
        assertNull(s["queryResult"], s.toString())
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("Smoke query 'no_such_query' failed"), notes)
        assertTrue(notes.contains("rell.get_app_structure"), notes)
    }

    /** Validation refusals happen before any probe - the closed port proves it. */
    @Test
    fun invalidBridAndWaitAreValidationErrors() {
        val offline = McpTestSupport.offlineRepository()
        val badBrid = verify(offline, buildJsonObject { put("brid", "abc") })
        assertEquals(true, badBrid.isError)
        assertTrue((badBrid.content.first() as TextContent).text!!.contains("64-character hex"))

        val badWait = verify(offline, buildJsonObject { put("brid", hexBrid); put("waitMs", "soon") })
        assertEquals(true, badWait.isError)
        assertTrue((badWait.content.first() as TextContent).text!!.contains("waitMs"))

        val missing = verify(offline, buildJsonObject { })
        assertEquals(true, missing.isError)
        assertTrue(
            (missing.content.first() as TextContent).text!!.contains("Missing required parameter: brid")
        )
    }

    // ---- overall deadline (D1, live probe 2026-09-02) ------------------------
    // A mainnet chain hosted in a non-system cluster left the probe running
    // past the hosting platform's 60s proxy write timeout: the agent got a
    // closed socket (UND_ERR_SOCKET) instead of an answer. The tool's TOTAL
    // work must stay bounded well under that ceiling.

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

    /**
     * A REAL PROBE THAT REALLY RUNS OUT OF TIME.
     *
     * Replaces hangingFirstProbeReturnsBoundedTimedOutResultInsteadOfHanging,
     * which put a 60s sleep in the recorder. Nothing is slowed down here: the
     * budget is [VerifyDeployment.MIN_DEADLINE_MS] (100ms) and the repository is
     * built COLD inside the test, so the probe has to construct a postchain
     * client - signer discovery over the real network, measured 1.4-4.7s on
     * 2026-09-07 - before it can read anything. It cannot finish, and the tool
     * must answer anyway, bounded, with the actionable hint rather than a hang.
     */
    @Test
    fun liveProbeThatCannotFinishInsideARealBudgetTimesOutBounded() {
        LiveChromia.requireLive("gives a real live probe a budget too small to answer in")
        val startNanos = System.nanoTime()
        val result = runBlocking {
            VerifyDeploymentStrategy(deadlineMs = VerifyDeployment.MIN_DEADLINE_MS).execute(
                callToolRequest(
                    name = "verify_deployment",
                    arguments = buildJsonObject {
                        put("brid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                        put("network", "mainnet")
                        put("waitMs", 0)
                    }
                ),
                // COLD on purpose: the client construction the deadline has to
                // survive is exactly what a first call from an agent pays.
                LiveChromia.repository()
            )
        }
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(elapsedMs < 10_000, "answered in ${elapsedMs}ms - must be bounded, never hang")
        assertTrue(result.isError != true, "a timed-out probe is a normal answer, not a tool error")
        val s = result.structuredContent!!
        assertFalse(s["live"]!!.jsonPrimitive.boolean, s.toString())
        assertEquals(LiveChromia.ECONOMY_CHAIN_BRID_HEX.uppercase(), s["brid"]!!.jsonPrimitive.content)
        assertFalse(s["heightProgressing"]!!.jsonPrimitive.boolean)
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("timed out"), notes)
        assertTrue(notes.contains("cluster"), notes)
        assertTrue(notes.contains("node URL as `network`"), notes)
    }

    /**
     * ONE BUDGET FOR THE WHOLE VERIFICATION, SPENT BY REAL WORK.
     *
     * Replaces three recorder-driven tests, all of which worked by injecting a
     * 60s latency at a chosen moment:
     *
     *   deadlineIsEnforcedAcrossAttemptsNotPerAttempt - the second probe gets
     *       what is LEFT of the shared budget, not a fresh one;
     *   hangingSmokeQueryIsSkippedAtTheDeadlineChainStaysLive - and neither does
     *       the smoke query, and the chain stays live either way;
     *   waitBetweenReadsIsClampedToTheRemainingDeadline - which captured the
     *       wait through a delayFn seam. It is asserted here by the clock
     *       instead: a 10s wait inside a 4s budget cannot take 10s.
     *
     * The only real ingredient is the budget. The first probe answers (the chain
     * is live), the real wait then eats what remains, and the second probe and
     * the smoke query have nothing left - a genuinely exhausted deadline,
     * measured by the tool's own clock.
     */
    @Test
    fun liveSpentBudgetSkipsTheSecondProbeAndTheSmokeQueryAndClampsTheWait() {
        LiveChromia.requireLive("spends a real 4s budget against the live Economy Chain")
        val startNanos = System.nanoTime()
        val result = runBlocking {
            VerifyDeploymentStrategy(deadlineMs = 4_000).execute(
                callToolRequest(
                    name = "verify_deployment",
                    arguments = buildJsonObject {
                        put("brid", LiveChromia.ECONOMY_CHAIN_BRID_HEX)
                        put("network", LiveChromia.NETWORK)
                        // Longer than the whole budget: it MUST be clamped.
                        put("waitMs", VerifyDeployment.MAX_WAIT_MS)
                        put("query", "get_chr_asset")
                    }
                ),
                liveRepository
            )
        }
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
        assertTrue(
            elapsedMs < VerifyDeployment.MAX_WAIT_MS,
            "answered in ${elapsedMs}ms - a ${VerifyDeployment.MAX_WAIT_MS}ms wait inside a 4000ms " +
                "budget must be clamped to what is left of the budget"
        )
        val s = result.structuredContent!!
        assertTrue(s["live"]!!.jsonPrimitive.boolean, "the first probe answered - the chain IS live: $s")
        assertTrue(s["blockHeight"]!!.jsonPrimitive.long > 0L, s.toString())
        assertFalse(s["heightProgressing"]!!.jsonPrimitive.boolean, s.toString())
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("Second height probe skipped"), notes)
        assertTrue(notes.contains("deadline"), notes)
        assertNull(s["queryResult"], s.toString())
        assertTrue(notes.contains("Smoke query 'get_chr_asset' skipped"), notes)
    }

    // ---- PostchainClientService height plumbing ------------------------------

    private val rid = BlockchainRid.buildFromHex(hexBrid)

    /**
     * URL RESOLUTION, ASKED DIRECTLY.
     *
     * These two used to inject a `heightClient` lambda whose only job was to
     * capture the URL list it was handed. That is a double built to observe a
     * pure function - and `resolveUrls` IS a pure function, visible to the tests
     * because it is `internal` in the same module. Asking it is both simpler and
     * a stronger statement: the seam version would have kept passing if
     * `currentBlockHeight` stopped calling `resolveUrls` at all.
     */
    @Test
    fun aPredefinedNetworkNameResolvesToItsNodeUrls() {
        val urls = PostchainClientService(ChromiaConfig()).resolveUrls("testnet")
        assertTrue(
            urls.any { it.contains("node0.testnet.chromia.com") },
            "testnet must resolve to the configured testnet nodes: $urls"
        )
        assertTrue(urls.all { it.startsWith("https://") }, urls.toString())
    }

    @Test
    fun customNodeUrlResolvesToSingleUrl() {
        val service = PostchainClientService(ChromiaConfig())
        assertEquals(
            listOf("https://mynode.example:7740"),
            service.resolveUrls("https://mynode.example:7740/"),
            "a direct node URL is its own one-element pool, with the trailing slash trimmed"
        )
    }

    /**
     * An unknown network name never reaches the network at all, so there is
     * nothing to substitute: a plain production service refuses it before it
     * would have built a client.
     */
    @Test
    fun unknownNetworkNameIsConfigurationError() {
        val service = PostchainClientService(ChromiaConfig())
        val result = service.currentBlockHeight("not-a-real-network", rid)
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("not-a-real-network"))
        assertTrue(error.cause?.cause is NetworkConfigurationException)
    }

    /**
     * THE FAILURE MESSAGE THAT FEEDS failureHint, TAKEN FROM THE REAL NETWORK.
     *
     * The seam version threw `RuntimeException("Can't find blockchain with
     * blockchainRID: ...")` - a sentence the test had typed out - and then
     * asserted that `failureHint` classifies that sentence. So the classifier
     * was checked against a copy of the node's wording rather than the wording,
     * and the day the node rephrased it, nothing here would have moved.
     *
     * A BRID of the right shape that is not deployed on testnet is a real
     * question the real network answers, and its answer is the input
     * `VerifyDeployment.failureHint` has to classify.
     */
    @Test
    fun aBridThatIsNotOnTheNetworkProducesTheMessageFailureHintClassifies() {
        LiveChromia.requireLive("asks the live testnet for the height of a chain that is not deployed there")
        val result = PostchainClientService(ChromiaConfig())
            .currentBlockHeight("testnet", LiveChromia.unknownChainRid)
        assertTrue(result is NetworkResult.Error, "an undeployed BRID cannot have a height: $result")
        val message = (result as NetworkResult.Error).message
        val hint = VerifyDeployment.failureHint(message, "testnet")
        assertTrue(
            hint.contains("not on this network") || hint.isNotBlank(),
            "the node said \"$message\" and failureHint had nothing to say about it. That is the " +
                "classifier drifting from the node's real wording - the exact drift a typed-out " +
                "RuntimeException could never show."
        )
        assertTrue(
            hint.contains("not on this network"),
            "expected the not-on-this-network classification for an undeployed BRID. Node said: " +
                "\"$message\"; hint was: \"$hint\". If the node has rephrased, re-point failureHint " +
                "at the new wording - do not re-point this test at a fixture."
        )
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
