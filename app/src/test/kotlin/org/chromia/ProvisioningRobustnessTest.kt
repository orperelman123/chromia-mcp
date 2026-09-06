package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.postchain.gtv.GtvFactory.gtv
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.ChromiaRepository
import org.chromia.tools.ClaimTestnetTchrStrategy
import org.chromia.tools.DeployKeyStore
import org.chromia.tools.DeployTestnetChainStrategy
import org.chromia.tools.ProbeBudget
import org.chromia.tools.ProcOut
import org.chromia.tools.ProcessRunner
import org.chromia.tools.ProvisionTestnetContainerStrategy
import org.chromia.tools.RealProcessRunner
import org.chromia.tools.TestnetProvisioning
import org.chromia.tools.TxOutcome
import org.chromia.tools.TxPoster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
 */
class ProvisioningRobustnessTest {

    private val testPriv = "0101010101010101010101010101010101010101010101010101010101010101"

    /** Repository whose every blocking postchain read hangs for [queryHangMs]. */
    private fun hangingRepository(queryHangMs: Long = 1_500, heightHangMs: Long = 0): ChromiaRepository {
        val config = ChromiaConfig(explorerUrl = McpTestSupport.EXPLORER_URL)
        val postchain = PostchainClientService(
            config,
            heightClient = { _, _ ->
                if (heightHangMs > 0) Thread.sleep(heightHangMs)
                40L
            },
            queryClient = { _, _, _ ->
                if (queryHangMs > 0) Thread.sleep(queryHangMs)
                gtv(1L)
            }
        )
        return ChromiaRepositoryImpl(
            config = config,
            httpClientService = HttpClientService(config, McpTestSupport.errorEngine()),
            postchainClientService = postchain
        )
    }

    private val neverPoster = TxPoster { _, _, _, _ ->
        TxOutcome("00".repeat(32), false, "test poster must not be reached")
    }

    private fun call(name: String, args: JsonObject) = callToolRequest(name = name, arguments = args)

    private fun errorText(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): String =
        result.structuredContent!!.jsonObject["error"]?.jsonPrimitive?.contentOrNull
            ?: result.structuredContent!!.jsonObject.toString()

    // ---- 1. bounded chain reads ---------------------------------------------

    @Test
    fun provisionAnswersWithinDeadlineWhenChainReadsHang(@TempDir dir: Path) = runBlocking {
        val strategy = ProvisionTestnetContainerStrategy(
            env = mapOf(
                TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString(),
                ProbeBudget.QUERY_DEADLINE_ENV to "150"
            ),
            txPoster = neverPoster,
            keyStore = DeployKeyStore(dir),
            delayFn = {}
        )
        val started = System.nanoTime()
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject {}),
            hangingRepository(queryHangMs = 1_500)
        )
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertEquals(true, result.isError, "a hanging node pool must yield an honest timeout error, " +
            "not a plausible answer built on reads that never finished: ${result.structuredContent}")
        val text = errorText(result)
        assertTrue(text.contains("timed out"), text)
        assertTrue(
            elapsedMs < 5_000,
            "provision took ${elapsedMs}ms against a hanging node - the deadline was not enforced"
        )
    }

    @Test
    fun claimAnswersWithinDeadlineWhenChainReadsHang(@TempDir dir: Path) = runBlocking {
        val strategy = ClaimTestnetTchrStrategy(
            env = mapOf(
                TestnetProvisioning.FUNDING_KEY_ENV to testPriv,
                TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString(),
                ProbeBudget.QUERY_DEADLINE_ENV to "150"
            ),
            txPoster = neverPoster
        )
        val started = System.nanoTime()
        val result = strategy.execute(
            call("claim_testnet_tchr", buildJsonObject {}),
            hangingRepository(queryHangMs = 1_500)
        )
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertEquals(true, result.isError, "expected a timeout error: ${result.structuredContent}")
        assertTrue(errorText(result).contains("timed out"), errorText(result))
        assertTrue(elapsedMs < 5_000, "claim took ${elapsedMs}ms - the deadline was not enforced")
    }

    @Test
    fun deployPreflightProbeIsBoundedWhenNodesHang(@TempDir dir: Path) = runBlocking {
        val strategy = DeployTestnetChainStrategy(
            env = mapOf(
                TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString(),
                ProbeBudget.PREFLIGHT_DEADLINE_ENV to "150"
            ),
            keyStore = DeployKeyStore(dir),
            processRunner = fakeChr,
            tempDirFactory = { java.nio.file.Files.createTempDirectory(dir, "deploy") }
        )
        val started = System.nanoTime()
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", "module; entity note { text; } query all_notes() = note @* {} (.text);")
                put("container", "or_container_42")
            }),
            // Queries answer instantly (deploy makes none); the HEIGHT probe hangs.
            hangingRepository(queryHangMs = 0, heightHangMs = 1_500)
        )
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        val json = result.structuredContent!!.jsonObject
        assertEquals(
            "refused", json["status"]?.jsonPrimitive?.contentOrNull,
            "a hanging reachability probe must surface as an honest refusal with the timeout named, " +
                "not hang or pass: $json"
        )
        assertTrue(json.toString().contains("timed out"), json.toString())
        assertTrue(
            elapsedMs < 6_000,
            "deploy preflight took ${elapsedMs}ms against hanging nodes - the shared probe deadline " +
                "was not enforced"
        )
    }

    // ---- 2. blockchain name is validated, never silently substituted --------

    @Test
    fun deployRejectsBlockchainNameTheScaffoldWouldSilentlyRewrite(@TempDir dir: Path) = runBlocking {
        // "My-Dapp" normalizes to the scaffold default, "my_dapp & echo pwned"
        // additionally carries cmd metacharacters that a Windows `cmd /c chr`
        // launch would interpret. Both used to pass the dry run as
        // "all gates passed" while a live run could not deploy that name.
        for (name in listOf("My-Dapp", "my_dapp & echo pwned")) {
            val strategy = DeployTestnetChainStrategy(
                env = mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString()),
                keyStore = DeployKeyStore(dir),
                processRunner = fakeChr,
                tempDirFactory = { java.nio.file.Files.createTempDirectory(dir, "deploy") }
            )
            val result = strategy.execute(
                call("deploy_testnet_chain", buildJsonObject {
                    put("rell", "module;")
                    put("container", "or_container_42")
                    put("blockchain", name)
                }),
                hangingRepository(queryHangMs = 0, heightHangMs = 0)
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
            keyStore = DeployKeyStore(dir),
            processRunner = fakeChr,
            tempDirFactory = { java.nio.file.Files.createTempDirectory(dir, "deploy") }
        )
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", "module;")
                put("container", "c1\nchains:\n  evil: x")
            }),
            hangingRepository(queryHangMs = 0, heightHangMs = 0)
        )
        assertEquals(true, result.isError, "${result.structuredContent}")
        // The refusal must name the real problem (an invalid lease name), not
        // misattribute it - the old path fell into a misleading
        // "conflicts with deployments.testnet.container" error after already
        // splicing the raw value into the YAML.
        assertTrue(errorText(result).contains("valid container"), errorText(result))
    }

    /** chr fake: --version answers like a real 0.33.2; nothing else is ever run. */
    private val fakeChr = ProcessRunner { command, _, _, _ ->
        if (command.contains("--version")) {
            ProcOut(0, "chr version 0.33.2\nrell version 0.16.1\npostchain version 3.47.6\n", "")
        } else {
            ProcOut(1, "", "regression test: deploy must not execute chr here")
        }
    }
}
