# Technical Architecture & Codebase

## High-Level Architecture Description

Chromia MCP Server follows a layered architecture pattern with clear separation between protocol layer, routing layer, business logic layer, and data access layer. The system is built as a stateless MCP server that translates MCP tool calls into blockchain data queries.

**Architecture Layers:**

1. **MCP Protocol Layer** - Handles MCP protocol communication (stdio/SSE), tool registration, and request/response formatting
2. **Routing Layer** - Routes tool calls to appropriate strategy implementations
3. **Business Logic Layer** - Contains tool strategies that extract parameters, validate input, and orchestrate data retrieval
4. **Data Access Layer** - Abstracts data retrieval through repository pattern, routing to GraphQL or PostchainClient services
5. **External Service Layer** - Communicates with Chromia Explorer API, blockchain nodes, and GitLab packages

**Key Architectural Decisions:**

- **Stateless design** - No server-side state except in-memory embeddings cache. Each request is independent.
- **Strategy pattern** - Each tool has a dedicated strategy class, making tools easy to add and test independently.
- **Repository pattern** - Data access is abstracted through `ChromiaRepository` interface, allowing different implementations.
- **Dual data access paths** - GraphQL queries for network/transaction data, PostchainClient for direct dApp queries.
- **Async RAG store initialization** - Embeddings load asynchronously to avoid blocking server startup.

## Major Components and Responsibilities

### 1. Entry Point (`App.kt`)

**Responsibility:** Application bootstrap, MCP server initialization, and transport layer setup.

**Key Functions:**
- `createMcpServer()`: Creates and configures MCP server with tool registration
- `registerTools()`: Registers all available tools from `McpTools.kt`
- `runStdioMcpServer()`: Starts server in stdio mode (subprocess communication)
- `runSseMcpServer()`: Starts server in SSE mode (HTTP server on port 3001)
- `installCors()`: Configures CORS for SSE mode
- `installHealthEndpoint()`: Adds `/health` endpoint for monitoring

**Why it matters:** Centralizes server configuration and transport layer setup. Single entry point for both stdio and SSE modes.

**Key Dependencies:**
- `ToolExecutor` - Routes tool calls to strategies
- `ChromiaRepositoryImpl` - Provides data access
- `PromptManager` - Manages prompt templates

### 2. Tool Registration (`McpTools.kt`)

**Responsibility:** Defines all MCP tool schemas and metadata.

**Key Responsibilities:**
- Defines tool names, descriptions, and input schemas
- Provides detailed instructions for AI assistants on tool usage
- Specifies required/optional parameters with types
- Documents tool behavior and return values

**Pattern:** Each tool has a function that returns a `Tool` object with:
- `name`: Unique tool identifier (e.g., "get_blockchain_details")
- `description`: Detailed description for AI assistants, including workflows and use cases
- `inputSchema`: JSON schema defining parameter types and requirements
- `title`: Human-readable title

**Why it matters:** Single source of truth for tool definitions. Tool schemas are used by MCP clients to understand available capabilities.

**Example Tools:**
- `get_blockchain_details` - Get blockchain information by RID
- `get_network_stats` - Get network statistics
- `chromia_dapp_query` - Execute custom dApp queries
- `fetch_docs` - Semantic documentation search
- `filter_blockchains` - Filter blockchains by criteria

### 3. Tool Execution Router (`ToolExecutor.kt`)

**Responsibility:** Routes tool calls to appropriate strategy implementations.

**Key Components:**
- `ToolStrategy` interface: Contract for tool strategies
- `BaseToolStrategy`: Base class with helper methods for parameter extraction
- `strategies` map: Maps tool names to strategy instances
- `executeTool()`: Main routing function

**Key Responsibilities:**
- Maps tool names to strategy implementations
- Manages RAG store initialization (async, non-blocking)
- Handles tool execution errors and returns appropriate responses
- Provides parameter extraction utilities (string, int, boolean, list)

**Why it matters:** Centralizes tool routing logic. Adding a new tool only requires adding a strategy to the map.

**Strategy Pattern:**
- Each tool has a dedicated strategy class (e.g., `BlockchainDetailsStrategy`)
- Strategies extend `BaseToolStrategy` for common functionality
- Strategies extract parameters, validate input, call repository methods, and format responses

### 4. Tool Strategies (`ToolExecutor.kt` - Strategy Classes)

**Responsibility:** Implement business logic for individual tools.

**Key Strategy Classes:**

**`BlockchainDetailsStrategy`** - Retrieves blockchain information by RID
- Extracts `rid` and `network` parameters
- Calls `repository.getBlockchainDetails()`
- Returns blockchain metadata

**`NetworkStatsStrategy`** - Retrieves network statistics
- Extracts `network` parameter
- Calls `repository.getNetworkStats()`
- Returns network-wide statistics

**`DappInteractionStrategy`** - Executes custom dApp queries
- Extracts `network`, `blockchainRid`, `query`, and `arguments`
- Handles complex argument parsing (nested maps, arrays, primitives)
- Calls `repository.executeCustomQuery()`
- Returns query results

