# CI

The operator's manual for `.github/workflows/`. What runs, when, what a red in
each step means, what to do about it, and how to reproduce it on your own
machine.

Every claim this document makes about a step name, a trigger or a gate step is
pinned by `CiWorkflowDocumentationTest` in the Kotlin suite: it reads
`.github/workflows/*.yml` and this file and fails if they disagree. So a step
renamed in the yml without a matching edit here reds the build, and this
document cannot quietly go stale.

---

## The one rule

**The local gate and the CI gate must be the same gate.**
`scripts/gate-tally.mjs` is the single classifier. `scripts/loop-gate.mjs`
imports it; every workflow that runs the suite executes it as the
`Classify the tally (ours, theirs, and skips)` step. Two gates with different
definitions of green is one gate and one bypass — which is exactly what the
removed `--allow-skip` flag was.

Nothing in CI has an allowlist of test names. There is no list of tests that may
fail, and no list of tests that may skip.

**And nothing the gate checks is armed by an argument somebody remembers.** Both
of `gate-tally.mjs`'s optional arguments are DERIVED when they are not given —
see [The two derived arguments](#the-two-derived-arguments). Adversary round 20
measured `grep -c started-at .github/workflows/*.yml` and `grep -c expect-min` at
**zero in all four**, which left three of the gate's five conditions unable to
fire in the gate that decides merges.

---

## The four workflows

| Workflow | Trigger | What it decides |
| --- | --- | --- |
| `ci.yml` — **CI** | push to `main`, pull request into `main`, manual | Whether a tree is mergeable. |
| `release.yml` — **Release** | push of a `v*` tag, manual with a tag input | Whether a jar may be published for end users. |
| `nightly-fuzz.yml` — **Nightly deep fuzz** | 03:00 UTC daily, manual | Whether random-seeded inputs can crash the compiler tools. |
| `embeddings-refresh.yml` — **Embeddings refresh** | Mondays 04:00 UTC, manual | Whether a freshly built RAG index is good enough to replace the published one. |

`ci.yml` is scoped to `branches: [main]` on both `push` and `pull_request`: a
push to a lane branch does not spend a runner, and a PR between two side
branches does not either. The orchestrator merges a lane into `main` after
`scripts/loop-gate.mjs` passes in the lane's worktree, and the push to `main`
is what CI gates.

### Concurrency

`ci.yml` groups by `github.ref` with `cancel-in-progress: true`. Two pushes a
minute apart used to occupy a runner twice for a tree that existed for sixty
seconds. Cancelling the run for commit A when commit B lands on the same ref is
safe because B's tree contains A, so the newer run gates strictly more. What is
lost is A's own per-commit verdict — if you need it while bisecting, re-run the
workflow from the Actions tab (`workflow_dispatch` is enabled).

`release.yml`, `nightly-fuzz.yml` and `embeddings-refresh.yml` all use
`cancel-in-progress: false`. A half-finished publish is worse than a queued one,
and a superseded nightly is not superseded — it is the same tree with different
seeds.

---

## The three statuses, and the exact contract

The gate classifies every non-passing test into exactly one of three buckets.
`docs/UPSTREAM.md`, "The upstream-warning contract", is the long form; this is
the summary an operator needs.

**PASS.** The test ran and its claim held.

**RED — ours.** Everything else. Every failure with no proof, every failure
whose proof does not match this run, and **every skip**. A skip is a test that
silently did not run, so a green suite carrying skips covered less than it
claimed; there is no allowlist, and there has not been one since 2026-09-07.

**UPSTREAM — theirs.** A failure — it is a `<failure>` in the JUnit XML like any
other, nothing pretends to pass — that satisfies *all* of:

1. its message begins `UPSTREAM WARNING (proven): ` (optionally behind one
   `some.Exception: ` prefix, because Gradle and JUnit's own reporter spell the
   message attribute differently);
2. an evidence file exists at
   `app/build/upstream/warnings/<Class>.<method>.json` naming one of four
   allowlisted signatures — `explorer-graphql-internal-error`,
   `explorer-request-timeout`, `explorer-recaptcha`, `explorer-http-5xx` — and
   carrying the third party's own words;
3. the failure message **binds** that file: `[evidence sha256:<hex>]`, written
   by `LiveEnv.upstreamOutage` over the exact bytes it wrote, recomputed from
   disk by the gate. The message comes out of the test process's own throwable;
   the evidence file is an ordinary file anyone can write. The digest is what
   says the two belong to each other;
4. **either** an independent canary failed with the *same* signature (a plain
   `java.net.http` client on its own path — `FAILED_OTHER` proves nothing, and a
   canary carrying a *different* signature is two faults, not one outage),
   **or** a `docs/UPSTREAM.md` entry that the gate **opens and reads**: the
   entry must exist, its heading must match the evidence exactly, its section
   must carry a date, and it must name the query being excused;
5. the evidence timestamp is inside this run — and the run's start is **derived**
   from the marker `:app:test` writes before its first test, never defaulted and
   never a flag. See below.

An upstream warning is counted, printed by name, and does **not** set the exit
code. It is **a red for the third party, not a pass for us**: nothing those
tests name was verified, the ledger entry is the debt, and the remedy is to fix
or wait for the third party and re-run.

A failure whose message *claims* the status without evidence that survives all
five checks is an **ordinary red**. The gate fails closed, by design.

---

## The two derived arguments

`scripts/gate-tally.mjs` accepts `--started-at` and `--expect-min`, and until
2026-09-09 **no workflow passed either**. That is not a small omission: three of
the five conditions the gate fails on depend on them.

| condition `report()` fails on | armed by | before round 20 |
| --- | --- | --- |
| result files predate this run | the run's start | never computed — `tally` filled `stale` only `if (startedAt !== null)` |
| `tests === 0` | nothing | worked |
| the suite ran fewer than *n* tests | `--expect-min` | never passed, so a suite narrowed to ONE test was green |
| failures that are ours | nothing | worked |
| any skip | nothing | worked |

Passing the two flags in four more workflows would have left the fifth workflow
to forget them — the removed `--allow-skip` arrived at by omission. Both are
derived instead, and both fail towards a red.

### When this run started — `app/build/test-run/starts.tsv`

`:app:test`'s own `doFirst` writes one row per invocation, before the first test
executes: `<epoch-ms>\t<ISO-8601>\t<task path>`. It **truncates** the file when
`app/build/test-results/test` holds no XML — a new accumulation is beginning —
and **appends** otherwise, so the partitioned local gate (thirteen serial
`--tests` slices into one results directory) records every slice and the run's
start is the **earliest** of them. No slice's XMLs are stale against a later
slice's start.

The tally reads that file. `--started-at` survives only as a **narrowing**
override — the run start is `max(marker, flag)`, so nothing a caller says can
widen the window — and the old fallback, "ninety minutes before the oldest result
file" (the test task's own timeout), is **deleted**. With neither a marker nor a
flag the tally **fails closed**: no marker means no `:app:test` execution wrote
one in this tree, which is either "the suite did not run" or "the task was up to
date and executed nothing".

What this arms, measured in
`app/src/test/resources/exploit-corpus/realworld/adversary-round20`: an upstream
warning dated **one second** before the marker is refused as an earlier run's
evidence (it used to be accepted up to an hour early), one written during the run
is accepted, and the fatal stale-results check now runs on every classification.

### How big the suite should be — `ci/expected-min.json`

```json
{ "expectMin": 1650, "verifiedBy": "…", "verifiedAt": "…", "why": "…" }
```

`gate-tally.mjs` reads it whenever it classifies the repository's own
`app/build/test-results/test`, and fails the run when fewer tests executed. It is
the only check between `tests === 0` and a suite silently narrowed by a `--tests`
filter that matched almost nothing, or by a class that failed to compile and took
its tests with it — neither of which produces a single failure.

The floor is committed rather than read from the last green run's published
`test-report` artifact, and the reason is the one that decides everything else in
this document: the artifact **expires** (7 days), needs the API and a token, and
does not exist for a fork's first pull request, so a fortnight's quiet would
disarm the check silently. A committed file is in every checkout, never expires,
and moves only in a reviewable diff.

It is not a number anybody has to remember either: `scripts/loop-gate.mjs`
**ratchets it up** after a green FULL run — never down, because a docs-only run
covers a derived subset and a partition covers one slice, and either lowering it
would excuse exactly the narrowing the floor exists to catch. Lowering it is a
deliberate edit that has to be argued for in the diff.

A caller pointing `--results` at some other directory (a nested run's temp
directory, a probe harness) does **not** get the floor automatically — that is
not this suite — and must pass `--expect-min` to get a size check at all.

### One verdict object, one headline

`verdict(t, { expectMin })` in `gate-tally.mjs` returns every condition above
with its state, a `green` field and a `headline`. `report()` prints it and the
process exits on it, `--json` exits on it, and `scripts/ci-summary.mjs` renders
the `headline` **verbatim** and tabulates the five conditions.

Before that, the job summary computed its own headline from two of the five:

```js
const ours = t.red.length;
const verdict = ours || t.skippedNames.length ? 'RED - this build does not ship' : … 'GREEN';
```

so a run the gate failed on stale results, on `tests === 0` or on the floor was
headed `## CI gate: GREEN` — and in the stale case the summary printed GREEN at
the top and *"You are reading an earlier run's evidence. This is fatal."* twenty
lines below it. Two renderings of what a pass is is the same defect as two
classifiers. The presenter can no longer disagree with the exit code, because it
no longer has an opinion of its own.

---

## Reading a run without opening a log

Every workflow that runs the suite writes a **job summary** — the panel on the
run's own page, above the job list. It is produced by `scripts/ci-summary.mjs`
in the step named `Write the job summary`, and it contains:

- a verdict line, and a table of **tests / ours / upstream / skipped / errors**;
- every failure that is **ours**, by name, with the gate's own reason (including
  `claims UPSTREAM WARNING but …` when a test tried to claim the third status
  and could not prove it);
