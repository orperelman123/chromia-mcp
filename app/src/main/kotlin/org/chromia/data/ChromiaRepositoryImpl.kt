package org.chromia.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.chromia.domain.*
import kotlin.time.Duration.Companion.seconds

class ChromiaRepositoryImpl : ChromiaRepository {

    val explorerUrl: String = "https://explorer.chromia.com/api/explorer-service"
    val defaultNetwork: String = "mainnet"

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30.seconds.inWholeMilliseconds
            connectTimeoutMillis = 10.seconds.inWholeMilliseconds
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }

    private suspend fun executeQuery(
        query: GraphQLQuery,
        network: String?
    ): JsonResult = withContext(Dispatchers.IO) {
        runCatching {
            httpClient.post(explorerUrl) {
                parameter("network", network ?: defaultNetwork)
                setBody(query.toString())
            }
        }.fold(
            onSuccess = { response ->
                when {
                    !response.status.isSuccess() -> NetworkResult.Error(
                        "HTTP ${response.status.value}: ${response.status.description}"
                    )
                    else -> parseResponse(response.body())
                }
            },
            onFailure = { e -> NetworkResult.Error("Request failed: ${e.message}", e) }
        )
    }

    private fun parseResponse(responseText: String): JsonResult {
        return runCatching {
            Json.parseToJsonElement(responseText)
        }.fold(
            onSuccess = { jsonElement ->
                when (jsonElement) {
                    is JsonObject -> parseJsonObject(jsonElement)
                    else -> NetworkResult.Error("Invalid response format")
                }
            },
            onFailure = { e -> NetworkResult.Error("JSON parsing failed: ${e.message}", e) }
        )
    }

    private fun parseJsonObject(jsonObject: JsonObject): JsonResult {
        return jsonObject["errors"]?.let { errors ->
            val errorMessage = errors.jsonArray.firstOrNull()
                ?.jsonObject?.get("message")
                ?.jsonPrimitive?.content
                ?: "Unknown GraphQL error"
            NetworkResult.Error("GraphQL Error: $errorMessage")
        } ?: NetworkResult.Success(jsonObject)
    }

    override suspend fun getBlockchainsTransactions(network: String?): JsonResult {
        val query = graphqlQuery {
            query("query { groupedTransactionsByBlockchain { brid, blockchain { name, system, cluster, state }, blockHeight, throughput, count } }")
        }
        return executeQuery(query, network)
    }

    override suspend fun getNetworkAccountCount(network: String?): JsonResult {
        val query = graphqlQuery {
            query("query { countAllAccounts }")
        }
        return executeQuery(query, network)
    }

    override suspend fun getNetworkTransferCount(network: String?): JsonResult {
        val query = graphqlQuery {
            query("query { countAllTransfers }")
        }
        return executeQuery(query, network)
    }

    override suspend fun getMonthlyActiveAccounts(network: String?): JsonResult {
        val query = graphqlQuery {
            query("query { monthlyActiveAccounts }")
        }
        return executeQuery(query, network)
    }

    override suspend fun getTransactionsByCluster(network: String?): JsonResult {
        val query = graphqlQuery {
            query("query { groupedTransactionsByCluster { cluster, count } }")
        }
        return executeQuery(query, network)
    }

    override suspend fun getAllAssets(network: String?): JsonResult {
        val query = graphqlQuery {
            query("query { allAssets { name, iconUrl, symbol, id, brid, type, decimals, supply, transferCount, blockchainCount } }")
        }
        return executeQuery(query, network)
    }

    override suspend fun getTotalRewardsPaid(network: String?): JsonResult {
        val query = graphqlQuery {
            query("query { totalRewardsPaid }")
        }
        return executeQuery(query, network)
    }

    override suspend fun getNetworkStats(network: String?): JsonResult {
        val query = graphqlQuery {
            query("""
                query { 
                    dashboardData { 
                        countAllAccounts, 
                        countAllTransfers, 
                        countAllTransactions, 
                        monthlyActiveAccounts, 
                        topDappBlockchains { 
                            brid, blockchain { name, system, cluster, state }, 
                            blockHeight, throughput, count 
                        }, 
                        topAssets { 
                            id, brid, name, iconUrl, symbol, transferCount, blockchainCount 
                        }, 
                        groupedTransactionsByCluster { cluster, count } 
                    } 
                }
            """.trimIndent())
        }
        return executeQuery(query, network)
    }

    override suspend fun getAssetDistribution(
        assetId: String,
        network: String?,
        filters: AssetFilters
    ): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query getAssetDistribution($assetId: String!, $brids: [String], $accountTypes: [String], $excludeAccounts: [String], $excludeBrids: [String], $excludeAccountTypes: [String]) {
                    getAssetDistribution(assetId: $assetId, brids: $brids, accountTypes: $accountTypes, excludeAccounts: $excludeAccounts, excludeBrids: $excludeBrids, excludeAccountTypes: $excludeAccountTypes) {
                        brid, type, totalAmount
                    }
                }
            """.trimIndent())
            variable("assetId", assetId)
            filters.brids?.let { variable("brids", it) }
            filters.accountTypes?.let { variable("accountTypes", it) }
            filters.excludeAccounts?.let { variable("excludeAccounts", it) }
            filters.excludeBrids?.let { variable("excludeBrids", it) }
            filters.excludeAccountTypes?.let { variable("excludeAccountTypes", it) }
        }
        return executeQuery(query, network)
    }

    override suspend fun getAssetTopHolders(
        assetId: String,
        network: String?,
        limit: Int?,
        filters: AssetFilters
    ): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query getAssetTopHolders($assetId: String!, $limit: Int, $brids: [String], $accountTypes: [String], $excludeAccounts: [String], $excludeBrids: [String], $excludeAccountTypes: [String]) {
                    getAssetTopHolders(assetId: $assetId, limit: $limit, brids: $brids, accountTypes: $accountTypes, excludeAccounts: $excludeAccounts, excludeBrids: $excludeBrids, excludeAccountTypes: $excludeAccountTypes) {
                        accountId, totalBalance, chainCount, chainBrid, accountType
                    }
                }
            """.trimIndent())
            variable("assetId", assetId)
            limit?.let { variable("limit", it) }
            filters.brids?.let { variable("brids", it) }
            filters.accountTypes?.let { variable("accountTypes", it) }
            filters.excludeAccounts?.let { variable("excludeAccounts", it) }
            filters.excludeBrids?.let { variable("excludeBrids", it) }
            filters.excludeAccountTypes?.let { variable("excludeAccountTypes", it) }
        }
        return executeQuery(query, network)
    }

    override suspend fun getAssetBlockchains(network: String?, assetId: String): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query getAssetBlockchains($assetId: String!) {
                    getAssetBlockchains(assetId: $assetId) {
                        brid, transfersCount, isSource, blockchain { name }
                    }
                }
            """.trimIndent())
            variable("assetId", assetId)
        }
        return executeQuery(query, network)
    }

    override suspend fun filterAssets(network: String?, filters: AssetSearchFilters): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query filterAssets($brid: String, $searchQuery: String, $type: String, $limit: Int, $offset: Int, $sortBy: String, $sortDirection: SortDirection) {
                    filterAssets(brid: $brid, searchQuery: $searchQuery, type: $type, limit: $limit, offset: $offset, sortBy: $sortBy, sortDirection: $sortDirection) {
                        assets { name, iconUrl, symbol, id, brid, type, decimals, supply, transferCount, blockchainCount }, 
                        totalCount
                    }
                }
            """.trimIndent())
            filters.brid?.let { variable("brid", it) }
            filters.searchQuery?.let { variable("searchQuery", it) }
            filters.type?.let { variable("type", it) }
            filters.pagination.limit?.let { variable("limit", it) }
            filters.pagination.offset?.let { variable("offset", it) }
        }
        return executeQuery(query, network)
    }

    override suspend fun getBlockchainAnalytics(
        brid: String,
        network: String?,
        fromTimestamp: String?
    ): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query BlockchainAnalytics($brid: String!, $fromTimestamp: String) {
                    blockchainAnalytics(brid: $brid, fromTimestamp: $fromTimestamp) {
                        totalTransactions, totalOperations, totalActiveAccounts,
                        transactionsByDay { date, value },
                        operationsByDay { date, value },
                        accountsByDay { date, value }
                    }
                }
            """.trimIndent())
            variable("brid", brid)
            fromTimestamp?.let { variable("fromTimestamp", it) }
        }
        return executeQuery(query, network)
    }

    override suspend fun getBlockchainDetails(rid: String, network: String?): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query GetBlockchainDetails($rid: ID!) {
                    blockchain(rid: $rid) { rid name system container cluster state }
                }
            """.trimIndent())
            variable("rid", rid)
        }
        return executeQuery(query, network)
    }

    override suspend fun getMonthlyActiveAccountsPerChain(
        brid: String,
        network: String?,
        untilTimestamp: String?
    ): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query monthlyActiveAccountsPerChain($brid: String!, $untilTimestamp: String) {
                    monthlyActiveAccountsPerChain(brid: $brid, untilTimestamp: $untilTimestamp)
                }
            """.trimIndent())
            variable("brid", brid)
            untilTimestamp?.let { variable("untilTimestamp", it) }
        }
        return executeQuery(query, network)
    }

    override suspend fun getAllTransactions(network: String?, filters: TransactionFilters): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query allTransactions($rid: ID, $blockId: ID, $blockchainIds: [ID], $notInBlockchains: [ID], $timestampFrom: String, $timestampTo: String, $operations: [String], $notInOperations: [String], $signers: [ID], $excludedSigners: [ID], $accounts: [ID], $excludedAccounts: [ID], $assets: [ID], $limit: Int, $offset: Int, $sortBy: String, $sortDirection: SortDirection) {
                    allTransactions(rid: $rid, blockId: $blockId, blockchainIds: $blockchainIds, notInBlockchains: $notInBlockchains, timestampFrom: $timestampFrom, timestampTo: $timestampTo, operations: $operations, notInOperations: $notInOperations, signers: $signers, excludedSigners: $excludedSigners, accounts: $accounts, excludedAccounts: $excludedAccounts, assets: $assets, limit: $limit, offset: $offset, sortBy: $sortBy, sortDirection: $sortDirection) {
                        transactions {
                            rid, block { height }, blockchain { name, rid }, timestamp,
                            operations { operation }, accounts { accountId, opIndex },
                            signers { signer, opIndex, type },
                            transfers { id, blockchain { rid }, accountId, asset { id, name, iconUrl, decimals }, delta, isCrosschain, isInput }
                        }
                    }
                }
            """.trimIndent())
            filters.rid?.let { variable("rid", it) }
            filters.blockId?.let { variable("blockId", it) }
            filters.blockchainIds?.let { variable("blockchainIds", it) }
            filters.notInBlockchains?.let { variable("notInBlockchains", it) }
            filters.timestampFrom?.let { variable("timestampFrom", it) }
            filters.timestampTo?.let { variable("timestampTo", it) }
            filters.operations?.let { variable("operations", it) }
            filters.notInOperations?.let { variable("notInOperations", it) }
            filters.signers?.let { variable("signers", it) }
            filters.excludedSigners?.let { variable("excludedSigners", it) }
            filters.accounts?.let { variable("accounts", it) }
            filters.excludedAccounts?.let { variable("excludedAccounts", it) }
            filters.assets?.let { variable("assets", it) }
            filters.pagination.limit?.let { variable("limit", it) }
            filters.pagination.offset?.let { variable("offset", it) }
            filters.sorting.sortBy?.let { variable("sortBy", it) }
            filters.sorting.sortDirection?.let { variable("sortDirection", it) }
        }
        return executeQuery(query, network)
    }

    override suspend fun getAllOperations(network: String?): JsonResult {
        val query = graphqlQuery {
            query("query { operations { operation, brid } }")
        }
        return executeQuery(query, network)
    }

    override suspend fun getSignerBlockchains(network: String?, signer: String): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query signerBlockchains($signer: String!) {
                    signerBlockchains(signer: $signer) {
                        blockchain { rid, name }, transactionCount
                    }
                }
            """.trimIndent())
            variable("signer", signer)
        }
        return executeQuery(query, network)
    }

    override suspend fun getAccountBlockchains(accountId: String, network: String?): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query accountBlockchains($accountId: String!) {
                    accountBlockchains(accountId: $accountId) {
                        blockchain { rid, name }, transactionCount, transfersCount
                    }
                }
            """.trimIndent())
            variable("accountId", accountId)
        }
        return executeQuery(query, network)
    }

    override suspend fun getNodeUnavailability(
        pubkey: String,
        startTimestamp: String,
        network: String?
    ): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query getNodeUnavailability($pubkey: String!, $startTimestamp: String!) {
                    getNodeUnavailability(pubkey: $pubkey, startTimestamp: $startTimestamp) {
                        blockchainRid, intervals { start, end }
                    }
                }
            """.trimIndent())
            variable("pubkey", pubkey)
            variable("startTimestamp", startTimestamp)
        }
        return executeQuery(query, network)
    }

    override suspend fun getChrAggregates(
        network: String?,
        includeTotals: Boolean,
        includeGroupedDeposits: Boolean,
        includeGroupedWithdrawals: Boolean
    ): JsonResult {
        val query = graphqlQuery {
            query($$"""
                query GetChrAggregates($includeTotals: Boolean, $includeGroupedDeposits: Boolean, $includeGroupedWithdrawals: Boolean) {
                    chrAggregates(includeTotals: $includeTotals, includeGroupedDeposits: $includeGroupedDeposits, includeGroupedWithdrawals: $includeGroupedWithdrawals) {
                        groupedDeposits { address, networkId, total },
                        groupedWithdrawals { address, networkId, total },
                        totals { depositsTotal, withdrawalsTotal }
                    }
                }
            """.trimIndent())
            variable("includeTotals", includeTotals)
            variable("includeGroupedDeposits", includeGroupedDeposits)
            variable("includeGroupedWithdrawals", includeGroupedWithdrawals)
        }
        return executeQuery(query, network)
    }
}
