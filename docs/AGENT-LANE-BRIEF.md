# Lane brief for agents working on chromia-mcp

Read this first. It is the shared context every lane needs; your task message adds only what is specific to your lane. If anything here contradicts your task message, say so in your report rather than guessing.

`GOAL.md` at the repo root states what all of this is for. The two commands below exist so the loop's guarantees do not depend on anyone remembering them:

- **`node scripts/loop-gate.mjs --expect-min <n>`** — the merge gate. Pass the PREVIOUS VERIFIED test count as `--expect-min`, never an estimate: it exists to catch a suite that silently narrowed, and a guessed floor turns that check into a coin flip (it once blocked a clean 1148-test merge because the floor was invented). Forces a rerun (a cached task reports success having run nothing), refuses evidence older than the run, and fails on **any** skip or zero results. The orchestrator runs this; you do not.
  - **There is no `--allow-skip`.** It was removed 2026-09-07: CI has always run `ALLOWED = set()`, so the flag let a local gate pass evidence the merge gate would refuse. Passing it now fails with that reason.
  - The gate also **refuses to start** unless `CHROMIA_TEST_DATABASE_URL`, `CHROMIA_LIVE_PROVISIONING_TESTS=true` and `CHROMIA_REQUIRE_CHR=true` are set — real env vars (CI) or `local-test-env.properties`. That file is gitignored, so an unprovisioned worktree used to skip every environment-gated test and still print a green tally. If your worktree has no such file, say so; do not point at another lane's.
  - **`--docs-only --base <last gated commit>`** is the mode for a prose-only commit. It takes no test list: it refuses unless every changed path is documentation, then DERIVES the classes by grepping `app/src/test/kotlin` for the changed file names, adds `ExploitCorpusScoreboardTest`, requires each derived class to have produced results, and records the derivation in the gate line. A hand-picked list is what this replaces — the last one named five classes where a grep finds six.
- **`bash scripts/new-lane.sh <branch> [base]`** — provisions a lane: its own worktree, its own C.UTF-8 database, its own test-env file.

## The goal, and the only test that counts

The MCP must **generate secure dapps**: an agent that builds a dapp using *only* this server's guidance, templates and gates must produce something an independent hostile auditor cannot drain. Not "the gate returned ok:true" — that is a linter result, and on 2026-09-02 it certified four agent-built dapps of which two were trivially drainable. The score is always: *build with only the MCP, then attack the result.*

Four principles, in priority order:

1. **Guidance must not manufacture bugs.** A template that teaches an insecure pattern outranks any missed detection — it creates the hole. Defaults are what agents copy, so the default must be the safe one. (Precedent: the scaffold shipped `add_auth_handler(flags = [])` as the golden pattern; FT4 resolves flags with `contains_all()`, and `contains_all([])` is always true, so every value-moving op copied from it silently dropped the Transfer flag.)
2. **Detection must not be evadable.** A rule keyed on anything the attacker controls is not a boundary. Key on **type and use, never on names**, and attack your own rule before shipping it. (Precedent: the confused-deputy rule matched a parameter-name allowlist — HIGH as `from`, silent as `victim`, same drain.)
3. **The gate must not cry wolf.** A gate agents route around is worse than no gate. A rule that cannot be made precise is **dropped with the reasoning stated**, not shipped noisy. Undecidable properties (conservation, quorum) stay MEDIUM advisories and never make `ok:false`.
4. **What cannot be caught statically needs a different answer** — a template that makes the bug unwritable, a test the scaffold ships, or a runtime check. Never a regex pretending.

## Where things are

- Rules: `app/src/main/kotlin/org/chromia/tools/RellSecurityCheck.kt` (one `Finding` per rule; follow the existing conventions).
- **The scoreboard:** `app/src/test/resources/exploit-corpus/CORPUS.md` + `samples/`. `ExploitCorpusScoreboardTest` runs the real analyzer over every sample and fails in *both* directions — a regression, or a gap closed without being credited. It is the acceptance test for rule work. Never weaken it to make something pass; if a sample stays uncaught, it stays `GAP` and you say why.
- Template and guidance: `app/src/main/kotlin/org/chromia/tools/DappScaffold.kt`, `ChromiaRellPracticesHelp.kt`, `app/src/main/resources/prompt_templates.json`.
- Test runner: `RunRellTests.kt`. It **can** execute FT4 operations, but FT4 tests need the test-only admin module args (`DappScaffold.ft4TestModuleArgs()`), or every case fails with an opaque "Unable to create GTX module".
- Ground truth for FT4 semantics: `docs/knowledge/raw-ft4-src/v1.1.0r/`.

## How to work here (each of these cost real time today)

