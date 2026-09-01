// Fuzz marathon: many random seeds against the live MCP compiler tools, far
// beyond the CI suite's time budget. Any crash/hang/malformed result is a bug.
// Usage: node scripts/fuzz-marathon.mjs http://127.0.0.1:3001 [iterations] [seedBase]
const BASE = process.argv[2] || 'http://127.0.0.1:3001';
const ITERS = Number(process.argv[3] || 120);
const SEED_BASE = Number(process.argv[4] || 1000); // new seeds per marathon: pass a fresh base

let session = null, nextId = 1;
async function openSession() {
  if (session) { try { session.controller.abort(); } catch {} }
  const controller = new AbortController();
  const s = { controller, pending: new Map(), msgUrl: null };
  const res = await fetch(`${BASE}/`, { headers: { accept: 'text/event-stream' }, signal: controller.signal });
  const reader = res.body.getReader(); const dec = new TextDecoder();
  let buf = '', endpoint = null;
  (async () => { try { while (true) { const { done, value } = await reader.read(); if (done) break;
    buf += dec.decode(value, { stream: true }).replace(/\r\n/g, '\n'); let i;
    while ((i = buf.indexOf('\n\n')) >= 0) { const block = buf.slice(0, i); buf = buf.slice(i + 2);
      const ev = /^event:\s*(.+)$/m.exec(block)?.[1]?.trim();
      const data = [...block.matchAll(/^data:\s*(.*)$/gm)].map(m => m[1]).join('\n');
      if (ev === 'endpoint') endpoint = data.trim();
      else if (data) { try { const m = JSON.parse(data); if (m.id !== undefined && s.pending.has(m.id)) { s.pending.get(m.id)(m); s.pending.delete(m.id); } } catch {} } } } } catch {} })();
  await new Promise((r, j) => { const t0 = Date.now(); const iv = setInterval(() => { if (endpoint) { clearInterval(iv); r(); } else if (Date.now() - t0 > 20000) { clearInterval(iv); j(new Error('no endpoint')); } }, 100); });
  s.msgUrl = endpoint.startsWith('?') ? `${BASE}/${endpoint}` : BASE + endpoint;
  session = s;
  await rpc('initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'fuzz', version: '1' } });
  await fetch(s.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) });
}
async function rpc(method, params, t = 90000) {
  const s = session; const id = nextId++;
  const p = new Promise((res, rej) => { const h = setTimeout(() => { s.pending.delete(id); rej(new Error('TIMEOUT')); }, t); s.pending.set(id, m => { clearTimeout(h); res(m); }); });
  p.catch(() => {});
  await fetch(s.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', id, method, params }) });
  return p;
}
const call = (n, a, t) => rpc('tools/call', { name: n, arguments: a }, t);
const text = m => m?.result?.content?.[0]?.text ?? JSON.stringify(m?.error ?? m?.result ?? m);

// xorshift PRNG so every finding is reproducible from its seed
function prng(seed) { let x = seed >>> 0 || 1; return () => { x ^= x << 13; x >>>= 0; x ^= x >> 17; x ^= x << 5; x >>>= 0; return x / 4294967296; }; }

const FRAGS = [
  'module;\n', '@test module;\n', 'entity e { key k: text; mutable v: integer; }\n',
  'object o { mutable n: integer = 0; }\n', 'struct s { a: text; b: integer; }\n',
  'enum col { red, green }\n', 'function f(x: integer): integer = x * 2;\n',
  'operation op(t: text) { require(t != "", "e"); create e(t, 1); }\n',
  'operation noauth(t: text) { update e @* {} ( .v += 1 ); }\n',
  'query q() = e @* {} ( .k );\n', 'import lib.ft4.auth;\n', 'import lib.ft4.accounts;\n',
  'function test_x() { assert_equals(1, 1); }\n',
  '// } " \' /* update delete create\n', '/* { ( " */\n',
  'val s = "a { b ( c \\" d // e";\n', 'val h = x"' + 'ab'.repeat(32) + '";\n',
  'operation unterminated(', 'operation half() {', '}', ')', '"', "'", '/*', '﻿',
  'é世界🚀\n', 'val long = "' + 'x'.repeat(500) + '";\n',
];
function gen(rand) {
  let out = '';
  const n = 1 + Math.floor(rand() * 10);
  for (let i = 0; i < n; i++) out += FRAGS[Math.floor(rand() * FRAGS.length)];
  if (rand() < 0.3) { const cut = Math.floor(rand() * out.length); out = out.slice(0, cut); }
  if (rand() < 0.2) out = out.split('').reverse().join('');
  return out;
}

const findings = [];
await openSession();
console.log(`fuzz marathon: ${ITERS} iterations against ${BASE} (seeds ${SEED_BASE}..${SEED_BASE + ITERS - 1})`);
for (let i = 0; i < ITERS; i++) {
  const seed = SEED_BASE + i;
  const rand = prng(seed);
  const src = gen(rand);
  const tool = ['rell_check', 'rell_security_check', 'run_rell_tests'][i % 3];
  const args = tool === 'run_rell_tests' ? { files: { 'f_test.rell': src } } : { source: src };
  try {
    const m = await call(tool, args, 120000);
    const t = text(m);
    if (/Exception|StackTrace|\bat org\.chromia\b|kotlin\.\w+Exception/i.test(t)) {
      findings.push({ seed, tool, kind: 'leaked-exception', src: src.slice(0, 120), out: t.slice(0, 200) });
    } else if (!m?.result) {
      findings.push({ seed, tool, kind: 'no-result', src: src.slice(0, 120), out: t.slice(0, 200) });
    }
  } catch (e) {
    findings.push({ seed, tool, kind: e.message === 'TIMEOUT' ? 'hang' : 'transport', src: src.slice(0, 120), out: e.message });
    await openSession(); // recover and keep going
  }
  if ((i + 1) % 20 === 0) console.log(`  ...${i + 1}/${ITERS} (${findings.length} findings)`);
}
const health = await fetch(`${BASE}/health`).then(r => r.text()).catch(e => 'UNREACHABLE ' + e.message);
console.log('\nserver health after marathon:', health.replace(/\s+/g, ' '));
console.log(`\n=== FUZZ MARATHON: ${ITERS} iterations, ${findings.length} finding(s) ===`);
findings.slice(0, 10).forEach(f => console.log(`seed=${f.seed} ${f.tool} ${f.kind}\n  src: ${JSON.stringify(f.src)}\n  out: ${f.out}`));
try { session.controller.abort(); } catch {}
process.exit(findings.length ? 1 : 0);
