package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.RunRellTestsStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Regression tests for the adversarial audit findings (2026-08-31). */
class RellToolsAdversarialTest {

    // --- RellSecurityCheck: masked scanning ---

    @Test
    fun closingBraceInStringCannotHideMutation() {
        val r = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nentity item { key k: text; }\noperation evil(x: text) { val marker = \"}\"; create item(x); }\n")
        )
        assertTrue(r.findings.any { it.rule == "unauthenticated-mutation" }, r.findings.toString())
    }

    @Test
    fun parenInDefaultValueDoesNotHideOperation() {
        val r = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nentity item { key k: text; }\noperation with_default(marker: text = \"(\") { create item(marker); }\n")
        )
        assertEquals(1, r.operationsScanned)
        assertTrue(r.findings.any { it.rule == "unauthenticated-mutation" }, r.findings.toString())
    }

    @Test
    fun authMarkerInCommentDoesNotCountAsAuth() {
        val r = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\nentity b { key k: text; }\noperation drain(k: text) {\n    // TODO: add auth.authenticate() here\n    delete b @* { .k == k };\n}\n")
        )
        assertTrue(r.findings.any { it.rule == "unauthenticated-mutation" }, r.findings.toString())
    }

    @Test
    fun mutationKeywordInStringIsNotAMutation() {
        val r = RellSecurityCheck.analyze(
            mapOf("main.rell" to "module;\noperation log_note() {\n    require(true, \"please update your settings\");\n    print(\"{\");\n}\n")
        )
        assertFalse(r.findings.any { it.rule == "unauthenticated-mutation" }, r.findings.toString())
    }

    @Test
    fun blockCommentsAreInvisibleToAllRules() {
        val r = RellSecurityCheck.analyze(
            mapOf(
                "main.rell" to "module;\n/*\nimport lib.ft4.admin;\noperation dead(x: text) { create item(x); }\nval k = x\"15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304\";\n*/\nquery ping() = \"pong\";\n"
            )
        )
        assertTrue(r.findings.isEmpty(), r.findings.toString())
        assertEquals(0, r.operationsScanned)
    }

    // --- RunRellTests: module naming and detection ---

    @Test
    fun moduleNameDerivation() {
        assertEquals("tests", RunRellTests.moduleNameForPath("tests/module.rell"))
        assertEquals("tests.helpers", RunRellTests.moduleNameForPath("tests\\helpers.rell"))
        assertEquals("lib_test", RunRellTests.moduleNameForPath("./lib_test.rell"))
        assertEquals("", RunRellTests.moduleNameForPath("module.rell"))
    }

    @Test
    fun directoryModuleTestLayoutRuns() {
        val r = RunRellTests.run(
            mapOf("tests/module.rell" to "@test module;\nfunction test_math() { assert_equals(2 + 2, 4); }")
        )
        assertTrue(r.ok, r.notes)
        assertEquals(1, r.passed)
    }

    @Test
    fun testHeaderWithTrailingCommentIsDetected() {
        val r = RunRellTests.run(
            mapOf("t.rell" to "@test // pinned note\nmodule;\nfunction test_x() { assert_equals(1, 1); }")
        )
        assertTrue(r.ok, r.notes)
    }

    @Test
    fun testMarkerInsideCommentIsNotATestModule() {
        val result = runBlocking {
            RunRellTestsStrategy().execute(
                CallToolRequest(
                    name = "run_rell_tests",
                    arguments = buildJsonObject {
                        put("files", buildJsonObject { put("main.rell", "module;\n/*\n@test module docs\n*/\nquery q() = 1;") })
                    }
                ),
                RecordingRepository()
            )
        }
        assertTrue(result.isError == true)
        assertTrue((result.content.first() as TextContent).text!!.contains("No @test modules"), (result.content.first() as TextContent).text)
    }

    @Test
    fun compileFailureReportsDiagnostics() {
        val result = runBlocking {
            RunRellTestsStrategy().execute(
                CallToolRequest(
                    name = "run_rell_tests",
                    arguments = buildJsonObject {
                        put("files", buildJsonObject { put("t.rell", "@test module;\nfunction test_x() { assert_equals(nope_undefined, 1); }") })
                    }
                ),
                RecordingRepository()
            )
        }
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("t.rell"), "diagnostics should carry file positions: $text")
    }
}