- every **skipped** test by name;
- every **proven upstream outage** with its tool, query, signature, the
  `docs/UPSTREAM.md` entry that excuses it, and — in a collapsible block — what
  the third party actually said and how the canary measured;
- any result file that predates the run, and any evidence file matching no
  failure.

`scripts/ci-summary.mjs` is a **presenter, not a gate**. It imports `tally` from
`scripts/gate-tally.mjs` rather than re-implementing it, so every number in the
summary is the gate's own number, and it **always exits 0** by construction: a
presenter that could red or green the build would be a second gate with
different rules. The verdict is always the `Classify the tally` step.

It runs with `if: always()`, before the classification step, so the table exists
even when the suite step timed out.

### The artifacts

| Artifact | Contents | Retention | Uploaded |
| --- | --- | --- | --- |
| `test-report` (CI) | `app/build/reports/tests/test` — Gradle's HTML report | 7 days | every run |
| `upstream-evidence` (CI) | `app/build/upstream` — one JSON per proven outage | 30 days | every run |
| `nightly-test-report` | both of the above | 14 days | every run |
| `release-test-report` | both of the above | 90 days | every run |

All of them upload on `if: always()`. The test report used to be
`if: failure()`, which is backwards for two of the three things it is read for —
comparing a slow run against the last **green** one, and confirming what a green
run covered — and it left nothing at all behind for a run that was cancelled by
`concurrency` or killed by a timeout, which is neither success nor failure.

