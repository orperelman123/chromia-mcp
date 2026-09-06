package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.RunRellTests
import org.chromia.tools.VerifyGuardsStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * THE VERIFY_GUARDS SCOREBOARD. Round 11 attacked the tool the round it shipped
 * and got six wrong answers - four of them ok:true - each from one omission
 * (raw-text search that saw comments; replace-all within a file; a narrower
 * notion of "test file" than the runner's; alsoRemove crediting a vacuous guard
 * with a real one's red; a caller-supplied fragment outranking the guard's own
 * refusal). The probes live as fixtures under
 * exploit-corpus/realworld/adversary-round11/vg/ with their raw verdicts; this
 * class is the same probes as tests, each pinned to the TRUE verdict, so a
 * regression in the tool goes red here the way a regression in a rule goes red
 * in ExploitCorpusScoreboardTest. A verification tool that can be fooled is a
 * finding of the same rank as a drain, because an agent trusts it exactly as it
 * trusts ok:true.
 */
class VerifyGuardsProbeTest {

    private val repo = RecordingRepository()

    private val tests = """
        @test module;
        import main;
        function test_overdraft_must_fail() {
            rell.test.tx().op(main.seed(10)).run();
            rell.test.tx().op(main.take(11)).run_must_fail("insufficient");
        }
        function test_seed_negative_must_fail() {
            rell.test.tx().op(main.seed(-5)).run_must_fail("amount must be positive");
        }
        function test_small_take_succeeds() {
            rell.test.tx().op(main.seed(10)).run();
            rell.test.tx().op(main.take(5)).run();
        }
    """.trimIndent()

    private val bal = "require(p.balance >= amount, \"insufficient\");"
    private val pos = "require(amount > 0, \"amount must be positive\");"

    private fun run(
        files: Map<String, String>,
        guard: JsonObject,
        moduleArgs: Map<String, Map<String, Int>> = emptyMap()
    ): JsonObject {
        assertNotNull(System.getenv(RunRellTests.DATABASE_URL_ENV), "verify_guards runs real tests and needs CHROMIA_TEST_DATABASE_URL")
        val result = runBlocking {
            VerifyGuardsStrategy().execute(
                CallToolRequest(
                    name = "verify_guards",
                    arguments = buildJsonObject {
                        put("files", buildJsonObject { files.forEach { (k, v) -> put(k, v) } })
                        put("guards", buildJsonArray { add(guard) })
                        if (moduleArgs.isNotEmpty()) {
                            put(
                                "moduleArgs",
                                buildJsonObject {
                                    moduleArgs.forEach { (m, a) ->
                                        put(m, buildJsonObject { a.forEach { (k, v) -> put(k, v) } })
                                    }
                                }
                            )
                        }
                    }
                ),
                repo
            )
        }
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        return result.structuredContent!!.jsonObject["results"]!!.jsonArray.single().jsonObject
    }

