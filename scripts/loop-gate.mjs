#!/usr/bin/env node
// The merge gate, as a script rather than a habit.
//
// Every fake green this project has shipped came from a verification that
// looked like proof and was not:
//   - `BUILD SUCCESSFUL in 8s` from a task Gradle had cached (nothing ran)
//   - an empty results directory read as "0 failures"
//   - a green suite quietly carrying skips, so it covered less than it claimed
//   - a run whose XMLs belonged to an earlier, different tree
//   - a HAND-PICKED subset of classes presented as "the tests that cover this"
// Each was caught by hand, once, by remembering to look. This makes the looking
// structural: one command, exit 0 only when the evidence is real.
//
//   node scripts/loop-gate.mjs [--dir <repo>] [--expect-min <n>]
//   node scripts/loop-gate.mjs [--dir <repo>] --docs-only --base <commit>
//
// Exits 0 only if: the suite actually executed in THIS invocation, every result
// file is newer than the run's start, 0 failures, 0 errors, and ZERO SKIPS.
// Prints the tally it verified, always.
//
// THERE IS NO SKIP ALLOWLIST. `--allow-skip <Class::test>` used to exist and was
// removed 2026-09-07: it let a local gate pass with a test that did not run,
// while CI's own check (`ALLOWED = set()`, "Fail on any skipped test") refused
// the same evidence. Two gates with different definitions of green is one gate
// and one bypass. The environment-gated tests are enabled instead - see the
// live-environment preflight below - so nothing has to be excused.

import { spawnSync } from 'node:child_process';
import { connect } from 'node:net';
import { readdirSync, readFileSync, statSync, rmSync, existsSync } from 'node:fs';
import { join, resolve, basename } from 'node:path';
// THE TALLY AND THE THIRD STATUS live in ONE file, imported here and run by CI
// as a step of its own. Two gates with different definitions of green is one
// gate and one bypass - that is exactly what `--allow-skip` was - and a second
// copy of the classification would be the same mistake made by duplication.
import { tally, report, gateLine } from './gate-tally.mjs';

const argv = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = argv.indexOf(name);
  return i >= 0 && argv[i + 1] ? argv[i + 1] : fallback;
};
const flag = (name) => argv.includes(name);
const repo = resolve(opt('--dir', process.cwd()));
const expectMin = Number(opt('--expect-min', '0'));
const docsOnly = flag('--docs-only');
const docsBase = opt('--base', null);
const resultsDir = join(repo, 'app', 'build', 'test-results', 'test');
// Evidence for the third status: one file per PROVEN upstream outage, written
// by LiveEnv during the run. Cleared with the results below for the same reason
// - a warning file left over from a previous run would excuse a fresh failure.
const upstreamDir = join(repo, 'app', 'build', 'upstream');
const warningsDir = join(upstreamDir, 'warnings');

const fail = (msg) => { console.error(`GATE FAILED: ${msg}`); process.exit(1); };

if (argv.includes('--allow-skip')) {
  fail('--allow-skip was removed. A skip is a test that did not run, and CI has never accepted one;\n' +
    '  a local flag that excused it made the local gate weaker than the gate that decides merges.\n' +
    '  Enable the environment the test needs (local-test-env.properties) instead of excusing it.');
}

// PREFLIGHT 1: is the live test environment actually ENABLED?
//
// Every environment-gated test in this repo skips silently when its variable is
// unset, and a green suite carrying eleven skips looked exactly like a green
// suite carrying none. That is the same bypass `--allow-skip` was, arrived at by
// omission rather than by flag: run the gate in a worktree whose gitignored
// local-test-env.properties was never provisioned and the live database, the chr
// CLI probes and the testnet probes all quietly sit out.
//
// Real environment variables win (that is CI's path); otherwise the properties
// file must supply them, and the gate refuses to start without them.
const REQUIRED_LIVE_ENV = [
  {
    key: 'CHROMIA_TEST_DATABASE_URL',
    ok: (v) => typeof v === 'string' && v.trim() !== '',
    why: 'the C.UTF-8 PostgreSQL the DB-backed tests need (RunRellTests, LocalChain, the scaffold-to-green runs)'
  },
  {
    key: 'CHROMIA_LIVE_PROVISIONING_TESTS',
    ok: (v) => String(v).trim().toLowerCase() === 'true',
    why: 'the four live testnet probes in TestnetProvisioningLiveTest (network only, no key, nothing spent)'
  },
  {
    key: 'CHROMIA_REQUIRE_CHR',
    ok: (v) => String(v).trim().toLowerCase() === 'true',
    why: 'the real `chr` CLI probes - with this set a missing or broken chr FAILS instead of skipping'
  }
];

const envFile = [
  join(repo, 'local-test-env.properties'),
  join(repo, '..', 'chromia-mcp', 'local-test-env.properties')
].find((f) => existsSync(f)) ?? null;

