"""ROUND 20 FIX LANE - the ordinary-asks routing rows, moved the way round 17 moved its own.

`ordinaryAsksReachTheTemplateForTheirExploitClass` carries a list of asks whose honest
answer is "nothing covers this", and beside it a block that opens *"ROUND 17'S OWN CLASS,
which used to be in the list above: it has a template now, and TEMPLATE-GAPS.md's rule is
that the redirect moves in the same commit."* Round 20's class is in exactly that position,
so it moves exactly that way - out of the NO list, into a block that asserts the template
AND the guard it arrives with, because a route without its guard is the round-8 hazard.

The asks that stay in the NO list are the ones that still have no template: the WAGERING
half of the class (a book must be solvent for every outcome at once), a loyalty programme,
a payment channel and a multisig wallet.
"""
import io
from pathlib import Path

P = (Path(__file__).resolve().parent.parent
     / "app/src/test/kotlin/org/chromia/DappScaffoldSecureTemplatesTest.kt")
raw = io.open(P, encoding="utf-8", newline="").read()
NL = "\r\n" if "\r\n" in raw else "\n"
text = raw.replace("\r\n", "\n")


def sub(old, new, why):
    global text
    assert old in text, "anchor missing: " + why
    assert text.count(old) == 1, "anchor not unique: " + why
    text = text.replace(old, new)


sub(
    '''        listOf(
            "a raffle with on-chain randomness",
            "a prediction market",
            "a loyalty programme",
            // ROUND 17's three measured misroutes whose honest answer is NO: a lottery
            // reached `staking` on the word `rewards` (nothing in it makes a draw
            // unpredictable), and a payment channel and a multisig wallet both reached
            // `ft4` (which has neither a channel nor a signer set).
            "a weekly lottery with rewards for ticket holders",
            "a payment channel",
            "a multisig wallet"
        ).forEach { ask ->
            assertNull(DappScaffold.closestTemplate(ask), "'$ask' has no template and must not be given one")
            assertTrue(noteFor(ask).contains("No shipped template covers that name"), ask)
        }
        // ...and each honest NO names the guard that is missing, rather than shrugging.
        assertTrue(
            noteFor("a weekly lottery with rewards for ticket holders").contains("UNPREDICTABLE OUTCOME"),
            noteFor("a weekly lottery with rewards for ticket holders").take(200)
        )
        assertTrue(
            noteFor("a multisig wallet").contains("SIGNER SET"),
            noteFor("a multisig wallet").take(200)
        )''',
    '''        listOf(
            // ROUND 20 SPLIT THE UNPREDICTABLE-OUTCOME CLASS IN TWO, and this is the half
            // that still has no template: a raffle pays ONE winner out of a pot the
            // entrants funded, and a BOOK takes the other side of every bet and must be
            // SOLVENT FOR EVERY OUTCOME AT ONCE. Nothing here covers that.
            "a prediction market",
            "sports betting",
            "a loyalty programme",
            // ROUND 20's other two: a points programme MINTS its unit, so staking's
            // "every credit is a pool debit" is vacuous on it; and a fee splitter's
            // exploit is the weights and the rounding.
            "a points program where the issuer mints rewards for purchases",
            "a fee splitter that pays revenue to weighted recipients",
            // ROUND 17's two remaining measured misroutes: a payment channel and a
            // multisig wallet both reached `ft4`, which has neither a channel nor a
            // signer set.
            "a payment channel",
            "a multisig wallet"
        ).forEach { ask ->
            assertNull(DappScaffold.closestTemplate(ask), "'$ask' has no template and must not be given one")
            assertTrue(noteFor(ask).contains("No shipped template covers that name"), ask)
        }
        // ...and each honest NO names the guard that is missing, rather than shrugging.
        assertTrue(
            noteFor("a prediction market").contains("UNPREDICTABLE OUTCOME"),
            noteFor("a prediction market").take(200)
        )
        assertTrue(
            noteFor("a prediction market").contains("SOLVENT FOR EVERY OUTCOME AT ONCE"),
            "a book's missing guard is solvency, and it must be the thing that is named"
        )
        assertTrue(
            noteFor("a multisig wallet").contains("SIGNER SET"),
            noteFor("a multisig wallet").take(200)
        )
        // ROUND 20'S OWN CLASS, which used to be in the list above: it has a template now,
        // and TEMPLATE-GAPS.md's rule is that the redirect moves in the same commit. Round
        // 17 measured "a weekly lottery with rewards for ticket holders" reaching `staking`
        // on the word `rewards`, and round 19 measured "a tombola" reaching `ft4` with four
        // files; neither template makes a draw unpredictable. Round 20 built the one that
        // does - after building this project's own design note for the class and watching
        // it drained FIVE OF FIVE draws on 4.76% of the stake.
        listOf(
            "a raffle with on-chain randomness",
            "a weekly lottery with rewards for ticket holders",
            "a tombola for token holders that pays out weekly",
            "on-chain lottery",
            "une loterie hebdomadaire avec tirage au sort"
        ).forEach { assertRoute(it, "raffle") }
        assertTrue(
            noteFor("a raffle with on-chain randomness").contains("IN COMMIT ORDER"),
            "...and it must arrive with the guard, not just the template name: " +
                noteFor("a raffle with on-chain randomness").take(200)
        )
        assertTrue(
            noteFor("a raffle with on-chain randomness").contains("DENY a round by not revealing"),
            "a redirect that does not name its own residual is the round-8 disclaimer"
        )''',
    "ordinary-asks NO list",
)

io.open(P, "w", encoding="utf-8", newline=NL).write(text)
print("DappScaffoldSecureTemplatesTest: the raffle rows moved, the wagering half stayed")