    private fun guard(guard: String, test: String, replacement: String? = null, alsoRemove: List<String> = emptyList(), attackLanded: String? = null) =
        buildJsonObject {
            put("guard", guard)
            put("test", test)
            if (replacement != null) put("replacement", replacement)
            if (alsoRemove.isNotEmpty()) put("alsoRemove", buildJsonArray { alsoRemove.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            if (attackLanded != null) put("attackLanded", attackLanded)
        }

    private fun verdict(r: JsonObject) = r["verdict"]!!.jsonPrimitive.content
    private fun evidence(r: JsonObject) = r["evidence"]!!.jsonPrimitive.content

    // ---- P1: a comment quoting the check the other way round ----------------
    private val p1Main = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            // INVARIANT: require(p.balance >= amount) - you cannot take what is not there.
            require(amount <= p.balance, "insufficient");
            update p ( .balance -= amount );
        }
    """.trimIndent()

    /** Round 11 answered `vacuous` for a real guard. A comment is not a guard. */
    @Test
    fun p1CommentOnlyMatchIsNotFoundNotVacuous() {
        val r = run(mapOf("main.rell" to p1Main, "main_test.rell" to tests), guard("require(p.balance >= amount)", "test_overdraft_must_fail"))
        assertEquals("guard_not_found", verdict(r), r.toString())
        assertTrue(evidence(r).contains("COMMENT"), "the evidence must say it was found in prose: $r")
    }

    @Test
    fun p1ControlTheRealGuardIsLoadBearing() {
        val r = run(mapOf("main.rell" to p1Main, "main_test.rell" to tests), guard("require(amount <= p.balance, \"insufficient\");", "test_overdraft_must_fail"))
        assertEquals("load_bearing", verdict(r), r.toString())
    }

    // ---- P2: the tail of a block comment ------------------------------------
    private val p2Main = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            /* SECURITY NOTE: the line below is what stops an overdraft. */
            require(p.balance >= amount, "insufficient");
            /* Everything above is the withdrawal path. */
            update p ( .balance -= amount );
        }
    """.trimIndent()

    /** Round 11 answered `load_bearing` (ok:true): deleting the block-comment terminator commented out the real guard. */
    @Test
    fun p2BlockCommentTerminatorIsNotAGuard() {
        val r = run(mapOf("main.rell" to p2Main, "main_test.rell" to tests), guard("the line below is what stops an overdraft. */", "test_overdraft_must_fail"))
        assertEquals("guard_not_found", verdict(r), r.toString())
    }

    // ---- P3: the same line twice in one file, the named copy dead -----------
    private val p3Main = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            require(amount > 0, "amount must be positive");
            create pot(id = 1, balance = amount);
        }
        operation ping(amount: integer) {
            require(amount > 0, "amount must be positive");
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            require(p.balance >= amount, "insufficient");
            update p ( .balance -= amount );
        }
    """.trimIndent()

    /** Round 11 replaced BOTH copies and certified the dead one. */
    @Test
    fun p3DuplicateWithinOneFileIsAmbiguous() {
        val r = run(mapOf("main.rell" to p3Main, "main_test.rell" to tests), guard(pos, "test_seed_negative_must_fail"))
        assertEquals("guard_ambiguous", verdict(r), r.toString())
    }

    @Test
    fun p3bDuplicateAcrossFilesIsAmbiguous() {
        val helper = "module;\nfunction ping(amount: integer): integer {\n    require(amount > 0, \"amount must be positive\");\n    return amount;\n}"
        val main = p3Main.replace("operation ping(amount: integer) {\n    require(amount > 0, \"amount must be positive\");\n}\n", "")
        val r = run(mapOf("main.rell" to main, "helper.rell" to helper, "main_test.rell" to tests), guard(pos, "test_seed_negative_must_fail"))
        assertEquals("guard_ambiguous", verdict(r), r.toString())
    }

    /** Disambiguated by its trailing comment, the dead copy alone is vacuous - and a trailing comment must still match. */
    @Test
    fun p3cTheNamedCopyAloneIsVacuous() {
        val main = p3Main.replace(
            "operation ping(amount: integer) {\n    require(amount > 0, \"amount must be positive\");",
            "operation ping(amount: integer) {\n    require(amount > 0, \"amount must be positive\"); // ping"
        )
        val r = run(mapOf("main.rell" to main, "main_test.rell" to tests), guard("$pos // ping", "test_seed_negative_must_fail"))
        assertEquals("vacuous", verdict(r), r.toString())
    }

    // ---- P4: a header-less sibling of a @test module is TEST code -----------
    private val p4Main = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            require(p.balance >= amount, "insufficient");
            update p ( .balance -= amount );
        }
    """.trimIndent()

    /** Round 11 mutated the test helper's assertion and certified it ok:true. */
    @Test
    fun p4HeaderlessTestSiblingIsTestCode() {
        val suite = "@test module;\nimport main;\nfunction test_overdraft_must_fail() {\n    rell.test.tx().op(main.seed(10)).run();\n    overdraft_is_refused();\n}\n"
        val attacks = "function overdraft_is_refused() {\n    rell.test.tx().op(main.take(11)).run_must_fail(\"insufficient\");\n}\n"
        val r = run(
            mapOf("main.rell" to p4Main, "suite/module.rell" to suite, "suite/attacks.rell" to attacks),
            guard("run_must_fail(\"insufficient\")", "test_overdraft_must_fail", replacement = "run()", attackLanded = "insufficient")
        )
        assertEquals("guard_not_found", verdict(r), r.toString())
        assertTrue(evidence(r).contains("test code"), r.toString())
    }

