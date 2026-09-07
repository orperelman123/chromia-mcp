package org.chromia.tools

import dev.langchain4j.data.segment.TextSegment
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import org.chromia.domain.BlockchainFilters
import org.chromia.domain.ChromiaRepository
import org.chromia.domain.NetworkResult
import org.chromia.domain.PaginationParams
import org.chromia.domain.SortingParams

interface ToolStrategy {
    suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult

    /**
     * True when running this tool acts on the machine the server runs on, or uses
     * a private key. Concretely: it reads or writes anything outside a scratch
     * directory built solely from the caller's own arguments, starts or controls a
     * process or an embedded node, or signs/spends with a key from the environment
     * or the deploy keystore.
     *
     * This is the whole definition of the `public` profile
     * ([ToolProfiles.PUBLIC]): a no-auth connector URL is a URL anyone who has it
     * can call, so every marked tool is refused there. The property is abstract
     * here AND deliberately left unimplemented on [BaseToolStrategy], so a
     * strategy added later does not compile until its author decides - a new tool
     * cannot slip into the public surface by omission. [ToolProfiles.PUBLIC_DISABLED]
     * pins the resulting set, and ToolProfilesTest asserts the two agree.
     *
     * The in-process Rell compiler tools (rell_check, rell_security_check,
     * run_rell_tests, verify_guards) are false: they materialise only the caller's
     * own sources into a fresh temp directory (path traversal is rejected before
     * anything is written) and delete it afterwards. run_rell_tests additionally
     * wants a PostgreSQL; without one it refuses cleanly, which is what a public
     * deployment should leave it doing (see serve-public.ps1).
     */
    val touchesLocalMachine: Boolean
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
        "describe_tool" to DescribeToolStrategy {
            runCatching { org.chromia.App.effectiveDisabledTools() }.getOrElse { McpTools.disabledTools() }
        },
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
        "run_rell_tests" to RunRellTestsStrategy(),
        "verify_guards" to VerifyGuardsStrategy(),
        "local_chain_up" to LocalChainStrategy(),
        "translate_error" to TranslateErrorStrategy(),
        "onboarding_next_step" to OnboardingNextStepStrategy(),
        "verify_deployment" to VerifyDeploymentStrategy(),
        "deployment_preflight" to DeploymentPreflightStrategy(),
        "provision_testnet_container" to ProvisionTestnetContainerStrategy(),
        "claim_testnet_tchr" to ClaimTestnetTchrStrategy(),
        "deploy_testnet_chain" to DeployTestnetChainStrategy()
    )

    /**
     * Tool names whose strategy declares [ToolStrategy.touchesLocalMachine].
     * Derived from the live strategy table, so it cannot drift from the code;
     * [ToolProfiles.PUBLIC_DISABLED] is the pinned copy the server actually
     * applies, and ToolProfilesTest asserts the two are equal.
     */
    val localMachineToolNames: Set<String>
        get() = strategies.filterValues { it.touchesLocalMachine }.keys.toSortedSet()

    suspend fun executeTool(request: CallToolRequest): CallToolResult {
        val startedAt = System.nanoTime()
        val result = runCatching {
            val strategy = strategies[request.name]
                ?: return toolErrorResult("Unknown tool: ${request.name}")
                    .also { logToolCall(request.name, startedAt, ok = false) }
            unknownArgumentsError(request.name, request.argumentsOrEmpty.keys)?.let { message ->
                return toolErrorResult(message).also { logToolCall(request.name, startedAt, ok = false) }
            }
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
     * Argument names a strategy honours beyond its declared input schema.
     * Keep this list tiny and deliberate: an alias here is a promise that the
     * strategy reads it.
     */
    private val undeclaredArgumentAliases: Map<String, Set<String>> = mapOf(
        "write_deployment_config" to setOf("chain", "chromiaYml"),
        "scaffold_dapp" to setOf("notes_for"),
        // AUDIT F10: one canonical name per concept, every older spelling still
        // read. These are the ones the schema does not repeat - declaring four
        // synonyms per tool would cost every agent context for nothing.
        "check_dapp_project" to setOf("source"),
        "check_ft4_imports" to setOf("source"),
        "deployment_preflight" to setOf("source"),
        "deploy_testnet_chain" to setOf("source"),
        "run_rell_tests" to setOf("rell", "source"),
        "verify_guards" to setOf("rell", "source"),
        "rell_check" to setOf("rell"),
        "rell_security_check" to setOf("rell"),
        "local_chain_up" to setOf("rell", "source"),
        "get_blockchain_details" to setOf("rid"),
        "chromia_dapp_query" to setOf("blockchainRid")
    )

    /** Declared input-schema property names per tool (every tool, disabled ones included). */
    private val acceptedArguments: Map<String, Set<String>> by lazy {
        McpTools.allTools().associate { tool ->
            tool.name to (tool.inputSchema.propertiesOrEmpty.keys + undeclaredArgumentAliases[tool.name].orEmpty())
        }
    }

    /**
     * An argument the tool never declared is an argument it never reads, so a
     * call carrying one used to succeed while silently doing less than asked:
     * `module_args` (the spelling the description itself uses) on run_rell_tests
     * ran the tests WITHOUT the args and reported ok:true; `ttl` on local_chain_up
     * kept the default TTL; `include_iccf` on ft4_module_args answered without
     * ICCF; `dapp_name` on scaffold_dapp scaffolded "hello" (QA input-abuse lens
     * 2026-09-02). The MCP SDK does not validate arguments against the schema,
     * so reject them here with the declared names and a nearest-match hint.
     * Returns null when every argument is declared (or a deliberate alias).
     */
    internal fun unknownArgumentsError(toolName: String, argumentNames: Collection<String>): String? {
        val accepted = acceptedArguments[toolName] ?: return null
        val unknown = argumentNames.filter { it !in accepted }
        if (unknown.isEmpty()) return null
        val described = unknown.joinToString(", ") { name ->
            val suggestion = suggestArgument(name, accepted)
            if (suggestion != null) "`$name` (did you mean `$suggestion`?)" else "`$name`"
        }
        val declared = if (accepted.isEmpty()) "this tool declares no arguments"
        else "declared arguments: ${accepted.sorted().joinToString(", ")}"
        return "Unknown argument(s) for $toolName: $described - $declared. " +
            "Undeclared arguments are never honoured, so the call would have silently ignored them; rename or remove them."
    }

    /**
     * Names that mean the same thing across this server's tools. The tools are
     * not uniform - verify_deployment and the explorer tools say `brid`/`rid`,
     * chromia_dapp_query says `blockchainRid` and `arguments` - so an agent
     * carrying a name from the previous call gets no hint from spelling
     * distance alone (DX audit 2026-09-04: `brid` on chromia_dapp_query got
     * the declared list and nothing else, five probes in a row).
     */
    private val argumentSynonyms: List<Set<String>> = listOf(
        setOf("brid", "rid", "blockchainrid", "blockchainid", "chainrid", "chainid", "dapprid"),
        setOf("args", "arguments", "params", "parameters", "queryargs"),
        setOf("url", "nodeurl", "node", "endpoint", "apiurl", "network"),
        setOf("source", "code", "rell", "src"),
        setOf("yaml", "yml", "chromiayml", "config"),
        setOf("name", "chain", "blockchain", "dappname"),
    )

    internal fun suggestArgument(name: String, accepted: Set<String>): String? {
        fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
        val n = norm(name)
        if (n.isEmpty()) return null
        accepted.firstOrNull { norm(it) == n }?.let { return it }
        accepted.firstOrNull { norm(it).contains(n) || n.contains(norm(it)) }?.let { return it }
        val family = argumentSynonyms.firstOrNull { n in it } ?: return null
        return accepted.firstOrNull { norm(it) in family }
    }

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
/**
 * A successful tool answer.
 *
 * `isError = false` is stated rather than left to the default: kotlin-sdk 0.7.7
 * defaulted [CallToolResult.isError] to `false`, 0.15 defaults it to `null`, and
 * the field is serialized as it is set. Leaving it null would silently drop
 * `"isError": false` from every success this server has ever sent - a wire change
 * for any client that reads the field rather than its absence (the live
 * provisioning probes read it, and caught this). Saying it keeps the answer
 * identical across the upgrade.
 */
internal fun toolSuccessResult(data: JsonObject): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(Json.encodeToString(data))),
        structuredContent = data,
        isError = false
    )

internal fun toolErrorResult(message: String): CallToolResult =
    CallToolResult(
        content = listOf(TextContent(message)),
        structuredContent = buildJsonObject { put("error", message) },
        isError = true
    )

/**
 * translate_error rules whose match means the REMOTE side failed on a valid
 * call: an explorer incident, its testnet refusal, rate limiting, 5xx. Schema
 * drift and argument errors are deliberately absent - those are ours.
 */
internal val UPSTREAM_RULE_IDS = setOf("graphql_internal_error", "explorer_testnet_400", "http_rate_limited", "http_unavailable")

/**
 * An explorer/node failure, with the upstream verdict inline when the
 * translator can give one. The explorer answered `GraphQL Error:
 * INTERNAL_ERROR for <uuid>` on every aggregation field for hours on
 * 2026-09-04 (bisected live: `__typename` and `totalRewardsPaid` fine, every
 * `dashboardData` sub-field failing) and the tools relayed the opaque line
 * alone; the "not your fault, go chain-direct" verdict sat in translate_error,
 * one more call an agent had to know to make. Text keeps the original message
 * first (callers and tests match on it); `upstream`, `upstream_rule` and
 * `next_action` let a script branch without parsing prose.
 */
internal fun upstreamAwareErrorResult(message: String): CallToolResult {
    val translation = runCatching { ErrorTranslator.translate(message) }.getOrNull()
    val ruleId = translation?.ruleId?.takeIf { it in UPSTREAM_RULE_IDS } ?: return toolErrorResult(message)
    val full = "$message\nUPSTREAM ($ruleId): ${translation.meaning} ${translation.nextAction}"
    return CallToolResult(
        content = listOf(TextContent(full)),
        structuredContent = buildJsonObject {
            put("error", full)
            put("upstream", true)
            put("upstream_rule", ruleId)
            put("next_action", translation.nextAction)
        },
        isError = true
    )
}

/**
 * Sane ceiling for explorer pagination: the tool schemas default to 10-50
 * per page, so an absurdly large limit is an agent mistake worth flagging
 * rather than forwarding (QA finding).
 */
internal const val MAX_PAGINATION_LIMIT = 1000

/**
 * Extracts a {path: source} map from a `files` argument. Non-string values
 * (e.g. {"content": ...} objects) are returned as invalid keys instead of
 * silently dropped - dropping them made the tool analyze a partial project
 * without notice (audit 2026-09-01).
 */
/**
 * The Rell sources of a call, under any of this server's spellings.
 *
 * AUDIT F10 (2026-09-06): `source` (rell_check, rell_security_check), `rell`
 * (check_dapp_project, check_ft4_imports, deployment_preflight,
 * deploy_testnet_chain) and `files` (8 tools) all meant the same thing, so an
 * agent carrying a name from its previous call paid a wasted round trip per
 * tool. `files` is canonical - it had the majority - and the rest are accepted
 * with no warning and no preference. A single source STRING under any of them
 * is filed the way [placeSingleSource] files it.
 */
internal fun extractRellSourcesArg(args: Map<String, Any>): Any? =
    RELL_SOURCE_ALIASES.firstNotNullOfOrNull { key -> args[key]?.takeIf { it !is JsonNull } }

/**
 * Every spelling of "the Rell sources", canonical first. The order is the
 * PRECEDENCE, and [rellSourceAliasConflict] makes sure it is never the thing
 * that decides what gets analysed.
 *
 * The SET comes from [ArgumentVocabulary], which round 16 found was "a
 * schema-ordering lint and is not called at runtime" - so a spelling added
 * there and forgotten here would be a silently discarded alias all over again.
 * A name the vocabulary knows and this order does not is appended rather than
 * dropped.
 */
private val RELL_SOURCE_PRECEDENCE = listOf("files", "rell", "source", "src", "code")

internal val RELL_SOURCE_ALIASES: List<String> = run {
    val known = ArgumentVocabulary.RELL_SOURCES.all
    RELL_SOURCE_PRECEDENCE.filter { it in known } + (known - RELL_SOURCE_PRECEDENCE.toSet()).sorted()
}

/**
 * TWO ALIASES WITH DIFFERENT CONTENT ARE AN ERROR, NOT A PRECEDENCE.
 *
 * ROUND 16, on the audit-F10 alias chain itself: `args["files"] ?: args["rell"]
 * ?: args["source"] ...` returns the FIRST one present and drops the rest
 * without a word. Passing `files` with clean code and `source` with the tree
 * actually being shipped returned `ok:true` about a tree the tool never saw -
 * on `rell_check`, `rell_security_check`, `run_rell_tests` AND `verify_guards`,
 * the four tools an agent's security claim rests on. Aliases exist so an agent
 * carrying a name from its last call is not punished for it; they were never a
 * way to say two different things in one call, and an agent that sent both
 * meant SOMETHING - which of the two it meant is not ours to guess.
 *
 * Identical content under two names is exactly the harmless case aliases are
 * for and stays silent. Comparison is on the CANONICAL file map each spelling
 * produces, so `files={"main.rell": X}` and `source=X` are the same submission.
 */
internal fun rellSourceAliasConflict(args: Map<String, Any>, tool: String): String? {
    val present = RELL_SOURCE_ALIASES.mapNotNull { key ->
        canonicalRellSourceArg(args[key])?.let { key to it }
    }
    if (present.size < 2 || present.map { it.second }.distinct().size == 1) return null
    val names = present.map { it.first }
    return "$tool was given the Rell sources twice, under `${names.joinToString("` and `")}`, " +
        "with DIFFERENT content. These are aliases for one argument, so one of them would be " +
        "silently discarded and the answer would be about a tree you did not send. Pass the " +
        "sources once - `files` is the canonical name ({\"src/main.rell\": \"module; ...\"}) - " +
        "or, if these are different parts of one project, merge them into a single `files` map."
}

/** The {path: source} map a single alias value stands for, as a comparable string. */
private fun canonicalRellSourceArg(value: Any?): String? {
    if (value == null || value is JsonNull) return null
    val single = when {
        value is String -> value
        value is JsonPrimitive && value.isString -> value.content
        else -> null
    }
    if (single != null) {
        val files = linkedMapOf<String, String>()
        placeSingleSource(single, files)
        return canonicalFileMap(files, emptyList())
    }
    if (value is JsonPrimitive) return "prim:" + value.content
    if (value is JsonObject) {
        val (files, invalid) = extractRellFilesMap(value)
        if (files.isEmpty() && invalid.isEmpty()) return null
        return canonicalFileMap(files, invalid)
    }
    return "raw:" + value.toString()
}

private fun canonicalFileMap(files: Map<String, String>, invalid: List<String>): String =
    (files.entries.map { it.key to it.value } + invalid.map { it to "<non-string>" })
        .sortedBy { it.first }
        .joinToString(" | ") { "${it.first} => ${it.second}" }

internal fun extractRellFilesMap(filesArg: Any?): Pair<LinkedHashMap<String, String>, List<String>> {
    val files = linkedMapOf<String, String>()
    val invalid = mutableListOf<String>()
    if (filesArg is JsonObject) {
        filesArg.forEach { (path, content) ->
            if (content is JsonPrimitive && content.isString) files[path] = content.content
            else invalid.add(path)
        }
    }
    return files to invalid
}

/** A Rell file's first statement: `module;`, `@test module;`, `@mount("x") module;`. */
private val RELL_HEADER_REGEX = Regex("""(?m)^\s*(?:@\w+(?:\([^)]*\))?\s+)*module\s*;""")
/** A chromia.yml root key. */
private val YAML_ROOT_KEY_REGEX = Regex("""(?m)^(?:blockchains|compile|deployments|libs|database|test)\s*:""")

private val OWN_IMPORT_REGEX = Regex("""(?m)^\s*import\s+(?:\w+\s*:\s*)?([A-Za-z_][\w.]*)\s*(?:\.\{[^}]*\})?\s*;""")

/**
 * Places a single `source` argument in the files map. An app module is
 * main.rell. A `@test module` is NOT: filed as main.rell, `import main;`
 * resolved to the test file itself and every `main.x` came back as "Unknown
 * name: 'main.x'" - twenty errors about a file that was never submitted (DX
 * audit 2026-09-04, rell_security_check on the stablecoin test module alone).
 * A test module goes to test/main_test.rell, and the returned note names the
 * own modules it imports that have to be passed alongside it via `files`.
 */
internal fun placeSingleSource(source: String, files: LinkedHashMap<String, String>): String? {
    if (!RunRellTests.isTestModuleSource(source)) {
        files["main.rell"] = source
        return null
    }
    files["test/main_test.rell"] = source
    val own = OWN_IMPORT_REGEX.findAll(maskRellSource(source, maskStrings = true))
        .map { it.groupValues[1] }
        .filter { !it.startsWith("lib.") && !it.startsWith("^") && !it.startsWith("rell.") }
        .distinct().toList()
    return "`source` is a @test module, placed at test/main_test.rell. " +
        (if (own.isEmpty()) "It imports no module of its own, so it was compiled alone."
        else "It imports ${own.joinToString(", ")} - not submitted, so every name from ${
            if (own.size == 1) "it" else "them"} is unresolved. Pass `files` with the app module(s) AND the test file," +
            " e.g. {\"main.rell\": ..., \"test/main_test.rell\": ...}.")
}

abstract class BaseToolStrategy : ToolStrategy {
    /**
     * Absent and JSON-null mean "not provided". Primitive values coerce via
     * their content (numeric timestamps as strings are relied on), but an
     * object or array used to be silently serialized to its JSON text - so
     * `searchQuery: {"q": "CHR"}` searched the literal `{"q":"CHR"}` and
     * returned zero matches as success (audit round 6 F2). Now a validation
     * error, consistent with the other extractors.
     */
    protected fun extractString(arguments: Map<String, Any>, key: String): String? {
        val value = arguments[key] ?: return null
        return when (value) {
            is JsonNull -> null
            is String -> value
            is JsonPrimitive -> value.content
            else -> throw IllegalArgumentException(
                "$key must be a string; got ${describeJsonValue(value)}"
            )
        }
    }

