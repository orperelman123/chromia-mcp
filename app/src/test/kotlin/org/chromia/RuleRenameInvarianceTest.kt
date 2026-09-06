package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * A RULE WHOSE VERDICT DEPENDS ON A NAME IS CAUGHT BY THE SUITE FROM NOW ON.
 *
 * Round 14 took the four name-keyed static rules and silenced every one of
 * them by renaming ONE identifier and changing no other character:
 *
 *  - `unbounded-voting-period` fired on `amount_per_period`, a FEE, because the
 *    name matched `period|window|duration` and the `create` that stamps
 *    `started_at = op_context.last_block_time` also carries the fee.
 *    `amount_per_period` -> `fee_each_cycle`: zero findings.
 *  - `majority-without-quorum` fired on a DAO with a real participation floor,
 *    because the floor is called a turnout. `TURNOUT_BPS` -> `QUORUM_BPS`:
 *    zero findings.
 *  - `query-returns-secret-data` fired on a query returning a sale's total
 *    units, because the entity is called `private_sale`. `private_sale` ->
 *    `allocation_round`: zero findings.
 *  - `value-sink-without-withdrawal` fired on a lifetime fee STATISTIC whose
 *    every point is credited to a spendable balance in the same operation.
 *    `total_fee_amount` -> `charges_recorded`: zero findings.
 *
 * GOAL.md principle 2 is that detection must not be evadable: key on type and
 * use, NEVER on names. This test is that principle as a build failure. For each
 * of the four samples it analyses the file as shipped and then again under
 * renames in BOTH directions - the rename that used to silence the rule, and a
 * rename that hands an innocent identifier a word off the rule's own list - and
 * requires the set of findings to be IDENTICAL every time.
 *
 * Renames are whole-identifier substitutions over the source, so they change
 * nothing an analyzer may legitimately look at: no type, no control flow, no
 * data flow, not one point of value.
 */
class RuleRenameInvarianceTest {

    private fun corpusRoot(): File {
        val url = javaClass.classLoader.getResource("exploit-corpus/CORPUS.md")
            ?: error("exploit-corpus/CORPUS.md not found on the test classpath")
        return File(url.toURI()).parentFile
    }

    private fun sample(id: String): Map<String, String> {
        val dir = File(File(corpusRoot(), "samples"), id)
        val files = dir.walkTopDown().filter { it.isFile && it.extension == "rell" }
            .associate { it.relativeTo(dir).path.replace('\\', '/') to it.readText() }
        require(files.isNotEmpty()) { "$id: no .rell files under $dir" }
        return files
    }

    private fun rename(files: Map<String, String>, renames: Map<String, String>): Map<String, String> =
        files.mapValues { (_, text) ->
            renames.entries.fold(text) { acc, (from, to) ->
                acc.replace(Regex("""\b${Regex.escape(from)}\b"""), to)
            }
        }

    /** The verdict, as the corpus compares it: rule + severity, order-free. */
    private fun verdict(files: Map<String, String>): List<String> =
        RellSecurityCheck.analyze(files).findings
            .map { "${it.severity}/${it.rule}" }
            .sorted()

    private fun assertRenameInvariant(id: String, vararg renames: Pair<String, Map<String, String>>) {
        val original = sample(id)
        val base = verdict(original)
        renames.forEach { (label, map) ->
            val renamed = rename(original, map)
            map.keys.forEach { from ->
                assertTrue(
                    original.values.any { Regex("""\b${Regex.escape(from)}\b""").containsMatchIn(it) },
                    "$id/$label: '$from' does not occur in the sample - the rename tests nothing"
                )
            }
            assertEquals(
                base, verdict(renamed),
                "$id: the verdict CHANGED under the rename '$label' ($map). A rule whose verdict " +
                    "depends on an identifier is keyed on something the author - or an attacker - " +
                    "chooses, which is GOAL.md principle 2 in reverse. Key it on type and use."
            )
        }
    }

    /**
     * The four false-positive samples must be clean, and this test would pass
     * on four uniformly-wrong verdicts too - so it also pins that the baseline
     * is silence. The scoreboard pins the same thing from the other side.
     */
    private fun assertCleanBaseline(id: String) {
        val result = RellSecurityCheck.analyze(sample(id))
        assertTrue(
            result.findings.isEmpty(),
            "$id: expected no findings on correct code; got " +
                result.findings.joinToString("; ") { "${it.severity} ${it.rule} - ${it.text}" }
        )
    }

    @Test
    fun unboundedVotingPeriodIsRenameInvariant() {
        val id = "r14-fp-fee-per-period-is-not-a-time-window"
        assertCleanBaseline(id)
        assertRenameInvariant(
            id,
            // The rename round 14 used to silence the rule.
            "the fee stops matching period|window|duration" to mapOf(
                "amount_per_period" to "fee_each_cycle"
            ),
            // ...and the other direction: a money parameter given every word on
            // the list, plus the time window losing all of them.
            "money named like a window, window named like nothing" to mapOf(
                "funding" to "funding_window_duration",
                "period_ms" to "cycle_ms",
                "MIN_PERIOD_MS" to "MIN_CYCLE_MS",
                "MAX_PERIOD_MS" to "MAX_CYCLE_MS"
            )
        )
    }

