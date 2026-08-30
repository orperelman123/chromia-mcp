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
    fun toJsonCarriesTemplateField() {
        val json = DappScaffold.toJson("notes", template = "ft4")
        assertEquals("ft4", json.getValue("template").toString().trim('"'))
    }
}
