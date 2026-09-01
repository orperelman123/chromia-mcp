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
            accounts + auth handler, an authenticated & require()-validated operation, module_args,
            libs block, and a TypeScript client example. FT4 imports compile after `chr install`.
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
                            "enum" to kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive("hello"), JsonPrimitive("ft4"))),
                            "description" to JsonPrimitive("Skeleton flavor: 'hello' (query-only quickstart, default) or 'ft4' (accounts, authenticated operation, TS client).")
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
            Checks compile.rellVersion (required semver N.N.N), blockchains.*.module (module name, not a file path),
            merkle_hash_version == 2, blockchain key webStatic is accepted, and forbids FT4 admin / ras_open modules in libs and moduleArgs.
            Deployments: reserved names mainnet / testnet auto-fill Directory brid + url; custom names require both;
            a Directory Chain BRID that is not 64 hex is an error; official reserved BRIDs must match.
            require_mandatory_flags as a YAML / moduleArgs key is an error (main auth descriptor only).
            Warns if merkle_hash_version is missing on a chain config, deployments.*.container is missing,
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
                            "description" to JsonPrimitive("Help topic, e.g. 'chr_deploy_help' (or 'chr_deploy'). Omit to get the full topic list."),
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
                            "description" to JsonPrimitive("Optional module_args by module name, mirroring chromia.yml, e.g. {\"lib.ft4.core.accounts\": {\"auth_flags\": {\"mandatory\": [\"A\",\"T\"]}}}. Required to exercise real FT4 operations in tests (use ft4_module_args to get production-correct values).")
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
            - HIGH: hardcoded 64+ char hex literals that look like key material
            - MEDIUM: operations with parameters but no require(...) input validation
            Returns line-anchored findings with a concrete fix per finding. ok=true means no
            CRITICAL/HIGH findings. Heuristic static analysis - it does not replace an audit.
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
            (url, official Directory Chain BRID, chains placeholder).
            network must be testnet or mainnet. Does not invent a BRID.
            Since CLI 0.30.0, chr deployment create writes deployments.<net>.chains back into chromia.yml.
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
            Submitted vendored-library files under lib/ft4/ compile but are exempt from the
            import/security scanners (FT4's own sources legitimately contain e.g. ras_open); notes says so.
            Use this as the single pre-deploy gate instead of calling the four tools separately.
            Read-only: does not write files, run chr, generate keys, or send signed transactions.
        """.trimIndent(),
        inputSchema = Tool.Input(
            properties = JsonObject(
                mapOf(
                    "yaml" to JsonObject(
                        mapOf(
                            "type" to JsonPrimitive("string"),
                            "description" to JsonPrimitive("Full chromia.yml contents as a string")
                        )
                    ),
                    "rell" to JsonObject(
                        mapOf(
                            "description" to JsonPrimitive(
                                "One .rell source string, or an object of path -> source (e.g. src/main.rell)"
                            )
                        )
                    )
                )
            ),
            required = listOf("yaml", "rell")
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
            Returns {ok, errors, warnings, hits, forbidden}. Used by check_dapp_project.
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
