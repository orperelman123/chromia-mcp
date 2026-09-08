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
 * ROUND 19, `rell_security_check` at the constant evaluator's OWN DEFAULT.
 *
 * Round 18 stopped recognising shapes and started EVALUATING the floor term,
 * with one rule for everything it cannot value: unresolved keeps the benefit of
 * the doubt, because guessing there is how a rule starts firing on correct
 * code. Round 19 attacked that rule from both sides at once - eighteen dapps
 * through the shipped tool over stdio (`harness/seccheck_r19.py`), every one of
 * them put through `rell_check` as well so a sample that does not compile is
 * not counted as evidence:
 *
 *  - EIGHT that must be caught. Three were not: a floor that is a `when` with
 *    no common arm value (`when { book.pot > 0 -> 0; else -> -1 }`, every arm
 *    <= 0), and the same `limits(floor = 0)` read as `limits_of().floor` off a
 *    QUERY and off a FUNCTION. Two of the three were DRAINED on a real chain
 *    (`drain/*.chain.json`: pot 1000000 -> 0, attacker roll 0 -> 1000000, ONE
 *    signature, ONE ballot yes 1 no 0), and each has a CONTROL one token away
 *    that the evaluator already valued and the rule already fired on.
 *  - TEN CORRECT DAOs that must stay clean, whose legitimate floors are every
 *    shape the evaluator reads - a module arg with a default, `max()`, a
 *    constant index, equal `when` arms, a defaulted parameter, integer
 *    arithmetic, a text length, a struct constant's field, two calls deep - so
 *    that closing the three gaps is measured against firing on correct code and
 *    not only against the drains.
 *  - and `1 / ZERO`, which stays CLEAN and is a pin rather than a finding: Rell
 *    has no value for it either, the operation throws on every call, no value
 *    moves, and there is nothing to drain.
 *
 * This class is the RECORDER, and it is round 18's arrangement
 * ([Round18SecurityRuleProbeTest]) applied to round 19's tree:
 *
 *  - `seccheck/raw.before-fix.json` is THE ADVERSARY'S OWN recording, made
 *    against the analyzer as the round found it, through the jar over stdio.
 *    Nothing is asserted against it and nothing writes to it; it is the only
 *    record of what was wrong.
 *  - `seccheck/sources.json` is the eighteen probe SOURCES, written out of the
 *    harness's own `PROBES` dict so the code re-run here cannot drift from the
 *    code that was measured. The two files are asserted to name the same
 *    eighteen probes in the same order before any verdict is read.
 *  - `seccheck/raw.json` is the AFTER-FIX recording, re-recorded once when the
 *    three gaps were closed, and it is what this run is asserted against, value
 *    by value ([Round19Evidence], the b640e6c pattern). A verdict that drifts is
 *    a RED naming both values and both files.
 */
class Round19SecurityRuleProbeTest {

    private data class Probe(
        val name: String,
        val direction: String,
        val truth: String,
        val why: String,
        val source: String,
        val ruleFiredBefore: Boolean
    )

    private val rule = "majority-without-quorum"

    /** The eighteen round-19 seccheck probes: verdicts from the adversary's recording, sources from the harness. */
    private fun probes(): List<Probe> {
        val recording = File(Round19Evidence.committedRoot, "seccheck/raw.before-fix.json")
        require(recording.isFile) {
            "the adversary's own round-19 seccheck recording is missing at ${recording.absolutePath} - it is " +
                "the only record of what the analyzer said before the fix"
        }
        val sourcesFile = File(Round19Evidence.committedRoot, "seccheck/sources.json")
        require(sourcesFile.isFile) {
            "the round-19 probe sources are missing at ${sourcesFile.absolutePath} - they are written out of " +
                "harness/seccheck_r19.py's own PROBES dict so this run measures the code that was measured"
        }
        val rows = Json.parseToJsonElement(recording.readText()).jsonArray
        val sources = Json.parseToJsonElement(sourcesFile.readText()).jsonArray
        require(rows.size == 18) { "expected the round's eighteen seccheck probes, got ${rows.size}" }
        require(sources.size == rows.size) {
            "the recording holds ${rows.size} probes and the source file ${sources.size}"
        }
        return rows.mapIndexed { i, row ->
            val o = row.jsonObject
            val s = sources[i].jsonObject
            val name = o.getValue("probe").jsonPrimitive.content
            require(s.getValue("probe").jsonPrimitive.content == name) {
                "probe $i is '$name' in the recording and " +
                    "'${s.getValue("probe").jsonPrimitive.content}' in the source file"
            }
            Probe(
                name = name,
                direction = o.getValue("direction").jsonPrimitive.content,
                truth = o.getValue("truth").jsonPrimitive.content,
                why = o.getValue("why").jsonPrimitive.content,
                source = s.getValue("source").jsonPrimitive.content,
                ruleFiredBefore = o.getValue("rule_fired").jsonPrimitive.content.toBoolean()
            )
        }
    }

