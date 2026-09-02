package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.tools.ClaimTestnetTchrStrategy
import org.chromia.tools.DeployKeyStore
import org.chromia.tools.ProvisionTestnetContainerStrategy
import org.chromia.tools.RealTxPoster
import org.chromia.tools.TestnetProvisioning
import org.chromia.tools.TestnetProvisioning.toHex
import org.chromia.tools.TxOp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * LIVE proof for the testnet provisioning tools, gated on
 * `CHROMIA_LIVE_PROVISIONING_TESTS=true` (assumption-skip otherwise - never a
 * hard skip). Spends nothing and uses NO real key material: every signature in
 * here comes from a throwaway keypair generated inside the test, whose FT4
 * account does not exist - so the one posted transaction is guaranteed to be
 * rejected by ft4 auth while still proving GTX construction, secp256k1
 * signing, and node acceptance of the wire format end-to-end.
 */
class TestnetProvisioningLiveTest {

    private fun liveEnabled() =
        System.getenv("CHROMIA_LIVE_PROVISIONING_TESTS")?.equals("true", ignoreCase = true) == true

    private fun liveRepository(): ChromiaRepositoryImpl {
        val config = ChromiaConfig()
        return ChromiaRepositoryImpl(
            config = config,
            httpClientService = HttpClientService(config),
            postchainClientService = PostchainClientService(config)
        )
    }

    @Test
    fun liveDryRunPricesLeaseAndResolvesEverythingWithoutSpending(@TempDir dir: Path) = runBlocking {
        assumeTrue(liveEnabled(), "live provisioning tests disabled (set CHROMIA_LIVE_PROVISIONING_TESTS=true)")

        // Throwaway funding key: unregistered on the Economy Chain, so this also
        // exercises get_accounts_by_signer + get_pending_transfer_strategies live.
        val throwaway = TestnetProvisioning.cryptoSystem.generateKeyPair()
        val privHex = throwaway.privKey.data.toHex()
        val strategy = ProvisionTestnetContainerStrategy(
            env = mapOf(
                TestnetProvisioning.FUNDING_KEY_ENV to privHex,
                TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString()
            ),
            keyStore = DeployKeyStore(Files.createDirectory(dir.resolve("keys")))
        )
        val result = strategy.execute(
            CallToolRequest(name = "provision_testnet_container", arguments = buildJsonObject {}),
            liveRepository()
        )
        assertEquals(false, result.isError) { "live dry run failed: ${result.structuredContent}" }
        val json = result.structuredContent!!.jsonObject
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content)
        // Live price: ~35 tCHR/SCU-week on blue -> 70 tCHR for the 1 SCU x 2 week default.
        val costRaw = json["costRaw"]!!.jsonPrimitive.content.toLong()
        assertTrue(costRaw > 0, "live cost must be positive, got $costRaw")
        val funding = json["funding"]!!.jsonObject
        assertEquals(true, funding["configured"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(false, funding["registered"]!!.jsonPrimitive.content.toBoolean())
        // The bootstrap step names the derived account id, never the key.
        val text = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(), json
        )
        assertFalse(text.contains(privHex, ignoreCase = true), "private key leaked into live dry run output")
    }

