# Functional Overview

## High-Level Features

Chromia MCP Server provides three primary feature sets through MCP protocol tools:

### 1. Blockchain Data Querying

Comprehensive access to Chromia blockchain network data through GraphQL queries:

- **Network Statistics** - Get overall network metrics, transaction counts, and network health
- **Blockchain Information** - Query blockchain details, metadata, state, and deployment information
- **Transaction Analysis** - Filter and analyze transactions by various criteria (blockchain, signer, account, operation type, timestamp)
- **Asset Information** - Query asset details, distribution, top holders, and cross-chain asset tracking
- **Account Activity** - Analyze account activity across blockchains, monthly active accounts per chain
- **Node Performance** - Monitor node availability and unavailability periods
- **Operation Analysis** - Get all operation types and analyze operation patterns

### 2. dApp Query Execution

Direct interaction with deployed Chromia dApps:

- **dApp Structure Discovery** - Get complete dApp structure including queries, operations, entities, and modules
- **Custom Query Execution** - Execute any query defined in a dApp with custom parameters
- **Query Parameter Discovery** - Discover available queries and their parameter requirements
- **Real-time Data Retrieval** - Get current state data from blockchain applications

### 3. Semantic Documentation Search

RAG-powered documentation retrieval:

- **Semantic Search** - Find relevant documentation based on meaning, not just keywords
- **Context-Aware Retrieval** - Returns documentation segments most relevant to the query
- **Multi-Repository Support** - Searches across multiple Chromia documentation repositories
- **Vector-Based Matching** - Uses embedding similarity to find relevant content

## Primary User/System Flows

### Tool Execution Flow

**If an AI assistant calls a tool:**
1. AI assistant sends MCP tool call request with tool name and parameters
2. MCP server receives request and routes to `ToolExecutor`
3. `ToolExecutor` looks up strategy for tool name
4. Strategy extracts and validates parameters
5. Strategy calls repository method with parameters
6. Repository routes to appropriate service (GraphQL or PostchainClient)
7. Service executes query/request to external API or blockchain node
8. Response is formatted and returned to AI assistant

**If a required parameter is missing:**
- Strategy throws `IllegalArgumentException`
- Error is caught and returned as error response to AI assistant
- AI assistant receives error message indicating missing parameter

**If external service is unavailable:**
- Service returns `NetworkResult.Error`
- Strategy formats error message
- AI assistant receives error response with failure reason

### Network Selection Flow

**If network parameter is provided:**
1. Network parameter is extracted from tool arguments
2. Network is validated against predefined networks (mainnet, testnet, devnet1, devnet2)
3. If network is valid, appropriate API endpoint or node URLs are selected
4. Query is executed against selected network

**If network parameter is not provided:**
1. Default network ("mainnet") is used
2. Query is executed against mainnet endpoints

**If network parameter is invalid:**
1. `NetworkConfigurationException` is thrown
2. Error response includes list of valid network names
3. AI assistant receives error with available options

### Blockchain Filtering Flow

**If a user wants to find a specific blockchain:**
1. AI assistant calls `filter_blockchains` tool with search criteria (name, cluster, container, state, etc.)
2. Server executes GraphQL query with filters
3. Results are returned with matching blockchains
4. User can then use blockchain RID for subsequent queries

**If a user wants blockchain details:**
1. AI assistant calls `get_blockchain_details` with blockchain RID
2. Server retrieves detailed blockchain information
3. Response includes deployment info, state, cluster, container, and metadata

### dApp Query Execution Flow

**If a user wants to query a dApp:**
1. **Step 1:** AI assistant calls `filter_blockchains` to find blockchain by name
2. **Step 2:** AI assistant calls `chromia_dapp_query` with `rell.get_app_structure` query to get dApp structure
3. **Step 3:** AI assistant analyzes structure to find desired query
4. **Step 4:** AI assistant calls `chromia_dapp_query` with specific query name and parameters
5. **Step 5:** Server executes query on blockchain and returns results

**Query naming convention:**
- Simple queries: Use query name directly (e.g., "get_all_libraries")
- Mounted queries: Use "mount_name.query_name" format (e.g., "module1.query_name")

