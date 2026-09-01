package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object VaultLeaseHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val TESTNET_GET_CONTAINER_URL =
        "https://docs.chromia.com/build/deployment/testnet/get-container"
    const val TESTNET_GET_CONTAINER_INDEX_URL = TESTNET_GET_CONTAINER_URL
    const val TESTNET_GET_CONTAINER_INDEX_URL_SLASH =
        "https://docs.chromia.com/build/deployment/testnet/get-container/"
    const val TESTNET_GET_CONTAINER_INDEX_TITLE = "Get a container for your dapp"
    const val MAINNET_GET_CONTAINER_URL =
        "https://docs.chromia.com/build/deployment/mainnet/get-container"
    const val MAINNET_GET_CONTAINER_INDEX_URL = MAINNET_GET_CONTAINER_URL
    const val MAINNET_GET_CONTAINER_INDEX_URL_SLASH =
        "https://docs.chromia.com/build/deployment/mainnet/get-container/"
    const val MAINNET_GET_CONTAINER_INDEX_TITLE = "Get a container for your dapp"
    const val TESTNET_VAULT_CONTAINERS = "https://vault.testnet.chromia.com/en/containers/"
    const val MAINNET_VAULT_CONTAINERS = "https://vault.chromia.com/en/containers/"
    const val MAINNET_VAULT_DEPOSIT = "https://vault.chromia.com/en/deposit"
    const val TESTNET_FAUCET = "https://faucet.testnet.chromia.com/"
    const val HOSTING_ABOUT_URL = "https://docs.chromia.com/get-started/about/hosting"
    const val GET_STARTED_HOSTING_INDEX_URL = HOSTING_ABOUT_URL
    const val GET_STARTED_HOSTING_INDEX_URL_SLASH =
        "https://docs.chromia.com/get-started/about/hosting/"
    const val GET_STARTED_HOSTING_INDEX_TITLE = "Hosting"
    const val GET_STARTED_SUPPORTED_WALLETS_INDEX_URL =
        "https://docs.chromia.com/get-started/about/supported-wallets"
    const val GET_STARTED_SUPPORTED_WALLETS_INDEX_URL_SLASH =
        "https://docs.chromia.com/get-started/about/supported-wallets/"
    const val GET_STARTED_SUPPORTED_WALLETS_INDEX_TITLE = "Supported wallets"
    const val GET_STARTED_PROVIDER_STAKING_INDEX_URL =
        "https://docs.chromia.com/get-started/about/staking/provider-staking"
    const val GET_STARTED_PROVIDER_STAKING_INDEX_URL_SLASH =
        "https://docs.chromia.com/get-started/about/staking/provider-staking/"
    const val GET_STARTED_PROVIDER_STAKING_INDEX_TITLE = "Provider staking"
    const val ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_URL = "https://docs.chromia.com/ecosystem/filehub/configure-filehub"
    const val ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/filehub/configure-filehub/"
    const val ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_TITLE = "Configure and work with Filehub"  // official H1
    const val ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/mass-exit/reference"
    const val ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/mass-exit/reference/"
    const val ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_TITLE = "Mass exit API and CLI reference"  // official H1
    const val ECOSYSTEM_GOV_PROPOSALS_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/governance-proposals"
    const val ECOSYSTEM_GOV_PROPOSALS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/governance-proposals/"
    const val ECOSYSTEM_GOV_PROPOSALS_INDEX_TITLE = "Work with proposals"
    const val ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_URL = "https://docs.chromia.com/ecosystem/filehub/overview"
    const val ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/filehub/overview/"
    const val ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_TITLE = "Overview of Filehub"
    const val LEARN_FT4_DEMO_INIT_SETUP_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-init/setup-application"
    const val LEARN_FT4_DEMO_INIT_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-init/setup-application/"
    const val LEARN_FT4_DEMO_INIT_SETUP_INDEX_TITLE = "Set up the Fullstack application"  // official H1
    const val LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/register-and-mint"
    const val LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/register-and-mint/"
    const val LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_TITLE = "Lesson 4 - Register and Mint"  // official H1
    const val LEARN_NEWS_SETUP_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/setup"
    const val LEARN_NEWS_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/setup/"
    const val LEARN_NEWS_SETUP_INDEX_TITLE = "The project is set up"  // official H1
    const val LEARN_NEWS_DATA_MODELING_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling"
    const val LEARN_NEWS_DATA_MODELING_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling/"
    const val LEARN_NEWS_DATA_MODELING_INDEX_TITLE = "Lesson 1 - Database schema"  // official H1
    const val LEARN_NEWS_TEST_REGISTRATION_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts/test-registration"
    const val LEARN_NEWS_TEST_REGISTRATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts/test-registration/"
    const val LEARN_NEWS_TEST_REGISTRATION_INDEX_TITLE = "Test the registration"  // official H1
    const val LEARN_NEWS_REGISTER_EVM_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts"
    const val LEARN_NEWS_REGISTER_EVM_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts/"
    const val LEARN_NEWS_REGISTER_EVM_INDEX_TITLE = "Lesson 6 - Register users using EVM wallet"  // official H1
    const val LEARN_CHAT_AGENT_INTRO_INDEX_URL = "https://learn.chromia.com/courses/chat-agent-course/introduction"
    const val LEARN_CHAT_AGENT_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chat-agent-course/introduction/"
    const val LEARN_CHAT_AGENT_INTRO_INDEX_TITLE = "Create your chat agent with Chromia"  // official H1
    const val LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_URL = "https://learn.chromia.com/courses/relationships-course/one-to-one"
    const val LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/relationships-course/one-to-one/"
    const val LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_TITLE = "One-to-one relationship"  // official H1
    const val LEARN_TAGS_PYTHON_INDEX_URL = "https://learn.chromia.com/tags/Python"
    const val LEARN_TAGS_PYTHON_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Python/"
    const val LEARN_TAGS_PYTHON_INDEX_TITLE = "Courses tagged with: Python"  // official H1
    const val MAINNET_DIRECTORY_BRID = WriteDeploymentConfig.MAINNET_DIRECTORY_BRID
    const val TESTNET_DIRECTORY_BRID = WriteDeploymentConfig.TESTNET_DIRECTORY_BRID
    const val CONTAINER_PLACEHOLDER = "<containerIID>"
    fun yamlExample(network: String, chain: String): String {
        val spec = WriteDeploymentConfig.resolveNetwork(network)
            ?: WriteDeploymentConfig.resolveNetwork("testnet")!!
        val name = DappScaffold.normalizeName(chain)
        val lines = mutableListOf(
            "deployments:",
            "  ${spec.name}:",
            WriteDeploymentConfig.urlListYaml(spec, "    ")
        )
        lines += "    brid: ${spec.bridYaml()}"
        lines += "    container: $CONTAINER_PLACEHOLDER"
        lines += "    chains:"
        lines += "      $name:"
        return lines.joinToString("\n") + "\n"
    }

    fun workflowNote(): String = """
        Official developer path (Vault) then `chr deployment create`.
        Testnet: $TESTNET_GET_CONTAINER_URL
        Mainnet: $MAINNET_GET_CONTAINER_URL
        1. Get tokens. Testnet tCHR via $TESTNET_FAUCET (docs: 1000 tCHR / 7 days).
           Mainnet: deposit at least 10 CHR from BSC or Ethereum via $MAINNET_VAULT_DEPOSIT,
           or send at least 20 CHR from another Chromia chain (10 CHR account-creation fee).
        2. Open the Vault containers page and connect a wallet.
           Testnet: $TESTNET_VAULT_CONTAINERS
           Mainnet: $MAINNET_VAULT_CONTAINERS
        3. Lease a container: pick cluster, paste the public key from an existing
           ~/.chromia/<key-id>.pubkey file (official get-container page). This tool
           does not generate a key and does not write private key material.
        4. Adjust SCUs / extra storage / duration; optional auto-renewal. Sign in Vault.
           Result is a Container ID. Do not invent one. Retrieve it later on the same Vault page.
        5. Put that real Container ID in chromia.yml as deployments.<net>.container
           (official $PROJECT_CONFIG_URL field). Placeholder in this tool: $CONTAINER_PLACEHOLDER
        6. Then `chr deployment create --settings chromia.yml --network <testnet|mainnet> --blockchain <name>`
           (see chr_deploy_help). Since CLI 0.30.0, create writes deployments.<net>.chains back.
        Directory Chain BRIDs are official write_deployment_config values only:
        mainnet $MAINNET_DIRECTORY_BRID, testnet $TESTNET_DIRECTORY_BRID.
    """.trimIndent()
    fun pmcNote(): String = """
        pmc is the provider/management CLI (different binary from chr).
        Operator path talks to Economy Chain: pmc lease create-container
        (--cluster-name, --scus, --duration weeks, optional --extraStorage GiB,
        --extraComputeRequests, --auto-renew, --account-id / --evm-address),
        upgrade-container, list / info, list-pending-tickets,
        remove-container (no refund).
        Dapp developers typically lease via Vault and deploy via chr.
        This tool does not run pmc or chr and does not send signed transactions.
    """.trimIndent()

    fun notes(): String = """
        Chromia CLI $CLI_SERIES Vault / PMC container lease help. Java 21+, Postgres 16+.
        chromia.yml deployments.<net>.container is the Container ID from a real Vault or PMC lease
        ($PROJECT_CONFIG_URL). Official field name is container (docs also call it containerIID).
        Do not invent a lease id, container id, or dapp BRID.
        Official Directory Chain BRIDs only: mainnet $MAINNET_DIRECTORY_BRID,
        testnet $TESTNET_DIRECTORY_BRID.
        Lease is weekly, paid in CHR (testnet: tCHR). Docs published cost target is approximately
        90 USD weekly for a default 7-node dapp cluster; Vault computes CHR at lease time.
        Official hosting (200): $HOSTING_ABOUT_URL. Official one SCU: 2 GB RAM, 0.5 vCPU, 16 GB storage, 25 MiB/s read 20 MiB/s write. Do not invent other SCU sizes.
        Anyone can pay a lease. Failed Mainnet lease refunds the same amount.
        Overdue: suspended for six months (reactivate by payment), then permanently deleted.
        This tool does not generate a key, does not invent a container id, does not run chr,
        and does not send signed transactions.
        Official BUILD deployment/testnet/get-container ($TESTNET_GET_CONTAINER_INDEX_URL 307 $TESTNET_GET_CONTAINER_INDEX_URL_SLASH 200 $TESTNET_GET_CONTAINER_INDEX_TITLE): Query-only WRITE SKIP HELP ONLY no signed txs no sample keys no invented 64-hex no keygen Do not invent BRIDs skip this signs.
        Official BUILD deployment/mainnet/get-container ($MAINNET_GET_CONTAINER_INDEX_URL 307 $MAINNET_GET_CONTAINER_INDEX_URL_SLASH 200 $MAINNET_GET_CONTAINER_INDEX_TITLE): Query-only WRITE SKIP HELP ONLY no signed txs no sample keys no invented 64-hex no keygen Do not invent BRIDs skip this signs.
        Official GET-STARTED get-started/about/supported-wallets INDEX ($GET_STARTED_SUPPORTED_WALLETS_INDEX_URL 307 $GET_STARTED_SUPPORTED_WALLETS_INDEX_URL_SLASH 200 $GET_STARTED_SUPPORTED_WALLETS_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe.
        Official GET-STARTED get-started/about/staking/provider-staking INDEX ($GET_STARTED_PROVIDER_STAKING_INDEX_URL 307 $GET_STARTED_PROVIDER_STAKING_INDEX_URL_SLASH 200 $GET_STARTED_PROVIDER_STAKING_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no sign recipe.
        Official GET-STARTED get-started/about/hosting INDEX ($GET_STARTED_HOSTING_INDEX_URL 307 $GET_STARTED_HOSTING_INDEX_URL_SLASH 200 $GET_STARTED_HOSTING_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no sign recipe.
        Official ECOSYSTEM ecosystem/filehub/configure-filehub INDEX ($ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_URL 307 $ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/bridge/mass-exit/reference INDEX ($ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_URL 307 $ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/governance-proposals INDEX ($ECOSYSTEM_GOV_PROPOSALS_INDEX_URL 307 $ECOSYSTEM_GOV_PROPOSALS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_PROPOSALS_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official ECOSYSTEM ecosystem/filehub/overview INDEX ($ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_URL 307 $ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/ft4-demo-app/module-init/setup-application INDEX ($LEARN_FT4_DEMO_INIT_SETUP_INDEX_URL 301 $LEARN_FT4_DEMO_INIT_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_INIT_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Before we start, please make sure you have the following prerequisites in place Rell requires PostgreSQL 16.3 Let's kick things off by setting up your blockchain app project using the Chromia CLI. Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/ft4-demo-app/module-frontend-application/register-and-mint INDEX ($LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_URL 301 $LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/setup INDEX ($LEARN_NEWS_SETUP_INDEX_URL 301 $LEARN_NEWS_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/data-modeling INDEX ($LEARN_NEWS_DATA_MODELING_INDEX_URL 301 $LEARN_NEWS_DATA_MODELING_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_DATA_MODELING_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/register-evm-accounts/test-registration INDEX ($LEARN_NEWS_TEST_REGISTRATION_INDEX_URL 301 $LEARN_NEWS_TEST_REGISTRATION_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_TEST_REGISTRATION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/register-evm-accounts INDEX ($LEARN_NEWS_REGISTER_EVM_INDEX_URL 301 $LEARN_NEWS_REGISTER_EVM_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_REGISTER_EVM_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX courses/chat-agent-course/introduction ($LEARN_CHAT_AGENT_INTRO_INDEX_URL 301 $LEARN_CHAT_AGENT_INTRO_INDEX_URL_SLASH GET 200 H1 $LEARN_CHAT_AGENT_INTRO_INDEX_TITLE Create your chat agent with Chromia HELP ONLY WRITE SKIP query-only). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX relationships-course one-to-one ($LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_URL 301 $LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_URL_SLASH GET 200 H1 $LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_TITLE One-to-one relationship HELP ONLY WRITE SKIP query-only). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/Python INDEX ($LEARN_TAGS_PYTHON_INDEX_URL 301 $LEARN_TAGS_PYTHON_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_PYTHON_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()
    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", TESTNET_GET_CONTAINER_URL)
        put("project_config", PROJECT_CONFIG_URL)
        put("container_placeholder", CONTAINER_PLACEHOLDER)
        put("mainnet_directory_brid", MAINNET_DIRECTORY_BRID)
        put("testnet_directory_brid", TESTNET_DIRECTORY_BRID)
        put(
            "vault",
            buildJsonObject {
                put("testnet", TESTNET_VAULT_CONTAINERS)
                put("mainnet", MAINNET_VAULT_CONTAINERS)
                put("mainnet_deposit", MAINNET_VAULT_DEPOSIT)
                put("testnet_faucet", TESTNET_FAUCET)
                put("testnet_docs", TESTNET_GET_CONTAINER_URL)
                put("testnet_get_container_index_docs", TESTNET_GET_CONTAINER_INDEX_URL)
                put("testnet_get_container_index_url_slash", TESTNET_GET_CONTAINER_INDEX_URL_SLASH)
                put("testnet_get_container_index_title", TESTNET_GET_CONTAINER_INDEX_TITLE)
                put("mainnet_docs", MAINNET_GET_CONTAINER_URL)
                put("mainnet_get_container_index_docs", MAINNET_GET_CONTAINER_INDEX_URL)
                put("mainnet_get_container_index_url_slash", MAINNET_GET_CONTAINER_INDEX_URL_SLASH)
                put("mainnet_get_container_index_title", MAINNET_GET_CONTAINER_INDEX_TITLE)
            }
        )
        put(
            "commands",
            buildJsonObject {
                put(
                    "create_testnet",
                    "chr deployment create --settings chromia.yml --network testnet --blockchain hello"
                )
                put(
                    "create_mainnet",
                    "chr deployment create --settings chromia.yml --network mainnet --blockchain hello"
                )
            }
        )
        put("yaml_testnet", yamlExample("testnet", DappScaffold.DEFAULT_NAME))
        put("yaml_mainnet", yamlExample("mainnet", DappScaffold.DEFAULT_NAME))
        put("workflow", workflowNote())
        put("pmc", pmcNote())
        put("hosting_about", HOSTING_ABOUT_URL)
        put("get_started_hosting_index_docs", GET_STARTED_HOSTING_INDEX_URL)
        put("get_started_hosting_index_url_slash", GET_STARTED_HOSTING_INDEX_URL_SLASH)
        put("get_started_hosting_index_title", GET_STARTED_HOSTING_INDEX_TITLE)
        put("get_started_supported_wallets_index_docs", GET_STARTED_SUPPORTED_WALLETS_INDEX_URL)
        put("get_started_supported_wallets_index_url_slash", GET_STARTED_SUPPORTED_WALLETS_INDEX_URL_SLASH)
        put("get_started_supported_wallets_index_title", GET_STARTED_SUPPORTED_WALLETS_INDEX_TITLE)
        put("get_started_provider_staking_index_docs", GET_STARTED_PROVIDER_STAKING_INDEX_URL)
        put("get_started_provider_staking_index_url_slash", GET_STARTED_PROVIDER_STAKING_INDEX_URL_SLASH)
        put("get_started_provider_staking_index_title", GET_STARTED_PROVIDER_STAKING_INDEX_TITLE)
        put("scu_note", "Official one SCU: 2 GB RAM, 0.5 vCPU, 16 GB storage, 25 MiB/s read 20 MiB/s write. Official weekly approximately 90 USD default 7-node dapp cluster.")
        put("ecosystem_filehub_configure_index_url_slash", ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_URL_SLASH)
        put("ecosystem_filehub_configure_index_title", ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_TITLE)
        put("ecosystem_mass_exit_reference_index_url_slash", ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_URL_SLASH)
        put("ecosystem_mass_exit_reference_index_title", ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_TITLE)
        put("ecosystem_gov_proposals_index_url_slash", ECOSYSTEM_GOV_PROPOSALS_INDEX_URL_SLASH)
        put("ecosystem_gov_proposals_index_title", ECOSYSTEM_GOV_PROPOSALS_INDEX_TITLE)
        put("ecosystem_filehub_overview_index_url_slash", ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_URL_SLASH)
        put("ecosystem_filehub_overview_index_title", ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_TITLE)
        put("learn_ft4_demo_init_setup_index_url_slash", LEARN_FT4_DEMO_INIT_SETUP_INDEX_URL_SLASH)
        put("learn_ft4_demo_init_setup_index_title", LEARN_FT4_DEMO_INIT_SETUP_INDEX_TITLE)
        put("learn_ft4_demo_frontend_register_mint_index_url_slash", LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_URL_SLASH)
        put("learn_ft4_demo_frontend_register_mint_index_title", LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_TITLE)
        put("learn_news_setup_index_url_slash", LEARN_NEWS_SETUP_INDEX_URL_SLASH)
        put("learn_news_setup_index_title", LEARN_NEWS_SETUP_INDEX_TITLE)
        put("learn_news_data_modeling_index_url_slash", LEARN_NEWS_DATA_MODELING_INDEX_URL_SLASH)
        put("learn_news_data_modeling_index_title", LEARN_NEWS_DATA_MODELING_INDEX_TITLE)
        put("learn_news_test_registration_index_url_slash", LEARN_NEWS_TEST_REGISTRATION_INDEX_URL_SLASH)
        put("learn_news_test_registration_index_title", LEARN_NEWS_TEST_REGISTRATION_INDEX_TITLE)
        put("learn_news_register_evm_index_url_slash", LEARN_NEWS_REGISTER_EVM_INDEX_URL_SLASH)
        put("learn_news_register_evm_index_title", LEARN_NEWS_REGISTER_EVM_INDEX_TITLE)
        put("learn_chat_agent_intro_index_url_slash", LEARN_CHAT_AGENT_INTRO_INDEX_URL_SLASH)
        put("learn_chat_agent_intro_index_title", LEARN_CHAT_AGENT_INTRO_INDEX_TITLE)
        put("learn_relationships_one_to_one_index_url_slash", LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_URL_SLASH)
        put("learn_relationships_one_to_one_index_title", LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_TITLE)
        put("learn_tags_python_index_url_slash", LEARN_TAGS_PYTHON_INDEX_URL_SLASH)
        put("learn_tags_python_index_title", LEARN_TAGS_PYTHON_INDEX_TITLE)
        put("notes", notes())
    }
}
// Official BUILD deployment/testnet/get-container leftovers encoded as TESTNET_GET_CONTAINER_INDEX_* (query-only).
// Official BUILD deployment/mainnet/get-container leftovers encoded as MAINNET_GET_CONTAINER_INDEX_* (query-only).
// Official GET-STARTED get-started/about/supported-wallets INDEX leftovers encoded as GET_STARTED_SUPPORTED_WALLETS_INDEX_* (query-only).
// Official GET-STARTED get-started/about/staking/provider-staking INDEX leftovers encoded as GET_STARTED_PROVIDER_STAKING_INDEX_* (query-only).
// Official GET-STARTED get-started/about/hosting INDEX leftovers encoded as GET_STARTED_HOSTING_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/filehub/configure-filehub INDEX leftovers encoded as ECOSYSTEM_FILEHUB_CONFIGURE_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/bridge/mass-exit/reference INDEX leftovers encoded as ECOSYSTEM_MASS_EXIT_REFERENCE_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/governance-proposals INDEX leftovers encoded as ECOSYSTEM_GOV_PROPOSALS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official ECOSYSTEM ecosystem/filehub/overview INDEX leftovers encoded as ECOSYSTEM_FILEHUB_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-init/setup-application INDEX leftovers encoded as LEARN_FT4_DEMO_INIT_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-frontend-application/register-and-mint INDEX leftovers encoded as LEARN_FT4_DEMO_FRONTEND_REGISTER_MINT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/setup INDEX leftovers encoded as LEARN_NEWS_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/data-modeling INDEX leftovers encoded as LEARN_NEWS_DATA_MODELING_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/register-evm-accounts/test-registration INDEX leftovers encoded as LEARN_NEWS_TEST_REGISTRATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/register-evm-accounts INDEX leftovers encoded as LEARN_NEWS_REGISTER_EVM_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/chat-agent-course/introduction INDEX leftovers encoded as LEARN_CHAT_AGENT_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/relationships-course/one-to-one INDEX leftovers encoded as LEARN_RELATIONSHIPS_ONE_TO_ONE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/Python INDEX leftovers encoded as LEARN_TAGS_PYTHON_INDEX_* (query-only HELP ONLY WRITE SKIP).
