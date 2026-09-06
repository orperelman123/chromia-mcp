package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ROUND 16, the three rule EVASIONS and the one plain GAP - each pinned on the
 * structural signal the fix is keyed on rather than on the sample that found
 * it. The corpus rows pin the samples; this pins the contract, so a later
 * rewrite that happens to keep those four files caught but drops the property
 * still fails here.
 *
 *  (a) `unbounded-voting-period` read the lower bound's TERM and never its
 *      VALUE, so `val MIN_VOTING_MS = 0;` was a floor. A floor is a POSITIVE
 *      QUANTITY: the bound is resolved through literals, module-level `val`s
 *      and `struct module_args` defaults, and a bound worth 0 - or a module
 *      arg with no default at all - is not a bound.
 *  (b) `majority-without-quorum` accepted any term it could not resolve to a
 *      literal as "relative", and the most obvious unresolvable term is a row
 *      field THE PROPOSER WRITES. Relative/absolute was never the distinction;
 *      who writes the term is.
 *  (c) `query-returns-secret-data` read the return expression with local
 *      `val`s substituted, so a secret accumulated into `list<text>()` in a
 *      `for` loop returned a type constructor that names no entity. What is
 *      put into the thing that is returned is returned.
 *  (d) `unbacked-conversion-credit` wanted a `*` or a `/` in the credited
 *      amount, so the round-4 mint with the rate dropped - `balance += now -
 *      last_claim` - was clean in both declared types. An elapsed term is
 *      itself a quantity.
 */
class Round16SecurityRuleFixTest {

    private fun rules(vararg files: Pair<String, String>): List<String> =
        RellSecurityCheck.analyze(files.toMap()).findings.map { it.rule }

    private fun findings(vararg files: Pair<String, String>) =
        RellSecurityCheck.analyze(files.toMap()).findings

    // =====================================================================
    // (a) a lower bound is worth what its VALUE is worth
    // =====================================================================

    private fun dao(floorDecl: String, bound: String) = """
        module;
        $floorDecl
        entity motion { key id: integer; deadline: timestamp; }
        object book { mutable next_id: integer = 1; }
        operation open_motion(voting_period_ms: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require($bound, "the voting period is too short");
            create motion(id = book.next_id, deadline = op_context.last_block_time + voting_period_ms);
            book.next_id += 1;
        }
    """.trimIndent()

    @Test
    fun `a named constant that is zero is not a floor`() {
        assertTrue(
            "unbounded-voting-period" in
                rules("main.rell" to dao("val MIN_VOTING_MS = 0;", "voting_period_ms >= MIN_VOTING_MS")),
            "a floor whose VALUE is 0 is not a floor, however it is spelled"
        )
    }

    @Test
    fun `the same constant with a positive value is a floor`() {
        assertFalse(
            "unbounded-voting-period" in
                rules("main.rell" to dao("val MIN_VOTING_MS = 3600000;", "voting_period_ms >= MIN_VOTING_MS")),
            "a real minimum must silence the advisory - a rule that fires on correct code is a defect"
        )
    }

    @Test
    fun `an inline zero is still not a floor`() {
        assertTrue(
            "unbounded-voting-period" in rules("main.rell" to dao("", "voting_period_ms >= 0")),
            "the round-15 behaviour on a literal 0 must not regress"
        )
    }

    /**
     * `max(param, MIN)` is a floor spelled as arithmetic, and the rule has
     * always accepted it. It is worth exactly what MIN is worth, so it is
     * resolved on the same terms as a comparison's - the round-16 zero cannot
     * buy silence by moving into a max().
     *
     * The window is stored UNCLAMPED here on purpose: `now + max(param, MIN)`
     * is not a window at all under the round-14 tightening (the duration is
     * what the call returns, not the argument it was passed), so a sample that
     * clamps inline would test nothing about this branch.
     */
    private fun daoWithMaxFloor(floor: String) = """
        module;
        entity motion { key id: integer; deadline: timestamp; }
        object book { mutable next_id: integer = 1; }
        operation open_motion(voting_period_ms: integer) {
            require(voting_period_ms == max(voting_period_ms, $floor), "the voting period is too short");
            create motion(id = book.next_id, deadline = op_context.last_block_time + voting_period_ms);
            book.next_id += 1;
        }
    """.trimIndent()

    @Test
    fun `max against a zero is not a floor and max against a real minimum is`() {
        assertTrue(
            "unbounded-voting-period" in rules("main.rell" to daoWithMaxFloor("0")),
            "max(param, 0) clamps nothing - a period of 0 still passes"
        )
        assertFalse(
            "unbounded-voting-period" in rules("main.rell" to daoWithMaxFloor("3600000")),
            "max(param, 3600000) is a real floor spelled as arithmetic and must stay silent"
        )
    }

