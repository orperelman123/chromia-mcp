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
 * Skipped (hidden): lease-info, remove-container. Official BUILD deployment index slash/title/child-card leftovers live here (query-only). Official BUILD vault-listing read-only find_dapp_details; skip chr tx writes and sample 64-hex. Official get-tchr-binance READ-ONLY faucet/allowance; deploy-dapp explorer verify; connect-client started. Official BUILD deployment testnet index slash/title/child-card leftovers live here (query-only). Official BUILD deployment mainnet index slash/title/child-card leftovers live here (query-only). Official BUILD deployment testnet-tokens index slash/title/child-card leftovers live here (query-only). Official BUILD deployment vault-listing index slash/title/child-card leftovers live here (query-only). Official BUILD deployment deploy-frontend-dapp index slash/title leftovers live here (query-only). Official BUILD deployment testnet/getting-started index slash/title leftovers live here (query-only). Official BUILD deployment mainnet/getting-started index slash/title leftovers live here (query-only). Official BUILD deployment testnet/deploy-dapp index slash/title leftovers live here (query-only). Official BUILD deployment mainnet/deploy-dapp index slash/title leftovers live here (query-only). Official BUILD deployment testnet/list-dapp-vault index slash/title leftovers live here (query-only). Official BUILD deployment vault-listing/quick-vault-listing index slash/title leftovers live here (query-only). Official BUILD deployment vault-listing/dynamic-vault-listing index slash/title leftovers live here (query-only). Official BUILD deployment testnet-tokens/get-tchr-chromia index slash/title leftovers live here (query-only). Official BUILD deployment testnet-tokens/get-tchr-binance index slash/title leftovers live here (query-only). Official GET-STARTED create-dapp/deploy-to-testnet index slash/title leftovers live here (query-only).
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
        "Official BUILD testnet list-dapp-vault (200): $TESTNET_LIST_DAPP_VAULT_URL. Official prerequisites: a deployed dapp on the Chromia Testnet; access to your dapp's codebase; media files; tCHR tokens; Filehub or any comfortable web3 storage. Official tCHR index (200): $TESTNET_TOKENS_URL. Official get-tchr-chromia (200): $TESTNET_TCHR_CHROMIA_URL. Official get-tchr-binance (200): $TESTNET_TCHR_BINANCE_URL. Official Chromia Testnet Faucet: $TESTNET_FAUCET_URL. Official allowance 1000 tCHR every 7 days. Official get-tchr-chromia still prints weekly allowace (typo); allowance wins. Official tCHR has no real-world value. Official Testnet Economy Chain vault listing: $TESTNET_VAULT_DAPPS_URL. Official Testnet Economy Chain vault: $TESTNET_ECONOMY_CHAIN_VAULT_URL. Skip Connect Wallet / Request Tokens / faucet message sign."

    fun vaultHardcodedVsDbNote(): String = """
        Official BUILD testnet list-dapp-vault (200) hardcoded vs db (READ-ONLY names only).
        Official hardcoded: enum dapp_content_type; struct dapp_media (name, url, type); query find_dapp_details; function get_dapp_media; return fields rowid, name, description, launch_url, genre, chain_list (name, brid, role), content; .to_gtv_pretty(). Official get_dapp_media returns null when requested_content_types is empty.
        Official testnet hardcoded snippet still uses map<dapp_content_type, dapp_media> (one media per type). Official vault-listing/quick-vault-listing (200) hardcoded uses ec_media_tuple list<(dapp_content_type, dapp_media)> and multiple promotional images — vault-listing wins over testnet map snippet.
        Official db: struct module_args { admin_pubkey: byte_array }; require_admin_signer; chromia.yml moduleArgs keys admin_pubkey and dapp_name; entity dapp (key name; mutable description, launch_url, genre); entity dapp_media (key dapp, name; mutable url; type); entity blockchain (key dapp, brid; index brid; index mutable name; mutable role); operations create_or_update_dapp / create_or_update_blockchain / create_or_update_dapp_media (names only); helpers get_dapp_by_name, map_dapp_details, find_and_map_dapp_blockchains, find_and_map_dapp_media; @omit @sort .rowid.
        Official testnet page still inlines those snippets and bitbucket demo-vault-listing vault_list_hardcode / vault_list_db. Official vault-listing (200) uses import quick_vault_listing; import dynamic_vault_listing; and vault-listing libraries win for current listing.
    """.trimIndent()

    fun vaultAutoListingNote(): String =
        "Official BUILD testnet list-dapp-vault (200) automatic listing: once you implement the query and functions, your dapp will be automatically listed in the Chromia Testnet Vault."

    fun vaultCheckmarkNote(): String =
        "Official BUILD testnet list-dapp-vault (200) verification for a checkmark (optional): Contact the Chromia team admin for final verification. The admin will review your dapp to ensure it meets the required quality standards."

    fun vaultSetupMocksNote(): String =
        "Official BUILD testnet list-dapp-vault (200) example storing the data: setUpMocks.ts with Filehub media links. Official GitLab (200): $DAPP_AGGREGATOR_SETUPMOCKS_URL. Official source names: seedChain, seedImages, dapp_content_type. Skip sample 64-hex BRIDs and Filehub 64-hex media paths."

    fun vaultStaleClientInstallNote(): String =
        "Official BUILD testnet list-dapp-vault still links $STALE_POSTCHAIN_CLIENTS_INSTALL_404 (404). Official /build/clients/overview 307→200 client list wins: $CLIENTS_OVERVIEW_URL. Skip chr tx docs writes."


    fun tchrChromiaNote(): String = """
        Official BUILD get-tchr-chromia (200): $TESTNET_TCHR_CHROMIA_URL. Official Chromia Testnet Faucet: $TESTNET_FAUCET_URL. Official allowance 1000 tCHR every 7 days. Official get-tchr-chromia still prints weekly allowace (typo); allowance wins. Official tCHR has no real-world value. Official tokens are essential for testing deployments without real tokens. Official Testnet Economy Chain vault: $TESTNET_ECONOMY_CHAIN_VAULT_URL. Official Testnet Vault dapps: $TESTNET_VAULT_DAPPS_URL. Skip Connect Wallet / Request Tokens / Create Account / faucet message sign.
    """.trimIndent()

    fun tchrBinanceNote(): String = """
        Official BUILD get-tchr-binance (200): $TESTNET_TCHR_BINANCE_URL. Official Testnet token differences: Chromia testnet tCHR requires Economy Chain account through Chromia testnet faucet and 1000 tCHR weekly. Official BSC testnet tCHR can be claimed to any EVM wallet address without special account setup; primarily for cross-chain bridge testing and EVM integration. Official both token types serve same testing purpose with different acquisition methods.
        Official BscScan faucet (READ-ONLY URL): $TESTNET_BSCSCAN_FAUCET_URL. Official BSC tCHR token contract (READ-ONLY identifier): $TESTNET_BSC_TCHR_TOKEN. Official BSC faucet once a week. Official claim needs tBNB gas. Official tCHR has no real-world value.
        REAL bug: get-tchr-binance Step 1 still says Access the Chromia testnet faucet and usage notes still say testing on the Chromia Testnet. Official Testnet token differences + BscScan faucet URL win over Chromia Testnet title/usage notes. Official Chromia Testnet faucet remains $TESTNET_FAUCET_URL (get-tchr-chromia 200).
        Skip Connect to web3 / Connect Wallet / Write Contract claim / Request Tokens / message sign.
    """.trimIndent()

    fun explorerVerifyNote(): String = """
        Official BUILD testnet deploy-dapp (200) explorer verify (READ-ONLY): Chromia Explorer $EXPLORER_URL. Official set Current network to Testnet. Official Under Clusters, select system. Official copy API URL from bottom of the page (example host ${WriteDeploymentConfig.TESTNET_URL}) as url. Official Under System Chains, directory_chain and copy brid.
        Official testnet deploy-dapp still pastes Directory brid + url after explorer copy — reserved names mainnet / testnet auto-fill Directory brid + url since CLI 0.29.8 win. Official explorer verify is READ-ONLY confirmation, not required yaml for reserved names.
        Official BUILD mainnet deploy-dapp (200) has no explorer copy steps; reserved name mainnet auto-configures brid and url. Official Chromia Explorer host remains $EXPLORER_URL. Official mainnet explorer node-pool steps live on connect-client (200): $MAINNET_CONNECT_CLIENT_URL. Official testnet connect-client (200) also explorer + five minutes: $TESTNET_CONNECT_CLIENT_URL.
        Skip sample BRIDs / tx RIDs / sample container 64-hex.
    """.trimIndent()

    fun connectClientNote(): String = """
        Official BUILD testnet connect-client (200): $TESTNET_CONNECT_CLIENT_URL. Official TypeScript postchain-client. Official client needs directoryNodeUrlPool + dapp Blockchain RID. Official client automatically queries Directory chain on system nodes. Official testnet explorer verify: $EXPLORER_URL Current network Testnet; deployed dapp about five minutes. Official testnet directoryNodeUrlPool hosts: ${WriteDeploymentConfig.TESTNET_URLS.joinToString(", ")}. Official createClient (testnet snippet).
        Official BUILD mainnet connect-client (200): $MAINNET_CONNECT_CLIENT_URL. Official Client Libraries Overview $CLIENTS_OVERVIEW_URL. Official explorer: $EXPLORER_URL view list of nodes in system cluster. Official mainnet directoryNodeUrlPool snapshot hosts (connect-client, not required yaml): ${WriteDeploymentConfig.MAINNET_URLS.joinToString(", ")}, ${WriteDeploymentConfig.MAINNET_EXPLORER_SNAPSHOT_URLS.joinToString(", ")}.
        REAL bug: mainnet connect-client still prints pcl.creatClient (missing e). Official testnet connect-client pcl.createClient wins.
        Official TypeScript postchain-client $POSTCHAIN_CLIENT_NPM_URL. Official principles same with other client libraries. Official Hello World Quickstart $HELLO_WORLD_QUICKSTART_URL. Official remove private keys from production client code (no printed keys). Official testnet page says Directory chain; mainnet page says Directory Chain. Official testnet connect-client explorer + five minutes; mainnet connect-client explorer system cluster node list.
        Skip admin key pair snippet / sample BlockchainRID placeholders.
    """.trimIndent()

    fun scuNote(): String =
        "Official GET-STARTED hosting (200): $HOSTING_ABOUT_URL. Official one SCU: 2 GB RAM, 0.5 vCPU, 16 GB storage, 25 MiB/s read and 20 MiB/s write. Official weekly target approximately 90 USD default 7-node dapp cluster. Official lease weekly pay in CHR USD-equivalent target. Official cost number of SCUs plus additional storage. Official anyone can pay a lease. Official overdue suspended six months then permanently deleted. Do not invent other SCU sizes."

    fun getContainerNote(): String = """
        Official BUILD testnet get-container (200): $TESTNET_GET_CONTAINER_URL. Official tCHR required to lease a container. Official Testnet Vault containers: ${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS}. Official lease: cluster + SCUs / storage / duration + optional auto-renewal. Official Container ID is essential for deploy; retrieve later on the same Vault page. Official get-container links $HOSTING_ABOUT_URL (200) for SCUs / storage / duration. ${scuNote()} Official testnet container key is separate from wallet used to receive tCHR — reference an existing key id only.
        Official BUILD mainnet get-container (200): $MAINNET_GET_CONTAINER_URL. Official pay native CHR. Official deposit at least 10 CHR from BNB Smart Chain or Ethereum Mainnet via ${VaultLeaseHelp.MAINNET_VAULT_DEPOSIT}. Official Chromia account created automatically during deposit. Official Mainnet Vault containers: ${VaultLeaseHelp.MAINNET_VAULT_CONTAINERS}. Official failed lease refund same amount. Official chromia.yml prerequisite.
        Skip Generate a key pair / keygen / key-pair-management / Connect wallet / Lease sign / sample container 64-hex.
    """.trimIndent()

    fun multiDeploymentNote(): String = """
        Official BUILD mainnet multi-deployment (200): $MAINNET_MULTI_DEPLOYMENT_URL. Official READ-ONLY chr deployment voterset list and chr deployment proposal list already official on this tool. Official threshold is a fraction of total members.
        Skip voterset add-dapp-provider / voterset update / proposal vote / proposal revoke / pmc blockchain update / sample 64-hex member keys.
    """.trimIndent()

    fun testnetDeployDappNote(): String = """
        Official BUILD testnet deploy-dapp (200): $TESTNET_DEPLOY_DAPP_URL. Official testnet url host: ${WriteDeploymentConfig.TESTNET_URL}. Official first create: chr deployment create --settings chromia.yml --network testnet --blockchain hello. Official update: chr deployment update --settings chromia.yml --network testnet --blockchain hello. Official optional --config <config file path> (update snippet still prints --config config <config file path> — garbled; --config <path> wins). Official create writes deployments.testnet.chains back since CLI 0.30.0; deploy-dapp still says add the chains key by hand after the successful-deployment prompt — stale; source + CHANGELOG 0.30.0 win. Official update requires chains and does not rewrite chromia.yml. Official deployed dapp will be accessible in about five minutes. Official ~/.chromia/config key.id references an existing key id; this tool does not generate a key. Skip sample container 64-hex and Generate a key pair.
        Official BUILD testnet getting-started (200): $TESTNET_GETTING_STARTED_URL. Official getting-started still says Specify your network (mainnet) on TESTNET page — --network testnet wins.
        Official BUILD mainnet deploy-dapp (200): $MAINNET_DEPLOY_DAPP_URL. Official reserved name mainnet auto-configures brid and url. Official testnet deploy-dapp still pastes Directory brid + url by hand — reserved names mainnet / testnet auto-fill Directory brid + url since CLI 0.29.8 win. Official mainnet pre-deploy checklist: deployments.mainnet.brid, url, container, and chains empty for first create. Official mainnet also still says add the chains key by hand after successful-deployment prompt — stale; create write-back since CLI 0.30.0 wins. Official first create: chr deployment create --settings chromia.yml --network mainnet --blockchain hello. Official update: chr deployment update --settings chromia.yml --network mainnet --blockchain hello. Skip sample container 64-hex.
        ${explorerVerifyNote()}
    """.trimIndent()

    fun webStaticYaml(): String =
        "blockchains:\n  asset_management:\n    webStatic: out\n"

    fun nextJsExportConfig(): String =
        "const nextConfig = { output: \"export\", images: { unoptimized: true }, basePath: /web_query/\${process.env.NEXT_PUBLIC_BRID}/web_static };\n"

    fun deployFrontendNote(): String =
        "Official BUILD deploy-frontend-dapp (200): $DEPLOY_FRONTEND_URL. Official on-chain frontend: add $WEB_STATIC_KEY: $WEB_STATIC_EXAMPLE_VALUE under blockchains.<name> in rell/chromia.yml. Official project-config does not list $WEB_STATIC_KEY; deploy-frontend and CLI FetchConfigCommand do. Accept as key. Do not invent siblings. Official local URL (only printed host): $WEB_STATIC_LOCAL_URL. Official Next.js demo: output export; images.unoptimized true; basePath uses NEXT_PUBLIC_BRID. Official .env key NEXT_PUBLIC_BRID = blockchainRid from chr node start --wipe logs. Official apply: chr node update. Official page allows any frontend that generates files."

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
        Official BUILD cli/commands/deployment ($COMMANDS_DEPLOYMENT_INDEX_URL 307 $COMMANDS_DEPLOYMENT_INDEX_URL_SLASH 200 $COMMANDS_DEPLOYMENT_INDEX_TITLE): intro Usage chr deployment create update WRITE SKIP inspect info proposal list info voterset info list Query-only skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no keygen do not invent flags do not document chr tx signed send keygen samples.
        Official BUILD deployment ($DEPLOYMENT_INDEX_URL 307 $DEPLOYMENT_INDEX_URL_SLASH 200 $DEPLOYMENT_INDEX_TITLE): child cards $DEPLOYMENT_INDEX_CARD_TESTNET_TOKENS $DEPLOYMENT_INDEX_CARD_TESTNET $DEPLOYMENT_INDEX_CARD_MAINNET $DEPLOYMENT_INDEX_CARD_VAULT $DEPLOYMENT_INDEX_CARD_FRONTEND skip signed txs no sample keys no invented 64-hex.
        Official BUILD deployment/testnet ($TESTNET_INDEX_URL 307 $TESTNET_INDEX_URL_SLASH 200 $TESTNET_INDEX_TITLE): intro This section covers information about configuring your client and deploying and updating your dapp to the public testnet child cards $TESTNET_INDEX_CARD_GETTING_STARTED $TESTNET_GETTING_STARTED_URL Deploy your dapp to Chromia testnet in three steps: obtain a container, deploy your dapp, and connect a client $TESTNET_INDEX_CARD_GET_CONTAINER $TESTNET_GET_CONTAINER_URL Generate a key pair with Chromia CLI and lease a container using tCHR tokens to obtain a Container ID for deployment $TESTNET_INDEX_CARD_DEPLOY_DAPP $TESTNET_DEPLOY_DAPP_URL Use Chromia CLI to deploy your dapp by configuring chromia.yml, running deployment commands, and updating with the Blockchain RID $TESTNET_INDEX_CARD_CONNECT_CLIENT $TESTNET_CONNECT_CLIENT_URL Set up a frontend or client with postchain-client by configuring node URLs and your Blockchain RID $TESTNET_INDEX_CARD_LIST_VAULT $TESTNET_LIST_DAPP_VAULT_URL List your dapp on the Chromia Testnet Vault by implementing the find_dapp_details query and preparing media content through Filehub skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample BlockchainRID placeholders.
        Official BUILD deployment/mainnet ($MAINNET_INDEX_URL 307 $MAINNET_INDEX_URL_SLASH 200 $MAINNET_INDEX_TITLE): intro This section covers information about configuring your client and deploying and updating your dapp to the public Mainnet child cards $MAINNET_INDEX_CARD_GETTING_STARTED $MAINNET_GETTING_STARTED_URL Deploy your dapp to Chromia Mainnet in three steps: obtain a container, deploy your dapp, and connect a client $MAINNET_INDEX_CARD_GET_CONTAINER $MAINNET_GET_CONTAINER_URL Generate a key pair, create a Chromia account, and lease container space using Chromia CLI. Secure a Container ID for deployment $MAINNET_INDEX_CARD_DEPLOY_DAPP $MAINNET_DEPLOY_DAPP_URL Deploy your dapp with Chromia CLI by configuring chromia.yml and running deployment commands $MAINNET_INDEX_CARD_MULTI_DEPLOYMENT $MAINNET_MULTI_DEPLOYMENT_URL Deploy and update multi-owner dapps on Chromia using CLI tools by managing voter sets, thresholds, and proposals $MAINNET_INDEX_CARD_CONNECT_CLIENT $MAINNET_CONNECT_CLIENT_URL Connect your frontend to a dapp backend using postchain-client. Set up the client with system node URLs and your dapp's Blockchain RID $MAINNET_INDEX_CARD_LIST_VAULT $VAULT_LISTING_INDEX_URL List your dapp on the Chromia Vault by implementing the find_dapp_details query and preparing media content through Filehub skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample BlockchainRID placeholders.
        Official BUILD deployment/testnet-tokens ($TESTNET_TOKENS_INDEX_URL 307 $TESTNET_TOKENS_INDEX_URL_SLASH 200 $TESTNET_TOKENS_INDEX_TITLE): intro In this section you will discover how to claim Testnet tokens on the Chromia Testnet and Binance Testnets child cards $TESTNET_TOKENS_INDEX_CARD_TCHR_CHROMIA $TESTNET_TCHR_CHROMIA_URL Discover how you can claim your tCHR tokens for development purposes on the Chromia Testnet $TESTNET_TOKENS_INDEX_CARD_TCHR_BINANCE $TESTNET_TCHR_BINANCE_URL Learn how to obtain tCHR tokens on the Binance Smart Chain Testnet skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample BlockchainRID placeholders.
        Official BUILD deployment/vault-listing ($VAULT_LISTING_INDEX_URL_NOSLASH 307 $VAULT_LISTING_INDEX_URL_SLASH 200 $VAULT_LISTING_INDEX_TITLE): intro To list your dapp on the Chromia Vault, you need to implement a find_dapp_details query that provides metadata about your dapp prerequisites A deployed dapp on Chromia (Mainnet or Testnet) Media files for your dapp (icons, screenshots, etc.) Filehub or any web3 storage for media content Choose your approach child cards $VAULT_LISTING_INDEX_CARD_QUICK $VAULT_LISTING_QUICK_URL Quick setup (hardcoded metadata) Best for: Quick prototypes, simple dapps, or when you don't need frequent content updates Pros: Fast setup, simple implementation, no admin key management Cons: Requires redeployment for changes, no dynamic updates $VAULT_LISTING_INDEX_CARD_DYNAMIC $VAULT_LISTING_DYNAMIC_URL Dynamic setup (database-based metadata) Best for: Production dapps, frequent content updates, or when you need admin control Pros: Easy content updates without redeployment, admin control, no redeployment needed Cons: More complex initial setup, requires admin key management skip signed txs Generate a key pair keygen sample admin pubkey no sample keys no invented 64-hex no sample BlockchainRID placeholders.
        Official BUILD deployment/deploy-frontend-dapp ($DEPLOY_FRONTEND_INDEX_URL 307 $DEPLOY_FRONTEND_INDEX_URL_SLASH 200 $DEPLOY_FRONTEND_INDEX_TITLE): intro This topic provides a detailed guide on deploying a frontend application into the blockchain It includes steps for configuring environment variables, building and packaging the application, updating the blockchain settings, and accessing the deployed application through a web interface While this topic uses Next.js for demonstration, you can use any frontend framework (React, Vue.js, Angular, Svelte, vanilla JavaScript, etc.) as long as it generates files for deployment prerequisites A dapp should be ready for the deployment The dapp should be placed into the root of the frontend application file structure skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample BlockchainRID placeholders.
        Official BUILD deployment/testnet/getting-started ($TESTNET_GETTING_STARTED_INDEX_URL 307 $TESTNET_GETTING_STARTED_INDEX_URL_SLASH 200 $TESTNET_GETTING_STARTED_INDEX_TITLE): slash title intro Deploying a decentralized application (dapp) to the Chromia testnet involves several key steps Step 1 Obtain a container Step 2 Deploy your dapp Step 3 Connect a client query-only WRITE SKIP skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders. Official still says Specify your network (mainnet) on TESTNET page — bug --network testnet wins.
        Official BUILD deployment/mainnet/getting-started ($MAINNET_GETTING_STARTED_INDEX_URL 307 $MAINNET_GETTING_STARTED_INDEX_URL_SLASH 200 $MAINNET_GETTING_STARTED_INDEX_TITLE): slash title intro Deploying a decentralized application (dapp) to the Chromia Mainnet involves several key steps Step 1 Obtain a container Step 2 Deploy your dapp Step 3 Connect a client Specify your network (Mainnet) query-only WRITE SKIP skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders.
        Official BUILD deployment/testnet/deploy-dapp ($TESTNET_DEPLOY_DAPP_INDEX_URL 307 $TESTNET_DEPLOY_DAPP_INDEX_URL_SLASH 200 $TESTNET_DEPLOY_DAPP_INDEX_TITLE): slash title intro This topic covers deploying and updating a dapp with the Chromia CLI to the public testnet Prerequisite Install Chromia CLI Build a dapp that compiles Container ID Deploy the dapp Update the dapp query-only WRITE SKIP skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders. Official create update WRITE SKIP create write-back since CLI 0.30.0 wins over add the chains key by hand. Official reserved names auto-fill Directory brid + url since CLI 0.29.8 win.
        Official BUILD deployment/mainnet/deploy-dapp ($MAINNET_DEPLOY_DAPP_INDEX_URL 307 $MAINNET_DEPLOY_DAPP_INDEX_URL_SLASH 200 $MAINNET_DEPLOY_DAPP_INDEX_TITLE): slash title intro This topic covers deploying and updating a dapp with the Chromia CLI to the public Mainnet Prerequisites Install Chromia CLI Build a dapp that compiles Container ID Deploy the dapp Update the dapp query-only WRITE SKIP skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders no sample container hex placeholders. Official create update WRITE SKIP create write-back since CLI 0.30.0 wins over add the chains key by hand. Official reserved name mainnet auto-fills Directory brid + url since CLI 0.29.8 win.
        Official BUILD deployment/mainnet/multi-deployment ($MAINNET_MULTI_DEPLOYMENT_INDEX_URL 307 $MAINNET_MULTI_DEPLOYMENT_INDEX_URL_SLASH 200 $MAINNET_MULTI_DEPLOYMENT_INDEX_TITLE): slash title intro This guide provides step-by-step instructions for deploying and updating decentralized applications (dapps) on Chromia using multiple private keys and voter sets Prerequisites Steps for multi-owner deployment Steps for updating a dapp Steps for managing proposals Example configurations query-only WRITE SKIP skip signed txs voterset add-dapp-provider voterset update proposal vote proposal revoke pmc blockchain update Generate a key pair keygen no sample keys no invented 64-hex member keys placeholders.
        Official BUILD deployment/testnet/list-dapp-vault ($TESTNET_LIST_DAPP_VAULT_INDEX_URL 307 $TESTNET_LIST_DAPP_VAULT_INDEX_URL_SLASH 200 $TESTNET_LIST_DAPP_VAULT_INDEX_TITLE): slash title intro This guide provides step-by-step instructions for listing your decentralized application (dapp) on the Chromia Testnet Vault Prerequisites Listing steps find_dapp_details Filehub media Automatic listing Verification for a checkmark query-only WRITE SKIP skip Connect Wallet Request Tokens faucet message sign skip chr tx writes create_or_update_dapp create_or_update_blockchain create_or_update_dapp_media skip sample 64-hex BRIDs Filehub 64-hex media paths Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders.
        Official BUILD deployment/vault-listing/quick-vault-listing ($QUICK_VAULT_LISTING_INDEX_URL 307 $QUICK_VAULT_LISTING_INDEX_URL_SLASH 200 $QUICK_VAULT_LISTING_INDEX_TITLE): slash title intro Fast and simple approach to list your dapp on the Chromia Vault using hardcoded metadata Perfect for quick prototypes or simple dapps Prerequisites A deployed dapp on Chromia (Mainnet or Testnet) Media files Implementation Customization Configuration Include the module import quick_vault_listing Update your chromia.yml Deployment Automatic listing Making changes Verify the data Optional Storing media content on chain find_dapp_details Filehub media query-only WRITE SKIP skip signed txs chr deployment update writes skip sample 64-hex BRIDs Filehub 64-hex media paths Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders.
        Official BUILD deployment/vault-listing/dynamic-vault-listing ($DYNAMIC_VAULT_LISTING_INDEX_URL 307 $DYNAMIC_VAULT_LISTING_INDEX_URL_SLASH 200 $DYNAMIC_VAULT_LISTING_INDEX_TITLE): slash title intro Flexible approach to list your dapp on the Chromia Vault using database-stored metadata Perfect for production dapps or when you need frequent content updates Prerequisites A deployed dapp on Chromia (Mainnet or Testnet) Media files Admin key pair Implementation Configuration Include the module import dynamic_vault_listing Update your chromia.yml Deployment Automatic listing Making changes Admin key management Verify the data Optional Storing media content on chain find_dapp_details Filehub media query-only WRITE SKIP skip signed txs chr tx writes create_or_update_dapp create_or_update_blockchain create_or_update_dapp_media skip sample 64-hex BRIDs Filehub 64-hex media paths sample tx RIDs Generate a key pair keygen sample admin pubkey no sample keys no invented 64-hex no sample Blockchain RID placeholders.
        Official BUILD deployment/testnet-tokens/get-tchr-chromia ($GET_TCHR_CHROMIA_INDEX_URL 307 $GET_TCHR_CHROMIA_INDEX_URL_SLASH 200 $GET_TCHR_CHROMIA_INDEX_TITLE): slash title intro This guide will walk you through obtaining test tokens (tCHR) on the Chromia Testnet These tokens are essential for testing deployments and experimenting with Chromia's features without using real tokens Usage notes tCHR tokens are for testing on the Chromia Testnet and have no real-world value weekly allowace typo allowance 1000 tCHR every 7 days wins query-only WRITE SKIP skip faucet Connect Wallet Request Tokens Create Account faucet message sign signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders.
        Official BUILD deployment/testnet-tokens/get-tchr-binance ($GET_TCHR_BINANCE_INDEX_URL 307 $GET_TCHR_BINANCE_INDEX_URL_SLASH 200 $GET_TCHR_BINANCE_INDEX_TITLE): slash title intro This guide will walk you through obtaining test tokens (tCHR) on the Binance Smart Chain Testnet These tokens can be used for testing purposes when deploying a bridge between the BSC and Chromia Testnet token differences Chromia testnet tCHR 1000 tCHR weekly BSC testnet tCHR any EVM wallet cross-chain bridge EVM integration REAL bug Step 1 still says Access the Chromia testnet faucet BscScan faucet URL wins tBNB gas once a week no real-world value query-only WRITE SKIP skip Connect to web3 Connect Wallet Write Contract claim Request Tokens faucet message sign chr tx writes signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders.
        Official GET-STARTED create-dapp/deploy-to-testnet ($GET_STARTED_DEPLOY_TESTNET_INDEX_URL 307 $GET_STARTED_DEPLOY_TESTNET_INDEX_URL_SLASH 200 $GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE): slash title query-only HELP ONLY WRITE SKIP skip signed txs Generate a key pair keygen no sample keys no invented 64-hex no sample Blockchain RID placeholders.
        Official ECOSYSTEM ecosystem/providers/container-management/add-dapp-provider-to-voterset INDEX ($ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_URL 307 $ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/introduction INDEX ($LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_URL 301 $LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/marketplace-course/setup INDEX ($LEARN_MARKETPLACE_SETUP_INDEX_URL 301 $LEARN_MARKETPLACE_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_MARKETPLACE_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/Crosschain INDEX ($LEARN_TAGS_CROSSCHAIN_INDEX_URL 301 $LEARN_TAGS_CROSSCHAIN_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_CROSSCHAIN_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
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
        Official read-only query find_dapp_details(dapp_rowid: rowid, requested_content_types: list<dapp_content_type>? = null).
        Official verify quotes (quick + dynamic 200) must keep quotes: requested_content_types=[] and requested_content_types=["landscape", "portrait", "promotional", "video", "icon"].
        ${deployFrontendNote()}
        Official enum dapp_content_type: landscape, portrait, promotional, video, icon. Official indexes landscape=0 portrait=1 promotional=2 video=3 icon=4.
        Official BUILD testnet list-dapp-vault (200): $TESTNET_LIST_DAPP_VAULT_URL. Official instruction: $VAULT_IGNORE_ROWID. Official testnet verify quotes match vault-listing: requested_content_types=[]. Official recommended image sizes: 180x100 / 180x240 / 510x286 / 86x48 (recommended x3).
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
        Skip testnet chr tx writes, --secret, sample BRID placeholders, Filehub 64-hex media paths, setUpMocks sample 64-hex, sample container 64-hex, Connect Wallet / Request Tokens / faucet message sign, admin key pair snippet, and keygen.
        Official imports: import quick_vault_listing; import dynamic_vault_listing;.
        Official dynamic moduleArgs keys: admin_pubkey and dapp_name (keys only; do not invent a public key).
        Official Filehub cost 0.10 USD per MB perpetual; minimum 1 CHR; gateway host filehub-gw.chromia.com. Skip sample 64-hex Filehub paths.
        Skip create_or_update_dapp / create_or_update_blockchain / create_or_update_dapp_media chr tx writes and sample tx RIDs.
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
// Official BUILD cli/commands/deployment leftovers encoded as COMMANDS_DEPLOYMENT_INDEX_* (query-only).
// Official BUILD deployment/testnet/getting-started leftovers encoded as TESTNET_GETTING_STARTED_INDEX_* (query-only).
// Official BUILD deployment/mainnet/getting-started leftovers encoded as MAINNET_GETTING_STARTED_INDEX_* (query-only).
// Official BUILD deployment/testnet/deploy-dapp leftovers encoded as TESTNET_DEPLOY_DAPP_INDEX_* (query-only).
// Official BUILD deployment/mainnet/deploy-dapp leftovers encoded as MAINNET_DEPLOY_DAPP_INDEX_* (query-only).
// Official BUILD deployment/mainnet/multi-deployment leftovers encoded as MAINNET_MULTI_DEPLOYMENT_INDEX_* (query-only).
// Official BUILD deployment/testnet/list-dapp-vault leftovers encoded as TESTNET_LIST_DAPP_VAULT_INDEX_* (query-only).
// Official BUILD deployment/vault-listing/quick-vault-listing leftovers encoded as QUICK_VAULT_LISTING_INDEX_* (query-only).
// Official BUILD deployment/vault-listing/dynamic-vault-listing leftovers encoded as DYNAMIC_VAULT_LISTING_INDEX_* (query-only).
// Official BUILD deployment/testnet-tokens/get-tchr-chromia leftovers encoded as GET_TCHR_CHROMIA_INDEX_* (query-only).
// Official BUILD deployment/testnet-tokens/get-tchr-binance leftovers encoded as GET_TCHR_BINANCE_INDEX_* (query-only).
// Official GET-STARTED create-dapp/deploy-to-testnet leftovers encoded as GET_STARTED_DEPLOY_TESTNET_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/container-management/add-dapp-provider-to-voterset INDEX leftovers encoded as ECOSYSTEM_ADD_DAPP_PROVIDER_TO_VOTERSET_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/book-review/introduction INDEX leftovers encoded as LEARN_BOOK_REVIEW_INTRODUCTION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/marketplace-course/setup INDEX leftovers encoded as LEARN_MARKETPLACE_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/Crosschain INDEX leftovers encoded as LEARN_TAGS_CROSSCHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
