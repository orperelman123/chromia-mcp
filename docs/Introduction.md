# Introduction

**Project Name:** Chromia MCP Server

**Repository URL:** `https://gitlab.com/chromaway/core-tools/chromia-mcp`

**GitLab Project Path:** `chromaway/core-tools/chromia-mcp`

**Production API URL:** `https://mcp.chromia.dev`

## Project Summary

Chromia MCP Server is a **Model Context Protocol (MCP)** server that provides AI assistants (like Claude, ChatGPT, Cursor, JetBrains AI Assistant) with programmatic access to Chromia blockchain infrastructure and deployed dApps. It functions as a middleware layer between AI assistants and Chromia blockchain networks, exposing blockchain data through a standardized MCP protocol interface.

The service provides three primary capabilities:

1. **Blockchain Data Access** - Query network statistics, transactions, assets, accounts, node performance, and blockchain metadata through the Chromia Explorer GraphQL API.

2. **dApp Query Execution** - Execute custom queries on deployed Chromia dApps using PostchainClient to directly interact with blockchain nodes.

3. **RAG-Powered Documentation Search** - Semantic search through Chromia documentation using vector embeddings, enabling AI assistants to find and retrieve relevant documentation based on meaning rather than keywords.

The server supports two transport modes:
- **SSE (Server-Sent Events)** - HTTP-based transport for remote connections
- **Stdio (Standard Input/Output)** - Subprocess-based transport for local MCP clients

## Value Creation

Chromia MCP Server enables AI assistants to interact with Chromia blockchain data by:

- **Eliminating blockchain protocol knowledge requirements** - AI assistants can query blockchain data using natural language, and the server translates requests into appropriate GraphQL queries or PostchainClient calls.

- **Providing integrated semantic documentation search** - Built-in RAG (Retrieval-Augmented Generation) capabilities allow AI assistants to search Chromia documentation semantically, finding relevant information based on context and meaning.

- **Enabling multi-network access** - Single server interface supports multiple Chromia networks (mainnet, testnet, devnet1, devnet2) with automatic network routing.

- **Offering flexible transport options** - Supports both HTTP/SSE (for remote connections) and stdio (for local subprocess execution)

- **Providing comprehensive blockchain analytics** - Exposes rich blockchain data including network statistics, transaction analysis, asset distribution, account activity, and node performance metrics.

## Users

**Primary Users:**

- **AI Assistant Users** (developers, researchers, analysts) who want to query Chromia blockchain data through natural language interactions with AI assistants like Claude, ChatGPT, or Cursor.

- **Developers building on Chromia** who need AI-powered assistance for understanding blockchain data, analyzing transactions, exploring dApp structures, and finding documentation.

- **Blockchain analysts and researchers** who want to perform data analysis on Chromia networks through AI-assisted queries.

**User Access Patterns:**

- Direct MCP protocol communication from AI assistant clients (Cursor, Claude Desktop, JetBrains AI Assistant, ChatGPT)
- Configuration in MCP client settings
- Both local development (stdio mode) and remote production (SSE mode) access

## Upstream and Downstream Projects

### Upstream Dependencies (What Chromia MCP Server Consumes)

**1. Chromia Explorer GraphQL API**
- **Role:** Primary data source for network statistics, transactions, assets, accounts, and blockchain metadata
- **Endpoint:** `https://explorer.chromia.com/api/explorer-service`
- **Relationship:** Chromia MCP Server executes GraphQL queries against this API to retrieve blockchain data
- **Critical Dependency:** If Chromia Explorer API is unavailable, most blockchain data queries will fail. The server does not maintain its own database.

**2. Chromia Blockchain Networks (Postchain)**
- **Role:** Direct blockchain node access for dApp query execution
- **Relationship:** Chromia MCP Server uses PostchainClient to connect directly to blockchain nodes and execute custom queries on deployed dApps
- **Networks Supported:**
  - Mainnet
  - Testnet
  - Devnet1
  - Devnet2

**3. MCP Protocol SDK**
- **Role:** Kotlin SDK for implementing MCP server functionality
- **Package:** `io.modelcontextprotocol:kotlin-sdk:0.7.7`
- **Relationship:** Provides server infrastructure, tool registration, and transport layer (stdio/SSE)
- **Critical Dependency:** Core dependency for MCP protocol implementation.

**4. LangChain4j (RAG Library)**
- **Role:** Provides embedding store and content retrieval for semantic documentation search
- **Package:** `dev.langchain4j:langchain4j-easy-rag:1.8.0-beta15`
- **Relationship:** Used by `RagStore` for loading embeddings and performing semantic search
- **Dependency Type:** Required for documentation search functionality only.

