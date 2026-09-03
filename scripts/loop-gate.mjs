#!/usr/bin/env node
// The merge gate, as a script rather than a habit.
//
// Every fake green this project has shipped came from a verification that
// looked like proof and was not:
//   - `BUILD SUCCESSFUL in 8s` from a task Gradle had cached (nothing ran)
//   - an empty results directory read as "0 failures"
//   - a green suite quietly carrying skips, so it covered less than it claimed
//   - a run whose XMLs belonged to an earlier, different tree
// Each was caught by hand, once, by remembering to look. This makes the looking
// structural: one command, exit 0 only when the evidence is real.
//
//   node scripts/loop-gate.mjs [--dir <repo>] [--expect-min <n>] [--allow-skip <Class::test>]...
//
// Exits 0 only if: the suite actually executed in THIS invocation, every result
// file is newer than the run's start, 0 failures, 0 errors, and every skip is
// named on the allowlist. Prints the tally it verified, always.

import { spawnSync } from 'node:child_process';
import { connect } from 'node:net';
import { readdirSync, readFileSync, statSync, rmSync, existsSync } from 'node:fs';
import { join, resolve } from 'node:path';

const argv = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = argv.indexOf(name);
  return i >= 0 && argv[i + 1] ? argv[i + 1] : fallback;
};
const repo = resolve(opt('--dir', process.cwd()));
const expectMin = Number(opt('--expect-min', '0'));
const allowSkip = new Set(argv.flatMap((a, i) => (a === '--allow-skip' ? [argv[i + 1]] : [])));
const resultsDir = join(repo, 'app', 'build', 'test-results', 'test');

const fail = (msg) => { console.error(`GATE FAILED: ${msg}`); process.exit(1); };

// PREFLIGHT: is the database actually up?
//
// A dead PostgreSQL does not look like a dead PostgreSQL from in here - it looks
// like 58 failing tests. That happened on 2026-09-03: the WSL cluster stopped
// mid-run, the suite went red across nine unrelated classes, and the failures
// read as a broken AMM template until every message turned out to be
// "Connection to localhost:5433 refused". Two full 12-minute runs were spent
// before anyone looked at the text. The suite cannot tell infrastructure from
// code, so the gate says it up front - and fails in a second rather than in
// twelve minutes.
const dbUrl = (() => {
  if (process.env.CHROMIA_TEST_DATABASE_URL) return process.env.CHROMIA_TEST_DATABASE_URL;
  for (const f of [join(repo, 'local-test-env.properties'), join(repo, '..', 'chromia-mcp', 'local-test-env.properties')]) {
    if (!existsSync(f)) continue;
    const m = readFileSync(f, 'utf8').match(/^\s*CHROMIA_TEST_DATABASE_URL\s*=\s*(.+)$/m);
    if (m) return m[1].trim();
  }
  return null;
})();

if (dbUrl) {
  const hp = dbUrl.match(/\/\/([^:/?]+):(\d+)/);
  if (hp) {
    const [, host, port] = hp;
    const reachable = await new Promise((resolve) => {
      const sock = connect({ host, port: Number(port) });
      const done = (ok) => { sock.destroy(); resolve(ok); };
      sock.setTimeout(4000);
      sock.on('connect', () => done(true));
      sock.on('timeout', () => done(false));
      sock.on('error', () => done(false));
    });
    if (!reachable) {
      fail(`the test database at ${host}:${port} is not accepting connections - this is INFRASTRUCTURE, not your code.
` +
        `  Every DB-backed test would fail with "Connection refused" and read as a broken change.
` +
        `  Start it, then re-run:  wsl.exe -d Ubuntu -u root -- service postgresql start`);
    }
    console.log(`gate: database at ${host}:${port} is up`);
  }
} else {
  console.log('gate: no CHROMIA_TEST_DATABASE_URL found - DB-backed tests will fail, not skip (that is deliberate)');
}

// Clear stale results so a crashed or cached run cannot be mistaken for this one.
if (existsSync(resultsDir)) {
  try { rmSync(resultsDir, { recursive: true, force: true }); }
  catch (e) { fail(`could not clear stale results (${e.code}); another build is probably running - wait for it`); }
}

