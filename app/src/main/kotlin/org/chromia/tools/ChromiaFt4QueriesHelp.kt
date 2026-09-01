package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official FT4 v1.1.0r / API 1 read-only query catalog.
 * Names and parameters from gitlab.com/chromaway/ft4-lib tag v1.1.0r plus official
 * docs.chromia.com/build/ft4/pagination, setup, memo-guide, /build/ft4/prioritization, /build/ft4/terms, /build/ft4/intro, /build/ft4/client.
 * Queries / config read-only only. Never emits admin / ras_open / register / transfer / auth write paths.
 * Official BUILD ft4/intro leftovers live here (query-only).
 * Official BUILD ft4/intro INDEX leftovers also live here (query-only).
 * Official BUILD ft4/setup leftovers also live here (query-only).
 * Official BUILD ft4/client leftovers also live here (query-only).
 * Official BUILD ft4/backend leftovers also live here (query-only).
 * Official BUILD ft4/account-management leftovers also live here (query-only).
 * Official BUILD ft4/account-management INDEX leftovers also live here (query-only).
 * Official BUILD ft4/asset-management leftovers also live here (query-only).
 * Official BUILD ft4/asset-management INDEX leftovers also live here (query-only).
 * Official BUILD ft4/code-examples leftovers also live here (query-only).
 * Official BUILD ft4/code-examples INDEX leftovers also live here (query-only).
 * Official BUILD ft4/pagination leftovers also live here (query-only).
 * Official BUILD ft4/prioritization leftovers also live here (query-only).
 * Official BUILD ft4/configuration-values leftovers also live here (query-only).
 * Official BUILD ft4/configuration-values INDEX leftovers also live here (query-only).
 * Official BUILD ft4/releases/ft4 leftovers also live here (query-only).
 * Official BUILD ft4/terms leftovers also live here (query-only).
 * Official BUILD ft4/setup/ft4-setup leftovers also live here (query-only).
 * Official BUILD ft4/setup/ft4-setup INDEX leftovers also live here (query-only).
 * Official BUILD ft4/setup/imports leftovers also live here (query-only).
 * Official BUILD ft4/client/client-setup leftovers also live here (query-only).
 * Official BUILD ft4/client/client-auth-descriptors leftovers also live here (query-only).
 * Official BUILD ft4/client/client-account-registration leftovers also live here (query-only).
 * Official BUILD ft4/client/client-login leftovers also live here (query-only).
 * Official BUILD ft4/client/client-key-store leftovers also live here (query-only).
 * Official BUILD ft4/client/client-transfer-assets leftovers also live here (query-only).
 * Official BUILD ft4/client/client-orchestrator leftovers also live here (query-only).
 * Official BUILD ft4/backend/accounts leftovers also live here (query-only).
 * Official BUILD ft4/backend/authentication leftovers also live here (query-only).
 * Official BUILD ft4/backend/assets leftovers also live here (query-only).
 * Official BUILD ft4/backend/cross-chain leftovers also live here (query-only).
 * Official BUILD ft4/backend/cross-chain/introduction leftovers also live here (query-only).
 * Official BUILD ft4/backend/cross-chain/automate-cross-chain-asset-registration INDEX leftovers also live here (query-only).
 * Official BUILD ft4/account-management/overview leftovers also live here (query-only).
 * Official BUILD ft4/account-management/auth-descriptors leftovers also live here (query-only).
 * Official BUILD ft4/account-management/multisig leftovers also live here (query-only).
 * Official BUILD ft4/asset-management/asset leftovers also live here (query-only).
 * Official BUILD ft4/asset-management/transfer-assets leftovers also live here (query-only).
 * Official GET-STARTED get-started/about/protocols/ft4 INDEX leftovers also live here (query-only).
 * Official GET-STARTED get-started/about/protocols INDEX leftovers also live here (query-only).
 */
