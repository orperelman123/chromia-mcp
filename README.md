# Chromia MCP Server

A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps through the Chromia Explorer GraphQL API.

## Documentation

- [Introduction](./docs/Introduction.md)
- [Architecture](./docs/Architecture.md)
- [Functionality](./docs/Functional.md)
- [Setup & Development](./docs/Setup.md)
- [Deployment](./docs/Deployment.md)

## Overview

The Chromia MCP Server enables AI assistants to query and analyze Chromia blockchain data, including:

- Network statistics and analytics
- Blockchain information and metadata
- Transaction data and analysis
- Asset information and distribution
- Account activity and analytics
- Node performance monitoring
- dApp deployment information
- **Documentation retrieval and search**
- **In-process Rell compilation (`rell_check`)** — the agent feedback loop

## Documentation Tools

The server includes **RAG-powered (Retrieval-Augmented Generation) semantic documentation search** that uses vector embeddings to find relevant documentation based on meaning, not just keywords.
The AI assistant will automatically use semantic search to find and return the most relevant documentation sections.

## Rell Compile Check (`rell_check`)

Agents build reliable Rell by iterating **write → compile → fix**. The `rell_check` tool embeds the
real Rell compiler (the same `net.postchain.rell` compiler the Chromia CLI uses), so an AI assistant
can verify Rell code is 100% compilable *before* suggesting it — with no `chr` installation:

- Pass `source` (checked as `main.rell`) or `files` (`{"main.rell": "...", "lib/util.rell": "..."}`)
- Returns structured diagnostics: `ok`, `errors[]`/`warnings[]` with `file`, `line`, `column`, `text`
- **FT4 works out of the box**: the pinned FT4 v1.1.0r Rell sources are vendored into the server,
  so `import lib.ft4.accounts;` / `lib.ft4.auth` etc. compile in-process — no `chr install`.
  Applies to `rell_check`, `rell_security_check`, and `run_rell_tests` alike.
- Module args declared in the code are not required for the check; nothing is deployed and no
  network is used — sources are compiled in-process in a temp directory and deleted afterwards

## Rell Test Runner (`run_rell_tests`)

Executes `@test module;` Rell tests in-process with the embedded runner (the same engine
`chr test` wraps) and returns per-case pass/fail with error messages. The complete agent loop:

1. `rell_check` — it compiles
2. `rell_security_check` — it's secure
3. `run_rell_tests` — it behaves correctly

Pure-logic tests run with no setup. Tests that touch entities/database need PostgreSQL —
set `CHROMIA_TEST_DATABASE_URL` (jdbc url) on the server.

## Rell Security Check (`rell_security_check`)

Static security review of compiled Rell (compiles first via the embedded compiler):

- **CRITICAL** — banned admin modules (`lib.ft4.admin`, `admin.crosschain`) and open
  registration/transfer strategies (`ras_open`, `ras_transfer_open`)
- **HIGH** — operations mutating state (`create`/`update`/`delete`) without any auth check;
  hardcoded 64+ char hex literals that look like key material
- **MEDIUM** — operations with parameters but no `require(...)` input validation

Findings are line-anchored with a concrete fix each. `ok=true` = no CRITICAL/HIGH findings.
Heuristic static analysis — it does not replace a security audit. The agent loop is:
`rell_check` until it compiles → `rell_security_check` until clean → present the code.

## Hosted Options

