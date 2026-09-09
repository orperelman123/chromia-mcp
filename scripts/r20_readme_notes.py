"""ROUND 20 FIX LANE - the "what became of it" notes the round-20 README owes.

Sections 3 (the concept vocabulary) and 6 (the raffle) are the two this lane answered.
Nothing already in the README is edited: the round measured what it measured, and the
notes are appended so the finding and its answer read in order.
"""
import io
from pathlib import Path

P = (Path(__file__).resolve().parent.parent
     / "app/src/test/resources/exploit-corpus/realworld/adversary-round20/README.md")
raw = io.open(P, encoding="utf-8", newline="").read()
NL = "\r\n" if "\r\n" in raw else "\n"
text = raw.replace("\r\n", "\n")


def sub(old, new, why):
    global text
    assert old in text, "anchor missing: " + why
    assert text.count(old) == 1, "anchor not unique: " + why
    text = text.replace(old, new)


SECTION3 = """which is the template whose *"every credit is a pool debit"* the TEMPLATE-GAPS row
for that class says *cannot simply be carried over*.

---
"""

SECTION3_NEW = """which is the template whose *"every credit is a pool debit"* the TEMPLATE-GAPS row
for that class says *cannot simply be carried over*.

### What became of it — FIXED, `fix/round20-template`

**C, the seven false declines: closed at the root.** A class is recognised when the
ask is ABOUT it, not when its word appears. `closestTemplateNote` now reads WHERE a
key landed and gives the class a role: **HEAD** (inside the ask's head noun phrase —
the ask is about this class and is answered by it), **STRONG_OUT** (the class by name
after the head — a compound ask, answered by naming every covered half and scaffolding
none), **MENTION** (only a *weak* key, after the head — the word describes HOW
something works, so it becomes a note beside the scaffold and never a refusal in place
of it). A key inside a NEGATION window — three tokens, eight languages — is the class
being ruled out, so *"uses no VRF"* is not a VRF ask. `random`, `randomness`,
`randomly*`, `rng`, `vrf`, `giveaway*` and `at random` moved from the class's keys to
`RAFFLE_WEAK_KEYS`. All seven asks scaffold their real template, six of them carrying
the mention as a note; the seventh — the stablecoin — has nothing to name, because its
own words rule the class out.

**A, non-Latin: closed for the two scripts that have a settled Latin form, measured for
the six that do not.** `NON_LATIN_FOLD` transliterates Cyrillic and Greek before
tokenising — a hand-written table of 38 characters, no new dependency, every row
exercised by the multilingual corpora. Re-measured: **3 of 9** now reach their class
(Russian, Greek, and the mixed-script ask that used to be scaffolded onto `ft4` with
four files). Chinese, Japanese, Korean, Hebrew, Arabic and Hindi have no single-valued
letter-for-letter Latin form and stay the recorded gap in `docs/TEMPLATE-GAPS.md`.

**B, the two rows with no named refusal: closed for two of three.** `loyalty-points`
and `fee-splitter` are concept classes now, with vocabularies in eight languages and a
named missing guard each. `a gaming item shop` still falls to the roster and is still
the gap — split out of the loyalty row and recorded rather than glossed.

`Round20TemplateVocabularyProbeTest` is flipped row by row the way its own message said
to flip it, and records to NEW frozen files (`redirect/false-declines-closed.json`,
`redirect/laundered-closed.json`, `redirect/non-latin-closed.json`) so the before and
the after are both on disk. The rule was measured before it was written, in
`scripts/r20_routing_proto.py`, against every pinned corpus in the repository: **120
distinct asks, 0 mismatches, exactly the 20 intended flips**, with all 14 clean-pass
asks still routing.

---
"""

sub(SECTION3, SECTION3_NEW, "section 3 tail")

SECTION6 = """The control is the same attacker with the same stake in ONE account — the shape
the forfeit really does bound — and she wins what her stake is worth.

---
"""

