package org.chromia.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import net.postchain.common.BlockchainRid
import org.chromia.domain.BlockchainFilters
import org.chromia.domain.ChromiaRepository
import org.chromia.domain.NetworkResult
import org.chromia.domain.PaginationParams
import org.chromia.domain.SortingParams

interface ToolStrategy {
    suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult
}

class ToolExecutor(
    private val repository: ChromiaRepository,
    promptManager: PromptManager,
) {

    // NOTE: run this instance async (it loads documents, creates embeddings for them...) otherwise it will block
    private val ragStoreScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val ragStoreDeferred: Deferred<RagStore> = ragStoreScope.async { RagStore() }

    private val strategies = mapOf(
        "get_prompts" to PromptsToolStrategy(promptManager),
        "get_blockchains_transactions" to BlockchainsTransactionsStrategy(),
        "get_transactions_by_cluster" to TransactionsByClusterStrategy(),
        "get_all_assets" to AllAssetsStrategy(),
        "get_total_rewards_paid" to TotalRewardsPaidStrategy(),
        "get_asset_distribution" to AssetDistributionStrategy(),
        "get_asset_top_holders" to AssetTopHoldersStrategy(),
        "get_blockchain_analytics" to BlockchainAnalyticsStrategy(),
        "get_blockchain_details" to BlockchainDetailsStrategy(),
        "get_monthly_active_accounts_per_chain" to MonthlyActiveAccountsPerChainStrategy(),
        "get_all_transactions" to AllTransactionsStrategy(),
        "get_all_operations" to AllOperationsStrategy(),
        "filter_blockchains" to FilterBlockchainsStrategy(),
        "filter_assets" to FilterAssetsStrategy(),
        "get_chr_aggregates" to ChrAggregatesStrategy(),
        "get_asset_blockchains" to AssetBlockchainsStrategy(),
        "get_signer_blockchains" to SignerBlockchainsStrategy(),
        "get_account_blockchains" to AccountBlockchainsStrategy(),
        "get_node_unavailability" to NodeUnavailabilityStrategy(),
        "get_network_stats" to NetworkStatsStrategy(),
        "fetch_docs" to FetchDocsStrategy(ragStoreDeferred),
        "search" to FetchMock("search"),
        "fetch" to FetchMock(),
        "chromia_dapp_query" to DappInteractionStrategy()
    )

    suspend fun executeTool(request: CallToolRequest) = runCatching {
        val strategy = strategies[request.name]
            ?: return CallToolResult(
                content = listOf(TextContent("Unknown tool: ${request.name}"))
            )
        strategy.execute(request, repository)
    }.onFailure {
        CallToolResult(
            content = listOf(TextContent("Tool execution failed: ${it.message}"))
        )
    }.getOrNull()
        ?: CallToolResult(
            content = listOf(TextContent("Tool execution failed"))
        )
}

abstract class BaseToolStrategy : ToolStrategy {
    protected fun extractString(arguments: Map<String, Any>, key: String): String? {
        return arguments[key]?.let { value ->
            when (value) {
                is String -> value
                is JsonPrimitive -> value.content
                else -> value.toString()
            }
        }
    }

    protected fun extractInt(arguments: Map<String, Any>, key: String): Int? {
        return arguments[key]?.let { value ->
            when (value) {
                is Int -> value
                is JsonPrimitive -> value.intOrNull
                is String -> value.toIntOrNull()
                else -> value.toString().toIntOrNull()
            }
        }
    }

    protected fun extractBoolean(arguments: Map<String, Any>, key: String): Boolean? {
        return arguments[key]?.let { value ->
            when (value) {
                is Boolean -> value
                is JsonPrimitive -> value.booleanOrNull
                is String -> value.toBooleanStrictOrNull()
                else -> value.toString().toBooleanStrictOrNull()
            }
        }
    }

    protected fun extractStringList(arguments: Map<String, Any>, key: String): List<String>? {
        return arguments[key]?.let { value ->
            when (value) {
                is List<*> -> value.mapNotNull { item ->
                    when (item) {
                        is String -> item
                        is JsonPrimitive -> item.content
                        else -> item?.toString()
                    }
                }
                is JsonArray -> value.mapNotNull { element ->
                    (element as? JsonPrimitive)?.content ?: element.toString()
                }
                else -> null
            }
        }
    }

