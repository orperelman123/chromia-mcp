"""ROUND 20 FIX LANE - the routing rule, prototyped and measured before it is written
in Kotlin.

The host runs ONE gradle slot shared by six lanes, so the concept-vocabulary rule is
worked out here against every pinned corpus in the repository and ported to
DappScaffold.kt once. Nothing here ships; it is the measurement that says what to write.

Run: python scripts/r20_routing_proto.py
"""
import json
import re
import sys
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KEYS = json.loads((ROOT / "build/r20-keys.json").read_text(encoding="utf-8"))
RES = ROOT / "app/src/test/resources/exploit-corpus/realworld"

# --------------------------------------------------------------------------- fold
CYRILLIC = {
    "а": "a", "б": "b", "в": "v", "г": "g", "ґ": "g", "д": "d", "е": "e", "ё": "e",
    "є": "ye", "ж": "zh", "з": "z", "и": "i", "і": "i", "ї": "yi", "й": "y", "к": "k",
    "л": "l", "м": "m", "н": "n", "о": "o", "п": "p", "р": "r", "с": "s", "т": "t",
    "у": "u", "ў": "u", "ф": "f", "х": "kh", "ц": "ts", "ч": "ch", "ш": "sh",
    "щ": "shch", "ъ": "", "ы": "y", "ь": "", "э": "e", "ю": "yu", "я": "ya",
}
GREEK = {
    "α": "a", "β": "v", "γ": "g", "δ": "d", "ε": "e", "ζ": "z", "η": "i", "θ": "th",
    "ι": "i", "κ": "k", "λ": "l", "μ": "m", "ν": "n", "ξ": "x", "ο": "o", "π": "p",
    "ρ": "r", "σ": "s", "ς": "s", "τ": "t", "υ": "y", "φ": "f", "χ": "ch", "ψ": "ps",
    "ω": "o",
}
HAND = {"ß": "ss", "ø": "o", "æ": "ae", "œ": "oe", "ł": "l", "đ": "d", "ð": "d",
        "þ": "th", "ı": "i"}
COMBINING = re.compile(r"[̀-ͯ҃-҉֑-ֽ]+")
TOKEN = re.compile(r"[a-z0-9]+")


def fold(raw):
    s = unicodedata.normalize("NFD", raw.lower())
    s = COMBINING.sub("", s)
    out = []
    for c in s:
        if c in CYRILLIC:
            out.append(CYRILLIC[c])
        elif c in GREEK:
            out.append(GREEK[c])
        elif c in HAND:
            out.append(HAND[c])
        else:
            out.append(c)
    return "".join(out)


def tokens(raw):
    return TOKEN.findall(fold(raw))


