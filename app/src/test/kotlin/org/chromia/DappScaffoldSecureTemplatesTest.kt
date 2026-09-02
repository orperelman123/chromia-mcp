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
 * Pins the two templates that exist because the gate cannot block their
 * exploit class (north-star principle 4): `governance` (the round-1 DAO drain)
 * and `vault` (the round-1 unbacked oracle mint). Each must compile with the
 * vendored FT4, pass the security check with NO finding at all, ship tests
 * that actually run green through run_rell_tests, and - the proof that the
 * bug is unwritable - ship a must-fail replay of the exploit that goes RED
 * the moment its guard is deleted (the mutation tests at the bottom).
 */
class DappScaffoldSecureTemplatesTest {

    private fun rellOf(template: String): Map<String, String> =
        DappScaffold.files("treasury", template = template)
            .filterKeys { it.endsWith(".rell") }
            .mapKeys { (path, _) -> path.removePrefix("src/") }

    private fun moduleArgsOf(template: String) =
        if (template == "vault") DappScaffold.vaultTestModuleArgs() else DappScaffold.ft4TestModuleArgs()

    private val dbUrl: String? = System.getenv(RunRellTests.DATABASE_URL_ENV)

    @Test
    fun bothTemplatesShipYmlMainAndRunnableTests() {
        listOf("governance", "vault").forEach { template ->
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
            assertTrue(test.contains("test_round1_"), "$template must ship the round-1 exploit as a must-fail test")
            assertTrue(test.contains("assert_conserved()"), "$template must ship a conservation assertion")
            DappScaffold.forbiddenModules.forEach { banned ->
                files.forEach { (path, content) ->
                    if (path == "chromia.yml" && banned == "lib.ft4.core.admin") return@forEach
                    assertFalse(content.contains(banned), "$template/$path must not reference $banned")
                }
            }
        }
        assertEquals(listOf("hello", "ft4", "governance", "vault"), DappScaffold.templates)
        assertEquals("governance", DappScaffold.toJson("dao", template = "governance").getValue("template").toString().trim('"'))
        assertEquals("vault", DappScaffold.toJson("dex", template = "vault").getValue("template").toString().trim('"'))
        val unknown = DappScaffold.toJson("x", template = "dao")
        assertEquals("hello", unknown.getValue("template").toString().trim('"'))
        assertTrue(unknown.getValue("warnings").toString().contains("governance, vault"), "unknown-template warning must list the new templates")
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
        val execute = main.substringAfter("operation execute_proposal").substringBefore("\n}")
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
            val body = main.substringAfter("operation $op").substringBefore("\n}")
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

    @Test
    fun templatesCompileWithVendoredLib() {
        listOf("governance", "vault").forEach { template ->
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
        listOf("governance", "vault").forEach { template ->
            val files = DappScaffold.files("t", template = template)
            val result = RellSecurityCheck.analyze(
                mapOf(
                    "main.rell" to files.getValue("src/main.rell"),
                    "test/main_test.rell" to files.getValue("src/test/main_test.rell")
                )
            )
            assertTrue(result.ok, "$template: ${result.findings}")
            assertEquals(emptyList<RellSecurityCheck.Finding>(), result.findings, "$template must produce no advisory either")
        }
    }

    /**
     * The ft4 template's key discipline, applied to both new ymls: every 64+ hex
     * run is FT4's published test keypair or a library RID, the admin args sit
     * under test:, and - vault only - the production moduleArgs block carries NO
     * oracle key (only the comment telling the deployer to add theirs), while
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
        listOf("governance", "vault").forEach { template ->
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
                assertFalse(yml.contains("oracle_pubkey"), "governance yml carries no oracle key")
            }
            val ymlCheck = org.chromia.tools.ChromiaYml.validate(yml)
            assertTrue(ymlCheck.errors.isEmpty(), "$template yml must validate: ${ymlCheck.errors}")
        }
    }

    private fun runShipped(template: String, files: Map<String, String> = rellOf(template)): RunRellTests.Result {
        val result = RunRellTests.run(files, databaseUrl = dbUrl, moduleArgs = moduleArgsOf(template))
        // Printed so the gradle XML carries the per-case verdicts the report pastes.
        println("[$template] ok=${result.ok} total=${result.total} passed=${result.passed} failed=${result.failed}")
        result.cases.forEach { println("[$template]   ${it.name}: ${if (it.ok) "PASS" else "FAIL"}${it.error?.let { e -> " - $e" } ?: ""}") }
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

    /**
     * The proof the exploit is unwritable: delete ONE guard from the template
     * and the shipped round-1 replay must go red - for the exploit's reason,
     * i.e. the attack now SUCCEEDS where the test required it to be refused.
     * A must-fail test that stayed green without its guard would be theatre.
     */
    private fun assertGuardRemovalRedensExploitTest(
        template: String,
        guard: String,
        exploitTest: String,
        expectedFailureFragment: String
    ) {
        if (dbUrl == null) return // these run real transactions; the DB branch is authoritative and CI provides one
        val files = rellOf(template).toMutableMap()
        val main = files.getValue("main.rell")
        assertTrue(main.contains(guard), "$template guard must exist verbatim: $guard")
        files["main.rell"] = main.replace(guard, "")
        val mutant = runShipped("$template-without[${guard.take(48)}]", files)
        val case = mutant.cases.single { it.name.endsWith(exploitTest) }
        assertFalse(case.ok, "$template: $exploitTest must FAIL once '$guard' is removed - it stayed green, so it proves nothing")
        // Right reason: the attack step now SUCCEEDS (run_must_fail reports an
        // unexpected success). Wrong reason would be the attack still refused by
        // some other guard, or a message mismatch - both quote the guard's text.
        assertFalse(
            case.error?.contains(expectedFailureFragment) == true,
            "$template: $exploitTest failed for the wrong reason - the attack was still refused: ${case.error}"
        )
    }

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
        "price move exceeds bound"
    )
}
