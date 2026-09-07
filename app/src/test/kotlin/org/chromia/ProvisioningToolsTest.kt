package org.chromia

import org.chromia.tools.callToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.ChrLocator
import org.chromia.tools.ChrVersions
import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.ClaimTestnetTchrStrategy
import org.chromia.tools.DappScaffold
import org.chromia.tools.DeployKeyStore
import org.chromia.tools.DeployTestnetChainStrategy
import org.chromia.tools.ProvisionTestnetContainerStrategy
import org.chromia.tools.RealProcessRunner
import org.chromia.tools.TestnetProvisioning
import org.chromia.tools.TestnetProvisioning.hexToBytes
import org.chromia.tools.TestnetProvisioning.toHex
import org.chromia.tools.WriteDeploymentConfig
import org.chromia.tools.chainsEntryRid
import org.chromia.tools.declaredChainNames
import org.chromia.tools.declaredLibNames
import org.chromia.tools.outdatedChrNote
import org.chromia.tools.withChainsEntry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * provision_testnet_container / claim_testnet_tchr / deploy_testnet_chain:
 * agent-headless testnet provisioning.
 *
 * ZERO DOUBLES (2026-09-07). This file used to answer every chain read from a
 * mutable `FakeChain` behind `queryClient = { ... }` / `heightClient = { ... }`
 * seams on PostchainClientService, post every transaction into a `FakePoster`,
 * and run every `chr` through a `FakeRunner`. All three are gone, and so are
 * the seams. What is left is exactly three kinds of test:
 *
 *  - PURE, called directly with literal inputs: fee formatting, key parsing,
 *    account-id derivation, redaction, chromia.yml splicing and reading,
 *    version-string parsing, chr-failure classification, lib-warning
 *    stripping, and chr executable resolution against REAL files in a temp
 *    directory.
 *  - LIVE, through [LiveChromia.repository] - the production repository and
 *    the production config against the real testnet Economy and Directory
 *    Chains, gated once by [LiveChromia.requireLive]. Where a signing key is
 *    needed the test generates a throwaway keypair inside the test: it is
 *    unregistered and unfunded, so nothing can be spent, exactly as
 *    [TestnetProvisioningLiveTest] established.
 *  - REAL chr, resolved and launched by the production [ChrLocator] +
 *    [RealProcessRunner]. Where a test needs chr to be ABSENT it points
 *    CHROMIA_CHR_BIN at a path with no file at it, so the launch failure comes
 *    from the operating system rather than from a substitute runner.
 *
 * Everything that could only be reached by manufacturing a chain or node
 * outcome - a lease paid from a funded account, a rejected transfer, a faucet
 * cooldown, a raced deploy - was DELETED, with a comment where it stood saying
 * what it claimed and what is now unverified. Those all need a funded,
 * registered testnet key, which must not exist in CI.
 *
 * The account-id pair below is public chain data (pubkey 021BBC..., account
 * 9AA3F08C...) read off a real registered account on the live Economy Chain:
 * an address, not a credential.
 */
class ProvisioningToolsTest {

    // A real (pubkey -> FT4 account id) pair observed on the live testnet
    // Economy Chain - proves accountIdForSingleSigner uses the exact merkle
    // hash FT4 uses.
    private val livePub = "021BBC7C1F2247719DC15376823C6AF13C1250E3836B6CAE5124DFA34E394BA44C"
    private val liveAccount = "9AA3F08C45D240EB89B5470355A0F1C4E5399C4DD534BEBF9777688AF1EE84B3"

    // A fixed, well-known, never-funded private key. It is DATA for the pure
    // key-resolution and keystore tests, and for dry runs that must RESOLVE a
    // deploy key without ever signing with it; no test below posts anything
    // with it.
    private val testPriv = "0101010101010101010101010101010101010101010101010101010101010101"
    private val testPub = TestnetProvisioning.derivePubKey(testPriv.hexToBytes()).toHex()

    // ---- pure helpers -------------------------------------------------------

    @Test
    fun accountIdDerivationMatchesLiveChainPair() {
        assertEquals(
            liveAccount,
            TestnetProvisioning.accountIdForSingleSigner(livePub.hexToBytes()).toHex()
        )
    }

    @Test
    fun formatTchrFormatsSixDecimals() {
        assertEquals("70", TestnetProvisioning.formatTchr(70_000_000))
        assertEquals("10.5", TestnetProvisioning.formatTchr(10_500_000))
        assertEquals("0.000001", TestnetProvisioning.formatTchr(1))
        assertEquals("0", TestnetProvisioning.formatTchr(0))
        assertEquals("1000", TestnetProvisioning.formatTchr(TestnetProvisioning.FAUCET_AMOUNT_RAW))
    }

    @Test
    fun parsePrivKeyAcceptsOnly64Hex() {
        assertNotNull(TestnetProvisioning.parsePrivKey(testPriv))
        assertNotNull(TestnetProvisioning.parsePrivKey("0x$testPriv"))
        assertNull(TestnetProvisioning.parsePrivKey(null))
        assertNull(TestnetProvisioning.parsePrivKey(""))
        assertNull(TestnetProvisioning.parsePrivKey("zz".repeat(32)))
        assertNull(TestnetProvisioning.parsePrivKey(testPriv.dropLast(2)))
    }

    @Test
    fun sanitizeTextRedactsSecretsCaseInsensitively() {
        val out = TestnetProvisioning.sanitizeText(
            "reject: key ${testPriv.uppercase()} invalid / also ${testPriv.lowercase()}",
            setOf(testPriv)
        )
        assertFalse(out.contains(testPriv, ignoreCase = true))
        assertTrue(out.contains(TestnetProvisioning.REDACTED))
    }

    @Test
    fun singleSigAuthDescriptorGtvHasCompactShape() {
        val g = TestnetProvisioning.singleSigAuthDescriptorGtv(livePub.hexToBytes())
        val arr = g.asArray()
        assertEquals(3, arr.size)
        assertEquals(0L, arr[0].asInteger()) // auth_type.S compact index
        val args = arr[1].asArray()
        assertEquals(listOf("A", "T"), args[0].asArray().map { it.asString() })
        assertEquals(livePub, args[1].asByteArray().toHex())
        assertTrue(arr[2].isNull())
    }

    @Test
    fun ticketStateParsingHandlesCompactAndPrettyForms() {
        assertEquals(1, TestnetProvisioning.parseTicketState("SUCCESS"))
        assertEquals(0, TestnetProvisioning.parseTicketState("PENDING"))
        assertEquals(2, TestnetProvisioning.parseTicketState("FAILURE"))
        assertEquals(1, TestnetProvisioning.parseTicketState("1"))
        assertEquals(2, TestnetProvisioning.parseTicketState(2L))
        assertNull(TestnetProvisioning.parseTicketState(null))
    }

    // ---- funding key resolution --------------------------------------------
    // Real files in a real temp directory, read by the production resolver.

    @Test
    fun resolveFundingKeyPrefersEnvRawKey(@TempDir dir: Path) {
        val key = TestnetProvisioning.resolveFundingKey(
            mapOf(
                TestnetProvisioning.FUNDING_KEY_ENV to testPriv,
                TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString()
            )
        )
        assertNotNull(key)
        assertEquals("env:${TestnetProvisioning.FUNDING_KEY_ENV}", key!!.sourceLabel)
        assertEquals(testPriv.uppercase(), key.privKey.toHex())
    }

