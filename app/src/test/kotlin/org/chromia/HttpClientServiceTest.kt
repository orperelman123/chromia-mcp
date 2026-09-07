package org.chromia

import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import org.chromia.data.client.HttpClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.data.config.HttpTimeouts
import org.chromia.domain.NetworkResult
import org.chromia.domain.exceptions.GraphQLException
import org.chromia.domain.exceptions.HttpRequestException
import org.chromia.domain.graphqlQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

/**
 * THE EXPLORER, LIVE.
 *
 * Every test below used to run against a `MockEngine` holding a recorded
 * envelope, and the interesting assertions were about the request the engine had
 * captured. That arrangement can only confirm that the client sends what the
 * test thinks it sends; it is structurally incapable of noticing that the
 * explorer changed - which is the one thing an HTTP client test is for.
 *
 * The public explorer is a third party, so it is the source of the responses
 * now. It supplies every shape the fixtures used to fabricate:
 *
 *  - a 200 with `data` (any valid document on mainnet);
 *  - a 200 with `errors[]` (an undefined field - the explorer answers 200 and
 *    puts a validation error in the body, which is exactly the case
 *    GraphQLResponseParser exists for);
 *  - a real 4xx on `network=testnet`, the upstream limitation in
 *    docs/UPSTREAM.md #9, and a real 4xx on `network=devnet1`, which is the
 *    control proving the testnet hint is conditioned on the NETWORK;
 *  - a real transport failure, from a real closed port.
 *
 * One shape has no real input at all and its test is gone - see the note on
 * [http400OnADifferentNetworkStaysPlain].
 *
 * These are gated only by LiveChromia.requireLive, which the merge gate and CI
 * both set, so the normal state is that they run. An explorer outage is a red
 * with retry as the remedy; a fixture that stays green through an outage is the
 * failure mode this suite is being rid of.
 */
class HttpClientServiceTest {

    /** The production service against the real explorer. No engine, no seam. */
    private fun live(): HttpClientService = HttpClientService(ChromiaConfig())

    /** A well-formed document the explorer's schema actually serves. */
    private fun realQuery() = graphqlQuery { query("{ allAssets { id name symbol } }") }

    @Test
    fun liveTwoHundredParsesTheExplorersGraphQlBody() = runBlocking {
        LiveChromia.requireLive("posts a real GraphQL document to the live explorer")
        val result = live().executeGraphQLQuery(realQuery(), LiveChromia.EXPLORER_NETWORK)
        assertTrue(result is NetworkResult.Success, "live explorer query failed: $result")
        val body = (result as NetworkResult.Success).data
        val assets = body.getValue("data").jsonObject.getValue("allAssets")
        assertTrue(
            assets.toString().contains("\"id\""),
            "the explorer answered without any asset ids: $assets"
        )
    }

    /**
     * THE NETWORK IS A QUERY PARAMETER, PROVED BY THE EXPLORER.
     *
     * The old test asserted this by reading `request.url.parameters["network"]`
     * off a captured request - a statement about the client, checked by the
     * client. The explorer settles it: it serves `network=mainnet` and REFUSES
     * `network=testnet` with a 4xx. If the name were not reaching the URL, both
     * calls would take the same path and come back the same way. They do not.
     */
    @Test
    fun theNetworkNameReachesTheExplorerAsAQueryParameter() = runBlocking {
        LiveChromia.requireLive("proves the network name reaches the explorer's URL, not its variables")
        val onMainnet = live().executeGraphQLQuery(realQuery(), "mainnet")
        val onTestnet = live().executeGraphQLQuery(realQuery(), "testnet")
        assertTrue(onMainnet is NetworkResult.Success, "mainnet must serve: $onMainnet")
        assertTrue(
            onTestnet is NetworkResult.Error,
            "the public explorer refuses network=testnet (docs/UPSTREAM.md #9); if this passed, either " +
                "the network name is no longer reaching the query string, or upstream has fixed the " +
                "limitation - in which case update docs/UPSTREAM.md and this test together. Got: " + onTestnet
        )
    }

