// Synthetic agent: a deterministic stand-in for an LLM agent that builds a
// Chromia dapp using ONLY the MCP tools and their outputs. It validates the
// agent contract end-to-end: discovery -> docs -> scaffold -> break the code ->
// read diagnostics -> repair FROM the diagnostics -> security -> tests ->
// validate -> deploy config. If any tool output lacks what an agent needs to
// act next, this script fails.
//   node scripts/synthetic-agent.mjs http://127.0.0.1:3001
const BASE = process.argv[2] || 'http://127.0.0.1:3001';
console.log('SYNTHETIC AGENT TARGET:', BASE);

// --- minimal MCP-over-SSE client (mirrors e2e-sweep) ---
let session = null; let nextId = 1;
async function openSession() {
  if (session) { try { session.controller.abort(); } catch {} }
  const controller = new AbortController();
  const s = { controller, pending: new Map(), msgUrl: null };
  const res = await fetch(`${BASE}/`, { headers: { accept: 'text/event-stream' }, signal: controller.signal });
  const reader = res.body.getReader(); const decoder = new TextDecoder();
  let buf = '', endpoint = null;
  function onChunk(c) {
    buf += c.replace(/\r\n/g, '\n'); let i;
    while ((i = buf.indexOf('\n\n')) >= 0) {
      const block = buf.slice(0, i); buf = buf.slice(i + 2);
      const ev = /^event:\s*(.+)$/m.exec(block)?.[1]?.trim();
      const data = [...block.matchAll(/^data:\s*(.*)$/gm)].map(m => m[1]).join('\n');
      if (ev === 'endpoint') endpoint = data.trim();
      else if (data) { try { const m = JSON.parse(data); if (m.id !== undefined && s.pending.has(m.id)) { s.pending.get(m.id)(m); s.pending.delete(m.id); } } catch {} }
    }
  }
  (async () => { try { while (true) { const { done, value } = await reader.read(); if (done) break; onChunk(decoder.decode(value, { stream: true })); } } catch {} })();
  await new Promise((res2, rej) => { const t0 = Date.now(); const iv = setInterval(() => { if (endpoint) { clearInterval(iv); res2(); } else if (Date.now() - t0 > 20000) { clearInterval(iv); rej(new Error('no endpoint')); } }, 100); });
  s.msgUrl = endpoint.startsWith('?') ? `${BASE}/${endpoint}` : BASE + endpoint;
  session = s;
  await rpc('initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'synthetic-agent', version: '1' } });
  await fetch(s.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) });
}
async function rpc(method, params, t = 240000) {
  const s = session; const id = nextId++;
  const p = new Promise((res, rej) => { const h = setTimeout(() => { s.pending.delete(id); rej(new Error('timeout')); }, t); s.pending.set(id, m => { clearTimeout(h); res(m); }); });
  p.catch(() => {});
  await fetch(s.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', id, method, params }) });
  return p;
}
const call = (n, a, t) => rpc('tools/call', { name: n, arguments: a }, t);
const text = m => m?.result?.content?.[0]?.text ?? JSON.stringify(m?.error ?? m?.result ?? m);
const parse = m => { try { return JSON.parse(text(m)); } catch { return {}; } };

const steps = [];
function step(label, ok, detail) { steps.push([label, ok]); console.log(`${ok ? 'PASS' : 'FAIL'} agent: ${label} ${detail ?? ''}`); if (!ok) throw new Error(`agent blocked at: ${label} - ${detail}`); }

try {
  await openSession();

  // 1. Discovery: what can I do here?
  const tl = await rpc('tools/list', {});
  const tools = (tl.result?.tools || []).map(t => t.name);
  step('discovers tools', tools.includes('scaffold_dapp') && tools.includes('rell_check'), `${tools.length} tools`);

  // 2. Learn: ask the docs how FT4 accounts work.
  const hits = parse(await call('search', { query: 'how to register an FT4 account' }, 180000));
  step('doc search returns actionable hits', (hits.results || []).length > 0 && hits.results[0].id, `${(hits.results || []).length} hits`);
  const doc = parse(await call('fetch', { id: hits.results[0].id }));
  step('doc fetch returns readable text', (doc.text || '').length > 100, null);

  // 3. Scaffold a starting point.
  const scaffold = parse(await call('scaffold_dapp', { name: 'agent_dapp' }));
  const mainRell = scaffold.files?.['src/main.rell'];
  step('scaffold provides source files', !!mainRell && !!scaffold.files['src/test/main_test.rell'], null);

  // 4. The agent writes buggy code (typo'd symbol), compiles, and must be able
  //    to LOCATE the bug purely from the diagnostics.
  const buggy = mainRell.replace('my_name.name = name;', 'my_name.nmae = name;');
  const diag = parse(await call('rell_check', { source: buggy }, 180000));
  step('compiler pinpoints the planted bug', diag.ok === false && diag.errors?.[0]?.line > 0 && /nmae|unknown/i.test(diag.errors[0].text), JSON.stringify(diag.errors?.[0] ?? {}).slice(0, 120));

  // 5. Repair guided by the diagnostic (line number leads to the typo) and re-verify.
  const lines = buggy.split('\n');
  const badLineIdx = diag.errors[0].line - 1;
  lines[badLineIdx] = lines[badLineIdx].replace('nmae', 'name');
  const repaired = lines.join('\n');
  const recheck = parse(await call('rell_check', { source: repaired }, 180000));
  step('repair from diagnostics compiles', recheck.ok === true, JSON.stringify(recheck.errors ?? '').slice(0, 100));

  // 6. Security gate.
  const sec = parse(await call('rell_security_check', { source: repaired }, 180000));
  step('security gate passes', sec.ok === true, JSON.stringify(sec.findings ?? '').slice(0, 100));

  // 7. Behavior gate (db-limited failures acceptable without PostgreSQL).
  const tests = parse(await call('run_rell_tests', { files: { 'main.rell': repaired, 'test/main_test.rell': scaffold.files['src/test/main_test.rell'] } }, 240000));
  const logicFailures = (tests.cases || []).filter(c => !c.ok && !c.dbRequired);
  step('behavior gate passes', (tests.cases || []).length >= 1 && logicFailures.length === 0, tests.notes?.slice(0, 100));

  // 8. Ship-prep: validate config and get the deployment block.
  const val1 = parse(await call('validate_chromia_yml', { yaml: scaffold.files['chromia.yml'] }));
  step('config validates', val1.ok === true, JSON.stringify(val1.errors ?? '').slice(0, 100));
  const dep = parse(await call('write_deployment_config', { network: 'testnet', name: 'agent_dapp' }));
  step('deployment config produced', (dep.yaml ?? dep.chromia_yml ?? '').includes('testnet'), null);

  console.log(`\n=== SYNTHETIC AGENT JOURNEY: ${steps.length}/${steps.length} steps completed ===`);
  try { session.controller.abort(); } catch {}
  process.exit(0);
} catch (e) {
  console.log(`\n=== SYNTHETIC AGENT BLOCKED: ${e.message} (${steps.filter(s => s[1]).length}/${steps.length || 10} steps) ===`);
  try { session?.controller.abort(); } catch {}
  process.exit(1);
}
