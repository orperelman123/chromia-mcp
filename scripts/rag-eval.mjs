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
//        --embeddings app/build/embeddings.json [--min-pass 39] [--min-segments 20000]
//
// --production-shaped: boot the way every fresh install does - NO local
// embeddings.json (the path points at nothing), NO cached copy in
// CHROMIA_MCP_HOME (the cache is turned off) and NO token in the environment -
// so the index must come from the published remote. Then require
// that the index the server actually answers from is the GitHub release asset
// (--expect-origin, default /GitHub release asset/) and is not stale. This is
// the check that was missing on 2026-09-04, twice: a fresh store was published
// and production silently kept answering from the 2025-10-21 GitLab package -
// first because the loader could not read it under the JVM caps, then because
// the repo was private and the asset URL was 404. Every unit test was green
// both times. The provenance is read from the structured `index` object on the
// fetch_docs answer (what a client over the wire can see), stderr as fallback.
//
//   node scripts/rag-eval.mjs --production-shaped --jar app/build/libs/chromia-mcp-server.jar
//
// Exit 0 = publishable, 1 = below the bar, 2 = the server could not be driven.
import { spawn } from 'node:child_process';
import { resolve, join } from 'node:path';
import { tmpdir } from 'node:os';

const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, arr) => {
  if (a.startsWith('--')) acc.push([a.slice(2), arr[i + 1] && !arr[i + 1].startsWith('--') ? arr[i + 1] : 'true']);
  return acc;
}, []));
const jar = args.jar ?? 'app/build/libs/chromia-mcp-server.jar';
const productionShaped = args['production-shaped'] === 'true';
const embeddings = productionShaped
  ? join(tmpdir(), `chromia-mcp-no-local-index-${process.pid}`, 'embeddings.json') // does not exist, by design
  : resolve(args.embeddings ?? 'app/build/embeddings.json');
const expectOrigin = new RegExp(args['expect-origin'] ?? 'GitHub release asset', 'i');
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
  // Round 15 (2026-09-05): 24 more, each verified against the 25823-segment
  // store before joining. `--tests` was the one miss (repl.md outranked the
  // page that literally says `chr test --tests my_filter`) and is why long CLI
  // flags are now lexical identifiers; it stays in as the regression probe.
  ['How are rowids allocated for Rell entities in PostgreSQL?', [/rowid/i]],
  ['How do I run only some Rell tests with chr test --tests?', [/--tests/]],
  ['How do I register an FT4 account with a single-sig auth descriptor?', [/single_sig|auth_descriptor/i]],
  ['How does ICCF cross-chain proof verification work?', [/iccf/i]],
  ['How do I transfer FT4 assets between accounts?', [/transfer/i, /asset/i]],
  ['How do I sort and limit results in a Rell at-expression?', [/sort_desc|sort|limit/i, /@\*/]],
  ['How do I do a cross-chain transfer in FT4 with init_transfer and apply_transfer?', [/init_transfer|apply_transfer|crosschain/i]],
  ['How do I start the Rell REPL with chr repl?', [/repl/i]],
  ['How do I declare blockchain signers in chromia.yml?', [/signers/i]],
  ['How do I build and sign a GTX transaction with the postchain client in Kotlin?', [/gtx|gtv/i, /sign/i]],
  ['When should I use a struct instead of an entity in Rell?', [/struct/i, /entity/i]],
  ['How do I declare key and index on a Rell entity?', [/\bkey\b/i, /\bindex\b/i]],
  ['How do I mint an FT4 asset with ft4.assets.Unsafe.mint?', [/mint/i]],
  ['How do I start a local node with chr node start and wipe its database?', [/chr node|--wipe/i]],
  ['What is a Rell operation versus a query and which can modify state?', [/operation/i, /query/i]],
  ['How do I read chain_context.args module arguments in Rell?', [/chain_context/i]],
  ['How do I use op_context.get_signers or is_signer in Rell?', [/op_context|is_signer/i]],
  ['How do I compute a hash or verify a signature in Rell with crypto?', [/crypto|verify_signature|sha256/i]],
  ['How do I set up a Directory Chain container and lease for my dapp?', [/container|lease/i]],
  ['How do I use the EIF bridge to move ERC20 tokens to Chromia?', [/eif|erc20|bridge/i]],
  ['How do I paginate FT4 queries with page_size and page_cursor?', [/page_size|page_cursor|paginat/i]],
  ['How do I use a Rell map and list and iterate with for?', [/\bmap\b/i, /\blist\b/i]],
  ['How do I write a Rell abstract function and override it in another module?', [/abstract/i, /override/i]],
  ['How do I use rell.test.tx and sign with a keypair in a Rell test?', [/rell\.test|\.tx\(|keypair/i]],
];
const minPass = Number(args['min-pass'] ?? PROBES.length - 1);

