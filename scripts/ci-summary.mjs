#!/usr/bin/env node
// THE JOB SUMMARY. A PRESENTER, NEVER A VERDICT.
//
//   node scripts/ci-summary.mjs [--dir <repo>] [--out <file>]
//
// Writes the run's tally as Markdown into `$GITHUB_STEP_SUMMARY`, which GitHub
// renders on the run page itself. The point is narrow and worth stating: an
// operator looking at a red CI run should learn WHICH tests failed, WHICH
// upstream outages were excused and under which ledger entry, and whether
// anything skipped, WITHOUT opening a 40-minute log and scrolling for it.
//
// Before this existed, the tally was printed by `scripts/gate-tally.mjs` to
// stdout and stderr - and GitHub interleaves those two streams by arrival, not
// by order, so the one run in the last twelve that failed on classification
// (34272194244, 2026-09-08) rendered its verdict like this:
//
//     GATE FAILED: 1 failure(s)/error(s) that are OURS:
//         canary: ANSWERED at 2026-09-08T20:18:34Z - totalRewardsPaid = ...
//       Round17SecurityRuleProbeTest.record what rell_security_check says ...
//       upstream said: Failed to get all blockchains: GraphQL Error: ...
//
// - the failing test's name landed between two lines belonging to a DIFFERENT
// test's upstream evidence. That is a log an operator has to decode. This file
// renders the same data structure into a table nobody has to decode.
//
// WHAT THIS FILE MAY NOT DO, and the reason:
//
//   1. It MUST NOT classify. `scripts/gate-tally.mjs` is the single classifier
//      for the local merge gate and for CI - two implementations of "what is a
//      pass" is one gate and one bypass, which is the mistake `--allow-skip`
//      was. So this imports `tally` from that file and only formats what it
//      returns. Every number below is the gate's own number.
//
//      IT MUST NOT COMPUTE A VERDICT WORD EITHER, and until adversary round 20
//      it did. The headline was
//
//          const ours = t.red.length;
//          const verdict = ours || t.skippedNames.length ? 'RED ...' : ... 'GREEN';
//
//      - TWO of the five conditions `report()` fails on. A run the gate failed
//      on stale results, on `tests === 0`, or on the size floor was therefore
//      headed `## CI gate: GREEN`; in the stale case this file printed GREEN at
//      the top and, twenty lines below it, "You are reading an earlier run's
//      evidence. This is fatal." Measured through both real scripts over five
//      result directories: three of five disagreed
//      (exploit-corpus/realworld/adversary-round20/ci).
//
//      A second RENDERING of what is a pass is the same defect as a second
//      classifier, so the headline is now `verdict(t, { expectMin }).headline`
//      out of gate-tally.mjs - the identical object the exit code is taken from.
//      The summary cannot disagree with the verdict, because it no longer has an
//      opinion to disagree with.
//
//   2. It MUST NOT decide the exit code. It always exits 0, deliberately: the
//      verdict belongs to the `Classify the tally` step, which runs
//      `gate-tally.mjs` for real and reds the build. A presenter that could
//      fail the build would be a second gate with different rules (see 1), and
//      a presenter that could PASS the build would be worse. If this file
//      throws, the run still goes red on the step that matters and the operator
//      loses a table, not a verdict.
//
// It is therefore run with `if: always()`, before the classification step, so
// the summary exists whatever happens after it.

