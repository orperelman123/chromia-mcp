#!/usr/bin/env python3
"""Fire 0056: live sitemap leftover INDEX with Class.CONST cross-file resolve."""
import json, re, urllib.request, xml.etree.ElementTree as ET
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import ssl

HELP_DIR = Path("/workspace/chromia-mcp/app/src/main/kotlin/org/chromia/tools")
SKIP_EXACT = {
    "https://docs.chromia.com/ignore_me",
    "https://docs.chromia.com/search",
    "https://learn.chromia.com/search",
    "https://learn.chromia.com/login",
    "https://learn.chromia.com/ignore",
}
SKIP_PATH_RE = re.compile(r"/ignore_me|/search(?:/|$)|/block-explorer", re.I)
AUTH_WRITE_SKIP = re.compile(
    r"/(?:register|login|transfer|auth|keygen|admin|compare-authentication)(?:/|$)", re.I
)

def noslash(url):
    if url.endswith("/") and url.count("/") > 3:
        return url[:-1]
    return url

def slash_of(url):
    u = noslash(url)
    return u if u.endswith("/") else u + "/"

str_re = re.compile(r'const val (\w+)\s*=\s*"([^"]+)"')
alias_re = re.compile(r'const val (\w+)\s*=\s*([A-Za-z_][\w.]*)')

# Load ALL kotlin files in tools dir for Class.CONST resolve
global_strs = {}  # "Class.CONST" -> value OR "CONST" from same file later
file_strs = {}    # filename -> {name: value}
file_aliases = {} # filename -> {name: "Class.CONST" or "CONST"}
class_of = {}     # filename stem -> object/class name approx = stem

for p in sorted(HELP_DIR.glob("*.kt")):
    t = p.read_text()
    stem = p.stem
    class_of[stem] = stem
    # also detect object/class name
    m = re.search(r'(?:object|class)\s+(\w+)', t)
    cname = m.group(1) if m else stem
    class_of[stem] = cname
    strs = dict(str_re.findall(t))
    aliases = {}
    for name, alias in alias_re.findall(t):
        if name in strs:
            continue
        aliases[name] = alias
    file_strs[p.name] = strs
    file_aliases[p.name] = aliases
    for k, v in strs.items():
        global_strs[f"{cname}.{k}"] = v
        global_strs[k] = v  # last-wins for bare; Class.CONST preferred

# Build resolve that uses Class.CONST globally
def resolve_name(file_name, name, hops=0, seen=None):
    if seen is None:
        seen = set()
    if hops > 12 or (file_name, name) in seen:
        return None
    seen.add((file_name, name))
    strs = file_strs.get(file_name, {})
    aliases = file_aliases.get(file_name, {})
    if name in strs:
        return strs[name]
    if name in aliases:
        alias = aliases[name]
        if "." in alias:
            # Class.CONST
            cls, const = alias.rsplit(".", 1)
            key = f"{cls}.{const}"
            if key in global_strs:
                return global_strs[key]
            # find file for that class
            for fn, st in file_strs.items():
                stem = Path(fn).stem
                cname = class_of.get(stem, stem)
                if cname == cls or stem == cls:
                    return resolve_name(fn, const, hops + 1, seen)
            return None
        else:
            return resolve_name(file_name, alias, hops + 1, seen)
    # try global bare
    if name in global_strs and global_strs[name].startswith("http"):
        return global_strs[name]
    return None

pairs = []
incomplete = []
encoded = set()
triple_files = []

for p in sorted(HELP_DIR.glob("*Help.kt")):
    strs = file_strs[p.name]
    aliases = file_aliases[p.name]
    prefixes = set()
    for name in list(strs) + list(aliases):
        if name.endswith("_INDEX_URL"):
            prefixes.add(name[: -len("_INDEX_URL")])
        elif name.endswith("_INDEX_URL_SLASH"):
            prefixes.add(name[: -len("_INDEX_URL_SLASH")])
        elif name.endswith("_INDEX_TITLE"):
            prefixes.add(name[: -len("_INDEX_TITLE")])
    for pref in prefixes:
        uname = pref + "_INDEX_URL"
        sname = pref + "_INDEX_URL_SLASH"
        tname = pref + "_INDEX_TITLE"
        has_u = uname in strs or uname in aliases
        has_s = sname in strs or sname in aliases
        has_t = tname in strs or tname in aliases
        u = resolve_name(p.name, uname) if has_u else None
        s = resolve_name(p.name, sname) if has_s else None
        title = resolve_name(p.name, tname) if has_t else None
        if not (has_u and has_s and has_t):
            if has_u or has_s or has_t:
                incomplete.append({"file": p.name, "pref": pref, "u": has_u, "s": has_s, "t": has_t, "uv": u, "sv": s, "tv": title})
            continue
        urls = [v for v in (u, s) if v and isinstance(v, str) and v.startswith("http")]
        if not urls:
            incomplete.append({"file": p.name, "pref": pref, "u": True, "s": True, "t": True, "uv": u, "sv": s, "tv": title})
            continue
        for v in urls:
            encoded.add(noslash(v))
            encoded.add(slash_of(v))
        pairs.append({"file": p.name, "pref": pref, "u": u or "", "s": s or "", "title": title if isinstance(title, str) else ""})
        triple_files.append(p.name)

# Also: if incomplete has URL-only MODULES covered by RELL_LANGUAGE_MODULES_INDEX_*, note it
# Add any URL that appears as complete INDEX in any form

