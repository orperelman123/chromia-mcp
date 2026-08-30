#!/usr/bin/env python3
import re, json, time, concurrent.futures, html as htmlmod, http.client
from pathlib import Path
from urllib.parse import urlparse, urljoin
from collections import Counter

HELP_DIR = Path("/workspace/chromia-mcp/app/src/main/kotlin/org/chromia/tools")
SKIP_EXACT = {
    "https://docs.chromia.com/ignore_me",
    "https://docs.chromia.com/search",
    "https://learn.chromia.com/search",
    "https://learn.chromia.com/login",
    "https://learn.chromia.com/ignore",
}
SKIP_PATH_RE = re.compile(r"/ignore_me|/search(?:/|$)|/block-explorer", re.I)
REDIR = {301, 302, 303, 307, 308}
UA = "Mozilla/5.0 (compatible; leftover-index-mapper/1.0)"
H1_RE = re.compile(r"<h1[^>]*>(.*?)</h1>", re.I | re.S)
TAG_RE = re.compile(r"<[^>]+>")
WS_RE = re.compile(r"\s+")

def noslash(url: str) -> str:
    if url.endswith("/") and url.count("/") > 3:
        return url[:-1]
    return url

def slash_of(url: str) -> str:
    u = noslash(url)
    return u if u.endswith("/") else u + "/"

# Extract INDEX const pairs
pair_re = re.compile(
    r"const val (\w+_INDEX_URL)\s*=\s*(?:\"([^\"]+)\"|(\w+))\s*\n"
    r"\s*const val (\w+_INDEX_URL_SLASH)\s*=\s*(?:\"([^\"]+)\"|(\w+))\s*\n"
    r"\s*const val (\w+_INDEX_TITLE)\s*=\s*\"([^\"]+)\"",
    re.M,
)
# looser: collect all INDEX_URL / INDEX_URL_SLASH string literals
url_re = re.compile(r'const val (\w+_INDEX_URL(?:_SLASH)?)\s*=\s*"([^"]+)"')
alias_re = re.compile(r'const val (\w+_INDEX_URL(?:_SLASH)?)\s*=\s*(\w+)')

