package org.chromia.domain

interface ChromiaRepository {
    
    suspend fun getBlockchainsTransactions(network: String?): JsonResult
    suspend fun getNetworkAccountCount(network: String?): JsonResult
    suspend fun getNetworkTransferCount(network: String?): JsonResult
    suspend fun getMonthlyActiveAccounts(network: String?): JsonResult
    suspend fun getTransactionsByCluster(network: String?): JsonResult
    suspend fun getAllAssets(network: String?): JsonResult
    suspend fun getTotalRewardsPaid(network: String?): JsonResult
    suspend fun getNetworkStats(network: String?): JsonResult
    
    suspend fun getAssetDistribution(
        assetId: String,
        network: String?,
        filters: AssetFilters = AssetFilters()
    ): JsonResult
    
    suspend fun getAssetTopHolders(
        assetId: String,
        network: String?,
        limit: Int? = null,
        filters: AssetFilters = AssetFilters()
    ): JsonResult
    
    suspend fun getAssetBlockchains(network: String?, assetId: String): JsonResult
    suspend fun filterAssets(network: String?, filters: AssetSearchFilters): JsonResult
    
    suspend fun getBlockchainAnalytics(
        brid: String,
        network: String?,
        fromTimestamp: String? = null
    ): JsonResult
    
    suspend fun getBlockchainDetails(rid: String, network: String?): JsonResult
    suspend fun getMonthlyActiveAccountsPerChain(
        brid: String,
        network: String?,
        untilTimestamp: String? = null
    ): JsonResult
    
    suspend fun getAllTransactions(network: String?, filters: TransactionFilters): JsonResult
    suspend fun getAllOperations(network: String?): JsonResult
    
    suspend fun getSignerBlockchains(network: String?, signer: String): JsonResult
    suspend fun getAccountBlockchains(accountId: String, network: String?): JsonResult
    
    suspend fun getNodeUnavailability(
        pubkey: String,
        startTimestamp: String,
        network: String?
    ): JsonResult
    
    suspend fun getChrAggregates(
        network: String?,
        includeTotals: Boolean = true,
        includeGroupedDeposits: Boolean = true,
        includeGroupedWithdrawals: Boolean = true
    ): JsonResult
}

data class AssetFilters(
    val brids: List<String>? = null,
    val accountTypes: List<String>? = null,
    val excludeAccounts: List<String>? = null,
    val excludeBrids: List<String>? = null,
    val excludeAccountTypes: List<String>? = null
)

data class AssetSearchFilters(
    val brid: String? = null,
    val searchQuery: String? = null,
    val type: String? = null,
    val pagination: PaginationParams = PaginationParams()
)

data class TransactionFilters(
    val rid: String? = null,
    val blockId: String? = null,
    val blockchainIds: List<String>? = null,
    val notInBlockchains: List<String>? = null,
    val timestampFrom: String? = null,
    val timestampTo: String? = null,
    val operations: List<String>? = null,
    val notInOperations: List<String>? = null,
    val signers: List<String>? = null,
    val excludedSigners: List<String>? = null,
    val accounts: List<String>? = null,
    val excludedAccounts: List<String>? = null,
    val assets: List<String>? = null,
    val pagination: PaginationParams = PaginationParams(),
    val sorting: SortingParams = SortingParams()
)

data class PaginationParams(
    val limit: Int? = null,
    val offset: Int? = null
)

data class SortingParams(
    val sortBy: String? = null,
    val sortDirection: String? = null
) 