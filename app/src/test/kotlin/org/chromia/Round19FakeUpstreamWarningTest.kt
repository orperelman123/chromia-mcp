package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
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
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * ROUND 19: CAN A FAKE UPSTREAM WARNING BE PRODUCED?
 *
 * The third status exists so a ChromaWay outage does not read as our red. Its
 * whole claim, written into `scripts/gate-tally.mjs` and [LiveEnv], is that it
 * **cannot be produced by our own code failing**. Round 18 proved the two
 * negatives it thought of - a marker sentence with NO evidence file, and
 * evidence written before the run started - and both are still refused
 * ([UpstreamWarningGateTest]). This round attacks what the gate accepts.
 *
 * The gate is the REAL `scripts/gate-tally.mjs` run as a subprocess exactly as
 * the merge gate and CI run it, and the JUnit XML is written by the REAL
 * `LegacyXmlReportGeneratingListener` over a REAL failing test ([Marker]) - a
 * hand-written `<testsuite>` would be a fixture of the artifact whose parsing is
 * the thing under test. What IS written by hand is the evidence file, because
 * that is the attack: nothing in the repository asserts that only [LiveEnv] may
 * write into `app/build/upstream/warnings`, and the gate's contract is that a
 * file which is not proof must not buy the status whoever wrote it.
 *
 * The four attacks, each of which MUST be a red:
 *
 *  a1  A LEDGER ENTRY THAT DOES NOT EXIST. `warningProof` accepts any non-empty
 *      `ledgerEntry` + `ledgerHeading` when `canaryOutcome` is ANSWERED; it never
 *      opens `docs/UPSTREAM.md`. Only [LiveEnv.datedLedgerEntry] validates the
 *      number, on the producing side, and `LiveEnv` writes `evidence.ledgerEntry`
 *      raw.
 *  a2  A FILE THE PRODUCER DID NOT WRITE. A plain file with an allowlisted
 *      signature and `canaryOutcome: FAILED_OTHER` - the outcome [LiveEnv] itself
 *      documents as "may well be ours" - and a marker sentence thrown by the test
 *      rather than by `upstreamOutage`.
 *  a3  CONTROL, A FILE FOR A DIFFERENT TEST. The evidence names test B while test
 *      A is the one that failed. The gate keys on the testcase's own classname
 *      and re-checks `w.test`, so this must be a red - round 18's guarantee, held.
 *  a4  CONTROL, A STALE FILE. Evidence dated before the run. Refused by the
 *      `runStart` check - round 18's second negative, held.
 *
 * WHAT IT FOUND: a1 and a2 were accepted. `upstream=1`, exit 0, and an operator
 * reading the gate line was told ChromaWay was down.
 *
 * WHAT BECAME OF IT (2026-09-09). All four are RED and this test is green:
 *
 *  - **a1 is refused by a second, independent reader of the ledger.**
 *    `gate-tally.mjs` now OPENS `docs/UPSTREAM.md` itself (`ledgerProof`): the
 *    entry must exist, its heading must match the file's heading for that
 *    number EXACTLY, its section must carry a date, and it must name the query.
 *    `999` is not in the document, so citing it buys nothing - and the gate
 *    derives all of that from the file rather than from the evidence JSON,
 *    which is the only way a second check is worth anything.
 *  - **a2 is refused twice over.** `FAILED_OTHER` is no longer a failed canary
 *    on either side: only `FAILED_SIGNATURE` carrying the SAME allowlisted
 *    signature as the tool failure, measured on the canary's INDEPENDENT path,
 *    proves the third party is down. And the evidence is now BOUND to the run -
 *    `LiveEnv.upstreamOutage` hashes the bytes it writes and carries
 *    `[evidence sha256:<hex>]` in the assertion message, which the reporter
 *    copies into the XML from the throwable THIS process threw; the gate
 *    recomputes the digest from the file and matches it. [Marker] throws a
 *    message with no digest, as any hand-written attack must, so the file is
 *    not the one the failing test wrote whatever it contains.
 *  - **a3 and a4 are still red**, by the same two checks as before.
 *
 * The deep finding of the same section - a canary sharing `ChromiaConfig`,
 * `HttpClientService` and the endpoint with the code under test - is fixed in
 * [LiveEnv] and measured end to end by
 * `AssumptionLedgerTest.ourOwnRequestTimeoutThroughTheRealSeamStaysARed`.
 */
