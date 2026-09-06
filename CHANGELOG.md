# Changelog

## Unreleased

- **Streamable HTTP, beside SSE, from the same server**: the MCP Kotlin SDK is
  upgraded 0.7.7 -> 0.15.0 (ktor 3.5.1) and the `--sse` server (now also `--http`)
  serves `POST/GET/DELETE /mcp` alongside the existing root SSE endpoints, with the
  same bearer token, CORS rule, keepalive and `/health`. Streamable HTTP is the
  transport the MCP spec moved to and the one every current client outside ChatGPT's
  own docs speaks; SSE keeps working unchanged, so no client has to move.
  `scripts/e2e-sweep.mjs --transport http|sse` runs the whole sweep over either, and
  CI runs both against one server.
- **`public` tool profile**: `CHROMIA_MCP_PROFILE=public` / `--profile public`
  disables exactly the tools that act on the machine or use a key (`local_chain_up`,
  `provision_testnet_container`, `claim_testnet_tchr`, `deploy_testnet_chain`) and
  keeps everything else - the compiler loop, the docs tools, the explorer queries.
  It exists because a ChatGPT connector URL without auth is callable by anyone who
  has it. The set is pinned against a `touchesLocalMachine` marker that every tool
  strategy must declare (abstract, undefaulted: a new strategy does not compile
  until someone classifies it). The active profile is reported in `/health` and in
  the MCP `serverInfo` title.
- **DNS-rebinding protection on both HTTP transports**: requests whose `Host` is not
  allowed are refused with 403 before any tool runs. Loopback and
  `*.trycloudflare.com` are allowed by default (a quick tunnel's name is random and
  cannot be listed in advance, which is why the SDK's exact-hostname list could not
  be used); `CHROMIA_MCP_ALLOWED_HOSTS` adds a custom domain, `*` turns it off.
  `/health` is exempt. The SSE endpoints had no `Host` validation at all before.
- **`serve-public.ps1` / `.cmd`**: starts the server on `127.0.0.1` with the public
  profile, verifies `/health` agrees, then opens a Cloudflare quick tunnel and prints
  the public URL with the exact ChatGPT connector steps for `/mcp` and `/sse`.
  `-NoTunnel` stops before publishing anything. README documents OpenAI's Secure MCP
  Tunnel as the no-public-URL alternative.
- **ChatGPT's `search`/`fetch` contract is pinned**: `ChatGptSearchFetchContractTest`
  holds the exact wire shape OpenAI documents, in `structuredContent` and as the JSON
  string in `content[0]`.

- **Exploit corpus + coverage scoreboard**: the known exploit patterns and
  false positives from the adversarial audits now live permanently in
  `app/src/test/resources/exploit-corpus/` (30 minimal Rell samples with
  pinned expected verdicts, plus the four adversary-built dApps, their
  passing gate verdicts, and the running exploit PoCs).
  `ExploitCorpusScoreboardTest` runs `rell_security_check` over all of them
  every build and pins the gate's TRUE coverage: a rule regression, a silently
  closed gap, or a new false positive each fails the build naming the exact
  entry and the one-line scoreboard edit that credits the change.

## v0.5.0 — 2026-09-01

A full-day hardening pass: ~60 fixes found by repeated adversarial audits, a
stress/soak rig, and end-to-end sweeps. The theme throughout: the server no
longer *looks* fine while being wrong — wrong answers became errors, silent
skips became messages, and everything below was locked in with regression
tests (557 tests, up from ~380).

### Correctness — answers that were confidently wrong

- **Every FT4 balance/supply/amount query errored although the chain call
  succeeded**: `chromia_dapp_query` used a JSON encoder that cannot serialize
  Rell `big_integer` values. Big integers now come back as JSON strings.
- **The server's own recommended file layout failed all three Rell tools**:
  a header-less `.rell` file belongs to its directory module, but module names
  were derived from the filename alone, producing phantom "Module not found"
  errors. Module derivation now matches the real compiler's rules.
- Feeding `scaffold_dapp` output back into `rell_check` / `run_rell_tests`
  failed on the `src/` path prefix; paths are now normalized the same way
  everywhere, with collision detection.
- Generated `chromia.yml` pinned a Rell version (0.16.7) the Chromia CLI
  rejects; the pin is now 0.16.1 (what CLI 0.33.x actually supports) and the
  validator errors on pins above it.
- ISO-8601 date ranges were documented as validated but never actually
  checked; they are now.

### Silent data — wrong-but-plausible results became loud errors

- A wrong-typed filter argument (e.g. a string where a list belongs) was
  silently dropped, so tools like `get_asset_distribution` returned
  **network-wide data as a "filtered" success**. All 26 argument sites now
  raise named validation errors.
- Present-but-empty filters (`[]`, `[null]`, `[""]`) also returned unfiltered
  data as success; now an error saying to omit the parameter to mean "no
  filter".
- JSON-encoded-string arguments (the classic agent mistake) silently ran
  queries with **no arguments at all**; now a named error with a
  do-not-JSON-encode hint.
- Explicit JSON `null`s were silently deleted from query arguments (letting
  Rell defaults kick in); they now flow through as real nulls.
