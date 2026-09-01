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
        Official BUILD cli/commands/create-rell-dapp ($CREATE_RELL_DAPP_INDEX_URL 307 $CREATE_RELL_DAPP_INDEX_URL_SLASH 200 $CREATE_RELL_DAPP_INDEX_TITLE): intro Usage chr create-rell-dapp [<options>] [<name>] Generates a template project Template projects Minimal Plain Plain-Multi Plain-library Asset Management Options -d, --base-dir=<path> --template=(plain|plain-multi|minimal|plain-library|asset-management) --devcontainer -h, --help Arguments <name> Dapp name chromia.yml src/main.rell src/test arithmetic_test.rell data_test.rell Query-only WRITE SKIP HELP ONLY skip signed txs no sample keys no invented 64-hex do not invent flags do not document chr tx signed send keygen samples skip unofficial chromia.yml keys no keygen.
        Official GET-STARTED get-started/create-dapp INDEX ($CREATE_DAPP_INDEX_URL 307 $CREATE_DAPP_INDEX_URL_SLASH 200 $CREATE_DAPP_INDEX_TITLE): HELP ONLY Query-only WRITE SKIP Origin parked skip signed txs no sample keys no invented 64-hex no keygen do not invent flags BRIDs do not document chr tx signed send keygen samples.
        Official GET-STARTED get-started/create-dapp/run-dapp-cli INDEX ($GET_STARTED_RUN_DAPP_CLI_INDEX_URL 307 $GET_STARTED_RUN_DAPP_CLI_INDEX_URL_SLASH 200 $GET_STARTED_RUN_DAPP_CLI_INDEX_TITLE): slash title HELP ONLY Query-only WRITE SKIP Origin parked skip signed txs no sample keys no invented 64-hex BRIDs skip sign recipe no keygen do not invent flags do not document chr tx signed send keygen samples.
        Official ECOSYSTEM ecosystem/filehub/filehub-setup INDEX ($ECOSYSTEM_FILEHUB_SETUP_INDEX_URL 307 $ECOSYSTEM_FILEHUB_SETUP_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_FILEHUB_SETUP_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/nodes/node-config INDEX ($ECOSYSTEM_NODE_CONFIG_INDEX_URL 307 $ECOSYSTEM_NODE_CONFIG_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_NODE_CONFIG_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/import INDEX ($ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_URL 307 $ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/build-client/prerequisites INDEX ($LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_URL 301 $LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/big-data/setup INDEX ($LEARN_BIG_DATA_SETUP_INDEX_URL 301 $LEARN_BIG_DATA_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_BIG_DATA_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/ft4-asset/setup INDEX ($LEARN_FT4_ASSET_SETUP_INDEX_URL 301 $LEARN_FT4_ASSET_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_FT4_ASSET_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/iccf-course/setup INDEX ($LEARN_ICCF_SETUP_INDEX_URL 301 $LEARN_ICCF_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_ICCF_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/project-structure INDEX ($LEARN_NEWS_PROJECT_STRUCTURE_INDEX_URL 301 $LEARN_NEWS_PROJECT_STRUCTURE_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_PROJECT_STRUCTURE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official install (devcontainer path): $INSTALL_URL
        Generates a template project. Optional <name> is the folder; CHANGELOG default with no name is `$DEFAULT_FOLDER`.
        Official templates: ${templates.joinToString(", ")}.
        `--devcontainer` adds a Docker / VS Code devcontainer (CLI + Postgres + PMC).
        After create, pin compile.rellVersion ${DappScaffold.RELL_VERSION} and merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION}
        (docs / templates may still show 0.14.9 — source pin wins). Official 0.33.2 `chr create-rell-dapp` (no name, no --template) is silent and writes `$DEFAULT_FOLDER`; default chromia.yml is compile.rellVersion 0.14.5, blockchains.my_rell_dapp.module main, database.schema schema_my_rell_dapp, test.modules test — no host/user/password, no merkle_hash_version. Then optional `chr install` / `chr build` (compile step; default artifact build/my_rell_dapp.xml) / `chr test`. `chr node start` compiles Type=BLOCKCHAIN again. Built XML auto merkle_hash_version 2. There is no top-level `chr compile` in 0.33.x.
        Official first-run (run-dapp-cli $RUN_DAPP_URL, query-only):
        `chr create-rell-dapp` → `cd my-rell-dapp` → `chr node start` → `chr query hello_world`
        → `"Hello World!"`. Local walk: Postgres postchain database/user/password defaults actually worked org.postgresql.Driver 17.11 JDBC ${ChrNodeHelp.DEFAULT_JDBC}; node prints Node is initialized REST ${ChrNodeHelp.DEFAULT_API_URL} REST GET / 200 chain-id 0 computed BRID (do not invent hex); create silent 0-byte stdout/stderr artifact build/my_rell_dapp.xml yml module main test.modules test no merkle_hash_version; `chr query hello_world` from project dir needs no --blockchain-rid result Hello World!. Official query page also shows the explicit
        `chr query --blockchain-rid <BlockchainRID> hello_world`. Do not invent a BRID.
        Official Hello World query is `hello_world()` = `"Hello %s!".format(my_name.name)`
        with `object my_name { mutable name = "World"; }`. Default main.rell also has operation set_name. Skipped: key generation and the set_name write path. FT4 WRITE SKIP (hello_world has no FT4 auth register login transfer mint burn create-accounts).
        NEVER import ${DappScaffold.forbiddenModules.joinToString(", ")}.
        Cookbook chromia.yml keys test.timeout, test.parallel, database.schema_version are not official — do not use them.
        Official RELL rell/rell-intro INDEX ($RELL_INTRO_INDEX_URL 307 $RELL_INTRO_INDEX_URL_SLASH 200 H1 $RELL_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/tic-tac-toe/module-one/project-structure/modules INDEX ($LEARN_TTT_RELL_MODULES_INDEX_URL 301 $LEARN_TTT_RELL_MODULES_INDEX_URL_SLASH 200 H1 $LEARN_TTT_RELL_MODULES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX vector-db embedding-model ($LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_URL 301 $LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_URL_SLASH GET 200 H1 $LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX monetize-dapp account-registration ($LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_URL 301 $LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_URL_SLASH GET 200 H1 $LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof frontend ($LEARN_ZK_FRONTEND_INDEX_URL 301 $LEARN_ZK_FRONTEND_INDEX_URL_SLASH GET 200 H1 $LEARN_ZK_FRONTEND_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX relationships-course joins ($LEARN_RELATIONSHIPS_JOINS_INDEX_URL 301 $LEARN_RELATIONSHIPS_JOINS_INDEX_URL_SLASH GET 200 H1 $LEARN_RELATIONSHIPS_JOINS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX web3-for-web2-devs summary ($LEARN_WEB3_SUMMARY_INDEX_URL 301 $LEARN_WEB3_SUMMARY_INDEX_URL_SLASH GET 200 H1 $LEARN_WEB3_SUMMARY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/modules/namespace INDEX ($RELL_MODULE_NAMESPACE_INDEX_URL 307 $RELL_MODULE_NAMESPACE_INDEX_URL_SLASH 200 H1 $RELL_MODULE_NAMESPACE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/require-function INDEX ($RELL_SYSTEMLIB_REQUIRE_INDEX_URL 307 $RELL_SYSTEMLIB_REQUIRE_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_REQUIRE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/modules INDEX ($RELL_MODULES_INDEX_URL 307 $RELL_MODULES_INDEX_URL_SLASH 200 H1 $RELL_MODULES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official dapp-build INDEX help map ($CREATE_RELL_DAPP_INDEX_TITLE $CREATE_DAPP_INDEX_TITLE $GET_STARTED_RUN_DAPP_CLI_INDEX_TITLE $RELL_INTRO_INDEX_TITLE $RELL_MODULES_INDEX_TITLE $LEARN_FT4_ASSET_SETUP_INDEX_TITLE $LEARN_NEWS_PROJECT_STRUCTURE_INDEX_TITLE ${ChrBuildHelp.LEARN_HOME_INDEX_TITLE} ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} ${ChrNodeHelp.NODE_INDEX_TITLE} ${ChromiaVectorSearchHelp.LEARN_TAGS_VECTOR_DB_INDEX_TITLE} ${ChromiaVectorSearchHelp.LEARN_VECTOR_DB_SETUP_INDEX_TITLE} $LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_TITLE ${ChromiaRellExpressionsHelp.LEARN_VECTOR_DB_USE_CASES_INDEX_TITLE} HELP ONLY WRITE SKIP). Query-only. Architecture INDEX map: LEARN_HOME + LEARN install CLI → chr create-rell-dapp → chromia.yml project settings + project structure → Rell language/expressions/statements/types → FT4 WRITE SKIP (hello_world has no FT4 auth register login transfer) → LEARN_TAGS_VECTOR_DB (official tag URL currently 404; live Vector DB INDEX is learn vector-db course) + vector-db course INDEX → local node → chr query hello_world → deploy. Local 0.33.2 walk: silent create default folder my-rell-dapp default chromia.yml compile.rellVersion 0.14.5 blockchains.my_rell_dapp.module main database.schema schema_my_rell_dapp test.modules test no host/user/password no merkle_hash_version chr build artifact build/my_rell_dapp.xml Node is initialized REST ${ChrNodeHelp.DEFAULT_API_URL} REST GET / 200 query Hello World! chain-id 0 query from project dir needs no --blockchain-rid Postgres postchain defaults actually worked org.postgresql.Driver 17.11 JDBC ${ChrNodeHelp.DEFAULT_JDBC}. Points at INDEX titles already on disk plus sibling help tools. Does not encode new pages. HELP ONLY WRITE SKIP.
        Local 0.33.2 next-step walk (test generate-client FT4 yml import, query-only): `chr test` from project dir ran test.arithmetic_test:test_foo test.arithmetic_test:test_bar test.data_test:test_add_name SUMMARY 0 FAILED / 3 PASSED / 3 TOTAL ***** OK *****; generate subcommands client-stubs graph docs-site there is no top-level chr generate-client in 0.33.2 (root help prints generate only) `chr generate client-stubs --typescript -d generated-ts` printed Created files in generated-ts main/main.ts output path shape <target>/<module>/<module>.ts helloWorldQueryObject QueryObject<string> name hello_world setNameOperation Operation set_name imports postchain-client Operation QueryObject RawGtv no keys no signed send; FT4 chromia.yml libs import git shape registry ${DappScaffold.FT4_REGISTRY} path ${DappScaffold.FT4_PATH} tagOrBranch ${DappScaffold.FT4_VERSION} insecure false `chr install` printed Failed to install library ft4: Unknown error yet src/lib/ft4 materialized module.rell version.rell ft4 get_version 1.1.0 get_api_version ${DappScaffold.FT4_API} @mount('ft4'); retry with rid hung >2min killed do not invent a RID; `chr build` after yml change printed Building Blockchain: my_rell_dapp artifact build/my_rell_dapp.xml rebuilt and `chr test` still 3 PASSED. FT4 yml lib import only — no Rell import of ft4 in main.rell no moduleArgs HELP ONLY WRITE SKIP register login transfer auth mint burn create-accounts no signed FT4 ops no keys.
        Local 0.33.2 fire-0065 verify (query-only): `chr test` still SUMMARY 0 FAILED / 3 PASSED / 3 TOTAL ***** OK *****; `chr test --modules test.data_test` ran only test.data_test:test_add_name SUMMARY 0 FAILED / 1 PASSED / 1 TOTAL ***** OK *****; `chr generate client-stubs --typescript -m main -d generated-ts` printed Created files in generated-ts [main/main.ts] languages --kotlin --typescript --javascript --python module flag -m/--module comma-delimited; `chr generate graph -d generated-graph` printed Created files in generated-graph [rell.mmd] file empty for hello_world (no entities); FT4 chromia.yml libs import unchanged HELP ONLY WRITE SKIP register login transfer auth accounts no keys no signed ops no invented 64-hex.
        Local 0.33.2 library versions generate graph generate docs-site walk (query-only): `chr library versions com.chromia.ft4` printed Available Versions 2.0.2 1.1.0 1.0.0 1.1.1 1.2.0 Total: 5 versions default mainnet library-chain no --brid do not invent a RID do not invent a library-chain semver pin from this list git pin remains ${DappScaffold.FT4_VERSION} already on disk HELP ONLY WRITE SKIP register login transfer auth mint burn create-accounts no Rell import of ft4 no moduleArgs no signed ops no keys; `chr generate graph -d generated-graph` printed Created files in generated-graph [rell.mmd] file empty 0-byte for hello_world (no entities); `chr generate docs-site -d generated-docs` printed Documentation generated at generated-docs index.html navigation.html hello_world.html H1 My Rell Dapp default chromia.yml has no docs: section generate docs-site still wrote set_name WRITE SKIP query hello_world only titles already on disk ${ChrGenerateClientHelp.GENERATE_INDEX_TITLE} ${ChromiaDocsYmlHelp.DOCS_SITE_INDEX_TITLE} ${ChrLibraryHelp.LIBRARY_INDEX_TITLE} ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE}.
        Local 0.33.2 library view generate graph --mdx --class-diagram walk (query-only): `chr library view com.chromia.ft4` printed ID com.chromia.ft4 Name ft4 Organization Chromia Organization Version 1.2.0 Official Yes Description FT4 Library is a library for Rell modules default mainnet library-chain no --brid do not invent a RID do not invent a library-chain semver pin from this view (view Version 1.2.0 is not a pin versions already printed 2.0.2 1.1.0 1.0.0 1.1.1 1.2.0 Total 5 git pin remains ${DappScaffold.FT4_VERSION} already on disk) HELP ONLY WRITE SKIP register login transfer auth mint burn create-accounts no Rell import of ft4 no moduleArgs no signed ops no keys no install; `chr generate graph --help` on 0.33.2 lists --mdx Surround with mdx tags and --entity-relation / --class-diagram Presented as entity relation diagram or class diagram `chr generate graph --mdx -d generated-graph-mdx` printed Created files in generated-graph-mdx [rell.mdx] file empty 0-byte for hello_world (no entities) `chr generate graph --class-diagram -d generated-graph-class` printed Created files in generated-graph-class [rell.mmd] file empty 0-byte for hello_world (no entities) titles already on disk ${ChrGenerateClientHelp.GENERATE_INDEX_TITLE} ${ChrLibraryHelp.LIBRARY_INDEX_TITLE} ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE} ${ChromiaLanguageClientsHelp.JS_TS_INDEX_TITLE} ${ChromiaLanguageClientsHelp.JS_QUICKSTART_INDEX_TITLE} ${ChromiaLanguageClientsHelp.JS_REFERENCE_INDEX_TITLE} ${ChromiaLanguageClientsHelp.KOTLIN_INDEX_TITLE} ${ChromiaLanguageClientsHelp.PYTHON_INDEX_TITLE} ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} ${ChromiaRellDatabaseHelp.BUILD_GETTING_STARTED_INDEX_TITLE} ${ChromiaRellDatabaseHelp.BUILD_OVERVIEW_INDEX_TITLE} fire 0066 INDEX already on disk no new pages.
        Local 0.33.2 library list walk (query-only): `chr library list` needs no RID default mainnet library-chain --url and --brid are optional overrides do not invent a RID printed Available Libraries columns ID Name Organization Version Official Description Total: 20 libraries every row Chromia Organization every row Official Yes row com.chromia.ft4 ft4 Version 1.2.0 Official Yes row com.chromia.iccf 1.90.2 row com.chromia.iccf_test 1.90.0 row com.chromia.icmf 1.102.2 row com.chromia.ICMF 1.99.0 Deprecated - use icmf instead row com.chromia.vector_db 2.2.1 row com.chromia.hybridcompute 3.35.5 row com.chromia.begin_block 1.100.1 row com.chromia.eif 1.3.1 row com.chromia.zkp 1.0.0 row com.chromia.webauthn 1.0.1 row com.chromia.ai_inference 0.4.0. List Version column is the registry headline version not the max semver list ft4 1.2.0 equals view Version 1.2.0 yet versions first item is 2.0.2 Total 5 so do not invent a FT4 semver pin from list vs view vs versions vs git pin git pin remains ${DappScaffold.FT4_VERSION} already on disk list com.chromia.iccf 1.90.2 is likewise not a pin ICCF protocol page documents library-chain ${ChrLibraryHelp.ICCF_LIBRARY_CHAIN_VERSION} already on disk. Official list flags -l/--limit -o/--offset --sort-by=(asc|desc) verified `chr library list --limit 5 --sort-by=asc` printed Total: 5 libraries com.chromia.ICMF com.chromia.icmf com.chromia.iccf com.chromia.begin_block com.chromia.hybridcompute and `chr library list --limit 3 --sort-by=desc` printed Total: 3 libraries com.chromia.hybridcompute_query com.chromia.hbridge_admin com.chromia.hbridge_crc2 desc head matches the default unsorted order head. Narrow terminal clips the Version Official Description columns widen the terminal to read full rows. Titles already on disk ${ChrLibraryHelp.LIBRARY_INDEX_TITLE} ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE} ${ChromiaFt4QueriesHelp.FT4_IMPORTS_INDEX_TITLE} ${ChromiaFt4QueriesHelp.FT4_SETUP_PAGE_INDEX_TITLE}. FT4 HELP ONLY WRITE SKIP register login transfer auth mint burn create-accounts no install no Rell import of ft4 no moduleArgs no signed ops no keys no invented RID no invented 64-hex.
        Local 0.33.2 code check lint format walk (query-only): `chr code check` empty stdout exit 0 `chr code check --hide-lib-warnings` exit 0 `chr code lint src/main.rell` exit 0 `chr code lint src/test` exit 0 `chr code lint main/*` exit 0 `chr code lint` (no files) walks materialized src/lib/ft4 exit 1 unknown_name:iccf import:not_found:lib.iccf expr:smartnull hello_world main test remain clean do not install iccf to fix `chr code format --file=src/main.rell` printed Formatting src/main.rell... no changes `chr code format src/test` checksums unchanged default create writes .rell_format max_line_width=120 insert_spaces=true tab_size=4 .rell_lint rule_naming_convention rule_quote_format=double HELP ONLY WRITE SKIP --fix register login transfer auth no keys no signed ops no install no invented RID git pin remains ${DappScaffold.FT4_VERSION} already on disk titles already on disk ${ChrBuildHelp.CODE_INDEX_TITLE} ${ChrReplHelp.REPL_INDEX_TITLE}.
        Local 0.33.2 repl walk (query-only): `chr repl -c '1+1'` printed 2 exit 0 `chr repl --module main -c '1+1'` printed 2 exit 0 `chr repl --blockchain my_rell_dapp -c '1+1'` printed 2 exit 0 `chr repl --module main -c 'val x = 1; x + 2'` printed 3 exit 0 `chr repl --module main -c '"Hello %s!".format("World")'` printed "Hello World!" exit 0 `chr repl --module main -c 'hello_world()'` Run-time error: No database connection exit 1 `chr repl --module main --use-db -c 'hello_world()'` printed "Hello World!" exit 0 SqlInit Initializing database (chain_iid = 0) `chr repl --module main --use-db --sql-log -c 'hello_world()'` SqlConnectionLogger SELECT "Hello World!" exit 0 `chr repl --module main -d -c '1+1'` printed 2 Script took ...s to run exit 0 `chr repl --module main -c 'set_name("X")'` Type rell.test.op cannot be converted to Gtv HELP ONLY WRITE SKIP ops txs (needs rell.test.tx(...).run() + --use-db) no keys no signed ops no install no invented RID git pin remains ${DappScaffold.FT4_VERSION} already on disk titles already on disk ${ChrReplHelp.REPL_INDEX_TITLE} ${ChrToolsHelp.TOOLS_INDEX_TITLE} ${ChrQueryHelp.QUERY_INDEX_TITLE} `chr repl -c '1+1' -f JSON` printed 2 exit 0 `chr repl --module main -c '"Hello %s!".format("World")' -f JSON` printed "Hello World!" exit 0 `chr repl --module main --use-db -c 'hello_world()' -f JSON` printed "Hello World!" exit 0 `chr repl -c '1+1' -f XML` printed <int>2</int> exit 0 `chr repl --module main -c '"Hello %s!".format("World")' -f XML` printed <string>Hello World!</string> exit 0 `chr repl -c '1+1' -f YAML` Error: Unsupported output format YAML exit 1 help lists YAML `printf '1+1\\n' | chr repl -` printed 2 exit 0 `printf 'args\\n' | chr repl - alpha one` printed ["alpha", "one"] exit 0 `chr repl -c '2+2' -` Error: Cannot use -c when specifying script file exit 1 `chr repl --module main --sql-log -c 'hello_world()'` No database connection exit 1 set_name Switch to a different output format exit 0. Chr tools walk done (${ChrToolsHelp.TOOLS_INDEX_TITLE}). Next chr query after node (${ChrQueryHelp.QUERY_INDEX_TITLE} / ${ChrNodeHelp.NODE_INDEX_TITLE}).
        Local 0.33.2 tools walk (query-only): `chr tools --help` printed gtv validate-config lib-model exit 0 `chr tools gtv --hex` sample hex ${ChrToolsHelp.HEX_EXAMPLE} (not 64-hex) default pretty printed a=FOO b=BAR shape exit 0 -f JSON / XML / raw / YAML all exit 0 (YAML works for tools gtv unlike chr repl) `chr gtv` alias same pretty exit 0 no --hex waits stdin empty pipe Invalid GTV data: Unexpected end of input stream exit 1 --hex ZZ Error: invalid value for --hex: Char Z is not a hex digit exit 1 `chr tools validate-config` missing --file Error: missing option --file exit 1 -f src/main.rell Unsupported file format. Expected either .yml or .yaml exit 1 from parent dir -f my-rell-dapp/chromia.yml printed No issues found in chromia.yml exit 0 from inside dapp -f chromia.yml bare filename getParent(...) must not be null exit 3 `chr tools lib-model` missing --library-source exit 1 --library-source without valid git --registry Registry must be a valid git URL exit 1 with --name --registry --tag-or-branch --insecure prints git-shape libs: block and computes rid from library source — do not invent a RID never paste computed 64-hex into leftover_dapp_build_help HELP ONLY WRITE SKIP install no keys no signed txs titles already on disk ${ChrToolsHelp.TOOLS_INDEX_TITLE} ${ChrQueryHelp.QUERY_INDEX_TITLE} ${ChrNodeHelp.NODE_INDEX_TITLE}. Next chr query after node.
        Local 0.33.2 tools deep walk (query-only): `chr tools` with no subcommand prints the same help as `chr tools --help` Usage: chr tools [OPTIONS] COMMAND [ARGS]... Miscellaneous tools only option -h, --help exit 0 gtv -f pretty is the default and lowercase -f yaml is accepted exit 0 so the YAML gap is repl-only not the shared GTV formatter the piped example `chr gtv --output-format yaml < data.gtv` on a 28-byte binary GTV printed --- a: FOO b: BAR exit 0 and `chr tools gtv < data.gtv` printed pretty --hex wins over piped stdin --hash=1 and --hash=${DappScaffold.MERKLE_HASH_VERSION} print a 64-char uppercase hex Merkle hash computed from the GTV exit 0 never record or invent that hex --hash=0 Error: invalid value for --hash: Merkle hash version must be greater than 0 exit 1 validate-config -f ./chromia.yml and an absolute path both printed No issues found in chromia.yml exit 0 so ./ or an absolute path is the workaround for the bare-filename getParent(...) must not be null exit 3 (stack in /tmp/chromia/chromia-cli.log LoadKt.parseModel) nonexistent path Error: invalid value for --file: file ... does not exist exit 1 a directory Error: invalid value for -f: file ... is a directory exit 1 .yaml is accepted exactly like .yml the validator rejects the cookbook keys Additional property 'timeout' found but was invalid (location: test->timeout) Additional property 'parallel' found but was invalid (location: test->parallel) Additional property 'schema_version' found but was invalid (location: database->schema_version) exit 2 which confirms the not-official note already on disk an unknown top-level section Additional property 'not_a_section' found but was invalid (location: not_a_section) exit 2 the in-memory scaffold_dapp chromia.yml (blockchains module main config.features.merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION} compile.rellVersion ${DappScaffold.RELL_VERSION} libs.ft4 registry ${DappScaffold.FT4_REGISTRY} path ${DappScaffold.FT4_PATH} tagOrBranch ${DappScaffold.FT4_VERSION} rid insecure false) validated clean No issues found exit 0 so scaffold_dapp output passes the real 0.33.2 schema lib-model prints libs: <name>: registry path equal to the -s value verbatim tagOrBranch <Tag or branch the library is published on> placeholder when --tag-or-branch is omitted rid computed insecure false --insecure=true flips insecure: true repeat runs are byte-identical lib-model over the materialized src/lib/ft4 with --tag-or-branch ${DappScaffold.FT4_VERSION} reproduced exactly the FT4 rid already on disk so that pin is computed not invented do not invent a RID HELP ONLY WRITE SKIP lib-model only prints to stdout pasting the block into chromia.yml is a human decision register login transfer auth mint burn create-accounts no keys no signed ops titles already on disk ${ChrToolsHelp.TOOLS_INDEX_TITLE} ${ChrQueryHelp.QUERY_INDEX_TITLE} ${ChrNodeHelp.NODE_INDEX_TITLE}.
        Local 0.33.2 query after node walk (query-only): `chr node start` from project dir printed Node is initialized Building Blockchain: my_rell_dapp Chain-id: 0 REST ${ChrNodeHelp.DEFAULT_API_URL} Postgres Driver 17.11 do not invent or paste the computed BRID `chr query` (no <queryname>) Error: missing argument <queryname> exit 1 `chr query hello_world` from project dir printed "Hello World!" exit 0 (needs no --blockchain-rid) -f pretty / JSON both "Hello World!" exit 0 -f XML <string>Hello World!</string> exit 0 -f raw Hello World! (no quotes) exit 0 -f YAML and -f yaml --- Hello World! exit 0 (YAML works for chr query unlike chr repl) `chr query leftover_no_such_query` query: 400 Bad Request  Unknown query: leftover_no_such_query from http://localhost:7740 exit 1 `chr query --cid 0 hello_world` "Hello World!" exit 0 `chr query --api-url http://localhost:7740 hello_world` "Hello World!" exit 0 `chr query -s chromia.yml hello_world` "Hello World!" exit 0 `chr query hello_world foo=1` query: 400 Bad Request  Query 'hello_world' failed: Invalid argument(s): foo exit 1 `chr query hello_world --` "Hello World!" exit 0 `chr query set_name` Unknown query: set_name exit 1 (set_name is an operation HELP ONLY WRITE SKIP chr tx signed send) `chr query --blockchain my_rell_dapp hello_world` Error: missing option --network exit 1 `chr query --cid 99 hello_world` Could not auto-detect brid from http://localhost:7740, reason: 404 Not Found exit 1 `chr query --blockchain-rid ZZ hello_world` Char Z is not a hex digit exit 3 `chr query --blockchain-rid AB hello_world` Wrong size of Blockchain RID, was 1 should be 32 (64 characters) exit 3 `--brid` alone is not an option (Possible options: -brid, --cid) exit 1 `chr query --mainnet hello_world` Unknown query: hello_world from a mainnet node exit 1 (hello_world is local scaffold only) `chr query --testnet hello_world` hung / timed out against public testnet (timeout) from parent dir `chr query hello_world` still "Hello World!" exit 0 when local node is up (auto REST ${ChrNodeHelp.DEFAULT_API_URL}) `chr query -s my-rell-dapp/chromia.yml hello_world` from parent same exit 0 missing -s file does not exist exit 1 `--network testnet` without deployments block Specified target [testnet] does not exist exit 1 `--api-url http://127.0.0.1:1` Connection refused exit 1 -f FOO invalid choice (choose from pretty, raw, JSON, XML, YAML) exit 1 missing --config file does not exist exit 1 empty queryname Unknown query: exit 1 REST GET ${ChrNodeHelp.DEFAULT_API_URL}/ 200 text/html H1 Postchain REST API /apidocs 200 titles already on disk ${ChrQueryHelp.QUERY_INDEX_TITLE} ${ChrNodeHelp.NODE_INDEX_TITLE} ${ChrNodeHelp.INITIALIZED_LOG} HELP ONLY WRITE SKIP register login transfer auth chr tx signed send no keys no invented BRID no pasted 64-hex. Next deploy help only (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE}).
        Local 0.33.2 query deep walk (query-only): `chr query --help` prints an Examples box primitive_args dict_arg map_arg struct_arg option groups Configuration Properties dApp target options Deployment Options exit 0 `chr query hello_world 1` Index: 1, Size: 1 Error: invalid value for <args>: query must be done with named parameters in a dict exit 1 `chr query --foo hello_world` Error: no such option --foo. Did you mean -f? exit 1 `chr query --cid hello_world` root usage missing argument QUERYNAME invalid value for --cid: hello_world is not a valid integer exit 1 dict arg on hello_world Invalid argument(s): arg exit 1 `--output-format JSON` long form exit 0 `chr query get_version` / ft4.get_version / get_api_version Unknown query exit 1 so the materialized src/lib/ft4 is never imported in main.rell FT4 stays HELP ONLY WRITE SKIP REST /metadata/iid_0 lists the built-in GTX queries last_block_info tx_confirmation_time and ops nop __nop timeb (StandardOpsGTXModule) while hello_world and set_name are not listed `chr query last_block_info` printed a dict blockRID height timestamp exit 0 in pretty JSON XML raw YAML `chr query last_block_info foo=1` still exit 0 so the built-in GTX query ignores extra args unlike Rell hello_world `chr query tx_confirmation_time` without the required txRID query: 500 Internal Server Error  Unknown error exit 1 REST GET ${ChrNodeHelp.DEFAULT_API_URL}/query/iid_0?type=hello_world 200 Hello World! so the iid_<chainIid> alias replaces {blockchainRid} and a local query needs no BRID POST /query/iid_0 type hello_world 200 no type 400 Missing query type type set_name 400 QUERY_NOT_FOUND ops WRITE SKIP iid_99 404 chain Iid: 99 /query_gtv/iid_0 200 octet-stream /dquery Type error: array expected and /web_query Invalid argument(s): path, query_params /blockchain/iid_0/height /nodestate RUNNING_VALIDATOR /config/iid_0/features merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION} /errors /transactions empty /version 22 /infrastructure_version postchain 3.49.16 rest-api 22 database-server-version 17.11 /_debug moved to port 7750 /query /status /height /brid /node/iid_0 404 OPTIONS /query/iid_0 200 GET, POST, OPTIONS /apidocs rapi-doc matching ${ChromiaLanguageClientsHelp.POSTCHAIN_REST_API_INDEX_TITLE} already on disk `chr node --help` lists only start and update so there is no chr node stop titles already on disk ${ChrQueryHelp.QUERY_INDEX_TITLE} ${ChrNodeHelp.NODE_INDEX_TITLE} ${ChrToolsHelp.TOOLS_INDEX_TITLE} HELP ONLY WRITE SKIP register login transfer auth chr tx signed send no keys no invented BRID no pasted 64-hex. Next deploy help only (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE}).
        Local 0.33.2 deploy HELP ONLY walk (query-only; NOTHING was deployed): `chr deploy --help` is not a 0.33.2 command bare `chr deploy` printed Error: no such subcommand deploy. Did you mean deployment? and `chr deploy --help` printed the root help exit 0 the command is `chr deployment` `chr deployment --help` (and bare `chr deployment`) printed Create and maintain deployments create info inspect update resume pause remove proposal voterset container exit 0 `chr deployment create --help` flags -cfg/--config -s/--settings --secret --key-id -d/--network -bc/--blockchain --no-compression --hide-lib-warnings -y -h/--help `chr deployment update --help` adds --height --verify-only --skip-verification `chr deployment info --help` read-only -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain --verbose -f/--output-format=(table|JSON) `chr deployment inspect --help` read-only adds -m/--modules -l/--list-modules --module-args --definitions=(queries|operations|entities|objects) --signature create and update were NEVER run HELP ONLY WRITE SKIP no deploy no signing no keys no invented BlockchainRID no invented container id no testnet no mainnet read-only local checks only: `chr deployment inspect` from the project dir printed queries hello_world text operations set_name name: text entities [] objects my_name mutable name exit 0 --list-modules ["main"] --definitions=queries / --definitions=operations / -m main narrow the same JSON --signature=hello_world printed the single signature --module-args [] -f table printed Query/Operation/Object tables `chr deployment info` against the local node printed Cluster not found for blockchain rid <shortened BRID> and Unknown query: cm_get_blockchain_cluster from ${ChrNodeHelp.DEFAULT_API_URL} (local node is not Directory-managed) never paste the computed BRID `--network testnet` on info and inspect failed locally Specified target [testnet] does not exist exit 1 so nothing left the box `--blockchain-rid ZZ` Char Z is not a hex digit exit 3 `--blockchain-rid AB` Wrong size of Blockchain RID, was 1 should be 32 (64 characters) exit 3 chromia.yml deployments shape already on disk deployments.<net>.url brid container chains.<name> reserved names mainnet/testnet auto-fill Directory brid + url since CLI 0.29.8 create write-back of chains since CLI 0.30.0 update requires chains titles already on disk ${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} ${ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE} ${ChrDeployHelp.TESTNET_DEPLOY_DAPP_INDEX_TITLE} ${ChrDeployHelp.MAINNET_DEPLOY_DAPP_INDEX_TITLE} ${ChrDeployHelp.TESTNET_GETTING_STARTED_INDEX_TITLE} ${ChrDeployHelp.MAINNET_GETTING_STARTED_INDEX_TITLE} ${ChrDeployHelp.GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE} ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} plus sibling help tools chr_deploy_help ${ChromiaYmlDefinitionsHelp.TOOL_NAME} ${ChromiaDocsYmlHelp.TOOL_NAME}. Deploy stays HELP ONLY here settings missing -s does not exist exit 1 --blockchain missing --network exit 1 proposal list / voterset list missing --network exit 1 voterset info must provide one of --name, --container container --help configuration pause resume WRITE SKIP vault-lease HELP ONLY walk done (${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE}) chr tx HELP ONLY walk done (${ChrQueryHelp.TX_INDEX_TITLE}) multi-signature HELP ONLY walk done (${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE}) seeder HELP ONLY walk done (${ChrSeederHelp.SEEDER_INDEX_TITLE} ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE}) eif HELP ONLY walk done (${ChrEifHelp.EIF_INDEX_TITLE}) next version HELP ONLY. Query-only. HELP ONLY WRITE SKIP.

        Official next-step architecture INDEX map (titles already on disk, no new pages): test ${ChrBuildHelp.BUILD_INDEX_TITLE} ${ChrBuildHelp.CODE_INDEX_TITLE} → generate client-stubs graph docs-site ${ChrGenerateClientHelp.GENERATE_INDEX_TITLE} ${ChromiaDocsYmlHelp.DOCS_SITE_INDEX_TITLE} ${ChrGenerateClientHelp.TESTNET_CONNECT_INDEX_TITLE} generate graph --mdx rell.mdx 0-byte generate graph --class-diagram rell.mmd 0-byte → library list library versions library view ${ChrLibraryHelp.LIBRARY_INDEX_TITLE} ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE} query printed 2.0.2 1.1.0 1.0.0 1.1.1 1.2.0 Total 5 view printed ID com.chromia.ft4 Name ft4 Organization Chromia Organization Version 1.2.0 Official Yes no invented RID no invented semver pin git pin remains ${DappScaffold.FT4_VERSION} already on disk → FT4 ${ChromiaFt4QueriesHelp.FT4_SETUP_PAGE_INDEX_TITLE} ${ChromiaFt4QueriesHelp.FT4_IMPORTS_INDEX_TITLE} ${ChromiaFt4QueriesHelp.LEARN_TAGS_FT4_INDEX_TITLE} ${LEARN_FT4_ASSET_SETUP_INDEX_TITLE} HELP ONLY WRITE SKIP → clients ${ChromiaLanguageClientsHelp.JS_TS_INDEX_TITLE} ${ChromiaLanguageClientsHelp.KOTLIN_INDEX_TITLE} ${ChromiaLanguageClientsHelp.PYTHON_INDEX_TITLE} chromia.yml ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} database ${ChromiaRellDatabaseHelp.BUILD_GETTING_STARTED_INDEX_TITLE} ${ChromiaRellDatabaseHelp.BUILD_OVERVIEW_INDEX_TITLE} Rell ${RELL_MODULES_INDEX_TITLE} fire 0066 INDEX already on disk → ${ChrNodeHelp.NODE_INDEX_TITLE} → deploy HELP ONLY walk done (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} ${ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE} ${ChrDeployHelp.TESTNET_DEPLOY_DAPP_INDEX_TITLE} ${ChrDeployHelp.MAINNET_DEPLOY_DAPP_INDEX_TITLE}) chr deployment help only nothing deployed no signing no keys no invented RID vault-lease HELP ONLY walk done (${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE}) chr tx HELP ONLY walk done (${ChrQueryHelp.TX_INDEX_TITLE}) multi-signature HELP ONLY walk done (${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE}) seeder HELP ONLY walk done (${ChrSeederHelp.SEEDER_INDEX_TITLE} ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE}) eif HELP ONLY walk done (${ChrEifHelp.EIF_INDEX_TITLE}) next version HELP ONLY. Query-only. HELP ONLY WRITE SKIP.
        Local 0.33.2 vault lease HELP ONLY walk (query-only): titles already on disk ${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE} ${VaultLeaseHelp.MAINNET_GET_CONTAINER_INDEX_TITLE} ${VaultLeaseHelp.GET_STARTED_HOSTING_INDEX_TITLE} ${VaultLeaseHelp.GET_STARTED_SUPPORTED_WALLETS_INDEX_TITLE} ${VaultLeaseHelp.GET_STARTED_PROVIDER_STAKING_INDEX_TITLE} sibling help tool vault_lease_help workflow get tokens (testnet faucet / mainnet deposit) → open Vault containers → lease in Vault using an existing pubkey file → put the real Container ID in chromia.yml deployments.<net>.container (placeholder ${VaultLeaseHelp.CONTAINER_PLACEHOLDER}) → then chr deployment create HELP ONLY WRITE SKIP never invent a container id never invent or paste Directory Chain 64-hex this tool does not generate a key no signed txs no create from this walk. Query-only. HELP ONLY WRITE SKIP.
        Local 0.33.2 tx HELP ONLY walk (query-only): `chr tx --help` printed Usage: chr tx [<options>] <opname> [<args>]... Make a transaction towards a node FT4 --ft-auth --ft-account-id --evm-auth --ft-register-account ICCF --iccf-tx --iccf-source Examples primitive_args dict_arg map_arg struct_arg exit 0 `chr tx` (no <opname>) Error: missing argument <opname> exit 1 `chr tx set_name Alice` with no node on :7740 Could not auto-detect brid Connection Refused exit 1 `--blockchain-rid ZZ` Char Z is not a hex digit exit 3 `--blockchain-rid AB` Wrong size of Blockchain RID exit 3 `--api-url http://127.0.0.1:1 --cid 0` Connection Refused exit 1 `--network testnet` Specified target [testnet] does not exist exit 1 `--blockchain my_rell_dapp` missing option --network exit 1 missing -s file does not exist exit 1 `--cid hello` not a valid integer exit 1 `--foo` no such option exit 1 titles already on disk ${ChrQueryHelp.TX_INDEX_TITLE} HELP ONLY WRITE SKIP chr tx signed send set_name register login transfer auth no keys no invented BRID no pasted 64-hex.
        Local 0.33.2 multi-signature HELP ONLY walk (query-only): `chr multi-signature --help` printed Handle transactions with need of multiple signers create sign send view exit 0 `chr multi-signature view --help` View a existing transaction -f/--file exit 0 view (no flags) missing option --file exit 1 view -f missing file does not exist exit 1 create sign send WRITE SKIP create (no flags) missing OPNAME + must provide one of --signer, --signers, --signers-file exit 1 titles already on disk ${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE} HELP ONLY WRITE SKIP no keys no signed txs no invented BRID no pasted 64-hex. Seeder HELP ONLY walk done (${ChrSeederHelp.SEEDER_INDEX_TITLE}).
        Local 0.33.2 seeder HELP ONLY walk (query-only): `chr seeder --help` (and bare `chr seeder`) printed Usage: chr seeder [OPTIONS] COMMAND [ARGS]... Generate fake data for a local database init generate exit 0 `chr seed` printed Error: no such subcommand seed. Did you mean seeder? exit 1 `chr seeder seed` printed Error: no such subcommand seed exit 1 `chr seeder init --help` printed Create initial seeder configuration for blockchains -s/--settings -bc/--blockchain (defaults to all) exit 0 `chr seeder generate --help` printed Generate Rell blockchain seeder module --alternative-config-folder -s -bc exit 0 init and generate were NEVER run HELP ONLY WRITE SKIP `chr seeder init -s /tmp/no-such-chromia.yml` file does not exist exit 1 from /tmp Project settings file not found exit 1 `chr seeder --foo` no such option exit 1 titles already on disk ${ChrSeederHelp.SEEDER_INDEX_TITLE} ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE} ${ChrSeederHelp.SEEDER_GENERATOR_INDEX_TITLE} ${ChrSeederHelp.SEEDER_EXAMPLE_INDEX_TITLE} ${ChrSeederHelp.SEEDER_CONFIGURABLE_INDEX_TITLE} sibling help tools ${ChrSeederHelp.TOOL_NAME} ${ChromiaYmlDefinitionsHelp.TOOL_NAME} ${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} ${ChrQueryHelp.QUERY_INDEX_TITLE} do not invent seeder.yml keys no keys no signed txs no posted transaction no invented BlockchainRID no invented container id no pasted 64-hex this tool does not generate a key. Eif HELP ONLY walk done (${ChrEifHelp.EIF_INDEX_TITLE}) next version HELP ONLY. Query-only. HELP ONLY WRITE SKIP.
        Local 0.33.2 eif HELP ONLY walk (query-only): `chr eif --help` (and bare `chr eif`) printed Usage: chr eif [OPTIONS] COMMAND [ARGS]... Ethereum Integration Framework commands generate-events-config Generate solidity events that EIF will listen to exit 0 `chr eif generate-events-config --help` printed --abi --events --target (defaults to build/eif-events.yaml = ${ChrEifHelp.DEFAULT_TARGET}) --format=(XML|YAML) exit 0 generate-events-config was NEVER run HELP ONLY WRITE SKIP bare `chr eif generate-events-config` missing option --abi missing option --events exit 1 `--events foo` missing option --abi exit 1 `--abi` (no value) option --abi requires a value missing option --events exit 1 `--foo` no such option --foo. Did you mean --format? exit 1 `chr eif seed` no such subcommand seed exit 1 `chr eif generate` no such subcommand generate. Did you mean generate-events-config? exit 1 `chr eif foo` no such subcommand foo exit 1 missing ABI file path does not exist exit 1 `--format badformat` invalid choice (choose from XML, YAML) exit 1 `--abi /tmp` (directory) MalformedJsonException exit 3 `--target` with missing ABI never wrote ${ChrEifHelp.DEFAULT_TARGET} titles already on disk ${ChrEifHelp.EIF_INDEX_TITLE} ${ChrEifHelp.ECOSYSTEM_GOV_EIF_INDEX_TITLE} sibling help tool ${ChrEifHelp.TOOL_NAME} library list row com.chromia.eif Version 1.3.1 already on disk do not invent ABI event YAML keys no keys no signed txs no posted transaction no invented BlockchainRID no pasted 64-hex this tool does not generate a key. Seeder HELP ONLY walk done (${ChrSeederHelp.SEEDER_INDEX_TITLE}) next version HELP ONLY. Query-only. HELP ONLY WRITE SKIP.

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
                put("architecture", "INDEX path: LEARN_HOME + LEARN install CLI → chr create-rell-dapp → chromia.yml project settings + project structure → Rell language/expressions/statements/types → FT4 WRITE SKIP (hello_world has no FT4 auth register login transfer) → LEARN_TAGS_VECTOR_DB (official tag URL currently 404; live Vector DB INDEX is learn vector-db course) + vector-db course INDEX → chr node start (Postgres 16+ postchain defaults worked org.postgresql.Driver 17.11 REST ${ChrNodeHelp.DEFAULT_API_URL} Node is initialized chain-id 0 query hello_world Hello World!) → deploy. Local 0.33.2 walk: silent create default folder my-rell-dapp default chromia.yml compile.rellVersion 0.14.5 blockchains.my_rell_dapp.module main database.schema schema_my_rell_dapp test.modules test no host/user/password no merkle_hash_version chr build artifact build/my_rell_dapp.xml query from project dir needs no --blockchain-rid REST GET / 200. Query-only HELP ONLY WRITE SKIP.")
                put("official_loop", "chr create-rell-dapp → cd my-rell-dapp → chr node start → chr query hello_world")
                put("query_only", "HELP ONLY WRITE SKIP")
                put("leftover_local_create", "silent; default folder my-rell-dapp; default hello_world")
                put("leftover_default_rellVersion", "0.14.5")
                put("leftover_default_schema", "schema_my_rell_dapp")
                put("leftover_chr_build", "optional compile; chr node start compiles Type=BLOCKCHAIN; artifact build/my_rell_dapp.xml")
                put("leftover_query_from_project_dir", "chr query hello_world needs no --blockchain-rid")
                put("leftover_ft4", "HELP ONLY WRITE SKIP")
                put("leftover_postgres", "16+ postchain database/user/password defaults actually worked; default yml only sets schema; org.postgresql.Driver 17.11 JDBC ${ChrNodeHelp.DEFAULT_JDBC}")
                put("leftover_create_silent", "silent; 0-byte stdout/stderr; default folder my-rell-dapp")
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
                    "chr generate graph -d generated-graph → Created files in generated-graph: [rell.mmd]; hello_world rell.mmd empty 0-byte (no entities)"
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
                    "default chromia.yml has no docs: section; generate docs-site still wrote generated-docs"
                )
                put(
                    "leftover_generate_docs_site_write_skip",
                    "HELP ONLY WRITE SKIP set_name page; query hello_world only no keys no signed ops"
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
                    "HELP ONLY WRITE SKIP; query library-chain versions only; no Rell import of ft4 no moduleArgs no signed ops no keys do not invent a library-chain semver pin from this list git pin remains ${DappScaffold.FT4_VERSION}"
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
                    "HELP ONLY WRITE SKIP; query library-chain view only; view Version 1.2.0 is not a pin versions printed 2.0.2 1.1.0 1.0.0 1.1.1 1.2.0 Total 5 git pin remains ${DappScaffold.FT4_VERSION} already on disk no Rell import of ft4 no moduleArgs no install no signed ops no keys do not invent a library-chain semver pin from this view"
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
                    "HELP ONLY WRITE SKIP; query library-chain list only; no install no Rell import of ft4 no moduleArgs no signed ops no keys register login transfer auth mint burn create-accounts do not invent a RID do not invent a library-chain semver pin from this list git pin remains ${DappScaffold.FT4_VERSION}"
                )
                put(
                    "leftover_generate_graph_flags",
                    "0.33.2 chr generate graph --help: --mdx Surround with mdx tags; --entity-relation / --class-diagram Presented as entity relation diagram or class diagram"
                )
                put(
                    "leftover_generate_graph_mdx",
                    "chr generate graph --mdx -d generated-graph-mdx → Created files in generated-graph-mdx: [rell.mdx]; hello_world rell.mdx empty 0-byte (no entities)"
                )
                put("leftover_generate_graph_mdx_file", "generated-graph-mdx/rell.mdx")
                put(
                    "leftover_generate_graph_class_diagram",
                    "chr generate graph --class-diagram -d generated-graph-class → Created files in generated-graph-class: [rell.mmd]; hello_world rell.mmd empty 0-byte (no entities)"
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
                put("leftover_fire_0066_index", "clients JS/TS Kotlin Python project-config database getting-started/overview INDEX titles already on disk; no new pages")
                put("leftover_fire_0065_verified", "chr test + --modules filter + generate client-stubs -m main + generate graph; FT4 yml WRITE SKIP accounts")
                put("leftover_code_check", "chr code check")
                put("leftover_code_check_hide_lib_warnings", "chr code check --hide-lib-warnings")
                put(
                    "leftover_code_check_printed",
                    "empty stdout; exit 0 (hello_world does not import ft4 so materialized src/lib/ft4 does not fail check)"
                )
                put("leftover_code_lint", "chr code lint")
                put(
                    "leftover_code_lint_hello_world",
                    "chr code lint src/main.rell → exit 0; chr code lint src/test → exit 0; chr code lint main/* → exit 0"
                )
                put(
                    "leftover_code_lint_project",
                    "chr code lint (no files) walks materialized src/lib/ft4 exit 1 unknown_name:iccf import:not_found:lib.iccf expr:smartnull hello_world main test remain clean do not install iccf to fix HELP ONLY WRITE SKIP --fix register login transfer auth no keys no signed ops no invented RID git pin remains ${DappScaffold.FT4_VERSION}"
                )
                put(
                    "leftover_code_lint_no_fix",
                    "do not pass --fix; HELP ONLY WRITE SKIP lint auto-fix no install no keys no signed ops do not invent a RID"
                )
                put("leftover_code_format", "chr code format")
                put(
                    "leftover_code_format_printed",
                    "chr code format --file=src/main.rell → Formatting src/main.rell... no changes; src/test checksums unchanged after chr code format src/test"
                )
                put(
                    "leftover_code_rell_format",
                    "default create writes .rell_format [*.rell] max_line_width=120 insert_spaces=true tab_size=4"
                )
                put(
                    "leftover_code_rell_lint",
                    "default create writes .rell_lint [*.rell] rule_naming_convention rule_import_from_non_module rule_quote_format=double rule_formatter rule_constant_detection rule_unused_variable rule_outer_join_cartesian_product"
                )
                put(
                    "leftover_code_flags",
                    "check: --hide-lib-warnings; lint: --source-dir -fo/--formatter-options=.rell_format -lo/--linter-options=.rell_lint --fix (WRITE SKIP) <files>; format: --source-dir --file -fo/.rell_format <files>"
                )
                put(
                    "leftover_code_write_skip",
                    "HELP ONLY WRITE SKIP --fix format rewrite register login transfer auth no keys no signed ops no install iccf do not invent a RID git pin remains ${DappScaffold.FT4_VERSION}"
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
                    "HELP ONLY WRITE SKIP ops txs; chr repl --module main -c 'set_name(\"X\")' printed Type rell.test.op cannot be converted to Gtv Switch to a different output format exit 0 (needs rell.test.tx(...).run() + --use-db); no keys no signed ops no install do not invent a RID git pin remains ${DappScaffold.FT4_VERSION}"
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
                    "chr repl --help lists YAML; chr repl -c '1+1' -f YAML printed Error: Unsupported output format YAML exit 1 -f yaml same error (help mismatch 0.33.2)"
                )
                put(
                    "leftover_repl_output_format_raw",
                    "chr repl -c '1+1' -f raw → 2 exit 0; -r/--raw-output is deprecated use -f"
                )
                put(
                    "leftover_repl_script_stdin",
                    "printf '1+1\\n' | chr repl - → 2 exit 0 (experimental script stdin dash)"
                )
                put(
                    "leftover_repl_script_args",
                    "printf 'args\\n' | chr repl - alpha one → [\"alpha\", \"one\"] exit 0 (args: list<text>)"
                )
                put(
                    "leftover_repl_command_not_with_script",
                    "chr repl -c '2+2' - printed Error: Cannot use -c when specifying script file exit 1"
                )
                put(
                    "leftover_repl_sql_log_needs_use_db",
                    "chr repl --module main --sql-log -c 'hello_world()' printed Run-time error: No database connection exit 1 (--sql-log alone does not load entities needs --use-db + --module)"
                )
                put("leftover_tools_help", ChrToolsHelp.TOOL_NAME)
                put("leftover_tools_index_title", ChrToolsHelp.TOOLS_INDEX_TITLE)
                put("leftover_query_index_title", ChrQueryHelp.QUERY_INDEX_TITLE)
                put("leftover_tools", "chr tools")
                put(
                    "leftover_tools_commands",
                    "chr tools --help printed gtv validate-config lib-model exit 0"
                )
                put("leftover_tools_gtv", "chr tools gtv")
                put(
                    "leftover_tools_gtv_alias",
                    "chr gtv is alias of chr tools gtv (same --help / decode)"
                )
                put(
                    "leftover_tools_gtv_hex",
                    "chr tools gtv --hex ${ChrToolsHelp.HEX_EXAMPLE} (official sample hex not 64-hex)"
                )
                put(
                    "leftover_tools_gtv_pretty",
                    "chr tools gtv --hex <sample> default pretty printed [\"a\": \"FOO\", \"b\": \"BAR\"] exit 0"
                )
                put(
                    "leftover_tools_gtv_json",
                    "chr tools gtv --hex <sample> -f JSON printed {\"a\": \"FOO\", \"b\": \"BAR\"} exit 0"
                )
                put(
                    "leftover_tools_gtv_xml",
                    "chr tools gtv --hex <sample> -f XML printed <dict><entry key=\"a\"><string>FOO</string></entry>... exit 0"
                )
                put(
                    "leftover_tools_gtv_raw",
                    "chr tools gtv --hex <sample> -f raw printed a=FOO b=BAR exit 0"
                )
                put(
                    "leftover_tools_gtv_yaml",
                    "chr tools gtv --hex <sample> -f YAML printed --- a: FOO b: BAR exit 0 (YAML works for tools gtv unlike chr repl)"
                )
                put(
                    "leftover_tools_gtv_alias_decode",
                    "chr gtv --hex <sample> same pretty [\"a\": \"FOO\", \"b\": \"BAR\"] exit 0"
                )
                put(
                    "leftover_tools_gtv_missing_hex",
                    "chr tools gtv (no --hex no pipe) waits on stdin (timeout); empty pipe printed Invalid GTV data: Unexpected end of input stream exit 1"
                )
                put(
                    "leftover_tools_gtv_invalid_hex",
                    "chr tools gtv --hex ZZ printed Error: invalid value for --hex: Char Z is not a hex digit exit 1"
                )
                put("leftover_tools_validate_config", "chr tools validate-config")
                put(
                    "leftover_tools_validate_config_printed",
                    "from parent dir chr tools validate-config -f my-rell-dapp/chromia.yml printed No issues found in chromia.yml exit 0; --file= also accepted"
                )
                put(
                    "leftover_tools_validate_config_file_required",
                    "chr tools validate-config (no --file) printed Error: missing option --file exit 1; -f src/main.rell printed Unsupported file format. Expected either .yml or .yaml exit 1; from inside dapp -f chromia.yml (bare filename) printed getParent(...) must not be null exit 3 (use a path with a parent dir component)"
                )
                put("leftover_tools_lib_model", "chr tools lib-model")
                put(
                    "leftover_tools_lib_model_source_required",
                    "chr tools lib-model (no args) printed Error: missing option --library-source exit 1; --name alone same missing --library-source exit 1; --library-source without valid --registry printed Error: invalid value for --registry: Registry must be a valid git URL exit 1"
                )
                put(
                    "leftover_tools_lib_model_no_rid",
                    "chr tools lib-model --library-source=<dir> --name=<lib> --registry=<git URL> --tag-or-branch=<tag> --insecure=true|false prints git-shape libs: block and computes rid from library source — do not invent a RID (never paste computed 64-hex into leftover_dapp_build_help) HELP ONLY WRITE SKIP install"
                )
                put(
                    "leftover_tools_flags",
                    "gtv: --hex -f/--output-format=(pretty|raw|JSON|XML|YAML) --hash; validate-config: -f/--file; lib-model: --name -s/--library-source --registry --tag-or-branch --insecure=true|false"
                )
                put(
                    "leftover_tools_write_skip",
                    "HELP ONLY WRITE SKIP register login transfer auth install no keys no signed txs no invented RID no invented 64-hex"
                )
                put(
                    "leftover_tools_no_subcommand",
                    "chr tools (no subcommand) prints the same help as chr tools --help: Usage: chr tools [OPTIONS] COMMAND [ARGS]...; Miscellaneous tools; only option -h, --help; exit 0"
                )
                put(
                    "leftover_tools_gtv_yaml_lowercase",
                    "chr tools gtv --hex <sample> -f yaml (lowercase) also accepted -> --- a: FOO b: BAR exit 0; the YAML gap is chr repl only, not the shared GTV formatter"
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
                    "chr tools validate-config -f ./chromia.yml and an absolute path both print No issues found in chromia.yml exit 0; ./ or an absolute path is the workaround for the bare-filename getParent(...) must not be null exit 3 (stack in /tmp/chromia/chromia-cli.log LoadKt.parseModel)"
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
                    "the in-memory scaffold_dapp chromia.yml (blockchains module main + config.features.merkle_hash_version 2, compile.rellVersion ${DappScaffold.RELL_VERSION}, libs.ft4 registry/path/tagOrBranch/rid/insecure false) validated clean: No issues found exit 0 - scaffold_dapp output passes the real 0.33.2 schema"
                )
                put(
                    "leftover_tools_lib_model_printed_shape",
                    "libs: <name>: registry: <git URL>; path: the -s value verbatim; tagOrBranch: <Tag or branch the library is published on> placeholder when --tag-or-branch is omitted; rid: computed; insecure: false; repeat runs are byte-identical"
                )
                put(
                    "leftover_tools_lib_model_rid_matches_disk",
                    "chr tools lib-model -s src/lib/ft4 --name ft4 --registry <official FT4 git URL> --tag-or-branch v1.1.0r reproduced exactly the FT4 rid already on disk (chromia.yml libs.ft4.rid / DappScaffold FT4 rid) - the pin is computed from the library source, never invented; do not invent a RID and do not paste the computed hex here"
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
                    "chr query (no <queryname>) printed Error: missing argument <queryname> exit 1"
                )
                put(
                    "leftover_query_hello_world",
                    "chr query hello_world (from project dir after chr node start) printed \"Hello World!\" exit 0 needs no --blockchain-rid"
                )
                put(
                    "leftover_query_output_format",
                    "-f, --output-format=(pretty|raw|JSON|XML|YAML)"
                )
                put(
                    "leftover_query_output_format_pretty_json",
                    "chr query hello_world -f pretty and -f JSON both printed \"Hello World!\" exit 0"
                )
                put(
                    "leftover_query_output_format_xml",
                    "chr query hello_world -f XML printed <string>Hello World!</string> exit 0"
                )
                put(
                    "leftover_query_output_format_raw",
                    "chr query hello_world -f raw printed Hello World! (no quotes) exit 0"
                )
                put(
                    "leftover_query_output_format_yaml",
                    "chr query hello_world -f YAML and -f yaml printed --- Hello World! exit 0 (YAML works for chr query unlike chr repl)"
                )
                put(
                    "leftover_query_unknown",
                    "chr query leftover_no_such_query printed query: 400 Bad Request  Unknown query: leftover_no_such_query from http://localhost:7740 exit 1"
                )
                put(
                    "leftover_query_cid",
                    "chr query --cid 0 hello_world printed \"Hello World!\" exit 0"
                )
                put(
                    "leftover_query_api_url",
                    "chr query --api-url http://localhost:7740 hello_world printed \"Hello World!\" exit 0 (official default REST ${ChrNodeHelp.DEFAULT_API_URL})"
                )
                put(
                    "leftover_query_settings",
                    "chr query -s chromia.yml hello_world printed \"Hello World!\" exit 0"
                )
                put(
                    "leftover_query_invalid_arg",
                    "chr query hello_world foo=1 printed query: 400 Bad Request  Query 'hello_world' failed: Invalid argument(s): foo exit 1"
                )
                put(
                    "leftover_query_dashdash",
                    "chr query hello_world -- printed \"Hello World!\" exit 0"
                )
                put(
                    "leftover_query_op_as_query",
                    "chr query set_name printed Unknown query: set_name exit 1 (set_name is an operation HELP ONLY WRITE SKIP chr tx signed send)"
                )
                put(
                    "leftover_query_blockchain_needs_network",
                    "chr query --blockchain my_rell_dapp hello_world printed Error: missing option --network exit 1"
                )
                put(
                    "leftover_query_cid_missing",
                    "chr query --cid 99 hello_world printed Could not auto-detect brid from http://localhost:7740, reason: 404 Not Found exit 1"
                )
                put(
                    "leftover_query_brid_invalid_hex",
                    "chr query --blockchain-rid ZZ hello_world printed Char Z is not a hex digit exit 3"
                )
                put(
                    "leftover_query_brid_wrong_size",
                    "chr query --blockchain-rid AB hello_world printed Wrong size of Blockchain RID, was 1 should be 32 (64 characters) exit 3 do not invent or paste a BRID"
                )
                put(
                    "leftover_query_brid_alias",
                    "--brid alone is not an option (Possible options: -brid, --cid) exit 1 use -brid or --blockchain-rid"
                )
                put(
                    "leftover_query_mainnet_local_query",
                    "chr query --mainnet hello_world printed Unknown query: hello_world from a mainnet node exit 1 (hello_world is local scaffold only)"
                )
                put(
                    "leftover_query_testnet_timeout",
                    "chr query --testnet hello_world hung / timed out against public testnet (timeout) do not invent a BRID"
                )
                put(
                    "leftover_query_from_parent",
                    "from parent dir chr query hello_world still printed \"Hello World!\" exit 0 when local node is up (auto REST ${ChrNodeHelp.DEFAULT_API_URL})"
                )
                put(
                    "leftover_query_parent_settings",
                    "from parent dir chr query -s my-rell-dapp/chromia.yml hello_world printed \"Hello World!\" exit 0"
                )
                put(
                    "leftover_query_settings_missing",
                    "chr query -s /tmp/no-such-chromia.yml hello_world printed Error: invalid value for -s: file ... does not exist exit 1"
                )
                put(
                    "leftover_query_network_missing",
                    "chr query --network testnet hello_world (no deployments block) printed Error: invalid value for --network: Specified target [testnet] does not exist exit 1"
                )
                put(
                    "leftover_query_api_refused",
                    "chr query --api-url http://127.0.0.1:1 hello_world printed Connection refused exit 1"
                )
                put(
                    "leftover_query_bad_format",
                    "chr query hello_world -f FOO printed Error: invalid value for -f: invalid choice: FOO. (choose from pretty, raw, JSON, XML, YAML) exit 1"
                )
                put(
                    "leftover_query_config_missing",
                    "chr query --config /tmp/no-client.conf hello_world printed Error: invalid value for --config: file ... does not exist exit 1"
                )
                put(
                    "leftover_query_empty_name",
                    "chr query '' printed Unknown query: exit 1"
                )
                put(
                    "leftover_query_rest_root",
                    "REST GET ${ChrNodeHelp.DEFAULT_API_URL}/ 200 text/html H1 Postchain REST API /apidocs 200"
                )
                put(
                    "leftover_query_node_initialized",
                    "chr node start printed ${ChrNodeHelp.INITIALIZED_LOG} Building Blockchain: my_rell_dapp Chain-id: 0 do not invent or paste the computed BRID"
                )
                put(
                    "leftover_query_write_skip",
                    "HELP ONLY WRITE SKIP register login transfer auth chr tx signed send no keys no invented BRID no pasted 64-hex"
                )
                put(
                    "leftover_query_flags",
                    "-s/--settings -cfg/--config -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain -f/--output-format=(pretty|raw|JSON|XML|YAML)"
                )
                put("leftover_deploy", "chr deployment")
                put(
                    "leftover_query_help_examples",
                    "chr query --help on 0.33.2 prints an Examples box primitive_args arg1=123 arg2=Alice dict_arg map_arg struct_arg chr query my_query -- arg1=foo (short byte_array samples not a BRID do not invent one)"
                )
                put(
                    "leftover_query_help_groups",
                    "chr query --help option groups Configuration Properties -cfg/--config -s/--settings dApp target options -brid/--blockchain-rid --cid --api-url --mainnet/--testnet Deployment -d/--network -bc/--blockchain Options -f/--output-format -h/--help exit 0 chr query -h is the same help Make a query towards a running node"
                )
                put(
                    "leftover_query_help_arg_types",
                    "official <args> types integer: 123 big_integer: 1234L string bytearray array dict query must be done with named parameters"
                )
                put(
                    "leftover_query_positional_arg",
                    "chr query hello_world 1 printed Index: 1, Size: 1 then Error: invalid value for <args>: query must be done with named parameters in a dict exit 1"
                )
                put(
                    "leftover_query_unknown_option",
                    "chr query --foo hello_world printed Error: no such option --foo. Did you mean -f? exit 1"
                )
                put(
                    "leftover_query_cid_not_integer",
                    "chr query --cid hello_world ate the queryname as the --cid value and printed the root Usage: chr [OPTIONS] COMMAND [ARGS]... Error: missing argument QUERYNAME Error: invalid value for --cid: hello_world is not a valid integer exit 1"
                )
                put(
                    "leftover_query_dict_arg_unknown",
                    "chr query hello_world with a dict arg printed Query 'hello_world' failed: Invalid argument(s): arg exit 1 hello_world() takes no parameters so the dict_arg example needs its own Rell query"
                )
                put(
                    "leftover_query_output_format_long",
                    "chr query hello_world --output-format JSON and --output-format pretty long form both printed \"Hello World!\" exit 0 (same as -f)"
                )
                put(
                    "leftover_query_ft4_absent",
                    "chr query get_version / ft4.get_version / get_api_version all printed Unknown query exit 1 src/lib/ft4 is materialized on disk but main.rell never imports ft4 so no FT4 query is mounted on the chain FT4 stays HELP ONLY WRITE SKIP register login transfer auth"
                )
                put(
                    "leftover_query_gtx_metadata",
                    "REST GET ${ChrNodeHelp.DEFAULT_API_URL}/metadata/iid_0 printed the chain's built-in GTX surface queries last_block_info tx_confirmation_time operations nop __nop timeb all gtxModule net.postchain.gtx.StandardOpsGTXModule the Rell hello_world and set_name are NOT listed there"
                )
                put(
                    "leftover_query_last_block_info",
                    "chr query last_block_info (built-in StandardOpsGTXModule query no args) printed a dict blockRID height timestamp exit 0 a second query-only query that exists next to hello_world never record or invent that 64-char hex"
                )
                put(
                    "leftover_query_last_block_info_formats",
                    "chr query last_block_info -f pretty rell dict -f JSON -f XML -f raw -f YAML all exit 0 hex elided here on purpose"
                )
                put(
                    "leftover_query_last_block_info_extra_arg",
                    "chr query last_block_info foo=1 still printed the dict exit 0 the built-in GTX query ignores extra named args while the Rell hello_world rejects them Invalid argument(s): foo exit 1 so strict arg checking is a Rell property not a CLI property"
                )
                put(
                    "leftover_query_tx_confirmation_time_requires_arg",
                    "chr query tx_confirmation_time (metadata marks txRID BYTEARRAY or STRING required) printed query: 500 Internal Server Error  Unknown error exit 1 REST the same 500 a missing required GTX arg is a 500 not the 400 a Rell query gives no tx was sent HELP ONLY WRITE SKIP"
                )
                put(
                    "leftover_query_rest_iid_alias",
                    "REST GET ${ChrNodeHelp.DEFAULT_API_URL}/query/iid_0?type=hello_world printed 200 application/json \"Hello World!\" the iid_<chainIid> alias works wherever the local OpenAPI spells {blockchainRid} so a local REST query needs no BRID at all keep writing <BlockchainRID> and do not invent or paste one"
                )
                put(
                    "leftover_query_rest_missing_type",
                    "REST GET /query/iid_0 with no type and POST /query/iid_0 empty body both printed 400 Missing query type"
                )
                put(
                    "leftover_query_rest_post",
                    "REST POST /query/iid_0 Content-Type: application/json type hello_world printed 200 \"Hello World!\" a POST here is still query-only no tx no signature no keys"
                )
                put(
                    "leftover_query_rest_unknown_query",
                    "REST GET /query/iid_0?type=set_name printed 400 Unknown query: set_name code QUERY_NOT_FOUND set_name is an operation HELP ONLY WRITE SKIP ops txs"
                )
                put(
                    "leftover_query_rest_unknown_iid",
                    "REST GET /query/iid_99?type=hello_world and /brid/iid_99 both printed 404 Can't find blockchain with chain Iid: 99 in DB. Did you add this BC to the node? which is the REST twin of the CLI --cid 99 auto-detect failure"
                )
                put(
                    "leftover_query_rest_query_gtv",
                    "REST GET /query_gtv/iid_0?type=hello_world printed 200 application/octet-stream a 16-byte GTV body decode it with chr tools gtv (${ChrToolsHelp.TOOLS_INDEX_TITLE}) never with an invented hex"
                )
                put(
                    "leftover_query_rest_dquery_web_query",
                    "REST GET /dquery/iid_0?type=hello_world printed 400 Type error: array expected, found STRING and /web_query/iid_0/hello_world printed 400 Query 'hello_world' failed: Invalid argument(s): path, query_params both need a purpose-built Rell query shape and /dquery is deprecated on the local spec"
                )
                put(
                    "leftover_query_rest_height_state",
                    "REST GET /blockchain/iid_0/height printed blockHeight /blockchain/iid_0/nodestate printed RUNNING_VALIDATOR /config/iid_0/features printed merkle_hash_version ${DappScaffold.MERKLE_HASH_VERSION} /errors/iid_0 and /transactions/iid_0 printed empty arrays 200 on the untouched scaffold"
                )
                put(
                    "leftover_query_rest_config_xml",
                    "REST GET /config/iid_0 printed 200 text/xml a dict with add_primary_key_to_header blockstrategy mininterblockinterval 1000 net.postchain.base.BaseBlockBuildingStrategy config_consensus_strategy HEADER_HASH configurationfactory"
                )
                put(
                    "leftover_query_rest_version",
                    "REST GET /version and /version/iid_0 printed version 22 /infrastructure_version printed postchain 3.49.16 infrastructure net.postchain.ebft.BaseEBFTInfrastructureFactory rest-api 22 database-server-version 17.11 which matches chr --version postchain 3.49.16"
                )
                put(
                    "leftover_query_rest_debug_moved",
                    "REST GET /_debug printed 200 text/html H1 _debug endpoint moved to a separate port 7750 by default configurable with debug.port in node configuration or POSTCHAIN_DEBUG_PORT"
                )
                put(
                    "leftover_query_rest_404s",
                    "REST GET /query /status /height /brid /node/iid_0 all printed 404 with an empty body every query path needs the chain segment"
                )
                put(
                    "leftover_query_rest_cors",
                    "REST OPTIONS /query/iid_0 printed 200 access-control-allow-methods GET, POST, OPTIONS access-control-allow-headers Content-Type, Accept, X-Accept-Query-Response-Signature access-control-expose-headers X-Data-Truncated, X-Transaction-Timestamp, X-Block-Height, X-Query-Response-Signature"
                )
                put(
                    "leftover_query_rest_apidocs",
                    "REST GET /apidocs printed 200 a rapi-doc page spec-url /apidocs/postchain-restapi.yaml server-url ${ChrNodeHelp.DEFAULT_API_URL} the spec itself 200 text/yaml about 81 KB"
                )
                put(
                    "leftover_query_rest_openapi_query_group",
                    "the local OpenAPI query tag lists /query /query_gtv GET and POST /query_async /web_query and the deprecated /dquery documented errors Missing query type Unknown query code QUERY_NOT_FOUND optional request header X-Accept-Query-Response-Signature same groups as ${ChromiaLanguageClientsHelp.POSTCHAIN_REST_API_INDEX_TITLE} already on disk"
                )
                put(
                    "leftover_query_no_node_stop",
                    "chr node --help on 0.33.2 lists only start and update so there is no chr node stop end the chr node start process to stop the local test node and not leak it chr node start --help flags -s/--settings -bc/--blockchain-config --name -p -np/--node-properties --directory-chain-mock --hide-lib-warnings --sql-log --wipe/--no-wipe"
                )
                put(
                    "leftover_query_brid_never_pasted",
                    "REST GET /brid/iid_0 returns a 64-char BRID and /blocks/iid_0 embeds block RIDs neither is recorded in leftover_dapp_build_help always write <BlockchainRID> and never invent or paste the hex"
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
                    "chr deploy --help on 0.33.2 is NOT a command: bare chr deploy printed Error: no such subcommand deploy. Did you mean deployment? and chr deploy --help just printed the root help exit 0 command is chr deployment"
                )
                put(
                    "leftover_deploy_subcommands",
                    "chr deployment with no subcommand prints the same help as chr deployment --help exit 0 Create and maintain deployments create info inspect update resume pause remove proposal voterset container only option -h, --help"
                )
                put(
                    "leftover_deploy_create_flags",
                    "chr deployment create HELP ONLY flags -cfg/--config -s/--settings --secret --key-id -d/--network -bc/--blockchain --no-compression --hide-lib-warnings -y -h/--help NOT RUN"
                )
                put(
                    "leftover_deploy_create_write_skip",
                    "HELP ONLY WRITE SKIP chr deployment create was never run no deploy no signing no keys no invented BlockchainRID no invented container id -y confirms a new deployment non-interactive error already on disk ${ChrDeployHelp.CREATE_Y_ERROR}"
                )
                put(
                    "leftover_deploy_update_flags",
                    "chr deployment update HELP ONLY flags -cfg/--config -s/--settings --secret --key-id -d/--network -bc/--blockchain --no-compression --hide-lib-warnings --height --verify-only --skip-verification -h/--help NOT RUN"
                )
                put(
                    "leftover_deploy_update_write_skip",
                    "HELP ONLY WRITE SKIP chr deployment update was never run --verify-only and --skip-verification are flags not an excuse to send an update transaction no signing no keys"
                )
                put(
                    "leftover_deploy_info_flags",
                    "chr deployment info read-only flags -cfg/--config -s/--settings -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain --verbose -f/--output-format=(table|JSON) no key pair flags"
                )
                put(
                    "leftover_deploy_info_local",
                    "chr deployment info against the local scaffold node printed Cluster not found for blockchain rid <shortened BRID> query: 400 Bad Request  Unknown query: cm_get_blockchain_cluster from ${ChrNodeHelp.DEFAULT_API_URL} because the local node is not Directory-managed never paste the computed BRID"
                )
                put(
                    "leftover_deploy_inspect_flags",
                    "chr deployment inspect read-only flags -cfg/--config -s/--settings -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain -f/--output-format=(table|JSON) -m/--modules -l/--list-modules --module-args --definitions=(queries|operations|entities|objects) --signature no key pair flags"
                )
                put(
                    "leftover_deploy_inspect_local",
                    "chr deployment inspect from the project dir against the local node exit 0 printed JSON queries mount_name hello_world return_type text operations mount_name set_name parameters name text entities [] objects mount_name my_name mutable name text"
                )
                put(
                    "leftover_deploy_inspect_list_modules",
                    "chr deployment inspect --list-modules printed [ \"main\" ] exit 0"
                )
                put(
                    "leftover_deploy_inspect_definitions",
                    "chr deployment inspect --definitions=queries printed only the queries block hello_world exit 0 --definitions=operations printed only set_name exit 0 -m/--modules main same queries block exit 0"
                )
                put(
                    "leftover_deploy_inspect_signature",
                    "chr deployment inspect --signature=hello_world printed mount_name hello_world return_type text parameters {} and exited 0"
                )
                put(
                    "leftover_deploy_inspect_module_args",
                    "chr deployment inspect --module-args printed [] exit 0 (default scaffold has no moduleArgs)"
                )
                put(
                    "leftover_deploy_inspect_table",
                    "chr deployment inspect -f table printed Query Return type Parameters hello_world text Operation set_name name: text Object my_name mutable name: text exit 0 -f JSON matches the default JSON"
                )
                put(
                    "leftover_deploy_network_missing",
                    "chr deployment info --network testnet and chr deployment inspect --network testnet from the default scaffold (no deployments block) printed Error: invalid value for --network: Specified target [testnet] does not exist exit 1 so nothing left the box"
                )
                put(
                    "leftover_deploy_brid_invalid_hex",
                    "chr deployment info --blockchain-rid ZZ printed Char Z is not a hex digit exit 3"
                )
                put(
                    "leftover_deploy_brid_wrong_size",
                    "chr deployment info --blockchain-rid AB printed Wrong size of Blockchain RID, was 1 should be 32 (64 characters) exit 3 do not invent a BlockchainRID"
                )
                put(
                    "leftover_deploy_key_flags",
                    "key pair source --secret=<path> and --key-id=<key_id> exist only on create and update HELP ONLY reference an existing key id this fire generated no key and used none"
                )
                put(
                    "leftover_deploy_yml_deployments",
                    "chromia.yml deployments section shape already on disk deployments.<net>.url (string or list) brid container chains.<name> reserved names mainnet / testnet auto-fill Directory brid + url since CLI 0.29.8 container is the Vault/PMC lease id do not invent one (${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} ${ChromiaYmlDefinitionsHelp.TOOL_NAME} ${ChromiaDocsYmlHelp.TOOL_NAME})"
                )
                put(
                    "leftover_deploy_yml_write_back",
                    "since CLI 0.30.0 chr deployment create writes deployments.<net>.chains.<name> back into chromia.yml and chr deployment update requires chains and does not rewrite the file already on disk in chr_deploy_help HELP ONLY here"
                )
                put(
                    "leftover_deploy_sign_skip",
                    "HELP ONLY WRITE SKIP proposal vote retract-vote revoke rename voterset update add-dapp-provider container configuration pause resume deployment pause resume remove all skipped proposal list/info and voterset list/info stay read-only"
                )
                put(
                    "leftover_deploy_write_skip",
                    "HELP ONLY WRITE SKIP deploy is HELP ONLY here no deploy no signing no keys no invented BlockchainRID no invented container id no testnet no mainnet only chr deployment --help style inspection plus deploy INDEX titles already on disk"
                )
                put(
                    "leftover_deploy_nothing_deployed",
                    "nothing was deployed this fire only --help output local inspect against the scaffold node and local config errors"
                )

                put(
                    "leftover_deploy_commands",
                    "create info inspect update resume pause remove proposal voterset container"
                )
                put(
                    "leftover_deploy_missing_subcommand",
                    "chr deployment (no subcommand) printed Usage: chr deployment [OPTIONS] COMMAND [ARGS]... Create and maintain deployments create info inspect update resume pause remove proposal voterset container exit 0 chr deploy Error: no such subcommand deploy. Did you mean deployment? exit 1 chr deploy --help prints root chr help (not deployment help) exit 0"
                )
                put(
                    "leftover_deploy_create_help",
                    "chr deployment create --help printed Deploy new blockchain instance -d/--network -bc/--blockchain -s/--settings --secret --key-id -y Confirm that this will create a new deployment --no-compression --hide-lib-warnings exit 0 HELP ONLY WRITE SKIP create signed send chr deployment create (no flags) Error: missing option --network exit 1 chr deployment create --network testnet Error: invalid value for --network: Specified target [testnet] does not exist exit 1 never signed"
                )
                put(
                    "leftover_deploy_update_help",
                    "chr deployment update --help printed Update configuration of a deployed blockchain --height --verify-only Verifies blockchain config without sending update transaction --skip-verification Skip verification of blockchain config before sending update transaction -d/--network -bc/--blockchain exit 0 HELP ONLY WRITE SKIP update signed send chr deployment update (no flags) Error: missing option --network exit 1 chr deployment update --network testnet Error: invalid value for --network: Specified target [testnet] does not exist exit 1 never signed"
                )
                put(
                    "leftover_deploy_inspect_help",
                    "chr deployment inspect --help printed Inspect the API of a deployed blockchain -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain -f/--output-format=(table|JSON) -m/--modules -l/--list-modules --module-args --definitions=(queries|operations|entities|objects) --signature exit 0 chr deployment inspect (no flags while node is up) printed JSON queries mount_name hello_world operations set_name objects my_name exit 0 same -s chromia.yml --cid 0 --api-url http://localhost:7740 --cid 0 exit 0 needs no --blockchain-rid default output JSON do not invent or paste a BRID"
                )
                put(
                    "leftover_deploy_info_help",
                    "chr deployment info --help printed Information about a deployed blockchain --verbose Show verbose information about nodes -f/--output-format=(table|JSON) -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain exit 0 chr deployment info (no flags while node is up) printed Cluster not found for blockchain rid 5C:7EF query: 400 Bad Request  Unknown query: cm_get_blockchain_cluster from http://localhost:7740 exit 0 same with -s chromia.yml --cid 0 local node is not Directory-managed do not invent or paste a BRID never paste computed 64-hex"
                )
                put(
                    "leftover_deploy_no_deployments_block",
                    "scratch chromia.yml has no deployments block chr deployment inspect --network testnet printed Error: invalid value for --network: Specified target [testnet] does not exist exit 1 same for info / create / update --network testnet exit 1 never signed nothing left the box"
                )
                put(
                    "leftover_deploy_settings_missing",
                    "chr deployment inspect -s /tmp/no-such-chromia.yml printed Error: invalid value for -s: file \"/tmp/no-such-chromia.yml\" does not exist. exit 1 same for info exit 1"
                )
                put(
                    "leftover_deploy_flags",
                    "-s/--settings -cfg/--config -d/--network -bc/--blockchain -brid/--blockchain-rid --cid --api-url --mainnet/--testnet --secret --key-id -y --no-compression --hide-lib-warnings --height --verify-only --skip-verification -f/--output-format=(table|JSON) -m/--modules -l/--list-modules --module-args --definitions --signature --verbose --from --to --all --pending --id -n/--name -c/--container"
                )
                put(
                    "leftover_deploy_proposal_list_help",
                    "chr deployment proposal list --help printed List all proposals that you can vote on --from --to --all --pending -d/--network -f/--output-format=(table|JSON) exit 0 chr deployment proposal list (no flags) Error: missing option --network exit 1 HELP ONLY WRITE SKIP proposal vote retract-vote revoke rename"
                )
                put(
                    "leftover_deploy_proposal_info_help",
                    "chr deployment proposal info --help printed Get information of a given proposal --id -d/--network -f/--output-format=(table|JSON) exit 0 chr deployment proposal info (no flags) Error: missing option --id Error: missing option --network exit 1 HELP ONLY WRITE SKIP proposal vote retract-vote revoke rename"
                )
                put(
                    "leftover_deploy_voterset_list_help",
                    "chr deployment voterset list --help printed List all voter sets -d/--network -c/--container -f/--output-format exit 0 chr deployment voterset list (no flags) Error: missing option --network exit 1 HELP ONLY WRITE SKIP voterset update add-dapp-provider"
                )
                put(
                    "leftover_deploy_voterset_info_help",
                    "chr deployment voterset info --help printed Show information of voter set -n/--name -c/--container -d/--network -f/--output-format exit 0 chr deployment voterset info (no flags) Error: missing option --network Error: must provide one of --name, --container exit 1 HELP ONLY WRITE SKIP voterset update add-dapp-provider"
                )
                put(
                    "leftover_deploy_container_help",
                    "chr deployment container --help printed Manage container operations configuration Propose configurations pause resume exit 0 HELP ONLY WRITE SKIP container configuration pause resume next vault-lease HELP ONLY (${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE})"
                )
                put(
                    "leftover_deploy_inspect_brid_invalid_hex",
                    "chr deployment inspect --blockchain-rid ZZ printed Char Z is not a hex digit exit 3"
                )
                put(
                    "leftover_deploy_inspect_brid_wrong_size",
                    "chr deployment inspect --blockchain-rid AB printed Wrong size of Blockchain RID, was 1 should be 32 (64 characters) exit 3 do not invent or paste a BRID never paste 64-hex"
                )
                put(
                    "leftover_deploy_api_refused",
                    "chr deployment inspect --api-url http://127.0.0.1:1 --cid 0 printed Could not auto-detect brid from http://127.0.0.1:1, reason: 503 Client Error: Connection Refused exit 1 chr deployment info --api-url http://127.0.0.1:1 --cid 0 printed getBlockchainRID: 503 Client Error: Connection Refused from http://127.0.0.1:1 exit 1 after node died inspect --list-modules / -f table / info --verbose same Connection refused from http://localhost:7740 exit 1"
                )
                put(
                    "leftover_deploy_blockchain_needs_network",
                    "chr deployment inspect --blockchain my_rell_dapp printed Error: missing option --network exit 1 same for info --blockchain my_rell_dapp exit 1 same for proposal list voterset list create update (no flags) exit 1"
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
                    "vault HELP ONLY path already on disk via vault_lease_help 1 get tokens testnet faucet ${VaultLeaseHelp.TESTNET_FAUCET} mainnet deposit ${VaultLeaseHelp.MAINNET_VAULT_DEPOSIT} 2 open Vault containers testnet ${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS} mainnet ${VaultLeaseHelp.MAINNET_VAULT_CONTAINERS} 3 lease a container in Vault paste an existing pubkey from ~/.chromia/<key-id>.pubkey this tool does not generate a key 4 adjust SCUs sign in Vault result is a real Container ID never invent one placeholder ${VaultLeaseHelp.CONTAINER_PLACEHOLDER} 5 put that real Container ID in chromia.yml deployments.<net>.container (${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE}) 6 then chr deployment create HELP ONLY WRITE SKIP (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE}) Directory Chain BRIDs are write_deployment_config values already on disk — never invent or paste 64-hex titles ${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE} ${VaultLeaseHelp.MAINNET_GET_CONTAINER_INDEX_TITLE} ${VaultLeaseHelp.GET_STARTED_HOSTING_INDEX_TITLE}"
                )
                put(
                    "leftover_vault_write_skip",
                    "HELP ONLY WRITE SKIP register login transfer auth no keys no signed txs no invented container id no invented BlockchainRID no pasted 64-hex do not generate a key do not run chr deployment create from this walk"
                )
                put(
                    "leftover_vault_yml_container",
                    "chromia.yml deployments.<net>.container = real Vault Container ID placeholder ${VaultLeaseHelp.CONTAINER_PLACEHOLDER} never invent create write-back of chains since CLI 0.30.0 already on disk"
                )
                put("leftover_tx", "chr tx")
                put("leftover_tx_index_title", ChrQueryHelp.TX_INDEX_TITLE)
                put("leftover_tx_help", "chr_query_help")
                put(
                    "leftover_tx_help_text",
                    "chr tx --help printed Usage: chr tx [<options>] <opname> [<args>]... Make a transaction towards a node Supports both specifying the target node using url and brid/id or from a deployment posts asynchronously unless --await FT4 compatibility --ft-auth --ft-account-id --evm-auth --ft-register-account ICCF --iccf-tx --iccf-source --source-api-url --iccf-force-intra-network --iccf-arg-pos Examples primitive_args dict_arg map_arg struct_arg exit 0"
                )
                put(
                    "leftover_tx_missing_opname",
                    "chr tx (no <opname>) printed Error: missing argument <opname> exit 1"
                )
                put(
                    "leftover_tx_no_node",
                    "chr tx set_name Alice with no node on :7740 printed Could not auto-detect brid from http://localhost:7740, reason: 503 Client Error: Connection Refused exit 1 HELP ONLY WRITE SKIP do not send even when the node is up"
                )
                put(
                    "leftover_tx_brid_invalid_hex",
                    "chr tx --blockchain-rid ZZ set_name Alice printed Char Z is not a hex digit exit 3"
                )
                put(
                    "leftover_tx_brid_wrong_size",
                    "chr tx --blockchain-rid AB set_name Alice printed Wrong size of Blockchain RID, was 1 should be 32 (64 characters) exit 3 do not invent or paste a BRID never paste 64-hex"
                )
                put(
                    "leftover_tx_api_refused",
                    "chr tx --api-url http://127.0.0.1:1 --cid 0 set_name Alice printed Could not auto-detect brid from http://127.0.0.1:1, reason: 503 Client Error: Connection Refused exit 1"
                )
                put(
                    "leftover_tx_network_missing",
                    "chr tx --network testnet set_name Alice printed Error: invalid value for --network: Specified target [testnet] does not exist exit 1 chromia.yml has no deployments block nothing left the box"
                )
                put(
                    "leftover_tx_blockchain_needs_network",
                    "chr tx --blockchain my_rell_dapp set_name Alice printed Error: missing option --network exit 1"
                )
                put(
                    "leftover_tx_settings_missing",
                    "chr tx -s /tmp/no-such-chromia.yml set_name Alice printed Error: invalid value for -s: file \"/tmp/no-such-chromia.yml\" does not exist. exit 1"
                )
                put(
                    "leftover_tx_cid_not_integer",
                    "chr tx --cid hello set_name Alice printed Error: invalid value for --cid: hello is not a valid integer exit 1"
                )
                put(
                    "leftover_tx_unknown_option",
                    "chr tx --foo printed Error: no such option --foo Error: missing argument OPNAME exit 1"
                )
                put(
                    "leftover_tx_flags",
                    "-cfg/--config -s/--settings --secret --key-id -brid/--blockchain-rid --cid --api-url --mainnet/--testnet -d/--network -bc/--blockchain --ft-auth --ft-account-id --evm-auth --ft-register-account --iccf-tx --iccf-source --source-api-url --iccf-force-intra-network --iccf-arg-pos -a/--await/--no-await -nop --timeb-at --timeb-after"
                )
                put(
                    "leftover_tx_write_skip",
                    "HELP ONLY WRITE SKIP chr tx signed send set_name register login transfer auth --ft-auth --ft-register-account --evm-auth --secret --key-id no keys no invented BRID no pasted 64-hex titles already on disk ${ChrQueryHelp.TX_INDEX_TITLE}"
                )
                put("leftover_multi", "chr multi-signature")
                put("leftover_multi_help", ChrMultiSignatureHelp.TOOL_NAME)
                put("leftover_multi_index_title", ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE)
                put(
                    "leftover_multi_help_text",
                    "chr multi-signature --help printed Handle transactions with need of multiple signers Commands create sign send view exit 0 bare chr multi-signature prints the same help exit 0"
                )
                put(
                    "leftover_multi_view_help",
                    "chr multi-signature view --help printed View a existing transaction -f/--file exit 0 chr multi-signature view (no flags) Error: missing option --file exit 1 chr multi-signature view -f /tmp/no-such-tx.gtv Error: invalid value for -f: file \"/tmp/no-such-tx.gtv\" does not exist. exit 1 view is the only query-only multi-signature subcommand used here"
                )
                put(
                    "leftover_multi_write_skip",
                    "HELP ONLY WRITE SKIP create sign send create --help printed but create was NEVER run missing OPNAME + must provide one of --signer, --signers, --signers-file exit 1 no keys no signed txs no invented BRID no pasted 64-hex titles already on disk ${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE}"
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
                    "chr seeder --help (and bare chr seeder) printed Usage: chr seeder [OPTIONS] COMMAND [ARGS]... Generate fake data for a local database Options -h/--help Commands init Create initial seeder configuration for blockchains generate Generate Rell blockchain seeder module exit 0 titles already on disk ${ChrSeederHelp.SEEDER_INDEX_TITLE} ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE} sibling help tool ${ChrSeederHelp.TOOL_NAME} ${ChromiaYmlDefinitionsHelp.TOOL_NAME} ${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} ${ChrQueryHelp.QUERY_INDEX_TITLE}"
                )
                put(
                    "leftover_seeder_commands",
                    "init generate no other 0.33.2 seeder subcommands there is no apply run populate seed as a seeder subcommand"
                )
                put(
                    "leftover_seeder_missing_subcommand",
                    "chr seed (root) printed Error: no such subcommand seed. Did you mean seeder? exit 1 chr seeder seed printed Error: no such subcommand seed (no Did you mean) exit 1 the command is chr seeder unlike chr deploy chr seeder IS a 0.33.2 command"
                )
                put(
                    "leftover_seeder_init_help",
                    "chr seeder init --help printed Usage: chr seeder init [<options>] Create initial seeder configuration for blockchains Configuration Properties -s/--settings Alternate path for project settings file Options -bc/--blockchain Blockchains to generate configuration for (defaults to all) -h/--help exit 0 HELP ONLY WRITE SKIP init was NEVER run would write ${ChrSeederHelp.DEFAULT_CONFIG_FOLDER}"
                )
                put(
                    "leftover_seeder_generate_help",
                    "chr seeder generate --help printed Usage: chr seeder generate [<options>] Generate Rell blockchain seeder module Configuration Properties -s/--settings Options --alternative-config-folder=<path> Alternative path to the root seeder configuration folder -bc/--blockchain Blockchains to generate seeders for (defaults to all) -h/--help exit 0 HELP ONLY WRITE SKIP generate was NEVER run would write compile.source/seeder/seed_<blockchain>.rell"
                )
                put(
                    "leftover_seeder_settings_missing",
                    "chr seeder init -s /tmp/no-such-chromia.yml printed Error: invalid value for -s: file \"/tmp/no-such-chromia.yml\" does not exist. exit 1 same for generate -s /tmp/no-such-chromia.yml exit 1 nothing was written"
                )
                put(
                    "leftover_seeder_no_project",
                    "chr seeder init (no flags from /tmp) printed Project settings file not found exit 1 same for generate init --foo generate --foo (no -s) also Project settings file not found exit 1 settings lookup happens before unknown-option check when no -s nothing was written"
                )
                put(
                    "leftover_seeder_unknown_option",
                    "chr seeder --foo printed Error: no such option --foo exit 1 chr seeder init -s /tmp/no-such-chromia.yml --foo printed Error: no such option --foo Error: invalid value for -s exit 1 chr seeder init -s /tmp/no-such-chromia.yml --blockchain printed Error: option --blockchain requires a value Error: invalid value for -s exit 1 chr seeder generate -s /tmp/no-such-chromia.yml --alternative-config-folder printed Error: option --alternative-config-folder requires a value Error: invalid value for -s exit 1 --blockchain no_such with missing -s still file does not exist exit 1 nothing was written"
                )
                put(
                    "leftover_seeder_flags",
                    "-h/--help init -s/--settings -bc/--blockchain generate -s/--settings --alternative-config-folder=<path> -bc/--blockchain defaults to all blockchains early-stage may change pages do not publish a seeder.yml key schema do not invent keys ${ChromiaYmlDefinitionsHelp.TOOL_NAME}"
                )
                put("leftover_seeder_default_config_folder", ChrSeederHelp.DEFAULT_CONFIG_FOLDER)
                put(
                    "leftover_seeder_write_skip",
                    "HELP ONLY WRITE SKIP chr seeder init chr seeder generate were NEVER run never run would write ${ChrSeederHelp.DEFAULT_CONFIG_FOLDER}/<chain>/seeder.yml and compile.source/seeder/seed_<blockchain>.rell do not invent seeder.yml keys do not invent a BlockchainRID do not invent a container id no keys no signed txs no posted transaction no register login transfer auth titles already on disk ${ChrSeederHelp.SEEDER_INDEX_TITLE} ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE} ${ChrSeederHelp.SEEDER_GENERATOR_INDEX_TITLE} ${ChrSeederHelp.SEEDER_EXAMPLE_INDEX_TITLE} ${ChrSeederHelp.SEEDER_CONFIGURABLE_INDEX_TITLE} ${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} ${ChrQueryHelp.QUERY_INDEX_TITLE} ${ChromiaYmlDefinitionsHelp.TOOL_NAME}"
                )


                put("leftover_eif", "chr eif")
                put("leftover_eif_help", ChrEifHelp.TOOL_NAME)
                put("leftover_eif_index_title", ChrEifHelp.EIF_INDEX_TITLE)
                put("leftover_eif_gov_index_title", ChrEifHelp.ECOSYSTEM_GOV_EIF_INDEX_TITLE)
                put(
                    "leftover_eif_help_text",
                    "chr eif --help (and bare chr eif) printed Usage: chr eif [OPTIONS] COMMAND [ARGS]... Ethereum Integration Framework commands Options -h/--help Commands generate-events-config Generate solidity events that EIF will listen to exit 0 titles already on disk ${ChrEifHelp.EIF_INDEX_TITLE} sibling help tool ${ChrEifHelp.TOOL_NAME} library list row com.chromia.eif Version 1.3.1 already on disk ${ChrEifHelp.ECOSYSTEM_GOV_EIF_INDEX_TITLE}"
                )
                put(
                    "leftover_eif_generate_help",
                    "chr eif generate-events-config --help printed Usage: chr eif generate-events-config [<options>] Generate solidity events that EIF will listen to --abi=<path> Path to a JSON ABI file or a directory of JSON ABI files --events=<text> Names of the relevant events (Comma separated) --target=<path> Target file to generate events in (defaults to \"build/eif-events.yaml\") --format=(XML|YAML) Output file format -h/--help exit 0 HELP ONLY WRITE SKIP generate-events-config was NEVER run would write ${ChrEifHelp.DEFAULT_TARGET}"
                )
                put(
                    "leftover_eif_missing_abi",
                    "chr eif generate-events-config (no flags) printed Error: missing option --abi Error: missing option --events exit 1 chr eif generate-events-config --events foo printed Error: missing option --abi exit 1 nothing was written"
                )
                put(
                    "leftover_eif_missing_events",
                    "chr eif generate-events-config --abi /tmp/no-such-abi.json printed Error: invalid value for --abi: path \"/tmp/no-such-abi.json\" does not exist. Error: missing option --events exit 1 --events (no value) Error: option --events requires a value Error: missing option --abi exit 1 nothing was written"
                )
                put(
                    "leftover_eif_unknown_option",
                    "chr eif generate-events-config --foo printed Error: no such option --foo. Did you mean --format? Error: missing option --abi Error: missing option --events exit 1 nothing was written"
                )
                put(
                    "leftover_eif_missing_subcommand",
                    "chr eif seed printed Error: no such subcommand seed exit 1 chr eif generate printed Error: no such subcommand generate. Did you mean generate-events-config? exit 1 chr eif foo printed Error: no such subcommand foo exit 1 the only 0.33.2 eif subcommand is generate-events-config"
                )
                put(
                    "leftover_eif_abi_missing_file",
                    "chr eif generate-events-config --abi /tmp/no-such-abi.json --events Transfer printed Error: invalid value for --abi: path \"/tmp/no-such-abi.json\" does not exist. exit 1 with --target /tmp/should-not-write-0078.yaml same exit 1 nothing was written ${ChrEifHelp.DEFAULT_TARGET} never created"
                )
                put(
                    "leftover_eif_invalid_format",
                    "chr eif generate-events-config --abi /etc/hosts --events Transfer --format badformat printed Error: invalid value for --format: invalid choice: badformat. (choose from XML, YAML) exit 1 --format BAD with missing ABI also invalid choice + path does not exist exit 1 nothing was written"
                )
                put(
                    "leftover_eif_dir_vs_file",
                    "chr eif generate-events-config --abi /tmp --events Transfer (directory of non-ABI files) printed MalformedJsonException Unterminated array exit 3 HELP ONLY WRITE SKIP do not invent ABI JSON nothing under ${ChrEifHelp.DEFAULT_TARGET} was written"
                )
                put("leftover_eif_default_target", ChrEifHelp.DEFAULT_TARGET)
                put(
                    "leftover_eif_flags",
                    "-h/--help generate-events-config --abi=<path> --events=<text> --target=<path> (default ${ChrEifHelp.DEFAULT_TARGET}) --format=(XML|YAML) (default YAML) do not invent ABI event YAML keys ${ChrEifHelp.TOOL_NAME}"
                )
                put(
                    "leftover_eif_write_skip",
                    "HELP ONLY WRITE SKIP chr eif generate-events-config was NEVER run never run would write ${ChrEifHelp.DEFAULT_TARGET} do not invent ABI event YAML keys do not invent a BlockchainRID no keys no signed txs no posted transaction no register login transfer auth titles already on disk ${ChrEifHelp.EIF_INDEX_TITLE} ${ChrEifHelp.ECOSYSTEM_GOV_EIF_INDEX_TITLE} ${ChrEifHelp.TOOL_NAME} library list com.chromia.eif Version 1.3.1 already on disk"
                )

                put(
                    "leftover_next_step_architecture",
                    "next-step INDEX path: chr create-rell-dapp → chr build (${ChrBuildHelp.BUILD_INDEX_TITLE}) / chr code check (${ChrBuildHelp.CODE_INDEX_TITLE}) → chr test (3 PASSED) → chr generate client-stubs / chr generate graph / chr generate docs-site (${ChrGenerateClientHelp.GENERATE_INDEX_TITLE}; ${ChromiaDocsYmlHelp.DOCS_SITE_INDEX_TITLE}; connect a client ${ChrGenerateClientHelp.TESTNET_CONNECT_INDEX_TITLE}) generate graph --mdx rell.mdx 0-byte generate graph --class-diagram rell.mmd 0-byte → chr library list (${ChrLibraryHelp.LIBRARY_INDEX_TITLE}, ${ChrLibraryHelp.COMMANDS_LIBRARY_INDEX_TITLE}) no RID printed Total: 20 libraries com.chromia.ft4 Version 1.2.0 Official Yes com.chromia.iccf 1.90.2 com.chromia.ICMF Deprecated - use icmf instead → chr library versions com.chromia.ft4 query printed 2.0.2 1.1.0 1.0.0 1.1.1 1.2.0 Total 5 chr library view com.chromia.ft4 printed ID com.chromia.ft4 Name ft4 Organization Chromia Organization Version 1.2.0 Official Yes no invented RID no invented semver pin git pin ${DappScaffold.FT4_VERSION} already on disk → FT4 yml libs import (${ChromiaFt4QueriesHelp.FT4_SETUP_PAGE_INDEX_TITLE}, ${ChromiaFt4QueriesHelp.FT4_IMPORTS_INDEX_TITLE}) HELP ONLY WRITE SKIP clients ${ChromiaLanguageClientsHelp.JS_TS_INDEX_TITLE} ${ChromiaLanguageClientsHelp.KOTLIN_INDEX_TITLE} ${ChromiaLanguageClientsHelp.PYTHON_INDEX_TITLE} project-config ${ChromiaProjectStructureHelp.PROJECT_CONFIG_INDEX_TITLE} database ${ChromiaRellDatabaseHelp.BUILD_GETTING_STARTED_INDEX_TITLE} ${ChromiaRellDatabaseHelp.BUILD_OVERVIEW_INDEX_TITLE} fire 0066 INDEX already on disk → chr code check empty stdout exit 0 chr code lint src/main.rell src/test main/* exit 0 chr code lint (no files) walks materialized src/lib/ft4 exit 1 unknown_name:iccf import:not_found:lib.iccf chr code format --file src/main.rell printed Formatting src/main.rell... no changes default create .rell_format max_line_width=120 HELP ONLY WRITE SKIP --fix (${ChrBuildHelp.CODE_INDEX_TITLE}) → chr repl query only (${ChrReplHelp.REPL_INDEX_TITLE}) chr repl -c '1+1' 2 exit 0 --module main --use-db -c hello_world() Hello World! exit 0 --sql-log SELECT set_name Type rell.test.op HELP ONLY WRITE SKIP ops txs -f JSON 2 -f XML <int>2</int> -f YAML Unsupported output format YAML exit 1 stdin chr repl - 2 args list Cannot use -c when specifying script file --sql-log without --use-db No database connection chr tools walk done (${ChrToolsHelp.TOOLS_INDEX_TITLE}) gtv validate-config lib-model real exits deep walk gtv -f yaml lowercase ok binary stdin data.gtv ok --hash=0 must be greater than 0 validate-config ./ or absolute path ok cookbook keys Additional property exit 2 scaffold_dapp yml No issues found lib-model rid computed matches the pin already on disk chr node start (${ChrNodeHelp.NODE_INDEX_TITLE}) Node is initialized Chain-id: 0 REST ${ChrNodeHelp.DEFAULT_API_URL} chr query after node walk done (${ChrQueryHelp.QUERY_INDEX_TITLE}) chr query hello_world Hello World! exit 0 -f YAML works for query unlike repl --cid 0 ok Unknown query / Invalid argument(s) / missing --network / Char Z is not a hex digit / Wrong size of Blockchain RID exit 3 REST GET / Postchain REST API no pasted BRID deploy HELP ONLY walk done (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} / ${ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE}) chr deploy is not a 0.33.2 subcommand Did you mean deployment? chr deployment create update HELP ONLY WRITE SKIP never run no signing no keys no invented RID inspect info read-only inspect printed hello_world set_name my_name info Cluster not found locally --network testnet Specified target [testnet] does not exist exit 1 chromia.yml deployments url brid container chains write-back since 0.30.0 (${ChrDeployHelp.TESTNET_DEPLOY_DAPP_INDEX_TITLE} ${ChrDeployHelp.MAINNET_DEPLOY_DAPP_INDEX_TITLE} ${ChrDeployHelp.GET_STARTED_DEPLOY_TESTNET_INDEX_TITLE}) chr query deep walk done (${ChrQueryHelp.QUERY_INDEX_TITLE}) --help Examples primitive_args dict_arg map_arg struct_arg named parameters only query must be done with named parameters in a dict no such option --foo. Did you mean -f? --cid hello_world is not a valid integer last_block_info is a second query-only query and ignores extra args tx_confirmation_time without txRID 500 Unknown error get_version / ft4.get_version Unknown query so FT4 is not mounted REST /query/iid_0?type=hello_world 200 Hello World! no BRID needed /query_gtv octet-stream Missing query type / QUERY_NOT_FOUND / chain Iid: 99 /metadata/iid_0 nop __nop timeb ops WRITE SKIP no chr node stop on 0.33.2 never paste a BRID deploy HELP ONLY walk done (${ChrDeployHelp.DEPLOYMENT_INDEX_TITLE} ${ChrDeployHelp.COMMANDS_DEPLOYMENT_INDEX_TITLE}) chr deployment create update HELP ONLY WRITE SKIP never signed inspect info read-only Specified target [testnet] does not exist vault-lease HELP ONLY walk done (${VaultLeaseHelp.TESTNET_GET_CONTAINER_INDEX_TITLE} ${VaultLeaseHelp.MAINNET_GET_CONTAINER_INDEX_TITLE} ${VaultLeaseHelp.GET_STARTED_HOSTING_INDEX_TITLE} ${VaultLeaseHelp.GET_STARTED_SUPPORTED_WALLETS_INDEX_TITLE}) never invent a lease id placeholder ${VaultLeaseHelp.CONTAINER_PLACEHOLDER} chr tx HELP ONLY walk done (${ChrQueryHelp.TX_INDEX_TITLE}) missing <opname> exit 1 Char Z is not a hex digit exit 3 Wrong size of Blockchain RID exit 3 Specified target [testnet] does not exist exit 1 Connection Refused exit 1 HELP ONLY WRITE SKIP do not send chr multi-signature HELP ONLY walk done (${ChrMultiSignatureHelp.MULTI_SIGNATURE_INDEX_TITLE}) view query-only missing --file exit 1 create sign send WRITE SKIP seeder HELP ONLY walk done (${ChrSeederHelp.SEEDER_INDEX_TITLE} ${ChrSeederHelp.COMMANDS_SEEDER_INDEX_TITLE} ${ChrSeederHelp.SEEDER_GENERATOR_INDEX_TITLE} ${ChrSeederHelp.SEEDER_EXAMPLE_INDEX_TITLE} ${ChrSeederHelp.SEEDER_CONFIGURABLE_INDEX_TITLE}) chr seed is not a 0.33.2 subcommand Did you mean seeder? chr seeder IS a command init generate HELP ONLY WRITE SKIP never run Project settings file not found -s does not exist exit 1 do not invent seeder.yml keys eif HELP ONLY walk done (${ChrEifHelp.EIF_INDEX_TITLE}) next version HELP ONLY. Titles already on disk; no new pages. Query-only HELP ONLY WRITE SKIP."
                )
            }
        )
        put("notes", notes())
    }
}
// Official ECOSYSTEM ecosystem/filehub/filehub-setup INDEX leftovers encoded as ECOSYSTEM_FILEHUB_SETUP_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/nodes/node-config INDEX leftovers encoded as ECOSYSTEM_NODE_CONFIG_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/import INDEX leftovers encoded as ECOSYSTEM_GOV_STARTER_KIT_IMPORT_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/book-review/build-client/prerequisites INDEX leftovers encoded as LEARN_BOOK_REVIEW_PREREQUISITES_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/big-data/setup INDEX leftovers encoded as LEARN_BIG_DATA_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-asset/setup INDEX leftovers encoded as LEARN_FT4_ASSET_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/iccf-course/setup INDEX leftovers encoded as LEARN_ICCF_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/project-structure INDEX leftovers encoded as LEARN_NEWS_PROJECT_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/rell-intro INDEX leftovers encoded as RELL_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-one/project-structure/modules INDEX leftovers encoded as LEARN_TTT_RELL_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX vector-db embedding-model leftovers encoded as LEARN_VECTOR_DB_EMBEDDING_MODEL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX monetize-dapp account-registration leftovers encoded as LEARN_MONETIZE_ACCOUNT_REGISTRATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof frontend leftovers encoded as LEARN_ZK_FRONTEND_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX relationships-course joins leftovers encoded as LEARN_RELATIONSHIPS_JOINS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX web3-for-web2-devs summary leftovers encoded as LEARN_WEB3_SUMMARY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/modules/namespace INDEX leftovers encoded as RELL_MODULE_NAMESPACE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/require-function INDEX leftovers encoded as RELL_SYSTEMLIB_REQUIRE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/modules INDEX leftovers encoded as RELL_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official dapp-build INDEX help map architecture INDEX local walk Node is initialized REST query Hello World! chain-id 0 postgres defaults worked Driver 17.11 (query-only HELP ONLY WRITE SKIP; no new pages).
// Local next-step walk (chr test, chr generate client-stubs, FT4 chromia.yml libs import, chr build) encoded as leftover_test* / leftover_generate_client* / leftover_ft4_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no new pages).
// Local fire-0065 verify (chr test --modules filter, chr generate client-stubs -m main, chr generate graph empty rell.mmd) encoded as leftover_test_modules_filter* / leftover_generate_client_module_flag / leftover_generate_graph (query-only HELP ONLY WRITE SKIP; no keys, no signed ops).
// Local library versions generate graph generate docs-site walk (chr library versions com.chromia.ft4 printed 2.0.2 1.1.0 1.0.0 1.1.1 1.2.0 Total 5 no RID, chr generate graph empty rell.mmd, chr generate docs-site generated-docs hello_world query WRITE SKIP set_name) encoded as leftover_library_versions* / leftover_generate_docs_site* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no invented RID, no invented library-chain semver pin, no new pages).
// Local library view generate graph --mdx --class-diagram walk (chr library view com.chromia.ft4 printed ID com.chromia.ft4 Name ft4 Organization Chromia Organization Version 1.2.0 Official Yes no RID no invented library-chain semver pin git pin remains v1.1.0r, chr generate graph --mdx empty rell.mdx, chr generate graph --class-diagram empty rell.mmd, fire 0066 clients JS/TS Kotlin Python project-config database getting-started/overview INDEX titles already on disk) encoded as leftover_library_view* / leftover_generate_graph_mdx* / leftover_generate_graph_class_diagram* / leftover_clients_* / leftover_database_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no install, no invented RID, no invented library-chain semver pin, no new pages).
// Local library list walk (chr library list needs no RID, printed Available Libraries columns ID Name Organization Version Official Description Total: 20 libraries, com.chromia.ft4 Version 1.2.0 Official Yes, com.chromia.iccf 1.90.2, com.chromia.ICMF 1.99.0 Deprecated - use icmf instead, --limit/--sort-by verified) encoded as leftover_library_list* (query-only HELP ONLY WRITE SKIP; list Version is not the max semver and not a pin — do not invent a FT4 semver pin from list vs view vs versions vs git pin; git pin remains v1.1.0r already on disk; no keys, no signed FT4 ops, no install, no invented RID, no new pages).
// Local code check lint format walk (chr code check empty stdout exit 0, chr code lint src/main.rell src/test main/* exit 0, chr code lint project-wide walks materialized src/lib/ft4 exit 1 unknown_name:iccf import:not_found:lib.iccf, chr code format --file=src/main.rell printed Formatting src/main.rell... no changes, default create .rell_format max_line_width=120, HELP ONLY WRITE SKIP --fix) encoded as leftover_code_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no install iccf, no invented RID, no new pages).
// Local repl walk (chr repl -c '1+1' → 2 exit 0, chr repl --module main -c '1+1' → 2 exit 0, chr repl --blockchain my_rell_dapp -c '1+1' → 2 exit 0, chr repl --module main -c 'hello_world()' → No database connection exit 1, chr repl --module main --use-db -c 'hello_world()' → "Hello World!" exit 0 SqlInit chain_iid=0, chr repl --module main --use-db --sql-log -c 'hello_world()' → SqlConnectionLogger SELECT A00.\"name\" FROM \"c0.my_name\" A00 "Hello World!" exit 0, set_name Type rell.test.op Switch to a different output format exit 0 HELP ONLY WRITE SKIP ops/txs, -f JSON matches pretty, -f XML <int>2</int> <string>Hello World!</string>, -f YAML Error: Unsupported output format YAML exit 1 help mismatch, printf '1+1' | chr repl - → 2, printf 'args' | chr repl - alpha one → [\"alpha\", \"one\"], Cannot use -c when specifying script file exit 1, --sql-log without --use-db No database connection exit 1) encoded as leftover_repl_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no install, no invented RID, no new pages).
// Local tools walk (chr tools --help gtv validate-config lib-model exit 0, chr tools gtv --hex official sample pretty/JSON/XML/raw/YAML all exit 0 FOO/BAR, chr gtv alias same, missing --hex stdin hang / empty pipe Invalid GTV data exit 1, --hex ZZ Char Z is not a hex digit exit 1, validate-config missing --file exit 1, -f src/main.rell Unsupported file format exit 1, parent-dir -f my-rell-dapp/chromia.yml No issues found exit 0, bare -f chromia.yml getParent must not be null exit 3, lib-model missing --library-source exit 1, invalid --registry exit 1, full flags prints libs: block computes rid — do not invent RID never paste 64-hex, HELP ONLY WRITE SKIP install) encoded as leftover_tools_* (query-only HELP ONLY WRITE SKIP; no keys, no signed FT4 ops, no install, no invented RID, no new pages).
// Local tools deep walk (chr tools with no subcommand = chr tools --help exit 0, gtv -f yaml lowercase ok, official binary stdin chr gtv --output-format yaml < data.gtv ok, --hex wins over stdin, --hash=1/2 print a computed 64-char Merkle hash never recorded here, --hash=0 must be greater than 0 exit 1, validate-config ./ or absolute path ok while a bare filename NPEs exit 3, nonexistent/dir path exit 1, .yaml accepted, cookbook keys test.timeout/test.parallel/database.schema_version rejected Additional property ... exit 2, unknown top-level section exit 2, scaffold_dapp chromia.yml No issues found exit 0, lib-model printed shape + --insecure=true, lib-model over src/lib/ft4 reproduced the FT4 rid already on disk) encoded as leftover_tools_* (query-only HELP ONLY WRITE SKIP; no keys, no signed ops, no invented RID, no pasted 64-hex, no new pages).
// Local query after node walk (chr node start Node is initialized Chain-id 0 REST http://localhost:7740, chr query hello_world "Hello World!" exit 0 needs no --blockchain-rid, -f pretty/JSON/XML/raw/YAML all exit 0 YAML works unlike repl, Unknown query / Invalid argument(s) / missing --network / Char Z is not a hex digit / Wrong size of Blockchain RID exit 3, REST GET / Postchain REST API 200, HELP ONLY WRITE SKIP chr tx signed send, never paste computed BRID) encoded as leftover_query_* (query-only HELP ONLY WRITE SKIP; no keys, no signed ops, no invented RID, no pasted 64-hex, no new pages).
// Local deploy HELP ONLY walk (chr deploy is not a 0.33.2 subcommand — Error: no such subcommand deploy. Did you mean deployment?; chr deployment --help create info inspect update resume pause remove proposal voterset container exit 0; create/update --help flags recorded and NEVER run; info/inspect read-only; local inspect printed hello_world/set_name/my_name, --list-modules ["main"], --signature, --module-args [], -f table; info against the local node Cluster not found + Unknown query: cm_get_blockchain_cluster; --network testnet Specified target [testnet] does not exist exit 1; --blockchain-rid ZZ/AB exit 3) encoded as leftover_deploy_* (query-only HELP ONLY WRITE SKIP; nothing deployed, no signing, no keys, no invented BlockchainRID, no invented container id, no pasted 64-hex, no new pages; settings missing -s does not exist exit 1 --blockchain missing --network proposal / voterset missing --network container --help WRITE SKIP next vault-lease HELP ONLY).
// Local query deep walk (chr query --help official Examples primitive_args/dict_arg/map_arg/struct_arg + named-parameters-only rule, chr query hello_world 1 -> query must be done with named parameters in a dict exit 1, --foo -> no such option Did you mean -f?, --cid hello_world -> not a valid integer, dict arg on a no-arg query -> Invalid argument(s): arg, --output-format long form ok, get_version/ft4.get_version/get_api_version -> Unknown query so FT4 is not mounted, REST /metadata/iid_0 built-in GTX queries last_block_info + tx_confirmation_time and ops nop/__nop/timeb, chr query last_block_info dict blockRID/height/timestamp exit 0 in all five formats and it ignores extra args, tx_confirmation_time without txRID -> 500 Unknown error, REST /query/iid_0?type=hello_world 200 Hello World! via the iid_<chainIid> alias so no BRID is needed, POST /query/iid_0 200, Missing query type 400, QUERY_NOT_FOUND 400, chain Iid: 99 404, /query_gtv octet-stream, /dquery + /web_query need purpose-built Rell queries, /blockchain/iid_0/height + nodestate + config features, /version 22, /infrastructure_version postchain 3.49.16, /_debug moved to 7750, OPTIONS CORS, /apidocs rapi-doc spec, and chr node --help has no stop subcommand) encoded as leftover_query_* (query-only HELP ONLY WRITE SKIP; no keys, no signed ops, no invented RID, no pasted 64-hex, no new pages).

