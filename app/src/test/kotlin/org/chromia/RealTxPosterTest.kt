package org.chromia

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.crypto.KeyPair
import org.chromia.tools.RealTxPoster
import org.chromia.tools.TestnetProvisioning
import org.chromia.tools.TxOutcome
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException
import java.time.Duration

/**
 * Wire-level proof of what a [RealTxPoster] outcome carries, with no network:
 * a scripted HttpHandler plays the node, so each exchange the real client can
 * see is driven deterministically. The point is the null-reason cases. CI run
 * 34005621969 (2026-09-05) had `confirmed == false, rejectReason == null` and
 * nothing else - postchain-client turns a poll that runs out of retries into
 * WAITING/null and every poll error into UNKNOWN/null, indistinguishable from
 * a node that omitted its reason. The outcome now says which one happened.
 */
class RealTxPosterTest {

    private val brid = "090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874"
    private val nodeUrl = "http://node.invalid:7740"
    private val ops = listOf(TestnetProvisioning.faucetOp())

    private fun config(): PostchainClientConfig {
        val throwaway = TestnetProvisioning.cryptoSystem.generateKeyPair()
        return PostchainClientConfig(
            blockchainRid = BlockchainRid.buildFromHex(brid),
            endpointPool = EndpointPool.default(listOf(nodeUrl)),
            signers = listOf(KeyPair(throwaway.pubKey.data, throwaway.privKey.data)),
            statusPollCount = 3,
            statusPollInterval = Duration.ofMillis(1),
            // Pin the merkle version so the client does not round-trip /features.
            merkleHashVersion = 1
        )
    }

    private fun Response.json(body: String): Response =
        header("Content-Type", "application/json").body(body)

    /** Plays the node: one answer for the POST, a scripted answer per status poll. */
    private class ScriptedNode(
        private val postResponse: Response = Response(Status.OK),
        private val statusResponse: (pollNumber: Int) -> Response
    ) : HttpHandler {
        var polls = 0
        override fun invoke(request: Request): Response = when {
            request.method == Method.POST && request.uri.path.startsWith("/tx/") -> postResponse
            request.uri.path.endsWith("/status") -> statusResponse(++polls)
            else -> Response(Status.NOT_FOUND).json("""{"error":"no such route"}""")
        }
        private fun Response.json(body: String): Response =
            header("Content-Type", "application/json").body(body)
    }

    private fun post(node: ScriptedNode): TxOutcome = RealTxPoster.postWith(config(), node, ops)

    private fun recordedPoll(outcome: TxOutcome): String =
        outcome.lastStatusPollResponse ?: fail("no status poll was recorded: $outcome")

    @Test
    fun rejectedStatusCarriesReasonRawStatusAndTheExactBody() {
        val body = """{"status":"rejected","rejectReason":"Operation 'ft4.ft_auth' failed: Account not found"}"""
        val node = ScriptedNode { Response(Status.OK).json(body) }
        val outcome = post(node)

        assertFalse(outcome.confirmed)
        // The client parsed the reason out of a body the recorder had already
        // read - proves the bytes are handed back intact, not consumed.
        assertEquals("Operation 'ft4.ft_auth' failed: Account not found", outcome.rejectReason)
        assertEquals("REJECTED", outcome.finalStatus)
        assertEquals(1, node.polls, "a REJECTED status ends the poll at once")
        val poll = recordedPoll(outcome)
        assertTrue(poll.startsWith("poll #1 GET $nodeUrl/tx/$brid/${outcome.txRidHex}/status -> HTTP 200 "), poll)
        assertTrue(poll.endsWith(body), poll)
    }

    @Test
    fun confirmedStatusIsConfirmedWithNoReason() {
        val outcome = post(ScriptedNode { Response(Status.OK).json("""{"status":"confirmed"}""") })
        assertTrue(outcome.confirmed)
        assertNull(outcome.rejectReason)
        assertEquals("CONFIRMED", outcome.finalStatus)
        assertTrue(outcome.lastStatusPollResponse!!.endsWith("""{"status":"confirmed"}"""))
    }

    @Test
    fun stillWaitingWhenPollsRunOutIsReportedAsWaitingNotAsAReasonlessRejection() {
        val node = ScriptedNode { Response(Status.OK).json("""{"status":"waiting"}""") }
        val outcome = post(node)

        assertFalse(outcome.confirmed)
        assertNull(outcome.rejectReason, "postchain-client gives no reason for an unfinished poll")
        assertEquals("WAITING", outcome.finalStatus)
        assertEquals(3, node.polls, "statusPollCount polls, then the client gives up")
        val poll = recordedPoll(outcome)
        assertTrue(poll.startsWith("poll #3 GET "), poll)
        assertTrue(poll.endsWith("""-> HTTP 200 {"status":"waiting"}"""), poll)
    }

    @Test
    fun httpErrorOnEveryPollIsUnknownAndTheErrorBodyIsKept() {
        val node = ScriptedNode { Response(Status.NOT_FOUND).json("""{"error":"Transaction not found"}""") }
        val outcome = post(node)

        assertFalse(outcome.confirmed)
        assertNull(outcome.rejectReason, "the client swallows poll errors into UNKNOWN/null")
        assertEquals("UNKNOWN", outcome.finalStatus)
        assertEquals(3, node.polls)
        val poll = recordedPoll(outcome)
        assertTrue(poll.startsWith("poll #3 GET "), poll)
        assertTrue(poll.endsWith("""-> HTTP 404 {"error":"Transaction not found"}"""), poll)
    }

    @Test
    fun transportExceptionOnThePollIsRecordedByNameAndMessage() {
        val node = ScriptedNode { throw SocketTimeoutException("Read timed out") }
        val outcome = post(node)

        assertFalse(outcome.confirmed)
        assertNull(outcome.rejectReason)
        assertEquals("UNKNOWN", outcome.finalStatus)
        assertEquals(3, node.polls)
        val poll = recordedPoll(outcome)
        assertTrue(poll.startsWith("poll #3 GET "), poll)
        assertTrue(poll.endsWith(" threw java.net.SocketTimeoutException: Read timed out"), poll)
    }

    @Test
    fun postRejectedOnTheWireHasAReasonAndNoStatusPoll() {
        val node = ScriptedNode(
            postResponse = Response(Status.BAD_REQUEST).json("""{"error":"Invalid transaction: bad signature"}"""),
            statusResponse = { error("the client must not poll a tx the node refused to enqueue") }
        )
        val outcome = post(node)

        assertFalse(outcome.confirmed)
        assertEquals("Invalid transaction: bad signature", outcome.rejectReason)
        assertEquals("REJECTED", outcome.finalStatus)
        assertEquals(0, node.polls)
        assertNull(outcome.lastStatusPollResponse, "nothing was polled, so nothing is claimed")
    }

    @Test
    fun oversizedStatusBodyIsTruncatedInTheRecordButParsedWholeByTheClient() {
        val reason = "x".repeat(3000)
        val node = ScriptedNode { Response(Status.OK).json("""{"status":"rejected","rejectReason":"$reason"}""") }
        val outcome = post(node)

        assertEquals(reason, outcome.rejectReason, "the client must see the full body")
        val poll = recordedPoll(outcome)
        assertTrue(poll.contains("...[") && poll.endsWith(" chars]"), poll)
        assertTrue(poll.length < 2300, "record is bounded, got ${poll.length} chars")
    }
}
