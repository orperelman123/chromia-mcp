package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr node start` / `update` flag help.
 * Does not start a node, generate keys, invent a BRID, or send signed txs.
 * Source: docs.chromia.com/build/cli/commands/node (plus StartCommand / UpdateCommand /
 * AbstractNodeCommand on GitLab chromia-cli 0.33.x). Default local API URL is from
 * docs.chromia.com/build/cli/commands/query. Postgres requirement from CLI Functional.md
 * / install docs. Do not invent ports that contradict official CLI.
 */
object ChrNodeHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val NODE_DOCS_URL = "https://docs.chromia.com/build/cli/commands/node"
    const val NODE_INDEX_URL = NODE_DOCS_URL
    const val NODE_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/node/"
    const val NODE_INDEX_TITLE = "node"  // official H1
    const val GET_STARTED_ARCHITECTURE_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture"
    const val GET_STARTED_ARCHITECTURE_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/"
    const val GET_STARTED_ARCHITECTURE_INDEX_TITLE = "Architecture"  // official H1
    const val GET_STARTED_ARCHITECTURE_NODE_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/node"
    const val GET_STARTED_ARCHITECTURE_NODE_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/node/"
    const val GET_STARTED_ARCHITECTURE_NODE_INDEX_TITLE = "Nodes"  // official H1
    const val GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture-summary"
    const val GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture-summary/"
    const val GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_TITLE = "Architecture"  // official H1
    const val ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/overview"
    const val ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/overview/"
    const val ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_TITLE = "Introduction to providers"  // official H1
    const val ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/mass-exit/overview"
    const val ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/mass-exit/overview/"
    const val ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_TITLE = "How mass exit works"  // official H1
    const val ECOSYSTEM_SETUP_PROMETHEUS_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/setup-prometheus"
    const val ECOSYSTEM_SETUP_PROMETHEUS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/setup-prometheus/"
    const val ECOSYSTEM_SETUP_PROMETHEUS_INDEX_TITLE = "Monitor your node with Prometheus and Grafana"  // official H1
    const val ECOSYSTEM_PMC_CONFIG_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/config"
    const val ECOSYSTEM_PMC_CONFIG_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/config/"
    const val ECOSYSTEM_PMC_CONFIG_INDEX_TITLE = "config"  // official H1
    const val ECOSYSTEM_GOV_EXTENSIONS_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/extensions"
    const val ECOSYSTEM_GOV_EXTENSIONS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/extensions/"
    const val ECOSYSTEM_GOV_EXTENSIONS_INDEX_TITLE = "Extensions"  // official H1
    const val ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-requirements"
    const val ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-requirements/"
    const val ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_TITLE = "Vote requirements"  // official H1
    const val ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_URL = "https://docs.chromia.com/ecosystem/block-explorer/features"
    const val ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/block-explorer/features/"
    const val ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_TITLE = "Explorer features"  // official H1
    const val ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/overview"
    const val ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/overview/"
    const val ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_TITLE = "EVM bridge"  // official H1
    const val LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_URL = "https://learn.chromia.com/courses/book-review/build-client"
    const val LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/build-client/"
    const val LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_TITLE = "Lesson 6 - Build the client"  // official H1
    const val LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-review-entity/basic-operations"
    const val LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-review-entity/basic-operations/"
    const val LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_TITLE = "Adding an operation to create a book review"  // official H1
    const val LEARN_RELL_MASTERCLASS_INTRO_INDEX_URL = "https://learn.chromia.com/courses/rell-masterclass/introduction"
    const val LEARN_RELL_MASTERCLASS_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-masterclass/introduction/"
    const val LEARN_RELL_MASTERCLASS_INTRO_INDEX_TITLE = "Rell masterclass"  // official H1
    const val LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_URL = "https://learn.chromia.com/courses/chromia-for-evm-developers/news-feed-follower"
    const val LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-for-evm-developers/news-feed-follower/"
    const val LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_TITLE = "Extend news feed dapp"  // official H1
    const val LEARN_FT4_DEMO_TEST_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/test"
    const val LEARN_FT4_DEMO_TEST_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/test/"
    const val LEARN_FT4_DEMO_TEST_INDEX_TITLE = "Testing the Asset Management System"  // official H1
    const val LEARN_NEWS_VERIFY_INPUTS_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/input-verification/input-verification"
    const val LEARN_NEWS_VERIFY_INPUTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/input-verification/input-verification/"
    const val LEARN_NEWS_VERIFY_INPUTS_INDEX_TITLE = "Verify inputs"  // official H1
    const val LEARN_TTT_WRITE_QUERIES_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/data-modeling/write-queries"
    const val LEARN_TTT_WRITE_QUERIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/data-modeling/write-queries/"
    const val LEARN_TTT_WRITE_QUERIES_INDEX_TITLE = "Write basic queries"  // official H1
    const val LEARN_VECTOR_DB_PREPROCESS_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/preprocess"
    const val LEARN_VECTOR_DB_PREPROCESS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/preprocess/"
    const val LEARN_VECTOR_DB_PREPROCESS_INDEX_TITLE = "Preprocess movie data"  // official H1
    const val LEARN_MONETIZE_INTRO_INDEX_URL = "https://learn.chromia.com/courses/monetize-dapp/introduction"
    const val LEARN_MONETIZE_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/monetize-dapp/introduction/"
    const val LEARN_MONETIZE_INTRO_INDEX_TITLE = "Monetize your dapp"  // official H1
    const val LEARN_ZK_DAPP_VERIFICATION_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-verification"
    const val LEARN_ZK_DAPP_VERIFICATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-verification/"
    const val LEARN_ZK_DAPP_VERIFICATION_INDEX_TITLE = "PLONK verification"  // official H1
    const val LEARN_GOAT_INTRO_INDEX_URL = "https://learn.chromia.com/courses/chromia-goat-chat-agent/introduction"
    const val LEARN_GOAT_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-goat-chat-agent/introduction/"
    const val LEARN_GOAT_INTRO_INDEX_TITLE = "AI chat agent for Chromia transactions"  // official H1
    const val LEARN_WEB3_SCALABILITY_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/scalability"
    const val LEARN_WEB3_SCALABILITY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/scalability/"
    const val LEARN_WEB3_SCALABILITY_INDEX_TITLE = "Scalability"  // official H1
    const val RELL_SPECIAL_OPERATIONS_INDEX_URL = "https://docs.chromia.com/rell/special-operations"
    const val RELL_SPECIAL_OPERATIONS_INDEX_URL_SLASH = "https://docs.chromia.com/rell/special-operations/"
    const val RELL_SPECIAL_OPERATIONS_INDEX_TITLE = "Special operations"  // official H1
    const val QUERY_DOCS_URL = "https://docs.chromia.com/build/cli/commands/query"
    const val INSTALL_POSTGRES_URL =
        "https://docs.chromia.com/get-started/installation#set-up-postgresql-database"
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val DEFAULT_API_URL = "http://localhost:7740"
    const val DEFAULT_JDBC = "jdbc:postgresql://localhost:5432/postchain"
    const val INITIALIZED_LOG = "Node is initialized"
    const val PREEMPTION_DEFAULT = 2

    fun startExample(): String = "chr node start"

    fun startWipeExample(): String = "chr node start --wipe"

    fun startSettingsExample(): String = "chr node start --settings chromia.yml"

    fun startFromBuildExample(name: String = DappScaffold.DEFAULT_NAME): String {
        val chain = DappScaffold.normalizeName(name)
        return "chr node start --blockchain-config build/$chain.xml"
    }

    fun startNamedExample(): String = "chr node start --name foo --name bar"

    fun updateExample(): String = "chr node update"

    fun wipeNote(): String = """
        If a blockchain has already been started on the configured database schema,
        `chr node start` adds the new configuration at the next height (chain continues).
        `--wipe` wipes the database schema on startup so the chain starts from height=0.
        `--no-wipe` is the opposite flag (default is not to wipe).
        Restart after a code change: `chr node start --wipe` when you want a clean height-0 node.
        In-memory ICMF/ICCF replacements used by the test node lose unprocessed messages on restart
        (CLI warning: DO NOT RUN IN PRODUCTION).
    """.trimIndent()

    fun relationNote(): String = """
        `chr node start` compiles Type=BLOCKCHAIN entries from chromia.yml (same ChromiaCompileApi
        path as `chr build`) unless `-bc, --blockchain-config` points at a `chr build` artifact
        (official example: `chr node start --blockchain-config build/my_rell_dapp.xml`).
        Library chains are filtered out of `chr node start` / `chr node update` (CHANGELOG).
        `chr test` is a separate in-process runner (`RellApiRunTests`) against Postgres
        (`--use-db` default, `--no-db` to skip). Tests use schema `<schema>_tests`.
        Typical `chr test` does not start or require `chr node start`.
        Official first-run (run-dapp-cli): `chr create-rell-dapp` → `cd my-rell-dapp` →
        `chr node start` (Postgres required) → `chr query hello_world` → `"Hello World!"`.
        Then `chr test`. Then `chr deployment create` when a lease exists.
    """.trimIndent()

    fun postgresNote(): String = """
        `chr node start` requires a running Postgres 16+ instance (production pin; Java 21+).
        Official local JDBC default: $DEFAULT_JDBC (CLI Functional.md).
        chromia.yml database defaults: host ${ChromiaYmlSections.DEFAULT_HOST},
        database/user ${ChromiaYmlSections.DEFAULT_DB}, schema ${ChromiaYmlSections.DEFAULT_SCHEMA},
        driver ${ChromiaYmlSections.DRIVER}. See $INSTALL_POSTGRES_URL and $PROJECT_CONFIG_URL.
        Env overrides: CHR_DB_URL, CHR_DB_USER, CHR_DB_PASSWORD, CHR_DB_SCHEMA.
        Official 0.33.2 `chr create-rell-dapp` default yml only sets database.schema schema_my_rell_dapp; host/user/password still postchain defaults. `chr node start` REST $DEFAULT_API_URL prints Node is initialized chain-id 0 computed BRID (do not invent hex).
    """.trimIndent()

    fun notes(): String = """
        Chromia CLI $CLI_SERIES local test-node flag help. Java 21+, Postgres 16+.
        Official invoke page: $NODE_DOCS_URL
        Default local REST/API URL (official `chr query` docs): $DEFAULT_API_URL
        Do not invent other ports. Override node properties with `-np, --node-properties=<path>`
        or `-p key=value` (usage: -p key=value).
        Prints "$INITIALIZED_LOG" when all chains are started; stdout also shows each chain name,
        chain-id (iid from config order, starting at 0), and the computed dapp BRID.
        Do not invent a dapp BRID or a Directory Chain BRID. Official Directory BRIDs live on
        write_deployment_config (mainnet ${WriteDeploymentConfig.MAINNET_DIRECTORY_BRID}).
        `chr node update` schedules config at lastHeight + --preemption (default $PREEMPTION_DEFAULT,
        must be > 1). Invoke with the same chromia.yml and args as `chr node start` so chain ids match.
        `--directory-chain-mock` adds a chain on ID 0 that answers cluster-management / anchoring
        (ICCF, FT4 cross-chain, frontend node discovery). Required for `chr tx` ICCF against a local node.
        This tool does not start a node, does not generate a key, and does not send signed transactions.
        Official BUILD cli/commands/node ($NODE_INDEX_URL 307 $NODE_INDEX_URL_SLASH 200 $NODE_INDEX_TITLE): intro Usage chr node [OPTIONS] COMMAND [ARGS]... Interact with a test node Commands start update node start Usage chr node start [<options>] Configuration Properties -s, --settings Options -bc, --blockchain-config --name -p -np, --node-properties --directory-chain-mock --hide-lib-warnings --sql-log --wipe / --no-wipe examples chr node start --settings chromia.yml --blockchain-config build/my_rell_dapp.xml --name foo --name bar node update Usage chr node update [<options>] -n, --preemption Query-only WRITE SKIP HELP ONLY skip signed txs no sample keys no invented 64-hex no keygen do not invent flags do not document chr tx signed send keygen samples.
        Official GET-STARTED get-started/about/architecture INDEX ($GET_STARTED_ARCHITECTURE_INDEX_URL 307 $GET_STARTED_ARCHITECTURE_INDEX_URL_SLASH 200 $GET_STARTED_ARCHITECTURE_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex keygen do not invent flags BRIDs do not document chr tx signed send keygen samples.
        Official GET-STARTED get-started/about/architecture/node INDEX ($GET_STARTED_ARCHITECTURE_NODE_INDEX_URL 307 $GET_STARTED_ARCHITECTURE_NODE_INDEX_URL_SLASH 200 $GET_STARTED_ARCHITECTURE_NODE_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe do not invent flags do not document chr tx signed send keygen samples.
        Official GET-STARTED get-started/about/architecture-summary INDEX ($GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_URL 307 $GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_URL_SLASH 200 $GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe do not invent flags do not document chr tx signed send keygen samples.
        Official ECOSYSTEM ecosystem/providers/overview INDEX ($ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_URL 307 $ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/bridge/mass-exit/overview INDEX ($ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_URL 307 $ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/nodes/setup-prometheus INDEX ($ECOSYSTEM_SETUP_PROMETHEUS_INDEX_URL 307 $ECOSYSTEM_SETUP_PROMETHEUS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_SETUP_PROMETHEUS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/config INDEX ($ECOSYSTEM_PMC_CONFIG_INDEX_URL 307 $ECOSYSTEM_PMC_CONFIG_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_CONFIG_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/getting-started/extensions INDEX ($ECOSYSTEM_GOV_EXTENSIONS_INDEX_URL 307 $ECOSYSTEM_GOV_EXTENSIONS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_EXTENSIONS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure/vote-requirements INDEX ($ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_URL 307 $ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official ECOSYSTEM ecosystem/block-explorer/features INDEX ($ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_URL 307 $ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official ECOSYSTEM ecosystem/bridge/overview INDEX ($ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_URL 307 $ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/book-review/build-client INDEX ($LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_URL 301 $LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/book-review/book-review-entity/basic-operations INDEX ($LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_URL 301 $LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/rell-masterclass/introduction INDEX ($LEARN_RELL_MASTERCLASS_INTRO_INDEX_URL 301 $LEARN_RELL_MASTERCLASS_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_RELL_MASTERCLASS_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/chromia-for-evm-developers/news-feed-follower INDEX ($LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_URL 301 $LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_URL_SLASH 200 H1 $LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/ft4-demo-app/module-blockchain/test INDEX ($LEARN_FT4_DEMO_TEST_INDEX_URL 301 $LEARN_FT4_DEMO_TEST_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_TEST_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/my-news-feed/module-one/input-verification/input-verification INDEX ($LEARN_NEWS_VERIFY_INPUTS_INDEX_URL 301 $LEARN_NEWS_VERIFY_INPUTS_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_VERIFY_INPUTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/tic-tac-toe/module-one/data-modeling/write-queries INDEX ($LEARN_TTT_WRITE_QUERIES_INDEX_URL 301 $LEARN_TTT_WRITE_QUERIES_INDEX_URL_SLASH 200 H1 $LEARN_TTT_WRITE_QUERIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/vector-db-movie-demo/data-pipeline/preprocess INDEX ($LEARN_VECTOR_DB_PREPROCESS_INDEX_URL 301 $LEARN_VECTOR_DB_PREPROCESS_INDEX_URL_SLASH 200 H1 $LEARN_VECTOR_DB_PREPROCESS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/monetize-dapp/introduction INDEX ($LEARN_MONETIZE_INTRO_INDEX_URL 301 $LEARN_MONETIZE_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_MONETIZE_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof dapp-verification ($LEARN_ZK_DAPP_VERIFICATION_INDEX_URL 301 $LEARN_ZK_DAPP_VERIFICATION_INDEX_URL_SLASH GET 200 H1 $LEARN_ZK_DAPP_VERIFICATION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX chromia-goat-chat-agent introduction ($LEARN_GOAT_INTRO_INDEX_URL 301 $LEARN_GOAT_INTRO_INDEX_URL_SLASH GET 200 H1 $LEARN_GOAT_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX web3-for-web2-devs scalability ($LEARN_WEB3_SCALABILITY_INDEX_URL 301 $LEARN_WEB3_SCALABILITY_INDEX_URL_SLASH GET 200 H1 $LEARN_WEB3_SCALABILITY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/special-operations INDEX ($RELL_SPECIAL_OPERATIONS_INDEX_URL 307 $RELL_SPECIAL_OPERATIONS_INDEX_URL_SLASH 200 H1 $RELL_SPECIAL_OPERATIONS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", NODE_DOCS_URL)
        put("node_index_docs", NODE_INDEX_URL)
        put("node_index_url_slash", NODE_INDEX_URL_SLASH)
        put("node_index_title", NODE_INDEX_TITLE)
        put("get_started_architecture_index_docs", GET_STARTED_ARCHITECTURE_INDEX_URL)
        put("get_started_architecture_index_url_slash", GET_STARTED_ARCHITECTURE_INDEX_URL_SLASH)
        put("get_started_architecture_index_title", GET_STARTED_ARCHITECTURE_INDEX_TITLE)
        put("get_started_architecture_node_index_docs", GET_STARTED_ARCHITECTURE_NODE_INDEX_URL)
        put("get_started_architecture_node_index_url_slash", GET_STARTED_ARCHITECTURE_NODE_INDEX_URL_SLASH)
        put("get_started_architecture_node_index_title", GET_STARTED_ARCHITECTURE_NODE_INDEX_TITLE)
        put("get_started_architecture_summary_index_docs", GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_URL)
        put("get_started_architecture_summary_index_url_slash", GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_URL_SLASH)
        put("get_started_architecture_summary_index_title", GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_TITLE)
        put("default_api_url", DEFAULT_API_URL)
        put("default_jdbc", DEFAULT_JDBC)
        put("initialized_log", INITIALIZED_LOG)
        put(
            "commands",
            buildJsonObject {
                put("start", startExample())
                put("start_wipe", startWipeExample())
                put("start_settings", startSettingsExample())
                put("start_from_build", startFromBuildExample())
                put("start_named", startNamedExample())
                put("update", updateExample())
            }
        )
        put(
            "flags",
            buildJsonObject {
                put(
                    "start",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("blockchain_config", "-bc, --blockchain-config=<path>  # e.g. build/hello.xml")
                        put("name", "--name=<name>  # repeatable; only start specified blockchains")
                        put("property", "-p=<key=value>  # override any property value")
                        put("node_properties", "-np, --node-properties=<path>")
                        put(
                            "directory_chain_mock",
                            "--directory-chain-mock  # chain 0 mock for ICCF / FT4 x-chain / frontend"
                        )
                        put("hide_lib_warnings", "--hide-lib-warnings")
                        put("sql_log", "--sql-log")
                        put("wipe", "--wipe / --no-wipe  # wipe schema → start at height 0")
                    }
                )
                put(
                    "update",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("blockchain_config", "-bc, --blockchain-config=<path>")
                        put("name", "--name=<name>  # repeatable")
                        put("property", "-p=<key=value>")
                        put("node_properties", "-np, --node-properties=<path>")
                        put("directory_chain_mock", "--directory-chain-mock")
                        put("hide_lib_warnings", "--hide-lib-warnings")
                        put(
                            "preemption",
                            "-n, --preemption=<int>  # apply this many blocks ahead (default $PREEMPTION_DEFAULT, must be > 1)"
                        )
                    }
                )
            }
        )
        put("wipe", wipeNote())
        put("relation", relationNote())
        put("postgres_note", postgresNote())
        put("database", ChromiaYmlSections.databaseYaml())
        put("ecosystem_providers_overview_index_url_slash", ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_URL_SLASH)
        put("ecosystem_providers_overview_index_title", ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_TITLE)
        put("ecosystem_mass_exit_overview_index_url_slash", ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_URL_SLASH)
        put("ecosystem_mass_exit_overview_index_title", ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_TITLE)
        put("ecosystem_setup_prometheus_index_url_slash", ECOSYSTEM_SETUP_PROMETHEUS_INDEX_URL_SLASH)
        put("ecosystem_setup_prometheus_index_title", ECOSYSTEM_SETUP_PROMETHEUS_INDEX_TITLE)
        put("ecosystem_pmc_config_index_url_slash", ECOSYSTEM_PMC_CONFIG_INDEX_URL_SLASH)
        put("ecosystem_pmc_config_index_title", ECOSYSTEM_PMC_CONFIG_INDEX_TITLE)
        put("ecosystem_gov_extensions_index_url_slash", ECOSYSTEM_GOV_EXTENSIONS_INDEX_URL_SLASH)
        put("ecosystem_gov_extensions_index_title", ECOSYSTEM_GOV_EXTENSIONS_INDEX_TITLE)
        put("ecosystem_gov_vote_requirements_index_url_slash", ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_URL_SLASH)
        put("ecosystem_gov_vote_requirements_index_title", ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_TITLE)
        put("ecosystem_block_explorer_features_index_url_slash", ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_URL_SLASH)
        put("ecosystem_block_explorer_features_index_title", ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_TITLE)
        put("ecosystem_bridge_overview_index_url_slash", ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_URL_SLASH)
        put("ecosystem_bridge_overview_index_title", ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_TITLE)
        put("learn_book_review_build_client_index_url_slash", LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_URL_SLASH)
        put("learn_book_review_build_client_index_title", LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_TITLE)
        put("learn_book_review_add_review_op_index_url_slash", LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_URL_SLASH)
        put("learn_book_review_add_review_op_index_title", LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_TITLE)
        put("learn_rell_masterclass_intro_index_url_slash", LEARN_RELL_MASTERCLASS_INTRO_INDEX_URL_SLASH)
        put("learn_rell_masterclass_intro_index_title", LEARN_RELL_MASTERCLASS_INTRO_INDEX_TITLE)
        put("learn_evm_news_feed_follower_index_url_slash", LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_URL_SLASH)
        put("learn_evm_news_feed_follower_index_title", LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_TITLE)
        put("learn_ft4_demo_test_index_url_slash", LEARN_FT4_DEMO_TEST_INDEX_URL_SLASH)
        put("learn_ft4_demo_test_index_title", LEARN_FT4_DEMO_TEST_INDEX_TITLE)
        put("learn_news_verify_inputs_index_url_slash", LEARN_NEWS_VERIFY_INPUTS_INDEX_URL_SLASH)
        put("learn_news_verify_inputs_index_title", LEARN_NEWS_VERIFY_INPUTS_INDEX_TITLE)
        put("learn_ttt_write_queries_index_url_slash", LEARN_TTT_WRITE_QUERIES_INDEX_URL_SLASH)
        put("learn_ttt_write_queries_index_title", LEARN_TTT_WRITE_QUERIES_INDEX_TITLE)
        put("learn_vector_db_preprocess_index_url_slash", LEARN_VECTOR_DB_PREPROCESS_INDEX_URL_SLASH)
        put("learn_vector_db_preprocess_index_title", LEARN_VECTOR_DB_PREPROCESS_INDEX_TITLE)
        put("learn_monetize_intro_index_url_slash", LEARN_MONETIZE_INTRO_INDEX_URL_SLASH)
        put("learn_monetize_intro_index_title", LEARN_MONETIZE_INTRO_INDEX_TITLE)
        put("learn_zk_dapp_verification_index_url_slash", LEARN_ZK_DAPP_VERIFICATION_INDEX_URL_SLASH)
        put("learn_zk_dapp_verification_index_title", LEARN_ZK_DAPP_VERIFICATION_INDEX_TITLE)
        put("learn_goat_intro_index_url_slash", LEARN_GOAT_INTRO_INDEX_URL_SLASH)
        put("learn_goat_intro_index_title", LEARN_GOAT_INTRO_INDEX_TITLE)
        put("learn_web3_scalability_index_url_slash", LEARN_WEB3_SCALABILITY_INDEX_URL_SLASH)
        put("learn_web3_scalability_index_title", LEARN_WEB3_SCALABILITY_INDEX_TITLE)
        put("rell_special_operations_index_url_slash", RELL_SPECIAL_OPERATIONS_INDEX_URL_SLASH)
        put("rell_special_operations_index_title", RELL_SPECIAL_OPERATIONS_INDEX_TITLE)
        put("notes", notes())
    }
}

// Official BUILD cli/commands/node leftovers encoded as NODE_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture INDEX leftovers encoded as GET_STARTED_ARCHITECTURE_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture/node INDEX leftovers encoded as GET_STARTED_ARCHITECTURE_NODE_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture-summary INDEX leftovers encoded as GET_STARTED_ARCHITECTURE_SUMMARY_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/overview INDEX leftovers encoded as ECOSYSTEM_PROVIDERS_OVERVIEW_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/bridge/mass-exit/overview INDEX leftovers encoded as ECOSYSTEM_MASS_EXIT_OVERVIEW_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/nodes/setup-prometheus INDEX leftovers encoded as ECOSYSTEM_SETUP_PROMETHEUS_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/config INDEX leftovers encoded as ECOSYSTEM_PMC_CONFIG_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/getting-started/extensions INDEX leftovers encoded as ECOSYSTEM_GOV_EXTENSIONS_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure/vote-requirements INDEX leftovers encoded as ECOSYSTEM_GOV_VOTE_REQUIREMENTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official ECOSYSTEM ecosystem/block-explorer/features INDEX leftovers encoded as ECOSYSTEM_BLOCK_EXPLORER_FEATURES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official ECOSYSTEM ecosystem/bridge/overview INDEX leftovers encoded as ECOSYSTEM_BRIDGE_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/build-client INDEX leftovers encoded as LEARN_BOOK_REVIEW_BUILD_CLIENT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/book-review-entity/basic-operations INDEX leftovers encoded as LEARN_BOOK_REVIEW_ADD_REVIEW_OP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/rell-masterclass/introduction INDEX leftovers encoded as LEARN_RELL_MASTERCLASS_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/chromia-for-evm-developers/news-feed-follower INDEX leftovers encoded as LEARN_EVM_NEWS_FEED_FOLLOWER_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-blockchain/test INDEX leftovers encoded as LEARN_FT4_DEMO_TEST_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/input-verification/input-verification INDEX leftovers encoded as LEARN_NEWS_VERIFY_INPUTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-one/data-modeling/write-queries INDEX leftovers encoded as LEARN_TTT_WRITE_QUERIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/vector-db-movie-demo/data-pipeline/preprocess INDEX leftovers encoded as LEARN_VECTOR_DB_PREPROCESS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/monetize-dapp/introduction INDEX leftovers encoded as LEARN_MONETIZE_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof dapp-verification leftovers encoded as LEARN_ZK_DAPP_VERIFICATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX chromia-goat-chat-agent introduction leftovers encoded as LEARN_GOAT_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX web3-for-web2-devs scalability leftovers encoded as LEARN_WEB3_SCALABILITY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/special-operations INDEX leftovers encoded as RELL_SPECIAL_OPERATIONS_INDEX_* (query-only HELP ONLY WRITE SKIP).