    /** Same, with test SETUP as the "guard" and the default fragment: round 11 certified it ok:true. */
    @Test
    fun p4bHeaderlessTestSetupIsTestCode() {
        val suite = "@test module;\nimport main;\nfunction test_overdraft_must_fail() {\n    fixture();\n    rell.test.tx().op(main.take(1)).run_must_fail(\"insufficient\");\n}\n"
        val attacks = "function fixture() {\n    rell.test.tx().op(main.seed(10)).run();\n    rell.test.tx().op(main.take(10)).run();\n}\n"
        val r = run(
            mapOf("main.rell" to p4Main, "suite/module.rell" to suite, "suite/attacks.rell" to attacks),
            guard("rell.test.tx().op(main.take(10)).run();", "test_overdraft_must_fail")
        )
        assertEquals("guard_not_found", verdict(r), r.toString())
    }

    // ---- P5: alsoRemove carrying a vacuous guard on a real one's back -------
    private val p5Main = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation ping(amount: integer) {
            require(amount > 0, "ping needs a positive amount");
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            require(p.balance >= amount, "insufficient");
            update p ( .balance -= amount );
        }
    """.trimIndent()
    private val p5Tests = "@test module;\nimport main;\nfunction test_overdraft_must_fail() {\n    rell.test.tx().op(main.seed(10)).run();\n    rell.test.tx().op(main.take(11)).run_must_fail(\"insufficient\");\n}\n"

    /** Round 11 certified ping's guard because the REAL guard was listed in alsoRemove. */
    @Test
    fun p5AlsoRemoveCannotCarryAVacuousGuard() {
        val r = run(mapOf("main.rell" to p5Main, "main_test.rell" to p5Tests), guard("require(amount > 0, \"ping needs a positive amount\");", "test_overdraft_must_fail", alsoRemove = listOf(bal)))
        assertEquals("vacuous", verdict(r), r.toString())
        assertTrue(evidence(r).contains("alsoRemove"), "the evidence must name what actually carried the red: $r")
    }

    @Test
    fun p5bTheSameGuardWithoutAlsoRemoveIsVacuous() {
        val r = run(mapOf("main.rell" to p5Main, "main_test.rell" to p5Tests), guard("require(amount > 0, \"ping needs a positive amount\");", "test_overdraft_must_fail"))
        assertEquals("vacuous", verdict(r), r.toString())
    }

    // ---- P6: the guard text lives only inside a string literal --------------
    @Test
    fun p6GuardInsideAStringLiteralIsNotAGuard() {
        val main = p4Main.replace(
            "operation take(amount: integer) {",
            "function messages(): list<text> = [\"require(p.balance >= amount, \\\"insufficient\\\");\"];\noperation take(amount: integer) {"
        ).replace("require(p.balance >= amount, \"insufficient\");\n    update", "require(amount <= p.balance, \"insufficient\");\n    update")
        val r = run(mapOf("main.rell" to main, "main_test.rell" to tests), guard("require(p.balance >= amount, \\\"insufficient\\\");", "test_overdraft_must_fail"))
        assertEquals("guard_not_found", verdict(r), r.toString())
    }

    // ---- the guard's own message outranks any caller-supplied fragment ------
    /**
     * A replacement that TIGHTENS the guard so a legitimate take is refused, with
     * attackLanded chosen to match the refusal message. The test goes red under
     * the mutant - with the guard's own message in the error - and a free-form
     * fragment must not turn that refusal into "the attack landed".
     * (A first version of this case used a semantically identical replacement;
     * the mutant still refused the overdraft, the must-fail test stayed green,
     * and the tool correctly said vacuous - the test was wrong, not the tool.)
     */
    @Test
    fun theGuardsOwnMessageInTheErrorIsARefusalWhateverFragmentIsSupplied() {
        val r = run(
            mapOf("main.rell" to p4Main, "main_test.rell" to tests),
            guard(bal, "test_small_take_succeeds", replacement = "require(p.balance >= amount * 10, \"insufficient\");", attackLanded = "insufficient")
        )
        assertEquals("still_refused", verdict(r), r.toString())
        assertEquals("false", r["loadBearing"]!!.jsonPrimitive.content)
    }

    // =====================================================================
    // ROUND 12. The round-11 fix guards the SEARCH (comments and strings are
    // masked, duplicates refused, test ownership follows the runner) and the
    // ERROR (the guard's own message outranks any caller fragment). Nothing
    // guards the SUBSTITUTION, and nothing requires alsoRemove to be disjoint
    // from the guard. Three false verdicts came out of that, two of them
    // ok:true. The probes and their raw verdicts are in
    // exploit-corpus/realworld/adversary-round12/vg/.
    // =====================================================================

    private val LF = "\n"
    private val CRLF = "\r\n"

    /**
     * Two guards and an audit comment: the size check is VACUOUS for an
     * overdraft of 11, the balance check is the real one, and the comment
     * supplies a block-comment terminator for a replacement to run to.
     */
    private val p7Main = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            require(amount > 0, "amount must be positive");
            require(p.balance >= amount, "insufficient");
            /* AUDIT 2026-09: the two checks above are the withdrawal guards. */
            update p ( .balance -= amount );
        }
    """.trimIndent()

