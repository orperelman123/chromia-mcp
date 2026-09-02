# Lane brief for agents working on chromia-mcp

Read this first. It is the shared context every lane needs; your task message adds only what is specific to your lane. If anything here contradicts your task message, say so in your report rather than guessing.

`GOAL.md` at the repo root states what all of this is for. The two commands below exist so the loop's guarantees do not depend on anyone remembering them:

- **`node scripts/loop-gate.mjs --expect-min <n> --allow-skip <Class::test>`** — the merge gate. Forces a rerun (a cached task reports success having run nothing), refuses evidence older than the run, and fails on any unlisted skip or zero results. The orchestrator runs this; you do not.
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
- Ground truth for FT4 semantics: `docs/knowledge/raw-ft4-src/v1.1.0r/`; wider Chromia sources at `C:\Users\Orpe7\Downloads\chromia123`.

## How to work here (each of these cost real time today)

- **One gradle build at a time**, never a second while yours runs. Four concurrent builds OOM-killed the Kotlin daemon on this 32 GB box; worktrees isolate files, not RAM. Iterate with targeted `--tests '*YourTest*'`; **do not run the full suite yourself** — the merge gate is a forced full run done serially by the orchestrator.
- **Never run `gradlew --stop`.** It stops every Gradle daemon for the user, not just your worktree's — it killed another lane's build mid-run.
- **DB-backed tests need a database of their own per worktree.** Two suites sharing one Postgres schema collide (`Missing metadata entities for existing tables: c0.<other lane's tables>`), and it looks intermittent. Each worktree carries its own `local-test-env.properties` pointing at its own database (`chromia_mcp_test_wtqa`, `chromia_mcp_test_wtecon`, …); the orchestrator provisions these — if your worktree has none, say so rather than pointing at another lane's.
- **Never sit idle waiting on a build.** If you have nothing to do but wait, write up what you have and finish. Three agents burned ~250k tokens each in wait loops. A result that reads "waiting on the build" is treated as a stall and the lane is taken over.
- **A fast `BUILD SUCCESSFUL` proves nothing.** Gradle caches the test task; an 8-second green ran nothing. Use `--rerun-tasks` and read the XMLs in `app/build/test-results/test/`. Zero results is not a pass either.
- **Bogus "Unresolved reference" errors across unrelated files after a rebase** are stale incremental state, not a real break: `clean test`.
- **Everything must be real.** No stubs, no tests that pass without exercising what they name, no claim the code cannot back. A red that is true beats a green that is not; a branch is left failing rather than softened.
- **Attack your own rule before shipping it.** Rename every identifier; select into a local then mutate the local; wrap the mutation in a helper; split across files; rewrite `-=` as `= x - y` and `+= -y`; put the auth in an `if`. Every variant that stays silent while the exploit still executes is a finding — pin it. Then write the *secure* version in several styles and confirm it stays clean; every legitimate shape that gets flagged is a false positive — pin that too.
- **After two wrong hypotheses, stop theorising and instrument** — print the intermediate state. The systemic `paramDelegated` blind spot (`update v (...)` misread as a helper call, silencing every rule for the select-into-a-local shape) was found that way after three wrong guesses.
- Small conventional commits in **your own worktree**, one concern each, stating the failure they fix. Do not push, do not merge — the orchestrator verifies and merges. Kill only processes you started.

## Reporting

Honest and short beats padded. Per finding: the exploit, the reproduction, the fix, the regression test, the commit. Separately: what you suspected but could not reproduce, and what you deliberately left alone with the reason. "These rules held against everything I threw at them" is a valuable result. Inventing findings to look productive makes the work worthless.
