package org.chromia

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * ROUND 17, THE REDIRECT AFTER THE FIX - every one of the thirty-four asks the
 * adversary measured, plus the ones this lane's own change makes possible, pinned
 * to a FROZEN recording.
 *
 * The adversary's own recorder (`Round17SurfaceProbeTest`) writes
 * `adversary-round17/redirect/raw.json` on every run and asserts nothing; that
 * file is the BEFORE, and this lane does not touch it. This class is the AFTER,
 * and it is frozen for the reason round 16 froze its recorders: a redirect that
 * moves without anybody noticing is exactly the defect this round found. Round 8
 * measured what a confident redirect to the wrong template costs, and round 17
 * found the honest `else` branch naming ELEVEN templates when fifteen ship.
 *
 * A route that changes here fails with the committed value beside the fresh one.
 * Re-freezing is deliberate: copy `app/build/round17-template-fix/redirect/raw.json`
 * over the committed file and commit it with the reason.
 */
class Round17TemplateRedirectProbeTest {

    /** The thirty-four `adversary-round17/redirect/raw.json` measured, in its order. */
    private val measured = listOf(
        "a raffle with on-chain randomness",
        "a lottery that pays a random winner from the prize pool",
        "a weekly lottery with rewards for ticket holders",
        "an insurance pool with claims",
        "an insurance pool funded by premium payments",
        "a parametric insurance payout on an oracle trigger",
        "order books",
        "limit-orders",
        "cross-chain bridges",
        "escrows between two parties",
        "OTCs desk",
        "de-fi lending",
        "a de-fi yield aggregator",
        "a swap-and-escrow for two parties",
        "an NFT marketplace with escrowed offers",
        "a cross-chain DEX",
        "a liquidity mining program that pays rewards",
        "a staking vault",
        "a stablecoin with a governance token",
        "a lending market with a DAO treasury",
        "una boveda con oraculo",
        "ein Tresor mit Orakel",
        "un echange de jetons",
        "borsa di scambio",
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

    /**
     * This lane's own: every phrasing of the insurance class an agent would use,
     * the plural and hyphenated forms of it, the words that must NOT reach it, and
     * the three classes whose honest answer round 17 turned into a NO.
     */
    private val added = listOf(
        "insurance",
        "an insurance pool",
        "a mutual insurance pool with claims",
        "a mutual risk pool",
        "a risk pool for members",
        "underwriting cover for members",
        "a claims pool",
        "insurance premiums and cover",
        "parametric cover on a weather index",
        "an indemnity pool",
        "reinsurance for a pool",
        // ...and the words this class must NOT take from the class next door.
        "a premium membership plan",
        "a premium subscription tier",
        "a payment channel between two parties",
        "a multisig treasury wallet",
        "a lottery",
        "a liquidity mining program",
        "a liquidity pool",
        "an airdrop with a claim window",
        "otc",
        "OTC desks"
    )

    @Test
    fun `record where the redirect sends every measured ask after the fifteenth template`() {
        val lines = mutableListOf<String>()
        val rows = buildJsonArray {
            (measured + added).forEach { ask ->
                val target = DappScaffold.closestTemplate(ask)
                val note = DappScaffold.closestTemplateNote(ask)
                val scaffolded = if (target == null) listOf() else
                    runCatching { DappScaffold.files("probe", target).keys.toList() }
                        .getOrElse { listOf("<error: " + it.message + ">") }
                lines += "%-52s -> %-12s files=%d".format(ask, target ?: "(none)", scaffolded.size)
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("closestTemplate", target ?: "")
                        put("noteFirstSentence", note.substringBefore(". ").take(400))
                        put("scaffoldedFiles", buildJsonArray { scaffolded.forEach { add(it) } })
                    }
                )
            }
        }
        val relative = "redirect/raw.json"
        Round17TemplateEvidence.record(relative, rows)
        println("ROUND17-TEMPLATE-REDIRECT-AFTER\n" + lines.joinToString("\n"))
        Round17TemplateEvidence.assertFrozen(relative, rows)
    }

    /**
     * The seven misroutes `adversary-round17/README.md` section 3 tabulates, each
     * pinned to what it does NOW and to WHY that is not the same mistake with better
     * aim. Two rules, and every row obeys one of them: route to the template whose
     * GUARDS cover the ask, or say NO and name the guard that is missing. A redirect
     * to a template that does not cover the ask's exploit class is the hazard rounds
     * 8 and 14 recorded.
     */
    @Test
    fun `the seven measured misroutes are answered by guards or by an honest no`() {
        fun note(ask: String) = DappScaffold.closestTemplateNote(ask)

        // 1-2. INSURANCE. Both asks reached templates with no insurance book at all -
        //      `ft4` on the word `payment`, `vault` on `oracle` - and the build that
        //      followed the first drained twice. They reach their own template now.
        listOf(
            "an insurance pool funded by premium payments",
            "a parametric insurance payout on an oracle trigger",
            "an insurance pool with claims"
        ).forEach {
            assertEquals("insurance", DappScaffold.closestTemplate(it), it)
            assertTrue(note(it).contains("ONE HELPER THAT RETURNS A PREMIUM"), "$it must arrive with the guard")
            assertTrue(note(it).contains("NOTHING IS PAID INSIDE A CLAIM"), "$it must arrive with the other one")
        }

        // 3. THE LOTTERY. Round 17's finding was that this ask reached `staking` on the
        //    word `rewards`, and staking's guards are about a reward pool being FUNDED
        //    before it pays - nothing in it makes a draw unpredictable. Round 17 turned it
        //    into a NO that named the missing guard; round 20 BUILT that guard as the
        //    sixteenth template, after building this project's own design note for the
        //    class and watching it drained five of five draws. The ask must never reach
        //    `staking` again, and it no longer has to be a refusal to avoid it.
        val lottery = note("a weekly lottery with rewards for ticket holders")
        assertEquals("raffle", DappScaffold.closestTemplate("a weekly lottery with rewards for ticket holders"))
        assertTrue(lottery.startsWith("Use `template=raffle`"), lottery.take(120))
        assertTrue(lottery.contains("commit-reveal") || lottery.contains("COMMIT order"), lottery.take(400))
        // ...and the guard round 17 asked for is what the template ships, named here so
        // this row still measures the GUARD and not just a destination.
        assertTrue(lottery.contains("COMMIT order"), "the seed must be described, not just promised")
        assertTrue(lottery.contains("DENY a round by not revealing"), "the residual must still be stated")

        // 4. THE AIRDROP keeps `staking`, whose guards DO cover its exploit - a reward
        //    paid out of a pool nobody funded is round 4 - and is told what they do not.
        assertEquals("staking", DappScaffold.closestTemplate("a token airdrop with a claim window"))
        assertTrue(note("a token airdrop with a claim window").contains("there is no CLAIM WINDOW in it"))

        // 5. LIQUIDITY MINING is a reward EMISSION. It reached `amm` because `liquidity`
        //    is an unstarred key in that list and the amm branch precedes staking.
        assertEquals("staking", DappScaffold.closestTemplate("a liquidity mining program that pays rewards"))
        assertTrue(
            note("a liquidity mining program that pays rewards").contains("NOT `template=amm`"),
            "the compound ask must be told which half is which"
        )
        // ...and a plain liquidity ask still means the curve.
        assertEquals("amm", DappScaffold.closestTemplate("a liquidity pool"))

        // 6-7. THE PAYMENT CHANNEL and THE MULTISIG WALLET both reached `ft4`, a token
        //      ledger with neither a channel nor a signer set.
        listOf("a payment channel", "a multisig wallet").forEach {
            assertEquals(null, DappScaffold.closestTemplate(it), it)
            assertTrue(note(it).startsWith("No shipped template covers that name"), note(it).take(120))
        }
        assertTrue(note("a payment channel").contains("its exploit class is the CLOSE"))
        assertTrue(note("a multisig wallet").contains("chr_multi_signature_help"), "a NO must point at what DOES exist")
        assertTrue(
            note("a multisig wallet").contains("`template=bridge` ships exactly that shape"),
            "...including the shipped M-of-N shape, named as a shape rather than as a substitute"
        )

        // AND THE PLURAL. `otc` carried no star, so "OTCs desk" fell through to the
        // roster - the one ask in the thirty-four that missed for spelling alone.
        assertEquals("escrow", DappScaffold.closestTemplate("OTCs desk"))
        assertEquals("escrow", DappScaffold.closestTemplate("otc"))
    }

    /**
     * THE ROSTER IS DERIVED. The honest answer used to open "The ELEVEN hardened ones
     * are" and list eleven by hand; `escrow` - the fourteenth template - was not among
     * them, five weeks after `docs/TEMPLATE-GAPS.md` wrote the rule that a template's
     * redirect moves in the same commit as the template. So the sentence is built from
     * `templates` and from one map, and this pins that it cannot go stale again: a
     * template with no class line does not load, and every template is named with its
     * class in the note an unmatched ask actually receives.
     */
    @Test
    fun `the honest answer names every shipped template with its class, and counts them`() {
        val note = DappScaffold.closestTemplateNote("una boveda con oraculo")
        assertTrue(note.startsWith("No shipped template covers that name"), note.take(120))
        DappScaffold.templates.forEach {
            assertTrue(note.contains("`$it`"), "the roster must name $it: $note")
        }
        assertEquals(
            DappScaffold.templates.size - 2,
            DappScaffold.hardenedTemplates().size,
            "`hello` and `ft4` are named separately; everything else is hardened"
        )
        assertTrue(
            note.contains("The ${DappScaffold.countWord(DappScaffold.hardenedTemplates().size)} hardened ones are"),
            note.take(200)
        )
        assertFalse(note.contains("The eleven hardened ones"), "the count round 17 measured wrong")
        DappScaffold.hardenedTemplates().forEach {
            assertTrue(
                note.contains("`$it` (${DappScaffold.templateClasses.getValue(it)})"),
                "a name without its exploit class is what round 8 drained: $it"
            )
        }
        // The map is the single source, and nothing may be in it that is not a template.
        assertEquals(DappScaffold.templates.toSet(), DappScaffold.templateClasses.keys)
        // ...and the count word is a word, not a wrong word.
        assertEquals("thirteen", DappScaffold.countWord(13))
        assertEquals("21", DappScaffold.countWord(21))
    }
}
