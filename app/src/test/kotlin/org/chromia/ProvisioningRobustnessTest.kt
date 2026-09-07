package org.chromia

import org.chromia.tools.callToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.tools.ClaimTestnetTchrStrategy
import org.chromia.tools.DappScaffold
import org.chromia.tools.DeployKeyStore
import org.chromia.tools.DeployTestnetChainStrategy
import org.chromia.tools.ProbeBudget
import org.chromia.tools.ProvisionTestnetContainerStrategy
import org.chromia.tools.TestnetProvisioning
import org.chromia.tools.TestnetProvisioning.toHex
import org.chromia.tools.WriteDeploymentConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Path

/**
 * QA bug-hunt regressions for the freshly merged provisioning tools
 * (2026-09-02), covering two classes cc76b9e/192cd75 fixed elsewhere but the
 * merge reintroduced:
 *
 *  1. UNBOUNDED BLOCKING CHAIN READS: every other tool routes its blocking
 *     postchain-client reads through [ProbeBudget]; the provisioning tools
 *     called the repository raw, so a node pool that stalls (the exact
 *     TryNextOnError crawl documented on ProbeBudget) made
 *     provision_testnet_container / claim_testnet_tchr / deploy_testnet_chain
 *     hang past the hosted proxy's 60s write timeout - the caller got a closed
 *     socket instead of an honest, actionable timeout.
 *
 *  2. A PLAUSIBLE-NOT-TRUTHFUL dry run: deploy_testnet_chain accepted any
 *     `blockchain` string, but the generated chromia.yml silently normalizes
 *     invalid names to the scaffold default while the chr command line keeps
 *     the raw value - so a dry run reported "all gates passed" for a deploy
 *     whose live run could not succeed (and on Windows the raw value reached
 *     `cmd /c`, where shell metacharacters are live).
 *
 * Plus the RealProcessRunner timeout tests in [RealProcessRunnerTest].
 *
 * ZERO DOUBLES (2026-09-07). This file used to build its stalling node out of
 * `queryClient = { Thread.sleep(...) }` / `heightClient = { Thread.sleep(...) }`
 * seams on [PostchainClientService], and its chr out of a `ProcessRunner { }`
 * lambda. Both are gone:
 *
 *  - the stall is now a REAL TCP endpoint ([SilentTcpEndpoint]) that the
 *    production client connects to over a real socket and that never answers.
 *    That is the same category as [LiveChromia.unreachableConfig]'s closed
 *    port - a real address whose behaviour comes from the operating system,
 *    not from a substitute implementation of one of our types - except that it
 *    stalls instead of refusing, which is the failure mode ProbeBudget exists
 *    for and the one a closed port cannot reproduce.
 *  - the chr fake is gone entirely: the two validation tests below refuse
 *    their input before any process is launched, so they run the production
 *    [DeployTestnetChainStrategy] with its real [org.chromia.tools.RealProcessRunner].
 */
class ProvisioningRobustnessTest {

    /**
     * A REAL listening TCP socket on loopback that accepts connections, drains
     * whatever is written to it, and never writes a byte back.
     *
     * This is not a double. Nothing here implements one of our interfaces or
     * stands in for our code: it is an address, and every layer between the
     * tool and it - ChromiaRepositoryImpl, PostchainClientService, the
     * postchain client, Apache HC5, the JVM's socket stack - is the production
     * one doing exactly what it does in production. The stall is a property of
     * the endpoint, the way an overloaded node's stall is.
     *
     * The input is drained on its own daemon thread so the client's request
     * write always completes; without that, a full receive buffer could make
     * the write fail fast and turn the stall into an ordinary I/O error.
     */
    private class SilentTcpEndpoint : AutoCloseable {
        private val server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        private val held = java.util.Collections.synchronizedList(mutableListOf<Socket>())

        init {
            val acceptor = Thread({
                while (!server.isClosed) {
                    val socket = runCatching { server.accept() }.getOrNull() ?: break
                    held.add(socket)
                    val drainer = Thread({
                        runCatching {
                            val stream = socket.getInputStream()
                            val buffer = ByteArray(4096)
                            while (stream.read(buffer) >= 0) { /* read and discard; never answer */ }
                        }
                    }, "silent-tcp-endpoint-drain")
                    drainer.isDaemon = true
                    drainer.start()
                }
            }, "silent-tcp-endpoint-accept")
            acceptor.isDaemon = true
            acceptor.start()
        }

