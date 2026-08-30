package org.chromia.data.queries

import org.chromia.domain.*

object TransactionQueries {

    fun getAllTransactions(filters: TransactionFilters): GraphQLQuery = graphqlQuery {
        query(
            $$"""
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
            """.trimIndent()
        )
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

    fun getSignerBlockchains(signer: String): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query signerBlockchains($signer: String!) {
                signerBlockchains(signer: $signer) {
                    blockchain { rid, name }, transactionCount
                }
            }
            """.trimIndent()
        )
        variable("signer", signer)
    }

    fun getAccountBlockchains(accountId: String): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query accountBlockchains($accountId: String!) {
                accountBlockchains(accountId: $accountId) {
                    blockchain { rid, name }, transactionCount, transfersCount
                }
            }
            """.trimIndent()
        )
        variable("accountId", accountId)
    }

    fun getNodeUnavailability(pubkey: String, startTimestamp: String): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query getNodeUnavailability($pubkey: String!, $startTimestamp: String!) {
                getNodeUnavailability(pubkey: $pubkey, startTimestamp: $startTimestamp) {
                    blockchainRid, intervals { start, end }
                }
            }
            """.trimIndent()
        )
        variable("pubkey", pubkey)
        variable("startTimestamp", startTimestamp)
    }

    fun getChrAggregates(
        includeTotals: Boolean,
        includeGroupedDeposits: Boolean,
        includeGroupedWithdrawals: Boolean
    ): GraphQLQuery = graphqlQuery {
        query(
            $$"""
            query GetChrAggregates($includeTotals: Boolean, $includeGroupedDeposits: Boolean, $includeGroupedWithdrawals: Boolean) {
                chrAggregates(includeTotals: $includeTotals, includeGroupedDeposits: $includeGroupedDeposits, includeGroupedWithdrawals: $includeGroupedWithdrawals) {
                    groupedDeposits { address, networkId, total },
                    groupedWithdrawals { address, networkId, total },
                    totals { depositsTotal, withdrawalsTotal }
                }
            }
            """.trimIndent()
        )
        variable("includeTotals", includeTotals)
        variable("includeGroupedDeposits", includeGroupedDeposits)
        variable("includeGroupedWithdrawals", includeGroupedWithdrawals)
    }
}
