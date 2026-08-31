package org.chromia

import org.chromia.tools.RellCheck
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Audit finding (2026-08-31): rell_check reported ok=true while compiling ZERO
 * modules - a false green in the core agent loop. Two triggers: an empty scoped
 * module list (root module.rell + FT4 import), and projects made only of @test
 * modules. Both must now surface the real compile errors.
 */
class FalseGreenRegressionTest {

    private val broken = "function f() { zzz_undefined_symbol(); }"

    @Test
    fun rootModuleWithFt4ImportStillReportsErrors() {
        val result = RellCheck.check(
            mapOf("module.rell" to "module;\nimport lib.ft4.accounts;\n$broken"),
            null
        )
        assertFalse(result.ok, "broken code with an FT4 import must not report ok=true")
        assertTrue(result.errors.any { it.text.contains("zzz_undefined_symbol") }, result.errors.toString())
    }

    @Test
    fun rootModuleWithoutFt4StillReportsErrors() {
        val result = RellCheck.check(mapOf("module.rell" to "module;\n$broken"), null)
        assertFalse(result.ok, "broken root module must not report ok=true")
    }

    @Test
    fun testOnlyProjectIsCompiled() {
        val result = RellCheck.check(
            mapOf("tests/t.rell" to "@test module;\nfunction test_x() { zzz_undefined_symbol(); }"),
            null
        )
        assertFalse(result.ok, "@test modules must be compiled, not skipped")
        assertTrue(result.errors.any { it.text.contains("zzz_undefined_symbol") }, result.errors.toString())
    }

    @Test
    fun validTestOnlyProjectStillPasses() {
        val result = RellCheck.check(
            mapOf("tests/t.rell" to "@test module;\nfunction test_x() { assert_equals(1, 1); }"),
            null
        )
        assertTrue(result.ok, "valid test module must compile: ${result.errors}")
    }

    @Test
    fun validFt4CodeStillCompiles() {
        val result = RellCheck.check(
            mapOf("main.rell" to "module;\nimport lib.ft4.auth;\nquery ping() = \"pong\";"),
            null
        )
        assertTrue(result.ok, "valid FT4 code must still compile: ${result.errors}")
    }
}
