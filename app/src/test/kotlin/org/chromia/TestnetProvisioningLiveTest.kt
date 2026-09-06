package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
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
import org.chromia.tools.EconomyChainGateway
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

    /**
     * A funding account known to be registered on the testnet Economy Chain.
     * Public chain data - the same kind of thing as a wallet address - and it is
     * used ONLY as the subject of a read-only get_balance query. No key material
     * for it exists anywhere in this repository.
     */
    private val KNOWN_REGISTERED_ACCOUNT_ID =
        "7CBC0F013C10CC8BBBA85CD947A8A8E18140FB9D3C2546114C5B8F0EFB5C30A7"

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
            callToolRequest(name = "provision_testnet_container", arguments = buildJsonObject {}),
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
            callToolRequest(name = "claim_testnet_tchr", arguments = buildJsonObject {}),
            liveRepository()
        )
        assertEquals(false, result.isError)
        val json = result.structuredContent!!.jsonObject
        // Unregistered throwaway account: the tool must name the bootstrap, not pretend.
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content)
        assertNotNull(json["humanStep"])
    }

    /**
     * THE POSITIVE REGISTRATION PATH, WITHOUT ANY KEY.
     *
     * This replaces liveDefaultChrKeystorePathResolvesARegisteredAccount, which
     * asserted the same property through the machine's own chr keystore and so
     * SKIPPED on every runner - the one allowlisted skip this repo carried. Its
     * three assertions are all covered without a private key now:
     *   - key RESOLUTION from a keystore: FundingKeyResolutionTest, which builds
     *     its own keystores in a temp dir (no network, no secret);
     *   - the dry run never leaking private key material:
     *     liveDryRunPricesLeaseAndResolvesEverythingWithoutSpending, which does
     *     it with a throwaway keypair;
     *   - and the positive registration lookup, which is this test.
     *
     * `get_balance` answers only for an account that EXISTS on the Economy
     * Chain - for an unknown id the node returns `MISSING ACCOUNT: Account not
     * found` (verified live 2026-09-03), which surfaces here as a thrown
     * ChainQueryException rather than a null - so a balance for a known-registered
     * account id proves the account exists and the Economy Chain lookup path
     * works. Note the boundary: the provisioning tools decide `registered` via
     * `ft4.get_accounts_by_signer` (EconomyChainGateway.resolveFunding), which
     * needs the key; this test does not exercise that call, only the chain it
     * talks to. The account id is public chain data - an address, not a
     * credential - and nothing here signs, spends or needs a private key. The
     * three sibling live tests already cover the negative branch with a
     * throwaway keypair.
     */
    @Test
    fun liveKnownAccountIsRegisteredOnTheEconomyChain() = runBlocking {
        assumeTrue(liveEnabled(), "live provisioning tests disabled (set CHROMIA_LIVE_PROVISIONING_TESTS=true)")
        val gateway = EconomyChainGateway(liveRepository())
        val balance = gateway.balanceOf(KNOWN_REGISTERED_ACCOUNT_ID)
        assertNotNull(
            balance,
            "get_balance answered nothing for $KNOWN_REGISTERED_ACCOUNT_ID - either the Economy Chain " +
                "lookup path broke, or this account is no longer registered. It is the only positive " +
                "registration assertion CI has; do not delete it, re-point it."
        )
        assertTrue(balance!! >= 0, "a registered account's balance cannot be negative, got $balance")
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
        // Every message below carries the raw wire outcome. CI run 34005621969
        // (2026-09-05) failed the rejectReason assertion with nothing but
        // "expected: not <null>" - the node's actual answer was lost, so the
        // failure could not be diagnosed, only rerun. Never again.
        val wire = "finalStatus=${outcome.finalStatus} rejectReason=${outcome.rejectReason} " +
            "lastStatusPoll=${outcome.lastStatusPollResponse}"
        // Logged on success too, so every run's JUnit XML records what the chain
        // actually answered - the reference for pinning the expected message.
        println("live signed tx wire outcome: $wire")
        assertFalse(outcome.confirmed, "a nonexistent account must not authenticate ($wire)")
        assertNotNull(
            outcome.rejectReason,
            "rejection must carry the chain's reason, and it did not. The client ended with status " +
                "${outcome.finalStatus}: REJECTED means the node itself omitted the reason, WAITING means " +
                "the tx was still queued when the status poll ran out of retries (client default 20 x 500ms), " +
                "UNKNOWN means every status poll failed (HTTP error or connection). Last status exchange " +
                "on the wire: ${outcome.lastStatusPollResponse}"
        )
        // The reason should point at the missing account/auth descriptor, i.e.
        // the tx passed signature verification and reached the Rell operation.
        assertTrue(
            listOf("account", "auth", "not found", "does not exist").any {
                outcome.rejectReason!!.lowercase().contains(it)
            },
            "unexpected rejection: ${outcome.rejectReason} ($wire)"
        )
    }
}
