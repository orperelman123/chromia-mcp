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

    /**
     * THE FIX LANE'S OWN PROBES. The three inputs round 19 found are measured by
     * `probes.json` above, which is the adversary's evidence and does not move.
     * The FIX, though, says more than those three probes measure: it counts a
     * `rell.test.block()`'s operations through BOTH its builders, it resolves a
     * qualified call through NAMESPACES in five spellings, and it binds a
     * helper's parameters to the caller's arguments. A sentence in the long
     * form, the advertised description or the README that no probe measures is a
     * claim, not a measurement.
     *
     * `probes-fix.json` is one probe per form those sentences name - a list
     * literal in the block builder, the block CONSTRUCTOR carrying operations, a
     * whole transaction inside the block builder, a namespace in an imported
     * module and one nested, reached plainly, by wildcard, by exact import and
     * behind an alias, and two operations passed as PARAMETERS - plus four
     * controls in the other direction, which the widened models must still
     * certify: a namespaced helper that builds the honest one-operation
     * transaction, one operation reached through both builders, the operation
     * bound by NAME, the block constructor with one operation, and a
     * TRANSACTION passed as a parameter, which must not be substituted into the
     * closure or its one operation reads as two.
     *
     * Every one was written to disk and run on a REAL chain first
     * (`harness/vg_r19_fix.py`, this worktree's own database, one schema per
     * probe): baseline green, mutant red, recorded in `vg/<probe>.chain.json`.
     * And every Rell form any of them uses was put in front of the compiler on
     * its own first (`harness/spellings_r19.py`, recorded in
     * `vg/spellings.json`), so the fix models no spelling the language does not
     * have - `.ops([...])` and `.txs([...])` are both "no member" and neither is
     * modelled.
     */
    @Test
    fun `the block carriers namespace spellings and parameter binding the fixed tool claims are each measured`() {
        driveEveryProbe("probes-fix.json")
    }

    /**
     * EVERY SENTENCE ROUND 19 ADDED, WHERE AN AGENT READS IT, ATTACHED TO A
     * PROBE THAT MEASURED IT - the round-17/round-18 pattern applied to round
     * 19's own sentences. A sentence edited away is a red here, not a quiet
     * drift into prose that no longer describes the tool.
     */
    @Test
    fun `every verify_guards sentence round 19 added is present where an agent reads it`() {
        val long = McpTools.fullDescription("verify_guards").orEmpty()
        val advertised = McpTools.advertisedDescription("verify_guards").orEmpty()
        val readme = File("../README.md").readText()
        fun flat(text: String) = text.replace(Regex("\\s+"), " ").trim()
        val sources = mapOf("long" to flat(long), "advertised" to flat(advertised), "readme" to flat(readme))

        // (id, which text, the sentence verbatim, the probe that measured it)
        val claims = listOf(
            // the namespaces (r19b, f19d, f19e, f19f, f19g, f19h)
            listOf("long-namespace-qualifier", "long", "A QUALIFIER MAY ALSO BE A NAMESPACE", "r19b"),
            listOf("long-namespace-dotted-path", "long", "it may be a whole dotted path", "f19e"),
            listOf(
                "long-namespace-imported-module", "long",
                "a namespace of an IMPORTED test module is reached through every import form", "f19d"
            ),
            listOf("long-namespace-is-not-an-import", "long", "A namespace is not an import", "r19b"),
            // the parameter binding (r19c2, f19l, f19n)
            listOf("long-parameters-bound", "long", "A HELPER'S PARAMETERS ARE BOUND TO THE CALLER'S ARGUMENTS", "r19c2"),
            listOf("long-parameters-positional-or-named", "long", "positionally or by name", "f19l"),
            listOf("long-unknown-stays-unknown", "long", "is left as it stands and is still UNKNOWN", "f19n"),
            // the block carriers (r19a, r19a2, f19a, f19b, f19k)
            listOf("long-block-carries-operations", "long", "A BLOCK CARRIES OPERATIONS TOO", "r19a"),
            listOf("long-block-one-transaction-two-operations", "long", "is ONE transaction of TWO operations", "r19a2"),
            listOf("long-not-counted-twice", "long", "counted by that transaction's own carriers, never twice", "f19k"),
            listOf("long-no-ops-no-txs", "long", "There is no `.ops(` and no `.txs(`", "spellings"),
            // the same facts in the 1200-byte description an agent sees first
            listOf("advertised-namespaces", "advertised", "plus NAMESPACES", "r19b"),
            listOf("advertised-parameters", "advertised", "Helper PARAMETERS bind to call ARGUMENTS", "r19c2"),
            listOf("advertised-block-args", "advertised", "block .tx() args", "r19a"),
            // and in the README's own account of the two shapes
            listOf("readme-namespace-qualifier", "readme", "**A qualifier may also be a namespace**", "r19b"),
            listOf(
                "readme-namespace-imported", "readme",
                "a namespace of an imported test module is reached through every import form", "f19d"
            ),
            listOf("readme-parameters", "readme", "**A helper's parameters are bound to the caller's arguments**", "r19c2"),
            listOf("readme-block-carries", "readme", "**A block carries operations too**", "r19a"),
            listOf("readme-block-one-two", "readme", "**one transaction of two operations**", "r19a2"),
            listOf("readme-not-twice", "readme", "counted by that transaction's own carriers, never twice", "f19k"),
            listOf("readme-no-ops-no-txs", "readme", "There is no `.ops(` and no `.txs(`", "spellings")
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
        Round19Evidence.record("describe/verify_guards_claims.json", rows)
        val missing = claims.filterNot { sources.getValue(it[1]).contains(flat(it[2])) }
            .map { "${it[0]} (${it[1]}, measured by ${it[3]}): ${it[2]}" }
        assertAll(
            Executable {
                assertEquals(
                    emptyList<String>(),
                    missing,
                    "verify_guards sentence(s) round 19 added are no longer in the text an agent reads:\n" +
                        missing.joinToString("\n")
                )
            },
            Executable { Round19Evidence.assertFrozen("describe/verify_guards_claims.json", rows) }
        )
    }

    /**
     * The compiler's own answer for every Rell form the fix models, recorded by
     * `harness/spellings_r19.py` on a real chain. It is read here so that a
     * spelling the fix names is one the language HAS: round 18's lesson was two
     * probes written for forms that do not exist, and the two that do not exist
     * here - `.ops([...])` and `.txs([...])` - are pinned as not existing.
     */
    @Test
    fun `every rell form the fix models was put in front of the compiler`() {
        val file = File(dir, "spellings.json")
        assertTrue(file.isFile, "the spelling measurements are missing at ${file.absolutePath}")
        val rows = Json.parseToJsonElement(file.readText()).jsonArray.associate { row ->
            val o = row.jsonObject
            o.getValue("spelling").jsonPrimitive.content to o
        }
        val mustCompile = listOf(
            "block_tx_two_operations", "block_tx_one_operation", "block_tx_list_of_operations",
            "block_tx_of_a_transaction", "block_constructor_with_a_transaction",
            "block_constructor_with_a_list_of_transactions", "block_constructor_with_operations",
            "namespace_in_the_calling_module", "namespace_nested", "namespace_dotted_declaration",
            "namespace_through_a_plain_import", "namespace_through_a_wildcard_import",
            "namespace_through_an_exact_import", "namespace_through_an_alias",
            "operation_as_a_parameter", "operation_as_a_named_argument", "transaction_as_a_parameter"
        )
        val mustNotCompile = listOf("tx_ops_member", "block_txs_member")
        val wrong = mutableListOf<String>()
        (mustCompile + mustNotCompile).forEach { name ->
            val row = rows[name] ?: run { wrong += "$name was never measured"; return@forEach }
            val compiles = row.getValue("compiles").jsonPrimitive.content == "true"
            val expected = name in mustCompile
            if (compiles != expected) {
                wrong += "$name: compiles=$compiles, expected $expected (${row["compiler_says"]})"
            }
            if (expected && row.getValue("test_ok").jsonPrimitive.content != "true") {
                wrong += "$name compiles but does not RUN: ${row["compiler_says"]}"
            }
        }
        assertEquals(
            emptyList<String>(),
            wrong,
            "the compiler no longer agrees with what the fix models:\n" + wrong.joinToString("\n")
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
