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
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.io.File

/**
 * ROUND 20, SECTION 2: `rell_security_check`'s CEILING, and the half of the
 * floor rule that was really doing the work.
 *
 * Round 19 gave the participation-floor rule a second question - when
 * `evalBound` cannot value the term, `evalMaxBound` asks what the BEST it can
 * be is, and the rule fires when even that is below the smallest floor a quorum
 * can be. Round 20 attacked that ceiling from both ends and measured two things
 * a reader of round 19 would not have guessed:
 *
 *  1. THE CEILING IS LOST TO ONE `)`. `whenArmValues` refuses any `when` with
 *     text after its closing brace, and it is the common parse `evalWhen` and
 *     `evalMaxBound` both read, so `min(25, when { ... })` and
 *     `abs(when { ... })` have no ceiling at all. Both floors are at most 0 and
 *     1, and both DRAINED a DAO on a real chain.
 *  2. FIVE OF THE SIX FLOORS GO SILENT ON AN AUTHENTICATED DAO. The DAO every
 *     probe is written on leaves `ballot` and `settle_motion` unauthenticated;
 *     add the round-17 non-principal signer guard an attacker satisfies by
 *     signing her own transaction and `settle_motion` becomes a trusted writer,
 *     `book.pot` leaves [RellSecurityCheck.proposerControlledFields], and the
 *     report is `ok:true` with ZERO findings. All five still drain.
 *
 * THE RECORDING IS IN FOUR KINDS OF FILE, the round-17/18/19 arrangement:
 *  - `seccheck/raw.json` and `seccheck/authed.json` are THE ADVERSARY'S OWN
 *    recordings, made against the analyzer as this round found it, through the
 *    shipped jar over stdio. Nothing writes to them and nothing here asserts
 *    against them; they are read only to check that this class is talking about
 *    the same probes, and they keep their names because `harness/seccheck_r20.py`
 *    and `harness/authed_r20.py` write exactly those two paths when re-run.
 *  - `seccheck/sources.json`, `seccheck/authed-sources.json` and
 *    `seccheck/drained-sources.json` are the probe SOURCES, written out of the
 *    harnesses' own dicts (`scripts` in the round-20 fix lane), so this
 *    in-process run measures the code that was measured over stdio and on the
 *    chain rather than a retyping of it.
 *  - `seccheck/after-fix.json`, `seccheck/authed.after-fix.json` and
 *    `seccheck/drained.after-fix.json` are the AFTER-FIX recordings, and they
 *    are what every run here is asserted against value by value
 *    ([Round20Evidence]). A verdict that drifts is a RED naming both values and
 *    both files.
 *
 * The verdicts are asserted as the round's TRUTH and not only as a diff: every
 * MUST_FLAG probe draws `majority-without-quorum`, every MUST_STAY_CLEAN one
 * stays clean of it, all six floors are caught with the DAO authenticated AND
 * without, and all eight dapps that were drained on a real chain are flagged.
 */
class Round20SecurityRuleProbeTest {

    private val rule = "majority-without-quorum"

    private fun evidence(relative: String): List<Map<String, String>> {
        val file = File(Round20Evidence.committedRoot, relative)
        require(file.isFile) { "round-20 evidence missing at ${file.absolutePath}" }
        return Json.parseToJsonElement(file.readText()).jsonArray.map { row ->
            row.jsonObject.mapValues { (_, v) ->
                runCatching { v.jsonPrimitive.content }.getOrElse { v.toString() }
            }
        }
    }

    private fun quorumFindings(source: String): List<RellSecurityCheck.Finding> =
        RellSecurityCheck.analyze(mapOf("src/main.rell" to source)).findings.filter { it.rule == rule }

