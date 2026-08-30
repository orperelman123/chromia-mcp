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
        DappScaffold.forbiddenModules.forEach { banned ->
            files.values.forEach { content ->
                assertFalse(content.contains(banned), "template must not reference $banned")
            }
        }
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
    fun toJsonCarriesTemplateField() {
        val json = DappScaffold.toJson("notes", template = "ft4")
        assertEquals("ft4", json.getValue("template").toString().trim('"'))
    }
}
