package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for the 2026-09-02 gap-closure round: every rule here has
 * (1) the exploit shape from the corpus, (2) the evasions we deliberately
 * tried against our own first version - rename, alias through a local,
 * wrap in a helper, split across files, drop the braces - and (3) idiomatic
 * secure code that must stay clean, because a noisy gate trains agents to
 * route around it.
 *
 * The standing lesson (a2-renamed): never key a rule on anything the attacker
 * chooses. Detection below keys on types, positions, and data flow; the only
 * name-keyed rule is the query-secret advisory, where the names are the
 * AUTHOR's own and the threat is carelessness, not evasion.
 */
class GapClosureRulesTest {

    private fun analyze(vararg files: Pair<String, String>) =
        RellSecurityCheck.analyze(files.toMap())

    private fun rules(result: RellSecurityCheck.Result) = result.findings.map { it.rule }

    // ================= amount-without-lower-bound (C1) =================

    @Test
    fun negativeAmountMintIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    update vault @ { .owner == owner } ( .balance -= amount );
                }
            """.trimIndent()
        )
        val hit = result.findings.filter { it.rule == "amount-without-lower-bound" }
        assertTrue(hit.isNotEmpty(), "unbounded debit amount must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertEquals(false, result.ok)
    }

    /** Param and field renamed - the rule keys on type + use, never names. */
    @Test
    fun renamedAmountAndFieldStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity hoard { key owner: byte_array; mutable gold_units: integer; }
                operation take(owner: byte_array, delta: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    update hoard @ { .owner == owner } ( .gold_units -= delta );
                }
            """.trimIndent()
        )
        assertTrue("amount-without-lower-bound" in rules(result), result.findings.toString())
    }

    /** Routing the parameter through a local must not launder it. */
    @Test
    fun aliasedAmountStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    val net = amount;
                    val fee_adjusted = net;
                    update vault @ { .owner == owner } ( .balance -= fee_adjusted );
                }
            """.trimIndent()
        )
        assertTrue("amount-without-lower-bound" in rules(result), result.findings.toString())
    }

    /** Wrapping the debit in a helper (that never require()s) must not hide it. */
    @Test
    fun helperWrappedDebitStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                function do_debit(owner: byte_array, n: integer) {
                    update vault @ { .owner == owner } ( .balance -= n );
                }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    do_debit(owner, amount);
                }
            """.trimIndent()
        )
        assertTrue("amount-without-lower-bound" in rules(result), result.findings.toString())
    }

    /** An unsigned credit to someone ELSE's value row is the same inversion. */
    @Test
    fun unboundedCreditToValueFieldStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                operation tip(to: byte_array, amount: integer) {
                    require(op_context.is_signer(chain_context.args.admin), "not admin");
                    update wallet @ { .owner == to } ( .balance += amount );
                }
                struct module_args { admin: pubkey; }
            """.trimIndent()
        )
        assertTrue("amount-without-lower-bound" in rules(result), result.findings.toString())
    }

    /** `.balance = .balance - amount` is `-=` spelled differently. */
    @Test
    fun plainAssignSubtractionStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    val v = vault @ { .owner == owner };
                    update v ( .balance = v.balance - amount );
                }
            """.trimIndent()
        )
        assertTrue("amount-without-lower-bound" in rules(result), result.findings.toString())
    }

    /** `.balance += -amount` is also `-=` spelled differently. */
    @Test
    fun negatedCreditStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    update vault @ { .owner == owner } ( .balance += -amount );
                }
            """.trimIndent()
        )
        assertTrue("amount-without-lower-bound" in rules(result), result.findings.toString())
    }

    @Test
    fun lowerBoundedAmountStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    require(amount > 0, "positive");
                    update vault @ { .owner == owner } ( .balance -= amount );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "amount-without-lower-bound" }, result.findings.toString())
    }

    /** A bound on the derived local is a real bound. */
    @Test
    fun boundOnDerivedLocalStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    val net = amount / 100;
                    require(net > 0, "too small");
                    update vault @ { .owner == owner } ( .balance -= net );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "amount-without-lower-bound" }, result.findings.toString())
    }

    /** Validation delegated to a require()-bearing helper counts. */
    @Test
    fun helperValidatedAmountStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                function check_amount(n: integer) { require(n > 0, "positive"); }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    check_amount(amount);
                    update vault @ { .owner == owner } ( .balance -= amount );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "amount-without-lower-bound" }, result.findings.toString())
    }

    /**
     * DELIBERATE benefit of the doubt: a callee the submission does not define
     * (library/builtin, e.g. abs()) may validate or normalize the amount where
     * this scan cannot see. Rell compiles before analyze runs, so the callee
     * is a real resolvable function, not an attacker-invented no-op.
     */
    @Test
    fun amountPassedToUnknownCalleeGetsBenefitOfDoubt() {
        val result = analyze(
            "main.rell" to """
                module;
                entity vault { key owner: byte_array; mutable balance: integer; }
                operation withdraw(owner: byte_array, amount: integer) {
                    require(op_context.is_signer(owner), "not owner");
                    update vault @ { .owner == owner } ( .balance -= abs(amount) );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "amount-without-lower-bound" }, result.findings.toString())
    }

    // ================= conditional-auth-bypass (A5) =================

    @Test
    fun authInsideCallerControlledIfIsHigh() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                operation moderate(id: integer, as_admin: boolean) {
                    if (as_admin) {
                        require(op_context.is_signer(chain_context.args.admin_pubkey), "not admin");
                    }
                    update post @ { .id == id } ( .hidden = true );
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        val hit = result.findings.filter { it.rule == "conditional-auth-bypass" }
        assertTrue(hit.isNotEmpty(), "conditional auth must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertEquals(false, result.ok)
    }

    /** Dropping the braces must not drop the finding. */
    @Test
    fun bracelessConditionalAuthStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                operation moderate(id: integer, as_admin: boolean) {
                    if (as_admin) require(op_context.is_signer(chain_context.args.admin_pubkey), "not admin");
                    update post @ { .id == id } ( .hidden = true );
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue("conditional-auth-bypass" in rules(result), result.findings.toString())
    }

    /** Auth only in the else-branch covers only one path. */
    @Test
    fun elseOnlyAuthStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                operation moderate(id: integer, fast_path: boolean) {
                    if (fast_path) { } else {
                        require(op_context.is_signer(chain_context.args.admin_pubkey), "not admin");
                    }
                    update post @ { .id == id } ( .hidden = true );
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue("conditional-auth-bypass" in rules(result), result.findings.toString())
    }

    /** Mutation in one branch, auth in an INDEPENDENT branch: paths diverge. */
    @Test
    fun independentBranchesStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                operation moderate(id: integer, a: boolean, b: boolean) {
                    if (a) { update post @ { .id == id } ( .hidden = true ); }
                    if (b) { require(op_context.is_signer(chain_context.args.admin_pubkey), "not admin"); }
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue("conditional-auth-bypass" in rules(result), result.findings.toString())
    }

    /** Auth via a helper called conditionally is still conditional. */
    @Test
    fun conditionallyCalledAuthHelperStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                function require_admin() {
                    require(op_context.is_signer(chain_context.args.admin_pubkey), "not admin");
                }
                operation moderate(id: integer, as_admin: boolean) {
                    if (as_admin) { require_admin(); }
                    update post @ { .id == id } ( .hidden = true );
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue("conditional-auth-bypass" in rules(result), result.findings.toString())
    }

    /** A loop body runs zero times for an empty list - auth inside is conditional. */
    @Test
    fun loopOnlyAuthStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                operation moderate(id: integer, checks: list<pubkey>) {
                    for (k in checks) {
                        require(op_context.is_signer(k), "not signer");
                    }
                    update post @ { .id == id } ( .hidden = true );
                }
            """.trimIndent()
        )
        assertTrue("conditional-auth-bypass" in rules(result), result.findings.toString())
    }

    /** Unconditional auth with a conditional mutation is THE idiomatic shape. */
    @Test
    fun unconditionalAuthConditionalMutationStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity proposal { key id: integer; mutable yes: integer; mutable no: integer; }
                operation vote(id: integer, support: boolean) {
                    val account = auth.authenticate();
                    val p = proposal @ { .id == id };
                    if (support) update p ( .yes += 1 ); else update p ( .no += 1 );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "conditional-auth-bypass" }, result.findings.toString())
    }

    /** Every branch of an if/else authenticates: all completing paths covered. */
    @Test
    fun fullCoverageIfElseAuthStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                operation moderate(id: integer, as_admin: boolean) {
                    if (as_admin) {
                        require(op_context.is_signer(chain_context.args.admin_pubkey), "not admin");
                    } else {
                        require(op_context.is_signer(chain_context.args.moderator_pubkey), "not moderator");
                    }
                    update post @ { .id == id } ( .hidden = true );
                }
                struct module_args { admin_pubkey: pubkey; moderator_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "conditional-auth-bypass" }, result.findings.toString())
    }

    /** `if (not is_signer(...)) return;` guards everything after it. */
    @Test
    fun earlyExitSignerGuardStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                operation moderate(id: integer) {
                    if (not op_context.is_signer(chain_context.args.admin_pubkey)) return;
                    update post @ { .id == id } ( .hidden = true );
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "conditional-auth-bypass" }, result.findings.toString())
    }

    /** Mutation inside the positively signer-guarded branch is authorized. */
    @Test
    fun mutationInsideSignerGuardedBranchStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable hidden: boolean; }
                operation moderate(id: integer) {
                    if (op_context.is_signer(chain_context.args.admin_pubkey)) {
                        update post @ { .id == id } ( .hidden = true );
                    }
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "conditional-auth-bypass" }, result.findings.toString())
    }

    // ================= get_signers must gate (A4) =================

    @Test
    fun signerPresenceOnlyReadIsUnauthenticated() {
        val result = analyze(
            "main.rell" to """
                module;
                entity config { key k: integer; mutable admin: byte_array; }
                operation grant_admin(target: byte_array) {
                    val signers = op_context.get_signers();
                    update config @ { .k == 0 } ( .admin = target );
                }
            """.trimIndent()
        )
        assertTrue("unauthenticated-mutation" in rules(result), result.findings.toString())
    }

    /** `.size()` is a presence check every signed tx passes - not a gate. */
    @Test
    fun signersSizeCheckOnlyStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity config { key k: integer; mutable admin: byte_array; }
                operation grant_admin(target: byte_array) {
                    val signers = op_context.get_signers();
                    require(signers.size() > 0, "unsigned");
                    update config @ { .k == 0 } ( .admin = target );
                }
            """.trimIndent()
        )
        assertTrue("unauthenticated-mutation" in rules(result), result.findings.toString())
    }

    /** `.empty()` is the same presence check. */
    @Test
    fun signersEmptyCheckOnlyStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                entity config { key k: integer; mutable admin: byte_array; }
                operation grant_admin(target: byte_array) {
                    val signers = op_context.get_signers();
                    require(not signers.empty(), "unsigned");
                    update config @ { .k == 0 } ( .admin = target );
                }
            """.trimIndent()
        )
        assertTrue("unauthenticated-mutation" in rules(result), result.findings.toString())
    }

    /** Deriving the mutation FROM the signer list is the filechain idiom - clean. */
    @Test
    fun signerDerivedMutationStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity uploader { key pubkey: byte_array; }
                operation register_signers() {
                    for (s in op_context.get_signers()) {
                        create uploader ( s );
                    }
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    /** Membership-testing the signer list against a trusted key is a real gate. */
    @Test
    fun signerMembershipCheckStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity config { key k: integer; mutable flag: boolean; }
                operation set_flag() {
                    require(chain_context.args.admin_pubkey in op_context.get_signers(), "not admin");
                    update config @ { .k == 0 } ( .flag = true );
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    /** Comparing a bound signer list against trusted state is a real gate. */
    @Test
    fun boundSignersComparedStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity config { key k: integer; mutable flag: boolean; }
                operation set_flag() {
                    val signers = op_context.get_signers();
                    require(signers[0] == chain_context.args.admin_pubkey, "not admin");
                    update config @ { .k == 0 } ( .flag = true );
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    // ================= auth-marker binding (A8) =================

    @Test
    fun localNamespaceAuthSpoofIsUnauthenticated() {
        val result = analyze(
            "main.rell" to """
                module;
                namespace auth { function authenticate() { } }
                entity profile { key account_id: byte_array; mutable bio: text; }
                operation hijack(target: byte_array) {
                    auth.authenticate();
                    delete profile @ { .account_id == target };
                }
            """.trimIndent()
        )
        assertTrue("unauthenticated-mutation" in rules(result), result.findings.toString())
    }

    /**
     * The variant the old incidental catch missed: a spoof plus a mutation
     * keyed by nothing account-typed sailed through both the marker scan and
     * the confused-deputy rule. It must now read as unauthenticated.
     */
    @Test
    fun spoofWithNonAccountKeyedMutationStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                namespace auth { function authenticate() { } }
                entity counter { key k: integer; mutable v: integer; }
                operation reset(n: integer) {
                    auth.authenticate();
                    update counter @ { .k == n } ( .v = 0 );
                }
            """.trimIndent()
        )
        assertTrue("unauthenticated-mutation" in rules(result), result.findings.toString())
    }

    /** Aliasing a non-FT4 module AS `auth` (split across files) is the same spoof. */
    @Test
    fun aliasImportSpoofAcrossFilesStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                import auth: fake_auth;
                entity profile { key account_id: byte_array; mutable bio: text; }
                operation hijack(target: byte_array) {
                    auth.authenticate();
                    delete profile @ { .account_id == target };
                }
            """.trimIndent(),
            "fake_auth/module.rell" to """
                module;
                function authenticate() { }
            """.trimIndent()
        )
        assertTrue("unauthenticated-mutation" in rules(result), result.findings.toString())
    }

    /**
     * A file that REALLY imports lib.ft4.auth keeps its marker even while a
     * spoof exists elsewhere in the submission - a genuine import and a
     * same-named local namespace cannot coexist past the compiler.
     */
    @Test
    fun genuineFt4ImportTrustedDespiteSpoofElsewhere() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity profile { key account_id: byte_array; mutable bio: text; }
                operation update_my_bio(bio: text) {
                    val account = auth.authenticate();
                    require(bio.size() <= 500, "too long");
                    update profile @ { .account_id == account.id } ( .bio = bio );
                }
            """.trimIndent(),
            "other/module.rell" to """
                module;
                namespace auth { function authenticate() { } }
                function unrelated() { }
            """.trimIndent()
        )
        assertTrue(
            result.findings.none { it.rule == "unauthenticated-mutation" && it.file == "main.rell" },
            result.findings.toString()
        )
    }

    // ================= iccf-proof-without-provenance (E2) =================

    @Test
    fun proofWithoutChainBindingIsAdvisory() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.iccf;
                entity credited { key tx_hash: byte_array; }
                operation claim(tx_hash: byte_array) {
                    iccf.require_valid_proof(tx_hash, require_anchored_proof = true);
                    create credited ( tx_hash );
                }
            """.trimIndent()
        )
        val hit = result.findings.filter { it.rule == "iccf-proof-without-provenance" }
        assertTrue(hit.isNotEmpty(), "proof without provenance must be advised; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
        // The genuine library call still counts as auth - advisory, not blocking.
        assertTrue(result.findings.none { it.rule == "unauthenticated-mutation" }, result.findings.toString())
    }

    /** A local no-op require_valid_proof is a spoof: the auth marker dies with it. */
    @Test
    fun localNoopProofSpoofIsUnauthenticated() {
        val result = analyze(
            "main.rell" to """
                module;
                entity credited { key tx_hash: byte_array; }
                function require_valid_proof(tx_hash: byte_array) { }
                operation claim(tx_hash: byte_array, amount: integer) {
                    require_valid_proof(tx_hash);
                    create credited ( tx_hash );
                }
            """.trimIndent()
        )
        assertTrue("unauthenticated-mutation" in rules(result), result.findings.toString())
        assertTrue("iccf-proof-without-provenance" in rules(result), result.findings.toString())
    }

    /** The documented pattern - proof PLUS source-chain binding - stays clean. */
    @Test
    fun proofWithChainBindingStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.iccf;
                struct module_args { source_brid: byte_array; }
                entity credited { key tx_hash: byte_array; }
                operation claim(tx_hash: byte_array, source: byte_array) {
                    require(source == chain_context.args.source_brid, "wrong source chain");
                    iccf.require_valid_proof(tx_hash, require_anchored_proof = true);
                    create credited ( tx_hash );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "iccf-proof-without-provenance" }, result.findings.toString())
    }

    // ================= icmf-sender-not-validated (E1) =================

    private val icmfLib = "lib/icmf/module.rell" to """
        module;
        @extendable function receive_icmf_message(sender: byte_array, topic: text, body: gtv);
    """.trimIndent()

    @Test
    fun icmfReceiverMutatingWithoutSenderCheckIsHigh() {
        val result = analyze(
            icmfLib,
            "main.rell" to """
                module;
                import lib.icmf.*;
                entity price { key sym: text; mutable v: integer; }
                @extend(receive_icmf_message) function on_msg(sender: byte_array, topic: text, body: gtv) {
                    val p = integer.from_gtv(body);
                    update price @ { .sym == "CHR" } ( .v = p );
                }
            """.trimIndent()
        )
        val hit = result.findings.filter { it.rule == "icmf-sender-not-validated" }
        assertTrue(hit.isNotEmpty(), "unvalidated ICMF sender must be flagged; got ${result.findings}")
        assertEquals("HIGH", hit.first().severity)
        assertEquals(false, result.ok)
    }

    /** The sender is the FIRST parameter by position - its name must not matter. */
    @Test
    fun renamedSenderParamStillCaught() {
        val result = analyze(
            icmfLib,
            "main.rell" to """
                module;
                import lib.icmf.*;
                entity price { key sym: text; mutable v: integer; }
                @extend(receive_icmf_message) function on_msg(src: byte_array, t: text, payload: gtv) {
                    update price @ { .sym == "CHR" } ( .v = integer.from_gtv(payload) );
                }
            """.trimIndent()
        )
        assertTrue("icmf-sender-not-validated" in rules(result), result.findings.toString())
    }

    /** Storing the sender in an audit row is not validating it. */
    @Test
    fun senderStoredButNotCheckedStillCaught() {
        val result = analyze(
            icmfLib,
            "main.rell" to """
                module;
                import lib.icmf.*;
                entity price { key sym: text; mutable v: integer; }
                entity audit_row { at: integer; who: byte_array; }
                @extend(receive_icmf_message) function on_msg(sender: byte_array, topic: text, body: gtv) {
                    create audit_row ( op_context.last_block_time, sender );
                    update price @ { .sym == "CHR" } ( .v = integer.from_gtv(body) );
                }
            """.trimIndent()
        )
        assertTrue("icmf-sender-not-validated" in rules(result), result.findings.toString())
    }

    /** Checking only the topic is not a trust boundary - the sender chain is. */
    @Test
    fun topicOnlyCheckStillCaught() {
        val result = analyze(
            icmfLib,
            "main.rell" to """
                module;
                import lib.icmf.*;
                entity price { key sym: text; mutable v: integer; }
                @extend(receive_icmf_message) function on_msg(sender: byte_array, topic: text, body: gtv) {
                    require(topic == "L_price", "wrong topic");
                    update price @ { .sym == "CHR" } ( .v = integer.from_gtv(body) );
                }
            """.trimIndent()
        )
        assertTrue("icmf-sender-not-validated" in rules(result), result.findings.toString())
    }

    /** Binding the sender to configured trust is the directory-chain pattern - clean. */
    @Test
    fun senderBoundToModuleArgsStaysClean() {
        val result = analyze(
            icmfLib,
            "main.rell" to """
                module;
                import lib.icmf.*;
                struct module_args { trusted_chain: byte_array; }
                entity price { key sym: text; mutable v: integer; }
                @extend(receive_icmf_message) function on_msg(sender: byte_array, topic: text, body: gtv) {
                    require(sender == chain_context.args.trusted_chain, "untrusted sender");
                    update price @ { .sym == "CHR" } ( .v = integer.from_gtv(body) );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "icmf-sender-not-validated" }, result.findings.toString())
    }

    /** A registry lookup of the sender is validation too. */
    @Test
    fun senderRegistryLookupStaysClean() {
        val result = analyze(
            icmfLib,
            "main.rell" to """
                module;
                import lib.icmf.*;
                entity trusted_chain { key rid: byte_array; }
                entity price { key sym: text; mutable v: integer; }
                @extend(receive_icmf_message) function on_msg(sender: byte_array, topic: text, body: gtv) {
                    require(exists(trusted_chain @? { .rid == sender }), "untrusted sender");
                    update price @ { .sym == "CHR" } ( .v = integer.from_gtv(body) );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "icmf-sender-not-validated" }, result.findings.toString())
    }

    /** A receiver that mutates nothing has nothing to guard. */
    @Test
    fun nonMutatingReceiverStaysClean() {
        val result = analyze(
            icmfLib,
            "main.rell" to """
                module;
                import lib.icmf.*;
                @extend(receive_icmf_message) function on_msg(sender: byte_array, topic: text, body: gtv) {
                    print(topic);
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "icmf-sender-not-validated" }, result.findings.toString())
    }

    // ================= unvalidated-stored-parameter (C2) =================

    @Test
    fun requireOnOneParamDoesNotCoverStoredText() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity ping_log { at: integer; y: text; }
                operation ping(x: integer, y: text) {
                    val account = auth.authenticate();
                    require(x > 0, "positive");
                    create ping_log ( x, y );
                }
            """.trimIndent()
        )
        val hit = result.findings.filter { it.rule == "unvalidated-stored-parameter" }
        assertTrue(hit.isNotEmpty(), "masked text param must be flagged; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
    }

    /** Renamed - keyed on the text type and the store, not the name. */
    @Test
    fun renamedStoredTextParamStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity note { at: integer; mutable body: text; }
                operation save(idx: integer, payload: text) {
                    val account = auth.authenticate();
                    require(idx > 0, "positive");
                    update note @ { .at == idx } ( .body = payload );
                }
            """.trimIndent()
        )
        assertTrue("unvalidated-stored-parameter" in rules(result), result.findings.toString())
    }

    /** Storing through a mutating helper must not hide the store. */
    @Test
    fun textStoredViaHelperStillCaught() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity note { at: integer; body: text; }
                function save_note(b: text) { create note ( op_context.last_block_time, b ); }
                operation save(idx: integer, payload: text) {
                    val account = auth.authenticate();
                    require(idx > 0, "positive");
                    save_note(payload);
                }
            """.trimIndent()
        )
        assertTrue("unvalidated-stored-parameter" in rules(result), result.findings.toString())
    }

    /** Per-parameter validation via a helper counts for that parameter. */
    @Test
    fun helperValidatedTextParamStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                import lib.ft4.auth;
                entity ping_log { at: integer; y: text; }
                function validate_y(y: text) { require(y.size() <= 200, "too long"); }
                operation ping(x: integer, y: text) {
                    val account = auth.authenticate();
                    require(x > 0, "positive");
                    validate_y(y);
                    create ping_log ( x, y );
                }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "unvalidated-stored-parameter" }, result.findings.toString())
    }

    /** No parameter validated at all: not the masking shape - a different rule's job. */
    @Test
    fun noValidatedParamsIsNotThisRulesShape() {
        val result = analyze(
            "main.rell" to """
                module;
                entity post { key id: integer; mutable body: text; }
                function require_admin() {
                    require(op_context.is_signer(chain_context.args.admin_pubkey), "not admin");
                }
                operation edit_post(id: integer, body: text) {
                    require_admin();
                    update post @ { .id == id } ( .body = body );
                }
                struct module_args { admin_pubkey: pubkey; }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "unvalidated-stored-parameter" }, result.findings.toString())
    }

    // ================= query-returns-secret-data (B1) =================

    @Test
    fun querySelectingSecretEntityIsAdvisory() {
        val result = analyze(
            "main.rell" to """
                module;
                entity secret_note { owner: byte_array; content: text; }
                query all_notes() = secret_note @* {} ( .owner, .content );
            """.trimIndent()
        )
        val hit = result.findings.filter { it.rule == "query-returns-secret-data" }
        assertTrue(hit.isNotEmpty(), "secret-named data in a query must be advised; got ${result.findings}")
        assertEquals("MEDIUM", hit.first().severity)
        assertTrue(result.ok, "advisory must not block: ${result.findings}")
    }

    /** Block-bodied query reading a secret-named field. */
    @Test
    fun blockBodiedQueryWithSecretFieldIsAdvisory() {
        val result = analyze(
            "main.rell" to """
                module;
                entity user_row { key id: byte_array; password_hash: text; }
                query lookup(id: byte_array): text {
                    val u = user_row @ { .id == id };
                    return u.password_hash;
                }
            """.trimIndent()
        )
        assertTrue("query-returns-secret-data" in rules(result), result.findings.toString())
    }

    /**
     * A caller-supplied "owner" filter does NOT make it clean: queries have no
     * caller identity, so anyone passes any owner.
     */
    @Test
    fun ownerFilteredSecretQueryStillAdvised() {
        val result = analyze(
            "main.rell" to """
                module;
                entity secret_note { owner: byte_array; content: text; }
                query my_notes(owner: byte_array) = secret_note @* { .owner == owner } ( .content );
            """.trimIndent()
        )
        assertTrue("query-returns-secret-data" in rules(result), result.findings.toString())
    }

    /** Public data queries are the bread and butter of every dApp - clean. */
    @Test
    fun publicDataQueryStaysClean() {
        val result = analyze(
            "main.rell" to """
                module;
                entity wallet { key owner: byte_array; mutable balance: integer; }
                query get_balance(owner: byte_array) = wallet @? { .owner == owner } ( .balance );
                query top_holders() = wallet @* {} ( .owner, .balance );
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "query-returns-secret-data" }, result.findings.toString())
    }

    /** Queries in @test modules never ship - exempt. */
    @Test
    fun testModuleQueriesExempt() {
        val result = analyze(
            "tests/probe_test.rell" to """
                @test module;
                entity secret_fixture { key k: integer; secret_value: text; }
                query peek() = secret_fixture @* {} ( .secret_value );
                function test_x() { }
            """.trimIndent()
        )
        assertTrue(result.findings.none { it.rule == "query-returns-secret-data" }, result.findings.toString())
    }
}
