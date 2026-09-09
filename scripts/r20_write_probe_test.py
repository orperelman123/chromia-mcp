"""ROUND 20 FIX LANE - rewrite Round20TemplateVocabularyProbeTest in its CLOSED form.

The adversary round wrote every row asserting the finding was STILL THERE, with a message
on each one saying "a row here going red means the finding was CLOSED: flip it and say
so". This flips them.

The nine non-Latin asks are spliced in FROM THE FROZEN EVIDENCE as \\uXXXX escapes rather
than retyped: a transcription error in a Chinese or Arabic literal would be invisible in
review and would silently turn a measurement into a different measurement.
"""
import io
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROBES = ROOT / "app/src/test/resources/exploit-corpus/realworld/adversary-round20/redirect/redirect-probes.json"
OUT = ROOT / "app/src/test/kotlin/org/chromia/Round20TemplateVocabularyProbeTest.kt"

rows = json.loads(PROBES.read_text(encoding="utf-8"))
non_latin = {r["probe"]: r["ask"] for r in rows if r["kind"] == "non-latin"}
assert len(non_latin) == 9, non_latin.keys()


def kt(s):
    """A Kotlin string literal, ASCII-only: every non-ASCII character as \\uXXXX."""
    out = ['"']
    for ch in s:
        if ch == '"':
            out.append('\\"')
        elif ch == "\\":
            out.append("\\\\")
        elif ch == "$":
            out.append("${'$'}")
        elif 0x20 <= ord(ch) < 0x7F:
            out.append(ch)
        else:
            assert ord(ch) <= 0xFFFF, ch
            out.append("\\u%04x" % ord(ch))
    out.append('"')
    return "".join(out)


def listing(probes, indent):
    return ("\n" + " " * indent).join(
        '%s to %s,' % (kt(p), kt(non_latin[p])) for p in probes
    ).rstrip(",")


TRANSLITERATED = ["a4_russian", "a5_greek", "a9_mixed_script"]
STILL_INVISIBLE = ["a1_chinese", "a2_japanese", "a3_korean", "a6_hebrew", "a7_arabic", "a8_hindi"]
assert sorted(TRANSLITERATED + STILL_INVISIBLE) == sorted(non_latin)