object ChromiaFt4QueriesHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val FT4_VERSION = DappScaffold.FT4_VERSION
    const val FT4_API = DappScaffold.FT4_API
    const val TOOL_NAME = "chromia_ft4_queries_help"
    const val PAGINATION_URL = "https://docs.chromia.com/build/ft4/pagination"
    const val PAGINATION_INDEX_URL = PAGINATION_URL
    const val PAGINATION_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/pagination/"
    const val PAGINATION_INDEX_TITLE = "Pagination"
    const val SETUP_URL = "https://docs.chromia.com/build/ft4/setup/ft4-setup"
    const val FT4_SETUP_PAGE_INDEX_URL = SETUP_URL
    const val FT4_SETUP_PAGE_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/setup/ft4-setup/"
    const val FT4_SETUP_PAGE_INDEX_TITLE = "Set up your FT4 project"
    const val FT4_SETUP_INDEX_URL = FT4_SETUP_PAGE_INDEX_URL
    const val FT4_SETUP_INDEX_URL_SLASH = FT4_SETUP_PAGE_INDEX_URL_SLASH
    const val FT4_SETUP_INDEX_TITLE = FT4_SETUP_PAGE_INDEX_TITLE
    const val CLIENT_SETUP_URL = "https://docs.chromia.com/build/ft4/client/client-setup"
    const val CLIENT_SETUP_INDEX_URL = CLIENT_SETUP_URL
    const val CLIENT_SETUP_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/client/client-setup/"
    const val CLIENT_SETUP_INDEX_TITLE = "FT4 client setup with TypeScript"
    const val CLIENT_INDEX_URL = "https://docs.chromia.com/build/ft4/client"
    const val CLIENT_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/client/"
    const val CLIENT_INDEX_TITLE = "Client"
    const val CLIENT_INDEX_CARD_SETUP = "Set up FT4 client"
    const val CLIENT_INDEX_CARD_AUTH = "Use auth descriptors"
    const val CLIENT_INDEX_CARD_REGISTER = "Register an account"
    const val CLIENT_INDEX_CARD_LOGIN = "Implement user login"
    const val CLIENT_INDEX_CARD_KEY_STORE = "Manage key stores"
    const val CLIENT_INDEX_CARD_TRANSFER = "Transfer assets with FT4 client"
    const val CLIENT_INDEX_CARD_ORCHESTRATOR = "Perform cross-chain transfers"
    const val CLIENT_AUTH_DESCRIPTORS_URL = "https://docs.chromia.com/build/ft4/client/client-auth-descriptors"
    const val CLIENT_AUTH_DESCRIPTORS_INDEX_URL = CLIENT_AUTH_DESCRIPTORS_URL
    const val CLIENT_AUTH_DESCRIPTORS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/client/client-auth-descriptors/"
    const val CLIENT_AUTH_DESCRIPTORS_INDEX_TITLE = "Use auth descriptors"
    const val CLIENT_ACCOUNT_REGISTRATION_URL = "https://docs.chromia.com/build/ft4/client/client-account-registration"
    const val CLIENT_ACCOUNT_REGISTRATION_INDEX_URL = CLIENT_ACCOUNT_REGISTRATION_URL
    const val CLIENT_ACCOUNT_REGISTRATION_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/client/client-account-registration/"
    const val CLIENT_ACCOUNT_REGISTRATION_INDEX_TITLE = "Register an account"
    const val CLIENT_LOGIN_URL = "https://docs.chromia.com/build/ft4/client/client-login"
    const val CLIENT_LOGIN_INDEX_URL = CLIENT_LOGIN_URL
    const val CLIENT_LOGIN_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/client/client-login/"
    const val CLIENT_LOGIN_INDEX_TITLE = "Disposable keys and login management"
    const val CLIENT_KEY_STORE_URL = "https://docs.chromia.com/build/ft4/client/client-key-store"
    const val CLIENT_KEY_STORE_INDEX_URL = CLIENT_KEY_STORE_URL
    const val CLIENT_KEY_STORE_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/client/client-key-store/"
    const val CLIENT_KEY_STORE_INDEX_TITLE = "Manage key stores"
    const val CLIENT_TRANSFER_ASSETS_URL = "https://docs.chromia.com/build/ft4/client/client-transfer-assets"
    const val CLIENT_TRANSFER_ASSETS_INDEX_URL = CLIENT_TRANSFER_ASSETS_URL
    const val CLIENT_TRANSFER_ASSETS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/client/client-transfer-assets/"
    const val CLIENT_TRANSFER_ASSETS_INDEX_TITLE = "Transfer assets with FT4 client"
    const val CLIENT_ORCHESTRATOR_URL = "https://docs.chromia.com/build/ft4/client/client-orchestrator"
    const val CLIENT_ORCHESTRATOR_INDEX_URL = CLIENT_ORCHESTRATOR_URL
    const val CLIENT_ORCHESTRATOR_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/client/client-orchestrator/"
    const val CLIENT_ORCHESTRATOR_INDEX_TITLE = "Perform cross-chain transfers"
    const val BACKEND_INDEX_URL = "https://docs.chromia.com/build/ft4/backend"
    const val BACKEND_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/"
    const val BACKEND_INDEX_TITLE = "FT4 Backend"
    const val BACKEND_INDEX_CARD_ACCOUNTS = "Accounts"
    const val BACKEND_INDEX_CARD_AUTHENTICATION = "Authentication"
    const val BACKEND_INDEX_CARD_ASSETS = "Assets"
    const val BACKEND_INDEX_CARD_CROSS_CHAIN = "Cross-chain"
    const val BACKEND_ACCOUNTS_URL = "https://docs.chromia.com/build/ft4/backend/accounts/"
    const val BACKEND_ACCOUNTS_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/accounts"
    const val BACKEND_ACCOUNTS_INDEX_URL_SLASH = BACKEND_ACCOUNTS_URL
    const val BACKEND_ACCOUNTS_INDEX_TITLE = "Accounts"
    const val BACKEND_ACCOUNT_LINKING_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/accounts/account-linking"
    const val BACKEND_ACCOUNT_LINKING_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/accounts/account-linking/"
    const val BACKEND_ACCOUNT_LINKING_INDEX_TITLE = "Link FT4 accounts"
    const val BACKEND_ACCOUNTS_OVERVIEW_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/accounts/overview"
    const val BACKEND_ACCOUNTS_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/accounts/overview/"
    const val BACKEND_ACCOUNTS_OVERVIEW_INDEX_TITLE = "Register FT4 accounts"
    const val BACKEND_ACCOUNTS_OPEN_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/accounts/open"
    const val BACKEND_ACCOUNTS_OPEN_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/accounts/open/"
    const val BACKEND_ACCOUNTS_OPEN_INDEX_TITLE = "Transfer open strategy"
    const val BACKEND_ACCOUNTS_FIXED_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/accounts/fixed"
    const val BACKEND_ACCOUNTS_FIXED_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/accounts/fixed/"
    const val BACKEND_ACCOUNTS_FIXED_INDEX_TITLE = "Transfer fee strategy"
    const val BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/accounts/subscription"
    const val BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/accounts/subscription/"
    const val BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_TITLE = "Transfer subscription strategy"
    const val BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/accounts/accounts-and-auth-descriptors"
    const val BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/accounts/accounts-and-auth-descriptors/"
    const val BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_TITLE = "Use auth descriptors for accounts"
    const val BACKEND_AUTHENTICATION_URL = "https://docs.chromia.com/build/ft4/backend/authentication/"
    const val BACKEND_AUTHENTICATION_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/authentication"
    const val BACKEND_AUTHENTICATION_INDEX_URL_SLASH = BACKEND_AUTHENTICATION_URL
    const val BACKEND_AUTHENTICATION_INDEX_TITLE = "Authentication"
    const val BACKEND_AUTHENTICATION_AUTH_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/authentication/auth"
    const val BACKEND_AUTHENTICATION_AUTH_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/authentication/auth/"
    const val BACKEND_AUTHENTICATION_AUTH_INDEX_TITLE = "Use auth handlers for authentication"
    const val BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/authentication/auth-descriptors-and-rules"
    const val BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/authentication/auth-descriptors-and-rules/"
    const val BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_TITLE = "Set up auth descriptors"
    const val BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/authentication/multi-sig"
    const val BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/authentication/multi-sig/"
    const val BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_TITLE = "Handle multi-signature transactions"
    const val BACKEND_ASSETS_URL = "https://docs.chromia.com/build/ft4/backend/assets/"
    const val BACKEND_ASSETS_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/assets"
    const val BACKEND_ASSETS_INDEX_URL_SLASH = BACKEND_ASSETS_URL
    const val BACKEND_ASSETS_INDEX_TITLE = "Assets"
    const val BACKEND_ASSETS_REGISTER_ASSETS_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/assets/register-assets"
    const val BACKEND_ASSETS_REGISTER_ASSETS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/assets/register-assets/"
    const val BACKEND_ASSETS_REGISTER_ASSETS_INDEX_TITLE = "Register assets in FT4"
    const val BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/assets/asset-amounts"
    const val BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/assets/asset-amounts/"
    const val BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_TITLE = "Manage asset balances"
    const val BACKEND_ASSETS_LOCKING_ASSETS_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/assets/locking-assets"
    const val BACKEND_ASSETS_LOCKING_ASSETS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/assets/locking-assets/"
    const val BACKEND_ASSETS_LOCKING_ASSETS_INDEX_TITLE = "Lock FT4 assets"
    const val BACKEND_CROSS_CHAIN_URL = "https://docs.chromia.com/build/ft4/backend/cross-chain/"
    const val BACKEND_CROSS_CHAIN_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/cross-chain"
    const val BACKEND_CROSS_CHAIN_INDEX_URL_SLASH = BACKEND_CROSS_CHAIN_URL
    const val BACKEND_CROSS_CHAIN_INDEX_TITLE = "Cross-chain"
    const val BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/cross-chain/introduction"
    const val BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/cross-chain/introduction/"
    const val BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_TITLE = "Get started with cross-chain operations"
    const val BACKEND_CROSS_CHAIN_ASSETS_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/cross-chain/cross-chain-assets"
    const val BACKEND_CROSS_CHAIN_ASSETS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/cross-chain/cross-chain-assets/"
    const val BACKEND_CROSS_CHAIN_ASSETS_INDEX_TITLE = "Manage cross-chain assets"
    const val BACKEND_CROSS_CHAIN_TRANSFERS_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/cross-chain/cross-chain-transfers"
    const val BACKEND_CROSS_CHAIN_TRANSFERS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/cross-chain/cross-chain-transfers/"
    const val BACKEND_CROSS_CHAIN_TRANSFERS_INDEX_TITLE = "Transfer assets across chains"
    const val BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_URL = "https://docs.chromia.com/build/ft4/backend/cross-chain/automate-cross-chain-asset-registration"
    const val BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/backend/cross-chain/automate-cross-chain-asset-registration/"
    const val BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_TITLE = "Automate cross-chain asset registration"
    const val AUTOMATE_CROSSCHAIN_ASSET_INDEX_URL = BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_URL
    const val AUTOMATE_CROSSCHAIN_ASSET_INDEX_URL_SLASH = BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_URL_SLASH
    const val AUTOMATE_CROSSCHAIN_ASSET_INDEX_TITLE = BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_TITLE
    const val MEMO_URL = "https://docs.chromia.com/build/integrations/memo-guide"
    const val PRIORITIZATION_URL = "https://docs.chromia.com/build/ft4/prioritization"
    const val PRIORITIZATION_INDEX_URL = PRIORITIZATION_URL
    const val PRIORITIZATION_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/prioritization/"
    const val PRIORITIZATION_INDEX_TITLE = "Prioritize transactions"
    const val TERMS_URL = "https://docs.chromia.com/build/ft4/terms"
    const val TERMS_INDEX_URL = TERMS_URL
    const val TERMS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/terms/"
    const val TERMS_INDEX_TITLE = "Glossary"
    const val INTRO_URL = "https://docs.chromia.com/build/ft4/intro"
    const val INTRO_URL_SLASH = "https://docs.chromia.com/build/ft4/intro/"
    const val INTRO_TITLE = "FT4 (Flexible Token 4)"
    const val INTRO_INDEX_URL = INTRO_URL
    const val INTRO_INDEX_URL_SLASH = INTRO_URL_SLASH
    const val INTRO_INDEX_TITLE = INTRO_TITLE
    const val INTRO_CARD_ACCOUNT = "FT4 Account Management"
    const val INTRO_CARD_TERMS = "FT4 Glossary"
    const val INTRO_CARD_ACCOUNT_URL = "https://docs.chromia.com/build/ft4/account-management/"
    const val ACCOUNT_MGMT_INDEX_URL = "https://docs.chromia.com/build/ft4/account-management"
    const val ACCOUNT_MGMT_INDEX_URL_SLASH = INTRO_CARD_ACCOUNT_URL
    const val ACCOUNT_MGMT_INDEX_TITLE = "Account management"
    const val ACCOUNT_MANAGEMENT_INDEX_URL = ACCOUNT_MGMT_INDEX_URL
    const val ACCOUNT_MANAGEMENT_INDEX_URL_SLASH = ACCOUNT_MGMT_INDEX_URL_SLASH
    const val ACCOUNT_MANAGEMENT_INDEX_TITLE = ACCOUNT_MGMT_INDEX_TITLE
    const val ACCOUNT_MGMT_INDEX_CARD_MANAGE = "Manage FT4 accounts"
    const val ACCOUNT_MGMT_INDEX_CARD_AUTH = "Auth descriptors"
    const val ACCOUNT_MGMT_INDEX_CARD_MULTISIG = "Multisig"
    const val ACCOUNT_MGMT_OVERVIEW_URL = "https://docs.chromia.com/build/ft4/account-management/overview"
    const val ACCOUNT_MGMT_OVERVIEW_INDEX_URL = ACCOUNT_MGMT_OVERVIEW_URL
    const val ACCOUNT_MGMT_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/account-management/overview/"
    const val ACCOUNT_MGMT_OVERVIEW_INDEX_TITLE = "Manage FT4 accounts"
    const val ACCOUNT_MGMT_AUTH_DESCRIPTORS_URL = "https://docs.chromia.com/build/ft4/account-management/auth-descriptors"
    const val ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_URL = ACCOUNT_MGMT_AUTH_DESCRIPTORS_URL
    const val ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/account-management/auth-descriptors/"
    const val ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_TITLE = "Auth descriptors"
    const val ACCOUNT_MGMT_MULTISIG_URL = "https://docs.chromia.com/build/ft4/account-management/multisig"
    const val ACCOUNT_MGMT_MULTISIG_INDEX_URL = ACCOUNT_MGMT_MULTISIG_URL
    const val ACCOUNT_MGMT_MULTISIG_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/account-management/multisig/"
    const val ACCOUNT_MGMT_MULTISIG_INDEX_TITLE = "Introduction to Multisig"
    const val ASSET_MGMT_INDEX_URL = "https://docs.chromia.com/build/ft4/asset-management"
    const val ASSET_MGMT_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/asset-management/"
    const val ASSET_MGMT_INDEX_TITLE = "Asset management"
    const val ASSET_MANAGEMENT_INDEX_URL = ASSET_MGMT_INDEX_URL
    const val ASSET_MANAGEMENT_INDEX_URL_SLASH = ASSET_MGMT_INDEX_URL_SLASH
    const val ASSET_MANAGEMENT_INDEX_TITLE = ASSET_MGMT_INDEX_TITLE
    const val ASSET_MGMT_INDEX_CARD_MANAGE = "Manage assets in FT4"
    const val ASSET_MGMT_INDEX_CARD_TRANSFER = "Transfer assets"
    const val ASSET_MGMT_ASSET_URL = "https://docs.chromia.com/build/ft4/asset-management/asset"
    const val ASSET_MGMT_ASSET_INDEX_URL = ASSET_MGMT_ASSET_URL
    const val ASSET_MGMT_ASSET_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/asset-management/asset/"
    const val ASSET_MGMT_ASSET_INDEX_TITLE = "Manage assets in FT4"
    const val ASSET_MGMT_TRANSFER_ASSETS_URL = "https://docs.chromia.com/build/ft4/asset-management/transfer-assets"
    const val ASSET_MGMT_TRANSFER_ASSETS_INDEX_URL = ASSET_MGMT_TRANSFER_ASSETS_URL
    const val ASSET_MGMT_TRANSFER_ASSETS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/asset-management/transfer-assets/"
    const val ASSET_MGMT_TRANSFER_ASSETS_INDEX_TITLE = "Transfer assets"
    const val CODE_EXAMPLES_URL = "https://docs.chromia.com/build/ft4/code-examples"
    const val CODE_EXAMPLES_URL_SLASH = "https://docs.chromia.com/build/ft4/code-examples/"
    const val CODE_EXAMPLES_TITLE = "Code examples"
    const val CODE_EXAMPLES_INDEX_URL = CODE_EXAMPLES_URL
    const val CODE_EXAMPLES_INDEX_URL_SLASH = CODE_EXAMPLES_URL_SLASH
    const val CODE_EXAMPLES_INDEX_TITLE = CODE_EXAMPLES_TITLE
    const val CODE_EXAMPLES_SECTION_CREATE_CONNECTION = "Create a connection"
    const val CODE_EXAMPLES_SECTION_PAGINATED = "Dealing with paginated entries"
    const val CODE_EXAMPLES_SECTION_AUTHENTICATING = "Authenticating an account"
    const val CODE_EXAMPLES_SECTION_AUTOMATIC_SIGNATURES = "Automatic signatures"
    const val CODE_EXAMPLES_SECTION_SIGNATURES = "Signatures"
    const val CODE_EXAMPLES_SECTION_ADMIN = "Using admin functions"
    const val CODE_EXAMPLES_SECTION_COMPLEX = "Complex transactions"
    const val IMPORTS_URL = "https://docs.chromia.com/build/ft4/setup/imports"
    const val FT4_IMPORTS_INDEX_URL = IMPORTS_URL
    const val FT4_IMPORTS_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/setup/imports/"
    const val FT4_IMPORTS_INDEX_TITLE = "Import FT4 into your project"
    const val SETUP_INDEX_URL = "https://docs.chromia.com/build/ft4/setup"
    const val SETUP_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/setup/"
    const val SETUP_INDEX_TITLE = "Project setup"
    const val SETUP_INDEX_CARD_SETUP = "Set up your FT4 project"
    const val SETUP_INDEX_CARD_IMPORTS = "Import FT4 into your project"
    const val CONFIG_VALUES_URL = "https://docs.chromia.com/build/ft4/configuration-values"
    const val CONFIG_VALUES_INDEX_URL = CONFIG_VALUES_URL
    const val CONFIG_VALUES_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/configuration-values/"
    const val CONFIG_VALUES_INDEX_TITLE = "FT4 configuration values"
    const val FT4_CONFIGURATION_VALUES_INDEX_URL = CONFIG_VALUES_INDEX_URL
    const val FT4_CONFIGURATION_VALUES_INDEX_URL_SLASH = CONFIG_VALUES_INDEX_URL_SLASH
    const val FT4_CONFIGURATION_VALUES_INDEX_TITLE = CONFIG_VALUES_INDEX_TITLE
    const val RELEASES_URL = "https://docs.chromia.com/build/ft4/releases/ft4"
    const val RELEASES_404_URL = "https://docs.chromia.com/build/ft4/releases"
    const val RELEASES_FT4_INDEX_URL = RELEASES_URL
    const val RELEASES_FT4_INDEX_URL_SLASH = "https://docs.chromia.com/build/ft4/releases/ft4/"
    const val RELEASES_FT4_INDEX_TITLE = "FT4 changelog"
    const val DOCS_LATEST_FT4 = "1.1.0r"
    const val DOCS_LATEST_FT4_DATE = "2025-02-25"
    const val FT4_RELL_API = "https://docs.chromia.com/pages/ft4-rell/"
    const val SOURCE = "https://gitlab.com/chromaway/ft4-lib"
    const val GET_STARTED_FT4_INDEX_URL = "https://docs.chromia.com/get-started/about/protocols/ft4"
    const val GET_STARTED_FT4_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/protocols/ft4/"
    const val GET_STARTED_FT4_INDEX_TITLE = "FT4"
    const val GET_STARTED_PROTOCOLS_INDEX_URL = "https://docs.chromia.com/get-started/about/protocols"
    const val GET_STARTED_PROTOCOLS_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/protocols/"
    const val GET_STARTED_PROTOCOLS_INDEX_TITLE = "Protocols"
    const val GET_STARTED_GTV_INDEX_URL = "https://docs.chromia.com/get-started/about/protocols/gtv"
    const val GET_STARTED_GTV_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/protocols/gtv/"
    const val GET_STARTED_GTV_INDEX_TITLE = "Generic Transfer Value (GTV)"
    const val GET_STARTED_PROTOCOLS_SUMMARY_INDEX_URL = "https://docs.chromia.com/get-started/about/protocols-summary"
    const val GET_STARTED_PROTOCOLS_SUMMARY_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/protocols-summary/"
    const val GET_STARTED_PROTOCOLS_SUMMARY_INDEX_TITLE = "Protocols"
    const val ECOSYSTEM_GOV_USER_TYPES_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/user-types"
    const val ECOSYSTEM_GOV_USER_TYPES_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/user-types/"
    const val ECOSYSTEM_GOV_USER_TYPES_INDEX_TITLE = "User types"
    const val ECOSYSTEM_GOV_VOTING_TYPES_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/governance-voting-process/voting-types"
    const val ECOSYSTEM_GOV_VOTING_TYPES_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/governance-voting-process/voting-types/"
    const val ECOSYSTEM_GOV_VOTING_TYPES_INDEX_TITLE = "Voting types"
    const val LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_URL = "https://learn.chromia.com/courses/book-review/build-client/complete-example"
    const val LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/build-client/complete-example/"
    const val LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_TITLE = "Complete the example"
    const val LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-review-entity/tables"
    const val LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-review-entity/tables/"
    const val LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_TITLE = "Defining the book review entity"
    const val LEARN_EVM_ASSET_TRANSFER_INDEX_URL = "https://learn.chromia.com/courses/chromia-for-evm-developers/asset-transfer"
    const val LEARN_EVM_ASSET_TRANSFER_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-for-evm-developers/asset-transfer/"
    const val LEARN_EVM_ASSET_TRANSFER_INDEX_TITLE = "Explore assets and transfers"
    const val LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_URL = "https://learn.chromia.com/courses/ft4-asset/consideration-recomendations"
    const val LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-asset/consideration-recomendations/"
    const val LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_TITLE = "Considerations and recommendations"
    const val LEARN_TAGS_FT4_INDEX_URL = "https://learn.chromia.com/tags/FT4"
    const val LEARN_TAGS_FT4_INDEX_URL_SLASH = "https://learn.chromia.com/tags/FT4/"
    const val LEARN_TAGS_FT4_INDEX_TITLE = "Courses tagged with: FT4"  // official H1

    val versionQueries = listOf(
        "ft4.get_version(): text  # official source returns \"1.1.0\"",
        "ft4.get_api_version(): integer  # official source returns 1"
    )

    val assetQueries = listOf(
        "ft4.get_all_assets(page_size: integer?, page_cursor: text?)",
        "ft4.get_assets_by_name(name, page_size: integer?, page_cursor: text?)",
        "ft4.get_assets_by_symbol(symbol: text, page_size: integer?, page_cursor: text?)",
        "ft4.get_asset_by_id(asset_id: byte_array)",
        "ft4.get_assets_by_type(type: text, page_size: integer?, page_cursor: text?)",
        "ft4.get_assets_filtered(asset_filter: assets.asset_filter?, page_size: integer?, page_cursor: text?)",
        "ft4.get_asset_balances(account_id: byte_array, page_size: integer?, page_cursor: text?)",
        "ft4.get_asset_balance(account_id: byte_array, asset_id: byte_array)",
        "ft4.get_balances_filtered(balance_filter: assets.balance_filter?, page_size: integer?, page_cursor: text?)",
        "ft4.get_transfer_history(account_id: byte_array, filter: assets.filter, page_size: integer?, page_cursor: text?)",
        "ft4.get_transfer_history_from_height(height: integer, asset_id: byte_array?, page_size: integer?, page_cursor: text?)",
        "ft4.get_transfer_history_entry(rowid)",
        "ft4.get_transfer_details(tx_rid: byte_array, op_index: integer): list<assets.transfer_detail>",
        "ft4.get_transfer_details_by_asset(tx_rid: byte_array, op_index: integer, asset_id: byte_array): list<assets.transfer_detail>",
        "ft4.get_transfer_history_entries_filtered(... page_size: integer?, page_cursor: text?)",
        "ft4.get_crosschain_transfer_history_entries_filtered(... page_size: integer?, page_cursor: text?)"
    )

    val accountQueries = listOf(
        "ft4.get_config()",
        "ft4.get_account_by_id(id: byte_array)",
        "ft4.get_accounts_by_signer(id: byte_array, page_size: integer?, page_cursor: text?)",
        "ft4.get_accounts_by_auth_descriptor_id(id: byte_array, page_size: integer?, page_cursor: text?)",
        "ft4.get_accounts_by_type(type: text, page_size: integer, page_cursor: text?)",
        "ft4.get_accounts_filtered(account_filter: account_filter, page_size: integer?, page_cursor: text?)",
        "ft4.get_account_auth_descriptors(id: byte_array)",
        "ft4.get_account_auth_descriptors_by_signer(account_id: byte_array, signer: byte_array)",
        "ft4.get_account_auth_descriptor_by_id(account_id: byte_array, id: byte_array)",
        "ft4.get_account_main_auth_descriptor(account_id: byte_array)",
        "ft4.get_account_auth_descriptors_filtered(account_auth_descriptor_filter?, page_size: integer?, page_cursor: text?)",
        "ft4.get_main_auth_descriptors_filtered(... page_size: integer?, page_cursor: text?)",
        "ft4.get_auth_descriptor_signers_filtered(auth_descriptor_signer_filter?, page_size: integer?, page_cursor: text?)",
        "ft4.get_rl_states_filtered(rl_state_filter?, page_size: integer?, page_cursor: text?)",
        "ft4.get_account_rate_limit_last_update(account_id: byte_array)",
        "ft4.is_auth_descriptor_valid(account_id: byte_array, auth_descriptor_id: byte_array)"
    )

    val memoQueries = listOf(
        "does_account_require_memo(account_id: byte_array): boolean"
    )

    val priorityQueries = listOf(
        "priority_check_v1(tx_body: gtx_transaction_body, tx_size: integer, tx_enter_timestamp: timestamp, current_timestamp: timestamp): priority_state_v1  # official /build/ft4/prioritization + source @mount('gtx_api')"
    )

    val priorityImports = listOf(
        "lib.ft4.core.prioritization.default  # official default rate-limit priority",
        "lib.ft4.core.prioritization  # official custom extend of priority_check"
    )

    val priorityStates = listOf(
        "priority_state_v1 { account_id: byte_array?; account_points: integer; tx_cost_points: integer; priority: decimal }",
        "no_op_priority_state()  # official lowest; priority 0.0; no account",
        "no_account_priority_state(priority: decimal)  # official high-priority bypass; example 1.0"
    )

    fun priorityExtendExample(): String = """
        import lib.ft4.core.prioritization.*;
        @extend(priority_check) function(tx_body: gtx_transaction_body, tx_size: integer, tx_enter_timestamp: timestamp, current_timestamp: timestamp): priority_state_v1 {
            // custom logic here
            return no_op_priority_state();
        }
    """.trimIndent() + "\n"

    fun prioritizationNote(): String = """
        Official prioritization ($PRIORITIZATION_URL, 200). Query/config read-only.
        Official default: import lib.ft4.core.prioritization.default — rate-limit rules apply in the tx queue.
        Official custom: import lib.ft4.core.prioritization and extend priority_check(...): priority_state_v1?.
        Official query priority_check_v1 calls priority_check; source module is @mount('gtx_api') — not ft4.*.
        Postchain calls this query. Do not invent a chr query invocation or a 64-hex id.
        Official priority is a zero or positive decimal; higher is first in the queue.
        Official default ratio: current_points / max_points (example 50/100 = 0.5), capped in [0, 1].
        Official no_op_priority_state() when no valid auth / account / descriptor.
        Official no_account_priority_state(1.0) when rate limiting is disabled or the account is exempt.
        If multiple accounts, official page assigns priority from the first valid account.
    """.trimIndent()

    val leftoverTerms = listOf(
        "Account  # /build/ft4/terms: on-chain entity; auth descriptors + assets; not tied to one key pair",
        "User account  # only type users control; auth descriptor by default",
        "Lock account  # associated with a user account; assets not directly user-accessible",
        "System account  # not linked to a user; dapp bookkeeping (e.g. cross-chain received/sent)",
        "Asset  # fungible on-chain entity; not ERC20-equivalent; no native max supply",
        "Auth descriptor  # signers + number of signers + flags + rules; bound to account(s)",
        "Auth flags  # official A = account edit; T = transfer/burn rights; dapp may define more",
        "Auth handler  # permissions needed to call an operation; not the same as an auth descriptor",
        "Balance  # on-chain entity separate from account and asset (not an EVM contract map)",
        "Expiration rules  # limit authorizations / block number / last-block timestamp",
        "User  # official glossary: a key pair. This tool does not generate or print keys.",
        "User accounts  # terms: only type users control; auth descriptor by default",
        "Lock accounts  # terms: any number per user; retrieve only via dapp ops (write skipped)",
        "System accounts  # terms: not linked to a user; e.g. cross-chain received/sent bookkeeping",
        "Asset burn  # terms: users can always burn; reduces supply. Write path skipped here",
        "Asset mint  # terms: admin-only; NEVER in production",
        "Asset supply  # terms: no native max supply; build your own if needed",
        "Auth descriptor parts  # terms: signers + number of signers + flags A/T + rules",
        "Expiration  # terms: authorizations count / block number / last-block timestamp"
    )

    val leftoverIntro = listOf(
        "Use FT4 when the dapp needs accounts, fungible assets, multi-sig/roles, transfers, cross-chain, or rate limits",
        "Simple read-only dapps may skip FT4 and use Rell directly",
        "Account ID = hash(public_key)  # native; official intro",
        "Account ID = hash(evm_address)  # EVM-compatible; official intro",
        "Do not invent a 64-hex Account ID  # intro prints formulas only",
        "When to use  # intro: accounts, fungible assets, multi-sig/roles, transfers, cross-chain, rate limits",
        "When to skip  # intro: simple read-only dapps may use Rell directly; FT4 is optional",
    )

    val skipped = listOf(
        "lib.ft4.admin / lib.ft4.core.admin / lib.ft4.external.admin (never emit)",
        "admin.crosschain / ras_open / ras_transfer_open",
        "ft4.admin.register_asset / ft4.admin.mint / ft4.transfer / burn / recall_unclaimed_transfer",
        "enable_transfer_memo / disable_transfer_memo / memo(text) operations",
        "auth write / account registration / crosschain apply or init operations",
        "get_asset_details_for_crosschain_registration (registration path; skipped here)",
        "terms/intro burn + mint write paths (admin mint never in production)",
        "intro transfer / multi-sig create/sign procedures",
        "/build/ft4/configuration-values sample admin pubkey (never emit)",
        "/build/ft4/releases (404; use /build/ft4/releases/ft4)",
        "later GitLab FT4 tags not on official changelog; do not scaffold as default"
    )

    fun paginationNote(): String = """
        Official pagination ($PAGINATION_URL): every FT4 query is paginated by default.
        page_cursor points at the next page. paged_result holds the current page plus a cursor.
        Official helpers: encode_cursor / decode_cursor (Base64), fetch_data_size, make_page, before_rowid.
        Official module_args.query_max_page_size default is 100 (lib.ft4).
        Official verify example ($SETUP_URL): chr query ft4.get_all_assets page_size=10 page_cursor=null
        Sample fields on that page: blockchain_rid, decimals, icon_url, id, name, supply, symbol, type: "ft4".
        First page_cursor is null. Do not invent a cursor or a 64-hex id.
    """.trimIndent()

    fun notes(): String = """
        Official FT4 $FT4_VERSION API $FT4_API read-only query catalog. Java 21+, Postgres 16+, CLI $CLI_SERIES.
        Official dapp-build hello_world has no FT4; FT4 auth register login transfer mint burn create-accounts WRITE SKIP. Query-only.
        Generated API: $FT4_RELL_API  Source tag: $SOURCE (v1.1.0r). Pagination: $PAGINATION_URL
        Official prioritization: $PRIORITIZATION_URL (200). Query/config read-only.
        Official terms: $TERMS_URL (200).
        Official BUILD ft4/intro ($INTRO_URL 307 $INTRO_URL_SLASH 200 $INTRO_TITLE): intro Chromia FT4 (Flexible Token 4) is an advanced token standard designed for the Chromia blockchain, offering enhanced asset and account management capabilities Similar to the ERC20 standard in Ethereum, FT4 facilitates token creation, transfer, and ownership tracking However, FT4 goes beyond basic token management by providing additional features like flexible account access controls, multi-signature support, and configurable rules for managing assets and accounts child cards $INTRO_CARD_ACCOUNT $INTRO_CARD_ACCOUNT_URL Terminology $INTRO_CARD_TERMS $TERMS_URL skip signed txs register account login transfer cross-chain writes no sample keys no invented 64-hex.
        Official BUILD ft4/setup ($SETUP_INDEX_URL 307 $SETUP_INDEX_URL_SLASH 200 $SETUP_INDEX_TITLE): intro Setting up your FT4 project involves installing the necessary packages, configuring your project files, and initializing key modules. This process ensures your environment is properly configured to interact with the Chromia blockchain. Once set up, you'll be ready to leverage FT4 features such as asset management, authentication, and cross-chain transfers, streamlining your blockchain integration and development child cards $SETUP_INDEX_CARD_SETUP $SETUP_URL Set up your FT4 project by installing required packages, configuring chromia.yml, and initializing key pairs and modules. Verify the setup by starting the node and querying the FT4 configuration $SETUP_INDEX_CARD_IMPORTS $IMPORTS_URL Import FT4 selectively into your project by using specific modules to control operations and reduce risks. Be cautious with importing the admin module and always review your imports to ensure only necessary functionalities are included skip signed txs key generation admin module import no sample keys no invented 64-hex.
        Official BUILD ft4/setup/ft4-setup ($FT4_SETUP_PAGE_INDEX_URL 307 $FT4_SETUP_PAGE_INDEX_URL_SLASH 200 $FT4_SETUP_PAGE_INDEX_TITLE): intro In this section, you'll learn how to set up a project to use FT4. If you want to see an example of a complete dapp using the FT4 protocol, you can explore the FT4 demo app Install Chromia CLI and set up PostgreSQL chr create-rell-dapp chromia.yml moduleArgs libs chr install chr query ft4.get_config FT4 client setup with TypeScript createConnection getAllAssets WRITE SKIP key generation admin module import Registering assets skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/setup/ft4-setup INDEX ($FT4_SETUP_INDEX_URL 307 $FT4_SETUP_INDEX_URL_SLASH 200 $FT4_SETUP_INDEX_TITLE): slash title WRITE SKIP.
        Official BUILD ft4/client ($CLIENT_INDEX_URL 307 $CLIENT_INDEX_URL_SLASH 200 $CLIENT_INDEX_TITLE): intro The FT4 Client provides the interface through which applications interact with the blockchain. It facilitates operations such as querying assets, signing transactions, transferring assets, and performing cross-chain transfers child cards $CLIENT_INDEX_CARD_SETUP $CLIENT_SETUP_URL To set up the FT4 client with TypeScript, install the necessary packages using npm install @chromia/ft4. Initialize the client by creating a connection to the Postchain network, and use it to interact with the blockchain (e.g., querying assets) $CLIENT_INDEX_CARD_AUTH $CLIENT_AUTH_DESCRIPTORS_URL FT4 supports Single-Signature (SingleSig) and Multi-Signature (MultiSig) auth descriptors, allowing customizable permissions and rules for secure access control in decentralized applications $CLIENT_INDEX_CARD_REGISTER $CLIENT_ACCOUNT_REGISTRATION_URL WRITE SKIP $CLIENT_INDEX_CARD_LOGIN $CLIENT_LOGIN_URL WRITE SKIP $CLIENT_INDEX_CARD_KEY_STORE $CLIENT_KEY_STORE_URL skip sample keys $CLIENT_INDEX_CARD_TRANSFER $CLIENT_TRANSFER_ASSETS_URL WRITE SKIP $CLIENT_INDEX_CARD_ORCHESTRATOR $CLIENT_ORCHESTRATOR_URL WRITE SKIP skip signed txs register login transfer cross-chain writes no sample keys no invented 64-hex.
        Official BUILD ft4/client/client-setup ($CLIENT_SETUP_INDEX_URL 307 $CLIENT_SETUP_INDEX_URL_SLASH 200 $CLIENT_SETUP_INDEX_TITLE): intro This section discusses how to install and initialize the FT4 client Install the client npm install @chromia/ft4 Initialize the client createClient postchain-client createConnection @chromia/ft4 http://localhost:7740 blockchainIid 0 Test the connection getAllAssets node index.js nextCursor WRITE SKIP sending transactions authenticate sign register login transfer skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/client/client-auth-descriptors ($CLIENT_AUTH_DESCRIPTORS_INDEX_URL 307 $CLIENT_AUTH_DESCRIPTORS_INDEX_URL_SLASH 200 $CLIENT_AUTH_DESCRIPTORS_INDEX_TITLE): intro The FT4 client library provides flexible tools for creating Single-Signature (SingleSig) and Multi-Signature (MultiSig) auth descriptors, along with optional rules to customize the permissions Auth descriptor structure AuthDescriptor AuthType SingleSig MultiSig flags A T Using rules in auth descriptors Simple rule Complex rule Key points to remember WRITE SKIP create authenticate sign register login transfer skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/client/client-account-registration ($CLIENT_ACCOUNT_REGISTRATION_INDEX_URL 307 $CLIENT_ACCOUNT_REGISTRATION_INDEX_URL_SLASH 200 $CLIENT_ACCOUNT_REGISTRATION_INDEX_TITLE): intro This section demonstrates how to register a new account on the blockchain using either Chromia-native keys or MetaMask (EVM-compatible keys). By now, you should have a client connection established and be familiar with auth descriptors WRITE SKIP register create sign authenticate sample keys sample admin pubkey keygen invented 64-hex signed txs registration strategies writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/client/client-login ($CLIENT_LOGIN_INDEX_URL 307 $CLIENT_LOGIN_INDEX_URL_SLASH 200 $CLIENT_LOGIN_INDEX_TITLE): intro To improve the user experience in web applications, FT4 provides a mechanism for generating disposable keys and adding them to a user's account. This allows non-interactive signing of operations using the directly accessible new key, eliminating the need for the user to sign each operation with MetaMask However, it's crucial to exercise caution when adding auth flags to disposable keys, as compromised keys with sensitive flags could lead to asset compromisation or other security risks FT4 offers a login function that simplifies the process of generating and managing disposable keys WRITE SKIP login authenticate sign sample keys sample admin pubkey keygen invented 64-hex signed txs session writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/client/client-key-store ($CLIENT_KEY_STORE_INDEX_URL 307 $CLIENT_KEY_STORE_INDEX_URL_SLASH 200 $CLIENT_KEY_STORE_INDEX_TITLE): intro In the FT4 client library, the KeyStore interface represents a general abstraction for managing cryptographic keys This abstraction is crucial for implementing security and transaction signing mechanisms Two key implementations extend this interface EvmKeyStore Designed for Ethereum-compatible keys FtKeyStore Designed for FT4-specific keys Both implementations provide methods to sign messages or transactions and to integrate with authentication handlers WRITE SKIP sample keys sample admin pubkey keygen invented 64-hex signed txs persist store writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/client/client-transfer-assets ($CLIENT_TRANSFER_ASSETS_INDEX_URL 307 $CLIENT_TRANSFER_ASSETS_INDEX_URL_SLASH 200 $CLIENT_TRANSFER_ASSETS_INDEX_TITLE): intro Transferring assets is a fundamental operation in decentralized applications built on the FT4 framework FT4 provides a secure and efficient way to transfer assets between accounts, ensuring proper authentication and authorization This section will guide you through the process of transferring assets using both the Chromia CLI and the Postchain client library WRITE SKIP transfer sign sample keys sample admin pubkey keygen invented 64-hex signed txs on-chain cross-chain writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/client/client-orchestrator ($CLIENT_ORCHESTRATOR_INDEX_URL 307 $CLIENT_ORCHESTRATOR_INDEX_URL_SLASH 200 $CLIENT_ORCHESTRATOR_INDEX_TITLE): intro The orchestrator is a utility object that facilitates cross-chain transfers It can be instantiated, listen to events, and execute asset transfers between blockchains WRITE SKIP init_transfer apply_transfer complete_transfer sign sample keys sample admin pubkey keygen invented 64-hex signed txs cross-chain writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend ($BACKEND_INDEX_URL 307 $BACKEND_INDEX_URL_SLASH 200 $BACKEND_INDEX_TITLE): intro FT4 provides a comprehensive set of tools for managing decentralized applications, with features for flexible account management, secure authentication, asset handling, and cross-chain operations. It enables efficient data retrieval through pagination, ensuring high performance and scalability in decentralized systems. These capabilities allow developers to integrate and manage accounts, assets, and transactions across multiple blockchains in a secure and efficient manner child cards $BACKEND_INDEX_CARD_ACCOUNTS $BACKEND_ACCOUNTS_URL FT4 allows flexible account registration strategies like open, fee, or subscription. Accounts can be linked for various purposes, including staking, asset locking, and auctions. Additionally, multiple auth descriptors can be associated with accounts, with one main descriptor managing access WRITE SKIP $BACKEND_INDEX_CARD_AUTHENTICATION $BACKEND_AUTHENTICATION_URL FT4 provides a robust authentication system using auth descriptors, which define key pairs, permissions, and multi-signature rules. Custom authentication handlers and resolvers offer flexibility in user authorization, and multi-signature transactions require multiple approvals, enhancing security for sensitive operations $BACKEND_INDEX_CARD_ASSETS $BACKEND_ASSETS_URL FT4 facilitates asset registration, ensuring each asset has a unique identity and can be securely tracked. It manages asset balances with attributes like precision, supply, and issuing blockchain. Additionally, assets can be securely locked in temporary accounts for scenarios such as staking or auctions, without transferring ownership $BACKEND_INDEX_CARD_CROSS_CHAIN $BACKEND_CROSS_CHAIN_URL FT4 enables seamless asset transfers across blockchains within the Chromia network WRITE SKIP skip signed txs account registration writes init_transfer apply_transfer complete_transfer no sample keys no invented 64-hex.
        Official BUILD ft4/backend/accounts ($BACKEND_ACCOUNTS_INDEX_URL 307 $BACKEND_ACCOUNTS_INDEX_URL_SLASH 200 $BACKEND_ACCOUNTS_INDEX_TITLE): intro FT4 accounts can be registered with transfer open, transfer fee, or transfer subscription strategies, each offering flexible access options auth descriptors control account access, while account_link manages relationships between accounts for scenarios like staking and asset locking Accounts can have multiple auth descriptors, with one main descriptor controlling access The account_link entity manages relationships between accounts WRITE SKIP account registration writes sample keys sample admin pubkey keygen invented 64-hex signed txs registration strategies writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/accounts/account-linking ($BACKEND_ACCOUNT_LINKING_INDEX_URL 307 $BACKEND_ACCOUNT_LINKING_INDEX_URL_SLASH 200 $BACKEND_ACCOUNT_LINKING_INDEX_TITLE): intro The account_link entity is a versatile structure designed to represent a relationship between two accounts. This entity can be customized for various purposes by developers, enabling the management of complex account interactions such as staking, locking, or auction-related transactions The account_link can be leveraged in scenarios where assets are held temporarily in a non-user-controlled account, or in cases where accounts must be linked for internal management or governance reasons Structure Entity definition account_link accounts.account secondary accounts.account type text stake bid Indices account secondary type Usage scenarios Lock accounts ACCOUNT_TYPE_LOCK Bidding and auction System-controlled accounts Custom non-user account types Implementation notes Customizability Security and access control On-chain transparency Related modules core.assets.locking core.accounts.account.type WRITE SKIP link register write sample keys sample admin pubkey keygen invented 64-hex signed txs link writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/accounts/overview ($BACKEND_ACCOUNTS_OVERVIEW_INDEX_URL 307 $BACKEND_ACCOUNTS_OVERVIEW_INDEX_URL_SLASH 200 $BACKEND_ACCOUNTS_OVERVIEW_INDEX_TITLE): intro In the FT4 library, accounts function as digital identities, enabling users to transfer assets, interact with dapps, and sign transactions Account registration allows the system to securely identify and authenticate users, supporting access control mechanisms that let users or administrators define permissions for actions an account can perform This approach is crucial for maintaining security, preventing unauthorized access, and ensuring that resources such as assets or data remain accessible only to the rightful owner Account registration framework Registration strategies Open Transfer open Transfer fee Transfer subscription lib.ft4.core.accounts.strategies.open lib.ft4.core.accounts.strategies.transfer.open lib.ft4.core.accounts.strategies.transfer.fee lib.ft4.core.accounts.strategies.transfer.subscription Strategy comparison Transfer strategy moduleArgs chromia.yml sender_blockchain timeout_days Current Chain Any Value Same Value Rate limiting rate_limit lib.ft4.core.accounts max_points recovery_time points_at_account_creation get_rate_limit_config_for_account Dapp-controlled accounts WRITE SKIP link register write sample keys sample admin pubkey keygen invented 64-hex signed txs link writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/accounts/open ($BACKEND_ACCOUNTS_OPEN_INDEX_URL 307 $BACKEND_ACCOUNTS_OPEN_INDEX_URL_SLASH 200 $BACKEND_ACCOUNTS_OPEN_INDEX_TITLE): intro The transfer open strategy requires users to transfer a specific amount of tokens to a non-existent account, which they must then claim to activate it. The non-existent account represents an empty account that needs to be funded with tokens. Once activated, users can utilize the tokens sent to the account This strategy provides a middle ground between completely free account creation and paid strategies, requiring token transfer but no fees Important considerations for production use The transfer open strategy can be used in production, but it comes with spam risks since there are no transfer fees neither local nor cross-chain Without proper safeguards, users could potentially create thousands of accounts by transferring tokens back and forth To use safely in production Consider limiting account creation to same-address transfers sender ID recipient ID Implement additional rate limiting or validation mechanisms Monitor for potential spam patterns chromia.yml moduleArgs lib.ft4.core.accounts.strategies.transfer rules sender_blockchain sender recipient asset name min_amount timeout_days strategy open Configuration example Cross-chain configuration example Code examples GitLab repository Complete cross-chain transfer example Bitbucket Rell examples Rell-side implementation Bitbucket JavaScript examples Client-side implementation WRITE SKIP register claim write sample keys sample admin pubkey keygen invented 64-hex signed txs register writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/accounts/fixed ($BACKEND_ACCOUNTS_FIXED_INDEX_URL 307 $BACKEND_ACCOUNTS_FIXED_INDEX_URL_SLASH 200 $BACKEND_ACCOUNTS_FIXED_INDEX_TITLE): intro The transfer fee strategy simplifies the process for users by requiring a one-time purchase of a specific amount of tokens to access your dapp's features. This system is ideal for users who prefer a clear and easy-to-follow process When implementing the transfer fee strategy for account registration, users need to transfer a specific amount of tokens to a non-existent account. Once the non-existent account receives the tokens, the fee is deducted from the transferred amount. The non-existent account represents an empty account that needs to be topped up with tokens Key details Users pay a fixed amount similar to a one-time purchase using a specific token to access the dapp's features This approach provides a clear and easy-to-understand system, suitable for users who prefer straightforward access Simplifies the dapp's financial management and encourages brand loyalty among satisfied users Benefits of transfer fees Clarity Users know exactly what they're paying for, fostering trust and transparency Convenience Eliminates the need for recurring payments, making it user-friendly Brand Loyalty Encourages users to explore all features, potentially increasing engagement Getting started A code example with tests is available chromia.yml moduleArgs lib.ft4.core.accounts.strategies.transfer.fee asset name amount fee_account lib.ft4.core.accounts.strategies.transfer rules sender_blockchain sender recipient min_amount timeout_days strategy fee Transfer strategy For any user to any recipient from any blockchain a fixed fee can be requested Transfer fee strategy The fee is set A clear and straightforward fee structure is essential Transparency ensures users know exactly what they're paying for To test the configuration above, refer to the available tests WRITE SKIP register write sample keys sample admin pubkey keygen invented 64-hex signed txs register writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/accounts/subscription ($BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_URL 307 $BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_URL_SLASH 200 $BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_TITLE): intro When implementing a transfer subscription strategy for your dapp, users will need to pay regularly to access its functionality This approach helps generate a steady income to sustain and grow your dapp To set up the transfer subscription strategy configure the chromia.yml file to enable subscriptions account registration two approaches with EVM signatures without EVM signatures Users must transfer a specific amount of tokens from an existing account to their non-existent account A part of these tokens will be deducted as a fee The non-existent account represents an empty account that needs to be filled with tokens subscription_period_days chromia.yml When the subscription period ends the account becomes inactive To reactivate users must renew the subscription Benefits of transfer subscriptions Recurring revenue Provides a predictable income stream to support dapp sustainability Easy setup Simple implementation offers clear advantages to users Getting started There are three ways to specify the asset Option 1 specifying the foreign asset issuing_blockchain_rid name Option 2 specifying the foreign asset by id id min_amount Option 3 specifying the local asset name A code example with tests is available chromia.yml moduleArgs lib.ft4.core.accounts.strategies.transfer.subscription asset name amount subscription_period_days free_operations subscription_account lib.ft4.core.accounts.strategies.transfer rules sender_blockchain sender recipient min_amount timeout_days strategy subscription Transfer strategy For any sender and recipient on any blockchain a subscription can be requested Transfer subscription strategy The subscription price is set Users may perform free_operations even without an active subscription To test the configuration above refer to the provided tests WRITE SKIP register write sample keys sample admin pubkey keygen invented 64-hex signed txs register writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/accounts/accounts-and-auth-descriptors ($BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_URL 307 $BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_URL_SLASH 200 $BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_TITLE): intro Accounts can have multiple account descriptors connected to them Each can be made to serve a different purpose but the most important one is the main auth descriptor The auth descriptor itself is a simple struct struct auth_descriptor auth_type args list gtv rules gtv The struct is then connected to the account using the account_auth_descriptor entity id byte_array account auth_type args byte_array single_sig_args auth_type S multi_sig_args auth_type M rules byte_array no rules null GTV_NULL_BYTES simple rule rule_expression complex rule and auth_descriptor_config max_rules expiration main_auth_descriptor GTV_NULL_BYTES ctr integer op_count created timestamp Main auth descriptor Each account has a main account auth descriptor which is set during account creation The main account descriptor can only be substituted it can't be deleted entity main_auth_descriptor account auth_descriptor account_auth_descriptor The main auth descriptor cannot be bound by any rules always valid until replaced mandatory flags Adding other auth descriptors auth_descriptor_signer pubkey EVM address without 0x Creating an account with an auth descriptor hash pubkey hash evm_address rl_state WRITE SKIP update_main_auth_descriptor add_auth_descriptor create_account_with_auth register write sample keys sample admin pubkey keygen invented 64-hex signed txs register writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/authentication ($BACKEND_AUTHENTICATION_INDEX_URL 307 $BACKEND_AUTHENTICATION_INDEX_URL_SLASH 200 $BACKEND_AUTHENTICATION_INDEX_TITLE): intro FT4 authentication utilizes auth descriptors to define key pairs, permissions, and multi-signature security Custom auth handlers enable flexible user authorization across FT and EVM signers Multi-signature transactions require multiple approvals WRITE SKIP authenticate sign register sample keys sample admin pubkey keygen invented 64-hex signed txs multi-signature writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/authentication/auth ($BACKEND_AUTHENTICATION_AUTH_INDEX_URL 307 $BACKEND_AUTHENTICATION_AUTH_INDEX_URL_SLASH 200 $BACKEND_AUTHENTICATION_AUTH_INDEX_TITLE): intro Authentication in decentralized applications (dapps) verifies if a user has permission to perform specific actions While the process is straightforward with regular Rell applications, it becomes more complex with FT4 due to support for native and EVM signatures and multiple keys for an FT4 account However, the complexity is abstracted, and authentication can be easily implemented using the authenticate operation The authenticate function auth authenticate Cannot find auth handler for operation auth_handler add_auth_handler scope flags rell meta mount_name extendable functions Overridable auth handlers add_overridable_auth_handler Custom auth messages EVM generic message custom message formatter gtv account_id auth_descriptor_id Account ID signer types FT Postchain signers EVM signers Signer type detection Account ID calculation hash pubkey hash evm_address without 0x Custom resolver delete_auth_descriptor Application scope auth handler Mount name scope auth handler SSO Metamask Example WRITE SKIP authenticate sign register sample keys sample admin pubkey keygen invented 64-hex signed txs authenticate writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/authentication/auth-descriptors-and-rules ($BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_URL 307 $BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_URL_SLASH 200 $BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_TITLE): intro Auth descriptors are a mechanism that defines the authorized key pairs and their associated permissions for interacting with accounts on the blockchain They enable features like multi-signature security and granular access control, allowing for secure and flexible management of account access Entities involved Account User Key pair glossary Key concepts A single user can have multiple key pairs A single key pair should never be shared among multiple users A single account can be accessed by multiple key pairs and users A single key pair can be used to access multiple accounts Never share your private key Multi-signature security Granular access control Auth descriptor components arguments rules Public keys Permissions flags Required signatures Single-signature single-sig Multi-signature multi-sig Operators Variables operation_count block_time block_height relative_block_height relative_block_time simple complex Active Inactive Expired automatically cleaned up WRITE SKIP authenticate register write sample keys sample admin pubkey keygen invented 64-hex signed txs authenticate writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/authentication/multi-sig ($BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_URL 307 $BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_URL_SLASH 200 $BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_TITLE): intro Multi-signature (multi-sig) transactions provide an added layer of security and shared control over blockchain transactions In a multi-signature setup, multiple parties must approve a transaction before it can be executed, reducing the risk of unauthorized actions Multi-sig arrangements are commonly used for scenarios requiring heightened security, such as managing corporate funds, safeguarding shared assets, or establishing trust in decentralized systems In a multi-signature transaction, a predefined number of designated signers must authorize the transaction for it to be valid This setup allows for flexible access control, as users can specify the minimum number of required signatures Creating a new multi-signature transaction Minimal command Signers file format Creating a transaction with FT auth Transaction output Adding signatures to a multi-wignature transaction Signing transaction output Sending a fully signed transaction Viewing a transaction WRITE SKIP create sign send authenticate register write create-sign sample keys sample admin pubkey keygen invented 64-hex signed txs create sign writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/assets ($BACKEND_ASSETS_INDEX_URL 307 $BACKEND_ASSETS_INDEX_URL_SLASH 200 $BACKEND_ASSETS_INDEX_TITLE): intro FT4 allows secure asset registration, ensuring unique identity and metadata via admin or custom operations Asset balances are managed with precise attributes like supply and precision, supporting secure operations and cross-chain compatibility FT4 lock accounts enable temporary asset storage for use cases such as staking or auctions, restricting access without transferring ownership WRITE SKIP register admin sample keys sample admin pubkey keygen invented 64-hex signed txs lock writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/assets/register-assets ($BACKEND_ASSETS_REGISTER_ASSETS_INDEX_URL 307 $BACKEND_ASSETS_REGISTER_ASSETS_INDEX_URL_SLASH 200 $BACKEND_ASSETS_REGISTER_ASSETS_INDEX_TITLE): intro Asset registration is a crucial process within the FT4 library It involves defining and initializing new assets (such as tokens) that will be used within the system, ensuring that each asset has a unique identity, associated metadata, and is properly tracked across different accounts There are two primary ways to register assets in FT4: using the built-in admin operation or by writing a custom operation Both methods achieve the same goal but allow for different levels of customization and control over the registration process Asset registration ensures that each token is recognized and managed according to a set of predefined rules It serves several purposes Asset initialization Registration initializes an asset on the blockchain by defining its properties (name, symbol, decimals, etc.) Without this, the blockchain has no way of identifying or interacting with the asset Standardization Registration ensures that all assets follow a standardized format, making it easier for wallets, dapps, and other services to recognize and interact with them The asset's properties (such as its decimal precision and symbol) are necessary for accurate tracking and transactions Security and control Registering an asset provides security by ensuring that the asset's identity is unique and tied to the correct issuing entity This prevents conflicts, duplicate assets, or fraud within the system Supply tracking Registration initializes the asset's supply (even if the initial supply is zero) and allows the system to track future issuance or transfers of the asset Integration with accounts Once registered, the asset can be held in accounts, transferred between users, or used in decentralized applications (dapps) Without registration, the system cannot recognize the asset or manage account balances Register assets with FT4 admin operation The Admin module should not be used in production since it is a security liability Steps for asset registration Register an asset with a custom operation Custom asset registration code WRITE SKIP register admin register_asset mint write sample keys sample admin pubkey keygen invented 64-hex signed txs register writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/assets/asset-amounts ($BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_URL 307 $BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_URL_SLASH 200 $BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_TITLE): intro The FT4 library allows managing various assets, each potentially having its own precision and formatting requirements, which are governed by specific parameters and functions as outlined below Asset structure ID byte_array 32 bytes Name and Symbol text 1024 Decimals Type ASSET_TYPE_FT4 Issuing blockchain Total supply big_integer Number formatting of asset amounts Decimals and amount representation format_amount_with_decimals amount big_integer decimals integer Example usage Validation validate_asset_decimals 0 78 Amount validation max_asset_amount require_zero_exclusive_asset_amount_limits Balance management and amount calculation Error handling in balance management INSUFFICIENT BALANCE INVALID AMOUNT Cross-chain asset compatibility issuing_blockchain_rid WRITE SKIP increase_balance deduct_balance mint burn register write sample keys sample admin pubkey keygen invented 64-hex signed txs mint writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/assets/locking-assets ($BACKEND_ASSETS_LOCKING_ASSETS_INDEX_URL 307 $BACKEND_ASSETS_LOCKING_ASSETS_INDEX_URL_SLASH 200 $BACKEND_ASSETS_LOCKING_ASSETS_INDEX_TITLE): intro In the FT4 library, a "lock account" functions as a secure storage for assets that users should not be able to access temporarily This is useful in situations like staking or auction bids, where assets are still owned by users but are restricted from active use Lock account overview ACCOUNT_TYPE_LOCK FT4_LOCK Purpose Lock accounts restrict access to assets without transferring ownership Usage scenarios Staking auction participation account_link account secondary type Creating a lock account ensure_lock_account Retrieving lock accounts get_lock_accounts get_lock_accounts_with_non_zero_balances Viewing locked balances get_locked_asset_balance get_locked_asset_aggregated_balance get_locked_asset_balances get_locked_asset_aggregated_balances WRITE SKIP lock unlock lock_asset unlock_asset ensure_lock_account write sample keys sample admin pubkey keygen invented 64-hex signed txs lock writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/cross-chain ($BACKEND_CROSS_CHAIN_INDEX_URL 307 $BACKEND_CROSS_CHAIN_INDEX_URL_SLASH 200 $BACKEND_CROSS_CHAIN_INDEX_TITLE): intro FT4 enables smooth asset transfers within the Chromia network, supporting stages like init_transfer, apply_transfer, and complete_transfer Cross-chain assets can be registered and tracked, ensuring they meet validation requirements like unique IDs and issuing chains Transfers rely on secure asset registration, origin chain validation, and ICCF anchoring for efficient cross-chain routing WRITE SKIP init_transfer apply_transfer complete_transfer sample keys sample admin pubkey keygen invented 64-hex signed txs cross-chain writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/cross-chain/introduction ($BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_URL 307 $BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_URL_SLASH 200 $BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_TITLE): intro Cross-chain transfers enable seamless asset movements between multiple blockchains Within the Chromia network, cross-chain transfers are an integral part of the FT4 protocol Why cross-chain transfers Cross-chain transfers empower applications and users Expand Reach Operate within the Chromia network across multiple chains Enhance Liquidity Facilitate asset transfers within the Chromia network Maintain Security Asset structures enable easier tracking of asset flows Key components FT4 assets Chromia's asset standard can be either internally minted or externally originated Origin chains The parent chain from which an asset can be received Operations WRITE SKIP init_transfer apply_transfer complete_transfer transfer sample keys sample admin pubkey keygen invented 64-hex signed txs transfer writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/cross-chain/cross-chain-assets ($BACKEND_CROSS_CHAIN_ASSETS_INDEX_URL 307 $BACKEND_CROSS_CHAIN_ASSETS_INDEX_URL_SLASH 200 $BACKEND_CROSS_CHAIN_ASSETS_INDEX_TITLE): intro In the FT4 library, cross-chain assets are assets that originate on one blockchain but can be registered, tracked, and transferred across other chains in the ecosystem Cross-chain asset registration register_crosschain_asset Unsafe Registration requirements and parameters asset_id name symbol decimals 0 78 issuing_blockchain_rid origin_blockchain_rid icon_url type core.asset.ASSET_TYPE_FT4 uniqueness_resolver Validation constraints origin_blockchain_rid must not be the current chain issuing_blockchain_rid cannot be the current chain Asset ID 32 bytes Avoiding name conflicts Uniqueness of name symbol uniqueness_resolver Structure of cross-chain asset management asset_origin entity assets.asset origin_blockchain_rid Blockchain accounts ACCOUNT_TYPE_BLOCKCHAIN ensure_blockchain_account WRITE SKIP register_crosschain_asset ensure_blockchain_account init_transfer apply_transfer complete_transfer transfer write sample keys sample admin pubkey keygen invented 64-hex signed txs transfer writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/cross-chain/automate-cross-chain-asset-registration ($BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_URL 307 $BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_URL_SLASH 200 $BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_TITLE): intro This topic provides an example of automating cross-chain FT4 asset registration It is not intended as a best practices guide Carefully assess the risks associated with admin key exposure If in doubt, do not implement this functionality Automating cross-chain FT4 asset registration can streamline workflows in controlled environments Proper validation of assets and careful handling of admin credentials are essential Set up a client chromiaClient Set up the admin signature provider Prevent admin key exposure Admin keys must be kept secure encryption hardware security modules HSMs manual admin input Register a cross-chain asset Manual validation exceptions origin issuing chains match prevent broken transfer paths WRITE SKIP register_crosschain_asset registerCrosschainAsset automate init_transfer apply_transfer complete_transfer write sample keys sample admin pubkey keygen invented 64-hex signed txs register automate writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/backend/cross-chain/automate-cross-chain-asset-registration INDEX ($AUTOMATE_CROSSCHAIN_ASSET_INDEX_URL 307 $AUTOMATE_CROSSCHAIN_ASSET_INDEX_URL_SLASH 200 $AUTOMATE_CROSSCHAIN_ASSET_INDEX_TITLE): slash title WRITE SKIP.
        Official BUILD ft4/account-management ($ACCOUNT_MANAGEMENT_INDEX_URL 307 $ACCOUNT_MANAGEMENT_INDEX_URL_SLASH 200 $ACCOUNT_MANAGEMENT_INDEX_TITLE): intro FT4 provides flexible account management for dapps, supporting multi-user accounts and customizable registration strategies. It enables secure access control through auth descriptors, which define key pairs and permissions, including single and multi-signature setups. Authentication is simplified with the authenticate operation and customizable auth_handler functions, supporting native and EVM signatures, multiple key pairs, disposable keys, and overrideable logic child cards $ACCOUNT_MGMT_INDEX_CARD_MANAGE $ACCOUNT_MGMT_OVERVIEW_URL FT4 allows you to manage accounts with unique identifiers, enabling secure interactions with decentralized applications. It supports multi-user accounts and customizable registration strategies to suit different access levels and security needs WRITE SKIP registration $ACCOUNT_MGMT_INDEX_CARD_AUTH $ACCOUNT_MGMT_AUTH_DESCRIPTORS_URL FT4 allows flexible account management through auth descriptors, which define the key pairs and permissions for accessing accounts. These descriptors support single and multi-signature setups, offering secure, customizable access control for different use cases $ACCOUNT_MGMT_INDEX_CARD_MULTISIG $ACCOUNT_MGMT_MULTISIG_URL Learn how FT4 supports secure account management with multisig access. Multisig setups allow multiple participants to cooperatively manage an account by requiring several signatures for critical actions. This ensures shared control and robust security, minimizing risks like key loss or unauthorized access WRITE SKIP create sign skip signed txs authenticate write registration multisig create-sign no sample keys no invented 64-hex.
        Official BUILD ft4/account-management/overview ($ACCOUNT_MGMT_OVERVIEW_INDEX_URL 307 $ACCOUNT_MGMT_OVERVIEW_INDEX_URL_SLASH 200 $ACCOUNT_MGMT_OVERVIEW_INDEX_TITLE): intro FT4 provides powerful tools for managing accounts in a decentralized environment Each account is represented by an ID, which serves as a unique identifier within the system The architecture supports the management of accounts by multiple users, each of whom can have different levels of access and control Account structure entity account key id byte_array index type text Multi-user account management account_auth_descriptor Rate limiter rate_limit lib.ft4.core.accounts max_points recovery_time points_at_account_creation WRITE SKIP account registration register_account admin sample keys sample admin pubkey keygen invented 64-hex signed txs registration writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/account-management/auth-descriptors ($ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_URL 307 $ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_URL_SLASH 200 $ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_TITLE): intro One of the core features of FT4 is the ability to use multiple key pairs or accounts to control a single account Auth descriptors are a mechanism that defines the authorized key pairs and their associated permissions for interacting with accounts on the blockchain They enable features like multi-signature security and granular access control, allowing for secure and flexible management of account access Types of auth descriptors Single-signature Multi-signature Flexible account management auth_descriptor lib.ft4.core.accounts max_number_per_account Authorization flags T A Expiration rules Block height Block time Number of authorizations max_auth_descriptor_rules WRITE SKIP authenticate sign add sample keys sample admin pubkey keygen invented 64-hex signed txs authenticate writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/account-management/multisig ($ACCOUNT_MGMT_MULTISIG_INDEX_URL 307 $ACCOUNT_MGMT_MULTISIG_INDEX_URL_SLASH 200 $ACCOUNT_MGMT_MULTISIG_INDEX_TITLE): intro When handling on-chain operations as an organization, it is in general a good idea to use multisignature access, or multisig for short The funds you receive are sent to an FT4 account, and using multisig access means that multiple people must cooperate to move those assets This is different from the default behavior, where a single key is used to access funds: in this case, there's no single person using the key that could maliciously access funds signers signatures required Example scenario Setting up a multisig account In FT4, all accounts are equal There is no such thing as a multisig account the accounts are accessed through auth descriptors an auth descriptor can be setup to be multisign or singlesig WRITE SKIP register create sign send transfer sample keys sample admin pubkey keygen invented 64-hex signed txs register writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/asset-management ($ASSET_MANAGEMENT_INDEX_URL 307 $ASSET_MANAGEMENT_INDEX_URL_SLASH 200 $ASSET_MANAGEMENT_INDEX_TITLE): intro FT4 provides tools to easily manage your assets, such as tokens, on the Chromia blockchain. You can register new assets, track their details, and transfer them across different blockchains child cards $ASSET_MGMT_INDEX_CARD_MANAGE $ASSET_MGMT_ASSET_URL FT4 allows you to register and track assets with important details like name, symbol, and supply. You can register assets using either the admin operation or custom methods, making it easy to manage and transfer them within the system WRITE SKIP register admin $ASSET_MGMT_INDEX_CARD_TRANSFER $ASSET_MGMT_TRANSFER_ASSETS_URL FT4 allows asset transfers within the same blockchain (on-chain) and across different blockchains in the Chromia network (cross-chain). It also supports transfers to EVM-compatible blockchains like Ethereum through a bridge, expanding asset usability across multiple platforms WRITE SKIP transfer skip signed txs admin register transfer writes no sample keys no invented 64-hex.
        Official BUILD ft4/asset-management/asset ($ASSET_MGMT_ASSET_INDEX_URL 307 $ASSET_MGMT_ASSET_INDEX_URL_SLASH 200 $ASSET_MGMT_ASSET_INDEX_TITLE): intro FT4 offers extensive asset management capabilities, supporting multiple types of assets within the same ecosystem Each asset is tracked within the asset table, which maintains all necessary metadata about the asset, such as its name, symbol, and total supply Asset registration is a crucial process within the FT4 framework on the Chromia blockchain Admin operations are enabled by importing the admin module and should not be used in production Asset definition entity asset key id byte_array name symbol decimals issuing_blockchain_rid icon_url type ASSET_TYPE_FT4 total_supply Balances entity balance amount WRITE SKIP register admin sample keys sample admin pubkey keygen invented 64-hex signed txs register writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/asset-management/transfer-assets ($ASSET_MGMT_TRANSFER_ASSETS_INDEX_URL 307 $ASSET_MGMT_TRANSFER_ASSETS_INDEX_URL_SLASH 200 $ASSET_MGMT_TRANSFER_ASSETS_INDEX_TITLE): intro FT4 assets on Chromia can be transferred both on-chain (within a single blockchain) and cross-chain (across different blockchains within the Chromia network) On-chain transfers On-chain transfers refer to transactions that occur within the same blockchain These are the standard transfers where assets are moved from one account to another on the same chain, securely and efficiently Cross-chain transfers Chromia's multi-chain architecture allows assets to be transferred between blockchains within the Chromia network This is known as a cross-chain transfer Even though these transactions span different blockchains, they remain within the Chromia ecosystem Transfer to EVM environments In addition to on-chain and cross-chain transfers within Chromia, the platform also supports cross-environment transfers to EVM-compatible blockchains such as Ethereum, Binance Smart Chain, and others This is facilitated through a bridge that connects Chromia's native assets to the EVM ecosystem EVM bridge The Chromia-EVM bridge allows FT4 assets to be transferred to and from Ethereum and other EVM-based blockchains This expands the utility of FT4 assets, enabling their use in external decentralized finance (DeFi) platforms, NFT marketplaces, and other applications within the EVM world WRITE SKIP transfer sign on-chain cross-chain sample keys sample admin pubkey keygen invented 64-hex signed txs transfer writes skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/code-examples ($CODE_EXAMPLES_URL 307 $CODE_EXAMPLES_URL_SLASH 200 $CODE_EXAMPLES_TITLE): intro This section showcases practical examples demonstrating how to use the client library. These scripts provide valuable insights into the library's functionality, even though the complete documentation is still under development $CODE_EXAMPLES_SECTION_CREATE_CONNECTION createClient createConnection getAccountById getBalances This code demonstrates how to connect to the blockchain using the client library. This connection allows you to retrieve data through queries but can't perform operations that modify the blockchain state $CODE_EXAMPLES_SECTION_PAGINATED getAllAssets nextCursor When you retrieve a large number of entities, they are often delivered in batches called PaginatedEntity $CODE_EXAMPLES_SECTION_AUTHENTICATING WRITE SKIP $CODE_EXAMPLES_SECTION_AUTOMATIC_SIGNATURES WRITE SKIP $CODE_EXAMPLES_SECTION_SIGNATURES WRITE SKIP $CODE_EXAMPLES_SECTION_ADMIN WRITE SKIP $CODE_EXAMPLES_SECTION_COMPLEX WRITE SKIP skip signed txs authenticate signatures admin complex txs no sample keys no invented 64-hex.
        Official BUILD ft4/pagination ($PAGINATION_INDEX_URL 307 $PAGINATION_INDEX_URL_SLASH 200 $PAGINATION_INDEX_TITLE): intro Each query in FT4 has pagination enabled by default The query will return a paged result containing the result and the cursor to fetch the next page skip signed txs no sample keys no invented 64-hex.
        Official BUILD ft4/prioritization ($PRIORITIZATION_INDEX_URL 307 $PRIORITIZATION_INDEX_URL_SLASH 200 $PRIORITIZATION_INDEX_TITLE): intro The prioritization is used to determine which transactions should be processed first on the blockchain The FT4 library offers a basic default implementation which calculates priority based on account activity, configured rate limits, and transaction cost in terms of points If dapps require a more complex way of calculating priority the priority_check function can be extended Core concepts Priority value Account points Transaction cost points import lib.ft4.core.prioritization.default import lib.ft4.core.prioritization extend priority_check priority_check_v1 gtx_api no_op_priority_state no_account_priority_state Query-only skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/setup/imports ($FT4_IMPORTS_INDEX_URL 307 $FT4_IMPORTS_INDEX_URL_SLASH 200 $FT4_IMPORTS_INDEX_TITLE): intro When you import the entire FT4 library, you might unintentionally expose many operations It's crucial to consider whether you want to import the admin module The library is split into modules that can be imported separately to have granular control FT4 library modules accounts assets auth cross-chain prioritization test utils Risks of importing all modules Imported entities and queries Exported queries and operations Paginating large entity queries Normal imports vs core imports import lib.ft4 import lib.ft4.core The crosschain module import lib.ft4.crosschain WRITE SKIP admin module import sample admin pubkey skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/configuration-values ($CONFIG_VALUES_INDEX_URL 307 $CONFIG_VALUES_INDEX_URL_SLASH 200 $CONFIG_VALUES_INDEX_TITLE): intro This topic provides the key configuration options available for FT4 modules, providing descriptions, types, and default values for each parameter You only need to configure the modules that you import in your dapp. If a module is not imported, its configuration is not required How to configure chromia.yml moduleArgs per blockchain Query-only skip admin module in production skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/configuration-values INDEX ($FT4_CONFIGURATION_VALUES_INDEX_URL 307 $FT4_CONFIGURATION_VALUES_INDEX_URL_SLASH 200 $FT4_CONFIGURATION_VALUES_INDEX_TITLE): slash title WRITE SKIP.
        Official BUILD ft4/releases/ft4 ($RELEASES_FT4_INDEX_URL 307 $RELEASES_FT4_INDEX_URL_SLASH 200 $RELEASES_FT4_INDEX_TITLE): intro All notable changes to this project will be documented in this file The format is based on Keep a changelog, and this project adheres to Semantic versioning Query-only skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official BUILD ft4/terms ($TERMS_INDEX_URL 307 $TERMS_INDEX_URL_SLASH 200 $TERMS_INDEX_TITLE): intro This section provides a high-level explanation of the terms most commonly used in the documentation for the FT4 library Account Different account types User accounts Lock accounts System accounts Asset Auth descriptor Auth handler Balance Expiration rules User Query-only skip signed txs no sample keys no invented 64-hex sample admin pubkey.
        Official GET-STARTED get-started/about/protocols/ft4 INDEX ($GET_STARTED_FT4_INDEX_URL 307 $GET_STARTED_FT4_INDEX_URL_SLASH 200 $GET_STARTED_FT4_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only.
        Official GET-STARTED get-started/about/protocols INDEX ($GET_STARTED_PROTOCOLS_INDEX_URL 307 $GET_STARTED_PROTOCOLS_INDEX_URL_SLASH 200 $GET_STARTED_PROTOCOLS_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only.
        Official GET-STARTED get-started/about/protocols/gtv INDEX ($GET_STARTED_GTV_INDEX_URL 307 $GET_STARTED_GTV_INDEX_URL_SLASH 200 $GET_STARTED_GTV_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only.
        Official GET-STARTED get-started/about/protocols-summary INDEX ($GET_STARTED_PROTOCOLS_SUMMARY_INDEX_URL 307 $GET_STARTED_PROTOCOLS_SUMMARY_INDEX_URL_SLASH 200 $GET_STARTED_PROTOCOLS_SUMMARY_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only.
        Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure/user-types INDEX ($ECOSYSTEM_GOV_USER_TYPES_INDEX_URL 307 $ECOSYSTEM_GOV_USER_TYPES_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_USER_TYPES_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official ECOSYSTEM ecosystem/governance/governance-voting-process/voting-types INDEX ($ECOSYSTEM_GOV_VOTING_TYPES_INDEX_URL 307 $ECOSYSTEM_GOV_VOTING_TYPES_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_VOTING_TYPES_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/book-review/build-client/complete-example INDEX ($LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_URL 301 $LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/book-review/book-review-entity/tables INDEX ($LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_URL 301 $LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/chromia-for-evm-developers/asset-transfer INDEX ($LEARN_EVM_ASSET_TRANSFER_INDEX_URL 301 $LEARN_EVM_ASSET_TRANSFER_INDEX_URL_SLASH 200 H1 $LEARN_EVM_ASSET_TRANSFER_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/ft4-asset/consideration-recomendations INDEX ($LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_URL 301 $LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_URL_SLASH 200 H1 $LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN tags/FT4 INDEX ($LEARN_TAGS_FT4_INDEX_URL 301 $LEARN_TAGS_FT4_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_FT4_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official releases: $RELEASES_URL (200). Official $RELEASES_404_URL is 404.
        Official changelog latest listed $DOCS_LATEST_FT4 ($DOCS_LATEST_FT4_DATE). Official docs pin remains $FT4_VERSION / API $FT4_API.
        Official 1.1.0r: get_api_version starts at 1 for "1.1.0"; get_account_by_id also returns type.
        Official intro Account ID formulas only (hash(public_key) / hash(evm_address)); no invented 64-hex.
        Client read: $CLIENT_SETUP_URL createConnection(client).getAllAssets() (already on chr_generate_client_help).
        Memo requirement query: $MEMO_URL does_account_require_memo(account_id: byte_array): boolean.
        Mount prefix on official setup/verify is ft4.* (ft4.get_all_assets, ft4.get_config, ft4.get_version).
        Official priority_check_v1 is mounted gtx_api (source), not ft4.* — do not invent ft4.priority_check_v1.
        Queries / config only. NEVER import ${DappScaffold.forbiddenModules.joinToString(", ")}.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("ft4", FT4_VERSION)
        put("ft4Api", FT4_API)
        put("tool", TOOL_NAME)
        put("docs", PAGINATION_URL)
        put("pagination_index_docs", PAGINATION_INDEX_URL)
        put("pagination_index_url_slash", PAGINATION_INDEX_URL_SLASH)
        put("pagination_index_title", PAGINATION_INDEX_TITLE)
        put("setup", SETUP_URL)
        put("ft4_setup_page_index_docs", FT4_SETUP_PAGE_INDEX_URL)
        put("ft4_setup_page_index_url_slash", FT4_SETUP_PAGE_INDEX_URL_SLASH)
        put("ft4_setup_page_index_title", FT4_SETUP_PAGE_INDEX_TITLE)
        put("ft4_setup_index_docs", FT4_SETUP_INDEX_URL)
        put("ft4_setup_index_url_slash", FT4_SETUP_INDEX_URL_SLASH)
        put("ft4_setup_index_title", FT4_SETUP_INDEX_TITLE)
        put("client_setup", CLIENT_SETUP_URL)
        put("client_setup_index_docs", CLIENT_SETUP_INDEX_URL)
        put("client_setup_index_url_slash", CLIENT_SETUP_INDEX_URL_SLASH)
        put("client_setup_index_title", CLIENT_SETUP_INDEX_TITLE)
        put("memo", MEMO_URL)
        put("prioritization_docs", PRIORITIZATION_URL)
        put("prioritization_index_docs", PRIORITIZATION_INDEX_URL)
        put("prioritization_index_url_slash", PRIORITIZATION_INDEX_URL_SLASH)
        put("prioritization_index_title", PRIORITIZATION_INDEX_TITLE)
        put("terms_docs", TERMS_URL)
        put("terms_index_docs", TERMS_INDEX_URL)
        put("terms_index_url_slash", TERMS_INDEX_URL_SLASH)
        put("terms_index_title", TERMS_INDEX_TITLE)
        put("intro_docs", INTRO_URL)
        put("intro_url_slash", INTRO_URL_SLASH)
        put("intro_title", INTRO_TITLE)
        put("intro_index_docs", INTRO_INDEX_URL)
        put("intro_index_url_slash", INTRO_INDEX_URL_SLASH)
        put("intro_index_title", INTRO_INDEX_TITLE)
        put("intro_card_account", INTRO_CARD_ACCOUNT)
        put("intro_card_terms", INTRO_CARD_TERMS)
        put("intro_card_account_url", INTRO_CARD_ACCOUNT_URL)
        put("setup_index_docs", SETUP_INDEX_URL)
        put("setup_index_url_slash", SETUP_INDEX_URL_SLASH)
        put("setup_index_title", SETUP_INDEX_TITLE)
        put("setup_index_card_setup", SETUP_INDEX_CARD_SETUP)
        put("setup_index_card_imports", SETUP_INDEX_CARD_IMPORTS)
        put("client_index_docs", CLIENT_INDEX_URL)
        put("client_index_url_slash", CLIENT_INDEX_URL_SLASH)
        put("client_index_title", CLIENT_INDEX_TITLE)
        put("client_index_card_setup", CLIENT_INDEX_CARD_SETUP)
        put("client_index_card_auth", CLIENT_INDEX_CARD_AUTH)
        put("client_index_card_register", CLIENT_INDEX_CARD_REGISTER)
        put("client_index_card_login", CLIENT_INDEX_CARD_LOGIN)
        put("client_index_card_key_store", CLIENT_INDEX_CARD_KEY_STORE)
        put("client_index_card_transfer", CLIENT_INDEX_CARD_TRANSFER)
        put("client_index_card_orchestrator", CLIENT_INDEX_CARD_ORCHESTRATOR)
        put("client_auth_descriptors_docs", CLIENT_AUTH_DESCRIPTORS_URL)
        put("client_auth_descriptors_index_docs", CLIENT_AUTH_DESCRIPTORS_INDEX_URL)
        put("client_auth_descriptors_index_url_slash", CLIENT_AUTH_DESCRIPTORS_INDEX_URL_SLASH)
        put("client_auth_descriptors_index_title", CLIENT_AUTH_DESCRIPTORS_INDEX_TITLE)
        put("client_account_registration_docs", CLIENT_ACCOUNT_REGISTRATION_URL)
        put("client_account_registration_index_docs", CLIENT_ACCOUNT_REGISTRATION_INDEX_URL)
        put("client_account_registration_index_url_slash", CLIENT_ACCOUNT_REGISTRATION_INDEX_URL_SLASH)
        put("client_account_registration_index_title", CLIENT_ACCOUNT_REGISTRATION_INDEX_TITLE)
        put("client_login_docs", CLIENT_LOGIN_URL)
        put("client_login_index_docs", CLIENT_LOGIN_INDEX_URL)
        put("client_login_index_url_slash", CLIENT_LOGIN_INDEX_URL_SLASH)
        put("client_login_index_title", CLIENT_LOGIN_INDEX_TITLE)
        put("client_key_store_docs", CLIENT_KEY_STORE_URL)
        put("client_key_store_index_docs", CLIENT_KEY_STORE_INDEX_URL)
        put("client_key_store_index_url_slash", CLIENT_KEY_STORE_INDEX_URL_SLASH)
        put("client_key_store_index_title", CLIENT_KEY_STORE_INDEX_TITLE)
        put("client_transfer_assets_docs", CLIENT_TRANSFER_ASSETS_URL)
        put("client_transfer_assets_index_docs", CLIENT_TRANSFER_ASSETS_INDEX_URL)
        put("client_transfer_assets_index_url_slash", CLIENT_TRANSFER_ASSETS_INDEX_URL_SLASH)
        put("client_transfer_assets_index_title", CLIENT_TRANSFER_ASSETS_INDEX_TITLE)
        put("client_orchestrator_docs", CLIENT_ORCHESTRATOR_URL)
        put("client_orchestrator_index_docs", CLIENT_ORCHESTRATOR_INDEX_URL)
        put("client_orchestrator_index_url_slash", CLIENT_ORCHESTRATOR_INDEX_URL_SLASH)
        put("client_orchestrator_index_title", CLIENT_ORCHESTRATOR_INDEX_TITLE)
        put("backend_index_docs", BACKEND_INDEX_URL)
        put("backend_index_url_slash", BACKEND_INDEX_URL_SLASH)
        put("backend_index_title", BACKEND_INDEX_TITLE)
        put("backend_index_card_accounts", BACKEND_INDEX_CARD_ACCOUNTS)
        put("backend_index_card_authentication", BACKEND_INDEX_CARD_AUTHENTICATION)
        put("backend_index_card_assets", BACKEND_INDEX_CARD_ASSETS)
        put("backend_index_card_cross_chain", BACKEND_INDEX_CARD_CROSS_CHAIN)
        put("backend_accounts_docs", BACKEND_ACCOUNTS_URL)
        put("backend_accounts_index_docs", BACKEND_ACCOUNTS_INDEX_URL)
        put("backend_accounts_index_url_slash", BACKEND_ACCOUNTS_INDEX_URL_SLASH)
        put("backend_accounts_index_title", BACKEND_ACCOUNTS_INDEX_TITLE)
        put("backend_account_linking_index_docs", BACKEND_ACCOUNT_LINKING_INDEX_URL)
        put("backend_account_linking_index_url_slash", BACKEND_ACCOUNT_LINKING_INDEX_URL_SLASH)
        put("backend_account_linking_index_title", BACKEND_ACCOUNT_LINKING_INDEX_TITLE)
        put("backend_accounts_overview_index_docs", BACKEND_ACCOUNTS_OVERVIEW_INDEX_URL)
        put("backend_accounts_overview_index_url_slash", BACKEND_ACCOUNTS_OVERVIEW_INDEX_URL_SLASH)
        put("backend_accounts_overview_index_title", BACKEND_ACCOUNTS_OVERVIEW_INDEX_TITLE)
        put("backend_accounts_open_index_docs", BACKEND_ACCOUNTS_OPEN_INDEX_URL)
        put("backend_accounts_open_index_url_slash", BACKEND_ACCOUNTS_OPEN_INDEX_URL_SLASH)
        put("backend_accounts_open_index_title", BACKEND_ACCOUNTS_OPEN_INDEX_TITLE)
        put("backend_accounts_fixed_index_docs", BACKEND_ACCOUNTS_FIXED_INDEX_URL)
        put("backend_accounts_fixed_index_url_slash", BACKEND_ACCOUNTS_FIXED_INDEX_URL_SLASH)
        put("backend_accounts_fixed_index_title", BACKEND_ACCOUNTS_FIXED_INDEX_TITLE)
        put("backend_accounts_subscription_index_docs", BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_URL)
        put("backend_accounts_subscription_index_url_slash", BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_URL_SLASH)
        put("backend_accounts_subscription_index_title", BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_TITLE)
        put("backend_accounts_and_auth_descriptors_index_docs", BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_URL)
        put("backend_accounts_and_auth_descriptors_index_url_slash", BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_URL_SLASH)
        put("backend_accounts_and_auth_descriptors_index_title", BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_TITLE)
        put("backend_authentication_docs", BACKEND_AUTHENTICATION_URL)
        put("backend_authentication_index_docs", BACKEND_AUTHENTICATION_INDEX_URL)
        put("backend_authentication_index_url_slash", BACKEND_AUTHENTICATION_INDEX_URL_SLASH)
        put("backend_authentication_index_title", BACKEND_AUTHENTICATION_INDEX_TITLE)
        put("backend_authentication_auth_index_docs", BACKEND_AUTHENTICATION_AUTH_INDEX_URL)
        put("backend_authentication_auth_index_url_slash", BACKEND_AUTHENTICATION_AUTH_INDEX_URL_SLASH)
        put("backend_authentication_auth_index_title", BACKEND_AUTHENTICATION_AUTH_INDEX_TITLE)
        put("backend_authentication_auth_descriptors_and_rules_index_docs", BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_URL)
        put("backend_authentication_auth_descriptors_and_rules_index_url_slash", BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_URL_SLASH)
        put("backend_authentication_auth_descriptors_and_rules_index_title", BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_TITLE)
        put("backend_authentication_multi_sig_index_docs", BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_URL)
        put("backend_authentication_multi_sig_index_url_slash", BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_URL_SLASH)
        put("backend_authentication_multi_sig_index_title", BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_TITLE)
        put("backend_assets_docs", BACKEND_ASSETS_URL)
        put("backend_assets_index_docs", BACKEND_ASSETS_INDEX_URL)
        put("backend_assets_index_url_slash", BACKEND_ASSETS_INDEX_URL_SLASH)
        put("backend_assets_index_title", BACKEND_ASSETS_INDEX_TITLE)
        put("backend_assets_register_assets_index_docs", BACKEND_ASSETS_REGISTER_ASSETS_INDEX_URL)
        put("backend_assets_register_assets_index_url_slash", BACKEND_ASSETS_REGISTER_ASSETS_INDEX_URL_SLASH)
        put("backend_assets_register_assets_index_title", BACKEND_ASSETS_REGISTER_ASSETS_INDEX_TITLE)
        put("backend_assets_asset_amounts_index_docs", BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_URL)
        put("backend_assets_asset_amounts_index_url_slash", BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_URL_SLASH)
        put("backend_assets_asset_amounts_index_title", BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_TITLE)
        put("backend_assets_locking_assets_index_docs", BACKEND_ASSETS_LOCKING_ASSETS_INDEX_URL)
        put("backend_assets_locking_assets_index_url_slash", BACKEND_ASSETS_LOCKING_ASSETS_INDEX_URL_SLASH)
        put("backend_assets_locking_assets_index_title", BACKEND_ASSETS_LOCKING_ASSETS_INDEX_TITLE)
        put("backend_cross_chain_docs", BACKEND_CROSS_CHAIN_URL)
        put("backend_cross_chain_index_docs", BACKEND_CROSS_CHAIN_INDEX_URL)
        put("backend_cross_chain_index_url_slash", BACKEND_CROSS_CHAIN_INDEX_URL_SLASH)
        put("backend_cross_chain_index_title", BACKEND_CROSS_CHAIN_INDEX_TITLE)
        put("backend_cross_chain_introduction_index_docs", BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_URL)
        put("backend_cross_chain_introduction_index_url_slash", BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_URL_SLASH)
        put("backend_cross_chain_introduction_index_title", BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_TITLE)
        put("backend_cross_chain_assets_index_docs", BACKEND_CROSS_CHAIN_ASSETS_INDEX_URL)
        put("backend_cross_chain_assets_index_url_slash", BACKEND_CROSS_CHAIN_ASSETS_INDEX_URL_SLASH)
        put("backend_cross_chain_assets_index_title", BACKEND_CROSS_CHAIN_ASSETS_INDEX_TITLE)
        put("backend_cross_chain_transfers_index_docs", BACKEND_CROSS_CHAIN_TRANSFERS_INDEX_URL)
        put("backend_cross_chain_transfers_index_url_slash", BACKEND_CROSS_CHAIN_TRANSFERS_INDEX_URL_SLASH)
        put("backend_cross_chain_transfers_index_title", BACKEND_CROSS_CHAIN_TRANSFERS_INDEX_TITLE)
        put("backend_cross_chain_automate_registration_index_docs", BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_URL)
        put("backend_cross_chain_automate_registration_index_url_slash", BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_URL_SLASH)
        put("backend_cross_chain_automate_registration_index_title", BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_TITLE)
        put("automate_crosschain_asset_index_docs", AUTOMATE_CROSSCHAIN_ASSET_INDEX_URL)
        put("automate_crosschain_asset_index_url_slash", AUTOMATE_CROSSCHAIN_ASSET_INDEX_URL_SLASH)
        put("automate_crosschain_asset_index_title", AUTOMATE_CROSSCHAIN_ASSET_INDEX_TITLE)
        put("account_mgmt_index_docs", ACCOUNT_MGMT_INDEX_URL)
        put("account_mgmt_index_url_slash", ACCOUNT_MGMT_INDEX_URL_SLASH)
        put("account_mgmt_index_title", ACCOUNT_MGMT_INDEX_TITLE)
        put("account_management_index_docs", ACCOUNT_MANAGEMENT_INDEX_URL)
        put("account_management_index_url_slash", ACCOUNT_MANAGEMENT_INDEX_URL_SLASH)
        put("account_management_index_title", ACCOUNT_MANAGEMENT_INDEX_TITLE)
        put("account_mgmt_index_card_manage", ACCOUNT_MGMT_INDEX_CARD_MANAGE)
        put("account_mgmt_index_card_auth", ACCOUNT_MGMT_INDEX_CARD_AUTH)
        put("account_mgmt_index_card_multisig", ACCOUNT_MGMT_INDEX_CARD_MULTISIG)
        put("account_mgmt_overview_docs", ACCOUNT_MGMT_OVERVIEW_URL)
        put("account_mgmt_overview_index_docs", ACCOUNT_MGMT_OVERVIEW_INDEX_URL)
        put("account_mgmt_overview_index_url_slash", ACCOUNT_MGMT_OVERVIEW_INDEX_URL_SLASH)
        put("account_mgmt_overview_index_title", ACCOUNT_MGMT_OVERVIEW_INDEX_TITLE)
        put("account_mgmt_auth_descriptors_docs", ACCOUNT_MGMT_AUTH_DESCRIPTORS_URL)
        put("account_mgmt_auth_descriptors_index_docs", ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_URL)
        put("account_mgmt_auth_descriptors_index_url_slash", ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_URL_SLASH)
        put("account_mgmt_auth_descriptors_index_title", ACCOUNT_MGMT_AUTH_DESCRIPTORS_INDEX_TITLE)
        put("account_mgmt_multisig_docs", ACCOUNT_MGMT_MULTISIG_URL)
        put("account_mgmt_multisig_index_docs", ACCOUNT_MGMT_MULTISIG_INDEX_URL)
        put("account_mgmt_multisig_index_url_slash", ACCOUNT_MGMT_MULTISIG_INDEX_URL_SLASH)
        put("account_mgmt_multisig_index_title", ACCOUNT_MGMT_MULTISIG_INDEX_TITLE)
        put("asset_mgmt_index_docs", ASSET_MGMT_INDEX_URL)
        put("asset_mgmt_index_url_slash", ASSET_MGMT_INDEX_URL_SLASH)
        put("asset_mgmt_index_title", ASSET_MGMT_INDEX_TITLE)
        put("asset_management_index_docs", ASSET_MANAGEMENT_INDEX_URL)
        put("asset_management_index_url_slash", ASSET_MANAGEMENT_INDEX_URL_SLASH)
        put("asset_management_index_title", ASSET_MANAGEMENT_INDEX_TITLE)
        put("asset_mgmt_index_card_manage", ASSET_MGMT_INDEX_CARD_MANAGE)
        put("asset_mgmt_index_card_transfer", ASSET_MGMT_INDEX_CARD_TRANSFER)
        put("asset_mgmt_asset_docs", ASSET_MGMT_ASSET_URL)
        put("asset_mgmt_asset_index_docs", ASSET_MGMT_ASSET_INDEX_URL)
        put("asset_mgmt_asset_index_url_slash", ASSET_MGMT_ASSET_INDEX_URL_SLASH)
        put("asset_mgmt_asset_index_title", ASSET_MGMT_ASSET_INDEX_TITLE)
        put("asset_mgmt_transfer_assets_docs", ASSET_MGMT_TRANSFER_ASSETS_URL)
        put("asset_mgmt_transfer_assets_index_docs", ASSET_MGMT_TRANSFER_ASSETS_INDEX_URL)
        put("asset_mgmt_transfer_assets_index_url_slash", ASSET_MGMT_TRANSFER_ASSETS_INDEX_URL_SLASH)
        put("asset_mgmt_transfer_assets_index_title", ASSET_MGMT_TRANSFER_ASSETS_INDEX_TITLE)
        put("code_examples_docs", CODE_EXAMPLES_URL)
        put("code_examples_url_slash", CODE_EXAMPLES_URL_SLASH)
        put("code_examples_title", CODE_EXAMPLES_TITLE)
        put("code_examples_index_docs", CODE_EXAMPLES_INDEX_URL)
        put("code_examples_index_url_slash", CODE_EXAMPLES_INDEX_URL_SLASH)
        put("code_examples_index_title", CODE_EXAMPLES_INDEX_TITLE)
        put("code_examples_section_create_connection", CODE_EXAMPLES_SECTION_CREATE_CONNECTION)
        put("code_examples_section_paginated", CODE_EXAMPLES_SECTION_PAGINATED)
        put("code_examples_section_authenticating", CODE_EXAMPLES_SECTION_AUTHENTICATING)
        put("code_examples_section_automatic_signatures", CODE_EXAMPLES_SECTION_AUTOMATIC_SIGNATURES)
        put("code_examples_section_signatures", CODE_EXAMPLES_SECTION_SIGNATURES)
        put("code_examples_section_admin", CODE_EXAMPLES_SECTION_ADMIN)
        put("code_examples_section_complex", CODE_EXAMPLES_SECTION_COMPLEX)
        put("imports_docs", IMPORTS_URL)
        put("ft4_imports_index_docs", FT4_IMPORTS_INDEX_URL)
        put("ft4_imports_index_url_slash", FT4_IMPORTS_INDEX_URL_SLASH)
        put("ft4_imports_index_title", FT4_IMPORTS_INDEX_TITLE)
        put("config_values_docs", CONFIG_VALUES_URL)
        put("config_values_index_docs", CONFIG_VALUES_INDEX_URL)
        put("config_values_index_url_slash", CONFIG_VALUES_INDEX_URL_SLASH)
        put("config_values_index_title", CONFIG_VALUES_INDEX_TITLE)
        put("ft4_configuration_values_index_docs", FT4_CONFIGURATION_VALUES_INDEX_URL)
        put("ft4_configuration_values_index_url_slash", FT4_CONFIGURATION_VALUES_INDEX_URL_SLASH)
        put("ft4_configuration_values_index_title", FT4_CONFIGURATION_VALUES_INDEX_TITLE)
        put("releases_docs", RELEASES_URL)
        put("releases_ft4_index_docs", RELEASES_FT4_INDEX_URL)
        put("releases_ft4_index_url_slash", RELEASES_FT4_INDEX_URL_SLASH)
        put("releases_ft4_index_title", RELEASES_FT4_INDEX_TITLE)
        put("releases_404", RELEASES_404_URL)
        put("docs_latest_ft4", DOCS_LATEST_FT4)
        put("docs_latest_ft4_date", DOCS_LATEST_FT4_DATE)
        put("rell_api", FT4_RELL_API)
        put("source", SOURCE)
        put("get_started_ft4_index_docs", GET_STARTED_FT4_INDEX_URL)
        put("get_started_ft4_index_url_slash", GET_STARTED_FT4_INDEX_URL_SLASH)
        put("get_started_ft4_index_title", GET_STARTED_FT4_INDEX_TITLE)
        put("get_started_protocols_index_docs", GET_STARTED_PROTOCOLS_INDEX_URL)
        put("get_started_protocols_index_url_slash", GET_STARTED_PROTOCOLS_INDEX_URL_SLASH)
        put("get_started_protocols_index_title", GET_STARTED_PROTOCOLS_INDEX_TITLE)
        put("get_started_gtv_index_docs", GET_STARTED_GTV_INDEX_URL)
        put("get_started_gtv_index_url_slash", GET_STARTED_GTV_INDEX_URL_SLASH)
        put("get_started_gtv_index_title", GET_STARTED_GTV_INDEX_TITLE)
        put("get_started_protocols_summary_index_docs", GET_STARTED_PROTOCOLS_SUMMARY_INDEX_URL)
        put("get_started_protocols_summary_index_url_slash", GET_STARTED_PROTOCOLS_SUMMARY_INDEX_URL_SLASH)
        put("get_started_protocols_summary_index_title", GET_STARTED_PROTOCOLS_SUMMARY_INDEX_TITLE)
        put("read_only", true)
        put(
            "commands",
            buildJsonObject {
                put("get_version", "chr query ft4.get_version")
                put("get_api_version", "chr query ft4.get_api_version")
                put("get_all_assets", "chr query ft4.get_all_assets page_size=10 page_cursor=null")
                put("get_config", "chr query ft4.get_config")
                put("get_assets_by_name", "chr query ft4.get_assets_by_name name=MyAsset page_size=10 page_cursor=null")
            }
        )
        put("version_queries", buildJsonArray { versionQueries.forEach { add(JsonPrimitive(it)) } })
        put("asset_queries", buildJsonArray { assetQueries.forEach { add(JsonPrimitive(it)) } })
        put("account_queries", buildJsonArray { accountQueries.forEach { add(JsonPrimitive(it)) } })
        put("memo_queries", buildJsonArray { memoQueries.forEach { add(JsonPrimitive(it)) } })
        put("priority_queries", buildJsonArray { priorityQueries.forEach { add(JsonPrimitive(it)) } })
        put("priority_imports", buildJsonArray { priorityImports.forEach { add(JsonPrimitive(it)) } })
        put("priority_states", buildJsonArray { priorityStates.forEach { add(JsonPrimitive(it)) } })
        put("priority_extend_example", priorityExtendExample())
        put("pagination", paginationNote())
        put("prioritization", prioritizationNote())
        put("leftover_terms", buildJsonArray { leftoverTerms.forEach { add(JsonPrimitive(it)) } })
        put("leftover_intro", buildJsonArray { leftoverIntro.forEach { add(JsonPrimitive(it)) } })
        put("skipped_write", buildJsonArray { skipped.forEach { add(JsonPrimitive(it)) } })
        put("generate_client_help", "chr_generate_client_help")
        put("ecosystem_gov_user_types_index_url_slash", ECOSYSTEM_GOV_USER_TYPES_INDEX_URL_SLASH)
        put("ecosystem_gov_user_types_index_title", ECOSYSTEM_GOV_USER_TYPES_INDEX_TITLE)
        put("ecosystem_gov_voting_types_index_url_slash", ECOSYSTEM_GOV_VOTING_TYPES_INDEX_URL_SLASH)
        put("ecosystem_gov_voting_types_index_title", ECOSYSTEM_GOV_VOTING_TYPES_INDEX_TITLE)
        put("learn_book_review_complete_example_index_url_slash", LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_URL_SLASH)
        put("learn_book_review_complete_example_index_title", LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_TITLE)
        put("learn_book_review_review_tables_index_url_slash", LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_URL_SLASH)
        put("learn_book_review_review_tables_index_title", LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_TITLE)
        put("learn_evm_asset_transfer_index_url_slash", LEARN_EVM_ASSET_TRANSFER_INDEX_URL_SLASH)
        put("learn_evm_asset_transfer_index_title", LEARN_EVM_ASSET_TRANSFER_INDEX_TITLE)
        put("learn_ft4_asset_considerations_index_url_slash", LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_URL_SLASH)
        put("learn_ft4_asset_considerations_index_title", LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_TITLE)
        put("learn_tags_ft4_index_url_slash", LEARN_TAGS_FT4_INDEX_URL_SLASH)
        put("learn_tags_ft4_index_title", LEARN_TAGS_FT4_INDEX_TITLE)
        put("notes", notes())
    }
}
// Official BUILD ft4/account-management/multisig leftovers encoded as ACCOUNT_MGMT_MULTISIG_INDEX_* (query-only).
// Official BUILD ft4/asset-management/asset leftovers encoded as ASSET_MGMT_ASSET_INDEX_* (query-only).
// Official BUILD ft4/asset-management/transfer-assets leftovers encoded as ASSET_MGMT_TRANSFER_ASSETS_INDEX_* (query-only).
// Official BUILD ft4/backend/accounts/account-linking leftovers encoded as BACKEND_ACCOUNT_LINKING_INDEX_* (query-only).
// Official BUILD ft4/backend/accounts/overview leftovers encoded as BACKEND_ACCOUNTS_OVERVIEW_INDEX_* (query-only).
// Official BUILD ft4/backend/accounts/open leftovers encoded as BACKEND_ACCOUNTS_OPEN_INDEX_* (query-only).
// Official BUILD ft4/backend/accounts/fixed leftovers encoded as BACKEND_ACCOUNTS_FIXED_INDEX_* (query-only).
// Official BUILD ft4/backend/accounts/subscription leftovers encoded as BACKEND_ACCOUNTS_SUBSCRIPTION_INDEX_* (query-only).
// Official BUILD ft4/backend/accounts/accounts-and-auth-descriptors leftovers encoded as BACKEND_ACCOUNTS_AND_AUTH_DESCRIPTORS_INDEX_* (query-only).
// Official BUILD ft4/backend/authentication/auth leftovers encoded as BACKEND_AUTHENTICATION_AUTH_INDEX_* (query-only).
// Official BUILD ft4/backend/authentication/auth-descriptors-and-rules leftovers encoded as BACKEND_AUTHENTICATION_AUTH_DESCRIPTORS_AND_RULES_INDEX_* (query-only).
// Official BUILD ft4/backend/authentication/multi-sig leftovers encoded as BACKEND_AUTHENTICATION_MULTI_SIG_INDEX_* (query-only).
// Official BUILD ft4/backend/assets/register-assets leftovers encoded as BACKEND_ASSETS_REGISTER_ASSETS_INDEX_* (query-only).
// Official BUILD ft4/backend/assets/asset-amounts leftovers encoded as BACKEND_ASSETS_ASSET_AMOUNTS_INDEX_* (query-only).
// Official BUILD ft4/backend/assets/locking-assets leftovers encoded as BACKEND_ASSETS_LOCKING_ASSETS_INDEX_* (query-only).
// Official BUILD ft4/backend/cross-chain/introduction leftovers encoded as BACKEND_CROSS_CHAIN_INTRODUCTION_INDEX_* (query-only).
// Official BUILD ft4/backend/cross-chain/cross-chain-assets leftovers encoded as BACKEND_CROSS_CHAIN_ASSETS_INDEX_* (query-only).
// Official BUILD ft4/backend/cross-chain/cross-chain-transfers leftovers encoded as BACKEND_CROSS_CHAIN_TRANSFERS_INDEX_* (query-only).
// Official BUILD ft4/backend/cross-chain/automate-cross-chain-asset-registration leftovers encoded as BACKEND_CROSS_CHAIN_AUTOMATE_REGISTRATION_INDEX_* (query-only).
// Official BUILD ft4/backend/cross-chain/automate-cross-chain-asset-registration INDEX leftovers encoded as AUTOMATE_CROSSCHAIN_ASSET_INDEX_* (query-only).
// Official BUILD ft4/prioritization leftovers encoded as PRIORITIZATION_INDEX_* (query-only).
// Official BUILD ft4/code-examples leftovers encoded as CODE_EXAMPLES_INDEX_* (query-only).
// Official BUILD ft4/intro leftovers encoded as INTRO_INDEX_* (query-only).
// Official BUILD ft4/account-management leftovers encoded as ACCOUNT_MANAGEMENT_INDEX_* (query-only).
// Official BUILD ft4/asset-management leftovers encoded as ASSET_MANAGEMENT_INDEX_* (query-only).
// Official BUILD ft4/configuration-values INDEX leftovers encoded as FT4_CONFIGURATION_VALUES_INDEX_* (query-only).
// Official BUILD ft4/setup/ft4-setup INDEX leftovers encoded as FT4_SETUP_INDEX_* (query-only).
// Official GET-STARTED get-started/about/protocols/ft4 INDEX leftovers encoded as GET_STARTED_FT4_INDEX_* (query-only).
// Official GET-STARTED get-started/about/protocols INDEX leftovers encoded as GET_STARTED_PROTOCOLS_INDEX_* (query-only).
// Official GET-STARTED get-started/about/protocols/gtv INDEX leftovers encoded as GET_STARTED_GTV_INDEX_* (query-only).
// Official GET-STARTED get-started/about/protocols-summary INDEX leftovers encoded as GET_STARTED_PROTOCOLS_SUMMARY_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure/user-types INDEX leftovers encoded as ECOSYSTEM_GOV_USER_TYPES_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/governance/governance-voting-process/voting-types INDEX leftovers encoded as ECOSYSTEM_GOV_VOTING_TYPES_INDEX_* (query-only).
// Official LEARN courses/book-review/build-client/complete-example INDEX leftovers encoded as LEARN_BOOK_REVIEW_COMPLETE_EXAMPLE_INDEX_* (query-only).
// Official LEARN courses/book-review/book-review-entity/tables INDEX leftovers encoded as LEARN_BOOK_REVIEW_REVIEW_TABLES_INDEX_* (query-only).
// Official LEARN courses/chromia-for-evm-developers/asset-transfer INDEX leftovers encoded as LEARN_EVM_ASSET_TRANSFER_INDEX_* (query-only).
// Official LEARN courses/ft4-asset/consideration-recomendations INDEX leftovers encoded as LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_* (query-only).
// Official LEARN tags/FT4 INDEX leftovers encoded as LEARN_TAGS_FT4_INDEX_* (query-only HELP ONLY WRITE SKIP).
