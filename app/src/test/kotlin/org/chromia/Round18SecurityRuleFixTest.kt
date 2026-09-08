package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.io.File
import java.time.Duration

/**
 * ROUND 18, the two rules that were evaded at the joins round 17 rebuilt -
 * pinned on the PROPERTY the fix is keyed on, never on the samples that found
 * it. The corpus rows pin the samples; this pins the contract, so a rewrite
 * that keeps those files caught while dropping the property still fails here.
 *
 *  (a) `majority-without-quorum`. Round 17 resolved a bound behind a call when
 *      the callee is "a named constant with parentheses - no parameters, a body
 *      that is a single return of a term the resolver can reduce". Round 18
 *      wrote SIX spellings of that same 1, each ONE SUBSTITUTION outside the
 *      shape and every one of them silent while `val FLOOR = 1;` fires: a
 *      DEFAULT-VALUED parameter, `2 - 1`, `min(1, 5)`, a NAMESPACED val, the
 *      stored floor read through a FUNCTION, and the same field read back
 *      through a QUERY. Adding six cases buys one round, so the value is
 *      EVALUATED now - a small constant evaluator over Rell expressions - and
 *      the who-writes-it trace expands the term through every callable it
 *      names, functions and queries alike. TEN MORE spellings are written here,
 *      before an adversary writes them, each beside its unqualified control and
 *      each beside the same DAO with a REAL floor, which must stay silent.
 *
 *  (b) `block-clock-randomness`. `winner_at(op_context.last_block_time)` hands
 *      the clock to a function whose body does `at % pot.staked` and whose
 *      RETURN is the winner. The modulo is visible once the helper is
 *      flattened, but nothing in the operation is derived from it - the account
 *      comes back through a `return` inside a loop - so the taint stopped at the
 *      call and a raffle anybody could time drew zero findings. The clock is
 *      followed through a callable's PARAMETERS now: a call that hands a
 *      clock-derived argument to a parameter the callee SELECTS with is itself
 *      a draw. Keyed on the data flow; the control hands the same function a
 *      revealed seed and must stay silent, because a rule that fires on a
 *      correct draw is worse than no rule.
 */
class Round18SecurityRuleFixTest {

    private fun rules(vararg files: Pair<String, String>): List<String> =
        RellSecurityCheck.analyze(files.toMap()).findings.map { it.rule }

    private fun corpusSample(id: String): Map<String, String> {
        val url = javaClass.classLoader.getResource("exploit-corpus/CORPUS.md")
            ?: error("exploit-corpus/CORPUS.md not found on the test classpath")
        val dir = File(File(url.toURI()).parentFile, "samples/$id")
        val files = dir.walkTopDown().filter { it.isFile && it.extension == "rell" }
            .associate { it.relativeTo(dir).invariantSeparatorsPath to it.readText() }
        require(files.isNotEmpty()) { "$id: no .rell files under $dir" }
        return files
    }

    private fun rulesOf(id: String): List<String> =
        RellSecurityCheck.analyze(corpusSample(id)).findings.map { it.rule }

    // =====================================================================
    // (a) the bound's VALUE
    // =====================================================================

    /**
     * Round 17's own DAO, byte for byte: a bare majority plus a participation
     * floor. [floorDecl] is whatever declares the floor and [floorTerm] is how
     * `settle_motion` names it, so the only difference between one spelling and
     * the next is those two.
     */
    private fun dao(floorDecl: String, floorTerm: String) = """
        module;

        entity roll { key owner: byte_array; mutable balance: integer = 0; mutable enrolled: integer = 0; }

        entity motion {
            key id: integer;
            beneficiary: byte_array;
            amount: integer;
            mutable yes_ballots: integer = 0;
            mutable no_ballots: integer = 0;
        }

        object book { mutable next_id: integer = 1; mutable pot: integer = 0; }

        $floorDecl

        operation propose(beneficiary: byte_array, amount: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require(amount > 0 and amount <= book.pot, "amount out of range");
            create motion(id = book.next_id, beneficiary = beneficiary, amount = amount);
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
            val r = require(roll @? { .owner == m.beneficiary }, "no such member");
            update r ( .balance += m.amount );
            book.pot -= m.amount;
        }
    """.trimIndent()

