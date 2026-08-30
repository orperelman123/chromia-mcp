package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official BUILD integrations hub (read-only facts).
 * No invented package ids. C# NuGet id is unpublished on the official C# page.
 * Skips exchange account-creation / transfer / memo write operations.
 * Leftover official leftover BUILD integrations index leftovers live here (query-only).
 * Leftover official leftover BUILD token-chain index leftovers also live here (query-only).
 * Leftover official leftover BUILD exchange-guide leftovers also live here (query-only).
 * Leftover official leftover BUILD integrations/exchange-guide index leftovers also live here (query-only).
 * Leftover official leftover BUILD integrations/memo-guide index leftovers also live here (query-only).
 * Leftover official leftover BUILD integrations/exchange-guide/overview index leftovers also live here (query-only).
 * Leftover official leftover BUILD integrations/exchange-guide/step-1-account index leftovers also live here (query-only).
 * Leftover official leftover BUILD integrations/exchange-guide/step-2-access index leftovers also live here (query-only).
 * Leftover official leftover BUILD integrations/exchange-guide/step-3-transaction index leftovers also live here (query-only).
 * Leftover official leftover BUILD integrations/exchange-guide/step-4-non-existent-accounts index leftovers also live here (query-only).
 * Leftover official leftover BUILD integrations/exchange-guide/additional-resources index leftovers also live here (query-only).
 * Leftover official leftover BUILD token-chain/user-account-creation index leftovers also live here (query-only).
 * Leftover official leftover BUILD token-chain/developer-token-proposal index leftovers also live here (query-only).
 * Leftover official leftover GET-STARTED get-started/about/architecture/chains INDEX leftovers also live here (query-only).
 * Leftover official leftover GET-STARTED get-started/about/architecture/chains/directory-chain INDEX leftovers also live here (query-only).
 * Leftover official leftover GET-STARTED get-started/about/providers INDEX leftovers also live here (query-only).
 * Leftover official leftover GET-STARTED get-started/about/architecture/chains/economy-chain INDEX leftovers also live here (query-only).
 * Leftover official leftover GET-STARTED get-started/about/architecture/chains/cluster-anchoring-chain INDEX leftovers also live here (query-only).
 * Leftover official leftover GET-STARTED get-started/use-cases/cross-chain INDEX leftovers also live here (query-only).
 * Leftover official leftover GET-STARTED get-started/about/architecture/chains/token-chain INDEX leftovers also live here (query-only).
 */
object ChromiaIntegrationsHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val TOOL_NAME = "chromia_integrations_help"
    const val HUB_URL = "https://docs.chromia.com/build/integrations/"
    const val INTEGRATIONS_INDEX_URL = "https://docs.chromia.com/build/integrations"
    const val INTEGRATIONS_INDEX_URL_SLASH = HUB_URL
    const val INTEGRATIONS_INDEX_TITLE = "Integrations"
    const val INTEGRATIONS_INDEX_CARD_EXCHANGE = "Exchange integration guide"
    const val INTEGRATIONS_INDEX_CARD_MEMO = "Memo integration guide"
    const val EXCHANGE_URL = "https://docs.chromia.com/build/integrations/exchange-guide"
    const val EXCHANGE_INDEX_URL = EXCHANGE_URL
    const val EXCHANGE_INDEX_URL_SLASH = "https://docs.chromia.com/build/integrations/exchange-guide/"
    const val EXCHANGE_INDEX_TITLE = "Exchange guide"
    const val EXCHANGE_GUIDE_INDEX_URL = EXCHANGE_INDEX_URL
    const val EXCHANGE_GUIDE_INDEX_URL_SLASH = EXCHANGE_INDEX_URL_SLASH
    const val EXCHANGE_GUIDE_INDEX_TITLE = "Exchange integration guide"
    const val EXCHANGE_INDEX_CARD_OVERVIEW = "Chromia overview"
    const val EXCHANGE_INDEX_CARD_STEP1 = "Step 1: Account creation"
    const val EXCHANGE_INDEX_CARD_STEP2 = "Step 2: Access Chromia's Economy Chain"
    const val EXCHANGE_INDEX_CARD_STEP3 = "Step 3: Deposits, withdrawals, and monitoring"
    const val EXCHANGE_INDEX_CARD_STEP4 = "Step 4: Sending assets to non-existent accounts"
    const val EXCHANGE_INDEX_CARD_RESOURCES = "Additional resources"
    const val EXCHANGE_OVERVIEW_URL = "https://docs.chromia.com/build/integrations/exchange-guide/overview"
    const val EXCHANGE_OVERVIEW_INDEX_URL = EXCHANGE_OVERVIEW_URL
    const val EXCHANGE_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/build/integrations/exchange-guide/overview/"
    const val EXCHANGE_OVERVIEW_INDEX_TITLE = "Core concepts of Chromia"
    const val EXCHANGE_STEP1_URL = "https://docs.chromia.com/build/integrations/exchange-guide/step-1-account"
    const val EXCHANGE_STEP1_INDEX_URL = EXCHANGE_STEP1_URL
    const val EXCHANGE_STEP1_INDEX_URL_SLASH = "https://docs.chromia.com/build/integrations/exchange-guide/step-1-account/"
    const val EXCHANGE_STEP1_INDEX_TITLE = "Step 1: Account creation"
    const val EXCHANGE_STEP_1_INDEX_URL = EXCHANGE_STEP1_INDEX_URL
    const val EXCHANGE_STEP_1_INDEX_URL_SLASH = EXCHANGE_STEP1_INDEX_URL_SLASH
    const val EXCHANGE_STEP_1_INDEX_TITLE = "Step 1: Account creation"
    const val EXCHANGE_STEP2_URL = "https://docs.chromia.com/build/integrations/exchange-guide/step-2-access"
    const val EXCHANGE_STEP2_INDEX_URL = EXCHANGE_STEP2_URL
    const val EXCHANGE_STEP2_INDEX_URL_SLASH = "https://docs.chromia.com/build/integrations/exchange-guide/step-2-access/"
    const val EXCHANGE_STEP2_INDEX_TITLE = "Step 2: Access Chromia's Economy Chain"
    const val EXCHANGE_STEP3_URL = "https://docs.chromia.com/build/integrations/exchange-guide/step-3-transaction"
    const val EXCHANGE_STEP3_INDEX_URL = EXCHANGE_STEP3_URL
    const val EXCHANGE_STEP3_INDEX_URL_SLASH = "https://docs.chromia.com/build/integrations/exchange-guide/step-3-transaction/"
    const val EXCHANGE_STEP3_INDEX_TITLE = "Step 3: Deposits, withdrawals, and transaction monitoring"
    const val EXCHANGE_STEP4_URL = "https://docs.chromia.com/build/integrations/exchange-guide/step-4-non-existent-accounts"
    const val EXCHANGE_STEP4_INDEX_URL = EXCHANGE_STEP4_URL
    const val EXCHANGE_STEP4_INDEX_URL_SLASH = "https://docs.chromia.com/build/integrations/exchange-guide/step-4-non-existent-accounts/"
    const val EXCHANGE_STEP4_INDEX_TITLE = "Step 4: Sending assets to non-existent accounts"
    const val EXCHANGE_RESOURCES_URL = "https://docs.chromia.com/build/integrations/exchange-guide/additional-resources"
    const val EXCHANGE_RESOURCES_INDEX_URL = EXCHANGE_RESOURCES_URL
    const val EXCHANGE_RESOURCES_INDEX_URL_SLASH = "https://docs.chromia.com/build/integrations/exchange-guide/additional-resources/"
    const val EXCHANGE_RESOURCES_INDEX_TITLE = "Additional resources"
    const val EXCHANGE_ADDITIONAL_RESOURCES_INDEX_URL = EXCHANGE_RESOURCES_INDEX_URL
    const val EXCHANGE_ADDITIONAL_RESOURCES_INDEX_URL_SLASH = EXCHANGE_RESOURCES_INDEX_URL_SLASH
    const val EXCHANGE_ADDITIONAL_RESOURCES_INDEX_TITLE = "Additional resources"
    const val MEMO_URL = "https://docs.chromia.com/build/integrations/memo-guide"
    const val MEMO_INDEX_URL = MEMO_URL
    const val MEMO_INDEX_URL_SLASH = "https://docs.chromia.com/build/integrations/memo-guide/"
    const val MEMO_INDEX_TITLE = "Memo integration guide"
    const val MEMO_GUIDE_INDEX_URL = MEMO_INDEX_URL
    const val MEMO_GUIDE_INDEX_URL_SLASH = MEMO_INDEX_URL_SLASH
    const val MEMO_GUIDE_INDEX_TITLE = "Memo integration guide"
    const val FT4_CLIENT_URL = "https://docs.chromia.com/build/ft4/client/client-setup"
    const val CSHARP_URL = ChromiaLanguageClientsHelp.CSHARP_URL
    const val TOKEN_CHAIN_URL = "https://docs.chromia.com/build/token-chain/"
    const val TOKEN_CHAIN_INDEX_URL = "https://docs.chromia.com/build/token-chain"
    const val TOKEN_CHAIN_INDEX_URL_SLASH = TOKEN_CHAIN_URL
    const val TOKEN_CHAIN_INDEX_TITLE = "Token Chain"
    const val TOKEN_CHAIN_INDEX_CARD_ACCOUNT = "User account creation"
    const val TOKEN_CHAIN_INDEX_CARD_PROPOSAL = "Developer token proposal"
    const val TOKEN_CHAIN_INDEX_SIDEBAR_PROPOSAL = "Developer token proposal and bridge setup"
    const val TOKEN_CHAIN_ACCOUNT_URL = "https://docs.chromia.com/build/token-chain/user-account-creation"
    const val TOKEN_CHAIN_ACCOUNT_INDEX_URL = TOKEN_CHAIN_ACCOUNT_URL
    const val TOKEN_CHAIN_ACCOUNT_INDEX_URL_SLASH = "https://docs.chromia.com/build/token-chain/user-account-creation/"
    const val TOKEN_CHAIN_ACCOUNT_INDEX_TITLE = "User account creation"
    const val TOKEN_CHAIN_PROPOSAL_URL = "https://docs.chromia.com/build/token-chain/developer-token-proposal/"
    const val TOKEN_CHAIN_PROPOSAL_REDIRECT = "https://docs.chromia.com/build/token-chain/developer-token-proposal"
    const val TOKEN_CHAIN_PROPOSAL_INDEX_URL = TOKEN_CHAIN_PROPOSAL_REDIRECT
    const val TOKEN_CHAIN_PROPOSAL_INDEX_URL_SLASH = TOKEN_CHAIN_PROPOSAL_URL
    const val TOKEN_CHAIN_PROPOSAL_INDEX_TITLE = "Developer token proposal and bridge setup"
    const val TOKEN_CHAIN_ARCH_URL = "https://docs.chromia.com/get-started/about/architecture/chains/token-chain/"
    const val GET_STARTED_SYSTEM_CHAINS_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/chains"
    const val GET_STARTED_SYSTEM_CHAINS_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/chains/"
    const val GET_STARTED_SYSTEM_CHAINS_INDEX_TITLE = "Chromia System Chains"  // official H1
    const val GET_STARTED_DIRECTORY_CHAIN_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/chains/directory-chain"
    const val GET_STARTED_DIRECTORY_CHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/chains/directory-chain/"
    const val GET_STARTED_DIRECTORY_CHAIN_INDEX_TITLE = "Directory Chain"  // official H1
    const val GET_STARTED_PROVIDERS_INDEX_URL = "https://docs.chromia.com/get-started/about/providers"
    const val GET_STARTED_PROVIDERS_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/providers/"
    const val GET_STARTED_PROVIDERS_INDEX_TITLE = "Providers"  // official H1
    const val GET_STARTED_ECONOMY_CHAIN_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/chains/economy-chain"
    const val GET_STARTED_ECONOMY_CHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/chains/economy-chain/"
    const val GET_STARTED_ECONOMY_CHAIN_INDEX_TITLE = "Economy Chain"  // official H1
    const val GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/chains/cluster-anchoring-chain"
    const val GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/chains/cluster-anchoring-chain/"
    const val GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_TITLE = "Cluster Anchoring Chain"  // official H1
    const val GET_STARTED_CROSS_CHAIN_INDEX_URL = "https://docs.chromia.com/get-started/use-cases/cross-chain"
    const val GET_STARTED_CROSS_CHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/cross-chain/"
    const val GET_STARTED_CROSS_CHAIN_INDEX_TITLE = "Cross-chain applications"  // official H1
    const val GET_STARTED_TOKEN_CHAIN_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/chains/token-chain"
    const val GET_STARTED_TOKEN_CHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/chains/token-chain/"
    const val GET_STARTED_TOKEN_CHAIN_INDEX_TITLE = "Token Chain"  // official H1
    const val LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_URL = "https://learn.chromia.com/courses/book-review/input-verification/structure"
    const val LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/input-verification/structure/"
    const val LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_TITLE = "Adding structured results from queries"  // official H1
    const val LEARN_ICCF_INTRO_INDEX_URL = "https://learn.chromia.com/courses/iccf-course/introduction"
    const val LEARN_ICCF_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/iccf-course/introduction/"
    const val LEARN_ICCF_INTRO_INDEX_TITLE = "Confirm Events Across Blockchains"  // official H1
    const val LEARN_NEWS_UNIT_TESTS_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/input-verification/tests"
    const val LEARN_NEWS_UNIT_TESTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/input-verification/tests/"
    const val LEARN_NEWS_UNIT_TESTS_INDEX_TITLE = "Run unit tests"  // official H1
    const val LEARN_TTT_PROJECT_STRUCTURE_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/project-structure"
    const val LEARN_TTT_PROJECT_STRUCTURE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/project-structure/"
    const val LEARN_TTT_PROJECT_STRUCTURE_INDEX_TITLE = "Lesson 3 - Project structure of the dapp"  // official H1
    const val LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/configure.rell"
    const val LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/configure.rell/"
    const val LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_TITLE = "Configure your Rell module"  // official H1
    const val LEARN_MONETIZE_SETUP_INDEX_URL = "https://learn.chromia.com/courses/monetize-dapp/setup"
    const val LEARN_MONETIZE_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/monetize-dapp/setup/"
    const val LEARN_MONETIZE_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_ZK_INTRO_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/introduction"
    const val LEARN_ZK_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/introduction/"
    const val LEARN_ZK_INTRO_INDEX_TITLE = "Introduction to the course"  // official H1
    const val LEARN_RELATIONSHIPS_INTRO_INDEX_URL = "https://learn.chromia.com/courses/relationships-course/introduction"
    const val LEARN_RELATIONSHIPS_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/relationships-course/introduction/"
    const val LEARN_RELATIONSHIPS_INTRO_INDEX_TITLE = "Understand relationships in Rell"  // official H1
    const val LEARN_WEB3_SECURITY_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/security"
    const val LEARN_WEB3_SECURITY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/security/"
    const val LEARN_WEB3_SECURITY_INDEX_TITLE = "Security"  // official H1
    const val LEARN_TAGS_WEB3_INDEX_URL = "https://learn.chromia.com/tags/Web3"
    const val LEARN_TAGS_WEB3_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Web3/"
    const val LEARN_TAGS_WEB3_INDEX_TITLE = "Courses tagged with: Web3"  // official H1
    const val TOKEN_CHAIN_SOURCE = "https://gitlab.com/chromaway/core/directory-chain/-/tree/dev/src/token_chain"

    val childPages = listOf(
        HUB_URL,
        EXCHANGE_URL,
        EXCHANGE_OVERVIEW_URL,
        EXCHANGE_STEP1_URL,
        EXCHANGE_STEP2_URL,
        EXCHANGE_STEP3_URL,
        EXCHANGE_STEP4_URL,
        EXCHANGE_RESOURCES_URL,
        MEMO_URL,
        TOKEN_CHAIN_URL,
        TOKEN_CHAIN_ACCOUNT_URL,
        TOKEN_CHAIN_PROPOSAL_URL
    )

    val skipped = listOf(
        "exchange-guide step-1 account creation (write)",
        "leftover exchange-guide/step-1-account account-creation / write recipes (WRITE SKIP)",
        "leftover exchange-guide/step-2-access replica-node / keygen / write recipes (WRITE SKIP)",
        "exchange-guide step-3 deposit / withdrawal / transfer (write)",
        "leftover exchange-guide/step-3-transaction deposit / withdrawal / write recipes (WRITE SKIP)",
        "leftover exchange-guide/step-4-non-existent-accounts transfer / registerAccount / write recipes (WRITE SKIP)",
        "leftover exchange-guide/additional-resources deposit / withdrawal / write recipes (WRITE SKIP)",
        "leftover exchange-guide/overview deposit / withdrawal / write recipes (WRITE SKIP)",
        "enable_transfer_memo / disable_transfer_memo / memo(text) operations",
        "C# NuGet package id (official page does not print one)",
        "invented exchange / wallet / broker package ids",
        "leftover token-chain user-account-creation chr tx transfer (hard skip)",
        "leftover token-chain propose_token / propose_token_bridge / mint_token / --evm-auth (hard skip)",
        "invented TOKEN_CHAIN_RID / ECONOMY_CHAIN_RID / EVM_TRANSACTION_SUBMITTER_CHAIN_RID / 64-hex",
        "leftover ras_token_iccf write docs",
        "leftover printed sample admin pubkey 03028A31"
    )

    val proposalFields = listOf(
        "Token name",
        "Token symbol",
        "Token decimals",
        "Token icon  # URL pointing to an image file",
        "Minting policies  # leftover official: who can mint, how many, how often",
        "Account creation blockchains  # leftover official: blockchain RIDs allowed to create accounts"
    )

    val leftoverFees = listOf(
        "100 CHR token proposal  # leftover official printed listing fee; verify with get_token_chain_constants",
        "100 CHR bridge proposal  # leftover official printed listing fee; verify with get_token_chain_constants",
        "25% burn / 25% Foundation / 50% project pool  # leftover official default split; burn enabled in a future release",
        "listing fee refunded if the proposal is rejected  # leftover official"
    )

    fun bridgeConfigurationStruct(): String = """
        struct bridge_configuration {
          network_id: integer;
          bridge_contract: byte_array;
          token_contract: byte_array;
          eif.hbridge.bridge_mode;
          use_snapshots: boolean;
          skip_to_height: integer;
        }
    """.trimIndent() + "\n"

    fun tokenChainQueryShapes(): List<String> = listOf(
        "chr query --blockchain-rid " + "\${TOKEN_CHAIN_RID} get_token_chain_constants  # leftover official; placeholder only",
        "chr query --blockchain-rid " + "\${TOKEN_CHAIN_RID} get_proposals_by_proposer proposer=\${YOUR_ACCOUNT_ID}  # leftover official argument name proposer",
        "chr query --blockchain-rid " + "\${TOKEN_CHAIN_RID} ft4.get_assets_by_name name=\${YOUR_TOKEN_NAME} page_size=null page_cursor=null",
        "chr query --blockchain-rid " + "\${DIRECTORY_CHAIN_RID} get_evm_transaction_submitter_chain_rid  # leftover official Directory Chain query",
        "chr query --blockchain-rid " + "\${EVM_TRANSACTION_SUBMITTER_CHAIN_RID} get_all_bridges  # leftover official; identify Token Chain validator contract"
    )


    fun notes(): String = """
        Official Chromia BUILD integrations hub. CLI $CLI_SERIES. Java 21+, Postgres 16+.
        Hub: $HUB_URL
        Leftover official BUILD integrations (leftover official $INTEGRATIONS_INDEX_URL leftover official 307 leftover official $INTEGRATIONS_INDEX_URL_SLASH leftover official 200 leftover official $INTEGRATIONS_INDEX_TITLE): leftover official leftover intro leftover official leftover Chromia integrations provide comprehensive guides and tools for seamlessly connecting external systems, exchanges, and applications with the Chromia blockchain leftover official leftover These integration resources help developers and technical teams efficiently implement secure, scalable solutions leftover official leftover child leftover official leftover cards leftover official leftover $INTEGRATIONS_INDEX_CARD_EXCHANGE leftover official leftover $EXCHANGE_URL leftover official leftover Complete step-by-step guide for exchanges to integrate with Chromia, covering account creation, transaction handling, monitoring, and advanced features like multisig operations leftover official leftover $INTEGRATIONS_INDEX_CARD_MEMO leftover official leftover $MEMO_URL leftover official leftover Learn how to implement memo functionality for FT4 transfers, enabling textual annotations for transactions that are essential for exchange integrations and shared account management leftover official leftover sidebar leftover official leftover Exchange guide leftover official leftover Memo integration guide leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover exchange leftover official leftover account-creation leftover official leftover write leftover official leftover deposit leftover official leftover withdrawal leftover official leftover write.
        Leftover official BUILD exchange-guide (leftover official $EXCHANGE_INDEX_URL leftover official 307 leftover official $EXCHANGE_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_INDEX_TITLE): leftover official leftover intro leftover official leftover This guide is designed for technical teams at exchanges that have decided to list Chromia and are focused on completing the integration efficiently leftover official leftover The guide provides a step-by-step process to assist exchanges in integrating with Chromia leftover official leftover child leftover official leftover cards leftover official leftover $EXCHANGE_INDEX_CARD_OVERVIEW leftover official leftover $EXCHANGE_OVERVIEW_URL leftover official leftover This section provides a high-level introduction to Chromia, its architecture, and key features to help exchanges understand the platform before starting integration leftover official leftover $EXCHANGE_INDEX_CARD_STEP1 leftover official leftover $EXCHANGE_STEP1_URL leftover official leftover Learn how to create and manage Chromia accounts leftover official leftover $EXCHANGE_INDEX_CARD_STEP2 leftover official leftover $EXCHANGE_STEP2_URL leftover official leftover Instructions on connecting to Chromia's Economy Chain using public system nodes or by setting up a private replica node for enhanced control leftover official leftover $EXCHANGE_INDEX_CARD_STEP3 leftover official leftover $EXCHANGE_STEP3_URL leftover official leftover Detailed guidance on handling deposits, withdrawals, and monitoring transactions using Chromia's FT4 library leftover official leftover $EXCHANGE_INDEX_CARD_STEP4 leftover official leftover $EXCHANGE_STEP4_URL leftover official leftover Explore how to send assets to accounts that have not yet been activated and ensure users can access their funds securely leftover official leftover $EXCHANGE_INDEX_CARD_RESOURCES leftover official leftover $EXCHANGE_RESOURCES_URL leftover official leftover Access extra tools, documentation, and links for testing, development, and troubleshooting to streamline integration leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover step-1 leftover official leftover account leftover official leftover creation leftover official leftover write leftover official leftover step-3 leftover official leftover deposit leftover official leftover withdrawal leftover official leftover write leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Leftover official leftover BUILD integrations/exchange-guide (leftover official $EXCHANGE_GUIDE_INDEX_URL leftover official 307 leftover official $EXCHANGE_GUIDE_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_GUIDE_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official BUILD integrations/exchange-guide/overview (leftover official $EXCHANGE_OVERVIEW_INDEX_URL leftover official 307 leftover official $EXCHANGE_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_OVERVIEW_INDEX_TITLE): leftover official leftover intro leftover official leftover Chromia is a Layer-1 blockchain platform designed to deliver high performance and scalability for decentralized applications leftover official leftover Its multi-chain architecture ensures that each dapp operates independently, avoiding network congestion and maintaining optimal performance even during peak usage leftover official leftover Key features of Chromia leftover official leftover Gas-Free Transactions leftover official leftover Users can interact with dapps without paying transaction fees leftover official leftover On-Chain Data Storage leftover official leftover Chromia allows large-scale data to be stored directly on the blockchain leftover official leftover Relational Data Model leftover official leftover By integrating PostgreSQL leftover official leftover Scalability leftover official leftover Each dapp operates on its dedicated chain leftover official leftover Key components leftover official leftover Economy Chain leftover official leftover the primary chain for integration leftover official leftover Postchain leftover official leftover eBFT leftover official leftover Enhanced Byzantine Fault Tolerance leftover official leftover Rell leftover official leftover FT4 protocol leftover official leftover Chromia Vault leftover official leftover Dapp clusters leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover deposit leftover official leftover withdrawal leftover official leftover write leftover official leftover recipes leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official BUILD integrations/exchange-guide/step-1-account (leftover official $EXCHANGE_STEP1_INDEX_URL leftover official 307 leftover official $EXCHANGE_STEP1_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_STEP1_INDEX_TITLE): leftover official leftover intro leftover official leftover Creating an account on Chromia's Economy Chain is a crucial first step for integrating with the network leftover official leftover An account acts as a unique identifier that enables interaction with decentralized applications on the Chromia blockchain leftover official leftover It is required for executing transactions, holding and managing CHR tokens, and participating in staking and other ecosystem functionalities leftover official leftover For exchange integration, you must create a dedicated account on the Economy Chain leftover official leftover This account will serve as the foundation for all exchange-related operations leftover official leftover Bridging CHR from an EVM-compatible blockchain leftover official leftover Staking CHR leftover official leftover Paying a fee via internal transfer leftover official leftover 10 CHR leftover official leftover printed leftover official leftover one-time leftover official leftover account leftover official leftover creation leftover official leftover fee leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover account leftover official leftover creation leftover official leftover write leftover official leftover recipes leftover official leftover bridging leftover official leftover staking leftover official leftover internal leftover official leftover transfer leftover official leftover write leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official leftover BUILD integrations/exchange-guide/step-1-account (leftover official $EXCHANGE_STEP_1_INDEX_URL leftover official 307 leftover official $EXCHANGE_STEP_1_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_STEP_1_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official BUILD integrations/exchange-guide/step-2-access (leftover official $EXCHANGE_STEP2_INDEX_URL leftover official 307 leftover official $EXCHANGE_STEP2_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_STEP2_INDEX_TITLE): leftover official leftover intro leftover official leftover To access Chromia's Economy Chain, you have two primary options leftover official leftover Public access leftover official leftover Use publicly available system nodes for a quick and simple connection without hosting your own infrastructure leftover official leftover Private node leftover official leftover Set up and run your own replica node for greater control, security, and reliability leftover official leftover These options cater to different levels of control, reliability, and infrastructure needs leftover official leftover Use public system nodes leftover official leftover For a quick and easy connection leftover official leftover you can use any of the publicly available system nodes leftover official leftover These nodes provide reliable access to the Economy Chain without the need to host your own infrastructure leftover official leftover Economy Chain BRID leftover official leftover public leftover official leftover nodes leftover official leftover Chromia leftover official leftover explorer leftover official leftover proceed leftover official leftover directly leftover official leftover to leftover official leftover Step 3 leftover official leftover Set up your own replica node leftover official leftover For greater control and security leftover official leftover setting up a private replica node is the recommended approach leftover official leftover Hosting your own node provides better reliability and flexibility leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover replica leftover official leftover node leftover official leftover register leftover official leftover write leftover official leftover recipes leftover official leftover pmc leftover official leftover keygen leftover official leftover provider leftover official leftover keypair leftover official leftover docker leftover official leftover write leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official BUILD integrations/exchange-guide/step-3-transaction (leftover official $EXCHANGE_STEP3_INDEX_URL leftover official 307 leftover official $EXCHANGE_STEP3_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_STEP3_INDEX_TITLE): leftover official leftover intro leftover official leftover Handling deposits, withdrawals, and monitoring transactions are essential components of integrating with Chromia's Economy Chain leftover official leftover These operations are streamlined using the FT4 library, which provides a comprehensive toolkit for interacting with the Chromia ecosystem leftover official leftover The FT4 library is designed to help developers build real-world applications within Chromia leftover official leftover Account creation and access management leftover official leftover Asset management leftover official leftover issuance leftover official leftover allocation leftover official leftover transfers leftover official leftover tracing leftover official leftover Using the FT4 library leftover official leftover manage deposits and withdrawals leftover official leftover monitoring transaction activities leftover official leftover Additional resources leftover official leftover Client setup leftover official leftover $FT4_CLIENT_URL leftover official leftover Postchain JS leftover official leftover TS leftover official leftover client leftover official leftover FT4 library overview leftover official leftover FT4 library GitLab leftover official leftover Memo integration guide leftover official leftover $MEMO_URL leftover official leftover Required Dependencies leftover official leftover @chromia/ft4@1.1.1 leftover official leftover postchain-client@1.22.0 leftover official leftover Node.js leftover official leftover 16 leftover official leftover query leftover official leftover createClient leftover official leftover createConnection leftover official leftover getAccountById leftover official leftover getTransferHistory leftover official leftover TransferHistoryType leftover official leftover Received leftover official leftover 200 leftover official leftover cap leftover official leftover pagination leftover official leftover nextCursor leftover official leftover getTransferDetails leftover official leftover getTransactionInfo leftover official leftover getTransactionStatus leftover official leftover getBlockInfo leftover official leftover getBalances leftover official leftover getBalanceByAssetId leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover deposit leftover official leftover withdrawal leftover official leftover transfer leftover official leftover write leftover official leftover recipes leftover official leftover key leftover official leftover store leftover official leftover offline leftover official leftover transaction leftover official leftover write leftover official leftover account leftover official leftover registration leftover official leftover write leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official BUILD integrations/exchange-guide/step-4-non-existent-accounts (leftover official $EXCHANGE_STEP4_INDEX_URL leftover official 307 leftover official $EXCHANGE_STEP4_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_STEP4_INDEX_TITLE): leftover official leftover intro leftover official leftover Exchanges can send assets to users who do not yet have a Chromia account by transferring funds to an unregistered account leftover official leftover This process allows users to activate their accounts through the Chromia Vault leftover official leftover The exchange should first verify if the user has an existing Chromia account leftover official leftover If the account exists leftover official leftover You can send any amount of CHR leftover official leftover If the account does not exist leftover official leftover Notify the user that they need to send at least 10 CHR leftover official leftover as this is the required fee for account creation leftover official leftover Once the transfer is complete leftover official leftover users can visit the Chromia Vault to activate their accounts leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover transfer leftover official leftover registerAccount leftover official leftover write leftover official leftover recipes leftover official leftover key leftover official leftover store leftover official leftover account leftover official leftover registration leftover official leftover write leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official leftover BUILD integrations/exchange-guide/step-4-non-existent-accounts (leftover official $EXCHANGE_STEP4_INDEX_URL leftover official 307 leftover official $EXCHANGE_STEP4_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_STEP4_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official BUILD integrations/exchange-guide/additional-resources (leftover official $EXCHANGE_RESOURCES_INDEX_URL leftover official 307 leftover official $EXCHANGE_RESOURCES_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_RESOURCES_INDEX_TITLE): leftover official leftover intro leftover official leftover Official website leftover official leftover Learn Chromia platform leftover official leftover Chromia Explorer leftover official leftover Chromia Vault leftover official leftover Staking leftover official leftover Testnet Explorer leftover official leftover Use the Testnet Explorer to monitor transactions and chain activity leftover official leftover Testnet staking leftover official leftover Test the staking functionality using a dedicated interface leftover official leftover Testnet Vault leftover official leftover Use Chromia Vault to test wallet features and bridging leftover official leftover Testnet faucet leftover official leftover Use the tCHR Faucet to obtain test tokens leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover deposit leftover official leftover withdrawal leftover official leftover write leftover official leftover recipes leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official leftover BUILD integrations/exchange-guide/additional-resources (leftover official $EXCHANGE_ADDITIONAL_RESOURCES_INDEX_URL leftover official 307 leftover official $EXCHANGE_ADDITIONAL_RESOURCES_INDEX_URL_SLASH leftover official 200 leftover official $EXCHANGE_ADDITIONAL_RESOURCES_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official BUILD integrations/memo-guide (leftover official $MEMO_INDEX_URL leftover official 307 leftover official $MEMO_INDEX_URL_SLASH leftover official 200 leftover official $MEMO_INDEX_TITLE): leftover official leftover intro leftover official leftover The memo feature adds an optional memo requirement for FT4 transfers, ensuring that certain accounts mandate memos for all incoming transfers leftover official leftover This feature is useful for attaching textual annotations to blockchain transactions, making it especially beneficial for applications requiring transaction context and shared account management leftover official leftover query leftover official leftover does_account_require_memo leftover official leftover account_id leftover official leftover byte_array leftover official leftover boolean leftover official leftover Checks if an FT4 account enforces memo requirements for incoming transfers leftover official leftover Enhanced transaction tracking leftover official leftover Operational efficiency leftover official leftover Risk mitigation leftover official leftover Exchange integrations leftover official leftover Shared account management leftover official leftover Payment processing leftover official leftover Audit leftover official leftover compliance leftover official leftover Cross-border leftover official leftover transfers leftover official leftover memo leftover official leftover text leftover official leftover 1 leftover official leftover 50 leftover official leftover characters leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover enable_transfer_memo leftover official leftover disable_transfer_memo leftover official leftover memo leftover official leftover write leftover official leftover transfer leftover official leftover recipes leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official leftover BUILD integrations/memo-guide (leftover official $MEMO_GUIDE_INDEX_URL leftover official 307 leftover official $MEMO_GUIDE_INDEX_URL_SLASH leftover official 200 leftover official $MEMO_GUIDE_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Official child pages are listed. This tool quotes read-only facts only.
        Hub facts: exchanges list CHR and other FT4 assets; memo-enabled transfers;
        private replica nodes; Economy Chain for asset management; FT4 client setup at $FT4_CLIENT_URL.
        Memo query only ($MEMO_URL): does_account_require_memo(account_id: byte_array): boolean.
        Official memo text constraint (on the write op, listed so callers know why the query exists): 1 to 50 characters.
        Skipped: account registration, deposits, withdrawals, enable/disable memo, memo+transfer, invented package ids.
        C# ($CSHARP_URL): official page does not print a NuGet package id — do not invent one.
        Leftover official token-chain hub (200): $TOKEN_CHAIN_URL — index only.
        Leftover official BUILD token-chain (leftover official $TOKEN_CHAIN_INDEX_URL leftover official 307 leftover official $TOKEN_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official $TOKEN_CHAIN_INDEX_TITLE): leftover official leftover intro leftover official leftover The Token Chain provides functionality for token management, account creation, and bridge setup leftover official leftover This section covers the practical aspects of working with the Token Chain leftover official leftover Install the Chromia CLI by following the instructions in Install and configure Chromia CLI leftover official leftover Make sure to configure the CLI to connect to a node in the relevant network leftover official leftover child leftover official leftover cards leftover official leftover $TOKEN_CHAIN_INDEX_CARD_ACCOUNT leftover official leftover $TOKEN_CHAIN_ACCOUNT_URL leftover official leftover Learn how end users can create accounts on the Token Chain to bridge assets or hold newly minted tokens leftover official leftover $TOKEN_CHAIN_INDEX_CARD_PROPOSAL leftover official leftover $TOKEN_CHAIN_PROPOSAL_URL leftover official leftover Guide for developers and project teams to propose new FT4 tokens and optional bridges on the Token Chain leftover official leftover sidebar leftover official leftover $TOKEN_CHAIN_INDEX_TITLE leftover official leftover $TOKEN_CHAIN_INDEX_CARD_ACCOUNT leftover official leftover $TOKEN_CHAIN_INDEX_SIDEBAR_PROPOSAL leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover user-account-creation leftover official leftover chr leftover official leftover tx leftover official leftover transfer leftover official leftover propose_token leftover official leftover mint leftover official leftover --evm-auth leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover printed leftover official leftover sample leftover official leftover admin leftover official leftover pubkey leftover official leftover 03028A31.
        Leftover official BUILD token-chain/user-account-creation (leftover official $TOKEN_CHAIN_ACCOUNT_INDEX_URL leftover official 307 leftover official $TOKEN_CHAIN_ACCOUNT_INDEX_URL_SLASH leftover official 200 leftover official $TOKEN_CHAIN_ACCOUNT_INDEX_TITLE): leftover official leftover intro leftover official leftover End users interacting with dapps on Chromia will need an account on the Token Chain to bridge assets or hold newly minted tokens leftover official leftover Below is the streamlined flow leftover official leftover Funding your account leftover official leftover Transfer CHR from the Economy Chain to your desired Token Chain address leftover official leftover If you don't have an Economy Chain account leftover official leftover Transfer CHR from another Token Chain account leftover official leftover Use a cross-chain transfer leftover official leftover small fee applies leftover official leftover Creating your account leftover official leftover Use the Vault UI or the Chromia CLI leftover official leftover This flow is for end users leftover official leftover if you're proposing or managing pools as a project leftover official leftover follow the developer flow in the Developer Token Proposal guide leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover chr leftover official leftover tx leftover official leftover transfer leftover official leftover hard skip leftover official leftover account leftover official leftover creation leftover official leftover write leftover official leftover recipes leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official BUILD token-chain/developer-token-proposal (leftover official $TOKEN_CHAIN_PROPOSAL_INDEX_URL leftover official 307 leftover official $TOKEN_CHAIN_PROPOSAL_INDEX_URL_SLASH leftover official 200 leftover official $TOKEN_CHAIN_PROPOSAL_INDEX_TITLE): leftover official leftover intro leftover official leftover Developers and project teams can propose new FT4 tokens and optional bridges on the Token Chain leftover official leftover The process includes fee payment, parameter configuration, and (optionally) bridge deployment leftover official leftover Anyone can submit a token proposal leftover official leftover A listing fee is required upfront and refunded if the proposal is rejected leftover official leftover 25% burned leftover official leftover 25% to Chromia Foundation leftover official leftover 50% directed to the project's resource pool leftover official leftover Current leftover official leftover fees leftover official leftover 100 CHR leftover official leftover token leftover official leftover proposals leftover official leftover 100 CHR leftover official leftover bridge leftover official leftover proposals leftover official leftover verify leftover official leftover get_token_chain_constants leftover official leftover proposal leftover official leftover Token name leftover official leftover Token symbol leftover official leftover Token decimals leftover official leftover Token icon leftover official leftover URL leftover official leftover Minting policies leftover official leftover Account creation blockchains leftover official leftover Blockchains RIDs leftover official leftover query leftover official leftover get_token_chain_constants leftover official leftover get_proposals_by_proposer leftover official leftover proposer leftover official leftover ft4.get_assets_by_name leftover official leftover get_evm_transaction_submitter_chain_rid leftover official leftover get_all_bridges leftover official leftover Token Chain leftover official leftover validator leftover official leftover contract leftover official leftover bridge_configuration leftover official leftover eif.hbridge.bridge_mode leftover official leftover ras_token_iccf leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover propose_token leftover official leftover propose_token_bridge leftover official leftover mint_token leftover official leftover --evm-auth leftover official leftover write leftover official leftover recipes leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover printed leftover official leftover sample leftover official leftover admin leftover official leftover pubkey leftover official leftover 03028A31 leftover official leftover WRITE leftover official leftover SKIP leftover official leftover keygen.
        Leftover official developer-token-proposal (200 with trailing slash): $TOKEN_CHAIN_PROPOSAL_URL. Bare $TOKEN_CHAIN_PROPOSAL_REDIRECT is 307.
        Related official architecture (get-started, 200): $TOKEN_CHAIN_ARCH_URL
        Leftover official leftover GET-STARTED get-started/about/architecture/chains INDEX (leftover official $GET_STARTED_SYSTEM_CHAINS_INDEX_URL leftover official 307 leftover official $GET_STARTED_SYSTEM_CHAINS_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_SYSTEM_CHAINS_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover sign leftover official leftover recipe leftover official leftover no leftover official leftover keygen.
        Leftover official leftover GET-STARTED get-started/about/architecture/chains/directory-chain INDEX (leftover official $GET_STARTED_DIRECTORY_CHAIN_INDEX_URL leftover official 307 leftover official $GET_STARTED_DIRECTORY_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_DIRECTORY_CHAIN_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover sign leftover official leftover recipe leftover official leftover no leftover official leftover keygen.
        Leftover official leftover GET-STARTED get-started/about/providers INDEX (leftover official $GET_STARTED_PROVIDERS_INDEX_URL leftover official 307 leftover official $GET_STARTED_PROVIDERS_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_PROVIDERS_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover GET-STARTED get-started/about/architecture/chains/economy-chain INDEX (leftover official $GET_STARTED_ECONOMY_CHAIN_INDEX_URL leftover official 307 leftover official $GET_STARTED_ECONOMY_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_ECONOMY_CHAIN_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover GET-STARTED get-started/about/architecture/chains/cluster-anchoring-chain INDEX (leftover official $GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_URL leftover official 307 leftover official $GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover GET-STARTED get-started/use-cases/cross-chain INDEX (leftover official $GET_STARTED_CROSS_CHAIN_INDEX_URL leftover official 307 leftover official $GET_STARTED_CROSS_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_CROSS_CHAIN_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover GET-STARTED get-started/about/architecture/chains/token-chain INDEX (leftover official $GET_STARTED_TOKEN_CHAIN_INDEX_URL leftover official 307 leftover official $GET_STARTED_TOKEN_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_TOKEN_CHAIN_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover LEARN courses/book-review/input-verification/structure INDEX (leftover official $LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/iccf-course/introduction INDEX (leftover official $LEARN_ICCF_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_ICCF_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICCF_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/my-news-feed/module-one/input-verification/tests INDEX (leftover official $LEARN_NEWS_UNIT_TESTS_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_UNIT_TESTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_UNIT_TESTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/tic-tac-toe/module-one/project-structure INDEX (leftover official $LEARN_TTT_PROJECT_STRUCTURE_INDEX_URL leftover official 301 leftover official $LEARN_TTT_PROJECT_STRUCTURE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_PROJECT_STRUCTURE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/vector-db-movie-demo/setup/configure.rell INDEX (leftover official $LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_URL leftover official 301 leftover official $LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/monetize-dapp/setup INDEX (leftover official $LEARN_MONETIZE_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_MONETIZE_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MONETIZE_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/zero-knowledge-proof/introduction INDEX (leftover official $LEARN_ZK_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_ZK_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ZK_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/relationships-course/introduction INDEX (leftover official $LEARN_RELATIONSHIPS_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_RELATIONSHIPS_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_RELATIONSHIPS_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/web3-for-web2-devs/security INDEX (leftover official $LEARN_WEB3_SECURITY_INDEX_URL leftover official 301 leftover official $LEARN_WEB3_SECURITY_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_WEB3_SECURITY_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/Web3 INDEX (leftover official $LEARN_TAGS_WEB3_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_WEB3_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_WEB3_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official leftover source tree: $TOKEN_CHAIN_SOURCE
        Leftover official read-only query shapes use docs placeholders only — do not invent 64-hex.
        Official leftover argument name for get_proposals_by_proposer is proposer.
        Official leftover ft4.get_assets_by_name uses name= plus page_size=null page_cursor=null.
        Official leftover get_evm_transaction_submitter_chain_rid is a Directory Chain query.
        Official leftover get_all_bridges is on the EVM transaction submitter chain; identify the Token Chain validator contract in the response.
        Leftover official listing fees printed as 100 CHR token + 100 CHR bridge — verify with get_token_chain_constants. Do not invent other fee numbers.
        Leftover official fee split: 25% burn / 25% Foundation / 50% project pool (burn enabled in a future release).
        Leftover official proposal fields: name, symbol, decimals, icon URL, minting policies, account-creation blockchain RIDs.
        Leftover official printed bridge_configuration includes eif.hbridge.bridge_mode without a field name — quote as printed; do not invent a field name.
        Never emit leftover printed sample admin pubkey 03028A31.
        See chromia_language_clients_help, chromia_ft4_queries_help, and chromia_vector_search_help.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("tool", TOOL_NAME)
        put("docs", HUB_URL)
        put("memo", MEMO_URL)
        put("memo_index_docs", MEMO_INDEX_URL)
        put("memo_index_url_slash", MEMO_INDEX_URL_SLASH)
        put("memo_index_title", MEMO_INDEX_TITLE)
        put("memo_guide_index_docs", MEMO_GUIDE_INDEX_URL)
        put("memo_guide_index_url_slash", MEMO_GUIDE_INDEX_URL_SLASH)
        put("memo_guide_index_title", MEMO_GUIDE_INDEX_TITLE)
        put("exchange", EXCHANGE_URL)
        put("exchange_index_url", EXCHANGE_INDEX_URL)
        put("exchange_index_url_slash", EXCHANGE_INDEX_URL_SLASH)
        put("exchange_index_title", EXCHANGE_INDEX_TITLE)
        put("exchange_guide_index_docs", EXCHANGE_GUIDE_INDEX_URL)
        put("exchange_guide_index_url_slash", EXCHANGE_GUIDE_INDEX_URL_SLASH)
        put("exchange_guide_index_title", EXCHANGE_GUIDE_INDEX_TITLE)
        put("exchange_index_card_overview", EXCHANGE_INDEX_CARD_OVERVIEW)
        put("exchange_index_card_step1", EXCHANGE_INDEX_CARD_STEP1)
        put("exchange_index_card_step2", EXCHANGE_INDEX_CARD_STEP2)
        put("exchange_index_card_step3", EXCHANGE_INDEX_CARD_STEP3)
        put("exchange_index_card_step4", EXCHANGE_INDEX_CARD_STEP4)
        put("exchange_index_card_resources", EXCHANGE_INDEX_CARD_RESOURCES)
        put("exchange_overview_index_docs", EXCHANGE_OVERVIEW_INDEX_URL)
        put("exchange_overview_index_url_slash", EXCHANGE_OVERVIEW_INDEX_URL_SLASH)
        put("exchange_overview_index_title", EXCHANGE_OVERVIEW_INDEX_TITLE)
        put("exchange_step1_index_docs", EXCHANGE_STEP1_INDEX_URL)
        put("exchange_step1_index_url_slash", EXCHANGE_STEP1_INDEX_URL_SLASH)
        put("exchange_step1_index_title", EXCHANGE_STEP1_INDEX_TITLE)
        put("exchange_step_1_index_docs", EXCHANGE_STEP_1_INDEX_URL)
        put("exchange_step_1_index_url_slash", EXCHANGE_STEP_1_INDEX_URL_SLASH)
        put("exchange_step_1_index_title", EXCHANGE_STEP_1_INDEX_TITLE)
        put("exchange_step2_index_docs", EXCHANGE_STEP2_INDEX_URL)
        put("exchange_step2_index_url_slash", EXCHANGE_STEP2_INDEX_URL_SLASH)
        put("exchange_step2_index_title", EXCHANGE_STEP2_INDEX_TITLE)
        put("exchange_step3_index_docs", EXCHANGE_STEP3_INDEX_URL)
        put("exchange_step3_index_url_slash", EXCHANGE_STEP3_INDEX_URL_SLASH)
        put("exchange_step3_index_title", EXCHANGE_STEP3_INDEX_TITLE)
        put("exchange_step4_index_docs", EXCHANGE_STEP4_INDEX_URL)
        put("exchange_step4_index_url_slash", EXCHANGE_STEP4_INDEX_URL_SLASH)
        put("exchange_step4_index_title", EXCHANGE_STEP4_INDEX_TITLE)
        put("exchange_resources_index_docs", EXCHANGE_RESOURCES_INDEX_URL)
        put("exchange_resources_index_url_slash", EXCHANGE_RESOURCES_INDEX_URL_SLASH)
        put("exchange_resources_index_title", EXCHANGE_RESOURCES_INDEX_TITLE)
        put("exchange_additional_resources_index_docs", EXCHANGE_ADDITIONAL_RESOURCES_INDEX_URL)
        put("exchange_additional_resources_index_url_slash", EXCHANGE_ADDITIONAL_RESOURCES_INDEX_URL_SLASH)
        put("exchange_additional_resources_index_title", EXCHANGE_ADDITIONAL_RESOURCES_INDEX_TITLE)
        put("integrations_index_url", INTEGRATIONS_INDEX_URL)
        put("integrations_index_url_slash", INTEGRATIONS_INDEX_URL_SLASH)
        put("integrations_index_title", INTEGRATIONS_INDEX_TITLE)
        put("integrations_index_card_exchange", INTEGRATIONS_INDEX_CARD_EXCHANGE)
        put("integrations_index_card_memo", INTEGRATIONS_INDEX_CARD_MEMO)
        put("ft4_client", FT4_CLIENT_URL)
        put("token_chain_docs", TOKEN_CHAIN_URL)
        put("token_chain_index_url", TOKEN_CHAIN_INDEX_URL)
        put("token_chain_index_url_slash", TOKEN_CHAIN_INDEX_URL_SLASH)
        put("token_chain_index_title", TOKEN_CHAIN_INDEX_TITLE)
        put("token_chain_index_card_account", TOKEN_CHAIN_INDEX_CARD_ACCOUNT)
        put("token_chain_index_card_proposal", TOKEN_CHAIN_INDEX_CARD_PROPOSAL)
        put("token_chain_index_sidebar_proposal", TOKEN_CHAIN_INDEX_SIDEBAR_PROPOSAL)
        put("token_chain_account_docs", TOKEN_CHAIN_ACCOUNT_URL)
        put("token_chain_account_index_docs", TOKEN_CHAIN_ACCOUNT_INDEX_URL)
        put("token_chain_account_index_url_slash", TOKEN_CHAIN_ACCOUNT_INDEX_URL_SLASH)
        put("token_chain_account_index_title", TOKEN_CHAIN_ACCOUNT_INDEX_TITLE)
        put("token_chain_proposal_docs", TOKEN_CHAIN_PROPOSAL_URL)
        put("token_chain_proposal_redirect", TOKEN_CHAIN_PROPOSAL_REDIRECT)
        put("token_chain_proposal_index_docs", TOKEN_CHAIN_PROPOSAL_INDEX_URL)
        put("token_chain_proposal_index_url_slash", TOKEN_CHAIN_PROPOSAL_INDEX_URL_SLASH)
        put("token_chain_proposal_index_title", TOKEN_CHAIN_PROPOSAL_INDEX_TITLE)
        put("token_chain_arch_docs", TOKEN_CHAIN_ARCH_URL)
        put("get_started_system_chains_index_docs", GET_STARTED_SYSTEM_CHAINS_INDEX_URL)
        put("get_started_system_chains_index_url_slash", GET_STARTED_SYSTEM_CHAINS_INDEX_URL_SLASH)
        put("get_started_system_chains_index_title", GET_STARTED_SYSTEM_CHAINS_INDEX_TITLE)
        put("get_started_directory_chain_index_docs", GET_STARTED_DIRECTORY_CHAIN_INDEX_URL)
        put("get_started_directory_chain_index_url_slash", GET_STARTED_DIRECTORY_CHAIN_INDEX_URL_SLASH)
        put("get_started_directory_chain_index_title", GET_STARTED_DIRECTORY_CHAIN_INDEX_TITLE)
        put("get_started_providers_index_docs", GET_STARTED_PROVIDERS_INDEX_URL)
        put("get_started_providers_index_url_slash", GET_STARTED_PROVIDERS_INDEX_URL_SLASH)
        put("get_started_providers_index_title", GET_STARTED_PROVIDERS_INDEX_TITLE)
        put("get_started_economy_chain_index_docs", GET_STARTED_ECONOMY_CHAIN_INDEX_URL)
        put("get_started_economy_chain_index_url_slash", GET_STARTED_ECONOMY_CHAIN_INDEX_URL_SLASH)
        put("get_started_economy_chain_index_title", GET_STARTED_ECONOMY_CHAIN_INDEX_TITLE)
        put("get_started_cluster_anchoring_chain_index_docs", GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_URL)
        put("get_started_cluster_anchoring_chain_index_url_slash", GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_URL_SLASH)
        put("get_started_cluster_anchoring_chain_index_title", GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_TITLE)
        put("get_started_cross_chain_index_docs", GET_STARTED_CROSS_CHAIN_INDEX_URL)
        put("get_started_cross_chain_index_url_slash", GET_STARTED_CROSS_CHAIN_INDEX_URL_SLASH)
        put("get_started_cross_chain_index_title", GET_STARTED_CROSS_CHAIN_INDEX_TITLE)
        put("get_started_token_chain_index_docs", GET_STARTED_TOKEN_CHAIN_INDEX_URL)
        put("get_started_token_chain_index_url_slash", GET_STARTED_TOKEN_CHAIN_INDEX_URL_SLASH)
        put("get_started_token_chain_index_title", GET_STARTED_TOKEN_CHAIN_INDEX_TITLE)
        put("token_chain_source", TOKEN_CHAIN_SOURCE)
        put("read_only", true)
        put("pages", buildJsonArray { childPages.forEach { add(JsonPrimitive(it)) } })
        put(
            "queries",
            buildJsonObject {
                put("does_account_require_memo", "does_account_require_memo(account_id: byte_array): boolean")
                put("get_token_chain_constants", tokenChainQueryShapes()[0])
                put("get_proposals_by_proposer", tokenChainQueryShapes()[1])
                put("ft4.get_assets_by_name", tokenChainQueryShapes()[2])
                put("get_evm_transaction_submitter_chain_rid", tokenChainQueryShapes()[3])
                put("get_all_bridges", tokenChainQueryShapes()[4])
            }
        )
        put(
            "packages",
            buildJsonObject {
                put("csharp_nuget", "official page does not print a package id")
            }
        )
        put("query_shapes", buildJsonArray { tokenChainQueryShapes().forEach { add(JsonPrimitive(it)) } })
        put("proposal_fields", buildJsonArray { proposalFields.forEach { add(JsonPrimitive(it)) } })
        put("leftover_fees", buildJsonArray { leftoverFees.forEach { add(JsonPrimitive(it)) } })
        put("bridge_configuration", bridgeConfigurationStruct())
        put("skipped_write_or_invented", buildJsonArray { skipped.forEach { add(JsonPrimitive(it)) } })
        put("ft4_queries_help", ChromiaFt4QueriesHelp.TOOL_NAME)
        put("language_clients_help", ChromiaLanguageClientsHelp.TOOL_NAME)
        put("vector_search_help", ChromiaVectorSearchHelp.TOOL_NAME)
        put("learn_book_review_input_structure_index_url_slash", LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_URL_SLASH)
        put("learn_book_review_input_structure_index_title", LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_TITLE)
        put("learn_iccf_intro_index_url_slash", LEARN_ICCF_INTRO_INDEX_URL_SLASH)
        put("learn_iccf_intro_index_title", LEARN_ICCF_INTRO_INDEX_TITLE)
        put("learn_news_unit_tests_index_url_slash", LEARN_NEWS_UNIT_TESTS_INDEX_URL_SLASH)
        put("learn_news_unit_tests_index_title", LEARN_NEWS_UNIT_TESTS_INDEX_TITLE)
        put("learn_ttt_project_structure_index_url_slash", LEARN_TTT_PROJECT_STRUCTURE_INDEX_URL_SLASH)
        put("learn_ttt_project_structure_index_title", LEARN_TTT_PROJECT_STRUCTURE_INDEX_TITLE)
        put("learn_vector_db_configure_rell_index_url_slash", LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_URL_SLASH)
        put("learn_vector_db_configure_rell_index_title", LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_TITLE)
        put("learn_monetize_setup_index_url_slash", LEARN_MONETIZE_SETUP_INDEX_URL_SLASH)
        put("learn_monetize_setup_index_title", LEARN_MONETIZE_SETUP_INDEX_TITLE)
        put("learn_zk_intro_index_url_slash", LEARN_ZK_INTRO_INDEX_URL_SLASH)
        put("learn_zk_intro_index_title", LEARN_ZK_INTRO_INDEX_TITLE)
        put("learn_relationships_intro_index_url_slash", LEARN_RELATIONSHIPS_INTRO_INDEX_URL_SLASH)
        put("learn_relationships_intro_index_title", LEARN_RELATIONSHIPS_INTRO_INDEX_TITLE)
        put("learn_web3_security_index_url_slash", LEARN_WEB3_SECURITY_INDEX_URL_SLASH)
        put("learn_web3_security_index_title", LEARN_WEB3_SECURITY_INDEX_TITLE)
        put("learn_tags_web3_index_url_slash", LEARN_TAGS_WEB3_INDEX_URL_SLASH)
        put("learn_tags_web3_index_title", LEARN_TAGS_WEB3_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover BUILD integrations/memo-guide leftovers encoded as MEMO_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/memo-guide leftovers encoded as MEMO_GUIDE_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide leftovers encoded as EXCHANGE_GUIDE_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide/overview leftovers encoded as EXCHANGE_OVERVIEW_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide/step-1-account leftovers encoded as EXCHANGE_STEP1_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide/step-1-account leftovers encoded as EXCHANGE_STEP_1_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide/step-2-access leftovers encoded as EXCHANGE_STEP2_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide/step-3-transaction leftovers encoded as EXCHANGE_STEP3_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide/step-4-non-existent-accounts leftovers encoded as EXCHANGE_STEP4_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide/additional-resources leftovers encoded as EXCHANGE_RESOURCES_INDEX_* (query-only).
// Leftover official leftover BUILD integrations/exchange-guide/additional-resources leftovers encoded as EXCHANGE_ADDITIONAL_RESOURCES_INDEX_* (query-only).
// Leftover official leftover BUILD token-chain/user-account-creation leftovers encoded as TOKEN_CHAIN_ACCOUNT_INDEX_* (query-only).
// Leftover official leftover BUILD token-chain/developer-token-proposal leftovers encoded as TOKEN_CHAIN_PROPOSAL_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/architecture/chains INDEX leftovers encoded as GET_STARTED_SYSTEM_CHAINS_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/architecture/chains/directory-chain INDEX leftovers encoded as GET_STARTED_DIRECTORY_CHAIN_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/providers INDEX leftovers encoded as GET_STARTED_PROVIDERS_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/architecture/chains/economy-chain INDEX leftovers encoded as GET_STARTED_ECONOMY_CHAIN_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/architecture/chains/cluster-anchoring-chain INDEX leftovers encoded as GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/use-cases/cross-chain INDEX leftovers encoded as GET_STARTED_CROSS_CHAIN_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/about/architecture/chains/token-chain INDEX leftovers encoded as GET_STARTED_TOKEN_CHAIN_INDEX_* (query-only).
// Leftover official leftover LEARN courses/book-review/input-verification/structure INDEX leftovers encoded as LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/iccf-course/introduction INDEX leftovers encoded as LEARN_ICCF_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/my-news-feed/module-one/input-verification/tests INDEX leftovers encoded as LEARN_NEWS_UNIT_TESTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-one/project-structure INDEX leftovers encoded as LEARN_TTT_PROJECT_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/vector-db-movie-demo/setup/configure.rell INDEX leftovers encoded as LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/monetize-dapp/setup INDEX leftovers encoded as LEARN_MONETIZE_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/zero-knowledge-proof/introduction INDEX leftovers encoded as LEARN_ZK_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/relationships-course/introduction INDEX leftovers encoded as LEARN_RELATIONSHIPS_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/web3-for-web2-devs/security INDEX leftovers encoded as LEARN_WEB3_SECURITY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/Web3 INDEX leftovers encoded as LEARN_TAGS_WEB3_INDEX_* (query-only HELP ONLY WRITE SKIP).