const startedAt = Date.now();
console.log(`gate: running full suite in ${repo} (forced rerun)`);
// --rerun-tasks: Gradle's up-to-date check is the single biggest source of
// "successful" builds that executed nothing.
// A .bat needs a shell on Windows; without one spawn fails silently and the
// gate would report "the suite did not run" without saying it never started it.
const isWin = process.platform === 'win32';
const gradle = isWin
  ? spawnSync('cmd', ['/c', join(repo, 'gradlew.bat'), 'test', '--rerun-tasks'],
      { cwd: repo, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
  : spawnSync(join(repo, 'gradlew'), ['test', '--rerun-tasks'],
      { cwd: repo, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
if (gradle.error) fail(`could not start gradle: ${gradle.error.message}`);
const out = `${gradle.stdout ?? ''}${gradle.stderr ?? ''}`;
for (const line of out.split('\n')) {
  if (/^e: |BUILD (SUCCESSFUL|FAILED)|tests? completed|OutOfMemory|FAILED$/.test(line)) console.log(`  ${line.trim()}`);
}

if (!existsSync(resultsDir)) fail('no test-results directory - the suite did not run');
const files = readdirSync(resultsDir).filter((f) => f.endsWith('.xml'));
if (files.length === 0) fail('no result files - the suite did not run (a fast "BUILD SUCCESSFUL" means a cached task)');

let tests = 0, failures = 0, errors = 0, skipped = 0;
const skippedNames = [], stale = [];
for (const f of files) {
  const path = join(resultsDir, f);
  if (statSync(path).mtimeMs < startedAt) stale.push(f);
  const xml = readFileSync(path, 'utf8');
  const suite = xml.match(/<testsuite\b[^>]*>/)?.[0] ?? '';
  const num = (attr) => Number(suite.match(new RegExp(`${attr}="(\\d+)"`))?.[1] ?? 0);
  tests += num('tests'); failures += num('failures'); errors += num('errors'); skipped += num('skipped');
  const cls = suite.match(/name="([^"]+)"/)?.[1] ?? f;
  for (const tc of xml.split('<testcase').slice(1)) {
    if (/<skipped\b/.test(tc)) {
      const name = tc.match(/name="([^"]+)"/)?.[1] ?? '?';
      skippedNames.push(`${cls.replace(/^org\.chromia\./, '')}::${name}`);
    }
  }
}

console.log(`gate: tests=${tests} failures=${failures} errors=${errors} skipped=${skipped} files=${files.length}`);
for (const s of skippedNames) console.log(`  skip: ${s}`);

if (stale.length) fail(`${stale.length} result file(s) predate this run (e.g. ${stale[0]}) - you are reading someone else's evidence`);
if (tests === 0) fail('zero tests recorded');
if (tests < expectMin) fail(`only ${tests} tests ran, expected at least ${expectMin} - a filter or a compile failure silently narrowed the suite`);
if (failures || errors) {
  // Same lesson from the other end: if the cluster dies PART WAY through, the
  // preflight above passed and the tally is still meaningless. Name it rather
  // than letting a reader diff a template against 58 unrelated reds.
  const conn = files.reduce((n, f) => {
    const xml = readFileSync(join(resultsDir, f), 'utf8');
    return n + (xml.match(/Connection (?:to [^"<]*refused|has been closed)/g) ?? []).length;
  }, 0);
  if (conn) {
    console.error(`  NOTE: ${conn} failure(s) are database connection errors - the cluster went down DURING this run.`);
    console.error('  That is infrastructure, not your change. Restart it and re-run before reading anything into these.');
  }
  fail(`${failures} failure(s), ${errors} error(s)`);
}
const unexpected = skippedNames.filter((s) => ![...allowSkip].some((a) => s.endsWith(a) || s === a));
if (unexpected.length) fail(`unexpected skip(s) - a skip is a test that did not run:\n  ${unexpected.join('\n  ')}`);
if (gradle.status !== 0) fail(`gradle exited ${gradle.status} despite a clean tally - read the output above`);

console.log(`gate: PASSED (${tests} tests, ${skippedNames.length} allowlisted skip(s))`);
