from pathlib import Path
import re
import importlib.util

spec = importlib.util.spec_from_file_location('gen', '/workspace/chromia-knowledge/raw/gen_study.py')
gen = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gen)

def first_paras(text, n=3):
    lines = []
    for ln in text.splitlines():
        s = ln.strip()
        if not s:
            if lines: lines.append('')
            continue
        if s.startswith('- [') or s.startswith('URL:') or s.startswith('# COURSE'):
            continue
        if s.startswith('#'):
            continue
        if 'On this page' in s or s == 'Home':
            continue
        lines.append(s)
    blob = '\n'.join(lines)
    parts = [x.strip() for x in blob.split('\n\n') if x.strip()]
    return parts[:n]

def bullets_from(text):
    keys = []
    for ln in text.splitlines():
        s = ln.strip()
        if s.startswith('- ') and len(s) > 8:
            keys.append(s[2:])
    out=[]; seen=set()
    for k in keys:
        if k in seen: continue
        seen.add(k); out.append(k)
    return out[:18]

KW = ('require','auth','admin','flag','module','chromia.yml','ft4','iccf','icmf','vector','nullifier','proof','session','sign','key','rate','paginat','batch','deploy','brid','wallet','metamask','unsafe','open strategy','fee','subscription','lock','mint','transfer','query','operation','entity','test','never','production','spam','container','dimension','context','plonk','commitment','topic','send_message')

header = Path('/workspace/chromia-knowledge/study-learn.md').read_text()
if '**Blocked:**' in header:
    header = header.split('**Blocked:** none.')[0] + '**Blocked:** none.\n'
parts = [header]
for i,(slug,title,level,url,repo) in enumerate(gen.COURSES,1):
    rows = gen.lessons(slug)
    alltext = '\n'.join(t for _,_,t in rows)
    links = gen.official_links(alltext)
    if repo and repo not in links:
        links = [repo] + links
    parts.append('\n---\n')
    parts.append(f'## {i}. {title} ({level})\n')
    parts.append(f'**URL:** {url}\n')
    if repo:
        parts.append(f'**Repo:** {repo}\n')
    if links:
        parts.append('\n**Official / repo links:**\n')
        for l in links:
            parts.append(f'- {l}\n')
    parts.append(f'\n**Lessons fetched:** {len(rows)}/{len(rows)} (public, not login-gated)\n')
    parts.append('\n### Syllabus\n')
    for title_l, u, _ in rows:
        parts.append(f'- [{title_l}]({u})\n')
    intro_text = ''
    for t,u,txt in rows:
        if '/introduction' in u:
            intro_text = txt
            break
    if not intro_text and rows:
        intro_text = rows[0][2]
    parts.append('\n### What it teaches\n\n')
    for para in first_paras(intro_text, 3):
        parts.append(para + '\n\n')
    parts.append('### Production-relevant patterns (from lesson bodies)\n\n')
    collected = []
    for t,u,txt in rows:
        if '/setup' in u and 'module' not in u and 'application' not in u:
            continue
        for b in bullets_from(txt):
            bl = b.lower()
            if any(k in bl for k in KW):
                collected.append(b)
    seen=set(); uniq=[]
    for b in collected:
        if b in seen: continue
        seen.add(b); uniq.append(b)
    for b in uniq[:24]:
        parts.append(f'- {b}\n')
    if not uniq:
        parts.append('- See syllabus lessons; course is mostly conceptual or setup.\n')
    parts.append('\n')
parts.append('\n---\n\n## Cross-cutting production checklist\n\n')
parts.append('1. Validate in Rell with require / require_not_exists. Pair with run_must_fail tests.\n')
parts.append('2. Authenticate with op_context.is_signer or auth.authenticate plus handler flags (A admin, T transfer, S/MySession).\n')
parts.append('3. Treat Unsafe mint/burn/transfer and open registration as dangerous defaults. Admin-gate mint/burn. Use transfer-fee or subscription before mainnet.\n')
parts.append('4. Pin FT4 and ICCF by RID with insecure: false.\n')
parts.append('5. Rate-limit FT4 accounts (points_at_account_creation, recovery_time, max_points).\n')
parts.append('6. Timestamps from op_context.last_block_time.\n')
parts.append('7. Deterministic IDs: tx_rid for mints; (name + blockchain_rid).hash() for pool/asset IDs.\n')
parts.append('8. Money/audit tables use @log and store IDs not live entity refs.\n')
parts.append('9. ICCF for user-presented proofs (bind BRID, make_transaction_unique). ICMF for automatic L_ scoped events.\n')
parts.append('10. Clients: directory node pool plus blockchain RID. FT4 session for wallet UX. Never embed production private keys.\n')
parts.append('11. Extensions need GTX modules: Vector DB, ICCF, ICMF sender/receiver, ZKP/PLONK keys.\n')
parts.append('12. Hosting is a leased container. Unique testnet chain names. Persist issued chain RID.\n')
parts.append('\n## Also on Learn, not on the homepage list\n\n')
parts.append('Sitemap/nav also expose short guides and extra courses (not requested): continuous-integration, rell-integration-test, latest-known-time, associate-function, random-number-generation, chromia-comparisons, chromia-for-evm-developers, relationships-course, rell-masterclass, tic-tac-toe.\n')
out = Path('/workspace/chromia-knowledge/study-learn.md')
out.write_text(''.join(parts))
print('wrote', out, out.stat().st_size)
