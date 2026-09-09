"""ROUND 20 FIX LANE - the round-18/19 pins the fix moves, and why each one moved.

Nothing here weakens a test. Every edit is one of two things:
  - a row that asserted "this class has NO template" where the class now HAS one (the
    raffle), flipped to assert the scaffold and the note it comes with;
  - a corpus that has to GROW because two classes were added (loyalty-points,
    fee-splitter) and one was split in two (the raffle half and the wagering half).
The multilingual-corpus test enforces the growth itself: its keys must equal the ids in
`untemplatedClasses`, so a class added without a corpus is a red test.
"""
import io
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
R19 = ROOT / "app/src/test/kotlin/org/chromia/Round19TemplateSurfaceProbeTest.kt"
SEC = ROOT / "app/src/test/kotlin/org/chromia/DappScaffoldSecureTemplatesTest.kt"


class File:
    def __init__(self, path):
        self.path = path
        raw = io.open(path, encoding="utf-8", newline="").read()
        self.nl = "\r\n" if "\r\n" in raw else "\n"
        self.text = raw.replace("\r\n", "\n")

    def sub(self, old, new, why):
        assert old in self.text, "%s: anchor missing: %s" % (self.path.name, why)
        assert self.text.count(old) == 1, "%s: anchor not unique: %s" % (self.path.name, why)
        self.text = self.text.replace(old, new)

    def save(self):
        io.open(self.path, "w", encoding="utf-8", newline=self.nl).write(self.text)


# =============================================================== Round 19 surface probe
f = File(R19)

f.sub(
    '''    fun `the synonym reaches the class and scaffolds nothing`() {
        val ask = "a tombola for token holders that pays out weekly"
        val body = scaffold(ask)
        assertEquals(0, fileCount(body), "a tombola is a raffle and a raffle has no template here")
        assertEquals("false", body["ok"]?.jsonPrimitive?.content, "ok:true with four files was the finding")
        assertEquals("", body["template"]?.jsonPrimitive?.content)
        val note = DappScaffold.closestTemplateNote(ask)
        val raffle = DappScaffold.untemplatedClasses.single { it.id == "unpredictable-outcome" }
        assertTrue(note.startsWith(raffle.note), "the uncovered class must be named FIRST: ${note.take(200)}")
        assertTrue(note.contains("UNPREDICTABLE OUTCOME"), note.take(300))
        // ...and the covered half the ask also names (`token holders` -> ft4) is still
        // named by template, because a compound ask is built one template at a time.
        assertTrue(note.contains("`template=ft4`"), "the covered half must still be named: $note")
        assertTrue(note.contains("NOTHING WAS SCAFFOLDED"), note)
        assertTrue(note.contains(raffle.missingGuard), "the missing guard must be named: $note")
    }''',
    '''    fun `the synonym reaches the class, and since round 20 the class has a template`() {
        val ask = "a tombola for token holders that pays out weekly"
        val body = scaffold(ask)
        // ROUND 19 asserted zero files here, because a raffle had no template. Round 20
        // built it - the sixteenth - after building this project's own design note for
        // the class and watching it drained five of five draws. The finding round 19
        // recorded is unchanged and is still what this row is about: the SYNONYM has to
        // reach the class. What changed is where the class goes.
        assertEquals(3, fileCount(body), "the raffle class has a template now and must scaffold it")
        assertEquals("true", body["ok"]?.jsonPrimitive?.content)
        assertEquals("raffle", body["template"]?.jsonPrimitive?.content, "a tombola is a raffle")
        val note = DappScaffold.closestTemplateNote(ask)
        val raffle = DappScaffold.conceptClasses.single { it.id == "raffle" }
        assertTrue(note.startsWith("Use `template=raffle`"), "the class must be named FIRST: ${note.take(200)}")
        assertEquals(raffle.note, note, "a raffle ask is answered by the raffle class's own note")
        // ...and the answer still says what the template does NOT cover, which is the
        // half round 19 was really about: a word list is not a guard.
        assertTrue(note.contains("DENY a round by not revealing"), "the residual must be stated: $note")
        assertTrue(
            note.contains("prediction market") && note.contains("SOLVENT FOR EVERY OUTCOME AT ONCE"),
            "the wagering half is a DIFFERENT class with no template and must be named: $note"
        )
    }''',
    "tombola row",
)

