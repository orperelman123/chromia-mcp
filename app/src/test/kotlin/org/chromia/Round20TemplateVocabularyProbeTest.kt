package org.chromia

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

/**
 * ROUND 20, THE CONCEPT VOCABULARY IN THE DIRECTION IT OPENED.
 *
 * Round 19 replaced a list of WORDS with a list of CONCEPTS: each uncovered
 * class carries every ordinary English name for itself plus the same class in
 * seven languages, folded so an accent is not a new word - and the uncovered
 * classes are read BEFORE the routing `when`, so a compound ask is answered by
 * the uncovered half whatever else it names. That order is round 18's fix and it
 * is right; what round 20 measures is what the widened vocabulary now takes with
 * it.
 *
 * `random`, `at random`, `randomly*`, `randomness`, `giveaway*` and `vrf` are
 * keys of the unpredictable-outcome class and are also ordinary words about six
 * classes this server SHIPS A TEMPLATE FOR. Because the uncovered list wins,
 * each of the seven asks below is answered "No shipped template covers that
 * name" with NOTHING scaffolded - including a stablecoin ask whose own words are
 * that it does not use a VRF.
 *
 * Two more rows measure what `docs/TEMPLATE-GAPS.md` records as still open,
 * rather than repeating it: an ask that names the raffle class in a non-Latin
 * script inside an English sentence is SCAFFOLDED (`ok:true`, `template=ft4`),
 * and so is the loyalty/points row that has no named refusal
 * (`template=staking`, the template whose "every credit is a pool debit" that
 * row says cannot be carried over).
 *
 * The 38 asks were measured end to end through the shipped jar over stdio first
 * (`exploit-corpus/realworld/adversary-round20/harness/redirect_r20.py`,
 * `redirect/redirect-probes.json`); this drives the same asks in process, which
 * is where `closestTemplate` and `closestTemplateNote` live, and freezes the
 * answer.
 *
 * A row here going red means the finding was CLOSED: flip it and say so, the way
 * a corpus GAP row is flipped to CAUGHT.
 */
class Round20TemplateVocabularyProbeTest {

    /** The sentence the unpredictable-outcome class paragraph owns. */
    private val classParagraph = "an UNPREDICTABLE OUTCOME"

    /** (ask, the class it really is, the key that took it) */
    private val falseDeclines = listOf(
        Triple("a price oracle vault that samples its feed at random intervals", "vault", "at random"),
        Triple("a staking pool whose validator is chosen at random each epoch", "staking", "chosen at random*"),
        Triple("a marketplace with a giveaway of free listings for new sellers", "marketplace", "giveaway*"),
        Triple("a stablecoin whose peg is deterministic and uses no VRF", "stablecoin", "vrf"),
        Triple("an order book exchange that assigns order ids randomly", "exchange", "randomly*"),
        Triple("a governance DAO that picks a proposal at random for audit", "governance", "at random"),
        Triple("a vault that reads a randomness beacon as a price input", "vault", "randomness")
    )

    /** Covered asks that must keep routing - round 19's clean pass, extended. */
    private val cleanPasses = listOf(
        "a lending market for abetting collateral positions" to "lending",
        "an escrow between a buyer and a seller with a deadline" to "escrow",
        "a token with transfers and balances" to "ft4",
        "a staking pool that pays rewards from a funded pool" to "staking",
        "a governance DAO with proposals and a quorum" to "governance",
        "a stablecoin with a collateral peg" to "stablecoin",
        "a marketplace with listings and escrowed payment" to "marketplace",
        "an insurance pool funded by premium payments" to "insurance",
        "a cross-chain token bridge for wrapped assets" to "bridge",
        "a subscription with recurring pull billing" to "subscription",
        "an order book exchange with limit orders" to "exchange",
        "a constant product amm with a liquidity pool" to "amm",
        "a vesting stream that pays a beneficiary over time" to "streaming",
        "a price oracle vault with bounded updates" to "vault"
    )

    @Test
    fun `seven asks for classes that ship a template are declined as an unpredictable outcome`() {
        val rows = buildJsonArray {
            falseDeclines.forEach { (ask, really, key) ->
                val target = DappScaffold.closestTemplate(ask)
                val note = DappScaffold.closestTemplateNote(ask)
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("the_class_it_really_is", really)
                        put("the_key_that_took_it", key)
                        put("template", target ?: "")
                        put("names_the_uncovered_class", note.contains(classParagraph))
                    }
                )
            }
        }
        Round20Evidence.record("redirect/false-declines.json", rows)
        val stillDeclined = falseDeclines.filter { (ask, _, _) ->
            DappScaffold.closestTemplate(ask) == null &&
                DappScaffold.closestTemplateNote(ask).contains(classParagraph)
        }.map { it.first }
        assertAll(
            Executable {
                assertEquals(
                    falseDeclines.map { it.first },
                    stillDeclined,
                    "one of round 20's false declines is CLOSED - an ask for a class this server ships a " +
                        "template for is no longer answered as an uncovered class. Flip the row and say so."
                )
            },
            Executable { Round20Evidence.assertFrozen("redirect/false-declines.json", rows) }
        )
    }

    @Test
    fun `the clean pass still routes every covered ask to its own template`() {
        val wrong = cleanPasses.mapNotNull { (ask, expected) ->
            val target = DappScaffold.closestTemplate(ask)
            if (target == expected) null else "'$ask' -> ${target ?: "(declined)"}, expected $expected"
        }
        assertEquals(
            emptyList<String>(),
            wrong,
            "a covered ask stopped routing to its template:\n" + wrong.joinToString("\n")
        )
    }

    /**
     * The two rows `docs/TEMPLATE-GAPS.md` records as still open, MEASURED. Both
     * are laundering in the round-18 sense: files, `ok:true`, and no word about
     * the class that has no guards.
     */
    @Test
    fun `a non latin word and the loyalty row are both scaffolded rather than named`() {
        val cyrillic = "a weekly лотерея for token holders"
        val loyalty = "a points program where the issuer mints rewards for purchases"
        val rows = buildJsonArray {
            listOf(cyrillic to "the raffle class named in Cyrillic inside an English ask",
                loyalty to "the loyalty/points row, which has no entry in untemplatedClasses").forEach { (ask, why) ->
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("why", why)
                        put("template", DappScaffold.closestTemplate(ask) ?: "")
                        put("names_the_uncovered_class", DappScaffold.closestTemplateNote(ask).contains(classParagraph))
                    }
                )
            }
        }
        Round20Evidence.record("redirect/laundered.json", rows)
        assertAll(
            Executable {
                assertEquals(
                    "ft4",
                    DappScaffold.closestTemplate(cyrillic),
                    "the non-Latin gap is CLOSED - `$cyrillic` is no longer scaffolded onto ft4. Flip the row."
                )
            },
            Executable {
                assertEquals(
                    "staking",
                    DappScaffold.closestTemplate(loyalty),
                    "the loyalty/points row has a named refusal now - it is no longer scaffolded onto staking. " +
                        "Flip the row."
                )
            },
            Executable { Round20Evidence.assertFrozen("redirect/laundered.json", rows) }
        )
    }
}
