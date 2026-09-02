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
- **Local chain (`local_chain_up`)** — a real queryable Chromia chain from Rell sources: embedded Postchain node + PostgreSQL, REST API on localhost, zero keys, bounded TTL
- **Error translation (`translate_error`)** — paste any cryptic Chromia-stack error (Rell compiler, chr CLI, postchain, postgres, explorer/GraphQL, FT4) and get its meaning, likely cause, and concrete next action from a curated offline rule table (no LLM, no network)
- **Onboarding state machine (`onboarding_next_step`)** — report what is honestly done (`hasProject`, `compiles`, `testsPass`, `deployedTo`, `goal` local/testnet/mainnet, ...) and get exactly one next action: which MCP tool to call with which args, or the exact human step with its URL (faucet, Vault lease, `chr keygen`), plus remaining steps and human-only blockers. Grounded in live-verified facts; never emits key material
- **Deployment verification (`verify_deployment`)** — prove a deployed dapp is live with no keys: is the BRID known on the network (name or custom node URL), is the block height progressing (bounded wait), and does an optional read-only smoke query answer
- **Deployment preflight (`deployment_preflight`)** — catch every deployment problem before a human burns a lease step or signs anything: validates the `deployments.<target>` block (brid/url/container/chains), flags wrong-network BRIDs or URLs as HIGH blockers, probes the target node read-only, runs the compile + security source gate when `rell` is supplied (CRITICAL/HIGH block mainnet), and checks the production pins. `ready:true` only with zero blockers — a mainnet target without sources stays blocked until the source gate runs, other targets note the skipped gate; when ready it emits the exact `chr deployment create|update` command

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
- **Submitting your own `lib/ft4` tree**: if the `files` map contains any `lib/ft4/` path, the
  vendored copy is skipped entirely and your tree is compiled instead (the result notes say so).
  Scanning is hash-gated: a `lib/ft4/` file byte-identical to the vendored v1.1.0r copy (line
  endings ignored) is exempt from import/security findings; a file that differs is scanned like
  app code, with a differs-from-vendored note. App files are always fully scanned.
- Module args declared in the code are not required for the check; nothing is deployed and no
  network is used — sources are compiled in-process in a temp directory and deleted afterwards

## Rell Test Runner (`run_rell_tests`)

Executes `@test module;` Rell tests in-process with the embedded runner (the same engine
`chr test` wraps) and returns per-case pass/fail with error messages. The complete agent loop:

1. `rell_check` — it compiles
2. `rell_security_check` — it's secure
3. `run_rell_tests` — it behaves correctly

Pure-logic tests run with no setup. Tests that touch entities/database need PostgreSQL —
set `CHROMIA_TEST_DATABASE_URL` (jdbc url) on the server. Database-backed runs share one
schema, so one server serializes them internally; give each server *instance* its own
database (or schema) — two servers pointed at the same URL can still collide.

## Local Chain (`local_chain_up`)

Stands up a **real, queryable local Chromia chain** from Rell sources — in-process, zero keys,
zero funds, zero human steps. Compiles the sources into a blockchain configuration and runs it
on the embedded Postchain engine (the same engine `chr node start` wraps) against
`CHROMIA_TEST_DATABASE_URL`, then serves the Postchain REST subset on `127.0.0.1`
(`/brid/iid_0`, `/query/{brid}` GET+POST, `/query_gtv/{brid}`, `/tx/{brid}`,
`/tx/{brid}/{txRid}/status`) — usable with curl or any postchain client. The final step of the
agent loop: compile → secure → tested → **running**. Returns the BRID and API URL; transactions
are signed with the public Chromia CLI dev key (privkey `42`×32 — local only, never a secret).
Bounded by design: one chain at a time, auto-stop TTL (default 30 min, max 2 h), a dedicated
PostgreSQL schema (`chromia_mcp_local_chain`) wiped on every start, and shutdown with the server.
Actions: `up` (default), `status`, `down`. The database must use a byte-order collation
(`LC_COLLATE 'C.UTF-8'`, or `LC_COLLATE 'C'` + `LC_CTYPE 'en-US'` on Windows).

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
- `CHROMIA_MCP_ALLOWED_ORIGINS=<origins>` — comma-separated list of browser origins allowed by
  CORS (e.g. `https://app.example.com,http://localhost:5173`). Unset or `*` allows any origin;
  credentials are never allowed cross-origin.
- `CHROMIA_MCP_TEST_TIMEOUT_SECONDS=<1..90>` — tighten-only override of the per-call
  `run_rell_tests` execution timeout (default 90s). Useful on small instances where a runaway
  test pins a core; values outside 1..90 or non-numeric fall back to the default.
- The SSE server warms the RAG store and embedding model at startup, eliminating the ~15s
  first-search latency measured on fresh instances.