- Objects and arrays coerced to their JSON text as "string" arguments
  (searching for the literal text `{"q":"CHR"}`); now errors.
- Oversized/decimal numbers, invalid pagination values, and invalid dapp
  names now fail fast with the rule spelled out instead of being silently
  dropped or defaulted.

### Security scanner — evasions closed, false alarms removed

- `rell_security_check` missed mutations hidden in helper functions,
  paren-form `delete(u)`/`update(u)(...)`, and operations not at the start of
  a line; all are now caught (transitive mutation call graph included).
- Same-named functions across files — or in different namespaces of one
  file — could shadow each other and hide a mutation behind a benign twin;
  the merge is now conservative in both directions.
- `validate_chromia_yml` parses flow-style `{...}` mappings and quoted
  escapes, so crafted YAML can no longer hide keys from the forbidden-module
  scan (and valid flow-style configs stop failing).
- Submitted `lib/ft4` files are exempt from scanning **only if byte-identical
  to the vendored FT4 v1.1.0r copy** (line endings ignored) — a modified or
  planted file under that path is scanned like app code and flagged. Without
  the exemption, FT4's own library files self-flag (the official zip itself
  declares `ras_open` and imports `lib.ft4.admin`).
- Fewer false positives: banned module names inside string literals no longer
  flag, `lib.ft4.admin_utils` is no longer confused with `lib.ft4.admin`, and
  aliased auth imports are recognized.
- The SSE bearer-token check now uses a constant-time comparison.

### Protocol and lifecycle — zombie JVMs and corrupted streams

- **Every client disconnect leaked an orphaned JVM**: stdio servers never
  exited on stdin EOF. They now exit cleanly, and startup failures exit
  non-zero instead of pretending success to supervisors.
- **Rell `print()` in a user test wrote to raw stdout — the JSON-RPC channel
  in stdio mode** — and could corrupt the protocol mid-frame. Print output is
  now captured (16KB cap) and returned to the agent as a `prints` field.
- CORS was configured so that **every** browser origin was rejected; browser
  MCP clients can now connect, controlled by `CHROMIA_MCP_ALLOWED_ORIGINS`.
- `/health?anything` was not recognized as the public health path (401).
- `--sse` rejects misspelled option keys instead of silently ignoring them.

### Performance and robustness

- Every on-chain query built (and never closed) two HTTP clients with their
  own connection pools; clients are now cached per endpoint with bounded
  eviction and an in-flight grace period.
- A pathological helper chain could pin an IO thread for hours in the
  security check (O(N^3) regex compilation); now a single pass with a 2 MiB
  total-source cap across the Rell tools.
- Concurrent database-backed `run_rell_tests` calls on one server corrupted
  each other's schema; they are now serialized per instance (pure-logic runs
  stay parallel).
- Runaway test runs are bounded: abandoned runners are capped at 4 (then runs
  are refused with a clear restart message), and the 90s per-run timeout can
  be tightened via `CHROMIA_MCP_TEST_TIMEOUT_SECONDS`.
- Docs-search index load failures now retry with a cooldown and report "index
  unavailable" instead of returning empty results forever.
- A new permanent stress/soak harness (`scripts/stress-soak.mjs`) validated
  the result: ~59,400 tool calls across concurrency bursts, a memory-ceiling
  run, a 20-minute soak, and runaway-test abuse — zero failures, zero
  protocol errors.

### Agent experience and docs

- **35,766 junk "leftover" tokens** — noise left in the help prose by an
  earlier automated salvage — were stripped from the 39 help files agents
  actually read, and **394 gibberish `leftover_*` payload keys** were renamed
  to honest field names.
- `run_rell_tests` reports captured `print()` output and says so when a
  `@test` module contains no test functions.
- Tool schemas now say paths are source-root relative; help content no longer
  calls the 0.16.7 source tag a "pin"; stale advice corrected across
  `Functional.md` and the prompt templates.
- `render.yaml` stopped claiming the server fits in 512MB; it provisions the
  standard (2GB) plan the process actually needs.

### Upgrading to 0.5.0

- **New env knobs** (both optional):
  - `CHROMIA_MCP_ALLOWED_ORIGINS` — comma-separated browser origins allowed
    by CORS; unset or `*` allows any origin (credentials never allowed
    cross-origin).
  - `CHROMIA_MCP_TEST_TIMEOUT_SECONDS` — tighten-only override (1..90) of the
    per-call `run_rell_tests` execution timeout; invalid values fall back to
    90.
- **Renamed help payload keys**: agents or scripts reading the old
  `leftover_*` / `leftover_official_*` JSON keys from help-tool payloads must
  switch to the new names (the payloads themselves say what each field is).
- **Rell version pin**: generated and validated `chromia.yml` now pins
  `rellVersion: 0.16.1`; pins above that are rejected because the Chromia CLI
  cannot build them.
- **Hosting**: the Render Blueprint now requests the standard (2GB) plan.
  On smaller instances keep
  `CHROMIA_MCP_DISABLE_TOOLS=rell_check,rell_security_check,run_rell_tests,chromia_dapp_query`
  set, as before.

Earlier releases (v0.4.0 and before) predate this changelog; see the git
history and GitHub Releases for their contents.
