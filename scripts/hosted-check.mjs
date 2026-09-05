#!/usr/bin/env node
// Is the HOSTED server actually running what main says it runs, and answering
// from the published documentation index?
//
//   node scripts/hosted-check.mjs [--url https://chromia-mcp.onrender.com] [--expect-commit <sha>]
//
// Two checks, both read over the wire the way a client would:
//   1. /health `version` is a prefix-match of --expect-commit (the Dockerfile
//      stamps the image with the first 12 chars of RENDER_GIT_COMMIT). Without
//      --expect-commit the version is only printed.
//   2. `fetch_docs` over SSE returns the `index` object and it is not stale.
//      A server too old to send `index` at all (before 2026-09-05) fails this:
//      it is by definition running code that predates the fresh index.
//
// Why this exists: on 2026-09-05 the hosted service turned out to be on a
// 2026-09-03 commit, 47 behind main, after six rounds had each ended with
// "pushed". CI is green on the runner; nothing in it can see the hosted box.
// Exit 0 = hosted is current and fresh, 1 = drift or stale, 2 = unreachable.
const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, arr) => {
  if (a.startsWith('--')) acc.push([a.slice(2), arr[i + 1] && !arr[i + 1].startsWith('--') ? arr[i + 1] : 'true']);
  return acc;
}, []));
const BASE = (args.url ?? 'https://chromia-mcp.onrender.com').replace(/\/$/, '');
const expectCommit = args['expect-commit'];
const results = [];
const check = (label, ok, detail) => { results.push(ok); console.log(`${ok ? 'PASS' : 'FAIL'}  ${label}${detail ? ` - ${detail}` : ''}`); };

let health;
try {
  health = await (await fetch(`${BASE}/health`, { signal: AbortSignal.timeout(30_000) })).json();
} catch (e) {
  console.error(`hosted server unreachable: ${e.message}`);
  process.exit(2);
}
const version = String(health.version ?? '');
console.log(`hosted ${BASE}: ${JSON.stringify(health)}`);
if (expectCommit) {
  const want = expectCommit.slice(0, 12);
  check('hosted version is the expected commit', version.length >= 7 && want.startsWith(version), `hosted=${version} expected=${want}`);
}

// One SSE session, one fetch_docs call.
const controller = new AbortController();
const pending = new Map(); let endpoint = null;
try {
  const sseRes = await fetch(`${BASE}/`, { headers: { accept: 'text/event-stream' }, signal: controller.signal });
  const reader = sseRes.body.getReader(); const dec = new TextDecoder(); let buf = '';
  (async () => {
    try {
      while (true) {
        const { done, value } = await reader.read(); if (done) break;
        buf += dec.decode(value, { stream: true }).replace(/\r\n/g, '\n'); let i;
        while ((i = buf.indexOf('\n\n')) >= 0) {
          const b = buf.slice(0, i); buf = buf.slice(i + 2);
          const ev = /^event:\s*(.+)$/m.exec(b)?.[1]?.trim();
          const data = [...b.matchAll(/^data:\s*(.*)$/gm)].map(m => m[1]).join('\n');
          if (ev === 'endpoint') endpoint = data.trim();
          else if (data) { try { const m = JSON.parse(data); if (pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } } catch { /* noise */ } }
        }
      }
    } catch { /* aborted */ }
  })();
  await new Promise((res, rej) => { const t0 = Date.now(); const iv = setInterval(() => { if (endpoint) { clearInterval(iv); res(); } else if (Date.now() - t0 > 20_000) { clearInterval(iv); rej(new Error('no endpoint event')); } }, 100); });
  const msgUrl = endpoint.startsWith('http') ? endpoint : (endpoint.startsWith('?') ? `${BASE}/${endpoint}` : BASE + endpoint);
  let id = 1;
  const post = body => fetch(msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });
  const rpc = async (method, params, t = 120_000) => {
    const my = id++;
    const p = new Promise((res, rej) => { const h = setTimeout(() => rej(new Error(`timeout ${method}`)), t); pending.set(my, m => { clearTimeout(h); res(m); }); });
    await post({ jsonrpc: '2.0', id: my, method, params });
    return p;
  };
  await rpc('initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'hosted-check', version: '1' } });
  await post({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} });
  const t0 = Date.now();
  const m = await rpc('tools/call', { name: 'fetch_docs', arguments: { query: 'Where should require_mandatory_flags be set in an FT4 auth descriptor?' } });
  const sc = m.result?.structuredContent ?? {};
  const index = sc.index ?? null;
  console.log(`fetch_docs in ${Date.now() - t0} ms: hits=${sc.hits?.length ?? 0} index=${JSON.stringify(index)}`);
  check('fetch_docs reports the index it answered from', !!index, index ? `${index.origin}` : 'no `index` object: server predates 2026-09-05');
  if (index) {
    check('index is not stale', index.stale === false, `${index.age_days} days old, ${index.segments} segments`);
    check('index is the published GitHub release asset', /GitHub release asset/i.test(index.origin ?? ''), index.origin);
  }
  check('the round-10 probe is answered', /require_mandatory_flags/.test(JSON.stringify(sc.hits ?? [])), `top hit: ${sc.hits?.[0]?.title}`);
} catch (e) {
  console.error(`could not drive the hosted server over SSE: ${e.message}`);
  controller.abort();
  process.exit(2);
}
controller.abort();
const failed = results.filter(r => !r).length;
console.log(failed ? `\nHOSTED CHECK FAILED: ${failed} of ${results.length}` : `\nHOSTED CHECK PASSED: ${results.length}/${results.length}`);
process.exit(failed ? 1 : 0);
