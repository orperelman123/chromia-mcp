package org.chromia.tools

import dev.langchain4j.data.segment.TextSegment
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
    ragStore: Deferred<RagStore>? = null,
    ragStoreFactory: (() -> RagStore)? = null,
) {

    // Runtime RagStore loads local embeddings.json first, then GitLab if that file is missing.
    // Construction of ToolExecutor must not start that load — only docs tools touch it.
    private val ragStoreDeferred: Deferred<RagStore> = ragStore
        ?: CoroutineScope(Dispatchers.IO + SupervisorJob()).async(start = CoroutineStart.LAZY) {
            (ragStoreFactory ?: { RagStore() })()
        }

    // Static help/INDEX tools, also reachable through the chromia_help(topic) gateway
    // so compact-tool deployments spend one schema instead of ~30 on agent context.
    private val helpStrategies = mapOf(
        "chr_build_help" to ChrBuildHelpStrategy(),
        "chr_repl_help" to ChrReplHelpStrategy(),
        "chr_tools_help" to ChrToolsHelpStrategy(),
        "chr_seeder_help" to ChrSeederHelpStrategy(),
        "blockchain_properties_help" to BlockchainPropertiesHelpStrategy(),
        "chr_eif_help" to ChrEifHelpStrategy(),
        "chromia_yml_definitions_help" to ChromiaYmlDefinitionsHelpStrategy(),
        "chr_completion_help" to ChrCompletionHelpStrategy(),
        "chromia_project_structure_help" to ChromiaProjectStructureHelpStrategy(),
        "chr_multi_signature_help" to ChrMultiSignatureHelpStrategy(),
        "chr_deploy_help" to ChrDeployHelpStrategy(),
        "chr_node_help" to ChrNodeHelpStrategy(),
        "chr_query_help" to ChrQueryHelpStrategy(),
        "vault_lease_help" to VaultLeaseHelpStrategy(),
        "chr_generate_client_help" to ChrGenerateClientHelpStrategy(),
        "chromia_docs_yml_help" to ChromiaDocsYmlHelpStrategy(),
        "chromia_cookbook_help" to ChromiaCookbookHelpStrategy(),
        "chr_key_id_help" to ChrKeyIdHelpStrategy(),
        "chromia_language_clients_help" to ChromiaLanguageClientsHelpStrategy(),
        "chromia_rell_language_help" to ChromiaRellLanguageHelpStrategy(),
        "chromia_rell_types_help" to ChromiaRellTypesHelpStrategy(),
        "chromia_rell_expressions_help" to ChromiaRellExpressionsHelpStrategy(),
        "chromia_rell_statements_help" to ChromiaRellStatementsHelpStrategy(),
        "chromia_rell_database_help" to ChromiaRellDatabaseHelpStrategy(),
        "chromia_rell_systemlib_help" to ChromiaRellSystemlibHelpStrategy(),
        "chromia_rell_practices_help" to ChromiaRellPracticesHelpStrategy(),
        "chromia_ft4_queries_help" to ChromiaFt4QueriesHelpStrategy(),
        "chromia_integrations_help" to ChromiaIntegrationsHelpStrategy(),
        "chromia_vector_search_help" to ChromiaVectorSearchHelpStrategy(),
        "chr_library_help" to ChrLibraryHelpStrategy(),
        "chr_create_rell_dapp_help" to ChrCreateRellDappHelpStrategy()
    )

    private val strategies = helpStrategies + mapOf(
        "chromia_help" to ChromiaHelpStrategy(helpStrategies),
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
        "search" to SearchDocsStrategy(ragStoreDeferred),
        "fetch" to FetchDocumentStrategy(ragStoreDeferred),
        "chromia_dapp_query" to DappInteractionStrategy(),
        "scaffold_dapp" to ScaffoldDappStrategy(),
        "validate_chromia_yml" to ValidateChromiaYmlStrategy(),
        "ft4_module_args" to Ft4ModuleArgsStrategy(),
        "write_deployment_config" to WriteDeploymentConfigStrategy(),
        "check_dapp_project" to CheckDappProjectStrategy(),
        "check_ft4_imports" to CheckFt4ImportsStrategy(),
        "rell_check" to RellCheckStrategy(),
        "rell_security_check" to RellSecurityCheckStrategy(),
        "run_rell_tests" to RunRellTestsStrategy()
    )

    suspend fun executeTool(request: CallToolRequest): CallToolResult {
        val startedAt = System.nanoTime()
        val result = runCatching {
            val strategy = strategies[request.name]
                ?: return toolErrorResult("Unknown tool: ${request.name}")
                    .also { logToolCall(request.name, startedAt, ok = false) }
            strategy.execute(request, repository)
        }.getOrElse { e ->
            toolErrorResult("Tool execution failed: ${e.message}")
        }
        // Usage telemetry: one structured stderr/file log line per call so hosted
        // deployments can see which tools agents actually use and how long they take.
        logToolCall(request.name, startedAt, ok = result.isError != true)
        return result
    }

    private fun logToolCall(name: String, startedAt: Long, ok: Boolean) {
        val ms = (System.nanoTime() - startedAt) / 1_000_000
        org.chromia.App.logger.info("tool-call name={} ms={} ok={}", name, ms, ok)
    }

    internal fun registeredToolNames(): Set<String> = strategies.keys

    /**
     * Pre-loads the RAG store and embedding model so the first real `search`
     * doesn't pay the ~15s cold-start observed in production telemetry.
     * Failures are logged and swallowed - warmup must never break startup.
     */
    suspend fun warmUpDocs() {
        runCatching {
            val started = System.nanoTime()
            ragStoreDeferred.await().query("warmup")
            org.chromia.App.logger.info("docs warmup done in {} ms", (System.nanoTime() - started) / 1_000_000)
        }.onFailure { e ->
            org.chromia.App.logger.warn("docs warmup failed: ${e.message}")
        }
    }
}

