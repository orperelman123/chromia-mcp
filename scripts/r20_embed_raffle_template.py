"""ROUND 20 FIX LANE - embed the raffle template (the sixteenth) into DappScaffold.kt.

The module and its suite were written and proved on a real chain FIRST (chr 0.29.10,
15/15 green, rell_security_check with zero findings); this script is the mechanical
half - it turns the two proven .rell files into the Kotlin raw strings the scaffold
serves, and wires the template into `templates`, `templateClasses` and `files()`.

Every anchor is asserted. Run:  python scripts/r20_embed_raffle_template.py <rell-dir>
"""
import io
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
KT = ROOT / "app/src/main/kotlin/org/chromia/tools/DappScaffold.kt"
SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else None
assert SRC and SRC.is_dir(), "pass the directory holding src/main.rell and src/test/main_test.rell"

MAIN = (SRC / "src/main.rell").read_text(encoding="utf-8")
TEST = (SRC / "src/test/main_test.rell").read_text(encoding="utf-8")

# The two things a Kotlin raw string cannot carry verbatim.
for name, body in (("main.rell", MAIN), ("main_test.rell", TEST)):
    assert "$" not in body, "%s carries a $ - a Kotlin raw string would read it as a template" % name
    assert '"""' not in body, "%s carries a triple quote" % name


def as_raw_string(body):
    """The Rell source indented by 8 spaces, the way every other template is written."""
    out = []
    for line in body.replace("\r\n", "\n").rstrip("\n").split("\n"):
        out.append("        " + line if line.strip() else "")
    return "\n".join(out)


BLOCK = '''    /**
     * THE SIXTEENTH TEMPLATE, and the first one built from a drain of THIS PROJECT'S OWN
     * ADVICE. `docs/TEMPLATE-GAPS.md` carried the raffle row with a design note -
     * "commit-reveal with a deposit the revealer forfeits" - and adversary round 20 built
     * exactly that, proved eleven guards load-bearing on a chain, and was drained five of
     * five draws by an attacker holding 4.76%% of the stake with NOTHING forfeited. The
     * note's sentence "her choice is between REVEALING and FORFEITING her deposit" is
     * true of one commitment and false of two, because the seed folded in REVEAL ORDER.
     *
     * The module below folds it in COMMIT order instead, refuses to draw a round that is
     * not complete, and burns a forfeited deposit rather than paying it into the prize.
     * Measured: 15/15 green on the first run, and in
     * `exploit-corpus/realworld/adversary-round20/raffle-fixed` the same 720-permutation
     * search that won five of five now wins ONE round in fifty against a 4.7619%% stake
     * share.
     */
    private fun raffleMainRell(): String = """
%s
    """.trimIndent() + "\\n"

    private fun raffleTestRell(): String = """
%s
    """.trimIndent() + "\\n"

''' % (as_raw_string(MAIN), as_raw_string(TEST))

text = io.open(KT, encoding="utf-8", newline="").read()
assert "raffleMainRell" not in text, "already embedded"
NL = "\r\n" if "\r\n" in text else "\n"
text = text.replace("\r\n", "\n")

# 1. the roster
old_templates = ('    val templates = listOf("hello", "ft4", "governance", "vault", "staking", '
                 '"marketplace", "lending", "streaming", "amm", "stablecoin", "exchange", '
                 '"subscription", "bridge", "escrow", "insurance")')
assert old_templates in text, "templates list"
text = text.replace(old_templates, old_templates[:-1] + ', "raffle")')

# 2. the one-line class, in the same commit as the template (the init block enforces it)
old_class = '''            "and cover is bounded by the reserve that backs it"
    )'''
new_class = '''            "and cover is bounded by the reserve that backs it",
        "raffle" to "a COMMIT-REVEAL DRAW: the seed folds the committed set in COMMIT order so no "
            + "reveal order can move it, a round that is not complete does not draw, and a forfeited "
            + "deposit is burned rather than paid into the prize"
    )'''
assert old_class in text, "templateClasses tail"
text = text.replace(old_class, new_class)

# 3. the files
old_files = '''            "insurance" -> linkedMapOf(
                "chromia.yml" to insuranceChromiaYml(chain),
                "src/main.rell" to insuranceMainRell(),
                "src/test/main_test.rell" to insuranceTestRell()
            )'''
new_files = old_files + '''
            "raffle" -> linkedMapOf(
                "chromia.yml" to ft4ChromiaYml(chain),
                "src/main.rell" to raffleMainRell(),
                "src/test/main_test.rell" to raffleTestRell()
            )'''
assert old_files in text, "files() insurance branch"
text = text.replace(old_files, new_files)

# 4. the two raw strings, beside the other templates'
anchor = "    private fun insuranceChromiaYml(name: String): String = ft4ChromiaYml("
assert anchor in text, "insuranceChromiaYml anchor"
text = text.replace(anchor, BLOCK + anchor, 1)

io.open(KT, "w", encoding="utf-8", newline=NL).write(text)
print("embedded raffleMainRell (%d lines) and raffleTestRell (%d lines)"
      % (MAIN.count("\n"), TEST.count("\n")))