    /**
     * THE TWENTY-FOUR SECCHECK PROBES: six floors that are not floors, six
     * controls one token away, and twelve CORRECT dapps whose legitimate floor
     * sits exactly at the smallest absolute floor. The twelve are the half that
     * says widening the ceiling did not buy the drains with false positives.
     */
    @Test
    fun `every round 20 floor that bounds nothing is caught and no correct dapp is`() {
        val sources = evidence("seccheck/sources.json")
        val recorded = evidence("seccheck/raw.json")
        require(sources.size == 24) { "expected the round's 24 seccheck probes, got ${sources.size}" }
        require(recorded.size == sources.size) {
            "the adversary recorded ${recorded.size} probes and the source file holds ${sources.size}"
        }
        sources.forEachIndexed { i, s ->
            require(s.getValue("probe") == recorded[i].getValue("probe")) {
                "probe $i is '${s["probe"]}' in the sources and '${recorded[i]["probe"]}' in the recording"
            }
        }

        val lines = mutableListOf<String>()
        val missed = mutableListOf<String>()
        val falsePositives = mutableListOf<String>()
        val rows = buildJsonArray {
            sources.forEachIndexed { i, s ->
                val name = s.getValue("probe")
                val truth = s.getValue("truth")
                val firedBefore = recorded[i].getValue("rule_fired").toBoolean()
                val quorum = quorumFindings(s.getValue("source"))
                val mustFlag = truth.startsWith("MUST_FLAG")
                if (mustFlag && quorum.isEmpty()) missed += name
                if (!mustFlag && quorum.isNotEmpty()) falsePositives += name
                lines += "%-66s %-9s before=%-7s after=%s".format(
                    name.take(66), if (mustFlag) "MUST_FLAG" else "CLEAN",
                    if (firedBefore) "quorum" else "silent",
                    if (quorum.isEmpty()) "silent" else "quorum"
                )
                add(
                    buildJsonObject {
                        put("probe", name)
                        put("direction", s.getValue("direction"))
                        put("truth", truth)
                        put("quorumBeforeTheFix", firedBefore)
                        put("quorum", quorum.isNotEmpty())
                        put("severities", buildJsonArray { quorum.forEach { add(it.severity) } })
                    }
                )
            }
        }
        val relative = "seccheck/after-fix.json"
        Round20Evidence.record(relative, rows)
        println("ROUND20-SECCHECK $rule\n" + lines.joinToString("\n"))
        assertAll(
            Executable {
                assertTrue(
                    missed.isEmpty(),
                    "${missed.size} floor(s) that bound nothing are still silent: $missed\n" +
                        lines.joinToString("\n")
                )
            },
            Executable {
                assertTrue(
                    falsePositives.isEmpty(),
                    "${falsePositives.size} CORRECT dapp(s) at the boundary now draw $rule: $falsePositives - " +
                        "the widened ceiling must not fire on a floor that IS a floor\n" + lines.joinToString("\n")
                )
            },
            Executable { Round20Evidence.assertFrozen(relative, rows) }
        )
    }