/**
 * Shared explorer-tool result shape. Success keeps the same JSON body as
 * [CallToolResult.content] (no per-tool outputSchema). Errors are
 * `{ "error": "<same message as text>" }` plus [CallToolResult.isError].
 */
internal fun toolSuccessResult(data: JsonObject): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(Json.encodeToString(data))),
        structuredContent = data
    )

internal fun toolErrorResult(message: String): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(message)),
        structuredContent = buildJsonObject { put("error", message) },
        isError = true
    )

abstract class BaseToolStrategy : ToolStrategy {
    protected fun extractString(arguments: Map<String, Any>, key: String): String? {
        val value = arguments[key] ?: return null
        return when (value) {
            is JsonNull -> null
            is String -> value
            is JsonPrimitive -> value.content
            else -> value.toString()
        }
    }

    protected fun extractInt(arguments: Map<String, Any>, key: String): Int? {
        val value = arguments[key] ?: return null
        return when (value) {
            is JsonNull -> null
            is Int -> value
            is JsonPrimitive -> value.intOrNull
            is String -> value.toIntOrNull()
            else -> value.toString().toIntOrNull()
        }
    }

    protected fun extractBoolean(arguments: Map<String, Any>, key: String): Boolean? {
        val value = arguments[key] ?: return null
        return when (value) {
            is JsonNull -> null
            is Boolean -> value
            is JsonPrimitive -> value.booleanOrNull
            is String -> value.toBooleanStrictOrNull()
            else -> value.toString().toBooleanStrictOrNull()
        }
    }

    protected fun extractStringList(arguments: Map<String, Any>, key: String): List<String>? {
        val raw = arguments[key] ?: return null
        if (raw is JsonNull) return null
        val items = when (raw) {
            is List<*> -> raw.mapNotNull { item ->
                when (item) {
                    null, is JsonNull -> null
                    is String -> item
                    is JsonPrimitive -> item.content
                    else -> item.toString()
                }?.trim()?.takeIf { it.isNotEmpty() }
            }
            is JsonArray -> raw.mapNotNull { element ->
                when (element) {
                    is JsonNull -> null
                    is JsonPrimitive -> element.content
                    else -> element.toString()
                }?.trim()?.takeIf { it.isNotEmpty() }
            }
            else -> return null
        }
        return items.takeIf { it.isNotEmpty() }
    }

