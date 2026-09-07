package org.chromia

import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvFactory.gtv
import org.chromia.tools.LocalChain
import org.chromia.tools.RealTxPoster
import org.chromia.tools.TxOp
import org.chromia.tools.TxOutcome
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * What a [RealTxPoster] outcome carries, proved against a REAL chain.
 *
 * There is no scripted node here any more. Every exchange below is the real
 * production path - `RealTxPoster.post` builds a GTX transaction, merkle-hashes
 * it, signs it with secp256k1, POSTs it over HTTP and polls the status endpoint
 * - against a REAL embedded Postchain node started by [LocalChain.up] (one node
 * per test CLASS; node start-up costs seconds), or against a REAL closed
 * loopback port.
 *
 * The point remains the null-reason cases. CI run 34005621969 (2026-09-05) had
 * `confirmed == false, rejectReason == null` and nothing else, because
 * postchain-client turns a poll that runs out of retries into WAITING/null and
 * every poll error into UNKNOWN/null, indistinguishable from a node that
 * omitted its reason. [TxOutcome.finalStatus] and
 * [TxOutcome.lastStatusPollResponse] are what tell those apart, and every test
 * below pins them on real wire traffic.
 *
 * Env-gated on CHROMIA_TEST_DATABASE_URL through [LiveEnv], like the repo's
 * other database-backed tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RealTxPosterTest {

    private val files = mapOf(
        "main.rell" to """
            module;
            entity book { key isbn: text; title: text; }
            operation add_book(isbn: text, title: text) { create book(isbn, title); }
            operation always_fails(marker: text) {
                require(marker == "", "always_fails refused the transaction: " + marker);
            }
        """.trimIndent()
    )

    private val devPrivKey = LocalChain.DEV_PRIV_KEY_HEX.hexStringToByteArray()
    private lateinit var brid: String
    private lateinit var apiUrl: String

    @BeforeAll
    fun startTheRealChainOnce() {
        val databaseUrl = LiveEnv.requireDatabaseUrl(
            "RealTxPoster is driven against a real embedded Postchain node instead of a scripted HttpHandler"
        )
        val up = LocalChain.up(files, databaseUrl = databaseUrl, ttlSeconds = 900)
        assertTrue(up.ok, "local chain failed to start: ${up.notes}")
        // Self-verifying: a registered chain with no PostchainNode behind it
        // could only come from a substitute starter, and the point of this class
        // is that the poster is driven against a real one.
        assertNotNull(
            LocalChain.running?.node,
            "the chain under test must be a REAL Postchain node"
        )
        brid = up.brid!!
        apiUrl = up.apiUrl!!
    }

    @AfterAll
    fun stopTheRealChain() {
        LocalChain.stopAll()
    }

    /**
     * ONE operation is the interesting case, and until 2026-09-07 it could not
     * be tested here at all.
     *
     * A GTX operations array holding exactly ONE element merkle-hashes
     * differently under version 1 and version 2 (GtvBinaryTreeFactoryArray
     * flattens the single child node in v1, keeps it in v2). The local chain was
     * supposed to run merkle_hash_version 2, LocalChainRestBridge served no
     * `/config/{brid}/features` route, and `RealTxPoster.post` builds its own
     * client config - so postchain-client auto-detected nothing, fell back to
     * version 1, and a test could not pin its way around it. These tests
     * therefore always sent TWO operations, the one shape where both versions
     * agree, and the disagreement stayed invisible.
     *
     * Both ends are fixed now (the chain really configures version 2 under
     * `features`, and the bridge serves the route the client asks for), so every
     * post below sends the single operation an agent actually writes. A
     * regression at either end shows up as a rejected transaction right here.
     */
    private fun postToTheRealChain(ops: List<TxOp>, bridHex: String = brid, url: String = apiUrl): TxOutcome =
        RealTxPoster.post(urls = listOf(url), bridHex = bridHex, ops = ops, privKey = devPrivKey)

    private fun recordedPoll(outcome: TxOutcome): String =
        outcome.lastStatusPollResponse ?: fail("no status poll was recorded: $outcome")

    @Test
    fun confirmedTransactionIsConfirmedWithNoReasonAndTheConfirmingPollIsRecorded() {
        val outcome = postToTheRealChain(
            listOf(TxOp("add_book", listOf(gtv("978-1-00-000001-0"), gtv("First"))))
        )
        val wire = "finalStatus=${outcome.finalStatus} rejectReason=${outcome.rejectReason} " +
            "lastStatusPoll=${outcome.lastStatusPollResponse}"

        assertTrue(outcome.confirmed, "a valid transaction must confirm on the local chain ($wire)")
        assertNull(outcome.rejectReason, wire)
        assertEquals("CONFIRMED", outcome.finalStatus, wire)
        val poll = recordedPoll(outcome)
        assertTrue(poll.startsWith("poll #"), poll)
        assertTrue(poll.contains("$apiUrl/tx/$brid/${outcome.txRidHex}/status"), poll)
        assertTrue(poll.endsWith("""-> HTTP 200 {"status":"confirmed"}"""), poll)
    }

    /**
     * A transaction the chain REJECTS while building the block: the poll ends at
     * REJECTED and the recorder keeps the exact status body the client parsed.
     *
     * rejectReason is null here, and that is the honest answer, not a gap in the
     * poster: this local bridge's status endpoint answers `{"status":"rejected"}`
     * with no `rejectReason` field (LocalChainRestBridge only maps
     * TransactionStatus). The case where a node DOES supply the reason - and the
     * poster surfaces it - is covered live against the testnet Economy Chain by
     * TestnetProvisioningLiveTest.liveSignedTransactionIsAcceptedOnWireAndRejectedByFt4Auth.
     */
    @Test
    fun rejectedTransactionEndsThePollAtRejectedAndKeepsTheExactStatusBody() {
        val outcome = postToTheRealChain(
            listOf(TxOp("always_fails", listOf(gtv("boom"))))
        )
        val wire = "finalStatus=${outcome.finalStatus} rejectReason=${outcome.rejectReason} " +
            "lastStatusPoll=${outcome.lastStatusPollResponse}"

        assertFalse(outcome.confirmed, wire)
        assertEquals("REJECTED", outcome.finalStatus, wire)
        val poll = recordedPoll(outcome)
        assertTrue(poll.startsWith("poll #"), poll)
        assertTrue(poll.contains("$apiUrl/tx/$brid/${outcome.txRidHex}/status"), poll)
        assertTrue(poll.endsWith("""-> HTTP 200 {"status":"rejected"}"""), poll)
        assertNull(
            outcome.rejectReason,
            "this bridge's status endpoint carries no rejectReason, so the outcome must not invent one ($wire)"
        )
    }

    /**
     * The node refuses to enqueue at all - here because the transaction is
     * addressed to a blockchain this node does not serve, which is a real 404
     * with a real error body. The reason must survive, and nothing may be
     * claimed about a status poll that never happened.
     */
    @Test
    fun postRejectedOnTheWireHasAReasonAndNoStatusPoll() {
        val outcome = postToTheRealChain(
            listOf(TxOp("add_book", listOf(gtv("978-1-00-000004-1"), gtv("Wrong chain")))),
            bridHex = "CD".repeat(32)
        )
        val wire = "finalStatus=${outcome.finalStatus} rejectReason=${outcome.rejectReason}"

        assertFalse(outcome.confirmed, wire)
        assertEquals("REJECTED", outcome.finalStatus, wire)
        assertNotNull(outcome.rejectReason, wire)
        assertTrue(
            outcome.rejectReason!!.contains("Unknown blockchain RID"),
            "the node's own refusal must reach the caller verbatim: $wire"
        )
        assertNull(outcome.lastStatusPollResponse, "nothing was polled, so nothing is claimed")
    }

    /**
     * A REAL closed loopback port. Nothing listens on 127.0.0.1:1, so the
     * transport fails on the POST itself: the outcome must be an explicit
     * not-confirmed with a reason and no invented poll - never a silent success.
     */
    @Test
    fun postToAClosedLoopbackPortIsRejectedWithAReasonAndNeverPolls() {
        val outcome = postToTheRealChain(
            listOf(TxOp("add_book", listOf(gtv("978-1-00-000006-5"), gtv("Nobody listening")))),
            url = "http://127.0.0.1:1"
        )
        val wire = "finalStatus=${outcome.finalStatus} rejectReason=${outcome.rejectReason}"

        assertFalse(outcome.confirmed, wire)
        assertEquals("REJECTED", outcome.finalStatus, wire)
        assertNotNull(outcome.rejectReason, "a dead endpoint must still explain itself: $wire")
        assertTrue(outcome.rejectReason!!.isNotBlank(), wire)
        assertNull(outcome.lastStatusPollResponse, "the post never got through, so nothing was polled")
    }

    // DELETED 2026-09-07 (zero-doubles): rejectedStatusCarriesReasonRawStatusAndTheExactBody
    // asserted that a status body of {"status":"rejected","rejectReason":"..."}
    // is surfaced as rejectReason + finalStatus REJECTED + the verbatim recorded
    // body, ending the poll after one round.
    // No real input can produce it here: this local bridge's status endpoint
    // emits only {"status":"<status>"} (LocalChainRestBridge maps
    // TransactionStatus and nothing else), and no real node can be told to
    // return a specific reject body on demand.
    // Covered by TestnetProvisioningLiveTest.liveSignedTransactionIsAcceptedOnWireAndRejectedByFt4Auth
    // (a real reject reason from the live testnet Economy Chain) plus
    // rejectedTransactionEndsThePollAtRejectedAndKeepsTheExactStatusBody above
    // (the REJECTED status and the verbatim recorded body).

    // DELETED 2026-09-07 (zero-doubles): stillWaitingWhenPollsRunOutIsReportedAsWaitingNotAsAReasonlessRejection
    // asserted that a transaction still queued when the status poll runs out of
    // retries ends as finalStatus WAITING with a null reason - i.e. is NOT
    // reported as a reasonless rejection - after exactly statusPollCount polls.
    // No real input can produce it: it needs a node that keeps a transaction
    // queued for the whole poll budget on command, and RealTxPoster.post builds
    // its own PostchainClientConfig, so statusPollCount cannot be shrunk to meet
    // a real chain's block time. The local chain confirms or rejects well inside
    // the client's 20 x 500ms default.
    // Nothing covers it now - the WAITING branch of TxOutcome.finalStatus is
    // unverified.

    // DELETED 2026-09-07 (zero-doubles): httpErrorOnEveryPollIsUnknownAndTheErrorBodyIsKept
    // asserted that when every status poll answers with an HTTP error the client
    // ends at UNKNOWN with a null reason and the recorder keeps the last error
    // body.
    // No real input can produce it: the post has to SUCCEED (or there is no poll
    // at all) and then every one of the following polls has to fail, which only
    // a node scripted to answer differently per request can do.
    // Nothing covers it now - the UNKNOWN branch of TxOutcome.finalStatus and the
    // recording of a non-200 poll body are unverified.

    // DELETED 2026-09-07 (zero-doubles): transportExceptionOnThePollIsRecordedByNameAndMessage
    // asserted that when the transport THROWS on a status poll, the recorder
    // keeps "poll #n GET <url> threw <exception class>: <message>" and the
    // outcome is UNKNOWN with a null reason.
    // No real input can produce it: it needs a transport that serves the POST
    // and then throws on the polls. A real closed port fails the POST first,
    // which is the honest transport failure and is covered by
    // postToAClosedLoopbackPortIsRejectedWithAReasonAndNeverPolls above.
    // Nothing covers it now - StatusPollRecorder's exception branch is unverified.

    // DELETED 2026-09-07 (zero-doubles): oversizedStatusBodyIsTruncatedInTheRecordButParsedWholeByTheClient
    // asserted that a status body longer than MAX_RECORDED_BODY_CHARS is
    // truncated in lastStatusPollResponse (with a "...[N chars]" marker and a
    // bounded total length) while the client still parses the whole body.
    // No real input can produce it: it needs a 3000-character reject reason in a
    // status response, and no real node emits one on demand - this bridge emits
    // a fixed 20-odd byte body and the live testnet's reject reasons are short.
    // Nothing covers it now - StatusPollRecorder's truncation branch is
    // unverified.
}
