package org.chromia.tools

import io.modelcontextprotocol.kotlin.sdk.types.Tool
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*

object McpTools {

    /**
     * Provenance of the documentation index an answer was drawn from - origin,
     * generated_at, age_days, segments, stale. Declared identically on
     * fetch_docs, search and fetch: audit F2 found that only fetch_docs said
     * anything, so the ChatGPT-contract pair every agent reaches for first
     * quoted a 320-day-old fragment as current API.
     */
    internal val DOCS_INDEX_PROPERTY = JsonObject(
        mapOf(
            "type" to JsonPrimitive("object"),
            "description" to JsonPrimitive(
                "Which documentation index answered: origin, generated_at, age_days, segments, and stale=true when it is past the freshness limit. Check `stale` before quoting a fragment as current."
            ),
            "properties" to JsonObject(
                mapOf(
                    "origin" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "generated_at" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "age_days" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "segments" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "stale" to JsonObject(mapOf("type" to JsonPrimitive("boolean")))
                )
            )
        )
    )

    /** The human-readable half of [DOCS_INDEX_PROPERTY], present only on a stale index. */
    internal val DOCS_INDEX_NOTE_PROPERTY = JsonObject(
        mapOf(
            "type" to JsonPrimitive("string"),
            "description" to JsonPrimitive(
                "Present only when the documentation index is older than its freshness limit: says when it was generated and that newer releases may be missing - confirm versions against GitLab tags."
            )
        )
    )

