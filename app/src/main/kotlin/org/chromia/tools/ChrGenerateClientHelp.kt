package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr generate client-stubs` / graph / docs-site help
 * plus official postchain-client / FT4 query-only wiring.
 * Does not run chr, generate keys, or send signed transactions.
 * Source: docs.chromia.com/build/cli/commands/generate and GenerateCommand (0.33.x),
 * JS/TS client + reference, FT4 client-setup, testnet/mainnet connect-client.
 * Official createClient setting name is blockchainRid.
 */
object ChrGenerateClientHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/cli/commands/generate"
    const val GENERATE_INDEX_URL = DOCS_URL
    const val GENERATE_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/generate/"
    const val GENERATE_INDEX_TITLE = "generate"  // official H1
    const val DOCS_SITE_URL = "https://docs.chromia.com/build/cli/generating-doc-site"
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val JS_CLIENT_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/"
    const val JS_QUICKSTART_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart"
    const val RUN_DAPP_CLI_URL = "https://docs.chromia.com/get-started/create-dapp/run-dapp-cli"
    const val JS_REFERENCE_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference"
    const val FT4_CLIENT_SETUP_URL = "https://docs.chromia.com/build/ft4/client/client-setup"
    const val FT4_CLIENT_SETUP_URL_SLASH = "https://docs.chromia.com/build/ft4/client/client-setup/"
    const val FT4_EMPTY_ASSETS = "{ data: [], nextCursor: null }"
    const val KOTLIN_CLIENT_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/kotlin-client"
    const val PYTHON_CLIENT_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/python-client"
    const val TESTNET_CONNECT_URL = "https://docs.chromia.com/build/deployment/testnet/connect-client"
    const val TESTNET_CONNECT_INDEX_URL = TESTNET_CONNECT_URL
    const val TESTNET_CONNECT_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/testnet/connect-client/"
    const val TESTNET_CONNECT_INDEX_TITLE = "Connect a client"  // official H1
    const val MAINNET_CONNECT_URL = "https://docs.chromia.com/build/deployment/mainnet/connect-client"
    const val MAINNET_CONNECT_INDEX_URL = MAINNET_CONNECT_URL
    const val MAINNET_CONNECT_INDEX_URL_SLASH = "https://docs.chromia.com/build/deployment/mainnet/connect-client/"
    const val MAINNET_CONNECT_INDEX_TITLE = "Connect a client"  // official H1
    const val ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-erc20-token"
    const val ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-erc20-token/"
    const val ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_TITLE = "Deploy an ERC20 token"  // official H1
    const val ECOSYSTEM_NODE_MAINTENANCE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/node-maintenance-guidelines"
    const val ECOSYSTEM_NODE_MAINTENANCE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/node-maintenance-guidelines/"
    const val ECOSYSTEM_NODE_MAINTENANCE_INDEX_TITLE = "Node maintenance guidelines"  // official H1
    const val ECOSYSTEM_PMC_NETWORK_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/network"
    const val ECOSYSTEM_PMC_NETWORK_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/network/"
    const val ECOSYSTEM_PMC_NETWORK_INDEX_TITLE = "network"  // official H1
    const val LEARN_CI_INTRODUCTION_INDEX_URL = "https://learn.chromia.com/courses/continuous-integration/introduction"
    const val LEARN_CI_INTRODUCTION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/continuous-integration/introduction/"
    const val LEARN_CI_INTRODUCTION_INDEX_TITLE = "Guide to Using the Chromia CLI for Testing and Deploying Rell Code on Chromia"  // official H1
    const val LEARN_ICMF_DELIVERY_CHAIN_INDEX_URL = "https://learn.chromia.com/courses/icmf-course/delivery-chain"
    const val LEARN_ICMF_DELIVERY_CHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/icmf-course/delivery-chain/"
    const val LEARN_ICMF_DELIVERY_CHAIN_INDEX_TITLE = "Delivery chain (receive message)"  // official H1
    const val LEARN_NEWS_MODULE_TWO_SETUP_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-two/setup"
    const val LEARN_NEWS_MODULE_TWO_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-two/setup/"
    const val LEARN_NEWS_MODULE_TWO_SETUP_INDEX_TITLE = "Set up the project"  // official H1
    const val LEARN_TTT_WHAT_NEXT_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/what-next"
    const val LEARN_TTT_WHAT_NEXT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/what-next/"
    const val LEARN_TTT_WHAT_NEXT_INDEX_TITLE = "What next?"  // official H1
    const val LEARN_TTT_CREATE_ACCOUNTS_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/create-accounts"
    const val LEARN_TTT_CREATE_ACCOUNTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/create-accounts/"
    const val LEARN_TTT_CREATE_ACCOUNTS_INDEX_TITLE = "Lesson 2 - Create accounts"  // official H1
    const val LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-setup-run"
    const val LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-setup-run/"
    const val LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_TITLE = "Frontend: setup and run"  // official H1
    const val LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_URL = "https://learn.chromia.com/courses/relationships-course/many-to-many"
    const val LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/relationships-course/many-to-many/"
    const val LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_TITLE = "Many-to-many relationships"  // official H1
    const val NPM_POSTCHAIN = "postchain-client"
    const val NPM_FT4 = "@chromia/ft4"
    const val PIP_POSTCHAIN = "postchain-client-py"
    const val MAVEN_POSTCHAIN = "net.postchain.client:postchain-client"
    const val MAVEN_CHROMIA = "net.postchain.client:chromia-client"
    const val MAVEN_FT4 = "net.postchain.client:ft4-client"
    const val PYTHON_MIN = "3.7+"
    const val KOTLIN_LOCAL_ENDPOINT = "http://127.0.0.1:7740"
    const val OUTDATED_BANNER = "We are currently updating this documentation"
    const val PYTHON_ENV_NODE = "POSTCHAIN_TEST_NODE"
    const val PYTHON_ENV_RID = "BLOCKCHAIN_TEST_RID"
    const val PYTHON_QUERY_REVIEWS = "get_all_reviews_for_book"
    const val KOTLIN_STANDARD_CLIENT = "StandardChromiaClient"
    val leftoverOfficialKotlinMavenRepos = listOf(
        "https://gitlab.com/api/v4/projects/50818999/packages/maven",
        "https://gitlab.com/api/v4/projects/32294340/packages/maven",
        "https://gitlab.com/api/v4/projects/46288950/packages/maven"
    )
    const val MAINNET_DIRECTORY_BRID = WriteDeploymentConfig.MAINNET_DIRECTORY_BRID
    const val TESTNET_DIRECTORY_BRID = WriteDeploymentConfig.TESTNET_DIRECTORY_BRID

    val languages = listOf("kotlin", "typescript", "javascript", "python")
    val createClientSettings = listOf(
        "nodeUrlPool",
        "directoryNodeUrlPool",
        "blockchainRid",
        "blockchainIid",
        "statusPollInterval",
        "statusPollCount",
        "failOverConfig",
        "useStickyNode"
    )
    const val DIRECTORY_CHAIN_IID = 0
    const val SOURCE_BRID_SETTING = "blockchainRid"
    const val LEFTOVER_OFFICIAL_PROSE_BRID = "blockchainRID"
    const val FAILOVER_DEFAULT_STRATEGY = "Abort On Error"
    const val FAILOVER_DEFAULT_ATTEMPTS = 3
    const val FAILOVER_DEFAULT_INTERVAL_MS = 5000
    const val FAILOVER_DEFAULT_UNREACHABLE_MS = 30000
    const val FAILOVER_DEFAULT_STATUS_POLL_COUNT = 1
    const val SOURCE_FAILOVER_ABORT = "abortOnError"
    const val LEFTOVER_OFFICIAL_STRATEGY_TYPO = "startegy"
    const val LEFTOVER_OFFICIAL_ABORT_TYPO = "abortOnErrror"
    val leftoverOfficialFailoverStrategies = listOf(
        "Abort on error",
        "Try next on error",
        "Single endpoint",
        "Query majority"
    )

    val docsKeys = ChromiaDocsYmlHelp.keys

    fun docsYaml(): String = ChromiaDocsYmlHelp.docsYaml()

    fun localCreateClient(): String = """
        import { createClient } from "postchain-client";

        const client = await createClient({
          nodeUrlPool: "http://localhost:7740",
          blockchainRid: "<BlockchainRID>",
        });
        const result = await client.query("hello_world");
    """.trimIndent() + "\n"

    fun testnetCreateClient(): String = """
        import { createClient } from "postchain-client";

        const client = await createClient({
          directoryNodeUrlPool: [
            "https://node0.testnet.chromia.com:7740",
            "https://node1.testnet.chromia.com:7740",
            "https://node2.testnet.chromia.com:7740",
            "https://node3.testnet.chromia.com:7740",
          ],
          blockchainRid: "<BlockchainRID>",
        });
        const result = await client.query("hello_world");
    """.trimIndent() + "\n"

    fun mainnetCreateClient(): String = """
        import { createClient } from "postchain-client";

        const client = await createClient({
          directoryNodeUrlPool: [
            "https://system.chromaway.com",
            "https://mainnet-dapp1.sunube.net:7740",
          ],
          blockchainRid: "<BlockchainRID>",
        });
        const result = await client.query("hello_world");
    """.trimIndent() + "\n"

    fun ft4LocalConnection(): String = """
        const { createClient } = require("postchain-client");
        const { createConnection } = require("@chromia/ft4");

        const client = await createClient({
          nodeUrlPool: "http://localhost:7740",
          blockchainIid: 0,
        });
        const connection = createConnection(client);
        const assets = await connection.getAllAssets();
    """.trimIndent() + "\n"

    fun kotlinQuery(): String =
        "psClient.query(\"hello_world\", GtvFactory.gtv(mapOf()))"

    fun pythonQuery(): String = """
        from postchain_client_py import BlockchainClient
        from postchain_client_py.blockchain_client.types import NetworkSettings

        settings = NetworkSettings(
            node_url_pool=["http://localhost:7740"],
            blockchain_rid="<BlockchainRID>",
        )
        client = await BlockchainClient.create(settings)
        result = await client.query("hello_world")
    """.trimIndent() + "\n"

    fun leftoverOfficialStandardChromiaClient(): String =
        "val chromiaClient = StandardChromiaClient(\"http://127.0.0.1:7740\")" + "\n"

    fun leftoverOfficialPythonReviewsQuery(): String =
        "reviews = await client.query(\"get_all_reviews_for_book\", {\"isbn\": \"ISBN123\"})" + "\n"

    fun leftoverOfficialPythonEnv(): String = """
        POSTCHAIN_TEST_NODE=http://localhost:7740
        BLOCKCHAIN_TEST_RID=your_blockchain_rid
    """.trimIndent() + "\n"

    fun leftoverOfficialStickyQueryClient(): String = """
        const client = createClient({
          useStickyNode: true,
          directoryNodeUrlPool: ["http://localhost:7740"],
        });
    """.trimIndent() + "\n"

    fun helloWorldRell(): String = """
        module;

        object my_name {
          mutable name = "World";
        }

        query hello_world() = "Hello %s!".format(my_name.name);
    """.trimIndent() + "\n"


    fun notes(): String = """
        Chromia CLI $CLI_SERIES `chr generate` help. Java 21+, Postgres 16+.
        Official page: $DOCS_URL
        Docs-site page: $DOCS_SITE_URL
        User-facing command is `chr generate client-stubs` (not a top-level `chr generate-client`).
        The top-level alias `generate-client-stubs` is deprecated; use `chr generate client-stubs`.
        Languages: ${languages.joinToString(", ")} (`--kotlin`, `--typescript`, `--javascript`, `--python`).
        Kotlin also takes `--package=<text>`.
        `-m, --module` is a module name (e.g. main), never a file path. Separate modules with ','.
        `-d, --target` is the output directory.
        Siblings: `chr generate graph` (mermaid entity relations; `--mdx`, `--entity-relation` / `--class-diagram`)
        and `chr generate docs-site` (configure the official `docs:` section in chromia.yml — $PROJECT_CONFIG_URL).
        Official `docs:` keys only: ${docsKeys.joinToString(", ")} (see chromia_docs_yml_help).
        GitHub / GitLab `sourceLink.remoteLineSuffix` is "#L"; Bitbucket is "#lines-".
        `chr generate docs-site -i, --include=<text>` takes a lib identifier (e.g. lib.foo).
        `--hide-lib-warnings` is official on `chr generate client-stubs`, `chr generate graph`, and `chr generate docs-site`.
        Official JS/TS setting name is blockchainRid. See packages, create_client_settings, local_create_client,
        testnet_create_client, mainnet_create_client, ft4_local_connection, kotlin_query, python_query.
        Official BUILD hello-world-quickstart (200): $JS_QUICKSTART_URL. Official Node.js 18+. Official query-only object my_name query hello_world() Hello %s!.format(my_name.name). client.query("hello_world"). default Hello World!. run-dapp-cli (200): $RUN_DAPP_CLI_URL. REAL bug: page still prints DEV-ONLY sample keys and signAndSendUniqueTransaction set_name; query-only wins. run-dapp-cli still prints chromia. yml (space); chromia.yml wins. Skip printed sample keys set_name tx keygen signAndSendUniqueTransaction.
        Official BUILD JS/TS reference (200): $JS_REFERENCE_URL. page still says We are currently updating this documentation some details may be outdated. Directory Chain Iid $DIRECTORY_CHAIN_IID. useStickyNode page still prints succefull. nodeUrlPool described as array example string http://localhost:7740 source NetworkSettings string or string[] example string wins. failover defaults $FAILOVER_DEFAULT_STRATEGY attemptsPerEndpoint $FAILOVER_DEFAULT_ATTEMPTS attemptInterval $FAILOVER_DEFAULT_INTERVAL_MS unreachableDuration $FAILOVER_DEFAULT_UNREACHABLE_MS statusPollCount $FAILOVER_DEFAULT_STATUS_POLL_COUNT sticky useStickyNode directoryNodeUrlPool http://localhost:7740 prose $LEFTOVER_OFFICIAL_PROSE_BRID parameter $SOURCE_BRID_SETTING source $SOURCE_BRID_SETTING wins. REAL bug: page still prints sample 64-hex blockchainRid from echo A blockchain example sha256sum $LEFTOVER_OFFICIAL_STRATEGY_TYPO $LEFTOVER_OFFICIAL_ABORT_TYPO three failover strategies lists four source $SOURCE_FAILOVER_ABORT wins typed query get_fobar get_foobar wins; BlockchainRID placeholder wins. Skip dummy Buffer.alloc keys secp256k1 newSignatureProvider printed sample 64-hex signAndSend.
        Official BUILD kotlin-client (200): $KOTLIN_CLIENT_URL outdated banner GitLab maven EndpointPool.singleUrl $KOTLIN_LOCAL_ENDPOINT query hello_world StandardChromiaClient without sample hex $OUTDATED_BANNER. REAL bug: Chromia client Gradle still prints $MAVEN_POSTCHAIN Maven $MAVEN_CHROMIA wins. Skip printed sample keys set_name awaitAnchoredTx sample 64-hex FT4 printed keys.
        Official BUILD python-client (200): $PYTHON_CLIENT_URL outdated banner Python $PYTHON_MIN pip $PIP_POSTCHAIN aiohttp query get_collections get_all_books get_all_reviews_for_book POSTCHAIN_TEST_NODE BLOCKCHAIN_TEST_RID NetworkSettings node_url_pool blockchain_rid YOUR_BLOCKCHAIN_RID rest_client.close source BlockchainRID placeholder wins. Skip PRIV_KEY coincurve sign_transaction send_transaction generate private key.
        Official BUILD FT4 client-setup (200 with trailing slash): $FT4_CLIENT_SETUP_URL_SLASH $FT4_CLIENT_SETUP_URL 307 createConnection getAllAssets empty $FT4_EMPTY_ASSETS blockchainIid 0 http://localhost:7740 @chromia/ft4 postchain-client. Skip signed txs key generation.
        Query-only wiring. Skipped: signed txs and key generation. See chr_key_id_help, chromia_cookbook_help,
        and chromia_language_clients_help (C# / Go / Rust / React / REST).
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official BUILD cli/commands/generate ($GENERATE_INDEX_URL 307 $GENERATE_INDEX_URL_SLASH 200 $GENERATE_INDEX_TITLE): intro Usage chr generate client-stubs graph docs-site Query-only WRITE SKIP skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no keygen do not invent flags do not document chr tx signed send keygen samples.
        Official BUILD deployment/testnet/connect-client ($TESTNET_CONNECT_INDEX_URL 307 $TESTNET_CONNECT_INDEX_URL_SLASH 200 $TESTNET_CONNECT_INDEX_TITLE): intro connect frontend postchain-client Directory chain Blockchain RID Chromia Explorer Testnet five minutes directoryNodeUrlPool pcl.createClient Query-only WRITE SKIP skip signed txs sample admin pubkey PrivkeyLink adminPubkey adminPrivkey Buffer.from keygen no sample keys no invented 64-hex no keygen prose blockchainRID source $SOURCE_BRID_SETTING wins do not invent 64-hex do not document signed send keygen admin key pair samples.
        Official BUILD deployment/mainnet/connect-client ($MAINNET_CONNECT_INDEX_URL 307 $MAINNET_CONNECT_INDEX_URL_SLASH 200 $MAINNET_CONNECT_INDEX_TITLE): intro connect frontend postchain-client Directory chain Blockchain RID Chromia Explorer Mainnet directoryNodeUrlPool pcl.creatClient Query-only WRITE SKIP skip signed txs sample admin pubkey PubkeyLink PrivkeyLink adminPubkey adminPrivkey Buffer.from keygen no sample keys no invented 64-hex no keygen prose blockchainRID source $SOURCE_BRID_SETTING wins do not invent 64-hex do not document signed send keygen admin key pair samples.
        Official ECOSYSTEM ecosystem/bridge/deploy-bridge/deploy-erc20-token INDEX ($ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_URL 307 $ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/nodes/node-maintenance-guidelines INDEX ($ECOSYSTEM_NODE_MAINTENANCE_INDEX_URL 307 $ECOSYSTEM_NODE_MAINTENANCE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_NODE_MAINTENANCE_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/network INDEX ($ECOSYSTEM_PMC_NETWORK_INDEX_URL 307 $ECOSYSTEM_PMC_NETWORK_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_NETWORK_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/continuous-integration/introduction INDEX ($LEARN_CI_INTRODUCTION_INDEX_URL 301 $LEARN_CI_INTRODUCTION_INDEX_URL_SLASH 200 H1 $LEARN_CI_INTRODUCTION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/icmf-course/delivery-chain INDEX ($LEARN_ICMF_DELIVERY_CHAIN_INDEX_URL 301 $LEARN_ICMF_DELIVERY_CHAIN_INDEX_URL_SLASH 200 H1 $LEARN_ICMF_DELIVERY_CHAIN_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-two/setup INDEX ($LEARN_NEWS_MODULE_TWO_SETUP_INDEX_URL 301 $LEARN_NEWS_MODULE_TWO_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_MODULE_TWO_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/tic-tac-toe/what-next INDEX ($LEARN_TTT_WHAT_NEXT_INDEX_URL 301 $LEARN_TTT_WHAT_NEXT_INDEX_URL_SLASH 200 H1 $LEARN_TTT_WHAT_NEXT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX courses/tic-tac-toe/module-one/create-accounts ($LEARN_TTT_CREATE_ACCOUNTS_INDEX_URL 301 $LEARN_TTT_CREATE_ACCOUNTS_INDEX_URL_SLASH GET 200 H1 $LEARN_TTT_CREATE_ACCOUNTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof frontend-setup-run ($LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_URL 301 $LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_URL_SLASH GET 200 H1 $LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX relationships-course many-to-many ($LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_URL 301 $LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_URL_SLASH GET 200 H1 $LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("generate_index_docs", GENERATE_INDEX_URL)
        put("generate_index_url_slash", GENERATE_INDEX_URL_SLASH)
        put("generate_index_title", GENERATE_INDEX_TITLE)
        put("docs_site", DOCS_SITE_URL)
        put("project_config", PROJECT_CONFIG_URL)
        put(
            "commands",
            buildJsonObject {
                put("client_stubs", "chr generate client-stubs")
                put("client_stubs_hide_lib_warnings", "chr generate client-stubs --hide-lib-warnings")
                put("kotlin", "chr generate client-stubs --kotlin --package com.example.client")
                put("typescript", "chr generate client-stubs --typescript")
                put("javascript", "chr generate client-stubs --javascript")
                put("python", "chr generate client-stubs --python")
                put("graph", "chr generate graph")
                put("docs_site", "chr generate docs-site")
            }
        )
        put(
            "flags",
            buildJsonObject {
                put("settings", "-s, --settings=<settings>")
                put("module", "-m, --module=<text>  # module names, comma-separated; not a file path")
                put("target", "-d, --target=<path>")
                put("hide_lib_warnings", "--hide-lib-warnings  # official on client-stubs, graph, and docs-site")
                put("language", "--kotlin, --typescript, --javascript, --python")
                put("kotlin_package", "--package=<text>  # kotlin only")
                put("graph_mdx", "--mdx")
                put("graph_style", "--entity-relation / --class-diagram")
                put("docs_include", "-i, --include=<text>  # lib identifier, e.g. lib.foo")
            }
        )
        put(
            "languages",
            kotlinx.serialization.json.buildJsonArray {
                languages.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
            }
        )
        put(
            "docs_keys",
            kotlinx.serialization.json.buildJsonArray {
                docsKeys.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
            }
        )
        put("docs_yaml", docsYaml())
        put("docs_yml_help", ChromiaDocsYmlHelp.TOOL_NAME)
        put("js_client", JS_CLIENT_URL)
        put("js_quickstart", JS_QUICKSTART_URL)
        put("run_dapp_cli", RUN_DAPP_CLI_URL)
        put("hello_world_rell", helloWorldRell())
        put("hello_world_result", "Hello World!")
        put("js_reference", JS_REFERENCE_URL)
        put("js_reference_directory_iid", DIRECTORY_CHAIN_IID)
        put("js_reference_brid_setting", SOURCE_BRID_SETTING)
        put("js_reference_leftover_official_prose_brid", LEFTOVER_OFFICIAL_PROSE_BRID)
        put(
            "js_reference_failover_defaults",
            buildJsonObject {
                put("strategy", FAILOVER_DEFAULT_STRATEGY)
                put("attemptsPerEndpoint", FAILOVER_DEFAULT_ATTEMPTS)
                put("attemptInterval_ms", FAILOVER_DEFAULT_INTERVAL_MS)
                put("unreachableDuration_ms", FAILOVER_DEFAULT_UNREACHABLE_MS)
                put("statusPollCount", FAILOVER_DEFAULT_STATUS_POLL_COUNT)
            }
        )
        put(
            "js_reference_failover_strategies",
            buildJsonArray { leftoverOfficialFailoverStrategies.forEach { add(JsonPrimitive(it)) } }
        )
        put("js_reference_sticky_create_client", leftoverOfficialStickyQueryClient())
        put("js_reference_leftover_official_strategy_typo", LEFTOVER_OFFICIAL_STRATEGY_TYPO)
        put("js_reference_leftover_official_abort_typo", LEFTOVER_OFFICIAL_ABORT_TYPO)
        put("js_reference_source_failover_abort", SOURCE_FAILOVER_ABORT)
        put("js_reference_query_foobar", "chromiaClient.query(\"get_foobar\", { foo: 1, bar: 2 })")
        put("js_reference_leftover_official_query_typo", "get_fobar")
        put("ft4_client_setup", FT4_CLIENT_SETUP_URL)
        put("ft4_client_setup_slash", FT4_CLIENT_SETUP_URL_SLASH)
        put("ft4_empty_assets", FT4_EMPTY_ASSETS)
        put("testnet_connect", TESTNET_CONNECT_URL)
        put("testnet_connect_index_docs", TESTNET_CONNECT_INDEX_URL)
        put("testnet_connect_index_url_slash", TESTNET_CONNECT_INDEX_URL_SLASH)
        put("testnet_connect_index_title", TESTNET_CONNECT_INDEX_TITLE)
        put("mainnet_connect", MAINNET_CONNECT_URL)
        put("mainnet_connect_index_docs", MAINNET_CONNECT_INDEX_URL)
        put("mainnet_connect_index_url_slash", MAINNET_CONNECT_INDEX_URL_SLASH)
        put("mainnet_connect_index_title", MAINNET_CONNECT_INDEX_TITLE)
        put("mainnet_directory_brid", MAINNET_DIRECTORY_BRID)
        put("testnet_directory_brid", TESTNET_DIRECTORY_BRID)
        put(
            "packages",
            buildJsonObject {
                put("npm_postchain", NPM_POSTCHAIN)
                put("npm_ft4", NPM_FT4)
                put("pip_postchain", PIP_POSTCHAIN)
                put("maven_postchain", MAVEN_POSTCHAIN)
                put("maven_chromia", MAVEN_CHROMIA)
                put("maven_ft4", MAVEN_FT4)
            }
        )
        put("kotlin_client", KOTLIN_CLIENT_URL)
        put("python_client", PYTHON_CLIENT_URL)
        put("python_min", PYTHON_MIN)
        put("kotlin_local_endpoint", KOTLIN_LOCAL_ENDPOINT)
        put("kotlin_chromia_maven", MAVEN_CHROMIA)
        put("kotlin_leftover_official_chromia_gradle", MAVEN_POSTCHAIN)
        put("python_query_collections", "await client.query(\"get_collections\")")
        put("python_query_books", "await client.query(\"get_all_books\")")
        put("python_query_reviews", leftoverOfficialPythonReviewsQuery())
        put("python_close", "await client.rest_client.close()")
        put("python_async", "aiohttp")
        put("leftover_official_outdated_banner", OUTDATED_BANNER)
        put("python_env_node", PYTHON_ENV_NODE)
        put("python_env_rid", PYTHON_ENV_RID)
        put("python_env", leftoverOfficialPythonEnv())
        put("kotlin_standard_chromia_client", leftoverOfficialStandardChromiaClient())
        put(
            "kotlin_maven_repos",
            buildJsonArray { leftoverOfficialKotlinMavenRepos.forEach { add(JsonPrimitive(it)) } }
        )
        put("key_id_help", ChrKeyIdHelp.TOOL_NAME)
        put("cookbook_help", "chromia_cookbook_help")
        put("language_clients_help", "chromia_language_clients_help")
        put(
            "create_client_settings",
            buildJsonArray { createClientSettings.forEach { add(JsonPrimitive(it)) } }
        )
        put("local_create_client", localCreateClient())
        put("testnet_create_client", testnetCreateClient())
        put("mainnet_create_client", mainnetCreateClient())
        put("ft4_local_connection", ft4LocalConnection())
        put("kotlin_query", kotlinQuery())
        put("python_query", pythonQuery())
        put(
            "directory_pools",
            buildJsonObject {
                put(
                    "testnet",
                    buildJsonArray {
                        WriteDeploymentConfig.TESTNET_URLS.forEach { add(JsonPrimitive(it)) }
                    }
                )
                put(
                    "mainnet_project_config",
                    buildJsonArray {
                        WriteDeploymentConfig.MAINNET_URLS.forEach { add(JsonPrimitive(it)) }
                    }
                )
            }
        )
        put("ecosystem_deploy_erc20_token_index_url_slash", ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_URL_SLASH)
        put("ecosystem_deploy_erc20_token_index_title", ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_TITLE)
        put("ecosystem_node_maintenance_index_url_slash", ECOSYSTEM_NODE_MAINTENANCE_INDEX_URL_SLASH)
        put("ecosystem_node_maintenance_index_title", ECOSYSTEM_NODE_MAINTENANCE_INDEX_TITLE)
        put("ecosystem_pmc_network_index_url_slash", ECOSYSTEM_PMC_NETWORK_INDEX_URL_SLASH)
        put("ecosystem_pmc_network_index_title", ECOSYSTEM_PMC_NETWORK_INDEX_TITLE)
        put("learn_ci_introduction_index_url_slash", LEARN_CI_INTRODUCTION_INDEX_URL_SLASH)
        put("learn_ci_introduction_index_title", LEARN_CI_INTRODUCTION_INDEX_TITLE)
        put("learn_icmf_delivery_chain_index_url_slash", LEARN_ICMF_DELIVERY_CHAIN_INDEX_URL_SLASH)
        put("learn_icmf_delivery_chain_index_title", LEARN_ICMF_DELIVERY_CHAIN_INDEX_TITLE)
        put("learn_news_module_two_setup_index_url_slash", LEARN_NEWS_MODULE_TWO_SETUP_INDEX_URL_SLASH)
        put("learn_news_module_two_setup_index_title", LEARN_NEWS_MODULE_TWO_SETUP_INDEX_TITLE)
        put("learn_ttt_what_next_index_url_slash", LEARN_TTT_WHAT_NEXT_INDEX_URL_SLASH)
        put("learn_ttt_what_next_index_title", LEARN_TTT_WHAT_NEXT_INDEX_TITLE)
        put("learn_ttt_create_accounts_index_url_slash", LEARN_TTT_CREATE_ACCOUNTS_INDEX_URL_SLASH)
        put("learn_ttt_create_accounts_index_title", LEARN_TTT_CREATE_ACCOUNTS_INDEX_TITLE)
        put("learn_zk_frontend_setup_run_index_url_slash", LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_URL_SLASH)
        put("learn_zk_frontend_setup_run_index_title", LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_TITLE)
        put("learn_relationships_many_to_many_index_url_slash", LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_URL_SLASH)
        put("learn_relationships_many_to_many_index_title", LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_TITLE)
        put("notes", notes())
    }
}

// Official BUILD cli/commands/generate leftovers encoded as GENERATE_INDEX_* (query-only).
// Official BUILD deployment/testnet/connect-client leftovers encoded as TESTNET_CONNECT_INDEX_* (query-only).
// Official BUILD deployment/mainnet/connect-client leftovers encoded as MAINNET_CONNECT_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/bridge/deploy-bridge/deploy-erc20-token INDEX leftovers encoded as ECOSYSTEM_DEPLOY_ERC20_TOKEN_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/nodes/node-maintenance-guidelines INDEX leftovers encoded as ECOSYSTEM_NODE_MAINTENANCE_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/network INDEX leftovers encoded as ECOSYSTEM_PMC_NETWORK_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/continuous-integration/introduction INDEX leftovers encoded as LEARN_CI_INTRODUCTION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/icmf-course/delivery-chain INDEX leftovers encoded as LEARN_ICMF_DELIVERY_CHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-two/setup INDEX leftovers encoded as LEARN_NEWS_MODULE_TWO_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/what-next INDEX leftovers encoded as LEARN_TTT_WHAT_NEXT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-one/create-accounts INDEX leftovers encoded as LEARN_TTT_CREATE_ACCOUNTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof frontend-setup-run leftovers encoded as LEARN_ZK_FRONTEND_SETUP_RUN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX relationships-course many-to-many leftovers encoded as LEARN_RELATIONSHIPS_MANY_TO_MANY_INDEX_* (query-only HELP ONLY WRITE SKIP).