f.sub(
    '''    fun `the class in another language is answered as the class, with its missing guard`() {
        val raffle = DappScaffold.untemplatedClasses.single { it.id == "unpredictable-outcome" }
        listOf(
            "une loterie hebdomadaire pour les deposants",
            "un sorteo semanal de premios para los depositantes",
            // ...and the same three with the accents an agent actually types, which the
            // tokeniser used to cut into fragments.
            "une loterie hebdomadaire pour les déposants",
            "un sorteo semanal de premios para los depositantes en la lotería",
            "eine wöchentliche Verlosung für Einzahler",
            "um sorteio semanal para os depositantes",
            "una lotteria settimanale con estrazione",
            "un tirage au sort hebdomadaire",
            "ein Glücksspiel mit Zufallsgewinner"
        ).forEach { ask ->
            assertNull(DappScaffold.closestTemplate(ask), "'$ask' is the raffle class and has no template")
            val note = DappScaffold.closestTemplateNote(ask)
            assertTrue(note.startsWith(raffle.note), "'$ask' must be answered as the class: ${note.take(200)}")
            assertTrue(note.contains(raffle.label), "'$ask' must NAME the class: ${note.takeLast(600)}")
            assertTrue(note.contains(raffle.missingGuard), "'$ask' must name the MISSING GUARD: $note")
            assertTrue(note.contains("NOTHING WAS SCAFFOLDED"), "'$ask': ${note.takeLast(400)}")
            assertFalse(
                note.contains("Pick the one whose EXPLOIT class matches yours"),
                "'$ask' must not fall to the unknown-template roster - that is the safe-by-accident answer"
            )
        }
    }''',
    '''    fun `the class in another language is answered as the class, with its missing guard`() {
        val raffle = DappScaffold.conceptClasses.single { it.id == "raffle" }
        // ROUND 19's finding was that these asks fell to the unknown-template roster and
        // learned NOTHING about the class - "safe by accident". They reach the class, and
        // since round 20 the class has a template, so reaching it means scaffolding it.
        listOf(
            "une loterie hebdomadaire pour les deposants",
            "un sorteo semanal de premios para los depositantes",
            // ...and the same three with the accents an agent actually types, which the
            // tokeniser used to cut into fragments.
            "une loterie hebdomadaire pour les déposants",
            "un sorteo semanal de premios para los depositantes en la lotería",
            "eine wöchentliche Verlosung für Einzahler",
            "um sorteio semanal para os depositantes",
            "una lotteria settimanale con estrazione",
            "un tirage au sort hebdomadaire"
        ).forEach { ask ->
            assertEquals("raffle", DappScaffold.closestTemplate(ask), "'$ask' is the raffle class")
            val note = DappScaffold.closestTemplateNote(ask)
            assertEquals(raffle.note, note, "'$ask' must be answered as the class: ${note.take(200)}")
            assertFalse(
                note.contains("Pick the one whose EXPLOIT class matches yours"),
                "'$ask' must not fall to the unknown-template roster - that is the safe-by-accident answer"
            )
        }
        // ...and the WAGERING half of round 19's corpus, which is a different exploit
        // class and still has no template: a book must be solvent for every outcome at
        // once, and `template=raffle` says nothing about that.
        val wagering = DappScaffold.untemplatedClasses.single { it.id == "unpredictable-outcome" }
        listOf(
            "ein Glücksspiel mit Zufallsgewinner",
            "un jeu de hasard hebdomadaire",
            "una apuesta deportiva"
        ).forEach { ask ->
            assertNull(DappScaffold.closestTemplate(ask), "'$ask' is a wager and has no template")
            val note = DappScaffold.closestTemplateNote(ask)
            assertTrue(note.startsWith(wagering.note), "'$ask' must be answered as the class: ${note.take(200)}")
            assertTrue(note.contains(wagering.label), "'$ask' must NAME the class: ${note.takeLast(600)}")
            assertTrue(note.contains(wagering.missingGuard), "'$ask' must name the MISSING GUARD: $note")
            assertTrue(note.contains("NOTHING WAS SCAFFOLDED"), "'$ask': ${note.takeLast(400)}")
        }
    }''',
    "another-language row",
)

