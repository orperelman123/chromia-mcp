package org.chromia

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Test
import java.io.File

/**
 * ROUND 17, `rell_security_check` at the four joins round 16 rebuilt: bound
 * VALUE resolution through literals, vals and module_args defaults; a floor the
 * PROPOSER writes rejected; accumulators followed for
 * `query-returns-secret-data`; and clock differences read as quantities for
 * `unbacked-conversion-credit`.
 *
 * Each of those four is a resolution step, and each has an input that does not
 * reach it. This class is the RECORDER for the round's samples - five
 * attack/control pairs, the correct DAO whose floor arrives at deployment, and
 * the insurance build - so the round's claims about the rules are measurements
 * rather than readings of the source. A thirteenth was added when the fix
 * landed: the round's own `p17s2` control never removed the operation it names,
 * so it is a second copy of the attack, and the control it MEANT to write is
 * recorded beside it.
 *
 * THE EVIDENCE IS FROZEN (the b640e6c pattern, [Round17Evidence]). This class
 * used to write straight into the committed
 * `realworld/adversary-round17/seccheck/raw.json`, so a gate left it modified
 * and the file the round README cites was whatever the last build said. Now the
 * run records to `build/adversary-round17/seccheck/raw.json` and ASSERTS it
 * equal, value by value, to the committed one; a finding that drifts is a
 * regression reported with both values and both paths.
 *
 * There are two frozen files, because round 17's rule lane changed the analyzer
 * this was measuring:
 *  - `seccheck/raw.before-fix.json` is the ADVERSARY'S OWN recording, the
 *    analyzer as the round found it. Nothing runs against it; it is the only
 *    record of what was wrong.
 *  - `seccheck/raw.json` is the AFTER-FIX recording, re-recorded once when
 *    `r17-quorum-floor-behind-a-function-call` and
 *    `r17-quorum-floor-set-by-a-second-operation` were closed, and it is what
 *    this run is asserted against. The two evasions now draw MEDIUM
 *    `majority-without-quorum`, and so does the broken `p17s2` control, which
 *    still holds the attack; every other verdict in the file - including the
 *    correct DAO at `p17s5` and both insurance drains, which stay clean - is
 *    unchanged from the before-fix recording, and the added control is silent.
 */
class Round17SecurityRuleProbeTest {

    /** A DAO whose participation floor is returned by a FUNCTION, not a literal or a val. */
    private val floorThroughAFunction = """
        module;

        entity roll { key owner: byte_array; mutable balance: integer = 0; }

        entity motion {
            key id: integer;
            beneficiary: byte_array;
            amount: integer;
            mutable yes_ballots: integer = 0;
            mutable no_ballots: integer = 0;
        }

        object book { mutable next_id: integer = 1; mutable pot: integer = 0; }

        // THE FLOOR IS A CALL. The value is 1 - one ballot carries anything - but the
        // bound the rule has to resolve is a function's RETURN, not a literal, a val or
        // a module_args default.
        function participation_floor(): integer = 1;

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

    /** The same DAO with the same value written inline, which round 16 pinned as caught. */
    private val floorControlInline = floorThroughAFunction
        .replace("function participation_floor(): integer = 1;", "val PARTICIPATION_FLOOR = 1;")
        .replace(">= participation_floor()", ">= PARTICIPATION_FLOOR")

    /** A floor written by a SECOND operation the same account controls. */
    private val floorWrittenByAnotherOperation = """
        module;

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

