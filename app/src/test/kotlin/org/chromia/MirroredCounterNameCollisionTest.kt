package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A lockstep pair must stay recognisable when both sides happen to share a
 * field name.
 *
 * `mirroredCounterFields` keyed its occurrence map on the bare field name, so
 * two distinct fields called the same thing on different entities collapsed
 * into one key - and the genuine pair became invisible to the very rule that
 * exists to see it. Found live: the lending template's `loan.scaled_debt` /
 * `pool.scaled_debt` fired `unbacked-conversion-credit` on `borrow`, and
 * renaming one side made it vanish with no other change. Renaming is a
 * workaround, not a fix; a builder who names both sides alike gets a false
 * positive on correct code, which is exactly the gate-fatigue GOAL.md warns of.
 */
class MirroredCounterNameCollisionTest {

    /** Both sides named `scaled_debt`, moving in lockstep - a liability, not a payout. */
    private val sameName = """
        module;
        import lib.ft4.auth;
        val RATE_BPS = 500;
        val YEAR_MS = 31536000000;
        entity pool { key id: text; opened_at: timestamp; mutable scaled_debt: integer = 0; mutable cash: integer = 0; }
        entity loan { key owner: byte_array; mutable scaled_debt: integer = 0; }
        operation borrow(amount: integer) {
            val account = auth.authenticate();
            require(amount > 0, "amount must be positive");
            val p = pool @ { .id == "main" };
            require(p.cash >= amount, "not enough cash");
            val l = loan @ { .owner == account.id };
            // Time-derived: this is the shape unbacked-conversion-credit looks at.
            val elapsed = op_context.last_block_time - p.opened_at;
            val interest = l.scaled_debt * RATE_BPS * elapsed / 10000 / YEAR_MS;
            update l ( .scaled_debt += amount + interest );
            update p ( .scaled_debt += amount + interest );
            update p ( .cash -= amount );
        }
        operation repay(amount: integer) {
            val account = auth.authenticate();
            require(amount > 0, "amount must be positive");
            val p = pool @ { .id == "main" };
            val l = loan @ { .owner == account.id };
            update l ( .scaled_debt -= amount );
            update p ( .scaled_debt -= amount );
            update p ( .cash += amount );
        }
    """.trimIndent()

    /** The same code with one side renamed - the shape the rule already handled. */
    private val distinctNames = sameName.replace(
        "entity pool { key id: text; opened_at: timestamp; mutable scaled_debt: integer = 0;",
        "entity pool { key id: text; opened_at: timestamp; mutable total_scaled_debt: integer = 0;"
    ).replace("update p ( .scaled_debt", "update p ( .total_scaled_debt")

    private fun findings(source: String) =
        RellSecurityCheck.analyze(mapOf("main.rell" to source)).findings

    @Test
    fun aLockstepPairSharingAFieldNameIsNotAnUnbackedCredit() {
        val hits = findings(sameName).filter { it.rule == "unbacked-conversion-credit" }
        assertTrue(
            hits.isEmpty(),
            "a liability counter mirrored in lockstep is not a payout, whatever the two sides are named; got $hits"
        )
    }

    /** The renamed form must keep working - this is the shape that already passed. */
    @Test
    fun theSamePairWithDistinctNamesStaysClean() {
        val hits = findings(distinctNames).filter { it.rule == "unbacked-conversion-credit" }
        assertTrue(hits.isEmpty(), "the distinctly-named pair was already clean and must stay clean; got $hits")
    }
}