const fileProps = (() => {
  if (!envFile) return {};
  const props = {};
  for (const line of readFileSync(envFile, 'utf8').split('\n')) {
    const m = line.match(/^\s*([A-Za-z0-9_]+)\s*=\s*(.*)$/);
    if (m && !line.trim().startsWith('#')) props[m[1]] = m[2].trim();
  }
  return props;
})();

const liveEnv = (key) => process.env[key] ?? fileProps[key];

const missingLive = REQUIRED_LIVE_ENV.filter((r) => !r.ok(liveEnv(r.key)));
if (missingLive.length) {
  console.error(`  local-test-env.properties: ${envFile ?? 'NOT FOUND'}`);
  for (const r of missingLive) {
    console.error(`  missing/disabled: ${r.key} - ${r.why}`);
  }
  console.error('  Set these as real environment variables (CI does) or in local-test-env.properties');
  console.error('  (gitignored; `bash scripts/new-lane.sh` writes one per worktree). A gate run that');
  console.error('  silently skips the live tests certifies less than it claims.');
  fail(`the live test environment is not enabled (${missingLive.map((r) => r.key).join(', ')})`);
}
console.log(`gate: live env enabled (${REQUIRED_LIVE_ENV.map((r) => r.key).join(', ')}) via ${envFile ?? 'process environment'}`);

// PREFLIGHT 2: is the database actually up?
//
// A dead PostgreSQL does not look like a dead PostgreSQL from in here - it looks
// like 58 failing tests. That happened on 2026-09-03: the WSL cluster stopped
// mid-run, the suite went red across nine unrelated classes, and the failures
// read as a broken AMM template until every message turned out to be
// "Connection to localhost:5433 refused". Two full 12-minute runs were spent
// before anyone looked at the text. The suite cannot tell infrastructure from
// code, so the gate says it up front - and fails in a second rather than in
// twelve minutes.
const dbUrl = liveEnv('CHROMIA_TEST_DATABASE_URL');

{
  const hp = dbUrl.match(/\/\/([^:/?]+):(\d+)/);
  if (hp) {
    const [, host, port] = hp;
    // Probe MORE THAN ONCE, a few seconds apart. On 2026-09-05 this failed three
    // runs in a row while a hand check seconds earlier said OPEN: the WSL VM boots
    // on every wsl.exe call (postgres auto-starts, the port opens) and shuts down
    // seconds after that call exits, so a single probe measures the instant, not
    // the next twenty minutes. Three probes over ~6 s still cannot prove the VM
    // will stay up, but they do separate a dead database from one that is merely
    // between heartbeats - and the message names the real fix.
    const probe = () => new Promise((resolve) => {
      const sock = connect({ host, port: Number(port) });
      const done = (ok) => { sock.destroy(); resolve(ok); };
      sock.setTimeout(4000);
      sock.on('connect', () => done(true));
      sock.on('timeout', () => done(false));
      sock.on('error', () => done(false));
    });
    const samples = [];
    for (let i = 0; i < 3; i++) {
      samples.push(await probe());
      if (i < 2) await new Promise((r) => setTimeout(r, 3000));
    }
    const reachable = samples.every(Boolean);
    if (!reachable) {
      const flapping = samples.some(Boolean);
      console.error('  Every DB-backed test would fail with "Connection refused" and read as a broken change.');
      console.error('  If it lives in WSL, the VM idles out seconds after the last wsl.exe command and takes');
      console.error('  postgres with it. Hold it up with a resident process BEFORE gating, e.g. in the background:');
      console.error('    wsl.exe -d Ubuntu -u root -- bash -c "service postgresql start; exec sleep infinity"');
      console.error('  then confirm the port stays open over ~45 s, not just once.');
      fail(`the test database at ${host}:${port} is not accepting connections` +
        (flapping ? ' RELIABLY (it answered some probes and not others)' : '') +
        ' - this is INFRASTRUCTURE, not your code');
    }
    console.log(`gate: database at ${host}:${port} is up`);
  }
}

// ---------------------------------------------------------------------------
// DOCS-ONLY MODE: the list is DERIVED, never supplied.
//
// A docs-only commit was once verified by rerunning "the five classes that read
// GOAL.md". `grep -rl GOAL.md app/src/test/kotlin` returns SIX, so the
// hand-picked list was already wrong and nothing in the process could have said
// so. This mode takes no list: it reads the diff, refuses anything that is not
// documentation, and derives the classes by grepping the test sources for the
// changed file names. The derivation is printed and recorded in the gate line,
// so a reader can recompute it.
// ---------------------------------------------------------------------------

/**
 * Documentation, for gate purposes: prose, outside every directory that is
 * compiled, executed or packaged. A .md under app/ (the exploit corpus's
 * CORPUS.md is one) is test DATA, not prose - it runs the full suite.
 */
