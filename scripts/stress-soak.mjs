// Concurrency stress + soak harness for the chromia-mcp SSE server. Launches
// its own server instances from the shadowJar and drives four phases:
//   burst   16 parallel SSE clients, mixed tool calls, ~3 min. Asserts zero
//           5xx/protocol errors, zero wrong-tool responses (nonce-checked
//           payloads per request id), /health 200 throughout; p95 per class.
//   memory  the same burst against a server capped at -Xmx768m (small
//           container). Asserts the process survives and RSS stabilizes
//           (no monotonic growth after warmup); prints the curve.
//   soak    20 min of steady 2-4 rps mixed load on the SAME capped instance.
//           Asserts no >2x latency degradation first-5-min vs last-5-min,
//           no memory creep (last 5 min within 15% of minutes 5-10), zero
//           errors, /health at the end.
//   abuse   5 simultaneous run_rell_tests calls with infinite-loop tests on a
//           server with CHROMIA_MCP_TEST_TIMEOUT_SECONDS=3. Asserts clean
//           timeout results, the leaked-runner ceiling's clear error once
//           reached, and that OTHER tools stay responsive while runners spin.
//   node scripts/stress-soak.mjs                 # all phases, full durations
//   node scripts/stress-soak.mjs --quick         # short self-test durations
//   node scripts/stress-soak.mjs --phases burst,abuse
import { spawn, execFile } from 'node:child_process';
import { mkdirSync, createWriteStream, readFileSync, existsSync } from 'node:fs';
import { promisify } from 'node:util';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const execFileP = promisify(execFile);
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const JAR = path.join(ROOT, 'app', 'build', 'libs', 'chromia-mcp-server.jar');
const LOG_DIR = path.join(ROOT, 'build', 'stress-soak-logs');
const QUICK = process.argv.includes('--quick');
const phasesArg = process.argv.find(a => a.startsWith('--phases'));
const PHASES = (phasesArg ? (phasesArg.split('=')[1] ?? process.argv[process.argv.indexOf(phasesArg) + 1]) : 'burst,memory,soak,abuse')
  .split(',').map(s => s.trim()).filter(Boolean);
const MIN = 60_000;
const DUR = QUICK
  ? { burst: 25_000, memory: 30_000, soak: 90_000 }
  : { burst: 3 * MIN, memory: 3 * MIN, soak: 20 * MIN };

if (!existsSync(JAR)) { console.error(`Missing ${JAR} - run: gradlew.bat shadowJar`); process.exit(2); }
mkdirSync(LOG_DIR, { recursive: true });
const sleep = ms => new Promise(r => setTimeout(r, ms));
const nonce = () => Math.random().toString(36).slice(2, 10);

// ---------------------------------------------------------------- server mgmt
async function startServer({ label, port, jvmArgs = [], env = {} }) {
  const log = createWriteStream(path.join(LOG_DIR, `${label}.log`));
  const proc = spawn('java', [...jvmArgs, '-jar', JAR, '--sse', '--port', String(port)], {
    cwd: ROOT, env: { ...process.env, ...env }, stdio: ['ignore', 'pipe', 'pipe'],
  });
  const srv = { label, port, proc, base: `http://127.0.0.1:${port}`, exitCode: null, logPath: path.join(LOG_DIR, `${label}.log`) };
  proc.stdout.pipe(log); proc.stderr.pipe(log);
  proc.on('exit', code => { srv.exitCode = code ?? -1; });
  const t0 = Date.now();
  while (Date.now() - t0 < 120_000) {
    if (srv.exitCode !== null) throw new Error(`${label}: server exited during startup (code ${srv.exitCode}) - see ${srv.logPath}`);
    try { const r = await fetch(`${srv.base}/health`); if (r.status === 200) return srv; } catch {}
    await sleep(400);
  }
  throw new Error(`${label}: /health never answered within 120s`);
}
function stopServer(srv) { if (srv && srv.exitCode === null) { try { srv.proc.kill(); } catch {} } }
async function sampleRss(pid) {
  try {
    const { stdout } = await execFileP('tasklist', ['/FI', `PID eq ${pid}`, '/FO', 'CSV', '/NH']);
    const m = stdout.match(/"([\d.,  ]+) K"/);
    return m ? parseInt(m[1].replace(/[^\d]/g, ''), 10) * 1024 : null;
  } catch { return null; }
}

