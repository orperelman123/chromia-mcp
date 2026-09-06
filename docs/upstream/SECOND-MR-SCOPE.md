# Scope note: what a second MR (secure scaffolding + guard verification) would take

This is a scope assessment only — no code for it is in this branch. It covers
the four things the fork built on top of `chromia-mcp` that have no upstream
counterpart:

| Fork capability | What it is |
| --- | --- |
| `scaffold_dapp` | Twelve hardened Rell dapp templates, each shipping its own must-fail tests |
| `rell_security_check` | A static Rell security gate (rules + findings + `ok`) |
| `verify_guards` | A mutant checker: is a guard actually load-bearing, or is its test vacuous? |
| Exploit corpus | A standing scoreboard of real drains, replayed on every build |

Measurements below are taken from the fork tree
(`github.com/orperelman123/chromia-mcp`, 2026-09-06) and from this clone of
upstream `dev` at `1467777`.

---

## 1. The hard prerequisite: upstream has no Rell compiler in process

**Upstream today does not compile Rell anywhere.** `app/build.gradle.kts` on
`dev` resolves `net.postchain.client:postchain-client:3.36.0`,
`net.postchain.client:chromia-client`, ktor, langchain4j and gson. There is no
`net.postchain.rell` dependency, no Rell sources, and no code path that parses
Rell. Everything the four capabilities do rests on a compiler that is not there.

Adding it is not a one-line dependency bump. The fork needs both:

- `net.postchain.rell:rell-api-base:0.16.7` — compile check (`rell_check`)
- `net.postchain.rell:rell-api-gtx:0.16.7` — in-process test runner
  (`run_rell_tests`), which is what makes `verify_guards` possible at all

and both drag a **second, newer postchain** (3.49.x) onto a classpath that
already carries postchain-client 3.36. The fork carries an explicit resolution
strategy for the fallout, and it was found the hard way:

- `rell-api-gtx` → postchain 3.49 constrains **http4k to 6.53.x**, which breaks
  postchain-client 3.36's runtime ABI (`ClientFilters.AcceptGZip`) and also
  carries Kotlin 2.4 metadata. The fork pins the whole http4k group to
  `6.0.1.0`.
- postchain 3.49 also constrains **httpclient5 to 5.6.x**, whose automatic
  content decompression collides with http4k's own gunzip: every live query
  fails with `Not in GZIP format`. The fork pins httpclient5 to `5.4.2`.
- `rell-api-gtx`'s postchain dependencies pull **kotlin-stdlib 2.4.0**, whose
  metadata an older compiler rejects; the fork forces stdlib and reflect back to
  the project's Kotlin version. Upstream compiles with Kotlin 2.1.20
  (`gradle/libs.versions.toml`), so it would hit this too.
- **Both http4k ABIs cannot be satisfied at once.** With http4k pinned to
  6.0.1.0 for postchain-client, postchain 3.49's *own* REST API
  (`RestApi`/`ServerFilters.GZip`) crashes with `NoSuchMethodError`. The fork
  works around this by never starting postchain's RestApi. Anything upstream
  that wants both is stuck until the versions converge.
- A fifth Maven repository is needed (the ChromaWay rell registry,
  `gitlab.com/api/v4/projects/32802097/packages/maven`).
- The fat jar crosses 65,535 entries, so the shadow jar needs zip64.

**Whoever takes this on should expect the dependency work to be the risky part,
not the rules.** It is upstream's `:app:shadowJar` and its deployed image that
have to keep working; a broken pin is silent until a live query fails.

An alternative worth putting on the table in the MR discussion: ship the
templates and the scanner **without** the compiler (templates are text; most
security rules are syntactic), and leave `run_rell_tests` / `verify_guards` for
a later step. That splits the risk cleanly — see "Staging" below.

## 2. Which upstream files change

Every path below is relative to the upstream repo root.

| Upstream file | Change |
| --- | --- |
| `app/build.gradle.kts` | Two `net.postchain.rell` dependencies, a `resolutionStrategy` block pinning http4k + httpclient5, zip64 on `shadowJar`, and the JUnit/test wiring this MR already adds |
| `app/src/main/kotlin/org/chromia/tools/McpTools.kt` | Four new `Tool(...)` definitions with their input schemas (`scaffold_dapp`, `rell_check`, `rell_security_check`, `verify_guards`, plus `run_rell_tests` if the runner comes along) |
| `app/src/main/kotlin/org/chromia/tools/ToolExecutor.kt` | Five new entries in the `strategies` map and their `BaseToolStrategy` subclasses; `VerifyGuardsStrategy` alone is ~334 lines |
| `app/src/main/kotlin/org/chromia/App.kt` | The new tools added to `registerTools()` |
| `app/src/main/kotlin/org/chromia/tools/` (new files) | `DappScaffold.kt`, `RellSecurityCheck.kt`, `RellCheck.kt`, `RunRellTests.kt`, `RellLibs.kt` |
| `app/src/main/resources/rell-libs/` (new) | The vendored FT4 v1.1.0r distribution zip (172 KB, one file) so `import lib.ft4.*` compiles with no `chr install` |
| `app/src/test/` (new) | The tests below — upstream has no test sources at all today; this MR's branch is what creates that source set |
| `README.md`, `docs/Functional.md` | Documentation for four tools whose whole point is that agents follow their prose |