        val url: String get() = "http://127.0.0.1:${server.localPort}"

        override fun close() {
            runCatching { server.close() }
            synchronized(held) { held.forEach { runCatching { it.close() } } }
        }
    }

    /**
     * The production repository, wired the way `App` wires it, pointed at a
     * node URL that stalls. No seam: [PostchainClientService] is constructed
     * with nothing but a config.
     */
    private fun stalledRepository(endpoint: SilentTcpEndpoint): ChromiaRepositoryImpl {
        val config = ChromiaConfig(
            explorerUrl = "${endpoint.url}/explorer-service",
            predefinedNetworks = mapOf(
                "mainnet" to listOf(endpoint.url),
                "testnet" to listOf(endpoint.url)
            )
        )
        return ChromiaRepositoryImpl(
            config = config,
            httpClientService = HttpClientService(config),
            postchainClientService = PostchainClientService(config)
        )
    }

    /** A chromia.yml for `my_dapp` whose testnet deployment URLs all stall. */
    private fun ymlPointingAt(endpoint: SilentTcpEndpoint): String {
        val spec = WriteDeploymentConfig.resolveNetwork("testnet")!!
        val yml = WriteDeploymentConfig.chromiaYml(spec, "my_dapp", DappScaffold.chromiaYmlFor("my_dapp"))
            .replace("<containerIID>", "or_container_42")
        // Every official node URL becomes the one endpoint that never answers.
        return WriteDeploymentConfig.TESTNET_URLS.fold(yml) { acc, url -> acc.replace(url, endpoint.url) }
    }

    private fun call(name: String, args: JsonObject) = callToolRequest(name = name, arguments = args)

    private fun errorText(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): String =
        result.structuredContent!!.jsonObject["error"]?.jsonPrimitive?.contentOrNull
            ?: result.structuredContent!!.jsonObject.toString()

    // ---- 1. bounded chain reads ---------------------------------------------

    @Test
    fun provisionAnswersWithinDeadlineWhenChainReadsHang(@TempDir dir: Path) = runBlocking {
        SilentTcpEndpoint().use { endpoint ->
            val strategy = ProvisionTestnetContainerStrategy(
                env = mapOf(
                    TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString(),
                    ProbeBudget.QUERY_DEADLINE_ENV to "150"
                ),
                keyStore = DeployKeyStore(dir)
            )
            val started = System.nanoTime()
            val result = strategy.execute(
                call("provision_testnet_container", buildJsonObject {}),
                stalledRepository(endpoint)
            )
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertEquals(true, result.isError, "a hanging node pool must yield an honest timeout error, " +
                "not a plausible answer built on reads that never finished: ${result.structuredContent}")
            val text = errorText(result)
            assertTrue(text.contains("timed out"), text)
            assertTrue(
                elapsedMs < 5_000,
                "provision took ${elapsedMs}ms against a stalling node - the deadline was not enforced"
            )
        }
    }

    @Test
    fun claimAnswersWithinDeadlineWhenChainReadsHang(@TempDir dir: Path) = runBlocking {
        SilentTcpEndpoint().use { endpoint ->
            // A throwaway keypair generated here: unregistered and unfunded, so
            // the call gets past key resolution and into the chain read that has
            // to be bounded, with nothing that could ever be spent.
            val throwaway = TestnetProvisioning.cryptoSystem.generateKeyPair()
            val strategy = ClaimTestnetTchrStrategy(
                env = mapOf(
                    TestnetProvisioning.FUNDING_KEY_ENV to throwaway.privKey.data.toHex(),
                    TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString(),
                    ProbeBudget.QUERY_DEADLINE_ENV to "150"
                )
            )
            val started = System.nanoTime()
            val result = strategy.execute(
                call("claim_testnet_tchr", buildJsonObject {}),
                stalledRepository(endpoint)
            )
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            assertEquals(true, result.isError, "expected a timeout error: ${result.structuredContent}")
            assertTrue(errorText(result).contains("timed out"), errorText(result))
            assertTrue(elapsedMs < 5_000, "claim took ${elapsedMs}ms - the deadline was not enforced")
        }
    }