### Downstream Consumers (What Depends on Chromia MCP Server)

**1. AI Assistant Clients**
- **Cursor/Windsurf IDEs** - Configure MCP server via IDE settings
- **Claude Desktop** - Configure via `claude_desktop_config.json`
- **JetBrains AI Assistant** - Configure via Settings → Tools → AI Assistant → MCP
- **ChatGPT** - Configure via Workspace settings → Connectors
- **Relationship:** These clients connect to Chromia MCP Server via MCP protocol to access Chromia blockchain tools
- **Data Flow:** AI assistant → MCP protocol → Chromia MCP Server → Chromia Explorer API / Blockchain nodes → Response back through chain

**2.[@chromia/chromia-lsp-mcp](https://www.npmjs.com/package/@chromia/chromia-lsp-mcp) (Complementary Service)**
- **Role:** Provides Rell language server capabilities for AI assistants or any other MCP clients that don't have direct access to LSP functionalities, e.g : `Claude Code`
- **Relationship:** Often used alongside Chromia MCP Server to provide both blockchain data access and Rell code understanding
- **Note:** This is a separate service, not a dependency of Chromia MCP Server.

## Data and Control Flow

### Request Flow

```
1. AI Assistant Client (Cursor, Claude Desktop, etc.)
   ↓
   MCP Protocol Request (JSON-RPC over stdio or SSE)
   ↓
2. Chromia MCP Server (App.kt)
   ↓
   Tool Registration & Routing (ToolExecutor.kt)
   ↓
3. Tool Strategy (e.g., BlockchainDetailsStrategy)
   ↓
   Parameter Extraction & Validation
   ↓
4. ChromiaRepository (ChromiaRepositoryImpl)
   ↓
   Routes to appropriate service:
   - HttpClientService → GraphQL queries → Chromia Explorer API
   - PostchainClientService → Direct queries → Blockchain nodes
   - RagStore → Semantic search → Embeddings store
   ↓
5. External Service
   ↓
   Chromia Explorer API / Blockchain Node / Embeddings Store
   ↓
6. Response flows back through the chain
   ↓
7. AI Assistant receives structured response
```

### Control Flow Details

**Tool Execution:**
- AI assistant sends tool call request with tool name and parameters
- `ToolExecutor` routes to appropriate strategy based on tool name
- Strategy extracts and validates parameters
- Strategy calls repository method
- Repository routes to appropriate client service (HTTP/GraphQL or PostchainClient)
- Response is formatted and returned to AI assistant

**Network Routing:**
- Most tools accept optional `network` parameter (mainnet, testnet, devnet1, devnet2)
- Network parameter is used to select appropriate API endpoint or blockchain node URLs
- Default network is "mainnet" if not specified

**RAG Store Initialization:**
- `RagStore` loads asynchronously at server startup to avoid blocking
- Downloads `embeddings.json` from GitLab packages
- If download fails, documentation search will not work (but server continues running)

**Transport Modes:**
- **SSE Mode:** Server runs as HTTP server, clients connect via HTTP/SSE
- **Stdio Mode:** Server runs as subprocess, communicates via stdin/stdout

### Data Persistence

Chromia MCP Server is **stateless** - it does not maintain its own database or cache. All data is:
- Retrieved on-demand from Chromia Explorer API (GraphQL queries)
- Retrieved on-demand from blockchain nodes (PostchainClient queries)
- Loaded from GitLab packages (embeddings for documentation search)

The only in-memory state is:
- RAG store embeddings (loaded at startup from GitLab packages)
- Server runtime state (tool registrations, active connections)

## External References and Resources

### MCP Protocol Documentation
- **MCP Protocol Specification:** https://modelcontextprotocol.io/
- **Kotlin MCP SDK:** https://github.com/modelcontextprotocol/kotlin-sdk

### Chromia Blockchain Documentation
- **Chromia Documentation Portal:** https://docs.chromia.com
- **Chromia Explorer:** https://explorer.chromia.com

### API Endpoints
- **Production MCP Server:** `https://mcp.chromia.dev`
- **Chromia Explorer GraphQL API:** `https://explorer.chromia.com/api/explorer-service`
- **Health Check Endpoint:** `https://mcp.chromia.dev/health`

### Package Registries
- **GitLab Maven Registry:** `https://gitlab.com/api/v4/projects/{PROJECT_ID}/packages/maven`
- **GitLab Generic Packages (Embeddings):** `https://gitlab.com/api/v4/projects/71940508/packages/generic/embeddings/v1`
