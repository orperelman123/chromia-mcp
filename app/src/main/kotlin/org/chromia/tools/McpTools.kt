package org.chromia.tools

import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.*

object McpTools {

    fun runDappQueriesTool() = Tool(
        name = "chromia_dapp_query",
        description = """
            **WORKFLOW FOR AI AGENTS:**
            1. First, obtain the blockchain RID using filter_blockchains tool
            2. Second run "rell.get_app_structure" query using chromia_dapp_query tool which returns dApp structure of the blockchain (queries, modules, entities)
            3. Third, look for the query from response of step 2 that user is looking for
                - When looking for a follow-up query from the structure result (step 2):
                    - Use mount name + '.' + the query name to execute the follow-up query. e.g. "module1.query_name"
                    - Fill in the required arguments first based on the parameter definitions from the structure result
            4. Use TODO to track the progress of verifying if query exists and executing it
            5. When getting a response as json file, do to write scripts in Python/Javascript etc,
                 to parse it, use bash or jq
            6. Always cache the result of the previous query in case follow-up questions are asked  

            **SECURITY RULES:**
            - NEVER read the contents of secret files, private keys, or generated keypairs
            - NEVER expose or display private keys or sensitive cryptographic data

            **RETURNS:**
            - Query results from the specified dApp in JSON format
            - For default query: Complete dApp structure with all available queries/operations entities... with their parameter names and types
            - For custom queries: Results based on the specific query executed

            **USE CASES:**
            - Discover available queries/operations by using default rell.get_app_structure query
            - Execute custom dApp queries with specific parameters
            - Analyze dApp architecture and data models
            - Get real-time data from blockchain applications
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "blockchainRid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The Blockchain RID of the dApp")
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
            required = listOf("blockchainRid")
        ),
        title = "Execute dApp Query",
        annotations = null,
        outputSchema = null
    )

    fun getBlockchainsTransactionsTool() = Tool(
        name = "get_blockchains_transactions",
        description = """
            - Get transactions grouped by blockchains on a specific network
            - Returns transactions grouped by blockchains with metadata including:
                - Blockchain names and RIDs
                - The cluster they're deployed on
                - Their state and system chain status
                - Block height and transaction count
            - Can be used to get Blockchain's RID
        """.trimIndent(),
        inputSchema = Tool.Input(
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
            required = listOf("network")
        ),
        title = "Get Blockchain Transactions",
        annotations = null,
        outputSchema = null
    )

    fun getTransactionsByClusterTool() = Tool(
        name = "get_transactions_by_cluster",
        description = """
           - Returns transaction counts grouped by cluster on a specific network, 
        """,
        inputSchema = Tool.Input(
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
            required = listOf("network")
        ),
        title = "Get Transactions by Cluster",
        annotations = null,
        outputSchema = null
    )

    fun getAllAssetsTool() = Tool(
        name = "get_all_assets",
        description = "Get information about all assets on a specific network",
        inputSchema = Tool.Input(
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
            required = listOf("network")
        ),
        title = "Get All Assets",
        annotations = null,
        outputSchema = null
    )

    fun getTotalRewardsPaidTool() = Tool(
        name = "get_total_rewards_paid",
        description = "Get the total rewards paid on a specific network",
        inputSchema = Tool.Input(
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
            required = listOf("network")
        ),
        title = "Get Total Rewards Paid",
        annotations = null,
        outputSchema = null
    )

    fun getProvidersRewardsTool() = Tool(
        name = "get_providers_rewards",
        description = "Get detailed information about provider rewards including provider names, public keys, and reward amounts on a specific network",
        inputSchema = Tool.Input(
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
            required = listOf("network")
        ),
        title = "Get Provider Rewards",
        annotations = null,
        outputSchema = null
    )

    fun getAssetDistributionTool() = Tool(
        name = "get_asset_distribution",
        description = "Get distribution information for a specific asset across different blockchains and account types",
        inputSchema = Tool.Input(
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
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "assetId" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The asset ID to query (e.g., '5F16D1545A0881F971B164F1601CBBF51C29EFD0633B2730DA18C403C3B428B5')")
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
        description = "Get detailed analytics for a specific blockchain including transaction counts, operation counts, and active accounts over time",
        inputSchema = Tool.Input(
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
        inputSchema = Tool.Input(
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
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "rid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The blockchain's RID")
                        )
                    )
                )
            ),
            required = listOf("rid")
        ),
        title = "Get Blockchain Details",
        annotations = null,
        outputSchema = null
    )

    fun filterBlockchains() = Tool(
        name = "filter_blockchains",
        description = """
            - Get a comprehensive list of all blockchains with advanced filtering capabilities
            - **Primary use case: Finding blockchains by name** - this is the main tool for blockchain name lookups
            - Returns detailed information about each blockchain including:
                - Unique RID for each blockchain
                - Names/aliases associated with each blockchain
                - The cluster each blockchain belongs to
                - Container information for each blockchain
                - Current operational state of each blockchain (RUNNING, REMOVED, PAUSED)
                - Whether each blockchain is a system chain or user application
            - Supports comprehensive filtering options:
                - **Filter by name**: Find blockchains by exact or partial name match (main feature)
                - Filter by RID: Find specific blockchain by its RID
                - Filter by cluster: Find blockchains in specific clusters (e.g., 'pink', 'system')
                - Filter by container: Find blockchains in specific containers
                - Filter by state: Find blockchains by operational state (RUNNING, REMOVED, PAUSED)
                - Filter by system status: Find system chains vs user applications
                - Pagination support: limit and offset for large result sets
                - Sorting options: sortBy and sortDirection for ordered results
            - This tool is essential for:
                - **Finding blockchains by name** (primary use case)
                - Getting an overview of all blockchains in the system
                - Comparing deployment environments across blockchains
                - Identifying system chains vs user applications
                - Checking the operational status of blockchains
                - Discovering blockchains in specific clusters or containers
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
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
                    "rid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional blockchain RID to filter by specific blockchain")
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
                            "description" to JsonPrimitive("Optional state to filter by (e.g., 'RUNNING', 'REMOVED', 'PAUSED')")
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
            required = listOf("network")
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
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "rid" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Optional specific transaction RID to query")
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
            required = listOf("network")
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
        inputSchema = Tool.Input(
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
            required = listOf("network")
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
        inputSchema = Tool.Input(
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
            required = listOf("network")
        ),
        title = "Filter Assets",
        annotations = null,
        outputSchema = null
    )