    protected fun extractStringMap(arguments: Map<String, Any>, key: String): Map<String, String>? {
        val raw = arguments[key] ?: return null
        if (raw is JsonNull) return null
        if (raw is String || (raw is JsonPrimitive && raw.isString)) {
            val text = extractString(arguments, key) ?: return null
            return mapOf("rell" to text)
        }
        val entries = when (raw) {
            is Map<*, *> -> raw.entries
            is JsonObject -> raw.entries
            else -> return null
        }
        val out = linkedMapOf<String, String>()
        entries.forEach { (k, v) ->
            if (k == null || v == null || v is JsonNull) return@forEach
            val path = k.toString().trim().takeIf { it.isNotEmpty() } ?: return@forEach
            val content = when (v) {
                is String -> v
                is JsonPrimitive -> v.content
                else -> return@forEach
            }
            out[path] = content
        }
        return out.takeIf { it.isNotEmpty() }
    }

    protected fun handleResult(result: NetworkResult<JsonObject>, errorMessage: String): CallToolResult {
        return when (result) {
            is NetworkResult.Success -> toolSuccessResult(result.data)
            is NetworkResult.Error -> toolErrorResult("$errorMessage: ${result.message}")
        }
    }

    protected fun requireParameter(arguments: Map<String, Any>, key: String): String {
        return extractString(arguments, key)?.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Missing required parameter: $key")
    }
}

class PromptsToolStrategy(private val promptManager: PromptManager) : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return runCatching {
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
                        promptManager.matchesTool(prompt, tool)
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

            val stats = promptManager.getStatistics()
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
                put(
                    "statistics",
                    buildJsonObject {
                        put("totalCategories", stats.totalCategories)
                        put("totalPrompts", stats.totalPrompts)
                        put("toolsUsed", stats.toolsUsed)
                        put(
                            "categoriesWithPrompts",
                            JsonArray(stats.categoriesWithPrompts.map { JsonPrimitive(it) })
                        )
                    }
                )
            }

            toolSuccessResult(result)
        }.getOrElse { e ->
            toolErrorResult("Failed to get prompts: ${e.message}")
        }
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
            ),
            sorting = SortingParams(
                sortBy = extractString(args, "sortBy"),
                sortDirection = extractString(args, "sortDirection")
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
        val raw = arguments[key] ?: return emptyMap()
        if (raw is JsonNull) return emptyMap()
        return when (raw) {
            is Map<*, *> -> {
                val stringMap = mutableMapOf<String, Any>()
                raw.forEach { (k, v) ->
                    if (k == null || v == null) return@forEach
                    extractPrimitiveValue(v)?.let { stringMap[k.toString()] = it }
                }
                stringMap
            }
            else -> emptyMap()
        }
    }

    private fun extractPrimitiveValue(value: Any): Any? {
        return when (value) {
            is JsonNull -> null
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
            is JsonArray -> value.mapNotNull { extractPrimitiveValue(it) }
            is JsonObject -> {
                val map = mutableMapOf<String, Any>()
                value.forEach { (k, v) ->
                    extractPrimitiveValue(v)?.let { map[k] = it }
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
            val hits = ragStore.query(query)?.takeIf { it.isNotEmpty() }
            if (hits == null) {
                val notFound = "Documentation not found for requested query!"
                CallToolResult(
                    content = listOf(TextContent(notFound)),
                    structuredContent = buildJsonObject {
                        put("text", notFound)
                        put("hits", buildJsonArray {})
                    },
                    isError = true
                )
            } else {
                val result = formatFetchDocsText(hits)
                val hitsJson = buildJsonArray {
                    hits.forEach { segment ->
                        add(
                            buildJsonObject {
                                put("id", segmentId(segment))
                                put("title", segmentTitle(segment))
                                put("url", segmentUrl(segment))
                                put("text", segment.text())
                            }
                        )
                    }
                }
                CallToolResult(
                    content = listOf(TextContent(result)),
                    structuredContent = buildJsonObject {
                        put("text", result)
                        put("hits", hitsJson)
                    }
                )
            }
        }.getOrElse { e ->
            val message = "Error fetching documentation: ${e.message}"
            CallToolResult(
                content = listOf(TextContent(message)),
                structuredContent = buildJsonObject {
                    put("text", message)
                    put("hits", buildJsonArray {})
                },
                isError = true
            )
        }
    }
}

class SearchDocsStrategy(private val ragStoreDeferred: Deferred<RagStore>) : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val query = requireParameter(args, "query")

        return runCatching {
            val ragStore = ragStoreDeferred.await()
            val segments = ragStore.query(query).orEmpty()
            val results = buildJsonArray {
                segments.forEach { segment ->
                    add(
                        buildJsonObject {
                            put("id", segmentId(segment))
                            put("title", segmentTitle(segment))
                            put("url", segmentUrl(segment))
                        }
                    )
                }
            }
            val payload = buildJsonObject { put("results", results) }
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(payload))),
                structuredContent = payload
            )
        }.getOrElse { e ->
            val message = "Error searching documentation: ${e.message}"
            val payload = buildJsonObject { put("results", buildJsonArray {}) }
            CallToolResult(
                content = listOf(TextContent(message)),
                structuredContent = payload,
                isError = true
            )
        }
    }
}

