#!/usr/bin/env python3
from pathlib import Path

ROOT = Path("/workspace/chromia-mcp/app/src/main/kotlin/org/chromia/tools")

def snake(prefix: str) -> str:
    return prefix.lower()

def patch(path: Path, *, last_title_line: str, prefix: str, url: str, slash: str, title: str,
          notes_anchor: str, comment: str, redir: str):
    t = path.read_text()
    if f"const val {prefix}_INDEX_URL " in t:
        print(f"SKIP already has {prefix} in {path.name}")
        return False
    consts = (
        f"{last_title_line}\n"
        f"    const val {prefix}_INDEX_URL = \"{url}\"\n"
        f"    const val {prefix}_INDEX_URL_SLASH = \"{slash}\"\n"
        f"    const val {prefix}_INDEX_TITLE = \"{title}\"  // official H1"
    )
    if last_title_line not in t:
        raise SystemExit(f"missing title line in {path.name}: {last_title_line[:80]}")
    t = t.replace(last_title_line, consts, 1)

    note = (
        f"{notes_anchor}\n"
        f"        Leftover official leftover {comment} (leftover official ${prefix}_INDEX_URL leftover official {redir} leftover official ${prefix}_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official ${prefix}_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP."
    )
    if notes_anchor not in t:
        raise SystemExit(f"missing notes anchor in {path.name}")
    t = t.replace(notes_anchor, note, 1)

    sn = snake(prefix)
    puts = (
        f'        put("{sn}_index_url_slash", {prefix}_INDEX_URL_SLASH)\n'
        f'        put("{sn}_index_title", {prefix}_INDEX_TITLE)\n'
        f'        put("notes", notes())'
    )
    if 'put("notes", notes())' not in t:
        raise SystemExit(f"missing put notes in {path.name}")
    t = t.replace('        put("notes", notes())', puts, 1)

    end = f"// Leftover official leftover {comment} leftovers encoded as {prefix}_INDEX_* (query-only HELP ONLY WRITE SKIP)."
    if not t.rstrip().endswith(end):
        t = t.rstrip() + "\n" + end + "\n"
    path.write_text(t)
    print(f"OK {path.name} {prefix} {title}")
    return True

