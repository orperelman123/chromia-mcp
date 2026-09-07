package org.chromia

import org.chromia.tools.propertiesOrEmpty

import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.LocalChain
import org.chromia.tools.LocalChainStrategy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import net.postchain.common.toHex
import org.junit.jupiter.api.Test
import java.net.ServerSocket

/**
 * Coverage for local_chain_up: input validation, blockchain-config generation,
 * planning (compile -> BRID), the failure diagnostics, the tool's schemas, and
 * the registry's idempotency/restart/down lifecycle.
 *
 * The lifecycle used to run on a test starter seam that fabricated a
 * `LocalChain.Running` without a node; it now starts REAL nodes against the
 * real PostgreSQL ([LiveEnv.requireDatabaseUrl]), because a registry that only
 * ever held a fake Running proved nothing about the registry that production
 * fills with real ones. Everything above the lifecycle section is pure and
 * still needs no database. The database-backed query/transaction end-to-end
 * path lives in LocalChainIntegrationTest and LocalChainRestBridgeTest.
 */
class LocalChainToolTest {

    private val repo = McpTestSupport.offlineRepository()

    private val goodFiles = mapOf(
        "main.rell" to "module;\nentity item { key name; }\nquery item_count() = (item @* {}).size();"
    )

    private fun run(arguments: kotlinx.serialization.json.JsonObject) = runBlocking {
        LocalChainStrategy().execute(
            callToolRequest(name = "local_chain_up", arguments = arguments),
            repo
        )
    }

    private fun errorText(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): String =
        (result.content.first() as TextContent).text!!

    /**
     * Other test classes still install node-starter overrides on this global
     * object. Clearing them BEFORE each test is what makes the real-node
     * lifecycle test below honest: a leaked override from another class would
     * otherwise turn a "real" start into someone else's fabrication without
     * anything failing.
     */
    @BeforeEach
    fun requireARealStarter() {
        LocalChain.starterOverrideForTests = null
        LocalChain.nodeStarterOverrideForTests = null
        LocalChain.startTimeoutSecondsOverrideForTests = null
    }

    @AfterEach
    fun tearDown() {
        LocalChain.stopAll()
        LocalChain.starterOverrideForTests = null
        LocalChain.nodeStarterOverrideForTests = null
        LocalChain.startTimeoutSecondsOverrideForTests = null
    }

    // ------------------------------------------------------------------
    // Strategy validation (no chain started)
    // ------------------------------------------------------------------

    @Test
    fun invalidActionIsRejected() {
        val result = run(buildJsonObject { put("action", "restart") })
        assertTrue(result.isError == true)
        assertTrue(errorText(result).contains("up, down, status"), errorText(result))
    }

    @Test
    fun upWithoutFilesGivesGuidance() {
        val result = run(buildJsonObject { })
        assertTrue(result.isError == true)
        assertTrue(errorText(result).contains("`files` map"), errorText(result))
    }