    /**
     * The 200-with-errors branch, from a document the real schema rejects. The
     * explorer answers HTTP 200 and puts a validation error in the body - the
     * exact case GraphQLResponseParser exists to turn into an error.
     */
    @Test
    fun liveGraphQlErrorsInATwoHundredAreNetworkResultError() = runBlocking {
        LiveChromia.requireLive("asks the live explorer for a field its schema does not define")
        val result = live().executeGraphQLQuery(
            graphqlQuery { query("{ noSuchFieldAtAll8f2b41c9 }") },
            LiveChromia.EXPLORER_NETWORK
        )
        assertTrue(result is NetworkResult.Error, "an undefined field must be an error: $result")
        val error = result as NetworkResult.Error
        assertTrue(error.cause is GraphQLException, "cause was " + error.cause)
        val messages = (error.cause as GraphQLException).errors
        assertTrue(messages.isNotEmpty(), "the explorer's own messages must survive: $error")
        assertTrue(
            messages.any { it.contains("noSuchFieldAtAll8f2b41c9") },
            "the explorer names the offending field and we must keep its words: $messages"
        )
        assertTrue(error.message.contains("noSuchFieldAtAll8f2b41c9"), error.message)
    }

    /**
     * The 4xx branch AND the testnet hint, from the explorer's real refusal of
     * `network=testnet`. This also carries the claim the deleted 503 test used
     * to carry on its own: an HTTP error arrives as an HttpRequestException
     * whose statusCode is the status the server actually sent.
     */
    @Test
    fun liveFourHundredOnTestnetAppendsTheUpstreamLimitationHint() = runBlocking {
        LiveChromia.requireLive("takes the explorer's real refusal of network=testnet")
        val result = live().executeGraphQLQuery(realQuery(), "testnet")
        assertTrue(result is NetworkResult.Error, "expected the explorer's refusal: $result")
        val error = result as NetworkResult.Error
        assertTrue(error.cause is HttpRequestException, "cause was " + error.cause)
        val status = (error.cause as HttpRequestException).statusCode
        assertTrue(status in 400..499, "docs/UPSTREAM.md #9 says 4xx; the explorer sent $status")
        assertTrue(error.message.contains("rejects network=testnet"), error.message)
        assertTrue(error.message.contains("docs/UPSTREAM.md"), error.message)
        assertTrue(error.message.contains("chromia_dapp_query"), error.message)
    }

    /**
     * The control for the hint's NETWORK conjunct: devnet1 is a real predefined
     * network the public explorer also refuses with a 4xx, and it must not get
     * the testnet sentence.
     *
     * The hint's other conjunct is the status RANGE (400..499), and the test
     * that covered it - a 503 on testnet getting no hint - is DELETED: the
     * explorer cannot be made to return 5xx by any request, and the only way to
     * produce one was a fixture.
     *
     * CLAIM REMOVED: "a 5xx on network=testnet does not get the 4xx-only
     * upstream hint". Nothing covers it now. The production branch is
     * `response.status.value in 400..499 && targetNetwork == "testnet"` in
     * HttpClientService; a regression that widened the range would surface only
     * the next time the explorer 5xxs on testnet.
     */
    @Test
    fun http400OnADifferentNetworkStaysPlain() = runBlocking {
        LiveChromia.requireLive("takes the explorer's real refusal of network=devnet1")
        val result = live().executeGraphQLQuery(realQuery(), "devnet1")
        assertTrue(result is NetworkResult.Error, "expected the explorer's refusal: $result")
        val error = result as NetworkResult.Error
        assertTrue(error.cause is HttpRequestException, "cause was " + error.cause)
        assertFalse(
            error.message.contains("rejects network=testnet"),
            "the testnet hint is for testnet only: " + error.message
        )
    }

    /**
     * A real transport failure: a real socket to a real address the operating
     * system refuses. The old version threw an IOException from inside a
     * MockEngine, which tested that Ktor propagates what a lambda throws.
     */
    @Test
    fun transportFailureKeepsTheRealCause() = runBlocking {
        val service = HttpClientService(ChromiaConfig(explorerUrl = "http://127.0.0.1:1/explorer-service"))
        val result = service.executeGraphQLQuery(realQuery(), "mainnet")
        assertTrue(result is NetworkResult.Error, "a closed port must be an error: $result")
        val error = result as NetworkResult.Error
        assertTrue(error.message.startsWith("Request failed:"), error.message)
        assertTrue(
            error.cause is java.io.IOException,
            "a refused connection must arrive as an IOException, got " + error.cause
        )
    }

    /**
     * A null network falls back to `config.defaultNetwork`. Proved by the
     * explorer rather than by a captured parameter: the default is mainnet,
     * mainnet is the one network the explorer serves, so a null network that did
     * NOT fall back to it would come back as a 4xx instead of data.
     */
    @Test
    fun nullNetworkFallsBackToTheDefaultNetwork() = runBlocking {
        LiveChromia.requireLive("proves a null network falls back to the served default")
        assertEquals("mainnet", ChromiaConfig().defaultNetwork)
        val result = live().executeGraphQLQuery(realQuery(), null)
        assertTrue(result is NetworkResult.Success, "null network did not reach mainnet: $result")
    }

