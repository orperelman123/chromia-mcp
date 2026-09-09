"""ROUND 20 FIX LANE - the one identifier the raffle's EXTENDING section names and the
module does not have.

Seam 4 said "If you raise MAX_STAKE, raise DEPOSIT with it". There is no MAX_STAKE: the
cap IS the deposit (`require(stake <= DEPOSIT)`), which is the whole point of the seam, and
an extender who goes looking for `MAX_STAKE` finds nothing. Every other identifier the
section names is real - `seed_of`, `settle_round`, `claim_refund`, `book.burned` - and the
suite's derived check only requires that ONE of them exists, so this would have passed
while still being wrong.

Both copies are patched: the Kotlin raw string that ships, and the proven .rell the embed
script reads, so the two stay byte-identical.
"""
import io
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KT = ROOT / "app/src/main/kotlin/org/chromia/tools/DappScaffold.kt"
RELL = Path(sys.argv[1]) / "src/main.rell" if len(sys.argv) > 1 else None

OLD = """//   4. THE STAKE CAP IS PART OF THE DEPOSIT'S ARITHMETIC. `stake <= DEPOSIT` is what makes
//      walking away from a losing draw cost at least what playing it costs. If you raise
//      MAX_STAKE, raise DEPOSIT with it, or the last revealer is paid to leave."""
NEW = """//   4. THE DEPOSIT IS THE STAKE CAP, AND THAT IS ARITHMETIC RATHER THAN POLICY.
//      `require(stake <= DEPOSIT)` is what makes walking away from a losing draw cost at
//      least what playing it costs: a walker forfeits `DEPOSIT`, a player who loses is out
//      `stake`. There is no separate maximum-stake constant to tune, on purpose - raising
//      the accepted stake means raising `DEPOSIT`, and the two can never drift apart. Round
//      20's module sized its deposit against `prize / participants` instead, which is a
//      fraction of one stake and bounded nothing."""


def patch(path, indent):
    raw = io.open(path, encoding="utf-8", newline="").read()
    nl = "\r\n" if "\r\n" in raw else "\n"
    text = raw.replace("\r\n", "\n")
    old = "\n".join(indent + l if l.strip() else "" for l in OLD.split("\n"))
    new = "\n".join(indent + l if l.strip() else "" for l in NEW.split("\n"))
    assert old in text, "anchor missing in %s" % path.name
    assert text.count(old) == 1, "anchor not unique in %s" % path.name
    io.open(path, "w", encoding="utf-8", newline=nl).write(text.replace(old, new))
    print("patched", path.name)


patch(KT, "        ")
if RELL:
    patch(RELL, "")
