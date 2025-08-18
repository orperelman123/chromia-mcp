package org.chromia.data.queries

import org.chromia.domain.*

object BlockchainQueries {

    fun getBlockchainAnalytics(brid: String, fromTimestamp: String?): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query BlockchainAnalytics($brid: String!, $fromTimestamp: String) {
                blockchainAnalytics(brid: $brid, fromTimestamp: $fromTimestamp) {
                    totalTransactions, totalOperations, totalActiveAccounts,
                    transactionsByDay { date, value },
                    operationsByDay { date, value },
                    accountsByDay { date, value }
                }
            }
            """.trimIndent()
        )
        variable("brid", brid)
        fromTimestamp?.let { variable("fromTimestamp", it) }
    }

    fun getBlockchainDetails(rid: String): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query GetBlockchainDetails($rid: ID!) {
                blockchain(rid: $rid) { rid name system container cluster state }
            }
            """.trimIndent()
        )
        variable("rid", rid)
    }

    fun getMonthlyActiveAccountsPerChain(brid: String, untilTimestamp: String?): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query monthlyActiveAccountsPerChain($brid: String!, $untilTimestamp: String) {
                monthlyActiveAccountsPerChain(brid: $brid, untilTimestamp: $untilTimestamp)
            }
            """.trimIndent()
        )
        variable("brid", brid)
        untilTimestamp?.let { variable("untilTimestamp", it) }
    }

    fun filterBlockchains(filters: BlockchainFilters): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query allBlockchains($rid: ID, $name: String, $cluster: String, $container: String, $state: String, $system: Boolean, $limit: Int, $offset: Int, $sortBy: String, $sortDirection: SortDirection) {
                allBlockchains(rid: $rid, name: $name, cluster: $cluster, container: $container, state: $state, system: $system, limit: $limit, offset: $offset, sortBy: $sortBy, sortDirection: $sortDirection) {
                    rid, name, cluster, container, state, system, __typename
                }
            }
            """.trimIndent()
        )
        filters.rid?.let { variable("rid", it) }
        filters.name?.let { variable("name", it) }
        filters.cluster?.let { variable("cluster", it) }
        filters.container?.let { variable("container", it) }
        filters.state?.let { variable("state", it) }
        filters.system?.let { variable("system", it) }
        filters.pagination.limit?.let { variable("limit", it) }
        filters.pagination.offset?.let { variable("offset", it) }
        filters.sorting.sortBy?.let { variable("sortBy", it) }
        filters.sorting.sortDirection?.let { variable("sortDirection", it) }
    }
}
