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
- **Error translation (`translate_error`)** — paste any cryptic Chromia-stack error (Rell compiler, chr CLI, postchain, postgres, explorer/GraphQL, FT4) and get its meaning, likely cause, and concrete next action from a curated offline rule table (no LLM, no network). The explorer tools apply the upstream rules themselves: an explorer incident (`INTERNAL_ERROR`, testnet 400, 429, 5xx) comes back with an `UPSTREAM (rule)` line and `upstream: true` / `upstream_rule` / `next_action` fields, so an agent never mistakes an outage for its own call
- **Onboarding state machine (`onboarding_next_step`)** — report what is honestly done (`hasProject`, `compiles`, `testsPass`, `deployedTo`, `goal` local/testnet/mainnet, ...) and get exactly one next action: which MCP tool to call with which args, or the exact human step with its URL (faucet, Vault lease, `chr keygen`), plus remaining steps and human-only blockers. Grounded in live-verified facts; never emits key material
- **Deployment verification (`verify_deployment`)** — prove a deployed dapp is live with no keys: is the BRID known on the network (name or custom node URL), is the block height progressing (bounded wait), and does an optional read-only smoke query answer
- **Deployment preflight (`deployment_preflight`)** — catch every deployment problem before a human burns a lease step or signs anything: validates the `deployments.<target>` block (brid/url/container/chains), flags wrong-network BRIDs or URLs as HIGH blockers, probes the target node read-only, runs the compile + security source gate when `rell` is supplied (CRITICAL/HIGH block mainnet), and checks the production pins. `ready:true` only with zero blockers — a mainnet target without sources stays blocked until the source gate runs, other targets note the skipped gate; when ready it emits the exact `chr deployment create|update` command
- **Testnet provisioning (`provision_testnet_container`, `claim_testnet_tchr`, `deploy_testnet_chain`)** — agent-headless container leasing and dapp deployment on the Chromia TESTNET, funded by a server-held key that never appears in any output. See [Testnet Provisioning](#testnet-provisioning-agent-headless) below

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
4. `verify_guards` — the guards those tests claim to prove are **load-bearing**

`tests` selects cases the way `chr test --tests` does — globs matched against the whole
function name, `module:function`, or the module (`["test_first_deposit*"]`, or one
comma-separated string). A filter that matches nothing is `ok=false` and the notes list the
test functions it could have matched, so a typo never reads as a green run.

## Guard Verification (`verify_guards`)

A must-fail test is only evidence if it goes red when the guard it depends on is removed —
and goes red *because the attack landed*. A test that passes with the guard and still passes
without it is a fake green with a security label on it; one was written in this repository by
its maintainers, and only a mutant caught it. Every shipped template guard is held to that
standard, and `verify_guards` runs the same check on **your** dapp: name a guard line and the
must-fail test that depends on it, and the tool checks the test passes as-is, deletes (or
weakens, via `replacement`) the guard, reruns only that test, and reads *why* it failed:

- `load_bearing` — failed because the attack landed (`run_must_fail` reports "did not fail")
- `vacuous` — stayed green without the guard; the test does not exercise it
- `still_refused` — the guard's own operation still refused the attack (another `require` in it,
  or a `module_args` bound); name it in `alsoRemove`. A refusal by a *later* operation or a
  test-side assertion is the damage being noticed, and counts as `load_bearing`
- `environmental` — the mutant did not compile or lacked `moduleArgs`; proves nothing
- `baseline_red` / `guard_not_found` / `guard_ambiguous` / `test_not_found` — the inputs cannot be verified yet
- `replacement_rejected` / `also_remove_overlaps_guard` — the mutation itself is refused: a
  replacement may change nothing outside the guard's own span (no opening a comment or a string),
  and `alsoRemove` must be disjoint from the guard. Round 12 got three false verdicts through
  those two inputs; the tool now refuses them instead of guessing.

`ok` is true only when every named guard is `load_bearing`. It says nothing about guards you
did not name, and it does not replace an audit — it turns the guards you did name from a
claim into evidence.

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

## Testnet Provisioning (agent-headless)