- `/health` and the MCP serverInfo report the real build version (git tag/commit), stamped by the
  Docker, CI, and release builds.

## Compact Tool Mode & `chromia_help`

61+ tool schemas cost an agent a lot of context before any work starts. Set
`CHROMIA_MCP_COMPACT_TOOLS=true` and the server advertises one `chromia_help(topic)` gateway
instead of the ~31 individual `*_help` tools (same content, one schema — call it with no topic
for the topic index). Default is the full catalog for backward compatibility.

## Run it locally (the primary path)

Local is the first-class way to run this server: no memory constraints, no disabled tools,
`local_chain_up` fully usable, no hosting cost. (The hosted service is deliberately a
reduced docs/analytics surface — see [docs/Deployment.md](docs/Deployment.md).) Both
client shapes work:

**Requirements (honest list):**

- **Java 21+** — the only hard requirement. Docs search (RAG), all analytics, `chromia_help`,
  `rell_check`, `rell_security_check`, `check_dapp_project`, `scaffold_dapp`, and pure-logic
  `run_rell_tests` all work with Java alone.
- **PostgreSQL** — only for the DB-backed tools: `run_rell_tests` on tests that touch
  entities, and `local_chain_up`. Point `CHROMIA_TEST_DATABASE_URL` at any local database
  (byte-order collation, e.g. `LC_COLLATE 'C.UTF-8'`); without it those tools refuse with a
  clean message and everything else keeps working.
- The jar: `.\gradlew.bat :app:shadowJar` builds `app/build/libs/chromia-mcp-server.jar`
  (or download a release via `node scripts/install.mjs`).

### Shape 1: stdio (Claude Code and most MCP clients)

Register the jar directly — this is the exact working registration:

```bash
claude mcp add chromia --scope user \
  --env "CHROMIA_TEST_DATABASE_URL=jdbc:postgresql://localhost:5432/rell_mcp_tests?user=postchain&password=postchain" \
  -- java -jar "C:\Users\Orpe7\chromia-mcp\app\build\libs\chromia-mcp-server.jar" --stdio
```

Or as MCP JSON for other stdio clients (Cursor, Claude Desktop, JetBrains):

```json
{
  "mcpServers": {
    "chromia": {
      "command": "java",
      "args": ["-jar", "C:\\Users\\Orpe7\\chromia-mcp\\app\\build\\libs\\chromia-mcp-server.jar", "--stdio"],
      "env": {
        "CHROMIA_TEST_DATABASE_URL": "jdbc:postgresql://localhost:5432/rell_mcp_tests?user=postchain&password=postchain"
      }
    }
  }
}
```

(`postchain`/`postchain` is the standard public Chromia dev credential, not a secret.
Leave the env var out entirely if you have no local PostgreSQL.)

### Shape 2: local SSE server (clients that want a URL)

For ChatGPT-style connectors, browser clients, or anything else on this machine or LAN
that connects by URL:

```powershell
.\serve-local.ps1        # or double-click serve-local.cmd
```

It finds the jar, auto-picks a free port (from 3001), applies the DB URL from your
environment (or the standard dev default), forces the **full 70-tool catalog** (no compact
mode, no disabled tools), gives the JVM a fixed 2 GB heap (locally there is no container
limit; measured steady state is ~1.5 GB), waits for `/health`, and prints the URL:

```
  Chromia MCP server is UP (v0.5.0, pid 12240)
    MCP SSE endpoint : http://127.0.0.1:3010/
    Health check     : http://127.0.0.1:3010/health
```

