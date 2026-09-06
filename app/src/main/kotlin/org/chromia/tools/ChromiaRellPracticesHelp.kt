package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Rell BUILD practice pages: security + best-practices.
 * Quotes docs.chromia.com/rell/security and /rell/rell-best-practices only.
 * BUILD / read-only guidance. No exploit recipes, no signing, no key material.
 * Skips proposal vote/retract. Does not invent YAML keys or 64-hex.
 */
object ChromiaRellPracticesHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val RELL_VERSION = DappScaffold.RELL_SOURCE_TAG
    const val TOOL_NAME = "chromia_rell_practices_help"
    const val SECURITY_URL = "https://docs.chromia.com/rell/security"
    const val BEST_PRACTICES_URL = "https://docs.chromia.com/rell/rell-best-practices"
    const val ANALYZE_URL = "https://docs.chromia.com/rell/analyze-rell-dapp-code"
    const val RELLDOC_URL = "https://docs.chromia.com/rell/rell-doc"
    const val ECOSYSTEM_AI_INFERENCE_INDEX_URL = "https://docs.chromia.com/ecosystem/extensions/ai_inference"
    const val ECOSYSTEM_AI_INFERENCE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/extensions/ai_inference/"
    const val ECOSYSTEM_AI_INFERENCE_INDEX_TITLE = "AI Inference"  // official H1
    const val ECOSYSTEM_ADD_NODE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/add-node"
    const val ECOSYSTEM_ADD_NODE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/add-node/"
    const val ECOSYSTEM_ADD_NODE_INDEX_TITLE = "Add a node to the network"  // official H1
    const val ECOSYSTEM_PMC_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc"
    const val ECOSYSTEM_PMC_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/"
    const val ECOSYSTEM_PMC_INDEX_TITLE = "Postchain Management Console CLI"  // official H1
    const val ECOSYSTEM_PMC_SUBNODE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/subnode"
    const val ECOSYSTEM_PMC_SUBNODE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/subnode/"
    const val ECOSYSTEM_PMC_SUBNODE_INDEX_TITLE = "subnode-image"  // official H1
    const val RELL_DATABASE_DELETE_INDEX_URL = "https://docs.chromia.com/rell/language-features/database/delete"
    const val RELL_DATABASE_DELETE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/database/delete/"
    const val RELL_DATABASE_DELETE_INDEX_TITLE = "Delete statement"  // official H1
    const val LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-entity/tables"
    const val LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-entity/tables/"
    const val LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_TITLE = "Create your first entity"  // official H1
    const val LEARN_FT4_ASSET_TESTING_INDEX_URL = "https://learn.chromia.com/courses/ft4-asset/testing"
    const val LEARN_FT4_ASSET_TESTING_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-asset/testing/"
    const val LEARN_FT4_ASSET_TESTING_INDEX_TITLE = "Testing"  // official H1
    const val LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/tools"
    const val LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/tools/"
    const val LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_TITLE = "Lesson 2 - Chromia tools"  // official H1
    const val LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-assets/buy-mystery-card"
    const val LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-assets/buy-mystery-card/"
    const val LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_TITLE = "Add a fee for buying a mystery card"  // official H1
    const val LEARN_NEWS_SCAFFOLD_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-two/scaffold"
    const val LEARN_NEWS_SCAFFOLD_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-two/scaffold/"
    const val LEARN_NEWS_SCAFFOLD_INDEX_TITLE = "Project scaffold"  // official H1
    const val LEARN_TTT_SETUP_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/setup"
    const val LEARN_TTT_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/setup/"
    const val LEARN_TTT_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts"
    const val LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts/"
    const val LEARN_NEWS_CREATE_ACCOUNTS_INDEX_TITLE = "Lesson 2 - Create accounts"  // official H1
    const val LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-explore"
    const val LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-explore/"
    const val LEARN_ZK_FRONTEND_EXPLORE_INDEX_TITLE = "Frontend architecture"  // official H1
    const val LEARN_GOAT_CODEBASE_INDEX_URL = "https://learn.chromia.com/courses/chromia-goat-chat-agent/codebase-overview"
    const val LEARN_GOAT_CODEBASE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-goat-chat-agent/codebase-overview/"
    const val LEARN_GOAT_CODEBASE_INDEX_TITLE = "Code walkthrough"  // official H1
    const val RELL_BEST_PRACTICES_INDEX_URL = "https://docs.chromia.com/rell/rell-best-practices"
    const val RELL_BEST_PRACTICES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/rell-best-practices/"
    const val RELL_BEST_PRACTICES_INDEX_TITLE = "Rell best practices"  // official H1
    const val RELL_STATEMENTS_LOOP_INDEX_URL = "https://docs.chromia.com/rell/language-features/statements/loop-statements"
    const val RELL_STATEMENTS_LOOP_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/statements/loop-statements/"
    const val RELL_STATEMENTS_LOOP_INDEX_TITLE = "Loop statements"  // official H1
    const val RELL_SYSTEMLIB_TIME_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/time"
    const val RELL_SYSTEMLIB_TIME_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/time/"
    const val RELL_SYSTEMLIB_TIME_INDEX_TITLE = "rell.time"  // official H1
    const val RELL_SECURITY_INDEX_URL = "https://docs.chromia.com/rell/security"
    const val RELL_SECURITY_INDEX_URL_SLASH = "https://docs.chromia.com/rell/security/"
    const val RELL_SECURITY_INDEX_TITLE = "Security tips for Chromia dapps"  // official H1
    const val LEARN_TAGS_ZKP_INDEX_URL = "https://learn.chromia.com/tags/ZKP"
    const val LEARN_TAGS_ZKP_INDEX_URL_SLASH = "https://learn.chromia.com/tags/ZKP/"
    const val LEARN_TAGS_ZKP_INDEX_TITLE = "Courses tagged with: ZKP"  // official H1

    val pages = listOf(SECURITY_URL, BEST_PRACTICES_URL)

    fun configDelayYaml(): String = """
        blockchains:
          my_blockchain:
            config:
              directory_chain:
                config_delay: 86400000
    """.trimIndent() + "\n"

    fun requireExample(): String = """
        operation transfer(from: account, to: account, asset, amount: big_integer) {
            require (from != to, "Sender and receiver have to be different");
            require (amount > 0, "Transfer amount must be positive");
        }
    """.trimIndent() + "\n"

    fun compositeKeyExample(): String = """
        entity balance {
          key accounts.account, asset;
          mutable amount: big_integer;
        }
        entity account {
          key id: byte_array;
          index type: text;
        }
    """.trimIndent() + "\n"

    fun inputValidationExample(): String = """
        function validate_asset_registration(
            name: text,
            symbol: text,
            decimals: integer
        ): boolean {
            require(name.size() >= 1, "Name cannot be empty");
            require(name.size() <= 1024, "Name too long");
            require(symbol.matches("^[A-Z0-9_]+$"),
                "Symbol must contain only uppercase letters, numbers, and underscores");
            require(symbol.size() <= 10, "Symbol too long");
            require(decimals >= 0, "Decimals cannot be negative");
            require(decimals <= 18, "Too many decimal places");
            return true;
        }
    """.trimIndent() + "\n"

    fun missingBalanceExample(): String = """
        function safe_get_balance(
            account_id: byte_array,
            asset_id: byte_array
        ): big_integer {
            val balance_record = balance @? {
                .account.id == account_id,
                .asset.id == asset_id
            };
            return if (balance_record != null) balance_record.amount else 0;
        }
    """.trimIndent() + "\n"

    fun runMustFailExample(): String = """
        function test_transfer_validation_must_fail() {
            val failure = rell.test.tx()
                .op(transfer(recipient, asset_id, -1))
                .run_must_fail("Amount must be positive");
            assert_true(failure.message.contains("Amount must be positive"));
        }
    """.trimIndent() + "\n"

    val securityKeys = listOf(
        "config.directory_chain.config_delay  # milliseconds; official example 86400000 = 24 hours",
        "lib.ft4.core.accounts.rate_limit.active",
        "lib.ft4.core.accounts.rate_limit.max_points",
        "lib.ft4.core.accounts.rate_limit.recovery_time",
        "lib.ft4.core.accounts.rate_limit.points_at_account_creation",
        "lib.governance.proposals.proposal_configs.option_item_limit",
        "lib.governance.proposals.proposal_configs.max_duration",
        "lib.governance.proposals.proposal_configs.min_duration",
        "lib.governance.votes.veto_config.veto_period"
    )

    val skipped = listOf(
        "proposal vote / retract (hard skip; official YAML keys only)",
        "live signing / chr tx / key generation",
        "official printed sample keys and 64-hex all-zero examples",
        "rell.test keypair sign helper  # official best-practices test uses test-scope sign; skipped here",
        "exploit recipes / attack procedures"
    )

    fun notes(): String = """
        Official Rell BUILD practice pages for CLI $CLI_SERIES. Rell language source tag $RELL_VERSION (docs may still list 0.16.4 — source wins); the chromia.yml compile.rellVersion pin is ${DappScaffold.RELL_VERSION}.
        Security: $SECURITY_URL
        Best practices: $BEST_PRACTICES_URL
        SQL analysis: $ANALYZE_URL (see chr_repl_help). RellDoc comments: $RELLDOC_URL (see chromia_rell_language_help).
        BUILD / read-only only. No exploit recipes, no signing, no key material.
        Official chromia.yml key on the security page: blockchains.<name>.config.directory_chain.config_delay
        (milliseconds; official example 86400000 = 24 hours). That key is NOT on blockchain-properties — quote the security page.
        Official governance moduleArgs keys (configs only): lib.governance.proposals.proposal_configs
        (option_item_limit, max_duration, min_duration) and lib.governance.votes.veto_config.veto_period.
        Proposal vote / retract is skipped.
        Official FT4 rate_limit keys (active, max_points, recovery_time, points_at_account_creation) already live on ft4_module_args.
        Official require example validates from != to and amount > 0. require details: chromia_rell_systemlib_help.
        Best practices: composite keys for contextual uniqueness; index fields used in filters / @* / joins; do not over-index (write cost).
        Missing rows: use @? then if (record != null) value else 0. Validate text sizes and symbol.matches before writes.
        Official account/asset id examples are 32 bytes and not all-zero — do not invent a 64-hex.
        Negative tests: rell.test.tx().op(...).run_must_fail("message") then assert_true(failure.message.contains(...)).
        Beyond input validation, prove ECONOMIC invariants with tests: conservation (a transfer
        never changes the total in circulation), no-negative-balance (overdraft must abort), and
        authorization (a NON-owner's attempt must run_must_fail - authenticating says who calls,
        require(row.owner == account.id) says they may touch that row). scaffold_dapp template=ft4
        ships runnable examples of all three; run them via run_rell_tests.
        Governance (DAO, treasury, voting): start from scaffold_dapp template=governance - quorum,
        a fixed voting window, stake-weighted votes and execute-once are structural there, not a
        require() to remember. Two more since adversary round 11, and they are the ones a DAO gets
        wrong: VOTING WEIGHT IS NOT MINTABLE (registration credits nothing - a permissionless
        1000-point welcome grant let four registrations outvote three honest members and take a
        7000 treasury) and THE BAR A PROPOSAL IS JUDGED AGAINST IS FIXED WHEN IT IS CREATED,
        weights included - a bar read live at execution is a veto anybody can buy, and two points
        of stake posted after voting closed killed an approved payout for ever. Oracle-priced value (vault, redemption, a bounded price FEED - NOT an "exchange", which is how
        adversary round 8 came to build a drainable AMM here; a vault covers a reserve and a feed,
        never a CURVE, so a swap pool is template=amm and an order book is template=exchange):
        start from
        template=vault - every credit is paid out of a reserve row in the same operation and price
        posts are bounded, rate-limited and time-checked. Staking, yield, rewards, emissions - a pool many stakers split, and NOT vesting (a clock-metered
        payout to ONE named beneficiary is template=streaming, a different exploit class that rounds 7
        and 8 both drained) - anything
        paid out over time: start from template=staking - rewards are released from a sponsor-funded
        pool (never computed from a rate), capped by what the pool holds, and unstaking has a
        cooldown. NFT marketplace, listings, auctions, anything with a buy button and creator
        royalties: start from template=marketplace - a buy names the EXACT price it agreed to on an
        IMMUTABLE listing row (a caller-supplied max_price CEILING is a sandwich: the seller reprices
        to the ceiling and pockets the buffer), offers escrow the bidder's points and settle
        atomically, and the header states honestly which royalty bypass no template can close.
        READ THAT LESSON NARROWLY - IT IS ABOUT A FIXED PRICE ON A ROW SOMEONE ELSE CAN EDIT, NOT
        ABOUT SLIPPAGE LIMITS IN GENERAL. The bug is that the counterparty can move the price
        INSIDE the tolerance and keep the difference, so the tolerance is a gift to them. A
        min_out FLOOR on an AMM swap is the OPPOSITE thing and must not be removed: the price
        there comes from a pool the caller does not control, min_out cannot pay anybody anything,
        and the only thing it can do is ABORT THE CALLER'S OWN TRADE when the pool moved against
        them. Deleting it does not close a sandwich - it removes the only protection an AMM user
        has, and turns a bounded loss into an unbounded one. The distinguishing question is not
        "is there a caller-supplied bound" but: CAN THE COUNTERPARTY CHOOSE A PRICE ANYWHERE
        INSIDE THAT BOUND AND POCKET THE DIFFERENCE? On a listing, yes. On a constant-product
        swap, no. THE AMM ANSWER IS A TEMPLATE, NOT A RULE, because a sandwich is not
        statically distinguishable from two honest trades by two strangers (corpus rows
        r8-amm-* stay GAP by decision): start from template=amm. It keeps no tolerance
        field at all - a swap NAMES THE EXACT RESERVES it was quoted at and pays that
        number or reverts, which is stronger than a floor, not a weakening of one - and
        it makes liquidity an IMMUTABLE POSITION ROW WITH A TERM, so the just-in-time
        deposit-before-a-swap-and-withdraw-after that round 8 also landed there cannot be
        written. Its header states what neither guard stops - price impact you re-quote
        into, and a cheap grief where anyone touching the reserves makes pending swaps
        revert - and ships both as tests rather than as claims.
        Order book, limit orders, bids and asks, matching, a venue where resting orders are
        FILLED rather than priced off a curve: start from template=exchange, NOT template=amm.
        Adversary round 12 drained one built freehand on this server's advice, and both of the
        sentences it followed were ours: the marketplace's immutable escrow row, so the order
        had no mutable field and a partial fill was delete-and-recreate; and "an order that can
        be pulled in the block it would have been filled in is not a commitment at all", so
        cancel required MIN_RESTING_MS. The remainder was a NEW row whose created_at was NOW,
        so ANY counterparty restarted the maker's cancel clock by taking one unit - one unit
        every 59 minutes froze a maker's whole position at 10 beside a bid of 20 and cost her
        1000 points, half her inventory's value, with gate ok:true and both conservation
        invariants exact. The template's answer is that A PARTIAL FILL WRITES ONE MONOTONE
        COUNTER AND NOTHING ELSE - the terms and the clock are immutable and the row is never
        re-created - and that NO OPERATION NAMES A COUNTERPARTY, so the book matches by price,
        then time, then id, and a crossing order is filled at the resting price in the block it
        is signed. Round 14 then corrected the OTHER half of that old sentence: refusing the
        CANCEL for MIN_RESTING_MS is a commitment only a maker with ONE ACCOUNT can be held to,
        because self-dealing is refused per ACCOUNT and registration is free - a maker with two
        keys removes a resting order in the block she places it by crossing it from the other
        one, which cost the one-account maker 500 points on the same stale quote and cost her
        nothing. So the template binds what a second key cannot reach: the QUOTE may be pulled in
        any block and the ESCROW comes back only MIN_RESTING_MS after the order was placed. A
        resting quote is NOT firm, and the header says so. The book is bounded the same way -
        by a MIN_NOTIONAL a row's author must pay for, not by a per-trader cap that a free
        registration resets.
        Stablecoin, CDP, synthetic, pegged asset - a coin MINTED AGAINST LOCKED COLLATERAL that
        a price can put under water: start from template=stablecoin, NOT template=vault. Round 9
        built one on the vault's advice, followed its reserve discipline to the letter, and was
        drained by REDEEMING THE COIN FOR COLLATERAL AT PAR out of a reserve that no longer
        covered it: 13332 of coin against collateral worth 10240 after three honest -20% posts,
        and whoever redeemed first took 100 cents on the dollar while the last holder kept 3082 of
        a coin nothing backed - thirty tokens moved on order alone, gate ok:true, zero findings.
        A CDP's coin is a LIABILITY of a position, not a claim on a pool, and the template has NO
        operation that pays a coin holder par out of somebody else's position: the peg is the
        debtor's burn at par against their OWN debt, an under-water position closes by
        LIQUIDATION capped at the position's PRO-RATA share so every liquidator is paid the same
        rate in any order AND ONLY WHILE THE SYSTEM ITSELF IS WORTH AT LEAST ITS COIN, and a
        system worth less than its coin at a fresh price is SETTLED - surplus back to each owner,
        the rest one pool every coin redeems the same share of. That system condition is round
        11's fix: the per-position cap held and the drain was one level up, because seized
        collateral leaves the COMMON settlement reserve faster than the coin it retires whenever
        the target is better backed than the system average - at 98% backing, liquidate-then-
        settle paid the liquidator 104 tokens against the 89 settle-first pays, and 7 of the 15
        tokens that moved came from a holder who was party to no liquidation at all. Mint
        and withdraw are ratio-checked against the WHOLE debt at a FRESH bounded price. Its
        header admits the residual: a price that falls faster than liquidators act still leaves
        bad debt, the coin is then worth the settlement rate, not par, and between the block the
        system goes under-backed and the block somebody calls settle() no bad position is closed
        by anyone.
        The AUCTION is in the MARKETPLACE template too - a timed ascending auction with NO mutable bid
        field (the standing bid is its own immutable escrow row, raising is delete-and-recreate, settlement
        is permissionless after the deadline), plus require_unencumbered, the one helper every
        token-moving path consults so a gift cannot walk a token out from under an escrowed bid.
        Do not write an auction freehand: a mutable highest_bid is the sandwich in auction clothes.
        Lending pool, credit line, money market - anything where depositors hold a SHARE of a pool
        whose value moves: start from template=lending. Round 6 drained a competent hand-built one
        for 1500 with nothing minted, because interest accrued only on the paths a borrower signs,
        so the price of a lender share was stale between touches. The template's answer is not
        "remember to accrue on every path" - NO CASH-DENOMINATED DEBT IS STORED ANYWHERE: positions
        and the pool both carry scaled_debt in index units, the cash figures exist only inside a
        pool_state, pool_now() is the only function that makes one, and debt_of / shares_for /
        cash_for / payment_for all TAKE one, so a new operation cannot price an entry or an exit
        without a fresh state. It keeps the vault's bounded oracle, over-collateralisation, a
        liquidation threshold with a close factor and bonus, and the minimum-first-deposit guard
        against ERC-4626 share inflation. Its EXTENDING section names the seam no rule can see: ANY
        CHANGE that makes pool value depend on something a caller can move in the same block - a
        fee, a bad-debt write-off, a donation, or a UTILISATION RATE CURVE - re-opens the
        just-in-time window, and it does not have to be a new operation. Adversary round 8 put the
        step inside the shared PRICING FUNCTION, added no operation at all, satisfied every seam as
        written, and a lender's own withdrawal then made a healthy borrower liquidatable at an
        unchanged oracle price. The fix for a step is to net it into the priced state so it accrues
        CONTINUOUSLY, never an entry/exit fee or a minimum holding period: a toll on a round trip
        does not stop an exit. For a moving RATE the sanctioned shape - and the only one - is the
        CHECKPOINTED INDEX the template now ships: a stored rate-weighted accumulator plus the
        block it was accrued to, advanced by the one function that produces an index, so a new rate
        applies only to time AFTER it changed. That is a stored snapshot, and it is safe for the
        one reason the round-6 stale field was not: it is written on every path that reads it.
        ANY CLOCK-METERED PAYOUT - a stream, a vesting schedule, a drip, an interest accrual -
        must make the entitlement a PURE FUNCTION of an IMMUTABLE start plus a MONOTONE released
        total. A mutable anchor any caller may advance - "pay what has accrued since the last
        settlement, then move the marker to now" - hands the beneficiary's income to whoever calls
        the operation: when the release is integer division, a settle spaced closer than one whole
        unit of entitlement pays ZERO and still advances the marker, so a stranger with nothing at
        stake settles often enough to grind the payout to nothing. The interval is destroyed, not
        deferred. Permissionless settlement is fine and often necessary; a permissionless MARKER
        MOVE is not. Adversary rounds 6 and 7 landed this same bug in two different value classes,
        a lending pool's interest index and a payment stream, so it is not a lending concern.
        Payment stream, payroll, subscription, vesting grant, drip - a clock-metered payout to ONE
        NAMED beneficiary: start from template=streaming, which is that rule made structural. NO
        OPERATION IN IT WRITES A TIMESTAMP: started_at is written once by the create and is not
        mutable, so there is no marker to move; every other term (payer, payee, rate, funded
        amount, cancellable) is immutable too, because a mutable payee is the same drain without
        even needing the timing. The stream is PREPAID - it can never promise more than it holds -
        and cancellation is terminal and PAYS BEFORE IT REFUNDS: the payee keeps everything
        accrued, the payer reclaims only the unearned remainder, and both halves are continuous in
        the cancelling block so neither side gains by choosing the moment. PAUSE/RESUME IS SHIPPED
        IN THE TEMPLATE, and this text used to describe it instead - which drained two builds in
        adversary round 8, so read the correction rather than the shape. A monotone
        total-paused-milliseconds counter is NOT a safety property: a monotone SUBTRAHEND is a
        monotone CLAWBACK, and raising it lowers the entitlement for every past instant too. What
        is load-bearing is that ACTIVE ELAPSED TIME NEVER GOES BACKWARDS, which needs
        require(not s.paused) on pause and require(s.paused) on resume - one missing require()
        handed a payer 100% of a payroll escrow. And a pause built as cancel-and-reopen must take
        the SAME cancellable term the cancellation does, or it ends a vesting grant that the
        header promised could not be clawed back. All six ship the real drain as a must-fail test.
        Official best-practices test also calls .sign on a rell.test keypair — skipped here (no signing / no key material).
        Pagination list queries: chromia_cookbook_help. Formatting: spaces around operators, indented blocks, multi-line params.
        Official analyze-page example chain name house-key-example has a hyphen; CLI 0.20.14+ forbids hyphens — do not ship it.
        Official ECOSYSTEM ecosystem/extensions/ai_inference INDEX ($ECOSYSTEM_AI_INFERENCE_INDEX_URL 307 $ECOSYSTEM_AI_INFERENCE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_AI_INFERENCE_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/nodes/add-node INDEX ($ECOSYSTEM_ADD_NODE_INDEX_URL 307 $ECOSYSTEM_ADD_NODE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_ADD_NODE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc INDEX ($ECOSYSTEM_PMC_INDEX_URL 307 $ECOSYSTEM_PMC_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/subnode INDEX ($ECOSYSTEM_PMC_SUBNODE_INDEX_URL 307 $ECOSYSTEM_PMC_SUBNODE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_SUBNODE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/database/delete INDEX ($RELL_DATABASE_DELETE_INDEX_URL 307 $RELL_DATABASE_DELETE_INDEX_URL_SLASH 200 H1 $RELL_DATABASE_DELETE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/book-review/book-entity/tables INDEX ($LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL 301 $LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/ft4-asset/testing INDEX ($LEARN_FT4_ASSET_TESTING_INDEX_URL 301 $LEARN_FT4_ASSET_TESTING_INDEX_URL_SLASH 200 H1 $LEARN_FT4_ASSET_TESTING_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/ft4-demo-app/module-frontend-application/tools INDEX ($LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL 301 $LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/marketplace-course/module-assets/buy-mystery-card INDEX ($LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL 301 $LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL_SLASH 200 H1 $LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-two/scaffold INDEX ($LEARN_NEWS_SCAFFOLD_INDEX_URL 301 $LEARN_NEWS_SCAFFOLD_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_SCAFFOLD_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/tic-tac-toe/setup INDEX ($LEARN_TTT_SETUP_INDEX_URL 301 $LEARN_TTT_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_TTT_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX news-feed create-accounts ($LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL 301 $LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_CREATE_ACCOUNTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/rell-best-practices INDEX ($RELL_BEST_PRACTICES_INDEX_URL 307 $RELL_BEST_PRACTICES_INDEX_URL_SLASH 200 H1 $RELL_BEST_PRACTICES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof frontend-explore ($LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL 301 $LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL_SLASH 200 H1 $LEARN_ZK_FRONTEND_EXPLORE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX chromia-goat-chat-agent codebase-overview ($LEARN_GOAT_CODEBASE_INDEX_URL 301 $LEARN_GOAT_CODEBASE_INDEX_URL_SLASH 200 H1 $LEARN_GOAT_CODEBASE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/statements/loop-statements INDEX ($RELL_STATEMENTS_LOOP_INDEX_URL 307 $RELL_STATEMENTS_LOOP_INDEX_URL_SLASH 200 H1 $RELL_STATEMENTS_LOOP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/namespaces/time INDEX ($RELL_SYSTEMLIB_TIME_INDEX_URL 307 $RELL_SYSTEMLIB_TIME_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_TIME_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/security INDEX ($RELL_SECURITY_INDEX_URL 307 $RELL_SECURITY_INDEX_URL_SLASH 200 H1 $RELL_SECURITY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official LEARN tags/ZKP INDEX ($LEARN_TAGS_ZKP_INDEX_URL 301 $LEARN_TAGS_ZKP_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_ZKP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("rell", RELL_VERSION)
        put("rellSourceTag", RELL_VERSION)
        put("rellVersionPin", DappScaffold.RELL_VERSION)
        put("tool", TOOL_NAME)
        put("security_docs", SECURITY_URL)
        put("best_practices_docs", BEST_PRACTICES_URL)
        put("analyze_docs", ANALYZE_URL)
        put("relldoc_docs", RELLDOC_URL)
        put("read_only", "true")
        put("pages", buildJsonArray { pages.forEach { add(JsonPrimitive(it)) } })
        put("config_delay_yaml", configDelayYaml())
        put("config_delay_ms", 86400000)
        put("require_example", requireExample())
        put("composite_key_example", compositeKeyExample())
        put("input_validation_example", inputValidationExample())
        put("missing_balance_example", missingBalanceExample())
        put("run_must_fail_example", runMustFailExample())
        put(
            "invariant_tests",
            "Conservation, no-negative-balance, and non-owner-must-fail tests: scaffold_dapp " +
                "template=ft4 ships runnable examples (src/test/main_test.rell) - run via run_rell_tests. " +
                "DAO / treasury: template=governance (its founder key is a module arg, main.founder_pubkey - registration mints no weight); oracle-priced reserve or vault, NOT an exchange: template=vault; swap pool / DEX pair / AMM: template=amm; " +
                "staking / rewards / emissions: template=staking; payroll / vesting / drip (PREPAID): template=streaming; subscription / recurring pull billing / allowance / membership: template=subscription, never streaming; NFT marketplace / listings / " +
                "royalties: template=marketplace; lending pool / credit line / money market: " +
                "template=lending; order book / limit orders / bids and asks / matching: template=exchange, never amm; " +
                "stablecoin / CDP / synthetic / pegged asset: template=stablecoin, never vault - " +
                "their guards are structural and their shipped tests replay the real drain as must-fail. " +
                "Then PROVE your own guards the same way the templates are proven: verify_guards removes a guard " +
                "you name, reruns only its must-fail test, and reports load_bearing only if that test failed " +
                "because the attack landed - a test that stays green without its guard is a fake green, and " +
                "one was written by this server's own maintainers before a mutant caught it."
        )
        put("security_keys", buildJsonArray { securityKeys.forEach { add(JsonPrimitive(it)) } })
        put("skipped", buildJsonArray { skipped.forEach { add(JsonPrimitive(it)) } })
        put("rate_limit_help", "ft4_module_args")
        put("require_help", ChromiaRellSystemlibHelp.TOOL_NAME)
        put("language_help", ChromiaRellLanguageHelp.TOOL_NAME)
        put("repl_help", ChrReplHelp.TOOL_NAME)
        put("cookbook_help", "chromia_cookbook_help")
        put("ecosystem_ai_inference_index_url_slash", ECOSYSTEM_AI_INFERENCE_INDEX_URL_SLASH)
        put("ecosystem_ai_inference_index_title", ECOSYSTEM_AI_INFERENCE_INDEX_TITLE)
        put("ecosystem_add_node_index_url_slash", ECOSYSTEM_ADD_NODE_INDEX_URL_SLASH)
        put("ecosystem_add_node_index_title", ECOSYSTEM_ADD_NODE_INDEX_TITLE)
        put("ecosystem_pmc_index_url_slash", ECOSYSTEM_PMC_INDEX_URL_SLASH)
        put("ecosystem_pmc_index_title", ECOSYSTEM_PMC_INDEX_TITLE)
        put("ecosystem_pmc_subnode_index_url_slash", ECOSYSTEM_PMC_SUBNODE_INDEX_URL_SLASH)
        put("ecosystem_pmc_subnode_index_title", ECOSYSTEM_PMC_SUBNODE_INDEX_TITLE)
        put("rell_database_delete_index_url_slash", RELL_DATABASE_DELETE_INDEX_URL_SLASH)
        put("rell_database_delete_index_title", RELL_DATABASE_DELETE_INDEX_TITLE)
        put("learn_book_review_entity_tables_index_url_slash", LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL_SLASH)
        put("learn_book_review_entity_tables_index_title", LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_TITLE)
        put("learn_ft4_asset_testing_index_url_slash", LEARN_FT4_ASSET_TESTING_INDEX_URL_SLASH)
        put("learn_ft4_asset_testing_index_title", LEARN_FT4_ASSET_TESTING_INDEX_TITLE)
        put("learn_ft4_demo_frontend_tools_index_url_slash", LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL_SLASH)
        put("learn_ft4_demo_frontend_tools_index_title", LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_TITLE)
        put("learn_marketplace_buy_mystery_index_url_slash", LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL_SLASH)
        put("learn_marketplace_buy_mystery_index_title", LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_TITLE)
        put("learn_news_scaffold_index_url_slash", LEARN_NEWS_SCAFFOLD_INDEX_URL_SLASH)
        put("learn_news_scaffold_index_title", LEARN_NEWS_SCAFFOLD_INDEX_TITLE)
        put("learn_ttt_setup_index_url_slash", LEARN_TTT_SETUP_INDEX_URL_SLASH)
        put("learn_ttt_setup_index_title", LEARN_TTT_SETUP_INDEX_TITLE)
        put("learn_news_create_accounts_index_url_slash", LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL_SLASH)
        put("learn_news_create_accounts_index_title", LEARN_NEWS_CREATE_ACCOUNTS_INDEX_TITLE)
        put("rell_best_practices_index_url_slash", RELL_BEST_PRACTICES_INDEX_URL_SLASH)
        put("rell_best_practices_index_title", RELL_BEST_PRACTICES_INDEX_TITLE)
        put("learn_zk_frontend_explore_index_url_slash", LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL_SLASH)
        put("learn_zk_frontend_explore_index_title", LEARN_ZK_FRONTEND_EXPLORE_INDEX_TITLE)
        put("learn_goat_codebase_index_url_slash", LEARN_GOAT_CODEBASE_INDEX_URL_SLASH)
        put("learn_goat_codebase_index_title", LEARN_GOAT_CODEBASE_INDEX_TITLE)
        put("rell_statements_loop_index_url_slash", RELL_STATEMENTS_LOOP_INDEX_URL_SLASH)
        put("rell_statements_loop_index_title", RELL_STATEMENTS_LOOP_INDEX_TITLE)
        put("rell_systemlib_time_index_url_slash", RELL_SYSTEMLIB_TIME_INDEX_URL_SLASH)
        put("rell_systemlib_time_index_title", RELL_SYSTEMLIB_TIME_INDEX_TITLE)
        put("rell_security_index_url_slash", RELL_SECURITY_INDEX_URL_SLASH)
        put("rell_security_index_title", RELL_SECURITY_INDEX_TITLE)
        put("learn_tags_zkp_index_url_slash", LEARN_TAGS_ZKP_INDEX_URL_SLASH)
        put("learn_tags_zkp_index_title", LEARN_TAGS_ZKP_INDEX_TITLE)
        put("notes", notes())
    }
}
// Official ECOSYSTEM ecosystem/extensions/ai_inference INDEX encoded as ECOSYSTEM_AI_INFERENCE_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/nodes/add-node INDEX encoded as ECOSYSTEM_ADD_NODE_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc INDEX encoded as ECOSYSTEM_PMC_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/subnode INDEX encoded as ECOSYSTEM_PMC_SUBNODE_INDEX_* (query-only HELP ONLY).
// Official RELL rell/language-features/database/delete INDEX encoded as RELL_DATABASE_DELETE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/book-entity/tables INDEX encoded as LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-asset/testing INDEX encoded as LEARN_FT4_ASSET_TESTING_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-frontend-application/tools INDEX encoded as LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/marketplace-course/module-assets/buy-mystery-card INDEX encoded as LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-two/scaffold INDEX encoded as LEARN_NEWS_SCAFFOLD_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/setup INDEX encoded as LEARN_TTT_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX news-feed create-accounts encoded as LEARN_NEWS_CREATE_ACCOUNTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/rell-best-practices INDEX encoded as RELL_BEST_PRACTICES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof frontend-explore encoded as LEARN_ZK_FRONTEND_EXPLORE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX chromia-goat-chat-agent codebase-overview encoded as LEARN_GOAT_CODEBASE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/statements/loop-statements INDEX encoded as RELL_STATEMENTS_LOOP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/namespaces/time INDEX encoded as RELL_SYSTEMLIB_TIME_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/security INDEX encoded as RELL_SECURITY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/ZKP INDEX encoded as LEARN_TAGS_ZKP_INDEX_* (query-only HELP ONLY WRITE SKIP).
