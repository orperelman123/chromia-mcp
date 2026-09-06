package org.chromia

import org.chromia.tools.DappScaffold
import org.chromia.tools.Ft4ModuleArgs
import org.chromia.tools.RellCheck
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

    private val secureTemplates = listOf("governance", "vault", "staking", "marketplace", "lending", "streaming", "amm", "stablecoin", "exchange")

    /** The templates whose main module reads an oracle key from configuration. */
    private val oracleTemplates = setOf("vault", "lending", "stablecoin")

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
        // governance reads the FOUNDER key that countersigns a genesis claim. Without
        // it every governance case dies with "Unable to create GTX module" - the
        // vacuous-mutant failure mode this map exists to prevent.
        template == "governance" -> DappScaffold.governanceTestModuleArgs()
        template in oracleTemplates -> DappScaffold.oracleTestModuleArgs()
        else -> DappScaffold.ft4TestModuleArgs()
    }

    private val dbUrl: String? = System.getenv(RunRellTests.DATABASE_URL_ENV)

    // These tests assert what a template's shipped suite (or its mutant) DECIDES,
    // not how fast the runner decides it; the 90s production deadline has its own
    // tests (the abandon path in AuditFindingsRegressionTest, the env cap in
    // RunRellTestsToolTest). The lending suite alone takes ~56s under the
    // real chr on the dev box, and this class ran 75 full suites against a shared
    // database while another agent's `chr test` was on the same host - gate #9
    // (amm) and gate #14 (lending) each lost one to the deadline with the verdict
    // otherwise green, at 40 minutes a rerun. A deadline still exists, so a real
    // runaway is still caught, just not mistaken for a starved box.
    private val SHIPPED_SUITE_TIMEOUT_SECONDS = 600L

    /** Rell's run_must_fail failure text when the transaction it expected to fail succeeds. */
    private val RUN_MUST_FAIL_UNEXPECTED_SUCCESS = "Transaction did not fail"
    private fun opBody(main: String, op: String): String =
        main.substringAfter("operation $op").substringBefore("\n}")

    private fun withoutComments(source: String): String =
        source.lineSequence().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")

    /**
     * Every header opens by COUNTING its structural guards, and round 12 found that
     * count wrong: the stablecoin said "Six guards are STRUCTURAL" and listed seven -
     * round 11 added one and did not move the number. A count in a header is a claim
     * like any other, so it is measured here from the list itself: one entry is a
     * `//   NAME  - ` line, and its continuation lines are indented two spaces further.
     */
    private fun guardCount(main: String): Int =
        Regex("(?m)^\\s*//\\s{3}[A-Z][A-Z0-9 ,'/-]*\\s+-\\s").findAll(main).count()

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
        assertEquals(listOf("hello", "ft4", "governance", "vault", "staking", "marketplace", "lending", "streaming", "amm", "stablecoin", "exchange"), DappScaffold.templates)
        assertEquals("exchange", DappScaffold.toJson("book", template = "exchange").getValue("template").toString().trim('"'), "the class round 12 drained must scaffold its own template")
        assertEquals("governance", DappScaffold.toJson("dao", template = "governance").getValue("template").toString().trim('"'))
        assertEquals("stablecoin", DappScaffold.toJson("peg", template = "stablecoin").getValue("template").toString().trim('"'))
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
        assertTrue(notes.contains("template=amm"), "notes must steer swap-pool / DEX / market-maker builders to the template")
        assertTrue(
            notes.contains("A SWAP NAMES THE EXACT RESERVES IT") &&
                notes.contains("WAS QUOTED AT AND THERE IS NO TOLERANCE FIELD AT ALL"),
            "the notes must name the amm guard precisely - the template removes the tolerance, it does not tune it"
        )
        assertTrue(
            notes.contains("STRONGER than a min_out floor rather than a") &&
                notes.contains("weakening of one - a floor can only abort your own trade and must never be"),
            "the notes must say the amm guard is not a weakened min_out - the corpus pins a MUST_STAY_CLEAN row on that floor"
        )
        assertTrue(
            notes.contains("IS AN IMMUTABLE POSITION ROW WITH A TERM") &&
                notes.contains("until COMMITMENT_MS (a constant, never a parameter) after the row was created."),
            "the notes must name the JIT guard, not just the template"
        )
        // Round 9 built its stablecoin on the vault's advice BECAUSE these notes sent
        // "stablecoin" to template=vault. They must now name the class's own template,
        // the absence that makes the drain unwritable, and the residual the header admits.
        assertTrue(notes.contains("start from template=stablecoin, NOT template=vault"), "notes must steer stablecoin / CDP builders to their own template")
        assertTrue(
            notes.contains("NO operation that pays a coin holder par out of\nsomebody else's position"),
            "the notes must name the stablecoin guard - it is an ABSENCE, and the corpus pins the drain on its presence"
        )
        assertTrue(notes.contains("PRO-RATA share (collateral * repaid / debt, never more, whatever the\norder)"), "the notes must name the liquidation cap that makes order irrelevant")
        assertTrue(notes.contains("still leaves bad debt"), "the notes must carry the header's residual, not a claim of a peg no template can hold")
        // Round 12: the stablecoin's residual named the window AFTER the system goes
        // under-backed and the loss is created BEFORE it - the guard is a bonus floor.
        assertTrue(
            notes.contains("that band starts at 105% BACKING, not at 100%"),
            "the notes must name the band the round-12 guard actually opens at"
        )
        // Round 12's un-templated class. Every name for an order book must reach its own
        // template, and the notes must name the guard rather than just the template.
        assertTrue(notes.contains("start from template=exchange, NOT template=amm"), "notes must steer order-book builders to their own template")
        assertTrue(
            notes.contains("A PARTIAL FILL WRITES ONE\nMONOTONE COUNTER"),
            "the notes must name the exchange guard - the drain was a partial fill that re-created the row"
        )
        assertTrue(
            notes.contains("NO\nOPERATION NAMES A COUNTERPARTY"),
            "the notes must say the book matches and no caller does"
        )
        assertTrue(
            notes.contains("a resting order is a free option"),
            "the notes must carry the exchange header's residual, not only its guards"
        )
    }

    /**
     * The unknown-template fallback must ROUTE, not just list names. Round 6's drain
     * landed in the un-templated class: `scaffold_dapp(template="lending")` answered
     * "Unknown template (valid: ...); scaffolded the 'hello' template", and the agent
     * wrote the whole value class freehand. `lending` is now a real template, so the
     * near-miss NAMES for it must route there rather than to the nearest cousin.
     */
    /**
     * DX audit 2026-09-04: `template="Stablecoin"` fell back to hello with an
     * "Unknown template" warning, and `name="My-Peg App"` fell back to `hello`
     * with a regex and no example. Case and whitespace are not a different ask;
     * an invalid name should come back with the name it most likely meant.
     */
    @Test
    fun scaffoldToleratesTemplateCaseAndSuggestsAValidName() {
        for (asked in listOf("Stablecoin", " stablecoin ", "STABLECOIN")) {
            val out = DappScaffold.toJson("peg", template = asked)
            assertEquals("stablecoin", out.getValue("template").toString().trim('"'), "template '$asked' must resolve to stablecoin")
            assertEquals("[]", out.getValue("warnings").toString(), "case/whitespace must not be reported as an unknown template: $asked")
            assertTrue(out.getValue("files").toString().contains("mint_stable"), "the stablecoin files must be the ones returned for '$asked'")
        }
        val warning = DappScaffold.toJson("My-Peg App", template = "stablecoin").getValue("warnings").toString()
        assertTrue(warning.contains("e.g. name=\\\"my_peg_app\\\""), "an invalid name must come with the valid name it most likely meant: $warning")
        assertEquals("my_peg_app", DappScaffold.suggestName("My-Peg App"))
        assertEquals("v2_peg", DappScaffold.suggestName("2 v2 peg"))
        assertNull(DappScaffold.suggestName("123"), "a name with no usable letter has no suggestion")
        assertEquals(32, DappScaffold.suggestName("a".repeat(40))!!.length)
    }

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
        // Round 8's AMM was built ONLY because this answer sent `amm` to `vault`. Every
        // name for the class must now reach its own template, and `oracle` must still
        // reach the vault - a keyword list that swallowed it would be a mis-route
        // dressed as coverage.
        listOf("dex", "swap_pool", "uniswap", "market_maker", "liquidity_pool", "token_pair", "amm_v2").forEach { asked ->
            val warning = DappScaffold.toJson("x", template = asked).getValue("warnings").toString()
            assertTrue(warning.contains("Use `template=amm`"), "$asked must be routed to the amm template: $warning")
            assertTrue(warning.contains("NAMES THE EXACT RESERVES"), "$asked must be told what makes the round-8 sandwich unwritable: $warning")
        }
        assertTrue(
            "amm" in DappScaffold.templates,
            "the class round 8 drained must no longer redirect to vault"
        )
        // Round 9's stablecoin was built on the vault's advice because "stablecoin"
        // sat in the vault's keyword list and the redirect said so. Every name for the
        // class must reach its own template AHEAD of lending (which claims "debt") and
        // of vault; `oracle`, `redeem` and `debt_market` must still land where they did.
        listOf("stable_coin", "cdp", "cdp_vault", "collateralized_debt", "pegged_token", "synthetic_asset").forEach { asked ->
            val warning = DappScaffold.toJson("x", template = asked).getValue("warnings").toString()
            assertTrue(warning.contains("Use `template=stablecoin`"), "$asked must be routed to the stablecoin template: $warning")
            assertTrue(warning.contains("NO operation that pays a coin"), "$asked must be told what makes the round-9 drain unwritable: $warning")
        }
        listOf("oracle", "redeem", "price_feed").forEach { asked ->
            assertTrue(
                DappScaffold.toJson("x", template = asked).getValue("warnings").toString().contains("Use `template=vault`"),
                "$asked must still reach the vault template"
            )
        }
        assertTrue(
            DappScaffold.toJson("x", template = "debt_market").getValue("warnings").toString().contains("Use `template=lending`"),
            "a debt market must still reach the lending template"
        )
        // Round 12's drain landed in the un-templated order-book class, and the answer
        // that produced it was this table's own: it said nothing covered an order book and
        // offered two sentences, both of which were implemented literally.
        listOf("order_book", "orderbook", "limit_order", "matching_engine", "clob", "bid_ask").forEach { asked ->
            val warning = DappScaffold.toJson("x", template = asked).getValue("warnings").toString()
            assertTrue(warning.contains("Use `template=exchange`"), "$asked must be routed to the exchange template: $warning")
            assertTrue(warning.contains("ONE MONOTONE COUNTER"), "$asked must be told what makes the round-12 grind unwritable: $warning")
        }
        assertFalse(
            DappScaffold.toJson("x", template = "order book").getValue("warnings").toString().contains("NO SHIPPED TEMPLATE COVERS AN ORDER BOOK"),
            "the answer that produced round 12's drain must not still be given"
        )
        // ...and the amm answer, which `exchange` phrasings also reach, must name it too
        // rather than repeating that nothing covers a book.
        assertTrue(
            DappScaffold.toJson("x", template = "swap_pool").getValue("warnings").toString().contains("that IS covered now - `template=exchange`"),
            "the amm answer must name the order-book template now that one ships"
        )
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
        // The quorum is snapshotted at creation, and since round 11 that snapshot is
        // the WHOLE bar: round 10's live term at execution turned out to be a veto
        // anybody could buy for two points, and round 10's own drain is closed at the
        // money instead (a proposal reserves what it may spend when it is created).
        assertTrue(main.contains("quorum_weight = quorum_for(dao.total_stake)"), "quorum must be snapshotted from total stake at creation")
        // ROUND 11, THE BAR. It is the quorum of the stake that existed AT CREATION and
        // nothing else, and every voter's weight is frozen the same way. Round 10 read
        // the quorum live at execution; votes freeze at the deadline and that bar did
        // not, so two points of stake staked afterwards vetoed an approved payout for
        // ever - 0.02% of the stake, repeatable, at no cost.
        val code = withoutComments(main)
        assertTrue(main.contains("stake_at_creation = dao.total_stake,"), "the stake at creation must be recorded on the proposal")
        assertTrue(main.contains(GOVERNANCE_CREATION_QUORUM_GUARD), "execute_proposal must apply the bar fixed at creation")
        assertFalse(code.contains("max(p.quorum_weight, quorum_for(dao.total_stake))"), "round 10's live term is round 11's two-point veto")
        assertFalse(opBody(code, "execute_proposal").contains("dao.total_stake"), "the bar must read no stake that arrived after the proposal")
        assertTrue(
            main.contains("create proposal_stake(proposal = p, owner = m.owner, weight = m.stake);"),
            "every voter's weight must be frozen at creation - stake bought into a running vote is the same defect as a bar bought after it"
        )
        assertTrue(
            main.contains("val snapshot = proposal_stake @? { .proposal == p, .owner == voter.owner };"),
            "a vote must weigh its snapshot row, never the member's live stake"
        )
        // ROUND 11, THE MINT. Registration credits nothing: four registrations minting
        // 1000 points each outvoted three honest members and took a 7000 treasury, and
        // no voting rule survives free identities plus a free mint. The only mint left
        // is the genesis claim - founder-countersigned, once per member, capped at
        // GENESIS_POINTS, and shut for good at the first stake.
        assertFalse(
            main.contains("create member(owner = account.id, balance = WELCOME_POINTS)"),
            "round 11: a permissionless welcome grant IS the security parameter"
        )
        assertTrue(main.contains("create member(owner = account.id);"), "registration must create a member with nothing")
        assertTrue(main.contains("struct module_args {\n    founder_pubkey: pubkey;\n}"), "the founder key must be configuration, not a parameter")
        assertFalse(Regex("[0-9A-Fa-f]{64,}").containsMatchIn(main), "no key-like hex in the governance source")
        assertTrue(main.contains("val GENESIS_POINTS ="), "the whole supply of voting weight must be a named constant")
        val claim = opBody(code, "claim_allocation")
        assertTrue(claim.contains("require(op_context.is_signer(chain_context.args.founder_pubkey), \"the founder must countersign a genesis claim\");"))
        assertTrue(claim.contains("require(not dao.genesis_closed, \"genesis allocation is closed\");"), "the mint must shut when the FOUNDER shuts it - round 12: a member staking one point shut it for everyone")
        assertTrue(claim.contains("require(not m.allocated, \"already allocated\");"), "one claim per member, ever")
        assertTrue(claim.contains("require(dao.allocated + WELCOME_POINTS <= GENESIS_POINTS, \"genesis supply exhausted\");"))
        // ROUND 10, closed without reading live stake, and since round 12 closed at the
        // ARITHMETIC rather than at a scan: a proposal may be paid only out of money that
        // had already arrived when it was created, and each point of it once. Two monotone
        // counters, O(1), and no number of proposers changes it.
        assertTrue(main.contains(GOVERNANCE_VINTAGE_GUARD), "an approval must not be payable out of money that arrived after it")
        assertTrue(main.contains("funded_at_creation = dao.funded_total"), "the vintage must be recorded on the proposal")
        assertTrue(main.contains("dao.paid_total += p.amount;") && main.contains("dao.funded_total += amount;"), "both counters must be monotone")
        assertTrue(main.contains(GOVERNANCE_COMMITTED_TREASURY_GUARD), "a proposal must reserve its amount from the uncommitted treasury")
        assertFalse(code.contains("require(amount <= dao.treasury_balance, \"amount exceeds treasury\")"), "the raw treasury check is round 10's parked approval")
        // ROUND 12, THE FREEZE. committed_treasury() counted every row that was
        // `not .executed`, so one point of stake out of 2001 reserved the whole treasury
        // by LOSING a vote 2000 to 1 and could renew it in the block its window lapsed.
        // Only an APPROVED proposal reserves against other members now.
        assertTrue(main.contains(GOVERNANCE_APPROVED_ONLY_TERM), "only an approved proposal may reserve the treasury against other members")
        assertFalse(
            code.contains("if (now < p.deadline + EXECUTION_WINDOW_MS) total += p.amount;"),
            "round 12: a rejected proposal that still reserves is a veto one point of stake can buy"
        )
        assertTrue(
            main.contains("function is_approved(p: proposal, now: timestamp): boolean"),
            "approved must be a named predicate, not an inline guess"
        )
        assertTrue(main.contains("not has_live_claim(now, account.id, beneficiary, amount),"), "a re-proposal must not buy a fresh reservation")
        // ROUND 12, THE GENESIS WINDOW. It closed on `dao.total_stake == 0`, so the first
        // member the founder countersigned shut it on everybody else by staking one point.
        // Only the founder closes it, and staking is refused until they have.
        assertFalse(code.contains("require(dao.total_stake == 0, \"genesis allocation is closed\")"), "a member's stake must not close the mint")
        assertTrue(opBody(code, "close_genesis").contains("require(op_context.is_signer(chain_context.args.founder_pubkey), \"the founder must sign the genesis close\");"))
        assertTrue(opBody(code, "close_genesis").contains("dao.genesis_closed = true;"))
        assertTrue(opBody(code, "fund_treasury").contains(GOVERNANCE_GENESIS_PHASE_GUARD), "staking must be refused while the genesis window is open")
        assertEquals(
            1,
            Regex("dao\\.genesis_closed = ").findAll(code).count(),
            "the genesis flag must be written in exactly one place, and it must be the founder's operation"
        )
        // An approval cannot be parked: it expires EXECUTION_WINDOW_MS after the deadline.
        assertTrue(main.contains("val EXECUTION_WINDOW_MS ="), "execution window must be a named constant")
        assertTrue(main.contains("require(op_context.last_block_time < p.deadline + EXECUTION_WINDOW_MS, \"proposal expired\")"))
        // Votes weigh stake, and zero stake cannot propose or vote.
        assertTrue(main.contains("require(weight > 0, \"no voting weight"), "zero stake must not be able to vote")
        assertTrue(main.contains("require(proposer.stake > 0, \"only members with stake may propose\")"))
        // Executed exactly once, flipped in the paying operation.
        val execute = opBody(main, "execute_proposal")
        assertTrue(execute.contains("require(not p.executed, \"proposal already executed\")"))
        assertTrue(execute.contains("update p ( .executed = true );"))
        assertTrue(execute.contains("dao.treasury_balance -= p.amount;"))
        // Every entity/constant the guards need exists as declared state.
        listOf(
            "quorum_weight: integer", "stake_at_creation: integer", "funded_at_creation: integer",
            "mutable yes_weight", "mutable no_weight",
            "mutable executed", "key proposal, voter", "key proposal, owner", "mutable allocated: boolean",
            "mutable allocated: integer", "mutable genesis_closed: boolean", "mutable funded_total: integer",
            "mutable paid_total: integer"
        ).forEach {
            assertTrue(main.contains(it), "governance entities must declare $it")
        }
        // ROUND 13, ONE POCKET AT A TIME. execute_proposal moved points out of the
        // treasury and retired no stake, and there is no unstake, so the same 1000 points
        // could be funded, voted out to their owner and funded again: four turns bought
        // 4000 of voting weight out of 1000 allocated points and then took two honest
        // members' entire stake 5000 to 2000 over a bar of 3500. A payout now retires the
        // weight that backed it, pro rata, so total_stake == treasury_balance at every
        // block and the DAO's whole weight can never pass what genesis allocated.
        assertTrue(execute.contains("retire_stake_backing(p.amount);"), "a payout must retire the stake that was backing it")
        val retire = main.substringAfter("function retire_stake_backing").substringBefore("\n}")
        assertTrue(retire.contains("dao.total_stake -= amount;"), "the retirement must lower the DAO's total weight")
        assertTrue(retire.contains("update m ( .stake -= share );"), "the retirement must lower the stakers' own weight")
        assertTrue(retire.contains("( @sort .owner )"), "the rounding must fall in a canonical order - an unsorted at-expression is not a consensus rule")
        assertEquals(
            1,
            Regex("dao\\.total_stake -= ").findAll(code).count(),
            "stake must be retired in exactly one place, and it must be the payout"
        )
        assertFalse(code.contains("operation unstake"), "there is no unstake: a point leaves the treasury by winning a vote or not at all")
        // The header COUNTS its guards. Round 12 found the stablecoin's count off by one.
        assertTrue(main.contains("Ten guards are STRUCTURAL"), "the governance header must state its guard count")
        assertEquals(10, guardCount(main), "the governance header's stated count must be the number of guards it lists")
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


    /**
     * The eighth template, and the second class (after streaming) whose drain
     * landed with NO template at all. Round 8's AMM existed only because
     * `scaffold_dapp template=amm` redirected to `template=vault`, and the two
     * things that drained it - a sandwich through a caller-chosen slippage
     * tolerance, and just-in-time liquidity around one fee-bearing swap - are
     * both invisible to a static rule. Both guards are pinned here.
     */
    @Test
    fun ammGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("swappool", template = "amm").getValue("src/main.rell")
        val code = withoutComments(main)
        val squashed = code.replace(Regex("\\s+"), " ")

        // GUARD 1: A SWAP NAMES THE EXACT RESERVES IT PRICED AGAINST. The output is a
        // pure function of the amount in and those two numbers, so the swap pays what
        // the caller was quoted or does not happen. A BAND HERE IS THE ROUND-8 DRAIN:
        // a 4000 front-run moves a 500000/500000 pool's RESERVES just 79 bps (round 8's
        // victim's execution fell 144, 83124 -> 81920), and a band is against reserves
        // - so the 2% the victim signed admits it, and so would 0.5%. The attacker
        // picks the front-run size after seeing the width, which is why the guard is
        // equality and not a bound.
        assertTrue(
            squashed.contains(
                "require(quoted_reserve_a == pool.reserve_a and quoted_reserve_b == pool.reserve_b, " +
                    "\"the pool moved since you quoted\");"
            ),
            "the swap must require the QUOTED reserves exactly - a tolerance is the window round 8 sandwiched through"
        )
        // ...and there is no tolerance field anywhere for one to be smuggled back into.
        assertFalse(
            Regex("min_out|max_out|slippage|tolerance").containsMatchIn(code),
            "no slippage parameter may exist in the amm template - naming the reserves pins the output to one number"
        )
        // ONE PLACE A SWAP EXECUTES, so guard 1 and the curve check cannot be present
        // on one direction and missing on the other.
        assertEquals(1, Regex("function execute_swap\\(").findAll(code).count(), "execute_swap must be defined once")
        assertEquals(2, Regex("\\bexecute_swap\\(").findAll(code).count() - 1, "exactly two callers - one per direction")
        listOf("swap_a_for_b", "swap_b_for_a").forEach { op ->
            assertTrue(
                code.contains("operation $op(amount_in: integer, quoted_reserve_a: integer, quoted_reserve_b: integer) {"),
                "$op must take the quoted reserves, A then B in both directions, and nothing else"
            )
            val body = opBody(code, op)
            assertTrue(body.contains("execute_swap("), "$op must go through the one place a swap executes")
            assertFalse(body.contains("pool.reserve"), "$op must not move the reserves itself")
            assertFalse(body.contains("amount_out("), "$op must not price the trade itself")
        }
        // THE CURVE, ENFORCED rather than only asserted in a test: round 8 shipped the
        // k invariant as a passing test; here a swap that would shrink it aborts.
        assertTrue(
            squashed.contains("require(k_of(pool.reserve_a, pool.reserve_b) >= k_before, \"the curve must not lose value\");"),
            "execute_swap must require k not to fall, at runtime"
        )
        val swapFn = code.substringAfter("function execute_swap(").substringBefore("\n}")
        assertTrue(
            swapFn.indexOf("val k_before = k_of(pool.reserve_a, pool.reserve_b);") in
                0 until swapFn.indexOf("require(k_of(pool.reserve_a, pool.reserve_b) >= k_before"),
            "k must be snapshot before the reserves move and compared after"
        )
        assertTrue(
            swapFn.contains("update me ( .token_a -= amount_in, .token_b += out );") &&
                swapFn.contains("pool.reserve_a += amount_in;") && swapFn.contains("pool.reserve_b -= out;"),
            "every credit to a trader is the reserve's debit in the same branch"
        )
        assertTrue(swapFn.contains("require(out < reserve_out, \"output would empty the reserve\");"))

        // GUARD 2: A POSITION IS AN IMMUTABLE ROW WITH A TERM. No mutable share balance
        // exists to top up and shave, so "in before the trade and out after" is not a
        // sentence this module can express.
        val entity = code.substringAfter("entity position {").substringBefore("\n}")
        assertFalse(entity.contains("mutable"), "every field of a position is a term - none may be mutable")
        listOf("owner", "shares", "opened_at", "unlocks_at").forEach { field ->
            assertTrue(Regex("(^|\\n)\\s*(key\\s+|index\\s+)?$field:").containsMatchIn(entity), "the position must declare $field")
            assertFalse(Regex("\\.$field\\s*=(?!=)").containsMatchIn(code), "no operation may write $field after creation")
            assertFalse(Regex("\\.$field\\s*[-+]=").containsMatchIn(code), "no operation may adjust $field after creation")
        }
        assertEquals(1, Regex("create position\\(").findAll(code).count(), "a position is created in exactly one place")
        assertEquals(
            1,
            Regex("(^|\\n)\\s*delete p;").findAll(code).count(),
            "a position is deleted WHOLE in exactly one place - there is no partial burn to shave a fee off with"
        )
        assertTrue(
            code.contains("operation remove_liquidity(position_id: integer) {"),
            "an exit names ONE row and takes it whole; a share amount would be the partial burn back again"
        )

        // THE BLOCK CLOCK IS WRITTEN IN EXACTLY TWO PLACES, both in the create that
        // opens a position. `unlocks_at` is set once and never moved, which is what
        // makes the term something no caller can bring forward.
        assertEquals(
            listOf("opened_at = op_context.last_block_time", "unlocks_at = op_context.last_block_time"),
            Regex("(?<![\\w.])(?<!val )[\\w.]+\\s*=\\s*op_context\\.last_block_time").findAll(code).map { it.value }.toList(),
            "the block clock may be READ anywhere but written only by the create that opens a position"
        )

        val remove = opBody(code, "remove_liquidity")
        assertTrue(remove.contains("require(p.owner == acc.id, \"only the owner may withdraw this position\");"))
        assertTrue(
            remove.contains("require(op_context.last_block_time >= p.unlocks_at, \"liquidity is committed until its term ends\");"),
            "THIS is round 8's JIT drain: without it liquidity can be rented for the length of one trade"
        )
        assertTrue(
            remove.indexOf("require(op_context.last_block_time >= p.unlocks_at") in 0 until remove.indexOf("delete p;"),
            "the term must be checked before the row is destroyed"
        )
        assertTrue(remove.contains("pool.total_shares -= burned;"), "the burn must retire exactly the row's shares")

        val add = opBody(code, "add_liquidity")
        assertTrue(add.contains("require(minted > 0, \"deposit too small to mint a share\");"), "a zero-share mint is refused, never swallowed")
        assertTrue(add.contains("\"the first deposit is too small to seed the pool\""))
        assertTrue(add.contains("\"deposit must match the pool ratio\""))
        assertTrue(
            add.indexOf("update me ( .token_a -= amount_a, .token_b -= amount_b );") in 0 until add.indexOf("create position("),
            "the deposit must leave the provider's balance before the position row that claims it exists"
        )

        // Every bound is a named constant, never a parameter a caller could widen -
        // and a COMMITMENT TERM a caller picks is a term an attacker sets to zero.
        listOf(
            "WELCOME_A", "WELCOME_B", "FEE_NUMERATOR", "FEE_DENOMINATOR",
            "MAX_AMOUNT", "MIN_INITIAL_LIQUIDITY", "COMMITMENT_MS"
        ).forEach {
            assertTrue(code.contains("val $it ="), "$it must be a named constant")
            assertFalse(Regex("operation\\s+\\w+\\s*\\([^)]*\\b$it\\b").containsMatchIn(code), "$it must not be a caller-supplied parameter")
        }
        assertFalse(
            Regex("operation\\s+\\w+\\s*\\([^)]*\\b(term|term_ms|lock_ms|duration|unlocks_at|opened_at)\\b").containsMatchIn(code),
            "no operation may take its own commitment term"
        )

        // CONSERVATION: every row that holds tokens is summed, the live positions add
        // up to what the pool issued, and shares and reserves are empty together.
        listOf("a_in_circulation", "b_in_circulation").forEach {
            val q = code.substringAfter("query $it(): integer {").substringBefore("\n}")
            assertTrue(q.contains("in account @* {}"), "$it must sum the account rows that hold tokens")
            assertTrue(q.contains("pool.reserve"), "$it must include the reserve")
        }
        assertTrue(code.contains("query shares_match_positions(): boolean {"))
        assertTrue(code.contains("query pool_is_shares_backed(): boolean ="))

        // The seams a static rule cannot see are written down where an extender reads.
        assertTrue(main.contains("EXTENDING THIS TEMPLATE"), "the header must tell an extender which invariants are theirs to keep")
        listOf(
            "NEVER ADD A SLIPPAGE TOLERANCE",
            "EVERY NEW WAY OUT OF THE POOL MUST TAKE THE TERM",
            "A POSITION TRANSFER IS THE SUBTLE ONE",
            "a_in_circulation()",
            "shares_match_positions()"
        ).forEach {
            assertTrue(main.contains(it), "the EXTENDING section must name the '$it' seam")
        }

        // The residual list is where an auditor trusts most, so it must state limits
        // rather than imply guards - round 7 drained a build through an inverted one,
        // and round 8 through a header sentence that was simply false.
        assertTrue(main.contains("WHAT THIS TEMPLATE DOES NOT SOLVE"), "the header must state the limits it does not close")
        listOf(
            "PRICE IMPACT IS REAL AND THIS TEMPLATE DOES NOT REMOVE IT",
            "A FRONT-RUN CAN STILL MAKE YOUR SWAP REVERT",
            "A COMMITTED POSITION CANNOT RUN FROM A PRICE MOVE",
            "IMPERMANENT LOSS IS NOT A BUG",
            "A DUST POSITION CAN BE UNBURNABLE",
            "ONE PAIR, TWO TOKENS, NO ORACLE"
        ).forEach {
            assertTrue(main.contains(it), "the residual list must state '$it'")
        }
        // The lending header says a minimum holding period is NOT the fix for a step in
        // pool value, and it is right. This template ships one anyway, against a
        // different shape, so it must reconcile the two IN THE SAME PLACE rather than
        // leave an auditor to discover the contradiction.
        assertTrue(
            main.contains("exit-only attack and decisive against an in-and-out one"),
            "the header must reconcile its term with the lending header's warning about holding periods"
        )
        // And it must say, where the guard is described, that the guard does not abolish
        // price impact - the claim round 8's headers kept getting wrong was the one that
        // sounded like a proof.
        assertTrue(
            main.contains("THEY HAVE CONSENTED TO THE IMPACT"),
            "the sandwich guard must name what it does NOT stop in the same breath as what it does"
        )
        assertFalse(Regex("[0-9A-Fa-f]{64,}").containsMatchIn(main), "no key-like hex in the amm source")
    }

    /**
     * Round 9's drain was an OPERATION - redeem the coin for collateral at par out
     * of the reserve - so the load-bearing guard is an absence, and this test pins
     * it: no operation in the template pays a coin holder collateral except the
     * pro-rata liquidation and the post-settlement redemption, and neither of them
     * reads par. Everything else is the shape of the two operations that do.
     */
    @Test
    fun stablecoinGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("peg", template = "stablecoin").getValue("src/main.rell")
        val code = withoutComments(main)
        // The oracle: configuration, the vault's bounds, never a parameter or a constant.
        assertTrue(main.contains("struct module_args {\n    oracle_pubkey: pubkey;\n}"), "the oracle key must be the module_args struct's only field")
        assertTrue(main.contains("require(op_context.is_signer(chain_context.args.oracle_pubkey), \"oracle only\")"))
        assertFalse(Regex("[0-9A-Fa-f]{64,}").containsMatchIn(main), "no key-like hex in the stablecoin source")
        assertTrue(main.contains("price * BPS <= prev * (BPS + MAX_PRICE_MOVE_BPS)"))
        assertTrue(main.contains(">= MIN_PRICE_UPDATE_INTERVAL_MS, \"price update too soon\")"))
        assertTrue(main.contains("<= MAX_PRICE_AGE_MS,\n        \"price feed is stale\""))
        // NO REDEMPTION AT PAR. The only operations that credit a token_account other
        // than the caller's own deposit coming back are liquidate (pro rata) and
        // redeem_settled (pool share); each is priced from the position or the pool,
        // and no operation converts coin to collateral through the live price.
        val operations = Regex("operation\\s+(\\w+)").findAll(code).map { it.groupValues[1] }.toList()
        assertEquals(
            listOf("set_price", "register_account", "deposit_collateral", "mint_stable", "burn_stable",
                "withdraw_collateral", "liquidate", "settle", "redeem_settled"),
            operations,
            "the template must ship exactly these operations - a redeem/exchange at par is the round-9 drain"
        )
        operations.filter { it !in setOf("liquidate", "redeem_settled", "withdraw_collateral", "settle") }.forEach { op ->
            assertFalse(opBody(code, op).contains("my_tokens ( .balance +="), "$op must not pay collateral to the caller")
        }
        assertFalse(code.contains("* PRICE_SCALE / current_price()"), "no coin-to-collateral conversion at the live price")
        // The peg is the debtor's OWN debt: burn retires the caller's position only.
        val burn = opBody(code, "burn_stable")
        assertTrue(burn.contains("val c = cdp_of(account.id);") && burn.contains("require(c.debt >= amount, \"more than this position owes\");"))
        assertFalse(Regex("operation\\s+burn_stable\\s*\\([^)]*byte_array").containsMatchIn(code), "burn_stable must not name another position")
        // Mint and withdraw: the WHOLE debt against a FRESH price, in the same body.
        val mint = opBody(code, "mint_stable")
        assertTrue(mint.contains("val price = current_price();"))
        assertTrue(mint.contains("require(meets_ratio(c.collateral, c.debt + amount, price), \"under the collateral ratio\");"))
        assertTrue(mint.contains("update c ( .debt += amount );") && mint.contains("update me ( .balance += amount );"), "every minted unit is a unit of the position's debt")
        val withdraw = opBody(code, "withdraw_collateral")
        assertTrue(withdraw.contains("require(meets_ratio(c.collateral - amount, c.debt, price), \"under the collateral ratio\");"))
        // LIQUIDATION: refused while healthy, bonus bounded, and CAPPED AT PRO RATA -
        // the line that makes transaction order worthless.
        val liquidate = opBody(code, "liquidate")
        assertTrue(liquidate.contains("require(not is_healthy(t, price), \"position is healthy\");"))
        assertTrue(liquidate.contains("val pro_rata = t.collateral * stable_in / t.debt;"))
        assertTrue(liquidate.contains("val seize = min(with_bonus, pro_rata);"), "a liquidator must never take more than the position's pro-rata share")
        // ...and the SYSTEM's own backing gates the operation at all, before AND after
        // the seizure. Round 11: the per-position cap held while the system did not, so
        // the bonus came out of the settlement pool every coin holder shares - 15 tokens
        // moved on transaction order, 7 of them from a party to no liquidation.
        // ...and since round 12 that line is the BONUS, not solvency: a floor at 100%
        // left [100%, 105%) live, and at 102.5% backing the same liquidation moved twelve
        // tokens on transaction order with seven of them out of a party to nothing.
        assertTrue(
            liquidate.contains("collateral_value(system.total_collateral, price) * BPS\n            >= system.total_debt * (BPS + LIQUIDATION_BONUS_BPS)"),
            "liquidation must be refused unless the system is worth its coin PLUS the bonus it is about to pay"
        )
        assertTrue(
            liquidate.contains("collateral_value(system.total_collateral - seize, price) * BPS\n                >= (system.total_debt - stable_in) * (BPS + LIQUIDATION_BONUS_BPS)"),
            "and refused when it would LEAVE the system inside the bonus of insolvency"
        )
        assertFalse(
            liquidate.contains("collateral_value(system.total_collateral, price) >= system.total_debt\n"),
            "round 12: a floor at 100% is a live band up to the bonus rate"
        )
        assertTrue(
            liquidate.contains("\"system is under-backed - settle instead of liquidating\""),
            "the refusal must name settle() as the exit - it is the only one left"
        )
        assertTrue(liquidate.contains("require(target != account.id, \"cannot liquidate your own position\");"))
        assertTrue(liquidate.contains("update t ( .debt -= stable_in, .collateral -= seize );"))
        // SETTLEMENT: only an insolvent system, surplus back to owners, one pool, one
        // rate; and it stops everything else.
        val settle = opBody(code, "settle")
        assertTrue(settle.contains("collateral_value(system.total_collateral, price) < system.total_debt,\n        \"system is solvent\""), "a solvent system must not be freezable")
        assertTrue(settle.contains("val owed = min(c.collateral, c.debt * PRICE_SCALE / price);"))
        assertTrue(settle.contains("settlement.settled = true;"))
        listOf("deposit_collateral", "mint_stable", "burn_stable", "withdraw_collateral", "liquidate", "settle", "set_price").forEach { op ->
            assertTrue(opBody(code, op).contains("live();"), "$op must refuse to run after settlement")
        }
        val redeem = opBody(code, "redeem_settled")
        assertTrue(redeem.contains("require(settlement.settled, \"system is not settled\");"))
        assertTrue(redeem.contains("val tokens_out = stable_in * settlement.pool / settlement.supply;"), "post-settlement redemption must be the same share for every coin")
        // The reserve is a row of the users' entity keyed by the chain's own id.
        assertTrue(main.contains("function vault_id(): byte_array = chain_context.blockchain_rid;"))
        // Constants, not parameters.
        listOf("MIN_COLLATERAL_RATIO_BPS", "LIQUIDATION_RATIO_BPS", "LIQUIDATION_BONUS_BPS", "MAX_PRICE_MOVE_BPS", "MAX_AMOUNT").forEach {
            assertTrue(main.contains("val $it ="), "$it must be a named constant")
        }
        // The header admits what the template cannot fix, and names the RIGHT band: the
        // loss is created before the system goes under-backed, not after.
        assertTrue(main.contains("What no template can fix: a price that falls faster than liquidators act still"))
        assertTrue(
            main.contains("the band that matters starts at 105% BACKING, NOT AT 100%"),
            "the residual must name the window the guard actually opens, which round 12 measured"
        )
        assertTrue(main.contains("Seven guards are STRUCTURAL"), "the stablecoin header must state its guard count")
        assertEquals(7, guardCount(main), "round 12: this header said six and listed seven")
    }

    /**
     * The eleventh template, and the class `docs/TEMPLATE-GAPS.md` ranked first. Round
     * 12 built an order book from this server's own two sentences - the marketplace's
     * immutable escrow row, and "an order that can be pulled in the block it would have
     * been filled in is not a commitment at all" - implemented both literally, and
     * drained it: with no mutable field a partial fill is delete-and-recreate, so the
     * remainder's created_at was NOW and any counterparty restarted the maker's cancel
     * clock with one unit an hour. The guards below are what makes that unwritable.
     */
    @Test
    fun exchangeGuardsAreStructuralNotOptional() {
        val main = DappScaffold.files("book", template = "exchange").getValue("src/main.rell")
        val code = withoutComments(main)
        // IMMUTABLE TERMS, ONE MUTABLE COUNTER. Round 12's build had NO mutable field,
        // which is what forced the recreate; carrying created_at through the recreate
        // instead is the other pinned drain (r9-amm-position-transfer-sells-the-jit).
        val orderEntity = main.substringAfter("entity order {").substringBefore("\n}")
        assertEquals(1, Regex("mutable ").findAll(orderEntity).count(), "an order row must have exactly ONE mutable field: $orderEntity")
        assertTrue(orderEntity.contains("mutable filled: integer = 0;"), orderEntity)
        listOf("is_buy: boolean;", "price: integer;", "qty: integer;", "created_at: timestamp;").forEach {
            assertTrue(orderEntity.contains(it), "an order's terms must be immutable: $it")
        }
        assertFalse(orderEntity.contains("mutable created_at"), "the maker's clock is a term, not a counter")
        assertFalse(orderEntity.contains("escrow"), "the escrow must be derived from the terms, never a field that can drift")
        // THE ROW IS NEVER RE-CREATED. This single assertion is the round-12 fix.
        assertEquals(1, Regex("create order\\(").findAll(code).count(), "a partial fill must not delete and re-create the order - that recreate IS the drain")
        assertEquals(1, Regex("created_at = op_context").findAll(code).count(), "created_at must be written exactly once, by place_order")
        assertTrue(opBody(code, "place_order").contains("created_at = op_context.last_block_time"))
        assertTrue(code.contains("update o ( .filled = new_filled );"), "a fill must write the counter and nothing else")
        assertFalse(code.contains(".filled = new_filled, .created_at"), "a fill must not touch the maker's clock")
        // NO CALLER NAMES A COUNTERPARTY: there is no fill_order(id) at all.
        assertEquals(
            listOf("register_trader", "place_order", "cancel_order"),
            Regex("operation\\s+(\\w+)").findAll(code).map { it.groupValues[1] }.toList(),
            "the template must ship exactly these operations - an operation that names a resting order is the ordering problem handed to the caller"
        )
        assertFalse(Regex("operation\\s+fill_order").containsMatchIn(main), "no operation may name the order it fills")
        assertFalse(opBody(code, "place_order").contains("order_id"), "a taker gives a side, a limit and a size")
        // ...and the book's own priority is price, then time, then id.
        assertTrue(main.contains("return if (a.is_buy) a.price > b.price else a.price < b.price;"), "best price first")
        assertTrue(main.contains("if (a.created_at != b.created_at) return a.created_at < b.created_at;"), "then the order that has rested longest")
        assertTrue(main.contains("return a.id < b.id;"), "then the lowest id")
        assertTrue(main.contains("o.maker != taker"), "the matcher must skip a taker's own orders")
        // RESTED CANCEL, measured from the clock nothing writes twice.
        val cancel = opBody(code, "cancel_order")
        assertTrue(cancel.contains("require(o.maker == account.id, \"not your order\");"))
        assertTrue(cancel.contains("op_context.last_block_time - o.created_at >= MIN_RESTING_MS"))
        assertTrue(main.contains("val MIN_RESTING_MS ="), "the resting period must be a named constant")
        assertFalse(Regex("operation\\s+place_order\\s*\\([^)]*resting").containsMatchIn(main), "the resting period must not be a parameter")
        // ESCROWED AT REST, and derived from the terms.
        assertTrue(main.contains("function escrow_of(o: order): integer =\n    if (o.is_buy) o.price * remaining(o) else remaining(o);"))
        assertTrue(opBody(code, "place_order").contains("update me ( .points -= escrow );") && opBody(code, "place_order").contains("update me ( .units -= escrow );"))
        // Bounds before any multiplication, the default auth flag, and the header's count.
        listOf("MAX_PRICE", "MAX_QTY", "WELCOME_POINTS", "WELCOME_UNITS").forEach {
            assertTrue(main.contains("val $it ="), "$it must be a named constant")
        }
        assertTrue(main.contains("Eight guards are STRUCTURAL"), "the exchange header must state its guard count")
        assertEquals(8, guardCount(main), "the exchange header's stated count must be the number of guards it lists")
        assertTrue(main.contains("A RESTING ORDER IS A FREE OPTION WRITTEN TO THE MARKET FOR MIN_RESTING_MS"), "the header must admit what the cancel delay costs")
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
                if (template == "governance") {
                    // The DAO's founder key is configured exactly the way the oracle is:
                    // unset in production so the chain cannot build with a placeholder,
                    // FT4's published test key under test: so the shipped tests can
                    // countersign a genesis claim.
                    val uncommentedFounder = production.lineSequence().filter { !it.trimStart().startsWith("#") }
                        .any { it.contains("founder_pubkey") }
                    assertFalse(uncommentedFounder, "governance production yml must not set a placeholder founder key")
                    assertTrue(production.contains("#   founder_pubkey: x\"<your founder public key>\""), "governance yml must tell the deployer where the founder key goes")
                    assertTrue(testBlock.contains("    main:\n      founder_pubkey: x\"${DappScaffold.TEST_ADMIN_PUBKEY}\""), "governance test.moduleArgs must wire the founder test key")
                    assertEquals(
                        DappScaffold.TEST_ADMIN_PUBKEY,
                        DappScaffold.governanceTestModuleArgs().getValue("main").getValue("founder_pubkey").toString().trim('"'),
                        "governanceTestModuleArgs must mirror the yml"
                    )
                }
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
    private fun runShipped(
        template: String,
        label: String = template,
        files: Map<String, String> = rellOf(template),
        tests: List<String> = emptyList()
    ): RunRellTests.Result {
        val result = RunRellTests.run(files, databaseUrl = dbUrl, moduleArgs = moduleArgsOf(template), timeoutSeconds = SHIPPED_SUITE_TIMEOUT_SECONDS, tests = tests)
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
            "test_majority_of_stake_can_reject",
            "test_round10_parked_cheap_quorum_drain_must_fail",
            "test_approved_proposal_expires_unexecuted",
            "test_r11_free_stake_sybil_takeover_must_fail",
            "test_r11_two_point_stake_cannot_veto_an_approved_proposal",
            "test_r12_one_point_of_stake_cannot_freeze_the_treasury_must_fail",
            "test_r12_first_claimant_cannot_shut_the_genesis_window_must_fail",
            "test_r12_a_re_proposal_does_not_buy_a_fresh_window",
            "test_r13_restaked_payout_cannot_compound_voting_weight_must_fail",
            "test_r13_a_payout_retires_the_stake_that_backed_it"
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
            "test_round9_subsecond_grind_must_fail",
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
            "test_interest_moves_only_from_borrower_to_lender",
            "test_round9_liquidation_may_not_manufacture_insolvency_must_fail",
            "test_underwater_liquidation_is_value_for_value_with_no_bonus"
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

    @Test
    fun ammShippedTestsRunGreen() = assertShippedGreen(
        "amm",
        setOf(
            "test_round8_swap_sandwich_must_fail",
            "test_price_impact_is_documented_not_enforced",
            "test_round8_jit_liquidity_capture_must_fail",
            "test_liquidity_returns_to_its_provider_after_its_term",
            "test_k_never_falls_under_grinding",
            "test_first_depositor_inflation_refuses_instead_of_swallowing",
            "test_round9_dust_pool_deposit_is_refused_not_haircut",
            "test_bounds_and_ownership"
        )
    )

    @Test
    fun stablecoinShippedTestsRunGreen() = assertShippedGreen(
        "stablecoin",
        setOf(
            "test_round9_redemption_at_par_out_of_a_shortfall_must_fail",
            "test_round9_control_order_does_not_change_the_outcome",
            "test_round9_settlement_shares_the_shortfall_in_any_order",
            "test_settlement_returns_surplus_to_its_owner",
            "test_liquidation_is_bounded_and_never_worsens_a_position",
            "test_mint_and_withdraw_are_ratio_checked_at_a_fresh_price",
            "test_r11_liquidation_out_of_the_settlement_pool_must_fail",
            "test_r11_settling_first_pays_the_same_three_numbers",
            "test_r12_liquidation_inside_the_bonus_band_must_fail",
            "test_r12_settling_the_47_00_fixture_pays_the_same_three_numbers"
        )
    )

    @Test
    fun exchangeShippedTestsRunGreen() = assertShippedGreen(
        "exchange",
        setOf(
            "test_place_and_fill_conserve_both_assets",
            "test_round12_partial_fill_cannot_reset_the_makers_clock_must_fail",
            "test_round12_a_maker_is_never_left_resting_beside_a_better_price_must_fail",
            "test_matching_is_price_then_time_and_no_caller_chooses",
            "test_bounds_and_ownership"
        )
    )

    /** What the Rell runner reports when a run_must_fail transaction succeeds - the attack landed. */
    private val attackLanded = "did not fail"

    /**
     * The governance bar as shipped since round 11: the quorum of the stake that
     * existed WHEN THE PROPOSAL WAS CREATED, and nothing else. Removing it, and
     * putting round 10's live term back, are two of the four mutants below.
     */
    private val GOVERNANCE_CREATION_QUORUM_GUARD =
        "require(p.yes_weight + p.no_weight >= p.quorum_weight, \"quorum not reached\");"

    /**
     * Round 10's parked cheap quorum, closed at the money instead of at the bar:
     * a proposal reserves what it may spend when it is created. (The template is
     * trimIndent-ed, so the operation body sits at four spaces.)
     */
    private val GOVERNANCE_COMMITTED_TREASURY_GUARD = listOf(
        "    require(",
        "        amount <= dao.treasury_balance - committed_treasury(now) - committed_by(now, account.id),",
        "        \"amount exceeds the uncommitted treasury\"",
        "    );"
    ).joinToString("\n")

    /**
     * Round 12's freeze, in one term: only an APPROVED proposal reserves the treasury
     * against other members. The shipped line before it counted every row that was
     * `not .executed`, which is what let one point of stake out of 2001 hold the whole
     * treasury by losing a vote 2000 to 1 - and renew it for ever.
     */
    private val GOVERNANCE_APPROVED_ONLY_TERM =
        "if (now < p.deadline + EXECUTION_WINDOW_MS and is_approved(p, now)) total += p.amount;"

    /**
     * The guard that actually closes round 10, and the reason the reservation above can
     * afford to release on a defeat: two monotone counters, O(1), no scan to size.
     */
    private val GOVERNANCE_VINTAGE_GUARD = listOf(
        "    require(",
        "        dao.paid_total + p.amount <= p.funded_at_creation,",
        "        \"proposal cannot be paid out of money that arrived after it\"",
        "    );"
    ).joinToString("\n")

    /**
     * Round 12: repetition buys nothing. It is a SECOND refusal of round 10's
     * "pay me twice", which asks for the same payment as "pay me" - so the round-10
     * mutant below has to strip this too before that attack can land, and this guard
     * carries a mutant of its own rather than riding on the reservation's back.
     */
    private val GOVERNANCE_RE_PROPOSAL_GUARD = listOf(
        "    require(",
        "        not has_live_claim(now, account.id, beneficiary, amount),",
        "        \"an identical proposal is still live\"",
        "    );"
    ).joinToString("\n")

    /** Round 12: the genesis phase and the live phase are strictly ordered. */
    private val GOVERNANCE_GENESIS_PHASE_GUARD =
        "require(dao.genesis_closed, \"genesis allocation is still open\");"

    /** The window as round 11 shipped it: shut by the first member to stake a point. */
    private val GOVERNANCE_STAKE_CLOSES_GENESIS_MUTANT =
        "require(dao.total_stake == 0, \"genesis allocation is closed\");"


    /** Registration as round 11 found it: a permissionless mint of voting weight. */
    private val GOVERNANCE_FREE_GRANT_MUTANT = "create member(owner = account.id, balance = WELCOME_POINTS);"

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
        // Only the exploit case runs: the verdict below reads exactly one case,
        // and the environmental failures it rules out (module args, compile,
        // schema) hit every case alike, so one case sees them as surely as ten.
        // Running the whole suite per mutant was 60 x a full suite - 2148 of the
        // gate's 2380 seconds (gate #17, 2026-09-04) - and pushed the test task
        // against its 45-minute CI budget. The shipped suites still run whole,
        // once each, in the *ShippedTestsRunGreen tests.
        val mutant = runShipped(template, label = "$template-without[${guard.take(48)}]", files = files, tests = listOf(exploitTest))
        // The mutant must still be a RUNNING dapp: no case may fail for an
        // environmental reason (module args, compile, schema) - that is the
        // vacuous-mutant failure mode, exactly what the first vault mutants did
        // while looking green.
        mutant.cases.forEach {
            val e = it.error.orEmpty()
            assertFalse(
                e.contains("Unable to create GTX module") || e.contains("do not compile") || e.contains("Missing metadata"),
                "$template mutant failed for an environmental reason, proving nothing about the guard: ${it.name} - $e"
            )
        }
        // A bare `single {}` here threw NoSuchElementException with no context when
        // the runner hit its 90s deadline on a starved box (gate #9, 2026-09-04,
        // 0.4 GB free RAM): name the cases that DID come back and the runner's notes,
        // so a timeout reads as a timeout and not as a missing test.
        val case = mutant.cases.singleOrNull { it.name.endsWith(exploitTest) }
        assertNotNull(
            case,
            "$template: the mutant run returned ${mutant.cases.size} case(s) [${mutant.cases.joinToString { it.name }}]" +
                " and none is $exploitTest - the runner did not finish (ok=${mutant.ok}; notes: ${mutant.notes})"
        )
        case!!
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
        GOVERNANCE_CREATION_QUORUM_GUARD,
        "test_round1_single_account_drain_must_fail",
        "quorum not reached"
    )

    /**
     * Round 11's veto: put round 10's LIVE term back and the approved payout the
     * replay executes is refused for ever by two points of stake posted after
     * voting closed. The replay goes red on "quorum not reached" - which here IS
     * the attack landing, because the attack is a refusal that should not happen.
     * Nothing else in the replay moves: the proposal's own quorum_weight is still
     * 1000 under the mutant, so only the bar read at execution can be what changed.
     */
    @Test
    fun governanceVetoReplayGoesRedWithRound10sLiveQuorumTerm() = assertGuardMutationRedensExploitTest(
        "governance",
        GOVERNANCE_CREATION_QUORUM_GUARD,
        "require(p.yes_weight + p.no_weight >= max(p.quorum_weight, quorum_for(dao.total_stake)), \"quorum not reached\");",
        "test_r11_two_point_stake_cannot_veto_an_approved_proposal",
        "proposal expired",
        "quorum not reached"
    )

    /**
     * Round 10, at the money instead of the bar: drop the reservation and the second
     * cheap approval can be created against a treasury the first has already claimed -
     * 100 of them against a treasury of 1000, which is the drain. The replay's must-fail
     * stops failing. Round 12 added a SECOND refusal of that same step - "pay me twice"
     * asks for the same payment as "pay me", so the re-proposal rule catches it once the
     * reservation is gone - and the mutant has to strip both, or it would report an
     * attack still refused as an attack landed.
     */
    @Test
    fun governanceRound10ReplayGoesRedWithoutTheCommittedTreasuryGuard() = assertGuardRemovalRedensExploitTest(
        "governance",
        GOVERNANCE_COMMITTED_TREASURY_GUARD,
        "test_round10_parked_cheap_quorum_drain_must_fail",
        "only members with stake may propose",
        alsoRemove = listOf(GOVERNANCE_RE_PROPOSAL_GUARD)
    )

    /**
     * ...and that second guard on its own: strip it and the attacker may say the same
     * thing again while her first attempt is still inside its window, which is how the
     * round-12 freeze renewed itself. The replay's must-fail stops failing, and the
     * reservation and the vintage guard are both still in place so neither can be what
     * went red.
     */
    @Test
    fun governanceR12ReplayGoesRedWhenAReProposalBuysAFreshWindow() = assertGuardRemovalRedensExploitTest(
        "governance",
        GOVERNANCE_RE_PROPOSAL_GUARD,
        "test_r12_a_re_proposal_does_not_buy_a_fresh_window",
        "amount exceeds the uncommitted treasury"
    )

    /**
     * Round 11's sybil takeover: put the free welcome grant back - the exact line
     * the template shipped through round 10 - and registration mints voting weight
     * again, so the sybil's fund_treasury stops being refused. Nothing else is
     * touched: the founder-countersigned claim, its cap and its closing condition
     * all still stand, so the mint is the only thing that can have changed.
     */
    @Test
    fun governanceSybilReplayGoesRedWhenRegistrationMintsPoints() = assertGuardMutationRedensExploitTest(
        "governance",
        "create member(owner = account.id);",
        GOVERNANCE_FREE_GRANT_MUTANT,
        "test_r11_free_stake_sybil_takeover_must_fail",
        "only members with stake may propose",
        attackLanded
    )

    /**
     * Round 13's stake ratchet: take the retirement out and a payout stops retiring the
     * weight that backed it - which is exactly the template round 13 attacked. Four turns
     * of fund-vote-execute then buy 4000 of voting weight out of 1000 allocated points,
     * and the 2000-point proposal the replay REQUIRES to be refused is carried 5000 to
     * 2000 over a bar of 3500, so run_must_fail reports that the transaction did not fail.
     * Nothing else moves: the vintage rule, the reservation, the bar fixed at creation and
     * the genesis cap all still stand, so only the retirement can be what changed - and
     * the replay asserts nothing before the refusal, so it cannot redden for another reason.
     */
    @Test
    fun governanceR13ReplayGoesRedWhenAPayoutRetiresNoStake() = assertGuardRemovalRedensExploitTest(
        "governance",
        "retire_stake_backing(p.amount);",
        "test_r13_restaked_payout_cannot_compound_voting_weight_must_fail",
        "proposal was not approved"
    )

    /** The approval cannot be parked either: strip the window and the expiry replay goes red. */
    @Test
    fun governanceExpiryReplayGoesRedWithoutTheExecutionWindow() = assertGuardRemovalRedensExploitTest(
        "governance",
        "require(op_context.last_block_time < p.deadline + EXECUTION_WINDOW_MS, \"proposal expired\");",
        "test_approved_proposal_expires_unexecuted",
        "proposal expired"
    )

    @Test
    fun governanceExploitTestGoesRedWithoutTheStakeGuard() = assertGuardRemovalRedensExploitTest(
        "governance",
        "require(proposer.stake > 0, \"only members with stake may propose\");",
        "test_round1_single_account_drain_must_fail",
        "only members with stake may propose"
    )

    /**
     * THE ROUND-9 DRAIN, PUT BACK where the template lets it be put back. The
     * template has no redeem-at-par to delete - the guard is an ABSENCE - so the
     * mutant re-creates the shape in the one operation that pays coin holders
     * after a crash: post-settlement redemption at PAR out of the pool, first come
     * first served, instead of the same share for every coin. The attacker's 6666
     * then buys 130 tokens of a 200 pool and the honest holder is left 70 - round
     * 9's exact numbers - and the replay's 100/100 assertion trips.
     */
    @Test
    fun stablecoinRound9ReplayGoesRedWhenSettledCoinRedeemsAtPar() = assertGuardMutationRedensExploitTest(
        "stablecoin",
        "val tokens_out = stable_in * settlement.pool / settlement.supply;",
        "val tokens_out = min(stable_in * PRICE_SCALE / settlement.price, settlement.pool - settlement.paid);",
        "test_round9_settlement_shares_the_shortfall_in_any_order",
        "vault cannot cover the redemption",
        "expected"
    )

    /**
     * Drop the pro-rata cap and a liquidator is paid the bonus rate out of a
     * position that cannot afford it - the order-dependent overpayment round 9 was
     * built on. The replay this reddens moved with round 11: the round-9 setup no
     * longer liquidates at all (its system is 77% backed, so the operation is
     * refused), and the cap is now exercised where the SYSTEM is sound and the
     * position is not - bob takes 15 tokens of alice's collateral for 1000 of coin,
     * where the bonus alone would pay 20. Without the cap he takes 20 and the 15
     * assertion trips.
     */
    @Test
    fun stablecoinRound9ReplayGoesRedWithoutTheProRataCap() = assertGuardMutationRedensExploitTest(
        "stablecoin",
        "val seize = min(with_bonus, pro_rata);",
        "val seize = with_bonus;",
        "test_settlement_returns_surplus_to_its_owner",
        "vault cannot cover the liquidation",
        "expected"
    )

    /** Let a SOLVENT system be settled and anyone can freeze every position at a price of their choosing. */
    @Test
    fun stablecoinSettlementTestGoesRedWhenASolventSystemCanBeSettled() = assertGuardMutationRedensExploitTest(
        "stablecoin",
        "collateral_value(system.total_collateral, price) < system.total_debt,\n        \"system is solvent\"",
        "collateral_value(system.total_collateral, price) < system.total_debt or true,\n        \"system is solvent\"",
        "test_round9_settlement_shares_the_shortfall_in_any_order",
        "price feed is stale",
        attackLanded
    )

    /** Without the health check a healthy position can be liquidated for the bonus. */
    @Test
    fun stablecoinLiquidationTestGoesRedWithoutTheHealthCheck() = assertGuardMutationRedensExploitTest(
        "stablecoin",
        "require(not is_healthy(t, price), \"position is healthy\");",
        "require(not is_healthy(t, price) or true, \"position is healthy\");",
        "test_liquidation_is_bounded_and_never_worsens_a_position",
        "amount too small",
        attackLanded
    )

    /** Without the ratio check on mint the coin is minted against nothing - the unbacked mint. */
    @Test
    fun stablecoinRatioTestGoesRedWithoutTheMintRatioCheck() = assertGuardMutationRedensExploitTest(
        "stablecoin",
        "require(meets_ratio(c.collateral, c.debt + amount, price), \"under the collateral ratio\");",
        "require(meets_ratio(c.collateral, c.debt + amount, price) or true, \"under the collateral ratio\");",
        "test_mint_and_withdraw_are_ratio_checked_at_a_fresh_price",
        "price feed not initialised",
        attackLanded
    )

    /**
     * The round-11 guard as shipped, verbatim (the template is trimIndent-ed, so
     * the operation body sits at four spaces). Removing it is the mutant below.
     */
    private val STABLECOIN_SYSTEM_BACKING_GUARD = listOf(
        "    require(",
        "        collateral_value(system.total_collateral, price) * BPS",
        "            >= system.total_debt * (BPS + LIQUIDATION_BONUS_BPS)",
        "            and collateral_value(system.total_collateral - seize, price) * BPS",
        "                >= (system.total_debt - stable_in) * (BPS + LIQUIDATION_BONUS_BPS),",
        "        \"system is under-backed - settle instead of liquidating\"",
        "    );"
    ).joinToString("\n")

    /** The same guard as round 11 shipped it: a floor at 100%, with the bonus at 105%. */
    private val STABLECOIN_ROUND11_100PCT_FLOOR = listOf(
        "    require(",
        "        collateral_value(system.total_collateral, price) >= system.total_debt",
        "            and collateral_value(system.total_collateral - seize, price) >= system.total_debt - stable_in,",
        "        \"system is under-backed - settle instead of liquidating\"",
        "    );"
    ).joinToString("\n")

    /**
     * Round 11: take the system-backing guard out and the liquidation the replay
     * requires to be refused lands again - trudy repays 3000 of bob's debt and
     * takes 70 tokens out of a 256-token settlement pool while retiring 3000 of a
     * 13756 supply, which is worth 15 tokens to her for choosing the order of two
     * operations she is entitled to perform. The replay's first must-fail stops
     * failing, which is exactly the attack landing; the pro-rata cap and the
     * health check are both still in place, so neither can be what went red.
     */
    @Test
    fun stablecoinRound11ReplayGoesRedWithoutTheSystemBackingGuard() = assertGuardRemovalRedensExploitTest(
        "stablecoin",
        STABLECOIN_SYSTEM_BACKING_GUARD,
        "test_r11_liquidation_out_of_the_settlement_pool_must_fail",
        "position is healthy"
    )

    /**
     * ROUND 12, THE FREEZE. Put back the term that counted every unexecuted proposal and
     * trudy's DEFEATED 2001-point proposal reserves the whole treasury again: the honest
     * majority's own payout cannot be CREATED, which is the attack landing - the drain
     * here is a refusal that should not happen, exactly as in the round-11 veto replay
     * above. Nothing else moves: the vintage guard, the reservation and the re-proposal
     * refusal are all still in place, so only the approved-only term can be what changed.
     */
    @Test
    fun governanceR12ReplayGoesRedWhenARejectedProposalKeepsItsReservation() = assertGuardMutationRedensExploitTest(
        "governance",
        GOVERNANCE_APPROVED_ONLY_TERM,
        "if (now < p.deadline + EXECUTION_WINDOW_MS) total += p.amount;",
        "test_r12_one_point_of_stake_cannot_freeze_the_treasury_must_fail",
        "only members with stake may propose",
        "amount exceeds the uncommitted treasury"
    )

    /**
     * ROUND 12, THE GENESIS WINDOW. Take the phase gate off fund_treasury and put the
     * round-11 closing condition back into claim_allocation, and the first claimant's one
     * point of stake shuts the mint on everybody again: the replay's must-fail stops
     * failing, which is the attack landing. The founder's close_genesis() is untouched and
     * carries its own message, so it cannot be what went red.
     */
    @Test
    fun governanceR12ReplayGoesRedWhenAMembersStakeShutsTheGenesisWindow() = assertGuardMutationRedensExploitTest(
        "governance",
        GOVERNANCE_GENESIS_PHASE_GUARD,
        "",
        "test_r12_first_claimant_cannot_shut_the_genesis_window_must_fail",
        "the founder must sign the genesis close",
        attackLanded,
        alsoReplace = listOf(
            "require(not dao.genesis_closed, \"genesis allocation is closed\");" to GOVERNANCE_STAKE_CLOSES_GENESIS_MUTANT
        )
    )

    /**
     * ROUND 12, THE BONUS BAND. Restore the floor at 100% and the liquidation the replay
     * requires to be refused lands again at 102.5% backing: trudy takes 67 tokens for 3000
     * of coin, the pool falls 256 to 190 against a supply of 10756, and the three parties
     * settle 101 / 117 / 81 instead of 89 / 124 / 86. The pro-rata cap and the health
     * check are both still in place, so neither can be what went red.
     */
    @Test
    fun stablecoinR12ReplayGoesRedWithTheFloorBackAtOneHundredPercent() = assertGuardMutationRedensExploitTest(
        "stablecoin",
        STABLECOIN_SYSTEM_BACKING_GUARD,
        STABLECOIN_ROUND11_100PCT_FLOOR,
        "test_r12_liquidation_inside_the_bonus_band_must_fail",
        "position is healthy",
        attackLanded
    )

    /**
     * ROUND 12, THE MAKER'S CLOCK. This is the round-12 build's own shape, put back: make
     * created_at mutable and let the fill write it, and three one-unit fills push the
     * maker's cancel out past the hour she started - her cancel is refused where the
     * replay requires it to succeed, which is the attack landing. `filled` still advances
     * and the escrow still comes back, so nothing but the clock changed.
     */
    @Test
    fun exchangeR12ReplayGoesRedWhenAFillRewritesTheMakersClock() = assertGuardMutationRedensExploitTest(
        "exchange",
        "update o ( .filled = new_filled );",
        "update o ( .filled = new_filled, .created_at = op_context.last_block_time );",
        "test_round12_partial_fill_cannot_reset_the_makers_clock_must_fail",
        "not your order",
        "the order has not rested long enough",
        alsoReplace = listOf("created_at: timestamp;" to "mutable created_at: timestamp;")
    )

    /**
     * ...and the other half of an order book: take price priority out of the matcher and
     * the taker is filled at 12 where the book showed 10, so the assertion that alice's
     * order - the best price - is the one that moved trips. Nobody chose that: the point
     * is that the RULE decides, and a rule that is not price-first is a different venue.
     */
    @Test
    fun exchangeMatchingTestGoesRedWithoutPricePriority() = assertGuardMutationRedensExploitTest(
        "exchange",
        "return if (a.is_buy) a.price > b.price else a.price < b.price;",
        "return a.id < b.id;",
        "test_matching_is_price_then_time_and_no_caller_chooses",
        "insufficient points",
        "expected"
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
        val mutant = runShipped("vault", label = "vault-without-lower-bound-only", files = files, tests = listOf("test_round1_price_crash_must_fail"))
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

    // ------------------------------------------------------------------------
    // ROUND 8, dapp_c_amm: the two drains the amm template makes unwritable, and
    // the mutants that prove each guard is load-bearing rather than decorative.
    // ------------------------------------------------------------------------

    /**
     * ROUND 8'S SANDWICH, and it is the absence of a tolerance field. Delete the
     * equality on the quoted reserves and the victim's stale-quote swap executes at
     * the price the front-run created - exactly what happened when the guard was a
     * caller-chosen min_out.
     */
    @Test
    fun ammSandwichTestGoesRedWithoutTheQuotedReserveGuard() = assertGuardRemovalRedensExploitTest(
        "amm",
        "require(quoted_reserve_a == pool.reserve_a and quoted_reserve_b == pool.reserve_b, \"the pool moved since you quoted\");",
        "test_round8_swap_sandwich_must_fail",
        // Wrong reason would be the replay failing to set itself up at all.
        "register an account first"
    )

    /**
     * ...and A BAND IS NOT A GUARD, which is the whole point of the equality. This
     * mutant does not delete anything: it replaces the exact match with the 2%
     * tolerance round 8's victim actually signed, which is the number a competent
     * author writes when they think they are being careful. A 4000 front-run on a
     * 500000/500000 pool moves the RESERVES only 79 bps - a band is compared against
     * the reserves, so 2% admits it with room to spare and so would 0.5%. The victim's
     * swap executes, and the sandwich lands again. There is no safe width, because the
     * attacker sizes the front-run AFTER reading the width out of the caller's own
     * transaction.
     */
    @Test
    fun ammSandwichTestGoesRedWhenTheQuoteBecomesATolerance() = assertGuardMutationRedensExploitTest(
        "amm",
        "require(quoted_reserve_a == pool.reserve_a and quoted_reserve_b == pool.reserve_b, \"the pool moved since you quoted\");",
        "require(quoted_reserve_a * 98 / 100 <= pool.reserve_a and quoted_reserve_b * 98 / 100 <= pool.reserve_b, \"the pool moved since you quoted\");",
        "test_round8_swap_sandwich_must_fail",
        "register an account first",
        attackLanded
    )

    /**
     * ROUND 8'S JIT LIQUIDITY, and it is one require(). Take the term away and the
     * attacker's deposit-before-the-trade / withdrawal-after is allowed again: the
     * fee is collected by capital that carried the price risk for one block.
     */
    @Test
    fun ammJitTestGoesRedWithoutTheCommitmentTerm() = assertGuardRemovalRedensExploitTest(
        "amm",
        "require(op_context.last_block_time >= p.unlocks_at, \"liquidity is committed until its term ends\");",
        "test_round8_jit_liquidity_capture_must_fail",
        "only the owner may withdraw this position"
    )

    /** Take away the owner check and anyone can burn anyone's position for its reserves. */
    @Test
    fun ammPositionTestGoesRedWithoutTheOwnerCheck() = assertGuardRemovalRedensExploitTest(
        "amm",
        "require(p.owner == acc.id, \"only the owner may withdraw this position\");",
        "test_bounds_and_ownership",
        "liquidity is committed until its term ends"
    )

    /** Drop the minimum seed and the first-depositor inflation steal starts. */
    @Test
    fun ammInflationTestGoesRedWithoutTheMinimumSeed() = assertGuardMutationRedensExploitTest(
        "amm",
        "require(amount_a >= MIN_INITIAL_LIQUIDITY and amount_b >= MIN_INITIAL_LIQUIDITY, \"the first deposit is too small to seed the pool\");",
        "require(amount_a >= 1 and amount_b >= 1, \"the first deposit is too small to seed the pool\");",
        "test_first_depositor_inflation_refuses_instead_of_swallowing",
        "deposit too small to mint a share",
        attackLanded
    )

    /** Swallow a zero-share deposit instead of refusing it and the victim's money is gone. */
    @Test
    fun ammInflationTestGoesRedWithoutTheZeroShareRefusal() = assertGuardMutationRedensExploitTest(
        "amm",
        "require(minted > 0, \"deposit too small to mint a share\");",
        "require(minted >= 0, \"deposit too small to mint a share\");",
        "test_first_depositor_inflation_refuses_instead_of_swallowing",
        "the first deposit is too small to seed the pool",
        attackLanded
    )

    /** Widen the ratio check and an unbalanced deposit is silently donated to the LPs. */
    @Test
    fun ammBoundsTestGoesRedWithoutTheBalancedDepositGuard() = assertGuardMutationRedensExploitTest(
        "amm",
        "require(amount_b.to_big_integer() == need, \"deposit must match the pool ratio\");",
        "require(amount_b.to_big_integer() >= need / big_integer(2), \"deposit must match the pool ratio\");",
        "test_bounds_and_ownership",
        "amount must be positive",
        attackLanded,
        // Defence in depth, found when the redeemability guard landed: with the
        // ratio check weakened, the unbalanced deposit is refused by the
        // redeemability guard instead ("deposit would be rounded away against the
        // pool") - the right outcome for the pool, the wrong reason for THIS mutant,
        // which exists to prove the ratio check. Both must go for the attack to land.
        alsoRemove = listOf(
            "require(back_a >= (amount_a - 1).to_big_integer() and back_b >= (amount_b - 1).to_big_integer(), \"deposit would be rounded away against the pool\");"
        )
    )

    /**
     * THE CURVE, and this one takes two edits because the guard is a check rather
     * than a shape: weaken the runtime k requirement AND round the swap output UP
     * instead of down. Either alone is refused - with the requirement intact the
     * rounded-up swap aborts, and with the rounding intact k cannot fall - so both
     * halves must move before the grinder can extract, which is precisely what
     * makes the requirement load-bearing. Round 8 shipped this invariant as a
     * passing test and nothing enforced it at runtime.
     */
    @Test
    fun ammGrindingTestGoesRedWhenTheCurveCheckIsWeakenedAndTheOutputRoundsUp() = assertGuardMutationRedensExploitTest(
        "amm",
        "require(k_of(pool.reserve_a, pool.reserve_b) >= k_before, \"the curve must not lose value\");",
        "require(k_of(pool.reserve_a, pool.reserve_b) >= k_before / big_integer(2), \"the curve must not lose value\");",
        "test_k_never_falls_under_grinding",
        "the curve must not lose value",
        "expected",
        alsoReplace = listOf(
            "return (numerator / denominator).to_integer();" to
                "return ((numerator + denominator - big_integer(1)) / denominator).to_integer();"
        )
    )

    /**
     * THE NUMBERS IN THE PROSE ARE RECOMPUTED, NOT TRUSTED.
     *
     * A suite cannot tell whether a sentence is true, and this project has now
     * been drained twice by one that was not - round 7's prescribed holding
     * period, which WAS the vulnerability, and round 8's claim that a monotone
     * paused-milliseconds counter "can never rewrite the past". The amm seam
     * shipped a third: it cited 144 bps as what a 4000 front-run does to a
     * 500000/500000 POOL. The 144 is real, but it is the VICTIM'S EXECUTION
     * loss; the reserves move 79 bps. That distinction is the whole argument,
     * because a slippage band is compared against the RESERVES - so a reader
     * who took 144 for the reserve movement would conclude a 1% band excludes
     * the sandwich, when 79 clears 1% and would clear 0.5% too. A wrong number
     * pointing at a false safe width, inside the seam whose rule is that no
     * width is safe. It passed a 1185-test green gate.
     *
     * So the arithmetic is done HERE, from the template's own constants, and
     * the text has to agree with it. Change FEE_NUMERATOR and the recomputed
     * figures move, the literals in the prose stop matching, and this test goes
     * red until the sentences are corrected - which is the point: a stale
     * number in a seam becomes unwritable rather than merely unlucky.
     */
    @Test
    fun ammSeamNumbersAreRecomputedFromTheCurveNotAsserted() {
        val main = DappScaffold.files("pool", template = "amm").getValue("src/main.rell")
        fun constant(name: String): Long =
            Regex("""val $name = (\d+)(?:\s*\*\s*(\d+))*;""").find(main)
                ?.let { m -> m.groupValues.drop(1).filter { it.isNotEmpty() }.fold(1L) { a, b -> a * b.toLong() } }
                ?: error("the amm template no longer defines $name - this test's arithmetic is stale")

        val feeNum = constant("FEE_NUMERATOR")
        val feeDen = constant("FEE_DENOMINATOR")

        // The template's own curve: exact input, fee on the way in, output floored.
        fun amountOut(amountIn: Long, reserveIn: Long, reserveOut: Long): Long {
            val withFee = amountIn * feeNum
            return withFee * reserveOut / (reserveIn * feeDen + withFee)
        }

        // Round 8's measured scenario, replayed arithmetically.
        val r = 500_000L
        val frontRun = 4_000L
        val victimIn = 100_000L

        val fair = amountOut(victimIn, r, r)
        val frontOut = amountOut(frontRun, r, r)
        val (ra, rb) = (r + frontRun) to (r - frontOut)
        val victimGot = amountOut(victimIn, ra, rb)

        val executionLossBps = (fair - victimGot) * 10_000 / fair
        val reserveMoveBps = (r - rb) * 10_000 / r

        // These are the figures round 8 measured; if the curve ever stops
        // reproducing them, the corpus sample and the prose are both stale.
        assertEquals(83_124L, fair, "the honest quote round 8 recorded")
        assertEquals(81_920L, victimGot, "what the sandwiched victim actually received")
        assertEquals(1_204L, fair - victimGot, "the victim's measured loss")
        assertEquals(144L, executionLossBps, "the victim's EXECUTION loss in bps")
        assertEquals(79L, reserveMoveBps, "what the POOL's reserves actually moved, in bps")

        // ...and now the prose has to attribute them correctly. Every mention of
        // the execution figure must sit in a sentence about the trade, and the
        // reserve figure must be present and attributed to the reserves. This is
        // the assertion the original defect would have failed.
        val seam = main.substringAfter("NEVER ADD A SLIPPAGE TOLERANCE").substringBefore("EVERY NEW WAY OUT")
        assertTrue(
            seam.contains("$reserveMoveBps bps"),
            "the seam must state what the RESERVES move ($reserveMoveBps bps), because a band is " +
                "compared against the reserves - stating only the execution loss invites a reader " +
                "to pick a band that does not exclude the sandwich"
        )
        // THE REDIRECT IS COVERED TOO, because that is where this defect survived.
        // The morning's fix corrected the template seam and left closestTemplateNote()
        // saying "a 4000 front-run moved the price 144 bps" - and the redirect is the
        // FIRST thing an agent reads, before any template exists on disk. Round 9 found
        // it still there. Any place that states these numbers has to state them right.
        for ((where, text) in mapOf(
            "closestTemplateNote(amm)" to DappScaffold.closestTemplateNote("a constant product swap pool"),
            "notes()" to DappScaffold.notes("pool")
        )) {
            val flat = text.replace(Regex("""\s+"""), " ")
            if (!flat.contains("144")) continue
            assertTrue(
                Regex("""(?i)144 bps of\s+EXECUTION|144 bps of execution""").containsMatchIn(flat) ||
                    Regex("""(?i)victim 144 bps""").containsMatchIn(flat),
                "$where cites 144 without saying it is the victim's EXECUTION loss. The reserves " +
                    "moved $reserveMoveBps bps; a band is compared against the reserves, so calling " +
                    "144 the pool's or the price's movement points a reader at a safe band width " +
                    "that does not exist. Text was: $flat"
            )
            assertTrue(
                flat.contains("$reserveMoveBps bps"),
                "$where must also state the $reserveMoveBps bps reserve movement - the figure a " +
                    "tolerance would actually have to exclude. Text was: $flat"
            )
        }

        val reserveClaim = Regex("""(?i)reserves?\s+moved?\s+(?:only\s+)?(\d+)\s*bps""")
            .find(seam.replace(Regex("""\s+"""), " "))
        assertEquals(
            reserveMoveBps.toString(),
            reserveClaim?.groupValues?.get(1),
            "the seam's 'reserves move N bps' must be the recomputed $reserveMoveBps, not the " +
                "victim's execution loss of $executionLossBps - conflating the two is the exact " +
                "defect this test exists for"
        )
    }


    /**
     * AN ORDER BOOK MUST NOT BE ANSWERED WITH A CURVE - AND, SINCE ROUND 12, MUST
     * NOT BE ANSWERED WITH TWO SENTENCES EITHER.
     *
     * `closestTemplateNote()` answers an ask it has no template for by naming the
     * nearest one, and that redirect is itself a hazard: round 8's drainable AMM
     * existed because `template=amm` silently became `template=vault`. Shipping
     * the amm template fixed that ask and quietly created a smaller version of
     * the same problem - "exchange" is in the amm word list, so an order-book ask
     * began landing on a constant-product pool. This test then pinned the honest
     * answer: no template covers it, with the hazard named.
     *
     * ROUND 12 BUILT FROM THAT ANSWER AND WAS DRAINED BY IT. The two sentences it
     * offered - the marketplace's immutable escrow row, and "an order that can be
     * pulled in the block it would have been filled in is not a commitment at
     * all" - were implemented literally, and they compose: an order with no
     * mutable field makes a partial fill delete-and-recreate, so the remainder's
     * clock starts now and any counterparty grinds the maker's cancel away. A
     * paragraph that describes a safe shape and leaves the guards to the reader
     * has now produced a drain three times, so the pin moves to the template: the
     * ask must be ROUTED, and told what the template makes unwritable.
     */
    @Test
    fun anOrderBookAskIsRoutedToItsOwnTemplate() {
        val orderBook = DappScaffold.closestTemplateNote("a limit order book with matching")
        assertTrue(
            orderBook.contains("Use `template=exchange`"),
            "an order-book ask must be routed to its own template, got: $orderBook"
        )
        assertFalse(
            orderBook.contains("NO SHIPPED TEMPLATE COVERS AN ORDER BOOK"),
            "the answer round 12 built its drain from must not still be given: $orderBook"
        )
        assertTrue(
            orderBook.contains("RESTARTED THE MAKER'S CANCEL CLOCK BY TAKING ONE UNIT"),
            "and must be told WHY the class is dangerous, not merely which template to use: $orderBook"
        )
        assertTrue(
            orderBook.contains("ONE MONOTONE COUNTER") && orderBook.contains("NO OPERATION NAMES A COUNTERPARTY"),
            "and what the template makes unwritable - a redirect that names a template without its guard is the round-8 hazard again: $orderBook"
        )

        // The case that nearly shipped: `marketplace` claims "bid", and it used to be
        // matched first, so an order book described in the words an author would
        // actually use was answered with listings and an auction. `lending` claims
        // "loan" and would have taken a margin book the same way. This branch is
        // FIRST for that reason, and these two pin the ordering rather than the
        // single phrase that happened to dodge every other keyword.
        for (ask in listOf("an order book with bid/ask spreads", "a limit order book for loans")) {
            assertTrue(
                DappScaffold.closestTemplateNote(ask).contains("Use `template=exchange`"),
                "\"$ask\" must reach the order-book answer, not the branch that shares one of its words"
            )
        }

        // The plain swap ask keeps its template - the fix must not cost that.
        val swap = DappScaffold.closestTemplateNote("a constant product swap pool")
        assertTrue(swap.contains("template=amm"), "a swap venue is covered and must still say so: $swap")

        // And the ambiguous word that caused this still says which machine it
        // answered - and now names the other one instead of leaving it uncovered.
        val exchange = DappScaffold.closestTemplateNote("an exchange")
        assertTrue(
            exchange.contains("NOT an order book"),
            "'exchange' reaches the amm answer, so that answer must say what it is not: $exchange"
        )
        assertTrue(
            exchange.contains("`template=exchange`"),
            "...and must send an order book to the template that now covers it: $exchange"
        )
    }


    /**
     * ROUND 9: A LIQUIDATION MUST NOT MANUFACTURE THE INSOLVENCY IT IS FOR. A
     * seizure of 110% of the repayment lowers the backing ratio of any position
     * under 110%, so a max-close at an unchanged price took 6060-against-6000 to
     * 2787-against-3001 and moved 107 cash from the honest lender to the
     * liquidator. The residual list said the window existed "only against an
     * already-insolvent position", which the arithmetic disproves in one line.
     * Removing the solvent-after guard lets round 9's close through: the replay
     * goes red because the transaction it says must fail "did not fail".
     */
    @Test
    fun lendingRound9ReplayGoesRedWithoutTheSolventAfterGuard() = assertGuardMutationRedensExploitTest(
        "lending",
        "require(backing_after >= debt_after, \"liquidation would leave the position insolvent - close less\");",
        "",
        "test_round9_liquidation_may_not_manufacture_insolvency_must_fail",
        "position is healthy",
        attackLanded
    )

    /**
     * UNDER WATER THERE IS NO BONUS. The debt is non-recourse, so what a
     * liquidator "repays" on an insolvent position was never going to be paid,
     * and a 10% bonus on it is collateral taken from the lenders, round after
     * round. Putting the bonus formula back on the insolvent branch pays the
     * liquidator more value than the cash they brought, and the value-for-value
     * assertion trips. (Not pro-rata of face: a thousand-year-old debt would then
     * hand a liquidator zero tokens and liquidation would simply stop.)
     */
    @Test
    fun lendingUnderwaterTestGoesRedWhenTheBonusIsPaidUnderWater() = assertGuardMutationRedensExploitTest(
        "lending",
        "p.cash.to_big_integer() * PRICE_SCALE.to_big_integer() / price.to_big_integer()",
        "p.cash.to_big_integer() * (BPS + LIQUIDATION_BONUS_BPS).to_big_integer() / BPS.to_big_integer() * PRICE_SCALE.to_big_integer() / price.to_big_integer()",
        "test_underwater_liquidation_is_value_for_value_with_no_bonus",
        "position is healthy",
        "expected"
    )

    /**
     * THE AMM'S min() WAS A SILENT HAIRCUT. The header said a later deposit "is
     * never a silent haircut" because it must match the pool ratio. On a pool
     * swapped down to 3 B, a 1000 A + 1 B deposit floors by_a to 1 share while
     * by_b is 333: the depositor gets ~501 A and 0 B back for 1000 A and a third
     * of the B reserve. A deposit must be redeemable for what it deposited at the
     * moment it is made. Removing that guard admits the deposit and the replay
     * goes red because the deposit it says must fail "did not fail".
     */
    @Test
    fun ammDustPoolDepositGoesRedWithoutTheRedeemabilityGuard() = assertGuardMutationRedensExploitTest(
        "amm",
        "require(back_a >= (amount_a - 1).to_big_integer() and back_b >= (amount_b - 1).to_big_integer(), \"deposit would be rounded away against the pool\");",
        "",
        "test_round9_dust_pool_deposit_is_refused_not_haircut",
        "deposit must match the pool ratio",
        attackLanded
    )
}
