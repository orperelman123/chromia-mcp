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
import org.chromia.tools.McpTools
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
 *
 * WHAT BECAME OF IT (round-20 tool fix lane). All four are FIXED and the pins
 * below are untouched - the verdicts moved because the TOOL moved. A helper is
 * registered under its own namespace path and nothing else, and `resolveHelper`
 * takes the namespace the call is READ IN: enclosing namespaces innermost first,
 * then the module's own top level and its namespaces, then an exact import, then
 * a wildcard one. A namespace member has NO bare spelling (sp20f), so `namespace
 * h { function audited }` no longer shadows `import tests.aux.{ audited };`
 * (r20a, r20a2) and no longer appends a second body to a name that has one
 * (r20c); a bare call INSIDE a namespace still names its sibling (sp20g) and
 * `b.f(...)` inside `namespace a` still names `a.b.f` (sp20h), because that is
 * relative resolution and it is real. And the operation a carrier's argument
 * names is followed through operation-returning helpers, one or a chain of them
 * (sp20c, sp20i), so `.op(make())` carries `take` and r20b2 is load_bearing.
 * `probes-fix.json` measures the fix's own sentences the same way.
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
            "sp20e_an_operation_parameter_two_helpers_deep",
            // THE FIX'S OWN, measured by `harness/spellings_r20_fix.py` before a
            // line of Kotlin changed: relative resolution is real (a bare call
            // inside a namespace names its sibling, and `b.f(...)` inside
            // `namespace a` names `a.b.f`), and an operation-returning helper may
            // call another one, so the operation is found by following the CHAIN.
            "sp20g_a_bare_call_inside_a_namespace_names_its_sibling",
            "sp20h_a_relative_namespace_path_inside_a_namespace",
            "sp20i_an_operation_returning_helper_calls_another"
        )
        // THE NEGATIVE the whole fix rests on: a suffix of a namespace path is
        // NOT a spelling of the name, so a namespaced helper has no bare name at
        // the module's top level. Round 18's lesson was two probes written for
        // forms the language does not have; this one is pinned as not existing.
        val mustNotCompile = listOf("sp20f_a_namespaced_helper_has_no_bare_spelling")
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
        mustNotCompile.forEach { name ->
            val row = rows[name] ?: run { wrong += "$name was never measured"; return@forEach }
            if (row.getValue("compiles").jsonPrimitive.content != "false") {
                wrong += "$name COMPILES, and the fix assumes the compiler refuses it"
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
        for (element in probes("probes.json") + probes("probes-fix.json")) {
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

    /**
     * THE FIX LANE'S OWN PROBES. The four false verdicts round 20 found are
     * measured by `probes.json` above, which is the adversary's evidence and does
     * not move. The FIX says more than those four probes measure: it claims a
     * resolution ORDER (enclosing namespace, then the module's own top level and
     * namespaces, then an exact import, then a wildcard one), it claims a
     * namespace member has no bare spelling at all, and it claims the operation a
     * carrier's argument names is followed through operation-returning helpers. A
     * sentence in the long form, the advertised description or the README that no
     * probe measures is a claim, not a measurement.
     *
     * `probes-fix.json` is one probe per form those sentences name - r20a
     * MIRRORED, so that the import wins and the honest test is certified (f20a);
     * a bare call inside a namespace naming its SIBLING while the module's top
     * level declares the same name (f20b); the two-segment form of it, `b.f(...)`
     * inside `namespace a` (f20c); and an operation reached through a CHAIN of
     * operation-returning helpers (f20d) - plus two controls in the direction
     * that must still refuse: an operation-returning helper feeding a transaction
     * that already carries one operation, which following the argument must not
     * collapse into one (f20e), and a QUALIFIED `h.audited(...)` whose namespaced
     * body really does add the second operation, which dropping the suffix
     * registration must not stop expanding (f20f).
     *
     * Every one was written to disk and run on a REAL chain first
     * (`harness/vg_r20_fix.py`, this worktree's own database, one schema per
     * probe): baseline green, mutant red, recorded in `vg/<probe>.chain.json`.
     * And every Rell form any of them uses was put in front of the compiler on
     * its own first (`harness/spellings_r20_fix.py`, merged into
     * `vg/spellings.json` as sp20f..sp20i), so the fix models no spelling the
     * language does not have - sp20f is pinned as NOT compiling.
     */
    @Test
    fun `the resolution order and the operation returning helpers the fixed tool claims are each measured`() {
        driveEveryProbe("probes-fix.json")
    }

    /**
     * EVERY SENTENCE ROUND 20 ADDED, WHERE AN AGENT READS IT, ATTACHED TO A PROBE
     * THAT MEASURED IT - the round-17/18/19 pattern applied to round 20's own
     * sentences. A sentence edited away is a red here, not a quiet drift into
     * prose that no longer describes the tool.
     */
    @Test
    fun `every verify_guards sentence round 20 added is present where an agent reads it`() {
        val long = McpTools.fullDescription("verify_guards").orEmpty()
        val advertised = McpTools.advertisedDescription("verify_guards").orEmpty()
        val readme = File("../README.md").readText()
        fun flat(text: String) = text.replace(Regex("\\s+"), " ").trim()
        val sources = mapOf("long" to flat(long), "advertised" to flat(advertised), "readme" to flat(readme))

        // (id, which text, the sentence verbatim, the probe that measured it)
        val claims = listOf(
            // the resolution order (r20a, r20a2, r20c, f20a, f20b, f20c)
            listOf("long-order", "long", "A NAME BINDS THE WAY THE COMPILER BINDS IT", "f20b"),
            listOf("long-relative-bare", "long", "a bare f(...) is h.f even when the module declares a top-level f", "f20b"),
            listOf("long-relative-path", "long", "b.f(...) is a.b.f", "f20c"),
            listOf("long-no-bare-spelling", "long", "A NAMESPACE MEMBER HAS NO BARE SPELLING", "f20a"),
            listOf("long-never-shadows-an-import", "long", "so a local namespace never shadows an `import a.b.{ f };`", "r20a"),
            // the operation-returning helpers (r20b2, f20d, f20e)
            listOf(
                "long-operation-followed", "long",
                "THE OPERATION A CARRIER'S ARGUMENT NAMES IS FOLLOWED THROUGH OPERATION-RETURNING HELPERS", "r20b2"
            ),
            listOf("long-operation-chain", "long", "a CHAIN of such helpers is followed to the end", "f20d"),
            listOf(
                "long-operation-not-followed", "long",
                "A helper whose body builds or runs a transaction of its own is not followed", "f20e"
            ),
            // the same order in the 1200-byte description an agent sees first
            listOf(
                "advertised-order", "advertised",
                "in the COMPILER'S order: enclosing namespace, then module, then imports, a member has no bare name", "f20a"
            ),
            // and in the README's own account of the two shapes
            listOf(
                "readme-order", "readme",
                "**A name binds the way the compiler binds it, and the tool follows that order**", "f20b"
            ),
            listOf("readme-no-bare-spelling", "readme", "**A namespace member has no bare spelling**", "f20a"),
            listOf(
                "readme-operation-followed", "readme",
                "**The operation a carrier's argument names is followed through operation-returning helpers**", "r20b2"
            ),
            listOf("readme-operation-chain", "readme", "a chain of such helpers is followed to the end", "f20d")
        )
        val rows = buildJsonArray {
            for (claim in claims) {
                add(
                    buildJsonObject {
                        put("claim", claim[0])
                        put("where", claim[1])
                        put("present", sources.getValue(claim[1]).contains(flat(claim[2])))
                        put("measured_by", claim[3])
                        put("text", flat(claim[2]))
                    }
                )
            }
        }
        Round20Evidence.record("describe/verify_guards_claims.json", rows)
        val missing = claims.filterNot { sources.getValue(it[1]).contains(flat(it[2])) }
            .map { "${it[0]} (${it[1]}, measured by ${it[3]}): ${it[2]}" }
        assertAll(
            Executable {
                assertEquals(
                    emptyList<String>(),
                    missing,
                    "verify_guards sentence(s) round 20 added are no longer in the text an agent reads:\n" +
                        missing.joinToString("\n")
                )
            },
            Executable { Round20Evidence.assertFrozen("describe/verify_guards_claims.json", rows) }
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
