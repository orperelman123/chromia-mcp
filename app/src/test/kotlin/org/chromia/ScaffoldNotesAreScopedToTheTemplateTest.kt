package org.chromia

import org.chromia.tools.propertiesOrEmpty
import org.chromia.tools.callToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AUDIT F5 (2026-09-06) - scaffold_dapp shipped an identical 22 KB `notes` blob
 * with every template.
 *
 * Measured across all twelve templates in the audited jar plus the `bridge`
 * fallback: `notes` was 22,120-22,129 bytes (~5,530 tokens) in every single
 * response - byte-for-byte the same catalogue of redirects for lending, amm,
 * stablecoin, exchange, subscription, streaming, marketplace, governance and
 * vault.
 *
 *   template  | total B | notes B | files B | notes as % of response
 *   hello     |  24,761 |  22,122 |   2,204 | 89%
 *   vault     |  39,823 |  22,122 |  17,266 | 56%
 *   staking   |  49,518 |  22,124 |  26,955 | 45%
 *   ft4       |  34,945 |  22,123 |  12,388 | 63%
 *   lending   | 117,275 |  22,124 |  94,712 | 19%
 *
 * An agent that scaffolded `hello` to try the server paid ~5,530 tokens of
 * essay about stablecoin liquidation and order-book cancel clocks for 2,204
 * bytes of Rell - and then skimmed, which is how the paragraph that mattered
 * (the moduleArgs merge, F4) went unread 14 KB in.
 */
class ScaffoldNotesAreScopedToTheTemplateTest {

    private val executor = ToolExecutor(RecordingRepository(), PromptManager())

    private fun scaffold(template: String, notesFor: String? = null): JsonObject = runBlocking {
        executor.executeTool(
            callToolRequest(
                name = "scaffold_dapp",
                arguments = buildJsonObject {
                    put("name", "my_dapp")
                    put("template", template)
                    if (notesFor != null) put("notesFor", notesFor)
                }
            )
        ).structuredContent!!
    }

    private fun notesOf(payload: JsonObject) = payload["notes"]!!.jsonPrimitive.content

    private fun responseBytes(payload: JsonObject) = Json.encodeToString(JsonObject.serializer(), payload).length

    /**
     * The measured bound. `hello` scaffolded 24,761 B of which 22,122 B were
     * notes; the code is 2,204 B. Everything that is NOT the catalogue - header,
     * pins, forbidden list, the moduleArgs field, the trailer - is a few KB, so
     * 10,000 B is a bound the old response missed by 15 KB and a scoped one
     * clears with room for the guidance to grow.
     */
    private val helloResponseCap = 10_000

    @Test
    fun theHelloResponseIsUnderItsMeasuredBound() {
        val payload = scaffold("hello")
        val bytes = responseBytes(payload)
        assertTrue(
            bytes < helloResponseCap,
            "hello was 24,761 B (89% of it a catalogue about stablecoins and order books); now $bytes B"
        )
        val notes = notesOf(payload)
        assertTrue(notes.length < 6_000, "hello's notes were 22,122 B; now ${notes.length} B")
        // The header an agent needs whatever it scaffolded is still there.
        assertTrue(notes.contains("merkle_hash_version must stay 2"), notes)
        assertTrue(notes.contains("HOW TO PASS module_args"), notes)
        assertTrue(notes.contains("A passing security check is NOT economic soundness"), notes)
        // ...and the trailer.
        assertTrue(notes.contains("NEVER import"), notes)
        assertTrue(notes.contains("does not send signed transactions"), notes)
    }

