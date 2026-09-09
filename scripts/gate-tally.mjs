#!/usr/bin/env node
// THE TALLY, AND THE THIRD STATUS - one implementation, used by every gate.
//
//   node scripts/gate-tally.mjs [--dir <repo>] [--results <dir>] [--warnings <dir>]
//                               [--started-at <epoch-ms>] [--expect-min <n>] [--json]
//
// BOTH OPTIONAL ARGUMENTS ARE DERIVED WHEN THEY ARE NOT GIVEN, and that is the
// round-20 fix rather than a convenience: the run's start comes from the marker
// `:app:test` writes before its first test (RUN_START_MARKER) and the size floor
// comes from the committed `ci/expected-min.json` (EXPECTED_MIN_FILE). No
// workflow passed either flag - `grep -c started-at .github/workflows/*.yml` was
// 0 in all four - so the freshness rule, the stale-results check and the only
// size check beyond `tests === 0` were all inert in the gate that decides
// merges. A flag nobody passes is not a check; a derivation is.
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
//               - a digest of that file INSIDE the failure message, so the file
//                 belongs to the run that failed (see BINDING below),
//               - an INDEPENDENT canary that failed with the SAME signature, or
//                 a DATED docs/UPSTREAM.md entry naming the query - and the
//                 entry is read out of docs/UPSTREAM.md HERE, not believed,
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
// ADVERSARY ROUND 19, section 4, closed three holes in exactly that claim:
//
//   BINDING. Nothing in the repository says only LiveEnv may write into
//   app/build/upstream/warnings, so a hand-written file bought the status. What
//   an attacker cannot hand-write is the JUnit XML: the message attribute is
//   written by the TEST PROCESS, from the throwable the test threw. So
//   LiveEnv now hashes the evidence bytes it just wrote and puts
//   `[evidence sha256:<hex>]` in that message, and this script RECOMPUTES the
//   digest from the file on disk and matches it. A file the failing test did
//   not write cannot match a message it did not produce.
//
//   THE LEDGER. `warningProof` used to accept any non-empty ledgerEntry +
//   ledgerHeading; only LiveEnv.datedLedgerEntry checked the number, on the
//   PRODUCING side, and LiveEnv wrote what it was handed. An entry of `999`
//   with a heading of "999. A heading no document has" was proof. The gate now
//   OPENS docs/UPSTREAM.md itself (see `ledgerProof`): the entry must exist,
//   its heading must match the file's heading for that number exactly, its
//   section must carry a date and must name the query. Independently of the
//   producer, which is the only way a second check is worth anything.
//
//   THE CANARY. `FAILED_OTHER` - the outcome LiveEnv itself documents as "may
//   well be ours" - counted as a failed canary on both sides. It does not any
//   more: only FAILED_SIGNATURE, carrying the SAME allowlisted signature as the
//   tool failure, measured on the canary's INDEPENDENT path (a plain
//   java.net.http client with its own bounds - LiveEnv.probeExplorer), is proof
//   that the third party is down. Two of the four allowlisted signatures
//   (`explorer-request-timeout`, `explorer-http-5xx`) are reachable from our own
//   code; the independent path is what stops one fault of ours from satisfying
//   the signature and the canary at once. docs/UPSTREAM.md, "The
//   upstream-warning contract", is the long form.
//
// The failure mode this whole shape is designed against is adversary round 18
// section 4: `get_asset_top_holders` answered eight consecutive live
// INTERNAL_ERRORs and the suite reported eight PASSES, because an upstream
// marker in the text was treated as evidence. A warning is a RED FOR THE
// UPSTREAM, not for us - it is never a green.

import { readdirSync, readFileSync, statSync, existsSync } from 'node:fs';
import { join, resolve, basename } from 'node:path';
import { pathToFileURL } from 'node:url';
import { createHash } from 'node:crypto';

/** The prefix LiveEnv.UPSTREAM_WARNING_PREFIX writes. Kept identical on purpose. */
export const UPSTREAM_WARNING_PREFIX = 'UPSTREAM WARNING (proven): ';

