package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.AssetDistributionStrategy
import org.chromia.tools.DappScaffold
import org.chromia.tools.FetchDocumentStrategy
import org.chromia.tools.FilterBlockchainsStrategy
import org.chromia.tools.RagStore
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.WriteDeploymentConfigStrategy
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.TimeUnit

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

    /**
     * The production repository pointed at a REAL closed loopback port. Every
     * test below that takes it asserts a validation failure raised BEFORE any
     * call, or drives a docs tool that never touches the network - so a tool
     * that unexpectedly reached for it would fail with a genuine
     * ConnectException instead of being handed an answer a fixture invented.
     * This replaced `RecordingRepository`, a double of our own repository.
     */
    private val repo = McpTestSupport.offlineRepository()
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

    // DELETED 2026-09-07 (zero-doubles): absentAndValidFilterArgumentsStillWork
    // asserted that an ABSENT filter stays "no filter" and that a VALID boolean
    // or string array still filters. It read both back off a RecordingRepository,
    // so it could only restate the strategy's own call.
    // Both halves are asserted live now, where the explorer is the judge:
    // ToolExecutorStrategiesTest.liveJsonNullFiltersAreAbsentAndTheLiteralStringNullIsAValue
    // (absent/JSON-null filters do not filter) and
    // ToolExecutorStrategiesTest.liveFilterBlockchainsFiltersByNameAndSystem plus
    // ToolExecutorRemainingToolsTest's live asset-distribution filters (a valid
    // boolean and a valid list really do narrow the answer).

    // ---------------------------------------------------------------- F2

    // DELETED 2026-09-07 (zero-doubles): dappQueryNullsPreservedInArraysAndNestedObjects
    // and dappQueryNullsReachGtvAsGtvNull asserted that an explicit JSON null
    // survives as a Kotlin null and reaches the chain as GtvNull - the first by
    // reading the arguments back off a RecordingRepository, the second by handing
    // PostchainClientService a trailing-lambda query client that inspected the Gtv
    // and then produced the answer itself. Both are restatements: the test wrote
    // the expectation and the test checked it.
    //
    // What covers it now: ToolExecutorStrategiesTest
    // .chromiaDappQueryNestedListMapArgsAreBoundByTheLiveChain sends
    // `ft4.get_assets_filtered` a nested struct whose `symbol` and `type` members
    // are explicit JSON nulls, next to a top-level `page_cursor` null, and Rell
    // BINDS it on the live Economy Chain - a dropped null leaves the struct
    // incomplete and cannot bind at all.
    //
    // NOT covered any more, and now unverified: a null as a LIST ELEMENT keeping
    // the list's length ([1, null, 2] staying three long). No query on the live
    // Economy Chain takes a list with nullable elements, so there is no real
    // input that can produce it.

    // ---------------------------------------------------------------- F3

    @Test
    fun fetchWithFailedIndexLoaderReportsUnavailableNotNotFound(@TempDir tempDir: Path) = runBlocking {
        // A real air-gapped configuration: registry download enabled, no remote
        // URL configured, and no local file at the path - so the index is
        // genuinely unavailable rather than a lambda saying it is.
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = tempDir.resolve("missing-embeddings.json"),
            remoteUrls = emptyList(),
            // Explicit: the default is the developer's real ~150 MB
            // ~/.chromia-mcp cache, which no test may read or overwrite.
            cacheEmbeddingsPath = null
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
        // A real index built with the production embedding model. The old
        // fixture embedded three hand-written floats against the real model's
        // 384 dimensions - a store the production code could never have made.
        val store = TestDocsIndex.store(segment)
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

    // DELETED 2026-09-07 (zero-doubles): evictionDoesNotCloseClientMidQuery
    // asserted audit round 4 F4 - an evicted client must not be closed while a
    // query is still in flight on it, and must be closed once the grace window
    // (PostchainClientService.EVICTION_CLOSE_GRACE_MS) passes.
    //
    // No real input can produce it: seeing the close required handing the service
    // a `clientFactory` whose client the test could watch, and seeing it happen
    // DURING a query required a client that blocks on command. No real node can
    // be asked to hold one response open at a chosen moment while thirty-two
    // other clients are built, so both halves needed a substitute.
    //
    // What covers the rest: AuditConcurrencyRegressionTest
    // .theClientCacheStopsAtItsBound builds a client per real mainnet chain and
    // requires the cache to stop at MAX_CACHED_CLIENTS, so the eviction itself
    // still runs and is asserted. The DEFERRED CLOSE - the grace window, and the
    // fact that the evictee is closed at all - is now unverified.

    // ---------------------------------------------------------------- F5

    @Test
    fun interruptedDbRunDefersPermitReleaseUntilTheRealRunnerFinishes() {
        val databaseUrl = LiveEnv.requireDatabaseUrl(
            "a REAL run_rell_tests run has to still be holding the shared test database when its " +
                "caller gives up; a lambda that blocks on a latch is not a runner"
        )
        val baselineLeaked = RunRellTests.leakedRunners.get()
        assertEquals(1, RunRellTests.dbRunPermit.availablePermits(), "test needs an idle permit")

        // A real Rell test that takes a few seconds, so the caller can be
        // interrupted while the runner genuinely still owns the database. The
        // window it opens sits on top of the compile and chain setup the run
        // pays for before the loop even starts. The loop is pure
        // arithmetic - no entities, no blocks - so its cost is the interpreter's
        // and nothing else.
        val slowTest = mapOf(
            "slow_test.rell" to (
                "@test module;\n" +
                    "function test_slow() {\n" +
                    "    var i = 0;\n" +
                    "    var s = 0;\n" +
                    "    while (i < 2000000) { s += i; i += 1; }\n" +
                    "    assert_true(s > 0);\n" +
                    "}"
                )
        )
        val caller = Thread {
            runCatching { RunRellTests.run(slowTest, databaseUrl = databaseUrl) }
        }.apply { isDaemon = true; start() }

        // The permit is taken the moment the run starts.
        val startDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
        while (RunRellTests.dbRunPermit.availablePermits() == 1 && System.nanoTime() < startDeadline) {
            Thread.sleep(5)
        }
        assertEquals(0, RunRellTests.dbRunPermit.availablePermits(), "the real run never started")

        caller.interrupt()
        caller.join(30_000)
        assertFalse(caller.isAlive, "interrupted caller must return promptly")

        // The runner still owns the shared test database: the permit must NOT
        // have been released by the interrupted caller (audit round 4 F5).
        assertEquals(
            0,
            RunRellTests.dbRunPermit.availablePermits(),
            "DB permit was released while the real runner was still executing"
        )
        assertEquals(baselineLeaked + 1, RunRellTests.leakedRunners.get())

        // ...and it IS released once the real run finishes on its own.
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(300)
        while (RunRellTests.dbRunPermit.availablePermits() < 1 && System.nanoTime() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(
            1,
            RunRellTests.dbRunPermit.availablePermits(),
            "permit must be released once the runner finishes"
        )
        while (RunRellTests.leakedRunners.get() > baselineLeaked && System.nanoTime() < deadline) {
            Thread.sleep(50)
        }
        assertEquals(baselineLeaked, RunRellTests.leakedRunners.get())
    }

    // ---------------------------------------------------------------- F6

    // DELETED 2026-09-07 (zero-doubles): nullEmbeddingModelReportsIndexUnavailableNotEmptyResults
    // asserted audit round 4 F6 - a RagStore that cannot resolve an embedding
    // model must report the index as UNAVAILABLE rather than answer an empty
    // success. It produced the condition with `store.embeddingModelSpiLoader =
    // { null }`, a lambda standing in for langchain4j's SPI lookup.
    //
    // No real input can produce it in this build: the model is
    // `dev.langchain4j:langchain4j-easy-rag`'s bundled quantized BGE-small ONNX,
    // a compile-time dependency of the jar, so the SPI lookup cannot fail at
    // runtime. The only way to reach the branch is to remove the dependency,
    // which the shipped artifact never does.
    //
    // The sibling half is still covered for real:
    // fetchWithFailedIndexLoaderReportsUnavailableNotNotFound above reports an
    // unavailable index (no remote configured, no local file) as "unavailable"
    // rather than "not found". The MODEL-absent branch is now unverified.

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
