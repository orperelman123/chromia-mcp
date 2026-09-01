package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official BUILD integrations hub (read-only facts).
 * No invented package ids. C# NuGet id is unpublished on the official C# page.
 * Skips exchange account-creation / transfer / memo write operations.
 * Official BUILD integrations index leftovers live here (query-only).
 * Official BUILD token-chain index leftovers also live here (query-only).
 * Official BUILD exchange-guide leftovers also live here (query-only).
 * Official BUILD integrations/exchange-guide index leftovers also live here (query-only).
 * Official BUILD integrations/memo-guide index leftovers also live here (query-only).
 * Official BUILD integrations/exchange-guide/overview index leftovers also live here (query-only).
 * Official BUILD integrations/exchange-guide/step-1-account index leftovers also live here (query-only).
 * Official BUILD integrations/exchange-guide/step-2-access index leftovers also live here (query-only).
 * Official BUILD integrations/exchange-guide/step-3-transaction index leftovers also live here (query-only).
 * Official BUILD integrations/exchange-guide/step-4-non-existent-accounts index leftovers also live here (query-only).
 * Official BUILD integrations/exchange-guide/additional-resources index leftovers also live here (query-only).
 * Official BUILD token-chain/user-account-creation index leftovers also live here (query-only).
 * Official BUILD token-chain/developer-token-proposal index leftovers also live here (query-only).
 * Official GET-STARTED get-started/about/architecture/chains INDEX leftovers also live here (query-only).
 * Official GET-STARTED get-started/about/architecture/chains/directory-chain INDEX leftovers also live here (query-only).
 * Official GET-STARTED get-started/about/providers INDEX leftovers also live here (query-only).
 * Official GET-STARTED get-started/about/architecture/chains/economy-chain INDEX leftovers also live here (query-only).
 * Official GET-STARTED get-started/about/architecture/chains/cluster-anchoring-chain INDEX leftovers also live here (query-only).
 * Official GET-STARTED get-started/use-cases/cross-chain INDEX leftovers also live here (query-only).
 * Official GET-STARTED get-started/about/architecture/chains/token-chain INDEX leftovers also live here (query-only).
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
        "exchange-guide/step-1-account account-creation / write recipes (WRITE SKIP)",
        "exchange-guide/step-2-access replica-node / keygen / write recipes (WRITE SKIP)",
        "exchange-guide step-3 deposit / withdrawal / transfer (write)",
        "exchange-guide/step-3-transaction deposit / withdrawal / write recipes (WRITE SKIP)",
        "exchange-guide/step-4-non-existent-accounts transfer / registerAccount / write recipes (WRITE SKIP)",
        "exchange-guide/additional-resources deposit / withdrawal / write recipes (WRITE SKIP)",
        "exchange-guide/overview deposit / withdrawal / write recipes (WRITE SKIP)",
        "enable_transfer_memo / disable_transfer_memo / memo(text) operations",
        "C# NuGet package id (official page does not print one)",
        "invented exchange / wallet / broker package ids",
        "token-chain user-account-creation chr tx transfer (hard skip)",
        "token-chain propose_token / propose_token_bridge / mint_token / --evm-auth (hard skip)",
        "invented TOKEN_CHAIN_RID / ECONOMY_CHAIN_RID / EVM_TRANSACTION_SUBMITTER_CHAIN_RID / 64-hex",
        "ras_token_iccf write docs",
        "printed sample admin pubkey 03028A31"
    )

    val proposalFields = listOf(
        "Token name",
        "Token symbol",
        "Token decimals",
        "Token icon  # URL pointing to an image file",
        "Minting policies  # official: who can mint, how many, how often",
        "Account creation blockchains  # official: blockchain RIDs allowed to create accounts"
    )

    val fees = listOf(
        "100 CHR token proposal  # printed listing fee; verify with get_token_chain_constants",
        "100 CHR bridge proposal  # printed listing fee; verify with get_token_chain_constants",
        "25% burn / 25% Foundation / 50% project pool  # default split; burn enabled in a future release",
        "listing fee refunded if the proposal is rejected  # official"
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
        "chr query --blockchain-rid " + "\${TOKEN_CHAIN_RID} get_token_chain_constants  # official; placeholder only",
        "chr query --blockchain-rid " + "\${TOKEN_CHAIN_RID} get_proposals_by_proposer proposer=\${YOUR_ACCOUNT_ID}  # argument name proposer",
        "chr query --blockchain-rid " + "\${TOKEN_CHAIN_RID} ft4.get_assets_by_name name=\${YOUR_TOKEN_NAME} page_size=null page_cursor=null",
        "chr query --blockchain-rid " + "\${DIRECTORY_CHAIN_RID} get_evm_transaction_submitter_chain_rid  # Directory Chain query",
        "chr query --blockchain-rid " + "\${EVM_TRANSACTION_SUBMITTER_CHAIN_RID} get_all_bridges  # official; identify Token Chain validator contract"
    )


    fun notes(): String = """
        Official Chromia BUILD integrations hub. CLI $CLI_SERIES. Java 21+, Postgres 16+.
        Hub: $HUB_URL
        Official BUILD integrations ($INTEGRATIONS_INDEX_URL 307 $INTEGRATIONS_INDEX_URL_SLASH 200 $INTEGRATIONS_INDEX_TITLE): intro Chromia integrations provide comprehensive guides and tools for seamlessly connecting external systems, exchanges, and applications with the Chromia blockchain These integration resources help developers and technical teams efficiently implement secure, scalable solutions child cards $INTEGRATIONS_INDEX_CARD_EXCHANGE $EXCHANGE_URL Complete step-by-step guide for exchanges to integrate with Chromia, covering account creation, transaction handling, monitoring, and advanced features like multisig operations $INTEGRATIONS_INDEX_CARD_MEMO $MEMO_URL Learn how to implement memo functionality for FT4 transfers, enabling textual annotations for transactions that are essential for exchange integrations and shared account management sidebar Exchange guide Memo integration guide skip signed txs no sample keys no invented 64-hex exchange account-creation write deposit withdrawal write.
        Official BUILD exchange-guide ($EXCHANGE_INDEX_URL 307 $EXCHANGE_INDEX_URL_SLASH 200 $EXCHANGE_INDEX_TITLE): intro This guide is designed for technical teams at exchanges that have decided to list Chromia and are focused on completing the integration efficiently The guide provides a step-by-step process to assist exchanges in integrating with Chromia child cards $EXCHANGE_INDEX_CARD_OVERVIEW $EXCHANGE_OVERVIEW_URL This section provides a high-level introduction to Chromia, its architecture, and key features to help exchanges understand the platform before starting integration $EXCHANGE_INDEX_CARD_STEP1 $EXCHANGE_STEP1_URL Learn how to create and manage Chromia accounts $EXCHANGE_INDEX_CARD_STEP2 $EXCHANGE_STEP2_URL Instructions on connecting to Chromia's Economy Chain using public system nodes or by setting up a private replica node for enhanced control $EXCHANGE_INDEX_CARD_STEP3 $EXCHANGE_STEP3_URL Detailed guidance on handling deposits, withdrawals, and monitoring transactions using Chromia's FT4 library $EXCHANGE_INDEX_CARD_STEP4 $EXCHANGE_STEP4_URL Explore how to send assets to accounts that have not yet been activated and ensure users can access their funds securely $EXCHANGE_INDEX_CARD_RESOURCES $EXCHANGE_RESOURCES_URL Access extra tools, documentation, and links for testing, development, and troubleshooting to streamline integration skip signed txs step-1 account creation write step-3 deposit withdrawal write no sample keys no invented 64-hex.
        Official BUILD integrations/exchange-guide ($EXCHANGE_GUIDE_INDEX_URL 307 $EXCHANGE_GUIDE_INDEX_URL_SLASH 200 $EXCHANGE_GUIDE_INDEX_TITLE): Query-only WRITE SKIP HELP ONLY no signed txs no sample keys no invented 64-hex no keygen skip this signs.
        Official BUILD integrations/exchange-guide/overview ($EXCHANGE_OVERVIEW_INDEX_URL 307 $EXCHANGE_OVERVIEW_INDEX_URL_SLASH 200 $EXCHANGE_OVERVIEW_INDEX_TITLE): intro Chromia is a Layer-1 blockchain platform designed to deliver high performance and scalability for decentralized applications Its multi-chain architecture ensures that each dapp operates independently, avoiding network congestion and maintaining optimal performance even during peak usage Key features of Chromia Gas-Free Transactions Users can interact with dapps without paying transaction fees On-Chain Data Storage Chromia allows large-scale data to be stored directly on the blockchain Relational Data Model By integrating PostgreSQL Scalability Each dapp operates on its dedicated chain Key components Economy Chain the primary chain for integration Postchain eBFT Enhanced Byzantine Fault Tolerance Rell FT4 protocol Chromia Vault Dapp clusters skip signed txs deposit withdrawal write recipes no sample keys no invented 64-hex WRITE SKIP keygen.
        Official BUILD integrations/exchange-guide/step-1-account ($EXCHANGE_STEP1_INDEX_URL 307 $EXCHANGE_STEP1_INDEX_URL_SLASH 200 $EXCHANGE_STEP1_INDEX_TITLE): intro Creating an account on Chromia's Economy Chain is a crucial first step for integrating with the network An account acts as a unique identifier that enables interaction with decentralized applications on the Chromia blockchain It is required for executing transactions, holding and managing CHR tokens, and participating in staking and other ecosystem functionalities For exchange integration, you must create a dedicated account on the Economy Chain This account will serve as the foundation for all exchange-related operations Bridging CHR from an EVM-compatible blockchain Staking CHR Paying a fee via internal transfer 10 CHR printed one-time account creation fee skip signed txs account creation write recipes bridging staking internal transfer write no sample keys no invented 64-hex WRITE SKIP keygen.
        Official BUILD integrations/exchange-guide/step-1-account ($EXCHANGE_STEP_1_INDEX_URL 307 $EXCHANGE_STEP_1_INDEX_URL_SLASH 200 $EXCHANGE_STEP_1_INDEX_TITLE): Query-only WRITE SKIP HELP ONLY no signed txs no sample keys no invented 64-hex no keygen skip this signs.
        Official BUILD integrations/exchange-guide/step-2-access ($EXCHANGE_STEP2_INDEX_URL 307 $EXCHANGE_STEP2_INDEX_URL_SLASH 200 $EXCHANGE_STEP2_INDEX_TITLE): intro To access Chromia's Economy Chain, you have two primary options Public access Use publicly available system nodes for a quick and simple connection without hosting your own infrastructure Private node Set up and run your own replica node for greater control, security, and reliability These options cater to different levels of control, reliability, and infrastructure needs Use public system nodes For a quick and easy connection you can use any of the publicly available system nodes These nodes provide reliable access to the Economy Chain without the need to host your own infrastructure Economy Chain BRID public nodes Chromia explorer proceed directly to Step 3 Set up your own replica node For greater control and security setting up a private replica node is the recommended approach Hosting your own node provides better reliability and flexibility skip signed txs replica node register write recipes pmc keygen provider keypair docker write no sample keys no invented 64-hex WRITE SKIP keygen.
        Official BUILD integrations/exchange-guide/step-3-transaction ($EXCHANGE_STEP3_INDEX_URL 307 $EXCHANGE_STEP3_INDEX_URL_SLASH 200 $EXCHANGE_STEP3_INDEX_TITLE): intro Handling deposits, withdrawals, and monitoring transactions are essential components of integrating with Chromia's Economy Chain These operations are streamlined using the FT4 library, which provides a comprehensive toolkit for interacting with the Chromia ecosystem The FT4 library is designed to help developers build real-world applications within Chromia Account creation and access management Asset management issuance allocation transfers tracing Using the FT4 library manage deposits and withdrawals monitoring transaction activities Additional resources Client setup $FT4_CLIENT_URL Postchain JS TS client FT4 library overview FT4 library GitLab Memo integration guide $MEMO_URL Required Dependencies @chromia/ft4@1.1.1 postchain-client@1.22.0 Node.js 16 query createClient createConnection getAccountById getTransferHistory TransferHistoryType Received 200 cap pagination nextCursor getTransferDetails getTransactionInfo getTransactionStatus getBlockInfo getBalances getBalanceByAssetId skip signed txs deposit withdrawal transfer write recipes key store offline transaction write account registration write no sample keys no invented 64-hex WRITE SKIP keygen.
        Official BUILD integrations/exchange-guide/step-4-non-existent-accounts ($EXCHANGE_STEP4_INDEX_URL 307 $EXCHANGE_STEP4_INDEX_URL_SLASH 200 $EXCHANGE_STEP4_INDEX_TITLE): intro Exchanges can send assets to users who do not yet have a Chromia account by transferring funds to an unregistered account This process allows users to activate their accounts through the Chromia Vault The exchange should first verify if the user has an existing Chromia account If the account exists You can send any amount of CHR If the account does not exist Notify the user that they need to send at least 10 CHR as this is the required fee for account creation Once the transfer is complete users can visit the Chromia Vault to activate their accounts skip signed txs transfer registerAccount write recipes key store account registration write no sample keys no invented 64-hex WRITE SKIP keygen.
        Official BUILD integrations/exchange-guide/step-4-non-existent-accounts ($EXCHANGE_STEP4_INDEX_URL 307 $EXCHANGE_STEP4_INDEX_URL_SLASH 200 $EXCHANGE_STEP4_INDEX_TITLE): Query-only WRITE SKIP HELP ONLY no signed txs no sample keys no invented 64-hex no keygen skip this signs.
        Official BUILD integrations/exchange-guide/additional-resources ($EXCHANGE_RESOURCES_INDEX_URL 307 $EXCHANGE_RESOURCES_INDEX_URL_SLASH 200 $EXCHANGE_RESOURCES_INDEX_TITLE): intro Official website Learn Chromia platform Chromia Explorer Chromia Vault Staking Testnet Explorer Use the Testnet Explorer to monitor transactions and chain activity Testnet staking Test the staking functionality using a dedicated interface Testnet Vault Use Chromia Vault to test wallet features and bridging Testnet faucet Use the tCHR Faucet to obtain test tokens skip signed txs deposit withdrawal write recipes no sample keys no invented 64-hex WRITE SKIP keygen.
        Official BUILD integrations/exchange-guide/additional-resources ($EXCHANGE_ADDITIONAL_RESOURCES_INDEX_URL 307 $EXCHANGE_ADDITIONAL_RESOURCES_INDEX_URL_SLASH 200 $EXCHANGE_ADDITIONAL_RESOURCES_INDEX_TITLE): Query-only WRITE SKIP HELP ONLY no signed txs no sample keys no invented 64-hex no keygen skip this signs.
        Official BUILD integrations/memo-guide ($MEMO_INDEX_URL 307 $MEMO_INDEX_URL_SLASH 200 $MEMO_INDEX_TITLE): intro The memo feature adds an optional memo requirement for FT4 transfers, ensuring that certain accounts mandate memos for all incoming transfers This feature is useful for attaching textual annotations to blockchain transactions, making it especially beneficial for applications requiring transaction context and shared account management query does_account_require_memo account_id byte_array boolean Checks if an FT4 account enforces memo requirements for incoming transfers Enhanced transaction tracking Operational efficiency Risk mitigation Exchange integrations Shared account management Payment processing Audit compliance Cross-border transfers memo text 1 50 characters skip signed txs enable_transfer_memo disable_transfer_memo memo write transfer recipes no sample keys no invented 64-hex WRITE SKIP keygen.
        Official BUILD integrations/memo-guide ($MEMO_GUIDE_INDEX_URL 307 $MEMO_GUIDE_INDEX_URL_SLASH 200 $MEMO_GUIDE_INDEX_TITLE): Query-only WRITE SKIP HELP ONLY no signed txs no sample keys no invented 64-hex no keygen skip this signs.
        Official child pages are listed. This tool quotes read-only facts only.
        Hub facts: exchanges list CHR and other FT4 assets; memo-enabled transfers;
        private replica nodes; Economy Chain for asset management; FT4 client setup at $FT4_CLIENT_URL.
        Memo query only ($MEMO_URL): does_account_require_memo(account_id: byte_array): boolean.
        Official memo text constraint (on the write op, listed so callers know why the query exists): 1 to 50 characters.
        Skipped: account registration, deposits, withdrawals, enable/disable memo, memo+transfer, invented package ids.
        C# ($CSHARP_URL): official page does not print a NuGet package id — do not invent one.
        Official token-chain hub (200): $TOKEN_CHAIN_URL — index only.
        Official BUILD token-chain ($TOKEN_CHAIN_INDEX_URL 307 $TOKEN_CHAIN_INDEX_URL_SLASH 200 $TOKEN_CHAIN_INDEX_TITLE): intro The Token Chain provides functionality for token management, account creation, and bridge setup This section covers the practical aspects of working with the Token Chain Install the Chromia CLI by following the instructions in Install and configure Chromia CLI Make sure to configure the CLI to connect to a node in the relevant network child cards $TOKEN_CHAIN_INDEX_CARD_ACCOUNT $TOKEN_CHAIN_ACCOUNT_URL Learn how end users can create accounts on the Token Chain to bridge assets or hold newly minted tokens $TOKEN_CHAIN_INDEX_CARD_PROPOSAL $TOKEN_CHAIN_PROPOSAL_URL Guide for developers and project teams to propose new FT4 tokens and optional bridges on the Token Chain sidebar $TOKEN_CHAIN_INDEX_TITLE $TOKEN_CHAIN_INDEX_CARD_ACCOUNT $TOKEN_CHAIN_INDEX_SIDEBAR_PROPOSAL skip signed txs user-account-creation chr tx transfer propose_token mint --evm-auth no sample keys no invented 64-hex printed sample admin pubkey 03028A31.
        Official BUILD token-chain/user-account-creation ($TOKEN_CHAIN_ACCOUNT_INDEX_URL 307 $TOKEN_CHAIN_ACCOUNT_INDEX_URL_SLASH 200 $TOKEN_CHAIN_ACCOUNT_INDEX_TITLE): intro End users interacting with dapps on Chromia will need an account on the Token Chain to bridge assets or hold newly minted tokens Below is the streamlined flow Funding your account Transfer CHR from the Economy Chain to your desired Token Chain address If you don't have an Economy Chain account Transfer CHR from another Token Chain account Use a cross-chain transfer small fee applies Creating your account Use the Vault UI or the Chromia CLI This flow is for end users if you're proposing or managing pools as a project follow the developer flow in the Developer Token Proposal guide skip signed txs chr tx transfer hard skip account creation write recipes no sample keys no invented 64-hex WRITE SKIP keygen.
        Official BUILD token-chain/developer-token-proposal ($TOKEN_CHAIN_PROPOSAL_INDEX_URL 307 $TOKEN_CHAIN_PROPOSAL_INDEX_URL_SLASH 200 $TOKEN_CHAIN_PROPOSAL_INDEX_TITLE): intro Developers and project teams can propose new FT4 tokens and optional bridges on the Token Chain The process includes fee payment, parameter configuration, and (optionally) bridge deployment Anyone can submit a token proposal A listing fee is required upfront and refunded if the proposal is rejected 25% burned 25% to Chromia Foundation 50% directed to the project's resource pool Current fees 100 CHR token proposals 100 CHR bridge proposals verify get_token_chain_constants proposal Token name Token symbol Token decimals Token icon URL Minting policies Account creation blockchains Blockchains RIDs query get_token_chain_constants get_proposals_by_proposer proposer ft4.get_assets_by_name get_evm_transaction_submitter_chain_rid get_all_bridges Token Chain validator contract bridge_configuration eif.hbridge.bridge_mode ras_token_iccf skip signed txs propose_token propose_token_bridge mint_token --evm-auth write recipes no sample keys no invented 64-hex printed sample admin pubkey 03028A31 WRITE SKIP keygen.
        Official developer-token-proposal (200 with trailing slash): $TOKEN_CHAIN_PROPOSAL_URL. Bare $TOKEN_CHAIN_PROPOSAL_REDIRECT is 307.
        Related official architecture (get-started, 200): $TOKEN_CHAIN_ARCH_URL
        Official GET-STARTED get-started/about/architecture/chains INDEX ($GET_STARTED_SYSTEM_CHAINS_INDEX_URL 307 $GET_STARTED_SYSTEM_CHAINS_INDEX_URL_SLASH 200 $GET_STARTED_SYSTEM_CHAINS_INDEX_TITLE): slash title HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no sign recipe no keygen.
        Official GET-STARTED get-started/about/architecture/chains/directory-chain INDEX ($GET_STARTED_DIRECTORY_CHAIN_INDEX_URL 307 $GET_STARTED_DIRECTORY_CHAIN_INDEX_URL_SLASH 200 $GET_STARTED_DIRECTORY_CHAIN_INDEX_TITLE): slash title HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no sign recipe no keygen.
        Official GET-STARTED get-started/about/providers INDEX ($GET_STARTED_PROVIDERS_INDEX_URL 307 $GET_STARTED_PROVIDERS_INDEX_URL_SLASH 200 $GET_STARTED_PROVIDERS_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe.
        Official GET-STARTED get-started/about/architecture/chains/economy-chain INDEX ($GET_STARTED_ECONOMY_CHAIN_INDEX_URL 307 $GET_STARTED_ECONOMY_CHAIN_INDEX_URL_SLASH 200 $GET_STARTED_ECONOMY_CHAIN_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe.
        Official GET-STARTED get-started/about/architecture/chains/cluster-anchoring-chain INDEX ($GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_URL 307 $GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_URL_SLASH 200 $GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe.
        Official GET-STARTED get-started/use-cases/cross-chain INDEX ($GET_STARTED_CROSS_CHAIN_INDEX_URL 307 $GET_STARTED_CROSS_CHAIN_INDEX_URL_SLASH 200 $GET_STARTED_CROSS_CHAIN_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe.
        Official GET-STARTED get-started/about/architecture/chains/token-chain INDEX ($GET_STARTED_TOKEN_CHAIN_INDEX_URL 307 $GET_STARTED_TOKEN_CHAIN_INDEX_URL_SLASH 200 $GET_STARTED_TOKEN_CHAIN_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe.
        Official LEARN courses/book-review/input-verification/structure INDEX ($LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_URL 301 $LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/iccf-course/introduction INDEX ($LEARN_ICCF_INTRO_INDEX_URL 301 $LEARN_ICCF_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_ICCF_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/input-verification/tests INDEX ($LEARN_NEWS_UNIT_TESTS_INDEX_URL 301 $LEARN_NEWS_UNIT_TESTS_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_UNIT_TESTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/tic-tac-toe/module-one/project-structure INDEX ($LEARN_TTT_PROJECT_STRUCTURE_INDEX_URL 301 $LEARN_TTT_PROJECT_STRUCTURE_INDEX_URL_SLASH 200 H1 $LEARN_TTT_PROJECT_STRUCTURE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/vector-db-movie-demo/setup/configure.rell INDEX ($LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_URL 301 $LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_URL_SLASH 200 H1 $LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/monetize-dapp/setup INDEX ($LEARN_MONETIZE_SETUP_INDEX_URL 301 $LEARN_MONETIZE_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_MONETIZE_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/zero-knowledge-proof/introduction INDEX ($LEARN_ZK_INTRO_INDEX_URL 301 $LEARN_ZK_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_ZK_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/relationships-course/introduction INDEX ($LEARN_RELATIONSHIPS_INTRO_INDEX_URL 301 $LEARN_RELATIONSHIPS_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_RELATIONSHIPS_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/web3-for-web2-devs/security INDEX ($LEARN_WEB3_SECURITY_INDEX_URL 301 $LEARN_WEB3_SECURITY_INDEX_URL_SLASH 200 H1 $LEARN_WEB3_SECURITY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/Web3 INDEX ($LEARN_TAGS_WEB3_INDEX_URL 301 $LEARN_TAGS_WEB3_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_WEB3_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official source tree: $TOKEN_CHAIN_SOURCE
        Official read-only query shapes use docs placeholders only — do not invent 64-hex.
        Official argument name for get_proposals_by_proposer is proposer.
        Official ft4.get_assets_by_name uses name= plus page_size=null page_cursor=null.
        Official get_evm_transaction_submitter_chain_rid is a Directory Chain query.
        Official get_all_bridges is on the EVM transaction submitter chain; identify the Token Chain validator contract in the response.
        Official listing fees printed as 100 CHR token + 100 CHR bridge — verify with get_token_chain_constants. Do not invent other fee numbers.
        Official fee split: 25% burn / 25% Foundation / 50% project pool (burn enabled in a future release).
        Official proposal fields: name, symbol, decimals, icon URL, minting policies, account-creation blockchain RIDs.
        Official printed bridge_configuration includes eif.hbridge.bridge_mode without a field name — quote as printed; do not invent a field name.
        Never emit printed sample admin pubkey 03028A31.
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
        put("fees", buildJsonArray { fees.forEach { add(JsonPrimitive(it)) } })
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
// Official BUILD integrations/memo-guide leftovers encoded as MEMO_INDEX_* (query-only).
// Official BUILD integrations/memo-guide leftovers encoded as MEMO_GUIDE_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide leftovers encoded as EXCHANGE_GUIDE_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide/overview leftovers encoded as EXCHANGE_OVERVIEW_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide/step-1-account leftovers encoded as EXCHANGE_STEP1_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide/step-1-account leftovers encoded as EXCHANGE_STEP_1_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide/step-2-access leftovers encoded as EXCHANGE_STEP2_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide/step-3-transaction leftovers encoded as EXCHANGE_STEP3_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide/step-4-non-existent-accounts leftovers encoded as EXCHANGE_STEP4_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide/additional-resources leftovers encoded as EXCHANGE_RESOURCES_INDEX_* (query-only).
// Official BUILD integrations/exchange-guide/additional-resources leftovers encoded as EXCHANGE_ADDITIONAL_RESOURCES_INDEX_* (query-only).
// Official BUILD token-chain/user-account-creation leftovers encoded as TOKEN_CHAIN_ACCOUNT_INDEX_* (query-only).
// Official BUILD token-chain/developer-token-proposal leftovers encoded as TOKEN_CHAIN_PROPOSAL_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture/chains INDEX leftovers encoded as GET_STARTED_SYSTEM_CHAINS_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture/chains/directory-chain INDEX leftovers encoded as GET_STARTED_DIRECTORY_CHAIN_INDEX_* (query-only).
// Official GET-STARTED get-started/about/providers INDEX leftovers encoded as GET_STARTED_PROVIDERS_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture/chains/economy-chain INDEX leftovers encoded as GET_STARTED_ECONOMY_CHAIN_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture/chains/cluster-anchoring-chain INDEX leftovers encoded as GET_STARTED_CLUSTER_ANCHORING_CHAIN_INDEX_* (query-only).
// Official GET-STARTED get-started/use-cases/cross-chain INDEX leftovers encoded as GET_STARTED_CROSS_CHAIN_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture/chains/token-chain INDEX leftovers encoded as GET_STARTED_TOKEN_CHAIN_INDEX_* (query-only).
// Official LEARN courses/book-review/input-verification/structure INDEX leftovers encoded as LEARN_BOOK_REVIEW_INPUT_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/iccf-course/introduction INDEX leftovers encoded as LEARN_ICCF_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/input-verification/tests INDEX leftovers encoded as LEARN_NEWS_UNIT_TESTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-one/project-structure INDEX leftovers encoded as LEARN_TTT_PROJECT_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/vector-db-movie-demo/setup/configure.rell INDEX leftovers encoded as LEARN_VECTOR_DB_CONFIGURE_RELL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/monetize-dapp/setup INDEX leftovers encoded as LEARN_MONETIZE_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/zero-knowledge-proof/introduction INDEX leftovers encoded as LEARN_ZK_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/relationships-course/introduction INDEX leftovers encoded as LEARN_RELATIONSHIPS_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/web3-for-web2-devs/security INDEX leftovers encoded as LEARN_WEB3_SECURITY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/Web3 INDEX leftovers encoded as LEARN_TAGS_WEB3_INDEX_* (query-only HELP ONLY WRITE SKIP).