// ---------------------------------------------------------------- MCP client
// One SSE session per client (mirrors e2e-sweep's plumbing). Every response is
// matched to its pending request id; a response for an id we never sent - or
// already timed out - is counted, never silently dropped.
async function openClient(base, stats) {
  const controller = new AbortController();
  const c = { base, controller, pending: new Map(), timedOut: new Set(), nextId: 1, msgUrl: null, stats, closed: false };
  const res = await fetch(`${base}/`, { headers: { accept: 'text/event-stream' }, signal: controller.signal });
  if (res.status !== 200) throw new Error(`SSE open: HTTP ${res.status}`);
  const reader = res.body.getReader(); const dec = new TextDecoder();
  let buf = '', endpoint = null;
  (async () => {
    try {
      while (true) {
        const { done, value } = await reader.read(); if (done) break;
        buf += dec.decode(value, { stream: true }).replace(/\r\n/g, '\n'); let i;
        while ((i = buf.indexOf('\n\n')) >= 0) {
          const block = buf.slice(0, i); buf = buf.slice(i + 2);
          const ev = /^event:\s*(.+)$/m.exec(block)?.[1]?.trim();
          const data = [...block.matchAll(/^data:\s*(.*)$/gm)].map(m => m[1]).join('\n');
          if (ev === 'endpoint') endpoint = data.trim();
          else if (data) {
            try {
              const m = JSON.parse(data);
              if (m.id === undefined) continue; // notification
              if (c.pending.has(m.id)) { c.pending.get(m.id)(m); c.pending.delete(m.id); }
              else if (c.timedOut.has(m.id)) { c.timedOut.delete(m.id); stats.late++; }
              else stats.protocolErrors.push(`unmatched response id ${m.id}`);
            } catch (e) { stats.protocolErrors.push(`SSE data not JSON: ${String(data).slice(0, 80)}`); }
          }
        }
      }
    } catch {}
    if (!c.closed) stats.sseDrops++;
  })();
  const t0 = Date.now();
  while (!endpoint) { if (Date.now() - t0 > 20_000) throw new Error('no endpoint event'); await sleep(50); }
  c.msgUrl = endpoint.startsWith('http') ? endpoint : (endpoint.startsWith('?') ? `${base}/${endpoint}` : base + endpoint);
  await rpc(c, 'initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'stress-soak', version: '1' } });
  await fetch(c.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) });
  return c;
}
function closeClient(c) { c.closed = true; try { c.controller.abort(); } catch {} }
async function rpc(c, method, params, timeoutMs = 180_000) {
  const id = c.nextId++;
  const p = new Promise((res, rej) => {
    const h = setTimeout(() => { c.pending.delete(id); c.timedOut.add(id); rej(new Error(`rpc timeout ${timeoutMs}ms`)); }, timeoutMs);
    c.pending.set(id, m => { clearTimeout(h); res(m); });
  });
  p.catch(() => {});
  const post = await fetch(c.msgUrl, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ jsonrpc: '2.0', id, method, params }) });
  if (post.status >= 500) c.stats.http5xx.push(`${method}: HTTP ${post.status}`);
  else if (post.status >= 400) c.stats.http4xx.push(`${method}: HTTP ${post.status}`);
  return p;
}
const call = (c, name, args, t) => rpc(c, 'tools/call', { name, arguments: args }, t);
const text = m => m?.result?.content?.[0]?.text ?? '';
const isErr = m => m?.error !== undefined || m?.result?.isError === true;

// ---------------------------------------------------------------- workload mix
// Every task embeds a nonce and asserts the response reflects THAT request -
// a passing call proves the response was for this tool and this payload.
function securitySource(n) {
  // ~50 functions plus operations, one deliberately unauthenticated mutation
  // named after the nonce so the finding must echo it back.
  let src = 'module;\nentity acct_' + n + ' { key name: text; mutable bal: integer; }\n';
  for (let i = 0; i < 42; i++) src += `function f${i}_${n}(x: integer): integer = x + ${i};\n`;
  for (let i = 0; i < 7; i++) src += `query q${i}_${n}() = f${i}_${n}(${i});\n`;
  src += `operation drain_${n}(who: text) { update acct_${n} @* { .name == who } ( .bal -= 1 ); }\n`;
  return src;
}
const expect = (cond, msg) => { if (!cond) throw new Error(msg); };
// A known-valid chromia.yml, taken from a real scaffold at phase init so the
// fixture always matches what the validator expects.
let VALID_YML = null;

