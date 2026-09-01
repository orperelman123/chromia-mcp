# Functional Overview

## High-Level Features

Chromia MCP Server provides three primary feature sets through MCP protocol tools, plus three static MCP resources (`chromia://server/health`, `chromia://config/docs-repositories`, `chromia://config/prompt-catalog`) that expose data already in the server. MCP `prompts` is not advertised (use the `get_prompts` tool). There is no OpenAPI spec and no signed-transaction / `execute_transaction` tool. Server version is `0.2.2` from Gradle `project.version`.

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
5. Segments are returned as one line per hit (`id: <sha-256> | <text>` with real newlines written as `\\n`) plus structured `hits` (original text)
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

- **Embeddings are pre-computed** - Documentation embeddings are created separately, persisted locally (`embeddings.json`), and optionally uploaded to GitLab packages
- **Semantic search is accurate** - Vector similarity (minScore: 0.6) provides relevant results
- **Documentation is comprehensive** - All relevant Chromia documentation is included in embeddings

## Business and Technical Constraints

### Query Constraints

- **Query result size** - Large result sets may be truncated or require pagination

### Documentation Constraints

- **Embedding availability** - Documentation search requires a local `embeddings.json` or the published GitLab package
- **Search quality** - Semantic search quality depends on embedding model and similarity threshold

## Non-Obvious or Surprising Behavior

### dApp Query Execution

**Default query behavior:**
- If `query` parameter is not provided to `chromia_dapp_query`, it defaults to `rell.get_app_structure`
- This allows discovering available queries without explicitly specifying the structure query

**Complete dApp structure:**
- `PostchainClientService` returns converted `rell.get_app_structure` JSON as-is
- The structure includes queries, operations, functions, and entities

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

### dApp build helpers

Read-only BUILD tools (no explorer, no signed txs, no `chr` subprocess):

