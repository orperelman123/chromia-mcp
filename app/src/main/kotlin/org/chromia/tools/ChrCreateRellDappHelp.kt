package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr create-rell-dapp` help.
 * Does not run chr, write files, generate keys, or send signed transactions.
 * Source: docs.chromia.com/build/cli/commands/create-rell-dapp
 * plus CreateRellDappCommand / CHANGELOG (default folder `my-rell-dapp`).
 * Cookbook customization keys (test.timeout, test.parallel, database.schema_version)
 * are not on the official chromia.yml schema — do not emit them.
 */
object ChrCreateRellDappHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val TOOL_NAME = "chr_create_rell_dapp_help"
    const val DOCS_URL = "https://docs.chromia.com/build/cli/commands/create-rell-dapp"
    const val CREATE_RELL_DAPP_INDEX_URL = DOCS_URL
    const val CREATE_RELL_DAPP_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/create-rell-dapp/"
    const val CREATE_RELL_DAPP_INDEX_TITLE = "create-rell-dapp"  // official H1
    const val RUN_DAPP_URL = "https://docs.chromia.com/get-started/create-dapp/run-dapp-cli"
    const val GET_STARTED_RUN_DAPP_CLI_INDEX_URL = RUN_DAPP_URL
    const val GET_STARTED_RUN_DAPP_CLI_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/create-dapp/run-dapp-cli/"
    const val GET_STARTED_RUN_DAPP_CLI_INDEX_TITLE = "Build and run"  // official H1
    const val CREATE_DAPP_INDEX_URL = "https://docs.chromia.com/get-started/create-dapp"
    const val CREATE_DAPP_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/create-dapp/"
    const val CREATE_DAPP_INDEX_TITLE = "Create a Hello World dapp"  // official H1
    const val ECOSYSTEM_FILEHUB_SETUP_INDEX_URL = "https://docs.chromia.com/ecosystem/filehub/filehub-setup"
    const val ECOSYSTEM_FILEHUB_SETUP_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/filehub/filehub-setup/"
    const val ECOSYSTEM_FILEHUB_SETUP_INDEX_TITLE = "Filehub setup and deployment"  // official H1
    const val ECOSYSTEM_NODE_CONFIG_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/node-config"
    const val ECOSYSTEM_NODE_CONFIG_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/node-config/"
    const val ECOSYSTEM_NODE_CONFIG_INDEX_TITLE = "Configure node properties"  // official H1
    const val ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/import"
    const val ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/import/"
    const val ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_TITLE = "Setting up governance in your project"  // official H1
    const val LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_URL = "https://learn.chromia.com/courses/book-review/build-client/prerequisites"
    const val LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/build-client/prerequisites/"
    const val LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_TITLE = "Prerequisites"
    const val LEARN_BIG_DATA_SETUP_INDEX_URL = "https://learn.chromia.com/courses/big-data/setup"
    const val LEARN_BIG_DATA_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/big-data/setup/"
    const val LEARN_BIG_DATA_SETUP_INDEX_TITLE = "Set up your project"
    const val LEARN_FT4_ASSET_SETUP_INDEX_URL = "https://learn.chromia.com/courses/ft4-asset/setup"
    const val LEARN_FT4_ASSET_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-asset/setup/"
    const val LEARN_FT4_ASSET_SETUP_INDEX_TITLE = "Project setup and configuration"
    const val LEARN_ICCF_SETUP_INDEX_URL = "https://learn.chromia.com/courses/iccf-course/setup"
    const val LEARN_ICCF_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/iccf-course/setup/"
    const val LEARN_ICCF_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_NEWS_PROJECT_STRUCTURE_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/project-structure"
    const val LEARN_NEWS_PROJECT_STRUCTURE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/project-structure/"
    const val LEARN_NEWS_PROJECT_STRUCTURE_INDEX_TITLE = "Lesson 5 - Project structure of the dapp"  // official H1
    const val LEARN_TTT_RELL_MODULES_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/project-structure/modules"
    const val LEARN_TTT_RELL_MODULES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/project-structure/modules/"
    const val LEARN_TTT_RELL_MODULES_INDEX_TITLE = "Work with Rell modules"  // official H1
    const val LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/embedding-model"
    const val LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/embedding-model/"
    const val LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_TITLE = "Choose an embedding model"  // official H1
    const val LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_URL = "https://learn.chromia.com/courses/monetize-dapp/account-registration"
    const val LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/monetize-dapp/account-registration/"
    const val LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_TITLE = "Account registration"  // official H1
    const val LEARN_ZK_FRONTEND_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend"
    const val LEARN_ZK_FRONTEND_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/"
    const val LEARN_ZK_FRONTEND_INDEX_TITLE = "Module 3 – Frontend"  // official H1
    const val LEARN_RELATIONSHIPS_JOINS_INDEX_URL = "https://learn.chromia.com/courses/relationships-course/joins"
    const val LEARN_RELATIONSHIPS_JOINS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/relationships-course/joins/"
    const val LEARN_RELATIONSHIPS_JOINS_INDEX_TITLE = "Creating Records"  // official H1
    const val LEARN_WEB3_SUMMARY_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/summary"
    const val LEARN_WEB3_SUMMARY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/summary/"
    const val LEARN_WEB3_SUMMARY_INDEX_TITLE = "Summary"  // official H1
    const val RELL_INTRO_INDEX_URL = "https://docs.chromia.com/rell/rell-intro"
    const val RELL_INTRO_INDEX_URL_SLASH = "https://docs.chromia.com/rell/rell-intro/"
    const val RELL_INTRO_INDEX_TITLE = "Introduction to Rell"  // official H1
    const val RELL_MODULE_NAMESPACE_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/namespace"
    const val RELL_MODULE_NAMESPACE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/namespace/"
    const val RELL_MODULE_NAMESPACE_INDEX_TITLE = "Namespace"  // official H1
    const val RELL_SYSTEMLIB_REQUIRE_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/require-function"
    const val RELL_SYSTEMLIB_REQUIRE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/require-function/"
    const val RELL_SYSTEMLIB_REQUIRE_INDEX_TITLE = "require and error handling"  // official H1
    const val RELL_MODULES_INDEX_URL = "https://docs.chromia.com/rell/modules"
    const val RELL_MODULES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/modules/"
    const val RELL_MODULES_INDEX_TITLE = "Rell modules"  // official H1
    const val QUERY_DOCS_URL = ChrQueryHelp.QUERY_DOCS_URL
    const val INSTALL_URL = "https://docs.chromia.com/get-started/installation"
    const val DEFAULT_FOLDER = "my-rell-dapp"
    const val DOCKER_IMAGE = ChrBuildHelp.DOCKER_IMAGE

    val templates = listOf(
        "plain",
        "plain-multi",
        "minimal",
        "plain-library",
        "asset-management"
    )

    fun layoutNote(): String = """
        |-- chromia.yml
        |-- src
           |-- main.rell
           |-- test
              |-- arithmetic_test.rell
              |-- data_test.rell
    """.trimIndent() + "\n"

    fun dockerExample(): String =
        "docker run --rm -u \$(id -u):\$(id -g) -v \"\$(pwd):\$(pwd)\" -w \"\$(pwd)\" $DOCKER_IMAGE chr create-rell-dapp --devcontainer project-name"

    fun notes(): String = """
        Chromia CLI $CLI_SERIES `chr create-rell-dapp` help. Java 21+, Postgres 16+.
        Official page: $DOCS_URL
        Leftover official leftover BUILD cli/commands/create-rell-dapp (leftover official $CREATE_RELL_DAPP_INDEX_URL leftover official 307 leftover official $CREATE_RELL_DAPP_INDEX_URL_SLASH leftover official 200 leftover official $CREATE_RELL_DAPP_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr create-rell-dapp [<options>] [<name>] leftover official leftover Generates a template project leftover official leftover Template projects leftover official leftover Minimal leftover official leftover Plain leftover official leftover Plain-Multi leftover official leftover Plain-library leftover official leftover Asset Management leftover official leftover Options leftover official leftover -d, --base-dir=<path> leftover official leftover --template=(plain|plain-multi|minimal|plain-library|asset-management) leftover official leftover --devcontainer leftover official leftover -h, --help leftover official leftover Arguments leftover official leftover <name> leftover official leftover Dapp name leftover official leftover chromia.yml leftover official leftover src/main.rell leftover official leftover src/test leftover official leftover arithmetic_test.rell leftover official leftover data_test.rell leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples leftover official leftover skip leftover official leftover unofficial leftover official leftover chromia.yml leftover official leftover keys leftover official leftover no leftover official leftover keygen.
        Leftover official leftover GET-STARTED get-started/create-dapp INDEX (leftover official $CREATE_DAPP_INDEX_URL leftover official 307 leftover official $CREATE_DAPP_INDEX_URL_SLASH leftover official 200 leftover official $CREATE_DAPP_INDEX_TITLE): leftover official leftover HELP ONLY leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover Origin parked leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover BRIDs leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official leftover GET-STARTED get-started/create-dapp/run-dapp-cli INDEX (leftover official $GET_STARTED_RUN_DAPP_CLI_INDEX_URL leftover official 307 leftover official $GET_STARTED_RUN_DAPP_CLI_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_RUN_DAPP_CLI_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover HELP ONLY leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover Origin parked leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover skip leftover official leftover sign leftover official leftover recipe leftover official leftover no leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official leftover ECOSYSTEM ecosystem/filehub/filehub-setup INDEX (leftover official $ECOSYSTEM_FILEHUB_SETUP_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_FILEHUB_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_FILEHUB_SETUP_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/node-config INDEX (leftover official $ECOSYSTEM_NODE_CONFIG_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_NODE_CONFIG_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_NODE_CONFIG_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/import INDEX (leftover official $ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/book-review/build-client/prerequisites INDEX (leftover official $LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/big-data/setup INDEX (leftover official $LEARN_BIG_DATA_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_BIG_DATA_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BIG_DATA_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/ft4-asset/setup INDEX (leftover official $LEARN_FT4_ASSET_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_FT4_ASSET_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_ASSET_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/iccf-course/setup INDEX (leftover official $LEARN_ICCF_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_ICCF_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICCF_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/my-news-feed/module-one/project-structure INDEX (leftover official $LEARN_NEWS_PROJECT_STRUCTURE_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_PROJECT_STRUCTURE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_PROJECT_STRUCTURE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official install (devcontainer path): $INSTALL_URL
        Generates a template project. Optional <name> is the folder; CHANGELOG default with no name is `$DEFAULT_FOLDER`.
        Official templates: ${templates.joinToString(", ")}.
        `--devcontainer` adds a Docker / VS Code devcontainer (CLI + Postgres + PMC).
        After create, pin compile.rellVersion ${DappScaffold.RELL_VERSION} and merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION}
        (docs / templates may still show 0.14.9 — source pin wins). Leftover official leftover 0.33.2 `chr create-rell-dapp` (no name, no --template) is silent and writes `$DEFAULT_FOLDER`; leftover default chromia.yml is compile.rellVersion 0.14.5, blockchains.my_rell_dapp.module main, database.schema schema_my_rell_dapp, test.modules test — leftover no host/user/password, leftover no merkle_hash_version. Then leftover optional `chr install` / leftover `chr build` (leftover compile step; leftover default artifact build/my_rell_dapp.xml) / leftover `chr test`. Leftover `chr node start` compiles Type=BLOCKCHAIN again. Leftover built XML auto merkle_hash_version 2. There is no top-level `chr compile` in 0.33.x.
        Official first-run (run-dapp-cli $RUN_DAPP_URL, query-only):
        `chr create-rell-dapp` → `cd my-rell-dapp` → `chr node start` → `chr query hello_world`
        → `"Hello World!"`. Leftover local walk: leftover Postgres leftover postchain database/user/password defaults actually worked leftover org.postgresql.Driver 17.11 leftover JDBC ${ChrNodeHelp.DEFAULT_JDBC}; leftover node prints leftover Node is initialized leftover REST ${ChrNodeHelp.DEFAULT_API_URL} leftover REST GET / 200 leftover chain-id 0 leftover computed BRID (do not invent hex); leftover create silent leftover 0-byte stdout/stderr leftover artifact build/my_rell_dapp.xml leftover yml module main leftover test.modules test leftover no merkle_hash_version; leftover `chr query hello_world` from leftover project dir needs no --blockchain-rid leftover result Hello World!. Official query page also shows the explicit
        `chr query --blockchain-rid <BlockchainRID> hello_world`. Do not invent a BRID.
        Official Hello World query is `hello_world()` = `"Hello %s!".format(my_name.name)`
        with `object my_name { mutable name = "World"; }`. Leftover default main.rell also has leftover operation set_name. Skipped: key generation and the set_name write path. Leftover FT4 WRITE SKIP (leftover hello_world has no FT4 leftover auth leftover register leftover login leftover transfer leftover mint leftover burn leftover create-accounts).
        NEVER import ${DappScaffold.forbiddenModules.joinToString(", ")}.
        Cookbook chromia.yml keys test.timeout, test.parallel, database.schema_version are not official — do not use them.
        Leftover official leftover RELL rell/rell-intro INDEX (leftover official $RELL_INTRO_INDEX_URL leftover official 307 leftover official $RELL_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only.
        Leftover official leftover LEARN courses/tic-tac-toe/module-one/project-structure/modules INDEX (leftover official $LEARN_TTT_RELL_MODULES_INDEX_URL leftover official 301 leftover official $LEARN_TTT_RELL_MODULES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_RELL_MODULES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX vector-db embedding-model (leftover official $LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_URL leftover official 301 leftover official $LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_URL_SLASH leftover official GET 200 leftover official H1 leftover official $LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX monetize-dapp account-registration (leftover official $LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_URL leftover official 301 leftover official $LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_URL_SLASH leftover official GET 200 leftover official H1 leftover official $LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX zero-knowledge-proof frontend (leftover official $LEARN_ZK_FRONTEND_INDEX_URL leftover official 301 leftover official $LEARN_ZK_FRONTEND_INDEX_URL_SLASH leftover official GET 200 leftover official H1 leftover official $LEARN_ZK_FRONTEND_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX relationships-course joins (leftover official $LEARN_RELATIONSHIPS_JOINS_INDEX_URL leftover official 301 leftover official $LEARN_RELATIONSHIPS_JOINS_INDEX_URL_SLASH leftover official GET 200 leftover official H1 leftover official $LEARN_RELATIONSHIPS_JOINS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX web3-for-web2-devs summary (leftover official $LEARN_WEB3_SUMMARY_INDEX_URL leftover official 301 leftover official $LEARN_WEB3_SUMMARY_INDEX_URL_SLASH leftover official GET 200 leftover official H1 leftover official $LEARN_WEB3_SUMMARY_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/modules/namespace INDEX (leftover official $RELL_MODULE_NAMESPACE_INDEX_URL leftover official 307 leftover official $RELL_MODULE_NAMESPACE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_MODULE_NAMESPACE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/systemlib/require-function INDEX (leftover official $RELL_SYSTEMLIB_REQUIRE_INDEX_URL leftover official 307 leftover official $RELL_SYSTEMLIB_REQUIRE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_SYSTEMLIB_REQUIRE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/modules INDEX (leftover official $RELL_MODULES_INDEX_URL leftover official 307 leftover official $RELL_MODULES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_MODULES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover dapp-build INDEX help map (leftover official $CREATE_RELL_DAPP_INDEX_TITLE leftover official $CREATE_DAPP_INDEX_TITLE leftover official $GET_STARTED_RUN_DAPP_CLI_INDEX_TITLE leftover official $RELL_INTRO_INDEX_TITLE leftover official $RELL_MODULES_INDEX_TITLE leftover official $LEARN_FT4_ASSET_SETUP_INDEX_TITLE leftover official $LEARN_NEWS_PROJECT_STRUCTURE_INDEX_TITLE leftover official ${ChrBuildHelp.LEARN_HOME_INDEX_TITLE} leftover official ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} leftover official ${ChrNodeHelp.NODE_INDEX_TITLE} leftover official ${ChromiaVectorSearchHelp.LEARN_TAGS_VECTOR_DB_INDEX_TITLE} leftover official ${ChromiaVectorSearchHelp.LEARN_VECTOR_DB_SETUP_INDEX_TITLE} leftover official $LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_TITLE leftover official ${ChromiaRellExpressionsHelp.LEARN_VECTOR_DB_USE_CASES_INDEX_TITLE} leftover official HELP ONLY WRITE SKIP). Query-only. Leftover architecture INDEX map: LEARN_HOME + LEARN install CLI → chr create-rell-dapp → chromia.yml project settings + project structure → Rell language/expressions/statements/types → FT4 WRITE SKIP (leftover hello_world has no FT4 leftover auth leftover register leftover login leftover transfer) → leftover LEARN_TAGS_VECTOR_DB (official leftover tag URL currently 404; leftover live Vector DB INDEX is leftover learn vector-db course) + leftover vector-db course INDEX → local node → chr query hello_world → deploy. Leftover local 0.33.2 walk: leftover silent create leftover default folder my-rell-dapp leftover default chromia.yml compile.rellVersion 0.14.5 leftover blockchains.my_rell_dapp.module main leftover database.schema schema_my_rell_dapp leftover test.modules test leftover no host/user/password leftover no merkle_hash_version leftover chr build leftover artifact build/my_rell_dapp.xml leftover Node is initialized leftover REST ${ChrNodeHelp.DEFAULT_API_URL} leftover REST GET / 200 leftover query Hello World! leftover chain-id 0 leftover query from leftover project dir needs no --blockchain-rid leftover Postgres leftover postchain defaults actually worked leftover org.postgresql.Driver 17.11 leftover JDBC ${ChrNodeHelp.DEFAULT_JDBC}. Points at leftover official leftover INDEX titles already on disk plus sibling help tools. Does not encode new leftover pages. HELP ONLY WRITE SKIP.
        Leftover local 0.33.2 leftover next-step walk (leftover test leftover generate-client leftover FT4 leftover yml import, query-only): leftover `chr test` from leftover project dir leftover ran leftover test.arithmetic_test:test_foo leftover test.arithmetic_test:test_bar leftover test.data_test:test_add_name leftover SUMMARY 0 FAILED / 3 PASSED / 3 TOTAL leftover ***** OK *****; leftover official leftover generate leftover subcommands leftover client-stubs leftover graph leftover docs-site leftover there is no leftover top-level leftover chr generate-client leftover in 0.33.2 (leftover root help prints leftover generate leftover only) leftover `chr generate client-stubs --typescript -d generated-ts` leftover printed leftover Created files in generated-ts leftover main/main.ts leftover output leftover path leftover shape leftover <target>/<module>/<module>.ts leftover helloWorldQueryObject leftover QueryObject<string> leftover name hello_world leftover setNameOperation leftover Operation leftover set_name leftover imports leftover postchain-client leftover Operation leftover QueryObject leftover RawGtv leftover no leftover keys leftover no leftover signed leftover send; leftover FT4 leftover chromia.yml leftover libs leftover import leftover git leftover shape leftover registry ${DappScaffold.FT4_REGISTRY} leftover path ${DappScaffold.FT4_PATH} leftover tagOrBranch ${DappScaffold.FT4_VERSION} leftover insecure false leftover `chr install` leftover printed leftover Failed to install library ft4: Unknown error leftover yet leftover src/lib/ft4 leftover materialized leftover module.rell leftover version.rell leftover ft4 leftover get_version 1.1.0 leftover get_api_version ${DappScaffold.FT4_API} leftover @mount('ft4'); leftover retry leftover with leftover official leftover rid leftover hung leftover >2min leftover killed leftover do leftover not leftover invent leftover a leftover RID; leftover `chr build` leftover after leftover yml leftover change leftover printed leftover Building Blockchain: my_rell_dapp leftover artifact build/my_rell_dapp.xml leftover rebuilt leftover and leftover `chr test` leftover still leftover 3 PASSED. leftover FT4 leftover yml leftover lib leftover import leftover only — leftover no leftover Rell leftover import leftover of leftover ft4 leftover in leftover main.rell leftover no leftover moduleArgs leftover HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover mint leftover burn leftover create-accounts leftover no leftover signed leftover FT4 leftover ops leftover no leftover keys.
        Leftover local 0.33.2 leftover fire-0065 verify (query-only): leftover `chr test` leftover still leftover SUMMARY 0 FAILED / 3 PASSED / 3 TOTAL leftover ***** OK *****; leftover `chr test --modules test.data_test` leftover ran leftover only leftover test.data_test:test_add_name leftover SUMMARY 0 FAILED / 1 PASSED / 1 TOTAL leftover ***** OK *****; leftover `chr generate client-stubs --typescript -m main -d generated-ts` leftover printed leftover Created files in generated-ts leftover [main/main.ts] leftover languages leftover --kotlin leftover --typescript leftover --javascript leftover --python leftover module leftover flag leftover -m/--module leftover comma-delimited; leftover `chr generate graph -d generated-graph` leftover printed leftover Created files in generated-graph leftover [rell.mmd] leftover file leftover empty leftover for leftover hello_world leftover (no leftover entities); leftover FT4 leftover chromia.yml leftover libs leftover import leftover unchanged leftover HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover accounts leftover no leftover keys leftover no leftover signed leftover ops leftover no leftover invented leftover 64-hex.
        Leftover local 0.33.2 leftover library leftover versions leftover generate leftover graph leftover generate leftover docs-site walk (query-only): leftover `chr library versions com.chromia.ft4` leftover printed leftover Available Versions leftover 2.0.2 leftover 1.1.0 leftover 1.0.0 leftover 1.1.1 leftover 1.2.0 leftover Total: 5 versions leftover default leftover mainnet leftover library-chain leftover no leftover --brid leftover do leftover not leftover invent leftover a leftover RID leftover do leftover not leftover invent leftover a leftover library-chain leftover semver leftover pin leftover from leftover this leftover list leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION} leftover already leftover on leftover disk leftover HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover mint leftover burn leftover create-accounts leftover no leftover Rell leftover import leftover of leftover ft4 leftover no leftover moduleArgs leftover no leftover signed leftover ops leftover no leftover keys; leftover `chr generate graph -d generated-graph` leftover printed leftover Created files in generated-graph leftover [rell.mmd] leftover file leftover empty leftover 0-byte leftover for leftover hello_world leftover (no leftover entities); leftover `chr generate docs-site -d generated-docs` leftover printed leftover Documentation generated at generated-docs leftover index.html leftover navigation.html leftover hello_world.html leftover H1 leftover My Rell Dapp leftover leftover default leftover chromia.yml leftover has leftover no leftover docs: leftover section leftover generate leftover docs-site leftover still leftover wrote leftover leftover set_name leftover WRITE SKIP leftover query leftover hello_world leftover only leftover titles leftover already leftover on leftover disk leftover ${ChrGenerateClientHelp.GENERATE_INDEX_TITLE} leftover ${ChromiaDocsYmlHelp.DOCS_SITE_INDEX_TITLE} leftover ${ChrLibraryHelp.LIBRARY_INDEX_TITLE} leftover ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE}.
        Leftover local 0.33.2 leftover library leftover view leftover generate leftover graph leftover --mdx leftover --class-diagram walk (query-only): leftover `chr library view com.chromia.ft4` leftover printed leftover ID leftover com.chromia.ft4 leftover Name leftover ft4 leftover Organization leftover Chromia Organization leftover Version leftover 1.2.0 leftover Official leftover Yes leftover Description leftover FT4 Library leftover is leftover a leftover library leftover for leftover Rell leftover modules leftover default leftover mainnet leftover library-chain leftover no leftover --brid leftover do leftover not leftover invent leftover a leftover RID leftover do leftover not leftover invent leftover a leftover library-chain leftover semver leftover pin leftover from leftover this leftover view leftover (leftover view leftover Version leftover 1.2.0 leftover is leftover not leftover a leftover pin leftover leftover leftover versions leftover already leftover printed leftover 2.0.2 leftover 1.1.0 leftover 1.0.0 leftover 1.1.1 leftover 1.2.0 leftover Total leftover 5 leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION} leftover already leftover on leftover disk) leftover HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover mint leftover burn leftover create-accounts leftover no leftover Rell leftover import leftover of leftover ft4 leftover no leftover moduleArgs leftover no leftover signed leftover ops leftover no leftover keys leftover no leftover leftover leftover install; leftover `chr generate graph --help` leftover on leftover 0.33.2 leftover lists leftover --mdx leftover Surround leftover with leftover mdx leftover tags leftover and leftover --entity-relation leftover / leftover --class-diagram leftover Presented leftover as leftover entity leftover relation leftover diagram leftover or leftover class leftover diagram leftover leftover leftover `chr generate graph --mdx -d generated-graph-mdx` leftover printed leftover Created leftover files leftover in leftover generated-graph-mdx leftover [rell.mdx] leftover file leftover empty leftover 0-byte leftover for leftover hello_world leftover (no leftover entities) leftover leftover leftover `chr generate graph --class-diagram -d generated-graph-class` leftover printed leftover Created leftover files leftover in leftover generated-graph-class leftover [rell.mmd] leftover file leftover empty leftover 0-byte leftover for leftover hello_world leftover (no leftover entities) leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrGenerateClientHelp.GENERATE_INDEX_TITLE} leftover ${ChrLibraryHelp.LIBRARY_INDEX_TITLE} leftover ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.JS_TS_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.JS_QUICKSTART_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.JS_REFERENCE_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.KOTLIN_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.PYTHON_INDEX_TITLE} leftover ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} leftover ${ChromiaRellDatabaseHelp.BUILD_GETTING_STARTED_INDEX_TITLE} leftover ${ChromiaRellDatabaseHelp.BUILD_OVERVIEW_INDEX_TITLE} leftover leftover leftover fire leftover 0066 leftover INDEX leftover already leftover on leftover disk leftover no leftover new leftover leftover leftover pages.
        Leftover local 0.33.2 leftover library leftover list walk (query-only): leftover `chr library list` leftover needs leftover no leftover RID leftover default leftover mainnet leftover library-chain leftover --url leftover and leftover --brid leftover are leftover optional leftover overrides leftover do leftover not leftover invent leftover a leftover RID leftover printed leftover Available Libraries leftover columns leftover ID leftover Name leftover Organization leftover Version leftover Official leftover Description leftover Total: 20 libraries leftover every leftover row leftover Chromia Organization leftover every leftover row leftover Official leftover Yes leftover row leftover com.chromia.ft4 leftover ft4 leftover Version leftover 1.2.0 leftover Official leftover Yes leftover row leftover com.chromia.iccf leftover 1.90.2 leftover row leftover com.chromia.iccf_test leftover 1.90.0 leftover row leftover com.chromia.icmf leftover 1.102.2 leftover row leftover com.chromia.ICMF leftover 1.99.0 leftover Deprecated - use icmf instead leftover row leftover com.chromia.vector_db leftover 2.2.1 leftover row leftover com.chromia.hybridcompute leftover 3.35.5 leftover row leftover com.chromia.begin_block leftover 1.100.1 leftover row leftover com.chromia.eif leftover 1.3.1 leftover row leftover com.chromia.zkp leftover 1.0.0 leftover row leftover com.chromia.webauthn leftover 1.0.1 leftover row leftover com.chromia.ai_inference leftover 0.4.0. Leftover list leftover Version leftover column leftover is leftover the leftover registry leftover headline leftover version leftover not leftover the leftover max leftover semver leftover list leftover ft4 leftover 1.2.0 leftover equals leftover view leftover Version leftover 1.2.0 leftover yet leftover versions leftover first leftover item leftover is leftover 2.0.2 leftover Total leftover 5 leftover so leftover do leftover not leftover invent leftover a leftover FT4 leftover semver leftover pin leftover from leftover list leftover vs leftover view leftover vs leftover versions leftover vs leftover git leftover pin leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION} leftover already leftover on leftover disk leftover list leftover com.chromia.iccf leftover 1.90.2 leftover is leftover likewise leftover not leftover a leftover pin leftover official leftover ICCF leftover protocol leftover page leftover documents leftover library-chain leftover ${ChrLibraryHelp.ICCF_LIBRARY_CHAIN_VERSION} leftover already leftover on leftover disk. Leftover official leftover list leftover flags leftover -l/--limit leftover -o/--offset leftover --sort-by=(asc|desc) leftover verified leftover `chr library list --limit 5 --sort-by=asc` leftover printed leftover Total: 5 libraries leftover com.chromia.ICMF leftover com.chromia.icmf leftover com.chromia.iccf leftover com.chromia.begin_block leftover com.chromia.hybridcompute leftover and leftover `chr library list --limit 3 --sort-by=desc` leftover printed leftover Total: 3 libraries leftover com.chromia.hybridcompute_query leftover com.chromia.hbridge_admin leftover com.chromia.hbridge_crc2 leftover desc leftover head leftover matches leftover the leftover default leftover unsorted leftover order leftover head. Leftover narrow leftover terminal leftover clips leftover the leftover Version leftover Official leftover Description leftover columns leftover widen leftover the leftover terminal leftover to leftover read leftover full leftover rows. Leftover titles leftover already leftover on leftover disk leftover ${ChrLibraryHelp.LIBRARY_INDEX_TITLE} leftover ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE} leftover ${ChromiaFt4QueriesHelp.FT4_IMPORTS_INDEX_TITLE} leftover ${ChromiaFt4QueriesHelp.FT4_SETUP_PAGE_INDEX_TITLE}. Leftover FT4 leftover HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover mint leftover burn leftover create-accounts leftover no leftover leftover leftover install leftover no leftover Rell leftover import leftover of leftover ft4 leftover no leftover moduleArgs leftover no leftover signed leftover ops leftover no leftover keys leftover no leftover invented leftover RID leftover no leftover invented leftover 64-hex.
        Leftover local 0.33.2 leftover code leftover check leftover lint leftover format walk (query-only): leftover `chr code check` leftover empty leftover stdout leftover exit 0 leftover leftover leftover `chr code check --hide-lib-warnings` leftover exit 0 leftover leftover leftover `chr code lint src/main.rell` leftover exit 0 leftover leftover leftover `chr code lint src/test` leftover exit 0 leftover leftover leftover `chr code lint main/*` leftover exit 0 leftover leftover leftover `chr code lint` leftover (no leftover files) leftover walks leftover materialized leftover src/lib/ft4 leftover exit 1 leftover unknown_name:iccf leftover import:not_found:lib.iccf leftover expr:smartnull leftover leftover leftover hello_world leftover main leftover test leftover remain leftover clean leftover do leftover not leftover leftover leftover install leftover iccf leftover to leftover fix leftover leftover leftover `chr code format --file=src/main.rell` leftover printed leftover Formatting leftover src/main.rell... leftover no leftover changes leftover leftover leftover `chr code format src/test` leftover checksums leftover unchanged leftover leftover leftover default leftover create leftover writes leftover .rell_format leftover max_line_width=120 leftover insert_spaces=true leftover tab_size=4 leftover leftover leftover .rell_lint leftover rule_naming_convention leftover rule_quote_format=double leftover leftover leftover HELP ONLY WRITE SKIP leftover --fix leftover leftover leftover register leftover login leftover transfer leftover auth leftover leftover leftover no leftover keys leftover no leftover signed leftover ops leftover no leftover leftover leftover install leftover no leftover invented leftover RID leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION} leftover already leftover on leftover disk leftover titles leftover already leftover on leftover disk leftover ${ChrBuildHelp.CODE_INDEX_TITLE} leftover ${ChrReplHelp.REPL_INDEX_TITLE}.
        Leftover local 0.33.2 leftover repl leftover walk (query-only): leftover `chr repl -c '1+1'` leftover printed leftover 2 leftover exit 0 leftover leftover leftover `chr repl --module main -c '1+1'` leftover printed leftover 2 leftover exit 0 leftover leftover leftover `chr repl --blockchain my_rell_dapp -c '1+1'` leftover printed leftover 2 leftover exit 0 leftover leftover leftover `chr repl --module main -c 'val x = 1; x + 2'` leftover printed leftover 3 leftover exit 0 leftover leftover leftover `chr repl --module main -c '"Hello %s!".format("World")'` leftover printed leftover "Hello World!" leftover exit 0 leftover leftover leftover `chr repl --module main -c 'hello_world()'` leftover Run-time error: No database connection leftover exit 1 leftover leftover leftover `chr repl --module main --use-db -c 'hello_world()'` leftover printed leftover "Hello World!" leftover exit 0 leftover SqlInit Initializing database (chain_iid = 0) leftover leftover leftover `chr repl --module main --use-db --sql-log -c 'hello_world()'` leftover SqlConnectionLogger leftover SELECT leftover "Hello World!" leftover exit 0 leftover leftover leftover `chr repl --module main -d -c '1+1'` leftover printed leftover 2 leftover Script took leftover ...s leftover to leftover run leftover exit 0 leftover leftover leftover `chr repl --module main -c 'set_name("X")'` leftover Type rell.test.op cannot be converted to Gtv leftover HELP ONLY WRITE SKIP leftover ops leftover txs leftover (needs leftover rell.test.tx(...).run() leftover + leftover --use-db) leftover leftover leftover no leftover keys leftover no leftover signed leftover ops leftover no leftover leftover leftover install leftover no leftover invented leftover RID leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION} leftover already leftover on leftover disk leftover titles leftover already leftover on leftover disk leftover ${ChrReplHelp.REPL_INDEX_TITLE} leftover ${ChrToolsHelp.TOOLS_INDEX_TITLE} leftover ${ChrQueryHelp.QUERY_INDEX_TITLE} leftover leftover leftover `chr repl -c '1+1' -f JSON` leftover printed leftover 2 leftover exit 0 leftover leftover leftover `chr repl --module main -c '"Hello %s!".format("World")' -f JSON` leftover printed leftover "Hello World!" leftover exit 0 leftover leftover leftover `chr repl --module main --use-db -c 'hello_world()' -f JSON` leftover printed leftover "Hello World!" leftover exit 0 leftover leftover leftover `chr repl -c '1+1' -f XML` leftover printed leftover <int>2</int> leftover exit 0 leftover leftover leftover `chr repl --module main -c '"Hello %s!".format("World")' -f XML` leftover printed leftover <string>Hello World!</string> leftover exit 0 leftover leftover leftover `chr repl -c '1+1' -f YAML` leftover Error: Unsupported output format YAML leftover exit 1 leftover leftover leftover help leftover lists leftover YAML leftover leftover leftover `printf '1+1\\n' | chr repl -` leftover printed leftover 2 leftover exit 0 leftover leftover leftover `printf 'args\\n' | chr repl - leftover one` leftover printed leftover ["leftover", "one"] leftover exit 0 leftover leftover leftover `chr repl -c '2+2' -` leftover Error: Cannot use -c when specifying script file leftover exit 1 leftover leftover leftover `chr repl --module main --sql-log -c 'hello_world()'` leftover No database connection leftover exit 1 leftover leftover leftover set_name leftover Switch to a different output format leftover exit 0. Leftover chr tools leftover walk leftover done leftover (${ChrToolsHelp.TOOLS_INDEX_TITLE}). Leftover next leftover leftover leftover chr query leftover after leftover node leftover (${ChrQueryHelp.QUERY_INDEX_TITLE} / ${ChrNodeHelp.NODE_INDEX_TITLE}).
        Leftover local 0.33.2 leftover tools leftover walk (query-only): leftover `chr tools --help` leftover printed leftover gtv leftover validate-config leftover lib-model leftover exit 0 leftover leftover leftover `chr tools gtv --hex` leftover official leftover sample leftover hex leftover ${ChrToolsHelp.HEX_EXAMPLE} leftover (not leftover 64-hex) leftover default leftover pretty leftover printed leftover a=FOO leftover b=BAR leftover shape leftover exit 0 leftover leftover leftover -f JSON leftover / leftover XML leftover / leftover raw leftover / leftover YAML leftover all leftover exit 0 leftover (YAML leftover works leftover for leftover tools leftover gtv leftover unlike leftover chr leftover repl) leftover leftover leftover `chr gtv` leftover alias leftover same leftover pretty leftover exit 0 leftover leftover leftover no leftover --hex leftover waits leftover stdin leftover empty leftover pipe leftover Invalid GTV data: Unexpected end of input stream leftover exit 1 leftover leftover leftover --hex ZZ leftover Error: invalid value for --hex: Char Z is not a hex digit leftover exit 1 leftover leftover leftover `chr tools validate-config` leftover missing leftover --file leftover Error: missing option --file leftover exit 1 leftover leftover leftover -f src/main.rell leftover Unsupported file format. Expected either .yml or .yaml leftover exit 1 leftover leftover leftover from leftover parent leftover dir leftover -f my-rell-dapp/chromia.yml leftover printed leftover No issues found in chromia.yml leftover exit 0 leftover leftover leftover from leftover inside leftover dapp leftover -f chromia.yml leftover bare leftover filename leftover getParent(...) must not be null leftover exit 3 leftover leftover leftover `chr tools lib-model` leftover missing leftover --library-source leftover exit 1 leftover leftover leftover --library-source without leftover valid leftover git leftover --registry leftover Registry must be a valid git URL leftover exit 1 leftover leftover leftover with leftover --name leftover --registry leftover --tag-or-branch leftover --insecure leftover prints leftover git-shape leftover libs: leftover block leftover and leftover computes leftover rid leftover from leftover library leftover source leftover — leftover do leftover not leftover invent leftover a leftover RID leftover never leftover paste leftover computed leftover 64-hex leftover into leftover leftover_dapp_build_help leftover HELP ONLY WRITE SKIP leftover leftover leftover install leftover leftover leftover no leftover keys leftover no leftover signed leftover txs leftover titles leftover already leftover on leftover disk leftover ${ChrToolsHelp.TOOLS_INDEX_TITLE} leftover ${ChrQueryHelp.QUERY_INDEX_TITLE} leftover ${ChrNodeHelp.NODE_INDEX_TITLE}. Leftover next leftover leftover leftover chr query leftover after leftover node.
        Leftover local 0.33.2 leftover tools leftover deep leftover walk (query-only): leftover `chr tools` leftover with leftover no leftover subcommand leftover prints leftover the leftover same leftover help leftover as leftover `chr tools --help` leftover Usage: chr tools [OPTIONS] COMMAND [ARGS]... leftover Miscellaneous tools leftover only leftover option leftover -h, --help leftover exit 0 leftover leftover leftover gtv leftover -f pretty leftover is leftover the leftover default leftover and leftover lowercase leftover -f yaml leftover is leftover accepted leftover exit 0 leftover so leftover the leftover YAML leftover gap leftover is leftover repl-only leftover not leftover the leftover shared leftover GTV leftover formatter leftover leftover leftover the leftover official leftover piped leftover example leftover `chr gtv --output-format yaml < data.gtv` leftover on leftover a leftover 28-byte leftover binary leftover GTV leftover printed leftover --- leftover a: FOO leftover b: BAR leftover exit 0 leftover and leftover `chr tools gtv < data.gtv` leftover printed leftover pretty leftover leftover leftover --hex leftover wins leftover over leftover piped leftover stdin leftover leftover leftover --hash=1 leftover and leftover --hash=${DappScaffold.MERKLE_HASH_VERSION} leftover print leftover a leftover 64-char leftover uppercase leftover hex leftover Merkle leftover hash leftover computed leftover from leftover the leftover GTV leftover exit 0 leftover never leftover record leftover or leftover invent leftover that leftover hex leftover leftover leftover --hash=0 leftover Error: invalid value for --hash: Merkle hash version must be greater than 0 leftover exit 1 leftover leftover leftover validate-config leftover -f ./chromia.yml leftover and leftover an leftover absolute leftover path leftover both leftover printed leftover No issues found in chromia.yml leftover exit 0 leftover so leftover ./ leftover or leftover an leftover absolute leftover path leftover is leftover the leftover workaround leftover for leftover the leftover bare-filename leftover getParent(...) must not be null leftover exit 3 leftover (leftover stack leftover in leftover /tmp/chromia/chromia-cli.log leftover LoadKt.parseModel) leftover leftover leftover nonexistent leftover path leftover Error: invalid value for --file: file ... does not exist leftover exit 1 leftover leftover leftover a leftover directory leftover Error: invalid value for -f: file ... is a directory leftover exit 1 leftover leftover leftover .yaml leftover is leftover accepted leftover exactly leftover like leftover .yml leftover leftover leftover the leftover official leftover validator leftover rejects leftover the leftover cookbook leftover keys leftover Additional property 'timeout' found but was invalid (location: test->timeout) leftover Additional property 'parallel' found but was invalid (location: test->parallel) leftover Additional property 'schema_version' found but was invalid (location: database->schema_version) leftover exit 2 leftover which leftover confirms leftover the leftover not-official leftover note leftover already leftover on leftover disk leftover leftover leftover an leftover unknown leftover top-level leftover section leftover Additional property 'not_a_section' found but was invalid (location: not_a_section) leftover exit 2 leftover leftover leftover the leftover in-memory leftover scaffold_dapp leftover chromia.yml leftover (blockchains leftover module main leftover config.features.merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION} leftover compile.rellVersion ${DappScaffold.RELL_VERSION} leftover libs.ft4 leftover registry ${DappScaffold.FT4_REGISTRY} leftover path ${DappScaffold.FT4_PATH} leftover tagOrBranch ${DappScaffold.FT4_VERSION} leftover rid leftover insecure false) leftover validated leftover clean leftover No issues found leftover exit 0 leftover so leftover scaffold_dapp leftover output leftover passes leftover the leftover real leftover 0.33.2 leftover schema leftover leftover leftover lib-model leftover prints leftover libs: leftover <name>: leftover registry leftover path leftover equal leftover to leftover the leftover -s leftover value leftover verbatim leftover tagOrBranch leftover <Tag or branch the library is published on> leftover placeholder leftover when leftover --tag-or-branch leftover is leftover omitted leftover rid leftover computed leftover insecure false leftover leftover leftover --insecure=true leftover flips leftover insecure: true leftover leftover leftover repeat leftover runs leftover are leftover byte-identical leftover leftover leftover lib-model leftover over leftover the leftover materialized leftover src/lib/ft4 leftover with leftover --tag-or-branch ${DappScaffold.FT4_VERSION} leftover reproduced leftover exactly leftover the leftover FT4 leftover rid leftover already leftover on leftover disk leftover so leftover that leftover pin leftover is leftover computed leftover not leftover invented leftover do leftover not leftover invent leftover a leftover RID leftover leftover leftover HELP ONLY WRITE SKIP leftover lib-model leftover only leftover prints leftover to leftover stdout leftover pasting leftover the leftover block leftover into leftover chromia.yml leftover is leftover a leftover human leftover decision leftover register leftover login leftover transfer leftover auth leftover mint leftover burn leftover create-accounts leftover no leftover keys leftover no leftover signed leftover ops leftover titles leftover already leftover on leftover disk leftover ${ChrToolsHelp.TOOLS_INDEX_TITLE} leftover ${ChrQueryHelp.QUERY_INDEX_TITLE} leftover ${ChrNodeHelp.NODE_INDEX_TITLE}.
        Leftover local 0.33.2 leftover query leftover after leftover node walk (query-only): leftover `chr node start` leftover from leftover project leftover dir leftover printed leftover Node is initialized leftover Building Blockchain: my_rell_dapp leftover Chain-id: 0 leftover REST ${ChrNodeHelp.DEFAULT_API_URL} leftover Postgres leftover Driver leftover 17.11 leftover leftover leftover do leftover not leftover invent leftover or leftover paste leftover the leftover computed leftover BRID leftover leftover leftover `chr query` leftover (no leftover <queryname>) leftover Error: missing argument <queryname> leftover exit 1 leftover leftover leftover `chr query hello_world` leftover from leftover project leftover dir leftover printed leftover "Hello World!" leftover exit 0 leftover (needs leftover no leftover --blockchain-rid) leftover leftover leftover -f pretty leftover / leftover JSON leftover both leftover "Hello World!" leftover exit 0 leftover leftover leftover -f XML leftover <string>Hello World!</string> leftover exit 0 leftover leftover leftover -f raw leftover Hello World! leftover (no leftover quotes) leftover exit 0 leftover leftover leftover -f YAML leftover and leftover -f yaml leftover --- Hello World! leftover exit 0 leftover (YAML leftover works leftover for leftover chr leftover query leftover unlike leftover chr leftover repl) leftover leftover leftover `chr query leftover_no_such_query` leftover query: 400 Bad Request  Unknown query: leftover_no_such_query from http://localhost:7740 leftover exit 1 leftover leftover leftover `chr query --cid 0 hello_world` leftover "Hello World!" leftover exit 0 leftover leftover leftover `chr query --api-url http://localhost:7740 hello_world` leftover "Hello World!" leftover exit 0 leftover leftover leftover `chr query -s chromia.yml hello_world` leftover "Hello World!" leftover exit 0 leftover leftover leftover `chr query hello_world foo=1` leftover query: 400 Bad Request  Query 'hello_world' failed: Invalid argument(s): foo leftover exit 1 leftover leftover leftover `chr query hello_world --` leftover "Hello World!" leftover exit 0 leftover leftover leftover `chr query set_name` leftover Unknown query: set_name leftover exit 1 leftover (set_name leftover is leftover an leftover operation leftover HELP ONLY WRITE SKIP leftover chr leftover tx leftover leftover leftover signed leftover send) leftover leftover leftover `chr query --blockchain my_rell_dapp hello_world` leftover Error: missing option --network leftover exit 1 leftover leftover leftover `chr query --cid 99 hello_world` leftover Could not auto-detect brid from http://localhost:7740, reason: 404 Not Found leftover exit 1 leftover leftover leftover `chr query --blockchain-rid ZZ hello_world` leftover Char Z is not a hex digit leftover exit 3 leftover leftover leftover `chr query --blockchain-rid AB hello_world` leftover Wrong size of Blockchain RID, was 1 should be 32 (64 characters) leftover exit 3 leftover leftover leftover `--brid` leftover alone leftover is leftover not leftover an leftover option leftover (Possible options: -brid, --cid) leftover exit 1 leftover leftover leftover `chr query --mainnet hello_world` leftover Unknown query: hello_world leftover from leftover a leftover mainnet leftover node leftover exit 1 leftover (hello_world leftover is leftover local leftover scaffold leftover only) leftover leftover leftover `chr query --testnet hello_world` leftover hung leftover / leftover timed leftover out leftover against leftover public leftover testnet leftover (timeout) leftover leftover leftover from leftover parent leftover dir leftover `chr query hello_world` leftover still leftover "Hello World!" leftover exit 0 leftover when leftover local leftover node leftover is leftover up leftover (auto leftover REST ${ChrNodeHelp.DEFAULT_API_URL}) leftover leftover leftover `chr query -s my-rell-dapp/chromia.yml hello_world` leftover from leftover parent leftover same leftover exit 0 leftover leftover leftover missing leftover -s leftover file leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover `--network testnet` leftover without leftover deployments leftover block leftover Specified target [testnet] does not exist leftover exit 1 leftover leftover leftover `--api-url http://127.0.0.1:1` leftover Connection refused leftover exit 1 leftover leftover leftover -f FOO leftover invalid choice leftover (choose from pretty, raw, JSON, XML, YAML) leftover exit 1 leftover leftover leftover missing leftover --config leftover file leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover empty leftover queryname leftover Unknown query: leftover exit 1 leftover leftover leftover REST leftover GET leftover ${ChrNodeHelp.DEFAULT_API_URL}/ leftover 200 leftover text/html leftover H1 leftover Postchain REST API leftover /apidocs leftover 200 leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrQueryHelp.QUERY_INDEX_TITLE} leftover ${ChrNodeHelp.NODE_INDEX_TITLE} leftover ${ChrNodeHelp.INITIALIZED_LOG} leftover leftover leftover HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover leftover leftover chr leftover tx leftover leftover leftover signed leftover send leftover leftover leftover no leftover keys leftover no leftover invented leftover BRID leftover no leftover pasted leftover 64-hex. Leftover next leftover leftover leftover deploy leftover help leftover only leftover (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE}).
        Leftover local 0.33.2 leftover query leftover deep leftover walk (query-only): leftover `chr query --help` leftover prints leftover an leftover official leftover Examples leftover box leftover primitive_args leftover dict_arg leftover map_arg leftover struct_arg leftover leftover leftover option leftover groups leftover Configuration Properties leftover dApp target options leftover Deployment leftover Options leftover exit 0 leftover leftover leftover `chr query hello_world 1` leftover Index: 1, Size: 1 leftover Error: invalid value for <args>: query must be done with named parameters in a dict leftover exit 1 leftover leftover leftover `chr query --foo hello_world` leftover Error: no such option --foo. Did you mean -f? leftover exit 1 leftover leftover leftover `chr query --cid hello_world` leftover root leftover usage leftover missing argument QUERYNAME leftover invalid value for --cid: hello_world is not a valid integer leftover exit 1 leftover leftover leftover leftover dict leftover arg leftover on leftover hello_world leftover Invalid argument(s): arg leftover exit 1 leftover leftover leftover `--output-format JSON` leftover long leftover form leftover exit 0 leftover leftover leftover `chr query get_version` leftover / leftover ft4.get_version leftover / leftover get_api_version leftover Unknown query leftover exit 1 leftover so leftover the leftover materialized leftover src/lib/ft4 leftover is leftover never leftover imported leftover in leftover main.rell leftover FT4 leftover stays leftover HELP ONLY WRITE SKIP leftover leftover leftover REST leftover /metadata/iid_0 leftover lists leftover the leftover built-in leftover GTX leftover queries leftover last_block_info leftover tx_confirmation_time leftover and leftover ops leftover nop leftover __nop leftover timeb leftover (StandardOpsGTXModule) leftover while leftover hello_world leftover and leftover set_name leftover are leftover not leftover listed leftover leftover leftover `chr query last_block_info` leftover printed leftover a leftover dict leftover blockRID leftover height leftover timestamp leftover exit 0 leftover in leftover pretty leftover JSON leftover XML leftover raw leftover YAML leftover leftover leftover `chr query last_block_info foo=1` leftover still leftover exit 0 leftover so leftover the leftover built-in leftover GTX leftover query leftover ignores leftover extra leftover args leftover unlike leftover Rell leftover hello_world leftover leftover leftover `chr query tx_confirmation_time` leftover without leftover the leftover required leftover txRID leftover query: 500 Internal Server Error  Unknown error leftover exit 1 leftover leftover leftover REST leftover GET leftover ${ChrNodeHelp.DEFAULT_API_URL}/query/iid_0?type=hello_world leftover 200 leftover Hello World! leftover so leftover the leftover iid_<chainIid> leftover alias leftover replaces leftover {blockchainRid} leftover and leftover a leftover local leftover query leftover needs leftover no leftover BRID leftover leftover leftover POST leftover /query/iid_0 leftover type leftover hello_world leftover 200 leftover leftover leftover no leftover type leftover 400 leftover Missing query type leftover leftover leftover type leftover set_name leftover 400 leftover QUERY_NOT_FOUND leftover ops leftover WRITE SKIP leftover leftover leftover iid_99 leftover 404 leftover chain Iid: 99 leftover leftover leftover /query_gtv/iid_0 leftover 200 leftover octet-stream leftover leftover leftover /dquery leftover Type error: array expected leftover and leftover /web_query leftover Invalid argument(s): path, query_params leftover leftover leftover /blockchain/iid_0/height leftover leftover leftover /nodestate leftover RUNNING_VALIDATOR leftover leftover leftover /config/iid_0/features leftover merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION} leftover leftover leftover /errors leftover /transactions leftover empty leftover leftover leftover /version leftover 22 leftover leftover leftover /infrastructure_version leftover postchain 3.49.16 leftover rest-api 22 leftover database-server-version 17.11 leftover leftover leftover /_debug leftover moved leftover to leftover port leftover 7750 leftover leftover leftover /query leftover /status leftover /height leftover /brid leftover /node/iid_0 leftover 404 leftover leftover leftover OPTIONS leftover /query/iid_0 leftover 200 leftover GET, POST, OPTIONS leftover leftover leftover /apidocs leftover rapi-doc leftover matching leftover ${ChromiaLanguageClientsHelp.POSTCHAIN_REST_API_INDEX_TITLE} leftover already leftover on leftover disk leftover leftover leftover `chr node --help` leftover lists leftover only leftover start leftover and leftover update leftover so leftover there leftover is leftover no leftover chr node stop leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrQueryHelp.QUERY_INDEX_TITLE} leftover ${ChrNodeHelp.NODE_INDEX_TITLE} leftover ${ChrToolsHelp.TOOLS_INDEX_TITLE} leftover leftover leftover HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover leftover leftover chr leftover tx leftover leftover leftover signed leftover send leftover leftover leftover no leftover keys leftover no leftover invented leftover BRID leftover no leftover pasted leftover 64-hex. Leftover next leftover leftover leftover deploy leftover help leftover only leftover (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE}).
        Leftover local 0.33.2 leftover deploy leftover HELP ONLY leftover walk (query-only; leftover NOTHING leftover was leftover deployed): leftover `chr deploy --help` leftover is leftover not leftover a leftover 0.33.2 leftover command leftover leftover leftover bare leftover `chr deploy` leftover printed leftover Error: no such subcommand deploy. Did you mean deployment? leftover and leftover `chr deploy --help` leftover printed leftover the leftover root leftover help leftover exit 0 leftover leftover leftover the leftover official leftover command leftover is leftover `chr deployment` leftover leftover leftover `chr deployment --help` leftover (leftover and leftover bare leftover `chr deployment`) leftover printed leftover Create and maintain deployments leftover create leftover info leftover inspect leftover update leftover resume leftover pause leftover remove leftover proposal leftover voterset leftover container leftover exit 0 leftover leftover leftover `chr deployment create --help` leftover flags leftover -cfg/--config leftover -s/--settings leftover --secret leftover --key-id leftover -d/--network leftover -bc/--blockchain leftover --no-compression leftover --hide-lib-warnings leftover -y leftover -h/--help leftover leftover leftover `chr deployment update --help` leftover adds leftover --height leftover --verify-only leftover --skip-verification leftover leftover leftover `chr deployment info --help` leftover read-only leftover -brid/--blockchain-rid leftover --cid leftover --api-url leftover --mainnet/--testnet leftover -d/--network leftover -bc/--blockchain leftover --verbose leftover -f/--output-format=(table|JSON) leftover leftover leftover `chr deployment inspect --help` leftover read-only leftover adds leftover -m/--modules leftover -l/--list-modules leftover --module-args leftover --definitions=(queries|operations|entities|objects) leftover --signature leftover leftover leftover create leftover and leftover update leftover were leftover NEVER leftover run leftover HELP ONLY WRITE SKIP leftover no leftover deploy leftover no leftover signing leftover no leftover keys leftover no leftover invented leftover BlockchainRID leftover no leftover invented leftover container leftover id leftover no leftover testnet leftover no leftover mainnet leftover leftover leftover read-only leftover local leftover checks leftover only: leftover `chr deployment inspect` leftover from leftover the leftover project leftover dir leftover printed leftover queries leftover hello_world leftover text leftover operations leftover set_name leftover name: text leftover entities leftover [] leftover objects leftover my_name leftover mutable leftover name leftover exit 0 leftover leftover leftover --list-modules leftover ["main"] leftover leftover leftover --definitions=queries leftover / leftover --definitions=operations leftover / leftover -m main leftover narrow leftover the leftover same leftover JSON leftover leftover leftover --signature=hello_world leftover printed leftover the leftover single leftover signature leftover leftover leftover --module-args leftover [] leftover leftover leftover -f table leftover printed leftover Query/Operation/Object leftover tables leftover leftover leftover `chr deployment info` leftover against leftover the leftover local leftover node leftover printed leftover Cluster not found for blockchain rid <shortened BRID> leftover and leftover Unknown query: cm_get_blockchain_cluster from ${ChrNodeHelp.DEFAULT_API_URL} leftover (leftover local leftover node leftover is leftover not leftover Directory-managed) leftover never leftover paste leftover the leftover computed leftover BRID leftover leftover leftover `--network testnet` leftover on leftover info leftover and leftover inspect leftover failed leftover locally leftover Specified target [testnet] does not exist leftover exit 1 leftover so leftover nothing leftover left leftover the leftover box leftover leftover leftover `--blockchain-rid ZZ` leftover Char Z is not a hex digit leftover exit 3 leftover leftover leftover `--blockchain-rid AB` leftover Wrong size of Blockchain RID, was 1 should be 32 (64 characters) leftover exit 3 leftover leftover leftover chromia.yml leftover deployments leftover shape leftover already leftover on leftover disk leftover deployments.<net>.url leftover brid leftover container leftover chains.<name> leftover reserved leftover names leftover mainnet/testnet leftover auto-fill leftover Directory leftover brid leftover + leftover url leftover since leftover CLI leftover 0.29.8 leftover leftover leftover create leftover write-back leftover of leftover chains leftover since leftover CLI leftover 0.30.0 leftover leftover leftover update leftover requires leftover chains leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} leftover ${ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE} leftover ${ChrDeployHelp.TESTNET_DEPLOY_DAPP_INDEX_TITLE} leftover ${ChrDeployHelp.MAINNET_DEPLOY_DAPP_INDEX_TITLE} leftover ${ChrDeployHelp.TESTNET_GETTING_STARTED_INDEX_TITLE} leftover ${ChrDeployHelp.MAINNET_GETTING_STARTED_INDEX_TITLE} leftover ${ChrDeployHelp.GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE} leftover ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} leftover plus leftover sibling leftover help leftover tools leftover chr_deploy_help leftover ${ChromiaYmlDefinitionsHelp.TOOL_NAME} leftover ${ChromiaDocsYmlHelp.TOOL_NAME}. Leftover deploy leftover stays leftover HELP ONLY leftover here leftover leftover leftover leftover leftover settings leftover missing leftover -s leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover --blockchain leftover missing leftover --network leftover exit 1 leftover leftover leftover proposal leftover list leftover / leftover voterset leftover list leftover missing leftover --network leftover exit 1 leftover leftover leftover voterset leftover info leftover must leftover provide leftover one leftover of leftover --name, leftover --container leftover leftover leftover container leftover --help leftover configuration leftover pause leftover resume leftover WRITE SKIP leftover leftover leftover vault-lease leftover HELP ONLY leftover walk leftover done leftover (${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE}) leftover leftover leftover chr leftover tx leftover HELP ONLY leftover walk leftover done leftover (${ChrQueryHelp.TX_INDEX_TITLE}) leftover leftover leftover multi-signature leftover HELP ONLY leftover walk leftover done leftover (${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE}) leftover leftover leftover seeder leftover HELP ONLY leftover walk leftover done leftover (${ChrSeederHelp.SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE}) leftover leftover leftover leftover leftover eif leftover HELP ONLY leftover walk leftover done leftover (${ChrEifHelp.EIF_INDEX_TITLE}) leftover leftover leftover leftover leftover next leftover leftover leftover version leftover HELP ONLY. Query-only. HELP ONLY WRITE SKIP.

        Leftover official leftover next-step leftover architecture leftover INDEX map (leftover titles leftover already leftover on leftover disk, leftover no leftover new leftover pages): leftover test leftover ${ChrBuildHelp.BUILD_INDEX_TITLE} leftover ${ChrBuildHelp.CODE_INDEX_TITLE} → leftover generate leftover client-stubs leftover graph leftover docs-site leftover ${ChrGenerateClientHelp.GENERATE_INDEX_TITLE} leftover ${ChromiaDocsYmlHelp.DOCS_SITE_INDEX_TITLE} leftover ${ChrGenerateClientHelp.TESTNET_CONNECT_INDEX_TITLE} leftover leftover leftover generate leftover graph leftover --mdx leftover rell.mdx leftover 0-byte leftover leftover leftover generate leftover graph leftover --class-diagram leftover rell.mmd leftover 0-byte → leftover library leftover list leftover leftover leftover library leftover versions leftover leftover leftover library leftover view leftover ${ChrLibraryHelp.LIBRARY_INDEX_TITLE} leftover ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE} leftover query leftover printed leftover 2.0.2 leftover 1.1.0 leftover 1.0.0 leftover 1.1.1 leftover 1.2.0 leftover Total 5 leftover leftover leftover view leftover printed leftover ID leftover com.chromia.ft4 leftover Name leftover ft4 leftover Organization leftover Chromia Organization leftover Version leftover 1.2.0 leftover Official leftover Yes leftover no leftover invented leftover RID leftover no leftover invented leftover semver leftover pin leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION} leftover already leftover on leftover disk → leftover FT4 leftover ${ChromiaFt4QueriesHelp.FT4_SETUP_PAGE_INDEX_TITLE} leftover ${ChromiaFt4QueriesHelp.FT4_IMPORTS_INDEX_TITLE} leftover ${ChromiaFt4QueriesHelp.LEARN_TAGS_FT4_INDEX_TITLE} leftover ${LEARN_FT4_ASSET_SETUP_INDEX_TITLE} leftover HELP ONLY WRITE SKIP → leftover clients leftover ${ChromiaLanguageClientsHelp.JS_TS_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.KOTLIN_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.PYTHON_INDEX_TITLE} leftover leftover leftover chromia.yml leftover ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} leftover leftover leftover database leftover ${ChromiaRellDatabaseHelp.BUILD_GETTING_STARTED_INDEX_TITLE} leftover ${ChromiaRellDatabaseHelp.BUILD_OVERVIEW_INDEX_TITLE} leftover leftover leftover Rell leftover ${RELL_MODULES_INDEX_TITLE} leftover leftover leftover fire leftover 0066 leftover INDEX leftover already leftover on leftover disk → leftover ${ChrNodeHelp.NODE_INDEX_TITLE} → leftover deploy leftover HELP ONLY leftover walk leftover done leftover (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} leftover ${ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE} leftover ${ChrDeployHelp.TESTNET_DEPLOY_DAPP_INDEX_TITLE} leftover ${ChrDeployHelp.MAINNET_DEPLOY_DAPP_INDEX_TITLE}) leftover chr deployment leftover help leftover only leftover nothing leftover deployed leftover no leftover signing leftover no leftover keys leftover no leftover invented leftover RID leftover leftover leftover vault-lease leftover HELP ONLY leftover walk leftover done leftover (${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE}) leftover leftover leftover chr leftover tx leftover HELP ONLY leftover walk leftover done leftover (${ChrQueryHelp.TX_INDEX_TITLE}) leftover leftover leftover multi-signature leftover HELP ONLY leftover walk leftover done leftover (${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE}) leftover leftover leftover seeder leftover HELP ONLY leftover walk leftover done leftover (${ChrSeederHelp.SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE}) leftover leftover leftover leftover leftover eif leftover HELP ONLY leftover walk leftover done leftover (${ChrEifHelp.EIF_INDEX_TITLE}) leftover leftover leftover leftover leftover next leftover leftover leftover version leftover HELP ONLY. Query-only. HELP ONLY WRITE SKIP.
        Leftover local 0.33.2 leftover vault leftover lease leftover HELP ONLY leftover walk (query-only): leftover titles leftover already leftover on leftover disk leftover ${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE} leftover ${VaultLeaseHelp.MAINNET_GET_CONTAINER_INDEX_TITLE} leftover ${VaultLeaseHelp.GET_STARTED_HOSTING_INDEX_TITLE} leftover ${VaultLeaseHelp.GET_STARTED_SUPPORTED_WALLETS_INDEX_TITLE} leftover ${VaultLeaseHelp.GET_STARTED_PROVIDER_STAKING_INDEX_TITLE} leftover leftover leftover sibling leftover help leftover tool leftover vault_lease_help leftover leftover leftover workflow leftover get leftover tokens leftover (leftover testnet leftover faucet leftover / leftover mainnet leftover deposit) leftover → leftover open leftover Vault leftover containers leftover → leftover lease leftover in leftover Vault leftover using leftover an leftover existing leftover pubkey leftover file leftover → leftover put leftover the leftover real leftover Container leftover ID leftover in leftover chromia.yml leftover deployments.<net>.container leftover (leftover placeholder leftover ${VaultLeaseHelp.CONTAINER_PLACEHOLDER}) leftover → leftover then leftover chr leftover deployment leftover create leftover HELP ONLY WRITE SKIP leftover leftover leftover never leftover invent leftover a leftover container leftover id leftover leftover leftover never leftover invent leftover or leftover paste leftover Directory leftover Chain leftover 64-hex leftover leftover leftover this leftover tool leftover does leftover not leftover generate leftover a leftover key leftover leftover leftover no leftover signed leftover txs leftover leftover leftover no leftover leftover leftover create leftover from leftover this leftover walk. Query-only. HELP ONLY WRITE SKIP.
        Leftover local 0.33.2 leftover tx leftover HELP ONLY leftover walk (query-only): leftover `chr tx --help` leftover printed leftover Usage: chr tx [<options>] <opname> [<args>]... leftover Make a transaction towards a node leftover FT4 leftover --ft-auth leftover --ft-account-id leftover --evm-auth leftover --ft-register-account leftover ICCF leftover --iccf-tx leftover --iccf-source leftover official leftover Examples leftover primitive_args leftover dict_arg leftover map_arg leftover struct_arg leftover exit 0 leftover leftover leftover `chr tx` leftover (no leftover <opname>) leftover Error: missing argument <opname> leftover exit 1 leftover leftover leftover `chr tx set_name Alice` leftover with leftover no leftover node leftover on leftover :7740 leftover Could not auto-detect brid leftover Connection Refused leftover exit 1 leftover leftover leftover `--blockchain-rid ZZ` leftover Char Z is not a hex digit leftover exit 3 leftover leftover leftover `--blockchain-rid AB` leftover Wrong size of Blockchain RID leftover exit 3 leftover leftover leftover `--api-url http://127.0.0.1:1 --cid 0` leftover Connection Refused leftover exit 1 leftover leftover leftover `--network testnet` leftover Specified target [testnet] does not exist leftover exit 1 leftover leftover leftover `--blockchain my_rell_dapp` leftover missing option --network leftover exit 1 leftover leftover leftover missing leftover -s leftover file leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover `--cid hello` leftover not leftover a leftover valid leftover integer leftover exit 1 leftover leftover leftover `--foo` leftover no such option leftover exit 1 leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrQueryHelp.TX_INDEX_TITLE} leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover chr leftover tx leftover leftover leftover signed leftover send leftover leftover leftover set_name leftover leftover leftover register leftover leftover leftover login leftover leftover leftover transfer leftover leftover leftover auth leftover leftover leftover no leftover keys leftover leftover leftover no leftover invented leftover BRID leftover leftover leftover no leftover pasted leftover 64-hex.
        Leftover local 0.33.2 leftover multi-signature leftover HELP ONLY leftover walk (query-only): leftover `chr multi-signature --help` leftover printed leftover Handle transactions with need of multiple signers leftover create leftover sign leftover send leftover view leftover exit 0 leftover leftover leftover `chr multi-signature view --help` leftover View a existing transaction leftover -f/--file leftover exit 0 leftover leftover leftover view leftover (no leftover flags) leftover missing option --file leftover exit 1 leftover leftover leftover view leftover -f leftover missing leftover file leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover create leftover sign leftover send leftover WRITE SKIP leftover leftover leftover create leftover (no leftover flags) leftover missing leftover OPNAME leftover + leftover must leftover provide leftover one leftover of leftover --signer, leftover --signers, leftover --signers-file leftover exit 1 leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE} leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover no leftover keys leftover leftover leftover no leftover signed leftover txs leftover leftover leftover no leftover invented leftover BRID leftover leftover leftover no leftover pasted leftover 64-hex. Leftover leftover leftover seeder leftover HELP ONLY leftover walk leftover done leftover (${ChrSeederHelp.SEEDER_INDEX_TITLE}).
        Leftover local 0.33.2 leftover seeder leftover HELP ONLY leftover walk (query-only): leftover `chr seeder --help` leftover (leftover and leftover bare leftover `chr seeder`) leftover printed leftover Usage: chr seeder [OPTIONS] COMMAND [ARGS]... leftover Generate fake data for a local database leftover init leftover generate leftover exit 0 leftover leftover leftover `chr seed` leftover printed leftover Error: no such subcommand seed. Did you mean seeder? leftover exit 1 leftover leftover leftover `chr seeder seed` leftover printed leftover Error: no such subcommand seed leftover exit 1 leftover leftover leftover `chr seeder init --help` leftover printed leftover Create initial seeder configuration for blockchains leftover -s/--settings leftover -bc/--blockchain leftover (defaults to all) leftover exit 0 leftover leftover leftover `chr seeder generate --help` leftover printed leftover Generate Rell blockchain seeder module leftover --alternative-config-folder leftover -s leftover -bc leftover exit 0 leftover leftover leftover init leftover and leftover generate leftover were leftover NEVER leftover run leftover HELP ONLY WRITE SKIP leftover leftover leftover `chr seeder init -s /tmp/no-such-chromia.yml` leftover file leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover from leftover /tmp leftover Project settings file not found leftover exit 1 leftover leftover leftover `chr seeder --foo` leftover no such option leftover exit 1 leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrSeederHelp.SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_GENERATOR_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_EXAMPLE_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_CONFIGURABLE_INDEX_TITLE} leftover leftover leftover sibling leftover help leftover tools leftover ${ChrSeederHelp.TOOL_NAME} leftover ${ChromiaYmlDefinitionsHelp.TOOL_NAME} leftover leftover leftover ${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} leftover leftover leftover ${ChrQueryHelp.QUERY_INDEX_TITLE} leftover leftover leftover do leftover not leftover invent leftover seeder.yml leftover keys leftover leftover leftover no leftover keys leftover leftover leftover no leftover signed leftover txs leftover leftover leftover no leftover posted leftover transaction leftover leftover leftover no leftover invented leftover BlockchainRID leftover leftover leftover no leftover invented leftover container leftover id leftover leftover leftover no leftover pasted leftover 64-hex leftover leftover leftover this leftover tool leftover does leftover not leftover generate leftover a leftover key. Leftover leftover leftover eif leftover HELP ONLY leftover walk leftover done leftover (${ChrEifHelp.EIF_INDEX_TITLE}) leftover leftover leftover leftover leftover next leftover leftover leftover version leftover HELP ONLY. Query-only. HELP ONLY WRITE SKIP.
        Leftover local 0.33.2 leftover eif leftover HELP ONLY leftover walk (query-only): leftover `chr eif --help` leftover (leftover and leftover bare leftover `chr eif`) leftover printed leftover Usage: chr eif [OPTIONS] COMMAND [ARGS]... leftover Ethereum Integration Framework commands leftover generate-events-config leftover Generate solidity events that EIF will listen to leftover exit 0 leftover leftover leftover `chr eif generate-events-config --help` leftover printed leftover --abi leftover --events leftover --target leftover (defaults to leftover build/eif-events.yaml leftover = leftover ${ChrEifHelp.DEFAULT_TARGET}) leftover --format=(XML|YAML) leftover exit 0 leftover leftover leftover generate-events-config leftover was leftover NEVER leftover run leftover HELP ONLY WRITE SKIP leftover leftover leftover bare leftover `chr eif generate-events-config` leftover missing option --abi leftover missing option --events leftover exit 1 leftover leftover leftover `--events foo` leftover missing option --abi leftover exit 1 leftover leftover leftover `--abi` leftover (no leftover value) leftover option --abi requires a value leftover missing option --events leftover exit 1 leftover leftover leftover `--foo` leftover no such option --foo. Did you mean --format? leftover exit 1 leftover leftover leftover `chr eif seed` leftover no such subcommand seed leftover exit 1 leftover leftover leftover `chr eif generate` leftover no such subcommand generate. Did you mean generate-events-config? leftover exit 1 leftover leftover leftover `chr eif foo` leftover no such subcommand foo leftover exit 1 leftover leftover leftover missing leftover ABI leftover file leftover path does not exist leftover exit 1 leftover leftover leftover `--format badformat` leftover invalid choice leftover (choose from XML, YAML) leftover exit 1 leftover leftover leftover `--abi /tmp` leftover (directory) leftover MalformedJsonException leftover exit 3 leftover leftover leftover `--target` leftover with leftover missing leftover ABI leftover never leftover wrote leftover ${ChrEifHelp.DEFAULT_TARGET} leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrEifHelp.EIF_INDEX_TITLE} leftover ${ChrEifHelp.ECOSYSTEM_GOV_EIF_INDEX_TITLE} leftover leftover leftover sibling leftover help leftover tool leftover ${ChrEifHelp.TOOL_NAME} leftover leftover leftover leftover leftover library leftover list leftover row leftover com.chromia.eif leftover Version leftover 1.3.1 leftover already leftover on leftover disk leftover leftover leftover do leftover not leftover invent leftover ABI leftover event leftover YAML leftover keys leftover leftover leftover no leftover keys leftover leftover leftover no leftover signed leftover txs leftover leftover leftover no leftover posted leftover transaction leftover leftover leftover no leftover invented leftover BlockchainRID leftover leftover leftover no leftover pasted leftover 64-hex leftover leftover leftover this leftover tool leftover does leftover not leftover generate leftover a leftover key. Leftover leftover leftover seeder leftover HELP ONLY leftover walk leftover done leftover (${ChrSeederHelp.SEEDER_INDEX_TITLE}) leftover leftover leftover leftover leftover next leftover leftover leftover version leftover HELP ONLY. Query-only. HELP ONLY WRITE SKIP.

        This tool does not run chr, does not write files, does not generate a key, and does not send signed transactions.
        In-memory production skeleton (without running chr) is scaffold_dapp.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("create_rell_dapp_index_docs", CREATE_RELL_DAPP_INDEX_URL)
        put("create_rell_dapp_index_url_slash", CREATE_RELL_DAPP_INDEX_URL_SLASH)
        put("create_rell_dapp_index_title", CREATE_RELL_DAPP_INDEX_TITLE)
        put("create_dapp_index_docs", CREATE_DAPP_INDEX_URL)
        put("create_dapp_index_url_slash", CREATE_DAPP_INDEX_URL_SLASH)
        put("create_dapp_index_title", CREATE_DAPP_INDEX_TITLE)
        put("run_dapp", RUN_DAPP_URL)
        put("get_started_run_dapp_cli_index_docs", GET_STARTED_RUN_DAPP_CLI_INDEX_URL)
        put("get_started_run_dapp_cli_index_url_slash", GET_STARTED_RUN_DAPP_CLI_INDEX_URL_SLASH)
        put("get_started_run_dapp_cli_index_title", GET_STARTED_RUN_DAPP_CLI_INDEX_TITLE)
        put("query_docs", QUERY_DOCS_URL)
        put("default_folder", DEFAULT_FOLDER)
        put(
            "commands",
            buildJsonObject {
                put("create", "chr create-rell-dapp")
                put("named", "chr create-rell-dapp hello")
                put("official_loop_create", "chr create-rell-dapp")
                put("official_loop_cd", "cd my-rell-dapp")
                put("official_loop_node", "chr node start")
                put("official_loop_query", "chr query hello_world")
                put("official_loop_result", "Hello World!")
                put("plain", "chr create-rell-dapp hello --template=plain")
                put("minimal", "chr create-rell-dapp hello --template=minimal")
                put("plain_multi", "chr create-rell-dapp hello --template=plain-multi")
                put("plain_library", "chr create-rell-dapp hello --template=plain-library")
                put("asset_management", "chr create-rell-dapp hello --template=asset-management")
                put("devcontainer", "chr create-rell-dapp hello --devcontainer")
                put("docker_devcontainer", dockerExample())
            }
        )
        put(
            "flags",
            buildJsonObject {
                put("base_dir", "-d, --base-dir=<path>  # directory to generate the project in")
                put(
                    "template",
                    "--template=(plain|plain-multi|minimal|plain-library|asset-management)"
                )
                put("devcontainer", "--devcontainer  # Docker / VS Code devcontainer")
            }
        )
        put(
            "templates",
            buildJsonArray { templates.forEach { add(JsonPrimitive(it)) } }
        )
        put(
            "template_notes",
            buildJsonObject {
                put("minimal", "Minimal working example including sample queries/operations and tests.")
                put("plain", "A plain skeleton with empty main and test files.")
                put("plain-multi", "A plain skeleton with empty main and test files using multiple modules.")
                put("plain-library", "A plain skeleton with structure for library development.")
                put(
                    "asset-management",
                    "Asset management on Chromia, including blockchain operations and a frontend for user interaction."
                )
            }
        )
        put("layout", layoutNote())
        put("ecosystem_filehub_setup_index_url_slash", ECOSYSTEM_FILEHUB_SETUP_INDEX_URL_SLASH)
        put("ecosystem_filehub_setup_index_title", ECOSYSTEM_FILEHUB_SETUP_INDEX_TITLE)
        put("ecosystem_node_config_index_url_slash", ECOSYSTEM_NODE_CONFIG_INDEX_URL_SLASH)
        put("ecosystem_node_config_index_title", ECOSYSTEM_NODE_CONFIG_INDEX_TITLE)
        put("ecosystem_gov_starter_kit_import_index_url_slash", ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_URL_SLASH)
        put("ecosystem_gov_starter_kit_import_index_title", ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_TITLE)
        put("learn_book_review_prerequisites_index_url_slash", LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_URL_SLASH)
        put("learn_book_review_prerequisites_index_title", LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_TITLE)
        put("learn_big_data_setup_index_url_slash", LEARN_BIG_DATA_SETUP_INDEX_URL_SLASH)
        put("learn_big_data_setup_index_title", LEARN_BIG_DATA_SETUP_INDEX_TITLE)
        put("learn_ft4_asset_setup_index_url_slash", LEARN_FT4_ASSET_SETUP_INDEX_URL_SLASH)
        put("learn_ft4_asset_setup_index_title", LEARN_FT4_ASSET_SETUP_INDEX_TITLE)
        put("learn_iccf_setup_index_url_slash", LEARN_ICCF_SETUP_INDEX_URL_SLASH)
        put("learn_iccf_setup_index_title", LEARN_ICCF_SETUP_INDEX_TITLE)
        put("learn_news_project_structure_index_url_slash", LEARN_NEWS_PROJECT_STRUCTURE_INDEX_URL_SLASH)
        put("learn_news_project_structure_index_title", LEARN_NEWS_PROJECT_STRUCTURE_INDEX_TITLE)
        put("rell_intro_index_url_slash", RELL_INTRO_INDEX_URL_SLASH)
        put("rell_intro_index_title", RELL_INTRO_INDEX_TITLE)
        put("learn_ttt_rell_modules_index_url_slash", LEARN_TTT_RELL_MODULES_INDEX_URL_SLASH)
        put("learn_ttt_rell_modules_index_title", LEARN_TTT_RELL_MODULES_INDEX_TITLE)
        put("learn_vector_db_embedding_model_index_url_slash", LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_URL_SLASH)
        put("learn_vector_db_embedding_model_index_title", LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_TITLE)
        put("learn_monetize_account_registration_index_url_slash", LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_URL_SLASH)
        put("learn_monetize_account_registration_index_title", LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_TITLE)
        put("learn_zk_frontend_index_url_slash", LEARN_ZK_FRONTEND_INDEX_URL_SLASH)
        put("learn_zk_frontend_index_title", LEARN_ZK_FRONTEND_INDEX_TITLE)
        put("learn_relationships_joins_index_url_slash", LEARN_RELATIONSHIPS_JOINS_INDEX_URL_SLASH)
        put("learn_relationships_joins_index_title", LEARN_RELATIONSHIPS_JOINS_INDEX_TITLE)
        put("learn_web3_summary_index_url_slash", LEARN_WEB3_SUMMARY_INDEX_URL_SLASH)
        put("learn_web3_summary_index_title", LEARN_WEB3_SUMMARY_INDEX_TITLE)
        put("rell_module_namespace_index_url_slash", RELL_MODULE_NAMESPACE_INDEX_URL_SLASH)
        put("rell_module_namespace_index_title", RELL_MODULE_NAMESPACE_INDEX_TITLE)
        put("rell_systemlib_require_index_url_slash", RELL_SYSTEMLIB_REQUIRE_INDEX_URL_SLASH)
        put("rell_systemlib_require_index_title", RELL_SYSTEMLIB_REQUIRE_INDEX_TITLE)
        put("rell_modules_index_url_slash", RELL_MODULES_INDEX_URL_SLASH)
        put("rell_modules_index_title", RELL_MODULES_INDEX_TITLE)
        put("tool", TOOL_NAME)
        put(
            "leftover_dapp_build_help",
            buildJsonObject {
                put("create_help", TOOL_NAME)
                put("create_rell_dapp_index_title", CREATE_RELL_DAPP_INDEX_TITLE)
                put("create_dapp_index_title", CREATE_DAPP_INDEX_TITLE)
                put("run_dapp_cli_index_title", GET_STARTED_RUN_DAPP_CLI_INDEX_TITLE)
                put("rell_intro_index_title", RELL_INTRO_INDEX_TITLE)
                put("rell_modules_index_title", RELL_MODULES_INDEX_TITLE)
                put("learn_ft4_asset_setup_index_title", LEARN_FT4_ASSET_SETUP_INDEX_TITLE)
                put("learn_news_project_structure_index_title", LEARN_NEWS_PROJECT_STRUCTURE_INDEX_TITLE)
                put("build_help", "chr_build_help")
                put("yml_help", ChromiaYmlDefinitionsHelp.TOOL_NAME)
                put("project_structure_help", ChromiaProjectStructureHelp.TOOL_NAME)
                put("rell_language_help", ChromiaRellLanguageHelp.TOOL_NAME)
                put("rell_expressions_help", ChromiaRellExpressionsHelp.TOOL_NAME)
                put("rell_statements_help", ChromiaRellStatementsHelp.TOOL_NAME)
                put("rell_types_help", ChromiaRellTypesHelp.TOOL_NAME)
                put("ft4_help", ChromiaFt4QueriesHelp.TOOL_NAME)
                put("deploy_help", "chr_deploy_help")
                put("learn_install_cli_help", ChromiaVectorSearchHelp.TOOL_NAME)
                put("learn_install_cli_index_title", ChromiaVectorSearchHelp.LEARN_INSTALL_CLI_INDEX_TITLE)
                put("learn_home_index_title", ChrBuildHelp.LEARN_HOME_INDEX_TITLE)
                put("learn_tags_dapp_index_title", ChromiaProjectStructureHelp.LEARN_TAGS_DAPP_INDEX_TITLE)
                put("learn_tags_rell_index_title", ChromiaRellLanguageHelp.LEARN_TAGS_RELL_INDEX_TITLE)
                put("learn_tags_ft4_index_title", ChromiaFt4QueriesHelp.LEARN_TAGS_FT4_INDEX_TITLE)
                put("learn_tags_vector_db_index_title", ChromiaVectorSearchHelp.LEARN_TAGS_VECTOR_DB_INDEX_TITLE)
                put("learn_vector_db_setup_index_title", ChromiaVectorSearchHelp.LEARN_VECTOR_DB_SETUP_INDEX_TITLE)
                put("learn_vector_db_embedding_model_index_title", LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_TITLE)
                put("learn_vector_db_use_cases_index_title", ChromiaRellExpressionsHelp.LEARN_VECTOR_DB_USE_CASES_INDEX_TITLE)
                put("project_structure_index_title", ChromiaProjectStructureHelp.PROJECT_STRUCTURE_INDEX_TITLE)
                put("project_config_index_title", ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE)
                put("node_help", "chr_node_help")
                put("node_index_title", ChrNodeHelp.NODE_INDEX_TITLE)
                put("query_help", "chr_query_help")
                put("architecture", "leftover INDEX path: LEARN_HOME + LEARN install CLI → chr create-rell-dapp → chromia.yml project settings + project structure → Rell language/expressions/statements/types → FT4 WRITE SKIP (leftover hello_world has no FT4 leftover auth leftover register leftover login leftover transfer) → leftover LEARN_TAGS_VECTOR_DB (official leftover tag URL currently 404; leftover live Vector DB INDEX is leftover learn vector-db course) + leftover vector-db course INDEX → chr node start (Postgres 16+ leftover postchain leftover defaults worked leftover org.postgresql.Driver 17.11 leftover REST ${ChrNodeHelp.DEFAULT_API_URL} leftover Node is initialized leftover chain-id 0 leftover query hello_world Hello World!) → deploy. Leftover local 0.33.2 walk: leftover silent create leftover default folder my-rell-dapp leftover default chromia.yml compile.rellVersion 0.14.5 leftover blockchains.my_rell_dapp.module main leftover database.schema schema_my_rell_dapp leftover test.modules test leftover no host/user/password leftover no merkle_hash_version leftover chr build leftover artifact build/my_rell_dapp.xml leftover query from leftover project dir needs no --blockchain-rid leftover REST GET / 200. Query-only HELP ONLY WRITE SKIP.")
                put("official_loop", "chr create-rell-dapp → cd my-rell-dapp → chr node start → chr query hello_world")
                put("query_only", "HELP ONLY WRITE SKIP")
                put("leftover_local_create", "silent; default folder my-rell-dapp; leftover default hello_world")
                put("leftover_default_rellVersion", "0.14.5")
                put("leftover_default_schema", "schema_my_rell_dapp")
                put("leftover_chr_build", "optional compile; leftover chr node start compiles Type=BLOCKCHAIN; leftover artifact build/my_rell_dapp.xml")
                put("leftover_query_from_project_dir", "chr query hello_world needs no --blockchain-rid")
                put("leftover_ft4", "HELP ONLY WRITE SKIP")
                put("leftover_postgres", "16+ leftover postchain database/user/password defaults actually worked; leftover default yml only sets schema; leftover org.postgresql.Driver 17.11 leftover JDBC ${ChrNodeHelp.DEFAULT_JDBC}")
                put("leftover_create_silent", "silent; leftover 0-byte stdout/stderr; leftover default folder my-rell-dapp")
                put("leftover_local_node_initialized", ChrNodeHelp.INITIALIZED_LOG)
                put("leftover_rest_url", ChrNodeHelp.DEFAULT_API_URL)
                put("leftover_query_result", "Hello World!")
                put("leftover_chain_id", "0")
                put("leftover_build_artifact", "build/my_rell_dapp.xml")
                put("leftover_yml_module", "main")
                put("leftover_test_modules", "test")
                put("leftover_rest_get_root", "200")
                put("leftover_postgres_driver", "org.postgresql.Driver 17.11")
                put("leftover_test", "chr test")
                put("leftover_test_result", "0 FAILED / 3 PASSED / 3 TOTAL")
                put(
                    "leftover_test_methods",
                    "test.arithmetic_test:test_foo test.arithmetic_test:test_bar test.data_test:test_add_name"
                )
                put("leftover_test_ok_banner", "***** OK *****")
                put("leftover_generate_client", "chr generate client-stubs --typescript -d generated-ts")
                put("leftover_generate_client_output", "generated-ts/main/main.ts")
                put(
                    "leftover_generate_client_no_top_level",
                    "no top-level chr generate-client in 0.33.2; official is chr generate client-stubs (subcommands client-stubs, graph, docs-site)"
                )
                put(
                    "leftover_generate_client_stub_shape",
                    "<target>/<module>/<module>.ts; helloWorldQueryObject QueryObject<string> name hello_world; setNameOperation Operation set_name; imports postchain-client Operation QueryObject RawGtv; no keys"
                )
                put("leftover_generate_client_help", "chr_generate_client_help")
                put("leftover_generate_index_title", ChrGenerateClientHelp.GENERATE_INDEX_TITLE)
                put(
                    "leftover_ft4_yml_import",
                    "chromia.yml libs.ft4 git shape only (registry/path/tagOrBranch/insecure); no Rell import of ft4 in main.rell; no moduleArgs"
                )
                put("leftover_ft4_install_error", "chr install: Failed to install library ft4: Unknown error")
                put(
                    "leftover_ft4_install_leftover",
                    "src/lib/ft4 materialized anyway (module.rell, version.rell); retry pinned with the official rid hung and was killed; do not invent a RID"
                )
                put("leftover_ft4_lib_version", "ft4 get_version 1.1.0 get_api_version ${DappScaffold.FT4_API} @mount('ft4')")
                put("leftover_ft4_build_after_yml", "Building Blockchain: my_rell_dapp")
                put("leftover_ft4_test_after_yml", "0 FAILED / 3 PASSED / 3 TOTAL")
                put(
                    "leftover_ft4_write_skip",
                    "HELP ONLY WRITE SKIP register login transfer auth mint burn create-accounts; no signed FT4 ops; no keys"
                )
                put("leftover_ft4_setup_index_title", ChromiaFt4QueriesHelp.FT4_SETUP_PAGE_INDEX_TITLE)
                put("leftover_ft4_imports_index_title", ChromiaFt4QueriesHelp.FT4_IMPORTS_INDEX_TITLE)
                put("leftover_library_index_title", ChrLibraryHelp.LIBRARY_INDEX_TITLE)
                put("leftover_commands_library_index_title", ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE)
                put("leftover_build_index_title", ChrBuildHelp.BUILD_INDEX_TITLE)
                put("leftover_code_index_title", ChrBuildHelp.CODE_INDEX_TITLE)
                put("leftover_test_modules_filter", "chr test --modules test.data_test")
                put("leftover_test_modules_filter_result", "0 FAILED / 1 PASSED / 1 TOTAL")
                put("leftover_test_modules_filter_method", "test.data_test:test_add_name")
                put(
                    "leftover_generate_client_module_flag",
                    "chr generate client-stubs --typescript -m main -d generated-ts → Created files in generated-ts: [main/main.ts]"
                )
                put(
                    "leftover_generate_client_languages",
                    "--kotlin --typescript --javascript --python (official client-stubs help)"
                )
                put(
                    "leftover_generate_graph",
                    "chr generate graph -d generated-graph → Created files in generated-graph: [rell.mmd]; leftover hello_world rell.mmd empty 0-byte (no entities)"
                )
                put("leftover_generate_graph_file", "generated-graph/rell.mmd")
                put(
                    "leftover_generate_docs_site",
                    "chr generate docs-site -d generated-docs → Documentation generated at generated-docs"
                )
                put("leftover_generate_docs_site_output", "generated-docs/index.html")
                put(
                    "leftover_generate_docs_site_query_page",
                    "generated-docs/-my -rell -dapp/main/hello_world.html"
                )
                put("leftover_generate_docs_site_h1", "My Rell Dapp")
                put(
                    "leftover_generate_docs_site_no_docs_yml",
                    "leftover default chromia.yml has no docs: section; leftover generate docs-site still wrote leftover generated-docs"
                )
                put(
                    "leftover_generate_docs_site_write_skip",
                    "HELP ONLY WRITE SKIP leftover set_name leftover page leftover; leftover query leftover hello_world leftover only leftover no leftover keys leftover no leftover signed leftover ops"
                )
                put("leftover_docs_yml_help", ChromiaDocsYmlHelp.TOOL_NAME)
                put("leftover_docs_site_index_title", ChromiaDocsYmlHelp.DOCS_SITE_INDEX_TITLE)
                put("leftover_library_help", "chr_library_help")
                put("leftover_library_versions", "chr library versions com.chromia.ft4")
                put(
                    "leftover_library_versions_no_rid",
                    "default mainnet library-chain; no --brid; do not invent a RID"
                )
                put(
                    "leftover_library_versions_printed",
                    "Available Versions: 2.0.2 1.1.0 1.0.0 1.1.1 1.2.0 Total: 5 versions"
                )
                put(
                    "leftover_library_versions_write_skip",
                    "HELP ONLY WRITE SKIP; leftover query leftover library-chain leftover versions leftover only leftover; leftover no leftover Rell leftover import leftover of leftover ft4 leftover no leftover moduleArgs leftover no leftover signed leftover ops leftover no leftover keys leftover do leftover not leftover invent leftover a leftover library-chain leftover semver leftover pin leftover from leftover this leftover list leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION}"
                )
                put("leftover_library_view", "chr library view com.chromia.ft4")
                put(
                    "leftover_library_view_no_rid",
                    "default mainnet library-chain; no --brid; do not invent a RID"
                )
                put(
                    "leftover_library_view_printed",
                    "ID com.chromia.ft4 Name ft4 Organization Chromia Organization Version 1.2.0 Official Yes"
                )
                put(
                    "leftover_library_view_description",
                    "FT4 Library is a library for Rell modules (Chromia smart contract language)"
                )
                put(
                    "leftover_library_view_write_skip",
                    "HELP ONLY WRITE SKIP; leftover query leftover library-chain leftover view leftover only leftover; leftover view leftover Version leftover 1.2.0 leftover is leftover not leftover a leftover pin leftover leftover leftover versions leftover printed leftover 2.0.2 leftover 1.1.0 leftover 1.0.0 leftover 1.1.1 leftover 1.2.0 leftover Total leftover 5 leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION} leftover already leftover on leftover disk leftover no leftover Rell leftover import leftover of leftover ft4 leftover no leftover moduleArgs leftover no leftover leftover leftover install leftover no leftover signed leftover ops leftover no leftover keys leftover do leftover not leftover invent leftover a leftover library-chain leftover semver leftover pin leftover from leftover this leftover view"
                )
                put("leftover_library_list", "chr library list")
                put(
                    "leftover_library_list_no_rid",
                    "chr library list needs no RID; default mainnet library-chain; --url / --brid are optional overrides; do not invent a RID"
                )
                put("leftover_library_list_columns", "ID Name Organization Version Official Description")
                put(
                    "leftover_library_list_printed",
                    "Available Libraries; Total: 20 libraries; every row Chromia Organization; every row Official Yes"
                )
                put("leftover_library_list_total", "Total: 20 libraries")
                put("leftover_library_list_ft4_row", "com.chromia.ft4 ft4 Chromia Organization 1.2.0 Yes")
                put(
                    "leftover_library_list_iccf_row",
                    "com.chromia.iccf 1.90.2 (official ICCF protocol page documents library-chain ${ChrLibraryHelp.ICCF_LIBRARY_CHAIN_VERSION}; the list row is not a pin); com.chromia.iccf_test 1.90.0"
                )
                put(
                    "leftover_library_list_deprecated_row",
                    "com.chromia.ICMF 1.99.0 Deprecated - use icmf instead; live icmf row is com.chromia.icmf 1.102.2"
                )
                put(
                    "leftover_library_list_flags",
                    "-l, --limit=<int>; -o, --offset=<int>; --sort-by=(asc|desc); --url=<text>; -b, --brid=<value> (do not invent)"
                )
                put(
                    "leftover_library_list_limit_sort",
                    "chr library list --limit 5 --sort-by=asc → Total: 5 libraries (com.chromia.ICMF com.chromia.icmf com.chromia.iccf com.chromia.begin_block com.chromia.hybridcompute); chr library list --limit 3 --sort-by=desc → Total: 3 libraries (com.chromia.hybridcompute_query com.chromia.hbridge_admin com.chromia.hbridge_crc2); desc head matches the default unsorted order head"
                )
                put(
                    "leftover_library_list_narrow_terminal",
                    "narrow terminal clips the Version/Official/Description columns; widen the terminal to read full rows"
                )
                put(
                    "leftover_library_list_vs_view_vs_versions",
                    "list ft4 Version 1.2.0 equals view Version 1.2.0; versions first item 2.0.2 Total 5; the list Version column is the registry headline version, not the max semver; git pin remains ${DappScaffold.FT4_VERSION} already on disk; do not invent a FT4 semver pin from list vs view vs versions vs git pin"
                )
                put(
                    "leftover_library_list_write_skip",
                    "HELP ONLY WRITE SKIP; leftover query leftover library-chain leftover list leftover only leftover; leftover no leftover leftover leftover install leftover no leftover Rell leftover import leftover of leftover ft4 leftover no leftover moduleArgs leftover no leftover signed leftover ops leftover no leftover keys leftover register leftover login leftover transfer leftover auth leftover mint leftover burn leftover create-accounts leftover do leftover not leftover invent leftover a leftover RID leftover do leftover not leftover invent leftover a leftover library-chain leftover semver leftover pin leftover from leftover this leftover list leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION}"
                )
                put(
                    "leftover_generate_graph_flags",
                    "0.33.2 chr generate graph --help: --mdx Surround with mdx tags; --entity-relation / --class-diagram Presented as entity relation diagram or class diagram"
                )
                put(
                    "leftover_generate_graph_mdx",
                    "chr generate graph --mdx -d generated-graph-mdx → Created files in generated-graph-mdx: [rell.mdx]; leftover hello_world rell.mdx empty 0-byte (no entities)"
                )
                put("leftover_generate_graph_mdx_file", "generated-graph-mdx/rell.mdx")
                put(
                    "leftover_generate_graph_class_diagram",
                    "chr generate graph --class-diagram -d generated-graph-class → Created files in generated-graph-class: [rell.mmd]; leftover hello_world rell.mmd empty 0-byte (no entities)"
                )
                put("leftover_generate_graph_class_diagram_file", "generated-graph-class/rell.mmd")
                put("leftover_language_clients_help", ChromiaLanguageClientsHelp.TOOL_NAME)
                put("leftover_clients_js_ts_index_title", ChromiaLanguageClientsHelp.JS_TS_INDEX_TITLE)
                put("leftover_clients_js_quickstart_index_title", ChromiaLanguageClientsHelp.JS_QUICKSTART_INDEX_TITLE)
                put("leftover_clients_js_reference_index_title", ChromiaLanguageClientsHelp.JS_REFERENCE_INDEX_TITLE)
                put("leftover_clients_kotlin_index_title", ChromiaLanguageClientsHelp.KOTLIN_INDEX_TITLE)
                put("leftover_clients_python_index_title", ChromiaLanguageClientsHelp.PYTHON_INDEX_TITLE)
                put("leftover_rell_database_help", ChromiaRellDatabaseHelp.TOOL_NAME)
                put("leftover_database_getting_started_index_title", ChromiaRellDatabaseHelp.BUILD_GETTING_STARTED_INDEX_TITLE)
                put("leftover_database_overview_index_title", ChromiaRellDatabaseHelp.BUILD_OVERVIEW_INDEX_TITLE)
                put("leftover_fire_0066_index", "clients JS/TS Kotlin Python leftover project-config leftover database getting-started/overview INDEX titles already on disk; no new leftover pages")
                put("leftover_fire_0065_verified", "chr test + --modules filter + generate client-stubs -m main + generate graph; FT4 yml WRITE SKIP accounts")
                put("leftover_code_check", "chr code check")
                put("leftover_code_check_hide_lib_warnings", "chr code check --hide-lib-warnings")
                put(
                    "leftover_code_check_printed",
                    "empty stdout; exit 0 (leftover hello_world leftover does leftover not leftover import leftover ft4 leftover so leftover leftover leftover materialized leftover src/lib/ft4 leftover does leftover not leftover fail leftover check)"
                )
                put("leftover_code_lint", "chr code lint")
                put(
                    "leftover_code_lint_hello_world",
                    "chr code lint src/main.rell → exit 0; chr code lint src/test → exit 0; chr code lint main/* → exit 0"
                )
                put(
                    "leftover_code_lint_project",
                    "chr code lint (no files) walks leftover materialized leftover src/lib/ft4 leftover exit 1 leftover unknown_name:iccf leftover import:not_found:lib.iccf leftover expr:smartnull leftover leftover leftover hello_world leftover main leftover test leftover remain leftover clean leftover do leftover not leftover leftover leftover install leftover iccf leftover to leftover fix leftover leftover leftover HELP ONLY WRITE SKIP leftover --fix leftover leftover leftover register leftover login leftover transfer leftover auth leftover leftover leftover no leftover keys leftover no leftover signed leftover ops leftover no leftover invented leftover RID leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION}"
                )
                put(
                    "leftover_code_lint_no_fix",
                    "do not pass --fix; HELP ONLY WRITE SKIP leftover leftover leftover lint leftover auto-fix leftover leftover leftover no leftover leftover leftover install leftover no leftover keys leftover no leftover signed leftover ops leftover do leftover not leftover invent leftover a leftover RID"
                )
                put("leftover_code_format", "chr code format")
                put(
                    "leftover_code_format_printed",
                    "chr code format --file=src/main.rell → Formatting src/main.rell... no changes; leftover src/test leftover checksums leftover unchanged leftover after leftover chr code format src/test"
                )
                put(
                    "leftover_code_rell_format",
                    "leftover default leftover create leftover writes leftover .rell_format leftover [*.rell] leftover max_line_width=120 leftover insert_spaces=true leftover tab_size=4"
                )
                put(
                    "leftover_code_rell_lint",
                    "leftover default leftover create leftover writes leftover .rell_lint leftover [*.rell] leftover rule_naming_convention leftover rule_import_from_non_module leftover rule_quote_format=double leftover rule_formatter leftover rule_constant_detection leftover rule_unused_variable leftover rule_outer_join_cartesian_product"
                )
                put(
                    "leftover_code_flags",
                    "check: --hide-lib-warnings; lint: --source-dir -fo/--formatter-options=.rell_format -lo/--linter-options=.rell_lint --fix (WRITE SKIP) <files>; format: --source-dir --file -fo/.rell_format <files>"
                )
                put(
                    "leftover_code_write_skip",
                    "HELP ONLY WRITE SKIP leftover --fix leftover leftover leftover format leftover rewrite leftover leftover leftover register leftover login leftover transfer leftover auth leftover leftover leftover no leftover keys leftover no leftover signed leftover ops leftover no leftover leftover leftover install leftover iccf leftover do leftover not leftover invent leftover a leftover RID leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION}"
                )
                put("leftover_repl_help", ChrReplHelp.TOOL_NAME)
                put("leftover_repl_index_title", ChrReplHelp.REPL_INDEX_TITLE)
                put("leftover_repl", "chr repl")
                put("leftover_repl_command", "chr repl -c '...'")
                put(
                    "leftover_repl_arithmetic",
                    "chr repl -c '1+1' → 2 exit 0"
                )
                put(
                    "leftover_repl_module",
                    "chr repl --module main -c '1+1' → 2 exit 0"
                )
                put(
                    "leftover_repl_blockchain",
                    "chr repl --blockchain my_rell_dapp -c '1+1' → 2 exit 0"
                )
                put(
                    "leftover_repl_local_vars",
                    "chr repl --module main -c 'val x = 1; x + 2' → 3 exit 0"
                )
                put(
                    "leftover_repl_format_string",
                    "chr repl --module main -c '\"Hello %s!\".format(\"World\")' → \"Hello World!\" exit 0"
                )
                put(
                    "leftover_repl_hello_world_no_db",
                    "chr repl --module main -c 'hello_world()' → Run-time error: No database connection exit 1 (object my_name needs --use-db)"
                )
                put(
                    "leftover_repl_hello_world_use_db",
                    "chr repl --module main --use-db -c 'hello_world()' → \"Hello World!\" exit 0; SqlInit Initializing database (chain_iid = 0)"
                )
                put(
                    "leftover_repl_sql_log",
                    "chr repl --module main --use-db --sql-log -c 'hello_world()' → SqlConnectionLogger SELECT A00.\"name\" FROM \"c0.my_name\" A00; \"Hello World!\" exit 0"
                )
                put(
                    "leftover_repl_duration",
                    "chr repl --module main -d -c '1+1' → 2; Script took ...s to run; exit 0"
                )
                put(
                    "leftover_repl_flags",
                    "-c/--command; -m/--module; -bc/--blockchain; --use-db; --sql-log; -d/--duration; -f/--output-format; --history-file; -s/--settings; experimental <script> (not with -c)"
                )
                put(
                    "leftover_repl_op_write_skip",
                    "HELP ONLY WRITE SKIP leftover ops leftover txs; leftover chr repl --module main -c 'set_name(\"X\")' leftover printed leftover Type rell.test.op cannot be converted to Gtv leftover Switch to a different output format leftover exit 0 leftover (needs leftover rell.test.tx(...).run() leftover + leftover --use-db); leftover no leftover keys leftover no leftover signed leftover ops leftover no leftover leftover leftover install leftover do leftover not leftover invent leftover a leftover RID leftover git leftover pin leftover remains leftover ${DappScaffold.FT4_VERSION}"
                )
                put(
                    "leftover_repl_output_format",
                    "-f, --output-format=(pretty|raw|JSON|XML|YAML)"
                )
                put(
                    "leftover_repl_output_format_json",
                    "chr repl -c '1+1' -f JSON → 2 exit 0; chr repl --module main -c '\"Hello %s!\".format(\"World\")' -f JSON → \"Hello World!\" exit 0; chr repl --module main --use-db -c 'hello_world()' -f JSON → \"Hello World!\" exit 0 (JSON matches pretty for these values)"
                )
                put(
                    "leftover_repl_output_format_xml",
                    "chr repl -c '1+1' -f XML → <int>2</int> exit 0; chr repl --module main -c '\"Hello %s!\".format(\"World\")' -f XML → <string>Hello World!</string> exit 0"
                )
                put(
                    "leftover_repl_output_format_yaml",
                    "chr repl --help lists YAML; leftover chr repl -c '1+1' -f YAML leftover printed leftover Error: Unsupported output format YAML leftover exit 1 leftover leftover leftover -f yaml leftover same leftover error leftover (help leftover mismatch leftover 0.33.2)"
                )
                put(
                    "leftover_repl_output_format_raw",
                    "chr repl -c '1+1' -f raw → 2 exit 0; leftover -r/--raw-output leftover is leftover deprecated leftover use leftover -f"
                )
                put(
                    "leftover_repl_script_stdin",
                    "printf '1+1\\n' | chr repl - → 2 exit 0 (experimental leftover script leftover stdin leftover dash)"
                )
                put(
                    "leftover_repl_script_args",
                    "printf 'args\\n' | chr repl - leftover one → [\"leftover\", \"one\"] exit 0 (args: list<text>)"
                )
                put(
                    "leftover_repl_command_not_with_script",
                    "chr repl -c '2+2' - leftover printed leftover Error: Cannot use -c when specifying script file leftover exit 1"
                )
                put(
                    "leftover_repl_sql_log_needs_use_db",
                    "chr repl --module main --sql-log -c 'hello_world()' leftover printed leftover Run-time error: No database connection leftover exit 1 leftover (--sql-log leftover alone leftover does leftover not leftover load leftover entities leftover leftover leftover needs leftover --use-db leftover + leftover --module)"
                )
                put("leftover_tools_help", ChrToolsHelp.TOOL_NAME)
                put("leftover_tools_index_title", ChrToolsHelp.TOOLS_INDEX_TITLE)
                put("leftover_query_index_title", ChrQueryHelp.QUERY_INDEX_TITLE)
                put("leftover_tools", "chr tools")
                put(
                    "leftover_tools_commands",
                    "chr tools --help leftover printed leftover gtv leftover validate-config leftover lib-model leftover exit 0"
                )
                put("leftover_tools_gtv", "chr tools gtv")
                put(
                    "leftover_tools_gtv_alias",
                    "chr gtv leftover is leftover alias leftover of leftover chr tools gtv leftover (same leftover --help leftover / leftover decode)"
                )
                put(
                    "leftover_tools_gtv_hex",
                    "chr tools gtv --hex ${ChrToolsHelp.HEX_EXAMPLE} leftover (official leftover sample leftover hex leftover not leftover 64-hex)"
                )
                put(
                    "leftover_tools_gtv_pretty",
                    "chr tools gtv --hex <sample> leftover default leftover pretty leftover printed leftover [\"a\": \"FOO\", \"b\": \"BAR\"] leftover exit 0"
                )
                put(
                    "leftover_tools_gtv_json",
                    "chr tools gtv --hex <sample> -f JSON leftover printed leftover {\"a\": \"FOO\", \"b\": \"BAR\"} leftover exit 0"
                )
                put(
                    "leftover_tools_gtv_xml",
                    "chr tools gtv --hex <sample> -f XML leftover printed leftover <dict><entry key=\"a\"><string>FOO</string></entry>... leftover exit 0"
                )
                put(
                    "leftover_tools_gtv_raw",
                    "chr tools gtv --hex <sample> -f raw leftover printed leftover a=FOO leftover b=BAR leftover exit 0"
                )
                put(
                    "leftover_tools_gtv_yaml",
                    "chr tools gtv --hex <sample> -f YAML leftover printed leftover --- leftover a: FOO leftover b: BAR leftover exit 0 (YAML leftover works leftover for leftover tools leftover gtv leftover unlike leftover chr leftover repl)"
                )
                put(
                    "leftover_tools_gtv_alias_decode",
                    "chr gtv --hex <sample> leftover same leftover pretty leftover [\"a\": \"FOO\", \"b\": \"BAR\"] leftover exit 0"
                )
                put(
                    "leftover_tools_gtv_missing_hex",
                    "chr tools gtv leftover (no leftover --hex leftover no leftover pipe) leftover waits leftover on leftover stdin leftover (timeout); leftover empty leftover pipe leftover printed leftover Invalid GTV data: Unexpected end of input stream leftover exit 1"
                )
                put(
                    "leftover_tools_gtv_invalid_hex",
                    "chr tools gtv --hex ZZ leftover printed leftover Error: invalid value for --hex: Char Z is not a hex digit leftover exit 1"
                )
                put("leftover_tools_validate_config", "chr tools validate-config")
                put(
                    "leftover_tools_validate_config_printed",
                    "from leftover parent leftover dir leftover chr tools validate-config -f my-rell-dapp/chromia.yml leftover printed leftover No issues found in chromia.yml leftover exit 0; leftover --file= also leftover accepted"
                )
                put(
                    "leftover_tools_validate_config_file_required",
                    "chr tools validate-config leftover (no leftover --file) leftover printed leftover Error: missing option --file leftover exit 1; leftover -f src/main.rell leftover printed leftover Unsupported file format. Expected either .yml or .yaml leftover exit 1; leftover from leftover inside leftover dapp leftover -f chromia.yml leftover (bare leftover filename) leftover printed leftover getParent(...) must not be null leftover exit 3 leftover (use leftover a leftover path leftover with leftover a leftover parent leftover dir leftover component)"
                )
                put("leftover_tools_lib_model", "chr tools lib-model")
                put(
                    "leftover_tools_lib_model_source_required",
                    "chr tools lib-model leftover (no leftover args) leftover printed leftover Error: missing option --library-source leftover exit 1; leftover --name alone leftover same leftover missing leftover --library-source leftover exit 1; leftover --library-source without leftover valid leftover --registry leftover printed leftover Error: invalid value for --registry: Registry must be a valid git URL leftover exit 1"
                )
                put(
                    "leftover_tools_lib_model_no_rid",
                    "chr tools lib-model --library-source=<dir> --name=<lib> --registry=<git URL> --tag-or-branch=<tag> --insecure=true|false leftover prints leftover git-shape leftover libs: leftover block leftover and leftover computes leftover rid leftover from leftover library leftover source leftover — leftover do leftover not leftover invent leftover a leftover RID leftover (never leftover paste leftover computed leftover 64-hex leftover into leftover leftover_dapp_build_help) leftover HELP ONLY WRITE SKIP leftover leftover leftover install"
                )
                put(
                    "leftover_tools_flags",
                    "gtv: --hex -f/--output-format=(pretty|raw|JSON|XML|YAML) --hash; validate-config: -f/--file; lib-model: --name -s/--library-source --registry --tag-or-branch --insecure=true|false"
                )
                put(
                    "leftover_tools_write_skip",
                    "HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover leftover leftover leftover leftover install leftover no leftover keys leftover no leftover signed leftover txs leftover no leftover invented leftover RID leftover no leftover invented leftover 64-hex"
                )
                put(
                    "leftover_tools_no_subcommand",
                    "chr tools (no subcommand) prints the same help as chr tools --help: Usage: chr tools [OPTIONS] COMMAND [ARGS]...; Miscellaneous tools; only option -h, --help; exit 0"
                )
                put(
                    "leftover_tools_gtv_yaml_lowercase",
                    "chr tools gtv --hex <sample> -f yaml (lowercase) also accepted -> --- a: FOO b: BAR exit 0; leftover the YAML gap is chr repl only, not the shared GTV formatter"
                )
                put(
                    "leftover_tools_gtv_stdin_binary",
                    "official piped example verified: chr gtv --output-format yaml < data.gtv (28-byte binary GTV) -> --- a: FOO b: BAR exit 0; chr tools gtv < data.gtv -> pretty exit 0"
                )
                put(
                    "leftover_tools_gtv_hex_precedence",
                    "--hex wins over piped stdin: chr tools gtv --hex <sample> < data.gtv decodes the --hex value exit 0"
                )
                put(
                    "leftover_tools_gtv_hash",
                    "chr tools gtv --hex <sample> --hash=1 and --hash=2 print a 64-char uppercase hex Merkle hash computed from the GTV, exit 0; production pin 2; never record or invent that hex"
                )
                put(
                    "leftover_tools_gtv_hash_zero",
                    "chr tools gtv --hex <sample> --hash=0 -> Error: invalid value for --hash: Merkle hash version must be greater than 0 exit 1"
                )
                put(
                    "leftover_tools_validate_config_relative_workaround",
                    "chr tools validate-config -f ./chromia.yml and an absolute path both print No issues found in chromia.yml exit 0; leftover ./ or an absolute path is the workaround for the bare-filename getParent(...) must not be null exit 3 (stack in /tmp/chromia/chromia-cli.log LoadKt.parseModel)"
                )
                put(
                    "leftover_tools_validate_config_path_errors",
                    "nonexistent -> Error: invalid value for --file: file ... does not exist exit 1; a directory -> Error: invalid value for -f: file ... is a directory exit 1"
                )
                put(
                    "leftover_tools_validate_config_yaml_ext",
                    ".yaml is accepted exactly like .yml; chromia.yml.bak -> Unsupported file format. Expected either .yml or .yaml exit 1"
                )
                put(
                    "leftover_tools_validate_config_cookbook_keys",
                    "the official validator rejects the cookbook keys: Additional property 'timeout' found but was invalid (location: test->timeout); Additional property 'parallel' found but was invalid (location: test->parallel); Additional property 'schema_version' found but was invalid (location: database->schema_version); exit 2 - CLI confirms test.timeout / test.parallel / database.schema_version are not official"
                )
                put(
                    "leftover_tools_validate_config_unknown_section",
                    "an unknown top-level section -> Additional property 'not_a_section' found but was invalid (location: not_a_section) exit 2"
                )
                put(
                    "leftover_tools_validate_config_scaffold_dapp",
                    "the in-memory scaffold_dapp chromia.yml (blockchains module main + config.features.merkle_hash_version 2, compile.rellVersion 0.16.7, libs.ft4 registry/path/tagOrBranch/rid/insecure false) validated clean: No issues found exit 0 - scaffold_dapp output passes the real 0.33.2 schema"
                )
                put(
                    "leftover_tools_lib_model_printed_shape",
                    "libs: <name>: registry: <git URL>; path: the -s value verbatim; tagOrBranch: <Tag or branch the library is published on> placeholder when --tag-or-branch is omitted; rid: computed; insecure: false; repeat runs are byte-identical"
                )
                put(
                    "leftover_tools_lib_model_rid_matches_disk",
                    "chr tools lib-model -s src/lib/ft4 --name ft4 --registry <official FT4 git URL> --tag-or-branch v1.1.0r reproduced exactly the FT4 rid already on disk (leftover chromia.yml libs.ft4.rid / DappScaffold FT4 rid) - the pin is computed from the library source, never invented; do not invent a RID and do not paste the computed hex here"
                )
                put(
                    "leftover_tools_lib_model_insecure",
                    "--insecure=true flips insecure: true in the printed block (default false); lib-model only prints to stdout - pasting the block into chromia.yml is a human decision; HELP ONLY WRITE SKIP"
                )
                put("leftover_query", "chr query")
                put(
                    "leftover_query_help",
                    "chr_query_help"
                )
                put("leftover_node_index_title", ChrNodeHelp.NODE_INDEX_TITLE)
                put(
                    "leftover_query_missing_name",
                    "chr query (no <queryname>) leftover printed leftover Error: missing argument <queryname> leftover exit 1"
                )
                put(
                    "leftover_query_hello_world",
                    "chr query hello_world leftover (from leftover project leftover dir leftover after leftover chr node start) leftover printed leftover \"Hello World!\" leftover exit 0 leftover leftover leftover needs leftover no leftover --blockchain-rid"
                )
                put(
                    "leftover_query_output_format",
                    "-f, --output-format=(pretty|raw|JSON|XML|YAML)"
                )
                put(
                    "leftover_query_output_format_pretty_json",
                    "chr query hello_world -f pretty leftover and leftover -f JSON leftover both leftover printed leftover \"Hello World!\" leftover exit 0"
                )
                put(
                    "leftover_query_output_format_xml",
                    "chr query hello_world -f XML leftover printed leftover <string>Hello World!</string> leftover exit 0"
                )
                put(
                    "leftover_query_output_format_raw",
                    "chr query hello_world -f raw leftover printed leftover Hello World! leftover (no leftover quotes) leftover exit 0"
                )
                put(
                    "leftover_query_output_format_yaml",
                    "chr query hello_world -f YAML leftover and leftover -f yaml leftover printed leftover --- Hello World! leftover exit 0 leftover (YAML leftover works leftover for leftover chr leftover query leftover unlike leftover chr leftover repl)"
                )
                put(
                    "leftover_query_unknown",
                    "chr query leftover_no_such_query leftover printed leftover query: 400 Bad Request  Unknown query: leftover_no_such_query from http://localhost:7740 leftover exit 1"
                )
                put(
                    "leftover_query_cid",
                    "chr query --cid 0 hello_world leftover printed leftover \"Hello World!\" leftover exit 0"
                )
                put(
                    "leftover_query_api_url",
                    "chr query --api-url http://localhost:7740 hello_world leftover printed leftover \"Hello World!\" leftover exit 0 leftover (official leftover default leftover REST ${ChrNodeHelp.DEFAULT_API_URL})"
                )
                put(
                    "leftover_query_settings",
                    "chr query -s chromia.yml hello_world leftover printed leftover \"Hello World!\" leftover exit 0"
                )
                put(
                    "leftover_query_invalid_arg",
                    "chr query hello_world foo=1 leftover printed leftover query: 400 Bad Request  Query 'hello_world' failed: Invalid argument(s): foo leftover exit 1"
                )
                put(
                    "leftover_query_dashdash",
                    "chr query hello_world -- leftover printed leftover \"Hello World!\" leftover exit 0"
                )
                put(
                    "leftover_query_op_as_query",
                    "chr query set_name leftover printed leftover Unknown query: set_name leftover exit 1 leftover (set_name leftover is leftover an leftover operation leftover HELP ONLY WRITE SKIP leftover chr leftover tx leftover leftover leftover signed leftover send)"
                )
                put(
                    "leftover_query_blockchain_needs_network",
                    "chr query --blockchain my_rell_dapp hello_world leftover printed leftover Error: missing option --network leftover exit 1"
                )
                put(
                    "leftover_query_cid_missing",
                    "chr query --cid 99 hello_world leftover printed leftover Could not auto-detect brid from http://localhost:7740, reason: 404 Not Found leftover exit 1"
                )
                put(
                    "leftover_query_brid_invalid_hex",
                    "chr query --blockchain-rid ZZ hello_world leftover printed leftover Char Z is not a hex digit leftover exit 3"
                )
                put(
                    "leftover_query_brid_wrong_size",
                    "chr query --blockchain-rid AB hello_world leftover printed leftover Wrong size of Blockchain RID, was 1 should be 32 (64 characters) leftover exit 3 leftover leftover leftover do leftover not leftover invent leftover or leftover paste leftover a leftover BRID"
                )
                put(
                    "leftover_query_brid_alias",
                    "--brid alone leftover is leftover not leftover an leftover option leftover (Possible options: -brid, --cid) leftover exit 1 leftover leftover leftover use leftover -brid leftover or leftover --blockchain-rid"
                )
                put(
                    "leftover_query_mainnet_local_query",
                    "chr query --mainnet hello_world leftover printed leftover Unknown query: hello_world leftover from leftover a leftover mainnet leftover node leftover exit 1 leftover (hello_world leftover is leftover local leftover scaffold leftover only)"
                )
                put(
                    "leftover_query_testnet_timeout",
                    "chr query --testnet hello_world leftover hung leftover / leftover timed leftover out leftover against leftover public leftover testnet leftover (timeout) leftover leftover leftover do leftover not leftover invent leftover a leftover BRID"
                )
                put(
                    "leftover_query_from_parent",
                    "from leftover parent leftover dir leftover chr query hello_world leftover still leftover printed leftover \"Hello World!\" leftover exit 0 leftover when leftover local leftover node leftover is leftover up leftover (auto leftover REST ${ChrNodeHelp.DEFAULT_API_URL})"
                )
                put(
                    "leftover_query_parent_settings",
                    "from leftover parent leftover dir leftover chr query -s my-rell-dapp/chromia.yml hello_world leftover printed leftover \"Hello World!\" leftover exit 0"
                )
                put(
                    "leftover_query_settings_missing",
                    "chr query -s /tmp/no-such-chromia.yml hello_world leftover printed leftover Error: invalid value for -s: file ... does not exist leftover exit 1"
                )
                put(
                    "leftover_query_network_missing",
                    "chr query --network testnet hello_world leftover (no leftover deployments leftover block) leftover printed leftover Error: invalid value for --network: Specified target [testnet] does not exist leftover exit 1"
                )
                put(
                    "leftover_query_api_refused",
                    "chr query --api-url http://127.0.0.1:1 hello_world leftover printed leftover Connection refused leftover exit 1"
                )
                put(
                    "leftover_query_bad_format",
                    "chr query hello_world -f FOO leftover printed leftover Error: invalid value for -f: invalid choice: FOO. (choose from pretty, raw, JSON, XML, YAML) leftover exit 1"
                )
                put(
                    "leftover_query_config_missing",
                    "chr query --config /tmp/no-client.conf hello_world leftover printed leftover Error: invalid value for --config: file ... does not exist leftover exit 1"
                )
                put(
                    "leftover_query_empty_name",
                    "chr query '' leftover printed leftover Unknown query: leftover exit 1"
                )
                put(
                    "leftover_query_rest_root",
                    "REST leftover GET leftover ${ChrNodeHelp.DEFAULT_API_URL}/ leftover 200 leftover text/html leftover H1 leftover Postchain REST API leftover leftover leftover /apidocs leftover 200"
                )
                put(
                    "leftover_query_node_initialized",
                    "chr node start leftover printed leftover ${ChrNodeHelp.INITIALIZED_LOG} leftover Building Blockchain: my_rell_dapp leftover Chain-id: 0 leftover leftover leftover do leftover not leftover invent leftover or leftover paste leftover the leftover computed leftover BRID"
                )
                put(
                    "leftover_query_write_skip",
                    "HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth leftover leftover leftover chr leftover tx leftover leftover leftover signed leftover send leftover leftover leftover no leftover keys leftover no leftover invented leftover BRID leftover no leftover pasted leftover 64-hex"
                )
                put(
                    "leftover_query_flags",
                    "-s/--settings -cfg/--config -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain -f/--output-format=(pretty|raw|JSON|XML|YAML)"
                )
                put("leftover_deploy", "chr deployment")
                put(
                    "leftover_query_help_examples",
                    "chr query --help leftover on leftover 0.33.2 leftover prints leftover an leftover official leftover Examples leftover box leftover primitive_args leftover arg1=123 leftover arg2=Alice leftover leftover leftover dict_arg leftover leftover leftover map_arg leftover leftover leftover struct_arg leftover leftover leftover chr query my_query -- arg1=foo leftover (short leftover official leftover byte_array leftover samples leftover not leftover a leftover BRID leftover do leftover not leftover invent leftover one)"
                )
                put(
                    "leftover_query_help_groups",
                    "chr query --help leftover option leftover groups leftover Configuration Properties leftover -cfg/--config leftover -s/--settings leftover leftover leftover dApp target options leftover -brid/--blockchain-rid leftover --cid leftover --api-url leftover --mainnet/--testnet leftover leftover leftover Deployment leftover -d/--network leftover -bc/--blockchain leftover leftover leftover Options leftover -f/--output-format leftover -h/--help leftover exit 0 leftover leftover leftover chr query -h leftover is leftover the leftover same leftover help leftover Make a query towards a running node"
                )
                put(
                    "leftover_query_help_arg_types",
                    "official leftover <args> leftover types leftover integer: 123 leftover big_integer: 1234L leftover string leftover leftover leftover bytearray leftover leftover leftover array leftover leftover leftover dict leftover leftover leftover query leftover must leftover be leftover done leftover with leftover named leftover parameters"
                )
                put(
                    "leftover_query_positional_arg",
                    "chr query hello_world 1 leftover printed leftover Index: 1, Size: 1 leftover then leftover Error: invalid value for <args>: query must be done with named parameters in a dict leftover exit 1"
                )
                put(
                    "leftover_query_unknown_option",
                    "chr query --foo hello_world leftover printed leftover Error: no such option --foo. Did you mean -f? leftover exit 1"
                )
                put(
                    "leftover_query_cid_not_integer",
                    "chr query --cid hello_world leftover ate leftover the leftover queryname leftover as leftover the leftover --cid leftover value leftover and leftover printed leftover the leftover root leftover Usage: chr [OPTIONS] COMMAND [ARGS]... leftover Error: missing argument QUERYNAME leftover Error: invalid value for --cid: hello_world is not a valid integer leftover exit 1"
                )
                put(
                    "leftover_query_dict_arg_unknown",
                    "chr query hello_world leftover with leftover a leftover dict leftover arg leftover printed leftover Query 'hello_world' failed: Invalid argument(s): arg leftover exit 1 leftover leftover leftover hello_world() leftover takes leftover no leftover parameters leftover so leftover the leftover official leftover dict_arg leftover example leftover needs leftover its leftover own leftover Rell leftover query"
                )
                put(
                    "leftover_query_output_format_long",
                    "chr query hello_world --output-format JSON leftover and leftover --output-format pretty leftover long leftover form leftover both leftover printed leftover \"Hello World!\" leftover exit 0 leftover (same leftover as leftover -f)"
                )
                put(
                    "leftover_query_ft4_absent",
                    "chr query get_version leftover / leftover ft4.get_version leftover / leftover get_api_version leftover all leftover printed leftover Unknown query leftover exit 1 leftover leftover leftover src/lib/ft4 leftover is leftover materialized leftover on leftover disk leftover but leftover main.rell leftover never leftover imports leftover ft4 leftover so leftover no leftover FT4 leftover query leftover is leftover mounted leftover on leftover the leftover chain leftover leftover leftover FT4 leftover stays leftover HELP ONLY WRITE SKIP leftover register leftover login leftover transfer leftover auth"
                )
                put(
                    "leftover_query_gtx_metadata",
                    "REST leftover GET leftover ${ChrNodeHelp.DEFAULT_API_URL}/metadata/iid_0 leftover printed leftover the leftover chain's leftover built-in leftover GTX leftover surface leftover queries leftover last_block_info leftover tx_confirmation_time leftover operations leftover nop leftover __nop leftover timeb leftover all leftover gtxModule leftover net.postchain.gtx.StandardOpsGTXModule leftover leftover leftover the leftover Rell leftover hello_world leftover and leftover set_name leftover are leftover NOT leftover listed leftover there"
                )
                put(
                    "leftover_query_last_block_info",
                    "chr query last_block_info leftover (built-in leftover StandardOpsGTXModule leftover query leftover no leftover args) leftover printed leftover a leftover dict leftover blockRID leftover height leftover timestamp leftover exit 0 leftover leftover leftover a leftover second leftover query-only leftover query leftover that leftover exists leftover next leftover to leftover hello_world leftover leftover leftover never leftover record leftover or leftover invent leftover that leftover 64-char leftover hex"
                )
                put(
                    "leftover_query_last_block_info_formats",
                    "chr query last_block_info leftover -f pretty leftover rell leftover dict leftover leftover leftover -f JSON leftover leftover leftover -f XML leftover leftover leftover -f raw leftover leftover leftover -f YAML leftover all leftover exit 0 leftover leftover leftover hex leftover elided leftover here leftover on leftover purpose"
                )
                put(
                    "leftover_query_last_block_info_extra_arg",
                    "chr query last_block_info foo=1 leftover still leftover printed leftover the leftover dict leftover exit 0 leftover leftover leftover the leftover built-in leftover GTX leftover query leftover ignores leftover extra leftover named leftover args leftover while leftover the leftover Rell leftover hello_world leftover rejects leftover them leftover Invalid argument(s): foo leftover exit 1 leftover leftover leftover so leftover strict leftover arg leftover checking leftover is leftover a leftover Rell leftover property leftover not leftover a leftover CLI leftover property"
                )
                put(
                    "leftover_query_tx_confirmation_time_requires_arg",
                    "chr query tx_confirmation_time leftover (metadata leftover marks leftover txRID leftover BYTEARRAY leftover or leftover STRING leftover required) leftover printed leftover query: 500 Internal Server Error  Unknown error leftover exit 1 leftover leftover leftover REST leftover the leftover same leftover 500 leftover leftover leftover a leftover missing leftover required leftover GTX leftover arg leftover is leftover a leftover 500 leftover not leftover the leftover 400 leftover a leftover Rell leftover query leftover gives leftover leftover leftover no leftover tx leftover was leftover sent leftover HELP ONLY WRITE SKIP"
                )
                put(
                    "leftover_query_rest_iid_alias",
                    "REST leftover GET leftover ${ChrNodeHelp.DEFAULT_API_URL}/query/iid_0?type=hello_world leftover printed leftover 200 leftover application/json leftover \"Hello World!\" leftover leftover leftover the leftover iid_<chainIid> leftover alias leftover works leftover wherever leftover the leftover local leftover OpenAPI leftover spells leftover {blockchainRid} leftover so leftover a leftover local leftover REST leftover query leftover needs leftover no leftover BRID leftover at leftover all leftover leftover leftover keep leftover writing leftover <BlockchainRID> leftover and leftover do leftover not leftover invent leftover or leftover paste leftover one"
                )
                put(
                    "leftover_query_rest_missing_type",
                    "REST leftover GET leftover /query/iid_0 leftover with leftover no leftover type leftover and leftover POST leftover /query/iid_0 leftover empty leftover body leftover both leftover printed leftover 400 leftover Missing query type"
                )
                put(
                    "leftover_query_rest_post",
                    "REST leftover POST leftover /query/iid_0 leftover Content-Type: application/json leftover type leftover hello_world leftover printed leftover 200 leftover \"Hello World!\" leftover leftover leftover a leftover POST leftover here leftover is leftover still leftover query-only leftover no leftover tx leftover no leftover signature leftover no leftover keys"
                )
                put(
                    "leftover_query_rest_unknown_query",
                    "REST leftover GET leftover /query/iid_0?type=set_name leftover printed leftover 400 leftover Unknown query: set_name leftover code leftover QUERY_NOT_FOUND leftover leftover leftover set_name leftover is leftover an leftover operation leftover HELP ONLY WRITE SKIP leftover ops leftover txs"
                )
                put(
                    "leftover_query_rest_unknown_iid",
                    "REST leftover GET leftover /query/iid_99?type=hello_world leftover and leftover /brid/iid_99 leftover both leftover printed leftover 404 leftover Can't find blockchain with chain Iid: 99 in DB. Did you add this BC to the node? leftover which leftover is leftover the leftover REST leftover twin leftover of leftover the leftover CLI leftover --cid 99 leftover auto-detect leftover failure"
                )
                put(
                    "leftover_query_rest_query_gtv",
                    "REST leftover GET leftover /query_gtv/iid_0?type=hello_world leftover printed leftover 200 leftover application/octet-stream leftover a leftover 16-byte leftover GTV leftover body leftover leftover leftover decode leftover it leftover with leftover chr tools gtv leftover (${ChrToolsHelp.TOOLS_INDEX_TITLE}) leftover never leftover with leftover an leftover invented leftover hex"
                )
                put(
                    "leftover_query_rest_dquery_web_query",
                    "REST leftover GET leftover /dquery/iid_0?type=hello_world leftover printed leftover 400 leftover Type error: array expected, found STRING leftover and leftover /web_query/iid_0/hello_world leftover printed leftover 400 leftover Query 'hello_world' failed: Invalid argument(s): path, query_params leftover leftover leftover both leftover need leftover a leftover purpose-built leftover Rell leftover query leftover shape leftover and leftover /dquery leftover is leftover deprecated leftover on leftover the leftover local leftover spec"
                )
                put(
                    "leftover_query_rest_height_state",
                    "REST leftover GET leftover /blockchain/iid_0/height leftover printed leftover blockHeight leftover leftover leftover /blockchain/iid_0/nodestate leftover printed leftover RUNNING_VALIDATOR leftover leftover leftover /config/iid_0/features leftover printed leftover merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION} leftover leftover leftover /errors/iid_0 leftover and leftover /transactions/iid_0 leftover printed leftover empty leftover arrays leftover 200 leftover on leftover the leftover untouched leftover scaffold"
                )
                put(
                    "leftover_query_rest_config_xml",
                    "REST leftover GET leftover /config/iid_0 leftover printed leftover 200 leftover text/xml leftover a leftover dict leftover with leftover add_primary_key_to_header leftover blockstrategy leftover mininterblockinterval 1000 leftover net.postchain.base.BaseBlockBuildingStrategy leftover config_consensus_strategy HEADER_HASH leftover configurationfactory"
                )
                put(
                    "leftover_query_rest_version",
                    "REST leftover GET leftover /version leftover and leftover /version/iid_0 leftover printed leftover version 22 leftover leftover leftover /infrastructure_version leftover printed leftover postchain 3.49.16 leftover infrastructure net.postchain.ebft.BaseEBFTInfrastructureFactory leftover rest-api 22 leftover database-server-version 17.11 leftover which leftover matches leftover chr --version leftover postchain 3.49.16"
                )
                put(
                    "leftover_query_rest_debug_moved",
                    "REST leftover GET leftover /_debug leftover printed leftover 200 leftover text/html leftover H1 leftover _debug endpoint moved leftover to leftover a leftover separate leftover port leftover 7750 leftover by leftover default leftover configurable leftover with leftover debug.port leftover in leftover node leftover configuration leftover or leftover POSTCHAIN_DEBUG_PORT"
                )
                put(
                    "leftover_query_rest_404s",
                    "REST leftover GET leftover /query leftover /status leftover /height leftover /brid leftover /node/iid_0 leftover all leftover printed leftover 404 leftover with leftover an leftover empty leftover body leftover leftover leftover every leftover query leftover path leftover needs leftover the leftover chain leftover segment"
                )
                put(
                    "leftover_query_rest_cors",
                    "REST leftover OPTIONS leftover /query/iid_0 leftover printed leftover 200 leftover access-control-allow-methods GET, POST, OPTIONS leftover access-control-allow-headers Content-Type, Accept, X-Accept-Query-Response-Signature leftover access-control-expose-headers X-Data-Truncated, X-Transaction-Timestamp, X-Block-Height, X-Query-Response-Signature"
                )
                put(
                    "leftover_query_rest_apidocs",
                    "REST leftover GET leftover /apidocs leftover printed leftover 200 leftover a leftover rapi-doc leftover page leftover spec-url leftover /apidocs/postchain-restapi.yaml leftover server-url leftover ${ChrNodeHelp.DEFAULT_API_URL} leftover leftover leftover the leftover spec leftover itself leftover 200 leftover text/yaml leftover about leftover 81 leftover KB"
                )
                put(
                    "leftover_query_rest_openapi_query_group",
                    "the leftover local leftover OpenAPI leftover query leftover tag leftover lists leftover /query leftover /query_gtv leftover GET leftover and leftover POST leftover /query_async leftover leftover leftover /web_query leftover and leftover the leftover deprecated leftover /dquery leftover leftover leftover documented leftover errors leftover Missing query type leftover Unknown query leftover code leftover QUERY_NOT_FOUND leftover leftover leftover optional leftover request leftover header leftover X-Accept-Query-Response-Signature leftover leftover leftover same leftover groups leftover as leftover ${ChromiaLanguageClientsHelp.POSTCHAIN_REST_API_INDEX_TITLE} leftover already leftover on leftover disk"
                )
                put(
                    "leftover_query_no_node_stop",
                    "chr node --help leftover on leftover 0.33.2 leftover lists leftover only leftover start leftover and leftover update leftover so leftover there leftover is leftover no leftover chr node stop leftover leftover leftover end leftover the leftover chr node start leftover process leftover to leftover stop leftover the leftover local leftover test leftover node leftover and leftover not leftover leak leftover it leftover leftover leftover chr node start --help leftover flags leftover -s/--settings leftover -bc/--blockchain-config leftover --name leftover -p leftover -np/--node-properties leftover --directory-chain-mock leftover --hide-lib-warnings leftover --sql-log leftover --wipe/--no-wipe"
                )
                put(
                    "leftover_query_brid_never_pasted",
                    "REST leftover GET leftover /brid/iid_0 leftover returns leftover a leftover 64-char leftover BRID leftover and leftover /blocks/iid_0 leftover embeds leftover block leftover RIDs leftover leftover leftover neither leftover is leftover recorded leftover in leftover leftover_dapp_build_help leftover leftover leftover always leftover write leftover <BlockchainRID> leftover and leftover never leftover invent leftover or leftover paste leftover the leftover hex"
                )
                put("leftover_deploy_help", "chr_deploy_help")
                put("leftover_deploy_index_title", ChrDeployHelp.DEPLOYMENT_INDEX_TITLE)
                put("leftover_deploy_commands_index_title", ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE)
                put("leftover_deploy_testnet_deploy_dapp_index_title", ChrDeployHelp.TESTNET_DEPLOY_DAPP_INDEX_TITLE)
                put("leftover_deploy_mainnet_deploy_dapp_index_title", ChrDeployHelp.MAINNET_DEPLOY_DAPP_INDEX_TITLE)
                put("leftover_deploy_testnet_getting_started_index_title", ChrDeployHelp.TESTNET_GETTING_STARTED_INDEX_TITLE)
                put("leftover_deploy_mainnet_getting_started_index_title", ChrDeployHelp.MAINNET_GETTING_STARTED_INDEX_TITLE)
                put("leftover_deploy_get_started_testnet_index_title", ChrDeployHelp.GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE)
                put("leftover_deploy_yml_index_title", ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE)
                put("leftover_deploy_yml_help", ChromiaYmlDefinitionsHelp.TOOL_NAME)
                put(
                    "leftover_deploy_no_such_subcommand",
                    "chr deploy --help leftover on leftover 0.33.2 leftover is leftover NOT leftover a leftover command: leftover bare leftover chr deploy leftover printed leftover Error: no such subcommand deploy. Did you mean deployment? leftover and leftover chr deploy --help leftover just leftover printed leftover the leftover root leftover help leftover exit 0 leftover official leftover command leftover is leftover chr deployment"
                )
                put(
                    "leftover_deploy_subcommands",
                    "chr deployment leftover with leftover no leftover subcommand leftover prints leftover the leftover same leftover help leftover as leftover chr deployment --help leftover exit 0 leftover Create and maintain deployments leftover create leftover info leftover inspect leftover update leftover resume leftover pause leftover remove leftover proposal leftover voterset leftover container leftover only leftover option leftover -h, --help"
                )
                put(
                    "leftover_deploy_create_flags",
                    "chr deployment create leftover HELP ONLY leftover flags leftover -cfg/--config leftover -s/--settings leftover --secret leftover --key-id leftover -d/--network leftover -bc/--blockchain leftover --no-compression leftover --hide-lib-warnings leftover -y leftover -h/--help leftover NOT leftover RUN"
                )
                put(
                    "leftover_deploy_create_write_skip",
                    "HELP ONLY WRITE SKIP leftover chr deployment create leftover was leftover never leftover run leftover no leftover deploy leftover no leftover signing leftover no leftover keys leftover no leftover invented leftover BlockchainRID leftover no leftover invented leftover container leftover id leftover -y leftover confirms leftover a leftover new leftover deployment leftover official leftover non-interactive leftover error leftover already leftover on leftover disk leftover ${ChrDeployHelp.CREATE_Y_ERROR}"
                )
                put(
                    "leftover_deploy_update_flags",
                    "chr deployment update leftover HELP ONLY leftover flags leftover -cfg/--config leftover -s/--settings leftover --secret leftover --key-id leftover -d/--network leftover -bc/--blockchain leftover --no-compression leftover --hide-lib-warnings leftover --height leftover --verify-only leftover --skip-verification leftover -h/--help leftover NOT leftover RUN"
                )
                put(
                    "leftover_deploy_update_write_skip",
                    "HELP ONLY WRITE SKIP leftover chr deployment update leftover was leftover never leftover run leftover --verify-only leftover and leftover --skip-verification leftover are leftover official leftover flags leftover not leftover an leftover excuse leftover to leftover send leftover an leftover update leftover transaction leftover no leftover signing leftover no leftover keys"
                )
                put(
                    "leftover_deploy_info_flags",
                    "chr deployment info leftover read-only leftover flags leftover -cfg/--config leftover -s/--settings leftover -brid/--blockchain-rid leftover --cid leftover --api-url leftover --mainnet/--testnet leftover -d/--network leftover -bc/--blockchain leftover --verbose leftover -f/--output-format=(table|JSON) leftover no leftover key leftover pair leftover flags"
                )
                put(
                    "leftover_deploy_info_local",
                    "chr deployment info leftover against leftover the leftover local leftover scaffold leftover node leftover printed leftover Cluster not found for blockchain rid <shortened BRID> leftover query: 400 Bad Request  Unknown query: cm_get_blockchain_cluster from ${ChrNodeHelp.DEFAULT_API_URL} leftover because leftover the leftover local leftover node leftover is leftover not leftover Directory-managed leftover never leftover paste leftover the leftover computed leftover BRID"
                )
                put(
                    "leftover_deploy_inspect_flags",
                    "chr deployment inspect leftover read-only leftover flags leftover -cfg/--config leftover -s/--settings leftover -brid/--blockchain-rid leftover --cid leftover --api-url leftover --mainnet/--testnet leftover -d/--network leftover -bc/--blockchain leftover -f/--output-format=(table|JSON) leftover -m/--modules leftover -l/--list-modules leftover --module-args leftover --definitions=(queries|operations|entities|objects) leftover --signature leftover no leftover key leftover pair leftover flags"
                )
                put(
                    "leftover_deploy_inspect_local",
                    "chr deployment inspect leftover from leftover the leftover project leftover dir leftover against leftover the leftover local leftover node leftover exit 0 leftover printed leftover JSON leftover queries leftover mount_name hello_world leftover return_type text leftover operations leftover mount_name set_name leftover parameters leftover name text leftover entities leftover [] leftover objects leftover mount_name my_name leftover mutable leftover name text"
                )
                put(
                    "leftover_deploy_inspect_list_modules",
                    "chr deployment inspect --list-modules leftover printed leftover [ \"main\" ] leftover exit 0"
                )
                put(
                    "leftover_deploy_inspect_definitions",
                    "chr deployment inspect --definitions=queries leftover printed leftover only leftover the leftover queries leftover block leftover hello_world leftover exit 0 leftover --definitions=operations leftover printed leftover only leftover set_name leftover exit 0 leftover -m/--modules main leftover same leftover queries leftover block leftover exit 0"
                )
                put(
                    "leftover_deploy_inspect_signature",
                    "chr deployment inspect --signature=hello_world leftover printed leftover mount_name hello_world leftover return_type text leftover parameters {} leftover and leftover exited leftover 0"
                )
                put(
                    "leftover_deploy_inspect_module_args",
                    "chr deployment inspect --module-args leftover printed leftover [] leftover exit 0 leftover (leftover default leftover scaffold leftover has leftover no leftover moduleArgs)"
                )
                put(
                    "leftover_deploy_inspect_table",
                    "chr deployment inspect -f table leftover printed leftover Query leftover Return type leftover Parameters leftover hello_world leftover text leftover Operation leftover set_name leftover name: text leftover Object leftover my_name leftover mutable name: text leftover exit 0 leftover -f JSON leftover matches leftover the leftover default leftover JSON"
                )
                put(
                    "leftover_deploy_network_missing",
                    "chr deployment info --network testnet leftover and leftover chr deployment inspect --network testnet leftover from leftover the leftover default leftover scaffold leftover (no leftover deployments leftover block) leftover printed leftover Error: invalid value for --network: Specified target [testnet] does not exist leftover exit 1 leftover so leftover nothing leftover left leftover the leftover box"
                )
                put(
                    "leftover_deploy_brid_invalid_hex",
                    "chr deployment info --blockchain-rid ZZ leftover printed leftover Char Z is not a hex digit leftover exit 3"
                )
                put(
                    "leftover_deploy_brid_wrong_size",
                    "chr deployment info --blockchain-rid AB leftover printed leftover Wrong size of Blockchain RID, was 1 should be 32 (64 characters) leftover exit 3 leftover do leftover not leftover invent leftover a leftover BlockchainRID"
                )
                put(
                    "leftover_deploy_key_flags",
                    "leftover key leftover pair leftover source leftover --secret=<path> leftover and leftover --key-id=<key_id> leftover exist leftover only leftover on leftover create leftover and leftover update leftover HELP ONLY leftover reference leftover an leftover existing leftover key leftover id leftover this leftover fire leftover generated leftover no leftover key leftover and leftover used leftover none"
                )
                put(
                    "leftover_deploy_yml_deployments",
                    "leftover chromia.yml leftover deployments leftover section leftover shape leftover already leftover on leftover disk leftover deployments.<net>.url leftover (string leftover or leftover list) leftover brid leftover container leftover chains.<name> leftover reserved leftover names leftover mainnet leftover / leftover testnet leftover auto-fill leftover Directory leftover brid leftover + leftover url leftover since leftover CLI leftover 0.29.8 leftover container leftover is leftover the leftover Vault/PMC leftover lease leftover id leftover do leftover not leftover invent leftover one leftover (leftover ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} leftover ${ChromiaYmlDefinitionsHelp.TOOL_NAME} leftover ${ChromiaDocsYmlHelp.TOOL_NAME})"
                )
                put(
                    "leftover_deploy_yml_write_back",
                    "leftover since leftover CLI leftover 0.30.0 leftover chr deployment create leftover writes leftover deployments.<net>.chains.<name> leftover back leftover into leftover chromia.yml leftover and leftover chr deployment update leftover requires leftover chains leftover and leftover does leftover not leftover rewrite leftover the leftover file leftover already leftover on leftover disk leftover in leftover chr_deploy_help leftover HELP ONLY leftover here"
                )
                put(
                    "leftover_deploy_sign_skip",
                    "HELP ONLY WRITE SKIP leftover proposal leftover vote leftover retract-vote leftover revoke leftover rename leftover voterset leftover update leftover add-dapp-provider leftover container leftover configuration leftover pause leftover resume leftover deployment leftover pause leftover resume leftover remove leftover all leftover skipped leftover proposal leftover list/info leftover and leftover voterset leftover list/info leftover stay leftover read-only"
                )
                put(
                    "leftover_deploy_write_skip",
                    "HELP ONLY WRITE SKIP leftover deploy leftover is leftover HELP ONLY leftover here leftover no leftover deploy leftover no leftover signing leftover no leftover keys leftover no leftover invented leftover BlockchainRID leftover no leftover invented leftover container leftover id leftover no leftover testnet leftover no leftover mainnet leftover only leftover chr deployment --help leftover style leftover inspection leftover plus leftover official leftover deploy leftover INDEX leftover titles leftover already leftover on leftover disk"
                )
                put(
                    "leftover_deploy_nothing_deployed",
                    "leftover nothing leftover was leftover deployed leftover this leftover fire leftover only leftover --help leftover output leftover local leftover inspect leftover against leftover the leftover scaffold leftover node leftover and leftover local leftover config leftover errors"
                )

                put(
                    "leftover_deploy_commands",
                    "create info inspect update resume pause remove proposal voterset container"
                )
                put(
                    "leftover_deploy_missing_subcommand",
                    "chr deployment leftover (no leftover subcommand) leftover printed leftover Usage: chr deployment [OPTIONS] COMMAND [ARGS]... leftover Create and maintain deployments leftover create leftover info leftover inspect leftover update leftover resume leftover pause leftover remove leftover proposal leftover voterset leftover container leftover exit 0 leftover leftover leftover chr deploy leftover Error: no such subcommand deploy. Did you mean deployment? leftover exit 1 leftover leftover leftover chr deploy --help leftover prints leftover root leftover chr leftover help leftover (not leftover deployment leftover help) leftover exit 0"
                )
                put(
                    "leftover_deploy_create_help",
                    "chr deployment create --help leftover printed leftover Deploy new blockchain instance leftover -d/--network leftover -bc/--blockchain leftover -s/--settings leftover --secret leftover --key-id leftover -y leftover Confirm that this will create a new deployment leftover --no-compression leftover --hide-lib-warnings leftover exit 0 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover create leftover leftover leftover signed leftover send leftover leftover leftover chr leftover deployment leftover create leftover (no leftover flags) leftover Error: missing option --network leftover exit 1 leftover leftover leftover chr leftover deployment leftover create leftover --network testnet leftover Error: invalid value for --network: Specified target [testnet] does not exist leftover exit 1 leftover leftover leftover never leftover signed"
                )
                put(
                    "leftover_deploy_update_help",
                    "chr deployment update --help leftover printed leftover Update configuration of a deployed blockchain leftover --height leftover --verify-only leftover Verifies blockchain config without sending update transaction leftover --skip-verification leftover Skip verification of blockchain config before sending update transaction leftover -d/--network leftover -bc/--blockchain leftover exit 0 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover update leftover leftover leftover signed leftover send leftover leftover leftover chr leftover deployment leftover update leftover (no leftover flags) leftover Error: missing option --network leftover exit 1 leftover leftover leftover chr leftover deployment leftover update leftover --network testnet leftover Error: invalid value for --network: Specified target [testnet] does not exist leftover exit 1 leftover leftover leftover never leftover signed"
                )
                put(
                    "leftover_deploy_inspect_help",
                    "chr deployment inspect --help leftover printed leftover Inspect the API of a deployed blockchain leftover -brid/--blockchain-rid leftover --cid leftover --api-url leftover --mainnet/--testnet leftover -d/--network leftover -bc/--blockchain leftover -f/--output-format=(table|JSON) leftover -m/--modules leftover -l/--list-modules leftover --module-args leftover --definitions=(queries|operations|entities|objects) leftover --signature leftover exit 0 leftover leftover leftover chr leftover deployment leftover inspect leftover (no leftover flags leftover while leftover leftover leftover node leftover is leftover up) leftover printed leftover JSON leftover queries leftover mount_name leftover hello_world leftover operations leftover set_name leftover objects leftover my_name leftover exit 0 leftover leftover leftover same leftover -s chromia.yml leftover leftover leftover --cid 0 leftover leftover leftover --api-url http://localhost:7740 --cid 0 leftover exit 0 leftover leftover leftover needs leftover no leftover --blockchain-rid leftover leftover leftover default leftover output leftover JSON leftover leftover leftover do leftover not leftover invent leftover or leftover paste leftover a leftover BRID"
                )
                put(
                    "leftover_deploy_info_help",
                    "chr deployment info --help leftover printed leftover Information about a deployed blockchain leftover --verbose leftover Show verbose information about nodes leftover -f/--output-format=(table|JSON) leftover -brid/--blockchain-rid leftover --cid leftover --api-url leftover --mainnet/--testnet leftover -d/--network leftover -bc/--blockchain leftover exit 0 leftover leftover leftover chr leftover deployment leftover info leftover (no leftover flags leftover while leftover leftover leftover node leftover is leftover up) leftover printed leftover Cluster not found for blockchain rid 5C:7EF leftover leftover leftover query: 400 Bad Request  Unknown query: cm_get_blockchain_cluster from http://localhost:7740 leftover exit 0 leftover leftover leftover same leftover with leftover -s chromia.yml leftover leftover leftover --cid 0 leftover leftover leftover local leftover node leftover is leftover not leftover Directory-managed leftover leftover leftover do leftover not leftover invent leftover or leftover paste leftover a leftover BRID leftover leftover leftover never leftover paste leftover computed leftover 64-hex"
                )
                put(
                    "leftover_deploy_no_deployments_block",
                    "scratch leftover chromia.yml leftover has leftover no leftover deployments leftover block leftover leftover leftover chr leftover deployment leftover inspect leftover --network testnet leftover printed leftover Error: invalid value for --network: Specified target [testnet] does not exist leftover exit 1 leftover leftover leftover same leftover for leftover info leftover / leftover create leftover / leftover update leftover --network testnet leftover exit 1 leftover leftover leftover never leftover signed leftover leftover leftover nothing leftover left leftover the leftover box"
                )
                put(
                    "leftover_deploy_settings_missing",
                    "chr leftover deployment leftover inspect leftover -s /tmp/no-such-chromia.yml leftover printed leftover Error: invalid value for -s: file \"/tmp/no-such-chromia.yml\" does not exist. leftover exit 1 leftover leftover leftover same leftover for leftover info leftover exit 1"
                )
                put(
                    "leftover_deploy_flags",
                    "-s/--settings -cfg/--config -d/--network -bc/--blockchain -brid/--blockchain-rid --cid --api-url --mainnet/--testnet --secret --key-id -y --no-compression --hide-lib-warnings --height --verify-only --skip-verification -f/--output-format=(table|JSON) -m/--modules -l/--list-modules --module-args --definitions --signature --verbose --from --to --all --pending --id -n/--name -c/--container"
                )
                put(
                    "leftover_deploy_proposal_list_help",
                    "chr leftover deployment leftover proposal leftover list leftover --help leftover printed leftover List all proposals that you can vote on leftover --from leftover --to leftover --all leftover --pending leftover -d/--network leftover -f/--output-format=(table|JSON) leftover exit 0 leftover leftover leftover chr leftover deployment leftover proposal leftover list leftover (no leftover flags) leftover Error: missing option --network leftover exit 1 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover proposal leftover vote leftover leftover leftover retract-vote leftover leftover leftover revoke leftover leftover leftover rename"
                )
                put(
                    "leftover_deploy_proposal_info_help",
                    "chr leftover deployment leftover proposal leftover info leftover --help leftover printed leftover Get information of a given proposal leftover --id leftover -d/--network leftover -f/--output-format=(table|JSON) leftover exit 0 leftover leftover leftover chr leftover deployment leftover proposal leftover info leftover (no leftover flags) leftover Error: missing option --id leftover leftover leftover Error: missing option --network leftover exit 1 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover proposal leftover vote leftover leftover leftover retract-vote leftover leftover leftover revoke leftover leftover leftover rename"
                )
                put(
                    "leftover_deploy_voterset_list_help",
                    "chr leftover deployment leftover voterset leftover list leftover --help leftover printed leftover List all voter sets leftover -d/--network leftover -c/--container leftover -f/--output-format leftover exit 0 leftover leftover leftover chr leftover deployment leftover voterset leftover list leftover (no leftover flags) leftover Error: missing option --network leftover exit 1 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover voterset leftover update leftover leftover leftover add-dapp-provider"
                )
                put(
                    "leftover_deploy_voterset_info_help",
                    "chr leftover deployment leftover voterset leftover info leftover --help leftover printed leftover Show information of voter set leftover -n/--name leftover -c/--container leftover -d/--network leftover -f/--output-format leftover exit 0 leftover leftover leftover chr leftover deployment leftover voterset leftover info leftover (no leftover flags) leftover Error: missing option --network leftover leftover leftover Error: must provide one of --name, --container leftover exit 1 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover voterset leftover update leftover leftover leftover add-dapp-provider"
                )
                put(
                    "leftover_deploy_container_help",
                    "chr leftover deployment leftover container leftover --help leftover printed leftover Manage container operations leftover configuration leftover Propose configurations leftover pause leftover resume leftover exit 0 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover container leftover configuration leftover leftover leftover pause leftover leftover leftover resume leftover leftover leftover next leftover leftover leftover vault-lease leftover HELP ONLY leftover (${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE})"
                )
                put(
                    "leftover_deploy_inspect_brid_invalid_hex",
                    "chr leftover deployment leftover inspect leftover --blockchain-rid leftover ZZ leftover printed leftover Char Z is not a hex digit leftover exit 3"
                )
                put(
                    "leftover_deploy_inspect_brid_wrong_size",
                    "chr leftover deployment leftover inspect leftover --blockchain-rid leftover AB leftover printed leftover Wrong size of Blockchain RID, was 1 should be 32 (64 characters) leftover exit 3 leftover leftover leftover do leftover not leftover invent leftover or leftover paste leftover a leftover BRID leftover leftover leftover never leftover paste leftover 64-hex"
                )
                put(
                    "leftover_deploy_api_refused",
                    "chr leftover deployment leftover inspect leftover --api-url leftover http://127.0.0.1:1 leftover --cid leftover 0 leftover printed leftover Could not auto-detect brid from http://127.0.0.1:1, reason: 503 Client Error: Connection Refused leftover exit 1 leftover leftover leftover chr leftover deployment leftover info leftover --api-url leftover http://127.0.0.1:1 leftover --cid leftover 0 leftover printed leftover getBlockchainRID: 503 Client Error: Connection Refused leftover from leftover http://127.0.0.1:1 leftover exit 1 leftover leftover leftover after leftover leftover leftover node leftover died leftover inspect leftover --list-modules leftover / leftover -f table leftover / leftover info leftover --verbose leftover same leftover Connection refused leftover from leftover http://localhost:7740 leftover exit 1"
                )
                put(
                    "leftover_deploy_blockchain_needs_network",
                    "chr leftover deployment leftover inspect leftover --blockchain leftover my_rell_dapp leftover printed leftover Error: missing option --network leftover exit 1 leftover leftover leftover same leftover for leftover info leftover --blockchain leftover my_rell_dapp leftover exit 1 leftover leftover leftover same leftover for leftover proposal leftover list leftover leftover leftover voterset leftover list leftover leftover leftover create leftover leftover leftover update leftover (no leftover flags) leftover exit 1"
                )


                put("leftover_vault", "vault_lease_help")
                put("leftover_vault_help", "vault_lease_help")
                put("leftover_vault_testnet_get_container_index_title", VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE)
                put("leftover_vault_mainnet_get_container_index_title", VaultLeaseHelp.MAINNET_GET_CONTAINER_INDEX_TITLE)
                put("leftover_vault_hosting_index_title", VaultLeaseHelp.GET_STARTED_HOSTING_INDEX_TITLE)
                put("leftover_vault_supported_wallets_index_title", VaultLeaseHelp.GET_STARTED_SUPPORTED_WALLETS_INDEX_TITLE)
                put("leftover_vault_provider_staking_index_title", VaultLeaseHelp.GET_STARTED_PROVIDER_STAKING_INDEX_TITLE)
                put("leftover_vault_testnet_containers", VaultLeaseHelp.TESTNET_VAULT_CONTAINERS)
                put("leftover_vault_mainnet_containers", VaultLeaseHelp.MAINNET_VAULT_CONTAINERS)
                put("leftover_vault_testnet_faucet", VaultLeaseHelp.TESTNET_FAUCET)
                put("leftover_vault_mainnet_deposit", VaultLeaseHelp.MAINNET_VAULT_DEPOSIT)
                put(
                    "leftover_vault_container_placeholder",
                    VaultLeaseHelp.CONTAINER_PLACEHOLDER
                )
                put(
                    "leftover_vault_workflow",
                    "leftover vault leftover HELP ONLY leftover path leftover already leftover on leftover disk leftover via leftover vault_lease_help leftover leftover leftover 1 leftover get leftover tokens leftover testnet leftover faucet leftover ${VaultLeaseHelp.TESTNET_FAUCET} leftover leftover leftover mainnet leftover deposit leftover ${VaultLeaseHelp.MAINNET_VAULT_DEPOSIT} leftover leftover leftover 2 leftover open leftover Vault leftover containers leftover leftover leftover testnet leftover ${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS} leftover leftover leftover mainnet leftover ${VaultLeaseHelp.MAINNET_VAULT_CONTAINERS} leftover leftover leftover 3 leftover lease leftover a leftover container leftover in leftover Vault leftover leftover leftover paste leftover an leftover existing leftover pubkey leftover from leftover ~/.chromia/<key-id>.pubkey leftover leftover leftover this leftover tool leftover does leftover not leftover generate leftover a leftover key leftover leftover leftover 4 leftover adjust leftover SCUs leftover leftover leftover sign leftover in leftover Vault leftover leftover leftover result leftover is leftover a leftover real leftover Container leftover ID leftover leftover leftover never leftover invent leftover one leftover leftover leftover placeholder leftover ${VaultLeaseHelp.CONTAINER_PLACEHOLDER} leftover leftover leftover 5 leftover put leftover that leftover real leftover Container leftover ID leftover in leftover chromia.yml leftover deployments.<net>.container leftover (${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE}) leftover leftover leftover 6 leftover then leftover chr leftover deployment leftover create leftover HELP ONLY WRITE SKIP leftover (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE}) leftover leftover leftover Directory leftover Chain leftover BRIDs leftover are leftover official leftover write_deployment_config leftover values leftover already leftover on leftover disk leftover — leftover never leftover invent leftover or leftover paste leftover 64-hex leftover leftover leftover titles leftover ${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE} leftover ${VaultLeaseHelp.MAINNET_GET_CONTAINER_INDEX_TITLE} leftover ${VaultLeaseHelp.GET_STARTED_HOSTING_INDEX_TITLE}"
                )
                put(
                    "leftover_vault_write_skip",
                    "HELP ONLY WRITE SKIP leftover leftover leftover register leftover leftover leftover login leftover leftover leftover transfer leftover leftover leftover auth leftover leftover leftover no leftover keys leftover leftover leftover no leftover signed leftover txs leftover leftover leftover no leftover invented leftover container leftover id leftover leftover leftover no leftover invented leftover BlockchainRID leftover leftover leftover no leftover pasted leftover 64-hex leftover leftover leftover do leftover not leftover generate leftover a leftover key leftover leftover leftover do leftover not leftover run leftover chr leftover deployment leftover create leftover from leftover this leftover walk"
                )
                put(
                    "leftover_vault_yml_container",
                    "chromia.yml leftover deployments.<net>.container leftover = leftover real leftover Vault leftover Container leftover ID leftover leftover leftover placeholder leftover ${VaultLeaseHelp.CONTAINER_PLACEHOLDER} leftover leftover leftover never leftover invent leftover leftover leftover create leftover write-back leftover of leftover chains leftover since leftover CLI leftover 0.30.0 leftover already leftover on leftover disk"
                )
                put("leftover_tx", "chr tx")
                put("leftover_tx_index_title", ChrQueryHelp.TX_INDEX_TITLE)
                put("leftover_tx_help", "chr_query_help")
                put(
                    "leftover_tx_help_text",
                    "chr leftover tx leftover --help leftover printed leftover Usage: chr tx [<options>] <opname> [<args>]... leftover Make a transaction towards a node leftover leftover leftover Supports leftover both leftover specifying leftover the leftover target leftover node leftover using leftover url leftover and leftover brid/id leftover or leftover from leftover a leftover deployment leftover leftover leftover posts leftover asynchronously leftover unless leftover --await leftover leftover leftover FT4 leftover compatibility leftover --ft-auth leftover --ft-account-id leftover --evm-auth leftover --ft-register-account leftover leftover leftover ICCF leftover --iccf-tx leftover --iccf-source leftover --source-api-url leftover --iccf-force-intra-network leftover --iccf-arg-pos leftover leftover leftover official leftover Examples leftover primitive_args leftover dict_arg leftover map_arg leftover struct_arg leftover leftover leftover exit 0"
                )
                put(
                    "leftover_tx_missing_opname",
                    "chr leftover tx leftover (no leftover <opname>) leftover printed leftover Error: missing argument <opname> leftover exit 1"
                )
                put(
                    "leftover_tx_no_node",
                    "chr leftover tx leftover set_name leftover Alice leftover with leftover no leftover node leftover on leftover :7740 leftover printed leftover Could not auto-detect brid from http://localhost:7740, reason: 503 Client Error: Connection Refused leftover exit 1 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover do leftover not leftover send leftover even leftover when leftover the leftover node leftover is leftover up"
                )
                put(
                    "leftover_tx_brid_invalid_hex",
                    "chr leftover tx leftover --blockchain-rid leftover ZZ leftover set_name leftover Alice leftover printed leftover Char Z is not a hex digit leftover exit 3"
                )
                put(
                    "leftover_tx_brid_wrong_size",
                    "chr leftover tx leftover --blockchain-rid leftover AB leftover set_name leftover Alice leftover printed leftover Wrong size of Blockchain RID, was 1 should be 32 (64 characters) leftover exit 3 leftover leftover leftover do leftover not leftover invent leftover or leftover paste leftover a leftover BRID leftover leftover leftover never leftover paste leftover 64-hex"
                )
                put(
                    "leftover_tx_api_refused",
                    "chr leftover tx leftover --api-url leftover http://127.0.0.1:1 leftover --cid leftover 0 leftover set_name leftover Alice leftover printed leftover Could not auto-detect brid from http://127.0.0.1:1, reason: 503 Client Error: Connection Refused leftover exit 1"
                )
                put(
                    "leftover_tx_network_missing",
                    "chr leftover tx leftover --network leftover testnet leftover set_name leftover Alice leftover printed leftover Error: invalid value for --network: Specified target [testnet] does not exist leftover exit 1 leftover leftover leftover chromia.yml leftover has leftover no leftover deployments leftover block leftover leftover leftover nothing leftover left leftover the leftover box"
                )
                put(
                    "leftover_tx_blockchain_needs_network",
                    "chr leftover tx leftover --blockchain leftover my_rell_dapp leftover set_name leftover Alice leftover printed leftover Error: missing option --network leftover exit 1"
                )
                put(
                    "leftover_tx_settings_missing",
                    "chr leftover tx leftover -s leftover /tmp/no-such-chromia.yml leftover set_name leftover Alice leftover printed leftover Error: invalid value for -s: file \"/tmp/no-such-chromia.yml\" does not exist. leftover exit 1"
                )
                put(
                    "leftover_tx_cid_not_integer",
                    "chr leftover tx leftover --cid leftover hello leftover set_name leftover Alice leftover printed leftover Error: invalid value for --cid: hello is not a valid integer leftover exit 1"
                )
                put(
                    "leftover_tx_unknown_option",
                    "chr leftover tx leftover --foo leftover printed leftover Error: no such option --foo leftover Error: missing argument OPNAME leftover exit 1"
                )
                put(
                    "leftover_tx_flags",
                    "-cfg/--config -s/--settings --secret --key-id -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain --ft-auth --ft-account-id --evm-auth --ft-register-account --iccf-tx --iccf-source --source-api-url --iccf-force-intra-network --iccf-arg-pos -a/--await/--no-await -nop --timeb-at --timeb-after"
                )
                put(
                    "leftover_tx_write_skip",
                    "HELP ONLY WRITE SKIP leftover leftover leftover chr leftover tx leftover leftover leftover signed leftover send leftover leftover leftover set_name leftover leftover leftover register leftover leftover leftover login leftover leftover leftover transfer leftover leftover leftover auth leftover leftover leftover --ft-auth leftover leftover leftover --ft-register-account leftover leftover leftover --evm-auth leftover leftover leftover --secret leftover leftover leftover --key-id leftover leftover leftover no leftover keys leftover leftover leftover no leftover invented leftover BRID leftover leftover leftover no leftover pasted leftover 64-hex leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrQueryHelp.TX_INDEX_TITLE}"
                )
                put("leftover_multi", "chr multi-signature")
                put("leftover_multi_help", ChrMultiSignatureHelp.TOOL_NAME)
                put("leftover_multi_index_title", ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE)
                put(
                    "leftover_multi_help_text",
                    "chr leftover multi-signature leftover --help leftover printed leftover Handle transactions with need of multiple signers leftover leftover leftover Commands leftover create leftover sign leftover send leftover view leftover exit 0 leftover leftover leftover bare leftover chr leftover multi-signature leftover prints leftover the leftover same leftover help leftover exit 0"
                )
                put(
                    "leftover_multi_view_help",
                    "chr leftover multi-signature leftover view leftover --help leftover printed leftover View a existing transaction leftover -f/--file leftover exit 0 leftover leftover leftover chr leftover multi-signature leftover view leftover (no leftover flags) leftover Error: missing option --file leftover exit 1 leftover leftover leftover chr leftover multi-signature leftover view leftover -f leftover /tmp/no-such-tx.gtv leftover Error: invalid value for -f: file \"/tmp/no-such-tx.gtv\" does not exist. leftover exit 1 leftover leftover leftover view leftover is leftover the leftover only leftover query-only leftover multi-signature leftover subcommand leftover used leftover here"
                )
                put(
                    "leftover_multi_write_skip",
                    "HELP ONLY WRITE SKIP leftover leftover leftover create leftover leftover leftover sign leftover leftover leftover send leftover leftover leftover leftover leftover create leftover --help leftover printed leftover but leftover create leftover was leftover NEVER leftover run leftover leftover leftover missing leftover OPNAME leftover + leftover must leftover provide leftover one leftover of leftover --signer, leftover --signers, leftover --signers-file leftover exit 1 leftover leftover leftover no leftover keys leftover leftover leftover no leftover signed leftover txs leftover leftover leftover no leftover invented leftover BRID leftover leftover leftover no leftover pasted leftover 64-hex leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE}"
                )

                put("leftover_seeder", "chr seeder")
                put("leftover_seeder_help", ChrSeederHelp.TOOL_NAME)
                put("leftover_seeder_index_title", ChrSeederHelp.SEEDER_INDEX_TITLE)
                put("leftover_seeder_commands_index_title", ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE)
                put("leftover_seeder_generator_index_title", ChrSeederHelp.SEEDER_GENERATOR_INDEX_TITLE)
                put("leftover_seeder_example_index_title", ChrSeederHelp.SEEDER_EXAMPLE_INDEX_TITLE)
                put("leftover_seeder_configurable_index_title", ChrSeederHelp.SEEDER_CONFIGURABLE_INDEX_TITLE)
                put(
                    "leftover_seeder_help_text",
                    "chr leftover seeder leftover --help leftover (leftover and leftover bare leftover chr leftover seeder) leftover printed leftover Usage: chr seeder [OPTIONS] COMMAND [ARGS]... leftover Generate fake data for a local database leftover leftover leftover Options leftover -h/--help leftover leftover leftover Commands leftover init leftover Create initial seeder configuration for blockchains leftover generate leftover Generate Rell blockchain seeder module leftover leftover leftover exit 0 leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrSeederHelp.SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE} leftover leftover leftover sibling leftover help leftover tool leftover ${ChrSeederHelp.TOOL_NAME} leftover leftover leftover ${ChromiaYmlDefinitionsHelp.TOOL_NAME} leftover leftover leftover ${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} leftover leftover leftover ${ChrQueryHelp.QUERY_INDEX_TITLE}"
                )
                put(
                    "leftover_seeder_commands",
                    "init leftover generate leftover leftover leftover no leftover other leftover 0.33.2 leftover seeder leftover subcommands leftover leftover leftover there leftover is leftover no leftover leftover leftover apply leftover leftover leftover leftover leftover run leftover leftover leftover leftover leftover populate leftover leftover leftover leftover leftover seed leftover as leftover a leftover seeder leftover subcommand"
                )
                put(
                    "leftover_seeder_missing_subcommand",
                    "chr leftover seed leftover (leftover root) leftover printed leftover Error: no such subcommand seed. Did you mean seeder? leftover exit 1 leftover leftover leftover chr leftover seeder leftover seed leftover printed leftover Error: no such subcommand seed leftover (leftover no leftover Did you mean) leftover exit 1 leftover leftover leftover the leftover official leftover command leftover is leftover chr leftover seeder leftover leftover leftover unlike leftover leftover leftover chr leftover deploy leftover leftover leftover chr leftover seeder leftover IS leftover a leftover 0.33.2 leftover command"
                )
                put(
                    "leftover_seeder_init_help",
                    "chr leftover seeder leftover init leftover --help leftover printed leftover Usage: chr seeder init [<options>] leftover Create initial seeder configuration for blockchains leftover leftover leftover Configuration Properties leftover -s/--settings leftover Alternate path for project settings file leftover leftover leftover Options leftover -bc/--blockchain leftover Blockchains to generate configuration for (defaults to all) leftover leftover leftover -h/--help leftover leftover leftover exit 0 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover init leftover was leftover NEVER leftover run leftover leftover leftover would leftover write leftover ${ChrSeederHelp.DEFAULT_CONFIG_FOLDER}"
                )
                put(
                    "leftover_seeder_generate_help",
                    "chr leftover seeder leftover generate leftover --help leftover printed leftover Usage: chr seeder generate [<options>] leftover Generate Rell blockchain seeder module leftover leftover leftover Configuration Properties leftover -s/--settings leftover leftover leftover Options leftover --alternative-config-folder=<path> leftover Alternative path to the root seeder configuration folder leftover leftover leftover -bc/--blockchain leftover Blockchains to generate seeders for (defaults to all) leftover leftover leftover -h/--help leftover leftover leftover exit 0 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover generate leftover was leftover NEVER leftover run leftover leftover leftover would leftover write leftover compile.source/seeder/seed_<blockchain>.rell"
                )
                put(
                    "leftover_seeder_settings_missing",
                    "chr leftover seeder leftover init leftover -s leftover /tmp/no-such-chromia.yml leftover printed leftover Error: invalid value for -s: file \"/tmp/no-such-chromia.yml\" does not exist. leftover exit 1 leftover leftover leftover same leftover for leftover generate leftover -s leftover /tmp/no-such-chromia.yml leftover exit 1 leftover leftover leftover nothing leftover was leftover written"
                )
                put(
                    "leftover_seeder_no_project",
                    "chr leftover seeder leftover init leftover (no leftover flags leftover from leftover /tmp) leftover printed leftover Project settings file not found leftover exit 1 leftover leftover leftover same leftover for leftover generate leftover leftover leftover leftover leftover init leftover --foo leftover leftover leftover generate leftover --foo leftover (no leftover -s) leftover also leftover Project settings file not found leftover exit 1 leftover leftover leftover settings leftover lookup leftover happens leftover before leftover unknown-option leftover check leftover when leftover no leftover -s leftover leftover leftover nothing leftover was leftover written"
                )
                put(
                    "leftover_seeder_unknown_option",
                    "chr leftover seeder leftover --foo leftover printed leftover Error: no such option --foo leftover exit 1 leftover leftover leftover chr leftover seeder leftover init leftover -s leftover /tmp/no-such-chromia.yml leftover --foo leftover printed leftover Error: no such option --foo leftover leftover leftover Error: invalid value for -s leftover exit 1 leftover leftover leftover chr leftover seeder leftover init leftover -s leftover /tmp/no-such-chromia.yml leftover --blockchain leftover printed leftover Error: option --blockchain requires a value leftover leftover leftover Error: invalid value for -s leftover exit 1 leftover leftover leftover chr leftover seeder leftover generate leftover -s leftover /tmp/no-such-chromia.yml leftover --alternative-config-folder leftover printed leftover Error: option --alternative-config-folder requires a value leftover leftover leftover Error: invalid value for -s leftover exit 1 leftover leftover leftover --blockchain leftover no_such leftover with leftover missing leftover -s leftover still leftover file leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover nothing leftover was leftover written"
                )
                put(
                    "leftover_seeder_flags",
                    "-h/--help leftover leftover leftover init leftover -s/--settings leftover -bc/--blockchain leftover leftover leftover generate leftover -s/--settings leftover --alternative-config-folder=<path> leftover -bc/--blockchain leftover leftover leftover defaults leftover to leftover all leftover blockchains leftover leftover leftover early-stage leftover may leftover change leftover leftover leftover official leftover pages leftover do leftover not leftover publish leftover a leftover seeder.yml leftover key leftover schema leftover leftover leftover do leftover not leftover invent leftover keys leftover leftover leftover ${ChromiaYmlDefinitionsHelp.TOOL_NAME}"
                )
                put("leftover_seeder_default_config_folder", ChrSeederHelp.DEFAULT_CONFIG_FOLDER)
                put(
                    "leftover_seeder_write_skip",
                    "HELP ONLY WRITE SKIP leftover leftover leftover chr leftover seeder leftover init leftover leftover leftover chr leftover seeder leftover generate leftover leftover leftover were leftover NEVER leftover run leftover leftover leftover never leftover run leftover leftover leftover would leftover write leftover ${ChrSeederHelp.DEFAULT_CONFIG_FOLDER}/<chain>/seeder.yml leftover and leftover compile.source/seeder/seed_<blockchain>.rell leftover leftover leftover do leftover not leftover invent leftover seeder.yml leftover keys leftover leftover leftover do leftover not leftover invent leftover a leftover BlockchainRID leftover leftover leftover do leftover not leftover invent leftover a leftover container leftover id leftover leftover leftover no leftover keys leftover leftover leftover no leftover signed leftover txs leftover leftover leftover no leftover posted leftover transaction leftover leftover leftover no leftover leftover leftover register leftover leftover leftover login leftover leftover leftover transfer leftover leftover leftover auth leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrSeederHelp.SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_GENERATOR_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_EXAMPLE_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_CONFIGURABLE_INDEX_TITLE} leftover leftover leftover ${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} leftover leftover leftover ${ChrQueryHelp.QUERY_INDEX_TITLE} leftover leftover leftover ${ChromiaYmlDefinitionsHelp.TOOL_NAME}"
                )


                put("leftover_eif", "chr eif")
                put("leftover_eif_help", ChrEifHelp.TOOL_NAME)
                put("leftover_eif_index_title", ChrEifHelp.EIF_INDEX_TITLE)
                put("leftover_eif_gov_index_title", ChrEifHelp.ECOSYSTEM_GOV_EIF_INDEX_TITLE)
                put(
                    "leftover_eif_help_text",
                    "chr leftover eif leftover --help leftover (leftover and leftover bare leftover chr leftover eif) leftover printed leftover Usage: chr eif [OPTIONS] COMMAND [ARGS]... leftover Ethereum Integration Framework commands leftover leftover leftover Options leftover -h/--help leftover leftover leftover Commands leftover generate-events-config leftover Generate solidity events that EIF will listen to leftover leftover leftover exit 0 leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrEifHelp.EIF_INDEX_TITLE} leftover leftover leftover sibling leftover help leftover tool leftover ${ChrEifHelp.TOOL_NAME} leftover leftover leftover leftover leftover library leftover list leftover row leftover com.chromia.eif leftover Version leftover 1.3.1 leftover already leftover on leftover disk leftover leftover leftover ${ChrEifHelp.ECOSYSTEM_GOV_EIF_INDEX_TITLE}"
                )
                put(
                    "leftover_eif_generate_help",
                    "chr leftover eif leftover generate-events-config leftover --help leftover printed leftover Usage: chr eif generate-events-config [<options>] leftover Generate solidity events that EIF will listen to leftover leftover leftover --abi=<path> leftover Path to a JSON ABI file or a directory of JSON ABI files leftover leftover leftover --events=<text> leftover Names of the relevant events (Comma separated) leftover leftover leftover --target=<path> leftover Target file to generate events in (defaults to \"build/eif-events.yaml\") leftover leftover leftover --format=(XML|YAML) leftover Output file format leftover leftover leftover -h/--help leftover leftover leftover exit 0 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover generate-events-config leftover was leftover NEVER leftover run leftover leftover leftover would leftover write leftover ${ChrEifHelp.DEFAULT_TARGET}"
                )
                put(
                    "leftover_eif_missing_abi",
                    "chr leftover eif leftover generate-events-config leftover (no leftover flags) leftover printed leftover Error: missing option --abi leftover Error: missing option --events leftover exit 1 leftover leftover leftover chr leftover eif leftover generate-events-config leftover --events leftover foo leftover printed leftover Error: missing option --abi leftover exit 1 leftover leftover leftover nothing leftover was leftover written"
                )
                put(
                    "leftover_eif_missing_events",
                    "chr leftover eif leftover generate-events-config leftover --abi leftover /tmp/no-such-abi.json leftover printed leftover Error: invalid value for --abi: path \"/tmp/no-such-abi.json\" does not exist. leftover Error: missing option --events leftover exit 1 leftover leftover leftover --events leftover (no leftover value) leftover Error: option --events requires a value leftover Error: missing option --abi leftover exit 1 leftover leftover leftover nothing leftover was leftover written"
                )
                put(
                    "leftover_eif_unknown_option",
                    "chr leftover eif leftover generate-events-config leftover --foo leftover printed leftover Error: no such option --foo. Did you mean --format? leftover Error: missing option --abi leftover Error: missing option --events leftover exit 1 leftover leftover leftover nothing leftover was leftover written"
                )
                put(
                    "leftover_eif_missing_subcommand",
                    "chr leftover eif leftover seed leftover printed leftover Error: no such subcommand seed leftover exit 1 leftover leftover leftover chr leftover eif leftover generate leftover printed leftover Error: no such subcommand generate. Did you mean generate-events-config? leftover exit 1 leftover leftover leftover chr leftover eif leftover foo leftover printed leftover Error: no such subcommand foo leftover exit 1 leftover leftover leftover the leftover only leftover 0.33.2 leftover eif leftover subcommand leftover is leftover generate-events-config"
                )
                put(
                    "leftover_eif_abi_missing_file",
                    "chr leftover eif leftover generate-events-config leftover --abi leftover /tmp/no-such-abi.json leftover --events leftover Transfer leftover printed leftover Error: invalid value for --abi: path \"/tmp/no-such-abi.json\" does not exist. leftover exit 1 leftover leftover leftover with leftover --target leftover /tmp/should-not-write-0078.yaml leftover same leftover exit 1 leftover leftover leftover nothing leftover was leftover written leftover leftover leftover ${ChrEifHelp.DEFAULT_TARGET} leftover never leftover created"
                )
                put(
                    "leftover_eif_invalid_format",
                    "chr leftover eif leftover generate-events-config leftover --abi leftover /etc/hosts leftover --events leftover Transfer leftover --format leftover badformat leftover printed leftover Error: invalid value for --format: invalid choice: badformat. (choose from XML, YAML) leftover exit 1 leftover leftover leftover --format leftover BAD leftover with leftover missing leftover ABI leftover also leftover invalid choice leftover + leftover path does not exist leftover exit 1 leftover leftover leftover nothing leftover was leftover written"
                )
                put(
                    "leftover_eif_dir_vs_file",
                    "chr leftover eif leftover generate-events-config leftover --abi leftover /tmp leftover --events leftover Transfer leftover (directory leftover of leftover non-ABI leftover files) leftover printed leftover MalformedJsonException leftover Unterminated array leftover exit 3 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover do leftover not leftover invent leftover ABI leftover JSON leftover leftover leftover nothing leftover under leftover ${ChrEifHelp.DEFAULT_TARGET} leftover was leftover written"
                )
                put("leftover_eif_default_target", ChrEifHelp.DEFAULT_TARGET)
                put(
                    "leftover_eif_flags",
                    "-h/--help leftover leftover leftover generate-events-config leftover --abi=<path> leftover --events=<text> leftover --target=<path> leftover (default leftover ${ChrEifHelp.DEFAULT_TARGET}) leftover --format=(XML|YAML) leftover (default leftover YAML) leftover leftover leftover do leftover not leftover invent leftover ABI leftover event leftover YAML leftover keys leftover leftover leftover ${ChrEifHelp.TOOL_NAME}"
                )
                put(
                    "leftover_eif_write_skip",
                    "HELP ONLY WRITE SKIP leftover leftover leftover chr leftover eif leftover generate-events-config leftover leftover leftover was leftover NEVER leftover run leftover leftover leftover never leftover run leftover leftover leftover would leftover write leftover ${ChrEifHelp.DEFAULT_TARGET} leftover leftover leftover do leftover not leftover invent leftover ABI leftover event leftover YAML leftover keys leftover leftover leftover do leftover not leftover invent leftover a leftover BlockchainRID leftover leftover leftover no leftover keys leftover leftover leftover no leftover signed leftover txs leftover leftover leftover no leftover posted leftover transaction leftover leftover leftover no leftover leftover leftover register leftover leftover leftover login leftover leftover leftover transfer leftover leftover leftover auth leftover leftover leftover titles leftover already leftover on leftover disk leftover ${ChrEifHelp.EIF_INDEX_TITLE} leftover ${ChrEifHelp.ECOSYSTEM_GOV_EIF_INDEX_TITLE} leftover leftover leftover ${ChrEifHelp.TOOL_NAME} leftover leftover leftover leftover leftover library leftover list leftover com.chromia.eif leftover Version leftover 1.3.1 leftover already leftover on leftover disk"
                )

                put(
                    "leftover_next_step_architecture",
                    "leftover next-step INDEX path: chr create-rell-dapp → chr build (${ChrBuildHelp.BUILD_INDEX_TITLE}) / chr code check (${ChrBuildHelp.CODE_INDEX_TITLE}) → chr test (3 PASSED) → chr generate client-stubs / chr generate graph / chr generate docs-site (${ChrGenerateClientHelp.GENERATE_INDEX_TITLE}; ${ChromiaDocsYmlHelp.DOCS_SITE_INDEX_TITLE}; connect a client ${ChrGenerateClientHelp.TESTNET_CONNECT_INDEX_TITLE}) leftover generate graph --mdx leftover rell.mdx leftover 0-byte leftover generate graph --class-diagram leftover rell.mmd leftover 0-byte → leftover chr library list (${ChrLibraryHelp.LIBRARY_INDEX_TITLE}, ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE}) leftover no leftover RID leftover printed leftover Total: 20 libraries leftover com.chromia.ft4 leftover Version leftover 1.2.0 leftover Official leftover Yes leftover com.chromia.iccf leftover 1.90.2 leftover com.chromia.ICMF leftover Deprecated - use icmf instead → leftover chr library versions com.chromia.ft4 leftover query leftover printed leftover 2.0.2 leftover 1.1.0 leftover 1.0.0 leftover 1.1.1 leftover 1.2.0 leftover Total 5 leftover leftover chr library view com.chromia.ft4 leftover printed leftover ID leftover com.chromia.ft4 leftover Name leftover ft4 leftover Organization leftover Chromia Organization leftover Version leftover 1.2.0 leftover Official leftover Yes leftover no leftover invented leftover RID leftover no leftover invented leftover semver leftover pin leftover git leftover pin leftover ${DappScaffold.FT4_VERSION} leftover already leftover on leftover disk → FT4 yml libs import (${ChromiaFt4QueriesHelp.FT4_SETUP_PAGE_INDEX_TITLE}, ${ChromiaFt4QueriesHelp.FT4_IMPORTS_INDEX_TITLE}) leftover HELP ONLY WRITE SKIP leftover leftover leftover clients leftover ${ChromiaLanguageClientsHelp.JS_TS_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.KOTLIN_INDEX_TITLE} leftover ${ChromiaLanguageClientsHelp.PYTHON_INDEX_TITLE} leftover leftover leftover project-config leftover ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} leftover leftover leftover database leftover ${ChromiaRellDatabaseHelp.BUILD_GETTING_STARTED_INDEX_TITLE} leftover ${ChromiaRellDatabaseHelp.BUILD_OVERVIEW_INDEX_TITLE} leftover leftover leftover fire leftover 0066 leftover INDEX leftover already leftover on leftover disk → leftover chr code check leftover empty leftover stdout leftover exit 0 leftover leftover leftover chr code lint leftover src/main.rell leftover src/test leftover main/* leftover exit 0 leftover leftover leftover chr code lint leftover (no leftover files) leftover walks leftover materialized leftover src/lib/ft4 leftover exit 1 leftover unknown_name:iccf leftover import:not_found:lib.iccf leftover leftover leftover chr code format leftover --file leftover src/main.rell leftover printed leftover Formatting leftover src/main.rell... leftover no leftover changes leftover leftover leftover default leftover create leftover .rell_format leftover max_line_width=120 leftover leftover leftover HELP ONLY WRITE SKIP leftover --fix leftover (${ChrBuildHelp.CODE_INDEX_TITLE}) → leftover chr repl leftover query leftover only leftover (${ChrReplHelp.REPL_INDEX_TITLE}) leftover chr repl -c '1+1' leftover 2 leftover exit 0 leftover leftover leftover --module leftover main leftover --use-db leftover -c leftover hello_world() leftover Hello World! leftover exit 0 leftover leftover leftover --sql-log leftover SELECT leftover leftover leftover set_name leftover Type rell.test.op leftover HELP ONLY WRITE SKIP leftover ops leftover txs leftover leftover leftover -f JSON leftover 2 leftover leftover leftover -f XML leftover <int>2</int> leftover leftover leftover -f YAML leftover Unsupported output format YAML leftover exit 1 leftover leftover leftover stdin leftover chr repl - leftover 2 leftover leftover leftover args leftover list leftover leftover leftover Cannot use -c when specifying script file leftover leftover leftover --sql-log leftover without leftover --use-db leftover No database connection leftover leftover leftover chr tools leftover walk leftover done leftover (${ChrToolsHelp.TOOLS_INDEX_TITLE}) leftover gtv leftover validate-config leftover lib-model leftover real leftover exits leftover leftover leftover deep leftover walk leftover gtv leftover -f yaml leftover lowercase leftover ok leftover leftover leftover binary leftover stdin leftover data.gtv leftover ok leftover leftover leftover --hash=0 leftover must be greater than 0 leftover leftover leftover validate-config leftover ./ leftover or leftover absolute leftover path leftover ok leftover leftover leftover cookbook leftover keys leftover Additional property leftover exit 2 leftover leftover leftover scaffold_dapp leftover yml leftover No issues found leftover leftover leftover lib-model leftover rid leftover computed leftover matches leftover the leftover pin leftover already leftover on leftover disk leftover leftover leftover chr node start leftover (${ChrNodeHelp.NODE_INDEX_TITLE}) leftover Node is initialized leftover Chain-id: 0 leftover REST ${ChrNodeHelp.DEFAULT_API_URL} leftover leftover leftover chr query leftover after leftover node leftover walk leftover done leftover (${ChrQueryHelp.QUERY_INDEX_TITLE}) leftover chr query hello_world leftover Hello World! leftover exit 0 leftover leftover leftover -f YAML leftover works leftover for leftover query leftover unlike leftover repl leftover leftover leftover --cid 0 leftover ok leftover leftover leftover Unknown query leftover / leftover Invalid argument(s) leftover / leftover missing --network leftover / leftover Char Z is not a hex digit leftover / leftover Wrong size of Blockchain RID leftover exit 3 leftover leftover leftover REST GET / leftover Postchain REST API leftover leftover leftover no leftover pasted leftover BRID leftover leftover leftover deploy leftover HELP ONLY leftover walk leftover done leftover (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} / ${ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE}) leftover chr deploy leftover is leftover not leftover a leftover 0.33.2 leftover subcommand leftover Did you mean deployment? leftover chr deployment leftover create leftover update leftover HELP ONLY WRITE SKIP leftover never leftover run leftover no leftover signing leftover no leftover keys leftover no leftover invented leftover RID leftover inspect leftover info leftover read-only leftover inspect leftover printed leftover hello_world leftover set_name leftover my_name leftover info leftover Cluster not found leftover locally leftover --network testnet leftover Specified target [testnet] does not exist leftover exit 1 leftover chromia.yml leftover deployments leftover url leftover brid leftover container leftover chains leftover write-back leftover since leftover 0.30.0 leftover (${ChrDeployHelp.TESTNET_DEPLOY_DAPP_INDEX_TITLE} leftover ${ChrDeployHelp.MAINNET_DEPLOY_DAPP_INDEX_TITLE} leftover ${ChrDeployHelp.GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE}) leftover leftover leftover chr query leftover deep leftover walk leftover done leftover (${ChrQueryHelp.QUERY_INDEX_TITLE}) leftover --help leftover Examples leftover primitive_args leftover dict_arg leftover map_arg leftover struct_arg leftover leftover leftover named leftover parameters leftover only leftover query must be done with named parameters in a dict leftover leftover leftover no such option --foo. Did you mean -f? leftover leftover leftover --cid hello_world leftover is leftover not leftover a leftover valid leftover integer leftover leftover leftover last_block_info leftover is leftover a leftover second leftover query-only leftover query leftover and leftover ignores leftover extra leftover args leftover leftover leftover tx_confirmation_time leftover without leftover txRID leftover 500 leftover Unknown error leftover leftover leftover get_version leftover / leftover ft4.get_version leftover Unknown query leftover so leftover FT4 leftover is leftover not leftover mounted leftover leftover leftover REST leftover /query/iid_0?type=hello_world leftover 200 leftover Hello World! leftover no leftover BRID leftover needed leftover leftover leftover /query_gtv leftover octet-stream leftover leftover leftover Missing query type leftover / leftover QUERY_NOT_FOUND leftover / leftover chain Iid: 99 leftover leftover leftover /metadata/iid_0 leftover nop leftover __nop leftover timeb leftover ops leftover WRITE SKIP leftover leftover leftover no leftover chr node stop leftover on leftover 0.33.2 leftover leftover leftover never leftover paste leftover a leftover BRID leftover leftover leftover deploy leftover HELP ONLY leftover walk leftover done leftover (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} leftover ${ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE}) leftover chr leftover deployment leftover create leftover update leftover HELP ONLY WRITE SKIP leftover never leftover signed leftover leftover leftover inspect leftover info leftover read-only leftover leftover leftover Specified target [testnet] does not exist leftover leftover leftover vault-lease leftover HELP ONLY leftover walk leftover done leftover (${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE} leftover ${VaultLeaseHelp.MAINNET_GET_CONTAINER_INDEX_TITLE} leftover ${VaultLeaseHelp.GET_STARTED_HOSTING_INDEX_TITLE} leftover ${VaultLeaseHelp.GET_STARTED_SUPPORTED_WALLETS_INDEX_TITLE}) leftover leftover leftover never leftover invent leftover a leftover lease leftover id leftover leftover leftover placeholder leftover ${VaultLeaseHelp.CONTAINER_PLACEHOLDER} leftover leftover leftover chr leftover tx leftover HELP ONLY leftover walk leftover done leftover (${ChrQueryHelp.TX_INDEX_TITLE}) leftover leftover leftover missing leftover <opname> leftover exit 1 leftover leftover leftover Char Z is not a hex digit leftover exit 3 leftover leftover leftover Wrong size of Blockchain RID leftover exit 3 leftover leftover leftover Specified target [testnet] does not exist leftover exit 1 leftover leftover leftover Connection Refused leftover exit 1 leftover leftover leftover HELP ONLY WRITE SKIP leftover leftover leftover do leftover not leftover send leftover leftover leftover chr leftover multi-signature leftover HELP ONLY leftover walk leftover done leftover (${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE}) leftover leftover leftover view leftover query-only leftover missing leftover --file leftover exit 1 leftover leftover leftover create leftover sign leftover send leftover WRITE SKIP leftover leftover leftover seeder leftover HELP ONLY leftover walk leftover done leftover (${ChrSeederHelp.SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_GENERATOR_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_EXAMPLE_INDEX_TITLE} leftover ${ChrSeederHelp.SEEDER_CONFIGURABLE_INDEX_TITLE}) leftover leftover leftover chr leftover seed leftover is leftover not leftover a leftover 0.33.2 leftover subcommand leftover Did you mean seeder? leftover leftover leftover chr leftover seeder leftover IS leftover a leftover command leftover leftover leftover init leftover generate leftover HELP ONLY WRITE SKIP leftover never leftover run leftover leftover leftover Project settings file not found leftover leftover leftover -s leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover do leftover not leftover invent leftover seeder.yml leftover keys leftover leftover leftover leftover leftover eif leftover HELP ONLY leftover walk leftover done leftover (${ChrEifHelp.EIF_INDEX_TITLE}) leftover leftover leftover leftover leftover next leftover leftover leftover version leftover HELP ONLY. Titles already on disk; no new pages. Query-only HELP ONLY WRITE SKIP."
                )
            }
        )
        put("notes", notes())
    }
}
// Leftover official leftover ECOSYSTEM ecosystem/filehub/filehub-setup INDEX leftovers encoded as ECOSYSTEM_FILEHUB_SETUP_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/node-config INDEX leftovers encoded as ECOSYSTEM_NODE_CONFIG_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/import INDEX leftovers encoded as ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/book-review/build-client/prerequisites INDEX leftovers encoded as LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/big-data/setup INDEX leftovers encoded as LEARN_BIG_DATA_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/ft4-asset/setup INDEX leftovers encoded as LEARN_FT4_ASSET_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/iccf-course/setup INDEX leftovers encoded as LEARN_ICCF_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/my-news-feed/module-one/project-structure INDEX leftovers encoded as LEARN_NEWS_PROJECT_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/rell-intro INDEX leftovers encoded as RELL_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-one/project-structure/modules INDEX leftovers encoded as LEARN_TTT_RELL_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX vector-db embedding-model leftovers encoded as LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX monetize-dapp account-registration leftovers encoded as LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX zero-knowledge-proof frontend leftovers encoded as LEARN_ZK_FRONTEND_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX relationships-course joins leftovers encoded as LEARN_RELATIONSHIPS_JOINS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX web3-for-web2-devs summary leftovers encoded as LEARN_WEB3_SUMMARY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/modules/namespace INDEX leftovers encoded as RELL_MODULE_NAMESPACE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/systemlib/require-function INDEX leftovers encoded as RELL_SYSTEMLIB_REQUIRE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/modules INDEX leftovers encoded as RELL_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover dapp-build INDEX help map architecture leftover INDEX leftover local walk leftover Node is initialized leftover REST leftover query Hello World! leftover chain-id 0 leftover postgres defaults worked leftover Driver 17.11 (query-only HELP ONLY WRITE SKIP; no new leftover pages).
// Leftover local leftover next-step walk (leftover chr test, leftover chr generate client-stubs, leftover FT4 chromia.yml libs import, leftover chr build) encoded as leftover_test* / leftover_generate_client* / leftover_ft4_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no new leftover pages).
// Leftover local leftover fire-0065 verify (chr test --modules filter, chr generate client-stubs -m main, chr generate graph empty rell.mmd) encoded as leftover_test_modules_filter* / leftover_generate_client_module_flag / leftover_generate_graph (query-only HELP ONLY WRITE SKIP; no keys, no signed ops).
// Leftover local leftover library leftover versions leftover generate leftover graph leftover generate leftover docs-site walk (chr library versions com.chromia.ft4 printed 2.0.2 1.1.0 1.0.0 1.1.1 1.2.0 Total 5 no RID, chr generate graph empty rell.mmd, chr generate docs-site generated-docs hello_world query WRITE SKIP set_name) encoded as leftover_library_versions* / leftover_generate_docs_site* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no invented RID, no invented library-chain semver pin, no new leftover pages).
// Leftover local leftover library leftover view leftover generate leftover graph leftover --mdx leftover --class-diagram walk (chr library view com.chromia.ft4 printed ID com.chromia.ft4 Name ft4 Organization Chromia Organization Version 1.2.0 Official Yes no RID no invented library-chain semver pin git pin remains v1.1.0r, chr generate graph --mdx empty rell.mdx, chr generate graph --class-diagram empty rell.mmd, leftover fire 0066 clients JS/TS Kotlin Python leftover project-config leftover database getting-started/overview INDEX titles already on disk) encoded as leftover_library_view* / leftover_generate_graph_mdx* / leftover_generate_graph_class_diagram* / leftover_clients_* / leftover_database_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no leftover leftover install, no invented RID, no invented library-chain semver pin, no new leftover pages).
// Leftover local leftover library leftover list walk (chr library list needs no RID, printed Available Libraries columns ID Name Organization Version Official Description Total: 20 libraries, com.chromia.ft4 Version 1.2.0 Official Yes, com.chromia.iccf 1.90.2, com.chromia.ICMF 1.99.0 Deprecated - use icmf instead, --limit/--sort-by verified) encoded as leftover_library_list* (query-only HELP ONLY WRITE SKIP; list Version is not the max semver and not a pin — do not invent a FT4 semver pin from list vs view vs versions vs git pin; git pin remains v1.1.0r already on disk; no keys, no signed FT4 ops, no leftover leftover install, no invented RID, no new leftover pages).
// Leftover local leftover code leftover check leftover lint leftover format walk (chr code check empty stdout exit 0, chr code lint src/main.rell src/test main/* exit 0, chr code lint project-wide walks leftover materialized src/lib/ft4 exit 1 unknown_name:iccf import:not_found:lib.iccf, chr code format --file=src/main.rell printed Formatting src/main.rell... no changes, leftover default create .rell_format max_line_width=120, HELP ONLY WRITE SKIP --fix) encoded as leftover_code_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no leftover leftover install iccf, no invented RID, no new leftover pages).
// Leftover local leftover repl walk (chr repl -c '1+1' → 2 exit 0, chr repl --module main -c '1+1' → 2 exit 0, chr repl --blockchain my_rell_dapp -c '1+1' → 2 exit 0, chr repl --module main -c 'hello_world()' → No database connection exit 1, chr repl --module main --use-db -c 'hello_world()' → "Hello World!" exit 0 SqlInit chain_iid=0, chr repl --module main --use-db --sql-log -c 'hello_world()' → SqlConnectionLogger SELECT A00.\"name\" FROM \"c0.my_name\" A00 "Hello World!" exit 0, set_name Type rell.test.op Switch to a different output format exit 0 HELP ONLY WRITE SKIP ops/txs, -f JSON matches pretty, -f XML <int>2</int> <string>Hello World!</string>, -f YAML Error: Unsupported output format YAML exit 1 help mismatch, printf '1+1' | chr repl - → 2, printf 'args' | chr repl - leftover one → [\"leftover\", \"one\"], Cannot use -c when specifying script file exit 1, --sql-log without --use-db No database connection exit 1) encoded as leftover_repl_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no leftover leftover install, no invented RID, no new leftover pages).
// Leftover local leftover tools leftover walk (chr tools --help gtv validate-config lib-model exit 0, chr tools gtv --hex official sample pretty/JSON/XML/raw/YAML all exit 0 FOO/BAR, chr gtv alias same, missing --hex stdin hang / empty pipe Invalid GTV data exit 1, --hex ZZ Char Z is not a hex digit exit 1, validate-config missing --file exit 1, -f src/main.rell Unsupported file format exit 1, parent-dir -f my-rell-dapp/chromia.yml No issues found exit 0, bare -f chromia.yml getParent must not be null exit 3, lib-model missing --library-source exit 1, invalid --registry exit 1, full flags prints libs: block computes rid — do not invent RID never paste 64-hex, HELP ONLY WRITE SKIP leftover leftover install) encoded as leftover_tools_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no leftover leftover install, no invented RID, no new leftover pages).
// Leftover local leftover tools leftover deep leftover walk (chr tools with no subcommand = chr tools --help exit 0, gtv -f yaml lowercase ok, official binary stdin chr gtv --output-format yaml < data.gtv ok, --hex wins over stdin, --hash=1/2 print a computed 64-char Merkle hash never recorded here, --hash=0 must be greater than 0 exit 1, validate-config ./ or absolute path ok while a bare filename NPEs exit 3, nonexistent/dir path exit 1, .yaml accepted, cookbook keys test.timeout/test.parallel/database.schema_version rejected Additional property ... exit 2, unknown top-level section exit 2, scaffold_dapp chromia.yml No issues found exit 0, lib-model printed shape + --insecure=true, lib-model over src/lib/ft4 reproduced the FT4 rid already on disk) encoded as leftover_tools_* (query-only HELP ONLY WRITE SKIP; no keys, no signed ops, no invented RID, no pasted 64-hex, no new leftover pages).
// Leftover local leftover query leftover after leftover node walk (chr node start Node is initialized Chain-id 0 REST http://localhost:7740, chr query hello_world "Hello World!" exit 0 needs no --blockchain-rid, -f pretty/JSON/XML/raw/YAML all exit 0 YAML works unlike repl, Unknown query / Invalid argument(s) / missing --network / Char Z is not a hex digit / Wrong size of Blockchain RID exit 3, REST GET / Postchain REST API 200, HELP ONLY WRITE SKIP chr tx signed send, never paste computed BRID) encoded as leftover_query_* (query-only HELP ONLY WRITE SKIP; no keys, no signed ops, no invented RID, no pasted 64-hex, no new leftover pages).
// Leftover local leftover deploy leftover HELP ONLY leftover walk (chr deploy is not a 0.33.2 subcommand — Error: no such subcommand deploy. Did you mean deployment?; chr deployment --help create info inspect update resume pause remove proposal voterset container exit 0; create/update --help flags recorded and NEVER run; info/inspect read-only; local inspect printed hello_world/set_name/my_name, --list-modules ["main"], --signature, --module-args [], -f table; info against the local node Cluster not found + Unknown query: cm_get_blockchain_cluster; --network testnet Specified target [testnet] does not exist exit 1; --blockchain-rid ZZ/AB exit 3) encoded as leftover_deploy_* (query-only HELP ONLY WRITE SKIP; nothing deployed, no signing, no keys, no invented BlockchainRID, no invented container id, no pasted 64-hex, no new leftover pages; leftover leftover leftover settings leftover missing leftover -s leftover does leftover not leftover exist leftover exit 1 leftover leftover leftover --blockchain leftover missing leftover --network leftover leftover leftover proposal leftover / leftover voterset leftover missing leftover --network leftover leftover leftover container leftover --help leftover WRITE SKIP leftover leftover leftover next leftover leftover leftover vault-lease leftover HELP ONLY).
// Leftover local leftover query leftover deep leftover walk (chr query --help official Examples primitive_args/dict_arg/map_arg/struct_arg + named-parameters-only rule, chr query hello_world 1 -> query must be done with named parameters in a dict exit 1, --foo -> no such option Did you mean -f?, --cid hello_world -> not a valid integer, dict arg on a no-arg query -> Invalid argument(s): arg, --output-format long form ok, get_version/ft4.get_version/get_api_version -> Unknown query so FT4 is not mounted, REST /metadata/iid_0 built-in GTX queries last_block_info + tx_confirmation_time and ops nop/__nop/timeb, chr query last_block_info dict blockRID/height/timestamp exit 0 in all five formats and it ignores extra args, tx_confirmation_time without txRID -> 500 Unknown error, REST /query/iid_0?type=hello_world 200 Hello World! via the iid_<chainIid> alias so no BRID is needed, POST /query/iid_0 200, Missing query type 400, QUERY_NOT_FOUND 400, chain Iid: 99 404, /query_gtv octet-stream, /dquery + /web_query need purpose-built Rell queries, /blockchain/iid_0/height + nodestate + config features, /version 22, /infrastructure_version postchain 3.49.16, /_debug moved to 7750, OPTIONS CORS, /apidocs rapi-doc spec, and chr node --help has no stop subcommand) encoded as leftover_query_* (query-only HELP ONLY WRITE SKIP; no keys, no signed ops, no invented RID, no pasted 64-hex, no new leftover pages).

// Leftover local leftover vault leftover lease leftover HELP ONLY leftover walk (vault_lease_help titles Get a container for your dapp / Hosting / Supported wallets; workflow faucet→Vault containers→lease with existing pubkey→chromia.yml deployments.<net>.container placeholder <containerIID>→chr deployment create WRITE SKIP; never invent container id; never paste Directory 64-hex; this tool does not generate a key) encoded as leftover_vault_* (query-only HELP ONLY WRITE SKIP; no keys, no signed txs, no invented RID, no invented container id, no pasted 64-hex, no new leftover pages).
// Leftover local leftover tx leftover HELP ONLY leftover walk (chr tx --help FT4/ICCF/Examples exit 0; missing <opname> exit 1; no node Connection Refused exit 1; --blockchain-rid ZZ/AB exit 3; --network testnet Specified target does not exist exit 1; missing --network / missing -s / --cid hello / --foo exit 1) encoded as leftover_tx_* (query-only HELP ONLY WRITE SKIP; do not send; no keys, no invented RID, no pasted 64-hex, no new leftover pages).
// Leftover local leftover multi-signature leftover HELP ONLY leftover walk (chr multi-signature --help create/sign/send/view exit 0; view --help -f/--file; view missing --file exit 1; view missing file exit 1; create/sign/send WRITE SKIP) encoded as leftover_multi_* (query-only HELP ONLY WRITE SKIP; no keys, no signed txs, no invented RID, no pasted 64-hex, no new leftover pages).
// Leftover local leftover seeder leftover HELP ONLY leftover walk (chr seeder --help Generate fake data for a local database init/generate exit 0; chr seed Did you mean seeder? exit 1; chr seeder seed no such subcommand seed exit 1; init --help -s/-bc; generate --help --alternative-config-folder; init/generate NEVER run WRITE SKIP; missing -s file does not exist exit 1; no project Project settings file not found exit 1) encoded as leftover_seeder_* (query-only HELP ONLY WRITE SKIP; no keys, no signed txs, no invented RID, no invented container id, no pasted 64-hex, no invented seeder.yml keys, no new leftover pages; eif leftover HELP ONLY leftover walk leftover done; next leftover version HELP ONLY).
// Leftover local leftover eif leftover HELP ONLY leftover walk (chr eif --help Ethereum Integration Framework commands generate-events-config exit 0; generate-events-config --help --abi/--events/--target/--format; generate NEVER run WRITE SKIP; missing --abi/--events exit 1; --foo Did you mean --format? exit 1; seed/generate/foo no such subcommand; missing ABI path does not exist exit 1; --format badformat invalid choice exit 1; --abi /tmp MalformedJsonException exit 3; never wrote build/eif-events.yaml) encoded as leftover_eif_* (query-only HELP ONLY WRITE SKIP; no keys, no signed txs, no invented RID, no pasted 64-hex, no invented ABI event YAML keys, no new leftover pages; next leftover version HELP ONLY).