    /**
     * THE SAME SIX FLOORS WITH `ballot` AND `settle_motion` AUTHENTICATED. The
     * floor rule's ceiling question must answer on its own: a floor that bounds
     * nothing bounds nothing whether or not an arbitrary signer had to sign.
     */
    @Test
    fun `a floor that bounds nothing is caught whether or not the dao is authenticated`() {
        val sources = evidence("seccheck/authed-sources.json")
        val recorded = evidence("seccheck/authed.json")
        require(sources.size == 12) { "expected six floors x two dapps, got ${sources.size}" }
        require(recorded.size == sources.size) {
            "the adversary recorded ${recorded.size} rows and the source file holds ${sources.size}"
        }
        sources.forEachIndexed { i, s ->
            require(
                s.getValue("probe") == recorded[i].getValue("probe") &&
                    s.getValue("ballot_and_settle") == recorded[i].getValue("ballot_and_settle")
            ) { "row $i names ${s["probe"]}/${s["ballot_and_settle"]} in the sources and " +
                "${recorded[i]["probe"]}/${recorded[i]["ballot_and_settle"]} in the recording" }
        }

        val lines = mutableListOf<String>()
        val silent = mutableListOf<String>()
        val rows = buildJsonArray {
            sources.forEachIndexed { i, s ->
                val name = s.getValue("probe")
                val auth = s.getValue("ballot_and_settle")
                val firedBefore = recorded[i].getValue("floor_rule_fired").toBoolean()
                val quorum = quorumFindings(s.getValue("source"))
                if (quorum.isEmpty()) silent += "$name [$auth]"
                lines += "%-56s %-16s before=%-7s after=%s".format(
                    name.take(56), auth, if (firedBefore) "quorum" else "silent",
                    if (quorum.isEmpty()) "silent" else "quorum"
                )
                add(
                    buildJsonObject {
                        put("probe", name)
                        put("floor", s.getValue("floor"))
                        put("ballot_and_settle", auth)
                        put("floorRuleFiredBeforeTheFix", firedBefore)
                        put("floorRuleFired", quorum.isNotEmpty())
                    }
                )
            }
        }
        val relative = "seccheck/authed.after-fix.json"
        Round20Evidence.record(relative, rows)
        println("ROUND20-AUTHED $rule\n" + lines.joinToString("\n"))
        assertAll(
            Executable {
                assertTrue(
                    silent.isEmpty(),
                    "${silent.size} floor(s) still bound nothing and draw no $rule: $silent. The ceiling " +
                        "question decides this finding on its own; the who-writes-the-state trace is a " +
                        "second, separate reason and cannot be what carries it\n" + lines.joinToString("\n")
                )
            },
            Executable { Round20Evidence.assertFrozen(relative, rows) }
        )
    }

    /**
     * THE EIGHT DAPPS THAT WERE DRAINED ON A REAL CHAIN - three from
     * `drain_r20.py`, five from `authed_r20.py`, each with its own committed
     * `drain/<probe>.chain.json` (pot 1000000 -> 0, attacker roll 0 -> 1000000,
     * ONE signature, ONE ballot). A rule that is silent about a dapp somebody
     * emptied is the finding; every one of them must be flagged.
     */
    @Test
    fun `every dapp round 20 drained on a real chain is flagged`() {
        val sources = evidence("seccheck/drained-sources.json")
        require(sources.size == 8) { "expected the round's eight drained dapps, got ${sources.size}" }

        val lines = mutableListOf<String>()
        val silent = mutableListOf<String>()
        val rows = buildJsonArray {
            sources.forEach { s ->
                val name = s.getValue("probe")
                // The chain run beside it: a dapp is only counted here because
                // it was actually emptied, not because a table says so.
                val chain = File(Round20Evidence.committedRoot, "drain/$name.chain.json")
                require(chain.isFile) { "no chain run for $name at ${chain.absolutePath}" }
                val drained = Json.parseToJsonElement(chain.readText())
                    .jsonObject.getValue("drained").jsonPrimitive.content
                require(drained == "true") { "$name is not recorded as drained in ${chain.absolutePath}" }
                val quorum = quorumFindings(s.getValue("source"))
                if (quorum.isEmpty()) silent += name
                lines += "%-62s drained=true flagged=%s".format(name.take(62), quorum.isNotEmpty())
                add(
                    buildJsonObject {
                        put("probe", name)
                        put("ballot_and_settle", s.getValue("ballot_and_settle"))
                        put("drainedOnAChain", true)
                        put("flagged", quorum.isNotEmpty())
                        put("rules", buildJsonArray { quorum.forEach { add("${it.severity} ${it.rule}") } })
                    }
                )
            }
        }
        val relative = "seccheck/drained.after-fix.json"
        Round20Evidence.record(relative, rows)
        println("ROUND20-DRAINED $rule\n" + lines.joinToString("\n"))
        assertAll(
            Executable {
                assertTrue(
                    silent.isEmpty(),
                    "${silent.size} of the eight dapps that were DRAINED on a real chain draw no $rule: " +
                        "$silent\n" + lines.joinToString("\n")
                )
            },
            Executable { Round20Evidence.assertFrozen(relative, rows) }
        )
    }
}
