package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Round 17's recorders and their FROZEN evidence - the pattern commit b640e6c
 * established for round 16 ([Round16Evidence]), applied to the round-17
 * directory.
 *
 * [Round17SecurityRuleProbeTest] used to write its raw verdicts straight back
 * into the committed evidence at
 * `src/test/resources/exploit-corpus/realworld/adversary-round17/seccheck/raw.json`
 * on every run, so a gate left that tracked file modified and the evidence the
 * round README cites paragraph by paragraph was whatever the LAST build
 * happened to say. Now:
 *
 * 1. the committed files under [committedRoot] are frozen evidence - nothing in
 *    the suite writes there;
 * 2. every run writes its fresh recording to the same relative path under
 *    [freshRoot] (`app/build/adversary-round17/`, gitignored) BEFORE anything is
 *    asserted, so a red run still leaves the evidence behind;
 * 3. [assertFrozen] compares the fresh recording with the committed one value by
 *    value (the diff is [Round16Evidence.diff], the same one), and a difference
 *    is a REGRESSION reported with both values and both paths. Re-freezing is a
 *    deliberate act: copy the fresh file over the committed one and commit it
 *    with the reason.
 *
 * The round-17 seccheck evidence exists in TWO frozen files, because the round
 * changed the analyzer it was measuring:
 *  - `seccheck/raw.before-fix.json` is the ADVERSARY'S OWN recording, the
 *    analyzer as round 17 found it, and nothing runs against it - it is the only
 *    record of what was wrong and a recorder that overwrites it destroys the
 *    round;
 *  - `seccheck/raw.json` is the after-fix recording, re-recorded ONCE when the
 *    two rule fixes landed, and it is what this run is asserted against.
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
