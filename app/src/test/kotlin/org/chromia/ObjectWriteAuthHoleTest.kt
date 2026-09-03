package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AN OBJECT FIELD WRITE IS A MUTATION, AND THE AUTH RULE COULD NOT SEE ONE.
 *
 * `unauthenticated-mutation` decided an operation mutates state with
 * MUTATION_REGEX = create|update|delete. Rell also mutates through a plain
 * assignment to an `object` field - `config.fee = f`, `pool.reserve_a += x`,
 * `dao.treasury_balance -= p.amount` - and none of those words appear. So an
 * operation whose ONLY mutation is an object write was invisible to the rule:
 * no auth check, no finding, `ok:true`.
 *
 * The default template taught exactly that shape. `hello` ships
 * `operation set_name(name) { my_name.name = name; }` with no
 * `auth.authenticate()` and no validation, and `hello` is what an agent gets
 * for any un-templated ask. GOAL.md principle 1: defaults are what agents
 * copy, so the default must be the safe one - and here the default taught the
 * pattern the gate happened to be blind to.
 *
 * Found by the round-9 prose audit, not by any test, because the corpus's own
 * `a1-unauth-mutation` sample uses entity rows and so was CAUGHT all along.
 */
class ObjectWriteAuthHoleTest {

    private fun highs(r: RellSecurityCheck.Result) =
        r.findings.filter { it.severity == "HIGH" || it.severity == "CRITICAL" }

    @Test
    fun anUnauthenticatedObjectWriteIsFlagged() {
        val src = """
            module;
            object config { mutable fee_bps: integer = 0; }
            operation set_fee(fee_bps: integer) {
                config.fee_bps = fee_bps;
            }
        """.trimIndent()
        val r = RellSecurityCheck.analyze(mapOf("src/main.rell" to src))
        assertTrue(
            highs(r).any { it.rule.startsWith("unauthenticated-mutation") },
            "an operation whose only mutation is an OBJECT field write still mutates state, and " +
                "anyone can call it - the rule saw create/update/delete only: ${r.findings}"
        )
        assertEquals(false, r.ok, "and it must block")
    }

    /** The compound forms mutate too, and are what the value templates use. */
    @Test
    fun compoundObjectWritesAreFlaggedToo() {
        val src = """
            module;
            object pool { mutable reserve: integer = 0; }
            operation drain(amount: integer) {
                pool.reserve -= amount;
            }
        """.trimIndent()
        val r = RellSecurityCheck.analyze(mapOf("src/main.rell" to src))
        assertTrue(
            highs(r).any { it.rule.startsWith("unauthenticated-mutation") },
            "`-=` on an object field is a mutation: ${r.findings}"
        )
    }

    /**
     * ...and the rule must not start firing on a READ. `==`, `>=`, `<=` and `!=`
     * all contain '=' and none of them writes anything - a rule that cannot tell
     * them apart would fire on correct code everywhere and get routed around
     * (GOAL.md principle 3).
     */
    @Test
    fun comparisonsAndReadsAreNotMutations() {
        val src = """
            module;
            object config { mutable fee_bps: integer = 0; }
            query fee_is(expected: integer) = config.fee_bps == expected;
            query fee_at_least(floor: integer) = config.fee_bps >= floor;
            query fee_not(other: integer) = config.fee_bps != other;
            query current_fee() = config.fee_bps;
        """.trimIndent()
        val r = RellSecurityCheck.analyze(mapOf("src/main.rell" to src))
        assertTrue(
            highs(r).none { it.rule.startsWith("unauthenticated-mutation") },
            "reading an object field is not mutating it: ${r.findings}"
        )
    }
}