- `scaffold_dapp` — chromia.yml + `src/main.rell` + `src/test/main_test.rell` (Rell 0.16.1, merkle_hash_version 2, FT4 v1.1.0r API 1)
- `validate_chromia_yml` — `{ok, errors[], warnings[]}` for compile.rellVersion (semver N.N.N), `blockchains.*.module` (name, not path), merkle_hash_version 2, reserved deployment names, 64-hex Directory BRID, forbidden FT4 admin / ras_open libs
- `ft4_module_args` — production FT4 module_args + libs (`insecure: false`). `require_mandatory_flags` main-only. `DEFAULT_LOGIN_CONFIG_NAME` is `"default"`. Never emits admin / ras_open. `includeIccf=true` also emits official `net.postchain.d1.iccf.IccfGTXModule` gtx wiring
- `chr_build_help` — official CLI 0.33.x install / `chr install` / `chr build` / `chr test` commands and expected chromia.yml shape
- `chr_repl_help` — official CLI 0.33.x `chr repl` flags (`--sql-log` lives here; removed from `chr test` in 0.31.0). Does not run chr, generate keys, invent a BRID, or send a tx
- `chr_tools_help` — official CLI 0.33.x `chr tools` (`gtv` / `validate-config` / `lib-model`). `chr gtv` alias. Does not run chr, generate keys, invent a BRID, or send a tx
- `chr_seeder_help` — official CLI 0.33.x `chr seeder init` / `generate` (early-stage). Does not run chr, generate keys, invent a BRID, or send a tx
- `blockchain_properties_help` — official `blockchains.<name>.config` keys (gtx / blockstrategy / query timeouts). `merkle_hash_version` 2. Official keys only
- `chr_eif_help` — official CLI 0.33.x `chr eif generate-events-config` (`--abi`, `--events`, `--target`, `--format=(XML|YAML)`). Does not run chr, generate keys, invent a BRID, or send a tx
- `chromia_yml_definitions_help` — official chromia.yml `definitions` / YAML anchors / `!include` (project-config examples only). Does not invent include semantics. Does not run chr, generate keys, invent a BRID, or send a tx
- `chr_completion_help` — official CLI 0.33.x `chr help` / `chr version` / `chr --generate-completion` (bash|zsh|fish) and two-letter shortcuts. Documents skipped hidden verbs. Does not run chr, generate keys, invent a BRID, or send a tx
- `chromia_project_structure_help` — official project-structure + Rell modules layout (`create-rell-dapp` files, multi-file directory modules, recommended `app/` files, official import forms). `blockchains.<name>.module` is a module name, never a path. Does not run chr, generate keys, invent a BRID, or send a tx
- `chr_multi_signature_help` — official CLI 0.33.x read-only `chr multi-signature view` (`-f/--file` only). Skips create / sign / send and `chr tx`. Does not run chr, generate keys, invent a BRID, or send a tx
- `chromia_docs_yml_help` — official chromia.yml `docs:` keys for `chr generate docs-site` (project-config only; no theme/nav/logo)
- `write_deployment_config` — `deployments.<testnet|mainnet>` block (url, official Directory BRID, chains placeholder). CLI 0.30.0+ writes chains back. Does not invent a BRID or send a tx
- `chr_deploy_help` — official CLI 0.33.x `chr deployment create` / `update` / `inspect` plus read-only `info` / `proposal list|info` / `voterset info|list` flags (`-y`, `--key-id` reference only on create/update). Source write-back + schema-compare DROP warning. Official `database` / `test` chromia.yml snippets. Skips vote/propose/pause/resume/remove and hidden lease-info / remove-container. Does not generate keys, invent a lease/BRID, run chr, or send a tx
- `chr_node_help` — official CLI 0.33.x `chr node start` / `update` flags (`--wipe` / `--no-wipe`, Postgres 16+, default API `http://localhost:7740`). Relation to `chr build` / `chr test`. Does not start a node, generate keys, invent a BRID, run chr, or send a tx
- `chr_query_help` — official CLI 0.33.x read-only `chr query` against a local node or named deployment. Does not sign, execute a transaction, generate keys, invent a BRID, or run chr
- `vault_lease_help` — official Vault / PMC container lease workflow (testnet/mainnet). Paste a real Container ID into `deployments.<net>.container`. Official Directory BRIDs only. Does not invent a lease id, generate keys, run chr, or send a tx
- `chr_generate_client_help` — official CLI 0.33.x `chr generate client-stubs` / `graph` / `docs-site` (kotlin, typescript, javascript, python) plus official postchain-client / `@chromia/ft4` query-only wiring (`createClient`, `directoryNodeUrlPool`, `blockchainRid`). Official `docs:` YAML keys. Not a top-level `chr generate-client`. Does not run chr, generate keys, or send a tx
- `chromia_cookbook_help` — official BUILD cookbook pages for queries, client reads, and tests. Skips signed-tx recipes, cookbook-only `--local`, stale `chr test --sql-log`, and non-schema chromia.yml keys. Does not run chr, generate keys, or send a tx
- `chr_key_id_help` — official key-pair-management existing-key reference only (`--key-id` / `key.id` precedence). Does not generate or print a private key, run chr, or send a tx
- `chromia_language_clients_help` — official C# / Go / Rust / React Kit / REST query-only wiring. Skips signed txs, key generation, and C# NuGet id (not printed). JS/TS, Kotlin, Python, FT4 local reads stay on `chr_generate_client_help`. Does not run chr, generate keys, or send a tx
- `chr_library_help` — official CLI 0.33.x public `chr library` (install / list / view / versions). `chr install` alias. Library-chain vs git `libs:`. Does not invent a library-chain BRID, generate keys, run chr, or send a tx
- `chr_create_rell_dapp_help` — official CLI 0.33.x `chr create-rell-dapp` templates (`plain`, `plain-multi`, `minimal`, `plain-library`, `asset-management`) and `--devcontainer`. Does not run chr, write files, generate keys, or send a tx
- `check_dapp_project` — read-only in-memory scan of a `chromia.yml` string plus one or more `.rell` file contents; combines `validate_chromia_yml` and `check_ft4_imports`, compiles the sources (`rell_check`), and security-scans them (`rell_security_check`) when they build, into `{ok, errors[], warnings[]}`
- `check_ft4_imports` — read-only in-memory scan of `.rell` sources for forbidden FT4 production imports (`lib.ft4.admin`, `ras_open`, `ras_transfer_open`, `admin.crosschain`)
- `rell_check` — in-process Rell compilation (the same compiler the Chromia CLI embeds) with structured `{file, line, column, severity, text}` diagnostics; vendored FT4 sources make `import lib.ft4.*` compile without `chr install`
- `rell_security_check` — static security pass over compiling Rell code (banned admin modules, unauthenticated mutations, hardcoded key material, missing input validation) with line-anchored findings and fixes
- `run_rell_tests` — in-process Rell test execution (embedded CLI test runner) with per-case pass/fail results; entity/database tests need PostgreSQL via `CHROMIA_TEST_DATABASE_URL`, pure-logic tests run without it
- `chromia_rell_language_help` — official Rell definition syntax (query / operation / entity / object / struct / enum / function / module). Official Hello World query `hello_world`. Does not invent language features, run chr, generate keys, or send a tx
- `chromia_rell_types_help` — official Rell types (simple, collection, complex, iterables, sub-types, virtual). Official slug is `sub-types` (not `subtypes`, which 404s). Does not invent types, run chr, generate keys, or send a tx
- `chromia_rell_expressions_help` — official Rell values / operators / conditional / jump / lambda pages. Does not invent operators. Does not run chr, generate keys, or send a tx
- `chromia_rell_statements_help` — official Rell val/var / assignment / if/when / for/while / break/continue. Does not invent statements. Does not run chr, generate keys, or send a tx
- `chromia_rell_database_help` — official Rell at / create / update / delete syntax (runs inside operations). `create-copy` and `/database/at` 404. Does not document `chr tx` or signed send
- `chromia_rell_systemlib_help` — STARTED official systemlib (global functions, require/error, system entities, system queries). Namespaces listed, not expanded. Skips privkey/signing helpers. Does not run chr, generate keys, or send a tx

