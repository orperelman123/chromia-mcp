package org.chromia.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import net.postchain.common.BlockchainRid
import org.chromia.domain.ChromiaRepository
import org.chromia.domain.NetworkResult
import org.chromia.tools.TestnetProvisioning.hexToBytes
import org.chromia.tools.TestnetProvisioning.toHex

/** Scalar query results arrive wrapped as {"data": <value>}; objects arrive as-is. */
internal fun JsonObject.dataElement(): JsonElement = this["data"] ?: this

/**
 * Agent-headless testnet provisioning strategies. See [TestnetProvisioning]
 * for the live-verified ground truth these are built on.
 *
 * Shared design rules (owner-approved):
 *  - dryRun defaults to TRUE everywhere; nothing is signed or sent without an
 *    explicit dryRun=false.
 *  - Private keys are server-side only (env / keystore). They never appear in
 *    any output or error - every outgoing string passes [sanitize].
 *  - When a step genuinely needs a human (the one-time funding bootstrap), the
 *    tool says so precisely instead of pretending (no-fake rule).
 */
internal class EconomyChainGateway(
    private val repository: ChromiaRepository,
    private val network: String = "testnet",
    /**
     * Per-read wall-clock deadline for every blocking postchain-client query
     * this gateway makes. The merge that introduced these tools bypassed the
     * [ProbeBudget] family (cc76b9e): a node pool that stalls (the documented
     * TryNextOnError crawl) made every provisioning tool hang past the hosted
     * proxy's 60s write timeout - a closed socket instead of an honest answer.
     * Clamped; operator-tunable via [ProbeBudget.QUERY_DEADLINE_ENV].
     */
    private val queryDeadlineMs: Long = ProbeBudget.configuredDeadlineMs(ProbeBudget.QUERY_DEADLINE_ENV)
) {

    /** Secrets collected during a call; swept out of every outgoing string. */
    val secrets = mutableSetOf<String>()

    fun sanitize(text: String): String = TestnetProvisioning.sanitizeText(text, secrets)

    class ChainQueryException(message: String) : Exception(message)

    private var cachedEcBrid: String? = null

    /**
     * The Economy Chain BRID, resolved live from the testnet Directory Chain
     * (`get_economy_chain_rid`) rather than hardcoded, so a chain migration
     * cannot silently break provisioning.
     */
    suspend fun economyChainBrid(): String {
        cachedEcBrid?.let { return it }
        val result = query(
            WriteDeploymentConfig.TESTNET_DIRECTORY_BRID,
            "get_economy_chain_rid",
            emptyMap()
        )
        val hex = (result.dataElement() as? JsonPrimitive)?.contentOrNull
            ?: throw ChainQueryException("get_economy_chain_rid returned an unexpected shape")
        if (!Regex("^[0-9a-fA-F]{64}$").matches(hex)) {
            throw ChainQueryException("get_economy_chain_rid returned a non-BRID value")
        }
        return hex.uppercase().also { cachedEcBrid = it }
    }

    suspend fun query(bridHex: String, name: String, args: Map<String, Any?>): JsonObject {
        val result = ProbeBudget.withBudget(queryDeadlineMs) {
            repository.executeCustomQuery(
                network, BlockchainRid.buildFromHex(bridHex), name, args
            )
        } ?: throw ChainQueryException(
            "query $name timed out: no answer from the $network node pool within the " +
                "${queryDeadlineMs}ms deadline - the nodes may be slow or unreachable; retry, or " +
                "tune ${ProbeBudget.QUERY_DEADLINE_ENV} (capped at ${ProbeBudget.MAX_DEADLINE_MS}ms)"
        )
        return when (result) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> throw ChainQueryException(
                "query $name failed: ${result.message}"
            )
        }
    }

    suspend fun ecQuery(name: String, args: Map<String, Any?> = emptyMap()): JsonObject =
        query(economyChainBrid(), name, args)

    // ---- funding account ----------------------------------------------------

    data class FundingInfo(
        val configured: Boolean,
        val privKey: ByteArray?,
        val pubHex: String?,
        val accountIdHex: String?,
        val registered: Boolean,
        val balanceRaw: Long?,
        val pendingRegistration: Boolean,
        val keySourceLabel: String? = null
    )

    suspend fun resolveFunding(env: Map<String, String>): FundingInfo {
        // A present-but-malformed env key must degrade the same way as an
        // absent one - and its raw value must never be echoed.
        env[TestnetProvisioning.FUNDING_KEY_ENV]?.let { secrets.add(it) }
        val fundingKey = TestnetProvisioning.resolveFundingKey(env)
            ?: return FundingInfo(false, null, null, null, false, null, false)
        val privKey = fundingKey.privKey
        secrets.add(privKey.toHex())
        val pubKey = TestnetProvisioning.derivePubKey(privKey)
        val pubHex = pubKey.toHex()
        val derivedId = TestnetProvisioning.accountIdForSingleSigner(pubKey).toHex()

        // Prefer the chain's own answer for the account id: any account where
        // this key is a signer counts as the funding account.
        val accounts = ecQuery(
            "ft4.get_accounts_by_signer",
            mapOf("id" to pubKey, "page_size" to 1L, "page_cursor" to null)
        ).dataElement()
        val liveId = ((accounts as? JsonObject)?.get("data") as? JsonArray ?: accounts as? JsonArray)
            ?.firstOrNull()?.let { entry ->
                when (entry) {
                    is JsonObject -> entry["id"]?.jsonPrimitive?.contentOrNull
                    is JsonPrimitive -> entry.contentOrNull
                    else -> null
                }
            }

        val accountIdHex = (liveId ?: derivedId).uppercase()
        val registered = liveId != null
        val balanceRaw = if (registered) {
            (ecQuery("get_balance", mapOf("account_id" to accountIdHex.hexToBytes()))
                .dataElement() as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
        } else null

        val pendingRegistration = if (!registered) {
            val pending = ecQuery(
                "ft4.get_pending_transfer_strategies",
                mapOf("recipient_id" to derivedId.hexToBytes(), "filter" to null)
            ).dataElement()
            (pending as? JsonArray)?.any {
                (it as? JsonPrimitive)?.contentOrNull in setOf("fee", "open")
            } == true
        } else false

        return FundingInfo(
            true, privKey, pubHex, accountIdHex, registered, balanceRaw, pendingRegistration,
            keySourceLabel = fundingKey.sourceLabel
        )
    }

    /**
     * Completes the funding account's FT4 registration from a pending
     * fee-strategy transfer (the one-time human transfer has already landed).
     * Returns null on success or a sanitized failure reason.
     */
    suspend fun completeRegistration(funding: FundingInfo, txPoster: TxPoster, urls: List<String>): String? {
        val privKey = funding.privKey ?: return "no funding key"
        val pubKey = TestnetProvisioning.derivePubKey(privKey)
        val assetIdHex = ((ecQuery("get_chr_asset").dataElement() as? JsonObject)
            ?.get("id") as? JsonPrimitive)?.contentOrNull
            ?: return "could not resolve the tCHR asset id"
        val ops = TestnetProvisioning.registerAccountOps(assetIdHex.hexToBytes(), pubKey)
        val outcome = txPoster.post(urls, economyChainBrid(), ops, privKey)
        return if (outcome.confirmed) null
        else sanitize("account registration was rejected: ${outcome.rejectReason ?: "unknown reason"}")
    }

    suspend fun mainAuthDescriptorId(accountIdHex: String): ByteArray {
        val descriptor = ecQuery(
            "ft4.get_account_main_auth_descriptor",
            mapOf("account_id" to accountIdHex.hexToBytes())
        ).dataElement() as? JsonObject
            ?: throw ChainQueryException("could not read the account's main auth descriptor")
        val idHex = (descriptor["id"] as? JsonPrimitive)?.contentOrNull
            ?: throw ChainQueryException("main auth descriptor has no id")
        return idHex.hexToBytes()
    }

    suspend fun balanceOf(accountIdHex: String): Long? =
        (ecQuery("get_balance", mapOf("account_id" to accountIdHex.hexToBytes()))
            .dataElement() as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

    fun endpointUrls(): List<String> =
        org.chromia.data.config.ChromiaConfig().predefinedNetworks.getValue("testnet")

}

/**
 * `provision_testnet_container` - lease a container on the Chromia testnet
 * from the server-held funding account, fully headless. dryRun (default)
 * prices and validates everything without sending.
 */
class ProvisionTestnetContainerStrategy(
    private val env: Map<String, String> = System.getenv(),
    private val txPoster: TxPoster = RealTxPoster,
    private val keyStore: DeployKeyStore = DeployKeyStore(DeployKeyStore.defaultDir()),
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
    private val keyPairGenerator: () -> net.postchain.crypto.KeyPair = {
        TestnetProvisioning.cryptoSystem.generateKeyPair()
    }
) : BaseToolStrategy() {

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val gateway = EconomyChainGateway(
            repository,
            queryDeadlineMs = ProbeBudget.configuredDeadlineMs(
                ProbeBudget.QUERY_DEADLINE_ENV, env[ProbeBudget.QUERY_DEADLINE_ENV]
            )
        )
        return runCatching {
            executeInner(request, gateway)
        }.getOrElse { e ->
            toolErrorResult(gateway.sanitize("provision_testnet_container failed: ${e.message}"))
        }
    }

    private suspend fun executeInner(request: CallToolRequest, gateway: EconomyChainGateway): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val statusTxRid = extractString(args, "statusTxRid")?.takeIf { it.isNotBlank() }
        if (statusTxRid != null) return ticketStatus(statusTxRid, gateway)

        val requestedCluster = extractString(args, "cluster")?.trim()?.takeIf { it.isNotBlank() }
            ?: TestnetProvisioning.DEFAULT_CLUSTER
        val scu = extractPositiveInt(args, "scu") ?: TestnetProvisioning.DEFAULT_SCU
        val durationWeeks = extractPositiveInt(args, "durationWeeks")
            ?: TestnetProvisioning.DEFAULT_DURATION_WEEKS
        val extraStorageGib = extractNonNegativeInt(args, "extraStorageGib")
            ?: TestnetProvisioning.DEFAULT_EXTRA_STORAGE_GIB
        val autoRenew = extractBoolean(args, "autoRenew") ?: false
        val requestedDryRun = extractBoolean(args, "dryRun") ?: true
        val providedDeployPubkey = extractString(args, "deployPubkey")?.takeIf { it.isNotBlank() }
        if (providedDeployPubkey != null && TestnetProvisioning.parsePubKey(providedDeployPubkey) == null) {
            return toolErrorResult(
                "deployPubkey must be a 33-byte compressed secp256k1 public key in hex (66 hex chars)"
            )
        }

        val notes = mutableListOf<String>()

        // ---- validate cluster + duration against the live chain -------------
        val clusters = (gateway.ecQuery("get_clusters").dataElement() as? JsonArray) ?: JsonArray(emptyList())
        val clusterNames = clusters.mapNotNull {
            (it as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull
        }
        // `Blue` is not a different cluster (DX audit 2026-09-04): fold onto the
        // live name so the rest of the call - and the lease itself - use it.
        val cluster = clusterNames.firstOrNull { it == requestedCluster }
            ?: clusterNames.firstOrNull { it.equals(requestedCluster, ignoreCase = true) }
            ?: requestedCluster
        val clusterEntry = clusters.firstOrNull {
            (it as? JsonObject)?.get("name")?.jsonPrimitive?.contentOrNull == cluster
        } as? JsonObject
        if (cluster == "system" || clusterEntry == null) {
            return toolErrorResult(
                "cluster \"$cluster\" is not a leasable dapp cluster on testnet; live clusters: " +
                    clusterNames.filter { it != "system" }.joinToString(", ")
            )
        }

        val minWeeks = (gateway.ecQuery("get_min_lease_duration").dataElement() as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 1
        val maxWeeks = (gateway.ecQuery("get_max_lease_duration").dataElement() as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 12
        if (durationWeeks < minWeeks || durationWeeks > maxWeeks) {
            return toolErrorResult(
                "durationWeeks must be between $minWeeks and $maxWeeks (live chain limits); got $durationWeeks"
            )
        }

        // ---- live cost ------------------------------------------------------
        val costRaw = (gateway.ecQuery(
            "create_container_cost",
            mapOf(
                "container_units" to scu.toLong(),
                "duration_weeks" to durationWeeks.toLong(),
                "extra_storage_gib" to extraStorageGib.toLong(),
                "cluster_name" to cluster,
                "subnode_image_name" to "",
                "extra_compute_requests" to 0L
            )
        ).dataElement() as? JsonPrimitive)
            ?.contentOrNull?.toLongOrNull()
            ?: return toolErrorResult("create_container_cost returned an unexpected shape")

        // ---- funding --------------------------------------------------------
        val funding = gateway.resolveFunding(env)
        var dryRun = requestedDryRun
        if (!funding.configured && !requestedDryRun) {
            dryRun = true
            notes += TestnetProvisioning.noFundingKeyMessage()
        }

        var registered = funding.registered
        var balanceRaw = funding.balanceRaw
        var humanStep: String? = null

        if (funding.configured && !registered) {
            if (funding.pendingRegistration && !dryRun) {
                val failure = gateway.completeRegistration(funding, txPoster, gateway.endpointUrls())
                if (failure != null) return toolErrorResult(failure)
                registered = true
                balanceRaw = gateway.balanceOf(funding.accountIdHex!!)
                notes += "Funding account registration completed headlessly from the pending transfer."
            } else {
                humanStep = TestnetProvisioning.bootstrapHumanStep(funding.accountIdHex!!, costRaw)
                if (funding.pendingRegistration) {
                    notes += "A pending account-creation transfer is already waiting - re-run with " +
                        "dryRun=false and the server will complete registration and can then lease."
                }
            }
        }

        val sufficient = balanceRaw != null && balanceRaw >= costRaw

        // ---- dry run / blocked ---------------------------------------------
        val base = buildJsonObject {
            put("cluster", cluster)
            put("scu", scu)
            put("durationWeeks", durationWeeks)
            put("extraStorageGib", extraStorageGib)
            put("autoRenew", autoRenew)
            put("costTchr", TestnetProvisioning.formatTchr(costRaw))
            put("costRaw", costRaw)
            put("funding", buildJsonObject {
                put("configured", funding.configured)
                funding.keySourceLabel?.let { put("keySource", it) }
                funding.accountIdHex?.let { put("accountId", it) }
                put("registered", registered)
                balanceRaw?.let { put("balanceTchr", TestnetProvisioning.formatTchr(it)) }
                put("sufficient", sufficient)
            })
        }

        if (humanStep != null && !dryRun) {
            return sanitized(gateway, base, mapOf(
                "status" to JsonPrimitive("blocked_human_step"),
                "humanStep" to JsonPrimitive(humanStep),
                "notes" to JsonPrimitive(notes.joinToString(" "))
            ))
        }

        if (dryRun) {
            notes += "Dry run - nothing was signed or sent. Re-run with dryRun=false to lease " +
                "(cost ${TestnetProvisioning.formatTchr(costRaw)} tCHR is charged to the funding account; " +
                "the container name is assigned by the chain and returned)."
            if (funding.configured && registered && !sufficient) {
                val balance = balanceRaw ?: 0
                notes += "Funding balance ${TestnetProvisioning.formatTchr(balance)} tCHR does not cover the " +
                    "cost; a live run will first claim from the on-chain faucet " +
                    "(${TestnetProvisioning.formatTchr(TestnetProvisioning.FAUCET_AMOUNT_RAW)} tCHR per " +
                    "7 days per account) and refuse if still short. The chain exposes no cooldown query, " +
                    "so this dry run cannot predict whether that claim will succeed."
            }
            return sanitized(gateway, base, buildMap<String, JsonElement> {
                put("status", JsonPrimitive("dry_run"))
                humanStep?.let { put("humanStep", JsonPrimitive(it)) }
                put("notes", JsonPrimitive(notes.joinToString(" ")))
            })
        }

        // ---- live path ------------------------------------------------------
        val accountIdHex = funding.accountIdHex!!
        val privKey = funding.privKey!!
        val urls = gateway.endpointUrls()

        var liveBalance = balanceRaw ?: 0
        if (liveBalance < costRaw) {
            val adId = gateway.mainAuthDescriptorId(accountIdHex)
            val outcome = txPoster.post(
                urls, gateway.economyChainBrid(),
                listOf(TestnetProvisioning.ftAuthOp(accountIdHex.hexToBytes(), adId), TestnetProvisioning.faucetOp()),
                privKey
            )
            if (outcome.confirmed) {
                liveBalance = gateway.balanceOf(accountIdHex) ?: liveBalance
                notes += "Topped up from the on-chain faucet; balance is now " +
                    "${TestnetProvisioning.formatTchr(liveBalance)} tCHR."
            } else {
                notes += gateway.sanitize(
                    "On-chain faucet claim did not succeed (${outcome.rejectReason ?: "unknown"})."
                )
            }
            if (liveBalance < costRaw) {
                return sanitized(gateway, base, mapOf(
                    "status" to JsonPrimitive("refused"),
                    "notes" to JsonPrimitive(
                        (notes + ("Refusing to lease: funding balance " +
                            "${TestnetProvisioning.formatTchr(liveBalance)} tCHR cannot cover the " +
                            "${TestnetProvisioning.formatTchr(costRaw)} tCHR cost. The faucet grants " +
                            "${TestnetProvisioning.formatTchr(TestnetProvisioning.FAUCET_AMOUNT_RAW)} tCHR per " +
                            "account per 7 days - wait for the cooldown or fund the account.")
                        ).joinToString(" ")
                    )
                ))
            }
        }

        // Deploy key: caller-provided pubkey (they hold the private half) or a
        // server-generated ephemeral pair whose private half never leaves the
        // server keystore.
        val deployPubHex: String
        if (providedDeployPubkey != null) {
            deployPubHex = providedDeployPubkey.uppercase()
            notes += "Using the caller-provided deploy pubkey; its private key stays wherever the caller keeps it."
        } else {
            val pair = keyPairGenerator()
            deployPubHex = pair.pubKey.data.toHex()
            val privHex = pair.privKey.data.toHex()
            gateway.secrets.add(privHex)
            keyStore.storeEphemeral(deployPubHex, privHex)
            notes += "Generated an ephemeral deploy keypair; the private key is held server-side only."
        }

        val adId = gateway.mainAuthDescriptorId(accountIdHex)
        val outcome = txPoster.post(
            urls, gateway.economyChainBrid(),
            listOf(
                TestnetProvisioning.ftAuthOp(accountIdHex.hexToBytes(), adId),
                TestnetProvisioning.createContainerOp(
                    deployPubHex.hexToBytes(), scu, durationWeeks, extraStorageGib, cluster, autoRenew
                )
            ),
            privKey
        )
        if (!outcome.confirmed) {
            // The ephemeral key was minted for this tx alone; a rejected tx
            // means nothing on-chain will ever reference it - do not leave the
            // private key on disk (QA resource-lifecycle lens 2026-09-02).
            if (providedDeployPubkey == null) keyStore.discardEphemeral(deployPubHex)
            return toolErrorResult(gateway.sanitize(
                "container lease transaction was rejected: ${outcome.rejectReason ?: "unknown reason"}" +
                    (if (providedDeployPubkey == null) " The ephemeral deploy key generated for it was discarded." else "")
            ))
        }
        keyStore.recordTx(outcome.txRidHex, deployPubHex)

        // ---- poll the ICMF ticket ------------------------------------------
        var state: Int? = null
        var containerName: String? = null
        var errorMessage = ""
        for (attempt in 0 until TICKET_POLL_ATTEMPTS) {
            if (attempt > 0) delayFn(TICKET_POLL_INTERVAL_MS)
            val ticket = gateway.ecQuery(
                "get_create_container_ticket_by_transaction",
                mapOf("tx_rid" to outcome.txRidHex.hexToBytes())
            ).dataElement()
            val obj = ticket as? JsonObject ?: continue
            state = TestnetProvisioning.parseTicketState(
                (obj["state"] as? JsonPrimitive)?.contentOrNull
            )
            containerName = (obj["container_name"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            errorMessage = (obj["error_message"] as? JsonPrimitive)?.contentOrNull ?: ""
            if (state != null && state != TestnetProvisioning.TICKET_PENDING) break
        }

        val remaining = gateway.balanceOf(accountIdHex)
        return when (state) {
            TestnetProvisioning.TICKET_SUCCESS -> {
                containerName?.let { keyStore.recordContainer(it, deployPubHex) }
                sanitized(gateway, base, buildMap<String, JsonElement> {
                    put("status", JsonPrimitive("provisioned"))
                    put("txRid", JsonPrimitive(outcome.txRidHex))
                    containerName?.let { put("containerName", JsonPrimitive(it)) }
                    put("deployPubkey", JsonPrimitive(deployPubHex))
                    remaining?.let {
                        put("fundingBalanceAfterTchr", JsonPrimitive(TestnetProvisioning.formatTchr(it)))
                    }
                    put("notes", JsonPrimitive(
                        (notes + ("Container leased. Use deploy_testnet_chain with " +
                            "container=\"${containerName ?: "<name>"}\" - the matching deploy key is held " +
                            "server-side and used automatically.")).joinToString(" ")
                    ))
                })
            }
            TestnetProvisioning.TICKET_FAILURE -> toolErrorResult(gateway.sanitize(
                "container creation ticket FAILED on-chain: ${errorMessage.ifBlank { "no error message" }} " +
                    "(txRid ${outcome.txRidHex}; the lease cost may have been charged - check " +
                    "get_lease_purchases_by_account)"
            ))
            else -> sanitized(gateway, base, buildMap<String, JsonElement> {
                put("status", JsonPrimitive("submitted_pending"))
                put("txRid", JsonPrimitive(outcome.txRidHex))
                put("deployPubkey", JsonPrimitive(deployPubHex))
                put("notes", JsonPrimitive(
                    (notes + ("The lease transaction is confirmed and the cost is charged, but the " +
                        "container is still being created (asynchronous ICMF round trip). Re-call this tool " +
                        "with statusTxRid=\"${outcome.txRidHex}\" in ~30s to get the container name.")
                        ).joinToString(" ")
                ))
            })
        }
    }

    private suspend fun ticketStatus(txRidHex: String, gateway: EconomyChainGateway): CallToolResult {
        if (!Regex("^[0-9a-fA-F]{64}$").matches(txRidHex.trim())) {
            return toolErrorResult("statusTxRid must be the 64-hex transaction RID returned by a live provisioning call")
        }
        val ticket = gateway.ecQuery(
            "get_create_container_ticket_by_transaction",
            mapOf("tx_rid" to txRidHex.trim().hexToBytes())
        ).dataElement()
        val obj = ticket as? JsonObject
            ?: return toolErrorResult(
                "no create-container ticket found for txRid ${txRidHex.trim().uppercase()} - " +
                    "was this the txRid returned by provision_testnet_container?"
            )
        if (obj is JsonObject && obj.isEmpty()) {
            return toolErrorResult("no create-container ticket found for txRid ${txRidHex.trim().uppercase()}")
        }
        val state = TestnetProvisioning.parseTicketState((obj["state"] as? JsonPrimitive)?.contentOrNull)
        val containerName = (obj["container_name"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        val stateName = state?.let { TestnetProvisioning.ticketStateName(it) } ?: "UNKNOWN"
        if (state == TestnetProvisioning.TICKET_SUCCESS && containerName != null) {
            keyStore.pubKeyForTx(txRidHex.trim())?.let { keyStore.recordContainer(containerName, it) }
        }
        return toolSuccessResult(buildJsonObject {
            put("status", if (state == TestnetProvisioning.TICKET_SUCCESS) "provisioned" else "ticket_$stateName".lowercase())
            put("ticketState", stateName)
            containerName?.let { put("containerName", it) }
            (obj["error_message"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
                ?.let { put("errorMessage", gateway.sanitize(it)) }
            put("notes", when (state) {
                TestnetProvisioning.TICKET_SUCCESS ->
                    "Container is created. Use deploy_testnet_chain with container=\"$containerName\"."
                TestnetProvisioning.TICKET_PENDING ->
                    "Still creating - poll again shortly."
                else -> "Ticket did not succeed - see errorMessage."
            })
        })
    }

    private fun sanitized(
        gateway: EconomyChainGateway,
        base: JsonObject,
        extra: Map<String, JsonElement>
    ): CallToolResult {
        val merged = JsonObject(base + extra)
        val cleaned = sanitizeJson(merged, gateway) as JsonObject
        return toolSuccessResult(cleaned)
    }

    private fun sanitizeJson(element: JsonElement, gateway: EconomyChainGateway): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { sanitizeJson(it.value, gateway) })
        is JsonArray -> JsonArray(element.map { sanitizeJson(it, gateway) })
        is JsonPrimitive ->
            if (element.isString) JsonPrimitive(gateway.sanitize(element.content)) else element
        JsonNull -> element
    }

    private fun extractPositiveInt(args: Map<String, Any>, key: String): Int? {
        val value = extractString(args, key) ?: return null
        val parsed = value.trim().toIntOrNull()
            ?: throw IllegalArgumentException("$key must be a positive integer; got \"$value\"")
        require(parsed > 0) { "$key must be a positive integer; got $parsed" }
        return parsed
    }

    private fun extractNonNegativeInt(args: Map<String, Any>, key: String): Int? {
        val value = extractString(args, key) ?: return null
        val parsed = value.trim().toIntOrNull()
            ?: throw IllegalArgumentException("$key must be a non-negative integer; got \"$value\"")
        require(parsed >= 0) { "$key must be a non-negative integer; got $parsed" }
        return parsed
    }

    companion object {
        const val TICKET_POLL_ATTEMPTS = 10
        const val TICKET_POLL_INTERVAL_MS = 3_000L
    }
}

/**
 * `claim_testnet_tchr` - top up the server-held funding account from the
 * ON-CHAIN testnet faucet operation (module economy_chain_test_claim_tchr):
 * 1000 tCHR per account per 7 days, FT4-authenticated, no captcha at the
 * chain level. dryRun (default) reports balance and claimability only.
 */
class ClaimTestnetTchrStrategy(
    private val env: Map<String, String> = System.getenv(),
    private val txPoster: TxPoster = RealTxPoster
) : BaseToolStrategy() {

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val gateway = EconomyChainGateway(
            repository,
            queryDeadlineMs = ProbeBudget.configuredDeadlineMs(
                ProbeBudget.QUERY_DEADLINE_ENV, env[ProbeBudget.QUERY_DEADLINE_ENV]
            )
        )
        return runCatching {
            executeInner(request, gateway)
        }.getOrElse { e ->
            toolErrorResult(gateway.sanitize("claim_testnet_tchr failed: ${e.message}"))
        }
    }

    private suspend fun executeInner(request: CallToolRequest, gateway: EconomyChainGateway): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val requestedDryRun = extractBoolean(args, "dryRun") ?: true

        val funding = gateway.resolveFunding(env)
        val notes = mutableListOf<String>()
        var dryRun = requestedDryRun
        if (!funding.configured) {
            if (!requestedDryRun) dryRun = true
            return toolSuccessResult(buildJsonObject {
                put("status", "blocked_human_step")
                put("claimAmountTchr", TestnetProvisioning.formatTchr(TestnetProvisioning.FAUCET_AMOUNT_RAW))
                put("cooldownDays", TestnetProvisioning.FAUCET_COOLDOWN_MS / 86_400_000)
                put("humanStep", TestnetProvisioning.noFundingKeyMessage())
                put("notes", "The on-chain faucet mints to the authenticated FT4 account - a funding key must be configured first.")
            })
        }

        var registered = funding.registered
        var balanceRaw = funding.balanceRaw

        if (!registered) {
            if (funding.pendingRegistration && !dryRun) {
                val failure = gateway.completeRegistration(funding, txPoster, gateway.endpointUrls())
                if (failure != null) return toolErrorResult(failure)
                registered = true
                balanceRaw = gateway.balanceOf(funding.accountIdHex!!)
                notes += "Funding account registration completed headlessly from the pending transfer."
            } else {
                val accountIdHex = funding.accountIdHex!!
                return toolSuccessResult(sanitizeStrings(buildJsonObject {
                    put("status", "blocked_human_step")
                    put("accountId", accountIdHex)
                    put("humanStep", TestnetProvisioning.bootstrapHumanStep(accountIdHex, 0))
                    if (funding.pendingRegistration) {
                        put("notes", "A pending account-creation transfer is waiting - re-run with dryRun=false to complete registration and claim.")
                    } else {
                        put("notes", "The faucet needs an existing FT4 account; complete the one-time bootstrap first.")
                    }
                }, gateway))
            }
        }

        if (dryRun) {
            return toolSuccessResult(sanitizeStrings(buildJsonObject {
                put("status", "dry_run")
                put("accountId", funding.accountIdHex!!)
                balanceRaw?.let { put("balanceTchr", TestnetProvisioning.formatTchr(it)) }
                put("claimAmountTchr", TestnetProvisioning.formatTchr(TestnetProvisioning.FAUCET_AMOUNT_RAW))
                put("cooldownDays", TestnetProvisioning.FAUCET_COOLDOWN_MS / 86_400_000)
                put("notes", (notes + ("Dry run - nothing was sent. A live run signs faucet() with the " +
                    "server-held key and mints " +
                    "${TestnetProvisioning.formatTchr(TestnetProvisioning.FAUCET_AMOUNT_RAW)} tCHR " +
                    "unless the account claimed within the last 7 days. The Economy Chain exposes no query " +
                    "for the faucet cooldown, so this dry run CANNOT predict whether a claim will succeed - " +
                    "a live claim inside the window returns status on_cooldown with the exact wait instead " +
                    "of minting.")).joinToString(" "))
            }, gateway))
        }

        val accountIdHex = funding.accountIdHex!!
        val adId = gateway.mainAuthDescriptorId(accountIdHex)
        val outcome = txPoster.post(
            gateway.endpointUrls(), gateway.economyChainBrid(),
            listOf(TestnetProvisioning.ftAuthOp(accountIdHex.hexToBytes(), adId), TestnetProvisioning.faucetOp()),
            funding.privKey!!
        )
        if (!outcome.confirmed) {
            val reason = outcome.rejectReason ?: "unknown reason"
            val waitSeconds = Regex("wait (\\d+) seconds").find(reason)?.groupValues?.get(1)?.toLongOrNull()
            if (waitSeconds != null) {
                return toolSuccessResult(sanitizeStrings(buildJsonObject {
                    put("status", "on_cooldown")
                    put("accountId", accountIdHex)
                    balanceRaw?.let { put("balanceTchr", TestnetProvisioning.formatTchr(it)) }
                    put("nextClaimableInSeconds", waitSeconds)
                    put("notes", (notes + ("This account claimed within the last 7 days; next claim in " +
                        "~${waitSeconds / 3600} hours.")).joinToString(" "))
                }, gateway))
            }
            return toolErrorResult(gateway.sanitize("faucet claim was rejected: $reason"))
        }
        val newBalance = gateway.balanceOf(accountIdHex)
        return toolSuccessResult(sanitizeStrings(buildJsonObject {
            put("status", "claimed")
            put("accountId", accountIdHex)
            put("txRid", outcome.txRidHex)
            newBalance?.let { put("balanceTchr", TestnetProvisioning.formatTchr(it)) }
            put("claimAmountTchr", TestnetProvisioning.formatTchr(TestnetProvisioning.FAUCET_AMOUNT_RAW))
            put("notes", (notes + "Claimed from the on-chain faucet; next claim possible in 7 days.")
                .joinToString(" "))
        }, gateway))
    }

    private fun sanitizeStrings(obj: JsonObject, gateway: EconomyChainGateway): JsonObject =
        JsonObject(obj.mapValues { (_, v) ->
            if (v is JsonPrimitive && v.isString) JsonPrimitive(gateway.sanitize(v.content)) else v
        })
}

/**
 * `deploy_testnet_chain` - deploy dapp sources to a leased testnet container,
 * fully headless. Runs the compile + security gates FIRST (a broken or
 * insecure dapp must not reach a chain), then `chr deployment create|update`
 * signed by the server-held deploy key, then verifies the chain answers.
 * dryRun (default) runs every gate and reports the exact command without
 * executing it.
 */
class DeployTestnetChainStrategy(
    private val env: Map<String, String> = System.getenv(),
    private val keyStore: DeployKeyStore = DeployKeyStore(DeployKeyStore.defaultDir()),
    private val processRunner: ProcessRunner = RealProcessRunner,
    private val tempDirFactory: () -> java.nio.file.Path = {
        java.nio.file.Files.createTempDirectory("chromia-mcp-deploy")
    }
) : BaseToolStrategy() {

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val secrets = mutableSetOf<String>()
        return runCatching {
            executeInner(request, repository, secrets)
        }.getOrElse { e ->
            toolErrorResult(
                TestnetProvisioning.sanitizeText("deploy_testnet_chain failed: ${e.message}", secrets)
            )
        }
    }

    private suspend fun executeInner(
        request: CallToolRequest,
        repository: ChromiaRepository,
        secrets: MutableSet<String>
    ): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val rellParam = extractStringMap(args, "rell")
        val filesAlias = if (rellParam == null) extractStringMap(args, "files") else null
        val files = rellParam ?: filesAlias
            ?: return toolErrorResult(
                "Missing required parameter: rell (the dapp sources - one source string or an object of " +
                    "path -> source). The gates cannot vouch for code they never saw."
            )
        val blockchain = extractString(args, "blockchain")?.takeIf { it.isNotBlank() } ?: "my_dapp"
        // The name is a chromia.yml key AND a chr command-line argument. An
        // invalid name used to slip through: the generated scaffold silently
        // normalized it (DappScaffold.normalizeName falls back to "my_dapp")
        // while `chr --blockchain` kept the raw value, so a dry run reported
        // all gates passed for a deploy that could not succeed - and on
        // Windows the raw value reached `cmd /c`, where shell metacharacters
        // are live. Reject instead of silently substituting.
        if (!Regex("^[a-z][a-z0-9_]{0,31}$").matches(blockchain)) {
            return toolErrorResult(
                "blockchain \"${blockchain.take(60)}\" is not a valid chain name: it must start with a " +
                    "lowercase letter and contain only lowercase letters, digits, or underscores, at most " +
                    "32 characters ([a-z][a-z0-9_]{0,31}). The name keys the chromia.yml blockchains " +
                    "block and is passed to chr, so nothing else deploys the code you sent."
            )
        }
        val containerArg = extractString(args, "container")?.takeIf { it.isNotBlank() }
        // Spliced into the generated YAML and compared against the yml's own
        // container line, which only ever matches this charset - anything else
        // used to corrupt the YAML first and then fail with a misattributed
        // "conflicts with deployments.testnet.container" error.
        if (containerArg != null && !Regex("^[A-Za-z0-9_.-]{1,128}$").matches(containerArg)) {
            return toolErrorResult(
                "container \"${containerArg.take(60)}\" is not a valid container lease name (letters, " +
                    "digits, and _ . - only) - pass the name returned by provision_testnet_container."
            )
        }
        val providedYml = extractString(args, "chromiaYml")?.takeIf { it.isNotBlank() }
        val mode = when (val m = extractString(args, "mode")?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: "create") {
            "create", "update" -> m
            else -> return toolErrorResult("mode must be \"create\" or \"update\"; got \"$m\"")
        }
        val dryRun = extractBoolean(args, "dryRun") ?: true
        val notes = mutableListOf<String>()
        if (filesAlias != null) {
            notes += "`files` was accepted as an alias for the `rell` parameter."
        }

        // ---- chromia.yml ----------------------------------------------------
        val spec = WriteDeploymentConfig.resolveNetwork("testnet")!!
        var rellVersionUsed: String? = null
        var rellVersionSource: String? = null
        var yml = providedYml ?: run {
            if (containerArg == null) {
                return toolErrorResult(
                    "No chromiaYml was provided, so `container` is required to generate one - pass the " +
                        "container name returned by provision_testnet_container."
                )
            }
            var generated = WriteDeploymentConfig.chromiaYml(spec, blockchain)
            // Adaptive compile.rellVersion: the generated config is destined for
            // the INSTALLED chr, so pin the Rell it actually bundles. chr 0.29.10
            // ships Rell 0.15.0 and rejects the 0.33.x-era default pin
            // ${DappScaffold.RELL_VERSION} with "Unknown Rell version" (live run
            // 2026-09-02). Probing costs one read-only `chr --version` (cached).
            val probed = probeChrVersions()
            val probedRell = probed?.rell
            if (probedRell != null && probedRell != DappScaffold.RELL_VERSION) {
                generated = generated.replace(
                    "rellVersion: ${DappScaffold.RELL_VERSION}", "rellVersion: $probedRell"
                )
                rellVersionUsed = probedRell
                rellVersionSource = "probed: chr ${probed.cli ?: "?"} bundles Rell $probedRell"
            } else if (probedRell != null) {
                rellVersionUsed = DappScaffold.RELL_VERSION
                rellVersionSource = "probed: chr ${probed.cli ?: "?"} bundles Rell $probedRell (matches the default pin)"
            } else {
                rellVersionUsed = DappScaffold.RELL_VERSION
                rellVersionSource = "default ${DappScaffold.RELL_VERSION} (chr --version could not be probed)"
            }
            notes += "Generated chromia.yml (scaffold pins + deployments.testnet block) since none was provided. " +
                "compile.rellVersion $rellVersionUsed ($rellVersionSource)."
            // A chr older than 0.30.0 writes the pre-0.30 deployment layout and
            // bundles a Rell two minor versions behind the production pin (the
            // box that ran the 2026-09-04 DX audit had 0.29.10). Say so once,
            // instead of letting the deploy fail on a template the old compiler
            // cannot build.
            outdatedChrNote(probed?.cli)?.let { notes += it }
            generated
        }
        // The chain name keys the yml's `blockchains` block and is what
        // `chr --blockchain` looks up: with a provided yml that lacks it the
        // preflight's first blocker is whatever else is missing, and the real
        // mistake is never named (DX audit 2026-09-04, T11).
        declaredChainNames(yml).takeIf { it.isNotEmpty() && blockchain !in it }?.let { declared ->
            return toolErrorResult(
                "blockchain \"$blockchain\" is not declared in the provided chromiaYml (blockchains: " +
                    "${declared.joinToString(", ")}) - `chr deployment $mode --blockchain $blockchain` has nothing to " +
                    "deploy. Pass blockchain=\"${declared.first()}\" or add the chain to the yml."
            )
        }
        if (containerArg != null && yml.contains("<containerIID>")) {
            yml = yml.replace("<containerIID>", containerArg)
        }
        val containerInYml = Regex("(?m)^\\s*container:\\s*[\"']?([A-Za-z0-9_.-]+)[\"']?\\s*$")
            .find(yml)?.groupValues?.get(1)
        val container = containerArg ?: containerInYml
        if (container == null || container == "<containerIID>") {
            return toolErrorResult(
                "No container lease is configured: pass `container` (from provision_testnet_container) or a " +
                    "chromiaYml whose deployments.testnet.container is a real lease name."
            )
        }
        if (containerArg != null && containerInYml != null && containerArg != containerInYml) {
            return toolErrorResult(
                "container \"$containerArg\" conflicts with deployments.testnet.container " +
                    "\"$containerInYml\" in the provided chromiaYml - make them agree."
            )
        }

        // ---- gate 1: security (block CRITICAL/HIGH even on testnet) ---------
        val security = RellSecurityCheck.analyze(files)
        val blockingFindings = security.findings.filter { it.severity in setOf("CRITICAL", "HIGH") }
        if (blockingFindings.isNotEmpty()) {
            return toolSuccessResult(buildJsonObject {
                put("status", "refused")
                put("blockchain", blockchain)
                put("container", container)
                put("securityFindings", JsonArray(blockingFindings.map { f ->
                    buildJsonObject {
                        put("severity", f.severity)
                        put("rule", f.rule)
                        put("file", f.file)
                        put("line", f.line)
                        put("text", f.text)
                        put("fix", f.fix)
                    }
                }))
                put("notes", "Refusing to deploy: the security gate found " +
                    "${blockingFindings.size} CRITICAL/HIGH finding(s). An insecure dapp must not reach a " +
                    "chain - fix the findings (each carries a `fix`) and re-run.")
            })
        }

        // ---- gate 2: compile + config preflight ------------------------------
        // Same bounded-probe contract as DeploymentPreflightStrategy: one
        // deadline shared across ALL probed URLs, so a stalling node pool
        // (the ProbeBudget crawl) yields an honest refusal instead of an
        // unbounded hang - the raw repository call here bypassed cc76b9e.
        val probeDeadlineMs = ProbeBudget.configuredDeadlineMs(
            ProbeBudget.PREFLIGHT_DEADLINE_ENV, env[ProbeBudget.PREFLIGHT_DEADLINE_ENV]
        )
        var probeStartNanos = -1L
        val preflight = DeploymentPreflight.run(yml, "testnet", files, null) { network, bridHex ->
            if (probeStartNanos < 0) probeStartNanos = System.nanoTime()
            val remainingMs = probeDeadlineMs - (System.nanoTime() - probeStartNanos) / 1_000_000
            ProbeBudget.withBudget(remainingMs) {
                repository.getBlockchainHeight(network, BlockchainRid.buildFromHex(bridHex))
            } ?: NetworkResult.Error(ProbeBudget.preflightProbeTimeoutMessage(probeDeadlineMs))
        }
        if (!preflight.ready) {
            return toolSuccessResult(buildJsonObject {
                put("status", "refused")
                put("blockchain", blockchain)
                put("container", container)
                put("preflight", preflight.toJson())
                put("notes", "Refusing to deploy: deployment preflight is not ready - fix the blockers " +
                    "listed under preflight.blockers and re-run.")
            })
        }

        // ---- deploy key ------------------------------------------------------
        val storedPub = keyStore.pubKeyForContainer(container)
        val storedPriv = storedPub?.let { keyStore.privKeyFor(it) }
        val envPriv = TestnetProvisioning.parsePrivKey(env[TestnetProvisioning.DEPLOY_KEY_ENV])
        if (envPriv == null) env[TestnetProvisioning.DEPLOY_KEY_ENV]?.let { secrets.add(it) }
        val privKey = storedPriv ?: envPriv
        val pubHex = when {
            storedPriv != null -> storedPub
            envPriv != null -> TestnetProvisioning.derivePubKey(envPriv).toHex()
            else -> null
        }
        privKey?.let { secrets.add(it.toHex()) }

        val command = chrCommand.command + listOf(
            "deployment", mode,
            "--settings", "chromia.yml", "--network", "testnet", "--blockchain", blockchain,
            // Headless: chr prompts "Please specify -y option to force deployment" without it
            // (live run 2026-09-02, chr 0.29.10).
            "-y"
        )

        if (privKey == null && !dryRun) {
            return toolSuccessResult(buildJsonObject {
                put("status", "blocked_human_step")
                put("blockchain", blockchain)
                put("container", container)
                put("command", command.joinToString(" "))
                put("humanStep", "No deploy key is held for container \"$container\": the server keystore " +
                    "has no entry (provision_testnet_container stores one when it leases the container) and " +
                    "env ${TestnetProvisioning.DEPLOY_KEY_ENV} is not set. If this container was leased " +
                    "elsewhere (e.g. via the Vault), the server operator must set " +
                    "${TestnetProvisioning.DEPLOY_KEY_ENV} to that lease's deployer private key - the key " +
                    "never appears in tool output.")
                put("notes", notes.joinToString(" "))
            })
        }

        if (dryRun) {
            return toolSuccessResult(buildJsonObject {
                put("status", "dry_run")
                put("blockchain", blockchain)
                put("container", container)
                put("gates", buildJsonObject {
                    put("security", "passed (${security.findings.size} non-blocking finding(s))")
                    put("preflight", "ready")
                })
                pubHex?.let { put("deployPubkey", it) }
                put("command", command.joinToString(" "))
                put("chrResolution", chrCommand.source)
                rellVersionUsed?.let { put("rellVersion", it) }
                rellVersionSource?.let { put("rellVersionSource", it) }
                put("notes", (notes + ("Dry run - all gates passed; nothing was deployed. Re-run with " +
                    "dryRun=false to execute the command above headlessly, signed by the server-held " +
                    "deploy key.")).joinToString(" "))
            })
        }

        // ---- chr availability -----------------------------------------------
        val chrCheck = probeChrVersions()
        if (chrCheck == null) {
            return toolSuccessResult(buildJsonObject {
                put("status", "blocked_human_step")
                put("blockchain", blockchain)
                put("container", container)
                put("command", command.joinToString(" "))
                put("chrResolution", chrCommand.source)
                put("humanStep", "The Chromia CLI (chr) is not available on this server, and deployment " +
                    "cannot be performed without it (resolution attempted: ${chrCommand.source}). " +
                    "Server operator: install chr on the MCP server host " +
                    "(https://docs.chromia.com/intro/installation) or set " +
                    "${TestnetProvisioning.CHR_BIN_ENV} to its path, then re-run - no other human step is " +
                    "needed; signing uses the server-held key automatically.")
                put("notes", notes.joinToString(" "))
            })
        }

        // ---- execute headlessly ---------------------------------------------
        val workDir = tempDirFactory()
        try {
            java.nio.file.Files.writeString(workDir.resolve("chromia.yml"), yml)
            for ((path, content) in files) {
                val normalized = if (path.startsWith("src/") || path.startsWith("src\\")) path else "src/" + path
                val target = workDir.resolve(normalized).normalize()
                if (!target.startsWith(workDir)) {
                    return toolErrorResult("rell path \"" + path + "\" escapes the project directory")
                }
                java.nio.file.Files.createDirectories(target.parent)
                java.nio.file.Files.writeString(target, content)
            }

            val pubForEnv = pubHex ?: TestnetProvisioning.derivePubKey(privKey!!).toHex()
            val result = processRunner.run(
                command, workDir,
                mapOf(
                    "POSTCHAIN_CLIENT_PRIVKEY" to privKey!!.toHex(),
                    "POSTCHAIN_CLIENT_PUBKEY" to pubForEnv
                ),
                CHR_TIMEOUT_MS
            )
            val combinedOut = TestnetProvisioning.sanitizeText(
                (result.stdout + "\n" + result.stderr).trim(), secrets
            )
            if (result.exitCode != 0) {
                return toolErrorResult(
                    "chr deployment $mode failed (exit ${result.exitCode}): ${combinedOut.takeLast(1500)}"
                )
            }

            // brid: CLI >= 0.30 writes deployments.testnet.chains.<name>: x"<rid>"
            val updatedYml = runCatching {
                java.nio.file.Files.readString(workDir.resolve("chromia.yml"))
            }.getOrDefault(yml)
            // Never let key-shaped material masquerade as a BRID: candidates
            // matching any held secret are dropped before anything reaches the
            // output (the sweep test covers a chr that echoes its env).
            val secretsUpper = secrets.map { it.uppercase() }.toSet()
            val brid = Regex(Regex.escape(blockchain) + ":\\s*x?[\"']?([0-9A-Fa-f]{64})[\"']?")
                .find(updatedYml)?.groupValues?.get(1)?.uppercase()
                ?.takeIf { it !in secretsUpper }
                ?: Regex("[0-9A-Fa-f]{64}").findAll(result.stdout)
                    .map { it.value.uppercase() }
                    .firstOrNull { it != WriteDeploymentConfig.TESTNET_DIRECTORY_BRID && it !in secretsUpper }

            // ---- verify ------------------------------------------------------
            var live = false
            var height: Long? = null
            var verifyNote = ""
            if (brid != null) {
                // Bounded like every other height read (cc76b9e): a cluster
                // that has not started the fresh chain yet stalls exactly like
                // an unreachable one, and the deploy already succeeded - the
                // answer must not hang on the verify step.
                val probe = ProbeBudget.withBudget(probeDeadlineMs) {
                    repository.getBlockchainHeight("testnet", BlockchainRid.buildFromHex(brid))
                } ?: NetworkResult.Error(
                    "height probe timed out after ${probeDeadlineMs}ms"
                )
                when (probe) {
                    is NetworkResult.Success -> { live = true; height = probe.data }
                    is NetworkResult.Error -> verifyNote =
                        "Height probe did not answer yet (${probe.message}) - a freshly created chain can " +
                            "take minutes to start on the cluster; verify later with verify_deployment."
                }
            } else {
                verifyNote = "Could not extract the new chain BRID from chr output - run verify_deployment " +
                    "once you have it (CLI 0.30.0+ writes it into deployments.testnet.chains in chromia.yml; " +
                    "0.29.x only prints it on stdout)."
            }

            return toolSuccessResult(buildJsonObject {
                put("status", if (live) "deployed" else "deployed_unverified")
                put("blockchain", blockchain)
                put("container", container)
                brid?.let { put("brid", it) }
                put("live", live)
                height?.let { put("blockHeight", it) }
                put("chrResolution", chrCommand.source)
                rellVersionUsed?.let { put("rellVersion", it) }
                rellVersionSource?.let { put("rellVersionSource", it) }
                put("updatedChromiaYml", TestnetProvisioning.sanitizeText(updatedYml, secrets))
                put("notes", (notes + listOfNotNull(
                    "chr deployment $mode succeeded.",
                    verifyNote.ifBlank { null },
                    if (live) "The chain is known and answering on testnet." else null
                )).joinToString(" "))
            })
        } finally {
            runCatching { workDir.toFile().deleteRecursively() }
        }
    }

    /**
     * Resolved once per strategy instance: honors CHROMIA_CHR_BIN, then a
     * Windows PATH+PATHEXT search (scoop installs a chr.cmd shim that
     * ProcessBuilder cannot launch bare - the live 2026-09-02 run hit this).
     * The source string is surfaced in tool output for diagnosability.
     */
    private val chrCommand: ChrCommand by lazy { ChrLocator.resolve(env) }

    private val probeLock = Any()
    private var probeAttempted = false
    private var cachedVersions: ChrVersions? = null

    /**
     * `chr --version`, run at most once per strategy instance (the installed
     * CLI does not change mid-process). Returns null when chr is unavailable.
     */
    private fun probeChrVersions(): ChrVersions? = synchronized(probeLock) {
        if (!probeAttempted) {
            probeAttempted = true
            cachedVersions = runCatching {
                val out = processRunner.run(
                    chrCommand.command + "--version", java.nio.file.Path.of("."), emptyMap(), 30_000
                )
                if (out.exitCode == 0) ChrVersions.parse(out.stdout + "\n" + out.stderr) else null
            }.getOrNull()
        }
        cachedVersions
    }

    companion object {
        const val CHR_TIMEOUT_MS = 300_000L
    }
}
