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

    // ---- unbacked-conversion-credit ----

    /**
     * The adversary oracle mint (dapp_d_oracle): sell_tokens credits USD
     * computed at the posted price, debiting only the token side - the USD
     * comes from nowhere. 100 -> 200,000,000 in the running exploit.
     */
    @Test
    fun oraclePricedCreditWithoutReserveIsMediumAdvisoryAndNeverBlocks() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    val PRICE_SCALE = 1000000;
                    entity price_feed { key name: text; mutable price: integer; }
                    entity usd_account { key owner: byte_array; mutable balance: integer = 0; }
                    entity token_account { key owner: byte_array; mutable balance: integer = 0; }
                    operation sell_tokens(token_amount: integer) {
                        val account = auth.authenticate();
                        require(token_amount > 0, "positive");
                        val tok = token_account @ { .owner == account.id };
                        require(tok.balance >= token_amount, "insufficient tokens");
                        val price = price_feed @ { .name == "TOKEN_USD" } ( .price );
                        update tok ( .balance -= token_amount );
                        update usd_account @ { .owner == account.id } ( .balance += token_amount * price / PRICE_SCALE );
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "unbacked-conversion-credit" }
        assertTrue(hit.isNotEmpty(), "price-derived credit with no same-asset debit must get the advisory; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
        assertTrue(result.ok, "an economic advisory must never make ok=false; got ${result.findings}")
    }

    /** Price read through a helper (the full adversary dApp's get_price()) still counts. */
    @Test
    fun priceReadViaHelperStillGetsAdvisory() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity price_feed { key name: text; mutable price: integer; }
                    entity usd_account { key owner: byte_array; mutable balance: integer = 0; }
                    entity token_account { key owner: byte_array; mutable balance: integer = 0; }
                    function get_price(): integer {
                        val f = price_feed @? { .name == "TOKEN_USD" };
                        require(f != null, "no feed");
                        return f.price;
                    }
                    operation sell_tokens(token_amount: integer) {
                        val account = auth.authenticate();
                        require(token_amount > 0, "positive");
                        val tok = token_account @ { .owner == account.id };
                        require(tok.balance >= token_amount, "insufficient tokens");
                        update tok ( .balance -= token_amount );
                        update usd_account @ { .owner == account.id } ( .balance += token_amount * get_price() / 1000000 );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "unbacked-conversion-credit" in rules(result),
            "helper-read price must still count; got ${result.findings}"
        )
    }

    /** Paying the credit out of a same-asset reserve row IS conservation - clean. */
    @Test
    fun reserveBackedConversionStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    val PRICE_SCALE = 1000000;
                    entity price_feed { key name: text; mutable price: integer; }
                    entity usd_account { key owner: byte_array; mutable balance: integer = 0; }
                    entity token_account { key owner: byte_array; mutable balance: integer = 0; }
                    operation sell_tokens(token_amount: integer) {
                        val account = auth.authenticate();
                        require(token_amount > 0, "positive");
                        val tok = token_account @ { .owner == account.id };
                        require(tok.balance >= token_amount, "insufficient tokens");
                        val price = price_feed @ { .name == "TOKEN_USD" } ( .price );
                        val usd_out = token_amount * price / PRICE_SCALE;
                        val reserve = usd_account @ { .owner == chain_context.args.reserve_owner };
                        require(reserve.balance >= usd_out, "reserve cannot cover");
                        update reserve ( .balance -= usd_out );
                        update tok ( .balance -= token_amount );
                        update usd_account @ { .owner == account.id } ( .balance += usd_out );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "unbacked-conversion-credit" !in rules(result),
            "reserve-debited conversion must stay clean; got ${result.findings}"
        )
    }

    /** A fee computed from CONFIG (chain_context.args) is not an oracle conversion - clean. */
    @Test
    fun configRateFeeTransferStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity wallet { key owner: byte_array; mutable balance: integer = 0; }
                    entity treasury { key id: integer; mutable balance: integer = 0; }
                    operation transfer(to: byte_array, amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        val sender = wallet @ { .owner == account.id };
                        require(sender.balance >= amount, "insufficient");
                        val fee = amount * chain_context.args.fee_rate / 10000;
                        update sender ( .balance -= amount );
                        update wallet @ { .owner == to } ( .balance += amount - fee );
                        update treasury @ { .id == 0 } ( .balance += fee );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "unbacked-conversion-credit" !in rules(result),
            "config-sourced rates must not count as oracle prices; got ${result.findings}"
        )
    }

    /** An escrow purchase that reads a listing price but does no rate arithmetic - clean. */
    @Test
    fun escrowBuyReadingListingPriceStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity listing { key id: integer; seller: byte_array; mutable price: integer; }
                    entity credit_account { key owner: byte_array; mutable balance: integer = 0; }
                    entity purchase { index buyer: byte_array; amount: integer; }
                    operation buy(listing_id: integer) {
                        val account = auth.authenticate();
                        val l = listing @ { .id == listing_id };
                        val buyer = credit_account @ { .owner == account.id };
                        require(buyer.balance >= l.price, "insufficient credits");
                        update buyer ( .balance -= l.price );
                        create purchase(buyer = account.id, amount = l.price);
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "unbacked-conversion-credit" !in rules(result),
            "no conversion arithmetic, no advisory; got ${result.findings}"
        )
    }

    // ---- value-sink-without-withdrawal ----

    /**
     * Both adversary fee sinks (dapp_a treasury, dapp_b fee_pot): balance
     * += fee on every transfer, no operation ever debits it - the value is
     * permanently locked. The helper-returned alias (get_or_create_fee_pot)
     * is how the real dApps write it, so it must resolve.
     */
    @Test
    fun feePotOnlyEverCreditedIsMediumAdvisoryAndNeverBlocks() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity wallet { key owner: byte_array; mutable balance: integer = 0; }
                    entity fee_pot { key id: integer; mutable balance: integer = 0; }
                    function get_or_create_fee_pot(): fee_pot {
                        val f = fee_pot @? { .id == 0 };
                        if (f != null) return f;
                        return create fee_pot(id = 0);
                    }
                    operation transfer(to: byte_array, amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        val sender = wallet @ { .owner == account.id };
                        require(sender.balance >= amount, "insufficient");
                        val fee = amount / 100;
                        update sender ( .balance -= amount );
                        update wallet @ { .owner == to } ( .balance += amount - fee );
                        val f = get_or_create_fee_pot();
                        update f ( .balance += fee );
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "value-sink-without-withdrawal" }
        assertTrue(hit.isNotEmpty(), "credit-only fee pot must get the advisory; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
        assertTrue(result.ok, "an economic advisory must never make ok=false; got ${result.findings}")
    }

    /** A withdrawal operation debiting the pot - even in another file - is the fix. */
    @Test
    fun feePotWithWithdrawalPathStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity wallet { key owner: byte_array; mutable balance: integer = 0; }
                    entity fee_pot { key id: integer; mutable balance: integer = 0; }
                    operation transfer(to: byte_array, amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        val sender = wallet @ { .owner == account.id };
                        require(sender.balance >= amount, "insufficient");
                        val fee = amount / 100;
                        update sender ( .balance -= amount );
                        update wallet @ { .owner == to } ( .balance += amount - fee );
                        update fee_pot @ { .id == 0 } ( .balance += fee );
                    }
                """.trimIndent(),
                "admin.rell" to """
                    module;
                    operation withdraw_fees(amount: integer) {
                        require(op_context.is_signer(chain_context.args.admin_pubkey), "admin only");
                        require(amount > 0, "positive");
                        val pot = fee_pot @ { .id == 0 };
                        require(pot.balance >= amount, "insufficient fees");
                        update pot ( .balance -= amount );
                        update wallet @ { .owner == chain_context.args.admin_wallet } ( .balance += amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "value-sink-without-withdrawal" !in rules(result),
            "a withdrawal path anywhere in the app must clear the advisory; got ${result.findings}"
        )
    }

    /** Monotonic statistics counters are not value sinks - the name gate keeps them out. */
    @Test
    fun statsCounterStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity stats { key id: integer; mutable total_transfer_count: integer = 0; }
                    operation record(n: integer) {
                        val account = auth.authenticate();
                        require(n > 0, "positive");
                        update stats @ { .id == 0 } ( .total_transfer_count += n );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "value-sink-without-withdrawal" !in rules(result),
            "a counter is not a value sink; got ${result.findings}"
        )
    }

    /** An unresolvable write (dotted target) could be the withdrawal - conservative silence. */
    @Test
    fun unresolvableDebitSuppressesTheSinkAdvisory() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity owner_link { key id: integer; pot: fee_pot; }
                    entity fee_pot { key id: integer; mutable balance: integer = 0; }
                    operation collect(fee: integer) {
                        val account = auth.authenticate();
                        require(fee > 0, "positive");
                        update fee_pot @ { .id == 0 } ( .balance += fee );
                    }
                    operation payout(link_id: integer, amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        val l = owner_link @ { .id == link_id };
                        update l.pot ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "value-sink-without-withdrawal" !in rules(result),
            "a dotted-target debit must count as a possible withdrawal; got ${result.findings}"
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