**`FetchDocsStrategy`** - Semantic documentation search
- Extracts `query` parameter
- Waits for RAG store to load (if not ready)
- Calls `ragStore.query()` for semantic search
- Returns relevant documentation segments

**`FilterBlockchainsStrategy`** - Filters blockchains by criteria
- Extracts complex filter parameters (rid, name, cluster, container, state, system)
- Handles pagination and sorting parameters
- Calls `repository.filterBlockchains()`
- Returns filtered blockchain list

**Why it matters:** Separation of concerns. Each tool's logic is isolated, making it easy to modify or test individual tools.

### 5. Repository Layer (`ChromiaRepository` Interface & `ChromiaRepositoryImpl`)

**Responsibility:** Data access abstraction layer.

**Interface (`ChromiaRepository.kt`):**
- Defines all data access methods
- Returns `JsonResult` (alias for `NetworkResult<JsonObject>`)
- Methods organized by domain (blockchain, asset, transaction, network)

**Implementation (`ChromiaRepositoryImpl.kt`):**
- Routes queries to appropriate client service:
  - `HttpClientService` for GraphQL queries (network stats, transactions, assets)
  - `PostchainClientService` for direct blockchain queries (dApp queries)
- Delegates to client services based on query type

**Why it matters:** Repository pattern allows swapping implementations without changing business logic. Clear separation between business logic and data access.

### 6. Client Services (`data/client/`)

**Responsibility:** Direct communication with external services.

**`HttpClientService`** - GraphQL query execution
- Uses Ktor HTTP client with JSON serialization
- Executes GraphQL queries against Chromia Explorer API
- Handles network selection (mainnet, testnet, etc.)
- Manages HTTP timeouts (30s request, 10s connect)
- Parses GraphQL responses and handles errors

**`PostchainClientService`** - Direct blockchain query execution
- Uses PostchainClient to connect to blockchain nodes
- Resolves network URLs from configuration
- Executes queries on specific blockchains (by RID)
- Converts GTV (blockchain format) to JSON
- Filters module structure to queries only
- Handles blockchain connection errors

**Why it matters:** Encapsulates external service communication. Handles protocol-specific details (GraphQL, Postchain) away from business logic.

### 7. Query Definitions (`data/queries/`)

**Responsibility:** GraphQL query construction.

**Components:**
- `NetworkQueries.kt` - Network-wide queries (stats, transactions, operations)
- `BlockchainQueries.kt` - Blockchain-specific queries (details, analytics, active accounts)
- `AssetQueries.kt` - Asset-related queries (distribution, top holders, blockchains)
- `TransactionQueries.kt` - Transaction queries (filtering, signer/account blockchains)

**Pattern:** Each query file contains functions that build GraphQL query strings with parameters.

**Why it matters:** Centralizes GraphQL query logic. Makes it easy to update queries when API changes.

### 8. Configuration (`data/config/ChromiaConfig.kt`)

**Responsibility:** Application configuration and network definitions.

**Key Configuration:**
- `explorerUrl`: Chromia Explorer GraphQL API endpoint
- `defaultNetwork`: Default network if not specified ("mainnet")
- `predefinedNetworks`: Map of network names to node URL lists
  - mainnet: 13 node URLs
  - testnet: 4 node URLs
  - devnet1: 4 node URLs
  - devnet2: 4 node URLs
- `httpTimeouts`: HTTP client timeout configuration

**Why it matters:** Centralizes configuration. Network URLs are defined once and reused across services.

### 9. RAG Store (`tools/RagStore.kt`)

**Responsibility:** Semantic documentation search using vector embeddings.

**Key Components:**
- Downloads `embeddings.json` from GitLab packages at initialization
- Uses LangChain4j `InMemoryEmbeddingStore` for vector storage
- Uses `EmbeddingStoreContentRetriever` for semantic search
- Configures search parameters (maxResults: 15, minScore: 0.6)

**Initialization:**
- Loads asynchronously in `ToolExecutor` to avoid blocking server startup
- Downloads embeddings from GitLab Generic Packages
- Falls back to creating embeddings if download fails (commented out in current code)

**Why it matters:** Enables semantic documentation search. AI assistants can find relevant documentation based on meaning, not just keywords.

### 10. Prompt Manager (`tools/PromptManager.kt`)

**Responsibility:** Manages prompt templates for AI assistants.

**Key Functions:**
- Loads prompts from `prompt_templates.json` resource file
- Organizes prompts by category
- Provides filtering by category, tool, and search query
- Used by `get_prompts` tool

**Why it matters:** Centralizes prompt templates. Makes it easy to provide context-specific prompts to AI assistants.

### 11. Domain Models (`domain/`)

**Responsibility:** Data structures and type definitions.

**Components:**
- `NetworkResult.kt` - Result type (Success/Error) for repository methods
- `GraphQLQuery.kt` - GraphQL query wrapper
- `ChromiaRepository.kt` - Repository interface
- Filter classes: `BlockchainFilters`, `AssetFilters`, `TransactionFilters`, `AssetSearchFilters`
- Pagination and sorting: `PaginationParams`, `SortingParams`
- Exception classes: `HttpRequestException`, `PostchainClientException`, `NetworkConfigurationException`