const TASKS = {
  help_index: async c => {
    const t = text(await call(c, 'chromia_help', {}));
    expect(t.includes('chr_deploy_help'), `help index wrong payload: ${t.slice(0, 60)}`);
  },
  help_topic: async c => {
    const t = text(await call(c, 'chromia_help', { topic: 'chr_deploy' }));
    expect(t.includes('0.33'), `help topic wrong payload: ${t.slice(0, 60)}`);
  },
  rell_check_ok: async c => {
    const n = nonce();
    const j = JSON.parse(text(await call(c, 'rell_check', { source: `module;\nquery q_${n}() = "${n}";` })));
    expect(j.ok === true, `rell_check_ok not ok: ${JSON.stringify(j).slice(0, 100)}`);
  },
  rell_check_bad: async c => {
    const n = nonce();
    const j = JSON.parse(text(await call(c, 'rell_check', { source: `module;\nquery b_${n}() = missing_${n};` })));
    expect(j.ok === false && JSON.stringify(j.errors).includes(`missing_${n}`),
      `rell_check_bad did not echo nonce: ${JSON.stringify(j).slice(0, 120)}`);
  },
  security_50: async c => {
    const n = nonce();
    const j = JSON.parse(text(await call(c, 'rell_security_check', { source: securitySource(n) })));
    expect(j.ok === false && JSON.stringify(j.findings).includes(`drain_${n}`),
      `security_50 did not flag drain_${n}: ${JSON.stringify(j).slice(0, 140)}`);
  },
  validate_yml: async c => {
    const j = JSON.parse(text(await call(c, 'validate_chromia_yml', { yaml: VALID_YML })));
    expect(j.ok === true, `validate_yml not ok: ${JSON.stringify(j).slice(0, 120)}`);
  },
  scaffold: async c => {
    const n = `stress_${nonce()}`;
    const j = JSON.parse(text(await call(c, 'scaffold_dapp', { name: n })));
    expect(j.name === n && j.files && j.files['chromia.yml'] && j.files['chromia.yml'].includes(n),
      `scaffold did not echo name ${n}: ${JSON.stringify(j).slice(0, 100)}`);
  },
  run_tests: async c => {
    const n = nonce();
    const j = JSON.parse(text(await call(c, 'run_rell_tests', {
      files: {
        [`m_${n}.rell`]: `module;\nfunction inc_${n}(x: integer): integer = x + 1;`,
        [`t_${n}.rell`]: `@test module;\nimport m_${n};\nfunction test_${n}() { assert_equals(m_${n}.inc_${n}(1), 2); }`,
      },
    })));
    expect(j.ok === true && j.cases?.[0]?.name?.includes(`test_${n}`),
      `run_tests wrong payload: ${JSON.stringify(j).slice(0, 140)}`);
  },
  search: async c => {
    const j = JSON.parse(text(await call(c, 'search', { query: 'register an FT4 account' })));
    expect(Array.isArray(j.results) && j.results.length > 0, `search empty: ${JSON.stringify(j).slice(0, 100)}`);
  },
};
const MIX = [
  ['help_index', 3], ['help_topic', 3], ['rell_check_ok', 2], ['rell_check_bad', 2],
  ['security_50', 1], ['validate_yml', 2], ['scaffold', 1], ['run_tests', 1], ['search', 2],
];
function pickTask(mix) {
  const total = mix.reduce((s, [, w]) => s + w, 0);
  let r = Math.random() * total;
  for (const [name, w] of mix) { r -= w; if (r <= 0) return name; }
  return mix[0][0];
}