class FetchDocumentStrategy(private val ragStoreDeferred: Deferred<RagStore>) : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val id = normalizeSegmentId(requireParameter(args, "id"))

        return runCatching {
            val ragStore = ragStoreDeferred.await()
            val segment = ragStore.fetchById(id)
            val payload = if (segment != null) {
                buildJsonObject {
                    put("id", id)
                    put("title", segmentTitle(segment))
                    put("text", segment.text())
                    put("url", segmentUrl(segment))
                }
            } else {
                buildJsonObject {
                    put("id", id)
                    put("error", "Documentation not found for requested id")
                }
            }
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(payload))),
                structuredContent = payload,
                isError = segment == null
            )
        }.getOrElse { e ->
            val payload = buildJsonObject {
                put("id", id)
                put("error", "Error fetching documentation: ${e.message}")
            }
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(payload))),
                structuredContent = payload,
                isError = true
            )
        }
    }
}

/**
 * fetch_docs one-line records. Real newlines in segment text are written as the two
 * characters `\n` so each hit stays one line. [fetch] still returns the original text.
 */
internal fun escapeFetchDocsSegmentText(text: String): String =
    text.replace("\\", "\\\\")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")

internal fun formatFetchDocsText(hits: List<TextSegment>): String =
    hits.joinToString("\n") { segment ->
        "id: ${segmentId(segment)} | ${escapeFetchDocsSegmentText(segment.text())}"
    }

class ScaffoldDappStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val name = extractString(args, "name")
        val template = extractString(args, "template") ?: "hello"
        val payload = DappScaffold.toJson(name, template)
        return toolSuccessResult(payload)
    }
}



class ValidateChromiaYmlStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val yaml = requireParameter(args, "yaml")
        return toolSuccessResult(ChromiaYmlValidator.validate(yaml).toJson())
    }
}

/**
 * Gateway over the static help/INDEX strategies: one `chromia_help(topic)` schema
 * instead of ~30 individual tool schemas in the agent's context. With no or an
 * unknown topic it returns the topic index; otherwise it delegates to the matching
 * help strategy and returns that tool's exact payload.
 */