class Round19FakeUpstreamWarningTest {

    /**
     * A REAL test that fails with the REAL marker sentence, run through the REAL
     * launcher so the REAL reporter writes the XML the gate reads. It carries no
     * evidence of its own: every attack below supplies the evidence, which is the
     * whole question.
     */
    class Marker {
        /**
         * GREEN in the ordinary suite and RED only when this file selects it.
         * The gate reads a JUnit XML, so the failure has to come out of the real
         * reporter over a real test - and a nested class with a @Test that always
         * failed would be a permanent red in every other run. The flag is set
         * only by [noHandWrittenEvidenceBuysTheUpstreamStatus], immediately
         * around the launcher call.
         *
         * There is deliberately no early `return` here.
         * [AssumptionLedgerTest.anEarlyReturnFromATestMustFollowAnAssertion]
         * refuses one that has asserted nothing, and it is right to: a body that
         * leaves before it claims anything reports a pass for work it did not
         * do. So the inert path ASSERTS what makes it inert - the flag is off -
         * which is the only thing this method has to say when it is not being
         * used as the attack's marker.
         */
        @Test
        fun theExplorerRefusedTheQuery() {
            val armed = System.getProperty(MARKER_FLAG)
            if (armed == "on") {
                throw AssertionError(
                    LiveEnv.UPSTREAM_WARNING_PREFIX +
                        "get_blockchain_analytics: Request failed: Request timeout has expired " +
                        "[url=https://explorer.chromia.com/graphql, request_timeout=1 ms]"
                )
            }
            assertNull(
                armed,
                "$MARKER_FLAG is set outside the launcher call in " +
                    "noHandWrittenEvidenceBuysTheUpstreamStatus, which is the only place that may " +
                    "arm this marker. Left set, it turns a probe into a permanent red in every " +
                    "other run of the suite."
            )
        }
    }

    companion object {
        const val MARKER_FLAG = "org.chromia.round19.marker"
    }

    private val repo: Path get() = RepoFiles.root
    // gate-tally keys on the testcase's own classname with `$...` stripped, so a
    // nested class is keyed by its OUTER class (gate-tally.mjs, `owner.replace(/\$.*$/, '')`).
    private val key = "Round19FakeUpstreamWarningTest.theExplorerRefusedTheQuery"

    /** The signature this round uses everywhere: the one OUR OWN outbound bound produces. */
    private val signature = "explorer-request-timeout"

    private val node: String by lazy {
        val candidates = listOf("node", "node.exe", "C:\\Program Files\\nodejs\\node.exe")
        candidates.firstOrNull { candidate ->
            runCatching {
                val probe = ProcessBuilder(candidate, "--version").redirectErrorStream(true).start()
                probe.waitFor(60, TimeUnit.SECONDS) && probe.exitValue() == 0
            }.getOrDefault(false)
        } ?: throw AssertionError(
            "no working node on PATH, and the merge gate's tally runs on node - so this box cannot " +
                "verify the classification that decides its own pushes"
        )
    }

