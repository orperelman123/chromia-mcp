package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia project-structure + Rell modules layout help.
 * Sources: docs.chromia.com/build/configuration/project-structure
 * and docs.chromia.com/rell/modules.
 * Official examples only. Does not run chr.
 * Official BUILD configuration index slash/title/child-card leftovers live here (query-only).
 * Official BUILD configuration/project-structure index slash/title leftovers live here (query-only).
 * Official BUILD configuration/project-config index slash/title leftovers live here (query-only).
 * Official GET-STARTED get-started/about INDEX leftovers live here (query-only).
 * Official GET-STARTED get-started/about/architecture/chain-governance INDEX leftovers live here (query-only).
 * Official GET-STARTED get-started/about/architecture/platform-architecture INDEX leftovers live here (query-only).
 * Official GET-STARTED get-started/about/staking INDEX leftovers live here (query-only).
 * Official ECOSYSTEM ecosystem/governance/overview INDEX leftovers live here (query-only).
 */
object ChromiaProjectStructureHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val PROJECT_STRUCTURE_URL = "https://docs.chromia.com/build/configuration/project-structure"
    const val MODULES_URL = "https://docs.chromia.com/rell/modules"
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val TOOL_NAME = "chromia_project_structure_help"
    const val CONFIGURATION_INDEX_URL = "https://docs.chromia.com/build/configuration"
    const val CONFIGURATION_INDEX_URL_SLASH = "https://docs.chromia.com/build/configuration/"
    const val CONFIGURATION_INDEX_TITLE = "Configuration"
    const val CONFIGURATION_INDEX_CARD_PROJECT_STRUCTURE = "Project structure"
    const val CONFIGURATION_INDEX_CARD_PROJECT_SETTINGS = "Project settings file"
    const val CONFIGURATION_INDEX_CARD_PROPERTIES = "Configuration properties"
    const val PROJECT_STRUCTURE_INDEX_URL = PROJECT_STRUCTURE_URL
    const val PROJECT_STRUCTURE_INDEX_URL_SLASH = "https://docs.chromia.com/build/configuration/project-structure/"
    const val PROJECT_STRUCTURE_INDEX_TITLE = "Project structure"
    const val PROJECT_CONFIG_INDEX_URL = "https://docs.chromia.com/build/configuration/project-config"
    const val PROJECT_CONFIG_INDEX_URL_SLASH = "https://docs.chromia.com/build/configuration/project-config/"
    const val PROJECT_CONFIG_INDEX_TITLE = "Project settings file"
    const val GET_STARTED_ABOUT_INDEX_URL = "https://docs.chromia.com/get-started/about"
    const val GET_STARTED_ABOUT_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/"
    const val GET_STARTED_ABOUT_INDEX_TITLE = "Introduction"
    const val GET_STARTED_CHAIN_GOVERNANCE_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/chain-governance"
    const val GET_STARTED_CHAIN_GOVERNANCE_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/chain-governance/"
    const val GET_STARTED_CHAIN_GOVERNANCE_INDEX_TITLE = "Governance"
    const val GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/platform-architecture"
    const val GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/platform-architecture/"
    const val GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_TITLE = "Platform architecture"
    const val GET_STARTED_STAKING_INDEX_URL = "https://docs.chromia.com/get-started/about/staking"
    const val GET_STARTED_STAKING_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/staking/"
    const val GET_STARTED_STAKING_INDEX_TITLE = "Staking"
    const val ECOSYSTEM_PMC_INSTALL_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/pmccli-installation"
    const val ECOSYSTEM_PMC_INSTALL_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/pmccli-installation/"
    const val ECOSYSTEM_PMC_INSTALL_INDEX_TITLE = "Install PMC CLI"  // official H1
    const val REFERENCE_FT4_INDEX_URL = "https://docs.chromia.com/reference/ft4"
    const val REFERENCE_FT4_INDEX_URL_SLASH = "https://docs.chromia.com/reference/ft4/"
    const val REFERENCE_FT4_INDEX_TITLE = "FT4 API reference"  // official H1
    const val ECOSYSTEM_GOV_OVERVIEW_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/overview"
    const val ECOSYSTEM_GOV_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/overview/"
    const val ECOSYSTEM_GOV_OVERVIEW_INDEX_TITLE = "Overview"  // official H1
    const val LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_URL = "https://learn.chromia.com/courses/big-data/blockchain-side-description"
    const val LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/big-data/blockchain-side-description/"
    const val LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_TITLE = "Blockchain components"
    const val LEARN_FT4_DEMO_INTRO_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/introduction"
    const val LEARN_FT4_DEMO_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/introduction/"
    const val LEARN_FT4_DEMO_INTRO_INDEX_TITLE = "Build an asset management system with FT4"
    const val LEARN_CI_DEPLOY_INDEX_URL = "https://learn.chromia.com/courses/continuous-integration-deploy"
    const val LEARN_CI_DEPLOY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/continuous-integration-deploy/"
    const val LEARN_CI_DEPLOY_INDEX_TITLE = "Deploy your project"
    const val LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/account-regisration"
    const val LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/account-regisration/"
    const val LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_TITLE = "Lesson 3 - Account Registration"
    const val LEARN_NEWS_INPUT_VERIFICATION_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/input-verification"
    const val LEARN_NEWS_INPUT_VERIFICATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/input-verification/"
    const val LEARN_NEWS_INPUT_VERIFICATION_INDEX_TITLE = "Lesson 4 - Input verification and validation"  // official H1
    const val LEARN_TTT_DATA_MODELING_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/data-modeling"
    const val LEARN_TTT_DATA_MODELING_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/data-modeling/"
    const val LEARN_TTT_DATA_MODELING_INDEX_TITLE = "Lesson 1 - Design database, use operations and queries"  // official H1
    const val LEARN_VECTOR_DB_PIPELINE_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline"
    const val LEARN_VECTOR_DB_PIPELINE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/"
    const val LEARN_VECTOR_DB_PIPELINE_INDEX_TITLE = "Module 2 – Run the data pipeline"  // official H1
    const val LEARN_ZK_DAPP_OVERVIEW_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-overview"
    const val LEARN_ZK_DAPP_OVERVIEW_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-overview/"
    const val LEARN_ZK_DAPP_OVERVIEW_INDEX_TITLE = "Dapp overview"  // official H1
    const val LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_URL = "https://learn.chromia.com/courses/book-review/sign-transaction/structure"
    const val LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/sign-transaction/structure/"
    const val LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_TITLE = "Using filters and sorting in queries"  // official H1
    const val LEARN_WEB3_CLASSIC_STACK_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/classic-web2-stack"
    const val LEARN_WEB3_CLASSIC_STACK_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/classic-web2-stack/"
    const val LEARN_WEB3_CLASSIC_STACK_INDEX_TITLE = "Traditional web app overview"  // official H1
    const val RELL_IDENTIFIERS_INDEX_URL = "https://docs.chromia.com/rell/language-features/identifiers-syntax"
    const val RELL_IDENTIFIERS_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/identifiers-syntax/"
    const val RELL_IDENTIFIERS_INDEX_TITLE = "Identifiers"  // official H1
    const val RELL_EXPRESSIONS_OPERATORS_INDEX_URL = "https://docs.chromia.com/rell/language-features/expressions/operators"
    const val RELL_EXPRESSIONS_OPERATORS_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/expressions/operators/"
    const val RELL_EXPRESSIONS_OPERATORS_INDEX_TITLE = "Operators"  // official H1
    const val RELL_SYSTEMLIB_QUERIES_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/system-queries"
    const val RELL_SYSTEMLIB_QUERIES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/system-queries/"
    const val RELL_SYSTEMLIB_QUERIES_INDEX_TITLE = "System queries"  // official H1
    const val LEARN_TAGS_DAPP_INDEX_URL = "https://learn.chromia.com/tags/Dapp"
    const val LEARN_TAGS_DAPP_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Dapp/"
    const val LEARN_TAGS_DAPP_INDEX_TITLE = "Courses tagged with: Dapp"  // official H1

    fun createRellDappLayout(): String = """
        |--chromia.yml
        |--src
           |--main.rell
           |--test
              |--arithmetic_test.rell
              |--data_test.rell
    """.trimIndent() + "\n"

    fun multiFileLayout(): String = """
        |--chromia.yml
        |--src
          |--module_a
            |--module.rell
            |--operations.rell
            |--queries.rell
          |--module_b
            |--module.rell
            |--util.rell
    """.trimIndent() + "\n"

    fun recommendedAppLayout(): String = """
        .
        └── app
            ├── module.rell
            ├── entities.rell
            ├── operations.rell
            ├── queries.rell
            ├── functions.rell
            └── structs.rell
    """.trimIndent() + "\n"

    fun singleFileModuleExample(): String = """
        module;
        // entities, operations, queries, functions, and other definitions
    """.trimIndent() + "\n"

    val importExamples = listOf(
        "import app.single;",
        "import alias: app.multi;",
        "import .d;",
        "import alias: ^;",
        "import alias: ^^;",
        "import ^.e;",
        "import foo.*;",
        "import foo.{ns.*};",
        "import sub: foo.{ns.*};",
        "import foo.{f};",
        "import foo.{g, h};",
        "import ns: foo.{f, g};",
        "import foo.{a: f, b: g};"
    )

    fun notes(): String = """
        Official Chromia project structure and Rell modules for CLI $CLI_SERIES.
        Project layout: $PROJECT_STRUCTURE_URL
        Official BUILD configuration ($CONFIGURATION_INDEX_URL 307 $CONFIGURATION_INDEX_URL_SLASH 200 $CONFIGURATION_INDEX_TITLE): intro To build and run a dapp on Chromia, set up your project and configure your blockchain to connect the frontend with the Rell backend for efficient data handling child cards $CONFIGURATION_INDEX_CARD_PROJECT_STRUCTURE $PROJECT_STRUCTURE_URL How a Chromia project is organized $CONFIGURATION_INDEX_CARD_PROJECT_SETTINGS $PROJECT_CONFIG_URL The chromia.yml configuration file for your dapp $CONFIGURATION_INDEX_CARD_PROPERTIES ${BlockchainPropertiesHelp.DOCS_URL} Blockchain configuration properties and settings skip signed txs no sample keys no invented 64-hex.
        Official BUILD configuration/project-structure ($PROJECT_STRUCTURE_INDEX_URL 307 $PROJECT_STRUCTURE_INDEX_URL_SLASH 200 $PROJECT_STRUCTURE_INDEX_TITLE): intro In Chromia, projects help you organize your Rell code and resources in a single unit that is easy to store and share In simple words, a project is a directory that keeps everything that makes up your dapp A typical project normally has a set of settings and one or several modules Rell modules can be composed and reused more easily than traditional smart contracts and they can be updated without disrupting the entire blockchain network define assets manage user accounts create custom token economies implement complex business logic for dapps interact with other modules and external systems through well-defined interfaces and APIs chr create-rell-dapp main.rell module keywoard single file module Hello World query src/test/ chromia.yml module_a module_b multifile modules module.rell module keyword All files within the same module can access each other's definitions without explicit imports note When specifying an entry point in chromia.yml use the module name single module filename or foldername not a file path skip signed txs no sample keys no invented 64-hex.
        Official BUILD configuration/project-config ($PROJECT_CONFIG_INDEX_URL 307 $PROJECT_CONFIG_INDEX_URL_SLASH 200 $PROJECT_CONFIG_INDEX_TITLE): intro Most configurations for your dapp are available in the project configuration file By default, it's called chromia.yml, but you can name it to something else if there is a need to do so Following is an example of such a file Remember that most attributes have default values and don't need to be configured unless you want to override the default behavior blockchains defines the blockchains used in your project module Specifies blockchains entry point by module moduleArgs arguments config configuration settings test tests deployments deployment targets mainnet testnet predefined network names compile Rell compiler settings rellVersion source target deprecatedError quiet strictGtvConversion database PostgreSQL connection settings libs Library-chain libraries External Git libraries docs generate docs-site YAML anchors definitions !include skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex.
        Official GET-STARTED get-started/about INDEX ($GET_STARTED_ABOUT_INDEX_URL 307 $GET_STARTED_ABOUT_INDEX_URL_SLASH 200 $GET_STARTED_ABOUT_INDEX_TITLE): slash title HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no sign recipe.
        Official GET-STARTED get-started/about/architecture/chain-governance INDEX ($GET_STARTED_CHAIN_GOVERNANCE_INDEX_URL 307 $GET_STARTED_CHAIN_GOVERNANCE_INDEX_URL_SLASH 200 $GET_STARTED_CHAIN_GOVERNANCE_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe docs INDEX only not on-chain voting.
        Official GET-STARTED get-started/about/architecture/platform-architecture INDEX ($GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_URL 307 $GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_URL_SLASH 200 $GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no keygen no sign recipe.
        Official GET-STARTED get-started/about/staking INDEX ($GET_STARTED_STAKING_INDEX_URL 307 $GET_STARTED_STAKING_INDEX_URL_SLASH 200 $GET_STARTED_STAKING_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no sign recipe.
        Official ECOSYSTEM ecosystem/providers/pmc/pmccli-installation INDEX ($ECOSYSTEM_PMC_INSTALL_INDEX_URL 307 $ECOSYSTEM_PMC_INSTALL_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_INSTALL_INDEX_TITLE). Query-only.
        Official REFERENCE reference/ft4 INDEX ($REFERENCE_FT4_INDEX_URL 307 $REFERENCE_FT4_INDEX_URL_SLASH 200 H1 $REFERENCE_FT4_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/governance/overview INDEX ($ECOSYSTEM_GOV_OVERVIEW_INDEX_URL 307 $ECOSYSTEM_GOV_OVERVIEW_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_OVERVIEW_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP. Governance Tool is on-chain voting help docs INDEX only, not an on-chain voting bot. Skip signed txs, sample keys, invented 64-hex.
        Official LEARN courses/big-data/blockchain-side-description INDEX ($LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_URL 301 $LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_URL_SLASH 200 H1 $LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/ft4-demo-app/introduction INDEX ($LEARN_FT4_DEMO_INTRO_INDEX_URL 301 $LEARN_FT4_DEMO_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/continuous-integration-deploy INDEX ($LEARN_CI_DEPLOY_INDEX_URL 301 $LEARN_CI_DEPLOY_INDEX_URL_SLASH 200 H1 $LEARN_CI_DEPLOY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/ft4-demo-app/module-frontend-application/account-regisration INDEX ($LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_URL 301 $LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/input-verification INDEX ($LEARN_NEWS_INPUT_VERIFICATION_INDEX_URL 301 $LEARN_NEWS_INPUT_VERIFICATION_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_INPUT_VERIFICATION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/tic-tac-toe/module-one/data-modeling INDEX ($LEARN_TTT_DATA_MODELING_INDEX_URL 301 $LEARN_TTT_DATA_MODELING_INDEX_URL_SLASH 200 H1 $LEARN_TTT_DATA_MODELING_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/vector-db-movie-demo/data-pipeline INDEX ($LEARN_VECTOR_DB_PIPELINE_INDEX_URL 301 $LEARN_VECTOR_DB_PIPELINE_INDEX_URL_SLASH 200 H1 $LEARN_VECTOR_DB_PIPELINE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Rell modules: $MODULES_URL
        chromia.yml entry point: $PROJECT_CONFIG_URL (`blockchains.<name>.module` is a module name, never a file path).
        `chr create-rell-dapp` writes chromia.yml + src/main.rell (single-file `module;`) + src/test/arithmetic_test.rell + data_test.rell.
        Official 0.33.2 default chromia.yml: compile.rellVersion 0.14.5 blockchains.my_rell_dapp.module main database.schema schema_my_rell_dapp test.modules test no merkle_hash_version no database host/user/password.
        Multi-file: a folder is a directory module when it contains `module.rell` starting with `module`. All files in that folder see each other without imports.
        Official Rell modules page: a module is a single .rell file with a `module;` header, or a directory of .rell files.
        A .rell file with no module header belongs to the directory module. `module.rell` always belongs to the directory module even if it has a header. A directory module does not require `module.rell`.
        A single-file module sees only its own definitions. There may be a root module (directory module of .rell files in the source root) with an empty name.
        Official recommended larger-module files under `app/`: module.rell (imports + mount names), entities.rell, operations.rell, queries.rell, functions.rell, structs.rell (when more than ~3 structs).
        At run-time only the main module (named in chromia.yml) and modules it imports (directly or indirectly) are active. Inactive modules contribute neither operations/queries nor tables.
        Official import forms are listed. Do not invent import verbs.
        The modules page namespace example imports lib.ft4.core.admin — that is a docs sample. NEVER ship ${DappScaffold.forbiddenModules.joinToString(", ")} in production.
        Official RELL rell/language-features/identifiers-syntax INDEX ($RELL_IDENTIFIERS_INDEX_URL 307 $RELL_IDENTIFIERS_INDEX_URL_SLASH 200 H1 $RELL_IDENTIFIERS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/zero-knowledge-proof/dapp/dapp-overview INDEX ($LEARN_ZK_DAPP_OVERVIEW_INDEX_URL 301 $LEARN_ZK_DAPP_OVERVIEW_INDEX_URL_SLASH 200 H1 $LEARN_ZK_DAPP_OVERVIEW_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/sign-transaction/structure INDEX ($LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_URL 301 $LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official RELL rell/language-features/expressions/operators INDEX ($RELL_EXPRESSIONS_OPERATORS_INDEX_URL 307 $RELL_EXPRESSIONS_OPERATORS_INDEX_URL_SLASH 200 H1 $RELL_EXPRESSIONS_OPERATORS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/web3-for-web2-devs/classic-web2-stack INDEX ($LEARN_WEB3_CLASSIC_STACK_INDEX_URL 301 $LEARN_WEB3_CLASSIC_STACK_INDEX_URL_SLASH 200 H1 $LEARN_WEB3_CLASSIC_STACK_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/system-queries INDEX ($RELL_SYSTEMLIB_QUERIES_INDEX_URL 307 $RELL_SYSTEMLIB_QUERIES_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_QUERIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/Dapp INDEX ($LEARN_TAGS_DAPP_INDEX_URL 301 $LEARN_TAGS_DAPP_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_DAPP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", PROJECT_STRUCTURE_URL)
        put("modules_docs", MODULES_URL)
        put("project_config", PROJECT_CONFIG_URL)
        put("tool", TOOL_NAME)
        put("configuration_index_url", CONFIGURATION_INDEX_URL)
        put("configuration_index_url_slash", CONFIGURATION_INDEX_URL_SLASH)
        put("configuration_index_title", CONFIGURATION_INDEX_TITLE)
        put("configuration_index_card_project_structure", CONFIGURATION_INDEX_CARD_PROJECT_STRUCTURE)
        put("configuration_index_card_project_settings", CONFIGURATION_INDEX_CARD_PROJECT_SETTINGS)
        put("configuration_index_card_properties", CONFIGURATION_INDEX_CARD_PROPERTIES)
        put("project_structure_index_docs", PROJECT_STRUCTURE_INDEX_URL)
        put("project_structure_index_url_slash", PROJECT_STRUCTURE_INDEX_URL_SLASH)
        put("project_structure_index_title", PROJECT_STRUCTURE_INDEX_TITLE)
        put("project_config_index_docs", PROJECT_CONFIG_INDEX_URL)
        put("project_config_index_url_slash", PROJECT_CONFIG_INDEX_URL_SLASH)
        put("project_config_index_title", PROJECT_CONFIG_INDEX_TITLE)
        put("get_started_about_index_docs", GET_STARTED_ABOUT_INDEX_URL)
        put("get_started_about_index_url_slash", GET_STARTED_ABOUT_INDEX_URL_SLASH)
        put("get_started_about_index_title", GET_STARTED_ABOUT_INDEX_TITLE)
        put("get_started_chain_governance_index_docs", GET_STARTED_CHAIN_GOVERNANCE_INDEX_URL)
        put("get_started_chain_governance_index_url_slash", GET_STARTED_CHAIN_GOVERNANCE_INDEX_URL_SLASH)
        put("get_started_chain_governance_index_title", GET_STARTED_CHAIN_GOVERNANCE_INDEX_TITLE)
        put("get_started_platform_architecture_index_docs", GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_URL)
        put("get_started_platform_architecture_index_url_slash", GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_URL_SLASH)
        put("get_started_platform_architecture_index_title", GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_TITLE)
        put("get_started_staking_index_docs", GET_STARTED_STAKING_INDEX_URL)
        put("get_started_staking_index_url_slash", GET_STARTED_STAKING_INDEX_URL_SLASH)
        put("get_started_staking_index_title", GET_STARTED_STAKING_INDEX_TITLE)
        put("create_rell_dapp_layout", createRellDappLayout())
        put("multi_file_layout", multiFileLayout())
        put("recommended_app_layout", recommendedAppLayout())
        put("single_file_module", singleFileModuleExample())
        put(
            "import_examples",
            buildJsonArray { importExamples.forEach { add(JsonPrimitive(it)) } }
        )
        put("entry_point_rule", "Use the module name (single-file name or folder name), not a file path. Example: module_a, not module_a/module.rell")
        put("ecosystem_pmc_install_index_url_slash", ECOSYSTEM_PMC_INSTALL_INDEX_URL_SLASH)
        put("ecosystem_pmc_install_index_title", ECOSYSTEM_PMC_INSTALL_INDEX_TITLE)
        put("reference_ft4_index_url_slash", REFERENCE_FT4_INDEX_URL_SLASH)
        put("reference_ft4_index_title", REFERENCE_FT4_INDEX_TITLE)
        put("ecosystem_gov_overview_index_url_slash", ECOSYSTEM_GOV_OVERVIEW_INDEX_URL_SLASH)
        put("ecosystem_gov_overview_index_title", ECOSYSTEM_GOV_OVERVIEW_INDEX_TITLE)
        put("learn_big_data_blockchain_components_index_url_slash", LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_URL_SLASH)
        put("learn_big_data_blockchain_components_index_title", LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_TITLE)
        put("learn_ft4_demo_intro_index_url_slash", LEARN_FT4_DEMO_INTRO_INDEX_URL_SLASH)
        put("learn_ft4_demo_intro_index_title", LEARN_FT4_DEMO_INTRO_INDEX_TITLE)
        put("learn_ci_deploy_index_url_slash", LEARN_CI_DEPLOY_INDEX_URL_SLASH)
        put("learn_ci_deploy_index_title", LEARN_CI_DEPLOY_INDEX_TITLE)
        put("learn_ft4_demo_account_reg_index_url_slash", LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_URL_SLASH)
        put("learn_ft4_demo_account_reg_index_title", LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_TITLE)
        put("learn_news_input_verification_index_url_slash", LEARN_NEWS_INPUT_VERIFICATION_INDEX_URL_SLASH)
        put("learn_news_input_verification_index_title", LEARN_NEWS_INPUT_VERIFICATION_INDEX_TITLE)
        put("rell_identifiers_index_url_slash", RELL_IDENTIFIERS_INDEX_URL_SLASH)
        put("rell_identifiers_index_title", RELL_IDENTIFIERS_INDEX_TITLE)
        put("learn_ttt_data_modeling_index_url_slash", LEARN_TTT_DATA_MODELING_INDEX_URL_SLASH)
        put("learn_ttt_data_modeling_index_title", LEARN_TTT_DATA_MODELING_INDEX_TITLE)
        put("learn_vector_db_pipeline_index_url_slash", LEARN_VECTOR_DB_PIPELINE_INDEX_URL_SLASH)
        put("learn_vector_db_pipeline_index_title", LEARN_VECTOR_DB_PIPELINE_INDEX_TITLE)
        put("learn_zk_dapp_overview_index_url_slash", LEARN_ZK_DAPP_OVERVIEW_INDEX_URL_SLASH)
        put("learn_zk_dapp_overview_index_title", LEARN_ZK_DAPP_OVERVIEW_INDEX_TITLE)
        put("learn_book_review_sign_tx_structure_index_url_slash", LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_URL_SLASH)
        put("learn_book_review_sign_tx_structure_index_title", LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_TITLE)
        put("rell_expressions_operators_index_url_slash", RELL_EXPRESSIONS_OPERATORS_INDEX_URL_SLASH)
        put("rell_expressions_operators_index_title", RELL_EXPRESSIONS_OPERATORS_INDEX_TITLE)
        put("learn_web3_classic_stack_index_url_slash", LEARN_WEB3_CLASSIC_STACK_INDEX_URL_SLASH)
        put("learn_web3_classic_stack_index_title", LEARN_WEB3_CLASSIC_STACK_INDEX_TITLE)
        put("rell_systemlib_queries_index_url_slash", RELL_SYSTEMLIB_QUERIES_INDEX_URL_SLASH)
        put("rell_systemlib_queries_index_title", RELL_SYSTEMLIB_QUERIES_INDEX_TITLE)
        put("learn_tags_dapp_index_url_slash", LEARN_TAGS_DAPP_INDEX_URL_SLASH)
        put("learn_tags_dapp_index_title", LEARN_TAGS_DAPP_INDEX_TITLE)
        put("notes", notes())
    }
}
// Official GET-STARTED get-started/about INDEX leftovers encoded as GET_STARTED_ABOUT_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture/chain-governance INDEX leftovers encoded as GET_STARTED_CHAIN_GOVERNANCE_INDEX_* (query-only).
// Official GET-STARTED get-started/about/architecture/platform-architecture INDEX leftovers encoded as GET_STARTED_PLATFORM_ARCHITECTURE_INDEX_* (query-only).
// Official GET-STARTED get-started/about/staking INDEX leftovers encoded as GET_STARTED_STAKING_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/pmc/pmccli-installation INDEX leftovers encoded as ECOSYSTEM_PMC_INSTALL_INDEX_* (query-only).
// Official REFERENCE reference/ft4 INDEX leftovers encoded as REFERENCE_FT4_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/governance/overview INDEX leftovers encoded as ECOSYSTEM_GOV_OVERVIEW_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/big-data/blockchain-side-description INDEX leftovers encoded as LEARN_BIG_DATA_BLOCKCHAIN_COMPONENTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/introduction INDEX leftovers encoded as LEARN_FT4_DEMO_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/continuous-integration-deploy INDEX leftovers encoded as LEARN_CI_DEPLOY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-frontend-application/account-regisration INDEX leftovers encoded as LEARN_FT4_DEMO_ACCOUNT_REG_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/input-verification INDEX leftovers encoded as LEARN_NEWS_INPUT_VERIFICATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/identifiers-syntax INDEX leftovers encoded as RELL_IDENTIFIERS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-one/data-modeling INDEX leftovers encoded as LEARN_TTT_DATA_MODELING_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/vector-db-movie-demo/data-pipeline INDEX leftovers encoded as LEARN_VECTOR_DB_PIPELINE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/zero-knowledge-proof/dapp/dapp-overview INDEX leftovers encoded as LEARN_ZK_DAPP_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/sign-transaction/structure INDEX leftovers encoded as LEARN_BOOK_REVIEW_SIGN_TX_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/expressions/operators INDEX leftovers encoded as RELL_EXPRESSIONS_OPERATORS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/web3-for-web2-devs/classic-web2-stack INDEX leftovers encoded as LEARN_WEB3_CLASSIC_STACK_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/system-queries INDEX leftovers encoded as RELL_SYSTEMLIB_QUERIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/Dapp INDEX leftovers encoded as LEARN_TAGS_DAPP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official BUILD configuration/project-config leftovers encoded as PROJECT_CONFIG_INDEX_* (query-only).
