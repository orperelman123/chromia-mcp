package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
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

    private val repo = RecordingRepository()

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

    private fun rid(n: Int): BlockchainRid = BlockchainRid.buildFromHex("%064x".format(n))

    private fun countingService(
        created: AtomicInteger,
        closed: MutableList<String>
    ): PostchainClientService = PostchainClientService(
        ChromiaConfig(),
        clientFactory = { _, brid ->
            created.incrementAndGet()
            PostchainClientService.CachedQueryClient(
                object : net.postchain.client.core.PostchainQuery {
                    override fun query(name: String, args: Gtv): Gtv =
                        GtvFactory.gtv(mapOf("echo" to GtvFactory.gtv(name)))
                }
            ) { closed.add(brid.toHex()) }
        }
    )

    @Test
    fun repeatedQueriesToSameTargetReuseOneCachedClient() {
        val created = AtomicInteger()
        val closed = java.util.concurrent.CopyOnWriteArrayList<String>()
        val service = countingService(created, closed)
        repeat(5) {
            val result = service.executeBlockchainQuery("mainnet", rid(1), "q", emptyMap())
            assertTrue(result is NetworkResult.Success, result.toString())
        }
        assertEquals(1, created.get(), "every call used to build fresh clients (audit F2)")
        assertEquals(1, service.cachedClientCount())
        assertTrue(closed.isEmpty())
    }

    @Test
    fun distinctTargetsGetDistinctCachedClientsAndEvictionCloses() {
        val created = AtomicInteger()
        val closed = java.util.concurrent.CopyOnWriteArrayList<String>()
        val service = countingService(created, closed)
        service.evictionCloseGraceMs = 0 // close promptly; production defers 30s
        val total = PostchainClientService.MAX_CACHED_CLIENTS + 1
        (1..total).forEach { n ->
            val result = service.executeBlockchainQuery("mainnet", rid(n), "q", emptyMap())
            assertTrue(result is NetworkResult.Success, result.toString())
        }
        assertEquals(total, created.get())
        assertEquals(PostchainClientService.MAX_CACHED_CLIENTS, service.cachedClientCount())
        // LRU: the oldest entry (rid 1) was evicted and is eventually closed -
        // deferred behind the grace window so in-flight queries survive (round 4 F4).
        val deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10)
        while (closed.isEmpty() && System.nanoTime() < deadline) Thread.sleep(10)
        assertEquals(listOf(rid(1).toHex()), closed)
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

    @Test
    fun ragStoreRetriesFailedLoadAfterCooldown(@TempDir tempDir: Path) {
        val loaderCalls = AtomicInteger()
        val registryUp = AtomicBoolean(false)
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = tempDir.resolve("missing-embeddings.json"),
            registryLoader = {
                loaderCalls.incrementAndGet()
                if (registryUp.get()) {
                    dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<dev.langchain4j.data.segment.TextSegment>().also {
                        it.add(
                            dev.langchain4j.data.embedding.Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)),
                            dev.langchain4j.data.segment.TextSegment.from("RETRY_MARKER")
                        )
                    }
                } else {
                    throw RuntimeException("simulated GitLab outage")
                }
            }
        )
        // Startup load failed; the failure must NOT be cached forever (audit F5).
        assertNull(store.embeddingStore)
        assertEquals(1, loaderCalls.get())

        // Within the cooldown the loader is not hammered.
        assertNull(store.query("anything"))
        assertEquals(1, loaderCalls.get())

        // After the cooldown the next use retries and recovers. The retry is what
        // this test is about; the search itself cannot succeed here because the
        // fixture embeds 3 dimensions against the real model's 384, which the
        // store now reports as a retrieval failure instead of swallowing into an
        // empty result (audit F5's sibling fix). Either outcome proves the retry.
        registryUp.set(true)
        store.clock = { System.currentTimeMillis() + 2 * RagStore.LOAD_RETRY_COOLDOWN_MS }
        runCatching { store.query("anything") }
        assertEquals(2, loaderCalls.get(), "query must retry the load after the cooldown")
        assertNotNull(store.embeddingStore, "the retry must have loaded the store")
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
