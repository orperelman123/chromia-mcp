package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.RunRellTests
import org.chromia.tools.VerifyGuardsStrategy
import org.chromia.tools.callToolRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * ROUND 16, verify_guards. Round 15 rebuilt step 4 of
 * [VerifyGuardsStrategy] on three joins - caller resolution through the
 * submission's call graph, the test's STATEMENT ORDER (with differential
 * truncation), and the attribution of a frame-less error to the query that owns
 * its words. This class drives the SAME eight file maps the round measured on a
 * real chain (`exploit-corpus/realworld/adversary-round16/vg/probes.json`,
 * baseline green and mutant red for every one of them) through the real tool,
 * and pins each to the TRUE verdict.
 *
 * It is a recorder as well as a scoreboard: every raw verdict is written back
 * to `vg/<probe>.tool.json` before anything is asserted, so a red run still
 * leaves the evidence behind.
 */
class Round16VerifyGuardsProbeTest {

    private val repo = RecordingRepository()

    private val dir = File("src/test/resources/exploit-corpus/realworld/adversary-round16/vg")

    private fun probes(): JsonArray =
        Json.parseToJsonElement(File(dir, "probes.json").readText()).jsonArray

    private fun runProbe(spec: JsonObject): JsonObject {
        val files = spec["files"]!!.jsonObject
        val result = runBlocking {
            VerifyGuardsStrategy().execute(
                callToolRequest(
                    name = "verify_guards",
                    arguments = buildJsonObject {
                        put("files", files)
                        put(
                            "guards",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("guard", spec["guard"]!!.jsonPrimitive.content)
                                        put("test", spec["test"]!!.jsonPrimitive.content)
                                        put("replacement", spec["replacement"]!!.jsonPrimitive.content)
                                    }
                                )
                            }
                        )
                    }
                ),
                repo
            )
        }
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
        val structured = result.structuredContent!!.jsonObject
        return buildJsonObject {
            put("ok", structured["ok"]!!)
            put("result", structured["results"]!!.jsonArray.single())
        }
    }

    @Test
    fun `round 16 probes get the true verdict out of verify_guards`() {
        assertNotNull(
            System.getenv(RunRellTests.DATABASE_URL_ENV),
            "verify_guards runs real tests and needs ${RunRellTests.DATABASE_URL_ENV}"
        )
        assertTrue(dir.isDirectory, "probe fixtures missing at ${dir.absolutePath}")
        val wrong = mutableListOf<String>()
        for (element in probes()) {
            val spec = element.jsonObject
            val name = spec["probe"]!!.jsonPrimitive.content
            val truth = spec["true_verdict"]!!.jsonPrimitive.content
            val out = runProbe(spec)
            val one = out["result"]!!.jsonObject
            val verdict = one["verdict"]!!.jsonPrimitive.content
            File(dir, "$name.tool.json").writeText(
                Json { prettyPrint = true }.encodeToString(
                    JsonObject.serializer(),
                    buildJsonObject {
                        put("probe", name)
                        put("true_verdict", truth)
                        put("tool_verdict", verdict)
                        put("tool_ok", out["ok"]!!)
                        put("loadBearing", one["loadBearing"]!!)
                        put("evidence", one["evidence"]!!)
                        put("why", spec["why"]!!)
                        put("false_verdict", verdict != truth)
                    }
                )
            )
            if (verdict != truth) {
                wrong += "$name: verify_guards said `$verdict` (loadBearing=" +
                    "${one["loadBearing"]!!.jsonPrimitive.content}), the truth is `$truth` - " +
                    "${spec["why"]!!.jsonPrimitive.content}. Evidence: ${one["evidence"]!!.jsonPrimitive.content}"
            }
        }
        assertEquals(
            emptyList<String>(),
            wrong,
            "verify_guards gave ${wrong.size} verdict(s) that are not the truth:\n" + wrong.joinToString("\n\n")
        )
    }
}