# Fetch live sitemaps
ctx = ssl.create_default_context()
def fetch_sitemap(url):
    req = urllib.request.Request(url, headers={"User-Agent": "chromia-mcp-leftover/0056"})
    with urllib.request.urlopen(req, context=ctx, timeout=30) as r:
        data = r.read()
    root = ET.fromstring(data)
    ns = {"sm": "http://www.sitemaps.org/schemas/sitemap/0.9"}
    locs = [e.text.strip() for e in root.findall(".//sm:loc", ns) if e.text]
    return locs

docs_locs = fetch_sitemap("https://docs.chromia.com/sitemap.xml")
learn_locs = fetch_sitemap("https://learn.chromia.com/sitemap.xml")
Path("/tmp/docs-locs-live-now.txt").write_text("\n".join(docs_locs) + "\n")
Path("/tmp/learn-locs-live-now.txt").write_text("\n".join(learn_locs) + "\n")

all_locs = []
seen = set()
for loc in docs_locs + learn_locs:
    if SKIP_PATH_RE.search(loc):
        continue
    ns = noslash(loc)
    if ns in {s.rstrip("/") for s in SKIP_EXACT} or noslash(ns) in {noslash(s) for s in SKIP_EXACT}:
        continue
    if ns not in seen:
        seen.add(ns)
        all_locs.append(ns)

cands = [u for u in all_locs if noslash(u) not in encoded and slash_of(u) not in encoded]

def http_check(url):
    ns = noslash(url)
    sl = slash_of(url)
    out = {"url": ns, "head": None, "get": None, "h1": ""}
    try:
        req = urllib.request.Request(ns, method="HEAD", headers={"User-Agent": "chromia-mcp-leftover/0056"})
        with urllib.request.urlopen(req, context=ctx, timeout=15) as r:
            out["head"] = r.status
    except urllib.error.HTTPError as e:
        out["head"] = e.code
    except Exception as e:
        out["head"] = f"ERR:{e}"
    try:
        req = urllib.request.Request(sl, headers={"User-Agent": "chromia-mcp-leftover/0056"})
        with urllib.request.urlopen(req, context=ctx, timeout=20) as r:
            out["get"] = r.status
            body = r.read().decode("utf-8", "replace")
            m = re.search(r"<h1[^>]*>(.*?)</h1>", body, re.I | re.S)
            if m:
                h1 = re.sub(r"<[^>]+>", "", m.group(1))
                h1 = re.sub(r"\s+", " ", h1).strip()
                out["h1"] = h1
    except urllib.error.HTTPError as e:
        out["get"] = e.code
    except Exception as e:
        out["get"] = f"ERR:{e}"
    return out

results = []
with ThreadPoolExecutor(max_workers=12) as ex:
    futs = {ex.submit(http_check, u): u for u in cands}
    for fut in as_completed(futs):
        results.append(fut.result())

real_holes = []
non_holes = []
for r in sorted(results, key=lambda x: x["url"]):
    head = r["head"]
    get = r["get"]
    h1 = r["h1"]
    url = r["url"]
    # skip404
    if get == 404 or head == 404:
        non_holes.append({**r, "class": "404"})
        continue
    if AUTH_WRITE_SKIP.search(url):
        non_holes.append({**r, "class": "auth_write_skip"})
        continue
    if not h1:
        non_holes.append({**r, "class": "empty_h1_csr"})
        continue
    # real hole: redirect or 200 noslash + slash 200 + H1
    ok_head = head in (200, 301, 302, 307, 308) or (isinstance(head, int) and 300 <= head < 400)
    ok_get = get == 200
    if ok_head and ok_get and h1:
        real_holes.append(r)
    else:
        non_holes.append({**r, "class": f"other_head{head}_get{get}"})

out = {
    "fire": "0056",
    "complete_triples": len(pairs),
    "learn_pairs": sum(1 for x in pairs if "learn.chromia.com" in x["u"] + x["s"]),
    "docs_pairs": sum(1 for x in pairs if "docs.chromia.com" in x["u"] + x["s"]),
    "encoded_url_set": len(encoded),
    "docs_locs": len(docs_locs),
    "learn_locs": len(learn_locs),
    "unique_locs": len(all_locs),
    "candidate_count": len(cands),
    "candidates": cands,
    "incomplete": incomplete,
    "real_holes": real_holes,
    "real_hole_count": len(real_holes),
    "non_holes": non_holes,
}
Path("/tmp/leftover-index-resolve-0056.json").write_text(json.dumps(out, indent=2))
Path("/tmp/leftover-index-holes-0056.json").write_text(json.dumps(out, indent=2))
Path("/tmp/leftover-index-holes-now.json").write_text(json.dumps(out, indent=2))
print("complete_triples", len(pairs))
print("encoded_url_set", len(encoded))
print("docs_locs", len(docs_locs), "learn_locs", len(learn_locs), "unique", len(all_locs))
print("candidates", len(cands), "real_holes", len(real_holes), "non_holes", len(non_holes))
print("incomplete", len(incomplete))
print("--- REAL HOLES ---")
for r in real_holes:
    print(r["url"], "|", r["head"], "|", r["get"], "|", r["h1"])
print("--- NON HOLES ---")
for r in non_holes:
    print(r["class"], r["url"], "|", r.get("head"), "|", r.get("get"), "|", repr(r.get("h1")))
print("--- incomplete ---")
for row in incomplete:
    print(row)
