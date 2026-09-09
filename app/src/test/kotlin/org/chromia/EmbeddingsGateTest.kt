package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * THE REFRESH GATE'S VERDICT, ON REAL SIDECARS.
 *
 * `scripts/embeddings-gate.mjs` decides whether a freshly generated index may
 * replace the published one, and it is the second gate in this repository to
 * hold that veto - the first compared BYTES and refused two healthy runs
 * (34103273206 on 2026-09-07, 34344751517 on 2026-09-09) because f0ee597
 * (audit F15) had deliberately taken 6,481 segments of host-language test
 * sources out of the corpus. Five days of a stale published index came out of
 * that, so this gate's arithmetic is asserted rather than trusted.
 *
 * Every input is a REAL file under `app/src/test/resources/embeddings-gate`:
 *
 *  - `published-2026-09-04.provenance.json` is the sidecar the `embeddings`
 *    release actually carries, byte for byte, fetched with
 *    `gh release download embeddings -p embeddings.provenance.json`. It predates
 *    the breakdown, so it exercises the totals fallback the next real run will
 *    take.
 *  - `local-refresh.provenance.json` is the sidecar a real
 *    `:app:generateEmbeddingsNoUpload` wrote on this laptop on 2026-09-09:
 *    seven repositories cloned, 381 sitemap pages fetched, 2,547 documents and
 *    19,281 segments indexed, 540 documents and 6,542 segments left out by the
 *    F15 rule - 25,823 offered against the published 25,588, from a store of
 *    111,686,474 bytes against 147,681,194, which is 75.6% and would have been
 *    the byte gate's third refusal of a healthy ingest.
 *  - `local-refresh-postchain-lost.provenance.json` is that same run with the
 *    `postchain` row's four numbers set to zero and the totals recomputed from
 *    the rows: what the sidecar of a run whose `postchain` clone produced
 *    nothing looks like. It is the failing case stated in the units the gate
 *    reads, not an imitation of one.
 *
 * The script is run as a process, the way the workflow runs it, so the exit code
 * under test is the exit code the workflow sees.
 */
class EmbeddingsGateTest {

    private val repo: Path get() = RepoFiles.root
    private fun fixture(name: String): String =
        repo.resolve("app/src/test/resources/embeddings-gate/$name").toString()

    private val published = fixture("published-2026-09-04.provenance.json")
    private val fresh = fixture("local-refresh.provenance.json")
    private val cut = fixture("local-refresh-postchain-lost.provenance.json")

    /**
     * A node that actually RUNS, found by asking every one on PATH for its
     * version. Windows keeps an App-Execution-Alias stub called `node.exe` in
     * WindowsApps that starts, prints nothing and exits - and a test that ran it
     * would report "the gate printed nothing" for a gate that never executed.
     * The same precaution UpstreamWarningGateTest takes, for the same reason.
     */
    private val node: String by lazy {
        val tried = mutableListOf<String>()
        val candidates = System.getenv("PATH").orEmpty().split(java.io.File.pathSeparator)
            .flatMap { dir -> listOf("node.exe", "node").map { java.io.File(dir, it) } }
            .filter { it.isFile }
            .map { it.absolutePath }
            .distinct()
        for (candidate in candidates) {
            val probe = ProcessBuilder(candidate, "--version").redirectErrorStream(true).start()
            val said = probe.inputStream.bufferedReader().readText().trim()
            probe.waitFor(60, TimeUnit.SECONDS)
            if (probe.exitValue() == 0 && said.startsWith("v")) return@lazy candidate
            tried += "$candidate -> exit ${probe.exitValue()}, said ${said.ifBlank { "<nothing>" }}"
        }
        throw AssertionError(
            "no working node on PATH, and the embeddings refresh gate runs on node - so this box cannot " +
                "verify the check that decides whether a fresh index is published. Tried " +
                "${candidates.size} candidate(s):\n  " + tried.joinToString("\n  ")
        )
    }

