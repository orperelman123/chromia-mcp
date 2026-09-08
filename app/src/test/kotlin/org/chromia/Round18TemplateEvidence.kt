package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * The round-18 TEMPLATE lane's own recorder root, and its frozen evidence.
 *
 * It is separate from `adversary-round18/` for the reason [Round17TemplateEvidence]
 * is separate from `adversary-round17/`: that directory is the ADVERSARY's
 * measurement of where fifty-four asks landed BEFORE this lane touched anything -
 * including the laundered mixed ask its README section 5 names - and the round's
 * own PROVENANCE paragraph says nothing in the suite writes there. Editing it would
 * delete the BEFORE half of a before/after, which is the only evidence the routing
 * changed at all. So this lane writes its AFTER here.
 *
 * The mechanism is [Round16Evidence]'s, applied the way [Round18Evidence] applies
 * it: every run writes to `app/build/round18-template-fix/` (gitignored) BEFORE
 * anything is asserted, so a red run still leaves its evidence behind, and
 * [assertFrozen] then compares that against the committed copy value by value. A
 * difference is a REGRESSION reported with both values and both paths; re-freezing
 * is a deliberate act - copy the fresh file over the committed one and commit it
 * with the reason.
 */
object Round18TemplateEvidence {

    val committedRoot = File("src/test/resources/exploit-corpus/realworld/round18-template-fix")
    val freshRoot = File("build/round18-template-fix")

    private val json = Json { prettyPrint = true }

    fun record(relative: String, fresh: JsonElement): File {
        val file = File(freshRoot, relative)
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(JsonElement.serializer(), fresh))
        return file
    }

    fun assertFrozen(relative: String, fresh: JsonElement) {
        val committedFile = File(committedRoot, relative)
        val freshFile = File(freshRoot, relative)
        assertTrue(
            committedFile.isFile,
            "frozen round-18 template evidence missing at ${committedFile.absolutePath}; this run's recording " +
                "is at ${freshFile.absolutePath} - if it is the intended baseline, copy it there and commit it"
        )
        val committed = Json.parseToJsonElement(committedFile.readText())
        val diffs = Round16Evidence.diff(committed, fresh, "$")
        assertTrue(
            diffs.isEmpty(),
            "round-18 template evidence `$relative` DRIFTED from its frozen recording - ${diffs.size} value(s) " +
                "differ (committed: ${committedFile.absolutePath}; this run: ${freshFile.absolutePath}). " +
                "A drift is a regression unless the redirect was changed on purpose, in which case re-freeze by " +
                "copying this run's file over the committed one:\n" + diffs.joinToString("\n")
        )
    }
}
