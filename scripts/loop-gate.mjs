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
if (failures || errors) fail(`${failures} failure(s), ${errors} error(s)`);
const unexpected = skippedNames.filter((s) => ![...allowSkip].some((a) => s.endsWith(a) || s === a));
if (unexpected.length) fail(`unexpected skip(s) - a skip is a test that did not run:\n  ${unexpected.join('\n  ')}`);
if (gradle.status !== 0) fail(`gradle exited ${gradle.status} despite a clean tally - read the output above`);

console.log(`gate: PASSED (${tests} tests, ${skippedNames.length} allowlisted skip(s))`);