    @Test
    fun resolveFundingKeyReadsChrKeystoreBareFileViaExplicitId(@TempDir dir: Path) {
        Files.writeString(dir.resolve("my_key"), testPriv)
        val key = TestnetProvisioning.resolveFundingKey(
            mapOf(
                TestnetProvisioning.FUNDING_KEY_ID_ENV to "my_key",
                TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString()
            )
        )
        assertNotNull(key)
        assertEquals("chr-keystore:my_key", key!!.sourceLabel)
    }

    @Test
    fun resolveFundingKeyFollowsConfigKeyIdAndSecretFiles(@TempDir dir: Path) {
        Files.writeString(dir.resolve("config"), "key.id = deploy_key\n")
        Files.writeString(dir.resolve("deploy_key.secret"), "pubkey=$testPub\nprivkey=$testPriv\n")
        val key = TestnetProvisioning.resolveFundingKey(
            mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString())
        )
        assertNotNull(key)
        assertEquals("chr-keystore:deploy_key", key!!.sourceLabel)
        assertEquals(testPriv.uppercase(), key.privKey.toHex())
    }

    @Test
    fun resolveFundingKeyRejectsPathTraversalIdsAndEmptyDir(@TempDir dir: Path) {
        Files.writeString(dir.resolve("config"), "key.id = ../evil\n")
        assertNull(
            TestnetProvisioning.resolveFundingKey(
                mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString())
            )
        )
        val empty = Files.createDirectory(dir.resolve("empty"))
        assertNull(
            TestnetProvisioning.resolveFundingKey(
                mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to empty.toString())
            )
        )
    }

    @Test
    fun deployKeyStoreRoundtripsWithoutLeakingKeys(@TempDir dir: Path) {
        val store = DeployKeyStore(dir)
        store.storeEphemeral(testPub, testPriv)
        store.recordTx("AA".repeat(32), testPub)
        store.recordContainer("cont1", testPub)
        assertEquals(testPub, store.pubKeyForTx("aa".repeat(32).uppercase()))
        assertEquals(testPub, store.pubKeyForContainer("cont1"))
        assertEquals(testPriv.uppercase(), store.privKeyFor(testPub)!!.toHex())
        assertNull(store.privKeyFor("02" + "00".repeat(32)))

        // The rejected-lease cleanup half, on the real store (see the DELETED
        // note on rejectedLeaseDiscardsTheEphemeralKeyItMinted below): minting
        // an ephemeral key writes exactly one artifact, discarding it removes
        // that artifact and nothing else, and a second discard is honest about
        // having removed nothing.
        val mintDir = Files.createDirectory(dir.resolve("mint"))
        val minted = DeployKeyStore(mintDir)
        minted.storeEphemeral(testPub, testPriv)
        assertNotNull(minted.privKeyFor(testPub))
        assertTrue(minted.discardEphemeral(testPub))
        assertNull(minted.privKeyFor(testPub))
        assertFalse(minted.discardEphemeral(testPub))
        val leftovers = Files.list(mintDir).use { it.map { p -> p.fileName.toString() }.toList() }
        assertTrue(leftovers.isEmpty(), "discarding must leave no keystore artifact, found: $leftovers")
    }

    // ---- shared helpers -----------------------------------------------------

    private fun envWith(vararg extra: Pair<String, String>, dir: Path): Map<String, String> =
        mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString(), *extra)

    private fun call(name: String, args: JsonObject) = callToolRequest(name = name, arguments = args)

    private fun resultJson(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): JsonObject =
        result.structuredContent!!.jsonObject

    private fun resultText(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): String =
        Json.encodeToString(JsonObject.serializer(), result.structuredContent!!.jsonObject)

    /** A throwaway keypair: unregistered on the Economy Chain, unfunded, unspendable. */
    private fun throwawayPrivHex(): String =
        TestnetProvisioning.cryptoSystem.generateKeyPair().privKey.data.toHex()

    private fun accountIdFor(privHex: String): String =
        TestnetProvisioning.accountIdForSingleSigner(
            TestnetProvisioning.derivePubKey(privHex.hexToBytes())
        ).toHex()

    /**
     * The versions the chr ACTUALLY installed on this machine reports, resolved
     * and launched exactly the way the production strategy does, or null when
     * this machine has no working chr. Tests that assert on the adaptive Rell
     * pin take their expectation from this rather than from a fixture, so they
     * assert on every machine instead of skipping on some.
     */
    private fun installedChr(): ChrVersions? {
        val resolved = ChrLocator.resolve(System.getenv())
        val out = runCatching {
            RealProcessRunner.run(resolved.command + "--version", Path.of("."), emptyMap(), 60_000)
        }.getOrNull()
        return out?.takeIf { it.exitCode == 0 }
            ?.let { ChrVersions.parse(it.stdout + "\n" + it.stderr) }
    }

    /**
     * A CHROMIA_CHR_BIN value naming a real path where no file exists.
     * Launching it fails in ProcessBuilder for real, which is how the
     * "chr is not available on this server" branch is reached without a double.
     */
    private fun missingChrBin(dir: Path): String = dir.resolve("no-such-chr").toString()

    private fun provisionStrategy(env: Map<String, String>, keystoreDir: Path) =
        ProvisionTestnetContainerStrategy(env = env, keyStore = DeployKeyStore(keystoreDir))

    private fun deployStrategy(env: Map<String, String>, keystoreDir: Path) =
        DeployTestnetChainStrategy(env = env, keyStore = DeployKeyStore(keystoreDir))

    // ---- provision ----------------------------------------------------------

    @Test
    fun provisionDryRunPricesTheLeaseAndResolvesTheFundingKeyLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive(
            "prices a container lease on the live Economy Chain and reports where the funding key came from"
        )
        // There is no poster to pass any more - RealTxPoster is the only one
        // there is - so the answer being "dry_run" is the proof that nothing
        // was signed.
        val privHex = throwawayPrivHex()
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to privHex, dir = dir),
            Files.createDirectory(dir.resolve("keys"))
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject {}),
            LiveChromia.repository()
        )
        assertTrue(result.isError != true, resultText(result))
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content)
        assertEquals(TestnetProvisioning.DEFAULT_CLUSTER, json["cluster"]!!.jsonPrimitive.content)
        val costRaw = json["costRaw"]!!.jsonPrimitive.content.toLong()
        assertTrue(costRaw > 0, "the live create_container_cost must be positive, got $costRaw")
        assertEquals(TestnetProvisioning.formatTchr(costRaw), json["costTchr"]!!.jsonPrimitive.content)
        val funding = json["funding"]!!.jsonObject
        assertEquals(true, funding["configured"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "env:${TestnetProvisioning.FUNDING_KEY_ENV}",
            funding["keySource"]!!.jsonPrimitive.content
        )
        // A throwaway key's account does not exist on the chain, so the id is
        // the DERIVED one and the chain reports it as unregistered.
        assertEquals(accountIdFor(privHex), funding["accountId"]!!.jsonPrimitive.content)
        assertEquals(false, funding["registered"]!!.jsonPrimitive.content.toBoolean())
        assertFalse(resultText(result).contains(privHex, ignoreCase = true))
    }

    @Test
    fun provisionWithoutFundingKeyDegradesToDryRunWithClearMessage(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("prices a lease with no funding key configured and refuses to go live")
        val strategy = provisionStrategy(envWith(dir = dir), dir)
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, resultText(result))
        assertEquals(
            false,
            json["funding"]!!.jsonObject["configured"]!!.jsonPrimitive.content.toBoolean()
        )
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains(TestnetProvisioning.FUNDING_KEY_ENV))
    }

    @Test
    fun provisionRejectsUnknownClusterAndBadDurationLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("validates cluster and lease duration against the live chain's own limits")
        val strategy = provisionStrategy(envWith(dir = dir), dir)
        val repository = LiveChromia.repository()

        // The live cluster list is the reference for everything below.
        val default = strategy.execute(call("provision_testnet_container", buildJsonObject {}), repository)
        assertTrue(default.isError != true, resultText(default))
        val liveCluster = resultJson(default)["cluster"]!!.jsonPrimitive.content
        assertEquals(TestnetProvisioning.DEFAULT_CLUSTER, liveCluster, resultText(default))

        val bad = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("cluster", "nope") }),
            repository
        )
        assertEquals(true, bad.isError, resultText(bad))
        assertTrue(resultText(bad).contains("is not a leasable dapp cluster"), resultText(bad))
        assertTrue(resultText(bad).contains(liveCluster), resultText(bad))

        // The system cluster is real and listed, and must still be refused.
        val system = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("cluster", "system") }),
            repository
        )
        assertEquals(true, system.isError, resultText(system))

        val tooLong = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("durationWeeks", 100_000) }),
            repository
        )
        assertEquals(true, tooLong.isError, resultText(tooLong))
        assertTrue(resultText(tooLong).contains("durationWeeks must be between"), resultText(tooLong))

        // `Blue` is not a different cluster (DX audit 2026-09-04, T3): the dry
        // run folds onto the live name and prices it.
        val cased = strategy.execute(
            call(
                "provision_testnet_container",
                buildJsonObject { put("cluster", " ${liveCluster.uppercase()} ") }
            ),
            repository
        )
        assertTrue(cased.isError != true, resultText(cased))
        assertEquals(liveCluster, resultJson(cased)["cluster"]!!.jsonPrimitive.content)
        assertEquals("dry_run", resultJson(cased)["status"]!!.jsonPrimitive.content)
    }

    // DELETED 2026-09-07 (zero-doubles): provisionLiveLeasesContainerAndKeepsPrivateKeyServerSide
    // asserted that a live lease posts exactly one ft_auth + create_container_with_subnode_image
    // transaction carrying the freshly minted deploy pubkey, records the ephemeral private key
    // against the returned container name, and never prints it.
    // No real input can produce it: leasing a container SPENDS ~70 tCHR from a registered, funded
    // Economy Chain account, and no such private key exists in this repository or in CI - every
    // assertion was a read of what a FakePoster had been handed. The pieces that survive it: the
    // auth-descriptor/op shapes (singleSigAuthDescriptorGtvHasCompactShape), the real signing and
    // wire format (TestnetProvisioningLiveTest.liveSignedTransactionIsAcceptedOnWireAndRejectedBy-
    // Ft4Auth), and the keystore lifecycle (deployKeyStoreRoundtripsWithoutLeakingKeys). Nothing now
    // verifies the production branch that composes the lease transaction and records the
    // container -> pubkey mapping after a CONFIRMED lease.

    // DELETED 2026-09-07 (zero-doubles): provisionLiveRefusesWhenBalanceStillShortAfterFaucet
    // asserted that when a faucet claim confirms but the balance is still under the lease cost, the
    // tool posts the faucet transaction only, never the lease, and answers status "refused".
    // No real input can produce it: it needs a registered funded account whose balance stays short
    // after a CONFIRMED faucet claim - a chain state no live testnet account can be put into on
    // demand, and the "confirmed" outcome came from a fake poster. Nothing - the production
    // refuse-after-faucet branch is now unverified.

    // DELETED 2026-09-07 (zero-doubles): provisionLiveTopsUpFromFaucetThenLeases asserted that a
    // short balance is topped up from the on-chain faucet first and the lease is posted as a second
    // transaction afterwards.
    // No real input can produce it: it needs a registered account and two confirmed SPENDING
    // transactions; the balance only moved because the fake poster's callback moved it. Nothing -
    // the production top-up-then-lease sequencing is now unverified.

    // DELETED 2026-09-07 (zero-doubles): rejectedLeaseDiscardsTheEphemeralKeyItMinted asserted that
    // a REJECTED lease transaction discards the ephemeral deploy key minted for it, leaving no
    // orphan .key file behind (QA resource-lifecycle lens 2026-09-02).
    // No real input can produce it: reaching the lease post at all needs a registered funded
    // account, and the rejection itself was manufactured by a FakePoster returning confirmed=false.
    // The store half - discardEphemeral removes exactly the minted artifact and is honest on a
    // second call - is now asserted on the real store in deployKeyStoreRoundtripsWithoutLeakingKeys;
    // nothing now verifies that the production rejection branch CALLS it.

    @Test
    fun provisionUnregisteredAccountReportsExactBootstrapStepLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive(
            "asks the live Economy Chain about a throwaway account and requires the one-time bootstrap step"
        )
        val privHex = throwawayPrivHex()
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to privHex, dir = dir),
            Files.createDirectory(dir.resolve("keys"))
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content, resultText(result))
        val step = json["humanStep"]!!.jsonPrimitive.content
        assertTrue(step.contains(accountIdFor(privHex)), step)
        assertTrue(step.contains("tCHR"), step)
        assertFalse(resultText(result).contains(privHex, ignoreCase = true))
    }

    // DELETED 2026-09-07 (zero-doubles): provisionCompletesRegistrationFromPendingTransferThenLeases
    // asserted that a pending fee-strategy transfer is turned into ft4.ras_transfer_fee +
    // ft4.register_account and the lease then proceeds headlessly.
    // No real input can produce it: a pending account-creation transfer only exists after a funded
    // human transfer to that account, it expires within 24 hours, and completing it deducts a
    // 10 tCHR registration fee - none of which CI can have. The registration ops themselves are
    // built by TestnetProvisioning.registerAccountOps; nothing now verifies the production
    // complete-registration-then-lease branch.

    @Test
    fun provisionStatusPollRefusesAMalformedTxRid(@TempDir dir: Path) = runBlocking {
        // The rid is validated before any chain is read, so this drives the
        // production strategy against the production repository with no query
        // ever made - which is itself part of the claim.
        val strategy = provisionStrategy(envWith(dir = dir), dir)
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("statusTxRid", "not-a-rid") }),
            LiveChromia.repository()
        )
        assertEquals(true, result.isError, resultText(result))
        assertTrue(resultText(result).contains("statusTxRid must be the 64-hex"), resultText(result))
    }

    @Test
    fun provisionStatusPollReportsNoTicketForAnUnknownTxRidLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("asks the live Economy Chain for a create-container ticket that does not exist")
        val strategy = provisionStrategy(envWith(dir = dir), dir)
        // A well-formed rid of a transaction that was never posted.
        val unknownRid = "5A".repeat(32)
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("statusTxRid", unknownRid) }),
            LiveChromia.repository()
        )
        assertEquals(
            true, result.isError,
            "a txRid with no ticket must be an honest error, not a plausible ticket: ${result.structuredContent}"
        )
        assertTrue(resultText(result).contains("ticket"), resultText(result))
    }

    // DELETED 2026-09-07 (zero-doubles): provisionPendingTicketReturnsTxRidAndStatusPolling asserted
    // the submitted_pending -> provisioned transition: a live lease answers with a txRid while the
    // ICMF ticket is PENDING, and re-calling with statusTxRid once the ticket flips to SUCCESS
    // returns the container name.
    // No real input can produce it: it needs a real, paid lease transaction (funded account) and a
    // ticket observed both PENDING and SUCCESS, which was a mutable field on the fake chain. The two
    // halves that need no lease are now real: provisionStatusPollRefusesAMalformedTxRid and
    // provisionStatusPollReportsNoTicketForAnUnknownTxRidLive; nothing now verifies the production
    // PENDING -> SUCCESS transition or the tx -> container keystore re-mapping it performs.

    // DELETED 2026-09-07 (zero-doubles): provisionFailedTicketSurfacesChainError asserted that a
    // ticket in state FAILURE surfaces the chain's error_message ("no space in cluster") as a tool
    // error rather than a success.
    // No real input can produce it: a FAILURE ticket requires a paid lease that the cluster then
    // refuses, which cannot be provoked on demand and needs a funded account either way. Nothing -
    // the production TICKET_FAILURE branch is now unverified.

    // ---- claim --------------------------------------------------------------

    // DELETED 2026-09-07 (zero-doubles): claimDryRunReportsBalanceWithoutPosting asserted that the
    // claim dry run reports the account's balance and the 1000 tCHR claim amount, and is explicit
    // that it CANNOT predict on_cooldown.
    // No real input can produce it: the dry_run branch is only reached for a REGISTERED account, so
    // it needs a funding key whose FT4 account exists - which CI must not hold. A throwaway key
    // reaches blocked_human_step instead, which TestnetProvisioningLiveTest.liveClaimDryRunReports-
    // FaucetTerms already asserts live. Nothing now verifies the dry_run balance report or the
    // cannot-predict-cooldown wording.

    // DELETED 2026-09-07 (zero-doubles): claimLivePostsFaucetAndReportsNewBalance asserted that a
    // live claim posts ft4.ft_auth + faucet and reports the new balance.
    // No real input can produce it: it mints 1000 tCHR into a registered account - a real draw on a
    // real faucet allowance from a key CI must not hold. The signing pipeline it stood for is proven
    // live by TestnetProvisioningLiveTest.liveSignedTransactionIsAcceptedOnWireAndRejectedByFt4Auth;
    // nothing now verifies the claimed-balance report.

    // DELETED 2026-09-07 (zero-doubles): claimOnCooldownReportsExactWait asserted that a faucet
    // rejection carrying "You must wait 522000 seconds before claiming more Chromia Test" becomes
    // status on_cooldown with nextClaimableInSeconds=522000 instead of an error.
    // No real input can produce it: the reject reason was manufactured by a FakePoster, and
    // provoking it for real needs a registered account that claimed within the last 7 days. The wait
    // is parsed by a Regex literal inline in ClaimTestnetTchrStrategy.executeInner, so there is no
    // production function to call directly either. Nothing - the production on_cooldown branch is
    // now unverified; extracting that regex into a named function would make it testable.

    @Test
    fun claimWithoutFundingKeyNamesTheSetupStep(@TempDir dir: Path) = runBlocking {
        // No funding key resolves, so the tool answers before it reads any
        // chain: the production repository below is never queried, which is
        // part of the claim.
        val strategy = ClaimTestnetTchrStrategy(env = envWith(dir = dir))
        val result = strategy.execute(
            call("claim_testnet_tchr", buildJsonObject { put("dryRun", false) }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content, resultText(result))
        assertTrue(json["humanStep"]!!.jsonPrimitive.content.contains(TestnetProvisioning.FUNDING_KEY_ENV))
    }

    // ---- deploy -------------------------------------------------------------

    private val goodRell = mapOf(
        "main.rell" to """
            module;
            entity note { text; }
            query all_notes() = note @* {} (.text);
        """.trimIndent()
    )

    /**
     * write_deployment_config MERGES into a caller-supplied chromia.yml (F7: it
     * no longer invents a project file), so the base has to be the scaffold yml
     * for the chain under test - `defaultChromiaYml()` declares `hello`, and
     * deploying `my_dapp` against it is correctly refused as "not declared in
     * the provided chromiaYml".
     */
    private fun deployYml(container: String = "or_container_42", chain: String = "my_dapp"): String {
        val spec = WriteDeploymentConfig.resolveNetwork("testnet")!!
        return WriteDeploymentConfig.chromiaYml(spec, chain, DappScaffold.chromiaYmlFor(chain))
            .replace("<containerIID>", container)
    }

    /** A real keystore holding a deploy key for `or_container_42`, as a lease leaves it. */
    private fun keystoreForContainer(dir: Path, privHex: String = testPriv): Path {
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        val pubHex = TestnetProvisioning.derivePubKey(privHex.hexToBytes()).toHex()
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(pubHex, privHex)
            it.recordContainer("or_container_42", pubHex)
        }
        return keystoreDir
    }

    @Test
    fun deployRequiresSources(@TempDir dir: Path) = runBlocking {
        val strategy = deployStrategy(envWith(dir = dir), dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject { put("container", "c1") }),
            LiveChromia.repository()
        )
        assertEquals(true, result.isError)
        assertTrue(resultText(result).contains("rell"))
    }

    @Test
    fun deployRefusesOnCriticalSecurityFinding(@TempDir dir: Path) = runBlocking {
        // Banned admin import is a CRITICAL finding in RellSecurityCheck. The
        // security gate runs before the reachability probe, so this refusal is
        // decided in-process whatever the network is doing.
        val insecure = mapOf(
            "main.rell" to """
                module;
                import lib.ft4.admin;
            """.trimIndent()
        )
        val strategy = deployStrategy(envWith(dir = dir), dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { insecure.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
                put("dryRun", false)
            }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("refused", json["status"]!!.jsonPrimitive.content, resultText(result))
        assertNotNull(json["securityFindings"])
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains("security gate"))
    }

    @Test
    fun deployRefusesWhenSourcesDoNotCompile(@TempDir dir: Path) = runBlocking {
        // The compile blocker makes the preflight not-ready whether or not the
        // reachability probe reaches a node, so this holds on any network.
        val broken = mapOf("main.rell" to "module; this is not rell")
        val strategy = deployStrategy(envWith(dir = dir), dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { broken.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
                put("dryRun", false)
            }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("refused", json["status"]!!.jsonPrimitive.content, resultText(result))
        assertNotNull(json["preflight"])
    }

    @Test
    fun deployDryRunPassesEveryGateLiveAndReportsTheExactCommand(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive(
            "runs every deploy gate including the live testnet reachability probe on the Directory Chain"
        )
        val keystoreDir = keystoreForContainer(dir)
        val strategy = deployStrategy(envWith(dir = dir), keystoreDir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("blockchain", "my_dapp")
            }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, resultText(result))
        val command = json["command"]!!.jsonPrimitive.content
        assertTrue(command.contains("deployment create"), command)
        // D4 regression (live run 2026-09-02): without -y a headless chr blocks
        // on "Please specify -y option to force deployment".
        assertTrue(command.endsWith(" -y"), command)
        assertEquals(testPub, json["deployPubkey"]!!.jsonPrimitive.content)
        assertNotNull(json["chrResolution"], resultText(result))
        // A dry run never installs and never deploys: production returns here,
        // before the temp project directory is even created. Now that no
        // recording runner counts invocations, "dry_run" plus this note IS that
        // assertion - the only thing the real chr on this machine was asked for
        // on the way here is the read-only `--version`.
        assertTrue(
            json["notes"]!!.jsonPrimitive.content.contains("nothing was deployed"),
            resultText(result)
        )
    }

    @Test
    fun deployRefusesAChainTheProvidedYmlDoesNotDeclareAndFoldsModeCase(@TempDir dir: Path) = runBlocking {
        // DX audit 2026-09-04 (T11/T12): blockchain="other" against a yml whose
        // only chain is my_dapp surfaced the preflight's first blocker (whatever
        // else was missing) and never the wrong name; mode="Update" was refused.
        // Both are settled before the security gate and the reachability probe,
        // so no chain is read - and the folded mode is visible in the message
        // the refusal carries, which is where it is asserted now.
        val keystoreDir = keystoreForContainer(dir)
        val strategy = deployStrategy(envWith(dir = dir), keystoreDir)
        val wrong = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("blockchain", "other")
                put("mode", " Update ")
            }),
            LiveChromia.repository()
        )
        assertEquals(true, wrong.isError, resultText(wrong))
        val text = resultText(wrong)
        assertTrue(
            text.contains("blockchain \\\"other\\\" is not declared in the provided chromiaYml (blockchains: my_dapp)"),
            text
        )
        // " Update " folded to "update".
        assertTrue(text.contains("`chr deployment update --blockchain other` has nothing to deploy"), text)
        assertTrue(text.contains("Pass blockchain=\\\"my_dapp\\\""), text)

        val badMode = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("mode", "replace")
            }),
            LiveChromia.repository()
        )
        assertEquals(true, badMode.isError, resultText(badMode))
        assertTrue(resultText(badMode).contains("mode must be"), resultText(badMode))
    }

    @Test
    fun outdatedChrNoteWarnsOnlyBelowTheDocumentedLayoutVersion() {
        // The box that ran the 2026-09-04 DX audit had chr 0.29.10 (Rell 0.15.0):
        // the generated yml was pinned honestly, but nothing said the CLI itself
        // was behind the 0.30.0 layout the pins document (T13). The warning is a
        // pure function of the version string chr prints, so it is asserted here
        // directly instead of through a runner told to claim it is 0.29.10.
        assertNull(outdatedChrNote("0.33.2"))
        assertNull(outdatedChrNote("0.30.0"))
        assertNull(outdatedChrNote(null))
        assertNull(outdatedChrNote("not.a.version"))
        val note: String = outdatedChrNote("0.29.10")
            ?: throw AssertionError("chr 0.29.10 predates the documented 0.30.0 layout and must be warned about")
        assertTrue(note.contains("WARNING: the installed chr 0.29.10 predates 0.30.0"), note)
        assertTrue(note.contains("Upgrade chr to a 0.33.x release"), note)
        assertEquals(listOf("my_dapp"), declaredChainNames(deployYml()))
        assertEquals(emptyList<String>(), declaredChainNames("compile:\n  rellVersion: 0.16.1\n"))
    }

    @Test
    fun deployWithoutDeployKeyNamesBlockedStepLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("passes the live preflight and then reports the missing deploy key")
        // Empty keystore, no CHROMIA_TESTNET_DEPLOY_PRIVKEY: the block is only
        // reached once every gate, including the live probe, has passed.
        val strategy = deployStrategy(envWith(dir = dir), dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("dryRun", false)
            }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content, resultText(result))
        assertTrue(json["humanStep"]!!.jsonPrimitive.content.contains(TestnetProvisioning.DEPLOY_KEY_ENV))
    }

    @Test
    fun deployWithoutChrNamesBlockedStepLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("passes the live preflight and then reports that chr cannot be launched")
        // chr is made genuinely unavailable: CHROMIA_CHR_BIN names a path where
        // no file exists, so the production ChrLocator resolves it and the
        // production RealProcessRunner fails to launch it for real.
        val keystoreDir = keystoreForContainer(dir)
        val strategy = deployStrategy(
            envWith(TestnetProvisioning.CHR_BIN_ENV to missingChrBin(dir), dir = dir),
            keystoreDir
        )
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("dryRun", false)
            }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content, resultText(result))
        val humanStep = json["humanStep"]!!.jsonPrimitive.content
        assertTrue(humanStep.contains("chr"), humanStep)
        // D1: a chr-not-available block must say HOW resolution was attempted
        // so the failure is diagnosable (the live 2026-09-02 run reported
        // "not available" while chr 0.29.10 sat on PATH as a .cmd shim).
        assertTrue(humanStep.contains("resolution attempted"), humanStep)
        assertTrue(humanStep.contains(TestnetProvisioning.CHR_BIN_ENV), humanStep)
        assertNotNull(json["chrResolution"], json.toString())
    }

    // DELETED 2026-09-07 (zero-doubles): deployLiveRunsChrHeadlesslyAndVerifies asserted the whole
    // headless deploy: chr sees the key ONLY through POSTCHAIN_CLIENT_PRIVKEY/PUBKEY, the yml handed
    // to it on a first create carries no `chains:` key (D2), the sources land under src/, `chr
    // install` runs before `chr deployment` and with NO key material in its environment, the argv
    // carries -y, and the new BRID is read back from the yml chr rewrote.
    // No real input can produce it: `chr deployment create` against testnet proposes a real chain in
    // a real leased container, signed by a funded deploy key - every assertion was a read of what a
    // FakeRunner had recorded or a write it had been told to make. The chr launch path itself is
    // proven live by realChrResolvesWithoutOverrideAndRuns; nothing now verifies the env isolation,
    // the install-before-deploy ordering, the no-chains-on-create yml, or the BRID read-back.

    @Test
    fun providedScaffoldYmlIsCompletedWithTheDeploymentsBlockLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("completes a scaffold chromia.yml and passes the live preflight with it")
        // First FT4 live deploy (2026-09-04): scaffold_dapp's chromia.yml handed
        // straight to deploy_testnet_chain with a container was REFUSED for
        // "deployments.testnet not found" - the tool knew the container and the
        // network. And its rellVersion pin is the production Rell, which an
        // older installed chr refuses with "Unknown Rell version".
        val keystoreDir = keystoreForContainer(dir)
        val scaffoldYml = DappScaffold.files("my_dapp").getValue("chromia.yml")
        assertFalse(scaffoldYml.contains("deployments:"), scaffoldYml)

        val result = deployStrategy(envWith(dir = dir), keystoreDir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", scaffoldYml)
                put("container", "or_container_42")
            }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, resultText(result))
        val notes = json["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("Appended the deployments.testnet block"), notes)
        assertTrue(notes.contains("container \"or_container_42\""), notes)

        // The Rell pin the tool chose must agree with the Rell the chr installed
        // on THIS machine actually bundles. The expectation comes from running
        // that chr, so both outcomes are asserted rather than one being skipped.
        val installedRell = installedChr()?.rell
        if (installedRell != null && installedRell != DappScaffold.RELL_VERSION) {
            assertEquals(installedRell, json["rellVersion"]!!.jsonPrimitive.content, resultText(result))
            assertTrue(
                json["rellVersionSource"]!!.jsonPrimitive.content
                    .contains("the provided pin ${DappScaffold.RELL_VERSION} was replaced"),
                resultText(result)
            )
            assertTrue(notes.contains("is not the Rell the installed chr bundles ($installedRell)"), notes)
        } else {
            assertNull(json["rellVersion"], resultText(result))
        }

        // A yml that already carries a deployments block is left alone.
        val complete = deployStrategy(envWith(dir = dir), keystoreDir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
            }),
            LiveChromia.repository()
        )
        assertEquals("dry_run", resultJson(complete)["status"]!!.jsonPrimitive.content, resultText(complete))
        val completeNotes = resultJson(complete)["notes"]!!.jsonPrimitive.content
        assertFalse(completeNotes.contains("Appended the deployments"), completeNotes)
    }

    @Test
    fun chrFailureTextIsClassifiedByTheProductionRegexesAndStrippedOfLibNoise() {
        // VERBATIM chr stderr from the first FT4 live deploy (2026-09-04). This
        // is DATA - the exact text the real CLI wrote - handed to the exact
        // production classifiers that decide whether the deploy happened. It
        // replaces a FakeRunner that was told to print it back.
        val libNoise = (1..17).joinToString("\n") {
            "lib/ft4/external/accounts/queries.rell($it:1) Warning: Variable 'x' cannot be null at this location"
        }
        val raced = "$libNoise\nErrors: 0, User Warnings: 0, Lib Warnings: 17\nquery: 400 Bad Request  " +
            "[proposal_blockchain:find_blockchain_rid(proposal_blockchain/proposal_blockchain.rell:107)] Query " +
            "'find_blockchain_rid' failed: No blockchain proposal found in given transaction from " +
            "https://node3.testnet.chromia.com:7740"
        val taken = "$libNoise\nDeployment of blockchain my_dapp failed: " +
            "[common:require_unique_blockchain(common/blockchain.rell:30)] " +
            "Operation 'proposal_blockchain:propose_blockchain' failed: A blockchain with the same name already exists"
        val other = "$libNoise\nInvalid blockchain configuration. Something else"

        // The Directory, not chr's exit code, decides whether the deploy
        // happened - but only for these two shapes, and they must not be
        // confused with each other or with an ordinary failure.
        val race = DeployTestnetChainStrategy.CHR_RID_LOOKUP_RACE_REGEX
        val nameTaken = DeployTestnetChainStrategy.CHR_NAME_TAKEN_REGEX
        assertTrue(race.containsMatchIn(raced), raced)
        assertFalse(race.containsMatchIn(taken), taken)
        assertFalse(race.containsMatchIn(other), other)
        assertTrue(nameTaken.containsMatchIn(taken), taken)
        assertFalse(nameTaken.containsMatchIn(raced), raced)
        assertFalse(nameTaken.containsMatchIn(other), other)

        // chr prints every vendored-library warning before the one line that
        // matters; on the FT4 template that is 17 lines, and the real error used
        // to fall off the 1500-char tail.
        val strippedTaken = DeployTestnetChainStrategy.withoutLibWarnings(taken)
        assertFalse(strippedTaken.contains(" Warning: "), strippedTaken)
        assertTrue(strippedTaken.contains("A blockchain with the same name already exists"), strippedTaken)
        assertEquals(
            "Invalid blockchain configuration. Something else",
            DeployTestnetChainStrategy.withoutLibWarnings(other)
        )
        assertEquals("a\nb", DeployTestnetChainStrategy.withoutLibWarnings("a\nlib/x.rell(1:1) Warning: y\nb\n"))
    }

    // DELETED 2026-09-07 (zero-doubles): theDirectoryNotChrsExitCodeDecidesWhetherTheDeployHappened
    // asserted the end-to-end branch selection around a non-zero chr exit: a raced
    // find_blockchain_rid becomes status "deployed" with the Directory's RID and a do-not-retry
    // note, a name already taken in THIS container becomes "already_deployed" with a
    // use-mode=update note, the same name taken ELSEWHERE becomes a pick-another-name error, and
    // anything else stays a failure with the lib warnings stripped.
    // No real input can produce it: every branch is selected by a chr exit code and stderr that only
    // a real, funded `chr deployment create` against a real leased container emits - a FakeRunner
    // was told to emit them, and a FakeChain was told what the Directory lists. The classification
    // itself is now asserted directly on the verbatim live stderr by
    // chrFailureTextIsClassifiedByTheProductionRegexesAndStrippedOfLibNoise; nothing now verifies
    // the production branch that combines a classification with the Directory lookup into a status.

    @Test
    fun chainsEntryRidAndWithChainsEntrySpliceTheDeployedChainRid() {
        // First live `mode=update` (2026-09-04, agent_hello): the yml carried no
        // deployments.testnet.chains.<name> RID, which is how `update` knows
        // WHICH chain to reconfigure. The splice and the read are pure text
        // functions, exercised here on a real generated chromia.yml.
        val base = deployYml()
        assertNull(chainsEntryRid(base, "my_dapp"))

        val rid = "AB".repeat(32)
        val pinned = withChainsEntry(base, "my_dapp", rid)
        assertEquals(rid, chainsEntryRid(pinned, "my_dapp"))
        assertTrue(pinned.contains("    chains:\n      my_dapp: x\"$rid\""), pinned)
        // Every other line is untouched.
        assertEquals(
            base.lines().filter { it.isNotBlank() },
            pinned.lines().filter { it.isNotBlank() && it.trim() != "chains:" && !it.contains("my_dapp: x\"") }
        )

        // Idempotent splice into an EXISTING chains block: one `chains:` key,
        // both entries readable.
        val twice = withChainsEntry(pinned, "other", "CD".repeat(32))
        assertEquals("CD".repeat(32), chainsEntryRid(twice, "other"))
        assertEquals(rid, chainsEntryRid(twice, "my_dapp"))
        assertEquals(1, twice.lines().count { it.trim() == "chains:" })
    }

    @Test
    fun deployUpdateCarriesTheChainRidAndNoConfirmFlagLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("runs the update-mode dry run against the live testnet preflight")
        // First live `mode=update` (2026-09-04, agent_hello): the dry run said
        // "ready" and chr exited 1 with "no such option -y" - `deployment update`
        // has no -y. The yml here already pins the chain's RID, so the tool has
        // no reason to ask the Directory (that lookup is covered live by
        // deployUpdateRefusesWhenTheDirectoryListsNoSuchChainLive).
        val keystoreDir = keystoreForContainer(dir)
        val pinnedYml = withChainsEntry(deployYml(), "my_dapp", "04".repeat(32))

        val update = deployStrategy(envWith(dir = dir), keystoreDir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", pinnedYml)
                put("mode", " Update ")
            }),
            LiveChromia.repository()
        )
        val updateJson = resultJson(update)
        assertEquals("dry_run", updateJson["status"]!!.jsonPrimitive.content, resultText(update))
        val updateCommand = updateJson["command"]!!.jsonPrimitive.content
        assertTrue(updateCommand.contains("deployment update"), updateCommand)
        assertFalse(updateCommand.contains(" -y"), "update has no -y: $updateCommand")

        // create keeps -y.
        val create = deployStrategy(envWith(dir = dir), keystoreDir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
            }),
            LiveChromia.repository()
        )
        val createCommand = resultJson(create)["command"]!!.jsonPrimitive.content
        assertTrue(createCommand.endsWith(" -y"), createCommand)
    }

    @Test
    fun deployUpdateRefusesWhenTheDirectoryListsNoSuchChainLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("asks the live testnet Directory whether a container holds a chain to update")
        // mode=update with no RID in the yml: the tool asks the live Directory's
        // get_container_blockchain, which lists no such chain for this container,
        // so there is nothing to update and it must say so instead of reporting
        // "ready". Keyless and read-only.
        val keystoreDir = keystoreForContainer(dir)
        val result = deployStrategy(envWith(dir = dir), keystoreDir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("mode", "update")
            }),
            LiveChromia.repository()
        )
        assertEquals("refused", resultJson(result)["status"]!!.jsonPrimitive.content, resultText(result))
        assertTrue(resultText(result).contains("no deployed chain to update"), resultText(result))
        assertTrue(resultText(result).contains("Use mode=\\\"create\\\" for a first deploy"), resultText(result))
    }

    @Test
    fun deployDryRunAnnouncesTheInstallStepAndSkipsItWithoutLibsLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("runs the dry run that announces `chr install` against the live preflight")
        // First REAL deploy (2026-09-04): `chr deployment create` failed with
        // "Library ft4 is not installed, install before building" because
        // nothing had run `chr install` in the fresh project dir - the yml
        // declares libs, so install must precede the deployment.
        val keystoreDir = keystoreForContainer(dir)
        val dry = deployStrategy(envWith(dir = dir), keystoreDir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
            }),
            LiveChromia.repository()
        )
        val dryJson = resultJson(dry)
        assertEquals("dry_run", dryJson["status"]!!.jsonPrimitive.content, resultText(dry))
        assertTrue(dryJson["installCommand"]!!.jsonPrimitive.content.endsWith(" install"), dryJson.toString())
        assertTrue(
            dryJson["notes"]!!.jsonPrimitive.content
                .contains("first runs `chr install` (chromia.yml declares libs: ft4)"),
            dryJson.toString()
        )

        // A yml without libs skips the step entirely.
        val noLibs = deployYml().replace(Regex("(?m)^libs:\\R(?:[ \\t]+.*\\R?)*"), "")
        assertFalse(noLibs.contains("libs:"), noLibs)
        assertTrue(noLibs.contains("deployments:"), noLibs)
        val plain = deployStrategy(envWith(dir = dir), keystoreDir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", noLibs)
            }),
            LiveChromia.repository()
        )
        assertEquals("dry_run", resultJson(plain)["status"]!!.jsonPrimitive.content, resultText(plain))
        assertNull(resultJson(plain)["installCommand"], resultText(plain))
        assertEquals(emptyList<String>(), declaredLibNames(noLibs))
        assertEquals(listOf("ft4"), declaredLibNames(deployYml()))
    }

    // DELETED 2026-09-07 (zero-doubles): the live half of
    // deployDryRunAnnouncesTheInstallStepAndALiveInstallFailureIsNamed asserted that a FAILING
    // `chr install` is reported as "chr install failed (exit 1) before the deployment could build",
    // names the declared libs, quotes the CLI output, and is never followed by a deploy.
    // No real input can produce it here: reaching the live install step needs dryRun=false past
    // every gate with a funded deploy key, and a genuine install failure needs a lib whose registry
    // cannot resolve - a multi-minute network fetch inside the 300s chr timeout on every CI run.
    // The failure text was a FakeRunner's installStderr. Nothing - the production install-failure
    // branch, and the "a failed install must not be followed by a deploy" ordering, are now
    // unverified.

    // ---- D1: chr binary resolution (live-run defect 2026-09-02) -------------
    // ProcessBuilder cannot launch the .cmd shim scoop installs for chr, so a
    // bare `chr` spawn reported "chr is not available" while chr 0.29.10 sat
    // on PATH. Resolution must honor CHROMIA_CHR_BIN, then PATH+PATHEXT.
    //
    // These used to hand ChrLocator a `fileExists` lambda that decided which
    // paths existed. They now create REAL files in a temp directory and let it
    // use its own production predicate, so what is asserted is what the
    // filesystem answers.

    @Test
    fun chrLocatorHonorsExplicitOverrideFirst() {
        val plain = ChrLocator.resolve(
            mapOf(TestnetProvisioning.CHR_BIN_ENV to "/opt/chromia/bin/chr"),
            windows = false
        )
        assertEquals(listOf("/opt/chromia/bin/chr"), plain.command)
        assertTrue(plain.source.contains(TestnetProvisioning.CHR_BIN_ENV), plain.source)

        // A .cmd/.bat override on Windows is wrapped in `cmd /c` - Java cannot
        // launch a script directly.
        val bat = ChrLocator.resolve(
            mapOf(TestnetProvisioning.CHR_BIN_ENV to "C:\\tools\\chr.BAT"),
            windows = true
        )
        assertEquals(listOf("cmd", "/c", "C:\\tools\\chr.BAT"), bat.command)
        assertTrue(bat.source.contains("cmd /c"), bat.source)
    }

    @Test
    fun chrLocatorFindsScoopCmdShimViaPathext(@TempDir dir: Path) {
        // A real scoop-shaped shim on disk, found by the production predicate.
        val shims = Files.createDirectory(dir.resolve("shims"))
        val shim = Files.writeString(shims.resolve("chr.CMD"), "@echo off\r\n")
        val env = mapOf(
            // Mixed-case key: Windows exposes "Path", not "PATH".
            "Path" to "C:\\Windows\\system32;$shims",
            "PATHEXT" to ".COM;.EXE;.BAT;.CMD"
        )
        val r = ChrLocator.resolve(env, windows = true)
        assertEquals(listOf("cmd", "/c", shim.toString()), r.command)
        assertTrue(r.source.contains("PATHEXT"), r.source)
        assertTrue(r.source.contains("cmd /c"), r.source)
    }

    @Test
    fun chrLocatorPrefersExeOverCmdWithinPathextOrder(@TempDir dir: Path) {
        val exe = Files.writeString(dir.resolve("chr.EXE"), "MZ")
        Files.writeString(dir.resolve("chr.CMD"), "@echo off\r\n")
        val env = mapOf("PATH" to dir.toString(), "PATHEXT" to ".COM;.EXE;.BAT;.CMD")
        val r = ChrLocator.resolve(env, windows = true)
        // .EXE precedes .CMD in PATHEXT, and an exe needs no cmd /c wrapper.
        assertEquals(listOf(exe.toString()), r.command)
    }

    @Test
    fun chrLocatorKeepsBareChrOnNonWindows() {
        val r = ChrLocator.resolve(emptyMap(), windows = false)
        assertEquals(listOf("chr"), r.command)
        assertTrue(r.source.contains("PATH"), r.source)
    }

    @Test
    fun chrLocatorWindowsNotFoundFallsBackToCmdC(@TempDir dir: Path) {
        // A real, empty directory on PATH: nothing named chr.* exists in it.
        val empty = Files.createDirectory(dir.resolve("empty"))
        val r = ChrLocator.resolve(mapOf("PATH" to empty.toString()), windows = true)
        assertEquals(listOf("cmd", "/c", "chr"), r.command)
        assertTrue(r.source.contains("not found"), r.source)
    }

    @Test
    fun chrVersionsParseRealChrOutput() {
        // Verbatim `chr --version` output observed from chr 0.29.10.
        val v = ChrVersions.parse(
            "chr version 0.29.10\nrell version 0.15.0\npostchain version 3.47.6\n" +
                "EIF version 0.27.3\nJava version 21.0.2\n"
        )
        assertEquals("0.29.10", v.cli)
        assertEquals("0.15.0", v.rell)
        val none = ChrVersions.parse("'chr' is not recognized")
        assertNull(none.cli)
        assertNull(none.rell)
    }

    /**
     * LIVE proof of D1, assumption-gated on a working chr install: WITHOUT
     * CHROMIA_CHR_BIN, resolution must find and actually run the installed
     * CLI (on the 2026-09-02 dev box that is scoop's chr.cmd shim).
     */
    @Test
    fun realChrResolvesWithoutOverrideAndRuns() {
        val envNoOverride = System.getenv().filterKeys { it != TestnetProvisioning.CHR_BIN_ENV }
        val resolved = ChrLocator.resolve(envNoOverride)
        val out = runCatching {
            RealProcessRunner.run(resolved.command + "--version", Path.of("."), emptyMap(), 60_000)
        }.getOrNull()
        LiveEnv.requireChrRan(
            out != null && out.exitCode == 0,
            "resolution: ${resolved.source}, exit=${out?.exitCode}, stdout=${out?.stdout?.take(200)}"
        )
        val versions = ChrVersions.parse(out!!.stdout + "\n" + out.stderr)
        assertNotNull(versions.cli, "unparseable chr --version output: ${out.stdout}")
        assertNotNull(versions.rell, "unparseable chr --version output: ${out.stdout}")
    }

    // ---- D3: adaptive compile.rellVersion -----------------------------------

    @Test
    fun deployGeneratedYmlPinsTheRellTheInstalledChrBundlesLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("generates a chromia.yml and passes the live preflight with it")
        // Live-run defect 2026-09-02: chr 0.29.10 ships Rell 0.15.0 and rejects
        // the newer default pin with "Unknown Rell version", so the generated
        // config must pin the Rell the INSTALLED chr bundles. The expectation is
        // taken from running that chr, not from a fixture that claims a version.
        val probedRell: String? = installedChr()?.rell
        val result = deployStrategy(envWith(dir = dir), dir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
            }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, resultText(result))
        val pin = json["rellVersion"]!!.jsonPrimitive.content
        val source = json["rellVersionSource"]!!.jsonPrimitive.content
        if (probedRell != null) {
            assertEquals(probedRell, pin, resultText(result))
            assertTrue(source.contains("probed"), source)
            assertTrue(json["notes"]!!.jsonPrimitive.content.contains(pin), resultText(result))
        } else {
            assertEquals(DappScaffold.RELL_VERSION, pin, resultText(result))
            assertTrue(source.contains("default"), source)
        }

        // Whatever pin is chosen, the shipped validator must accept it - older
        // than the default pin is a warning, never an error - and that must hold
        // for the oldest Rell a supported chr bundles too.
        val spec = WriteDeploymentConfig.resolveNetwork("testnet")!!
        for (candidate in listOf(pin, "0.15.0")) {
            val adapted = WriteDeploymentConfig.chromiaYml(spec, "my_dapp", DappScaffold.defaultChromiaYml())
                .replace("rellVersion: ${DappScaffold.RELL_VERSION}", "rellVersion: $candidate")
                .replace("<containerIID>", "or_container_42")
            val validated = ChromiaYmlValidator.validate(adapted)
            assertTrue(validated.ok, "rellVersion $candidate: ${validated.errors}")
        }
    }

    @Test
    fun deployGeneratedYmlKeepsDefaultPinWhenChrCannotBeLaunchedLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("generates a chromia.yml with no chr available and passes the live preflight")
        // chr is genuinely unavailable: CHROMIA_CHR_BIN names a path with no
        // file at it, so the production launcher fails for real and the version
        // probe answers nothing.
        val result = deployStrategy(
            envWith(TestnetProvisioning.CHR_BIN_ENV to missingChrBin(dir), dir = dir),
            dir
        ).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
            }),
            LiveChromia.repository()
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, resultText(result))
        assertEquals(DappScaffold.RELL_VERSION, json["rellVersion"]!!.jsonPrimitive.content)
        assertTrue(json["rellVersionSource"]!!.jsonPrimitive.content.contains("default"), json.toString())
    }

    // ---- key-material sweep -------------------------------------------------

    /**
     * Sweeps every output path all three tools can REACH without a funded,
     * registered testnet account for key-shaped material: neither the funding
     * key nor the deploy key may appear in any result, on the dry-run paths or
     * on the blocked and degraded ones.
     *
     * The manufactured-leak arms of the old sweep - a poster whose reject
     * reason echoed the key, a poster that threw with the key in its message, a
     * chr that printed the key on its stdout - went with the doubles that
     * produced them, and with them the REDACTED positive control. What replaces
     * them is the redaction function asserted directly
     * (sanitizeTextRedactsSecretsCaseInsensitively) plus this sweep of every
     * output an unfunded run can actually produce.
     */
    @Test
    fun noReachableOutputPathEverContainsPrivateKeyMaterialLive(@TempDir dir: Path) = runBlocking {
        LiveChromia.requireLive("sweeps every reachable provisioning output path for key material")
        val fundingPriv = throwawayPrivHex()
        val deployPriv = throwawayPrivHex()
        val keystoreDir = keystoreForContainer(dir, deployPriv)
        val repository = LiveChromia.repository()
        val env = envWith(TestnetProvisioning.FUNDING_KEY_ENV to fundingPriv, dir = dir)
        val outputs = mutableListOf<String>()

        // provision: the dry run, and the live path that stops at the bootstrap.
        for (args in listOf(buildJsonObject {}, buildJsonObject { put("dryRun", false) })) {
            outputs += resultText(
                provisionStrategy(env, keystoreDir)
                    .execute(call("provision_testnet_container", args), repository)
            )
        }

        // claim: dry run and live run, both blocked on the unregistered account.
        for (args in listOf(buildJsonObject {}, buildJsonObject { put("dryRun", false) })) {
            outputs += resultText(
                ClaimTestnetTchrStrategy(env = env)
                    .execute(call("claim_testnet_tchr", args), repository)
            )
        }

        // deploy: the dry run whose result carries the resolved deploy PUBKEY
        // while its private half stays in the keystore, and the same call with
        // the key handed in through the env override instead.
        val deployArgs = buildJsonObject {
            put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
            put("chromiaYml", deployYml())
        }
        outputs += resultText(
            deployStrategy(env, keystoreDir).execute(call("deploy_testnet_chain", deployArgs), repository)
        )
        outputs += resultText(
            deployStrategy(
                envWith(TestnetProvisioning.DEPLOY_KEY_ENV to deployPriv, dir = dir),
                Files.createDirectory(dir.resolve("empty-keys"))
            ).execute(call("deploy_testnet_chain", deployArgs), repository)
        )

        // A malformed env key must not be echoed either - it degrades exactly
        // like an absent one, and its raw value is a secret from the moment it
        // is read.
        outputs += resultText(
            provisionStrategy(
                envWith(TestnetProvisioning.FUNDING_KEY_ENV to "not-a-key-$fundingPriv", dir = dir),
                keystoreDir
            ).execute(call("provision_testnet_container", buildJsonObject {}), repository)
        )

        for ((i, text) in outputs.withIndex()) {
            assertFalse(
                text.contains(fundingPriv, ignoreCase = true),
                "output #$i contains funding key material: ${text.take(300)}"
            )
            assertFalse(
                text.contains(deployPriv, ignoreCase = true),
                "output #$i contains deploy key material: ${text.take(300)}"
            )
        }
    }
}
