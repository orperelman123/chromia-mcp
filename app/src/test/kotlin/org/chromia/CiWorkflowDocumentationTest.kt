package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `docs/CI.md` is the operator's manual for `.github/workflows/`, and prose
 * about a gate is worth exactly as much as its agreement with the gate.
 *
 * Documentation of CI drifts in one direction only: somebody renames a step, or
 * adds one, and the manual keeps describing the workflow of three weeks ago.
 * Nothing red-lines for that, so it never gets fixed - and the next operator
 * reading a failure follows a map of a machine that no longer exists. This test
 * makes the drift fatal in BOTH directions:
 *
 *   - every named step in ci.yml must appear in the manual's step table, so a
 *     new step cannot ship undocumented;
 *   - every step the manual names must exist in ci.yml, so the manual cannot
 *     describe a step that was deleted or renamed.
 *
 * It also pins the two steps that ARE the gate (`Classify the tally ...` and
 * `Fail on any skipped test`), the presenter's inability to decide anything,
 * and the timeout budgets the manual quotes as measured numbers - a budget
 * cited in prose and a different budget in the yml is the same defect wearing a
 * number.
 *
 * Same shape as DockerfileEmbeddingsBakeTest, which pins the Dockerfile's
 * embeddings URLs against the runtime's: two artefacts that cannot share code
 * get a test instead of a comment asking people to remember.
 */
class CiWorkflowDocumentationTest {

    /**
     * Line endings are NORMALISED before anything is matched. `.gitattributes`
     * pins `*.rell` and `*.json` to LF in the working tree, but a `.yml` and a
     * `.md` are checked out in the platform's default - so on Windows every one
     * of these files arrives with CRLF, and an assertion written with `\n` would
     * pass on the runner and fail on a developer's box for a reason that has
     * nothing to do with CI. This test asserts CONTENT; the terminator is not
     * part of what it is checking.
     */
    private fun read(relative: String): String = RepoFiles.text(relative).replace("\r\n", "\n")

    private val ci: String get() = read(".github/workflows/ci.yml")
    private val manual: String get() = read("docs/CI.md")

    /** A literal `$`, spelled so a Kotlin raw string does not read it as a template. */
    private val dollar = '$'

    /**
     * The `name:` of every STEP in ci.yml. Six spaces and `- name:` is a step in
     * the job's `steps:` list; the workflow's own top-level `name:` and an
     * artifact's `name:` under `with:` sit at different indentation and are not
     * steps. Steps written as a bare `- uses:` have no name and nothing to
     * document.
     */
    private fun ciStepNames(): List<String> =
        Regex("(?m)^ {6}- name: (.+)").findAll(ci).map { it.groupValues[1].trim() }.toList()

    /**
     * Every step name the manual's `## CI, step by step` table claims, read out
     * of the FIRST COLUMN of that table only. The rest of the document mentions
     * step names in passing and in abbreviated form, and a checker that scraped
     * those would pin abbreviations.
     */
    private fun documentedStepNames(): List<String> {
        val section = manual.substringAfter("## CI, step by step", "").substringBefore("\n## ")
        assertTrue(section.isNotBlank(), "docs/CI.md must keep its `## CI, step by step` section")
        return section.lineSequence()
            .filter { it.startsWith("| `") }
            .map { it.removePrefix("|").substringBefore('|') }
            .flatMap { firstCell -> Regex("`([^`]+)`").findAll(firstCell).map { it.groupValues[1] } }
            .toList()
    }

    @Test
    fun everyStepInCiYmlIsDocumentedAndEveryDocumentedStepExists() {
        val inYml = ciStepNames()
        val inDocs = documentedStepNames()
        assertTrue(inYml.size >= 10, "ci.yml should have a named step per check; found ${inYml.size}")

        val undocumented = inYml - inDocs.toSet()
        assertTrue(
            undocumented.isEmpty(),
            "these ci.yml steps are not in docs/CI.md's `## CI, step by step` table, so an operator " +
                "reading a red in them is told nothing: $undocumented"
        )

        val invented = inDocs - inYml.toSet()
        assertTrue(
            invented.isEmpty(),
            "docs/CI.md documents steps ci.yml does not have - renamed or deleted, and the manual " +
                "now describes a workflow that does not exist: $invented"
        )
    }