    @Test
    fun aTemplateGetsItsOwnParagraphsAndNotEveryoneElses() {
        val governance = notesOf(scaffold("governance"))
        assertTrue(governance.contains("Building a DAO / treasury"), governance)
        assertTrue(governance.contains("template=governance"), governance)
        // The classes it is not.
        listOf(
            "Building an ORDER BOOK",
            "Building a stablecoin",
            "Building a swap pool",
            "Building a lending pool"
        ).forEach {
            assertFalse(governance.contains(it), "governance notes must not carry `$it`")
        }

        val exchange = notesOf(scaffold("exchange"))
        assertTrue(exchange.contains("Building an ORDER BOOK"), exchange)
        assertFalse(exchange.contains("Building a DAO / treasury"), exchange)
    }

    @Test
    fun everyTemplateIsSmallerThanTheCatalogueAndKeepsAPointerToIt() {
        val fullCatalogue = DappScaffold.notes("my_dapp")
        assertTrue(fullCatalogue.length > 20_000, "the catalogue really is ~22 KB: ${fullCatalogue.length}")
        val lengths = DappScaffold.templates.associateWith { notesOf(scaffold(it)).length }
        lengths.forEach { (template, length) ->
            assertTrue(
                length < fullCatalogue.length,
                "$template still carries the whole catalogue ($length B)"
            )
        }
        // And they are no longer all the same number - which is what the audit
        // measured: 22,120-22,129 across every template.
        assertTrue(
            lengths.values.toSet().size > 1,
            "notes must differ by template; got ${lengths.values.toSet()}"
        )
        DappScaffold.templates.forEach { template ->
            val notes = notesOf(scaffold(template))
            assertTrue(
                notes.contains("notesFor") || notes.contains("chromia_rell_practices_help"),
                "$template must point at where the rest of the guidance is: $notes"
            )
        }
    }

    @Test
    fun notesForAllStillReturnsTheWholeCatalogue() {
        val all = notesOf(scaffold("hello", notesFor = "all"))
        assertEquals(DappScaffold.notes("my_dapp"), all)
        listOf(
            "Building a DAO / treasury",
            "Building an ORDER BOOK",
            "Building a stablecoin",
            "Building a BRIDGE",
            "Building RECURRING PULL BILLING"
        ).forEach { assertTrue(all.contains(it), "notesFor=all must keep `$it`") }
    }

    @Test
    fun notesForANamedTemplateReturnsThatOnesGuidance() {
        val amm = notesOf(scaffold("hello", notesFor = "amm"))
        assertTrue(amm.contains("Building a swap pool"), amm)
        assertFalse(amm.contains("Building a DAO / treasury"), amm)
        // The alias spelling an agent will also try.
        val viaAlias = runBlocking {
            executor.executeTool(
                callToolRequest(
                    name = "scaffold_dapp",
                    arguments = buildJsonObject {
                        put("name", "my_dapp"); put("template", "hello"); put("notes_for", "amm")
                    }
                )
            )
        }
        assertTrue(viaAlias.isError != true, viaAlias.structuredContent.toString())
        assertEquals(amm, notesOf(viaAlias.structuredContent!!))
    }

    @Test
    fun nothingIsLostAcrossTheSplit() {
        // Union of every template's scoped notes must cover the whole catalogue:
        // the split is a runtime re-slice of the same prose, not a rewrite, and
        // no paragraph may fall between two templates.
        val full = DappScaffold.notes("my_dapp")
        val covered = DappScaffold.templates
            .flatMap { DappScaffold.notes("my_dapp", it).lines() }
            .toSet()
        val missing = full.lines()
            .filter { it.isNotBlank() }
            .filterNot { it in covered }
        assertTrue(
            missing.isEmpty(),
            "these lines belong to no template and would never be shown:\n" + missing.joinToString("\n")
        )
    }

    @Test
    fun theSchemaDeclaresNotesFor() {
        val tool = McpTools.allTools().single { it.name == "scaffold_dapp" }
        assertNotNull(tool.inputSchema.propertiesOrEmpty["notesFor"])
        assertTrue(
            tool.inputSchema.propertiesOrEmpty["notesFor"]!!.jsonObject["description"]!!.jsonPrimitive.content
                .contains("all"),
            "an agent must be told how to get the rest back"
        )
    }
}