**If query does not exist:**
- PostchainClient returns error
- Error is formatted and returned to AI assistant
- Error message indicates query not found

**If query parameters are incorrect:**
- PostchainClient may return error or unexpected results
- Server returns response as-is (no parameter validation at server level)

### Documentation Search Flow

**If a user asks a documentation question:**
1. AI assistant calls `fetch_docs` tool with natural language query
2. Server waits for RAG store to load (if not already loaded)
3. RAG store performs semantic search using vector embeddings
4. Top 15 most relevant documentation segments are retrieved (min similarity: 0.6)
5. Segments are concatenated and returned to AI assistant
6. AI assistant uses retrieved documentation to answer user's question

**If RAG store is not loaded:**
- `FetchDocsStrategy` awaits RAG store initialization
- If initialization fails, error is returned
- Documentation search is unavailable until embeddings are loaded

**If no relevant documentation is found:**
- Server returns "Documentation not found for requested query!"
- AI assistant can try rephrasing query or using different keywords

### Transaction Analysis Flow

**If a user wants to analyze transactions:**
1. AI assistant calls `get_all_transactions` with filters (blockchain, signer, account, operation, timestamp range)
2. Server executes GraphQL query with filter parameters
3. Results include pagination support (limit, offset)
4. Results can be sorted by various fields (timestamp, block ID, etc.)
5. Transaction data is returned with full details

**If pagination is needed:**
- AI assistant can make multiple calls with different offset values
- Server returns transactions in pages based on limit parameter

## Important Assumptions

### Network Assumptions

- **Network names are standardized** - Only "mainnet", "testnet", "devnet1", "devnet2" are supported
- **Network URLs are stable** - Node URLs in configuration are expected to remain available
- **Default network is mainnet** - If network is not specified, mainnet is used

### Blockchain Assumptions

- **Blockchain RIDs are unique** - Each blockchain has a unique RID identifier
- **Blockchains are discoverable** - Blockchains can be found using `filter_blockchains` tool
- **Blockchain state is current** - Information returned reflects current blockchain state
- **dApps are deployed** - Custom queries assume dApps are already deployed on target blockchains

### Query Assumptions

- **Query results are JSON** - All responses are in JSON format
- **Error responses are informative** - External services provide meaningful error messages
- **Timeouts are reasonable** - HTTP timeouts (30s request, 10s connect) are sufficient for most queries

### Documentation Assumptions

- **Embeddings are pre-computed** - Documentation embeddings are created separately and stored in GitLab packages
- **Semantic search is accurate** - Vector similarity (minScore: 0.6) provides relevant results
- **Documentation is comprehensive** - All relevant Chromia documentation is included in embeddings

## Business and Technical Constraints

### Query Constraints

- **Query result size** - Large result sets may be truncated or require pagination

### Documentation Constraints

- **Embedding availability** - Documentation search requires embeddings to be available in GitLab packages
- **Search quality** - Semantic search quality depends on embedding model and similarity threshold

## Non-Obvious or Surprising Behavior

### dApp Query Execution

**Default query behavior:**
- If `query` parameter is not provided to `chromia_dapp_query`, it defaults to `rell.get_app_structure`
- This allows discovering available queries without explicitly specifying the structure query

**Query result filtering:**
- `PostchainClientService` filters module structure to show only queries (not operations or entities)
- This simplifies dApp structure discovery for AI assistants

**Argument type conversion:**
- `DappInteractionStrategy` converts JSON primitives to appropriate types (string, int, boolean, etc.)
- Complex nested structures are supported (maps, arrays)

### Documentation Search

**Asynchronous loading:**
- RAG store loads embeddings asynchronously at server startup
- First documentation query may wait for embeddings to load
- Subsequent queries use cached embeddings

**Similarity scoring:**
- Minimum similarity score of 0.6 may exclude some relevant results
- Maximum 15 results may not include all relevant documentation
- Results are ordered by similarity (most relevant first)

### Network Selection

**Fallback to default:**
- If network parameter is null or empty, default network ("mainnet") is used

**Network URL selection:**
- PostchainClient uses `EndpointPool.default()` which tries multiple node URLs
- If one node fails, client automatically tries next node in the pool
- This provides redundancy but may cause a bit of delays if multiple nodes are unavailable

