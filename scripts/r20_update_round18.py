"""ROUND 20 FIX LANE - the round-18 probe, where the fix moves it.

Round 18's finding was the LAUNDERING: an ask that names a covered class and an uncovered
one in the same sentence was scaffolded onto the covered one with not one word about the
other. That finding is not reopened by any of this - the ask still scaffolds NOTHING and
is still answered by naming what it is. What changed is that both halves of it now have a
template, so the answer names two templates instead of naming one and a hole.
"""
import io
from pathlib import Path

P = Path(__file__).resolve().parent.parent / "app/src/test/kotlin/org/chromia/Round18TemplateRedirectProbeTest.kt"
raw = io.open(P, encoding="utf-8", newline="").read()
NL = "\r\n" if "\r\n" in raw else "\n"
text = raw.replace("\r\n", "\n")


def sub(old, new, why):
    global text
    assert old in text, "anchor missing: " + why
    assert text.count(old) == 1, "anchor not unique: " + why
    text = text.replace(old, new)


sub(
    '''        assertEquals(15, templateTokens.size, "one entry per routing branch")
        assertEquals(4, declinedTokens.size, "one entry per class this server declines by name")''',
    '''        // ROUND 20: sixteen routing branches (the raffle joined them) and six classes
        // declined by name (the raffle LEFT them, and loyalty-points and fee-splitter -
        // the two TEMPLATE-GAPS rows that had no named refusal at all - joined them).
        assertEquals(16, templateTokens.size, "one entry per routing branch")
        assertEquals(6, declinedTokens.size, "one entry per class this server declines by name")''',
    "matrix sizes",
)

