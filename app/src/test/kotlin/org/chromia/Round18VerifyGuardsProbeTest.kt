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
    fun `round 18 import and transaction probes get the true verdict out of verify_guards`() {
        driveEveryProbe("probes.json")
    }

    /**
     * THE FIX LANE'S OWN PROBES. The three inputs round 18 found are measured by
     * `probes.json` above, which is the adversary's evidence and does not move.
     * The FIX, though, says more than those three probes measure: it parses
     * every import form the compiler accepts and counts the operations of a
     * transaction STRUCTURALLY, and a sentence in the long form, the advertised
     * description or the README that no probe measures is a claim, not a
     * measurement. `probes-fix.json` is one probe per form the fixed tool's
     * sentences name - `.op(a, b)`, a list literal, `.ops([...])`, an operation
     * built in one statement and run in another, a block of two transactions, a
     * RELATIVE exact import - plus the control that proves the structural count
     * still reads `rell.test.tx(take(...))` as ONE operation.
     *
     * Every one was written to disk and run on a REAL chain first
     * (`harness/vg_r18_fix.py`, this worktree's own database, one schema per
     * probe): baseline green, mutant red, recorded in
     * `vg/<probe>.chain.json`. Six of the seven were DANGEROUS before the fix -
     * the same attack as p18b, a second operation refusing ON THE DAMAGE
     * reported as the attack being refused, written in a form the `.op(`-count
     * could not see.
     */
    @Test
    fun `the import and transaction forms the fixed tool claims are each measured`() {
        driveEveryProbe("probes-fix.json")
    }

    /**
     * EVERY SENTENCE ROUND 18 ADDED, WHERE AN AGENT READS IT, ATTACHED TO A
     * PROBE THAT MEASURED IT.
     *
     * Round 18 found the round-17 description saying something false - "That
     * statement may reach the declaration through helpers in ANY test module,
     * via imports and aliases, up to 16 calls deep", while
     * `import tests.helpers.{ audited };` was an import it could not read
     * (p18a, p18k). The fix made that sentence true and added three more
     * claims. This test is what keeps them attached to the code: each is
     * recorded with whether it is present AND asserted, in all three places an
     * agent can read it - the `describe_tool` long form, the 1200-byte
     * advertised description, and the repository README - and each names the
     * probe that measured it. A sentence edited away is a red here, not a quiet
     * drift into prose that no longer describes the tool. It is the round-17
     * pattern ([Round17SurfaceProbeTest]) applied to round 18's own sentences.
     */
    @Test
    fun `every verify_guards sentence round 18 added is present where an agent reads it`() {
        val long = McpTools.fullDescription("verify_guards").orEmpty()
        val advertised = McpTools.advertisedDescription("verify_guards").orEmpty()
        val readme = File("../README.md").readText()
        fun flat(text: String) = text.replace(Regex("\\s+"), " ").trim()
        val sources = mapOf("long" to flat(long), "advertised" to flat(advertised), "readme" to flat(readme))

        // (id, which text, the sentence verbatim, the probe that measured it)
        val claims = listOf(
            // EVERY import form, not a subset (p18a/p18a2/p18k/p18k2 exact, f18f relative)
            listOf("long-every-import-form", "long", "EVERY IMPORT FORM", "p18a"),
            listOf("long-exact-import", "long", "the EXACT import `import a.b.{ x, y };`", "p18a"),
            listOf("long-relative-import", "long", "`import .sub;` (a submodule of the importing module)", "f18f"),
            listOf("long-no-as-in-exact", "long", "There is no `as` inside `{ }`", "compiler"),
            // the alias rule (p18c)
            listOf("long-qualifier-calling-module-first", "long", "A QUALIFIER IS RESOLVED IN THE CALLING MODULE FIRST", "p18c"),
            listOf("long-alias-bound-to-a-test-module", "long", "a qualifier bound to a test module is never the guard's declaration", "p18c"),
            // the structural operation count (p18b, f18a, f18b, f18d, f18e, f18g)
            listOf("long-structural-count", "long", "HOW THE OPERATIONS ARE COUNTED: structurally, not by counting `.op(`", "p18b"),
            listOf("long-list-literal", "long", "a list literal is counted element by element", "f18b"),
            listOf("long-block-is-not-one-transaction", "long", "a rell.test BLOCK of more than one transaction is not one transaction", "f18e"),
            listOf("long-unknown-is-not-zero", "long", "is NOT zero operations, it is an unknown number of them", "f18d"),
            listOf("long-two-ops-without-dot-op", "long", "carries TWO operations even though it contains no `.op(` at all", "p18b"),
            listOf("long-op-without-a-run", "long", "an operation call in test scope only builds a rell.test.op", "f18d"),
            listOf("long-shape-a-constructor", "long", "the same one operation written rell.test.tx(<that declaration>(...))", "f18g"),
            // the same facts in the 1200-byte description an agent sees first
            listOf("advertised-every-import-form", "advertised", "every import form the compiler takes, exact `.{ }` and relative too", "p18a"),
            listOf("advertised-alias-rule", "advertised", "a qualifier the CALLING module binds to a test module is a HELPER", "p18c"),
            listOf("advertised-structural-count", "advertised", "Its operations are counted structurally", "p18b"),
            listOf("advertised-unreadable-count", "advertised", "any count but one, or one it cannot read, is ambiguous_refusal", "f18d"),
            // and in the README's own account of the two shapes
            listOf("readme-every-import-form", "readme", "**Every import form the compiler takes** is parsed, not a subset", "p18a"),
            listOf("readme-relative-import", "readme", "the relative spellings `import .sub;` and `import ^.sibling;`", "f18f"),
            listOf("readme-alias-rule", "readme", "**A qualifier is resolved in the calling module first**", "p18c"),
            listOf("readme-structural-count", "readme", "The operations themselves are counted **structurally**", "p18b"),
            listOf("readme-unknown-is-not-zero", "readme", "it is an unknown number of them, and unknown is `ambiguous_refusal`", "f18d"),
            listOf("readme-block", "readme", "a `rell.test.block()` of more than one transaction is not one transaction", "f18e"),
            listOf("readme-shape-a-constructor", "readme", "written `rell.test.tx(<the declaration>(...))`", "f18g")
        )
        val rows = buildJsonArray {
            for (claim in claims) {
                val (id, where, text, probe) = claim
                add(
                    buildJsonObject {
                        put("claim", id)
                        put("where", where)
                        put("present", sources.getValue(where).contains(flat(text)))
                        put("measured_by", probe)
                        put("text", flat(text))
                    }
                )
            }
        }
        Round18Evidence.record("describe/verify_guards_claims.json", rows)
        val missing = claims.filterNot { sources.getValue(it[1]).contains(flat(it[2])) }
            .map { "${it[0]} (${it[1]}, measured by ${it[3]}): ${it[2]}" }
        assertAll(
            Executable {
                assertEquals(
                    emptyList<String>(),
                    missing,
                    "verify_guards sentence(s) round 18 added are no longer in the text an agent reads:\n" +
                        missing.joinToString("\n")
                )
            },
            Executable { Round18Evidence.assertFrozen("describe/verify_guards_claims.json", rows) }
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
