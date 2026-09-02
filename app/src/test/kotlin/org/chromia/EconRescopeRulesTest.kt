package org.chromia

import org.chromia.tools.DappScaffold
import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Adversary round 4 re-scoped three economic advisories (see the round-4 rows
 * of exploit-corpus/CORPUS.md):
 *
 *  1. unbacked-conversion-credit gains the TIME shape - a credit derived from
 *     elapsed block time x a rate/stake with no debit of a quantity from the
 *     same elapsed term (a staking dapp minted rewards from an empty pool).
 *  2. unbacked-conversion-credit no longer misreads an escrow record: a value
 *     written into a fresh row whose exact amount is debited elsewhere in the
 *     same operation is a transfer, not a mint - and the quote that row stores
 *     is tracked, so paying it out later WITHOUT a reserve debit is caught.
 *  3. vote-gated-payout-drops-quorum: a value-moving op gated by a tally
 *     comparison whose own body has no quorum term while a sibling op gated
 *     by the same tallies does (the copied payout that lost its quorum line).
 *
 * All three are MEDIUM and never make ok=false. Every exploit test asserts
 * both. The "knownEvasion"/"knownExposure" tests pin, honestly, what the
 * rules still miss or still over-flag after attacking them - they are the
 * record, not a claim of coverage.
 */
class EconRescopeRulesTest {

    private fun rules(result: RellSecurityCheck.Result): List<String> = result.findings.map { it.rule }

    private fun resource(path: String): String =
        javaClass.classLoader.getResource(path)?.readText() ?: error("test resource missing: $path")

    private fun analyze(main: String) = RellSecurityCheck.analyze(mapOf("main.rell" to main))

    private fun conversionFindings(result: RellSecurityCheck.Result) =
        result.findings.filter { it.rule == "unbacked-conversion-credit" }

