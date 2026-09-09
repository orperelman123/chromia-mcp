"""ROUND 20 FIX LANE - the wagering class's answer, which used to be the raffle's.

`unpredictable-outcome` kept its id (round 18's and round 19's corpora are keyed on it)
but it is now the half of that class with NO template: a bet somebody takes the other
side of. Its note still described the raffle - the half that now HAS one - so it is
rewritten to the class it is actually about, and it points at `template=raffle` for the
draw rather than at `template=marketplace`'s escrow.
"""
import io
from pathlib import Path

KT = Path(__file__).resolve().parent.parent / "app/src/main/kotlin/org/chromia/tools/DappScaffold.kt"
text = io.open(KT, encoding="utf-8", newline="").read()
NL = "\r\n" if "\r\n" in text else "\n"
text = text.replace("\r\n", "\n")

OLD = '''            missingGuard = "a draw NO CALLER CAN CHOOSE THE BLOCK FOR - `op_context.last_block_time` is the " +
                "timestamp of the block ALREADY COMMITTED, so an attacker predicts nothing: she watches " +
                "blocks land and answers the one whose ticket is hers. The shape is a COMMIT-REVEAL with a " +
                "deposit the revealer forfeits, or an anchor on a FUTURE block read only after a delay, and " +
                "the economic invariant test written before either",
            note =
                "No shipped template covers that name, and the honest answer is NO rather than the " +
                    "nearest template: an ask that turns on an UNPREDICTABLE OUTCOME has an exploit " +
                    "class no template here addresses. `template=staking` is where round 17 measured " +
                    "\\"a weekly lottery with rewards for ticket holders\\" landing, on the word " +
                    "`rewards`, and its guards are about a reward pool being FUNDED before it pays - " +
                    "nothing in it makes a draw unpredictable, and a draw an operation's signer can " +
                    "predict is not a draw, it is a withdrawal. On this chain a block's own data " +
                    "(timestamp, block rid, a hash of anything in the transaction) is visible to " +
                    "whoever chooses when to submit, so it is not entropy. If you build one anyway: " +
                    "commit-reveal with a deposit the revealer forfeits, or an external randomness " +
                    "source with the same discipline the vault applies to a price - bounded, " +
                    "rate-limited, staleness-checked - and write the economic invariant test FIRST. " +
                    "The pot's CUSTODY is a different half and is covered: money in before the draw " +
                    "and out after it is `template=marketplace`'s escrow discipline or " +
                    "`template=insurance`'s pro-rata payout of a short pool, neither of which makes " +
                    "the outcome fair."'''

NEW = '''            missingGuard = "SOLVENCY FOR EVERY OUTCOME AT ONCE, and a settlement source that is not the " +
                "house's own opinion - a book must be able to pay whichever side wins, so the guard is a " +
                "reserve checked against the WORST outcome before a bet is accepted (never against the " +
                "expected one), plus an oracle or adjudicator with the discipline `template=vault` applies " +
                "to a price, and the economic invariant test written before either",
            note =
                "No shipped template covers that name, and the honest answer is NO rather than the " +
                    "nearest template: an ask that turns on WAGERING - taking the other side of a bet " +
                    "on an UNPREDICTABLE OUTCOME - has an exploit class no template here addresses. " +
                    "IT IS NOT THE SAME CLASS AS A DRAW, and round 20 separated the two: a raffle " +
                    "pays ONE winner out of a pot the entrants themselves funded, so the pot is " +
                    "always exactly what was staked, and `template=raffle` ships it. A BOOK IS THE " +
                    "OPPOSITE SHAPE - it takes a position against every bettor, and it must be " +
                    "SOLVENT FOR EVERY OUTCOME AT ONCE. Nothing in `template=raffle` says anything " +
                    "about that: it has no reserve, no odds, no exposure and no worst case, and " +
                    "building a sportsbook on it gives you a draw with extra steps. Two things have " +
                    "to be true and neither is a check you can add later. THE RESERVE IS SIZED " +
                    "AGAINST THE WORST OUTCOME, not the expected one: a bet accepted because the " +
                    "average case is covered is a bet the house cannot pay when the unlikely side " +
                    "wins, and the entrants find that out together. AND THE SETTLEMENT IS NOT THE " +
                    "HOUSE'S OPINION - whoever says which side won is the thing an attacker buys, so " +
                    "that source needs the discipline `template=vault` applies to a price: bounded, " +
                    "rate-limited, staleness-halted, and unable to move the answer in one step. On " +
                    "this chain a block's own data (timestamp, block rid, a hash of anything in the " +
                    "transaction) is visible to whoever chooses when to submit, so it is not entropy " +
                    "either. The nearest shipped disciplines, and they are halves rather than " +
                    "answers: `template=insurance` for a POOL THAT MUST COVER CLAIMS IT CANNOT ALL " +
                    "PAY - cover bounded by the reserve that backs it, and every payout pro rata - " +
                    "which is the closest thing here to a book's solvency problem; and " +
                    "`template=raffle` for the draw itself if what you are building really is a draw. " +
                    "Write the economic invariant test FIRST: a passing security check is not " +
                    "economic soundness, and on this class it is not even a signal."'''

assert OLD in text, "wagering note anchor"
text = text.replace(OLD, NEW)
io.open(KT, "w", encoding="utf-8", newline=NL).write(text)
print("wagering note rewritten")
