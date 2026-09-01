package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr query` read-only flag help.
 * Official tx command page is HELP ONLY (flags + URL; does not send a signed tx).
 * Does not run chr, sign, execute a transaction, generate keys, or invent a BRID.
 * Source: docs.chromia.com/build/cli/commands/query and QueryCommand.kt (0.33.x).
 */
object ChrQueryHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val QUERY_DOCS_URL = "https://docs.chromia.com/build/cli/commands/query"
    const val QUERY_INDEX_URL = QUERY_DOCS_URL
    const val QUERY_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/query/"
    const val QUERY_INDEX_TITLE = "query"  // official H1
    const val TX_DOCS_URL = "https://docs.chromia.com/build/cli/commands/tx"
    const val TX_INDEX_URL = TX_DOCS_URL
    const val TX_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/tx/"
    const val TX_INDEX_TITLE = "tx"  // official H1
    const val GET_STARTED_REAL_TIME_DATA_INDEX_URL = "https://docs.chromia.com/get-started/use-cases/real-time-data"
    const val GET_STARTED_REAL_TIME_DATA_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/real-time-data/"
    const val GET_STARTED_REAL_TIME_DATA_INDEX_TITLE = "Real-time data applications"  // official H1
    const val ECOSYSTEM_STORK_INDEX_URL = "https://docs.chromia.com/ecosystem/extensions/stork"
    const val ECOSYSTEM_STORK_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/extensions/stork/"
    const val ECOSYSTEM_STORK_INDEX_TITLE = "Stork Oracle"  // official H1
    const val ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-bridge-contract"
    const val ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-bridge-contract/"
    const val ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_TITLE = "Deploy the token bridge contract"  // official H1
    const val ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/upgrade-postchain"
    const val ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/upgrade-postchain/"
    const val ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_TITLE = "Upgrade Postchain"  // official H1
    const val ECOSYSTEM_PMC_CLUSTER_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/cluster"
    const val ECOSYSTEM_PMC_CLUSTER_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/cluster/"
    const val ECOSYSTEM_PMC_CLUSTER_INDEX_TITLE = "cluster"  // official H1
    const val ECOSYSTEM_GOV_GETTING_STARTED_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started"
    const val ECOSYSTEM_GOV_GETTING_STARTED_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/"
    const val ECOSYSTEM_GOV_GETTING_STARTED_INDEX_TITLE = "Getting started"  // official H1
    const val ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-power-strategies"
    const val ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-power-strategies/"
    const val ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_TITLE = "Vote power strategies"  // official H1
    const val ECOSYSTEM_BLOCK_EXPLORER_INDEX_URL = "https://docs.chromia.com/ecosystem/block-explorer"
    const val ECOSYSTEM_BLOCK_EXPLORER_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/block-explorer/"
    const val ECOSYSTEM_BLOCK_EXPLORER_INDEX_TITLE = "Block Explorer"  // official H1
    const val ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-client/work-with-client"
    const val ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/bridge-client/work-with-client/"
    const val ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_TITLE = "Work with the client"  // official H1
    const val LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_URL = "https://learn.chromia.com/courses/associate-function/introduction"
    const val LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/associate-function/introduction/"
    const val LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_TITLE = "Associate objects by a key"  // official H1
    const val LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-entity/write-queries"
    const val LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-entity/write-queries/"
    const val LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_TITLE = "Write a query to retrieve all books"  // official H1
    const val LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_URL = "https://learn.chromia.com/courses/big-data/python-side-description"
    const val LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/big-data/python-side-description/"
    const val LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_TITLE = "Python components"  // official H1
    const val LEARN_EVM_CORE_CONCEPTS_INDEX_URL = "https://learn.chromia.com/courses/chromia-for-evm-developers/concepts"
    const val LEARN_EVM_CORE_CONCEPTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-for-evm-developers/concepts/"
    const val LEARN_EVM_CORE_CONCEPTS_INDEX_TITLE = "Core concepts"  // official H1
    const val LEARN_FT4_DEMO_ASSET_REG_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/asset-registration"
    const val LEARN_FT4_DEMO_ASSET_REG_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/asset-registration/"
    const val LEARN_FT4_DEMO_ASSET_REG_INDEX_TITLE = "Asset Registration and Minting"  // official H1
    const val LEARN_NEWS_OPS_QUERIES_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries"
    const val LEARN_NEWS_OPS_QUERIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries/"
    const val LEARN_NEWS_OPS_QUERIES_INDEX_TITLE = "Lesson 3 - Explore operations and queries"  // official H1
    const val LEARN_TTT_BASIC_OPS_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/data-modeling/basic-operations"
    const val LEARN_TTT_BASIC_OPS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/data-modeling/basic-operations/"
    const val LEARN_TTT_BASIC_OPS_INDEX_TITLE = "Perform basic operations"  // official H1
    const val LEARN_VECTOR_DB_EMBEDDINGS_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/generate-embeddings"
    const val LEARN_VECTOR_DB_EMBEDDINGS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/generate-embeddings/"
    const val LEARN_VECTOR_DB_EMBEDDINGS_INDEX_TITLE = "Generate sentence embeddings"  // official H1
    const val LEARN_ZK_DAPP_QUERIES_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-queries"
    const val LEARN_ZK_DAPP_QUERIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-queries/"
    const val LEARN_ZK_DAPP_QUERIES_INDEX_TITLE = "Dapp queries overview"  // official H1
    const val LEARN_CHAT_AGENT_API_KEY_INDEX_URL = "https://learn.chromia.com/courses/chat-agent-course/configure-api-key"
    const val LEARN_CHAT_AGENT_API_KEY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chat-agent-course/configure-api-key/"
    const val LEARN_CHAT_AGENT_API_KEY_INDEX_TITLE = "Configure your API key"  // official H1
    const val LEARN_WEB3_COMPARE_BACKEND_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/compare-backend"
    const val LEARN_WEB3_COMPARE_BACKEND_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/compare-backend/"
    const val LEARN_WEB3_COMPARE_BACKEND_INDEX_TITLE = "Comparing backends"  // official H1
    const val RELL_EXPRESSIONS_LAMBDA_INDEX_URL = "https://docs.chromia.com/rell/language-features/expressions/lambda-expressions"
    const val RELL_EXPRESSIONS_LAMBDA_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/expressions/lambda-expressions/"
    const val RELL_EXPRESSIONS_LAMBDA_INDEX_TITLE = "Lambda expressions"  // official H1
    const val RELL_RELLDOC_INDEX_URL = "https://docs.chromia.com/rell/rell-doc"
    const val RELL_RELLDOC_INDEX_URL_SLASH = "https://docs.chromia.com/rell/rell-doc/"
    const val RELL_RELLDOC_INDEX_TITLE = "RellDoc: Documentation Comments for Rell"  // official H1
    const val LEARN_COMPARE_AUTHENTICATION_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/compare-authentication"
    const val LEARN_COMPARE_AUTHENTICATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/compare-authentication/"
    const val LEARN_COMPARE_AUTHENTICATION_INDEX_TITLE = "Authentication"  // official H1
    const val RUN_DAPP_URL = "https://docs.chromia.com/get-started/create-dapp/run-dapp-cli"
    const val NODE_DOCS_URL = ChrNodeHelp.NODE_DOCS_URL
    const val DEFAULT_API_URL = ChrNodeHelp.DEFAULT_API_URL
    const val MAINNET_DIRECTORY_BRID = WriteDeploymentConfig.MAINNET_DIRECTORY_BRID
    const val TESTNET_DIRECTORY_BRID = WriteDeploymentConfig.TESTNET_DIRECTORY_BRID

    fun officialLocalExample(): String =
        "chr query hello_world"

    fun localExample(): String =
        "chr query --blockchain-rid <BlockchainRID> hello_world"

    fun localArgsExample(): String =
        "chr query --blockchain-rid <BlockchainRID> hello_world foo=17 bar=hello 'baz=\"hello world\"'"

    fun dashDashExample(): String =
        "chr query my_query -- arg1=foo arg2=x\"AB12\""

    fun namedDeploymentExample(name: String = DappScaffold.DEFAULT_NAME): String {
        val chain = DappScaffold.normalizeName(name)
        return "chr query --network testnet --blockchain $chain hello_world"
    }

    fun mainnetExample(): String =
        "chr query --mainnet --blockchain-rid <BlockchainRID> hello_world"

    fun dictExample(): String =
        "chr query dict_arg 'arg=[\"key\": 12]'"

    fun targetNote(): String = """
        If no dApp target or deployment options are used, `chr query` targets the local node
        started with `chr node start` (default API $DEFAULT_API_URL).
        Pass `--api-url` when the node is not on that default.
        Named deployment in chromia.yml: `-d, --network=<text>` plus `-bc, --blockchain=<text>`.
        Explicit target: `-brid, --blockchain-rid` / `--cid` / `--api-url`, or `--mainnet` / `--testnet`
        instead of `--api-url` (CLI 0.28.0+).
        Directory Chain BRIDs for a deployments block are official write_deployment_config values
        (mainnet $MAINNET_DIRECTORY_BRID, testnet $TESTNET_DIRECTORY_BRID). Do not invent a dapp BRID.
        Use the BRID printed by `chr node start` (or `filter_blockchains`) — never invent hex.
    """.trimIndent()

    fun notes(): String = """
        Chromia CLI $CLI_SERIES read-only `chr query` flag help. Java 21+, Postgres 16+.
        Official invoke page: $QUERY_DOCS_URL
        Official first-run (run-dapp-cli $RUN_DAPP_URL): `chr query hello_world` → `"Hello World!"`.
        Local node page: $NODE_DOCS_URL
        Default local REST/API URL: $DEFAULT_API_URL
        Arguments are named `key=value` pairs or a single GTV dict. Query must be named parameters.
        Output: `-f, --output-format=(pretty|raw|JSON|XML|YAML)` (default pretty).
        This is not `chr tx`. `chr query` does not sign and does not send a transaction.
        Official BUILD tx command page ($TX_DOCS_URL 200): HELP ONLY flags --await --ft-auth --ft-account-id --evm-auth --ft-register-account --iccf-tx --nop --timeb-at --timeb-after this tool does not send signed skip sample BRID hex skip set_name signed send no sample keys no invented 64-hex.
        Official BUILD cli/commands/query ($QUERY_INDEX_URL 307 $QUERY_INDEX_URL_SLASH 200 $QUERY_INDEX_TITLE): intro Usage chr query hello_world --blockchain-rid <BlockchainRID> Query-only WRITE SKIP skip signed txs no sample keys no invented 64-hex no keygen do not invent flags BRIDs do not document chr tx signed send.
        Official BUILD cli/commands/tx ($TX_INDEX_URL 307 $TX_INDEX_URL_SLASH 200 $TX_INDEX_TITLE): HELP ONLY WRITE SKIP this tool does not send signed skip sample BRID hex skip set_name signed send no sample keys no invented 64-hex no keygen do not invent flags BRIDs.
        Official get-started/use-cases/real-time-data INDEX ($GET_STARTED_REAL_TIME_DATA_INDEX_URL 307 $GET_STARTED_REAL_TIME_DATA_INDEX_URL_SLASH 200 $GET_STARTED_REAL_TIME_DATA_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no sign recipe.
        Official ECOSYSTEM ecosystem/extensions/stork INDEX ($ECOSYSTEM_STORK_INDEX_URL 307 $ECOSYSTEM_STORK_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_STORK_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/bridge/deploy-bridge/deploy-bridge-contract INDEX ($ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_URL 307 $ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/nodes/upgrade-postchain INDEX ($ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_URL 307 $ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/cluster INDEX ($ECOSYSTEM_PMC_CLUSTER_INDEX_URL 307 $ECOSYSTEM_PMC_CLUSTER_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_CLUSTER_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/getting-started INDEX ($ECOSYSTEM_GOV_GETTING_STARTED_INDEX_URL 307 $ECOSYSTEM_GOV_GETTING_STARTED_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_GETTING_STARTED_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure/vote-power-strategies INDEX ($ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_URL 307 $ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/block-explorer INDEX ($ECOSYSTEM_BLOCK_EXPLORER_INDEX_URL 307 $ECOSYSTEM_BLOCK_EXPLORER_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_BLOCK_EXPLORER_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/bridge/bridge-client/work-with-client INDEX ($ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_URL 307 $ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/associate-function/introduction INDEX ($LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_URL 301 $LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/book-entity/write-queries INDEX ($LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_URL 301 $LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/big-data/python-side-description INDEX ($LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_URL 301 $LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_URL_SLASH 200 H1 $LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/chromia-for-evm-developers/concepts INDEX ($LEARN_EVM_CORE_CONCEPTS_INDEX_URL 301 $LEARN_EVM_CORE_CONCEPTS_INDEX_URL_SLASH 200 H1 $LEARN_EVM_CORE_CONCEPTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/ft4-demo-app/module-blockchain/asset-registration INDEX ($LEARN_FT4_DEMO_ASSET_REG_INDEX_URL 301 $LEARN_FT4_DEMO_ASSET_REG_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_ASSET_REG_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/operations-queries INDEX ($LEARN_NEWS_OPS_QUERIES_INDEX_URL 301 $LEARN_NEWS_OPS_QUERIES_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_OPS_QUERIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/tic-tac-toe/module-one/data-modeling/basic-operations INDEX ($LEARN_TTT_BASIC_OPS_INDEX_URL 301 $LEARN_TTT_BASIC_OPS_INDEX_URL_SLASH 200 H1 $LEARN_TTT_BASIC_OPS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/vector-db-movie-demo/data-pipeline/generate-embeddings INDEX ($LEARN_VECTOR_DB_EMBEDDINGS_INDEX_URL 301 $LEARN_VECTOR_DB_EMBEDDINGS_INDEX_URL_SLASH 200 H1 $LEARN_VECTOR_DB_EMBEDDINGS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof dapp-queries ($LEARN_ZK_DAPP_QUERIES_INDEX_URL 301 $LEARN_ZK_DAPP_QUERIES_INDEX_URL_SLASH 200 H1 $LEARN_ZK_DAPP_QUERIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX chat-agent-course configure-api-key ($LEARN_CHAT_AGENT_API_KEY_INDEX_URL 301 $LEARN_CHAT_AGENT_API_KEY_INDEX_URL_SLASH 200 H1 $LEARN_CHAT_AGENT_API_KEY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official RELL rell/language-features/expressions/lambda-expressions INDEX ($RELL_EXPRESSIONS_LAMBDA_INDEX_URL 307 $RELL_EXPRESSIONS_LAMBDA_INDEX_URL_SLASH 200 H1 $RELL_EXPRESSIONS_LAMBDA_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX web3-for-web2-devs compare-backend ($LEARN_WEB3_COMPARE_BACKEND_INDEX_URL 301 $LEARN_WEB3_COMPARE_BACKEND_INDEX_URL_SLASH 200 H1 $LEARN_WEB3_COMPARE_BACKEND_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/rell-doc INDEX ($RELL_RELLDOC_INDEX_URL 307 $RELL_RELLDOC_INDEX_URL_SLASH 200 H1 $RELL_RELLDOC_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/web3-for-web2-devs/compare-authentication INDEX ($LEARN_COMPARE_AUTHENTICATION_INDEX_URL 301 $LEARN_COMPARE_AUTHENTICATION_INDEX_URL_SLASH 200 H1 $LEARN_COMPARE_AUTHENTICATION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP. HELP ONLY WRITE SKIP auth: query/help only, no register/login/transfer/auth/keygen/admin/mint/burn/create-accounts, no keys, no signing.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", QUERY_DOCS_URL)
        put("query_index_docs", QUERY_INDEX_URL)
        put("query_index_url_slash", QUERY_INDEX_URL_SLASH)
        put("query_index_title", QUERY_INDEX_TITLE)
        put("run_dapp", RUN_DAPP_URL)
        put("default_api_url", DEFAULT_API_URL)
        put("read_only", true)
        put(
            "commands",
            buildJsonObject {
                put("official_local", officialLocalExample())
                put("official_local_result", "Hello World!")
                put("local", localExample())
                put("local_args", localArgsExample())
                put("dash_dash", dashDashExample())
                put("named_deployment", namedDeploymentExample())
                put("mainnet", mainnetExample())
                put("dict", dictExample())
            }
        )
        put(
            "flags",
            buildJsonObject {
                put("settings", "-s, --settings=<settings>")
                put("config", "-cfg, --config=<config>")
                put("blockchain_rid", "-brid, --blockchain-rid=<text>")
                put("cid", "--cid=<int>")
                put("api_url", "--api-url=<text>")
                put("network_alias", "--mainnet, --testnet  # use instead of --api-url")
                put("network", "-d, --network=<text>  # named deployment in chromia.yml")
                put("blockchain", "-bc, --blockchain=<text>")
                put("output_format", "-f, --output-format=(pretty|raw|JSON|XML|YAML)")
            }
        )
        put("target", targetNote())
        put("mainnet_directory_brid", MAINNET_DIRECTORY_BRID)
        put("testnet_directory_brid", TESTNET_DIRECTORY_BRID)
        put("tx_docs", TX_DOCS_URL)
        put("tx_index_docs", TX_INDEX_URL)
        put("tx_index_url_slash", TX_INDEX_URL_SLASH)
        put("tx_index_title", TX_INDEX_TITLE)
        put("tx_help_only", true)
        put(
            "official_tx_flags",
            buildJsonObject {
                put("await", "-a, --await / --no-await")
                put("ft_auth", "--ft-auth")
                put("ft_account_id", "--ft-account-id=<text>")
                put("evm_auth", "--evm-auth=<address>")
                put("ft_register_account", "--ft-register-account")
                put("iccf_tx", "--iccf-tx=<text>")
                put("iccf_source", "--iccf-source=<value>")
                put("nop", "-nop")
                put("timeb_at", "--timeb-at=<value>")
                put("timeb_after", "--timeb-after=<value>")
                put(
                    "secret",
                    "--secret=<path>  # existing secret file; this tool does not write one"
                )
                put("key_id", "--key-id=<key_id>  # existing key id")
            }
        )
        put("get_started_real_time_data_index_docs", GET_STARTED_REAL_TIME_DATA_INDEX_URL)
        put("get_started_real_time_data_index_url_slash", GET_STARTED_REAL_TIME_DATA_INDEX_URL_SLASH)
        put("get_started_real_time_data_index_title", GET_STARTED_REAL_TIME_DATA_INDEX_TITLE)
        put("ecosystem_stork_index_url_slash", ECOSYSTEM_STORK_INDEX_URL_SLASH)
        put("ecosystem_stork_index_title", ECOSYSTEM_STORK_INDEX_TITLE)
        put("ecosystem_deploy_bridge_contract_index_url_slash", ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_URL_SLASH)
        put("ecosystem_deploy_bridge_contract_index_title", ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_TITLE)
        put("ecosystem_upgrade_postchain_index_url_slash", ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_URL_SLASH)
        put("ecosystem_upgrade_postchain_index_title", ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_TITLE)
        put("ecosystem_pmc_cluster_index_url_slash", ECOSYSTEM_PMC_CLUSTER_INDEX_URL_SLASH)
        put("ecosystem_pmc_cluster_index_title", ECOSYSTEM_PMC_CLUSTER_INDEX_TITLE)
        put("ecosystem_gov_getting_started_index_url_slash", ECOSYSTEM_GOV_GETTING_STARTED_INDEX_URL_SLASH)
        put("ecosystem_gov_getting_started_index_title", ECOSYSTEM_GOV_GETTING_STARTED_INDEX_TITLE)
        put("ecosystem_gov_vote_power_strategies_index_url_slash", ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_URL_SLASH)
        put("ecosystem_gov_vote_power_strategies_index_title", ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_TITLE)
        put("ecosystem_block_explorer_index_url_slash", ECOSYSTEM_BLOCK_EXPLORER_INDEX_URL_SLASH)
        put("ecosystem_block_explorer_index_title", ECOSYSTEM_BLOCK_EXPLORER_INDEX_TITLE)
        put("ecosystem_bridge_work_with_client_index_url_slash", ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_URL_SLASH)
        put("ecosystem_bridge_work_with_client_index_title", ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_TITLE)
        put("learn_associate_function_intro_index_url_slash", LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_URL_SLASH)
        put("learn_associate_function_intro_index_title", LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_TITLE)
        put("learn_book_review_write_queries_index_url_slash", LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_URL_SLASH)
        put("learn_book_review_write_queries_index_title", LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_TITLE)
        put("learn_big_data_python_components_index_url_slash", LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_URL_SLASH)
        put("learn_big_data_python_components_index_title", LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_TITLE)
        put("learn_evm_core_concepts_index_url_slash", LEARN_EVM_CORE_CONCEPTS_INDEX_URL_SLASH)
        put("learn_evm_core_concepts_index_title", LEARN_EVM_CORE_CONCEPTS_INDEX_TITLE)
        put("learn_ft4_demo_asset_reg_index_url_slash", LEARN_FT4_DEMO_ASSET_REG_INDEX_URL_SLASH)
        put("learn_ft4_demo_asset_reg_index_title", LEARN_FT4_DEMO_ASSET_REG_INDEX_TITLE)
        put("learn_news_ops_queries_index_url_slash", LEARN_NEWS_OPS_QUERIES_INDEX_URL_SLASH)
        put("learn_news_ops_queries_index_title", LEARN_NEWS_OPS_QUERIES_INDEX_TITLE)
        put("learn_ttt_basic_ops_index_url_slash", LEARN_TTT_BASIC_OPS_INDEX_URL_SLASH)
        put("learn_ttt_basic_ops_index_title", LEARN_TTT_BASIC_OPS_INDEX_TITLE)
        put("learn_vector_db_embeddings_index_url_slash", LEARN_VECTOR_DB_EMBEDDINGS_INDEX_URL_SLASH)
        put("learn_vector_db_embeddings_index_title", LEARN_VECTOR_DB_EMBEDDINGS_INDEX_TITLE)
        put("learn_zk_dapp_queries_index_url_slash", LEARN_ZK_DAPP_QUERIES_INDEX_URL_SLASH)
        put("learn_zk_dapp_queries_index_title", LEARN_ZK_DAPP_QUERIES_INDEX_TITLE)
        put("learn_chat_agent_api_key_index_url_slash", LEARN_CHAT_AGENT_API_KEY_INDEX_URL_SLASH)
        put("learn_chat_agent_api_key_index_title", LEARN_CHAT_AGENT_API_KEY_INDEX_TITLE)
        put("rell_expressions_lambda_index_url_slash", RELL_EXPRESSIONS_LAMBDA_INDEX_URL_SLASH)
        put("rell_expressions_lambda_index_title", RELL_EXPRESSIONS_LAMBDA_INDEX_TITLE)
        put("learn_web3_compare_backend_index_url_slash", LEARN_WEB3_COMPARE_BACKEND_INDEX_URL_SLASH)
        put("learn_web3_compare_backend_index_title", LEARN_WEB3_COMPARE_BACKEND_INDEX_TITLE)
        put("rell_relldoc_index_url_slash", RELL_RELLDOC_INDEX_URL_SLASH)
        put("rell_relldoc_index_title", RELL_RELLDOC_INDEX_TITLE)
        put("learn_compare_authentication_index_url_slash", LEARN_COMPARE_AUTHENTICATION_INDEX_URL_SLASH)
        put("learn_compare_authentication_index_title", LEARN_COMPARE_AUTHENTICATION_INDEX_TITLE)
        put("notes", notes())
    }
}

// Official BUILD cli/commands/query leftovers encoded as QUERY_INDEX_* (query-only).
// Official BUILD cli/commands/tx leftovers encoded as TX_INDEX_* (query-only HELP ONLY).
// Official get-started/use-cases/real-time-data INDEX leftovers encoded as GET_STARTED_REAL_TIME_DATA_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/extensions/stork INDEX leftovers encoded as ECOSYSTEM_STORK_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/bridge/deploy-bridge/deploy-bridge-contract INDEX leftovers encoded as ECOSYSTEM_DEPLOY_BRIDGE_CONTRACT_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/nodes/upgrade-postchain INDEX leftovers encoded as ECOSYSTEM_UPGRADE_POSTCHAIN_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/cluster INDEX leftovers encoded as ECOSYSTEM_PMC_CLUSTER_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/getting-started INDEX leftovers encoded as ECOSYSTEM_GOV_GETTING_STARTED_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure/vote-power-strategies INDEX leftovers encoded as ECOSYSTEM_GOV_VOTE_POWER_STRATEGIES_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/block-explorer INDEX leftovers encoded as ECOSYSTEM_BLOCK_EXPLORER_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/bridge/bridge-client/work-with-client INDEX leftovers encoded as ECOSYSTEM_BRIDGE_WORK_WITH_CLIENT_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/associate-function/introduction INDEX leftovers encoded as LEARN_ASSOCIATE_FUNCTION_INTRO_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/book-review/book-entity/write-queries INDEX leftovers encoded as LEARN_BOOK_REVIEW_WRITE_QUERIES_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/big-data/python-side-description INDEX leftovers encoded as LEARN_BIG_DATA_PYTHON_COMPONENTS_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/chromia-for-evm-developers/concepts INDEX leftovers encoded as LEARN_EVM_CORE_CONCEPTS_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/ft4-demo-app/module-blockchain/asset-registration INDEX leftovers encoded as LEARN_FT4_DEMO_ASSET_REG_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/my-news-feed/module-one/operations-queries INDEX leftovers encoded as LEARN_NEWS_OPS_QUERIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-one/data-modeling/basic-operations INDEX leftovers encoded as LEARN_TTT_BASIC_OPS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/vector-db-movie-demo/data-pipeline/generate-embeddings INDEX leftovers encoded as LEARN_VECTOR_DB_EMBEDDINGS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof dapp-queries leftovers encoded as LEARN_ZK_DAPP_QUERIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX chat-agent-course configure-api-key leftovers encoded as LEARN_CHAT_AGENT_API_KEY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/expressions/lambda-expressions INDEX leftovers encoded as RELL_EXPRESSIONS_LAMBDA_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX web3-for-web2-devs compare-backend leftovers encoded as LEARN_WEB3_COMPARE_BACKEND_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/rell-doc INDEX leftovers encoded as RELL_RELLDOC_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/web3-for-web2-devs/compare-authentication INDEX leftovers encoded as LEARN_COMPARE_AUTHENTICATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