Skipped (not official public BUILD/ops help):

- `chr fetch-config` — hidden + experimental in CLI 0.33.x source (`hiddenFromHelp`, `$EXPERIMENTAL_COMMAND`). Not on the official command index.
- `chr deployment lease-info` — hidden + experimental. Not on the live deployment page.
- `chr deployment remove-container` — hidden. Economy-chain FT4 `removeContainerOperation` (signed, **no refund**). Requires keys/tx — not documented as a procedure.
- `.rell_lint` / `.rell_format` schema — no live official schema page (only default paths on `chr code lint` / `format`). Covered as flag defaults in `chr_build_help`.


### No signed transaction sending

This MCP is a query / RAG expert. It does not hold keys or submit signed transactions. Use `chromia_dapp_query` for read-only dApp queries and `get_all_transactions` to inspect history.

### ChatGPT `search` / `fetch` Tools

- `search` and `fetch` use the same RAG store as `fetch_docs` (ChatGPT-compatible id/title/url and document payloads)
- `search` is fuzzy/semantic. `fetch` is exact id match against the loaded store (stable SHA-256 of source + chunk index + text; works across restarts). A miss is not-found + `isError`.
- `fetch_docs` remains the primary documentation tool


### RAG Store Embedding Availability

**Limitation:** Documentation search requires pre-computed embeddings. Runtime loads local `embeddings.json` (`CHROMIA_EMBEDDINGS_PATH`, else the first existing of `build/embeddings.json` or `app/build/embeddings.json` relative to cwd) first, then the GitLab generic package. `java -jar` from the repo root therefore finds the Gradle-generated `app/build/embeddings.json`. Production (`mcp.chromia.dev`) does **not** clone docs or generate embeddings on boot.

**Impact:** If neither the local file nor the GitLab package is available, documentation search will not work.

**Refresh path:** Embeddings are generated by `createAndUploadEmbeddings()`, always persisted locally, and uploaded to GitLab generic packages (`embeddings/v1`) only on the upload path. This is a dedicated generation path, not a production startup fallback. No cloud upload is invented for no-upload mode.

**CI job:** `generate-embeddings` (stage `embeddings`) runs `./gradlew :app:generateEmbeddings`. It is schedule-friendly:
- `rules` allow `schedule` and `web` (Run pipeline)
- Add a GitLab pipeline schedule on the default branch (`CI/CD` → `Schedules`) for periodic refresh
- Upload uses `GITLAB_ACCESS_TOKEN` (`PRIVATE-TOKEN`) if set, otherwise `CI_JOB_TOKEN` (`JOB-TOKEN`)
- **Local generation:**
1. Review remotes in `app/src/main/resources/docs-repositories.json` (public GitHub only; do not invent unofficial URLs)
   - Each repository entry requires: `name`, `url`, `branch`, and `subdirectories`
   - Optional `{{ENV_VAR}}` placeholders in URLs resolve from the environment; current remotes are public and do not use them
