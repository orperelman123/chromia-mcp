package org.chromia

import org.chromia.tools.DappScaffold
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DappScaffoldFt4TemplateTest {

    @Test
    fun ft4TemplateShipsGoldenPattern() {
        val files = DappScaffold.files("notes", template = "ft4")
        assertEquals(
            setOf("chromia.yml", "src/main.rell", "src/test/main_test.rell", "client/example.ts"),
            files.keys
        )
        val main = files.getValue("src/main.rell")
        assertTrue(main.contains("auth.authenticate()"), "operation must authenticate")
        assertTrue(main.contains("require("), "operation must validate inputs")
        assertTrue(main.contains("import lib.ft4.auth;"))
        // The four-step creed's steps 2 and 4 must be executable code, not prose:
        // an explicit ownership check and a value-moving op keyed off the caller.
        assertTrue(
            main.contains("require(n.owner == account.id"),
            "template must ship a concrete authorization check"
        )
        // The DEFAULT handler an agent copies must require the Transfer flag;
        // flags = [] may appear only as a per-operation, scoped exception for
        // operations that move no value (adversary round 2: an empty DEFAULT is
        // the hole that ships, because defaults are what people copy).
        val squashed = main.replace(Regex("\\s+"), "")
        assertTrue(
            squashed.contains("auth.add_auth_handler(flags=[\"T\"])"),
            "the default (unscoped) auth handler must require the Transfer flag"
        )
        assertTrue(
            !Regex("add_auth_handler\\(flags=\\[\\]\\)").containsMatchIn(squashed),
            "flags = [] must never appear unscoped"
        )
        assertTrue(
            squashed.contains("scope=\"add_note\",flags=[]"),
            "the no-value note demo keeps its scoped flags = [] exception"
        )
        assertTrue(
            !main.contains("scope = \"transfer\""),
            "transfer must ride the [\"T\"] default, not a scoped exception"
        )
        val test = files.getValue("src/test/main_test.rell")
        listOf(
            "test_transfer_conserves_total_points",
            "test_overdraft_must_fail",
            "test_non_owner_cannot_delete_note",
            "run_must_fail"
        ).forEach { needle ->
            assertTrue(test.contains(needle), "shipped test module must contain $needle")
        }
        assertFalse(
            test.contains("assert_equals(1, 1)"),
            "the placeholder test that passed without exercising anything must be gone"
        )
        DappScaffold.forbiddenModules.forEach { banned ->
            files.forEach { (path, content) ->
                // Sole exception: chromia.yml CONFIGURES lib.ft4.core.admin under
                // test.moduleArgs (configuring is not importing - Round 2 D3);
                // FT4's test helpers cannot start a test chain without it.
                if (path == "chromia.yml" && banned == "lib.ft4.core.admin") return@forEach
                assertFalse(content.contains(banned), "$path must not reference $banned")
            }
        }
        // The admin key must be TEST-SCOPED (under the top-level test: block,
        // never under blockchains.<name>), appear exactly once, and be FT4's
        // PUBLISHED test key - never a real credential.
        val ymlText = files.getValue("chromia.yml")
        val adminIdx = ymlText.indexOf("lib.ft4.core.admin")
        assertTrue(adminIdx >= 0, "chromia.yml must configure lib.ft4.core.admin under test.moduleArgs for chr test")
        val testBlockIdx = ymlText.indexOf("\ntest:")
        assertTrue(testBlockIdx in 0 until adminIdx, "admin args must sit under the top-level test: block")
        assertEquals(adminIdx, ymlText.lastIndexOf("lib.ft4.core.admin"), "admin must be configured exactly once")
        assertTrue(
            ymlText.contains(DappScaffold.TEST_ADMIN_PUBKEY),
            "admin_pubkey must be FT4's published test key, never a fresh credential"
        )
        val yml = files.getValue("chromia.yml")
        assertTrue(yml.contains("merkle_hash_version: 2"))
        assertTrue(yml.contains("rellVersion: ${DappScaffold.RELL_VERSION}"))
        assertTrue(yml.contains("tagOrBranch: ${DappScaffold.FT4_VERSION}"))
        assertTrue(yml.contains("rate_limit"), "module_args must come from Ft4ModuleArgs")
    }

    @Test
    fun ft4TemplateSecurityPassIsClean() {
        // The golden template's own operation must satisfy rell_security_check's
        // static rules (auth marker + require validation, no banned modules).
        val main = DappScaffold.files("notes", template = "ft4").getValue("src/main.rell")
        val result = org.chromia.tools.RellSecurityCheck.analyze(mapOf("main.rell" to main))
        assertTrue(
            result.findings.none { it.severity == "CRITICAL" || it.severity == "HIGH" },
            result.findings.toString()
        )
    }

    @Test
    fun helloTemplateUnchangedByDefault() {
        assertEquals(DappScaffold.files("hello"), DappScaffold.files("hello", template = "hello"))
        assertEquals(
            setOf("chromia.yml", "src/main.rell", "src/test/main_test.rell"),
            DappScaffold.files("hello").keys
        )
    }

    @Test
    fun ft4TemplateCompilesWithVendoredLib() {
        // The vendored FT4 v1.1.0r sources must let the golden template compile
        // in-process - no chr install. This is the core agent loop for real dapps.
        val main = DappScaffold.files("notes", template = "ft4").getValue("src/main.rell")
        val compile = org.chromia.tools.RellCheck.check(mapOf("main.rell" to main), null)
        assertTrue(compile.ok, "ft4 template must compile with vendored lib: ${compile.errors}")
    }

    @Test
    fun helloScaffoldPassesItsOwnToolchain() {
        // The product promise end-to-end: what scaffold_dapp emits must compile
        // (rell_check) and pass its own test (run_rell_tests) with no edits.
        val rell = DappScaffold.files("journey")
            .filterKeys { it.endsWith(".rell") }
            .mapKeys { (path, _) -> path.removePrefix("src/") }
        val compile = org.chromia.tools.RellCheck.check(rell, null)
        assertTrue(compile.ok, "scaffold must compile: ${compile.errors}")
        // The hello template's `object` is database-backed state, so without
        // PostgreSQL its test may only fail with dbRequired - never a logic error.
        val tests = org.chromia.tools.RunRellTests.run(rell, databaseUrl = System.getenv(org.chromia.tools.RunRellTests.DATABASE_URL_ENV))
        assertTrue(
            tests.cases.isNotEmpty() && tests.cases.all { it.ok || it.dbRequired },
            "scaffold tests must pass or be db-limited: ${tests.notes} ${tests.cases}"
        )
    }

    @Test
    fun ft4ScaffoldInvariantTestsRunGreen() {
        // The shipped invariant tests are real: they register FT4 test accounts,
        // sign operations, and must PASS through the same runner run_rell_tests
        // uses. Without a database they may only fail as dbRequired - with
        // CHROMIA_TEST_DATABASE_URL set (local-test-env.properties), every case
        // must actually run and pass; a fake-green placeholder cannot satisfy this.
        val rell = DappScaffold.files("notes", template = "ft4")
            .filterKeys { it.endsWith(".rell") }
            .mapKeys { (path, _) -> path.removePrefix("src/") }
        val dbUrl = System.getenv(org.chromia.tools.RunRellTests.DATABASE_URL_ENV)
        // FT4 cannot initialize without its module_args: run_rell_tests takes
        // them as a PARAMETER and does not read the generated chromia.yml, so
        // omitting them fails every case with an opaque "Unable to create GTX
        // module". Pass the same args the scaffold writes into that yml - this
        // is exactly what an agent must do, and the scaffold notes say so.
        val tests = org.chromia.tools.RunRellTests.run(
            rell,
            databaseUrl = dbUrl,
            moduleArgs = DappScaffold.ft4TestModuleArgs()
        )
        assertEquals(3, tests.total, "all three shipped invariant tests must be discovered: ${tests.cases}")
        if (dbUrl != null) {
            assertTrue(tests.ok, "shipped invariant tests must pass against the database: ${tests.notes} ${tests.cases}")
        } else {
            assertTrue(
                tests.cases.all { it.ok || it.dbRequired },
                "without a database the shipped tests may only be db-limited: ${tests.notes} ${tests.cases}"
            )
        }
    }

    @Test
    fun toJsonCarriesTemplateField() {
        val json = DappScaffold.toJson("notes", template = "ft4")
        assertEquals("ft4", json.getValue("template").toString().trim('"'))
    }
}