// ---------------------------------------------------------------- stats/report
function newStats() {
  return { records: [], http5xx: [], http4xx: [], protocolErrors: [], toolFailures: [], late: 0, sseDrops: 0, healthChecks: 0, healthFailures: [] };
}
const pct = (sorted, p) => sorted.length ? sorted[Math.min(sorted.length - 1, Math.floor(p / 100 * sorted.length))] : 0;
function latencyTable(records, sinceMs = -Infinity, untilMs = Infinity) {
  const byClass = new Map();
  for (const r of records) {
    if (r.t < sinceMs || r.t >= untilMs) continue;
    if (!byClass.has(r.task)) byClass.set(r.task, []);
    byClass.get(r.task).push(r.ms);
  }
  const rows = [];
  for (const [task, arr] of [...byClass.entries()].sort()) {
    arr.sort((a, b) => a - b);
    rows.push({ task, n: arr.length, p50: pct(arr, 50), p95: pct(arr, 95), max: arr[arr.length - 1] });
  }
  return rows;
}
function printLatency(rows, title) {
  console.log(`  ${title}`);
  console.log('    task            n     p50ms    p95ms    maxms');
  for (const r of rows) console.log(`    ${r.task.padEnd(14)}${String(r.n).padStart(6)}${String(Math.round(r.p50)).padStart(10)}${String(Math.round(r.p95)).padStart(9)}${String(Math.round(r.max)).padStart(9)}`);
}
function startHealthMonitor(srv, stats) {
  const iv = setInterval(async () => {
    stats.healthChecks++;
    try {
      const r = await fetch(`${srv.base}/health`, { signal: AbortSignal.timeout(4000) });
      if (r.status !== 200) stats.healthFailures.push(`HTTP ${r.status}`);
      else { const j = await r.json(); if (j.status !== 'healthy') stats.healthFailures.push(JSON.stringify(j).slice(0, 60)); }
    } catch (e) { stats.healthFailures.push(e.message); }
  }, 5_000);
  return () => clearInterval(iv);
}
function startRssMonitor(srv, samples) {
  const iv = setInterval(async () => {
    const rss = await sampleRss(srv.proc.pid);
    if (rss !== null) samples.push({ t: Date.now(), rss });
  }, 5_000);
  return () => clearInterval(iv);
}
function summarizeCurve(samples, t0) {
  const buckets = new Map();
  for (const s of samples) {
    const b = Math.floor((s.t - t0) / 30_000);
    if (!buckets.has(b)) buckets.set(b, []);
    buckets.get(b).push(s.rss);
  }
  return [...buckets.entries()].sort((a, b) => a[0] - b[0])
    .map(([b, arr]) => `${String(b * 30).padStart(5)}s:${Math.round(arr.reduce((x, y) => x + y, 0) / arr.length / 1048576)}MB`)
    .join(' ');
}
const avg = a => a.reduce((x, y) => x + y, 0) / (a.length || 1);

// ---------------------------------------------------------------- load runners
async function runMixedLoad({ srv, nClients, durationMs, mix, stats, paceMs = null, label }) {
  const clients = [];
  for (let i = 0; i < nClients; i++) clients.push(await openClient(srv.base, stats));
  const stopHealth = startHealthMonitor(srv, stats);
  const t0 = Date.now();
  const workers = clients.map(async c => {
    while (Date.now() - t0 < durationMs) {
      const task = pickTask(mix);
      const s = performance.now();
      try {
        await TASKS[task](c);
        stats.records.push({ task, ms: performance.now() - s, t: Date.now() });
      } catch (e) {
        stats.records.push({ task, ms: performance.now() - s, t: Date.now(), failed: true });
        stats.toolFailures.push(`${task}: ${e.message}`.slice(0, 200));
      }
      await sleep(paceMs ? paceMs() : 25 + Math.random() * 100);
    }
  });
  const progress = setInterval(() => {
    const done = stats.records.length;
    console.log(`  [${label}] ${Math.round((Date.now() - t0) / 1000)}s: ${done} calls, ${stats.toolFailures.length} failures`);
  }, QUICK ? 10_000 : 30_000);
  await Promise.all(workers);
  clearInterval(progress);
  stopHealth();
  clients.forEach(closeClient);
}

/** Per-phase init: grab the valid-yml fixture and probe search availability. */
async function initPhase(base) {
  const stats = newStats();
  const c = await openClient(base, stats);
  try {
    const j = JSON.parse(text(await call(c, 'scaffold_dapp', { name: 'fixture' })));
    VALID_YML = j.files['chromia.yml'];
    try { await TASKS.search(c); return true; }
    catch (e) { console.log(`  NOTE: search excluded from mix (environmental): ${e.message.slice(0, 120)}`); return false; }
  } finally { closeClient(c); }
}

