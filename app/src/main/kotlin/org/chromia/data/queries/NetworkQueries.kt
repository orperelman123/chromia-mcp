package org.chromia.data.queries

import org.chromia.domain.GraphQLQuery
import org.chromia.domain.graphqlQuery

object NetworkQueries {

    fun getBlockchainsTransactions(): GraphQLQuery = graphqlQuery {
        query(
            """
            query { 
                groupedTransactionsByBlockchain { 
                    brid, 
                    blockchain { name, system, cluster, state }, 
                    blockHeight, 
                    throughput, 
                    count 
                } 
            }
            """.trimIndent()
        )
    }

    fun getTransactionsByCluster(): GraphQLQuery = graphqlQuery {
        query("query { groupedTransactionsByCluster { cluster, count } }")
    }

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

    fun getNetworkStats(): GraphQLQuery = graphqlQuery {
        query(
            """
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
            """.trimIndent()
        )
    }

    fun getAllOperations(): GraphQLQuery = graphqlQuery {
        query("query { operations { operation, brid } }")
    }
}
