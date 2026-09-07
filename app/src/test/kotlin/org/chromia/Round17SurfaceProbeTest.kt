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
import org.junit.jupiter.api.Assertions.assertEquals
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
     * EVERY SENTENCE ROUND 17 PUT INTO THE AGENT-FACING TEXT, MEASURED.
     *
     * Round 17 found one long-form sentence that was not true of the code -
     * `describe_tool{tool:"verify_guards"}` ended with "A replacement's own
     * require() messages count as refusals exactly like the guard's", the
     * round-12/13 rule the round-16 rewrite deleted - and three that were true
     * only in a narrower case than they stated: "directly or through one
     * test-module helper" did not say the helper must be in the test's OWN file
     * (p17a), and neither the description nor the long form mentioned that a
     * call through an import ALIAS was not recognised (p17f) or that a helper
     * chain deeper than four was scored as ninety-nine call sites (p17g).
     *
     * The fix made the first sentence true (literal attribution reads the
     * MUTANT sources) and rewrote the other three. This test is the measurement
     * that keeps them attached to the code: every sentence the round added, in
     * all three places an agent can read it - the `describe_tool` long form, the
     * 1200-byte advertised description, and the repository README - is recorded
     * with whether it is present AND asserted. A sentence that is edited away
     * is a red here, not a quiet drift into prose that no longer describes the
     * tool.
     */
    @Test
    fun `every verify_guards sentence round 17 added is present where an agent reads it`() {
        val long = McpTools.fullDescription("verify_guards").orEmpty()
        val advertised = McpTools.advertisedDescription("verify_guards").orEmpty()
        val readme = File("../README.md").readText()
        fun flat(text: String) = text.replace(Regex("\\s+"), " ").trim()
        val sources = mapOf("long" to flat(long), "advertised" to flat(advertised), "readme" to flat(readme))

        // id to (which text, the sentence verbatim). Every one of these is a
        // claim about behaviour a round-17 probe measured.
        val claims = listOf(
            // the sentence p17m proved false, now true again
            Triple(
                "replacement-messages-count-as-refusals", "long",
                "A replacement's own require() messages count as refusals exactly like the guard's."
            ),
            Triple("replacement-literals-read-off-the-mutant", "long", "read off the MUTANT SOURCES THAT RAN"),
            Triple("replacement-still-refused-not-ambiguous", "long", "is still_refused rather than ambiguous_refusal"),
            // where a helper may live, and how a call to it resolves (p17a, p17f)
            Triple("helper-in-any-test-module", "long", "A HELPER MAY LIVE IN ANY TEST MODULE of the submission"),
            Triple("helper-through-an-import-alias", "long", "alias.h(...) through `import alias: a.b.mod;`"),
            Triple("helper-through-a-wildcard-import", "long", "h(...) in its own module or through `import mod.*;`"),
            // the depth rule, said as a cap rather than as a count (p17g)
            Triple("helper-chain-depth", "long", "followed up to 16 calls deep"),
            Triple("depth-refusal-names-the-cap", "long", "the chain is deeper than 16 calls and counts no call sites at all"),
            // operations counted over the whole closure (p17d)
            Triple("operations-over-the-whole-closure", "long", "read over the statement's WHOLE call closure"),
            Triple("helper-adding-an-operation-is-ambiguous", "long", "quietly adds a second operation makes the"),
            // the two arguments' scope
            Triple("pins-are-shape-b-only", "long", "stillRefused and attackLanded are read in SHAPE B ONLY"),
            // the shapes themselves, unchanged
            Triple("shape-a", "long", "SHAPE A, the must-fail test"),
            Triple("shape-b", "long", "SHAPE B, the must-hold test"),
            // the same four facts in the 1200-byte description an agent sees first
            Triple("advertised-helper-scope", "advertised", "helpers in ANY test module, via imports and aliases, up to"),
            Triple("advertised-helper-depth", "advertised", "16 calls deep"),
            Triple("advertised-replacement", "advertised", "A replacement's own messages are attributed like the guard's"),
            Triple("advertised-pins", "advertised", "stillRefused/attackLanded are read in the must-hold shape only"),
            // and in the README's own account of the two shapes
            Triple("readme-helper-in-any-test-module", "readme", "A helper may live in **any test module** of the submission"),
            Triple("readme-helper-alias", "readme", "`alias.h(...)` through `import alias: a.b.mod;`"),
            Triple("readme-helper-depth", "readme", "The chain is followed **16 calls deep**"),
            Triple("readme-depth-names-no-number", "readme", "it never names a number it cannot stand behind"),
            Triple("readme-operations-closure", "readme", "read over the statement's **whole call closure**"),
            Triple("readme-replacement", "readme", "read off the **mutant sources that ran**"),
            Triple("readme-pins", "readme", "are read in **shape B only**")
        )
        val rows = buildJsonArray {
            for ((id, where, text) in claims) {
                add(
                    buildJsonObject {
                        put("claim", id)
                        put("where", where)
                        put("present", sources.getValue(where).contains(flat(text)))
                        put("text", text)
                    }
                )
            }
        }
        File(out, "describe").mkdirs()
        File(out, "describe/verify_guards_claims.json").writeText(
            Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), rows)
        )
        println("ROUND17-VG-CLAIMS\n" + rows.joinToString("\n") { (it as JsonObject).toString() })
        val missing = claims.filterNot { (_, where, text) -> sources.getValue(where).contains(flat(text)) }
            .map { (id, where, text) -> "$id ($where): $text" }
        assertEquals(
            emptyList<String>(),
            missing,
            "verify_guards sentence(s) round 17 added are no longer in the text an agent reads:\n" +
                missing.joinToString("\n")
        )
    }
}