The upstream evidence is retained longest because an outage argument is made
across days ("#3b has been failing all week"), and because it is the only
durable proof that an `upstream=1` verdict was earned rather than asserted.

**The fat jar is deliberately not an artifact.** It was, at the 90-day default,
and it put 54 copies and 14.4 GB into the account's artifact quota in four days;
CI then went red on the *upload* step with a green 1175-test suite behind it,
twice (runs 33729077322 and 33730306288). A red that everyone learns to read as
"probably just the quota" is worse than no signal at all.

---

## CI, step by step

The steps below are the whole of `ci.yml`'s `build-and-test` job, in order.
Every step in the workflow also carries a `RED HERE:` comment beside it saying
the same thing at the point of use.

| Step | What a red means |
| --- | --- |
| `Set up JDK 21` | Temurin 21 could not be fetched. Infrastructure. |
| `Set up Gradle` | The action or the Gradle distribution could not be fetched. Infrastructure. |
| `Install Chromia CLI` | `apt.chromia.com` is unreachable or dropped the amd64 `chr` package. Upstream, and fatal on purpose: with no `chr`, the two CLI probes would skip. |
| `Run the unit suite` | Only infrastructure, or the 45-minute step budget. This step **swallows gradle's exit code on purpose** — see below. |
| `Write the job summary` | Cannot go red; it exits 0 by construction. |
| `Classify the tally (ours, theirs, and skips)` | **The verdict.** The suite failed, or did not run at all. Read the job summary. |
| `Build the fat jar` | `shadowJar` failed on a suite that already passed — in practice a duplicate or missing entry in the shadow merge, not a test problem. |
| `Fail on any skipped test` | A test skipped. Provision what it needs; there is no allowlist. |
| `Fail on a suite narrowed below the floor` | Far fewer tests ran than `ci/expected-min.json` says this suite has. A `--tests` filter matched almost nothing, or a class failed to compile and took its tests with it. Read the count, not a test name. |
| `Upload test report` / `Upload upstream evidence` | The artifact upload itself failed (quota, or a transient GitHub fault). Not a verdict on the code. |
| `Start server (full toolset) and run the end-to-end sweep` | Our contract broke against a live server. Read the `FAIL <check>` lines, **not** the `WARN-UPSTREAM` ones. |
| `PowerShell launchers parse` | `serve-local.ps1` or `serve-public.ps1` no longer parses. A syntax fix, never a flake. |
| `Stdio transport smoke (the Claude Code surface + npm launcher)` | The stdio transport — the surface Claude Code speaks — stopped answering, or the npm launcher stopped finding the jar it was handed. No network involved, so this is ours, always. |
| `npm launcher release download (the end-user install path)` | A new user's first `npx chromia-mcp` is broken. Check `release.yml`'s last run first: a release with no jar asset reds this without anything in your commit being wrong. |
| `Fresh-install boot (no local index, no cache, no token -> published asset)` | A fresh install would answer from a stale or missing index. Check `embeddings-refresh.yml`'s last run first. |