const CODE_DIRS = ['app/', 'scripts/', 'packages/', 'gradle/', '.github/', 'claude-code-chromia/', 'upstream/'];
const DOC_EXTENSIONS = ['.md', '.mdx', '.txt', '.adoc', '.rst'];
const isDocPath = (p) =>
  !CODE_DIRS.some((d) => p.startsWith(d)) && DOC_EXTENSIONS.some((e) => p.toLowerCase().endsWith(e));

const git = (args) => {
  const r = spawnSync('git', args, { cwd: repo, encoding: 'utf8' });
  if (r.status !== 0) fail(`git ${args.join(' ')} failed: ${(r.stderr ?? '').trim()}`);
  return r.stdout ?? '';
};

/** Every test source file whose text mentions any of [names]; returns class names. */
const testClassesReferencing = (names) => {
  const testRoot = join(repo, 'app', 'src', 'test', 'kotlin');
  const walk = (dir) => readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? walk(join(dir, e.name)) : [join(dir, e.name)]);
  const hits = new Map();
  for (const file of walk(testRoot).filter((f) => f.endsWith('.kt'))) {
    const text = readFileSync(file, 'utf8');
    const matched = names.filter((n) => text.includes(n));
    if (matched.length) hits.set(basename(file, '.kt'), matched);
  }
  return hits;
};

let derivation = null;
let gradleArgs = ['test', '--rerun-tasks'];

if (docsOnly) {
  if (!docsBase) fail('--docs-only needs --base <commit>: the diff is computed against the LAST GATED commit');
  const changed = git(['diff', '--name-only', `${docsBase}..HEAD`]).split('\n').map((s) => s.trim()).filter(Boolean);
  if (changed.length === 0) fail(`nothing changed between ${docsBase} and HEAD - there is nothing to gate`);
  const nonDocs = changed.filter((p) => !isDocPath(p));
  console.log(`gate: --docs-only, base ${docsBase}, ${changed.length} changed path(s)`);
  for (const p of changed) console.log(`  changed: ${p}${isDocPath(p) ? '' : '   <- NOT documentation'}`);
  if (nonDocs.length) {
    fail(`--docs-only refused: ${nonDocs.length} changed path(s) are not documentation (${nonDocs.join(', ')}).\n` +
      '  Run the full gate. Anything under app/, scripts/, packages/, gradle/ or .github/ is code, and so is\n' +
      '  a .md inside them (the exploit corpus is test data that a test reads and scores).');
  }
  // Grep the test sources for BOTH the repo-relative path and the bare file
  // name: tests reference GOAL.md by name and docs/X.md either way.
  const names = [...new Set(changed.flatMap((p) => [p, basename(p)]))];
  const hits = testClassesReferencing(names);
  // ExploitCorpusScoreboardTest always runs: it is the acceptance test for the
  // rules the prose describes, and prose that contradicts a rule is a defect of
  // the same kind as a missing rule (GOAL.md, "the prose is part of the attack
  // surface").
  const classes = [...new Set([...hits.keys(), 'ExploitCorpusScoreboardTest'])].sort();
  console.log(`gate: derived ${classes.length} test class(es) by grepping app/src/test/kotlin for ${names.join(', ')}`);
  for (const c of classes) {
    const why = hits.get(c);
    console.log(`  run: ${c}${why ? ` (references ${why.join(', ')})` : ' (always: the exploit-corpus scoreboard)'}`);
  }
  derivation = { base: docsBase, changed, names, classes };
  gradleArgs = ['test', '--rerun-tasks', ...classes.flatMap((c) => ['--tests', `*${c}*`])];
}

// Clear stale results so a crashed or cached run cannot be mistaken for this one.
if (existsSync(resultsDir)) {
  try { rmSync(resultsDir, { recursive: true, force: true }); }
  catch (e) { fail(`could not clear stale results (${e.code}); another build is probably running - wait for it`); }
}
// Same argument, for the upstream evidence: a warning file from a run three
// days ago would let today's identical failure be counted as somebody else's
// outage. The tally also refuses evidence older than the run, so this is the
// belt to that brace.
if (existsSync(upstreamDir)) {
  try { rmSync(upstreamDir, { recursive: true, force: true }); }
  catch (e) { fail(`could not clear stale upstream evidence (${e.code}); another build is probably running - wait for it`); }
}

