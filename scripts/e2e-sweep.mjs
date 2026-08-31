// End-to-end sweep of a running chromia-mcp SSE server: every tool category,
// error paths, and the full agent journey (scaffold -> compile -> security ->
// tests -> validate -> deploy config). Tools disabled on the target deployment
// (CHROMIA_MCP_DISABLE_TOOLS) are reported as SKIP, not FAIL.
//   node scripts/e2e-sweep.mjs http://127.0.0.1:3001
//   node scripts/e2e-sweep.mjs https://chromia-mcp.onrender.com
const BASE = process.argv[2] || 'https://chromia-mcp.onrender.com';
console.log('TARGET:', BASE);

// --- Reconnectable MCP-over-SSE session. A dropped stream (server hiccup,
// flaky third-party hop) must cost at most one retried check, never cascade
// timeouts through the rest of the sweep. ---
let session = null;
async function openSession() {
  if (session) { try { session.controller.abort(); } catch {} }
  const controller = new AbortController();
  const s = { controller, pending: new Map(), msgUrl: null };
  const sseRes = await fetch(`${BASE}/`, { headers: { accept: 'text/event-stream' }, signal: controller.signal });
  const reader = sseRes.body.getReader();
  const decoder = new TextDecoder();
  let sseBuf = '', endpoint = null;
  function handleChunk(chunk) {
    sseBuf += chunk.replace(/\r\n/g, '\n');
    let idx;
    while ((idx = sseBuf.indexOf('\n\n')) >= 0) {
      const block = sseBuf.slice(0, idx); sseBuf = sseBuf.slice(idx + 2);
      const ev = /^event:\s*(.+)$/m.exec(block)?.[1]?.trim();
      const data = [...block.matchAll(/^data:\s*(.*)$/gm)].map(m => m[1]).join('\n');
      if (ev === 'endpoint') endpoint = data.trim();
      else if (data) { try { const m = JSON.parse(data); if (m.id !== undefined && s.pending.has(m.id)) { s.pending.get(m.id)(m); s.pending.delete(m.id); } } catch {} }
    }
  }
  (async () => { try { while (true) { const { done, value } = await reader.read(); if (done) break; handleChunk(decoder.decode(value, { stream: true })); } } catch {} })();
  await new Promise((res, rej) => { const t0 = Date.now(); const iv = setInterval(() => { if (endpoint) { clearInterval(iv); res(); } else if (Date.now() - t0 > 20000) { clearInterval(iv); rej(new Error('no endpoint event')); } }, 100); });
  s.msgUrl = endpoint.startsWith('http') ? endpoint : (endpoint.startsWith('?') ? `${BASE}/${endpoint}` : BASE + endpoint);
  session = s;
  await rpc('initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'e2e-sweep', version: '1' } });
  await fetch(s.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) });
}

