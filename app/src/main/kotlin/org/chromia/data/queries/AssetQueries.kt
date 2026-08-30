package org.chromia.data.queries

import org.chromia.domain.*

object AssetQueries {

    fun getAssetDistribution(assetId: String, filters: AssetFilters): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query getAssetDistribution($assetId: String!, $brids: [String], $accountTypes: [String], $excludeAccounts: [String], $excludeBrids: [String], $excludeAccountTypes: [String]) {
                getAssetDistribution(assetId: $assetId, brids: $brids, accountTypes: $accountTypes, excludeAccounts: $excludeAccounts, excludeBrids: $excludeBrids, excludeAccountTypes: $excludeAccountTypes) {
                    brid, type, totalAmount
                }
            }
            """.trimIndent()
        )
        variable("assetId", assetId)
        filters.brids?.let { variable("brids", it) }
        filters.accountTypes?.let { variable("accountTypes", it) }
        filters.excludeAccounts?.let { variable("excludeAccounts", it) }
        filters.excludeBrids?.let { variable("excludeBrids", it) }
        filters.excludeAccountTypes?.let { variable("excludeAccountTypes", it) }
    }

    fun getAssetTopHolders(assetId: String, limit: Int?, filters: AssetFilters): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query getAssetTopHolders($assetId: String!, $limit: Int, $brids: [String], $accountTypes: [String], $excludeAccounts: [String], $excludeBrids: [String], $excludeAccountTypes: [String]) {
                getAssetTopHolders(assetId: $assetId, limit: $limit, brids: $brids, accountTypes: $accountTypes, excludeAccounts: $excludeAccounts, excludeBrids: $excludeBrids, excludeAccountTypes: $excludeAccountTypes) {
                    accountId, totalBalance, chainCount, chainBrid, accountType
                }
            }
            """.trimIndent()
        )
        variable("assetId", assetId)
        limit?.let { variable("limit", it) }
        filters.brids?.let { variable("brids", it) }
        filters.accountTypes?.let { variable("accountTypes", it) }
        filters.excludeAccounts?.let { variable("excludeAccounts", it) }
        filters.excludeBrids?.let { variable("excludeBrids", it) }
        filters.excludeAccountTypes?.let { variable("excludeAccountTypes", it) }
    }

    fun getAssetBlockchains(assetId: String): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query getAssetBlockchains($assetId: String!) {
                getAssetBlockchains(assetId: $assetId) {
                    brid, transfersCount, isSource, blockchain { name }
                }
            }
            """.trimIndent()
        )
        variable("assetId", assetId)
    }

    fun filterAssets(filters: AssetSearchFilters): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query filterAssets($brid: String, $searchQuery: String, $type: String, $limit: Int, $offset: Int, $sortBy: String, $sortDirection: SortDirection) {
                filterAssets(brid: $brid, searchQuery: $searchQuery, type: $type, limit: $limit, offset: $offset, sortBy: $sortBy, sortDirection: $sortDirection) {
                    assets { name, iconUrl, symbol, id, brid, type, decimals, supply, transferCount, blockchainCount }, 
                    totalCount
                }
            }
            """.trimIndent()
        )
        filters.brid?.let { variable("brid", it) }
        filters.searchQuery?.let { variable("searchQuery", it) }
        filters.type?.let { variable("type", it) }
        filters.pagination.limit?.let { variable("limit", it) }
        filters.pagination.offset?.let { variable("offset", it) }
        filters.sorting.sortBy?.let { variable("sortBy", it) }
        filters.sorting.sortDirection?.let { variable("sortDirection", it) }
    }
}
