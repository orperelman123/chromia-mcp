package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official BUILD query-only wiring for C# / Go / Rust / React Kit / REST.
 * JS/TS, Kotlin, Python, and FT4 local reads live on chr_generate_client_help.
 * Leftover official leftover BUILD clients ft4-client slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients postchain-clients slash/title/child-card leftovers live here (query-only).
 * Leftover official leftover BUILD clients react-kit slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients filehub-client slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients bridge-client slash/title leftovers live here (query-only).
 * Official pages only. Leftover official Filehub work getFile / leftover official MCP setup / leftover official bridge checkAllowance are read-only. Skips signed txs, key generation, FilehubAdministrator writes, leftover official MCP explorer-dump sample BRIDs, and invented package ids.
 * Leftover official leftover BUILD clients mcp-server slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients postchain-clients javascript-typescript slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients postchain-clients python-client slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients postchain-clients kotlin-client slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients postchain-clients c-sharp-client slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients postchain-clients rust-client slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients postchain-clients go-client slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients postchain-rest-api slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients overview slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD clients/postchain-clients/javascript-typescript/hello-world-quickstart leftovers encoded as JS_QUICKSTART_INDEX_* (query-only).
 * Leftover official leftover BUILD clients/postchain-clients/javascript-typescript/reference leftovers encoded as JS_REFERENCE_INDEX_* (query-only).
 * Leftover official leftover BUILD clients/postchain-clients/c-sharp-client leftovers encoded as CSHARP_CLIENT_INDEX_* (query-only).
 * Leftover official leftover BUILD clients/postchain-clients/go-client leftovers encoded as GO_CLIENT_INDEX_* (query-only).
 * Leftover official leftover BUILD clients/postchain-clients/rust-client leftovers encoded as RUST_CLIENT_INDEX_* (query-only).
 * Leftover official leftover BUILD clients/postchain-clients/kotlin-client leftovers encoded as KOTLIN_CLIENT_INDEX_* (query-only).
 * Leftover official leftover BUILD clients/postchain-clients/python-client leftovers encoded as PYTHON_CLIENT_INDEX_* (query-only).
 * Leftover official leftover get-started/about/dapp leftovers encoded as GET_STARTED_DAPP_INDEX_* (query-only).
 * Leftover official leftover get-started/about/chromia-vs-evm leftovers encoded as GET_STARTED_CHROMIA_VS_EVM_INDEX_* (query-only).
 * Leftover official leftover get-started/about/protocols/gtx leftovers encoded as GET_STARTED_GTX_INDEX_* (query-only).
 * Leftover official leftover get-started/about/architecture/chains/system-anchoring-chain leftovers encoded as GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_* (query-only).
 * Leftover official leftover get-started/about/staking/user-delegation leftovers encoded as GET_STARTED_USER_DELEGATION_INDEX_* (query-only).
 * Leftover official leftover get-started/about/protocols/iccf leftovers encoded as GET_STARTED_ICCF_PROTOCOL_INDEX_* (query-only).
 * Leftover official leftover ecosystem/governance/getting-started/governance-structure/user-proposal-flows leftovers encoded as ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_* (query-only).
 * Leftover official leftover ecosystem/governance/governance-voting-process/voting-flow leftovers encoded as ECOSYSTEM_GOV_VOTING_FLOW_INDEX_* (query-only).
 * Leftover official leftover ecosystem/bridge/bridge-client/client leftovers encoded as ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_* (query-only).
 */
object ChromiaLanguageClientsHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val TOOL_NAME = "chromia_language_clients_help"
    const val OVERVIEW_URL = "https://docs.chromia.com/build/clients/overview"
    const val OVERVIEW_URL_SLASH = "https://docs.chromia.com/build/clients/overview/"
    const val OVERVIEW_TITLE = "Clients"
    const val OVERVIEW_INDEX_URL = OVERVIEW_URL
    const val OVERVIEW_INDEX_URL_SLASH = OVERVIEW_URL_SLASH
    const val OVERVIEW_INDEX_TITLE = OVERVIEW_TITLE
    const val OVERVIEW_INDEX_CARD_REST = "Postchain Rest API"
    const val OVERVIEW_INDEX_CARD_REACT = "Chromia React Kit"
    const val OVERVIEW_INDEX_CARD_POSTCHAIN = "Chromia Postchain clients"
    const val OVERVIEW_INDEX_CARD_BRIDGE = "Chromia bridge client"
    const val OVERVIEW_INDEX_CARD_FT4 = "Chromia FT4 client"
    const val OVERVIEW_INDEX_CARD_FILEHUB = "Chromia Filehub client"
    const val OVERVIEW_INDEX_CARD_MCP = "Chromia MCP Server"
    const val DEPLOY_FRONTEND_URL = "https://docs.chromia.com/build/deployment/deploy-frontend-dapp"
    const val WEB_STATIC_LOCAL_URL = "http://localhost:7740/web_query/<blockchainRid>/web_static"
    const val CSHARP_URL = "https://docs.chromia.com/build/clients/postchain-clients/c-sharp-client"
    const val GO_URL = "https://docs.chromia.com/build/clients/postchain-clients/go-client"
    const val RUST_URL = "https://docs.chromia.com/build/clients/postchain-clients/rust-client"
    const val REACT_URL = "https://docs.chromia.com/build/clients/react-kit"
    const val REACT_URL_SLASH = "https://docs.chromia.com/build/clients/react-kit/"
    const val REACT_TITLE = "Chromia React Kit"
    const val REACT_KIT_INDEX_URL = REACT_URL
    const val REACT_KIT_INDEX_URL_SLASH = REACT_URL_SLASH
    const val REACT_KIT_INDEX_TITLE = REACT_TITLE
    const val REST_URL = "https://docs.chromia.com/build/clients/postchain-rest-api"
    const val COOKBOOK_CREATE_RELL_URL = "https://docs.chromia.com/build/cookbook/cli/create-rell-dapp"
    const val JS_QUICKSTART_URL = ChrGenerateClientHelp.JS_QUICKSTART_URL
    const val JS_QUICKSTART_INDEX_URL = "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart"
    const val JS_QUICKSTART_INDEX_URL_SLASH =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart/"
    const val JS_QUICKSTART_INDEX_TITLE = "Hello World Quickstart"
    const val RUN_DAPP_CLI_URL = ChrGenerateClientHelp.RUN_DAPP_CLI_URL
    const val JS_REFERENCE_URL = ChrGenerateClientHelp.JS_REFERENCE_URL
    const val JS_REFERENCE_INDEX_URL = "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference"
    const val JS_REFERENCE_INDEX_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference/"
    const val JS_REFERENCE_INDEX_TITLE = "JavaScript/TypeScript client details"
    const val FILEHUB_CLIENT_URL = "https://docs.chromia.com/build/clients/filehub-client/"
    const val FILEHUB_CLIENT_URL_NO_SLASH = "https://docs.chromia.com/build/clients/filehub-client"
    const val FILEHUB_CLIENT_TITLE = "Chromia Filehub client"
    const val FILEHUB_CLIENT_INDEX_URL = FILEHUB_CLIENT_URL_NO_SLASH
    const val FILEHUB_CLIENT_INDEX_URL_SLASH = FILEHUB_CLIENT_URL
    const val FILEHUB_CLIENT_INDEX_TITLE = FILEHUB_CLIENT_TITLE
    const val BRIDGE_CLIENT_URL = "https://docs.chromia.com/build/clients/bridge-client/"
    const val BRIDGE_CLIENT_URL_NO_SLASH = "https://docs.chromia.com/build/clients/bridge-client"
    const val BRIDGE_CLIENT_TITLE = "Chromia bridge client"
    const val BRIDGE_CLIENT_INDEX_URL = BRIDGE_CLIENT_URL_NO_SLASH
    const val BRIDGE_CLIENT_INDEX_URL_SLASH = BRIDGE_CLIENT_URL
    const val BRIDGE_CLIENT_INDEX_TITLE = BRIDGE_CLIENT_TITLE
    const val FT4_CLIENT_URL = "https://docs.chromia.com/build/clients/ft4-client"
    const val FT4_CLIENT_URL_SLASH = "https://docs.chromia.com/build/clients/ft4-client/"
    const val FT4_CLIENT_TITLE = "Chromia FT4 client"
    const val FT4_CLIENT_INDEX_URL = FT4_CLIENT_URL
    const val FT4_CLIENT_INDEX_URL_SLASH = FT4_CLIENT_URL_SLASH
    const val FT4_CLIENT_INDEX_TITLE = FT4_CLIENT_TITLE
    const val FT4_CLIENT_INTRO_URL = ChromiaFt4QueriesHelp.INTRO_URL
    const val FT4_CLIENT_NPM = ChrGenerateClientHelp.NPM_FT4
    const val POSTCHAIN_CLIENTS_URL = "https://docs.chromia.com/build/clients/postchain-clients"
    const val POSTCHAIN_CLIENTS_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-clients/"
    const val POSTCHAIN_CLIENTS_TITLE = "Chromia Postchain clients"
    const val POSTCHAIN_CLIENTS_INDEX_URL = POSTCHAIN_CLIENTS_URL
    const val POSTCHAIN_CLIENTS_INDEX_URL_SLASH = POSTCHAIN_CLIENTS_URL_SLASH
    const val POSTCHAIN_CLIENTS_INDEX_TITLE = POSTCHAIN_CLIENTS_TITLE
    const val POSTCHAIN_CLIENTS_INTRO_LANGS = "JavaScript/TypeScript, Kotlin, C#, Rust, or Python"
    const val POSTCHAIN_CLIENTS_CARD_JS = "JavaScript (JS)/TypeScript(TS)"
    const val POSTCHAIN_CLIENTS_CARD_KOTLIN = "Kotlin"
    const val POSTCHAIN_CLIENTS_CARD_CSHARP = "C#"
    const val POSTCHAIN_CLIENTS_CARD_RUST = "Rust"
    const val POSTCHAIN_CLIENTS_CARD_PYTHON = "Python"
    const val POSTCHAIN_CLIENTS_CARD_GO = "Go"
    const val POSTCHAIN_CLIENTS_CARD_JS_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript"
    const val JS_TS_INDEX_URL = POSTCHAIN_CLIENTS_CARD_JS_URL
    const val JS_TS_INDEX_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/"
    const val JS_TS_INDEX_TITLE = "JavaScript/TypeScript client"
    const val PYTHON_INDEX_URL = "https://docs.chromia.com/build/clients/postchain-clients/python-client"
    const val PYTHON_INDEX_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-clients/python-client/"
    const val PYTHON_INDEX_TITLE = "Python client"
    const val PYTHON_CLIENT_INDEX_URL = PYTHON_INDEX_URL
    const val PYTHON_CLIENT_INDEX_URL_SLASH = PYTHON_INDEX_URL_SLASH
    const val PYTHON_CLIENT_INDEX_TITLE = PYTHON_INDEX_TITLE
    const val KOTLIN_INDEX_URL = "https://docs.chromia.com/build/clients/postchain-clients/kotlin-client"
    const val KOTLIN_INDEX_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-clients/kotlin-client/"
    const val KOTLIN_INDEX_TITLE = "Kotlin client"
    const val KOTLIN_CLIENT_INDEX_URL = KOTLIN_INDEX_URL
    const val KOTLIN_CLIENT_INDEX_URL_SLASH = KOTLIN_INDEX_URL_SLASH
    const val KOTLIN_CLIENT_INDEX_TITLE = KOTLIN_INDEX_TITLE
    const val CSHARP_INDEX_URL = CSHARP_URL
    const val CSHARP_INDEX_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-clients/c-sharp-client/"
    const val CSHARP_INDEX_TITLE = "C# client"
    const val CSHARP_CLIENT_INDEX_URL = CSHARP_URL
    const val CSHARP_CLIENT_INDEX_URL_SLASH = CSHARP_INDEX_URL_SLASH
    const val CSHARP_CLIENT_INDEX_TITLE = CSHARP_INDEX_TITLE
    const val RUST_INDEX_URL = RUST_URL
    const val RUST_INDEX_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-clients/rust-client/"
    const val RUST_INDEX_TITLE = "Rust client"
    const val RUST_CLIENT_INDEX_URL = RUST_URL
    const val RUST_CLIENT_INDEX_URL_SLASH = RUST_INDEX_URL_SLASH
    const val RUST_CLIENT_INDEX_TITLE = RUST_INDEX_TITLE
    const val GO_INDEX_URL = GO_URL
    const val GO_INDEX_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-clients/go-client/"
    const val GO_INDEX_TITLE = "Go client"
    const val GO_CLIENT_INDEX_URL = GO_URL
    const val GO_CLIENT_INDEX_URL_SLASH = GO_INDEX_URL_SLASH
    const val GO_CLIENT_INDEX_TITLE = GO_INDEX_TITLE
    const val GET_STARTED_DAPP_INDEX_URL = "https://docs.chromia.com/get-started/about/dapp"
    const val GET_STARTED_DAPP_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/dapp/"
    const val GET_STARTED_DAPP_INDEX_TITLE = "Build dApps (Rell & Clients)"
    const val GET_STARTED_CHROMIA_VS_EVM_INDEX_URL = "https://docs.chromia.com/get-started/about/chromia-vs-evm"
    const val GET_STARTED_CHROMIA_VS_EVM_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/chromia-vs-evm/"
    const val GET_STARTED_CHROMIA_VS_EVM_INDEX_TITLE = "Chromia vs EVM"
    const val GET_STARTED_GTX_INDEX_URL = "https://docs.chromia.com/get-started/about/protocols/gtx"
    const val GET_STARTED_GTX_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/protocols/gtx/"
    const val GET_STARTED_GTX_INDEX_TITLE = "Generic Transaction Protocol (GTX)"
    const val GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_URL = "https://docs.chromia.com/get-started/about/architecture/chains/system-anchoring-chain"
    const val GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/architecture/chains/system-anchoring-chain/"
    const val GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_TITLE = "System Anchoring Chain"
    const val GET_STARTED_USER_DELEGATION_INDEX_URL = "https://docs.chromia.com/get-started/about/staking/user-delegation"
    const val GET_STARTED_USER_DELEGATION_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/staking/user-delegation/"
    const val GET_STARTED_USER_DELEGATION_INDEX_TITLE = "User staking and delegation"
    const val GET_STARTED_ICCF_PROTOCOL_INDEX_URL = "https://docs.chromia.com/get-started/about/protocols/iccf"
    const val GET_STARTED_ICCF_PROTOCOL_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/protocols/iccf/"
    const val GET_STARTED_ICCF_PROTOCOL_INDEX_TITLE = "Inter-chain Confirmation Facility (ICCF)"
    const val ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/user-proposal-flows"
    const val ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/user-proposal-flows/"
    const val ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_TITLE = "User and proposal flows"
    const val ECOSYSTEM_GOV_VOTING_FLOW_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/governance-voting-process/voting-flow"
    const val ECOSYSTEM_GOV_VOTING_FLOW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/governance-voting-process/voting-flow/"
    const val ECOSYSTEM_GOV_VOTING_FLOW_INDEX_TITLE = "Voting flow"
    const val FILEHUB_ECO_URL = "https://docs.chromia.com/ecosystem/filehub/overview/"
    const val BRIDGE_ECO_URL = "https://docs.chromia.com/ecosystem/bridge/overview/"
    const val ICCF_PROTOCOL_URL = "https://docs.chromia.com/get-started/about/protocols/iccf/"
    const val FILEHUB_CONFIGURE_URL = "https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-configure/"
    const val MCP_SERVER_URL = "https://docs.chromia.com/build/clients/mcp-server/"
    const val MCP_SERVER_INDEX_URL = "https://docs.chromia.com/build/clients/mcp-server"
    const val MCP_SERVER_INDEX_URL_SLASH = MCP_SERVER_URL
    const val MCP_SERVER_INDEX_TITLE = "Chromia MCP Server"
    const val FILEHUB_WORK_URL = "https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-work/"
    const val FILEHUB_WORK_BUILD_404 = "https://docs.chromia.com/build/clients/filehub-client/work"
    const val FILEHUB_BUILD_INDEX_404 = "https://docs.chromia.com/build/filehub/"
    const val BRIDGE_CONFIGURE_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-client/client/"
    const val ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-client/client"
    const val ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_URL_SLASH = BRIDGE_CONFIGURE_URL
    const val ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_TITLE = "Configure the Chromia bridge client"  // official H1
    const val LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-review-entity/write-queries"
    const val LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-review-entity/write-queries/"
    const val LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_TITLE = "Write a query to retrieve all reviews of a book"
    const val LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/account-management"
    const val LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/account-management/"
    const val LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_TITLE = "Account management"
    const val LEARN_NEWS_INTRODUCTION_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/introduction"
    const val LEARN_NEWS_INTRODUCTION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/introduction/"
    const val LEARN_NEWS_INTRODUCTION_INDEX_TITLE = "A simple app on Chromia is created using Rell, React, and FT4"  // official H1
    const val LEARN_TAGS_REACT_INDEX_URL = "https://learn.chromia.com/tags/React"
    const val LEARN_TAGS_REACT_INDEX_URL_SLASH = "https://learn.chromia.com/tags/React/"
    const val LEARN_TAGS_REACT_INDEX_TITLE = "Courses tagged with: React"  // official H1
    const val BRIDGE_WORK_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-client/work-with-client/"
    const val FILEHUB_NPM = "filehub"
    const val BRIDGE_NPM = "@chromia/bridge-client"
    const val LSP_MCP_NPM = "@chromia/chromia-lsp-mcp"
    const val LSP_MCP_RELL = "0.8.8"
    const val MCP_OFFICIAL_PAGE_KEY = "chromia-mcp"
    const val MCP_FAT_JAR_NAME = "chromia-mcp-server"
    const val MCP_PROD_URL = "https://mcp.chromia.dev"
    const val MCP_LOCAL_SSE = "http://127.0.0.1:3001"
    const val MCP_LOCAL_GRADLE = "./gradlew :app:runSse"
    const val MCP_REPO = "https://gitlab.com/chromaway/core-tools/chromia-mcp.git"
    const val MCP_LEFTOVER_OFFICIAL_PROD_SSE = "https://mcp.chromia.dev/sse"
    const val MCP_LEFTOVER_OFFICIAL_LOCAL_SSE = "http://127.0.0.1:3001/sse"
    const val MCP_LIVE_SSE_PATH = "/"
    const val MCP_LEFTOVER_OFFICIAL_SSE_PATH = "/sse"
    const val MCP_HEALTH_PATH = "/health"
    const val MCP_HEALTH_STATUS = "healthy"
    const val MCP_HEALTH_VERSION = "0.2.2"
    const val MCP_LEFTOVER_OFFICIAL_HOST_STATUS = "502"
    const val FILEHUB_GATEWAY_HOST = "filehub-gw.chromia.com"
    const val GO_MODULE = "gitlab.com/chromaway/ft4-go-client"
    const val GO_LEFTOVER_OFFICIAL_NODE = "https://node1.example.com"
    const val RUST_CRATE = "postchain-client"
    const val RUST_CRATE_VERSION = "0.0.3"
    const val RUST_LEFTOVER_OFFICIAL_DECIMAL_TYPO = "BigDecima"
    const val RUST_SOURCE_DECIMAL = "BigDecimal"
    const val RUST_LEFTOVER_OFFICIAL_ERROR_ARM = "Error(error: RestError)"
    const val RUST_SOURCE_ERR_ARM = "Err(error: RestError)"
    const val RUST_LEFTOVER_OFFICIAL_ERR_INCOMPLETE = "Err(error: )"
    const val RUST_LEFTOVER_OFFICIAL_ERR_IDENT = "err"
    const val RUST_SOURCE_ERR_IDENT = "error"
    const val RUST_LEFTOVER_OFFICIAL_QUERY_ARGS_REF = "query_arguments_ref"
    const val RUST_SOURCE_QUERY_ARGS = "query_arguments"
    const val RUST_LEFTOVER_OFFICIAL_BYTEARRAY = "Params:: ByteArray"
    const val RUST_SOURCE_BYTEARRAY = "Params::ByteArray"
    const val RUST_LEFTOVER_OFFICIAL_DECIMAL_SERDE = "serialize_bigint"
    const val RUST_SOURCE_DECIMAL_SERDE = "serialize_bigdecimal"
    const val RUST_LEFTOVER_OFFICIAL_RESTCLIENT_LIFETIME = "RestClient<'_>"
    const val RUST_LEFTOVER_OFFICIAL_RESTCLIENT_HTML_ENTITY = "RestClient&lt;'_>"
    const val REST_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-rest-api/"
    const val REST_TITLE = "Postchain Rest API"
    const val POSTCHAIN_REST_API_INDEX_URL = REST_URL
    const val POSTCHAIN_REST_API_INDEX_URL_SLASH = REST_URL_SLASH
    const val POSTCHAIN_REST_API_INDEX_TITLE = REST_TITLE
    const val REACT_NPM = "@chromia/react"
    const val REACT_LEFTOVER_OFFICIAL_POOL = "BLOCKCHAIN_URL"

    fun csharpQuery(): String = """
        var client = await ChromiaClient.Create("http://localhost:7740", blockchainRID);
        var response = client.Query<string>("get_city", ("zip", 22222));
    """.trimIndent() + "\n"

    fun csharpCreateFromIid(): String =
        """var client = await ChromiaClient.Create("http://localhost:7740", 0);""" + "\n"

    fun leftoverOfficialCsharpCreateFromDirectory(): String =
        """var client = await ChromiaClient.CreateFromDirectory("http://localhost:7750", blockchainRID);""" + "\n"

    fun leftoverOfficialPostchainClientsIntro(): String = """
        Postchain clients offer libraries for interacting with a blockchain using JavaScript/TypeScript, Kotlin, C#, Rust, or Python. These libraries enable easy transaction sending and data retrieval from Rell blockchain nodes, simplifying the development of decentralized applications.

        Additionally, Chromia exposes REST endpoints directly in the browser, enabling developers to test functionality, query the blockchain, and submit transactions.
    """.trimIndent() + "\n"

    fun leftoverOfficialReactKitIntro(): String = """
        The Chromia React Kit streamlines the integration of React-based front-end applications with the Chromia blockchain. It offers a suite of tools and react custom hooks that enable developers to build decentralized applications effortlessly (dapps), simplifying and managing blockchain interactions.
    """.trimIndent() + "\n"

    fun leftoverOfficialFilehubClientIntro(): String = """
        TypeScript. Persisting and reading files on the Chromia blockchain. Filehub utilises @chromia/ft4 and postchain-client as peer dependency.
    """.trimIndent() + "\n"

    fun leftoverOfficialBridgeClientIntro(): String = """
        TypeScript library with utilities for interacting with the Chromia token bridge
    """.trimIndent() + "\n"

    fun leftoverOfficialOverviewIntro(): String = """
        The Clients section highlights tools and libraries that enable integration of applications with the Chromia blockchain.
    """.trimIndent() + "\n"

    fun leftoverOfficialMcpServerIntro(): String = """
        A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps through the Chromia Explorer.
    """.trimIndent() + "\n"

    fun leftoverOfficialJsTsIndexIntro(): String = """
        The JavaScript/TypeScript client library provides a comprehensive set of functions and utilities for interacting with Chromia blockchain. It allows you to send transactions, retrieve information from blockchain nodes running Rell, and build decentralized applications with ease.
    """.trimIndent() + "\n"

    fun leftoverOfficialPythonIndexIntro(): String = """
        A Python client library for interacting with Postchain nodes on Chromia blockchain networks. This library provides an interface for creating, signing, and sending transactions, as well as querying the blockchain, with full async support.
    """.trimIndent() + "\n"

    fun leftoverOfficialKotlinIndexIntro(): String = """
        The Kotlin client library, postchain-client, provides the capability for interacting with a blockchain from a client app written in Kotlin or Java. With this library, you can easily send transactions and retrieve queries from a Rell blockchain node.
    """.trimIndent() + "\n"

    fun leftoverOfficialCsharpIndexIntro(): String = """
        The C# client provides functionality for interacting with a blockchain using C#. It allows you to send transactions and retrieve information from a blockchain node running Rell. It can be integrated into your C# projects through the NuGet package or directly referencing the DLL files. Additionally, specific instructions apply if you're working with Unity or Unity WebGL.
    """.trimIndent() + "\n"

    fun leftoverOfficialRustIndexIntro(): String = """
        The Rust client is used for interacting with the Chromia blockchain deployed to a Postchain single node (manual mode) or multi-nodes managed by the Directory Chain (managed mode).

        This library provides functionality for executing queries, creating and signing transactions, and managing blockchain operations.
    """.trimIndent() + "\n"

    fun leftoverOfficialFt4ClientIndexIntro(): String = """
        The FT4 client is written in TypeScript. It is made for easier access to the backend operations and queries and ready to be used on the frontend (or a js backend).
    """.trimIndent() + "\n"

    fun leftoverOfficialGoIndexIntro(): String = """
        The Go client offers a range of built-in functions and utilities designed to simplify interaction with decentralized applications (dApps) created using the Postchain blockchain framework, commonly referred to as Chromia.

        The Go client library provides tools for interacting with the Chromia blockchain platform. It includes functionality for:

        - Serializing and deserializing data using the GTV (Generic Transfer Value) format.
        - Creating, signing, and submitting transactions using the GTX (Generic Transaction) format.
        - Computing Merkle tree hashes for data verification.
        - Communicating with Postchain nodes via REST API.
    """.trimIndent() + "\n"

    fun leftoverOfficialPostchainRestApiIndexIntro(): String = """
        The Postchain Rest API, enables users to perform HTTP-based interactions with the blockchain to access vital information. This includes querying transaction records, inspecting block data, reviewing the blockchain’s current state, fetching configuration settings, and other related details.
    """.trimIndent() + "\n"

    fun leftoverOfficialCsharpDirectoryCreate(): String =
        """var client3 = await ChromiaClient.Create(new() {"http://localhost:7750", "http://localhost:7751"}, 0);""" + "\n"

    fun leftoverOfficialCsharpQueryParams(): String = """
        struct QueryParams : IGtvSerializable
        {
            [JsonProperty("zip")]
            public int Zip;
        }
        var response = client.Query<string>("get_city", new QueryParams(){ Zip = 22222 });
    """.trimIndent() + "\n"

    const val CSHARP_DIRECTORY_HOST = "http://localhost:7750"

    fun goQuery(): String = """
        nodeURL, err := url.Parse("http://localhost:7740")
        client := postchain.NewClient([]*url.URL{nodeURL})
        result, err := client.Query(blockchainRID, "get_account_balance", gtv.DictValue{Value: map[string]gtv.Value{}})
    """.trimIndent() + "\n"

    fun rustClient(): String = """
        use postchain_client::transport::client::RestClient;
        let client = RestClient {
            node_url: vec!["http://localhost:7740", "http://localhost:7741"],
            request_time_out: 30,
            poll_attemps: 5,
            poll_attemp_interval_time: 5
        };
    """.trimIndent() + "\n"

    fun rustQuery(): String = """
        let result = client.query(
            "<BLOCKCHAIN_RID>",
            None,
            "<query_name>",
            None,
            Some(&mut query_arguments)
        ).await?;
    """.trimIndent() + "\n"

    fun leftoverOfficialRustStatus(): String =
        """let status = client.get_transaction_status("<blockchain RID>", &tx_rid).await?;""" + "\n"

    fun leftoverOfficialRustQueryError(): String = """
        let result = client.query(/* ... */).await;
        match result {
            Ok(resp: RestResponse) => {
                if let RestResponse::Bytes(val1) = resp {
                    let params = gtv::decode(&val1);
                }
            },
            Error(error: RestError) => {
            }
        }
    """.trimIndent() + "\n"

    fun reactHooks(): String = """
        import { createChromiaHooks } from "@chromia/react";
        import type { ClientConfig } from "@chromia/react";
        import { FailoverStrategy } from "postchain-client";

        const clientConfig: ClientConfig = {
          directoryNodeUrlPool: ["http://localhost:7740"],
          failOverConfig: {
            strategy: FailoverStrategy.AbortOnError,
          },
        };

        export const { useChromiaInfiniteQuery, useChromiaImmutableQuery, useChromiaQuery } =
          createChromiaHooks({
            clientConfig,
          });
    """.trimIndent() + "\n"

    fun filehubGetFile(): String = """
        const { Filehub } = require("filehub");
        const filehub = new Filehub({
          directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL,
          blockchainRid: FILEHUB_BLOCKCHAIN_RID,
        });
        const file = await filehub.getFile(fileHash);
    """.trimIndent() + "\n"

    fun filehubConstruct(): String = """
        const filehub = new Filehub({
          directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL,
          blockchainRid: FILEHUB_BLOCKCHAIN_RID,
        });
    """.trimIndent() + "\n"

    fun mcpProdConfig(): String = """
        {
          "mcpServers": {
            "chromia-mcp": {
              "url": "https://mcp.chromia.dev"
            }
          }
        }
    """.trimIndent() + "\n"

    fun mcpLocalConfig(): String = """
        {
          "mcpServers": {
            "chromia-mcp": {
              "url": "http://127.0.0.1:3001"
            }
          }
        }
    """.trimIndent() + "\n"

    fun mcpLspConfig(): String = """
        {
          "mcpServers": {
            "lsp-mcp": {
              "command": "npx",
              "args": ["chromia-lsp-mcp", "0.8.8"]
            }
          }
        }
    """.trimIndent() + "\n"

    fun bridgeCheckAllowance(): String =
        "const allowance: bigint = await bcl.checkAllowance();\n"

    fun bridgeClientInit(): String = """
        const provider = new BrowserProvider(window.ethereum);
        const bcl = await bridgeClient({ bridgeAddress: "YOUR_BRIDGE_ADDRESS", tokenAddress: "YOUR_TOKEN_ADDRESS" }, provider);
    """.trimIndent() + "\n"

    val restGetPaths = listOf(
        "GET /query/{blockchainRid}",
        "GET /query_gtv/{blockchainRid}",
        "POST /query_gtv/{blockchainRid}",
        "GET /web_query/{blockchainRid}/{type}",
        "GET /brid/iid_{chainIid}",
        "GET /version",
        "GET /version/{blockchainRid}",
        "GET /blocks/{blockchainRid}",
        "GET /blocks/{blockchainRid}/{blockRid}",
        "GET /blocks/{blockchainRid}/height/{height}",
        "GET /blocks/{blockchainRid}/confirm/{blockRid}",
        "GET /blockchain/{blockchainRid}/height",
        "GET /blockchain/{blockchainRid}/nodestate",
        "GET /config/{blockchainRid}",
        "GET /config/{blockchainRid}/next_height",
        "GET /config/{blockchainRid}/features",
        "GET /errors/{blockchainRid}",
        "GET /node/{blockchainRid}/my_status",
        "GET /node/{blockchainRid}/statuses",
        "GET /tx/{blockchainRid}/waiting",
        "GET /tx/{blockchainRid}/waiting/{txRid}",
        "GET /tx/{blockchainRid}/rejected",
        "GET /tx/{blockchainRid}/{txRid}",
        "GET /tx/{blockchainRid}/{txRid}/status",
        "GET /tx/{blockchainRid}/{txRid}/confirmationProof",
        "GET /transactions/{blockchainRid}",
        "GET /transactions/{blockchainRid}/{txRid}",
        "GET /transactions/{blockchainRid}/count",
        "GET /infrastructure_version",
        "GET /infrastructure_version/{blockchainRid}",
        "GET /highest_block_height_anchoring_check/{blockchainRid}"
    )

    val skipped = listOf(
        "POST /tx/{blockchainRid}  # official Submit transaction",
        "POST /config/{blockchainRid}  # official Validate blockchain configuration",
        "C# SignatureProvider / Transaction.Build / SendTransaction / SendUniqueTransaction",
        "Go gtx.NewTransaction / tx.Sign / PostTransaction / AwaitConfirmation",
        "Rust Transaction::new / sign / send_transaction (printed sample keys on the official page are skipped)",
        "React FtProvider / createWeb3ProviderEvmKeyStore / useFileHubImage",
        "C# NuGet package id (official page does not print one)",
        "JS/TS hello-world DEV key pair and signAndSendUniqueTransaction",
        "leftover official JS/TS reference dummy Buffer.alloc keys / secp256k1 / newSignatureProvider / sample 64-hex / signAndSend",
        "leftover official BUILD Filehub / bridge client pages print no package id (leftover official leftover pages do)",
        "FilehubAdministrator / registerFilechain / enable-disable Filechain / payments writes",
        "Filehub storeFile write",
        "leftover official MCP explorer-dump sample BRIDs",
        "bridge deposit / withdraw / mass-exit / allowToken / setBlockchainRid / registerAccount writes",
        "iccf_proof write operations"
    )

    fun notes(): String = """
        Chromia CLI $CLI_SERIES official language-client / REST query-only help. Java 21+, Postgres 16+.
        Clients overview: $OVERVIEW_URL
        Leftover official BUILD clients overview (leftover official $OVERVIEW_URL leftover official 307 leftover official $OVERVIEW_URL_SLASH leftover official 200 leftover official $OVERVIEW_TITLE): leftover official leftover intro leftover official leftover The Clients section highlights tools and libraries leftover official leftover lists leftover official leftover Postchain clients leftover official leftover JavaScript(JS)/TypeScript(TS), Kotlin, C#, Rust, Python, Go leftover official leftover other leftover official leftover tools leftover official leftover Chromia React Kit, Bridge client, FT4 client, Filehub client leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients overview (leftover official $OVERVIEW_INDEX_URL leftover official 307 leftover official $OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official $OVERVIEW_INDEX_TITLE): leftover official leftover intro leftover official leftover The Clients section highlights tools and libraries leftover official leftover Available leftover official leftover Postchain leftover official leftover clients leftover official leftover JavaScript(JS)/TypeScript(TS), Kotlin, C#, Rust, Python, Go leftover official leftover Other leftover official leftover tools leftover official leftover child leftover official leftover cards leftover official leftover $OVERVIEW_INDEX_CARD_REST leftover official leftover $OVERVIEW_INDEX_CARD_REACT leftover official leftover $OVERVIEW_INDEX_CARD_POSTCHAIN leftover official leftover $OVERVIEW_INDEX_CARD_BRIDGE leftover official leftover $OVERVIEW_INDEX_CARD_FT4 leftover official leftover $OVERVIEW_INDEX_CARD_FILEHUB leftover official leftover $OVERVIEW_INDEX_CARD_MCP leftover official leftover Work leftover official leftover in leftover official leftover progress leftover official leftover Additional leftover official leftover resources leftover official leftover FT4 leftover official leftover documentation leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients postchain-clients (leftover official $POSTCHAIN_CLIENTS_URL leftover official 307 leftover official $POSTCHAIN_CLIENTS_URL_SLASH leftover official 200 leftover official $POSTCHAIN_CLIENTS_TITLE): leftover official leftover intro leftover official $POSTCHAIN_CLIENTS_INTRO_LANGS leftover official leftover omits leftover official leftover Go leftover official leftover child leftover official leftover cards leftover official leftover $POSTCHAIN_CLIENTS_CARD_JS leftover official leftover $POSTCHAIN_CLIENTS_CARD_JS_URL leftover official leftover $POSTCHAIN_CLIENTS_CARD_KOTLIN leftover official leftover $POSTCHAIN_CLIENTS_CARD_CSHARP leftover official leftover $POSTCHAIN_CLIENTS_CARD_RUST leftover official leftover $POSTCHAIN_CLIENTS_CARD_PYTHON leftover official leftover $POSTCHAIN_CLIENTS_CARD_GO leftover official leftover REST leftover official leftover endpoints leftover official leftover browser leftover official leftover query leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients postchain-clients (leftover official $POSTCHAIN_CLIENTS_INDEX_URL leftover official 307 leftover official $POSTCHAIN_CLIENTS_INDEX_URL_SLASH leftover official 200 leftover official $POSTCHAIN_CLIENTS_INDEX_TITLE): leftover official leftover intro leftover official leftover Postchain clients offer libraries leftover official leftover $POSTCHAIN_CLIENTS_INTRO_LANGS leftover official leftover omits leftover official leftover Go leftover official leftover child leftover official leftover cards leftover official leftover $POSTCHAIN_CLIENTS_CARD_JS leftover official leftover $POSTCHAIN_CLIENTS_CARD_JS_URL leftover official leftover $POSTCHAIN_CLIENTS_CARD_KOTLIN leftover official leftover $POSTCHAIN_CLIENTS_CARD_CSHARP leftover official leftover $POSTCHAIN_CLIENTS_CARD_RUST leftover official leftover $POSTCHAIN_CLIENTS_CARD_PYTHON leftover official leftover $POSTCHAIN_CLIENTS_CARD_GO leftover official leftover REST leftover official leftover endpoints leftover official leftover browser leftover official leftover query leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD deploy-frontend-dapp (200): $DEPLOY_FRONTEND_URL leftover official on-chain frontend key webStatic and leftover official local URL $WEB_STATIC_LOCAL_URL. Do not invent siblings. See chr_deploy_help.
        JS/TS, Kotlin, Python, and FT4 local createConnection live on chr_generate_client_help.
        Leftover official BUILD kotlin-client (200): ${ChrGenerateClientHelp.KOTLIN_CLIENT_URL} leftover official EndpointPool.singleUrl leftover official ${ChrGenerateClientHelp.KOTLIN_LOCAL_ENDPOINT} leftover official query leftover official hello_world leftover official StandardChromiaClient leftover official without leftover official sample leftover official hex leftover official ${ChrGenerateClientHelp.OUTDATED_BANNER}. REAL leftover official bug: leftover official Chromia client leftover official Gradle leftover official still leftover official prints leftover official ${ChrGenerateClientHelp.MAVEN_POSTCHAIN} leftover official Maven leftover official ${ChrGenerateClientHelp.MAVEN_CHROMIA} leftover official wins. Skip leftover official printed leftover official sample leftover official keys leftover official set_name leftover official awaitAnchoredTx leftover official sample leftover official 64-hex.
        Leftover official BUILD python-client (200): ${ChrGenerateClientHelp.PYTHON_CLIENT_URL} leftover official Python ${ChrGenerateClientHelp.PYTHON_MIN} leftover official pip leftover official ${ChrGenerateClientHelp.PIP_POSTCHAIN} leftover official query leftover official get_collections leftover official get_all_books leftover official get_all_reviews_for_book leftover official POSTCHAIN_TEST_NODE leftover official BLOCKCHAIN_TEST_RID leftover official YOUR_BLOCKCHAIN_RID leftover official BlockchainRID leftover official placeholder leftover official wins. Skip leftover official PRIV_KEY leftover official coincurve leftover official sign_transaction leftover official send_transaction.
        Leftover official BUILD clients postchain-clients javascript-typescript hello-world-quickstart (leftover official $JS_QUICKSTART_INDEX_URL leftover official 307 leftover official $JS_QUICKSTART_INDEX_URL_SLASH leftover official 200 leftover official $JS_QUICKSTART_INDEX_TITLE): leftover official leftover intro leftover official leftover This quickstart guide leftover official leftover Node.js 18+ leftover official leftover object my_name leftover official leftover query hello_world() leftover official leftover Hello %s!.format(my_name.name) leftover official leftover client.query("hello_world") leftover official leftover default leftover official leftover Hello World! leftover official leftover run-dapp-cli leftover official leftover $RUN_DAPP_CLI_URL leftover official leftover leftover official leftover chromia. yml leftover official leftover space leftover official leftover leftover official leftover chromia.yml leftover official leftover wins leftover official leftover REAL leftover official leftover bug leftover official leftover page leftover official leftover still leftover official leftover prints leftover official leftover DEV-ONLY leftover official leftover sample leftover official leftover keys leftover official leftover and leftover official leftover signAndSendUniqueTransaction leftover official leftover set_name leftover official leftover leftover official leftover query-only leftover official leftover wins leftover official leftover Query-only leftover official leftover skip leftover official leftover printed leftover official leftover sample leftover official leftover keys leftover official leftover set_name leftover official leftover tx leftover official leftover keygen leftover official leftover signAndSendUniqueTransaction leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD JS/TS reference (200): $JS_REFERENCE_URL leftover official Directory Chain leftover official Iid ${ChrGenerateClientHelp.DIRECTORY_CHAIN_IID} leftover official succefull leftover official failover leftover official defaults leftover official ${ChrGenerateClientHelp.FAILOVER_DEFAULT_STRATEGY} leftover official attemptsPerEndpoint ${ChrGenerateClientHelp.FAILOVER_DEFAULT_ATTEMPTS} leftover official attemptInterval ${ChrGenerateClientHelp.FAILOVER_DEFAULT_INTERVAL_MS} leftover official unreachableDuration ${ChrGenerateClientHelp.FAILOVER_DEFAULT_UNREACHABLE_MS} leftover official sticky leftover official useStickyNode leftover official prose leftover official ${ChrGenerateClientHelp.LEFTOVER_OFFICIAL_PROSE_BRID} leftover official source leftover official ${ChrGenerateClientHelp.SOURCE_BRID_SETTING} leftover official wins. REAL leftover official bug: leftover official ${ChrGenerateClientHelp.LEFTOVER_OFFICIAL_STRATEGY_TYPO} leftover official ${ChrGenerateClientHelp.LEFTOVER_OFFICIAL_ABORT_TYPO} leftover official three leftover official failover leftover official strategies leftover official lists leftover official four leftover official source leftover official ${ChrGenerateClientHelp.SOURCE_FAILOVER_ABORT} leftover official wins leftover official BlockchainRID leftover official placeholder leftover official wins. Skip leftover official dummy leftover official Buffer.alloc leftover official secp256k1 leftover official newSignatureProvider leftover official sample leftover official 64-hex leftover official signAndSend.
        Leftover official BUILD clients postchain-clients javascript-typescript reference (leftover official $JS_REFERENCE_INDEX_URL leftover official 307 leftover official $JS_REFERENCE_INDEX_URL_SLASH leftover official 200 leftover official $JS_REFERENCE_INDEX_TITLE): leftover official leftover Directory Chain leftover official leftover Iid leftover official leftover ${ChrGenerateClientHelp.DIRECTORY_CHAIN_IID} leftover official leftover succefull leftover official leftover failover leftover official leftover defaults leftover official leftover ${ChrGenerateClientHelp.FAILOVER_DEFAULT_STRATEGY} leftover official leftover attemptsPerEndpoint leftover official leftover ${ChrGenerateClientHelp.FAILOVER_DEFAULT_ATTEMPTS} leftover official leftover attemptInterval leftover official leftover ${ChrGenerateClientHelp.FAILOVER_DEFAULT_INTERVAL_MS} leftover official leftover unreachableDuration leftover official leftover ${ChrGenerateClientHelp.FAILOVER_DEFAULT_UNREACHABLE_MS} leftover official leftover sticky leftover official leftover useStickyNode leftover official leftover prose leftover official leftover ${ChrGenerateClientHelp.LEFTOVER_OFFICIAL_PROSE_BRID} leftover official leftover source leftover official leftover ${ChrGenerateClientHelp.SOURCE_BRID_SETTING} leftover official leftover wins leftover official leftover REAL leftover official leftover bug leftover official leftover ${ChrGenerateClientHelp.LEFTOVER_OFFICIAL_STRATEGY_TYPO} leftover official leftover ${ChrGenerateClientHelp.LEFTOVER_OFFICIAL_ABORT_TYPO} leftover official leftover three leftover official leftover failover leftover official leftover strategies leftover official leftover lists leftover official leftover four leftover official leftover source leftover official leftover ${ChrGenerateClientHelp.SOURCE_FAILOVER_ABORT} leftover official leftover wins leftover official leftover BlockchainRID leftover official leftover placeholder leftover official leftover wins leftover official leftover Query-only leftover official leftover skip leftover official leftover dummy leftover official leftover Buffer.alloc leftover official leftover secp256k1 leftover official leftover newSignatureProvider leftover official leftover signAndSend leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        C# ($CSHARP_URL): ChromiaClient.Create(url, blockchainRID) or Create(url, iid). Query:
        client.Query<string>("get_city", ("zip", 22222)) leftover official IGtvSerializable leftover official JsonProperty leftover official QueryParams leftover official Zip leftover official 22222.
        Official page says NuGet package or DLL / Unity Plugins; it does not print a package id.
        Directory discovery: ChromiaClient.CreateFromDirectory(systemUrl, rid|iid). Official local
        Directory example host is http://localhost:7750 leftover official $CSHARP_DIRECTORY_HOST leftover official ${ChrGenerateClientHelp.OUTDATED_BANNER}. REAL leftover official bug: leftover official directory leftover official pool leftover official still leftover official prints leftover official Create leftover official not leftover official CreateFromDirectory leftover official BlockchainRID leftover official placeholder leftover official wins. Skip leftover official sample leftover official 64-hex leftover official SignatureProvider leftover official SendTransaction leftover official SendUniqueTransaction.
        Go ($GO_URL): go get $GO_MODULE leftover official NewClient leftover official $GO_LEFTOVER_OFFICIAL_NODE leftover official http://localhost:7740 leftover official wins leftover official GetBlockchainRID leftover official 123. postchain.NewClient([]*url.URL{nodeURL}).
        Query: client.Query(blockchainRID, name, gtv.DictValue). Also GetBlockchainRID(iid),
        GetFeatures, DetectMerkleHashVersion (use merkle HashVersion2; version 1 has a hash-collision bug).
        Read-only retrieve: GetTransaction, GetTransactionInfo, GetConfirmationProof, BlockAtHeight, BlockByRID.
        Rust ($RUST_URL): crate $RUST_CRATE = "$RUST_CRATE_VERSION". RestClient { node_url, request_time_out: 30,
        poll_attemps: 5, poll_attemp_interval_time: 5 }. client.query(brid, None, query_type, None, args).
        Leftover official rust page prints leftover official $RUST_LEFTOVER_OFFICIAL_RESTCLIENT_LIFETIME (HTML entity leftover official $RUST_LEFTOVER_OFFICIAL_RESTCLIENT_HTML_ENTITY).
        Official field names poll_attemps / poll_attemp_interval_time are as printed leftover official get_transaction_status leftover official read-only. REAL leftover official bug: leftover official Params leftover official table leftover official still leftover official prints leftover official $RUST_LEFTOVER_OFFICIAL_DECIMAL_TYPO leftover official $RUST_SOURCE_DECIMAL leftover official wins leftover official query leftover official match leftover official still leftover official prints leftover official $RUST_LEFTOVER_OFFICIAL_ERROR_ARM leftover official $RUST_SOURCE_ERR_ARM leftover official wins leftover official $RUST_LEFTOVER_OFFICIAL_ERR_INCOMPLETE leftover official $RUST_LEFTOVER_OFFICIAL_ERR_IDENT leftover official $RUST_SOURCE_ERR_IDENT leftover official wins leftover official $RUST_LEFTOVER_OFFICIAL_QUERY_ARGS_REF leftover official $RUST_SOURCE_QUERY_ARGS leftover official wins leftover official $RUST_LEFTOVER_OFFICIAL_BYTEARRAY leftover official $RUST_SOURCE_BYTEARRAY leftover official wins leftover official decimal leftover official serde leftover official still leftover official prints leftover official $RUST_LEFTOVER_OFFICIAL_DECIMAL_SERDE leftover official $RUST_SOURCE_DECIMAL_SERDE leftover official wins. Skip leftover official printed leftover official sample leftover official keys leftover official Transaction::new leftover official sign leftover official send_transaction.
        React Kit ($REACT_URL): $REACT_NPM leftover official ${ChrGenerateClientHelp.OUTDATED_BANNER} leftover official $REACT_LEFTOVER_OFFICIAL_POOL leftover official http://localhost:7740 leftover official wins leftover official SWR leftover official QueryOrOperationType. createChromiaHooks returns useChromiaQuery,
        useChromiaImmutableQuery, useChromiaInfiniteQuery. Official FailoverStrategy.AbortOnError.
        useFtQuery({ queryName, queryParams, accountId }) is a read hook; skip FtProvider EVM keystore.
        Leftover official BUILD clients react-kit (leftover official $REACT_URL leftover official 307 leftover official $REACT_URL_SLASH leftover official 200 leftover official $REACT_TITLE): leftover official leftover intro leftover official leftover The Chromia React Kit streamlines leftover official leftover $REACT_NPM leftover official leftover SWR leftover official leftover createChromiaHooks leftover official leftover useChromiaQuery leftover official leftover useChromiaImmutableQuery leftover official leftover useChromiaInfiniteQuery leftover official leftover useFtQuery leftover official leftover skip leftover official leftover FtProvider leftover official leftover EVM leftover official leftover keystore leftover official leftover useFileHubImage leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients react-kit (leftover official $REACT_KIT_INDEX_URL leftover official 307 leftover official $REACT_KIT_INDEX_URL_SLASH leftover official 200 leftover official $REACT_KIT_INDEX_TITLE): leftover official leftover intro leftover official leftover The Chromia React Kit streamlines leftover official leftover Why use leftover official leftover Getting started leftover official leftover Install leftover official leftover $REACT_NPM leftover official leftover Core leftover official leftover features leftover official leftover Custom leftover official leftover hooks leftover official leftover createChromiaHooks leftover official leftover useChromiaQuery leftover official leftover useChromiaImmutableQuery leftover official leftover useChromiaInfiniteQuery leftover official leftover useFtQuery leftover official leftover SWR leftover official leftover Additional leftover official leftover resources leftover official leftover FT4 leftover official leftover documentation leftover official leftover ${ChrGenerateClientHelp.OUTDATED_BANNER} leftover official leftover Query-only leftover official leftover skip leftover official leftover FtProvider leftover official leftover EVM leftover official leftover keystore leftover official leftover useFileHubImage leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        REST ($REST_URL leftover official 307 leftover official $REST_URL_SLASH leftover official 200 leftover official $REST_TITLE): no install. Official groups: Block, Blockchain, Configuration, Error,
        Experimental, Node, Query, Transaction. Paths below are the official table path templates.
        Cookbook create-rell-dapp GET: curl -X GET 'localhost:7740/query/<BlockchainRID>?type=hello_world'
        ($COOKBOOK_CREATE_RELL_URL). Default REST port 7740. Do not invent a BRID.
        Leftover official BUILD clients postchain-rest-api (leftover official $POSTCHAIN_REST_API_INDEX_URL leftover official 307 leftover official $POSTCHAIN_REST_API_INDEX_URL_SLASH leftover official 200 leftover official $POSTCHAIN_REST_API_INDEX_TITLE): leftover official leftover intro leftover official leftover The Postchain Rest API, enables users leftover official leftover Postchain leftover official leftover API leftover official leftover usage leftover official leftover no leftover official leftover install leftover official leftover browser leftover official leftover groups leftover official leftover Block leftover official leftover Blockchain leftover official leftover Configuration leftover official leftover Error leftover official leftover Experimental leftover official leftover Node leftover official leftover Query leftover official leftover Transaction leftover official leftover ${ChrGenerateClientHelp.OUTDATED_BANNER} leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients ft4-client (leftover official $FT4_CLIENT_URL leftover official 307 leftover official $FT4_CLIENT_URL_SLASH leftover official 200 leftover official $FT4_CLIENT_TITLE): leftover official TypeScript leftover official $FT4_CLIENT_NPM leftover official backend leftover official operations leftover official and leftover official queries leftover official frontend leftover official or leftover official js leftover official backend leftover official $FT4_CLIENT_INTRO_URL leftover official FT4 leftover official documentation leftover official query-only leftover official skip leftover official signed leftover official txs leftover official no leftover official sample leftover official keys leftover official no leftover official invented leftover official 64-hex leftover official <BlockchainRID>.
        Leftover official BUILD clients ft4-client (leftover official $FT4_CLIENT_INDEX_URL leftover official 307 leftover official $FT4_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official $FT4_CLIENT_INDEX_TITLE): leftover official leftover intro leftover official leftover The FT4 client is written in TypeScript leftover official leftover $FT4_CLIENT_NPM leftover official leftover backend leftover official leftover operations leftover official leftover and leftover official leftover queries leftover official leftover frontend leftover official leftover or leftover official leftover js leftover official leftover backend leftover official leftover $FT4_CLIENT_INTRO_URL leftover official leftover FT4 leftover official leftover documentation leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients filehub-client (leftover official $FILEHUB_CLIENT_URL_NO_SLASH leftover official 307 leftover official $FILEHUB_CLIENT_URL leftover official 200 leftover official $FILEHUB_CLIENT_TITLE): leftover official leftover intro leftover official leftover TypeScript leftover official leftover persisting leftover official leftover reading leftover official leftover files leftover official leftover Chromia leftover official leftover blockchain leftover official leftover Filehub leftover official leftover utilises leftover official leftover @chromia/ft4 leftover official leftover postchain-client leftover official leftover peer leftover official leftover dependency leftover official leftover Filehub leftover official leftover documentation leftover official leftover $FILEHUB_ECO_URL leftover official leftover skip leftover official leftover persist leftover official leftover admin leftover official leftover writes leftover official leftover FilehubAdministrator leftover official leftover storeFile leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>. leftover official leftover BUILD leftover official leftover page leftover official leftover prints leftover official leftover no leftover official leftover package leftover official leftover id.
        Leftover official BUILD clients filehub-client (leftover official $FILEHUB_CLIENT_INDEX_URL leftover official 307 leftover official $FILEHUB_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official $FILEHUB_CLIENT_INDEX_TITLE): leftover official leftover intro leftover official leftover The Filehub client is written in TypeScript leftover official leftover persisting leftover official leftover reading leftover official leftover files leftover official leftover Chromia leftover official leftover blockchain leftover official leftover Filehub leftover official leftover utilises leftover official leftover @chromia/ft4 leftover official leftover postchain-client leftover official leftover peer leftover official leftover dependency leftover official leftover Filehub leftover official leftover documentation leftover official leftover $FILEHUB_ECO_URL leftover official leftover Query-only leftover official leftover skip leftover official leftover persist leftover official leftover admin leftover official leftover writes leftover official leftover FilehubAdministrator leftover official leftover storeFile leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>. leftover official leftover BUILD leftover official leftover page leftover official leftover prints leftover official leftover no leftover official leftover package leftover official leftover id.
        Leftover official Filehub work (200): $FILEHUB_WORK_URL. Leftover official BUILD $FILEHUB_WORK_BUILD_404 and $FILEHUB_BUILD_INDEX_404 are 404.
        Leftover official Filehub work read-only: require("filehub"); new Filehub({ directoryNodeUrlPool, blockchainRid }); filehub.getFile(fileHash).
        Leftover official placeholders DIRECTORY_NODE_URL_POOL / FILEHUB_BLOCKCHAIN_RID only. Leftover official Filehub work / overview / vault-listing print 0.10 USD per MB perpetual. Leftover official Filehub gateway host $FILEHUB_GATEWAY_HOST (leftover official vault-listing); skip leftover official sample 64-hex paths. Skip FilehubAdministrator / registerFilechain / storeFile / payments writes.
        Leftover official BUILD clients bridge-client (leftover official $BRIDGE_CLIENT_URL_NO_SLASH leftover official 307 leftover official $BRIDGE_CLIENT_URL leftover official 200 leftover official $BRIDGE_CLIENT_TITLE): leftover official leftover intro leftover official leftover TypeScript leftover official leftover library leftover official leftover utilities leftover official leftover interacting leftover official leftover Chromia leftover official leftover token leftover official leftover bridge leftover official leftover pointer leftover official leftover bridge leftover official leftover client leftover official leftover documentation leftover official leftover $BRIDGE_ECO_URL leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover admin leftover official leftover writes leftover official leftover deposit leftover official leftover withdraw leftover official leftover mass-exit leftover official leftover allowToken leftover official leftover setBlockchainRid leftover official leftover registerAccount leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>. leftover official leftover BUILD leftover official leftover page leftover official leftover prints leftover official leftover no leftover official leftover package leftover official leftover id.
        Leftover official BUILD clients bridge-client (leftover official $BRIDGE_CLIENT_INDEX_URL leftover official 307 leftover official $BRIDGE_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official $BRIDGE_CLIENT_INDEX_TITLE): leftover official leftover intro leftover official leftover The Chromia bridge client is a TypeScript leftover official leftover library leftover official leftover utilities leftover official leftover interacting leftover official leftover Chromia leftover official leftover token leftover official leftover bridge leftover official leftover pointer leftover official leftover bridge leftover official leftover client leftover official leftover documentation leftover official leftover $BRIDGE_ECO_URL leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover admin leftover official leftover writes leftover official leftover deposit leftover official leftover withdraw leftover official leftover mass-exit leftover official leftover allowToken leftover official leftover setBlockchainRid leftover official leftover registerAccount leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>. leftover official leftover BUILD leftover official leftover page leftover official leftover prints leftover official leftover no leftover official leftover package leftover official leftover id.
        TypeScript library; leftover official BUILD page prints no package id. Leftover official leftover ecosystem configure (200): $BRIDGE_CONFIGURE_URL and leftover official work-with-client (200): $BRIDGE_WORK_URL print package id $BRIDGE_NPM. Quote leftover official eif.hbridge.bridge_mode as printed (no invented field name; see chromia_integrations_help). Leftover official read-only checkAllowance. Leftover official work-with-client prints YOU_NODE_URL_POOL as printed. Skip deposit / withdraw / mass-exit / allowToken / setBlockchainRid / registerAccount writes.
        Related official leftover ecosystem overviews: $FILEHUB_ECO_URL and $BRIDGE_ECO_URL.
        Related official leftover ICCF protocol (200 with trailing slash): $ICCF_PROTOCOL_URL
        Docs print IccfGtxModule; source pin is net.postchain.d1.iccf.IccfGTXModule plus library-chain com.chromia.iccf 1.90.1;
        leftover official-but-stale FT4-setup git directory-chain tag is 1.87.0. See ft4_module_args. Skip iccf_proof writes.
        Leftover official Filehub configure page prints leftover official package id filehub from configure AND work.
        Leftover official BUILD MCP server (200): $MCP_SERVER_URL. Leftover official page key $MCP_OFFICIAL_PAGE_KEY vs fat JAR $MCP_FAT_JAR_NAME. Leftover official prod $MCP_PROD_URL leftover official leftover official $MCP_LEFTOVER_OFFICIAL_PROD_SSE leftover official leftover official local README $MCP_LEFTOVER_OFFICIAL_LOCAL_SSE leftover official this-tree leftover official live leftover official GET / leftover official is leftover official the leftover official SSE leftover official endpoint leftover official leftover official GET /sse leftover official 404 leftover official leftover official /health leftover official {status:healthy, server:$MCP_FAT_JAR_NAME, version:$MCP_HEALTH_VERSION} leftover official leftover official host leftover official currently leftover official $MCP_LEFTOVER_OFFICIAL_HOST_STATUS. Leftover official local SSE $MCP_LOCAL_SSE via $MCP_LOCAL_GRADLE from $MCP_REPO. Leftover official LSP $LSP_MCP_NPM with optional Rell $LSP_MCP_RELL; leftover official config key lsp-mcp. Leftover official ChatGPT auth: No authentication. Skip leftover official explorer-dump sample BRIDs.
        Leftover official BUILD clients mcp-server (leftover official $MCP_SERVER_INDEX_URL leftover official 307 leftover official $MCP_SERVER_INDEX_URL_SLASH leftover official 200 leftover official $MCP_SERVER_INDEX_TITLE): leftover official leftover intro leftover official leftover A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps through the Chromia Explorer leftover official leftover Overview leftover official leftover Documentation Tools leftover official leftover Setup leftover official leftover Claude Rell LSP Integration leftover official leftover Usage Examples leftover official leftover Networks leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover explorer-dump leftover official leftover sample leftover official leftover BRIDs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover a leftover official leftover hosted leftover official leftover URL leftover official leftover leftover official leftover host leftover official leftover currently leftover official leftover $MCP_LEFTOVER_OFFICIAL_HOST_STATUS.
        Leftover official BUILD clients postchain-clients javascript-typescript (leftover official $JS_TS_INDEX_URL leftover official 307 leftover official $JS_TS_INDEX_URL_SLASH leftover official 200 leftover official $JS_TS_INDEX_TITLE): leftover official leftover intro leftover official leftover The JavaScript/TypeScript client library provides a comprehensive set of functions leftover official leftover Getting started leftover official leftover Hello World Quickstart leftover official leftover $JS_QUICKSTART_URL leftover official leftover Installation leftover official leftover postchain-client leftover official leftover Query Execution leftover official leftover Multiple Node Support leftover official leftover TypeScript Support leftover official leftover Automatic Discovery leftover official leftover Client Reference leftover official leftover $JS_REFERENCE_URL leftover official leftover Chromia React Kit leftover official leftover Client libraries overview leftover official leftover Cookbook examples leftover official leftover React/Rell leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients postchain-clients python-client (leftover official $PYTHON_INDEX_URL leftover official 307 leftover official $PYTHON_INDEX_URL_SLASH leftover official 200 leftover official $PYTHON_INDEX_TITLE): leftover official leftover intro leftover official leftover A Python client library for interacting with Postchain nodes leftover official leftover Features leftover official leftover aiohttp leftover official leftover Prerequisites leftover official leftover Python ${ChrGenerateClientHelp.PYTHON_MIN} leftover official leftover Installation leftover official leftover pip leftover official leftover ${ChrGenerateClientHelp.PIP_POSTCHAIN} leftover official leftover Configuration leftover official leftover ${ChrGenerateClientHelp.PYTHON_ENV_NODE} leftover official leftover ${ChrGenerateClientHelp.PYTHON_ENV_RID} leftover official leftover Quick Start leftover official leftover BlockchainClient leftover official leftover NetworkSettings leftover official leftover get_collections leftover official leftover Queries leftover official leftover get_all_books leftover official leftover get_all_reviews_for_book leftover official leftover Development leftover official leftover Running Tests leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover BUILD clients/postchain-clients/python-client (leftover official $PYTHON_CLIENT_INDEX_URL leftover official 307 leftover official $PYTHON_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official $PYTHON_CLIENT_INDEX_TITLE): leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients postchain-clients kotlin-client (leftover official $KOTLIN_INDEX_URL leftover official 307 leftover official $KOTLIN_INDEX_URL_SLASH leftover official 200 leftover official $KOTLIN_INDEX_TITLE): leftover official leftover intro leftover official leftover The Kotlin client library, postchain-client leftover official leftover Postchain-client leftover official leftover Installation leftover official leftover ${ChrGenerateClientHelp.MAVEN_POSTCHAIN} leftover official leftover Initializing leftover official leftover the leftover official leftover client leftover official leftover EndpointPool.singleUrl leftover official leftover ${ChrGenerateClientHelp.KOTLIN_LOCAL_ENDPOINT} leftover official leftover Queries leftover official leftover hello_world leftover official leftover Chromia leftover official leftover client leftover official leftover ${ChrGenerateClientHelp.KOTLIN_STANDARD_CLIENT} leftover official leftover leftover official leftover Gradle leftover official leftover still leftover official leftover prints leftover official leftover ${ChrGenerateClientHelp.MAVEN_POSTCHAIN} leftover official leftover Maven leftover official leftover ${ChrGenerateClientHelp.MAVEN_CHROMIA} leftover official leftover wins leftover official leftover FT4 leftover official leftover Client leftover official leftover ${ChrGenerateClientHelp.MAVEN_FT4} leftover official leftover Features leftover official leftover Authentication leftover official leftover Descriptors leftover official leftover Asset leftover official leftover Management leftover official leftover getAssetBalance leftover official leftover ${ChrGenerateClientHelp.OUTDATED_BANNER} leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover BUILD clients/postchain-clients/kotlin-client (leftover official $KOTLIN_CLIENT_INDEX_URL leftover official 307 leftover official $KOTLIN_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official $KOTLIN_CLIENT_INDEX_TITLE): leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients postchain-clients c-sharp-client (leftover official $CSHARP_INDEX_URL leftover official 307 leftover official $CSHARP_INDEX_URL_SLASH leftover official 200 leftover official $CSHARP_INDEX_TITLE): leftover official leftover intro leftover official leftover The C# client provides functionality leftover official leftover Installation leftover official leftover Native leftover official leftover Unity leftover official leftover Importing leftover official leftover into leftover official leftover Unity leftover official leftover Unity WebGL leftover official leftover Injecting leftover official leftover custom leftover official leftover transport leftover official leftover UnityTransport leftover official leftover SetTransport leftover official leftover NuGet leftover official leftover DLL leftover official leftover leftover official leftover page leftover official leftover prints leftover official leftover no leftover official leftover package leftover official leftover id leftover official leftover Initializing leftover official leftover the leftover official leftover client leftover official leftover ChromiaClient.Create leftover official leftover CreateFromDirectory leftover official leftover $CSHARP_DIRECTORY_HOST leftover official leftover Queries leftover official leftover get_city leftover official leftover IGtvSerializable leftover official leftover JsonProperty leftover official leftover Error leftover official leftover handling leftover official leftover ChromiaException leftover official leftover TransportException leftover official leftover ${ChrGenerateClientHelp.OUTDATED_BANNER} leftover official leftover leftover official leftover directory leftover official leftover pool leftover official leftover still leftover official leftover prints leftover official leftover Create leftover official leftover not leftover official leftover CreateFromDirectory leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover BUILD clients/postchain-clients/c-sharp-client (leftover official $CSHARP_CLIENT_INDEX_URL leftover official 307 leftover official $CSHARP_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official $CSHARP_CLIENT_INDEX_TITLE): leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover BUILD clients/postchain-clients/go-client (leftover official $GO_CLIENT_INDEX_URL leftover official 307 leftover official $GO_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official $GO_CLIENT_INDEX_TITLE): leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover BUILD clients/postchain-clients/rust-client (leftover official $RUST_CLIENT_INDEX_URL leftover official 307 leftover official $RUST_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official $RUST_CLIENT_INDEX_TITLE): leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients postchain-clients rust-client (leftover official $RUST_INDEX_URL leftover official 307 leftover official $RUST_INDEX_URL_SLASH leftover official 200 leftover official $RUST_INDEX_TITLE): leftover official leftover intro leftover official leftover The Rust client is used for interacting leftover official leftover Installation leftover official leftover Cargo.toml leftover official leftover $RUST_CRATE leftover official leftover $RUST_CRATE_VERSION leftover official leftover tokio leftover official leftover serde leftover official leftover serde_json leftover official leftover Initializing leftover official leftover the leftover official leftover client leftover official leftover RestClient leftover official leftover Queries leftover official leftover client.query leftover official leftover Params leftover official leftover Error leftover official leftover handling leftover official leftover RestResponse leftover official leftover RestError leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_ERROR_ARM leftover official leftover $RUST_SOURCE_ERR_ARM leftover official leftover wins leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_ERR_INCOMPLETE leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_ERR_IDENT leftover official leftover $RUST_SOURCE_ERR_IDENT leftover official leftover wins leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_QUERY_ARGS_REF leftover official leftover $RUST_SOURCE_QUERY_ARGS leftover official leftover wins leftover official leftover Using leftover official leftover serde leftover official leftover for leftover official leftover serialization leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_DECIMAL_SERDE leftover official leftover $RUST_SOURCE_DECIMAL_SERDE leftover official leftover wins leftover official leftover Logging leftover official leftover tracing leftover official leftover Parameter leftover official leftover types leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_DECIMAL_TYPO leftover official leftover $RUST_SOURCE_DECIMAL leftover official leftover wins leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_BYTEARRAY leftover official leftover $RUST_SOURCE_BYTEARRAY leftover official leftover wins leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_RESTCLIENT_LIFETIME leftover official leftover leftover official leftover $RUST_LEFTOVER_OFFICIAL_RESTCLIENT_HTML_ENTITY leftover official leftover Examples leftover official leftover book-review leftover official leftover ${ChrGenerateClientHelp.OUTDATED_BANNER} leftover official leftover leftover official leftover Params leftover official leftover table leftover official leftover still leftover official leftover prints leftover official leftover $RUST_LEFTOVER_OFFICIAL_DECIMAL_TYPO leftover official leftover $RUST_SOURCE_DECIMAL leftover official leftover wins leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official BUILD clients postchain-clients go-client (leftover official $GO_INDEX_URL leftover official 307 leftover official $GO_INDEX_URL_SLASH leftover official 200 leftover official $GO_INDEX_TITLE): leftover official leftover intro leftover official leftover The Go client offers a range of built-in functions leftover official leftover Installation leftover official leftover go get leftover official leftover $GO_MODULE leftover official leftover Generic Transfer Value leftover official leftover GTV leftover official leftover Null leftover official leftover ByteArray leftover official leftover String leftover official leftover Integer leftover official leftover Dict leftover official leftover Array leftover official leftover BigInteger leftover official leftover Merkle leftover official leftover HashVersion2 leftover official leftover version leftover official leftover 1 leftover official leftover hash-collision leftover official leftover bug leftover official leftover Postchain Client leftover official leftover NewClient leftover official leftover $GO_LEFTOVER_OFFICIAL_NODE leftover official leftover http://localhost:7740 leftover official leftover wins leftover official leftover GetBlockchainRID leftover official leftover 123 leftover official leftover GetFeatures leftover official leftover DetectMerkleHashVersion leftover official leftover Queries leftover official leftover get_account_balance leftover official leftover Retrieving leftover official leftover Transactions leftover official leftover and leftover official leftover Blocks leftover official leftover GetTransaction leftover official leftover GetTransactionInfo leftover official leftover GetConfirmationProof leftover official leftover BlockAtHeight leftover official leftover BlockByRID leftover official leftover API leftover official leftover Reference leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover get-started/about/dapp (leftover official $GET_STARTED_DAPP_INDEX_URL leftover official 307 leftover official $GET_STARTED_DAPP_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_DAPP_INDEX_TITLE): leftover official leftover Query-only leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover get-started/about/chromia-vs-evm (leftover official $GET_STARTED_CHROMIA_VS_EVM_INDEX_URL leftover official 307 leftover official $GET_STARTED_CHROMIA_VS_EVM_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_CHROMIA_VS_EVM_INDEX_TITLE): leftover official leftover Query-only leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover get-started/about/protocols/gtx (leftover official $GET_STARTED_GTX_INDEX_URL leftover official 307 leftover official $GET_STARTED_GTX_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_GTX_INDEX_TITLE): leftover official leftover Query-only leftover official leftover HELP ONLY leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>.
        Leftover official leftover get-started/about/architecture/chains/system-anchoring-chain leftover official leftover slash leftover official leftover title (leftover official $GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_URL leftover official 307 leftover official $GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_TITLE): leftover official leftover Query-only leftover official leftover HELP ONLY leftover official leftover WRITE SKIP leftover official leftover Origin parked leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover leftover official leftover chr leftover official leftover keygen leftover official leftover leftover official leftover sign leftover official leftover recipe leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover leftover official leftover <BlockchainRID>.
        Leftover official leftover get-started/about/staking/user-delegation leftover official leftover slash leftover official leftover title (leftover official $GET_STARTED_USER_DELEGATION_INDEX_URL leftover official 307 leftover official $GET_STARTED_USER_DELEGATION_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_USER_DELEGATION_INDEX_TITLE): leftover official leftover Query-only leftover official leftover HELP ONLY leftover official leftover WRITE SKIP leftover official leftover Origin parked leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover leftover official leftover chr leftover official leftover keygen leftover official leftover leftover official leftover sign leftover official leftover recipe leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover leftover official leftover <BlockchainRID>.
        Leftover official leftover get-started/about/protocols/iccf leftover official leftover slash leftover official leftover title (leftover official $GET_STARTED_ICCF_PROTOCOL_INDEX_URL leftover official 307 leftover official $GET_STARTED_ICCF_PROTOCOL_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_ICCF_PROTOCOL_INDEX_TITLE): leftover official leftover Query-only leftover official leftover HELP ONLY leftover official leftover WRITE SKIP leftover official leftover Origin parked leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover leftover official leftover chr leftover official leftover keygen leftover official leftover leftover official leftover sign leftover official leftover recipe leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover leftover official leftover <BlockchainRID>.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-structure/user-proposal-flows INDEX (leftover official $ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover ECOSYSTEM ecosystem/governance/governance-voting-process/voting-flow INDEX (leftover official $ECOSYSTEM_GOV_VOTING_FLOW_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_VOTING_FLOW_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_GOV_VOTING_FLOW_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover ECOSYSTEM ecosystem/bridge/bridge-client/client INDEX (leftover official $ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover deposit leftover official leftover withdraw leftover official leftover mass-exit leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover leftover official leftover chr leftover official leftover keygen leftover official leftover leftover official leftover sign leftover official leftover recipe leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover <BlockchainRID>)
        Leftover official leftover LEARN courses/book-review/book-review-entity/write-queries INDEX (leftover official $LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/ft4-demo-app/module-blockchain/account-management INDEX (leftover official $LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_URL leftover official 301 leftover official $LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/my-news-feed/introduction INDEX (leftover official $LEARN_NEWS_INTRODUCTION_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_INTRODUCTION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_INTRODUCTION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/React INDEX (leftover official $LEARN_TAGS_REACT_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_REACT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_REACT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Skipped: signed txs, key generation, and official pages that only print sample private keys.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("tool", TOOL_NAME)
        put("docs", OVERVIEW_URL)
        put("overview_url", OVERVIEW_URL)
        put("overview_url_slash", OVERVIEW_URL_SLASH)
        put("overview_title", OVERVIEW_TITLE)
        put("overview_index_docs", OVERVIEW_INDEX_URL)
        put("overview_index_url_slash", OVERVIEW_INDEX_URL_SLASH)
        put("overview_index_title", OVERVIEW_INDEX_TITLE)
        put("overview_index_intro", leftoverOfficialOverviewIntro())
        put("overview_index_card_rest", OVERVIEW_INDEX_CARD_REST)
        put("overview_index_card_react", OVERVIEW_INDEX_CARD_REACT)
        put("overview_index_card_postchain", OVERVIEW_INDEX_CARD_POSTCHAIN)
        put("overview_index_card_bridge", OVERVIEW_INDEX_CARD_BRIDGE)
        put("overview_index_card_ft4", OVERVIEW_INDEX_CARD_FT4)
        put("overview_index_card_filehub", OVERVIEW_INDEX_CARD_FILEHUB)
        put("overview_index_card_mcp", OVERVIEW_INDEX_CARD_MCP)
        put("overview_intro", leftoverOfficialOverviewIntro())
        put("web_static_local_url", WEB_STATIC_LOCAL_URL)
        put(
            "pages",
            buildJsonObject {
                put("overview", OVERVIEW_URL)
                put("postchain_clients", POSTCHAIN_CLIENTS_URL)
                put("deploy_frontend", DEPLOY_FRONTEND_URL)
                put("csharp", CSHARP_URL)
                put("go", GO_URL)
                put("rust", RUST_URL)
                put("react", REACT_URL)
                put("rest", REST_URL)
                put("cookbook_create_rell_dapp", COOKBOOK_CREATE_RELL_URL)
                put("js_quickstart", JS_QUICKSTART_URL)
                put("run_dapp_cli", RUN_DAPP_CLI_URL)
                put("js_reference", JS_REFERENCE_URL)
                put("kotlin_client", ChrGenerateClientHelp.KOTLIN_CLIENT_URL)
                put("python_client", ChrGenerateClientHelp.PYTHON_CLIENT_URL)
                put("filehub_client", FILEHUB_CLIENT_URL)
                put("bridge_client", BRIDGE_CLIENT_URL)
                put("ft4_client", FT4_CLIENT_URL)
                put("filehub_ecosystem", FILEHUB_ECO_URL)
                put("bridge_ecosystem", BRIDGE_ECO_URL)
                put("iccf_protocol", ICCF_PROTOCOL_URL)
                put("filehub_configure", FILEHUB_CONFIGURE_URL)
                put("filehub_work", FILEHUB_WORK_URL)
                put("filehub_work_build_404", FILEHUB_WORK_BUILD_404)
                put("filehub_build_index_404", FILEHUB_BUILD_INDEX_404)
                put("bridge_configure", BRIDGE_CONFIGURE_URL)
                put("bridge_work", BRIDGE_WORK_URL)
                put("mcp_server", MCP_SERVER_URL)
            }
        )
        put(
            "packages",
            buildJsonObject {
                put("go_module", GO_MODULE)
                put("rust_crate", RUST_CRATE)
                put("rust_crate_version", RUST_CRATE_VERSION)
                put("react_npm", REACT_NPM)
                put("csharp_nuget", "official page does not print a package id")
                put("filehub_npm", FILEHUB_NPM)
                put("filehub_npm_source", "leftover official configure and leftover official work print filehub; leftover official BUILD client page prints none")
                put("bridge_npm", BRIDGE_NPM)
                put("bridge_npm_source", "leftover official leftover ecosystem configure and leftover official work-with-client print @chromia/bridge-client; leftover official BUILD bridge client page prints none")
                put("ft4_npm", FT4_CLIENT_NPM)
                put("ft4_npm_source", "leftover official leftover BUILD clients ft4-client prints @chromia/ft4")
                put("lsp_mcp_npm", LSP_MCP_NPM)
                put("lsp_mcp_rell", LSP_MCP_RELL)
            }
        )
        put("csharp_query", csharpQuery())
        put("csharp_create_iid", csharpCreateFromIid())
        put("csharp_create_directory", leftoverOfficialCsharpCreateFromDirectory())
        put("csharp_leftover_official_directory_create", leftoverOfficialCsharpDirectoryCreate())
        put("csharp_directory_host", CSHARP_DIRECTORY_HOST)
        put("csharp_query_params", leftoverOfficialCsharpQueryParams())
        put("go_query", goQuery())
        put("go_leftover_official_node", GO_LEFTOVER_OFFICIAL_NODE)
        put("rust_client", rustClient())
        put("rust_query", rustQuery())
        put("rust_status", leftoverOfficialRustStatus())
        put("rust_query_error", leftoverOfficialRustQueryError())
        put("rust_leftover_official_decimal_typo", RUST_LEFTOVER_OFFICIAL_DECIMAL_TYPO)
        put("rust_source_decimal", RUST_SOURCE_DECIMAL)
        put("rust_leftover_official_error_arm", RUST_LEFTOVER_OFFICIAL_ERROR_ARM)
        put("rust_source_err_arm", RUST_SOURCE_ERR_ARM)
        put("rust_leftover_official_err_incomplete", RUST_LEFTOVER_OFFICIAL_ERR_INCOMPLETE)
        put("rust_leftover_official_err_ident", RUST_LEFTOVER_OFFICIAL_ERR_IDENT)
        put("rust_source_err_ident", RUST_SOURCE_ERR_IDENT)
        put("rust_leftover_official_query_args_ref", RUST_LEFTOVER_OFFICIAL_QUERY_ARGS_REF)
        put("rust_source_query_args", RUST_SOURCE_QUERY_ARGS)
        put("rust_leftover_official_bytearray", RUST_LEFTOVER_OFFICIAL_BYTEARRAY)
        put("rust_source_bytearray", RUST_SOURCE_BYTEARRAY)
        put("rust_leftover_official_decimal_serde", RUST_LEFTOVER_OFFICIAL_DECIMAL_SERDE)
        put("rust_source_decimal_serde", RUST_SOURCE_DECIMAL_SERDE)
        put("rust_leftover_official_restclient_lifetime", RUST_LEFTOVER_OFFICIAL_RESTCLIENT_LIFETIME)
        put("rust_leftover_official_restclient_html_entity", RUST_LEFTOVER_OFFICIAL_RESTCLIENT_HTML_ENTITY)
        put("rest_url_slash", REST_URL_SLASH)
        put("rest_title", REST_TITLE)
        put("postchain_rest_api_index_docs", POSTCHAIN_REST_API_INDEX_URL)
        put("postchain_rest_api_index_url_slash", POSTCHAIN_REST_API_INDEX_URL_SLASH)
        put("postchain_rest_api_index_title", POSTCHAIN_REST_API_INDEX_TITLE)
        put("postchain_rest_api_index_intro", leftoverOfficialPostchainRestApiIndexIntro())
        put("ft4_client_url", FT4_CLIENT_URL)
        put("ft4_client_url_slash", FT4_CLIENT_URL_SLASH)
        put("ft4_client_title", FT4_CLIENT_TITLE)
        put("ft4_client_intro", FT4_CLIENT_INTRO_URL)
        put("ft4_client_index_docs", FT4_CLIENT_INDEX_URL)
        put("ft4_client_index_url_slash", FT4_CLIENT_INDEX_URL_SLASH)
        put("ft4_client_index_title", FT4_CLIENT_INDEX_TITLE)
        put("ft4_client_index_intro", leftoverOfficialFt4ClientIndexIntro())
        put("postchain_clients_url", POSTCHAIN_CLIENTS_URL)
        put("postchain_clients_url_slash", POSTCHAIN_CLIENTS_URL_SLASH)
        put("postchain_clients_title", POSTCHAIN_CLIENTS_TITLE)
        put("postchain_clients_index_docs", POSTCHAIN_CLIENTS_INDEX_URL)
        put("postchain_clients_index_url_slash", POSTCHAIN_CLIENTS_INDEX_URL_SLASH)
        put("postchain_clients_index_title", POSTCHAIN_CLIENTS_INDEX_TITLE)
        put("postchain_clients_intro_langs", POSTCHAIN_CLIENTS_INTRO_LANGS)
        put("postchain_clients_intro", leftoverOfficialPostchainClientsIntro())
        put("postchain_clients_card_js", POSTCHAIN_CLIENTS_CARD_JS)
        put("postchain_clients_card_kotlin", POSTCHAIN_CLIENTS_CARD_KOTLIN)
        put("postchain_clients_card_csharp", POSTCHAIN_CLIENTS_CARD_CSHARP)
        put("postchain_clients_card_rust", POSTCHAIN_CLIENTS_CARD_RUST)
        put("postchain_clients_card_python", POSTCHAIN_CLIENTS_CARD_PYTHON)
        put("postchain_clients_card_go", POSTCHAIN_CLIENTS_CARD_GO)
        put("postchain_clients_card_js_url", POSTCHAIN_CLIENTS_CARD_JS_URL)
        put(
            "postchain_clients_cards",
            buildJsonArray {
                add(JsonPrimitive(POSTCHAIN_CLIENTS_CARD_JS))
                add(JsonPrimitive(POSTCHAIN_CLIENTS_CARD_KOTLIN))
                add(JsonPrimitive(POSTCHAIN_CLIENTS_CARD_CSHARP))
                add(JsonPrimitive(POSTCHAIN_CLIENTS_CARD_RUST))
                add(JsonPrimitive(POSTCHAIN_CLIENTS_CARD_PYTHON))
                add(JsonPrimitive(POSTCHAIN_CLIENTS_CARD_GO))
            }
        )
        put("react_url", REACT_URL)
        put("react_url_slash", REACT_URL_SLASH)
        put("react_title", REACT_TITLE)
        put("react_intro", leftoverOfficialReactKitIntro())
        put("react_hooks", reactHooks())
        put("react_leftover_official_pool", REACT_LEFTOVER_OFFICIAL_POOL)
        put("react_kit_index_docs", REACT_KIT_INDEX_URL)
        put("react_kit_index_url_slash", REACT_KIT_INDEX_URL_SLASH)
        put("react_kit_index_title", REACT_KIT_INDEX_TITLE)
        put("react_kit_index_intro", leftoverOfficialReactKitIntro())
        put(
            "rest_read_paths",
            buildJsonArray { restGetPaths.forEach { add(JsonPrimitive(it)) } }
        )
        put(
            "rest_query_example",
            "curl -X GET 'localhost:7740/query/<BlockchainRID>?type=hello_world'"
        )
        put("filehub_client_url", FILEHUB_CLIENT_URL_NO_SLASH)
        put("filehub_client_url_slash", FILEHUB_CLIENT_URL)
        put("filehub_client_title", FILEHUB_CLIENT_TITLE)
        put("filehub_client_intro", leftoverOfficialFilehubClientIntro())
        put("filehub_client_index_docs", FILEHUB_CLIENT_INDEX_URL)
        put("filehub_client_index_url_slash", FILEHUB_CLIENT_INDEX_URL_SLASH)
        put("filehub_client_index_title", FILEHUB_CLIENT_INDEX_TITLE)
        put("filehub_client_index_intro", leftoverOfficialFilehubClientIntro())
        put(
            "filehub_get_file", filehubGetFile())
        put("filehub_construct", filehubConstruct())
        put("filehub_gateway_host", FILEHUB_GATEWAY_HOST)
        put("filehub_cost", "leftover official printed 0.10 USD per MB perpetual")
        put("bridge_client_url", BRIDGE_CLIENT_URL_NO_SLASH)
        put("bridge_client_url_slash", BRIDGE_CLIENT_URL)
        put("bridge_client_title", BRIDGE_CLIENT_TITLE)
        put("bridge_client_intro", leftoverOfficialBridgeClientIntro())
        put("bridge_client_index_docs", BRIDGE_CLIENT_INDEX_URL)
        put("bridge_client_index_url_slash", BRIDGE_CLIENT_INDEX_URL_SLASH)
        put("bridge_client_index_title", BRIDGE_CLIENT_INDEX_TITLE)
        put("bridge_client_index_intro", leftoverOfficialBridgeClientIntro())
        put("bridge_check_allowance", bridgeCheckAllowance())
        put("bridge_client_init", bridgeClientInit())
        put("mcp_official_page_key", MCP_OFFICIAL_PAGE_KEY)
        put("mcp_fat_jar_server_name", MCP_FAT_JAR_NAME)
        put("mcp_prod_url", MCP_PROD_URL)
        put("mcp_local_sse", MCP_LOCAL_SSE)
        put("mcp_local_gradle", MCP_LOCAL_GRADLE)
        put("mcp_repo", MCP_REPO)
        put("mcp_leftover_official_prod_sse", MCP_LEFTOVER_OFFICIAL_PROD_SSE)
        put("mcp_leftover_official_local_sse", MCP_LEFTOVER_OFFICIAL_LOCAL_SSE)
        put("mcp_live_sse_path", MCP_LIVE_SSE_PATH)
        put("mcp_leftover_official_sse_path", MCP_LEFTOVER_OFFICIAL_SSE_PATH)
        put("mcp_health_path", MCP_HEALTH_PATH)
        put("mcp_health_status", MCP_HEALTH_STATUS)
        put("mcp_health_version", MCP_HEALTH_VERSION)
        put("mcp_leftover_official_host_status", MCP_LEFTOVER_OFFICIAL_HOST_STATUS)
        put("mcp_prod_config", mcpProdConfig())
        put("mcp_local_config", mcpLocalConfig())
        put("mcp_lsp_config", mcpLspConfig())
        put("mcp_server_index_docs", MCP_SERVER_INDEX_URL)
        put("mcp_server_index_url_slash", MCP_SERVER_INDEX_URL_SLASH)
        put("mcp_server_index_title", MCP_SERVER_INDEX_TITLE)
        put("mcp_server_index_intro", leftoverOfficialMcpServerIntro())
        put("js_ts_index_docs", JS_TS_INDEX_URL)
        put("js_ts_index_url_slash", JS_TS_INDEX_URL_SLASH)
        put("js_ts_index_title", JS_TS_INDEX_TITLE)
        put("js_ts_index_intro", leftoverOfficialJsTsIndexIntro())
        put("js_quickstart_index_docs", JS_QUICKSTART_INDEX_URL)
        put("js_quickstart_index_url_slash", JS_QUICKSTART_INDEX_URL_SLASH)
        put("js_quickstart_index_title", JS_QUICKSTART_INDEX_TITLE)
        put("js_reference_index_docs", JS_REFERENCE_INDEX_URL)
        put("js_reference_index_url_slash", JS_REFERENCE_INDEX_URL_SLASH)
        put("js_reference_index_title", JS_REFERENCE_INDEX_TITLE)
        put("python_index_docs", PYTHON_INDEX_URL)
        put("python_index_url_slash", PYTHON_INDEX_URL_SLASH)
        put("python_index_title", PYTHON_INDEX_TITLE)
        put("python_index_intro", leftoverOfficialPythonIndexIntro())
        put("python_client_index_docs", PYTHON_CLIENT_INDEX_URL)
        put("python_client_index_url_slash", PYTHON_CLIENT_INDEX_URL_SLASH)
        put("python_client_index_title", PYTHON_CLIENT_INDEX_TITLE)
        put("kotlin_index_docs", KOTLIN_INDEX_URL)
        put("kotlin_index_url_slash", KOTLIN_INDEX_URL_SLASH)
        put("kotlin_index_title", KOTLIN_INDEX_TITLE)
        put("kotlin_index_intro", leftoverOfficialKotlinIndexIntro())
        put("kotlin_client_index_docs", KOTLIN_CLIENT_INDEX_URL)
        put("kotlin_client_index_url_slash", KOTLIN_CLIENT_INDEX_URL_SLASH)
        put("kotlin_client_index_title", KOTLIN_CLIENT_INDEX_TITLE)
        put("csharp_index_docs", CSHARP_INDEX_URL)
        put("csharp_index_url_slash", CSHARP_INDEX_URL_SLASH)
        put("csharp_index_title", CSHARP_INDEX_TITLE)
        put("csharp_client_index_docs", CSHARP_CLIENT_INDEX_URL)
        put("csharp_client_index_url_slash", CSHARP_CLIENT_INDEX_URL_SLASH)
        put("csharp_client_index_title", CSHARP_CLIENT_INDEX_TITLE)
        put("go_client_index_docs", GO_CLIENT_INDEX_URL)
        put("go_client_index_url_slash", GO_CLIENT_INDEX_URL_SLASH)
        put("go_client_index_title", GO_CLIENT_INDEX_TITLE)
        put("get_started_dapp_index_docs", GET_STARTED_DAPP_INDEX_URL)
        put("get_started_dapp_index_url_slash", GET_STARTED_DAPP_INDEX_URL_SLASH)
        put("get_started_dapp_index_title", GET_STARTED_DAPP_INDEX_TITLE)
        put("get_started_chromia_vs_evm_index_docs", GET_STARTED_CHROMIA_VS_EVM_INDEX_URL)
        put("get_started_chromia_vs_evm_index_url_slash", GET_STARTED_CHROMIA_VS_EVM_INDEX_URL_SLASH)
        put("get_started_chromia_vs_evm_index_title", GET_STARTED_CHROMIA_VS_EVM_INDEX_TITLE)
        put("get_started_gtx_index_docs", GET_STARTED_GTX_INDEX_URL)
        put("get_started_gtx_index_url_slash", GET_STARTED_GTX_INDEX_URL_SLASH)
        put("get_started_gtx_index_title", GET_STARTED_GTX_INDEX_TITLE)
        put("get_started_system_anchoring_chain_index_docs", GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_URL)
        put("get_started_system_anchoring_chain_index_url_slash", GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_URL_SLASH)
        put("get_started_system_anchoring_chain_index_title", GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_TITLE)
        put("get_started_user_delegation_index_docs", GET_STARTED_USER_DELEGATION_INDEX_URL)
        put("get_started_user_delegation_index_url_slash", GET_STARTED_USER_DELEGATION_INDEX_URL_SLASH)
        put("get_started_user_delegation_index_title", GET_STARTED_USER_DELEGATION_INDEX_TITLE)
        put("get_started_iccf_protocol_index_docs", GET_STARTED_ICCF_PROTOCOL_INDEX_URL)
        put("get_started_iccf_protocol_index_url_slash", GET_STARTED_ICCF_PROTOCOL_INDEX_URL_SLASH)
        put("get_started_iccf_protocol_index_title", GET_STARTED_ICCF_PROTOCOL_INDEX_TITLE)
        put("csharp_index_intro", leftoverOfficialCsharpIndexIntro())
        put("rust_index_docs", RUST_INDEX_URL)
        put("rust_index_url_slash", RUST_INDEX_URL_SLASH)
        put("rust_index_title", RUST_INDEX_TITLE)
        put("rust_client_index_docs", RUST_CLIENT_INDEX_URL)
        put("rust_client_index_url_slash", RUST_CLIENT_INDEX_URL_SLASH)
        put("rust_client_index_title", RUST_CLIENT_INDEX_TITLE)
        put("rust_index_intro", leftoverOfficialRustIndexIntro())
        put("go_index_docs", GO_INDEX_URL)
        put("go_index_url_slash", GO_INDEX_URL_SLASH)
        put("go_index_title", GO_INDEX_TITLE)
        put("go_index_intro", leftoverOfficialGoIndexIntro())
        put(
            "skipped_sign_or_key",
            buildJsonArray { skipped.forEach { add(JsonPrimitive(it)) } }
        )
        put("hello_world_rell", ChrGenerateClientHelp.helloWorldRell())
        put("hello_world_result", "Hello World!")
        put("js_reference_directory_iid", ChrGenerateClientHelp.DIRECTORY_CHAIN_IID)
        put("js_reference_brid_setting", ChrGenerateClientHelp.SOURCE_BRID_SETTING)
        put("js_reference_leftover_official_prose_brid", ChrGenerateClientHelp.LEFTOVER_OFFICIAL_PROSE_BRID)
        put("js_reference_sticky_create_client", ChrGenerateClientHelp.leftoverOfficialStickyQueryClient())
        put("js_reference_leftover_official_strategy_typo", ChrGenerateClientHelp.LEFTOVER_OFFICIAL_STRATEGY_TYPO)
        put("js_reference_leftover_official_abort_typo", ChrGenerateClientHelp.LEFTOVER_OFFICIAL_ABORT_TYPO)
        put("js_reference_source_failover_abort", ChrGenerateClientHelp.SOURCE_FAILOVER_ABORT)
        put("leftover_official_outdated_banner", ChrGenerateClientHelp.OUTDATED_BANNER)
        put("python_env_node", ChrGenerateClientHelp.PYTHON_ENV_NODE)
        put("python_env_rid", ChrGenerateClientHelp.PYTHON_ENV_RID)
        put("python_query_reviews", ChrGenerateClientHelp.leftoverOfficialPythonReviewsQuery())
        put("python_env", ChrGenerateClientHelp.leftoverOfficialPythonEnv())
        put("kotlin_standard_chromia_client", ChrGenerateClientHelp.leftoverOfficialStandardChromiaClient())
        put("generate_client_help", ChrGenerateClientHelp.DOCS_URL.let { "chr_generate_client_help" })
        put("cookbook_help", "chromia_cookbook_help")
        put("ecosystem_gov_user_proposal_flows_index_url_slash", ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_URL_SLASH)
        put("ecosystem_gov_user_proposal_flows_index_title", ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_TITLE)
        put("ecosystem_gov_voting_flow_index_url_slash", ECOSYSTEM_GOV_VOTING_FLOW_INDEX_URL_SLASH)
        put("ecosystem_gov_voting_flow_index_title", ECOSYSTEM_GOV_VOTING_FLOW_INDEX_TITLE)
        put("ecosystem_bridge_client_configure_index_url_slash", ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_URL_SLASH)
        put("ecosystem_bridge_client_configure_index_title", ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_TITLE)
        put("learn_book_review_review_queries_index_url_slash", LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_URL_SLASH)
        put("learn_book_review_review_queries_index_title", LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_TITLE)
        put("learn_ft4_demo_account_mgmt_index_url_slash", LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_URL_SLASH)
        put("learn_ft4_demo_account_mgmt_index_title", LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_TITLE)
        put("learn_news_introduction_index_url_slash", LEARN_NEWS_INTRODUCTION_INDEX_URL_SLASH)
        put("learn_news_introduction_index_title", LEARN_NEWS_INTRODUCTION_INDEX_TITLE)
        put("learn_tags_react_index_url_slash", LEARN_TAGS_REACT_INDEX_URL_SLASH)
        put("learn_tags_react_index_title", LEARN_TAGS_REACT_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover BUILD clients/postchain-clients leftovers encoded as POSTCHAIN_CLIENTS_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-clients/javascript-typescript leftovers encoded as JS_TS_INDEX_* (query-only).
// Leftover official leftover BUILD clients/ft4-client leftovers encoded as FT4_CLIENT_INDEX_* (query-only).
// Leftover official leftover BUILD clients/react-kit leftovers encoded as REACT_KIT_INDEX_* (query-only).
// Leftover official leftover BUILD clients/filehub-client leftovers encoded as FILEHUB_CLIENT_INDEX_* (query-only).
// Leftover official leftover BUILD clients/bridge-client leftovers encoded as BRIDGE_CLIENT_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-rest-api leftovers encoded as POSTCHAIN_REST_API_INDEX_* (query-only).
// Leftover official leftover BUILD clients/overview leftovers encoded as OVERVIEW_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-clients/javascript-typescript/hello-world-quickstart leftovers encoded as JS_QUICKSTART_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-clients/javascript-typescript/reference leftovers encoded as JS_REFERENCE_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-clients/c-sharp-client leftovers encoded as CSHARP_CLIENT_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-clients/go-client leftovers encoded as GO_CLIENT_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-clients/rust-client leftovers encoded as RUST_CLIENT_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-clients/kotlin-client leftovers encoded as KOTLIN_CLIENT_INDEX_* (query-only).
// Leftover official leftover BUILD clients/postchain-clients/python-client leftovers encoded as PYTHON_CLIENT_INDEX_* (query-only).
// Leftover official leftover get-started/about/dapp leftovers encoded as GET_STARTED_DAPP_INDEX_* (query-only).
// Leftover official leftover get-started/about/chromia-vs-evm leftovers encoded as GET_STARTED_CHROMIA_VS_EVM_INDEX_* (query-only).
// Leftover official leftover get-started/about/protocols/gtx leftovers encoded as GET_STARTED_GTX_INDEX_* (query-only).
// Leftover official leftover get-started/about/architecture/chains/system-anchoring-chain leftovers encoded as GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_* (query-only).
// Leftover official leftover get-started/about/staking/user-delegation leftovers encoded as GET_STARTED_USER_DELEGATION_INDEX_* (query-only).
// Leftover official leftover get-started/about/protocols/iccf leftovers encoded as GET_STARTED_ICCF_PROTOCOL_INDEX_* (query-only).
// Leftover official leftover ecosystem/governance/getting-started/governance-structure/user-proposal-flows leftovers encoded as ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_* (query-only).
// Leftover official leftover ecosystem/governance/governance-voting-process/voting-flow leftovers encoded as ECOSYSTEM_GOV_VOTING_FLOW_INDEX_* (query-only).
// Leftover official leftover ecosystem/bridge/bridge-client/client leftovers encoded as ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_* (query-only).
// Leftover official leftover LEARN courses/book-review/book-review-entity/write-queries leftovers encoded as LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_* (query-only).
// Leftover official leftover LEARN courses/ft4-demo-app/module-blockchain/account-management leftovers encoded as LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_* (query-only).
// Leftover official leftover LEARN courses/my-news-feed/introduction INDEX leftovers encoded as LEARN_NEWS_INTRODUCTION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/React INDEX leftovers encoded as LEARN_TAGS_REACT_INDEX_* (query-only HELP ONLY WRITE SKIP).
