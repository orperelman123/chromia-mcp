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
import net.postchain.common.exception.UserMistake
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.NON_STRICT_QUERY_ARGUMENT
import org.chromia.tools.LocalChain
import org.chromia.tools.LocalChainRestBridge
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * HTTP-surface tests for the local chain's REST facade against a fake chain
 * gateway - proves paths, bodies, status codes, and error mapping match the
 * postchain REST semantics without needing PostgreSQL. The same surface is
 * exercised against a REAL node (including via postchain-client) in
 * LocalChainIntegrationTest.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LocalChainRestBridgeTest {

    private val brid = "AB".repeat(32)

    private val fakeGateway = object : LocalChainRestBridge.ChainGateway {
        override fun query(name: String, args: Gtv): Gtv = when (name) {
            "echo_args" -> args
            "boom" -> throw UserMistake("Query error: unknown thing")
            else -> gtv("result-of-$name")
        }

        override fun queryWithHeight(name: String, args: Gtv): Pair<Gtv, Long> =
            query(name, args) to 7L

        override fun postTransaction(raw: ByteArray) {
            when {
                raw.isEmpty() -> throw UserMistake("Transaction is invalid")
                raw.size == 1 -> throw LocalChainRestBridge.DuplicateTx("Transaction already in queue")
                else -> Unit
            }
        }

        override fun transactionStatus(txRid: ByteArray): TransactionStatus =
            if (txRid.first() == 0xAA.toByte()) TransactionStatus.CONFIRMED else TransactionStatus.UNKNOWN
    }

    private lateinit var bridge: LocalChainRestBridge
    private val client = HttpClient(CIO)
    private val base: String get() = "http://127.0.0.1:${bridge.port}"

    @BeforeAll
    fun setUp() {
        bridge = LocalChainRestBridge(fakeGateway, brid, LocalChain.freePortIn(LocalChain.API_PORT_RANGE))
    }

    @AfterAll
    fun tearDown() {
        client.close()
        bridge.close()
    }

    @Test
    fun bridEndpointReturnsPlainTextRid() = runBlocking {
        assertEquals(brid, client.get("$base/brid/iid_0").bodyAsText())
    }

    @Test
    fun postQueryAnswersJson() = runBlocking {
        val response = client.post("$base/query/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"my_query","limit":3}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("\"result-of-my_query\"", response.bodyAsText())
    }

    @Test
    fun postQueryPassesArgumentsWithoutTypeKey() = runBlocking {
        val body = client.post("$base/query/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"echo_args","name":"neo","limit":3}""")
        }.bodyAsText()
        // The echoed args carry name/limit but never "type"; the non-strict
        // marker matches postchain's own REST behavior.
        assertTrue(body.contains("\"name\":\"neo\""), body)
        assertTrue(body.contains("\"limit\":3"), body)
        assertTrue(!body.contains("\"type\""), body)
        assertTrue(body.contains(NON_STRICT_QUERY_ARGUMENT), body)
    }

    @Test
    fun getQueryUsesQueryParameters() = runBlocking {
        val body = client.get("$base/query/$brid?type=echo_args&name=neo&flag=true").bodyAsText()
        assertTrue(body.contains("\"name\":\"neo\""), body)
        assertTrue(body.contains("\"flag\":1") || body.contains("\"flag\":true"), body)
    }

    @Test
    fun missingQueryTypeIs400() = runBlocking {
        val response = client.post("$base/query/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"no_type":1}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Missing query type"), response.bodyAsText())
    }

    @Test
    fun queryErrorsSurfaceAs400WithMessage() = runBlocking {
        val response = client.post("$base/query/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"boom"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("unknown thing"), response.bodyAsText())
    }

    @Test
    fun wrongBridIs404WithBothRids() = runBlocking {
        val other = "CD".repeat(32)
        val response = client.post("$base/query/$other") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"my_query"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(other) && body.contains(brid), body)
    }

    @Test
    fun postTransactionAcceptsJsonHexAndBinary() = runBlocking {
        val ok = client.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":"DEADBEEF"}""")
        }
        assertEquals(HttpStatusCode.OK, ok.status)
        assertEquals("{}", ok.bodyAsText())

        val binary = client.post("$base/tx/$brid") {
            contentType(ContentType.Application.OctetStream)
            setBody(byteArrayOf(1, 2, 3))
        }
        assertEquals(HttpStatusCode.OK, binary.status)
    }

    @Test
    fun duplicateTransactionIs409AndInvalidIs400() = runBlocking {
        val duplicate = client.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":"AA"}""") // 1 byte -> fake duplicate
        }
        assertEquals(HttpStatusCode.Conflict, duplicate.status)

        val invalid = client.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":""}""") // empty -> fake invalid
        }
        assertEquals(HttpStatusCode.BadRequest, invalid.status)

        val badHex = client.post("$base/tx/$brid") {
            contentType(ContentType.Application.Json)
            setBody("""{"tx":"zz"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, badHex.status)
        assertTrue(badHex.bodyAsText().contains("hex"), badHex.bodyAsText())
    }

    @Test
    fun statusEndpointReportsLowercaseStatuses() = runBlocking {
        val confirmed = client.get("$base/tx/$brid/${"AA".repeat(32)}/status").bodyAsText()
        assertEquals("""{"status":"confirmed"}""", confirmed)
        val unknown = client.get("$base/tx/$brid/${"BB".repeat(32)}/status").bodyAsText()
        assertEquals("""{"status":"unknown"}""", unknown)
        val badRid = client.get("$base/tx/$brid/1234/status")
        assertEquals(HttpStatusCode.BadRequest, badRid.status)
    }
}