        // ROUND 16 REJECTED A FLOOR THE PROPOSER PASSES. Here nobody passes it: it is a
        // stored field, stamped into the motion by propose. It is set by a SECOND
        // permissionless operation the proposer also signs, one block earlier.
        operation set_floor(n: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
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

    /**
     * The same DAO with the floor a constant the operator cannot move - AS THE
     * ROUND WROTE IT, AND IT IS NOT A CONTROL.
     *
     * Kept byte for byte so the after-fix recording is comparable with the
     * before-fix one probe by probe, and kept with this note because the round
     * cited it as a control that the analyzer was silent on. The second
     * `.replace` never matches: `.trimIndent()` strips the raw string's common
     * twelve-space indent, `.prependIndent("        ")` puts eight back on
     * every line, and `.trim()` then removes them from the FIRST line only - so
     * the anchor's body lines carry twelve spaces where the target carries
     * four. `set_floor` is therefore still in this file, writing `book.floor`
     * from its own caller's argument, and the only difference from the attack
     * sample is the `mutable` keyword on a field the operation still assigns.
     *
     * So the `majority-without-quorum` this now draws is CORRECT: the attack is
     * still here. [floorControlNoWriterAtAll] is what the round meant to write,
     * and Round17SecurityRuleFixTest carries the two positive controls (a floor
     * only a module-args admin can set, and one only a stored owner key can).
     */
    private val floorControlImmutableConstant = floorWrittenByAnotherOperation
        .replace(
            "mutable floor: integer = 3; }",
            "floor: integer = 3; }"
        )
        .replace(
            """
            operation set_floor(n: integer) {
                require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
                require(n > 0, "the floor must be positive");
                book.floor = n;
            }
            """.trimIndent().prependIndent("        ").trim(),
            ""
        )

    /**
     * THE CONTROL THE ROUND MEANT: the same DAO with the writing operation
     * REMOVED, so the floor is a constant nobody can move. The removal is
     * checked in the test rather than hoped for.
     */
    private val floorControlNoWriterAtAll = floorWrittenByAnotherOperation
        .replace(Regex("""(?s)\n *// ROUND 16 REJECTED.*?\n *operation set_floor\(n: integer\) \{.*?\n *}\n"""), "\n")
        .replace("mutable floor: integer = 3; }", "floor: integer = 3; }")

    /** Every secret in the table, returned inside a STRUCT rather than as a list of text. */
    private val secretThroughAStruct = """
        module;

        entity credential {
            key owner: byte_array;
            mutable label: text = "";
            mutable secret_token: text = "";
        }

        struct credential_view { owner: byte_array; label: text; secret_token: text; }

        operation store(token: text) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            create credential(owner = op_context.get_signers()[0], secret_token = token);
        }

        // EVERY TOKEN IN THE TABLE, wrapped one level deep in a struct inside a list.
        query all_credentials(): list<credential_view> {
            val out = list<credential_view>();
            for (c in credential @* {}) {
                out.add(credential_view(owner = c.owner, label = c.label, secret_token = c.secret_token));
            }
            return out;
        }
    """.trimIndent()

    /** The same query returning a map keyed by owner. */
    private val secretThroughAMap = """
        module;

        entity credential {
            key owner: byte_array;
            mutable label: text = "";
            mutable secret_token: text = "";
        }

        operation store(token: text) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            create credential(owner = op_context.get_signers()[0], secret_token = token);
        }

        // The same table, in a map<byte_array, text> of owner -> token.
        query token_book(): map<byte_array, text> {
            val out = map<byte_array, text>();
            for (c in credential @* {}) {
                out.put(c.owner, c.secret_token);
            }
            return out;
        }
    """.trimIndent()

    /** A clock difference DIVIDED before it is credited. */
    private val clockDifferenceDivided = """
        module;

        entity vault_row { key owner: byte_array; mutable balance: integer = 0; mutable opened_at: integer = 0; }

        object rate_book { mutable rate_per_second: integer = 1; }

        operation open() {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            create vault_row(owner = op_context.get_signers()[0], opened_at = op_context.last_block_time);
        }

        // THE CLOCK DIFFERENCE IS A QUANTITY, and dividing it by a thousand does not make
        // it one less: nothing here is paid out of a reserve, so the credit is minted
        // from elapsed time.
        operation accrue() {
            val v = require(vault_row @? { .owner == op_context.get_signers()[0] }, "open one first");
            val elapsed_ms = op_context.last_block_time - v.opened_at;
            val seconds = elapsed_ms / 1000;
            update v ( .balance += seconds * rate_book.rate_per_second, .opened_at = op_context.last_block_time );
        }
    """.trimIndent()

    /** The same accrual with the credit taken out of a funded pool in the same operation. */
    private val clockControlPaidFromAPool = """
        module;

        entity vault_row { key owner: byte_array; mutable balance: integer = 0; mutable opened_at: integer = 0; }

        object rate_book { mutable rate_per_second: integer = 1; mutable pool: integer = 0; }

        operation fund(n: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require(n > 0, "amount out of range");
            rate_book.pool += n;
        }

        operation open() {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            create vault_row(owner = op_context.get_signers()[0], opened_at = op_context.last_block_time);
        }

        operation accrue() {
            val v = require(vault_row @? { .owner == op_context.get_signers()[0] }, "open one first");
            val elapsed_ms = op_context.last_block_time - v.opened_at;
            val seconds = elapsed_ms / 1000;
            val want = seconds * rate_book.rate_per_second;
            val paid = if (want > rate_book.pool) rate_book.pool else want;
            rate_book.pool -= paid;
            update v ( .balance += paid, .opened_at = op_context.last_block_time );
        }
    """.trimIndent()

    /**
     * THE FALSE-POSITIVE QUESTION the round-17 brief asks by name: a CORRECT DAO
     * whose participation floor arrives as a module argument at deployment, with
     * no default in the source. The floor is a real number chosen by whoever
     * deploys the chain and it cannot be moved afterwards; if the gate flags
     * this, it is flagging correct code and the agent's answer is to route
     * around it (GOAL.md principle 3).
     */
    private val correctDaoWithADeploymentFloor = """
        module;

        struct module_args { participation_floor: integer; }

        entity roll { key owner: byte_array; mutable balance: integer = 0; }

        entity motion {
            key id: integer;
            beneficiary: byte_array;
            amount: integer;
            floor_at_creation: integer;
            mutable yes_ballots: integer = 0;
            mutable no_ballots: integer = 0;
        }

        object book { mutable next_id: integer = 1; mutable pot: integer = 0; }

        // The floor is configuration, chosen once at deployment and readable by anyone.
        function participation_floor(): integer {
            val n = chain_context.args.participation_floor;
            require(n >= 3, "the participation floor must be at least three");
            return n;
        }

        operation propose(beneficiary: byte_array, amount: integer) {
            require(op_context.is_signer(op_context.get_signers()[0]), "sign it");
            require(amount > 0 and amount <= book.pot, "amount out of range");
            create motion(
                id = book.next_id, beneficiary = beneficiary, amount = amount,
                floor_at_creation = participation_floor()
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

    /** The insurance pool this round built from the `template=ft4` redirect, and its fix. */
    private fun sampleFile(id: String): String =
        File("src/test/resources/exploit-corpus/samples/$id/main.rell").readText()

    private val probes: List<Triple<String, String, String>> by lazy {
        listOf(
            Triple("p17s1_floor_through_a_function", floorThroughAFunction,
                "the participation floor is a function's return rather than a literal, a val or a module_args default"),
            Triple("p17s1_control_the_same_floor_as_a_val", floorControlInline,
                "the identical DAO with the same 1 written as a val"),
            Triple("p17s2_floor_written_by_another_operation", floorWrittenByAnotherOperation,
                "the floor is a stored field, set by a second permissionless operation the proposer also signs"),
            Triple("p17s2_control_the_floor_is_an_immutable_constant", floorControlImmutableConstant,
                "the round's own control, which is NOT one - the set_floor removal never matched, so the " +
                    "writing operation is still in the file and the finding on it is correct"),
            Triple("p17s2_control_the_writer_is_removed_outright", floorControlNoWriterAtAll,
                "MUST STAY CLEAN of majority-without-quorum: the control the round meant - the same DAO " +
                    "with the writing operation actually gone, so nobody can move the floor"),
            Triple("p17s3_secret_through_a_struct", secretThroughAStruct,
                "every token in the table, one level deep inside a struct in a list"),
            Triple("p17s3_secret_through_a_map", secretThroughAMap,
                "the same table as a map<byte_array, text>"),
            Triple("p17s4_clock_difference_divided", clockDifferenceDivided,
                "the clock difference is divided by a thousand before it is credited, and nothing backs it"),
            Triple("p17s4_control_the_same_accrual_paid_from_a_pool", clockControlPaidFromAPool,
                "the identical accrual with the credit clamped to a funded pool in the same operation"),
            Triple("p17s5_correct_dao_with_a_deployment_floor", correctDaoWithADeploymentFloor,
                "MUST STAY CLEAN: a correct DAO whose floor is a module argument with no default, bounded at three"),
            Triple("p17s6_insurance_cancel_refunds_a_spent_premium",
                sampleFile("r17-insurance-cancel-refunds-a-spent-premium"),
                "the running drain this round built from the template=ft4 redirect"),
            Triple("p17s6_insurance_exit_race", sampleFile("r17-insurance-claim-exit-race-on-a-short-reserve"),
                "the same build's first-come-first-served claim against a short reserve"),
            Triple("p17s6_control_insurance_fixed", sampleFile("r17-insurance-cancel-retires-only-the-unclaimed-cover-clean"),
                "MUST STAY CLEAN: the same file with the refund clamped to the unspent premium")
        )
    }

    @Test
    fun `record what rell_security_check says about the round 17 rule probes`() {
        // The round's own p17s2 control still contains the operation it meant to
        // remove; the added one must really be without it, and hoping is not a
        // check.
        require(floorControlImmutableConstant.contains("operation set_floor")) {
            "p17s2's shipped control is recorded BECAUSE it still writes the floor - if that changed, " +
                "the probe's note and the README row have to change with it"
        }
        require(!floorControlNoWriterAtAll.contains("set_floor")) {
            "p17s2's added control must not contain the writing operation; it still does"
        }
        val lines = mutableListOf<String>()
        val rows = buildJsonArray {
            for ((name, source, why) in probes) {
                val result = RellSecurityCheck.analyze(mapOf("main.rell" to source))
                val findings = result.findings.map { "${it.severity} ${it.rule} (line ${it.line})" }
                lines += "%-52s ok=%-5s %s".format(name, result.ok, if (findings.isEmpty()) "no findings" else findings.joinToString("; "))
                add(
                    buildJsonObject {
                        put("probe", name)
                        put("why", why)
                        put("ok", result.ok)
                        put("findings", buildJsonArray { findings.forEach { add(it) } })
                        put("source", source)
                    }
                )
            }
        }
        val relative = "seccheck/raw.json"
        Round17Evidence.record(relative, rows)
        println("ROUND17-SECCHECK\n" + lines.joinToString("\n"))
        Round17Evidence.assertFrozen(relative, rows)
    }
}