Three tools let an agent take a dapp from sources to a live **testnet** chain with no human involved, built on live-verified Economy Chain ground truth (operations `create_container_with_subnode_image`, `faucet`, `renew_container`; queries `create_container_cost`, `get_create_container_ticket_by_transaction`, `get_leases_by_account`; the Economy Chain BRID is resolved at runtime from the Directory Chain's `get_economy_chain_rid`):

- **`provision_testnet_container`** — prices a container lease live (`create_container_cost`; e.g. 1 SCU × 2 weeks on cluster `blue` ≈ 70 tCHR), validates cluster and duration against live limits, and on a live run signs the lease with the server-held funding key. If the balance is short it first claims from the **on-chain faucet** and refuses if still short — it never spends more than the reported cost. Returns the chain-assigned container name (or `txRid` to poll via `statusTxRid` — container creation is an asynchronous ICMF ticket). Generates an **ephemeral deploy keypair per lease**; only the public key is ever returned.
- **`claim_testnet_tchr`** — tops up the funding account from the on-chain testnet faucet operation (module `economy_chain_test_claim_tchr`): 1000 tCHR per account per 7 days, FT4-authenticated, **no captcha and no website**. On cooldown it reports exactly when the next claim is possible.
- **`deploy_testnet_chain`** — gates first (`rell_security_check` CRITICAL/HIGH refuse even on testnet; `deployment_preflight` blockers refuse), then runs `chr deployment create|update` headlessly with the container's server-held deploy key via `POSTCHAIN_CLIENT_PRIVKEY`/`POSTCHAIN_CLIENT_PUBKEY`, reads back the new chain BRID and verifies it with a live height probe. Requires the `chr` CLI on the server host; if it is missing the tool names that exact blocked step instead of pretending.

**dryRun defaults to TRUE everywhere** — nothing is signed or sent without an explicit `dryRun: false`. These tools are testnet-only by construction (the network parameter does not exist; the Directory Chain BRID is the testnet one).

**Key policy (server-side only).** The funding key is resolved in order: env `CHROMIA_TESTNET_FUNDING_PRIVKEY` (raw hex) → env `CHROMIA_TESTNET_FUNDING_KEY_ID` (a key id in the chr keystore) → the chr keystore's own default `key.id` in `~/.chromia/config` (`CHROMIA_DIR` overrides the directory). Ephemeral deploy keys live in the server keystore (`CHROMIA_MCP_KEYSTORE_DIR`, default `~/.chromia-mcp/keys`); `CHROMIA_TESTNET_DEPLOY_PRIVKEY` covers containers leased elsewhere. **No private key ever appears in any tool output, note, or error** — every outgoing string is swept, and a dedicated test sweeps every output path of all three tools for key-shaped material, including error paths where the chain or `chr` echoes input back.

If no funding account is usable the tools degrade to dryRun and state the exact setup step. If the configured key's FT4 account does not exist on the Economy Chain, the tools report the precise one-time bootstrap (send ≥ 20 tCHR to the derived account id; a pending fee-strategy transfer is then completed headlessly, 10 tCHR registration fee). A registered account with **zero balance is not a blocker** — the faucet claim is the first automatic step.

## Transports

Three, from one jar:

| Transport | How to start it | Endpoint | Who uses it |
| --- | --- | --- | --- |
| stdio | `java -jar chromia-mcp-server.jar --stdio` | — | Claude Code, Cursor, most desktop clients |
| Streamable HTTP | `--sse` (or `--http`) | `POST/GET/DELETE <base>/mcp` | every current URL-based MCP client; the transport the spec moved to in 2025-03-26 |
| HTTP+SSE (legacy) | the same `--sse` server | `GET <base>/` + `POST <base>/?sessionId=…` | older connectors, including what ChatGPT's own docs still show |

One process, one port, one `/health`. The HTTP server serves **both** HTTP
transports at once, behind the same bearer token, the same CORS rule and the same
`Host` allow-list, so pointing a client at `/mcp` or at `/` is a client-side
choice, not a deployment. `--http` is an alias for `--sse` for clients that only
know the newer name.

Streamable HTTP here is **stateful**: the first `POST /mcp` (an `initialize`)
answers with an `Mcp-Session-Id`, every later request echoes it, and `DELETE /mcp`
ends the session. A `GET /mcp` opens the server-to-client stream and is kept alive
with an SSE heartbeat every 15s, the same trick the legacy endpoint uses so an idle
tunnel or load balancer does not cut a quiet session.

## Hosted Options

- `CHROMIA_MCP_PROFILE=public` (or `--profile public`) — see
  [Publish it to ChatGPT](#publish-it-to-chatgpt-serve-publicps1). Disables every tool that
  acts on the machine or uses a key. Reported in `/health` and in the MCP `serverInfo` title.
- `CHROMIA_MCP_AUTH_TOKEN=<secret>` — require `Authorization: Bearer <secret>` on every request
  except `/health`. Off by default (ChatGPT's no-auth connector needs the open mode).
- `CHROMIA_MCP_ALLOWED_HOSTS=<hosts>` — comma-separated extra `Host` values the MCP endpoints
  accept. `localhost`, `127.0.0.1`, `[::1]` and `*.trycloudflare.com` are always allowed, as is
  the interface you bind with `--host` when it is a concrete address. This is DNS-rebinding
  protection: a browser page on `evil.com` that resolves to `127.0.0.1` still sends
  `Host: evil.com`, and is refused with 403 before any tool runs. **Serving a custom domain
  needs an entry here** (`CHROMIA_MCP_ALLOWED_HOSTS=mcp.example.com`); `*` turns the check off
  and logs a warning. `/health` is exempt so an IP-addressed load-balancer probe keeps working.
  (A tunnel that terminates on this machine — the OpenAI Secure MCP Tunnel, an SSH forward —
  needs no entry: the request arrives from loopback.)
- `CHROMIA_MCP_ALLOWED_ORIGINS=<origins>` — comma-separated list of browser origins allowed by
  CORS (e.g. `https://app.example.com,http://localhost:5173`). Unset or `*` allows any origin;
  credentials are never allowed cross-origin. The `Mcp-Session-Id` header is allowed on
  requests and exposed on responses, so a browser client can hold a Streamable HTTP session.
- `CHROMIA_MCP_HTTP_MAX_SESSIONS=<n>` (default 256) and
  `CHROMIA_MCP_HTTP_SESSION_IDLE_MS=<ms>` (default 1800000) — bounds on the Streamable
  HTTP session table. A session outlives the request that made it and holds a whole
  tool registry, so on a no-auth URL `initialize` is an unauthenticated allocation:
  minting one first reclaims anything idle past the timeout, and a mint that would
  exceed the cap is refused with 503 instead of served. `DELETE /mcp` is still the
  explicit way to end a session.
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
  -- java -XX:+AutoCreateSharedArchive "-XX:SharedArchiveFile=C:\Users\Orpe7\.chromia-mcp\chromia-mcp-server.jsa" \
     -jar "C:\Users\Orpe7\chromia-mcp\app\build\libs\chromia-mcp-server.jar" --stdio
```

Or as MCP JSON for other stdio clients (Cursor, Claude Desktop, JetBrains):

```json
{
  "mcpServers": {
    "chromia": {
      "command": "java",
      "args": [
        "-XX:+AutoCreateSharedArchive",
        "-XX:SharedArchiveFile=C:\\Users\\Orpe7\\.chromia-mcp\\chromia-mcp-server.jsa",
        "-jar", "C:\\Users\\Orpe7\\chromia-mcp\\app\\build\\libs\\chromia-mcp-server.jar", "--stdio"
      ],
      "env": {
        "CHROMIA_TEST_DATABASE_URL": "jdbc:postgresql://localhost:5432/rell_mcp_tests?user=postchain&password=postchain"
      }
    }
  }
}
```

(`postchain`/`postchain` is the standard public Chromia dev credential, not a secret.
Leave the env var out entirely if you have no local PostgreSQL.)

The two `-XX` flags are optional but worth keeping: the first run writes a ~28 MB class-data
archive at exit and every later start maps it instead of loading the 280 MB jar's classes
class by class - spawn → `initialize` **1.6 s → 0.7 s** measured (2026-09-05; Java 21 only,
which the server needs anyway). `npx chromia-mcp` and `scripts/install.mjs` add them for you.

Windows PowerShell trap (hit 2026-09-05): npm's `claude.ps1` shim swallows the `--` separator,
so `claude mcp add ... -- java -XX:...` fails with `unknown option '-XX:'` (PowerShell also splits
an unquoted `-XX:+Flag` at the colon). Run the same command from `cmd.exe`, or call the exe
directly from PowerShell with the `-XX` arguments single-quoted:

```powershell
$claude = Join-Path (Split-Path (Get-Command claude).Source) 'node_modules\@anthropic-ai\claude-code\bin\claude.exe'
& $claude mcp add chromia --scope user -- java '-XX:+AutoCreateSharedArchive' `
  '-XX:SharedArchiveFile=C:\Users\Orpe7\.chromia-mcp\chromia-mcp-server.jsa' `
  -jar C:\Users\Orpe7\chromia-mcp\app\build\libs\chromia-mcp-server.jar --stdio
```

### Shape 2: local HTTP server (clients that want a URL)

For ChatGPT-style connectors, browser clients, or anything else on this machine or LAN
that connects by URL. One server, both HTTP transports (see [Transports](#transports)):

```powershell
.\serve-local.ps1        # or double-click serve-local.cmd
```

It finds the jar, auto-picks a free port (from 3001), applies the DB URL from your
environment (or the standard dev default), forces the **full tool catalog** (no compact
mode, no disabled tools - `tools/list` is the authoritative count; a number written here
would rot), gives the JVM a fixed 2 GB heap (locally there is no container
limit; measured steady state is ~1.5 GB), waits for `/health`, and prints the URL:

```
  Chromia MCP server is UP (v0.5.0, pid 12240)
    MCP SSE endpoint : http://127.0.0.1:3010/
    Health check     : http://127.0.0.1:3010/health
```

Connect any URL-based MCP client with `{ "url": "http://127.0.0.1:<port>/mcp" }`
(Streamable HTTP — prefer this), or `{ "url": "http://127.0.0.1:<port>/" }` for the
legacy SSE endpoint. Both are live on that port at the same time.
Ctrl+C stops it cleanly and frees the port. Options: `-Port 3005` pins a port,
`-BindHost 0.0.0.0` serves the LAN (set `CHROMIA_MCP_AUTH_TOKEN` first!), `-Heap 4g`,
`-NoDb`, `-Jar <path>`. Auto-start on login (optional, nothing installed by default) is
documented in [docs/Deployment.md](docs/Deployment.md#optional-auto-start-the-sse-server-on-login-windows).

## Publish it to ChatGPT (`serve-public.ps1`)

`serve-local.ps1` gives you a URL on this machine. To hand ChatGPT a URL it can
reach, you need a tunnel — and a public URL means *whoever has it can call every
advertised tool*. `serve-public.ps1` is the same server with that fact designed in:

```powershell
.\serve-public.ps1              # or double-click serve-public.cmd
.\serve-public.ps1 -NoTunnel    # dry run: start, print what WOULD be published, stop
```

It starts the server on `127.0.0.1` with `--profile public`, refuses to go further
unless `/health` confirms that profile, then runs
`cloudflared tunnel --url http://127.0.0.1:<port>` (a **quick tunnel**: no
Cloudflare account, no login, nothing to accept) and prints the
`https://<random>.trycloudflare.com` URL with both endpoints and the exact ChatGPT
steps. Ctrl+C takes the tunnel down first, then the server, and verifies the port
is free. `cloudflared` is expected on `PATH` or at
`C:\Program Files (x86)\cloudflared\cloudflared.exe` (`-Cloudflared <path>` overrides).

### What the `public` profile is

`CHROMIA_MCP_PROFILE=public` disables **exactly** the tools that act on your
machine or use a key:

| Disabled | Why |
| --- | --- |
| `local_chain_up` | boots an embedded Postchain node against your PostgreSQL and leaves it running |
| `provision_testnet_container` | generates a key pair, writes it to the deploy keystore, signs a funded transaction |
| `claim_testnet_tchr` | signs a faucet claim with your funding key |
| `deploy_testnet_chain` | reads a private key from the deploy keystore or the environment and runs the `chr` CLI |

Everything else stays, because that is the product: the compiler loop
(`rell_check`, `rell_security_check`, `run_rell_tests`, `verify_guards`,
`check_dapp_project`, `scaffold_dapp`), the docs tools (`search`, `fetch`,
`fetch_docs`, `chromia_help`), the explorer/analytics queries, `chromia_dapp_query`,
the deployment advisors (`write_deployment_config`, `deployment_preflight`,
`verify_deployment`) and `get_prompts`. A disabled tool is not merely hidden from
`tools/list`: calling it by name answers with a refusal that names the gate and a
working alternative.

`serve-public.ps1` also clears `CHROMIA_TEST_DATABASE_URL` unless you pass
`-WithDb`, so a public caller cannot run tests against your local database, and
warns when `CHROMIA_MCP_AUTH_TOKEN` is unset. `-Profile full` needs an explicit
`-IUnderstandFullProfileIsDangerous`.

The profile is not a secret: `/health` reports `"profile": "public"` and the MCP
`serverInfo.title` is `Chromia MCP Server (profile: public)`, so a connector — and
you, before you paste the URL anywhere — can see what is on the other end.
`ToolProfilesTest` pins the disabled set against a `touchesLocalMachine` marker that
every tool strategy must declare, so a tool added later cannot drift into the public
surface by omission (it does not compile until someone classifies it).

### ChatGPT connector steps

Settings → Connectors → Advanced → **Developer mode**, then Create:

- **MCP server URL**: `https://<name>.trycloudflare.com/mcp` — Streamable HTTP, prefer this.
  Use `https://<name>.trycloudflare.com/sse` only if the connector insists on SSE
  (ChatGPT's own docs still show the `/sse` shape).
- **Authentication**: *No authentication*, or API key / Bearer set to your
  `CHROMIA_MCP_AUTH_TOKEN` if you set one.
- ChatGPT calls `search` and `fetch` for research; developer mode exposes the rest.

### Alternative: OpenAI's Secure MCP Tunnel

If you would rather not publish a URL at all, OpenAI's
[`tunnel-client`](https://github.com/openai/tunnel-client) dials **out** from your
machine, so nothing is listening on the internet and the server sees loopback
traffic (no `Host` allow-list entry needed):

```bash
# the full local toolset over stdio - nothing binds a port at all
tunnel-client --mcp-command "java -jar app/build/libs/chromia-mcp-server.jar --stdio"

# or point it at a server you already have running
.\serve-local.ps1                      # in another terminal
tunnel-client --mcp-server-url http://127.0.0.1:3001/mcp
```

The stdio form is the private full-toolset shape: no port, no profile needed, and
the tools run as you. The `--mcp-server-url` form reaches whatever profile that
server was started with — use `serve-public.ps1 -NoTunnel` if you want the public
subset without a Cloudflare tunnel.

## Install (one command)

With `gh` and `claude` CLIs available:

```bash
node scripts/install.mjs
```

Downloads the latest released `chromia-mcp-server.jar` to `~/.chromia-mcp/` and registers it as
the `chromia` MCP server in Claude Code (user scope, compact tools). Releases are produced by
`.github/workflows/release.yml` on any `v*` tag.

An npm launcher lives in `packages/npm` (`npx chromia-mcp` → one-time ~280 MB jar download into
`CHROMIA_MCP_HOME`, default `~/.chromia-mcp/`, then stdio; `CHROMIA_MCP_JAR` points it at a local
jar instead). The repo and its releases are public (since 2026-09-05), so the download path is
live and CI exercises it on every push (`stdio-smoke.mjs --launcher-download`). Publishing the
package needs an npm account: `cd packages/npm && npm publish`.

## The documentation index on a local install

The RAG index (`embeddings.json`, ~141 MB, 25 588 segments as of the 2026-09-04 release) is
not in the jar. Where a boot gets it, in order:

1. `CHROMIA_EMBEDDINGS_PATH`, else `build/embeddings.json` or `app/build/embeddings.json`
   relative to the working directory (what `:app:generateEmbeddingsNoUpload` writes).
2. **The local cache**: `$CHROMIA_MCP_HOME/embeddings.bin` + `embeddings.cache.json`, default
   `~/.chromia-mcp/` — the same directory the npm launcher keeps the jar in. The downloaded
   JSON is re-encoded once into a flat binary layout (`EmbeddingStoreBinary`, 61 MB for the
   25 588-segment store) because parsing the JSON *was* the boot: measured 2026-09-05 on the
   real jar, initialize → first `fetch_docs` answer went from **16.9 s** (JSON cache) to
   **3.7 s** (binary cache; the store reads in 0.4-0.65 s and the ONNX embedding model, 2.8 s,
   now loads on its own thread alongside it). The stdio server also starts that load at
   spawn instead of on the first docs call, so what an agent actually sees on a warm cache
   is **1.4 s** for a `fetch_docs` fired the instant `initialize` returns and **0.14 s** for
   one fired 6 s later (measured 2026-09-05); stdin EOF still ends the process within
   ~0.1-0.6 s even while that load or a download is in flight. A copy younger than **7 days** is read back
   without touching the network. An older one is refreshed from the release asset and
   replaced; if that refresh fails (offline), the old copy is still served and the answer
   says so in its `index` provenance (`cached GitHub release asset … (fetched <date>)`). A
   hand-copied `embeddings.json` in the home is honoured too (dated by its mtime).
3. The published [`embeddings` release asset](https://github.com/orperelman123/chromia-mcp/releases/tag/embeddings)
   (rebuilt weekly by the `Embeddings refresh` workflow; ~15 s to first answer including the
   download and the 0.6 s re-encode), which is then kept as the cache. `CHROMIA_EMBEDDINGS_URL` can put another remote first; the year-old
   GitLab package is the last resort.

`CHROMIA_EMBEDDINGS_CACHE=off` downloads every boot and keeps nothing (CI's production-shaped
boot uses this so "a fresh install works" is never answered from a cache). The repo is public,
so no token is needed; `CHROMIA_EMBEDDINGS_TOKEN` / `GITHUB_TOKEN` are only read if it ever
goes private again (the asset is then resolved through the releases API).

**Memory for the full index:** the store needs a heap between 200 and 224 MB to load and
answer the first search (measured 2026-09-04; the file is streamed in by `EmbeddingStoreJson`,
never read whole). `serve-local.ps1` gives the JVM 2 GB; for a hand-rolled `java -jar` keep at
least `-Xmx512m`. When `search`, `fetch_docs` and `fetch` are ALL in
`CHROMIA_MCP_DISABLE_TOOLS`, startup logs `docs tools disabled - skipping index warmup` and
never loads the index.

## Hosted SSE deployment (retired 2026-09-05)

This fork was hosted on Render (`chromia-mcp.onrender.com`) as a public docs/analytics
endpoint and ChatGPT connector from 2026-09-02 to 2026-09-05. It was retired because the
service stopped picking up deploys (47 commits behind `main`, serving a year-old index while
every check in this repo was green) and because the product is the **local** install above:
the compiler loop, on-chain queries and local chains never fit a 512 MB instance anyway. The
Render service is suspended, not deleted.

What remains, in case anyone redeploys: the `Dockerfile` (multi-stage, runs `--sse` on
`$PORT`, `/health`; bakes the release asset into the image and sets `CHROMIA_EMBEDDINGS_PATH`;
`-XX:MaxRAMPercentage=50` = 256 MB heap on 512 MB, measured to fit with the store's growth as
the only margin) and `render.yaml`. The Dockerfile is still live for the
`claude-code-chromia/` image. The hosted-only pieces — `scripts/hosted-check.mjs` and the
6-hourly `hosted-check.yml` drift monitor — were removed with the service. Memory measurements
for the hosted shape are kept in [docs/Deployment.md](docs/Deployment.md). ChatGPT's `search`
/ `fetch` contract tools are still in the server; point a connector at
[`serve-public.ps1`](#publish-it-to-chatgpt-serve-publicps1), at a local server (Shape 2
above), or at ChromaWay's own `https://mcp.chromia.dev/sse`.

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
   checks always fail hard. Retry spend adapts during an outage: once the same upstream
   signature has degraded 2 checks in a run, later checks hitting it fail fast to
   WARN-UPSTREAM instead of paying full 8s retry backoffs (classification and guardrails
   unchanged). Exit codes: 0 pass, 1 real FAIL (CI retries once), 3 guardrail-only failure
   (still red, but deterministic during an outage - CI skips the retry).
3. **Stdio smoke** (`scripts/stdio-smoke.mjs [jar|--launcher|--launcher-download]`) — checks
   over the transport Claude Code uses. `--launcher` runs the same checks through the real npm
   launcher (`packages/npm/bin/chromia-mcp.mjs`) pointed at the local shadowJar via
   `CHROMIA_MCP_JAR`. `--launcher-download` is the real first run of `npx chromia-mcp`: an
   empty `CHROMIA_MCP_HOME`, no `CHROMIA_MCP_JAR`, so the launcher must download the release
   jar (`v<package.json version>`, ~280 MB) from the public GitHub release and run it;
   transport-level checks only, since that jar is the last release, not the working tree.
4. **Fresh-install boot** (`scripts/rag-eval.mjs --production-shaped --jar <jar>`) — boots
   the jar the way a first `npx chromia-mcp` does (no local `embeddings.json`, cache off, no
   token) and requires that the index it answers from is the published GitHub release asset,
   is not stale, and passes the RAG probes. Reads the `index` object `fetch_docs` puts in its
   structured output (`origin`, `generated_at`, `age_days`, `segments`, `stale`), which is also
   how any client can see over the wire which index it is being answered from. Added
   2026-09-05 after two publishes in a row left the then-hosted server on the year-old store
   with every unit test green.
5. **Nightly deep fuzz** (`scripts/fuzz-marathon.mjs <url> [iterations]`) — random-seeded
   programs fired at the live compiler tools; `.github/workflows/nightly-fuzz.yml` runs 600
   iterations every night and opens an issue with a reproducible seed on any crash, hang, or
   leaked exception.
6. **Synthetic agent** (`scripts/synthetic-agent.mjs <url>`) — a scripted agent builds a dapp
   using only tool outputs: discovery → doc search → scaffold → plant a bug → locate it purely
   from compiler diagnostics → repair → security gate → behavior gate → validated deploy config
   → onboarding names the next step → the dapp runs on a live local chain and answers a real
   REST query → deployment preflight (placeholder container blocks; a real lease id clears it)
   → chain down. If any tool output lacks what an agent needs to act, this fails; the
   local-chain leg skips with a reason when the server has no PostgreSQL.

## Continuous Integration

- `.github/workflows/ci.yml` — tests + fat jar on every push/PR (Ubuntu, JDK 21), then the
  e2e sweep, the stdio smoke, the npm launcher's real release download, and the
  fresh-install boot against the published index (no artifact upload: see the comment in
  the workflow for why)
- `.github/workflows/embeddings-refresh.yml` — weekly RAG embeddings regeneration (Mondays
  04:00 UTC, or manual dispatch). Runs `scripts/rag-eval.mjs` (40 probe questions, segment
  floor) and a size check against the published asset; only a store that passes both is
  uploaded, with `--clobber`, as `embeddings.json` on the rolling `embeddings` release, next to
  an `embeddings.provenance.json` sidecar (date, commit, segments, probe score). Uses the
  built-in `GITHUB_TOKEN` — no secret to configure.

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

> **Note for local development**: When running locally, point your MCP client at `http://127.0.0.1:3001/mcp` (Streamable HTTP) instead of `https://mcp.chromia.dev/sse`. The legacy SSE endpoint is also live on the same port, at the **root** path (`http://127.0.0.1:3001/`) rather than `/sse` — see [Transports](#transports).

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

ChatGPT reaches an MCP server over HTTP, so it needs a URL it can resolve — a
`127.0.0.1` address only works if ChatGPT is running on this machine. The full
walkthrough, including the tunnel, is
[Publish it to ChatGPT](#publish-it-to-chatgpt-serve-publicps1). In short:

1. Enable developer mode: Settings → Connectors → Advanced → Developer mode
2. Get a URL: `.\serve-public.ps1` (Cloudflare quick tunnel, `public` profile), or
   OpenAI's [`tunnel-client`](https://github.com/openai/tunnel-client) if you would
   rather not publish one.
3. Connectors → Create:
   - **MCP Server URL**: `<base>/mcp` — Streamable HTTP. `<base>/sse` also works and is
     what ChatGPT's own docs show; both are served by the same process.
     ChromaWay's hosted instance is `https://mcp.chromia.dev/sse`.
   - **Authentication**: No authentication, or Bearer/API key = your `CHROMIA_MCP_AUTH_TOKEN`.
4. Create. The connector detail page lists the tools the server advertises — that
   list is the profile you published; `/health` names it too.

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

This server is primarily a **query / documentation expert**. The only tools that sign and send transactions are the three [testnet provisioning](#testnet-provisioning-agent-headless) tools — TESTNET only, dryRun by default, funded by a server-held key that never reaches any output. Nothing here touches mainnet funds, acts as a general wallet, or executes arbitrary transactions: `chromia_dapp_query` stays read-only, transaction *inspection* (`get_all_transactions`) is supported, arbitrary transaction *execution* is not. There is no OpenAPI spec.

MCP resources are the existing health JSON, `docs-repositories.json`, and `prompt_templates.json` (not a generated library). Prompt templates are the `get_prompts` tool; the server does not advertise MCP `prompts`.

## Local extras

- Stdio mode: `./gradlew :app:run` or `java -jar app/build/libs/chromia-mcp-server.jar --stdio`
- Fat JAR: `./gradlew :app:shadowJar` (do not run `jib` and `shadowJar` as concurrent Gradle tasks; they both write under `app/build/libs`)
- Embeddings refresh (does not run on server boot): `./gradlew :app:generateEmbeddingsNoUpload` persists `embeddings.json` to `app/build/embeddings.json` (`CHROMIA_EMBEDDINGS_PATH`). Runtime `RagStore` loads that local file first (`CHROMIA_EMBEDDINGS_PATH`, else the first existing of `build/embeddings.json` or `app/build/embeddings.json` relative to cwd, so `java -jar app/build/libs/chromia-mcp-server.jar` from the repo root finds the Gradle file), then the cache `$CHROMIA_MCP_HOME/embeddings.bin` + `embeddings.cache.json` (default `~/.chromia-mcp/`; the downloaded JSON re-encoded once into `EmbeddingStoreBinary`, reused while younger than 7 days, refreshed after, still served if the refresh fails; `CHROMIA_EMBEDDINGS_CACHE=off` disables it), and only then downloads, in order: `CHROMIA_EMBEDDINGS_URL` (optional override), the `embeddings` release asset on this GitHub repo (`RagStore.GITHUB_RELEASE_URL`; the repo is public since 2026-09-05 so the plain URL works with no credentials - if it is ever made private again, set `CHROMIA_EMBEDDINGS_TOKEN` / `/etc/secrets/CHROMIA_EMBEDDINGS_TOKEN` / `GITHUB_TOKEN` and the asset is resolved through the releases API instead), then the GitLab package (`RagStore.PACKAGE_URL`). A 404, HTTP error, timeout or corrupt body at one remote moves on to the next. Local no-upload ingest 2026-08-26 19:54 IDT: 3084 documents / 25555 segments with `DocumentSplitters.recursive(1000, 150)` + heading markdown. Persisted `app/build/embeddings.json` (140.66 MiB, 25555 vectors; gitignored under `build/`).
- Index age is not silent: at load the server logs where the index came from (local path + mtime, or the remote URL + its `Last-Modified`) and its segment count. Past 120 days (`RagStore.STALE_AFTER`) it logs a WARN and every `fetch_docs` answer ends with a `NOTE: documentation index is STALE ...` line (also `index_note` in the structured output) naming the generation date and the fix. Fresh or stale, every `fetch_docs` answer carries an `index` object in its structured output (`origin`, `generated_at`, `age_days`, `segments`, `stale`) - the text stays lean, but a client can always tell which index it was answered from. Found 2026-09-04: the published GitLab package was still the 2025-10-21 build (18.8 MB), so a server without a local file - production included - answered from a year-old store. The fix is the `Embeddings refresh` workflow (Actions tab, "Run workflow"): it regenerates, gates, and publishes the release asset the server and the Docker image download first.
- The store is read as a stream (`EmbeddingStoreJson`, Gson `JsonReader`, 1000-entry batches into `InMemoryEmbeddingStore.addAll`), not with `InMemoryEmbeddingStore.fromFile`. Found 2026-09-04 under the production JVM flags: `fromFile`'s `Files.readAllBytes` on the 150 MB store failed with "Cannot reserve 150059434 bytes of direct buffer memory" (`-XX:MaxDirectMemorySize=64m`) and the server silently fell back to the stale package - a fresh asset alone would have changed nothing in production. Streaming loads the 25 823 segments in ~4.5 s with the file never in memory; the download path (`downloadFile`) streams to a temp file the same way instead of `body<ByteArray>()`. Loading needs a 200-224 MB heap for this store; the image's `-XX:MaxRAMPercentage` went 35 -> 50 accordingly (256 MB on a 512 MB instance, measured working set ~425 MB).
