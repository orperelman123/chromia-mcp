package org.chromia.data.queries

import org.chromia.domain.GraphQLQuery
import org.chromia.domain.graphqlQuery

object NetworkQueries {

    // Retired 2026-09-07 together with their tools: the explorer answers
    // INTERNAL_ERROR for `dashboardData` (get_network_stats,
    // get_transactions_by_cluster) and for top-level
    // `groupedTransactionsByBlockchain` (get_blockchains_transactions) on every
    // selection set, so there was no working form of these queries to keep.
    // See docs/UPSTREAM.md #3a.

    fun getAllAssets(): GraphQLQuery = graphqlQuery {
        query(
            """
            query { 
                allAssets { 
                    name, iconUrl, symbol, id, brid, type, 
                    decimals, supply, transferCount, blockchainCount 
                } 
            }
            """.trimIndent()
        )
    }

    fun getTotalRewardsPaid(): GraphQLQuery = graphqlQuery {
        query("query { totalRewardsPaid }")
    }

    fun getAllOperations(): GraphQLQuery = graphqlQuery {
        query("query { operations { operation, brid } }")
    }
}