    // ---- P7: the REPLACEMENT's own message ---------------------------------
    /**
     * Round 12. Identical to
     * theGuardsOwnMessageInTheErrorIsARefusalWhateverFragmentIsSupplied except
     * for the words inside the replacement's require(). The guard is replaced by
     * a STRICTER one, a legitimate take of 5 from a balance of 10 is refused, no
     * overdraft happens - and because the refusal message is the REPLACEMENT's
     * rather than the guard's, the round-11 rule does not see it and the
     * caller-supplied attackLanded is believed instead. Round 12 measured
     * verdict=load_bearing, ok:true. The refusal messages have to be read from
     * the replacement as well as from the guard.
     */
    @Test
    fun p7AReplacementThatRefusesInItsOwnWordsIsStillARefusal() {
        val r = run(
            mapOf("main.rell" to p4Main, "main_test.rell" to tests),
            guard(
                bal,
                "test_small_take_succeeds",
                replacement = "require(p.balance >= amount * 10, \"vault is closed\");",
                attackLanded = "vault is closed"
            )
        )
        assertEquals("still_refused", verdict(r), r.toString())
        assertEquals("false", r["loadBearing"]!!.jsonPrimitive.content)
    }

    // ---- P8: the replacement is a comment opener ---------------------------
    /**
     * Round 12. maskRellSource runs over the SUBMITTED text before the search;
     * the replacement is substituted raw afterwards. A replacement that opens a
     * block comment at a vacuous guard's site runs to the next terminator in the
     * file and comments the REAL guard out on the way, so the overdraft lands
     * and the vacuous guard is credited with it. Round 12 measured
     * verdict=load_bearing, ok:true - round 11's P2 (a block comment's
     * terminator named as a "guard") rebuilt out of the one input the fix does
     * not look at. The fix refuses the substitution: the masked file must be
     * identical outside the replaced span, so a replacement that opens a
     * comment or a string is `replacement_rejected` rather than judged - the
     * guard IS vacuous (p8b), but a verdict read through a mutation that
     * rewrote the rest of the file would be a guess wearing a verdict's name.
     */
    @Test
    fun p8AReplacementCannotCommentOutTheRealGuard() {
        val r = run(
            mapOf("main.rell" to p7Main, "main_test.rell" to tests),
            guard(pos, "test_overdraft_must_fail", replacement = "/*")
        )
        assertEquals("replacement_rejected", verdict(r), r.toString())
        assertEquals("false", r["loadBearing"]!!.jsonPrimitive.content)
    }