Connect any URL-based MCP client with `{ "url": "http://127.0.0.1:<port>/" }`.
Ctrl+C stops it cleanly and frees the port. Options: `-Port 3005` pins a port,
`-BindHost 0.0.0.0` serves the LAN (set `CHROMIA_MCP_AUTH_TOKEN` first!), `-Heap 4g`,
`-NoDb`, `-Jar <path>`. Auto-start on login (optional, nothing installed by default) is
documented in [docs/Deployment.md](docs/Deployment.md#optional-auto-start-the-sse-server-on-login-windows).

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

**Embeddings are baked into the image at build time:** the Docker build downloads
`embeddings.json` from the GitLab package registry (same URL as the runtime fallback;
`DockerfileEmbeddingsBakeTest` keeps the two in sync) and sets `CHROMIA_EMBEDDINGS_PATH`
so boot loads the index from disk instead of downloading it — the boot-time download+parse
spike used to OOM-crash-loop 512MB containers. If GitLab is unreachable during the image
build, the build still succeeds with a loud warning and the runtime GitLab download fallback
applies as before. To pick up refreshed embeddings, redeploy (rebuild the image).

**Skipping RAG entirely (lite config):** when `search`, `fetch_docs`, and `fetch` are ALL in
`CHROMIA_MCP_DISABLE_TOOLS`, startup logs `docs tools disabled - skipping index warmup` and
never loads the embeddings index, so a small instance pays no RAG memory at all.

**Memory sizing (measured 2026-09-02, full breakdown in
[docs/Deployment.md](docs/Deployment.md)):** with the hosted reduced surface
(`CHROMIA_MCP_DISABLE_TOOLS=rell_check,rell_security_check,run_rell_tests,chromia_dapp_query,local_chain_up`)
and the tuned JVM flags in the Dockerfile, the server measures **~240MB steady state,
~340–370MB transient peak at boot warmup, ~250MB under docs-search load** — comfortable
on a **512MB instance**. The hosted server is a rock-solid docs/analytics endpoint (ChatGPT
`search`/`fetch` included); developers run the compiler loop, on-chain queries, and local
chains through the local jar/stdio install, which has no such limit. The in-process Rell
compiler tools were measured past 512MB under load — hosting the *full* toolset needs a
**2GB instance** (and PostgreSQL for entity tests / `local_chain_up`).

## Upstreaming

[docs/UPSTREAM.md](docs/UPSTREAM.md) lists the eleven findings from this fork that also affect
the official `chromaway/core-tools/chromia-mcp` or its ecosystem (silent list-filter corruption,
live explorer schema drifts and the testnet-400 regression, swallowed tool errors, stdout log
corruption, context-bomb docs, dead sorting, the `make_gtv_gson` big_integer footgun, FT4's
self-flagging security patterns) — patch-ready notes for a merge request from the company
account.

## Testing Layers

Every push runs the full pyramid — none of these can be skipped:

1. **Unit + regression suite** (`./gradlew test`, 557 tests) — includes `RellToolsFuzzTest`,
   a seeded property-based fuzzer that throws generated/mutated Rell at the compiler tools and
   asserts they always return structured results, never crash or hang (found a real crash on
   its first run: unterminated `operation x() {` at EOF).
2. **E2E sweep** (`scripts/e2e-sweep.mjs <url>`) — every advertised tool must respond (100%
   coverage gate), MCP resources, all help topics, the agent journey, error paths; real
   behavioral checks for the newest tools (local chain up/query-over-HTTP/down,
   deployment preflight incl. the `files`-alias regression, live + bogus verify_deployment,
   the onboarding walk, translate_error on real in-sweep errors); reconnecting session;
   DB- or loopback-dependent checks degrade to SKIP with the reason. Live-network checks
   tag demonstrably third-party failures WARN-UPSTREAM instead of FAIL via an allowlisted
   classifier (`scripts/upstream-classifier.mjs`) with guardrails: all-live-warn is a FAIL,
   more than `SWEEP_MAX_UPSTREAM_WARNS` (default 8) warnings is a FAIL, and non-network
   checks always fail hard.
3. **Stdio smoke** (`scripts/stdio-smoke.mjs [jar|--launcher]`) — checks over the transport
   Claude Code uses. `--launcher` runs the same checks through the real npm launcher
   (`packages/npm/bin/chromia-mcp.mjs`) pointed at the local shadowJar via `CHROMIA_MCP_JAR`;
   the launcher's release-download path itself is not testable while the repo is private
   (it needs a public GitHub release asset), so that mode skips with the reason when no
   local jar exists instead of faking a download.
4. **Nightly deep fuzz** (`scripts/fuzz-marathon.mjs <url> [iterations]`) — random-seeded
   programs fired at the live compiler tools; `.github/workflows/nightly-fuzz.yml` runs 600
   iterations every night and opens an issue with a reproducible seed on any crash, hang, or
   leaked exception.
5. **Synthetic agent** (`scripts/synthetic-agent.mjs <url>`) — a scripted agent builds a dapp
   using only tool outputs: discovery → doc search → scaffold → plant a bug → locate it purely
   from compiler diagnostics → repair → security gate → behavior gate → validated deploy config
   → onboarding names the next step → the dapp runs on a live local chain and answers a real
   REST query → deployment preflight (placeholder container blocks; a real lease id clears it)
   → chain down. If any tool output lacks what an agent needs to act, this fails; the
   local-chain leg skips with a reason when the server has no PostgreSQL.

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

> **Note for local development**: When running locally, configure your MCP client to use `http://127.0.0.1:3001/` (this fork serves the SSE endpoint at the root path, not `/sse`) instead of `https://mcp.chromia.dev/sse`.

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
      "url": "http://127.0.0.1:3001/"
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
     - MCP Server URL: `https://mcp.chromia.dev/sse` (or `http://127.0.0.1:3001/` for local development - root path on this fork)
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
