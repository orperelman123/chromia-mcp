/assign_reviewer @misha-chromaway @tim.steinholtz @issame.zguiri

# fix: live-API drift, silently wrong list filters, swallowed errors, stdout log corruption

## What does this Solve?

Eleven findings from running this server against the live `explorer.chromia.com`
API and against real dapp chains. Nine carry a code change here, one commit each
with a test (eight are defects in this repository; finding 9 is a client-side
mitigation for an explorer behaviour). Two carry no code change and are here
only so they are on the record.

Two of the nine are **silent**: they return a plausible answer that is wrong.
Those are the ones worth reviewing first.

This branch also adds the first `app/src/test` sources. `:app:test` was
`NO-SOURCE` on `dev` — the JUnit 5 dependency and `useJUnitPlatform()` were
already in `app/build.gradle.kts`, there were simply no tests. `./gradlew
check` in CI now actually runs something.

Every "verified" line below was re-run against the live explorer on
**2026-09-06**; the exact requests are quoted so they can be re-run.

---

## 1. GraphQL list variables were sent as `toString()` — silently empty results

**Symptom.** Every list-typed filter on every analytics tool (`accountTypes`,
`brids`, `operations`, `signers`, `excludedAccounts`, …) returned nothing, with
no error anywhere. HTTP 200, empty result set, a plausible-looking answer.

**Root cause.** `GraphQLQuery.toJsonObject()` encoded lists with
`value.toString()`, so `listOf("FT4_USER")` went on the wire as the JSON
*string* `"[FT4_USER]"`. GraphQL's list-input coercion wraps a non-list into a
single-element list, so the explorer filtered on the literal `[FT4_USER]`,
matched nothing, and answered 200.

**Verification (live, 2026-09-06).** Same query, same asset, only the variable
encoding differs:

```
# what dev sends
'{"variables":{"assetId":"5F16…28B5","limit":3,"accountTypes":"[FT4_USER]"}}'
→ 200 {"data":{"getAssetTopHolders":[]}}

# what this branch sends
'{"variables":{"assetId":"5F16…28B5","limit":3,"accountTypes":["FT4_USER"]}}'
→ 200 {"data":{"getAssetTopHolders":[{"accountId":"3008BC…","totalBalance":"109236626253906"}, …]}}
```

**Fix.** Encode `List<*>` as a `JsonArray`, keeping each item's JSON type
(number stays a number, boolean stays a boolean).
Test: `GraphQLQueryTest` — including an explicit assertion that a list is never
stringified.

## 2. `getAssetTopHolders` — the exclusion argument is `excludedAccounts`

**Symptom.** `get_asset_top_holders` fails outright whenever the exclusion
filter is used.

**Root cause.** The explorer schema renamed the argument on
`getAssetTopHolders` to `excludedAccounts`. `getAssetDistribution` was **not**
renamed and still takes `excludeAccounts`, so the two queries now legitimately
differ.

**Verification (live, 2026-09-06).** Schema introspection:

```
getAssetTopHolders   [assetId, limit, brids, excludeBrids, accountTypes, excludeAccountTypes, accounts, excludedAccounts]
getAssetDistribution [assetId, brids, excludeBrids, accountTypes, excludeAccountTypes, accounts, excludeAccounts]
```

and sending `dev`'s query text verbatim:

```
Validation error (UnknownArgument@[getAssetTopHolders]) : Unknown field argument 'excludeAccounts'
```

with the renamed argument the same call returns three holders.

**Fix.** Rename the argument in the top-holders query text and binding only.
The Kotlin-side field stays `AssetFilters.excludeAccounts`, so no tool schema
changes.
Test: `AssetQueriesTest` — pins both queries, including that distribution keeps
the old name.

## 3. Top-level `groupedTransactionsByCluster` was removed from the schema

**Symptom.** `get_transactions_by_cluster` is 100% broken.

**Root cause.** The field is no longer a member of `Query`; the data lives
under `dashboardData`.

**Verification (live, 2026-09-06).**