const startedAt = Date.now();
console.log(`gate: running ${docsOnly ? 'the derived classes' : 'full suite'} in ${repo} (forced rerun)`);
// --rerun-tasks: Gradle's up-to-date check is the single biggest source of
// "successful" builds that executed nothing.
// A .bat needs a shell on Windows; without one spawn fails silently and the
// gate would report "the suite did not run" without saying it never started it.
const isWin = process.platform === 'win32';
const gradle = isWin
  ? spawnSync('cmd', ['/c', join(repo, 'gradlew.bat'), ...gradleArgs],
      { cwd: repo, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
  : spawnSync(join(repo, 'gradlew'), gradleArgs,
      { cwd: repo, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
if (gradle.error) fail(`could not start gradle: ${gradle.error.message}`);
const out = `${gradle.stdout ?? ''}${gradle.stderr ?? ''}`;
for (const line of out.split('\n')) {
  if (/^e: |BUILD (SUCCESSFUL|FAILED)|tests? completed|OutOfMemory|FAILED$/.test(line)) console.log(`  ${line.trim()}`);
}

// A KILLED task and an ABSENT one look identical from here, and they are not
// the same problem. When Gradle stops `test` for exceeding its own
// `timeout.set(...)`, it removes the result XMLs on the way out - so the gate
// reported "the suite did not run" for a suite that had run 935 tests and was
// killed at 25 minutes. Two cycles went into diagnosing that as a missing run.
// Gradle says exactly what it did; read it back rather than guessing.
const timedOut = /exceeded its configured timeout|Timeout has been exceeded/.test(out);
if (timedOut) {
  const completed = out.match(/(\d+) tests? completed/)?.[1];
  console.error('  the suite was KILLED for exceeding the timeout in app/build.gradle.kts'
    + (completed ? ` after ${completed} test(s)` : '')
    + ' - the results were removed with it, so any tally above is partial.');
  console.error('  This is neither a failing suite nor a missing one. Check WHICH it is before');
  console.error('  raising timeout.set(...): a raised timeout hides a genuine hang perfectly.');
  fail('test task killed by its own timeout');
}
// THE TALLY - one implementation, shared with CI (scripts/gate-tally.mjs).
// It also decides the third status: a failure whose message begins
// `UPSTREAM WARNING (proven): ` AND whose evidence file carries an allowlisted
// signature, a failed canary or a dated docs/UPSTREAM.md entry, and a timestamp
// inside this run, is counted as `upstream=N` and printed by name. It is still
// a failure in the XML - nothing pretends to pass - but it does not set the
// exit code, because it is a red for the THIRD PARTY, not for us. Everything
// else, including a message that claims the status without evidence, is red as
// before.
const t = tally({ resultsDir, warningsDir, startedAt });
if (t.error) fail(t.error);

if (docsOnly) {
  // A filter that matches nothing narrows the run in total silence, which is the
  // failure this whole mode exists to prevent. Every derived class must have
  // produced results.
  const absent = derivation.classes.filter((c) => !t.ranClasses.some((r) => r === c || r.endsWith(`.${c}`)));
  if (absent.length) {
    console.log(gateLine(t));
    fail(`derived class(es) produced no results: ${absent.join(', ')} - the --tests filter did not match them`);
  }
}

if (t.red.length) {
  // If the cluster dies PART WAY through, the preflight above passed and the
  // tally is still meaningless. Name it rather than letting a reader diff a
  // template against 58 unrelated reds.
  const conn = readdirSync(resultsDir).filter((f) => f.endsWith('.xml')).reduce((n, f) => {
    const xml = readFileSync(join(resultsDir, f), 'utf8');
    return n + (xml.match(/Connection (?:to [^"<]*refused|has been closed)/g) ?? []).length;
  }, 0);
  if (conn) {
    console.error(`  NOTE: ${conn} failure(s) are database connection errors - the cluster went down DURING this run.`);
    console.error('  That is infrastructure, not your change. Restart it and re-run before reading anything into these.');
  }
}

if (!report(t, { expectMin: docsOnly ? 0 : expectMin })) {
  process.exit(1);
}
// Gradle exits non-zero on ANY failing test, including a proven upstream
// warning, so its status alone can no longer decide the gate. It still has to
// agree once the warnings are accounted for: a non-zero exit with a clean tally
// and no warnings means something failed that produced no test result at all.
if (gradle.status !== 0 && t.upstream.length === 0) {
  fail(`gradle exited ${gradle.status} despite a clean tally - read the output above`);
}
const tests = t.tests;
// The verdict says the warnings out loud. "PASSED (1534 tests, 0 skips)" over a
// run in which two live claims were never verified would be the same kind of
// sentence this file exists to stop being written.
const upstreamSuffix = t.upstream.length
  ? `, ${t.upstream.length} PROVEN upstream warning(s): ${t.upstream.map((u) => u.key).join(', ')}`
  : '';

if (docsOnly) {
  console.log(
    `gate: PASSED (docs-only, ${tests} tests, 0 skips${upstreamSuffix}) ` +
    `base=${derivation.base} docs=[${derivation.changed.join(' ')}] ` +
    `derived=[${derivation.classes.join(' ')}]`
  );
} else {
  console.log(`gate: PASSED (${tests} tests, 0 skips${upstreamSuffix})`);
}