### Why the suite step swallows gradle's exit code

A proven upstream outage is a `<failure>` in the XML like any other, so
`:app:test` exits non-zero for something that is ChromaWay's to fix rather than
ours. If gradle's exit code were the verdict, the third status could not exist.
The step therefore runs `./gradlew :app:test … || rc=$?`, prints the code, and
lets the `Classify the tally` step decide. The fat jar is built in a *separate*
step **after** the classification, so a red suite still cannot ship one.

### Failure policy: strict vs degraded

- The **unit suite** is strict. No retries. Any failure that is ours is fatal.
- The **e2e sweep** exercises live third parties (explorer, mainnet/testnet
  nodes, docs site). The sweep classifies clean upstream errors as
  `WARN-UPSTREAM` — degraded, non-fatal — with guardrails so it cannot hide real
  breakage (all-live-warn is a FAIL; more than `SWEEP_MAX_UPSTREAM_WARNS`
  warnings is a FAIL — an environment variable, default 8; non-network checks
  never warn). See `scripts/upstream-classifier.mjs`.
- A `FAIL` from the sweep therefore means our contract broke — **except** a
  single transient blip, which one bounded retry absorbs. Each attempt gets a
  fresh server on a freed port. Two consecutive failures are fatal.
- Sweep **exit 3** means the only failures were the upstream guardrails. That is
  deterministic for the duration of an outage, so the retry is skipped and the
  run reds immediately.

**Measured limit, recorded rather than fixed:** the retry re-runs the whole e2e
— both transports and the synthetic agent — when any one of them failed. Run
34053646001 spent 17m32s failing `agent: preflight blocks the placeholder
container` twice for a deterministic defect, and the second 8m41s bought
nothing. A per-check retry needs the sweep to report which check failed in a
machine-readable way, which it does not do today. The cost is about nine
minutes, and only on a genuinely broken push.

---

## Expected durations, and where the budgets come from

