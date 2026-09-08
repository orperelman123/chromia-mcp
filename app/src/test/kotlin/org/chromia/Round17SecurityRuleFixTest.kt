package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.io.File
import java.time.Duration

/**
 * ROUND 17, the two rule EVASIONS at the joins round 16 rebuilt - each pinned
 * on the structural property the fix is keyed on rather than on the sample that
 * found it. The corpus rows pin the samples; this pins the contract, so a later
 * rewrite that happens to keep those two files caught while dropping the
 * property still fails here.
 *
 *  (a) ROUND 16 RESOLVES A BOUND'S VALUE through a literal, a module-level
 *      `val` and a `struct module_args` default - and has no case for a CALL.
 *      `function participation_floor(): integer = 1;` is the same 1 that fires
 *      one token away as `val PARTICIPATION_FLOOR = 1;`. A parameterless
 *      function whose body is a single return of a reducible term is a named
 *      constant with parentheses and resolves like one; a function that reads
 *      state stays unresolved and keeps the benefit of the doubt, because
 *      guessing there is how a rule starts firing on correct code. The walk is
 *      depth-bounded and cycle-safe. (ROUND 18 went round this six ways and
 *      replaced the shape with an EVALUATOR; the one pin below that moved is a
 *      call with a LITERAL argument, which is now worth that literal.)
 *  (b) ROUND 16 REJECTS A FLOOR THE PROPOSER PASSES to `propose`. A floor
 *      STORED in a field that a permissionless operation writes from ITS
 *      caller's arguments is the same floor one block earlier - `set_floor(1)`
 *      then `propose()` - so the closure runs one hop further: a field is
 *      proposer-controlled when an operation whose auth names NO principal
 *      writes it from its own parameters, or from another field already in the
 *      set. The data-flow condition is what keeps `roll.enrolled += 1` (a
 *      permissionless registration an attacker can only push UP) a real floor.
 */
class Round17SecurityRuleFixTest {

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
    // (a) a bound behind a call
    // =====================================================================

    /**
     * A DAO gated on a bare majority plus a participation floor. [floorDecl] is
     * whatever declares the floor and [floorTerm] is how `settle_motion` names
     * it, so the only difference between an evasion and its control is the two
     * of them.
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

    @Test
    fun `a floor of one behind a function call is not a floor, and the val control agrees`() {
        assertTrue(
            "majority-without-quorum" in
                rules("main.rell" to dao("function participation_floor(): integer = 1;", "participation_floor()")),
            "the round-17 evasion: the same 1 that fires as a val must fire behind `f()`"
        )
        assertTrue(
            "majority-without-quorum" in
                rules("main.rell" to dao("val PARTICIPATION_FLOOR = 1;", "PARTICIPATION_FLOOR")),
            "the control must stay caught, or the pair proves nothing about the rule"
        )
    }

    @Test
    fun `a function returning a real minimum is a real floor`() {
        assertFalse(
            "majority-without-quorum" in
                rules("main.rell" to dao("function participation_floor(): integer = 25;", "participation_floor()")),
            "25 ballots is a floor whether it is written inline or returned - a rule that fires here " +
                "is firing on correct code"
        )
    }

    @Test
    fun `a function that reads state is not resolved and keeps the benefit of the doubt`() {
        assertFalse(
            "majority-without-quorum" in rules(
                "main.rell" to dao(
                    "function participation_floor(): integer = roll @ { .balance > 0 } .enrolled;",
                    "participation_floor()"
                )
            ),
            "a floor this scan cannot value is still a floor - resolving a state read to a guess is " +
                "how a rule starts firing on correct code"
        )
    }

    /**
     * ROUND 18 SUPERSEDED THIS PIN, IN THE STRICTER DIRECTION. Round 17 left a
     * call with ARGUMENTS unresolved because "a function with parameters
     * returns what the CALL SITE passed, so the declaration is not its value" -
     * true of the DECLARATION, and beside the point at a call site that passes
     * a LITERAL: `participation_floor(1)` is the number 1, and one ballot
     * carries a treasury payout. Round 18's evaluator substitutes the argument.
     * The half of round 17's claim that is still true is pinned beside it: a
     * parameter the call site fills from STATE is a number this scan cannot
     * see, and it keeps the benefit of the doubt.
     */
    @Test
    fun `a function called with a literal argument is worth that literal, and one filled from state is not`() {
        assertTrue(
            "majority-without-quorum" in rules(
                "main.rell" to dao(
                    "function participation_floor(n: integer): integer = n;",
                    "participation_floor(1)"
                )
            ),
            "`participation_floor(1)` is the same 1 that fires as `val PARTICIPATION_FLOOR = 1;` - the " +
                "parameter is bound to a literal AT THE CALL SITE, which is where a bound is read"
        )
        assertFalse(
            "majority-without-quorum" in rules(
                "main.rell" to dao(
                    "function participation_floor(n: integer): integer = n;",
                    "participation_floor(25)"
                )
            ),
            "the same call carrying a real floor is correct code and must stay silent"
        )
        assertFalse(
            "majority-without-quorum" in rules(
                "main.rell" to dao(
                    "function participation_floor(n: integer): integer = n;",
                    "participation_floor(roll @ { .balance > 0 } .enrolled)"
                )
            ),
            "a parameter the call site fills from STATE is a number this scan cannot see, and resolving " +
                "it to a guess is how a rule starts firing on correct code"
        )
    }

