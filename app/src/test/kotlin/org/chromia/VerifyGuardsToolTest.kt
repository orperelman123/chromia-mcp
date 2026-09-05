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
 * verify_guards is the mutant discipline exported to the agent's dapp, so it
 * is tested the way the discipline demands: every verdict it can give is
 * driven, including the two that name a fake green.
 *
 * The dapp is deliberately tiny and FT4-free so nothing here depends on
 * module_args: a pot, a seed, and a take() with one balance guard.
 */
class VerifyGuardsToolTest {

    private val repo = RecordingRepository()

    private val main = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            require(amount > 0, "amount must be positive");
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            require(amount > 0, "amount must be positive");
            val p = pot @ { .id == 1 };
            require(p.balance >= amount, "insufficient");
            update p ( .balance -= amount );
        }
    """.trimIndent()

    private val tests = """
        @test module;
        import main;
        function test_overdraft_must_fail() {
            rell.test.tx().op(main.seed(10)).run();
            rell.test.tx().op(main.take(11)).run_must_fail("insufficient");
        }
        function test_trivial() {
            assert_equals(1, 1);
        }
        function test_wrong_on_purpose() {
            assert_equals(1, 2);
        }
    """.trimIndent()

    private val balanceGuard = "require(p.balance >= amount, \"insufficient\");"

    private fun call(vararg guards: JsonObject): JsonObject {
        // These run real transactions; a database is required and its absence is a
        // failure, never a skip (GOAL.md: a fake green is worse than a red).
        assertNotNull(System.getenv(RunRellTests.DATABASE_URL_ENV), "verify_guards runs real tests and needs CHROMIA_TEST_DATABASE_URL")
        val result = runBlocking {
            VerifyGuardsStrategy().execute(
                CallToolRequest(
                    name = "verify_guards",
                    arguments = buildJsonObject {
                        put("files", buildJsonObject { put("main.rell", main); put("main_test.rell", tests) })
                        put("guards", buildJsonArray { guards.forEach { add(it) } })
                    }
                ),
                repo
            )
        }
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        return result.structuredContent!!.jsonObject
    }


    private fun callWith(files: Map<String, String>, vararg guards: JsonObject): JsonObject {
        assertNotNull(System.getenv(RunRellTests.DATABASE_URL_ENV), "verify_guards runs real tests and needs CHROMIA_TEST_DATABASE_URL")
        val result = runBlocking {
            VerifyGuardsStrategy().execute(
                CallToolRequest(
                    name = "verify_guards",
                    arguments = buildJsonObject {
                        put("files", buildJsonObject { files.forEach { (k, v) -> put(k, v) } })
                        put("guards", buildJsonArray { guards.forEach { add(it) } })
                    }
                ),
                repo
            )
        }
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        return result.structuredContent!!.jsonObject
    }

    private fun guard(test: String, replacement: String? = null) = buildJsonObject {
        put("guard", balanceGuard)
        put("test", test)
        if (replacement != null) put("replacement", replacement)
    }

    private fun verdictOf(r: JsonObject) = r["results"]!!.jsonArray.single().jsonObject["verdict"]!!.jsonPrimitive.content

    @Test
    fun aRealGuardIsLoadBearing() {
        val r = call(guard("test_overdraft_must_fail"))
        assertEquals("load_bearing", verdictOf(r), r.toString())
        assertEquals("true", r["ok"]!!.jsonPrimitive.content, r.toString())
        assertTrue(
            r["results"]!!.jsonArray.single().jsonObject["evidence"]!!.jsonPrimitive.content.contains("did not fail"),
            "the evidence must quote the attack landing: $r"
        )
    }

    /** The fake green with a security label on it: green with the guard, green without. */
    @Test
    fun aTestThatDoesNotExerciseTheGuardIsVacuous() {
        val r = call(guard("test_trivial"))
        assertEquals("vacuous", verdictOf(r), r.toString())
        assertEquals("false", r["ok"]!!.jsonPrimitive.content, "a vacuous test must not count as proof: $r")
    }

    /** A test that fails WITH the guard proves nothing about it in either state. */
    @Test
    fun aTestThatFailsOnTheRealCodeIsBaselineRed() {
        val r = call(guard("test_wrong_on_purpose"))
        assertEquals("baseline_red", verdictOf(r), r.toString())
        assertEquals("false", r["ok"]!!.jsonPrimitive.content)
    }

    /** A mutant that does not compile went red for the wrong reason. */
    @Test
    fun aMutantThatDoesNotCompileIsEnvironmentalNotProof() {
        val r = call(guard("test_overdraft_must_fail", replacement = "require("))
        assertEquals("environmental", verdictOf(r), r.toString())
        assertEquals("false", r["ok"]!!.jsonPrimitive.content, "a broken mutant must never read as a proven guard: $r")
    }

    /** Several guards: one real, one vacuous - ok is false and both verdicts are reported. */
    @Test
    fun okRequiresEveryNamedGuardToBeLoadBearing() {
        val r = call(guard("test_overdraft_must_fail"), guard("test_trivial"))
        val verdicts = r["results"]!!.jsonArray.map { it.jsonObject["verdict"]!!.jsonPrimitive.content }
        assertEquals(listOf("load_bearing", "vacuous"), verdicts, r.toString())
        assertEquals("false", r["ok"]!!.jsonPrimitive.content)
        assertEquals("1", r["loadBearing"]!!.jsonPrimitive.content)
    }

    @Test
    fun aGuardThatIsNotInTheSourceIsReportedNotGuessed() {
        val r = call(buildJsonObject { put("guard", "require(false, \"never here\");"); put("test", "test_overdraft_must_fail") })
        assertEquals("guard_not_found", verdictOf(r), r.toString())
    }

    /**
     * THE MUTATION MUST HIT PRODUCTION CODE. A guard string can also appear in
     * the test module - here as the message the test asserts on, quoted whole.
     * Mutating the first file that contains it would delete the ASSERTION and
     * report whatever happened next as a verdict about the guard. The test file
     * is listed first on purpose.
     */
    @Test
    fun aGuardThatAlsoAppearsInTheTestModuleIsMutatedInProductionOnly() {
        val testsQuotingTheGuard = tests.replace(
            "function test_trivial() {",
            "// the guard, quoted whole: require(p.balance >= amount, \"insufficient\");\nfunction test_trivial() {"
        )
        val r = callWith(linkedMapOf("main_test.rell" to testsQuotingTheGuard, "main.rell" to main), guard("test_overdraft_must_fail"))
        assertEquals("load_bearing", verdictOf(r), "the production guard must be the one removed: $r")
    }

    /** A guard that lives only in test code is not a guard. */
    @Test
    fun aGuardFoundOnlyInTestCodeIsNotFound() {
        val r = callWith(
            linkedMapOf("main.rell" to main, "main_test.rell" to tests),
            buildJsonObject { put("guard", "assert_equals(1, 2);"); put("test", "test_overdraft_must_fail") }
        )
        assertEquals("guard_not_found", verdictOf(r), r.toString())
        assertTrue(
            r["results"]!!.jsonArray.single().jsonObject["evidence"]!!.jsonPrimitive.content.contains("only in test code"),
            "the evidence must say where it WAS found: $r"
        )
    }

    /** Two production files carrying the same line: refuse to guess which one is meant. */
    @Test
    fun aGuardPresentInTwoProductionFilesIsAmbiguousNotGuessed() {
        val twin = "module;\nfunction unrelated(amount: integer): integer {\n    require(amount > 0, \"amount must be positive\");\n    return amount;\n}"
        val r = callWith(
            linkedMapOf("main.rell" to main, "helper.rell" to twin, "main_test.rell" to tests),
            buildJsonObject { put("guard", "require(amount > 0, \"amount must be positive\");"); put("test", "test_overdraft_must_fail") }
        )
        assertEquals("guard_ambiguous", verdictOf(r), r.toString())
    }
}
