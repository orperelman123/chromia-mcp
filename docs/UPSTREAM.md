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

## 3a. Explorer: `dashboardData` and `groupedTransactionsByBlockchain` are dead

Probed live 2026-09-07 against `explorer.chromia.com` (mainnet, production
query shapes and schema introspection):

```
{ dashboardData { countAllAccounts } }        INTERNAL_ERROR for <uuid>
{ groupedTransactionsByBlockchain { brid } }  INTERNAL_ERROR for <uuid>
{ dappAnalytics { calculatedAt } }            INTERNAL_ERROR for <uuid>
{ totalRewardsPaid }                          200, answers                  
```

The fields are still IN the schema (introspection lists all three), so a
client cannot tell from the schema that they are gone - only from calling
them. Every aggregate the explorer's own dashboard is built on is affected,
and there is no other field carrying the same data: the 28 query fields have
no top-level `countAllAccounts` / `countAllTransfers` / `monthlyActiveAccounts`,
no `groupedTransactionsByCluster` (see #3), and per-blockchain transaction
counts only via `blockchainAnalytics(brid)`, one chain per call.

Upstream `chromia-mcp` advertises `get_network_stats`,
`get_transactions_by_cluster` and `get_blockchains_transactions` on top of
these fields; all three are fully broken against the live service. This repo
retired its copies of them on 2026-09-07 rather than keep advertising data
the explorer will not serve.

## 3b. Explorer: `allBlockchains(state: ...)` answers INTERNAL_ERROR

Same probe, 2026-09-07: `allBlockchains` answers normally with `rid`, `name`,
`cluster`, `container`, `system`, `limit`, `offset` - and INTERNAL_ERROR the
moment a `state` argument is supplied, with or without the others:

```
{ allBlockchains(limit: 5) { rid } }                    200, 5 rows
{ allBlockchains(cluster: "pink", limit: 5) { rid } }    200, 5 rows
{ allBlockchains(state: "RUNNING", limit: 5) { rid } }   INTERNAL_ERROR for <uuid>
```

So `filter_blockchains{state}` is broken for every client of the explorer
while the rest of the tool works. This repo does not retire the tool (the
other filters are fine) - it pins the failure live and says so in the tool's
description, so an agent is not left thinking its own call was malformed.

**Ledger key: `allBlockchains(state:)`.** Amended 2026-09-08. The claim that
`state` actually FILTERS has no live coverage while this stands, and there was
no honest way to write it: a test asserting it would be red every run, and a
permanent red is the gate crying wolf. It is written now, and this entry is what
makes it legal - `ToolExecutorStrategiesTest.liveFilterBlockchainsFiltersByChainState`
reports an UPSTREAM WARNING against this entry (`LiveEnv.datedLedgerEntry("3b",
"allBlockchains(state:)")` requires both the key above and a date on this
section), which is a failure in the XML that the gate counts separately rather
than a pass. Deleting this entry when the explorer serves `state` again turns
that test back into an ordinary red - which is the point of writing the debt
down.

## 3c. Explorer: `getAssetTopHolders` is INTERMITTENT, and it hid inside our own test

Adversary round 18 drove all fifteen remaining explorer tools against the live
explorer with arguments parsed out of the explorer's own answers - 30 calls,
25 answers, 4 tool errors, 1 no-input - and **every one of the four errors was
`get_asset_top_holders`**: three distinct real asset ids taken from
`get_all_assets` plus the id its own description offers as an example, each
retried once, **eight consecutive live attempts and eight `INTERNAL_ERROR`s**
(2026-09-07). The other fourteen tools answered.

**Re-measured 2026-09-08: it answers.** `liveGetAssetTopHoldersAnswersForARealAsset`
made two live calls for CHR and passed on the data - holders only, `limit`
honoured exactly, the synthetic `Others` row lifted out into `othersRemainder`,
and `excludeAccounts` bound by the explorer - in 6.1 s. So this field is not dead
the way `dashboardData` is (#3a); it is **intermittent**, and the tool stays.

What has to be said with it is that our own suite could not tell the difference.
`assertLiveExplorerTool` returned `null` when a tool error matched an upstream
marker - `internal_error` first in the list - and the test body did
`?: return@runBlocking`, so the test named "answers for a real asset" passed
through all eight failures. An early return is not even counted the way a skip
is. Both helpers now FAIL on an upstream refusal, carrying the explorer's own
words and the retry advice, and `AssumptionLedgerTest` pins the shape. While this
field is down the suite is **red**, which is what README "Testing Layers" has
always said it would be.

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

## 11. Explorer analytics: `blockchainAnalytics` is unbounded in chain size, and the index is stale since 2026-09-04

Measured live 2026-09-08 against `explorer.chromia.com` (mainnet).

**The index is four days behind.** The newest transaction the explorer will
return is

```
{ allTransactions(limit: 1, sortBy: "timestamp", sortDirection: DESC)
    { transactions { timestamp rid } } }
  -> timestamp 1788504000333 = 2026-09-04T06:40:00.333Z   (measured 2026-09-08T18:41Z)
```

so every count, aggregate and "active accounts over time" the explorer serves
stops at 2026-09-04 06:40Z. Nothing in the response says so - the shape is
identical to a fresh one - and a client that trusts it reports four-day-old
numbers as current. The explorer back-end lane's root cause is an unbounded
per-transaction-per-account query plus a stalled synchronizer.

**`blockchainAnalytics` is the query that shows it.** Per chain, one call each,
node client, 60 s ceiling:

| chain | brid | wall |
|---|---|---|
| CONNECTED_FASHION (15 tx) | `0E091F5E…` | 0.6 s |
| Fanzeal (100 tx) | `BC04E517…` | 0.6 s |
| AllianceGames (5.3 M tx) | `8A61C857…` | 18.8 s |
| directory_chain (2.3 M tx) | `7E5BE539…` | 24.4 s, then 14.8 / 15.1 / 14.7 s |
| AllianceGamesArcade (3.0 M tx) | `431B410C…` | 28.7 s |
| Cod3CrewMarketplace (1.4 M tx) | `50CC05D0…` | 41.5 s |

The cost tracks the chain's transaction count, not the request: 15 transactions
answer in half a second, 1.4 M take 41 s. `ChromiaConfig.httpTimeouts.requestTimeout`
is **60 s**, so the busiest chains are inside the bound only by margin - and
earlier the same day the explorer back-end lane measured `blockchainAnalytics`
**dropping the connection at 60 s for every chain it tried**, including the two
small ones and the directory chain. Between that measurement and this one the
query recovered to "slow but answering"; nothing changed on our side.

**Correction to the premise this entry was opened on.** It was opened as "times
out for EVERY chain". Re-measured before writing, that is not what the explorer
is doing at 2026-09-08T18:41Z, and the entry says what was measured rather than
what was expected. What stands: the query is unbounded in the chain's size, it
has already been observed past the 60 s bound today, and the data it returns is
four days stale.

**What this means for the suite.**
`ToolExecutorStrategiesTest.liveGetBlockchainAnalyticsAnswersForARealChain`
passes while the query answers, and is an **upstream warning** - not a pass, not
our red - whenever it comes back with `Request timeout has expired` on the
explorer URL, for as long as this entry is open. The entry is the guardrail that
makes that legal (`LiveEnv.datedLedgerEntry("11", "blockchainAnalytics")` reads
this file and requires both the date and the query name). Closing it removes the
excuse: when ChromaWay bounds the query and catches the index up, delete this
entry, the `blockchainAnalytics` row in
`ToolExecutorStrategiesTest.upstreamLedgerEntries` and the staleness note in the
tool's description, and a timeout there goes back to being an ordinary red.

## The upstream-warning contract (what the gate validates)

Written 2026-09-09, after adversary round 19 produced two fake warnings.

This file is not only a list of upstream defects: the numbered entries above are
**the ledger the test gate reads**. A live test whose third party is proven down
is neither a pass nor a red — it is an UPSTREAM WARNING, a failure in the JUnit
XML that `scripts/gate-tally.mjs` counts and prints separately and that does not
set the exit code.

**What the gate validates.** `scripts/gate-tally.mjs` re-derives every clause
from the artifacts and refuses on any one of them:

1. the failure's message begins `UPSTREAM WARNING (proven): ` (one optional
   `some.Exception: ` prefix allowed, because Gradle and JUnit's own reporter
   spell it differently);
2. an evidence file exists at `app/build/upstream/warnings/<Class>.<method>.json`

One file per warning, written by the test that failed, in the directory the test JVM was started with (`app/build/upstream` unless `-Dchromia.upstream.dir` says otherwise - the property exists so a test that re-runs a live test through a nested launcher does not overwrite the evidence the outer run bound by digest).
   and names that test;
3. its `signature` is one of the four allowlisted names — things only the third
   party can say;
4. **the message binds the file** (below);
5. **an independent canary agrees, or an entry in this file excuses the query**
   (below);
6. the evidence is timestamped **inside this run**, and "this run" is derived
   rather than assumed (below).

**When this run started, and why that is not a flag.** Clause 6 was the one
clause that could not fire where it mattered. The gate computed the run's start
as

```js
const runStart = startedAt ?? (Math.min(...mtimes) - TEST_TASK_TIMEOUT_MS);   // 90 minutes
```

where `startedAt` was the `--started-at` argument — and **no workflow passed it**
(`grep -c started-at .github/workflows/*.yml` was `0` in all four, measured by
adversary round 20). Classified the way CI classified, an evidence file dated
**sixty minutes before the run** was accepted as a proven outage, and `t.stale` —
the check `report()` calls fatal, *"you are reading someone else's evidence"* —
was only ever computed `if (startedAt !== null)`, so it could not fire in CI at
all. A warning file left in `app/build` by an outage last week would have excused
an identical failure today.

The remedy is not to pass the argument in four more places; a gate armed by
remembering a flag is a gate disarmed by forgetting one. The run's start is now
**derived from the run**: `:app:test` writes `app/build/test-run/starts.tsv`
before its first test executes, one row per invocation
(`<epoch-ms>\t<ISO-8601>\t<task path>`), truncated when the results directory
holds no XML and appended otherwise — so the partitioned local gate records every
slice and the run's start is the earliest of them. `--started-at` survives only
as a NARROWING override (`max(marker, flag)`), and with neither a marker nor a
flag the tally **fails closed**: a results directory nobody can date is not
evidence of anything.

`Round20UpstreamBindingProbeTest` re-measures that on every run, against the
marker THAT RUN wrote, through the real `scripts/gate-tally.mjs` called the way
CI calls it — `--dir` and no flags — over a JUnit XML the real launcher and the
real `LegacyXmlReportGeneratingListener` produced, so the `[evidence sha256:…]`
that binds the file to the failure is one the test process actually threw.
Evidence an hour older than the marker is refused, evidence **one second** older
is refused, evidence written during the run is accepted and does not set the exit
code; a result file older than the marker is reported stale **with no flag
passed**; `--started-at` narrows the window and cannot widen it; and a checkout
with no marker is refused outright. The recordings are frozen at
`exploit-corpus/realworld/adversary-round20/upstream/freshness-after-fix.json`,
`…/run-window-after-fix.json` and `…/fails-closed-with-no-marker.json`.

**What binds evidence to a run.** Nothing in the repository says only `LiveEnv`
may write into `app/build/upstream/warnings` — round 19's a2 wrote a file by hand
and bought the status with it. What an attacker cannot hand-write is the JUnit
XML: its `message` attribute is produced by the *test process*, from the
throwable the test threw. So `LiveEnv.upstreamOutage` hashes the exact bytes it
writes and puts `[evidence sha256:<hex>]` in that message, and the gate
recomputes the digest from the file on disk and matches it. A file the failing
test did not write cannot match a message it did not produce.

**What the gate does with an entry above.** It opens this file. (Round 19's a1:
before that it took the producer's word for both the number and the heading, and
an entry of `999` with a heading no document has was proof.) The entry must
**exist** as `## <number>. <heading>`, its heading must match the heading in the
evidence **exactly**, its section must carry a **date**, and its section must
**name the query** being excused. That is the rule `LiveEnv.datedLedgerEntry`
applies on the producing side, re-derived from the same file by a second,
independent reader — two checks of one fact are worth one unless they are
independent.

**What an independent canary is.** One measurement per test JVM of the cheapest
query the explorer has (`{ totalRewardsPaid }` — the field that kept answering
through the 2026-09-04 and 2026-09-07 incidents, #3a), made on a path that shares
nothing with the tool that failed: a plain `java.net.http.HttpClient` built in
`LiveEnv`, its **own** 20 s request bound and 10 s connect bound (never
`ChromiaConfig.httpTimeouts`), its own request body, its own reading of the
answer — and only the **URL** taken from the production constant, because the
question is about that service. Only `FAILED_SIGNATURE` **carrying the same
allowlisted signature as the tool failure** is proof: `FAILED_OTHER` is the state
`LiveEnv` itself documents as "may well be ours", and a different signature is a
second fault rather than one outage.

Why it has to be independent: of the four allowlisted signatures, **two are
reachable from our own code**. `explorer-request-timeout` is ktor's text for
*our* `HttpTimeouts.requestTimeout` expiring, and `explorer-http-5xx` is any 5xx
our own handling lets through. A canary that used the production config, the
production client and the production bounds — which is what it did until
2026-09-09 — therefore fell over at the same moment we did, and a single fault of
ours satisfied the signature guard and the canary guard at once. That is measured
rather than argued:
`AssumptionLedgerTest.ourOwnRequestTimeoutThroughTheRealSeamStaysARed` tightens
`requestTimeout` to 1 ms through the real `ChromiaConfig` / `HttpTimeouts` seam,
makes a real live `filter_blockchains` call, gets a real `Request timeout has
expired` carrying a real allowlisted signature — and proves the verdict is
**RED**, with no evidence file written. The other two signatures are not
reachable from here at all: `INTERNAL_ERROR for <hex>` needs the explorer's own
request id, and `reCAPTCHA`'s rule id is not in `UPSTREAM_RULE_IDS`, so its prose
is never appended to a tool error.

**A warning is not a green.** The XML still records a `<failure>`, nothing about
the tool was verified, and the remedy is to fix or wait for the third party and
RE-RUN. The entry above is the debt; deleting it removes the excuse.

## Status (2026-09-06, amended 2026-09-07)

Findings #3a and #3b were added on 2026-09-07 and are NOT part of the ported
branch: they are outages inside the explorer service, not defects in upstream's
code, so there is nothing to patch - what upstream can do is stop advertising the
three tools built on the dead fields, which is what this fork did.

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
