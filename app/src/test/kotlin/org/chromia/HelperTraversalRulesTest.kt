package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Adversary round 3 found that three per-operation rules analyzed only the
 * raw operation body: moving the keyed mutation (confused deputy), the phantom
 * `is_signer(param)` gate, or spelling a full-table wipe with a tautological
 * where-clause made the gate return ok:true while the exploit still ran.
 *
 * These tests pin the fix in BOTH directions. Every exploit shape - one and
 * two helpers deep, the parameter renamed at the call, namespaced and named
 * arguments, cross-file - must be flagged; and every legitimate delegation
 * shape (an ownership require() in a helper, a self-authenticating helper,
 * a helper keyed off the authenticated identity, a helper local that merely
 * shares a parameter's name) must stay clean. A gate that flags delegation
 * is routed around, which is worse than no gate.
 */
class HelperTraversalRulesTest {

    private fun rules(result: RellSecurityCheck.Result): List<String> = result.findings.map { it.rule }

    private fun analyze(vararg files: Pair<String, String>): RellSecurityCheck.Result =
        RellSecurityCheck.analyze(files.associate { (p, c) -> p to c.trimIndent() })

    private fun assertDeputy(result: RellSecurityCheck.Result, param: String) {
        val hit = result.findings.filter { it.rule == "authorization-not-bound-to-caller" }
        assertTrue(hit.isNotEmpty(), "confused deputy through a helper must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertTrue(hit.any { "'$param'" in it.text }, "finding must name '$param'; got $hit")
        assertEquals(false, result.ok)
    }

    private fun assertNoDeputy(result: RellSecurityCheck.Result) {
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            "legitimate delegation must stay clean; got ${result.findings}"
        )
    }

    // ---- authorization-not-bound-to-caller through helpers ----

    /** The pinned exploit: the debit keyed by caller-supplied `from` sits one call deep. */
    @Test
    fun drainOneHelperDeepIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function do_transfer(from: byte_array, to: byte_array, amount: integer) {
                    update wallet @ { .owner == from } ( .balance -= amount );
                    update wallet @ { .owner == to } ( .balance += amount );
                }
                operation transfer(from: byte_array, to: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    require(amount > 0, "positive");
                    do_transfer(from, to, amount);
                }
            """
        )
        assertDeputy(result, "from")
    }

    /** Two-level chain: op -> move() -> debit(), the parameter renamed at every hop. */
    @Test
    fun drainTwoHelpersDeepWithRenamedFormalsIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function debit(victim: byte_array, qty: integer) {
                    update wallet @ { .owner == victim } ( .balance -= qty );
                }
                function move(src: byte_array, dst: byte_array, qty: integer) {
                    debit(src, qty);
                    update wallet @ { .owner == dst } ( .balance += qty );
                }
                operation transfer(from: byte_array, to: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    require(amount > 0, "positive");
                    move(from, to, amount);
                }
            """
        )
        assertDeputy(result, "from")
    }

    /** Helper reached through a namespace, arguments passed by name in a different order. */
    @Test
    fun drainViaNamespacedHelperWithNamedArgsIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                namespace ledger {
                    function settle(dst: byte_array, src: byte_array, qty: integer) {
                        update wallet @ { .owner == src } ( .balance -= qty );
                        update wallet @ { .owner == dst } ( .balance += qty );
                    }
                }
                operation transfer(payer: byte_array, payee: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    require(amount > 0, "positive");
                    ledger.settle(qty = amount, src = payer, dst = payee);
                }
            """
        )
        assertDeputy(result, "payer")
    }

    /** The delete branch, two files apart, the identity selected into a local inside the helper. */
    @Test
    fun crossFileDeleteViaLocalSelectedInHelperIsHigh() {
        val result = analyze(
            "members.rell" to """
                module;
                entity membership { key account_id: byte_array; mutable tier: integer; }
                function purge(target: byte_array) {
                    val row = membership @ { .account_id == target };
                    delete row;
                }
            """,
            "main.rell" to """
                module;
                import lib.ft4.auth;
                import members.*;
                operation revoke(who: byte_array) {
                    val account = auth.authenticate();
                    purge(who);
                }
            """
        )
        assertDeputy(result, "who")
    }

    /**
     * An unrelated require() inside the draining helper is NOT delegation:
     * the attacker's obvious next move after the helper fix is to add
     * `require(amount > 0)` to do_transfer and hope the "helper validates"
     * benefit of the doubt silences the rule. Inlining shows what the
     * require actually relates, so only a real binding counts.
     */
    @Test
    fun unrelatedRequireInsideDrainingHelperStillFlags() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function do_transfer(from: byte_array, to: byte_array, amount: integer) {
                    require(amount > 0, "positive");
                    require(from != to, "distinct");
                    update wallet @ { .owner == from } ( .balance -= amount );
                    update wallet @ { .owner == to } ( .balance += amount );
                }
                operation transfer(from: byte_array, to: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    do_transfer(from, to, amount);
                }
            """
        )
        assertDeputy(result, "from")
    }

    /** Delegated ownership check: the helper's require() relates the parameter to the identity. */
    @Test
    fun ownershipRequireInHelperStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function ensure_owner(who: byte_array, acc: auth.account) {
                    require(who == acc.id, "not your wallet");
                }
                function do_transfer(from: byte_array, to: byte_array, amount: integer) {
                    update wallet @ { .owner == from } ( .balance -= amount );
                    update wallet @ { .owner == to } ( .balance += amount );
                }
                operation transfer(from: byte_array, to: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    require(amount > 0, "positive");
                    ensure_owner(from, account);
                    do_transfer(from, to, amount);
                }
            """
        )
        assertNoDeputy(result)
        assertEquals(true, result.ok, result.findings.toString())
    }

    /** A helper that authenticates itself and binds the parameter before debiting. */
    @Test
    fun selfAuthenticatingHelperThatBindsStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function debit_own(who: byte_array, qty: integer) {
                    val acc = auth.authenticate();
                    require(who == acc.id, "not yours");
                    update wallet @ { .owner == who } ( .balance -= qty );
                }
                operation withdraw(from: byte_array, amount: integer) {
                    require(amount > 0, "positive");
                    debit_own(from, amount);
                }
            """
        )
        assertNoDeputy(result)
        assertEquals(true, result.ok, result.findings.toString())
    }

    /** The idiomatic transfer: the helper is handed the authenticated identity, the parameter is only credited. */
    @Test
    fun helperKeyedByAuthenticatedIdentityStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function do_transfer(from: byte_array, to: byte_array, amount: integer) {
                    update wallet @ { .owner == from } ( .balance -= amount );
                    update wallet @ { .owner == to } ( .balance += amount );
                }
                operation pay(to: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    require(amount > 0, "positive");
                    do_transfer(account.id, to, amount);
                }
            """
        )
        assertNoDeputy(result)
        assertEquals(true, result.ok, result.findings.toString())
    }

    /**
     * A helper LOCAL that happens to share the parameter's name is not the
     * parameter: without renaming helper locals, `val to = ...` inside the
     * helper would read as the caller-supplied `to` and flag a debit of the
     * caller's own row.
     */
    @Test
    fun helperLocalSharingParameterNameStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function charge_caller(acc: auth.account, qty: integer) {
                    val to = acc.id;
                    update wallet @ { .owner == to } ( .balance -= qty );
                }
                operation pay(to: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    require(amount > 0, "positive");
                    charge_caller(account, amount);
                    update wallet @ { .owner == to } ( .balance += amount );
                }
            """
        )
        assertNoDeputy(result)
        assertEquals(true, result.ok, result.findings.toString())
    }

    // ---- signer-check-on-untrusted-argument through helpers ----

    private fun assertPhantom(result: RellSecurityCheck.Result, param: String) {
        val hit = result.findings.filter { it.rule == "signer-check-on-untrusted-argument" }
        assertTrue(hit.isNotEmpty(), "phantom signer gate through a helper must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertTrue(hit.any { "'$param'" in it.text }, "finding must name '$param'; got $hit")
        assertEquals(false, result.ok)
    }

    /** The pinned exploit: `check(admin)` require()s is_signer(admin) one call deep. */
    @Test
    fun phantomGateOneHelperDeepIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                entity config { key k: integer; mutable fee: integer; }
                function check(admin: pubkey) { require(op_context.is_signer(admin), "not admin"); }
                operation set_fee(admin: pubkey, fee: integer) {
                    check(admin);
                    update config @ { .k == 0 } ( .fee = fee );
                }
            """
        )
        assertPhantom(result, "admin")
    }

    /** Two levels, namespaced, the key renamed at every hop. */
    @Test
    fun phantomGateTwoHelpersDeepViaNamespaceIsHigh() {
        val result = analyze(
            "guards.rell" to """
                module;
                namespace guards {
                    function is_admin(k: pubkey): boolean = op_context.is_signer(k);
                    function check(candidate: pubkey) { require(is_admin(candidate), "not admin"); }
                }
            """,
            "main.rell" to """
                module;
                import guards.*;
                entity config { key k: integer; mutable fee: integer; }
                operation set_fee(operator_key: pubkey, fee: integer) {
                    guards.check(operator_key);
                    update config @ { .k == 0 } ( .fee = fee );
                }
            """
        )
        assertPhantom(result, "operator_key")
    }

    /** Self-binding through a helper: the key is checked AND stored, so is_signer proves ownership. */
    @Test
    fun selfRegisteringHelperStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity member { key pubkey; mutable handle: text; }
                function enroll(k: pubkey, handle: text) {
                    require(op_context.is_signer(k), "must sign with the key being enrolled");
                    create member ( k, handle );
                }
                operation register(k: pubkey, handle: text) {
                    require(handle.size() > 0, "handle");
                    enroll(k, handle);
                }
            """
        )
        assertTrue(
            result.findings.none { it.rule == "signer-check-on-untrusted-argument" },
            "self-binding through a helper must stay clean; got ${result.findings}"
        )
    }

    /** A parameterless admin helper keyed to module args is the sanctioned gate. */
    @Test
    fun constantAdminHelperStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity config { key k: integer; mutable fee: integer; }
                function require_admin() { require(op_context.is_signer(chain_context.args.admin_pubkey), "not admin"); }
                operation set_fee(fee: integer) {
                    require_admin();
                    require(fee >= 0, "fee");
                    update config @ { .k == 0 } ( .fee = fee );
                }
            """
        )
        assertTrue(
            result.findings.none { it.rule == "signer-check-on-untrusted-argument" },
            result.findings.toString()
        )
        assertEquals(true, result.ok, result.findings.toString())
    }

    // ---- mass-mutation: tautological where-clauses ----

    private fun assertMass(result: RellSecurityCheck.Result) {
        val hit = result.findings.filter { it.rule == "mass-mutation" }
        assertTrue(hit.isNotEmpty(), "tautological where must be flagged as mass-mutation; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertEquals(false, result.ok)
    }

    @Test
    fun deleteWithNotEqualEmptyKeyIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity profile { key account_id: byte_array; mutable bio: text; }
                operation cleanup() {
                    val account = auth.authenticate();
                    delete profile @* { .account_id != x"" };
                }
            """
        )
        assertMass(result)
    }

    @Test
    fun updateWithLiteralTautologyIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity acct { key id: byte_array; mutable balance: integer; }
                operation zero_everyone() {
                    val account = auth.authenticate();
                    update acct @* { 1 == 1 } ( .balance = 0 );
                }
            """
        )
        assertMass(result)
    }

    @Test
    fun deleteWithTrueOrCallerFlagIsHigh() {
        val plain = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity note { key id: integer; body: text; }
                operation wipe() {
                    val account = auth.authenticate();
                    delete note @* { true };
                }
            """
        )
        assertMass(plain)
        val flag = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity note { key id: integer; body: text; }
                operation wipe(all: boolean) {
                    val account = auth.authenticate();
                    require(all, "must opt in");
                    delete note @* { all };
                }
            """
        )
        assertMass(flag)
    }

    /** `or` with a tautological branch selects every row; an `and` with a real key constraint does not. */
    @Test
    fun disjunctionWithTautologyIsHighConjunctionWithKeyStaysClean() {
        val or = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity note { key id: integer; owner: byte_array; }
                operation wipe() {
                    val account = auth.authenticate();
                    delete note @* { .owner == account.id or 1 == 1 };
                }
            """
        )
        assertMass(or)
        val and = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity note { key id: integer; owner: byte_array; tag: text; }
                operation remove_mine() {
                    val account = auth.authenticate();
                    delete note @* { .tag != "" and .owner == account.id };
                }
            """
        )
        assertTrue(and.findings.none { it.rule.startsWith("mass-mutation") }, and.findings.toString())
        assertEquals(true, and.ok, and.findings.toString())
    }

    /** A real literal is a real filter, and a brace inside the literal must not break the parse. */
    @Test
    fun nonEmptyLiteralFilterIsNotTautological() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity profile { key account_id: byte_array; mutable bio: text; }
                operation tidy() {
                    val account = auth.authenticate();
                    update profile @* { .bio != "}" and .account_id == account.id } ( .bio = "" );
                }
            """
        )
        assertTrue(result.findings.none { it.rule.startsWith("mass-mutation") }, result.findings.toString())
        assertEquals(true, result.ok, result.findings.toString())
        assertTrue(RellSecurityCheck.isTautologicalWhere(""".account_id != x""  """))
        assertTrue(RellSecurityCheck.isTautologicalWhere("""op_context.last_block_time > 0"""))
        assertTrue(!RellSecurityCheck.isTautologicalWhere(""".account_id != x"ab""""))
        assertTrue(!RellSecurityCheck.isTautologicalWhere(""".expires_at < op_context.last_block_time"""))
        assertTrue(!RellSecurityCheck.isTautologicalWhere(""".balance != 0"""))
    }

    // ---- bulk-mutation-not-caller-bound (advisory MEDIUM) ----

    @Test
    fun callerIndependentBulkDeleteIsAdvisoryOnly() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity order { key id: integer; owner: byte_array; mutable status: text; }
                operation flush() {
                    val account = auth.authenticate();
                    delete order @* { .status == "pending" };
                }
            """
        )
        val hit = result.findings.filter { it.rule == "bulk-mutation-not-caller-bound" }
        assertTrue(hit.isNotEmpty(), "caller-independent bulk delete should get the advisory; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
        assertEquals(true, result.ok, "advisory must never block: ${result.findings}")
    }

    /** Filters bound to the caller, the chain, a call, or module-args admin gating stay silent. */
    @Test
    fun boundOrGatedBulkMutationsGetNoAdvisory() {
        val shapes = listOf(
            "delete order @* { .owner == account.id };",
            "delete order @* { .status == status };",
            "delete order @* { .expires_at < op_context.last_block_time };",
            "val cutoff = op_context.last_block_time - 1000; delete order @* { .expires_at < cutoff };",
            "delete order @* { .expires_at < deadline() };",
            "require(account.id == chain_context.args.admin, \"admin\"); delete order @* { .status == \"stale\" };"
        )
        shapes.forEach { stmt ->
            val result = analyze(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity order { key id: integer; owner: byte_array; mutable status: text; expires_at: integer; }
                    function deadline(): integer = op_context.last_block_time;
                    operation flush(status: text) {
                        val account = auth.authenticate();
                        require(status.size() > 0, "status");
                        $stmt
                    }
                """
            )
            assertTrue(
                result.findings.none { it.rule == "bulk-mutation-not-caller-bound" },
                "no advisory expected for `$stmt`; got ${result.findings}"
            )
        }
    }

    /** The advisory sees through helpers with the same binding as the deputy rule, and skips @test modules. */
    @Test
    fun advisoryFollowsHelperBindingAndSkipsTestModules() {
        val boundViaHelper = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity order { key id: integer; owner: byte_array; mutable status: text; }
                function drop_status(s: text) { delete order @* { .status == s }; }
                operation flush(status: text) {
                    val account = auth.authenticate();
                    require(status.size() > 0, "status");
                    drop_status(status);
                }
            """
        )
        assertTrue(
            boundViaHelper.findings.none { it.rule == "bulk-mutation-not-caller-bound" },
            boundViaHelper.findings.toString()
        )
        val constantViaHelper = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity order { key id: integer; owner: byte_array; mutable status: text; }
                function drop_pending() { delete order @* { .status == "pending" }; }
                operation flush() {
                    val account = auth.authenticate();
                    drop_pending();
                }
            """
        )
        assertTrue(
            "bulk-mutation-not-caller-bound" in rules(constantViaHelper),
            constantViaHelper.findings.toString()
        )
        val testModule = analyze(
            "test/fixtures.rell" to """
                @test module;
                import main.*;
                function reset() { delete order @* { .status == "pending" }; }
                operation seed() { reset(); }
            """
        )
        assertTrue(
            testModule.findings.none { it.rule == "bulk-mutation-not-caller-bound" },
            testModule.findings.toString()
        )
    }

    // ---- attacking the fix: the parameter under a local's name ----

    /** `val victim = from;` then handing the local to the helper is the same drain. */
    @Test
    fun drainViaLocalAliasOfParamThroughHelperIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function do_transfer(src: byte_array, dst: byte_array, qty: integer) {
                    update wallet @ { .owner == src } ( .balance -= qty );
                    update wallet @ { .owner == dst } ( .balance += qty );
                }
                operation transfer(from: byte_array, to: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    require(amount > 0, "positive");
                    val victim = from;
                    do_transfer(victim, to, amount);
                }
            """
        )
        assertDeputy(result, "from")
    }

    /** The mirror image: a local bound to the authenticated identity is not the parameter. */
    @Test
    fun localBoundToIdentityHandedToHelperStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                function do_transfer(src: byte_array, dst: byte_array, qty: integer) {
                    update wallet @ { .owner == src } ( .balance -= qty );
                    update wallet @ { .owner == dst } ( .balance += qty );
                }
                operation pay(to: byte_array, amount: integer) {
                    val account = auth.authenticate();
                    require(amount > 0, "positive");
                    val me = account.id;
                    do_transfer(me, to, amount);
                }
            """
        )
        assertNoDeputy(result)
        assertEquals(true, result.ok, result.findings.toString())
    }

    /** `val k = admin; check(k);` - the phantom gate through a local and a helper. */
    @Test
    fun phantomGateViaLocalAliasIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                entity config { key k: integer; mutable fee: integer; }
                function check(key: pubkey) { require(op_context.is_signer(key), "not admin"); }
                operation set_fee(admin: pubkey, fee: integer) {
                    val k = admin;
                    check(k);
                    update config @ { .k == 0 } ( .fee = fee );
                }
            """
        )
        assertPhantom(result, "admin")
    }

    /** ...but a local that is checked AND used to key the write is self-binding. */
    @Test
    fun localAliasCheckedAndUsedStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity member { key pubkey; mutable handle: text; }
                operation rename(k: pubkey, handle: text) {
                    val who = k;
                    require(op_context.is_signer(who), "sign with your own key");
                    require(handle.size() > 0, "handle");
                    update member @ { .pubkey == who } ( .handle = handle );
                }
            """
        )
        assertTrue(
            result.findings.none { it.rule == "signer-check-on-untrusted-argument" },
            result.findings.toString()
        )
    }

    /** Dressing the tautology up as a method call or a negation reaches the advisory, never silence. */
    @Test
    fun disguisedTautologyGetsAtLeastTheAdvisory() {
        listOf(
            "delete profile @* { .account_id.size() >= 0 };",
            "delete profile @* { not (.account_id == x\"\") };",
            "delete profile @* { .tier >= 0 or .tier < 0 };"
        ).forEach { stmt ->
            val result = analyze(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    entity profile { key account_id: byte_array; mutable tier: integer; }
                    operation cleanup() {
                        val account = auth.authenticate();
                        $stmt
                    }
                """
            )
            assertTrue(
                result.findings.any { it.rule == "mass-mutation" || it.rule == "bulk-mutation-not-caller-bound" },
                "`$stmt` must not pass in silence; got ${result.findings}"
            )
        }
    }
}