sub(
    '''    @Test
    fun `the laundered mixed ask names the uncovered class first and scaffolds nothing`() {
        val note = DappScaffold.closestTemplateNote(laundered)
        assertNull(DappScaffold.closestTemplate(laundered))
        assertFalse(note.startsWith("Use `template="), "the covered half must not be the first thing said: $note")
        val randomness = DappScaffold.untemplatedClasses.single { it.id == "unpredictable-outcome" }
        assertTrue(note.startsWith(randomness.note), "the uncovered class must be named FIRST: ${note.take(200)}")
        assertTrue(note.contains("UNPREDICTABLE OUTCOME"), note.take(200))
        // ...the missing guard, named as a shape rather than as a shrug.
        assertTrue(note.contains("op_context.last_block_time"), "the missing guard must be named: $note")
        assertTrue(note.contains("COMMIT-REVEAL"), note)
        assertTrue(note.contains("FUTURE block"), note)
        // ...the covered half, by template and with its exploit class.
        assertTrue(note.contains("`template=lending`"), "the covered half must still be named: $note")
        assertTrue(note.contains("does not cover"), note)
        // ...and the measurement that says why this is a NO rather than a footnote.
        assertTrue(note.contains("trudy staked 90 of 1890"), note)
        assertTrue(note.contains("FIVE OF FIVE"), note)
        assertTrue(note.contains("NOTHING WAS SCAFFOLDED"), note)
        // The other three two-class asks were declined by accident of order before; they
        // are declined by rule now, and they say the same things.
        listOf(
            "a marketplace with an escrow and a multisig treasury",
            "a staking contract with a payment channel for the rewards",
            "an amm that pays a lottery jackpot from the swap fee"
        ).forEach {
            val n = DappScaffold.closestTemplateNote(it)
            assertNull(DappScaffold.closestTemplate(it), it)
            assertTrue(n.startsWith("No shipped template covers that name"), n.take(160))
            assertTrue(n.contains("NOTHING WAS SCAFFOLDED"), "$it must name the covered half it is refusing: $n")
        }
    }''',
    '''    @Test
    fun `the laundered mixed ask names both halves and scaffolds nothing`() {
        val note = DappScaffold.closestTemplateNote(laundered)
        // ROUND 18'S FINDING IS UNCHANGED AND SO IS ITS ANSWER: the ask that was
        // scaffolded onto `lending` with not one word about the raffle still scaffolds
        // NOTHING, and still says why. What round 20 changed is the OTHER half - the
        // raffle has a template now, so this names two templates instead of naming one
        // and a hole.
        assertNull(DappScaffold.closestTemplate(laundered))
        assertFalse(note.startsWith("Use `template="), "the covered half must not be the first thing said: $note")
        assertTrue(note.startsWith("THIS ASK NAMES MORE THAN ONE COVERED CLASS"), note.take(200))
        // ...both halves, by template and each with its exploit class.
        assertTrue(note.contains("`template=lending`"), "the lending half must be named: $note")
        assertTrue(note.contains("`template=raffle`"), "the raffle half must be named: $note")
        assertTrue(
            note.contains(DappScaffold.templateClasses.getValue("lending")) &&
                note.contains(DappScaffold.templateClasses.getValue("raffle")),
            "a template NAME without its exploit class is the round-8 hazard: $note"
        )
        // ...and the measurement that says why this is a NO rather than a best-effort
        // scaffold of the half we happened to read first.
        assertTrue(note.contains("trudy staked 90 of 1890"), note)
        assertTrue(note.contains("FIVE OF FIVE"), note)
        assertTrue(note.contains("NOTHING WAS SCAFFOLDED"), note)
        // The two-class asks whose OTHER half still has no template are declined the way
        // they always were, by the uncovered class, first.
        listOf(
            "a marketplace with an escrow and a multisig treasury",
            "a staking contract with a payment channel for the rewards"
        ).forEach {
            val n = DappScaffold.closestTemplateNote(it)
            assertNull(DappScaffold.closestTemplate(it), it)
            assertTrue(n.startsWith("No shipped template covers that name"), n.take(160))
            assertTrue(n.contains("NOTHING WAS SCAFFOLDED"), "$it must name the covered half it is refusing: $n")
        }
        // ...and the one whose other half is the raffle is answered like the laundered
        // ask above: two covered classes, one at a time, nothing scaffolded.
        val jackpot = "an amm that pays a lottery jackpot from the swap fee"
        val jn = DappScaffold.closestTemplateNote(jackpot)
        assertNull(DappScaffold.closestTemplate(jackpot), jackpot)
        assertTrue(jn.startsWith("THIS ASK NAMES MORE THAN ONE COVERED CLASS"), jn.take(160))
        assertTrue(jn.contains("`template=amm`") && jn.contains("`template=raffle`"), jn)
        assertTrue(jn.contains("NOTHING WAS SCAFFOLDED"), jn)
    }''',
    "laundered test",
)

sub(
    '''    private val measuredTargets: Map<String, String> = mapOf(''',
    '''    /**
     * ROUND 20 moved exactly ONE of these, and it is the laundered ask itself: `a lending
     * pool that also runs a weekly raffle for depositors` was recorded landing on
     * `lending` BEFORE round 18's fix, round 18 turned it into a refusal, and round 20
     * gave its other half a template - so it is now a two-covered-class refusal rather
     * than a covered-plus-a-hole one. It is dropped from this map, which is the record of
     * what must not REGRESS, and asserted by name in the laundered-ask test instead.
     */
    private val measuredTargets: Map<String, String> = mapOf(''',
    "measuredTargets doc",
)

sub(
    '''        "yield-farming rewards" to "lending",
        "a lending pool that also runs a weekly raffle for depositors" to "lending",
        "an insurance pool whose premiums are streamed by the second" to "insurance"''',
    '''        "yield-farming rewards" to "lending",
        "an insurance pool whose premiums are streamed by the second" to "insurance"''',
    "laundered row in measuredTargets",
)

io.open(P, "w", encoding="utf-8", newline=NL).write(text)
print("Round18TemplateRedirectProbeTest updated")