    @Test
    fun deployPreflightProbeIsBoundedWhenNodesHang(@TempDir dir: Path) = runBlocking {
        SilentTcpEndpoint().use { endpoint ->
            val strategy = DeployTestnetChainStrategy(
                env = mapOf(
                    TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString(),
                    ProbeBudget.PREFLIGHT_DEADLINE_ENV to "150"
                ),
                keyStore = DeployKeyStore(dir)
            )
            val started = System.nanoTime()
            val result = strategy.execute(
                call("deploy_testnet_chain", buildJsonObject {
                    put("rell", "module; entity note { text; } query all_notes() = note @* {} (.text);")
                    // The yml names the stalling endpoint as the deployment URL,
                    // so it is the REACHABILITY probe that hangs.
                    put("chromiaYml", ymlPointingAt(endpoint))
                    put("container", "or_container_42")
                }),
                stalledRepository(endpoint)
            )
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            val json = result.structuredContent!!.jsonObject
            assertEquals(
                "refused", json["status"]?.jsonPrimitive?.contentOrNull,
                "a hanging reachability probe must surface as an honest refusal with the timeout named, " +
                    "not hang or pass: $json"
            )
            assertTrue(json.toString().contains("timed out"), json.toString())
            // Unbounded, the postchain client waits out its own per-endpoint
            // response timeout (60s) on every probed URL; bounded, the shared
            // 150ms budget is spent on the first. The slack below is the real
            // Rell compile + security gate that run BEFORE the probe, which are
            // in-process work and not what this test bounds.
            assertTrue(
                elapsedMs < 30_000,
                "deploy preflight took ${elapsedMs}ms against a stalling node - the shared probe " +
                    "deadline was not enforced"
            )
        }
    }

    // ---- 2. blockchain name is validated, never silently substituted --------

    @Test
    fun deployRejectsBlockchainNameTheScaffoldWouldSilentlyRewrite(@TempDir dir: Path) = runBlocking {
        // "My-Dapp" normalizes to the scaffold default, "my_dapp & echo pwned"
        // additionally carries cmd metacharacters that a Windows `cmd /c chr`
        // launch would interpret. Both used to pass the dry run as
        // "all gates passed" while a live run could not deploy that name.
        //
        // The refusal happens before any chromia.yml is generated, any chr is
        // launched and any chain is read, so this drives the production
        // strategy with its real process runner and the production repository:
        // neither is reached, which is itself part of the claim.
        for (name in listOf("My-Dapp", "my_dapp & echo pwned")) {
            val strategy = DeployTestnetChainStrategy(
                env = mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString()),
                keyStore = DeployKeyStore(dir)
            )
            val result = strategy.execute(
                call("deploy_testnet_chain", buildJsonObject {
                    put("rell", "module;")
                    put("container", "or_container_42")
                    put("blockchain", name)
                }),
                LiveChromia.repository()
            )
            assertEquals(
                true, result.isError,
                "blockchain \"$name\" must be a validation error - the generated chromia.yml would " +
                    "define a DIFFERENT name than the chr command deploys: ${result.structuredContent}"
            )
            assertTrue(errorText(result).contains("valid chain name"), errorText(result))
        }
    }

    @Test
    fun deployRejectsContainerNameOutsideTheLeaseCharset(@TempDir dir: Path) = runBlocking {
        val strategy = DeployTestnetChainStrategy(
            env = mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString()),
            keyStore = DeployKeyStore(dir)
        )
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", "module;")
                put("container", "c1\nchains:\n  evil: x")
            }),
            LiveChromia.repository()
        )
        assertEquals(true, result.isError, "${result.structuredContent}")
        // The refusal must name the real problem (an invalid lease name), not
        // misattribute it - the old path fell into a misleading
        // "conflicts with deployments.testnet.container" error after already
        // splicing the raw value into the YAML.
        assertTrue(errorText(result).contains("valid container"), errorText(result))
    }
}
