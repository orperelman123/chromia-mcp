"""Extract the DappScaffold key lists into JSON so the round-20 routing rule can
be prototyped and measured outside a gradle build."""
import json
import re
import sys
from pathlib import Path

SRC = Path(sys.argv[1] if len(sys.argv) > 1 else
           "app/src/main/kotlin/org/chromia/tools/DappScaffold.kt")
OUT = Path(sys.argv[2] if len(sys.argv) > 2 else "build/r20-keys.json")

text = SRC.read_text(encoding="utf-8")

blocks = {}
for m in re.finditer(r"private val ([A-Z0-9_]+_KEYS) = listOf\(", text):
    name = m.group(1)
    i = m.end()
    depth = 1
    while depth > 0:
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
        i += 1
    body = text[m.end():i - 1]
    body = re.sub(r"//[^\n]*", "", body)
    keys = re.findall(r'"([^"]*)"', body)
    assert keys, name
    blocks[name] = keys

assert len(blocks) >= 19, blocks.keys()

# The routing order, derived from templateKeys.
tk = re.search(r"internal val templateKeys: List<Pair<String, List<String>>> = listOf\((.*?)\n    \)",
               text, re.S)
assert tk, "templateKeys not found"
pairs = re.findall(r'"([a-z0-9_]+)" to ([A-Z0-9_]+_KEYS)', tk.group(1))
assert len(pairs) == 15, pairs

untemplated = re.findall(r'id = "([a-z\-]+)",\s*\n\s*label = "([^"]*)",\s*\n\s*keys = ([A-Z0-9_]+_KEYS)', text)
assert len(untemplated) == 4, untemplated

OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text(json.dumps({
    "blocks": blocks,
    "templateKeys": pairs,
    "untemplated": untemplated,
}, indent=1), encoding="utf-8")
print("wrote", OUT, len(blocks), "key lists,", len(pairs), "routing pairs,", len(untemplated), "uncovered classes")
