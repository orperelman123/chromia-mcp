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
 * ROUND 20, verify_guards ONE DECLARATION PAST THE ROUND-19 FIX.
 *
 * Round 19 stopped resolving a qualified helper call through imports alone. A
 * call now resolves through the CALLING module's own namespaces as well, and to
 * make that work every function a test file declares is registered under its
 * full namespace path AND under EVERY SUFFIX of it, the bare name included:
 *
 *     namespacedFunctions(maskedFile).forEach { d ->
 *         val segments = d.name.split('.')
 *         for (k in segments.indices) {
 *             val spelling = segments.drop(k).joinToString(".")
 *             fns.getOrPut(spelling) { mutableListOf() }.add(d.body)
 *
 * with the comment "A suffix can only WIDEN which body a name reaches, which
 * costs an ambiguous_refusal and never a false load_bearing." Round 20 attacks
 * both halves of that sentence, and both are wrong:
 *
 *  - A SUFFIX CAN REACH THE WRONG BODY. `namespace h { function audited(...) }`
 *    registers the BARE name `audited` in the calling module, and
 *    `resolveHelper`'s empty-qualifier branch tries `listOf(module) +
 *    exactImports + wildcards` IN THAT ORDER. A module that also holds
 *    `import tests.aux.{ audited };` - measured to compile, and measured to make
 *    a bare `audited(...)` name the IMPORTED one (`vg/spellings.json`, sp20a) -
 *    therefore has the tool reading the LOCAL no-op body while the chain runs
 *    the imported one, which adds a SECOND operation to the transaction. The
 *    statement reads as the canonical single-operation shape: `still_refused` in
 *    shape A (r20a) and `load_bearing` with ok:TRUE in shape B (r20a2), on a
 *    transaction that rolled back with nothing measured and nothing persisted.
 *  - AND A SUFFIX CAN REACH TWO BODIES. `helperBodies` is a LIST and `flatten`
 *    appends every body a name has, so a module holding a top-level
 *    `function run_one` and a `namespace h { function run_one }` (sp20b) has BOTH
 *    appended and an operation only the namespaced body adds is counted into a
 *    transaction the compiler never puts it in (r20c) - an honest one-operation
 *    must-hold test refused for carrying two.
 *
 * And the operation-returning helper round 19 left open: `run_it(make())` with
 * `function make(): rell.test.op = take("a", 11);` (sp20c). `bindParameters`
 * substitutes the argument `make()` - it IS a call and builds no transaction of
 * its own - so the carrier reads `.op(make())`, whose one operation NAMES
 * `make`, which is not a declaration the guard runs in. Shape A escapes through
 * the "did not fail" shortcut (r20b, clean); shape B has no shortcut (r20b2).
 *
 * Every probe was run on a REAL CHAIN first
 * (`exploit-corpus/realworld/adversary-round20/harness/vg_r20.py`, chr 0.29.10 /
 * rell 0.15.0, one schema per probe on this worktree's own database): every
 * baseline is green and every mutant is red, recorded in `vg/<probe>.chain.json`,
 * so each is a valid mutant experiment before the tool is asked anything. Every
 * Rell form any of them uses was put in front of the compiler on its own first
 * (`harness/spellings_r20.py`, `vg/spellings.json`).
 *
 * r20f and r20f2 are r20a and r20a2 with the five lines of `namespace h` DELETED
 * and nothing else changed; their chain errors are identical. So the difference
 * between `ambiguous_refusal` and a false `ok:true` is exactly one declaration.
 *
 * WHAT IT FOUND: four of ten verdicts are not the truth - two DANGEROUS (r20a
 * `still_refused`, whose remedy sentence sends the author to weaken a test that
 * measures exactly what it says, and r20a2 `load_bearing` with ok:TRUE) and two
 * CONSERVATIVE (r20b2 and r20c, honest single-operation tests refused). THIS
 * TEST IS RED UNTIL A FIX LANE LANDS; that is the round-19 shape, and the red is
 * the finding.
 */
class Round20VerifyGuardsProbeTest {

    // The real repository pointed at a closed port: verify_guards never reaches
    // the network, and if a probe ever did, it fails loudly instead of being
    // handed an invented answer.
    private val repo = McpTestSupport.offlineRepository()

    private val dir = File(Round20Evidence.committedRoot, "vg")

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
    fun `round 20 namespace suffix and operation returning helper probes get the true verdict out of verify_guards`() {
        driveEveryProbe("probes.json")
    }

    /**
     * The compiler's own answer for every Rell form the round-20 probes rest on,
     * recorded by `harness/spellings_r20.py` on a real chain. Round 18's lesson
     * was two probes written for forms the language does not have, so nothing
     * here is asserted about a spelling that was not put in front of `chr test`
     * on its own first - and sp20a and sp20b prove not only that the form
     * compiles but WHICH declaration a bare call reaches, by the state the
     * operation it adds writes.
     */
    @Test
    fun `every rell form the round 20 probes rest on was put in front of the compiler`() {
        val file = File(dir, "spellings.json")
        assertTrue(file.isFile, "the spelling measurements are missing at ${file.absolutePath}")
        val rows = Json.parseToJsonElement(file.readText()).jsonArray.associate { row ->
            val o = row.jsonObject
            o.getValue("spelling").jsonPrimitive.content to o
        }
        val mustCompileAndRun = listOf(
            "sp20a_an_exact_import_and_a_local_namespace_bind_the_same_name",
            "sp20b_a_top_level_helper_and_a_namespaced_one_share_a_name",
            "sp20c_a_helper_returns_an_operation",
            "sp20d_a_helper_returns_a_block_the_caller_extends",
            "sp20e_an_operation_parameter_two_helpers_deep"
        )
        val wrong = mutableListOf<String>()
        mustCompileAndRun.forEach { name ->
            val row = rows[name] ?: run { wrong += "$name was never measured"; return@forEach }
            if (row.getValue("compiles").jsonPrimitive.content != "true") {
                wrong += "$name does not compile: ${row["compiler_says"]}"
            }
            if (row.getValue("test_ok").jsonPrimitive.content != "true") {
                wrong += "$name compiles but its claim does not HOLD on the chain: ${row["compiler_says"]}"
            }
        }
        assertEquals(
            emptyList<String>(),
            wrong,
            "the compiler no longer agrees with what the round-20 probes rest on:\n" + wrong.joinToString("\n")
        )
    }

    /**
     * THE CHAIN RUNS THEMSELVES. A probe is only a mutant experiment if its
     * baseline is GREEN and its mutant is RED on a real chain; a verdict quoted
     * from a probe whose baseline never passed proves nothing about the tool.
     * The committed `vg/<probe>.chain.json` files are those runs, and this reads
     * them rather than trusting the README.
     */
    @Test
    fun `every round 20 probe has a green baseline and a red mutant on a real chain`() {
        val wrong = mutableListOf<String>()
        for (element in probes("probes.json")) {
            val name = element.jsonObject["probe"]!!.jsonPrimitive.content
            val file = File(dir, "$name.chain.json")
            if (!file.isFile) {
                wrong += "$name has no chain run at ${file.absolutePath}"
                continue
            }
            val row = Json.parseToJsonElement(file.readText()).jsonObject
            if (row["baseline_ok"]?.jsonPrimitive?.content != "true") {
                wrong += "$name baseline is not green: ${row["baseline_error"]}"
            }
            if (row["mutant_ok"]?.jsonPrimitive?.content != "false") {
                wrong += "$name mutant is not red: ${row["mutant_error"]}"
            }
        }
        assertEquals(
            emptyList<String>(),
            wrong,
            "round-20 probe(s) are not valid mutant experiments:\n" + wrong.joinToString("\n")
        )
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
            Round20Evidence.record(relative, record)
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
                Executable { Round20Evidence.assertFrozen(relative, record) }
            }
        )
    }
}
