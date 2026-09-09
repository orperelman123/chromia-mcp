package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.security.MessageDigest
import java.time.Instant

/**
 * ROUND 20, SECTION 4 - THE FRESHNESS CLAUSE, AND THE MARKER THAT ARMS IT.
 *
 * Round 19 bound every UPSTREAM WARNING to its run three ways: the gate
 * recomputes the evidence file's digest from disk and matches it against the
 * digest in the failure message, it reads `docs/UPSTREAM.md` itself rather than
 * believing the evidence's ledger claim, and only a `FAILED_SIGNATURE` canary on
 * the INDEPENDENT path proves an outage. There is a fourth clause:
 *
 *     if (at < startedAt) return { ok: false, why: `evidence is dated ${w.at}, before this run
 *                                  started ... - a warning from an earlier run cannot excuse this one` }
 *
 * WHAT ROUND 20 FOUND. `startedAt` was computed as
 *
 *     const runStart = startedAt ?? (Math.min(...mtimes) - TEST_TASK_TIMEOUT_MS);   // 90 minutes
 *
 * from the `--started-at` flag, and NO workflow passed it
 * (`grep -c started-at .github/workflows/*.yml` was `0` in all four). So in CI
 * the freshness window was "ninety minutes before the oldest result file": an
 * evidence file written an HOUR before the run was accepted as a proven outage.
 * Worse, `stale` - the check `report()` calls fatal, *"you are reading someone
 * else's evidence"* - was filled only `if (startedAt !== null)`, so it could not
 * fire in CI at all.
 *
 * WHAT BECAME OF IT (2026-09-09, `fix/round20-gate`). The run's start is DERIVED,
 * not defaulted and not a flag: `:app:test`'s own `doFirst` writes
 * `app/build/test-run/starts.tsv` before the first test executes, one row per
 * invocation, and the gate takes the EARLIEST. `--started-at` survives only as a
 * NARROWING override (`max(marker, flag)`), the timeout-subtracting fallback is
 * deleted, `stale` is computed on every classification, and with neither marker
 * nor flag the tally FAILS CLOSED.
 *
 * This class pins all of it against the REAL marker THIS RUN WROTE, through the
 * real `scripts/gate-tally.mjs`, run the way CI runs it - `--dir "$PWD"` and no
 * flags. Nothing here is a fixture of our own artefacts:
 *
 *  - the JUnit XML comes from the REAL launcher and the REAL
 *    `LegacyXmlReportGeneratingListener` over [Marker], a test that really
 *    fails, because the whole point of the digest clause is that the message in
 *    the XML was thrown by the process that wrote the evidence;
 *  - the run's start comes from the marker file `:app:test` wrote for THIS
 *    invocation, so a fix that stopped writing it fails here rather than
 *    quietly widening the window again;
 *  - the proof is the canary path (`FAILED_SIGNATURE`, independent, same
 *    signature as the tool's), not a `docs/UPSTREAM.md` entry, so what these
 *    cases isolate is the FRESHNESS clause and not the ledger's health.
 */
class Round20UpstreamBindingProbeTest {

    /**
     * A REAL test that fails with the REAL marker sentence, run through the REAL
     * launcher so the REAL reporter writes the XML the gate reads.
     *
     * GREEN in the ordinary suite and RED only when this file selects it: the
     * message is handed over in a system property that is set immediately around
     * the launcher call and cleared in a `finally`. There is deliberately no
     * early `return` - [AssumptionLedgerTest.anEarlyReturnFromATestMustFollowAnAssertion]
     * refuses a body that leaves before it claims anything, so the inert path
     * ASSERTS what makes it inert.
     */
    class Marker {
        @Test
        fun theExplorerRefusedTheQuery() {
            val armed = System.getProperty(MESSAGE_PROPERTY)
            if (armed != null) throw AssertionError(armed)
            assertNull(
                armed,
                "$MESSAGE_PROPERTY is set outside the launcher call in " +
                    "Round20UpstreamBindingProbeTest, which is the only place that may arm this " +
                    "marker. Left set, it turns a probe into a permanent red in every other run."
            )
        }
    }