# --------------------------------------------------------------------------- keys
RAFFLE_STRONG = [
    "lotter*", "loter*", "lotto*", "loto",
    "raffle*", "sweepstake*", "prize draw*", "prize drawing*", "lucky draw*", "lucky dip*",
    "random winner*", "random draw*", "random selection*",
    "tombola*", "jackpot*", "scratch card*", "scratchcard*",
    "winning ticket*", "raffle ticket*", "chance to win*", "pick a winner*", "picks a winner*",
    "picks the winner*", "draws a winner*",
    "tirage*", "gagnant au hasard*",
    "sorteo*", "sorteio*", "rifa*", "premio mayor*",
    "verlosung*", "gewinnspiel*", "ziehung*", "auslosung*",
    "estrazion*", "sorteggio*",
    "loterij*", "losowanie*", "trekking*",
    "klirosi*", "klirose*", "lacheio*", "rozygrysh*",
]
RAFFLE_WEAK = [
    "random", "at random", "randomly*", "randomness", "chosen at random*",
    "rng", "vrf", "giveaway*", "give away*", "sortition",
    "hasard", "aleatoire*", "azar", "aleatorio*", "aleatoria*", "zufall*",
]
WAGERING = [
    "prediction market*", "betting*", "bettor*", "wager*", "gambl*", "casino*",
    "dice roll*", "coin flip*", "coinflip*", "roulette*", "bingo*",
    "sportsbook*", "sports book*", "bookmaker*", "parimutuel*", "pari mutuel*", "parlay*",
    "odds of winning*",
    "pari", "parier*", "parieur*", "paris sportifs*", "jeu de hasard*",
    "apuest*", "apost*", "juego de azar*",
    "glucksspiel*", "gluecksspiel*", "wette*",
    "scommess*", "zaklad bukmacherski*",
    "stoichima*", "tzogos*", "stavka*", "azartn*",
]
LOYALTY = [
    "loyalty*", "points program*", "points programme*", "reward points*", "loyalty point*",
    "points for purchases*", "frequent flyer*", "air miles*", "stamp card*", "punch card*",
    "cashback*", "cash back*", "store credit*", "gift card*", "voucher*",
    "programme de fidelite*", "programa de fidelidad*", "puntos de fidelidad*",
    "treuepunkt*", "kundenbindungsprogramm*", "bonuspunkt*",
    "programa de fidelidade*", "programma fedelta*", "punti fedelta*",
    "spaarpunten*", "program lojalnosciowy*",
]
FEE_SPLITTER = [
    "fee splitter*", "fee split*", "revenue split*", "revenue share*", "revenue sharing*",
    "payment splitter*", "split the fee*", "splits the fee*", "splitting the fee*",
    "split the revenue*", "splits the revenue*", "profit share*", "profit split*",
    "donation pool*", "donation*", "charity*", "charitable*", "tip jar*",
    "weighted recipient*", "payout split*", "split between recipients*",
    "repartition des frais*", "partage des revenus*", "cagnotte caritative*",
    "reparto de ingresos*", "division de comisiones*", "donacion*",
    "einnahmenaufteilung*", "gebuhrenaufteilung*", "spenden*",
    "divisao de receita*", "doacao*", "doacoes*",
    "divisione dei ricavi*", "donazion*",
    "opbrengstverdeling*", "podzial oplat*",
]

B = KEYS["blocks"]
# The covered routing, in the `when`'s order, with the raffle branch FIRST.
TEMPLATE_KEYS = [("raffle", RAFFLE_STRONG)] + [(n, B[k]) for n, k in KEYS["templateKeys"]]

SEPARATE = [
    # id, template (None = no template ships for it), strong keys, weak keys
    ("unpredictable-outcome", "raffle", RAFFLE_STRONG, RAFFLE_WEAK),
    ("wagering", None, WAGERING, []),
    ("payment-channel", None, B["PAYMENT_CHANNEL_KEYS"], []),
    ("signer-set", None, B["SIGNER_SET_KEYS"], []),
    ("crowdfunding", None, B["CROWDFUNDING_KEYS"], []),
    ("loyalty-points", None, LOYALTY, []),
    ("fee-splitter", None, FEE_SPLITTER, []),
]

NEGATORS = {"no", "not", "never", "without", "non", "nor", "none", "neither",
            "excludes", "exclude", "excluding", "avoids", "avoid", "avoiding",
            "sans", "sin", "ohne", "zonder", "senza", "sem", "bez"}
# Where the ask's HEAD noun phrase ends. Everything from here on modifies it or
# names a second thing.
BOUNDARY = {"that", "which", "whose", "who", "where", "when", "with", "and", "but",
            "for", "plus", "also", "using", "uses", "use", "while", "so", "if",
            "including", "includes", "include", "offering", "offers", "offer"}
BUILD = {"build", "make", "create", "scaffold", "implement", "write", "design",
         "generate", "want", "need"}
ARTICLES = {"a", "an", "the", "me", "my", "us", "our", "some", "one"}

HEAD = "HEAD"
STRONG_OUT = "STRONG_OUT"
MENTION = "MENTION"


def key_spans(key, toks):
    prefix = key.endswith("*")
    parts = TOKEN.findall(fold(key[:-1] if prefix else key))
    out = []
    if not parts or len(parts) > len(toks):
        return out
    for start in range(len(toks) - len(parts) + 1):
        ok = True
        for j, part in enumerate(parts):
            t = toks[start + j]
            hit = t.startswith(part) if (prefix and j == len(parts) - 1) else t == part
            if not hit:
                ok = False
                break
        if ok:
            out.append((start, start + len(parts)))
    return out


def negated(span, toks):
    s = span[0]
    return any(t in NEGATORS for t in toks[max(0, s - 3):s])