    private fun quorumFires(floorDecl: String, floorTerm: String): Boolean =
        "majority-without-quorum" in rules("main.rell" to dao(floorDecl, floorTerm))

    /**
     * One spelling of a bound, pinned in BOTH directions at once: the floor of
     * ONE must fire, and the SAME spelling carrying a real floor must not. A
     * pair like that cannot be satisfied by a rule that fires on everything or
     * on nothing.
     */
    private fun assertSpelling(label: String, oneDecl: String, oneTerm: String, realDecl: String, realTerm: String) {
        assertTrue(
            quorumFires(oneDecl, oneTerm),
            "$label: a floor of 1 spelled `$oneDecl` / `$oneTerm` is the same 1 that fires as " +
                "`val FLOOR = 1;`, and one ballot carries a treasury payout"
        )
        assertFalse(
            quorumFires(realDecl, realTerm),
            "$label: the SAME spelling carrying a real floor (`$realDecl` / `$realTerm`) is correct " +
                "code - a rule that fires here is firing on the spelling and not on the value"
        )
    }

    @Test
    fun `the unqualified val control fires, and a real floor written the same way does not`() {
        assertSpelling(
            "the control", "val FLOOR = 1;", "FLOOR", "val FLOOR = 25;", "FLOOR"
        )
    }

    // ---- the six round-18 spellings ----

    @Test
    fun `a floor behind a function with a default valued parameter is not a floor`() {
        assertSpelling(
            "default-valued parameter",
            "function participation_floor(cap: integer = 1): integer = cap;", "participation_floor()",
            "function participation_floor(cap: integer = 25): integer = cap;", "participation_floor()"
        )
    }

    @Test
    fun `a floor that is an arithmetic of constants is not a floor`() {
        assertSpelling(
            "arithmetic of constants",
            "function participation_floor(): integer = 2 - 1;", "participation_floor()",
            "function participation_floor(): integer = 20 + 5;", "participation_floor()"
        )
    }

    @Test
    fun `a floor that is a min of two literals is not a floor`() {
        assertSpelling(
            "min of two literals",
            "function participation_floor(): integer = min(1, 5);", "participation_floor()",
            "function participation_floor(): integer = min(25, 90);", "participation_floor()"
        )
    }

    @Test
    fun `a floor behind a namespaced val is not a floor`() {
        assertSpelling(
            "namespaced val",
            "namespace cfg { val FLOOR = 1; }", "cfg.FLOOR",
            "namespace cfg { val FLOOR = 25; }", "cfg.FLOOR"
        )
    }

    // ---- the ten this round writes before the adversary does ----

