package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.domain.ChromiaRepository
import org.chromia.tools.ChrLocator
import org.chromia.tools.ChrVersions
import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.ClaimTestnetTchrStrategy
import org.chromia.tools.DappScaffold
import org.chromia.tools.DeployKeyStore
import org.chromia.tools.DeployTestnetChainStrategy
import org.chromia.tools.ProcOut
import org.chromia.tools.ProcessRunner
import org.chromia.tools.ProvisionTestnetContainerStrategy
import org.chromia.tools.RealProcessRunner
import org.chromia.tools.TestnetProvisioning
import org.chromia.tools.TestnetProvisioning.hexToBytes
import org.chromia.tools.TestnetProvisioning.toHex
import org.chromia.tools.TxOp
import org.chromia.tools.TxOutcome
import org.chromia.tools.TxPoster
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
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * provision_testnet_container / claim_testnet_tchr / deploy_testnet_chain:
 * agent-headless testnet provisioning. Unit-level only - the postchain query
 * seam and the TxPoster / ProcessRunner seams replace all network and key I/O.
 *
 * The GTV fixtures mirror shapes observed on the LIVE testnet Economy Chain
 * (2026-09-02): create_container_cost 70000000 for 1 SCU x 2 weeks on "blue",
 * clusters system/pink/blue, lease limits 1-12 weeks, and the account-id pair
 * (pubkey 021BBC..., account 9AA3F08C...) taken from a real registered account.
 */
class ProvisioningToolsTest {

    // A real (pubkey -> FT4 account id) pair observed on the live testnet
    // Economy Chain - proves accountIdForSingleSigner uses the exact merkle
    // hash FT4 uses.
    private val livePub = "021BBC7C1F2247719DC15376823C6AF13C1250E3836B6CAE5124DFA34E394BA44C"
    private val liveAccount = "9AA3F08C45D240EB89B5470355A0F1C4E5399C4DD534BEBF9777688AF1EE84B3"

    // Fixed test private key (never a real funded key). Its pubkey/account are
    // derived in-code so fixtures stay consistent.
    private val testPriv = "0101010101010101010101010101010101010101010101010101010101010101"
    private val testPub = TestnetProvisioning.derivePubKey(testPriv.hexToBytes()).toHex()
    private val testAccount = TestnetProvisioning.accountIdForSingleSigner(testPub.hexToBytes()).toHex()

