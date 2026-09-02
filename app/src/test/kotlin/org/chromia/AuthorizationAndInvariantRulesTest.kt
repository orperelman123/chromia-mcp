package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the authorization-correctness and economic-invariant
 * rules added after two independent audits agreed on the same blind spot:
 * "an auth token appears somewhere + a require() exists" was being certified
 * as secure, while a DAO that let one zero-contribution account drain the
 * treasury and an oracle that minted unbacked value both came back ok:true.
 *
 * Every rule here has BOTH directions: the exploit sample must produce the
 * finding, and idiomatic secure FT4 code must stay clean - a noisy gate
 * trains agents to ignore the gate, which is worse than no gate.
 */
class AuthorizationAndInvariantRulesTest {

    private fun rules(result: RellSecurityCheck.Result): List<String> = result.findings.map { it.rule }

    // ---- authorization-not-bound-to-caller (confused deputy) ----

    /**
     * The adversary's drain: the operation authenticates (so the old
     * unauthenticated-mutation rule is silent) but then debits rows selected
     * by a caller-supplied account parameter that is never related to the
     * authenticated identity. Anyone drains anyone.
     */
    @Test
    fun authenticatedDrainByCallerSuppliedAccountIsHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation withdraw(from_account: byte_array, amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update wallet @ { .owner == from_account } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "authorization-not-bound-to-caller" }
        assertTrue(hit.isNotEmpty(), "confused-deputy drain must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertEquals(false, result.ok)
    }

    /** Deleting rows keyed by a caller-supplied owner is the same class. */
    @Test
    fun authenticatedDeleteByCallerSuppliedOwnerIsHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity listing { key id: integer; owner: byte_array; }
                    operation cancel_listing(owner: byte_array) {
                        val account = auth.authenticate();
                        delete listing @* { .owner == owner };
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "authorization-not-bound-to-caller" in rules(result),
            "delete keyed by unbound caller param must be flagged; got ${result.findings}"
        )
    }

    /** Self-keyed mutation (`.owner == account.id`) is THE idiomatic pattern - must stay clean. */
    @Test
    fun selfKeyedMutationStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation withdraw(amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update wallet @ { .owner == account.id } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(result.ok, result.findings.toString())
    }

    /** Param explicitly bound to the authenticated identity is authorized - clean. */
    @Test
    fun paramBoundToAuthenticatedIdStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation close_wallet(owner: byte_array) {
                        val account = auth.authenticate();
                        require(owner == account.id, "not your wallet");
                        delete wallet @ { .owner == owner };
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            result.findings.toString()
        )
    }

    /** Admin ops keyed to chain_context.args are the sanctioned break-glass pattern - clean. */
    @Test
    fun adminOpKeyedToChainContextArgsStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation admin_seize(owner: byte_array) {
                        val account = auth.authenticate();
                        require(account.id == chain_context.args.admin_account, "admin only");
                        delete wallet @ { .owner == owner };
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            result.findings.toString()
        )
    }

    /**
     * A transfer that debits self and credits a caller-named recipient is the
     * most idiomatic value-moving shape there is; the credit (`+=` only) to
     * the unbound param must not read as a drain.
     */
    @Test
    fun creditOnlyMutationOfCallerSuppliedRecipientStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation tip(to_user: byte_array, amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update wallet @ { .owner == account.id } ( .balance -= amount );
                        update wallet @ { .owner == to_user } ( .balance += amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            result.findings.toString()
        )
    }

    /** is_signer(<that param>) binds the param to an actual transaction signer - clean. */
    @Test
    fun paramCheckedWithIsSignerStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation withdraw(owner: byte_array, amount: integer) {
                        require(op_context.is_signer(owner), "not signer");
                        require(amount > 0, "positive");
                        update wallet @ { .owner == owner } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            result.findings.toString()
        )
    }
}
