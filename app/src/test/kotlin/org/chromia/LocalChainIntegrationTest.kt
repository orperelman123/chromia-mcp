package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.chromia.tools.LocalChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import net.postchain.common.hexStringToByteArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * End-to-end proof that local_chain_up stands up a REAL queryable chain:
 * compiles Rell sources, runs an embedded Postchain node against PostgreSQL,
 * answers REST queries, and shuts down cleanly. Env-gated like the repo's
 * other database-backed tests (CHROMIA_TEST_DATABASE_URL, set in CI).
 */
class LocalChainIntegrationTest {

    private val databaseUrl = System.getenv(LocalChain.DATABASE_URL_ENV)

    private val files = mapOf(
        "main.rell" to """
            module;
            entity book { key isbn: text; title: text; }
            operation add_book(isbn: text, title: text) { create book(isbn, title); }
            query all_books() = book @* {} (.isbn, .title);
            query book_count() = (book @* {}).size();
        """.trimIndent()
    )

    @AfterEach
    fun tearDown() {
        LocalChain.stopAll()
    }

    @Test
    fun chainStartsAnswersQueriesAndStops() {
        assumeTrue(!databaseUrl.isNullOrBlank(), "needs ${LocalChain.DATABASE_URL_ENV}")

        val up = LocalChain.up(files, databaseUrl = databaseUrl, ttlSeconds = 300)
        assertTrue(up.ok, "chain failed to start: ${up.notes}")
        assertEquals("started", up.status)
        val brid = up.brid!!
        val apiUrl = up.apiUrl!!
        assertEquals(64, brid.length, "BRID must be 32 bytes hex: $brid")

        runBlocking {
            HttpClient(CIO).use { client ->
                // The node reports the brid for chain iid 0.
                val reportedBrid = client.get("$apiUrl/brid/iid_0").bodyAsText()
                assertEquals(brid, reportedBrid.trim().uppercase().removePrefix("\"").removeSuffix("\"").uppercase())

                // A real Rell query over REST answers from the running chain.
                val count = client.post("$apiUrl/query/$brid") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"type":"book_count"}""")
                }.bodyAsText()
                assertEquals("0", count.trim(), "book_count on the fresh chain")

                val books = client.post("$apiUrl/query/$brid") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"type":"all_books"}""")
                }.bodyAsText()
                assertEquals("[]", books.trim().replace(" ", ""), "all_books on the fresh chain")
            }
        }

        // A real WRITE: sign a transaction with the dev key, post it, await a
        // block - proves the single-signer node actually builds blocks.
        val clientConfig = net.postchain.client.config.PostchainClientConfig(
            blockchainRid = net.postchain.common.BlockchainRid.buildFromHex(brid),
            endpointPool = net.postchain.client.request.EndpointPool.singleUrl(apiUrl),
            signers = listOf(
                net.postchain.crypto.KeyPair(
                    net.postchain.crypto.PubKey(up.nodePubkey!!.hexStringToByteArray()),
                    net.postchain.crypto.PrivKey(LocalChain.DEV_PRIV_KEY_HEX.hexStringToByteArray())
                )
            )
        )
        net.postchain.client.impl.PostchainClientProviderImpl().createClient(clientConfig).use { pcClient ->
            val txResult = pcClient.transactionBuilder()
                .addOperation(
                    "add_book",
                    net.postchain.gtv.GtvFactory.gtv("978-3-16-148410-0"),
                    net.postchain.gtv.GtvFactory.gtv("Rell for Agents")
                )
                .addNop()
                .postAwaitConfirmation()
            assertEquals(
                net.postchain.common.tx.TransactionStatus.CONFIRMED,
                txResult.status,
                "transaction must confirm in a block: ${txResult.rejectReason}"
            )
        }

        runBlocking {
            HttpClient(CIO).use { client ->
                val countAfter = client.post("$apiUrl/query/$brid") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"type":"book_count"}""")
                }.bodyAsText()
                assertEquals("1", countAfter.trim(), "book_count after the confirmed tx")
            }
        }

        // Idempotency: same sources return the running chain, not a restart.
        val again = LocalChain.up(files, databaseUrl = databaseUrl, ttlSeconds = 300)
        assertTrue(again.ok, again.notes)
        assertEquals("already_running", again.status)
        assertEquals(brid, again.brid)

        // status reports it too.
        val status = LocalChain.status()
        assertEquals("running", status.status)
        assertEquals(brid, status.brid)

        // down stops it and the API port stops answering.
        val down = LocalChain.down()
        assertEquals("stopped", down.status)
        assertEquals("not_running", LocalChain.status().status)
    }
}
