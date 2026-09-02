package org.chromia

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import org.chromia.tools.RellCheck
import org.chromia.tools.RunRellTests
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * QA finding (2026-09-02): "an app module declaring `struct module_args`
 * combined with a tests/module.rell test module fails with `Module 'tests' is
 * not a test module`". Root cause (root-caused here, not a layout or
 * module_args problem): the QA probe named its module_args field `limit`, a
 * reserved at-expression keyword, so `app.limit()` in the TEST file is a syntax
 * error - and Rell 0.16.7 drops the AST of a @test file that fails to parse,
 * loses the `@test module;` header with it, and reports the module as "not a
 * test module" instead of the syntax error. Every layout fails that way, and
 * every layout passes with a legal name.
 *
 * Two pins: (1) the full layout matrix - {file, directory} app x {file,
 * directory} test x {no module_args, declared+passed, declared-not-passed} x
 * {pure body, body reading chain_context.args} - runs green; (2) the QA shape
 * with the keyword surfaces the real syntax error (file, line, "Syntax error")
 * plus an explanation of the compiler's misleading verdict, through both
 * run_rell_tests and rell_check.
 */
class ModuleArgsLayoutMatrixTest {

    enum class AppLayout { FILE, DIR }
    enum class TestLayout { FILE, DIR }
    enum class Args { NONE, DECLARED_AND_PASSED, DECLARED_NOT_PASSED }
    enum class Body { PURE, CHAIN_ARGS }

    data class Layout(val app: AppLayout, val test: TestLayout, val args: Args, val body: Body) {
        override fun toString() = "app=$app test=$test args=$args body=$body"
    }

