package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official BUILD query-only wiring for C# / Go / Rust / React Kit / REST.
 * JS/TS, Kotlin, Python, and FT4 local reads live on chr_generate_client_help.
 * Official BUILD clients ft4-client slash/title leftovers live here (query-only).
 * Official BUILD clients postchain-clients slash/title/child-card leftovers live here (query-only).
 * Official BUILD clients react-kit slash/title leftovers live here (query-only).
 * Official BUILD clients filehub-client slash/title leftovers live here (query-only).
 * Official BUILD clients bridge-client slash/title leftovers live here (query-only).
 * Official pages only. Official Filehub work getFile / MCP setup / bridge checkAllowance are read-only. Skips signed txs, key generation, FilehubAdministrator writes, MCP explorer-dump sample BRIDs, and invented package ids.
 * Official BUILD clients mcp-server slash/title leftovers live here (query-only).
 * Official BUILD clients postchain-clients javascript-typescript slash/title leftovers live here (query-only).
 * Official BUILD clients postchain-clients python-client slash/title leftovers live here (query-only).
 * Official BUILD clients postchain-clients kotlin-client slash/title leftovers live here (query-only).
 * Official BUILD clients postchain-clients c-sharp-client slash/title leftovers live here (query-only).
 * Official BUILD clients postchain-clients rust-client slash/title leftovers live here (query-only).
 * Official BUILD clients postchain-clients go-client slash/title leftovers live here (query-only).
 * Official BUILD clients postchain-rest-api slash/title leftovers live here (query-only).
 * Official BUILD clients overview slash/title leftovers live here (query-only).
 * Official BUILD clients/postchain-clients/javascript-typescript/hello-world-quickstart leftovers encoded as JS_QUICKSTART_INDEX_* (query-only).
 * Official BUILD clients/postchain-clients/javascript-typescript/reference leftovers encoded as JS_REFERENCE_INDEX_* (query-only).
 * Official BUILD clients/postchain-clients/c-sharp-client leftovers encoded as CSHARP_CLIENT_INDEX_* (query-only).
 * Official BUILD clients/postchain-clients/go-client leftovers encoded as GO_CLIENT_INDEX_* (query-only).
 * Official BUILD clients/postchain-clients/rust-client leftovers encoded as RUST_CLIENT_INDEX_* (query-only).
 * Official BUILD clients/postchain-clients/kotlin-client leftovers encoded as KOTLIN_CLIENT_INDEX_* (query-only).
 * Official BUILD clients/postchain-clients/python-client leftovers encoded as PYTHON_CLIENT_INDEX_* (query-only).
 * Official get-started/about/dapp leftovers encoded as GET_STARTED_DAPP_INDEX_* (query-only).
 * Official get-started/about/chromia-vs-evm leftovers encoded as GET_STARTED_CHROMIA_VS_EVM_INDEX_* (query-only).
 * Official get-started/about/protocols/gtx leftovers encoded as GET_STARTED_GTX_INDEX_* (query-only).
 * Official get-started/about/architecture/chains/system-anchoring-chain leftovers encoded as GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_* (query-only).
 * Official get-started/about/staking/user-delegation leftovers encoded as GET_STARTED_USER_DELEGATION_INDEX_* (query-only).
 * Official get-started/about/protocols/iccf leftovers encoded as GET_STARTED_ICCF_PROTOCOL_INDEX_* (query-only).
 * Official ecosystem/governance/getting-started/governance-structure/user-proposal-flows leftovers encoded as ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_* (query-only).
 * Official ecosystem/governance/governance-voting-process/voting-flow leftovers encoded as ECOSYSTEM_GOV_VOTING_FLOW_INDEX_* (query-only).
 * Official ecosystem/bridge/bridge-client/client leftovers encoded as ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_* (query-only).
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
    const val MCP_OFFICIAL_PROD_SSE = "https://mcp.chromia.dev/sse"
    const val MCP_OFFICIAL_LOCAL_SSE = "http://127.0.0.1:3001/sse"
    const val MCP_LIVE_SSE_PATH = "/"
    const val MCP_OFFICIAL_SSE_PATH = "/sse"
    const val MCP_HEALTH_PATH = "/health"
    const val MCP_HEALTH_STATUS = "healthy"
    const val MCP_HEALTH_VERSION = "0.2.2"
    const val MCP_OFFICIAL_HOST_STATUS = "502"
    const val FILEHUB_GATEWAY_HOST = "filehub-gw.chromia.com"
    const val GO_MODULE = "gitlab.com/chromaway/ft4-go-client"
    const val GO_OFFICIAL_NODE = "https://node1.example.com"
    const val RUST_CRATE = "postchain-client"
    const val RUST_CRATE_VERSION = "0.0.3"
    const val RUST_OFFICIAL_DECIMAL_TYPO = "BigDecima"
    const val RUST_SOURCE_DECIMAL = "BigDecimal"
    const val RUST_OFFICIAL_ERROR_ARM = "Error(error: RestError)"
    const val RUST_SOURCE_ERR_ARM = "Err(error: RestError)"
    const val RUST_OFFICIAL_ERR_INCOMPLETE = "Err(error: )"
    const val RUST_OFFICIAL_ERR_IDENT = "err"
    const val RUST_SOURCE_ERR_IDENT = "error"
    const val RUST_OFFICIAL_QUERY_ARGS_REF = "query_arguments_ref"
    const val RUST_SOURCE_QUERY_ARGS = "query_arguments"
    const val RUST_OFFICIAL_BYTEARRAY = "Params:: ByteArray"
    const val RUST_SOURCE_BYTEARRAY = "Params::ByteArray"
    const val RUST_OFFICIAL_DECIMAL_SERDE = "serialize_bigint"
    const val RUST_SOURCE_DECIMAL_SERDE = "serialize_bigdecimal"
    const val RUST_OFFICIAL_RESTCLIENT_LIFETIME = "RestClient<'_>"
    const val RUST_OFFICIAL_RESTCLIENT_HTML_ENTITY = "RestClient&lt;'_>"
    const val REST_URL_SLASH = "https://docs.chromia.com/build/clients/postchain-rest-api/"
    const val REST_TITLE = "Postchain Rest API"
    const val POSTCHAIN_REST_API_INDEX_URL = REST_URL
    const val POSTCHAIN_REST_API_INDEX_URL_SLASH = REST_URL_SLASH
    const val POSTCHAIN_REST_API_INDEX_TITLE = REST_TITLE
    const val REACT_NPM = "@chromia/react"
    const val REACT_OFFICIAL_POOL = "BLOCKCHAIN_URL"

    fun csharpQuery(): String = """
        var client = await ChromiaClient.Create("http://localhost:7740", blockchainRID);
        var response = client.Query<string>("get_city", ("zip", 22222));
    """.trimIndent() + "\n"

    fun csharpCreateFromIid(): String =
        """var client = await ChromiaClient.Create("http://localhost:7740", 0);""" + "\n"

    fun officialCsharpCreateFromDirectory(): String =
        """var client = await ChromiaClient.CreateFromDirectory("http://localhost:7750", blockchainRID);""" + "\n"

    fun officialPostchainClientsIntro(): String = """
        Postchain clients offer libraries for interacting with a blockchain using JavaScript/TypeScript, Kotlin, C#, Rust, or Python. These libraries enable easy transaction sending and data retrieval from Rell blockchain nodes, simplifying the development of decentralized applications.

        Additionally, Chromia exposes REST endpoints directly in the browser, enabling developers to test functionality, query the blockchain, and submit transactions.
    """.trimIndent() + "\n"

    fun officialReactKitIntro(): String = """
        The Chromia React Kit streamlines the integration of React-based front-end applications with the Chromia blockchain. It offers a suite of tools and react custom hooks that enable developers to build decentralized applications effortlessly (dapps), simplifying and managing blockchain interactions.
    """.trimIndent() + "\n"

    fun officialFilehubClientIntro(): String = """
        TypeScript. Persisting and reading files on the Chromia blockchain. Filehub utilises @chromia/ft4 and postchain-client as peer dependency.
    """.trimIndent() + "\n"

    fun officialBridgeClientIntro(): String = """
        TypeScript library with utilities for interacting with the Chromia token bridge
    """.trimIndent() + "\n"

    fun officialOverviewIntro(): String = """
        The Clients section highlights tools and libraries that enable integration of applications with the Chromia blockchain.
    """.trimIndent() + "\n"

    fun officialMcpServerIntro(): String = """
        A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps through the Chromia Explorer.
    """.trimIndent() + "\n"

    fun officialJsTsIndexIntro(): String = """
        The JavaScript/TypeScript client library provides a comprehensive set of functions and utilities for interacting with Chromia blockchain. It allows you to send transactions, retrieve information from blockchain nodes running Rell, and build decentralized applications with ease.
    """.trimIndent() + "\n"

    fun officialPythonIndexIntro(): String = """
        A Python client library for interacting with Postchain nodes on Chromia blockchain networks. This library provides an interface for creating, signing, and sending transactions, as well as querying the blockchain, with full async support.
    """.trimIndent() + "\n"

    fun officialKotlinIndexIntro(): String = """
        The Kotlin client library, postchain-client, provides the capability for interacting with a blockchain from a client app written in Kotlin or Java. With this library, you can easily send transactions and retrieve queries from a Rell blockchain node.
    """.trimIndent() + "\n"

    fun officialCsharpIndexIntro(): String = """
        The C# client provides functionality for interacting with a blockchain using C#. It allows you to send transactions and retrieve information from a blockchain node running Rell. It can be integrated into your C# projects through the NuGet package or directly referencing the DLL files. Additionally, specific instructions apply if you're working with Unity or Unity WebGL.
    """.trimIndent() + "\n"

    fun officialRustIndexIntro(): String = """
        The Rust client is used for interacting with the Chromia blockchain deployed to a Postchain single node (manual mode) or multi-nodes managed by the Directory Chain (managed mode).

        This library provides functionality for executing queries, creating and signing transactions, and managing blockchain operations.
    """.trimIndent() + "\n"

    fun officialFt4ClientIndexIntro(): String = """
        The FT4 client is written in TypeScript. It is made for easier access to the backend operations and queries and ready to be used on the frontend (or a js backend).
    """.trimIndent() + "\n"

    fun officialGoIndexIntro(): String = """
        The Go client offers a range of built-in functions and utilities designed to simplify interaction with decentralized applications (dApps) created using the Postchain blockchain framework, commonly referred to as Chromia.

        The Go client library provides tools for interacting with the Chromia blockchain platform. It includes functionality for:

        - Serializing and deserializing data using the GTV (Generic Transfer Value) format.
        - Creating, signing, and submitting transactions using the GTX (Generic Transaction) format.
        - Computing Merkle tree hashes for data verification.
        - Communicating with Postchain nodes via REST API.
    """.trimIndent() + "\n"

    fun officialPostchainRestApiIndexIntro(): String = """
        The Postchain Rest API, enables users to perform HTTP-based interactions with the blockchain to access vital information. This includes querying transaction records, inspecting block data, reviewing the blockchain’s current state, fetching configuration settings, and other related details.
    """.trimIndent() + "\n"

    fun officialCsharpDirectoryCreate(): String =
        """var client3 = await ChromiaClient.Create(new() {"http://localhost:7750", "http://localhost:7751"}, 0);""" + "\n"

    fun officialCsharpQueryParams(): String = """
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

    fun officialRustStatus(): String =
        """let status = client.get_transaction_status("<blockchain RID>", &tx_rid).await?;""" + "\n"

    fun officialRustQueryError(): String = """
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
        "JS/TS reference dummy Buffer.alloc keys / secp256k1 / newSignatureProvider / sample 64-hex / signAndSend",
        "BUILD Filehub / bridge client pages print no package id (pages do)",
        "FilehubAdministrator / registerFilechain / enable-disable Filechain / payments writes",
        "Filehub storeFile write",
        "MCP explorer-dump sample BRIDs",
        "bridge deposit / withdraw / mass-exit / allowToken / setBlockchainRid / registerAccount writes",
        "iccf_proof write operations"
    )

    fun notes(): String = """
        Chromia CLI $CLI_SERIES official language-client / REST query-only help. Java 21+, Postgres 16+.
        Clients overview: $OVERVIEW_URL
        Official BUILD clients overview ($OVERVIEW_URL 307 $OVERVIEW_URL_SLASH 200 $OVERVIEW_TITLE): intro The Clients section highlights tools and libraries lists Postchain clients JavaScript(JS)/TypeScript(TS), Kotlin, C#, Rust, Python, Go other tools Chromia React Kit, Bridge client, FT4 client, Filehub client skip signed txs no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients overview ($OVERVIEW_INDEX_URL 307 $OVERVIEW_INDEX_URL_SLASH 200 $OVERVIEW_INDEX_TITLE): intro The Clients section highlights tools and libraries Available Postchain clients JavaScript(JS)/TypeScript(TS), Kotlin, C#, Rust, Python, Go Other tools child cards $OVERVIEW_INDEX_CARD_REST $OVERVIEW_INDEX_CARD_REACT $OVERVIEW_INDEX_CARD_POSTCHAIN $OVERVIEW_INDEX_CARD_BRIDGE $OVERVIEW_INDEX_CARD_FT4 $OVERVIEW_INDEX_CARD_FILEHUB $OVERVIEW_INDEX_CARD_MCP Work in progress Additional resources FT4 documentation Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients postchain-clients ($POSTCHAIN_CLIENTS_URL 307 $POSTCHAIN_CLIENTS_URL_SLASH 200 $POSTCHAIN_CLIENTS_TITLE): intro $POSTCHAIN_CLIENTS_INTRO_LANGS omits Go child cards $POSTCHAIN_CLIENTS_CARD_JS $POSTCHAIN_CLIENTS_CARD_JS_URL $POSTCHAIN_CLIENTS_CARD_KOTLIN $POSTCHAIN_CLIENTS_CARD_CSHARP $POSTCHAIN_CLIENTS_CARD_RUST $POSTCHAIN_CLIENTS_CARD_PYTHON $POSTCHAIN_CLIENTS_CARD_GO REST endpoints browser query skip signed txs no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients postchain-clients ($POSTCHAIN_CLIENTS_INDEX_URL 307 $POSTCHAIN_CLIENTS_INDEX_URL_SLASH 200 $POSTCHAIN_CLIENTS_INDEX_TITLE): intro Postchain clients offer libraries $POSTCHAIN_CLIENTS_INTRO_LANGS omits Go child cards $POSTCHAIN_CLIENTS_CARD_JS $POSTCHAIN_CLIENTS_CARD_JS_URL $POSTCHAIN_CLIENTS_CARD_KOTLIN $POSTCHAIN_CLIENTS_CARD_CSHARP $POSTCHAIN_CLIENTS_CARD_RUST $POSTCHAIN_CLIENTS_CARD_PYTHON $POSTCHAIN_CLIENTS_CARD_GO REST endpoints browser query Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD deploy-frontend-dapp (200): $DEPLOY_FRONTEND_URL on-chain frontend key webStatic and local URL $WEB_STATIC_LOCAL_URL. Do not invent siblings. See chr_deploy_help.
        JS/TS, Kotlin, Python, and FT4 local createConnection live on chr_generate_client_help.
        Official BUILD kotlin-client (200): ${ChrGenerateClientHelp.KOTLIN_CLIENT_URL} EndpointPool.singleUrl ${ChrGenerateClientHelp.KOTLIN_LOCAL_ENDPOINT} query hello_world StandardChromiaClient without sample hex ${ChrGenerateClientHelp.OUTDATED_BANNER}. REAL bug: Chromia client Gradle still prints ${ChrGenerateClientHelp.MAVEN_POSTCHAIN} Maven ${ChrGenerateClientHelp.MAVEN_CHROMIA} wins. Skip printed sample keys set_name awaitAnchoredTx sample 64-hex.
        Official BUILD python-client (200): ${ChrGenerateClientHelp.PYTHON_CLIENT_URL} Python ${ChrGenerateClientHelp.PYTHON_MIN} pip ${ChrGenerateClientHelp.PIP_POSTCHAIN} query get_collections get_all_books get_all_reviews_for_book POSTCHAIN_TEST_NODE BLOCKCHAIN_TEST_RID YOUR_BLOCKCHAIN_RID BlockchainRID placeholder wins. Skip PRIV_KEY coincurve sign_transaction send_transaction.
        Official BUILD clients postchain-clients javascript-typescript hello-world-quickstart ($JS_QUICKSTART_INDEX_URL 307 $JS_QUICKSTART_INDEX_URL_SLASH 200 $JS_QUICKSTART_INDEX_TITLE): intro This quickstart guide Node.js 18+ object my_name query hello_world() Hello %s!.format(my_name.name) client.query("hello_world") default Hello World! run-dapp-cli $RUN_DAPP_CLI_URL chromia. yml space chromia.yml wins REAL bug page still prints DEV-ONLY sample keys and signAndSendUniqueTransaction set_name query-only wins Query-only skip printed sample keys set_name tx keygen signAndSendUniqueTransaction sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD JS/TS reference (200): $JS_REFERENCE_URL Directory Chain Iid ${ChrGenerateClientHelp.DIRECTORY_CHAIN_IID} succefull failover defaults ${ChrGenerateClientHelp.FAILOVER_DEFAULT_STRATEGY} attemptsPerEndpoint ${ChrGenerateClientHelp.FAILOVER_DEFAULT_ATTEMPTS} attemptInterval ${ChrGenerateClientHelp.FAILOVER_DEFAULT_INTERVAL_MS} unreachableDuration ${ChrGenerateClientHelp.FAILOVER_DEFAULT_UNREACHABLE_MS} sticky useStickyNode prose ${ChrGenerateClientHelp.OFFICIAL_PROSE_BRID} source ${ChrGenerateClientHelp.SOURCE_BRID_SETTING} wins. REAL bug: ${ChrGenerateClientHelp.OFFICIAL_STRATEGY_TYPO} ${ChrGenerateClientHelp.OFFICIAL_ABORT_TYPO} three failover strategies lists four source ${ChrGenerateClientHelp.SOURCE_FAILOVER_ABORT} wins BlockchainRID placeholder wins. Skip dummy Buffer.alloc secp256k1 newSignatureProvider sample 64-hex signAndSend.
        Official BUILD clients postchain-clients javascript-typescript reference ($JS_REFERENCE_INDEX_URL 307 $JS_REFERENCE_INDEX_URL_SLASH 200 $JS_REFERENCE_INDEX_TITLE): Directory Chain Iid ${ChrGenerateClientHelp.DIRECTORY_CHAIN_IID} succefull failover defaults ${ChrGenerateClientHelp.FAILOVER_DEFAULT_STRATEGY} attemptsPerEndpoint ${ChrGenerateClientHelp.FAILOVER_DEFAULT_ATTEMPTS} attemptInterval ${ChrGenerateClientHelp.FAILOVER_DEFAULT_INTERVAL_MS} unreachableDuration ${ChrGenerateClientHelp.FAILOVER_DEFAULT_UNREACHABLE_MS} sticky useStickyNode prose ${ChrGenerateClientHelp.OFFICIAL_PROSE_BRID} source ${ChrGenerateClientHelp.SOURCE_BRID_SETTING} wins REAL bug ${ChrGenerateClientHelp.OFFICIAL_STRATEGY_TYPO} ${ChrGenerateClientHelp.OFFICIAL_ABORT_TYPO} three failover strategies lists four source ${ChrGenerateClientHelp.SOURCE_FAILOVER_ABORT} wins BlockchainRID placeholder wins Query-only skip dummy Buffer.alloc secp256k1 newSignatureProvider signAndSend signed txs sample admin pubkey keygen no sample keys no invented 64-hex <BlockchainRID>.
        C# ($CSHARP_URL): ChromiaClient.Create(url, blockchainRID) or Create(url, iid). Query:
        client.Query<string>("get_city", ("zip", 22222)) IGtvSerializable JsonProperty QueryParams Zip 22222.
        Official page says NuGet package or DLL / Unity Plugins; it does not print a package id.
        Directory discovery: ChromiaClient.CreateFromDirectory(systemUrl, rid|iid). Official local
        Directory example host is http://localhost:7750 $CSHARP_DIRECTORY_HOST ${ChrGenerateClientHelp.OUTDATED_BANNER}. REAL bug: directory pool still prints Create not CreateFromDirectory BlockchainRID placeholder wins. Skip sample 64-hex SignatureProvider SendTransaction SendUniqueTransaction.
        Go ($GO_URL): go get $GO_MODULE NewClient $GO_OFFICIAL_NODE http://localhost:7740 wins GetBlockchainRID 123. postchain.NewClient([]*url.URL{nodeURL}).
        Query: client.Query(blockchainRID, name, gtv.DictValue). Also GetBlockchainRID(iid),
        GetFeatures, DetectMerkleHashVersion (use merkle HashVersion2; version 1 has a hash-collision bug).
        Read-only retrieve: GetTransaction, GetTransactionInfo, GetConfirmationProof, BlockAtHeight, BlockByRID.
        Rust ($RUST_URL): crate $RUST_CRATE = "$RUST_CRATE_VERSION". RestClient { node_url, request_time_out: 30,
        poll_attemps: 5, poll_attemp_interval_time: 5 }. client.query(brid, None, query_type, None, args).
        Official rust page prints $RUST_OFFICIAL_RESTCLIENT_LIFETIME (HTML entity $RUST_OFFICIAL_RESTCLIENT_HTML_ENTITY).
        Official field names poll_attemps / poll_attemp_interval_time are as printed get_transaction_status read-only. REAL bug: Params table still prints $RUST_OFFICIAL_DECIMAL_TYPO $RUST_SOURCE_DECIMAL wins query match still prints $RUST_OFFICIAL_ERROR_ARM $RUST_SOURCE_ERR_ARM wins $RUST_OFFICIAL_ERR_INCOMPLETE $RUST_OFFICIAL_ERR_IDENT $RUST_SOURCE_ERR_IDENT wins $RUST_OFFICIAL_QUERY_ARGS_REF $RUST_SOURCE_QUERY_ARGS wins $RUST_OFFICIAL_BYTEARRAY $RUST_SOURCE_BYTEARRAY wins decimal serde still prints $RUST_OFFICIAL_DECIMAL_SERDE $RUST_SOURCE_DECIMAL_SERDE wins. Skip printed sample keys Transaction::new sign send_transaction.
        React Kit ($REACT_URL): $REACT_NPM ${ChrGenerateClientHelp.OUTDATED_BANNER} $REACT_OFFICIAL_POOL http://localhost:7740 wins SWR QueryOrOperationType. createChromiaHooks returns useChromiaQuery,
        useChromiaImmutableQuery, useChromiaInfiniteQuery. Official FailoverStrategy.AbortOnError.
        useFtQuery({ queryName, queryParams, accountId }) is a read hook; skip FtProvider EVM keystore.
        Official BUILD clients react-kit ($REACT_URL 307 $REACT_URL_SLASH 200 $REACT_TITLE): intro The Chromia React Kit streamlines $REACT_NPM SWR createChromiaHooks useChromiaQuery useChromiaImmutableQuery useChromiaInfiniteQuery useFtQuery skip FtProvider EVM keystore useFileHubImage signed txs no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients react-kit ($REACT_KIT_INDEX_URL 307 $REACT_KIT_INDEX_URL_SLASH 200 $REACT_KIT_INDEX_TITLE): intro The Chromia React Kit streamlines Why use Getting started Install $REACT_NPM Core features Custom hooks createChromiaHooks useChromiaQuery useChromiaImmutableQuery useChromiaInfiniteQuery useFtQuery SWR Additional resources FT4 documentation ${ChrGenerateClientHelp.OUTDATED_BANNER} Query-only skip FtProvider EVM keystore useFileHubImage signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        REST ($REST_URL 307 $REST_URL_SLASH 200 $REST_TITLE): no install. Official groups: Block, Blockchain, Configuration, Error,
        Experimental, Node, Query, Transaction. Paths below are the official table path templates.
        Cookbook create-rell-dapp GET: curl -X GET 'localhost:7740/query/<BlockchainRID>?type=hello_world'
        ($COOKBOOK_CREATE_RELL_URL). Default REST port 7740. Do not invent a BRID.
        Official BUILD clients postchain-rest-api ($POSTCHAIN_REST_API_INDEX_URL 307 $POSTCHAIN_REST_API_INDEX_URL_SLASH 200 $POSTCHAIN_REST_API_INDEX_TITLE): intro The Postchain Rest API, enables users Postchain API usage no install browser groups Block Blockchain Configuration Error Experimental Node Query Transaction ${ChrGenerateClientHelp.OUTDATED_BANNER} Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients ft4-client ($FT4_CLIENT_URL 307 $FT4_CLIENT_URL_SLASH 200 $FT4_CLIENT_TITLE): TypeScript $FT4_CLIENT_NPM backend operations and queries frontend or js backend $FT4_CLIENT_INTRO_URL FT4 documentation query-only skip signed txs no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients ft4-client ($FT4_CLIENT_INDEX_URL 307 $FT4_CLIENT_INDEX_URL_SLASH 200 $FT4_CLIENT_INDEX_TITLE): intro The FT4 client is written in TypeScript $FT4_CLIENT_NPM backend operations and queries frontend or js backend $FT4_CLIENT_INTRO_URL FT4 documentation Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients filehub-client ($FILEHUB_CLIENT_URL_NO_SLASH 307 $FILEHUB_CLIENT_URL 200 $FILEHUB_CLIENT_TITLE): intro TypeScript persisting reading files Chromia blockchain Filehub utilises @chromia/ft4 postchain-client peer dependency Filehub documentation $FILEHUB_ECO_URL skip persist admin writes FilehubAdministrator storeFile signed txs no sample keys no invented 64-hex <BlockchainRID>. BUILD page prints no package id.
        Official BUILD clients filehub-client ($FILEHUB_CLIENT_INDEX_URL 307 $FILEHUB_CLIENT_INDEX_URL_SLASH 200 $FILEHUB_CLIENT_INDEX_TITLE): intro The Filehub client is written in TypeScript persisting reading files Chromia blockchain Filehub utilises @chromia/ft4 postchain-client peer dependency Filehub documentation $FILEHUB_ECO_URL Query-only skip persist admin writes FilehubAdministrator storeFile signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>. BUILD page prints no package id.
        Official Filehub work (200): $FILEHUB_WORK_URL. Official BUILD $FILEHUB_WORK_BUILD_404 and $FILEHUB_BUILD_INDEX_404 are 404.
        Official Filehub work read-only: require("filehub"); new Filehub({ directoryNodeUrlPool, blockchainRid }); filehub.getFile(fileHash).
        Official placeholders DIRECTORY_NODE_URL_POOL / FILEHUB_BLOCKCHAIN_RID only. Official Filehub work / overview / vault-listing print 0.10 USD per MB perpetual. Official Filehub gateway host $FILEHUB_GATEWAY_HOST (vault-listing); skip sample 64-hex paths. Skip FilehubAdministrator / registerFilechain / storeFile / payments writes.
        Official BUILD clients bridge-client ($BRIDGE_CLIENT_URL_NO_SLASH 307 $BRIDGE_CLIENT_URL 200 $BRIDGE_CLIENT_TITLE): intro TypeScript library utilities interacting Chromia token bridge pointer bridge client documentation $BRIDGE_ECO_URL skip signed txs admin writes deposit withdraw mass-exit allowToken setBlockchainRid registerAccount no sample keys no invented 64-hex <BlockchainRID>. BUILD page prints no package id.
        Official BUILD clients bridge-client ($BRIDGE_CLIENT_INDEX_URL 307 $BRIDGE_CLIENT_INDEX_URL_SLASH 200 $BRIDGE_CLIENT_INDEX_TITLE): intro The Chromia bridge client is a TypeScript library utilities interacting Chromia token bridge pointer bridge client documentation $BRIDGE_ECO_URL Query-only skip signed txs admin writes deposit withdraw mass-exit allowToken setBlockchainRid registerAccount sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>. BUILD page prints no package id.
        TypeScript library; BUILD page prints no package id. Official ecosystem configure (200): $BRIDGE_CONFIGURE_URL and work-with-client (200): $BRIDGE_WORK_URL print package id $BRIDGE_NPM. Quote eif.hbridge.bridge_mode as printed (no invented field name; see chromia_integrations_help). Official read-only checkAllowance. Official work-with-client prints YOU_NODE_URL_POOL as printed. Skip deposit / withdraw / mass-exit / allowToken / setBlockchainRid / registerAccount writes.
        Related official ecosystem overviews: $FILEHUB_ECO_URL and $BRIDGE_ECO_URL.
        Related official ICCF protocol (200 with trailing slash): $ICCF_PROTOCOL_URL
        Docs print IccfGtxModule; source pin is net.postchain.d1.iccf.IccfGTXModule plus library-chain com.chromia.iccf 1.90.1;
        official-but-stale FT4-setup git directory-chain tag is 1.87.0. See ft4_module_args. Skip iccf_proof writes.
        Official Filehub configure page prints package id filehub from configure AND work.
        Official BUILD MCP server (200): $MCP_SERVER_URL. Official page key $MCP_OFFICIAL_PAGE_KEY vs fat JAR $MCP_FAT_JAR_NAME. Official prod $MCP_PROD_URL $MCP_OFFICIAL_PROD_SSE local README $MCP_OFFICIAL_LOCAL_SSE this-tree live GET / is the SSE endpoint GET /sse 404 /health {status:healthy, server:$MCP_FAT_JAR_NAME, version:$MCP_HEALTH_VERSION} host currently $MCP_OFFICIAL_HOST_STATUS. Official local SSE $MCP_LOCAL_SSE via $MCP_LOCAL_GRADLE from $MCP_REPO. Official LSP $LSP_MCP_NPM with optional Rell $LSP_MCP_RELL; config key lsp-mcp. Official ChatGPT auth: No authentication. Skip explorer-dump sample BRIDs.
        Official BUILD clients mcp-server ($MCP_SERVER_INDEX_URL 307 $MCP_SERVER_INDEX_URL_SLASH 200 $MCP_SERVER_INDEX_TITLE): intro A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps through the Chromia Explorer Overview Documentation Tools Setup Claude Rell LSP Integration Usage Examples Networks Query-only skip signed txs explorer-dump sample BRIDs no sample keys no invented 64-hex sample admin pubkey do not invent a hosted URL host currently $MCP_OFFICIAL_HOST_STATUS.
        Official BUILD clients postchain-clients javascript-typescript ($JS_TS_INDEX_URL 307 $JS_TS_INDEX_URL_SLASH 200 $JS_TS_INDEX_TITLE): intro The JavaScript/TypeScript client library provides a comprehensive set of functions Getting started Hello World Quickstart $JS_QUICKSTART_URL Installation postchain-client Query Execution Multiple Node Support TypeScript Support Automatic Discovery Client Reference $JS_REFERENCE_URL Chromia React Kit Client libraries overview Cookbook examples React/Rell Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients postchain-clients python-client ($PYTHON_INDEX_URL 307 $PYTHON_INDEX_URL_SLASH 200 $PYTHON_INDEX_TITLE): intro A Python client library for interacting with Postchain nodes Features aiohttp Prerequisites Python ${ChrGenerateClientHelp.PYTHON_MIN} Installation pip ${ChrGenerateClientHelp.PIP_POSTCHAIN} Configuration ${ChrGenerateClientHelp.PYTHON_ENV_NODE} ${ChrGenerateClientHelp.PYTHON_ENV_RID} Quick Start BlockchainClient NetworkSettings get_collections Queries get_all_books get_all_reviews_for_book Development Running Tests Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients/postchain-clients/python-client ($PYTHON_CLIENT_INDEX_URL 307 $PYTHON_CLIENT_INDEX_URL_SLASH 200 $PYTHON_CLIENT_INDEX_TITLE): Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients postchain-clients kotlin-client ($KOTLIN_INDEX_URL 307 $KOTLIN_INDEX_URL_SLASH 200 $KOTLIN_INDEX_TITLE): intro The Kotlin client library, postchain-client Postchain-client Installation ${ChrGenerateClientHelp.MAVEN_POSTCHAIN} Initializing the client EndpointPool.singleUrl ${ChrGenerateClientHelp.KOTLIN_LOCAL_ENDPOINT} Queries hello_world Chromia client ${ChrGenerateClientHelp.KOTLIN_STANDARD_CLIENT} Gradle still prints ${ChrGenerateClientHelp.MAVEN_POSTCHAIN} Maven ${ChrGenerateClientHelp.MAVEN_CHROMIA} wins FT4 Client ${ChrGenerateClientHelp.MAVEN_FT4} Features Authentication Descriptors Asset Management getAssetBalance ${ChrGenerateClientHelp.OUTDATED_BANNER} Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients/postchain-clients/kotlin-client ($KOTLIN_CLIENT_INDEX_URL 307 $KOTLIN_CLIENT_INDEX_URL_SLASH 200 $KOTLIN_CLIENT_INDEX_TITLE): Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients postchain-clients c-sharp-client ($CSHARP_INDEX_URL 307 $CSHARP_INDEX_URL_SLASH 200 $CSHARP_INDEX_TITLE): intro The C# client provides functionality Installation Native Unity Importing into Unity Unity WebGL Injecting custom transport UnityTransport SetTransport NuGet DLL page prints no package id Initializing the client ChromiaClient.Create CreateFromDirectory $CSHARP_DIRECTORY_HOST Queries get_city IGtvSerializable JsonProperty Error handling ChromiaException TransportException ${ChrGenerateClientHelp.OUTDATED_BANNER} directory pool still prints Create not CreateFromDirectory Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients/postchain-clients/c-sharp-client ($CSHARP_CLIENT_INDEX_URL 307 $CSHARP_CLIENT_INDEX_URL_SLASH 200 $CSHARP_CLIENT_INDEX_TITLE): Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients/postchain-clients/go-client ($GO_CLIENT_INDEX_URL 307 $GO_CLIENT_INDEX_URL_SLASH 200 $GO_CLIENT_INDEX_TITLE): Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients/postchain-clients/rust-client ($RUST_CLIENT_INDEX_URL 307 $RUST_CLIENT_INDEX_URL_SLASH 200 $RUST_CLIENT_INDEX_TITLE): Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients postchain-clients rust-client ($RUST_INDEX_URL 307 $RUST_INDEX_URL_SLASH 200 $RUST_INDEX_TITLE): intro The Rust client is used for interacting Installation Cargo.toml $RUST_CRATE $RUST_CRATE_VERSION tokio serde serde_json Initializing the client RestClient Queries client.query Params Error handling RestResponse RestError $RUST_OFFICIAL_ERROR_ARM $RUST_SOURCE_ERR_ARM wins $RUST_OFFICIAL_ERR_INCOMPLETE $RUST_OFFICIAL_ERR_IDENT $RUST_SOURCE_ERR_IDENT wins $RUST_OFFICIAL_QUERY_ARGS_REF $RUST_SOURCE_QUERY_ARGS wins Using serde for serialization $RUST_OFFICIAL_DECIMAL_SERDE $RUST_SOURCE_DECIMAL_SERDE wins Logging tracing Parameter types $RUST_OFFICIAL_DECIMAL_TYPO $RUST_SOURCE_DECIMAL wins $RUST_OFFICIAL_BYTEARRAY $RUST_SOURCE_BYTEARRAY wins $RUST_OFFICIAL_RESTCLIENT_LIFETIME $RUST_OFFICIAL_RESTCLIENT_HTML_ENTITY Examples book-review ${ChrGenerateClientHelp.OUTDATED_BANNER} Params table still prints $RUST_OFFICIAL_DECIMAL_TYPO $RUST_SOURCE_DECIMAL wins Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official BUILD clients postchain-clients go-client ($GO_INDEX_URL 307 $GO_INDEX_URL_SLASH 200 $GO_INDEX_TITLE): intro The Go client offers a range of built-in functions Installation go get $GO_MODULE Generic Transfer Value GTV Null ByteArray String Integer Dict Array BigInteger Merkle HashVersion2 version 1 hash-collision bug Postchain Client NewClient $GO_OFFICIAL_NODE http://localhost:7740 wins GetBlockchainRID 123 GetFeatures DetectMerkleHashVersion Queries get_account_balance Retrieving Transactions and Blocks GetTransaction GetTransactionInfo GetConfirmationProof BlockAtHeight BlockByRID API Reference Query-only skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official get-started/about/dapp ($GET_STARTED_DAPP_INDEX_URL 307 $GET_STARTED_DAPP_INDEX_URL_SLASH 200 $GET_STARTED_DAPP_INDEX_TITLE): Query-only HELP ONLY skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official get-started/about/chromia-vs-evm ($GET_STARTED_CHROMIA_VS_EVM_INDEX_URL 307 $GET_STARTED_CHROMIA_VS_EVM_INDEX_URL_SLASH 200 $GET_STARTED_CHROMIA_VS_EVM_INDEX_TITLE): Query-only HELP ONLY skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official get-started/about/protocols/gtx ($GET_STARTED_GTX_INDEX_URL 307 $GET_STARTED_GTX_INDEX_URL_SLASH 200 $GET_STARTED_GTX_INDEX_TITLE): Query-only HELP ONLY skip signed txs sample admin pubkey no sample keys no invented 64-hex <BlockchainRID>.
        Official get-started/about/architecture/chains/system-anchoring-chain slash title ($GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_URL 307 $GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_URL_SLASH 200 $GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_TITLE): Query-only HELP ONLY WRITE SKIP Origin parked skip signed txs sample admin pubkey keygen sign recipe no sample keys no invented 64-hex BRIDs <BlockchainRID>.
        Official get-started/about/staking/user-delegation slash title ($GET_STARTED_USER_DELEGATION_INDEX_URL 307 $GET_STARTED_USER_DELEGATION_INDEX_URL_SLASH 200 $GET_STARTED_USER_DELEGATION_INDEX_TITLE): Query-only HELP ONLY WRITE SKIP Origin parked skip signed txs sample admin pubkey keygen sign recipe no sample keys no invented 64-hex BRIDs <BlockchainRID>.
        Official get-started/about/protocols/iccf slash title ($GET_STARTED_ICCF_PROTOCOL_INDEX_URL 307 $GET_STARTED_ICCF_PROTOCOL_INDEX_URL_SLASH 200 $GET_STARTED_ICCF_PROTOCOL_INDEX_TITLE): Query-only HELP ONLY WRITE SKIP Origin parked skip signed txs sample admin pubkey keygen sign recipe no sample keys no invented 64-hex BRIDs <BlockchainRID>.
        Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure/user-proposal-flows INDEX ($ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_URL 307 $ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official ECOSYSTEM ecosystem/governance/governance-voting-process/voting-flow INDEX ($ECOSYSTEM_GOV_VOTING_FLOW_INDEX_URL 307 $ECOSYSTEM_GOV_VOTING_FLOW_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_VOTING_FLOW_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official ECOSYSTEM ecosystem/bridge/bridge-client/client INDEX ($ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_URL 307 $ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_TITLE HELP ONLY WRITE SKIP Query-only skip signed txs deposit withdraw mass-exit sample admin pubkey keygen sign recipe no sample keys no invented 64-hex <BlockchainRID>)
        Official LEARN courses/book-review/book-review-entity/write-queries INDEX ($LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_URL 301 $LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/ft4-demo-app/module-blockchain/account-management INDEX ($LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_URL 301 $LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/my-news-feed/introduction INDEX ($LEARN_NEWS_INTRODUCTION_INDEX_URL 301 $LEARN_NEWS_INTRODUCTION_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_INTRODUCTION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/React INDEX ($LEARN_TAGS_REACT_INDEX_URL 301 $LEARN_TAGS_REACT_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_REACT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
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
        put("overview_index_intro", officialOverviewIntro())
        put("overview_index_card_rest", OVERVIEW_INDEX_CARD_REST)
        put("overview_index_card_react", OVERVIEW_INDEX_CARD_REACT)
        put("overview_index_card_postchain", OVERVIEW_INDEX_CARD_POSTCHAIN)
        put("overview_index_card_bridge", OVERVIEW_INDEX_CARD_BRIDGE)
        put("overview_index_card_ft4", OVERVIEW_INDEX_CARD_FT4)
        put("overview_index_card_filehub", OVERVIEW_INDEX_CARD_FILEHUB)
        put("overview_index_card_mcp", OVERVIEW_INDEX_CARD_MCP)
        put("overview_intro", officialOverviewIntro())
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
                put("filehub_npm_source", "configure and work print filehub; BUILD client page prints none")
                put("bridge_npm", BRIDGE_NPM)
                put("bridge_npm_source", "ecosystem configure and work-with-client print @chromia/bridge-client; BUILD bridge client page prints none")
                put("ft4_npm", FT4_CLIENT_NPM)
                put("ft4_npm_source", "BUILD clients ft4-client prints @chromia/ft4")
                put("lsp_mcp_npm", LSP_MCP_NPM)
                put("lsp_mcp_rell", LSP_MCP_RELL)
            }
        )
        put("csharp_query", csharpQuery())
        put("csharp_create_iid", csharpCreateFromIid())
        put("csharp_create_directory", officialCsharpCreateFromDirectory())
        put("csharp_official_directory_create", officialCsharpDirectoryCreate())
        put("csharp_directory_host", CSHARP_DIRECTORY_HOST)
        put("csharp_query_params", officialCsharpQueryParams())
        put("go_query", goQuery())
        put("go_official_node", GO_OFFICIAL_NODE)
        put("rust_client", rustClient())
        put("rust_query", rustQuery())
        put("rust_status", officialRustStatus())
        put("rust_query_error", officialRustQueryError())
        put("rust_official_decimal_typo", RUST_OFFICIAL_DECIMAL_TYPO)
        put("rust_source_decimal", RUST_SOURCE_DECIMAL)
        put("rust_official_error_arm", RUST_OFFICIAL_ERROR_ARM)
        put("rust_source_err_arm", RUST_SOURCE_ERR_ARM)
        put("rust_official_err_incomplete", RUST_OFFICIAL_ERR_INCOMPLETE)
        put("rust_official_err_ident", RUST_OFFICIAL_ERR_IDENT)
        put("rust_source_err_ident", RUST_SOURCE_ERR_IDENT)
        put("rust_official_query_args_ref", RUST_OFFICIAL_QUERY_ARGS_REF)
        put("rust_source_query_args", RUST_SOURCE_QUERY_ARGS)
        put("rust_official_bytearray", RUST_OFFICIAL_BYTEARRAY)
        put("rust_source_bytearray", RUST_SOURCE_BYTEARRAY)
        put("rust_official_decimal_serde", RUST_OFFICIAL_DECIMAL_SERDE)
        put("rust_source_decimal_serde", RUST_SOURCE_DECIMAL_SERDE)
        put("rust_official_restclient_lifetime", RUST_OFFICIAL_RESTCLIENT_LIFETIME)
        put("rust_official_restclient_html_entity", RUST_OFFICIAL_RESTCLIENT_HTML_ENTITY)
        put("rest_url_slash", REST_URL_SLASH)
        put("rest_title", REST_TITLE)
        put("postchain_rest_api_index_docs", POSTCHAIN_REST_API_INDEX_URL)
        put("postchain_rest_api_index_url_slash", POSTCHAIN_REST_API_INDEX_URL_SLASH)
        put("postchain_rest_api_index_title", POSTCHAIN_REST_API_INDEX_TITLE)
        put("postchain_rest_api_index_intro", officialPostchainRestApiIndexIntro())
        put("ft4_client_url", FT4_CLIENT_URL)
        put("ft4_client_url_slash", FT4_CLIENT_URL_SLASH)
        put("ft4_client_title", FT4_CLIENT_TITLE)
        put("ft4_client_intro", FT4_CLIENT_INTRO_URL)
        put("ft4_client_index_docs", FT4_CLIENT_INDEX_URL)
        put("ft4_client_index_url_slash", FT4_CLIENT_INDEX_URL_SLASH)
        put("ft4_client_index_title", FT4_CLIENT_INDEX_TITLE)
        put("ft4_client_index_intro", officialFt4ClientIndexIntro())
        put("postchain_clients_url", POSTCHAIN_CLIENTS_URL)
        put("postchain_clients_url_slash", POSTCHAIN_CLIENTS_URL_SLASH)
        put("postchain_clients_title", POSTCHAIN_CLIENTS_TITLE)
        put("postchain_clients_index_docs", POSTCHAIN_CLIENTS_INDEX_URL)
        put("postchain_clients_index_url_slash", POSTCHAIN_CLIENTS_INDEX_URL_SLASH)
        put("postchain_clients_index_title", POSTCHAIN_CLIENTS_INDEX_TITLE)
        put("postchain_clients_intro_langs", POSTCHAIN_CLIENTS_INTRO_LANGS)
        put("postchain_clients_intro", officialPostchainClientsIntro())
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
        put("react_intro", officialReactKitIntro())
        put("react_hooks", reactHooks())
        put("react_official_pool", REACT_OFFICIAL_POOL)
        put("react_kit_index_docs", REACT_KIT_INDEX_URL)
        put("react_kit_index_url_slash", REACT_KIT_INDEX_URL_SLASH)
        put("react_kit_index_title", REACT_KIT_INDEX_TITLE)
        put("react_kit_index_intro", officialReactKitIntro())
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
        put("filehub_client_intro", officialFilehubClientIntro())
        put("filehub_client_index_docs", FILEHUB_CLIENT_INDEX_URL)
        put("filehub_client_index_url_slash", FILEHUB_CLIENT_INDEX_URL_SLASH)
        put("filehub_client_index_title", FILEHUB_CLIENT_INDEX_TITLE)
        put("filehub_client_index_intro", officialFilehubClientIntro())
        put(
            "filehub_get_file", filehubGetFile())
        put("filehub_construct", filehubConstruct())
        put("filehub_gateway_host", FILEHUB_GATEWAY_HOST)
        put("filehub_cost", "printed 0.10 USD per MB perpetual")
        put("bridge_client_url", BRIDGE_CLIENT_URL_NO_SLASH)
        put("bridge_client_url_slash", BRIDGE_CLIENT_URL)
        put("bridge_client_title", BRIDGE_CLIENT_TITLE)
        put("bridge_client_intro", officialBridgeClientIntro())
        put("bridge_client_index_docs", BRIDGE_CLIENT_INDEX_URL)
        put("bridge_client_index_url_slash", BRIDGE_CLIENT_INDEX_URL_SLASH)
        put("bridge_client_index_title", BRIDGE_CLIENT_INDEX_TITLE)
        put("bridge_client_index_intro", officialBridgeClientIntro())
        put("bridge_check_allowance", bridgeCheckAllowance())
        put("bridge_client_init", bridgeClientInit())
        put("mcp_official_page_key", MCP_OFFICIAL_PAGE_KEY)
        put("mcp_fat_jar_server_name", MCP_FAT_JAR_NAME)
        put("mcp_prod_url", MCP_PROD_URL)
        put("mcp_local_sse", MCP_LOCAL_SSE)
        put("mcp_local_gradle", MCP_LOCAL_GRADLE)
        put("mcp_repo", MCP_REPO)
        put("mcp_official_prod_sse", MCP_OFFICIAL_PROD_SSE)
        put("mcp_official_local_sse", MCP_OFFICIAL_LOCAL_SSE)
        put("mcp_live_sse_path", MCP_LIVE_SSE_PATH)
        put("mcp_official_sse_path", MCP_OFFICIAL_SSE_PATH)
        put("mcp_health_path", MCP_HEALTH_PATH)
        put("mcp_health_status", MCP_HEALTH_STATUS)
        put("mcp_health_version", MCP_HEALTH_VERSION)
        put("mcp_official_host_status", MCP_OFFICIAL_HOST_STATUS)
        put("mcp_prod_config", mcpProdConfig())
        put("mcp_local_config", mcpLocalConfig())
        put("mcp_lsp_config", mcpLspConfig())
        put("mcp_server_index_docs", MCP_SERVER_INDEX_URL)
        put("mcp_server_index_url_slash", MCP_SERVER_INDEX_URL_SLASH)
        put("mcp_server_index_title", MCP_SERVER_INDEX_TITLE)
        put("mcp_server_index_intro", officialMcpServerIntro())
        put("js_ts_index_docs", JS_TS_INDEX_URL)
        put("js_ts_index_url_slash", JS_TS_INDEX_URL_SLASH)
        put("js_ts_index_title", JS_TS_INDEX_TITLE)
        put("js_ts_index_intro", officialJsTsIndexIntro())
        put("js_quickstart_index_docs", JS_QUICKSTART_INDEX_URL)
        put("js_quickstart_index_url_slash", JS_QUICKSTART_INDEX_URL_SLASH)
        put("js_quickstart_index_title", JS_QUICKSTART_INDEX_TITLE)
        put("js_reference_index_docs", JS_REFERENCE_INDEX_URL)
        put("js_reference_index_url_slash", JS_REFERENCE_INDEX_URL_SLASH)
        put("js_reference_index_title", JS_REFERENCE_INDEX_TITLE)
        put("python_index_docs", PYTHON_INDEX_URL)
        put("python_index_url_slash", PYTHON_INDEX_URL_SLASH)
        put("python_index_title", PYTHON_INDEX_TITLE)
        put("python_index_intro", officialPythonIndexIntro())
        put("python_client_index_docs", PYTHON_CLIENT_INDEX_URL)
        put("python_client_index_url_slash", PYTHON_CLIENT_INDEX_URL_SLASH)
        put("python_client_index_title", PYTHON_CLIENT_INDEX_TITLE)
        put("kotlin_index_docs", KOTLIN_INDEX_URL)
        put("kotlin_index_url_slash", KOTLIN_INDEX_URL_SLASH)
        put("kotlin_index_title", KOTLIN_INDEX_TITLE)
        put("kotlin_index_intro", officialKotlinIndexIntro())
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
        put("csharp_index_intro", officialCsharpIndexIntro())
        put("rust_index_docs", RUST_INDEX_URL)
        put("rust_index_url_slash", RUST_INDEX_URL_SLASH)
        put("rust_index_title", RUST_INDEX_TITLE)
        put("rust_client_index_docs", RUST_CLIENT_INDEX_URL)
        put("rust_client_index_url_slash", RUST_CLIENT_INDEX_URL_SLASH)
        put("rust_client_index_title", RUST_CLIENT_INDEX_TITLE)
        put("rust_index_intro", officialRustIndexIntro())
        put("go_index_docs", GO_INDEX_URL)
        put("go_index_url_slash", GO_INDEX_URL_SLASH)
        put("go_index_title", GO_INDEX_TITLE)
        put("go_index_intro", officialGoIndexIntro())
        put(
            "skipped_sign_or_key",
            buildJsonArray { skipped.forEach { add(JsonPrimitive(it)) } }
        )
        put("hello_world_rell", ChrGenerateClientHelp.helloWorldRell())
        put("hello_world_result", "Hello World!")
        put("js_reference_directory_iid", ChrGenerateClientHelp.DIRECTORY_CHAIN_IID)
        put("js_reference_brid_setting", ChrGenerateClientHelp.SOURCE_BRID_SETTING)
        put("js_reference_official_prose_brid", ChrGenerateClientHelp.OFFICIAL_PROSE_BRID)
        put("js_reference_sticky_create_client", ChrGenerateClientHelp.officialStickyQueryClient())
        put("js_reference_official_strategy_typo", ChrGenerateClientHelp.OFFICIAL_STRATEGY_TYPO)
        put("js_reference_official_abort_typo", ChrGenerateClientHelp.OFFICIAL_ABORT_TYPO)
        put("js_reference_source_failover_abort", ChrGenerateClientHelp.SOURCE_FAILOVER_ABORT)
        put("official_outdated_banner", ChrGenerateClientHelp.OUTDATED_BANNER)
        put("python_env_node", ChrGenerateClientHelp.PYTHON_ENV_NODE)
        put("python_env_rid", ChrGenerateClientHelp.PYTHON_ENV_RID)
        put("python_query_reviews", ChrGenerateClientHelp.officialPythonReviewsQuery())
        put("python_env", ChrGenerateClientHelp.officialPythonEnv())
        put("kotlin_standard_chromia_client", ChrGenerateClientHelp.officialStandardChromiaClient())
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
// Official BUILD clients/postchain-clients leftovers encoded as POSTCHAIN_CLIENTS_INDEX_* (query-only).
// Official BUILD clients/postchain-clients/javascript-typescript leftovers encoded as JS_TS_INDEX_* (query-only).
// Official BUILD clients/ft4-client leftovers encoded as FT4_CLIENT_INDEX_* (query-only).
// Official BUILD clients/react-kit leftovers encoded as REACT_KIT_INDEX_* (query-only).
// Official BUILD clients/filehub-client leftovers encoded as FILEHUB_CLIENT_INDEX_* (query-only).
// Official BUILD clients/bridge-client leftovers encoded as BRIDGE_CLIENT_INDEX_* (query-only).
// Official BUILD clients/postchain-rest-api leftovers encoded as POSTCHAIN_REST_API_INDEX_* (query-only).
// Official BUILD clients/overview leftovers encoded as OVERVIEW_INDEX_* (query-only).
// Official BUILD clients/postchain-clients/javascript-typescript/hello-world-quickstart leftovers encoded as JS_QUICKSTART_INDEX_* (query-only).
// Official BUILD clients/postchain-clients/javascript-typescript/reference leftovers encoded as JS_REFERENCE_INDEX_* (query-only).
// Official BUILD clients/postchain-clients/c-sharp-client leftovers encoded as CSHARP_CLIENT_INDEX_* (query-only).
// Official BUILD clients/postchain-clients/go-client leftovers encoded as GO_CLIENT_INDEX_* (query-only).
// Official BUILD clients/postchain-clients/rust-client leftovers encoded as RUST_CLIENT_INDEX_* (query-only).
// Official BUILD clients/postchain-clients/kotlin-client leftovers encoded as KOTLIN_CLIENT_INDEX_* (query-only).
// Official BUILD clients/postchain-clients/python-client leftovers encoded as PYTHON_CLIENT_INDEX_* (query-only).
// Official get-started/about/dapp leftovers encoded as GET_STARTED_DAPP_INDEX_* (query-only).
// Official get-started/about/chromia-vs-evm leftovers encoded as GET_STARTED_CHROMIA_VS_EVM_INDEX_* (query-only).
// Official get-started/about/protocols/gtx leftovers encoded as GET_STARTED_GTX_INDEX_* (query-only).
// Official get-started/about/architecture/chains/system-anchoring-chain leftovers encoded as GET_STARTED_SYSTEM_ANCHORING_CHAIN_INDEX_* (query-only).
// Official get-started/about/staking/user-delegation leftovers encoded as GET_STARTED_USER_DELEGATION_INDEX_* (query-only).
// Official get-started/about/protocols/iccf leftovers encoded as GET_STARTED_ICCF_PROTOCOL_INDEX_* (query-only).
// Official ecosystem/governance/getting-started/governance-structure/user-proposal-flows leftovers encoded as ECOSYSTEM_GOV_USER_PROPOSAL_FLOWS_INDEX_* (query-only).
// Official ecosystem/governance/governance-voting-process/voting-flow leftovers encoded as ECOSYSTEM_GOV_VOTING_FLOW_INDEX_* (query-only).
// Official ecosystem/bridge/bridge-client/client leftovers encoded as ECOSYSTEM_BRIDGE_CLIENT_CONFIGURE_INDEX_* (query-only).
// Official LEARN courses/book-review/book-review-entity/write-queries leftovers encoded as LEARN_BOOK_REVIEW_REVIEW_QUERIES_INDEX_* (query-only).
// Official LEARN courses/ft4-demo-app/module-blockchain/account-management leftovers encoded as LEARN_FT4_DEMO_ACCOUNT_MGMT_INDEX_* (query-only).
// Official LEARN courses/my-news-feed/introduction INDEX leftovers encoded as LEARN_NEWS_INTRODUCTION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/React INDEX leftovers encoded as LEARN_TAGS_REACT_INDEX_* (query-only HELP ONLY WRITE SKIP).