class ChromiaHelpStrategy(private val helpStrategies: Map<String, ToolStrategy>) : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val rawTopic = extractString(args, "topic")?.trim()?.lowercase()
        // Accept both "chr_build" and "chr_build_help" spellings.
        val topic = rawTopic?.let { if (it in helpStrategies) it else "${it}_help".takeIf { t -> t in helpStrategies } }

        if (topic == null) {
            val index = buildJsonObject {
                put("topics", buildJsonArray { helpStrategies.keys.sorted().forEach { add(JsonPrimitive(it)) } })
                put(
                    "notes",
                    (if (rawTopic == null) "Pass one of these topics to get that help payload. "
                    else "Unknown topic '$rawTopic'. ") +
                        "Topic names map 1:1 to the full help catalog (CLI commands, chromia.yml, Rell language, FT4, deploy, integrations)."
                )
            }
            return toolSuccessResult(index)
        }
        return helpStrategies.getValue(topic).execute(request, repository)
    }
}

class RellCheckStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val source = extractString(args, "source")
        val filesArg = args["files"]
        val modules = extractStringList(args, "modules")

        val files = linkedMapOf<String, String>()
        if (filesArg is JsonObject) {
            filesArg.forEach { (path, content) ->
                if (content is JsonPrimitive && content.isString) {
                    files[path] = content.content
                }
            }
        }
        if (source != null && files.isEmpty()) {
            files["main.rell"] = source
        }
        if (files.isEmpty()) {
            return toolErrorResult(
                "rell_check needs Rell code: pass `source` (single main.rell) or `files` ({\"path.rell\": \"code\"})"
            )
        }

        return runCatching {
            val result = withContext(Dispatchers.IO) {
                with(RellCheck) { check(files, modules).toJson() }
            }
            toolSuccessResult(result)
        }.getOrElse { e ->
            toolErrorResult("rell_check failed: ${e.message}")
        }
    }
}

class RellSecurityCheckStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val source = extractString(args, "source")
        val filesArg = args["files"]

        val files = linkedMapOf<String, String>()
        if (filesArg is JsonObject) {
            filesArg.forEach { (path, content) ->
                if (content is JsonPrimitive && content.isString) {
                    files[path] = content.content
                }
            }
        }
        if (source != null && files.isEmpty()) {
            files["main.rell"] = source
        }
        if (files.isEmpty()) {
            return toolErrorResult(
                "rell_security_check needs Rell code: pass `source` or `files` ({\"path.rell\": \"code\"})"
            )
        }

        return runCatching {
            // Security findings on uncompilable code are noise - compile first.
            val compile = withContext(Dispatchers.IO) { RellCheck.check(files, null) }
            if (!compile.ok) {
                val compileJson = with(RellCheck) { compile.toJson() }
                return toolSuccessResult(
                    buildJsonObject {
                        put("ok", false)
                        put("operationsScanned", 0)
                        put("findings", buildJsonArray {})
                        put("compileErrors", compileJson.getValue("errors"))
                        put("notes", "Code does not compile - fix rell_check errors first, then re-run the security check.")
                    }
                )
            }
            toolSuccessResult(with(RellSecurityCheck) { analyze(files).toJson() })
        }.getOrElse { e ->
            toolErrorResult("rell_security_check failed: ${e.message}")
        }
    }
}

class RunRellTestsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val filesArg = args["files"]
        val files = linkedMapOf<String, String>()
        if (filesArg is JsonObject) {
            filesArg.forEach { (path, content) ->
                if (content is JsonPrimitive && content.isString) {
                    files[path] = content.content
                }
            }
        }
        if (files.isEmpty()) {
            return toolErrorResult(
                "run_rell_tests needs a `files` map including at least one `@test module;` file, e.g. {\"main.rell\": \"module; ...\", \"tests/main_test.rell\": \"@test module; ...\"}"
            )
        }
        return runCatching {
            val result = withContext(Dispatchers.IO) {
                with(RunRellTests) { run(files).toJson() }
            }
            toolSuccessResult(result)
        }.getOrElse { e ->
            toolErrorResult("run_rell_tests failed: ${e.message}")
        }
    }
}