    /**
     * Absent and JSON-null mean "not provided". A present value of any other
     * JSON type used to be silently ignored, so `system: "yes"` returned the
     * unfiltered result as success (audit round 4 F1) - it is now a validation
     * error, consistent with [extractPagination].
     */
    protected fun extractBoolean(arguments: Map<String, Any>, key: String): Boolean? {
        val value = arguments[key] ?: return null
        if (value is JsonNull) return null
        val parsed = when (value) {
            is Boolean -> value
            is JsonPrimitive -> value.booleanOrNull
            is String -> value.toBooleanStrictOrNull()
            else -> null
        }
        return parsed ?: throw IllegalArgumentException(
            "$key must be a boolean (true or false); got ${describeJsonValue(value)}"
        )
    }

    /**
     * Pagination guard: negative/zero limits and negative offsets used to be
     * forwarded to the explorer, which answers with an opaque
     * "GraphQL Error: INTERNAL_ERROR for <uuid>" (QA finding). Fail locally
     * with an actionable message instead. Malformed values (non-numeric,
     * out of range) used to be silently dropped by the old extractInt helper, returning
     * unpaginated results with no hint the argument was ignored (QA finding);
     * a present-but-invalid value is now a validation error.
     */
    protected fun extractPagination(arguments: Map<String, Any>): org.chromia.domain.PaginationParams {
        val limit = extractPaginationValue(arguments, "limit")
        val offset = extractPaginationValue(arguments, "offset")
        require(limit == null || limit > 0) { "limit must be a positive integer (got $limit)" }
        require(limit == null || limit <= MAX_PAGINATION_LIMIT) {
            "limit must not exceed $MAX_PAGINATION_LIMIT (got $limit)"
        }
        require(offset == null || offset >= 0) { "offset must be zero or a positive integer (got $offset)" }
        return org.chromia.domain.PaginationParams(limit = limit?.toInt(), offset = offset?.toInt())
    }

    /**
     * Absent and JSON-null values mean "not provided"; anything else must be
     * an integer, and one that fits in an Int (the explorer takes GraphQL
     * Int) - otherwise the caller gets a clear validation error instead of a
     * silently ignored argument.
     */
    private fun extractPaginationValue(arguments: Map<String, Any>, key: String): Long? {
        val value = arguments[key] ?: return null
        if (value is JsonNull) return null
        val raw = if (value is JsonPrimitive) value.content else value.toString()
        val parsed = raw.trim().toLongOrNull()
        require(parsed != null) { "$key must be an integer (got \"$raw\")" }
        require(parsed in Int.MIN_VALUE..Int.MAX_VALUE) { "$key is out of range (got $parsed)" }
        return parsed
    }

    /**
     * Rejects an inverted time window before it reaches the explorer
     * (QA finding). Understands the formats agents actually send: epoch
     * numbers and ISO-8601 strings (the tool schema documents "ISO format" -
     * previously ISO values were silently skipped because only
     * [String.toLongOrNull] was tried, QA finding). Ordering is enforced only
     * when both bounds parse as the same format; mixed or malformed values
     * are passed through for the explorer to report - never thrown on.
     */
    protected fun requireOrderedTimestamps(from: String?, to: String?) {
        if (from == null || to == null) return
        val numericFrom = from.trim().toLongOrNull()
        val numericTo = to.trim().toLongOrNull()
        if (numericFrom != null && numericTo != null) {
            require(numericFrom <= numericTo) {
                "timestampFrom ($from) must not be later than timestampTo ($to)"
            }
            return
        }
        if (numericFrom != null || numericTo != null) return // mixed formats: let the explorer decide
        val isoFrom = parseIsoTimestamp(from)
        val isoTo = parseIsoTimestamp(to)
        require(isoFrom == null || isoTo == null || isoFrom <= isoTo) {
            "timestampFrom ($from) must not be later than timestampTo ($to)"
        }
    }

    private fun parseIsoTimestamp(value: String): java.time.Instant? {
        val v = value.trim()
        val parsers = listOf<(String) -> java.time.Instant>(
            { java.time.Instant.parse(it) },
            { java.time.OffsetDateTime.parse(it).toInstant() },
            { java.time.LocalDateTime.parse(it).toInstant(java.time.ZoneOffset.UTC) },
            { java.time.LocalDate.parse(it).atStartOfDay(java.time.ZoneOffset.UTC).toInstant() },
        )
        return parsers.firstNotNullOfOrNull { parse -> runCatching { parse(v) }.getOrNull() }
    }

    /**
     * Absent and JSON-null mean "not provided" (no filter). A present value of
     * any non-array JSON type used to be silently ignored, so `brids: "XYZ"`
     * dropped the filter and returned network-wide data as success (audit
     * round 4 F1) - it is now a validation error, consistent with
     * [extractPagination] and [extractStringMap]. A present-but-empty array
     * (`brids: []`, or one whose entries are all null/blank) used to collapse
     * to null the same silent way - network-wide data as filtered success - and
     * null/blank ENTRIES were silently dropped, shortening the filter. Both are
     * now validation errors: omit the parameter to mean "no filter", and every
     * entry must be a non-empty string.
     */
    protected fun extractStringList(arguments: Map<String, Any>, key: String): List<String>? {
        val raw = arguments[key] ?: return null
        if (raw is JsonNull) return null
        // JsonArray is a List<JsonElement>, so one branch covers both shapes.
        val elements = raw as? List<*> ?: throw IllegalArgumentException(
            "$key must be an array of strings; got ${describeJsonValue(raw)}"
        )
        if (elements.isEmpty()) {
            throw IllegalArgumentException(
                "$key is an empty array; omit the parameter entirely to mean no filter"
            )
        }
        return elements.mapIndexed { idx, item ->
            val text = when {
                item == null || item is JsonNull -> throw IllegalArgumentException(
                    "$key[$idx] is null; entries must be non-empty strings"
                )
                item is String -> item
                item is JsonPrimitive && item.isString -> item.content
                // A non-string entry used to coerce via toString(), so
                // excludeAccounts: [{"accountId": ".."}] became the literal
                // filter text {"accountId":".."} and matched nothing - wrong
                // filtered-looking data as success (audit round 6 F1).
                else -> throw IllegalArgumentException(
                    "$key[$idx] must be a string; got ${describeJsonValue(item)}"
                )
            }.trim()
            text.ifEmpty {
                throw IllegalArgumentException("$key[$idx] is blank; entries must be non-empty strings")
            }
        }
    }

    /** Human-readable JSON type of a wrong-typed argument, for validation errors. */
    protected fun describeJsonValue(value: Any): String = when (value) {
        is JsonNull -> "null"
        is JsonPrimitive -> when {
            value.isString -> "a string (\"${value.content.take(40)}\")"
            value.booleanOrNull != null -> "a boolean (${value.content})"
            else -> "a number (${value.content})"
        }
        is JsonArray, is List<*> -> "an array"
        is JsonObject, is Map<*, *> -> "an object"
        is String -> "a string (\"${value.take(40)}\")"
        is Boolean -> "a boolean ($value)"
        is Number -> "a number ($value)"
        else -> value::class.simpleName ?: "an unsupported value"
    }

    protected fun extractStringMap(arguments: Map<String, Any>, key: String): Map<String, String>? {
        val raw = arguments[key] ?: return null
        if (raw is JsonNull) return null
        if (raw is String || (raw is JsonPrimitive && raw.isString)) {
            val text = extractString(arguments, key) ?: return null
            // A bare string is a real source file. Keying it "rell" (no .rell
            // suffix) made check_dapp_project's compile+security gate silently
            // skip it and report ok=true on unaudited code (audit 2026-09-01).
            return mapOf("main.rell" to text)
        }
        val entries = when (raw) {
            is Map<*, *> -> raw.entries
            is JsonObject -> raw.entries
            else -> throw IllegalArgumentException(
                "`$key` must be an object mapping .rell file paths to source strings (got ${raw::class.simpleName})"
            )
        }
        // Invalid entries used to be silently dropped, so the tool analyzed a
        // partial project - and when EVERY value was invalid the caller was told
        // the parameter was missing when it was sent (audit 2026-09-01). Same
        // treatment as extractRellFilesMap: name the offending keys.
        val out = linkedMapOf<String, String>()
        val invalid = mutableListOf<String>()
        entries.forEach { (k, v) ->
            val path = (k?.toString() ?: "").trim()
            val content = when (v) {
                is String -> v
                is JsonPrimitive -> if (v.isString) v.content else null
                else -> null
            }
            if (path.isEmpty() || content == null) invalid.add(path.ifEmpty { "(empty path)" }) else out[path] = content
        }
        require(invalid.isEmpty()) {
            "`$key` values must be source strings keyed by file path; invalid entr${if (invalid.size == 1) "y" else "ies"} at: ${invalid.joinToString(", ")}"
        }
        require(out.isNotEmpty()) { "`$key` was provided but is an empty object - pass at least one file, e.g. {\"main.rell\": \"module; ...\"}" }
        return out
    }

    protected fun handleResult(result: NetworkResult<JsonObject>, errorMessage: String): CallToolResult {
        return when (result) {
            is NetworkResult.Success -> toolSuccessResult(result.data)
            is NetworkResult.Error -> upstreamAwareErrorResult("$errorMessage: ${result.message}")
        }
    }

    /**
     * The canonical name first, then any aliases, and BLANK COUNTS AS MISSING at
     * every position. The F10 alias chains were written as
     * `extractString(args, "brid") ?: extractString(args, "rid") ?: requireParameter(args, "brid")`,
     * which reads the blank check out of the code entirely: `{"rid": "   "}` made
     * an earlier arm return a present-but-blank string, so the require arm never
     * ran and a blank chain id reached the repository as if it were real
     * (ToolExecutorTest.executeToolBlankRidIsMissingRequiredParameterAndIsError,
     * ToolExecutorStrategiesTest.getBlockchainDetailsBlankRidThrows). Passing the
     * aliases here keeps one blank check for the whole chain, and the failure
     * message names the canonical spelling first so an agent learns it, while
     * still listing the aliases so the name the caller actually typed appears.
     */
    protected fun requireParameter(
        arguments: Map<String, Any>,
        key: String,
        vararg aliases: String
    ): String {
        for (name in listOf(key) + aliases) {
            val value = extractString(arguments, name)?.takeIf { it.isNotBlank() }
            if (value != null) return value
        }
        val alsoAccepted =
            if (aliases.isEmpty()) "" else " (also accepted: ${aliases.joinToString(", ")})"
        throw IllegalArgumentException("Missing required parameter: $key$alsoAccepted")
    }

    /**
     * Explicit JSON nulls are preserved as Kotlin null everywhere (top-level
     * keys, array elements, object members) so they reach the chain as GtvNull.
     * They used to be silently dropped, which made a Rell parameter with a
     * default (`filter: text? = "active"`) use the default instead of null,
     * and shortened arrays ([1,null,2] -> [1,2]) (audit round 4 F2).
     *
     * A wrong-typed `arguments` value (a JSON-encoded STRING of the object -
     * the classic agent mistake - or an array) used to map to emptyMap()
     * silently, running the query with NO arguments: wrong-but-plausible
     * results whenever the Rell query has parameter defaults. It is now a
     * validation error, consistent with the sibling extract helpers.
     *
     * Lives on the base class because [DappInteractionStrategy] and
     * [VerifyDeploymentStrategy] take the same `arguments` shape.
     */
    protected fun extractArgumentsMap(arguments: Map<String, Any>, key: String): Map<String, Any?> {
        val raw = arguments[key] ?: return emptyMap()
        if (raw is JsonNull) return emptyMap()
        return when (raw) {
            is Map<*, *> -> {
                val stringMap = mutableMapOf<String, Any?>()
                raw.forEach { (k, v) ->
                    if (k == null) return@forEach
                    stringMap[k.toString()] = extractPrimitiveValue(v)
                }
                stringMap
            }
            else -> {
                val hint =
                    if (raw is String || (raw is JsonPrimitive && raw.isString)) " - do not JSON-encode it" else ""
                throw IllegalArgumentException(
                    "$key must be an object mapping parameter names to values; got ${describeJsonValue(raw)}$hint"
                )
            }
        }
    }

    private fun extractPrimitiveValue(value: Any?): Any? {
        return when (value) {
            null, is JsonNull -> null
            is JsonPrimitive -> {
                when {
                    value.isString -> value.content
                    value.booleanOrNull != null -> value.boolean
                    value.intOrNull != null -> value.int
                    value.longOrNull != null -> value.long
                    // GTV has no float type: a bare decimal or an integer past
                    // Long range used to be forwarded as a Double and die in
                    // postchain with the opaque "Cannot convert object of type
                    // Double to GTV". Fail locally with an actionable message.
                    value.doubleOrNull != null -> throw IllegalArgumentException(
                        if (value.content.contains('.') || value.content.contains('e', ignoreCase = true)) {
                            "numeric argument ${value.content} is not an integer; send decimals as strings (e.g. \"${value.content}\")"
                        } else {
                            "numeric argument ${value.content} does not fit a 64-bit integer; send it as a string"
                        }
                    )
                    else -> value.content
                }
            }
            is JsonArray -> value.map { extractPrimitiveValue(it) }
            is JsonObject -> {
                val map = mutableMapOf<String, Any?>()
                value.forEach { (k, v) ->
                    map[k] = extractPrimitiveValue(v)
                }
                map
            }
            else -> value
        }
    }
}

class PromptsToolStrategy(private val promptManager: PromptManager) : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return runCatching {
            val args = request.argumentsOrEmpty as Map<String, Any>
            val category = extractString(args, "category")
            val tool = extractString(args, "tool")
            val search = extractString(args, "search")

            val categories = promptManager.getCategories()
            // `Dapp_Build` / ` chr ` is not a different category.
            val resolvedCategory = category?.trim()?.let { asked ->
                categories.firstOrNull { it.equals(asked, ignoreCase = true) } ?: asked
            }
            val allPrompts = if (resolvedCategory != null) {
                mapOf(resolvedCategory to promptManager.getPromptsForCategory(resolvedCategory))
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
                // An unknown category used to answer {"prompts":{}} with the valid
                // names only inside `statistics` (DX audit 2026-09-04) - say it.
                if (resolvedCategory != null && resolvedCategory !in categories) {
                    val near = categories.filter { it.contains(resolvedCategory, ignoreCase = true) || resolvedCategory.contains(it, ignoreCase = true) }
                    put(
                        "notes",
                        "No prompt category named '$resolvedCategory'. Valid categories: ${categories.joinToString(", ")}." +
                            (if (near.isNotEmpty()) " Did you mean ${near.joinToString(" or ") { "'$it'" }}?" else "") +
                            " Use `search` to match prompt text across every category."
                    )
                }
            }

            toolSuccessResult(result)
        }.getOrElse { e ->
            toolErrorResult("Failed to get prompts: ${e.message}")
        }
    }
}

class BlockchainsTransactionsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getBlockchainsTransactions(network)
        return handleResult(result, "Failed to get blockchains transactions")
    }
}

class TransactionsByClusterStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getTransactionsByCluster(network)
        return handleResult(result, "Failed to get transactions by cluster")
    }
}

class AllAssetsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getAllAssets(network)
        return handleResult(result, "Failed to get all assets")
    }
}

class TotalRewardsPaidStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getTotalRewardsPaid(network)
        return handleResult(result, "Failed to get total rewards paid")
    }
}

class AssetDistributionStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
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
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val assetId = requireParameter(args, "assetId")
        val network = extractString(args, "network")
        // Same validation as extractPagination: a malformed limit used to be
        // silently dropped ("limit":"abc" -> unlimited) and an absurd limit was
        // forwarded uncapped (audit 2026-09-01).
        val limit = extractPagination(args).limit

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
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val brid = requireParameter(args, "brid")
        val network = extractString(args, "network")
        val fromTimestamp = extractString(args, "fromTimestamp")

        val result = repository.getBlockchainAnalytics(brid, network, fromTimestamp)
        return handleResult(result, "Failed to get blockchain analytics")
    }
}

class BlockchainDetailsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        // AUDIT F10: `brid` is the canonical chain id on this server (the name
        // verify_deployment and chromia.yml both use); `rid` still works.
        val rid = requireParameter(args, "brid", "rid")
        val network = extractString(args, "network")

        val result = repository.getBlockchainDetails(rid, network)
        return handleResult(result, "Failed to get blockchain details")
    }
}

class MonthlyActiveAccountsPerChainStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val brid = requireParameter(args, "brid")
        val network = extractString(args, "network")
        val untilTimestamp = extractString(args, "untilTimestamp")

        val result = repository.getMonthlyActiveAccountsPerChain(brid, network, untilTimestamp)
        return handleResult(result, "Failed to get monthly active accounts per chain")
    }
}

class AllTransactionsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        requireOrderedTimestamps(extractString(args, "timestampFrom"), extractString(args, "timestampTo"))
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
            pagination = extractPagination(args),
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
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getAllOperations(network)
        return handleResult(result, "Failed to get all operations")
    }
}

class FilterAssetsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        val filters = org.chromia.domain.AssetSearchFilters(
            brid = extractString(args, "brid"),
            searchQuery = extractString(args, "searchQuery"),
            type = extractString(args, "type"),
            pagination = extractPagination(args),
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
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")
        val includeTotals = extractBoolean(args, "includeTotals") ?: true
        val includeGroupedDeposits = extractBoolean(args, "includeGroupedDeposits") ?: true
        val includeGroupedWithdrawals = extractBoolean(args, "includeGroupedWithdrawals") ?: true
        val full = extractBoolean(args, "full") ?: false

        val result = repository.getChrAggregates(
            network,
            includeTotals,
            includeGroupedDeposits,
            includeGroupedWithdrawals
        )
        if (full) return handleResult(result, "Failed to get CHR aggregates")
        return when (result) {
            is NetworkResult.Success -> toolSuccessResult(summarizeChrAggregates(result.data))
            is NetworkResult.Error -> toolErrorResult("Failed to get CHR aggregates: ${result.message}")
        }
    }
}

/** Default array cap for the summarized get_chr_aggregates response. */
internal const val CHR_AGGREGATES_ARRAY_CAP = 50