import { existsSync, writeFileSync, appendFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { pathToFileURL } from 'node:url';
import { tally, verdict, expectedMinFloor, defaultResultsDir, EXPECTED_MIN_FILE } from './gate-tally.mjs';

/** GitHub renders `|` as a column separator and `\n` ends the row. */
const cell = (s) => String(s ?? '').replace(/\|/g, '\\|').replace(/[\r\n]+/g, ' ').trim();

/** Long third-party prose in a table cell is unreadable; in a details block it is not. */
const clip = (s, n) => {
  const flat = String(s ?? '').replace(/\s+/g, ' ').trim();
  return flat.length > n ? `${flat.slice(0, n)}…` : flat;
};

/**
 * The Markdown for [t], a `tally()` result. Pure - it reads nothing and writes
 * nothing - so the shape can be asserted from a test without a CI run.
 */
export function summarize(t, { runUrl = null, expectMin = 0 } = {}) {
  const out = [];
  const say = (line = '') => out.push(line);

  if (t.error) {
    // The suite did not run at all. This is the case the operator most needs
    // named, because a missing result directory reads as "0 failures" to
    // anything that counts rather than checks.
    say(`## CI gate: ${verdict(t, { expectMin }).headline}`);
    say('');
    say(`\`${t.error}\``);
    say('');
    say('A missing or empty results directory is **not** a green suite. Look at the');
    say('`Run the unit suite` step: gradle either failed before any test executed, or the');
    say('`test` task was up to date and executed nothing.');
    return `${out.join('\n')}\n`;
  }

  const ours = t.red.length;
  // THE HEADLINE IS THE GATE'S OWN, not a second opinion computed here from two
  // of its five conditions (round 20, section 5). Same object, same expectMin,
  // same `green` field the process exits on.
  const v = verdict(t, { expectMin });

  say(`## CI gate: ${v.headline}`);
  say('');
  say('| the gate fails on | this run | |');
  say('| --- | --- | :-: |');
  for (const c of v.conditions) {
    say(`| ${cell(c.condition)} | ${cell(clip(c.detail, 160))} | ${c.failed ? '**RED**' : 'ok'} |`);
  }
  say('');
  say('Those five are `verdict()` in `scripts/gate-tally.mjs`, and the headline above is its');
  say('`headline` field - the same object the `Classify the tally` step exits on. This table is');
  say('here because the headline used to be computed from two of the five, so a run that failed on');
  say('stale results, on zero tests, or on the size floor was headed GREEN.');
  say('');
  say('| | count | meaning |');
  say('| --- | ---: | --- |');
  say(`| tests | ${t.tests} | executed, across ${t.files} result file(s) |`);
  say(`| **ours** | ${ours} | failures and errors that are this repository's. **Any is fatal.** |`);
  say(`| **upstream** | ${t.upstream.length} | failures PROVEN to be a third party's. Counted, named, never a pass. |`);
  say(`| **skipped** | ${t.skippedNames.length} | a skip is a test that did not run. **There is no allowlist; any is fatal.** |`);
  say(`| errors | ${t.errors} | included in *ours* above unless proven upstream |`);
  say('');

  if (ours) {
    say(`### ${ours} failure(s) that are OURS`);
    say('');
    say('| test | why |');
    say('| --- | --- |');
    for (const r of t.red) {
      say(`| \`${cell(r.key)}\` | ${cell(r.why ?? clip(r.message, 220))} |`);
    }
    say('');
    say('A `claims UPSTREAM WARNING but …` reason means the test *said* a third party was');
    say('down and could not prove it. That is an ordinary red, on purpose: the gate fails');
    say('closed. See `docs/UPSTREAM.md`, "The upstream-warning contract".');
    say('');
  }

  if (t.skippedNames.length) {
    say(`### ${t.skippedNames.length} SKIPPED test(s)`);
    say('');
    say('A skip is a test that silently did not run, so the suite covered less than it');
    say('claimed. Provision what the test needs; do not excuse it.');
    say('');
    for (const s of t.skippedNames) say(`- \`${s}\``);
    say('');
  }

  if (t.upstream.length) {
    say(`### ${t.upstream.length} PROVEN upstream outage(s)`);
    say('');
    say('| test | tool | query | signature | ledger entry | proven by |');
    say('| --- | --- | --- | --- | --- | --- |');
    for (const u of t.upstream) {
      const ledger = u.ledgerEntry
        ? `\`docs/UPSTREAM.md\` #${cell(u.ledgerEntry)} — ${cell(u.ledgerHeading)}`
        : '_(none cited; proven by canary)_';
      say(
        `| \`${cell(u.key)}\` | \`${cell(u.tool)}\` | \`${cell(u.query)}\` | \`${cell(u.signature)}\` | ` +
        `${ledger} | ${cell(u.proofBy)} |`
      );
    }
    say('');
    for (const u of t.upstream) {
      say('<details>');
      say(`<summary>What the third party said to <code>${cell(u.key)}</code></summary>`);
      say('');
      say('```');
      // The third party wrote this text. A ``` inside it would close the fence
      // and turn the rest of the summary into markup, so the fence character is
      // the one thing that does not survive verbatim.
      say(clip(u.errorText, 1200).replace(/`{3,}/g, "'''"));
      say('```');
      say('');
      say(`canary: \`${cell(u.canary?.outcome ?? u.canaryOutcome)}\`` +
        (u.canary?.independent ? ' measured on the **independent** path' : '') +
        ` at \`${cell(u.canary?.at ?? '?')}\``);
      say('');
      say(`evidence: \`app/build/upstream/warnings/${cell(u.key)}.json\` ` +
        '(uploaded as the `upstream-evidence` artifact on every run)');
      say('</details>');
      say('');
    }
    say('**A warning is a red for the upstream, not a pass for us.** Nothing those tests');
    say('name was verified; the ledger entry is the debt, and the remedy is to fix or wait');
    say('for the third party and re-run.');
    say('');
  }

  if (t.stale.length) {
    say(`### ${t.stale.length} result file(s) predate this run`);
    say('');
    say('You are reading an earlier run\'s evidence. This is fatal.');
    say('');
    for (const f of t.stale) say(`- \`${f}\``);
    say('');
  }

  if (t.orphanWarnings.length) {
    say('### Evidence files matching no failure');
    say('');
    say('Written by a test that then passed, or left over. Not fatal, but not nothing:');
    say('');
    for (const o of t.orphanWarnings) say(`- \`${o}.json\``);
    say('');
  }

  say('---');
  say('');
  say('The verdict is the **Classify the tally** step (`scripts/gate-tally.mjs`), the same');
  say('script `scripts/loop-gate.mjs` runs locally. This summary only renders what it');
  say('computed and never decides anything. `docs/CI.md` is the long form.');
  say('');
  say(`This run started \`${new Date(t.runStart).toISOString()}\`, derived from ${cell(t.runStartSource)}; ` +
    `the size floor is ${expectMin ? `**${expectMin}** test(s), from \`${EXPECTED_MIN_FILE}\`` : '**not in force**'}. ` +
    'Both used to be flags no workflow passed.');
  if (runUrl) {
    say('');
    say(`Artifacts for this run: ${runUrl}#artifacts`);
  }
  return `${out.join('\n')}\n`;
}

// --------------------------------------------------------------------------
// CLI
// --------------------------------------------------------------------------
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
  const out = opt('--out', process.env.GITHUB_STEP_SUMMARY ?? null);

  // THE SAME TWO DERIVED INPUTS THE GATE USES, resolved the same way, because a
  // presenter given different inputs is a presenter that disagrees for a reason
  // nobody can see (round 20, section 5). `--started-at` and `--expect-min` are
  // accepted so a caller can drive both scripts identically; neither is needed
  // in CI, and that is the point of deriving them.
  const startedAtArg = opt('--started-at', null);
  const expectMinArg = opt('--expect-min', null);
  const ownResults = resultsDir === resolve(defaultResultsDir(repo));
  const expectMin = expectMinArg !== null
    ? Number(expectMinArg)
    : (ownResults ? expectedMinFloor(repo).floor : 0);

  let markdown;
  try {
    const t = tally({
      resultsDir, warningsDir, repoDir: repo,
      startedAt: startedAtArg === null ? null : Number(startedAtArg),
    });
    const runUrl = process.env.GITHUB_SERVER_URL && process.env.GITHUB_REPOSITORY && process.env.GITHUB_RUN_ID
      ? `${process.env.GITHUB_SERVER_URL}/${process.env.GITHUB_REPOSITORY}/actions/runs/${process.env.GITHUB_RUN_ID}`
      : null;
    markdown = summarize(t, { runUrl, expectMin });
  } catch (e) {
    // A presenter that crashes must not take the run with it - the verdict is
    // the next step's, and it is about to run for real.
    markdown = `## CI gate summary unavailable\n\n\`\`\`\n${e.stack ?? e.message}\n\`\`\`\n\n` +
      'This is the SUMMARY failing, not the gate. The **Classify the tally** step below is\n' +
      'the verdict; read it (and `docs/CI.md`).\n';
  }

  if (out) {
    // GitHub's own step-summary file is APPENDED to across steps; a fresh
    // `--out` path is written.
    if (process.env.GITHUB_STEP_SUMMARY === out && existsSync(out)) appendFileSync(out, markdown);
    else writeFileSync(out, markdown);
    console.log(`wrote ${markdown.length} bytes of job summary to ${out}`);
  }
  // Always stdout as well: a local run has no $GITHUB_STEP_SUMMARY, and the log
  // is still the fallback when the summary is truncated (GitHub caps it at 1 MiB).
  console.log(markdown);
  process.exit(0);
}