```
query { groupedTransactionsByCluster { cluster, count } }
→ Validation error (FieldUndefined@[groupedTransactionsByCluster])

__type(name:"DashboardData").fields
→ [… groupedTransactionsByCluster …]
```

Introspection of `Query` confirms the top-level field is gone (29 fields, none
of them it).

**Fix.** Query it under `dashboardData`. **Note for reviewers: the returned
JSON gains one nesting level** (`data.dashboardData.groupedTransactionsByCluster`
instead of `data.groupedTransactionsByCluster`). `get_network_stats` already
read it from `dashboardData`, so the two tools are now consistent.
Test: `NetworkQueriesTest`.

## 4. `ToolExecutor.executeTool` swallowed every failure message

**Symptom.** Every tool failure reached the calling assistant as
`Tool execution failed`, with no reason. A missing required parameter lost its
own message.

**Root cause.** The error `CallToolResult` was built inside
`Result.onFailure { }` — which returns its *receiver*, so the built value was
discarded. `getOrNull() ?: CallToolResult("Tool execution failed")` then served
the generic text.

**Verification.** `get_asset_top_holders` with no `assetId` throws
`IllegalArgumentException("Missing required parameter: assetId")`; on `dev` the
caller sees `Tool execution failed`.

**Fix.** `getOrElse { e -> … }`, including `e.message`. The return type is now
declared explicitly (`: CallToolResult`) rather than inferred.
Test: `ToolExecutorTest`.

## 5. Console logging targets stdout, corrupting MCP stdio JSON-RPC

**Symptom.** In stdio mode (Claude Desktop, Cursor, and every other
one-process-per-session host) log lines are interleaved into the JSON-RPC
stream and corrupt the protocol.

**Root cause.** `App.registerTools()` logs every request and every response at
`info`, and `log4j2.properties` declares a Console appender with **no
`target`** — log4j2's default console target is `SYSTEM_OUT`, the same stream
`StdioServerTransport` writes frames to.

*(An earlier internal write-up of this blamed the ktor `Logging` client plugin.
That was wrong — `dev` installs no such plugin. The defect is the log4j2
console target.)*

**Fix.** `appender.console.target=SYSTEM_ERR`. The rolling file appender is
untouched, so nothing is lost.
Test: `LoggingTargetTest` reads the packaged `log4j2.properties` off the
classpath and asserts the target is explicit and is stderr.

## 6. `fetch_docs` returned an unbounded result

**Symptom.** One `fetch_docs` call returns the whole concatenation of every
matched documentation segment. The assistant on the other end pays for all of
it in context, and has no way to ask for less or for the next part.

**Honest scoping.** An earlier note of ours claimed calls of up to ~863 KB.
**That figure did not survive re-verification and we withdraw it** — the
current embeddings artifact caps `RagStore.query` at 15 segments above score
0.6, which is tens of KB, not hundreds. What is still true and is what this
commit fixes: the tool has **no output bound at all** and **no way to page**.

**Fix.** `DocsResultWindow` — optional `offset` and `maxChars` (default 20,000
characters) on the `fetch_docs` schema and handler. When the result is
truncated, the marker names the exact offset to pass in for the next part, so
nothing becomes unreachable.
Test: `DocsResultWindowTest`, including a test that pages the whole result back
together and asserts nothing was lost.

We deliberately did **not** add a keyword `search` parameter: this tool is a
semantic search already, and a second search mode inside it is a design choice
for the maintainers, not a bug fix.

## 7. `filter_assets` sorting was declared but never bound

**Symptom.** `sortBy` / `sortDirection` are advertised in the `filter_assets`
tool schema and declared in the GraphQL query text, and are silently ignored.

**Root cause.** Three separate gaps: `AssetSearchFilters` had no sorting field,
`FilterAssetsStrategy` never extracted the arguments, and `filterAssets()`
never bound the declared variables.

**Fix.** All three, matching how `getAllTransactions` already does it.
Tests: `AssetQueriesTest` (the variables reach the query, and are omitted when
not asked for) and `ToolExecutorTest` (the arguments reach the repository).

## 8. `chromia_dapp_query` failed on every `big_integer` response

