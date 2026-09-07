package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import net.postchain.common.BlockchainRid
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.NetworkResult
import org.chromia.tools.RagStore
import org.chromia.tools.RellCheck
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RellSecurityCheckStrategy
import org.chromia.tools.RunRellTests
import org.chromia.tools.RunRellTestsStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Regressions for the 2026-09-01 concurrency/resource audit (F1-F5 + minors):
 * F1 Rell print() must never reach System.out (stdio JSON-RPC corruption),
 * F2 chromia_dapp_query client caching (per-call Apache HC5 pool leak),
 * F3 rell_security_check call-graph closure must not be O(N^3) + input cap,
 * F4 backslash path keys must behave like forward-slash keys,
 * F5 RAG store load failure must retry instead of staying dead until redeploy.
 */
class AuditConcurrencyRegressionTest {

    private val repo = McpTestSupport.offlineRepository()

    // ---------------------------------------------------------------- F1

    @Test
    fun rellPrintIsCapturedInResultAndNeverWrittenToStdout() = runBlocking {
        val originalOut = System.out
        val stdout = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(stdout, true, Charsets.UTF_8))
            val result = RunRellTestsStrategy().execute(
                callToolRequest(
                    name = "run_rell_tests",
                    arguments = buildJsonObject {
                        put(
                            "files",
                            buildJsonObject {
                                put(
                                    "print_test.rell",
                                    "@test module;\n" +
                                        "function test_prints() { print('HELLO_STDOUT_MARKER'); log('HELLO_LOG_MARKER'); assert_equals(1, 1); }"
                                )
                            }
                        )
                    }
                ),
                repo
            )
            assertTrue(result.isError != true, (result.content.first() as TextContent).text)
            val structured = result.structuredContent!!
            assertEquals(true, structured.getValue("ok").jsonPrimitive.content.toBoolean())
            val prints = structured.getValue("prints").jsonPrimitive.content
            assertTrue(prints.contains("HELLO_STDOUT_MARKER"), prints)
            assertTrue(prints.contains("HELLO_LOG_MARKER"), prints)
        } finally {
            System.setOut(originalOut)
        }
        // In --stdio mode System.out IS the JSON-RPC stream - a raw print line
        // corrupts the protocol (audit F1). Nothing may have leaked onto it.
        val leaked = stdout.toString(Charsets.UTF_8)
        assertFalse(leaked.contains("HELLO_STDOUT_MARKER"), "print() leaked to stdout: $leaked")
        assertFalse(leaked.contains("HELLO_LOG_MARKER"), "log() leaked to stdout: $leaked")
    }

    @Test
    fun printHeavyLoopRespectsCaptureCapAndNotesTruncation() {
        val result = RunRellTests.run(
            files = mapOf(
                "loud_test.rell" to (
                    "@test module;\n" +
                        "function test_loud() { var i = 0; while (i < 2000) { print('${"X".repeat(50)}'); i += 1; } }"
                    )
            ),
            databaseUrl = null
        )
        assertTrue(result.prints.isNotEmpty())
        assertTrue(
            result.prints.length <= RunRellTests.MAX_PRINT_CAPTURE_CHARS,
            "captured ${result.prints.length} chars, cap is ${RunRellTests.MAX_PRINT_CAPTURE_CHARS}"
        )
        assertTrue(result.notes.contains("truncated"), result.notes)
    }

    // ---------------------------------------------------------------- F2

    /**
     * ONE MAINNET SYSTEM NODE, ADDRESSED DIRECTLY.
     *
     * `resolveUrls` takes a node URL as well as a network name, so a single URL
     * makes the cache key `<url>|<brid>` and the client talks to one endpoint
     * instead of failing over across fourteen. Nothing here signs or spends:
     * `currentBlockHeight` is a read.
     */
    private val mainnetNodeUrl = ChromiaConfig().predefinedNetworks.getValue("mainnet").first()

    /** Real mainnet chain rids, asked of the live explorer. */
    private suspend fun liveMainnetChainRids(atLeast: Int): List<String> {
        val result = LiveChromia.repository().filterBlockchains(
            LiveChromia.EXPLORER_NETWORK,
            org.chromia.domain.BlockchainFilters(
                pagination = org.chromia.domain.PaginationParams(limit = 300)
            )
        )
        assertTrue(result is NetworkResult.Success, "the explorer must list mainnet chains: $result")
        val rows = (result as NetworkResult.Success).data
            .getValue("data").jsonObject.getValue("allBlockchains").jsonArray
        val rids = rows
            .filter { it.jsonObject.getValue("state").jsonPrimitive.content == "RUNNING" }
            .map { it.jsonObject.getValue("rid").jsonPrimitive.content }
            .distinct()
        assertTrue(
            rids.size >= atLeast,
            "this test needs $atLeast distinct real chains to fill the client cache; mainnet listed " +
                "${rids.size}. If mainnet really has fewer chains than the cache holds, the eviction " +
                "bound can no longer be reached with real targets and this test must go."
        )
        return rids
    }

    /**
     * Audit F2: every chromia_dapp_query used to build a fresh
     * StandardChromiaClient (each with its own Apache HC5 pool) and close none.
     *
     * This used to be asserted with `clientFactory = { ... }`, a lambda that
     * counted its own invocations and handed back a client that answered from
     * the test. That is a double of the thing whose caching is the claim. The
     * REAL client against the REAL Economy Chain proves the same property
     * through `cachedClientCount()`: five successful queries, one client.
     */
    @Test
    fun repeatedQueriesToTheSameChainReuseOneCachedClient() {
        LiveChromia.requireLive("queries the live Economy Chain five times and counts the cached clients")
        val service = PostchainClientService(ChromiaConfig())
        repeat(5) { attempt ->
            val result = service.executeBlockchainQuery(
                LiveChromia.NETWORK, LiveChromia.economyChainRid, "get_chr_asset", emptyMap()
            )
            assertTrue(result is NetworkResult.Success, "query ${attempt + 1} failed: $result")
        }
        assertEquals(
            1, service.cachedClientCount(),
            "five queries to one chain must share one client - before audit F2 each call built its own"
        )
    }

    /**
     * The LRU bound, with real clients for real chains.
     *
     * `MAX_CACHED_CLIENTS + 1` distinct mainnet chains are asked for their block
     * height through one node URL, so each is a distinct cache key. The cache
     * must stop at the bound: an unbounded map is the leak F2 was about, one
     * client per key forever.
     *
     * WHAT WENT WITH THE DOUBLE, and is now unverified: the eviction's DEFERRED
     * CLOSE (audit round 4 F4 - the evictee is closed after
     * EVICTION_CLOSE_GRACE_MS so an in-flight query survives). Observing a close
     * required handing the service a client whose close() the test could watch,
     * and observing a close DURING a query required a client that blocks on
     * command. Neither is a real client, and no real node can be asked to hold a
     * response open at a chosen moment, so `evictionDoesNotCloseClientMidQuery`
     * is deleted rather than faked: the grace window and the close-on-eviction
     * are exercised in production and asserted nowhere.
     */
    @Test
    fun theClientCacheStopsAtItsBound() = runBlocking {
        LiveChromia.requireLive("opens a client per real mainnet chain until the LRU bound is reached")
        val rids = liveMainnetChainRids(PostchainClientService.MAX_CACHED_CLIENTS + 1)
        val service = PostchainClientService(ChromiaConfig())
        // A Success means a client was really built for that chain and really
        // answered, so counting answers counts distinct cache keys - which the
        // cache size itself cannot do once it saturates at the bound.
        var answered = 0
        for (rid in rids) {
            val height = service.currentBlockHeight(mainnetNodeUrl, BlockchainRid.buildFromHex(rid))
            if (height is NetworkResult.Success) answered++
            if (answered > PostchainClientService.MAX_CACHED_CLIENTS) break
        }
        assertTrue(
            answered > PostchainClientService.MAX_CACHED_CLIENTS,
            "only $answered of ${rids.size} running mainnet chains answered a height through " +
                "$mainnetNodeUrl, so the cache bound was never crossed and this test proved nothing"
        )
        assertEquals(
            PostchainClientService.MAX_CACHED_CLIENTS, service.cachedClientCount(),
            "the cache must be bounded - it held ${service.cachedClientCount()} clients after $created " +
                "distinct chains"
        )
    }

    // ---------------------------------------------------------------- F3

    @Test
    fun deepCallChainClosureCompletesInBoundedTimeWithCorrectPropagation() {
        val depth = 500
        val source = buildString {
            // Mutation chain: f0 mutates, f_i calls f_(i-1).
            append("function f0(x: integer) { create item(name = x); }\n")
            for (i in 1 until depth) append("function f$i(x: integer) { f${i - 1}(x); }\n")
            // Auth chain: g0 authenticates, g_i calls g_(i-1).
            append("function g0() { auth.authenticate(); }\n")
            for (i in 1 until depth) append("function g$i() { g${i - 1}(); }\n")
            append("operation op_deep(v: integer) { require(v > 0); f${depth - 1}(v); }\n")
            append("operation op_ok(v: integer) { require(v > 0); g${depth - 1}(); f${depth - 1}(v); }\n")
        }
        val startedAt = System.nanoTime()
        val result = RellSecurityCheck.analyze(mapOf("main.rell" to source))
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        assertTrue(elapsedMs < 10_000, "closure took ${elapsedMs}ms - O(N^3) regression (audit F3)")
        // Mutation must propagate through the whole chain; auth likewise.
        val deepFindings = result.findings.filter { it.rule == "unauthenticated-mutation" }
        assertEquals(listOf("operation op_deep mutates state without an auth check"), deepFindings.map { it.text })
    }

    @Test
    fun oversizedInputIsRejectedCleanlyByAllThreeRellTools() = runBlocking {
        val big = "x".repeat(RellCheck.MAX_TOTAL_SOURCE_CHARS + 1)
        val checkError = assertThrows<IllegalArgumentException> {
            RellCheck.check(mapOf("main.rell" to big), null)
        }
        assertTrue(checkError.message!!.contains("exceeds"), checkError.message)

        val testsError = assertThrows<IllegalArgumentException> {
            RunRellTests.run(mapOf("main_test.rell" to big))
        }
        assertTrue(testsError.message!!.contains("exceeds"), testsError.message)

        val security = RellSecurityCheckStrategy().execute(
            callToolRequest(
                name = "rell_security_check",
                arguments = buildJsonObject {
                    put("files", buildJsonObject { put("main.rell", big) })
                }
            ),
            repo
        )
        assertEquals(true, security.isError)
        assertTrue((security.content.first() as TextContent).text!!.contains("exceeds"))
    }

    // ---------------------------------------------------------------- F4

    @Test
    fun backslashPathKeysBehaveLikeForwardSlashKeys() {
        val forward = RellCheck.check(
            mapOf("src/main.rell" to "module;\nfunction f(): integer = 1;"),
            null
        )
        val backslash = RellCheck.check(
            mapOf("src\\main.rell" to "module;\nfunction f(): integer = 1;"),
            null
        )
        assertTrue(backslash.ok, backslash.errors.toString())
        assertEquals(forward.modules, backslash.modules)

        val tests = RunRellTests.run(
            files = mapOf(
                "src\\main.rell" to "module;\nfunction double(x: integer): integer = x * 2;",
                "src\\main_test.rell" to "@test module;\nimport main;\nfunction test_double() { assert_equals(main.double(2), 4); }"
            ),
            databaseUrl = null
        )
        assertTrue(tests.ok, tests.cases.toString())
        assertEquals(1, tests.total)
    }

    @Test
    fun mixedSeparatorCollisionIsRejected() {
        val error = assertThrows<IllegalArgumentException> {
            RellCheck.check(
                mapOf(
                    "src\\a.rell" to "module;",
                    "src/a.rell" to "module;"
                ),
                null
            )
        }
        val message = error.message!!
        assertTrue(
            message.contains("collision") || message.contains("resolve to the same file"),
            message
        )
    }

    // ---------------------------------------------------------------- F5

    /**
     * Audit F5: a failed index load must NOT be cached forever - the store
     * retries after a cooldown instead of staying dead until a redeploy.
     *
     * The failure used to be a `registryLoader` lambda that threw
     * "simulated GitLab outage" and then stopped throwing. That lambda replaced
     * the entire download path, so nothing it proved involved HTTP at all.
     *
     * Here the store is pointed at a REAL local HTTP server (the same
     * `remoteUrls` list an operator fills with `CHROMIA_EMBEDDINGS_URL`), which
     * answers 503 and then serves a real index file written by the production
     * writer. The client, the streaming parse and the fallback order are the
     * production ones; only the peer is local.
     */
    @Test
    fun ragStoreRetriesAFailedIndexDownloadAfterTheCooldown(@TempDir tempDir: Path) {
        val indexFile = TestDocsIndex.persist(
            tempDir.resolve("published-embeddings.json"),
            TextSegment.from(
                "RETRY_MARKER: the index served on the retry.",
                Metadata.from("file_name", "retry.md")
            )
        )
        val serving = AtomicBoolean(false)
        val requests = AtomicInteger()
        val server = embeddedServer(ServerCIO, port = 0) {
            routing {
                get("/embeddings.json") {
                    requests.incrementAndGet()
                    if (serving.get()) {
                        call.respondBytes(Files.readAllBytes(indexFile), ContentType.Application.Json)
                    } else {
                        call.respond(HttpStatusCode.ServiceUnavailable, "index is being rebuilt")
                    }
                }
            }
        }.start(wait = false)
        try {
            val port = runBlocking { server.engine.resolvedConnectors() }.first().port
            val store = RagStore(
                loadFromRegistry = true,
                localEmbeddingsPath = tempDir.resolve("missing-embeddings.json"),
                remoteUrls = listOf("http://127.0.0.1:$port/embeddings.json"),
                cacheEmbeddingsPath = null
            )
            // The startup download really failed; the failure must not be permanent.
            assertNull(store.embeddingStore)
            assertEquals(1, requests.get(), "the store must have tried the URL once at construction")

            // Inside the cooldown the server is not hammered.
            assertNull(store.query("anything"))
            assertEquals(1, requests.get())

            // Past the cooldown, with the server serving, the next use recovers.
            serving.set(true)
            store.clock = { System.currentTimeMillis() + 2 * RagStore.LOAD_RETRY_COOLDOWN_MS }
            val hits = store.query("RETRY_MARKER")
            assertEquals(2, requests.get(), "query must retry the download after the cooldown")
            assertNotNull(store.embeddingStore, "the retry must have loaded the store")
            assertNotNull(hits, "a loaded store answers")
        } finally {
            server.stop(0, 0)
        }
    }

    // ---------------------------------------------------------------- minors

    @Test
    fun leakedRunnerCeilingRefusesNewRunsUntilRestart() {
        val before = RunRellTests.leakedRunners.get()
        RunRellTests.leakedRunners.set(RunRellTests.MAX_LEAKED_RUNNERS)
        try {
            val error = assertThrows<IllegalStateException> {
                RunRellTests.run(
                    files = mapOf("t.rell" to "@test module;\nfunction test_x() { assert_equals(1, 1); }"),
                    databaseUrl = null
                )
            }
            assertTrue(error.message!!.contains("restart"), error.message)
        } finally {
            RunRellTests.leakedRunners.set(before)
        }
    }
}
