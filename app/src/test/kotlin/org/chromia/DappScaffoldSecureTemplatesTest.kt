package org.chromia

import org.chromia.tools.DappScaffold
import org.chromia.tools.Ft4ModuleArgs
import org.chromia.tools.RellCheck
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pins the templates that exist because the gate cannot block their exploit
 * class (north-star principle 4): `governance` (the round-1 DAO drain), `vault`
 * (the round-1 unbacked oracle mint) and `staking` (the round-4 unbacked reward
 * mint). Each must compile with the vendored FT4, pass the security check with
 * NO finding at all, ship tests that actually run green through run_rell_tests,
 * and - the proof that the bug is unwritable - ship a must-fail replay of the
 * exploit that goes RED the moment its guard is deleted (the mutation tests at
 * the bottom).
 */
class DappScaffoldSecureTemplatesTest {

    private val secureTemplates = listOf("governance", "vault", "staking", "marketplace")

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
    private fun moduleArgsOf(template: String) =
        if (template == "vault") DappScaffold.vaultTestModuleArgs() else DappScaffold.ft4TestModuleArgs()

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
        assertEquals(listOf("hello", "ft4", "governance", "vault", "staking", "marketplace"), DappScaffold.templates)
        assertEquals("governance", DappScaffold.toJson("dao", template = "governance").getValue("template").toString().trim('"'))
        assertEquals("vault", DappScaffold.toJson("dex", template = "vault").getValue("template").toString().trim('"'))
        assertEquals("staking", DappScaffold.toJson("yield", template = "staking").getValue("template").toString().trim('"'))
        assertEquals("marketplace", DappScaffold.toJson("bazaar", template = "marketplace").getValue("template").toString().trim('"'))
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
        assertEquals(2, Regex("\\bsettle_sale\\(").findAll(code).count() - 1, "settle_sale must have exactly two callers")
        assertTrue(buy.contains("update buyer ( .balance -= price );"), "buy_nft must debit the buyer")
        assertTrue(buy.indexOf("delete l;") in 0 until buy.indexOf("settle_sale("), "buy_nft must consume the listing before it pays out")
        assertTrue(accept.indexOf("delete o;") in 0 until accept.indexOf("settle_sale("), "accept_offer must consume the escrow before it pays out")

        // Every operation that credits a balance debits the same amount in the same
        // body; the two sale paths do it through settle_sale, asserted above.
        Regex("operation (\\w+)\\(").findAll(code).map { it.groupValues[1] }.forEach { op ->
            val body = opBody(code, op)
            Regex("\\.balance \\+= (\\w+)").findAll(body).map { it.groupValues[1] }.forEach { amount ->
                val debited = body.contains("-= $amount") ||
                    (op == "cancel_offer" && body.contains("val $amount = o.amount;") && body.contains("delete o;"))
                assertTrue(debited, "$op credits .balance += $amount without debiting $amount in the same operation")
            }
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
            if (template == "vault") {
                // Production: the oracle key line exists only as a comment.
                val uncommentedOracle = production.lineSequence().filter { !it.trimStart().startsWith("#") }
                    .any { it.contains("oracle_pubkey") }
                assertFalse(uncommentedOracle, "vault production yml must not set a placeholder oracle key")
                assertTrue(production.contains("#   oracle_pubkey: x\"<your oracle public key>\""), "vault yml must tell the deployer where the oracle key goes")
                // Test: FT4's published test pubkey, under main, under test:.
                assertTrue(testBlock.contains("    main:\n      oracle_pubkey: x\"${DappScaffold.TEST_ADMIN_PUBKEY}\""), "vault test.moduleArgs must wire the oracle test key")
                assertEquals(
                    DappScaffold.TEST_ADMIN_PUBKEY,
                    DappScaffold.vaultTestModuleArgs().getValue("main").getValue("oracle_pubkey").toString().trim('"'),
                    "vaultTestModuleArgs must mirror the yml"
                )
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
            "test_escrow_and_ownership_hold"
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
        alsoRemove: List<String> = emptyList()
    ) {
        if (dbUrl == null) return // these run real transactions; the DB branch is authoritative and CI provides one
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
        if (dbUrl == null) return
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
}