const results = [];
function phaseResult(name, failures, detail = '') {
  const pass = failures.length === 0;
  results.push({ name, pass, failures });
  console.log(`\n=== ${name}: ${pass ? 'PASS' : 'FAIL'} ${detail}`);
  failures.forEach(f => console.log(`  FAIL ${f}`));
}
function collectCommonFailures(stats, { requireZeroToolFailures = true } = {}) {
  const f = [];
  if (stats.http5xx.length) f.push(`${stats.http5xx.length} HTTP 5xx: ${stats.http5xx.slice(0, 3).join('; ')}`);
  if (stats.http4xx.length) f.push(`${stats.http4xx.length} HTTP 4xx: ${stats.http4xx.slice(0, 3).join('; ')}`);
  if (stats.protocolErrors.length) f.push(`${stats.protocolErrors.length} protocol errors: ${stats.protocolErrors.slice(0, 3).join('; ')}`);
  if (stats.sseDrops) f.push(`${stats.sseDrops} SSE stream drop(s) mid-phase`);
  if (requireZeroToolFailures && stats.toolFailures.length) {
    const sample = [...new Set(stats.toolFailures)].slice(0, 5).join('\n      ');
    f.push(`${stats.toolFailures.length} tool call failures/wrong payloads:\n      ${sample}`);
  }
  if (stats.healthFailures.length) f.push(`${stats.healthFailures.length}/${stats.healthChecks} health checks failed: ${stats.healthFailures.slice(0, 3).join('; ')}`);
  return f;
}

// ---------------------------------------------------------------- phases
async function phaseBurst() {
  console.log(`\n### PHASE burst: 16 clients, mixed load, ${Math.round(DUR.burst / 1000)}s`);
  const srv = await startServer({ label: 'burst', port: 3101 });
  try {
    const searchOk = await initPhase(srv.base);
    const mix = searchOk ? MIX : MIX.filter(([n]) => n !== 'search');
    const stats = newStats();
    await runMixedLoad({ srv, nClients: 16, durationMs: DUR.burst, mix, stats, label: 'burst' });
    printLatency(latencyTable(stats.records), `latency (${stats.records.length} calls, ${stats.late} late-after-timeout)`);
    const failures = collectCommonFailures(stats);
    if (srv.exitCode !== null) failures.push(`server exited mid-phase (code ${srv.exitCode})`);
    phaseResult('burst', failures, `${stats.records.length} calls`);
  } finally { stopServer(srv); }
}

