package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvNull
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.NetworkResult
import org.chromia.tools.AssetDistributionStrategy
import org.chromia.tools.DappInteractionStrategy
import org.chromia.tools.DappScaffold
import org.chromia.tools.FetchDocumentStrategy
import org.chromia.tools.FilterBlockchainsStrategy
import org.chromia.tools.RagStore
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.SearchDocsStrategy
import org.chromia.tools.WriteDeploymentConfigStrategy
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Regressions for the 2026-09-01 audit round 4:
 * F1 wrong-JSON-type optional filters must be validation errors, not silently
 *    ignored (network-wide data returned as filtered success),
 * F2 chromia_dapp_query must preserve explicit JSON nulls as GtvNull,
 * F3 fetch with an unloaded docs index must say "unavailable", not "not found",
 * F4 client LRU eviction must not close a client mid-query,
 * F5 an interrupted run_rell_tests must not release the DB permit while the
 *    runner may still own the database,
 * F6 a missing embedding model must report "unavailable", not empty results,
 * F7 minors: write_deployment_config name validation, security-check same-name
 *    call-graph direction, BoundedPrinter exact-fit truncation flag.
 */
class AuditRound4RegressionTest {

    private val repo = RecordingRepository()
    private val validBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    // ---------------------------------------------------------------- F1

