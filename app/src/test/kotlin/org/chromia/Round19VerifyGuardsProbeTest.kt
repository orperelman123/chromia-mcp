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
 * ROUND 19, verify_guards ONE BUILDER AND ONE NAMESPACE PAST THE ROUND-18 FIX.
 *
 * Round 18 stopped modelling Rell with two regexes: imports are PARSED (plain,
 * aliased, wildcard, exact `.{ }`, relative `^.`/`.`) and a transaction's
 * operations are counted STRUCTURALLY over the statement's whole call closure.
 * Both models are still read off the TEST module's TEXT, and both still have to
 * answer one question - "what does this one statement run" - from it. Round 19
 * attacks exactly that question with forms the language has and the models do
 * not:
 *
 *  - `rell.test.block().tx(take(...), audit(...))` - ONE block holding ONE
 *    transaction of TWO operations. `transactionOperations` treats only
 *    `rell.test.tx(` and `.op(` as carriers, so it counts ZERO operations and
 *    reports `builds=false`; `blockTransactions` counts one `.tx(`, which is not
 *    `> 1`. Every branch of the shape check is skipped and the statement reads
 *    as a canonical single-operation shape. That is p18b - a second operation
 *    refusing ON THE DAMAGE reported as the attack being refused - one builder
 *    along (r19a, and r19a2 in the direction that CERTIFIES).
 *  - a test-module helper declared inside a `namespace` and called `h.audited(...)`.
 *    `resolveHelper` resolves a QUALIFIED call only through the calling module's
 *    IMPORT bindings, and a namespace is not an import, so the helper is never
 *    expanded and the `.op(audit(...))` inside it is never counted. That is p18a
 *    through a door the import parser does not open, because it is not a door in
 *    the import system at all (r19b).
 *  - an honest ONE-operation must-hold test whose operation is passed to a
 *    helper as a `rell.test.op` PARAMETER. The closure is flattened, the only
 *    carrier is `.op(o)`, `o` is not a call, and "an argument this scan cannot
 *    reduce is not zero operations" makes it `ambiguous_refusal` - the
 *    conservative direction, a correct test refused (r19c2). In SHAPE A the same
 *    test escapes through the `did not fail` shortcut, which is why r19c passes
 *    and r19c2 does not.
 *
 * Every probe was run on a REAL CHAIN first
 * (`exploit-corpus/realworld/adversary-round19/harness/vg_r19.py`, chr 0.29.10 /
 * rell 0.15.0, one schema per probe on this worktree's own database): every
 * baseline is green and every mutant is red, recorded in `vg/<probe>.chain.json`,
 * so each is a valid mutant experiment before the tool is asked anything. Every
 * dangerous probe ships a CONTROL one token away - the same dapp, the same test,
 * the same two operations, spelled the way round 18 recognises (r19f), and the
 * same block builder carrying ONE operation (r19h) - so a verdict that differs is
 * the round-19 input and nothing else.
 *
 * This class drives the same file maps through the real tool - which runs its own
 * real tests, so nothing here is replayed - and pins each to the TRUE verdict. It
 * is a recorder as well as a scoreboard: every raw verdict is written to
 * `build/adversary-round19/vg/<probe>.tool.json` before anything is asserted and
 * asserted equal to the FROZEN committed recording ([Round19Evidence]), so a
 * verdict that drifts is a regression reported with both values.
 *
 * WHAT IT FOUND: four of ten verdicts are not the truth - two DANGEROUS
 * (`still_refused`, whose remedy sentence sends the author to weaken a test that
 * measures exactly what it says), one that draws a false `ok:TRUE` (r19a2
 * certifies a guard on a transaction that never landed), and one CONSERVATIVE
 * (r19c2 refuses an honest single-operation shape B).
 */
class Round19VerifyGuardsProbeTest {

    // The real repository pointed at a closed port: verify_guards never reaches
    // the network, and if a probe ever did, it fails loudly instead of being
    // handed an invented answer.
    private val repo = McpTestSupport.offlineRepository()

    private val dir = File(Round19Evidence.committedRoot, "vg")

    private fun probes(file: String): JsonArray =
        Json.parseToJsonElement(File(dir, file).readText()).jsonArray

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
    fun `round 19 transaction builder and namespace probes get the true verdict out of verify_guards`() {
        driveEveryProbe("probes.json")
    }

    private fun driveEveryProbe(probesFile: String) {
        assertNotNull(
            System.getenv(RunRellTests.DATABASE_URL_ENV),
            "verify_guards runs real tests and needs ${RunRellTests.DATABASE_URL_ENV}"
        )
        assertTrue(dir.isDirectory, "probe fixtures missing at ${dir.absolutePath}")
        val wrong = mutableListOf<String>()
        val recordings = mutableListOf<Pair<String, JsonObject>>()
        for (element in probes(probesFile)) {
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
            Round19Evidence.record(relative, record)
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
                Executable { Round19Evidence.assertFrozen(relative, record) }
            }
        )
    }
}
