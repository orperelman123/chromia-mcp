package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File

/**
 * Round 16's recorders and their FROZEN evidence.
 *
 * The three round-16 probe classes ([Round16VerifyGuardsProbeTest],
 * [Round16SecurityRuleProbeTest], [Round16RedirectProbeTest]) used to write
 * their raw verdicts straight back into the committed evidence under
 * `src/test/resources/exploit-corpus/realworld/adversary-round16/` on every run,
 * so a gate left fifteen-plus tracked files modified and the evidence a README
 * cites line by line was whatever the LAST build happened to say. Now:
 *
 * 1. the committed files under [committedRoot] are frozen evidence - nothing in
 *    the suite writes there;
 * 2. every run writes its fresh recording to the same relative path under
 *    [freshRoot] (`app/build/adversary-round16/`, gitignored) BEFORE anything is
 *    asserted, so a red run still leaves the evidence behind;
 * 3. [assertFrozen] compares the fresh recording with the committed one, value
 *    by value, and a difference is a REGRESSION reported with both values and
 *    both paths. Re-freezing is a deliberate act: copy the fresh file over the
 *    committed one and commit it with the reason.
 */
object Round16Evidence {

    val committedRoot = File("src/test/resources/exploit-corpus/realworld/adversary-round16")
    val freshRoot = File("build/adversary-round16")

    private val json = Json { prettyPrint = true }

    /** Writes [fresh] to `build/adversary-round16/<relative>` and returns that file. */
    fun record(relative: String, fresh: JsonElement): File {
        val file = File(freshRoot, relative)
        file.parentFile.mkdirs()
        file.writeText(json.encodeToString(JsonElement.serializer(), fresh))
        return file
    }

    /**
     * Asserts that the recording at `build/adversary-round16/<relative>` (what
     * this run measured) equals the frozen one at
     * `src/test/resources/.../adversary-round16/<relative>`. Every differing
     * value is listed with its JSON path, the committed value and the fresh one.
     */
    fun assertFrozen(relative: String, fresh: JsonElement) {
        val committedFile = File(committedRoot, relative)
        val freshFile = File(freshRoot, relative)
        assertTrue(
            committedFile.isFile,
            "frozen round-16 evidence missing at ${committedFile.absolutePath}; this run's recording is at " +
                "${freshFile.absolutePath} - if it is the intended baseline, copy it there and commit it"
        )
        val committed = Json.parseToJsonElement(committedFile.readText())
        val diffs = diff(committed, fresh, "$")
        assertTrue(
            diffs.isEmpty(),
            "round-16 evidence `$relative` DRIFTED from its frozen recording - ${diffs.size} value(s) differ " +
                "(committed: ${committedFile.absolutePath}; this run: ${freshFile.absolutePath}). " +
                "A drift is a regression unless the tool was changed on purpose, in which case re-freeze " +
                "by copying this run's file over the committed one:\n" + diffs.joinToString("\n")
        )
    }

    /** Leaf-by-leaf structural diff: `path: committed=<a> fresh=<b>`. */
    fun diff(committed: JsonElement, fresh: JsonElement, path: String): List<String> = when {
        committed is JsonObject && fresh is JsonObject -> {
            (committed.keys + fresh.keys).distinct().flatMap { key ->
                val a = committed[key]
                val b = fresh[key]
                when {
                    a == null -> listOf("$path.$key: committed=<absent> fresh=${show(b!!)}")
                    b == null -> listOf("$path.$key: committed=${show(a)} fresh=<absent>")
                    else -> diff(a, b, "$path.$key")
                }
            }
        }
        committed is JsonArray && fresh is JsonArray -> {
            if (committed.size != fresh.size) {
                listOf("$path: committed has ${committed.size} element(s), fresh has ${fresh.size}") +
                    committed.indices.filter { it < fresh.size }.flatMap { diff(committed[it], fresh[it], "$path[$it]") }
            } else {
                committed.indices.flatMap { diff(committed[it], fresh[it], "$path[$it]") }
            }
        }
        committed == fresh -> emptyList()
        else -> listOf("$path: committed=${show(committed)} fresh=${show(fresh)}")
    }

    private fun show(e: JsonElement): String = when (e) {
        is JsonNull -> "null"
        is JsonPrimitive -> if (e.isString) "\"${e.content}\"" else e.content
        else -> e.toString()
    }
}
