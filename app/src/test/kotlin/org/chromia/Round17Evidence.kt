package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Round 17's recorders and their FROZEN evidence - the b640e6c pattern, applied
 * to the round-17 tree.
 *
 * `Round17VerifyGuardsProbeTest` and `Round17SurfaceProbeTest` wrote their raw
 * verdicts straight back into the committed evidence under
 * `src/test/resources/exploit-corpus/realworld/adversary-round17/` on every run
 * - the same defect b640e6c fixed for round 16 one commit before round 17's
 * files were written. Twenty-one tracked files were rewritten by every gate,
 * and the evidence the round README cites line by line was whatever the last
 * build happened to say. Now:
 *
 * 1. the committed files under [committedRoot] are frozen evidence - nothing in
 *    the suite writes there;
 * 2. every run writes its fresh recording to the same relative path under
 *    [freshRoot] (`app/build/adversary-round17/`, gitignored) BEFORE anything is
 *    asserted, so a red run still leaves the evidence behind;
 * 3. [assertFrozen] compares the fresh recording with the committed one, value
 *    by value, and a difference is a REGRESSION reported with both values and
 *    both paths. Re-freezing is a deliberate act: copy the fresh file over the
 *    committed one and commit it with the reason.
 *
 * The leaf-by-leaf diff is [Round16Evidence.diff] - one implementation, used by
 * both rounds, so a change to how a drift is reported cannot describe two
 * rounds differently.
 */
object Round17Evidence {

    val committedRoot = File("src/test/resources/exploit-corpus/realworld/adversary-round17")
    val freshRoot = File("build/adversary-round17")

    private val json = Json { prettyPrint = true }

    /** Writes [fresh] to `build/adversary-round17/<relative>` and returns that file. */
    fun record(relative: String, fresh: JsonElement): File {
        val file = File(freshRoot, relative)
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(JsonElement.serializer(), fresh))
        return file
    }

    /**
     * Asserts that the recording at `build/adversary-round17/<relative>` (what
     * this run measured) equals the frozen one at
     * `src/test/resources/.../adversary-round17/<relative>`. Every differing
     * value is listed with its JSON path, the committed value and the fresh one.
     */
    fun assertFrozen(relative: String, fresh: JsonElement) {
        val committedFile = File(committedRoot, relative)
        val freshFile = File(freshRoot, relative)
        assertTrue(
            committedFile.isFile,
            "frozen round-17 evidence missing at ${committedFile.absolutePath}; this run's recording is at " +
                "${freshFile.absolutePath} - if it is the intended baseline, copy it there and commit it"
        )
        val committed = Json.parseToJsonElement(committedFile.readText())
        val diffs = Round16Evidence.diff(committed, fresh, "$")
        assertTrue(
            diffs.isEmpty(),
            "round-17 evidence `$relative` DRIFTED from its frozen recording - ${diffs.size} value(s) differ " +
                "(committed: ${committedFile.absolutePath}; this run: ${freshFile.absolutePath}). " +
                "A drift is a regression unless the tool was changed on purpose, in which case re-freeze " +
                "by copying this run's file over the committed one:\n" + diffs.joinToString("\n")
        )
    }
}