    @Test
    fun `a two deep chain of constant returns resolves`() {
        assertTrue(
            "majority-without-quorum" in rules(
                "main.rell" to dao(
                    "function base_floor(): integer = 1;\n        " +
                        "function participation_floor(): integer = base_floor();",
                    "participation_floor()"
                )
            ),
            "`f() = g(); g() = 1;` hides the same 1 one hop further out"
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

    // =====================================================================
    // (a) the same resolver on the OTHER rule that reads a bound's value
    // =====================================================================

    private fun window(floorDecl: String, bound: String) = """
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
    fun `a voting floor of zero behind a function call is not a floor`() {
        assertTrue(
            "unbounded-voting-period" in rules(
                "main.rell" to window(
                    "function min_voting_ms(): integer = 0;",
                    "voting_period_ms >= min_voting_ms()"
                )
            ),
            "round 16 made a floor of 0 no floor; putting the same 0 behind a call must not buy it back"
        )
        assertFalse(
            "unbounded-voting-period" in rules(
                "main.rell" to window(
                    "function min_voting_ms(): integer = 3600000;",
                    "voting_period_ms >= min_voting_ms()"
                )
            ),
            "an hour is a real minimum however it is spelled"
        )
    }

    // =====================================================================
    // (b) a floor written by a second operation
    // =====================================================================

    /**
     * The round-17 shape: the floor is a stored `book.floor`, stamped into the
     * motion by `propose`, and written by a SECOND operation. [floorGuard] is
     * that second operation's authorisation - the whole of the difference
     * between the attack and its control.
     */
    private fun storedFloorDao(floorGuard: String) = """
        module;

        struct module_args { admin_pubkey: byte_array; }

        entity roll { key owner: byte_array; mutable balance: integer = 0; }

        entity motion {
            key id: integer;
            beneficiary: byte_array;
            amount: integer;
            floor_at_creation: integer;
            mutable yes_ballots: integer = 0;
            mutable no_ballots: integer = 0;
        }

        object book { mutable next_id: integer = 1; mutable pot: integer = 0; mutable floor: integer = 3; }

        operation set_floor(n: integer) {
            $floorGuard
            require(n > 0, "the floor must be positive");
            book.floor = n;
        }

        operation propose(beneficiary: byte_array, amount: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require(amount > 0 and amount <= book.pot, "amount out of range");
            create motion(
                id = book.next_id, beneficiary = beneficiary, amount = amount,
                floor_at_creation = book.floor
            );
            book.next_id += 1;
        }

        operation ballot(id: integer, yes: boolean) {
            val m = require(motion @? { .id == id }, "no such motion");
            if (yes) update m ( .yes_ballots += 1 ); else update m ( .no_ballots += 1 );
        }

        operation settle_motion(id: integer) {
            val m = require(motion @? { .id == id }, "no such motion");
            require(m.yes_ballots + m.no_ballots >= m.floor_at_creation, "not enough ballots");
            require(m.yes_ballots > m.no_ballots, "the motion did not carry");
            val r = require(roll @? { .owner == m.beneficiary }, "no such member");
            update r ( .balance += m.amount );
            book.pot -= m.amount;
        }
    """.trimIndent()

    @Test
    fun `a floor any signer can set is not a floor`() {
        assertTrue(
            "majority-without-quorum" in rules(
                "main.rell" to storedFloorDao("""require(op_context.is_signer(op_context.get_signers()[0]), "sign it");""")
            ),
            "set_floor(1) then propose() is the round-16 drain in two transactions: the writer's only " +
                "gate is that SOMEBODY signed, which every transaction satisfies"
        )
    }

    @Test
    fun `a floor only a module args admin can set is a real floor`() {
        assertFalse(
            "majority-without-quorum" in rules(
                "main.rell" to storedFloorDao(
                    """require(op_context.is_signer(chain_context.args.admin_pubkey), "admins only");"""
                )
            ),
            "a floor the proposer cannot move is a floor - a rule that flags this is flagging correct code"
        )
    }

    @Test
    fun `a floor only a stored owner can set is a real floor`() {
        val ownerGuarded = storedFloorDao(
            """val cfg = require(roll @? { .balance > 0 }, "no config");
            require(op_context.is_signer(cfg.owner), "owner only");"""
        )
        assertFalse(
            "majority-without-quorum" in rules("main.rell" to ownerGuarded),
            "is_signer against a STORED key is a principal the caller does not choose"
        )
    }

    @Test
    fun `an unauthenticated writer is not a principal either`() {
        assertTrue(
            "majority-without-quorum" in rules("main.rell" to storedFloorDao("")),
            "no gate at all on the writer is the same floor with one fewer line"
        )
    }

    // =====================================================================
    // (b) the samples, and the round-16 shape that must not move
    // =====================================================================

    @Test
    fun `the round 17 samples are caught and the round 16 ones have not moved`() {
        assertTrue(
            "majority-without-quorum" in rulesOf("r17-quorum-floor-behind-a-function-call"),
            "r17-quorum-floor-behind-a-function-call must draw the rule its CORPUS row names"
        )
        assertTrue(
            "majority-without-quorum" in rulesOf("r17-quorum-floor-control-the-same-one-as-a-val"),
            "its control must stay caught"
        )
        assertTrue(
            "majority-without-quorum" in rulesOf("r17-quorum-floor-set-by-a-second-operation"),
            "r17-quorum-floor-set-by-a-second-operation must draw the rule its CORPUS row names"
        )
        assertTrue(
            "majority-without-quorum" in rulesOf("r16-quorum-floor-written-by-the-proposer"),
            "a floor the proposer PASSES was round 16's finding and must still be one"
        )
        assertTrue(
            "majority-without-quorum" in rulesOf("r16-quorum-floor-control-a-literal-two"),
            "round 16's literal-two control must stay caught"
        )
    }

    /**
     * THE FALSE-POSITIVE SIDE, which is the whole reason the closure needs a
     * data-flow condition rather than "any field a permissionless operation
     * writes". `r14-fp-turnout-floor-without-the-word-quorum` stamps its motion
     * from `turnout_floor(roll.enrolled)` and `roll.enrolled` is incremented by
     * a permissionless `enrol()` - but by `+= 1`, so no argument of the
     * attacker's reaches it and they can only push the bar UP.
     * `clean-quorum-gated-dao` reads its floor straight out of module args.
     * Both must stay silent.
     */
    @Test
    fun `a floor derived from state the proposer cannot choose stays a floor`() {
        listOf("r14-fp-turnout-floor-without-the-word-quorum", "clean-quorum-gated-dao").forEach { id ->
            val findings = RellSecurityCheck.analyze(corpusSample(id)).findings
            assertTrue(
                findings.isEmpty(),
                "$id is correct code and must draw nothing; got " +
                    findings.joinToString("; ") { "${it.severity} ${it.rule} - ${it.text}" }
            )
        }
    }
}
