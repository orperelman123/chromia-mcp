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
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.io.File

/**
 * ROUND 17, verify_guards at the SHAPE RECOGNISER.
 *
 * Round 16 replaced five rounds of error-text heuristics with two canonical
 * TEST SHAPES, each requiring the guard's declaration to be invoked in EXACTLY
 * ONE top-level statement of the named test, and answers `ambiguous_refusal`
 * for everything else. Both shapes rest on one claim: that the scan sees every
 * invocation the test makes and every operation the statement's transaction
 * carries.
 *
 * This class drives the SAME sixteen file maps round 17 measured on a real
 * chain (`exploit-corpus/realworld/adversary-round17/vg/probes.json`, baseline
 * green and mutant red for every one of them) through the real tool, and pins
 * each to the TRUE verdict. Every probe ships a CONTROL one file boundary, one
 * alias or one nesting level away, and each pair is written in the `src/`
 * layout `scaffold_dapp` produces AND in a flat one, because round 16 showed a
 * suite with one layout is blind.
 *
 * It is a recorder as well as a scoreboard: every raw verdict is written to
 * `build/adversary-round17/vg/<probe>.tool.json` before anything is asserted,
 * so a red run still leaves the evidence behind. The committed
 * `vg/<probe>.tool.json` files are FROZEN evidence (see [Round17Evidence]): the
 * run asserts its fresh recording equals the committed one value by value, so a
 * verdict that drifts is a regression reported with both values, not a silent
 * rewrite of the file the round README cites.
 */
class Round17VerifyGuardsProbeTest {

    // The real repository pointed at a closed port: verify_guards never reaches
    // the network, and if a probe ever did, it fails loudly instead of being
    // handed an invented answer (the recording double this used to take is gone).
    private val repo = McpTestSupport.offlineRepository()

    private val dir = File(Round17Evidence.committedRoot, "vg")

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
    fun `round 17 shape probes get the true verdict out of verify_guards`() {
        assertNotNull(
            System.getenv(RunRellTests.DATABASE_URL_ENV),
            "verify_guards runs real tests and needs ${RunRellTests.DATABASE_URL_ENV}"
        )
        assertTrue(dir.isDirectory, "probe fixtures missing at ${dir.absolutePath}")
        val wrong = mutableListOf<String>()
        val recordings = mutableListOf<Pair<String, JsonObject>>()
        for (element in probes()) {
            val spec = element.jsonObject
            val name = spec["probe"]!!.jsonPrimitive.content
            val truth = spec["true_verdict"]!!.jsonPrimitive.content
            val out = runProbe(spec)
            val one = out["result"]!!.jsonObject
            val verdict = one["verdict"]!!.jsonPrimitive.content
            val record = buildJsonObject {
                put("probe", name)
                put("direction", spec["direction"]!!)
                put("true_verdict", truth)
                put("tool_verdict", verdict)
                put("tool_ok", out["ok"]!!)
                put("loadBearing", one["loadBearing"]!!)
                put("evidence", one["evidence"]!!)
                put("why", spec["why"]!!)
                put("false_verdict", verdict != truth)
            }
            val relative = "vg/$name.tool.json"
            Round17Evidence.record(relative, record)
            recordings += relative to record
            if (verdict != truth) {
                wrong += "$name (${spec["direction"]!!.jsonPrimitive.content}): verify_guards said " +
                    "`$verdict` (loadBearing=${one["loadBearing"]!!.jsonPrimitive.content}), the truth is " +
                    "`$truth` - ${spec["why"]!!.jsonPrimitive.content}. Evidence: " +
                    one["evidence"]!!.jsonPrimitive.content
            }
        }
        assertAll(
            listOf(
                Executable {
                    assertEquals(
                        emptyList<String>(),
                        wrong,
                        "verify_guards gave ${wrong.size} verdict(s) that are not the truth:\n" +
                            wrong.joinToString("\n\n")
                    )
                }
            ) + recordings.map { (relative, record) ->
                Executable { Round17Evidence.assertFrozen(relative, record) }
            }
        )
    }
}