jobs = [
dict(
    path=ROOT / "ChrBuildHelp.kt",
    last_title_line='    const val RELL_RELEASES_INDEX_TITLE = "Rell releases"  // official H1',
    prefix="DOCS_ROOT",
    url="https://docs.chromia.com",
    slash="https://docs.chromia.com/",
    title="Chromia Docs",
    notes_anchor="        Leftover official leftover RELL rell/releases INDEX (leftover official $RELL_RELEASES_INDEX_URL leftover official 307 leftover official $RELL_RELEASES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_RELEASES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.",
    comment="DOCS docs.chromia.com root INDEX",
    redir="200",
),
dict(
    path=ROOT / "ChromiaRellLanguageHelp.kt",
    last_title_line='    const val RELL_LANGUAGE_MODULES_INDEX_TITLE = "Definitions"  // official H1',
    prefix="LEARN_TAGS_RELL",
    url="https://learn.chromia.com/tags/Rell",
    slash="https://learn.chromia.com/tags/Rell/",
    title="Courses tagged with: Rell",
    notes_anchor="        Leftover official leftover RELL rell/language-features/modules INDEX (leftover official $RELL_LANGUAGE_MODULES_INDEX_URL leftover official 307 leftover official $RELL_LANGUAGE_MODULES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_LANGUAGE_MODULES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.",
    comment="LEARN tags/Rell INDEX",
    redir="301",
),
dict(
    path=ROOT / "ChromiaFt4QueriesHelp.kt",
    last_title_line='    const val LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_TITLE = "Considerations and recommendations"',
    prefix="LEARN_TAGS_FT4",
    url="https://learn.chromia.com/tags/FT4",
    slash="https://learn.chromia.com/tags/FT4/",
    title="Courses tagged with: FT4",
    notes_anchor="        Leftover official leftover LEARN courses/ft4-asset/consideration-recomendations INDEX (leftover official $LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_URL leftover official 301 leftover official $LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_ASSET_CONSIDERATIONS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)",
    comment="LEARN tags/FT4 INDEX",
    redir="301",
),
dict(
    path=ROOT / "ChromiaLanguageClientsHelp.kt",
    last_title_line='    const val LEARN_NEWS_INTRODUCTION_INDEX_TITLE = "A simple app on Chromia is created using Rell, React, and FT4"  // official H1',
    prefix="LEARN_TAGS_REACT",
    url="https://learn.chromia.com/tags/React",
    slash="https://learn.chromia.com/tags/React/",
    title="Courses tagged with: React",
    notes_anchor="        Leftover official leftover LEARN courses/my-news-feed/introduction INDEX (leftover official $LEARN_NEWS_INTRODUCTION_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_INTRODUCTION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_INTRODUCTION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.",
    comment="LEARN tags/React INDEX",
    redir="301",
),
dict(
    path=ROOT / "ChromiaIntegrationsHelp.kt",
    last_title_line='    const val LEARN_WEB3_SECURITY_INDEX_TITLE = "Security"  // official H1',
    prefix="LEARN_TAGS_WEB3",
    url="https://learn.chromia.com/tags/Web3",
    slash="https://learn.chromia.com/tags/Web3/",
    title="Courses tagged with: Web3",
    notes_anchor="        Leftover official leftover LEARN courses/web3-for-web2-devs/security INDEX (leftover official $LEARN_WEB3_SECURITY_INDEX_URL leftover official 301 leftover official $LEARN_WEB3_SECURITY_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_WEB3_SECURITY_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.",
    comment="LEARN tags/Web3 INDEX",
    redir="301",
),
dict(
    path=ROOT / "ChrDeployHelp.kt",
    last_title_line='    const val LEARN_MARKETPLACE_SETUP_INDEX_TITLE = "Set up your project"  // official H1',
    prefix="LEARN_TAGS_CROSSCHAIN",
    url="https://learn.chromia.com/tags/Crosschain",
    slash="https://learn.chromia.com/tags/Crosschain/",
    title="Courses tagged with: Crosschain",
    notes_anchor="        Leftover official leftover LEARN courses/marketplace-course/setup INDEX (leftover official $LEARN_MARKETPLACE_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.",
    comment="LEARN tags/Crosschain INDEX",
    redir="301",
),
dict(
    path=ROOT / "ChromiaProjectStructureHelp.kt",
    last_title_line='    const val RELL_SYSTEMLIB_QUERIES_INDEX_TITLE = "System queries"  // official H1',
    prefix="LEARN_TAGS_DAPP",
    url="https://learn.chromia.com/tags/Dapp",
    slash="https://learn.chromia.com/tags/Dapp/",
    title="Courses tagged with: Dapp",
    notes_anchor="        Leftover official leftover RELL rell/language-features/systemlib/system-queries INDEX (leftover official $RELL_SYSTEMLIB_QUERIES_INDEX_URL leftover official 307 leftover official $RELL_SYSTEMLIB_QUERIES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_SYSTEMLIB_QUERIES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.",
    comment="LEARN tags/Dapp INDEX",
    redir="301",
),
dict(
    path=ROOT / "ChromiaCookbookHelp.kt",
    last_title_line='    const val LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_TITLE = "Add randomness to the card"  // official H1',
    prefix="LEARN_TAGS_DEFI",
    url="https://learn.chromia.com/tags/DeFi",
    slash="https://learn.chromia.com/tags/DeFi/",
    title="Courses tagged with: DeFi",
    notes_anchor="        Leftover official leftover LEARN courses/marketplace-course/module-nft/randomness INDEX (leftover official $LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_NFT_RANDOMNESS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.",
    comment="LEARN tags/DeFi INDEX",
    redir="301",
),
]

n = 0
for j in jobs:
    if patch(**j):
        n += 1
print(f"encoded {n}/{len(jobs)}")
