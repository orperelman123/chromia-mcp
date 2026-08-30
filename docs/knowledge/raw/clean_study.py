from pathlib import Path
import re

p = Path('/workspace/chromia-knowledge/study-learn.md')
text = p.read_text()

# drop broken gitcd links
text = text.replace('- https://bitbucket.org/chromawallet/vector-db-movie-demo.gitcd\n','')
text = text.replace('- https://bitbucket.org/chromawallet/chromia-goat-demo.gitcd\n','')
text = text.replace('- https://bitbucket.org/chromawallet/zkp-demo.git</span><br></span></code></pre></div></div>\n','')

def clean_block(block):
    out=[]
    in_pat=False
    for ln in block.splitlines(True):
        if ln.startswith('### Production-relevant'):
            in_pat=True
            out.append(ln)
            continue
        if in_pat and ln.startswith('###'):
            in_pat=False
        if in_pat and ln.startswith('- '):
            b=ln[2:].strip()
            if 'On this page' in b: continue
            if b.startswith('Lesson ') or b.startswith('Module '): continue
            if b.startswith('[') and '](/courses/' in b: continue
            if b.startswith('http') and 'learn.png' in b: continue
            if len(b) < 20: continue
            out.append(ln)
            continue
        out.append(ln)
    return ''.join(out)

parts=re.split(r'(?=\n---\n## )', text)
text=''.join(clean_block(x) for x in parts)

# inject c01 curated notes after first production heading if present
c01=Path('/workspace/chromia-knowledge/raw/sections/c01.md')
if c01.exists():
    extra=c01.read_text().strip()
    # only the pattern-like lines starting with dash after Production
    marker='## 1. Semantic search'
    if marker in text and extra:
        # append curated file before next --- after course 1 patterns
        text=text.replace('### Production-relevant patterns (from lesson bodies)\n','### Production-relevant patterns (from lesson bodies)\n\n_Curated notes:_\n'+extra.split('### Production-relevant patterns')[-1] if '### Production' in extra else '### Production-relevant patterns (from lesson bodies)\n\n'+extra+'\n', 1)

p.write_text(text)
print('cleaned', p.stat().st_size)