/**
 * The marker, as it actually reaches an XML `message` attribute.
 *
 * Two writers produce these files and they do NOT agree. JUnit's own
 * LegacyXmlReportGeneratingListener writes the throwable's message verbatim;
 * GRADLE writes `java.lang.AssertionError: <message>`, class name and all. A
 * plain startsWith() therefore matched the nested run and missed every real
 * suite run - measured 2026-09-08 on both artifacts, which is the reason
 * UpstreamWarningGateTest runs the real reporter instead of asserting against a
 * <testsuite> string this file wrote.
 *
 * One optional `some.Exception: ` prefix is allowed and nothing else: the marker
 * still has to be at the START of what the test said, so a message that merely
 * MENTIONS the phrase further along cannot claim the status.
 */
const MARKER = new RegExp(
  `^(?:[\\w.$]+(?:Error|Exception|Failure)[\\w.$]*:\\s*)?${
    UPSTREAM_WARNING_PREFIX.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`
);

/** Does [message] claim the third status, in either writer's spelling? */
export const claimsUpstreamWarning = (message) => MARKER.test(String(message ?? ''));

/**
 * `liveFooBar()` in a Gradle XML, `liveFooBar` from LiveEnv and from the legacy
 * reporter. The evidence file is keyed by the METHOD, so drop the argument list
 * (and a parameterized invocation's `[1]` suffix with it).
 */
export const methodKey = (testcaseName) => String(testcaseName ?? '').replace(/\(.*$/, '');

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
 * THE RUN'S START, DERIVED - never defaulted, and never a flag somebody has to
 * remember.
 *
 * ADVERSARY ROUND 20, section 4, found the freshness rule inert everywhere it
 * actually runs. It used to read:
 *
 *     const runStart = startedAt ?? (Math.min(...mtimes) - TEST_TASK_TIMEOUT_MS);
 *
 * where `startedAt` was the `--started-at` flag and TEST_TASK_TIMEOUT_MS was the
 * test task's own 90-minute timeout. `grep -c started-at .github/workflows/*.yml`
 * was 0 in all four, so in CI the window was "ninety minutes before the oldest
 * result file": an evidence file written an HOUR before the run was accepted as
 * a proven upstream outage. And `stale` - the check report() calls fatal, "you
 * are reading someone else's evidence" - was computed only
 * `if (startedAt !== null)`, so it could not fire in CI at all.
 *
 * The remedy is not to pass the flag in four more places. A gate armed by
 * remembering an argument is a gate disarmed by forgetting one, which is the
 * removed `--allow-skip` arrived at by omission. So the start is DERIVED from
 * the run itself:
 *
 *   app/build/test-run/starts.tsv, one row per `:app:test` invocation, written
 *   by the task's own doFirst BEFORE the first test executes (see
 *   app/build.gradle.kts). Each row is `<epoch-ms>\t<ISO-8601>\t<task path>`.
 *   The task TRUNCATES that file when the results directory holds no XML (a new
 *   accumulation begins) and APPENDS otherwise, so a partitioned gate - thirteen
 *   serial `--tests` slices into one results directory on the laptop - records
 *   every slice, and the run's start is the EARLIEST of them.
 *
 * A caller may still pass `--started-at`, and it can only NARROW the window: the
 * run start is `max(marker, flag)`. Nothing a caller says can make the gate
 * accept evidence older than the test task itself recorded.
 *
 * With no marker AND no flag the tally FAILS CLOSED. That is deliberate: no
 * marker means no `:app:test` execution wrote one in this tree, which is either
 * "the suite did not run" or "the task was up to date and executed nothing" -
 * both of them fake greens this file exists to refuse.
 */
export const RUN_START_MARKER = join('app', 'build', 'test-run', 'starts.tsv');

/**
 * The rows of [RUN_START_MARKER] under [repoDir], and the run's start: the
 * EARLIEST invocation recorded there. `at` is null when the file is absent or
 * holds no usable row; every caller in this file fails closed on that.
 */