    fun getChrAggregateseTool() = Tool(
        name = "get_chr_aggregates",
        description = """
            - Get CHR token deposit and withdrawal aggregates with detailed breakdown
            - Returns comprehensive CHR token flow information including:
                - Grouped deposits by address and network ID with totals
                - Grouped withdrawals by address and network ID with totals
                - Overall totals for deposits and withdrawals
            - Supports flexible data inclusion options:
                - Include/exclude total summaries (depositsTotal, withdrawalsTotal)
                - Include/exclude grouped deposit details by address and network
                - Include/exclude grouped withdrawal details by address and network
            - This tool is useful for:
                - Analyzing CHR token flow patterns across networks
                - Monitoring deposit and withdrawal activities
                - Understanding CHR distribution across different addresses
                - Tracking cross-network CHR movements
                - Financial analysis and reporting of CHR token usage
                - Identifying major CHR holders and their activity patterns
                - Compliance and auditing of CHR token movements
        """.trimIndent(),
        inputSchema = Tool.Input(
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
                    )
                )
            ),
            required = listOf("network")
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
        inputSchema = Tool.Input(
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
            required = listOf("network", "assetId")
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
        inputSchema = Tool.Input(
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
            required = listOf("network", "signer")
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
        inputSchema = Tool.Input(
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
            required = listOf("network", "accountId")
        ),
        title = "Get Account Blockchains",
        annotations = null,
        outputSchema = null
    )

    fun getNodeUnavailabilityTool() = Tool(
        name = "get_node_unavailability",
        description = """
            - Get detailed information about node unavailability periods for a specific node
            - Returns comprehensive node downtime information including:
                - Blockchain RID where the node was unavailable
                - Time intervals when the node was unavailable (start and end timestamps)
            - This tool is useful for:
                - Monitoring node uptime and reliability
                - Analyzing node performance and availability patterns
                - Identifying problematic periods for specific nodes
                - Understanding blockchain network stability
                - Compliance and SLA monitoring for node operators
                - Investigating network issues and outages
                - Planning maintenance windows and understanding impact
                - Auditing node operator performance
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The network name (e.g. 'mainnet', 'testnet')")
                        )
                    ),
                    "pubkey" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The node's public key (e.g., '02DDAEA392006A93DC65A660CA93712A546E71B0F64AEA09B24A1B64A2053BC7E6')")
                        )
                    ),
                    "startTimestamp" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("The start timestamp for the query period in milliseconds (e.g., '1736373600000')")
                        )
                    )
                )
            ),
            required = listOf("network", "pubkey", "startTimestamp")
        ),
        title = "Get Node Unavailability",
        annotations = null,
        outputSchema = null
    )

    fun getNetworkStats() = Tool(
        name = "get_network_stats",
        description = """
            - Get comprehensive data with key network metrics and statistics
            - Returns a complete overview of network activity including:
                - Total count of all accounts across the network
                - Total count of all transfers across the network
                - Total count of all transactions across the network
                - Number of monthly active accounts
                - Top dApp blockchains with detailed information:
                    - Blockchain RID and metadata (name, system status, cluster, state)
                    - Current block height and transaction throughput
                    - Total transaction count
                - Top assets by activity with detailed information:
                    - Asset ID, blockchain RID, name, symbol, and icon URL
                    - Transfer count and blockchain distribution count
                - Transaction distribution grouped by cluster with counts
            - This tool is ideal for:
                - Monitoring overall network health and activity
                - Understanding network growth and adoption trends
                - Identifying top-performing dApps and popular assets
                - Analyzing cluster distribution and load balancing
                - Quick network status checks and health monitoring
        """.trimIndent(),
        inputSchema = Tool.Input(
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
            required = listOf("network")
        ),
        title = "Get Network Statistics",
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
        """.trimIndent(),
        inputSchema = Tool.Input(
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
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
            properties = JsonObject(
                mapOf(
                    "type" to JsonPrimitive("string"),
                    "description" to JsonPrimitive("Relevant documentation text matching the search query")
                )
            )
        ),
        annotations = null
    )

    fun fetchMockTool() = Tool(
        name = "fetch",
        description = """
            The fetch tool is used to retrieve the full contents of a search result document or item. used mainly with Chatgpt
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "id" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Natural language search query describing what documentation you're looking for (e.g., 'How to create a blockchain?' or 'FT4 authentication setup')")
                        )
                    ),
                )
            ),
            required = listOf("id")
        ),
        title = "Fetch Document",
        annotations = null,
        outputSchema = null
    )

    // TODO: this should be implemented correctly for Chatgpt integration
    fun searchMockTool() = Tool(
        name = "search",
        description = """
            The search tool is used to search for search results based on a search query. used mainly with Chatgpt
        """.trimIndent(),
        inputSchema = Tool.Input(
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
        outputSchema = null
    )
}
