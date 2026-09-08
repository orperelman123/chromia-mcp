#!/usr/bin/env node
// THE TALLY, AND THE THIRD STATUS - one implementation, used by every gate.
//
//   node scripts/gate-tally.mjs [--dir <repo>] [--results <dir>] [--warnings <dir>]
//                               [--started-at <epoch-ms>] [--expect-min <n>] [--json]
//
// Or, 2026-09-08: a live test whose third party is PROVEN down is neither a pass
// nor a red. It is an UPSTREAM WARNING - a third status - and it can never be
// produced by our own code failing.
//
// THE THREE STATUSES, and what qualifies:
//
//   PASS      the test ran and its claim held.
//   RED       everything else, including every skip, every failure with no
//             proof, and every failure whose proof does not match this run.
//   UPSTREAM  a FAILURE (it is a <failure> in the XML like any other; nothing
//             pretends to pass) whose message begins `UPSTREAM WARNING
//             (proven): ` AND which is backed by an evidence file in
//             app/build/upstream/warnings/<Class>.<method>.json carrying:
//               - an allowlisted signature only the third party can produce,
//               - a canary that FAILED, or a DATED docs/UPSTREAM.md entry
//                 naming the query (a partial outage - the explorer answers,
//                 one query does not - is excused by the ledger or not at all),
//               - a timestamp inside this run.
//             It is counted and printed BY NAME with its evidence, and it does
//             NOT set the exit code.
//
// The message prefix ALONE is never enough. A test can write any string it
// likes into an assertion; what it cannot do is write an evidence file whose
// canary disagrees with it, or name a ledger entry that does not exist. That is
// why the proof lives in a file the producing test did not get to shape freely
// (LiveEnv writes it, from a measurement) and why this script re-checks it
// rather than trusting the sentence.
//
// The failure mode this whole shape is designed against is adversary round 18
// section 4: `get_asset_top_holders` answered eight consecutive live
// INTERNAL_ERRORs and the suite reported eight PASSES, because an upstream
// marker in the text was treated as evidence. A warning is a RED FOR THE
// UPSTREAM, not for us - it is never a green.

import { readdirSync, readFileSync, statSync, existsSync } from 'node:fs';
import { join, resolve, basename } from 'node:path';

/** The prefix LiveEnv.UPSTREAM_WARNING_PREFIX writes. Kept identical on purpose. */
export const UPSTREAM_WARNING_PREFIX = 'UPSTREAM WARNING (proven): ';

/**
 * The signature names LiveEnv.UPSTREAM_SIGNATURES may report. A file naming
 * anything else is not proof - it is a file. Mirrors the narrow allowlist in
 * LiveEnv.kt, which is itself the sweep's list (scripts/upstream-classifier.mjs)
 * minus everything that could be ours (4xx, connection refused, bare "timeout").
 */
export const UPSTREAM_SIGNATURE_NAMES = [
  'explorer-graphql-internal-error',
  'explorer-request-timeout',
  'explorer-recaptcha',
  'explorer-http-5xx',
];

/**
 * `timeout.set(Duration.ofMinutes(90))` in app/build.gradle.kts. Nothing this
 * run produced can be older than that, so it is the honest default window when
 * a caller does not know the run's start (CI runs on a fresh checkout; the merge
 * gate passes its real start instead).
 */
export const TEST_TASK_TIMEOUT_MS = 90 * 60 * 1000;