**Symptom.** Any dapp query answering with a Rell `big_integer` — every FT4
balance, `total_supply` and amount field — reported an error although the chain
had answered successfully.

**Root cause.** `PostchainClientService` serialized the answer with postchain's
`make_gtv_gson()`, whose `BIGINTEGER` branch throws
`big_integer cannot be serialized as JSON`. `makeStrictGtvGson()` encodes big
integers as JSON strings and is otherwise the same serializer.

**Verification.** Pinned in the test itself: the test asserts that
`make_gtv_gson()` still throws on a `big_integer` and that
`makeStrictGtvGson()` produces `"1180591620717411303424"`, so the root cause
cannot be re-introduced by "simplifying" the builder back.

**Fix.** Use `makeStrictGtvGson()`. To make the mapping testable without a
chain, the chain call is behind a one-method `ChainQueryRunner` fun interface
with the live implementation as the default argument — the production call path
is unchanged.
Test: `PostchainClientServiceTest`, which also asserts that **every other** Gtv
type serializes exactly as before (integer stays a JSON number, byte_array
stays hex, null stays null).

**This is also worth an issue on `core/postchain`** (not filed by us): the
obvious-default builder name throws on a first-class Rell type that appears in
essentially every FT4 response, and this repository is the proof that a client
picks it and ships broken. The behaviour is asserted directly against the
resolved `postchain-gtv` in `PostchainClientServiceTest`. Deprecating the name,
or making the default strict, would stop the next client doing the same.

## 9. Explorer returns HTTP 400 for `network=testnet` — and we reported it opaquely

**Symptom.** Every analytics tool advertises `'testnet'` as a valid `network`.
Against the live explorer those calls fail, and the failure reached the caller
as `Bad Request` and nothing else.

**Verification (live, 2026-09-06).** Identical body, only the query parameter
differs:

```
?network=mainnet  → 200 {"data":{"totalRewardsPaid":"42776353836497"}}
?network=testnet  → 400 {"message":{}}
```

**Server side, not ours.** We cannot fix the 400. What we can fix is that
`HttpClientService` reported only `HttpStatusCode.description` and dropped the
response body, so no caller could tell *what* was rejected.

**Fix (client side).** Include the response body (truncated to 500 characters)
in the error. To test it, the `HttpClient` is now a constructor parameter with
the existing CIO client as its default — the production configuration is
unchanged, moved verbatim into `defaultHttpClient()`.
Test: `HttpClientServiceTest` with a mock engine (adds
`testImplementation("io.ktor:ktor-client-mock")`), covering the 400-with-body
case, an empty body, an oversized body, a success, and a GraphQL error inside a
200.

**Ask for the explorer team:** either serve `testnet` again, or return a real
error message, or tell us to stop advertising `testnet` on these tools and we
will remove it from the schemas.

## 10. `getNodeUnavailability` now requires a reCAPTCHA token — no code fix possible

**Symptom.** `get_node_unavailability` is broken against the live explorer for
every programmatic client, this server included.

**Verification (live, 2026-09-06).** HTTP 200, with:

```
"reCAPTCHA verification failed: token is required. Add X-reCAPTCHA-Token header with a valid token."
```

**No commit in this MR.** The response is a well-formed GraphQL error and
`GraphQLResponseParser` already surfaces it verbatim, so there is nothing to
fix in this repository. It is here so it is on the record and so the tool is
not assumed to work.

**Ask:** either offer a non-browser API path for this query, or we should gate
or remove the tool. Pinned as a test case in `HttpClientServiceTest` (a
reCAPTCHA error inside a 200 must reach the caller as an error, not as data).

## 11. FT4 v1.1.0r contains the patterns a Rell security scanner must flag — no code fix here

**What it is.** The official FT4 v1.1.0r distribution legitimately declares
`operation ras_open` (the open registration strategy) and its test helper
imports `lib.ft4.admin`. Any Rell security scanner that bans those and walks a
full source tree will report CRITICALs *inside the library* whenever a project
vendors FT4 — false alarms pointing at code the user cannot change.