Measured over the last twelve CI runs (34032465035 … 34274736432, 2026-09-06 to
2026-09-08) through `gh api repos/…/actions/runs/<id>/jobs`:

| | measured | budget | why that budget |
| --- | --- | ---: | --- |
| whole CI job | 16m08s – 43m52s | `timeout-minutes: 80` | above the sum of the step budgets, so a **step** timeout always fires first and names the culprit |
| `Run the unit suite` | 18m27s and 32m51s since rounds 17-19 landed (5m43s – 8m02s on the ten runs before) | 45 | worst measured plus the 1.8x spread seen between two consecutive runs |
| the e2e sweep step | 8m51s median, 17m32s worst | 30 | two attempts of the worst single pass (~9m) plus the 30s pause is ~19m |
| setup + jar + tail | ~1m00s + 39s + ~35s | — | |
| nightly fuzz: suite | 7m33s, 8m28s, then 43m41s after rounds 17-19 | 50 | |
| nightly fuzz: the fuzzer | 19s and 20s for 600 iterations | 30 | |

**The old `timeout-minutes: 50` was provably too tight.** Worst measured unit
suite + worst measured e2e + the fixed cost is 52m37s, above the budget; the
best run to date came within 6m08s of it. A job killed by its budget produces no
verdict and no summary at all.

This also answers the FLAG left in `app/build.gradle.kts`, which asks whether
`ubuntu-latest` is materially faster than the 15 W laptop or whether CI was
being killed: the same suite is **58m55s** on the laptop and **32m51s** on the
runner, so the runner is faster and CI was not being killed — but 50 minutes had
stopped covering it, which is the half of the flag that was true.

**Do not raise a timeout to make a red go away.** A raised timeout hides a hang
perfectly. Decide first whether the run was hanging or genuinely longer: the
task's own `timeout.set(Duration.ofMinutes(90))` in `app/build.gradle.kts`
carries the same warning, and that mistake has been made here.

**The nightly fuzzer's own budget is twenty seconds.** 600 iterations took 19s
and 20s on the two green runs. The workflow header calls this "far beyond the
per-push suite's time budget", which is not true of the default; the lever is
the `iterations` workflow input, and raising the default is a product decision
that has not been taken.

---

## The local gate equals the CI gate

`node scripts/loop-gate.mjs --expect-min <previous verified count>` is the merge
gate as a command. It and CI agree by construction, not by convention:

| | local (`loop-gate.mjs`) | CI |
| --- | --- | --- |
| classifier | imports `tally` / `report` from `scripts/gate-tally.mjs` | runs `node scripts/gate-tally.mjs --dir "$PWD"` |
| skips allowed | none (`--allow-skip` was removed 2026-09-07 and now fails with that reason) | none (`ALLOWED = set()`) |
| the run's start | derived from `app/build/test-run/starts.tsv`, which it clears before the run; its own wall clock can only narrow it | the same marker, from the same file the same task wrote |
| stale results | refuses any XML older than the run's start | the same check, now that the start exists; `--dir` also binds the ledger to this checkout |
| size floor | `ci/expected-min.json`, and **raises** it after a green full run | the same file, plus an independent second reader in `Fail on a suite narrowed below the floor` |
| cached task | forces `--rerun-tasks`; a fast `BUILD SUCCESSFUL` proves nothing | a fresh checkout every run |
| live environment | **refuses to start** unless `CHROMIA_TEST_DATABASE_URL`, `CHROMIA_LIVE_PROVISIONING_TESTS=true` and `CHROMIA_REQUIRE_CHR=true` are set, from real env vars or `local-test-env.properties` | the same three are set in the workflow |
| upstream ledger | reads `docs/UPSTREAM.md` in the repo it was pointed at | the same file in the checkout |

The one asymmetry is **how the suite is run**, not what counts as green. CI runs
`:app:test` once on a runner with a dedicated Postgres. On the 15 W laptop the
suite is run **partitioned** — serial `--tests` slices, one Gradle build at a
time, thirteen of them at the last full gate (GOAL.md, round 19) — because
there is one build slot, one PostgreSQL and 2 GB of headroom, and four
concurrent builds have OOM-killed the Kotlin daemon. The
partitioning changes the schedule, never the verdict: each slice writes its own
XMLs into `app/build/test-results/test/` and the tally reads the union. See
`docs/AGENT-LANE-BRIEF.md` for the build-slot discipline.