    @Test
    fun `eleven more spellings of the same number, each with its own control`() {
        assertSpelling(
            "a when expression whose every arm is the same number",
            "function participation_floor(): integer = when { book.pot > 0 -> 1; else -> 1 };",
            "participation_floor()",
            "function participation_floor(): integer = when { book.pot > 0 -> 25; else -> 25 };",
            "participation_floor()"
        )
        assertSpelling(
            "a struct field of a module-level constant",
            "struct limits { floor: integer; }\n        val LIMITS = limits(floor = 1);", "LIMITS.floor",
            "struct limits { floor: integer; }\n        val LIMITS = limits(floor = 25);", "LIMITS.floor"
        )
        assertSpelling(
            "a list index of literals",
            "function participation_floor(): integer = [1, 5, 9][0];", "participation_floor()",
            "function participation_floor(): integer = [25, 5, 9][0];", "participation_floor()"
        )
        assertSpelling(
            "abs of a negative literal",
            "function participation_floor(): integer = abs(-1);", "participation_floor()",
            "function participation_floor(): integer = abs(-25);", "participation_floor()"
        )
        assertSpelling(
            "the length of a text constant",
            "val FLOOR_TEXT = \"x\";", "FLOOR_TEXT.size()",
            "val FLOOR_TEXT = \"xxxxxxxxxxxxxxxxxxxxxxxxx\";", "FLOOR_TEXT.size()"
        )
        assertSpelling(
            "a chained default parameter",
            "function base_cap(cap: integer = 1): integer = cap;\n        " +
                "function participation_floor(cap: integer = base_cap()): integer = cap;",
            "participation_floor()",
            "function base_cap(cap: integer = 25): integer = cap;\n        " +
                "function participation_floor(cap: integer = base_cap()): integer = cap;",
            "participation_floor()"
        )
        assertSpelling(
            "a floor divided by itself",
            "function participation_floor(): integer = 5 / 5;", "participation_floor()",
            "function participation_floor(): integer = 125 / 5;", "participation_floor()"
        )
        assertSpelling(
            "a modulo of two constants",
            "function participation_floor(): integer = 7 % 3;", "participation_floor()",
            "function participation_floor(): integer = 77 % 26;", "participation_floor()"
        )
        assertSpelling(
            "a namespaced function",
            "namespace cfg { function floor(): integer = 1; }", "cfg.floor()",
            "namespace cfg { function floor(): integer = 25; }", "cfg.floor()"
        )
        assertSpelling(
            "a call with literal arguments",
            "function floor_of(n: integer): integer = n;", "floor_of(1)",
            "function floor_of(n: integer): integer = n;", "floor_of(25)"
        )
        assertSpelling(
            "a doubly negated literal",
            "function participation_floor(): integer = -(-1);", "participation_floor()",
            "function participation_floor(): integer = -(-25);", "participation_floor()"
        )
    }

    @Test
    fun `a floor this scan cannot value is still a floor`() {
        assertFalse(
            quorumFires("function participation_floor(): integer = roll @ { .balance > 0 } .enrolled;", "participation_floor()"),
            "a floor read out of state the proposer does not write is a floor the evaluator cannot " +
                "value, and resolving a state read to a guess is how a rule starts firing on correct code"
        )
        assertFalse(
            quorumFires(
                "function participation_floor(): integer { val n = roll @ { .balance > 0 } .enrolled; " +
                    "require(n >= 3, \"too few\"); return n; }",
                "participation_floor()"
            ),
            "a body that is more than one statement is not an expression this evaluator reads, and an " +
                "unreadable bound keeps the benefit of the doubt"
        )
        assertFalse(
            quorumFires("function participation_floor(n: integer): integer = n;", "participation_floor(m.yes_ballots)"),
            "a parameter the call site fills from state is not a number this scan can see"
        )
    }

    @Test
    fun `a cycle of constant returns terminates and resolves nothing`() {
        val cyclic = dao(
            "function participation_floor(): integer = other_floor();\n        " +
                "function other_floor(): integer = participation_floor();",
            "participation_floor()"
        )
        val found: List<String> = assertTimeoutPreemptively(Duration.ofSeconds(20)) {
            rules("main.rell" to cyclic)
        }
        assertFalse(
            "majority-without-quorum" in found,
            "a cycle resolves to nothing, and a bound that resolves to nothing is still a bound"
        )
    }

    @Test
    fun `the round 17 shapes have not moved`() {
        assertTrue(
            quorumFires("function participation_floor(): integer = 1;", "participation_floor()"),
            "round 17's own evasion must stay caught"
        )
        assertFalse(
            quorumFires("function participation_floor(): integer = 25;", "participation_floor()"),
            "round 17's own control must stay silent"
        )
        assertTrue(
            quorumFires(
                "function base_floor(): integer = 1;\n        " +
                    "function participation_floor(): integer = base_floor();",
                "participation_floor()"
            ),
            "`f() = g(); g() = 1;` hides the same 1 one hop further out"
        )
    }

    // ---- the stored floor, read through a function and through a QUERY ----

