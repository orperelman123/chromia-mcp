// End-to-end sweep of a running chromia-mcp SSE server: every tool category,
// error paths, and the full agent journey (scaffold -> compile -> security ->
// tests -> validate -> deploy config -> preflight -> verify -> local chain).
// Tools disabled on the target deployment (CHROMIA_MCP_DISABLE_TOOLS) are
// reported as SKIP, not FAIL; so are checks whose prerequisites are absent on
// the target (no PostgreSQL behind the server, or a non-loopback target for
// the local-chain REST probes).
//
// Live-network checks (explorer, chain nodes, docs site) can additionally
// degrade to WARN-UPSTREAM instead of FAIL when the failure is demonstrably
// the third party's - see upstream-classifier.mjs for the contract and the
// signature allowlist, and the guardrails near the summary at the bottom
// (all-warn is a FAIL; more than SWEEP_MAX_UPSTREAM_WARNS warnings is a FAIL;
// non-network checks never warn).
//   node scripts/e2e-sweep.mjs                          (a local server on 127.0.0.1:3001, SSE transport)
//   node scripts/e2e-sweep.mjs http://127.0.0.1:3010
//   node scripts/e2e-sweep.mjs http://127.0.0.1:3010 --transport http   (Streamable HTTP at /mcp)
// The hosted Render target this used to default to was retired 2026-09-05.
//
// --transport picks the wire, not the checks: every check below runs unchanged
// over whichever transport is selected, because both must serve the identical
// tool surface from the same process. Run it twice (sse, then http) to prove
// they do - CI does exactly that.
import { upstreamSignature, UpstreamError, probeExplorerCanary, registerUpstreamSignature } from './upstream-classifier.mjs';
const argv = process.argv.slice(2);
const transportArg = (() => {
  const i = argv.findIndex(a => a === '--transport' || a.startsWith('--transport='));
  if (i < 0) return 'sse';
  const v = argv[i].includes('=') ? argv[i].split('=')[1] : argv[i + 1];
  if (!['sse', 'http'].includes(v)) { console.error(`unknown --transport ${v} (expected sse or http)`); process.exit(2); }
  argv.splice(i, argv[i].includes('=') ? 1 : 2);
  return v;
})();
const TRANSPORT = transportArg;
const BASE = argv[0] || 'http://127.0.0.1:3001';
/** Streamable HTTP endpoint; the SSE transport uses BASE itself. */
const MCP_URL = `${BASE.replace(/\/$/, '')}/mcp`;
console.log('TARGET:', BASE, `(transport: ${TRANSPORT}${TRANSPORT === 'http' ? ` -> ${MCP_URL}` : ''})`);

let nextId = 1;

// --- Reconnectable MCP session (SSE or Streamable HTTP). A dropped stream (server hiccup,
// flaky third-party hop) must cost at most one retried check, never cascade
// timeouts through the rest of the sweep. ---
let session = null;
async function openSession() {
  return TRANSPORT === 'http' ? openStreamableHttpSession() : openSseSession();
}

/**
 * Streamable HTTP session: one POST /mcp mints an Mcp-Session-Id and every later
 * POST echoes it. The server runs with enableJsonResponse, so a request/response
 * pair comes back as a plain JSON body - no stream to hold open, which is why
 * this session has nothing to abort and no endpoint event to wait for.
 */
async function openStreamableHttpSession() {
  const s = { controller: { abort() {} }, pending: new Map(), sessionId: null, msgUrl: MCP_URL };
  session = s;
  const init = await httpRpc(s, 'initialize', {
    protocolVersion: '2025-06-18',
    capabilities: {},
    clientInfo: { name: 'e2e-sweep', version: '1' },
  });
  if (!s.sessionId) throw new Error('no mcp-session-id on the initialize response');
  if (init?.error) throw new Error(`initialize failed: ${JSON.stringify(init.error)}`);
  await httpSend(s, { jsonrpc: '2.0', method: 'notifications/initialized', params: {} });
}

