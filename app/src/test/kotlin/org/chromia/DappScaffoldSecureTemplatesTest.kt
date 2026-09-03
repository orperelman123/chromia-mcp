package org.chromia

import org.chromia.tools.DappScaffold
import org.chromia.tools.Ft4ModuleArgs
import org.chromia.tools.RellCheck
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pins the templates that exist because the gate cannot block their exploit
 * class (north-star principle 4): `governance` (the round-1 DAO drain), `vault`
 * (the round-1 unbacked oracle mint), `staking` (the round-4 unbacked reward
 * mint), `marketplace` (the round-5 price sandwich) and `lending` (the round-6
 * just-in-time interest capture). Each must compile with the vendored FT4, pass the security check with
 * NO finding at all, ship tests that actually run green through run_rell_tests,
 * and - the proof that the bug is unwritable - ship a must-fail replay of the
 * exploit that goes RED the moment its guard is deleted (the mutation tests at
 * the bottom).
 */
class DappScaffoldSecureTemplatesTest {

    private val secureTemplates = listOf("governance", "vault", "staking", "marketplace", "lending", "streaming")

    /** The templates whose main module reads an oracle key from configuration. */
    private val oracleTemplates = setOf("vault", "lending")

    private fun rellOf(template: String): Map<String, String> =
        DappScaffold.files("treasury", template = template)
            .filterKeys { it.endsWith(".rell") }
            .mapKeys { (path, _) -> path.removePrefix("src/") }

    /**
     * Keyed on the TEMPLATE, never on a run label: the vault mutants used to be
     * run under a label like "vault-without[...]", got the plain ft4 args
     * without the oracle key, failed every case with a module_args error, and
     * so "went red" without the guard ever being exercised.
     */
    private fun moduleArgsOf(template: String) = when {
        // lending reads TWO configured keys: the oracle and the protocol fee key.
        template == "lending" -> DappScaffold.lendingTestModuleArgs()
        template in oracleTemplates -> DappScaffold.oracleTestModuleArgs()
        else -> DappScaffold.ft4TestModuleArgs()
    }

    private val dbUrl: String? = System.getenv(RunRellTests.DATABASE_URL_ENV)

    /** Rell's run_must_fail failure text when the transaction it expected to fail succeeds. */
    private val RUN_MUST_FAIL_UNEXPECTED_SUCCESS = "Transaction did not fail"
    private fun opBody(main: String, op: String): String =
        main.substringAfter("operation $op").substringBefore("\n}")

    private fun withoutComments(source: String): String =
        source.lineSequence().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")

    @Test
    fun secureTemplatesShipYmlMainAndRunnableTests() {
        secureTemplates.forEach { template ->
            val files = DappScaffold.files("treasury", template = template)
            assertEquals(
                setOf("chromia.yml", "src/main.rell", "src/test/main_test.rell"),
                files.keys,
                template
            )
            val main = files.getValue("src/main.rell")
            assertTrue(main.contains("auth.authenticate()"), "$template must authenticate")
            assertTrue(main.contains("import lib.ft4.auth;"), template)
            // Same default the ft4 template pins: the unscoped handler requires the
            // Transfer flag, and flags = [] never appears unscoped.
            val squashed = main.replace(Regex("\\s+"), "")
            assertTrue(squashed.contains("auth.add_auth_handler(flags=[\"T\"])"), "$template default handler must require T")
            assertFalse(Regex("add_auth_handler\\(flags=\\[\\]\\)").containsMatchIn(squashed), "$template: flags = [] unscoped")
            val test = files.getValue("src/test/main_test.rell")
            assertTrue(test.startsWith("@test module;"), template)
            assertTrue(test.contains("run_must_fail"), "$template tests must contain a must-fail case")
            assertTrue(Regex("function test_round\\d+_\\w+_must_fail\\(").containsMatchIn(test), "$template must ship the adversary round's exploit as a must-fail test")
            assertTrue(test.contains("assert_conserved()"), "$template must ship a conservation assertion")
            DappScaffold.forbiddenModules.forEach { banned ->
                files.forEach { (path, content) ->
                    if (path == "chromia.yml" && banned == "lib.ft4.core.admin") return@forEach
                    assertFalse(content.contains(banned), "$template/$path must not reference $banned")
                }
            }
        }
        assertEquals(listOf("hello", "ft4", "governance", "vault", "staking", "marketplace", "lending", "streaming"), DappScaffold.templates)
        assertEquals("governance", DappScaffold.toJson("dao", template = "governance").getValue("template").toString().trim('"'))
        assertEquals("vault", DappScaffold.toJson("dex", template = "vault").getValue("template").toString().trim('"'))
        assertEquals("staking", DappScaffold.toJson("yield", template = "staking").getValue("template").toString().trim('"'))
        assertEquals("marketplace", DappScaffold.toJson("bazaar", template = "marketplace").getValue("template").toString().trim('"'))
        assertEquals("lending", DappScaffold.toJson("pool", template = "lending").getValue("template").toString().trim('"'))
        val unknown = DappScaffold.toJson("x", template = "dao")
        assertEquals("hello", unknown.getValue("template").toString().trim('"'))
        assertTrue(unknown.getValue("warnings").toString().contains("governance, vault, staking"), "unknown-template warning must list the new templates")
        // The notes steer staking / rewards / vesting builders to the template and
        // say in one place how module_args are passed (the round-4 stall).
        val notes = DappScaffold.notes("yield")
        assertTrue(notes.contains("template=staking"), "notes must steer staking builders to the template")
        assertTrue(notes.contains("HOW TO PASS module_args to run_rell_tests"), "notes must say how module_args are passed")
        assertTrue(notes.contains("x\"...\" literal, as 0x..., or as bare hex"), "notes must say the yml literal is accepted")
        assertTrue(notes.contains("template=marketplace"), "notes must steer NFT / marketplace / listing builders to the template")
        assertTrue(notes.contains("AUCTIONS ARE IN THAT TEMPLATE"), "the notes must not advertise an auction the template does not ship")
        assertTrue(notes.contains("template=lending"), "notes must steer lending / credit-line / money-market builders to the template")
        assertTrue(notes.contains("NO CASH-DENOMINATED DEBT IS"), "the notes must name the guard, not just the template")
        assertTrue(notes.contains("template=streaming"), "notes must steer stream / payroll / vesting / subscription builders to the template")
        assertTrue(
            notes.contains("NO OPERATION IN IT WRITES A\nTIMESTAMP AN ENTITLEMENT IS MEASURED FROM"),
            "the notes must name the streaming guard precisely - the template DOES write paused_at, under a transition guard"
        )
        assertTrue(
            notes.contains("require(not s.paused)") && notes.contains("require(s.paused)"),
            "the notes must name the two pause guards - round 8 drained a build that had one of them"
        )
        assertFalse(
            notes.contains("you need an entry/exit fee or a minimum holding period"),
            "round 7 built exactly that and still drained - the notes must not carry advice the header retracted"
        )
        assertFalse(
            Regex("rewards or vesting").containsMatchIn(notes),
            "the notes must stop routing vesting to staking now that its own class has a template"
        )
    }

    /**
     * The unknown-template fallback must ROUTE, not just list names. Round 6's drain
     * landed in the un-templated class: `scaffold_dapp(template="lending")` answered
     * "Unknown template (valid: ...); scaffolded the 'hello' template", and the agent
     * wrote the whole value class freehand. `lending` is now a real template, so the
     * near-miss NAMES for it must route there rather than to the nearest cousin.
     */
    @Test
    fun unknownTemplateFallbackRoutesToTheClosestTemplate() {
        assertTrue(
            "lending" in DappScaffold.templates,
            "the class round 6 drained must no longer fall back to hello"
        )
        listOf("borrow", "credit_line", "money_market", "loan_pool", "debt_market").forEach { asked ->
            val warning = DappScaffold.toJson("x", template = asked).getValue("warnings").toString()
            assertTrue(warning.contains("Use `template=lending`"), "$asked must be routed to the lending template: $warning")
            assertTrue(warning.contains("NO cash-denominated debt"), "$asked must be told what makes the round-6 drain unwritable: $warning")
        }
        assertTrue(
            DappScaffold.toJson("x", template = "dao").getValue("warnings").toString().contains("template=governance"),
            "a DAO must be routed to the governance template"
        )
        assertTrue(
            DappScaffold.toJson("x", template = "oracle").getValue("warnings").toString().contains("template=vault"),
            "an oracle must be routed to the vault template"
        )
        assertTrue(
            DappScaffold.toJson("x", template = "auction").getValue("warnings").toString().contains("template=marketplace"),
            "an auction must be routed to the marketplace template, which now ships one"
        )
        // Round 7's drain landed in a class with NO template at all. Every name for it
        // must now route here, and `harvest` must still reach staking - a keyword list
        // that swallows it would be a mis-route dressed as coverage.
        listOf("stream", "payment_stream", "vesting", "payroll", "subscription", "salary_drip", "allowance").forEach { asked ->
            val warning = DappScaffold.toJson("x", template = asked).getValue("warnings").toString()
            assertTrue(warning.contains("Use `template=streaming`"), "$asked must be routed to the streaming template: $warning")
            assertTrue(warning.contains("NO OPERATION IN IT"), "$asked must be told what makes the round-7 grief unwritable: $warning")
        }
        listOf("harvest", "harvest_farm", "staking_rewards").forEach { asked ->
            assertTrue(
                DappScaffold.toJson("x", template = asked).getValue("warnings").toString().contains("template=staking"),
                "$asked must still reach the staking template"
            )
        }
        assertTrue(
            DappScaffold.toJson("x", template = "zzz_nothing_like_this").getValue("warnings").toString().contains("No shipped template covers that name"),
            "an unrecognisable name must still get the four hardened templates and the write-the-invariant-test-first advice"
        )
    }

