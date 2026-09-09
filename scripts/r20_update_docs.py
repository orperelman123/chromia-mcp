"""ROUND 20 FIX LANE - the documentation every change ships with.

docs/TEMPLATE-GAPS.md: the raffle row RETIRED (the rule this file states about itself -
"when a template lands, delete its row and fix its redirect in the same commit"), the two
rows that had no named refusal updated to say they have one, and the design-note section
rewritten to say what became of it, because it is the section that was drained.
"""
import io
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GAPS = ROOT / "docs/TEMPLATE-GAPS.md"

raw = io.open(GAPS, encoding="utf-8", newline="").read()
NL = "\r\n" if "\r\n" in raw else "\n"
text = raw.replace("\r\n", "\n")


def sub(old, new, why):
    global text
    assert old in text, "anchor missing: " + why
    assert text.count(old) == 1, "anchor not unique: " + why
    text = text.replace(old, new)


# --------------------------------------------------------- 1. the row, retired
RAFFLE_ROW_START = "| **a lottery, a raffle, a prize draw, a prediction market** - DRAINED ON A CHAIN, round 18 |"
i = text.index(RAFFLE_ROW_START)
j = text.index("\n", i) + 1
old_row = text[i:j]
new_row = (
    "| **a wager somebody takes the other side of** - a prediction market, a sportsbook, a "
    "casino game | `(none)` - a NAMED refusal, and since 2026-09-09 a refusal that points at "
    "`raffle` for the DRAW half and says why a book is not one | SOLVENCY FOR EVERY OUTCOME AT "
    "ONCE. A raffle pays one winner out of a pot the entrants funded, so the pot is always "
    "exactly what was staked; a book takes a position against every bettor and must be able to "
    "pay whichever side wins. `template=raffle` has no reserve, no odds and no worst case | a "
    "reserve sized against the EXPECTED outcome rather than the worst one, and a settlement "
    "source that is the house's own opinion. `insurance` is the nearest shipped shape - cover "
    "bounded by the reserve that backs it, every payout pro rata |\n"
)
text = text[:i] + new_row + text[j:]

sub(
    "| a loyalty programme, points, a gaming item shop | `(none)` | an issuer who can mint the points at will |",
    "| a loyalty programme, points | `(none)` - a NAMED refusal since 2026-09-09, where it fell "
    "to the roster before; **a gaming item shop still falls to the roster and is the row this "
    "one was split off from** | an issuer who can mint the points at will |",
    "loyalty row",
)

sub(
    "| a fee splitter, a charity or donation pool, a yield aggregator | `(none)` |",
    "| a fee splitter, a charity or donation pool, a yield aggregator | `(none)` - a NAMED "
    "refusal since 2026-09-09, where it fell to the roster before |",
    "fee splitter row",
)

# ------------------------------------------- 2. the design note, and what it cost
sub(
    """## The commit-reveal raffle, designed - what a future template lane builds

This is the design note the top row has been owed since round 18 drained the class on a
chain. It is a DESIGN, measured against the drain it has to make unwritable; it is not a
template, and this lane did not build it, because a template here is a main module, a
shipped suite, a guard-by-guard mutant for every guard, a redirect, module args and corpus
rows, all built and measured on a real chain the way the other fifteen were.
""",
    """## The commit-reveal raffle - BUILT, and first DRAINED FROM THIS SECTION

**STATUS, 2026-09-09: `template=raffle` ships, and the row above is retired.** Read the
rest of this section anyway, because it is the only place in this file where the ADVICE
was the attack surface.

Adversary round 20 did what this section asks a template lane to do: it built the design
below, exactly as written. Eleven honest guards went green on a real chain and every one
of them was proved load-bearing. Then an attacker holding **4.76% of the stake won FIVE OF
FIVE draws, with ZERO deposits forfeited and ZERO secrets withheld** - so the deposit, the
whole economic mechanism this section rests on, never came up. The sentence that failed is
in step 3 below: *"her choice is between REVEALING and FORFEITING her deposit"*. It is
true of ONE commitment and false of two, because the mix

    round.mixed = hash(round.mixed ++ secret)

folds the secrets in **REVEAL ORDER**. Six commitments are 720 orderings of the same
accumulator, every one of them reached by revealing everything and paying nothing extra.
She read the honest secrets off the chain, ran the module's own public `pick` over each
ordering of her own, and revealed in the one whose ticket was hers.

**WHAT THE SHIPPED TEMPLATE DOES INSTEAD**, and each of these is structural rather than a
check a later operation can forget:

1. **The seed folds the COMMITTED set, in COMMIT order.** `reveal` stores the secret and
   touches no accumulator at all; the seed is computed once, in `settle_round`, folding
   over `@sort .seq` - an immutable field written by `commit` before any secret in the
   round exists. There is no ordering left to choose.
2. **A round that is not complete does not draw.** `reveals == commits` or the round
   refunds, so the 2^m withholding subsets collapse into one choice: reveal everything, or
   there is nothing to win.
3. **A forfeited deposit is BURNED**, never added to the prize - the round-20 module paid
   forfeits into the pot, which pays the survivors for the denial.
4. **The stake is capped by the deposit**, so walking away from a losing draw never costs
   less than playing it. Round 20 sized its deposit against `prize / participants`, a
   fraction of one stake, which bounded nothing.

Measured on a chain, in `exploit-corpus/realworld/adversary-round20/raffle-fixed`: all 24
reveal orders of one round settle to the same seed and the same winner; and over FIFTY
rounds the same 720-permutation search still finds a winning ordering in 50 of 50 rounds
and wins **one round in fifty - 20000 ppm against a 47619 ppm stake share**, where the
drained module won every draw.

**THE LESSON THIS FILE OWES ITSELF:** a design note in this repository is read by agents
and built by them, so it is production advice with no test behind it. This one had eleven
provable guards and one false sentence, and the false sentence was the whole of it. A
future row here should carry the *invariant* it claims - "her expected value from
deviating is negative" - as something a chain can be made to answer, not as a sentence.

The original design follows, unchanged, so the note and its counterexample sit together.
""",
    "design note header",
)

io.open(GAPS, "w", encoding="utf-8", newline=NL).write(text)
print("docs/TEMPLATE-GAPS.md updated")