async function httpSend(s, body, t = 90000) {
  const headers = {
    'content-type': 'application/json',
    // The transport rejects a POST that does not accept BOTH.
    accept: 'application/json, text/event-stream',
  };
  if (s.sessionId) headers['mcp-session-id'] = s.sessionId;
  // One immediate retry on a CONNECTION-level failure, and only that.
  //
  // SSE keeps one long-lived connection, so the rest of the sweep only meets
  // this on a genuine drop and check() reconnects. Streamable HTTP makes a
  // request per call, so it also meets the ordinary keep-alive race: the pool
  // hands out a socket the server has already closed and the send fails before
  // it is written. That is not the server answering wrongly - nothing was
  // answered - and it hit the coverage gate, which (unlike every other check)
  // treats a thrown error as an immediate FAIL with no retry (2026-09-06: 15
  // help tools in a row, while the server log showed the first of them served
  // ok=true). A second attempt opens a fresh socket. An HTTP status, a
  // JSON-RPC error and a timeout are all untouched and still propagate.
  let res;
  for (let attempt = 0; ; attempt++) {
    try {
      res = await fetch(MCP_URL, { method: 'POST', headers, body: JSON.stringify(body), signal: AbortSignal.timeout(t) });
      break;
    } catch (e) {
      if (e.name === 'TimeoutError') throw new Error('timeout');
      // Match the SSE path's retryable vocabulary so check() reconnects and retries.
      if (attempt >= 1) throw new Error(`fetch failed: ${e.message}`);
    }
  }
  const minted = res.headers.get('mcp-session-id');
  if (minted) s.sessionId = minted;
  if (res.status === 202) return null; // notification acknowledged
  const text = await res.text();
  if (!res.ok) throw new Error(`HTTP ${res.status}: ${text.slice(0, 200)}`);
  if (!text) return null;
  // enableJsonResponse gives us JSON; tolerate an SSE framing anyway.
  const payload = (res.headers.get('content-type') || '').includes('text/event-stream')
    ? text.split(/\r?\n/).filter(l => l.startsWith('data:')).map(l => l.slice(5).trim()).join('')
    : text;
  const parsed = JSON.parse(payload);
  return Array.isArray(parsed) ? parsed[0] : parsed;
}

async function httpRpc(s, method, params, t = 90000) {
  return httpSend(s, { jsonrpc: '2.0', id: nextId++, method, params }, t);
}

