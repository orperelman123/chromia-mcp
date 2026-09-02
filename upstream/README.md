# upstream/ - submission-ready upstream contributions

Everything needed to report (and where possible, fix) the bugs this fork
found in Chromaway's own code and services. Nothing here has been submitted
anywhere; every submission step below is yours to take, from your own
company account.

All claims were fact-checked on 2026-09-02 against the upstream `dev`
sources on GitLab, the live explorer API, and the published artifacts.
Upstream `chromia-mcp` `dev` HEAD is `14677776` (2026-01-13) - exactly the
commit this fork was salvaged from, so every code finding is still present
upstream.

## Contents

| File | What it is |
| --- | --- |
| `FINDINGS.md` | The full report: 10 CONFIRMED + 2 OBSERVED findings, grouped by venue, each with location, repro, fix reference and severity. Also lists what we deliberately excluded and why. |
| `mr-chromia-mcp.md` | Ready-to-paste MR title + body for `gitlab.com/chromaway/core-tools/chromia-mcp`. |
| `mr-postchain.md` | Ready-to-paste issue title + body for `gitlab.com/chromaway/core/postchain` (design suggestion, no patch). |
| `mr-explorer.md` | Ready-to-paste report for the explorer.chromia.com service. No public repo exists for it - the file explains the venue options. |
| `patches/chromia-mcp/0001..0009-*.patch` | A git-am patch series against upstream `dev`. Each patch is one bug, minimal and mechanical, authored as Or Perelman <or.perelman@chromaway.com>. |

Verification already done for you (2026-09-02):

- The series applies cleanly with `git am` on upstream `dev` HEAD.
- `./gradlew compileKotlin compileTestKotlin` passes with all nine applied.
- Upstream has no test suite (`:app:test` is NO-SOURCE), so compilation is
  the maximal in-repo check; the same fixes are regression-tested in this
  fork's 676-test suite.

## How to submit (per venue)

### 1. chromia-mcp (the main one - an MR with the patch series)

    # from your company account:
    # fork https://gitlab.com/chromaway/core-tools/chromia-mcp on GitLab, then
    git clone git@gitlab.com:<your-fork>/chromia-mcp.git
    cd chromia-mcp
    git checkout -b fix/live-api-and-stdio dev
    git am /path/to/this/repo/upstream/patches/chromia-mcp/*.patch
    ./gradlew compileKotlin compileTestKotlin   # sanity check
    git push -u origin fix/live-api-and-stdio

Then open an MR against `chromaway/core-tools/chromia-mcp`, target branch
`dev`, and paste the title + body from `mr-chromia-mcp.md`. The patches are
authored with your name/company email; adjust trailers if your policy wants
Co-authored-by lines.

### 2. postchain (issue only)

Open an issue on `gitlab.com/chromaway/core/postchain` with the title +
body from `mr-postchain.md`. If external issues are closed on that project,
the fallback is a mention in the chromia-mcp MR (the body already links the
two).

### 3. Explorer service (venue uncertain)

There is no public repo. `mr-explorer.md` contains the report; deliver it
either as part of the chromia-mcp MR discussion (recommended - the
maintainers can route it internally) or via Chromia's developer support /
community channels. Since you are at ChromaWay, an internal channel is
probably fastest - the report is self-contained either way.

### 4. FT4 - nothing to submit

Finding D1 in `FINDINGS.md` (FT4 v1.1.0r legitimately contains patterns that
Rell security scanners must exempt, and how to exempt them safely) is
informational guidance for tooling authors, not an FT4 defect. No issue
prepared; it is recorded in FINDINGS.md so the knowledge is not lost.

## What was deliberately NOT included

See "Excluded findings" at the end of `FINDINGS.md`:

- the old "fetch_docs returns ~863KB files" claim - failed fact-check
  against upstream's current embeddings artifact, dropped;
- the SSE session leak believed to exist in the MCP Kotlin SDK's `mcp{}`
  plugin - non-Chromaway venue, not re-verified in SDK sources;
- all fork-only features and their hardening (no upstream counterpart code).
