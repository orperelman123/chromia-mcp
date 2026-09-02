# Upstream findings - full report, grouped by venue

Prepared 2026-09-02 from the chromia-mcp fork (salvaged from
`gitlab.com/chromaway/core-tools/chromia-mcp` at merge commit
`146777767968721ecb6c97b1905721516d3281d0`, the current `dev` HEAD as of
2026-09-02). Every claim below was re-verified on 2026-09-02 against the
upstream `dev` sources on GitLab, the live `explorer.chromia.com` API, or the
published artifacts, as stated per finding.

Status labels:

- **CONFIRMED** - we reproduced the defect and verified it exists in the
  upstream sources / artifacts as of 2026-09-02.
- **OBSERVED** - live upstream-service behavior we can demonstrate but cannot
  fix ourselves (server side).

Fork commits referenced below live in `github.com/orperelman123/chromia-mcp`.

---

## Venue A - `chromia-mcp` (gitlab.com/chromaway/core-tools/chromia-mcp)

Nine CONFIRMED code defects, all still present on `dev` HEAD
(`14677776`, 2026-01-13). A compile-verified patch series is in
`patches/chromia-mcp/` (see `mr-chromia-mcp.md` for the MR body).

### A1. GraphQL list variables serialized with `toString()` - silent empty results

**Status:** CONFIRMED (code, on `dev` HEAD).
**Where:** `app/src/main/kotlin/org/chromia/domain/GraphQLQuery.kt`, in
`toJsonObject()`: `is List<*> -> put(key, value.toString())`.
**What happens:** `listOf("FT4_USER").toString()` is `"[FT4_USER]"` - a JSON
*string*, not an array. GraphQL list-input coercion wraps a non-list value into
a single-element list, so the server filters on the literal string
`"[FT4_USER]"`, matches nothing, and answers **HTTP 200 with an empty result
set** - no error anywhere. Every list-typed filter on every analytics tool
(`accountTypes`, `brids`, `operations`, `signers`, `excludedAccounts`, ...) is
affected: results look valid and are silently wrong.
**Repro:** call any tool with a list filter (e.g. `get_all_transactions` with
`operations`) and compare against the same query sent with a real JSON array.
Verified A/B against the live API on 2026-08-30 by our e2e sweep.
**Fix:** encode `List<*>` as a `JsonArray` of primitives. Fork commit
`cdf64b2` (file `GraphQLQuery.kt`); minimal upstream patch: `0001`.
**Severity:** High - silent wrong data, the worst failure mode for an
analytics tool feeding an AI assistant.

### A2. `getAssetTopHolders` argument renamed to `excludedAccounts` in the explorer schema

**Status:** CONFIRMED (code + live schema introspection 2026-09-02).
**Where:** `app/src/main/kotlin/org/chromia/data/queries/AssetQueries.kt`,
`getAssetTopHolders()` query text uses `excludeAccounts:`.
**What happens:** live introspection of
`https://explorer.chromia.com/api/explorer-service?network=mainnet` shows
`getAssetTopHolders(assetId, limit, brids, excludeBrids, accountTypes,
excludeAccountTypes, accounts, excludedAccounts)` - the exclusion argument is
now `excludedAccounts`, while `getAssetDistribution` still uses
`excludeAccounts`. Upstream's query fails validation with `UnknownArgument`
whenever the argument is used.
**Repro:**

    curl -s -X POST 'https://explorer.chromia.com/api/explorer-service?network=mainnet' \
      -H 'Content-Type: application/json' \
      -d '{"query":"query { __schema { queryType { fields { name args { name } } } } }"}'

**Fix:** rename the argument in the top-holders query only. Fork commit
`dfd4c5d`; upstream patch `0002`.
**Severity:** Medium - hard failure of one tool argument.

### A3. Top-level `groupedTransactionsByCluster` removed from the explorer schema

**Status:** CONFIRMED (code + live check 2026-09-02).
**Where:** `app/src/main/kotlin/org/chromia/data/queries/NetworkQueries.kt`,
`getTransactionsByCluster()`.
**What happens:** `query { groupedTransactionsByCluster { cluster, count } }`
now returns `Validation error (FieldUndefined@[groupedTransactionsByCluster])`
(verified live 2026-09-02). The data still exists under `dashboardData`. The
`get_transactions_by_cluster` tool is fully broken.
**Fix:** query `dashboardData { groupedTransactionsByCluster { ... } }`
(note: the returned JSON gains one level of nesting). Fork commit `dfd4c5d`;
upstream patch `0003`.
**Severity:** Medium - one tool 100% broken.

### A4. `ToolExecutor.executeTool` swallows every error message