- **One gradle build at a time**, never a second while yours runs. Four concurrent builds OOM-killed the Kotlin daemon on this 32 GB box; worktrees isolate files, not RAM. The slot is shared with the orchestrator's gate on main, so CHECK before every build: `Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'gradle-wrapper\.jar|GradleWorkerMain|Gradle Test Executor' }` (the Gradle and Kotlin compile daemons idle between builds and are not builds; a wrapper client or a test executor is). If something is running, do work that needs no build and check again - do not start on top of it, and do not wait idle. Iterate with targeted `--tests '*YourTest*'`; **do not run the full suite yourself** — the merge gate is a forced full run done serially by the orchestrator.
- **What this host must deliver, measured 2026-09-08.** `run_rell_tests` abandons a run at 90 s and that bound may only be tightened. Round 18 reported one FT4 registration plus the chain bootstrap at 86.4 s here and concluded no two-account FT4 test could finish through the tool; re-measured with `chr test` on an idle box, the numbers are **17.2 s of fixed cost for a dapp with no FT4 at all and 16.8 s for the shipped insurance template** (so the fixed cost is `chr`'s JVM start, the compile and the PostgreSQL schema creation, NOT the FT4 tree), **~2 s per test case**, and **~1 s per FT4 account registration** once the JVM is warm (the first costs ~7 s more, which is JIT). The round's own drain fixture, byte for byte: **17.7 s of runner time on an idle box, 23.6 s with two other Gradle builds running, against 128.0 s on 2026-09-07**. So concurrent builds cost about a third (worth having on a 59-minute suite) and the round's 7.2x is not explained by load alone - the rest is not attributed here. The point that survives is that an FT4 test needs about twenty seconds of work and has ninety, so a run that hits the bound means the host is degraded, not that the bound is wrong. Requirements: one JVM build at a time and >=2 GB free, a C.UTF-8 PostgreSQL with a database per worktree, `chr` on PATH, and ~20 s of budget per `chr test` invocation before any test body runs.
- **Never run `gradlew --stop`.** It stops every Gradle daemon for the user, not just your worktree's — it killed another lane's build mid-run.
- **DB-backed tests need a database of their own per worktree.** Two suites sharing one Postgres schema collide (`Missing metadata entities for existing tables: c0.<other lane's tables>`), and it looks intermittent. Each worktree carries its own `local-test-env.properties` pointing at its own database (`chromia_mcp_test_wtqa`, `chromia_mcp_test_wtecon`, …); the orchestrator provisions these — if your worktree has none, say so rather than pointing at another lane's.
- **Never sit idle waiting on a build.** If you have nothing to do but wait, write up what you have and finish. Three agents burned ~250k tokens each in wait loops. A result that reads "waiting on the build" is treated as a stall and the lane is taken over.
- **A fast `BUILD SUCCESSFUL` proves nothing.** Gradle caches the test task; an 8-second green ran nothing. Use `--rerun-tasks` and read the XMLs in `app/build/test-results/test/`. Zero results is not a pass either.
- **Bogus "Unresolved reference" errors across unrelated files after a rebase** are stale incremental state, not a real break: `clean test`.
- **Everything must be real.** No stubs, no tests that pass without exercising what they name, no claim the code cannot back. A red that is true beats a green that is not; a branch is left failing rather than softened. Two tests enforce this mechanically and both fail on a new violation: `NoTestDoublesTest` scans the test sources for doubles and requires there to be NONE - not of our own code and not of a third party either. The ledger that used to allow a double with a named live counterpart was removed on 2026-09-07, after pointing the explorer tools at the real explorer showed four of them had never worked and every one had a green unit test, and `AssumptionLedgerTest` does the same for every site that can skip.
- **Attack your own rule before shipping it.** Rename every identifier; select into a local then mutate the local; wrap the mutation in a helper; split across files; rewrite `-=` as `= x - y` and `+= -y`; put the auth in an `if`. Every variant that stays silent while the exploit still executes is a finding — pin it. Then write the *secure* version in several styles and confirm it stays clean; every legitimate shape that gets flagged is a false positive — pin that too.
- **After two wrong hypotheses, stop theorising and instrument** — print the intermediate state. The systemic `paramDelegated` blind spot (`update v (...)` misread as a helper call, silencing every rule for the select-into-a-local shape) was found that way after three wrong guesses.
- Small conventional commits in **your own worktree**, one concern each, stating the failure they fix. Do not push, do not merge — the orchestrator verifies and merges. Kill only processes you started.

## Reporting

Honest and short beats padded. Per finding: the exploit, the reproduction, the fix, the regression test, the commit. Separately: what you suspected but could not reproduce, and what you deliberately left alone with the reason. "These rules held against everything I threw at them" is a valuable result. Inventing findings to look productive makes the work worthless.

## Running several agents at once

The scarce resource on this machine is not reasoning, it is the **one build
slot**. A full suite is 18-23 minutes, there is one Gradle daemon and one
PostgreSQL, and four concurrent builds have already OOM-killed the Kotlin
daemon once. Three agents once burned roughly 250k tokens each sitting in wait
loops for a build that was starved behind others - about 750k tokens for zero
output. More lanes than build slots is not parallelism, it is a queue with
tokens attached to it.

So split the work by whether it needs a build:

- **Analysis parallelises.** Reading prose, recomputing a header's arithmetic
  from the template's own constants, auditing the redirect table, tracing an
  import graph, ranking corpus rows - none of it compiles anything. Run as many
  of these at once as the question deserves, and say **READ-ONLY: do not edit,
  do not run gradle** in the brief, in those words. This is also where the
  richest findings currently are: three rounds running, the top finding was a
  false sentence of ours, and a green suite cannot see one.
- **Verification does not parallelise.** Applying a fix, running the gate,
  merging, pushing - one lane at a time, and prefer taking the work over
  dispatching it when a lane stalls.

Give every agent the reasoning, not just the task. That means pointing it at
GOAL.md, this file, docs/ADVERSARY-ROUND-BRIEF.md and docs/TEMPLATE-GAPS.md,
and telling it **what is already known** so it does not spend a budget
rediscovering a pinned finding. An agent handed a task without the why produces
work that has to be re-derived; that is what makes the difference between a
swarm and a crowd.

Two things every brief needs, learned the hard way:

- **A build takes 10-20 minutes: run it in the BACKGROUND and wait.** Do not
  poll in a loop, and never report "waiting on the build" as a result. One lane
  did exactly that after 264k tokens, and its build had failed seven minutes
  earlier with five red tests on its branch.
- **A lane's own report is not evidence.** Whoever merges re-runs the gate in
  the lane's worktree with `--expect-min` set to the previously VERIFIED count,
  and recomputes any number an argument rests on. Both have caught a defect
  that had already passed a lane's green gate.