    /** Control: the same guard DELETED rather than replaced is correctly vacuous today. */
    @Test
    fun p8bControlDeletingTheSameGuardIsVacuous() {
        val r = run(mapOf("main.rell" to p7Main, "main_test.rell" to tests), guard(pos, "test_overdraft_must_fail"))
        assertEquals("vacuous", verdict(r), r.toString())
    }

    // ---- P9: alsoRemove that contains the guard ----------------------------
    /**
     * Round 12. The control run strips ONLY alsoRemove and requires the test to
     * still pass, "or the named guard proved nothing". Nothing requires
     * alsoRemove to be disjoint from the guard, so an entry that CONTAINS the
     * guard strips the guard in the control too, the control goes red, and a
     * genuinely load-bearing guard is reported vacuous - with evidence reading
     * "the lines in alsoRemove are what the test measures, not this guard" while
     * the guard was one of those lines. Round 12 measured verdict=vacuous; the
     * control below shows the same guard is load_bearing. The fix refuses the
     * input (`also_remove_overlaps_guard`) instead of guessing which of the two
     * readings the caller meant: alsoRemove must be disjoint text from the guard.
     */
    @Test
    fun p9AlsoRemoveOverlappingTheGuardCannotMakeItVacuous() {
        val r = run(
            mapOf("main.rell" to p7Main, "main_test.rell" to tests),
            guard(bal, "test_overdraft_must_fail", alsoRemove = listOf(pos + LF + "    " + bal))
        )
        assertEquals("also_remove_overlaps_guard", verdict(r), r.toString())
        assertEquals("false", r["loadBearing"]!!.jsonPrimitive.content)
    }

    /** Control: the same guard, no alsoRemove. Correctly load_bearing today. */
    @Test
    fun p9bControlWithoutAlsoRemoveIsLoadBearing() {
        val r = run(mapOf("main.rell" to p7Main, "main_test.rell" to tests), guard(bal, "test_overdraft_must_fail"))
        assertEquals("load_bearing", verdict(r), r.toString())
    }

    // ---- P10: CRLF. Probed in round 12 and NOT broken ----------------------
    /**
     * Windows line endings in the submitted files, with the guard typed using LF
     * as an agent types it into JSON. A single-line guard is found and verified
     * correctly; a guard spanning two lines is not found, which is a refusal
     * rather than a wrong answer. Pinned so a future offset or masking change
     * cannot quietly turn either of them into a verdict.
     */
    @Test
    fun p10CrlfSingleLineGuardIsStillLoadBearing() {
        val r = run(
            mapOf("main.rell" to p4Main.replace(LF, CRLF), "main_test.rell" to tests.replace(LF, CRLF)),
            guard(bal, "test_overdraft_must_fail")
        )
        assertEquals("load_bearing", verdict(r), r.toString())
    }

    @Test
    fun p10bCrlfTwoLineGuardIsNotFoundNotGuessed() {
        val r = run(
            mapOf("main.rell" to p7Main.replace(LF, CRLF), "main_test.rell" to tests.replace(LF, CRLF)),
            guard(pos + LF + "    " + bal, "test_overdraft_must_fail")
        )
        assertEquals("guard_not_found", verdict(r), r.toString())
    }