### `--docs-only`

For a prose-only commit: `node scripts/loop-gate.mjs --docs-only --base <last
gated commit>`. It takes no test list. It refuses unless every changed path is
documentation, then **derives** the classes to run by grepping
`app/src/test/kotlin` for the changed file names, always adds
`ExploitCorpusScoreboardTest`, requires each derived class to have produced
results, and records the derivation in the gate line. A hand-picked list is what
this replaces: the last one named five classes where a grep finds six.

Anything under `app/`, `scripts/`, `packages/`, `gradle/`, `.github/`,
`claude-code-chromia/` or `upstream/` is **code**, including a `.md` inside them
— the exploit corpus is test data a test reads and scores. **A change to
`.github/workflows/` is therefore never `--docs-only`.**

---

## Reproducing a CI failure locally

Provision first: a C.UTF-8 PostgreSQL with a database of your own, `chr` on
PATH, and `local-test-env.properties` in the repo root (gitignored) supplying
`CHROMIA_TEST_DATABASE_URL`, `CHROMIA_LIVE_PROVISIONING_TESTS=true` and
`CHROMIA_REQUIRE_CHR=true`. `bash scripts/new-lane.sh <branch>` does all of it
for a new lane. Check the build slot before starting anything:

```powershell
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -match 'gradle-wrapper\.jar|GradleWorkerMain|Gradle Test Executor' }
```

