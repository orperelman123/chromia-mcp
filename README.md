# Chromia MCP Server

[![CI](https://github.com/orperelman123/chromia-mcp/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/orperelman123/chromia-mcp/actions/workflows/ci.yml)

A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps through the Chromia Explorer GraphQL API.

The badge is `main`'s last CI verdict. Green means the whole suite ran with
**zero skips**, every failure was either fixed or PROVEN to be a third party's,
and the end-to-end sweep passed over both HTTP transports. [docs/CI.md](./docs/CI.md)
says what each of those means and how to read a red one.

## Documentation

- [Introduction](./docs/Introduction.md)
- [Architecture](./docs/Architecture.md)
- [Functionality](./docs/Functional.md)
- [Setup & Development](./docs/Setup.md)
- [Deployment](./docs/Deployment.md)
- [CI](./docs/CI.md) — what runs on which trigger, what a red in each step means, and how to reproduce one locally

## Overview

The Chromia MCP Server enables AI assistants to query and analyze Chromia blockchain data, including:

- Blockchain, asset and transaction analytics
- Blockchain information and metadata
- Transaction data and analysis
- Asset information and distribution
- Account activity and analytics
- dApp deployment information
- **Documentation retrieval and search**
- **In-process Rell compilation (`rell_check`)** — the agent feedback loop
- **Local chain (`local_chain_up`)** — a real queryable Chromia chain from Rell sources: embedded Postchain node + PostgreSQL, REST API on localhost, zero keys, bounded TTL
- **Error translation (`translate_error`)** — paste any cryptic Chromia-stack error (Rell compiler, chr CLI, postchain, postgres, explorer/GraphQL, FT4) and get its meaning, likely cause, and concrete next action from a curated offline rule table (no LLM, no network). The explorer tools apply the upstream rules themselves: an explorer incident (`INTERNAL_ERROR`, testnet 400, 429, 5xx) comes back with an `UPSTREAM (rule)` line and `upstream: true` / `upstream_rule` / `next_action` fields, so an agent never mistakes an outage for its own call
- **Onboarding state machine (`onboarding_next_step`)** — report what is honestly done (`hasProject`, `compiles`, `testsPass`, `deployedTo`, `goal` local/testnet/mainnet, ...) and get exactly one next action: which MCP tool to call with which args, or the exact human step with its URL (faucet, Vault lease, `chr keygen`), plus remaining steps and human-only blockers. Grounded in live-verified facts; never emits key material
- **Deployment verification (`verify_deployment`)** — prove a deployed dapp is live with no keys: is the BRID known on the network (name or custom node URL), is the block height progressing (bounded wait), and does an optional read-only smoke query answer
- **Deployment preflight (`deployment_preflight`)** — catch every deployment problem before a human burns a lease step or signs anything: validates the `deployments.<target>` block (brid/url/container/chains), flags wrong-network BRIDs or URLs as HIGH blockers, probes the target node read-only, runs the compile + security source gate when `rell` is supplied (CRITICAL/HIGH block mainnet), and checks the production pins. `ready:true` only with zero blockers — a mainnet target without sources stays blocked until the source gate runs, other targets note the skipped gate; when ready it emits the exact `chr deployment create|update` command
- **Testnet provisioning (`provision_testnet_container`, `claim_testnet_tchr`, `deploy_testnet_chain`)** — agent-headless container leasing and dapp deployment on the Chromia TESTNET, funded by a server-held key that never appears in any output. See [Testnet Provisioning](#testnet-provisioning-agent-headless) below

**Retired tools.** `get_network_stats`, `get_transactions_by_cluster`,
`get_blockchains_transactions` and `get_node_unavailability` were removed on 2026-09-07:
the live explorer answers `INTERNAL_ERROR` for `dashboardData` and for top-level
`groupedTransactionsByBlockchain` on every selection set, and gates `getNodeUnavailability`
behind an `X-reCAPTCHA-Token` header, with nowhere else in the schema serving the same data
(probed live; see [docs/UPSTREAM.md](docs/UPSTREAM.md) #3a and #7a). All four had a green
unit test, because a recorded fixture was answering on the explorer's behalf - which is why
this repository no longer has any. `filter_blockchains` keeps working, but its `state`
filter is refused upstream the same way (#3b).

## Documentation Tools

The server includes **RAG-powered (Retrieval-Augmented Generation) semantic documentation search** that uses vector embeddings to find relevant documentation based on meaning, not just keywords.
The AI assistant will automatically use semantic search to find and return the most relevant documentation sections.

**Documentation outranks the compiler's own sources.** The corpus mixes the docs site with the
Rell and Postchain repositories, and dense retrieval cannot tell which one an agent asking "how
do I declare `module_args`?" wants. The agent-experience audit graded ten realistic Rell
questions and two of them answered out of the Rell compiler's Kotlin *unit tests*. So:
host-language TEST sources are no longer indexed (19,281 segments, down from 25,823), and a hit
is ranked by the kind of file it came from — prose docs, release notes, Rell, config,
host-language source — inside the exact-name block and inside the semantic block. An exact
DEFINITION still wins, except in a host-language source, where `val BIG_INTEGER` in the
compiler's `rt_primitive_types.kt` was beating the page that explains `big_integer` arithmetic.

Re-asked with `node scripts/rag-audit-ten.mjs --jar <jar> --embeddings <index>`
(top hit per question, graded the way the audit graded them):

| | before | after |
|---|---|---|
| correct / partly / wrong | 3 / 5 / 2 | **6 / 3 / 1** |
| top hit is a documentation page | 4 / 10 | **10 / 10** |
| top hit is a host-language source | 3 / 10 (two of them compiler unit tests) | **0 / 10** |

Still wrong after the change: *"at-expression syntax: filter, sort and limit"* answers from
`rell-architecture.md` (how at-expressions COMPILE, not how to write one) — the corpus has no
good page for it.

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

- `load_bearing` — failed because the attack landed
- `vacuous` — stayed green without the guard; the test does not exercise it
- `still_refused` — something refused the attack anyway (another `require`, a second guard, a
  `module_args` bound); name it in `alsoRemove` if it is defence in depth
- `ambiguous_refusal` — red, and the tool **will not guess** whether that red is the attack being
  refused or the damage being noticed. Never counted as proven, and never a reason to weaken the
  test — the evidence names the shape to write
- `environmental` — the mutant did not compile or lacked `moduleArgs`; proves nothing
- `baseline_red` / `guard_not_found` / `guard_ambiguous` / `test_not_found` — the inputs cannot be verified yet
- `replacement_rejected` / `also_remove_overlaps_guard` — the mutation itself is refused: a
  replacement may change nothing outside the guard's own span (no opening a comment or a string),
  and `alsoRemove` must be disjoint from the guard. Round 12 got three false verdicts through
  those two inputs; the tool now refuses them instead of guessing.

### The two shapes

Rounds 11–15 each widened a heuristic over the runner's error text, and each was beaten one
token away — an `@extend` the call graph could not see, a `for` loop the statement splitter
could not read, a frame-less query refusal past an empty statement list. The error text a
runner happens to print is not a specification, so the tool no longer tries to classify every
red. It names the two shapes it can prove and refuses the rest.

Both shapes require the test to invoke the guard's declaration — the one it sits in, or (for a
guard in a `function`) any operation or query that reaches it through your call graph, following
name calls **and `@extend`/`@extendable`** — in **exactly one** top-level statement, directly or
through test-module helpers. That is the whole point: *which invocation refused* is the
question every beaten heuristic was guessing at, and a test with one invocation cannot pose it.

A helper may live in **any test module** of the submission, not only in the test's own file, and a
call is resolved through the calling module's own imports: `h(...)` in its own module or through
`import mod.*;`, `mod.h(...)` through `import a.b.mod;`, and `alias.h(...)` through
`import alias: a.b.mod;`. **Every import form the compiler takes** is parsed, not a subset — the
exact import `import a.b.{ x, y };`, which puts `x` and `y` in the calling module's own namespace,
and the relative spellings `import .sub;` and `import ^.sibling;` (`^^.` for two segments up),
exact forms included. There is no `as` inside `{ }`: rell 0.15.0 answers "Syntax error", so
nothing renames a definition on import. **A qualifier may also be a namespace**, and it may be a
whole dotted path: `h.f(...)` reaches `namespace h { function f }` in the calling module,
`a.b.f(...)` a nested one (declared either way), and a namespace of an imported test module is
reached through every import form — `helpers.h.f(...)` after `import tests.helpers;`,
`x.h.f(...)` after `import x: tests.helpers;`, and `h.f(...)` after `import tests.helpers.*;` or
`import tests.helpers.{ h };`. A namespace is not an import, and a scan that knew only imports
could not see such a helper at all. **A name binds the way the compiler binds it, and the tool
follows that order** rather than trying every spelling of a name: an unqualified `n(...)` names the
member of the namespace the call is *read in*, innermost first — inside `namespace h`, a bare
`f(...)` is `h.f` even when the module declares a top-level `f` — then the calling module's own
top-level declaration, then an exact import, then a wildcard one; a qualified `q.n(...)` resolves
against those enclosing namespaces first (inside `namespace a`, `b.f(...)` is `a.b.f`), then the
module's own namespaces, then its imports as above. **A namespace member has no bare spelling** —
`namespace h { function f }` called as `f(...)` from the module's top level is
`Unknown name: 'f'` — so a local namespace never shadows an `import a.b.{ f };`. Round 19
registered every helper under every *suffix* of its namespace path instead, and round 20 walked in
one declaration later: the tool read a no-op local body while the chain ran the imported one, and
certified `ok:true` on a transaction that had rolled back. **A qualifier is resolved in the calling module first** —
`import main: tests.helpers;` makes `main.take(...)` a call to the *test helper*, not to the
production module it is spelled like, and a qualifier bound to a test module is never the guard's
declaration. The chain is followed **16 calls deep**; past that, or through a helper
that calls itself, the tool says the chain is deeper than 16 calls and counts no call sites — it
never names a number it cannot stand behind. The operations of the transaction and the
`run_must_fail` are read over the statement's **whole call closure** — every helper it calls, not
only the ones that reach the guard — so a helper that quietly adds a second operation makes the
statement `ambiguous_refusal` instead of a single-operation shape A. **A helper's parameters are
bound to the caller's arguments** when that closure is flattened, positionally or by name, so
`run_it(take("a", 11))` with `function run_it(o: rell.test.op)` runs one operation rather than an
unreadable one; an argument that is not a plain call, or that builds a transaction of its own, is
left as it stands and stays unknown. **The operation a carrier's argument names is followed through
operation-returning helpers**: `.op(make())` with `function make(): rell.test.op = take("a", 11);`
carries `take`, and a chain of such helpers is followed to the end. A helper whose body builds or
runs a transaction of its own is not followed — its operations are counted by its own carriers —
and neither is one this scan cannot read, because a body it cannot read is not an operation it may
rename.

The operations themselves are counted **structurally**, not by counting `.op(`: every argument of
every `rell.test.tx(...)` and `.op(...)` in that closure is one operation, a list literal is
counted element by element, and a `rell.test.block()` of more than one transaction is not one
transaction. **A block carries operations too** — the arguments of `rell.test.block(...)` and of
its `.tx(...)` — so `rell.test.block().tx(take(...), audit(...))` is **one transaction of two
operations**, and an argument of either that builds a transaction of its own is counted by that
transaction's own carriers, never twice. There is no `.ops(` and no `.txs(`: rell 0.15.0 answers
"Type rell.test.tx has no member 'ops'" and "Type rell.test.block has no member 'txs'", so
neither is modelled. An argument that is not a call — an operation held in a variable — is **not
zero operations**, it is an unknown number of them, and unknown is `ambiguous_refusal`. So
`rell.test.tx(take(...), audit(...))` carries **two** operations though it contains no `.op(` at
all. And a statement that invokes the guard's operation without running a transaction —
`val attack = take("a", 11);` — executes nothing: an operation call in test scope only builds a
`rell.test.op`, so the red belongs to the statement that ran it.

- **Shape A, the must-fail test.** That one statement is a single-operation
  `rell.test.tx().op(<the declaration>(...)).run_must_fail(...)` — or the same one operation
  written `rell.test.tx(<the declaration>(...))`. The guard is proven only when
  removing it makes the transaction **succeed** — the runner says "did not fail". Any other red
  is the attack still being refused, whoever refused it.
- **Shape B, the must-hold test.** That one statement expects the call to **succeed** (a
  single-operation `.run()`, or a direct call to the query) and the damage is measured after it.
  A `rell.test.assert_*` failure is the damage being measured; a refusal naming one of the
  guard's own declarations is that invocation being refused; a refusal naming anything else
  happened after the attack had already landed. A frame-less error — never an operation, since an
  operation refusal always carries a frame — is attributed to the test module or to a production
  query by the string literals each owns.

Anything else is `ambiguous_refusal`, `ok:false`: a loop or a table-driven test, several invoking
statements, a transaction carrying more than that one operation, a helper with several call
sites. Rewrite the test in one of the two shapes and the same dapp answers on its own.

Two rules about the mutant itself. A `replacement`'s own `require()` messages count as refusals
exactly like the guard's: the string literals a frame-less error is attributed to are read off the
**mutant sources that ran**, so a replacement that refuses the attack is `still_refused`. And
`stillRefused` / `attackLanded` are read in **shape B only** — a shape A red either says "did not
fail" (the transaction went through, whatever you pinned) or carries the frame of the one
operation it ran, which is a declaration the guard runs in, so there is no third red left for a
caller's fragment to decide.

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

### One statement about transports

`--stdio` is the local product: one process per client, no port, the full
toolset. **`--sse` is the name of the URL-server flag, not a mode.** It is kept
because existing scripts and configs pass it; `--http` is an alias for it. The
server it starts serves **both** HTTP transports on one port at the same time:

- **Streamable HTTP at `<base>/mcp`** — `POST`/`GET`/`DELETE`. Preferred, and
  what every current URL-based MCP client should be pointed at.
- **legacy HTTP+SSE at the ROOT** — `GET <base>/` opens the stream,
  `POST <base>/?sessionId=…` delivers messages.

**This server has no `/sse` path**: `GET /sse` is a 404 here, so a connector
configured with one will fail to connect. ChromaWay's hosted instance at
`https://mcp.chromia.dev/sse` is a **different server** with its own routing;
its path says nothing about this one.

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
- The URL server (`--sse`) warms the RAG store and embedding model at startup, eliminating the ~15s
  first-search latency measured on fresh instances.
- `/health` and the MCP serverInfo report the real build version (git tag/commit), stamped by the
  Docker, CI, and release builds.

## Context economics: compact mode, `describe_tool` and a sectioned `chromia_help`

The tool catalog is what an agent pays before it does anything. Measured on the wire with
`node scripts/first-contact-bytes.mjs --jar app/build/libs/chromia-mcp-server.jar`, on the
commit this work started from and on this one (bytes of the `tools/list`, `prompts/list` and
`resources/list` payloads; ~tokens at 4 bytes/token):

| | before (b982fbb) | now |
|---|---|---|
| tools advertised, full / compact | 74 / 43 | 71 / 40 |
| `tools/list`, full | 126,463 B | **111,270 B** |
| `tools/list`, compact | 101,208 B | **24,044 B** |
| `prompts/list` | JSON-RPC error −32601 | 82 prompts (19,575 B compact) |
| `resources/list` | 579 B | 579 B |
| **first contact, compact** | **101,787 B (~25.4k tok)** | **44,198 B (~11.0k tok)** |

Three changes, each pinned by a test that measures bytes and fails above the bound
(`ContextBudgetTest`, `ToolDescriptionBudgetTest`, `ChromiaHelpSectionTest`):

- **No advertised description over 1,200 B.** The long form of the 14 that were —
  `scaffold_dapp` was 4.1 KB, `verify_guards` 5.2 KB, `rell_security_check` 3.6 KB — was
  **moved, not deleted**: `describe_tool{tool:"scaffold_dapp"}` returns it verbatim with the
  untrimmed input schema, and `chromia_help{topic:"verify_guards"}` returns the same prose.
  The test requires every sentence of the old descriptions to still be reachable by name.
- **`CHROMIA_MCP_COMPACT_TOOLS=true` now saves 76%**, not 22%. It drops the ~31 `*_help`
  schemas (the `chromia_help` gateway covers them), replaces each description with a headline
  that is a literal prefix of it, strips prose from the schemas (types, enums and `required`
  stay) and drops output schemas. `describe_tool` is always advertised — it is the way back
  to the full text.
- **`chromia_help{topic}` returns a table of contents**, not the payload: section names with
  the bytes each costs. One topic used to cost 27,288 tokens in a single call and all 31
  together 196,422. `chromia_help{topic, section}` returns one section (nothing this server
  offers is over ~6 KB — a big value is listed by its own sub-keys, array chunks or
  paragraphs, with a preview), and `section:"all"` is still the whole payload, byte-identical
  to calling the individual `*_help` tool.

Default is still the full catalog for backward compatibility.

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
  -- java -XX:+AutoCreateSharedArchive "-XX:SharedArchiveFile=C:\Users\<you>\.chromia-mcp\chromia-mcp-server.jsa" \
     -jar "C:\Users\<you>\chromia-mcp\app\build\libs\chromia-mcp-server.jar" --stdio
```

No working directory is needed or honoured: the docs index is resolved from the jar's own
location (see *The documentation index on a local install*), so the client may launch this
command from the user's project directory - as Claude Code does - and still answer from the
same store as a launch from the repo root.

Or as MCP JSON for other stdio clients (Cursor, Claude Desktop, JetBrains):

```json
{
  "mcpServers": {
    "chromia": {
      "command": "java",
      "args": [
        "-XX:+AutoCreateSharedArchive",
        "-XX:SharedArchiveFile=C:\\Users\\<you>\\.chromia-mcp\\chromia-mcp-server.jsa",
        "-jar", "C:\\Users\\<you>\\chromia-mcp\\app\\build\\libs\\chromia-mcp-server.jar", "--stdio"
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
  '-XX:SharedArchiveFile=C:\Users\<you>\.chromia-mcp\chromia-mcp-server.jsa' `
  -jar C:\Users\<you>\chromia-mcp\app\build\libs\chromia-mcp-server.jar --stdio
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
    Streamable HTTP  : http://127.0.0.1:3010/mcp
    legacy HTTP+SSE  : http://127.0.0.1:3010/
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
  Use `https://<name>.trycloudflare.com/` (the ROOT, not `/sse`) if the connector
  insists on the legacy HTTP+SSE shape ChatGPT's own docs still show. There is no
  `/sse` path on this server - pointing a connector at one 404s.
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

1. `CHROMIA_EMBEDDINGS_PATH`, else the store **beside the jar** - `<jar dir>/embeddings.json`
   or one level up, i.e. `app/build/embeddings.json` next to
   `app/build/libs/chromia-mcp-server.jar` (what `:app:generateEmbeddingsNoUpload` writes).
   **Resolved from the jar's own location, never from the process working directory**, so the
   registration above needs no `cwd` and two clients launching the same jar from two different
   directories answer from the same store. (Audit F1, 2026-09-06: they did not - the same jar
   answered from 25 823 segments or from 3 208 depending on nothing but where it was launched.)
   The cwd-relative `build/embeddings.json` lookup remains, deliberately, on the *generate*
   path only, where Gradle's JavaExec working directory decides where the file is written.
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
   without touching the network - **unless its own `generated_at` is already past the 120-day
   staleness limit**, in which case the refresh is attempted first however recently it was
   downloaded, and a download older than what is already cached never replaces it. An older one
   is refreshed from the release asset and
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

## Hosted URL deployment (retired 2026-09-05)

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
above), or at ChromaWay's own `https://mcp.chromia.dev/sse` - a different server, whose
routing is its own: on anything you start from this repository that URL is a 404.

## Upstreaming

[docs/UPSTREAM.md](docs/UPSTREAM.md) lists the eleven findings from this fork that also affect
the official `chromaway/core-tools/chromia-mcp` or its ecosystem (silent list-filter corruption,
live explorer schema drifts and the testnet-400 regression, swallowed tool errors, stdout log
corruption, context-bomb docs, dead sorting, the `make_gtv_gson` big_integer footgun, FT4's
self-flagging security patterns) — patch-ready notes for a merge request from the company
account.

## Testing Layers

Every push runs the full pyramid — none of these can be skipped:

### There are no test doubles, and the proof is structural

Not a mock, not a fake, not a stub, not a recorded response, not an `object :`
substitute for one of our own types, and no mocking framework on the classpath.

**The scan reads the compiled classes, not the source.** Until 2026-09-08 it was
eight source regexes and their own KDoc said they were "deliberately close to
`grep`". Adversary round 18 ported all eight verbatim and ran them over eight
doubles written the way a person in a hurry writes one: **six were invisible** —
an anonymous object with `by` delegation (the detector wants the supertype
followed by `{`), a nested subclass of a production *class* rather than a listed
interface, an import alias on the seam type, a supertype on the next line over an
unlisted seam, a `java.lang.reflect.Proxy` whose substitute type is not in the
source at all, and a SAM lambda on a seam the five-name lambda list omitted. The
suite really did contain no double those regexes could see, which is exactly what
that claim was worth.

`NoTestDoublesTest` reads the compiled classes and asks what each class **is**,
with three detectors that are not keyed on any spelling:

- **SUPERTYPE** — the transitive supertype closure contains a **seam**. `by`
  delegation, nesting, an import alias and a supertype on the next line all
  compile to the same supertype list; a test class extending a test base class
  that extends a seam is followed through, and so is a supertype that comes out
  of a dependency jar, through the jar's own hierarchy.
- **REFLECTION_PROXY** — the class **calls** a JDK entry point that manufactures
  an implementation, read off the constant pool as `owner#name`:
  `Proxy.newProxyInstance`, `MethodHandleProxies.asInterfaceInstance`,
  `Lookup.defineClass` / `defineHiddenClass` / `defineHiddenClassWithClassData`,
  `ClassLoader.defineClass`, `Unsafe.defineAnonymousClass`. It is a list of
  calls, not of types: `MethodHandles.lookup()` manufactures nothing and
  `javaClass.classLoader` is not `ClassLoader.defineClass`.
- **SAM_CONVERSION** — an `invokedynamic` whose call-site descriptor returns a
  substitutable single-method interface. A SAM lambda compiles to no class at
  all, so this is the only structural trace it leaves.

**A seam is derived, not listed.** It is every interface or abstract class
production's own bytecode names — its constant pool, its method and field
descriptors, and the types its locals are declared as — minus the language's and
the harness's own types. So `EmbeddingModel` is one because `RagStore` takes one,
`ContentRetriever` is one because `RagStore` builds one into a local, and a new
seam is covered the day it appears rather than the day someone remembers the
list. Who wrote the `interface` keyword does not come into it: a test answering
in langchain4j's place is a double exactly as `BagOfWordsEmbeddingModel` was. The
real library implementation is never a finding, because only the test trees are
scanned and a class out of a dependency jar is not in one.

**What gets scanned is Gradle's answer, not a string.** The `test` task hands the
JVM its own resolved runtime classpath and its own production output, and the
scan searches every directory on it that is neither a dependency jar nor
production's — `app/build/classes/kotlin/test`, `app/build/classes/java/test`
(the java plugin's convention: the test source set compiles Java too) and
`app/build/resources/test`. A class file is recognised by its **magic** and not by
a `.class` suffix, so bytes checked in under any name are read as well, and the
suite asserts the scanned set **is** the classpath's own directories, so a source
set added tomorrow cannot appear unscanned. `RepoFiles.testSources()` walks
`.java` beside `.kt` for the same reason. Every one of those five sentences is
adversary round 19 (section 3), which walked past the previous answers four
times: a named class and a SAM lambda over `ContentRetriever`, a
`MethodHandleProxies` instance of `EmbeddingModel`, and a class defined from
bytes into a directory the scan had never opened.

The twelve probes — round 18's eight and round 19's four with their control — are
kept as **a Gradle source set that is compiled and never run**
(`app/src/doubleProbes/kotlin`) — a double compiled into the test tree would
still be a double in the suite — and the scan's own suite asserts each is caught,
by name and by detector. It also asserts the source regexes still *miss* the six,
so "the regexes are enough" cannot be believed again by accident. The assertion
over the real test tree is **zero**, in both layers, and the source regexes are
kept as the cheap second layer that sees a double before it has been compiled.

This replaced a ledger of 41 doubles, each with a live check said to cover the
same path. The conversion is what proved the ledger wrong: pointed at the real
explorer, **four** of the sixteen explorer tools turned out not to work at all,
and every one had a green unit test. They are retired (see *Retired tools*
above). What a double is replaced by is one of three things, decided per site:
the real thing in process (the `chr` on PATH, the embedded Postchain node the
suite starts, the production embedding model, a real HTTP server on a real port,
a real *closed* port when a connection error is the honest outcome), the real
thing live, or **deletion** — and a deleted test takes its claim with it, in a
comment saying what now covers it, or that nothing does.

The production seams the doubles came through went with them: an interface with
one implementation and a defaulted constructor parameter is not an abstraction
once nothing is left to inject. `TxPoster`, `ProcessRunner` and `ChainGateway`
went in the first pass; `RagStore` stopped being `open` on 2026-09-08 (nothing
subclassed it in production or in the tests, so `open` was a door with no room
behind it). What is left extendable is `ChromiaRepository`, the domain port every
tool strategy is written against, and `ToolStrategy` / `BaseToolStrategy`, which
some seventy production strategies implement: those are the program's own
structure rather than injection points, and the scan **detects** them instead of
pretending they are gone.

### The live third-party tests are part of the gate, and an outage is a red

The tests that call the public explorer, the testnet Economy Chain and the real
`chr` are not a separate optional tier: the gate refuses to start unless
`CHROMIA_TEST_DATABASE_URL`, `CHROMIA_LIVE_PROVISIONING_TESTS=true` and
`CHROMIA_REQUIRE_CHR=true` are set, and `LiveEnv` turns an
enabled-but-broken resource into a **failure** rather than a skip.

So when an upstream service is down, this suite goes red. That is deliberate.
The remedy is to fix or wait for the upstream and **re-run** — never to skip the
test, never to add an allowlist entry, never to swap in a recorded answer for the
duration. A green tally that was reached by not asking is the failure mode this
whole section exists to prevent.

**That was not true until 2026-09-08, and the exception was invisible.** The live
explorer helper returned `null` whenever a tool's error text matched an upstream
marker — `internal_error` first in the list — and its callers wrote
`?: return@runBlocking`. Adversary round 18 measured `get_asset_top_holders`
answering **eight consecutive live `INTERNAL_ERROR`s**, across three real asset
ids and the example id in its own description, while the test named
"answers for a real asset" passed every time: the body left before it asserted
anything, and unlike a skip nothing counts an early return. The three live
helpers now **fail** with the third party's own words and the retry advice, they
cannot hand back a null for a caller to leave on, and `AssumptionLedgerTest` pins
both shapes (no early return in the three lines after a live assertion, no live
helper with a nullable return). While the explorer will not serve that tool the
suite is **red**, which is the rule as written.

(The e2e sweep, layer 2 below, is the one place with a softer rule, and it is
bounded: demonstrably third-party failures become `WARN-UPSTREAM` through an
allowlisted classifier, but all-live-warn is a FAIL, more than
`SWEEP_MAX_UPSTREAM_WARNS` warnings is a FAIL, and non-network checks always fail
hard.)

### Three statuses: pass, red, and UPSTREAM WARNING

**A warning is a red for the upstream, not for us; the ledger entry is the
debt.** Added 2026-09-08. The rule above — an outage reds the suite, and the
remedy is to re-run — is right about the test and wrong about the *push*: a run
whose only failures are a third party refusing to serve us has nothing in it for
anyone here to fix, and a gate that cannot say so teaches people to read reds as
noise. So the tally has three outcomes instead of two:

| status | what it means | exit code |
|---|---|---|
| **PASS** | the test ran and its claim held | 0 |
| **RED** | anything else — every skip, every failure, every unproven claim | 1 |
| **UPSTREAM WARNING** | a failure that is *proven* the third party's | 0, printed by name |

**What qualifies — all five, not any of them.** A failure is counted as
`upstream=N` only when:

1. its message begins `UPSTREAM WARNING (proven): `, which only
   `LiveEnv.upstreamOutage` writes;
2. the third party's own text matches an **allowlisted signature**:
   `INTERNAL_ERROR for <uuid>` (the explorer's own request id),
   `Request timeout has expired`, `reCAPTCHA`, a 5xx. Deliberately *not*
   allowlisted — 4xx, `Connection refused`, the bare word `timeout` — because
   every one of those can be ours;
3. **the outage is measured on a path that is not ours**: either the run's
   **independent canary** (`{ totalRewardsPaid }`, one cheap call per test JVM
   through a plain `java.net.http` client with its *own* bounds — never
   `ChromiaConfig.httpTimeouts`, never `HttpClientService`, only the URL from the
   production constant) failed **with the same signature**, so the explorer is
   down for everything, **or** the specific query has a **dated entry in
   `docs/UPSTREAM.md`** naming it, which the tally reads out of that file itself.
   A partial outage — the canary answers, one query does not — is excused by a
   written, dated ledger entry or not at all;
4. an **evidence file** exists at
   `app/build/upstream/warnings/<Class>.<method>.json` carrying the tool, the
   query, the third party's words, the canary's outcome and the ledger entry,
   timestamped **inside this run**;
5. the failure message **binds that file**, `[evidence sha256:<hex>]` over its
   bytes. The message is written by the test process from its own throwable and
   the gate recomputes the digest from the file, so a file someone else wrote
   cannot match a message it did not produce.

`scripts/gate-tally.mjs` re-checks all five rather than trusting the sentence,
and it is one implementation: `scripts/loop-gate.mjs` imports it and CI runs it
as a step of its own. The gate line becomes
`gate: tests=… failures=… errors=… skipped=0 upstream=N (names…)`, with every
warning printed alongside its evidence.

**What never qualifies.** A pass — the XML still records a `<failure>`, the
claim is still unverified, and nothing continues past `upstreamOutage` (it
returns `Nothing`). A skip — the skip count is red as it always was, and CI's
allowlist is still `set()`. A failure of our own code, however upstream it
looks. A message that claims the status with no evidence file: red, so a test
cannot excuse itself by writing a sentence. Evidence from an earlier run: red,
so last week's outage cannot cover today's failure. A test name: **there is no
allowlist of tests**, here or in CI.

**Adversary round 19 produced two fake warnings, and all three holes are shut.**
The gate never opened `docs/UPSTREAM.md`, so an entry of `999` under a heading no
document has was proof — it opens the file now and requires the entry to exist,
its heading to match exactly, and its section to be dated and to name the query.
`canaryOutcome: FAILED_OTHER` — the state `LiveEnv` itself calls "may well be
ours" — counted as a failed canary on both sides; only `FAILED_SIGNATURE` with
the *same* signature does now. And a hand-written evidence file bought the status
outright, which is what clause 5 above ends. The third hole was the deepest: the
canary shared `ChromiaConfig`, `HttpClientService` and the endpoint with the code
under test, so *our* `HttpTimeouts.requestTimeout` expiring produced ktor's
`Request timeout has expired` — an allowlisted signature — for the tool and for
the canary at once, and our own bug read as ChromaWay being down. Two of the four
allowlisted signatures are reachable from our own code that way; the independent
path is what makes them safe, and
`AssumptionLedgerTest.ourOwnRequestTimeoutThroughTheRealSeamStaysARed` measures
it end to end — production `requestTimeout` set to 1 ms through the real seam, a
real live tool call, verdict **RED**. `docs/UPSTREAM.md`, "The upstream-warning
contract", is the long form.

The bar is that high because of the paragraph above it. `get_asset_top_holders`
answered eight consecutive live `INTERNAL_ERROR`s and the suite reported eight
*passes*, because an upstream marker in the text was read as an outage. The
third status is that same judgement made honestly: measured, written down,
dated — and still a failure.

Two entries are open today: `docs/UPSTREAM.md` **#3b**
(`allBlockchains(state:)` has answered `INTERNAL_ERROR` since 2026-09-07, so
`filter_blockchains{state}` cannot be verified) and **#11** (the explorer's
index is stale since 2026-09-04 06:40Z and `blockchainAnalytics` is unbounded in
chain size — 0.6 s for a 15-transaction chain, 41.5 s for a 1.4 M one, and
observed dropping the connection at the 60 s bound). Closing an entry removes
its excuse: delete it and the test is an ordinary red again.

### What the host has to deliver (measured 2026-09-08)

`run_rell_tests` abandons a run at **90 s** (`RunRellTests.EXECUTION_TIMEOUT_SECONDS`).
That bound is a product decision - a runaway test pins a core until its loop ends -
and `CHROMIA_MCP_TEST_TIMEOUT_SECONDS` may only *tighten* it. Adversary round 18
reported that one FT4 registration plus the chain bootstrap measured **86.4 s** on
this box, so no FT4 test with two accounts could finish through the tool, and it
raised the question of whether the runner needed a schema cache, a compile cache
or a warm module.

**It does not. The measurement was of a starved machine, not of FT4.** Phase
timings, `chr test`, chr 0.29.10 / rell 0.15.0, one C.UTF-8 PostgreSQL schema per
project:

| what was run | wall | the runner's own total | per case |
|---|---|---|---|
| two entities, no FT4 anywhere, 2 cases | 22.2 s | 5.0 s | `assert_equals(1,1)` **2.0 s**; one entity-writing transaction **3.0 s** |
| the shipped `insurance` template's `main.rell`, 3 cases | 29.8 s | 13.0 s | 0 registrations **2.1 s**; the FIRST registration **8.7 s**; TWO more registrations **2.3 s** |

Read across the two rows:

- **The ~17 s that sits outside the test cases is identical with and without
  FT4** (17.2 s against 16.8 s). It is `chr`'s JVM start, the Rell compile and the
  PostgreSQL schema creation. Compiling the whole vendored FT4 + iccf tree adds
  under a second to it, so a compile cache would buy nothing.
- **A test case's floor is ~2 s**, FT4 or not - the runner's own per-case chain.
- **An FT4 account registration costs about 1 s once the JVM is warm.** The first
  one in a JVM costs ~7 s more, which is JIT, not FT4. Two accounts is ~5 s of
  work, not 86.
- `chr install` (a git clone of the two lib registries) is **21.7 s**, once per
  project, and is network rather than CPU.

And the round's own fixture, re-run byte for byte, twice:

```
2026-09-07, round 18                          73.5s + 54.5s = 128.0s
2026-09-08, idle box                          11.9s +  5.8s =  17.7s   wall 43.5s
2026-09-08, two other Gradle builds running   16.7s +  6.9s =  23.6s   wall 57.2s
```

**7.2x faster than the round - and concurrent builds account for only 1.33x of
it.** The round ran while a 24-minute Gradle test run held the box with under
2 GB free, so load is part of the story; it is not all of it, and the rest is not
attributed here rather than guessed at (a 15 W laptop that has been at full tilt
for hours also thermally throttles, and nothing in the round's recording says
what the box was doing). What IS established, three ways, is the thing the
question turned on: **86.4 s was never the price of an FT4 registration.** A
registration is about a second, a bootstrap about two, and the whole two-case
drain is under twenty.

So the host requirement, and it is a requirement rather than a nicety:

- **One JVM build at a time, and at least 2 GB free.** Measured here a second and
  third build cost a third of the runtime, which on a 59-minute suite is twenty
  minutes; the box has also produced far worse than that. An FT4 test through
  `run_rell_tests` needs about twenty seconds of work and has ninety, so a run
  that hits the bound is a signal that the host is degraded - not that the bound
  is wrong. `docs/AGENT-LANE-BRIEF.md` says this for lanes; it is the same fact.
- **A C.UTF-8 PostgreSQL, one database per worktree**, reachable at
  `CHROMIA_TEST_DATABASE_URL`. Two suites in one schema collide and it looks
  intermittent.
- **`chr` on PATH** (`CHROMIA_REQUIRE_CHR=true` makes an absent or unlaunchable
  one a failure rather than a skip).
- **Budget ~20 s of fixed cost per `chr test` invocation** before any test body
  runs, and ~2 s per case after it. A suite of many small Rell fixtures pays that
  fixed cost every time; this is why the dapp-template classes are the slowest in
  the suite.

The bound was not raised and no cache was added: the numbers say there is nothing
in our runner large enough to be worth caching (the vendored FT4 zip is 165
entries and 568 KB, unzipped per call in milliseconds), and the thing that was
actually slow was another JVM on the same box.

### The merge gate, and the two modes it has

`node scripts/loop-gate.mjs --dir <repo> --expect-min <previously verified count>`
is the only thing that certifies a branch. It forces a rerun (`--rerun-tasks`;
Gradle's up-to-date check is the largest single source of "successful" builds
that executed nothing), refuses result files older than the run it started, and
exits non-zero on any failure, any error, **any skip**, or a tally below
`--expect-min`.

**There is no skip allowlist.** `--allow-skip <Class::test>` existed until
2026-09-07 and was removed: CI's own check has always been `ALLOWED = set()`, so
the flag made the local gate accept evidence the gate that decides merges would
have refused. Two definitions of green is one gate and one bypass. Passing the
flag now fails immediately with that explanation rather than being ignored.

The same reasoning closed the quieter version of the hole: every
environment-gated test skips when its variable is unset, and those variables live
in a **gitignored** `local-test-env.properties`, so a fresh worktree skipped them
all and still printed a green tally. The gate now refuses to start unless
`CHROMIA_TEST_DATABASE_URL`, `CHROMIA_LIVE_PROVISIONING_TESTS=true` and
`CHROMIA_REQUIRE_CHR=true` are set (as real environment variables, which is CI's
path, or in that file), and `LiveEnv` escalates a broken-but-enabled resource
from a skip to a failure. `AssumptionLedgerTest` pins the eleven remaining
assumption sites with the third party each depends on.

**`--docs-only --base <last gated commit>`** is the cheap mode for a commit that
changes nothing but prose. It does not accept a list of tests — a docs-only
commit was once verified by rerunning "the five classes that read GOAL.md" when
`grep -rl GOAL.md app/src/test/kotlin` returns **six**, and nothing in the
process could have noticed. The mode derives the list instead:

1. `git diff --name-only <base>..HEAD`;
2. **refuse** unless every changed path is documentation — prose outside
   `app/`, `scripts/`, `packages/`, `gradle/`, `.github/`, `claude-code-chromia/`
   and `upstream/`. A `.md` inside those is code or test data (the exploit
   corpus's `CORPUS.md` is scored by a test), and anything else runs the full
   suite;
3. grep `app/src/test/kotlin` for each changed path and its bare file name;
4. run exactly those classes plus `ExploitCorpusScoreboardTest`, which always
   runs because the prose is part of the attack surface;
5. require every derived class to have produced results — a `--tests` filter
   that matches nothing narrows a run silently — and print the derivation in the
   gate line, so a reader can recompute it.

1. **Unit + regression suite** (`./gradlew test`, 1536 tests, zero skips) — includes `RellToolsFuzzTest`,
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

**[docs/CI.md](./docs/CI.md) is the operator's manual** — every step, what a red in it
means, how to reproduce it locally, the expected durations and where the timeout budgets
come from. `CiWorkflowDocumentationTest` reads `.github/workflows/ci.yml` and that document
and fails if they disagree, so the manual cannot go stale.

- `.github/workflows/ci.yml` — **CI**. Push to `main` and pull requests into `main`
  (Ubuntu, JDK 21, a C.UTF-8 PostgreSQL and the real `chr` CLI): the full suite, then the
  e2e sweep over **both** HTTP transports, the synthetic agent, the stdio smoke, the npm
  launcher's real release download, and the fresh-install boot against the published index.
  The verdict is `scripts/gate-tally.mjs` — the same classifier the local merge gate
  imports — not gradle's exit code, so a **proven** third-party outage is counted and named
  separately instead of being confused with a real failure. Any skip is fatal. Every run
  writes a job summary naming the failures and uploads the test report and the upstream
  evidence (the fat jar is deliberately not an artifact: see the comment in the workflow).
- `.github/workflows/release.yml` — **Release**. A `v*` tag builds the jar behind the same
  gate and attaches it to a GitHub Release. That asset is what a first `npx chromia-mcp`
  downloads.
- `.github/workflows/nightly-fuzz.yml` — **Nightly deep fuzz**. 03:00 UTC: the same gate,
  then random-seeded fuzzing of the compiler tools against a live server, with a fresh seed
  block per run. A finding opens an issue carrying the exact reproduction command.
- `.github/workflows/embeddings-refresh.yml` — **Embeddings refresh**. Weekly RAG
  embeddings regeneration (Mondays 04:00 UTC, or manual dispatch). Runs
  `scripts/rag-eval.mjs` (40 probe questions, segment floor) and
  `scripts/embeddings-gate.mjs`, which compares the SEGMENTS each configured source
  offered — indexed plus the ones the ingest rules deliberately exclude — against the
  published sidecar, per source. It compared file SIZES until 2026-09-09 and refused two
  healthy runs for five days, because audit F15 (f0ee597) had taken 6,481 segments of
  host-language test sources out of the corpus on purpose and bytes cannot tell that
  from a half-failed clone; the sizes are still printed, as information. Only a store
  that passes both gates is uploaded, with `--clobber`, as `embeddings.json` on the
  rolling `embeddings` release, next to an `embeddings.provenance.json` sidecar (date,
  commit, run, segments, probe score, size, and the per-source breakdown).

**No repository secrets are required.** Nothing references `secrets.*` or `vars.*`; the
only credential is the automatic `GITHUB_TOKEN`, and each workflow declares the narrowest
`permissions` it needs.

## Installation

### Prerequisites

- Java Development Kit v21 or higher

#### For local development With JDK

Clone the [chromia-mcp repository](https://gitlab.com/chromaway/core-tools/chromia-mcp):

```bash
git clone https://gitlab.com/chromaway/core-tools/chromia-mcp.git
cd chromia-mcp
```

Run the application as a URL server:

```bash
./gradlew :app:runSse
```

This starts the URL server on `127.0.0.1:3001` by default, serving Streamable HTTP at
`/mcp` and the legacy HTTP+SSE at the root.

> **Note for local development**: point your MCP client at `http://127.0.0.1:3001/mcp` (Streamable HTTP, preferred). The legacy HTTP+SSE endpoint is live on the same port at the **root** path (`http://127.0.0.1:3001/`); this server has no `/sse` path. `https://mcp.chromia.dev/sse` is ChromaWay's hosted server, not this one — see [Transports](#transports).

## Setup

The MCP server runs automatically when configured in your AI assistant.

### All AI Assistants (Cursor, Claude Desktop, JetBrains AI Assistant)

All AI assistants use the same MCP configuration format. Add the following JSON configuration:

**For ChromaWay's hosted server** - a different deployment from this fork. Its path is
its own routing; nothing you run from this repository answers a `/sse` URL, it 404s:

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
   - **MCP Server URL**: `<base>/mcp` — Streamable HTTP, prefer this. If the connector
     insists on the legacy HTTP+SSE shape ChatGPT's own docs show, use `<base>/` — the
     ROOT. This server serves no `/sse` path. (ChromaWay's hosted instance answers at
     `https://mcp.chromia.dev/sse`; that is a different server.)
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

MCP resources are the existing health JSON, `docs-repositories.json`, and `prompt_templates.json` (not a generated library). The prompt catalogue is served three ways: MCP `prompts/list` + `prompts/get` (the capability is advertised), the `get_prompts` tool, and `chromia://config/prompt-catalog`.

## Local extras

- Stdio mode: `./gradlew :app:run` or `java -jar app/build/libs/chromia-mcp-server.jar --stdio`
- Fat JAR: `./gradlew :app:shadowJar` (do not run `jib` and `shadowJar` as concurrent Gradle tasks; they both write under `app/build/libs`)
- Embeddings refresh (does not run on server boot): `./gradlew :app:generateEmbeddingsNoUpload` persists `embeddings.json` to `app/build/embeddings.json` (`CHROMIA_EMBEDDINGS_PATH`). Runtime `RagStore` loads that local file first (`CHROMIA_EMBEDDINGS_PATH`, else the first existing of `<jar dir>/embeddings.json` or `<jar dir>/../embeddings.json` - resolved from the jar's own location, never from the working directory, so `java -jar <anywhere>/app/build/libs/chromia-mcp-server.jar` finds the Gradle file whatever cwd the client used), then the cache `$CHROMIA_MCP_HOME/embeddings.bin` + `embeddings.cache.json` (default `~/.chromia-mcp/`; the downloaded JSON re-encoded once into `EmbeddingStoreBinary`, reused while younger than 7 days, refreshed after, still served if the refresh fails; `CHROMIA_EMBEDDINGS_CACHE=off` disables it), and only then downloads, in order: `CHROMIA_EMBEDDINGS_URL` (optional override), the `embeddings` release asset on this GitHub repo (`RagStore.GITHUB_RELEASE_URL`; the repo is public since 2026-09-05 so the plain URL works with no credentials - if it is ever made private again, set `CHROMIA_EMBEDDINGS_TOKEN` / `/etc/secrets/CHROMIA_EMBEDDINGS_TOKEN` / `GITHUB_TOKEN` and the asset is resolved through the releases API instead), then the GitLab package (`RagStore.PACKAGE_URL`). A 404, HTTP error, timeout or corrupt body at one remote moves on to the next. Local no-upload ingest 2026-08-26 19:54 IDT: 3084 documents / 25555 segments with `DocumentSplitters.recursive(1000, 150)` + heading markdown. Persisted `app/build/embeddings.json` (140.66 MiB, 25555 vectors; gitignored under `build/`).
- Index age is not silent: at load the server logs where the index came from (local path + mtime, or the remote URL + its `Last-Modified`) and its segment count. Past 120 days (`RagStore.STALE_AFTER`) it logs a WARN and every `fetch_docs` answer ends with a `NOTE: documentation index is STALE ...` line (also `index_note` in the structured output) naming the generation date and the fix. Fresh or stale, every `fetch_docs` answer carries an `index` object in its structured output (`origin`, `generated_at`, `age_days`, `segments`, `stale`) - the text stays lean, but a client can always tell which index it was answered from. Found 2026-09-04: the published GitLab package was still the 2025-10-21 build (18.8 MB), so a server without a local file - production included - answered from a year-old store. The fix is the `Embeddings refresh` workflow (Actions tab, "Run workflow"): it regenerates, gates, and publishes the release asset the server and the Docker image download first.
- The store is read as a stream (`EmbeddingStoreJson`, Gson `JsonReader`, 1000-entry batches into `InMemoryEmbeddingStore.addAll`), not with `InMemoryEmbeddingStore.fromFile`. Found 2026-09-04 under the production JVM flags: `fromFile`'s `Files.readAllBytes` on the 150 MB store failed with "Cannot reserve 150059434 bytes of direct buffer memory" (`-XX:MaxDirectMemorySize=64m`) and the server silently fell back to the stale package - a fresh asset alone would have changed nothing in production. Streaming loads the 25 823 segments in ~4.5 s with the file never in memory; the download path (`downloadFile`) streams to a temp file the same way instead of `body<ByteArray>()`. Loading needs a 200-224 MB heap for this store; the image's `-XX:MaxRAMPercentage` went 35 -> 50 accordingly (256 MB on a 512 MB instance, measured working set ~425 MB).