    @Test
    fun liveClaimDryRunReportsFaucetTerms(@TempDir dir: Path) = runBlocking {
        assumeTrue(liveEnabled(), "live provisioning tests disabled (set CHROMIA_LIVE_PROVISIONING_TESTS=true)")
        val throwaway = TestnetProvisioning.cryptoSystem.generateKeyPair()
        val strategy = ClaimTestnetTchrStrategy(
            env = mapOf(
                TestnetProvisioning.FUNDING_KEY_ENV to throwaway.privKey.data.toHex(),
                TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString()
            )
        )
        val result = strategy.execute(
            CallToolRequest(name = "claim_testnet_tchr", arguments = buildJsonObject {}),
            liveRepository()
        )
        assertEquals(false, result.isError)
        val json = result.structuredContent!!.jsonObject
        // Unregistered throwaway account: the tool must name the bootstrap, not pretend.
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content)
        assertNotNull(json["humanStep"])
    }

    /**
     * The zero-human default path: with no explicit env config, the funding key
     * resolves from the machine's own chr keystore (~/.chromia config key.id)
     * and its FT4 account is found registered on the live Economy Chain. Dry
     * run only - nothing is signed; tolerant of balance changes. Additionally
     * gated on a chr keystore actually existing on this machine.
     */
    @Test
    fun liveDefaultChrKeystorePathResolvesARegisteredAccount(@TempDir dir: Path) = runBlocking {
        assumeTrue(liveEnabled(), "live provisioning tests disabled (set CHROMIA_LIVE_PROVISIONING_TESTS=true)")
        val chromiaDir = Path.of(System.getProperty("user.home"), ".chromia")
        assumeTrue(Files.exists(chromiaDir.resolve("config")), "no local chr keystore with a config")
        val key = TestnetProvisioning.resolveFundingKey(emptyMap())
        assumeTrue(key != null, "chr keystore config names no loadable key")

        val strategy = ProvisionTestnetContainerStrategy(
            env = emptyMap<String, String>(),
            keyStore = DeployKeyStore(Files.createDirectory(dir.resolve("keys")))
        )
        val result = strategy.execute(
            CallToolRequest(name = "provision_testnet_container", arguments = buildJsonObject { put("dryRun", true) }),
            liveRepository()
        )
        assertEquals(false, result.isError) { "live keystore dry run failed: ${result.structuredContent}" }
        val json = result.structuredContent!!.jsonObject
        val funding = json["funding"]!!.jsonObject
        assertEquals(true, funding["configured"]!!.jsonPrimitive.content.toBoolean())
        assertTrue(
            funding["keySource"]!!.jsonPrimitive.content.startsWith("chr-keystore:"),
            "expected the chr keystore source, got ${funding["keySource"]}"
        )
        assertEquals(true, funding["registered"]!!.jsonPrimitive.content.toBoolean())
        val text = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(), json
        )
        assertFalse(
            text.contains(key!!.privKey.toHex(), ignoreCase = true),
            "private key leaked into keystore dry run output"
        )
    }

    @Test
    fun liveSignedTransactionIsAcceptedOnWireAndRejectedByFt4Auth() {
        assumeTrue(liveEnabled(), "live provisioning tests disabled (set CHROMIA_LIVE_PROVISIONING_TESTS=true)")

        // Throwaway key, nonexistent account: ft4.ft_auth must reject it, which
        // proves the whole signing pipeline (GTX build, merkle digest, secp256k1
        // signature, REST post, status poll) against the LIVE chain without any
        // possibility of spending.
        val throwaway = TestnetProvisioning.cryptoSystem.generateKeyPair()
        val pub = throwaway.pubKey.data
        val accountId = TestnetProvisioning.accountIdForSingleSigner(pub)
        val outcome = RealTxPoster.post(
            urls = ChromiaConfig().predefinedNetworks.getValue("testnet"),
            bridHex = "090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874",
            ops = listOf(
                TestnetProvisioning.ftAuthOp(accountId, accountId /* no descriptor exists */),
                TestnetProvisioning.faucetOp()
            ),
            privKey = throwaway.privKey.data
        )
        assertFalse(outcome.confirmed, "a nonexistent account must not authenticate")
        assertNotNull(outcome.rejectReason, "rejection must carry the chain's reason")
        // The reason should point at the missing account/auth descriptor, i.e.
        // the tx passed signature verification and reached the Rell operation.
        assertTrue(
            listOf("account", "auth", "not found", "does not exist").any {
                outcome.rejectReason!!.lowercase().contains(it)
            },
            "unexpected rejection: ${outcome.rejectReason}"
        )
    }
}
