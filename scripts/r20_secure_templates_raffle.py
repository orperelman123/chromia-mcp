"""ROUND 20 FIX LANE - the sixteenth template's own rows in DappScaffoldSecureTemplatesTest.

`everyTemplateShipsAnExtendingSectionNamingItsOwnSeam` asserts that `extendingSeam`'s keys
EQUAL `DappScaffold.templates`, so a template cannot ship without its seam sentence pinned.
And every other template has a structural test that asserts the shape its drain needed and
the module does not have; this adds the raffle's.
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
    '        "insurance" to "EVERY PATH OUT OF A POLICY IS A SECOND EXIT PATH"\n    )',
    '        "insurance" to "EVERY PATH OUT OF A POLICY IS A SECOND EXIT PATH",\n'
    '        "raffle" to "THE SEED FOLDS IN COMMIT ORDER"\n    )',
    "extendingSeam map",
)

D = '$'          # written this way so the Kotlin below can carry ${...} unharmed
RAFFLE_TEST = '''    /**
     * THE SIXTEENTH TEMPLATE, STRUCTURALLY. Every assertion here is a shape adversary
     * round 20's drain needed and this module does not have. The drain was one line -
     * `round.mixed = crypto.sha256(round.mixed + secret)` inside `reveal` - so the first
     * assertion is that `reveal` touches no accumulator at all.
     */
    @Test
    fun raffleFoldsTheCommittedSetAndDrawsOnlyACompleteRound() {
        val files = DappScaffold.files("draw", template = "raffle")
        val main = files.getValue("src/main.rell")
        val code = withoutComments(main)
        assertEquals(10, guardCount(main), "the raffle header's stated count must be the number of guards it lists")

        // 1. THE SEED IS A FUNCTION OF THE COMMITTED SET, NOT OF THE REVEAL ORDER.
        val reveal = opBody(code, "reveal")
        assertFalse(reveal.contains("seed"), "reveal must not touch the seed: DOLLAR{reveal}")
        assertTrue(reveal.contains(".secret = secret"), "reveal stores the secret and stops: DOLLAR{reveal}")
        assertTrue(
            code.contains("entry @* { .round == r } ( @omit @sort .seq, .secret )"),
            "the seed must fold the entries in COMMIT order, which is `seq`"
        )
        // `seq` is written by commit and by nothing else, and it is not mutable: the set
        // the seed folds over is fixed before the first secret in the round exists.
        assertTrue(code.contains("seq: integer;"), "seq must be an immutable field")
        assertFalse(code.contains("mutable seq"), "a mutable commit order is not a commit order")
        listOf("reveal", "settle_round", "claim_refund").forEach { op ->
            assertFalse(opBody(code, op).contains(".seq ="), "DOLLAR{op} must not write seq")
        }

        // 2. A ROUND THAT IS NOT COMPLETE DOES NOT DRAW - what turns 2^m withholding
        //    subsets into one choice.
        val settle = opBody(code, "settle_round")
        assertTrue(settle.contains("r.reveals == r.commits"), "settle must refuse an incomplete round: DOLLAR{settle}")

        // 3. A FORFEITED DEPOSIT IS BURNED, AND NOTHING PAYS OUT OF IT.
        assertTrue(opBody(code, "claim_refund").contains("book.burned += e.deposit"), "a forfeit must be burned")
        assertFalse(settle.contains("burned"), "the prize must not grow with somebody else's forfeit")
        assertEquals(
            0,
            Regex("burned -=").findAll(code).count(),
            "nothing may pay out of `burned` - that is what makes denial unprofitable"
        )

        // 4. THE STAKE IS CAPPED BY THE DEPOSIT, so walking away never costs less than
        //    playing. Round 20 sized its deposit against the prize DIVIDED BY the
        //    participants, which is a fraction of one stake.
        assertTrue(
            opBody(code, "commit").contains("stake <= DEPOSIT"),
            "a stake above the deposit pays the last revealer to walk away"
        )

        // 5. NO OPERATION WRITES A TIMESTAMP AN ENTITLEMENT IS MEASURED FROM.
        assertTrue(code.contains("opened_at: timestamp;"), "opened_at must be immutable")
        listOf("commit", "reveal", "settle_round", "claim_refund").forEach { op ->
            assertFalse(
                opBody(code, op).contains(".opened_at ="),
                "DOLLAR{op} must not write opened_at - a window a caller can push is a window she can wait out"
            )
        }

        // 6. THE DRAW READS NO BLOCK DATA. The clock appears only in inequalities.
        assertTrue(settle.contains("pick(seed, r.pot)"), "the ticket comes from the seed and nothing else")
        assertFalse(settle.contains("pick(op_context"), settle)

        // 7. ...and the shipped suite RUNS round 20's search rather than describing it.
        val test = files.getValue("src/test/main_test.rell")
        assertTrue(test.contains("permutations("), "the suite must run the attacker's search")
        assertTrue(test.contains("test_round20_raffle1_"), test.take(200))
        assertTrue(test.contains("test_round20_raffle2_"), test.take(200))
    }

'''.replace("DOLLAR", D)

sub(
    "    @Test\n    fun templatesCompileWithVendoredLib() {",
    RAFFLE_TEST + "    @Test\n    fun templatesCompileWithVendoredLib() {",
    "templatesCompileWithVendoredLib anchor",
)

io.open(P, "w", encoding="utf-8", newline=NL).write(text)
print("extendingSeam + the raffle's structural test added")
