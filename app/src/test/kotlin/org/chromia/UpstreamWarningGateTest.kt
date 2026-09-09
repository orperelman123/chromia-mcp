package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.reporting.legacy.xml.LegacyXmlReportGeneratingListener
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * THE GATE'S CLASSIFICATION, PROVED ON THE REAL ARTIFACTS.
 *
 * `scripts/gate-tally.mjs` is the one implementation of the tally - the merge
 * gate imports it and CI runs it as a step - and it is what turns a JUnit
 * failure into `upstream=N` instead of a red. Getting that wrong in either
 * direction is a fake green: too generous and any test can excuse itself by
 * writing a sentence; too strict and a proven ChromaWay outage blocks a push
 * that has nothing wrong with it.
 *
 * So it is asserted on real inputs, produced here:
 *
 *  - **the XML is real.** One live test is run through the REAL JUnit launcher
 *    and the REAL `LegacyXmlReportGeneratingListener` writes the report. A
 *    hand-written `<testsuite>` string would be a fixture of the exact artifact
 *    whose parsing is under test, and it would agree with whatever this file
 *    believed the reporter emits.
 *  - **the evidence is real.** The warning file comes from
 *    [LiveEnv.upstreamOutage] measuring the live explorer during that run - the
 *    canary, the signature and the ledger lookup all happened.
 *  - **the negatives are the same real corpus with the proof taken away**, not
 *    a different, invented one: the classifier must call the identical failure a
 *    RED when the evidence is missing, and when the evidence predates the run.
 */
class UpstreamWarningGateTest {

    /**
     * The live claim this drives: `filter_blockchains{state}`, an advertised
     * argument the explorer has refused with INTERNAL_ERROR since 2026-09-07
     * (docs/UPSTREAM.md #3b). While that entry stands the test is an upstream
     * warning; when ChromaWay fixes the field it goes green and this class
     * asserts the green path instead - see [theTallyAgreesWithWhatTheRunActuallyDid].
     */
    private val liveClass = ToolExecutorStrategiesTest::class.java
    private val liveMethod = "liveFilterBlockchainsFiltersByChainState"

    private val repo: Path get() = RepoFiles.root

    /**
     * Runs [liveMethod] through the real launcher into a fresh report directory
     * and returns it. This is a REAL run of a REAL live test - it calls the
     * explorer - so its verdict is whatever the third party is doing right now.
     */
    /**
     * Where the nested run's evidence goes. The outer gradle run of [liveClass]
     * has already written its own file under `app/build/upstream/warnings`, and
     * its JUnit XML binds THAT file's digest; a nested run writing the same name
     * there would replace it with a file of a different timestamp and turn the
     * real upstream warning into a red of ours. So the nested run gets its own
     * directory through [LiveEnv.UPSTREAM_DIR_PROPERTY], and everything this
     * test reads comes from here.
     */
    private val nestedUpstreamDir: Path by lazy { Files.createTempDirectory("upstream-gate-evidence") }

    private fun realJUnitXmlForOneLiveTest(): Path {
        val reports = Files.createTempDirectory("upstream-gate-xml")
        val previous = System.getProperty(LiveEnv.UPSTREAM_DIR_PROPERTY)
        System.setProperty(LiveEnv.UPSTREAM_DIR_PROPERTY, nestedUpstreamDir.toString())
        try {
            return launchNested(reports)
        } finally {
            if (previous == null) System.clearProperty(LiveEnv.UPSTREAM_DIR_PROPERTY)
            else System.setProperty(LiveEnv.UPSTREAM_DIR_PROPERTY, previous)
        }
    }