    private val ecBrid = "090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874"
    private val adId = "AB".repeat(32)

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
    }

    // ---- fake chain ---------------------------------------------------------

    /**
     * Query fake mirroring live testnet Economy/Directory Chain answers.
     * Mutable knobs let tests vary balance, registration, tickets, pendings.
     */
    private class FakeChain(
        val pubHex: String,
        val accountHex: String,
        val adIdHex: String
    ) {
        var registered = true
        var balance = 200_000_000L // 200 tCHR
        var pendingStrategies = listOf<String>()
        var ticketState = 1
        var ticketContainerName = "or_container_42"
        var ticketErrorMessage = ""
        var ticketExists = true
        /** (name, rid) pairs the Directory lists for the container (`get_container_blockchain`). */
        var containerChains = listOf<Pair<String, String>>()
        val queriesSeen = mutableListOf<String>()

        fun answer(queryName: String, args: Gtv): Gtv {
            queriesSeen.add(queryName)
            return when (queryName) {
                "get_economy_chain_rid" ->
                    gtv("090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874".hexToBytes())
                "get_clusters" -> gtv(
                    listOf("system", "pink", "blue").map { name ->
                        gtv(mapOf(
                            "name" to gtv(name),
                            "cluster_units" to gtv(3L),
                            "extra_storage" to gtv(0L),
                            "is_operational" to gtv(1L),
                            "number_of_nodes" to gtv(3L),
                            "tag_name" to gtv(if (name == "system") "system" else "tag")
                        ))
                    }
                )
                "get_min_lease_duration" -> gtv(1L)
                "get_max_lease_duration" -> gtv(12L)
                "create_container_cost" -> gtv(70_000_000L)
                "ft4.get_accounts_by_signer" -> gtv(mapOf(
                    "data" to if (registered) gtv(listOf(gtv(mapOf(
                        "id" to gtv(accountHex.hexToBytes()),
                        "type" to gtv("FT4_USER")
                    )))) else gtv(emptyList<Gtv>()),
                    "next_cursor" to GtvNull
                ))
                "get_balance" -> gtv(balance)
                "ft4.get_pending_transfer_strategies" -> gtv(pendingStrategies.map { gtv(it) })
                "ft4.get_account_main_auth_descriptor" -> gtv(mapOf(
                    "id" to gtv(adIdHex.hexToBytes()),
                    "account_id" to gtv(accountHex.hexToBytes())
                ))
                "get_chr_asset" -> gtv(mapOf(
                    "id" to gtv("9EF73A786A66F435B3B40E72F5E9D85A4B09815997E087C809913E1E7EC686B4".hexToBytes()),
                    "symbol" to gtv("tCHR"),
                    "decimals" to gtv(6L)
                ))
                "get_create_container_ticket_by_transaction" ->
                    if (!ticketExists) GtvNull else gtv(mapOf(
                        "ticket_id" to gtv(7L),
                        "type" to gtv(0L),
                        "state" to gtv(ticketState.toLong()),
                        "error_message" to gtv(ticketErrorMessage),
                        "container_name" to gtv(ticketContainerName)
                    ))
                "get_lease_by_container_name" -> gtv(mapOf(
                    "container_name" to gtv(ticketContainerName),
                    "cluster_name" to gtv("blue")
                ))
                "get_container_blockchain" -> gtv(containerChains.map { (name, rid) ->
                    gtv(mapOf(
                        "name" to gtv(name), "rid" to gtv(rid.hexToBytes()),
                        "state" to gtv("RUNNING"), "system" to gtv(0L)
                    ))
                })
                else -> error("unexpected query in test: $queryName")
            }
        }
    }

    private fun repositoryFor(chain: FakeChain): ChromiaRepository {
        val config = ChromiaConfig(explorerUrl = McpTestSupport.EXPLORER_URL)
        val postchain = PostchainClientService(
            config,
            heightClient = { _, _ -> 40L },
            queryClient = { _, queryName, args -> chain.answer(queryName, args) }
        )
        return ChromiaRepositoryImpl(
            config = config,
            httpClientService = HttpClientService(config, McpTestSupport.errorEngine()),
            postchainClientService = postchain
        )
    }

    /** TxPoster that records posts; never sees the network. */
    private class FakePoster(
        var outcome: TxOutcome = TxOutcome("CD".repeat(32), true, null),
        val onPost: ((List<TxOp>) -> Unit)? = null
    ) : TxPoster {
        val posts = mutableListOf<List<TxOp>>()
        override fun post(urls: List<String>, bridHex: String, ops: List<TxOp>, privKey: ByteArray): TxOutcome {
            posts.add(ops)
            onPost?.invoke(ops)
            return outcome
        }
    }

    private fun envWith(vararg extra: Pair<String, String>, dir: Path): Map<String, String> =
        mapOf(TestnetProvisioning.CHROMIA_DIR_ENV to dir.toString(), *extra)

    private fun provisionStrategy(
        chainEnv: Map<String, String>,
        poster: TxPoster,
        keystoreDir: Path
    ) = ProvisionTestnetContainerStrategy(
        env = chainEnv,
        txPoster = poster,
        keyStore = DeployKeyStore(keystoreDir),
        delayFn = { /* no waiting in tests */ }
    )

    private fun call(name: String, args: JsonObject) = callToolRequest(name = name, arguments = args)

    private fun resultJson(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): JsonObject =
        result.structuredContent!!.jsonObject

    private fun resultText(result: io.modelcontextprotocol.kotlin.sdk.types.CallToolResult): String =
        Json.encodeToString(JsonObject.serializer(), result.structuredContent!!.jsonObject)

    // ---- provision: dry run -------------------------------------------------

    @Test
    fun provisionDryRunPricesAndValidatesWithoutPosting(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        val poster = FakePoster(onPost = { error("dry run must not post transactions") })
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), poster, dir
        )
        val result = strategy.execute(call("provision_testnet_container", buildJsonObject {}), repositoryFor(chain))
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content)
        assertEquals("70", json["costTchr"]!!.jsonPrimitive.content)
        assertEquals(70_000_000L, json["costRaw"]!!.jsonPrimitive.content.toLong())
        val funding = json["funding"]!!.jsonObject
        assertEquals(true, funding["configured"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(true, funding["registered"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(testAccount, funding["accountId"]!!.jsonPrimitive.content)
        assertEquals("200", funding["balanceTchr"]!!.jsonPrimitive.content)
        assertEquals(true, funding["sufficient"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("env:${TestnetProvisioning.FUNDING_KEY_ENV}", funding["keySource"]!!.jsonPrimitive.content)
        assertTrue(poster.posts.isEmpty())
    }

    @Test
    fun provisionWithoutFundingKeyDegradesToDryRunWithClearMessage(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        val strategy = provisionStrategy(envWith(dir = dir), FakePoster(), dir)
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content)
        assertEquals(
            false,
            json["funding"]!!.jsonObject["configured"]!!.jsonPrimitive.content.toBoolean()
        )
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains(TestnetProvisioning.FUNDING_KEY_ENV))
    }

    @Test
    fun provisionRejectsUnknownClusterAndBadDuration(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        val strategy = provisionStrategy(envWith(dir = dir), FakePoster(), dir)
        val bad = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("cluster", "nope") }),
            repositoryFor(chain)
        )
        assertEquals(true, bad.isError)
        assertTrue(resultText(bad).contains("blue"))

        val system = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("cluster", "system") }),
            repositoryFor(chain)
        )
        assertEquals(true, system.isError)

        val tooLong = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("durationWeeks", 13) }),
            repositoryFor(chain)
        )
        assertEquals(true, tooLong.isError)
        assertTrue(resultText(tooLong).contains("between 1 and 12"))

        // `Blue` is not a different cluster (DX audit 2026-09-04, T3): the dry
        // run folds onto the live name and prices it.
        val cased = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("cluster", " Blue ") }),
            repositoryFor(chain)
        )
        assertTrue(cased.isError != true, resultText(cased))
        assertEquals("blue", resultJson(cased)["cluster"]!!.jsonPrimitive.content)
        assertEquals("dry_run", resultJson(cased)["status"]!!.jsonPrimitive.content)
    }

    // ---- provision: live paths ----------------------------------------------

    @Test
    fun provisionLiveLeasesContainerAndKeepsPrivateKeyServerSide(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        val poster = FakePoster()
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), poster, keystoreDir
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("provisioned", json["status"]!!.jsonPrimitive.content)
        assertEquals("or_container_42", json["containerName"]!!.jsonPrimitive.content)
        val deployPub = json["deployPubkey"]!!.jsonPrimitive.content
        assertEquals(66, deployPub.length)

        // Exactly one tx: ft_auth + create_container_with_subnode_image.
        assertEquals(1, poster.posts.size)
        val ops = poster.posts.single()
        assertEquals(listOf("ft4.ft_auth", "create_container_with_subnode_image"), ops.map { it.name })
        val createArgs = ops[1].args
        assertEquals(deployPub, createArgs[0].asByteArray().toHex())
        assertEquals(1L, createArgs[1].asInteger())   // scu
        assertEquals(2L, createArgs[2].asInteger())   // durationWeeks
        assertEquals("blue", createArgs[4].asString())

        // The ephemeral private key exists server-side, mapped to the container...
        val store = DeployKeyStore(keystoreDir)
        assertEquals(deployPub, store.pubKeyForContainer("or_container_42"))
        assertNotNull(store.privKeyFor(deployPub))
        // ...and never appears in the output.
        val text = resultText(result)
        assertFalse(text.contains(store.privKeyFor(deployPub)!!.toHex(), ignoreCase = true))
    }

    @Test
    fun provisionLiveRefusesWhenBalanceStillShortAfterFaucet(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        chain.balance = 5_000_000L // 5 tCHR, cost is 70
        // Faucet "succeeds" but the fake balance stays short (e.g. cooldown burn).
        val poster = FakePoster()
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), poster, dir
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("refused", json["status"]!!.jsonPrimitive.content)
        // Only the faucet tx was attempted - never the lease.
        assertEquals(1, poster.posts.size)
        assertEquals(listOf("ft4.ft_auth", "faucet"), poster.posts.single().map { it.name })
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains("Refusing to lease"))
    }

    @Test
    fun provisionLiveTopsUpFromFaucetThenLeases(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        chain.balance = 5_000_000L
        val poster = FakePoster(onPost = { ops ->
            if (ops.any { it.name == "faucet" }) chain.balance += TestnetProvisioning.FAUCET_AMOUNT_RAW
        })
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), poster, dir
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("provisioned", json["status"]!!.jsonPrimitive.content)
        assertEquals(2, poster.posts.size)
        assertEquals(listOf("ft4.ft_auth", "faucet"), poster.posts[0].map { it.name })
        assertEquals(
            listOf("ft4.ft_auth", "create_container_with_subnode_image"),
            poster.posts[1].map { it.name }
        )
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains("Topped up from the on-chain faucet"))
    }

    /**
     * The ephemeral deploy key is written to the keystore BEFORE the lease tx
     * is posted. When the tx is rejected nothing on-chain will ever reference
     * that pubkey, yet the private key stayed on disk forever - one orphan
     * .key file per rejected lease, unreachable by any tx/container lookup
     * (QA concurrency and resource-lifecycle lens 2026-09-02). An explicit
     * rejection must discard it; a caller-provided pubkey has no server-side
     * key to discard.
     */
    @Test
    fun rejectedLeaseDiscardsTheEphemeralKeyItMinted(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        val poster = FakePoster(outcome = TxOutcome("EF".repeat(32), false, "insufficient balance"))
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), poster, keystoreDir
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        assertEquals(true, result.isError)
        val text = resultText(result)
        assertTrue(text.contains("rejected"), text)
        assertTrue(text.contains("discarded"), text)
        // The lease was attempted once with a freshly minted deploy pubkey...
        val createArgs = poster.posts.single { ops -> ops.any { it.name == "create_container_with_subnode_image" } }
            .last().args
        val mintedPub = createArgs[0].asByteArray().toHex()
        // ...and after the rejection its private half is gone, with no other
        // keystore artifact left behind either.
        val leftovers = Files.list(keystoreDir).use { it.map { p -> p.fileName.toString() }.toList() }
        assertTrue(leftovers.isEmpty(), "keystore must be empty after a rejected lease, found: $leftovers")
        assertNull(DeployKeyStore(keystoreDir).privKeyFor(mintedPub))
        // Discarding is idempotent and honest about what it removed.
        assertFalse(DeployKeyStore(keystoreDir).discardEphemeral(mintedPub))
    }

    @Test
    fun provisionUnregisteredAccountReportsExactBootstrapStep(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        chain.registered = false
        val poster = FakePoster(onPost = { error("must not post for an unregistered account without a pending transfer") })
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), poster, dir
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content)
        val step = json["humanStep"]!!.jsonPrimitive.content
        assertTrue(step.contains(testAccount))
        assertTrue(step.contains("tCHR"))
    }

    @Test
    fun provisionCompletesRegistrationFromPendingTransferThenLeases(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        chain.registered = false
        chain.pendingStrategies = listOf("fee")
        val poster = FakePoster(onPost = { ops ->
            if (ops.any { it.name == "ft4.register_account" }) chain.registered = true
        })
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), poster, dir
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("provisioned", json["status"]!!.jsonPrimitive.content)
        assertEquals(
            listOf("ft4.ras_transfer_fee", "ft4.register_account"),
            poster.posts.first().map { it.name }
        )
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains("registration completed headlessly"))
    }

    @Test
    fun provisionPendingTicketReturnsTxRidAndStatusPolling(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        chain.ticketState = 0
        val poster = FakePoster()
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), poster, dir
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("submitted_pending", json["status"]!!.jsonPrimitive.content)
        val txRid = json["txRid"]!!.jsonPrimitive.content
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains(txRid))

        // Poll: ticket resolves to SUCCESS.
        chain.ticketState = 1
        val poll = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("statusTxRid", txRid) }),
            repositoryFor(chain)
        )
        val pollJson = resultJson(poll)
        assertEquals("provisioned", pollJson["status"]!!.jsonPrimitive.content)
        assertEquals("or_container_42", pollJson["containerName"]!!.jsonPrimitive.content)
    }

    @Test
    fun provisionFailedTicketSurfacesChainError(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        chain.ticketState = 2
        chain.ticketErrorMessage = "no space in cluster"
        val strategy = provisionStrategy(
            envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir), FakePoster(), dir
        )
        val result = strategy.execute(
            call("provision_testnet_container", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        assertEquals(true, result.isError)
        assertTrue(resultText(result).contains("no space in cluster"))
    }

    // ---- claim --------------------------------------------------------------

    @Test
    fun claimDryRunReportsBalanceWithoutPosting(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        val poster = FakePoster(onPost = { error("dry run must not post") })
        val strategy = ClaimTestnetTchrStrategy(
            env = envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir),
            txPoster = poster
        )
        val result = strategy.execute(call("claim_testnet_tchr", buildJsonObject {}), repositoryFor(chain))
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content)
        assertEquals("200", json["balanceTchr"]!!.jsonPrimitive.content)
        assertEquals("1000", json["claimAmountTchr"]!!.jsonPrimitive.content)
        // Live run 2026-09-02: there is no on-chain cooldown query, so the dry
        // run must say it cannot predict on_cooldown rather than implying the
        // claim will succeed.
        val notes = json["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("CANNOT predict"), notes)
        assertTrue(notes.contains("on_cooldown"), notes)
    }

    @Test
    fun claimLivePostsFaucetAndReportsNewBalance(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        chain.balance = 0
        val poster = FakePoster(onPost = { chain.balance += TestnetProvisioning.FAUCET_AMOUNT_RAW })
        val strategy = ClaimTestnetTchrStrategy(
            env = envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir),
            txPoster = poster
        )
        val result = strategy.execute(
            call("claim_testnet_tchr", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("claimed", json["status"]!!.jsonPrimitive.content)
        assertEquals("1000", json["balanceTchr"]!!.jsonPrimitive.content)
        assertEquals(listOf("ft4.ft_auth", "faucet"), poster.posts.single().map { it.name })
    }

    @Test
    fun claimOnCooldownReportsExactWait(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        val poster = FakePoster(
            outcome = TxOutcome("EF".repeat(32), false,
                "Operation 'faucet' failed: You must wait 522000 seconds before claiming more Chromia Test")
        )
        val strategy = ClaimTestnetTchrStrategy(
            env = envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir),
            txPoster = poster
        )
        val result = strategy.execute(
            call("claim_testnet_tchr", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("on_cooldown", json["status"]!!.jsonPrimitive.content)
        assertEquals(522000L, json["nextClaimableInSeconds"]!!.jsonPrimitive.content.toLong())
    }

    @Test
    fun claimWithoutFundingKeyNamesTheSetupStep(@TempDir dir: Path) = runBlocking {
        val chain = FakeChain(testPub, testAccount, adId)
        val strategy = ClaimTestnetTchrStrategy(env = envWith(dir = dir), txPoster = FakePoster())
        val result = strategy.execute(
            call("claim_testnet_tchr", buildJsonObject { put("dryRun", false) }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content)
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

    private class FakeRunner(
        var versionExit: Int = 0,
        // Real `chr --version` output shape (chr 0.29.10, this machine).
        var versionStdout: String = "chr version 0.33.2\nrell version 0.16.1\npostchain version 3.47.6\n",
        var deployExit: Int = 0,
        var deployStdout: String = "",
        var deployStderr: String = "",
        var installExit: Int = 0,
        var installStderr: String = "",
        var onDeploy: ((workDir: Path, env: Map<String, String>) -> Unit)? = null
    ) : ProcessRunner {
        val commands = mutableListOf<List<String>>()
        val envsSeen = mutableListOf<Map<String, String>>()
        override fun run(command: List<String>, workDir: Path, extraEnv: Map<String, String>, timeoutMs: Long): ProcOut {
            commands.add(command)
            envsSeen.add(extraEnv)
            return if (command.contains("--version")) {
                ProcOut(versionExit, versionStdout, "")
            } else if (command.last() == "install") {
                // Real chr vendors the libs under src/lib; the build then finds them.
                if (installExit == 0) Files.createDirectories(workDir.resolve("src").resolve("lib").resolve("ft4"))
                ProcOut(installExit, "", installStderr)
            } else {
                onDeploy?.invoke(workDir, extraEnv)
                ProcOut(deployExit, deployStdout, deployStderr)
            }
        }
    }

    private fun deployStrategy(
        env: Map<String, String>,
        keystoreDir: Path,
        runner: ProcessRunner,
        tempDir: Path
    ) = DeployTestnetChainStrategy(
        env = env,
        keyStore = DeployKeyStore(keystoreDir),
        processRunner = runner,
        tempDirFactory = { Files.createTempDirectory(tempDir, "deploy") }
    )

    @Test
    fun deployRequiresSources(@TempDir dir: Path) = runBlocking {
        val strategy = deployStrategy(envWith(dir = dir), dir, FakeRunner(), dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject { put("container", "c1") }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        assertEquals(true, result.isError)
        assertTrue(resultText(result).contains("rell"))
    }

    @Test
    fun deployRefusesOnCriticalSecurityFinding(@TempDir dir: Path) = runBlocking {
        // Banned admin import is a CRITICAL finding in RellSecurityCheck.
        val insecure = mapOf(
            "main.rell" to """
                module;
                import lib.ft4.admin;
            """.trimIndent()
        )
        val strategy = deployStrategy(envWith(dir = dir), dir, FakeRunner(), dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { insecure.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
                put("dryRun", false)
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        val json = resultJson(result)
        assertEquals("refused", json["status"]!!.jsonPrimitive.content)
        assertNotNull(json["securityFindings"])
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains("security gate"))
    }

    @Test
    fun deployRefusesWhenSourcesDoNotCompile(@TempDir dir: Path) = runBlocking {
        val broken = mapOf("main.rell" to "module; this is not rell")
        val strategy = deployStrategy(envWith(dir = dir), dir, FakeRunner(), dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { broken.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
                put("dryRun", false)
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        val json = resultJson(result)
        assertEquals("refused", json["status"]!!.jsonPrimitive.content)
        assertNotNull(json["preflight"])
    }

    @Test
    fun deployDryRunRunsGatesAndReportsCommandWithoutRunningChr(@TempDir dir: Path) = runBlocking {
        val runner = FakeRunner()
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val strategy = deployStrategy(envWith(dir = dir), keystoreDir, runner, dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("blockchain", "my_dapp")
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content)
        val command = json["command"]!!.jsonPrimitive.content
        assertTrue(command.contains("deployment create"), command)
        // D4 regression (live run 2026-09-02): without -y a headless chr blocks
        // on "Please specify -y option to force deployment".
        assertTrue(command.endsWith(" -y"), command)
        assertEquals(testPub, json["deployPubkey"]!!.jsonPrimitive.content)
        // A dry run never runs install or deployment; the only chr invocation
        // is the read-only `--version` that checks the yml's Rell pin against
        // the installed CLI (first FT4 live deploy, 2026-09-04).
        assertTrue(runner.commands.all { it.contains("--version") }, runner.commands.toString())
    }

    @Test
    fun deployNamesAChainTheProvidedYmlDoesNotDeclareAndFoldsModeCase(@TempDir dir: Path) = runBlocking {
        // DX audit 2026-09-04 (T11/T12): blockchain="other" against a yml whose
        // only chain is my_dapp surfaced the preflight's first blocker (whatever
        // else was missing) and never the wrong name; mode="Update" was refused.
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val strategy = deployStrategy(envWith(dir = dir), keystoreDir, FakeRunner(), dir)
        val wrong = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("blockchain", "other")
                put("mode", "Update")
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        assertEquals(true, wrong.isError, resultText(wrong))
        val text = resultText(wrong)
        assertTrue(text.contains("blockchain \\\"other\\\" is not declared in the provided chromiaYml (blockchains: my_dapp)"), text)
        assertTrue(text.contains("`chr deployment update --blockchain other` has nothing to deploy"), text)
        assertTrue(text.contains("Pass blockchain=\\\"my_dapp\\\""), text)

        val cased = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("blockchain", "my_dapp")
                put("mode", " Update ")
            }),
            // update needs a deployed chain to target (the Directory lists it)
            repositoryFor(FakeChain(testPub, testAccount, adId).apply { containerChains = listOf("my_dapp" to "04".repeat(32)) })
        )
        val json = resultJson(cased)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, resultText(cased))
        assertTrue(json["command"]!!.jsonPrimitive.content.contains("deployment update"), resultText(cased))
    }

    @Test
    fun deployWarnsWhenTheInstalledChrPredatesTheDocumentedLayout(@TempDir dir: Path) = runBlocking {
        // The box that ran the 2026-09-04 DX audit had chr 0.29.10 (Rell 0.15.0):
        // the generated yml was pinned honestly, but nothing said the CLI itself
        // was behind the 0.30.0 layout the pins document (T13).
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val old = FakeRunner(versionStdout = "chr version 0.29.10\nrell version 0.15.0\n")
        val strategy = deployStrategy(envWith(dir = dir), keystoreDir, old, dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        val notes = resultJson(result)["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("WARNING: the installed chr 0.29.10 predates 0.30.0"), notes)
        assertTrue(notes.contains("Upgrade chr to a 0.33.x release"), notes)

        assertNull(outdatedChrNote("0.33.2"))
        assertNull(outdatedChrNote("0.30.0"))
        assertNull(outdatedChrNote(null))
        assertNotNull(outdatedChrNote("0.29.10"))
        assertEquals(listOf("my_dapp"), declaredChainNames(deployYml()))
        assertEquals(emptyList<String>(), declaredChainNames("compile:\n  rellVersion: 0.16.1\n"))
    }

    @Test
    fun deployWithoutDeployKeyNamesBlockedStep(@TempDir dir: Path) = runBlocking {
        val strategy = deployStrategy(envWith(dir = dir), dir, FakeRunner(), dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("dryRun", false)
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        val json = resultJson(result)
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content)
        assertTrue(json["humanStep"]!!.jsonPrimitive.content.contains(TestnetProvisioning.DEPLOY_KEY_ENV))
    }

    @Test
    fun deployWithoutChrNamesBlockedStep(@TempDir dir: Path) = runBlocking {
        val runner = FakeRunner(versionExit = 1)
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val strategy = deployStrategy(envWith(dir = dir), keystoreDir, runner, dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("dryRun", false)
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        val json = resultJson(result)
        assertEquals("blocked_human_step", json["status"]!!.jsonPrimitive.content)
        val humanStep = json["humanStep"]!!.jsonPrimitive.content
        assertTrue(humanStep.contains("chr"))
        // D1: a chr-not-available block must say HOW resolution was attempted
        // so the failure is diagnosable (the live 2026-09-02 run reported
        // "not available" while chr 0.29.10 sat on PATH as a .cmd shim).
        assertTrue(humanStep.contains("resolution attempted"), humanStep)
        assertTrue(humanStep.contains(TestnetProvisioning.CHR_BIN_ENV), humanStep)
        assertNotNull(json["chrResolution"], json.toString())
    }

    @Test
    fun deployLiveRunsChrHeadlesslyAndVerifies(@TempDir dir: Path) = runBlocking {
        val newBrid = "11".repeat(32)
        val runner = FakeRunner(onDeploy = { workDir, env ->
            // chr receives the key ONLY via env, and writes the dapp RID back.
            assertEquals(testPriv.uppercase(), env["POSTCHAIN_CLIENT_PRIVKEY"])
            assertEquals(testPub, env["POSTCHAIN_CLIENT_PUBKEY"])
            val yml = Files.readString(workDir.resolve("chromia.yml"))
            // D2 regression: the config chr sees on a FIRST create must have no
            // chains key at all (a placeholder null fails chr 0.29.10 with
            // "Incorrect type, expected string"). Note: `blockchains:` is fine.
            assertFalse(Regex("(?m)^\\s+chains:").containsMatchIn(yml), "yml handed to chr must omit chains:\n$yml")
            assertFalse(yml.contains("placeholder"), yml)
            // Simulate the CLI >= 0.30.0 write-back of the new dapp RID.
            Files.writeString(
                workDir.resolve("chromia.yml"),
                yml + "    chains:\n      my_dapp: x\"$newBrid\"\n"
            )
            // The sources landed under src/.
            assertTrue(Files.exists(workDir.resolve("src").resolve("main.rell")))
        })
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val chain = FakeChain(testPub, testAccount, adId)
        val repository = repositoryFor(chain)
        val strategy = deployStrategy(envWith(dir = dir), keystoreDir, runner, dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
                put("dryRun", false)
            }),
            repository
        )
        val json = resultJson(result)
        assertEquals("deployed", json["status"]!!.jsonPrimitive.content)
        assertEquals(newBrid.uppercase(), json["brid"]!!.jsonPrimitive.content)
        assertEquals(true, json["live"]!!.jsonPrimitive.content.toBoolean())
        // D4 regression at command-construction level: the actual argv chr was
        // launched with must carry -y (headless create prompts without it).
        val deployArgv = runner.commands.last { !it.contains("--version") }
        assertEquals("-y", deployArgv.last(), deployArgv.toString())
        assertTrue(deployArgv.containsAll(listOf("deployment", "create", "--network", "testnet")), deployArgv.toString())
        // The private key must not leak into the result even though chr saw it in env.
        assertFalse(resultText(result).contains(testPriv, ignoreCase = true))
        // First REAL deploy (2026-09-04): `chr deployment create` failed with
        // "Library ft4 is not installed, install before building" because
        // nothing had run `chr install` in the fresh project dir - the yml
        // declares libs, so install must precede the deployment, key-free.
        val installIdx = runner.commands.indexOfFirst { it.last() == "install" }
        val deployIdx = runner.commands.indexOfFirst { it.contains("deployment") }
        assertTrue(installIdx in 0 until deployIdx, "chr install must run before chr deployment: ${runner.commands}")
        assertEquals(emptyMap<String, String>(), runner.envsSeen[installIdx], "install needs no key material")
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains("chr install vendored the declared libs (ft4)"), json.toString())
    }

    @Test
    fun providedScaffoldYmlIsCompletedWithTheDeploymentsBlockAndTheInstalledRell(@TempDir dir: Path) = runBlocking {
        // First FT4 live deploy (2026-09-04): scaffold_dapp's chromia.yml handed
        // straight to deploy_testnet_chain with a container was REFUSED for
        // "deployments.testnet not found" - the tool knew the container and the
        // network. And its rellVersion pin is the production Rell, which an
        // older installed chr refuses with "Unknown Rell version".
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val chain = FakeChain(testPub, testAccount, adId)
        val scaffoldYml = DappScaffold.files("my_dapp").getValue("chromia.yml")
        assertFalse(scaffoldYml.contains("deployments:"), scaffoldYml)
        val old = FakeRunner(versionStdout = "chr version 0.29.10\nrell version 0.15.0\n")
        val strategy = deployStrategy(envWith(dir = dir), keystoreDir, old, dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", scaffoldYml)
                put("container", "or_container_42")
            }),
            repositoryFor(chain)
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, resultText(result))
        val notes = json["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("Appended the deployments.testnet block"), notes)
        assertTrue(notes.contains("container \"or_container_42\""), notes)
        assertTrue(notes.contains("compile.rellVersion ${DappScaffold.RELL_VERSION} in the provided chromiaYml is not the Rell the installed chr bundles (0.15.0)"), notes)
        assertTrue(notes.contains("WARNING: the installed chr 0.29.10 predates 0.30.0"), notes)
        assertEquals("0.15.0", json["rellVersion"]!!.jsonPrimitive.content)
        assertTrue(json["rellVersionSource"]!!.jsonPrimitive.content.contains("the provided pin ${DappScaffold.RELL_VERSION} was replaced"), json.toString())

        // A yml that already has a deployments block and a matching pin is left alone.
        val complete = deployStrategy(envWith(dir = dir), keystoreDir, FakeRunner(), dir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", deployYml())
            }),
            repositoryFor(chain)
        )
        val completeNotes = resultJson(complete)["notes"]!!.jsonPrimitive.content
        assertEquals("dry_run", resultJson(complete)["status"]!!.jsonPrimitive.content, resultText(complete))
        assertFalse(completeNotes.contains("Appended the deployments"), completeNotes)
        assertFalse(completeNotes.contains("was replaced"), completeNotes)
        assertNull(resultJson(complete)["rellVersion"], resultText(complete))
    }

    @Test
    fun theDirectoryNotChrsExitCodeDecidesWhetherTheDeployHappened(@TempDir dir: Path) = runBlocking {
        // First FT4 live deploy (2026-09-04): the proposal was accepted and the
        // chain came up RUNNING, but chr 0.29.10 exited 1 - its post-deploy
        // find_blockchain_rid hit a node that had not seen the block. The tool
        // reported a failure; the natural retry then failed for real with
        // "A blockchain with the same name already exists".
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val rid = "D8".repeat(32)
        val chain = FakeChain(testPub, testAccount, adId).apply { containerChains = listOf("my_dapp" to rid) }
        val libNoise = (1..17).joinToString("\n") { "lib/ft4/external/accounts/queries.rell($it:1) Warning: Variable 'x' cannot be null at this location" }
        val raced = FakeRunner(
            deployExit = 1,
            deployStderr = "$libNoise\nErrors: 0, User Warnings: 0, Lib Warnings: 17\nquery: 400 Bad Request  " +
                "[proposal_blockchain:find_blockchain_rid(proposal_blockchain/proposal_blockchain.rell:107)] Query " +
                "'find_blockchain_rid' failed: No blockchain proposal found in given transaction from https://node3.testnet.chromia.com:7740"
        )
        val args = buildJsonObject {
            put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
            put("chromiaYml", deployYml())
            put("dryRun", false)
        }
        val result = deployStrategy(envWith(dir = dir), keystoreDir, raced, dir)
            .execute(call("deploy_testnet_chain", args), repositoryFor(chain))
        assertTrue(result.isError != true, resultText(result))
        val json = resultJson(result)
        assertEquals("deployed", json["status"]!!.jsonPrimitive.content, resultText(result))
        assertEquals(rid, json["brid"]!!.jsonPrimitive.content)
        val notes = json["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("post-deploy RID lookup raced the block"), notes)
        assertTrue(notes.contains("the deploy SUCCEEDED - do not retry create"), notes)
        assertTrue("get_container_blockchain" in chain.queriesSeen, chain.queriesSeen.toString())

        // The name-taken refusal: in THIS container it is "already deployed"
        // (nothing changed, use mode=update); elsewhere on the network it is a
        // real refusal that names the fix.
        val taken = FakeRunner(
            deployExit = 1,
            deployStderr = "$libNoise\nDeployment of blockchain my_dapp failed: [common:require_unique_blockchain(common/blockchain.rell:30)] " +
                "Operation 'proposal_blockchain:propose_blockchain' failed: A blockchain with the same name already exists"
        )
        val already = deployStrategy(envWith(dir = dir), keystoreDir, taken, dir)
            .execute(call("deploy_testnet_chain", args), repositoryFor(chain))
        assertTrue(already.isError != true, resultText(already))
        assertEquals("already_deployed", resultJson(already)["status"]!!.jsonPrimitive.content)
        assertEquals(rid, resultJson(already)["brid"]!!.jsonPrimitive.content)
        val alreadyNotes = resultJson(already)["notes"]!!.jsonPrimitive.content
        assertTrue(alreadyNotes.contains("re-run with mode=\"update\""), alreadyNotes)
        assertFalse(alreadyNotes.contains(" Warning: "), "lib warnings must not bury the message: $alreadyNotes")

        val elsewhere = deployStrategy(envWith(dir = dir), keystoreDir, taken, dir)
            .execute(call("deploy_testnet_chain", args), repositoryFor(FakeChain(testPub, testAccount, adId)))
        assertEquals(true, elsewhere.isError, resultText(elsewhere))
        assertTrue(resultText(elsewhere).contains("already exists on testnet, but not in container"), resultText(elsewhere))
        assertTrue(resultText(elsewhere).contains("or_container_42"), resultText(elsewhere))
        assertTrue(resultText(elsewhere).contains("pick another name"), resultText(elsewhere))

        // Any other failure is still a failure - with the library noise stripped.
        val broken = FakeRunner(deployExit = 2, deployStderr = "$libNoise\nInvalid blockchain configuration. Something else")
        val failed = deployStrategy(envWith(dir = dir), keystoreDir, broken, dir)
            .execute(call("deploy_testnet_chain", args), repositoryFor(chain))
        assertEquals(true, failed.isError, resultText(failed))
        assertTrue(resultText(failed).contains("chr deployment create failed (exit 2): Invalid blockchain configuration. Something else"), resultText(failed))
        assertFalse(resultText(failed).contains(" Warning: "), resultText(failed))
        assertEquals("a\nb", DeployTestnetChainStrategy.withoutLibWarnings("a\nlib/x.rell(1:1) Warning: y\nb\n"))
    }

    @Test
    fun updateCarriesTheChainRidFromTheDirectoryAndNoConfirmFlag(@TempDir dir: Path) = runBlocking {
        // First live `mode=update` (2026-09-04, agent_hello): the dry run said
        // "ready" and chr exited 1 with "no such option -y" - `deployment update`
        // has no -y. And the yml carried no deployments.testnet.chains.<name>
        // RID, which is how `update` knows WHICH chain to reconfigure.
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val rid = "04".repeat(32)
        val chain = FakeChain(testPub, testAccount, adId).apply { containerChains = listOf("my_dapp" to rid) }
        var ymlSeenByChr: String? = null
        val runner = FakeRunner(onDeploy = { workDir, _ -> ymlSeenByChr = Files.readString(workDir.resolve("chromia.yml")) })
        val args = buildJsonObject {
            put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
            put("chromiaYml", deployYml())
            put("mode", "update")
        }
        val dry = deployStrategy(envWith(dir = dir), keystoreDir, runner, dir)
            .execute(call("deploy_testnet_chain", args), repositoryFor(chain))
        val dryJson = resultJson(dry)
        assertEquals("dry_run", dryJson["status"]!!.jsonPrimitive.content, resultText(dry))
        val command = dryJson["command"]!!.jsonPrimitive.content
        assertTrue(command.contains("deployment update"), command)
        assertFalse(command.contains(" -y"), "update has no -y: $command")
        assertTrue(dryJson["notes"]!!.jsonPrimitive.content.contains("Added deployments.testnet.chains.my_dapp: x\"$rid\""), resultText(dry))
        // create keeps -y
        val createCmd = resultJson(
            deployStrategy(envWith(dir = dir), keystoreDir, runner, dir).execute(
                call("deploy_testnet_chain", buildJsonObject { put("rell", args["rell"]!!); put("chromiaYml", deployYml()) }),
                repositoryFor(chain)
            )
        )["command"]!!.jsonPrimitive.content
        assertTrue(createCmd.endsWith(" -y"), createCmd)

        val live = deployStrategy(envWith(dir = dir), keystoreDir, runner, dir).execute(
            call("deploy_testnet_chain", buildJsonObject { args.forEach { (k, v) -> put(k, v) }; put("dryRun", false) }),
            repositoryFor(chain)
        )
        assertTrue(live.isError != true, resultText(live))
        assertEquals("deployed", resultJson(live)["status"]!!.jsonPrimitive.content, resultText(live))
        assertEquals(rid, resultJson(live)["brid"]!!.jsonPrimitive.content)
        assertTrue(resultJson(live)["notes"]!!.jsonPrimitive.content.contains("takes effect at a later block height"), resultText(live))
        val deployCommand = runner.commands.last { "deployment" in it }
        assertEquals("update", deployCommand[deployCommand.indexOf("deployment") + 1])
        assertFalse("-y" in deployCommand, deployCommand.toString())
        assertNotNull(ymlSeenByChr)
        assertEquals(rid, chainsEntryRid(ymlSeenByChr!!, "my_dapp"), ymlSeenByChr)
        assertTrue(ymlSeenByChr!!.contains("    chains:\n      my_dapp: x\"$rid\""), ymlSeenByChr)
        // every other line is untouched
        assertEquals(
            deployYml().lines().filter { it.isNotBlank() },
            ymlSeenByChr!!.lines().filter { it.isNotBlank() && it.trim() != "chains:" && !it.contains("my_dapp: x\"") }
        )

        // No chain of that name in the container: nothing to update, say so.
        val nothing = deployStrategy(envWith(dir = dir), keystoreDir, runner, dir)
            .execute(call("deploy_testnet_chain", args), repositoryFor(FakeChain(testPub, testAccount, adId)))
        assertEquals("refused", resultJson(nothing)["status"]!!.jsonPrimitive.content, resultText(nothing))
        assertTrue(resultText(nothing).contains("no deployed chain to update"), resultText(nothing))
        assertTrue(resultText(nothing).contains("Use mode=\\\"create\\\" for a first deploy"), resultText(nothing))

        // A yml that already names the RID is used as-is: no Directory lookup.
        val pinned = withChainsEntry(deployYml(), "my_dapp", "AB".repeat(32))
        assertEquals("AB".repeat(32), chainsEntryRid(pinned, "my_dapp"))
        val fresh = FakeChain(testPub, testAccount, adId)
        val asIs = deployStrategy(envWith(dir = dir), keystoreDir, runner, dir).execute(
            call("deploy_testnet_chain", buildJsonObject { put("rell", args["rell"]!!); put("chromiaYml", pinned); put("mode", "update") }),
            repositoryFor(fresh)
        )
        assertEquals("dry_run", resultJson(asIs)["status"]!!.jsonPrimitive.content, resultText(asIs))
        assertFalse("get_container_blockchain" in fresh.queriesSeen, fresh.queriesSeen.toString())
        // idempotent splice into an existing chains block
        val twice = withChainsEntry(pinned, "other", "CD".repeat(32))
        assertEquals("CD".repeat(32), chainsEntryRid(twice, "other"))
        assertEquals("AB".repeat(32), chainsEntryRid(twice, "my_dapp"))
        assertEquals(1, twice.lines().count { it.trim() == "chains:" })
        assertNull(chainsEntryRid(deployYml(), "my_dapp"))
    }

    @Test
    fun deployDryRunAnnouncesTheInstallStepAndALiveInstallFailureIsNamed(@TempDir dir: Path) = runBlocking {
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val chain = FakeChain(testPub, testAccount, adId)
        val args = buildJsonObject {
            put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
            put("chromiaYml", deployYml())
        }
        val dry = deployStrategy(envWith(dir = dir), keystoreDir, FakeRunner(), dir)
            .execute(call("deploy_testnet_chain", args), repositoryFor(chain))
        val dryJson = resultJson(dry)
        assertEquals("dry_run", dryJson["status"]!!.jsonPrimitive.content)
        assertTrue(dryJson["installCommand"]!!.jsonPrimitive.content.endsWith(" install"), dryJson.toString())
        assertTrue(dryJson["notes"]!!.jsonPrimitive.content.contains("first runs `chr install` (chromia.yml declares libs: ft4)"), dryJson.toString())

        val failing = FakeRunner(installExit = 1, installStderr = "Could not resolve lib ft4 from registry")
        val live = deployStrategy(envWith(dir = dir), keystoreDir, failing, dir).execute(
            call("deploy_testnet_chain", buildJsonObject { args.forEach { (k, v) -> put(k, v) }; put("dryRun", false) }),
            repositoryFor(chain)
        )
        assertEquals(true, live.isError, resultText(live))
        val text = resultText(live)
        assertTrue(text.contains("chr install failed (exit 1) before the deployment could build"), text)
        assertTrue(text.contains("libs (ft4)"), text)
        assertTrue(text.contains("Could not resolve lib ft4"), text)
        assertTrue(failing.commands.none { it.contains("deployment") }, "a failed install must not be followed by a deploy: ${failing.commands}")

        // A yml without libs skips the step entirely.
        val noLibs = deployYml().replace(Regex("(?m)^libs:\\R(?:[ \\t]+.*\\R?)*"), "")
        assertFalse(noLibs.contains("libs:"), noLibs)
        assertTrue(noLibs.contains("deployments:"), noLibs)
        val plain = deployStrategy(envWith(dir = dir), keystoreDir, FakeRunner(), dir).execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("chromiaYml", noLibs)
            }),
            repositoryFor(chain)
        )
        assertEquals("dry_run", resultJson(plain)["status"]!!.jsonPrimitive.content, resultText(plain))
        assertNull(resultJson(plain)["installCommand"], resultText(plain))
        assertEquals(emptyList<String>(), declaredLibNames(noLibs))
        assertEquals(listOf("ft4"), declaredLibNames(deployYml()))
    }

    // ---- D1: chr binary resolution (live-run defect 2026-09-02) -------------
    // ProcessBuilder cannot launch the .cmd shim scoop installs for chr, so a
    // bare `chr` spawn reported "chr is not available" while chr 0.29.10 sat
    // on PATH. Resolution must honor CHROMIA_CHR_BIN, then PATH+PATHEXT.

    @Test
    fun chrLocatorHonorsExplicitOverrideFirst() {
        val plain = ChrLocator.resolve(
            mapOf(TestnetProvisioning.CHR_BIN_ENV to "/opt/chromia/bin/chr"),
            windows = false
        ) { false }
        assertEquals(listOf("/opt/chromia/bin/chr"), plain.command)
        assertTrue(plain.source.contains(TestnetProvisioning.CHR_BIN_ENV), plain.source)

        // A .cmd/.bat override on Windows is wrapped in `cmd /c` - Java cannot
        // launch a script directly.
        val bat = ChrLocator.resolve(
            mapOf(TestnetProvisioning.CHR_BIN_ENV to "C:\\tools\\chr.BAT"),
            windows = true
        ) { false }
        assertEquals(listOf("cmd", "/c", "C:\\tools\\chr.BAT"), bat.command)
        assertTrue(bat.source.contains("cmd /c"), bat.source)
    }

    @Test
    fun chrLocatorFindsScoopCmdShimViaPathext() {
        val shim = Path.of("C:\\Users\\dev\\scoop\\shims", "chr.CMD")
        val env = mapOf(
            // Mixed-case key: Windows exposes "Path", not "PATH".
            "Path" to "C:\\Windows\\system32;C:\\Users\\dev\\scoop\\shims",
            "PATHEXT" to ".COM;.EXE;.BAT;.CMD"
        )
        val r = ChrLocator.resolve(env, windows = true) { it == shim }
        assertEquals(listOf("cmd", "/c", shim.toString()), r.command)
        assertTrue(r.source.contains("PATHEXT"), r.source)
        assertTrue(r.source.contains("cmd /c"), r.source)
    }

    @Test
    fun chrLocatorPrefersExeOverCmdWithinPathextOrder() {
        val exe = Path.of("C:\\chromia", "chr.EXE")
        val cmd = Path.of("C:\\chromia", "chr.CMD")
        val env = mapOf("PATH" to "C:\\chromia", "PATHEXT" to ".COM;.EXE;.BAT;.CMD")
        val r = ChrLocator.resolve(env, windows = true) { it == exe || it == cmd }
        // .EXE precedes .CMD in PATHEXT, and an exe needs no cmd /c wrapper.
        assertEquals(listOf(exe.toString()), r.command)
    }

    @Test
    fun chrLocatorKeepsBareChrOnNonWindows() {
        val r = ChrLocator.resolve(emptyMap(), windows = false) { true }
        assertEquals(listOf("chr"), r.command)
        assertTrue(r.source.contains("PATH"), r.source)
    }

    @Test
    fun chrLocatorWindowsNotFoundFallsBackToCmdC() {
        val r = ChrLocator.resolve(mapOf("PATH" to "C:\\nowhere"), windows = true) { false }
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
        assumeTrue(
            out != null && out.exitCode == 0,
            "no working chr on this machine (resolution: ${resolved.source})"
        )
        val versions = ChrVersions.parse(out!!.stdout + "\n" + out.stderr)
        assertNotNull(versions.cli, "unparseable chr --version output: ${out.stdout}")
        assertNotNull(versions.rell, "unparseable chr --version output: ${out.stdout}")
    }

    // ---- D3: adaptive compile.rellVersion -----------------------------------

    @Test
    fun deployGeneratedYmlAdaptsRellPinToInstalledCli(@TempDir dir: Path) = runBlocking {
        // Fake CLI = the real chr 0.29.10, which ships Rell 0.15.0 and rejects
        // the 0.33.x default pin with "Unknown Rell version" (live run defect).
        val runner = FakeRunner(
            versionStdout = "chr version 0.29.10\nrell version 0.15.0\npostchain version 3.47.6\n"
        )
        val strategy = deployStrategy(envWith(dir = dir), dir, runner, dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, json.toString())
        assertEquals("0.15.0", json["rellVersion"]!!.jsonPrimitive.content)
        assertTrue(json["rellVersionSource"]!!.jsonPrimitive.content.contains("probed"), json.toString())
        assertTrue(json["notes"]!!.jsonPrimitive.content.contains("0.15.0"), json.toString())
        // Probing is the only chr invocation on a dry run, and it is read-only.
        assertEquals(1, runner.commands.size, runner.commands.toString())
        assertTrue(runner.commands.single().contains("--version"))
        // The adaptive pin must also pass the shipped validator (older than
        // the default pin is a warning, never an error).
        val spec = WriteDeploymentConfig.resolveNetwork("testnet")!!
        val adapted = WriteDeploymentConfig.chromiaYml(spec, "my_dapp", DappScaffold.defaultChromiaYml())
            .replace("rellVersion: ${DappScaffold.RELL_VERSION}", "rellVersion: 0.15.0")
            .replace("<containerIID>", "or_container_42")
        val validated = ChromiaYmlValidator.validate(adapted)
        assertTrue(validated.ok, validated.errors.toString())
    }

    @Test
    fun deployGeneratedYmlKeepsDefaultPinWhenChrUnavailable(@TempDir dir: Path) = runBlocking {
        val runner = FakeRunner(versionExit = 1)
        val strategy = deployStrategy(envWith(dir = dir), dir, runner, dir)
        val result = strategy.execute(
            call("deploy_testnet_chain", buildJsonObject {
                put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
                put("container", "or_container_42")
            }),
            repositoryFor(FakeChain(testPub, testAccount, adId))
        )
        val json = resultJson(result)
        assertEquals("dry_run", json["status"]!!.jsonPrimitive.content, json.toString())
        assertEquals(DappScaffold.RELL_VERSION, json["rellVersion"]!!.jsonPrimitive.content)
        assertTrue(json["rellVersionSource"]!!.jsonPrimitive.content.contains("default"), json.toString())
    }

    // ---- key-material sweep -------------------------------------------------

    /**
     * Sweeps EVERY output path of all three tools for key-shaped material:
     * the configured funding private key must never appear in any result,
     * including error paths where the poster or chain echoes it back.
     */
    @Test
    fun noOutputPathEverContainsPrivateKeyMaterial(@TempDir dir: Path) = runBlocking {
        val leakyReason = "boom key=$testPriv leaked ${testPriv.lowercase()}"
        val outputs = mutableListOf<String>()

        val chain = FakeChain(testPub, testAccount, adId)
        val env = envWith(TestnetProvisioning.FUNDING_KEY_ENV to testPriv, dir = dir)
        val keystoreDir = Files.createDirectory(dir.resolve("keys"))

        // provision: dry run, live success, live rejection echoing the key,
        // poster throwing with the key in the exception message.
        val okPoster = FakePoster()
        val leakPoster = FakePoster(outcome = TxOutcome("00".repeat(32), false, leakyReason))
        val throwPoster = TxPoster { _, _, _, _ -> error(leakyReason) }
        for ((poster, args) in listOf(
            FakePoster() to buildJsonObject {},
            okPoster to buildJsonObject { put("dryRun", false) },
            leakPoster to buildJsonObject { put("dryRun", false) },
            throwPoster to buildJsonObject { put("dryRun", false) }
        )) {
            val strategy = ProvisionTestnetContainerStrategy(
                env = env, txPoster = poster, keyStore = DeployKeyStore(keystoreDir), delayFn = {}
            )
            outputs += resultText(strategy.execute(call("provision_testnet_container", args), repositoryFor(chain)))
        }

        // claim: dry run, claimed, rejection echoing the key.
        for ((poster, args) in listOf(
            FakePoster() to buildJsonObject {},
            FakePoster() to buildJsonObject { put("dryRun", false) },
            leakPoster to buildJsonObject { put("dryRun", false) }
        )) {
            val strategy = ClaimTestnetTchrStrategy(env = env, txPoster = poster)
            outputs += resultText(strategy.execute(call("claim_testnet_tchr", args), repositoryFor(chain)))
        }

        // deploy: dry run, chr failure echoing the key on stderr, live success
        // where chr writes the key into its stdout (worst case).
        DeployKeyStore(keystoreDir).also {
            it.storeEphemeral(testPub, testPriv)
            it.recordContainer("or_container_42", testPub)
        }
        val deployArgsBase = buildJsonObject {
            put("rell", buildJsonObject { goodRell.forEach { (k, v) -> put(k, v) } })
            put("chromiaYml", deployYml())
        }
        val failingRunner = FakeRunner(deployExit = 1, deployStderr = leakyReason)
        val leakyStdoutRunner = FakeRunner(deployStdout = "deployed! privkey=$testPriv")
        for ((runner, extra) in listOf(
            FakeRunner() to mapOf<String, kotlinx.serialization.json.JsonElement>(),
            failingRunner to mapOf("dryRun" to JsonPrimitive(false)),
            leakyStdoutRunner to mapOf("dryRun" to JsonPrimitive(false))
        )) {
            val strategy = deployStrategy(env, keystoreDir, runner, dir)
            val result = strategy.execute(
                call("deploy_testnet_chain", JsonObject(deployArgsBase + extra)),
                repositoryFor(chain)
            )
            outputs += resultText(result)
        }

        // funding-not-configured paths must not echo a malformed env value either.
        val malformedEnv = envWith(TestnetProvisioning.FUNDING_KEY_ENV to "not-a-key-$testPriv", dir = dir)
        val degraded = ProvisionTestnetContainerStrategy(
            env = malformedEnv, txPoster = FakePoster(), keyStore = DeployKeyStore(keystoreDir), delayFn = {}
        )
        outputs += resultText(
            degraded.execute(call("provision_testnet_container", buildJsonObject {}), repositoryFor(chain))
        )

        for ((i, text) in outputs.withIndex()) {
            assertFalse(
                text.contains(testPriv, ignoreCase = true),
                "output #$i contains private key material: ${text.take(300)}"
            )
        }
        // Positive control: the redaction marker appears where a leak was attempted.
        assertTrue(outputs.any { it.contains(TestnetProvisioning.REDACTED) })
    }
}
