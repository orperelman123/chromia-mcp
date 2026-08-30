from pathlib import Path
import re

HELP = Path('/workspace/chromia-mcp/app/src/main/kotlin/org/chromia/tools')

FIRE0066 = [
    "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript",
    "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart",
    "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference",
    "https://docs.chromia.com/build/clients/postchain-clients/kotlin-client",
    "https://docs.chromia.com/build/clients/postchain-clients/python-client",
    "https://docs.chromia.com/build/configuration/project-config",
    "https://docs.chromia.com/build/database/getting-started",
    "https://docs.chromia.com/build/database/overview",
]

holes = []
find_path = Path('/workspace/chromia-mcp/leftover-find-0066.txt')
if find_path.exists():
    for line in find_path.read_text().splitlines():
        if line.startswith('https://') and '| HEAD' in line:
            holes.append(line.split(' | ')[0].strip())
# Always include the fire-0066 target URLs.
for u in FIRE0066:
    if u not in holes:
        holes.append(u)

encoded = set()
incomplete = []
for p in sorted(HELP.glob('*Help.kt')):
    t = p.read_text()
    str_consts = dict(re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', t))
    alias_map = dict(re.findall(r'const val (\w+)\s*=\s*(\w+)\s*$', t, re.M))
    def resolve(name, depth=0):
        if name in str_consts:
            return str_consts[name]
        if depth > 5:
            return None
        if name in alias_map:
            return resolve(alias_map[name], depth + 1)
        return None
    prefs = set()
    for name in list(str_consts) + list(alias_map):
        if name.endswith('_INDEX_URL'):
            prefs.add(name[:-10])
        elif name.endswith('_INDEX_URL_SLASH'):
            prefs.add(name[:-16])
        elif name.endswith('_INDEX_TITLE'):
            prefs.add(name[:-12])
    for pref in prefs:
        u = resolve(pref + '_INDEX_URL')
        s = resolve(pref + '_INDEX_URL_SLASH')
        title = resolve(pref + '_INDEX_TITLE')
        if u and s and title:
            for x in (u, s):
                encoded.add(x.rstrip('/'))
                encoded.add(x.rstrip('/') + '/')
        elif u or s or title:
            incomplete.append((p.name, pref, bool(u), bool(s), bool(title), u))

def covered(url):
    u = url.rstrip('/')
    return u in encoded or (u + '/') in encoded

real = [h for h in holes if not covered(h)]
print('candidate_holes_from_buggy_finder', len(holes))
print('encoded_urls', len(encoded))
print('real_uncovered', len(real))
for h in real:
    print('HOLE', h)
print('incomplete_count', len(incomplete))
for x in incomplete[:20]:
    print('INCOMPLETE', x)
print('fire0066_targets', len(FIRE0066))
print('fire0066_covered', sum(1 for u in FIRE0066 if covered(u)))
print('fire0066_uncovered', sum(1 for u in FIRE0066 if not covered(u)))
for u in FIRE0066:
    print(('COVERED' if covered(u) else 'UNCOVERED'), u)
