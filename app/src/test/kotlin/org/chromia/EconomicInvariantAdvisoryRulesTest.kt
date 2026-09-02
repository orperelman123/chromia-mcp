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

    // ---- unbounded-voting-period ----

    /**
     * The adversary DAO accepted voting_period_ms = 1: `require(p > 0)` reads
     * like validation but the window is over before anyone else can vote.
     * Advisory MEDIUM; must never block.
     */
    @Test
    fun votingPeriodBoundedOnlyByZeroIsMediumAdvisoryAndNeverBlocks() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity proposal { proposer: byte_array; deadline: timestamp; }
                    operation create_proposal(voting_period_ms: integer) {
                        val account = auth.authenticate();
                        require(voting_period_ms > 0, "voting period must be positive");
                        create proposal(proposer = account.id, deadline = op_context.last_block_time + voting_period_ms);
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "unbounded-voting-period" }
        assertTrue(hit.isNotEmpty(), "zero-only bound on a voting window must get the advisory; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
        assertTrue(result.ok, "an economic advisory must never make ok=false; got ${result.findings}")
    }

    /** A real minimum (module args or constant) is exactly the fix - clean. */
    @Test
    fun votingPeriodWithRealMinimumStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity proposal { proposer: byte_array; deadline: timestamp; }
                    operation create_proposal(voting_period_ms: integer) {
                        val account = auth.authenticate();
                        require(voting_period_ms >= chain_context.args.min_voting_period_ms, "too short");
                        create proposal(proposer = account.id, deadline = op_context.last_block_time + voting_period_ms);
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "unbounded-voting-period" !in rules(result),
            "a real minimum must clear the advisory; got ${result.findings}"
        )
    }

    /** Bounding delegated to a require()-bearing helper the param is passed to - clean. */
    @Test
    fun votingPeriodValidatedInHelperStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity proposal { proposer: byte_array; deadline: timestamp; }
                    function validate_period(period_ms: integer) {
                        require(period_ms >= 3600000, "too short");
                    }
                    operation create_proposal(voting_period_ms: integer) {
                        val account = auth.authenticate();
                        validate_period(voting_period_ms);
                        create proposal(proposer = account.id, deadline = op_context.last_block_time + voting_period_ms);
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "unbounded-voting-period" !in rules(result),
            "helper-delegated validation must clear the advisory; got ${result.findings}"
        )
    }

    /** A period param that never touches a deadline/time anchor is not a window - clean. */
    @Test
    fun periodParamWithoutTimeAnchorStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity report { key id: integer; mutable period: integer; }
                    operation set_reporting_period(period: integer) {
                        val account = auth.authenticate();
                        require(period > 0, "must be positive");
                        update report @ { .id == 0 } ( .period = period );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "unbounded-voting-period" !in rules(result),
            "no time anchor, no advisory; got ${result.findings}"
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