async function phaseMemoryAndSoak(runMemory, runSoak) {
  const srv = await startServer({ label: 'memory-soak', port: 3102, jvmArgs: ['-Xmx768m'] });
  const rssSamples = [];
  const stopRss = startRssMonitor(srv, rssSamples);
  try {
    const searchOk = await initPhase(srv.base);
    const mix = searchOk ? MIX : MIX.filter(([n]) => n !== 'search');

    if (runMemory) {
      console.log(`\n### PHASE memory: burst under -Xmx768m, ${Math.round(DUR.memory / 1000)}s`);
      const t0 = Date.now();
      const stats = newStats();
      await runMixedLoad({ srv, nClients: 16, durationMs: DUR.memory, mix, stats, label: 'memory' });
      await sleep(6000); // let one more RSS sample land
      const phaseSamples = rssSamples.filter(s => s.t >= t0);
      console.log(`  RSS curve: ${summarizeCurve(phaseSamples, t0)}`);
      printLatency(latencyTable(stats.records), `latency (${stats.records.length} calls)`);
      const failures = collectCommonFailures(stats);
      if (srv.exitCode !== null) failures.push(`server DIED under 768MB heap (code ${srv.exitCode}) - see ${srv.logPath}`);
      const oom = readFileSync(srv.logPath, 'utf8').includes('OutOfMemoryError');
      if (oom) failures.push('OutOfMemoryError in server log');
      // Monotonic growth: post-warmup quarters must not be strictly increasing
      // with >10% total growth.
      const warmup = Math.min(60_000, DUR.memory / 4);
      const post = phaseSamples.filter(s => s.t - t0 >= warmup).map(s => s.rss);
      if (post.length >= 8) {
        const q = 4, qs = [];
        for (let i = 0; i < q; i++) qs.push(avg(post.slice(Math.floor(i * post.length / q), Math.floor((i + 1) * post.length / q))));
        const monotonic = qs.every((v, i) => i === 0 || v > qs[i - 1]);
        const growth = (qs[q - 1] - qs[0]) / qs[0];
        console.log(`  post-warmup RSS quarters: ${qs.map(v => Math.round(v / 1048576) + 'MB').join(' -> ')} (growth ${(growth * 100).toFixed(1)}%)`);
        if (monotonic && growth > 0.10) failures.push(`RSS grows monotonically across the run: ${qs.map(v => Math.round(v / 1048576) + 'MB').join(' -> ')}`);
      }
      phaseResult('memory', failures, `${stats.records.length} calls`);
    }

    if (runSoak) {
      console.log(`\n### PHASE soak: 2-4 rps steady load on the same instance, ${Math.round(DUR.soak / 1000)}s`);
      const t0 = Date.now();
      const stats = newStats();
      // 4 clients, each ~0.7 rps => ~2.8 rps aggregate.
      await runMixedLoad({ srv, nClients: 4, durationMs: DUR.soak, mix, stats, paceMs: () => 1000 + Math.random() * 1000, label: 'soak' });
      const failures = collectCommonFailures(stats);
      if (srv.exitCode !== null) failures.push(`server exited during soak (code ${srv.exitCode})`);
      // Latency degradation: per class, last window p95 must be <= 2x first
      // window p95 (with a 150ms noise floor on the delta).
      const W = Math.min(5 * MIN, Math.floor(DUR.soak / 3));
      const first = latencyTable(stats.records, t0, t0 + W);
      const last = latencyTable(stats.records, t0 + DUR.soak - W, Infinity);
      printLatency(first, `first ${Math.round(W / 1000)}s`);
      printLatency(last, `last ${Math.round(W / 1000)}s`);
      for (const l of last) {
        const f = first.find(r => r.task === l.task);
        if (f && f.n >= 5 && l.n >= 5 && l.p95 > 2 * f.p95 && l.p95 - f.p95 > 150) {
          failures.push(`latency degraded >2x for ${l.task}: p95 ${Math.round(f.p95)}ms -> ${Math.round(l.p95)}ms`);
        }
      }
      // Memory creep: last-5-min avg within 15% of minutes 5-10 avg.
      const soakSamples = rssSamples.filter(s => s.t >= t0);
      console.log(`  RSS curve: ${summarizeCurve(soakSamples, t0)}`);
      const refWin = soakSamples.filter(s => s.t - t0 >= W && s.t - t0 < 2 * W).map(s => s.rss);
      const lastWin = soakSamples.filter(s => s.t - t0 >= DUR.soak - W).map(s => s.rss);
      if (refWin.length && lastWin.length) {
        const creep = (avg(lastWin) - avg(refWin)) / avg(refWin);
        console.log(`  RSS ref-window avg ${Math.round(avg(refWin) / 1048576)}MB, last-window avg ${Math.round(avg(lastWin) / 1048576)}MB (creep ${(creep * 100).toFixed(1)}%)`);
        if (creep > 0.15) failures.push(`memory creep ${(creep * 100).toFixed(1)}% (> 15%)`);
      }
      try {
        const r = await fetch(`${srv.base}/health`);
        if (r.status !== 200) failures.push(`final /health HTTP ${r.status}`);
      } catch (e) { failures.push(`final /health unreachable: ${e.message}`); }
      phaseResult('soak', failures, `${stats.records.length} calls`);
    }
  } finally { stopRss(); stopServer(srv); }
}