def head_end(toks):
    start = 0
    while start < len(toks) and (toks[start] in BUILD or toks[start] in ARTICLES):
        if toks[start] in BUILD:
            start += 1
            while start < len(toks) and toks[start] in ARTICLES:
                start += 1
            break
        break
    for i in range(start, len(toks)):
        if toks[i] in BOUNDARY:
            return i
    return len(toks)


def matches(keys, toks):
    out = []
    for k in keys:
        for span in key_spans(k, toks):
            if not negated(span, toks):
                out.append((k, span))
    return out


def analyse(ask):
    toks = tokens(ask)
    hend = head_end(toks)
    roles = {}
    hits = {}
    for cid, template, strong, weak in SEPARATE:
        ms = matches(strong, toks)
        mw = matches(weak, toks)
        if not ms and not mw:
            continue
        hits[cid] = [k for k, _ in ms + mw]
        if any(sp[0] < hend for _, sp in ms + mw):
            roles[cid] = HEAD
        elif ms:
            roles[cid] = STRONG_OUT
        else:
            roles[cid] = MENTION
    covered = [name for name, keys in TEMPLATE_KEYS if matches(keys, toks)]
    return toks, hend, roles, hits, covered


def route(ask):
    """Returns (template or None, why) - the decision closestTemplateNote will make."""
    toks, hend, roles, hits, covered = analyse(ask)
    by_id = {c[0]: c for c in SEPARATE}
    declining = [cid for cid, r in roles.items()
                 if by_id[cid][1] is None and (r in (HEAD, STRONG_OUT) or not covered)]
    if declining:
        return None, "declined:" + ",".join(sorted(declining)) + (
            "+covered:" + ",".join(covered) if covered else "")
    withtpl = [cid for cid, r in roles.items() if by_id[cid][1] is not None]
    for cid in withtpl:
        r = roles[cid]
        tpl = by_id[cid][1]
        if r == HEAD:
            return tpl, "head:" + cid
        if r == STRONG_OUT:
            if covered:
                return None, "compound:" + cid + "+" + ",".join(covered)
            return tpl, "no-covered-head:" + cid
        # MENTION
        if not covered:
            return tpl, "no-covered-head:" + cid
    for name, keys in TEMPLATE_KEYS:
        if matches(keys, toks):
            note = "+mention:" + ",".join(sorted(roles)) if roles else ""
            return name, "when:" + name + note
    return None, "roster"


# --------------------------------------------------------------------------- corpus
def load(p):
    return json.loads((RES / p).read_text(encoding="utf-8"))


