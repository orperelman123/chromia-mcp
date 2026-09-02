package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the ADVISORY economic-invariant rules added after the
 * adversary round: an agent built four dApps through this MCP, the gate said
 * ok:true on all four, and two were drainable BY DESIGN (quorumless DAO,
 * unbacked oracle conversion). These shapes cannot be proven statically -
 * whether a DAO needs a quorum is design judgment, conservation is not
 * decidable from syntax - so every rule here:
 *
 *  1. reports MEDIUM, and
 *  2. NEVER makes ok=false (asserted explicitly in every exploit test) -
 *     a gate that blocks on a heuristic trains agents to route around it.
 *
 * Each rule has both directions: the real exploit shape must produce the
 * advisory, and the idiomatic correct version must stay clean.
 */
class EconomicInvariantAdvisoryRulesTest {

    private fun rules(result: RellSecurityCheck.Result): List<String> = result.findings.map { it.rule }

    // ---- majority-without-quorum ----

    /**
     * The adversary DAO drain (dapp_c_dao): execution requires only
     * yes > no, so one account proposes paying itself, votes 1-0, executes.
     * Must produce the advisory - and must NOT block, because the same shape
     * is legitimate in e.g. a two-party escrow.
     */
    @Test
    fun bareMajorityPayoutIsMediumAdvisoryAndNeverBlocks() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity treasury { key id: integer; mutable balance: integer = 0; }
                    entity proposal {
                        beneficiary: byte_array; amount: integer;
                        mutable yes_votes: integer = 0; mutable no_votes: integer = 0;
                    }
                    operation execute_proposal(proposal_id: rowid) {
                        auth.authenticate();
                        val p = proposal @ { .rowid == proposal_id };
                        require(p.yes_votes > p.no_votes, "not approved");
                        val t = treasury @ { .id == 0 };
                        update t ( .balance -= p.amount );
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "majority-without-quorum" }
        assertTrue(hit.isNotEmpty(), "bare-majority payout must get the advisory; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity, "economic advisories are MEDIUM, never blocking")
        assertTrue(result.ok, "an economic advisory must never make ok=false; got ${result.findings}")
    }

    /** A quorum floor next to the majority check is exactly the fix - clean. */
    @Test
    fun quorumGatedMajorityStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity treasury { key id: integer; mutable balance: integer = 0; }
                    entity proposal {
                        amount: integer;
                        mutable yes_votes: integer = 0; mutable no_votes: integer = 0;
                    }
                    operation execute_proposal(proposal_id: rowid) {
                        auth.authenticate();
                        val p = proposal @ { .rowid == proposal_id };
                        require(p.yes_votes + p.no_votes >= chain_context.args.quorum, "quorum not reached");
                        require(p.yes_votes > p.no_votes, "not approved");
                        val t = treasury @ { .id == 0 };
                        update t ( .balance -= p.amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "majority-without-quorum" !in rules(result),
            "a quorum-gated majority must not get the advisory; got ${result.findings}"
        )
    }

    /**
     * Stake-weighted votes are the other legitimate design. The execute
     * operation is textually IDENTICAL to the unweighted one - the weighting
     * lives in cast_vote - so the rule must scan the whole submission for
     * stake/weight evidence, not just the executing operation.
     */
    @Test
    fun stakeWeightedMajorityStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity stake { key owner: byte_array; mutable balance: integer = 0; }
                    entity treasury { key id: integer; mutable balance: integer = 0; }
                    entity proposal {
                        amount: integer;
                        mutable yes_votes: integer = 0; mutable no_votes: integer = 0;
                    }
                    operation cast_vote(proposal_id: rowid, support: boolean) {
                        val account = auth.authenticate();
                        val p = proposal @ { .rowid == proposal_id };
                        val s = stake @ { .owner == account.id };
                        if (support) update p ( .yes_votes += s.balance ); else update p ( .no_votes += s.balance );
                    }
                    operation execute_proposal(proposal_id: rowid) {
                        auth.authenticate();
                        val p = proposal @ { .rowid == proposal_id };
                        require(p.yes_votes > p.no_votes, "not approved");
                        val t = treasury @ { .id == 0 };
                        update t ( .balance -= p.amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "majority-without-quorum" !in rules(result),
            "stake/weight terms anywhere in the submission are quorum evidence; got ${result.findings}"
        )
    }

    /** Quorum checked in a helper the operation calls counts too. */
    @Test
    fun quorumDelegatedToHelperStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity treasury { key id: integer; mutable balance: integer = 0; }
                    entity proposal {
                        amount: integer;
                        mutable yes_votes: integer = 0; mutable no_votes: integer = 0;
                    }
                    function check_quorum(yes: integer, no: integer) {
                        require(yes + no >= chain_context.args.quorum, "quorum not reached");
                    }
                    operation execute_proposal(proposal_id: rowid) {
                        auth.authenticate();
                        val p = proposal @ { .rowid == proposal_id };
                        check_quorum(p.yes_votes, p.no_votes);
                        require(p.yes_votes > p.no_votes, "not approved");
                        val t = treasury @ { .id == 0 };
                        update t ( .balance -= p.amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "majority-without-quorum" !in rules(result),
            "a quorum helper must be recognized; got ${result.findings}"
        )
    }

    /** A majority comparison that moves no value (e.g. closing a poll) is not the shape. */
    @Test
    fun majorityWithoutValueMovementStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity poll { key id: integer; mutable yes_votes: integer = 0; mutable no_votes: integer = 0; mutable passed: boolean = false; }
                    operation close_poll(poll_id: rowid) {
                        auth.authenticate();
                        val p = poll @ { .rowid == poll_id };
                        if (p.yes_votes > p.no_votes) update p ( .passed = true );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "majority-without-quorum" !in rules(result),
            "no value moved, no advisory; got ${result.findings}"
        )
    }
}