BODY = '''package org.chromia

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

/**
 * ROUND 20, THE CONCEPT VOCABULARY - THE FINDINGS, AND WHAT THEY BECAME.
 *
 * The adversary round wrote this class with every row asserting that the finding was STILL
 * THERE, and a message on each one saying *"a row here going red means the finding was
 * CLOSED: flip it and say so"*. They are flipped, and each row now asserts the fix.
 *
 * The round's own measurement of the bug is untouched beside it: the frozen
 * `redirect/false-declines.json`, `redirect/laundered.json` and
 * `redirect/redirect-probes.json` are the adversary round's record and nothing here
 * rewrites them. These rows record to NEW frozen files, so the before and the after are
 * both on disk and neither of them is a claim.
 *
 * WHAT WAS WRONG. Round 19 replaced a list of WORDS with a list of CONCEPTS, and round 18
 * put the uncovered classes BEFORE the routing `when` so a compound ask is answered by the
 * uncovered half whatever else it names. Both are right. Together they fired on a MENTION:
 * `random`, `at random`, `randomly*`, `randomness`, `giveaway*` and `vrf` were keys of the
 * unpredictable-outcome class AND ordinary words about six classes this server ships a
 * template for, so seven such asks were refused outright with NOTHING scaffolded -
 * including a stablecoin ask whose own words are that it does not use a VRF.
 *
 * WHAT IS THERE NOW. A class is recognised when the ask is ABOUT it: a key inside the head
 * noun phrase answers the ask, the class named by name AFTER the head is a compound ask, a
 * merely WEAK key after the head becomes a note beside the scaffold, and a key inside a
 * negation window is the class being ruled out. The raffle half of the class has a
 * template now - the sixteenth - so a raffle ask SCAFFOLDS; the wagering half, which must
 * be solvent for every outcome at once, still declines by name.
 *
 * Every ask below was measured end to end through the shipped jar over stdio
 * (`adversary-round20/harness/redirect_r20.py`); this drives the same asks in process,
 * which is where `closestTemplate` and `closestTemplateNote` live. The nine non-Latin
 * literals are generated from the frozen evidence by `scripts/r20_write_probe_test.py`
 * and written as \\uXXXX escapes, because a mistyped character in a Chinese or Arabic
 * literal is invisible in review and would silently change what is being measured.
 */
class Round20TemplateVocabularyProbeTest {

    /** The sentence the wagering (unpredictable-outcome) class paragraph owns. */
    private val classParagraph = "an UNPREDICTABLE OUTCOME"

    /** The sentence the mention tail owns - a note BESIDE a scaffold, never instead of one. */
    private val mentionTail = "NAMED RATHER THAN SWALLOWED"

    /** (ask, the class it really is, the key that used to take it) */
    private val falseDeclines = listOf(
        Triple("a price oracle vault that samples its feed at random intervals", "vault", "at random"),
        Triple("a staking pool whose validator is chosen at random each epoch", "staking", "chosen at random*"),
        Triple("a marketplace with a giveaway of free listings for new sellers", "marketplace", "giveaway*"),
        Triple("a stablecoin whose peg is deterministic and uses no VRF", "stablecoin", "vrf"),
        Triple("an order book exchange that assigns order ids randomly", "exchange", "randomly*"),
        Triple("a governance DAO that picks a proposal at random for audit", "governance", "at random"),
        Triple("a vault that reads a randomness beacon as a price input", "vault", "randomness")
    )

    /** The ask whose own words rule the class out - it has no mention to name. */
    private val negatedAsk = "a stablecoin whose peg is deterministic and uses no VRF"

    /**
     * THE CLEAN PASS: round 19's fourteen, plus the seven that used to be declined, plus
     * the four raffle asks the sixteenth template now covers. Twenty-five asks that must
     * route to a template, and one of them is here BECAUSE it says `no VRF`.
     */
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
    ) + falseDeclines.map { (ask, really, _) -> ask to really } + listOf(
        // The sixteenth template's own asks. Every one of these used to be a refusal.
        "a weekly raffle for token holders" to "raffle",
        "a lottery that pays a random winner from the prize pool" to "raffle",
        "a tombola for token holders that pays out weekly" to "raffle",
        "build me a commit-reveal raffle with a deposit" to "raffle"
    )

    /** The three non-Latin asks a settled transliteration reaches. */
    private val transliterated = listOf(
        %(TRANSLITERATED)s
    )

    /**
     * The six with no single-valued letter-for-letter Latin form - Chinese and Japanese
     * are not alphabetic, Korean is syllabic, and Hebrew and Arabic drop the vowels a key
     * would need. They are the recorded gap in `docs/TEMPLATE-GAPS.md`, MEASURED here
     * rather than restated.
     */
    private val stillInvisible = listOf(
        %(STILL_INVISIBLE)s
    )

    /**
     * FINDING C, CLOSED. Seven asks for classes this server ships a template for now
     * scaffold that template - and six of the seven carry the MENTION as a note, so the
     * word is named rather than swallowed. The seventh is the stablecoin: `uses no VRF`
     * is a negation, so there is nothing to name.
     */
    @Test
    fun `the seven false declines now scaffold their own template, with the mention named beside it`() {
        val rows = buildJsonArray {
            falseDeclines.forEach { (ask, really, key) ->
                val target = DappScaffold.closestTemplate(ask)
                val note = DappScaffold.closestTemplateNote(ask)
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("the_class_it_really_is", really)
                        put("the_key_that_used_to_take_it", key)
                        put("template", target ?: "")
                        put("declined_as_an_uncovered_class", note.contains(classParagraph))
                        put("names_the_mention", note.contains(mentionTail))
                    }
                )
            }
        }
        Round20Evidence.record("redirect/false-declines-closed.json", rows)
        val wrong = falseDeclines.mapNotNull { (ask, really, _) ->
            val target = DappScaffold.closestTemplate(ask)
            if (target == really) null else "'" + ask + "' -> " + (target ?: "(declined)") + ", expected " + really
        }
        assertAll(
            Executable {
                assertEquals(
                    emptyList<String>(),
                    wrong,
                    "round 20's false declines are back:\\n" + wrong.joinToString("\\n")
                )
            },
            Executable {
                falseDeclines.forEach { (ask, _, _) ->
                    assertTrue(
                        !DappScaffold.closestTemplateNote(ask).contains(classParagraph),
                        "'" + ask + "' is answered as an uncovered class again, and it is not one"
                    )
                }
            },
            Executable {
                assertTrue(
                    !DappScaffold.closestTemplateNote(negatedAsk).contains(mentionTail),
                    "an ask that says it does NOT use a VRF must not be annotated as though it did"
                )
                falseDeclines.map { it.first }.filter { it != negatedAsk }.forEach { ask ->
                    assertTrue(
                        DappScaffold.closestTemplateNote(ask).contains(mentionTail),
                        "'" + ask + "' scaffolds, but the word that used to refuse it is swallowed in silence"
                    )
                }
            },
            Executable { Round20Evidence.assertFrozen("redirect/false-declines-closed.json", rows) }
        )
    }

    @Test
    fun `the clean pass still routes every covered ask to its own template`() {
        val wrong = cleanPasses.mapNotNull { (ask, expected) ->
            val target = DappScaffold.closestTemplate(ask)
            if (target == expected) null else "'" + ask + "' -> " + (target ?: "(declined)") + ", expected " + expected
        }
        assertEquals(
            emptyList<String>(),
            wrong,
            "a covered ask stopped routing to its template:\\n" + wrong.joinToString("\\n")
        )
    }

    /**
     * FINDING A, CLOSED FOR THE TWO SCRIPTS THAT HAVE A SETTLED LATIN FORM, and MEASURED
     * for the six that do not. All nine of round 20's non-Latin asks, re-run: 0 of 9
     * reached their class, and 3 of 9 do now.
     */
    @Test
    fun `the nine non latin asks, re-measured after the fold`() {
        val rows = buildJsonArray {
            (transliterated + stillInvisible).forEach { (probe, ask) ->
                add(
                    buildJsonObject {
                        put("probe", probe)
                        put("template", DappScaffold.closestTemplate(ask) ?: "")
                        put("reaches_its_class", DappScaffold.closestTemplate(ask) == "raffle")
                    }
                )
            }
        }
        Round20Evidence.record("redirect/non-latin-closed.json", rows)
        assertAll(
            Executable {
                transliterated.forEach { (probe, ask) ->
                    assertEquals(
                        "raffle",
                        DappScaffold.closestTemplate(ask),
                        probe + " names the raffle class and must reach it through NON_LATIN_FOLD"
                    )
                }
            },
            Executable {
                stillInvisible.forEach { (probe, ask) ->
                    assertEquals(
                        null,
                        DappScaffold.closestTemplate(ask),
                        probe + " now reaches a template - the non-Latin gap narrowed. Flip this row and " +
                            "update docs/TEMPLATE-GAPS.md in the same commit."
                    )
                }
            },
            Executable { Round20Evidence.assertFrozen("redirect/non-latin-closed.json", rows) }
        )
    }

    /**
     * FINDING B, CLOSED. The mixed-script ask and the loyalty/points ask were both
     * SCAFFOLDED - files, `ok:true`, and no word about the class with no guards. Each is
     * now answered by its own class: the raffle one has a template, the loyalty one has a
     * named refusal.
     */
    @Test
    fun `the two laundered asks are each answered by their own class`() {
        val cyrillic = transliterated.single { it.first == "a9_mixed_script" }.second
        val loyalty = "a points program where the issuer mints rewards for purchases"
        val loyaltyClass = DappScaffold.untemplatedClasses.single { it.id == "loyalty-points" }
        val rows = buildJsonArray {
            listOf(
                cyrillic to "the raffle class named in Cyrillic inside an English ask",
                loyalty to "the loyalty/points row, which now has a class of its own"
            ).forEach { (ask, why) ->
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("why", why)
                        put("template", DappScaffold.closestTemplate(ask) ?: "")
                        put("names_its_own_class", DappScaffold.closestTemplateNote(ask).contains(loyaltyClass.note))
                    }
                )
            }
        }
        Round20Evidence.record("redirect/laundered-closed.json", rows)
        assertAll(
            Executable {
                assertEquals(
                    "raffle",
                    DappScaffold.closestTemplate(cyrillic),
                    "the mixed-script ask was ok:true / template=ft4 / four files; it is a raffle ask"
                )
            },
            Executable {
                assertEquals(
                    null,
                    DappScaffold.closestTemplate(loyalty),
                    "'" + loyalty + "' was ok:true / template=staking / three files - the template whose " +
                        "'every credit is a pool debit' is VACUOUS on an issuer that mints"
                )
            },
            Executable {
                val note = DappScaffold.closestTemplateNote(loyalty)
                assertTrue(note.startsWith(loyaltyClass.note), "the loyalty class must be named FIRST: " + note.take(200))
                assertTrue(note.contains(loyaltyClass.missingGuard), "the missing guard must be named: " + note)
                assertTrue(note.contains("NOTHING WAS SCAFFOLDED"), note.takeLast(400))
                // ...and the covered half it also names is still named by template.
                assertTrue(note.contains("`template=staking`"), "the covered half must still be named: " + note)
            },
            Executable { Round20Evidence.assertFrozen("redirect/laundered-closed.json", rows) }
        )
    }

    /**
     * THE OTHER FOUR ROWS OF FINDING B, and they are not all closed. The fee-splitter
     * family has a class now; the gaming-item-shop row still has none, and that is
     * recorded rather than glossed.
     */
    @Test
    fun `the fee splitter family is answered by name and the gaming item row is still the gap`() {
        val splitter = DappScaffold.untemplatedClasses.single { it.id == "fee-splitter" }
        listOf(
            "a fee splitter that pays revenue to weighted recipients",
            "a charity donation pool split between causes by weight",
            "a yield aggregator that splits the fees it earns"
        ).forEach { ask ->
            val note = DappScaffold.closestTemplateNote(ask)
            assertEquals(null, DappScaffold.closestTemplate(ask), "'" + ask + "' has no template and must decline")
            assertTrue(note.startsWith(splitter.note), "'" + ask + "' must be answered by its own class: " + note.take(160))
            assertTrue(note.contains(splitter.missingGuard), "'" + ask + "' must name the missing guard")
        }
        // Still the gap: no class, no named refusal, the generic roster.
        val itemShop = "a gaming item shop that issues in-game items"
        assertEquals(null, DappScaffold.closestTemplate(itemShop))
        assertTrue(
            DappScaffold.closestTemplateNote(itemShop).contains("Pick the one whose EXPLOIT class matches yours"),
            "the gaming-item row is still the roster answer - if that changed, flip this row and update " +
                "docs/TEMPLATE-GAPS.md in the same commit"
        )
    }

    /**
     * THE MIXED ASK IS STILL A NO. Round 18's rule is unchanged by any of this: an ask
     * that names two classes is built one template at a time and nothing is scaffolded for
     * it. What changed is that BOTH halves have a template now, so the answer says "ask
     * for each of these" instead of "this half has no guards at all".
     */
    @Test
    fun `an ask that names two covered classes still scaffolds nothing`() {
        listOf(
            "a lending pool that also runs a weekly raffle for depositors",
            "an amm that pays a lottery jackpot from the swap fee",
            "a lending pool with a weekly raffle, a cross-chain bridge and an order book"
        ).forEach { ask ->
            assertEquals(null, DappScaffold.closestTemplate(ask), "'" + ask + "' is a compound ask and scaffolds nothing")
            val note = DappScaffold.closestTemplateNote(ask)
            assertTrue(
                note.contains("MORE THAN ONE COVERED CLASS") || note.contains("NOTHING WAS SCAFFOLDED"),
                "'" + ask + "' must say that nothing was scaffolded, and why: " + note.take(240)
            )
            assertTrue(note.contains("`template=raffle`"), "'" + ask + "' names a raffle: the template must be named")
        }
        // ...and a covered ask that RULES THE OTHER CLASS OUT is not a compound ask at all.
        assertEquals(
            "lending",
            DappScaffold.closestTemplate("a lending pool without any raffle"),
            "a negated class is the class being ruled out, not the class being named"
        )
    }
}
''' % {
    "TRANSLITERATED": listing(TRANSLITERATED, 8),
    "STILL_INVISIBLE": listing(STILL_INVISIBLE, 8),
}

assert all(ord(c) < 128 for c in BODY), "the generated test must be pure ASCII"
io.open(OUT, "w", encoding="utf-8", newline="\r\n").write(BODY)
print("wrote", OUT.name, len(BODY.splitlines()), "lines, ASCII-only")