**Status:** CONFIRMED (code, on `dev` HEAD).
**Where:** `app/src/main/kotlin/org/chromia/tools/ToolExecutor.kt`,
`executeTool`.
**What happens:** the error `CallToolResult` carrying `it.message` is built
inside `Result.onFailure { }` - but `onFailure` returns the receiver and the
built value is discarded. The `getOrNull() ?: CallToolResult("Tool execution
failed")` fallback then serves a generic message. Example: a missing required
parameter throws `IllegalArgumentException("Missing required parameter:
assetId")` and the caller sees only "Tool execution failed".
**Fix:** use `getOrElse { e -> ... }` and include `e.message`. Fork commit
`cdf64b2`; upstream patch `0004`.
**Severity:** Medium - every tool failure becomes undiagnosable for the
calling agent.

### A5. Console logging targets stdout, corrupting MCP stdio JSON-RPC

**Status:** CONFIRMED (code, on `dev` HEAD).
**Where:** `app/src/main/resources/log4j2.properties` (Console appender has no
`target`, and log4j2's default Console target is `SYSTEM_OUT`) together with
`app/src/main/kotlin/org/chromia/App.kt` (`logger.info("Request: $request")`
and the response log on every tool call, at the enabled `info` level).
**What happens:** in stdio mode `StdioServerTransport` writes JSON-RPC to
`System.out`. Every log line the Console appender emits is interleaved into
the same stream and corrupts the protocol.
**Repro:** run the server in stdio mode and issue any tool call; the log lines
appear on stdout between JSON-RPC frames.
**Fix:** `appender.console.target=SYSTEM_ERR`. Fork commit `cdf64b2`;
upstream patch `0005`.
**Severity:** High for stdio deployments - protocol-level corruption.
(Note: an earlier internal write-up attributed this to the Ktor `Logging`
plugin; that was wrong - the base installs no Ktor Logging plugin. The
defect is the log4j2 console target. Corrected here.)

### A6. `filter_assets` sorting declared but never bound

**Status:** CONFIRMED (code, on `dev` HEAD).
**Where:** `app/src/main/kotlin/org/chromia/data/queries/AssetQueries.kt`
`filterAssets()` declares `$sortBy`/`$sortDirection` in the query text, but no
`variable(...)` call binds them; `FilterAssetsStrategy` in `ToolExecutor.kt`
never extracts them and `AssetSearchFilters` has no sorting field - yet the
tool schema advertises sorting.
**What happens:** sorting parameters are accepted and silently ignored.
**Fix:** add `SortingParams` to `AssetSearchFilters`, extract in the strategy,
bind in the query. Fork commit `cdf64b2`; upstream patch `0006`.
**Severity:** Low-medium - silent no-op of an advertised feature.

### A7. `chromia_dapp_query` fails on every `big_integer` response

**Status:** CONFIRMED (code, on `dev` HEAD; root cause verified in postchain
sources - see Venue B).
**Where:** `app/src/main/kotlin/org/chromia/data/client/PostchainClientService.kt`
line 36: `make_gtv_gson().toJsonTree(queryResult)`.
**What happens:** postchain's `make_gtv_gson()` registers a Gtv adapter with
`supportBigInteger = false`, whose BIGINTEGER branch throws
`IllegalStateException("big_integer cannot be serialized as JSON")`
(`postchain-gtv/src/main/kotlin/net/postchain/gtv/gtvjson.kt`). Any dapp
query returning a Rell `big_integer` - every FT4 balance, `total_supply`,
amount field - reports an error even though the chain answered successfully.
**Repro:** `chromia_dapp_query` against any FT4 chain, e.g. any query
returning a balance or amount.
**Fix:** use `makeStrictGtvGson()` (serializes `big_integer` as a JSON
string; the serializer is otherwise identical - both use `strict = true`,
they differ only in `supportBigInteger`). Fork commit `a54408d`; upstream
patch `0007`.
**Severity:** High - the flagship on-chain query tool fails on the most
common FT4 data.

### A8. stdio server never exits when its client disconnects

**Status:** CONFIRMED (code, on `dev` HEAD).
**Where:** `app/src/main/kotlin/org/chromia/App.kt`, `runStdioMcpServer()`.
**What happens:** the run loop waits on `done.join()`, completed only from
`server.onClose` - but the SDK's `Server.onClose` fires only on an explicit
`close()`. On stdin EOF (client gone) only the *transport* closes, so the JVM
blocks forever: one orphaned JVM per client disconnect.
**Repro:** `echo '' | java -jar chromia-mcp.jar` - the process never exits.
**Fix:** also hook `transport.onClose { done.complete() }`. Fork commit
`0d716c9`; upstream patch `0008`.
**Severity:** Medium - resource leak on every session for stdio hosts
(Claude Desktop, Cursor, etc. spawn one process per session).

### A9. CORS installed with no allowed hosts - browser MCP clients rejected

**Status:** CONFIRMED (code, on `dev` HEAD).
**Where:** `app/src/main/kotlin/org/chromia/App.kt`, `installCors()`.
**What happens:** the Ktor CORS plugin with no `anyHost()`/`allowHost(...)`
rejects every cross-origin request, so browser-based MCP clients can never
connect to the SSE server. The plugin is installed but allows nothing.
**Fix:** `anyHost()` as the minimal fix (the fork made origins configurable
via an env var - `0d716c9` - but that is policy; the one-liner unbreaks it).
Upstream patch `0009`.
**Severity:** Medium for SSE deployments serving browser clients.

---

## Venue B - postchain (gitlab.com/chromaway/core/postchain)

### B1. `make_gtv_gson()` is a default-named footgun that throws on a core Rell type

**Status:** CONFIRMED (source-verified 2026-09-02 on postchain `dev`:
`postchain-gtv/src/main/kotlin/net/postchain/gtv/gtvjson.kt` - the
`GtvType.BIGINTEGER` branch throws `IllegalStateException("big_integer cannot
be serialized as JSON")` when `supportBigInteger = false`, and
`make_gtv_gson()` builds the adapter with exactly that flag).
**What is wrong:** this is *documented* ("Does not support BigInteger" in the
KDoc), so it is a design/naming issue rather than a bug - but the
obvious-default name is the builder every client reaches for, and it throws on
`big_integer`, a first-class Rell type that appears in essentially every FT4
response (balances, supplies, amounts). Chromaway's own official `chromia-mcp`
picked it and shipped broken (Venue A, finding A7). We verified in
postchain-gtv 3.49.18 (the version resolved on our runtime classpath) that the
serializer branches of `make_gtv_gson()` and `makeStrictGtvGson()` are
otherwise identical.
**Suggestion (not a patch):** deprecate `make_gtv_gson()` in favor of an
explicit name, or make the default strict; at minimum add a loud KDoc warning
that FT4/typical dapp responses will throw.
**Severity:** Medium ecosystem-wide - a trap for every postchain-client
consumer; the failure surfaces far from its cause (query "fails" although the
chain answered).

### B2 (borderline, environment-specific). postchain 3.49.x vs postchain-client dependency conflict

**Status:** OBSERVED in our build only - verify before filing.
Mixing postchain 3.49 (constrains http4k 6.53.x / httpclient5 5.6.x, e.g. via
`rell-api-gtx`) with postchain-client (targets http4k 6.0.1 + httpclient5
5.4.x) on one classpath broke every live query with `Not in GZIP format`
(httpclient5 5.6 auto-decompresses, http4k gunzips again). Upstream
`chromia-mcp` does **not** hit this (it lacks the rell-api-gtx dependency);
we hit it only because our fork adds in-process Rell compilation. Fork commit
`53e30cf` pins http4k 6.0.1.0 + httpclient5 5.4.2. Filed here for
completeness; only worth reporting if Chromaway considers the
postchain/postchain-client version matrix supported.

---

## Venue C - Chromia Explorer service (explorer.chromia.com)

**Venue uncertain:** we found no public repository for the explorer service
(searched the `chromaway` GitLab group, 2026-09-02). These are server-side
behaviors we can demonstrate but not patch. Suggested channels: Chromia's
developer support (docs.chromia.com community links / Telegram / Discord), or
simply as context in the chromia-mcp MR so the maintainers can route them
internally. We deliberately do not guess a repo.

### C1. `getNodeUnavailability` now requires a reCAPTCHA token

**Status:** OBSERVED (live, re-verified 2026-09-02).
**Repro:**

    curl -s -X POST 'https://explorer.chromia.com/api/explorer-service?network=mainnet' \
      -H 'Content-Type: application/json' \
      -d '{"query":"query getNodeUnavailability($pubkey: String!, $startTimestamp: String!) { getNodeUnavailability(pubkey: $pubkey, startTimestamp: $startTimestamp) { blockchainRid, intervals { start, end } } }","variables":{"pubkey":"03e5c53e1f3a11d939ffab54f1d883e0d9b47e2ba1bbd52a55d61e4bd2e109e79b","startTimestamp":"1735689600000"}}'

Returns HTTP 200 with
`"reCAPTCHA verification failed: token is required. Add X-reCAPTCHA-Token
header with a valid token."`
**Impact:** every programmatic client of this query - including the official
`chromia-mcp`'s `get_node_unavailability` tool - is broken until the explorer
offers a non-browser API path (or the tool is removed/gated).

### C2. `network=testnet` returns HTTP 400 for queries that succeed on mainnet

**Status:** OBSERVED (live, re-verified 2026-09-02).
**Repro:** same body, only the query parameter differs:

    curl -s -o /dev/null -w '%{http_code}\n' -X POST \
      'https://explorer.chromia.com/api/explorer-service?network=mainnet' \
      -H 'Content-Type: application/json' -d '{"query":"query { totalRewardsPaid }"}'   # 200
    curl -s -w '\n%{http_code}\n' -X POST \
      'https://explorer.chromia.com/api/explorer-service?network=testnet' \
      -H 'Content-Type: application/json' -d '{"query":"query { totalRewardsPaid }"}'   # {"message":{}} 400

**Impact:** `chromia-mcp` advertises `'testnet'` as a valid `network` argument
on every analytics tool, so all such calls fail with an opaque error. Either
the explorer should serve testnet again (or return a real error message), or
clients should stop advertising it.

### C3. Schema-consistency note

`getAssetTopHolders` uses `excludedAccounts` while `getAssetDistribution`
uses `excludeAccounts` (introspected live 2026-09-02). Renaming one and not
the other broke API clients (finding A2); aligning the names (with a
deprecation alias) would prevent the next round of breakage. Removing
top-level `groupedTransactionsByCluster` (finding A3) was likewise a silent
breaking change for API consumers.

---

## Venue D - FT4 (gitlab.com/chromaway/ft4-lib) - informational only

### D1. FT4 v1.1.0r contains the exact patterns Rell security scanners must flag in app code

**Status:** CONFIRMED (verified in the official FT4 v1.1.0r distribution zip:
`lib/ft4/core/accounts/strategies/open/module.rell` line 22 declares
`operation ras_open`, and the test helper `lib/ft4/test/core/assets.rell`
line 3 contains `import lib.ft4.admin;`).
**What this means:** this is **not a bug in FT4** - the library legitimately
implements the open registration strategy and admin tooling. But any Rell
security scanner that bans `ras_open` / admin imports and walks a full source
tree will report CRITICALs *inside the library* whenever a project vendors
FT4 - false alarms pointing at code the user cannot change. And a path-based
exemption alone is unsafe: malicious code parked under `lib/ft4/` would dodge
the scan. Our fork exempts a `lib/ft4/` file only if it is byte-identical
(modulo line endings) to the vendored v1.1.0r copy, and scans anything that
differs (fork commits `b66f033`, `15dbaf8`).
**Venue:** uncertain - this is guidance for security-tooling authors, not an
FT4 defect. We did not prepare an issue for ft4-lib; if Chromia's docs ever
add a "writing Rell security tooling" page, this belongs there. Included here
so the knowledge is not lost.

---

## Excluded findings (fact-check failed or out of scope)

1. **"`fetch_docs` returns whole files (up to ~863KB per call)"**
   (item 6 of our internal `docs/UPSTREAM.md`). EXCLUDED: we could not
   substantiate it on 2026-09-02. The upstream RAG store downloads its
   embeddings from the GitLab package registry
   (`projects/71940508/packages/generic/embeddings/v1/embeddings.json`); the
   current artifact contains 3,208 entries whose largest text segment is
   4,027 characters (median 789) and contains no `llms-full.txt`, and
   `RagStore.query` caps results at 15 with `minScore 0.6` - worst case
   roughly 60KB, not 863KB. Whatever we measured on 2026-08-30 is not
   reproducible against today's artifact, so we do not report it.

2. **SSE session leak in the MCP Kotlin SDK's `mcp{}` Ktor plugin.** Our
   transport rewrite found that a disconnected SSE client's session is removed
   only via `Server.onClose` (explicit close only), leaking the session map
   entry (fork commit `81d4fda`, with a failing-then-passing test). We believe
   the SDK's own plugin (io.modelcontextprotocol:kotlin-sdk 0.7.7) has the
   same pattern, but we did not re-verify the SDK sources, and the venue
   (github.com/modelcontextprotocol/kotlin-sdk) is not Chromaway. Not filed.

3. **Everything fork-specific**: the new tools (rell_check,
   rell_security_check, run_rell_tests, scaffold, local_chain_up,
   translate_error, deployment_preflight, help gateway, compact mode), bearer
   auth, Docker/render deployment, embeddings pipeline, CI, and the 676-test
   suite. These have no upstream counterpart code, so nothing to fix upstream.
