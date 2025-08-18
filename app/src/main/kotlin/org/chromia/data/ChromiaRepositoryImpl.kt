package org.chromia.data

import net.postchain.common.BlockchainRid
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig
import org.chromia.data.queries.*
import org.chromia.domain.*

class ChromiaRepositoryImpl(
    private val config: ChromiaConfig = ChromiaConfig(),
    private val httpClientService: HttpClientService = HttpClientService(config),
    private val postchainClientService: PostchainClientService = PostchainClientService(config)
) : ChromiaRepository {

    override suspend fun executeCustomQuery(network: String?, blockchainRid: BlockchainRid, queryName: String?, arguments: Map<String, Any>): JsonResult {
        return postchainClientService.executeBlockchainQuery(network, blockchainRid, queryName, arguments)
    }

    override suspend fun getBlockchainsTransactions(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getBlockchainsTransactions(), network)

    override suspend fun getNetworkAccountCount(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getNetworkAccountCount(), network)

    override suspend fun getNetworkTransferCount(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getNetworkTransferCount(), network)

    override suspend fun getMonthlyActiveAccounts(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getMonthlyActiveAccounts(), network)

    override suspend fun getTransactionsByCluster(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getTransactionsByCluster(), network)

    override suspend fun getAllAssets(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getAllAssets(), network)

    override suspend fun getTotalRewardsPaid(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getTotalRewardsPaid(), network)

    override suspend fun getNetworkStats(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getNetworkStats(), network)

    override suspend fun getAllOperations(network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(NetworkQueries.getAllOperations(), network)

    override suspend fun getAssetDistribution(
        assetId: String,
        network: String?,
        filters: AssetFilters
    ): JsonResult = httpClientService.executeGraphQLQuery(
        AssetQueries.getAssetDistribution(assetId, filters),
        network
    )

    override suspend fun getAssetTopHolders(
        assetId: String,
        network: String?,
        limit: Int?,
        filters: AssetFilters
    ): JsonResult = httpClientService.executeGraphQLQuery(
        AssetQueries.getAssetTopHolders(assetId, limit, filters),
        network
    )

    override suspend fun getAssetBlockchains(network: String?, assetId: String): JsonResult =
        httpClientService.executeGraphQLQuery(AssetQueries.getAssetBlockchains(assetId), network)

    override suspend fun filterAssets(network: String?, filters: AssetSearchFilters): JsonResult =
        httpClientService.executeGraphQLQuery(AssetQueries.filterAssets(filters), network)

    override suspend fun getBlockchainAnalytics(
        brid: String,
        network: String?,
        fromTimestamp: String?
    ): JsonResult = httpClientService.executeGraphQLQuery(
        BlockchainQueries.getBlockchainAnalytics(brid, fromTimestamp),
        network
    )

    override suspend fun getBlockchainDetails(rid: String, network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(BlockchainQueries.getBlockchainDetails(rid), network)

    override suspend fun getMonthlyActiveAccountsPerChain(
        brid: String,
        network: String?,
        untilTimestamp: String?
    ): JsonResult = httpClientService.executeGraphQLQuery(
        BlockchainQueries.getMonthlyActiveAccountsPerChain(brid, untilTimestamp),
        network
    )

    override suspend fun filterBlockchains(network: String?, filters: BlockchainFilters): JsonResult =
        httpClientService.executeGraphQLQuery(BlockchainQueries.filterBlockchains(filters), network)

    override suspend fun getAllTransactions(network: String?, filters: TransactionFilters): JsonResult =
        httpClientService.executeGraphQLQuery(TransactionQueries.getAllTransactions(filters), network)

    override suspend fun getSignerBlockchains(network: String?, signer: String): JsonResult =
        httpClientService.executeGraphQLQuery(TransactionQueries.getSignerBlockchains(signer), network)

    override suspend fun getAccountBlockchains(accountId: String, network: String?): JsonResult =
        httpClientService.executeGraphQLQuery(TransactionQueries.getAccountBlockchains(accountId), network)

    override suspend fun getNodeUnavailability(
        pubkey: String,
        startTimestamp: String,
        network: String?
    ): JsonResult = httpClientService.executeGraphQLQuery(
        TransactionQueries.getNodeUnavailability(pubkey, startTimestamp),
        network
    )

    override suspend fun getChrAggregates(
        network: String?,
        includeTotals: Boolean,
        includeGroupedDeposits: Boolean,
        includeGroupedWithdrawals: Boolean
    ): JsonResult = httpClientService.executeGraphQLQuery(
        TransactionQueries.getChrAggregates(includeTotals, includeGroupedDeposits, includeGroupedWithdrawals),
        network
    )
}
