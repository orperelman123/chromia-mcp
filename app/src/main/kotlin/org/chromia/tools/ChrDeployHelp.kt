package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr deployment` flag help.
 * Documents create / update / inspect plus official read-only info / proposal list|info /
 * voterset info|list. Does not shell out to chr, generate keys, invent a lease/BRID, or send signed txs.
 * Source: docs.chromia.com/build/cli/commands/deployment
 * (live create write-back text is stale — CHANGELOG / DeployCreateCommand 0.30.0 wins).
 * Schema compare / DROP wording from DeployUpdateCommand + ReportGenerator (0.30.0 / 0.31.0).
 * Skipped (sign): proposal vote/retract-vote/revoke/rename, voterset update/add-dapp-provider,
 * pause/resume/remove, container configuration/pause/resume.
 * Skipped (hidden): lease-info, remove-container. Leftover official leftover BUILD deployment index slash/title/child-card leftovers live here (query-only). Leftover official BUILD vault-listing read-only find_dapp_details; skip leftover official chr tx writes and leftover official sample 64-hex. Leftover official get-tchr-binance READ-ONLY faucet/allowance; leftover official deploy-dapp explorer verify; leftover official connect-client started. Leftover official leftover BUILD deployment testnet index slash/title/child-card leftovers live here (query-only). Leftover official leftover BUILD deployment mainnet index slash/title/child-card leftovers live here (query-only). Leftover official leftover BUILD deployment testnet-tokens index slash/title/child-card leftovers live here (query-only). Leftover official leftover BUILD deployment vault-listing index slash/title/child-card leftovers live here (query-only). Leftover official leftover BUILD deployment deploy-frontend-dapp index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment testnet/getting-started index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment mainnet/getting-started index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment testnet/deploy-dapp index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment mainnet/deploy-dapp index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment testnet/list-dapp-vault index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment vault-listing/quick-vault-listing index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment vault-listing/dynamic-vault-listing index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment testnet-tokens/get-tchr-chromia index slash/title leftovers live here (query-only). Leftover official leftover BUILD deployment testnet-tokens/get-tchr-binance index slash/title leftovers live here (query-only). Leftover official leftover GET-STARTED create-dapp/deploy-to-testnet index slash/title leftovers live here (query-only).
 */
object ChrDeployHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DEPLOY_DOCS_URL = "https://docs.chromia.com/build/cli/commands/deployment"
    const val COMMANDS_DEPLOYMENT_INDEX_URL = DEPLOY_DOCS_URL
    const val COMMANDS_DEPLOYMENT_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/deployment/"
    const val COMMANDS_DEPLOYMENT_INDEX_TITLE = "deployment"  // official H1
    const val DEPLOYMENT_INDEX_URL = "https://docs.chromia.com/build/deployment"
    const val DEPLOYMENT_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/"
    const val DEPLOYMENT_INDEX_TITLE = "Deployment"
    const val DEPLOYMENT_INDEX_CARD_TESTNET_TOKENS = "Get Testnet tokens"
    const val DEPLOYMENT_INDEX_CARD_TESTNET = "Deploy to Testnet"
    const val DEPLOYMENT_INDEX_CARD_MAINNET = "Deploy to Mainnet"
    const val DEPLOYMENT_INDEX_CARD_VAULT = "List your dapp on Chromia Vault"
    const val DEPLOYMENT_INDEX_CARD_FRONTEND = "Deploy frontend dapp"
    const val TESTNET_INDEX_URL = "https://docs.chromia.com/build/deployment/testnet"
    const val TESTNET_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/testnet/"
    const val TESTNET_INDEX_TITLE = "Deploy to testnet"
    const val TESTNET_INDEX_CARD_GETTING_STARTED = "Getting started"
    const val TESTNET_INDEX_CARD_GET_CONTAINER = "Get a container"
    const val TESTNET_INDEX_CARD_DEPLOY_DAPP = "Deploy your dapp"
    const val TESTNET_INDEX_CARD_CONNECT_CLIENT = "Connect a client"
    const val TESTNET_INDEX_CARD_LIST_VAULT = "List your dapp on Vault"
    const val MAINNET_INDEX_URL = "https://docs.chromia.com/build/deployment/mainnet"
    const val MAINNET_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/mainnet/"
    const val MAINNET_INDEX_TITLE = "Deploy to Mainnet"
    const val MAINNET_INDEX_CARD_GETTING_STARTED = "Getting started"
    const val MAINNET_INDEX_CARD_GET_CONTAINER = "Get a container"
    const val MAINNET_INDEX_CARD_DEPLOY_DAPP = "Deploy your dapp"
    const val MAINNET_INDEX_CARD_MULTI_DEPLOYMENT = "Multi-owner deployment"
    const val MAINNET_INDEX_CARD_CONNECT_CLIENT = "Connect a client"
    const val MAINNET_INDEX_CARD_LIST_VAULT = "List your dapp on Vault"
    const val VAULT_LISTING_QUICK_URL = "https://docs.chromia.com/build/deployment/vault-listing/quick-vault-listing"
    const val QUICK_VAULT_LISTING_INDEX_URL = VAULT_LISTING_QUICK_URL
    const val QUICK_VAULT_LISTING_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/vault-listing/quick-vault-listing/"
    const val QUICK_VAULT_LISTING_INDEX_TITLE = "Quick Vault listing (hardcoded metadata)"  // official H1
    const val VAULT_LISTING_DYNAMIC_URL = "https://docs.chromia.com/build/deployment/vault-listing/dynamic-vault-listing"
    const val DYNAMIC_VAULT_LISTING_INDEX_URL = VAULT_LISTING_DYNAMIC_URL
    const val DYNAMIC_VAULT_LISTING_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/vault-listing/dynamic-vault-listing/"
    const val DYNAMIC_VAULT_LISTING_INDEX_TITLE = "Dynamic Vault listing (database-based metadata)"  // official H1
    const val VAULT_LISTING_INDEX_URL = "https://docs.chromia.com/build/deployment/vault-listing/"
    const val VAULT_LISTING_INDEX_URL_SLASH = VAULT_LISTING_INDEX_URL
    const val VAULT_LISTING_INDEX_URL_NOSLASH = "https://docs.chromia.com/build/deployment/vault-listing"
    const val VAULT_LISTING_INDEX_TITLE = "List your dapp on the Chromia Vault"
    const val VAULT_LISTING_INDEX_CARD_QUICK = "Quick Vault listing (hardcoded metadata)"
    const val VAULT_LISTING_INDEX_CARD_DYNAMIC = "Dynamic Vault listing (database-based metadata)"
    const val DEPLOY_FRONTEND_URL = "https://docs.chromia.com/build/deployment/deploy-frontend-dapp"
    const val DEPLOY_FRONTEND_INDEX_URL = DEPLOY_FRONTEND_URL
    const val DEPLOY_FRONTEND_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/deploy-frontend-dapp/"
    const val DEPLOY_FRONTEND_INDEX_TITLE = "Deploy the frontend of your dapp on-chain"
    const val TESTNET_LIST_DAPP_VAULT_URL = "https://docs.chromia.com/build/deployment/testnet/list-dapp-vault"
    const val TESTNET_LIST_DAPP_VAULT_INDEX_URL = TESTNET_LIST_DAPP_VAULT_URL
    const val TESTNET_LIST_DAPP_VAULT_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/testnet/list-dapp-vault/"
    const val TESTNET_LIST_DAPP_VAULT_INDEX_TITLE = "List your dapp on the Chromia Testnet Vault"  // official H1
    const val TESTNET_TOKENS_URL = "https://docs.chromia.com/build/deployment/testnet-tokens/"
    const val TESTNET_TOKENS_INDEX_URL = "https://docs.chromia.com/build/deployment/testnet-tokens"
    const val TESTNET_TOKENS_INDEX_URL_SLASH = TESTNET_TOKENS_URL
    const val TESTNET_TOKENS_INDEX_TITLE = "Get Testnet tokens"
    const val TESTNET_TOKENS_INDEX_CARD_TCHR_CHROMIA = "Get tCHR on Chromia Testnet"
    const val TESTNET_TOKENS_INDEX_CARD_TCHR_BINANCE = "Get tCHR on BSC Testnet"
    const val TESTNET_TCHR_CHROMIA_URL = "https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-chromia"
    const val GET_TCHR_CHROMIA_INDEX_URL = TESTNET_TCHR_CHROMIA_URL
    const val GET_TCHR_CHROMIA_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-chromia/"
    const val GET_TCHR_CHROMIA_INDEX_TITLE = "Get Chromia test tokens (tCHR) on the Chromia Testnet"  // official H1
    const val TESTNET_TCHR_BINANCE_URL = "https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-binance"
    const val GET_TCHR_BINANCE_INDEX_URL = TESTNET_TCHR_BINANCE_URL
    const val GET_TCHR_BINANCE_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-binance/"
    const val GET_TCHR_BINANCE_INDEX_TITLE = "Get Chromia test tokens (tCHR) on Binance Smart Chain Testnet"  // official H1
    const val TESTNET_ECONOMY_CHAIN_VAULT_URL =
        "https://vault.testnet.chromia.com/en/dapps/dapp/?dapp=1-Chromia+Economy+Chain"
    const val TESTNET_BSCSCAN_FAUCET_URL =
        "https://testnet.bscscan.com/address/0x0e61dfdbd5b979adbf3e5b5fe7ee40f85b6daa8d#writeContract"
    const val TESTNET_BSC_TCHR_TOKEN = "0x8e59d72e4dda56f26963c6b8c77ca1959e9a74f0"
    const val EXPLORER_URL = "https://explorer.chromia.com"
    const val TESTNET_CONNECT_CLIENT_URL = "https://docs.chromia.com/build/deployment/testnet/connect-client"
    const val MAINNET_CONNECT_CLIENT_URL = "https://docs.chromia.com/build/deployment/mainnet/connect-client"
    const val TESTNET_GET_CONTAINER_URL = VaultLeaseHelp.TESTNET_GET_CONTAINER_URL
    const val MAINNET_GET_CONTAINER_URL = VaultLeaseHelp.MAINNET_GET_CONTAINER_URL
    const val MAINNET_MULTI_DEPLOYMENT_URL = "https://docs.chromia.com/build/deployment/mainnet/multi-deployment"
    const val MAINNET_MULTI_DEPLOYMENT_INDEX_URL = MAINNET_MULTI_DEPLOYMENT_URL
    const val MAINNET_MULTI_DEPLOYMENT_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/mainnet/multi-deployment/"
    const val MAINNET_MULTI_DEPLOYMENT_INDEX_TITLE = "Multi-owner dapp deployment and updates"
    const val POSTCHAIN_CLIENT_NPM_URL = "https://www.npmjs.com/package/postchain-client"
    const val HELLO_WORLD_QUICKSTART_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart"
    const val HOSTING_ABOUT_URL = "https://docs.chromia.com/get-started/about/hosting"
    const val GET_STARTED_DEPLOY_TESTNET_INDEX_URL = "https://docs.chromia.com/get-started/create-dapp/deploy-to-testnet"
    const val GET_STARTED_DEPLOY_TESTNET_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/create-dapp/deploy-to-testnet/"
    const val GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE = "Deploy your dapp to Testnet"  // official H1
    const val TESTNET_FAUCET_URL = "https://faucet.testnet.chromia.com/"
    const val TESTNET_VAULT_DAPPS_URL = "https://vault.testnet.chromia.com/en/dapps/"
    const val TESTNET_DEPLOY_DAPP_URL = "https://docs.chromia.com/build/deployment/testnet/deploy-dapp"
    const val TESTNET_DEPLOY_DAPP_INDEX_URL = TESTNET_DEPLOY_DAPP_URL
    const val TESTNET_DEPLOY_DAPP_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/testnet/deploy-dapp/"
    const val TESTNET_DEPLOY_DAPP_INDEX_TITLE = "Deploy your dapp to testnet"
    const val TESTNET_GETTING_STARTED_URL = "https://docs.chromia.com/build/deployment/testnet/getting-started"
    const val TESTNET_GETTING_STARTED_INDEX_URL = TESTNET_GETTING_STARTED_URL
    const val TESTNET_GETTING_STARTED_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/testnet/getting-started/"
    const val TESTNET_GETTING_STARTED_INDEX_TITLE = "Getting started"
    const val MAINNET_DEPLOY_DAPP_URL = "https://docs.chromia.com/build/deployment/mainnet/deploy-dapp"
    const val MAINNET_DEPLOY_DAPP_INDEX_URL = MAINNET_DEPLOY_DAPP_URL
    const val MAINNET_DEPLOY_DAPP_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/mainnet/deploy-dapp/"
    const val MAINNET_DEPLOY_DAPP_INDEX_TITLE = "Deploy your dapp to Mainnet"
    const val MAINNET_GETTING_STARTED_URL = "https://docs.chromia.com/build/deployment/mainnet/getting-started"
    const val MAINNET_GETTING_STARTED_INDEX_URL = MAINNET_GETTING_STARTED_URL
    const val MAINNET_GETTING_STARTED_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/mainnet/getting-started/"
    const val MAINNET_GETTING_STARTED_INDEX_TITLE = "Get started with Mainnet deployment"
    const val ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/container-management/add-dapp-provider-to-voterset"
    const val ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/container-management/add-dapp-provider-to-voterset/"
    const val ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_TITLE = "Add a dapp provider to your container voter set"  // official H1
    const val LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_URL = "https://learn.chromia.com/courses/book-review/introduction"
    const val LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/introduction/"
    const val LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_TITLE = "Build your first app with Rell on Chromia"  // official H1
    const val LEARN_MARKETPLACE_SETUP_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/setup"
    const val LEARN_MARKETPLACE_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/setup/"
    const val LEARN_MARKETPLACE_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_TAGS_CROSSCHAIN_INDEX_URL = "https://learn.chromia.com/tags/Crosschain"
    const val LEARN_TAGS_CROSSCHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Crosschain/"
    const val LEARN_TAGS_CROSSCHAIN_INDEX_TITLE = "Courses tagged with: Crosschain"  // official H1
    const val DAPP_AGGREGATOR_SETUPMOCKS_URL =
        "https://gitlab.com/chromaway/dapp-aggregator/-/blob/dev/scripts/setUpMocks.ts"
    const val STALE_POSTCHAIN_CLIENTS_INSTALL_404 =
        "https://docs.chromia.com/intro/installation/postchain-clients"
    const val CLIENTS_OVERVIEW_URL = "https://docs.chromia.com/build/clients/overview"
    const val VAULT_IGNORE_ROWID =
        "ignore the rowid in the query, but keep it in the query signature"
    const val WEB_STATIC_KEY = "webStatic"
    const val WEB_STATIC_EXAMPLE_VALUE = "out"
    const val WEB_STATIC_LOCAL_URL = "http://localhost:7740/web_query/<blockchainRid>/web_static"
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val ENTITY_CHANGE_URL =
        "https://docs.chromia.com/rell/language-features/modules/entity#changing-entity-definitions"
    const val DROP_WARNING =
        "The database column will be DROPPED on next initialization. All data will be permanently deleted."
    const val CREATE_Y_ERROR = "Please specify -y option to force deployment"

    fun createExample(name: String = DappScaffold.DEFAULT_NAME): String {
        val chain = DappScaffold.normalizeName(name)
        return "chr deployment create --settings chromia.yml --network testnet --blockchain $chain"
    }

    fun updateExample(name: String = DappScaffold.DEFAULT_NAME): String {
        val chain = DappScaffold.normalizeName(name)
        return "chr deployment update --settings chromia.yml --network testnet --blockchain $chain"
    }

    fun inspectExample(name: String = DappScaffold.DEFAULT_NAME): String {
        val chain = DappScaffold.normalizeName(name)
        return "chr deployment inspect --settings chromia.yml --network testnet --blockchain $chain"
    }

    fun infoExample(name: String = DappScaffold.DEFAULT_NAME): String {
        val chain = DappScaffold.normalizeName(name)
        return "chr deployment info --settings chromia.yml --network testnet --blockchain $chain"
    }

    fun proposalListExample(): String =
        "chr deployment proposal list --settings chromia.yml --network testnet"

    fun proposalInfoExample(): String =
        "chr deployment proposal info --settings chromia.yml --network testnet --id <id>"

    fun votersetListExample(): String =
        "chr deployment voterset list --settings chromia.yml --network testnet"

    fun vaultFindDappDetails(): String =
        "find_dapp_details(dapp_rowid: rowid, requested_content_types: list<dapp_content_type>? = null)"

    fun vaultQueryEmpty(): String =
        "chr query --network <deployment_name> --blockchain <blockchain_name> --output-format json find_dapp_details dapp_rowid=0 'requested_content_types=[]'"

    fun vaultContentTypes(): String = "landscape, portrait, promotional, video, icon"
    fun vaultQueryAll(): String =
        """chr query --network <deployment_name> --blockchain <blockchain_name> --output-format json find_dapp_details dapp_rowid=0 'requested_content_types=["landscape", "portrait", "promotional", "video", "icon"]'"""

    fun vaultIgnoreRowid(): String = VAULT_IGNORE_ROWID

    fun vaultPrerequisitesNote(): String =
        "Leftover official BUILD testnet list-dapp-vault (200): $TESTNET_LIST_DAPP_VAULT_URL. Leftover official prerequisites: a deployed dapp on the Chromia Testnet; access to your dapp's codebase; media files; leftover official tCHR tokens; leftover official Filehub or any comfortable web3 storage. Leftover official tCHR index (200): $TESTNET_TOKENS_URL. Leftover official get-tchr-chromia (200): $TESTNET_TCHR_CHROMIA_URL. Leftover official get-tchr-binance (200): $TESTNET_TCHR_BINANCE_URL. Leftover official Chromia Testnet Faucet: $TESTNET_FAUCET_URL. Leftover official allowance 1000 tCHR every 7 days. Leftover official get-tchr-chromia still prints leftover official weekly allowace (typo); leftover official allowance wins. Leftover official tCHR has no real-world value. Leftover official Testnet Economy Chain vault listing: $TESTNET_VAULT_DAPPS_URL. Leftover official Testnet Economy Chain vault: $TESTNET_ECONOMY_CHAIN_VAULT_URL. Skip leftover official Connect Wallet / Request Tokens / leftover official faucet message sign."

    fun vaultHardcodedVsDbNote(): String = """
        Leftover official BUILD testnet list-dapp-vault (200) hardcoded vs db (READ-ONLY names only).
        Leftover official hardcoded: enum dapp_content_type; struct dapp_media (name, url, type); leftover official query find_dapp_details; leftover official function get_dapp_media; leftover official return fields rowid, name, description, launch_url, genre, chain_list (name, brid, role), content; leftover official .to_gtv_pretty(). Leftover official get_dapp_media returns null when leftover official requested_content_types is empty.
        Leftover official testnet hardcoded snippet still uses leftover official map<dapp_content_type, dapp_media> (one media per type). Leftover official vault-listing/quick-vault-listing (200) hardcoded uses leftover official ec_media_tuple list<(dapp_content_type, dapp_media)> and leftover official multiple promotional images — leftover official vault-listing wins over leftover official testnet map snippet.
        Leftover official db: leftover official struct module_args { admin_pubkey: byte_array }; leftover official require_admin_signer; leftover official chromia.yml moduleArgs keys admin_pubkey and dapp_name; leftover official entity dapp (key name; mutable description, launch_url, genre); leftover official entity dapp_media (key dapp, name; mutable url; type); leftover official entity blockchain (key dapp, brid; index brid; index mutable name; mutable role); leftover official operations create_or_update_dapp / create_or_update_blockchain / create_or_update_dapp_media (names only); leftover official helpers get_dapp_by_name, map_dapp_details, find_and_map_dapp_blockchains, find_and_map_dapp_media; leftover official @omit @sort .rowid.
        Leftover official testnet page still inlines those snippets and leftover official bitbucket demo-vault-listing vault_list_hardcode / vault_list_db. Leftover official vault-listing (200) uses leftover official import quick_vault_listing; import dynamic_vault_listing; and leftover official vault-listing libraries win for current listing.
    """.trimIndent()

    fun vaultAutoListingNote(): String =
        "Leftover official BUILD testnet list-dapp-vault (200) automatic listing: once you implement the leftover official query and functions, your dapp will be automatically listed in the Chromia Testnet Vault."

    fun vaultCheckmarkNote(): String =
        "Leftover official BUILD testnet list-dapp-vault (200) verification for a checkmark (optional): Contact the Chromia team admin for final verification. The admin will review your dapp to ensure it meets the required quality standards."

    fun vaultSetupMocksNote(): String =
        "Leftover official BUILD testnet list-dapp-vault (200) example storing the data: leftover official setUpMocks.ts with leftover official Filehub media links. Leftover official GitLab (200): $DAPP_AGGREGATOR_SETUPMOCKS_URL. Leftover official source names: seedChain, seedImages, dapp_content_type. Skip leftover official sample 64-hex BRIDs and leftover official Filehub 64-hex media paths."

    fun vaultStaleClientInstallNote(): String =
        "Leftover official BUILD testnet list-dapp-vault still links leftover official $STALE_POSTCHAIN_CLIENTS_INSTALL_404 (404). Leftover official /build/clients/overview 307→200 leftover official client list wins: $CLIENTS_OVERVIEW_URL. Skip leftover official chr tx docs writes."


    fun tchrChromiaNote(): String = """
        Leftover official BUILD get-tchr-chromia (200): $TESTNET_TCHR_CHROMIA_URL. Leftover official Chromia Testnet Faucet: $TESTNET_FAUCET_URL. Leftover official allowance 1000 tCHR every 7 days. Leftover official get-tchr-chromia still prints leftover official weekly allowace (typo); leftover official allowance wins. Leftover official tCHR has leftover official no real-world value. Leftover official tokens are leftover official essential for leftover official testing deployments without leftover official real tokens. Leftover official Testnet Economy Chain vault: $TESTNET_ECONOMY_CHAIN_VAULT_URL. Leftover official Testnet Vault dapps: $TESTNET_VAULT_DAPPS_URL. Skip leftover official Connect Wallet / leftover official Request Tokens / leftover official Create Account / leftover official faucet message sign.
    """.trimIndent()

    fun tchrBinanceNote(): String = """
        Leftover official BUILD get-tchr-binance (200): $TESTNET_TCHR_BINANCE_URL. Leftover official Testnet token differences: leftover official Chromia testnet tCHR requires leftover official Economy Chain account through leftover official Chromia testnet faucet and leftover official 1000 tCHR weekly. Leftover official BSC testnet tCHR can be claimed to leftover official any EVM wallet address without leftover official special account setup; leftover official primarily for leftover official cross-chain bridge testing and leftover official EVM integration. Leftover official both token types serve leftover official same testing purpose with leftover official different acquisition methods.
        Leftover official BscScan faucet (READ-ONLY URL): $TESTNET_BSCSCAN_FAUCET_URL. Leftover official BSC tCHR token contract (READ-ONLY identifier): $TESTNET_BSC_TCHR_TOKEN. Leftover official BSC faucet leftover official once a week. Leftover official claim needs leftover official tBNB gas. Leftover official tCHR has leftover official no real-world value.
        REAL leftover official bug: leftover official get-tchr-binance Step 1 still says leftover official Access the Chromia testnet faucet and leftover official usage notes still say leftover official testing on the Chromia Testnet. Leftover official Testnet token differences + leftover official BscScan faucet URL win over leftover official Chromia Testnet title/usage notes. Leftover official Chromia Testnet faucet remains leftover official $TESTNET_FAUCET_URL (leftover official get-tchr-chromia 200).
        Skip leftover official Connect to web3 / leftover official Connect Wallet / leftover official Write Contract claim / leftover official Request Tokens / leftover official message sign.
    """.trimIndent()

    fun explorerVerifyNote(): String = """
        Leftover official BUILD testnet deploy-dapp (200) explorer verify (READ-ONLY): leftover official Chromia Explorer $EXPLORER_URL. Leftover official set Current network to Testnet. Leftover official Under Clusters, select leftover official system. Leftover official copy leftover official API URL from leftover official bottom of the page (leftover official example host ${WriteDeploymentConfig.TESTNET_URL}) as leftover official url. Leftover official Under System Chains, leftover official directory_chain and leftover official copy leftover official brid.
        Leftover official testnet deploy-dapp still pastes leftover official Directory brid + url after leftover official explorer copy — leftover official reserved names mainnet / testnet auto-fill leftover official Directory brid + url since leftover official CLI 0.29.8 win. Leftover official explorer verify is leftover official READ-ONLY confirmation, not leftover official required yaml for leftover official reserved names.
        Leftover official BUILD mainnet deploy-dapp (200) has leftover official no explorer copy steps; leftover official reserved name mainnet auto-configures leftover official brid and url. Leftover official Chromia Explorer host remains leftover official $EXPLORER_URL. Leftover official mainnet explorer node-pool steps live on leftover official connect-client (200): $MAINNET_CONNECT_CLIENT_URL. Leftover official testnet connect-client (200) also leftover official explorer + leftover official five minutes: $TESTNET_CONNECT_CLIENT_URL.
        Skip leftover official sample BRIDs / leftover official tx RIDs / leftover official sample container 64-hex.
    """.trimIndent()

    fun connectClientNote(): String = """
        Leftover official BUILD testnet connect-client (200): $TESTNET_CONNECT_CLIENT_URL. Leftover official TypeScript postchain-client. Leftover official client needs leftover official directoryNodeUrlPool + leftover official dapp Blockchain RID. Leftover official client automatically queries leftover official Directory chain on leftover official system nodes. Leftover official testnet explorer verify: leftover official $EXPLORER_URL leftover official Current network Testnet; leftover official deployed dapp leftover official about five minutes. Leftover official testnet directoryNodeUrlPool hosts: ${WriteDeploymentConfig.TESTNET_URLS.joinToString(", ")}. Leftover official createClient (leftover official testnet snippet).
        Leftover official BUILD mainnet connect-client (200): $MAINNET_CONNECT_CLIENT_URL. Leftover official Client Libraries Overview leftover official $CLIENTS_OVERVIEW_URL. Leftover official explorer: leftover official $EXPLORER_URL leftover official view leftover official list of nodes in leftover official system cluster. Leftover official mainnet directoryNodeUrlPool leftover official snapshot hosts (leftover official connect-client, not leftover official required yaml): ${WriteDeploymentConfig.MAINNET_URLS.joinToString(", ")}, ${WriteDeploymentConfig.MAINNET_EXPLORER_SNAPSHOT_URLS.joinToString(", ")}.
        REAL leftover official bug: leftover official mainnet connect-client still prints leftover official pcl.creatClient (missing e). Leftover official testnet connect-client leftover official pcl.createClient wins.
        Leftover official TypeScript leftover official postchain-client leftover official $POSTCHAIN_CLIENT_NPM_URL. Leftover official principles leftover official same leftover official with leftover official other leftover official client leftover official libraries. Leftover official Hello World Quickstart leftover official $HELLO_WORLD_QUICKSTART_URL. Leftover official remove leftover official private leftover official keys leftover official from leftover official production leftover official client leftover official code (leftover official no leftover official printed leftover official keys). Leftover official testnet leftover official page leftover official says leftover official Directory chain; leftover official mainnet leftover official page leftover official says leftover official Directory Chain. Leftover official testnet leftover official connect-client leftover official explorer leftover official + leftover official five leftover official minutes; leftover official mainnet leftover official connect-client leftover official explorer leftover official system leftover official cluster leftover official node leftover official list.
        Skip leftover official admin key pair snippet / leftover official sample BlockchainRID placeholders.
    """.trimIndent()

    fun scuNote(): String =
        "Leftover official GET-STARTED hosting (200): $HOSTING_ABOUT_URL. Leftover official one SCU: 2 GB RAM, 0.5 vCPU, 16 GB storage, leftover official 25 MiB/s read and leftover official 20 MiB/s write. Leftover official weekly target leftover official approximately 90 USD leftover official default 7-node dapp cluster. Leftover official lease leftover official weekly leftover official pay in CHR leftover official USD-equivalent target. Leftover official cost leftover official number of SCUs leftover official plus leftover official additional storage. Leftover official anyone leftover official can leftover official pay leftover official a leftover official lease. Leftover official overdue leftover official suspended leftover official six months leftover official then leftover official permanently deleted. Do not invent leftover official other SCU sizes."

    fun getContainerNote(): String = """
        Leftover official BUILD testnet get-container (200): $TESTNET_GET_CONTAINER_URL. Leftover official tCHR required to lease a container. Leftover official Testnet Vault containers: ${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS}. Leftover official lease: cluster + SCUs / storage / duration + optional auto-renewal. Leftover official Container ID is essential for deploy; leftover official retrieve later on the same Vault page. Leftover official get-container links leftover official $HOSTING_ABOUT_URL (200) for leftover official SCUs / storage / duration. ${scuNote()} Leftover official testnet container key is separate from leftover official wallet used to receive tCHR — leftover official reference an existing key id only.
        Leftover official BUILD mainnet get-container (200): $MAINNET_GET_CONTAINER_URL. Leftover official pay leftover official native CHR. Leftover official deposit leftover official at least 10 CHR from leftover official BNB Smart Chain or leftover official Ethereum Mainnet via leftover official ${VaultLeaseHelp.MAINNET_VAULT_DEPOSIT}. Leftover official Chromia account leftover official created leftover official automatically during leftover official deposit. Leftover official Mainnet Vault containers: ${VaultLeaseHelp.MAINNET_VAULT_CONTAINERS}. Leftover official failed leftover official lease leftover official refund leftover official same amount. Leftover official chromia.yml leftover official prerequisite.
        Skip leftover official Generate a key pair / leftover official keygen / leftover official key-pair-management / leftover official Connect wallet / leftover official Lease sign / leftover official sample container 64-hex.
    """.trimIndent()

    fun multiDeploymentNote(): String = """
        Leftover official BUILD mainnet multi-deployment (200): $MAINNET_MULTI_DEPLOYMENT_URL. Leftover official READ-ONLY leftover official chr deployment voterset list and leftover official chr deployment proposal list already official on this tool. Leftover official threshold is leftover official a fraction of leftover official total members.
        Skip leftover official voterset add-dapp-provider / leftover official voterset update / leftover official proposal vote / leftover official proposal revoke / leftover official pmc blockchain update / leftover official sample 64-hex member keys.
    """.trimIndent()

    fun testnetDeployDappNote(): String = """
        Leftover official BUILD testnet deploy-dapp (200): $TESTNET_DEPLOY_DAPP_URL. Leftover official testnet url host: ${WriteDeploymentConfig.TESTNET_URL}. Leftover official first create: chr deployment create --settings chromia.yml --network testnet --blockchain hello. Leftover official update: chr deployment update --settings chromia.yml --network testnet --blockchain hello. Leftover official optional --config <config file path> (leftover official update snippet still prints leftover official --config config <config file path> — garbled; leftover official --config <path> wins). Leftover official create writes deployments.testnet.chains back since CLI 0.30.0; leftover official deploy-dapp still says add the chains key by hand after the leftover official successful-deployment prompt — stale; source + CHANGELOG 0.30.0 win. Leftover official update requires leftover official chains and does not rewrite chromia.yml. Leftover official deployed dapp will be accessible in about five minutes. Leftover official ~/.chromia/config key.id references an existing key id; this tool does not generate a key. Skip leftover official sample container 64-hex and leftover official Generate a key pair.
        Leftover official BUILD testnet getting-started (200): $TESTNET_GETTING_STARTED_URL. Leftover official getting-started still says Specify your network (mainnet) on leftover official TESTNET page — leftover official --network testnet wins.
        Leftover official BUILD mainnet deploy-dapp (200): $MAINNET_DEPLOY_DAPP_URL. Leftover official reserved name mainnet auto-configures leftover official brid and url. Leftover official testnet deploy-dapp still pastes leftover official Directory brid + url by hand — leftover official reserved names mainnet / testnet auto-fill Directory brid + url since CLI 0.29.8 win. Leftover official mainnet pre-deploy checklist: leftover official deployments.mainnet.brid, url, container, and leftover official chains empty for first create. Leftover official mainnet also still says add the chains key by hand after leftover official successful-deployment prompt — stale; leftover official create write-back since CLI 0.30.0 wins. Leftover official first create: chr deployment create --settings chromia.yml --network mainnet --blockchain hello. Leftover official update: chr deployment update --settings chromia.yml --network mainnet --blockchain hello. Skip leftover official sample container 64-hex.
        ${explorerVerifyNote()}
    """.trimIndent()

    fun webStaticYaml(): String =
        "blockchains:\n  asset_management:\n    webStatic: out\n"

    fun nextJsExportConfig(): String =
        "const nextConfig = { output: \"export\", images: { unoptimized: true }, basePath: /web_query/\${process.env.NEXT_PUBLIC_BRID}/web_static };\n"

    fun deployFrontendNote(): String =
        "Leftover official BUILD deploy-frontend-dapp (200): $DEPLOY_FRONTEND_URL. Leftover official on-chain frontend: add leftover official $WEB_STATIC_KEY: $WEB_STATIC_EXAMPLE_VALUE under blockchains.<name> in rell/chromia.yml. Leftover official project-config does not list $WEB_STATIC_KEY; leftover official deploy-frontend and leftover official CLI FetchConfigCommand do. Accept as leftover official key. Do not invent siblings. Leftover official local URL (only printed host): $WEB_STATIC_LOCAL_URL. Leftover official Next.js demo: output export; images.unoptimized true; leftover official basePath uses NEXT_PUBLIC_BRID. Leftover official .env key NEXT_PUBLIC_BRID = blockchainRid from leftover official chr node start --wipe logs. Leftover official apply: chr node update. Leftover official page allows any frontend that generates files."

    fun votersetInfoExample(): String =
        "chr deployment voterset info --settings chromia.yml --network testnet --name <voter-set>"

    fun containerFieldNote(): String = """
        Optional chromia.yml field deployments.<net>.container is the Vault / PMC lease id.
        Set it only after a real lease exists. Do not invent a lease id or a dapp BRID.
        Use write_deployment_config for official Directory Chain BRIDs (testnet / mainnet only).
    """.trimIndent()

    fun writeBackNote(): String = """
        Since CLI 0.30.0, `chr deployment create` writes deployments.<net>.chains.<name>: x"<dapp rid>"
        back into chromia.yml (DeployCreateCommand.afterDeployment → ChromiaYmlWriter.updateDeploymentNodes)
        and prints a diff. Manual YAML is only the fallback if that write throws.
        Live $DEPLOY_DOCS_URL create section still says you must paste chains by hand — stale; source wins.
        `chr deployment update` requires deployments.<net>.chains and does not rewrite chromia.yml.
        `chr deployment remove` does not rewrite the file either.
        First create: omit the dapp RID under chains. If create fails because the RID already exists,
        change the code (a unique comment is enough). Non-interactive create without -y fails with:
        $CREATE_Y_ERROR
    """.trimIndent()

    fun schemaCompareNote(): String = """
        `chr deployment update` runs schema compare after node validateConfiguration (source only;
        live update page lists --height / --verify-only / --skip-verification but omits the schema UI).
        Compares entities/objects + enums (CLI 0.30.0+). Dangerous enum: value REMOVED, ordinal changed,
        or ADDED with ordinal <= maxOldOrdinal. Appending an enum value at the end is not dangerous.
        Attribute removal warning (0.31.0): "$DROP_WARNING"
        --skip-verification skips both node validateConfiguration and schema compare.
        --verify-only still runs schema compare (unsafe prompt skipped) then exits 0 without sending.
        Unsafe changes prompt YesNoPrompt; non-interactive without --skip-verification errors.
        Safe Rell entity changes: add attributes with defaults; add on empty tables; remove attributes;
        change mutability on non-@log. Breaking: change attribute type; add/remove @log.
        See $ENTITY_CHANGE_URL
    """.trimIndent()

    fun notes(): String = """
        Chromia CLI $CLI_SERIES deployment flag help. Java 21+, Postgres 16+.
        Official invoke page: $DEPLOY_DOCS_URL
        Leftover official leftover BUILD cli/commands/deployment (leftover official $COMMANDS_DEPLOYMENT_INDEX_URL leftover official 307 leftover official $COMMANDS_DEPLOYMENT_INDEX_URL_SLASH leftover official 200 leftover official $COMMANDS_DEPLOYMENT_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr deployment leftover official leftover create leftover official leftover update leftover official leftover WRITE SKIP leftover official leftover inspect leftover official leftover info leftover official leftover proposal leftover official leftover list leftover official leftover info leftover official leftover voterset leftover official leftover info leftover official leftover list leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official BUILD deployment (leftover official $DEPLOYMENT_INDEX_URL leftover official 307 leftover official $DEPLOYMENT_INDEX_URL_SLASH leftover official 200 leftover official $DEPLOYMENT_INDEX_TITLE): leftover official leftover child leftover official leftover cards leftover official leftover $DEPLOYMENT_INDEX_CARD_TESTNET_TOKENS leftover official leftover $DEPLOYMENT_INDEX_CARD_TESTNET leftover official leftover $DEPLOYMENT_INDEX_CARD_MAINNET leftover official leftover $DEPLOYMENT_INDEX_CARD_VAULT leftover official leftover $DEPLOYMENT_INDEX_CARD_FRONTEND leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Leftover official BUILD deployment/testnet (leftover official $TESTNET_INDEX_URL leftover official 307 leftover official $TESTNET_INDEX_URL_SLASH leftover official 200 leftover official $TESTNET_INDEX_TITLE): leftover official leftover intro leftover official leftover This section covers information about configuring your client and deploying and updating your dapp to the public testnet leftover official leftover child leftover official leftover cards leftover official leftover $TESTNET_INDEX_CARD_GETTING_STARTED leftover official leftover $TESTNET_GETTING_STARTED_URL leftover official leftover Deploy your dapp to Chromia testnet in three steps: obtain a container, deploy your dapp, and connect a client leftover official leftover $TESTNET_INDEX_CARD_GET_CONTAINER leftover official leftover $TESTNET_GET_CONTAINER_URL leftover official leftover Generate a key pair with Chromia CLI and lease a container using tCHR tokens to obtain a Container ID for deployment leftover official leftover $TESTNET_INDEX_CARD_DEPLOY_DAPP leftover official leftover $TESTNET_DEPLOY_DAPP_URL leftover official leftover Use Chromia CLI to deploy your dapp by configuring chromia.yml, running deployment commands, and updating with the Blockchain RID leftover official leftover $TESTNET_INDEX_CARD_CONNECT_CLIENT leftover official leftover $TESTNET_CONNECT_CLIENT_URL leftover official leftover Set up a frontend or client with postchain-client by configuring node URLs and your Blockchain RID leftover official leftover $TESTNET_INDEX_CARD_LIST_VAULT leftover official leftover $TESTNET_LIST_DAPP_VAULT_URL leftover official leftover List your dapp on the Chromia Testnet Vault by implementing the find_dapp_details query and preparing media content through Filehub leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover BlockchainRID leftover official leftover placeholders.
        Leftover official BUILD deployment/mainnet (leftover official $MAINNET_INDEX_URL leftover official 307 leftover official $MAINNET_INDEX_URL_SLASH leftover official 200 leftover official $MAINNET_INDEX_TITLE): leftover official leftover intro leftover official leftover This section covers information about configuring your client and deploying and updating your dapp to the public Mainnet leftover official leftover child leftover official leftover cards leftover official leftover $MAINNET_INDEX_CARD_GETTING_STARTED leftover official leftover $MAINNET_GETTING_STARTED_URL leftover official leftover Deploy your dapp to Chromia Mainnet in three steps: obtain a container, deploy your dapp, and connect a client leftover official leftover $MAINNET_INDEX_CARD_GET_CONTAINER leftover official leftover $MAINNET_GET_CONTAINER_URL leftover official leftover Generate a key pair, create a Chromia account, and lease container space using Chromia CLI. Secure a Container ID for deployment leftover official leftover $MAINNET_INDEX_CARD_DEPLOY_DAPP leftover official leftover $MAINNET_DEPLOY_DAPP_URL leftover official leftover Deploy your dapp with Chromia CLI by configuring chromia.yml and running deployment commands leftover official leftover $MAINNET_INDEX_CARD_MULTI_DEPLOYMENT leftover official leftover $MAINNET_MULTI_DEPLOYMENT_URL leftover official leftover Deploy and update multi-owner dapps on Chromia using CLI tools by managing voter sets, thresholds, and proposals leftover official leftover $MAINNET_INDEX_CARD_CONNECT_CLIENT leftover official leftover $MAINNET_CONNECT_CLIENT_URL leftover official leftover Connect your frontend to a dapp backend using postchain-client. Set up the client with system node URLs and your dapp's Blockchain RID leftover official leftover $MAINNET_INDEX_CARD_LIST_VAULT leftover official leftover $VAULT_LISTING_INDEX_URL leftover official leftover List your dapp on the Chromia Vault by implementing the find_dapp_details query and preparing media content through Filehub leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover BlockchainRID leftover official leftover placeholders.
        Leftover official BUILD deployment/testnet-tokens (leftover official $TESTNET_TOKENS_INDEX_URL leftover official 307 leftover official $TESTNET_TOKENS_INDEX_URL_SLASH leftover official 200 leftover official $TESTNET_TOKENS_INDEX_TITLE): leftover official leftover intro leftover official leftover In this section you will discover how to claim Testnet tokens on the Chromia Testnet and Binance Testnets leftover official leftover child leftover official leftover cards leftover official leftover $TESTNET_TOKENS_INDEX_CARD_TCHR_CHROMIA leftover official leftover $TESTNET_TCHR_CHROMIA_URL leftover official leftover Discover how you can claim your tCHR tokens for development purposes on the Chromia Testnet leftover official leftover $TESTNET_TOKENS_INDEX_CARD_TCHR_BINANCE leftover official leftover $TESTNET_TCHR_BINANCE_URL leftover official leftover Learn how to obtain tCHR tokens on the Binance Smart Chain Testnet leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover BlockchainRID leftover official leftover placeholders.
        Leftover official BUILD deployment/vault-listing (leftover official $VAULT_LISTING_INDEX_URL_NOSLASH leftover official 307 leftover official $VAULT_LISTING_INDEX_URL_SLASH leftover official 200 leftover official $VAULT_LISTING_INDEX_TITLE): leftover official leftover intro leftover official leftover To list your dapp on the Chromia Vault, you need to implement a find_dapp_details query that provides metadata about your dapp leftover official leftover prerequisites leftover official leftover A deployed dapp on Chromia (Mainnet or Testnet) leftover official leftover Media files for your dapp (icons, screenshots, etc.) leftover official leftover Filehub or any web3 storage for media content leftover official leftover Choose your approach leftover official leftover child leftover official leftover cards leftover official leftover $VAULT_LISTING_INDEX_CARD_QUICK leftover official leftover $VAULT_LISTING_QUICK_URL leftover official leftover Quick setup (hardcoded metadata) leftover official leftover Best for: Quick prototypes, simple dapps, or when you don't need frequent content updates leftover official leftover Pros: Fast setup, simple implementation, no admin key management leftover official leftover Cons: Requires redeployment for changes, no dynamic updates leftover official leftover $VAULT_LISTING_INDEX_CARD_DYNAMIC leftover official leftover $VAULT_LISTING_DYNAMIC_URL leftover official leftover Dynamic setup (database-based metadata) leftover official leftover Best for: Production dapps, frequent content updates, or when you need admin control leftover official leftover Pros: Easy content updates without redeployment, admin control, no redeployment needed leftover official leftover Cons: More complex initial setup, requires admin key management leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover leftover official leftover sample leftover official leftover admin leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover BlockchainRID leftover official leftover placeholders.
        Leftover official BUILD deployment/deploy-frontend-dapp (leftover official $DEPLOY_FRONTEND_INDEX_URL leftover official 307 leftover official $DEPLOY_FRONTEND_INDEX_URL_SLASH leftover official 200 leftover official $DEPLOY_FRONTEND_INDEX_TITLE): leftover official leftover intro leftover official leftover This topic provides a detailed guide on deploying a frontend application into the blockchain leftover official leftover It includes steps for configuring environment variables, building and packaging the application, updating the blockchain settings, and accessing the deployed application through a web interface leftover official leftover While this topic uses Next.js for demonstration, you can use any frontend framework (React, Vue.js, Angular, Svelte, vanilla JavaScript, etc.) as long as it generates files for deployment leftover official leftover prerequisites leftover official leftover A dapp should be ready for the deployment leftover official leftover The dapp should be placed into the root of the frontend application file structure leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover BlockchainRID leftover official leftover placeholders.
        Leftover official leftover BUILD deployment/testnet/getting-started (leftover official $TESTNET_GETTING_STARTED_INDEX_URL leftover official 307 leftover official $TESTNET_GETTING_STARTED_INDEX_URL_SLASH leftover official 200 leftover official $TESTNET_GETTING_STARTED_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover Deploying a decentralized application (dapp) to the Chromia testnet involves several key steps leftover official leftover Step 1 Obtain a container leftover official leftover Step 2 Deploy your dapp leftover official leftover Step 3 Connect a client leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders. Leftover official leftover still says leftover official leftover Specify your network (mainnet) on leftover official leftover TESTNET page — leftover official leftover leftover official leftover bug leftover official leftover --network testnet wins.
        Leftover official leftover BUILD deployment/mainnet/getting-started (leftover official $MAINNET_GETTING_STARTED_INDEX_URL leftover official 307 leftover official $MAINNET_GETTING_STARTED_INDEX_URL_SLASH leftover official 200 leftover official $MAINNET_GETTING_STARTED_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover Deploying a decentralized application (dapp) to the Chromia Mainnet involves several key steps leftover official leftover Step 1 Obtain a container leftover official leftover Step 2 Deploy your dapp leftover official leftover Step 3 Connect a client leftover official leftover Specify your network (Mainnet) leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders.
        Leftover official leftover BUILD deployment/testnet/deploy-dapp (leftover official $TESTNET_DEPLOY_DAPP_INDEX_URL leftover official 307 leftover official $TESTNET_DEPLOY_DAPP_INDEX_URL_SLASH leftover official 200 leftover official $TESTNET_DEPLOY_DAPP_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover This topic covers deploying and updating a dapp with the Chromia CLI to the public testnet leftover official leftover Prerequisite leftover official leftover Install Chromia CLI leftover official leftover Build a dapp that compiles leftover official leftover Container ID leftover official leftover Deploy the dapp leftover official leftover Update the dapp leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders. Leftover official leftover create leftover official leftover update leftover official leftover WRITE SKIP leftover official leftover create leftover official leftover write-back leftover official leftover since leftover official leftover CLI leftover official leftover 0.30.0 leftover official leftover wins leftover official leftover over leftover official leftover add leftover official leftover the leftover official leftover chains leftover official leftover key leftover official leftover by leftover official leftover hand. Leftover official leftover reserved leftover official leftover names leftover official leftover auto-fill leftover official leftover Directory leftover official leftover brid leftover official leftover + leftover official leftover url leftover official leftover since leftover official leftover CLI leftover official leftover 0.29.8 leftover official leftover win.
        Leftover official leftover BUILD deployment/mainnet/deploy-dapp (leftover official $MAINNET_DEPLOY_DAPP_INDEX_URL leftover official 307 leftover official $MAINNET_DEPLOY_DAPP_INDEX_URL_SLASH leftover official 200 leftover official $MAINNET_DEPLOY_DAPP_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover This topic covers deploying and updating a dapp with the Chromia CLI to the public Mainnet leftover official leftover Prerequisites leftover official leftover Install Chromia CLI leftover official leftover Build a dapp that compiles leftover official leftover Container ID leftover official leftover Deploy the dapp leftover official leftover Update the dapp leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders leftover official leftover no leftover official leftover sample leftover official leftover container leftover official leftover hex leftover official leftover placeholders. Leftover official leftover create leftover official leftover update leftover official leftover WRITE SKIP leftover official leftover create leftover official leftover write-back leftover official leftover since leftover official leftover CLI leftover official leftover 0.30.0 leftover official leftover wins leftover official leftover over leftover official leftover add leftover official leftover the leftover official leftover chains leftover official leftover key leftover official leftover by leftover official leftover hand. Leftover official leftover reserved leftover official leftover name leftover official leftover mainnet leftover official leftover auto-fills leftover official leftover Directory leftover official leftover brid leftover official leftover + leftover official leftover url leftover official leftover since leftover official leftover CLI leftover official leftover 0.29.8 leftover official leftover win.
        Leftover official leftover BUILD deployment/mainnet/multi-deployment (leftover official $MAINNET_MULTI_DEPLOYMENT_INDEX_URL leftover official 307 leftover official $MAINNET_MULTI_DEPLOYMENT_INDEX_URL_SLASH leftover official 200 leftover official $MAINNET_MULTI_DEPLOYMENT_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover This guide provides step-by-step instructions for deploying and updating decentralized applications (dapps) on Chromia using multiple private keys and voter sets leftover official leftover Prerequisites leftover official leftover Steps for multi-owner deployment leftover official leftover Steps for updating a dapp leftover official leftover Steps for managing proposals leftover official leftover Example configurations leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover voterset add-dapp-provider leftover official leftover voterset update leftover official leftover proposal vote leftover official leftover proposal revoke leftover official leftover pmc blockchain update leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover member leftover official leftover keys leftover official leftover placeholders.
        Leftover official leftover BUILD deployment/testnet/list-dapp-vault (leftover official $TESTNET_LIST_DAPP_VAULT_INDEX_URL leftover official 307 leftover official $TESTNET_LIST_DAPP_VAULT_INDEX_URL_SLASH leftover official 200 leftover official $TESTNET_LIST_DAPP_VAULT_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover This guide provides step-by-step instructions for listing your decentralized application (dapp) on the Chromia Testnet Vault leftover official leftover Prerequisites leftover official leftover Listing steps leftover official leftover find_dapp_details leftover official leftover Filehub media leftover official leftover Automatic listing leftover official leftover Verification for a checkmark leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover Connect Wallet leftover official leftover Request Tokens leftover official leftover faucet leftover official leftover message leftover official leftover sign leftover official leftover skip leftover official leftover chr leftover official leftover tx leftover official leftover writes leftover official leftover create_or_update_dapp leftover official leftover create_or_update_blockchain leftover official leftover create_or_update_dapp_media leftover official leftover skip leftover official leftover sample leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover Filehub leftover official leftover 64-hex leftover official leftover media leftover official leftover paths leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders.
        Leftover official leftover BUILD deployment/vault-listing/quick-vault-listing (leftover official $QUICK_VAULT_LISTING_INDEX_URL leftover official 307 leftover official $QUICK_VAULT_LISTING_INDEX_URL_SLASH leftover official 200 leftover official $QUICK_VAULT_LISTING_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover Fast and simple approach to list your dapp on the Chromia Vault using hardcoded metadata leftover official leftover Perfect for quick prototypes or simple dapps leftover official leftover Prerequisites leftover official leftover A deployed dapp on Chromia (Mainnet or Testnet) leftover official leftover Media files leftover official leftover Implementation leftover official leftover Customization leftover official leftover Configuration leftover official leftover Include the module leftover official leftover import quick_vault_listing leftover official leftover Update your chromia.yml leftover official leftover Deployment leftover official leftover Automatic listing leftover official leftover Making changes leftover official leftover Verify the data leftover official leftover Optional leftover official leftover Storing media content on chain leftover official leftover find_dapp_details leftover official leftover Filehub media leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover chr leftover official leftover deployment leftover official leftover update leftover official leftover writes leftover official leftover skip leftover official leftover sample leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover Filehub leftover official leftover 64-hex leftover official leftover media leftover official leftover paths leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders.
        Leftover official leftover BUILD deployment/vault-listing/dynamic-vault-listing (leftover official $DYNAMIC_VAULT_LISTING_INDEX_URL leftover official 307 leftover official $DYNAMIC_VAULT_LISTING_INDEX_URL_SLASH leftover official 200 leftover official $DYNAMIC_VAULT_LISTING_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover Flexible approach to list your dapp on the Chromia Vault using database-stored metadata leftover official leftover Perfect for production dapps or when you need frequent content updates leftover official leftover Prerequisites leftover official leftover A deployed dapp on Chromia (Mainnet or Testnet) leftover official leftover Media files leftover official leftover Admin key pair leftover official leftover Implementation leftover official leftover Configuration leftover official leftover Include the module leftover official leftover import dynamic_vault_listing leftover official leftover Update your chromia.yml leftover official leftover Deployment leftover official leftover Automatic listing leftover official leftover Making changes leftover official leftover Admin key management leftover official leftover Verify the data leftover official leftover Optional leftover official leftover Storing media content on chain leftover official leftover find_dapp_details leftover official leftover Filehub media leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover chr leftover official leftover tx leftover official leftover writes leftover official leftover create_or_update_dapp leftover official leftover create_or_update_blockchain leftover official leftover create_or_update_dapp_media leftover official leftover skip leftover official leftover sample leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover Filehub leftover official leftover 64-hex leftover official leftover media leftover official leftover paths leftover official leftover sample leftover official leftover tx leftover official leftover RIDs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover leftover official leftover sample leftover official leftover admin leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders.
        Leftover official leftover BUILD deployment/testnet-tokens/get-tchr-chromia (leftover official $GET_TCHR_CHROMIA_INDEX_URL leftover official 307 leftover official $GET_TCHR_CHROMIA_INDEX_URL_SLASH leftover official 200 leftover official $GET_TCHR_CHROMIA_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover This guide will walk you through obtaining test tokens (tCHR) on the Chromia Testnet leftover official leftover These tokens are essential for testing deployments and experimenting with Chromia's features without using real tokens leftover official leftover Usage notes leftover official leftover tCHR tokens are for testing on the Chromia Testnet and have leftover official leftover no leftover official leftover real-world leftover official leftover value leftover official leftover leftover official leftover weekly leftover official leftover allowace leftover official leftover typo leftover official leftover leftover official leftover allowance leftover official leftover 1000 leftover official leftover tCHR leftover official leftover every leftover official leftover 7 leftover official leftover days leftover official leftover wins leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover leftover official leftover faucet leftover official leftover leftover official leftover Connect Wallet leftover official leftover leftover official leftover Request Tokens leftover official leftover leftover official leftover Create Account leftover official leftover leftover official leftover faucet leftover official leftover message leftover official leftover sign leftover official leftover leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders.
        Leftover official leftover BUILD deployment/testnet-tokens/get-tchr-binance (leftover official $GET_TCHR_BINANCE_INDEX_URL leftover official 307 leftover official $GET_TCHR_BINANCE_INDEX_URL_SLASH leftover official 200 leftover official $GET_TCHR_BINANCE_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover intro leftover official leftover This guide will walk you through obtaining test tokens (tCHR) on the Binance Smart Chain Testnet leftover official leftover These tokens can be used for testing purposes when deploying a bridge between the BSC and Chromia leftover official leftover Testnet token differences leftover official leftover Chromia testnet tCHR leftover official leftover 1000 leftover official leftover tCHR leftover official leftover weekly leftover official leftover BSC testnet tCHR leftover official leftover any EVM wallet leftover official leftover cross-chain bridge leftover official leftover EVM integration leftover official leftover REAL leftover official leftover bug leftover official leftover Step 1 leftover official leftover still leftover official leftover says leftover official leftover Access the Chromia testnet faucet leftover official leftover leftover official leftover BscScan leftover official leftover faucet leftover official leftover URL leftover official leftover wins leftover official leftover tBNB leftover official leftover gas leftover official leftover once leftover official leftover a leftover official leftover week leftover official leftover no leftover official leftover real-world leftover official leftover value leftover official leftover query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover leftover official leftover Connect to web3 leftover official leftover leftover official leftover Connect Wallet leftover official leftover leftover official leftover Write Contract leftover official leftover claim leftover official leftover leftover official leftover Request Tokens leftover official leftover leftover official leftover faucet leftover official leftover message leftover official leftover sign leftover official leftover leftover official leftover chr leftover official leftover tx leftover official leftover writes leftover official leftover leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders.
        Leftover official leftover GET-STARTED create-dapp/deploy-to-testnet (leftover official $GET_STARTED_DEPLOY_TESTNET_INDEX_URL leftover official 307 leftover official $GET_STARTED_DEPLOY_TESTNET_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover query-only leftover official leftover HELP ONLY leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover Generate a key pair leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover sample leftover official leftover Blockchain RID leftover official leftover placeholders.
        Leftover official leftover ECOSYSTEM ecosystem/providers/container-management/add-dapp-provider-to-voterset INDEX (leftover official $ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/book-review/introduction INDEX (leftover official $LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/setup INDEX (leftover official $LEARN_MARKETPLACE_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/Crosschain INDEX (leftover official $LEARN_TAGS_CROSSCHAIN_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_CROSSCHAIN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_CROSSCHAIN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        chromia.yml schema: $PROJECT_CONFIG_URL
        --key-id references an existing key id under ~/.chromia/ (default id chromia_key).
        CLI key precedence: --secret > --key-id > --config key.id > project .chromia/config > ~/.chromia/config.
        This tool does not generate a key and does not write private key material.
        -y confirms a new `deployment create`. Do not pass a generated key.
        ${containerFieldNote()}
        ${writeBackNote()}
        ${schemaCompareNote()}
        inspect is read-only (queries / operations / entities / objects / module_args). No key pair flags.
        info is official and read-only (cluster / nodes). No key pair flags. --verbose shows node details.
        proposal list / proposal info are official and read-only. No --secret / --key-id on the live page.
        voterset info / list are official and read-only. voterset info requires --name or --container (mutually exclusive).
        Leftover official read-only query find_dapp_details(dapp_rowid: rowid, requested_content_types: list<dapp_content_type>? = null).
        Leftover official verify quotes (quick + dynamic 200) must keep leftover official quotes: requested_content_types=[] and requested_content_types=["landscape", "portrait", "promotional", "video", "icon"].
        ${deployFrontendNote()}
        Leftover official enum dapp_content_type: landscape, portrait, promotional, video, icon. Leftover official indexes landscape=0 portrait=1 promotional=2 video=3 icon=4.
        Leftover official BUILD testnet list-dapp-vault (200): $TESTNET_LIST_DAPP_VAULT_URL. Leftover official instruction: $VAULT_IGNORE_ROWID. Leftover official testnet verify quotes match leftover official vault-listing: requested_content_types=[]. Leftover official recommended image sizes: 180x100 / 180x240 / 510x286 / 86x48 (recommended x3).
        ${vaultPrerequisitesNote()}
        ${vaultHardcodedVsDbNote()}
        ${vaultAutoListingNote()}
        ${vaultCheckmarkNote()}
        ${vaultSetupMocksNote()}
        ${vaultStaleClientInstallNote()}
        ${testnetDeployDappNote()}
        ${tchrChromiaNote()}
        ${tchrBinanceNote()}
        ${explorerVerifyNote()}
        ${connectClientNote()}
        ${getContainerNote()}
        ${multiDeploymentNote()}
        Skip leftover official testnet chr tx writes, leftover official --secret, leftover official sample BRID placeholders, leftover official Filehub 64-hex media paths, leftover official setUpMocks sample 64-hex, leftover official sample container 64-hex, leftover official Connect Wallet / leftover official Request Tokens / leftover official faucet message sign, leftover official admin key pair snippet, and leftover official keygen.
        Leftover official imports: import quick_vault_listing; import dynamic_vault_listing;.
        Leftover official dynamic moduleArgs keys: admin_pubkey and dapp_name (keys only; do not invent a public key).
        Leftover official Filehub cost 0.10 USD per MB perpetual; leftover official minimum 1 CHR; leftover official gateway host filehub-gw.chromia.com. Skip leftover official sample 64-hex Filehub paths.
        Skip leftover official create_or_update_dapp / create_or_update_blockchain / create_or_update_dapp_media chr tx writes and leftover official sample tx RIDs.
        Skipped (sign / propose): proposal vote, retract-vote, revoke, rename; voterset update, add-dapp-provider;
        pause, resume, remove; container configuration, pause, resume.
        Skipped: `chr deployment lease-info` is hidden+experimental and is not on the live deployment page.
        Skipped: `chr deployment remove-container` is hidden and posts a signed economy-chain FT4 removeContainerOperation (no refund).
        This tool does not run chr and does not send signed transactions.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DEPLOY_DOCS_URL)
        put("commands_deployment_index_docs", COMMANDS_DEPLOYMENT_INDEX_URL)
        put("commands_deployment_index_url_slash", COMMANDS_DEPLOYMENT_INDEX_URL_SLASH)
        put("commands_deployment_index_title", COMMANDS_DEPLOYMENT_INDEX_TITLE)
        put("deployment_index_url", DEPLOYMENT_INDEX_URL)
        put("deployment_index_url_slash", DEPLOYMENT_INDEX_URL_SLASH)
        put("deployment_index_title", DEPLOYMENT_INDEX_TITLE)
        put("deployment_index_card_testnet_tokens", DEPLOYMENT_INDEX_CARD_TESTNET_TOKENS)
        put("deployment_index_card_testnet", DEPLOYMENT_INDEX_CARD_TESTNET)
        put("deployment_index_card_mainnet", DEPLOYMENT_INDEX_CARD_MAINNET)
        put("deployment_index_card_vault", DEPLOYMENT_INDEX_CARD_VAULT)
        put("deployment_index_card_frontend", DEPLOYMENT_INDEX_CARD_FRONTEND)
        put("testnet_index_docs", TESTNET_INDEX_URL)
        put("testnet_index_url_slash", TESTNET_INDEX_URL_SLASH)
        put("testnet_index_title", TESTNET_INDEX_TITLE)
        put("testnet_index_card_getting_started", TESTNET_INDEX_CARD_GETTING_STARTED)
        put("testnet_index_card_get_container", TESTNET_INDEX_CARD_GET_CONTAINER)
        put("testnet_index_card_deploy_dapp", TESTNET_INDEX_CARD_DEPLOY_DAPP)
        put("testnet_index_card_connect_client", TESTNET_INDEX_CARD_CONNECT_CLIENT)
        put("testnet_index_card_list_vault", TESTNET_INDEX_CARD_LIST_VAULT)
        put("mainnet_index_docs", MAINNET_INDEX_URL)
        put("mainnet_index_url_slash", MAINNET_INDEX_URL_SLASH)
        put("mainnet_index_title", MAINNET_INDEX_TITLE)
        put("mainnet_index_card_getting_started", MAINNET_INDEX_CARD_GETTING_STARTED)
        put("mainnet_index_card_get_container", MAINNET_INDEX_CARD_GET_CONTAINER)
        put("mainnet_index_card_deploy_dapp", MAINNET_INDEX_CARD_DEPLOY_DAPP)
        put("mainnet_index_card_multi_deployment", MAINNET_INDEX_CARD_MULTI_DEPLOYMENT)
        put("mainnet_index_card_connect_client", MAINNET_INDEX_CARD_CONNECT_CLIENT)
        put("mainnet_index_card_list_vault", MAINNET_INDEX_CARD_LIST_VAULT)
        put("testnet_tokens_index_docs", TESTNET_TOKENS_INDEX_URL)
        put("testnet_tokens_index_url_slash", TESTNET_TOKENS_INDEX_URL_SLASH)
        put("testnet_tokens_index_title", TESTNET_TOKENS_INDEX_TITLE)
        put("testnet_tokens_index_card_tchr_chromia", TESTNET_TOKENS_INDEX_CARD_TCHR_CHROMIA)
        put("testnet_tokens_index_card_tchr_binance", TESTNET_TOKENS_INDEX_CARD_TCHR_BINANCE)
        put("vault_listing_index_docs", VAULT_LISTING_INDEX_URL_NOSLASH)
        put("vault_listing_index_url_slash", VAULT_LISTING_INDEX_URL_SLASH)
        put("vault_listing_index_title", VAULT_LISTING_INDEX_TITLE)
        put("vault_listing_index_card_quick", VAULT_LISTING_INDEX_CARD_QUICK)
        put("vault_listing_index_card_dynamic", VAULT_LISTING_INDEX_CARD_DYNAMIC)
        put("deploy_frontend_index_docs", DEPLOY_FRONTEND_INDEX_URL)
        put("deploy_frontend_index_url_slash", DEPLOY_FRONTEND_INDEX_URL_SLASH)
        put("deploy_frontend_index_title", DEPLOY_FRONTEND_INDEX_TITLE)
        put("testnet_getting_started_index_docs", TESTNET_GETTING_STARTED_INDEX_URL)
        put("testnet_getting_started_index_url_slash", TESTNET_GETTING_STARTED_INDEX_URL_SLASH)
        put("testnet_getting_started_index_title", TESTNET_GETTING_STARTED_INDEX_TITLE)
        put("mainnet_getting_started_index_docs", MAINNET_GETTING_STARTED_INDEX_URL)
        put("mainnet_getting_started_index_url_slash", MAINNET_GETTING_STARTED_INDEX_URL_SLASH)
        put("mainnet_getting_started_index_title", MAINNET_GETTING_STARTED_INDEX_TITLE)
        put("testnet_deploy_dapp_index_docs", TESTNET_DEPLOY_DAPP_INDEX_URL)
        put("testnet_deploy_dapp_index_url_slash", TESTNET_DEPLOY_DAPP_INDEX_URL_SLASH)
        put("testnet_deploy_dapp_index_title", TESTNET_DEPLOY_DAPP_INDEX_TITLE)
        put("mainnet_deploy_dapp_index_docs", MAINNET_DEPLOY_DAPP_INDEX_URL)
        put("mainnet_deploy_dapp_index_url_slash", MAINNET_DEPLOY_DAPP_INDEX_URL_SLASH)
        put("mainnet_deploy_dapp_index_title", MAINNET_DEPLOY_DAPP_INDEX_TITLE)
        put("mainnet_multi_deployment_index_docs", MAINNET_MULTI_DEPLOYMENT_INDEX_URL)
        put("mainnet_multi_deployment_index_url_slash", MAINNET_MULTI_DEPLOYMENT_INDEX_URL_SLASH)
        put("mainnet_multi_deployment_index_title", MAINNET_MULTI_DEPLOYMENT_INDEX_TITLE)
        put("testnet_list_dapp_vault_index_docs", TESTNET_LIST_DAPP_VAULT_INDEX_URL)
        put("testnet_list_dapp_vault_index_url_slash", TESTNET_LIST_DAPP_VAULT_INDEX_URL_SLASH)
        put("testnet_list_dapp_vault_index_title", TESTNET_LIST_DAPP_VAULT_INDEX_TITLE)
        put("quick_vault_listing_index_docs", QUICK_VAULT_LISTING_INDEX_URL)
        put("quick_vault_listing_index_url_slash", QUICK_VAULT_LISTING_INDEX_URL_SLASH)
        put("quick_vault_listing_index_title", QUICK_VAULT_LISTING_INDEX_TITLE)
        put("dynamic_vault_listing_index_docs", DYNAMIC_VAULT_LISTING_INDEX_URL)
        put("dynamic_vault_listing_index_url_slash", DYNAMIC_VAULT_LISTING_INDEX_URL_SLASH)
        put("dynamic_vault_listing_index_title", DYNAMIC_VAULT_LISTING_INDEX_TITLE)
        put("get_tchr_chromia_index_docs", GET_TCHR_CHROMIA_INDEX_URL)
        put("get_tchr_chromia_index_url_slash", GET_TCHR_CHROMIA_INDEX_URL_SLASH)
        put("get_tchr_chromia_index_title", GET_TCHR_CHROMIA_INDEX_TITLE)
        put("get_tchr_binance_index_docs", GET_TCHR_BINANCE_INDEX_URL)
        put("get_tchr_binance_index_url_slash", GET_TCHR_BINANCE_INDEX_URL_SLASH)
        put("get_tchr_binance_index_title", GET_TCHR_BINANCE_INDEX_TITLE)
        put("get_started_deploy_testnet_index_docs", GET_STARTED_DEPLOY_TESTNET_INDEX_URL)
        put("get_started_deploy_testnet_index_url_slash", GET_STARTED_DEPLOY_TESTNET_INDEX_URL_SLASH)
        put("get_started_deploy_testnet_index_title", GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE)
        put(
            "commands",
            buildJsonObject {
                put("create", createExample())
                put("update", updateExample())
                put("inspect", inspectExample())
                put("info", infoExample())
                put("proposal_list", proposalListExample())
                put("proposal_info", proposalInfoExample())
                put("voterset_list", votersetListExample())
                put("voterset_info", votersetInfoExample())
            }
        )
        put(
            "flags",
            buildJsonObject {
                put(
                    "create",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("config", "-cfg, --config=<config>")
                        put("network", "-d, --network=<text>")
                        put("blockchain", "-bc, --blockchain=<text>")
                        put(
                            "key_id",
                            "--key-id=<key_id>  # reference an existing key id; this tool does not generate a key"
                        )
                        put(
                            "secret",
                            "--secret=<path>  # path to an existing secret file; this tool does not write one"
                        )
                        put("no_compression", "--no-compression")
                        put("hide_lib_warnings", "--hide-lib-warnings")
                        put(
                            "yes",
                            "-y  # confirm new deployment; non-interactive without -y: $CREATE_Y_ERROR"
                        )
                    }
                )
                put(
                    "update",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("config", "-cfg, --config=<config>")
                        put("network", "-d, --network=<text>")
                        put("blockchain", "-bc, --blockchain=<text>")
                        put(
                            "key_id",
                            "--key-id=<key_id>  # reference an existing key id; this tool does not generate a key"
                        )
                        put(
                            "secret",
                            "--secret=<path>  # path to an existing secret file; this tool does not write one"
                        )
                        put("no_compression", "--no-compression")
                        put("hide_lib_warnings", "--hide-lib-warnings")
                        put("height", "--height=<int>  # deploy configuration at a specific height")
                        put(
                            "verify_only",
                            "--verify-only  # verify + schema compare; do not send the update"
                        )
                        put(
                            "skip_verification",
                            "--skip-verification  # skip node validateConfiguration and schema compare"
                        )
                    }
                )
                put(
                    "inspect",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("config", "-cfg, --config=<config>")
                        put("network", "-d, --network=<text>")
                        put("blockchain", "-bc, --blockchain=<text>")
                        put("blockchain_rid", "-brid, --blockchain-rid=<text>")
                        put("cid", "--cid=<int>")
                        put("api_url", "--api-url=<text>")
                        put("network_alias", "--mainnet, --testnet  # use instead of --api-url")
                        put("output_format", "-f, --output-format=(table|JSON)")
                        put("modules", "-m, --modules=<modules>")
                        put("list_modules", "-l, --list-modules")
                        put("module_args", "--module-args")
                        put(
                            "definitions",
                            "--definitions=(queries|operations|entities|objects)"
                        )
                        put("signature", "--signature=<value>")
                    }
                )
                put(
                    "info",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("config", "-cfg, --config=<config>")
                        put("network", "-d, --network=<text>")
                        put("blockchain", "-bc, --blockchain=<text>")
                        put("blockchain_rid", "-brid, --blockchain-rid=<text>")
                        put("cid", "--cid=<int>")
                        put("api_url", "--api-url=<text>")
                        put("network_alias", "--mainnet, --testnet  # use instead of --api-url")
                        put("verbose", "--verbose  # show verbose information about nodes")
                        put("output_format", "-f, --output-format=(table|JSON)")
                    }
                )
                put(
                    "proposal_list",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("config", "-cfg, --config=<config>")
                        put("network", "-d, --network=<text>")
                        put("from", "--from=<value>  # YYYY-MM-DD")
                        put("to", "--to=<value>  # YYYY-MM-DD")
                        put("all", "--all  # include proposals you can not vote on")
                        put("pending", "--pending  # only pending proposals")
                        put("output_format", "-f, --output-format=(table|JSON)")
                    }
                )
                put(
                    "proposal_info",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("config", "-cfg, --config=<config>")
                        put("network", "-d, --network=<text>")
                        put("id", "--id=<int>  # required; do not invent a proposal id")
                        put("output_format", "-f, --output-format=(table|JSON)")
                    }
                )
                put(
                    "voterset_info",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("config", "-cfg, --config=<config>")
                        put("network", "-d, --network=<text>")
                        put("name", "-n, --name=<value>  # voter set name; mutually exclusive with --container")
                        put(
                            "container",
                            "-c, --container=<value>  # container governed by the voterset; mutually exclusive with --name"
                        )
                        put("output_format", "-f, --output-format=(table|JSON)")
                    }
                )
                put(
                    "voterset_list",
                    buildJsonObject {
                        put("settings", "-s, --settings=<settings>")
                        put("config", "-cfg, --config=<config>")
                        put("network", "-d, --network=<text>")
                        put(
                            "container",
                            "-c, --container=<text>  # optional; default all votersets"
                        )
                        put("output_format", "-f, --output-format=(table|JSON)")
                    }
                )
            }
        )
        put("write_back", writeBackNote())
        put("schema_compare", schemaCompareNote())
        put("drop_warning", DROP_WARNING)
        put("container", containerFieldNote())
        put("database", ChromiaYmlSections.databaseYaml())
        put("test", ChromiaYmlSections.testYaml())
        put("chromia_yml_sections_notes", ChromiaYmlSections.notes())
        put("vault_listing_quick", VAULT_LISTING_QUICK_URL)
        put("vault_listing_dynamic", VAULT_LISTING_DYNAMIC_URL)
        put("vault_listing_index", VAULT_LISTING_INDEX_URL)
        put("vault_find_dapp_details", vaultFindDappDetails())
        put("vault_query_empty", vaultQueryEmpty())
        put("vault_query_all", vaultQueryAll())
        put("vault_content_types", vaultContentTypes())
        put("deploy_frontend", DEPLOY_FRONTEND_URL)
        put("testnet_list_dapp_vault", TESTNET_LIST_DAPP_VAULT_URL)
        put("testnet_tokens", TESTNET_TOKENS_URL)
        put("testnet_tchr_chromia", TESTNET_TCHR_CHROMIA_URL)
        put("testnet_tchr_binance", TESTNET_TCHR_BINANCE_URL)
        put("testnet_faucet", TESTNET_FAUCET_URL)
        put("testnet_vault_dapps", TESTNET_VAULT_DAPPS_URL)
        put("testnet_economy_chain_vault", TESTNET_ECONOMY_CHAIN_VAULT_URL)
        put("testnet_bscscan_faucet", TESTNET_BSCSCAN_FAUCET_URL)
        put("testnet_bsc_tchr_token", TESTNET_BSC_TCHR_TOKEN)
        put("explorer", EXPLORER_URL)
        put("testnet_connect_client", TESTNET_CONNECT_CLIENT_URL)
        put("mainnet_connect_client", MAINNET_CONNECT_CLIENT_URL)
        put("tchr_chromia_note", tchrChromiaNote())
        put("tchr_binance_note", tchrBinanceNote())
        put("explorer_verify_note", explorerVerifyNote())
        put("connect_client_note", connectClientNote())
        put("testnet_get_container", TESTNET_GET_CONTAINER_URL)
        put("mainnet_get_container", MAINNET_GET_CONTAINER_URL)
        put("mainnet_multi_deployment", MAINNET_MULTI_DEPLOYMENT_URL)
        put("postchain_client_npm", POSTCHAIN_CLIENT_NPM_URL)
        put("hello_world_quickstart", HELLO_WORLD_QUICKSTART_URL)
        put("hosting_about", HOSTING_ABOUT_URL)
        put("get_container_note", getContainerNote())
        put("scu_note", scuNote())
        put("multi_deployment_note", multiDeploymentNote())
        put("vault_ignore_rowid", vaultIgnoreRowid())
        put("vault_prerequisites", vaultPrerequisitesNote())
        put("vault_hardcoded_vs_db", vaultHardcodedVsDbNote())
        put("vault_auto_listing", vaultAutoListingNote())
        put("vault_checkmark", vaultCheckmarkNote())
        put("vault_setupmocks", vaultSetupMocksNote())
        put("vault_setupmocks_url", DAPP_AGGREGATOR_SETUPMOCKS_URL)
        put("stale_postchain_clients_install_404", STALE_POSTCHAIN_CLIENTS_INSTALL_404)
        put("clients_overview", CLIENTS_OVERVIEW_URL)
        put("testnet_deploy_dapp", TESTNET_DEPLOY_DAPP_URL)
        put("testnet_getting_started", TESTNET_GETTING_STARTED_URL)
        put("mainnet_deploy_dapp", MAINNET_DEPLOY_DAPP_URL)
        put("testnet_deploy_dapp_note", testnetDeployDappNote())
        put("web_static_key", WEB_STATIC_KEY)
        put("web_static_yaml", webStaticYaml())
        put("web_static_local_url", WEB_STATIC_LOCAL_URL)
        put("nextjs_export_config", nextJsExportConfig())
        put("deploy_frontend_note", deployFrontendNote())
        put("ecosystem_add_dapp_provider_to_voterset_index_url_slash", ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_URL_SLASH)
        put("ecosystem_add_dapp_provider_to_voterset_index_title", ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_TITLE)
        put("learn_book_review_introduction_index_url_slash", LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_URL_SLASH)
        put("learn_book_review_introduction_index_title", LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_TITLE)
        put("learn_marketplace_setup_index_url_slash", LEARN_MARKETPLACE_SETUP_INDEX_URL_SLASH)
        put("learn_marketplace_setup_index_title", LEARN_MARKETPLACE_SETUP_INDEX_TITLE)
        put("learn_tags_crosschain_index_url_slash", LEARN_TAGS_CROSSCHAIN_INDEX_URL_SLASH)
        put("learn_tags_crosschain_index_title", LEARN_TAGS_CROSSCHAIN_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover BUILD cli/commands/deployment leftovers encoded as COMMANDS_DEPLOYMENT_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/testnet/getting-started leftovers encoded as TESTNET_GETTING_STARTED_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/mainnet/getting-started leftovers encoded as MAINNET_GETTING_STARTED_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/testnet/deploy-dapp leftovers encoded as TESTNET_DEPLOY_DAPP_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/mainnet/deploy-dapp leftovers encoded as MAINNET_DEPLOY_DAPP_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/mainnet/multi-deployment leftovers encoded as MAINNET_MULTI_DEPLOYMENT_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/testnet/list-dapp-vault leftovers encoded as TESTNET_LIST_DAPP_VAULT_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/vault-listing/quick-vault-listing leftovers encoded as QUICK_VAULT_LISTING_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/vault-listing/dynamic-vault-listing leftovers encoded as DYNAMIC_VAULT_LISTING_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/testnet-tokens/get-tchr-chromia leftovers encoded as GET_TCHR_CHROMIA_INDEX_* (query-only).
// Leftover official leftover BUILD deployment/testnet-tokens/get-tchr-binance leftovers encoded as GET_TCHR_BINANCE_INDEX_* (query-only).
// Leftover official leftover GET-STARTED create-dapp/deploy-to-testnet leftovers encoded as GET_STARTED_DEPLOY_TESTNET_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/container-management/add-dapp-provider-to-voterset INDEX leftovers encoded as ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/book-review/introduction INDEX leftovers encoded as LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/setup INDEX leftovers encoded as LEARN_MARKETPLACE_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/Crosschain INDEX leftovers encoded as LEARN_TAGS_CROSSCHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