2. Set `GITLAB_ACCESS_TOKEN` if uploading to GitLab packages (or `CI_JOB_TOKEN` in GitLab CI). Missing token skips upload after ingest.
3. Run:
   ```bash
   ./gradlew :app:generateEmbeddings
   ```
   or `java -jar app/build/libs/chromia-mcp-server.jar --generate-embeddings`
   For fetch+ingest only (no GitLab upload): `./gradlew :app:generateEmbeddingsNoUpload` or `--generate-embeddings-no-upload`. This writes `app/build/embeddings.json`, which `run` / `runSse` then load first.

**Docs remotes:** public GitHub only, verified default branches and existing trees (nested module paths allowed):
- `ChromiaProject/rell` (`dev`, `doc` + `rell-base` + `rell-gtx` + `rell-api-base` + `rell-api-gtx` + `rell-api-native` + `rell-api-shell`)
- `ChromiaProject/postchain` (`dev`, `doc` + `postchain-base` + `postchain-common` + `postchain-gtv` + `postchain-gtx-data` + `postchain-server` + `postchain-cli` + `postchain-spi`)
- `ChromiaProject/ft4-lib` (`development`, `doc` + `rell` + `client`)
- `ChromiaProject/directory-chain` (`dev`, `doc` + `src`)
- `ChromiaProject/postchain-eif` (`dev`, `doc`)
- `ChromiaProject/chromia-cli` (`dev`, `docs`)
- `ChromiaProject/postchain-client` (`dev`, nested `postchain-client/doc`)

The fetcher prefers sparse checkout and keeps nested paths (not only top-level `docs`/`doc`/`src`). Official `docs.chromia.com` source is still private. A public sitemap crawl (`https://docs.chromia.com/sitemap.xml`) is ingested during generation (skip-on-failure). Pages that merely mention Bitbucket in examples are kept; only real login-wall phrases are skipped. Ingest loads documentation and source text (`.md`, `.rell`, `.kt`, `.ts`, `.yml`, …) and skips binaries/secrets (`.png`, `.jks`, `.keypair`). Generation chunks with `DocumentSplitters.recursive(1000, 150)`; sitemap HTML keeps heading structure. Local no-upload ingest on 2026-08-26 19:54 IDT: 3084 documents / 25555 segments. Persisted `app/build/embeddings.json` (140.66 MiB, 25555 vectors). Published GitLab `embeddings.json` is unchanged until a tokenized upload runs.

**Standalone agent pack:** repo-root `AGENTS.md` (Codex) and `CLAUDE.md` (Claude Code). `get_prompts` category `chromia_stack` / title `Chromia stack expert` repeats the production pins. ChatGPT tools remain registered as `search` and `fetch` (Kotlin helpers renamed to `searchTool` / `fetchTool`).

**Status:** Embedding generation is a CI job plus local Gradle/`--generate-embeddings` / `--generate-embeddings-no-upload`. Runtime load order is local file, then `downloadFromRegistry()`.

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
- `FetchDocsStrategy` / `SearchDocsStrategy` / `FetchDocumentStrategy` catch the exception
- `isError` is set
- `search` structuredContent is `{results: []}`; `fetch_docs` is `{text, hits: []}`; `fetch` is `{id, error}`
- AI assistant still receives a readable error string in `content`

**If no documentation is found:**
- RAG store returns null or empty list
- Server returns: "Documentation not found for requested query!"
- AI assistant can try rephrasing query

### Error Response Format

Explorer tools (`handleResult`) set `structuredContent` to the same JSON body as `TextContent` on success. Explorer, unknown-tool, and missing-parameter errors set `structuredContent` to `{ "error": "<same message as text>" }` plus `isError`. RAG tools (`search`, `fetch`, `fetch_docs`) keep their schema-shaped `structuredContent`. Explorer tools do not declare per-tool `outputSchema`.
