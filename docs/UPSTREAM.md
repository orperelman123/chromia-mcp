# Fixes worth upstreaming to `gitlab.com/chromaway/core-tools/chromia-mcp`

Bugs found in this fork on 2026-08-30 that also exist in the official upstream
`chromia-mcp` (base of this tree). Each was verified against the live
`explorer.chromia.com` API. Diffs reference this repo's commits; the upstream
patches are small and mechanical.

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

## 5. Ktor HTTP logging on stdout corrupts MCP stdio

Upstream installs the Ktor `Logging` plugin with the default logger, which
writes to stdout - the same stream MCP JSON-RPC uses in stdio mode. Any HTTP
log line corrupts the protocol. Fix: route logs to stderr (or a file), never
stdout, in stdio mode.

## 6. `fetch_docs` returns whole files (up to ~863KB per call)

Upstream returns the entire `llms-full.txt` resource in one tool result, which
overflows an AI assistant's context. Fix: add `search` (matches with context)
and `offset`/`maxChars` pagination to the tool schema and handler.

## 7. `filter_assets` sorting declared but never bound

The GraphQL query declares `$sortBy`/`$sortDirection` but nothing binds them -
sorting silently does nothing. Fix: thread `sortBy`/`sortDirection` through
the filter model and strategy.