### Transaction Filtering

**Complex filter combinations:**
- Multiple filters can be combined (blockchain, signer, account, operation, timestamp)
- Filters are AND-ed together (all conditions must match)
- Exclusion filters (excludeAccounts, excludeSigners) work alongside inclusion filters

**Pagination behavior:**
- If limit is not specified, default pagination may apply (depends on GraphQL API)
- Offset-based pagination requires tracking offset across multiple requests
- Sorting can be applied independently of pagination

## Known Functional Limitations

### Mock Tools for ChatGPT

- `search` and `fetch` tools return mock data instead of using RAG store. required for ChatGPT integration
- ChatGPT still uses `fetch_docs` tool instead, which uses real RAG store.


### RAG Store Embedding Availability

**Limitation:** Documentation search requires embeddings to be pre-computed and available in GitLab packages.

**Impact:** If embeddings are not available, documentation search will not work.

**Workaround:** Embedding generation code exists but is commented out. Can be uncommented to generate embeddings locally.

**To enable local embedding generation:**
1. Add documentation GitHub/GitLab repositories to `app/src/main/resources/docs-repositories.json`:
   ```json
   {
     "repositories": [
       {
         "name": "repository-name",
         "url": "https://github.com/organization/repo.git",
         "branch": "main",
         "subdirectories": ["docs"]
       }
     ]
   }
   ```
   - Each repository entry requires: `name`, `url`, `branch`, and `subdirectories` (array of directories to include)
   - For private repositories, include credentials in URL: `https://username:token@github.com/org/repo.git`
        - NOTE: Don't forget to remove the credentials before commiting
2. Uncomment the imports in `app/src/main/kotlin/org/chromia/tools/RagStore.kt` (lines 14-16):
   ```kotlin
   import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
   import org.chromia.uploadFile
   import kotlin.io.path.createTempFile
   ```
3. Uncomment the `createAndUploadEmbeddings()` function (lines 51-82 in `RagStore.kt`)
4. Update line 28 to use the function as fallback:
   ```kotlin
   var embeddingStore = downloadFromRegistry() ?: createAndUploadEmbeddings()
   ```
5. Set `GITLAB_ACCESS_TOKEN` environment variable if uploading to GitLab packages

**Status:** Embedding generation automation in CI/CD pipeline is not yet implemented. It would be beneficial to have a periodic CI/CD job that automatically generates and updates embeddings when documentation repositories change.

###  Network Timeout Handling

**Limitation:** HTTP timeouts are fixed (30s request, 10s connect) and may not be appropriate for all queries.

**Impact:** Long-running queries may timeout, causing failures.

**Workaround:** Complex queries may need to be broken into smaller queries.

**Status:** Timeouts could be made configurable per query type.

## Error Handling Behavior

### Parameter Validation Errors

**If required parameter is missing:**
- Strategy throws `IllegalArgumentException` with parameter name
- Error is caught and returned as error response
- AI assistant receives: "Missing required parameter: {parameter_name}"

### Network Configuration Errors

**If network name is invalid:**
- `NetworkConfigurationException` is thrown
- Error message includes invalid network name and list of valid networks
- AI assistant receives: "Network '{network}' not found. Available networks: {list}"

### External Service Errors

**If GraphQL query fails:**
- `HttpRequestException` is thrown with error details
- Error message includes HTTP status and response body
- AI assistant receives: "Failed to execute GraphQL query: {error_message}"

**If PostchainClient query fails:**
- `PostchainClientException` is thrown with blockchain RID and error
- Error message includes query name and failure reason
- AI assistant receives: "Failed to execute dapp query {query}: {error_message}"

### RAG Store Errors

**If RAG store fails to load:**
- `FetchDocsStrategy` catches exception
- Error message indicates RAG store is unavailable
- AI assistant receives: "Error fetching documentation from classpath: {error_message}"

**If no documentation is found:**
- RAG store returns null or empty list
- Server returns: "Documentation not found for requested query!"
- AI assistant can try rephrasing query

### Error Response Format

All errors are returned as `CallToolResult` with `TextContent` containing error message. Error messages are plain text strings, not structured JSON error objects.