class Ft4ModuleArgsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val name = extractString(args, "name")
        val includeIccf = extractBoolean(args, "includeIccf") ?: false
        return toolSuccessResult(Ft4ModuleArgs.toJson(name, includeIccf))
    }
}

class ChrBuildHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrBuildHelp.toJson())
    }
}

class ChrReplHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrReplHelp.toJson())
    }
}

class ChrToolsHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrToolsHelp.toJson())
    }
}

class ChrSeederHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrSeederHelp.toJson())
    }
}

class BlockchainPropertiesHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(BlockchainPropertiesHelp.toJson())
    }
}

class ChrEifHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrEifHelp.toJson())
    }
}

class ChromiaYmlDefinitionsHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaYmlDefinitionsHelp.toJson())
    }
}

class ChrCompletionHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrCompletionHelp.toJson())
    }
}

class ChromiaProjectStructureHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaProjectStructureHelp.toJson())
    }
}

class ChrMultiSignatureHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrMultiSignatureHelp.toJson())
    }
}

class WriteDeploymentConfigStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val network = requireParameter(args, "network")
        val name = extractString(args, "name") ?: extractString(args, "chain")
        if (WriteDeploymentConfig.resolveNetwork(network) == null) {
            return toolErrorResult(WriteDeploymentConfig.unknownNetworkMessage(network))
        }
        return toolSuccessResult(WriteDeploymentConfig.toJson(network, name))
    }
}

class ChrDeployHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrDeployHelp.toJson())
    }
}

class ChrNodeHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrNodeHelp.toJson())
    }
}

class ChrQueryHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrQueryHelp.toJson())
    }
}

class VaultLeaseHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(VaultLeaseHelp.toJson())
    }
}

class ChrGenerateClientHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrGenerateClientHelp.toJson())
    }
}

class ChromiaCookbookHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaCookbookHelp.toJson())
    }
}

class ChrKeyIdHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrKeyIdHelp.toJson())
    }
}

class ChromiaLanguageClientsHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaLanguageClientsHelp.toJson())
    }
}

class ChromiaDocsYmlHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaDocsYmlHelp.toJson())
    }
}

class ChrLibraryHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrLibraryHelp.toJson())
    }
}

class ChrCreateRellDappHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrCreateRellDappHelp.toJson())
    }
}

class CheckDappProjectStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val yaml = requireParameter(args, "yaml")
        val rellFiles = extractStringMap(args, "rell")
            ?: throw IllegalArgumentException("Missing required parameter: rell")
        return toolSuccessResult(CheckDappProject.check(yaml, rellFiles).toJson())
    }
}

class CheckFt4ImportsStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.arguments as Map<String, Any>
        val rellFiles = extractStringMap(args, "rell")
            ?: throw IllegalArgumentException("Missing required parameter: rell")
        return toolSuccessResult(Ft4ImportCheck.scanFiles(rellFiles).toJson())
    }
}

class ChromiaRellLanguageHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellLanguageHelp.toJson())
    }
}

class ChromiaFt4QueriesHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaFt4QueriesHelp.toJson())
    }
}

class ChromiaIntegrationsHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaIntegrationsHelp.toJson())
    }
}

class ChromiaVectorSearchHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaVectorSearchHelp.toJson())
    }
}

class ChromiaRellTypesHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellTypesHelp.toJson())
    }
}

class ChromiaRellExpressionsHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellExpressionsHelp.toJson())
    }
}

class ChromiaRellStatementsHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellStatementsHelp.toJson())
    }
}

class ChromiaRellDatabaseHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellDatabaseHelp.toJson())
    }
}

class ChromiaRellSystemlibHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellSystemlibHelp.toJson())
    }
}

class ChromiaRellPracticesHelpStrategy : BaseToolStrategy() {
    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellPracticesHelp.toJson())
    }
}
