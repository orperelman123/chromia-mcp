#!/usr/bin/env node
// RAG quality gate for a freshly generated embeddings.json.
//
// Starts the server over stdio against the given store and asks fetch_docs the
// probe questions below. Each probe names the substrings a correct answer must
// contain somewhere in the returned hits. The `Embeddings refresh` workflow runs
// this before publishing: a store that answers fewer probes than --min-pass, or
// that shrank below --min-segments (a half-failed ingest), is not published.
//
//   node scripts/rag-eval.mjs --jar app/build/libs/chromia-mcp-server.jar \
//        --embeddings app/build/embeddings.json [--min-pass 15] [--min-segments 20000]
//
// Exit 0 = publishable, 1 = below the bar, 2 = the server could not be driven.
import { spawn } from 'node:child_process';
import { resolve } from 'node:path';

const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, arr) => {
  if (a.startsWith('--')) acc.push([a.slice(2), arr[i + 1] && !arr[i + 1].startsWith('--') ? arr[i + 1] : 'true']);
  return acc;
}, []));
const jar = args.jar ?? 'app/build/libs/chromia-mcp-server.jar';
const embeddings = resolve(args.embeddings ?? 'app/build/embeddings.json');
const minSegments = Number(args['min-segments'] ?? 20000);

// [question, substrings/regexes a correct answer must contain (all of them, case-insensitive)]
// Round 10 (2026-09-04) set: the misses that motivated hybrid retrieval and the
// staleness note are in here, so a regression on either fails the gate.
export const PROBES = [
  ['How do I declare which chain to deploy to in chromia.yml with chr deployment create?', [/deployments/i, /chains/i]],
  ['What merkle_hash_version should a new blockchain config use?', [/merkle_hash_version/i, /\b2\b/]],
  ['How do I submit a transaction to a Postchain node over REST?', [/\/tx\b/]],
  ['Where should require_mandatory_flags be set in an FT4 auth descriptor?', [/require_mandatory_flags/i]],
  ['Which FT4 modules must never be included in a production dapp?', [/admin/i]],
  ['How does Rell store json values in PostgreSQL?', [/json/i]],
  ['How do I write a Rell unit test with @test module and run_must_fail?', [/@test/i, /run_must_fail|rell\.test/i]],
  ['How do I authenticate an operation with ft4 auth.authenticate?', [/auth\.authenticate|authenticate\(\)/i]],
  ['What does chr install do and where do libraries come from?', [/chr install|libs/i]],
  ['How do I query a dapp with the postchain client from TypeScript?', [/query/i, /client/i]],
  ['How do I get the blockchain RID of a deployed dapp from the directory chain?', [/brid|blockchain.?rid/i]],
  ['What are the ICMF message topics and how do I receive a message in Rell?', [/icmf/i]],
  ['How do I add a module_args struct and pass values in chromia.yml?', [/module_args/i, /moduleArgs/]],
  ['What is the difference between @? and @* at-expressions in Rell?', [/@\?/, /@\*/]],
  ['How do I set up a rate limit for FT4 accounts?', [/rate_limit/i]],
  ['FT4 auth descriptor flags', [/flags/i, /auth.?descriptor/i]],
];
const minPass = Number(args['min-pass'] ?? PROBES.length - 1);

const proc = spawn('java', ['-jar', jar, '--stdio'], {
  env: { ...process.env, CHROMIA_EMBEDDINGS_PATH: embeddings, CHROMIA_MCP_COMPACT_TOOLS: 'true' },
});
let buf = ''; const pending = new Map(); let nextId = 1; let stderr = '';
proc.stdout.on('data', d => {
  buf += d.toString(); let i;
  while ((i = buf.indexOf('\n')) >= 0) {
    const line = buf.slice(0, i).trim(); buf = buf.slice(i + 1);
    if (!line) continue;
    try { const m = JSON.parse(line); if (m.id !== undefined && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } } catch { /* log noise */ }
  }
});
proc.stderr.on('data', d => { stderr += d.toString(); });
proc.on('exit', code => {
  if (pending.size) { console.error(`server exited (code ${code}) with ${pending.size} calls outstanding\n${stderr.slice(-2000)}`); process.exit(2); }
});
const rpc = (method, params, timeoutMs = 300_000) => {
  const id = nextId++;
  proc.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
  return new Promise((res, rej) => {
    const h = setTimeout(() => rej(new Error(`timeout ${method}`)), timeoutMs);
    pending.set(id, m => { clearTimeout(h); res(m); });
  });
};

try {
  await rpc('initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'rag-eval', version: '1' } });
  proc.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) + '\n');

  let pass = 0; const misses = [];
  for (const [question, must] of PROBES) {
    const m = await rpc('tools/call', { name: 'fetch_docs', arguments: { query: question } });
    const hits = m?.result?.structuredContent?.hits ?? [];
    const haystack = hits.map(h => `${h.title ?? ''}\n${h.url ?? ''}\n${h.text ?? ''}`).join('\n');
    const ok = !m?.result?.isError && must.every(re => re.test(haystack));
    if (ok) pass++; else misses.push({ question, hits: hits.length, top: hits[0]?.title ?? m?.result?.content?.[0]?.text?.slice(0, 120) });
    console.log(`${ok ? 'PASS' : 'MISS'}  ${question}`);
  }

  const provenance = stderr.split('\n').find(l => l.includes('documentation index:')) ?? '';
  const segments = Number(/(\d+) segments/.exec(provenance)?.[1] ?? 0);
  console.log(`\n${pass}/${PROBES.length} probes answered (min ${minPass}); ${segments} segments (min ${minSegments})`);
  console.log(provenance.replace(/^.*App - /, ''));
  for (const miss of misses) console.log(`  miss: ${miss.question} -> hits=${miss.hits} top=${miss.top}`);

  const publishable = pass >= minPass && segments >= minSegments;
  console.log(publishable ? '\nGATE PASSED: store is publishable' : '\nGATE FAILED: store must not be published');
  proc.kill();
  process.exit(publishable ? 0 : 1);
} catch (e) {
  console.error(`rag-eval could not drive the server: ${e.message}\n${stderr.slice(-2000)}`);
  proc.kill();
  process.exit(2);
}