**Why it is in this MR at all.** It is not a defect in FT4 and it is not a
defect in this repository, which does no scanning. It is recorded here because
it is a real trap for anyone who adds scanning later, and because the safe
exemption is not obvious: a path-string exemption alone is unsafe (malicious
code parked under `lib/ft4/` would dodge the scan). The exemption has to be
hash-gated — exempt a `lib/ft4/` file only if it is byte-identical (line
endings ignored) to the vendored release copy, and scan anything that differs.

**No commit in this MR.** See `SECOND-MR-SCOPE.md`.

---

## What is NOT in this MR

- **No new tools and no new capabilities.** Every commit either fixes a broken
  behaviour or bounds an unbounded one. Tool names, argument names and result
  shapes are unchanged except where the fix requires it (finding 3's extra
  nesting level, finding 6's two optional arguments).
- **No Rell compilation, scaffolding, security scanning or guard
  verification.** Those are the fork's own work, they need a compiler upstream
  does not have, and they are an order of magnitude larger. Scoped separately
  in `SECOND-MR-SCOPE.md`.
- **No `search` parameter on `fetch_docs`** (finding 6) — a design choice, not a
  bug fix.
- **No change to the advertised `network` values** (finding 9) — that is the
  explorer team's call, and we would rather ask than guess.
- **No dependency upgrades.** The only build change is one test-scoped
  dependency (`ktor-client-mock`, already at the project's `ktorVersion`).
- **Two further defects we confirmed on `dev` but deliberately left out**, because
  both change `App.kt`'s public shape and would need their own review. Say the
  word and we will send them as a follow-up:
  1. **The stdio server never exits when its client disconnects.**
     `runStdioMcpServer()` waits on `done.join()`, completed only from
     `Server.onClose` - which the SDK fires only on an explicit `close()`. On
     stdin EOF only the *transport* closes, so the JVM blocks forever and every
     disconnect leaks a process. Repro: `echo '' | java -jar chromia-mcp.jar`
     never returns. Fix: also hook `transport.onClose { done.complete() }`.
  2. **CORS is installed with no allowed host.** `installCors()` adds the ktor
     CORS plugin with methods but no `anyHost()`/`allowHost(...)`, so every
     cross-origin request - preflight included - is rejected and no browser MCP
     client can reach the SSE server.
- **The retracted 863 KB `fetch_docs` claim** (see finding 6) and a suspected
  SSE session leak in the MCP Kotlin SDK's `mcp{}` plugin, which we did not
  re-verify in the SDK sources and whose venue is not ChromaWay. Neither is
  asserted here.

## Notes for the reviewer

- The commits are ordered and independent; each is one finding plus its test.
- Three commits add a constructor parameter with a default so a unit test can
  substitute a fake (`ToolExecutor`'s RAG store, `PostchainClientService`'s
  chain call, `HttpClientService`'s HTTP client). In all three the production
  call path and configuration are byte-for-byte what they were — the default
  argument is the old code.
- `app/src/test` is new. If you would rather land the fixes without the tests,
  say so and we will split them — but findings 1 and 7 returned wrong data with
  no error at all, and finding 8 turned a successful chain answer into an error.
  That is exactly the class of defect a test suite exists for.

## Release Notes

### Features
- `fetch_docs` accepts `offset` and `maxChars` and reports how to read the rest of a truncated result

### Bugfix
- GraphQL list filters (`accountTypes`, `brids`, `operations`, `signers`, …) were sent as strings and silently returned no results
- `get_asset_top_holders` failed with `UnknownArgument`; the explorer renamed the argument to `excludedAccounts`
- `get_transactions_by_cluster` was fully broken; the field moved under `dashboardData`
- Tool failures reported `Tool execution failed` with no reason
- Console logging wrote to stdout and corrupted MCP JSON-RPC in stdio mode
- `filter_assets` accepted `sortBy`/`sortDirection` and ignored them
- `chromia_dapp_query` failed on every `big_integer` response (all FT4 balances, supplies and amounts)
- Explorer HTTP errors reported only the status text and dropped the response body
