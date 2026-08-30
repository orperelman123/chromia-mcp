package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official BUILD cookbook help for building a dapp (queries, client reads, tests).
 * Official pages only, including docs.chromia.com/rell/tests builders and asserts.
 * Skips recipes that sign a live tx, cookbook-only flags/keys, and official printed sample keys.
 * Source: docs.chromia.com/build/cookbook/ plus command / rell/tests pages when they disagree.
 * Leftover official leftover GET-STARTED get-started/use-cases/real-time-data/stork INDEX leftovers live here (query-only).
 */
object ChromiaCookbookHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val OVERVIEW_URL = "https://docs.chromia.com/build/cookbook/overview"
    const val OVERVIEW_URL_SLASH = "https://docs.chromia.com/build/cookbook/overview/"
    const val OVERVIEW_TITLE = "Overview"
    const val OVERVIEW_INDEX_URL = OVERVIEW_URL
    const val OVERVIEW_INDEX_URL_SLASH = OVERVIEW_URL_SLASH
    const val OVERVIEW_INDEX_TITLE = "Welcome to the Chromia Cookbook"
    const val CLI_URL = "https://docs.chromia.com/build/cookbook/cli"
    const val CLI_URL_SLASH = "https://docs.chromia.com/build/cookbook/cli/"
    const val CLI_TITLE = "CLI"
    const val CLI_INDEX_URL = CLI_URL
    const val CLI_INDEX_URL_SLASH = CLI_URL_SLASH
    const val CLI_INDEX_TITLE = CLI_TITLE
    const val QUERY_CREATION_URL = "https://docs.chromia.com/build/cookbook/query-creation"
    const val QUERY_CREATION_URL_SLASH = "https://docs.chromia.com/build/cookbook/query-creation/"
    const val QUERY_CREATION_TITLE = "Create queries"
    const val QUERY_CREATION_INDEX_URL = QUERY_CREATION_URL
    const val QUERY_CREATION_INDEX_URL_SLASH = QUERY_CREATION_URL_SLASH
    const val QUERY_CREATION_INDEX_TITLE = QUERY_CREATION_TITLE
    const val MAKE_QUERY_URL = "https://docs.chromia.com/build/cookbook/query-creation/make-query"
    const val MAKE_QUERY_URL_SLASH = "https://docs.chromia.com/build/cookbook/query-creation/make-query/"
    const val MAKE_QUERY_TITLE = "How to make queries with parameters"
    const val MAKE_QUERY_INDEX_URL = MAKE_QUERY_URL
    const val MAKE_QUERY_INDEX_URL_SLASH = MAKE_QUERY_URL_SLASH
    const val MAKE_QUERY_INDEX_TITLE = MAKE_QUERY_TITLE
    const val MAKE_QUERY_JS_TAB = "JS/TS client"
    const val PAGINATION_URL = "https://docs.chromia.com/build/cookbook/query-creation/pagination"
    const val PAGINATION_URL_SLASH = "https://docs.chromia.com/build/cookbook/query-creation/pagination/"
    const val PAGINATION_TITLE = "How to implement custom pagination"
    const val PAGINATION_INDEX_URL = PAGINATION_URL
    const val PAGINATION_INDEX_URL_SLASH = PAGINATION_URL_SLASH
    const val PAGINATION_INDEX_TITLE = PAGINATION_TITLE
    const val PAGINATION_JS_TAB = "JS/TS client"
    const val PAGINATION_FT4_URL = "https://docs.chromia.com/build/cookbook/query-creation/pagination-with-ft4"
    const val PAGINATION_FT4_URL_SLASH = "https://docs.chromia.com/build/cookbook/query-creation/pagination-with-ft4/"
    const val PAGINATION_FT4_TITLE = "How to implement pagination with FT4"
    const val PAGINATION_FT4_INDEX_URL = PAGINATION_FT4_URL
    const val PAGINATION_FT4_INDEX_URL_SLASH = PAGINATION_FT4_URL_SLASH
    const val PAGINATION_FT4_INDEX_TITLE = PAGINATION_FT4_TITLE
    const val PAGINATION_FT4_JS_TAB = "JS/TS client"
    const val MEMO_QUERY_URL = "https://docs.chromia.com/build/cookbook/query-creation/check-account-memo-requirement"
    const val MEMO_QUERY_URL_SLASH = "https://docs.chromia.com/build/cookbook/query-creation/check-account-memo-requirement/"
    const val MEMO_QUERY_TITLE = "How to check account memo requirement"
    const val MEMO_QUERY_INDEX_URL = MEMO_QUERY_URL
    const val MEMO_QUERY_INDEX_URL_SLASH = MEMO_QUERY_URL_SLASH
    const val MEMO_QUERY_INDEX_TITLE = MEMO_QUERY_TITLE
    const val MEMO_QUERY_JS_TAB = "JS/TS client"
    const val MEMO_ASSET_MGMT_URL = "https://docs.chromia.com/build/ft4/asset-management/"
    const val GET_ACCOUNT_BALANCE_URL = "https://docs.chromia.com/build/cookbook/query-creation/get-account-balance"
    const val GET_ACCOUNT_BALANCE_URL_SLASH = "https://docs.chromia.com/build/cookbook/query-creation/get-account-balance/"
    const val GET_ACCOUNT_BALANCE_TITLE = "How to get account balance"
    const val GET_ACCOUNT_BALANCE_INDEX_URL = GET_ACCOUNT_BALANCE_URL
    const val GET_ACCOUNT_BALANCE_INDEX_URL_SLASH = GET_ACCOUNT_BALANCE_URL_SLASH
    const val GET_ACCOUNT_BALANCE_INDEX_TITLE = GET_ACCOUNT_BALANCE_TITLE
    const val GET_ACCOUNT_BALANCE_JS_TAB = "JS/TS client"
    const val RUN_QUERIES_URL = "https://docs.chromia.com/build/cookbook/cli/run-queries"
    const val RUN_QUERIES_URL_SLASH = "https://docs.chromia.com/build/cookbook/cli/run-queries/"
    const val RUN_QUERIES_TITLE = "How to run queries"
    const val RUN_QUERIES_INDEX_URL = RUN_QUERIES_URL
    const val RUN_QUERIES_INDEX_URL_SLASH = RUN_QUERIES_URL_SLASH
    const val RUN_QUERIES_INDEX_TITLE = "How to run queries"
    const val RUN_TESTS_URL = "https://docs.chromia.com/build/cookbook/cli/run-tests"
    const val RUN_TESTS_URL_SLASH = "https://docs.chromia.com/build/cookbook/cli/run-tests/"
    const val RUN_TESTS_TITLE = "How to run tests"
    const val RUN_TESTS_INDEX_URL = RUN_TESTS_URL
    const val RUN_TESTS_INDEX_URL_SLASH = RUN_TESTS_URL_SLASH
    const val RUN_TESTS_INDEX_TITLE = "How to run tests"
    const val RUN_OPERATIONS_URL = "https://docs.chromia.com/build/cookbook/cli/run-operations"
    const val RUN_OPERATIONS_URL_SLASH = "https://docs.chromia.com/build/cookbook/cli/run-operations/"
    const val RUN_OPERATIONS_TITLE = "How to run operations"
    const val RUN_OPERATIONS_INDEX_URL = RUN_OPERATIONS_URL
    const val RUN_OPERATIONS_INDEX_URL_SLASH = RUN_OPERATIONS_URL_SLASH
    const val RUN_OPERATIONS_INDEX_TITLE = "How to run operations"
    const val CREATE_RELL_DAPP_URL = "https://docs.chromia.com/build/cookbook/cli/create-rell-dapp"
    const val CREATE_RELL_DAPP_URL_SLASH = "https://docs.chromia.com/build/cookbook/cli/create-rell-dapp/"
    const val CREATE_RELL_DAPP_TITLE = "How to create a new Rell dapp"
    const val CREATE_RELL_DAPP_INDEX_URL = CREATE_RELL_DAPP_URL
    const val CREATE_RELL_DAPP_INDEX_URL_SLASH = CREATE_RELL_DAPP_URL_SLASH
    const val CREATE_RELL_DAPP_INDEX_TITLE = "How to create a new Rell dapp"
    const val DATA_INSPECTION_URL = "https://docs.chromia.com/build/cookbook/data-inspection"
    const val DATA_INSPECTION_URL_SLASH = "https://docs.chromia.com/build/cookbook/data-inspection/"
    const val DATA_INSPECTION_TITLE = "Data inspection"
    const val DATA_INSPECTION_INDEX_URL = DATA_INSPECTION_URL
    const val DATA_INSPECTION_INDEX_URL_SLASH = DATA_INSPECTION_URL_SLASH
    const val DATA_INSPECTION_INDEX_TITLE = DATA_INSPECTION_TITLE
    const val ACCOUNT_CREATION_URL = "https://docs.chromia.com/build/cookbook/account-creation"
    const val ACCOUNT_CREATION_URL_SLASH = "https://docs.chromia.com/build/cookbook/account-creation/"
    const val ACCOUNT_CREATION_TITLE = "Account creation"
    const val ACCOUNT_CREATION_INDEX_URL = ACCOUNT_CREATION_URL
    const val ACCOUNT_CREATION_INDEX_URL_SLASH = ACCOUNT_CREATION_URL_SLASH
    const val ACCOUNT_CREATION_INDEX_TITLE = ACCOUNT_CREATION_TITLE
    const val TRANSACTION_CREATION_URL = "https://docs.chromia.com/build/cookbook/transaction-creation"
    const val TRANSACTION_CREATION_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/"
    const val TRANSACTION_CREATION_TITLE = "Create & manage transactions"
    const val TRANSACTION_CREATION_INDEX_URL = TRANSACTION_CREATION_URL
    const val TRANSACTION_CREATION_INDEX_URL_SLASH = TRANSACTION_CREATION_URL_SLASH
    const val TRANSACTION_CREATION_INDEX_TITLE = TRANSACTION_CREATION_TITLE
    const val SIMPLE_TRANSACTION_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction"
    const val SIMPLE_TRANSACTION_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction/"
    const val SIMPLE_TRANSACTION_TITLE = "How to send a simple transaction"
    const val SIMPLE_TRANSACTION_INDEX_URL = SIMPLE_TRANSACTION_URL
    const val SIMPLE_TRANSACTION_INDEX_URL_SLASH = SIMPLE_TRANSACTION_URL_SLASH
    const val SIMPLE_TRANSACTION_INDEX_TITLE = SIMPLE_TRANSACTION_TITLE
    const val SIMPLE_TRANSACTION_JS_TAB = "JS/TS client"
    const val MAKE_TRANSFER_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/make-transfer"
    const val MAKE_TRANSFER_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/make-transfer/"
    const val MAKE_TRANSFER_TITLE = "How to make a transfer"
    const val MAKE_TRANSFER_INDEX_URL = MAKE_TRANSFER_URL
    const val MAKE_TRANSFER_INDEX_URL_SLASH = MAKE_TRANSFER_URL_SLASH
    const val MAKE_TRANSFER_INDEX_TITLE = MAKE_TRANSFER_TITLE
    const val ENABLE_DISABLE_MEMO_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/enable-disable-memo"
    const val ENABLE_DISABLE_MEMO_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/enable-disable-memo/"
    const val ENABLE_DISABLE_MEMO_TITLE = "How to enable/disable memo for transfers"
    const val ENABLE_DISABLE_MEMO_INDEX_URL = ENABLE_DISABLE_MEMO_URL
    const val ENABLE_DISABLE_MEMO_INDEX_URL_SLASH = ENABLE_DISABLE_MEMO_URL_SLASH
    const val ENABLE_DISABLE_MEMO_INDEX_TITLE = ENABLE_DISABLE_MEMO_TITLE
    const val TRANSFER_WITH_MEMO_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/transfer-with-memo"
    const val TRANSFER_WITH_MEMO_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/transfer-with-memo/"
    const val TRANSFER_WITH_MEMO_TITLE = "How to make a transfer with memo"
    const val TRANSFER_WITH_MEMO_INDEX_URL = TRANSFER_WITH_MEMO_URL
    const val TRANSFER_WITH_MEMO_INDEX_URL_SLASH = TRANSFER_WITH_MEMO_URL_SLASH
    const val TRANSFER_WITH_MEMO_INDEX_TITLE = TRANSFER_WITH_MEMO_TITLE
    const val TIME_BOUND_TRANSACTIONS_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/time-bound-transactions"
    const val TIME_BOUND_TRANSACTIONS_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/time-bound-transactions/"
    const val TIME_BOUND_TRANSACTIONS_TITLE = "How to create time-bound transactions"
    const val TIME_BOUND_TRANSACTIONS_INDEX_URL = TIME_BOUND_TRANSACTIONS_URL
    const val TIME_BOUND_TRANSACTIONS_INDEX_URL_SLASH = TIME_BOUND_TRANSACTIONS_URL_SLASH
    const val TIME_BOUND_TRANSACTIONS_INDEX_TITLE = TIME_BOUND_TRANSACTIONS_TITLE
    const val TIME_BOUND_URL = TIME_BOUND_TRANSACTIONS_URL
    const val TIME_BOUND_INDEX_URL = TIME_BOUND_URL
    const val TIME_BOUND_INDEX_URL_SLASH = TIME_BOUND_TRANSACTIONS_URL_SLASH
    const val TIME_BOUND_INDEX_TITLE = TIME_BOUND_TRANSACTIONS_TITLE
    const val CALL_OPERATION_FT4_AUTH_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/call-operation-with-ft4-auth"
    const val CALL_OPERATION_FT4_AUTH_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/call-operation-with-ft4-auth/"
    const val CALL_OPERATION_FT4_AUTH_TITLE = "How to call operations with FT4 authentication"
    const val CALL_OPERATION_FT4_AUTH_INDEX_URL = CALL_OPERATION_FT4_AUTH_URL
    const val CALL_OPERATION_FT4_AUTH_INDEX_URL_SLASH = CALL_OPERATION_FT4_AUTH_URL_SLASH
    const val CALL_OPERATION_FT4_AUTH_INDEX_TITLE = CALL_OPERATION_FT4_AUTH_TITLE
    const val CALL_OP_FT4_URL = CALL_OPERATION_FT4_AUTH_URL
    const val CALL_OP_FT4_INDEX_URL = CALL_OP_FT4_URL
    const val CALL_OP_FT4_INDEX_URL_SLASH = CALL_OPERATION_FT4_AUTH_URL_SLASH
    const val CALL_OP_FT4_INDEX_TITLE = CALL_OPERATION_FT4_AUTH_TITLE
    const val REGISTER_CROSSCHAIN_ASSET_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/register-crosschain-asset"
    const val REGISTER_CROSSCHAIN_ASSET_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/register-crosschain-asset/"
    const val REGISTER_CROSSCHAIN_ASSET_TITLE = "How to register crosschain assets"
    const val REGISTER_CROSSCHAIN_ASSET_INDEX_URL = REGISTER_CROSSCHAIN_ASSET_URL
    const val REGISTER_CROSSCHAIN_ASSET_INDEX_URL_SLASH = REGISTER_CROSSCHAIN_ASSET_URL_SLASH
    const val REGISTER_CROSSCHAIN_ASSET_INDEX_TITLE = REGISTER_CROSSCHAIN_ASSET_TITLE
    const val REGISTER_ASSET_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/register-asset"
    const val REGISTER_ASSET_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/register-asset/"
    const val REGISTER_ASSET_TITLE = "How to register assets"
    const val REGISTER_ASSET_INDEX_URL = REGISTER_ASSET_URL
    const val REGISTER_ASSET_INDEX_URL_SLASH = REGISTER_ASSET_URL_SLASH
    const val REGISTER_ASSET_INDEX_TITLE = REGISTER_ASSET_TITLE
    const val CROSSCHAIN_TRANSFER_URL = "https://docs.chromia.com/build/cookbook/transaction-creation/crosschain-transfer"
    const val CROSSCHAIN_TRANSFER_URL_SLASH = "https://docs.chromia.com/build/cookbook/transaction-creation/crosschain-transfer/"
    const val CROSSCHAIN_TRANSFER_TITLE = "How to make crosschain transfers"
    const val CROSSCHAIN_TRANSFER_INDEX_URL = CROSSCHAIN_TRANSFER_URL
    const val CROSSCHAIN_TRANSFER_INDEX_URL_SLASH = CROSSCHAIN_TRANSFER_URL_SLASH
    const val CROSSCHAIN_TRANSFER_INDEX_TITLE = CROSSCHAIN_TRANSFER_TITLE
    const val TX_STATUS_URL = "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-status"
    const val TX_STATUS_URL_SLASH = "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-status/"
    const val TX_STATUS_INDEX_URL = TX_STATUS_URL
    const val TX_STATUS_INDEX_URL_SLASH = TX_STATUS_URL_SLASH
    const val TX_STATUS_INDEX_TITLE = "How to get transaction status"
    const val TX_DATA_URL = "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-data"
    const val TX_DATA_URL_SLASH = "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-data/"
    const val TX_DATA_INDEX_URL = TX_DATA_URL
    const val TX_DATA_INDEX_URL_SLASH = TX_DATA_URL_SLASH
    const val TX_DATA_INDEX_TITLE = "How to get and decode transaction data"
    const val BLOCK_DATA_URL = "https://docs.chromia.com/build/cookbook/data-inspection/get-block-data"
    const val BLOCK_DATA_URL_SLASH = "https://docs.chromia.com/build/cookbook/data-inspection/get-block-data/"
    const val BLOCK_DATA_INDEX_URL = BLOCK_DATA_URL
    const val BLOCK_DATA_INDEX_URL_SLASH = BLOCK_DATA_URL_SLASH
    const val BLOCK_DATA_INDEX_TITLE = "How to fetch and decode block data"
    const val GET_BLOCK_DATA_INDEX_URL = BLOCK_DATA_URL
    const val GET_BLOCK_DATA_INDEX_URL_SLASH = BLOCK_DATA_URL_SLASH
    const val GET_BLOCK_DATA_INDEX_TITLE = BLOCK_DATA_INDEX_TITLE
    const val ACCOUNT_BY_ID_URL = "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-id"
    const val ACCOUNT_BY_ID_URL_SLASH = "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-id/"
    const val ACCOUNT_BY_ID_TITLE = "How to get an account by ID"
    const val ACCOUNT_BY_ID_INDEX_URL = ACCOUNT_BY_ID_URL
    const val ACCOUNT_BY_ID_INDEX_URL_SLASH = ACCOUNT_BY_ID_URL_SLASH
    const val ACCOUNT_BY_ID_INDEX_TITLE = "How to get an account by ID"
    const val GET_ACCOUNT_BY_ID_INDEX_URL = ACCOUNT_BY_ID_URL
    const val GET_ACCOUNT_BY_ID_INDEX_URL_SLASH = ACCOUNT_BY_ID_URL_SLASH
    const val GET_ACCOUNT_BY_ID_INDEX_TITLE = ACCOUNT_BY_ID_TITLE
    const val ACCOUNT_BY_ID_JS_TAB = "JS/TS client"
    const val ACCOUNT_BY_ID_FT4 = "npm install @chromia/ft4"
    const val ACCOUNT_BY_ID_FT4_CLIENT_URL = "https://docs.chromia.com/build/clients/ft4-client"
    const val ACCOUNT_BY_ID_ACCOUNT_MGMT_URL = "https://docs.chromia.com/build/ft4/account-management/"
    const val ACCOUNT_BY_SIGNER_URL = "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-signer"
    const val ACCOUNT_BY_SIGNER_URL_SLASH = "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-signer/"
    const val ACCOUNT_BY_SIGNER_TITLE = "How to get accounts by signer"
    const val ACCOUNT_BY_SIGNER_INDEX_URL = ACCOUNT_BY_SIGNER_URL
    const val ACCOUNT_BY_SIGNER_INDEX_URL_SLASH = ACCOUNT_BY_SIGNER_URL_SLASH
    const val ACCOUNT_BY_SIGNER_INDEX_TITLE = "How to get accounts by signer"
    const val GET_ACCOUNT_BY_SIGNER_INDEX_URL = ACCOUNT_BY_SIGNER_URL
    const val GET_ACCOUNT_BY_SIGNER_INDEX_URL_SLASH = ACCOUNT_BY_SIGNER_URL_SLASH
    const val GET_ACCOUNT_BY_SIGNER_INDEX_TITLE = ACCOUNT_BY_SIGNER_TITLE
    const val ACCOUNT_BY_SIGNER_JS_TAB = "JS/TS client"
    const val ACCOUNT_TRANSFER_HISTORY_URL = "https://docs.chromia.com/build/cookbook/data-inspection/get-account-transfer-history"
    const val ACCOUNT_TRANSFER_HISTORY_URL_SLASH = "https://docs.chromia.com/build/cookbook/data-inspection/get-account-transfer-history/"
    const val ACCOUNT_TRANSFER_HISTORY_TITLE = "How to get account transfer history"
    const val ACCOUNT_TRANSFER_HISTORY_INDEX_URL = ACCOUNT_TRANSFER_HISTORY_URL
    const val ACCOUNT_TRANSFER_HISTORY_INDEX_URL_SLASH = ACCOUNT_TRANSFER_HISTORY_URL_SLASH
    const val ACCOUNT_TRANSFER_HISTORY_INDEX_TITLE = "How to get account transfer history"
    const val GET_ACCOUNT_TRANSFER_HISTORY_INDEX_URL = ACCOUNT_TRANSFER_HISTORY_URL
    const val GET_ACCOUNT_TRANSFER_HISTORY_INDEX_URL_SLASH = ACCOUNT_TRANSFER_HISTORY_URL_SLASH
    const val GET_ACCOUNT_TRANSFER_HISTORY_INDEX_TITLE = ACCOUNT_TRANSFER_HISTORY_TITLE
    const val ACCOUNT_TRANSFER_HISTORY_JS_TAB = "JS/TS client"
    const val OPEN_STRATEGY_URL = "https://docs.chromia.com/build/cookbook/account-creation/open-strategy"
    const val OPEN_STRATEGY_URL_SLASH = "https://docs.chromia.com/build/cookbook/account-creation/open-strategy/"
    const val OPEN_STRATEGY_TITLE = "How to create account with open strategy"
    const val OPEN_STRATEGY_INDEX_URL = OPEN_STRATEGY_URL
    const val OPEN_STRATEGY_INDEX_URL_SLASH = OPEN_STRATEGY_URL_SLASH
    const val OPEN_STRATEGY_INDEX_TITLE = "How to create account with open strategy"
    const val TRANSFER_FEE_STRATEGY_URL = "https://docs.chromia.com/build/cookbook/account-creation/transfer-fee-strategy"
    const val TRANSFER_FEE_STRATEGY_URL_SLASH = "https://docs.chromia.com/build/cookbook/account-creation/transfer-fee-strategy/"
    const val TRANSFER_FEE_STRATEGY_TITLE = "How to create account with transfer fee strategy"
    const val TRANSFER_FEE_STRATEGY_INDEX_URL = TRANSFER_FEE_STRATEGY_URL
    const val TRANSFER_FEE_STRATEGY_INDEX_URL_SLASH = TRANSFER_FEE_STRATEGY_URL_SLASH
    const val TRANSFER_FEE_STRATEGY_INDEX_TITLE = "How to create account with transfer fee strategy"
    const val TRANSFER_OPEN_STRATEGY_URL = "https://docs.chromia.com/build/cookbook/account-creation/transfer-open-strategy"
    const val TRANSFER_OPEN_STRATEGY_URL_SLASH = "https://docs.chromia.com/build/cookbook/account-creation/transfer-open-strategy/"
    const val TRANSFER_OPEN_STRATEGY_TITLE = "How to create account with transfer open strategy"
    const val TRANSFER_OPEN_STRATEGY_INDEX_URL = TRANSFER_OPEN_STRATEGY_URL
    const val TRANSFER_OPEN_STRATEGY_INDEX_URL_SLASH = TRANSFER_OPEN_STRATEGY_URL_SLASH
    const val TRANSFER_OPEN_STRATEGY_INDEX_TITLE = "How to create account with transfer open strategy"
    const val TRANSFER_SUBSCRIPTION_STRATEGY_URL = "https://docs.chromia.com/build/cookbook/account-creation/transfer-subscription-strategy"
    const val TRANSFER_SUBSCRIPTION_STRATEGY_URL_SLASH = "https://docs.chromia.com/build/cookbook/account-creation/transfer-subscription-strategy/"
    const val TRANSFER_SUBSCRIPTION_STRATEGY_TITLE = "How to create account with transfer subscription strategy"
    const val TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_URL = TRANSFER_SUBSCRIPTION_STRATEGY_URL
    const val TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_URL_SLASH = TRANSFER_SUBSCRIPTION_STRATEGY_URL_SLASH
    const val TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_TITLE = "How to create account with transfer subscription strategy"
    const val GET_STARTED_STORK_INDEX_URL = "https://docs.chromia.com/get-started/use-cases/real-time-data/stork"
    const val GET_STARTED_STORK_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/real-time-data/stork/"
    const val GET_STARTED_STORK_INDEX_TITLE = "Real-time data feed applications"  // official H1
    const val ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/container-management/transfer-container-ownership"
    const val ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/container-management/transfer-container-ownership/"
    const val ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_TITLE = "Transfer container lease ownership"  // official H1
    const val LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL = "https://learn.chromia.com/courses/book-review/input-verification"
    const val LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/input-verification/"
    const val LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_TITLE = "Lesson 3 - Verify and validate inputs"  // official H1
    const val LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-nft/randomness"
    const val LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-nft/randomness/"
    const val LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_TITLE = "Add randomness to the card"  // official H1
    const val LEARN_TAGS_DEFI_INDEX_URL = "https://learn.chromia.com/tags/DeFi"
    const val LEARN_TAGS_DEFI_INDEX_URL_SLASH = "https://learn.chromia.com/tags/DeFi/"
    const val LEARN_TAGS_DEFI_INDEX_TITLE = "Courses tagged with: DeFi"  // official H1
    val leftoverOfficialTxStatuses = listOf("Unknown", "Waiting", "Confirmed", "Rejected")
    const val QUERY_COMMAND_URL = ChrQueryHelp.QUERY_DOCS_URL
    const val TEST_COMMAND_URL = "https://docs.chromia.com/build/cli/commands/test"
    const val JS_CLIENT_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference"
    const val TX_DATA_TITLE = "How to get and decode transaction data"
    const val BLOCK_DATA_TITLE = "How to fetch and decode block data"
    const val TX_DATA_JS_TAB = "JavaScript get and decode transaction"
    const val TX_DATA_JS_REFERENCE =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference#transactions"
    const val BLOCK_DATA_JS_TAB = "JS/TS client"
    const val BLOCK_DATA_NPM = "npm install postchain-client"
    const val RELL_TESTS_URL = "https://docs.chromia.com/rell/tests"
    const val BEST_PRACTICES_URL = "https://docs.chromia.com/rell/rell-best-practices"

    val includedPages = listOf(
        OVERVIEW_URL,
        CLI_URL,
        QUERY_CREATION_URL,
        MAKE_QUERY_URL,
        PAGINATION_URL,
        PAGINATION_FT4_URL,
        MEMO_QUERY_URL,
        RUN_QUERIES_URL,
        RUN_TESTS_URL,
        CREATE_RELL_DAPP_URL,
        DATA_INSPECTION_URL,
        ACCOUNT_CREATION_URL,
        TRANSACTION_CREATION_URL,
        "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-id",
        "https://docs.chromia.com/build/cookbook/data-inspection/get-account-by-signer",
        "https://docs.chromia.com/build/cookbook/data-inspection/get-account-transfer-history",
        "https://docs.chromia.com/build/cookbook/data-inspection/get-block-data",
        "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-data",
        "https://docs.chromia.com/build/cookbook/data-inspection/get-transaction-status"
    )

    val skippedPages = listOf(
        "https://docs.chromia.com/build/cookbook/account-creation/open-strategy",
        "https://docs.chromia.com/build/cookbook/account-creation/transfer-fee-strategy",
        "https://docs.chromia.com/build/cookbook/account-creation/transfer-open-strategy",
        "https://docs.chromia.com/build/cookbook/account-creation/transfer-subscription-strategy",
        "https://docs.chromia.com/build/cookbook/transaction-creation/make-transfer",
        "https://docs.chromia.com/build/cookbook/transaction-creation/call-operation-with-ft4-auth",
        "https://docs.chromia.com/build/cookbook/transaction-creation/crosschain-transfer",
        "https://docs.chromia.com/build/cookbook/transaction-creation/enable-disable-memo",
        "https://docs.chromia.com/build/cookbook/transaction-creation/register-asset",
        "https://docs.chromia.com/build/cookbook/transaction-creation/register-crosschain-asset",
        "https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction",
        "https://docs.chromia.com/build/cookbook/transaction-creation/time-bound-transactions",
        "https://docs.chromia.com/build/cookbook/transaction-creation/transfer-with-memo",
        "https://docs.chromia.com/build/cookbook/cli/run-operations",
        "https://docs.chromia.com/build/cookbook/query-creation/get-account-balance"
    )

    fun rellTestExample(): String = """
        @test module;

        function test_hello_world() {
            val result = hello_world();
            assert_equals(result, "Hello World!");
        }
    """.trimIndent() + "\n"

    fun rellTestTxExample(): String = """
        @test module;
        import data;
        function test_add_user() {
            assert_equals(data.user @* {}(.name), list<text>());
            val tx = rell.test.tx(data.add_user('Bob'));
            assert_equals(data.user @* {}(.name), list<text>());
            tx.run();
            assert_equals(data.user @* {}(.name), ['Bob']);
        }
    """.trimIndent() + "\n"

    fun rellTestMustFailExample(): String = """
        function test_transfer_validation_must_fail() {
            val failure = rell.test.tx()
                .op(transfer(recipient, asset_id, -1))
                .run_must_fail("Amount must be positive");
            assert_true(failure.message.contains("Amount must be positive"));
        }
    """.trimIndent() + "\n"

    fun rellTestDisabledExample(): String = """
        @test @disabled module;
        function test_addition() {
        }
        @test module;
        @disabled @test function test_edge_case() {
        }
        @test function test_happy_path() {
        }
    """.trimIndent() + "\n"

    val rellTestBuilders = listOf(
        "rell.test.block  # builder for test blocks containing transactions",
        "rell.test.tx  # builder for test transactions containing operations",
        "rell.test.op  # produced by calling an operation in test scope",
        ".run()  # execute immediately",
        ".run_must_fail() / .run_must_fail(\"message\")  # assert failure"
    )

    val rellTestAsserts = listOf(
        "assert_equals / assert_not_equals",
        "assert_true / assert_false",
        "assert_null / assert_not_null",
        "assert_gt / assert_ge / assert_lt / assert_le",
        "assert_ge_le / assert_ge_lt / assert_gt_le / assert_gt_lt",
        "assert_fails",
        "assert_events"
    )

    val rellTestTime = listOf(
        "rell.test.last_block_time",
        "rell.test.next_block_time",
        "rell.test.block_interval  # default 10 seconds",
        "rell.test.set_next_block_time",
        "rell.test.set_next_block_time_delta",
        "rell.test.set_block_interval"
    )

    val rellTestOther = listOf(
        "rell.test.nop()  # unique no-op so identical txs differ",
        "rell.test.get_events()"
    )

    fun notes(): String = """
        Chromia CLI $CLI_SERIES official BUILD cookbook help (queries / tests / data inspection). Java 21+, Postgres 16+.
        Overview: $OVERVIEW_URL
        Official query invoke page wins flags: $QUERY_COMMAND_URL
        Official test invoke page wins flags: $TEST_COMMAND_URL
        Official create-rell-dapp / query / cookbook Hello World query name is hello_world (returns "Hello World!").
        Local default (no dApp target): `chr query hello_world` targets `chr node start` ($QUERY_COMMAND_URL).
        Explicit local: `chr query --blockchain-rid <BlockchainRID> hello_world`. Do not invent a BRID.
        REST read (cookbook create-rell-dapp): GET localhost:7740/query/<BlockchainRID>?type=hello_world
        JS/TS query (official client reference $JS_CLIENT_URL): client.query("hello_world") or
        client.query("get_foobar", { foo: 1, bar: 2 }). Official setting name is blockchainRid (not a signed tx).
        FT4 memo check query name on the cookbook page: does_account_require_memo (read-only).
        FT4 pagination query pattern: get_users_paginated with page_size / page_cursor; first page_cursor is null.
        Rell tests ($RELL_TESTS_URL): @test module; functions named test or starting with test_ are executed.
        A module name ending in _test is the companion of the same name without the suffix (program <-> program_test).
        @disabled (since 0.15.1) on a test module skips every test in that module and its submodules; on a test function skips that function.
        Using @disabled on a non-test module or non-test function is a compile error. There is no official setUp / tearDown keyword.
        Official leftover best-practices ($BEST_PRACTICES_URL): pass the expected string to run_must_fail;
        then assert_true(failure.message.contains(...)). One failure mode per negative test. Official page also
        uses .sign(rell.test.keypairs.*) — skipped (no signing / no key material). Test key NAMES: cookbook below.
        Official builders: rell.test.block / rell.test.tx / rell.test.op with .run() and .run_must_fail(["message"]).
        Official asserts: assert_equals, assert_not_equals, assert_true, assert_false, assert_null, assert_not_null,
        assert_gt / ge / lt / le, range forms assert_ge_le / assert_ge_lt / assert_gt_le / assert_gt_lt, assert_fails, assert_events.
        Official test key NAMES (never production): rell.test.keypairs / rell.test.pubkeys with alice, bob, charlie, dave, eve, frank, grace, heidi, trudy.
        Official printed sample key material is skipped. `chr test`. Official --modules is comma-delimited
        (`chr test --modules test.data_test` on $TEST_COMMAND_URL).
        Cookbook `chr query --local` is not on the official query command page — skipped.
        Cookbook `chr test --sql-log` is stale (CLI 0.31.0 removed it; use chr_repl_help `chr repl --sql-log`).
        Cookbook `chr test --verbose` is not on the official test command page — skipped.
        Cookbook chromia.yml keys test.timeout, test.parallel, database.schema_version, build.output_dir, build.optimize
        are not on project-config — skipped.
        Skipped (sign / live tx / private key): account-creation/*, transaction-creation/*, cli/run-operations,
        query-creation/get-account-balance (EVM key pair).
        Leftover official BUILD cookbook get-transaction-status (leftover official $TX_STATUS_URL leftover official 307 leftover official $TX_STATUS_URL_SLASH leftover official 200): leftover official Unknown leftover official Waiting leftover official Confirmed leftover official Rejected leftover official REST leftover official pointer leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook get-transaction-data (leftover official $TX_DATA_URL leftover official 307 leftover official $TX_DATA_URL_SLASH leftover official 200 leftover official $TX_DATA_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $TX_DATA_JS_TAB leftover official $TX_DATA_JS_REFERENCE leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook get-block-data (leftover official $BLOCK_DATA_URL leftover official 307 leftover official $BLOCK_DATA_URL_SLASH leftover official 200 leftover official $BLOCK_DATA_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $BLOCK_DATA_JS_TAB leftover official $BLOCK_DATA_NPM leftover official $JS_CLIENT_URL leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook get-account-by-id (leftover official $ACCOUNT_BY_ID_URL leftover official 307 leftover official $ACCOUNT_BY_ID_URL_SLASH leftover official 200 leftover official $ACCOUNT_BY_ID_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $ACCOUNT_BY_ID_JS_TAB leftover official $ACCOUNT_BY_ID_FT4 leftover official $BLOCK_DATA_NPM leftover official $ACCOUNT_BY_ID_FT4_CLIENT_URL leftover official $ACCOUNT_BY_ID_ACCOUNT_MGMT_URL leftover official $JS_CLIENT_URL leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook get-account-by-signer (leftover official $ACCOUNT_BY_SIGNER_URL leftover official 307 leftover official $ACCOUNT_BY_SIGNER_URL_SLASH leftover official 200 leftover official $ACCOUNT_BY_SIGNER_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $ACCOUNT_BY_SIGNER_JS_TAB leftover official $ACCOUNT_BY_ID_FT4 leftover official $BLOCK_DATA_NPM leftover official $ACCOUNT_BY_ID_FT4_CLIENT_URL leftover official $ACCOUNT_BY_ID_ACCOUNT_MGMT_URL leftover official $JS_CLIENT_URL leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook get-account-transfer-history (leftover official $ACCOUNT_TRANSFER_HISTORY_URL leftover official 307 leftover official $ACCOUNT_TRANSFER_HISTORY_URL_SLASH leftover official 200 leftover official $ACCOUNT_TRANSFER_HISTORY_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $ACCOUNT_TRANSFER_HISTORY_JS_TAB leftover official $ACCOUNT_BY_ID_FT4 leftover official $BLOCK_DATA_NPM leftover official $ACCOUNT_BY_ID_FT4_CLIENT_URL leftover official $ACCOUNT_BY_ID_ACCOUNT_MGMT_URL leftover official $JS_CLIENT_URL leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook pagination-with-ft4 (leftover official $PAGINATION_FT4_URL leftover official 307 leftover official $PAGINATION_FT4_URL_SLASH leftover official 200 leftover official $PAGINATION_FT4_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $PAGINATION_FT4_JS_TAB leftover official $ACCOUNT_BY_ID_FT4 leftover official $BLOCK_DATA_NPM leftover official $ACCOUNT_BY_ID_FT4_CLIENT_URL leftover official $JS_CLIENT_URL leftover official lib.ft4.utils leftover official get_users_paginated leftover official page_size leftover official page_cursor leftover official next_cursor leftover official ft4_utils.before_rowid leftover official ft4_utils.pagination_result leftover official ft4_utils.fetch_data_size leftover official ft4_utils.make_page leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook check-account-memo-requirement (leftover official $MEMO_QUERY_URL leftover official 307 leftover official $MEMO_QUERY_URL_SLASH leftover official 200 leftover official $MEMO_QUERY_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $MEMO_QUERY_JS_TAB leftover official $BLOCK_DATA_NPM leftover official $ACCOUNT_BY_ID_FT4_CLIENT_URL leftover official $MEMO_ASSET_MGMT_URL leftover official $JS_CLIENT_URL leftover official does_account_require_memo leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook pagination (leftover official $PAGINATION_URL leftover official 307 leftover official $PAGINATION_URL_SLASH leftover official 200 leftover official $PAGINATION_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $PAGINATION_JS_TAB leftover official $JS_CLIENT_URL leftover official get_users_paginated leftover official page_cursor leftover official data_size leftover official paged_result leftover official next_cursor leftover official paginator.rell leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook make-query (leftover official $MAKE_QUERY_URL leftover official 307 leftover official $MAKE_QUERY_URL_SLASH leftover official 200 leftover official $MAKE_QUERY_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $MAKE_QUERY_JS_TAB leftover official $BLOCK_DATA_NPM leftover official $JS_CLIENT_URL leftover official client.query leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook run-queries (leftover official $RUN_QUERIES_URL leftover official 307 leftover official $RUN_QUERIES_URL_SLASH leftover official 200 leftover official $RUN_QUERIES_TITLE): leftover official curl leftover official localhost:7740 leftover official hello_world leftover official Hello World! leftover official --blockchain-rid leftover official <BlockchainRID> leftover official --network leftover official testnet leftover official --blockchain leftover official my_rell_dapp leftover official foo=17 leftover official bar=hello leftover official x"AB12" leftover official query-only leftover official skip leftover official --local leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official run-operations leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook run-tests (leftover official $RUN_TESTS_URL leftover official 307 leftover official $RUN_TESTS_URL_SLASH leftover official 200 leftover official $RUN_TESTS_TITLE): leftover official src/test leftover official test_ leftover official @test leftover official module leftover official hello_world leftover official Hello World! leftover official assert_equals leftover official assert_true leftover official assert_false leftover official assert_lt leftover official assert_gt leftover official assert_fails leftover official --modules leftover official arithmetic_test leftover official data_test leftover official print leftover official seeder leftover official init leftover official generate leftover official chr_seeder_help leftover official skip leftover official --verbose leftover official skip leftover official --sql-log leftover official on leftover official chr leftover official test leftover official analyze leftover official chr repl --sql-log --use-db --module leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official run-operations leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook create-rell-dapp (leftover official $CREATE_RELL_DAPP_URL leftover official 307 leftover official $CREATE_RELL_DAPP_URL_SLASH leftover official 200 leftover official $CREATE_RELL_DAPP_TITLE): leftover official templates leftover official chr_create_rell_dapp_help leftover official my-rell-dapp leftover official chromia.yml leftover official src/main.rell leftover official src/test leftover official arithmetic_test.rell leftover official data_test.rell leftover official chr create-rell-dapp leftover official cd my-rell-dapp leftover official chr node start leftover official chr query hello_world leftover official hello_world leftover official Hello World! leftover official curl leftover official localhost:7740 leftover official /query/<BlockchainRID>?type=hello_world leftover official --blockchain-rid leftover official <BlockchainRID> leftover official skip leftover official --local leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official run-operations leftover official skip leftover official test.timeout leftover official test.parallel leftover official database.schema_version leftover official build.output_dir leftover official build.optimize leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook overview (leftover official $OVERVIEW_URL leftover official 307 leftover official $OVERVIEW_URL_SLASH leftover official 200 leftover official $OVERVIEW_TITLE): leftover official Welcome to the Chromia Cookbook leftover official CLI leftover official https://docs.chromia.com/build/cookbook/cli/ leftover official Account creation leftover official https://docs.chromia.com/build/cookbook/account-creation/ leftover official this signs leftover official Data inspection leftover official $DATA_INSPECTION_URL leftover official Create & manage transactions leftover official https://docs.chromia.com/build/cookbook/transaction-creation/ leftover official this signs leftover official Create queries leftover official $QUERY_CREATION_URL leftover official ready-made code leftover official replacing chain BRIDs leftover official <BlockchainRID> leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official run-operations leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official leftover BUILD cookbook/overview (leftover official $OVERVIEW_INDEX_URL leftover official 307 leftover official $OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official $OVERVIEW_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover run-operations leftover official leftover this leftover official leftover signs.
        Leftover official BUILD cookbook CLI (leftover official $CLI_URL leftover official 307 leftover official $CLI_URL_SLASH leftover official 200 leftover official $CLI_TITLE): leftover official practical leftover official hands-on leftover official recipes leftover official Chromia CLI leftover official development leftover official workflows leftover official How to create a new Rell dapp leftover official $CREATE_RELL_DAPP_URL leftover official Bootstrap a new project leftover official How to run queries leftover official $RUN_QUERIES_URL leftover official Test data retrieval leftover official How to run operations leftover official https://docs.chromia.com/build/cookbook/cli/run-operations leftover official this signs leftover official How to run tests leftover official $RUN_TESTS_URL leftover official Create effective tests leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official run-operations leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official leftover BUILD cookbook/cli (leftover official $CLI_INDEX_URL leftover official 307 leftover official $CLI_INDEX_URL_SLASH leftover official 200 leftover official $CLI_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover run-operations leftover official leftover this leftover official leftover signs.
        Leftover official leftover BUILD cookbook/query-creation (leftover official $QUERY_CREATION_INDEX_URL leftover official 307 leftover official $QUERY_CREATION_INDEX_URL_SLASH leftover official 200 leftover official $QUERY_CREATION_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover get-account-balance leftover official leftover EVM leftover official leftover key leftover official leftover pair leftover official leftover this leftover official leftover signs.
        Leftover official leftover BUILD cookbook/query-creation/make-query (leftover official $MAKE_QUERY_INDEX_URL leftover official 307 leftover official $MAKE_QUERY_INDEX_URL_SLASH leftover official 200 leftover official $MAKE_QUERY_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/query-creation/check-account-memo-requirement (leftover official $MEMO_QUERY_INDEX_URL leftover official 307 leftover official $MEMO_QUERY_INDEX_URL_SLASH leftover official 200 leftover official $MEMO_QUERY_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/query-creation/get-account-balance (leftover official $GET_ACCOUNT_BALANCE_INDEX_URL leftover official 307 leftover official $GET_ACCOUNT_BALANCE_INDEX_URL_SLASH leftover official 200 leftover official $GET_ACCOUNT_BALANCE_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover EVM leftover official leftover key leftover official leftover pair leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official leftover BUILD cookbook/query-creation/pagination-with-ft4 (leftover official $PAGINATION_FT4_INDEX_URL leftover official 307 leftover official $PAGINATION_FT4_INDEX_URL_SLASH leftover official 200 leftover official $PAGINATION_FT4_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/query-creation/pagination (leftover official $PAGINATION_INDEX_URL leftover official 307 leftover official $PAGINATION_INDEX_URL_SLASH leftover official 200 leftover official $PAGINATION_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover add_user leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official BUILD cookbook query-creation (leftover official $QUERY_CREATION_URL leftover official 307 leftover official $QUERY_CREATION_URL_SLASH leftover official 200 leftover official $QUERY_CREATION_TITLE): leftover official Explore leftover official simple leftover official practical leftover official recipes leftover official querying blockchain data leftover official inspecting account states leftover official How to make queries with parameters leftover official $MAKE_QUERY_URL leftover official Learn the fundamental pattern leftover official How to check account memo requirement leftover official $MEMO_QUERY_URL leftover official Query whether an FT4 account requires memo leftover official How to get account balance leftover official $GET_ACCOUNT_BALANCE_URL leftover official EVM key pair leftover official How to implement pagination with FT4 leftover official $PAGINATION_FT4_URL leftover official efficient data retrieval leftover official How to implement custom pagination leftover official $PAGINATION_URL leftover official full control over pagination logic leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official get-account-balance leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official BUILD cookbook data-inspection (leftover official $DATA_INSPECTION_URL leftover official 307 leftover official $DATA_INSPECTION_URL_SLASH leftover official 200 leftover official $DATA_INSPECTION_TITLE): leftover official Explore leftover official simple leftover official practical leftover official recipes leftover official reading on-chain data leftover official blocks leftover official transactions leftover official events leftover official How to fetch and decode block data leftover official $BLOCK_DATA_URL leftover official Tx RID leftover official block RID leftover official block info leftover official Postchain leftover official How to get and decode transaction data leftover official $TX_DATA_URL leftover official Postchain leftover official FT4 leftover official How to get transaction status leftover official $TX_STATUS_URL leftover official Retrieve the status leftover official RID leftover official How to get account transfer history leftover official $ACCOUNT_TRANSFER_HISTORY_URL leftover official paginated transfer history leftover official How to get accounts by signer leftover official $ACCOUNT_BY_SIGNER_URL leftover official public key leftover official EVM address leftover official How to get an account by ID leftover official $ACCOUNT_BY_ID_URL leftover official unique ID leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official run-operations leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official leftover BUILD cookbook/data-inspection (leftover official $DATA_INSPECTION_INDEX_URL leftover official 307 leftover official $DATA_INSPECTION_INDEX_URL_SLASH leftover official 200 leftover official $DATA_INSPECTION_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official BUILD cookbook account-creation (leftover official $ACCOUNT_CREATION_URL leftover official 307 leftover official $ACCOUNT_CREATION_URL_SLASH leftover official 200 leftover official $ACCOUNT_CREATION_TITLE): leftover official practical leftover official hands-on leftover official recipes leftover official Chromia CLI leftover official development leftover official workflows leftover official How to create account with open strategy leftover official https://docs.chromia.com/build/cookbook/account-creation/open-strategy leftover official this signs leftover official How to create account with transfer fee strategy leftover official https://docs.chromia.com/build/cookbook/account-creation/transfer-fee-strategy leftover official this signs leftover official How to create account with transfer open strategy leftover official https://docs.chromia.com/build/cookbook/account-creation/transfer-open-strategy leftover official this signs leftover official How to create account with transfer subscription strategy leftover official https://docs.chromia.com/build/cookbook/account-creation/transfer-subscription-strategy leftover official this signs leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official account-creation leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official leftover BUILD cookbook/account-creation (leftover official $ACCOUNT_CREATION_INDEX_URL leftover official 307 leftover official $ACCOUNT_CREATION_INDEX_URL_SLASH leftover official 200 leftover official $ACCOUNT_CREATION_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover register leftover official leftover login leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official BUILD cookbook transaction-creation (leftover official $TRANSACTION_CREATION_URL leftover official 307 leftover official $TRANSACTION_CREATION_URL_SLASH leftover official 200 leftover official $TRANSACTION_CREATION_TITLE): leftover official Explore leftover official simple leftover official practical leftover official recipes leftover official creating leftover official transactions leftover official How to send a simple transaction leftover official https://docs.chromia.com/build/cookbook/transaction-creation/simple-transaction leftover official this signs leftover official How to make a transfer leftover official https://docs.chromia.com/build/cookbook/transaction-creation/make-transfer leftover official this signs leftover official How to enable/disable memo for transfers leftover official https://docs.chromia.com/build/cookbook/transaction-creation/enable-disable-memo leftover official this signs leftover official How to make a transfer with memo leftover official https://docs.chromia.com/build/cookbook/transaction-creation/transfer-with-memo leftover official this signs leftover official How to create time-bound transactions leftover official https://docs.chromia.com/build/cookbook/transaction-creation/time-bound-transactions leftover official this signs leftover official How to call operations with FT4 authentication leftover official https://docs.chromia.com/build/cookbook/transaction-creation/call-operation-with-ft4-auth leftover official this signs leftover official How to register crosschain assets leftover official https://docs.chromia.com/build/cookbook/transaction-creation/register-crosschain-asset leftover official this signs leftover official How to register assets leftover official https://docs.chromia.com/build/cookbook/transaction-creation/register-asset leftover official this signs leftover official How to make crosschain transfers leftover official https://docs.chromia.com/build/cookbook/transaction-creation/crosschain-transfer leftover official this signs leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official transaction-creation leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex.
        Leftover official leftover BUILD cookbook/transaction-creation (leftover official $TRANSACTION_CREATION_INDEX_URL leftover official 307 leftover official $TRANSACTION_CREATION_INDEX_URL_SLASH leftover official 200 leftover official $TRANSACTION_CREATION_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover create leftover official leftover sign leftover official leftover send leftover official leftover txs leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official leftover BUILD cookbook/transaction-creation/simple-transaction (leftover official $SIMPLE_TRANSACTION_INDEX_URL leftover official 307 leftover official $SIMPLE_TRANSACTION_INDEX_URL_SLASH leftover official 200 leftover official $SIMPLE_TRANSACTION_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover JS/TS leftover official leftover client leftover official leftover tab leftover official leftover $SIMPLE_TRANSACTION_JS_TAB leftover official leftover $ACCOUNT_BY_ID_FT4 leftover official leftover $BLOCK_DATA_NPM leftover official leftover $ACCOUNT_BY_ID_FT4_CLIENT_URL leftover official leftover $JS_CLIENT_URL leftover official leftover signAndSendUniqueTransaction leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official leftover BUILD cookbook/transaction-creation/make-transfer (leftover official $MAKE_TRANSFER_INDEX_URL leftover official 307 leftover official $MAKE_TRANSFER_INDEX_URL_SLASH leftover official 200 leftover official $MAKE_TRANSFER_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover transfer leftover official leftover sign leftover official leftover send leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official leftover BUILD cookbook/transaction-creation/enable-disable-memo (leftover official $ENABLE_DISABLE_MEMO_INDEX_URL leftover official 307 leftover official $ENABLE_DISABLE_MEMO_INDEX_URL_SLASH leftover official 200 leftover official $ENABLE_DISABLE_MEMO_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover memo leftover official leftover enable leftover official leftover disable leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official leftover BUILD cookbook/transaction-creation/transfer-with-memo (leftover official $TRANSFER_WITH_MEMO_INDEX_URL leftover official 307 leftover official $TRANSFER_WITH_MEMO_INDEX_URL_SLASH leftover official 200 leftover official $TRANSFER_WITH_MEMO_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover transfer leftover official leftover memo leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs.
        Leftover official leftover BUILD cookbook/transaction-creation/time-bound-transactions (leftover official $TIME_BOUND_INDEX_URL leftover official 307 leftover official $TIME_BOUND_INDEX_URL_SLASH leftover official 200 leftover official $TIME_BOUND_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover timeb leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/transaction-creation/call-operation-with-ft4-auth (leftover official $CALL_OP_FT4_INDEX_URL leftover official 307 leftover official $CALL_OP_FT4_INDEX_URL_SLASH leftover official 200 leftover official $CALL_OP_FT4_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover FT4 leftover official leftover auth leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/transaction-creation/register-crosschain-asset (leftover official $REGISTER_CROSSCHAIN_ASSET_INDEX_URL leftover official 307 leftover official $REGISTER_CROSSCHAIN_ASSET_INDEX_URL_SLASH leftover official 200 leftover official $REGISTER_CROSSCHAIN_ASSET_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover register leftover official leftover crosschain leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/transaction-creation/register-asset (leftover official $REGISTER_ASSET_INDEX_URL leftover official 307 leftover official $REGISTER_ASSET_INDEX_URL_SLASH leftover official 200 leftover official $REGISTER_ASSET_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover register leftover official leftover asset leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/transaction-creation/crosschain-transfer (leftover official $CROSSCHAIN_TRANSFER_INDEX_URL leftover official 307 leftover official $CROSSCHAIN_TRANSFER_INDEX_URL_SLASH leftover official 200 leftover official $CROSSCHAIN_TRANSFER_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover crosschain leftover official leftover transfer leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/data-inspection/get-transaction-status (leftover official $TX_STATUS_INDEX_URL leftover official 307 leftover official $TX_STATUS_INDEX_URL_SLASH leftover official 200 leftover official $TX_STATUS_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-transaction-data (leftover official $TX_DATA_INDEX_URL leftover official 307 leftover official $TX_DATA_INDEX_URL_SLASH leftover official 200 leftover official $TX_DATA_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-block-data (leftover official $BLOCK_DATA_INDEX_URL leftover official 307 leftover official $BLOCK_DATA_INDEX_URL_SLASH leftover official 200 leftover official $BLOCK_DATA_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-block-data (leftover official $GET_BLOCK_DATA_INDEX_URL leftover official 307 leftover official $GET_BLOCK_DATA_INDEX_URL_SLASH leftover official 200 leftover official $GET_BLOCK_DATA_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-account-by-id (leftover official $ACCOUNT_BY_ID_INDEX_URL leftover official 307 leftover official $ACCOUNT_BY_ID_INDEX_URL_SLASH leftover official 200 leftover official $ACCOUNT_BY_ID_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-account-by-id (leftover official $GET_ACCOUNT_BY_ID_INDEX_URL leftover official 307 leftover official $GET_ACCOUNT_BY_ID_INDEX_URL_SLASH leftover official 200 leftover official $GET_ACCOUNT_BY_ID_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-account-by-signer (leftover official $ACCOUNT_BY_SIGNER_INDEX_URL leftover official 307 leftover official $ACCOUNT_BY_SIGNER_INDEX_URL_SLASH leftover official 200 leftover official $ACCOUNT_BY_SIGNER_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-account-by-signer (leftover official $GET_ACCOUNT_BY_SIGNER_INDEX_URL leftover official 307 leftover official $GET_ACCOUNT_BY_SIGNER_INDEX_URL_SLASH leftover official 200 leftover official $GET_ACCOUNT_BY_SIGNER_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-account-transfer-history (leftover official $ACCOUNT_TRANSFER_HISTORY_INDEX_URL leftover official 307 leftover official $ACCOUNT_TRANSFER_HISTORY_INDEX_URL_SLASH leftover official 200 leftover official $ACCOUNT_TRANSFER_HISTORY_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/data-inspection/get-account-transfer-history (leftover official $GET_ACCOUNT_TRANSFER_HISTORY_INDEX_URL leftover official 307 leftover official $GET_ACCOUNT_TRANSFER_HISTORY_INDEX_URL_SLASH leftover official 200 leftover official $GET_ACCOUNT_TRANSFER_HISTORY_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/account-creation/open-strategy (leftover official $OPEN_STRATEGY_INDEX_URL leftover official 307 leftover official $OPEN_STRATEGY_INDEX_URL_SLASH leftover official 200 leftover official $OPEN_STRATEGY_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover register leftover official leftover login leftover official leftover open leftover official leftover strategy leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/account-creation/transfer-fee-strategy (leftover official $TRANSFER_FEE_STRATEGY_INDEX_URL leftover official 307 leftover official $TRANSFER_FEE_STRATEGY_INDEX_URL_SLASH leftover official 200 leftover official $TRANSFER_FEE_STRATEGY_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover register leftover official leftover transfer leftover official leftover fee leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/account-creation/transfer-open-strategy (leftover official $TRANSFER_OPEN_STRATEGY_INDEX_URL leftover official 307 leftover official $TRANSFER_OPEN_STRATEGY_INDEX_URL_SLASH leftover official 200 leftover official $TRANSFER_OPEN_STRATEGY_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover register leftover official leftover transfer leftover official leftover open leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/account-creation/transfer-subscription-strategy (leftover official $TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_URL leftover official 307 leftover official $TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_URL_SLASH leftover official 200 leftover official $TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover register leftover official leftover transfer leftover official leftover subscription leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/cli/run-operations (leftover official $RUN_OPERATIONS_INDEX_URL leftover official 307 leftover official $RUN_OPERATIONS_INDEX_URL_SLASH leftover official 200 leftover official $RUN_OPERATIONS_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover chr leftover official leftover tx leftover official leftover --secret leftover official leftover this leftover official leftover signs leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover skip leftover official leftover this leftover official leftover signs leftover official leftover skip leftover official leftover demo leftover official leftover script leftover official leftover tab.
        Leftover official leftover BUILD cookbook/cli/run-queries (leftover official $RUN_QUERIES_INDEX_URL leftover official 307 leftover official $RUN_QUERIES_INDEX_URL_SLASH leftover official 200 leftover official $RUN_QUERIES_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover chr leftover official leftover query leftover official leftover curl leftover official leftover localhost:7740 leftover official leftover hello_world leftover official leftover skip leftover official leftover --local leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover skip leftover official leftover run-operations leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official leftover BUILD cookbook/cli/run-tests (leftover official $RUN_TESTS_INDEX_URL leftover official 307 leftover official $RUN_TESTS_INDEX_URL_SLASH leftover official 200 leftover official $RUN_TESTS_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover chr leftover official leftover test leftover official leftover @test leftover official leftover module leftover official leftover assert_equals leftover official leftover assert_true leftover official leftover assert_false leftover official leftover assert_lt leftover official leftover assert_gt leftover official leftover assert_fails leftover official leftover skip leftover official leftover --verbose leftover official leftover skip leftover official leftover --sql-log leftover official leftover on leftover official leftover chr leftover official leftover test leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover skip leftover official leftover run-operations leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover rell.test.keypairs leftover official leftover NAMES leftover official leftover only leftover official leftover skip leftover official leftover printed leftover official leftover sample leftover official leftover key leftover official leftover material.
        Leftover official leftover BUILD cookbook/cli/create-rell-dapp (leftover official $CREATE_RELL_DAPP_INDEX_URL leftover official 307 leftover official $CREATE_RELL_DAPP_INDEX_URL_SLASH leftover official 200 leftover official $CREATE_RELL_DAPP_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skippedPages leftover official leftover HELP ONLY leftover official leftover chr leftover official leftover create-rell-dapp leftover official leftover my-rell-dapp leftover official leftover chromia.yml leftover official leftover src/main.rell leftover official leftover src/test leftover official leftover hello_world leftover official leftover Hello World! leftover official leftover chr leftover official leftover query leftover official leftover hello_world leftover official leftover curl leftover official leftover localhost:7740 leftover official leftover skip leftover official leftover --local leftover official leftover skip leftover official leftover sample leftover official leftover BRID leftover official leftover hex leftover official leftover skip leftover official leftover run-operations leftover official leftover skip leftover official leftover test.timeout leftover official leftover test.parallel leftover official leftover database.schema_version leftover official leftover build.output_dir leftover official leftover build.optimize leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover Do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs.
        Leftover official BUILD cookbook run-operations (leftover official $RUN_OPERATIONS_URL leftover official 307 leftover official $RUN_OPERATIONS_URL_SLASH leftover official 200 leftover official $RUN_OPERATIONS_TITLE): leftover official Operations leftover official transactions leftover official modify leftover official blockchain leftover official state leftover official chr tx leftover official --secret leftover official this signs leftover official --await leftover official --nop leftover official --blockchain-rid leftover official <BlockchainRID> leftover official --network leftover official testnet leftover official --blockchain leftover official my_rell_dapp leftover official --local leftover official create_book leftover official create_house leftover official create_company leftover official create_library leftover official skip leftover official keygen leftover official skip leftover official sample leftover official BRID leftover official hex leftover official skip leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex leftover official this signs.
        Leftover official BUILD cookbook get-account-balance (leftover official $GET_ACCOUNT_BALANCE_URL leftover official 307 leftover official $GET_ACCOUNT_BALANCE_URL_SLASH leftover official 200 leftover official $GET_ACCOUNT_BALANCE_TITLE): leftover official JS/TS leftover official client leftover official tab leftover official $GET_ACCOUNT_BALANCE_JS_TAB leftover official $ACCOUNT_BY_ID_FT4 leftover official $BLOCK_DATA_NPM leftover official $ACCOUNT_BY_ID_FT4_CLIENT_URL leftover official $MEMO_ASSET_MGMT_URL leftover official $ACCOUNT_BY_ID_ACCOUNT_MGMT_URL leftover official $JS_CLIENT_URL leftover official getBalanceByAccountId leftover official Get account balance demo leftover official EVM key pair leftover official query-only leftover official demo leftover official script leftover official tab leftover official skipped leftover official skip leftover official sample leftover official keys leftover official skip leftover official live leftover official sign leftover official --blockchain-rid leftover official <BlockchainRID> leftover official no leftover official invented leftover official 64-hex.
        Leftover official leftover GET-STARTED get-started/use-cases/real-time-data/stork INDEX (leftover official $GET_STARTED_STORK_INDEX_URL leftover official 307 leftover official $GET_STARTED_STORK_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_STORK_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover ECOSYSTEM ecosystem/providers/container-management/transfer-container-ownership INDEX (leftover official $ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/book-review/input-verification INDEX (leftover official $LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-nft/randomness INDEX (leftover official $LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/DeFi INDEX (leftover official $LEARN_TAGS_DEFI_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_DEFI_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_DEFI_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", OVERVIEW_URL)
        put("query_command", QUERY_COMMAND_URL)
        put("test_command", TEST_COMMAND_URL)
        put("rell_tests", RELL_TESTS_URL)
        put("best_practices", BEST_PRACTICES_URL)
        put(
            "pages",
            buildJsonObject {
                put(
                    "included",
                    buildJsonArray { includedPages.forEach { add(JsonPrimitive(it)) } }
                )
                put(
                    "skipped_sign_or_key",
                    buildJsonArray { skippedPages.forEach { add(JsonPrimitive(it)) } }
                )
            }
        )
        put(
            "commands",
            buildJsonObject {
                put("query_local_default", "chr query hello_world")
                put("query_local_brid", "chr query --blockchain-rid <BlockchainRID> hello_world")
                put(
                    "query_rest",
                    "curl -X GET 'localhost:7740/query/<BlockchainRID>?type=hello_world'"
                )
                put(
                    "query_named",
                    "chr query --network testnet --blockchain my_rell_dapp hello_world"
                )
                put("query_args", "chr query hello_world foo=17 bar=hello 'baz=\"hello world\"'")
                put("test", "chr test")
                put("test_modules", "chr test --modules test.data_test")
                put("test_file", "chr test --file=<path>")
                put("test_tests", "chr test --tests my_filter")
                put("js_query", "client.query(\"hello_world\")")
                put("js_query_args", "client.query(\"get_foobar\", { foo: 1, bar: 2 })")
            }
        )
        put("rell_test", rellTestExample())
        put("rell_test_tx_example", rellTestTxExample())
        put("rell_test_must_fail_example", rellTestMustFailExample())
        put("rell_test_disabled_example", rellTestDisabledExample())
        put("rell_test_builders", buildJsonArray { rellTestBuilders.forEach { add(JsonPrimitive(it)) } })
        put("rell_test_asserts", buildJsonArray { rellTestAsserts.forEach { add(JsonPrimitive(it)) } })
        put("rell_test_time", buildJsonArray { rellTestTime.forEach { add(JsonPrimitive(it)) } })
        put("rell_test_other", buildJsonArray { rellTestOther.forEach { add(JsonPrimitive(it)) } })
        put("tx_status_url", TX_STATUS_URL)
        put("tx_status_url_slash", TX_STATUS_URL_SLASH)
        put("tx_status_index_docs", TX_STATUS_INDEX_URL)
        put("tx_status_index_url_slash", TX_STATUS_INDEX_URL_SLASH)
        put("tx_status_index_title", TX_STATUS_INDEX_TITLE)
        put("tx_data_url", TX_DATA_URL)
        put("tx_data_url_slash", TX_DATA_URL_SLASH)
        put("tx_data_title", TX_DATA_TITLE)
        put("tx_data_js_tab", TX_DATA_JS_TAB)
        put("tx_data_js_reference", TX_DATA_JS_REFERENCE)
        put("tx_data_index_docs", TX_DATA_INDEX_URL)
        put("tx_data_index_url_slash", TX_DATA_INDEX_URL_SLASH)
        put("tx_data_index_title", TX_DATA_INDEX_TITLE)
        put("block_data_url", BLOCK_DATA_URL)
        put("block_data_url_slash", BLOCK_DATA_URL_SLASH)
        put("block_data_title", BLOCK_DATA_TITLE)
        put("block_data_js_tab", BLOCK_DATA_JS_TAB)
        put("block_data_npm", BLOCK_DATA_NPM)
        put("block_data_index_docs", BLOCK_DATA_INDEX_URL)
        put("block_data_index_url_slash", BLOCK_DATA_INDEX_URL_SLASH)
        put("block_data_index_title", BLOCK_DATA_INDEX_TITLE)
        put("get_block_data_index_docs", GET_BLOCK_DATA_INDEX_URL)
        put("get_block_data_index_url_slash", GET_BLOCK_DATA_INDEX_URL_SLASH)
        put("get_block_data_index_title", GET_BLOCK_DATA_INDEX_TITLE)
        put("account_by_id_url", ACCOUNT_BY_ID_URL)
        put("account_by_id_url_slash", ACCOUNT_BY_ID_URL_SLASH)
        put("account_by_id_title", ACCOUNT_BY_ID_TITLE)
        put("account_by_id_js_tab", ACCOUNT_BY_ID_JS_TAB)
        put("account_by_id_ft4", ACCOUNT_BY_ID_FT4)
        put("account_by_id_ft4_client_url", ACCOUNT_BY_ID_FT4_CLIENT_URL)
        put("account_by_id_account_mgmt_url", ACCOUNT_BY_ID_ACCOUNT_MGMT_URL)
        put("account_by_id_index_docs", ACCOUNT_BY_ID_INDEX_URL)
        put("account_by_id_index_url_slash", ACCOUNT_BY_ID_INDEX_URL_SLASH)
        put("account_by_id_index_title", ACCOUNT_BY_ID_INDEX_TITLE)
        put("get_account_by_id_index_docs", GET_ACCOUNT_BY_ID_INDEX_URL)
        put("get_account_by_id_index_url_slash", GET_ACCOUNT_BY_ID_INDEX_URL_SLASH)
        put("get_account_by_id_index_title", GET_ACCOUNT_BY_ID_INDEX_TITLE)
        put("account_by_signer_url", ACCOUNT_BY_SIGNER_URL)
        put("account_by_signer_url_slash", ACCOUNT_BY_SIGNER_URL_SLASH)
        put("account_by_signer_title", ACCOUNT_BY_SIGNER_TITLE)
        put("account_by_signer_js_tab", ACCOUNT_BY_SIGNER_JS_TAB)
        put("account_by_signer_index_docs", ACCOUNT_BY_SIGNER_INDEX_URL)
        put("account_by_signer_index_url_slash", ACCOUNT_BY_SIGNER_INDEX_URL_SLASH)
        put("account_by_signer_index_title", ACCOUNT_BY_SIGNER_INDEX_TITLE)
        put("get_account_by_signer_index_docs", GET_ACCOUNT_BY_SIGNER_INDEX_URL)
        put("get_account_by_signer_index_url_slash", GET_ACCOUNT_BY_SIGNER_INDEX_URL_SLASH)
        put("get_account_by_signer_index_title", GET_ACCOUNT_BY_SIGNER_INDEX_TITLE)
        put("account_transfer_history_url", ACCOUNT_TRANSFER_HISTORY_URL)
        put("account_transfer_history_url_slash", ACCOUNT_TRANSFER_HISTORY_URL_SLASH)
        put("account_transfer_history_title", ACCOUNT_TRANSFER_HISTORY_TITLE)
        put("account_transfer_history_js_tab", ACCOUNT_TRANSFER_HISTORY_JS_TAB)
        put("account_transfer_history_index_docs", ACCOUNT_TRANSFER_HISTORY_INDEX_URL)
        put("account_transfer_history_index_url_slash", ACCOUNT_TRANSFER_HISTORY_INDEX_URL_SLASH)
        put("account_transfer_history_index_title", ACCOUNT_TRANSFER_HISTORY_INDEX_TITLE)
        put("get_account_transfer_history_index_docs", GET_ACCOUNT_TRANSFER_HISTORY_INDEX_URL)
        put("get_account_transfer_history_index_url_slash", GET_ACCOUNT_TRANSFER_HISTORY_INDEX_URL_SLASH)
        put("get_account_transfer_history_index_title", GET_ACCOUNT_TRANSFER_HISTORY_INDEX_TITLE)
        put("open_strategy_index_docs", OPEN_STRATEGY_INDEX_URL)
        put("open_strategy_index_url_slash", OPEN_STRATEGY_INDEX_URL_SLASH)
        put("open_strategy_index_title", OPEN_STRATEGY_INDEX_TITLE)
        put("transfer_fee_strategy_index_docs", TRANSFER_FEE_STRATEGY_INDEX_URL)
        put("transfer_fee_strategy_index_url_slash", TRANSFER_FEE_STRATEGY_INDEX_URL_SLASH)
        put("transfer_fee_strategy_index_title", TRANSFER_FEE_STRATEGY_INDEX_TITLE)
        put("transfer_open_strategy_index_docs", TRANSFER_OPEN_STRATEGY_INDEX_URL)
        put("transfer_open_strategy_index_url_slash", TRANSFER_OPEN_STRATEGY_INDEX_URL_SLASH)
        put("transfer_open_strategy_index_title", TRANSFER_OPEN_STRATEGY_INDEX_TITLE)
        put("transfer_subscription_strategy_index_docs", TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_URL)
        put("transfer_subscription_strategy_index_url_slash", TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_URL_SLASH)
        put("transfer_subscription_strategy_index_title", TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_TITLE)
        put("pagination_ft4_url", PAGINATION_FT4_URL)
        put("pagination_ft4_url_slash", PAGINATION_FT4_URL_SLASH)
        put("pagination_ft4_title", PAGINATION_FT4_TITLE)
        put("pagination_ft4_js_tab", PAGINATION_FT4_JS_TAB)
        put("pagination_ft4_index_docs", PAGINATION_FT4_INDEX_URL)
        put("pagination_ft4_index_url_slash", PAGINATION_FT4_INDEX_URL_SLASH)
        put("pagination_ft4_index_title", PAGINATION_FT4_INDEX_TITLE)
        put("memo_query_url", MEMO_QUERY_URL)
        put("memo_query_url_slash", MEMO_QUERY_URL_SLASH)
        put("memo_query_title", MEMO_QUERY_TITLE)
        put("memo_query_js_tab", MEMO_QUERY_JS_TAB)
        put("memo_query_index_docs", MEMO_QUERY_INDEX_URL)
        put("memo_query_index_url_slash", MEMO_QUERY_INDEX_URL_SLASH)
        put("memo_query_index_title", MEMO_QUERY_INDEX_TITLE)
        put("memo_asset_mgmt_url", MEMO_ASSET_MGMT_URL)
        put("get_account_balance_url", GET_ACCOUNT_BALANCE_URL)
        put("get_account_balance_url_slash", GET_ACCOUNT_BALANCE_URL_SLASH)
        put("get_account_balance_title", GET_ACCOUNT_BALANCE_TITLE)
        put("get_account_balance_js_tab", GET_ACCOUNT_BALANCE_JS_TAB)
        put("get_account_balance_index_docs", GET_ACCOUNT_BALANCE_INDEX_URL)
        put("get_account_balance_index_url_slash", GET_ACCOUNT_BALANCE_INDEX_URL_SLASH)
        put("get_account_balance_index_title", GET_ACCOUNT_BALANCE_INDEX_TITLE)
        put("pagination_url", PAGINATION_URL)
        put("pagination_url_slash", PAGINATION_URL_SLASH)
        put("pagination_title", PAGINATION_TITLE)
        put("pagination_js_tab", PAGINATION_JS_TAB)
        put("pagination_index_docs", PAGINATION_INDEX_URL)
        put("pagination_index_url_slash", PAGINATION_INDEX_URL_SLASH)
        put("pagination_index_title", PAGINATION_INDEX_TITLE)
        put("make_query_url", MAKE_QUERY_URL)
        put("make_query_url_slash", MAKE_QUERY_URL_SLASH)
        put("make_query_title", MAKE_QUERY_TITLE)
        put("make_query_js_tab", MAKE_QUERY_JS_TAB)
        put("make_query_index_docs", MAKE_QUERY_INDEX_URL)
        put("make_query_index_url_slash", MAKE_QUERY_INDEX_URL_SLASH)
        put("make_query_index_title", MAKE_QUERY_INDEX_TITLE)
        put("run_queries_url", RUN_QUERIES_URL)
        put("run_queries_url_slash", RUN_QUERIES_URL_SLASH)
        put("run_queries_title", RUN_QUERIES_TITLE)
        put("run_queries_index_docs", RUN_QUERIES_INDEX_URL)
        put("run_queries_index_url_slash", RUN_QUERIES_INDEX_URL_SLASH)
        put("run_queries_index_title", RUN_QUERIES_INDEX_TITLE)
        put("run_tests_url", RUN_TESTS_URL)
        put("run_tests_url_slash", RUN_TESTS_URL_SLASH)
        put("run_tests_title", RUN_TESTS_TITLE)
        put("run_tests_index_docs", RUN_TESTS_INDEX_URL)
        put("run_tests_index_url_slash", RUN_TESTS_INDEX_URL_SLASH)
        put("run_tests_index_title", RUN_TESTS_INDEX_TITLE)
        put("run_operations_url", RUN_OPERATIONS_URL)
        put("run_operations_url_slash", RUN_OPERATIONS_URL_SLASH)
        put("run_operations_title", RUN_OPERATIONS_TITLE)
        put("run_operations_index_docs", RUN_OPERATIONS_INDEX_URL)
        put("run_operations_index_url_slash", RUN_OPERATIONS_INDEX_URL_SLASH)
        put("run_operations_index_title", RUN_OPERATIONS_INDEX_TITLE)
        put("create_rell_dapp_url", CREATE_RELL_DAPP_URL)
        put("create_rell_dapp_url_slash", CREATE_RELL_DAPP_URL_SLASH)
        put("create_rell_dapp_title", CREATE_RELL_DAPP_TITLE)
        put("create_rell_dapp_index_docs", CREATE_RELL_DAPP_INDEX_URL)
        put("create_rell_dapp_index_url_slash", CREATE_RELL_DAPP_INDEX_URL_SLASH)
        put("create_rell_dapp_index_title", CREATE_RELL_DAPP_INDEX_TITLE)
        put("overview_url", OVERVIEW_URL)
        put("overview_url_slash", OVERVIEW_URL_SLASH)
        put("overview_title", OVERVIEW_TITLE)
        put("overview_index_docs", OVERVIEW_INDEX_URL)
        put("overview_index_url_slash", OVERVIEW_INDEX_URL_SLASH)
        put("overview_index_title", OVERVIEW_INDEX_TITLE)
        put("cli_url", CLI_URL)
        put("cli_url_slash", CLI_URL_SLASH)
        put("cli_title", CLI_TITLE)
        put("cli_index_docs", CLI_INDEX_URL)
        put("cli_index_url_slash", CLI_INDEX_URL_SLASH)
        put("cli_index_title", CLI_INDEX_TITLE)
        put("query_creation_url", QUERY_CREATION_URL)
        put("query_creation_url_slash", QUERY_CREATION_URL_SLASH)
        put("query_creation_title", QUERY_CREATION_TITLE)
        put("query_creation_index_docs", QUERY_CREATION_INDEX_URL)
        put("query_creation_index_url_slash", QUERY_CREATION_INDEX_URL_SLASH)
        put("query_creation_index_title", QUERY_CREATION_INDEX_TITLE)
        put("data_inspection_url", DATA_INSPECTION_URL)
        put("data_inspection_url_slash", DATA_INSPECTION_URL_SLASH)
        put("data_inspection_title", DATA_INSPECTION_TITLE)
        put("data_inspection_index_docs", DATA_INSPECTION_INDEX_URL)
        put("data_inspection_index_url_slash", DATA_INSPECTION_INDEX_URL_SLASH)
        put("data_inspection_index_title", DATA_INSPECTION_INDEX_TITLE)
        put("account_creation_url", ACCOUNT_CREATION_URL)
        put("account_creation_url_slash", ACCOUNT_CREATION_URL_SLASH)
        put("account_creation_title", ACCOUNT_CREATION_TITLE)
        put("account_creation_index_docs", ACCOUNT_CREATION_INDEX_URL)
        put("account_creation_index_url_slash", ACCOUNT_CREATION_INDEX_URL_SLASH)
        put("account_creation_index_title", ACCOUNT_CREATION_INDEX_TITLE)
        put("transaction_creation_url", TRANSACTION_CREATION_URL)
        put("transaction_creation_url_slash", TRANSACTION_CREATION_URL_SLASH)
        put("transaction_creation_title", TRANSACTION_CREATION_TITLE)
        put("transaction_creation_index_docs", TRANSACTION_CREATION_INDEX_URL)
        put("transaction_creation_index_url_slash", TRANSACTION_CREATION_INDEX_URL_SLASH)
        put("transaction_creation_index_title", TRANSACTION_CREATION_INDEX_TITLE)
        put("simple_transaction_url", SIMPLE_TRANSACTION_URL)
        put("simple_transaction_url_slash", SIMPLE_TRANSACTION_URL_SLASH)
        put("simple_transaction_title", SIMPLE_TRANSACTION_TITLE)
        put("simple_transaction_js_tab", SIMPLE_TRANSACTION_JS_TAB)
        put("simple_transaction_index_docs", SIMPLE_TRANSACTION_INDEX_URL)
        put("simple_transaction_index_url_slash", SIMPLE_TRANSACTION_INDEX_URL_SLASH)
        put("simple_transaction_index_title", SIMPLE_TRANSACTION_INDEX_TITLE)
        put("make_transfer_index_docs", MAKE_TRANSFER_INDEX_URL)
        put("make_transfer_index_url_slash", MAKE_TRANSFER_INDEX_URL_SLASH)
        put("make_transfer_index_title", MAKE_TRANSFER_INDEX_TITLE)
        put("enable_disable_memo_index_docs", ENABLE_DISABLE_MEMO_INDEX_URL)
        put("enable_disable_memo_index_url_slash", ENABLE_DISABLE_MEMO_INDEX_URL_SLASH)
        put("enable_disable_memo_index_title", ENABLE_DISABLE_MEMO_INDEX_TITLE)
        put("transfer_with_memo_index_docs", TRANSFER_WITH_MEMO_INDEX_URL)
        put("transfer_with_memo_index_url_slash", TRANSFER_WITH_MEMO_INDEX_URL_SLASH)
        put("transfer_with_memo_index_title", TRANSFER_WITH_MEMO_INDEX_TITLE)
        put("time_bound_transactions_index_docs", TIME_BOUND_TRANSACTIONS_INDEX_URL)
        put("time_bound_transactions_index_url_slash", TIME_BOUND_TRANSACTIONS_INDEX_URL_SLASH)
        put("time_bound_transactions_index_title", TIME_BOUND_TRANSACTIONS_INDEX_TITLE)
        put("time_bound_index_docs", TIME_BOUND_INDEX_URL)
        put("time_bound_index_url_slash", TIME_BOUND_INDEX_URL_SLASH)
        put("time_bound_index_title", TIME_BOUND_INDEX_TITLE)
        put("call_operation_ft4_auth_index_docs", CALL_OPERATION_FT4_AUTH_INDEX_URL)
        put("call_operation_ft4_auth_index_url_slash", CALL_OPERATION_FT4_AUTH_INDEX_URL_SLASH)
        put("call_operation_ft4_auth_index_title", CALL_OPERATION_FT4_AUTH_INDEX_TITLE)
        put("call_op_ft4_index_docs", CALL_OP_FT4_INDEX_URL)
        put("call_op_ft4_index_url_slash", CALL_OP_FT4_INDEX_URL_SLASH)
        put("call_op_ft4_index_title", CALL_OP_FT4_INDEX_TITLE)
        put("register_crosschain_asset_index_docs", REGISTER_CROSSCHAIN_ASSET_INDEX_URL)
        put("register_crosschain_asset_index_url_slash", REGISTER_CROSSCHAIN_ASSET_INDEX_URL_SLASH)
        put("register_crosschain_asset_index_title", REGISTER_CROSSCHAIN_ASSET_INDEX_TITLE)
        put("register_asset_index_docs", REGISTER_ASSET_INDEX_URL)
        put("register_asset_index_url_slash", REGISTER_ASSET_INDEX_URL_SLASH)
        put("register_asset_index_title", REGISTER_ASSET_INDEX_TITLE)
        put("crosschain_transfer_index_docs", CROSSCHAIN_TRANSFER_INDEX_URL)
        put("crosschain_transfer_index_url_slash", CROSSCHAIN_TRANSFER_INDEX_URL_SLASH)
        put("crosschain_transfer_index_title", CROSSCHAIN_TRANSFER_INDEX_TITLE)
        put(
            "tx_statuses",
            buildJsonArray { leftoverOfficialTxStatuses.forEach { add(JsonPrimitive(it)) } }
        )
        put("hello_world_query", "hello_world")
        put("hello_world_result", "Hello World!")
        put(
            "skipped_flags_and_keys",
            buildJsonArray {
                listOf(
                    "chr query --local  # not on official query command page",
                    "chr test --sql-log  # removed in CLI 0.31.0; use chr repl --sql-log",
                    "chr test --verbose  # not on official test command page",
                    "test.timeout  # not official chromia.yml",
                    "test.parallel  # not official chromia.yml",
                    "database.schema_version  # not official chromia.yml",
                    "build.output_dir  # not official chromia.yml",
                    "build.optimize  # not official chromia.yml",
                    "official printed sample test key material (never production)"
                ).forEach { add(JsonPrimitive(it)) }
            }
        )
        put("client_help", "chr_generate_client_help")
        put("query_help", "chr_query_help")
        put("build_help", "chr_build_help")
        put("get_started_stork_index_docs", GET_STARTED_STORK_INDEX_URL)
        put("get_started_stork_index_url_slash", GET_STARTED_STORK_INDEX_URL_SLASH)
        put("get_started_stork_index_title", GET_STARTED_STORK_INDEX_TITLE)
        put("ecosystem_transfer_container_ownership_index_url_slash", ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_URL_SLASH)
        put("ecosystem_transfer_container_ownership_index_title", ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_TITLE)
        put("learn_book_review_input_verification_index_url_slash", LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL_SLASH)
        put("learn_book_review_input_verification_index_title", LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_TITLE)
        put("learn_marketplace_nft_randomness_index_url_slash", LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_URL_SLASH)
        put("learn_marketplace_nft_randomness_index_title", LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_TITLE)
        put("learn_tags_defi_index_url_slash", LEARN_TAGS_DEFI_INDEX_URL_SLASH)
        put("learn_tags_defi_index_title", LEARN_TAGS_DEFI_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover BUILD cookbook/overview leftovers encoded as OVERVIEW_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/cli leftovers encoded as CLI_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/query-creation leftovers encoded as QUERY_CREATION_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/query-creation/make-query leftovers encoded as MAKE_QUERY_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/query-creation/check-account-memo-requirement leftovers encoded as MEMO_QUERY_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/query-creation/get-account-balance leftovers encoded as GET_ACCOUNT_BALANCE_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/query-creation/pagination-with-ft4 leftovers encoded as PAGINATION_FT4_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/query-creation/pagination leftovers encoded as PAGINATION_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection leftovers encoded as DATA_INSPECTION_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/account-creation leftovers encoded as ACCOUNT_CREATION_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation leftovers encoded as TRANSACTION_CREATION_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/simple-transaction leftovers encoded as SIMPLE_TRANSACTION_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-transaction-status leftovers encoded as TX_STATUS_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/make-transfer leftovers encoded as MAKE_TRANSFER_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/enable-disable-memo leftovers encoded as ENABLE_DISABLE_MEMO_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/transfer-with-memo leftovers encoded as TRANSFER_WITH_MEMO_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/time-bound-transactions leftovers encoded as TIME_BOUND_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/call-operation-with-ft4-auth leftovers encoded as CALL_OP_FT4_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/register-crosschain-asset leftovers encoded as REGISTER_CROSSCHAIN_ASSET_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/register-asset leftovers encoded as REGISTER_ASSET_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/transaction-creation/crosschain-transfer leftovers encoded as CROSSCHAIN_TRANSFER_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-transaction-data leftovers encoded as TX_DATA_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-block-data leftovers encoded as BLOCK_DATA_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-block-data leftovers encoded as GET_BLOCK_DATA_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-account-by-id leftovers encoded as ACCOUNT_BY_ID_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-account-by-id leftovers encoded as GET_ACCOUNT_BY_ID_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-account-by-signer leftovers encoded as ACCOUNT_BY_SIGNER_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-account-by-signer leftovers encoded as GET_ACCOUNT_BY_SIGNER_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-account-transfer-history leftovers encoded as ACCOUNT_TRANSFER_HISTORY_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/data-inspection/get-account-transfer-history leftovers encoded as GET_ACCOUNT_TRANSFER_HISTORY_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/account-creation/open-strategy leftovers encoded as OPEN_STRATEGY_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/account-creation/transfer-fee-strategy leftovers encoded as TRANSFER_FEE_STRATEGY_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/account-creation/transfer-open-strategy leftovers encoded as TRANSFER_OPEN_STRATEGY_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/account-creation/transfer-subscription-strategy leftovers encoded as TRANSFER_SUBSCRIPTION_STRATEGY_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/cli/run-operations leftovers encoded as RUN_OPERATIONS_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/cli/run-queries leftovers encoded as RUN_QUERIES_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/cli/run-tests leftovers encoded as RUN_TESTS_INDEX_* (query-only).
// Leftover official leftover BUILD cookbook/cli/create-rell-dapp leftovers encoded as CREATE_RELL_DAPP_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/use-cases/real-time-data/stork INDEX leftovers encoded as GET_STARTED_STORK_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/container-management/transfer-container-ownership INDEX leftovers encoded as ECOSYSTEM_TRANSFER_CONTAINER_OWNERSHIP_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/book-review/input-verification INDEX leftovers encoded as LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/module-nft/randomness INDEX leftovers encoded as LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/DeFi INDEX leftovers encoded as LEARN_TAGS_DEFI_INDEX_* (query-only HELP ONLY WRITE SKIP).