    @Test
    fun governanceGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("dao", template = "governance").getValue("src/main.rell")
        // The window is a constant - there is no parameter a proposer could shrink
        // (the round-1 dapp took voting_period_ms and accepted 1).
        assertTrue(main.contains("val VOTING_PERIOD_MS ="), "voting window must be a named constant")
        assertFalse(Regex("operation\\s+create_proposal\\s*\\([^)]*period").containsMatchIn(main), "create_proposal must not take a period parameter")
        assertTrue(main.contains("deadline = now + VOTING_PERIOD_MS"), "the deadline must come from the constant")
        // Quorum is snapshotted at creation and checked at execution.
        assertTrue(main.contains("quorum_weight = quorum_for(dao.total_stake)"), "quorum must be snapshotted from total stake at creation")
        assertTrue(main.contains("require(p.yes_weight + p.no_weight >= p.quorum_weight, \"quorum not reached\")"))
        // Votes weigh stake, and zero stake cannot propose or vote.
        assertTrue(main.contains("val weight = voter.stake;"), "a vote must weigh the voter's stake")
        assertTrue(main.contains("require(weight > 0, \"no voting weight"), "zero stake must not be able to vote")
        assertTrue(main.contains("require(proposer.stake > 0, \"only members with stake may propose\")"))
        // Executed exactly once, flipped in the paying operation.
        val execute = opBody(main, "execute_proposal")
        assertTrue(execute.contains("require(not p.executed, \"proposal already executed\")"))
        assertTrue(execute.contains("update p ( .executed = true );"))
        assertTrue(execute.contains("dao.treasury_balance -= p.amount;"))
        // Every entity/constant the guards need exists as declared state.
        listOf("quorum_weight: integer", "mutable yes_weight", "mutable no_weight", "mutable executed", "key proposal, voter").forEach {
            assertTrue(main.contains(it), "governance entities must declare $it")
        }
    }

    @Test
    fun vaultGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("dex", template = "vault").getValue("src/main.rell")
        // The oracle is configuration, never a parameter and never a source constant.
        assertTrue(main.contains("struct module_args {\n    oracle_pubkey: pubkey;\n}"), "the oracle key must be the module_args struct's only field")
        assertTrue(main.contains("require(op_context.is_signer(chain_context.args.oracle_pubkey), \"oracle only\")"))
        assertFalse(Regex("x\"[0-9A-Fa-f]{64,}\"").containsMatchIn(main), "no key-like hex in the vault source")
        // Bounded and rate-limited posts; stale feed halts trading.
        assertTrue(main.contains("price * 10000 <= prev * (10000 + MAX_PRICE_MOVE_BPS)"))
        assertTrue(main.contains("price * 10000 >= prev * (10000 - MAX_PRICE_MOVE_BPS)"))
        assertTrue(main.contains(">= MIN_PRICE_UPDATE_INTERVAL_MS, \"price update too soon\")"))
        assertTrue(main.contains("<= MAX_PRICE_AGE_MS,\n        \"price feed is stale\""))
        assertTrue(main.contains("require(price_feed.price > 0, \"price feed not initialised\")"))
        // Every credit in a trade is paired with a reserve debit in the same operation:
        // the vault's rows are rows of the SAME entities, so a trade is a transfer.
        listOf("buy_tokens", "sell_tokens").forEach { op ->
            val body = opBody(main, op)
            val credits = Regex("update (\\w+) \\( \\.balance \\+= (\\w+) \\)").findAll(body).map { it.groupValues[2] }.toList()
            val debits = Regex("update (\\w+) \\( \\.balance -= (\\w+) \\)").findAll(body).map { it.groupValues[2] }.toList()
            assertEquals(2, credits.size, "$op must credit exactly two rows")
            assertEquals(credits.sorted(), debits.sorted(), "$op: every amount credited must be the amount debited from another row")
            assertTrue(body.contains("\"vault cannot cover the trade\""), "$op must refuse what the reserve cannot cover")
        }
        assertTrue(main.contains("function vault_id(): byte_array = chain_context.blockchain_rid;"))
        // Amounts are bounded before the multiplication (i64 overflow aborts).
        assertTrue(main.contains("require(cash_in <= MAX_TRADE_AMOUNT"))
        assertTrue(main.contains("require(tokens_in <= MAX_TRADE_AMOUNT"))
        assertTrue(main.contains("require(price > 0 and price <= MAX_PRICE"))
    }

    /**
     * The round-4 mint was `reward = staked * elapsed * REWARD_PER_SECOND` credited
     * with no pool debit. Here that formula has no line to live on: the rate only
     * ever appears inside a release capped by the pool, the pool's only inflow is
     * a sponsor's paid-for deposit, and every balance credit in every operation is
     * a debit of the same amount in the same body.
     */
    @Test
    fun stakingGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("yield", template = "staking").getValue("src/main.rell")
        val code = withoutComments(main)
        // SPONSOR-FUNDED: exactly one inflow into the pool, paid for by the caller's own balance.
        assertEquals(listOf("amount"), Regex("pool\\.undistributed \\+= (\\w+);").findAll(code).map { it.groupValues[1] }.toList(), "pool.undistributed must have exactly one inflow")
        val fund = opBody(code, "fund_rewards")
        assertTrue(fund.contains("update m ( .balance -= amount );") && fund.contains("pool.undistributed += amount;"), "fund_rewards must debit the sponsor for what it puts in the pool")
        assertTrue(fund.contains("update_pool();"), "fund_rewards must consume the clock before adding funds (no retroactive release)")
        // RELEASE-CAPPED: the clock's release is min()'d against the pool, and the pool is debited for it.
        val updatePool = code.substringAfter("function update_pool()").substringBefore("\n}")
        assertTrue(updatePool.contains("val earned = min(pool.undistributed, elapsed_ms / 1000 * REWARD_PER_SECOND);"), "the release must be capped by the pool")
        assertTrue(updatePool.contains("pool.undistributed -= earned;") && updatePool.contains("pool.unclaimed += earned;"), "a release must move points from undistributed to unclaimed")
        assertTrue(updatePool.indexOf("pool.last_update = now;") < updatePool.indexOf("if (pool.total_staked == 0 or pool.undistributed == 0) return;"), "the clock must be consumed even when nothing is released")
        // The round-4 formula is unwritable: no expression multiplies stake by the rate, and the
        // rate is used ONLY inside the capped release (declaration + update_pool + its read-only mirror).
        assertFalse(Regex("staked[^;]*\\*[^;]*REWARD_PER_SECOND|REWARD_PER_SECOND[^;]*\\*[^;]*staked").containsMatchIn(code), "no line may multiply stake by the rate")
        val cappedUses = Regex("min\\(pool\\.undistributed, elapsed_ms / 1000 \\* REWARD_PER_SECOND\\)").findAll(code).count()
        assertEquals(2, cappedUses, "update_pool and projected_acc")
        assertEquals(cappedUses + 1, Regex("\\bREWARD_PER_SECOND\\b").findAll(code).count(), "the rate constant must appear only in its declaration and the capped release")
        // PAIRED CREDIT: claim_rewards refuses what the pool cannot cover and debits it for what it pays.
        val claim = opBody(code, "claim_rewards")
        assertTrue(claim.contains("require(reward > 0, \"nothing to claim\");"))
        assertTrue(claim.contains("require(pool.unclaimed >= reward, \"pool cannot cover the claim\");"))
        assertTrue(claim.indexOf("pool.unclaimed -= reward;") in 0 until claim.indexOf(".balance += reward"), "the pool debit must precede the member credit")
        // Every operation that credits a balance debits the same amount in the same body.
        Regex("operation (\\w+)\\(").findAll(code).map { it.groupValues[1] }.forEach { op ->
            val body = opBody(code, op)
            Regex("\\.balance \\+= (\\w+)").findAll(body).map { it.groupValues[1] }.forEach { amount ->
                val debited = body.contains("-= $amount") ||
                    (op == "withdraw_unstaked" && body.contains("val $amount = r.amount;") && body.contains("delete r;"))
                assertTrue(debited, "$op credits .balance += $amount without debiting $amount in the same operation")
            }
        }
        // ACCUMULATOR: settle before the stake changes; stake bounded before the multiplication.
        val stake = opBody(code, "stake")
        assertTrue(stake.indexOf("settle(m);") in 0 until stake.indexOf(".staked += amount"), "stake must settle before the stake grows")
        assertTrue(stake.contains("require(amount <= MAX_STAKE, \"amount too large\");"))
        assertTrue(opBody(code, "request_unstake").indexOf("settle(m);") in 0 until opBody(code, "request_unstake").indexOf(".staked -= amount"))
        // COOLDOWN: a constant, not a parameter; enforced in the withdrawing operation.
        assertTrue(code.contains("val COOLDOWN_MS ="), "cooldown must be a named constant")
        assertFalse(Regex("operation\\s+request_unstake\\s*\\([^)]*(cooldown|ready)").containsMatchIn(code), "request_unstake must not take a cooldown parameter")
        assertTrue(code.contains("ready_at = op_context.last_block_time + COOLDOWN_MS"))
        assertTrue(opBody(code, "withdraw_unstaked").contains("require(op_context.last_block_time >= r.ready_at, \"cooldown not over\");"))
        // Every field the guards need exists as declared state.
        listOf("mutable undistributed: integer", "mutable unclaimed: integer", "mutable acc_reward_per_share: big_integer", "mutable reward_snapshot: big_integer", "mutable pending_reward: integer", "key member;").forEach {
            assertTrue(code.contains(it), "staking state must declare $it")
        }
        assertFalse(Regex("[0-9A-Fa-f]{64,}").containsMatchIn(main), "no key-like hex in the staking source")
    }

    /**
     * The round-5 sandwich was `buy(listing, max_price)` - a caller-supplied CEILING
     * compared against a price the seller could move. Here that shape has nowhere to
     * live: the buy compares for EQUALITY, and the listing row has no mutable field
     * and no operation that edits it, so repricing means destroying the listing.
     * The royalty bypass is the opposite kind of assertion: it is documented as open,
     * and the header is checked for saying so.
     */
    @Test
    fun marketplaceGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("bazaar", template = "marketplace").getValue("src/main.rell")
        val code = withoutComments(main)

        // EXACT PRICE: the buy names the price and it is compared for equality. No
        // ceiling exists anywhere in the code (MAX_PRICE is the upper bound on what a
        // listing may say, not a caller's slippage buffer - the match is case-sensitive).
        val buy = opBody(code, "buy_nft")
        assertTrue(buy.contains("require(l.price == expected_price,"), "buy_nft must compare the listing price for EQUALITY")
        assertFalse(Regex("max_price|min_price|slippage").containsMatchIn(code), "no caller-supplied price ceiling may exist - that is the sandwich")
        assertFalse(Regex("\\.price\\s*(<=|>=|<|>)").containsMatchIn(code), "the listing price must never be compared as a bound")

        // IMMUTABLE LISTING: no mutable field, no operation that edits a live listing.
        val listing = code.substringAfter("entity listing {").substringBefore("}")
        assertFalse(listing.contains("mutable"), "no listing field may be mutable - a mutable price re-creates the sandwich")
        assertFalse(code.contains("update_listing_price"), "repricing must be cancel + list, not an edit")
        assertFalse(Regex("update\\s+l\\s*\\(").containsMatchIn(code), "no operation may update a listing row")

        // The same guard on the other side: the escrowed amount is immutable, the
        // accept names it, and an offer expires.
        val offer = code.substringAfter("entity offer {").substringBefore("}")
        assertFalse(offer.contains("mutable"), "no offer field may be mutable - a mutable bid sandwiches the seller")
        val accept = opBody(code, "accept_offer")
        assertTrue(accept.contains("require(o.amount == expected_amount,"), "accept_offer must compare the escrowed amount for EQUALITY")
        assertTrue(accept.contains("require(op_context.last_block_time < o.expires_at, \"offer expired\");"), "an offer must expire")

        // ESCROW: the bidder is debited before the row exists, and both ways out of
        // the row delete it in the operation that pays it out.
        val makeOffer = opBody(code, "make_offer")
        assertTrue(makeOffer.indexOf("update bidder ( .balance -= amount );") in 0 until makeOffer.indexOf("create offer("), "make_offer must debit the bidder before it escrows")
        listOf("cancel_offer", "accept_offer").forEach { op ->
            assertTrue(opBody(code, op).contains("delete o;"), "$op must consume the escrow row")
        }

        // PAIRED SETTLEMENT: settle_sale is the only place a seller or creator is
        // credited, it asserts the split is exact, and both callers debit `price` in
        // the same operation - the buyer's balance, or the escrow row just deleted.
        val settle = code.substringAfter("function settle_sale(").substringBefore("\n}")
        assertTrue(settle.contains("require(royalty + proceeds == price, \"the split must pay out exactly the price\");"), "settle_sale must assert the split is exact")
        assertEquals(3, Regex("\\bsettle_sale\\(").findAll(code).count() - 1, "settle_sale must have exactly three callers")
        assertTrue(buy.contains("update buyer ( .balance -= price );"), "buy_nft must debit the buyer")
        assertTrue(buy.indexOf("delete l;") in 0 until buy.indexOf("settle_sale("), "buy_nft must consume the listing before it pays out")
        assertTrue(accept.indexOf("delete o;") in 0 until accept.indexOf("settle_sale("), "accept_offer must consume the escrow before it pays out")

        // Every operation that credits a balance debits the same amount in the same
        // body; the two sale paths do it through settle_sale, asserted above.
        Regex("operation (\\w+)\\(").findAll(code).map { it.groupValues[1] }.forEach { op ->
            val body = opBody(code, op)
            Regex("\\.balance \\+= (\\w+)").findAll(body).map { it.groupValues[1] }.forEach { amount ->
                val debited = body.contains("-= $amount") ||
                    (op == "cancel_offer" && body.contains("val $amount = o.amount;") && body.contains("delete o;")) ||
                    // place_bid's debit is the standing bid row it destroys in the same
                    // operation - an escrow released by deletion, not by a compound assignment.
                    (op == "place_bid" && body.contains("val $amount = standing.amount;") && body.contains("delete standing;"))
                assertTrue(debited, "$op credits .balance += $amount without debiting $amount in the same operation")
            }
        }

        // TIMED AUCTION: no mutable term anywhere, no operation that edits an auction
        // or a bid, raising is delete-and-recreate, and settlement is permissionless.
        val auction = code.substringAfter("entity auction {").substringBefore("}")
        val bidEntity = code.substringAfter("entity bid {").substringBefore("}")
        assertFalse(auction.contains("mutable"), "no auction field may be mutable - a movable reserve or deadline is the sandwich")
        assertFalse(bidEntity.contains("mutable"), "no bid field may be mutable - a mutable highest_bid IS the round-5 sandwich")
        assertFalse(Regex("update\\s+a\\s*\\(").containsMatchIn(code), "no operation may update an auction row")
        assertFalse(Regex("update\\s+(standing|winning)\\s*\\(").containsMatchIn(code), "no operation may update a bid row")
        val placeBid = opBody(code, "place_bid")
        assertTrue(placeBid.indexOf("delete standing;") in 0 until placeBid.indexOf("create bid("), "raising a bid must be delete-and-recreate")
        assertTrue(placeBid.indexOf("update previous ( .balance += refund );") in 0 until placeBid.indexOf("update bidder ( .balance -= amount );"), "the outbid escrow must be refunded in the operation the new one is taken")
        assertTrue(opBody(code, "cancel_auction").contains("require(bid @? { .auction == a } == null, \"auction has a bid\");"), "a seller may not cancel out from under a standing bid")
        val settleAuction = opBody(code, "settle_auction")
        assertTrue(settleAuction.contains("require(op_context.last_block_time >= a.ends_at, \"auction has not ended\");"), "the deadline is a term, not a suggestion")
        assertFalse(settleAuction.contains("a.seller == account.id"), "settle_auction must be permissionless - a seller who walks away must not be able to strand the escrow")
        assertTrue(settleAuction.indexOf("delete winning;") in 0 until settleAuction.indexOf("settle_sale("), "settle_auction must consume the bid escrow before it pays out")

        // ONE ENCUMBRANCE HELPER, consulted by every path that moves a token.
        assertTrue(code.contains("function require_unencumbered(token: nft) {"), "the encumbrance question must live in one helper")
        listOf("buy_nft", "transfer_nft", "accept_offer", "list_nft", "start_auction").forEach { op ->
            assertTrue(opBody(code, op).contains("require_unencumbered(token);"), "$op moves a token or opens a market state and must consult require_unencumbered")
        }

        // CONSERVATION: every row that holds points is summed.
        val circulation = code.substringAfter("query points_in_circulation()").substringBefore("\n}")
        listOf("member", "offer", "bid").forEach { row ->
            assertTrue(circulation.contains("in $row @* {}"), "points_in_circulation must sum the $row rows that hold points")
        }

        // The seams a static rule cannot see are written down where an extender reads.
        assertTrue(main.contains("EXTENDING THIS TEMPLATE"), "the header must tell an extender which invariants are theirs to keep")
        listOf("MUTUALLY EXCLUSIVE", "ENCUMBRANCE HELPER", "CONSERVATION TOTAL").forEach {
            assertTrue(main.contains(it), "the EXTENDING section must name the '$it' seam")
        }

        // ROYALTY: fixed at mint, capped, never rounded away - and the header says
        // plainly that the off-market bypass is open rather than implying a guard.
        assertFalse(code.contains("mutable royalty_bps"), "a raisable royalty front-runs a pending sale")
        assertFalse(Regex("update\\s+\\w+\\s*\\(\\s*\\.royalty_bps").containsMatchIn(code), "no operation may write a royalty after mint")
        assertTrue(code.contains("require(royalty_bps <= MAX_ROYALTY_BPS, \"royalty too high\");"))
        assertTrue(code.contains("val royalty = if (exact > 0) exact else 1;"), "a recorded sale must never round the royalty away to zero")
        assertTrue(main.contains("AN HONEST BOUNDARY, NOT A GUARD"), "the royalty header must not imply enforcement")
        assertTrue(main.contains("It is NOT enforced on the trade, and no template can"), "the header must say plainly that the bypass is open")
        assertFalse(Regex("[0-9A-Fa-f]{64,}").containsMatchIn(main), "no key-like hex in the marketplace source")
    }

    /**
     * The round-6 drain was `pool_value()` - a cash-denominated debt counter refreshed
     * only on the paths a BORROWER signs - pricing a lender share. Here that counter
     * has nowhere to live: no entity, object or local holds a cash debt across
     * operations; the cash figures exist only inside a `pool_state`; `pool_now()` is
     * the only function that builds one; and every pricing helper TAKES one, so an
     * operation cannot price an entry or an exit without a state built this block.
     */
    @Test
    fun lendingGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("pool", template = "lending").getValue("src/main.rell")
        val code = withoutComments(main)

        // NO CASH-DENOMINATED DEBT IS STORED. The position and the pool both carry
        // index units, and the names that went stale in round 6 do not exist.
        val loan = code.substringAfter("entity loan {").substringBefore("}")
        assertTrue(loan.contains("mutable scaled_debt: integer"), "a position must record its debt in index units")
        assertFalse(loan.contains("principal"), "a cash principal is the field that goes stale")
        assertFalse(loan.contains("accrued_at"), "a per-loan accrual stamp is the other half of the round-6 bug")
        val poolObj = code.substringAfter("object pool {").substringBefore("}")
        assertTrue(poolObj.contains("mutable total_scaled_debt: integer"), "the pool must record debt in the same units as the positions")
        assertFalse(poolObj.contains("total_debt"), "a cash debt counter on the pool is exactly what round 6 read stale")
        assertFalse(code.contains("pool_value()"), "there is no standalone share price to read - it lives in a pool_state")

        // ONE PRODUCER. pool_state is constructed in exactly one place, and that
        // place reads the block clock.
        assertEquals(
            1,
            Regex("pool_state\\(\\s*\\n?\\s*debt_index").findAll(code).count(),
            "pool_state must be built in exactly one place"
        )
        val poolNow = code.substringAfter("function pool_now(): pool_state {").substringBefore("\n}")
        assertTrue(poolNow.contains("val now_index = current_index();"), "pool_now must take the index from the clock")
        assertTrue(poolNow.contains("to_cash_down(pool.total_scaled_debt, now_index, MAX_POOL_DEBT)"), "pool_now must convert the pool's units to cash itself")
        // THE INDEX IS CHECKPOINTED, AND THERE IS EXACTLY ONE PRODUCER OF IT. Round 8
        // drained a build whose index multiplied the rate NOW by the WHOLE elapsed
        // span, so a utilisation curve re-priced every past second on every deposit,
        // borrow, repay and withdrawal. The checkpoint is the sanctioned exception to
        // "never store a snapshot", and what makes it not the round-6 bug is that the
        // only function that READS it WRITES it first.
        assertTrue(code.contains("function current_index(): integer {"), "the index must exist as one function")
        assertTrue(
            code.contains("val elapsed = op_context.last_block_time - pool.last_accrual_at;"),
            "the checkpoint must accrue from the last accrual block, not from the pool's opening"
        )
        assertFalse(
            code.contains("op_context.last_block_time - pool.opened_at"),
            "an index measured from the pool's opening re-prices history whenever the rate moves - round 8"
        )
        assertEquals(
            1,
            Regex("function current_index\\(").findAll(code).count() +
                Regex("function accrue_to_now\\(").findAll(code).count() - 1,
            "exactly one function may produce an index, and exactly one may advance the checkpoint"
        )
        assertEquals(1, Regex("\\bcurrent_index\\(\\)").findAll(code).count() - 1, "current_index must have exactly one caller - pool_now")
        assertEquals(1, Regex("\\baccrue_to_now\\(\\)").findAll(code).count() - 1, "accrue_to_now must have exactly one caller - pool_now")
        assertEquals(
            1,
            Regex("pool\\.rate_ms_accrued\\s*\\+?=").findAll(code).count(),
            "the accumulator is written in exactly one place, and only ever gains"
        )
        assertTrue(
            code.contains("pool.rate_ms_accrued += current_rate_bps_per_year().to_big_integer() * elapsed.to_big_integer();"),
            "the accumulator sums rate * interval BEFORE any division, so a flat rate is bit-identical to the old form"
        )
        // The checkpoint is advanced BEFORE anything is derived from it. Reversed, the
        // interval just ended would accrue at the rate the operation is about to create.
        assertTrue(
            poolNow.indexOf("accrue_to_now();") in 0 until poolNow.indexOf("val now_index = current_index();"),
            "pool_now must advance the checkpoint before it reads the index"
        )
        // The rate is a named seam, so a curve has exactly one place to go.
        assertTrue(code.contains("function current_rate_bps_per_year(): integer"), "the rate must be a named function - that is where a curve goes")
        assertEquals(
            1,
            Regex("\\bINTEREST_RATE_BPS_PER_YEAR\\b").findAll(code).count() - 1,
            "the rate constant may be read only by current_rate_bps_per_year - anything else bypasses the curve seam"
        )
        // The anchor is written once, by pool_now, and by nothing else.
        assertEquals(1, Regex("pool\\.opened_at\\s*=").findAll(code).count(), "opened_at must be written in exactly one place")
        assertTrue(poolNow.contains("pool.opened_at = op_context.last_block_time;"), "and that place is pool_now")

        // THE RECOVERABLE BOUND IS PER POSITION. Pool-wide, a stranger with no debt
        // could add collateral, raise the share price, exit at it and take the
        // collateral back next block - round 8's free lever, which this header used to
        // call an accounting imprecision.
        val recoverable = code.substringAfter("function recoverable_debt(").substringBefore("\n}")
        assertTrue(
            recoverable.contains("for (l in loan @* { .scaled_debt > 0 } ( .scaled_debt, .collateral ))"),
            "the bound must be taken over positions that owe something, so a debt-free row contributes nothing"
        )
        assertTrue(recoverable.contains("total += if (backing < face) backing else face;"), "sum(min(face, backing)), never min(sum, sum)")
        assertFalse(
            recoverable.contains("pool.total_collateral"),
            "a pool-wide aggregate is exactly the lever: it counts collateral behind no debt"
        )

        // EVERY PRICING HELPER TAKES A pool_state - so it cannot be asked about a
        // stale number, and a new operation cannot price without calling pool_now().
        listOf(
            "function debt_of(l: loan, st: pool_state)",
            "function shares_for(cash: integer, st: pool_state)",
            "function cash_for(shares: integer, st: pool_state)",
            "function payment_for(l: loan, offered: integer, st: pool_state)",
            "function is_liquidatable(l: loan, st: pool_state, price: integer)"
        ).forEach { assertTrue(code.contains(it), "the pricing helper must take a pool_state: $it") }
        listOf("deposit_cash", "withdraw_cash", "borrow", "repay", "remove_collateral", "liquidate").forEach { op ->
            assertTrue(opBody(code, op).contains("val st = pool_now();"), "$op reads or writes the share price and must price through pool_now()")
        }

        // A PAYMENT IS PRICED IN BOTH DIRECTIONS from the same state, so it can never
        // retire more debt than it paid for (the rounding drain) nor charge more than
        // was offered, and offering the whole debt clears the position.
        val paymentFor = code.substringAfter("function payment_for(").substringBefore("\n}")
        assertTrue(paymentFor.contains("min(l.scaled_debt, to_scaled_down(offered, st.debt_index))"), "a payment retires only the units it covers")
        assertTrue(paymentFor.contains("to_cash_up(scaled, st.debt_index, MAX_DEBT)"), "and is charged the rounded-up price of exactly those")
        listOf("repay", "liquidate").forEach { op ->
            assertTrue(opBody(code, op).contains("val p = payment_for(l, "), "$op must price its payment through payment_for")
        }
        // The borrow records at least what left the pool.
        assertTrue(opBody(code, "borrow").contains("val added = to_scaled_up(amount, st.debt_index);"))

        // THE FIVE REFUSALS round 6's build already had, kept.
        assertTrue(code.contains("require(owed + amount <= borrow_limit, \"over the borrow limit\");"))
        assertTrue(code.contains("require(owed * BPS <= remaining * MAX_LTV_BPS, \"that would put the position under water\");"))
        assertTrue(code.contains("require(is_liquidatable(l, st, price), \"position is healthy\");"))
        assertTrue(code.contains("require(repay_amount <= max_close, \"over the close factor\");"))
        assertTrue(code.contains("require(borrower != acc.id, \"cannot liquidate your own position\");"))
        assertTrue(code.contains("require(seize <= l.collateral, \"not enough collateral to cover the bonus\");"))
        assertTrue(code.contains("require(st.shares > 0 or amount >= MIN_INITIAL_DEPOSIT,"), "the one-unit seed must be refused outright")
        assertTrue(code.contains("require(minted > 0, \"deposit too small to mint a share\");"), "a deposit that mints nothing must abort, not be swallowed")
        assertTrue(code.contains("require(pool.cash_available >= amount, \"pool is illiquid - wait for repayments\");"), "only cash the pool holds may leave it")
        // The vault's oracle, unchanged in substance: configured key, bounded move,
        // rate limit, staleness halt - and no key material in the source.
        assertTrue(code.contains("require(op_context.is_signer(chain_context.args.oracle_pubkey), \"not the oracle\");"))
        assertTrue(code.contains("require(move * BPS <= previous * MAX_PRICE_MOVE_BPS, \"price move too large\");"))
        assertTrue(code.contains(">= MIN_PRICE_UPDATE_INTERVAL_MS,\n            \"price posted too soon\""))
        assertTrue(code.contains("require(op_context.last_block_time - price_feed.updated_at <= MAX_PRICE_AGE_MS, \"price is stale\");"))
        assertTrue(
            code.contains("struct module_args {\n    oracle_pubkey: pubkey;\n    treasury_pubkey: pubkey;\n}"),
            "the two configured keys must be the module_args struct's only fields"
        )
        assertFalse(Regex("x\"[0-9A-Fa-f]{64,}\"").containsMatchIn(main), "no key-like hex in the lending source")
        // Every ratio is a named constant, never a parameter a caller could widen.
        listOf("MAX_LTV_BPS", "LIQUIDATION_THRESHOLD_BPS", "LIQUIDATION_BONUS_BPS", "CLOSE_FACTOR_BPS", "MIN_INITIAL_DEPOSIT", "INTEREST_RATE_BPS_PER_YEAR").forEach {
            assertTrue(code.contains("val $it ="), "$it must be a named constant")
            assertFalse(Regex("operation\\s+\\w+\\s*\\([^)]*\\b$it\\b").containsMatchIn(code), "$it must not be a caller-supplied parameter")
        }

        // Every operation that credits cash or collateral debits the same amount in
        // the same body: nothing here creates value outside the welcome grant.
        Regex("operation (\\w+)\\(").findAll(code).map { it.groupValues[1] }.forEach { op ->
            val body = opBody(code, op)
            Regex("\\.(?:cash|tokens|collateral) \\+= (\\w+)").findAll(body).map { it.groupValues[1] }.forEach { amount ->
                assertTrue(body.contains("-= $amount"), "$op credits += $amount without debiting $amount in the same operation")
            }
        }

        // ROUND 7, DEFECT G3: BAD DEBT IS PRICED. The pool's debt is worth what the
        // collateral behind it can repay, and that cap lives inside pool_now() - not in
        // an operation somebody has to remember to call - so the exit ORDER cannot
        // decide who eats the loss. Shipped exactly as it was, this template handed
        // 13920 of 14000 to whoever withdrew first.
        assertTrue(
            code.contains("total += if (backing < face) backing else face;"),
            "the pool must value EACH debt at what THAT position`s collateral can repay - " +
                "a pool-wide min() lets one surplus-collateralised loan mask another that is underwater"
        )
        assertTrue(poolNow.contains("val debt = recoverable_debt(now_index, price);"), "and pool_now() must be the place that does it")
        assertTrue(poolNow.contains("val price = if (face > 0) fresh_price() else 0;"), "pricing a share means pricing the collateral behind the pool's debt")
        assertEquals(
            2,
            Regex("recoverable_debt\\(").findAll(code).count(),
            "recoverable_debt must be defined once and called from exactly one place - pool_now()"
        )
        // The aggregate that cap reads is maintained in the same operation as every
        // position it sums, and an invariant query compares the two.
        assertTrue(code.contains("mutable total_collateral: integer"), "the pool must carry the collateral aggregate the cap reads")
        listOf("add_collateral", "remove_collateral", "liquidate").forEach { op ->
            assertTrue(opBody(code, op).contains("pool.total_collateral"), "$op moves collateral and must move the aggregate in the same body")
        }
        assertTrue(code.contains("query collateral_matches_positions(): boolean {"), "the collateral aggregate needs its own conservation query")

        // ROUND 7, DEFECT G1: A STEP IN POOL VALUE IS NETTED OUT OF pool_state, NOT
        // TOLLED. The protocol fee accrues on the interest still OUTSTANDING as well as
        // on the interest already paid, so a repayment moves no pool value and there is
        // no block to straddle - and so there is no holding period and no exit fee
        // anywhere in this template.
        assertTrue(code.contains("val interest = pool.interest_realised + max(0, outstanding);"), "the fee must accrue on outstanding interest, not only on paid interest")
        assertTrue(poolNow.contains("val fee = accrued_fee(pool.total_scaled_debt, debt);"), "pool_now must net the accrued fee out of the pool's value")
        assertTrue(poolNow.contains("val net = pool.cash_available + debt - fee;"), "the fee is not part of what a share is a claim on")
        listOf("repay", "liquidate").forEach { op ->
            assertTrue(opBody(code, op).contains("record_interest(p);"), "$op realises interest and must bank it so the fee does not evaporate")
        }
        assertFalse(code.contains("HOLDING_PERIOD"), "a holding period is the mitigation round 7 proved does not bind an exit")
        assertFalse(Regex("EXIT_FEE|WITHDRAW_FEE").containsMatchIn(code), "an exit fee is sized by the attacker, who chose the position it is charged on")
        // Collecting the fee is value-neutral by construction: cash out and
        // fee_collected up, by the same amount, in the same body.
        val collect = opBody(code, "collect_fees")
        assertTrue(
            collect.contains("pool.cash_available -= amount;") && collect.contains("pool.fee_collected += amount;"),
            "a collection must move cash and the collected counter by the same amount"
        )
        assertTrue(collect.contains("chain_context.args.treasury_pubkey"), "only the configured protocol key may collect")

        // ROUND 7, DEFECT G5: the two saturation ceilings are DERIVED from one number,
        // so the pool cannot value debt above what a borrower will actually be charged.
        assertTrue(code.contains("val MAX_DEBT = MAX_AMOUNT * MAX_INDEX_GROWTH;"), "a position's ceiling must be the largest borrow grown by the index's ceiling")
        assertTrue(code.contains("val MAX_POOL_DEBT = MAX_DEBT * MAX_POSITIONS_PRICED;"), "the aggregate ceiling must be derived from the per-position one")

        // The seam a static rule cannot see is written down where an extender reads.
        assertTrue(main.contains("EXTENDING THIS TEMPLATE"), "the header must tell an extender which invariants are theirs to keep")
        listOf("PRICE THROUGH", "MOVES POOL VALUE IN A STEP", "cash_in_circulation()", "collateral_matches_positions()").forEach {
            assertTrue(main.contains(it), "the EXTENDING section must name the '$it' seam")
        }
        // ROUND 7, DEFECT G1: the prescription that did not work is gone, and the rule
        // that does is in its place - stated as the fix, not as an aside.
        assertFalse(
            main.contains("you need an entry/exit\n        //      fee or a minimum holding period"),
            "the header must not prescribe a mitigation round 7 implemented faithfully and still drained through"
        )
        assertTrue(
            main.contains("MUST BE NETTED OUT OF pool_state SO IT") &&
                main.contains("A toll on the round trip does not stop an exit."),
            "the header must name the structural rule that replaces it"
        )
        // ROUND 7, DEFECT G3: the residual list is where an auditor trusts this file, so
        // the inverted claim is corrected there rather than quietly dropped.
        assertFalse(
            main.contains("the loss sits in the share price"),
            "the inverted bad-debt claim must be gone"
        )
        assertTrue(main.contains("IT DID NOT, and the inversion cost an"), "and the correction must say what was wrong")
        // ROUND 7, DEFECT G2: three shipped tests go red if an extender gates a round
        // trip, and the header names which, and how to adapt them.
        listOf(
            "A SEAM-2 MITIGATION WILL TURN SHIPPED TESTS RED",
            "test_round6_jit_interest_capture_must_fail - the attacker's one-block",
            "test_first_depositor_inflation_refuses_instead_of_swallowing - the",
            "test_interest_moves_only_from_borrower_to_lender - a fee changes the",
            "ADAPTED FOR THE EXTENSION"
        ).forEach {
            assertTrue(main.contains(it), "the header must name the tests a seam-2 mitigation invalidates: $it")
        }
        assertTrue(main.contains("WHAT NO TEMPLATE CAN FIX"), "the header must state the limits it does not close")
    }

    /**
     * The round-7 grief was `due(s)` measured from `anchor_at`, a MUTABLE timestamp
     * that every settle advanced - so a stranger who chose the cadence chose how much
     * of the payee's entitlement simply ceased to exist. Here that shape has nowhere
     * to live: there is exactly ONE assignment to a timestamp field in the whole
     * module and it is the `create`, every term of the deal is declared without
     * `mutable`, and the entitlement is a function of those terms and a clock it is
     * handed. The other half of that drain - the payer closing the stream and keeping
     * 100% - is refused by the ORDER of two lines, which is asserted here and replayed
     * in the shipped tests.
     */
    @Test
    fun streamingGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("payroll", template = "streaming").getValue("src/main.rell")
        val code = withoutComments(main)

        // THE BLOCK CLOCK IS WRITTEN IN EXACTLY TWO PLACES, and each is pinned with
        // what makes it safe. `started_at` is the create - the round-7 line
        // `update s ( .anchor_at = ... )` still has nowhere to live. `paused_at` is
        // the transition INTO a pause, and round 8 is why the rule is no longer
        // "never a mutable timestamp": a stored timestamp is safe exactly when it is
        // written only on a state transition that can happen once per state, which is
        // require(not s.paused) below. A THIRD assignment is the edit to refuse.
        assertEquals(
            listOf("started_at = op_context.last_block_time", ".paused_at = op_context.last_block_time"),
            // The resume READS the clock in `val frozen = op_context.last_block_time - ...`
            // and that must not count as a write. `(?<!val )` alone does not do it:
            // `[\w.]+` backtracks and restarts one character later, so `frozen` was
            // rejected and `rozen` matched - the assertion reported a third clock
            // write that does not exist. `(?<![\w.])` pins the match to a real token
            // start, so the `val` exclusion cannot be stepped around.
            Regex("(?<![\\w.])(?<!val )[\\w.]+\\s*=\\s*op_context\\.last_block_time").findAll(code).map { it.value }.toList(),
            "the block clock may be READ anywhere but written only by the create and by the pause transition"
        )

        // EVERY TERM OF THE DEAL IS IMMUTABLE, and the only mutable fields are the
        // monotone ones. A mutable payee is the same drain without even needing timing.
        val entity = code.substringAfter("entity stream {").substringBefore("\n}")
        listOf("payer", "payee", "rate_per_hour", "started_at", "funded", "cancellable").forEach { term ->
            assertTrue(Regex("(^|\\n)\\s*(index\\s+)?$term:").containsMatchIn(entity), "the stream must declare $term")
            assertFalse(Regex("mutable\\s+$term\\b").containsMatchIn(entity), "$term is a term of the deal and must not be mutable")
            assertFalse(Regex("\\.$term\\s*=(?!=)").containsMatchIn(code), "no operation may write $term after creation")
        }
        assertEquals(
            setOf("released", "escrow", "refunded", "closed", "paused", "paused_at", "paused_ms"),
            Regex("mutable\\s+(\\w+)").findAll(entity).map { it.groupValues[1] }.toSet(),
            "a new mutable field on the stream is the one edit to this template to refuse"
        )

        // THE PAUSE PAIR IS A STATE MACHINE, and that - not the monotonicity of
        // paused_ms - is what keeps ACTIVE ELAPSED non-decreasing. Round 8 drained a
        // build whose paused_ms was provably monotone. Each transition is written in
        // exactly one place and gated on being in the other state.
        assertEquals(
            listOf(".paused = true"),
            Regex("\\.paused\\s*=\\s*true").findAll(code).map { it.value }.toList(),
            "a stream enters the paused state in exactly one place"
        )
        assertEquals(
            listOf(".paused = false"),
            Regex("\\.paused\\s*=\\s*false").findAll(code).map { it.value }.toList(),
            "a stream leaves the paused state in exactly one place"
        )
        assertEquals(
            listOf(".paused_ms = s.paused_ms + frozen"),
            Regex("\\.paused_ms\\s*=[^,)]*").findAll(code).map { it.value.trim() }.toList(),
            "paused_ms is written once, by the resume, and only ever gains the interval that was frozen"
        )

        // MONOTONE: `released` and `refunded` only ever rise, `escrow` only ever falls
        // (or is zeroed by the terminal cancellation), and `closed` is written once.
        assertEquals(1, Regex("\\.released \\+= ").findAll(code).count(), "released must be incremented in exactly one place")
        assertFalse(Regex("\\.released\\s*=(?!=)").containsMatchIn(code), "released must never be assigned - only incremented")
        assertEquals(1, Regex("\\.escrow -= ").findAll(code).count(), "the escrow must be debited in exactly one place")
        assertEquals(
            listOf(".escrow = 0"),
            Regex("\\.escrow\\s*=(?!=)[^,)]*").findAll(code).map { it.value.trim() }.toList(),
            "the only assignment to the escrow is the terminal zeroing in cancel_stream"
        )
        assertEquals(1, Regex("\\.refunded\\s*=(?!=)").findAll(code).count(), "the refund is recorded in exactly one place")
        assertEquals(1, Regex("\\.closed = true").findAll(code).count(), "a stream is closed in exactly one place")

        // THE ENTITLEMENT IS A PURE FUNCTION. earned_by reads no mutable field, it is
        // defined once and called from exactly two places, and no operation can hand it
        // anything but the block clock because no operation takes a timestamp.
        val earnedBy = code.substringAfter("function earned_by(s: stream, at: timestamp): integer {").substringBefore("\n}")
        val activeElapsed = code.substringAfter("function active_elapsed(s: stream, at: timestamp): integer {").substringBefore("\n}")
        listOf(".released", ".escrow", ".refunded", ".closed").forEach {
            assertFalse(earnedBy.contains(it), "the entitlement must not depend on the bookkeeping field $it")
            assertFalse(activeElapsed.contains(it), "active elapsed time must not depend on the bookkeeping field $it")
        }
        // The ONLY mutable state the entitlement may read is the pause trio, and it
        // reads it through this one function, which measures from the IMMUTABLE start.
        assertTrue(earnedBy.contains("val raw = active_elapsed(s, at);"), "the entitlement is priced off ACTIVE elapsed time")
        assertTrue(activeElapsed.contains("val raw = at - s.started_at;"), "active elapsed must be measured from the IMMUTABLE start")
        assertTrue(
            activeElapsed.contains("val open_pause = if (s.paused) at - s.paused_at else 0;") &&
                activeElapsed.contains("return raw - s.paused_ms - open_pause;"),
            "an OPEN pause must be subtracted as it runs, or the entitlement jumps at the resume block"
        )
        assertEquals(2, Regex("\\bactive_elapsed\\(").findAll(code).count(), "active_elapsed is defined once and called only by earned_by")
        assertEquals(3, Regex("\\bearned_by\\(").findAll(code).count(), "earned_by must be defined once and called from exactly two places")
        assertFalse(Regex("operation\\s+\\w+\\s*\\([^)]*timestamp").containsMatchIn(code), "no operation may take a timestamp - that is the anchor by another route")
        assertTrue(code.contains("function owed(s: stream): integer = payable_at(s, op_context.last_block_time);"))
        assertTrue(
            code.contains("val outstanding = earned_by(s, at) - s.released;"),
            "what is payable is the entitlement less the MONOTONE released total, and nothing else"
        )

        // PAIRED PAYOUT: one place credits a payee, and it debits the escrow and
        // records the release in the same statement. Paying zero writes NOTHING.
        val payOut = code.substringAfter("function pay_out(s: stream) {").substringBefore("\n}")
        assertTrue(payOut.contains("if (amount <= 0) return;"), "a zero payout must write nothing at all - that is the whole round-7 bug")
        assertTrue(
            payOut.indexOf("update s ( .escrow -= amount, .released += amount );") in
                0 until payOut.indexOf("update payee_account ( .balance += amount );"),
            "the escrow debit and the release must be recorded with the credit"
        )
        assertEquals(3, Regex("\\bpay_out\\(").findAll(code).count() - 1, "pay_out must have exactly three callers - settle, cancel_stream and pause_stream")

        // PREPAID: the payer is debited for the whole escrow before the row exists.
        val open = opBody(code, "open_stream")
        assertTrue(
            open.indexOf("update me ( .balance -= amount );") in 0 until open.indexOf("create stream("),
            "a stream must be funded before it exists - it can never promise more than it holds"
        )
        assertTrue(open.contains("funded = amount,") && open.contains("escrow = amount"), "funded and escrow both start at what was paid in")
        assertTrue(open.contains("require(payee != acc.id, \"cannot stream to yourself\");"))
        assertTrue(open.contains("\"the payee must register an account first\""), "a payout must never be blocked by a payee row that is not there")

        // PERMISSIONLESS SETTLEMENT, deliberately: it pays the stream's own payee out
        // of the stream's own escrow, so there is nothing for a stranger to take.
        val settle = opBody(code, "settle")
        assertFalse(settle.contains("acc.id"), "settle must be permissionless - a payee who must be online to be paid can be starved")
        assertTrue(settle.contains("require(not s.closed, \"stream is closed\");"))
        assertTrue(settle.contains("pay_out(s);"))

        // CANCELLATION: restricted to the two parties, refused outright on a committed
        // grant, terminal, and it PAYS BEFORE IT REFUNDS - the order is the guard.
        val cancel = opBody(code, "cancel_stream")
        assertTrue(cancel.contains("require(acc.id == s.payer or acc.id == s.payee, \"only the payer or the payee may cancel\");"))
        assertTrue(cancel.contains("require(s.cancellable, \"this stream is not cancellable\");"))
        assertTrue(
            cancel.indexOf("pay_out(s);") in 0 until cancel.indexOf("val refund = s.escrow;"),
            "cancel_stream must pay the payee everything accrued BEFORE it computes the payer's refund"
        )
        assertTrue(cancel.contains("update s ( .escrow = 0, .refunded = refund, .closed = true );"))

        // PAUSE/RESUME. Round 8 drained two builds through this seam - one missing
        // require(s.paused), one whose pause never looked at `cancellable`. All three
        // guards are pinned here because all three are one line each.
        val pause = opBody(code, "pause_stream")
        assertTrue(pause.contains("require(acc.id == s.payer or acc.id == s.payee, \"only the payer or the payee may pause\");"))
        assertTrue(
            pause.contains("require(s.cancellable, \"a committed grant cannot be paused\");"),
            "a pause that ignores `cancellable` voids the guarantee the header sells - round 8, dapp_a3"
        )
        assertTrue(
            pause.contains("require(not s.paused, \"stream is already paused\");"),
            "without this a second pause moves paused_at forward and a frozen stream keeps accruing"
        )
        assertTrue(
            pause.indexOf("pay_out(s);") in 0 until pause.indexOf("update s ( .paused = true"),
            "the pause must pay what the clock owed before it stops the clock"
        )
        val resume = opBody(code, "resume_stream")
        assertTrue(resume.contains("require(acc.id == s.payer or acc.id == s.payee, \"only the payer or the payee may resume\");"))
        assertTrue(
            resume.contains("require(s.paused, \"stream is not paused\");"),
            "THIS is round 8's drain: without it a resume of a running stream claws back earned income"
        )
        assertTrue(
            resume.indexOf("require(s.paused,") in 0 until resume.indexOf("val frozen = op_context.last_block_time - s.paused_at;"),
            "the frozen interval may only be computed once the stream is known to be paused"
        )

        // Every operation that credits a balance debits the same amount in the same
        // body; the refund's debit is the escrow it zeroes in that same statement.
        Regex("operation (\\w+)\\(").findAll(code).map { it.groupValues[1] }.forEach { op ->
            val body = opBody(code, op)
            assertFalse(body.contains("earned_by("), "$op must price through owed()/payable_at, never build an entitlement itself")
            Regex("\\.balance \\+= (\\w+)").findAll(body).map { it.groupValues[1] }.forEach { amount ->
                val debited = body.contains("-= $amount") ||
                    (op == "cancel_stream" && body.contains("val $amount = s.escrow;") && body.contains(".escrow = 0"))
                assertTrue(debited, "$op credits .balance += $amount without debiting $amount in the same operation")
            }
        }

        // CONSERVATION: every row that holds points is summed, and the sealed ledger
        // says every point a stream held is with the payee, escrowed, or back home.
        val circulation = code.substringAfter("query points_in_circulation(): integer {").substringBefore("\n}")
        listOf("account", "stream").forEach { row ->
            assertTrue(circulation.contains("in $row @* {}"), "points_in_circulation must sum the $row rows that hold points")
        }
        assertTrue(code.contains("query stream_ledger_balances(): boolean {"))
        assertTrue(code.contains("if (s.funded != s.released + s.escrow + s.refunded) return false;"))

        // Every bound is a named constant, never a parameter a caller could widen.
        listOf("WELCOME_POINTS", "HOUR_MS", "MAX_AMOUNT", "MAX_RATE_PER_HOUR").forEach {
            assertTrue(code.contains("val $it ="), "$it must be a named constant")
            assertFalse(Regex("operation\\s+\\w+\\s*\\([^)]*\\b$it\\b").containsMatchIn(code), "$it must not be a caller-supplied parameter")
        }

        // The seams a static rule cannot see are written down where an extender reads.
        assertTrue(main.contains("EXTENDING THIS TEMPLATE"), "the header must tell an extender which invariants are theirs to keep")
        listOf(
            "NEVER ADD A MUTABLE TIMESTAMP",
            "EVERY NEW TERM MUST BE IMMUTABLE",
            "A CLIFF, IF YOU ADD ONE",
            "points_in_circulation()",
            "stream_ledger_balances()"
        ).forEach {
            assertTrue(main.contains(it), "the EXTENDING section must name the '$it' seam")
        }
        // A pause/resume feature is the anchor wearing a feature's clothes, and round 8
        // proved that naming it is not enough: two builds walked into it from a seam that
        // described the shape and left the guards to the reader. It is now SHIPPED, so
        // that is what gets pinned - a later edit that demotes it back to prose is the
        // regression this assertion exists to catch.
        assertTrue(main.contains("PAUSE/RESUME IS SHIPPED IN THIS TEMPLATE"), "the pause/resume seam must be shipped, not described")

        // The residual list is where an auditor trusts most, so it must state limits
        // rather than imply guards - round 7 drained a build through an inverted one.
        assertTrue(main.contains("WHAT THIS TEMPLATE DOES NOT SOLVE"), "the header must state the limits it does not close")
        listOf(
            "A CANCELLABLE STREAM IS NOT AN INCOME GUARANTEE",
            "SETTLEMENT COSTS THE CALLER A TRANSACTION FEE AND PAYS THEM NOTHING",
            "THE ESCROW IS IDLE",
            "A STREAM IS PUBLIC"
        ).forEach {
            assertTrue(main.contains(it), "the residual list must state '$it'")
        }
        assertFalse(Regex("[0-9A-Fa-f]{64,}").containsMatchIn(main), "no key-like hex in the streaming source")
    }

    @Test
    fun templatesCompileWithVendoredLib() {
        secureTemplates.forEach { template ->
            val main = DappScaffold.files("t", template = template).getValue("src/main.rell")
            val compile = RellCheck.check(mapOf("main.rell" to main), null)
            assertTrue(compile.ok, "$template template must compile with vendored lib: ${compile.errors}")
        }
    }

    /**
     * These templates exist to satisfy the economic advisories, so the bar is
     * higher than the ft4 template's: not one finding of ANY severity on the
     * template's own code, MEDIUM included. If an advisory ever fires here on
     * correct code, that is a false positive in the rule to fix - never
     * something to paper over in the template.
     */
    @Test
    fun templatesSecurityPassHasNoFindingsAtAll() {
        secureTemplates.forEach { template ->
            val files = DappScaffold.files("t", template = template)
            val result = RellSecurityCheck.analyze(
                mapOf(
                    "main.rell" to files.getValue("src/main.rell"),
                    "test/main_test.rell" to files.getValue("src/test/main_test.rell")
                )
            )
            println("[$template] gate ok=${result.ok} findings=${result.findings} notes=${result.notes}")
            assertTrue(result.ok, "$template: ${result.findings}")
            assertEquals(emptyList<RellSecurityCheck.Finding>(), result.findings, "$template must produce no advisory either")
        }
    }

    /**
     * The ft4 template's key discipline, applied to every secure yml: every 64+
     * hex run is FT4's published test keypair or a library RID, the admin args
     * sit under test:, and - vault only - the production moduleArgs block carries
     * NO oracle key (only the comment telling the deployer to add theirs), while
     * test.moduleArgs wires FT4's test key as the oracle for the shipped tests.
     */
    @Test
    fun templatesEmitOnlyFt4sPublishedTestKeysAndOnlyUnderTest() {
        val allowed = setOf(
            DappScaffold.TEST_ADMIN_PUBKEY.uppercase(),
            DappScaffold.TEST_ADMIN_PRIVKEY.uppercase(),
            DappScaffold.FT4_RID.uppercase().filter { it in "0123456789ABCDEF" },
            Ft4ModuleArgs.ICCF_GIT_RID.uppercase().filter { it in "0123456789ABCDEF" }
        )
        secureTemplates.forEach { template ->
            val files = DappScaffold.files("t", template = template)
            files.forEach { (path, content) ->
                Regex("[0-9A-Fa-f]{64,}").findAll(content).forEach { m ->
                    assertTrue(m.value.uppercase() in allowed, "$template/$path emits unexpected key-like material: ${m.value}")
                }
            }
            val yml = files.getValue("chromia.yml")
            val testBlockIdx = yml.indexOf("\ntest:")
            assertTrue(testBlockIdx > 0, "$template yml must have a test: block")
            val production = yml.substring(0, testBlockIdx)
            val testBlock = yml.substring(testBlockIdx)
            assertFalse(production.contains(DappScaffold.TEST_ADMIN_PUBKEY), "$template: no test key under blockchains")
            assertFalse(production.contains(DappScaffold.TEST_ADMIN_PRIVKEY), "$template: no test key under blockchains")
            assertTrue(testBlock.contains("lib.ft4.core.admin:"), "$template: admin args under test: only")
            assertEquals(yml.indexOf("lib.ft4.core.admin"), yml.lastIndexOf("lib.ft4.core.admin"), "$template: admin configured exactly once")
            assertTrue(yml.contains("merkle_hash_version: 2"))
            assertTrue(yml.contains("rellVersion: ${DappScaffold.RELL_VERSION}"))
            assertTrue(yml.contains("tagOrBranch: ${DappScaffold.FT4_VERSION}"))
            assertTrue(yml.contains("rate_limit"), "module_args must come from Ft4ModuleArgs")
            if (template in oracleTemplates) {
                // Production: the oracle key line exists only as a comment.
                val uncommentedOracle = production.lineSequence().filter { !it.trimStart().startsWith("#") }
                    .any { it.contains("oracle_pubkey") }
                assertFalse(uncommentedOracle, "$template production yml must not set a placeholder oracle key")
                assertTrue(production.contains("#   oracle_pubkey: x\"<your oracle public key>\""), "$template yml must tell the deployer where the oracle key goes")
                // Test: FT4's published test pubkey, under main, under test:.
                assertTrue(testBlock.contains("    main:\n      oracle_pubkey: x\"${DappScaffold.TEST_ADMIN_PUBKEY}\""), "$template test.moduleArgs must wire the oracle test key")
                assertEquals(
                    DappScaffold.TEST_ADMIN_PUBKEY,
                    DappScaffold.oracleTestModuleArgs().getValue("main").getValue("oracle_pubkey").toString().trim('"'),
                    "oracleTestModuleArgs must mirror the yml"
                )
                if (template == "lending") {
                    // The protocol fee key is configured the same way the oracle is:
                    // unset in production, FT4's published test key under test:.
                    val uncommentedTreasury = production.lineSequence().filter { !it.trimStart().startsWith("#") }
                        .any { it.contains("treasury_pubkey") }
                    assertFalse(uncommentedTreasury, "lending production yml must not set a placeholder protocol key")
                    assertTrue(production.contains("#   treasury_pubkey: x\"<your protocol fee key>\""), "lending yml must tell the deployer where the protocol fee key goes")
                    assertTrue(production.contains("DIFFERENT keys held by different parties"), "the yml must say the two keys are not the same key")
                    assertTrue(testBlock.contains("      treasury_pubkey: x\"${DappScaffold.TEST_ADMIN_PUBKEY}\""), "lending test.moduleArgs must wire the protocol fee test key")
                    assertEquals(
                        DappScaffold.TEST_ADMIN_PUBKEY,
                        DappScaffold.lendingTestModuleArgs().getValue("main").getValue("treasury_pubkey").toString().trim('"'),
                        "lendingTestModuleArgs must mirror the yml"
                    )
                }
            } else {
                assertFalse(yml.contains("oracle_pubkey"), "$template yml carries no oracle key")
            }
            val ymlCheck = org.chromia.tools.ChromiaYmlValidator.validate(yml)
            assertTrue(ymlCheck.errors.isEmpty(), "$template yml must validate: ${ymlCheck.errors}")
        }
    }

        // Module args are keyed by the TEMPLATE, never by the label: the vault
        // mutants used to be run under a label that fell back to the ft4 args,
        // so main.oracle_pubkey was missing and every case died with "Unable to
        // create GTX module" - a failure that satisfied the old
        // wrong-reason check while proving nothing about the guard.
    private fun runShipped(template: String, label: String = template, files: Map<String, String> = rellOf(template)): RunRellTests.Result {
        val result = RunRellTests.run(files, databaseUrl = dbUrl, moduleArgs = moduleArgsOf(template))
        // Printed so the gradle XML carries the per-case verdicts the report pastes.
        println("[$label] ok=${result.ok} total=${result.total} passed=${result.passed} failed=${result.failed}")
        result.cases.forEach { println("[$label]   ${it.name}: ${if (it.ok) "PASS" else "FAIL"}${it.error?.let { e -> " - $e" } ?: ""}") }
        return result
    }

    private fun assertShippedGreen(template: String, expectedCases: Set<String>) {
        val tests = runShipped(template)
        assertEquals(expectedCases, tests.cases.map { it.name.substringAfterLast(':').substringAfterLast('.') }.toSet(), "$template: ${tests.cases}")
        if (dbUrl != null) {
            assertTrue(tests.ok, "$template shipped tests must pass against the database: ${tests.notes} ${tests.cases}")
        } else {
            assertTrue(
                tests.cases.all { it.ok || it.dbRequired || it.error?.contains("Block execution failed") == true },
                "$template: without a database the shipped tests may only be environment-limited: ${tests.notes} ${tests.cases}"
            )
        }
    }

    @Test
    fun governanceShippedTestsRunGreen() = assertShippedGreen(
        "governance",
        setOf(
            "test_round1_single_account_drain_must_fail",
            "test_stake_weighted_proposal_pays_once_and_conserves_points",
            "test_majority_of_stake_can_reject"
        )
    )

    @Test
    fun vaultShippedTestsRunGreen() = assertShippedGreen(
        "vault",
        setOf(
            "test_round1_price_crash_must_fail",
            "test_round1_unbacked_sell_must_fail",
            "test_stale_or_missing_price_halts_trading"
        )
    )

    @Test
    fun stakingShippedTestsRunGreen() = assertShippedGreen(
        "staking",
        setOf(
            "test_round4_unbacked_reward_must_fail",
            "test_rewards_come_only_from_sponsor_funding",
            "test_late_staker_earns_nothing_for_the_past_and_cooldown_holds",
            "test_bounds_and_ownership"
        )
    )

    @Test
    fun marketplaceShippedTestsRunGreen() = assertShippedGreen(
        "marketplace",
        setOf(
            "test_round5_price_sandwich_must_fail",
            "test_round5_royalty_bypass_is_documented_not_enforced",
            "test_sale_and_escrow_conserve_points",
            "test_escrow_and_ownership_hold",
            "test_round6_auction_terms_cannot_move_under_a_standing_bid",
            "test_round6_auction_escrow_cannot_be_stranded"
        )
    )

    @Test
    fun lendingShippedTestsRunGreen() = assertShippedGreen(
        "lending",
        setOf(
            "test_round6_jit_interest_capture_must_fail",
            "test_round7_fee_step_jit_capture_must_fail",
            "test_round7_bad_debt_exit_race_must_fail",
            "test_healthy_position_cannot_be_liquidated",
            "test_under_water_position_cannot_hide",
            "test_self_liquidation_nets_nothing",
            "test_first_depositor_inflation_refuses_instead_of_swallowing",
            "test_borrow_limit_cannot_be_sliced",
            "test_stale_or_missing_price_halts_lending",
            "test_interest_moves_only_from_borrower_to_lender"
        )
    )

    @Test
    fun streamingShippedTestsRunGreen() = assertShippedGreen(
        "streaming",
        setOf(
            "test_round7_anchor_reset_grief_must_fail",
            "test_escrow_equals_paid_plus_reclaimable_at_every_point",
            "test_cancellation_is_fair_in_both_directions",
            "test_bounds_and_ownership",
            "test_round8_pause_clawback_must_fail",
            "test_round8_pause_cannot_end_a_committed_grant_must_fail"
        )
    )

    /** What the Rell runner reports when a run_must_fail transaction succeeds - the attack landed. */
    private val attackLanded = "did not fail"

    /**
     * The proof the exploit is unwritable: mutate ONE guard in the template and
     * the shipped replay must go red - for the exploit's reason, i.e. the attack
     * now SUCCEEDS where the test required it to be refused (or the conservation
     * assertion trips because value was created). A must-fail test that stayed
     * green without its guard would be theatre; so would one that went red
     * because the mutant no longer compiled or ran without its module_args.
     */
    private fun assertGuardMutationRedensExploitTest(
        template: String,
        guard: String,
        replacement: String,
        exploitTest: String,
        stillRefusedFragment: String,
        redFragment: String,
        alsoRemove: List<String> = emptyList(),
        alsoReplace: List<Pair<String, String>> = emptyList()
    ) {
        assertNotNull(dbUrl, "this test proves a drain is REFUSED and cannot do that without a database - " +
            "returning early here would report green having executed nothing (GOAL.md: a fake green is worse than a red)") // these run real transactions; the DB branch is authoritative and CI provides one
        val files = rellOf(template).toMutableMap()
        var main = files.getValue("main.rell")
        assertTrue(main.contains(guard), "$template guard must exist verbatim: $guard")
        main = main.replace(guard, replacement)
        // Defense in depth: some exploits are refused by a second guard once the
        // first is gone, so a mutant may have to strip several before the attack
        // can land. Each must exist verbatim, or the mutant proves nothing.
        alsoRemove.forEach { g ->
            assertTrue(main.contains(g), "$template guard must exist verbatim: $g")
            main = main.replace(g, "")
        }
        // Some bugs are a SHAPE rather than a guard: the streaming grief needs the
        // mutable anchor PUT BACK - a field, its initialisation and the line that
        // advances it - before it can be committed at all. Each half must exist
        // verbatim for the same reason alsoRemove's do.
        alsoReplace.forEach { (from, to) ->
            assertTrue(main.contains(from), "$template source must contain verbatim: $from")
            main = main.replace(from, to)
        }
        files["main.rell"] = main
        val mutant = runShipped(template, label = "$template-without[${guard.take(48)}]", files = files)
        // The mutant must still be a working dapp: the OTHER shipped tests keep
        // passing, so the only thing the mutated guard changes is the exploit.
        // (Rules out a mutant that fails for environmental reasons - a missing
        // module arg, a compile error - which is exactly what the first vault
        // mutants did while looking green.)
        // The mutant must still be a RUNNING dapp: no case may fail for an
        // environmental reason (module args, compile, schema) - that is the
        // vacuous-mutant failure mode. Other shipped tests MAY go red too: when a
        // guard is removed, value gets created, and the conservation test and the
        // exploit replay can both trip. Two independent proofs, not a broken run.
        mutant.cases.forEach {
            val e = it.error.orEmpty()
            assertFalse(
                e.contains("Unable to create GTX module") || e.contains("do not compile") || e.contains("Missing metadata"),
                "$template mutant failed for an environmental reason, proving nothing about the guard: ${it.name} - $e"
            )
        }
        val case = mutant.cases.single { it.name.endsWith(exploitTest) }
        assertFalse(case.ok, "$template: $exploitTest must FAIL once '$guard' is mutated - it stayed green, so it proves nothing")
        val error = case.error.orEmpty()
        // Right reason: the attack step now SUCCEEDS (run_must_fail reports that the
        // transaction did not fail) or the created value trips the conservation /
        // capped-payout assertion. Wrong reason would be the attack still refused
        // by some other guard (quotes the guard's text), or an environment failure.
        assertFalse(error.contains(stillRefusedFragment), "$template: $exploitTest failed for the wrong reason - the attack was still refused: $error")
        assertFalse(error.contains("Unable to create GTX module"), "$template: mutant ran without its module_args - vacuous: $error")
        assertTrue(error.contains(redFragment, ignoreCase = true), "$template: $exploitTest must fail because the attack landed ('$redFragment'), got: $error")
    }

    private fun assertGuardRemovalRedensExploitTest(
        template: String,
        guard: String,
        exploitTest: String,
        expectedFailureFragment: String,
        alsoRemove: List<String> = emptyList()
    ) = assertGuardMutationRedensExploitTest(template, guard, "", exploitTest, expectedFailureFragment, attackLanded, alsoRemove)

    @Test
    fun governanceExploitTestGoesRedWithoutTheQuorumGuard() = assertGuardRemovalRedensExploitTest(
        "governance",
        "require(p.yes_weight + p.no_weight >= p.quorum_weight, \"quorum not reached\");",
        "test_round1_single_account_drain_must_fail",
        "quorum not reached"
    )

    @Test
    fun governanceExploitTestGoesRedWithoutTheStakeGuard() = assertGuardRemovalRedensExploitTest(
        "governance",
        "require(proposer.stake > 0, \"only members with stake may propose\");",
        "test_round1_single_account_drain_must_fail",
        "only members with stake may propose"
    )

    @Test
    fun vaultExploitTestGoesRedWithoutTheReserveGuard() = assertGuardRemovalRedensExploitTest(
        "vault",
        "require(reserve_cash.balance >= cash_out, \"vault cannot cover the trade\");",
        "test_round1_unbacked_sell_must_fail",
        "vault cannot cover the trade"
    )

    @Test
    fun vaultExploitTestGoesRedWithoutThePriceBound() = assertGuardRemovalRedensExploitTest(
        "vault",
        "require(price * 10000 >= prev * (10000 - MAX_PRICE_MOVE_BPS), \"price move exceeds bound\");",
        "test_round1_price_crash_must_fail",
        "price move exceeds bound",
        // Defense in depth: with only the lower bound gone, the crash is STILL
        // refused by the rate limit (see the test below). The attack succeeds only
        // once every price-move guard is gone - which is exactly what "structural"
        // means, and what this mutant must strip to prove it.
        alsoRemove = listOf(
            "require(price * 10000 <= prev * (10000 + MAX_PRICE_MOVE_BPS), \"price move exceeds bound\");",
            "require(now - price_feed.updated_at >= MIN_PRICE_UPDATE_INTERVAL_MS, \"price update too soon\");"
        )
    )

    /** Removing the price bound alone is not enough to crash the price: the rate limit still refuses it. */
    @Test
    fun vaultPriceCrashIsStillRefusedByTheRateLimitWhenOnlyTheBoundIsRemoved() {
        assertNotNull(dbUrl, "this test proves a drain is REFUSED and cannot do that without a database - " +
            "returning early here would report green having executed nothing (GOAL.md: a fake green is worse than a red)")
        val files = rellOf("vault").toMutableMap()
        val bound = "require(price * 10000 >= prev * (10000 - MAX_PRICE_MOVE_BPS), \"price move exceeds bound\");"
        files["main.rell"] = files.getValue("main.rell").replace(bound, "")
        val mutant = runShipped("vault", label = "vault-without-lower-bound-only", files = files)
        val case = mutant.cases.single { it.name.endsWith("test_round1_price_crash_must_fail") }
        assertFalse(case.ok, "the exploit test asserts the bound's message, so it must go red")
        assertTrue(
            case.error?.contains("price update too soon") == true,
            "the crash must still be REFUSED - by the rate limit - not succeed: ${'$'}{case.error}"
        )
    }
    /** Uncap the release: the clock alone now creates points, and the round-4 claim from an empty pool lands. */
    @Test
    fun stakingExploitTestGoesRedWithoutTheReleaseCap() = assertGuardMutationRedensExploitTest(
        "staking",
        "min(pool.undistributed, elapsed_ms / 1000 * REWARD_PER_SECOND)",
        "elapsed_ms / 1000 * REWARD_PER_SECOND",
        "test_round4_unbacked_reward_must_fail",
        "nothing to claim",
        // The release cap is a CAP, not a refusal: with it gone the claim succeeds and
        // the capped-payout assertion trips (50 -> 31,536,010). That assertion tripping
        // IS the attack landing - value was created - so it is the success marker here,
        // and the wrong-reason check still excludes the guard's own refusal text.
        "assert_equals"
    )

    /** Stop debiting the pool for what it releases: sponsors' 300 pays out again and again, and conservation trips. */
    @Test
    fun stakingConservationTestGoesRedWithoutThePoolDebit() = assertGuardMutationRedensExploitTest(
        "staking",
        "pool.undistributed -= earned;",
        "",
        "test_rewards_come_only_from_sponsor_funding",
        "pool cannot cover the claim",
        "expected"
    )

    @Test
    fun stakingCooldownTestGoesRedWithoutTheCooldownGuard() = assertGuardRemovalRedensExploitTest(
        "staking",
        "require(op_context.last_block_time >= r.ready_at, \"cooldown not over\");",
        "test_late_staker_earns_nothing_for_the_past_and_cooldown_holds",
        "cooldown not over"
    )

    /**
     * Turn the exact price back into no check at all and the round-5 sandwich lands
     * again: the buyer who named 100 pays the 300 the seller relisted at.
     */
    @Test
    fun marketplaceSandwichTestGoesRedWithoutTheExactPriceGuard() = assertGuardRemovalRedensExploitTest(
        "marketplace",
        "require(l.price == expected_price, \"listing price changed - buy at the price you were shown\");",
        "test_round5_price_sandwich_must_fail",
        "listing price changed"
    )

    /** The same sandwich from the bidder's side: without the equality, an accept settles at whatever the bid was re-made at. */
    @Test
    fun marketplaceSandwichTestGoesRedWithoutTheExactOfferAmountGuard() = assertGuardRemovalRedensExploitTest(
        "marketplace",
        "require(o.amount == expected_amount, \"offer amount changed - accept the amount you were shown\");",
        "test_round5_price_sandwich_must_fail",
        "offer amount changed"
    )

    /** Stop debiting the bidder for what the escrow row holds and the offer mints points: conservation trips. */
    @Test
    fun marketplaceConservationTestGoesRedWithoutTheEscrowDebit() = assertGuardMutationRedensExploitTest(
        "marketplace",
        "update bidder ( .balance -= amount );",
        "",
        "test_sale_and_escrow_conserve_points",
        "insufficient balance",
        "expected"
    )

    /**
     * Drop the one-point floor and a recorded 1-point sale pays the creator nothing
     * again - exactly what round 5's `list at 1, pay the rest off-book` relied on.
     * The bypass itself stays open either way; this proves the arithmetic half is
     * load-bearing, and that the documentation test is asserting a real number.
     */
    @Test
    fun marketplaceRoyaltyDocumentationTestGoesRedWithoutTheOnePointFloor() = assertGuardMutationRedensExploitTest(
        "marketplace",
        "val royalty = if (exact > 0) exact else 1;",
        "val royalty = exact;",
        "test_round5_royalty_bypass_is_documented_not_enforced",
        "royalty cannot exceed the price",
        "expected"
    )

    /**
     * Take away the deadline check and the seller settles the moment a bid they like
     * arrives - the auction's terms moved under the standing bid after all. The
     * must-fail settle in the replay now SUCCEEDS.
     */
    @Test
    fun marketplaceAuctionTestGoesRedWithoutTheDeadline() = assertGuardRemovalRedensExploitTest(
        "marketplace",
        "require(op_context.last_block_time >= a.ends_at, \"auction has not ended\");",
        "test_round6_auction_terms_cannot_move_under_a_standing_bid",
        "auction has not ended"
    )

    /**
     * Empty out the ONE encumbrance helper and a plain gift walks the token out from
     * under the escrowed bid: transfer_nft, list_nft and accept_offer all succeed
     * where the replay required them to be refused, and the winner's points are
     * stranded with no settlement left that can pay them out. Nothing is minted, so
     * no conservation total and no static rule would ever have noticed.
     */
    @Test
    fun marketplaceAuctionEscrowTestGoesRedWithoutTheEncumbranceHelper() = assertGuardRemovalRedensExploitTest(
        "marketplace",
        "require(auction @? { .nft == token } == null, \"token is in an auction\");",
        "test_round6_auction_escrow_cannot_be_stranded",
        "token is in an auction"
    )

    /**
     * Stop refunding the outbid escrow and raising a bid quietly destroys the
     * previous bidder's points: the row that held them is deleted and nothing pays
     * them back, so the conservation total drops and the replay's balance assertion
     * trips. The delete-and-recreate is only safe because the refund is in the same
     * operation as the delete.
     */
    @Test
    fun marketplaceAuctionTestGoesRedWithoutTheOutbidRefund() = assertGuardMutationRedensExploitTest(
        "marketplace",
        "update previous ( .balance += refund );",
        "",
        "test_round6_auction_terms_cannot_move_under_a_standing_bid",
        "insufficient balance",
        "expected"
    )

    /**
     * THE ROUND-6 DRAIN, PUT BACK. There is no guard to delete here - the bug is a
     * SHAPE, and the shape does not exist while the entry is priced from a
     * pool_state. So the mutant re-creates it the only way it can be re-created: by
     * hand-building the deposit price out of the raw stored counters, reading
     * `pool.total_scaled_debt` (index units, as at the pool's first block) as though it
     * were cash. That is exactly round 6's stale `pool_value()`, and with it the
     * attacker's 10000 buys 10000 shares instead of 7692, the exit at the true price
     * returns 11500, and the replay's "no more than 10000 comes out" trips. Nothing
     * is minted in the mutant either - conservation stays green throughout, which is
     * why no invariant test and no static rule would have caught it.
     */
    @Test
    fun lendingJitTestGoesRedWhenTheEntryIsPricedFromTheRawCounters() = assertGuardMutationRedensExploitTest(
        "lending",
        "val minted = shares_for(amount, st);",
        "val minted = if (st.shares <= 0) amount else amount * st.shares / (pool.cash_available + pool.total_scaled_debt);",
        "test_round6_jit_interest_capture_must_fail",
        "deposit too small to mint a share",
        "expected"
    )

    /**
     * THE ROUND-7 BAD-DEBT EXIT RACE, PUT BACK. Stop capping the pool's debt at what
     * its collateral can repay and every index unit is valued at face again - which is
     * the template EXACTLY AS IT SHIPPED IN ROUND 7, when it handed 13920 of 14000 to
     * whoever exited first. With the cap gone the attacker's 1000 shares are worth
     * ~13000 instead of 1160, they take almost the whole 14000 the pool holds, and the
     * honest lender's IDENTICAL 1000 shares can no longer be paid at all - the replay
     * goes red on the exit that first-come-first-served has left nothing for. Nothing
     * is minted in the mutant either: conservation stays green throughout, which is why
     * no invariant test and no static rule would have caught it.
     *
     * Measured: 1000 shares pay 13000 instead of 1160, the drain itself.
     */
    @Test
    fun lendingBadDebtTestGoesRedWithoutTheRecoverabilityCap() = assertGuardMutationRedensExploitTest(
        "lending",
        // recoverable_debt is now PER POSITION - one pool-wide min() valued a
        // hopelessly underwater loan against another loan`s surplus collateral.
        // The mutation is unchanged in meaning: drop the recoverability cap and
        // value every debt at face however little backs it.
        "total += if (backing < face) backing else face;",
        "total += face;",
        "test_round7_bad_debt_exit_race_must_fail",
        // Wrong reason would be the attacker's own withdrawal being refused for want of
        // cash - then the mutant would prove nothing about the price it was refused at.
        "pool is illiquid",
        "expected"
    )

    /**
     * THE ROUND-7 FEE STEP, PUT BACK. Accrue the protocol's cut only on interest
     * borrowers have already PAID and it stops moving with the clock: it lands as a
     * jump in the block the repayment settles in, which is precisely what round 7's
     * dapp_a_feepool did on this header's own (wrong) advice. The attacker exits the
     * block before, the whole 600 falls on the lender who stayed, and the replay's
     * "both lenders take out the same" assertion trips - 11500 against 10900, the
     * numbers round 7 measured. The minimum holding period the header used to
     * prescribe would not have touched this: there is no deposit in it.
     */
    @Test
    fun lendingFeeStepTestGoesRedWhenTheFeeAccruesOnlyOnPaidInterest() = assertGuardMutationRedensExploitTest(
        "lending",
        "val interest = pool.interest_realised + max(0, outstanding);",
        "val interest = pool.interest_realised + max(0, outstanding * 0);",
        "test_round7_fee_step_jit_capture_must_fail",
        "more than the fee accrued so far",
        "expected"
    )

    /** Neuter the minimum first deposit and the ERC-4626 one-unit seed lands again. */
    @Test
    fun lendingInflationTestGoesRedWithoutTheMinimumFirstDeposit() = assertGuardMutationRedensExploitTest(
        "lending",
        "require(st.shares > 0 or amount >= MIN_INITIAL_DEPOSIT, \"the first deposit is too small to seed the pool\");",
        "require(st.shares > 0 or amount >= 0, \"the first deposit is too small to seed the pool\");",
        "test_first_depositor_inflation_refuses_instead_of_swallowing",
        "deposit too small to mint a share",
        attackLanded
    )

    /** Stop refusing a deposit that mints nothing and the victim's cash is swallowed. */
    @Test
    fun lendingInflationTestGoesRedWithoutTheZeroShareRefusal() = assertGuardRemovalRedensExploitTest(
        "lending",
        "require(minted > 0, \"deposit too small to mint a share\");",
        "test_first_depositor_inflation_refuses_instead_of_swallowing",
        "the first deposit is too small to seed the pool"
    )

    /** Take away the health check and a healthy position is liquidated for the bonus. */
    @Test
    fun lendingLiquidationTestGoesRedWithoutTheHealthCheck() = assertGuardMutationRedensExploitTest(
        "lending",
        "require(is_liquidatable(l, st, price), \"position is healthy\");",
        "require(is_liquidatable(l, st, price) or true, \"position is healthy\");",
        "test_healthy_position_cannot_be_liquidated",
        "payment too small to retire any debt",
        attackLanded
    )

    /** Widen the borrow limit and the position walks past its collateral. */
    @Test
    fun lendingBorrowLimitTestGoesRedWithoutTheLimit() = assertGuardMutationRedensExploitTest(
        "lending",
        "require(owed + amount <= borrow_limit, \"over the borrow limit\");",
        "require(owed + amount <= borrow_limit * 1000, \"over the borrow limit\");",
        "test_borrow_limit_cannot_be_sliced",
        "pool is illiquid",
        attackLanded
    )

    /** Widen the re-check and the borrower walks the collateral out from under the debt. */
    @Test
    fun lendingUnderWaterTestGoesRedWithoutTheCollateralRecheck() = assertGuardMutationRedensExploitTest(
        "lending",
        "require(owed * BPS <= remaining * MAX_LTV_BPS, \"that would put the position under water\");",
        "require(owed * BPS <= remaining * MAX_LTV_BPS * 1000, \"that would put the position under water\");",
        "test_under_water_position_cannot_hide",
        "not that much collateral",
        attackLanded
    )

    /** Widen the close factor and one liquidation takes the whole position. */
    @Test
    fun lendingSelfLiquidationTestGoesRedWithoutTheCloseFactor() = assertGuardMutationRedensExploitTest(
        "lending",
        "require(repay_amount <= max_close, \"over the close factor\");",
        "require(repay_amount <= max_close * 1000, \"over the close factor\");",
        "test_self_liquidation_nets_nothing",
        "not enough collateral to cover the bonus",
        attackLanded
    )

    /** Widen the price bound and the oracle can crash the price in one post. */
    @Test
    fun lendingOracleTestGoesRedWithoutThePriceBound() = assertGuardMutationRedensExploitTest(
        "lending",
        "require(move * BPS <= previous * MAX_PRICE_MOVE_BPS, \"price move too large\");",
        "require(move * BPS <= previous * MAX_PRICE_MOVE_BPS * 1000000, \"price move too large\");",
        "test_stale_or_missing_price_halts_lending",
        "price posted too soon",
        attackLanded
    )

    /** Drop the staleness halt and a day-old price still prices a new loan. */
    @Test
    fun lendingOracleTestGoesRedWithoutTheStalenessHalt() = assertGuardRemovalRedensExploitTest(
        "lending",
        "require(op_context.last_block_time - price_feed.updated_at <= MAX_PRICE_AGE_MS, \"price is stale\");",
        "test_stale_or_missing_price_halts_lending",
        "price move too large"
    )

    /**
     * THE ROUND-7 GRIEF, PUT BACK. There is no guard to delete here - the bug is a
     * SHAPE, and the shape does not exist while the entitlement is measured from an
     * immutable start. So the mutant re-creates it the only way it can be re-created:
     * by adding the mutable anchor back (the field, its initialisation at creation,
     * and the line that advances it on every payout, paid or not) and measuring from
     * it. That is realworld/adversary-round7/dapp_b_stream, restored line for line.
     * With it, trudy's ten settles a minute-minus-a-millisecond apart each release
     * ZERO and each move the marker, so bob is paid nothing and is owed nothing -
     * against the ten the clock says he earned. Nothing is minted in the mutant
     * either: conservation stays green throughout, which is exactly why the gate
     * reported zero findings on the original and no invariant test caught it.
     */
    @Test
    fun streamingGriefTestGoesRedWhenTheEntitlementIsMeasuredFromAMovableAnchor() = assertGuardMutationRedensExploitTest(
        "streaming",
        "val outstanding = earned_by(s, at) - s.released;",
        "val outstanding = s.rate_per_hour * (at - s.anchor_at) / HOUR_MS;",
        "test_round7_anchor_reset_grief_must_fail",
        // Wrong reason would be the replay failing to set itself up at all.
        "no such stream",
        "expected",
        alsoReplace = listOf(
            "mutable released: integer = 0;" to
                "mutable released: integer = 0;\n    mutable anchor_at: timestamp;",
            "started_at = op_context.last_block_time," to
                "started_at = op_context.last_block_time,\n        anchor_at = op_context.last_block_time,",
            "val amount = owed(s);\n    if (amount <= 0) return;" to
                "val amount = owed(s);\n    update s ( .anchor_at = op_context.last_block_time );\n    if (amount <= 0) return;"
        )
    )

    /**
     * The monotone total is load-bearing in the OTHER direction too. Stop subtracting
     * `released` and every settle pays the whole earned-to-date again: the grind that
     * paid bob nothing in round 7 now pays him 45 where the clock says 10, draining
     * alice's unearned escrow to whoever settles most. The replay's assertion is
     * written against the clock rather than against the module's counters precisely so
     * that it fails in both directions.
     */
    @Test
    fun streamingGriefTestGoesRedWhenTheReleasedTotalIsNotSubtracted() = assertGuardMutationRedensExploitTest(
        "streaming",
        "val outstanding = earned_by(s, at) - s.released;",
        "val outstanding = earned_by(s, at);",
        "test_round7_anchor_reset_grief_must_fail",
        "no such stream",
        "expected"
    )

    /**
     * Stop debiting the escrow for what a payout pays and the stream mints points: the
     * payee is credited, the escrow still holds the same money, and the payer gets all
     * of it back at cancellation. The sealed ledger (funded == released + escrow +
     * refunded) breaks in the first settle.
     */
    @Test
    fun streamingConservationTestGoesRedWithoutTheEscrowDebit() = assertGuardMutationRedensExploitTest(
        "streaming",
        "update s ( .escrow -= amount, .released += amount );",
        "update s ( .released += amount );",
        "test_escrow_equals_paid_plus_reclaimable_at_every_point",
        "insufficient balance",
        "expected"
    )

    /**
     * THE OTHER HALF OF THE ROUND-7 DRAIN, PUT BACK: the payer takes the escrow back
     * without paying what the payee has already earned. The guard is the ORDER of two
     * lines in cancel_stream, which is exactly the kind of thing no static rule can
     * see - so the replay asserts it instead. With the payout removed, bob keeps
     * nothing of the minute he worked and alice reclaims 100%.
     */
    @Test
    fun streamingCancellationTestGoesRedWhenTheRefundComesBeforeThePayout() = assertGuardMutationRedensExploitTest(
        "streaming",
        "pay_out(s);\n    val refund = s.escrow;",
        "val refund = s.escrow;",
        "test_cancellation_is_fair_in_both_directions",
        "only the payer or the payee may cancel",
        "expected"
    )

    /**
     * Neuter the committed-grant term and a vesting grant can be clawed back after
     * all: the cancellation the replay requires to be refused now succeeds, and the
     * beneficiary's remaining entitlement goes back to the grantor.
     */
    @Test
    fun streamingCancellationTestGoesRedWithoutTheCommittedGrantTerm() = assertGuardMutationRedensExploitTest(
        "streaming",
        "require(s.cancellable, \"this stream is not cancellable\");",
        "require(true, \"this stream is not cancellable\");",
        "test_cancellation_is_fair_in_both_directions",
        "this stream is not cancellable",
        attackLanded
    )

    /**
     * ROUND 8'S DRAIN, and it is ONE `require()`. dapp_a_pause and
     * dapp_a2_pause_variant differ by exactly this line: with it every attack was
     * refused, without it a spurious resume_stream on a RUNNING stream adds the
     * whole span since the last pause to `paused_ms`, rewrites the payee's
     * entitlement backwards past what they were already paid, and the payer
     * cancels and keeps the escrow. `paused_ms` is monotone in BOTH versions,
     * which is why the header's old "can never rewrite the past" was worthless.
     */
    @Test
    fun streamingPauseTestGoesRedWhenAResumeNeedNotFollowAPause() = assertGuardMutationRedensExploitTest(
        "streaming",
        guard = "require(s.paused, \"stream is not paused\");",
        replacement = "",
        exploitTest = "test_round8_pause_clawback_must_fail",
        stillRefusedFragment = "only the payer or the payee may resume",
        redFragment = attackLanded
    )

    /**
     * The other transition guard. Without it a second pause moves `paused_at`
     * forward, the open pause active_elapsed() subtracts shrinks, and a stream
     * that is supposed to be frozen keeps accruing - the same seam, in the
     * direction that costs the payer instead of the payee.
     */
    @Test
    fun streamingPauseTestGoesRedWhenAStreamCanBePausedTwice() = assertGuardMutationRedensExploitTest(
        "streaming",
        guard = "require(not s.paused, \"stream is already paused\");",
        replacement = "",
        exploitTest = "test_round8_pause_clawback_must_fail",
        stillRefusedFragment = "only the payer or the payee may pause",
        redFragment = attackLanded
    )

    /**
     * Round 8, dapp_a3_pause_as_cancel: a pause that never looks at `cancellable`
     * voids the one guarantee this template sells to a vesting grant. The drain
     * there was terminal (cancel-and-reopen); here the pause is in-place, so what
     * the missing line buys the payer is the power to freeze a grant nobody can
     * cancel - the money never vests and never comes back.
     */
    @Test
    fun streamingCommittedGrantTestGoesRedWhenThePauseIgnoresTheCancellableTerm() = assertGuardMutationRedensExploitTest(
        "streaming",
        guard = "require(s.cancellable, \"a committed grant cannot be paused\");",
        replacement = "",
        exploitTest = "test_round8_pause_cannot_end_a_committed_grant_must_fail",
        stillRefusedFragment = "this stream is not cancellable",
        redFragment = attackLanded
    )

    // ------------------------------------------------------------------------
    // ROUND 8, dapp_b_ratecurve: the extension the header now sanctions, and the
    // proof it is safe only because the index is checkpointed.
    //
    // This is not a mutant of the shipped template - it is the EXTENSION an author
    // is told to write, applied to the template exactly as seam 2 now describes
    // it: a utilisation curve in `current_rate_bps_per_year()` and nothing else
    // touched. Round 8 built the same curve on the pre-checkpoint template, passed
    // `rell_check` and `rell_security_check` with zero findings, and a lender's own
    // withdrawal then made a HEALTHY borrower liquidatable at an UNCHANGED oracle
    // price, because the index multiplied the rate NOW by the WHOLE elapsed span.
    // ------------------------------------------------------------------------

    /** The Compound/Aave kink, verbatim from round 8's build: 700 bps at 50% utilisation, 811 at 61.19%. */
    private val utilisationCurve = """
        val BASE_RATE_BPS = 200;
        val SLOPE1_BPS = 800;
        val KINK_BPS = 8000;
        val SLOPE2_BPS = 6000;

        function utilisation_bps(): integer {
            val d = pool.total_scaled_debt;
            if (d <= 0) return 0;
            val total = pool.cash_available + d;
            if (total <= 0) return 0;
            val u = d.to_big_integer() * BPS.to_big_integer() / total.to_big_integer();
            val cap = BPS.to_big_integer();
            return (if (u > cap) cap else u).to_integer();
        }

        function current_rate_bps_per_year(): integer {
            val u = utilisation_bps();
            if (u <= KINK_BPS) return BASE_RATE_BPS + u * SLOPE1_BPS / KINK_BPS;
            return BASE_RATE_BPS + SLOPE1_BPS + (u - KINK_BPS) * SLOPE2_BPS / (BPS - KINK_BPS);
        }
    """.trimIndent()

    private val flatRateSeam = "function current_rate_bps_per_year(): integer = INTEREST_RATE_BPS_PER_YEAR;"

    /**
     * The attack, adapted from realworld/adversary-round8/dapp_b_ratecurve's
     * test_b1: a borrower is healthy at 100 tokens against 6000 of debt after
     * three and a half years, an unrelated lender withdraws their own 2000, and
     * the position must STILL be healthy - the oracle has not moved and the
     * borrower has done nothing. The withdrawal raises the rate from here on; it
     * may not raise it on the past.
     */
    private fun rateCurveAttackTest(): String = """
        @test module;

        import main;
        import lib.ft4.test.core.{ register_alice, register_bob, register_trudy, ft_auth_operation_for };
        import lib.ft4.test.core.auth.{ admin_priv_key };

        function oracle(): rell.test.keypair =
            rell.test.keypair(priv = admin_priv_key(), pub = main.oracle_pubkey());

        function post_price(price: integer) {
            rell.test.tx().op(main.set_price(price)).nop().sign(oracle()).run();
        }

        function signed(keypair: rell.test.keypair, op: rell.test.op) {
            rell.test.tx().op(ft_auth_operation_for(keypair.pub)).op(op).nop().sign(keypair).run();
        }

        function signed_must_fail(keypair: rell.test.keypair, op: rell.test.op, expected: text) {
            rell.test.tx().op(ft_auth_operation_for(keypair.pub)).op(op).nop().sign(keypair).run_must_fail(expected);
        }

        function after(ms: integer) {
            rell.test.set_next_block_time_delta(ms);
            rell.test.block().run();
        }

        function assert_conserved() {
            assert_equals(main.cash_in_circulation(), main.account_count() * main.WELCOME_CASH);
            assert_equals(main.tokens_in_circulation(), main.account_count() * main.WELCOME_TOKENS);
            assert_equals(main.scaled_debt_matches_positions(), true);
            assert_equals(main.collateral_matches_positions(), true);
        }

        function wealth(owner: byte_array, price: integer): integer {
            val a = main.get_account(owner)!!;
            return a.cash + a.tokens * price / main.PRICE_SCALE;
        }

        // EXPLOIT MUST FAIL. A lender's own withdrawal must not make somebody else
        // liquidatable. In round 8 it did: utilisation 5000 -> 6119 bps, rate
        // 700 -> 811, applied to all three and a half years at once.
        function test_round8_rate_curve_reprices_history_must_fail() {
            val honest_lender = register_alice();
            val borrower = register_bob();
            val attacker = register_trudy();
            signed(honest_lender.keypair, main.register_account());
            signed(borrower.keypair, main.register_account());
            signed(attacker.keypair, main.register_account());
            val price = 100 * main.PRICE_SCALE;
            post_price(price);

            signed(honest_lender.keypair, main.deposit_cash(10000));
            signed(attacker.keypair, main.deposit_cash(2000));
            signed(borrower.keypair, main.add_collateral(100));
            signed(borrower.keypair, main.borrow(6000));
            assert_conserved();

            // The curve is live: 6000 out of 12000 is 50% utilisation, which on this
            // curve is 700 bps. If this ever stopped holding the replay would not be
            // replaying anything, so it is asserted rather than assumed.
            assert_equals(main.utilisation_bps(), 5000);
            assert_equals(main.current_rate_bps_per_year(), 700);

            val collateral_before = main.get_loan(borrower.account.id)!!.collateral;

            // Three and a half years of honest borrowing at 50% utilisation.
            after(3 * main.YEAR_MS + main.YEAR_MS / 2);
            post_price(price);

            // HEALTHY: 100 tokens at 100 is 10000 of collateral and the threshold is 75%.
            signed_must_fail(attacker.keypair, main.liquidate(borrower.account.id, 100), "position is healthy");
            assert_conserved();

            // NOT wealth(): it counts cash and collateral tokens but NOT pool shares, so
            // a lender turning shares back into cash makes it rise mechanically - 18000
            // to 20196 here, every unit of it the attacker`s own deposit and its honest
            // interest coming home. The payoff of THIS attack is SEIZED COLLATERAL, so
            // that is what gets measured, and the baseline is taken here rather than
            // before the 3.5 years for the same reason: the yield is not the drain.
            val attacker_tokens_before = main.get_account(attacker.account.id)!!.tokens;

            // THE ATTACK, ONE OPERATION: the attacker withdraws their own deposit,
            // pushing utilisation - and therefore the rate - up.
            signed(attacker.keypair, main.withdraw_cash(2000));
            assert_conserved();
            // The rate really did move, or this proves nothing.
            assert_equals(main.current_rate_bps_per_year() > 700, true);

            // THE PROPERTY: the past was not re-priced, so the same position at the
            // same oracle price is still healthy and there is nothing to seize.
            signed_must_fail(attacker.keypair, main.liquidate(borrower.account.id, 100), "position is healthy");
            assert_equals(main.get_loan(borrower.account.id)!!.collateral, collateral_before);
            assert_equals(main.get_account(attacker.account.id)!!.tokens, attacker_tokens_before);
            assert_conserved();
        }

    """.trimIndent() + "\n"

    /**
     * The sanctioned extension must be GREEN: a utilisation curve on the shipped
     * template refuses round 8's drain.
     */
    @Test
    fun lendingRateCurveExtensionRefusesRound8Drain() {
        assertNotNull(dbUrl, "this test proves a drain is REFUSED and cannot do that without a database - " +
            "returning early here would report green having executed nothing (GOAL.md: a fake green is worse than a red)")
        val files = rellOf("lending").toMutableMap()
        val main = files.getValue("main.rell")
        assertTrue(main.contains(flatRateSeam), "the rate must be a one-line seam a curve can replace: $flatRateSeam")
        files["main.rell"] = main.replace(flatRateSeam, utilisationCurve)
        files["test/main_test.rell"] = rateCurveAttackTest()
        val run = runShipped("lending", label = "lending+utilisation-curve", files = files)
        run.cases.forEach {
            assertFalse(
                it.error.orEmpty().contains("Unable to create GTX module") || it.error.orEmpty().contains("do not compile"),
                "the sanctioned extension must compile and run, or it proves nothing: ${it.name} - ${it.error}"
            )
        }
        assertTrue(run.ok, "a utilisation curve on the checkpointed index must refuse round 8's drain: ${run.notes} ${run.cases}")
    }

    /**
     * ...and it is the CHECKPOINT that refuses it. Put back round 8's index - the
     * rate NOW times the WHOLE elapsed span - and the same curve, the same attack
     * and the same oracle price drain a healthy position. This is the mutant that
     * makes the guard load-bearing rather than decorative.
     */
    @Test
    fun lendingRateCurveDrainsOnceTheIndexIsNoLongerCheckpointed() {
        assertNotNull(dbUrl, "this test proves a drain is REFUSED and cannot do that without a database - " +
            "returning early here would report green having executed nothing (GOAL.md: a fake green is worse than a red)")
        val files = rellOf("lending").toMutableMap()
        var main = files.getValue("main.rell")
        assertTrue(main.contains(flatRateSeam))
        main = main.replace(flatRateSeam, utilisationCurve)
        // Round 8's index, restored: no checkpoint, the current rate applied to the
        // whole span since the pool opened.
        val checkpointed = "pool.rate_ms_accrued += current_rate_bps_per_year().to_big_integer() * elapsed.to_big_integer();"
        assertTrue(main.contains(checkpointed), "the checkpoint must exist verbatim: $checkpointed")
        main = main.replace(
            checkpointed,
            "pool.rate_ms_accrued = current_rate_bps_per_year().to_big_integer() * " +
                "(op_context.last_block_time - pool.opened_at).to_big_integer();"
        )
        files["main.rell"] = main
        files["test/main_test.rell"] = rateCurveAttackTest()
        val mutant = runShipped("lending", label = "lending+curve-without-checkpoint", files = files)
        mutant.cases.forEach {
            val e = it.error.orEmpty()
            assertFalse(
                e.contains("Unable to create GTX module") || e.contains("do not compile") || e.contains("Missing metadata"),
                "mutant failed for an environmental reason, proving nothing: ${it.name} - $e"
            )
        }
        val case = mutant.cases.single { it.name.endsWith("test_round8_rate_curve_reprices_history_must_fail") }
        assertFalse(case.ok, "without the checkpoint the rate curve must re-price history and the drain must land")
        val error = case.error.orEmpty()
        assertFalse(
            error.contains("Unable to create GTX module"),
            "mutant ran without its module_args - vacuous: $error"
        )
        assertTrue(
            error.contains(attackLanded, ignoreCase = true),
            "the liquidation of a healthy position must SUCCEED without the checkpoint ('$attackLanded'), got: $error"
        )
    }
}
