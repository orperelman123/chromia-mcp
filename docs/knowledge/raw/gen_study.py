#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path("/workspace/chromia-knowledge")
OUT = ROOT / "study-learn.md"
BY = ROOT / "raw" / "by-course"
EXT = ROOT / "raw" / "extracted"

COURSES = [
    ("vector-db-movie-demo", "Semantic search with Vector DB on Chromia", "Advanced",
     "https://learn.chromia.com/courses/vector-db-movie-demo/introduction",
     "https://bitbucket.org/chromawallet/vector-db-movie-demo/src/main/"),
    ("chat-agent-course", "Create your chat agent with Chromia", "Advanced",
     "https://learn.chromia.com/courses/chat-agent-course/introduction",
     "https://bitbucket.org/chromawallet/chat-agent-course"),
    ("chromia-goat-chat-agent", "Chat agent for native Chromia transactions with GOAT", "Advanced",
     "https://learn.chromia.com/courses/chromia-goat-chat-agent/introduction",
     "https://bitbucket.org/chromawallet/chromia-goat-demo"),
    ("zero-knowledge-proof", "Zero-Knowledge Proofs on Chromia", "Advanced",
     "https://learn.chromia.com/courses/zero-knowledge-proof/introduction",
     "https://bitbucket.org/chromawallet/zkp-demo.git"),
    ("book-review", "Build your first app with Rell on Chromia (BookView)", "Beginner",
     "https://learn.chromia.com/courses/book-review/introduction",
     "https://bitbucket.org/chromawallet/book-course"),
    ("web3-for-web2-devs", "Web3 for Web2 developers", "Beginner",
     "https://learn.chromia.com/courses/web3-for-web2-devs/introduction",
     None),
    ("big-data", "Big Data", "Intermediate",
     "https://learn.chromia.com/courses/big-data/introduction",
     "https://bitbucket.org/chromawallet/big-data-spark/src/main/"),
    ("ft4-asset", "FT4 Asset Management", "Advanced",
     "https://learn.chromia.com/courses/ft4-asset/introduction",
     "https://bitbucket.org/chromawallet/ft4-course/src/main/"),
    ("marketplace-course", "Build a decentralized marketplace using FT4", "Advanced",
     "https://learn.chromia.com/courses/marketplace-course/introduction",
     "https://bitbucket.org/chromawallet/marketplace-course"),
    ("monetize-dapp", "Monetize your dapp", "Advanced",
     "https://learn.chromia.com/courses/monetize-dapp/introduction",
     "https://bitbucket.org/chromawallet/fee-samples/src/main/"),
    ("iccf-course", "Confirm events across blockchains", "Intermediate",
     "https://learn.chromia.com/courses/iccf-course/introduction",
     "https://bitbucket.org/chromawallet/iccf-course"),
    ("icmf-course", "Build an event-driven multi-blockchain dapp", "Advanced",
     "https://learn.chromia.com/courses/icmf-course/introduction",
     "https://bitbucket.org/chromawallet/icmf-course"),
    ("my-news-feed", "Create a simple app on Chromia using Rell and React", "Beginner",
     "https://learn.chromia.com/courses/my-news-feed/introduction",
     "https://bitbucket.org/chromawallet/news-course"),
    ("ft4-demo-app", "Build an Asset Management System With React and FT4", "Intermediate",
     "https://learn.chromia.com/courses/ft4-demo-app/introduction",
     "https://bitbucket.org/chromawallet/dapp-templates/src/main/asset_management/"),
]

def lessons(slug):
    files = sorted((EXT).glob(f"courses__{slug}__*.md"))
    rows = []
    for f in files:
        text = f.read_text(errors="replace")
        m = re.search(r"^# (.+)$", text, re.M)
        title = m.group(1).strip() if m else f.stem
        path = f.stem.replace("courses__", "/courses/").replace("__", "/")
        url = "https://learn.chromia.com" + path
        if url.endswith(".rell"):
            pass
        # drop trailing md-only
        rows.append((title, url, text))
    return rows

def official_links(text):
    links = re.findall(r"https://[A-Za-z0-9._~:/?#@&=+,%-]+", text)
    keep = []
    for l in links:
        l = l.rstrip(".,);")
        if any(x in l for x in ["docs.chromia.com", "bitbucket.org/chromawallet", "gitlab.com/chromaway", "github.com/goat-sdk"]):
            if l not in keep and "learn.png" not in l:
                keep.append(l)
    return keep[:20]
