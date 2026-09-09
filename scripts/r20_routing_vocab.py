"""ROUND 20 FIX LANE, part 1 - the FOLD and the CONCEPT VOCABULARIES.

Measured first in scripts/r20_routing_proto.py against every pinned corpus in the
repository (120 distinct asks, 0 mismatches, exactly the 20 intended flips); this
script writes that measurement into DappScaffold.kt. Every anchor is asserted.

    python scripts/r20_routing_vocab.py
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
    assert text.count(old) == 1, "anchor is not unique: " + why
    text = text.replace(old, new)


# --------------------------------------------------------------------- 1. the fold
sub(
    """    internal fun foldAsk(raw: String): String {
        val decomposed = java.text.Normalizer.normalize(raw.lowercase(), java.text.Normalizer.Form.NFD)
        val stripped = COMBINING_MARKS.replace(decomposed, "")
        val out = StringBuilder(stripped.length)
        stripped.forEach { c ->
            when (c) {""",
    """    internal fun foldAsk(raw: String): String {
        val decomposed = java.text.Normalizer.normalize(raw.lowercase(), java.text.Normalizer.Form.NFD)
        val stripped = COMBINING_MARKS.replace(decomposed, "")
        val out = StringBuilder(stripped.length)
        stripped.forEach { c ->
            // ROUND 20, LAUNDERING A. Two scripts that DO have a settled Latin
            // transliteration are folded through it. See [NON_LATIN_FOLD].
            val transliterated = NON_LATIN_FOLD[c]
            if (transliterated != null) {
                out.append(transliterated)
                return@forEach
            }
            when (c) {""",
    "foldAsk body",
)

sub(
    """    private val COMBINING_MARKS = Regex("\\\\p{Mn}+")
""",
    """    private val COMBINING_MARKS = Regex("\\\\p{Mn}+")

    /**
     * ROUND 20, LAUNDERING A - CYRILLIC AND GREEK, TRANSLITERATED RATHER THAN LEFT
     * INVISIBLE.
     *
     * Round 19 folded the Latin scripts and wrote down, honestly, what that did not
     * reach: "a script with no Latin transliteration is invisible to every key list here,
     * covered and uncovered alike". Round 20 MEASURED the consequence over nine non-Latin
     * asks for a class this server declines by name, and 0 of 9 reached the class
     * paragraph. The ninth is worse than invisible:
     *
     *     `a weekly лотерея for token holders`  ->  ok:true, template=ft4, FOUR FILES
     *
     * One word in Cyrillic inside an English sentence, and `token holders` sent the ask to
     * `ft4` with the class paragraph nowhere in the answer - round 19's synonym laundering
     * with the synonym written in another alphabet.
     *
     * The two scripts below are the ones with a SETTLED, single-valued transliteration
     * into Latin: Russian/Ukrainian/Belarusian/Bulgarian/Serbian Cyrillic, and Greek. They
     * are written out here as a table rather than pulled in as a dependency - a
     * transliteration library is a supply-chain surface for thirty-eight characters - and
     * every row is exercised by the multilingual corpora in the round-19 and round-20
     * probe tests, so a row that can never fire is a red test.
     *
     * The table runs AFTER Unicode NFD and after combining marks are dropped, so the
     * accented forms decompose into the base letters first: `ё` -> `е` -> `e`, `й` -> `и`
     * -> `i`, `ή` -> `η` -> `i`. That is why neither is listed. `лотерея` folds to
     * `lotereya`, which the key `loter*` takes; `κλήρωση` folds to `klirosi`.
     *
     * WHAT THIS STILL DOES NOT REACH, written down rather than implied: Chinese, Japanese,
     * Korean, Hebrew, Arabic and Hindi have no single-valued letter-for-letter Latin form
     * (Chinese and Japanese are not alphabetic at all; Hebrew and Arabic drop the vowels a
     * key would need). Those six remain the recorded gap in `docs/TEMPLATE-GAPS.md`, and
     * the honest shape for them is a per-language key list, not a fold.
     */
    private val NON_LATIN_FOLD: Map<Char, String> = mapOf(
        // Cyrillic.
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'ґ' to "g", 'д' to "d", 'е' to "e",
        'є' to "ye", 'ж' to "zh", 'з' to "z", 'и' to "i", 'і' to "i", 'ї' to "yi",
        'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p",
        'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ў' to "u", 'ф' to "f",
        'х' to "kh", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh", 'щ' to "shch",
        // The two signs carry no sound and no Latin letter: they fold to nothing, which
        // is what every transliteration standard does with them.
        'ъ' to "", 'ь' to "", 'ы' to "y", 'э' to "e", 'ю' to "yu", 'я' to "ya",
        // Greek. Final sigma is the same letter as medial sigma.
        'α' to "a", 'β' to "v", 'γ' to "g", 'δ' to "d", 'ε' to "e", 'ζ' to "z",
        'η' to "i", 'θ' to "th", 'ι' to "i", 'κ' to "k", 'λ' to "l", 'μ' to "m",
        'ν' to "n", 'ξ' to "x", 'ο' to "o", 'π' to "p", 'ρ' to "r", 'σ' to "s",
        'ς' to "s", 'τ' to "t", 'υ' to "y", 'φ' to "f", 'χ' to "ch", 'ψ' to "ps",
        'ω' to "o"
    )
""",
    "COMBINING_MARKS declaration",
)

# ------------------------------------------------- 2. the vocabulary, split in three
start = text.index("    private val UNPREDICTABLE_OUTCOME_KEYS = listOf(")
end = text.index("    private val PAYMENT_CHANNEL_KEYS = listOf(")
NEW_KEYS = '''    /**
     * THE RAFFLE CLASS, and since round 20 it is a COVERED one: `template=raffle` ships
     * the commit-reveal draw whose seed folds in COMMIT order. These are the keys that
     * name THE THING ITSELF - a draw with a winner - in the languages agents write asks
     * in, and they are read as a class rather than as words: see the head/mention rule in
     * [closestTemplateNote].
     *
     * WHAT IS NOT HERE, AND WHY IT MOVED. `random`, `randomness`, `randomly*`, `rng`,
     * `vrf`, `giveaway*` and `at random` used to sit in this one list, and round 20
     * measured what that cost: SEVEN asks for classes this server ships a template for
     * were refused as "an UNPREDICTABLE OUTCOME" with nothing scaffolded - an oracle
     * sampled at random intervals, a validator chosen at random each epoch, a giveaway of
     * free listings, order ids assigned randomly, a proposal picked at random for audit, a
     * randomness beacon read as a price input, and - the sharpest one - `a stablecoin
     * whose peg is deterministic and USES NO VRF`, refused for saying the word. Those
     * seven words describe HOW something is done, not WHAT is being built, so they are
     * [RAFFLE_WEAK_KEYS] and they can no longer decline an ask on their own.
     */
    private val RAFFLE_KEYS = listOf(
        // The stem that carries lottery/lotteries AND lotteria/lotterie (Italian,
        // Spanish, Portuguese, Polish) - it is the first key, and the round-18 matrix
        // derives this class's probe token from it.
        "lotter*",
        // ...and the OTHER Latin stem for the same word: loterie (fr), lotería (es),
        // loteria (pt/pl), loterij (nl). `lottery` does not start with `loter`, which is
        // why both stems are here rather than one.
        "loter*", "lotto*", "loto",
        // English, by every ordinary name.
        "raffle*", "sweepstake*", "prize draw*", "prize drawing*", "lucky draw*", "lucky dip*",
        "random winner*", "random draw*", "random selection*",
        "tombola*", "jackpot*", "scratch card*", "scratchcard*",
        "winning ticket*", "raffle ticket*", "chance to win*", "pick a winner*", "picks a winner*",
        "picks the winner*", "draws a winner*",
        // French: loterie/tombola are above; tirage (au sort), gagnant au hasard.
        "tirage*", "gagnant au hasard*",
        // Spanish and Portuguese: sorteo/sorteio, rifa, premio mayor.
        "sorteo*", "sorteio*", "rifa*", "premio mayor*",
        // German: Lotterie is above; Verlosung, Gewinnspiel, Ziehung, Auslosung.
        "verlosung*", "gewinnspiel*", "ziehung*", "auslosung*",
        // Italian: lotteria is above; estrazione, sorteggio.
        "estrazion*", "sorteggio*",
        // Dutch and Polish.
        "loterij*", "losowanie*", "trekking*",
        // Greek and Russian, through the transliteration in [NON_LATIN_FOLD]: κλήρωση
        // folds to `klirosi`, λαχείο to `lacheio`, розыгрыш to `rozygrysh`.
        "klirosi*", "klirose*", "lacheio*", "rozygrysh*"
    )

    /**
     * ROUND 20, FINDING C - THE WORDS THAT DESCRIBE HOW, NOT WHAT.
     *
     * A vault priced by an oracle SAMPLED AT RANDOM INTERVALS is a vault. A stablecoin
     * whose peg USES NO VRF is a stablecoin. A marketplace with A GIVEAWAY of free
     * listings is a marketplace. Every one of those was refused outright, because these
     * words were keys of the raffle class and the class list was consulted before the
     * routing `when` and won whatever else the ask named.
     *
     * A weak key can still reach the raffle class - `on-chain randomness` with no covered
     * class in the ask is a raffle ask and is answered as one - but it can no longer take
     * an ask AWAY from a covered class it is merely describing. Where a covered class is
     * the head of the ask, a weak word becomes a NOTE beside the scaffold instead of a
     * refusal in place of it.
     */
    private val RAFFLE_WEAK_KEYS = listOf(
        "random", "at random", "randomly*", "randomness", "chosen at random*",
        "rng", "vrf", "giveaway*", "give away*", "sortition",
        // The same words in the languages the class list already claims.
        "hasard", "aleatoire*", "azar", "aleatorio*", "aleatoria*", "zufall*"
    )

    /**
     * WAGERING - a bet on an outcome - is the half of round 19's unpredictable-outcome
     * class that STILL has no template, and it is separated from [RAFFLE_KEYS] because
     * the two are different exploit classes wearing the same word. A raffle draws ONE
     * winner from a pot the entrants funded; a prediction market, a sportsbook or a casino
     * takes the other side of a bet and must be SOLVENT for every outcome at once, which
     * `template=raffle` says nothing about. Round 19's `abetting` rule is unchanged: there
     * is no `bet*` and no bare `pari*` here.
     */
    private val WAGERING_KEYS = listOf(
        // First key: the round-18 matrix derives this class's probe token from it.
        "prediction market*", "betting*", "bettor*", "wager*", "gambl*", "casino*",
        "dice roll*", "coin flip*", "coinflip*", "roulette*", "bingo*",
        "sportsbook*", "sports book*", "bookmaker*", "parimutuel*", "pari mutuel*", "parlay*",
        "odds of winning*",
        // French, Spanish, German, Italian, Greek, Russian, Polish.
        "pari", "parier*", "parieur*", "paris sportifs*", "jeu de hasard*",
        "apuest*", "apost*", "juego de azar*",
        "glucksspiel*", "gluecksspiel*", "wette*",
        "scommess*", "zaklad bukmacherski*",
        "stoichima*", "tzogos*", "stavka*", "azartn*"
    )

'''
text = text[:start] + NEW_KEYS + text[end:]

# ------------------------------------------- 3. the two rows with no named refusal
sub(
    '''        "crowdfunding campagne*", "zbiorka*"
    )
''',
    '''        "crowdfunding campagne*", "zbiorka*"
    )

    /**
     * ROUND 20, LAUNDERING B - LOYALTY AND POINTS, WHICH HAD NO NAMED REFUSAL AT ALL.
     *
     *     `a points program where the issuer mints rewards for purchases`
     *         ->  ok:true, template=staking, THREE FILES
     *
     * `docs/TEMPLATE-GAPS.md` carries this row and says the staking template's discipline
     * - "every credit is a pool debit" - CANNOT SIMPLY BE CARRIED OVER to it, and the ask
     * was answered with that very template on the word `rewards`. A points programme is
     * the opposite shape: the issuer MINTS the points, so there is no pool to debit and
     * the conservation invariant the staking notes teach is vacuous on it.
     */
    private val LOYALTY_POINTS_KEYS = listOf(
        // First key: the round-18 matrix derives this class's probe token from it.
        "loyalty*", "points program*", "points programme*", "reward points*", "loyalty point*",
        "points for purchases*", "frequent flyer*", "air miles*", "stamp card*", "punch card*",
        "cashback*", "cash back*", "store credit*", "gift card*", "voucher*",
        // French, Spanish, German, Portuguese, Italian, Dutch, Polish.
        "programme de fidelite*", "programa de fidelidad*", "puntos de fidelidad*",
        "treuepunkt*", "kundenbindungsprogramm*", "bonuspunkt*",
        "programa de fidelidade*", "programma fedelta*", "punti fedelta*",
        "spaarpunten*", "program lojalnosciowy*"
    )

    /**
     * ROUND 20, LAUNDERING B, THE OTHER ROW - A SPLITTER OF INCOME AMONG WEIGHTED
     * RECIPIENTS: a fee splitter, a revenue share, a donation pool, a charity.
     *
     * The TEMPLATE-GAPS row for it has been open since the file was written and had no
     * entry here, so every phrasing of it fell to the generic roster - which teaches an
     * agent that we have no template with that NAME, and nothing about the exploit class.
     * Round 19 deliberately kept `donation*` and `charity*` out of [CROWDFUNDING_KEYS]
     * because they belong to THIS class; this is the class they were being kept for.
     */
    private val FEE_SPLITTER_KEYS = listOf(
        // First key: the round-18 matrix derives this class's probe token from it.
        "fee splitter*", "fee split*", "revenue split*", "revenue share*", "revenue sharing*",
        "payment splitter*", "split the fee*", "splits the fee*", "splitting the fee*",
        "split the revenue*", "splits the revenue*", "profit share*", "profit split*",
        "donation pool*", "donation*", "charity*", "charitable*", "tip jar*",
        "weighted recipient*", "payout split*", "split between recipients*",
        // French, Spanish, German, Portuguese, Italian, Dutch, Polish.
        "repartition des frais*", "partage des revenus*", "cagnotte caritative*",
        "reparto de ingresos*", "division de comisiones*", "donacion*",
        "einnahmenaufteilung*", "gebuhrenaufteilung*", "spenden*",
        "divisao de receita*", "doacao*", "doacoes*",
        "divisione dei ricavi*", "donazion*",
        "opbrengstverdeling*", "podzial oplat*"
    )
''',
    "CROWDFUNDING_KEYS tail",
)

# ---------------------------------------------- 4. raffle joins the covered routing
sub(
    """    internal val templateKeys: List<Pair<String, List<String>>> = listOf(
        "bridge" to BRIDGE_KEYS,""",
    """    internal val templateKeys: List<Pair<String, List<String>>> = listOf(
        // ROUND 20: the SIXTEENTH template, and it is read first. Every realistic raffle
        // ask names a token, a holder or a reward, so `a weekly lottery with rewards for
        // ticket holders` reached `staking` (round 17) and `a tombola for token holders`
        // reached `ft4` (round 19) - both of them four branches before the draw was
        // looked at, and neither template makes an outcome unpredictable.
        "raffle" to RAFFLE_KEYS,
        "bridge" to BRIDGE_KEYS,""",
    "templateKeys head",
)

io.open(KT, "w", encoding="utf-8", newline=NL).write(text)
print("vocabularies written: fold table, RAFFLE/RAFFLE_WEAK/WAGERING, loyalty-points, fee-splitter, raffle routing")
