package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the authorization-correctness and economic-invariant
 * rules added after two independent audits agreed on the same blind spot:
 * "an auth token appears somewhere + a require() exists" was being certified
 * as secure, while a DAO that let one zero-contribution account drain the
 * treasury and an oracle that minted unbacked value both came back ok:true.
 *
 * Every rule here has BOTH directions: the exploit sample must produce the
 * finding, and idiomatic secure FT4 code must stay clean - a noisy gate
 * trains agents to ignore the gate, which is worse than no gate.
 */
class AuthorizationAndInvariantRulesTest {

    private fun rules(result: RellSecurityCheck.Result): List<String> = result.findings.map { it.rule }

    // ---- authorization-not-bound-to-caller (confused deputy) ----

    /**
     * The adversary's drain: the operation authenticates (so the old
     * unauthenticated-mutation rule is silent) but then debits rows selected
     * by a caller-supplied account parameter that is never related to the
     * authenticated identity. Anyone drains anyone.
     */
    @Test
    fun authenticatedDrainByCallerSuppliedAccountIsHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation withdraw(from_account: byte_array, amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update wallet @ { .owner == from_account } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "authorization-not-bound-to-caller" }
        assertTrue(hit.isNotEmpty(), "confused-deputy drain must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertEquals(false, result.ok)
    }

