package org.chromia

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.io.File

/**
 * ROUND 20, THE GATE'S TWO ARGUMENTS AND THE PRESENTER'S TWO INPUTS.
 *
 * `scripts/gate-tally.mjs` is the single classifier and `report()` is the
 * verdict. It fails a run on FIVE conditions:
 *
 *     t.stale.length            result files predate this run
 *     t.tests === 0             zero tests recorded
 *     expectMin && t.tests < n  the suite was silently narrowed
 *     t.red.length              failures that are ours
 *     t.skippedNames.length     any skip
 *
 * Two of the five are reachable only through flags, and NO workflow passes
 * either of them:
 *
 *   * `--expect-min` is the size check. Without it the only size assertion in CI
 *     is `t.tests === 0`, so a suite narrowed from hundreds of tests to one is a
 *     green build. `docs/ADVERSARY-ROUND-BRIEF.md` tells a reviewer to run
 *     `scripts/loop-gate.mjs` with `--expect-min` set to the previously verified
 *     count BY HAND, which is the same check CI declines to make.
 *   * `--started-at` is what makes `stale` computable at all
 *     (`if (startedAt !== null && ...)`) and what makes the upstream freshness
 *     rule mean "inside this run" rather than "inside the last ninety minutes"
 *     (`startedAt ?? (Math.min(...mtimes) - TEST_TASK_TIMEOUT_MS)`). Measured in
 *     `exploit-corpus/realworld/adversary-round20/upstream`: a warning dated an
 *     HOUR before the run is accepted as proven when the flag is absent, and
 *     refused when it is present.
 *
 * And `scripts/ci-summary.mjs` - which opens "A PRESENTER, NEVER A VERDICT" and
 * is right that it does not set the exit code - renders a verdict WORD from two
 * of those five inputs:
 *
 *     const verdict = ours || t.skippedNames.length ? 'RED - this build does not ship' : ...
 *
 * so a run the gate fails on `stale`, on `tests === 0` or on `expectMin` is
 * headed `## CI gate: GREEN`. Measured with both real scripts in
 * `exploit-corpus/realworld/adversary-round20/ci`: three of five cases disagree.
 *
 * This test pins the two facts a fix would change. Red here means the finding is
 * CLOSED - flip it and say so, the way a corpus GAP row is flipped to CAUGHT.
 */
class Round20CiVerdictProbeTest {

    private fun read(path: String) = File("..", path).readText()

    private val workflows = listOf(
        ".github/workflows/ci.yml",
        ".github/workflows/release.yml",
        ".github/workflows/nightly-fuzz.yml",
        ".github/workflows/embeddings-refresh.yml"
    )

    @Test
    fun `no workflow passes expect-min or started-at to the gate`() {
        val rows = buildJsonArray {
            workflows.forEach { path ->
                val text = read(path)
                add(
                    buildJsonObject {
                        put("workflow", path)
                        put("runs_gate_tally", text.contains("scripts/gate-tally.mjs"))
                        put("expect_min", text.contains("--expect-min"))
                        put("started_at", text.contains("--started-at"))
                    }
                )
            }
        }
        Round20Evidence.record("ci/workflow-flags.json", rows)
        val withFlags = workflows.filter { read(it).contains("--expect-min") || read(it).contains("--started-at") }
        assertAll(
            Executable {
                assertEquals(
                    emptyList<String>(),
                    withFlags,
                    "a workflow now passes --expect-min or --started-at, so one of round 20's two CI findings " +
                        "is CLOSED: ${withFlags.joinToString()}. Flip the row and say which check it arms."
                )
            },
            Executable { Round20Evidence.assertFrozen("ci/workflow-flags.json", rows) }
        )
    }

    @Test
    fun `the job summary computes its verdict word from two of the gate's five conditions`() {
        val summary = read("scripts/ci-summary.mjs")
        val tally = read("scripts/gate-tally.mjs")
        // The presenter's verdict expression, verbatim. `t.stale.length` DOES appear
        // further down the file - in the "### N result file(s) predate this run"
        // section - which is exactly the point: the summary knows about it and its
        // headline does not, so the statement is read on its own rather than the file.
        val verdictHead = "const ours = t.red.length;"
        val verdictTail = "'GREEN';"
        val from = summary.indexOf(verdictHead)
        val to = summary.indexOf(verdictTail, from)
        val verdictStatement =
            if (from < 0 || to < 0) "" else summary.substring(from, to + verdictTail.length)
        // The three conditions report() fails on that the verdict word ignores.
        val gateOnly = listOf("t.stale.length", "t.tests === 0", "expectMin && t.tests < expectMin")
        val rows = buildJsonObject {
            put("presenter_verdict_statement", verdictStatement.replace(Regex("""\s+"""), " "))
            put(
                "conditions_the_gate_fails_on_that_the_headline_ignores",
                buildJsonArray {
                    gateOnly.forEach { c ->
                        add(
                            buildJsonObject {
                                put("condition", c)
                                put("in_gate_report", tally.contains(c))
                                put("in_presenter_verdict_statement", verdictStatement.contains(c))
                                put("anywhere_in_the_presenter", summary.contains(c))
                            }
                        )
                    }
                }
            )
        }
        Round20Evidence.record("ci/verdict-inputs.json", rows)
        val missing = gateOnly.filter { tally.contains(it) && !verdictStatement.contains(it) }
        assertAll(
            Executable {
                // The finding: all three are in the gate and none in the presenter.
                assertEquals(
                    gateOnly,
                    missing,
                    "the job summary's verdict now reads a condition it used to ignore, so round 20's CI finding " +
                        "is (partly) CLOSED. Flip the row and name which one."
                )
            },
            Executable { Round20Evidence.assertFrozen("ci/verdict-inputs.json", rows) }
        )
    }
}
