package org.chromia

import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ADDING THE TEST MODULE THE SCAFFOLD SHIPS MUST NOT DISARM THE GATE.
 *
 * The test-surface rule exists for a real reason: a HIGH inside a test fixture
 * is advisory, because code only tests can reach never ships. It decided "app
 * surface" by looking for app roots - non-test modules NOTHING imports - and a
 * project's own `@test module; import main;` made `main` imported, so `main`
 * stopped being a root, no roots existed, and `main` itself was reclassified as
 * test surface. Every HIGH in production code became a non-blocking advisory
 * and `ok` flipped to true.
 *
 * That is not an evasion an attacker had to construct. The scaffold ships that
 * test module, `run_rell_tests` requires it, and the whole documented loop is
 * to write code, write tests, and check the gate. The gate stayed armed only
 * for a `main.rell` submitted alone - which is the one shape a real project
 * never has. Adversary round 9 hit it by accident and it swallowed seven HIGHs
 * at once on one pinned sample.
 *
 * Both directions are pinned here, because the fix must not be "scan
 * everything" - that would fail the gate on fixtures again and teach agents to
 * route around it (GOAL.md principle 3).
 */
class GateTestSurfaceDisarmTest {

    /** A confused-deputy drain: the caller is authenticated, then ignored. */
    private val vulnerableMain = """
        module;
        import lib.ft4.auth;
        entity account { key owner: byte_array; mutable balance: integer = 0; }
        operation withdraw(victim: byte_array, amount: integer) {
            val caller = auth.authenticate();
            require(amount > 0, "amount must be positive");
            val from = account @ { .owner == victim };
            require(from.balance >= amount, "insufficient");
            update from ( .balance -= amount );
            val to = account @ { .owner == caller.id };
            update to ( .balance += amount );
        }
    """.trimIndent()

    /** Exactly what `scaffold_dapp` ships and `run_rell_tests` requires. */
    private val ownTestModule = """
        @test module;
        import main;
        function test_withdraw() {
            main.withdraw(x"AB", 1);
        }
    """.trimIndent()

    private fun highs(r: RellSecurityCheck.Result) = r.findings.filter { it.severity == "HIGH" || it.severity == "CRITICAL" }

    @Test
    fun aProductionHighSurvivesTheProjectsOwnTestModule() {
        val alone = RellSecurityCheck.analyze(mapOf("src/main.rell" to vulnerableMain))
        assertTrue(highs(alone).isNotEmpty(), "the drain must be a HIGH when main.rell is submitted alone")
        assertEquals(false, alone.ok, "and must block")

        val withTests = RellSecurityCheck.analyze(
            mapOf("src/main.rell" to vulnerableMain, "src/test/main_test.rell" to ownTestModule)
        )
        assertEquals(
            highs(alone).map { it.rule to it.line },
            highs(withTests).map { it.rule to it.line },
            "the SAME production line must keep the SAME severity once the project's own test module is " +
                "added - downgrading it is the gate disarming itself on every real project"
        )
        assertEquals(
            false,
            withTests.ok,
            "ok must stay false. It flipped to true here, so every dapp that followed the documented " +
                "loop - scaffold, write tests, check the gate - was certified with its drains intact"
        )
    }

    /**
     * The other direction: the rule this was built for must still work, or the
     * fix has just traded a swallowed HIGH for a gate agents route around.
     */
    @Test
    fun aHighReachableOnlyFromTestsIsStillAdvisory() {
        val fixtureHelper = """
            module;
            entity seeded { key id: text; mutable balance: integer = 0; }
            function force_balance(amount: integer) {
                val s = seeded @ { .id == "fixture" };
                update s ( .balance = amount );
            }
        """.trimIndent()
        val testUsingHelper = """
            @test module;
            import fixture_helper;
            function test_seed() { fixture_helper.force_balance(100); }
        """.trimIndent()

        val r = RellSecurityCheck.analyze(
            mapOf(
                "src/main.rell" to "module;\nquery healthy() = true;",
                "src/test/fixture_helper.rell" to fixtureHelper,
                "src/test/seed_test.rell" to testUsingHelper
            )
        )
        assertTrue(
            r.ok,
            "a helper only the tests can reach must not block the gate - that is what the test-surface " +
                "rule is for, and losing it would teach agents to ignore the gate: ${r.findings}"
        )
    }

    /**
     * ...and the anchor must not be the NAME. `main` is app surface by the
     * Chromia convention chromia.yml enforces, which is what separates a dapp
     * from a fixture when both are reachable only from a test. Anchoring on
     * that name ALONE would hand the disarm straight back: rename the entry
     * module and the gate goes quiet again. This is the rule attacking itself -
     * a rule keyed on something the author picks is not a boundary
     * (GOAL.md principle 2, which this file is a fresh instance of).
     */
    @Test
    fun renamingTheEntryModuleDoesNotDisarmTheGate() {
        val renamed = RellSecurityCheck.analyze(
            mapOf(
                "src/app.rell" to vulnerableMain,
                "src/test/app_test.rell" to """
                    @test module;
                    import app;
                    function test_w() { app.withdraw(x"AB", 1); }
                """.trimIndent()
            )
        )
        assertTrue(
            highs(renamed).isNotEmpty(),
            "an entry module called anything other than `main`, imported only by its own tests, is " +
                "still production code - if renaming it silences the HIGH, the fix is keyed on a name " +
                "the author chooses: ${renamed.findings}"
        )
        assertEquals(false, renamed.ok, "and it must still block")
    }
}