    companion object {
        const val MESSAGE_PROPERTY = "org.chromia.round20.binding.message"

        /** The signature these cases use: one of the four the gate allowlists. */
        const val SIGNATURE = "explorer-graphql-internal-error"

        /** The third party's own words, in the spelling that signature matches. */
        const val ERROR_TEXT =
            "GraphQL Error: INTERNAL_ERROR for 0f2b1e6a-0000-0000-0000-000000000000"
    }

    private val repo: Path get() = RepoFiles.root

    /**
     * gate-tally keys on the TESTCASE's own classname with `$...` stripped, so a
     * nested class is keyed by its outer class
     * (`gate-tally.mjs`, `owner.replace(/\$.*$/, '')`).
     */
    private val key = "Round20UpstreamBindingProbeTest.theExplorerRefusedTheQuery"

    // ---- the run's start, as the test task recorded it ----------------------

    /**
     * The EARLIEST invocation in `app/build/test-run/starts.tsv` - the same
     * number `gate-tally.mjs` derives, read here the same way, from the file
     * `:app:test` wrote before this very test executed.
     *
     * Its absence is a failure, not a skip. The marker is what arms the
     * freshness clause and the stale-results check; a run that did not write one
     * is a run whose evidence cannot be dated, and saying so is the fix.
     */
    private val runStartMarker: Long by lazy {
        val marker = repo.resolve("app/build/test-run/starts.tsv").toFile()
        assertTrue(
            marker.isFile,
            "no run-start marker at ${marker.absolutePath}. `:app:test`'s doFirst writes one row " +
                "before its first test, and scripts/gate-tally.mjs derives the run's start from it - " +
                "without it the gate fails closed and this class cannot measure what it exists to " +
                "measure. If the suite was launched outside :app:test, that is the finding."
        )
        val starts = marker.readLines().filter { it.isNotBlank() }
            .mapNotNull { it.substringBefore('\t').trim().toLongOrNull() }
            .filter { it > 0 }
        assertTrue(
            starts.isNotEmpty(),
            "${marker.absolutePath} holds no usable row; each row is `<epoch-ms>\\t<ISO-8601>\\t<task>`"
        )
        starts.min()
    }

