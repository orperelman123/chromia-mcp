package org.chromia

import kotlinx.serialization.json.JsonPrimitive
import org.chromia.tools.CheckDappProject
import org.chromia.tools.DappScaffold
import org.chromia.tools.RunRellTests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The one-call project gate must actually compile (strategic-audit finding:
 * check_dapp_project could return ok=true for code that does not parse), and
 * run_rell_tests must accept module_args so real FT4 operations can be tested.
 */
class ProjectGateAndModuleArgsTest {

    private val scaffold = DappScaffold.files("gate_demo")
    private val yaml = scaffold.getValue("chromia.yml")
    private val main = scaffold.getValue("src/main.rell")

    @Test
    fun projectGateAcceptsAValidScaffold() {
        val result = CheckDappProject.check(yaml, mapOf("src/main.rell" to main))
        assertTrue(result.ok, "valid scaffold must pass the gate: ${result.errors}")
    }

    @Test
    fun projectGateRejectsCodeThatDoesNotCompile() {
        val broken = main + "\nquery broken() = no_such_symbol;\n"
        val result = CheckDappProject.check(yaml, mapOf("src/main.rell" to broken))
        assertFalse(result.ok, "non-compiling code must fail the gate")
        assertTrue(result.errors.any { it.contains("no_such_symbol") }, result.errors.toString())
    }

    @Test
    fun projectGateRejectsInsecureCode() {
        val insecure = """
            module;
            entity vault { key owner: text; mutable amount: integer; }
            operation drain(owner: text, amount: integer) {
                update vault @ { .owner == owner } ( .amount -= amount );
            }
        """.trimIndent()
        val result = CheckDappProject.check(yaml, mapOf("src/main.rell" to insecure))
        assertFalse(result.ok, "unauthenticated mutation must fail the gate")
        assertTrue(result.errors.any { it.contains("unauthenticated-mutation") }, result.errors.toString())
    }

    @Test
    fun projectGateCanSkipCompilation() {
        val broken = main + "\nquery broken() = no_such_symbol;\n"
        val result = CheckDappProject.check(yaml, mapOf("src/main.rell" to broken), compile = false)
        assertTrue(result.ok, "text-only mode must not compile: ${result.errors}")
    }

    @Test
    fun runRellTestsAcceptsModuleArgs() {
        // A module declaring module_args cannot compile without them being supplied.
        val files = mapOf(
            "cfg.rell" to "module;\nstruct module_args { greeting: text; }\nfunction greeting(): text = chain_context.args.greeting;",
            "cfg_test.rell" to "@test module;\nimport cfg;\nfunction test_greeting() { assert_equals(cfg.greeting(), \"hi\"); }"
        )
        val result = RunRellTests.run(
            files,
            databaseUrl = null,
            moduleArgs = mapOf("cfg" to mapOf("greeting" to JsonPrimitive("hi")))
        )
        assertTrue(result.ok, "module_args must reach the compiler: ${result.notes} ${result.cases}")
    }

    /**
     * Adversary round 4 stall: chromia.yml writes byte_array module args as
     * x"02C4..." and every non-expert pasted that literal into run_rell_tests
     * moduleArgs, failing every case with "Can't create ByteArray from string
     * 'x"02C4..."'". The yml literal, 0x-prefixed hex and bare hex must all bind
     * to a byte_array; a non-hex text arg must stay text.
     */
    @Test
    fun runRellTestsAcceptsWrappedHexByteArrayModuleArgs() {
        val files = mapOf(
            "cfg.rell" to """
                module;
                struct module_args {
                    wrapped: byte_array;
                    prefixed: pubkey;
                    bare: byte_array;
                    label: text;
                }
                function wrapped(): byte_array = chain_context.args.wrapped;
                function prefixed(): byte_array = chain_context.args.prefixed;
                function bare(): byte_array = chain_context.args.bare;
                function label(): text = chain_context.args.label;
            """.trimIndent(),
            "cfg_test.rell" to """
                @test module;
                import cfg;
                function test_wrapped() { assert_equals(cfg.wrapped(), x"02C4049F9550DCFF6003347BB3944DF2AA2D6EF5202C22834284B085C56DE8C6DD"); }
                function test_prefixed() { assert_equals(cfg.prefixed(), x"02C4049F9550DCFF6003347BB3944DF2AA2D6EF5202C22834284B085C56DE8C6DD"); }
                function test_bare() { assert_equals(cfg.bare(), x"00CED79962D1150BF844CACB76310D4746C4426558A7FD9C827B30203DACC4CE"); }
                function test_label() { assert_equals(cfg.label(), "0xnot-hex"); }
            """.trimIndent()
        )
        val result = RunRellTests.run(
            files,
            databaseUrl = null,
            moduleArgs = mapOf(
                "cfg" to mapOf(
                    "wrapped" to JsonPrimitive("x\"02C4049F9550DCFF6003347BB3944DF2AA2D6EF5202C22834284B085C56DE8C6DD\""),
                    "prefixed" to JsonPrimitive("0x02c4049f9550dcff6003347bb3944df2aa2d6ef5202c22834284b085c56de8c6dd"),
                    "bare" to JsonPrimitive("00CED79962D1150BF844CACB76310D4746C4426558A7FD9C827B30203DACC4CE"),
                    "label" to JsonPrimitive("0xnot-hex")
                )
            )
        )
        assertTrue(result.ok, "x\"...\", 0x... and bare hex must all bind to byte_array: ${result.notes} ${result.cases}")
        assertEquals(4, result.passed, result.cases.toString())
    }

    @Test
    fun hexLiteralDecodingIsExactAboutWhatItConverts() {
        with(RunRellTests) {
            assertEquals(listOf<Byte>(2, -60), hexLiteralBytes("x\"02C4\"")!!.toList())
            assertEquals(listOf<Byte>(2, -60), hexLiteralBytes("x'02c4'")!!.toList())
            assertEquals(listOf<Byte>(2, -60), hexLiteralBytes("0x02C4")!!.toList())
            assertEquals(listOf<Byte>(2, -60), hexLiteralBytes("  0X02C4 ")!!.toList())
            assertEquals(0, hexLiteralBytes("x\"\"")!!.size)
            // Not literals: bare hex (GtvString decodes it itself), odd length, non-hex, prose.
            assertEquals(null, hexLiteralBytes("02C4"))
            assertEquals(null, hexLiteralBytes("x\"02C\""))
            assertEquals(null, hexLiteralBytes("0x02C"))
            assertEquals(null, hexLiteralBytes("0xnot-hex"))
            assertEquals(null, hexLiteralBytes("0x"))
            assertEquals(null, hexLiteralBytes("x\"02C4\" trailing"))
            assertEquals(net.postchain.gtv.GtvType.BYTEARRAY, jsonToGtv(JsonPrimitive("x\"02C4\"")).type)
            assertEquals(net.postchain.gtv.GtvType.STRING, jsonToGtv(JsonPrimitive("02C4")).type)
            assertEquals(net.postchain.gtv.GtvType.STRING, jsonToGtv(JsonPrimitive("0xnot-hex")).type)
        }
    }
}