    protected fun handleResult(result: NetworkResult<JsonObject>, errorMessage: String): CallToolResult {
        return when (result) {
            is NetworkResult.Success -> CallToolResult(
                content = listOf(TextContent(Json.encodeToString(result.data)))
            )
            is NetworkResult.Error -> {
                CallToolResult(
                    content = listOf(TextContent("$errorMessage: ${result.message}"))
                )
            }
        }
    }

    protected fun requireParameter(arguments: Map<String, Any>, key: String): String {
        return extractString(arguments, key)
            ?: throw IllegalArgumentException("Missing required parameter: $key")
    }
}

class PromptsToolStrategy(private val promptManager: PromptManager) : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val category = extractString(args, "category")
        val tool = extractString(args, "tool")
        val search = extractString(args, "search")

        val allPrompts = if (category != null) {
            mapOf(category to promptManager.getPromptsForCategory(category))
        } else {
            promptManager.getCategories().associateWith { cat ->
                promptManager.getPromptsForCategory(cat)
            }
        }

        val filteredPrompts = allPrompts.mapValues { (_, prompts) ->
            prompts?.filter { prompt ->
                if (tool != null) {
                    promptManager.getToolForPrompt(prompt) == tool
                } else {
                    true
                }
            }
        }

        val searchedPrompts = if (search != null) {
            filteredPrompts.mapValues { (_, prompts) ->
                prompts?.filter { prompt ->
                    val promptText = prompt["prompt"]?.jsonPrimitive?.content ?: ""
                    val description = prompt["description"]?.jsonPrimitive?.content ?: ""
                    promptText.contains(search, ignoreCase = true) ||
                        description.contains(search, ignoreCase = true)
                }
            }
        } else {
            filteredPrompts
        }

        val result = buildJsonObject {
            put(
                "prompts",
                buildJsonObject {
                    searchedPrompts.filter { (_, prompts) ->
                        prompts?.isNotEmpty() == true
                    }.forEach { (category, prompts) ->
                        put(category, JsonArray(prompts!!.map { JsonObject(it.toMap()) }))
                    }
                }
            )
        }

        return CallToolResult(
            content = listOf(TextContent(Json.encodeToString(result)))
        )
    }
}

class BlockchainsTransactionsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getBlockchainsTransactions(network)
        return handleResult(result, "Failed to get blockchains transactions")
    }
}

class TransactionsByClusterStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getTransactionsByCluster(network)
        return handleResult(result, "Failed to get transactions by cluster")
    }
}

class AllAssetsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getAllAssets(network)
        return handleResult(result, "Failed to get all assets")
    }
}

class TotalRewardsPaidStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getTotalRewardsPaid(network)
        return handleResult(result, "Failed to get total rewards paid")
    }
}

class AssetDistributionStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val assetId = requireParameter(args, "assetId")
        val network = extractString(args, "network")

        val filters = org.chromia.domain.AssetFilters(
            brids = extractStringList(args, "brids"),
            accountTypes = extractStringList(args, "accountTypes"),
            excludeAccounts = extractStringList(args, "excludeAccounts"),
            excludeBrids = extractStringList(args, "excludeBrids"),
            excludeAccountTypes = extractStringList(args, "excludeAccountTypes")
        )

        val result = repository.getAssetDistribution(assetId, network, filters)
        return handleResult(result, "Failed to get asset distribution")
    }
}

class AssetTopHoldersStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val assetId = requireParameter(args, "assetId")
        val network = extractString(args, "network")
        val limit = extractInt(args, "limit")

        val filters = org.chromia.domain.AssetFilters(
            brids = extractStringList(args, "brids"),
            accountTypes = extractStringList(args, "accountTypes"),
            excludeAccounts = extractStringList(args, "excludeAccounts"),
            excludeBrids = extractStringList(args, "excludeBrids"),
            excludeAccountTypes = extractStringList(args, "excludeAccountTypes")
        )

        val result = repository.getAssetTopHolders(assetId, network, limit, filters)
        return handleResult(result, "Failed to get asset top holders")
    }
}

class BlockchainAnalyticsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val brid = requireParameter(args, "brid")
        val network = extractString(args, "network")
        val fromTimestamp = extractString(args, "fromTimestamp")

        val result = repository.getBlockchainAnalytics(brid, network, fromTimestamp)
        return handleResult(result, "Failed to get blockchain analytics")
    }
}

class BlockchainDetailsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val rid = requireParameter(args, "rid")
        val network = extractString(args, "network")

        val result = repository.getBlockchainDetails(rid, network)
        return handleResult(result, "Failed to get blockchain details")
    }
}

class MonthlyActiveAccountsPerChainStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val brid = requireParameter(args, "brid")
        val network = extractString(args, "network")
        val untilTimestamp = extractString(args, "untilTimestamp")

        val result = repository.getMonthlyActiveAccountsPerChain(brid, network, untilTimestamp)
        return handleResult(result, "Failed to get monthly active accounts per chain")
    }
}

class AllTransactionsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val filters = org.chromia.domain.TransactionFilters(
            rid = extractString(args, "rid"),
            blockId = extractString(args, "blockId"),
            blockchainIds = extractStringList(args, "blockchainIds"),
            notInBlockchains = extractStringList(args, "notInBlockchains"),
            timestampFrom = extractString(args, "timestampFrom"),
            timestampTo = extractString(args, "timestampTo"),
            operations = extractStringList(args, "operations"),
            notInOperations = extractStringList(args, "notInOperations"),
            signers = extractStringList(args, "signers"),
            excludedSigners = extractStringList(args, "excludedSigners"),
            accounts = extractStringList(args, "accounts"),
            excludedAccounts = extractStringList(args, "excludedAccounts"),
            assets = extractStringList(args, "assets"),
            pagination = PaginationParams(
                limit = extractInt(args, "limit"),
                offset = extractInt(args, "offset")
            ),
            sorting = SortingParams(
                sortBy = extractString(args, "sortBy"),
                sortDirection = extractString(args, "sortDirection")
            )
        )

        val result = repository.getAllTransactions(network, filters)
        return handleResult(result, "Failed to get all transactions")
    }
}

class AllOperationsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getAllOperations(network)
        return handleResult(result, "Failed to get all operations")
    }
}

class FilterAssetsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val filters = org.chromia.domain.AssetSearchFilters(
            brid = extractString(args, "brid"),
            searchQuery = extractString(args, "searchQuery"),
            type = extractString(args, "type"),
            pagination = PaginationParams(
                limit = extractInt(args, "limit"),
                offset = extractInt(args, "offset")
            )
        )

        val result = repository.filterAssets(network, filters)
        return handleResult(result, "Failed to filter assets")
    }
}

class ChrAggregatesStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")
        val includeTotals = extractBoolean(args, "includeTotals") ?: true
        val includeGroupedDeposits = extractBoolean(args, "includeGroupedDeposits") ?: true
        val includeGroupedWithdrawals = extractBoolean(args, "includeGroupedWithdrawals") ?: true

        val result = repository.getChrAggregates(
            network,
            includeTotals,
            includeGroupedDeposits,
            includeGroupedWithdrawals
        )
        return handleResult(result, "Failed to get CHR aggregates")
    }
}

class AssetBlockchainsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val assetId = requireParameter(args, "assetId")
        val network = extractString(args, "network")

        val result = repository.getAssetBlockchains(network, assetId)
        return handleResult(result, "Failed to get asset blockchains")
    }
}

class SignerBlockchainsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val signer = requireParameter(args, "signer")
        val network = extractString(args, "network")

        val result = repository.getSignerBlockchains(network, signer)
        return handleResult(result, "Failed to get signer blockchains")
    }
}

class AccountBlockchainsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val accountId = requireParameter(args, "accountId")
        val network = extractString(args, "network")

        val result = repository.getAccountBlockchains(accountId, network)
        return handleResult(result, "Failed to get account blockchains")
    }
}

class NodeUnavailabilityStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val pubkey = requireParameter(args, "pubkey")
        val startTimestamp = requireParameter(args, "startTimestamp")
        val network = extractString(args, "network")

        val result = repository.getNodeUnavailability(pubkey, startTimestamp, network)
        return handleResult(result, "Failed to get node unavailability")
    }
}

class NetworkStatsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getNetworkStats(network)
        return handleResult(result, "Failed to get network stats")
    }
}

class FilterBlockchainsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")

        val filters = BlockchainFilters(
            rid = extractString(args, "rid"),
            name = extractString(args, "name"),
            cluster = extractString(args, "cluster"),
            container = extractString(args, "container"),
            state = extractString(args, "state"),
            system = extractBoolean(args, "system"),
            pagination = PaginationParams(
                limit = extractInt(args, "limit"),
                offset = extractInt(args, "offset")
            ),
            sorting = SortingParams(
                sortBy = extractString(args, "sortBy"),
                sortDirection = extractString(args, "sortDirection")
            )
        )

        val result = repository.filterBlockchains(network, filters)
        return handleResult(result, "Failed to get all blockchains")
    }
}

class DappInteractionStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = extractString(args, "network")
        val blockchainRid = requireParameter(args, "blockchainRid")
        val queryName = extractString(args, "query")
        val arguments = extractArgumentsMap(args, "arguments")

        val result = repository.executeCustomQuery(
            network,
            BlockchainRid.buildFromHex(blockchainRid),
            queryName,
            arguments
        )

        return handleResult(result, "Failed to execute dapp query $queryName --> $arguments")
    }

    private fun extractArgumentsMap(arguments: Map<String, Any>, key: String): Map<String, Any> {
        return arguments[key]?.let { value ->
            when (value) {
                is Map<*, *> -> {
                    val stringMap = mutableMapOf<String, Any>()
                    value.forEach { (k, v) ->
                        if (k != null && v != null) {
                            stringMap[k.toString()] = extractPrimitiveValue(v)
                        }
                    }
                    stringMap
                }
                else -> emptyMap()
            }
        } ?: emptyMap()
    }

    private fun extractPrimitiveValue(value: Any): Any {
        return when (value) {
            is JsonPrimitive -> {
                when {
                    value.isString -> value.content
                    value.booleanOrNull != null -> value.boolean
                    value.intOrNull != null -> value.int
                    value.longOrNull != null -> value.long
                    value.doubleOrNull != null -> value.double
                    else -> value.content
                }
            }
            is JsonArray -> value.map { extractPrimitiveValue(it) }
            is JsonObject -> {
                val map = mutableMapOf<String, Any>()
                value.forEach { (k, v) ->
                    map[k] = extractPrimitiveValue(v)
                }
                map
            }
            else -> value
        }
    }
}

class FetchDocsStrategy(private val ragStoreDeferred: Deferred<RagStore>) : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val query = requireParameter(args, "query")

        return runCatching {
            val ragStore = ragStoreDeferred.await()
            val result = ragStore.query(query)?.joinToString("\n\n") 
                ?: "Documentation not found for requested query!"
            CallToolResult(
                content = listOf(TextContent(result))
            )
        }.getOrElse { e ->
            CallToolResult(
                content = listOf(TextContent("Error fetching documentation from classpath: ${e.message}"))
            )
        }
    }
}

// TODO: properly implement this function or use the RAG tool so that it will work with ChatGPT
class FetchMock(val type: String = "fetch") : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {

        return runCatching {
            val jsonSearch = buildJsonObject {
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", buildJsonObject {
                            put("results", buildJsonArray {
                                add(buildJsonObject {
                                    put("id", "doc-1")
                                    put("title", "chromia docs")
                                    put("url", "https://docs.chromia.com")
                                })
                            })
                        }.toString())
                    })
                })
            }

            val jsonFetch = buildJsonObject {
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "text")
                        put("text", buildJsonObject {
                            put("id", "doc-1")
                            put("title", "chromia docs")
                            put("text", "full text of chromia docs")
                            put("url", "https://docs.chromia.com")
                            put("metadata", buildJsonObject {
                                put("source", "vector_store")
                            })
                        }.toString())
                    })
                })
            }

            val jsonString = Json.encodeToString(if (type == "search") jsonSearch else jsonFetch)
            CallToolResult(
                content = listOf(TextContent(jsonString))
            )
        }.getOrElse { e ->
            CallToolResult(
                content = listOf(TextContent("Error fetching documentation from classpath: ${e.message}"))
            )
        }
    }
}
