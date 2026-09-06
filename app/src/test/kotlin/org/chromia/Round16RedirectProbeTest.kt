package org.chromia

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.junit.jupiter.api.Test
import java.io.File

/**
 * ROUND 16, audit fix F2 - "unknown-template routing to the CLOSEST template".
 * `closestTemplateNote` is an ORDERED `when` of unanchored, case-insensitive
 * substring tests, so an earlier branch wins on a substring that appears inside
 * an unrelated word. This recorder asks it for a set of ordinary asks and writes
 * where each one lands, so the round's claim about the redirect is a measurement
 * rather than a reading of the source.
 *
 * Output: `realworld/adversary-round16/redirect/raw.json`.
 */
class Round16RedirectProbeTest {

    private val out = File("src/test/resources/exploit-corpus/realworld/adversary-round16/redirect")

    private val asks = listOf(
        "an investment DAO",
        "an investment club with a treasury",
        "a DAO",
        "a bidirectional payment channel",
        "an NFT lending protocol",
        "a yield harvesting vault",
        "a cross-chain DEX",
        "a wallet with a spending allowance",
        "a raffle with on-chain randomness",
        "an insurance pool with claims",
        "a prediction market",
        "a loyalty programme"
    )

    @Test
    fun `record where the redirect sends twelve ordinary asks`() {
        out.mkdirs()
        val lines = mutableListOf<String>()
        val rows = buildJsonArray {
            for (ask in asks) {
                val target = DappScaffold.closestTemplate(ask)
                val note = DappScaffold.closestTemplateNote(ask)
                // What ScaffoldDappStrategy would hand back: `effectiveTemplate =
                // exact ?: redirect`, then `files(chain, effectiveTemplate)`.
                val scaffolded = if (target == null) listOf<String>() else
                    runCatching { DappScaffold.files("probe", target).keys.toList() }
                        .getOrElse { listOf("<error: " + it.message + ">") }
                lines += "%-38s -> %-14s files=%s".format(ask, target ?: "(none)", scaffolded)
                add(
                    buildJsonObject {
                        put("ask", ask)
                        put("closestTemplate", target ?: "")
                        put("noteFirstSentence", note.substringBefore(". ").take(300))
                        put("scaffoldedFiles", buildJsonArray { scaffolded.forEach { add(it) } })
                    }
                )
            }
        }
        File(out, "raw.json").writeText(
            Json { prettyPrint = true }.encodeToString(JsonArray.serializer(), rows)
        )
        println("ROUND16-REDIRECT\n" + lines.joinToString("\n"))
    }
}
