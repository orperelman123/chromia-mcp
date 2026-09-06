package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.tools.FetchDocsStrategy
import org.chromia.tools.FetchDocumentStrategy
import org.chromia.tools.McpTools
import org.chromia.tools.RagStore
import org.chromia.tools.SearchDocsStrategy
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

/**
 * AUDIT F2 (2026-09-06) - `search` and `fetch` never said the index was stale;
 * only `fetch_docs` did.
 *
 * Measured with the 320-day-old store loaded:
 *   fetch_docs{query:"module_args"} ends with
 *     "NOTE: documentation index is STALE: generated 2025-10-21, 320 days ago (limit 120)..."
 *   search{query:"..."}  -> exactly {"results":[{id,title,url}, ...]} - zero matches for /stale/i
 *                           across all ten audit questions.
 *   fetch{id:...}        -> exactly {"id","title","text","url"} - the same silence.
 *
 * search/fetch are the ChatGPT-contract pair every agent reaches for first, so
 * the one tool that told the truth was the one least likely to be called. All
 * three now carry the same `index` provenance and the same STALE note, in
 * structuredContent AND in the text an agent reads.
 */
class StaleIndexOnAllDocsToolsTest {

    private val segment = TextSegment.from(
        "module_args are declared as a struct module_args and set in chromia.yml.",
        Metadata.from("file_name", "chain_context.md")
    )

    private fun answering(): RagStore = object : RagStore(loadFromRegistry = false) {
        override fun query(query: String): List<TextSegment>? = listOf(segment).also { rememberQueryHits(it) }
        override fun fetchById(id: String): TextSegment? = segment.takeIf { id == segmentId(segment) }
    }

    private val staleAt = Instant.parse("2025-10-21T09:13:14Z")

    private fun stale(): RagStore = answering().also {
        it.provenance = RagStore.Provenance(
            "cached GitLab registry package ${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME} (fetched 2026-09-05)",
            staleAt,
            3208
        )
    }

    private fun fresh(): RagStore = answering().also {
        it.provenance = RagStore.Provenance(
            "GitHub release asset ${RagStore.GITHUB_RELEASE_URL}",
            Instant.now().minus(Duration.ofDays(1)),
            25588
        )
    }

    private suspend fun search(store: RagStore) = SearchDocsStrategy(CompletableDeferred(store)).execute(
        CallToolRequest(name = "search", arguments = buildJsonObject { put("query", "module_args") }),
        ChromiaRepositoryImpl()
    )

    private suspend fun fetch(store: RagStore) = FetchDocumentStrategy(CompletableDeferred(store)).execute(
        CallToolRequest(name = "fetch", arguments = buildJsonObject { put("id", segmentId(segment)) }),
        ChromiaRepositoryImpl()
    )

    private suspend fun fetchDocs(store: RagStore) = FetchDocsStrategy(CompletableDeferred(store)).execute(
        CallToolRequest(name = "fetch_docs", arguments = buildJsonObject { put("query", "module_args") }),
        ChromiaRepositoryImpl()
    )

    @Test
    fun aStaleStoreAnswersWithTheNoteOnAllThreeTools() = runBlocking {
        listOf("fetch_docs" to fetchDocs(stale()), "search" to search(stale()), "fetch" to fetch(stale()))
            .forEach { (tool, result) ->
                val text = (result.content.single() as TextContent).text!!
                assertTrue(
                    Regex("stale", RegexOption.IGNORE_CASE).containsMatchIn(text),
                    "$tool must say the index is stale in the text an agent reads: $text"
                )
                assertTrue(text.contains("generated 2025-10-21"), "$tool: $text")

                val structured = result.structuredContent!!.jsonObject
                val note = structured["index_note"]?.jsonPrimitive?.content
                assertNotNull(note, "$tool must carry index_note in structuredContent")
                assertTrue(note!!.contains("320 days ago") || note.contains("days ago"), "$tool: $note")

                val index = structured["index"]?.jsonObject
                assertNotNull(index, "$tool must name the index it answered from")
                assertEquals320Days(index!!)
            }
    }

    private fun assertEquals320Days(index: kotlinx.serialization.json.JsonObject) {
        assertTrue(index["origin"]!!.jsonPrimitive.content.contains("GitLab"), index.toString())
        assertTrue(index["generated_at"]!!.jsonPrimitive.content.startsWith("2025-10-21"), index.toString())
        assertTrue(index["segments"]!!.jsonPrimitive.content == "3208", index.toString())
        assertTrue(index["stale"]!!.jsonPrimitive.content == "true", index.toString())
    }

    @Test
    fun aFreshStoreAddsNoNoteButStillNamesTheIndexOnAllThree() = runBlocking {
        listOf("fetch_docs" to fetchDocs(fresh()), "search" to search(fresh()), "fetch" to fetch(fresh()))
            .forEach { (tool, result) ->
                val structured = result.structuredContent!!.jsonObject
                assertNull(structured["index_note"], "$tool must not cry stale about a fresh index")
                val index = structured["index"]?.jsonObject
                assertNotNull(index, "$tool names the index even when fresh - a deploy is verified over the wire")
                assertTrue(index!!["stale"]!!.jsonPrimitive.content == "false", index.toString())
            }
    }

    @Test
    fun anIndexOfUnknownProvenanceSaysNothingOnAnyOfThem() = runBlocking {
        listOf("fetch_docs" to fetchDocs(answering()), "search" to search(answering()), "fetch" to fetch(answering()))
            .forEach { (tool, result) ->
                val structured = result.structuredContent!!.jsonObject
                assertNull(structured["index_note"], tool)
                assertNull(structured["index"], "$tool: no provenance, no index object")
            }
    }

    @Test
    fun aFetchMissStillCarriesTheStalenessOfTheIndexThatMissed() = runBlocking {
        val result = FetchDocumentStrategy(CompletableDeferred(stale())).execute(
            CallToolRequest(name = "fetch", arguments = buildJsonObject { put("id", "deadbeef") }),
            ChromiaRepositoryImpl()
        )
        val structured = result.structuredContent!!.jsonObject
        assertNotNull(structured["error"])
        assertNotNull(structured["index_note"], "a miss on a 320-day-old index is a different fact from a miss on a fresh one")
        assertTrue(structured["index"]!!.jsonObject["stale"]!!.jsonPrimitive.content == "true")
    }

    @Test
    fun allThreeSchemasDeclareTheIndexTheyNowReturn() {
        listOf(McpTools.fetchDocsTool(), McpTools.searchTool(), McpTools.fetchTool()).forEach { tool ->
            val props = tool.outputSchema!!.properties
            assertNotNull(props["index"], "${tool.name} returns `index` and must declare it")
            assertNotNull(props["index_note"], "${tool.name} returns `index_note` and must declare it")
        }
    }

    @Test
    fun theStaleTextIsIdenticalOnAllThreeTools() = runBlocking {
        val notes = listOf(fetchDocs(stale()), search(stale()), fetch(stale()))
            .map { it.structuredContent!!.jsonObject["index_note"]!!.jsonPrimitive.content }
            .distinct()
        assertTrue(notes.size == 1, "one index, one sentence about it - got ${notes.size} different notes: $notes")
        assertFalse(notes.single().isBlank())
    }
}
