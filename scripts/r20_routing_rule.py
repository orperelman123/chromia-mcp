"""ROUND 20 FIX LANE, part 2 - THE ROUTING RULE.

An uncovered class is recognised when the ask is ABOUT it, not when its word appears.
Measured in scripts/r20_routing_proto.py first: 120 distinct asks over every pinned
corpus, 0 mismatches, exactly the 20 intended flips.

    python scripts/r20_routing_rule.py
"""
import io
from pathlib import Path

KT = Path(__file__).resolve().parent.parent / "app/src/main/kotlin/org/chromia/tools/DappScaffold.kt"
text = io.open(KT, encoding="utf-8", newline="").read()
NL = "\r\n" if "\r\n" in text else "\n"
text = text.replace("\r\n", "\n")


def sub(old, new, why):
    global text
    assert old in text, "anchor missing: " + why
    assert text.count(old) == 1, "anchor not unique: " + why
    text = text.replace(old, new)


# ------------------------------------------------------- 1. the record and the list
sub(
    """    internal class UntemplatedClass(
        val id: String,
        val label: String,
        val keys: List<String>,
        val missingGuard: String,
        val note: String
    )

    internal val untemplatedClasses: List<UntemplatedClass> = listOf(
        UntemplatedClass(
            id = "unpredictable-outcome",
            label = "an unpredictable outcome (a raffle, a lottery, a prize draw, a prediction market)",
            keys = UNPREDICTABLE_OUTCOME_KEYS,""",
    """    /**
     * A VALUE CLASS THE ROUTING KNOWS BY CONCEPT, its whole-token keys, and the guard an
     * honest answer has to name.
     *
     * ROUND 20 SPLIT THIS RECORD IN TWO DIRECTIONS at once. [template] is the shipped
     * template for the class, or null when none covers it - the raffle class HAS one now,
     * and a record that could only say "no template" could not express that. [weakKeys]
     * are words that describe HOW the class works rather than WHAT is being built, and
     * they cannot take an ask away from a covered class on their own: that is the seven
     * false declines of round 20, section 3, made unwritable.
     */
    internal class ConceptClass(
        val id: String,
        val label: String,
        val keys: List<String>,
        val missingGuard: String,
        val note: String,
        /** Words that name the class only weakly - see [RAFFLE_WEAK_KEYS]. */
        val weakKeys: List<String> = emptyList(),
        /** The shipped template for this class, or null when none covers it. */
        val template: String? = null
    )

    /**
     * The classes the routing reads BEFORE the `when` below, covered and uncovered alike.
     * Order matters only for the answer's wording; membership is what the rule reads.
     */
    internal val conceptClasses: List<ConceptClass> = listOf(
        ConceptClass(
            id = "raffle",
            label = "a raffle or lottery (a draw with one winner)",
            keys = RAFFLE_KEYS,
            weakKeys = RAFFLE_WEAK_KEYS,
            template = "raffle",
            missingGuard = "a SEED NO PARTICIPANT CAN CHOOSE - and `template=raffle` now ships one",
            note = RAFFLE_NOTE
        ),
        ConceptClass(
            id = "unpredictable-outcome",
            label = "a wager on an unpredictable outcome (a prediction market, sports betting, a casino game)",
            keys = WAGERING_KEYS,""",
    "UntemplatedClass declaration and first entry",
)

for old_ctor in ("        UntemplatedClass(\n            id = \"payment-channel\",",
                 "        UntemplatedClass(\n            id = \"signer-set\",",
                 "        UntemplatedClass(\n            id = \"crowdfunding\","):
    sub(old_ctor, old_ctor.replace("UntemplatedClass(", "ConceptClass("), old_ctor[:60])

