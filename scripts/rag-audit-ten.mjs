#!/usr/bin/env node
// The ten Rell questions from the agent-experience audit (finding F15), asked
// through `search` exactly as the audit asked them, printing the TOP HIT for
// each so a corpus or ranking change can be graded before/after.
//
//   node scripts/rag-audit-ten.mjs --jar app/build/libs/chromia-mcp-server.jar \
//        --embeddings app/build/embeddings.json [--json out.json]
//
// The audit's measured baseline (fresh index, 25,823 segments, 2026-09-06) is
// BASELINE below: 3 correct / 5 partly / 2 wrong, with two answers coming out of
// the Rell compiler's own Kotlin unit tests. Exit 1 when any top hit is a
// host-language source file rather than a documentation page.
import { spawn } from 'node:child_process';
import { resolve } from 'node:path';
import { writeFileSync } from 'node:fs';

export const QUESTIONS = [
  'How do I declare module_args in a Rell module and read them?',
  'FT4 authentication: how do I require a signer and authorize an operation?',
  'Rell at-expression syntax: how do I filter, sort and limit a query over an entity?',
  'When should I use require versus assert in Rell?',
  'big_integer arithmetic in Rell: division, rounding and overflow',
  'How do I get the current block time in a Rell operation?',
  'How do I define an index or a key on a Rell entity?',
  'Rell import statement: importing a module and aliasing it',
  'How do I structure a Rell test module and run tests?',
  'chromia.yml deployment configuration: deployments block, brid, container',
];

// Top hit and grade on the index the audit measured, in question order.
export const BASELINE = [
  ['ReplDefinitionTest.kt', 'wrong'],
  ['transaction-builder.ts', 'partly'],
  ['rell-architecture.md', 'wrong'],
  ['require-function.md', 'correct'],
  ['rt_primitive_types.kt', 'partly'],
  ['LibRellTestBlockClockTest.kt', 'partly'],
  ['core-concepts.md', 'correct'],
  ['modules.rst', 'partly'],
  ['0.10.4.txt', 'partly'],
  ['deploy-dapp.md', 'correct'],
];

const DOCS_EXTENSIONS = new Set(['md', 'mdx', 'rst', 'adoc']);
const CODE_EXTENSIONS = new Set(['kt', 'kts', 'java', 'ts', 'js', 'py']);
const extensionOf = (title) => (title ?? '').toLowerCase().split('.').pop();

const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, arr) => {
  if (a.startsWith('--')) acc.push([a.slice(2), arr[i + 1] && !arr[i + 1].startsWith('--') ? arr[i + 1] : 'true']);
  return acc;
}, []));
const jar = resolve(args.jar ?? 'app/build/libs/chromia-mcp-server.jar');
const embeddings = resolve(args.embeddings ?? 'app/build/embeddings.json');

const proc = spawn('java', ['-jar', jar, '--stdio'], {
  env: { ...process.env, CHROMIA_MCP_EMBEDDINGS_PATH: embeddings },
  stdio: ['pipe', 'pipe', 'pipe'],
});
let stdout = '';
const pending = new Map();
proc.stdout.on('data', (d) => {
  stdout += d;
  let nl;
  while ((nl = stdout.indexOf('\n')) >= 0) {
    const line = stdout.slice(0, nl).trim();
    stdout = stdout.slice(nl + 1);
    if (!line) continue;
    let message;
    try { message = JSON.parse(line); } catch { continue; }
    const resolveFn = pending.get(message.id);
    if (resolveFn) { pending.delete(message.id); resolveFn(message); }
  }
});
proc.stderr.on('data', () => {});

let nextId = 1;
const send = (method, params) => new Promise((res) => {
  const id = nextId++;
  pending.set(id, res);
  proc.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
});

const main = async () => {
  await send('initialize', {
    protocolVersion: '2024-11-05',
    capabilities: {},
    clientInfo: { name: 'rag-audit-ten', version: '1' },
  });
  proc.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) + '\n');

  const rows = [];
  for (const question of QUESTIONS) {
    const answer = await send('tools/call', { name: 'search', arguments: { query: question } });
    const text = answer?.result?.content?.[0]?.text ?? '';
    let top = null;
    try {
      const parsed = JSON.parse(text);
      const results = parsed.results ?? parsed.hits ?? [];
      top = results[0] ?? null;
    } catch { /* non-JSON answer: leave top null */ }
    rows.push({ question, title: top?.title ?? null, url: top?.url ?? null, id: top?.id ?? null });
  }

  let regressions = 0;
  console.log('#  top hit (this index)                 was (audit baseline)          grade-was');
  rows.forEach((row, i) => {
    const [wasTitle, wasGrade] = BASELINE[i];
    const ext = extensionOf(row.title);
    const flag = CODE_EXTENSIONS.has(ext) ? ' <-- source file' : DOCS_EXTENSIONS.has(ext) ? '' : ' <-- not a docs page';
    if (CODE_EXTENSIONS.has(ext)) regressions++;
    console.log(
      `${String(i + 1).padStart(2)} ${String(row.title ?? '(no hit)').padEnd(36)} ${wasTitle.padEnd(29)} ${wasGrade}${flag}`
    );
  });
  const docs = rows.filter((r) => DOCS_EXTENSIONS.has(extensionOf(r.title))).length;
  console.log(`\ndocs-page top hits: ${docs}/${rows.length} (audit baseline: 4/10); source-file top hits: ${regressions} (baseline: 3)`);

  if (args.json && args.json !== 'true') writeFileSync(args.json, JSON.stringify(rows, null, 1));
  proc.kill();
  process.exit(regressions > 0 ? 1 : 0);
};

setTimeout(() => { console.error('timeout'); proc.kill(); process.exit(2); }, 15 * 60_000);
main().catch((e) => { console.error(e); proc.kill(); process.exit(2); });