    private fun launchNested(reports: Path): Path {
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectMethod(liveClass, liveMethod))
            .build()
        // The summary listener is here to DIAGNOSE, not to decorate: "the
        // reporter wrote nothing" has two very different causes - the selector
        // matched nothing, or it ran and the report went somewhere else - and a
        // message that cannot tell them apart costs a build cycle to resolve.
        val summary = SummaryGeneratingListener()
        LauncherFactory.create().execute(
            request,
            LegacyXmlReportGeneratingListener(reports, PrintWriter(StringWriter())),
            summary
        )
        assertEquals(
            1L, summary.summary.testsStartedCount,
            "the nested launcher started ${summary.summary.testsStartedCount} test(s) for " +
                "${liveClass.simpleName}.$liveMethod (found ${summary.summary.testsFoundCount}). " +
                "Without a real run there is no real XML, and the classifier would end up asserted " +
                "against a fixture of the very artifact whose parsing is under test."
        )
        // Found by listing, not by guessing the name: the legacy reporter's file
        // naming is JUnit's business, and a wrong guess here would report a
        // missing run for a run that happened.
        val written = Files.newDirectoryStream(reports).use { it.toList() }
        assertTrue(
            written.any { it.fileName.toString().endsWith(".xml") },
            "the real JUnit reporter ran the test but wrote no .xml to $reports (found $written)"
        )
        return reports
    }

    /**
     * A node that actually RUNS, found by trying every one on PATH until one
     * answers `--version`.
     *
     * Not paranoia: a bare `ProcessBuilder("node")` in the Gradle test JVM
     * started something that printed nothing on either stream and exited
     * without an exception, so the test reported "the tally printed no JSON"
     * for a tally that was never executed. Windows keeps an App-Execution-Alias
     * stub called `node.exe` in WindowsApps for machines with no Node
     * installed, and it behaves exactly like that. Proving the interpreter
     * before blaming the script is the difference between a diagnosis and a
     * guess.
     */
    private val node: String by lazy {
        val tried = mutableListOf<String>()
        val candidates = System.getenv("PATH").orEmpty().split(java.io.File.pathSeparator)
            .flatMap { dir -> listOf("node.exe", "node").map { java.io.File(dir, it) } }
            .filter { it.isFile }
            .map { it.absolutePath }
            .distinct()
        for (candidate in candidates) {
            val probe = ProcessBuilder(candidate, "--version").redirectErrorStream(true).start()
            val said = probe.inputStream.bufferedReader().readText().trim()
            probe.waitFor(60, TimeUnit.SECONDS)
            if (probe.exitValue() == 0 && said.startsWith("v")) return@lazy candidate
            tried += "$candidate -> exit ${probe.exitValue()}, said ${said.ifBlank { "<nothing>" }}"
        }
        throw AssertionError(
            "no working node on PATH, and the merge gate's tally runs on node - so this box cannot " +
                "verify the classification that decides its own pushes. Tried " +
                "${candidates.size} candidate(s):\n  " + tried.joinToString("\n  ")
        )
    }

    /** `node scripts/gate-tally.mjs ... --json`, as the gate and CI run it. */
    private fun classify(results: Path, warnings: Path, startedAt: Long?): Pair<Int, JsonObject> {
        val command = mutableListOf(
            node, repo.resolve("scripts/gate-tally.mjs").toString(),
            "--results", results.toString(),
            "--warnings", warnings.toString(),
            "--json"
        )
        if (startedAt != null) { command += listOf("--started-at", startedAt.toString()) }
        val process = ProcessBuilder(command)
            .directory(repo.toFile())
            .redirectErrorStream(false)
            .start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        assertTrue(
            process.waitFor(120, TimeUnit.SECONDS),
            "scripts/gate-tally.mjs did not finish - the gate cannot hang, it is the thing that " +
                "decides whether a push happens"
        )
        assertTrue(
            stdout.trimStart().startsWith("{"),
            "gate-tally.mjs --json must print the tally as JSON.\n  command: ${command.joinToString(" ")}\n" +
                "  cwd: $repo\n  exit: ${process.exitValue()}\n  stdout: ${stdout.ifBlank { "<nothing>" }}\n" +
                "  stderr: ${stderr.ifBlank { "<nothing>" }}"
        )
        return process.exitValue() to Json.parseToJsonElement(stdout).jsonObject
    }

    private fun keys(tally: JsonObject, field: String): List<String> =
        tally.getValue(field).jsonArray.map { it.jsonObject.getValue("key").jsonPrimitive.content }

    @Test
    fun theTallyAgreesWithWhatTheRunActuallyDid() {
        LiveChromia.requireLive("runs one real live explorer test and classifies its real JUnit XML")
        val startedAt = System.currentTimeMillis() - 5_000
        val results = realJUnitXmlForOneLiveTest()
        val warnings = nestedUpstreamDir.resolve("warnings")
        val key = "${liveClass.simpleName}.$liveMethod"

        val (exit, tally) = classify(results, warnings, startedAt)
        assertEquals(1, tally.getValue("tests").jsonPrimitive.content.toInt(), "one test was selected")
        assertEquals(0, tally.getValue("skipped").jsonPrimitive.content.toInt(), "a live test never skips")

        val upstream = keys(tally, "upstream")
        val red = keys(tally, "red")

        if (upstream.contains(key)) {
            // The third status, on today's explorer. Every clause of the
            // contract is asserted, because each one is a way this could be
            // generous by accident.
            assertEquals(emptyList<String>(), red, "a proven upstream warning must not also be red")
            assertEquals(0, exit, "an upstream warning does not set the exit code - it is not OUR failure")
            assertEquals(
                1, tally.getValue("failures").jsonPrimitive.content.toInt(),
                "the JUnit XML must still record a FAILURE. Nothing pretends to pass: the claim in " +
                    "${liveClass.simpleName}.$liveMethod was not verified by this run."
            )
            val evidence = tally.getValue("upstream").jsonArray.single().jsonObject
            assertEquals("filter_blockchains", evidence.getValue("tool").jsonPrimitive.content)
            assertEquals("allBlockchains(state:)", evidence.getValue("query").jsonPrimitive.content)
            assertEquals(
                "explorer-graphql-internal-error",
                evidence.getValue("signature").jsonPrimitive.content,
                "the signature must be the allowlisted one the explorer actually produced"
            )
            assertTrue(
                Files.exists(warnings.resolve("$key.json")),
                "the warning must be backed by a file at ${warnings.resolve("$key.json")}"
            )

            // NEGATIVE 1: the SAME real XML, with no evidence beside it. A
            // sentence is not proof; the classifier must call it a red.
            val empty = Files.createTempDirectory("upstream-gate-no-evidence")
            val (unprovenExit, unproven) = classify(results, empty, startedAt)
            assertEquals(
                emptyList<String>(), keys(unproven, "upstream"),
                "an UPSTREAM WARNING message with NO evidence file was counted as a warning. Any " +
                    "test could then excuse itself by writing that sentence into an assertion."
            )
            assertEquals(listOf(key), keys(unproven, "red"), "with no evidence it is an ordinary red")
            assertEquals(1, unprovenExit, "an unproven claim must red the gate")

            // NEGATIVE 2: the same real XML and the same real evidence, but a
            // run that started AFTER the evidence was written. Yesterday's
            // outage cannot excuse today's failure.
            val (staleExit, stale) = classify(results, warnings, System.currentTimeMillis() + 60_000)
            assertEquals(
                emptyList<String>(), keys(stale, "upstream"),
                "evidence written before the run started was accepted - a warning file left in " +
                    "app/build from an outage last week would excuse an identical failure today"
            )
            assertEquals(listOf(key), keys(stale, "red"))
            assertEquals(1, staleExit)
        } else {
            // The explorer serves `state` again. Then there is nothing to warn
            // about, the test passed, and docs/UPSTREAM.md #3b is stale.
            assertEquals(emptyList<String>(), red, "the live test failed for a reason that is OURS")
            assertEquals(0, exit)
            assertEquals(
                0, tally.getValue("failures").jsonPrimitive.content.toInt(),
                "the explorer now serves allBlockchains(state:), so the live test passes - and " +
                    "docs/UPSTREAM.md #3b, the note in filter_blockchains's schema and the ledger " +
                    "row in ToolExecutorStrategiesTest.upstreamLedgerEntries are now STALE and must " +
                    "come out. A closed outage that keeps its excuse is the next fake green."
            )
        }
    }

    /**
     * The gate line the operator reads. It has to carry the third status and the
     * NAMES, because a bare `failures=1` on a run whose only failure was
     * ChromaWay's is the sentence that sends someone hunting through their own
     * diff for an hour.
     */
    @Test
    fun theGateLineNamesTheThirdStatus() {
        val gate = RepoFiles.text("scripts/loop-gate.mjs")
        assertTrue(
            gate.contains("from './gate-tally.mjs'"),
            "scripts/loop-gate.mjs must import the ONE tally implementation. Two gates with " +
                "different definitions of green is one gate and one bypass."
        )
        val tallyScript = RepoFiles.text("scripts/gate-tally.mjs")
        assertTrue(
            tallyScript.contains("upstream=\${t.upstream.length}"),
            "the gate line must carry upstream=N"
        )
        assertTrue(
            tallyScript.contains(LiveEnv.UPSTREAM_WARNING_PREFIX),
            "gate-tally.mjs and LiveEnv must agree on the marker EXACTLY - a drifted prefix silently " +
                "turns every warning back into a red, and nothing would say so"
        )
        for (signature in LiveEnv.UPSTREAM_SIGNATURES.map { it.first }) {
            assertTrue(
                tallyScript.contains(signature),
                "the gate refuses evidence naming a signature it does not know, so `$signature` " +
                    "must be listed in scripts/gate-tally.mjs too"
            )
        }
        val ci = RepoFiles.text(".github/workflows/ci.yml")
        assertTrue(
            ci.contains("scripts/gate-tally.mjs"),
            "CI must apply the SAME classification with the SAME script - the local gate being " +
                "kinder than the merge gate is the bypass this repo removed once already"
        )
        assertTrue(
            ci.contains("ALLOWED = set()"),
            "CI's skip check keeps an EMPTY allowlist: the third status is for a proven THIRD-PARTY " +
                "outage, never a way to stop counting a skip"
        )
    }
}