    /**
     * The round-17 stored-floor DAO with round 18's two reads. [floorGuard] is
     * the writing operation's authorisation - the whole of the difference
     * between the attack and its control - and [floorRead] is how the settle
     * reaches `book.floor`.
     */
    private fun storedFloorDao(floorGuard: String, floorRead: String) = """
        module;

        struct module_args { admin_pubkey: byte_array; }

        entity roll { key owner: byte_array; mutable balance: integer = 0; }

        entity motion {
            key id: integer;
            beneficiary: byte_array;
            amount: integer;
            mutable yes_ballots: integer = 0;
            mutable no_ballots: integer = 0;
        }

        object book { mutable next_id: integer = 1; mutable pot: integer = 0; mutable floor: integer = 3; }

        $floorRead

        operation set_floor(n: integer) {
            $floorGuard
            require(n > 0, "the floor must be positive");
            book.floor = n;
        }

        operation propose(beneficiary: byte_array, amount: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require(amount > 0 and amount <= book.pot, "amount out of range");
            create motion(id = book.next_id, beneficiary = beneficiary, amount = amount);
            book.next_id += 1;
        }

        operation ballot(id: integer, yes: boolean) {
            val m = require(motion @? { .id == id }, "no such motion");
            if (yes) update m ( .yes_ballots += 1 ); else update m ( .no_ballots += 1 );
        }

        operation settle_motion(id: integer) {
            val m = require(motion @? { .id == id }, "no such motion");
            require(m.yes_ballots + m.no_ballots >= participation_floor(), "not enough ballots");
            require(m.yes_ballots > m.no_ballots, "the motion did not carry");
            val r = require(roll @? { .owner == m.beneficiary }, "no such member");
            update r ( .balance += m.amount );
            book.pot -= m.amount;
        }
    """.trimIndent()

    private val throughAFunction = "function participation_floor(): integer = book.floor;"
    private val throughAQuery =
        "query floor_of(): integer = book.floor;\n        function participation_floor(): integer = floor_of();"
    private val firstSignerGuard = """require(op_context.is_signer(op_context.get_signers()[0]), "sign it");"""
    private val adminGuard = """require(op_context.is_signer(chain_context.args.admin_pubkey), "admin only");"""

    @Test
    fun `a stored floor any signer can set is not a floor, read through a function or through a query`() {
        assertTrue(
            "majority-without-quorum" in rules("main.rell" to storedFloorDao(firstSignerGuard, throughAFunction)),
            "round 17 called is_signer(get_signers()[0]) no principal at all; putting the READ behind a " +
                "function must not buy back the silence, because the floor is whatever the proposer set " +
                "one block earlier"
        )
        assertTrue(
            "majority-without-quorum" in rules("main.rell" to storedFloorDao(firstSignerGuard, throughAQuery)),
            "the same field one hop further out - a query - is the same field: a data flow that follows " +
                "a function and not a query is keyed on which keyword the author typed"
        )
    }

    @Test
    fun `a stored floor only a module args admin can set is a real floor, through either read`() {
        listOf(throughAFunction to "a function", throughAQuery to "a query").forEach { (read, how) ->
            assertFalse(
                "majority-without-quorum" in rules("main.rell" to storedFloorDao(adminGuard, read)),
                "a floor the proposer cannot move is a floor whether it is read through $how - a rule " +
                    "that flags this is flagging correct code"
            )
        }
    }

    // ---- the samples, and the round-17 open question ----

    @Test
    fun `the round 18 quorum samples are caught and their control has not moved`() {
        listOf(
            "r18-quorum-floor-behind-a-function-with-a-default-parameter",
            "r18-quorum-floor-as-an-arithmetic-of-constants",
            "r18-quorum-floor-as-a-min-of-two-literals",
            "r18-quorum-floor-behind-a-namespaced-val",
            "r18-quorum-floor-writer-guarded-only-by-the-first-signer",
            "r18-quorum-floor-read-back-through-a-query",
            "r18-quorum-floor-control-the-same-val-unqualified"
        ).forEach { id ->
            assertTrue(
                "majority-without-quorum" in rulesOf(id),
                "$id must draw the rule its CORPUS row names; got ${rulesOf(id)}"
            )
        }
    }

