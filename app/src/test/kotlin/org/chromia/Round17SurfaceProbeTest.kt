package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.ToolDocs
import org.junit.jupiter.api.Test
import java.io.File

/**
 * ROUND 17 - the two surfaces that are read rather than run: where the REDIRECT
 * sends an ask, and whether every `describe_tool` LONG FORM is true.
 *
 * Both are recorders, not assertions: they write what the shipped code actually
 * answers into `realworld/adversary-round17/` so the round's claims are
 * measurements. Round 16 already showed the redirect is the hazard rather than
 * the disclaimer - `scaffold_dapp template=amm` silently became `template=vault`
 * and the AMM built on that answer was drained - and GOAL.md rules out "we never
 * claimed to cover it" as a defence, so an ask answered by a template whose
 * guards do not cover it is a finding whatever the note says.
 */
class Round17SurfaceProbeTest {

    private val out = File("src/test/resources/exploit-corpus/realworld/adversary-round17")

    /**
     * Thirty-four asks. The first block is the classes `docs/TEMPLATE-GAPS.md`
     * names as the next queue when its table is empty; the rest are the shapes
     * the round-17 brief names - plural forms, hyphenation, two classes in one
     * ask, and words that are not English.
     */
    private val asks = listOf(
        // the un-templated queue
        "a raffle with on-chain randomness",
        "a lottery that pays a random winner from the prize pool",
        "a weekly lottery with rewards for ticket holders",
        "an insurance pool with claims",
        "an insurance pool funded by premium payments",
        "a parametric insurance payout on an oracle trigger",
        // plurals and hyphenation
        "order books",
        "limit-orders",
        "cross-chain bridges",
        "escrows between two parties",
        "OTCs desk",
        "de-fi lending",
        "a de-fi yield aggregator",
        // two classes in one ask
        "a swap-and-escrow for two parties",
        "an NFT marketplace with escrowed offers",
        "a cross-chain DEX",
        "a liquidity mining program that pays rewards",
        "a staking vault",
        "a stablecoin with a governance token",
        "a lending market with a DAO treasury",
        // not English
        "una boveda con oraculo",
        "ein Tresor mit Orakel",
        "un echange de jetons",
        "borsa di scambio",
        // ordinary asks that should land somewhere sensible
        "a payment channel",
        "a loyalty programme with points",
        "a prediction market",
        "a gaming item shop",
        "a charity donation pool",
        "a crowdfunding campaign with refunds",
        "a vesting schedule for founders",
        "a multisig wallet",
        "a token airdrop with a claim window",
        "a fee splitter contract"
    )

    @Test
    fun `record where the redirect sends thirty-four asks`() {
        out.mkdirs()
        File(out, "redirect").mkdirs()
        val lines = mutableListOf<String>()
        val rows = buildJsonArray {
            for (ask in asks) {
                val target = DappScaffold.closestTemplate(ask)
                val note = DappScaffold.closestTemplateNote(ask)
                val scaffolded = if (target == null) listOf() else
                    runCatching { DappScaffold.files("probe", target).keys.toList() }
                        .getOrElse { listOf("<error: " + it.message + ">") }
                lines += "%-46s -> %-12s files=%d".format(ask, target ?: "(none)", scaffolded.size)
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("closestTemplate", target ?: "")
                        put("noteFirstSentence", note.substringBefore(". ").take(400))
                        put("noteLength", note.length)
                        put("scaffoldedFiles", buildJsonArray { scaffolded.forEach { add(it) } })
                    }
                )
            }
        }
        File(out, "redirect/raw.json").writeText(
            Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), rows)
        )
        println("ROUND17-REDIRECT\n" + lines.joinToString("\n"))
    }

    /**
     * Every tool's advertised description beside its `describe_tool` long form,
     * with the mechanical checks a long form can fail: a verdict, a flag or a
     * shape the long form names that the advertised description does not, and -
     * the round-17 question - a claim in the long form about behaviour the code
     * no longer has.
     */
    @Test
    fun `record every tool's short description beside its describe_tool long form`() {
        out.mkdirs()
        File(out, "describe").mkdirs()
        val names = McpTools.allTools().map { it.name }.sorted()
        val rows = buildJsonArray {
            for (name in names) {
                val short = McpTools.advertisedDescription(name).orEmpty()
                val full = McpTools.fullDescription(name).orEmpty()
                val moved = name in ToolDocs.LONG
                add(
                    buildJsonObject {
                        put("tool", name)
                        put("movedFromToolsList", moved)
                        put("shortBytes", short.toByteArray().size)
                        put("longBytes", full.toByteArray().size)
                        put("short", short)
                        put("long", if (moved) full else "")
                    }
                )
            }
        }
        File(out, "describe/raw.json").writeText(
            Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), rows)
        )
        val summary = names.joinToString("\n") { n ->
            val short = McpTools.advertisedDescription(n).orEmpty().toByteArray().size
            val long = McpTools.fullDescription(n).orEmpty().toByteArray().size
            "%-34s short=%5d long=%6d moved=%s".format(n, short, long, n in ToolDocs.LONG)
        }
        println("ROUND17-DESCRIBE\n$summary")
    }

    /**
     * The one claim in a long form that round 17 tests against the code rather
     * than by reading: `describe_tool{tool:"verify_guards"}` ends with
     *
     *     "A replacement's own require() messages count as refusals exactly
     *      like the guard's."
     *
     * That was the round-12/13 rule - a LIST of message sources, the guard's
     * then the replacement's - and the round-16 rewrite deleted it. `p17m` in
     * `vg/` measures the consequence on a running chain: a replacement whose own
     * require REFUSES the attack, in a query whose refusal carries no frame,
     * is answered `ambiguous_refusal` because the only two literal-attribution
     * helpers left read the ORIGINAL `files` map and the replacement's message
     * is in neither. This test pins the sentence's presence so the finding is
     * attached to the text it is about.
     */
    @Test
    fun `record the verify_guards long form sentences round 17 tested`() {
        val long = McpTools.fullDescription("verify_guards").orEmpty()
        val claims = mapOf(
            "replacement-messages-count-as-refusals" to
                "A replacement's own require() messages count as refusals exactly like the guard's.",
            "one-test-module-helper" to "directly or through one\n        test-module helper",
            "shape-a" to "SHAPE A, the must-fail test",
            "shape-b" to "SHAPE B, the must-hold test"
        )
        val rows = buildJsonArray {
            for ((id, text) in claims) {
                add(
                    buildJsonObject {
                        put("claim", id)
                        put("present", long.contains(text.replace("\n        ", " ")) || long.contains(text))
                        put("text", text.replace("\n        ", " "))
                    }
                )
            }
        }
        File(out, "describe").mkdirs()
        File(out, "describe/verify_guards_claims.json").writeText(
            Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), rows)
        )
        println("ROUND17-VG-CLAIMS\n" + rows.joinToString("\n") { (it as JsonObject).toString() })
    }
}