    fun runDappQueriesTool() = Tool(
        name = "chromia_dapp_query",
        description = """
            Execute a query against a live dapp chain and get JSON back.
            Workflow: (1) filter_blockchains for the blockchain RID; (2) chromia_dapp_query with the
            default "rell.get_app_structure" query, which returns the chain's queries, operations and
            entities with their parameter names and types; (3) call the one you want as
            "<mount>.<query>" with the arguments that structure reported. Cache the structure result -
            follow-up questions usually need it again.
            SECURITY: never read or display private keys, keystores or generated keypairs.
            The whole query is bounded by an overall deadline (default 20s, env
            CHROMIA_MCP_QUERY_DEADLINE_MS, capped at 45s), so a chain the queried nodes do not serve
            returns an actionable error (usually: pass the dapp's own node URL as `network`) instead of
            crawling every endpoint for minutes.
            Full workflow and use cases: describe_tool{tool:"chromia_dapp_query"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(

                    "brid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Blockchain RID (64-char hex). The canonical name for a chain id on this server; `rid` and `blockchainRid` are accepted as aliases with no warning.")
                        )
                    ),

                    "blockchainRid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The Blockchain RID of the dApp")
                        )
                    ),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),

                    "query" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "The query name to execute (default: 'rell.get_app_structure')"
                            )
                        )
                    ),

                    "arguments" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive(
                                "Optional arguments for the query as a map/dictionary that will be converted to GTV format"
                            ),
                            "properties" to JsonObject(emptyMap()),
                            "additionalProperties" to JsonPrimitive(true)
                        )
                    )
                ),
            ),
            required = listOf("brid")
        ),
        title = "Execute dApp Query",
        annotations = null,
        outputSchema = null
    )

    fun getAllAssetsTool() = Tool(
        name = "get_all_assets",
        description = "Get information about all assets on a specific network",
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Get All Assets",
        annotations = null,
        outputSchema = null
    )

    fun getTotalRewardsPaidTool() = Tool(
        name = "get_total_rewards_paid",
        description = "Get the total rewards paid on a specific network",
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Get Total Rewards Paid",
        annotations = null,
        outputSchema = null
    )

    fun getAssetDistributionTool() = Tool(
        name = "get_asset_distribution",
        description = "Get distribution information for a specific asset across different blockchains and account types",
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "assetId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The asset ID to query")
                        )
                    ),
                    "brids" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of blockchain RIDs to filter by")
                        )
                    ),
                    "accountTypes" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of account types to filter by")
                        )
                    ),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "excludeAccounts" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of account IDs to exclude")
                        )
                    ),
                    "excludeBrids" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of blockchain RIDs to exclude")
                        )
                    ),
                    "excludeAccountTypes" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of account types to exclude")
                        )
                    )
                )
            ),
            required = listOf("assetId")
        ),
        title = "Get Asset Distribution",
        annotations = null,
        outputSchema = null
    )

    fun getAssetTopHoldersTool() = Tool(
        name = "get_asset_top_holders",
        description = """
            - Get the top holders of a specific asset 
            - Returns detailed holder information including:
                - Account ID of the holder
                - Total balance held by the account
                - Number of chains where the account holds the asset
                - Specific chain blockchain RID
                - Account type (e.g., 'FT4_USER', 'SYSTEM', etc.)
            - This tool is useful for:
                - Analyzing asset concentration and distribution patterns
                - Identifying major stakeholders
                - Understanding asset holder demographics by account type
                - Cross-chain asset holder analysis
            - Output shape (this server, not the raw explorer):
                - `data.getAssetTopHolders` is holders only and honours `limit`;
                  `holderCount` is how many came back
                - `othersRemainder`: the explorer's synthetic "Others" row (the
                  combined balance outside the page), lifted out of the list
                  because it is not an account
                - an unknown asset id is an ERROR saying so, not `[]` (the raw
                  explorer answers `[]` for unknown ids and over-narrow filters
                  alike; this tool tells them apart via get_asset_blockchains)
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "assetId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The asset ID to query (e.g., '5F16D1545A0881F971B164F1601CBBF51C29EFD0633B2730DA18C403C3B428B5')")
                        )
                    ),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Optional limit for number of holders to return (default: 50)")
                        )
                    ),
                    "brids" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of blockchain RIDs to filter by")
                        )
                    ),
                    "accountTypes" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of account types to filter by")
                        )
                    ),
                    "excludeAccounts" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of account IDs to exclude")
                        )
                    ),
                    "excludeBrids" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of blockchain RIDs to exclude")
                        )
                    ),
                    "excludeAccountTypes" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of account types to exclude")
                        )
                    ),
                )
            ),
            required = listOf("assetId")
        ),
        title = "Get Asset Top Holders",
        annotations = null,
        outputSchema = null
    )

    fun getBlockchainAnalyticsTool() = Tool(
        name = "get_blockchain_analytics",
        // The upstream state is part of the answer, not a footnote: an agent
        // that reads these counts as current reports four-day-old numbers, and
        // one that waits on a busy chain has no way to know a minute is normal.
        // docs/UPSTREAM.md #11 carries the measurements and closes the entry
        // when ChromaWay fixes it; this note goes with it. Well inside
        // MAX_DESCRIPTION_BYTES, so describe_tool serves the same text.
        description = """
            Get detailed analytics for a specific blockchain including transaction counts, operation counts, and active accounts over time.
            UPSTREAM, measured 2026-09-08 (docs/UPSTREAM.md #11): the explorer's index is STALE since 2026-09-04 06:40Z, so every count here
            stops at that moment and the response does not say so. Cost tracks the chain's transaction count, not the request: a 15-transaction
            chain answers in 0.6s, a 1.4M-transaction chain took 41.5s, and the query has been observed dropping the connection at the 60s
            request timeout. Treat a timeout as the explorer's, retry, and read the numbers as of 2026-09-04.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "brid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The blockchain RID")
                        )
                    ),
                    "fromTimestamp" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional timestamp to start analytics from")
                        )
                    ),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    )
                )
            ),
            required = listOf("brid")
        ),
        title = "Get Blockchain Analytics",
        annotations = null,
        outputSchema = null
    )

    fun getMonthlyActiveAccountsPerChainTool() = Tool(
        name = "get_monthly_active_accounts_per_chain",
        description = "Get the number of monthly active accounts for a specific blockchain",
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "brid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The blockchain RID")
                        )
                    ),
                    "untilTimestamp" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional timestamp to get analytics until")
                        )
                    ),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    )
                )
            ),
            required = listOf("brid")
        ),
        title = "Get Monthly Active Accounts",
        annotations = null,
        outputSchema = null
    )

    fun getBlockchainDetailsTool() = Tool(
        name = "get_blockchain_details",
        description = """
            - Get detailed information about a specific blockchain by its RID 
            - Returns comprehensive metadata about the blockchain including:
                - The blockchain's unique RID
                - All names/aliases associated with the blockchain
                - Whether it's a system blockchain or a user application
                - The container information where the blockchain is deployed
                - The cluster the blockchain belongs to
                - Current operational state of the blockchain
            - This tool is useful for:
                - Verifying blockchain existence and current status
                - Understanding the deployment environment of a blockchain
                - Checking if a blockchain is a system chain or user application
                - Determining which cluster hosts a specific blockchain
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "brid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Blockchain RID (64-char hex). The canonical name for a chain id on this server; `rid` and `blockchainRid` are accepted as aliases with no warning.")
                        )
                    ),

                    "rid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The blockchain's RID")
                        )
                    ),

                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    )
                )
            ),
            required = listOf("brid")
        ),
        title = "Get Blockchain Details",
        annotations = null,
        outputSchema = null
    )

    fun filterBlockchains() = Tool(
        name = "filter_blockchains",
        description = """
            The primary tool for blockchain NAME lookups: list blockchains with filtering by name (exact
            or partial), RID, cluster, container, operational state (RUNNING, REMOVED, PAUSED) and
            system-chain vs user-application, with limit/offset paging and sortBy/sortDirection.
            Each row carries the blockchain's RID, its names/aliases, its cluster, its container, its
            state and whether it is a system chain.
            Use it to get the RID before chromia_dapp_query, to survey deployment environments, or to
            check a chain's operational status.
            Full field and filter list: describe_tool{tool:"filter_blockchains"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(

                    "brid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Blockchain RID (64-char hex). The canonical name for a chain id on this server; `rid` and `blockchainRid` are accepted as aliases with no warning.")
                        )
                    ),

                    "rid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional blockchain RID to filter by specific blockchain")
                        )
                    ),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),

                    "name" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional blockchain name to filter by (primary use case - e.g., 'auro', 'MarbleRumble')")
                        )
                    ),

                    "cluster" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional cluster name to filter by (e.g., 'pink', 'system')")
                        )
                    ),

                    "container" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional container ID to filter by")
                        )
                    ),

                    "state" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional state to filter by (e.g., 'RUNNING', 'REMOVED', 'PAUSED'). UPSTREAM LIMITATION (verified live 2026-09-07): the explorer answers INTERNAL_ERROR whenever this argument is supplied, with or without the others - it is not your call that is wrong (docs/UPSTREAM.md #3b). Filter by cluster/container/name instead and read `state` off the rows.")
                        )
                    ),

                    "system" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive("Optional filter for system chains (true) vs user applications (false)")
                        )
                    ),

                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Optional limit for number of blockchains to return (default: 10)")
                        )
                    ),

                    "offset" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Optional offset for pagination (default: 0)")
                        )
                    ),

                    "sortBy" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional field to sort by (e.g., 'name', 'cluster', 'state')")
                        )
                    ),

                    "sortDirection" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional sort direction ('ASC' or 'DESC')")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Filter Blockchains",
        annotations = null,
        outputSchema = null
    )

    fun getAllTransactionsTool() = Tool(
        name = "get_all_transactions",
        description = """
            - Get a comprehensive list of transactions with advanced filtering capabilities
            - Returns detailed transaction information including:
                - Transaction RID
                - Block information (height)
                - Blockchain information (name and RID)
                - Transaction timestamp
                - Operations performed in the transaction
                - Accounts involved in the transaction
                - Signers of the transaction
                - Asset transfers within the transaction
            - Supports extensive filtering options:
                - Filter by specific transaction RID
                - Filter by block ID
                - Filter by blockchain IDs (include/exclude)
                - Filter by timestamp range (from/to)
                - Filter by operation types (include/exclude)
                - Filter by signers (include/exclude)
                - Filter by accounts (include/exclude)
                - Filter by assets
                - Pagination support (limit/offset)
                - Sorting options (sortBy/sortDirection)
            - This tool is useful for:
                - Analyzing transaction patterns and trends
                - Investigating specific transactions or transaction types
                - Monitoring account activity and asset transfers
                - Building transaction reports and analytics
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(

                    "brid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Blockchain RID (64-char hex). The canonical name for a chain id on this server; `rid` and `blockchainRid` are accepted as aliases with no warning.")
                        )
                    ),

                    "rid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional specific transaction RID to query")
                        )
                    ),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),

                    "blockId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional block ID to filter transactions")
                        )
                    ),

                    "blockchainIds" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of blockchain IDs to include")
                        )
                    ),

                    "notInBlockchains" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of blockchain IDs to exclude")
                        )
                    ),

                    "timestampFrom" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional start timestamp for filtering (ISO format)")
                        )
                    ),

                    "timestampTo" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional end timestamp for filtering (ISO format)")
                        )
                    ),

                    "operations" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of operation types to include")
                        )
                    ),

                    "notInOperations" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of operation types to exclude")
                        )
                    ),

                    "signers" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of signer IDs to include")
                        )
                    ),

                    "excludedSigners" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of signer IDs to exclude")
                        )
                    ),

                    "accounts" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of account IDs to include")
                        )
                    ),

                    "excludedAccounts" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of account IDs to exclude")
                        )
                    ),

                    "assets" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("string")
                                )
                            ),
                            "description" to JsonPrimitive("Optional list of asset IDs to filter by")
                        )
                    ),

                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Optional limit for number of transactions to return (default: 50)")
                        )
                    ),

                    "offset" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Optional offset for pagination (default: 0)")
                        )
                    ),

                    "sortBy" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional field to sort by (e.g., 'timestamp', 'blockHeight')")
                        )
                    ),

                    "sortDirection" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional sort direction ('ASC' or 'DESC')")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Get All Transactions",
        annotations = null,
        outputSchema = null
    )

    fun getAllOperationsTool() = Tool(
        name = "get_all_operations",
        description = """
            - Get a comprehensive list of all operations available across blockchains
            - Returns detailed information about each operation including:
                - Operation name/type
                - Blockchain RID where the operation is available
            - This tool is useful for:
                - Discovering what operations are available on different blockchains
                - Understanding the functionality provided by each blockchain
                - Mapping operations to their respective blockchains
                - Analyzing the distribution of operations across the network
                - Building operation-based filters for transaction queries
                - Understanding the capabilities of different dApps and system chains
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Get All Operations",
        annotations = null,
        outputSchema = null
    )

    fun getFilterAssetsTool() = Tool(
        name = "filter_assets",
        description = """
            - Search and filter assets with advanced filtering capabilities
            - Returns detailed asset information including:
                - Asset name, symbol, and unique ID
                - Asset icon URL for display purposes
                - Blockchain RID where the asset exists
                - Asset type (e.g., 'FT' for fungible tokens, 'NFT' for non-fungible tokens)
                - Decimal precision for the asset
                - Total supply of the asset
                - Number of transfers involving the asset
                - Number of blockchains where the asset is present
                - Total count of matching assets
            - Supports extensive filtering and search options:
                - Filter by specific blockchain RID
                - Search by asset name, symbol, or other text (e.g., "BJORN", "CHR")
                - Filter by asset type (fungible, non-fungible, etc.)
                - Pagination support (limit/offset)
                - Sorting options (sortBy/sortDirection)
            - This tool is useful for:
                - Finding specific assets by name or symbol
                - Discovering assets on particular blockchains
                - Analyzing asset distribution across blockchains
                - Building asset portfolios and tracking
                - Market research and asset discovery
                - Understanding asset characteristics and usage patterns
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "brid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional blockchain RID to filter assets by specific blockchain")
                        )
                    ),
                    "searchQuery" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional search query to find assets by name, symbol, or other text (e.g., 'BJORN', 'CHR')")
                        )
                    ),
                    "type" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional asset type filter (e.g., 'FT' for fungible tokens, 'NFT' for non-fungible tokens)")
                        )
                    ),
                    "limit" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Optional limit for number of assets to return (default: 50)")
                        )
                    ),
                    "offset" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Optional offset for pagination (default: 0)")
                        )
                    ),
                    "sortBy" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional field to sort by (e.g., 'name', 'symbol', 'supply', 'transferCount')")
                        )
                    ),
                    "sortDirection" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional sort direction ('ASC' or 'DESC')")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Filter Assets",
        annotations = null,
        outputSchema = null
    )

    fun getChrAggregatesTool() = Tool(
        name = "get_chr_aggregates",
        description = """
            CHR token deposit and withdrawal aggregates: deposits and withdrawals grouped by address and
            network id with per-group totals, plus overall depositsTotal / withdrawalsTotal - each part
            can be included or excluded.
            RESPONSE SIZE: by default the grouped arrays are capped at the first 50 entries each, with a
            `note` field saying how many entries were omitted. Pass full:true for the complete,
            uncapped response (can be hundreds of KB).
            Useful for CHR flow analysis, cross-network movement, major-holder activity and auditing.
            Full option list: describe_tool{tool:"get_chr_aggregates"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "includeTotals" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive("Whether to include overall deposit and withdrawal totals (default: true)")
                        )
                    ),
                    "includeGroupedDeposits" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive("Whether to include grouped deposits by address and network (default: true)")
                        )
                    ),
                    "includeGroupedWithdrawals" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive("Whether to include grouped withdrawals by address and network (default: true)")
                        )
                    ),
                    "full" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "false (default): summarized response - grouped arrays capped at the first 50 entries " +
                                    "each plus a `note` about what was omitted. true: the complete uncapped response " +
                                    "(observed at several hundred KB on mainnet)."
                            )
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Get CHR Aggregates",
        annotations = null,
        outputSchema = null
    )

    fun getAssetBlockchainsTool() = Tool(
        name = "get_asset_blockchains",
        description = """
            - Get detailed information about which blockchains contain a specific asset
            - Returns comprehensive asset blockchain distribution including:
                - Blockchain RID where the asset exists
                - Number of transfers involving the asset on each blockchain
                - Whether the blockchain is the source/origin of the asset
                - Blockchain name for easy identification
            - This tool is useful for:
                - Understanding asset distribution across multiple blockchains
                - Analyzing cross-chain asset activity and usage patterns
                - Identifying the origin blockchain of an asset
                - Tracking asset transfer volumes per blockchain
                - Investigating asset liquidity and availability across networks
                - Planning cross-chain asset strategies and integrations
                - Auditing asset presence and activity across the ecosystem
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "assetId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The asset ID to query (e.g., '5F16D1545A0881F971B164F1601CBBF51C29EFD0633B2730DA18C403C3B428B5')")
                        )
                    )
                )
            ),
            required = listOf("assetId")
        ),
        title = "Get Asset Blockchains",
        annotations = null,
        outputSchema = null
    )

    fun getSignerBlockchainsTool() = Tool(
        name = "get_signer_blockchains",
        description = """
            - Get detailed information about which blockchains a specific signer has been active on
            - Returns comprehensive signer activity information including:
                - Blockchain RID where the signer has been active
                - Blockchain name for easy identification
                - Number of transactions the signer has participated in on each blockchain
            - This tool is useful for:
                - Analyzing user activity patterns across multiple blockchains
                - Understanding signer engagement and participation levels
                - Tracking cross-chain user behavior and preferences
                - Identifying the most active blockchains for specific users
                - Investigating user transaction history and blockchain usage
                - Compliance and auditing of user activities across the ecosystem
                - Understanding user distribution and blockchain adoption patterns
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "signer" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The signer public key or identifier (e.g., '025C06D481B9469CC074863E850D0554ABF526F3D63ECE11F6E875239D4B3F01FC')")
                        )
                    )
                )
            ),
            required = listOf("signer")
        ),
        title = "Get Signer Blockchains",
        annotations = null,
        outputSchema = null
    )

    fun getAccountBlockchainsTool() = Tool(
        name = "get_account_blockchains",
        description = """
            - Get detailed information about which blockchains a specific account has been active on
            - Returns comprehensive account activity information including:
                - Blockchain RID where the account has been active
                - Blockchain name for easy identification
                - Number of transactions the account has participated in on each blockchain
                - Number of transfers the account has made on each blockchain
            - This tool is useful for:
                - Analyzing account activity patterns across multiple blockchains
                - Understanding account engagement and participation levels
                - Tracking cross-chain account behavior and preferences
                - Identifying the most active blockchains for specific accounts
                - Investigating account transaction and transfer history
                - Compliance and auditing of account activities across the ecosystem
                - Understanding account distribution and blockchain adoption patterns
                - Comparing transaction vs transfer activity for accounts
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "accountId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The account ID to query (e.g., '82d77d61b4b3ab966fe112791d8330f116eeacc87d4fadd48cf94a5d17c3eb67')")
                        )
                    )
                )
            ),
            required = listOf("accountId")
        ),
        title = "Get Account Blockchains",
        annotations = null,
        outputSchema = null
    )

    fun getPromptsTool() = Tool(
        name = "get_prompts",
        description = """
            - Get available prompts for interacting with the MCP tools
            - Returns prompt information including:
                - Categories of available prompts
                - Prompt templates for each category
                - Tool mappings and parameters
                - Example usage and descriptions
            - Supports filtering by:
                - Category
                - Tool name
                - Search text
            - This tool is useful for:
                - Discovering available MCP capabilities
                - Understanding how to use different tools
                - Finding example prompts for common tasks
                - Learning the parameter requirements for tools
            - Filling a prompt's blanks over prompts/get: every argument is capped
              at ${McpPrompts.MAX_ARGUMENT_CHARS} characters and may not carry control characters other
              than tab, newline and carriage return. Over the cap is an error naming
              it, not a value echoed back - a template can interpolate the same blank
              several times, so an unbounded argument is amplified into your context.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "category" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional category to filter prompts")
                        )
                    ),
                    "tool" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional tool name to filter prompts")
                        )
                    ),
                    "search" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional search text to filter prompts")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Get Available Prompts",
        annotations = null,
        outputSchema = null
    )

    fun fetchDocsTool() = Tool(
        name = "fetch_docs",
        description = """
            **NOTE FOR AI AGENTS: USE THIS TOOL FIRST TO QUERY DOCUMENTATION ABOUT CHROMIA, CHR/PMC CLI, FT4, RELL... 
            **SEMANTIC DOCUMENTATION SEARCH**

            Uses Retrieval-Augmented Generation with vector embeddings to find relevant documentation
            based on semantic similarity rather than exact keyword matching.

            **HOW IT WORKS:**
            - All Chromia documentation is chunked and indexed in an in-memory vector database
            - User query is converted to an embedding and matched against indexed documentation
            - Returns the most semantically relevant sections, even if they don't match exact keywords
            - Much more efficient than returning entire documentation files
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "query" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Natural language search query describing what documentation you're looking for (e.g., 'How to create a blockchain?' or 'FT4 authentication setup')")
                        )
                    ),
                )
            ),
            required = listOf("query")
        ),
        title = "Search relevant documentation about Chromia platform",
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "text" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Relevant documentation text. Each hit is one line: `id: <sha-256> | <text>` with real newlines written as \\n."
                            )
                        )
                    ),
                    "hits" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "description" to JsonPrimitive(
                                "Matching documentation segments, each with the stable id accepted by fetch"
                            ),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("object"),
                                    "properties" to JsonObject(
                                        mapOf(
                                            "id" to JsonObject(
                                                mapOf(
                                                    "type" to JsonPrimitive("string"),
                                                    "description" to JsonPrimitive(
                                                        "Stable SHA-256 id accepted by fetch"
                                                    )
                                                )
                                            ),
                                            "title" to JsonObject(
                                                mapOf("type" to JsonPrimitive("string"))
                                            ),
                                            "url" to JsonObject(
                                                mapOf("type" to JsonPrimitive("string"))
                                            ),
                                            "text" to JsonObject(
                                                mapOf("type" to JsonPrimitive("string"))
                                            )
                                        )
                                    ),
                                    "required" to JsonArray(
                                        listOf(JsonPrimitive("id"), JsonPrimitive("text"))
                                    )
                                )
                            )
                        )
                    ),
                    "index_note" to DOCS_INDEX_NOTE_PROPERTY,
                    "index" to DOCS_INDEX_PROPERTY
                )
            ),
            required = listOf("text", "hits")
        ),
        annotations = null
    )

    fun fetchTool() = Tool(
        name = "fetch",
        description = """
            ChatGPT-compatible fetch. Retrieves a documentation segment from the same RAG store as fetch_docs.
            Exact id match against the loaded store. Ids are stable SHA-256 hashes of source + chunk index + text
            and work across process restarts without a prior search. A miss is not-found.
            Prefer fetch_docs for Chromia documentation search; use this after search or fetch_docs when an id is required.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "id" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Document id returned by search or fetch_docs hits"
                            )
                        )
                    ),
                )
            ),
            required = listOf("id")
        ),
        title = "Fetch Document",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "id" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Stable SHA-256 document id (lowercase hex)"
                            )
                        )
                    ),
                    "title" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Document title when found")
                        )
                    ),
                    "url" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Source URL when found")
                        )
                    ),
                    "text" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Document text when found")
                        )
                    ),
                    "error" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Error message when the id is not found")
                        )
                    ),
                    "index_note" to DOCS_INDEX_NOTE_PROPERTY,
                    "index" to DOCS_INDEX_PROPERTY
                )
            ),
            required = listOf("id")
        )
    )

    fun searchTool() = Tool(
        name = "search",
        description = """
            ChatGPT-compatible search. Semantic documentation search over the same RAG store as fetch_docs.
            Prefer fetch_docs for Chromia documentation; this returns id/title/url results for ChatGPT.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "query" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Natural language search query describing what documentation you're looking for (e.g., 'How to create a blockchain?' or 'FT4 authentication setup')")
                        )
                    ),
                )
            ),
            required = listOf("query")
        ),
        title = "Search Documentation",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "results" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "description" to JsonPrimitive(
                                "Matching documentation segments, each with the stable id accepted by fetch"
                            ),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("object"),
                                    "properties" to JsonObject(
                                        mapOf(
                                            "id" to JsonObject(
                                                mapOf(
                                                    "type" to JsonPrimitive("string"),
                                                    "description" to JsonPrimitive(
                                                        "Stable SHA-256 id accepted by fetch"
                                                    )
                                                )
                                            ),
                                            "title" to JsonObject(
                                                mapOf("type" to JsonPrimitive("string"))
                                            ),
                                            "url" to JsonObject(
                                                mapOf("type" to JsonPrimitive("string"))
                                            )
                                        )
                                    ),
                                    "required" to JsonArray(
                                        listOf(JsonPrimitive("id"), JsonPrimitive("title"), JsonPrimitive("url"))
                                    )
                                )
                            )
                        )
                    ),
                    "index_note" to DOCS_INDEX_NOTE_PROPERTY,
                    "index" to DOCS_INDEX_PROPERTY
                )
            ),
            required = listOf("results")
        )
    )


    fun scaffoldDappTool() = Tool(
        name = "scaffold_dapp",
        description = """
            Return a production-correct new Chromia dapp skeleton (chromia.yml, src/main.rell, test).
            Pins: Rell ${DappScaffold.RELL_VERSION}, merkle_hash_version 2, FT4 v1.1.0r API 1, Chromia CLI 0.33.x.
            Pick the template by the EXPLOIT CLASS of what you are building - the `template` enum lists
            every one. 'hello' is the query-only quickstart; 'ft4' is the golden accounts template with
            the full authenticate -> authorize -> validate -> check-invariants pattern and runnable
            conservation / overdraft / non-owner-must-fail tests. Every other template is hardened
            against one named drain and ships the must-fail test that replays it.
            Which template refuses which attack, and what each one leaves as a module arg:
            describe_tool{tool:"scaffold_dapp"} or chromia_help{topic:"scaffold_dapp"}.
            NEVER includes lib.ft4.admin, admin.crosschain, ras_open, or ras_transfer_open.
            Does not send signed transactions and does not run chr. Confirm APIs with fetch_docs.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "name" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Optional dapp / chain name (lowercase [a-z][a-z0-9_]{0,31}). Default: hello"
                            )
                        )
                    ),
                    "notesFor" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Which per-class guidance to include in `notes`. Default: the template actually returned, plus a one-line pointer to the rest. \"all\" returns the whole catalogue (~22 KB / ~5,530 tokens - it used to be attached to EVERY response, 89% of a `hello` scaffold); a template name returns just that one's. `notes_for` is accepted as an alias.")
                        )
                    ),
                    "template" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "enum" to kotlinx.serialization.json.JsonArray(DappScaffold.templates.map { JsonPrimitive(it) }),
                            "description" to JsonPrimitive("Skeleton flavor: 'hello' (query-only quickstart, default), 'ft4' (accounts, authenticated operation, TS client), 'governance' (DAO treasury: quorum, fixed voting window, stake-weighted votes, execute-once - structural, with the drain replayed as a must-fail test; registration mints NO voting weight - weight comes from a founder-countersigned genesis allocation, so its founder key is a module arg (main.founder_pubkey), see the notes), 'vault' (oracle-priced reserve, NOT an exchange or a curve - a swap pool is 'amm': reserve-backed credits, bounded and rate-limited price, staleness halt - with the unbacked mint replayed as a must-fail test), or 'staking' (staking / yield / rewards / emissions - NOT vesting, which is 'streaming': sponsor-funded pool as the only reward source, pool-capped release, per-share accumulator, cooldown unstake - with the round-4 empty-pool mint replayed as a must-fail test), or 'marketplace' (NFT marketplace / listings / auctions with creator royalties: exact-price buys on an immutable listing so a seller cannot sandwich a pending buy, escrowed offers with expiry settled atomically, royalty fixed at mint - with the round-5 price sandwich replayed as a must-fail test and the off-market royalty bypass documented, not faked; it ALSO ships the timed ascending auction - no mutable bid field, the standing bid is its own immutable escrow row, settlement permissionless after the deadline - and one encumbrance helper every token-moving path consults, so do not write an auction freehand), or 'lending' (lending pool / credit line / money market - anything where depositors hold a SHARE of a pool whose value moves: NO cash-denominated debt is stored anywhere, so the round-6 just-in-time interest capture is unwritable rather than merely guarded - positions and the pool carry scaled_debt in index units, the cash figures exist only inside a pool_state, pool_now() is the only function that makes one and every pricing helper takes one - plus the vault's bounded oracle, over-collateralisation, a liquidation threshold with close factor and bonus, and the minimum-first-deposit guard against ERC-4626 share inflation, with the round-6 drain replayed as a must-fail test; its oracle key is a module arg, see the notes), or 'subscription' (RECURRING PULL BILLING - a merchant collects period after period against one authorisation: the claim is the escrow the payer funded, the fee accrues pro rata so nothing is billed in advance, and either party may always cancel; round 13 drained the build that used 'streaming' for this), 'streaming' (payment stream / payroll / vesting grant / drip, PREPAID - a clock-metered payout to ONE named beneficiary: no operation writes a timestamp an entitlement is measured from, every term is immutable, the stream is prepaid and cancellation pays before it refunds, and pause/resume is shipped with both transition guards - with the round-7 anchor grief and both round-8 pause drains replayed as must-fail tests), or 'amm' (constant-product swap pool / DEX pair / automated market maker: a swap NAMES THE EXACT RESERVES it was quoted at and there is no tolerance field at all, so it pays the quoted number or reverts - stronger than a min_out floor, not a weakening of one - and liquidity is an IMMUTABLE POSITION ROW WITH A TERM, so the just-in-time deposit-before-a-swap-withdraw-after cannot be written; both round-8 drains ship as must-fail tests and the residuals the guards do NOT close ship as a test too), or 'stablecoin' (a coin minted against LOCKED COLLATERAL - CDP, synthetic, pegged asset - NOT 'vault', which is where round 9's drain was sent: there is NO operation that redeems the coin for collateral at par out of somebody else's position; the peg is the debtor's burn-at-par against their OWN debt, under-water positions close by PRO-RATA liquidation that pays every liquidator the same rate in any order, and a system worth less than its coin is SETTLED so every coin redeems the same share of one pool; mint and withdraw are ratio-checked against the whole debt at a fresh bounded price - with round 9 replayed in both orders as must-fail tests), or 'exchange' (an ORDER BOOK - resting limit orders, bids and asks, matching, partial fills, cancellation - NOT 'amm', which is a constant-product pool and prices every trade off two reserves: a resting order's TERMS are immutable and a partial fill writes ONE MONOTONE COUNTER and nothing else, so the row is never delete-and-recreated and no counterparty can restart the maker's cancel clock - which is exactly how adversary round 12 drained an order book built freehand on this server's advice, half a maker's inventory to a party that posted no order; NO OPERATION NAMES A COUNTERPARTY, so the book matches best price then longest rested then lowest id and nobody chooses who is filled at a stale price; and a crossing order is filled at the resting price in the block it is signed - with the round-12 grind and its setup replayed as must-fail tests), or 'bridge' (a ONE-WAY BRIDGE RECEIVER and its exit - mint a wrapped unit when a burn is proved on a source chain, burn it again on the way out - NOT 'ft4', which is where this ask used to go and where adversary round 14's 10x mint came from: the processed-burns registry is keyed by the burn's identity on the source chain (source_chain, source_tx, log_index), so one burn has ONE row and a repeat is refused by the database rather than by a check somebody has to remember; the row BINDS what the burn pays, with recipient and amount written once by the attestation that opens it and read from the ROW by the mint, so a later attestation cannot substitute either; a relayer SET with a threshold replaces the single key, one relayer counts once per burn, and the mint is capped per period and in total. Read its last guard before copying any invariant: a TRANSFER-conservation test is structurally blind to a mint - it was exact at every step of the 10x mint - so this template's invariant compares what was MINTED against the burns it ACCEPTED; both round-14 drains ship as must-fail tests), or 'escrow' (a TWO-PARTY OTC SWAP with a timeout - an escrow between a maker and ONE named counterparty, a peer-to-peer trade, an atomic swap - NOT 'amm', which is where this ask used to go because 'swap' is in that keyword list and where adversary round 15's build came from: a swap SETTLES IN FULL OR NOT AT ALL, so a partial fill cannot re-create the remainder as a new row with a fresh timeout (round 15 held a one-hour offer open for six hours, one unit at a time, with 94 of the maker's 100 units still escrowed); the terms and the deadline are immutable fields written once and NO OPERATION WRITES A TIMESTAMP; the offer is REVOCABLE IN ANY BLOCK, so the window is not an option the maker wrote for free; after the deadline either party may close it and the escrowed leg goes home to whoever escrowed it; and conservation is asserted across BOTH assets - both round-15 drains ship as must-fail tests), or 'insurance' (a MUTUAL POOL WITH CLAIMS - premiums bought against cover, claims paid out of a common reserve, a refund when a member leaves - NOT 'ft4', which is where this ask used to go on the word 'payment' and where adversary round 17's two drains came from: there is ONE helper that returns a premium and every exit path calls it, and what it returns is the premium LESS WHAT THE POLICY HAS ALREADY BEEN PAID, so refunding a spent premium has nowhere to be written; NOTHING IS PAID INSIDE A CLAIM - a claim round snapshots the reserve and the total claimed and pays every claimant reserve * claim / total_claimed, so transaction order moves nothing - and a REFUND IS PRO RATA for the same reason; cover is bounded by the reserve times a configured multiplier with a positive default and the premium is a fraction of the cover. It does NOT decide whether a loss happened: every guard is about what a claim is PAID, never whether it is true). An unknown template name SCAFFOLDS the closest shipped template - its files, and its name in the `template` field - with a warning naming what it does NOT cover; when no shipped template is close, NOTHING is scaffolded: ok:false, isError, and no `files` at all.")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Scaffold Chromia dapp",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "name" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "pins" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "forbidden" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "files" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "moduleArgs" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive("The module args this template's SHIPPED tests need, ready to paste into run_rell_tests{moduleArgs}: chromia.yml's blockchains.<name>.moduleArgs merged with its test.moduleArgs, keyed by Rell module name. Empty {} for templates that need none. Assembling this by hand used to be a mandatory second 36-second call.")
                        )
                    ),
                    "nextCall" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The literal next call that makes the shipped tests green on the FIRST run.")
                        )
                    ),
                    "ok" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive("false when nothing was scaffolded (an unknown template with no close shipped match); `files` is then absent and `warnings` says what to do instead.")
                        )
                    ),
                    "template" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The template actually scaffolded. For an unknown name this is the CLOSEST shipped template, whose files are what you get - never `hello` as a silent substitute.")
                        )
                    ),
                    "warnings" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ok", "name", "template", "pins", "forbidden", "notes")
        )
    )


    fun validateChromiaYmlTool() = Tool(
        name = "validate_chromia_yml",
        description = """
            Validate a chromia.yml string against production pins. Checks compile.rellVersion (semver
            N.N.N), blockchains.*.module (a module name, not a file path), merkle_hash_version == 2, the
            accepted webStatic key, and forbids FT4 admin / ras_open modules in libs and code.
            MISSING pins are warnings by default - chr builds official configs that omit them; pass
            strict:true to make them errors. A rellVersion newer than the CLI-bundled compiler, or a
            present-but-wrong merkle value, is always an error.
            Deployments: reserved names mainnet / testnet auto-fill the Directory brid + url; custom
            names require both, and a Directory Chain BRID that is not 64 hex is an error.
            Returns structured {ok, errors[], warnings[]}. Does not run chr or send signed transactions.
            Every rule and warning, including moduleArgs key handling:
            describe_tool{tool:"validate_chromia_yml"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Optional Rell sources (path -> source), the same map rell_check and run_rell_tests take. PASS THEM: a yml can only be checked against what the CODE needs when the code is here - a module declaring `struct module_args` the yml never sets, or an FT4 import with no lib.ft4.core.accounts configuration (rate_limit, auth_descriptor, auth_flags.mandatory), are errors `chr build` would otherwise be the first to report. `rell` and `source` are accepted as aliases. Without sources the validator checks the yml alone, exactly as before.")
                        )
                    ),
                    "rell" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive("Alias for `files`.")
                        )
                    ),
                    "source" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("A single Rell source, when there is only one file.")
                        )
                    ),
                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Full chromia.yml text to validate")
                        )
                    ),
                    "strict" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Default false. true makes MISSING production pins (compile.rellVersion, " +
                                    "merkle_hash_version) errors instead of warnings - use for release gates."
                            )
                        )
                    )
                )
            ),
            required = listOf("yaml")
        ),
        title = "Validate chromia.yml",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ok" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "errors" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "warnings" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "pins" to JsonObject(mapOf("type" to JsonPrimitive("object")))
                )
            ),
            required = listOf("ok", "errors", "warnings")
        )
    )

    fun ft4ModuleArgsTool() = Tool(
        name = "ft4_module_args",
        description = """
            Return a production-correct FT4 v1.1.0r API 1 module_args + libs block for chromia.yml.
            To run FT4 tests, pass run_rell_tests moduleArgs as one JSON object keyed by module name
            that merges this block with chromia.yml's test.moduleArgs (the test-only admin keys);
            byte_array values may be the yml's x"..." literal, 0x..., or bare hex.
            require_mandatory_flags only on the main auth descriptor. DEFAULT_LOGIN_CONFIG_NAME is "default".
            Official /build/ft4/configuration-values + /setup/imports (200). Use auth_descriptor.max_rules, not stale max_auth_descriptor_rules.
            NEVER emits lib.ft4.admin, admin.crosschain, ras_open, or ras_transfer_open.
            Does not send signed transactions. Confirm keys with fetch_docs.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "name" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Optional dapp / chain name (lowercase [a-z][a-z0-9_]{0,31}). Default: hello"
                            )
                        )
                    ),
                    "includeIccf" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "If true, emit official ICCF protocol-page library-chain com.chromia.iccf 1.90.1 plus IccfGTXModule (net.postchain.d1.iccf.IccfGTXModule). Also documents official FT4-setup git pin 1.87.0. Default: false"
                            )
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "FT4 module_args",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "name" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "libs" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "moduleArgs" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "gtx" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "DEFAULT_LOGIN_CONFIG_NAME" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "require_mandatory_flags" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "forbidden" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("name", "libs", "moduleArgs", "yaml", "forbidden", "notes")
        )
    )

    /** Names of the static help tools that the chromia_help gateway covers. */
    val HELP_TOOL_NAMES: Set<String> = setOf(
        "chr_build_help", "chr_repl_help", "chr_tools_help", "chr_seeder_help",
        "blockchain_properties_help", "chr_eif_help", "chromia_yml_definitions_help",
        "chr_completion_help", "chromia_project_structure_help", "chr_multi_signature_help",
        "chr_deploy_help", "chr_node_help", "chr_query_help", "vault_lease_help",
        "chr_generate_client_help", "chromia_docs_yml_help", "chromia_cookbook_help",
        "chr_key_id_help", "chromia_language_clients_help", "chromia_rell_language_help",
        "chromia_rell_types_help", "chromia_rell_expressions_help", "chromia_rell_statements_help",
        "chromia_rell_database_help", "chromia_rell_systemlib_help", "chromia_rell_practices_help",
        "chromia_ft4_queries_help", "chromia_integrations_help", "chromia_vector_search_help",
        "chr_library_help", "chr_create_rell_dapp_help"
    )

    /**
     * Topics the chromia_help gateway answers: the static help tools plus the
     * long-form tool guidance F9 moved out of `tools/list` ([ToolDocs.TOPICS]).
     */
    val HELP_TOPIC_NAMES: Set<String> get() = HELP_TOOL_NAMES + ToolDocs.TOPICS

    fun chromiaHelpTool() = Tool(
        name = "chromia_help",
        description = """
            One gateway to the full static Chromia help catalog: CLI commands (build, deploy, node,
            query, repl, keys, library, completion, seeder, eif, multisig, client generation,
            create-rell-dapp), chromia.yml definitions and docs config, project structure, vault
            leases, the Rell language (types, expressions, statements, database ops, system library,
            best practices), FT4 queries, integrations, vector search, and the cookbook - plus the
            long-form guidance of the security tools (scaffold_dapp, verify_guards,
            rell_security_check, ...) as a topic under the tool's own name.
            No arguments lists the topics. A topic returns that topic's TABLE OF CONTENTS - the
            section names with their byte cost - because one help payload can cost more context
            than the whole tool catalog. Add `section` for that section, or section:"all" for the
            entire payload (the same bytes the individual *_help tool returns).
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "topic" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Help topic, e.g. 'chr_deploy_help' (or 'chr_deploy'), or a tool name such as 'verify_guards' for that tool's full description. Aliases: 'security', 'best_practices' and 'best-practices' map to chromia_rell_practices_help. Omit to get the full topic list."),
                            "enum" to kotlinx.serialization.json.JsonArray(HELP_TOPIC_NAMES.sorted().map { JsonPrimitive(it) })
                        )
                    ),
                    "section" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("One section name from the topic's table of contents (dotted for a nested one, e.g. 'commands.deployment'), or \"all\" for the whole payload. Omit to get the table of contents with each section's byte cost.")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Chromia help catalog",
        annotations = null,
        outputSchema = null
    )

    fun describeToolTool() = Tool(
        name = "describe_tool",
        description = """
            The FULL description and input schema of any tool on this server, by name.
            `tools/list` advertises a short form so the catalog fits an agent's context (audit F9:
            the full catalog cost ~29k tokens before the first useful call), and compact mode
            (CHROMIA_MCP_COMPACT_TOOLS=true) advertises headlines only. Nothing was deleted - the
            long form of scaffold_dapp, verify_guards, rell_security_check, deploy_testnet_chain
            and the rest is returned here verbatim, and also as chromia_help{topic:"<tool>"}.
            Call it with no arguments for the list of tool names.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "tool" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Tool name, e.g. 'scaffold_dapp'. Omit to list every tool name this deployment implements.")
                        )
                    ),
                    "includeSchema" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive("Include the full inputSchema and outputSchema (default true). Pass false for the prose only.")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Describe a tool",
        annotations = null,
        outputSchema = null
    )

    fun rellCheckTool() = Tool(
        name = "rell_check",
        description = """
            Compile Rell source code with the real Rell compiler (same one the Chromia CLI embeds)
            and get structured diagnostics back - no chr installation needed.
            This is the write -> compile -> fix loop for building on Chromia:
            1. Write or edit Rell code
            2. Call rell_check with the code
            3. Fix the first reported error (file, line, column, message) and repeat until ok=true
            Pass a single module as `source` (compiled as main.rell), or a whole project as `files`
            ({"main.rell": "...", "lib/util.rell": "..."}). Module args declared in the code are
            not required for the check. Compilation runs in-process on temp files; nothing is
            deployed and no network is used.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Map of relative .rell file paths to file contents for multi-file projects, e.g. {\"main.rell\": \"module; ...\"}. Paths are relative to the Rell source root - drop the project's src/ prefix (a leading ./ or src/ is normalized away automatically).")
                        )
                    ),
                    "source" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Rell source for a single-file check; compiled as main.rell. Ignored when `files` is given.")
                        )
                    ),
                    "modules" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Optional list of app module names to compile. Omit to compile all modules found.")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Compile-check Rell code",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ok" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "modules" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "errors" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "warnings" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ok", "modules", "errors", "warnings", "notes")
        )
    )

    fun runRellTestsTool() = Tool(
        name = "run_rell_tests",
        description = """
            Execute Rell tests in-process with the embedded Rell test runner (same engine the Chromia
            CLI wraps) and return per-case pass/fail results - no chr installation needed.
            Step 3 of the agent verification loop: rell_check (it compiles) -> rell_security_check (it
            is secure) -> run_rell_tests (it behaves) -> verify_guards (every guard you rely on is
            LOAD-BEARING, i.e. its must-fail test goes red without it because the attack lands).
            Pass `files` including at least one file starting with `@test module;` whose test functions
            are named test_*. Tests that touch entities/database need PostgreSQL via
            CHROMIA_TEST_DATABASE_URL on the server; pure-logic tests run without it.
            Happy-path tests are not enough: for any dapp that holds value also ship INVARIANT tests -
            conservation, no-negative-balance, and authorization via
            rell.test.tx()...run_must_fail("message"). scaffold_dapp ships runnable examples to copy.
            Chasing one red case? Pass `tests` (same as `chr test --tests`) to run only the matching
            functions. Nothing is deployed; sources run in a temp directory and are deleted afterwards.
            Full guidance: describe_tool{tool:"run_rell_tests"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Map of relative .rell paths to contents: app modules plus at least one @test module, e.g. {\"main.rell\": \"module; ...\", \"main_test.rell\": \"@test module; import main; function test_x() { ... }\"}. Paths are relative to the Rell source root - drop the project's src/ prefix (a leading ./ or src/ is normalized away automatically).")
                        )
                    ),
                    "tests" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Optional test selection, `chr test --tests` semantics: each entry is a glob (* and ?) matched against the WHOLE function name (test_first_deposit), the qualified module:function (main_test:test_first_deposit) or the test module name; several entries union. Omit to run every test. A filter that matches nothing returns ok=false and lists the test functions it could have matched. A single comma-separated string is accepted too.")
                        )
                    ),
                    "moduleArgs" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive("Optional module_args by module name. YOU RARELY NEED TO BUILD THIS BY HAND: pass chromia.yml (inside `files`, or as `yaml`) and this tool merges blockchains.<name>.moduleArgs with test.moduleArgs for you, exactly as `chr test` does; scaffold_dapp also returns the finished object as its own top-level `moduleArgs` field - copy that verbatim. Shape: {\"lib.ft4.core.accounts\": {\"auth_flags\": {\"mandatory\": [\"A\",\"T\"]}}}. Required to exercise real FT4 operations in tests: use ft4_module_args for production-correct values, and when using lib.ft4.test.core helpers (register_alice etc.) the test-only admin keys - lib.ft4.core.admin {admin_pubkey} and lib.ft4.test.core.auth {admin_priv_key} (FT4's published test keys) - must be present too, or every tx fails with 'Unable to create GTX module'. PRECEDENCE, and it is not a merge: if `moduleArgs` is present AT ALL - `{}` included - it is the WHOLE set the run gets and the yml contributes nothing; the yml is read only when `moduleArgs` is omitted. `moduleArgs: {}` therefore means RUN WITH NO MODULE ARGS, not \"use whatever the yml says\". The answer's `moduleArgsSource` and `moduleArgsUsed` fields always say which set the run actually had. byte_array / pubkey values may be the yml's x\"02C4...\" literal, 0x02c4..., or bare hex - all three decode to bytes.")
                        )
                    ),
                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional chromia.yml. ONLY when `moduleArgs` is omitted entirely, the module args are read from it - blockchains.<name>.moduleArgs merged with test.moduleArgs, the same merge `chr test` performs. An explicit `moduleArgs` (even an empty object) overrides this file completely - the two are never merged. A chromia.yml passed inside `files` is used the same way and is not compiled.")
                        )
                    )
                )
            ),
            required = listOf("files")
        ),
        title = "Run Rell tests",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ok" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "total" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "passed" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "failed" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "cases" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "prints" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("print()/log() output captured from the tests (capped); omitted when the tests print nothing.")
                        )
                    ),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "moduleArgsSource" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Present only when the module args were read from a chromia.yml you passed instead of from the `moduleArgs` argument.")
                        )
                    ),
                    "moduleArgsUsed" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive("The merged module args the run actually used, when they were derived from chromia.yml - paste-able as `moduleArgs` on a later call.")
                        )
                    )
                )
            ),
            required = listOf("ok", "total", "passed", "failed", "cases", "notes")
        )
    )

    fun localChainUpTool() = Tool(
        name = "local_chain_up",
        description = """
            Stand up a REAL local Chromia chain from Rell sources - in-process, zero keys, zero funds,
            zero human steps. Compiles the sources into a blockchain configuration, runs it on the
            embedded Postchain engine against the server's PostgreSQL, and then
            serves a subset of the Postchain REST API on 127.0.0.1:
            GET /brid/iid_0, GET+POST /query/{brid}, POST /query_gtv/{brid}, POST /tx/{brid},
            and GET /tx/{brid}/{txRid}/status - block and confirmation-proof endpoints are NOT served.
            Last step of the agent loop: rell_check -> rell_security_check -> run_rell_tests ->
            verify_guards -> local_chain_up. Returns the BRID and apiUrl.
            actions: "up" (default; requires `files`), "status", "down". One chain at a time, auto-stops
            after ttlSeconds, in a dedicated PostgreSQL schema wiped on every start; calling up again
            with identical inputs returns the running chain, and a change to the sources,
            moduleArgs, or databaseUrl restarts it.
            Needs PostgreSQL via CHROMIA_TEST_DATABASE_URL (or `databaseUrl`).
            How to query it and submit transactions: describe_tool{tool:"local_chain_up"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Map of relative .rell paths to contents (same convention as rell_check), e.g. {\"main.rell\": \"module; ...\"}. Required for action \"up\". Paths are relative to the Rell source root - a leading ./ or src/ is normalized away.")
                        )
                    ),
                    "action" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "enum" to kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive("up"), JsonPrimitive("down"), JsonPrimitive("status"))),
                            "description" to JsonPrimitive("\"up\" starts (or returns) the chain (default), \"status\" reports the running chain, \"down\" stops it now.")
                        )
                    ),
                    "moduleArgs" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive("Optional module_args by module name, mirroring chromia.yml, e.g. {\"lib.ft4.core.accounts\": {...}} - required for FT4 dapps (use ft4_module_args for production-correct values).")
                        )
                    ),
                    "ttlSeconds" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Auto-stop after this many seconds (default 1800, clamped to 30..7200). Call up again to extend.")
                        )
                    ),
                    "apiPort" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("REST API port on 127.0.0.1 (default: first free port in 7741..7999).")
                        )
                    ),
                    "databaseUrl" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional PostgreSQL JDBC URL override (jdbc:postgresql://host:port/db?user=...&password=...); defaults to the server's CHROMIA_TEST_DATABASE_URL.")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Run a local Chromia chain",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ok" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "status" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("started | already_running | running | stopped | not_running (a failed start is a tool error, not a status)")
                        )
                    ),
                    "brid" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "apiUrl" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "chainId" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "nodePubkey" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "expiresInSeconds" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ok", "status", "notes")
        )
    )

    fun verifyGuardsTool() = Tool(
        name = "verify_guards",
        description = """
            Prove a guard in YOUR dapp is load-bearing: a must-fail test is evidence only if it goes red
            without it, BECAUSE THE ATTACK LANDED. Per {guard,test}: the test must PASS as given, the
            guard is replaced (default: deleted), that test alone re-runs, the verdict is WHY it failed.
            TWO shapes: one must-fail statement on the guard's declaration, or one must-hold plus an
            assertion; else ambiguous_refusal. It reaches it through
            helpers in ANY test module, via imports and aliases, up to 16 calls deep - every import form
            the compiler takes, exact `.{ }` and relative too, plus NAMESPACES, in the COMPILER'S order:
            enclosing namespace, then module, then imports, a member has no bare name - a qualifier the
            CALLING module binds to a test module is a HELPER. Helper PARAMETERS bind to call ARGUMENTS.
            Its operations are counted structurally (rell.test.tx(), .op(), block .tx() args): any
            count but one, or one it cannot read, is ambiguous_refusal. A replacement's own messages are
            attributed like the guard's; stillRefused/attackLanded are read in the must-hold shape only.
            ok=true only when EVERY guard is load_bearing; says nothing about guards you did not name.
            Full: describe_tool{tool:"verify_guards"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Map of relative .rell paths to contents: the production module(s) AND the @test module holding the must-fail test, exactly as you would pass them to run_rell_tests.")
                        )
                    ),
                    "guards" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("object"),
                                    "properties" to JsonObject(
                                        mapOf(
                                            "guard" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("The guard line, VERBATIM as it appears in your source, e.g. require(p.balance >= amount, \"insufficient\");"))),
                                            "test" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("The must-fail test function that depends on this guard, e.g. test_overdraft_must_fail. Only this test is run against the mutant."))),
                                            "replacement" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("Optional text to put in the guard's place. Default deletes the line. Use it to WEAKEN a guard (e.g. turn an equality into a 2% band) rather than remove it."))),
                                            "alsoRemove" to JsonObject(mapOf("type" to JsonPrimitive("array"), "items" to JsonObject(mapOf("type" to JsonPrimitive("string"))), "description" to JsonPrimitive("Optional further guard lines (verbatim) to strip in the same mutant - defence in depth that would otherwise still refuse the attack."))),
                                            "stillRefused" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("Optional error fragment that means the attack was STILL refused (e.g. the message of another require). SHAPE B ONLY, the must-hold test: its presence there yields still_refused instead of load_bearing. It changes no SHAPE A verdict and cannot - a must-fail red either says \"did not fail\" (the transaction went through) or carries the frame of the one operation it ran, which is a declaration this guard runs in, so the runner has already answered which it is."))),
                                            "attackLanded" to JsonObject(mapOf("type" to JsonPrimitive("string"), "description" to JsonPrimitive("Optional error fragment that proves the attack SUCCEEDED. The default \"did not fail\" - what run_must_fail reports when the transaction it expected to fail went through - always counts as the attack landing, in both shapes, whatever you pass. A CUSTOM fragment is read in SHAPE B ONLY, the must-hold test (e.g. \"expected\" when the test's own assert_equals is what should trip); there its absence yields red_for_another_reason. It changes no SHAPE A verdict.")))
                                        )
                                    ),
                                    "required" to JsonArray(listOf(JsonPrimitive("guard"), JsonPrimitive("test")))
                                )
                            ),
                            "description" to JsonPrimitive("One entry per guard to prove. Each names the guard line and the must-fail test that depends on it.")
                        )
                    ),
                    "moduleArgs" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive("Optional module_args by module name, exactly as for run_rell_tests (moduleArgs merged with test.moduleArgs, keyed by module). Required for FT4 dapps or every mutant fails with 'Unable to create GTX module' and the verdict is environmental.")
                        )
                    )
                )
            ),
            required = listOf("files", "guards")
        ),
        title = "Verify guards are load-bearing",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ok" to JsonObject(mapOf("type" to JsonPrimitive("boolean"), "description" to JsonPrimitive("true only when every named guard is load_bearing"))),
                    "guards" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "loadBearing" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "results" to JsonObject(mapOf("type" to JsonPrimitive("array"), "description" to JsonPrimitive("Per guard: {guard, test, verdict, loadBearing, evidence}"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ok", "guards", "loadBearing", "results", "notes")
        )
    )

    fun rellSecurityCheckTool() = Tool(
        name = "rell_security_check",
        description = """
            Static security review of Rell code, run after a successful compile (compiles first via the
            embedded Rell compiler; uncompilable code returns the compile errors instead). Rules:
            banned admin modules and open registration/transfer strategies (CRITICAL); unauthenticated
            state writes, the confused deputy, phantom is_signer gates, empty-where update/delete and
            key-shaped hex literals (HIGH); empty auth flags, public 64-hex constants and unvalidated
            parameters (MEDIUM). Findings are line-anchored with a concrete fix; ok=true means no
            CRITICAL/HIGH.
            ok=true is NOT economic soundness. Static rules structurally cannot see missing
            AUTHORIZATION, unbacked minting, missing quorum/stake/timelock, funds with no withdrawal or
            timeout path, i64 overflow, whether an outcome meant to be unpredictable actually is, or
            TRANSACTION ORDERING / MEV. Prove those with invariant tests via run_rell_tests, then
            verify_guards.
            The full rule list, the test-surface downgrade, lib/** handling and allowAdminModules:
            describe_tool{tool:"rell_security_check"} or chromia_help{topic:"rell_security_check"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Map of relative .rell file paths to file contents for multi-file projects.")
                        )
                    ),
                    "source" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Rell source for a single-file review; analyzed as main.rell. Ignored when `files` is given.")
                        )
                    ),
                    "allowAdminModules" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Default false. true downgrades banned admin-module/open-strategy findings from " +
                                    "CRITICAL to MEDIUM (non-blocking). For admin/ops tooling builds only - never " +
                                    "enable for a production dApp."
                            )
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Security-review Rell code",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ok" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "operationsScanned" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "findings" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ok", "operationsScanned", "findings", "notes")
        )
    )

    fun writeDeploymentConfigTool() = Tool(
        name = "write_deployment_config",
        description = """
            Return the chromia.yml deployments.<network> block that Chromia CLI 0.33.x expects
            (url, official Directory Chain BRID; chains omitted on purpose - a first
            chr deployment create must not carry it, and a placeholder null value is rejected by chr).
            PASS YOUR EXISTING chromia.yml AS `yaml`: the block is then MERGED into it - moduleArgs,
            test.moduleArgs, libs, compile pins and comments all preserved - and comes back as
            `chromia_yml`. Without `yaml` you get the block alone and no full file: this tool does
            not regenerate a project config. (It used to, from the `hello` scaffold, and adopting
            that output deleted an FT4 project's moduleArgs including auth_flags.mandatory, its
            test.moduleArgs block and libs.iccf - silently, with every gate green. Audit F7.)
            An existing deployments.<network> block is replaced, but a real container id and any
            chains map in it are carried over.
            network must be testnet or mainnet. Does not invent a BRID.
            Since CLI 0.30.0, chr deployment create writes deployments.<net>.chains back into chromia.yml;
            on 0.29.x add chains.<name>: x"<dapp rid>" by hand after the first create.
            Does not send signed transactions and does not run chr.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Target network: testnet or mainnet")
                        )
                    ),
                    "name" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Optional dapp / chain name (lowercase [a-z][a-z0-9_]{0,31}). Default: hello. `chain` is accepted as an alias."
                            )
                        )
                    ),
                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Your project's existing chromia.yml. PASS IT: the deployments.<network> block is MERGED into it and every other key - moduleArgs, test.moduleArgs, libs, compile pins, comments - is preserved verbatim, and the merged file comes back as `chromia_yml`. Without it you get only the `yaml` block to paste yourself; this tool never regenerates a project file. `chromiaYml` is accepted as an alias."
                            )
                        )
                    )
                )
            ),
            required = listOf("network")
        ),
        title = "Write deployment config",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "name" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "url" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "brid" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The deployments.<network> block alone.")
                        )
                    ),
                    "chromia_yml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Present ONLY when you passed `yaml`: that same file with the deployments block merged in and every other key preserved. It is never a file this tool made up.")
                        )
                    ),
                    "merge_note" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Present when no `yaml` was passed: says why there is no full chromia.yml to adopt.")
                        )
                    ),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("network", "name", "url", "brid", "yaml", "notes")
        )
    )

    fun chrBuildHelpTool() = Tool(
        name = "chr_build_help",
        description = """
            Return official Chromia CLI 0.33.x install / build / test commands and the expected chromia.yml shape.
            Official /build/cli/introduction + /cli-release-notes (200): docs latest 0.30.0 vs source tags 0.33.x.
            Does not shell out to chr and does not send signed transactions.
            Commands: chr install, chr build, chr code check, chr test. chr repl --sql-log (see chr_repl_help).
            Java 21+, Postgres 16+.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI build help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "install" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "chromia_yml_shape" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "install", "chromia_yml_shape", "notes")
        )
    )

    fun chrDeployHelpTool() = Tool(
        name = "chr_deploy_help",
        description = """
            Official Chromia CLI 0.33.x `chr deployment` flag help: create / update / inspect plus
            read-only info / proposal list|info / voterset info|list (no key-pair flags). Includes -y,
            --key-id (reference only), the schema-compare DROP warning, and that create writes
            deployments.<net>.chains back. Also returns the official chromia.yml database / test
            snippets (Java 21+, Postgres 16+) and the official BUILD deployment pages' values -
            testnet/mainnet getting-started and deploy-dapp, Vault listing, testnet tCHR, frontend.
            Does not shell out to chr, generate a key, or send signed transactions. Skips
            vote/propose/pause/resume/remove and the hidden lease-info / remove-container commands.
            Page-by-page provenance notes: describe_tool{tool:"chr_deploy_help"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI deploy help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "write_back" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "schema_compare" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "drop_warning" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "container" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "database" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "test" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "write_back", "schema_compare", "notes")
        )
    )

    fun chrNodeHelpTool() = Tool(
        name = "chr_node_help",
        description = """
            Official Chromia CLI 0.33.x chr node start / update flag help.
            Covers --wipe / --no-wipe, Postgres 16+ requirement, default local API
            http://localhost:7740, and how chr node start relates to chr build / chr test.
            Does not start a node, generate a key, invent a BRID, run chr, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI node help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "wipe" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "relation" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "default_api_url" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "wipe", "relation", "notes")
        )
    )

    fun chrQueryHelpTool() = Tool(
        name = "chr_query_help",
        description = """
            Official Chromia CLI 0.33.x read-only chr query flag help.
            Targets a local chr node start (default http://localhost:7740) or a named
            chromia.yml deployment / --mainnet / --testnet. Does not sign or execute a transaction.
            Official tx command page is HELP ONLY (official flags + URL; skip sample BRID hex).
            Does not run chr, generate a key, send a signed tx, or invent a BRID.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI query help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "target" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "target", "notes")
        )
    )

    fun vaultLeaseHelpTool() = Tool(
        name = "vault_lease_help",
        description = """
            Official Vault / PMC container lease workflow for Chromia CLI 0.33.x testnet/mainnet.
            How to obtain a Container ID and set deployments.<net>.container.
            Official Directory Chain BRIDs only. Does not invent a lease/container id,
            generate a key, run chr, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Vault / PMC lease help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "vault" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "yaml_testnet" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "yaml_mainnet" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "workflow" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "vault", "yaml_testnet", "workflow", "notes")
        )
    )


    fun chrGenerateClientHelpTool() = Tool(
        name = "chr_generate_client_help",
        description = """
            Official Chromia CLI 0.33.x chr generate client-stubs / graph / docs-site help
            plus official postchain-client / FT4 query-only wiring (createClient, directoryNodeUrlPool, blockchainRid).
            Languages: kotlin, typescript, javascript, python. Not a top-level chr generate-client.
            chromia.yml docs: keys live in chromia_docs_yml_help (project-config only).
            Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI generate-client help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "languages" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "docs_yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "docs_keys" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "docs_yaml", "notes")
        )
    )

    fun chrLibraryHelpTool() = Tool(
        name = "chr_library_help",
        description = """
            Official Chromia CLI 0.33.x public chr library help (install / list / view / versions).
            chr install is an alias of chr library install. Documents both official chromia.yml library shapes (library-chain and git).
            Official ICCF: library-chain com.chromia.iccf 1.90.1 (protocol page) and git 1.87.0 (FT4 setup).
            Does not invent a library-chain BRID, run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI library help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "library_chain_yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "git_yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "library_chain_yaml", "git_yaml", "notes")
        )
    )

    fun chrCreateRellDappHelpTool() = Tool(
        name = "chr_create_rell_dapp_help",
        description = """
            Official Chromia CLI 0.33.x chr create-rell-dapp help.
            Templates: plain, plain-multi, minimal, plain-library, asset-management.
            Optional --devcontainer. Does not run chr, write files, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI create-rell-dapp help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "templates" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "layout" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "templates", "layout", "notes")
        )
    )

    fun chrReplHelpTool() = Tool(
        name = "chr_repl_help",
        description = """
            Official Chromia CLI 0.33.x chr repl flag help.
            Interactive Rell shell: --module, --blockchain, --sql-log, --use-db, -c/--command.
            CLI 0.31.0 removed chr test --sql-log; use chr repl --sql-log --use-db --module for entity SQL
            (official /rell/analyze-rell-dapp-code).
            Does not run chr, generate a key, invent a BRID, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI repl help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "notes")
        )
    )

    fun chrToolsHelpTool() = Tool(
        name = "chr_tools_help",
        description = """
            Official Chromia CLI 0.33.x chr tools help (gtv / validate-config / lib-model).
            chr gtv is the official alias of chr tools gtv. Does not run chr, generate a key,
            invent a BRID, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI tools help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "gtv_flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "validate_config_flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "lib_model_flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "gtv_flags", "notes")
        )
    )

    fun chrSeederHelpTool() = Tool(
        name = "chr_seeder_help",
        description = """
            Official Chromia CLI 0.33.x chr seeder help (init / generate).
            Early-stage local fake-data helper. Does not run chr, generate a key,
            invent a BRID, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI seeder help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "init_flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "generate_flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "init_flags", "generate_flags", "notes")
        )
    )

    fun blockchainPropertiesHelpTool() = Tool(
        name = "blockchain_properties_help",
        description = """
            Official chromia.yml blockchains.<name>.config blockchain-properties (CLI 0.33.x).
            Official keys only: gtx / blockstrategy / query timeouts plus the documented core/features/revolt lists.
            merkle_hash_version 2. Does not invent keys, run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "chromia.yml blockchain-properties help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "keys" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "config_yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "keys", "config_yaml", "notes")
        )
    )

    fun chrEifHelpTool() = Tool(
        name = "chr_eif_help",
        description = """
            Official Chromia CLI 0.33.x chr eif generate-events-config help.
            --abi, --events, --target (default build/eif-events.yaml), --format=(XML|YAML).
            Does not run chr, generate a key, invent a BRID, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI eif help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "notes")
        )
    )

    fun chromiaYmlDefinitionsHelpTool() = Tool(
        name = "chromia_yml_definitions_help",
        description = """
            Official chromia.yml definitions / YAML anchors / !include help (CLI 0.33.x).
            Official project-config examples only: definitions + &anchor / *alias,
            !include other.yml, !include other.yml#tag. Does not invent include semantics.
            Does not run chr, generate a key, invent a BRID, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "chromia.yml definitions / YAML include help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "anchors_yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "include_whole_file_yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "include_tag_yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "anchors_yaml", "include_whole_file_yaml", "include_tag_yaml", "notes")
        )
    )

    fun chrCompletionHelpTool() = Tool(
        name = "chr_completion_help",
        description = """
            Official Chromia CLI 0.33.x chr help / chr version / --generate-completion help.
            Official pages /build/cli/introduction, /cli-release-notes, /commands/help and /version (200).
            Docs-site latest listed CLI 0.30.0 (2026-02-27); source tags 0.33.x — state both.
            Official pages /build/cli/commands/help and /version (200): usage + -h/--help only.
            bash|zsh|fish completion scripts and two-letter shortcuts (chr de cr).
            Documents skipped hidden verbs: fetch-config, deployment lease-info, remove-container.
            Does not run chr, generate a key, invent a BRID, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI completion help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "skipped" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "skipped", "notes")
        )
    )

    fun chrMultiSignatureHelpTool() = Tool(
        name = "chr_multi_signature_help",
        description = """
            Official Chromia CLI 0.33.x read-only chr multi-signature view help.
            Only -f/--file. Does not document create, sign, or send.
            Does not run chr, generate a key, invent a BRID, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI multi-signature view help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "skipped" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "flags", "skipped", "notes")
        )
    )

    fun chromiaProjectStructureHelpTool() = Tool(
        name = "chromia_project_structure_help",
        description = """
            Official Chromia project-structure and Rell modules layout help (CLI 0.33.x).
            create-rell-dapp layout, multi-file directory modules, recommended app/ files,
            official import forms. blockchains.<name>.module is a module name, never a path.
            Does not run chr, generate a key, invent a BRID, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "chromia.yml project-structure / Rell modules help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "create_rell_dapp_layout" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "multi_file_layout" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "recommended_app_layout" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "import_examples" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "create_rell_dapp_layout", "multi_file_layout", "notes")
        )
    )

    fun chromiaDocsYmlHelpTool() = Tool(
        name = "chromia_docs_yml_help",
        description = """
            Official chromia.yml docs: section for chr generate docs-site (CLI 0.33.x).
            Official project-config keys only: title, footerMessage, customStyleSheets,
            customAssets, additionalContent, sourceLink.remoteUrl, sourceLink.remoteLineSuffix
            (GitHub/GitLab #L, Bitbucket #lines-). Does not invent theme/nav/logo.
            Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "chromia.yml docs section help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "keys" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "line_suffixes" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "docs_yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "keys", "docs_yaml", "notes")
        )
    )

    fun chromiaCookbookHelpTool() = Tool(
        name = "chromia_cookbook_help",
        description = """
            Official Chromia BUILD cookbook help for building a dapp: queries and query creation, client
            reads, account balance, account and transaction creation, running operations, the CLI
            recipe, and the /rell/tests builders, asserts and @disabled.
            Official pages only. Recipes that sign a live transaction, cookbook-only flags, non-schema
            keys and printed sample keys are skipped.
            Does not run chr, generate a key, or send signed transactions.
            Per-recipe provenance: describe_tool{tool:"chromia_cookbook_help"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia cookbook help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "commands" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "pages" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "commands", "pages", "notes")
        )
    )

    fun chrKeyIdHelpTool() = Tool(
        name = "chr_key_id_help",
        description = """
            Official Chromia CLI 0.33.x existing-key reference only (--key-id / key.id precedence).
            Official keygen command page is HELP ONLY (official flags + URL).
            Does not generate a key, print a private key, print a sample key, run chr, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI existing-key reference",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "flags" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "precedence" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "flags", "precedence", "notes")
        )
    )


    fun chromiaLanguageClientsHelpTool() = Tool(
        name = "chromia_language_clients_help",
        description = """
            Official Chromia BUILD query-only C# / Go / Rust / React Kit / REST client wiring.
            Official pages only. JS/TS, Kotlin, Python, and FT4 local reads live on chr_generate_client_help.
            Official Filehub work getFile, MCP setup, bridge checkAllowance. Skips signed txs, key generation, FilehubAdministrator writes, MCP explorer-dump sample BRIDs, and invented package ids.
            Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia language clients help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "packages" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "pages" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "packages", "pages", "notes")
        )
    )



    fun chromiaRellTypesHelpTool() = Tool(
        name = "chromia_rell_types_help",
        description = """
            Official Rell type-system help (simple, collection, complex, iterables, sub-types, virtual).
            Quotes docs.chromia.com/rell type pages only. Official slug is sub-types (not subtypes).
            Rell pin 0.16.7. Definition syntax lives on chromia_rell_language_help.
            Does not invent types, run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell types help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "simple_types" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "collection_example" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "simple_types", "notes")
        )
    )

    fun chromiaRellLanguageHelpTool() = Tool(
        name = "chromia_rell_language_help",
        description = """
            Official Rell definition syntax (query / operation / entity / object / struct / enum / function / module
            plus namespace / mount / abstract / size-constraint-annotations / RellDoc / identifiers).
            Quotes docs.chromia.com/rell pages only. Rell pin 0.16.7 (docs may still list 0.16.4).
            Official Hello World query hello_world returns "Hello World!".
            Official /rell/rell-doc tags: @param @return @throws @see @since @author.
            Official /rell/releases: docs-site latest listed 0.16.4; source pin 0.16.7
            (source notes 0.16.5 / 0.16.6 / 0.16.7). Official 0.14.5 T.hash() default is V1; production pin 2.
            Size-constraint applies to parameters, struct attributes, and entity/object attributes.
            Modules/imports/layouts live on chromia_project_structure_help.
            Does not invent language features, run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell language definition help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "query_short" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "entity_example" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "hello_world_query" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "query_short", "hello_world_query", "notes")
        )
    )

    fun chromiaRellPracticesHelpTool() = Tool(
        name = "chromia_rell_practices_help",
        description = """
            Official Rell BUILD practice pages (security + best-practices).
            Quotes docs.chromia.com/rell/security and /rell/rell-best-practices only.
            chromia.yml key config.directory_chain.config_delay. FT4 rate_limit pointer.
            Composite keys, indexing, require validation, run_must_fail. BUILD / read-only.
            No exploit recipes, no signing, no key material, no proposal vote/retract.
            Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell security and best-practices help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "security_docs" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "best_practices_docs" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "security_docs", "best_practices_docs", "notes")
        )
    )

    fun chromiaFt4QueriesHelpTool() = Tool(
        name = "chromia_ft4_queries_help",
        description = """
            Official FT4 v1.1.0r / API 1 read-only query catalog (get_all_assets, get_assets_by_name,
            get_account_by_id, pagination page_size/page_cursor, does_account_require_memo).
            Official /build/ft4/prioritization (200): priority_check_v1 on gtx_api, not ft4.*.
            Official /build/ft4/terms + /intro + /setup/imports + /configuration-values + /releases/ft4 (200).
            Official /build/ft4/releases is 404. Changelog latest listed 1.1.0r; pin remains v1.1.0r / API 1.
            Queries / config only. Never emits admin / ras_open / register / transfer / auth write paths.
            Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "FT4 read-only query catalog",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "asset_queries" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "account_queries" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "pagination" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "asset_queries", "account_queries", "notes")
        )
    )

    fun chromiaIntegrationsHelpTool() = Tool(
        name = "chromia_integrations_help",
        description = """
            Official Chromia BUILD integrations hub (read-only). Memo query does_account_require_memo.
            Official child page URLs only. Does not invent package ids (C# NuGet id unpublished).
            Official token-chain query shapes (get_token_chain_constants, get_proposals_by_proposer proposer=, get_all_bridges).
            Skips exchange account-creation / transfer / memo write operations.
            Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia integrations hub help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "pages" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "queries" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "pages", "queries", "notes")
        )
    )

    fun chromiaVectorSearchHelpTool() = Tool(
        name = "chromia_vector_search_help",
        description = """
            Official BUILD vector-search (read-only). Live pages:
            /build/vector-search/overview/ and /sample-workloads (200).
            Official /build/vector-search/ is 404. /build/extensions/ is 404.
            Official BUILD pages print no module names, yml keys, or query names.
            Hard skip: ingest embeddings / ONNX. Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia vector-search help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "pages" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "capabilities" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "pages", "capabilities", "notes")
        )
    )

    fun chromiaRellExpressionsHelpTool() = Tool(
        name = "chromia_rell_expressions_help",
        description = """
            Official Rell expression help (values, operators, conditional, jump, lambda).
            Quotes docs.chromia.com/rell expression pages only. Does not invent operators.
            Rell pin 0.16.7. Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell expressions help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "official_operators" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "official_operators", "notes")
        )
    )

    fun chromiaRellStatementsHelpTool() = Tool(
        name = "chromia_rell_statements_help",
        description = """
            Official Rell statement help (val/var, assignment, if/when, for/while, break/continue).
            Quotes docs.chromia.com/rell statement pages only. Rell pin 0.16.7.
            Does not invent statements, run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell statements help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "statements" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "statements", "notes")
        )
    )

    fun chromiaRellDatabaseHelpTool() = Tool(
        name = "chromia_rell_database_help",
        description = """
            Official Rell database-language help (at / create / update / delete syntax).
            Quotes docs.chromia.com/rell database pages only. These constructs run inside operations.
            Official /build/database/getting-started + /overview (200). architecture/scaling 404.
            getting-started says chromia start — NOT a chr command; official local loop is chr node start.
            Does not document chr tx or signed send. create-copy and /database/at are 404.
            Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell database language help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "create_example" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "create_example", "notes")
        )
    )

    fun chromiaRellSystemlibHelpTool() = Tool(
        name = "chromia_rell_systemlib_help",
        description = """
            Official Rell system library help (global functions, require/error, system entities,
            system queries, plus official namespaces: chain_context, op_context, crypto HASH/VERIFY,
            rell.meta, rell.time). Quotes docs.chromia.com/rell systemlib pages only.
            Skips privkey / signing helpers and official printed sample keys.
            Rell pin 0.16.7. Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell system library help",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cli" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "global_functions" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("cli", "global_functions", "notes")
        )
    )

    fun checkDappProjectTool() = Tool(
        name = "check_dapp_project",
        description = """
            One-call project gate. Takes a chromia.yml string plus one or more .rell file contents and
            runs the FULL check: validate_chromia_yml + check_ft4_imports + rell_check (real
            compilation, FT4 imports included) + rell_security_check when it compiles. Returns combined
            {ok, errors, warnings, notes}; ok=true means the project parses, compiles, and has no
            CRITICAL/HIGH security findings.
            `yaml` is optional: when omitted, a minimal default chromia.yml at the current pins
            (rellVersion ${DappScaffold.RELL_VERSION}) is used and noted in the output.
            allowAdminModules:true (default false) downgrades banned admin-module findings from errors
            to warnings - for admin/ops tooling only, never for production dApps.
            Use this as the single pre-deploy gate instead of calling the four tools separately.
            Read-only: does not write files, run chr, generate keys, or send signed transactions.
            Per-chain compilation and lib/** handling: describe_tool{tool:"check_dapp_project"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "Alias for `rell` (same shape) - accepted because rell_check and " +
                                    "run_rell_tests name this parameter `files`. When both are present, `rell` wins."
                            )
                        )
                    ),
                    "rell" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "One .rell source string, or an object of path -> source (e.g. src/main.rell)"
                            )
                        )
                    ),
                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Full chromia.yml contents as a string. Optional: when omitted, a minimal default " +
                                    "chromia.yml at the current pins (rellVersion ${DappScaffold.RELL_VERSION}) is used and noted."
                            )
                        )
                    ),
                    "allowAdminModules" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Default false. true downgrades banned admin-module/open-strategy findings " +
                                    "(lib.ft4.admin, ras_open, ...) from errors to warnings. For admin/ops tooling " +
                                    "builds only - never enable for a production dApp."
                            )
                        )
                    )
                )
            ),
            required = listOf("files")
        ),
        title = "Check dapp project",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ok" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "errors" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                        )
                    ),
                    "warnings" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                        )
                    ),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ok", "errors", "warnings", "notes")
        )
    )

    fun checkFt4ImportsTool() = Tool(
        name = "check_ft4_imports",
        description = """
            Read-only in-memory scan of one or more .rell file contents for forbidden FT4 production imports
            (lib.ft4.admin, admin.crosschain, ras_open, ras_transfer_open, and the rest of DappScaffold.forbiddenModules).
            Official /build/ft4/setup/imports (200): public vs core; list label cross-chain is import lib.ft4.crosschain.
            @test modules are exempt (test code legitimately exercises admin modules and strategies);
            vendored-identical lib/ft4 and lib/iccf files are exempt; other lib/* files are skipped as
            third-party library code - all counted in notes.
            Returns {ok, errors, warnings, hits, forbidden}. Used by check_dapp_project.
            allowAdminModules:true (default false) downgrades forbidden-module findings from errors
            to warnings - for admin/ops tooling only, never for production dApps.
            Does not write files, run chr, generate keys, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Rell sources as a map of relative path -> source, e.g. {\"src/main.rell\": \"module; ...\"}. This is the canonical name for Rell sources on every code-taking tool here; `rell` and `source` are accepted as aliases with no warning. A single source string is accepted too (filed as main.rell).")
                        )
                    ),

                    "rell" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "One .rell source string, or an object of path -> source (e.g. src/main.rell)"
                            )
                        )
                    ),

                    "allowAdminModules" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Default false. true downgrades forbidden FT4 production module findings from " +
                                    "errors to warnings. For admin/ops tooling builds only - never enable for a " +
                                    "production dApp."
                            )
                        )
                    )
                )
            ),
            required = listOf("files")
        ),
        title = "Check FT4 imports",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ok" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "errors" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "warnings" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "hits" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "forbidden" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ok", "errors", "warnings")
        )
    )

    fun translateErrorTool() = Tool(
        name = "translate_error",
        description = """
            Translate a cryptic error from ANYWHERE in the Chromia stack into plain terms: what it
            means, the most likely cause, and the concrete next action (which MCP tool to call or
            what to change). Covers Rell compiler diagnostics, chr CLI, postchain runtime, the
            postgres under postchain, explorer/GraphQL, FT4, and this server's own messages.
            Engine: a curated ordered rule table mined from verified failures - NO LLM, NO network.
            Returns {matched, meaning, likelyCause, nextAction, relatedTools, searchTerms, notes};
            when no rule matches it says so (matched=false) and returns triage guidance plus
            docs-search terms extracted from the error - it never pretends to know.
            Paste the error verbatim (up to ~8 KB); add optional `context` like "during chr build".
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "error" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "The error text, pasted verbatim (max 8192 chars - for huge logs, paste the first diagnostic or the final 'Caused by')"
                            )
                        )
                    ),
                    "context" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Optional free text about what you were doing, e.g. 'during chr build' or 'calling run_rell_tests'"
                            )
                        )
                    )
                )
            ),
            required = listOf("error")
        ),
        title = "Translate Chromia Error",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "matched" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "ruleId" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "family" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "meaning" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "likelyCause" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "nextAction" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "relatedTools" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "searchTerms" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("matched", "meaning", "likelyCause", "nextAction")
        )
    )

    fun onboardingNextStepTool() = Tool(
        name = "onboarding_next_step",
        description = """
            State machine for the journey from nothing to a deployed Chromia dapp: report what is
            honestly done so far and get exactly ONE next action - which MCP tool to call with which
            args, or the exact human step with its URL - plus the remaining steps and human-only
            blockers. Grounded in live-verified facts: the tCHR faucet web UI requires a captcha so a
            human claims it (1000 tCHR / 7 days; `pmc economy claim-test-chr` exists for testnet but
            needs a configured provider account, so it is no keyless-agent path), the testnet
            container lease is a Vault web step priced in tCHR at lease time (observed ~35
            tCHR/SCU-week on the testnet economy chain 2026-09-01; 1-12 weeks),
            `chr deployment create/update` is headless and signed by the
            container key (POSTCHAIN_CLIENT_PUBKEY/POSTCHAIN_CLIENT_PRIVKEY env vars, Chromia's
            documented CI pattern; the key holds no funds), and mainnet needs a Vault deposit of at
            least 10 CHR plus a lease first. Absent fields mean "not done". This tool never
            generates keys or emits key material - key creation is a human `chr keygen` step.
            Read-only: does not write files, run chr, generate keys, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "hasProject" to boolProp("A dapp project (chromia.yml + Rell sources) exists"),
                    "compiles" to boolProp("check_dapp_project / rell_check reports ok:true"),
                    "securityClean" to boolProp("No CRITICAL/HIGH security findings remain"),
                    "testsPass" to boolProp("run_rell_tests reports every case passed"),
                    "hasLocalChain" to boolProp("A local node is running the dapp"),
                    "hasTestnetContainer" to boolProp(
                        "A container lease exists on the goal network's Vault (testnet lease for goal " +
                            "\"testnet\", mainnet lease for goal \"mainnet\")"
                    ),
                    "hasTestnetKey" to boolProp(
                        "A deployment keypair exists (created by a human with `chr keygen`; " +
                            "never paste the private key)"
                    ),
                    "hasDeploymentConfig" to boolProp(
                        "chromia.yml has a deployments.<network> section (write_deployment_config)"
                    ),
                    "deployedTo" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "enum" to JsonArray(OnboardingNextStep.DEPLOYED_TO.map { JsonPrimitive(it) }),
                            "description" to JsonPrimitive(
                                "Where the dapp is currently deployed (default \"none\")"
                            )
                        )
                    ),
                    "goal" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "enum" to JsonArray(OnboardingNextStep.GOALS.map { JsonPrimitive(it) }),
                            "description" to JsonPrimitive(
                                "Target of the journey (default \"testnet\"); \"local\" ends at a " +
                                    "running local chain with no keys, tokens, or Vault steps"
                            )
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Onboarding next step",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "stage" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "nextAction" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "properties" to JsonObject(
                                mapOf(
                                    "what" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                                    "who" to JsonObject(
                                        mapOf(
                                            "type" to JsonPrimitive("string"),
                                            "enum" to JsonArray(
                                                listOf(JsonPrimitive("agent"), JsonPrimitive("human"))
                                            )
                                        )
                                    ),
                                    "how" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                                    "verify" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                                )
                            )
                        )
                    ),
                    "remainingSteps" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                        )
                    ),
                    "blockers" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                        )
                    ),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("stage", "nextAction", "remainingSteps", "blockers", "notes")
        )
    )

    private fun boolProp(description: String) = JsonObject(
        mapOf(
            "type" to JsonPrimitive("boolean"),
            "description" to JsonPrimitive("$description. Absent means not done.")
        )
    )

    fun verifyDeploymentTool() = Tool(
        name = "verify_deployment",
        description = """
            Prove a deployment actually works - read-only, no keys. Given a BRID and network
            (predefined name like "testnet"/"mainnet", or a direct node URL), checks that the chain
            is known and live on that network, reads the block height twice (bounded wait, default
            2s, max 10s) to see whether it is progressing, and optionally smoke-tests one dapp query.
            Failure text is actionable: an unknown BRID means the chain is not on that network; an
            unreachable node names the network/URL as the thing to check. An idle chain that produces
            no blocks is reported live with heightProgressing:false and an explanatory note.
            The whole verification is bounded by an overall deadline (default 20s, env
            CHROMIA_MCP_VERIFY_DEADLINE_MS, capped at 45s) so it can never hang; a probe that
            outlives it returns live:false with the likely cause (usually a chain the queried
            nodes do not serve - pass the dapp's own node URL as network).
            Does not write files, run chr, generate keys, or send signed transactions.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "brid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "The blockchain RID: 64 hex chars, bare or 0x-prefixed or x\"...\" form " +
                                    "(written into chromia.yml by `chr deployment create`)"
                            )
                        )
                    ),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "\"testnet\" (default), \"mainnet\", another predefined network, or a " +
                                    "direct node URL (http/https) for custom nodes"
                            )
                        )
                    ),
                    "query" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Optional dapp query name to smoke-test (e.g. from rell.get_app_structure)"
                            )
                        )
                    ),
                    "arguments" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive(
                                "Optional arguments for the smoke-test query (same shape as chromia_dapp_query)"
                            ),
                            "properties" to JsonObject(emptyMap()),
                            "additionalProperties" to JsonPrimitive(true)
                        )
                    ),
                    "waitMs" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive(
                                "Milliseconds between the two height reads (default " +
                                    "${VerifyDeployment.DEFAULT_WAIT_MS}, clamped to 0-${VerifyDeployment.MAX_WAIT_MS})"
                            )
                        )
                    )
                )
            ),
            required = listOf("brid")
        ),
        title = "Verify deployment",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "live" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "brid" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "blockHeight" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "heightProgressing" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "queryResult" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("live", "brid", "heightProgressing", "notes")
        )
    )

    fun deploymentPreflightTool() = Tool(
        name = "deployment_preflight",
        description = """
            Catch every deployment problem BEFORE a human burns a lease step or signs anything. Given
            chromia.yml text and a deployment target name (e.g. "testnet" / "mainnet") it checks:
            (1) the deployments.<target> block - brid, url, a real lease container, matching chains;
            (2) reachability - a bounded read-only height probe of the Directory Chain BRID;
            (3) network sanity - a target whose brid or url points at the OTHER network is a HIGH
            blocker, because wrong-network deploys are unrecoverable;
            (4) the source gate when `rell` is supplied - it must compile, and for MAINNET targets
            CRITICAL/HIGH security findings are blockers (warnings for testnet);
            (5) the production pins (rellVersion, merkle_hash_version).
            Returns {ready, target, network, findings, blockers, nextAction, notes}; ready=true only with
            zero blockers - a MAINNET target without `rell` stays blocked until the source gate runs,
            while other targets can be ready with the skipped source gate explicitly called out in notes.
            When ready, nextAction is the exact chr deployment command.
            Read-only: no keys, no signing, no network writes.
            Per-check detail: describe_tool{tool:"deployment_preflight"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(

                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Full chromia.yml contents as a string (must contain the deployments.<target> block)"
                            )
                        )
                    ),

                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Deployment target - a key under deployments in the yaml, e.g. \"testnet\" or \"mainnet\". Named `network` because that is what 22 other tools on this server call it; `target` is accepted as an alias with no warning.")
                        )
                    ),

                    "target" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Alias for `network`.")
                        )
                    ),
                    "files" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "Optional Rell sources for the compile + security gate: a map of " +
                                    "path -> source, or one source string (checked as main.rell). Omitting " +
                                    "them skips the source gate (noted; a mainnet target then stays " +
                                    "blocked). `rell` and `source` are accepted as aliases, with no " +
                                    "warning and no preference - `files` is the name every other " +
                                    "code-taking tool here uses."
                            )
                        )
                    ),

                    "rell" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive("Alias for `files`, same shape.")
                        )
                    ),

                    "strict" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Pin strictness: missing compile.rellVersion / merkle_hash_version become " +
                                    "blockers. Default true for mainnet targets, false otherwise."
                            )
                        )
                    )
                )
            ),
            required = listOf("yaml", "network")
        ),
        title = "Deployment preflight",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "ready" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "target" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "mainnet | testnet | custom | unknown - classified from the target name and Directory Chain BRID"
                            )
                        )
                    ),
                    "findings" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(
                                mapOf(
                                    "type" to JsonPrimitive("object"),
                                    "properties" to JsonObject(
                                        mapOf(
                                            "severity" to JsonObject(
                                                mapOf(
                                                    "type" to JsonPrimitive("string"),
                                                    "enum" to JsonArray(
                                                        listOf("BLOCKER", "HIGH", "WARNING", "INFO").map { JsonPrimitive(it) }
                                                    )
                                                )
                                            ),
                                            "check" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                                            "message" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                                            "fix" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    "blockers" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("array"),
                            "items" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                        )
                    ),
                    "nextAction" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ready", "target", "network", "findings", "blockers", "nextAction", "notes")
        )
    )

    fun provisionTestnetContainerTool() = Tool(
        name = "provision_testnet_container",
        description = """
            Lease a dapp container on the Chromia TESTNET with no human involved. Prices the lease live
            (create_container_cost on the Economy Chain, resolved via the Directory Chain), validates the
            cluster and duration against live chain limits, and - on a live run - signs
            create_container_with_subnode_image with the SERVER-held funding key (env
            CHROMIA_TESTNET_FUNDING_PRIVKEY, or a chr keystore key id via CHROMIA_TESTNET_FUNDING_KEY_ID /
            ~/.chromia config key.id). If the funding balance cannot cover the cost it first claims from the
            on-chain testnet faucet (1000 tCHR per account per 7 days) and refuses if still short - it never
            spends more than the reported cost. Container creation is asynchronous: a live call returns the
            chain-assigned container name, or txRid to poll via statusTxRid. An ephemeral deploy keypair is
            generated per lease and held server-side; only its PUBLIC key is ever returned - private keys
            never appear in any output or error. dryRun defaults to TRUE: it reports cost, balance and
            readiness without signing or sending anything. If the funding account does not exist yet the tool
            states the exact one-time bootstrap step instead of pretending.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "cluster" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Testnet cluster to lease in (default \"blue\"; live clusters come from get_clusters)"
                            )
                        )
                    ),
                    "scu" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Container units / SCUs (default 1)")
                        )
                    ),
                    "durationWeeks" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive(
                                "Lease duration in weeks (default 2; live chain limits are 1-12)"
                            )
                        )
                    ),
                    "extraStorageGib" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("integer"),
                            "description" to JsonPrimitive("Extra storage in GiB (default 0)")
                        )
                    ),
                    "autoRenew" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Auto-renew the lease weekly from the funding account (default false)"
                            )
                        )
                    ),
                    "deployPubkey" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Optional 33-byte compressed secp256k1 PUBLIC key (66 hex chars) to set as the " +
                                    "container's deployer; omit to have the server generate an ephemeral keypair " +
                                    "and keep the private half server-side"
                            )
                        )
                    ),
                    "dryRun" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Default TRUE: price and validate everything without signing or sending. " +
                                    "Set false to actually lease (spends tCHR from the funding account)."
                            )
                        )
                    ),
                    "statusTxRid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Poll a previous live call: the 64-hex txRid it returned. Returns the ticket " +
                                    "state and the container name once created."
                            )
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Provision testnet container",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "status" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "dry_run | provisioned | submitted_pending | refused | blocked_human_step | " +
                                    "ticket_pending | ticket_failure"
                            )
                        )
                    ),
                    "costTchr" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "costRaw" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "cluster" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "scu" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "durationWeeks" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "funding" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "containerName" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "txRid" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "deployPubkey" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("PUBLIC key of the deploy keypair (never the private key)")
                        )
                    ),
                    "humanStep" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("status")
        )
    )

    fun claimTestnetTchrTool() = Tool(
        name = "claim_testnet_tchr",
        description = """
            Top up the server-held testnet funding account from the ON-CHAIN faucet operation (module
            economy_chain_test_claim_tchr on the Economy Chain): 1000 tCHR per account per 7 days,
            FT4-authenticated with the server-held key - no captcha, no website, no human. dryRun defaults
            to TRUE and reports the account, balance and claim size without sending - but the chain
            exposes no cooldown query, so a dry run CANNOT predict whether a claim will succeed. A live
            claim reports the new balance; if the account claimed within the last 7 days the tool reports
            exactly how long until the next claim (status on_cooldown) instead of failing vaguely. Key
            material never appears in any output.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "dryRun" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Default TRUE: report balance and claimability without sending the claim."
                            )
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Claim testnet tCHR",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "status" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("dry_run | claimed | on_cooldown | blocked_human_step")
                        )
                    ),
                    "accountId" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "balanceTchr" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "claimAmountTchr" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "txRid" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "nextClaimableInSeconds" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "humanStep" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("status")
        )
    )

    fun deployTestnetChainTool() = Tool(
        name = "deploy_testnet_chain",
        description = """
            Deploy dapp sources to a leased TESTNET container with no human involved. Order is fixed and
            gated: (1) rell_security_check - CRITICAL/HIGH findings refuse the deploy even on testnet;
            (2) deployment_preflight with the sources - any blocker refuses, including a module whose
            `struct module_args` has no default and no entry under blockchains.<name>.moduleArgs (the
            oracle/vault/lending/stablecoin templates leave main.oracle_pubkey deliberately unset);
            (3) only then `chr install` (when chromia.yml declares libs) followed by
            `chr deployment create|update --settings chromia.yml --network testnet --blockchain <name>`
            run headlessly and signed from the server-held deploy key; (4) the new chain BRID is read
            back and a live height probe verifies it - a fresh chain can take minutes to start, which is
            reported honestly with verify_deployment as the follow-up.
            dryRun defaults to TRUE: gates run, nothing deploys. If chr is missing or no deploy key is
            held, the tool names the exact blocked step instead of pretending. Key material never
            appears in any output.
            Generated-config rules and chr resolution: describe_tool{tool:"deploy_testnet_chain"}.
        """.trimIndent(),
        inputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive("Alias for `rell` (same shape).")
                        )
                    ),
                    "rell" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "The dapp sources: one source string (treated as main.rell) or an object of " +
                                    "path -> source. Required - the gates cannot vouch for unseen code."
                            )
                        )
                    ),
                    "blockchain" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Blockchain name in chromia.yml (default \"my_dapp\")")
                        )
                    ),
                    "container" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Container lease name from provision_testnet_container (required unless " +
                                    "chromiaYml already carries deployments.testnet.container)"
                            )
                        )
                    ),
                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Your project's chromia.yml. `yaml` is the canonical name for it on this server; `chromiaYml` is accepted as an alias with no warning. Without one a MINIMAL config is generated from the default scaffold - it carries no moduleArgs, no test.moduleArgs and no extra libs, so an FT4 project must pass its own.")
                        )
                    ),
                    "chromiaYml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Full chromia.yml contents; omit to generate one (scaffold pins + " +
                                    "deployments.testnet with the given container)"
                            )
                        )
                    ),
                    "mode" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "enum" to JsonArray(listOf(JsonPrimitive("create"), JsonPrimitive("update"))),
                            "description" to JsonPrimitive(
                                "\"create\" for a first deploy (default), \"update\" to ship new code to a chain that " +
                                    "already exists in the container: the tool reads the chain's RID from " +
                                    "deployments.testnet.chains.<name> in chromiaYml or, when absent, from the " +
                                    "Directory, and refuses when no such chain is deployed. The new code takes " +
                                    "effect at a later block height (~1-2 min); re-query before concluding it did not apply."
                            )
                        )
                    ),
                    "dryRun" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("boolean"),
                            "description" to JsonPrimitive(
                                "Default TRUE: run every gate and report the exact command without deploying."
                            )
                        )
                    )
                )
            ),
            required = listOf("files")
        ),
        title = "Deploy testnet chain",
        annotations = null,
        outputSchema = ToolSchema(
            properties = JsonObject(
                mapOf(
                    "status" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "dry_run | deployed | deployed_unverified | refused | blocked_human_step"
                            )
                        )
                    ),
                    "blockchain" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "container" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "brid" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "live" to JsonObject(mapOf("type" to JsonPrimitive("boolean"))),
                    "blockHeight" to JsonObject(mapOf("type" to JsonPrimitive("integer"))),
                    "gates" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "securityFindings" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "preflight" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "command" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "updatedChromiaYml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "humanStep" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("status")
        )
    )

    /**
     * All advertised tools. `compact = true` (env `CHROMIA_MCP_COMPACT_TOOLS=true` via
     * [compactToolsMode]) drops the individual *_help schemas - their content stays
     * reachable through the chromia_help gateway - so agents spend ~30 fewer schemas
     * of context. Default (full) keeps every tool for backward compatibility.
     */
    fun compactToolsMode(env: Map<String, String> = System.getenv()): Boolean =
        env["CHROMIA_MCP_COMPACT_TOOLS"]?.equals("true", ignoreCase = true) == true

    /**
     * Comma-separated tool names to drop from advertisement, e.g. for memory-constrained
     * hosted deployments where the in-process Rell compiler tools do not fit:
     * CHROMIA_MCP_DISABLE_TOOLS=rell_check,rell_security_check,run_rell_tests
     */
    fun disabledTools(env: Map<String, String> = System.getenv()): Set<String> =
        env["CHROMIA_MCP_DISABLE_TOOLS"]?.split(',')
            ?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()

    /** Every tool name this server implements, whether or not it is advertised. */
    val ALL_TOOL_NAMES: Set<String> by lazy { fullToolList().map { it.name }.toSet() }

    /**
     * Tool names actually callable on THIS deployment: everything implemented
     * minus [disabledTools]. This is the set advice tools (onboarding_next_step)
     * must consult - recommending a compiled-in-but-disabled tool sends agents
     * into the disabled-tool refusal. Compact mode is irrelevant here: it hides
     * only *_help schemas from advertisement, and those tools stay callable.
     */
    fun enabledToolNames(disabled: Set<String> = disabledTools()): Set<String> =
        ALL_TOOL_NAMES - disabled

    // Names the argument shape too: an agent porting a rell_check call sent
    // {files:{...}} and hit "Missing required parameter: rell" (live probe
    // 2026-09-02) - the refusal must hand over a working call, not just a name.
    private const val CHECK_DAPP_PROJECT_ALTERNATIVE =
        "use check_dapp_project on this server instead - it performs compilation and security scanning " +
            "(pass your sources as `files`, a map of path -> source or a single source string; " +
            "`rell` and `source` are accepted as aliases)"

    /** Working alternatives on the same deployment for commonly disabled tools. */
    private val DISABLED_TOOL_ALTERNATIVES: Map<String, String> = mapOf(
        "rell_check" to CHECK_DAPP_PROJECT_ALTERNATIVE,
        "rell_security_check" to CHECK_DAPP_PROJECT_ALTERNATIVE,
        "run_rell_tests" to CHECK_DAPP_PROJECT_ALTERNATIVE,
        "verify_guards" to "no equivalent tool: run_rell_tests can run the exploit case but cannot prove the guard is " +
            "load-bearing. Do it by hand - remove the guard, rerun ONLY that test, and require it to fail because " +
            "the attack landed (run_must_fail reports 'did not fail'), not for any other reason",
        "local_chain_up" to "verify behavior with run_rell_tests if available, or compile-check with check_dapp_project",
        "chromia_dapp_query" to "use the explorer analytics tools on this server " +
            "(filter_blockchains, get_blockchain_details, get_all_transactions, ...) for on-chain data"
    )

    /**
     * Actionable refusal for a call to a real-but-disabled tool, or null when the
     * name is not a disabled tool of this server. A disabled tool used to answer
     * with the SDK's bare "Tool X not found" - indistinguishable from a tool that
     * never existed, with no alternative offered (hosted probe 2026-09-01). The
     * refusal names the deployment gate, a working alternative on the same server
     * (never one that is itself disabled), and the local-run escape hatch.
     */
    fun disabledToolRefusal(name: String, disabled: Set<String>): String? {
        if (name !in disabled || name !in ALL_TOOL_NAMES) return null
        val alternative = DISABLED_TOOL_ALTERNATIVES[name]?.takeIf {
            !(it.contains("check_dapp_project") && "check_dapp_project" in disabled)
        }
        return buildString {
            append("Tool '$name' is disabled on this deployment (CHROMIA_MCP_DISABLE_TOOLS).")
            if (alternative != null) append(" As an alternative, $alternative.")
            append(" Run chromia-mcp locally for the full toolset.")
        }
    }

    /**
     * The RAG-backed docs tools - the only tools that touch the embeddings
     * index. Compact mode ([compactToolsMode]) never hides these (it drops
     * only [HELP_TOOL_NAMES]), so the index is unreachable exactly when ALL
     * of them are in the [disabledTools] set.
     */
    val DOCS_TOOL_NAMES: Set<String> = setOf("search", "fetch_docs", "fetch")

    /**
     * True when every RAG-backed docs tool is disabled, so the startup index
     * warmup must be skipped: a lite hosted config (e.g. a 512MB instance
     * with docs tools off) must never pay the embeddings download/parse
     * memory spike. RagStore lazy-init is untouched - if a client still
     * calls an unadvertised docs tool by name, the index loads on demand.
     */
    fun docsToolsDisabled(disabled: Set<String> = disabledTools()): Boolean =
        disabled.containsAll(DOCS_TOOL_NAMES)

    /**
     * Byte bound on an advertised `tools/list` description (audit F9). The long
     * form of anything that used to exceed it lives in [ToolDocs] and comes back
     * verbatim from describe_tool / chromia_help.
     */
    const val MAX_DESCRIPTION_BYTES = 1200

    /** Byte bound on a compact-mode headline. */
    const val COMPACT_HEADLINE_BYTES = 280

    /**
     * Byte bound on the whole compact `tools/list` payload. Compact mode used to
     * drop only the 31 *_help schemas - 22% - while the 15 largest schemas, which
     * are most of the cost, were identical in both modes (audit section 1).
     */
    const val COMPACT_TOOLS_BYTES = 26_000

    /** Byte bound on the whole full-mode `tools/list` payload. */
    const val FULL_TOOLS_BYTES = 115_000

    /**
     * First whole lines of [description] that fit in [COMPACT_HEADLINE_BYTES],
     * never cutting a line in half; a first line that is itself too long is cut
     * at the last sentence or word boundary that fits.
     */
    internal fun headline(description: String?): String {
        val text = description?.trim().orEmpty()
        if (text.toByteArray().size <= COMPACT_HEADLINE_BYTES) return text
        val kept = StringBuilder()
        for (line in text.lines()) {
            val candidate = if (kept.isEmpty()) line else kept.toString() + "\n" + line
            if (candidate.toByteArray().size > COMPACT_HEADLINE_BYTES) break
            kept.setLength(0)
            kept.append(candidate)
        }
        if (kept.isNotEmpty()) return kept.toString()
        // A single over-long first line: cut it at a boundary that fits.
        var cut = text
        while (cut.toByteArray().size > COMPACT_HEADLINE_BYTES) cut = cut.dropLast(1)
        val boundary = maxOf(cut.lastIndexOf(". "), cut.lastIndexOf(' '))
        return (if (boundary > 40) cut.substring(0, boundary) else cut).trimEnd() + " ..."
    }

    /** [schema] with every `description` stripped, at any depth. Types, enums and required stay. */
    internal fun withoutDescriptions(schema: ToolSchema): ToolSchema =
        schema.properties?.let { schema.copy(properties = stripDescriptions(it) as JsonObject) } ?: schema

    private fun stripDescriptions(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(
            element.filterKeys { it != "description" }.mapValues { stripDescriptions(it.value) }
        )
        is kotlinx.serialization.json.JsonArray -> kotlinx.serialization.json.JsonArray(element.map { stripDescriptions(it) })
        else -> element
    }

    /**
     * The compact-mode form of [tool]: a headline instead of the description, no
     * schema prose, and no outputSchema. Everything removed here is one
     * describe_tool call away, and ToolDescriptionBudgetTest pins that.
     */
    internal fun compactTool(tool: Tool): Tool = tool.copy(
        description = headline(tool.description),
        inputSchema = withoutDescriptions(tool.inputSchema),
        outputSchema = null
    )

    fun allTools(compact: Boolean = false, disabled: Set<String> = emptySet()): List<Tool> {
        val all = fullToolList()
        val afterCompact =
            if (compact) all.filter { it.name !in HELP_TOOL_NAMES }.map(::compactTool) else all
        return if (disabled.isEmpty()) afterCompact else afterCompact.filter { it.name !in disabled }
    }

    /** The advertised (full-mode) description of [name], or null for an unknown tool. */
    fun advertisedDescription(name: String): String? =
        fullToolList().firstOrNull { it.name == name }?.description

    /**
     * The long-form guidance for [name]: what [ToolDocs] holds when F9 moved it
     * out of the schema, else the advertised description (already the full text).
     */
    fun fullDescription(name: String): String? = ToolDocs.full(name, advertisedDescription(name))

    /** `describe_tool` payload: the full prose plus, by default, the untrimmed schemas. */
    fun describeToolJson(
        name: String?,
        includeSchema: Boolean = true,
        disabled: Set<String> = emptySet()
    ): JsonObject {
        val tool = name?.trim()?.let { requested -> fullToolList().firstOrNull { it.name == requested } }
        if (tool == null) {
            return buildJsonObject {
                put("tools", kotlinx.serialization.json.JsonArray(
                    fullToolList().map { JsonPrimitive(it.name) }
                ))
                put(
                    "notes",
                    (if (name.isNullOrBlank()) "" else "Unknown tool '${name.trim()}'. ") +
                        "Pass one of these names as `tool` for its full description and input schema."
                )
            }
        }
        return buildJsonObject {
            put("tool", JsonPrimitive(tool.name))
            tool.title?.let { put("title", JsonPrimitive(it)) }
            put("description", JsonPrimitive(fullDescription(tool.name).orEmpty()))
            put("movedFromToolsList", JsonPrimitive(tool.name in ToolDocs.LONG))
            put("enabled", JsonPrimitive(tool.name !in disabled))
            if (includeSchema) {
                put("inputSchema", schemaJson(tool.inputSchema))
                tool.outputSchema?.let { put("outputSchema", schemaJson(it)) }
            }
        }
    }

    internal fun schemaJson(schema: ToolSchema): JsonObject = buildJsonObject {
        put("type", JsonPrimitive("object"))
        schema.properties?.let { put("properties", it) }
        schema.required?.let { required ->
            put("required", kotlinx.serialization.json.JsonArray(required.map { JsonPrimitive(it) }))
        }
    }

    private fun fullToolList() = listOf(
        chromiaHelpTool(),
        describeToolTool(),
        getPromptsTool(),
        scaffoldDappTool(),
        validateChromiaYmlTool(),
        checkDappProjectTool(),
        checkFt4ImportsTool(),
        rellCheckTool(),
        rellSecurityCheckTool(),
        runRellTestsTool(),
        verifyGuardsTool(),
        localChainUpTool(),
        translateErrorTool(),
        onboardingNextStepTool(),
        verifyDeploymentTool(),
        deploymentPreflightTool(),
        provisionTestnetContainerTool(),
        claimTestnetTchrTool(),
        deployTestnetChainTool(),
        ft4ModuleArgsTool(),
        chrBuildHelpTool(),
        chrReplHelpTool(),
        chrToolsHelpTool(),
        chrSeederHelpTool(),
        blockchainPropertiesHelpTool(),
        chrEifHelpTool(),
        chromiaYmlDefinitionsHelpTool(),
        chrCompletionHelpTool(),
        chromiaProjectStructureHelpTool(),
        chrMultiSignatureHelpTool(),
        writeDeploymentConfigTool(),
        chrDeployHelpTool(),
        chrNodeHelpTool(),
        chrQueryHelpTool(),
        vaultLeaseHelpTool(),
        chrGenerateClientHelpTool(),
        chromiaDocsYmlHelpTool(),
        chromiaCookbookHelpTool(),
        chrKeyIdHelpTool(),
        chromiaLanguageClientsHelpTool(),
        chromiaRellLanguageHelpTool(),
        chromiaRellTypesHelpTool(),
        chromiaRellExpressionsHelpTool(),
        chromiaRellStatementsHelpTool(),
        chromiaRellDatabaseHelpTool(),
        chromiaRellSystemlibHelpTool(),
        chromiaRellPracticesHelpTool(),
        chromiaFt4QueriesHelpTool(),
        chromiaIntegrationsHelpTool(),
        chromiaVectorSearchHelpTool(),
        chrLibraryHelpTool(),
        chrCreateRellDappHelpTool(),
        getAllAssetsTool(),
        getTotalRewardsPaidTool(),
        getAssetTopHoldersTool(),
        getAssetDistributionTool(),
        getBlockchainAnalyticsTool(),
        filterBlockchains(),
        getBlockchainDetailsTool(),
        getMonthlyActiveAccountsPerChainTool(),
        getAllTransactionsTool(),
        getAllOperationsTool(),
        getFilterAssetsTool(),
        getChrAggregatesTool(),
        getAssetBlockchainsTool(),
        getSignerBlockchainsTool(),
        getAccountBlockchainsTool(),
        fetchDocsTool(),
        fetchTool(),
        searchTool(),
        runDappQueriesTool()
    )
}