    // ---- P11: DEFENCE IN DEPTH read as the attack landing -----------------
    // ROUND 13, AND P11/P12 ARE RED ON PURPOSE. The tool is NOT fixed here: an
    // adversary round pins the TRUE verdict and leaves the fix to the fix lane,
    // so the two probes below fail against the shipped tool, and the failure
    // message is the finding.
    //
    // Round 11's lesson was "an error that contains any string literal from the
    // GUARD line is the guard REFUSING, whatever fragment the caller supplied";
    // round 12 added the REPLACEMENT's literals for the same reason. Neither
    // consults any OTHER production line. So when a second require() refuses the
    // same attack - which is what defence in depth IS - its refusal message is
    // free for a caller's attackLanded fragment to match, and the tool answers
    // load_bearing with ok:true.
    //
    // The error shape says it outright. run_must_fail produced
    //   "expected to contain <does not hold> but was
    //    <[main:take(main.rell:9)] Operation 'main:take' failed:
    //     the balance would go negative>"
    // - the operation FAILED, so the attack did not land, and a transaction
    // refused with a different message is the definition of still_refused. Only
    // "Transaction did not fail" means the attack landed, which is why that is
    // the DEFAULT attackLanded - and with the default this same probe correctly
    // answers red_for_another_reason (p11c). Every false verdict in this class
    // needs a caller-supplied fragment, exactly as round 11's did.
    private val depthMain = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            require(amount <= p.balance, "the pot does not hold that much");
            require(p.balance - amount >= 0, "the balance would go negative");
            update p ( .balance -= amount );
        }
    """.trimIndent()

    // The expected fragment is a STRICT SUBSTRING of the guard's message, which
    // is what an author writes: assert on the stable part of your own error.
    // That is the whole difference from round 11's probe, where the fragment WAS
    // the guard's entire message, so the guard's own literal landed in the
    // mismatch text by accident of the fixture and the tool held for a reason
    // that was not a guard.
    private val depthTests = """
        @test module;
        import main;
        function test_overdraft_must_fail() {
            rell.test.tx().op(main.seed(10)).run();
            rell.test.tx().op(main.take(11)).run_must_fail("does not hold");
        }
    """.trimIndent()

    private val depthGuard = "require(amount <= p.balance, \"the pot does not hold that much\");"
    private val depthSecond = "require(p.balance - amount >= 0, \"the balance would go negative\");"

    /** TRUE VERDICT: still_refused. The tool answers load_bearing, ok:true. */
    @Test
    fun p11ASecondGuardsRefusalIsNotTheAttackLanding() {
        val r = run(
            mapOf("main.rell" to depthMain, "main_test.rell" to depthTests),
            guard(depthGuard, "test_overdraft_must_fail", attackLanded = "negative")
        )
        assertEquals("still_refused", verdict(r), r.toString())
        assertEquals("false", r["loadBearing"]!!.jsonPrimitive.content, r.toString())
    }

    /**
     * The same shape with the refusing bound coming from module_args instead of
     * from source, so the value that refuses is not in any file the tool can
     * read. TRUE VERDICT: still_refused. The tool answers load_bearing, ok:true.
     */
    private val depthArgsMain = """
        module;
        struct module_args { daily_limit: integer; }
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            require(amount <= p.balance, "the pot does not hold that much");
            require(amount <= chain_context.args.daily_limit, "over the negative-balance protection limit");
            update p ( .balance -= amount );
        }
    """.trimIndent()

    @Test
    fun p12AModuleArgsLimitRefusingTheAttackIsNotTheAttackLanding() {
        val r = run(
            mapOf("main.rell" to depthArgsMain, "main_test.rell" to depthTests),
            guard(depthGuard, "test_overdraft_must_fail", attackLanded = "negative"),
            moduleArgs = mapOf("main" to mapOf("daily_limit" to 10))
        )
        assertEquals("still_refused", verdict(r), r.toString())
        assertEquals("false", r["loadBearing"]!!.jsonPrimitive.content, r.toString())
    }

    /**
     * CONTROL 1: the same files and the same guard with the DEFAULT
     * attackLanded. The tool is right here - the mismatch text does not contain
     * "did not fail" - which is why every false verdict in this class needs a
     * caller-supplied fragment. GREEN today.
     */
    @Test
    fun p11cTheDefaultFragmentDoesNotMistakeTheSecondRefusal() {
        val r = run(
            mapOf("main.rell" to depthMain, "main_test.rell" to depthTests),
            guard(depthGuard, "test_overdraft_must_fail")
        )
        assertEquals("red_for_another_reason", verdict(r), r.toString())
    }

    /**
     * CONTROL 2: the honest call - the second guard named in alsoRemove, so both
     * come out and the attack really does land. GREEN today, and it is what
     * makes p11 a WRONG answer rather than a conservative one: the tool has a
     * correct path to load_bearing for this guard and did not take it.
     */
    @Test
    fun p11dWithTheSecondGuardInAlsoRemoveTheAttackReallyLands() {
        val r = run(
            mapOf("main.rell" to depthMain, "main_test.rell" to depthTests),
            guard(depthGuard, "test_overdraft_must_fail", alsoRemove = listOf(depthSecond))
        )
        assertEquals("load_bearing", verdict(r), r.toString())
    }

    // ---- P13: the FILES MAP. Probed in round 13 and NOT broken -------------
    private val caseLive = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            require(amount <= p.balance, "insufficient");
            update p ( .balance -= amount );
        }
    """.trimIndent()

    /**
     * Two production paths differing only in case, the guard in ONE of them.
     * Most file systems the runner writes to treat them as the same file, so a
     * verdict computed from the map could be about a file the run never
     * compiled. The runner refuses the submission by name. Pinned so a future
     * runner cannot start silently collapsing them.
     */
    @Test
    fun p13CaseCollidingPathsAreRefusedNotGuessed() {
        val dead = caseLive.replace("    require(amount <= p.balance, \"insufficient\");\n", "")
        val r = run(
            mapOf("src/Main.rell" to caseLive, "src/main.rell" to dead, "src/test/main_test.rell" to tests),
            guard("require(amount <= p.balance, \"insufficient\");", "test_overdraft_must_fail")
        )
        assertEquals("runner_error", verdict(r), r.toString())
        assertTrue(evidence(r).contains("Case-insensitive"), evidence(r))
    }

    /** A files-map key that walks out of the source root is refused by name. */
    @Test
    fun p13bAPathThatWalksOutOfTheSourceRootIsRefused() {
        val r = run(
            mapOf("../escaped.rell" to caseLive, "main_test.rell" to tests),
            guard("require(amount <= p.balance, \"insufficient\");", "test_overdraft_must_fail")
        )
        assertEquals("runner_error", verdict(r), r.toString())
        assertTrue(evidence(r).contains(".."), evidence(r))
    }

    /**
     * The SAME test name declared in two test modules. The tool must not answer
     * about a test it cannot identify: it returns test_not_found naming both.
     */
    @Test
    fun p13cTheSameTestNameInTwoModulesIsNotGuessed() {
        val empty = "@test module;\nimport main;\nfunction test_overdraft_must_fail() {\n" +
            "    rell.test.tx().op(main.seed(10)).run();\n}\n"
        val r = run(
            mapOf("main.rell" to caseLive, "a_test.rell" to tests, "b_test.rell" to empty),
            guard("require(amount <= p.balance, \"insufficient\");", "test_overdraft_must_fail")
        )
        assertEquals("test_not_found", verdict(r), r.toString())
    }

    /**
     * A module.rell that declares the guard's own module a TEST module moves the
     * production file onto test surface, and the tool says so rather than
     * mutating a file the runner treats differently.
     */
    @Test
    fun p13dAModuleRellThatRetypesTheGuardsModuleIsNamedNotMutated() {
        val r = run(
            mapOf(
                "src/main.rell" to caseLive,
                "src/main/module.rell" to "@test module;",
                "src/test/main_test.rell" to tests
            ),
            guard("require(amount <= p.balance, \"insufficient\");", "test_overdraft_must_fail")
        )
        assertEquals("guard_not_found", verdict(r), r.toString())
        assertTrue(evidence(r).contains("test code"), evidence(r))
    }

    /**
     * A UTF-8 BOM in front of the module and a non-ASCII character inside the
     * guard's own message: maskRellSource stays length-preserving through both,
     * so the offset still maps back. Probed in round 13 and NOT broken.
     */
    @Test
    fun p13eABomAndANonAsciiGuardMessageAreStillFound() {
        val bom = "\uFEFF"
        val main = bom + caseLive.replace("\"insufficient\"", "\"insufficient \u2013 the pot is short\"")
        val r = run(
            mapOf("main.rell" to main, "main_test.rell" to tests),
            guard("require(amount <= p.balance, \"insufficient \u2013 the pot is short\");", "test_overdraft_must_fail")
        )
        assertEquals("load_bearing", verdict(r), r.toString())
    }

}
