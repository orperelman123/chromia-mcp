// Synthetic agent: a deterministic stand-in for an LLM agent that builds a
// Chromia dapp using ONLY the MCP tools and their outputs. It validates the
// agent contract end-to-end: discovery -> docs -> scaffold -> break the code ->
// read diagnostics -> repair FROM the diagnostics -> security -> tests ->
// validate -> deploy config -> ask onboarding what's next -> run the dapp on a
// LIVE local chain (real query over the returned REST URL) -> deployment
// preflight -> chain down. If any tool output lacks what an agent needs to act
// next, this script fails. Steps that need PostgreSQL behind the server (the
// local chain) or a loopback target skip with a stated reason.
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
let skipped = 0;
function step(label, ok, detail) { steps.push([label, ok]); console.log(`${ok ? 'PASS' : 'FAIL'} agent: ${label} ${detail ?? ''}`); if (!ok) throw new Error(`agent blocked at: ${label} - ${detail}`); }
function skip(label, reason) { skipped++; console.log(`SKIP agent: ${label} (${reason})`); }

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

  // 9. Ask the server what to do next. The agent has a compiling, secure,
  //    tested project and wants to run it: the plan must name the local chain.
  const plan = parse(await call('onboarding_next_step', {
    hasProject: true, compiles: true, securityClean: true, testsPass: true, goal: 'local',
  }));
  step('onboarding points at the local chain', plan.stage === 'local_chain' &&
    /local_chain_up|chr node start/.test(plan.nextAction?.how ?? ''),
  JSON.stringify({ stage: plan.stage, how: (plan.nextAction?.how ?? '').slice(0, 80) }));

  // 10. Follow the plan: bring the dapp up on a LIVE local chain and exercise
  //     it over the REST URL the tool returned. Needs PostgreSQL behind the
  //     server and a loopback target (the bridge binds 127.0.0.1 server-side).
  let chainWasUp = false;
  const loopback = /^https?:\/\/(127\.0\.0\.1|localhost|\[::1\])(:|\/|$)/i.test(BASE);
  if (!tools.includes('local_chain_up')) {
    skip('live local chain', 'local_chain_up is disabled on this deployment');
  } else if (!loopback) {
    skip('live local chain', 'target is remote; its local-chain REST bridge is loopback-only');
  } else {
    // A DB-less server answers with a plain-text tool error, not JSON.
    const upRaw = text(await call('local_chain_up', { files: { 'main.rell': repaired }, ttlSeconds: 300 }, 300000));
    if (/No PostgreSQL configured/i.test(upRaw)) {
      skip('live local chain', 'server has no CHROMIA_TEST_DATABASE_URL - a real chain needs PostgreSQL');
    } else {
      const up = (() => { try { return JSON.parse(upRaw); } catch { return {}; } })();
      step('local chain comes up', up.ok === true && !!up.brid && !!up.apiUrl, JSON.stringify(up).slice(0, 200));
      chainWasUp = true;
      // 11. The dapp answers a REAL query over the returned REST URL.
      const q = await fetch(`${up.apiUrl}/query/${up.brid}`, {
        method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ type: 'hello_world' }),
      });
      const qBody = await q.text();
      step('dapp answers over the live chain', q.status === 200 && qBody.includes('Hello'),
        `POST ${up.apiUrl}/query -> ${q.status} ${qBody.slice(0, 80)}`);
    }
  }

  // 12. Preflight the deployment config the server just wrote. The placeholder
  //     container MUST block (that lease is a human Vault step)...
  const pf1 = parse(await call('deployment_preflight', {
    yaml: dep.chromia_yml, target: 'testnet', rell: { 'main.rell': repaired },
  }, 240000));
  step('preflight blocks the placeholder container', pf1.ready === false &&
    (pf1.findings || []).some(f => f.check === 'container' && f.severity === 'BLOCKER'),
  JSON.stringify({ ready: pf1.ready, blockers: pf1.blockers }).slice(0, 200));
  // ...and with a real-looking lease id that blocker is gone. ready then hinges
  // only on the LIVE testnet reachability probe - upstream latency is warned,
  // never failed (same policy as the e2e sweep's live checks).
  const pf2 = parse(await call('deployment_preflight', {
    yaml: dep.chromia_yml.replace('<containerIID>', 'agentlease1234567890'),
    target: 'testnet', rell: { 'main.rell': repaired },
  }, 240000));
  const pf2Blocking = (pf2.findings || []).filter(f => f.severity === 'BLOCKER' || f.severity === 'HIGH');
  step('real lease id clears the container blocker', !pf2Blocking.some(f => f.check === 'container'),
    JSON.stringify(pf2.blockers).slice(0, 200));
  if (pf2.ready === true) {
    step('preflight hands the exact deploy command', (pf2.nextAction ?? '').includes('chr deployment create'),
      (pf2.nextAction ?? '').slice(0, 120));
  } else if (pf2Blocking.length && pf2Blocking.every(f => f.check === 'reachability')) {
    console.log('WARN agent: preflight not ready due to upstream testnet reachability only - tolerated');
  } else {
    step('preflight ready (or reachability-only)', false, JSON.stringify(pf2.blockers).slice(0, 200));
  }

  // 13. Leave nothing running.
  if (chainWasUp) {
    const down = parse(await call('local_chain_up', { action: 'down' }));
    step('local chain down', down.ok === true && down.status === 'stopped', JSON.stringify(down).slice(0, 120));
  }

  const skippedNote = skipped ? ` (${skipped} step(s) skipped with reasons above)` : '';
  console.log(`\n=== SYNTHETIC AGENT JOURNEY: ${steps.length}/${steps.length} steps completed${skippedNote} ===`);
  try { session.controller.abort(); } catch {}
  process.exit(0);
} catch (e) {
  console.log(`\n=== SYNTHETIC AGENT BLOCKED: ${e.message} (${steps.filter(s => s[1]).length}/${steps.length || 10} steps) ===`);
  try { session?.controller.abort(); } catch {}
  process.exit(1);
}
