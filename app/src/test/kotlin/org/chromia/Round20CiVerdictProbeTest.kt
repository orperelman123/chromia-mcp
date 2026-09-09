package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime

/**
 * ROUND 20, THE GATE'S TWO ARGUMENTS AND THE PRESENTER'S TWO INPUTS.
 *
 * `scripts/gate-tally.mjs` is the single classifier and its verdict is what reds
 * a merge. It fails a run on FIVE conditions:
 *
 *     every result file belongs to this run       result files predate this run
 *     the suite recorded tests                    tests === 0
 *     the suite was not narrowed below the floor  fewer tests than the floor
 *     no failure or error is ours                 failures that are ours
 *     nothing skipped                             any skip
 *
 * WHAT ROUND 20 FOUND (sections 4 and 5 of the round README). Two of the five
 * were reachable only through flags, and NO workflow passed either -
 * `grep -c expect-min` over the four files in `.github/workflows`, and the same
 * count for `started-at`, were `0` in all four:
 *
 *   * `--expect-min` was the size check. Without it the only size assertion was
 *     `tests === 0`, so a suite narrowed from 1650 tests to ONE was a green
 *     build.
 *   * `--started-at` was what made `stale` computable at all
 *     (`if (startedAt !== null && ...)`) and what made the upstream freshness
 *     rule mean "inside this run" rather than "inside the last ninety minutes"
 *     (`startedAt ?? (Math.min(...mtimes) - TEST_TASK_TIMEOUT_MS)`).
 *
 * And `scripts/ci-summary.mjs` - which opens "A PRESENTER, NEVER A VERDICT" and
 * is right that it does not set the exit code - rendered a verdict WORD computed
 * from two of those five inputs:
 *
 *     const verdict = ours || t.skippedNames.length ? 'RED - this build does not ship' : ...
 *
 * so a run the gate failed on `stale`, on `tests === 0` or on the floor was
 * headed `## CI gate: GREEN`. Three of five measured cases disagreed.
 *
 * WHAT BECAME OF IT (2026-09-09, `fix/round20-gate`). Both arguments are DERIVED
 * and the headline is the gate's own:
 *
 *   * the run's start comes from `app/build/test-run/starts.tsv`, which
 *     `:app:test`'s `doFirst` writes before the first test executes;
 *   * the floor comes from the committed `ci/expected-min.json`, which
 *     `scripts/loop-gate.mjs` ratchets UP after a green full run;
 *   * `verdict(t, { expectMin })` in `gate-tally.mjs` is the ONE statement of
 *     the five conditions. `report()` prints it and the process exits on it,
 *     `--json` exits on it, and `ci-summary.mjs` renders its `headline`
 *     verbatim. The presenter cannot disagree with the exit code because it no
 *     longer computes anything.
 *
 * This class pins that through BOTH REAL SCRIPTS, over five result directories
 * built here - one per condition, so a case that goes green names WHICH check
 * stopped working rather than saying that "something" did.
 *
 * The JUnit XML in these five directories is written by hand, and that is a
 * deliberate difference from [UpstreamWarningGateTest] and
 * [Round20UpstreamBindingProbeTest], which both drive the real reporter. What is
 * under test here is the ARITHMETIC of the verdict over a results directory -
 * "three tests, none failing, one file older than the marker" - and two of the
 * five shapes (a suite that recorded zero tests, a suite narrowed to one) are
 * exactly the shapes a real run cannot be asked to produce on demand. Where the
 * BINDING between a failure message and an evidence file is what is being
 * proven, the XML has to come from the reporter, and there it does.
 */
class Round20CiVerdictProbeTest {

    private fun read(relative: String) = RepoFiles.text(relative).replace("\r\n", "\n")

    /**
     * The file's CODE - every whole-line `//` comment and every JSDoc/KDoc
     * continuation line removed.
     *
     * Not tidiness. Both scripts now QUOTE the defects this class pins, in their
     * own header comments, so that whoever reads them next understands why the
     * derivation exists:
     *
     *     //          const ours = t.red.length;
     *     //          const verdict = ours || t.skippedNames.length ? 'RED ...' : ... 'GREEN';
     *      *     const runStart = startedAt ?? (Math.min(...mtimes) - TEST_TASK_TIMEOUT_MS);
     *
     * A probe that searched the raw text would find the FIX's own explanation of
     * the bug and report the bug - measured 2026-09-09, both of them. Reading the
     * code and not the prose is what makes "this expression is gone" mean it.
     */
    private fun codeOnly(source: String): String =
        source.lineSequence()
            .filterNot { val t = it.trimStart(); t.startsWith("//") || t.startsWith("*") }
            .joinToString("\n")

