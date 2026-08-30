package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x install / build / test / code commands.
 * Does not shell out to chr. Source: docs.chromia.com/get-started/installation
 * and gitlab.com/chromaway/core-tools/chromia-cli (tags through 0.33.x).
 */
object ChrBuildHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val INTRO_URL = "https://docs.chromia.com/build/cli/introduction"
    const val RELEASE_NOTES_URL = "https://docs.chromia.com/build/cli/cli-release-notes"
    const val BUILD_URL = "https://docs.chromia.com/build/cli/commands/build"
    const val BUILD_INDEX_URL = BUILD_URL
    const val BUILD_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/build/"
    const val BUILD_INDEX_TITLE = "build"  // official H1
    const val CODE_URL = "https://docs.chromia.com/build/cli/commands/code"
    const val CODE_INDEX_URL = CODE_URL
    const val CODE_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/code/"
    const val CODE_INDEX_TITLE = "code"  // official H1
    const val GET_STARTED_BENEFITS_INDEX_URL = "https://docs.chromia.com/get-started/about/benefits"
    const val GET_STARTED_BENEFITS_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/benefits/"
    const val GET_STARTED_BENEFITS_INDEX_TITLE = "Benefits"  // official H1
    const val GET_STARTED_EXTENSIONS_INDEX_URL = "https://docs.chromia.com/get-started/about/extensions"
    const val GET_STARTED_EXTENSIONS_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/extensions/"
    const val GET_STARTED_EXTENSIONS_INDEX_TITLE = "What are Chromia extensions?"  // official H1
    const val GET_STARTED_USE_CASES_INDEX_URL = "https://docs.chromia.com/get-started/use-cases"
    const val GET_STARTED_USE_CASES_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/"
    const val GET_STARTED_USE_CASES_INDEX_TITLE = "Use Cases"  // official H1
    const val GET_STARTED_STAKING_SUMMARY_INDEX_URL = "https://docs.chromia.com/get-started/about/staking-summary"
    const val GET_STARTED_STAKING_SUMMARY_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/staking-summary/"
    const val GET_STARTED_STAKING_SUMMARY_INDEX_TITLE = "Staking"  // official H1
    const val GET_STARTED_INSTALLATION_INDEX_URL = "https://docs.chromia.com/get-started/installation"
    const val GET_STARTED_INSTALLATION_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/installation/"
    const val GET_STARTED_INSTALLATION_INDEX_TITLE = "Get started with Chromia"  // official H1
    const val ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_URL = "https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filehub"
    const val ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filehub/"
    const val ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_TITLE = "Deploy Filehub"  // official H1
    const val ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/mass-exit/operations"
    const val ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/mass-exit/operations/"
    const val ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_TITLE = "Mass exit operations"  // official H1
    const val ECOSYSTEM_SETUP_TLS_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/setup-tls"
    const val ECOSYSTEM_SETUP_TLS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/setup-tls/"
    const val ECOSYSTEM_SETUP_TLS_INDEX_TITLE = "Set up a reverse proxy for TLS"  // official H1
    const val ECOSYSTEM_PMC_CONTAINER_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/container"
    const val ECOSYSTEM_PMC_CONTAINER_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/container/"
    const val ECOSYSTEM_PMC_CONTAINER_INDEX_TITLE = "container"  // official H1
    const val ECOSYSTEM_GOV_DELEGATES_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/extensions/delegates"
    const val ECOSYSTEM_GOV_DELEGATES_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/extensions/delegates/"
    const val ECOSYSTEM_GOV_DELEGATES_INDEX_TITLE = "Governance Tool Delegates extension"  // official H1
    const val ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-weight-strategy"
    const val ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-weight-strategy/"
    const val ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_TITLE = "Vote weight strategies"  // official H1
    const val ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_URL = "https://docs.chromia.com/ecosystem/block-explorer/overview"
    const val ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/block-explorer/overview/"
    const val ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_TITLE = "About Chromia Block Explorer"  // official H1
    const val ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_URL = "https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-configure"
    const val ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-configure/"
    const val ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_TITLE = "Configure Filehub"  // official H1
    const val LEARN_BIG_DATA_INTRO_INDEX_URL = "https://learn.chromia.com/courses/big-data/introduction"
    const val LEARN_BIG_DATA_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/big-data/introduction/"
    const val LEARN_BIG_DATA_INTRO_INDEX_TITLE = "Big data analysis with Chromia blockchain and PySpark"  // official H1
    const val LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-review-entity"
    const val LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-review-entity/"
    const val LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_TITLE = "Lesson 2 - Create a related entity"  // official H1
    const val LEARN_EVM_DEVELOPERS_INTRO_INDEX_URL = "https://learn.chromia.com/courses/chromia-for-evm-developers/introduction"
    const val LEARN_EVM_DEVELOPERS_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-for-evm-developers/introduction/"
    const val LEARN_EVM_DEVELOPERS_INTRO_INDEX_TITLE = "Chromia for EVM developers"  // official H1
    const val LEARN_EVM_NEWS_FEED_INDEX_URL = "https://learn.chromia.com/courses/chromia-for-evm-developers/news-feed"
    const val LEARN_EVM_NEWS_FEED_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-for-evm-developers/news-feed/"
    const val LEARN_EVM_NEWS_FEED_INDEX_TITLE = "Build a news feed dapp"  // official H1
    const val LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/setup"
    const val LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/setup/"
    const val LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_TITLE = "Lesson 1 - Configure the Blockchain dapp"  // official H1
    const val LEARN_NEWS_BASIC_OPS_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries/basic-operations"
    const val LEARN_NEWS_BASIC_OPS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries/basic-operations/"
    const val LEARN_NEWS_BASIC_OPS_INDEX_TITLE = "Basic operations"  // official H1
    const val LEARN_TTT_DATA_TABLES_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/data-modeling/tables"
    const val LEARN_TTT_DATA_TABLES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/data-modeling/tables/"
    const val LEARN_TTT_DATA_TABLES_INDEX_TITLE = "Design the data model"  // official H1
    const val LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/upload-vectors"
    const val LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/upload-vectors/"
    const val LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_TITLE = "Upload vectors to the blockchain"  // official H1
    const val LEARN_MONETIZE_INTRO_INDEX_URL = "https://learn.chromia.com/courses/monetize-dapp/introduction"
    const val LEARN_MONETIZE_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/monetize-dapp/introduction/"
    const val LEARN_MONETIZE_INTRO_INDEX_TITLE = "Monetize your dapp"  // official H1
    const val LEARN_ZK_DAPP_SETUP_RUN_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-setup-run"
    const val LEARN_ZK_DAPP_SETUP_RUN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-setup-run/"
    const val LEARN_ZK_DAPP_SETUP_RUN_INDEX_TITLE = "Dapp: setup and run"  // official H1
    const val LEARN_CHAT_AGENT_EXPLORE_INDEX_URL = "https://learn.chromia.com/courses/chat-agent-course/explore-and-extend"
    const val LEARN_CHAT_AGENT_EXPLORE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chat-agent-course/explore-and-extend/"
    const val LEARN_CHAT_AGENT_EXPLORE_INDEX_TITLE = "Explore and extend"  // official H1
    const val LEARN_WEB3_COMPARE_FRONTEND_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/compare-frontend"
    const val LEARN_WEB3_COMPARE_FRONTEND_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/compare-frontend/"
    const val LEARN_WEB3_COMPARE_FRONTEND_INDEX_TITLE = "Comparing frontends"  // official H1
    const val RELL_EXPRESSIONS_JUMP_INDEX_URL = "https://docs.chromia.com/rell/language-features/expressions/jump-expressions"
    const val RELL_EXPRESSIONS_JUMP_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/expressions/jump-expressions/"
    const val RELL_EXPRESSIONS_JUMP_INDEX_TITLE = "Jump expressions"  // official H1
    const val RELL_RELEASES_INDEX_URL = "https://docs.chromia.com/rell/releases"
    const val RELL_RELEASES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/releases/"
    const val RELL_RELEASES_INDEX_TITLE = "Rell releases"  // official H1
    const val DOCS_ROOT_INDEX_URL = "https://docs.chromia.com"
    const val DOCS_ROOT_INDEX_URL_SLASH = "https://docs.chromia.com/"
    const val DOCS_ROOT_INDEX_TITLE = "Chromia Docs"  // official H1
    const val LEARN_HOME_INDEX_URL = "https://learn.chromia.com"
    const val LEARN_HOME_INDEX_URL_SLASH = "https://learn.chromia.com/"
    const val LEARN_HOME_INDEX_TITLE = "Learn Chromia"  // official H1
    const val DOCS_LATEST_CLI = "0.30.0"
    const val DOCS_LATEST_CLI_DATE = "2026-02-27"
    const val DOCKER_IMAGE = "registry.gitlab.com/chromaway/core-tools/chromia-cli/chr:latest"
    const val HOMEBREW_TAP = "brew tap chromia/core https://gitlab.com/chromaway/core-tools/homebrew-chromia.git"
    const val HOMEBREW_INSTALL = "brew install chromia/core/chr"
    const val APT_KEY = "curl -fsSL https://apt.chromia.com/chromia.gpg | sudo tee /usr/share/keyrings/chromia.gpg >/dev/null"
    const val APT_REPO = "echo \"deb [arch=amd64 signed-by=/usr/share/keyrings/chromia.gpg] https://apt.chromia.com stable main\" | sudo tee /etc/apt/sources.list.d/chromia.list"
    const val APT_INSTALL = "sudo apt-get update && sudo apt-get install chr"
    const val SCOOP_BUCKET = "scoop bucket add chromia https://gitlab.com/chromaway/core-tools/scoop-chromia.git"
    const val SCOOP_INSTALL = "scoop install chr"
    const val DOCKER_RUN = "docker run --rm -v \"$(pwd):$(pwd)\" -w \"$(pwd)\" $DOCKER_IMAGE chr"

    fun expectedYmlShape(name: String = DappScaffold.DEFAULT_NAME): String = """
        blockchains:
          $name:
            module: main
            config:
              features:
                merkle_hash_version: ${DappScaffold.MERKLE_HASH_VERSION}

        compile:
          rellVersion: ${DappScaffold.RELL_VERSION}

        libs:
          ft4:
            registry: ${DappScaffold.FT4_REGISTRY}
            path: ${DappScaffold.FT4_PATH}
            tagOrBranch: ${DappScaffold.FT4_VERSION}
            rid: ${DappScaffold.FT4_RID}
            insecure: false
    """.trimIndent() + "\n"

    fun notes(): String = """
        Chromia CLI $CLI_SERIES (GitLab tags through 0.33.2). Java 21+, Postgres 16+.
        Official leftover intro (200): $INTRO_URL
        Official leftover release notes (200): $RELEASE_NOTES_URL
        Official leftover docs-site latest listed CLI $DOCS_LATEST_CLI ($DOCS_LATEST_CLI_DATE); source tags $CLI_SERIES — state both. Do not invent flags from 0.30.x docs as if they were $CLI_SERIES-only.
        Official leftover intro first-run: `chr create-rell-dapp` → `chr node start` → `chr query hello_world` (no extra options when a single local chain) → `chr test`.
        Leftover official leftover 0.33.2 create default compile.rellVersion is 0.14.5 (leftover project-config example 0.14.9 is stale; leftover pin ${DappScaffold.RELL_VERSION}). Leftover `chr build` is leftover optional compile; leftover `chr node start` compiles Type=BLOCKCHAIN.
        Official leftover 0.20.14: blockchain names cannot contain hyphens.
        Official leftover 0.21.0: Java 21 required.
        Official leftover 0.25.0: merkle hash calculator v2 (production pin merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION}).
        Official leftover 0.30.0 also bumped docs-site Rell to 0.15.2 — stale vs source pin ${DappScaffold.RELL_VERSION}.
        Official install: https://docs.chromia.com/get-started/installation
        Verify: `chr --version` (docs) or `chr version` (prints CLI + rell + postchain + EIF + Java).
        `chr install` is an alias for `chr library install` (installs libs from chromia.yml).
        `chr build` builds the dapp and writes blockchain configuration (there is no top-level `chr compile` in 0.33.x).
        Official `chr build` flags (docs.chromia.com/build/cli/commands/build): -s/--settings, -bc/--blockchain,
        -f/--format=(GTV|XML), --hide-lib-warnings, --skip-lib-check (since 0.30.0).
        Leftover official leftover BUILD cli/commands/build (leftover official $BUILD_INDEX_URL leftover official 307 leftover official $BUILD_INDEX_URL_SLASH leftover official 200 leftover official $BUILD_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr build [<options>] leftover official leftover Build the dapp and write blockchain configuration leftover official leftover Options leftover official leftover -s, --settings leftover official leftover -bc, --blockchain leftover official leftover -f, --format=(GTV|XML) leftover official leftover --hide-lib-warnings leftover official leftover --skip-lib-check leftover official leftover -h, --help leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official leftover BUILD cli/commands/code (leftover official $CODE_INDEX_URL leftover official 307 leftover official $CODE_INDEX_URL_SLASH leftover official 200 leftover official $CODE_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr code leftover official leftover verbs leftover official leftover lint leftover official leftover format leftover official leftover check leftover official leftover chr code check leftover official leftover --hide-lib-warnings leftover official leftover chr code lint leftover official leftover .rell_lint leftover official leftover --fix leftover official leftover chr code format leftover official leftover .rell_format leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover verbs leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official leftover GET-STARTED get-started/about/benefits INDEX (leftover official $GET_STARTED_BENEFITS_INDEX_URL leftover official 307 leftover official $GET_STARTED_BENEFITS_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_BENEFITS_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only.
        Leftover official leftover GET-STARTED get-started/about/extensions INDEX (leftover official $GET_STARTED_EXTENSIONS_INDEX_URL leftover official 307 leftover official $GET_STARTED_EXTENSIONS_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_EXTENSIONS_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only.
        Leftover official leftover GET-STARTED get-started/use-cases INDEX (leftover official $GET_STARTED_USE_CASES_INDEX_URL leftover official 307 leftover official $GET_STARTED_USE_CASES_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_USE_CASES_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only.
        Leftover official leftover GET-STARTED get-started/about/staking-summary INDEX (leftover official $GET_STARTED_STAKING_SUMMARY_INDEX_URL leftover official 307 leftover official $GET_STARTED_STAKING_SUMMARY_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_STAKING_SUMMARY_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only.
        Leftover official leftover GET-STARTED get-started/installation INDEX (leftover official $GET_STARTED_INSTALLATION_INDEX_URL leftover official 307 leftover official $GET_STARTED_INSTALLATION_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_INSTALLATION_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover ECOSYSTEM ecosystem/filehub/filehub-setup/deploy-filehub INDEX (leftover official $ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/bridge/mass-exit/operations INDEX (leftover official $ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP. Leftover official leftover intro leftover official leftover trigger leftover official leftover mass leftover official leftover exit leftover official leftover process leftover official leftover enable leftover official leftover users leftover official leftover withdraw leftover official leftover funds leftover official leftover account leftover official leftover state leftover official leftover snapshots leftover official leftover standard leftover official leftover deposits leftover official leftover and leftover official leftover withdrawals leftover official leftover immediately leftover official leftover blocked leftover official leftover bridge leftover official leftover enters leftover official leftover mass leftover official leftover exit leftover official leftover mode leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover writes leftover official leftover leftover official leftover deposit leftover official leftover leftover official leftover withdraw leftover official leftover leftover official leftover mass-exit leftover official leftover leftover official leftover register leftover official leftover leftover official leftover triggerMassExit leftover official leftover leftover official leftover procedure leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/setup-tls INDEX (leftover official $ECOSYSTEM_SETUP_TLS_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_SETUP_TLS_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_SETUP_TLS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/container INDEX (leftover official $ECOSYSTEM_PMC_CONTAINER_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_CONTAINER_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_CONTAINER_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/extensions/delegates INDEX (leftover official $ECOSYSTEM_GOV_DELEGATES_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_DELEGATES_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_GOV_DELEGATES_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-structure/vote-weight-strategy INDEX (leftover official $ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover ECOSYSTEM ecosystem/block-explorer/overview INDEX (leftover official $ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover ECOSYSTEM ecosystem/filehub/configure-filehub/filehub-configure INDEX (leftover official $ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/big-data/introduction INDEX (leftover official $LEARN_BIG_DATA_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_BIG_DATA_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BIG_DATA_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/book-review/book-review-entity INDEX (leftover official $LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/chromia-for-evm-developers/introduction INDEX (leftover official $LEARN_EVM_DEVELOPERS_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_EVM_DEVELOPERS_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_EVM_DEVELOPERS_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/chromia-for-evm-developers/news-feed INDEX (leftover official $LEARN_EVM_NEWS_FEED_INDEX_URL leftover official 301 leftover official $LEARN_EVM_NEWS_FEED_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_EVM_NEWS_FEED_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/ft4-demo-app/module-blockchain/setup INDEX (leftover official $LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/my-news-feed/module-one/operations-queries/basic-operations INDEX (leftover official $LEARN_NEWS_BASIC_OPS_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_BASIC_OPS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_BASIC_OPS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/tic-tac-toe/module-one/data-modeling/tables INDEX (leftover official $LEARN_TTT_DATA_TABLES_INDEX_URL leftover official 301 leftover official $LEARN_TTT_DATA_TABLES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_DATA_TABLES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/vector-db-movie-demo/data-pipeline/upload-vectors INDEX (leftover official $LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_URL leftover official 301 leftover official $LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/monetize-dapp/introduction INDEX (leftover official $LEARN_MONETIZE_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_MONETIZE_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MONETIZE_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX zero-knowledge-proof dapp-setup-run (leftover official $LEARN_ZK_DAPP_SETUP_RUN_INDEX_URL leftover official 301 leftover official $LEARN_ZK_DAPP_SETUP_RUN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ZK_DAPP_SETUP_RUN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX chat-agent-course explore-and-extend (leftover official $LEARN_CHAT_AGENT_EXPLORE_INDEX_URL leftover official 301 leftover official $LEARN_CHAT_AGENT_EXPLORE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_CHAT_AGENT_EXPLORE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Default compile.source is src; compile.target is build. Official compile keys also include deprecatedError,
        quiet, and strictGtvConversion (default true; leave true). project-config example rellVersion 0.14.9 is stale.
        Official `chr code` verbs (docs.chromia.com/build/cli/commands/code): lint, format, check.
        `chr code check` checks Rell for compilation errors (`--hide-lib-warnings` is official).
        `chr code lint` uses `.rell_lint` (`--fix` auto-fixes). `chr code format` uses `.rell_format`.
        `chr test` runs tests in the working directory (official flags: --blockchain, --modules, --file, --tests,
        --use-db / --no-db, --test-report, --fail-on-error, --hide-lib-warnings).
        CLI 0.31.0 removed `chr test --sql-log`; use `chr repl --sql-log` (see chr_repl_help).
        `chr build --skip-lib-check` exists since 0.30.0.
        Since 0.30.0, `chr deployment create` writes deployments.<net>.chains into chromia.yml.
        Pin compile.rellVersion ${DappScaffold.RELL_VERSION} and merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION}.
        This tool does not run chr and does not send signed transactions.
        Leftover official leftover RELL rell/language-features/expressions/jump-expressions INDEX (leftover official $RELL_EXPRESSIONS_JUMP_INDEX_URL leftover official 307 leftover official $RELL_EXPRESSIONS_JUMP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_EXPRESSIONS_JUMP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX web3-for-web2-devs compare-frontend (leftover official $LEARN_WEB3_COMPARE_FRONTEND_INDEX_URL leftover official 301 leftover official $LEARN_WEB3_COMPARE_FRONTEND_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_WEB3_COMPARE_FRONTEND_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/releases INDEX (leftover official $RELL_RELEASES_INDEX_URL leftover official 307 leftover official $RELL_RELEASES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_RELEASES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover DOCS docs.chromia.com root INDEX (leftover official $DOCS_ROOT_INDEX_URL leftover official 200 leftover official $DOCS_ROOT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $DOCS_ROOT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN learn.chromia.com home INDEX (leftover official $LEARN_HOME_INDEX_URL leftover official 200 leftover official $LEARN_HOME_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_HOME_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP. leftover official leftover empty CSR H1.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("intro_docs", INTRO_URL)
        put("release_notes_docs", RELEASE_NOTES_URL)
        put("build_docs", BUILD_URL)
        put("build_index_docs", BUILD_INDEX_URL)
        put("build_index_url_slash", BUILD_INDEX_URL_SLASH)
        put("build_index_title", BUILD_INDEX_TITLE)
        put("docs_latest_cli", DOCS_LATEST_CLI)
        put("docs_latest_cli_date", DOCS_LATEST_CLI_DATE)
        put("source_cli", CLI_SERIES)
        put(
            "install",
            buildJsonObject {
                put(
                    "macos",
                    "$HOMEBREW_TAP && $HOMEBREW_INSTALL && chr --version"
                )
                put(
                    "linux",
                    "$APT_KEY && $APT_REPO && $APT_INSTALL && chr --version"
                )
                put(
                    "windows",
                    "$SCOOP_BUCKET && scoop bucket add java && $SCOOP_INSTALL && chr --version"
                )
                put("docker", "$DOCKER_RUN")
                put("dockerImage", DOCKER_IMAGE)
            }
        )
        put(
            "commands",
            buildJsonObject {
                put("version", "chr --version")
                put("versionLong", "chr version")
                put("install_libs", "chr install")
                put("install_libs_alias_of", "chr library install")
                put("build", "chr build")
                put("build_hide_lib_warnings", "chr build --hide-lib-warnings")
                put("build_skip_lib_check", "chr build --skip-lib-check")
                put("build_format", "chr build --format=GTV")
                put("code_check", "chr code check")
                put("code_check_hide_lib_warnings", "chr code check --hide-lib-warnings")
                put("code_lint", "chr code lint")
                put("code_format", "chr code format")
                put("test", "chr test")
                put("test_hide_lib_warnings", "chr test --hide-lib-warnings")
                put("repl_sql_log", "chr repl --sql-log")
            }
        )
        put(
            "build_flags",
            buildJsonObject {
                put("settings", "-s, --settings=<settings>")
                put("blockchain", "-bc, --blockchain=<blockchain>")
                put("format", "-f, --format=(GTV|XML)")
                put("hide_lib_warnings", "--hide-lib-warnings")
                put("skip_lib_check", "--skip-lib-check  # since 0.30.0")
            }
        )
        put(
            "compile_keys",
            buildJsonObject {
                put("rellVersion", "semver N.N.N; production pin ${DappScaffold.RELL_VERSION} (docs example 0.14.9 is stale)")
                put("source", "relative source dir; default src")
                put("target", "relative output dir; default build")
                put("deprecatedError", "boolean")
                put("quiet", "boolean")
                put("strictGtvConversion", "boolean; default true; leave true (Rell 0.13.9+)")
            }
        )
        put(
            "code_flags",
            buildJsonObject {
                put("check_hide_lib_warnings", "--hide-lib-warnings")
                put("lint_fix", "--fix  # lint only")
                put("source_dir", "--source-dir=<path>  # lint / format")
                put("formatter_options", "-fo, --formatter-options=<path>  # default .rell_format")
                put("linter_options", "-lo, --linter-options=<path>  # lint; default .rell_lint")
                put("format_file", "--file=<path>  # format a single Rell file")
            }
        )
        put(
            "test_flags",
            buildJsonObject {
                put("settings", "-s, --settings=<settings>")
                put("blockchain", "-bc, --blockchain=<blockchain>")
                put("modules", "-m, --modules=<modules>  # comma-delimited; not a file path")
                put("file", "--file=<path>")
                put("tests", "--tests=<pattern>")
                put("db", "--use-db / --no-db")
                put("test_report", "--test-report")
                put("test_report_dir", "--test-report-dir=<path>  # default build/reports")
                put("fail_on_error", "--fail-on-error[=true|false]")
                put("timestamp", "-ts, --timestamp")
                put("hide_lib_warnings", "--hide-lib-warnings")
            }
        )
        put("code_docs", CODE_URL)
        put("code_index_docs", CODE_INDEX_URL)
        put("code_index_url_slash", CODE_INDEX_URL_SLASH)
        put("code_index_title", CODE_INDEX_TITLE)
        put("get_started_benefits_index_docs", GET_STARTED_BENEFITS_INDEX_URL)
        put("get_started_benefits_index_url_slash", GET_STARTED_BENEFITS_INDEX_URL_SLASH)
        put("get_started_benefits_index_title", GET_STARTED_BENEFITS_INDEX_TITLE)
        put("get_started_extensions_index_docs", GET_STARTED_EXTENSIONS_INDEX_URL)
        put("get_started_extensions_index_url_slash", GET_STARTED_EXTENSIONS_INDEX_URL_SLASH)
        put("get_started_extensions_index_title", GET_STARTED_EXTENSIONS_INDEX_TITLE)
        put("get_started_use_cases_index_docs", GET_STARTED_USE_CASES_INDEX_URL)
        put("get_started_use_cases_index_url_slash", GET_STARTED_USE_CASES_INDEX_URL_SLASH)
        put("get_started_use_cases_index_title", GET_STARTED_USE_CASES_INDEX_TITLE)
        put("get_started_staking_summary_index_docs", GET_STARTED_STAKING_SUMMARY_INDEX_URL)
        put("get_started_staking_summary_index_url_slash", GET_STARTED_STAKING_SUMMARY_INDEX_URL_SLASH)
        put("get_started_staking_summary_index_title", GET_STARTED_STAKING_SUMMARY_INDEX_TITLE)
        put("get_started_installation_index_docs", GET_STARTED_INSTALLATION_INDEX_URL)
        put("get_started_installation_index_url_slash", GET_STARTED_INSTALLATION_INDEX_URL_SLASH)
        put("get_started_installation_index_title", GET_STARTED_INSTALLATION_INDEX_TITLE)
        put("ecosystem_filehub_deploy_filehub_index_url_slash", ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_URL_SLASH)
        put("ecosystem_filehub_deploy_filehub_index_title", ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_TITLE)
        put("ecosystem_mass_exit_operations_index_url_slash", ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_URL_SLASH)
        put("ecosystem_mass_exit_operations_index_title", ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_TITLE)
        put("ecosystem_setup_tls_index_url_slash", ECOSYSTEM_SETUP_TLS_INDEX_URL_SLASH)
        put("ecosystem_setup_tls_index_title", ECOSYSTEM_SETUP_TLS_INDEX_TITLE)
        put("ecosystem_pmc_container_index_url_slash", ECOSYSTEM_PMC_CONTAINER_INDEX_URL_SLASH)
        put("ecosystem_pmc_container_index_title", ECOSYSTEM_PMC_CONTAINER_INDEX_TITLE)
        put("ecosystem_gov_delegates_index_url_slash", ECOSYSTEM_GOV_DELEGATES_INDEX_URL_SLASH)
        put("ecosystem_gov_delegates_index_title", ECOSYSTEM_GOV_DELEGATES_INDEX_TITLE)
        put("ecosystem_gov_vote_weight_strategy_index_url_slash", ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_URL_SLASH)
        put("ecosystem_gov_vote_weight_strategy_index_title", ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_TITLE)
        put("ecosystem_block_explorer_overview_index_url_slash", ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_URL_SLASH)
        put("ecosystem_block_explorer_overview_index_title", ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_TITLE)
        put("ecosystem_filehub_configure_filehub_index_url_slash", ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_URL_SLASH)
        put("ecosystem_filehub_configure_filehub_index_title", ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_TITLE)
        put("learn_big_data_intro_index_url_slash", LEARN_BIG_DATA_INTRO_INDEX_URL_SLASH)
        put("learn_big_data_intro_index_title", LEARN_BIG_DATA_INTRO_INDEX_TITLE)
        put("learn_book_review_related_entity_index_url_slash", LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_URL_SLASH)
        put("learn_book_review_related_entity_index_title", LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_TITLE)
        put("learn_evm_developers_intro_index_url_slash", LEARN_EVM_DEVELOPERS_INTRO_INDEX_URL_SLASH)
        put("learn_evm_developers_intro_index_title", LEARN_EVM_DEVELOPERS_INTRO_INDEX_TITLE)
        put("learn_evm_news_feed_index_url_slash", LEARN_EVM_NEWS_FEED_INDEX_URL_SLASH)
        put("learn_evm_news_feed_index_title", LEARN_EVM_NEWS_FEED_INDEX_TITLE)
        put("learn_ft4_demo_blockchain_setup_index_url_slash", LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_URL_SLASH)
        put("learn_ft4_demo_blockchain_setup_index_title", LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_TITLE)
        put("test_docs", "https://docs.chromia.com/build/cli/commands/test")
        put("chromia_yml_shape", expectedYmlShape())
        put("learn_news_basic_ops_index_url_slash", LEARN_NEWS_BASIC_OPS_INDEX_URL_SLASH)
        put("learn_news_basic_ops_index_title", LEARN_NEWS_BASIC_OPS_INDEX_TITLE)
        put("learn_ttt_data_tables_index_url_slash", LEARN_TTT_DATA_TABLES_INDEX_URL_SLASH)
        put("learn_ttt_data_tables_index_title", LEARN_TTT_DATA_TABLES_INDEX_TITLE)
        put("learn_vector_db_upload_vectors_index_url_slash", LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_URL_SLASH)
        put("learn_vector_db_upload_vectors_index_title", LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_TITLE)
        put("learn_monetize_intro_index_url_slash", LEARN_MONETIZE_INTRO_INDEX_URL_SLASH)
        put("learn_monetize_intro_index_title", LEARN_MONETIZE_INTRO_INDEX_TITLE)
        put("learn_zk_dapp_setup_run_index_url_slash", LEARN_ZK_DAPP_SETUP_RUN_INDEX_URL_SLASH)
        put("learn_zk_dapp_setup_run_index_title", LEARN_ZK_DAPP_SETUP_RUN_INDEX_TITLE)
        put("learn_chat_agent_explore_index_url_slash", LEARN_CHAT_AGENT_EXPLORE_INDEX_URL_SLASH)
        put("learn_chat_agent_explore_index_title", LEARN_CHAT_AGENT_EXPLORE_INDEX_TITLE)
        put("rell_expressions_jump_index_url_slash", RELL_EXPRESSIONS_JUMP_INDEX_URL_SLASH)
        put("rell_expressions_jump_index_title", RELL_EXPRESSIONS_JUMP_INDEX_TITLE)
        put("learn_web3_compare_frontend_index_url_slash", LEARN_WEB3_COMPARE_FRONTEND_INDEX_URL_SLASH)
        put("learn_web3_compare_frontend_index_title", LEARN_WEB3_COMPARE_FRONTEND_INDEX_TITLE)
        put("rell_releases_index_url_slash", RELL_RELEASES_INDEX_URL_SLASH)
        put("rell_releases_index_title", RELL_RELEASES_INDEX_TITLE)
        put("docs_root_index_url_slash", DOCS_ROOT_INDEX_URL_SLASH)
        put("docs_root_index_title", DOCS_ROOT_INDEX_TITLE)
        put("learn_home_index_url_slash", LEARN_HOME_INDEX_URL_SLASH)
        put("learn_home_index_title", LEARN_HOME_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover BUILD cli/commands/build leftovers encoded as BUILD_INDEX_* (query-only).
// Leftover official leftover BUILD cli/commands/code leftovers encoded as CODE_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/benefits INDEX leftovers encoded as GET_STARTED_BENEFITS_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/extensions INDEX leftovers encoded as GET_STARTED_EXTENSIONS_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/use-cases INDEX leftovers encoded as GET_STARTED_USE_CASES_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/staking-summary INDEX leftovers encoded as GET_STARTED_STAKING_SUMMARY_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/installation INDEX leftovers encoded as GET_STARTED_INSTALLATION_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/filehub/filehub-setup/deploy-filehub INDEX leftovers encoded as ECOSYSTEM_FILEHUB_DEPLOY_FILEHUB_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/bridge/mass-exit/operations INDEX leftovers encoded as ECOSYSTEM_MASS_EXIT_OPERATIONS_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/setup-tls INDEX leftovers encoded as ECOSYSTEM_SETUP_TLS_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/container INDEX leftovers encoded as ECOSYSTEM_PMC_CONTAINER_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/extensions/delegates INDEX leftovers encoded as ECOSYSTEM_GOV_DELEGATES_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-structure/vote-weight-strategy INDEX leftovers encoded as ECOSYSTEM_GOV_VOTE_WEIGHT_STRATEGY_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/block-explorer/overview INDEX leftovers encoded as ECOSYSTEM_BLOCK_EXPLORER_OVERVIEW_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/filehub/configure-filehub/filehub-configure INDEX leftovers encoded as ECOSYSTEM_FILEHUB_CONFIGURE_FILEHUB_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/big-data/introduction INDEX leftovers encoded as LEARN_BIG_DATA_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/book-review/book-review-entity INDEX leftovers encoded as LEARN_BOOK_REVIEW_RELATED_ENTITY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/chromia-for-evm-developers/introduction INDEX leftovers encoded as LEARN_EVM_DEVELOPERS_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/chromia-for-evm-developers/news-feed INDEX leftovers encoded as LEARN_EVM_NEWS_FEED_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/ft4-demo-app/module-blockchain/setup INDEX leftovers encoded as LEARN_FT4_DEMO_BLOCKCHAIN_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/my-news-feed/module-one/operations-queries/basic-operations INDEX leftovers encoded as LEARN_NEWS_BASIC_OPS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-one/data-modeling/tables INDEX leftovers encoded as LEARN_TTT_DATA_TABLES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/vector-db-movie-demo/data-pipeline/upload-vectors INDEX leftovers encoded as LEARN_VECTOR_DB_UPLOAD_VECTORS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/monetize-dapp/introduction INDEX leftovers encoded as LEARN_MONETIZE_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX zero-knowledge-proof dapp-setup-run leftovers encoded as LEARN_ZK_DAPP_SETUP_RUN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX chat-agent-course explore-and-extend leftovers encoded as LEARN_CHAT_AGENT_EXPLORE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/expressions/jump-expressions INDEX leftovers encoded as RELL_EXPRESSIONS_JUMP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX web3-for-web2-devs compare-frontend leftovers encoded as LEARN_WEB3_COMPARE_FRONTEND_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/releases INDEX leftovers encoded as RELL_RELEASES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover DOCS docs.chromia.com root INDEX leftovers encoded as DOCS_ROOT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN learn.chromia.com home INDEX leftovers encoded as LEARN_HOME_INDEX_* (query-only HELP ONLY WRITE SKIP).
