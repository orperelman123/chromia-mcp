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
 *      state or takes parameters stays unresolved and keeps the benefit of the
 *      doubt, because guessing there is how a rule starts firing on correct
 *      code. The walk is depth-bounded and cycle-safe.
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

    @Test
    fun `a function with parameters is not a named constant`() {
        assertFalse(
            "majority-without-quorum" in rules(
                "main.rell" to dao(
                    "function participation_floor(n: integer): integer = n;",
                    "participation_floor(1)"
                )
            ),
            "a function with parameters returns what the CALL SITE passed, so the declaration is not " +
                "its value and the bound stays unresolved"
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
    // the sample and its control
    // =====================================================================

    @Test
    fun `the floor behind a call is caught and its val control stays caught`() {
        assertTrue(
            "majority-without-quorum" in rulesOf("r17-quorum-floor-behind-a-function-call"),
            "r17-quorum-floor-behind-a-function-call must draw the rule its CORPUS row names"
        )
        assertTrue(
            "majority-without-quorum" in rulesOf("r17-quorum-floor-control-the-same-one-as-a-val"),
            "its control must stay caught"
        )
    }

    /**
     * The bound resolver must not start valuing a floor it cannot see.
     * `r14-fp-turnout-floor-without-the-word-quorum` derives its floor from a
     * helper over the roll and `clean-quorum-gated-dao` reads it out of module
     * args; both are correct code and must stay silent.
     */
    @Test
    fun `a floor this scan cannot value stays a floor`() {
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