export function runStartFromMarker(repoDir) {
  const path = join(repoDir ?? process.cwd(), RUN_START_MARKER);
  if (!existsSync(path)) return { at: null, rows: [], path, why: `no start marker at ${path}` };
  const rows = readFileSync(path, 'utf8').split('\n')
    .map((line) => line.trim()).filter(Boolean)
    .map((line) => {
      const [epoch, iso, task] = line.split('\t');
      return { at: Number(epoch), iso: iso ?? '', task: task ?? '' };
    })
    .filter((r) => Number.isFinite(r.at) && r.at > 0);
  if (rows.length === 0) {
    return { at: null, rows, path, why: `the start marker at ${path} holds no usable row` };
  }
  return { at: Math.min(...rows.map((r) => r.at)), rows, path, why: null };
}

/**
 * THE FLOOR, COMMITTED - the other flag CI never passed (round 20, section 5).
 *
 * `--expect-min` is the gate's only size check beyond `tests === 0`, and no
 * workflow passed it either, so a suite narrowed from 1650 tests to ONE - by a
 * `--tests` filter that matched almost nothing, or by a class that failed to
 * compile - was a green build. `docs/ADVERSARY-ROUND-BRIEF.md` told a reviewer
 * to supply the number BY HAND, which is the same check nobody makes twice.
 *
 * The floor is now READ, out of a committed `ci/expected-min.json`. The two
 * candidates were weighed and this is why this one won:
 *
 *   the published `test-report` ARTIFACT of the last green run is authoritative,
 *   but it EXPIRES (7 days' retention), it needs the API and a token, a fork's
 *   first pull request has no previous run to read, and a fortnight's quiet
 *   would silently disarm the check. A floor that can evaporate is a floor that
 *   will.
 *
 *   a COMMITTED FILE is in every checkout including a fork's, never expires,
 *   moves only in a reviewable diff, and is written by the merge gate itself -
 *   `scripts/loop-gate.mjs` ratchets it UP after a green FULL run - so it is not
 *   a number a human has to remember either.
 *
 * The committed file wins on the property that decides everything else in this
 * file: it cannot be forgotten, because reading it is not optional and raising
 * it is not a human step.
 */
export const EXPECTED_MIN_FILE = join('ci', 'expected-min.json');

/**
 * The committed floor under [repoDir], or 0 when there is none. Returns the
 * whole record, because a number with no provenance is the estimate this
 * replaces.
 */
export function expectedMinFloor(repoDir) {
  const path = join(repoDir ?? process.cwd(), EXPECTED_MIN_FILE);
  if (!existsSync(path)) return { floor: 0, path, source: 'none', record: null };
  try {
    const record = JSON.parse(readFileSync(path, 'utf8'));
    const floor = Number(record?.expectMin);
    if (!Number.isFinite(floor) || floor < 0) return { floor: 0, path, source: 'unusable', record };
    return { floor: Math.trunc(floor), path, source: 'committed', record };
  } catch (e) {
    return { floor: 0, path, source: `unreadable: ${e.message}`, record: null };
  }
}

/**
 * The results directory a repository's own suite writes to. The committed floor
 * is about THIS suite, so it is applied automatically only when the tally is
 * reading that directory; a caller pointing `--results` somewhere else (the
 * nested runs in UpstreamWarningGateTest, a probe harness) is classifying
 * something that is not the suite, and has to say `--expect-min` to get a size
 * check.
 */
export const defaultResultsDir = (repoDir) =>
  join(repoDir ?? process.cwd(), 'app', 'build', 'test-results', 'test');

