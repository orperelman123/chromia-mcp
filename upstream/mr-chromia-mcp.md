# MR for gitlab.com/chromaway/core-tools/chromia-mcp (target branch: dev)

Attach: the nine patches in `patches/chromia-mcp/` (apply in order with
`git am`). Series verified 2026-09-02: applies cleanly on `dev` HEAD
(`14677776`), `compileKotlin` and `compileTestKotlin` pass. The upstream
repo currently has no test suite (`:app:test NO-SOURCE`); these fixes carry
regression tests in our fork's suite.

---

## Title

fix: nine production bugs - live explorer API drift, silent list filters, stdio corruption/leaks, big_integer responses

## Body (ready to paste)

Hi! While building on top of chromia-mcp we found and fixed a number of bugs
that affect the upstream server as-is. This MR contains only the minimal,
mechanical fixes - one commit per bug, independent where possible. All were
re-verified against `dev` HEAD and the live explorer API on 2026-09-02.

**Live-API drift (tools currently broken against explorer.chromia.com):**

1. **GraphQL list variables are serialized with `toString()`**
   (`GraphQLQuery.kt`): `listOf("FT4_USER")` is sent as the string
   `"[FT4_USER]"`. GraphQL list-input coercion turns that into a
   single-element list containing the literal string, so the server returns
   HTTP 200 with an empty result set - every list filter (`accountTypes`,
   `brids`, `operations`, `signers`, ...) silently returns nothing. Fixed by
   encoding lists as JSON arrays.
2. **`getAssetTopHolders` argument was renamed to `excludedAccounts`** in the
   explorer schema (introspectable live; `getAssetDistribution` still uses
   `excludeAccounts`). Calls using the argument fail with `UnknownArgument`.
3. **Top-level `groupedTransactionsByCluster` was removed** from the schema
   (`FieldUndefined`); the data lives under `dashboardData` now.
   `get_transactions_by_cluster` is fully broken without this. Note the tool's
   JSON response gains one nesting level (`dashboardData.{...}`).

**Protocol / runtime:**

4. **Tool errors are swallowed** (`ToolExecutor.executeTool`): the error
   result with the exception message is built inside `Result.onFailure { }`,
   which discards it (`onFailure` returns the receiver) - callers always get
   a generic "Tool execution failed". Fixed with `getOrElse`.
5. **Console logging corrupts MCP stdio**: the log4j2 Console appender has no
   `target`, so it defaults to SYSTEM_OUT - the same stream stdio JSON-RPC
   uses - and every request/response is logged at info. Fixed with
   `appender.console.target=SYSTEM_ERR`.
6. **`filter_assets` sorting is declared but never bound**: the query text
   declares `$sortBy`/`$sortDirection`, but nothing extracts or binds them, so
   sorting is silently a no-op. Threaded through `AssetSearchFilters`.
7. **`chromia_dapp_query` fails on every `big_integer` response**:
   `make_gtv_gson()`'s BIGINTEGER branch throws (`big_integer cannot be
   serialized as JSON`), so any FT4 balance/supply/amount query reports an
   error although the chain answered. Switched to `makeStrictGtvGson()`
   (big_integer as JSON string; serializer otherwise identical).
8. **stdio server never exits when the client disconnects**: `done.join()` is
   completed only from `Server.onClose`, which fires only on explicit
   `close()`; on stdin EOF only the transport closes, leaving an orphaned JVM
   per disconnect. Also hook `transport.onClose`.
9. **CORS installed with no allowed hosts**: the Ktor CORS plugin then
   rejects every cross-origin request, so browser-based MCP clients cannot
   connect to the SSE server at all. Minimal fix: `anyHost()` (tighten to
   specific origins as policy dictates).

**Server-side observations you may want to route internally** (we cannot fix
these client-side; both re-verified live 2026-09-02):

- `getNodeUnavailability` now requires a reCAPTCHA token
  ("reCAPTCHA verification failed: token is required. Add X-reCAPTCHA-Token
  header"), which breaks the `get_node_unavailability` tool for every
  programmatic client.
- `POST /api/explorer-service?network=testnet` returns HTTP 400
  (`{"message":{}}`) for queries that return 200 on mainnet, while the tool
  schemas advertise `'testnet'` as valid.
- Schema naming drifted apart: `getAssetTopHolders` takes `excludedAccounts`
  while `getAssetDistribution` takes `excludeAccounts`.

These fixes come from a downstream fork
(github.com/orperelman123/chromia-mcp) where they are covered by regression
tests; happy to upstream tests as a follow-up if wanted.

---

## Submission notes (for the submitter, not part of the MR body)

- One commit per bug is preserved in the patch files; keep them separate so
  each can be reviewed/reverted independently.
- Patches 0002/0006 both touch `AssetQueries.kt` and 0004/0006 both touch
  `ToolExecutor.kt`; apply the series in numeric order.
- If upstream prefers fewer commits, squashing is safe - the series has no
  internal conflicts.