    @Test
    fun `record what rell_security_check says about the round 19 rule probes`() {
        val probes = probes()
        val lines = mutableListOf<String>()
        var flagged = 0
        var mustFlag = 0
        var quietBefore = 0
        val rows = buildJsonArray {
            probes.forEach { probe ->
                val result = RellSecurityCheck.analyze(mapOf("main.rell" to probe.source))
                val quorum = result.findings.filter { it.rule == rule }
                val findings = result.findings.map { "${it.severity} ${it.rule} (line ${it.line})" }
                val wantsQuorum = probe.truth.startsWith("MUST_FLAG $rule")
                if (wantsQuorum) {
                    mustFlag++
                    if (quorum.isNotEmpty()) flagged++
                    if (!probe.ruleFiredBefore) quietBefore++
                }
                lines += "%-72s before=%-7s after=%-7s %s".format(
                    probe.name.take(72),
                    if (probe.ruleFiredBefore) "quorum" else "silent",
                    if (quorum.isNotEmpty()) "quorum" else "silent",
                    if (findings.isEmpty()) "no findings" else findings.joinToString("; ")
                )
                add(
                    buildJsonObject {
                        put("probe", probe.name)
                        put("direction", probe.direction)
                        put("truth", probe.truth)
                        put("why", probe.why)
                        put("ok", result.ok)
                        put("quorumBeforeTheFix", probe.ruleFiredBefore)
                        put("quorum", buildJsonArray { quorum.forEach { add(it.rule) } })
                        put("findings", buildJsonArray { findings.forEach { add(it) } })
                    }
                )
            }
        }
        val relative = "seccheck/raw.json"
        Round19Evidence.record(relative, rows)
        println(
            "ROUND19-SECCHECK $rule: $flagged/$mustFlag of the MUST_FLAG probes " +
                "(before the fix: ${mustFlag - quietBefore}/$mustFlag)\n" + lines.joinToString("\n")
        )
        Round19Evidence.assertFrozen(relative, rows)

        // The scoreboard the round is judged on, computed from the run rather
        // than from a reading of the rule: every probe whose truth is
        // MUST_FLAG majority-without-quorum draws it, and no MUST_STAY_CLEAN
        // probe does - the ten correct DAOs included, which is the half of the
        // round that says the fix did not buy the drains with false positives.
        assertTrue(
            flagged == mustFlag,
            "$flagged of $mustFlag MUST_FLAG $rule probes are caught; the round-19 fix claims all of " +
                "them:\n" + lines.joinToString("\n")
        )
        probes.filter { it.truth.startsWith("MUST_STAY_CLEAN") }.forEach { probe ->
            val quorum = RellSecurityCheck.analyze(mapOf("main.rell" to probe.source))
                .findings.filter { it.rule == rule }
            assertTrue(
                quorum.isEmpty(),
                "${probe.name} is correct code for this rule (${probe.truth}) and drew ${quorum.size} " +
                    "$rule finding(s) - ${probe.why}. A floor at or above the smallest absolute floor is a " +
                    "floor, and firing here is firing on correct code"
            )
        }
    }
}
