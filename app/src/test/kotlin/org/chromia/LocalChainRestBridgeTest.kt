package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.core.BlockchainEngine
import net.postchain.crypto.KeyPair
import net.postchain.crypto.PrivKey
import net.postchain.crypto.PubKey
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.crypto.secp256k1_derivePubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.Gtx
import org.chromia.tools.LocalChain
import org.chromia.tools.LocalChainRestBridge
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * HTTP-surface tests for the local chain's REST facade, driven against the REAL
 * embedded Postchain node.
 *
 * There is no gateway substitute here any more. Every request below is answered
 * by [LocalChainRestBridge.EngineGateway] over a real `BlockchainEngine`, which
 * is the only thing that ever answers it in production: a real Rell app is
 * compiled, a real node is started against a real PostgreSQL, real Rell queries
 * run, and real secp256k1-signed GTX transactions are enqueued and confirmed in
 * real blocks.
 *
 * ONE node per test CLASS. Starting a Postchain node (schema wipe, blockchain
 * init, first block) costs seconds, so [startTheRealChainOnce] brings up a
 * single chain in @BeforeAll and [stopTheRealChain] tears it down in @AfterAll;
 * the tests are written so that none of them depends on another's writes.
 *
 * Env-gated exactly like the repo's other database-backed tests
 * (CHROMIA_TEST_DATABASE_URL via [LiveEnv]; the merge gate refuses to run
 * without it, so the normal state is zero skips).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalChainRestBridgeTest {

    /**
     * A real dapp. `answer`, `echo_text`, `echo_flag` and `echo_pair` are pure
     * (no state), so the query tests are order-independent; `add_book` is the
     * write the transaction tests use, each with its own ISBN.
     */
    private val files = mapOf(
        "main.rell" to """
            module;
            entity book { key isbn: text; title: text; }
            operation add_book(isbn: text, title: text) { create book(isbn, title); }
            query answer() = 42;
            query echo_text(name: text) = name;
            query echo_flag(flag: boolean) = flag;
            query echo_pair(name: text, limit: integer) = (name, limit);
        """.trimIndent()
    )

    private val cryptoSystem = Secp256K1CryptoSystem()
    private val devPrivKey = LocalChain.DEV_PRIV_KEY_HEX.hexStringToByteArray()
    private val devKeyPair = KeyPair(PubKey(secp256k1_derivePubKey(devPrivKey)), PrivKey(devPrivKey))

    private val http = HttpClient(CIO)
    private lateinit var brid: String
    private lateinit var base: String

    /**
     * Used only to BUILD signed transactions (GTX body, merkle digest,
     * secp256k1 signature); every post below goes over the bridge's own HTTP
     * surface with ktor, so the JSON-hex and octet-stream bodies are the ones
     * under test rather than whatever postchain-client happens to send.
     *
     * merkleHashVersion is pinned to 2 on purpose: the chain is configured with
     * merkle_hash_version 2 ([LocalChain.configTemplate]) and this bridge
     * serves no `/config/{brid}/features` route, so the client's auto-detect
     * would fall back to version 1 and sign a digest the node does not agree
     * with. (Reported as a production gap - see the report for this change.)
     */
    private lateinit var builderClient: PostchainClient

    @BeforeAll
    fun startTheRealChainOnce() {
        val databaseUrl = LiveEnv.requireDatabaseUrl(
            "the REST bridge is exercised against a real embedded Postchain node, not a substitute gateway"
        )
        val up = LocalChain.up(files, databaseUrl = databaseUrl, ttlSeconds = 900)
        assertTrue(up.ok, "local chain failed to start: ${up.notes}")
        brid = up.brid!!
        base = up.apiUrl!!
        builderClient = PostchainClientProviderImpl().createClient(
            PostchainClientConfig(
                blockchainRid = BlockchainRid.buildFromHex(brid),
                endpointPool = EndpointPool.singleUrl(base),
                signers = listOf(devKeyPair),
                merkleHashVersion = 2
            )
        )
    }

    @AfterAll
    fun stopTheRealChain() {
        runCatching { builderClient.close() }
        http.close()
        LocalChain.stopAll()
    }

    /** The engine the running node serves - the production gateway's input. */
    private fun realEngine(): BlockchainEngine =
        LocalChain.running!!.node!!.processManager.retrieveBlockchain(LocalChain.CHAIN_IID)!!.blockchainEngine

    private fun signedTx(name: String, vararg args: Gtv): Gtx =
        builderClient.transactionBuilder()
            .addOperation(name, *args)
            .finish()
            .sign(devKeyPair.sigMaker(cryptoSystem))
            .buildGtx()

    private fun Gtx.ridHex(): String = calculateTxRid(builderClient.merkleHashCalculator).toHex()

    private fun statusOf(txRidHex: String): String = runBlocking {
        http.get("$base/tx/$brid/$txRidHex/status").bodyAsText()
    }

    /** Polls the bridge's own status endpoint until [wanted], or gives up. */
    private fun awaitStatus(txRidHex: String, wanted: String, timeoutMs: Long = 60_000): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        var last = ""
        while (System.currentTimeMillis() < deadline) {
            last = statusOf(txRidHex)
            if (last.contains(""""status":"$wanted"""")) return last
            Thread.sleep(200)
        }
        return last
    }

    @Test
    fun bridEndpointReturnsPlainTextRid() = runBlocking {
        assertEquals(brid, http.get("$base/brid/iid_0").bodyAsText())
    }

    @Test
    fun postQueryAnswersJson() = runBlocking {
        val response = http.post("$base/query/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"answer"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("42", response.bodyAsText().trim())
    }

    /**
     * A real Rell query REFUSES arguments it does not declare (RellPostchainModule
     * throws "unknown arguments: ..." for every key that is not a parameter and
     * is not the `~non-strict` marker - which is exactly the 400 the next test
     * pins). So this call succeeding IS the proof that the bridge strips the
     * `type` key before handing the arguments to the chain: had it forwarded
     * `type`, the node would have rejected the query.
     *
     * What is NOT asserted any more: the literal presence of
     * NON_STRICT_QUERY_ARGUMENT in the argument dictionary. The bridge builds
     * that dictionary inside a ktor route and a real node's only reaction to the
     * marker is to strip it, so no real request can tell a bridge that sends it
     * from one that does not.
     */
    @Test
    fun postQueryPassesArgumentsWithoutTypeKey() = runBlocking {
        val response = http.post("$base/query/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"echo_pair","name":"neo","limit":3}""")
        }
        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val body = response.bodyAsText()
        assertTrue(body.contains("\"neo\""), body)
        assertTrue(body.contains("3"), body)
    }

    @Test
    fun getQueryUsesQueryParameters() = runBlocking {
        val text = http.get("$base/query/$brid?type=echo_text&name=neo").bodyAsText()
        assertEquals("\"neo\"", text.trim())
        // "true"/"false" become real booleans (RestApi parity); a Rell boolean
        // comes back over GTV as 1/0, so accept either rendering.
        val flag = http.get("$base/query/$brid?type=echo_flag&flag=true").bodyAsText().trim()
        assertTrue(flag == "1" || flag == "true", flag)
    }

    @Test
    fun missingQueryTypeIs400() = runBlocking {
        val response = http.post("$base/query/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"no_type":1}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Missing query type"), response.bodyAsText())
    }

    /**
     * The chain's own error - an argument the Rell query does not declare - must
     * reach the caller as a 400 whose body names the offending argument. The
     * exact wording belongs to Rell, so only the part Rell composes from our
     * input (the argument name) is pinned.
     */
    @Test
    fun queryErrorsSurfaceAs400WithMessage() = runBlocking {
        val response = http.post("$base/query/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"echo_text","name":"neo","bogus_argument":1}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        val body = response.bodyAsText()
        assertTrue(body.startsWith("""{"error":"""), body)
        assertTrue(body.contains("bogus_argument"), body)
    }

    @Test
    fun wrongBridIs404WithBothRids() = runBlocking {
        val other = "CD".repeat(32)
        val response = http.post("$base/query/$other") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"answer"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(other) && body.contains(brid), body)
    }

    /**
     * Both accepted content types carry a REAL signed transaction, and both end
     * in a real block: postchain-client itself only ever posts octet-stream, so
     * the `{"tx":"<hex>"}` form is a path only this test covers.
     */
    @Test
    fun postTransactionAcceptsJsonHexAndBinaryAndBothConfirm() = runBlocking {
        val viaJson = signedTx("add_book", gtv("978-0-00-000001-1"), gtv("Posted as JSON hex"))
        val jsonResponse = http.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":"${viaJson.encode().toHex()}"}""")
        }
        assertEquals(HttpStatusCode.OK, jsonResponse.status, jsonResponse.bodyAsText())
        assertEquals("{}", jsonResponse.bodyAsText())

        val viaBinary = signedTx("add_book", gtv("978-0-00-000002-8"), gtv("Posted as octet-stream"))
        val binaryResponse = http.post("$base/tx/$brid") {
            contentType(ContentType.Application.OctetStream)
            setBody(viaBinary.encode())
        }
        assertEquals(HttpStatusCode.OK, binaryResponse.status, binaryResponse.bodyAsText())

        // The status endpoint reports the real, lowercase, confirmed status for
        // both - i.e. the node really built blocks containing them.
        assertEquals("""{"status":"confirmed"}""", awaitStatus(viaJson.ridHex(), "confirmed"))
        assertEquals("""{"status":"confirmed"}""", awaitStatus(viaBinary.ridHex(), "confirmed"))
    }

    @Test
    fun duplicateTransactionIs409AndInvalidIs400() = runBlocking {
        val tx = signedTx("add_book", gtv("978-0-00-000003-5"), gtv("Posted twice"))
        val hex = tx.encode().toHex()
        val first = http.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":"$hex"}""")
        }
        assertEquals(HttpStatusCode.OK, first.status, first.bodyAsText())

        // The same bytes again: still queued -> DUPLICATE, already in a block ->
        // "already in database". A real node answers 409 either way.
        val duplicate = http.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":"$hex"}""")
        }
        assertEquals(HttpStatusCode.Conflict, duplicate.status, duplicate.bodyAsText())

        // Empty bytes are not a transaction: the real transaction factory
        // refuses to decode them.
        val invalid = http.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":""}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalid.status, invalid.bodyAsText())

        val badHex = http.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":"zz"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, badHex.status)
        assertTrue(badHex.bodyAsText().contains("hex"), badHex.bodyAsText())
    }

    @Test
    fun statusEndpointReportsLowercaseStatusesAndRejectsMalformedRids() = runBlocking {
        // A transaction the chain has never seen.
        val neverPosted = "BB".repeat(32)
        assertEquals("""{"status":"unknown"}""", statusOf(neverPosted))
        // ... and one that exists, to prove "unknown" is not the only answer the
        // endpoint can give.
        val tx = signedTx("add_book", gtv("978-0-00-000004-2"), gtv("Status probe"))
        http.post("$base/tx/$brid") {
            contentType(ContentType.Application.OctetStream)
            setBody(tx.encode())
        }
        val confirmed = awaitStatus(tx.ridHex(), "confirmed")
        assertEquals("""{"status":"confirmed"}""", confirmed)
        assertNotEquals(confirmed, statusOf(neverPosted))

        val badRid = http.get("$base/tx/$brid/1234/status")
        assertEquals(HttpStatusCode.BadRequest, badRid.status)
    }

    /**
     * A busy `apiPort` used to surface as ktor CIO's raw
     * "LazyStandaloneCoroutine is cancelling" (the real BindException sits two
     * causes deep), so local_chain_up reported coroutine noise instead of the
     * port-busy hint LocalChain.diagnose was written to produce - its
     * "address already in use"/"bind" branch was unreachable for the only
     * failure mode that exists in production (QA lens 2026-09-02).
     *
     * The bridge under test is built over the RUNNING node's own engine, i.e.
     * the exact production constructor `LocalChainRestBridge(BlockchainEngine,
     * brid, port)`; it never gets to serve, because the bind fails first.
     */
    @Test
    fun busyPortFailsWithActionablePortBusyMessage() {
        java.net.ServerSocket(0, 50, java.net.InetAddress.getByName("127.0.0.1")).use { blocker ->
            val port = blocker.localPort
            val thrown = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
                LocalChainRestBridge(realEngine(), brid, port)
            }
            val message = thrown.message.orEmpty()
            assertTrue(message.contains("$port"), "message must name the busy port: $message")
            assertTrue(message.contains("already in use"), "message must say the port is in use: $message")

            // And LocalChain.diagnose classifies the real exception into the
            // retry-without-apiPort hint (previously it fell through to the
            // generic branch with the coroutine-cancellation text).
            val hint = LocalChain.diagnose(thrown, null)
            assertTrue(hint.contains("busy"), "diagnose must classify as port-busy: $hint")
            assertTrue(hint.contains("apiPort"), "diagnose must point at the apiPort escape hatch: $hint")
        }
    }
}
