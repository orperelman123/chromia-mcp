package org.chromia

import org.chromia.tools.RellCheck
import org.chromia.tools.RellSecurityCheck
import org.chromia.tools.RunRellTests
import org.chromia.tools.maskRellSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertTimeoutPreemptively
import java.time.Duration
import java.util.Random

/**
 * Property-based fuzzing of the Rell tool surface: whatever bytes an agent
 * throws at these tools, they must return a structured result or a clean
 * validation error - never crash, never hang. Seeded PRNG = fully
 * deterministic, so a failure is always reproducible and never flaky.
 */
class RellToolsFuzzTest {

    private val seed = 20260831L

    private val nastyCorpus = listOf(
        "",
        "\n\n\n",
        "module;",
        "module", // missing semicolon
        "operation x(", // unterminated
        "operation x() {", // unbalanced
        "operation x() { val s = \"}\"; }",
        "operation x(p: text = \"(\") { }",
        "/* @test module */ module;",
        "// operation ghost() { create x(); }",
        "module;\nval s = 'unterminated",
        "module;\nval s = \"unterminated",
        "module;\n/* unterminated block comment",
        "module;\nquery q() = \"\\u00e9\\n\\t\\\\\";",
        "module;\n" + "entity e { k: text; }\n".repeat(50),
        "module;\noperation " + "a".repeat(500) + "() { }",
        "module;\nquery q() = " + "(".repeat(80) + "1" + ")".repeat(80) + ";",
        "module;\né世界 operation café() { }",
        "{}{}{}((()))\"'\"'//**//*",
        "module;\noperation deep() { " + "if (true) { ".repeat(60) + "print(1);" + " }".repeat(60) + " }"
    )

    private fun randomChunk(rnd: Random): String {
        val pieces = listOf(
            "module;\n", "entity e${rnd.nextInt(9)} { key k: text; v: integer; }\n",
            "object o${rnd.nextInt(9)} { mutable n: integer = 0; }\n",
            "function f${rnd.nextInt(9)}(x: integer): integer = x + ${rnd.nextInt(100)};\n",
            "operation op${rnd.nextInt(9)}(t: text) { require(t != \"\", \"empty\"); create e0(t, ${rnd.nextInt(9)}); }\n",
            "query q${rnd.nextInt(9)}() = ${rnd.nextInt(100)};\n",
            "// comment with } and \" and update \n",
            "/* block { comment */\n",
            "val s${rnd.nextInt(9)} = \"str with { } ( ) // /* update delete \";\n",
            "import lib.ft4.auth;\n"
        )
        return pieces[rnd.nextInt(pieces.size)]
    }

    private fun randomProgram(rnd: Random): String {
        val sb = StringBuilder("module;\n")
        repeat(1 + rnd.nextInt(8)) { sb.append(randomChunk(rnd)) }
        return sb.toString()
    }

    private fun mutate(rnd: Random, source: String): String {
        if (source.isEmpty()) return source
        return when (rnd.nextInt(4)) {
            0 -> { val i = rnd.nextInt(source.length); source.removeRange(i, minOf(source.length, i + 1 + rnd.nextInt(5))) }
            1 -> { val i = rnd.nextInt(source.length); source.substring(0, i) + "\"'{}/*" [rnd.nextInt(6)] + source.substring(i) }
            2 -> { val i = rnd.nextInt(source.length); source.substring(0, i) + source }
            else -> source.reversed()
        }
    }

    @Test
    fun maskingPreservesLineStructureAndNeverThrows() {
        val rnd = Random(seed)
        val inputs = nastyCorpus + (1..150).map { mutate(rnd, randomProgram(rnd)) }
        for (input in inputs) {
            for (maskStrings in listOf(true, false)) {
                val masked = maskRellSource(input, maskStrings)
                assertEquals(
                    input.count { it == '\n' }, masked.count { it == '\n' },
                    "newline count must survive masking for input: ${input.take(60)}"
                )
            }
        }
    }

    @Test
    fun securityAnalyzeNeverThrowsOnArbitraryInput() {
        val rnd = Random(seed + 1)
        val inputs = nastyCorpus + (1..120).map { mutate(rnd, randomProgram(rnd)) }
        assertTimeoutPreemptively(Duration.ofSeconds(60)) {
            for (input in inputs) {
                val result = RellSecurityCheck.analyze(mapOf("fuzz.rell" to input))
                assertNotNull(result.notes)
            }
        }
    }

    @Test
    fun rellCheckReturnsStructuredResultForArbitraryInput() {
        val rnd = Random(seed + 2)
        val inputs = nastyCorpus.take(12) + (1..18).map { mutate(rnd, randomProgram(rnd)) }
        assertTimeoutPreemptively(Duration.ofMinutes(4)) {
            for (input in inputs) {
                val result = runCatching { RellCheck.check(mapOf("fuzz.rell" to input), null) }
                // Either a structured result (ok or errors) or a clean validation error.
                result.fold(
                    onSuccess = { r -> assertTrue(r.ok || r.errors.isNotEmpty(), "not-ok result must carry errors: ${input.take(60)}") },
                    onFailure = { e ->
                        assertTrue(e is IllegalArgumentException, "unexpected ${e::class.simpleName}: ${e.message} for input: ${input.take(60)}")
                    }
                )
            }
        }
    }

    @Test
    fun moduleNameDerivationNeverThrows() {
        val rnd = Random(seed + 3)
        val paths = listOf(
            "", ".", "..", "a", "a.rell", "module.rell", "a/b/c.rell", "a\\b\\module.rell",
            "./a.rell", "a//b.rell", "é/世.rell", "a.b.c.rell", "a/.rell", ".rell"
        ) + (1..100).map { (1..rnd.nextInt(5) + 1).joinToString("/") { "s${rnd.nextInt(10)}" } + ".rell" }
        for (p in paths) {
            assertNotNull(RunRellTests.moduleNameForPath(p), "path: $p")
        }
    }

    @Test
    fun runRellTestsCleanlyRejectsOrRunsArbitraryTestSources() {
        val rnd = Random(seed + 4)
        val inputs = (1..8).map { mutate(rnd, "@test module;\nfunction test_a() { assert_equals(1, 1); }\n" + randomChunk(rnd)) }
        assertTimeoutPreemptively(Duration.ofMinutes(4)) {
            for (input in inputs) {
                val result = runCatching { RunRellTests.run(mapOf("fuzz_test.rell" to input), databaseUrl = null) }
                result.fold(
                    onSuccess = { r -> assertNotNull(r.notes) },
                    onFailure = { e ->
                        assertTrue(e is IllegalArgumentException, "unexpected ${e::class.simpleName}: ${e.message} for input: ${input.take(60)}")
                    }
                )
            }
        }
    }
}