const decode = (s) => String(s ?? '')
  .replace(/&lt;/g, '<').replace(/&gt;/g, '>')
  .replace(/&quot;/g, '"').replace(/&apos;/g, "'")
  .replace(/&#10;/g, '\n').replace(/&#13;/g, '\r').replace(/&#9;/g, '\t')
  .replace(/&amp;/g, '&');

/** Every evidence file in [dir], keyed `Class.method`. */
export function readWarnings(dir) {
  const found = new Map();
  if (!dir || !existsSync(dir)) return found;
  for (const file of readdirSync(dir).filter((f) => f.endsWith('.json'))) {
    const path = join(dir, file);
    let parsed = null;
    let error = null;
    try { parsed = JSON.parse(readFileSync(path, 'utf8')); }
    catch (e) { error = `unreadable: ${e.message}`; }
    found.set(basename(file, '.json'), { file: path, warning: parsed, error });
  }
  return found;
}

/**
 * Is this evidence file actually proof, for a run that started at [startedAt]?
 * Returns { ok, why } - `why` explains a refusal in the operator's terms.
 */
export function warningProof(entry, key, startedAt) {
  if (!entry) return { ok: false, why: `no evidence file app/build/upstream/warnings/${key}.json` };
  if (entry.error) return { ok: false, why: `evidence file ${entry.error}` };
  const w = entry.warning;
  if (!w || typeof w !== 'object') return { ok: false, why: 'evidence file is not an object' };
  if (w.test !== key) return { ok: false, why: `evidence names ${JSON.stringify(w.test)}, not ${key}` };
  if (!UPSTREAM_SIGNATURE_NAMES.includes(w.signature)) {
    return { ok: false, why: `signature ${JSON.stringify(w.signature)} is not allowlisted` };
  }
  if (typeof w.errorText !== 'string' || w.errorText.trim() === '') {
    return { ok: false, why: "the third party's own words are missing" };
  }
  // The guardrail: the explorer is down for everything, or this one query is
  // written down as broken, with a date. Nothing else excuses a live failure.
  const canaryDown = typeof w.canaryOutcome === 'string' && w.canaryOutcome !== 'ANSWERED';
  const ledgered = typeof w.ledgerEntry === 'string' && w.ledgerEntry !== '' &&
    typeof w.ledgerHeading === 'string' && w.ledgerHeading !== '';
  if (!canaryDown && !ledgered) {
    return {
      ok: false,
      why: `the canary ANSWERED and no dated docs/UPSTREAM.md entry names \`${w.query}\` - ` +
        'a partial outage is excused by a dated ledger entry or not at all',
    };
  }
  const at = Date.parse(w.at ?? '');
  if (Number.isNaN(at)) return { ok: false, why: `evidence has no usable timestamp (${JSON.stringify(w.at)})` };
  if (at < startedAt) {
    return {
      ok: false,
      why: `evidence is dated ${w.at}, before this run started (${new Date(startedAt).toISOString()}) - ` +
        'a warning from an earlier run cannot excuse this one',
    };
  }
  return { ok: true, why: canaryDown ? `canary ${w.canaryOutcome}` : `docs/UPSTREAM.md #${w.ledgerEntry}` };
}

/**
 * Reads the JUnit XMLs in [resultsDir] and the evidence in [warningsDir] and
 * classifies every non-passing test into RED or UPSTREAM.
 */
export function tally({ resultsDir, warningsDir, startedAt = null }) {
  if (!existsSync(resultsDir)) {
    return { error: `no test-results directory at ${resultsDir} - the suite did not run` };
  }
  const files = readdirSync(resultsDir).filter((f) => f.endsWith('.xml'));
  if (files.length === 0) {
    return { error: 'no result files - the suite did not run (a fast "BUILD SUCCESSFUL" means a cached task)' };
  }

  const mtimes = files.map((f) => statSync(join(resultsDir, f)).mtimeMs);
  // Without a caller-supplied start, nothing this run produced can predate the
  // oldest XML by more than the test task's own timeout.
  const runStart = startedAt ?? (Math.min(...mtimes) - TEST_TASK_TIMEOUT_MS);
  const warnings = readWarnings(warningsDir);

  let tests = 0, failures = 0, errors = 0, skipped = 0;
  const skippedNames = [], upstream = [], red = [], stale = [], ranClasses = new Set();

  for (const f of files) {
    const path = join(resultsDir, f);
    if (startedAt !== null && statSync(path).mtimeMs < startedAt) stale.push(f);
    const xml = readFileSync(path, 'utf8');
    const suite = xml.match(/<testsuite\b[^>]*>/)?.[0] ?? '';
    const num = (attr) => Number(suite.match(new RegExp(`${attr}="(\\d+)"`))?.[1] ?? 0);
    tests += num('tests'); failures += num('failures'); errors += num('errors'); skipped += num('skipped');
    const cls = (suite.match(/name="([^"]+)"/)?.[1] ?? f).replace(/^org\.chromia\./, '');
    ranClasses.add(cls.replace(/\$.*$/, ''));

    for (const tc of xml.split('<testcase').slice(1)) {
      const name = tc.match(/name="([^"]+)"/)?.[1] ?? '?';
      if (/<skipped\b/.test(tc)) { skippedNames.push(`${cls}::${name}`); continue; }
      const problem = tc.match(/<(failure|error)\b[^>]*\bmessage="([^"]*)"/);
      if (!problem) continue;
      const [, kind, rawMessage] = problem;
      const message = decode(rawMessage);
      const key = `${cls.replace(/\$.*$/, '')}.${name}`;
      if (!message.startsWith(UPSTREAM_WARNING_PREFIX)) {
        red.push({ key, kind, message, why: null });
        continue;
      }
      const proof = warningProof(warnings.get(key), key, runStart);
      if (proof.ok) {
        upstream.push({ key, kind, message, ...warnings.get(key).warning, proofBy: proof.why });
      } else {
        // A message claiming the third status without evidence that survives
        // this check is an ORDINARY RED. Failing closed is the whole design.
        red.push({ key, kind, message, why: `claims UPSTREAM WARNING but ${proof.why}` });
      }
    }
  }

  const usedKeys = new Set(upstream.map((u) => u.key));
  const orphanWarnings = [...warnings.keys()].filter((k) => !usedKeys.has(k));

  return {
    resultsDir, warningsDir, files: files.length, runStart,
    tests, failures, errors, skipped,
    skippedNames, upstream, red, stale, orphanWarnings,
    ranClasses: [...ranClasses].sort(),
  };
}