    @Test
    fun majorityWithoutQuorumIsRenameInvariant() {
        val id = "r14-fp-turnout-floor-without-the-word-quorum"
        assertCleanBaseline(id)
        assertRenameInvariant(
            id,
            "the floor is spelled QUORUM" to mapOf("TURNOUT_BPS" to "QUORUM_BPS"),
            "the floor loses every hint it is one" to mapOf(
                "TURNOUT_BPS" to "BAR_BPS",
                "turnout_floor" to "bar_for",
                "floor_at_creation" to "bar_at_creation"
            )
        )
    }

    @Test
    fun querySecretExposureIsRenameInvariant() {
        val id = "r14-fp-private-sale-total-is-public"
        assertCleanBaseline(id)
        assertRenameInvariant(
            id,
            "the entity stops being called private" to mapOf("private_sale" to "allocation_round"),
            // An integer total is not a secret however it is spelled: renaming
            // it `secret_total` must not conjure a finding, and a query that
            // really returns a secret is caught on the FIELD's declared type
            // (b1-query-secret-exposure pins that direction).
            "an integer count named like a secret" to mapOf("total_units" to "secret_total")
        )
    }

    @Test
    fun valueSinkIsRenameInvariant() {
        val id = "r14-fp-fee-statistic-is-not-a-pot"
        assertCleanBaseline(id)
        assertRenameInvariant(
            id,
            "the counter stops matching the value-field list" to mapOf(
                "total_fee_amount" to "charges_recorded"
            ),
            "the counter is given both lists at once" to mapOf(
                "fee_ledger" to "fee_pot",
                "total_fee_amount" to "pot_reserve_fund"
            )
        )
    }

    // =====================================================================
    // ROUND 15. Four of the six samples below are rules that were SILENCED by
    // something the author picks - a declared type, a decoy field name, an
    // added ceiling, a literal floor - and one was a rule FIRING on a name.
    // Each fix has to be keyed on structure, so each sample gets the same
    // treatment: rename identifiers in both directions and require the verdict
    // not to move. Renames are whole-identifier substitutions, so they change
    // no type, no control flow and no data flow.
    // =====================================================================

    @Test
    fun timestampDeclaredBalanceIsRenameInvariant() {
        val id = "r15-ts-balance-declared-timestamp"
        assertRenameInvariant(
            id,
            "every identifier the mint is spelled with" to mapOf(
                "staker" to "position_row",
                "last_claim" to "since_ms",
                "elapsed" to "dt",
                "reward" to "credit_amount",
                "RATE" to "K"
            ),
            "the credited quantity is named like a deadline" to mapOf(
                "reward" to "funded_until",
                "last_claim" to "clock_at"
            )
        )
    }

    @Test
    fun timestampDecoyFieldIsRenameInvariant() {
        val id = "r15-ts-decoy-timestamp-field-elsewhere"
        assertRenameInvariant(
            id,
            "the decoy entity is renamed" to mapOf("ledger_close" to "epoch_marker"),
            "the mint's own row and locals are renamed" to mapOf(
                "staker" to "position_row",
                "elapsed" to "dt",
                "reward" to "credit_amount"
            )
        )
    }

    @Test
    fun upperBoundedVotingPeriodIsRenameInvariant() {
        val id = "r15-vp-upper-bound-only"
        assertRenameInvariant(
            id,
            "the window loses every word that reads like one" to mapOf(
                "voting_period_ms" to "w",
                "MAX_VOTING_MS" to "CAP",
                "closes_at" to "z",
                "motion" to "item"
            ),
            "the window is named like money" to mapOf("voting_period_ms" to "amount_per_period")
        )
    }

    @Test
    fun quorumFloorOfTwoIsRenameInvariant() {
        val id = "r15-mq-floor-of-two"
        assertRenameInvariant(
            id,
            "the motion and its payout are renamed" to mapOf(
                "motion" to "item",
                "payout" to "sum",
                "beneficiary" to "to_key"
            ),
            "the tallies are renamed" to mapOf("yes_votes" to "ayes", "no_votes" to "noes")
        )
    }

    @Test
    fun enrolmentCheckQueryIsRenameInvariant() {
        val id = "r15-qs-fp-enrolment-check"
        assertCleanBaseline(id)
        assertRenameInvariant(
            id,
            "the checked entity stops reading as secret" to mapOf("credential" to "enrolment_record"),
            "the returned attribute is renamed" to mapOf("display_name" to "label")
        )
    }

    @Test
    fun secretThroughAHelperIsRenameInvariant() {
        val id = "r15-qs-miss-secret-through-a-helper"
        assertRenameInvariant(
            id,
            "the helper and the query are renamed" to mapOf(
                "read_content" to "fetch",
                "note_content" to "q"
            ),
            "an integer key is named like a secret" to mapOf("id" to "secret_id")
        )
    }
}