    /** Deleting rows keyed by a caller-supplied owner is the same class. */
    @Test
    fun authenticatedDeleteByCallerSuppliedOwnerIsHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity listing { key id: integer; owner: byte_array; }
                    operation cancel_listing(owner: byte_array) {
                        val account = auth.authenticate();
                        delete listing @* { .owner == owner };
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "authorization-not-bound-to-caller" in rules(result),
            "delete keyed by unbound caller param must be flagged; got ${result.findings}"
        )
    }

    /** Self-keyed mutation (`.owner == account.id`) is THE idiomatic pattern - must stay clean. */
    @Test
    fun selfKeyedMutationStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation withdraw(amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update wallet @ { .owner == account.id } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(result.ok, result.findings.toString())
    }

    /** Param explicitly bound to the authenticated identity is authorized - clean. */
    @Test
    fun paramBoundToAuthenticatedIdStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation close_wallet(owner: byte_array) {
                        val account = auth.authenticate();
                        require(owner == account.id, "not your wallet");
                        delete wallet @ { .owner == owner };
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            result.findings.toString()
        )
    }

    /** Admin ops keyed to chain_context.args are the sanctioned break-glass pattern - clean. */
    @Test
    fun adminOpKeyedToChainContextArgsStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation admin_seize(owner: byte_array) {
                        val account = auth.authenticate();
                        require(account.id == chain_context.args.admin_account, "admin only");
                        delete wallet @ { .owner == owner };
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            result.findings.toString()
        )
    }

    /**
     * A transfer that debits self and credits a caller-named recipient is the
     * most idiomatic value-moving shape there is; the credit (`+=` only) to
     * the unbound param must not read as a drain.
     */
    @Test
    fun creditOnlyMutationOfCallerSuppliedRecipientStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation tip(to_user: byte_array, amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update wallet @ { .owner == account.id } ( .balance -= amount );
                        update wallet @ { .owner == to_user } ( .balance += amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            result.findings.toString()
        )
    }

    // ---- signer-check-on-untrusted-argument (phantom gate) ----

    /**
     * `require(is_signer(admin))` where `admin` is an operation PARAMETER: the
     * attacker passes their own pubkey and signs with it - the "gate" always
     * passes and the privileged mutation runs. The old auth scan counted
     * is_signer( as authentication and stayed silent.
     */
    @Test
    fun signerCheckOnCallerSuppliedKeyGatingPrivilegedMutationIsHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity config { mutable fee: integer; }
                    operation set_fee(admin: pubkey, fee: integer) {
                        require(op_context.is_signer(admin), "admin only");
                        require(fee >= 0, "fee");
                        update config @ { } ( .fee = fee );
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "signer-check-on-untrusted-argument" }
        assertTrue(hit.isNotEmpty(), "phantom signer gate must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertEquals(false, result.ok)
    }

    /** The gate is just as phantom when the mutation happens via a helper. */
    @Test
    fun phantomSignerGateOverTransitiveMutationIsHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity acct { key id: byte_array; mutable balance: integer; }
                    function do_mint(to: byte_array, amount: integer) {
                        update acct @ { .id == to } ( .balance += amount );
                    }
                    operation mint(caller: pubkey, to: byte_array, amount: integer) {
                        require(op_context.is_signer(caller), "not authorized");
                        do_mint(to, amount);
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            "signer-check-on-untrusted-argument" in rules(result),
            result.findings.toString()
        )
    }

    /**
     * The idiomatic self-binding pattern: prove you control the key you are
     * registering, then use that same key in the write. The parameter is bound
     * to a real signer AND used - this is a gate, not a phantom.
     */
    @Test
    fun selfBindingSignerCheckStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity user { key pubkey; name: text; }
                    operation register(pubkey, name: text) {
                        require(op_context.is_signer(pubkey), "must sign with the key being registered");
                        require(name.size() > 0, "name required");
                        create user(pubkey, name);
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "signer-check-on-untrusted-argument" },
            result.findings.toString()
        )
    }

    /** is_signer against a module-args constant is the real admin gate - clean. */
    @Test
    fun signerCheckAgainstChainContextArgsStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity config { mutable fee: integer; }
                    operation set_fee(fee: integer) {
                        require(op_context.is_signer(chain_context.args.admin_pubkey), "admin only");
                        require(fee >= 0, "fee");
                        update config @ { } ( .fee = fee );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "signer-check-on-untrusted-argument" },
            result.findings.toString()
        )
    }

    // ---- value-op-without-transfer-flag ----
    // Ground truth: FT4 has_flags is flags.contains_all(required_flags), and
    // contains_all([]) is ALWAYS true (raw-ft4-src v1.1.0r
    // core/accounts/module.rell:502-504). flags = [] therefore lets ANY auth
    // descriptor - including a limited session key the user believed could not
    // spend - call every operation the handler governs.

    private val emptyFlagsHandler = """
        module;
        import lib.ft4.auth;
        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            flags = []
        );
    """.trimIndent()

    @Test
    fun valueMutationUnderEmptyFlagsHandlerIsMedium() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to emptyFlagsHandler + """

                    entity acct { key id: byte_array; mutable balance: integer; }
                    operation spend(amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update acct @ { .id == account.id } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "value-op-without-transfer-flag" }
        assertTrue(hit.isNotEmpty(), "balance debit under flags=[] must be flagged; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
    }

    /** The value mutation is just as exposed when it happens inside a helper. */
    @Test
    fun transitiveValueMutationUnderEmptyFlagsHandlerIsMedium() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to emptyFlagsHandler + """

                    entity acct { key id: byte_array; mutable balance: integer; }
                    function debit(id: byte_array, amount: integer) {
                        update acct @ { .id == id } ( .balance -= amount );
                    }
                    operation spend(amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        debit(account.id, amount);
                    }
                """.trimIndent()
            )
        )
        assertTrue("value-op-without-transfer-flag" in rules(result), result.findings.toString())
    }

    /** A handler requiring the Transfer flag is the documented fix - clean. */
    @Test
    fun valueMutationUnderTransferFlagHandlerStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    @extend(auth.auth_handler)
                    function () = auth.add_auth_handler(
                        flags = ["T"]
                    );
                    entity acct { key id: byte_array; mutable balance: integer; }
                    operation spend(amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update acct @ { .id == account.id } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "value-op-without-transfer-flag" },
            result.findings.toString()
        )
    }

    /**
     * The scaffold's own golden template: flags = [] governing an operation
     * that moves NO value (creates a note). Explicitly documented as fine -
     * must stay clean or the gate flags our own scaffold.
     */
    @Test
    fun nonValueOpUnderEmptyFlagsHandlerStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to emptyFlagsHandler + """

                    entity note { index owner: byte_array; body: text; }
                    operation add_note(body: text) {
                        val account = auth.authenticate();
                        require(body.size() > 0, "note must not be empty");
                        create note(owner = account.id, body);
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "value-op-without-transfer-flag" },
            result.findings.toString()
        )
    }

    /**
     * With MIXED handlers (some scoped handler carries flags) we cannot tell
     * statically which one governs which operation - stay quiet rather than
     * noisy.
     */
    @Test
    fun mixedHandlersAreNotFlagged() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    import lib.ft4.auth;
                    @extend(auth.auth_handler)
                    function () = auth.add_auth_handler(
                        flags = []
                    );
                    @extend(auth.auth_handler)
                    function () = auth.add_auth_handler(
                        scope = "spend",
                        flags = ["T"]
                    );
                    entity acct { key id: byte_array; mutable balance: integer; }
                    operation spend(amount: integer) {
                        val account = auth.authenticate();
                        require(amount > 0, "positive");
                        update acct @ { .id == account.id } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "value-op-without-transfer-flag" },
            result.findings.toString()
        )
    }

    // ---- mass-mutation ----

    @Test
    fun deleteAllRowsIsHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity note { key id: integer; body: text; }
                    operation wipe() {
                        val account = auth.authenticate();
                        delete note @* { };
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "mass-mutation" }
        assertTrue(hit.isNotEmpty(), "delete @* {} must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertEquals(false, result.ok)
    }

    @Test
    fun updateAllRowsIsHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity acct { key id: byte_array; mutable balance: integer; }
                    operation zero_everyone() {
                        val account = auth.authenticate();
                        update acct @* { } ( .balance = 0 );
                    }
                """.trimIndent()
            )
        )
        assertTrue("mass-mutation" in rules(result), result.findings.toString())
    }

    /** Filtered mutations and @*-queries are normal Rell - clean. */
    @Test
    fun filteredMutationAndQueryStayClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity note { key id: integer; owner: byte_array; body: text; }
                    operation remove_mine() {
                        val account = auth.authenticate();
                        delete note @* { .owner == account.id };
                    }
                    query all_notes() = note @* { } ( .body );
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule.startsWith("mass-mutation") },
            result.findings.toString()
        )
    }

    /** Wiping tables is what test fixtures DO - reuse the test-surface downgrade. */
    @Test
    fun massMutationInTestModuleDowngradesToMedium() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "test/fixtures.rell" to """
                    @test module;
                    entity scratch { key id: integer; }
                    function reset() {
                        delete scratch @* { };
                    }
                    operation seed() {
                        delete scratch @* { };
                    }
                """.trimIndent()
            )
        )
        val hit = result.findings.filter { it.rule == "mass-mutation-test-surface" }
        assertTrue(hit.isNotEmpty(), result.findings.toString())
        assertTrue(hit.all { it.severity == "MEDIUM" }, result.findings.toString())
        assertTrue(result.findings.none { it.rule == "mass-mutation" }, result.findings.toString())
    }

    // ---- gate-fatigue fixes: an ignored gate is worse than no gate ----

    /**
     * A public BRID constant is a chain identifier, not key material - HIGH
     * here trains agents to wave the rule through. Named like an identifier,
     * it reports MEDIUM under its own rule.
     */
    @Test
    fun bridConstantIsMediumChainIdentifierNotHighKeyMaterial() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to
                    "module;\nval TARGET_BRID = x\"15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304\";\n"
            )
        )
        assertTrue(
            result.findings.none { it.rule == "hardcoded-key-material" },
            result.findings.toString()
        )
        val hit = result.findings.filter { it.rule == "hardcoded-chain-identifier" }
        assertTrue(hit.isNotEmpty(), result.findings.toString())
        assertEquals("MEDIUM", hit.first().severity)
    }

    /** A hex literal named like a private key stays HIGH - even if it also says 'id'. */
    @Test
    fun privateKeyHexStaysHighKeyMaterial() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to
                    "module;\nval signer_priv_key_id = x\"15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304\";\n"
            )
        )
        assertTrue(
            result.findings.any { it.rule == "hardcoded-key-material" && it.severity == "HIGH" },
            result.findings.toString()
        )
    }

    /** An anonymous 64-hex literal (no identifier to judge by) stays HIGH. */
    @Test
    fun unnamedHexLiteralStaysHigh() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to
                    "module;\nfunction f() { g(x\"15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304\"); }\nfunction g(b: byte_array) {}\n"
            )
        )
        assertTrue(
            result.findings.any { it.rule == "hardcoded-key-material" && it.severity == "HIGH" },
            result.findings.toString()
        )
    }

    /**
     * Input validation done in a helper is still validation: the auth closure
     * already walks helpers, the require() closure must too, or every
     * validate_x() helper pattern eats an unvalidated-inputs finding.
     */
    @Test
    fun requireInsideHelperCountsAsValidation() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity note { key id: text; }
                    function validate_id(id: text) {
                        require(id.size() > 0, "empty id");
                        require(id.size() <= 64, "id too long");
                    }
                    operation add_note(id: text) {
                        validate_id(id);
                        create note(id);
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "unvalidated-inputs" },
            result.findings.toString()
        )
    }

    /** is_signer(<that param>) binds the param to an actual transaction signer - clean. */
    @Test
    fun paramCheckedWithIsSignerStaysClean() {
        val result = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to """
                    module;
                    entity wallet { key owner: byte_array; mutable balance: integer; }
                    operation withdraw(owner: byte_array, amount: integer) {
                        require(op_context.is_signer(owner), "not signer");
                        require(amount > 0, "positive");
                        update wallet @ { .owner == owner } ( .balance -= amount );
                    }
                """.trimIndent()
            )
        )
        assertTrue(
            result.findings.none { it.rule == "authorization-not-bound-to-caller" },
            result.findings.toString()
        )
    }
}