# the two new uncovered classes, and the derived view the round-18/19 tests read
sub(
    """                    "on it - which is what a partial refund is. Write the economic invariant test FIRST: a " +
                    "passing security check is not economic soundness."
        )
    )
""",
    """                    "on it - which is what a partial refund is. Write the economic invariant test FIRST: a " +
                    "passing security check is not economic soundness."
        ),
        ConceptClass(
            id = "loyalty-points",
            label = "a loyalty or points programme (points the issuer mints)",
            keys = LOYALTY_POINTS_KEYS,
            missingGuard = "WHAT BACKS A POINT AND WHAT RETIRES IT - a points programme MINTS its unit, so " +
                "there is no pool to debit and no conservation invariant to copy; the guard is a stated " +
                "liability, a redemption path that BURNS what it pays out, and a bound on what may be " +
                "minted per period",
            note =
                "No shipped template covers that name, and the nearest one is actively wrong for it. A " +
                    "LOYALTY OR POINTS PROGRAMME's exploit class is that the ISSUER MINTS THE UNIT: " +
                    "points appear on a purchase, out of nothing, and the only thing standing behind " +
                    "them is the issuer's promise to honour them. Round 20 measured `a points program " +
                    "where the issuer mints rewards for purchases` being answered `ok:true`, " +
                    "`template=staking`, THREE FILES - and `docs/TEMPLATE-GAPS.md` had already written " +
                    "down why that is the wrong answer: the staking template's discipline is that " +
                    "EVERY CREDIT IS A POOL DEBIT out of a sponsor-funded pool, which is exactly what a " +
                    "points programme does not do. Copy that invariant onto a minting issuer and it is " +
                    "VACUOUS - it passes on a ledger that can print. The nearest shipped disciplines, " +
                    "and they are halves rather than answers: `template=ft4` for a token whose supply " +
                    "is explicit and whose transfers conserve, and `template=subscription` for a " +
                    "merchant's claim bounded by what a payer actually funded. Write the invariant test " +
                    "FIRST, and make it about the LIABILITY - how many points exist, what they may be " +
                    "redeemed for, and what burns them - because a passing security check cannot see a " +
                    "promise."
        ),
        ConceptClass(
            id = "fee-splitter",
            label = "a splitter of income among weighted recipients (a fee splitter, a revenue share, a donation pool)",
            keys = FEE_SPLITTER_KEYS,
            missingGuard = "THE WEIGHTS AND THE ROUNDING - who may change a share and from when, and where " +
                "the remainder of a division goes; a split that pays `total * weight / total_weight` to " +
                "each recipient in turn leaves dust behind on every distribution and pays the last " +
                "recipient short, and a weight a later operation can move re-prices income that has " +
                "already been earned",
            note =
                "No shipped template covers that name. A SPLITTER OF INCOME AMONG WEIGHTED RECIPIENTS - " +
                    "a fee splitter, a revenue share, a donation pool, a charity - has two exploit " +
                    "classes and neither is about authentication. THE WEIGHTS ARE RE-PRICEABLE: if a " +
                    "share may be changed while income is already accrued, whoever may change it is " +
                    "paid retroactively, which is the round-9 accrual hazard in a different class. AND " +
                    "THE DIVISION HAS A REMAINDER: integer division loses dust on every distribution, " +
                    "so `sum of payouts < amount` and the difference accumulates somewhere - a pot with " +
                    "no owner, or the last recipient's shortfall. The nearest shipped disciplines, and " +
                    "they are halves rather than answers: `template=insurance` for the PRO-RATA payout " +
                    "of a pot that cannot cover everything claimed on it, which is the same arithmetic " +
                    "and ships the remainder handling; and `template=staking` for a per-share " +
                    "accumulator that pays a share of income WITHOUT re-pricing what is already accrued. " +
                    "Write the economic invariant test FIRST, and make it assert that the payouts SUM " +
                    "to the amount split - that is the assertion the dust hides from."
        )
    )

    /**
     * The classes with NO shipped template, derived from [conceptClasses] rather than
     * kept in a second list: a class that gains a template leaves this view in the same
     * commit, and there is no way to add one to the routing and forget the other.
     */
    internal val untemplatedClasses: List<ConceptClass>
        get() = conceptClasses.filter { it.template == null }
""",
    "crowdfunding entry tail",
)

io.open(KT, "w", encoding="utf-8", newline=NL).write(text)
print("concept classes written")
