package org.chromia

import org.chromia.tools.DappScaffold
import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Adversary round 5 produced three rule defects; this pins the fix for each
 * (the corresponding corpus rows are `r5-*` in exploit-corpus/CORPUS.md):
 *
 *  1. value-sink-without-withdrawal cried wolf on a per-share reward
 *     accumulator and on the stake total it divides by - both are accounting
 *     terms, not pots, and the rule's own advice (add an admin op that debits
 *     it) would corrupt every staker's payout. The regression appeared the
 *     moment a builder moved `object pool` to `entity pool`, which is the
 *     first change a multi-pool program needs.
 *  2. unbacked-conversion-credit called a fully-backed vesting payout a mint:
 *     it wanted a `-=` on a pool row and did not read a released-so-far
 *     counter as the same debit.
 *  3. block-clock-randomness is new: the block clock used to SELECT who
 *     receives value is a drain (HIGH), and the block clock used to BOUND when
 *     something may happen is every legitimate schedule and must stay silent.
 *
 * The evasion tests attack rule 3 the way the lane brief requires - rename
 * everything, route through helpers and intermediate vals, swap time for
 * height, hash it, park it in a row, split the draw from the payout - and the
 * precision tests re-assert the templates' own clock shapes stay clean.
 */
class Round5RuleFixesTest {

    private fun analyze(main: String) = RellSecurityCheck.analyze(mapOf("main.rell" to main))

    private fun rules(result: RellSecurityCheck.Result) = result.findings.map { it.rule }

    private fun assertNoRule(result: RellSecurityCheck.Result, rule: String, why: String) =
        assertTrue(result.findings.none { it.rule == rule }, "$why; got ${result.findings}")

