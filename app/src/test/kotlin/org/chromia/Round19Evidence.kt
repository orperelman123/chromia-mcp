package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Round 18's recorder and its FROZEN evidence - the b640e6c pattern that
 * [Round17Evidence] applies to the round-17 tree, applied to round 19's.
 *
 * The committed files under [committedRoot] are evidence the round README cites
 * line by line, and nothing in the suite writes there. Every run writes its
 * fresh recording to the same relative path under [freshRoot]
 * (`app/build/adversary-round19/`, gitignored) BEFORE anything is asserted, so a
 * red run still leaves the evidence behind, and [assertFrozen] then compares the
 * two value by value: a verdict that drifts is a REGRESSION naming both values
 * and both files, never a silent rewrite. Re-freezing is deliberate - copy the
 * fresh file over the committed one and commit it with the reason.
 *
 * The leaf-by-leaf diff is [Round16Evidence.diff], the one implementation all
 * three rounds use, so a change to how a drift is reported cannot describe two
 * rounds differently.
 */
object Round19Evidence {

    val committedRoot = File("src/test/resources/exploit-corpus/realworld/adversary-round19")
    val freshRoot = File("build/adversary-round19")

    private val json = Json { prettyPrint = true }

    /** Writes [fresh] to `build/adversary-round19/<relative>` and returns that file. */
    fun record(relative: String, fresh: JsonElement): File {
        val file = File(freshRoot, relative)
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(JsonElement.serializer(), fresh))
        return file
    }

    /**
     * Asserts that the recording at `build/adversary-round19/<relative>` equals
     * the frozen one under [committedRoot]. Every differing value is listed with
     * its JSON path, the committed value and the fresh one.
     */
    fun assertFrozen(relative: String, fresh: JsonElement) {
        val committedFile = File(committedRoot, relative)
        val freshFile = File(freshRoot, relative)
        assertTrue(
            committedFile.isFile,
            "frozen round-19 evidence missing at ${committedFile.absolutePath}; this run's recording is at " +
                "${freshFile.absolutePath} - if this is a NEW probe, commit that file as its evidence"
        )
        val committed = Json.parseToJsonElement(committedFile.readText())
        val differences = Round16Evidence.diff(committed, fresh, "$")
        assertTrue(
            differences.isEmpty(),
            "round-19 evidence drifted at $relative. The committed file is ${committedFile.absolutePath}, " +
                "this run wrote ${freshFile.absolutePath}. Re-freezing is deliberate: copy the fresh file over " +
                "the committed one and commit it with the reason.\n  " + differences.joinToString("\n  ")
        )
    }
}
