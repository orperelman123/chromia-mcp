package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.RellSecurityCheck
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * ROUND 16, `rell_security_check`. Four rules were rebuilt after round 15 - the
 * timestamp exclusion per (entity, field) with pure clock arithmetic, the
 * lower-bound requirement on `unbounded-voting-period`, the relative floor on
 * `majority-without-quorum`, and reading the RETURN expression for
 * `query-returns-secret-data`. Round 16 evaded three of them with a one-token
 * change and found the fourth shape missed outright, and this class recorded
 * what the analyzer said over the eight samples (four attack/control PAIRS, one
 * variable apart) into `realworld/adversary-round16/seccheck/raw.json`, so the
 * CORPUS rows that pinned them were written from a measurement.
 *
 * ON THIS BRANCH IT IS THE OTHER HALF OF THAT. `raw.json` is FROZEN: it is
 * round 16's measurement of the analyzer as the round found it, cited line by
 * line in the round README, and a recorder that overwrites its own evidence on
 * every build destroys the only record of what was wrong. So the run writes
 * `raw-after-fix.json` beside it and ASSERTS the true verdict for every sample -
 * all eight caught, each by the rule its CORPUS row names. The fixes themselves
 * are pinned structurally in [Round16SecurityRuleFixTest] and against renames in
 * [RuleRenameInvarianceTest]; this class is the round's own eight files,
 * asserted as a set.
 */
class Round16SecurityRuleProbeTest {

    private val samples = File("src/test/resources/exploit-corpus/samples")
    private val out = File("src/test/resources/exploit-corpus/realworld/adversary-round16/seccheck")

    /** Every round-16 seccheck sample with the rule that must catch it now. */
    private val expected = listOf(
        "r16-clock-mint-declared-as-a-timestamp" to "unbacked-conversion-credit",
        "r16-clock-mint-control-declared-as-an-integer" to "unbacked-conversion-credit",
        "r16-voting-floor-is-a-constant-zero" to "unbounded-voting-period",
        "r16-voting-floor-control-the-same-zero-inline" to "unbounded-voting-period",
        "r16-quorum-floor-written-by-the-proposer" to "majority-without-quorum",
        "r16-quorum-floor-control-a-literal-two" to "majority-without-quorum",
        "r16-secret-published-through-an-accumulator" to "query-returns-secret-data",
        "r16-secret-control-returned-as-a-projection" to "query-returns-secret-data"
    )

    @Test
    fun `every round 16 sample and its control is caught by the rule that should catch it`() {
        out.mkdirs()
        val lines = mutableListOf<String>()
        val misses = mutableListOf<String>()
        val rows = buildJsonArray {
            for ((id, rule) in expected) {
                val dir = File(samples, id)
                assertTrue(dir.isDirectory, "missing sample " + dir.absolutePath)
                val rellFiles = dir.walkTopDown().filter { it.isFile && it.name.endsWith(".rell") }.toList()
                assertTrue(rellFiles.isNotEmpty(), id + " has no .rell files")
                val fileMap = rellFiles.associate { f -> f.name to f.readText() }
                val result = RellSecurityCheck.analyze(fileMap)
                val summary = result.findings.joinToString("; ") { it.severity + "/" + it.rule }
                    .ifEmpty { "no findings" }
                lines += "%-52s ok=%-6s %s".format(id, result.ok, summary)
                if (result.findings.none { it.rule == rule }) {
                    misses += "$id: expected $rule, got $summary"
                }
                add(
                    buildJsonObject {
                        put("sample", id)
                        put("expectedRule", rule)
                        put("ok", result.ok)
                        put("findingCount", result.findings.size)
                        put(
                            "findings",
                            buildJsonArray {
                                result.findings.forEach { f ->
                                    add(
                                        buildJsonObject {
                                            put("severity", f.severity)
                                            put("rule", f.rule)
                                            put("line", f.line)
                                            put("text", f.text)
                                        }
                                    )
                                }
                            }
                        )
                    }
                )
            }
        }
        File(out, "raw-after-fix.json").writeText(
            Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), rows)
        )
        println("ROUND16-SECCHECK\n" + lines.joinToString("\n"))
        assertTrue(
            misses.isEmpty(),
            "round 16 evaded three of the four rebuilt rules with one token each and missed a fourth " +
                "shape outright. Each sample must now draw the rule its CORPUS row names:\n" +
                misses.joinToString("\n")
        )
    }
}
