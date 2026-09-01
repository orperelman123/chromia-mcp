package org.chromia

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.postchain.common.BlockchainRid
import org.chromia.domain.AssetFilters
import org.chromia.domain.AssetSearchFilters
import org.chromia.domain.BlockchainFilters
import org.chromia.domain.ChromiaRepository
import org.chromia.domain.JsonResult
import org.chromia.domain.NetworkResult
import org.chromia.domain.TransactionFilters

class RecordingRepository : ChromiaRepository {
    var next: JsonResult = NetworkResult.Success(buildJsonObject { put("ok", true) })
    var lastCall: String? = null
    var lastNetwork: String? = null
    var lastBlockchainFilters: BlockchainFilters? = null
    var lastAssetSearchFilters: AssetSearchFilters? = null
    var lastTransactionFilters: TransactionFilters? = null
    var lastAssetId: String? = null
    var lastAssetFilters: AssetFilters? = null
    var lastLimit: Int? = null
    var lastDetailsRid: String? = null
    var lastBrid: String? = null
    var lastFromTimestamp: String? = null
    var lastUntilTimestamp: String? = null
    var lastSigner: String? = null
    var lastAccountId: String? = null
    var lastPubkey: String? = null
    var lastStartTimestamp: String? = null
    var lastIncludeTotals: Boolean? = null
    var lastIncludeGroupedDeposits: Boolean? = null
    var lastIncludeGroupedWithdrawals: Boolean? = null
    var lastDapp: DappCall? = null

    data class DappCall(
        val network: String?,
        val brid: String,
        val query: String?,
        val arguments: Map<String, Any?>
    )

    override suspend fun filterBlockchains(network: String?, filters: BlockchainFilters): JsonResult {
        lastCall = "filterBlockchains"
        lastNetwork = network
        lastBlockchainFilters = filters
        return next
    }

    override suspend fun filterAssets(network: String?, filters: AssetSearchFilters): JsonResult {
        lastCall = "filterAssets"
        lastNetwork = network
        lastAssetSearchFilters = filters
        return next
    }

    override suspend fun getNetworkStats(network: String?): JsonResult {
        lastCall = "getNetworkStats"
        lastNetwork = network
        return next
    }

    override suspend fun executeCustomQuery(
        network: String?,
        blockchainRid: BlockchainRid,
        queryName: String?,
        arguments: Map<String, Any?>
    ): JsonResult {
        lastCall = "executeCustomQuery"
        lastDapp = DappCall(network, blockchainRid.toHex(), queryName, arguments)
        return next
    }

    override suspend fun getAllTransactions(network: String?, filters: TransactionFilters): JsonResult {
        lastCall = "getAllTransactions"
        lastNetwork = network
        lastTransactionFilters = filters
        return next
    }

    override suspend fun getAssetTopHolders(
        assetId: String,
        network: String?,
        limit: Int?,
        filters: AssetFilters
    ): JsonResult {
        lastCall = "getAssetTopHolders"
        lastNetwork = network
        lastAssetId = assetId
        lastLimit = limit
        lastAssetFilters = filters
        return next
    }

    override suspend fun getBlockchainDetails(rid: String, network: String?): JsonResult {
        lastCall = "getBlockchainDetails"
        lastNetwork = network
        lastDetailsRid = rid
        return next
    }

    override suspend fun getBlockchainsTransactions(network: String?): JsonResult {
        lastCall = "getBlockchainsTransactions"
        lastNetwork = network
        return next
    }

    override suspend fun getTransactionsByCluster(network: String?): JsonResult {
        lastCall = "getTransactionsByCluster"
        lastNetwork = network
        return next
    }

    override suspend fun getAllAssets(network: String?): JsonResult {
        lastCall = "getAllAssets"
        lastNetwork = network
        return next
    }

    override suspend fun getTotalRewardsPaid(network: String?): JsonResult {
        lastCall = "getTotalRewardsPaid"
        lastNetwork = network
        return next
    }

    override suspend fun getAssetDistribution(
        assetId: String,
        network: String?,
        filters: AssetFilters
    ): JsonResult {
        lastCall = "getAssetDistribution"
        lastNetwork = network
        lastAssetId = assetId
        lastAssetFilters = filters
        return next
    }

    override suspend fun getAssetBlockchains(network: String?, assetId: String): JsonResult {
        lastCall = "getAssetBlockchains"
        lastNetwork = network
        lastAssetId = assetId
        return next
    }

    override suspend fun getBlockchainAnalytics(
        brid: String,
        network: String?,
        fromTimestamp: String?
    ): JsonResult {
        lastCall = "getBlockchainAnalytics"
        lastNetwork = network
        lastBrid = brid
        lastFromTimestamp = fromTimestamp
        return next
    }

    override suspend fun getMonthlyActiveAccountsPerChain(
        brid: String,
        network: String?,
        untilTimestamp: String?
    ): JsonResult {
        lastCall = "getMonthlyActiveAccountsPerChain"
        lastNetwork = network
        lastBrid = brid
        lastUntilTimestamp = untilTimestamp
        return next
    }

    override suspend fun getAllOperations(network: String?): JsonResult {
        lastCall = "getAllOperations"
        lastNetwork = network
        return next
    }

    override suspend fun getSignerBlockchains(network: String?, signer: String): JsonResult {
        lastCall = "getSignerBlockchains"
        lastNetwork = network
        lastSigner = signer
        return next
    }

    override suspend fun getAccountBlockchains(accountId: String, network: String?): JsonResult {
        lastCall = "getAccountBlockchains"
        lastAccountId = accountId
        lastNetwork = network
        return next
    }

    override suspend fun getNodeUnavailability(
        pubkey: String,
        startTimestamp: String,
        network: String?
    ): JsonResult {
        lastCall = "getNodeUnavailability"
        lastPubkey = pubkey
        lastStartTimestamp = startTimestamp
        lastNetwork = network
        return next
    }

    override suspend fun getChrAggregates(
        network: String?,
        includeTotals: Boolean,
        includeGroupedDeposits: Boolean,
        includeGroupedWithdrawals: Boolean
    ): JsonResult {
        lastCall = "getChrAggregates"
        lastNetwork = network
        lastIncludeTotals = includeTotals
        lastIncludeGroupedDeposits = includeGroupedDeposits
        lastIncludeGroupedWithdrawals = includeGroupedWithdrawals
        return next
    }
}