/**
 * Summarized get_chr_aggregates shape: totals and every scalar are kept
 * verbatim, but any array (the per-address groupedDeposits/groupedWithdrawals
 * breakdowns) is capped at its first [cap] entries. The uncapped mainnet
 * response was observed at ~808KB (hosted probe 2026-09-01) - far past what an
 * agent context can absorb by default. When anything was truncated a top-level
 * `note` names each truncated array and points at full:true.
 */
internal fun summarizeChrAggregates(data: JsonObject, cap: Int = CHR_AGGREGATES_ARRAY_CAP): JsonObject {
    val truncated = mutableListOf<String>()
    fun capElement(element: JsonElement, path: String): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.mapValues { (key, value) -> capElement(value, if (path.isEmpty()) key else "$path.$key") }
        )
        is JsonArray -> {
            if (element.size > cap) truncated += "$path: first $cap of ${element.size} entries"
            JsonArray(element.take(cap).map { capElement(it, path) })
        }
        else -> element
    }
    val capped = capElement(data, "") as JsonObject
    if (truncated.isEmpty()) return capped
    return JsonObject(
        capped + mapOf(
            "note" to JsonPrimitive(
                "Summarized response - ${truncated.joinToString("; ")}. Pass full:true for the complete response."
            )
        )
    )
}

class AssetBlockchainsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val assetId = requireParameter(args, "assetId")
        val network = extractString(args, "network")

        val result = repository.getAssetBlockchains(network, assetId)
        return handleResult(result, "Failed to get asset blockchains")
    }
}

class SignerBlockchainsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val signer = requireParameter(args, "signer")
        val network = extractString(args, "network")

        val result = repository.getSignerBlockchains(network, signer)
        return handleResult(result, "Failed to get signer blockchains")
    }
}

class AccountBlockchainsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val accountId = requireParameter(args, "accountId")
        val network = extractString(args, "network")

        val result = repository.getAccountBlockchains(accountId, network)
        return handleResult(result, "Failed to get account blockchains")
    }
}

class NodeUnavailabilityStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val pubkey = requireParameter(args, "pubkey")
        val startTimestamp = requireParameter(args, "startTimestamp")
        val network = extractString(args, "network")

        val result = repository.getNodeUnavailability(pubkey, startTimestamp, network)
        return handleResult(result, "Failed to get node unavailability")
    }
}

class NetworkStatsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        val result = repository.getNetworkStats(network)
        return handleResult(result, "Failed to get network stats")
    }
}

class FilterBlockchainsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")

        val filters = BlockchainFilters(
            rid = extractString(args, "rid"),
            name = extractString(args, "name"),
            cluster = extractString(args, "cluster"),
            container = extractString(args, "container"),
            state = extractString(args, "state"),
            system = extractBoolean(args, "system"),
            pagination = extractPagination(args),
            sorting = SortingParams(
                sortBy = extractString(args, "sortBy"),
                sortDirection = extractString(args, "sortDirection")
            )
        )

        val result = repository.filterBlockchains(network, filters)
        return handleResult(result, "Failed to get all blockchains")
    }
}

class DappInteractionStrategy(
    /**
     * Overall wall-clock deadline for the blocking query, client construction
     * (signer discovery) included; clamped. CI run 33601190754: a query
     * against a chain the predefined system nodes do not serve made
     * postchain-client crawl all ~14 endpoints at up to 60s each, outliving
     * even the e2e sweep's 240s rpc timeout and surfacing as a transport
     * error instead of an answer. A healthy live query answers in ~1-2s, so
     * the default leaves ample headroom. See [ProbeBudget].
     */
    deadlineMs: Long? = null
) : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    private val deadlineMs: Long = ProbeBudget.clampDeadlineMs(
        deadlineMs ?: ProbeBudget.configuredDeadlineMs(ProbeBudget.QUERY_DEADLINE_ENV)
    )

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = extractString(args, "network")
        // AUDIT F10: `brid` is the canonical chain id; `blockchainRid` still works.
        val blockchainRid = requireParameter(args, "brid", "blockchainRid")
        val queryName = extractString(args, "query")
        val arguments = extractArgumentsMap(args, "arguments")

        val result = ProbeBudget.withBudget(deadlineMs) {
            repository.executeCustomQuery(
                network,
                BlockchainRid.buildFromHex(blockchainRid),
                queryName,
                arguments
            )
        } ?: return toolErrorResult(ProbeBudget.queryTimeoutHint(network, deadlineMs))

        // A node's "Invalid argument(s): account" names the wrong name and not
        // the right one; the chain publishes its signatures, so ask it (one
        // extra read, only on that class of refusal - live stablecoin chain,
        // 2026-09-04). Never let the follow-up hide the original error.
        if (result is NetworkResult.Error && queryName != "rell.get_app_structure" && QuerySignatureHint.applies(result.message)) {
            val structure = ProbeBudget.withBudget(deadlineMs) {
                repository.executeCustomQuery(network, BlockchainRid.buildFromHex(blockchainRid), "rell.get_app_structure", emptyMap())
            } as? NetworkResult.Success
            val hint = structure?.let { QuerySignatureHint.hint(it.data, queryName ?: "", arguments.keys) }
            if (hint != null) {
                return toolErrorResult("Failed to execute dapp query $queryName --> $arguments: ${result.message}. $hint")
            }
        }

        return handleResult(result, "Failed to execute dapp query $queryName --> $arguments")
    }

}

class FetchDocsStrategy(private val ragStoreDeferred: Deferred<RagStore>) : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val query = requireParameter(args, "query")

        return runCatching {
            val ragStore = ragStoreDeferred.await()
            val queried = ragStore.query(query)
                ?: return docsIndexUnavailableResult("hits") // null = index missing, not "no match"
            val hits = queried.takeIf { it.isNotEmpty() }
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
                // A stale index answers confidently from an old release; say so on
                // every hit list rather than only in a boot log nobody reads.
                val staleNote = ragStore.staleWarning()
                val result = formatFetchDocsText(hits) + (staleNote?.let { "\n\nNOTE: $it" } ?: "")
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
                        if (staleNote != null) put("index_note", staleNote)
                        // Origin/age of the index on every answer, so a deploy can be
                        // verified over the wire; the text stays lean for the agent.
                        ragStore.provenance?.let { put("index", it.toJson()) }
                    },
                    // Stated, not defaulted - see toolSuccessResult.
                    isError = false
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
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val query = requireParameter(args, "query")

        return runCatching {
            val ragStore = ragStoreDeferred.await()
            val segments = ragStore.query(query)
                ?: return docsIndexUnavailableResult("results") // null = index missing, not "no match"
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
            // AUDIT F2: `search` and `fetch` are the ChatGPT-contract pair every
            // agent reaches for first, and they used to return {id,title,url} with
            // no word about the index behind them - so a 320-day-old fragment was
            // quoted as current API. The one tool that told the truth (fetch_docs)
            // is the one an agent is least likely to call. Same `index` object and
            // the same STALE note, on all three.
            val staleNote = ragStore.staleWarning()
            val payload = buildJsonObject {
                put("results", results)
                if (staleNote != null) put("index_note", staleNote)
                ragStore.provenance?.let { put("index", it.toJson()) }
            }
            CallToolResult(
                content = listOf(TextContent(Json.encodeToString(payload))),
                structuredContent = payload,
                // Stated, not defaulted - see toolSuccessResult.
                isError = false
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
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val id = normalizeSegmentId(requireParameter(args, "id"))

        return runCatching {
            val ragStore = ragStoreDeferred.await()
            val segment = ragStore.fetchById(id)
            if (segment == null && ragStore.isIndexUnavailable()) {
                // Index never loaded: a miss is not "unknown id" (audit round 4 F3).
                val payload = buildJsonObject {
                    put("id", id)
                    put("error", DOCS_INDEX_UNAVAILABLE_MESSAGE)
                }
                return CallToolResult(
                    content = listOf(TextContent(Json.encodeToString(payload))),
                    structuredContent = payload,
                    isError = true
                )
            }
            val staleNote = ragStore.staleWarning()
            val payload = if (segment != null) {
                buildJsonObject {
                    put("id", id)
                    put("title", segmentTitle(segment))
                    put("text", segment.text())
                    put("url", segmentUrl(segment))
                    // AUDIT F2: the text an agent is about to quote carries the age
                    // of the index it came from, in the same shape fetch_docs uses.
                    if (staleNote != null) put("index_note", staleNote)
                    ragStore.provenance?.let { put("index", it.toJson()) }
                }
            } else {
                buildJsonObject {
                    put("id", id)
                    put("error", "Documentation not found for requested id")
                    if (staleNote != null) put("index_note", staleNote)
                    ragStore.provenance?.let { put("index", it.toJson()) }
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
 * Docs index unavailable (embeddings never loaded - e.g. the GitLab download
 * failed at startup). RagStore retries the load with a cooldown (audit F5);
 * until then the tools must say so instead of a misleading "not found".
 */
internal const val DOCS_INDEX_UNAVAILABLE_MESSAGE =
    "Documentation index is unavailable (embeddings could not be loaded); " +
        "the server retries loading periodically - try again shortly."

internal fun docsIndexUnavailableResult(emptyArrayField: String): CallToolResult {
    return CallToolResult(
        content = listOf(TextContent(DOCS_INDEX_UNAVAILABLE_MESSAGE)),
        structuredContent = buildJsonObject {
            put("text", DOCS_INDEX_UNAVAILABLE_MESSAGE)
            put(emptyArrayField, buildJsonArray {})
        },
        isError = true
    )
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
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val name = extractString(args, "name")
        val template = extractString(args, "template") ?: "hello"
        val notesFor = extractString(args, "notesFor") ?: extractString(args, "notes_for")
        val payload = DappScaffold.toJson(name, template, notesFor)
        // AUDIT F6: an unknown template with no close shipped match scaffolds
        // NOTHING, and says so the way every other tool here says it - ok:false
        // AND isError, so an agent that checks either one stops.
        val ok = (payload["ok"] as? JsonPrimitive)?.content != "false"
        return CallToolResult(
            content = listOf(TextContent(Json.encodeToString(payload))),
            structuredContent = payload,
            isError = if (ok) null else true
        )
    }
}



class ValidateChromiaYmlStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val yaml = requireParameter(args, "yaml")
        // strict=true turns missing production pins (compile.rellVersion,
        // merkle_hash_version) into errors; the default warns, since chr
        // builds official configs that omit them (real-world round 2 D3).
        val strict = extractBoolean(args, "strict") ?: false
        // AUDIT F7: with the sources, the validator can see what the yml is
        // MISSING - a module that declares `struct module_args` the yml never
        // sets, or an FT4 import with no lib.ft4.core.accounts configuration.
        // Without them it behaves exactly as before and guesses nothing.
        val (files, invalid) = extractRellFilesMap(args["files"] ?: args["rell"])
        if (invalid.isNotEmpty()) {
            return toolErrorResult(
                "`files` values must be Rell source strings; non-string value(s) at: ${invalid.joinToString(", ")}"
            )
        }
        val single = extractString(args, "source")
        val rell = LinkedHashMap(files).also { map ->
            if (map.isEmpty() && single != null) map["main.rell"] = single
        }
        return toolSuccessResult(ChromiaYmlValidator.validate(yaml, strict, rell).toJson())
    }
}

/**
 * Gateway over the static help/INDEX strategies: one `chromia_help(topic)` schema
 * instead of ~30 individual tool schemas in the agent's context. With no or an
 * unknown topic it returns the topic index. With a topic it returns that topic's
 * TABLE OF CONTENTS - section names and the bytes each costs - because audit F9
 * measured one topic at 27,288 tokens, four times what compact mode saves in
 * total. `section` returns one section; section:"all" returns the whole payload,
 * byte-identical to calling the individual *_help tool.
 */
class ChromiaHelpStrategy(private val helpStrategies: Map<String, ToolStrategy>) : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false


    internal companion object {
        /**
         * What agents actually ask for vs what the topic is named: "security" and
         * "best practices" both live in chromia_rell_practices_help (probe finding
         * 2026-09-01 - those spellings fell through to the unknown-topic index).
         */
        val TOPIC_ALIASES = mapOf(
            "security" to "chromia_rell_practices_help",
            "best_practices" to "chromia_rell_practices_help",
            "best-practices" to "chromia_rell_practices_help"
        )

        /** `section` value that asks for the whole payload - nothing is unreachable. */
        const val WHOLE_PAYLOAD = "all"

        /**
         * A value bigger than this is listed in the table of contents by its own
         * parts instead, so an agent can spend a paragraph rather than a chapter.
         * Also the hard cut on one section: nothing this gateway offers as a
         * section costs more than this plus its wrapper - measured by
         * ChromiaHelpSectionTest, which prints the worst on every run.
         */
        const val SECTION_SPLIT_BYTES = 6_000

        /** How deep the splitting recursion goes before it hands over a big leaf. */
        private const val MAX_SPLIT_DEPTH = 4

        /** Characters of a section's own text shown in the table of contents. */
        private const val PREVIEW_CHARS = 60

        /** Serialized byte cost of a JSON value - what the agent actually pays. */
        internal fun byteSize(element: JsonElement): Int = element.toString().toByteArray().size

        /** One addressable section: what to ask for, and what it costs. */
        internal data class Section(val name: String, val value: JsonElement, val preview: String?)

        /** [text] cut to at most [bytes] UTF-8 bytes, never mid-character. */
        private fun cutToBytes(text: String, bytes: Int): String {
            var cut = text
            while (cut.toByteArray().size > bytes) cut = cut.dropLast(1)
            return cut
        }

        /**
         * A long string split at line boundaries into pieces that each fit
         * [SECTION_SPLIT_BYTES]; a single line longer than that is cut to fit
         * rather than handed over whole, so a section always has a bound.
         */
        private fun splitText(text: String): List<String> {
            val parts = mutableListOf<String>()
            val current = StringBuilder()
            fun flush() {
                if (current.isNotEmpty()) {
                    parts += current.toString()
                    current.setLength(0)
                }
            }
            text.lines().forEach { line ->
                var rest = line
                while (rest.toByteArray().size > SECTION_SPLIT_BYTES) {
                    flush()
                    val head = cutToBytes(rest, SECTION_SPLIT_BYTES)
                    parts += head
                    rest = rest.substring(head.length)
                }
                val candidate = if (current.isEmpty()) rest else current.toString() + "\n" + rest
                if (candidate.toByteArray().size > SECTION_SPLIT_BYTES) {
                    flush()
                    current.append(rest)
                } else {
                    current.setLength(0)
                    current.append(candidate)
                }
            }
            flush()
            return parts.ifEmpty { listOf(text) }
        }

        private fun previewOf(element: JsonElement): String? {
            val text = when {
                element is JsonPrimitive && element.isString -> element.content
                element is JsonObject -> element.keys.joinToString(", ")
                element is JsonArray -> element.joinToString(", ") { it.toString() }
                else -> return null
            }.replace(Regex("\\s+"), " ").trim()
            return if (text.length <= PREVIEW_CHARS) text.ifEmpty { null }
            else text.take(PREVIEW_CHARS) + "..."
        }

        private fun collect(name: String, value: JsonElement, depth: Int, out: MutableList<Section>) {
            if (byteSize(value) <= SECTION_SPLIT_BYTES || depth >= MAX_SPLIT_DEPTH) {
                out += Section(name, value, previewOf(value))
                return
            }
            when {
                value is JsonObject && value.isNotEmpty() ->
                    value.forEach { (key, child) -> collect("$name.$key", child, depth + 1, out) }

                value is JsonArray && value.isNotEmpty() -> {
                    var index = 1
                    val group = mutableListOf<JsonElement>()
                    fun flushGroup() {
                        if (group.isNotEmpty()) {
                            collect("$name.${index++}", JsonArray(group.toList()), MAX_SPLIT_DEPTH, out)
                            group.clear()
                        }
                    }
                    value.forEach { element ->
                        if (byteSize(element) > SECTION_SPLIT_BYTES) {
                            flushGroup()
                            collect("$name.${index++}", element, depth + 1, out)
                        } else {
                            group += element
                            if (byteSize(JsonArray(group.toList())) > SECTION_SPLIT_BYTES) {
                                val last = group.removeLast()
                                flushGroup()
                                group += last
                            }
                        }
                    }
                    flushGroup()
                }

                value is JsonPrimitive && value.isString -> {
                    val parts = splitText(value.content)
                    if (parts.size == 1) {
                        out += Section(name, JsonPrimitive(cutToBytes(parts.first(), SECTION_SPLIT_BYTES)), previewOf(value))
                    } else {
                        parts.forEachIndexed { i, part ->
                            val piece = JsonPrimitive(part)
                            out += Section("$name.${i + 1}", piece, previewOf(piece))
                        }
                    }
                }

                else -> out += Section(name, value, previewOf(value))
            }
        }

        /**
         * Addressable sections of [payload], in payload order. A key whose value
         * costs more than [SECTION_SPLIT_BYTES] is listed by its own parts -
         * sub-keys, array chunks, or paragraphs of a long string - so every
         * section an agent can ask for has a bound.
         */
        internal fun sections(payload: JsonObject): List<Section> {
            val out = mutableListOf<Section>()
            payload.forEach { (key, value) -> collect(key, value, depth = 1, out = out) }
            return out
        }

        /** [payload]'s table of contents: every section with the bytes it costs. */
        internal fun tableOfContents(topic: String, payload: JsonObject, note: String?): JsonObject {
            val listed = sections(payload)
            return buildJsonObject {
                put("topic", JsonPrimitive(topic))
                put("bytes", JsonPrimitive(byteSize(payload)))
                put("sections", buildJsonArray {
                    listed.forEach { section ->
                        add(buildJsonObject {
                            put("section", JsonPrimitive(section.name))
                            put("bytes", JsonPrimitive(byteSize(section.value)))
                            section.preview?.takeIf { section.name.contains('.') }
                                ?.let { put("preview", JsonPrimitive(it)) }
                        })
                    }
                })
                put(
                    "notes",
                    (note?.plus(" ") ?: "") +
                        "This is the table of contents, not the payload: call " +
                        "chromia_help{topic:\"$topic\", section:\"<section>\"} for one section, or " +
                        "section:\"$WHOLE_PAYLOAD\" for all ${byteSize(payload)} bytes."
                )
            }
        }
    }

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val requested = extractString(args, "topic")?.trim()?.lowercase()
        val section = extractString(args, "section")?.trim()
        val rawTopic = requested?.let { TOPIC_ALIASES[it] ?: it }
        // Accept both "chr_build" and "chr_build_help" spellings.
        val topic = rawTopic?.let { if (it in helpStrategies) it else "${it}_help".takeIf { t -> t in helpStrategies } }

        // A tool name is a topic too: F9 moved the long form of the security tools
        // out of tools/list, and it has to stay reachable by the name agents know.
        if (topic == null && rawTopic != null && rawTopic in ToolDocs.TOPICS) {
            return toolSuccessResult(buildJsonObject {
                put("topic", JsonPrimitive(rawTopic))
                put("tool", JsonPrimitive(rawTopic))
                put("description", JsonPrimitive(McpTools.fullDescription(rawTopic).orEmpty()))
                put(
                    "notes",
                    "The full description of the `$rawTopic` tool, moved out of tools/list to keep " +
                        "the catalog small (audit F9). describe_tool{tool:\"$rawTopic\"} returns the " +
                        "same text with the tool's input schema."
                )
            })
        }

        if (topic == null) {
            val index = buildJsonObject {
                put("topics", buildJsonArray { McpTools.HELP_TOPIC_NAMES.sorted().forEach { add(JsonPrimitive(it)) } })
                put(
                    "notes",
                    (if (rawTopic == null) "Pass one of these topics; you get its table of contents, then a section. "
                    else "Unknown topic '$rawTopic'. ") +
                        "Topic names map 1:1 to the full help catalog (CLI commands, chromia.yml, Rell language, " +
                        "FT4, deploy, integrations), plus a tool name for that tool's full description."
                )
            }
            return toolSuccessResult(index)
        }

        val delegated = helpStrategies.getValue(topic).execute(request, repository)
        val payload = delegated.structuredContent
        // Only a structured payload can be sectioned; anything else passes through.
        if (payload == null || delegated.isError == true) return delegated

        if (section == null) return toolSuccessResult(tableOfContents(topic, payload, note = null))
        if (section.equals(WHOLE_PAYLOAD, ignoreCase = true)) return delegated

        val match = sections(payload).firstOrNull { it.name.equals(section, ignoreCase = true) }
            ?: return toolSuccessResult(tableOfContents(topic, payload, note = "Unknown section '$section'."))
        return toolSuccessResult(buildJsonObject {
            put("topic", JsonPrimitive(topic))
            put("section", JsonPrimitive(match.name))
            put("bytes", JsonPrimitive(byteSize(match.value)))
            put(match.name, match.value)
        })
    }
}