async function phaseAbuse() {
  console.log('\n### PHASE abuse: 5 concurrent infinite-loop run_rell_tests (3s timeout)');
  const srv = await startServer({ label: 'abuse', port: 3103, env: { CHROMIA_MCP_TEST_TIMEOUT_SECONDS: '3' } });
  const failures = [];
  const stats = newStats();
  try {
    const c0 = await openClient(srv.base, stats);
    // Warm the compiler/runner path so cold-start JIT cannot eat the 3s budget;
    // retry because the very first in-process compile can exceed 3s itself.
    let warmed = false;
    for (let i = 0; i < 4 && !warmed; i++) {
      try {
        const n = nonce();
        const j = JSON.parse(text(await call(c0, 'run_rell_tests', { files: { [`w_${n}.rell`]: `@test module;\nfunction test_${n}() { assert_equals(1, 1); }` } }, 60_000)));
        warmed = j.ok === true;
      } catch {}
      if (!warmed) await sleep(1500);
    }
    if (!warmed) failures.push('could not warm run_rell_tests (trivial test never passed within 3s)');

    // 5 clients fire infinite loops simultaneously; a 6th probes OTHER tools.
    const spinClients = [];
    for (let i = 0; i < 5; i++) spinClients.push(await openClient(srv.base, stats));
    const probeStats = { ok: 0, fail: 0, lat: [] };
    let probing = true;
    const prober = (async () => {
      while (probing) {
        const s = performance.now();
        try {
          const t = text(await call(c0, 'chromia_help', {}, 15_000));
          if (t.includes('chr_deploy_help')) { probeStats.ok++; probeStats.lat.push(performance.now() - s); }
          else probeStats.fail++;
        } catch { probeStats.fail++; }
        await sleep(300);
      }
    })();
    const spins = spinClients.map(async (c, i) => {
      const n = `spin${i}_${nonce()}`;
      const s = performance.now();
      const m = await call(c, 'run_rell_tests', {
        files: { [`s_${n}.rell`]: `@test module;\nfunction test_${n}() { var x = 0; while (x >= 0) { x = (x + 1) % 1000000; } }` },
      }, 90_000);
      const ms = performance.now() - s;
      const t = text(m);
      let j = null; try { j = JSON.parse(t); } catch {}
      // Either a clean timeout result... or (a fast-timeout race) the ceiling
      // error if 4 siblings already leaked before this one entered execute().
      const cleanTimeout = j && j.ok === false && /exceeded 3s/.test(j.notes ?? '');
      const ceiling = /abandoned test runner thread/.test(t);
      expect(cleanTimeout || ceiling, `spinner ${i}: not a clean timeout: ${t.slice(0, 200)}`);
      return { i, ms, outcome: cleanTimeout ? 'timeout' : 'ceiling' };
    });
    let spinResults = [];
    try { spinResults = await Promise.all(spins); }
    catch (e) { failures.push(e.message); }
    for (const r of spinResults) console.log(`  spinner ${r.i}: ${r.outcome} after ${Math.round(r.ms)}ms`);
    probing = false; await prober;
    console.log(`  responsiveness while spinning: ${probeStats.ok} ok / ${probeStats.fail} failed, p95 ${Math.round(pct(probeStats.lat.sort((a, b) => a - b), 95))}ms`);
    if (probeStats.fail > 0) failures.push(`${probeStats.fail} chromia_help probe(s) failed while runners were spinning`);
    if (probeStats.ok < 3) failures.push('too few successful probes while spinning to prove responsiveness');

    // The 5 leaked runners exceed MAX_LEAKED_RUNNERS=4: the next call must be
    // refused with the clear ceiling error, not queued or crashed.
    await sleep(2000);
    const n = nonce();
    const m = await call(c0, 'run_rell_tests', { files: { [`z_${n}.rell`]: `@test module;\nfunction test_${n}() { assert_equals(1, 1); }` } }, 30_000);
    const t = text(m);
    if (!(isErr(m) && /abandoned test runner thread/.test(t) && /restart/.test(t))) {
      failures.push(`leaked-runner ceiling did not engage cleanly: ${t.slice(0, 200)}`);
    } else {
      console.log(`  ceiling engaged: ${t.slice(0, 120)}...`);
    }
    // Server must still answer other tools and /health even at the ceiling.
    const ht = text(await call(c0, 'chromia_help', { topic: 'chr_deploy' }, 15_000));
    if (!ht.includes('0.33')) failures.push('chromia_help broken after ceiling reached');
    const hr = await fetch(`${srv.base}/health`);
    if (hr.status !== 200) failures.push(`/health HTTP ${hr.status} after abuse`);
    spinClients.forEach(closeClient);
    closeClient(c0);
    failures.push(...collectCommonFailures(stats, { requireZeroToolFailures: false }));
    phaseResult('abuse', failures);
  } finally { stopServer(srv); }
}

// ---------------------------------------------------------------- main
try {
  if (PHASES.includes('burst')) await phaseBurst();
  if (PHASES.includes('memory') || PHASES.includes('soak')) {
    await phaseMemoryAndSoak(PHASES.includes('memory'), PHASES.includes('soak'));
  }
  if (PHASES.includes('abuse')) await phaseAbuse();
} catch (e) {
  console.error('HARNESS ERROR:', e);
  results.push({ name: 'harness', pass: false, failures: [e.message] });
}
console.log(`\n=== STRESS/SOAK SUMMARY (${QUICK ? 'quick' : 'full'}) ===`);
for (const r of results) console.log(`${r.pass ? 'PASS' : 'FAIL'} ${r.name}`);
process.exit(results.some(r => !r.pass) ? 1 : 0);