    @Test
    fun nonStringFileValuesAreRejected() {
        val result = run(
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", 42) })
            }
        )
        assertTrue(result.isError == true)
        assertTrue(errorText(result).contains("main.rell"), errorText(result))
    }

    @Test
    fun malformedModuleArgsAreRejected() {
        val result = run(
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", "module;") })
                put("moduleArgs", buildJsonObject { put("lib.ft4.core.accounts", "not-an-object") })
            }
        )
        assertTrue(result.isError == true)
        assertTrue(errorText(result).contains("lib.ft4.core.accounts"), errorText(result))
    }

    @Test
    fun apiPortOutOfRangeIsRejected() {
        val result = run(
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", "module;") })
                put("apiPort", 70000)
            }
        )
        assertTrue(result.isError == true)
        assertTrue(errorText(result).contains("apiPort"), errorText(result))
    }

    @Test
    fun nonPostgresDatabaseUrlIsRejected() {
        val result = run(
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", "module;") })
                put("databaseUrl", "jdbc:mysql://localhost/db")
            }
        )
        assertTrue(result.isError == true)
        assertTrue(errorText(result).contains("jdbc:postgresql"), errorText(result))
    }

    @Test
    fun statusAndDownWithoutRunningChainAreCleanNoOps() {
        val status = run(buildJsonObject { put("action", "status") })
        assertTrue(status.isError != true)
        assertEquals("not_running", status.structuredContent!!.getValue("status").jsonPrimitive.content)

        val down = run(buildJsonObject { put("action", "down") })
        assertTrue(down.isError != true)
        assertEquals("not_running", down.structuredContent!!.getValue("status").jsonPrimitive.content)
    }

    @Test
    fun missingDatabaseIsAnActionableError() {
        val result = LocalChain.up(goodFiles, databaseUrl = null)
        assertFalse(result.ok)
        assertTrue(result.notes.contains(LocalChain.DATABASE_URL_ENV), result.notes)
        assertTrue(result.notes.contains("run_rell_tests"), result.notes)
    }

    // ------------------------------------------------------------------
    // Planning: compile -> blockchain config -> BRID (no database needed)
    // ------------------------------------------------------------------

    private val dbUrl = "jdbc:postgresql://localhost:5432/db?user=u&password=p"

    @Test
    fun prepareProducesFullBlockchainConfigAndDeterministicBrid() {
        val plan = LocalChain.prepare(goodFiles, dbUrl, emptyMap(), requestedApiPort = 7799)
        assertEquals(64, plan.brid.length, plan.brid)
        assertEquals(7799, plan.apiPort)
        assertTrue(plan.messagingPort in LocalChain.MESSAGING_PORT_RANGE)

        val dict = plan.configWithSigners.asDict()
        assertEquals(
            "net.postchain.gtx.GTXBlockchainConfigurationFactory",
            dict.getValue("configurationfactory").asString()
        )
        assertEquals(2L, dict.getValue("merkle_hash_version").asInteger())
        assertEquals(
            "net.postchain.base.BaseBlockBuildingStrategy",
            dict.getValue("blockstrategy").asDict().getValue("name").asString()
        )
        val gtx = dict.getValue("gtx").asDict()
        val modules = gtx.getValue("modules").asArray().map { it.asString() }
        assertTrue("net.postchain.rell.module.RellPostchainModuleFactory" in modules, modules.toString())
        assertTrue("net.postchain.gtx.StandardOpsGTXModule" in modules, modules.toString())
        val rell = gtx.getValue("rell").asDict()
        assertTrue(rell.getValue("sources").asDict().keys.any { it.endsWith("main.rell") })
        assertTrue(rell.getValue("modules").asArray().isNotEmpty())
        // Exactly one signer: the well-known CLI dev key.
        val signers = dict.getValue("signers").asArray()
        assertEquals(1, signers.size)
        assertEquals(plan.pubKeyHex, signers[0].asByteArray().toHex())
        assertEquals(LocalChain.DEV_PRIV_KEY_HEX, plan.privKeyHex)

        // Same sources -> same BRID (agents can rely on it across restarts).
        val plan2 = LocalChain.prepare(goodFiles, dbUrl, emptyMap(), requestedApiPort = 7799)
        assertEquals(plan.brid, plan2.brid)
    }

    @Test
    fun moduleArgsAreInjectedIntoTheConfig() {
        val files = mapOf(
            "main.rell" to "module;\nstruct module_args { greeting: text; }\nquery greet() = chain_context.args.greeting;"
        )
        val plan = LocalChain.prepare(
            files,
            dbUrl,
            mapOf("main" to mapOf("greeting" to kotlinx.serialization.json.JsonPrimitive("hi"))),
            requestedApiPort = null
        )
        val rell = plan.configWithSigners.asDict().getValue("gtx").asDict().getValue("rell").asDict()
        val moduleArgs = rell.getValue("moduleArgs").asDict()
        assertEquals("hi", moduleArgs.getValue("main").asDict().getValue("greeting").asString())
    }

    @Test
    fun compileErrorsPointAtRellCheck() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            LocalChain.prepare(mapOf("main.rell" to "module;\nquery broken() = undefined_name;"), dbUrl, emptyMap(), null)
        }
        assertTrue(e.message!!.contains("rell_check"), e.message)
        assertTrue(e.message!!.contains("main.rell"), e.message)
    }

    @Test
    fun testOnlyFilesCannotFormAChain() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            LocalChain.prepare(
                mapOf("tests/module.rell" to "@test module;\nfunction test_x() {}"),
                dbUrl, emptyMap(), null
            )
        }
        assertTrue(e.message!!.contains("app module"), e.message)
    }

    @Test
    fun testModulesAreExcludedFromTheChainConfig() {
        val files = goodFiles + mapOf("tests/module.rell" to "@test module;\nimport main;\nfunction test_x() {}")
        val plan = LocalChain.prepare(files, dbUrl, emptyMap(), null)
        val rellModules = plan.configWithSigners.asDict().getValue("gtx").asDict()
            .getValue("rell").asDict().getValue("modules").asArray().map { it.asString() }
        assertTrue(rellModules.none { it.contains("test") }, rellModules.toString())
    }

    @Test
    fun pathTraversalAndNonRellFilesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalChain.prepare(mapOf("../evil.rell" to "module;"), dbUrl, emptyMap(), null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            LocalChain.prepare(mapOf("main.txt" to "module;"), dbUrl, emptyMap(), null)
        }
    }

    // ------------------------------------------------------------------
    // Pure helpers
    // ------------------------------------------------------------------

    @Test
    fun ttlIsClamped() {
        assertEquals(LocalChain.DEFAULT_TTL_SECONDS, LocalChain.boundedTtl(null))
        assertEquals(LocalChain.MIN_TTL_SECONDS, LocalChain.boundedTtl(1))
        assertEquals(LocalChain.MAX_TTL_SECONDS, LocalChain.boundedTtl(999_999))
        assertEquals(600L, LocalChain.boundedTtl(600))
    }

    @Test
    fun credentialsAreParsedFromJdbcUrl() {
        assertEquals(
            "alice" to "s3cr%t",
            LocalChain.credentialsFromJdbcUrl("jdbc:postgresql://h:5432/db?user=alice&password=s3cr%25t")
        )
        assertEquals(
            "postgres" to "postgres",
            LocalChain.credentialsFromJdbcUrl("jdbc:postgresql://h:5432/db")
        )
    }

    @Test
    fun fingerprintTracksSourcesArgsAndDatabase() {
        val base = LocalChain.fingerprint(goodFiles, emptyMap(), dbUrl)
        assertEquals(base, LocalChain.fingerprint(goodFiles, emptyMap(), dbUrl))
        assertNotEquals(base, LocalChain.fingerprint(goodFiles + ("x.rell" to "module;"), emptyMap(), dbUrl))
        assertNotEquals(
            base,
            LocalChain.fingerprint(
                goodFiles,
                mapOf("m" to mapOf("a" to kotlinx.serialization.json.JsonPrimitive(1))),
                dbUrl
            )
        )
        assertNotEquals(base, LocalChain.fingerprint(goodFiles, emptyMap(), "jdbc:postgresql://other/db"))
    }

    @Test
    fun freePortSkipsBusyPorts() {
        ServerSocket(0).use { taken ->
            // A range consisting only of the taken port fails with guidance...
            val e = assertThrows(IllegalStateException::class.java) {
                LocalChain.freePortIn(taken.localPort..taken.localPort)
            }
            assertTrue(e.message!!.contains("free port"), e.message)
        }
        // ...and the real range yields a bindable port.
        val port = LocalChain.freePortIn(LocalChain.API_PORT_RANGE)
        assertTrue(port in LocalChain.API_PORT_RANGE)
        ServerSocket(port).close()
    }

    @Test
    fun diagnosticsAreActionable() {
        val plan = LocalChain.prepare(goodFiles, dbUrl, emptyMap(), requestedApiPort = 7788)
        assertTrue(
            LocalChain.diagnose(RuntimeException("Database collation check failed, please initialize..."), plan)
                .contains("TEMPLATE template0")
        )
        assertTrue(
            LocalChain.diagnose(RuntimeException("Connection to localhost:5432 refused"), plan)
                .contains(LocalChain.DATABASE_URL_ENV)
        )
        assertTrue(
            LocalChain.diagnose(RuntimeException("FATAL: password authentication failed for user"), plan)
                .contains("user/password")
        )
        assertTrue(
            LocalChain.diagnose(RuntimeException("FATAL: database \"nope\" does not exist"), plan)
                .contains("CREATE DATABASE")
        )
        assertTrue(
            LocalChain.diagnose(RuntimeException("Address already in use: bind"), plan)
                .contains("7788")
        )
    }

    // ------------------------------------------------------------------
    // Registry lifecycle - REAL nodes, real database
    // ------------------------------------------------------------------

    /**
     * The registry contract an agent depends on, driven end to end with real
     * embedded Postchain nodes: identical sources reuse the RUNNING chain (same
     * Running instance - not merely the same reported BRID, which a restart
     * would also produce), changed sources really do replace it, and
     * status/down report the truth afterwards.
     *
     * The JSON-shape assertions in the middle are what
     * `upResultJsonShapeMatchesOutputSchema` used to check on a fabricated
     * result; they were folded in here rather than dropped, because
     * [LocalChain.UpResult.toJson] only emits brid/apiUrl/chainId/nodePubkey/
     * expiresInSeconds when they are present, so the claim needs a genuinely
     * started chain and a second real node start just to assert it would be
     * minutes of CI for nothing.
     */
    @Test
    fun upIsIdempotentForIdenticalSourcesAndRestartsOnChange() {
        val databaseUrl = LiveEnv.requireDatabaseUrl(
            "the local_chain_up registry lifecycle is driven with real embedded Postchain node starts"
        )

        val first = LocalChain.up(goodFiles, databaseUrl = databaseUrl, ttlSeconds = 120)
        assertTrue(first.ok, first.notes)
        assertEquals("started", first.status)
        assertTrue(first.expiresInSeconds!! in 100..120, first.expiresInSeconds.toString())
        assertTrue(first.notes.contains(first.apiUrl!!), first.notes)
        assertTrue(first.notes.contains("rell.get_app_structure"), first.notes)
        val startedChain = LocalChain.running
        assertNotNull(startedChain, "a started chain must be registered")

        val json = with(LocalChain) { first.toJson() }
        for (key in listOf("ok", "status", "brid", "apiUrl", "chainId", "nodePubkey", "expiresInSeconds", "notes")) {
            assertTrue(json.containsKey(key), "missing $key in ${json.keys}")
        }
        assertEquals("0", json.getValue("chainId").jsonPrimitive.content)

        val second = LocalChain.up(goodFiles, databaseUrl = databaseUrl, ttlSeconds = 120)
        assertEquals("already_running", second.status)
        assertEquals(first.brid, second.brid)
        assertEquals(first.apiUrl, second.apiUrl)
        assertSame(startedChain, LocalChain.running, "identical sources must not restart the chain")

        val third = LocalChain.up(goodFiles + ("extra.rell" to "module;"), databaseUrl = databaseUrl)
        assertTrue(third.ok, third.notes)
        assertEquals("started", third.status)
        assertNotSame(startedChain, LocalChain.running, "changed sources must restart the chain")
        assertNotEquals(first.brid, third.brid, "different sources are a different blockchain")

        assertEquals("running", LocalChain.status().status)
        assertEquals("stopped", LocalChain.down().status)
        assertEquals("not_running", LocalChain.status().status)
        assertEquals("not_running", LocalChain.down().status)
    }

    // DELETED 2026-09-07 (zero-doubles): upResultJsonShapeMatchesOutputSchema
    // asserted that UpResult.toJson emits every key the output schema
    // advertises. It stood on a fabricated LocalChain.Running from
    // starterOverrideForTests. The claim was NOT dropped - it is asserted in
    // upIsIdempotentForIdenticalSourcesAndRestartsOnChange above, against the
    // result of a real node start, which is the only state in which toJson
    // emits the optional keys at all.

    @Test
    fun localChainUpIsRegisteredWithSchemas() {
        val tool = org.chromia.tools.McpTools.allTools().first { it.name == "local_chain_up" }
        assertTrue(tool.description!!.contains("run_rell_tests"), "description should place the tool in the agent loop")
        assertTrue(tool.inputSchema.propertiesOrEmpty.keys.containsAll(
            listOf("files", "action", "moduleArgs", "ttlSeconds", "apiPort", "databaseUrl")
        ))
        assertTrue(tool.outputSchema!!.propertiesOrEmpty.keys.containsAll(listOf("ok", "status", "brid", "apiUrl", "notes")))
    }

    @Test
    fun descriptionIsHonestAboutServedEndpointsAndReuseFingerprint() {
        val tool = org.chromia.tools.McpTools.allTools().first { it.name == "local_chain_up" }
        val description = tool.description!!
        // The bridge serves a 6-endpoint SUBSET, not "the standard Postchain
        // REST API" - an agent expecting confirmation proofs or block endpoints
        // must learn that from the description, not from 404s.
        assertTrue(description.contains("subset of the Postchain REST API"), description)
        for (endpoint in listOf(
            "/brid/iid_0", "/query/{brid}", "/query_gtv/{brid}", "/tx/{brid}", "/tx/{brid}/{txRid}/status"
        )) {
            assertTrue(description.contains(endpoint), "description must name $endpoint")
        }
        assertFalse(description.contains("standard Postchain REST API"), description)
        // The reuse fingerprint covers sources AND moduleArgs AND databaseUrl -
        // the description must name all three restart triggers.
        assertTrue(description.contains("moduleArgs, or databaseUrl restarts it"), description)
    }

    @Test
    fun outputSchemaStatusHasNoPhantomErrorValue() {
        // ok=false becomes a tool error (toolErrorResult), so "error" never
        // reaches structuredContent - the schema must not advertise it.
        val tool = org.chromia.tools.McpTools.allTools().first { it.name == "local_chain_up" }
        val statusDescription = (tool.outputSchema!!.propertiesOrEmpty["status"] as kotlinx.serialization.json.JsonObject)
            .getValue("description").jsonPrimitive.content
        assertFalse(statusDescription.contains("| error"), statusDescription)
        for (status in listOf("started", "already_running", "running", "stopped", "not_running")) {
            assertTrue(statusDescription.contains(status), statusDescription)
        }
    }

    // ------------------------------------------------------------------
    // Abandoned-start lifecycle
    // ------------------------------------------------------------------

    // DELETED 2026-09-07 (zero-doubles): lateCompletingStartAfterTimeoutIsShutDownNotLeaked
    // asserted that a node start which outlives START_TIMEOUT_SECONDS, is
    // abandoned with future.cancel(true), and then COMPLETES anyway, has its
    // REST bridge closed and its node shut down by the cleanup queued behind it
    // in LocalChain.startBounded - instead of leaking a listening port and a
    // schema-sharing node forever.
    //
    // No real input can produce it. The test needed a node starter that (a)
    // swallows the cancellation interrupt, (b) completes on a schedule the test
    // chooses, and (c) hands back a LocalChain.Running holding a bridge the
    // test had already bound to a port it could watch - i.e. a fabricated
    // replacement for our own startNode (LocalChain.nodeStarterOverrideForTests).
    // A REAL start does complete late if START_TIMEOUT_SECONDS is shrunk, but
    // its bridge binds and is closed again by the queued cleanup within
    // microseconds of each other, so no poll can observe the port ever being
    // held; and the abandoned node keeps running in the background for the rest
    // of the real start, where it would collide with the next test's chain on
    // the shared local_chain schema.
    //
    // Nothing covers it now - the LocalChain.startBounded timeout branch (both
    // the "Node start exceeded Ns" message and the late-completion cleanup that
    // closes the bridge and shuts the node down) is unverified.
}