    /** The REAL JUnit XML for [Marker], written by the REAL report generator. */
    private fun realJUnitXml(): Path {
        val dir = Files.createTempDirectory("r19-upstream-xml")
        val request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectMethod(Marker::class.java, "theExplorerRefusedTheQuery"))
            .build()
        val launcher = LauncherFactory.create()
        System.setProperty(MARKER_FLAG, "on")
        try {
            launcher.execute(request, LegacyXmlReportGeneratingListener(dir, PrintWriter(StringWriter())))
        } finally {
            System.clearProperty(MARKER_FLAG)
        }
        return dir
    }

    /** `node scripts/gate-tally.mjs ... --json`, as the gate and CI run it. */
    private fun classify(results: Path, warnings: Path, startedAt: Long?): Pair<Int, JsonObject> {
        val command = mutableListOf(
            node, repo.resolve("scripts/gate-tally.mjs").toString(),
            "--results", results.toString(),
            "--warnings", warnings.toString(),
            "--json"
        )
        if (startedAt != null) command += listOf("--started-at", startedAt.toString())
        val process = ProcessBuilder(command).directory(repo.toFile()).start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        assertTrue(process.waitFor(120, TimeUnit.SECONDS), "gate-tally.mjs did not finish")
        assertTrue(
            stdout.trimStart().startsWith("{"),
            "gate-tally.mjs --json must print JSON. exit ${process.exitValue()}; stderr: $stderr"
        )
        return process.exitValue() to Json.parseToJsonElement(stdout).jsonObject
    }

    private fun keys(tally: JsonObject, field: String): List<String> =
        tally.getValue(field).jsonArray.map { it.jsonObject.getValue("key").jsonPrimitive.content }

    /** One evidence file, written the way an attacker would - not by [LiveEnv]. */
    private fun evidence(
        dir: Path,
        fileName: String,
        test: String,
        canaryOutcome: String,
        ledgerEntry: String?,
        ledgerHeading: String?,
        at: Instant
    ) {
        val body = buildJsonObject {
            put("test", test)
            put("tool", "get_blockchain_analytics")
            put("query", "blockchainAnalytics")
            put("signature", signature)
            put(
                "errorText",
                "Request failed: Request timeout has expired " +
                    "[url=https://explorer.chromia.com/graphql, request_timeout=1 ms]"
            )
            put("canaryOutcome", canaryOutcome)
            if (ledgerEntry != null) put("ledgerEntry", ledgerEntry)
            if (ledgerHeading != null) put("ledgerHeading", ledgerHeading)
            put("at", at.toString())
        }
        Files.createDirectories(dir)
        Files.writeString(dir.resolve(fileName), body.toString())
    }

    @Test
    fun noHandWrittenEvidenceBuysTheUpstreamStatus() {
        val results = realJUnitXml()
        val startedAt = System.currentTimeMillis() - 60_000
        val now = Instant.now()

        // a1: a ledger entry number that is not in docs/UPSTREAM.md, with the
        //     canary ANSWERED - the explorer is up and says so.
        val a1 = Files.createTempDirectory("r19-a1")
        evidence(a1, "$key.json", key, "ANSWERED", "999", "999. A heading no document has", now)

        // a2: a file the producer did not write, claiming the canary failed for a
        //     reason LiveEnv itself documents as possibly ours.
        val a2 = Files.createTempDirectory("r19-a2")
        evidence(a2, "$key.json", key, "FAILED_OTHER", null, null, now)

        // a3 CONTROL: the evidence names a different test.
        val a3 = Files.createTempDirectory("r19-a3")
        evidence(a3, "SomeOther.test.json", "SomeOther.test", "FAILED_SIGNATURE", null, null, now)

        // a4 CONTROL: real-shaped evidence dated before the run started.
        val a4 = Files.createTempDirectory("r19-a4")
        evidence(a4, "$key.json", key, "FAILED_SIGNATURE", null, null, now.minusSeconds(86_400))

        val attacks = listOf(
            Triple("a1_a_ledger_entry_that_does_not_exist", a1, "dangerous"),
            Triple("a2_a_file_the_producer_did_not_write", a2, "dangerous"),
            Triple("a3_control_evidence_for_a_different_test", a3, "control"),
            Triple("a4_control_stale_evidence", a4, "control")
        )
        val rows = buildJsonArray {
            attacks.forEach { (name, dir, direction) ->
                val (exit, tally) = classify(results, dir, startedAt)
                val upstream = keys(tally, "upstream")
                add(
                    buildJsonObject {
                        put("attack", name)
                        put("direction", direction)
                        put("classified_upstream", upstream.contains(key))
                        put("classified_red", keys(tally, "red").contains(key))
                        put("exit", exit)
                        put("truth", "RED - the gate's contract is that only a PROVEN upstream outage is a warning")
                    }
                )
            }
        }
        Round19Evidence.record("upstream/fake-warnings.json", rows)

        val bought = rows.map { it.jsonObject }
            .filter { it.getValue("classified_upstream").jsonPrimitive.content == "true" }
            .map { it.getValue("attack").jsonPrimitive.content }
        assertAll(
            Executable {
                assertEquals(
                    emptyList<String>(), bought,
                    "a fake UPSTREAM WARNING was produced. Every attack here is our own failure " +
                        "wearing evidence the gate does not verify: it never opens docs/UPSTREAM.md " +
                        "to check the ledger entry, and nothing in the repository asserts that only " +
                        "LiveEnv may write into app/build/upstream/warnings. The status says " +
                        "ChromaWay was down and the exit code says the run was clean.\n  " +
                        bought.joinToString("\n  ")
                )
            },
            Executable { Round19Evidence.assertFrozen("upstream/fake-warnings.json", rows) }
        )
    }
}