    @Test
    fun theTwoGateStepsExistAndBothReadTheSameClassifier() {
        val steps = ciStepNames()
        assertTrue(
            "Classify the tally (ours, theirs, and skips)" in steps,
            "ci.yml must keep the step that IS the verdict; without it gradle's exit code decides " +
                "merges again, and a proven upstream outage cannot be told from a real failure"
        )
        assertTrue(
            "Fail on any skipped test" in steps,
            "ci.yml must keep the independent second reader of the XMLs - a skip is a test that did " +
                "not run, and two readers are why a bug in either cannot make one invisible"
        )
        assertTrue(
            ci.contains("node scripts/gate-tally.mjs --dir \"${dollar}PWD\""),
            "the verdict must be scripts/gate-tally.mjs, the SAME classifier scripts/loop-gate.mjs " +
                "imports. Two gates with different definitions of green is one gate and one bypass"
        )
        assertTrue(
            ci.contains("ALLOWED = set()"),
            "the skip check keeps an EMPTY allowlist: the third status is for a proven THIRD-PARTY " +
                "outage, never a way to stop counting a skip"
        )
    }

    @Test
    fun theJobSummaryIsWrittenOnEveryRunAndIsNotTheVerdict() {
        assertTrue(
            "Write the job summary" in ciStepNames(),
            "an operator must not have to open a 40-minute log to learn which tests failed"
        )
        val summaryStep = ci.substringAfter("- name: Write the job summary").substringBefore("- name: ")
        assertTrue(
            summaryStep.contains("if: always()"),
            "the summary must be written even when the suite step timed out or was cancelled"
        )
        assertTrue(
            summaryStep.contains("node scripts/ci-summary.mjs"),
            "the summary comes from scripts/ci-summary.mjs"
        )
        val summaryScript = read("scripts/ci-summary.mjs")
        assertTrue(
            summaryScript.contains("from './gate-tally.mjs'"),
            "ci-summary.mjs must IMPORT the classifier rather than re-implement it - a second " +
                "implementation of \"what is a pass\" is a second gate"
        )
        assertTrue(
            summaryScript.contains("process.exit(0)") && !summaryScript.contains("process.exit(1)"),
            "ci-summary.mjs is a presenter: it must never set a non-zero exit code, or it becomes a " +
                "gate with rules of its own"
        )
        // The summary runs BEFORE the verdict, so a red classification cannot
        // stop the table from being written.
        assertTrue(
            ci.indexOf("- name: Write the job summary") <
                ci.indexOf("- name: Classify the tally (ours, theirs, and skips)"),
            "the summary must be written before the step that reds the build"
        )
    }

    @Test
    fun bothArtifactsAreUploadedOnEveryRunWithARetention() {
        for (artifact in listOf("Upload test report", "Upload upstream evidence")) {
            val step = ci.substringAfter("- name: $artifact").substringBefore("- name: ")
            assertTrue(
                step.contains("if: always()"),
                "`$artifact` must upload on a GREEN run too: it was `if: failure()`, which left " +
                    "nothing behind for a cancelled or timed-out job and nothing to compare a slow " +
                    "run against"
            )
            assertTrue(
                Regex("retention-days: \\d+").containsMatchIn(step),
                "`$artifact` must state a retention; the 90-day default is what put 14.4 GB into " +
                    "the artifact quota in four days"
            )
        }
    }

    @Test
    fun theTriggersAndConcurrencyAreWhatTheManualDescribes() {
        assertTrue(
            Regex("(?s)push:\\s*\\n\\s*branches: \\[main]").containsMatchIn(ci),
            "CI gates pushes to main"
        )
        assertTrue(
            Regex("(?s)pull_request:\\s*\\n\\s*branches: \\[main]").containsMatchIn(ci),
            "docs/CI.md promises the same gate on a pull request INTO MAIN, scoped with `branches:`"
        )
        val concurrency = ci.substringAfter("\nconcurrency:").substringBefore("\njobs:")
        assertTrue(
            concurrency.contains("github.ref") && concurrency.contains("cancel-in-progress: true"),
            "docs/CI.md promises a superseded push is cancelled, grouped by REF so a pull-request " +
                "run never cancels main's. Found: ${concurrency.trim()}"
        )
        assertTrue(
            manual.contains("cancel-in-progress: true") && manual.contains("cancel-in-progress: false"),
            "docs/CI.md must explain both settings - CI cancels, the publishing workflows do not"
        )
    }