encoded = set()
pairs = []
help_files = sorted(HELP_DIR.glob("*Help.kt"))
print(f"help files: {len(help_files)}")
for p in help_files:
    t = p.read_text()
    consts = {}
    str_consts = dict(re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', t))
    for name, val in url_re.findall(t):
        consts[name] = val
    for name, alias in alias_re.findall(t):
        if alias in str_consts:
            consts[name] = str_consts[alias]
        elif alias in consts:
            consts[name] = consts[alias]
    # pair by prefix
    prefixes = set()
    for name in consts:
        if name.endswith("_INDEX_URL"):
            prefixes.add(name[:-len("_INDEX_URL")])
        elif name.endswith("_INDEX_URL_SLASH"):
            prefixes.add(name[:-len("_INDEX_URL_SLASH")])
    for pref in prefixes:
        u = consts.get(pref + "_INDEX_URL")
        s = consts.get(pref + "_INDEX_URL_SLASH")
        title_m = re.search(rf'const val {re.escape(pref)}_INDEX_TITLE\s*=\s*"([^"]+)"', t)
        if u and s and title_m:
            encoded.add(noslash(u))
            encoded.add(slash_of(u))
            encoded.add(noslash(s))
            encoded.add(slash_of(s))
            pairs.append((p.name, pref, u, s, title_m.group(1)))

print(f"INDEX pairs: {len(pairs)}")
print(f"encoded urls: {len(encoded)}")
learn_pairs = [x for x in pairs if "learn.chromia.com" in x[2] or "learn.chromia.com" in x[3]]
docs_pairs = [x for x in pairs if "docs.chromia.com" in x[2] or "docs.chromia.com" in x[3]]
print(f"learn pairs: {len(learn_pairs)} docs pairs: {len(docs_pairs)}")

def fetch_sitemap(host_url, out_path):
    p = urlparse(host_url)
    conn = http.client.HTTPSConnection(p.netloc, timeout=30)
    conn.request("GET", p.path or "/sitemap.xml", headers={"User-Agent": UA, "Accept": "application/xml,text/xml,*/*"})
    resp = conn.getresponse()
    body = resp.read()
    status = resp.status
    loc = resp.getheader("Location")
    hops = 0
    while status in REDIR and loc and hops < 4:
        absu = urljoin(host_url, loc)
        pp = urlparse(absu)
        conn.close()
        conn = http.client.HTTPSConnection(pp.netloc, timeout=30)
        conn.request("GET", pp.path or "/", headers={"User-Agent": UA})
        resp = conn.getresponse()
        body = resp.read()
        status = resp.status
        loc = resp.getheader("Location")
        hops += 1
    conn.close()
    Path(out_path).write_bytes(body)
    locs = re.findall(rb"<loc>([^<]+)</loc>", body)
    urls = [u.decode("utf-8", "replace").strip() for u in locs]
    print(f"sitemap {host_url} status={status} locs={len(urls)}")
    return urls

docs_locs = fetch_sitemap("https://docs.chromia.com/sitemap.xml", "/tmp/docs-sitemap-live-now.xml")
learn_locs = fetch_sitemap("https://learn.chromia.com/sitemap.xml", "/tmp/learn-sitemap-live-now.xml")
Path("/tmp/docs-locs-live-now.txt").write_text("\n".join(docs_locs) + "\n")
Path("/tmp/learn-locs-live-now.txt").write_text("\n".join(learn_locs) + "\n")

all_locs = []
for loc in docs_locs + learn_locs:
    if SKIP_PATH_RE.search(loc):
        continue
    ns = noslash(loc)
    if ns in {s.rstrip("/") for s in SKIP_EXACT}:
        continue
    all_locs.append(ns)

# unique preserve order
seen = set()
uniq = []
for u in all_locs:
    if u not in seen:
        seen.add(u)
        uniq.append(u)

candidates = [u for u in uniq if noslash(u) not in encoded and slash_of(u) not in encoded]
print(f"unique sitemap locs (after skipExact): {len(uniq)}")
print(f"unencoded candidates (no INDEX pair): {len(candidates)}")

# HTTP check candidates
def http_req(method, url, read_body=False):
    p = urlparse(url)
    path = p.path or "/"
    if p.query:
        path += "?" + p.query
    try:
        conn = http.client.HTTPSConnection(p.netloc, timeout=20)
        conn.request(method, path, headers={"User-Agent": UA, "Accept": "text/html,application/xhtml+xml,*/*;q=0.8", "Connection": "close"})
        resp = conn.getresponse()
        status = resp.status
        loc = resp.getheader("Location")
        body = resp.read() if read_body else resp.read(0)
        conn.close()
        return status, loc, body, None
    except Exception as e:
        return None, None, b"", str(e)

def extract_h1(body: bytes) -> str:
    if not body:
        return ""
    text = body.decode("utf-8", "replace")
    m = H1_RE.search(text)
    if m:
        raw = m.group(1)
    else:
        tm = re.search(r"<title[^>]*>(.*?)</title>", text, re.I | re.S)
        raw = tm.group(1) if tm else ""
    if not raw:
        return ""
    raw = TAG_RE.sub("", raw.replace("&nbsp;", " "))
    raw = htmlmod.unescape(raw)
    raw = WS_RE.sub(" ", raw).strip().replace("\u200b", "")
    for suf in (" | Chromia Learning", " | Chromia"):
        if raw.endswith(suf):
            raw = raw[: -len(suf)].strip()
    return raw

def abs_loc(base, loc):
    if not loc:
        return None
    return urljoin(base, loc)

def check_one(url):
    ns = noslash(url)
    st, loc, _, err = http_req("HEAD", ns, read_body=False)
    if st in (404, 410):
        return {"url": ns, "head": st, "skip": "404"}
    if st not in REDIR and st != 200:
        return {"url": ns, "head": st, "err": err, "skip": "other"}
    su = abs_loc(ns, loc) if st in REDIR and loc else slash_of(ns)
    gst, gloc, body, gerr = http_req("GET", su, read_body=True)
    hops = 0
    while gst in REDIR and gloc and hops < 3:
        su = abs_loc(su, gloc)
        gst, gloc, body, gerr = http_req("GET", su, read_body=True)
        hops += 1
    if gst != 200:
        return {"url": ns, "head": st, "slash_url": su, "get": gst, "skip": "get_fail", "err": gerr}
    h1 = extract_h1(body)
    return {"url": ns, "head": st, "slash_url": su, "get": gst, "h1": h1, "hole": True}

print(f"HTTP checking {len(candidates)} candidates...", flush=True)
t0 = time.time()
results = []
with concurrent.futures.ThreadPoolExecutor(max_workers=16) as ex:
    futs = list(ex.map(check_one, candidates))
    results = futs
print(f"HTTP done in {time.time()-t0:.1f}s", flush=True)

holes = [r for r in results if r.get("hole")]
skip404 = [r for r in results if r.get("skip") == "404"]
print(f"HOLES={len(holes)} skip404={len(skip404)} other={len(results)-len(holes)-len(skip404)}")

# classify
def prio(u):
    if "/rell/" in u or "/architecture" in u or "/platform" in u or "/language-features" in u:
        return 0
    if "/build/" in u or "/chr" in u or "/ft4" in u or "chromia.yml" in u or "/deploy" in u:
        return 1
    if "/ecosystem/" in u:
        return 2
    if "learn.chromia.com" in u:
        # prefer auth/monetize/zk
        if "monetize" in u or "/auth" in u or "authentication" in u or "zero-knowledge" in u:
            return 3
        return 4
    return 5

holes.sort(key=lambda r: (prio(r["url"]), r["url"]))
out = {
    "pairs": len(pairs),
    "learn_pairs": len(learn_pairs),
    "docs_pairs": len(docs_pairs),
    "docs_locs": len(docs_locs),
    "learn_locs": len(learn_locs),
    "holes": holes,
    "hole_count": len(holes),
}
Path("/tmp/leftover-index-holes-now.json").write_text(json.dumps(out, indent=2))
print("--- HOLES ---")
for h in holes:
    print(f"{h['url']} | HEAD {h['head']} | {h.get('slash_url')} GET {h.get('get')} | {h.get('h1')}")
print("--- learn pairs sample ---")
for x in learn_pairs[:15]:
    print(x[0], x[1], x[4])
print("DONE")
