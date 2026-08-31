package org.chromia

import kotlinx.serialization.json.JsonPrimitive
import org.chromia.tools.CheckDappProject
import org.chromia.tools.DappScaffold
import org.chromia.tools.RunRellTests
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
}