    private fun assertTimeAdvisory(result: RellSecurityCheck.Result, op: String, why: String) {
        val hit = conversionFindings(result).filter { it.text.contains("operation $op ") && it.text.contains("elapsed") }
        assertTrue(hit.isNotEmpty(), "$why: expected the elapsed-time advisory on $op; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity, "economic advisories are MEDIUM, never blocking")
        assertTrue(result.ok, "an economic advisory must never make ok=false; got ${result.findings}")
    }

    private fun assertNoConversionAdvisory(result: RellSecurityCheck.Result, why: String) {
        assertTrue(conversionFindings(result).isEmpty(), "$why; got ${result.findings}")
    }

    // ------------------------------------------------------------------
    // 1. time-based emission with no pool debit
    // ------------------------------------------------------------------

    /** The round-4 staking shape, parameterised so each evasion changes exactly one thing. */
    private fun staking(
        claim: String,
        rate: String = "val REWARD_PER_SECOND = 1;",
        helpers: String = "",
        extraOps: String = ""
    ): String = """
        module;
        import lib.ft4.auth;
        entity member { key owner: byte_array; mutable balance: integer = 0; mutable staked: integer = 0; mutable last_claim: timestamp = 0; mutable pending_reward: integer = 0; }
        object pool { mutable total_staked: integer = 0; mutable undistributed: integer = 0; }
        $rate
        @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
        function member_of(owner: byte_array): member = require(member @? { .owner == owner }, "register first");
        $helpers
        operation stake(amount: integer) {
            val a = auth.authenticate(); val m = member_of(a.id);
            require(amount > 0, "positive"); require(m.balance >= amount, "insufficient");
            update m ( .balance -= amount, .staked += amount, .last_claim = op_context.last_block_time );
            pool.total_staked += amount;
        }
        operation claim_rewards() {
            val a = auth.authenticate(); val m = member_of(a.id);
            $claim
        }
        $extraOps
    """.trimIndent()

    private val ROUND4_CLAIM = """
        val now = op_context.last_block_time;
        val since = if (m.last_claim == 0) now else m.last_claim;
        val reward = m.staked * ((now - since) / 1000) * REWARD_PER_SECOND / 1000;
        require(reward > 0, "nothing to claim");
        update m ( .last_claim = now, .balance += reward );
    """.trimIndent()

    @Test
    fun timeRateEmissionWithoutPoolDebitIsMediumAdvisoryAndNeverBlocks() {
        val result = analyze(staking(ROUND4_CLAIM))
        assertTimeAdvisory(result, "claim_rewards", "stake x elapsed x rate credited with no pool debit is the round-4 mint")
    }

    /** The rate constant, the locals and the timestamp field renamed: the rule keys on the time source and the flow, not names. */
    @Test
    fun timeEmissionSurvivesRenamingEveryIdentifier() {
        val claim = """
            val t = op_context.last_block_time;
            val s = if (m.last_claim == 0) t else m.last_claim;
            val pay = m.staked * ((t - s) / 1000) * K / 1000;
            require(pay > 0, "nothing");
            update m ( .last_claim = t, .balance += pay );
        """.trimIndent()
        val result = analyze(staking(claim, rate = "val K = 7;"))
        assertTimeAdvisory(result, "claim_rewards", "renaming the rate constant and every local must not silence the rule")
    }

    /** Elapsed math moved into a helper, and the credit moved into another helper. */
    @Test
    fun timeEmissionMathAndPayoutInHelpersStillAdvises() {
        val helpers = """
            function owed_to(m: member): integer {
                val now = op_context.last_block_time;
                val since = if (m.last_claim == 0) now else m.last_claim;
                return m.staked * ((now - since) / 1000) * REWARD_PER_SECOND / 1000;
            }
            function pay(m: member, amount: integer) {
                update m ( .last_claim = op_context.last_block_time, .balance += amount );
            }
        """.trimIndent()
        val claim = """
            val reward = owed_to(m);
            require(reward > 0, "nothing to claim");
            pay(m, reward);
        """.trimIndent()
        val result = analyze(staking(claim, helpers = helpers))
        assertTimeAdvisory(result, "claim_rewards", "helper-wrapped reward math and payout are flattened into the operation")
    }

    /** Accrue the time-derived amount into a pending field in one op, pay it in another: the accrual is the mint. */
    @Test
    fun timeEmissionAccruedIntoPendingFieldAcrossTwoOpsStillAdvises() {
        val claim = """
            val reward = m.pending_reward;
            require(reward > 0, "nothing to claim");
            update m ( .pending_reward = 0, .balance += reward );
        """.trimIndent()
        val accrue = """
            operation accrue() {
                val a = auth.authenticate(); val m = member_of(a.id);
                val elapsed = op_context.last_block_time - m.last_claim;
                update m ( .last_claim = op_context.last_block_time, .pending_reward += m.staked * elapsed * REWARD_PER_SECOND );
            }
        """.trimIndent()
        val result = analyze(staking(claim, extraOps = accrue))
        assertTimeAdvisory(result, "accrue", "the accrual op credits pending_reward from elapsed time with no debit")
    }

    @Test
    fun blockHeightEmissionStillAdvises() {
        val claim = """
            val h = op_context.block_height;
            val reward = m.staked * (h - m.last_claim) * REWARD_PER_SECOND;
            update m ( .last_claim = h, .balance += reward );
        """.trimIndent()
        val result = analyze(staking(claim))
        assertTimeAdvisory(result, "claim_rewards", "height-based emission is the same mint")
    }

    /** `+=` spelled as `= old + x`, and the time source used inline with no local. */
    @Test
    fun rewrittenCompoundAssignmentAndInlineTimeSourceStillAdvise() {
        val claim = """
            update m ( .balance = m.balance + m.staked * ((op_context.last_block_time - m.last_claim) / 1000) * REWARD_PER_SECOND, .last_claim = op_context.last_block_time );
        """.trimIndent()
        val result = analyze(staking(claim))
        assertTimeAdvisory(result, "claim_rewards", "= old + x is a credit; the time source needs no local")
    }

    /** A debit of an unrelated quantity (or of literal zero) does not back the emission. */
    @Test
    fun decoyDebitOfUnrelatedAmountDoesNotSilence() {
        val claim = """
            val now = op_context.last_block_time;
            val reward = m.staked * ((now - m.last_claim) / 1000) * REWARD_PER_SECOND;
            val fee = 1;
            update m ( .last_claim = now, .staked -= fee, .balance += reward );
            pool.undistributed -= 0;
        """.trimIndent()
        val result = analyze(staking(claim))
        assertTimeAdvisory(result, "claim_rewards", "a debit that does not move the emitted quantity is not backing")
    }

    /**
     * The correct pattern (adversary round 4 dapp_c_staking, the REAL dapp the
     * exploit model was cut from): update_pool() emits min(pool.undistributed,
     * elapsed * RATE), debits pool.undistributed by it and accumulates a
     * per-share index; settle() credits pending_reward from that index; claim
     * debits pool.unclaimed. Every credit is backed by a debit derived from the
     * same elapsed term, two helpers deep - it must stay clean.
     */
    @Test
    fun poolDebitedPerShareStakingStaysClean() {
        val real = resource("exploit-corpus/realworld/adversary-round4/dapp_c_staking/src/main.rell")
        val result = analyze(real)
        assertNoConversionAdvisory(result, "the pool-debited per-share staking dapp is conserving and must stay clean")
        assertTrue(result.ok, "dapp_c_staking must keep passing: ${result.findings}")
        // ...and the V8 model the adversary drained (same dapp, claim rewritten to time x rate) must flag.
        val poc = resource("exploit-corpus/realworld/adversary-round4/poc/v8_staking_mint_model.rell")
        assertTimeAdvisory(analyze(poc), "claim_rewards", "the drained V8 staking model")
    }

    /** Time-scaled vesting paid by debiting the grant's remaining balance - conserving, clean. */
    @Test
    fun vestingClaimDebitingRemainingStaysClean() {
        val result = analyze(
            """
            module;
            import lib.ft4.auth;
            entity wallet { key owner: byte_array; mutable balance: integer = 0; }
            entity grant { key owner: byte_array; total: integer; start: timestamp; duration: integer; mutable remaining: integer; }
            operation claim() {
                val a = auth.authenticate();
                val g = require(grant @? { .owner == a.id }, "no grant");
                val w = wallet @ { .owner == a.id };
                val now = op_context.last_block_time;
                val vested = g.total * (now - g.start) / g.duration;
                val claimable = vested - (g.total - g.remaining);
                require(claimable > 0, "nothing vested");
                update g ( .remaining -= claimable );
                update w ( .balance += claimable );
            }
            """.trimIndent()
        )
        assertNoConversionAdvisory(result, "a vesting claim that debits the grant's remaining balance is backed")
    }

    /**
     * KNOWN EXPOSURE. The same vesting claim with the bookkeeping inverted -
     * a monotone `released +=` counter instead of a `remaining -=` debit - is
     * conserving too, but nothing in the operation is debited, so the rule
     * advises. Pinned so the exposure is visible; the fix text points at the
     * debit form, which is also the form where the cap is structural.
     */
    @Test
    fun knownExposureVestingWithReleasedCounterIsAdvised() {
        val result = analyze(
            """
            module;
            import lib.ft4.auth;
            entity wallet { key owner: byte_array; mutable balance: integer = 0; }
            entity grant { key owner: byte_array; total: integer; start: timestamp; duration: integer; mutable released: integer = 0; }
            operation claim() {
                val a = auth.authenticate();
                val g = require(grant @? { .owner == a.id }, "no grant");
                val w = wallet @ { .owner == a.id };
                val now = op_context.last_block_time;
                val vested = g.total * (now - g.start) / g.duration;
                val claimable = vested - g.released;
                require(claimable > 0, "nothing vested");
                update g ( .released += claimable );
                update w ( .balance += claimable );
            }
            """.trimIndent()
        )
        assertTimeAdvisory(result, "claim", "known exposure: released-counter vesting has no debit and is advised")
    }

    /**
     * KNOWN EVASION. A debit whose amount is built from the emitted quantity
     * but nets to zero (`reward - reward`) shares the elapsed term with the
     * credit and reads as backing. Deliberately not chased: recognising
     * algebraic identities is not a static rule's job, and the construction
     * is visible to any reader.
     */
    @Test
    fun knownEvasionSelfCancellingDebitSilencesTheAdvisory() {
        val claim = """
            val now = op_context.last_block_time;
            val reward = m.staked * ((now - m.last_claim) / 1000) * REWARD_PER_SECOND;
            pool.undistributed -= reward - reward;
            update m ( .last_claim = now, .balance += reward );
        """.trimIndent()
        val result = analyze(staking(claim))
        assertNoConversionAdvisory(result, "pinned: a self-cancelling debit currently counts as backing")
    }

    /**
     * KNOWN LIMITATION shared with every value rule in the gate: the credited
     * field must be value-named (balance/amount/reward/...). A field called
     * `points` is invisible to VALUE_FIELD_NAME_REGEX, and to VALUE_MUTATION,
     * amount-lower-bound and the sink rule alike. Not this lane's scope.
     */
    @Test
    fun knownLimitationNonValueFieldNameIsInvisible() {
        val result = analyze(
            staking(ROUND4_CLAIM.replace(".balance += reward", ".points += reward"))
                .replace("mutable balance: integer = 0;", "mutable balance: integer = 0; mutable points: integer = 0;")
        )
        assertNoConversionAdvisory(result, "pinned: a non-value-named credited field is invisible")
    }

    // ------------------------------------------------------------------
    // 2. escrow records are not mints; stored quotes paid without a reserve are
    // ------------------------------------------------------------------

    private fun vault(request: String, process: String): String = """
        module;
        import lib.ft4.auth;
        entity account { key owner: byte_array; mutable balance: integer = 0; mutable tokens: integer = 0; }
        object feed { mutable price: integer = 0; mutable updated_at: timestamp = 0; }
        object reserve { mutable cash: integer = 0; mutable tokens: integer = 0; }
        entity redemption { index owner: byte_array; tokens_locked: integer; cash_due: integer; mutable paid: boolean = false; }
        val PRICE_SCALE = 1000000;
        @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
        function acct(owner: byte_array): account = require(account @? { .owner == owner }, "register first");
        function current_price(): integer {
            require(feed.price > 0, "no price");
            require(op_context.last_block_time - feed.updated_at <= 86400000, "stale");
            return feed.price;
        }
        operation request_redemption(tokens_in: integer) {
            val a = auth.authenticate(); val seller = acct(a.id);
            require(tokens_in > 0 and seller.tokens >= tokens_in, "tokens");
            $request
        }
        operation process_redemptions() {
            auth.authenticate();
            for (r in redemption @* { .paid == false } ( $ ) limit 10) {
                val owner = acct(r.owner);
                update r ( .paid = true );
                $process
            }
        }
    """.trimIndent()

    /** The round-4 false positive, with the cash paid from an OBJECT reserve (a dotted debit, not an update). */
    @Test
    fun escrowRecordWithSameAmountDebitedStaysClean() {
        val result = analyze(
            vault(
                request = """
                    val cash_due = tokens_in * current_price() / PRICE_SCALE;
                    require(cash_due > 0, "too small");
                    update seller ( .tokens -= tokens_in );
                    create redemption(owner = a.id, tokens_locked = tokens_in, cash_due = cash_due);
                """.trimIndent(),
                process = """
                    if (reserve.cash < r.cash_due) break;
                    reserve.cash -= r.cash_due;
                    reserve.tokens += r.tokens_locked;
                    update owner ( .balance += r.cash_due );
                """.trimIndent()
            )
        )
        assertNoConversionAdvisory(result, "an escrow row whose amount is debited from the seller in the same op is not a mint")
        assertTrue(result.ok, result.findings.toString())
    }

    /** The quote is not stored: cash is computed at processing time from the current price and paid from the reserve. */
    @Test
    fun redemptionSettledAtProcessingTimeStaysClean() {
        val result = analyze(
            vault(
                request = """
                    require(tokens_in * current_price() / PRICE_SCALE > 0, "too small");
                    update seller ( .tokens -= tokens_in );
                    create redemption(owner = a.id, tokens_locked = tokens_in, cash_due = 0);
                """.trimIndent(),
                process = """
                    val due = r.tokens_locked * current_price() / PRICE_SCALE;
                    if (reserve.cash < due) break;
                    reserve.cash -= due;
                    reserve.tokens += r.tokens_locked;
                    update owner ( .balance += due );
                """.trimIndent()
            )
        )
        assertNoConversionAdvisory(result, "settling at processing time out of a debited reserve is conserving")
    }

    /**
     * The evasion the escrow fix opens if taken naively: the conversion is
     * stored in the row at request time (no reserve involved, correctly), and
     * the paying op credits the stored quote WITHOUT any reserve debit - a
     * mint one operation removed from the price read.
     */
    @Test
    fun storedQuotePaidWithoutReserveDebitIsAdvised() {
        val result = analyze(
            vault(
                request = """
                    val cash_due = tokens_in * current_price() / PRICE_SCALE;
                    require(cash_due > 0, "too small");
                    update seller ( .tokens -= tokens_in );
                    create redemption(owner = a.id, tokens_locked = tokens_in, cash_due = cash_due);
                """.trimIndent(),
                process = """
                    update owner ( .balance += r.cash_due );
                """.trimIndent()
            )
        )
        val hit = conversionFindings(result).filter { it.text.contains("operation process_redemptions ") }
        assertTrue(hit.isNotEmpty(), "paying a stored price-derived quote with no reserve debit is the split mint; got ${result.findings}")
        assertTrue(hit.first().text.contains("cash_due"), "the finding names the stored quote: ${hit.first().text}")
        assertTrue(conversionFindings(result).none { it.text.contains("request_redemption") }, "the escrow op itself stays clean: ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
        assertTrue(result.ok, "advisory only: ${result.findings}")
    }

    /** The real round-4 multivault: escrow queue plus a settled path - no conversion advisory anywhere. */
    @Test
    fun realMultivaultStaysClean() {
        val result = analyze(resource("exploit-corpus/realworld/adversary-round4/dapp_b_multivault/src/main.rell"))
        assertNoConversionAdvisory(result, "dapp_b_multivault conserves value through its redemption queue")
    }

    /** The existing oracle mint must not be backed by the escrow relaxation: its debit is the input amount, its credit the converted one. */
    @Test
    fun conversionOfADebitedAmountIsStillAMint() {
        val result = analyze(resource("exploit-corpus/samples/econ-oracle-unbacked-mint/main.rell"))
        assertTrue(conversionFindings(result).isNotEmpty(), "debiting token_amount does not back a credit of token_amount * price; got ${result.findings}")
    }

    // ------------------------------------------------------------------
    // 3. the copied payout that dropped its quorum line
    // ------------------------------------------------------------------

    private val RULE3 = "vote-gated-payout-drops-quorum"

    private fun dao(execute: String, payout: String, helpers: String = ""): String = """
        module;
        import lib.ft4.auth;
        entity member { key owner: byte_array; mutable balance: integer = 0; mutable stake: integer = 0; }
        object dao { mutable treasury_balance: integer = 0; mutable total_stake: integer = 0; }
        entity proposal {
            index proposer: byte_array; beneficiary: byte_array; amount: integer; deadline: timestamp; quorum_weight: integer;
            mutable yes_weight: integer = 0; mutable no_weight: integer = 0; mutable executed: boolean = false;
        }
        val QUORUM_BPS = 5000;
        @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
        function member_of(owner: byte_array): member = require(member @? { .owner == owner }, "register first");
        $helpers
        operation cast_vote(proposal_id: rowid, support: boolean) {
            val a = auth.authenticate(); val voter = member_of(a.id);
            val p = require(proposal @? { .rowid == proposal_id }, "not found");
            require(op_context.last_block_time < p.deadline, "ended");
            if (support) update p ( .yes_weight += voter.stake ); else update p ( .no_weight += voter.stake );
        }
        operation execute_proposal(proposal_id: rowid) {
            auth.authenticate();
            val p = require(proposal @? { .rowid == proposal_id }, "not found");
            require(not p.executed, "done"); require(op_context.last_block_time >= p.deadline, "open");
            $execute
            val b = member_of(p.beneficiary);
            update p ( .executed = true ); dao.treasury_balance -= p.amount; update b ( .balance += p.amount );
        }
        operation execute_payout(proposal_id: rowid) {
            auth.authenticate();
            val p = require(proposal @? { .rowid == proposal_id }, "not found");
            require(not p.executed, "done"); require(op_context.last_block_time >= p.deadline, "open");
            $payout
            val b = member_of(p.beneficiary);
            update p ( .executed = true ); dao.treasury_balance -= p.amount; update b ( .balance += p.amount );
        }
    """.trimIndent()

    private val QUORUM_AND_MAJORITY = """
        require(p.yes_weight + p.no_weight >= p.quorum_weight, "quorum not reached");
        require(p.yes_weight > p.no_weight, "rejected");
    """.trimIndent()

    private fun assertRule3(result: RellSecurityCheck.Result, op: String, why: String) {
        val hit = result.findings.filter { it.rule == RULE3 && it.text.contains("operation $op ") }
        assertTrue(hit.isNotEmpty(), "$why: expected $RULE3 on $op; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity, "advisory MEDIUM, never blocking")
        assertTrue(result.findings.none { it.rule == RULE3 && !it.text.contains("operation $op ") }, "only the dropped path is flagged: ${result.findings}")
        assertTrue(result.ok, "an economic advisory must never make ok=false; got ${result.findings}")
    }

    @Test
    fun copiedPayoutWithoutQuorumIsMediumAdvisoryAndNeverBlocks() {
        val result = analyze(resource("exploit-corpus/samples/r4-gov-copied-payout-no-quorum/main.rell"))
        assertRule3(result, "execute_payout", "the round-4 copied payout minus the quorum line")
        assertTrue("majority-without-quorum" !in rules(result), "the submission-wide bias of majority-without-quorum is untouched")
    }

    @Test
    fun reversedComparisonStillAdvises() {
        val result = analyze(dao(QUORUM_AND_MAJORITY, """require(p.no_weight < p.yes_weight, "rejected");"""))
        assertRule3(result, "execute_payout", "no < yes is the same gate")
    }

    /** The majority check hidden in a helper the copied op calls; the quorum check only in the sibling. */
    @Test
    fun majorityCheckInHelperStillAdvises() {
        val result = analyze(
            dao(
                execute = QUORUM_AND_MAJORITY,
                payout = """require(approved(p), "rejected");""",
                helpers = "function approved(p: proposal): boolean = p.yes_weight > p.no_weight;"
            )
        )
        assertRule3(result, "execute_payout", "the tally comparison one helper deep is flattened into the op")
    }

    /** Both paths gate through one shared helper that checks quorum AND majority - the recommended fix - clean. */
    @Test
    fun quorumInSharedHelperStaysClean() {
        val result = analyze(
            dao(
                execute = """require(passed(p), "not passed");""",
                payout = """require(passed(p), "not passed");""",
                helpers = "function passed(p: proposal): boolean = p.yes_weight + p.no_weight >= p.quorum_weight and p.yes_weight > p.no_weight;"
            )
        )
        assertTrue(RULE3 !in rules(result), "a shared quorum helper gates both paths; got ${result.findings}")
    }

    /** No sibling has a quorum term: the deliberate majority-without-quorum bias applies and this rule has no evidence - quiet. */
    @Test
    fun consistentlyQuorumlessStakeWeightedDaoStaysClean() {
        val result = analyze(
            dao(
                execute = """require(p.yes_weight > p.no_weight, "rejected");""",
                payout = """require(p.yes_weight > p.no_weight, "rejected");"""
            )
        )
        assertTrue(RULE3 !in rules(result), "without a quorum-gated sibling there is no dropped gate to report; got ${result.findings}")
    }

    /** The real grants dapp (two quorum-gated paths) and the governance template stay clean. */
    @Test
    fun realGrantsDappAndGovernanceTemplateStayClean() {
        val grants = analyze(resource("exploit-corpus/realworld/adversary-round4/dapp_a_grants/src/main.rell"))
        assertTrue(RULE3 !in rules(grants), "dapp_a_grants gates every payout path with a quorum; got ${grants.findings}")
        assertNoConversionAdvisory(grants, "dapp_a_grants has no conversion")
        val template = analyze(DappScaffold.files("t", template = "governance").getValue("src/main.rell"))
        assertEquals(emptyList<RellSecurityCheck.Finding>(), template.findings, "the governance template must stay finding-free")
    }

    /**
     * KNOWN EVASION. Any reference to a quorum/weight term in the copied op's
     * body counts as evidence - an unused `val q = p.quorum_weight;` decoy
     * silences the rule. Deciding whether a referenced term actually GATES the
     * payout is the same undecidable question the submission-wide bias
     * exists for; pinned rather than pretended.
     */
    @Test
    fun knownEvasionDecoyQuorumReferenceSilences() {
        val result = analyze(
            dao(
                execute = QUORUM_AND_MAJORITY,
                payout = """val q = p.quorum_weight; require(p.yes_weight > p.no_weight, "rejected");"""
            )
        )
        assertTrue(RULE3 !in rules(result), "pinned: a decoy reference to a quorum term reads as evidence")
    }

    /**
     * KNOWN EXPOSURE. A path that legitimately needs no quorum - refunding a
     * proposer's deposit once the proposal is rejected (no > yes) - has the
     * exact shape and is advised. The finding text says so; pinned so the
     * exposure is on record.
     */
    @Test
    fun knownExposureRejectedProposalRefundPathIsAdvised() {
        val result = analyze(
            dao(
                execute = QUORUM_AND_MAJORITY,
                payout = """require(p.no_weight > p.yes_weight, "not rejected");"""
            )
        )
        assertRule3(result, "execute_payout", "pinned exposure: a quorumless refund-on-rejection path is advised")
    }
}
