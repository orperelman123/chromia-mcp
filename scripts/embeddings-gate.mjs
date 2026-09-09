#!/usr/bin/env node
// SHRINK GATE for a freshly generated RAG index, in SEGMENTS and PER SOURCE.
//
//   node scripts/embeddings-gate.mjs --new app/build/embeddings.provenance.json \
//        --published published.provenance.json [--floor 0.8] [--json]
//
// Exit 0 = the store may replace the published one, 1 = a source really shrank,
// 2 = the gate could not read what it needs (which is not a pass).
//
// WHY NOT BYTES. The `Embeddings refresh` workflow used to compare the size of
// the new embeddings.json against the published asset and refuse anything under
// 80% of it. That is the right question - a repository that half-arrived must
// never overwrite a good index - asked of a quantity that cannot answer it. It
// went red on 2026-09-07 (run 34103273206) and again on 2026-09-09 (run
// 34344751517): 109,958,949 bytes against 147,681,194, 74.5%. Nothing had
// failed. Commit f0ee597 (audit F15) had taken the host-language TEST sources
// out of the corpus - 540 documents, 6,481 segments - because `*Test.kt` files
// were outranking the `.md` pages on ten basic Rell questions. The store is
// smaller ON PURPOSE, and the byte count cannot tell that apart from a failed
// clone. Meanwhile the published index stayed five days stale and kept serving
// the very files F15 removed.
//
// WHAT IS COMPARED INSTEAD. The generator writes a per-source breakdown beside
// the store (RagStore -> IngestBreakdown): for every repository and for the
// sitemap, the documents and segments it INDEXED and, separately, the documents
// and segments the ingest rules REFUSED. Their sum - `available` below - is what
// the source OFFERED, and that is the quantity that survives a change to the
// ingest rules. The gate requires, per source:
//
//     new.segments + new.excluded_segments  >=  floor * published.available
//
// For the two failing runs, on the totals: 19,107 + 6,481 = 25,588 >= 0.8 x
// 25,588. That is not a tolerance being stretched; it is the proof that nothing
// was lost. A source that genuinely half-arrives has fewer segments in BOTH
// columns and fails, by name, with its numbers in the job summary.
//
// The published sidecar of 2026-09-04 predates the breakdown and carries only
// the total (25,588 segments), so the gate falls back to comparing totals the
// same way and says so. Once a run publishes a sidecar with `sources`, the
// comparison is per source and a single repository failing to clone can no
// longer hide inside a healthy total.
//
// ONE THING THAT MOVES THE NUMBERS AND IS NOT AN INGEST: the line endings of
// the checkout the segments were split from. A local run of this generator on
// the dev laptop measured 19,281 segments on 2026-09-09; run 34344751517, the
// same corpus the same day, measured 19,107. Not a drop and not drift - the
// laptop's git has core.autocrlf=true (system config), so every text file
// arrives with one extra character per line. Measured on chromia-cli's `docs`,
// cloned twice: 86,078 characters against 83,939 over 2,139 lines, exactly one
// per line. The same 0.9% sits between the index f0ee597 audited locally
// (25,823) and the runner's published one (25,588). Inside the workflow both
// sides are the same Linux runner, so the gate never sees this; a LOCAL sidecar
// compared against a PUBLISHED one runs about 0.9% high, which against a 20%
// floor decides nothing. Planned and stored are NOT the seam: run 34344751517
// split 19,107 and rag-eval read 19,107 back out of the store.
//
// And the fallback is weak in a second way that the per-source form is not: a
// run that loses `postchain` outright - all 5,546 segments it offers - lands at
// 79.2% of the published total, 194 segments under the floor. It is caught, but
// only just. Per source that same run is 0% of postchain and refused by name.
import { readFileSync, appendFileSync } from 'node:fs';

const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, arr) => {
  if (a.startsWith('--')) acc.push([a.slice(2), arr[i + 1] && !arr[i + 1].startsWith('--') ? arr[i + 1] : 'true']);
  return acc;
}, []));

const floor = Number(args.floor ?? 0.8);
const asJson = args.json === 'true';
const summaryFile = args.summary && args.summary !== 'true' ? args.summary : process.env.GITHUB_STEP_SUMMARY;

const out = [];
const say = line => { out.push(line); if (!asJson) console.log(line); };

const die = (code, message) => {
  if (asJson) console.log(JSON.stringify({ ok: false, error: message }, null, 2));
  else console.error(message);
  process.exit(code);
};

const readSidecar = (path, what) => {
  let text;
  try { text = readFileSync(path, 'utf8'); } catch (e) { die(2, `cannot read the ${what} sidecar ${path}: ${e.message}`); }
  try { return JSON.parse(text); } catch (e) { die(2, `the ${what} sidecar ${path} is not JSON: ${e.message}`); }
};

if (!args.new || args.new === 'true') die(2, 'usage: embeddings-gate.mjs --new <provenance.json> [--published <provenance.json>] [--floor 0.8]');