const decode = (s) => String(s ?? '')
  .replace(/&lt;/g, '<').replace(/&gt;/g, '>')
  .replace(/&quot;/g, '"').replace(/&apos;/g, "'")
  .replace(/&#10;/g, '\n').replace(/&#13;/g, '\r').replace(/&#9;/g, '\t')
  .replace(/&amp;/g, '&');

/**
 * The digest a failing test writes into its OWN assertion message to bind the
 * evidence file to this run - `[evidence sha256:<64 hex>]`, written by
 * LiveEnv.upstreamOutage over the exact bytes it wrote to disk.
 *
 * The XML message attribute is produced by the test process from the throwable
 * the test threw; the evidence file is an ordinary file anyone can write. This
 * is the join between them, and it is the round-19 a2 fix: a hand-written file
 * cannot match a message it did not produce.
 */
export const EVIDENCE_DIGEST = /\[evidence sha256:([0-9a-f]{64})\]/;

/** The evidence file's content digest, in the spelling the message carries. */
const sha256 = (bytes) => createHash('sha256').update(bytes).digest('hex');

/** Every evidence file in [dir], keyed `Class.method`, with its content digest. */
export function readWarnings(dir) {
  const found = new Map();
  if (!dir || !existsSync(dir)) return found;
  for (const file of readdirSync(dir).filter((f) => f.endsWith('.json'))) {
    const path = join(dir, file);
    let parsed = null;
    let error = null;
    let digest = null;
    try {
      // Bytes, not a decoded string: the digest has to be of what is ON DISK,
      // because that is what the producing test hashed.
      const bytes = readFileSync(path);
      digest = sha256(bytes);
      parsed = JSON.parse(bytes.toString('utf8'));
    } catch (e) { error = `unreadable: ${e.message}`; }
    found.set(basename(file, '.json'), { file: path, warning: parsed, error, sha256: digest });
  }
  return found;
}

const escapeRe = (s) => String(s).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

/**
 * THE LEDGER, READ OUT OF docs/UPSTREAM.md BY THE GATE.
 *
 * A partial outage - the explorer answers, one query does not - is excused by a
 * DATED entry naming that query, or not at all. Round 19 (a1) showed the gate
 * taking the producer's word for all of it: any non-empty `ledgerEntry` and
 * `ledgerHeading` passed, and `999` / "999. A heading no document has" bought
 * the status. So this opens the file, in [repoDir], and requires four things:
 *
 *  1. the entry EXISTS as `## <entry>. <heading>`;
 *  2. the heading in the file matches the heading in the evidence EXACTLY, so
 *     evidence cannot cite a real number under a story of its own;
 *  3. the entry's section carries a DATE - an undated entry cannot be aged out
 *     and would excuse a query forever;
 *  4. the section NAMES the query being excused - an entry about something else
 *     is evidence about something else.
 *
 * 3 and 4 are the rule LiveEnv.datedLedgerEntry applies on the producing side,
 * re-derived here from the same file. Two checks of the same fact are only
 * worth one unless they are independent, which is why this is a second reader
 * of docs/UPSTREAM.md rather than a second look at the evidence JSON.
 */
export function ledgerProof(repoDir, entry, heading, query) {
  const path = join(repoDir ?? process.cwd(), 'docs', 'UPSTREAM.md');
  if (!existsSync(path)) {
    return { ok: false, why: `there is no docs/UPSTREAM.md at ${path} to check entry #${entry} against` };
  }
  const text = readFileSync(path, 'utf8');
  const found = new RegExp(`^##\\s+${escapeRe(entry)}\\.\\s+(.*)$`, 'm').exec(text);
  if (!found) {
    return { ok: false, why: `docs/UPSTREAM.md has no entry \`## ${entry}.\` - a ledger entry that is not written down excuses nothing` };
  }
  const fileHeading = found[1].trim();
  const claimedHeading = String(heading ?? '').trim();
  if (fileHeading !== claimedHeading) {
    return {
      ok: false,
      why: `evidence calls docs/UPSTREAM.md #${entry} ${JSON.stringify(claimedHeading)}, the file calls it ` +
        `${JSON.stringify(fileHeading)}`,
    };
  }
  const rest = text.slice(found.index + found[0].length);
  const end = rest.search(/^##\s/m);
  const section = found[1] + (end < 0 ? rest : rest.slice(0, end));
  if (!/\b20\d\d-\d\d-\d\d\b/.test(section)) {
    return { ok: false, why: `docs/UPSTREAM.md #${entry} carries no date - an undated entry would excuse this query forever` };
  }
  if (!query || !section.includes(query)) {
    return { ok: false, why: `docs/UPSTREAM.md #${entry} does not name \`${query}\` - an entry only excuses the query it names` };
  }
  return { ok: true, why: `docs/UPSTREAM.md #${entry}: ${fileHeading}` };
}

/**
 * Is this evidence file actually proof, for a run that started at [startedAt]?
 * Returns { ok, why } - `why` explains a refusal in the operator's terms.
 *
 * [message] is the failing test's own assertion message, out of the JUnit XML;
 * [repoDir] is the checkout whose docs/UPSTREAM.md is the ledger. Both are
 * required for the round-19 checks and both fail CLOSED when absent.
 */
export function warningProof(entry, key, startedAt, { message = '', repoDir = process.cwd() } = {}) {
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

  // BINDING (round 19, a2). The failing test hashed the bytes it wrote and put
  // the digest in the message the reporter copied into the XML. Recompute it.
  const bound = EVIDENCE_DIGEST.exec(String(message ?? ''));
  if (!bound) {
    return {
      ok: false,
      why: 'the failure message carries no `[evidence sha256:<hex>]`, so nothing ties this file to ' +
        'the test that failed - only LiveEnv.upstreamOutage writes that digest, and it writes it ' +
        'over the bytes it just wrote',
    };
  }
  if (bound[1] !== entry.sha256) {
    return {
      ok: false,
      why: `the failure message binds evidence sha256:${bound[1]} but ${basename(entry.file)} hashes to ` +
        `sha256:${entry.sha256} - this file is not the one the failing test wrote`,
    };
  }

  // The guardrail: the explorer is down for everything, PROVEN on the canary's
  // independent path with the same signature, or this one query is written down
  // as broken, with a date, in docs/UPSTREAM.md. Nothing else excuses a live
  // failure.
  //
  // FAILED_OTHER is deliberately NOT a failed canary (round 19, a2): LiveEnv
  // documents that state as "it refused with something else - which may well be
  // ours", and an outcome that may well be ours proves nothing about the third
  // party. Neither is a FAILED_SIGNATURE carrying a DIFFERENT signature from the
  // tool failure: two unrelated faults are not one outage.
  const canary = w.canary && typeof w.canary === 'object' ? w.canary : null;
  const canaryProves = w.canaryOutcome === 'FAILED_SIGNATURE' &&
    canary !== null && canary.independent === true &&
    UPSTREAM_SIGNATURE_NAMES.includes(canary.signature) &&
    canary.signature === w.signature;
  const canaryWhy = w.canaryOutcome === 'ANSWERED'
    ? 'the canary ANSWERED, so the explorer is up'
    : w.canaryOutcome === 'FAILED_OTHER'
      ? 'the canary failed with FAILED_OTHER, which LiveEnv itself documents as "may well be ours"'
      : canary === null || canary.independent !== true
        ? 'the canary in this evidence was not measured on the independent path'
        : canary.signature !== w.signature
          ? `the canary failed with [${canary.signature}] and the tool failed with [${w.signature}] - ` +
            'two different faults are not one outage'
          : `the canary outcome ${JSON.stringify(w.canaryOutcome)} is not proof of an outage`;
  const named = typeof w.ledgerEntry === 'string' && w.ledgerEntry.trim() !== '';
  const ledger = named
    ? ledgerProof(repoDir, w.ledgerEntry.trim(), w.ledgerHeading, w.query)
    : { ok: false, why: `no docs/UPSTREAM.md entry is named for \`${w.query}\`` };
  if (!canaryProves && !ledger.ok) {
    return {
      ok: false,
      why: `${canaryWhy}, and ${ledger.why} - a partial outage is excused by a dated ledger entry ` +
        'that names the query, or not at all',
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
  return {
    ok: true,
    why: canaryProves ? `independent canary FAILED_SIGNATURE [${canary.signature}]` : ledger.why,
  };
}

/**
 * Reads the JUnit XMLs in [resultsDir] and the evidence in [warningsDir] and
 * classifies every non-passing test into RED or UPSTREAM.
 */
export function tally({ resultsDir, warningsDir, startedAt = null, repoDir = process.cwd() }) {
  if (!existsSync(resultsDir)) {
    return { error: `no test-results directory at ${resultsDir} - the suite did not run` };
  }
  const files = readdirSync(resultsDir).filter((f) => f.endsWith('.xml'));
  if (files.length === 0) {
    return { error: 'no result files - the suite did not run (a fast "BUILD SUCCESSFUL" means a cached task)' };
  }

  // THE RUN'S START, DERIVED (round 20, section 4). The marker the test task
  // wrote before its first test, narrowed by whatever the caller passed - never
  // widened by it, and never defaulted to "ninety minutes ago".
  const marker = runStartFromMarker(repoDir);
  const bounds = [marker.at, startedAt].filter((v) => typeof v === 'number' && Number.isFinite(v));
  if (bounds.length === 0) {
    return {
      error: `${marker.why} and no --started-at was given, so this tally cannot date its own run. ` +
        'The marker is written by `:app:test`\'s doFirst before the first test executes; its absence ' +
        'means the suite did not run in this tree, or the task was up to date and executed nothing. ' +
        'The freshness rule and the stale-results check are both measured against it, so classifying ' +
        'without one would certify evidence of unknown age',
    };
  }
  const runStart = Math.max(...bounds);
  const runStartSource = marker.at === null
    ? '--started-at (no marker; a caller-supplied start)'
    : runStart === marker.at
      ? `${RUN_START_MARKER} (${marker.rows.length} invocation(s), earliest ` +
        `${marker.rows.find((r) => r.at === marker.at)?.iso ?? new Date(marker.at).toISOString()})`
      : `--started-at, which NARROWED the marker's ${new Date(marker.at).toISOString()}`;
  const warnings = readWarnings(warningsDir);

  let tests = 0, failures = 0, errors = 0, skipped = 0;
  const skippedNames = [], upstream = [], red = [], stale = [], ranClasses = new Set();

  for (const f of files) {
    const path = join(resultsDir, f);
    // ALWAYS computed now. It used to be guarded by `startedAt !== null`, and
    // no workflow ever passed one, so the fatal "you are reading someone else's
    // evidence" check was dead in CI (round 20, section 4).
    if (statSync(path).mtimeMs < runStart) stale.push(f);
    const xml = readFileSync(path, 'utf8');
    const suite = xml.match(/<testsuite\b[^>]*>/)?.[0] ?? '';
    const num = (attr) => Number(suite.match(new RegExp(`${attr}="(\\d+)"`))?.[1] ?? 0);
    tests += num('tests'); failures += num('failures'); errors += num('errors'); skipped += num('skipped');
    const cls = (suite.match(/name="([^"]+)"/)?.[1] ?? f).replace(/^org\.chromia\./, '');
    ranClasses.add(cls.replace(/\$.*$/, ''));

    for (const tc of xml.split('<testcase').slice(1)) {
      const name = tc.match(/name="([^"]+)"/)?.[1] ?? '?';
      // The TESTCASE's own classname, not the suite's. Gradle names the suite
      // after the class, but JUnit's LegacyXmlReportGeneratingListener names it
      // after the ENGINE ("JUnit Jupiter") when the plan is a single selected
      // method - so a key built from the suite came out as
      // `JUnit Jupiter.liveFooBar` and matched no evidence file. Both writers
      // put the real class on the testcase. Measured 2026-09-08 on both.
      const owner = (tc.match(/classname="([^"]+)"/)?.[1] ?? cls).replace(/^org\.chromia\./, '');
      if (/<skipped\b/.test(tc)) { skippedNames.push(`${owner}::${name}`); continue; }
      const problem = tc.match(/<(failure|error)\b[^>]*\bmessage="([^"]*)"/);
      if (!problem) continue;
      const [, kind, rawMessage] = problem;
      const message = decode(rawMessage);
      const key = `${owner.replace(/\$.*$/, '')}.${methodKey(name)}`;
      if (!claimsUpstreamWarning(message)) {
        red.push({ key, kind, message, why: null });
        continue;
      }
      const proof = warningProof(warnings.get(key), key, runStart, { message, repoDir });
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
    resultsDir, warningsDir, repoDir, files: files.length,
    runStart, runStartSource, runStartMarker: marker.path, runStartInvocations: marker.rows,
    tests, failures, errors, skipped,
    skippedNames, upstream, red, stale, orphanWarnings,
    ranClasses: [...ranClasses].sort(),
  };
}

/**
 * THE VERDICT OBJECT - every condition the gate fails on, and its state.
 *
 * ADVERSARY ROUND 20, section 5: `scripts/ci-summary.mjs` opens "A PRESENTER,
 * NEVER A VERDICT" and is right that it does not set the exit code, but the
 * first line it rendered was computed from TWO of the five conditions
 * report() fails on:
 *
 *     const ours = t.red.length;
 *     const verdict = ours || t.skippedNames.length ? 'RED ...' : ...
 *
 * so a run the gate failed on stale results, on `tests === 0`, or on the size
 * floor was headed `## CI gate: GREEN` - with, in the stale case, "You are
 * reading an earlier run's evidence. This is fatal." twenty lines below the
 * word GREEN. Measured through both real scripts over five result directories
 * (exploit-corpus/realworld/adversary-round20/ci): three of five disagreed.
 *
 * Two renderings of "what is a pass" is one gate and one bypass, exactly as two
 * classifiers would be. So there is now ONE statement of the conditions, here,
 * and everything downstream reads it: report() prints them and sets the exit
 * code from `green`, the CLI exits on the same field in --json mode as well, and
 * the presenter renders `headline` verbatim. The summary CANNOT disagree with
 * the exit code, because it is not computing anything.
 *
 * [t] may be an errored tally (`{ error }`); that is the sixth condition and the
 * one an operator most needs named, because a missing results directory reads as
 * "0 failures" to anything that counts rather than checks.
 */
export function verdict(t, { expectMin = 0 } = {}) {
  const conditions = t.error
    ? [{
      condition: 'the suite ran at all',
      failed: true,
      detail: t.error,
      fatal: `GATE FAILED: ${t.error}`,
    }]
    : [
      {
        condition: 'every result file belongs to this run',
        failed: t.stale.length > 0,
        detail: t.stale.length
          ? `${t.stale.length} of ${t.files} result file(s) predate ${new Date(t.runStart).toISOString()}` +
            ` (run start from ${t.runStartSource})`
          : `${t.files} result file(s), all newer than ${new Date(t.runStart).toISOString()}`,
        fatal: t.stale.length
          ? `GATE FAILED: ${t.stale.length} result file(s) predate this run (e.g. ${t.stale[0]}) - ` +
            'you are reading someone else\'s evidence'
          : null,
      },
      {
        condition: 'the suite recorded tests',
        failed: t.tests === 0,
        detail: `tests=${t.tests}`,
        fatal: t.tests === 0 ? 'GATE FAILED: zero tests recorded' : null,
      },
      {
        condition: 'the suite was not narrowed below the floor',
        failed: Boolean(expectMin) && t.tests < expectMin,
        detail: expectMin
          ? `${t.tests} ran, floor ${expectMin}`
          : 'no floor in force (no ci/expected-min.json and no --expect-min)',
        fatal: expectMin && t.tests < expectMin
          ? `GATE FAILED: only ${t.tests} tests ran, expected at least ${expectMin} - a filter or a ` +
            'compile failure silently narrowed the suite'
          : null,
      },
      {
        condition: 'no failure or error is ours',
        failed: t.red.length > 0,
        detail: t.red.length ? t.red.map((r) => r.key).join(', ') : 'none',
        fatal: t.red.length ? `GATE FAILED: ${t.red.length} failure(s)/error(s) that are OURS:` : null,
      },
      {
        condition: 'nothing skipped',
        failed: t.skippedNames.length > 0,
        detail: t.skippedNames.length ? t.skippedNames.join(', ') : 'none',
        fatal: t.skippedNames.length
          ? 'GATE FAILED: skipped test(s) - a skip is a test that did not run, and there is no ' +
            `allowlist:\n  ${t.skippedNames.join('\n  ')}`
          : null,
      },
    ];
  const green = !conditions.some((c) => c.failed);
  const headline = t.error
    ? 'THE SUITE DID NOT RUN'
    : !green
      ? 'RED - this build does not ship'
      : t.upstream.length
        ? 'GREEN for us, with a proven upstream outage - nothing those tests name was verified'
        : 'GREEN';
  return { green, headline, expectMin, conditions, failed: conditions.filter((c) => c.failed) };
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
    log(`    canary: ${u.canary?.outcome ?? u.canaryOutcome}` +
      (u.canary?.independent ? ' (independent path)' : '') +
      ` at ${u.canary?.at ?? '?'}` +
      (u.canary?.explorerSaid ? ` - ${String(u.canary.explorerSaid).slice(0, 120)}` : ''));
    log(`  upstream said: ${String(u.errorText).replace(/\s+/g, ' ').slice(0, 200)}`);
    log(`  evidence: app/build/upstream/warnings/${u.key}.json`);
  }
  for (const s of t.skippedNames) log(`  skip: ${s}`);
  for (const r of t.red) log(`  FAILED: ${r.key}${r.why ? ` <- ${r.why}` : ''}`);
  for (const o of t.orphanWarnings) {
    log(`  NOTE: evidence file ${o}.json matches no UPSTREAM WARNING failure in these results`);
  }

  // THE VERDICT, from the one statement of the conditions (round 20, section 5).
  // report() no longer decides anything either - it prints what verdict() says,
  // in the same wording it always used, and so does the job summary. A presenter
  // and a gate that agree by construction cannot disagree by drift.
  const v = verdict(t, { expectMin });
  log(`gate: run started ${new Date(t.runStart).toISOString()} - derived from ${t.runStartSource}`);
  for (const c of v.conditions) {
    if (!c.failed) continue;
    err(c.fatal);
    if (c.condition === 'no failure or error is ours') {
      for (const r of t.red) err(`  ${r.key}${r.why ? ` <- ${r.why}` : ''}`);
    }
  }
  const ok = v.green;
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
// pathToFileURL, not string surgery. The hand-rolled version of this line
// produced `file://C:/...` on Windows against an import.meta.url of
// `file:///C:/...`, so the CLI silently did NOTHING and exited 0 - a gate that
// certifies every run by not running is the exact failure this file exists to
// prevent, and it was invisible until UpstreamWarningGateTest spawned it for
// real and found empty stdout. Caught 2026-09-08.
const isMain = process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href;
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
    resultsDir, warningsDir, repoDir: repo,
    startedAt: startedAtArg === null ? null : Number(startedAtArg),
  });
  // THE FLOOR, RESOLVED THE SAME WAY FOR EVERY CALLER (round 20, section 5).
  // An explicit --expect-min wins; otherwise the committed ci/expected-min.json
  // arms the check automatically, but only when this is the repository's OWN
  // results directory - a caller pointing --results at a nested run's temp
  // directory is not classifying the suite, and a floor of 1650 against its one
  // test would be a red about nothing.
  const expectMinArg = opt('--expect-min', null);
  const floor = expectedMinFloor(repo);
  const ownResults = resultsDir === resolve(defaultResultsDir(repo));
  const expectMin = expectMinArg !== null
    ? Number(expectMinArg)
    : (ownResults ? floor.floor : 0);
  if (t.error) { console.error(`GATE FAILED: ${t.error}`); process.exit(1); }
  const v = verdict(t, { expectMin });
  // --json exits on the SAME verdict. It used to exit on
  // `t.red.length || t.skippedNames.length` - two of the five conditions, the
  // very split round 20 found in the presenter, in the mode the probe harnesses
  // and the nested-run tests use.
  if (argv.includes('--json')) {
    console.log(JSON.stringify({ ...t, verdict: v }, null, 2));
    process.exit(v.green ? 0 : 1);
  }
  if (expectMin) {
    console.log(`gate: floor ${expectMin} test(s), from ` +
      (expectMinArg !== null ? '--expect-min' : `${EXPECTED_MIN_FILE} (${floor.record?.verifiedBy ?? 'no provenance'})`));
  } else if (ownResults) {
    console.log(`gate: NO SIZE FLOOR - ${floor.path} is ${floor.source}. A suite narrowed to one ` +
      'test would pass everything but the zero-tests check');
  }
  const ok = report(t, { expectMin });
  process.exit(ok ? 0 : 1);
}