- `CHROMIA_MCP_AUTH_TOKEN=<secret>` — require `Authorization: Bearer <secret>` on every request
  except `/health`. Off by default (ChatGPT's no-auth connector needs the open mode).
- The SSE server warms the RAG store and embedding model at startup, eliminating the ~15s
  first-search latency measured on fresh instances.
- `/health` and the MCP serverInfo report the real build version (git tag/commit), stamped by the
  Docker, CI, and release builds.

## Compact Tool Mode & `chromia_help`

61+ tool schemas cost an agent a lot of context before any work starts. Set
`CHROMIA_MCP_COMPACT_TOOLS=true` and the server advertises one `chromia_help(topic)` gateway
instead of the ~31 individual `*_help` tools (same content, one schema — call it with no topic
for the topic index). Default is the full catalog for backward compatibility.

## Install (one command)

With `gh` and `claude` CLIs available:

```bash
node scripts/install.mjs
```

Downloads the latest released `chromia-mcp-server.jar` to `~/.chromia-mcp/` and registers it as
the `chromia` MCP server in Claude Code (user scope, compact tools). Releases are produced by
`.github/workflows/release.yml` on any `v*` tag.

An npm launcher lives in `packages/npm` (`npx chromia-mcp` → one-time jar download, then stdio).
Publishing it needs an npm account: `cd packages/npm && npm publish` (make the repo/release
public first, or users must set `CHROMIA_MCP_JAR`).

## Using from ChatGPT

The hosted server works as a ChatGPT connector (Settings → Connectors → Add custom connector,
or Deep Research MCP):

- **URL**: `https://chromia-mcp.onrender.com/` (the SSE endpoint is the root path; no authentication)
- ChatGPT's connector contract requires `search` and `fetch` tools — this server ships both
  natively: `search` returns `{results: [{id, title, url}]}` over the Chromia docs RAG store and
  `fetch` returns `{id, title, text, url}` for a result id.
- In full MCP clients (developer mode), all tools are available — analytics, `chromia_help`,
  `rell_check`, `rell_security_check`, `run_rell_tests`, `scaffold_dapp`, and the rest.

## Hosted SSE Deployment

`Dockerfile` (multi-stage, runs `--sse` on `$PORT`, `/health` endpoint) plus `render.yaml`
Blueprint for a one-click Render deploy.

**Memory sizing (measured in production):** docs + analytics + RAG fit in a 512MB instance, but
the in-process Rell compiler tools (`rell_check`, `rell_security_check`, `run_rell_tests`) push
the process past 512MB and get the container OOM-killed. Either:

- run a **2GB instance** for the full toolset, or
- on small instances set
  `CHROMIA_MCP_DISABLE_TOOLS=rell_check,rell_security_check,run_rell_tests,chromia_dapp_query`
  (the on-chain query client is a second memory spike measured past 512MB) — the hosted server
  stays a rock-solid docs/analytics endpoint (ChatGPT `search`/`fetch` included) and developers
  run the compiler loop and on-chain queries through the local jar/stdio install, which has no
  such limit.

## Upstreaming

[docs/UPSTREAM.md](docs/UPSTREAM.md) lists the seven bugs found here that also affect the
official `chromaway/core-tools/chromia-mcp` (silent list-filter corruption, two live explorer
schema drifts, swallowed tool errors, stdout log corruption, context-bomb docs, dead sorting) —
patch-ready notes for a merge request from the company account.

## Testing Layers

Every push runs the full pyramid — none of these can be skipped:

1. **Unit + regression suite** (`./gradlew test`, 380+ tests) — includes `RellToolsFuzzTest`,
   a seeded property-based fuzzer that throws generated/mutated Rell at the compiler tools and
   asserts they always return structured results, never crash or hang (found a real crash on
   its first run: unterminated `operation x() {` at EOF).
2. **E2E sweep** (`scripts/e2e-sweep.mjs <url>`) — every advertised tool must respond (100%
   coverage gate), MCP resources, all help topics, the agent journey, error paths; reconnecting
   session, upstream explorer latency classified separately from real failures.
3. **Stdio smoke** (`scripts/stdio-smoke.mjs [jar|--launcher]`) — 17 checks over the transport
   Claude Code uses, run against the jar and through the npm launcher.
4. **Synthetic agent** (`scripts/synthetic-agent.mjs <url>`) — a scripted agent builds a dapp
   using only tool outputs: discovery → doc search → scaffold → plant a bug → locate it purely
   from compiler diagnostics → repair → security gate → behavior gate → validated deploy config.
   If any tool output lacks what an agent needs to act, this fails.

## Continuous Integration

- `.github/workflows/ci.yml` — tests + fat jar on every push/PR (Ubuntu, JDK 21); the jar is
  attached as a run artifact
- `.github/workflows/embeddings-refresh.yml` — weekly RAG embeddings regeneration (Mondays
  04:00 UTC, or manual dispatch); uploads to the GitLab package registry when a
  `GITLAB_ACCESS_TOKEN` repo secret is configured, otherwise attaches `embeddings.json` as an
  artifact

## Installation

### Prerequisites

- Java Development Kit v21 or higher

#### For local development With JDK

Clone the [chromia-mcp repository](https://gitlab.com/chromaway/core-tools/chromia-mcp):

```bash
git clone https://gitlab.com/chromaway/core-tools/chromia-mcp.git
cd chromia-mcp
```

Run the application using gradle run in sse mode:

```bash
./gradlew :app:runSse
```

This will start the MCP server in SSE mode on `127.0.0.1:3001` by default.

> **Note for local development**: When running locally, configure your MCP client to use `http://127.0.0.1:3001/sse` instead of `https://mcp.chromia.dev/sse`.

## Setup

The MCP server runs automatically when configured in your AI assistant.

### All AI Assistants (Cursor, Claude Desktop, JetBrains AI Assistant)

All AI assistants use the same MCP configuration format. Add the following JSON configuration:

**For production/remote server:**

```json
{
  "mcpServers": {
    "chromia-mcp": {
      "url": "https://mcp.chromia.dev/sse"
    }
  }
}
```

**For local development:**

```json
{
  "mcpServers": {
    "chromia-mcp": {
      "url": "http://127.0.0.1:3001/sse"
    }
  }
}
```

#### Platform-specific setup locations

**Cursor/Windsurf IDEs:**

1. Open Cursor settings and navigate to **MCP & Integration** → **MCP TOOLS**
2. Add the JSON configuration above

**Claude Desktop:**

1. Edit your Claude Desktop configuration file:
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows**: `%APPDATA%/Claude/claude_desktop_config.json`
2. Add the JSON configuration above

**JetBrains AI Assistant:**

1. Go to Settings → Tools → AI Assistant → MCP
2. Click on `+` to open the add dialog
3. In the dropdown switch from "Command" to "As JSON" and paste the JSON configuration above
4. Set the working directory to where this project is cloned

**ChatGPT:**

1. Enable developer mode: Go to Settings → Connectors → Advanced → Developer mode
2. Import Chromia MCP:
   - Open Workspace settings → Connectors → Create
   - Enter the following:
     - MCP Server URL: `https://mcp.chromia.dev/sse` (or `http://127.0.0.1:3001/sse` for local development)
     - Authentication: No authentication
   - Click Create
   - In connector details, new tool lists and descriptions from the MCP server will be shown

> The MCP server will be available in the MCP panel

## Rell LSP Integration

For enhanced Rell code understanding and analysis, you can also install the Chromia LSP MCP server alongside this blockchain MCP server. The LSP MCP server provides Claude with direct access to Rell language server capabilities for better code comprehension and analysis.

### Installation

```bash
npm install @chromia/chromia-lsp-mcp -g
```

### Configuration

Add the LSP MCP server to your MCP configuration

```json
{
  "mcpServers": {
    "lsp-mcp": {
      "command": "npx",
      "args": ["chromia-lsp-mcp", "0.8.8"]
    }
  }
}
```

> **Note**: The second argument `"0.8.8"` is optional and specifies the Rell LSP version to use. If omitted, the latest version will be used.

This combination provides Claude with both blockchain data access and deep Rell code understanding capabilities.

## Usage Examples

### Basic Network Statistics

> **Example Query: What is the BRID of My Neighbor Alice, and in which cluster and container is it deployed?**
>
> <details>
> <summary>Click to expand answer</summary>
>
> Based on the search results, here's the information about My Neighbor Alice blockchains on Chromia mainnet:
>
> **My Neighbor Alice Blockchain Information**
>
> I found **4 My Neighbor Alice blockchains** on mainnet:
>
> **Active/Running Blockchains:**
>
> 1. **Main My Neighbor Alice Blockchain**
>    - **BRID (Blockchain RID)**: `F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2`
>    - **Name**: `my_neighbor_alice`
>    - **Cluster**: `pink`
>    - **Container**: `611062cdf1047b265b652d229858e5a18dbf487afbb62f0f6d30ec4fd81366f9`
>    - **State**: `RUNNING` ✅
> 2. **My Neighbor Alice BS2**
>    - **BRID**: `8BC5E4DAA94410BEC640DAEE87CE5C546465C2E7E8D654668BA6267200C97560`
>    - **Name**: `my_neighbor_alice_bs2`
>    - **Cluster**: `pink`
>    - **Container**: `9f49e091ff1929bc6438a9ee9c5387ec7991eb5eda4154cd7509dd878aa9f70e`
>    - **State**: `RUNNING` ✅
>
> **Removed/Legacy Blockchains:**
>
> 3. **Legacy Instance 1**
>    - **BRID**: `9F0CCB09BBB441E840035086F5326294F51FFA92344D39FFF54B223C7547D81D`
>    - **State**: `REMOVED`
> 4. **Legacy Instance 2**
>    - **BRID**: `AEBD8E8CD9FCC268D7B5AC3DAC8855F1F9A817B8DD1A98C0F13E60D01DECB90C`
>    - **State**: `REMOVED`
>
> **Summary**
>
> The **currently active My Neighbor Alice** deployment is:
>
> - **Primary BRID**: `F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2`
> - **Deployed in**: `pink` cluster
> - **Container ID**: `611062cdf1047b265b652d229858e5a18dbf487afbb62f0f6d30ec4fd81366f9`
>
> Both active blockchains are running in the **pink cluster**, which suggests this is a multi-blockchain deployment for the My Neighbor Alice game ecosystem.
>
> </details>

```
Get network statistics and analytics for the Chromia Mainnet environment
```

```
What is the brid of my neighbor alice, and in which cluster and container it is deployed on
```

```
Get the latest 10 transactions on Mainnet with operation type "transfer"
```

```
Show activity for account ID: DEF456... across all blockchains
```

```
Which account holds most of CHR asset on mainnet
```

```
How many active users does my neighbor Alice's dApp have on mainnet?
```

```
How to register an account using FT4 open strategy in Rell, can you provide TypeScript client code as well
```

```
what is the difference between query and operation in Rell
```

```
Could you create a Rell application for managing books that includes FT4 account registration functionality
```

```
how to use Chromia CLI to deploy my newly created dapp on testnet
```

```
What are the steps to deploy a dApp on Chromia testnet using CLI?
```

```
How do I configure network settings for a Chromia blockchain?
```

### Advanced Blockchain Querying

You can query specific dApps deployed on Chromia networks and execute their custom queries:

```
Can you query all the libraries on library chain dApp on testnet?
```

**Expected workflow:**

1. The AI will search for blockchains with "library chain" on testnet
2. Retrieves the blockchain RID
3. Fetches the dApp structure to discover available queries and operations using `Postchain Client`
4. Executes the `get_all_libraries` query inside Cursor/Junie...
5. Returns the results from the library dApp

**Other examples of blockchain-specific queries:**

```
Get the deployed dApp structure for My Neighbor Alice on mainnet
```

```
Run the get_user_balance query on MNA blockchain for account ABC123
```

```
Can you list all the queries available for My Neighbor Alice on mainnet ?
```

### Rell Query Translation to SQL and vice versa

- Simple SELECT translation:

```
"Can you translate this SQL query to Rell?
SELECT name, genre FROM plays WHERE duration_minutes > 120;"
```

- JOIN queries:

```
How would I write this SQL join in Rell?
SELECT p.name, t.name, b.timestamp
FROM bookings b
JOIN performances p ON b.performance_id = p.id
JOIN theater_halls t ON p.theater_id = t.id
WHERE b.status = 'CONFIRMED';"
```

- Aggregation queries:

```
Convert this SQL aggregation to Rell syntax:
SELECT play_name, COUNT(*) as total_bookings, SUM(price) as revenue
FROM bookings b
JOIN performances p ON b.performance_id = p.id
GROUP BY play_name
HAVING COUNT(*) > 5;"
```

- Subqueries:

```
How do I write this SQL subquery in Rell?
SELECT name FROM plays
WHERE id IN (
    SELECT play_id FROM performances
    WHERE timestamp > NOW()
);"
```

- Complex conditions:

```
Translate this SQL query with multiple conditions to Rell:
SELECT DISTINCT p.name, t.name
FROM plays p
JOIN performances pf ON p.id = pf.play_id
JOIN theater_halls t ON pf.theater_id = t.id
WHERE p.genre = 'DRAMA'
AND pf.timestamp BETWEEN ? AND ?
AND EXISTS (
    SELECT 1 FROM bookings b
    WHERE b.performance_id = pf.id
);"
```

- From Rell to SQL

```
Can you translate this Rell query to SQL?
(b: bookings, p: performances) @* {
    b.performance_id == p.id
} (
    @group play_name = p.play_name,
    total_bookings = @sum 1,
    revenue = @sum b.price
) @* {
    .total_bookings > 5
}
```

When asking it's helpful to:

1. Provide the complete SQL/Rell query with proper formatting
2. Specify any relevant entity structures when working with an external project
3. Mention any special requirements (e.g., sorting, limiting, null handling)
4. Include context about the data model if it's not obvious

Example complete prompt:

```
I have these entities in my Rell code:

entity play {
    name: text;
    genre: text;
    duration: integer;
}

entity performance {
    play: play;
    date: timestamp;
    status: text;
}

Can you help me translate this SQL query to Rell?

SELECT p.name, COUNT(pf.id) as performance_count
FROM plays p
LEFT JOIN performances pf ON p.id = pf.play_id
WHERE p.genre = 'DRAMA'
GROUP BY p.name
HAVING COUNT(pf.id) > 5
ORDER BY performance_count DESC;"
```

This format provides all the necessary context for accurate translation. The AI can understand:

1. The exact data structure
2. The relationships between entities
3. The desired query logic
4. Any special requirements for the output

## Networks

The server supports multiple Chromia networks:

- **Mainnet** - Production network
- **Testnet** - Testing network

Specify the network parameter in your queries to target the appropriate environment.

## Out of scope

This server is a **query / documentation expert**. It does **not** send signed transactions, hold keys, or act as a wallet. Use `chromia_dapp_query` for read-only dApp queries. Transaction *inspection* (`get_all_transactions`) is supported; transaction *execution* is not. There is no OpenAPI spec.

MCP resources are the existing health JSON, `docs-repositories.json`, and `prompt_templates.json` (not a generated library). Prompt templates are the `get_prompts` tool; the server does not advertise MCP `prompts`.

## Local extras

- Stdio mode: `./gradlew :app:run` or `java -jar app/build/libs/chromia-mcp-server.jar --stdio`
- Fat JAR: `./gradlew :app:shadowJar` (do not run `jib` and `shadowJar` as concurrent Gradle tasks; they both write under `app/build/libs`)
- Embeddings refresh (does not run on server boot): `./gradlew :app:generateEmbeddingsNoUpload` persists `embeddings.json` to `app/build/embeddings.json` (`CHROMIA_EMBEDDINGS_PATH`). Runtime `RagStore` loads that local file first (`CHROMIA_EMBEDDINGS_PATH`, else the first existing of `build/embeddings.json` or `app/build/embeddings.json` relative to cwd, so `java -jar app/build/libs/chromia-mcp-server.jar` from the repo root finds the Gradle file) and falls back to the published GitLab package only if the local file is missing. Local no-upload ingest 2026-08-26 19:54 IDT: 3084 documents / 25555 segments with `DocumentSplitters.recursive(1000, 150)` + heading markdown. Persisted `app/build/embeddings.json` (140.66 MiB, 25555 vectors; gitignored under `build/`). Upload still needs `GITLAB_ACCESS_TOKEN`.