/**
 * `describe_tool`: the full description and untrimmed schemas of one tool.
 * F9 measured `tools/list` at 115,908 B (~29k tokens) before an agent's first
 * useful call; the long form of the biggest descriptions was MOVED here, never
 * deleted, and ToolDescriptionBudgetTest pins that every sentence that left a
 * description is still reachable by name.
 */
class DescribeToolStrategy(private val disabled: () -> Set<String> = { emptySet() }) : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val name = extractString(args, "tool") ?: extractString(args, "name")
        val includeSchema = extractBoolean(args, "includeSchema") ?: true
        return toolSuccessResult(McpTools.describeToolJson(name, includeSchema, disabled()))
    }
}

class RellCheckStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        rellSourceAliasConflict(args, "rell_check")?.let { return toolErrorResult(it) }
        val source = extractString(args, "source")
        val filesArg = args["files"] ?: args["rell"]
        val modules = extractStringList(args, "modules")

        val (files, invalidKeys) = extractRellFilesMap(filesArg)
        if (invalidKeys.isNotEmpty()) {
            return toolErrorResult(
                "`files` values must be Rell source strings; non-string value(s) at: ${invalidKeys.joinToString(", ")}"
            )
        }
        val sourceNote = if (source != null && files.isEmpty()) placeSingleSource(source, files) else null
        if (files.isEmpty()) {
            return toolErrorResult(
                "rell_check needs Rell code: pass `source` (single main.rell) or `files` ({\"path.rell\": \"code\"})"
            )
        }

        return runCatching {
            val result = withContext(Dispatchers.IO) {
                val checked = RellCheck.check(files, modules)
                val noted = if (sourceNote == null || checked.ok) checked else checked.copy(notes = "$sourceNote ${checked.notes}")
                with(RellCheck) { noted.toJson() }
            }
            toolSuccessResult(result)
        }.getOrElse { e ->
            toolErrorResult("rell_check failed: ${e.message}")
        }
    }
}

class RellSecurityCheckStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        rellSourceAliasConflict(args, "rell_security_check")?.let { return toolErrorResult(it) }
        val source = extractString(args, "source")
        val filesArg = args["files"] ?: args["rell"]
        val allowAdminModules = extractBoolean(args, "allowAdminModules") ?: false

        val (files, invalidKeys) = extractRellFilesMap(filesArg)
        if (invalidKeys.isNotEmpty()) {
            return toolErrorResult(
                "`files` values must be Rell source strings; non-string value(s) at: ${invalidKeys.joinToString(", ")}"
            )
        }
        val sourceNote = if (source != null && files.isEmpty()) placeSingleSource(source, files) else null
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
                        // Compile notes carry load-bearing context (e.g. "Using your
                        // submitted lib/ft4 sources ...") - dropping them misattributed
                        // errors to the vendored tree (audit F1 follow-up).
                        put(
                            "notes",
                            "Code does not compile - fix rell_check errors first, then re-run the security check. " +
                                (sourceNote?.let { "$it " } ?: "") + compile.notes
                        )
                    }
                )
            }
            toolSuccessResult(with(RellSecurityCheck) { analyze(files, allowAdminModules).toJson() })
        }.getOrElse { e ->
            toolErrorResult("rell_security_check failed: ${e.message}")
        }
    }
}

class RunRellTestsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        rellSourceAliasConflict(args, "run_rell_tests")?.let { return toolErrorResult(it) }
        val filesArg = extractRellSourcesArg(args)
        val (submitted, invalidKeys) = extractRellFilesMap(filesArg)
        if (invalidKeys.isNotEmpty()) {
            return toolErrorResult(
                "`files` values must be Rell source strings; non-string value(s) at: ${invalidKeys.joinToString(", ")}"
            )
        }
        // AUDIT F4: scaffold_dapp hands back chromia.yml IN `files`, and an agent
        // that forwards what it was given used to be told "Only .rell files are
        // supported". Take the yml out of the map and read the module args from
        // it instead - `chr test` merges moduleArgs with test.moduleArgs, and so
        // does this, so scaffold_dapp -> run_rell_tests needs no hand-merging.
        val yamlFromFiles = submitted.entries
            .firstOrNull { (path, _) ->
                val name = path.substringAfterLast('/').substringAfterLast('\\').lowercase()
                name == "chromia.yml" || name == "chromia.yaml"
            }
        val files = LinkedHashMap(submitted).also { map -> yamlFromFiles?.let { map.remove(it.key) } }
        val yaml = extractString(args, "yaml") ?: yamlFromFiles?.value
        if (files.isEmpty()) {
            return toolErrorResult(
                "run_rell_tests needs a `files` map including at least one `@test module;` file, e.g. {\"main.rell\": \"module; ...\", \"tests/main_test.rell\": \"@test module; ...\"}"
            )
        }
        // module_args by module name, e.g. {"lib.ft4.core.accounts": {...}} - required
        // to exercise real FT4 operations in tests. Malformed shapes used to be
        // silently ignored (non-object -> no args, per-module non-object -> {}),
        // so tests ran without the args the agent sent (audit 2026-09-01).
        val moduleArgsArg = args["moduleArgs"]
        val moduleArgs = when (moduleArgsArg) {
            null, is JsonNull -> emptyMap()
            is JsonObject -> {
                val badModules = moduleArgsArg.filterValues { it !is JsonObject }.keys
                if (badModules.isNotEmpty()) {
                    return toolErrorResult(
                        "moduleArgs value for module(s) ${badModules.joinToString(", ")} must be an args object " +
                            "(e.g. {\"rate_limit\": {...}}) - got a non-object value. Do not JSON-encode the args."
                    )
                }
                moduleArgsArg.mapValues { (_, v) -> (v as JsonObject).toMap() }
            }
            else -> {
                val got = if (moduleArgsArg is JsonPrimitive && moduleArgsArg.isString) {
                    "a string - do not JSON-encode it"
                } else {
                    "${moduleArgsArg::class.simpleName}"
                }
                return toolErrorResult(
                    "moduleArgs must be an object mapping module name -> args object " +
                        "(e.g. {\"lib.ft4.core.accounts\": {\"rate_limit\": {...}}}); got $got."
                )
            }
        }

        // `tests`: an array of patterns, or - what agents actually type - one name
        // or a comma list as a string. Anything else is named by position.
        val tests = when (val testsArg = args["tests"]) {
            null, is JsonNull -> emptyList()
            is JsonPrimitive -> {
                if (!testsArg.isString) return toolErrorResult("`tests` must be an array of test-name patterns (or one comma-separated string); got ${testsArg.content}")
                testsArg.content.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            }
            is JsonArray -> {
                val bad = testsArg.withIndex().firstOrNull { (_, v) -> v !is JsonPrimitive || !v.isString }
                if (bad != null) {
                    return toolErrorResult("`tests[${bad.index}]` must be a string pattern such as test_x or *inflation*; got ${bad.value}")
                }
                testsArg.map { it.jsonPrimitive.content.trim() }.filter { it.isNotEmpty() }
            }
            else -> return toolErrorResult("`tests` must be an array of test-name patterns; got ${testsArg::class.simpleName}")
        }

        // THE PRECEDENCE, PINNED (round 16). Two readings of "moduleArgs plus a
        // yaml" were possible and the code shipped a third: `moduleArgs.isEmpty()`
        // made `moduleArgs: {}` - an explicit "run with NO module args" - the
        // same call as sending none at all, so the yml's args ran instead and
        // the run was configured by a file the caller may only have passed for
        // its `.rell` neighbours.
        //
        // The rule now is the one an agent cannot get wrong: THE ARGUMENT YOU
        // PASSED IS THE ARGUMENT THAT RUNS. If `moduleArgs` is present at all -
        // `{}` included - it is the WHOLE set and the yaml contributes nothing.
        // The yaml is read only when `moduleArgs` is absent or null. Per-key
        // merging was the alternative and it is the one that goes wrong
        // silently: setting one module's args would inherit every other
        // module's from a file, and no answer the tool returns would say so.
        val moduleArgsProvided = moduleArgsArg != null && moduleArgsArg !is JsonNull
        val derived = if (!moduleArgsProvided && yaml != null) ChromiaYmlModuleArgs.merged(yaml) else emptyMap()
        val effectiveModuleArgs = if (moduleArgsProvided) moduleArgs else derived

        return runCatching {
            val result = withContext(Dispatchers.IO) {
                with(RunRellTests) { run(files, moduleArgs = effectiveModuleArgs, tests = tests).toJson() }
            }
            val annotated = if (derived.isNotEmpty()) {
                buildJsonObject {
                    result.forEach { (k, v) ->
                        if (k == "notes") {
                            put(
                                k,
                                (v as? JsonPrimitive)?.content.orEmpty() +
                                    " moduleArgs were read from the chromia.yml you passed" +
                                    (if (yamlFromFiles != null) " in `files`" else "") +
                                    ": blockchains.<name>.moduleArgs merged with test.moduleArgs, " +
                                    "for ${derived.keys.sorted().joinToString(", ")}. Pass an explicit " +
                                    "`moduleArgs` object to override."
                            )
                        } else {
                            put(k, v)
                        }
                    }
                    put("moduleArgsSource", "chromia.yml")
                    put(
                        "moduleArgsUsed",
                        buildJsonObject {
                            derived.forEach { (m, a) -> put(m, buildJsonObject { a.forEach { (k, v) -> put(k, v) } }) }
                        }
                    )
                }
            } else if (moduleArgsProvided && yaml != null) {
                // THE YAML WAS THERE AND WAS NOT USED. Silence here is what let
                // `moduleArgs: {}` mean two different things; the answer says
                // which set the run actually had.
                buildJsonObject {
                    result.forEach { (k, v) ->
                        if (k == "notes") {
                            put(
                                k,
                                (v as? JsonPrimitive)?.content.orEmpty() +
                                    " You passed BOTH an explicit `moduleArgs` and a chromia.yml" +
                                    (if (yamlFromFiles != null) " in `files`" else "") +
                                    ", and the explicit argument is the whole set: the run used " +
                                    (
                                        if (moduleArgs.isEmpty()) "NO module args at all (`moduleArgs: {}` means none, not \"whatever the yml says\")"
                                        else "your ${moduleArgs.keys.sorted().joinToString(", ")} and nothing from the yml"
                                        ) +
                                    ". Omit `moduleArgs` entirely to have the yml's " +
                                    "blockchains.<name>.moduleArgs and test.moduleArgs used instead."
                            )
                        } else {
                            put(k, v)
                        }
                    }
                    put("moduleArgsSource", "moduleArgs argument")
                    put(
                        "moduleArgsUsed",
                        buildJsonObject {
                            moduleArgs.forEach { (m, a) -> put(m, buildJsonObject { a.forEach { (k, v) -> put(k, v) } }) }
                        }
                    )
                }
            } else {
                result
            }
            toolSuccessResult(annotated)
        }.getOrElse { e ->
            toolErrorResult("run_rell_tests failed: ${e.message}")
        }
    }
}

/**
 * verify_guards: the mutant discipline this repository holds its own templates
 * to, exported to the agent's dapp.
 *
 * Every shipped template guard has a test that proves it is LOAD-BEARING: the
 * must-fail test passes with the guard present, and once the guard is removed
 * it fails BECAUSE THE ATTACK LANDED - not because the mutant stopped
 * compiling, not because module_args went missing, not because some other
 * guard still refused the transaction. Agents copy the templates' must-fail
 * tests and write their own, and nothing checked that a copied test would go
 * red if its guard vanished. A test that passes in both states is a fake green
 * with a security label on it; one was written here, by the maintainers, and
 * only a mutant caught it.
 *
 * So the tool runs exactly that check. For each {guard, test}: the test must
 * pass on the submitted files (else `baseline_red` - it proves nothing in
 * either state); the guard must exist verbatim (else `guard_not_found`); the
 * guard is replaced (default: deleted) and ONLY that test is run. Verdicts:
 *   load_bearing          the test failed because the attack landed
 *   vacuous               the test stayed green with the guard gone
 *   still_refused         another guard refused it - name it in alsoRemove
 *   environmental         the mutant is not a running dapp (compile, module_args)
 *   red_for_another_reason red, but not the attack - read the error
 * `ok` is true only when every guard is load_bearing.
 */
class VerifyGuardsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    private val environmentalFragments = listOf("Unable to create GTX module", "do not compile", "Missing metadata")

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        rellSourceAliasConflict(args, "verify_guards")?.let { return toolErrorResult(it) }
        val (files, invalidKeys) = extractRellFilesMap(extractRellSourcesArg(args))
        if (invalidKeys.isNotEmpty()) {
            return toolErrorResult(
                "`files` values must be Rell source strings; non-string value(s) at: ${invalidKeys.joinToString(", ")}"
            )
        }
        if (files.isEmpty()) {
            return toolErrorResult(
                "verify_guards needs a `files` map with the production module AND the `@test module;` file that holds the must-fail test"
            )
        }
        val moduleArgs: Map<String, Map<String, JsonElement>> = when (val m = args["moduleArgs"]) {
            null -> emptyMap()
            is JsonObject -> {
                val bad = m.filterValues { it !is JsonObject }.keys
                if (bad.isNotEmpty()) {
                    return toolErrorResult("moduleArgs value for module(s) ${bad.joinToString(", ")} must be an args object")
                }
                m.mapValues { (_, v) -> (v as JsonObject).toMap() }
            }
            else -> return toolErrorResult("moduleArgs must be an object mapping module name -> args object")
        }
        val guardsArg = args["guards"] as? JsonArray
            ?: return toolErrorResult(
                "verify_guards needs `guards`: an array of {guard: <the guard line, verbatim>, test: <the must-fail test that " +
                    "depends on it>, replacement?: <text to put in its place, default: delete it>, alsoRemove?: [<other guards " +
                    "that would still refuse the attack>], stillRefused?: <error fragment meaning the attack was refused>, " +
                    "attackLanded?: <error fragment proving the attack succeeded, default \"did not fail\">}"
            )
        if (guardsArg.isEmpty()) {
            return toolErrorResult("`guards` is empty - name at least one guard line and the must-fail test that depends on it")
        }

        return runCatching {
            val verdicts = mutableListOf<JsonObject>()
            for ((idx, g) in guardsArg.withIndex()) {
                val obj = g as? JsonObject ?: return toolErrorResult("guards[$idx] must be an object")
                val guard = (obj["guard"] as? JsonPrimitive)?.content
                    ?: return toolErrorResult("guards[$idx].guard is required: the guard line, verbatim")
                val test = (obj["test"] as? JsonPrimitive)?.content
                    ?: return toolErrorResult("guards[$idx].test is required: the must-fail test that depends on this guard")
                val replacement = (obj["replacement"] as? JsonPrimitive)?.content ?: ""
                val alsoRemove = (obj["alsoRemove"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content } ?: emptyList()
                val stillRefused = (obj["stillRefused"] as? JsonPrimitive)?.content
                val attackLanded = (obj["attackLanded"] as? JsonPrimitive)?.content ?: "did not fail"
                verdicts += verifyOne(files, moduleArgs, guard, replacement, alsoRemove, test, stillRefused, attackLanded)
            }
            val proven = verdicts.count { (it["loadBearing"] as? JsonPrimitive)?.content == "true" }
            toolSuccessResult(
                buildJsonObject {
                    put("ok", proven == verdicts.size)
                    put("guards", verdicts.size)
                    put("loadBearing", proven)
                    put("results", JsonArray(verdicts))
                    put(
                        "notes",
                        "ok=true means EVERY named guard is load-bearing: its must-fail test passes with the guard present and " +
                            "fails WITHOUT it because the attack landed. Any other verdict - vacuous, baseline_red, " +
                            "environmental, still_refused, ambiguous_refusal, red_for_another_reason, guard_not_found - " +
                            "means the test proves " +
                            "nothing about that guard yet. This is the standard the shipped templates are held to (every " +
                            "guard has a mutant that reddens a must-fail test because the attack lands). It does not " +
                            "replace an audit, and it says nothing about guards you did not name."
                    )
                }
            )
        }.getOrElse { e -> toolErrorResult("verify_guards failed: ${e.message}") }
    }

    /**
     * Round 11 attacked this tool the round it shipped and got six wrong answers
     * out of it, four of them ok:true. Each was one omission:
     *   - the guard was searched in RAW text, so a comment quoting the guard was a
     *     "production hit" (deleted, nothing changed: false vacuous) and a block
     *     comment's terminator was a "guard" whose deletion commented out the real
     *     one (false load_bearing);
     *   - replace() hit EVERY copy in the file, so a dead duplicate certified the
     *     live line;
     *   - "test file" was narrower than the runner's rule, so a header-less
     *     sibling of a @test module was mutated as production code;
     *   - alsoRemove could strip a REAL guard alongside a vacuous named one, and
     *     the resulting red was credited to the named guard;
     *   - attackLanded was a free substring, so a caller could make the guard's
     *     own refusal read as the attack succeeding.
     * The protocol below closes each: comments are masked before searching
     * (maskRellSource is length-preserving, so the match offset maps back to the
     * original text and exactly ONE occurrence is replaced); more than one
     * occurrence in a file is refused as ambiguous; test ownership follows the
     * runner - a file belongs to a test module if its module name is one a
     * @test file declares; a CONTROL run strips only alsoRemove and must still
     * pass, or the named guard proved nothing; and an error that contains any
     * string literal from the guard line is the guard REFUSING, whatever
     * fragment the caller supplied.
     *
     * ROUND 16 REPLACED THE HEURISTIC WITH TWO SHAPES. Rounds 11 to 15 each
     * answered the round before - the guard's literals, then the replacement's,
     * then "a refused transaction is never the attack landing", then "compare
     * the runner's failing frame with the declaration the guard lives in", then
     * the test's statement ORDER with a truncated re-run to decide it - and each
     * was beaten one token away by the next: an `@extend` the call graph could
     * not see, a `for` loop the statement splitter could not read, a frame-less
     * query refusal past an empty statement list. The error text a runner
     * happens to print is not a specification, so step 4 no longer tries to
     * classify every red. It names the two TEST SHAPES it can prove, proves
     * those exactly, and answers `ambiguous_refusal` - naming the shape to
     * write - for everything else.
     *
     * WHAT THE TOOL KNOWS: the guard's FILE, its OFFSET, the declaration it
     * sits in and that file's MODULE (derived the way the RUNNER derives it),
     * plus the whole submission - so it can read the production call graph and
     * the test's own statements.
     * WHAT THE RUNNER GIVES: one error string per failing case, whose frames
     * are `[<module>:<declaration>(<file>:<line>)]`.
     *
     * THE GUARD'S OWN DECLARATIONS are computed, not guessed: the declaration
     * the guard sits in, module-qualified, PLUS - when that is a `function` -
     * every operation and query that REACHES it through the submission's call
     * graph, transitively, following name calls AND `@extend`/`@extendable`
     * (Rell runs every `@extend` of an extendable function when the extendable
     * is called, and no call site anywhere names the extension).
     *
     * THE SHAPES. Both require the test to invoke one of those declarations in
     * EXACTLY ONE top-level statement - directly, or through one test-module
     * helper chain the scan can reduce to a single call site. That is the whole
     * point: "which invocation refused" is the question every beaten heuristic
     * was guessing at, and a test with one invocation cannot pose it.
     *
     *  SHAPE A, THE MUST-FAIL TEST. That one statement is a single-operation
     *  `rell.test.tx().op(<the declaration>(...)).run_must_fail(...)`. The
     *  guard is load-bearing only if removing it makes that transaction
     *  SUCCEED, which the runner reports as "did not fail". Any other red is
     *  the attack still being REFUSED - by the guard's own declaration
     *  (defence in depth: name it in alsoRemove), by anything else in the
     *  stack, or by a query whose literals own a frame-less message. There is
     *  no path from a refusal to `load_bearing` in shape A.
     *
     *  SHAPE B, THE MUST-HOLD TEST. That one statement expects the invocation
     *  to SUCCEED - a single-operation `.run()`, or a direct call to the query
     *  - and the damage is measured afterwards. Because it is the only
     *  statement in which anything this guard reaches executes, removing the
     *  guard cannot change what the test does BEFORE it. So: a rell.test
     *  assert_* failure is the damage being measured (`load_bearing`); a
     *  refusal frame naming one of the guard's own declarations is THAT
     *  invocation being refused (`still_refused`); a refusal frame naming
     *  anything else necessarily happened after the attack had landed
     *  (`load_bearing`); and a frame-less error - which is never an operation,
     *  since an operation refusal always carries a frame - is attributed to the
     *  test module or to a production query by the string LITERALS each owns,
     *  `still_refused` when that query is the guard's own, `ambiguous_refusal`
     *  when both or neither can produce the words.
     *
     * ANYTHING ELSE IS `ambiguous_refusal`, ok:false, with the two shapes
     * spelled out: a loop or a table-driven test, several invoking statements,
     * a transaction carrying more than the guard's own operation, a helper with
     * several call sites, a guard in no declaration at all. It is never
     * `still_refused` (which would send an author off to weaken a load-bearing
     * test) and never `load_bearing` (the direction that certifies a guard that
     * is not). Round 15's DIFFERENTIAL TRUNCATION - a second mutant run against
     * a cut-down test - is gone with the question it answered: it only ever
     * arose for tests that are not one of the shapes.
     *
     * The steps before this one are unchanged and still carry rounds 11 to 13:
     * comments and string literals are masked before searching (maskRellSource
     * is length-preserving, so the match offset maps back and exactly ONE
     * occurrence is replaced); more than one occurrence is refused as
     * ambiguous; test ownership follows the runner - a file belongs to a test
     * module if its module name is one a @test file declares; a CONTROL run
     * strips only alsoRemove and must still pass; and a replacement may not
     * change code outside the guard's own span.
     */
    private suspend fun verifyOne(
        files: Map<String, String>,
        moduleArgs: Map<String, Map<String, JsonElement>>,
        guard: String,
        replacement: String,
        alsoRemove: List<String>,
        test: String,
        stillRefused: String?,
        attackLanded: String
    ): JsonObject = withContext(Dispatchers.IO) {
        fun verdict(v: String, evidence: String, loadBearing: Boolean = false) = buildJsonObject {
            put("guard", guard)
            put("test", test)
            put("verdict", v)
            put("loadBearing", loadBearing)
            put("evidence", evidence)
        }
        if (guard.isBlank()) return@withContext verdict("guard_not_found", "the guard is blank")

        // TEST OWNERSHIP, the runner's way: every module a @test file declares is a
        // test module, and any file whose module name resolves to one of them -
        // including a header-less sibling - is test code. Plus anything under a
        // test/ or tests/ directory, as the security gate treats it.
        fun underTestDir(path: String): Boolean {
            val n = path.replace('\\', '/').removePrefix("./").removePrefix("src/")
            return n.startsWith("test/") || n.startsWith("tests/") || n.contains("/test/") || n.contains("/tests/")
        }
        // Module names the way the RUNNER derives them - relative to the Rell
        // source root, so src/main.rell is `main`, never `src.main`. Round 16:
        // every frame the runner emitted said `main:take` and the tool compared
        // it with `src.main:take`, so under the scaffold's own layout a guard
        // refused by its own operation was certified load_bearing, ok:true.
        fun runnerModule(path: String, src: String) =
            RunRellTests.moduleNameForPath(RellCheck.normalizeSourceRoot(path), src)
        val testModules = files.filterValues { RunRellTests.isTestModuleSource(it) }
            .map { (path, content) -> runnerModule(path, content) }.toSet()
        val isTest = files.mapValues { (path, src) ->
            RunRellTests.isTestModuleSource(src) || underTestDir(path) ||
                runnerModule(path, src) in testModules
        }
        // Every module a test file lives in, for telling the runner's frames apart.
        val testModuleNames = files.filter { (p, _) -> isTest.getValue(p) }
            .map { (p, src) -> runnerModule(p, src) }.toSet()

        // A MATCH COUNTS ONLY IF IT STARTS IN CODE. The raw text is searched (so a
        // caller may disambiguate a line with its trailing comment), but a match
        // whose first non-blank character sits inside a comment or a string
        // literal is not a guard: deleting a comment changes nothing (false
        // vacuous), deleting a block comment's terminator comments OUT the real
        // guard (false load_bearing), and a message table is not control flow.
        // maskRellSource is length-preserving, so the masked text is consulted
        // at the same offset: a code character survives masking unchanged.
        fun occurrences(text: String, needle: String): List<Int> {
            val out = mutableListOf<Int>()
            var from = 0
            while (true) {
                val i = text.indexOf(needle, from)
                if (i < 0) break
                out += i
                from = i + 1
            }
            return out
        }
        fun codeOccurrences(text: String, needle: String): List<Int> {
            val maskedAll = maskRellSource(text, maskStrings = true)
            return occurrences(text, needle).filter { i ->
                val k = (i until i + needle.length).firstOrNull { !text[it].isWhitespace() } ?: i
                maskedAll[k] == text[k]
            }
        }
        val hits = files.keys.filter { !isTest.getValue(it) }.associateWith { codeOccurrences(files.getValue(it), guard) }
            .filterValues { it.isNotEmpty() }
        val inTestOnly = files.keys.filter { isTest.getValue(it) && files.getValue(it).contains(guard) }
        val inProseOnly = files.keys.filter { !isTest.getValue(it) && files.getValue(it).contains(guard) }
        if (hits.isEmpty()) {
            return@withContext verdict(
                "guard_not_found",
                when {
                    inProseOnly.isNotEmpty() -> "the guard text appears in ${inProseOnly.joinToString()} only inside a COMMENT or a STRING LITERAL - that is not a guard, and deleting it proves nothing (or worse, comments out the real one). Name the code line"
                    inTestOnly.isNotEmpty() -> "the guard appears only in test code (${inTestOnly.joinToString()}), not in a production module - name the production line"
                    else -> "no submitted file contains the guard verbatim in code - the check proves nothing until the exact line is named"
                }
            )
        }
        if (hits.size > 1) {
            return@withContext verdict(
                "guard_ambiguous",
                "the guard appears in ${hits.size} production files (${hits.keys.joinToString()}) - include enough of the line to make it unique"
            )
        }
        val (path, offsets) = hits.entries.single()
        if (offsets.size > 1) {
            return@withContext verdict(
                "guard_ambiguous",
                "the guard appears ${offsets.size} times in $path - which copy is the live one is not the tool's guess to make; " +
                    "include enough of the surrounding line to name exactly one"
            )
        }
        fun stripAll(src: Map<String, String>, needles: List<String>): Map<String, String>? {
            val out = LinkedHashMap(src)
            for (r in needles) {
                val where = out.keys.filter { !isTest.getValue(it) }.filter { codeOccurrences(out.getValue(it), r).isNotEmpty() }
                if (where.isEmpty()) return null
                for (k in where) {
                    var text = out.getValue(k)
                    // remove every code occurrence, from the end so offsets stay valid
                    for (i in codeOccurrences(text, r).reversed()) text = text.substring(0, i) + text.substring(i + r.length)
                    out[k] = text
                }
            }
            return out
        }
        for (r in alsoRemove) {
            // Round 12: an alsoRemove entry that CONTAINS the guard (or is contained by
            // it) would strip the guard itself in the control run and report a
            // load-bearing guard as vacuous. The two must be disjoint text.
            if (r.contains(guard) || guard.contains(r)) {
                return@withContext verdict(
                    "also_remove_overlaps_guard",
                    "an alsoRemove entry overlaps the guard text - the control run strips alsoRemove with the guard KEPT, " +
                        "so an entry that contains the guard would remove it and prove the wrong thing. Name lines that are " +
                        "disjoint from the guard"
                )
            }
            if (files.keys.none { !isTest.getValue(it) && codeOccurrences(files.getValue(it), r).isNotEmpty() }) {
                return@withContext verdict("guard_not_found", "alsoRemove entry not found verbatim in production code: $r")
            }
        }

        fun runOnly(src: Map<String, String>) = runCatching { RunRellTests.run(src, moduleArgs = moduleArgs, tests = listOf(test)) }
        fun environmental(m: String) = environmentalFragments.any { f -> m.contains(f) }

        // 1. BASELINE: everything present, the test must PASS.
        val baseline = runOnly(files).getOrElse {
            val m = it.message.orEmpty()
            return@withContext if (environmental(m)) verdict("baseline_red", "the submitted files do not run as they are ($m) - fix that before verifying any guard")
            else verdict("runner_error", "baseline run failed: $m")
        }
        val baseCase = baseline.cases.singleOrNull { it.name.endsWith(test) }
            ?: return@withContext verdict(
                "test_not_found",
                "the baseline run returned ${baseline.cases.size} case(s) [${baseline.cases.joinToString { it.name }}] and none is $test - check the name; notes: ${baseline.notes}"
            )
        if (!baseCase.ok) {
            return@withContext verdict("baseline_red", "the test FAILS with the guard present (${baseCase.error}) - it proves nothing about the guard in either state. Fix the test until it passes on the real code")
        }

        // 2. CONTROL: strip ONLY alsoRemove, keep the named guard. If the test
        //    already fails here, the red that follows is theirs, not this guard's.
        if (alsoRemove.isNotEmpty()) {
            val control = stripAll(files, alsoRemove)
                ?: return@withContext verdict("guard_not_found", "an alsoRemove entry vanished between checks")
            val ctl = runOnly(control).getOrElse {
                return@withContext verdict("environmental", "the control (alsoRemove stripped, guard kept) is not a running dapp: ${it.message}")
            }
            val ctlCase = ctl.cases.singleOrNull { it.name.endsWith(test) }
            if (ctlCase != null && !ctlCase.ok) {
                return@withContext verdict(
                    "vacuous",
                    "with ONLY the alsoRemove lines stripped and this guard still present, the test already fails (${ctlCase.error}) - " +
                        "the lines in alsoRemove are what the test measures, not this guard. Name one of them as the guard instead"
                )
            }
        }

        // 3. MUTANT: replace exactly the one occurrence, strip alsoRemove, run.
        val mutated = LinkedHashMap(files)
        val src = mutated.getValue(path)
        val at = offsets.single()
        val mutatedSrc = src.substring(0, at) + replacement + src.substring(at + guard.length)
        run {
            val origMask = maskRellSource(src, maskStrings = true)
            val mutMask = maskRellSource(mutatedSrc, maskStrings = true)
            val prefixSame = origMask.substring(0, at) == mutMask.substring(0, at)
            val suffixSame = origMask.substring(at + guard.length) == mutMask.substring(at + replacement.length)
            if (!prefixSame || !suffixSame) {
                return@withContext verdict(
                    "replacement_rejected",
                    "the replacement changes code OUTSIDE the guard's own span - it opens or closes a comment or a string, so " +
                        "what every later line means has changed (round 12 used a replacement of \"/*\" to comment out the " +
                        "real guard and have a vacuous one credited). A mutation may alter only the line it names"
                )
            }
        }
        mutated[path] = mutatedSrc
        val withAlso = if (alsoRemove.isEmpty()) mutated else (stripAll(mutated, alsoRemove) ?: mutated)
        val mutant = runOnly(withAlso).getOrElse {
            val m = it.message.orEmpty()
            return@withContext if (environmental(m)) verdict("environmental", "the mutant is not a running dapp ($m) - a failure for that reason proves nothing about the guard. Usually the replacement broke compilation, or module_args are missing")
            else verdict("runner_error", "mutant run failed: $m")
        }
        val env = mutant.cases.firstOrNull { c -> environmental(c.error.orEmpty()) }
        if (env != null) {
            return@withContext verdict("environmental", "the mutant is not a running dapp (${env.error}) - a failure for that reason proves nothing about the guard")
        }
        val case = mutant.cases.singleOrNull { it.name.endsWith(test) }
            ?: return@withContext verdict("runner_did_not_finish", "the mutant run returned ${mutant.cases.size} case(s) [${mutant.cases.joinToString { it.name }}] and none is $test; notes: ${mutant.notes}")
        if (case.ok) {
            return@withContext verdict("vacuous", "the test stays GREEN with the guard removed - it does not exercise this guard and proves nothing about it. A test that passes in both states is a fake green. Make the test drive the attack the guard refuses")
        }
        val error = case.error.orEmpty()

        // 4. WHY did it fail. See the KDoc above verifyOne for the rule, and for
        //    what each round found wrong with the round before it.
        if (error.contains("did not fail", ignoreCase = true)) {
            return@withContext verdict("load_bearing", "with the guard removed the test fails because the attack LANDED: $error", loadBearing = true)
        }

        // Every load-bearing conclusion still passes the caller's own pins.
        fun landed(note: String): JsonObject {
            if (stillRefused != null && error.contains(stillRefused)) {
                return verdict(
                    "still_refused",
                    "the attack was still refused, by something other than this guard: $error. If that is defence in " +
                        "depth, name it in alsoRemove; otherwise the test is measuring a different guard"
                )
            }
            val callerPinned = attackLanded.isNotBlank() && !attackLanded.equals("did not fail", ignoreCase = true)
            if (callerPinned && !error.contains(attackLanded, ignoreCase = true)) {
                return verdict(
                    "red_for_another_reason",
                    "the test went red without the guard and nothing in the guard's declaration refused, but the " +
                        "fragment you pinned ('$attackLanded') is absent: $error. Read it before counting this guard as proven"
                )
            }
            return verdict("load_bearing", "with the guard removed the test fails and $note: $error", loadBearing = true)
        }

        // 4a. THE GUARD'S OWN DECLARATIONS, module-qualified.
        val guardModule = runnerModule(path, src)
        val declRx = Regex("\\b(operation|query|function)\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(")
        val guardDecl = declRx.findAll(maskRellSource(src, maskStrings = true)).lastOrNull { it.range.first < at }
            ?.let { it.groupValues[1] to it.groupValues[2] }
        val ownDeclarations = LinkedHashSet<String>()
        if (guardDecl != null) {
            ownDeclarations += "$guardModule:${guardDecl.second}"
            if (guardDecl.first == "function") {
                ownDeclarations += declarationsReaching(files, isTest, guardDecl.second)
            }
        }


        // 4b. THE CANONICAL SHAPES. Five rounds of heuristics over the runner's
        //     error text were each beaten one token away, so round 16 stopped
        //     widening them: the tool now names the two test shapes it can
        //     prove, proves those exactly, and answers `ambiguous_refusal` -
        //     with the shape to write - for everything else. See the KDoc above
        //     verifyOne.
        val frames = FRAME_REGEX.findAll(error).map {
            Frame(it.groupValues[1], it.groupValues[2], baseName(it.groupValues[3]), it.groupValues[4].toIntOrNull() ?: 0)
        }.toList()
        val productionFrames = frames.filter { it.module !in testModuleNames }
        val ownFrame = productionFrames.lastOrNull { "${it.module}:${it.declaration}" in ownDeclarations }
        val statements = testStatementsInvoking(files, isTest, test, ownDeclarations)?.statements ?: emptyList()
        val decls = ownDeclarations.joinToString()

        val shapeHelp = "The two shapes verify_guards proves, both of which invoke the guard's declaration in " +
            "EXACTLY ONE statement of the test: SHAPE A, the must-fail test - one " +
            "`rell.test.tx().op($decls(...)).run_must_fail(...)` carrying that ONE operation, where removing the " +
            "guard must make the transaction SUCCEED (the runner says \"did not fail\"); SHAPE B, the must-hold test " +
            "- one invocation that is expected to SUCCEED (a single-operation `.run()`, or a direct call to the " +
            "query), with the damage measured AFTER it by a rell.test assert_*, by a later transaction, or by a " +
            "test-side check. Anything else - a loop, a table, several invocations, a helper this scan cannot follow " +
            "- leaves \"was the attack refused or was the damage noticed\" unanswerable, and those are opposite verdicts."

        fun notAShape(why: String) = verdict(
            "ambiguous_refusal",
            "the mutant went red, but $test is neither shape this tool can read: $why Error: $error. It is NOT " +
                "counted as proven, and that is not a reason to weaken the test - it is a reason to write the " +
                "measurement down. $shapeHelp"
        )

        fun stillRefusedByTheGuard(refusedBy: String) = verdict(
            "still_refused",
            "the attack was still REFUSED by $refusedBy with the guard removed, and that is a declaration this guard " +
                "runs in: $error. The test invokes it exactly once, so this refusal IS that invocation - the attack, " +
                "not a later transaction noticing damage. If it is defence in depth, name the refusing line in " +
                "alsoRemove; otherwise the test is measuring a different guard"
        )

        // Which declarations can own a FRAME-LESS error. An operation refusal
        // always carries `[module:op(file:line)]`, so a frame-less error is
        // either the TEST module or a production QUERY (or a function a query
        // calls) - and which is read off the string literals each one owns.
        fun frameLessTestOwners() = testSourcesProducing(files, isTest, error)
        fun frameLessQueryOwners() = queriesProducing(files, isTest, error)
            .filter { (m, q) -> testStatementsInvoking(files, isTest, test, setOf("$m:$q"))?.statements?.isNotEmpty() == true }
            .map { (m, q) -> "$m:$q" }
            .distinct()

        // 4c. IS THIS ONE OF THE SHAPES? Everything that makes "which
        //     invocation refused" unanswerable is refused here, by structure,
        //     before any error text is read.
        if (ownDeclarations.isEmpty()) {
            return@withContext notAShape(
                "the guard is not inside any operation, query or function, so there is no declaration whose refusal " +
                    "could be its own and no invocation to place the red in."
            )
        }
        val notCanonical: String? = when {
            statements.isEmpty() ->
                "no top-level statement of it invokes $decls - the scan follows direct calls and ONE test-module " +
                    "helper chain, and found neither."
            statements.size > 1 ->
                "it invokes $decls in ${statements.size} separate statements (lines " +
                    "${statements.joinToString { it.first.toString() }}), and the runner's error does not say which " +
                    "of them the red belongs to."
            else -> statements.single().why.ifEmpty { null }
        }
        if (notCanonical != null) return@withContext notAShape(notCanonical)
        val stmt = statements.single()

        // 4d. SHAPE A, THE MUST-FAIL TEST. The test asserts that the attack
        //     FAILS, in its only invocation of the guard's declaration. The
        //     single proof that the guard is load-bearing is the transaction
        //     SUCCEEDING without it - "did not fail", answered above. So every
        //     other red here is the attack still being refused: by the guard's
        //     own declaration (defence in depth), by something else in the same
        //     stack, or by a query whose literals own a frame-less message.
        if (stmt.runMustFail) {
            if (ownFrame != null) {
                return@withContext stillRefusedByTheGuard(
                    "'${ownFrame.module}:${ownFrame.declaration}' (${ownFrame.file}:${ownFrame.line})"
                )
            }
            val other = productionFrames.lastOrNull()
            if (other != null) {
                return@withContext verdict(
                    "still_refused",
                    "the attack was still REFUSED - by '${other.module}:${other.declaration}' " +
                        "(${other.file}:${other.line}), which is not a declaration this guard runs in: $error. In a " +
                        "must-fail test (SHAPE A) the ONLY red that proves the guard is load-bearing is the " +
                        "transaction succeeding, which the runner reports as \"did not fail\"; a transaction that " +
                        "still fails - whoever refused it - proves nothing about this guard. Name the refusing line " +
                        "in alsoRemove if it is defence in depth"
                )
            }
            val queryOwners = frameLessQueryOwners()
            val testOwners = frameLessTestOwners()
            val ownQuery = queryOwners.singleOrNull()?.takeIf { testOwners.isEmpty() }
            return@withContext when {
                ownQuery != null && ownQuery in ownDeclarations -> stillRefusedByTheGuard(
                    "the query '$ownQuery', whose refusal carries no frame"
                )
                ownQuery != null -> verdict(
                    "still_refused",
                    "the attack was still REFUSED: the error carries no frame, and the only declaration that can " +
                        "produce its text is the query '$ownQuery' ($error). In a must-fail test only \"did not " +
                        "fail\" proves the attack landed"
                )
                TEST_ASSERT_REGEX.containsMatchIn(error) -> notAShape(
                    "its red is a rell.test assertion (${TEST_ASSERT_REGEX.find(error)!!.value}) rather than the " +
                        "must-fail statement, so the test measures something beside the attack it names."
                )
                else -> notAShape(
                    "its red carries NO frame and no string literal in the test module or in any production query " +
                        "it invokes can produce that text, so what refused - if anything - cannot be established."
                )
            }
        }

        // 4e. SHAPE B, THE MUST-HOLD TEST. The test drives the attack with a
        //     single `.run()` (or one direct query call) that is expected to
        //     SUCCEED, and the damage is measured afterwards. Because that is
        //     the ONLY statement in which any declaration this guard runs in
        //     executes, removing the guard cannot change anything the test does
        //     BEFORE it - so a refusal from the guard's own declarations is that
        //     invocation being refused, and a refusal from anything else
        //     happened after the attack had already landed.
        if (ownFrame != null) {
            return@withContext stillRefusedByTheGuard(
                "'${ownFrame.module}:${ownFrame.declaration}' (${ownFrame.file}:${ownFrame.line})"
            )
        }
        val other = productionFrames.lastOrNull()
        if (other != null) {
            return@withContext landed(
                "the refusal came from '${other.module}:${other.declaration}' (${other.file}:${other.line}), which is " +
                    "not a declaration this guard runs in. $test invokes $decls in exactly one statement (line " +
                    "${stmt.first}) and expects it to SUCCEED, so nothing this guard reaches ran before it and this " +
                    "refusal is a later transaction NOTICING the damage"
            )
        }
        val assertion = TEST_ASSERT_REGEX.find(error)
        if (assertion != null) {
            return@withContext landed(
                "the failing text is `${assertion.value}` - a rell.test assertion, which only the TEST module can " +
                    "produce (a refusing operation always carries a [module:declaration(file:line)] frame). The " +
                    "test measured the damage; nothing refused it"
            )
        }
        val testOwners = frameLessTestOwners()
        val queryOwners = frameLessQueryOwners()
        return@withContext when {
            testOwners.isNotEmpty() && queryOwners.isEmpty() -> landed(
                "the error carries no frame, and a refusing operation always carries one - the only source in the " +
                    "submission that owns a string literal producing this text is the TEST module " +
                    "(${testOwners.joinToString()}), so the test NOTICED the damage and nothing refused"
            )
            queryOwners.size == 1 && testOwners.isEmpty() -> {
                val q = queryOwners.single()
                if (q in ownDeclarations) {
                    stillRefusedByTheGuard("the query '$q', whose refusal carries no frame")
                } else {
                    landed(
                        "the error carries no frame; the only declaration that can produce its text is the query " +
                            "'$q', which is not a declaration this guard runs in - that is the damage being noticed"
                    )
                }
            }
            else -> {
                val sources = mutableListOf<String>()
                if (testOwners.isNotEmpty()) sources += "the test module (${testOwners.joinToString()})"
                if (queryOwners.isNotEmpty()) {
                    sources += "the production quer${if (queryOwners.size == 1) "y" else "ies"} " +
                        queryOwners.joinToString()
                }
                verdict(
                    "ambiguous_refusal",
                    when {
                        sources.size >= 2 -> "the mutant went red with an error that carries NO frame: $error. Its " +
                            "text can be produced by ${sources.joinToString(" AND ")}, which $test invokes - so a " +
                            "refusal and the test noticing the damage are the same string here, and the two " +
                            "verdicts they imply (still_refused and load_bearing) are opposite. Reword one of the " +
                            "two messages so no production declaration and no test assertion share words"
                        queryOwners.size > 1 -> "the mutant went red with an error that carries NO frame: $error. " +
                            "More than one production query $test invokes owns a literal that produces it " +
                            "(${queryOwners.joinToString()}), so which one refused cannot be established. Give " +
                            "them distinct messages and run it again"
                        else -> "the mutant went red with an error that carries NO frame: $error, and no string " +
                            "literal in the test module or in any production query $test invokes can produce that " +
                            "text - so whether $decls refused the attack or the damage was noticed afterwards " +
                            "cannot be established. It is NOT counted as proven. Make the damage visible to the " +
                            "test itself - an assertion on the state the attack changes, or a run_must_fail on the " +
                            "attack - or pass attackLanded naming a fragment only the attack landing produces"
                    }
                )
            }
        }
    }

    /** `[main:take(main.rell:9)]` - one frame of the runner's stack. */
    private val FRAME_REGEX = Regex("\\[([A-Za-z_][\\w.]*)\\s*:\\s*([A-Za-z_]\\w*)\\(([^()]*?):(\\d+)\\)]")

    private data class Frame(val module: String, val declaration: String, val file: String, val line: Int)

    /**
     * One top-level statement of the named test that invokes the guard's own
     * declaration: its line range, whether it ends in `run_must_fail` (SHAPE A)
     * rather than `run()` (SHAPE B), and - in [why] - the reason it is NOT one
     * of the two canonical shapes, empty when it is. A loop, a transaction
     * carrying more than the guard's own operation, or a helper chain with
     * several call sites all leave "which invocation refused" unanswerable, and
     * that is exactly the question five rounds of heuristics kept guessing at.
     */
    private data class TestStatement(
        val first: Int,
        val last: Int,
        val runMustFail: Boolean,
        val why: String
    )

    /** The test function's file and its invoking statements in source order. */
    private data class TestInvocations(val path: String, val statements: List<TestStatement>)

    /** `System function 'rell.test.assert_equals'` - only a TEST module can produce this. */
    private val TEST_ASSERT_REGEX = Regex("""System function 'rell\.test\.assert_\w+'""")

    /** `operation take(`, `query left(`, `function clamp(` - one declaration head. */
    private val DECL_HEAD_REGEX = Regex("""\b(operation|query|function)\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(""")

    /**
     * The shortest string literal this scan will accept as EVIDENCE that a
     * declaration produced a message. Two characters of shared text is a
     * coincidence; three is the shortest refusal message the corpus holds
     * (round 14's `"cap"`).
     */
    private val MIN_EVIDENCE_LITERAL = 3

    /** One declaration's body span in the RAW source (masking is length-preserving, so offsets carry over). */
    private data class DeclSpan(val kind: String, val name: String, val start: Int, val end: Int)

    /**
     * Every operation/query/function body of [raw], as offsets into [raw]
     * itself. The structure is read from the MASKED text - a `}` inside a
     * string is not a brace - and the span is then sliced out of the raw text,
     * so the body still carries its string literals.
     */
    private fun declSpans(raw: String): List<DeclSpan> {
        val masked = maskRellSource(raw, maskStrings = true)
        val out = mutableListOf<DeclSpan>()
        DECL_HEAD_REGEX.findAll(masked).forEach { m ->
            val parenStart = masked.indexOf('(', m.range.first)
            if (parenStart < 0) return@forEach
            val parenEnd = matchBrace(masked, parenStart, '(', ')') ?: return@forEach
            var j = parenEnd + 1
            while (j < masked.length && masked[j] != '{' && masked[j] != '=' && masked[j] != ';') j++
            when {
                j < masked.length && masked[j] == '{' -> {
                    val cl = matchBrace(masked, j, '{', '}') ?: return@forEach
                    out += DeclSpan(m.groupValues[1], m.groupValues[2], j + 1, cl)
                }
                j < masked.length && masked[j] == '=' -> {
                    val semi = masked.indexOf(';', j).let { if (it < 0) masked.length else it }
                    out += DeclSpan(m.groupValues[1], m.groupValues[2], j + 1, semi)
                }
            }
        }
        return out
    }

    /**
     * Every string literal in [raw], unescaped, ignoring comments. What a
     * declaration can SAY is exactly the set of literals it owns, and that is
     * how a frame-less error is attributed to a declaration.
     */
    private fun stringLiteralsIn(raw: String): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var state = 0 // 0 code, 1 line comment, 2 block comment, 3 in literal
        var quote = ' '
        while (i < raw.length) {
            val c = raw[i]
            val next = if (i + 1 < raw.length) raw[i + 1] else '\u0000'
            when (state) {
                0 -> when {
                    c == '/' && next == '/' -> { state = 1; i += 2; continue }
                    c == '/' && next == '*' -> { state = 2; i += 2; continue }
                    c == '"' || c == '\'' -> { state = 3; quote = c; sb.setLength(0) }
                }
                1 -> if (c == '\n') state = 0
                2 -> if (c == '*' && next == '/') { state = 0; i += 2; continue }
                else -> when {
                    c == '\\' && i + 1 < raw.length -> {
                        sb.append(
                            when (next) {
                                'n' -> '\n'
                                't' -> '\t'
                                'r' -> '\r'
                                else -> next
                            }
                        )
                        i += 2
                        continue
                    }
                    c == quote -> { state = 0; out += sb.toString() }
                    else -> sb.append(c)
                }
            }
            i++
        }
        return out
    }

    /** The submission's TEST files that own a literal the error text contains. */
    private fun testSourcesProducing(
        files: Map<String, String>,
        isTest: Map<String, Boolean>,
        error: String
    ): List<String> = files.filter { isTest.getValue(it.key) }
        .filter { (_, c) ->
            stringLiteralsIn(c).any { it.trim().length >= MIN_EVIDENCE_LITERAL && error.contains(it) }
        }
        .keys.toList()

    /**
     * Every module-qualified production QUERY that owns - directly, or through
     * the production functions it transitively calls - a string literal the
     * error text contains. Operations are deliberately absent: their refusal
     * carries a frame, so they can never be the source of a frame-less error
     * (p14b shares its words with the operation `sweep`, which is why sharing
     * words with an operation must not make the answer ambiguous).
     */
    private fun queriesProducing(
        files: Map<String, String>,
        isTest: Map<String, Boolean>,
        error: String
    ): List<Pair<String, String>> {
        val functionBodies = mutableMapOf<String, MutableList<String>>()
        val queries = mutableListOf<Triple<String, String, String>>()
        files.forEach { (p, content) ->
            if (isTest.getValue(p)) return@forEach
            val module = RunRellTests.moduleNameForPath(RellCheck.normalizeSourceRoot(p), content)
            declSpans(content).forEach { d ->
                val body = content.substring(d.start, d.end)
                when (d.kind) {
                    "function" -> functionBodies.getOrPut(d.name) { mutableListOf() }.add(body)
                    "query" -> queries += Triple(module, d.name, body)
                    else -> Unit
                }
            }
        }
        fun owns(text: String) =
            stringLiteralsIn(text).any { it.trim().length >= MIN_EVIDENCE_LITERAL && error.contains(it) }
        return queries.filter { (_, _, body) ->
            if (owns(body)) return@filter true
            val seen = mutableSetOf<String>()
            val work = ArrayDeque(RellSecurityCheck.calledNames(maskRellSource(body, maskStrings = true)))
            while (work.isNotEmpty()) {
                val n = work.removeFirst()
                if (!seen.add(n)) continue
                functionBodies[n]?.forEach { b ->
                    if (owns(b)) return@filter true
                    RellSecurityCheck.calledNames(maskRellSource(b, maskStrings = true))
                        .forEach { if (it !in seen) work.add(it) }
                }
            }
            false
        }.map { it.first to it.second }
    }

    private fun baseName(path: String) = path.replace('\\', '/').substringAfterLast('/')

    /** The index of the `close` matching the `open` at [start], or null. */
    private fun matchBrace(text: String, start: Int, open: Char, close: Char): Int? {
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                open -> depth++
                close -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return null
    }

    /**
     * Every module-qualified operation and query of the submission's own
     * PRODUCTION files that reaches the function [target] through the call
     * graph, transitively. Rell function names are module-scoped and a
     * cross-module call is written `mod.fn(...)`, so the graph is keyed on the
     * BARE name: two modules with a same-named helper collapse into one node,
     * which can only WIDEN "the guard's own declarations" and never narrow it -
     * the safe direction, because the widening costs an `ambiguous_refusal` and
     * never a false `load_bearing`.
     */
    private fun declarationsReaching(
        files: Map<String, String>,
        isTest: Map<String, Boolean>,
        target: String
    ): Set<String> {
        val bodies = mutableMapOf<String, MutableList<String>>()
        val entryPoints = mutableListOf<Pair<String, String>>()
        files.forEach { (p, content) ->
            if (isTest.getValue(p)) return@forEach
            val masked = maskRellSource(content, maskStrings = true)
            val module = RunRellTests.moduleNameForPath(RellCheck.normalizeSourceRoot(p), content)
            RellSecurityCheck.functionDefinitions(masked).forEach { d ->
                bodies.getOrPut(d.name) { mutableListOf() }.add(d.body)
            }
            RellSecurityCheck.scanOperations(p, masked).forEach { entryPoints += "$module:${it.name}" to it.body }
            RellSecurityCheck.scanQueries(masked).forEach { entryPoints += "$module:${it.name}" to it.body }
        }
        val extensions = extensionsOf(files, isTest)
        return entryPoints.filter { (_, body) -> reaches(bodies, extensions, body, target) }
            .mapTo(LinkedHashSet()) { it.first }
    }

    /**
     * `@extend(pre_pay) function clamp_check(...)` - the extendable's name and
     * the extension's. Rell's rule is that calling an `@extendable` function
     * runs EVERY `@extend` of it, so an extension is reached by every caller of
     * the extendable even though no call site anywhere names it. Round 16's
     * p16a hid a guard behind exactly that: the guard's own operation refused,
     * the call graph could not see the edge, and the refusal was certified
     * load_bearing, ok:true.
     */
    private val EXTEND_REGEX = Regex(
        """@extend\s*\(\s*(?:[A-Za-z_]\w*\s*\.\s*)*([A-Za-z_]\w*)\s*\)\s*(?:@\w+(?:\s*\([^)]*\))?\s*)*function\s+([A-Za-z_]\w*)"""
    )

    /** extendable name -> every production `@extend` of it, keyed on the bare name like the rest of the call graph. */
    private fun extensionsOf(files: Map<String, String>, isTest: Map<String, Boolean>): Map<String, List<String>> {
        val out = mutableMapOf<String, MutableList<String>>()
        files.forEach { (p, content) ->
            if (isTest.getValue(p)) return@forEach
            EXTEND_REGEX.findAll(maskRellSource(content, maskStrings = true)).forEach { m ->
                out.getOrPut(m.groupValues[1]) { mutableListOf() }.add(m.groupValues[2])
            }
        }
        return out
    }

    /**
     * True when [body]'s transitive call closure over [bodies] contains
     * [target]. A call to an `@extendable` function reaches every `@extend` of
     * it ([extensions]), which is the only edge in Rell that no call site
     * spells out.
     */
    private fun reaches(
        bodies: Map<String, List<String>>,
        extensions: Map<String, List<String>>,
        body: String,
        target: String
    ): Boolean {
        val seen = mutableSetOf<String>()
        val work = ArrayDeque(RellSecurityCheck.calledNames(body))
        while (work.isNotEmpty()) {
            val n = work.removeFirst()
            if (!seen.add(n)) continue
            if (n == target) return true
            extensions[n]?.forEach { if (it !in seen) work.add(it) }
            bodies[n]?.forEach { b -> RellSecurityCheck.calledNames(b).forEach { if (it !in seen) work.add(it) } }
        }
        return false
    }

    /**
     * Every top-level statement of the test function [test] that invokes one of
     * [declarations] (module-qualified, `module:name`) - directly, or through a
     * TEST-MODULE HELPER in ANY test module of the submission - IN SOURCE
     * ORDER, each classified against the two canonical shapes.
     *
     * A "top-level statement" ends at a `;` at brace depth zero OR at the `}`
     * that closes a depth-zero block, so a `for`/`while`/`if` block is ONE
     * statement rather than none: round 16's p16b drove the same two
     * transactions from a table, the splitter saw no statement at all, and the
     * whole shape analysis was skipped.
     *
     * ROUND 17 WIDENED WHERE A HELPER MAY LIVE AND HOW A NAME RESOLVES.
     * `helperBodies` used to be built from `functionDefinitions` of the ONE
     * FILE being scanned, so a helper imported from another test module
     * invoked nothing the scan could see. An honest single invocation written
     * there read as no invocation at all - `ambiguous_refusal` where the same
     * helper in the same file is `load_bearing` (p17a) - and a SECOND
     * invocation hidden there left shape B's "this declaration ran exactly
     * once" false, so a later transaction NOTICING the damage was reported as
     * the attack being refused (p17b, the dangerous direction). Helpers now
     * come from EVERY test module of the submission, and a call is resolved
     * through the CALLING module's own imports: `h(...)` in its own module or
     * through `import mod.*;`, `mod.h(...)` through `import a.b.mod;`, and
     * `alias.h(...)` through `import alias: a.b.mod;`.
     *
     * A call site counts only when its QUALIFIER can name the declaration's own
     * module: `x.take(...)` is not `main.take(...)` (round 15's p15b - the call
     * graph is keyed on bare names, so without this a same-named operation in
     * another module would add a second "invoking" statement and cost an
     * `ambiguous_refusal`). The qualifier may be the module's own name, its
     * last segment, or a name the CALLING module binds to it with an import -
     * an ALIAS included: `import m: main;` then `m.take(...)`, which round 16
     * did not recognise at all and left outside both shapes (p17f).
     */
    private fun testStatementsInvoking(
        files: Map<String, String>,
        isTest: Map<String, Boolean>,
        test: String,
        declarations: Set<String>
    ): TestInvocations? {
        if (declarations.isEmpty()) return null
        val modulesByName = mutableMapOf<String, MutableSet<String>>()
        declarations.forEach { d ->
            val module = d.substringBeforeLast(':')
            modulesByName.getOrPut(d.substringAfterLast(':')) { mutableSetOf() }
                .addAll(listOf(module, module.substringAfterLast('.')))
        }
        val bare = modulesByName.keys

        // EVERY test module of the submission: the functions it declares (a
        // module can span several files - a header-less sibling belongs to the
        // module the RUNNER puts it in) and the names its own imports bind.
        val moduleOfFile = mutableMapOf<String, String>()
        val functionsByModule = mutableMapOf<String, MutableMap<String, MutableList<String>>>()
        val importBindings = mutableMapOf<String, MutableMap<String, String>>()
        val wildcardImports = mutableMapOf<String, MutableSet<String>>()
        files.forEach { (p, content) ->
            if (!isTest.getValue(p)) return@forEach
            val maskedFile = maskRellSource(content, maskStrings = true)
            val module = RunRellTests.moduleNameForPath(RellCheck.normalizeSourceRoot(p), content)
            moduleOfFile[p] = module
            val fns = functionsByModule.getOrPut(module) { mutableMapOf() }
            RellSecurityCheck.functionDefinitions(maskedFile).forEach { d ->
                fns.getOrPut(d.name) { mutableListOf() }.add(d.body)
            }
            val binds = importBindings.getOrPut(module) { mutableMapOf() }
            val wild = wildcardImports.getOrPut(module) { mutableSetOf() }
            IMPORT_REGEX.findAll(maskedFile).forEach { m ->
                val alias = m.groupValues[1]
                val target = m.groupValues[2]
                when {
                    alias.isNotEmpty() -> binds[alias] = target
                    m.groupValues[3].isNotEmpty() -> wild += target
                    else -> binds[target.substringAfterLast('.')] = target
                }
            }
        }

        /** The test-module function a call `q.n(...)` (q may be empty) in [module] names, as `module:n`. */
        fun resolveHelper(module: String, qualifier: String, name: String): String? {
            val candidates = if (qualifier.isEmpty()) {
                listOf(module) + wildcardImports[module].orEmpty()
            } else {
                listOfNotNull(importBindings[module]?.get(qualifier))
            }
            return candidates.firstOrNull { functionsByModule[it]?.containsKey(name) == true }?.let { "$it:$name" }
        }
        val helperBodies = mutableMapOf<String, List<String>>()
        functionsByModule.forEach { (m, fns) -> fns.forEach { (n, bodies) -> helperBodies["$m:$n"] = bodies } }

        val callSite = Regex(
            """(?:([A-Za-z_]\w*)\s*\.\s*)?\b(${bare.joinToString("|") { Regex.escape(it) }})\s*\("""
        )
        /** How many times [text], read from inside [module], invokes the guard's declaration. */
        fun invocationsIn(module: String, text: String) = callSite.findAll(text).count { m ->
            val qualifier = m.groupValues[1]
            val mods = modulesByName.getValue(m.groupValues[2])
            val bound = importBindings[module]?.get(qualifier)
            qualifier.isEmpty() || qualifier in mods ||
                (bound != null && (bound in mods || bound.substringAfterLast('.') in mods))
        }
        val anyCallSite = Regex("""(?:([A-Za-z_]\w*)\s*\.\s*)?\b([A-Za-z_]\w*)\s*\(""")
        /** Every test-module helper [text] calls from inside [module], and how many times. */
        fun helperCallsIn(module: String, text: String): Map<String, Int> {
            val out = mutableMapOf<String, Int>()
            anyCallSite.findAll(text).forEach { m ->
                val node = resolveHelper(module, m.groupValues[1], m.groupValues[2]) ?: return@forEach
                out[node] = (out[node] ?: 0) + 1
            }
            return out
        }
        // A test-module helper "reaches" the declaration when its own body
        // invokes it, or when it calls another helper that does.
        val reaching = mutableSetOf<String>()
        var grew = true
        while (grew) {
            grew = false
            helperBodies.forEach { (node, bodies) ->
                if (node in reaching) return@forEach
                val owner = node.substringBeforeLast(':')
                val hit = bodies.any { b ->
                    invocationsIn(owner, b) > 0 || helperCallsIn(owner, b).keys.any { it in reaching }
                }
                if (hit) { reaching += node; grew = true }
            }
        }
        /** The text a statement really executes: itself plus every reaching helper it calls. */
        fun flatten(module: String, text: String, depth: Int, seen: MutableSet<String>): String {
            if (depth > 4) return text
            val sb = StringBuilder(text)
            helperCallsIn(module, text).keys.filter { it in reaching }.forEach { node ->
                if (seen.add(node)) {
                    helperBodies.getValue(node).forEach {
                        sb.append('\n').append(flatten(node.substringBeforeLast(':'), it, depth + 1, seen))
                    }
                }
            }
            return sb.toString()
        }
        /** How many times the statement invokes the declaration, helpers expanded. */
        fun sites(module: String, text: String, depth: Int): Int {
            if (depth > 4) return 99
            var n = invocationsIn(module, text)
            helperCallsIn(module, text).forEach { (node, here) ->
                if (node in reaching) {
                    n += here * helperBodies.getValue(node).sumOf { sites(node.substringBeforeLast(':'), it, depth + 1) }
                }
            }
            return n
        }
        val loopRx = Regex("""\b(?:for|while)\b""")
        val opRx = Regex("""\.\s*op\s*\(""")
        val mustFailRx = Regex("""\brun_must_fail\s*\(""")
        val callee = Regex("""([A-Za-z_]\w*)\s*\(""")
        files.forEach { (p, content) ->
            if (!isTest.getValue(p)) return@forEach
            val masked = maskRellSource(content, maskStrings = true)
            val module = moduleOfFile.getValue(p)
            val head = Regex("\\bfunction\\s+${Regex.escape(test)}\\s*\\(").find(masked) ?: return@forEach
            val brace = masked.indexOf('{', head.range.last)
            if (brace < 0) return@forEach
            val close = matchBrace(masked, brace, '{', '}') ?: return@forEach
            val spans = mutableListOf<TestStatement>()
            var from = brace + 1
            var depth = 0
            var i = brace + 1
            fun record(endAt: Int) {
                val statement = masked.substring(from, endAt)
                val siteCount = sites(module, statement, 0)
                if (siteCount > 0) {
                    val flat = flatten(module, statement, 0, mutableSetOf())
                    val ops = opRx.findAll(flat).count()
                    val opName = opRx.find(flat)
                        ?.let { callee.find(flat, it.range.last + 1) }
                        ?.groupValues?.get(1)
                    val mustFail = mustFailRx.containsMatchIn(flat)
                    spans += TestStatement(
                        masked.substring(0, from).count { c -> c == '\n' } + 1,
                        masked.substring(0, endAt).count { c -> c == '\n' } + 1,
                        mustFail,
                        when {
                            loopRx.containsMatchIn(flat) ->
                                "the statement that invokes it is a LOOP (or a table driven from one), so it invokes " +
                                    "the declaration an unknown number of times and no single transaction can be " +
                                    "named as the attack."
                            siteCount != 1 ->
                                "the statement invokes the declaration $siteCount times (directly, or through a " +
                                    "test-module helper this scan cannot reduce to one call site)."
                            ops > 1 ->
                                "the transaction it runs carries $ops operations, so a refusal from it need not be " +
                                    "the guard's own operation being refused."
                            ops == 1 && opName != null && opName !in bare ->
                                "the one operation in that transaction is `$opName`, which is not a declaration this " +
                                    "guard runs in - the declaration is reached some other way inside the statement."
                            else -> ""
                        }
                    )
                }
                from = endAt + 1
            }
            while (i < close) {
                when (masked[i]) {
                    '(', '[', '{' -> depth++
                    ')', ']' -> depth--
                    '}' -> { depth--; if (depth == 0) record(i) }
                    ';' -> if (depth == 0) record(i)
                }
                i++
            }
            if (spans.isNotEmpty()) return TestInvocations(p, spans)
        }
        return null
    }

    /** `import a: b.c;` / `import b.c;` / `import b.c.*;` - one import of a (masked) module. */
    private val IMPORT_REGEX = Regex("""\bimport\s+(?:([A-Za-z_]\w*)\s*:\s*)?([A-Za-z_][\w.]*?)\s*(\.\s*\*)?\s*;""")
}

class LocalChainStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = true

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val action = (extractString(args, "action") ?: "up").lowercase()
        return runCatching {
            when (action) {
                "down" -> toolSuccessResult(with(LocalChain) { down().toJson() })
                "status" -> toolSuccessResult(with(LocalChain) { status().toJson() })
                "up" -> executeUp(args)
                else -> toolErrorResult("action must be one of: up, down, status (got \"$action\")")
            }
        }.getOrElse { e ->
            toolErrorResult("local_chain_up failed: ${e.message}")
        }
    }

    private suspend fun executeUp(args: Map<String, Any>): CallToolResult {
        val (files, invalidKeys) = extractRellFilesMap(extractRellSourcesArg(args))
        if (invalidKeys.isNotEmpty()) {
            return toolErrorResult(
                "`files` values must be Rell source strings; non-string value(s) at: ${invalidKeys.joinToString(", ")}"
            )
        }
        if (files.isEmpty()) {
            return toolErrorResult(
                "action \"up\" needs a `files` map with at least one app module, e.g. {\"main.rell\": \"module; ...\"}"
            )
        }
        val moduleArgs = when (val moduleArgsArg = args["moduleArgs"]) {
            null, is JsonNull -> emptyMap()
            is JsonObject -> {
                val badModules = moduleArgsArg.filterValues { it !is JsonObject }.keys
                if (badModules.isNotEmpty()) {
                    return toolErrorResult(
                        "moduleArgs value for module(s) ${badModules.joinToString(", ")} must be an args object - got a non-object value. Do not JSON-encode the args."
                    )
                }
                moduleArgsArg.mapValues { (_, v) -> (v as JsonObject).toMap() }
            }
            else -> return toolErrorResult(
                "moduleArgs must be an object mapping module name -> args object (e.g. {\"lib.ft4.core.accounts\": {...}})"
            )
        }
        val ttlSeconds = extractLong(args, "ttlSeconds")
        val apiPort = extractLong(args, "apiPort")?.let {
            if (it !in 1..65535) return toolErrorResult("apiPort must be in 1..65535 (got $it)") else it.toInt()
        }
        val databaseUrl = extractString(args, "databaseUrl")?.also {
            if (!it.startsWith("jdbc:postgresql://")) {
                return toolErrorResult("databaseUrl must be a PostgreSQL JDBC URL (jdbc:postgresql://...)")
            }
        } ?: System.getenv(LocalChain.DATABASE_URL_ENV)
        val result = withContext(Dispatchers.IO) {
            LocalChain.up(
                files = files,
                databaseUrl = databaseUrl,
                moduleArgs = moduleArgs,
                ttlSeconds = ttlSeconds,
                apiPort = apiPort
            )
        }
        return if (result.ok) {
            toolSuccessResult(with(LocalChain) { result.toJson() })
        } else {
            toolErrorResult(result.notes)
        }
    }

    /** Absent/JSON-null -> null; anything else must parse as a whole number. */
    private fun extractLong(arguments: Map<String, Any>, key: String): Long? {
        val value = arguments[key] ?: return null
        if (value is JsonNull) return null
        val raw = if (value is JsonPrimitive) value.content else value.toString()
        return raw.toLongOrNull()
            ?: throw IllegalArgumentException("$key must be an integer; got $raw")
    }
}