// Local vault lease HELP ONLY walk (vault_lease_help titles Get a container for your dapp / Hosting / Supported wallets; workflow faucet→Vault containers→lease with existing pubkey→chromia.yml deployments.<net>.container placeholder <containerIID>→chr deployment create WRITE SKIP; never invent container id; never paste Directory 64-hex; this tool does not generate a key) encoded as leftover_vault_* (query-only HELP ONLY WRITE SKIP; no keys, no signed txs, no invented RID, no invented container id, no pasted 64-hex, no new pages).
// Local tx HELP ONLY walk (chr tx --help FT4/ICCF/Examples exit 0; missing <opname> exit 1; no node Connection Refused exit 1; --blockchain-rid ZZ/AB exit 3; --network testnet Specified target does not exist exit 1; missing --network / missing -s / --cid hello / --foo exit 1) encoded as leftover_tx_* (query-only HELP ONLY WRITE SKIP; do not send; no keys, no invented RID, no pasted 64-hex, no new pages).
// Local multi-signature HELP ONLY walk (chr multi-signature --help create/sign/send/view exit 0; view --help -f/--file; view missing --file exit 1; view missing file exit 1; create/sign/send WRITE SKIP) encoded as leftover_multi_* (query-only HELP ONLY WRITE SKIP; no keys, no signed txs, no invented RID, no pasted 64-hex, no new pages).
// Local seeder HELP ONLY walk (chr seeder --help Generate fake data for a local database init/generate exit 0; chr seed Did you mean seeder? exit 1; chr seeder seed no such subcommand seed exit 1; init --help -s/-bc; generate --help --alternative-config-folder; init/generate NEVER run WRITE SKIP; missing -s file does not exist exit 1; no project Project settings file not found exit 1) encoded as leftover_seeder_* (query-only HELP ONLY WRITE SKIP; no keys, no signed txs, no invented RID, no invented container id, no pasted 64-hex, no invented seeder.yml keys, no new pages; eif HELP ONLY walk done; next version HELP ONLY).
// Local eif HELP ONLY walk (chr eif --help Ethereum Integration Framework commands generate-events-config exit 0; generate-events-config --help --abi/--events/--target/--format; generate NEVER run WRITE SKIP; missing --abi/--events exit 1; --foo Did you mean --format? exit 1; seed/generate/foo no such subcommand; missing ABI path does not exist exit 1; --format badformat invalid choice exit 1; --abi /tmp MalformedJsonException exit 3; never wrote build/eif-events.yaml) encoded as leftover_eif_* (query-only HELP ONLY WRITE SKIP; no keys, no signed txs, no invented RID, no pasted 64-hex, no invented ABI event YAML keys, no new pages; next version HELP ONLY).