**Why it matters:** Type-safe data structures. Clear contracts between layers.

## How Components Communicate

### Request Flow

```
MCP Client (AI Assistant)
    ↓
MCP Protocol (JSON-RPC over stdio/SSE)
    ↓
App.kt (createMcpServer)
    ↓
Tool Registration (registerTools)
    ↓
ToolExecutor.executeTool()
    ↓
Strategy Selection (strategies map lookup)
    ↓
Tool Strategy (e.g., BlockchainDetailsStrategy)
    ↓
Parameter Extraction & Validation
    ↓
ChromiaRepository (interface)
    ↓
ChromiaRepositoryImpl
    ↓
Route to Client Service:
    - HttpClientService → GraphQL → Chromia Explorer API
    - PostchainClientService → PostchainClient → Blockchain Nodes
    - RagStore → Embeddings → Semantic Search
    ↓
Response flows back through chain
    ↓
MCP Client receives structured response
```

### Component Interaction Details

**Tool Execution:**
1. MCP client sends `CallToolRequest` with tool name and arguments
2. `ToolExecutor.executeTool()` looks up strategy in `strategies` map
3. Strategy extracts parameters using `BaseToolStrategy` helpers
4. Strategy calls repository method with extracted parameters
5. Repository routes to appropriate client service
6. Client service executes query/request to external service
7. Response is wrapped in `NetworkResult` and returned
8. Strategy formats response as `CallToolResult`
9. `ToolExecutor` returns result to MCP server
10. MCP server sends response to client

**RAG Store Initialization:**
- `ToolExecutor` creates `Deferred<RagStore>` in async scope
- RAG store downloads embeddings from GitLab packages
- `FetchDocsStrategy` awaits RAG store before querying
- If RAG store fails to load, documentation search returns error

**Network Routing:**
- Tools accept optional `network` parameter
- Network parameter is passed to repository methods
- Repository passes network to client services
- Client services use network to select API endpoint or node URLs
- Default network is "mainnet" if not specified

## Key Frameworks, Libraries, and Versions

### Core Dependencies

- **Kotlin:** 2.2.0 (via Gradle plugin)
- **Java:** 21 (JDK required)
- **MCP Kotlin SDK:** 0.7.7 (`io.modelcontextprotocol:kotlin-sdk`)
- **Ktor:** 3.2.3 (HTTP client and server)
- **Postchain Client:** 3.36.0 (`net.postchain.client:postchain-client`)
- **LangChain4j:** 1.8.0-beta15 (`dev.langchain4j:langchain4j-easy-rag`)
- **Gson:** 2.13.2 (JSON parsing)
- **Log4j2:** 2.25.1 (Logging)

### Build Tools

- **Gradle:** 8.14.3 (via wrapper)
- **Shadow Plugin:** 8.3.6 (fat JAR creation)
- **Jib:** 3.4.5 (Docker image building)

### Key Libraries Purpose

- **MCP Kotlin SDK:** Provides MCP protocol implementation, server infrastructure, tool registration
- **Ktor:** HTTP client for GraphQL queries, HTTP server for SSE mode, JSON serialization
- **Postchain Client:** Direct blockchain node communication for dApp queries
- **LangChain4j:** RAG capabilities for semantic documentation search
- **Gson:** JSON parsing and GTV (blockchain format) conversion

## Known Technical Debt

### 1. Mock Tools for ChatGPT Compatibility

**Location:** `ToolExecutor.kt` - `FetchMock` class

**Issue:** `search` and `fetch` tools return mock data instead of using RAG store. this is for ChatGPT compatibility.

**TODO:** ChatGPT still uses the right tools, just for ChatGPT API cause this may change, and maybe we don't need to implement `search` and `fetch` mock tools

### 2. RAG Store Embedding Generation

**Current State:** The `createAndUploadEmbeddings()` method in `RagStore.kt` is commented out. The system currently relies on pre-computed embeddings from GitLab packages. This method should run periodically in a CI/CD pipeline to ensure up-to-date documentation embeddings.

**Location:** `app/src/main/kotlin/org/chromia/tools/RagStore.kt` - `createAndUploadEmbeddings()` method (lines 51-82, commented out)

**Issue:** Embedding generation code is commented out, requiring manual intervention or pre-existing embeddings in GitLab packages.

**Impact:** If embeddings are not available in GitLab packages, documentation search will not work. The server continues running, but the RAG store will fail to initialize.

**TODO:** Create a CI/CD pipeline that automatically creates and updates embeddings for documentation repositories periodically.

## Known Flaky Areas, that needs improvements

### RAG Store Initialization

**Risk:** If GitLab packages are unavailable or embeddings file is corrupted, RAG store will fail to load.

**Impact:** Documentation search will not work, but server continues running.

**Mitigation:** RAG store loads asynchronously, so server startup is not blocked.

### GraphQL API Changes

**Risk:** Chromia Explorer GraphQL API schema may change, breaking queries.

**Impact:** Tools that use GraphQL queries will fail.

**Mitigation:** Check Chromia Explorer Team for GraphQL API schema, Queries are centralized in `data/queries/` directory, making updates easier.