    private fun assertRandomnessDrain(result: RellSecurityCheck.Result, why: String) {
        val hit = result.findings.filter { it.rule == "block-clock-randomness" }
        assertTrue(hit.isNotEmpty(), "$why: expected block-clock-randomness; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity, "the clock deciding a payee is a drain, not an advisory")
        assertTrue(!result.ok, "a HIGH finding must make ok=false")
    }

    // ---------- 1. the accumulator is not a locked fee pot ----------

    private val perShareAccumulator = """
        module;
        import lib.ft4.auth;
        entity member { key owner: byte_array; mutable balance: integer = 0; }
        entity pool {
            key id: text;
            mutable total_staked: integer = 0;
            mutable undistributed: integer = 0;
            mutable unclaimed: integer = 0;
            mutable acc_reward_per_share: big_integer = 0L;
            mutable last_update: timestamp = 0;
        }
        entity position { key member, pool; mutable staked: integer = 0; mutable reward_snapshot: big_integer = 0L; mutable pending_reward: integer = 0; }
        val REWARD_PER_SECOND = 1;
        val ACC_SCALE = 1000000000000L;
        @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
        function member_of(owner: byte_array): member = require(member @? { .owner == owner }, "register first");
        function pool_of(pool_id: text): pool = require(pool @? { .id == pool_id }, "no such pool");
        function update_pool(p: pool) {
            val now = op_context.last_block_time;
            if (p.last_update == 0) { update p ( .last_update = now ); return; }
            val elapsed_ms = now - p.last_update;
            if (elapsed_ms <= 0) return;
            update p ( .last_update = now );
            if (p.total_staked == 0 or p.undistributed == 0) return;
            val earned = min(p.undistributed, elapsed_ms / 1000 * REWARD_PER_SECOND);
            if (earned <= 0) return;
            update p (
                .undistributed -= earned,
                .unclaimed += earned,
                .acc_reward_per_share += earned.to_big_integer() * ACC_SCALE / p.total_staked.to_big_integer()
            );
        }
        operation stake(pool_id: text, amount: integer) {
            val account = auth.authenticate();
            val m = member_of(account.id);
            val p = pool_of(pool_id);
            require(amount > 0, "amount must be positive");
            require(m.balance >= amount, "insufficient balance");
            update_pool(p);
            create position(member = m, pool = p, staked = amount, reward_snapshot = p.acc_reward_per_share);
            update m ( .balance -= amount );
            update p ( .total_staked += amount );
        }
        operation claim_rewards(pool_id: text) {
            val account = auth.authenticate();
            val m = member_of(account.id);
            val p = pool_of(pool_id);
            val pos = require(position @? { .member == m, .pool == p }, "no position");
            update_pool(p);
            val owed = (pos.staked.to_big_integer() * (p.acc_reward_per_share - pos.reward_snapshot) / ACC_SCALE).to_integer();
            update pos ( .pending_reward += owed, .reward_snapshot = p.acc_reward_per_share );
            val reward = pos.pending_reward;
            require(reward > 0, "nothing to claim");
            require(p.unclaimed >= reward, "pool cannot cover the claim");
            update p ( .unclaimed -= reward );
            update pos ( .pending_reward = 0 );
            update m ( .balance += reward );
        }
    """.trimIndent()

    @Test
    fun perShareAccumulatorIsNotAValueSink() {
        val result = analyze(perShareAccumulator)
        assertNoRule(
            result, "value-sink-without-withdrawal",
            "acc_reward_per_share is a scaled index and total_staked is the divisor it is scaled by - " +
                "debiting either corrupts every staker's payout, so neither is a locked pot"
        )
    }

    @Test
    fun aFeePotThatIsNeverReadIsStillASink() {
        // The guard on the exemption above: a sink is credited and then never
        // read at all, which is exactly why nothing can pay it out.
        val result = analyze(
            """
            module;
            import lib.ft4.auth;
            entity wallet { key owner: byte_array; mutable balance: integer = 0; }
            entity fee_pot { key id: integer; mutable balance: integer = 0; }
            @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
            operation transfer(to: byte_array, amount: integer) {
                val account = auth.authenticate();
                require(amount > 0, "amount must be positive");
                require(to.size() == 32, "invalid recipient id");
                val sender = wallet @ { .owner == account.id };
                require(sender.balance >= amount, "insufficient balance");
                val fee = amount / 100;
                update sender ( .balance -= amount );
                update wallet @ { .owner == to } ( .balance += amount - fee );
                update fee_pot @ { .id == 0 } ( .balance += fee );
            }
            """.trimIndent()
        )
        assertTrue(
            result.findings.any { it.rule == "value-sink-without-withdrawal" },
            "a credited-and-never-read fee pot is the true positive the exemption must not swallow; " +
                "got ${result.findings}"
        )
        assertTrue(result.ok, "the locked-sink advisory is MEDIUM and never blocking")
    }

    @Test
    fun anotherEntitysArithmeticDoesNotExemptASink() {
        // wallet.balance is divided all over this app; fee_pot.balance is not.
        // The exemption resolves the receiver (or requires the field name to be
        // unique) precisely so one cannot launder the other.
        val result = analyze(
            """
            module;
            import lib.ft4.auth;
            entity wallet { key owner: byte_array; mutable balance: integer = 0; }
            entity fee_pot { key id: integer; mutable balance: integer = 0; }
            @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
            operation split_pay(to: byte_array, amount: integer) {
                val account = auth.authenticate();
                require(amount > 0, "amount must be positive");
                require(to.size() == 32, "invalid recipient id");
                val sender = wallet @ { .owner == account.id };
                val half = sender.balance / 2;
                require(half >= amount, "insufficient balance");
                update sender ( .balance -= amount );
                update wallet @ { .owner == to } ( .balance += amount - amount / 50 );
                update fee_pot @ { .id == 0 } ( .balance += amount / 50 );
            }
            """.trimIndent()
        )
        assertTrue(
            result.findings.any { it.rule == "value-sink-without-withdrawal" },
            "wallet.balance / 2 must not exempt fee_pot.balance; got ${result.findings}"
        )
    }

    // ---------- 2. a released-so-far counter IS the paired debit ----------

    private val vestingGrant = """
        module;
        import lib.ft4.auth;
        entity member { key owner: byte_array; mutable balance: integer = 0; mutable next_grant_seq: integer = 0; }
        entity vesting_grant { key member, seq: integer; mutable total: integer; mutable released: integer = 0; start: timestamp; }
        entity reward_pool { key id: text; mutable unclaimed: integer = 0; }
        val VEST_CLIFF_MS = 30 * 24 * 60 * 60 * 1000;
        val VEST_DURATION_MS = 90 * 24 * 60 * 60 * 1000;
        @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
        function member_of(owner: byte_array): member = require(member @? { .owner == owner }, "register first");
        operation grant_reward(pool_id: text, amount: integer) {
            val account = auth.authenticate();
            val m = member_of(account.id);
            val p = require(reward_pool @? { .id == pool_id }, "no such pool");
            require(amount > 0, "amount must be positive");
            require(p.unclaimed >= amount, "pool cannot cover the grant");
            update p ( .unclaimed -= amount );
            create vesting_grant(member = m, seq = m.next_grant_seq, total = amount, start = op_context.last_block_time);
            update m ( .next_grant_seq += 1 );
        }
        function vested_amount(g: vesting_grant, now: timestamp): integer {
            if (now < g.start + VEST_CLIFF_MS) return 0;
            val elapsed = now - g.start;
            if (elapsed >= VEST_DURATION_MS) return g.total;
            return (g.total.to_big_integer() * elapsed.to_big_integer() / VEST_DURATION_MS.to_big_integer()).to_integer();
        }
        operation withdraw_vested(seq: integer) {
            val account = auth.authenticate();
            val m = member_of(account.id);
            val g = require(vesting_grant @? { .member == m, .seq == seq }, "no such grant");
            val vested = vested_amount(g, op_context.last_block_time);
            val claimable = vested - g.released;
            require(claimable > 0, "nothing vested yet");
            update g ( .released += claimable );
            update m ( .balance += claimable );
            if (g.released >= g.total) delete g;
        }
    """.trimIndent()

    @Test
    fun aReleasedCounterOnAnEscrowedGrantBacksThePayout() {
        val result = analyze(vestingGrant)
        assertNoRule(
            result, "unbacked-conversion-credit",
            "released += claimable is the debit: it strictly reduces the next claim, and the grant's " +
                "total was itself debited from the pool when the row was created"
        )
    }

    @Test
    fun anUnfundedTimeCreditIsStillAMint() {
        // The guard on the exemption above: the round-4 staking mint has no
        // counter carrying the payout amount, so it stays caught.
        val result = analyze(
            """
            module;
            import lib.ft4.auth;
            entity member { key owner: byte_array; mutable balance: integer = 0; mutable staked: integer = 0; mutable last_claim: timestamp = 0; }
            val REWARD_PER_SECOND = 1;
            @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
            function member_of(owner: byte_array): member = require(member @? { .owner == owner }, "register first");
            operation claim_rewards() {
                val a = auth.authenticate();
                val m = member_of(a.id);
                val now = op_context.last_block_time;
                val since = if (m.last_claim == 0) now else m.last_claim;
                val reward = m.staked * ((now - since) / 1000) * REWARD_PER_SECOND / 1000;
                require(reward > 0, "nothing to claim");
                update m ( .last_claim = now, .balance += reward );
            }
            """.trimIndent()
        )
        assertTrue(
            result.findings.any { it.rule == "unbacked-conversion-credit" },
            "an elapsed-time credit with no counter and no pool debit is still minted; got ${result.findings}"
        )
    }

    // ---------- 3. the block clock: selector vs bound ----------

    private fun raffle(draw: String) = """
        module;
        import lib.ft4.auth;
        entity player { key owner: byte_array; mutable balance: integer = 0; }
        entity raffle { key id: text; closes_at: timestamp; mutable ticket_count: integer = 0; mutable pot: integer = 0; mutable drawn: boolean = false; }
        entity ticket { key raffle, idx: integer; holder: byte_array; }
        @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
        function player_of(owner: byte_array): player = require(player @? { .owner == owner }, "register first");
        $draw
    """.trimIndent()

    @Test
    fun theBlockClockPickingAWinnerIsHigh() {
        assertRandomnessDrain(
            analyze(
                raffle(
                    """
                    operation draw_winner(raffle_id: text) {
                        val account = auth.authenticate();
                        player_of(account.id);
                        val r = require(raffle @? { .id == raffle_id }, "no such raffle");
                        require(not r.drawn, "already drawn");
                        require(op_context.last_block_time >= r.closes_at, "still open");
                        require(r.ticket_count > 0, "no tickets");
                        val idx = op_context.last_block_time % r.ticket_count;
                        val t = require(ticket @? { .raffle == r, .idx == idx }, "missing ticket");
                        val w = player_of(t.holder);
                        val pot = r.pot;
                        update r ( .drawn = true, .pot = 0 );
                        update w ( .balance += pot );
                    }
                    """.trimIndent()
                )
            ),
            "the round-5 raffle drain"
        )
    }

    @Test
    fun renamingAndHelperRoutingAndHeightDoNotEvadeIt() {
        assertRandomnessDrain(
            analyze(
                raffle(
                    """
                    function tick(): integer = op_context.block_height;
                    function fold(a: integer, b: integer): integer = a % b;
                    operation settle(raffle_id: text) {
                        val account = auth.authenticate();
                        player_of(account.id);
                        val r = require(raffle @? { .id == raffle_id }, "no such raffle");
                        require(not r.drawn, "already drawn");
                        require(op_context.last_block_time >= r.closes_at, "still open");
                        require(r.ticket_count > 0, "no tickets");
                        val entropy = tick();
                        val mixed = entropy + r.ticket_count;
                        val chosen = fold(mixed, r.ticket_count);
                        val e = require(ticket @? { .raffle == r, .idx == chosen }, "missing ticket");
                        val beneficiary = player_of(e.holder);
                        val purse = r.pot;
                        update r ( .drawn = true, .pot = 0 );
                        update beneficiary ( .balance += purse );
                    }
                    """.trimIndent()
                )
            ),
            "block height reduced by a helper, laundered through two locals, every name changed"
        )
    }

    @Test
    fun hashingTheClockDoesNotEvadeIt() {
        assertRandomnessDrain(
            analyze(
                """
                module;
                import lib.ft4.auth;
                entity player { key owner: byte_array; mutable balance: integer = 0; }
                entity raffle { key id: text; closes_at: timestamp; mutable pot: integer = 0; mutable drawn: boolean = false; }
                entity ticket { key raffle, tag: byte_array; holder: byte_array; }
                @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
                function player_of(owner: byte_array): player = require(player @? { .owner == owner }, "register first");
                operation draw_winner(raffle_id: text) {
                    val account = auth.authenticate();
                    player_of(account.id);
                    val r = require(raffle @? { .id == raffle_id }, "no such raffle");
                    require(not r.drawn, "already drawn");
                    require(op_context.last_block_time >= r.closes_at, "still open");
                    val digest = op_context.last_block_time.to_gtv().hash();
                    val t = require(ticket @? { .raffle == r, .tag == digest }, "no ticket for this block");
                    val w = player_of(t.holder);
                    val pot = r.pot;
                    update r ( .drawn = true, .pot = 0 );
                    update w ( .balance += pot );
                }
                """.trimIndent()
            ),
            "a hash of a public number is a public number; there is no modulo here at all"
        )
    }

    @Test
    fun parkingTheClockInARowFirstDoesNotEvadeIt() {
        assertRandomnessDrain(
            analyze(
                """
                module;
                import lib.ft4.auth;
                entity player { key owner: byte_array; mutable balance: integer = 0; }
                entity raffle { key id: text; closes_at: timestamp; mutable ticket_count: integer = 0; mutable pot: integer = 0; mutable seed: integer = 0; mutable drawn: boolean = false; }
                entity ticket { key raffle, idx: integer; holder: byte_array; }
                @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
                function player_of(owner: byte_array): player = require(player @? { .owner == owner }, "register first");
                operation seal(raffle_id: text) {
                    auth.authenticate();
                    val r = require(raffle @? { .id == raffle_id }, "no such raffle");
                    require(op_context.last_block_time >= r.closes_at, "still open");
                    update r ( .seed = op_context.last_block_time );
                }
                operation draw_winner(raffle_id: text) {
                    val account = auth.authenticate();
                    player_of(account.id);
                    val r = require(raffle @? { .id == raffle_id }, "no such raffle");
                    require(not r.drawn, "already drawn");
                    require(r.seed > 0, "not sealed");
                    require(r.ticket_count > 0, "no tickets");
                    val idx = r.seed % r.ticket_count;
                    val t = require(ticket @? { .raffle == r, .idx == idx }, "missing ticket");
                    val w = player_of(t.holder);
                    val pot = r.pot;
                    update r ( .drawn = true, .pot = 0 );
                    update w ( .balance += pot );
                }
                """.trimIndent()
            ),
            "the drawing operation never mentions the clock - it reads the copy the previous one parked"
        )
    }

    @Test
    fun aClockCoinFlipBetweenTwoNamedPayeesIsCaught() {
        assertRandomnessDrain(
            analyze(
                """
                module;
                import lib.ft4.auth;
                entity player { key owner: byte_array; mutable balance: integer = 0; }
                entity duel { key id: text; a: byte_array; b: byte_array; settles_at: timestamp; mutable stake: integer = 0; mutable settled: boolean = false; }
                @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
                function player_of(owner: byte_array): player = require(player @? { .owner == owner }, "register first");
                operation settle_duel(duel_id: text) {
                    val account = auth.authenticate();
                    player_of(account.id);
                    val d = require(duel @? { .id == duel_id }, "no such duel");
                    require(not d.settled, "already settled");
                    require(op_context.last_block_time >= d.settles_at, "too early");
                    val flip = op_context.last_block_time % 2;
                    val winner_key = if (flip == 0) d.a else d.b;
                    val w = player_of(winner_key);
                    val prize = d.stake;
                    update d ( .settled = true, .stake = 0 );
                    update w ( .balance += prize );
                }
                """.trimIndent()
            ),
            "no row lookup at all - the branch picks the payee"
        )
    }

    @Test
    fun storingTheDrawnIndexAndCollectingLaterDoesNotEvadeIt() {
        // The most natural refactor of the raffle: the draw stores an integer
        // index and the winner collects in a second operation, where the
        // credited row is the CALLER'S own. Tracing the credit target finds
        // only the authenticated account - what the clock decides is the guard.
        assertRandomnessDrain(
            analyze(
                """
                module;
                import lib.ft4.auth;
                entity player { key owner: byte_array; mutable balance: integer = 0; }
                entity raffle { key id: text; closes_at: timestamp; mutable ticket_count: integer = 0; mutable pot: integer = 0; mutable winner_idx: integer = -1; }
                entity ticket { key raffle, idx: integer; holder: byte_array; }
                @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
                function player_of(owner: byte_array): player = require(player @? { .owner == owner }, "register first");
                operation draw_winner(raffle_id: text) {
                    auth.authenticate();
                    val r = require(raffle @? { .id == raffle_id }, "no such raffle");
                    require(r.winner_idx < 0, "already drawn");
                    require(op_context.last_block_time >= r.closes_at, "still open");
                    require(r.ticket_count > 0, "no tickets");
                    update r ( .winner_idx = op_context.last_block_time % r.ticket_count );
                }
                operation collect(raffle_id: text) {
                    val account = auth.authenticate();
                    val w = player_of(account.id);
                    val r = require(raffle @? { .id == raffle_id }, "no such raffle");
                    require(r.winner_idx >= 0, "not drawn");
                    val t = require(ticket @? { .raffle == r, .idx == r.winner_idx }, "missing ticket");
                    require(t.holder == account.id, "not the winner");
                    val pot = r.pot;
                    require(pot > 0, "nothing to collect");
                    update r ( .pot = 0 );
                    update w ( .balance += pot );
                }
                """.trimIndent()
            ),
            "the clock gates WHO may collect even though the credit target is the caller's own row"
        )
    }

    @Test
    fun aClockKeyedAccountingBucketStaysClean() {
        // The precision guard on the identity trigger above: a per-day bucket
        // is selected by the clock, but the payee is still the caller and no
        // identity is compared against the drawn row.
        val result = analyze(
            """
            module;
            import lib.ft4.auth;
            entity member { key owner: byte_array; mutable balance: integer = 0; }
            entity day_bucket { key day: integer; mutable total: integer = 0; }
            val DAY_MS = 86400000;
            @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
            function member_of(owner: byte_array): member = require(member @? { .owner == owner }, "register first");
            operation daily_claim() {
                val account = auth.authenticate();
                val m = member_of(account.id);
                val today = op_context.last_block_time / DAY_MS;
                val b = day_bucket @? { .day == today };
                if (b == null) { create day_bucket(day = today, total = 1); } else { update b ( .total += 1 ); }
                update m ( .balance += 10 );
            }
            """.trimIndent()
        )
        assertNoRule(
            result, "block-clock-randomness",
            "the clock names a BUCKET, not a person - the payee is the authenticated caller"
        )
    }

    @Test
    fun theClockAsABoundStaysClean() {
        // The three shapes the templates ship. Each uses the clock in an
        // inequality, which can only abort - it never chooses who is paid.
        listOf(
            "governance" to DappScaffold.files("treasury", template = "governance"),
            "vault" to DappScaffold.files("dex", template = "vault"),
            "staking" to DappScaffold.files("yield", template = "staking")
        ).forEach { (name, files) ->
            val rell = files.filterKeys { it.endsWith(".rell") }
            val result = RellSecurityCheck.analyze(rell)
            assertNoRule(
                result, "block-clock-randomness",
                "the $name template's clock is a deadline/staleness/cooldown BOUND, not a draw"
            )
            assertTrue(
                rules(result).none { it == "block-clock-randomness-test-surface" },
                "the $name template must not trip the randomness rule anywhere; got ${result.findings}"
            )
        }
    }

    @Test
    fun aTimestampedReceiptAndAnEscrowReleaseStayClean() {
        val result = analyze(
            """
            module;
            import lib.ft4.auth;
            entity member { key owner: byte_array; mutable balance: integer = 0; }
            entity escrow { key id: text; payee: byte_array; amount: integer; release_at: timestamp; }
            entity receipt { key seq: integer; owner: byte_array; at: timestamp; }
            @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
            function member_of(owner: byte_array): member = require(member @? { .owner == owner }, "register first");
            operation release(id: text, seq: integer) {
                val account = auth.authenticate();
                val m = member_of(account.id);
                val e = require(escrow @? { .id == id }, "no escrow");
                require(e.payee == account.id, "not the payee");
                require(op_context.last_block_time >= e.release_at, "not yet releasable");
                val amount = e.amount;
                delete e;
                create receipt(seq = seq, owner = account.id, at = op_context.last_block_time);
                update m ( .balance += amount );
            }
            """.trimIndent()
        )
        assertNoRule(
            result, "block-clock-randomness",
            "the clock timestamps a receipt and gates a release; the payee is the authenticated caller"
        )
    }

    // ---------- 4. the tool must not claim more than it can see ----------

    @Test
    fun theNotesNameRandomnessAndOrderingAsBlindSpots() {
        val notes = analyze(
            """
            module;
            import lib.ft4.auth;
            entity note { key owner: byte_array; body: text; }
            @extend(auth.auth_handler) function () = auth.add_auth_handler(flags = ["T"]);
            operation add(body: text) {
                val account = auth.authenticate();
                require(body.size() > 0, "empty");
                create note(owner = account.id, body = body);
            }
            """.trimIndent()
        ).notes
        listOf("UNPREDICTABLE", "TRANSACTION ORDERING", "MEV").forEach {
            assertTrue(
                notes.contains(it),
                "an agent reading a clean report must be told randomness and ordering are not covered; " +
                    "'$it' missing from: $notes"
            )
        }
    }
}
