package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * ROUND 18, `rell_security_check` at the two joins round 17 rebuilt: a bound's
 * VALUE resolved through a call, and a stored floor rejected when the operation
 * that writes it is authenticated to nobody in particular.
 *
 * The round drove nine probes through the shipped jar over stdio
 * (`harness/seccheck_r18.py`) and found SIX spellings of the same 1 that the
 * round-17 shape does not reach - a default-valued parameter, `2 - 1`,
 * `min(1, 5)`, a namespaced val, the stored floor read through a function, and
 * the same field read back through a query - beside a control that is caught, a
 * module-args-admin writer that must stay quiet, and the deployment-floor DAO
 * that answers round 17's open question.
 *
 * This class is the RECORDER, and it is the round-17 arrangement
 * ([Round17SecurityRuleProbeTest]) applied to round 18's tree, so the claims
 * about the rules are measurements rather than readings of the source:
 *
 *  - `seccheck/raw.before-fix.json` is THE ADVERSARY'S OWN recording, made
 *    against the analyzer as the round found it, through the jar over stdio.
 *    Nothing is asserted against it and nothing writes to it; it is the only
 *    record of what was wrong, and this run reads the PROBE SOURCES out of it
 *    so the sources cannot drift from the ones that were measured.
 *  - `seccheck/raw.json` is the AFTER-FIX recording, re-recorded once when the
 *    six evasions were closed, and it is what this run is asserted against,
 *    value by value ([Round18Evidence], the b640e6c pattern). A verdict that
 *    drifts is a RED naming both values and both files.
 *
 * The two files do not have the same shape and are not meant to: the adversary's
 * harness read a `message` key the tool does not emit, so its every message is
 * empty. The after-fix recording keeps `probe`, `truth`, `why` and `source`
 * from it and records the findings the way round 17 recorded them.
 */
class Round18SecurityRuleProbeTest {

    private data class Probe(val name: String, val truth: String, val why: String, val source: String, val wasQuorum: Boolean)

    /** The nine round-18 seccheck probes, read out of the adversary's own frozen recording. */
    private fun probes(): List<Probe> {
        val file = File(Round18Evidence.committedRoot, "seccheck/raw.before-fix.json")
        require(file.isFile) {
            "the adversary's own round-18 seccheck recording is missing at ${file.absolutePath} - it is " +
                "the only record of what the analyzer said before the fix and the source of these probes"
        }
        val rows = Json.parseToJsonElement(file.readText()).jsonArray
        require(rows.size == 9) { "expected the round's nine seccheck probes, got ${rows.size}" }
        return rows.map { row ->
            val o = row.jsonObject
            Probe(
                name = o.getValue("probe").jsonPrimitive.content,
                truth = o.getValue("truth").jsonPrimitive.content,
                why = o.getValue("why").jsonPrimitive.content,
                source = o.getValue("source").jsonPrimitive.content,
                wasQuorum = o.getValue("quorum").jsonArray.isNotEmpty()
            )
        }
    }

    @Test
    fun `record what rell_security_check says about the round 18 rule probes`() {
        val probes = probes()
        val lines = mutableListOf<String>()
        var flagged = 0
        var mustFlag = 0
        var quietBefore = 0
        val rows = buildJsonArray {
            probes.forEach { probe ->
                val result = RellSecurityCheck.analyze(mapOf("main.rell" to probe.source))
                val quorum = result.findings.filter { it.rule == "majority-without-quorum" }
                val findings = result.findings.map { "${it.severity} ${it.rule} (line ${it.line})" }
                val wantsQuorum = probe.truth.startsWith("MUST_FLAG majority-without-quorum")
                if (wantsQuorum) {
                    mustFlag++
                    if (quorum.isNotEmpty()) flagged++
                    if (!probe.wasQuorum) quietBefore++
                }
                lines += "%-70s before=%-7s after=%-7s %s".format(
                    probe.name.take(70),
                    if (probe.wasQuorum) "quorum" else "silent",
                    if (quorum.isNotEmpty()) "quorum" else "silent",
                    if (findings.isEmpty()) "no findings" else findings.joinToString("; ")
                )
                add(
                    buildJsonObject {
                        put("probe", probe.name)
                        put("truth", probe.truth)
                        put("why", probe.why)
                        put("ok", result.ok)
                        put("quorumBeforeTheFix", probe.wasQuorum)
                        put("quorum", buildJsonArray { quorum.forEach { add(it.rule) } })
                        put("findings", buildJsonArray { findings.forEach { add(it) } })
                        put("source", probe.source)
                    }
                )
            }
        }
        val relative = "seccheck/raw.json"
        Round18Evidence.record(relative, rows)
        println(
            "ROUND18-SECCHECK majority-without-quorum: $flagged/$mustFlag of the MUST_FLAG probes " +
                "(before the fix: ${mustFlag - quietBefore}/$mustFlag)\n" + lines.joinToString("\n")
        )
        Round18Evidence.assertFrozen(relative, rows)

        // The scoreboard the round is judged on, computed from the file rather
        // than from a reading of the rule: every probe whose truth is
        // MUST_FLAG majority-without-quorum draws it, and neither
        // MUST_STAY_CLEAN probe does.
        assertTrue(
            flagged == mustFlag,
            "$flagged of $mustFlag MUST_FLAG majority-without-quorum probes are caught; the round-18 " +
                "fix claims all of them:\n" + lines.joinToString("\n")
        )
        probes.filter { it.truth.startsWith("MUST_STAY_CLEAN") }.forEach { probe ->
            val quorum = RellSecurityCheck.analyze(mapOf("main.rell" to probe.source))
                .findings.filter { it.rule == "majority-without-quorum" }
            assertTrue(
                quorum.isEmpty(),
                "${probe.name} is correct code for this rule (${probe.truth}) and drew " +
                    "${quorum.size} majority-without-quorum finding(s) - a floor the proposer cannot " +
                    "move is a floor, and firing here is firing on correct code"
            )
        }
    }
}
