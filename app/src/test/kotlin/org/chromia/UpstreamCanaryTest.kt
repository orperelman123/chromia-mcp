package org.chromia

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.config.ChromiaConfig
import org.chromia.tools.FilterBlockchainsStrategy
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * THE CANARY, and the allowlist it is judged against.
 *
 * [LiveEnv.explorerCanary] is the one thing standing between "a live test failed
 * with an upstream-looking message" and "the third party is down". Round 18
 * section 4 is what it costs to skip that step: `get_asset_top_holders` answered
 * eight consecutive live `INTERNAL_ERROR`s, the marker was read as an outage,
 * and eight passes were reported for calls that answered nothing.
 *
 * Everything asserted here is measured against the real explorer or against the
 * real text a real failing call produced. There is no invented error string in
 * this file: the positive signature comes from a live `allBlockchains(state:)`
 * refusal, and the negative comes from a real closed port refusing a real
 * socket.
 *
 * Since adversary round 19 the canary is also INDEPENDENT, and that is asserted
 * here too. It used to run through `HttpClientService(ChromiaConfig())` - the
 * same client, config and bounds as the tools - so a fault of ours satisfied the
 * signature guard and the canary guard at the same moment. It is now a plain
 * `java.net.http` client with its own bounds; only the endpoint is shared,
 * because the endpoint is the question.
 */
class UpstreamCanaryTest {

    @Test
    fun theCanaryMeasuresTheExplorerOnceAndRecordsWhatItSaw() {
        LiveChromia.requireLive(
            "asks the live explorer whether it is up at all, on the canary's own independent path"
        )

        val canary = LiveEnv.explorerCanary()
        assertSame(
            canary, LiveEnv.explorerCanary(),
            "the canary must be ONE measurement per JVM. A canary re-probed per failing assertion " +
                "would hammer a service that is already refusing us, and different tests in the same " +
                "run could then disagree about whether the explorer was up."
        )

        assertEquals(
            ChromiaConfig().explorerUrl, canary.explorerUrl,
            "the canary must measure the SAME endpoint the tools call, or it is evidence about a " +
                "different service than the one that failed"
        )
        // ...and NOTHING ELSE may be shared. Round 19 section 4: the canary used
        // to build ChromiaConfig() and HttpClientService(config), so our own
        // HttpTimeouts.requestTimeout expiring produced ktor's `Request timeout
        // has expired` - an allowlisted signature - for the tool AND for the
        // canary, and one fault of ours satisfied both guardrails at once. The
        // endpoint is the question; everything that carries the question is the
        // canary's own.
        assertEquals(
            "true", LiveEnv.canaryJson(canary)["independent"].toString(),
            "the canary's evidence must record that it was measured on the independent path - the " +
                "gate refuses a warning whose canary does not say so"
        )
        assertTrue(
            LiveEnv.CANARY_REQUEST_TIMEOUT.toMillis() !=
                ChromiaConfig().httpTimeouts.requestTimeout.inWholeMilliseconds,
            "the canary's request bound must not be ChromiaConfig.httpTimeouts.requestTimeout. " +
                "Sharing that field is how tightening OUR timeout made the canary fail too, which " +
                "is what turned our own bug into a proven ChromaWay outage."
        )
        assertTrue(canary.elapsedMs > 0, "an unmeasured canary: ${canary.summary()}")
        assertTrue(canary.explorerSaid.isNotBlank(), "the canary must carry the explorer's own words")
        assertTrue(canary.at.startsWith("20"), "the canary must be dated: ${canary.at}")

        when (canary.state) {
            LiveEnv.CanaryState.ANSWERED -> {
                assertTrue(canary.answered)
                assertNull(canary.signature, "an answering canary has no failure signature")
                assertTrue(
                    canary.explorerSaid.contains("totalRewardsPaid ="),
                    "an answering canary carries the value it read: ${canary.summary()}"
                )
            }

            LiveEnv.CanaryState.FAILED_SIGNATURE -> {
                assertNotNull(canary.signature, "a signature failure must name the signature")
                assertEquals(
                    canary.signature, LiveEnv.upstreamSignature(canary.explorerSaid),
                    "the recorded signature must be the one the allowlist actually matches"
                )
            }

            LiveEnv.CanaryState.FAILED_OTHER -> assertNull(
                canary.signature,
                "an unsignatured failure must not be dressed in one: ${canary.summary()}"
            )
        }

        // The file is the evidence a reader has an hour later, when the explorer
        // has recovered and the red run cannot be re-measured.
        val file = LiveEnv.upstreamDir.resolve("canary.json")
        assertTrue(Files.exists(file), "the canary must record itself at $file")
        val recorded = Json.parseToJsonElement(Files.readString(file)).jsonObject
        assertEquals("totalRewardsPaid", recorded.getValue("query").jsonPrimitive.content)
        assertEquals(ChromiaConfig().explorerUrl, recorded.getValue("explorerUrl").jsonPrimitive.content)
        assertEquals(canary.state.name, recorded.getValue("outcome").jsonPrimitive.content)
        assertEquals(canary.at, recorded.getValue("at").jsonPrimitive.content)
        assertTrue(recorded.containsKey("explorerSaid"), "the explorer's own text must be in the file")
        assertEquals(LiveEnv.canaryJson(canary), recorded, "the file must be the canary, verbatim")
    }

    /**
     * THE ALLOWLIST, JUDGED ON TEXTS THAT REALLY HAPPENED.
     *
     * The positive is a live `allBlockchains(state:)` refusal - the one
     * `INTERNAL_ERROR` a still-advertised tool can still produce
     * (docs/UPSTREAM.md #3b) - and the negative is a real connection refusal
     * from a real closed port. Both are produced here, in this test, by the
     * production code; neither is a string this file invented.
     */
    @Test
    fun theAllowlistMatchesARealExplorerRefusalAndNotARealLocalOne() = runBlocking {
        LiveChromia.requireLive("takes the explorer's real INTERNAL_ERROR text through the signature allowlist")

        val refused = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", LiveChromia.EXPLORER_NETWORK)
                    put("state", "RUNNING")
                    put("limit", 5)
                }
            ),
            LiveChromia.repository()
        )
        val explorerText = (refused.content.first() as TextContent).text!!
        assertEquals(
            true, refused.isError,
            "the explorer refuses a `state` filter with INTERNAL_ERROR (docs/UPSTREAM.md #3b). If it " +
                "now serves it, that entry, this assertion and the upstream warning that rests on it " +
                "are all stale and must come out: $explorerText"
        )
        assertEquals(
            "explorer-graphql-internal-error", LiveEnv.upstreamSignature(explorerText),
            "the explorer's own INTERNAL_ERROR-for-<uuid> must be allowlisted: $explorerText"
        )

        // A real closed port, refused by the operating system. It carries a
        // marker the live helpers' wording lists mention ("connection refused")
        // and it must NOT be allowlisted for a downgrade: nothing about a
        // refused socket says the third party is at fault.
        val offline = FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject {
                    put("network", "mainnet")
                    put("limit", 1)
                }
            ),
            McpTestSupport.offlineRepository()
        )
        val localText = (offline.content.first() as TextContent).text!!
        assertEquals(true, offline.isError, "a closed port must fail: $localText")
        assertNull(
            LiveEnv.upstreamSignature(localText),
            "a refused local socket is not evidence about the explorer, and a downgrade allowlist " +
                "that accepted it would turn every one of OUR connection bugs into somebody else's " +
                "problem: $localText"
        )
        Unit
    }
}