    private val workflows = listOf(
        ".github/workflows/ci.yml",
        ".github/workflows/release.yml",
        ".github/workflows/nightly-fuzz.yml",
        ".github/workflows/embeddings-refresh.yml"
    )

    /** The three workflows that run the suite and therefore carry the whole gate. */
    private val suiteWorkflows = listOf(
        ".github/workflows/ci.yml",
        ".github/workflows/release.yml",
        ".github/workflows/nightly-fuzz.yml"
    )

    // ---- the five result directories --------------------------------------

    /**
     * One case is a MINIATURE CHECKOUT - `ci/expected-min.json`,
     * `app/build/test-run/starts.tsv`, one XML under `app/build/test-results/test`
     * and an empty `app/build/upstream/warnings` - so both scripts run exactly as
     * CI runs them: `--dir "$PWD"` and nothing else. Pointing `--results`
     * somewhere else would not exercise the floor at all, because the committed
     * floor is applied only to a repository's OWN results directory.
     */
    private data class Case(
        val name: String,
        val claim: String,
        /** `null` writes no `ci/expected-min.json`, which is a checkout with no floor. */
        val floor: Int?,
        val xml: String,
        /** Backdates the XML to one second BEFORE the run-start marker. */
        val backdated: Boolean,
        /** The condition `verdict()` must fail on, or `null` for the green control. */
        val failing: String?
    )