    @Test
    fun `the deployment floor DAO is still clean, and so are the round 14 and 16 false positive samples`() {
        listOf(
            "r18-dao-deployment-floor-isolated-clean",
            "r14-fp-turnout-floor-without-the-word-quorum",
            "clean-quorum-gated-dao"
        ).forEach { id ->
            val findings = RellSecurityCheck.analyze(corpusSample(id)).findings
            assertTrue(
                findings.isEmpty(),
                "$id is correct code and must draw nothing; got " +
                    findings.joinToString("; ") { "${it.severity} ${it.rule} - ${it.text}" }
            )
        }
    }

    // =====================================================================
    // (b) the block clock through a callable's parameter
    // =====================================================================

    /**
     * The round-18 raffle, reduced to the draw. [pick] is how `draw` chooses the
     * winner - the whole of the difference between the drain and its control -
     * and every other guard the fixture carries is the one the un-templated
     * build already had right.
     */
    private fun raffle(pick: String) = """
        module;

        entity depositor { key account: byte_array; mutable amount: integer; }

        object pot {
            mutable prize: integer = 0;
            mutable staked: integer = 0;
            mutable last_draw: timestamp = 0;
            mutable seed: integer = 0;
        }

        val WEEK = 604800000;

        function winner_at(at: integer): byte_array {
            require(pot.staked > 0, "nobody has staked");
            val ticket = at % pot.staked;
            var seen = 0;
            for (d in depositor @* {} ( @sort account = .account, amount = .amount )) {
                seen += d.amount;
                if (ticket < seen) return d.account;
            }
            return (depositor @* {} ( @sort .account ))[0];
        }

        operation reveal(secret: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require(secret > 0, "no secret");
            pot.seed = secret;
        }

        operation draw(caller: byte_array) {
            require(op_context.is_signer(caller), "sign it yourself");
            require(op_context.last_block_time >= pot.last_draw + WEEK, "the week is not up");
            require(pot.prize > 0, "nothing to pay");
            $pick
            require(winner == caller, "you are not this week's winner");
            val d = require(depositor @? { .account == winner }, "no such depositor");
            update d ( .amount += pot.prize );
            pot.prize = 0;
            pot.last_draw = op_context.last_block_time;
        }
    """.trimIndent()

    @Test
    fun `a draw whose selector is inside the callee is still a draw from the block clock`() {
        assertTrue(
            "block-clock-randomness" in rules("main.rell" to raffle("val winner = winner_at(op_context.last_block_time);")),
            "the clock reaches the modulo through a PARAMETER and the winner comes back through a " +
                "return - the selector being one call away does not make the block any less public, " +
                "and the loser who waits for a favourable block takes the pot"
        )
    }

    @Test
    fun `the same function fed a revealed seed is not a block clock draw`() {
        assertFalse(
            "block-clock-randomness" in rules("main.rell" to raffle("val winner = winner_at(pot.seed);")),
            "the same helper, the same modulo, the same payout - fed a value the block clock never " +
                "reached. A rule that fires here is firing on the shape of the draw rather than on " +
                "where its entropy came from, and firing on a correct draw is worse than not firing"
        )
    }

    @Test
    fun `the clock as a bound in the same module is not a draw`() {
        val bounded = raffle("val winner = caller;")
        assertFalse(
            "block-clock-randomness" in rules("main.rell" to bounded),
            "`require(op_context.last_block_time >= pot.last_draw + WEEK)` is a BOUND - an inequality " +
                "that can only abort - and `winner_at` sitting unused in the same file changes nothing"
        )
    }

    @Test
    fun `the round 18 raffle sample is caught`() {
        assertTrue(
            "block-clock-randomness" in rulesOf("r18-raffle-a-draw-the-caller-chooses-the-block-for"),
            "r18-raffle-a-draw-the-caller-chooses-the-block-for is a raffle drained on a real chain - " +
                "300 points moved, trudy winning 5 of 5 - and every drained draw in it is a legal draw, " +
                "so the finding has to come from the entropy and not from the operation"
        )
    }
}