/** `gate: tests=... failures=... errors=... skipped=0 upstream=N (names...)`. */
export function gateLine(t) {
  const names = t.upstream.map((u) => u.key).join(', ');
  return `gate: tests=${t.tests} failures=${t.failures} errors=${t.errors} ` +
    `skipped=${t.skipped} upstream=${t.upstream.length}` +
    (t.upstream.length ? ` (${names})` : '') + ` files=${t.files}`;
}

/** Prints the tally and every warning's evidence. Returns true when it is clean. */
export function report(t, { log = console.log, err = console.error, expectMin = 0 } = {}) {
  log(gateLine(t));
  for (const u of t.upstream) {
    log(`  upstream: ${u.key}`);
    log(`      tool: ${u.tool}   query: ${u.query}   signature: ${u.signature}`);
    log(`     proof: ${u.proof ?? u.proofBy}`);
    log(`    canary: ${u.canary?.outcome ?? u.canaryOutcome} at ${u.canary?.at ?? '?'}` +
      (u.canary?.explorerSaid ? ` - ${String(u.canary.explorerSaid).slice(0, 120)}` : ''));
    log(`  upstream said: ${String(u.errorText).replace(/\s+/g, ' ').slice(0, 200)}`);
    log(`  evidence: app/build/upstream/warnings/${u.key}.json`);
  }
  for (const s of t.skippedNames) log(`  skip: ${s}`);
  for (const r of t.red) log(`  FAILED: ${r.key}${r.why ? ` <- ${r.why}` : ''}`);
  for (const o of t.orphanWarnings) {
    log(`  NOTE: evidence file ${o}.json matches no UPSTREAM WARNING failure in these results`);
  }

  let ok = true;
  if (t.stale.length) {
    err(`GATE FAILED: ${t.stale.length} result file(s) predate this run (e.g. ${t.stale[0]}) - ` +
      'you are reading someone else\'s evidence');
    ok = false;
  }
  if (t.tests === 0) { err('GATE FAILED: zero tests recorded'); ok = false; }
  if (expectMin && t.tests < expectMin) {
    err(`GATE FAILED: only ${t.tests} tests ran, expected at least ${expectMin} - a filter or a ` +
      'compile failure silently narrowed the suite');
    ok = false;
  }
  if (t.red.length) {
    err(`GATE FAILED: ${t.red.length} failure(s)/error(s) that are OURS:`);
    for (const r of t.red) err(`  ${r.key}${r.why ? ` <- ${r.why}` : ''}`);
    ok = false;
  }
  if (t.skippedNames.length) {
    err('GATE FAILED: skipped test(s) - a skip is a test that did not run, and there is no allowlist:\n  ' +
      t.skippedNames.join('\n  '));
    ok = false;
  }
  if (ok && t.upstream.length) {
    log(`gate: ${t.upstream.length} PROVEN upstream outage(s). A warning is a red for the UPSTREAM, not`);
    log('  for us: nothing those tests name was verified, the ledger entry is the debt, and the');
    log('  remedy is to fix or wait for the third party and RE-RUN. It is not a pass.');
  }
  return ok;
}

// --------------------------------------------------------------------------
// CLI
// --------------------------------------------------------------------------
const isMain = process.argv[1] && import.meta.url === `file://${process.argv[1].replace(/\\/g, '/')}`;
if (isMain) {
  const argv = process.argv.slice(2);
  const opt = (name, fallback) => {
    const i = argv.indexOf(name);
    return i >= 0 && argv[i + 1] ? argv[i + 1] : fallback;
  };
  const repo = resolve(opt('--dir', process.cwd()));
  const resultsDir = resolve(opt('--results', join(repo, 'app', 'build', 'test-results', 'test')));
  const warningsDir = resolve(opt('--warnings', join(repo, 'app', 'build', 'upstream', 'warnings')));
  const startedAtArg = opt('--started-at', null);
  const t = tally({
    resultsDir, warningsDir,
    startedAt: startedAtArg === null ? null : Number(startedAtArg),
  });
  if (t.error) { console.error(`GATE FAILED: ${t.error}`); process.exit(1); }
  if (argv.includes('--json')) { console.log(JSON.stringify(t, null, 2)); process.exit(t.red.length || t.skippedNames.length ? 1 : 0); }
  const ok = report(t, { expectMin: Number(opt('--expect-min', '0')) });
  process.exit(ok ? 0 : 1);
}