    // ---- reality audit D5: an unknown network must fail LOCALLY, never be
    // forwarded upstream where the explorer may silently default it ----------
    //
    // These need no network and never did: the gate returns before any request
    // is made. The MockEngine they used to carry existed only so the test could
    // assert it had received nothing - a double whose whole purpose was to stay
    // untouched. A service pointed at a closed port makes the point better: if
    // the gate ever stopped short-circuiting, the call would fail with a real
    // connection error instead of the message asserted here.

    private fun gated(): HttpClientService =
        HttpClientService(ChromiaConfig(explorerUrl = "http://127.0.0.1:1/explorer-service"))

    @Test
    fun typoNetworkIsRejectedLocallyNamingTheValidValues() = runBlocking {
        val result = gated().executeGraphQLQuery(graphqlQuery { query("{ ping }") }, "tesnet")
        assertTrue(result is NetworkResult.Error, result.toString())
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("Unknown network \"tesnet\""), error.message)
        assertTrue(error.message.contains("mainnet"), error.message)
        assertTrue(error.message.contains("testnet"), error.message)
        assertTrue(
            error.cause is IllegalArgumentException,
            "a typo must be refused locally, never sent - got " + error.cause
        )
    }

    @Test
    fun nodeUrlAsNetworkGetsExplorerVsNodeDirectHint() = runBlocking {
        val result = gated().executeGraphQLQuery(
            graphqlQuery { query("{ ping }") },
            "https://mynode.example:7740"
        )
        assertTrue(result is NetworkResult.Error, result.toString())
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("chromia_dapp_query"), error.message)
        assertTrue(error.message.contains("network name"), error.message)
        assertTrue(error.cause is IllegalArgumentException, "" + error.cause)
    }

    /**
     * `Mainnet` used to be refused as a typo (DX audit 2026-09-04). Live, the
     * folding is proved by the explorer answering with data: an unfolded
     * " Mainnet " never leaves the process.
     */
    @Test
    fun networkNameCaseIsFoldedOntoTheCanonicalName() = runBlocking {
        LiveChromia.requireLive("proves ' Mainnet ' folds onto the canonical name the explorer serves")
        val result = live().executeGraphQLQuery(realQuery(), " Mainnet ")
        assertTrue(
            result is NetworkResult.Success,
            "' Mainnet ' must fold onto mainnet and reach the explorer: $result"
        )
    }

    /**
     * Every predefined name must pass the LOCAL gate. Upstream is free to refuse
     * three of the four - it does, with a 4xx, and that is upstream's answer,
     * not ours. What must never happen is our own "Unknown network" for a name
     * that is in the config, which is what this asserts.
     */
    @Test
    fun everyPredefinedNetworkNamePassesTheLocalGate() = runBlocking {
        LiveChromia.requireLive("sends every predefined network name to the live explorer")
        ChromiaConfig().predefinedNetworks.keys.forEach { name ->
            val result = live().executeGraphQLQuery(realQuery(), name)
            if (result is NetworkResult.Error) {
                assertFalse(
                    result.cause is IllegalArgumentException,
                    "$name is a predefined network and must never be refused locally: $result"
                )
                assertTrue(
                    result.cause is HttpRequestException || result.cause is GraphQLException,
                    "$name: the only acceptable refusal is upstream's, got " + result.cause
                )
            }
        }
    }

    @Test
    fun createProductionClientInstallsContentNegotiationAndTimeouts() {
        val config = ChromiaConfig(
            httpTimeouts = HttpTimeouts(requestTimeout = 15.seconds, connectTimeout = 5.seconds)
        )
        val client = HttpClientService.createProductionClient(config)
        try {
            assertEquals("CIOEngine", client.engine::class.simpleName)
            assertNotNull(
                client.pluginOrNull(ContentNegotiation),
                "production factory must install ContentNegotiation"
            )
            assertNotNull(
                client.pluginOrNull(HttpTimeout),
                "production factory must install HttpTimeout"
            )
            assertEquals(15.seconds, config.httpTimeouts.requestTimeout)
            assertEquals(5.seconds, config.httpTimeouts.connectTimeout)
        } finally {
            client.close()
        }
    }
}
