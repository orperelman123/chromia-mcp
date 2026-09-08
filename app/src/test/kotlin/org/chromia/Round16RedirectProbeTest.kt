package org.chromia

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.junit.jupiter.api.Test

/**
 * ROUND 16, audit fix F2 - "unknown-template routing to the CLOSEST template".
 * `closestTemplateNote` is an ORDERED `when` of unanchored, case-insensitive
 * substring tests, so an earlier branch wins on a substring that appears inside
 * an unrelated word. This recorder asks it for a set of ordinary asks and writes
 * where each one lands, so the round's claim about the redirect is a measurement
 * rather than a reading of the source.
 *
 * Output: `build/adversary-round16/redirect/raw.json`, asserted equal to the
 * FROZEN `realworld/adversary-round16/redirect/raw.json` value by value (see
 * [Round16Evidence]) - an ask that lands somewhere else, or scaffolds a
 * different file set, is a regression reported with both values.
 *
 * RE-FROZEN 2026-09-07, by the round-17 template lane, and this is the record of
 * why - re-freezing is a deliberate act and a silent one is the drift this
 * mechanism exists to catch. FOUR of the twelve rows moved, all four on purpose:
 *
 *  - `an insurance pool with claims` (none) -> **insurance**. Round 17 built that
 *    class from this server's own answer and drained it twice, so it is the
 *    FIFTEENTH template and `docs/TEMPLATE-GAPS.md`'s rule is that the redirect
 *    moves in the same commit.
 *  - `a bidirectional payment channel` **ft4** -> (none). Round 17 recorded that
 *    as a misroute: `ft4` claims `payment*`, and a token ledger has no channel,
 *    no sequence number and no dispute window, so it covers none of the class's
 *    exploit. The answer now says NO and names the missing guard.
 *  - `a raffle with on-chain randomness` and `a prediction market` stay (none)
 *    and keep scaffolding nothing; only their first sentence changed, because the
 *    honest answer is a branch that names the class rather than the roster's
 *    catch-all.
 *
 * Nothing else in the twelve moved, which is the assurance this re-freeze is
 * meant to give: the round-16 fixes it pins - `an investment DAO` reaching
 * governance rather than streaming, `a loyalty programme` not reaching amm
 * through "progr-amm-e" - are all still exactly where round 16 left them.
 */
class Round16RedirectProbeTest {

    private val asks = listOf(
        "an investment DAO",
        "an investment club with a treasury",
        "a DAO",
        "a bidirectional payment channel",
        "an NFT lending protocol",
        "a yield harvesting vault",
        "a cross-chain DEX",
        "a wallet with a spending allowance",
        "a raffle with on-chain randomness",
        "an insurance pool with claims",
        "a prediction market",
        "a loyalty programme"
    )

    @Test
    fun `record where the redirect sends twelve ordinary asks`() {
        val lines = mutableListOf<String>()
        val rows = buildJsonArray {
            for (ask in asks) {
                val target = DappScaffold.closestTemplate(ask)
                val note = DappScaffold.closestTemplateNote(ask)
                // What ScaffoldDappStrategy would hand back: `effectiveTemplate =
                // exact ?: redirect`, then `files(chain, effectiveTemplate)`.
                val scaffolded = if (target == null) listOf<String>() else
                    runCatching { DappScaffold.files("probe", target).keys.toList() }
                        .getOrElse { listOf("<error: " + it.message + ">") }
                lines += "%-38s -> %-14s files=%s".format(ask, target ?: "(none)", scaffolded)
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("closestTemplate", target ?: "")
                        put("noteFirstSentence", note.substringBefore(". ").take(300))
                        put("scaffoldedFiles", buildJsonArray { scaffolded.forEach { add(it) } })
                    }
                )
            }
        }
        val relative = "redirect/raw.json"
        Round16Evidence.record(relative, rows)
        println("ROUND16-REDIRECT\n" + lines.joinToString("\n"))
        Round16Evidence.assertFrozen(relative, rows)
    }
}