    companion object {
        @JvmStatic
        fun layouts(): List<Layout> =
            AppLayout.values().flatMap { a ->
                TestLayout.values().flatMap { t ->
                    Args.values().flatMap { m ->
                        Body.values().map { b -> Layout(a, t, m, b) }
                    }
                }
            }.filter { it.body == Body.PURE || it.args != Args.NONE } // chain_context.args needs the struct

        /** The four physical layouts, for the keyword-shape pin. */
        @JvmStatic
        fun physicalLayouts(): List<Layout> =
            AppLayout.values().flatMap { a ->
                TestLayout.values().map { t -> Layout(a, t, Args.DECLARED_AND_PASSED, Body.CHAIN_ARGS) }
            }

        /** [field] is the module_args field / accessor name: `cap` is legal, `limit` is a reserved word. */
        fun files(layout: Layout, field: String = "cap"): Map<String, String> {
            val decl = if (layout.args == Args.NONE) "" else "struct module_args { $field: integer; }\n"
            val body = when (layout.body) {
                Body.PURE -> "function $field(): integer = 7;\n"
                Body.CHAIN_ARGS -> "function $field(): integer = chain_context.args.$field;\n"
            }
            val testBody = "@test module;\nimport app;\nfunction test_$field() { assert_equals(app.$field(), 7); }\n"
            val app = when (layout.app) {
                AppLayout.FILE -> mapOf("app.rell" to "module;\n$decl$body")
                AppLayout.DIR -> mapOf("app/module.rell" to "module;\n$decl", "app/logic.rell" to body)
            }
            val test = when (layout.test) {
                TestLayout.FILE -> mapOf("app_test.rell" to testBody)
                TestLayout.DIR -> mapOf("tests/module.rell" to testBody)
            }
            return app + test
        }

        fun moduleArgs(layout: Layout, field: String = "cap"): Map<String, Map<String, JsonElement>> =
            if (layout.args == Args.DECLARED_AND_PASSED) mapOf("app" to mapOf(field to JsonPrimitive(7)))
            else emptyMap()

        fun testFile(layout: Layout) = if (layout.test == TestLayout.FILE) "app_test.rell" else "tests/module.rell"
        fun testModule(layout: Layout) = if (layout.test == TestLayout.FILE) "app_test" else "tests"
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("layouts")
    fun everyLayoutRunsItsTest(layout: Layout) {
        val result = try {
            RunRellTests.run(files(layout), databaseUrl = null, moduleArgs = moduleArgs(layout))
        } catch (e: IllegalArgumentException) {
            throw AssertionError("$layout -> run_rell_tests rejected the sources: ${e.message}", e)
        }
        if (layout.args == Args.DECLARED_NOT_PASSED && layout.body == Body.CHAIN_ARGS) {
            // Reading chain_context.args without passing module_args is a real
            // runtime failure and must stay one - but of the test case, with the
            // module named, never a compile-time layout accusation.
            assertFalse(result.ok, "$layout -> ${result.notes} ${result.cases}")
            assertEquals(1, result.total, "$layout -> ${result.notes} ${result.cases}")
            assertTrue(
                result.cases.single().error?.contains("No module args for module 'app'") == true,
                "$layout -> ${result.cases}"
            )
            return
        }
        assertTrue(result.ok, "$layout -> ${result.notes} ${result.cases}")
        assertEquals(1, result.passed, "$layout -> ${result.notes} ${result.cases}")
    }

    @ParameterizedTest(name = "keyword shape {0}")
    @MethodSource("physicalLayouts")
    fun keywordInTestFileReportsTheSyntaxErrorThroughRunRellTests(layout: Layout) {
        val e = assertThrows<IllegalArgumentException> {
            RunRellTests.run(files(layout, "limit"), databaseUrl = null, moduleArgs = moduleArgs(layout, "limit"))
        }
        val message = e.message ?: ""
        // The real diagnostic: the test file, its position, and that it is a syntax error.
        assertTrue(message.contains("${testFile(layout)}(3:"), "$layout -> $message")
        assertTrue(message.contains("Syntax error", ignoreCase = true), "$layout -> $message")
        // The app side's `limit` field is a syntax error too - same round, not the next one.
        val appFile = if (layout.app == AppLayout.FILE) "app.rell" else "app/module.rell"
        assertTrue(message.contains("$appFile(2:"), "$layout -> $message")
        // The compiler's verdict is explained, not left as a bare layout accusation.
        assertTrue(message.contains("Module '${testModule(layout)}' is not a test module"), "$layout -> $message")
        assertTrue(message.contains("failed to parse"), "$layout -> $message")
        assertTrue(message.contains("the module layout is fine"), "$layout -> $message")
        assertFalse(message.trim().endsWith("is not a test module"), "$layout -> bare verdict: $message")
    }

    @ParameterizedTest(name = "keyword shape {0}")
    @MethodSource("physicalLayouts")
    fun keywordInTestFileReportsTheSyntaxErrorThroughRellCheck(layout: Layout) {
        val result = RellCheck.check(files(layout, "limit"), null)
        assertFalse(result.ok, result.toString())
        val syntax = result.errors.filter { it.severity == "ERROR" && it.text.contains("Syntax error", ignoreCase = true) }
        // The test file's error is reported with its position; the app side's
        // `limit` field is a syntax error too and is reported in the same round.
        assertTrue(syntax.any { it.file == testFile(layout) && it.line == 3 }, "$layout -> ${result.errors}")
        val appFile = if (layout.app == AppLayout.FILE) "app.rell" else "app/module.rell"
        assertTrue(syntax.any { it.file == appFile && it.line == 2 }, "$layout -> ${result.errors}")
        assertTrue(result.notes.contains("Module '${testModule(layout)}' is not a test module"), "$layout -> ${result.notes}")
        assertTrue(result.notes.contains("failed to parse"), "$layout -> ${result.notes}")
    }

    /** An app-side syntax error was never masked - it must still come through unchanged, with no test-module hint. */
    @Test
    fun appSideSyntaxErrorIsReportedAsBefore() {
        val files = mapOf(
            "app/module.rell" to "module;\n",
            "app/logic.rell" to "function cap(): integer = ;\n",
            "tests/module.rell" to "@test module;\nimport app;\nfunction test_cap() { assert_equals(app.cap(), 7); }\n"
        )
        val e = assertThrows<IllegalArgumentException> { RunRellTests.run(files, databaseUrl = null) }
        val message = e.message ?: ""
        assertTrue(message.contains("app/logic.rell(1:27) ERROR: Syntax error"), message)
        assertFalse(message.contains("is not a test module"), message)
        val check = RellCheck.check(files, null)
        assertFalse(check.ok)
        assertEquals("app/logic.rell", check.errors.first().file, check.errors.toString())
        assertFalse(check.notes.contains("is not a test module"), check.notes)
    }
}