class Ft4ModuleArgsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val name = extractString(args, "name")
        val includeIccf = extractBoolean(args, "includeIccf") ?: false
        return toolSuccessResult(Ft4ModuleArgs.toJson(name, includeIccf))
    }
}

class ChrBuildHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrBuildHelp.toJson())
    }
}

class ChrReplHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrReplHelp.toJson())
    }
}

class ChrToolsHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrToolsHelp.toJson())
    }
}

class ChrSeederHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrSeederHelp.toJson())
    }
}

class BlockchainPropertiesHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(BlockchainPropertiesHelp.toJson())
    }
}

class ChrEifHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrEifHelp.toJson())
    }
}

class ChromiaYmlDefinitionsHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaYmlDefinitionsHelp.toJson())
    }
}

class ChrCompletionHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrCompletionHelp.toJson())
    }
}

class ChromiaProjectStructureHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaProjectStructureHelp.toJson())
    }
}

class ChrMultiSignatureHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrMultiSignatureHelp.toJson())
    }
}

class WriteDeploymentConfigStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val network = requireParameter(args, "network")
        val name = extractString(args, "name") ?: extractString(args, "chain")
        if (WriteDeploymentConfig.resolveNetwork(network) == null) {
            return toolErrorResult(WriteDeploymentConfig.unknownNetworkMessage(network))
        }
        // AUDIT F7: the caller's own chromia.yml, merged into rather than replaced.
        // deployment_preflight's fix line sends agents here, and the file this tool
        // used to synthesise dropped the project's FT4 moduleArgs, its
        // test.moduleArgs block and libs.iccf - invisibly to validate_chromia_yml.
        val yaml = extractString(args, "yaml") ?: extractString(args, "chromiaYml")
        return toolSuccessResult(WriteDeploymentConfig.toJson(network, name, yaml))
    }
}

class ChrDeployHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrDeployHelp.toJson())
    }
}

class ChrNodeHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrNodeHelp.toJson())
    }
}

class ChrQueryHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrQueryHelp.toJson())
    }
}

class VaultLeaseHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(VaultLeaseHelp.toJson())
    }
}

class ChrGenerateClientHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrGenerateClientHelp.toJson())
    }
}

class ChromiaCookbookHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaCookbookHelp.toJson())
    }
}

class ChrKeyIdHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrKeyIdHelp.toJson())
    }
}

class ChromiaLanguageClientsHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaLanguageClientsHelp.toJson())
    }
}

class ChromiaDocsYmlHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaDocsYmlHelp.toJson())
    }
}

class ChrLibraryHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrLibraryHelp.toJson())
    }
}

class ChrCreateRellDappHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChrCreateRellDappHelp.toJson())
    }
}

class CheckDappProjectStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        // yaml is optional: agents often have only Rell code in hand. A blank or
        // missing yaml falls back to a minimal default at the current pins and the
        // result says so, instead of a "Missing required parameter" dead end.
        val yaml = extractString(args, "yaml")?.takeIf { it.isNotBlank() }
        // AUDIT F10: `files` is the CANONICAL name for Rell sources on every
        // code-taking tool here (8 already used it), and `rell` / `source` are
        // accepted with no warning and no preference. The note this used to
        // append - "prefer `rell` in future calls" - actively pushed an agent
        // away from the spelling the rest of the server uses.
        val rellFiles = extractStringMap(args, "files")
            ?: extractStringMap(args, "rell")
            ?: extractStringMap(args, "source")
            ?: throw IllegalArgumentException(
                "Missing required parameter: files - the dapp sources, a map of path -> source " +
                    "(e.g. {\"src/main.rell\": \"module; ...\"}) or a single source string " +
                    "(`rell` and `source` are accepted as aliases). The gates cannot vouch for " +
                    "code they never saw."
            )
        val allowAdminModules = extractBoolean(args, "allowAdminModules") ?: false
        // Swapped arguments (yaml = Rell source, rell = the chromia.yml) produced
        // two true-but-useless errors - "YAML parse error: expected key: value at
        // line 1" and "Syntax error: Unexpected token 'blockchains'" - that read
        // as two broken files (DX audit 2026-09-04, Q4). Name the swap.
        val singleRell = rellFiles.values.singleOrNull()
        if (yaml != null && RELL_HEADER_REGEX.containsMatchIn(yaml) && singleRell != null && YAML_ROOT_KEY_REGEX.containsMatchIn(singleRell)) {
            return toolErrorResult(
                "check_dapp_project: the arguments look swapped - `yaml` starts with a Rell module header" +
                    " (`${RELL_HEADER_REGEX.find(yaml)?.value?.trim()}`) and `rell` has a chromia.yml root key" +
                    " (`${YAML_ROOT_KEY_REGEX.find(singleRell)?.value?.trim()}`). Pass the chromia.yml text as `yaml`" +
                    " and the Rell sources as `rell` ({\"src/main.rell\": \"module; ...\"})."
            )
        }
        var result = CheckDappProject.check(
            yaml = yaml ?: DappScaffold.defaultChromiaYml(),
            rellFiles = rellFiles,
            allowAdminModules = allowAdminModules,
            usedDefaultYaml = yaml == null
        )
        return toolSuccessResult(result.toJson())
    }
}

class CheckFt4ImportsStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val rellFiles = extractStringMap(args, "files")
            ?: extractStringMap(args, "rell")
            ?: extractStringMap(args, "source")
            ?: throw IllegalArgumentException(
                "Missing required parameter: files - the Rell sources to scan, a map of " +
                    "path -> source (e.g. {\"src/main.rell\": \"module; ...\"}) or a single source " +
                    "string (`rell` and `source` are accepted as aliases)."
            )
        val allowAdminModules = extractBoolean(args, "allowAdminModules") ?: false
        // ok:true on an all-blank submission certified nothing (QA input-abuse lens).
        RellCheck.requireSomeSourceContent(rellFiles)
        return toolSuccessResult(Ft4ImportCheck.scanFiles(rellFiles, allowAdminModules).toJson())
    }
}

class TranslateErrorStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val error = requireParameter(args, "error")
        val context = extractString(args, "context")
        return toolSuccessResult(ErrorTranslator.translate(error, context).toJson())
    }
}

class ChromiaRellLanguageHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellLanguageHelp.toJson())
    }
}

class ChromiaFt4QueriesHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaFt4QueriesHelp.toJson())
    }
}

class ChromiaIntegrationsHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaIntegrationsHelp.toJson())
    }
}

class ChromiaVectorSearchHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaVectorSearchHelp.toJson())
    }
}

class ChromiaRellTypesHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellTypesHelp.toJson())
    }
}

class ChromiaRellExpressionsHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellExpressionsHelp.toJson())
    }
}

class ChromiaRellStatementsHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellStatementsHelp.toJson())
    }
}

class ChromiaRellDatabaseHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellDatabaseHelp.toJson())
    }
}

class ChromiaRellSystemlibHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellSystemlibHelp.toJson())
    }
}

class ChromiaRellPracticesHelpStrategy : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        return toolSuccessResult(ChromiaRellPracticesHelp.toJson())
    }
}

class OnboardingNextStepStrategy(
    /**
     * Runtime tool registry, checked dynamically so the plan names a tool
     * (local_chain_up, deployment_preflight) only when it is actually callable
     * on this deployment: compiled-in tools minus CHROMIA_MCP_DISABLE_TOOLS.
     * Recommending a disabled tool would send agents into the disabled-tool
     * refusal instead of the working fallback (e.g. `chr node start`). A
     * provider (not a snapshot) because [McpTools.ALL_TOOL_NAMES] is lazy.
     */
    private val registeredTools: () -> Set<String> = { McpTools.enabledToolNames() }
) : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val state = OnboardingNextStep.State(
            hasProject = extractBoolean(args, "hasProject") ?: false,
            compiles = extractBoolean(args, "compiles") ?: false,
            securityClean = extractBoolean(args, "securityClean") ?: false,
            testsPass = extractBoolean(args, "testsPass") ?: false,
            hasLocalChain = extractBoolean(args, "hasLocalChain") ?: false,
            hasTestnetContainer = extractBoolean(args, "hasTestnetContainer") ?: false,
            hasTestnetKey = extractBoolean(args, "hasTestnetKey") ?: false,
            hasDeploymentConfig = extractBoolean(args, "hasDeploymentConfig") ?: false,
            // `Testnet` is not a different goal (DX audit 2026-09-04): fold case and
            // whitespace here so the plan's own validation only fires on real typos.
            deployedTo = extractString(args, "deployedTo")?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: "none",
            goal = extractString(args, "goal")?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: "testnet"
        )
        return toolSuccessResult(OnboardingNextStep.plan(state, registeredTools()).toJson())
    }
}

class VerifyDeploymentStrategy(
    /** Test seam so the height-progression wait costs no suite time. */
    private val delayFn: suspend (Long) -> Unit = { kotlinx.coroutines.delay(it) },
    /**
     * Overall wall-clock deadline across ALL probe work (client construction
     * with its signer discovery, both height reads, the wait, the smoke
     * query); clamped. Live probe 2026-09-02 (D1): without it, a chain the
     * queried nodes do not serve kept the probe running past the hosting
     * platform's 60s proxy write timeout, and the caller got a closed socket
     * instead of the not-served hint. See [VerifyDeployment.DEFAULT_DEADLINE_MS].
     */
    deadlineMs: Long? = null
) : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    private val deadlineMs: Long =
        VerifyDeployment.clampDeadlineMs(deadlineMs ?: VerifyDeployment.configuredDeadlineMs())

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val brid = VerifyDeployment.parseBrid(requireParameter(args, "brid"))
        val network = extractString(args, "network")?.takeIf { it.isNotBlank() } ?: "testnet"
        val waitMs = VerifyDeployment.clampWaitMs(extractWaitMs(args))
        val queryName = extractString(args, "query")?.takeIf { it.isNotBlank() }
        val queryArgs = extractArgumentsMap(args, "arguments")
        val rid = BlockchainRid.buildFromHex(brid)

        // One deadline across ALL attempts: each stage gets only what is left.
        val startNanos = System.nanoTime()
        fun remainingMs(): Long = deadlineMs - (System.nanoTime() - startNanos) / 1_000_000

        val notes = mutableListOf<String>()
        val first = ProbeBudget.withBudget(remainingMs()) { repository.getBlockchainHeight(network, rid) }
            ?: return toolSuccessResult(
                buildJsonObject {
                    put("live", false)
                    put("brid", brid)
                    put("heightProgressing", false)
                    put("notes", VerifyDeployment.timeoutHint(network, deadlineMs))
                }
            )
        if (first is NetworkResult.Error) {
            // First REAL deploy (2026-09-04): for ~5 minutes after `chr deployment
            // create` the cluster nodes answered 404 for the new chain, and this
            // tool said "check the BRID and network" - a wrong-BRID diagnosis for
            // a chain that was registered and starting. The Directory knows the
            // difference: a registered chain has API URLs, an unknown BRID has none.
            val hosts = if (VerifyDeployment.isUnknownChain(first.message)) {
                directoryHostsFor(repository, network, brid, remainingMs())
            } else null
            if (hosts != null && hosts.isNotEmpty()) {
                notes += VerifyDeployment.startingHint(hosts)
                notes += "Node error: ${first.message}"
                return toolSuccessResult(
                    buildJsonObject {
                        put("live", false)
                        put("brid", brid)
                        put("heightProgressing", false)
                        put("registered", true)
                        put("hostedOn", buildJsonArray { hosts.forEach { add(JsonPrimitive(it)) } })
                        put("notes", notes.joinToString(" "))
                    }
                )
            }
            notes += "Height probe failed: ${VerifyDeployment.failureHint(first.message, network)}"
            if (hosts != null) notes += "The Directory chain lists no API URLs for this BRID on \"$network\" - it is not a registered chain there."
            notes += "Node error: ${first.message}"
            return toolSuccessResult(
                buildJsonObject {
                    put("live", false)
                    put("brid", brid)
                    put("heightProgressing", false)
                    if (hosts != null) put("registered", false)
                    put("notes", notes.joinToString(" "))
                }
            )
        }
        val firstHeight = (first as NetworkResult.Success).data

        delayFn(waitMs.coerceAtMost(remainingMs().coerceAtLeast(0L)))
        val second = ProbeBudget.withBudget(remainingMs()) { repository.getBlockchainHeight(network, rid) }
        val secondHeight = (second as? NetworkResult.Success)?.data ?: firstHeight
        if (second == null) {
            notes += "Second height probe skipped - the overall ${deadlineMs}ms deadline was " +
                "reached; reporting the first reading."
        } else if (second is NetworkResult.Error) {
            notes += "Second height probe failed (${second.message}) - reporting the first reading."
        }
        val progressing = secondHeight > firstHeight
        if (!progressing) {
            notes += "Block height did not advance within ${waitMs}ms - an idle dapp chain produces " +
                "no blocks without transactions, so this alone is not a failure; the chain is known " +
                "and answering."
        }

        var queryResult: kotlinx.serialization.json.JsonObject? = null
        if (queryName != null) {
            when (val q = ProbeBudget.withBudget(remainingMs()) {
                repository.executeCustomQuery(network, rid, queryName, queryArgs)
            }) {
                null -> notes += "Smoke query '$queryName' skipped - the overall ${deadlineMs}ms " +
                    "deadline was reached (the chain itself is live; retry the query via " +
                    "chromia_dapp_query)."
                is NetworkResult.Success -> queryResult = q.data
                is NetworkResult.Error -> notes +=
                    "Smoke query '$queryName' failed: ${q.message} " +
                        "(the chain itself is live; check the query name/arguments with " +
                        "rell.get_app_structure via chromia_dapp_query)."
            }
        }

        return toolSuccessResult(
            buildJsonObject {
                put("live", true)
                put("brid", brid)
                put("blockHeight", secondHeight)
                put("heightProgressing", progressing)
                queryResult?.let { put("queryResult", it) }
                put(
                    "notes",
                    (notes + "Chain $brid is known on \"$network\" at height $secondHeight.")
                        .joinToString(" ")
                )
            }
        )
    }

    /**
     * The API URLs the Directory chain of [network] lists for [brid]
     * (`cm_get_blockchain_api_urls`), or null when the question cannot be
     * asked: [network] is a raw node URL rather than mainnet/testnet, the
     * Directory did not answer, or the budget is spent. Empty list = the
     * Directory answered and does not know the chain.
     */
    private suspend fun directoryHostsFor(
        repository: ChromiaRepository,
        network: String,
        brid: String,
        budgetMs: Long
    ): List<String>? {
        val directory = ChromiaYmlValidator.officialDirectoryBrid(network.trim().lowercase()) ?: return null
        val answer = ProbeBudget.withBudget(budgetMs) {
            repository.executeCustomQuery(
                network, BlockchainRid.buildFromHex(directory),
                "cm_get_blockchain_api_urls", mapOf("blockchain_rid" to brid.hexStringToByteArray())
            )
        } ?: return null
        val data = (answer as? NetworkResult.Success)?.data?.get("data") as? JsonArray ?: return null
        return data.mapNotNull { (it as? JsonPrimitive)?.content }
    }

    /** Optional small wait override; a non-integer value is a validation error. */
    private fun extractWaitMs(arguments: Map<String, Any>): Long? {
        val value = arguments["waitMs"] ?: return null
        if (value is JsonNull) return null
        val raw = if (value is JsonPrimitive) value.content else value.toString()
        return raw.trim().toLongOrNull()
            ?: throw IllegalArgumentException(
                "waitMs must be an integer number of milliseconds (0-${VerifyDeployment.MAX_WAIT_MS}); got \"$raw\""
            )
    }
}

class DeploymentPreflightStrategy(
    /**
     * Overall wall-clock deadline for the reachability probe, shared by ALL
     * probed URLs (never per candidate); clamped. Without it the probe shared
     * chromia_dapp_query's unbounded blocking-height-read class - up to
     * [DeploymentPreflight.MAX_PROBED_URLS] full endpoint crawls back to
     * back. Compile/security gates are local work and run outside this
     * budget. See [ProbeBudget].
     */
    deadlineMs: Long? = null
) : BaseToolStrategy() {
    override val touchesLocalMachine: Boolean = false

    private val probeDeadlineMs: Long = ProbeBudget.clampDeadlineMs(
        deadlineMs ?: ProbeBudget.configuredDeadlineMs(ProbeBudget.PREFLIGHT_DEADLINE_ENV)
    )

    override suspend fun execute(request: CallToolRequest, repository: ChromiaRepository): CallToolResult {
        val args = request.argumentsOrEmpty as Map<String, Any>
        val yaml = requireParameter(args, "yaml")
        // AUDIT F10: `network` is what 22 other tools on this server call this;
        // `target` is the alias, accepted with no warning. The output keeps both
        // - `target` echoes what was asked for, `network` is the mainnet/testnet/
        // custom classification derived from it.
        val target = requireParameter(args, "network", "target")
        // Same shape as check_dapp_project's `rell`: one source string
        // (checked as main.rell) or a map of path -> source. `files` is
        // accepted as an alias (same pattern as CheckDappProjectStrategy):
        // rell_check and run_rell_tests take `files`, and a silently dropped
        // `files` here would skip the source gate and still report ready:true
        // on a testnet target.
        //
        // ROUND 16 fixed the comment that used to stand here: it said "`rell`
        // wins when both are present; the alias is noted", and the very next
        // line reads `files` FIRST and notes nothing. An agent that read the
        // comment and passed both would have shipped the wrong tree past the
        // deployment gate believing the other one was checked. `files` is the
        // canonical name (audit F10) and it is read first; `rell` and `source`
        // are the aliases, in that order. Sending two of them with DIFFERENT
        // content is an error on the four code-taking tools
        // ([rellSourceAliasConflict]) - do not rely on this order to choose.
        val rell = extractStringMap(args, "files")
            ?: extractStringMap(args, "rell")
            ?: extractStringMap(args, "source")
        val strict = extractBoolean(args, "strict")
        return runCatching {
            // Compile + security run blocking compiler work; the reachability
            // probe is the repository's suspend height read (same seam as
            // verify_deployment), so no live network in unit tests. The probe
            // budget starts at the FIRST probe call (after the local gates)
            // and is spent across all candidates: a first URL that eats the
            // whole budget leaves the rest nothing - they answer instantly
            // with the deadline message instead of starting fresh crawls.
            var probeStartNanos = -1L
            val result = withContext(Dispatchers.IO) {
                DeploymentPreflight.run(yaml, target, rell, strict) { network, bridHex ->
                    if (probeStartNanos < 0) probeStartNanos = System.nanoTime()
                    val remainingMs =
                        probeDeadlineMs - (System.nanoTime() - probeStartNanos) / 1_000_000
                    ProbeBudget.withBudget(remainingMs) {
                        repository.getBlockchainHeight(network, BlockchainRid.buildFromHex(bridHex))
                    } ?: NetworkResult.Error(
                        ProbeBudget.preflightProbeTimeoutMessage(probeDeadlineMs)
                    )
                }
            }
            toolSuccessResult(result.toJson())
        }.getOrElse { e ->
            toolErrorResult("deployment_preflight failed: ${e.message}")
        }
    }
}