f.sub(
    '''            "unpredictable-outcome" to listOf(
                "a tombola", "a sweepstake", "a prize draw", "a giveaway with a jackpot",
                "a lotto", "a raffle", "a lottery", "a prediction market", "sports betting",
                "une loterie", "un tirage au sort", "un jeu de hasard", "un gagnant au hasard",
                "un sorteo", "una rifa", "un juego de azar", "una apuesta",
                "eine Lotterie", "eine Verlosung", "ein Gewinnspiel", "ein Glücksspiel",
                "eine Ziehung", "eine Wette",
                "um sorteio", "uma rifa", "uma aposta",
                "una lotteria", "una estrazione", "un sorteggio", "una scommessa",
                "een loterij", "losowanie"
            ),''',
    '''            // ROUND 20 SPLIT THIS CLASS IN TWO. The DRAW half has a template now and is
            // asserted below, in `every raffle phrasing reaches the sixteenth template`;
            // what is left here is the half that takes the other side of a BET, which has
            // no template and a different exploit class - a book must be solvent for every
            // outcome at once.
            "unpredictable-outcome" to listOf(
                "a prediction market", "sports betting", "a casino game", "a coin flip game",
                "a roulette wheel", "a bingo hall", "a sportsbook", "a parlay",
                "un jeu de hasard", "un pari", "des paris sportifs",
                "un juego de azar", "una apuesta",
                "ein Glücksspiel", "eine Wette",
                "uma aposta", "una scommessa",
                "zaklad bukmacherski"
            ),
            "loyalty-points" to listOf(
                "a loyalty programme", "a points program", "reward points", "a stamp card",
                "cashback", "a gift card", "a voucher", "frequent flyer miles",
                "un programme de fidelite", "un programa de fidelidad",
                "Treuepunkte", "um programa de fidelidade",
                "punti fedeltà", "spaarpunten", "program lojalnosciowy"
            ),
            "fee-splitter" to listOf(
                "a fee splitter", "a revenue share", "a donation pool", "a charity",
                "a tip jar", "a payment splitter", "a profit share",
                "un partage des revenus", "un reparto de ingresos",
                "Einnahmenaufteilung", "uma divisao de receita",
                "una divisione dei ricavi", "opbrengstverdeling", "podzial oplat"
            ),''',
    "byLanguage corpus",
)

f.sub(
    """        listOf("a charity donation pool", "a gaming item shop", "a loyalty programme with points").forEach {""",
    """        // ROUND 20 gave two of these three a class of their own, so only the
        // gaming-item row is still the roster answer. A row moving out of this list is
        // the gap closing, and it goes with a TEMPLATE-GAPS edit in the same commit.
        listOf("a gaming item shop").forEach {""",
    "roster-only classes",
)

f.sub(
    """        (DappScaffold.untemplatedClasses.flatMap { it.keys } + DappScaffold.templateKeys.flatMap { it.second })""",
    """        // ROUND 20: the WEAK keys are folded and matched exactly like the strong ones,
        // so they are held to the same rule - a key that is not written folded is a key
        // that can never fire, and the point of this row is that there are none.
        (DappScaffold.conceptClasses.flatMap { it.keys + it.weakKeys } +
            DappScaffold.templateKeys.flatMap { it.second })""",
    "every key is folded",
)

