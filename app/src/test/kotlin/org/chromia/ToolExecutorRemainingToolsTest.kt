package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.domain.NetworkResult
import org.chromia.tools.AccountBlockchainsStrategy
import org.chromia.tools.AllAssetsStrategy
import org.chromia.tools.AllOperationsStrategy
import org.chromia.tools.AssetBlockchainsStrategy
import org.chromia.tools.AssetDistributionStrategy
import org.chromia.tools.BlockchainsTransactionsStrategy
import org.chromia.tools.BlockchainAnalyticsStrategy
import org.chromia.tools.ChrAggregatesStrategy
import org.chromia.tools.MonthlyActiveAccountsPerChainStrategy
import org.chromia.tools.NodeUnavailabilityStrategy
import org.chromia.tools.SignerBlockchainsStrategy
import org.chromia.tools.ToolStrategy
import org.chromia.tools.TotalRewardsPaidStrategy
import org.chromia.tools.TransactionsByClusterStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToolExecutorRemainingToolsTest {

    private fun textOf(result: io.modelcontextprotocol.kotlin.sdk.CallToolResult): String =
        (result.content.first() as TextContent).text!!

    private suspend fun executeSuccess(
        strategy: ToolStrategy,
        name: String,
        arguments: kotlinx.serialization.json.JsonObject,
        repo: RecordingRepository = RecordingRepository()
    ): Pair<RecordingRepository, String> {
        repo.next = NetworkResult.Success(buildJsonObject { put("ok", name) })
        val result = strategy.execute(CallToolRequest(name = name, arguments = arguments), repo)
        return repo to textOf(result)
    }

    @Test
    fun getBlockchainsTransactionsForwardsNetwork() = runBlocking {
        val (repo, text) = executeSuccess(
            BlockchainsTransactionsStrategy(),
            "get_blockchains_transactions",
            buildJsonObject { put("network", "mainnet") }
        )
        assertEquals("getBlockchainsTransactions", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("get_blockchains_transactions", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getTransactionsByClusterForwardsNetwork() = runBlocking {
        val (repo, text) = executeSuccess(
            TransactionsByClusterStrategy(),
            "get_transactions_by_cluster",
            buildJsonObject { put("network", "testnet") }
        )
        assertEquals("getTransactionsByCluster", repo.lastCall)
        assertEquals("testnet", repo.lastNetwork)
        assertEquals("get_transactions_by_cluster", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAllAssetsForwardsNetwork() = runBlocking {
        val (repo, text) = executeSuccess(
            AllAssetsStrategy(),
            "get_all_assets",
            buildJsonObject { put("network", "mainnet") }
        )
        assertEquals("getAllAssets", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("get_all_assets", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getTotalRewardsPaidForwardsNetwork() = runBlocking {
        val (repo, text) = executeSuccess(
            TotalRewardsPaidStrategy(),
            "get_total_rewards_paid",
            buildJsonObject { put("network", "testnet") }
        )
        assertEquals("getTotalRewardsPaid", repo.lastCall)
        assertEquals("testnet", repo.lastNetwork)
        assertEquals("get_total_rewards_paid", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAllOperationsForwardsNetwork() = runBlocking {
        val (repo, text) = executeSuccess(
            AllOperationsStrategy(),
            "get_all_operations",
            buildJsonObject { put("network", "mainnet") }
        )
        assertEquals("getAllOperations", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("get_all_operations", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAssetDistributionForwardsFilters() = runBlocking {
        val (repo, text) = executeSuccess(
            AssetDistributionStrategy(),
            "get_asset_distribution",
            buildJsonObject {
                put("assetId", "chr-asset")
                put("network", "mainnet")
                put("brids", buildJsonArray { add("brid-1") })
                put("accountTypes", buildJsonArray { add("FT4_USER") })
                put("excludeAccounts", buildJsonArray { add("treasury") })
                put("excludeBrids", buildJsonArray { add("brid-x") })
                put("excludeAccountTypes", buildJsonArray { add("SYSTEM") })
            }
        )
        assertEquals("getAssetDistribution", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("chr-asset", repo.lastAssetId)
        assertEquals(listOf("brid-1"), repo.lastAssetFilters?.brids)
        assertEquals(listOf("FT4_USER"), repo.lastAssetFilters?.accountTypes)
        assertEquals(listOf("treasury"), repo.lastAssetFilters?.excludeAccounts)
        assertEquals(listOf("brid-x"), repo.lastAssetFilters?.excludeBrids)
        assertEquals(listOf("SYSTEM"), repo.lastAssetFilters?.excludeAccountTypes)
        assertEquals("get_asset_distribution", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAssetDistributionMissingAssetIdThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AssetDistributionStrategy().execute(
                    CallToolRequest(
                        name = "get_asset_distribution",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("assetId"))
    }

    @Test
    fun getBlockchainAnalyticsForwardsParams() = runBlocking {
        val (repo, text) = executeSuccess(
            BlockchainAnalyticsStrategy(),
            "get_blockchain_analytics",
            buildJsonObject {
                put("brid", "brid-9")
                put("network", "mainnet")
                put("fromTimestamp", "2026-01-01T00:00:00Z")
            }
        )
        assertEquals("getBlockchainAnalytics", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("brid-9", repo.lastBrid)
        assertEquals("2026-01-01T00:00:00Z", repo.lastFromTimestamp)
        assertEquals("get_blockchain_analytics", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getBlockchainAnalyticsMissingBridThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                BlockchainAnalyticsStrategy().execute(
                    CallToolRequest(
                        name = "get_blockchain_analytics",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("brid"))
    }

    @Test
    fun getMonthlyActiveAccountsPerChainForwardsParams() = runBlocking {
        val (repo, text) = executeSuccess(
            MonthlyActiveAccountsPerChainStrategy(),
            "get_monthly_active_accounts_per_chain",
            buildJsonObject {
                put("brid", "brid-maa")
                put("network", "testnet")
                put("untilTimestamp", "2026-08-01T00:00:00Z")
            }
        )
        assertEquals("getMonthlyActiveAccountsPerChain", repo.lastCall)
        assertEquals("testnet", repo.lastNetwork)
        assertEquals("brid-maa", repo.lastBrid)
        assertEquals("2026-08-01T00:00:00Z", repo.lastUntilTimestamp)
        assertEquals(
            "get_monthly_active_accounts_per_chain",
            Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun getMonthlyActiveAccountsPerChainMissingBridThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                MonthlyActiveAccountsPerChainStrategy().execute(
                    CallToolRequest(
                        name = "get_monthly_active_accounts_per_chain",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("brid"))
    }

    @Test
    fun getChrAggregatesForwardsFlags() = runBlocking {
        val (repo, text) = executeSuccess(
            ChrAggregatesStrategy(),
            "get_chr_aggregates",
            buildJsonObject {
                put("network", "mainnet")
                put("includeTotals", false)
                put("includeGroupedDeposits", true)
                put("includeGroupedWithdrawals", false)
            }
        )
        assertEquals("getChrAggregates", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals(false, repo.lastIncludeTotals)
        assertEquals(true, repo.lastIncludeGroupedDeposits)
        assertEquals(false, repo.lastIncludeGroupedWithdrawals)
        assertEquals("get_chr_aggregates", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getChrAggregatesDefaultsFlagsToTrue() = runBlocking {
        val (repo, _) = executeSuccess(
            ChrAggregatesStrategy(),
            "get_chr_aggregates",
            buildJsonObject { put("network", "testnet") }
        )
        assertEquals("getChrAggregates", repo.lastCall)
        assertEquals(true, repo.lastIncludeTotals)
        assertEquals(true, repo.lastIncludeGroupedDeposits)
        assertEquals(true, repo.lastIncludeGroupedWithdrawals)
    }

    @Test
    fun getAssetBlockchainsForwardsParams() = runBlocking {
        val (repo, text) = executeSuccess(
            AssetBlockchainsStrategy(),
            "get_asset_blockchains",
            buildJsonObject {
                put("assetId", "asset-1")
                put("network", "mainnet")
            }
        )
        assertEquals("getAssetBlockchains", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("asset-1", repo.lastAssetId)
        assertEquals("get_asset_blockchains", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAssetBlockchainsMissingAssetIdThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AssetBlockchainsStrategy().execute(
                    CallToolRequest(
                        name = "get_asset_blockchains",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("assetId"))
    }

    @Test
    fun getSignerBlockchainsForwardsParams() = runBlocking {
        val (repo, text) = executeSuccess(
            SignerBlockchainsStrategy(),
            "get_signer_blockchains",
            buildJsonObject {
                put("signer", "025C06D4")
                put("network", "testnet")
            }
        )
        assertEquals("getSignerBlockchains", repo.lastCall)
        assertEquals("testnet", repo.lastNetwork)
        assertEquals("025C06D4", repo.lastSigner)
        assertEquals("get_signer_blockchains", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getSignerBlockchainsMissingSignerThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                SignerBlockchainsStrategy().execute(
                    CallToolRequest(
                        name = "get_signer_blockchains",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("signer"))
    }

    @Test
    fun getAccountBlockchainsForwardsParams() = runBlocking {
        val (repo, text) = executeSuccess(
            AccountBlockchainsStrategy(),
            "get_account_blockchains",
            buildJsonObject {
                put("accountId", "acc-42")
                put("network", "mainnet")
            }
        )
        assertEquals("getAccountBlockchains", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("acc-42", repo.lastAccountId)
        assertEquals("get_account_blockchains", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getAccountBlockchainsMissingAccountIdThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                AccountBlockchainsStrategy().execute(
                    CallToolRequest(
                        name = "get_account_blockchains",
                        arguments = buildJsonObject { put("network", "mainnet") }
                    ),
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("accountId"))
    }

    @Test
    fun getNodeUnavailabilityForwardsParams() = runBlocking {
        val (repo, text) = executeSuccess(
            NodeUnavailabilityStrategy(),
            "get_node_unavailability",
            buildJsonObject {
                put("pubkey", "02DDAEA3")
                put("startTimestamp", "1736373600000")
                put("network", "mainnet")
            }
        )
        assertEquals("getNodeUnavailability", repo.lastCall)
        assertEquals("mainnet", repo.lastNetwork)
        assertEquals("02DDAEA3", repo.lastPubkey)
        assertEquals("1736373600000", repo.lastStartTimestamp)
        assertEquals("get_node_unavailability", Json.parseToJsonElement(text).jsonObject["ok"]!!.jsonPrimitive.content)
    }

    @Test
    fun getNodeUnavailabilityMissingPubkeyThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                NodeUnavailabilityStrategy().execute(
                    CallToolRequest(
                        name = "get_node_unavailability",
                        arguments = buildJsonObject {
                            put("startTimestamp", "1736373600000")
                            put("network", "mainnet")
                        }
                    ),
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("pubkey"))
    }

    @Test
    fun getNodeUnavailabilityMissingStartTimestampThrows() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                NodeUnavailabilityStrategy().execute(
                    CallToolRequest(
                        name = "get_node_unavailability",
                        arguments = buildJsonObject {
                            put("pubkey", "02DDAEA3")
                            put("network", "mainnet")
                        }
                    ),
                    RecordingRepository()
                )
            }
        }
        assertTrue(error.message!!.contains("startTimestamp"))
    }
}