const fresh = readSidecar(args.new, 'new');
// A sidecar with no `sources` on the NEW side means the store was built by a
// generator that predates the breakdown - the gate has nothing to compare like
// with like and must not wave the publish through.
if (!Array.isArray(fresh.sources) || fresh.sources.length === 0) {
  die(1, `the new sidecar ${args.new} has no per-source \`sources\` breakdown: it was written by a generator that predates ` +
    'IngestBreakdown, so a shrink cannot be told from an ingest-rule change. Regenerate with :app:generateEmbeddingsNoUpload.');
}

/** Segments a source OFFERED: indexed plus deliberately excluded. `excluded_segments` is absent on pre-f0ee597 sidecars. */
const available = row => Number(row.segments ?? 0) + Number(row.excluded_segments ?? 0);
const byName = rows => new Map((rows ?? []).map(r => [String(r.source), r]));

const publishedPath = args.published && args.published !== 'true' ? args.published : null;
const published = publishedPath ? readSidecar(publishedPath, 'published') : null;

const rows = [];
let verdict = 'pass';
let basis;

if (!published) {
  basis = 'no published sidecar was given - nothing to compare, this is a first publish';
} else if (Array.isArray(published.sources) && published.sources.length > 0) {
  basis = 'per source, against the published sidecar\'s own breakdown';
  const freshByName = byName(fresh.sources);
  const publishedByName = byName(published.sources);
  for (const publishedRow of published.sources) {
    const name = String(publishedRow.source);
    const freshRow = freshByName.get(name);
    const was = available(publishedRow);
    const now = freshRow ? available(freshRow) : 0;
    const need = Math.ceil(floor * was);
    const ok = now >= need;
    if (!ok) verdict = 'fail';
    rows.push({ source: name, published: was, now, need, ok, missing: !freshRow });
  }
  for (const freshRow of fresh.sources) {
    if (!publishedByName.has(String(freshRow.source))) {
      rows.push({ source: String(freshRow.source), published: 0, now: available(freshRow), need: 0, ok: true, added: true });
    }
  }
} else {
  basis = `the published sidecar predates the breakdown (no \`sources\`), so the comparison is on the TOTALS - ` +
    `${fresh.segments} indexed + ${fresh.excluded_segments ?? 0} excluded by ${fresh.excluded_by ?? 'the ingest rules'}`;
  const was = available(published);
  const now = available(fresh);
  const need = Math.ceil(floor * was);
  const ok = now >= need;
  if (!ok) verdict = 'fail';
  rows.push({ source: '(all sources)', published: was, now, need, ok, total: true });
}

const pct = row => (row.published > 0 ? `${((row.now / row.published) * 100).toFixed(1)}%` : 'n/a');

const table = [
  '| source | published available | new available | floor | % | verdict |',
  '| --- | ---: | ---: | ---: | ---: | :--- |',
  ...rows.map(r => `| \`${r.source}\` | ${r.published} | ${r.now} | ${r.need} | ${pct(r)} | ${
    r.added ? 'NEW SOURCE' : r.missing ? 'MISSING - no rows at all' : r.ok ? 'ok' : 'REFUSED'} |`)
];

say('## Embeddings refresh - shrink gate');
say('');
say(`Comparing SEGMENTS OFFERED (indexed + deliberately excluded), ${basis}. Floor ${(floor * 100).toFixed(0)}%.`);
say('');
table.forEach(say);
say('');
say(`Totals: ${fresh.documents ?? '?'} documents / ${fresh.segments} segments indexed, ` +
  `${fresh.excluded_documents ?? 0} documents / ${fresh.excluded_segments ?? 0} segments excluded by ` +
  `\`${fresh.excluded_by ?? 'the ingest rules'}\` (audit F15).`);
// Informational, deliberately NOT the verdict: this is the number that refused
// two healthy runs. It stays visible so an unexplained size change is still
// noticed by a human.
say(`Bytes (informational, not the verdict): new ${fresh.size_bytes ?? '?'}, published ${published?.size_bytes ?? '?'}.`);
say('');
if (verdict === 'fail') {
  const failed = rows.filter(r => !r.ok).map(r => `${r.source} (${r.now} of ${r.published}, floor ${r.need})`);
  say(`REFUSED: ${failed.join('; ')} - fewer segments than the published index offered, ` +
    'even counting the ones the ingest rules exclude. A source that half-arrived must not overwrite a good index.');
} else {
  say('PASSED: every source still offers at least the floor of what the published index offered.');
}

if (summaryFile) {
  try { appendFileSync(summaryFile, out.join('\n') + '\n'); } catch (e) { console.error(`could not write the job summary: ${e.message}`); }
}
if (verdict === 'fail' && !asJson) console.log(`::error::embeddings refresh refused: ${rows.filter(r => !r.ok).map(r => r.source).join(', ')}`);
if (asJson) console.log(JSON.stringify({ ok: verdict === 'pass', floor, basis, rows }, null, 2));
process.exit(verdict === 'pass' ? 0 : 1);