    /**
     * The budgets are quoted in docs/CI.md as MEASURED numbers with the run ids
     * they came from. A budget that says one thing in the manual and another in
     * the yml is the same defect as an undocumented step, wearing a number.
     */
    @Test
    fun theTimeoutBudgetsMatchTheOnesTheManualQuotes() {
        val jobTimeout = Regex("(?m)^ {4}timeout-minutes: (\\d+)").find(ci)?.groupValues?.get(1)
        assertEquals("80", jobTimeout, "docs/CI.md quotes the CI job budget as 80 minutes")

        val stepTimeouts = Regex("(?m)^ {6}- name: (.+)\\n {8}(?:id: \\w+\\n {8})?timeout-minutes: (\\d+)")
            .findAll(ci).associate { it.groupValues[1].trim() to it.groupValues[2] }
        assertEquals(
            mapOf(
                "Run the unit suite" to "45",
                "Start server (full toolset) and run the end-to-end sweep" to "30",
            ),
            stepTimeouts,
            "docs/CI.md derives these two per-STEP budgets from the last twelve runs and explains " +
                "that the job budget sits above their sum so a step timeout fires first. Change one " +
                "and the manual's arithmetic stops being true"
        )
        for (n in listOf("45", "30", "80")) {
            assertTrue(
                manual.contains("| $n |") || manual.contains("`timeout-minutes: $n`"),
                "docs/CI.md's duration table must carry the budget $n it explains"
            )
        }
    }

    /**
     * The manual's workflow table is the index every other section hangs off. A
     * workflow added or renamed without a row there is a workflow nobody knows
     * how to read.
     */
    @Test
    fun theManualListsEveryWorkflowByFileAndByDisplayName() {
        val files = RepoFiles.root.resolve(".github/workflows").toFile()
            .listFiles { f -> f.name.endsWith(".yml") }?.map { it.name }?.sorted() ?: emptyList()
        assertTrue(files.isNotEmpty(), "no workflows found under .github/workflows")
        for (file in files) {
            assertTrue(
                manual.contains("`$file`"),
                "docs/CI.md's workflow table must have a row for `$file` - what triggers it and " +
                    "what it decides"
            )
            val displayName = Regex("(?m)^name: (.+)")
                .find(read(".github/workflows/$file"))?.groupValues?.get(1)?.trim()
            assertTrue(
                displayName != null && manual.contains(displayName),
                "docs/CI.md must call `$file` by the name GitHub shows for it ($displayName), " +
                    "because that is the name an operator sees on the Actions tab"
            )
        }
    }

    /**
     * Every workflow that runs the suite has to run the same gate. This is the
     * check that would have caught nightly-fuzz.yml taking gradle's exit code as
     * its verdict - reddening every night on a proven upstream outage so the
     * fuzzer never started - and release.yml publishing the jar end users
     * download behind a suite with no database, no chr and no live flag.
     */
    @Test
    fun everyWorkflowThatRunsTheSuiteRunsTheWholeGate() {
        for (file in listOf("ci.yml", "nightly-fuzz.yml", "release.yml")) {
            val text = read(".github/workflows/$file")
            assertTrue(
                text.contains(":app:test"),
                "$file is listed here as a suite-running workflow but does not run :app:test"
            )
            for (required in listOf(
                "node scripts/gate-tally.mjs",
                "ALLOWED = set()",
                "CHROMIA_TEST_DATABASE_URL",
                "CHROMIA_LIVE_PROVISIONING_TESTS",
                "CHROMIA_REQUIRE_CHR",
            )) {
                assertTrue(
                    text.contains(required),
                    "$file runs the suite, so it must be the SAME gate as ci.yml, and it is missing " +
                        "`$required`. A workflow whose suite skips what ci.yml runs, or whose verdict " +
                        "is gradle's exit code, is a second definition of green"
                )
            }
            assertTrue(
                Regex("(?m)^ {4}timeout-minutes: \\d+").containsMatchIn(text),
                "$file must bound its job; GitHub's default is six hours"
            )
        }
    }
}