async function openSseSession() {
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

async function rpc(method, params, t = 90000) {
  const s = session;
  if (TRANSPORT === 'http') return httpRpc(s, method, params, t);
  const id = nextId++;
  const p = new Promise((res, rej) => { const h = setTimeout(() => { s.pending.delete(id); rej(new Error('timeout')); }, t); s.pending.set(id, m => { clearTimeout(h); res(m); }); });
  p.catch(() => {}); // avoid unhandled rejection if the timeout fires while the POST below is in flight
  await fetch(s.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', id, method, params }) });
  return p;
}
const calledTools = new Set();
const call = (name, args, t) => { calledTools.add(name); return rpc('tools/call', { name, arguments: args }, t); };
const text = m => m?.result?.content?.[0]?.text ?? JSON.stringify(m?.error ?? m?.result ?? m);
/**
 * For LIVE checks only: same as text(), but a clean tool-level error (our
 * server answered; result.isError) whose text matches the upstream allowlist
 * throws UpstreamError, which check() tags WARN-UPSTREAM instead of FAIL.
 * Structural by construction: never fires on transport errors, rpc timeouts,
 * malformed responses, or non-error results - those keep failing hard.
 */
function liveText(m) {
  const t = text(m);
  if (m?.result?.isError === true) {
    const sig = upstreamSignature(t);
    if (sig) throw new UpstreamError(sig, t);
  }
  return t;
}

const results = [];
/** [{label, signature}] - checks degraded by a third-party failure. */
const upstreamWarns = [];
/**
 * signature -> how many checks in THIS run it has already degraded. Once a
 * signature has degraded KNOWN_DEGRADED_AFTER checks, the dependency behind
 * it is known-degraded for the rest of the run: later checks hitting the SAME
 * signature stop paying full retry backoffs and fail fast to WARN-UPSTREAM
 * (the 2026-09-01 explorer incident cost 9 tools x 2 x 8s of pure waiting in
 * the coverage phase). Failures with a signature not yet at the threshold
 * still get the full retries - isolated blips keep their chance to recover.
 * Classification is unchanged; only the retry SPEND adapts, so the guardrails
 * below (all-live-warn => FAIL, > max warns => FAIL) see the same warnings.
 */
const upstreamSignatureCounts = new Map();
const KNOWN_DEGRADED_AFTER = 2;
const noteUpstreamSignature = sig =>
  upstreamSignatureCounts.set(sig, (upstreamSignatureCounts.get(sig) ?? 0) + 1);
const isKnownDegraded = sig => (upstreamSignatureCounts.get(sig) ?? 0) >= KNOWN_DEGRADED_AFTER;
/** Live-network checks that actually executed (not SKIPped) - guardrail base. */
let liveChecksExecuted = 0;
const RETRYABLE = /timeout|fetch failed|no endpoint event|ECONNRESET|socket/i;
/** Thrown by a check to degrade to SKIP (prerequisite absent, never a failure). */
class Skip extends Error {}
/**
 * opts.live marks a check that exercises a third-party dependency (explorer,
 * chain nodes, docs site). ONLY live checks may degrade to WARN-UPSTREAM, and
 * only via UpstreamError (thrown by liveText() on an allowlisted clean tool
 * error, or directly by check code when a clean tool ANSWER demonstrates an
 * upstream condition). An UpstreamError escaping a non-live check is a plain
 * FAIL - the classification cannot leak into local checks.
 */
async function check(label, fn, requiresTool, opts = {}) {
  if (requiresTool && toolNames.length && !toolNames.includes(requiresTool)) {
    results.push([label, true, 'SKIP (tool disabled on this deployment)']);
    console.log(`SKIP ${label} (tool ${requiresTool} disabled on this deployment)`);
    return;
  }
  if (opts.live) liveChecksExecuted++;
  const settle = (e) => { // terminal non-pass outcomes shared by both attempts
    if (e instanceof Skip) {
      if (opts.live) liveChecksExecuted--; // did not exercise the network
      results.push([label, true, `SKIP (${e.message})`]);
      console.log(`SKIP ${label} (${e.message})`);
      return true;
    }
    if (opts.live && e instanceof UpstreamError) {
      upstreamWarns.push({ label, signature: e.signature });
      noteUpstreamSignature(e.signature);
      results.push([label, true, `WARN-UPSTREAM (${e.message})`]);
      console.log(`WARN-UPSTREAM ${label} - the failure is the upstream dependency's, not ours (${e.message})`);
      return true;
    }
    return false;
  };
  try {
    const detail = await fn();
    results.push([label, true, detail]); console.log(`PASS ${label} ${detail ?? ''}`);
    return;
  } catch (e) {
    if (settle(e)) return;
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
    if (settle(e)) return;
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
// Explorer canary (informational, not a check): ask the explorer itself for
// `{ __typename }` before the explorer-backed tools run. If it refuses the
// smallest valid document, an "HTTP 400: Bad Request" from those tools in this
// run is the explorer's and is classified WARN-UPSTREAM; without that evidence
// a 400 stays what it usually is - OUR malformed query - and FAILs. The
// guardrails below are untouched: an explorer-wide outage still exceeds the
// warning ceiling and exits 3.
{
  const canary = await probeExplorerCanary('mainnet');
  if (canary.ok) console.log('INFO explorer canary: `{ __typename }` answered (HTTP 200)');
  else {
    registerUpstreamSignature('explorer-rejects-valid-graphql', /\bHTTP 400\b/);
    console.log(`INFO explorer canary: explorer refused \`{ __typename }\` (HTTP ${canary.status}: ${canary.body.slice(0, 80)}) - ` +
      'explorer 400s in this run are classified upstream');
  }
}
await check('get_network_stats', async () => {
  const t = liveText(await call('get_network_stats', {}));
  expect(t.includes('countAllAccounts'), t.slice(0, 80)); return null;
}, null, { live: true });
let chrId = null;
await check('get_all_assets', async () => {
  const j = JSON.parse(liveText(await call('get_all_assets', {})));
  const assets = j.data?.allAssets || [];
  chrId = assets.find(a => a.symbol === 'CHR')?.id;
  expect(assets.length > 20 && chrId, 'assets/CHR missing'); return `${assets.length} assets`;
}, null, { live: true });
await check('top_holders filtered', async () => {
  if (!chrId) throw new Skip('no CHR asset id - get_all_assets did not answer (see its own tag)');
  const t = liveText(await call('get_asset_top_holders', { assetId: chrId, limit: 2, accountTypes: ['FT4_USER'], excludeAccounts: ['3008BC6FB654A749FC2F903772545B939A9B5D8047EA2437B8675952BDD6EFD0'] }));
  expect(t.includes('accountId') && !t.includes('3008BC6F'), t.slice(0, 100)); return null;
}, null, { live: true });
await check('transactions_by_cluster', async () => {
  const t = liveText(await call('get_transactions_by_cluster', {}));
  expect(t.includes('groupedTransactionsByCluster'), t.slice(0, 80)); return null;
}, null, { live: true });
await check('all_transactions filtered', async () => {
  const t = liveText(await call('get_all_transactions', { limit: 2, blockchainIds: ['F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2'] }));
  expect(t.includes('"transactions"'), t.slice(0, 120)); return null;
}, null, { live: true });
await check('filter_blockchains', async () => {
  const t = liveText(await call('filter_blockchains', { name: 'alice' }));
  expect(t.toLowerCase().includes('alice'), t.slice(0, 100)); return null;
}, null, { live: true });
await check('blockchain_details', async () => {
  const t = liveText(await call('get_blockchain_details', { rid: 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2' }));
  expect(t.includes('my_neighbor_alice'), t.slice(0, 100)); return null;
}, null, { live: true });
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
  const t = liveText(await call('fetch_docs', { query: 'what is ICCF cross-chain proof' }, 120000));
  expect(t.length > 100, 'no content'); return null;
}, null, { live: true });
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
  const m = await call('chromia_dapp_query', { blockchainRid: 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2', query: 'rell.get_app_structure' }, 240000);
  // The tool is deadline-bounded now (default 20s, CHROMIA_MCP_QUERY_DEADLINE_MS):
  // a clean tool error naming its own deadline, for a chain we KNOW is live,
  // means the mainnet nodes produced no answer in time - upstream, like the
  // equivalent verify_deployment case below. Structural: fires only on a
  // clean isError answer carrying the tool's own deadline text, never on
  // transport errors or rpc timeouts (those keep failing hard - pre-deadline
  // this exact check hung past its 240s rpc timeout in CI run 33601190754).
  if (m?.result?.isError === true && /within the overall \d+ms\s+deadline/.test(text(m))) {
    throw new UpstreamError('mainnet-node-latency', text(m).slice(0, 160));
  }
  const t = liveText(m);
  expect(t.length > 100 && (t.includes('module') || t.includes('queries')), t.slice(0, 120)); return null;
}, 'chromia_dapp_query', { live: true });
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
// --- The five newest tools: real behavior, not clean refusals ----------------

await check('translate_error: real errors map to the right rules', async () => {
  // Both inputs are REAL error texts produced by this very server in-sweep.
  const unknownToolText = text(await call('definitely_no_such_tool', {}));
  const t1 = JSON.parse(text(await call('translate_error', { error: unknownToolText })));
  expect(t1.matched === true && t1.ruleId === 'own_unknown_tool',
    `unknown-tool text "${unknownToolText.slice(0, 60)}" -> ${JSON.stringify(t1).slice(0, 120)}`);
  const missingParamText = text(await call('get_blockchain_details', {}));
  const t2 = JSON.parse(text(await call('translate_error', { error: missingParamText })));
  expect(t2.matched === true && t2.ruleId === 'own_missing_parameter',
    `missing-param text "${missingParamText.slice(0, 60)}" -> ${JSON.stringify(t2).slice(0, 120)}`);
  let extra = '';
  if (toolNames.includes('rell_check')) {
    // A real compiler diagnostic, straight from rell_check on broken code.
    const diag = JSON.parse(text(await call('rell_check', { source: 'module;\nquery broken() = nope;' }, 120000)));
    expect(diag.ok === false && diag.errors?.length > 0, 'no compiler diagnostic to translate');
    const t3 = JSON.parse(text(await call('translate_error', { error: diag.errors[0].text })));
    expect(t3.matched === true && t3.ruleId === 'rell_unknown_name',
      `compiler text "${diag.errors[0].text.slice(0, 60)}" -> ${JSON.stringify(t3).slice(0, 120)}`);
    extra = ' + rell_unknown_name';
  }
  return `own_unknown_tool + own_missing_parameter${extra}`;
}, 'translate_error');

await check('onboarding_next_step: 3-step walk to testnet', async () => {
  // Empty state: the very first step is scaffolding, an agent task.
  const s1 = JSON.parse(text(await call('onboarding_next_step', {})));
  expect(s1.stage === 'scaffold_project' && s1.nextAction?.who === 'agent',
    JSON.stringify({ stage: s1.stage, who: s1.nextAction?.who }).slice(0, 150));
  expect((s1.blockers || []).length > 0, 'empty state must list the pending human steps as blockers');
  // Built + keyed: next is the container lease - since audit F3 an AGENT step,
  // because this server automates it (provision_testnet_container leases,
  // claim_testnet_tchr funds) and must never claim a human blocker for a step
  // a shipped tool performs. The step names the tool and the one condition it
  // cannot meet (no registered funding key => dryRun).
  const built = { hasProject: true, compiles: true, securityClean: true, testsPass: true, hasTestnetKey: true };
  const s2 = JSON.parse(text(await call('onboarding_next_step', built)));
  expect(s2.stage === 'testnet_container' && s2.nextAction?.who === 'agent',
    JSON.stringify({ stage: s2.stage, who: s2.nextAction?.who }).slice(0, 150));
  expect(/provision_testnet_container/.test(s2.nextAction?.how ?? ''),
    'agent container step must name the tool that leases: ' + (s2.nextAction?.how ?? '').slice(0, 120));
  // Deployed: nothing remains; the closing action is verify_deployment.
  const s3 = JSON.parse(text(await call('onboarding_next_step', {
    ...built, hasTestnetContainer: true, hasDeploymentConfig: true, deployedTo: 'testnet',
  })));
  expect(s3.stage === 'done' && (s3.remainingSteps || []).length === 0 && (s3.blockers || []).length === 0,
    JSON.stringify({ stage: s3.stage, remaining: s3.remainingSteps }).slice(0, 150));
  expect(/verify_deployment/.test(s3.nextAction?.verify ?? ''), 'done stage must point at verify_deployment');
  return 'scaffold_project -> testnet_container (agent: provision_testnet_container) -> done';
}, 'onboarding_next_step');

const blocking = j => (j.findings || []).filter(f => f.severity === 'BLOCKER' || f.severity === 'HIGH');

await check('preflight: files alias gates flawed rell (shipped-bug regression)', async () => {
  // The bug that shipped silently: `files` (the rell_check/run_rell_tests
  // param name) was dropped, the source gate never ran, and a testnet target
  // with flawed code reported ready:true. The alias must run the gate.
  const sc = JSON.parse(text(await call('scaffold_dapp', { name: 'sweep_dapp', template: 'hello' })));
  const cfg = JSON.parse(text(await call('write_deployment_config', { network: 'testnet', name: 'sweep_dapp', yaml: sc.files['chromia.yml'] })));
  expect(typeof cfg.chromia_yml === 'string', 'write_deployment_config must merge into the yaml it was given (audit F7): ' + JSON.stringify(cfg).slice(0, 160));
  const yaml = cfg.chromia_yml.replace('<containerIID>', 'abc123containerlease');
  const insecure = 'module;\nentity vault { key owner: text; mutable amount: integer; }\n' +
    'operation transfer(owner: text, amount: integer) { update vault @ { .owner == owner } ( .amount -= amount ); }';
  const j = JSON.parse(text(await call('deployment_preflight', { yaml, target: 'testnet', files: { 'main.rell': insecure } }, 240000)));
  expect((j.findings || []).some(f => f.check === 'security' && /unauthenticated-mutation/.test(f.message)),
    'security finding missing - the aliased source gate did not run: ' + JSON.stringify(j.findings).slice(0, 200));
  // audit F10: `files` is the canonical name now and older spellings are accepted
  // silently - there is no alias note to look for; the gate running is the proof.
  expect(!(j.notes || '').includes('Source gate SKIPPED'), 'source gate reported as skipped despite the alias');
  // Broken code through the alias must flip ready to false via a source BLOCKER
  // (on a testnet target security findings are warnings by design - they only
  // block mainnet - so the ready:false regression proof uses a compile error).
  const j2 = JSON.parse(text(await call('deployment_preflight', {
    yaml, target: 'testnet', files: { 'main.rell': 'module;\nquery broken() = nope;' },
  }, 240000)));
  expect(j2.ready === false && (j2.findings || []).some(f => f.check === 'source' && f.severity === 'BLOCKER'),
    JSON.stringify({ ready: j2.ready, blockers: j2.blockers }).slice(0, 250));
  return 'alias runs the gate: security finding surfaced, broken code -> ready:false';
}, 'deployment_preflight');

await check('preflight: clean testnet config is ready with the chr command', async () => {
  const sc = JSON.parse(text(await call('scaffold_dapp', { name: 'sweep_dapp', template: 'hello' })));
  const cfg = JSON.parse(text(await call('write_deployment_config', { network: 'testnet', name: 'sweep_dapp', yaml: sc.files['chromia.yml'] })));
  const yaml = cfg.chromia_yml.replace('<containerIID>', 'abc123containerlease');
  const j = JSON.parse(text(await call('deployment_preflight', {
    yaml, network: 'testnet', files: { 'main.rell': 'module;\n\nquery hello_world() = "hello";\n' },
  }, 240000)));
  const blockers = blocking(j);
  if (!j.ready && blockers.length && blockers.every(f => f.check === 'reachability')) {
    // Structural upstream evidence: every local gate passed and ONLY the live
    // testnet height probe blocked - a clean tool answer naming the upstream
    // cause. Routed through the shared WARN-UPSTREAM machinery.
    throw new UpstreamError('testnet-reachability',
      'all local gates clean; only the live reachability probe blocked: ' +
      JSON.stringify(blockers.map(f => f.message)).slice(0, 160));
  }
  expect(j.ready === true, JSON.stringify(j.blockers ?? j).slice(0, 250));
  expect((j.nextAction || '').includes(
    'chr deployment create --settings chromia.yml --network testnet --blockchain sweep_dapp'),
  'nextAction lacks the exact chr command: ' + (j.nextAction || '').slice(0, 200));
  return null;
}, 'deployment_preflight', { live: true });

await check('verify_deployment: live mainnet chain + bogus brid', async () => {
  // The same chain the dapp_query check exercises live in this sweep.
  const ALICE = 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2';
  const live = JSON.parse(text(await call('verify_deployment', { brid: ALICE, network: 'mainnet', waitMs: 0 }, 240000)));
  if (live.live !== true && /timed out|timeout|unavailable|refused|reset|503|502/i.test(live.notes || '')) {
    // A clean tool answer whose notes name an upstream cause for a chain we
    // KNOW is live: the mainnet nodes, not our tool, failed to answer.
    throw new UpstreamError('mainnet-node-latency', live.notes);
  }
  expect(live.live === true && live.blockHeight > 0, JSON.stringify(live).slice(0, 250));
  const bogus = JSON.parse(text(await call('verify_deployment', { brid: 'AB'.repeat(32), network: 'mainnet', waitMs: 0 }, 240000)));
  expect(bogus.live === false, JSON.stringify(bogus).slice(0, 200));
  // Two correct answers exist for a bogus BRID, and which one arrives depends
  // on upstream node health (verified live 2026-09-02): a healthy node 404s an
  // unknown BRID in <1s, but postchain-client's TryNextOnError strategy only
  // surfaces that 404 after crawling EVERY pool endpoint, so on the
  // 14-endpoint mainnet pool with any degraded node the overall deadline
  // fires first and the answer is the timeout hint instead. Both hints name
  // the same not-served-by-these-nodes cause and the same fixes (re-check the
  // BRID/network, or verify via the dapp's own node URL) - accept either.
  // VerifyDeploymentToolTest.bogusBridAnswersAgreeOnTheActionableCore pins
  // that shared core so tool and sweep cannot drift apart.
  expect(/Height probe (failed|timed out)/.test(bogus.notes || '') &&
    /check the BRID|could not be reached|translate_error/.test(bogus.notes || ''),
  'bogus brid answer lacks the actionable hint: ' + (bogus.notes || '').slice(0, 250));
  return `live at height ${live.blockHeight}`;
}, 'verify_deployment', { live: true });

const BASE_IS_LOOPBACK = /^https?:\/\/(127\.0\.0\.1|localhost|\[::1\])(:|\/|$)/i.test(BASE);

await check('local_chain_up: up -> HTTP answers -> status -> down -> port closed', async () => {
  if (!BASE_IS_LOOPBACK) {
    throw new Skip('target is remote; the local-chain REST bridge binds 127.0.0.1 on the server');
  }
  const files = { 'main.rell': 'module;\n\nquery ping() = "pong";\n' };
  // A DB-less server answers with a plain-text tool error, not JSON.
  const upRaw = text(await call('local_chain_up', { files, ttlSeconds: 300 }, 300000));
  if (/No PostgreSQL configured/i.test(upRaw)) {
    throw new Skip('server has no CHROMIA_TEST_DATABASE_URL - a real local chain needs PostgreSQL');
  }
  const up = JSON.parse(upRaw);
  expect(up.ok === true && (up.status === 'started' || up.status === 'already_running') && up.brid && up.apiUrl,
    JSON.stringify(up).slice(0, 250));
  try {
    // The chain must ANSWER, not merely claim to be up: BRID over REST...
    const bridRes = await fetch(`${up.apiUrl}/brid/iid_0`);
    const bridText = (await bridRes.text()).trim();
    expect(bridRes.status === 200 && bridText.toUpperCase() === up.brid.toUpperCase(),
      `GET /brid/iid_0 -> ${bridRes.status} "${bridText.slice(0, 70)}" (expected ${up.brid})`);
    // ...and the dapp's own query over POST /query/{brid}.
    const q = await fetch(`${up.apiUrl}/query/${up.brid}`, {
      method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ type: 'ping' }),
    });
    const qBody = await q.text();
    expect(q.status === 200 && qBody.includes('pong'), `POST /query -> ${q.status} ${qBody.slice(0, 120)}`);
    const st = JSON.parse(text(await call('local_chain_up', { action: 'status' })));
    expect(st.status === 'running' && st.brid === up.brid, JSON.stringify(st).slice(0, 200));
  } finally {
    // Always bring the chain down - no orphan node may outlive the sweep.
    const down = JSON.parse(text(await call('local_chain_up', { action: 'down' })));
    expect(down.ok === true && down.status === 'stopped', JSON.stringify(down).slice(0, 200));
  }
  await new Promise(r => setTimeout(r, 750));
  let closed = false;
  try { await fetch(`${up.apiUrl}/brid/iid_0`, { signal: AbortSignal.timeout(3000) }); } catch { closed = true; }
  expect(closed, `${up.apiUrl} still answers after action=down`);
  return `chain ${up.brid.slice(0, 12)}... answered over HTTP, then port closed`;
}, 'local_chain_up');

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
  // A guard that IS load-bearing: strip the balance check and take(11) on a pot
  // of 10 goes through, so run_must_fail reports the attack landed.
  verify_guards: {
    files: {
      'main.rell': 'module;\nentity pot { key id: integer; mutable balance: integer = 0; }\noperation seed(amount: integer) { require(amount > 0, "amount must be positive"); create pot(id = 1, balance = amount); }\noperation take(amount: integer) {\n    require(amount > 0, "amount must be positive");\n    val p = pot @ { .id == 1 };\n    require(p.balance >= amount, "insufficient");\n    update p ( .balance -= amount );\n}',
      'main_test.rell': '@test module;\nimport main;\nfunction test_overdraft_must_fail() {\n    rell.test.tx().op(main.seed(10)).run();\n    rell.test.tx().op(main.take(11)).run_must_fail("insufficient");\n}',
    },
    guards: [{ guard: 'require(p.balance >= amount, "insufficient");', test: 'test_overdraft_must_fail' }],
  },
  chromia_dapp_query: { blockchainRid: 'F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2', query: 'rell.get_app_structure' },
};
// Snapshot BEFORE the check so a session-reconnect retry re-verifies the same
// target list instead of vacuously passing on an empty "uncalled" set.
const coverageTargets = toolNames.filter(n => !calledTools.has(n));
await check('coverage: every advertised tool responds', async () => {
  const failures = [];
  const degraded = []; // covered-but-degraded: clean upstream error, tool responded
  for (const name of coverageTargets) {
    let lastText = '';
    let outcome = 'fail';
    let attemptsMade = 0;
    for (let attempt = 1; attempt <= 3; attempt++) {
      attemptsMade = attempt;
      try {
        const m = await call(name, KNOWN_ARGS[name] ?? {}, 240000);
        lastText = text(m);
        // Clean refusals: our own validation guidance, or documented upstream
        // limitations (explorer requires reCAPTCHA for node-unavailability).
        const cleanRefusal = /Missing required parameter|needs|Provide|pass |No @test modules|reCAPTCHA/i.test(lastText);
        if (m?.result && (m.result.isError !== true || cleanRefusal)) { outcome = 'ok'; break; }
        // A clean tool-level error matching the upstream allowlist (explorer
        // incident, cold-cache latency, node blip): our server answered
        // correctly. An ISOLATED signature gets the full backed-off retries -
        // it often recovers within the sweep. A signature that already
        // degraded KNOWN_DEGRADED_AFTER checks this run marks its dependency
        // known-degraded: fail fast to WARN-UPSTREAM instead of paying more
        // 8s backoffs per tool (the 2026-09-01 explorer incident degraded 9
        // tools - minutes of pure waiting for a dependency already proven down).
        if (m?.result?.isError === true && upstreamSignature(lastText)) {
          outcome = 'degraded';
          if (isKnownDegraded(upstreamSignature(lastText))) break;
          if (attempt < 3) { await new Promise(r => setTimeout(r, 8000)); continue; }
          break;
        }
        outcome = 'fail';
        break;
      } catch (e) { lastText = e.message; outcome = 'fail'; break; }
    }
    if (outcome === 'ok') continue;
    if (outcome === 'degraded') {
      const sig = upstreamSignature(lastText);
      degraded.push(name);
      noteUpstreamSignature(sig);
      const spend = attemptsMade < 3
        ? `after ${attemptsMade} attempt(s) - ${sig} already degraded ${upstreamSignatureCounts.get(sig) - 1} check(s) this run, dependency known-degraded`
        : 'after 3 attempts';
      console.log(`WARN-UPSTREAM ${name}: ${sig} ${spend} (server error path is clean): ${lastText.slice(0, 100)}`);
    } else {
      failures.push(`${name}: ${lastText.slice(0, 100)}`);
    }
  }
  expect(failures.length === 0, failures.join(' | ').slice(0, 400));
  if (degraded.length) {
    // Covered-but-degraded, not a pass and not a hard fail: each such tool
    // proved it responds with a clean error, but its real behavior went
    // unverified - the whole gate is tagged WARN-UPSTREAM and counts toward
    // the guardrails below.
    throw new UpstreamError('coverage-degraded',
      `${coverageTargets.length} tool(s) exercised, ${calledTools.size} total covered; ` +
      `${degraded.length} covered-but-degraded upstream: ${degraded.join(',')}`);
  }
  return `${coverageTargets.length} additional tool(s) exercised; ${calledTools.size} total covered`;
}, null, { live: true });

// --- Guardrails: WARN-UPSTREAM must stay incapable of hiding real breakage ---
// (a) ALL live checks warning is not an incident - it is this host (or our
//     server's egress) unable to reach anything, or a systemic bug: FAIL.
// (b) More than SWEEP_MAX_UPSTREAM_WARNS warnings fails the run. Default 8:
//     the real 2026-09-01 explorer incident degraded 5 checks, so 8 tolerates
//     a whole-service outage plus headroom, while a majority of the ~12 live
//     checks degrading still reds the run.
// (c) Non-network checks can never warn - enforced structurally in check().
const MAX_UPSTREAM_WARNS = Number(process.env.SWEEP_MAX_UPSTREAM_WARNS ?? 8);
if (liveChecksExecuted > 0 && upstreamWarns.length === liveChecksExecuted) {
  results.push(['guardrail: all live-network checks degraded', false,
    `all ${liveChecksExecuted} live-network checks warned upstream - nothing was reachable, which is never just an incident`]);
  console.log(`FAIL guardrail: all ${liveChecksExecuted} live-network checks warned upstream - nothing was reachable, which is never just an incident`);
}
if (upstreamWarns.length > MAX_UPSTREAM_WARNS) {
  results.push(['guardrail: too many upstream warnings', false,
    `${upstreamWarns.length} upstream warnings exceed the SWEEP_MAX_UPSTREAM_WARNS=${MAX_UPSTREAM_WARNS} ceiling`]);
  console.log(`FAIL guardrail: ${upstreamWarns.length} upstream warnings exceed the SWEEP_MAX_UPSTREAM_WARNS=${MAX_UPSTREAM_WARNS} ceiling`);
}

const failed = results.filter(r => !r[1]);
const passCount = results.length - failed.length - upstreamWarns.length;
console.log(`\n=== PRODUCTION SWEEP: ${passCount}/${results.length} PASS, ${upstreamWarns.length} WARN-UPSTREAM, ${failed.length} FAIL ===`);
if (upstreamWarns.length) {
  console.log(`degraded by upstream problems (${upstreamWarns.length} of ${liveChecksExecuted} live checks): ` +
    upstreamWarns.map(w => `${w.label} [${w.signature}]`).join('; '));
}
failed.forEach(f => console.log('FAILED:', f[0], '-', f[2]));
try { session.controller.abort(); } catch {}
// Exit codes: 0 = pass; 1 = at least one real FAIL (possibly transient - a
// retry may help); 3 = the ONLY failures are the upstream guardrails above
// (all-live-warn / too-many-warns). 3 still reds CI, but it is deterministic
// for the duration of an upstream outage - CI uses it to skip the pointless
// full-sweep retry that made run 33601190754 outlive its job timeout.
const guardrailOnly = failed.length > 0 && failed.every(f => String(f[0]).startsWith('guardrail:'));
process.exit(failed.length ? (guardrailOnly ? 3 : 1) : 0);