    @Test
    fun stringForArrayFilterIsValidationErrorNotNetworkWideData() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("brids", "XYZ")
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("brids"), error.message)
        assertTrue(error.message!!.contains("array of strings"), error.message)
        assertTrue(error.message!!.contains("string"), error.message)
    }

    @Test
    fun objectForArrayFilterIsValidationError() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                AssetDistributionStrategy().execute(
                    callToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject {
                            put("assetId", "chr")
                            put("accountTypes", buildJsonObject { put("a", "b") })
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("accountTypes"), error.message)
        assertTrue(error.message!!.contains("array of strings"), error.message)
        assertTrue(error.message!!.contains("object"), error.message)
    }

    @Test
    fun wrongTypeBooleanFilterIsValidationError() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                FilterBlockchainsStrategy().execute(
                    callToolRequest(
                        name = "filter_blockchains",
                        arguments = buildJsonObject { put("system", "yes") }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("system"), error.message)
        assertTrue(error.message!!.contains("boolean"), error.message)
    }

    @Test
    fun absentAndValidFilterArgumentsStillWork() = runBlocking {
        // Absent stays "no filter".
        FilterBlockchainsStrategy().execute(
            callToolRequest(name = "filter_blockchains", arguments = buildJsonObject {}),
            repo
        )
        assertNull(repo.lastBlockchainFilters?.system)

        // Valid boolean still filters.
        FilterBlockchainsStrategy().execute(
            callToolRequest(
                name = "filter_blockchains",
                arguments = buildJsonObject { put("system", true) }
            ),
            repo
        )
        assertEquals(true, repo.lastBlockchainFilters?.system)

        // Valid array still filters; absent list stays null.
        AssetDistributionStrategy().execute(
            callToolRequest(
                name = "get_asset_distribution",
                arguments = buildJsonObject {
                    put("assetId", "chr")
                    put("brids", buildJsonArray { add("brid-1"); add("brid-2") })
                }
            ),
            repo
        )
        assertEquals(listOf("brid-1", "brid-2"), repo.lastAssetFilters?.brids)
        assertNull(repo.lastAssetFilters?.accountTypes)
    }

    // ---------------------------------------------------------------- F2

    @Test
    fun dappQueryNullsPreservedInArraysAndNestedObjects() = runBlocking {
        DappInteractionStrategy().execute(
            callToolRequest(
                name = "chromia_dapp_query",
                arguments = buildJsonObject {
                    put("blockchainRid", validBrid)
                    put("query", "q")
                    put(
                        "arguments",
                        buildJsonObject {
                            put("top", JsonNull)
                            put("list", buildJsonArray { add(1); add(JsonNull); add(2) })
                            put(
                                "obj",
                                buildJsonObject {
                                    put("a", JsonNull)
                                    put("b", "x")
                                }
                            )
                        }
                    )
                }
            ),
            repo
        )
        val args = repo.lastDapp!!.arguments
        assertTrue("top" in args)
        assertNull(args["top"])
        assertEquals(listOf<Any?>(1, null, 2), args["list"], "[1,null,2] must keep length 3")
        assertEquals(mapOf<String, Any?>("a" to null, "b" to "x"), args["obj"])
    }

    @Test
    fun dappQueryNullsReachGtvAsGtvNull() {
        var asserted = false
        val service = PostchainClientService(ChromiaConfig()) { _, _, args ->
            val dict = args.asDict()
            assertEquals(GtvNull, dict.getValue("top"))
            val list = dict.getValue("list").asArray()
            assertEquals(3, list.size)
            assertEquals(1L, list[0].asInteger())
            assertEquals(GtvNull, list[1])
            assertEquals(2L, list[2].asInteger())
            assertEquals(GtvNull, dict.getValue("obj").asDict().getValue("a"))
            asserted = true
            GtvFactory.gtv(mapOf("ok" to GtvFactory.gtv(true)))
        }
        val result = service.executeBlockchainQuery(
            "mainnet",
            BlockchainRid.buildFromHex(validBrid),
            "q",
            mapOf(
                "top" to null,
                "list" to listOf(1, null, 2),
                "obj" to mapOf("a" to null)
            )
        )
        assertTrue(result is NetworkResult.Success, result.toString())
        assertTrue(asserted)
    }

    // ---------------------------------------------------------------- F3

    @Test
    fun fetchWithFailedIndexLoaderReportsUnavailableNotNotFound(@TempDir tempDir: Path) = runBlocking {
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = tempDir.resolve("missing-embeddings.json"),
            registryLoader = { null }
        )
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            callToolRequest(name = "fetch", arguments = buildJsonObject { put("id", "abc123") }),
            repo
        )
        assertEquals(true, result.isError)
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("abc123", payload["id"]!!.jsonPrimitive.content)
        val error = payload["error"]!!.jsonPrimitive.content
        assertTrue(error.contains("index is unavailable"), error)
        assertFalse(error.contains("not found"), error)
    }

    @Test
    fun fetchUnknownIdWithHealthyIndexStillReportsNotFound() = runBlocking {
        val segment = TextSegment.from(
            "FT4 auth descriptors overview.",
            Metadata.from("file_name", "ft4-auth.md")
        )
        val fixture = InMemoryEmbeddingStore<TextSegment>().also {
            it.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), segment)
        }
        val store = RagStore(loadFromRegistry = false, initialStore = fixture)
        val strategy = FetchDocumentStrategy(CompletableDeferred(store))

        val known = strategy.execute(
            callToolRequest(name = "fetch", arguments = buildJsonObject { put("id", segmentId(segment)) }),
            repo
        )
        assertTrue(known.isError != true)

        val unknown = strategy.execute(
            callToolRequest(name = "fetch", arguments = buildJsonObject { put("id", "does-not-exist") }),
            repo
        )
        assertEquals(true, unknown.isError)
        val text = (unknown.content.first() as TextContent).text!!
        assertTrue(text.contains("Documentation not found"), text)
        assertFalse(text.contains("index is unavailable"), text)
    }

    // ---------------------------------------------------------------- F4

    private fun rid(n: Int): BlockchainRid = BlockchainRid.buildFromHex("%064x".format(n))

    @Test
    fun evictionDoesNotCloseClientMidQuery() {
        val firstClientClosed = AtomicBoolean(false)
        val queryStarted = CountDownLatch(1)
        val releaseQuery = CountDownLatch(1)
        val service = PostchainClientService(
            ChromiaConfig(),
            clientFactory = { _, brid ->
                val blockFirst = brid == rid(1)
                PostchainClientService.CachedQueryClient(
                    object : net.postchain.client.core.PostchainQuery {
                        override fun query(name: String, args: net.postchain.gtv.Gtv): net.postchain.gtv.Gtv {
                            if (blockFirst) {
                                queryStarted.countDown()
                                releaseQuery.await()
                            }
                            return GtvFactory.gtv(mapOf("echo" to GtvFactory.gtv(name)))
                        }
                    }
                ) { if (blockFirst) firstClientClosed.set(true) }
            }
        )
        service.evictionCloseGraceMs = 5_000

        val inFlight = Thread {
            service.executeBlockchainQuery("mainnet", rid(1), "slow", emptyMap())
        }.apply { isDaemon = true; start() }
        assertTrue(queryStarted.await(10, TimeUnit.SECONDS), "first query never started")

        // Evict rid(1) while its query is still in flight.
        (2..PostchainClientService.MAX_CACHED_CLIENTS + 2).forEach { n ->
            val result = service.executeBlockchainQuery("mainnet", rid(n), "q", emptyMap())
            assertTrue(result is NetworkResult.Success, result.toString())
        }
        assertFalse(
            firstClientClosed.get(),
            "evicted client was closed while its query was still in flight (audit round 4 F4)"
        )

        releaseQuery.countDown()
        inFlight.join(10_000)
        assertFalse(inFlight.isAlive)

        // The deferred close still happens after the grace window.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (!firstClientClosed.get() && System.nanoTime() < deadline) Thread.sleep(20)
        assertTrue(firstClientClosed.get(), "evicted client must eventually be closed")
    }

    // ---------------------------------------------------------------- F5

    @Test
    fun interruptedDbRunDefersPermitReleaseUntilRunnerFinishes() {
        val baselineLeaked = RunRellTests.leakedRunners.get()
        assertEquals(1, RunRellTests.dbRunPermit.availablePermits(), "test needs an idle permit")
        val runnerStarted = CountDownLatch(1)
        val releaseRunner = CountDownLatch(1)
        RunRellTests.runnerOverrideForTests = {
            runnerStarted.countDown()
            // Uninterruptible: future.cancel(true) interrupts the runner thread,
            // and this test needs the runner to keep "owning the database".
            while (releaseRunner.count > 0) {
                try {
                    releaseRunner.await()
                } catch (_: InterruptedException) {
                }
            }
        }
        try {
            val caller = Thread {
                runCatching {
                    RunRellTests.run(
                        mapOf("t_test.rell" to "@test module;\nfunction test_x() { assert_equals(1, 1); }"),
                        databaseUrl = "jdbc:postgresql://localhost:5432/unused"
                    )
                }
            }.apply { start() }
            assertTrue(runnerStarted.await(10, TimeUnit.SECONDS), "runner never started")

            caller.interrupt()
            caller.join(10_000)
            assertFalse(caller.isAlive, "interrupted caller must return promptly")

            // The runner still owns the shared test database: the permit must NOT
            // have been released by the interrupted caller (audit round 4 F5).
            assertEquals(
                0,
                RunRellTests.dbRunPermit.availablePermits(),
                "DB permit was released while the runner was still executing"
            )
            assertEquals(baselineLeaked + 1, RunRellTests.leakedRunners.get())

            releaseRunner.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (RunRellTests.dbRunPermit.availablePermits() < 1 && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }
            assertEquals(
                1,
                RunRellTests.dbRunPermit.availablePermits(),
                "permit must be released once the runner finishes"
            )
            while (RunRellTests.leakedRunners.get() > baselineLeaked && System.nanoTime() < deadline) {
                Thread.sleep(10)
            }
            assertEquals(baselineLeaked, RunRellTests.leakedRunners.get())
        } finally {
            RunRellTests.runnerOverrideForTests = null
            releaseRunner.countDown()
        }
    }

    // ---------------------------------------------------------------- F6

    @Test
    fun nullEmbeddingModelReportsIndexUnavailableNotEmptyResults() = runBlocking {
        val fixture = InMemoryEmbeddingStore<TextSegment>().also {
            it.add(
                Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)),
                TextSegment.from("Rell compiler overview.", Metadata.from("file_name", "rell.md"))
            )
        }
        val store = RagStore(loadFromRegistry = false, initialStore = fixture)
        store.embeddingModelSpiLoader = { null }
        assertNull(store.query("rell"), "no model must mean unavailable, not empty success")

        val search = SearchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(name = "search", arguments = buildJsonObject { put("query", "rell") }),
            repo
        )
        assertEquals(true, search.isError)
        val text = (search.content.first() as TextContent).text!!
        assertTrue(text.contains("index is unavailable"), text)
    }

    // ---------------------------------------------------------------- F7 minors

    @Test
    fun writeDeploymentConfigInvalidNameIsErrorNotSilentHello() {
        val error = assertThrows<IllegalArgumentException> {
            runBlocking {
                WriteDeploymentConfigStrategy().execute(
                    callToolRequest(
                        name = "write_deployment_config",
                        arguments = buildJsonObject {
                            put("network", "testnet")
                            put("name", "Bad-Name")
                        }
                    ),
                    repo
                )
            }
        }
        assertTrue(error.message!!.contains("Bad-Name"), error.message)
        assertTrue(error.message!!.contains("[a-z][a-z0-9_]{0,31}"), error.message)
    }

    @Test
    fun requireValidNameKeepsDefaultForAbsentAndValidNames() {
        assertEquals("hello", DappScaffold.requireValidName(null))
        assertEquals("hello", DappScaffold.requireValidName("  "))
        assertEquals("wallet", DappScaffold.requireValidName(" Wallet "))
    }

    @Test
    fun sameNamedNonAuthHelperDoesNotCountAsAuth() {
        // a.rell has an auth-establishing check_user(); b.rell has a same-named
        // helper that does NOT authenticate. The name must not count as auth
        // (auth only if ALL definitions establish auth) - previously any auth
        // definition suppressed unauthenticated-mutation findings network-wide.
        val result = RellSecurityCheck.analyze(
            linkedMapOf(
                "a.rell" to "module;\nfunction check_user() { auth.authenticate(); }\n",
                "b.rell" to "module;\nentity note { key id: text; }\n" +
                    "function check_user() { require(true, 'noop'); }\n" +
                    "operation add_note(id: text) { check_user(); create note(id); }\n"
            )
        )
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" && it.file == "b.rell" },
            "op mutating behind an ambiguous same-named helper must be flagged: ${result.findings}"
        )
    }

    @Test
    fun mutatingHelperHiddenByLaterSameNameIsStillMutating() {
        // a.rell's do_write mutates; b.rell declares a benign do_write later.
        // The name must stay mutating (mutating if ANY definition mutates) -
        // previously the later definition clobbered the map and hid the mutation.
        val result = RellSecurityCheck.analyze(
            linkedMapOf(
                "a.rell" to "module;\nentity item { key id: text; }\n" +
                    "function do_write(id: text) { create item(id); }\n",
                "b.rell" to "module;\nfunction do_write() { require(true, 'noop'); }\n" +
                    "operation go(id: text) { do_write(id); }\n"
            )
        )
        assertTrue(
            result.findings.any { it.rule == "unauthenticated-mutation" && it.file == "b.rell" },
            "op mutating via a clobbered same-named helper must be flagged: ${result.findings}"
        )
    }

    @Test
    fun boundedPrinterExactFitIsNotReportedAsTruncated() {
        val printer = RunRellTests.BoundedPrinter(10)
        printer.print("0123456789")
        assertFalse(printer.truncated, "exact-fit output was not truncated")
        assertEquals("0123456789", printer.text())

        // The next print does exceed the cap.
        printer.print("x")
        assertTrue(printer.truncated)
        assertEquals("0123456789", printer.text())

        val overCap = RunRellTests.BoundedPrinter(5)
        overCap.print("123456")
        assertTrue(overCap.truncated)
        assertEquals("12345", overCap.text())
    }
}