let nextId = 1;
async function rpc(method, params, t = 90000) {
  const s = session;
  const id = nextId++;
  const p = new Promise((res, rej) => { const h = setTimeout(() => { s.pending.delete(id); rej(new Error('timeout')); }, t); s.pending.set(id, m => { clearTimeout(h); res(m); }); });
  p.catch(() => {}); // avoid unhandled rejection if the timeout fires while the POST below is in flight
  await fetch(s.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', id, method, params }) });
  return p;
}
const calledTools = new Set();
const call = (name, args, t) => { calledTools.add(name); return rpc('tools/call', { name, arguments: args }, t); };
const text = m => m?.result?.content?.[0]?.text ?? JSON.stringify(m?.error ?? m?.result ?? m);

const results = [];
const RETRYABLE = /timeout|fetch failed|no endpoint event|ECONNRESET|socket/i;
async function check(label, fn, requiresTool) {
  if (requiresTool && toolNames.length && !toolNames.includes(requiresTool)) {
    results.push([label, true, 'SKIP (tool disabled on this deployment)']);
    console.log(`SKIP ${label} (tool ${requiresTool} disabled on this deployment)`);
    return;
  }
  try {
    const detail = await fn();
    results.push([label, true, detail]); console.log(`PASS ${label} ${detail ?? ''}`);
    return;
  } catch (e) {
    if (!RETRYABLE.test(e.message ?? '')) {
      results.push([label, false, e.message]); console.log(`FAIL ${label} ${e.message}`);
      return;
    }
    console.log(`RETRY ${label} after transport error (${e.message}) - reconnecting session`);
  }
  try {
    await openSession();
    const detail = await fn();
    results.push([label, true, `${detail ?? ''} (after retry)`.trim()]); console.log(`PASS ${label} ${detail ?? ''} (after retry)`);
  } catch (e) {
    results.push([label, false, e.message]); console.log(`FAIL ${label} ${e.message} (after retry)`);
    try { await openSession(); } catch { /* next check will surface it */ }
  }
}
const expect = (cond, msg) => { if (!cond) throw new Error(msg); };

await openSession();

let toolNames = [];
await check('tools/list', async () => {
  const m = await rpc('tools/list', {});
  toolNames = (m.result?.tools || []).map(t => t.name);
  expect(toolNames.length >= 25, `only ${toolNames.length} tools`);
  expect(toolNames.includes('chromia_help'), 'gateway missing');
  expect(toolNames.includes('search') && toolNames.includes('fetch'), 'ChatGPT contract tools missing');
  return `${toolNames.length} tools`;
});

await check('chromia_help index', async () => {
  const t = text(await call('chromia_help', {}));
  expect(t.includes('chr_deploy_help'), 'no topics'); return null;
});
await check('chromia_help topic', async () => {
  const t = text(await call('chromia_help', { topic: 'chr_deploy' }));
  expect(t.includes('0.33'), 'no CLI payload'); return null;
});
await check('get_network_stats', async () => {
  const t = text(await call('get_network_stats', {}));
  expect(t.includes('countAllAccounts'), t.slice(0, 80)); return null;
});
let chrId = null;
await check('get_all_assets', async () => {
  const j = JSON.parse(text(await call('get_all_assets', {})));
  const assets = j.data?.allAssets || [];
  chrId = assets.find(a => a.symbol === 'CHR')?.id;
  expect(assets.length > 20 && chrId, 'assets/CHR missing'); return `${assets.length} assets`;
});
await check('top_holders filtered', async () => {
  const t = text(await call('get_asset_top_holders', { assetId: chrId, limit: 2, accountTypes: ['FT4_USER'], excludeAccounts: ['3008BC6FB654A749FC2F903772545B939A9B5D8047EA2437B8675952BDD6EFD0'] }));
  expect(t.includes('accountId') && !t.includes('3008BC6F'), t.slice(0, 100)); return null;
});
await check('transactions_by_cluster', async () => {
  const t = text(await call('get_transactions_by_cluster', {}));
  expect(t.includes('groupedTransactionsByCluster'), t.slice(0, 80)); return null;
});
await check('all_transactions filtered', async () => {
  const t = text(await call('get_all_transactions', { limit: 2, blockchainIds: ['F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2'] }));
  expect(t.includes('"transactions"'), t.slice(0, 120)); return null;
});
await check('filter_blockchains', async () => {
  const t = text(await call('filter_blockchains', { name: 'alice' }));
  expect(t.toLowerCase().includes('alice'), t.slice(0, 100)); return null;
});
await check('blockchain_details', async () => {
  const t = text(await call('get_blockchain_details', { rid: 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2' }));
  expect(t.includes('my_neighbor_alice'), t.slice(0, 100)); return null;
});
let fetchId = null;
await check('search (ChatGPT)', async () => {
  const j = JSON.parse(text(await call('search', { query: 'register an FT4 account' }, 120000)));
  fetchId = j.results?.[0]?.id;
  expect(fetchId, 'no results'); return `${j.results.length} hits`;
});
await check('fetch (ChatGPT)', async () => {
  const j = JSON.parse(text(await call('fetch', { id: fetchId })));
  expect((j.text || '').length > 50, 'no text'); return null;
});
await check('fetch_docs live+search', async () => {
  const t = text(await call('fetch_docs', { query: 'what is ICCF cross-chain proof' }, 120000));
  expect(t.length > 100, 'no content'); return null;
});
await check('rell_check valid', async () => {
  const j = JSON.parse(text(await call('rell_check', { source: 'module;\nquery ping() = "pong";' }, 120000)));
  expect(j.ok === true, JSON.stringify(j).slice(0, 120)); return null;
}, 'rell_check');
await check('rell_check broken has line', async () => {
  const j = JSON.parse(text(await call('rell_check', { source: 'module;\nquery broken() = nope;' })));
  expect(j.ok === false && j.errors?.[0]?.line === 2, JSON.stringify(j.errors).slice(0, 120)); return null;
}, 'rell_check');
await check('rell_security_check flags', async () => {
  const j = JSON.parse(text(await call('rell_security_check', { source: 'module;\nentity v { key k: text; mutable n: integer; }\noperation drain(k: text) { update v @ { .k == k } ( .n -= 1 ); }' })));
  expect(j.ok === false && JSON.stringify(j.findings).includes('unauthenticated-mutation'), JSON.stringify(j).slice(0, 150)); return null;
}, 'rell_security_check');
await check('run_rell_tests', async () => {
  const j = JSON.parse(text(await call('run_rell_tests', { files: { 'm.rell': 'module;\nfunction inc(x: integer): integer = x + 1;', 'm_test.rell': '@test module;\nimport m;\nfunction test_inc() { assert_equals(m.inc(1), 2); }' } }, 150000)));
  expect(j.ok === true && j.passed === 1, JSON.stringify(j).slice(0, 150)); return null;
}, 'run_rell_tests');
await check('scaffold ft4 + validate yml', async () => {
  const j = JSON.parse(text(await call('scaffold_dapp', { name: 'notes', template: 'ft4' })));
  expect(j.template === 'ft4' && j.files['client/example.ts'], 'ft4 files missing');
  const v = JSON.parse(text(await call('validate_chromia_yml', { yaml: j.files['chromia.yml'] })));
  expect(v.ok === true, 'scaffolded yml invalid: ' + JSON.stringify(v.errors).slice(0, 150)); return null;
});
await check('dapp_query live on-chain', async () => {
  const t = text(await call('chromia_dapp_query', { blockchainRid: 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2', query: 'rell.get_app_structure' }, 240000));
  expect(t.length > 100 && (t.includes('module') || t.includes('queries')), t.slice(0, 120)); return null;
}, 'chromia_dapp_query');
// --- The agent journey: the MCP builds a dapp through its own toolchain ---
await check('journey: scaffold compiles via rell_check', async () => {
  const scaffold = JSON.parse(text(await call('scaffold_dapp', { name: 'journey' })));
  const rell = Object.fromEntries(Object.entries(scaffold.files)
    .filter(([p]) => p.endsWith('.rell'))
    .map(([p, c]) => [p.replace(/^src\//, ''), c]));
  const j = JSON.parse(text(await call('rell_check', { files: rell }, 180000)));
  expect(j.ok === true, JSON.stringify(j.errors).slice(0, 200));
  return `${Object.keys(rell).length} files`;
}, 'rell_check');
await check('journey: scaffold passes its own tests', async () => {
  const scaffold = JSON.parse(text(await call('scaffold_dapp', { name: 'journey' })));
  const rell = Object.fromEntries(Object.entries(scaffold.files)
    .filter(([p]) => p.endsWith('.rell'))
    .map(([p, c]) => [p.replace(/^src\//, ''), c]));
  const j = JSON.parse(text(await call('run_rell_tests', { files: rell }, 180000)));
  // Without CHROMIA_TEST_DATABASE_URL on the server, db-backed scaffold tests
  // may fail with dbRequired=true - environmental, not a logic failure.
  const logicFailures = (j.cases || []).filter(c => !c.ok && !c.dbRequired);
  expect((j.cases || []).length >= 1 && logicFailures.length === 0, JSON.stringify(j).slice(0, 250));
  return j.ok ? `${j.passed} test(s) passed` : 'db-limited (no CHROMIA_TEST_DATABASE_URL)';
}, 'run_rell_tests');
await check('journey: scaffold is security-clean', async () => {
  // The ft4 template needs `chr install` to compile, so the security tool
  // (which compiles first) gets the hello template here; the ft4 template's
  // security-cleanliness is asserted by DappScaffoldFt4TemplateTest.
  const scaffold = JSON.parse(text(await call('scaffold_dapp', { name: 'journey' })));
  const j = JSON.parse(text(await call('rell_security_check', { source: scaffold.files['src/main.rell'] }, 180000)));
  expect(j.ok === true, JSON.stringify(j.findings ?? j).slice(0, 200));
  return null;
}, 'rell_security_check');

await check('get_prompts', async () => {
  const t = text(await call('get_prompts', {}));
  expect(t.includes('prompts'), t.slice(0, 80)); return null;
});
await check('write_deployment_config', async () => {
  const j = JSON.parse(text(await call('write_deployment_config', { network: 'testnet', name: 'notes' })));
  expect((j.yaml || j.chromia_yml || '').includes('testnet'), JSON.stringify(j).slice(0, 120)); return null;
});
await check('unknown tool errors cleanly', async () => {
  const m = await call('no_such_tool', {});
  const t = text(m);
  expect(t.toLowerCase().includes('unknown tool') || t.toLowerCase().includes('not found') || m.error, t.slice(0, 80)); return null;
});
await check('missing required param message', async () => {
  const t = text(await call('get_blockchain_details', {}));
  expect(t.toLowerCase().includes('rid') || t.toLowerCase().includes('missing'), t.slice(0, 100)); return null;
});

// --- MCP protocol surfaces beyond tools ---
await check('resources/list and read', async () => {
  const l = await rpc('resources/list', {});
  const resources = l.result?.resources || [];
  expect(resources.length >= 3, `only ${resources.length} resources`);
  const r = await rpc('resources/read', { uri: 'chromia://server/health' });
  const body = r.result?.contents?.[0]?.text ?? '';
  expect(body.includes('healthy'), body.slice(0, 80));
  return `${resources.length} resources`;
});

await check('chromia_help: every topic answers', async () => {
  const index = JSON.parse(text(await call('chromia_help', {})));
  const topics = index.topics || [];
  expect(topics.length >= 25, `only ${topics.length} topics`);
  const empty = [];
  for (const topic of topics) {
    const t = text(await call('chromia_help', { topic }));
    if (!t || t.length < 50 || t.includes('Unknown topic')) empty.push(topic);
  }
  expect(empty.length === 0, 'empty/unknown topics: ' + empty.join(','));
  return `${topics.length} topics`;
}, 'chromia_help');

// --- Total coverage: every advertised tool must respond to a call ---
const KNOWN_ARGS = {
  get_monthly_active_accounts_per_chain: { brid: 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2' },
  get_blockchain_analytics: { brid: 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2' },
  get_asset_distribution: { assetId: '5F16D1545A0881F971B164F1601CBBF51C29EFD0633B2730DA18C403C3B428B5' },
  get_asset_blockchains: { assetId: '5F16D1545A0881F971B164F1601CBBF51C29EFD0633B2730DA18C403C3B428B5' },
  filter_assets: { searchQuery: 'CHR' },
  get_account_blockchains: { accountId: '3008BC6FB654A749FC2F903772545B939A9B5D8047EA2437B8675952BDD6EFD0' },
  get_signer_blockchains: { signer: '03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070' },
  get_node_unavailability: { pubkey: '03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070', startTimestamp: '1690000000000' },
  ft4_module_args: { name: 'sweep' },
  rell_check: { source: 'module;\nquery ok() = 1;' },
  rell_security_check: { source: 'module;\nquery ok() = 1;' },
  run_rell_tests: { files: { 't.rell': '@test module;\nfunction test_x() { assert_equals(1, 1); }' } },
  chromia_dapp_query: { blockchainRid: 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2', query: 'rell.get_app_structure' },
};
// Snapshot BEFORE the check so a session-reconnect retry re-verifies the same
// target list instead of vacuously passing on an empty "uncalled" set.
const coverageTargets = toolNames.filter(n => !calledTools.has(n));
await check('coverage: every advertised tool responds', async () => {
  const failures = [];
  for (const name of coverageTargets) {
    try {
      const m = await call(name, KNOWN_ARGS[name] ?? {}, 240000);
      const t = text(m);
      // Clean refusals: our own validation guidance, or documented upstream
      // limitations (explorer requires reCAPTCHA for node-unavailability).
      const cleanRefusal = /Missing required parameter|needs|Provide|pass |No @test modules|reCAPTCHA/i.test(t);
      if (!m?.result) failures.push(`${name}: no result`);
      else if (m.result.isError === true && !cleanRefusal) failures.push(`${name}: ${t.slice(0, 100)}`);
    } catch (e) { failures.push(`${name}: ${e.message}`); }
  }
  expect(failures.length === 0, failures.join(' | ').slice(0, 400));
  return `${coverageTargets.length} additional tool(s) exercised; ${calledTools.size} total covered`;
});

const failed = results.filter(r => !r[1]);
console.log(`\n=== PRODUCTION SWEEP: ${results.length - failed.length}/${results.length} PASS ===`);
failed.forEach(f => console.log('FAILED:', f[0], '-', f[2]));
try { session.controller.abort(); } catch {}
process.exit(failed.length ? 1 : 0);