    @Test
    fun `a module arg with no default is reported as one to give a default`() {
        val src = dao(
            "struct module_args { min_voting_ms: integer; }",
            "voting_period_ms >= chain_context.args.min_voting_ms"
        )
        val f = findings("main.rell" to src).filter { it.rule == "unbounded-voting-period" }
        assertTrue(f.isNotEmpty(), "a bound that lives in configuration the chain may not have is not a bound")
        assertTrue(
            f.first().text.contains("min_voting_ms") && f.first().text.contains("no default"),
            "the finding must name the module arg and say the default is missing: ${f.first().text}"
        )
        assertTrue(
            f.first().fix.contains("default"),
            "the fix must be 'set a default > 0', not 'add a require()' the author already wrote: ${f.first().fix}"
        )
    }

    @Test
    fun `a module arg with a positive default is a floor`() {
        assertFalse(
            "unbounded-voting-period" in rules(
                "main.rell" to dao(
                    "struct module_args { min_voting_ms: integer = 86400000; }",
                    "voting_period_ms >= chain_context.args.min_voting_ms"
                )
            ),
            "a configured floor a chain cannot be deployed without IS a floor"
        )
    }

    @Test
    fun `a module arg whose default is zero is still not a floor`() {
        assertTrue(
            "unbounded-voting-period" in rules(
                "main.rell" to dao(
                    "struct module_args { min_voting_ms: integer = 0; }",
                    "voting_period_ms >= chain_context.args.min_voting_ms"
                )
            )
        )
    }

    // =====================================================================
    // (b) a floor the proposer writes is the proposer's floor
    // =====================================================================

    private fun governance(motionFields: String, createArgs: String, floorTerm: String) = """
        module;
        entity roll { key owner: byte_array; mutable balance: integer = 0; }
        entity register { key id: integer; mutable members: integer = 0; }
        entity motion {
            key id: integer;
            beneficiary: byte_array;
            amount: integer;
            $motionFields
            mutable yes_ballots: integer = 0;
            mutable no_ballots: integer = 0;
        }
        object book { mutable next_id: integer = 1; mutable pot: integer = 0; }
        operation propose(beneficiary: byte_array, amount: integer, bar: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require(amount > 0 and amount <= book.pot, "amount out of range");
            val r = register @ { .id == 1 };
            create motion(id = book.next_id, beneficiary = beneficiary, amount = amount$createArgs);
            book.next_id += 1;
        }
        operation ballot(id: integer, yes: boolean) {
            val m = require(motion @? { .id == id }, "no such motion");
            if (yes) update m ( .yes_ballots += 1 ); else update m ( .no_ballots += 1 );
        }
        operation settle_motion(id: integer) {
            val m = require(motion @? { .id == id }, "no such motion");
            require(m.yes_ballots + m.no_ballots >= $floorTerm, "not enough ballots");
            require(m.yes_ballots > m.no_ballots, "the motion did not carry");
            val t = require(roll @? { .owner == m.beneficiary }, "no such member");
            update t ( .balance += m.amount );
            book.pot -= m.amount;
        }
    """.trimIndent()

    @Test
    fun `a floor the proposer writes is not a participation floor`() {
        val f = findings(
            "main.rell" to governance(
                "floor_at_creation: integer;", ", floor_at_creation = bar", "m.floor_at_creation"
            )
        ).filter { it.rule == "majority-without-quorum" }
        assertTrue(f.isNotEmpty(), "propose with a bar of 1 and vote 1-0 on your own motion")
        assertTrue(
            f.first().text.contains("PROPOSER"),
            "the finding must say the floor is the proposer's, not 'no floor at all': ${f.first().text}"
        )
    }

    @Test
    fun `the same floor derived from the register is a participation floor`() {
        assertFalse(
            "majority-without-quorum" in rules(
                "main.rell" to governance(
                    "floor_at_creation: integer;", ", floor_at_creation = r.members / 2", "m.floor_at_creation"
                )
            ),
            "a floor read off state the proposer does not write in the same operation is a real floor"
        )
    }

    @Test
    fun `reading the proposer's floor through a local does not restore it`() {
        assertTrue(
            "majority-without-quorum" in rules(
                "main.rell" to governance(
                    "floor_at_creation: integer;", ", floor_at_creation = bar", "m.floor_at_creation"
                ).replace(
                    "require(m.yes_ballots + m.no_ballots >= m.floor_at_creation, \"not enough ballots\");",
                    "val f = m.floor_at_creation;\n            require(m.yes_ballots + m.no_ballots >= f, \"not enough ballots\");"
                )
            ),
            "one local binding must not launder an attacker-chosen bar into a floor"
        )
    }

    // =====================================================================
    // (c) what is put into what is returned, is returned
    // =====================================================================

    private val secretEntity = """
        entity credential {
            key owner: byte_array;
            mutable label: text = "";
            mutable secret_token: text = "";
        }
        operation store(token: text) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            create credential(owner = op_context.get_signers()[0], secret_token = token);
        }
    """.trimIndent()

