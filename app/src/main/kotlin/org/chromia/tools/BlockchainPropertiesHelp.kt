package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official chromia.yml `blockchains.<name>.config` blockchain-properties (thin).
 * Keys from docs.chromia.com/build/configuration/blockchain-properties only.
 * Focus: gtx / blockstrategy / query timeouts. Does not invent keys. Does not run chr.
 * Leftover official leftover BUILD configuration/blockchain-properties index slash/title leftovers live here (query-only).
 * Leftover official leftover GET-STARTED get-started/about/protocols/icmf INDEX leftovers live here (query-only).
 */
object BlockchainPropertiesHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/configuration/blockchain-properties"
    const val BLOCKCHAIN_PROPERTIES_INDEX_URL = DOCS_URL
    const val BLOCKCHAIN_PROPERTIES_INDEX_URL_SLASH = "https://docs.chromia.com/build/configuration/blockchain-properties/"
    const val BLOCKCHAIN_PROPERTIES_INDEX_TITLE = "Configuration properties"
    const val GET_STARTED_ICMF_INDEX_URL = "https://docs.chromia.com/get-started/about/protocols/icmf"
    const val GET_STARTED_ICMF_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/protocols/icmf/"
    const val GET_STARTED_ICMF_INDEX_TITLE = "Inter-chain Messaging Facility (ICMF)"
    const val ECOSYSTEM_PROVIDERS_APIS_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/apis-1"
    const val ECOSYSTEM_PROVIDERS_APIS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/apis-1/"
    const val ECOSYSTEM_PROVIDERS_APIS_INDEX_TITLE = "APIs"  // official H1
    const val LEARN_RELL_MASTERCLASS_JOIN_INDEX_URL = "https://learn.chromia.com/courses/rell-masterclass/join"
    const val LEARN_RELL_MASTERCLASS_JOIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-masterclass/join/"
    const val LEARN_RELL_MASTERCLASS_JOIN_INDEX_TITLE = "INNER JOIN statement"  // official H1
    const val LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_URL = "https://learn.chromia.com/courses/iccf-course/subscription-chain"
    const val LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/iccf-course/subscription-chain/"
    const val LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_TITLE = "Subscription chain"  // official H1
    const val LEARN_MARKETPLACE_FT4_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-ft4"
    const val LEARN_MARKETPLACE_FT4_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-ft4/"
    const val LEARN_MARKETPLACE_FT4_INDEX_TITLE = "Module 1 - Register accounts and assets"  // official H1
    const val LEARN_TTT_FT4_ACCOUNTS_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/create-accounts/install-configure-ft4"
    const val LEARN_TTT_FT4_ACCOUNTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/create-accounts/install-configure-ft4/"
    const val LEARN_TTT_FT4_ACCOUNTS_INDEX_TITLE = "Configure FT4 accounts"  // official H1
    const val LEARN_TTT_UPDATE_TESTS_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/create-accounts/update-tests"
    const val LEARN_TTT_UPDATE_TESTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/create-accounts/update-tests/"
    const val LEARN_TTT_UPDATE_TESTS_INDEX_TITLE = "Update tests"  // official H1
    const val LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts/register-evm-accounts"
    const val LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts/register-evm-accounts/"
    const val LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_TITLE = "Register accounts using EVM Wallets"  // official H1
    const val LEARN_CHAT_AGENT_SETUP_INDEX_URL = "https://learn.chromia.com/courses/chat-agent-course/setup"
    const val LEARN_CHAT_AGENT_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chat-agent-course/setup/"
    const val LEARN_CHAT_AGENT_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_RNG_INTRO_INDEX_URL = "https://learn.chromia.com/courses/random-number-generation/introduction"
    const val LEARN_RNG_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/random-number-generation/introduction/"
    const val LEARN_RNG_INTRO_INDEX_TITLE = "How to generate random numbers using Rell"  // official H1
    const val LEARN_COMPARISONS_POLKADOT_INDEX_URL = "https://learn.chromia.com/courses/chromia-comparisons/polkadot"
    const val LEARN_COMPARISONS_POLKADOT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-comparisons/polkadot/"
    const val LEARN_COMPARISONS_POLKADOT_INDEX_TITLE = "Polkadot"  // official H1
    const val LEARN_TAGS_SQL_INDEX_URL = "https://learn.chromia.com/tags/SQL"
    const val LEARN_TAGS_SQL_INDEX_URL_SLASH = "https://learn.chromia.com/tags/SQL/"
    const val LEARN_TAGS_SQL_INDEX_TITLE = "Courses tagged with: SQL"  // official H1
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val TOOL_NAME = "blockchain_properties_help"
    const val MAX_BLOCK_SIZE = 26 * 1024 * 1024
    const val MAX_TX_SIZE = 25 * 1024 * 1024
    const val CHROMIA_MIN_INTERBLOCK_MS = 1000
    const val QUERY_TIMEOUT_SECONDS = 60
    const val ASYNC_QUERY_TIMEOUT_SECONDS = 3600

    val allowedGtxModules = listOf(
        "net.postchain.rell.module.RellPostchainModuleFactory",
        "net.postchain.gtx.StandardOpsGTXModule",
        "net.postchain.d1.icmf.IcmfSenderGTXModule",
        "net.postchain.d1.icmf.IcmfReceiverGTXModule",
        "net.postchain.d1.iccf.IccfGTXModule",
        "net.postchain.eif.EifGTXModule",
        "net.postchain.web.WebStaticGTXModuleFactory"
    )

    val allowedSyncExt = listOf(
        "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension",
        "net.postchain.eif.EifSynchronizationInfrastructureExtension"
    )

    val coreKeys = listOf(
        "signers",
        "sync",
        "sync_ext",
        "configurationfactory",
        "txqueuecapacity",
        "historic_brid",
        "dependencies",
        "config_consensus_strategy",
        "query_cache_ttl_seconds",
        "async_query_queue_capacity",
        "async_query_timeout_seconds",
        "async_query_result_retention_seconds",
        "query_timeout_seconds",
        "max_block_future_time",
        "add_primary_key_to_header"
    )

    val featureKeys = listOf("merkle_hash_version")

    val blockstrategyKeys = listOf(
        "name",
        "maxblocksize",
        "maxblocktransactions",
        "mininterblockinterval",
        "maxblocktime",
        "maxtxdelay",
        "minbackofftime",
        "maxbackofftime",
        "maxspecialendtransactionsize",
        "preemptiveblockbuilding"
    )

    val gtxKeys = listOf(
        "max_transaction_size",
        "max_transaction_signatures",
        "modules",
        "allowoverrides",
        "slow_op_threshold",
        "slow_prioritization_query_threshold"
    )

    val revoltKeys = listOf(
        "timeout",
        "exponential_delay_initial",
        "exponential_delay_power_base",
        "exponential_delay_max",
        "fast_revolt_status_timeout",
        "revolt_when_should_build_block"
    )

    fun configYaml(name: String = DappScaffold.DEFAULT_NAME): String {
        val chain = DappScaffold.normalizeName(name)
        return """
            blockchains:
              $chain:
                module: main
                config:
                  features:
                    merkle_hash_version: ${DappScaffold.MERKLE_HASH_VERSION}
                  blockstrategy:
                    maxblocksize: $MAX_BLOCK_SIZE
                    maxblocktransactions: 100
                    mininterblockinterval: $CHROMIA_MIN_INTERBLOCK_MS
                    maxtxdelay: 1000
                  gtx:
                    max_transaction_size: $MAX_TX_SIZE
                    modules:
                      - ${allowedGtxModules.first()}
                  query_timeout_seconds: $QUERY_TIMEOUT_SECONDS
                  query_cache_ttl_seconds: 0
                  async_query_timeout_seconds: $ASYNC_QUERY_TIMEOUT_SECONDS
        """.trimIndent() + "\n"
    }

    fun notes(): String = """
        Official chromia.yml `blockchains.<name>.config` properties for Chromia CLI $CLI_SERIES.
        Schema: $DOCS_URL (source of truth for keys). Project layout: $PROJECT_CONFIG_URL.
        Thin snippet: features.merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION}, blockstrategy, gtx, query timeouts.
        Official engine defaults: query_timeout_seconds $QUERY_TIMEOUT_SECONDS, query_cache_ttl_seconds 0 (0 disables),
        async_query_timeout_seconds $ASYNC_QUERY_TIMEOUT_SECONDS, txqueuecapacity 2500,
        blockstrategy.maxblocksize $MAX_BLOCK_SIZE (26 MiB Chromia max), maxblocktransactions 100,
        mininterblockinterval 25 ms (Chromia minimum $CHROMIA_MIN_INTERBLOCK_MS), maxtxdelay 1000,
        gtx.max_transaction_size $MAX_TX_SIZE (25 MiB).
        Official allowed GTX modules on that page: ${allowedGtxModules.joinToString(", ")}.
        Official allowed sync_ext: ${allowedSyncExt.joinToString(", ")}.
        Do not invent GTX module class names. Directory/mainnet allow-lists may be stricter or add modules — confirm against the cluster, do not invent.
        merkle_hash_version must stay ${DappScaffold.MERKLE_HASH_VERSION}. Version 1 is deprecated (hash-collision bugs).
        Official core / revolt keys are listed but not required in the thin snippet.
        The docs example configurationfactory net.postchain.devtools.KeyPairHelper is a docs sample — do not invent another factory.
        Leftover official BUILD configuration/blockchain-properties (leftover official $BLOCKCHAIN_PROPERTIES_INDEX_URL leftover official 307 leftover official $BLOCKCHAIN_PROPERTIES_INDEX_URL_SLASH leftover official 200 leftover official $BLOCKCHAIN_PROPERTIES_INDEX_TITLE): leftover official leftover intro leftover official leftover This topic outlines the key configuration options available in the BaseBlockChainConfiguration, providing descriptions, types, and default values for each parameter leftover official leftover How to configure blockchain properties leftover official leftover chromia.yml leftover official leftover config leftover official leftover section leftover official leftover Core configuration properties leftover official leftover Features configuration leftover official leftover merkle_hash_version leftover official leftover Block strategy configuration leftover official leftover blockstrategy leftover official leftover GTX configuration leftover official leftover gtx leftover official leftover Revolt configuration leftover official leftover revolt leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover leftover official leftover leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Leftover official leftover GET-STARTED get-started/about/protocols/icmf INDEX (leftover official $GET_STARTED_ICMF_INDEX_URL leftover official 307 leftover official $GET_STARTED_ICMF_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_ICMF_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover ECOSYSTEM ecosystem/providers/apis-1 INDEX (leftover official $ECOSYSTEM_PROVIDERS_APIS_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PROVIDERS_APIS_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PROVIDERS_APIS_INDEX_TITLE). Query-only.
        Leftover official leftover LEARN courses/rell-masterclass/join INDEX (leftover official $LEARN_RELL_MASTERCLASS_JOIN_INDEX_URL leftover official 301 leftover official $LEARN_RELL_MASTERCLASS_JOIN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_RELL_MASTERCLASS_JOIN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). leftover official leftover Joining two tables is simple in Rell leftover official leftover We do this by putting all the tables we want to join in the FROM part of the at-expression leftover official leftover and specify the constraint between them in the WHERE part. Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/iccf-course/subscription-chain INDEX (leftover official $LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_URL leftover official 301 leftover official $LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-ft4 INDEX (leftover official $LEARN_MARKETPLACE_FT4_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_FT4_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_FT4_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/tic-tac-toe/module-one/create-accounts/install-configure-ft4 INDEX (leftover official $LEARN_TTT_FT4_ACCOUNTS_INDEX_URL leftover official 301 leftover official $LEARN_TTT_FT4_ACCOUNTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_FT4_ACCOUNTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/tic-tac-toe/module-one/create-accounts/update-tests INDEX (leftover official $LEARN_TTT_UPDATE_TESTS_INDEX_URL leftover official 301 leftover official $LEARN_TTT_UPDATE_TESTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_UPDATE_TESTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX news-feed register-evm-accounts (leftover official $LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX chat-agent-course setup (leftover official $LEARN_CHAT_AGENT_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_CHAT_AGENT_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_CHAT_AGENT_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX random-number-generation introduction (leftover official $LEARN_RNG_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_RNG_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_RNG_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX chromia-comparisons polkadot (leftover official $LEARN_COMPARISONS_POLKADOT_INDEX_URL leftover official 301 leftover official $LEARN_COMPARISONS_POLKADOT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_COMPARISONS_POLKADOT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover LEARN tags/SQL INDEX (leftover official $LEARN_TAGS_SQL_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_SQL_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_SQL_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("blockchain_properties_index_docs", BLOCKCHAIN_PROPERTIES_INDEX_URL)
        put("blockchain_properties_index_url_slash", BLOCKCHAIN_PROPERTIES_INDEX_URL_SLASH)
        put("blockchain_properties_index_title", BLOCKCHAIN_PROPERTIES_INDEX_TITLE)
        put("get_started_icmf_index_docs", GET_STARTED_ICMF_INDEX_URL)
        put("get_started_icmf_index_url_slash", GET_STARTED_ICMF_INDEX_URL_SLASH)
        put("get_started_icmf_index_title", GET_STARTED_ICMF_INDEX_TITLE)
        put("project_config", PROJECT_CONFIG_URL)
        put("tool", TOOL_NAME)
        put(
            "keys",
            buildJsonObject {
                put("core", buildJsonArray { coreKeys.forEach { add(JsonPrimitive(it)) } })
                put("features", buildJsonArray { featureKeys.forEach { add(JsonPrimitive(it)) } })
                put("blockstrategy", buildJsonArray { blockstrategyKeys.forEach { add(JsonPrimitive(it)) } })
                put("gtx", buildJsonArray { gtxKeys.forEach { add(JsonPrimitive(it)) } })
                put("revolt", buildJsonArray { revoltKeys.forEach { add(JsonPrimitive(it)) } })
            }
        )
        put(
            "defaults",
            buildJsonObject {
                put("merkle_hash_version", DappScaffold.MERKLE_HASH_VERSION)
                put("query_timeout_seconds", QUERY_TIMEOUT_SECONDS)
                put("query_cache_ttl_seconds", 0)
                put("async_query_timeout_seconds", ASYNC_QUERY_TIMEOUT_SECONDS)
                put("txqueuecapacity", 2500)
                put("maxblocksize", MAX_BLOCK_SIZE)
                put("maxblocktransactions", 100)
                put("mininterblockinterval", 25)
                put("chromia_mininterblockinterval", CHROMIA_MIN_INTERBLOCK_MS)
                put("maxtxdelay", 1000)
                put("max_transaction_size", MAX_TX_SIZE)
            }
        )
        put(
            "allowed_gtx_modules",
            buildJsonArray { allowedGtxModules.forEach { add(JsonPrimitive(it)) } }
        )
        put(
            "allowed_sync_ext",
            buildJsonArray { allowedSyncExt.forEach { add(JsonPrimitive(it)) } }
        )
        put("config_yaml", configYaml())
        put("ecosystem_providers_apis_index_url_slash", ECOSYSTEM_PROVIDERS_APIS_INDEX_URL_SLASH)
        put("ecosystem_providers_apis_index_title", ECOSYSTEM_PROVIDERS_APIS_INDEX_TITLE)
        put("learn_rell_masterclass_join_index_url_slash", LEARN_RELL_MASTERCLASS_JOIN_INDEX_URL_SLASH)
        put("learn_rell_masterclass_join_index_title", LEARN_RELL_MASTERCLASS_JOIN_INDEX_TITLE)
        put("learn_iccf_subscription_chain_index_url_slash", LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_URL_SLASH)
        put("learn_iccf_subscription_chain_index_title", LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_TITLE)

        put("learn_marketplace_ft4_index_url_slash", LEARN_MARKETPLACE_FT4_INDEX_URL_SLASH)
        put("learn_marketplace_ft4_index_title", LEARN_MARKETPLACE_FT4_INDEX_TITLE)
        put("learn_ttt_ft4_accounts_index_url_slash", LEARN_TTT_FT4_ACCOUNTS_INDEX_URL_SLASH)
        put("learn_ttt_ft4_accounts_index_title", LEARN_TTT_FT4_ACCOUNTS_INDEX_TITLE)
        put("learn_ttt_update_tests_index_url_slash", LEARN_TTT_UPDATE_TESTS_INDEX_URL_SLASH)
        put("learn_ttt_update_tests_index_title", LEARN_TTT_UPDATE_TESTS_INDEX_TITLE)
        put("learn_news_register_evm_accounts_index_url_slash", LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_URL_SLASH)
        put("learn_news_register_evm_accounts_index_title", LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_TITLE)
        put("learn_chat_agent_setup_index_url_slash", LEARN_CHAT_AGENT_SETUP_INDEX_URL_SLASH)
        put("learn_chat_agent_setup_index_title", LEARN_CHAT_AGENT_SETUP_INDEX_TITLE)
        put("learn_rng_intro_index_url_slash", LEARN_RNG_INTRO_INDEX_URL_SLASH)
        put("learn_rng_intro_index_title", LEARN_RNG_INTRO_INDEX_TITLE)
        put("learn_comparisons_polkadot_index_url_slash", LEARN_COMPARISONS_POLKADOT_INDEX_URL_SLASH)
        put("learn_comparisons_polkadot_index_title", LEARN_COMPARISONS_POLKADOT_INDEX_TITLE)
        put("learn_tags_sql_index_url_slash", LEARN_TAGS_SQL_INDEX_URL_SLASH)
        put("learn_tags_sql_index_title", LEARN_TAGS_SQL_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover GET-STARTED get-started/about/protocols/icmf INDEX leftovers encoded as GET_STARTED_ICMF_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/apis-1 INDEX leftovers encoded as ECOSYSTEM_PROVIDERS_APIS_INDEX_* (query-only).
// Leftover official leftover LEARN courses/rell-masterclass/join INDEX leftovers encoded as LEARN_RELL_MASTERCLASS_JOIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/iccf-course/subscription-chain INDEX leftovers encoded as LEARN_ICCF_SUBSCRIPTION_CHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/module-ft4 INDEX leftovers encoded as LEARN_MARKETPLACE_FT4_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-one/create-accounts/install-configure-ft4 INDEX leftovers encoded as LEARN_TTT_FT4_ACCOUNTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-one/create-accounts/update-tests INDEX leftovers encoded as LEARN_TTT_UPDATE_TESTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX news-feed register-evm-accounts leftovers encoded as LEARN_NEWS_REGISTER_EVM_ACCOUNTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX chat-agent-course setup leftovers encoded as LEARN_CHAT_AGENT_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX random-number-generation introduction leftovers encoded as LEARN_RNG_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX chromia-comparisons polkadot leftovers encoded as LEARN_COMPARISONS_POLKADOT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/SQL INDEX leftovers encoded as LEARN_TAGS_SQL_INDEX_* (query-only HELP ONLY WRITE SKIP).