Nothing in `app/src/main/kotlin/org/chromia/data/` changes: none of this
touches the explorer or the postchain client. The four capabilities are
additive; they do not alter any existing tool's behaviour.

## 3. Size

Production code, measured in the fork:

| Unit | Lines |
| --- | --- |
| `DappScaffold.kt` (twelve templates + their headers) | 10,773 |
| `RellSecurityCheck.kt` (the static gate) | 3,644 |
| `VerifyGuardsStrategy` (inside `ToolExecutor.kt`) | 334 |
| `RunRellTests.kt` | 740 |
| `RellCheck.kt` | 545 |
| `RellLibs.kt` (vendored-FT4 extraction + hash gate) | 196 |
| **Total production Kotlin** | **≈ 16,200 lines across 6 files** |

Plus resources: `rell-libs/ft4-v1.1.0r.zip` (172 KB, 1 file).

Tests that must come with it, also measured in the fork:

| Test | Lines | What it holds |
| --- | --- | --- |
| `DappScaffoldSecureTemplatesTest` | 3,613 | Every template's guards, and the arithmetic each header claims, recomputed from the template's own constants |
| `VerifyGuardsProbeTest` | 948 | Thirteen probe families pinned to the true verdict — the tool's own false verdicts from rounds 11–13 |
| `AuditLibFt4ExemptionRegressionTest` | 371 | The hash-gated `lib/ft4/` exemption (finding 10 of the first MR) |
| `DappScaffoldFt4TemplateTest`, `DappScaffoldTest` | 362 | Scaffold output shape |
| `VerifyGuardsToolTest` | 203 | Tool contract |
| `ExploitCorpusScoreboardTest` | 189 | Every corpus sample keeps its pinned verdict |
| `RunRellTestsToolTest`, `RunRellTestsFilterTest` | 327 | Runner contract |
| `RellToolsAdversarialTest`, `RellToolsFuzzTest` | 318 | Seeded fuzz + adversarial input against the compiler tools |
| `RellSecurityCheckToolTest`, `RellCheckToolTest` | 277 | Gate contract |
| `Ft4ImportCheckRegressionTest` | 68 | FT4 import rules |
| **Total** | **≈ 6,700 lines across 14 files** |

Plus the corpus itself: `app/src/test/resources/exploit-corpus/` — **459 files,
5.2 MB**, of which 103 sample directories, each a real drain with a pinned
verdict, plus `CORPUS.md` (the scoreboard) and a `realworld/` set.

So the honest total is roughly **16,200 lines of production Kotlin, 6,700 lines
of tests, 460 resource files (5.4 MB), across ~20 new files and 4 edited ones**
— an order of magnitude larger than the eleven-finding MR in this branch, and a
different kind of review.

## 4. What upstream would be taking on, beyond the diff

These are the parts that make this a policy decision, not just a merge:

- **The templates are guidance, and guidance can manufacture bugs.** Three of
  the fork's worst findings were wrong *sentences* in template headers, not
  missing guards: a prescribed holding period that was itself the
  vulnerability, a false "can never rewrite the past" claim, a header that said
  "Six" above seven guards. The fork's answer is that every claim in a header
  is either recomputed in a test or deleted. Upstream would inherit that rule,
  or inherit the defects.
- **A verification tool is attack surface.** `verify_guards` returned six wrong
  verdicts in its first round and three more in its second — four of them
  `ok:true`. Each is pinned as a probe. Upstream would need to keep running
  those probes, and to accept that the tool gets attacked when it changes.
- **The corpus is a scoreboard, not a fixture set.** Its value is that it is
  replayed on every build and that a row goes red when a class regresses. It
  costs build time and it will occasionally go red on a change that looks
  unrelated. That is the point, and it needs to be agreed up front.
- **`ok:true` reads to an agent as an audit pass.** The gate's notes have to
  state their boundary. Undecidable properties (conservation, quorum) stay
  advisory and must never flip `ok` to false, or agents route around the gate.

## 5. Staging (recommendation)

Three MRs, in this order, each independently useful:

1. **Templates only** — `scaffold_dapp` + `DappScaffold.kt` + the vendored FT4
   zip + `DappScaffoldSecureTemplatesTest`. No new compiler dependency: the
   templates are text and the tests assert on text. ~11,000 production lines,
   ~4,200 test lines. This is where most of the security value is, because a
   template makes its exploit class unwritable rather than merely detected.
2. **Static gate** — `rell_security_check` + its rules and tests + the exploit
   corpus. Still no compiler in most paths; the rules that need one can be
   gated off. ~3,700 production lines, ~650 test lines, 460 resource files.
3. **Compiler-backed tools** — `rell_check`, `run_rell_tests`, `verify_guards`,
   and only here the `net.postchain.rell` dependencies and the http4k /
   httpclient5 pins. ~1,600 production lines, ~1,800 test lines, and all of the
   dependency risk in one reviewable place.

Splitting this way keeps the dependency-conflict risk (section 1) out of the
first two MRs entirely, and lets each be reviewed by the people who care about
it.
