"""ROUND 20 FIX LANE, part 3 - THE DECISION, and the answers it produces.

    python scripts/r20_routing_decide.py
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


# ------------------------------------------------ 1. the raffle class's own answer
sub(
    """    internal class ConceptClass(""",
    '''    /**
     * THE RAFFLE CLASS'S ANSWER, and it is the first one in this file that says YES to a
     * class an adversary round drained. It must open with ``Use `template=raffle` `` -
     * [closestTemplate] reads that prefix and it is what makes the ask scaffold.
     */
    private val RAFFLE_NOTE: String =
        "Use `template=raffle`: it is the template for this class, and this class is the one this " +
            "server DECLINED BY NAME until round 20 - the answer used to be \\"no template covers an " +
            "UNPREDICTABLE OUTCOME\\" with nothing scaffolded, because a draw an operation's signer " +
            "can predict is not a draw, it is a withdrawal. What changed is that the guard now " +
            "exists as code you can read, and it exists because THE PROJECT'S OWN DESIGN NOTE FOR " +
            "THIS CLASS WAS DRAINED. `docs/TEMPLATE-GAPS.md` said the shape was \\"commit-reveal with " +
            "a deposit the revealer forfeits\\", and that \\"her choice is between REVEALING and " +
            "FORFEITING her deposit\\". Adversary round 20 built exactly that note, proved ELEVEN " +
            "guards load-bearing on a chain, and lost FIVE OF FIVE draws to an attacker holding " +
            "4.76% of the stake - with ZERO deposits forfeited and ZERO secrets withheld, so the " +
            "sentence the deposit was supposed to enforce never came up. It is true of ONE " +
            "commitment and false of two: the seed was `round.mixed = crypto.sha256(round.mixed + " +
            "secret)`, A FOLD IN REVEAL ORDER, so six commitments were 720 accumulators to choose " +
            "between for free. She read the honest secrets off the chain, ran the module's own " +
            "public `pick` over each ordering of her own, and revealed in the one whose ticket was " +
            "hers. WHAT THE TEMPLATE DOES INSTEAD, and each of these is structural rather than a " +
            "check a later operation can forget: THE SEED FOLDS THE COMMITTED SET IN COMMIT ORDER " +
            "(`@sort .seq`, written by `commit` before any secret in the round exists) and `reveal` " +
            "touches no accumulator at all, so reveal order is not an input to anything; A ROUND " +
            "THAT IS NOT COMPLETE DOES NOT DRAW (`reveals == commits`, or it refunds), which " +
            "collapses the 2^m withholding subsets into one choice; A FORFEITED DEPOSIT IS BURNED " +
            "rather than added to the prize, because a pot that grows when somebody fails to reveal " +
            "pays for the failure; and THE STAKE IS CAPPED BY THE DEPOSIT, so walking away from a " +
            "losing draw never costs less than playing it. Measured on a chain: the same " +
            "720-permutation search still finds an ordering in FIFTY of fifty rounds and now wins " +
            "ONE round in fifty, against a 4.7619% stake share. WHAT IT STILL DOES NOT COVER, said " +
            "plainly: a participant can always DENY a round by not revealing, for one deposit a " +
            "round - nobody profits from the denial, so it buys griefing and never a draw, and a " +
            "raffle that must not be stoppable needs a randomness BEACON read with the discipline " +
            "`template=vault` applies to a price (bounded, rate-limited, staleness-halted), not a " +
            "bigger deposit. AND IF WHAT YOU ARE BUILDING TAKES THE OTHER SIDE OF A BET - a " +
            "prediction market, a sportsbook, a casino game - that is a DIFFERENT exploit class " +
            "with no template here: a raffle pays one winner out of a pot the entrants funded, " +
            "while a book must be SOLVENT FOR EVERY OUTCOME AT ONCE, and nothing in this template " +
            "says anything about that."

    internal class ConceptClass(''',
    "ConceptClass declaration",
)

# --------------------------------------------------------------- 2. the decision
sub(
    """        val askTokens = TOKEN.findAll(t).map { it.value }.toList()
        fun matchesKey(key: String): Boolean {
            val prefix = key.endsWith("*")
            // The key is folded too, so a key written with an accent - `lotería` - is
            // the same key as the folded ask it has to match, and there is no way to
            // add a key that can never fire.
            val parts = TOKEN.findAll(foldAsk(if (prefix) key.dropLast(1) else key)).map { it.value }.toList()
            if (parts.isEmpty() || parts.size > askTokens.size) return false
            for (start in 0..askTokens.size - parts.size) {
                var ok = true
                for (j in parts.indices) {
                    val token = askTokens[start + j]
                    val part = parts[j]
                    val hit = if (prefix && j == parts.size - 1) token.startsWith(part) else token == part
                    if (!hit) { ok = false; break }
                }
                if (ok) return true
            }
            return false
        }""",
    """        val askTokens = TOKEN.findAll(t).map { it.value }.toList()
        // ROUND 20: WHERE a key matched, not merely whether. The rule below turns on the
        // POSITION of the match, so the matcher returns every start index rather than a
        // boolean.
        fun keyStarts(key: String): List<Int> {
            val prefix = key.endsWith("*")
            // The key is folded too, so a key written with an accent - `lotería` - is
            // the same key as the folded ask it has to match, and there is no way to
            // add a key that can never fire.
            val parts = TOKEN.findAll(foldAsk(if (prefix) key.dropLast(1) else key)).map { it.value }.toList()
            if (parts.isEmpty() || parts.size > askTokens.size) return emptyList()
            val out = mutableListOf<Int>()
            for (start in 0..askTokens.size - parts.size) {
                var ok = true
                for (j in parts.indices) {
                    val token = askTokens[start + j]
                    val part = parts[j]
                    val hit = if (prefix && j == parts.size - 1) token.startsWith(part) else token == part
                    if (!hit) { ok = false; break }
                }
                if (ok) out.add(start)
            }
            return out
        }
        // ROUND 20, AND ROUND 19'S r6 WAS THE SEED: a key inside a NEGATION is not the
        // class being named, it is the class being ruled out. `a stablecoin whose peg is
        // deterministic and USES NO VRF` was refused as an unpredictable outcome for
        // saying the word - an ask that says the dapp does NOT do this, answered as
        // though it did. Three tokens is the window an English (or French, Spanish,
        // German, Dutch, Italian, Portuguese, Polish) negator sits in before the thing it
        // negates: `without any raffle`, `uses no VRF`, `sin sorteo`, `ohne Verlosung`.
        fun negated(start: Int): Boolean =
            (maxOf(0, start - 3) until start).any { askTokens[it] in NEGATORS }
        fun liveStarts(keys: List<String>): List<Int> =
            keys.flatMap { keyStarts(it) }.filterNot { negated(it) }
        fun matchesKey(key: String): Boolean = keyStarts(key).any { !negated(it) }""",
    "matchesKey body",
)

sub(
    """        fun hasAny(keys: List<String>) = keys.any { matchesKey(it) }""",
    """        fun hasAny(keys: List<String>) = keys.any { matchesKey(it) }
        // WHERE THE ASK'S HEAD NOUN PHRASE ENDS. Everything from the first boundary word
        // on either modifies the head or names a SECOND thing: `a staking pool WHOSE
        // validator is chosen at random`, `a marketplace WITH a giveaway`. The head is
        // what the ask is ABOUT, and round 20's seven false declines were all classes
        // named after it.
        val headEnd = run {
            var start = 0
            if (start < askTokens.size && askTokens[start] in BUILD_VERBS) {
                start += 1
                while (start < askTokens.size && askTokens[start] in ARTICLES) start += 1
            }
            (start until askTokens.size).firstOrNull { askTokens[it] in HEAD_BOUNDARY } ?: askTokens.size
        }""",
    "hasAny declaration",
)

sub(
    """        val declined = untemplatedClasses.filter { c -> hasAny(c.keys) }
        // ...and the covered classes the same ask names, derived from the SAME key lists
        // the `when` matches on, in the `when`'s own order.
        val covered = templateKeys.filter { (_, keys) -> hasAny(keys) }.map { it.first }.distinct()""",
    """        //
        // ROUND 20 REPLACED "does this word appear" WITH "is the ask ABOUT this class".
        // Section 3 measured what the first rule cost once round 19's vocabulary made it
        // wide: SEVEN asks for classes this server SHIPS A TEMPLATE FOR were refused with
        // nothing scaffolded - a vault whose oracle is sampled at random intervals, a
        // staking pool whose validator is chosen at random, a marketplace with a giveaway,
        // an exchange that assigns order ids randomly, a governance DAO that picks a
        // proposal at random for audit, a vault reading a randomness beacon, and a
        // stablecoin whose peg `uses no VRF` - refused for saying the word it says it does
        // not use. Every one of them names its real class as the HEAD of the ask.
        //
        // So each class gets a ROLE from where its keys landed:
        //   HEAD       - a key of this class is inside the head noun phrase. The ask is
        //                about this class, and it is answered by it.
        //   STRONG_OUT - a STRONG key (the class by name) landed after the head. The ask
        //                names this class as a second thing: a compound ask.
        //   MENTION    - only WEAK keys landed, and after the head. The word describes how
        //                something works; it does not say what is being built.
        val roles = LinkedHashMap<String, Int>()
        conceptClasses.forEach { c ->
            val strong = liveStarts(c.keys)
            val weak = liveStarts(c.weakKeys)
            if (strong.isEmpty() && weak.isEmpty()) return@forEach
            roles[c.id] = when {
                (strong + weak).any { it < headEnd } -> ROLE_HEAD
                strong.isNotEmpty() -> ROLE_STRONG_OUT
                else -> ROLE_MENTION
            }
        }
        // ...and the covered classes the same ask names, derived from the SAME key lists
        // the `when` matches on, in the `when`'s own order.
        val covered = templateKeys.filter { (_, keys) -> hasAny(keys) }.map { it.first }.distinct()
        // AN UNCOVERED CLASS IS STILL ANSWERED FIRST, whatever else the ask names - that
        // is round 18's fix and it is unchanged. What round 20 narrowed is WHEN the class
        // counts as named: a mere MENTION of it beside a covered head no longer refuses
        // the ask, it annotates the scaffold.
        val declined = conceptClasses.filter { c ->
            c.template == null && roles.containsKey(c.id) &&
                (roles[c.id] != ROLE_MENTION || covered.isEmpty())
        }
        // The classes that DO have a template and that this ask names. In practice this is
        // the raffle, and the branch below is what makes a raffle ask scaffold.
        if (declined.isEmpty()) {
            conceptClasses.forEach { c ->
                val role = roles[c.id]
                if (c.template == null || role == null) return@forEach
                // The class's OWN template is in `covered` whenever a strong key fired, so
                // "does this ask name a SECOND covered class" is asked about the others.
                val otherCovered = covered.filter { it != c.template }
                when {
                    role == ROLE_HEAD -> return c.note
                    role == ROLE_STRONG_OUT && otherCovered.isEmpty() -> return c.note
                    role == ROLE_STRONG_OUT -> return bothCoveredNote(c, covered)
                    otherCovered.isEmpty() -> return c.note
                }
            }
        }
        // Whatever is left is a MENTION beside a covered head: the ask scaffolds, and the
        // mention is named in a note rather than swallowed.
        val mentioned = conceptClasses.filter { c -> roles[c.id] == ROLE_MENTION && c !in declined }""",
    "declined/covered derivation",
)

sub(
    """        return when {
            declined.isEmpty() && templateNote != null -> templateNote
            declined.isEmpty() ->""",
    """        return when {
            declined.isEmpty() && templateNote != null -> templateNote + mentionTail(mentioned)
            declined.isEmpty() ->""",
    "return when",
)

# ------------------------------------------------------------- 3. the token classes
sub(
    """    private val COUNT_WORDS = listOf(""",
    """    /**
     * ROUND 20 - THE THREE WORD SETS THE HEAD/MENTION RULE READS, and they are closed
     * lists on purpose: a rule that guessed at sentence structure would be a parser, and
     * a parser that is wrong is worse than a keyword that is wrong because nobody can see
     * where it went wrong.
     *
     * [HEAD_BOUNDARY] is where the ask's head noun phrase ends - the relative pronouns
     * and conjunctions after which everything either modifies the head or names a second
     * thing. [BUILD_VERBS] and [ARTICLES] are skipped before the head starts, so `build me
     * a raffle` has the same head as `a raffle`. [NEGATORS] is the window that makes `uses
     * no VRF` not a VRF ask, in the eight languages the concept vocabularies claim.
     */
    private val HEAD_BOUNDARY = setOf(
        "that", "which", "whose", "who", "where", "when", "with", "and", "but",
        "for", "plus", "also", "using", "uses", "use", "while", "so", "if",
        "including", "includes", "include", "offering", "offers", "offer"
    )
    private val BUILD_VERBS = setOf(
        "build", "make", "create", "scaffold", "implement", "write", "design",
        "generate", "want", "need"
    )
    private val ARTICLES = setOf("a", "an", "the", "me", "my", "us", "our", "some", "one")
    private val NEGATORS = setOf(
        "no", "not", "never", "without", "non", "nor", "none", "neither",
        "excludes", "exclude", "excluding", "avoids", "avoid", "avoiding",
        // French, Spanish, German, Dutch, Italian, Portuguese, Polish.
        "sans", "sin", "ohne", "zonder", "senza", "sem", "bez"
    )

    /** The three roles a concept class can play in an ask - see [closestTemplateNote]. */
    private const val ROLE_HEAD = 0
    private const val ROLE_STRONG_OUT = 1
    private const val ROLE_MENTION = 2

    private val COUNT_WORDS = listOf(""",
    "COUNT_WORDS declaration",
)

# ---------------------------------------------------------------- 4. the two tails
sub(
    """    /**
     * The staking answer, in one place because TWO branches give it: the keyword""",
    '''    /**
     * TWO COVERED CLASSES IN ONE ASK, and nothing is scaffolded for it. `a lending pool
     * that also runs a weekly raffle for depositors` is round 18's laundering, and the
     * answer is the same one it has had since: a compound ask is built ONE TEMPLATE AT A
     * TIME, each asked for by name. What round 20 changed is that BOTH halves now have a
     * template, so this says "ask for each of these" instead of "this half has no guards
     * at all".
     */
    private fun bothCoveredNote(named: ConceptClass, covered: List<String>): String {
        val each = covered.joinToString(", ") { "`template=$it` (${templateClasses.getValue(it)})" }
        return "THIS ASK NAMES MORE THAN ONE COVERED CLASS, AND NOTHING WAS SCAFFOLDED FOR IT: it is " +
            "${named.label} AND at least one other class this server ships a template for. Every half " +
            "of it has one - $each - and each is a DIFFERENT exploit class with its own guards, its " +
            "own invariant tests and its own adversary round behind it. Ask for them one at a time, " +
            "by name, and wire the two modules together yourself: a single scaffold that tried to be " +
            "both would carry neither set of guards honestly. ADVERSARY ROUND 18 MEASURED WHY THIS IS " +
            "A NO AND NOT A BEST-EFFORT SCAFFOLD: \\"a lending pool that also runs a weekly raffle for " +
            "depositors\\" was scaffolded onto `lending` - three files, ok:true, 1517 bytes about lazy " +
            "interest accrual and share pricing, and NOT ONE WORD about the raffle. The raffle half " +
            "built from that answer was drained on a real chain: trudy staked 90 of 1890, about one " +
            "week in twenty-one, and won FIVE OF FIVE weekly draws."
    }

    /**
     * ROUND 20, THE OTHER HALF OF FINDING C. The ask scaffolds - its head is a class this
     * server covers - and a word in it belongs to a class that is NOT what is being built.
     * `a vault that reads a randomness beacon as a price input` is a vault; `randomness`
     * is how its feed behaves. Before round 20 that word REFUSED the ask outright. Now it
     * adds this, so nothing is silently swallowed either: the agent gets its template AND
     * is told what the other word would have needed if it turns out to be load-bearing.
     */
    private fun mentionTail(mentioned: List<ConceptClass>): String {
        if (mentioned.isEmpty()) return ""
        val labels = mentioned.joinToString(" and ") { it.label }
        return " AND ONE MORE THING THE ASK SAYS, NAMED RATHER THAN SWALLOWED: it also uses a word " +
            "that belongs to $labels - and the scaffold above does NOT cover that class. It is " +
            "answered as a note and not as a refusal because the class is not what you asked to " +
            "build: it describes how something in your ask behaves. Round 20 measured the cost of " +
            "reading it the other way - SEVEN asks for classes this server ships a template for were " +
            "refused outright with nothing scaffolded, including `a stablecoin whose peg is " +
            "deterministic and USES NO VRF`, refused for saying the word it says it does not use. " +
            "BUT IF THAT WORD IS LOAD-BEARING IN YOUR DESIGN - if an outcome in it is meant to be " +
            "UNPREDICTABLE rather than merely irregular - stop and ask for that class on its own: " +
            mentioned.joinToString("; and ") {
                if (it.template != null) "`template=${it.template}` covers ${it.label}"
                else "what no template here ships for ${it.label} is ${it.missingGuard}"
            } + ". A draw an operation's signer can predict is not a draw, it is a withdrawal, and " +
            "that is the class both adversary rounds 18 and 20 drained."
    }

    /**
     * The staking answer, in one place because TWO branches give it: the keyword''',
    "STAKING_NOTE doc",
)

io.open(KT, "w", encoding="utf-8", newline=NL).write(text)
print("routing decision written")