def main():
    rows = []
    r18 = load("round18-template-fix/redirect/raw.json")
    for r in r18:
        rows.append(("r18", r["ask"], r["closestTemplate"] or None))
    r17 = load("round17-template-fix/redirect/raw.json")
    for r in r17:
        rows.append(("r17", r["ask"], r["closestTemplate"] or None))
    r16 = load("adversary-round16/redirect/raw.json")
    for r in r16:
        rows.append(("r16", r["ask"], r["closestTemplate"] or None))
    r20 = load("adversary-round20/redirect/redirect-probes.json")
    for r in r20:
        rows.append(("r20", r["ask"], r["template"] or None))

    # What this lane INTENDS to change, ask -> new expectation.
    intended = {}
    for ask in [
        "a raffle with on-chain randomness",
        "a lottery that pays a random winner from the prize pool",
        "a weekly lottery with rewards for ticket holders",
        "raffles",
        "on-chain lottery",
        "une loterie hebdomadaire avec tirage au sort",
        "a lottery",
        "a weekly raffle that pays a random winner",
        "a tombola for token holders that pays out weekly",
        "une loterie hebdomadaire pour les deposants",
        "un sorteo semanal de premios para los depositantes",
        "a weekly лотерея for token holders",
        "еженедельная лотерея для держателей токенов",
        "εβδομαδιαία κλήρωση για κατόχους",
    ]:
        intended[ask] = "raffle"
    intended["a lending pool without any raffle"] = "lending"
    for ask, tpl in [
        ("a price oracle vault that samples its feed at random intervals", "vault"),
        ("a staking pool whose validator is chosen at random each epoch", "staking"),
        ("a marketplace with a giveaway of free listings for new sellers", "marketplace"),
        ("a stablecoin whose peg is deterministic and uses no VRF", "stablecoin"),
        ("an order book exchange that assigns order ids randomly", "exchange"),
        ("a governance DAO that picks a proposal at random for audit", "governance"),
        ("a vault that reads a randomness beacon as a price input", "vault"),
    ]:
        intended[ask] = tpl
    for ask in ["a points program where the issuer mints rewards for purchases"]:
        intended[ask] = None

    seen = {}
    for origin, ask, before in rows:
        seen.setdefault(ask, (origin, before))

    bad = []
    flips = []
    for ask, (origin, before) in seen.items():
        after, why = route(ask)
        want = intended.get(ask, before)
        if after != want:
            bad.append((origin, ask, before, after, want, why))
        elif after != before:
            flips.append((origin, ask, before, after, why))

    # Round 19's own corpora, read out of the test that pins them.
    r19 = (ROOT / "app/src/test/kotlin/org/chromia/Round19TemplateSurfaceProbeTest.kt").read_text(
        encoding="utf-8")
    block_re = re.compile(r'"([a-z\-]+)" to listOf\((.*?)\n            \)', re.S)
    quoted = re.compile(r'"((?:[^"\\]|\\.)*)"')
    lang_rows = []
    for cid, block in block_re.findall(r19):
        for ask in quoted.findall(block):
            ask = ask.encode().decode("unicode_escape")
            after, why = route(ask)
            lang_rows.append((cid, ask, after, why))
            # The raffle half of round 19's unpredictable-outcome corpus now has a
            # template; the wagering half must keep declining. Every OTHER class
            # must still decline exactly as it did.
            if cid == "unpredictable-outcome":
                if after not in (None, "raffle"):
                    bad.append(("r19-lang", ask, cid, after, "raffle-or-declined", why))
            elif after is not None:
                bad.append(("r19-lang", ask, cid, after, None, why))
    for ask, want in re.findall(r'"((?:[^"\\]|\\.)*)" to "([a-z0-9]+)"', r19):
        ask = ask.encode().decode("unicode_escape")
        if " " not in ask:
            continue
        after, why = route(ask)
        if after != want:
            bad.append(("r19-clean", ask, "?", after, want, why))
    print("=== ROUND 19's MULTILINGUAL CORPUS, RE-ROUTED ===")
    for cid, ask, after, why in lang_rows:
        print("  %-22s %-46s -> %-11s %s" % (cid, ask[:46], after, why))

    extra = [
        ("a loyalty programme with points for shoppers", None),
        ("a fee splitter that pays revenue to weighted recipients", None),
        ("a charity donation pool split between causes by weight", None),
        ("a yield aggregator that splits the fees it earns", None),
        ("a lending pool that also runs a weekly raffle for depositors", None),
        ("an amm that pays a lottery jackpot from the swap fee", None),
        ("a marketplace with an escrow and a multisig treasury", None),
        ("a staking contract with a payment channel for the rewards", None),
        ("a prediction market", None),
        ("sports betting", None),
        ("a weekly raffle for token holders", "raffle"),
        ("build me a raffle", "raffle"),
        ("a commit-reveal raffle with a deposit", "raffle"),
        ("a lending pool without any raffle", "lending"),
        ("a lending pool with a weekly raffle, a cross-chain bridge and an order book", None),
        ("a gaming item shop", None),
        ("a loyalty programme with points", None),
        ("a charity donation pool", None),
        ("a fee splitter contract", None),
        ("a lending market for abetting collateral positions", "lending"),
    ]
    for ask, want in extra:
        after, why = route(ask)
        if after != want:
            bad.append(("extra", ask, "?", after, want, why))

    print("=== MISMATCHES (%d) ===" % len(bad))
    for origin, ask, before, after, want, why in bad:
        print("  [%s] %-64s before=%-11s after=%-11s want=%-11s  %s"
              % (origin, ask[:64], before, after, want, why))
    print("\n=== DELIBERATE FLIPS (%d) ===" % len(flips))
    for origin, ask, before, after, why in flips:
        print("  [%s] %-64s %-11s -> %-11s  %s" % (origin, ask[:64], before, after, why))
    print("\ncorpus: %d distinct asks" % len(seen))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