SECTION6_NEW = """The control is the same attacker with the same stake in ONE account — the shape
the forfeit really does bound — and she wins what her stake is worth.

### What became of it — FIXED, and SHIPPED as the sixteenth template

The drain is designed out rather than checked for, and then shipped, so the class this
round drained is the class `scaffold_dapp{template:"raffle"}` now answers.

**The design.** `exploit-corpus/realworld/adversary-round20/raffle-fixed` is this
round's own module with three changes, and each closes a different half:

1. **The mix is order-independent structurally, not by a check.** `reveal` stores the
   secret and touches no accumulator at all. The seed is computed ONCE, in
   `settle_round`, folding over the entries in **COMMIT** order — `seq`, an immutable
   field written by `commit` before any secret in the round is known. `seed_of` is a
   function of the committed set, and reveal order is not one of its inputs.
2. **A round draws only when every commitment has been revealed.** With ordering gone
   the only lever left is WHICH of her commitments she reveals — 2^m subsets.
   `settle_round` requires `reveals == commits`, so a subset is not a different draw,
   it is NO draw: the round refunds instead.
3. **Withholding costs at least what losing costs, and pays nobody.** `commit` refuses
   a stake larger than the deposit, and a forfeited deposit is **burned** —
   `book.burned`, which no operation pays out of. This round's module added forfeited
   deposits to the prize, which paid the survivors for the very denial the deposit was
   meant to price.

**Measured on a chain (chr 0.29.10 / rell 0.15.0), 16/16 green:**

| what | this round's module | the fixed design |
|---|---|---|
| all 24 reveal orders of one round | 24 different draws | one seed, one winner |
| the 720-permutation search, 50 rounds | finds an order | finds an order **50 of 50** |
| rounds won on a 4.7619% (47619 ppm) stake share | **5 of 5** | **1 of 50 — 20000 ppm** |
| deposits forfeited / secrets withheld | 0 / 0 | 0 / 0 |
| a withheld subset | drew on what was revealed | no draw; every stake home; she is out one deposit, and it went to nobody |

**The template.** `template=raffle` is the same module in FT4 shape, and it meets the
bar the other fifteen meet: `chromia.yml`, the module, a shipped suite **green on the
honest first run** (15/15 in 65 s), `rell_security_check` with **zero findings**, an
EXTENDING section naming the four seams, the round-20 drain shipped as two must-fail
tests — the 720-permutation search run verbatim against two byte-identical rounds, and
the withheld subset — and `docs/TEMPLATE-GAPS.md`'s top row retired. The Kotlin raw
strings are the proven `.rell` files byte for byte, and `scripts/r20_embed_raffle_template.py`
round-trips them to prove it.

**One MEDIUM finding was designed out rather than accepted:** `rell_security_check`
reported `bulk-mutation-not-caller-bound` on `update entry @* { .round == r }` in
settle. It was redundant — `claim_refund` refuses a settled round in its first
`require` — so it is gone, and `refunded` now means exactly *this entry claimed its own
refund*.

**The permissionless operations** (`commit`, `reveal`, `settle_round`, `claim_refund`)
are written the obvious way: `auth.authenticate()` first, the deposit debited from the
authenticated caller, and every row keyed by that caller. The rules lane's shape for
intentionally permissionless entry points was **not yet on `fix/round20-rules` when
this was written** (that branch had no commits), so this is the shape stated in the
lane brief as the fallback, and the shipped template answers `rell_security_check`
clean as written.

**What this section could not close, and says so instead:** a participant can always
DENY a round by not revealing, for one deposit a round. Nobody profits from the denial
— every stake goes home and the forfeit is burned — so it buys griefing and never a
draw. A raffle that must not be stoppable needs a randomness BEACON read with the
discipline `template=vault` applies to a price, and not a bigger deposit. That is in
the template header and in the redirect's own answer, not only here.

---
"""

sub(SECTION6, SECTION6_NEW, "section 6 tail")

io.open(P, "w", encoding="utf-8", newline=NL).write(text)
print("round-20 README: what became of sections 3 and 6")