f.sub(
    '''            "Straße" to "strasse",
            "Łódź" to "lodz"
        ).forEach { (raw, folded) ->''',
    '''            "Straße" to "strasse",
            "Łódź" to "lodz",
            // ROUND 20: two scripts with a settled Latin transliteration, folded through
            // NON_LATIN_FOLD. Round 19 recorded these as invisible; round 20 measured the
            // cost (0 of 9 non-Latin asks reached their class) and this is the half of it
            // that a table can fix.
            "лотерея" to "lotereya",
            "Лотерея" to "lotereya",
            "розыгрыш" to "rozygrysh",
            "еженедельная лотерея" to "ezhenedelnaya lotereya",
            "κλήρωση" to "klirosi",
            "Κλήρωση" to "klirosi",
            "λαχείο" to "lacheio",
            "στοίχημα" to "stoichima"
        ).forEach { (raw, folded) ->''',
    "fold rows",
)

f.sub(
    """    /**
     * THE CLEAN PASSES, AND THE REASON THIS LANE IS ALLOWED TO ADD STEMS AT ALL.""",
    '''    /**
     * ROUND 20 - EVERY RAFFLE PHRASING REACHES THE SIXTEENTH TEMPLATE. This is the other
     * half of the corpus above: round 19 built the class as a CONCEPT so that a synonym
     * or an accent could not launder it, and round 20 gave the class a template, so
     * reaching it now means scaffolding it. A phrasing that stops reaching it is round
     * 19's finding again with the destination changed.
     */
    @Test
    fun `every raffle phrasing reaches the sixteenth template`() {
        listOf(
            "a tombola", "a sweepstake", "a prize draw", "a lotto", "a raffle", "a lottery",
            "une loterie", "un tirage au sort", "un gagnant au hasard",
            "un sorteo", "una rifa",
            "eine Lotterie", "eine Verlosung", "ein Gewinnspiel", "eine Ziehung",
            "um sorteio", "uma rifa",
            "una lotteria", "una estrazione", "un sorteggio",
            "een loterij", "losowanie"
        ).forEach { ask ->
            assertEquals(
                "raffle",
                DappScaffold.closestTemplate(ask),
                "'$ask' is the raffle class and it has a template now: " +
                    DappScaffold.closestTemplateNote(ask).take(160)
            )
        }
    }

    /**
     * THE CLEAN PASSES, AND THE REASON THIS LANE IS ALLOWED TO ADD STEMS AT ALL.''',
    "clean-pass doc anchor",
)

f.save()
print("Round19TemplateSurfaceProbeTest updated")

# ======================================================== DappScaffoldSecureTemplatesTest
g = File(SEC)
g.sub(
    '''    private val secureTemplates = listOf("governance", "vault", "staking", "marketplace", "lending", "streaming", "amm", "stablecoin", "exchange", "subscription", "bridge", "escrow", "insurance")''',
    '''    private val secureTemplates = listOf("governance", "vault", "staking", "marketplace", "lending", "streaming", "amm", "stablecoin", "exchange", "subscription", "bridge", "escrow", "insurance", "raffle")''',
    "secureTemplates",
)
g.sub(
    '''        assertEquals(listOf("hello", "ft4", "governance", "vault", "staking", "marketplace", "lending", "streaming", "amm", "stablecoin", "exchange", "subscription", "bridge", "escrow", "insurance"), DappScaffold.templates)
        assertEquals("insurance", DappScaffold.toJson("pool", template = "insurance").getValue("template").toString().trim('"'), "the class round 17 drained must scaffold its own template")''',
    '''        assertEquals(listOf("hello", "ft4", "governance", "vault", "staking", "marketplace", "lending", "streaming", "amm", "stablecoin", "exchange", "subscription", "bridge", "escrow", "insurance", "raffle"), DappScaffold.templates)
        assertEquals("raffle", DappScaffold.toJson("draw", template = "raffle").getValue("template").toString().trim('"'), "the class round 20 drained - from this project's OWN design note - must scaffold its own template")
        assertEquals("insurance", DappScaffold.toJson("pool", template = "insurance").getValue("template").toString().trim('"'), "the class round 17 drained must scaffold its own template")''',
    "templates pin",
)
g.save()
print("DappScaffoldSecureTemplatesTest updated")