    /** `node scripts/embeddings-gate.mjs --new X --published Y`, as the workflow runs it. */
    private fun gate(newSidecar: String, publishedSidecar: String?): Pair<Int, String> {
        val command = mutableListOf(node, repo.resolve("scripts/embeddings-gate.mjs").toString(), "--new", newSidecar)
        if (publishedSidecar != null) command += listOf("--published", publishedSidecar)
        val process = ProcessBuilder(command).directory(repo.toFile()).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertTrue(
            process.waitFor(120, TimeUnit.SECONDS),
            "scripts/embeddings-gate.mjs did not finish; a gate that can hang is a refresh that never publishes"
        )
        return process.exitValue() to output
    }

    @Test
    fun theTwoFailingRunsWouldHavePassedOnTheirSegmentsInsteadOfTheirBytes() {
        val (exit, output) = gate(fresh, published)
        assertEquals(
            0, exit,
            "a full local ingest against the real published sidecar must be publishable. The byte gate " +
                "refused exactly this at 74.5%.\n$output"
        )
        assertTrue(
            output.contains("predates the breakdown"),
            "the published sidecar of 2026-09-04 has no `sources`, so the gate must say it is comparing " +
                "TOTALS and why.\n$output"
        )
        assertTrue(
            output.contains("Bytes (informational, not the verdict)"),
            "the byte figures stay visible - they are the thing a human still wants to see - but they " +
                "must be labelled as not the verdict.\n$output"
        )
        assertTrue(output.contains("PASSED:"), "the verdict line is missing\n$output")
    }

    @Test
    fun aSourceThatFetchedNothingIsRefusedByName() {
        val (exit, output) = gate(cut, fresh)
        assertEquals(
            1, exit,
            "a run whose postchain clone produced nothing must not overwrite a good index.\n$output"
        )
        assertTrue(
            output.contains("REFUSED") && output.contains("postchain"),
            "the refusal must NAME the source that shrank - the byte gate could only say a percentage, " +
                "which is why nobody could tell a deliberate exclusion from a failed clone.\n$output"
        )
        assertTrue(
            output.contains("MISSING - no rows at all") || output.contains("| `postchain` |"),
            "the failing source's numbers belong in the table an operator reads.\n$output"
        )
    }

    @Test
    fun theSameRunComparedAgainstItselfPassesPerSource() {
        val (exit, output) = gate(fresh, fresh)
        assertEquals(0, exit, "a sidecar cannot have shrunk against itself\n$output")
        assertTrue(
            output.contains("per source, against the published sidecar's own breakdown"),
            "once the published sidecar carries `sources` the comparison must be per source - a single " +
                "repository failing to clone can otherwise hide inside a healthy total.\n$output"
        )
    }

    @Test
    fun aTotalThatShrankIsRefusedEvenWithoutPerSourceNumbersOnTheOtherSide() {
        // The margin is the point, and it is thin: postchain offers 5,546 of
        // the 25,823 segments this ingest saw, so losing the whole repository
        // lands at 79.2% of the published 25,588 - 194 segments under the
        // floor. A totals-only comparison catches it by that much. The same run
        // compared per source is 0% of postchain, refused by name, which is
        // what the test above asserts and why the sidecar carries rows at all.
        val (exit, output) = gate(cut, published)
        assertEquals(
            1, exit,
            "the 2026-09-04 sidecar has only a total, and a run that lost a whole repository falls under " +
                "80% of it. The fallback has to refuse that.\n$output"
        )
        assertTrue(output.contains("(all sources)"), "the totals fallback labels its one row\n$output")
    }

    @Test
    fun aSidecarWithNoBreakdownCannotBePublished() {
        // The published sidecar of 2026-09-04 IS such a file - written by the
        // generator that predates IngestBreakdown - so the case is real rather
        // than invented, and it is the one a rollback would produce.
        val (exit, output) = gate(published, published)
        assertEquals(
            1, exit,
            "a store whose sidecar has no per-source breakdown cannot be compared like with like, and " +
                "an ungatable publish is not a passed gate.\n$output"
        )
        assertTrue(
            output.contains("no per-source"),
            "the message must say what is missing and how to get it back.\n$output"
        )
    }
}
