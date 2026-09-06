# Fixes worth upstreaming to `gitlab.com/chromaway/core-tools/chromia-mcp`

Bugs found in this fork on 2026-08-30 / 2026-09-01 that also exist in the
official upstream `chromia-mcp` (base of this tree), plus ecosystem findings
(postchain, FT4, explorer) any downstream tool will hit. Each was verified
against the live `explorer.chromia.com` API or the published artifacts.
Diffs reference this repo's commits; the upstream patches are small and
mechanical.

## 1. GraphQL list variables serialized as `toString()` (silent wrong data)

Upstream `GraphQLQuery.toJsonObject()` encodes `List<*>` variables with
`value.toString()`, sending `"[FT4_USER]"` (a string) instead of `["FT4_USER"]`.
The explorer returns HTTP 200 with an **empty result set** instead of an error,
so every list filter (`accountTypes`, `brids`, `operations`, `signers`, ...)
silently returns nothing. Verified A/B against the live API.
Fix: encode lists as `JsonArray` (this repo: `GraphQLQuery.kt`, commit `dfd4c5d`).

## 2. `getAssetTopHolders` argument renamed to `excludedAccounts`

The explorer schema now names the exclusion argument `excludedAccounts` on
`getAssetTopHolders` (while `getAssetDistribution` still uses
`excludeAccounts`). Upstream's query text passes `excludeAccounts:` and every
call fails with `Validation error (UnknownArgument)`. Verified by schema
introspection. Fix: rename the argument in the top-holders query only
(commit `dfd4c5d`).

## 3. Top-level `groupedTransactionsByCluster` removed from the schema

`query { groupedTransactionsByCluster { ... } }` now returns
`FieldUndefined`; the data lives only under `dashboardData`. Upstream's
`get_transactions_by_cluster` tool is fully broken. Fix: query
`dashboardData { groupedTransactionsByCluster { ... } }` (commit `dfd4c5d`).

## 4. Tool errors swallowed in `ToolExecutor.executeTool`

Upstream builds an error `CallToolResult` inside `Result.onFailure { }` and
discards it (onFailure returns the receiver), so callers get a generic
"Tool execution failed" with no reason - e.g. a missing required parameter
loses its message. Fix: use `getOrElse` and include `e.message`.

## 5. Console logging on stdout corrupts MCP stdio

**Root cause corrected 2026-09-06 while porting this to upstream:** upstream
installs no Ktor `Logging` plugin at all. The defect is `log4j2.properties`
declaring a Console appender with no `target`, and log4j2's default target is
`SYSTEM_OUT` - the same stream MCP JSON-RPC uses in stdio mode, so any log
line corrupts the protocol. Fix: `appender.console.target = SYSTEM_ERR`
(upstream branch `fix/fork-findings`, commit `b834ab3`, `LoggingTargetTest`).

## 6. `fetch_docs` returns an unbounded result

Upstream returns a whole documentation resource in one tool result with no
output bound and no paging. **The "~863 KB" figure this entry used to carry
did not survive re-verification on 2026-09-06 and is withdrawn** - the defect
is the missing bound, not a measured size. Fix: bound the result and let the
caller page it with `offset`/`maxChars` (upstream commit `3f5bf63`,
`DocsResultWindowTest`); the fork's `search` parameter is a design choice and
is not part of the upstream MR.

## 7a. Explorer: `getNodeUnavailability` now requires reCAPTCHA

Discovered 2026-08-31 by the e2e coverage sweep: programmatic calls to
`getNodeUnavailability` on explorer.chromia.com fail with
`GraphQL Error: reCAPTCHA verification failed: token is required`. Any
API/MCP client of this query (including the official chromia-mcp) is
silently broken until the explorer offers a non-browser API path.

## 7. `filter_assets` sorting declared but never bound

The GraphQL query declares `$sortBy`/`$sortDirection` but nothing binds them -
sorting silently does nothing. Fix: thread `sortBy`/`sortDirection` through
the filter model and strategy.

## 8. postchain's `make_gtv_gson()` cannot serialize `big_integer` (default footgun)

Discovered 2026-09-01: `net.postchain.gtv.make_gtv_gson()` - the obvious
builder every client reaches for - registers a BIGINTEGER branch that
**throws** (`big_integer cannot be serialized as JSON`). Any dapp query
returning a `big_integer` (every FT4 balance / `total_supply` / amount field)
appears to fail even though the chain answered successfully. The non-throwing
builder is `GtvObjectMapper`-adjacent `makeStrictGtvGson()`, which encodes
big integers as JSON strings; verified in postchain-gtv 3.49.18 that all
other branches are bit-identical between the two builders. Upstream
`chromia-mcp` (and any postchain-client consumer using the default builder)
is affected. Fix: use `makeStrictGtvGson()` for response serialization
(this repo: commit `a54408d`). Also worth an upstream postchain issue: the
default builder failing on a core Rell type is a trap.

## 9. Explorer API returns HTTP 400 for `network=testnet`

Observed live 2026-09-01: `POST
https://explorer.chromia.com/api/explorer-service?network=testnet` returns
HTTP 400 for queries that succeed with `network=mainnet` (same body, 200).
Upstream `chromia-mcp` advertises `'testnet'` as a valid `network` argument
on every analytics tool, so all of those calls fail against the live
explorer. Until the explorer serves testnet again (or documents its removal),
downstream tools should expect and explain the 400 rather than surfacing an
opaque error.

## 10. FT4 v1.1.0r flags its own security rules (downstream scanners must exempt it)

The official FT4 v1.1.0r zip itself contains the patterns a Rell security
scanner must treat as findings in *app* code: the library **declares
`operation ras_open`** (the open registration strategy) and its test helper
**imports `lib.ft4.admin`**. Any scanner that bans `ras_open` / admin imports
and walks a full source tree will therefore report CRITICALs *inside the
library* whenever a project vendors FT4 - false alarms pointing at code the
user cannot change. The exemption must not be path-string trust alone (code
parked under `lib/ft4/` would dodge the scan): this repo exempts a
`lib/ft4/` file only if it is byte-identical (modulo line endings) to the
vendored v1.1.0r copy, and scans any file that differs, with a note (commits
`b66f033`, `15dbaf8`). Relevant to upstream if it ever grows scanning, and
to any other Rell security tooling.

## Status (2026-09-06)

The nine code findings are ported to a clone of upstream `dev`
(`146777767968721ecb6c97b1905721516d3281d0`) on branch `fix/fork-findings`,
one commit each with a test (upstream had no test sources; the branch adds
36, all green). The merge-request text is `docs/upstream/MR.md`; what
upstreaming the fork's compiler loop, templates, `verify_guards` and corpus
would take is `docs/upstream/SECOND-MR-SCOPE.md`. Two further upstream
defects were confirmed and left for a follow-up because they change
`App.kt`'s public shape: the stdio server never exits on client disconnect
(orphan JVM per session) and CORS is installed with no allowed host. Opening
the MR is a ChromaWay-account action.
