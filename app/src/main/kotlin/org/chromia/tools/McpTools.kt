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

            The whole query is bounded by an overall deadline (default 20s, env
            CHROMIA_MCP_QUERY_DEADLINE_MS, capped at 45s) so it can never hang: a chain the queried
            nodes do not serve returns an actionable error (usually: pass the dapp's own node URL
            as network) instead of crawling every endpoint for minutes.
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
            required = listOf()
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
            required = listOf()
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
            required = listOf()
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
            required = listOf()
        ),
        title = "Get Total Rewards Paid",
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
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
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
            required = listOf()
        ),
        title = "Filter Assets",
        annotations = null,
        outputSchema = null
    )

    fun getChrAggregatesTool() = Tool(
        name = "get_chr_aggregates",
        description = """
            - Get CHR token deposit and withdrawal aggregates with detailed breakdown
            - Returns comprehensive CHR token flow information including:
                - Grouped deposits by address and network ID with totals
                - Grouped withdrawals by address and network ID with totals
                - Overall totals for deposits and withdrawals
            - RESPONSE SIZE: by default the response is summarized - the grouped
              deposit/withdrawal arrays are capped at the first 50 entries each, with a
              `note` field saying how many entries were omitted. Pass full:true for the
              complete, uncapped response (can be hundreds of KB).
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
            required = listOf("accountId")
        ),
        title = "Get Account Blockchains",
        annotations = null,
        outputSchema = null
    )

    fun getNodeUnavailabilityTool() = Tool(
        name = "get_node_unavailability",
        description = """
            - KNOWN LIMITATION (2026-08-31): the explorer currently requires a reCAPTCHA token
              for this query, so programmatic calls fail with "reCAPTCHA verification failed" -
              an upstream policy, not a server bug. Kept for when the requirement is lifted.
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
            required = listOf("pubkey", "startTimestamp")
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
            required = listOf()
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
                    )
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
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
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
                    )
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
        outputSchema = Tool.Output(
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
                    )
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
            Templates: 'hello' (default, query-only quickstart) or 'ft4' - the golden FT4 template:
            accounts + auth handlers, operations showing the full authenticate -> authorize ->
            validate -> check-invariants pattern (including an explicit ownership check and a
            Transfer-flag-scoped value move), plus RUNNABLE invariant tests (conservation,
            no-negative-balance, non-owner-must-fail) that execute via run_rell_tests - copy
            them for the app's own invariants. Also module_args, libs block, and a TypeScript
            client example. FT4 imports compile after `chr install`.
            Building a DAO / treasury: use 'governance' - quorum, a fixed voting window,
            stake-weighted votes and execute-once are structural, and its shipped tests replay
            the single-account treasury drain and require it to fail. Building an exchange,
            vault or anything priced by an oracle: use 'vault' - every credit is a reserve
            debit in the same operation, price posts are bounded and rate-limited, a stale
            price halts trading, and its tests replay the 100 -> 200,000,000 mint and require
            it to fail (its oracle key is a module arg: see the notes). Building staking, yield,
            rewards or farming emissions - a share of a REWARD POOL that many stakers split: use
            'staking' - rewards come only from a sponsor-funded pool, the clock releases at most
            what the pool holds, every credit is a pool debit in the same operation, unstaking has
            a cooldown, and its tests replay the round-4 stake-times-elapsed mint from an empty
            pool and require it to fail.
            Building an NFT marketplace, a listing board or anything with a buy button and creator
            royalties: use 'marketplace' - a buy names the EXACT price it agreed to and the listing
            row is immutable (so the round-5 max_price sandwich cannot be written), offers escrow the
            bidder's points and settle atomically, and the royalty's off-market bypass is DOCUMENTED
            in the template header with a shipped test asserting it still works.
            Building a lending pool, a credit line or a money market - anything where depositors hold
            a SHARE of a pool whose value moves: use 'lending' - it stores NO cash-denominated debt
            anywhere (positions and the pool carry scaled_debt in index units, the cash figures exist
            only inside a pool_state, pool_now() is the only function that makes one, and every
            pricing helper takes one), so the round-6 just-in-time interest capture - deposit at a
            share price stale between a borrower's touches, exit one block later, 10000 in and 11500
            out with nothing minted - cannot be written. It keeps the vault's bounded oracle (its
            key is a module arg: see the notes), over-collateralisation, a liquidation threshold with
            a close factor and bonus, and the minimum-first-deposit guard that kills ERC-4626 share
            inflation, and its tests replay the round-6 drain and require it to be refused.
            Building a payment stream, payroll, a subscription, a vesting grant or a drip - any
            payout METERED BY THE CLOCK to one named beneficiary: use 'streaming' - NO OPERATION IN
            IT WRITES A TIMESTAMP, so the round-7 grief cannot be written: started_at is written
            once by the create and is not mutable, the entitlement is a pure function of that
            immutable start and an immutable rate less a MONOTONE released total, and every other
            term is immutable too, so a stranger settling faster than one whole unit of entitlement
            (which released ZERO and still advanced the anchor in round 7, grinding the payee's
            income to nothing while the payer kept 100% of the escrow) is now a no-op. The stream
            is PREPAID, cancellation pays the payee everything accrued BEFORE refunding the payer
            the unearned remainder, and `cancellable` is fixed at creation so a vesting grant
            cannot be clawed back. Its tests replay the round-7 grind and require the payee to be
            paid what the clock says anyway.
            NEVER includes lib.ft4.admin, admin.crosschain, ras_open, or ras_transfer_open.
            Does not send signed transactions and does not run chr. Confirm APIs with fetch_docs.
        """.trimIndent(),
        inputSchema = Tool.Input(
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
                    "template" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "enum" to kotlinx.serialization.json.JsonArray(DappScaffold.templates.map { JsonPrimitive(it) }),
                            "description" to JsonPrimitive("Skeleton flavor: 'hello' (query-only quickstart, default), 'ft4' (accounts, authenticated operation, TS client), 'governance' (DAO treasury: quorum, fixed voting window, stake-weighted votes, execute-once - structural, with the drain replayed as a must-fail test), 'vault' (oracle-priced reserve, NOT an exchange or a curve - a swap pool is 'amm': reserve-backed credits, bounded and rate-limited price, staleness halt - with the unbacked mint replayed as a must-fail test), or 'staking' (staking / yield / rewards / emissions - NOT vesting, which is 'streaming': sponsor-funded pool as the only reward source, pool-capped release, per-share accumulator, cooldown unstake - with the round-4 empty-pool mint replayed as a must-fail test), or 'marketplace' (NFT marketplace / listings / auctions with creator royalties: exact-price buys on an immutable listing so a seller cannot sandwich a pending buy, escrowed offers with expiry settled atomically, royalty fixed at mint - with the round-5 price sandwich replayed as a must-fail test and the off-market royalty bypass documented, not faked; it ALSO ships the timed ascending auction - no mutable bid field, the standing bid is its own immutable escrow row, settlement permissionless after the deadline - and one encumbrance helper every token-moving path consults, so do not write an auction freehand), or 'lending' (lending pool / credit line / money market - anything where depositors hold a SHARE of a pool whose value moves: NO cash-denominated debt is stored anywhere, so the round-6 just-in-time interest capture is unwritable rather than merely guarded - positions and the pool carry scaled_debt in index units, the cash figures exist only inside a pool_state, pool_now() is the only function that makes one and every pricing helper takes one - plus the vault's bounded oracle, over-collateralisation, a liquidation threshold with close factor and bonus, and the minimum-first-deposit guard against ERC-4626 share inflation, with the round-6 drain replayed as a must-fail test; its oracle key is a module arg, see the notes), or 'streaming' (payment stream / payroll / subscription / vesting grant / drip - a clock-metered payout to ONE named beneficiary: no operation writes a timestamp an entitlement is measured from, every term is immutable, the stream is prepaid and cancellation pays before it refunds, and pause/resume is shipped with both transition guards - with the round-7 anchor grief and both round-8 pause drains replayed as must-fail tests), or 'amm' (constant-product swap pool / DEX pair / automated market maker: a swap NAMES THE EXACT RESERVES it was quoted at and there is no tolerance field at all, so it pays the quoted number or reverts - stronger than a min_out floor, not a weakening of one - and liquidity is an IMMUTABLE POSITION ROW WITH A TERM, so the just-in-time deposit-before-a-swap-withdraw-after cannot be written; both round-8 drains ship as must-fail tests and the residuals the guards do NOT close ship as a test too), or 'stablecoin' (a coin minted against LOCKED COLLATERAL - CDP, synthetic, pegged asset - NOT 'vault', which is where round 9's drain was sent: there is NO operation that redeems the coin for collateral at par out of somebody else's position; the peg is the debtor's burn-at-par against their OWN debt, under-water positions close by PRO-RATA liquidation that pays every liquidator the same rate in any order, and a system worth less than its coin is SETTLED so every coin redeems the same share of one pool; mint and withdraw are ratio-checked against the whole debt at a fresh bounded price - with round 9 replayed in both orders as must-fail tests). An unknown template name is answered with the closest shipped template and what it does NOT cover.")
                        )
                    )
                )
            ),
            required = listOf()
        ),
        title = "Scaffold Chromia dapp",
        annotations = null,
        outputSchema = Tool.Output(
            properties = JsonObject(
                mapOf(
                    "name" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "pins" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "forbidden" to JsonObject(mapOf("type" to JsonPrimitive("array"))),
                    "files" to JsonObject(mapOf("type" to JsonPrimitive("object"))),
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("name", "pins", "forbidden", "files", "notes")
        )
    )


    fun validateChromiaYmlTool() = Tool(
        name = "validate_chromia_yml",
        description = """
            Validate a chromia.yml string against production pins.
            Checks compile.rellVersion (semver N.N.N), blockchains.*.module (module name, not a file path),
            merkle_hash_version == 2, blockchain key webStatic is accepted, and forbids FT4 admin / ras_open modules
            in libs and code; moduleArgs KEYS naming admin modules (e.g. lib.ft4.core.admin for admin_pubkey) are
            legitimate configuration and are NOT flagged.
            MISSING production pins (compile.rellVersion, merkle_hash_version) are warnings by default - chr builds
            official configs that omit them; pass strict:true to make missing pins errors. A rellVersion newer than
            the CLI-bundled compiler or a present-but-wrong merkle value is always an error.
            Deployments: reserved names mainnet / testnet auto-fill Directory brid + url; custom names require both;
            a Directory Chain BRID that is not 64 hex is an error; official reserved BRIDs must match.
            require_mandatory_flags as a YAML / moduleArgs key is an error (main auth descriptor only).
            Warns if a chain config lacks merkle_hash_version while others set it, deployments.*.container is missing,
            or libs.*.insecure is true (skips RID check; not for production).
            Returns structured {ok, errors[], warnings[]}. Does not run chr or send signed transactions.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
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
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
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

    fun chromiaHelpTool() = Tool(
        name = "chromia_help",
        description = """
            One gateway to the full static Chromia help catalog: CLI commands (build, deploy, node,
            query, repl, keys, library, completion, seeder, eif, multisig, client generation,
            create-rell-dapp), chromia.yml definitions and docs config, project structure, vault
            leases, the Rell language (types, expressions, statements, database ops, system library,
            best practices), FT4 queries, integrations, vector search, and the cookbook.
            Call with no arguments to list all topics; call with a topic to get that payload.
            The same content as the individual *_help tools, through one schema.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "topic" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Help topic, e.g. 'chr_deploy_help' (or 'chr_deploy'). Aliases: 'security', 'best_practices' and 'best-practices' map to chromia_rell_practices_help. Omit to get the full topic list."),
                            "enum" to kotlinx.serialization.json.JsonArray(HELP_TOOL_NAMES.sorted().map { JsonPrimitive(it) })
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
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "source" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Rell source for a single-file check; compiled as main.rell. Ignored when `files` is given.")
                        )
                    ),
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Map of relative .rell file paths to file contents for multi-file projects, e.g. {\"main.rell\": \"module; ...\"}. Paths are relative to the Rell source root - drop the project's src/ prefix (a leading ./ or src/ is normalized away automatically).")
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
        outputSchema = Tool.Output(
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
            Execute Rell tests in-process with the embedded Rell test runner (same engine the
            Chromia CLI wraps) and return per-case pass/fail results - no chr installation needed.
            This completes the agent verification loop:
            1. rell_check - the code compiles
            2. rell_security_check - the code is secure
            3. run_rell_tests - the code behaves correctly
            Pass `files` including at least one file starting with `@test module;` whose test
            functions are named test_*. Tests that touch entities/database need PostgreSQL via the
            CHROMIA_TEST_DATABASE_URL env var on the server; pure-logic tests run without it.
            Happy-path tests are not enough: for any dapp that holds value, also ship INVARIANT
            tests - conservation (a transfer never changes the total), no-negative-balance
            (overdraft must abort), and authorization (a NON-owner's attempt must fail via
            rell.test.tx()...run_must_fail("message")). scaffold_dapp template=ft4 ships runnable
            examples of all three to copy; template=governance and template=vault ship the
            adversary's DAO drain and oracle mint as must-fail tests - copy those for your own
            exploit-must-fail cases.
            Nothing is deployed; sources run in a temp directory and are deleted afterwards.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Map of relative .rell paths to contents: app modules plus at least one @test module, e.g. {\"main.rell\": \"module; ...\", \"main_test.rell\": \"@test module; import main; function test_x() { ... }\"}. Paths are relative to the Rell source root - drop the project's src/ prefix (a leading ./ or src/ is normalized away automatically).")
                        )
                    ),
                    "moduleArgs" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "description" to JsonPrimitive("Optional module_args by module name, mirroring chromia.yml (its moduleArgs AND test.moduleArgs blocks), e.g. {\"lib.ft4.core.accounts\": {\"auth_flags\": {\"mandatory\": [\"A\",\"T\"]}}}. Required to exercise real FT4 operations in tests: use ft4_module_args for production-correct values, and when using lib.ft4.test.core helpers (register_alice etc.) ALSO pass the test-only admin keys - lib.ft4.core.admin {admin_pubkey} and lib.ft4.test.core.auth {admin_priv_key} (FT4's published test keys; scaffold_dapp template=ft4 writes a working set into chromia.yml test.moduleArgs). Without them every tx fails with 'Unable to create GTX module'. Pass it as ONE object that merges blockchains.<name>.moduleArgs with test.moduleArgs, keyed by module name; byte_array / pubkey values may be the yml's x\"02C4...\" literal, 0x02c4..., or bare hex - all three decode to bytes.")
                        )
                    )
                )
            ),
            required = listOf("files")
        ),
        title = "Run Rell tests",
        annotations = null,
        outputSchema = Tool.Output(
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
                    "notes" to JsonObject(mapOf("type" to JsonPrimitive("string")))
                )
            ),
            required = listOf("ok", "total", "passed", "failed", "cases", "notes")
        )
    )

    fun localChainUpTool() = Tool(
        name = "local_chain_up",
        description = """
            Stand up a REAL local Chromia chain from Rell sources - in-process, zero keys, zero
            funds, zero human steps. Compiles the sources into a blockchain configuration and runs
            it on the embedded Postchain engine (the same engine `chr node start` wraps) against
            the server's PostgreSQL, then serves a subset of the Postchain REST API on 127.0.0.1:
            GET /brid/iid_0, GET+POST /query/{brid}, POST /query_gtv/{brid}, POST /tx/{brid}, and
            GET /tx/{brid}/{txRid}/status - block and confirmation-proof endpoints are NOT served.
            This is the last step of the agent loop: rell_check (compiles) -> rell_security_check
            (secure) -> run_rell_tests (tests pass) -> local_chain_up (runs against a live chain).
            Returns the BRID and apiUrl; then query with
            POST {apiUrl}/query/{brid} {"type":"<query>", ...args} (start with type=rell.get_app_structure),
            and submit transactions with any postchain client against apiUrl + BRID, signed with the
            public Chromia CLI dev key (privkey 42 repeated 32 times - local only, never a secret).
            actions: "up" (default; requires `files`), "status", "down".
            Bounded by design: one chain at a time, auto-stops after ttlSeconds (default 1800, max
            7200), runs in a dedicated PostgreSQL schema that is wiped on every start. Calling up
            again with identical inputs returns the running chain (TTL refreshed); a change to the
            sources, moduleArgs, or databaseUrl restarts it. Needs PostgreSQL via
            CHROMIA_TEST_DATABASE_URL on the server (or
            `databaseUrl`); @test modules are excluded from the chain like `chr build`.
        """.trimIndent(),
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
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

    fun rellSecurityCheckTool() = Tool(
        name = "rell_security_check",
        description = """
            Static security review of Rell code, run after a successful compile (compiles first via
            the embedded Rell compiler; uncompilable code returns the compile errors instead).
            Checks the production security rules for Chromia dApps:
            - CRITICAL: banned admin modules (lib.ft4.admin, admin.crosschain) and open
              registration/transfer strategies (ras_open, ras_transfer_open)
            - HIGH: operations that create/update/delete state without any auth check
              (ft4 auth.authenticate, op_context.is_signer, signer require)
            - HIGH: authenticated operations that debit/delete rows selected by a caller-supplied
              account parameter never bound to the authenticated identity (confused deputy:
              anyone drains anyone)
            - HIGH: is_signer(<param>) gates where the caller supplies the very key being checked
              and the parameter is used nowhere else (a phantom admin gate)
            - HIGH: update/delete @* {} with an empty where-clause (hits every row)
            - HIGH: hardcoded 64+ char hex literals that look like key material
            - MEDIUM: value-moving operations when every registered auth handler has flags = []
              (FT4 contains_all([]) is always true, so limited session keys can spend)
            - MEDIUM: hardcoded 64-hex constants named like BRIDs/hashes (public identifiers)
            - MEDIUM: operations with parameters but no require(...) input validation
              (validation inside called helper functions counts)
            HIGH findings in test-only code (@test modules, files under test/ or tests/, and
            modules imported only from @test modules) are reported as MEDIUM with a
            "-test-surface" rule suffix. @test modules are fully exempt from the banned-module/
            open-strategy rules (test code legitimately exercises admin modules and registration
            strategies); in non-test code CRITICAL findings never downgrade. Submitted lib/** files
            are library code: vendored-identical lib/ft4 and lib/iccf files are exempt, differing
            ones are scanned and noted, and other lib/* trees are skipped as third-party code.
            allowAdminModules:true (default false) downgrades banned-module findings from
            CRITICAL to MEDIUM - for admin/ops tooling only, never for production dApps.
            Returns line-anchored findings with a concrete fix per finding. ok=true means no
            CRITICAL/HIGH findings. Heuristic static analysis - it does not replace an audit.
            ok=true is NOT economic soundness. Static rules structurally cannot see: missing
            AUTHORIZATION (an authenticated caller touching a row it does not own - key writes
            off the authenticated id, or require(row.owner == account.id)); unbacked minting
            (crediting value no reserve covers); missing quorum/stake/timelock in governance;
            funds with no withdrawal or timeout path; i64 overflow aborting large legitimate
            amounts; whether an outcome meant to be UNPREDICTABLE actually is (only the
            block-clock-as-selector shape is caught - no chain value is secret, so a hash, a
            counter or a seed mixed from on-chain state is still public before the transaction
            is signed, and a clean report is not proof of fair randomness); or TRANSACTION
            ORDERING / MEV (front-running, sandwiching, a price or listing that can change
            under a pending transaction - the order operations land in a block is invisible
            to every static rule). Prove those with invariant tests via run_rell_tests - scaffold_dapp
            template=ft4 ships runnable conservation/overdraft/non-owner-must-fail examples,
            and for a DAO or an oracle-priced vault start from template=governance / template=vault,
            where quorum, voting window, reserve-backing and price bounds are structural.
            Use with rell_check as the loop: compile clean, then security clean, then present.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "source" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Rell source for a single-file review; analyzed as main.rell. Ignored when `files` is given.")
                        )
                    ),
                    "files" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("object"),
                            "additionalProperties" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                            "description" to JsonPrimitive("Map of relative .rell file paths to file contents for multi-file projects.")
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
        outputSchema = Tool.Output(
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
            network must be testnet or mainnet. Does not invent a BRID.
            Since CLI 0.30.0, chr deployment create writes deployments.<net>.chains back into chromia.yml;
            on 0.29.x add chains.<name>: x"<dapp rid>" by hand after the first create.
            Does not send signed transactions and does not run chr.
        """.trimIndent(),
        inputSchema = Tool.Input(
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
                    )
                )
            ),
            required = listOf("network")
        ),
        title = "Write deployment config",
        annotations = null,
        outputSchema = Tool.Output(
            properties = JsonObject(
                mapOf(
                    "network" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "name" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "url" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "brid" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "yaml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
                    "chromia_yml" to JsonObject(mapOf("type" to JsonPrimitive("string"))),
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI build help",
        annotations = null,
        outputSchema = Tool.Output(
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
            Official Chromia CLI 0.33.x chr deployment flag help: create / update / inspect plus
            read-only info / proposal list|info / voterset info|list (no key-pair flags).
            Includes -y, --key-id (reference only; does not generate a key), schema-compare DROP warning,
            and that create writes deployments.<net>.chains back. Optional container: field after a
            Vault/PMC lease — does not invent a lease id or BRID.
            Also returns official chromia.yml database / test section snippets (Java 21+, Postgres 16+). Official BUILD vault-listing read-only find_dapp_details query (skip chr tx writes and sample 64-hex). Official BUILD testnet list-dapp-vault (200) checkmark / setUpMocks.ts / hardcoded vs db names. Official intro/installation/postchain-clients is 404; /build/clients/overview wins. Official BUILD testnet deploy-dapp / getting-started (200): create write-back wins; getting-started mainnet wording on TESTNET page is stale. Official BUILD get-tchr-binance (200) BSC vs Chromia tCHR differences; deploy-dapp explorer verify explorer.chromia.com. Official BUILD connect-client started (mainnet creatClient typo). Official BUILD deploy-frontend-dapp webStatic (200).
            Does not shell out to chr and does not send signed transactions.
            Skips vote/propose/pause/resume/remove and hidden lease-info / remove-container.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI deploy help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI node help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI query help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Vault / PMC lease help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI generate-client help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI library help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI create-rell-dapp help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI repl help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI tools help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI seeder help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "chromia.yml blockchain-properties help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI eif help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "chromia.yml definitions / YAML include help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI completion help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI multi-signature view help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "chromia.yml project-structure / Rell modules help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "chromia.yml docs section help",
        annotations = null,
        outputSchema = Tool.Output(
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
            Official Chromia BUILD cookbook help for building a dapp: queries, client reads, and tests.
            Official pages only, including /rell/tests builders, asserts, and @disabled.
            Official cookbook run-queries is HELP ONLY (skip sample BRID hex).
            Official cookbook run-tests is HELP ONLY (skip sample BRID hex; chr test --sql-log removed).
            Official cookbook create-rell-dapp is HELP ONLY (skip sample BRID hex; --local skipped).
            Official cookbook overview is HELP ONLY (Welcome to the Chromia Cookbook; skip sample BRID hex).
            Official cookbook CLI is HELP ONLY (CLI; skip sample BRID hex; run-operations this signs).
            Official cookbook query-creation is HELP ONLY (Create queries; skip sample BRID hex; get-account-balance EVM key pair).
            Official cookbook get-account-balance is HELP ONLY (How to get account balance; EVM key pair).
            Official cookbook account-creation is HELP ONLY (Account creation; this signs).
            Official cookbook transaction-creation is HELP ONLY (Create & manage transactions; this signs).
            Official cookbook run-operations is HELP ONLY (How to run operations; this signs).
            Skips recipes that sign a live tx, cookbook-only flags, non-schema keys, and printed sample keys.
            Does not run chr, generate a key, or send signed transactions.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia cookbook help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia CLI existing-key reference",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia language clients help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell types help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell language definition help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell security and best-practices help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "FT4 read-only query catalog",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia integrations hub help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Chromia vector-search help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell expressions help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell statements help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell database language help",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(emptyMap()),
            required = listOf()
        ),
        title = "Rell system library help",
        annotations = null,
        outputSchema = Tool.Output(
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
            One-call project gate. Takes a chromia.yml string plus one or more .rell file contents and runs
            the FULL check: validate_chromia_yml + check_ft4_imports + rell_check (real compilation, FT4
            imports included) + rell_security_check when it compiles. Returns combined {ok, errors, warnings, notes};
            ok=true means the project parses, compiles, and has no CRITICAL/HIGH security findings.
            `yaml` is optional: when omitted, a minimal default chromia.yml at the current pins
            (rellVersion ${DappScaffold.RELL_VERSION}) is used and noted in the output.
            allowAdminModules:true (default false) downgrades banned admin-module findings from errors
            to warnings - for admin/ops tooling only, never for production dApps.
            When the yaml declares multiple blockchains whose modules match submitted files, each chain's
            module set is compiled separately (like chr build) so per-chain alternative modules do not
            false-red with mount-name conflicts; notes says so.
            Submitted lib/ft4/ or lib/iccf/ files identical to the vendored sources compile but are exempt
            from the import/security scanners (FT4's own sources legitimately contain e.g. ras_open); a
            file differing from the vendored copy is scanned like app code and noted. Other lib/** files
            (lib/ft3, lib/icmf, ...) are skipped as third-party library code and noted. @test modules are
            exempt from the forbidden-module scan (test code legitimately exercises admin modules).
            Use this as the single pre-deploy gate instead of calling the four tools separately.
            Read-only: does not write files, run chr, generate keys, or send signed transactions.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Full chromia.yml contents as a string. Optional: when omitted, a minimal default " +
                                    "chromia.yml at the current pins (rellVersion ${DappScaffold.RELL_VERSION}) is used and noted."
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
                    "files" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "Alias for `rell` (same shape) - accepted because rell_check and " +
                                    "run_rell_tests name this parameter `files`. When both are present, `rell` wins."
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
            required = listOf("rell")
        ),
        title = "Check dapp project",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
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
            required = listOf("rell")
        ),
        title = "Check FT4 imports",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
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
            Catch every deployment problem BEFORE a human burns a lease step or signs anything.
            Given chromia.yml text and a deployment target name (e.g. "testnet" / "mainnet"), checks:
            (1) the deployments.<target> block - brid present/well-formed, url valid, container a real
            lease id (not a placeholder), chains matching declared blockchains; (2) reachability - a
            read-only height probe of the block's Directory Chain BRID against its own URL(s), with
            classified failure hints, bounded by one overall deadline shared by all probed URLs
            (default 20s, env CHROMIA_MCP_PREFLIGHT_PROBE_DEADLINE_MS, capped at 45s) so an
            unserved chain can never hang the tool; (3) network sanity - a testnet/mainnet target whose brid or url
            points at the OTHER network is a HIGH blocker (wrong-network deploys are unrecoverable);
            (4) the source gate when `rell` is supplied - code must compile, and for MAINNET targets
            CRITICAL/HIGH security findings are blockers (warnings for testnet); (5) production pins
            (rellVersion the CLI accepts, merkle_hash_version) - blockers for mainnet, warnings otherwise
            (`strict` overrides). Returns {ready, target, network, findings, blockers, nextAction, notes};
            ready=true only with zero blockers - a MAINNET target without `rell` stays blocked until the
            source gate runs, while other targets can be ready with the skipped source gate explicitly
            called out in notes (nothing skipped is silently vouched for). When ready, nextAction is the
            exact `chr deployment create|update --settings chromia.yml --network <target> --blockchain
            <name>` command. Read-only: no keys, no signing, no network writes.
        """.trimIndent(),
        inputSchema = Tool.Input(
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
                    "target" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive(
                                "Deployment target name - a key under deployments in the yaml, e.g. \"testnet\" or \"mainnet\""
                            )
                        )
                    ),
                    "rell" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "Optional Rell sources for the compile + security gate: one source string " +
                                    "(checked as main.rell) or an object of path -> source. Omitting it skips " +
                                    "the source gate (noted; a mainnet target then stays blocked)."
                            )
                        )
                    ),
                    "files" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "Alias for `rell` (same shape) - matches rell_check / run_rell_tests. " +
                                    "`rell` wins when both are present; using the alias is noted."
                            )
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
            required = listOf("yaml", "target")
        ),
        title = "Deployment preflight",
        annotations = null,
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
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
        inputSchema = Tool.Input(
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
        outputSchema = Tool.Output(
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
            gated: (1) the security gate (rell_security_check) - CRITICAL/HIGH findings refuse the deploy
            even on testnet; (2) the compile + config gate (deployment_preflight with the sources) - any
            blocker refuses; (3) only then `chr install` (when chromia.yml declares libs - a fresh project
            has no src/lib and the build fails without it) followed by `chr deployment create|update
            --settings chromia.yml --network testnet --blockchain <name>` run headlessly, signed via POSTCHAIN_CLIENT_PRIVKEY from the
            server-held deploy key for the container (stored by provision_testnet_container, or env
            CHROMIA_TESTNET_DEPLOY_PRIVKEY); (4) the new chain BRID is read back and a live height probe
            verifies it (a fresh chain can take minutes to start - that is reported honestly, with
            verify_deployment as the follow-up). Provide chromiaYml or let the tool generate one from the
            scaffold pins plus the container name; a generated config probes `chr --version` and pins
            compile.rellVersion to the Rell the INSTALLED CLI actually bundles (falling back to the
            scaffold default when chr cannot be probed - the choice and its source are reported), and
            omits deployments.testnet.chains, which a first create must not carry. dryRun defaults to
            TRUE: gates run, nothing deploys. chr is resolved via CHROMIA_CHR_BIN, then a PATH search
            honoring PATHEXT on Windows (.cmd/.bat shims run via `cmd /c`); the resolution used is
            reported. If chr is missing or no deploy key is held, the tool names the exact blocked step
            instead of pretending. Key material never appears in any output.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "rell" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "The dapp sources: one source string (treated as main.rell) or an object of " +
                                    "path -> source. Required - the gates cannot vouch for unseen code."
                            )
                        )
                    ),
                    "files" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive("Alias for `rell` (same shape).")
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
                                "\"create\" for a first deploy (default), \"update\" to upgrade an existing chain"
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
            required = listOf("rell")
        ),
        title = "Deploy testnet chain",
        annotations = null,
        outputSchema = Tool.Output(
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
            "(pass your sources as `rell`, a map of path -> source or a single source string; " +
            "a `files` map is accepted as an alias)"

    /** Working alternatives on the same deployment for commonly disabled tools. */
    private val DISABLED_TOOL_ALTERNATIVES: Map<String, String> = mapOf(
        "rell_check" to CHECK_DAPP_PROJECT_ALTERNATIVE,
        "rell_security_check" to CHECK_DAPP_PROJECT_ALTERNATIVE,
        "run_rell_tests" to CHECK_DAPP_PROJECT_ALTERNATIVE,
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

    fun allTools(compact: Boolean = false, disabled: Set<String> = emptySet()): List<Tool> {
        val all = fullToolList()
        val afterCompact = if (compact) all.filter { it.name !in HELP_TOOL_NAMES } else all
        return if (disabled.isEmpty()) afterCompact else afterCompact.filter { it.name !in disabled }
    }

    private fun fullToolList() = listOf(
        chromiaHelpTool(),
        getPromptsTool(),
        scaffoldDappTool(),
        validateChromiaYmlTool(),
        checkDappProjectTool(),
        checkFt4ImportsTool(),
        rellCheckTool(),
        rellSecurityCheckTool(),
        runRellTestsTool(),
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
        getBlockchainsTransactionsTool(),
        getTransactionsByClusterTool(),
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
        getNodeUnavailabilityTool(),
        getNetworkStats(),
        fetchDocsTool(),
        fetchTool(),
        searchTool(),
        runDappQueriesTool()
    )
}