    // ---- the artefacts a case is made of ------------------------------------

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }

    /**
     * Evidence in the shape [LiveEnv.upstreamOutage] writes, proven by the
     * CANARY rather than by a ledger entry: outcome `FAILED_SIGNATURE`, measured
     * on the independent path, carrying the same allowlisted signature as the
     * tool's failure. Every clause but the freshness one is therefore satisfied,
     * which is what makes these cases about freshness.
     */
    private fun evidenceBytes(at: Instant): ByteArray {
        val body = buildJsonObject {
            put("test", key)
            put("tool", "filter_blockchains")
            put("query", "allBlockchains(state:)")
            put("signature", SIGNATURE)
            put("errorText", ERROR_TEXT)
            put("at", at.toString())
            put("canaryOutcome", "FAILED_SIGNATURE")
            put(
                "canary",
                buildJsonObject {
                    put("outcome", "FAILED_SIGNATURE")
                    put("signature", SIGNATURE)
                    put("independent", true)
                    put("at", at.toString())
                    put("explorerSaid", "INTERNAL_ERROR")
                }
            )
        }
        return body.toString().toByteArray(Charsets.UTF_8)
    }

    /**
     * The REAL JUnit XML for [Marker], written by the REAL report generator,
     * whose failure message is the one this process threw - digest and all.
     */
    private fun realJUnitXml(message: String): Path {
        val dir = Files.createTempDirectory("r20-binding-xml")
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectMethod(Marker::class.java, "theExplorerRefusedTheQuery"))
            .build()
        val launcher = LauncherFactory.create()
        System.setProperty(MESSAGE_PROPERTY, message)
        try {
            launcher.execute(request, LegacyXmlReportGeneratingListener(dir, PrintWriter(StringWriter())))
        } finally {
            System.clearProperty(MESSAGE_PROPERTY)
        }
        return dir
    }

    /** One classification, as CI runs it: `--dir "$PWD"`, `--json`, and no flags. */
    private fun classify(
        results: Path,
        warnings: Path,
        startedAt: Long? = null,
        repoDir: Path = repo
    ): Pair<Int, JsonObject> {
        val args = mutableListOf(
            "--dir", repoDir.toString(),
            "--results", results.toString(),
            "--warnings", warnings.toString(),
            "--json"
        )
        if (startedAt != null) args += listOf("--started-at", startedAt.toString())
        val ran = NodeOnPath.exec(repo.resolve("scripts/gate-tally.mjs").toFile(), args, repo.toFile())
        assertTrue(
            ran.stdout.trimStart().startsWith("{"),
            "gate-tally.mjs --json must print the tally.\n  args: ${args.joinToString(" ")}\n" +
                ran.transcript()
        )
        return ran.exit to Json.parseToJsonElement(ran.stdout).jsonObject
    }

    private fun keys(tally: JsonObject, field: String): List<String> =
        tally.getValue(field).jsonArray.map { it.jsonObject.getValue("key").jsonPrimitive.content }

    /** Builds one case's results + warnings directories and classifies them. */
    private fun measure(
        name: String,
        evidenceAt: Instant,
        xmlOlderThanTheMarker: Boolean = false,
        startedAt: Long? = null
    ): Triple<Int, JsonObject, Path> {
        val bytes = evidenceBytes(evidenceAt)
        val warnings = Files.createTempDirectory("r20-binding-$name")
        Files.write(warnings.resolve("$key.json"), bytes)
        val message = LiveEnv.UPSTREAM_WARNING_PREFIX +
            "filter_blockchains could not verify `allBlockchains(state:)` - the explorer answered " +
            "INTERNAL_ERROR [evidence sha256:${sha256(bytes)}]"
        val results = realJUnitXml(message)
        if (xmlOlderThanTheMarker) {
            Files.list(results).use { stream ->
                stream.filter { it.toString().endsWith(".xml") }.forEach {
                    Files.setLastModifiedTime(it, FileTime.fromMillis(runStartMarker - 1_000))
                }
            }
        }
        val (exit, tally) = classify(results, warnings, startedAt)
        return Triple(exit, tally, results)
    }

    private fun whyRed(tally: JsonObject): String =
        tally.getValue("red").jsonArray.map { it.jsonObject }
            .firstOrNull { it.getValue("key").jsonPrimitive.content == key }
            ?.get("why")?.jsonPrimitive?.content ?: ""

    @Test
    fun evidenceOlderThanTheMarkerIsRefusedAndEvidenceInsideTheRunIsAccepted() {
        val marker = Instant.ofEpochMilli(runStartMarker)
        val probes = listOf(
            Triple(
                "an_hour_before_the_marker",
                marker.minusSeconds(3_600),
                "round 20's own u20a, reclassified. This was ACCEPTED as a proven outage, because " +
                    "with no --started-at the window opened ninety minutes before the oldest result " +
                    "file"
            ),
            Triple(
                "one_second_before_the_marker",
                marker.minusMillis(1_000),
                "the boundary: the marker is the run's start to the millisecond, so a warning from " +
                    "the run before this one is an earlier run's evidence however recent"
            ),
            Triple(
                "inside_the_run",
                marker.plusMillis(1_000),
                "CONTROL: the honest case. A fix that refused everything would refuse this too"
            )
        )

        val rows = buildJsonArray {
            probes.forEach { (name, at, claim) ->
                val (exit, tally) = measure(name, at)
                addJsonObject {
                    put("case", name)
                    put("claim", claim)
                    put("evidence_relative_to_the_marker_ms", at.toEpochMilli() - runStartMarker)
                    put("accepted_as_upstream", keys(tally, "upstream").contains(key))
                    put("classified_red", keys(tally, "red").contains(key))
                    put("exit", exit)
                    put("stale_result_files", tally.getValue("stale").jsonArray.size)
                    put("refused_for_being_earlier", whyRed(tally).contains("before this run started"))
                }
            }
        }
        Round20Evidence.record("upstream/freshness-after-fix.json", rows)

        val byCase = rows.map { it.jsonObject }.associateBy { it.getValue("case").jsonPrimitive.content }
        fun bool(case: String, field: String) =
            byCase.getValue(case).getValue(field).jsonPrimitive.content == "true"

        assertAll(
            Executable {
                assertEquals(
                    false,
                    bool("an_hour_before_the_marker", "accepted_as_upstream"),
                    "a warning dated an HOUR before the run was accepted as a proven upstream " +
                        "outage. That is round 20's finding: the window is derived from the marker " +
                        "`:app:test` wrote, not from the oldest result file minus the task timeout"
                )
            },
            Executable {
                assertEquals(
                    false,
                    bool("one_second_before_the_marker", "accepted_as_upstream"),
                    "evidence ONE SECOND older than the run's start was accepted. The boundary is " +
                        "the marker, exactly; anything before it belongs to another run"
                )
            },
            Executable {
                assertTrue(
                    bool("an_hour_before_the_marker", "refused_for_being_earlier") &&
                        bool("one_second_before_the_marker", "refused_for_being_earlier"),
                    "both must be refused BY THE FRESHNESS CLAUSE - refused for some other reason " +
                        "would mean this class is measuring the digest or the canary instead"
                )
            },
            Executable {
                assertTrue(
                    bool("inside_the_run", "accepted_as_upstream"),
                    "evidence written DURING the run must still be accepted; a gate that refuses " +
                        "every warning has not learned to date one"
                )
            },
            Executable {
                assertEquals(
                    0,
                    byCase.getValue("inside_the_run").getValue("exit").jsonPrimitive.content.toInt(),
                    "a proven upstream outage is a red for the THIRD PARTY, not for us: it must not " +
                        "set the exit code"
                )
            },
            Executable { Round20Evidence.assertFrozen("upstream/freshness-after-fix.json", rows) }
        )
    }

    @Test
    fun theStaleCheckFiresWithNoFlagAndAStartedAtFlagCanOnlyNarrowTheWindow() {
        val marker = Instant.ofEpochMilli(runStartMarker)

        // 1. A RESULT FILE OLDER THAN THE MARKER, classified the way CI
        //    classifies - no --started-at at all. `tally` used to fill `stale`
        //    only when the flag was present, so this was invisible in CI.
        val (staleExit, staleTally) = measure(
            "a_result_file_older_than_the_marker",
            evidenceAt = marker.plusMillis(1_000),
            xmlOlderThanTheMarker = true
        )

        // 2. THE FLAG NARROWS. Evidence written during the run, with a
        //    --started-at AFTER it: the caller's later start wins, so the
        //    evidence is an earlier run's.
        val (narrowedExit, narrowedTally) = measure(
            "started_at_narrows_the_window",
            evidenceAt = marker.plusMillis(1_000),
            startedAt = System.currentTimeMillis() + 60_000
        )

        // 3. THE FLAG CANNOT WIDEN. Evidence a second before the marker, with a
        //    --started-at two hours earlier: `max(marker, flag)` keeps the
        //    marker, so it is still refused.
        val (widenedExit, widenedTally) = measure(
            "started_at_cannot_widen_the_window",
            evidenceAt = marker.minusMillis(1_000),
            startedAt = runStartMarker - 7_200_000
        )

        val rows = buildJsonObject {
            put(
                "a_result_file_older_than_the_marker",
                buildJsonObject {
                    put("claim", "the fatal stale-results check, computed with NO --started-at")
                    put("stale_result_files", staleTally.getValue("stale").jsonArray.size)
                    put("exit", staleExit)
                }
            )
            put(
                "started_at_narrows_the_window",
                buildJsonObject {
                    put("claim", "a later --started-at wins over the marker: max(marker, flag)")
                    put("accepted_as_upstream", keys(narrowedTally, "upstream").contains(key))
                    put("exit", narrowedExit)
                }
            )
            put(
                "started_at_cannot_widen_the_window",
                buildJsonObject {
                    put("claim", "an earlier --started-at cannot buy back a window the marker closed")
                    put("accepted_as_upstream", keys(widenedTally, "upstream").contains(key))
                    put("exit", widenedExit)
                }
            )
        }
        Round20Evidence.record("upstream/run-window-after-fix.json", rows)

        assertAll(
            Executable {
                assertTrue(
                    staleTally.getValue("stale").jsonArray.isNotEmpty(),
                    "a result file older than this run's start was not reported stale, and NO " +
                        "--started-at was passed - which is exactly how CI calls the gate. The check " +
                        "report() calls fatal is inert again"
                )
            },
            Executable {
                assertEquals(
                    1, staleExit,
                    "stale result files must red the gate: you are reading someone else's evidence"
                )
            },
            Executable {
                assertEquals(
                    false,
                    keys(narrowedTally, "upstream").contains(key),
                    "--started-at must be able to NARROW the window - the merge gate passes its own " +
                        "start, and evidence older than that is an earlier run's"
                )
            },
            Executable {
                assertEquals(
                    false,
                    keys(widenedTally, "upstream").contains(key),
                    "--started-at WIDENED the window: a caller passing a time two hours before the " +
                        "marker bought back the very hour round 20 exploited. The run start is " +
                        "max(marker, flag), never min"
                )
            },
            Executable { Round20Evidence.assertFrozen("upstream/run-window-after-fix.json", rows) }
        )
    }

    @Test
    fun withNoMarkerAndNoFlagTheTallyFailsClosed() {
        // A checkout with results but no `app/build/test-run/starts.tsv`: either
        // the suite did not run in this tree, or the task was up to date and
        // executed nothing. Both are fake greens, so a directory nobody can date
        // is not evidence.
        val bytes = evidenceBytes(Instant.now())
        val warnings = Files.createTempDirectory("r20-binding-nomarker-w")
        Files.write(warnings.resolve("$key.json"), bytes)
        val message = LiveEnv.UPSTREAM_WARNING_PREFIX +
            "filter_blockchains could not verify `allBlockchains(state:)` - the explorer answered " +
            "INTERNAL_ERROR [evidence sha256:${sha256(bytes)}]"
        val results = realJUnitXml(message)

        val markerless = Files.createTempDirectory("r20-binding-nomarker-repo")
        val ran = NodeOnPath.exec(
            repo.resolve("scripts/gate-tally.mjs").toFile(),
            listOf(
                "--dir", markerless.toString(),
                "--results", results.toString(),
                "--warnings", warnings.toString(),
                "--json"
            ),
            repo.toFile()
        )

        val rows = buildJsonObject {
            put("claim", "no start marker and no --started-at: the tally refuses to classify at all")
            put("exit", ran.exit)
            put("printed_a_tally", ran.stdout.trimStart().startsWith("{"))
            put("said_no_start_marker", ran.stderr.contains("no start marker"))
            put(
                "said_it_cannot_date_its_own_run",
                ran.stderr.contains("cannot date its own run")
            )
        }
        Round20Evidence.record("upstream/fails-closed-with-no-marker.json", rows)

        assertAll(
            Executable {
                assertEquals(
                    1, ran.exit,
                    "a results directory nobody can date must FAIL the gate. Exiting 0 here is the " +
                        "old default - \"ninety minutes before the oldest result file\" - by another " +
                        "name.\n${ran.transcript()}"
                )
            },
            Executable {
                assertTrue(
                    ran.stderr.contains("no start marker") &&
                        ran.stderr.contains("cannot date its own run"),
                    "the refusal must say WHY, and name the marker: an operator who reads " +
                        "\"GATE FAILED\" with no reason re-runs the build.\n${ran.transcript()}"
                )
            },
            Executable {
                assertTrue(
                    !ran.stdout.trimStart().startsWith("{"),
                    "--json must not print a tally it could not date; a JSON body is what a harness " +
                        "reads as a result"
                )
            },
            Executable {
                assertTrue(
                    File(markerless.toFile(), "app").listFiles() == null,
                    "the markerless checkout must really have no app/build/test-run in it, or this " +
                        "case is measuring something else"
                )
            },
            Executable { Round20Evidence.assertFrozen("upstream/fails-closed-with-no-marker.json", rows) }
        )
    }
}