| CI step that failed | Reproduce with |
| --- | --- |
| `Run the unit suite` (one class) | `./gradlew :app:test --rerun --tests 'org.chromia.TheFailingTest'` |
| `Classify the tally` | `node scripts/loop-gate.mjs --expect-min <previous verified count>` — the same classifier, forced rerun, zero skips |
| the tally alone, over results you already have | `node scripts/gate-tally.mjs --dir "$PWD"` (add `--json` for the raw structure) |
| the job summary, as CI renders it | `node scripts/ci-summary.mjs --dir "$PWD"` |
| `Fail on any skipped test` | the same `loop-gate.mjs` run — it fails on any skip |
| `Fail on a suite narrowed below the floor` | the same `loop-gate.mjs` run — it reads `ci/expected-min.json` too |
| `Start server … end-to-end sweep` | `./gradlew :app:shadowJar` then `java -jar app/build/libs/chromia-mcp-server.jar --sse --host 127.0.0.1 --port 3001`, then in another shell: `node scripts/e2e-sweep.mjs http://127.0.0.1:3001 --transport sse`, again with `--transport http`, then `node scripts/synthetic-agent.mjs http://127.0.0.1:3001` |
| `PowerShell launchers parse` | `[System.Management.Automation.Language.Parser]::ParseFile((Resolve-Path 'serve-local.ps1'), [ref]$null, [ref]$errors)` |
| `Stdio transport smoke …` | `node scripts/stdio-smoke.mjs app/build/libs/chromia-mcp-server.jar` then `node scripts/stdio-smoke.mjs --launcher` |
| `npm launcher release download …` | `node scripts/stdio-smoke.mjs --launcher-download` (downloads the latest release's jar; needs network) |
| `Fresh-install boot …` | `node scripts/rag-eval.mjs --production-shaped --jar app/build/libs/chromia-mcp-server.jar` |
| the nightly fuzzer | `node scripts/fuzz-marathon.mjs http://127.0.0.1:3001 600 <the seed base from the issue>` — **not** the defaults, which are a different corpus |

**Line endings.** `.gitattributes` pins them per repository (`* text=auto`,
`*.rell`/`*.json` `text eol=lf`, `*.bat -text`, `*.cmd text eol=crlf`), so a
Windows checkout reads the same bytes CI does. `core.autocrlf` is a per-machine
setting and is not a substitute. If a test passes on the runner and fails on
Windows over whitespace, check `git ls-files --eol` for the file before
suspecting the test.

---

## Secrets and variables

**There are none.** No workflow references `secrets.*` or `vars.*`. The only
credential in play is the automatic `GITHUB_TOKEN`, passed as
`${{ github.token }}` where it is needed, and this repository's default for it
is **read-only** — every run prints `Contents: read, Metadata: read,
Packages: read` in its "Set up job" group. Each workflow therefore declares what
it needs:

| Workflow | `permissions` | Why |
| --- | --- | --- |
| `ci.yml` | `contents: read` | Publishes nothing. |
| `release.yml` | `contents: write` (job level) | `gh release create` publishes the jar. |
| `nightly-fuzz.yml` | `contents: read`, `issues: write` | `gh issue create` files what the fuzzer found. |
| `embeddings-refresh.yml` | `contents: write` | `gh release upload` replaces the rolling `embeddings` asset. |

Without the right `permissions` block a step does not fail loudly — it fails
with `Resource not accessible by integration`. That is exactly how the nightly
fuzzer's reporting went unnoticed: `gh issue create … || true` swallowed a
`createIssue` refusal on run 34325133558, and on every run before it.

Nothing else has to be configured. The e2e sweep and the live tests reach public
endpoints only: the live provisioning tests generate a throwaway keypair and
assert the account is **unregistered** (dry runs — nothing signed, nothing
spent), and the one positive lookup reads `get_balance` for a public account id.
No key, no funds, no configuration.

---

## Known reds that are not your commit

- **`embeddings-refresh.yml`, `Gate - not drastically smaller than the published
  asset`** — red since run 34103273206 (2026-09-07), which is why the published
  index has not moved since 2026-09-04. Working as designed: it refuses to
  replace a good index with a smaller one. The job summary now prints both sizes
  and the percentage. Somebody has to find out why the ingest shrank.
- **`ToolExecutorStrategiesTest.liveFilterBlockchainsFiltersByChainState`** —
  the explorer answers `allBlockchains(state:)` with `INTERNAL_ERROR` on every
  call. Counted as `upstream=1` under `docs/UPSTREAM.md` #3b. It does **not**
  red CI; it did red the nightly until 2026-09-09, because that job was not
  running the classifier.
- **`AuditRound4RegressionTest.interruptedDbRunDefersPermitReleaseUntilTheRealRunnerFinishes`**
  — observed failing once, on nightly run 34325133558 (2026-09-09), at
  `AuditRound4RegressionTest.kt:271` (`DB permit was released while the real
  runner was still executing`), and passing on CI run 34274736432 the evening
  before. It is timing-sensitive by construction: it starts a real Rell run,
  waits for the permit to be taken, interrupts the caller, and then asserts the
  permit is *still* held — which requires the real run not to have finished in
  the meantime. One observation is not a measured rate; it is recorded here so
  the second one is recognised rather than re-diagnosed.

---

## Nothing here can be proven by reading it

GitHub Actions cannot be run on a developer machine, and this repository does
not pretend otherwise. What *can* be proven locally is checked locally: the
workflows parse as YAML (a real parser — `on:` is a YAML 1.1 boolean, so a
grep-based check would not even find the triggers), every `run:` block that
goes to bash passes `bash -n` with the `${{ }}` expressions substituted, the
`shell: pwsh` block and both launchers pass the PowerShell parser, the scripts
pass `node --check`, `gh workflow list` resolves all four, `git add
--renormalize .` is a no-op against `.gitattributes`, and
`CiWorkflowDocumentationTest` asserts that this document and `ci.yml` agree, and
`Round20CiVerdictProbeTest` drives the real `gate-tally.mjs` and the real
`ci-summary.mjs` over five result directories and one start marker to prove the
verdict and the headline cannot disagree, and `Round20UpstreamBindingProbeTest`
drives the real `gate-tally.mjs` against the marker THE RUNNING SUITE ITSELF
wrote, to prove that evidence one second older than the run is refused, that
evidence written during it is accepted, and that a results directory with no
marker is refused outright.

A shell syntax error inside a YAML block scalar is invisible to a YAML parse
and to every test in the suite, which is why it is checked separately and why
it is worth checking at all. The run itself is proven by the next push to
`main`.