    @Test
    fun `a secret accumulated into a list in a loop is returned`() {
        assertTrue(
            "query-returns-secret-data" in rules(
                "main.rell" to """
                    module;
                    $secretEntity
                    query all_tokens(): list<text> {
                        val out = list<text>();
                        for (c in credential @* {}) {
                            out.add(c.secret_token);
                        }
                        return out;
                    }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `a secret accumulated into a map is returned too`() {
        assertTrue(
            "query-returns-secret-data" in rules(
                "main.rell" to """
                    module;
                    $secretEntity
                    query token_book(): map<byte_array, text> {
                        val out = map<byte_array, text>();
                        for (c in credential @* {}) {
                            out.put(c.owner, c.secret_token);
                        }
                        return out;
                    }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `a conservation query that sums balances in a loop stays clean`() {
        assertTrue(
            RellSecurityCheck.analyze(
                mapOf(
                    "main.rell" to """
                        module;
                        entity account { key owner: byte_array; mutable balance: integer = 0; }
                        query total(): integer {
                            var t = 0;
                            for (a in account @* {}) {
                                t += a.balance;
                            }
                            return t;
                        }
                    """.trimIndent()
                )
            ).findings.none { it.rule == "query-returns-secret-data" },
            "accumulating in a loop is idiomatic Rell - the shipped templates do it in their conservation queries"
        )
    }

    // =====================================================================
    // (d) an elapsed term is itself a quantity
    // =====================================================================

    private fun faucet(declaredType: String, spendable: Boolean = true): String {
        val spend = if (spendable) {
            """
            operation spend(to: byte_array, amount: integer) {
                val me = require(subscriber @? { .owner == op_context.get_signers()[0] }, "register first");
                val m = require(merchant @? { .owner == to }, "no merchant");
                require(amount > 0, "amount out of range");
                require(me.credit_balance >= amount, "insufficient credit");
                update me ( .credit_balance -= amount );
                update m ( .takings += amount );
            }
            """.trimIndent()
        } else {
            // The identical field, never taken FROM: a stored clock rather than
            // a balance, which is what the round-15 exclusion was written for.
            """
            query credit_of(owner: byte_array): integer {
                val me = require(subscriber @? { .owner == owner }, "register first");
                return me.credit_balance;
            }
            """.trimIndent()
        }
        return """
            module;
            entity subscriber {
                key owner: byte_array;
                mutable credit_balance: $declaredType = 0;
                mutable last_claim: timestamp = 0;
            }
            entity merchant { key owner: byte_array; mutable takings: integer = 0; }
            operation register() {
                require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
                create subscriber(owner = op_context.get_signers()[0], last_claim = op_context.last_block_time);
            }
            operation claim_credit() {
                val me = require(subscriber @? { .owner == op_context.get_signers()[0] }, "register first");
                update me ( .credit_balance += op_context.last_block_time - me.last_claim );
                update me ( .last_claim = op_context.last_block_time );
            }
            $spend
        """.trimIndent()
    }

    @Test
    fun `a clock difference credited to a spendable balance is a mint in either declared type`() {
        listOf("integer", "timestamp").forEach { type ->
            val f = findings("main.rell" to faucet(type)).filter { it.rule == "unbacked-conversion-credit" }
            assertTrue(f.isNotEmpty(), "declared $type: one unit of spendable credit per millisecond of wall clock")
            assertTrue(
                f.first().text.contains("per millisecond"),
                "declared $type: the finding must describe the shape it found: ${f.first().text}"
            )
        }
    }

    @Test
    fun `a deadline pushed forward by a purchased duration is not a mint`() {
        assertTrue(
            RellSecurityCheck.analyze(
                mapOf(
                    "main.rell" to """
                        module;
                        entity plan {
                            key payer: byte_array;
                            mutable funded_until: timestamp = 0;
                            mutable escrow: integer = 0;
                        }
                        function ms_bought(funding: integer, period_ms: integer, fee: integer): integer =
                            funding * period_ms / fee;
                        operation top_up(funding: integer, period_ms: integer, fee: integer) {
                            val s = require(plan @? { .payer == op_context.get_signers()[0] }, "no plan");
                            val bought = ms_bought(funding, period_ms, fee);
                            val now = op_context.last_block_time;
                            if (now >= s.funded_until) {
                                update s ( .funded_until = now + bought, .escrow = s.escrow + funding );
                            } else {
                                update s ( .funded_until = s.funded_until + bought, .escrow = s.escrow + funding );
                            }
                        }
                    """.trimIndent()
                )
            ).findings.none { it.rule == "unbacked-conversion-credit" },
            "a declared timestamp that is never DEBITED anywhere is a deadline, and crediting a deadline mints nothing"
        )
    }

    @Test
    fun `the timestamp exclusion is withdrawn only from fields the submission debits`() {
        // The same clock arithmetic, the same declared type, one difference:
        // this field is spent. That is the whole of the round-16 narrowing.
        assertTrue(
            findings("main.rell" to faucet("timestamp", spendable = true))
                .any { it.rule == "unbacked-conversion-credit" }
        )
        assertTrue(
            findings("main.rell" to faucet("timestamp", spendable = false))
                .none { it.rule == "unbacked-conversion-credit" },
            "with no debit of the field anywhere it is a stored clock, and the round-15 exclusion still holds"
        )
    }
}