    private fun suiteXml(cls: String, tests: Int, failures: Int): String {
        val bodies = StringBuilder()
        repeat(tests - failures) { i ->
            bodies.append("  <testcase name=\"passes$i()\" classname=\"org.chromia.$cls\" time=\"0.01\"/>\n")
        }
        repeat(failures) { i ->
            bodies.append("  <testcase name=\"fails$i()\" classname=\"org.chromia.$cls\" time=\"0.02\">\n")
            bodies.append(
                "    <failure message=\"expected true, was false\" " +
                    "type=\"org.opentest4j.AssertionFailedError\">stack</failure>\n"
            )
            bodies.append("  </testcase>\n")
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<testsuite name=\"org.chromia.$cls\" tests=\"$tests\" skipped=\"0\" failures=\"$failures\" " +
            "errors=\"0\" timestamp=\"2026-09-09T00:00:00\" hostname=\"probe\" time=\"1.0\">\n" +
            bodies +
            "</testsuite>\n"
    }

    private val cases = listOf(
        Case(
            name = "ci20a_result_files_that_predate_the_run",
            claim = "every result file is older than the run-start marker. This is the check " +
                "report() calls fatal, and before the fix it was computed only when --started-at " +
                "was passed - which no workflow does",
            floor = 3,
            xml = suiteXml("ManyTest", tests = 3, failures = 0),
            backdated = true,
            failing = "every result file belongs to this run"
        ),
        Case(
            name = "ci20b_a_suite_that_recorded_zero_tests",
            claim = "a result file with tests=\"0\", in a checkout with no floor at all - the one " +
                "size check that always worked, isolated",
            floor = null,
            xml = suiteXml("EmptyTest", tests = 0, failures = 0),
            backdated = false,
            failing = "the suite recorded tests"
        ),
        Case(
            name = "ci20c_a_suite_narrowed_below_the_committed_floor",
            claim = "ONE test where ci/expected-min.json says 900. Before the fix the floor was " +
                "--expect-min, which nothing passed, so this was a green build",
            floor = 900,
            xml = suiteXml("NarrowedTest", tests = 1, failures = 0),
            backdated = false,
            failing = "the suite was not narrowed below the floor"
        ),
        Case(
            name = "ci20d_control_a_failure_that_is_ours",
            claim = "CONTROL: an ordinary red, which is the only kind of red the old headline " +
                "could see",
            floor = 1,
            xml = suiteXml("OursTest", tests = 2, failures = 1),
            backdated = false,
            failing = "no failure or error is ours"
        ),
        Case(
            name = "ci20e_control_a_clean_run",
            claim = "CONTROL: three tests, none failing, at the floor. Nothing is wrong and both " +
                "must say so - a fix that reds everything agrees with itself too",
            floor = 3,
            xml = suiteXml("CleanTest", tests = 3, failures = 0),
            backdated = false,
            failing = null
        )
    )

    /** Builds [case] as a miniature checkout and returns its root. */
    private fun materialise(case: Case): File {
        val root = RepoFiles.root.resolve("app/build/round20-ci-verdict/${case.name}").toFile()
        root.deleteRecursively()
        val results = File(root, "app/build/test-results/test")
        results.mkdirs()
        File(root, "app/build/upstream/warnings").mkdirs()

        // THE MARKER, exactly as `:app:test`'s doFirst writes it: one row,
        // `<epoch-ms>\t<ISO-8601>\t<task path>`.
        val markerAt = System.currentTimeMillis() - 60_000
        val marker = File(root, "app/build/test-run/starts.tsv")
        marker.parentFile.mkdirs()
        marker.writeText("$markerAt\t${java.time.Instant.ofEpochMilli(markerAt)}\t:app:test\n")

        if (case.floor != null) {
            File(root, "ci").mkdirs()
            File(root, "ci/expected-min.json").writeText(
                "{\n  \"expectMin\": ${case.floor},\n" +
                    "  \"verifiedBy\": \"Round20CiVerdictProbeTest, a fixture floor\",\n" +
                    "  \"verifiedAt\": \"2026-09-09\",\n" +
                    "  \"why\": \"the floor the gate reads instead of a remembered --expect-min\"\n}\n"
            )
        }

        val xml = File(results, "TEST-probe.xml")
        xml.writeText(case.xml)
        val at = if (case.backdated) markerAt - 1_000 else markerAt + 1_000
        Files.setLastModifiedTime(xml.toPath(), FileTime.fromMillis(at))
        return root
    }

    private fun gateTally(root: File, json: Boolean): NodeOnPath.Ran =
        NodeOnPath.exec(
            RepoFiles.root.resolve("scripts/gate-tally.mjs").toFile(),
            listOf("--dir", root.absolutePath) + (if (json) listOf("--json") else emptyList()),
            RepoFiles.root.toFile()
        )

    private fun ciSummary(root: File): NodeOnPath.Ran =
        NodeOnPath.exec(
            RepoFiles.root.resolve("scripts/ci-summary.mjs").toFile(),
            listOf("--dir", root.absolutePath),
            RepoFiles.root.toFile()
        )

    /** The `## CI gate: ...` line the job summary opens with. */
    private fun headline(markdown: String): String =
        markdown.lineSequence().firstOrNull { it.startsWith("## CI gate:") }
            ?: "<no `## CI gate:` line in ${markdown.length} bytes of summary>"

    /** What one case measured through both real scripts. */
    private data class Measured(
        val case: Case,
        val gateExit: Int,
        val gateGreen: Boolean,
        val failed: List<String>,
        val conditions: List<String>,
        val summaryExit: Int,
        val headline: String,
        val summary: String
    ) {
        val saysGreen: Boolean get() = headline.removePrefix("## CI gate: ").startsWith("GREEN")
    }

    private fun measure(case: Case): Measured {
        val root = materialise(case)
        val plain = gateTally(root, json = false)
        val asJson = gateTally(root, json = true)
        assertTrue(
            asJson.stdout.trimStart().startsWith("{"),
            "gate-tally.mjs --json must print the tally for ${case.name}.\n${asJson.transcript()}"
        )
        val verdict = Json.parseToJsonElement(asJson.stdout).jsonObject.getValue("verdict").jsonObject
        val conditions = verdict.getValue("conditions").jsonArray.map { it.jsonObject }
        val summary = ciSummary(root)
        return Measured(
            case = case,
            gateExit = plain.exit,
            gateGreen = verdict.getValue("green").jsonPrimitive.content == "true",
            failed = conditions.filter { it.getValue("failed").jsonPrimitive.content == "true" }
                .map { it.getValue("condition").jsonPrimitive.content },
            conditions = conditions.map { it.getValue("condition").jsonPrimitive.content },
            summaryExit = summary.exit,
            headline = headline(summary.stdout),
            summary = summary.stdout
        )
    }

    @Test
    fun theGateAndTheJobSummaryAgreeOnEveryOneOfTheFiveConditions() {
        val measured = cases.map { measure(it) }

        val rows = buildJsonArray {
            measured.forEach { m ->
                addJsonObject {
                    put("case", m.case.name)
                    put("claim", m.case.claim)
                    put("gate_exit", m.gateExit)
                    put("gate_green", m.gateGreen)
                    putJsonArray("failed_conditions") { m.failed.forEach { add(it) } }
                    put("summary_headline", m.headline)
                    put("summary_exit", m.summaryExit)
                    put("summary_says_green", m.saysGreen)
                    put("disagree", (m.gateExit == 0) != m.saysGreen)
                }
            }
        }
        Round20Evidence.record("ci/verdict-agreement.json", rows)

        val disagreements = measured.filter { (it.gateExit == 0) != it.saysGreen }.map { it.case.name }
        val wrongCondition = measured.mapNotNull { m ->
            val expected = listOfNotNull(m.case.failing)
            if (m.failed == expected) null else "${m.case.name}: expected $expected, got ${m.failed}"
        }

        assertAll(
            Executable {
                assertEquals(
                    emptyList<String>(),
                    disagreements,
                    "the job summary's headline disagrees with the gate's exit code. That is the " +
                        "round-20 finding back: a presenter that computes its own verdict word is a " +
                        "second definition of green, and the headline is what an operator reads\n  " +
                        disagreements.joinToString("\n  ")
                )
            },
            Executable {
                assertEquals(
                    emptyList<String>(),
                    wrongCondition,
                    "a case failed on a condition other than the one it isolates, so these five are " +
                        "no longer measuring five separate checks\n  " + wrongCondition.joinToString("\n  ")
                )
            },
            Executable {
                assertEquals(
                    listOf("ci20e_control_a_clean_run"),
                    measured.filter { it.gateExit == 0 }.map { it.case.name },
                    "exactly one of the five is the green control; a fix that reds everything agrees " +
                        "with itself and proves nothing"
                )
            },
            Executable { Round20Evidence.assertFrozen("ci/verdict-agreement.json", rows) }
        )
    }

    @Test
    fun theJobSummaryRendersTheGatesOwnVerdictAndComputesNothing() {
        val summarySource = read("scripts/ci-summary.mjs")
        val summaryCode = codeOnly(summarySource)
        val tallySource = read("scripts/gate-tally.mjs")

        // The presenter's old verdict expression, read as a STATEMENT rather
        // than as a file: `t.stale.length` appears further down the summary - in
        // the "### N result file(s) predate this run" section - which was exactly
        // the finding, that the file knew about a condition its headline did not.
        val verdictHead = "const ours = t.red.length;"
        val verdictTail = "'GREEN';"
        val from = summaryCode.indexOf(verdictHead)
        val to = if (from < 0) -1 else summaryCode.indexOf(verdictTail, from)
        val computedVerdictStatement =
            if (from < 0 || to < 0) "" else summaryCode.substring(from, to + verdictTail.length)

        // The conditions come from the REAL verdict object of the clean control
        // rather than from a list of five strings written here, which would agree
        // with itself the day a sixth condition is added.
        val clean = measure(cases.single { it.failing == null })

        val rows = buildJsonObject {
            put("presenter_computes_a_verdict_word", computedVerdictStatement.isNotEmpty())
            put(
                "presenter_imports_the_gates_verdict",
                summarySource.contains("verdict") && summarySource.contains("from './gate-tally.mjs'")
            )
            put("presenter_renders_the_headline_verbatim", summarySource.contains("v.headline"))
            put("one_statement_of_the_conditions", "verdict(t, { expectMin }) in scripts/gate-tally.mjs")
            put("headline_on_the_clean_control", clean.headline)
            put(
                "conditions",
                buildJsonArray {
                    clean.conditions.forEach { condition ->
                        addJsonObject {
                            put("condition", condition)
                            put("in_the_verdict_object", true)
                            put("rendered_by_the_presenter", clean.summary.contains(condition))
                            put("named_in_the_gate", tallySource.contains(condition))
                        }
                    }
                }
            )
        }
        Round20Evidence.record("ci/verdict-inputs.json", rows)

        val unrendered = clean.conditions.filterNot { clean.summary.contains(it) }
        assertAll(
            Executable {
                assertEquals(
                    "",
                    computedVerdictStatement,
                    "scripts/ci-summary.mjs computes a verdict word of its own again. Two renderings " +
                        "of what a pass is is the same defect as two classifiers - the headline must " +
                        "be `verdict(...).headline` out of gate-tally.mjs, which is the object the " +
                        "process exits on"
                )
            },
            Executable {
                assertTrue(
                    summarySource.contains("v.headline"),
                    "the summary must render the gate's own `headline` field verbatim"
                )
            },
            Executable {
                assertEquals(
                    5,
                    clean.conditions.size,
                    "the gate fails on five conditions; if that number changed, this class and " +
                        "docs/CI.md's table have to change with it: ${clean.conditions}"
                )
            },
            Executable {
                assertEquals(
                    emptyList<String>(),
                    unrendered,
                    "the job summary must show every condition the gate fails on WITH ITS STATE. A " +
                        "condition the presenter does not name is a red an operator cannot see:\n  " +
                        unrendered.joinToString("\n  ")
                )
            },
            Executable { Round20Evidence.assertFrozen("ci/verdict-inputs.json", rows) }
        )
    }

    @Test
    fun neitherArgumentIsPassedByAnyWorkflowBecauseBothAreDerived() {
        // CODE, not prose: `app/build.gradle.kts` and `scripts/gate-tally.mjs`
        // both explain the round-20 defect in their own comments, quoting the
        // deleted expression verbatim, so a raw-text search finds the fix's
        // explanation and calls it the bug.
        val gradle = codeOnly(read("app/build.gradle.kts"))
        val tallySource = codeOnly(read("scripts/gate-tally.mjs"))
        val rows = buildJsonObject {
            put(
                "workflows",
                buildJsonArray {
                    workflows.forEach { path ->
                        val text = read(path)
                        addJsonObject {
                            put("workflow", path)
                            put("runs_gate_tally", text.contains("scripts/gate-tally.mjs"))
                            put("expect_min", text.contains("--expect-min"))
                            put("started_at", text.contains("--started-at"))
                            put("second_reader_of_the_floor", text.contains("ci/expected-min.json"))
                        }
                    }
                }
            )
            put("the_test_task_writes_a_start_marker", gradle.contains("test-run/starts.tsv"))
            put("the_gate_reads_that_marker", tallySource.contains("RUN_START_MARKER"))
            put("the_timeout_subtracting_fallback_is_gone", !tallySource.contains("- TEST_TASK_TIMEOUT_MS)"))
            put("the_floor_is_committed", RepoFiles.exists("ci/expected-min.json"))
            put("the_gate_reads_that_floor", tallySource.contains("EXPECTED_MIN_FILE"))
            put(
                "the_merge_gate_ratchets_the_floor",
                codeOnly(read("scripts/loop-gate.mjs")).contains("RAISED the floor")
            )
        }
        Round20Evidence.record("ci/workflow-flags.json", rows)

        val floor = Json.parseToJsonElement(read("ci/expected-min.json")).jsonObject
        val missingSecondReader = suiteWorkflows.filterNot { read(it).contains("ci/expected-min.json") }
        val withFlags = workflows.filter {
            read(it).contains("--expect-min") || read(it).contains("--started-at")
        }
        assertAll(
            Executable {
                // STILL zero, and now that is the fix rather than the finding: a
                // flag remembered in four workflows is a flag the fifth forgets.
                assertEquals(
                    emptyList<String>(),
                    withFlags,
                    "a workflow passes --expect-min or --started-at again. Both are DERIVED - the " +
                        "start from app/build/test-run/starts.tsv, the floor from " +
                        "ci/expected-min.json - and passing one by hand is how a check comes to " +
                        "depend on somebody remembering it: ${withFlags.joinToString()}"
                )
            },
            Executable {
                assertTrue(
                    gradle.contains("test-run/starts.tsv") && gradle.contains("doFirst"),
                    "`:app:test` must write app/build/test-run/starts.tsv before its first test. " +
                        "Without that marker the gate cannot date its own run, and the freshness " +
                        "rule and the stale-results check go back to being flags nobody passes"
                )
            },
            Executable {
                assertTrue(
                    !tallySource.contains("- TEST_TASK_TIMEOUT_MS)"),
                    "the `startedAt ?? (Math.min(...mtimes) - TEST_TASK_TIMEOUT_MS)` fallback is " +
                        "back. It dated a run as ninety minutes before its oldest result file, which " +
                        "is how a warning written an hour earlier became proof"
                )
            },
            Executable {
                assertTrue(
                    floor.getValue("expectMin").jsonPrimitive.content.toInt() > 0,
                    "ci/expected-min.json must carry a positive floor; a floor of zero is the " +
                        "`tests === 0` check wearing a file"
                )
            },
            Executable {
                assertEquals(
                    emptyList<String>(),
                    missingSecondReader,
                    "every workflow that runs the suite must read the floor a SECOND time, " +
                        "independently, the way the two skip readers already do:\n  " +
                        missingSecondReader.joinToString("\n  ")
                )
            },
            Executable { Round20Evidence.assertFrozen("ci/workflow-flags.json", rows) }
        )
    }
}
