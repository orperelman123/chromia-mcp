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
 * `query-returns-secret-data`. This recorder runs the analyzer over the eight
 * round-16 samples (four attack/control PAIRS, one variable apart) and writes
 * what it said to `realworld/adversary-round16/seccheck/raw.json`, so the CORPUS
 * rows that pin them are written from a measurement rather than a prediction.
 */
class Round16SecurityRuleProbeTest {

    private val samples = File("src/test/resources/exploit-corpus/samples")
    private val out = File("src/test/resources/exploit-corpus/realworld/adversary-round16/seccheck")

    private val ids = listOf(
        "r16-clock-mint-declared-as-a-timestamp",
        "r16-clock-mint-control-declared-as-an-integer",
        "r16-voting-floor-is-a-constant-zero",
        "r16-voting-floor-control-the-same-zero-inline",
        "r16-quorum-floor-written-by-the-proposer",
        "r16-quorum-floor-control-a-literal-two",
        "r16-secret-published-through-an-accumulator",
        "r16-secret-control-returned-as-a-projection"
    )

    @Test
    fun `record what the four rebuilt rules say about the round 16 samples`() {
        out.mkdirs()
        val lines = mutableListOf<String>()
        val rows = buildJsonArray {
            for (id in ids) {
                val dir = File(samples, id)
                assertTrue(dir.isDirectory, "missing sample " + dir.absolutePath)
                val rellFiles = dir.walkTopDown().filter { it.isFile && it.name.endsWith(".rell") }.toList()
                assertTrue(rellFiles.isNotEmpty(), id + " has no .rell files")
                val fileMap = rellFiles.associate { f -> f.name to f.readText() }
                val result = RellSecurityCheck.analyze(fileMap)
                lines += "%-52s ok=%-6s %s".format(
                    id,
                    result.ok,
                    result.findings.joinToString("; ") { it.severity + "/" + it.rule }
                        .ifEmpty { "no findings" }
                )
                add(
                    buildJsonObject {
                        put("sample", id)
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
        File(out, "raw.json").writeText(
            Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), rows)
        )
        println("ROUND16-SECCHECK\n" + lines.joinToString("\n"))
    }
}
