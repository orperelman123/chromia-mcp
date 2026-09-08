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
 * ROUND 18, verify_guards ONE STEP PAST the round-17 fix.
 *
 * Round 17 closed ten false verdicts by widening WHERE a test-module helper may
 * live and HOW a name resolves - helpers from EVERY test module, calls resolved
 * through the calling module's own plain, wildcard and alias imports, and the
 * operations counted over the statement's whole call closure. All of that rests
 * on one model of Rell's import system (`IMPORT_REGEX`, `resolveHelper`) and one
 * model of how a transaction is built (`opRx`, which is `\.\s*op\s*\(`). Round 18
 * attacks both models with forms the language has and the models do not:
 *
 *  - `import tests.helpers.{ settle };` - a Rell EXACT IMPORT. IMPORT_REGEX
 *    matches none of it, so the calling module binds nothing the scan can see and
 *    the helper is invisible again: p18a hides a second OPERATION there (round
 *    17's p17d through a door it did not look at) and p18k hides a second
 *    INVOCATION (p17b through the same door).
 *  - `rell.test.tx(take(...), audit(...))` - a two-operation transaction with no
 *    `.op(` in it at all. `ops` counts 0, and 0 passes every branch of the shape
 *    check, so it reads as a canonical single-operation SHAPE A (p18b).
 *  - `import main: tests.helpers;` - an alias that SHADOWS the guard's own
 *    module name. `invocationsIn` tests the qualifier against the guard's module
 *    names BEFORE consulting the calling module's bindings, so one honest
 *    invocation is counted twice (p18c).
 *
 * Every probe was run on a REAL CHAIN first
 * (`exploit-corpus/realworld/adversary-round18/harness/vg_r18.py`, one schema per
 * probe on this worktree's own database): every baseline is green and every
 * mutant is red, so each is a valid mutant experiment before the tool is asked
 * anything. Every one ships a CONTROL one token away - the same dapp, the same
 * test, the same transaction, with the import or the transaction spelled the way
 * round 17 recognises - and the dangerous ones are written in BOTH layouts,
 * `src/` (what `scaffold_dapp` writes) and flat.
 *
 * This class drives the same file maps through the real tool - which runs its own
 * real tests, so nothing here is replayed - and pins each to the TRUE verdict. It
 * is a recorder as well as a scoreboard: every raw verdict is written to
 * `build/adversary-round18/vg/<probe>.tool.json` before anything is asserted, and
 * asserted equal to the FROZEN committed recording (see [Round18Evidence]), so a
 * verdict that drifts is a regression reported with both values.
 *
 * WHAT IT FOUND: seven of eighteen verdicts are not the truth - six DANGEROUS
 * (`still_refused` for a test whose shape the tool cannot actually read, whose
 * remedy sentence tells the author to weaken a test that is measuring exactly
 * what it says) and one CONSERVATIVE (`ambiguous_refusal` for an honest
 * single-invocation SHAPE B). No probe drew a false `ok:true`.
 */
class Round18VerifyGuardsProbeTest {

    // The real repository pointed at a closed port: verify_guards never reaches
    // the network, and if a probe ever did, it fails loudly instead of being
    // handed an invented answer.
    private val repo = McpTestSupport.offlineRepository()

    private val dir = File(Round18Evidence.committedRoot, "vg")

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
    fun `round 18 import and transaction probes get the true verdict out of verify_guards`() {
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
            Round18Evidence.record(relative, record)
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
                Executable { Round18Evidence.assertFrozen(relative, record) }
            }
        )
    }
}