const env = { ...process.env, CHROMIA_EMBEDDINGS_PATH: embeddings, CHROMIA_MCP_COMPACT_TOOLS: 'true' };
if (productionShaped) {
  // A fresh install has none of these; a token here would test a path it does not take,
  // and a cached copy in ~/.chromia-mcp would make the "download works" check vacuous.
  for (const k of ['CHROMIA_EMBEDDINGS_TOKEN', 'GITHUB_TOKEN', 'GH_TOKEN', 'CHROMIA_EMBEDDINGS_URL']) delete env[k];
  env.CHROMIA_EMBEDDINGS_CACHE = 'off';
  console.log(`PRODUCTION-SHAPED: no local index (${embeddings}), no cache, no token -> the store must come from the published remote`);
}
const proc = spawn('java', ['-jar', jar, '--stdio'], { env });
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

  let pass = 0; const misses = []; let index = null;
  for (const [question, must] of PROBES) {
    // The first call also loads the store (a remote download in production-shaped mode).
    const m = await rpc('tools/call', { name: 'fetch_docs', arguments: { query: question } }, index ? 300_000 : 900_000);
    const hits = m?.result?.structuredContent?.hits ?? [];
    index ??= m?.result?.structuredContent?.index ?? null;
    const haystack = hits.map(h => `${h.title ?? ''}\n${h.url ?? ''}\n${h.text ?? ''}`).join('\n');
    const ok = !m?.result?.isError && must.every(re => re.test(haystack));
    if (ok) pass++; else misses.push({ question, hits: hits.length, top: hits[0]?.title ?? m?.result?.content?.[0]?.text?.slice(0, 120) });
    console.log(`${ok ? 'PASS' : 'MISS'}  ${question}`);
  }

  const provenanceLine = stderr.split('\n').find(l => l.includes('documentation index:')) ?? '';
  const segments = Number(index?.segments ?? /(\d+) segments/.exec(provenanceLine)?.[1] ?? 0);
  console.log(`\n${pass}/${PROBES.length} probes answered (min ${minPass}); ${segments} segments (min ${minSegments})`);
  console.log(index ? `index: ${JSON.stringify(index)}` : provenanceLine.replace(/^.*App - /, '') || 'index: provenance not reported');
  for (const miss of misses) console.log(`  miss: ${miss.question} -> hits=${miss.hits} top=${miss.top}`);

  let publishable = pass >= minPass && segments >= minSegments;
  if (productionShaped) {
    const origin = index?.origin ?? /from (.*?), generated/.exec(provenanceLine)?.[1] ?? '';
    const originOk = expectOrigin.test(origin);
    const fresh = index ? index.stale === false : !/STALE/.test(stderr);
    console.log(`${originOk ? 'PASS' : 'FAIL'}  index origin matches ${expectOrigin} (${origin || 'none'})`);
    console.log(`${fresh ? 'PASS' : 'FAIL'}  index is not stale${index?.age_days != null ? ` (${index.age_days} days old)` : ''}`);
    publishable = publishable && originOk && fresh;
  }
  console.log(publishable
    ? (productionShaped ? '\nGATE PASSED: a production-shaped boot answers from the published index' : '\nGATE PASSED: store is publishable')
    : (productionShaped ? '\nGATE FAILED: production would not answer from the published index' : '\nGATE FAILED: store must not be published'));
  proc.kill();
  process.exit(publishable ? 0 : 1);
} catch (e) {
  console.error(`rag-eval could not drive the server: ${e.message}\n${stderr.slice(-2000)}`);
  proc.kill();
  process.exit(2);
}
