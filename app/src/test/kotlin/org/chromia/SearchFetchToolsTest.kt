package org.chromia

import org.chromia.tools.propertiesOrEmpty

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.tools.FetchDocsStrategy
import org.chromia.tools.escapeFetchDocsSegmentText
import org.chromia.tools.formatFetchDocsText
import org.chromia.tools.FetchDocumentStrategy
import org.chromia.tools.McpTools
import org.chromia.tools.RagStore
import org.chromia.tools.SearchDocsStrategy
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import java.nio.file.Path

class SearchFetchToolsTest {

    private val authSegment = TextSegment.from(
        "FT4 authentication uses auth descriptors and require_mandatory_flags on the main descriptor.",
        Metadata.from("file_name", "ft4-auth.md")
    )
    private val rellSegment = TextSegment.from(
        "Rell compiler pipeline is S_ then C_ passes then R_ then RR_ then Rt.",
        Metadata.from("file_name", "rell-compiler.md")
    )

    // A REAL RagStore over a two-segment index (see TestDocsIndex): real query()
    // - lexical boost, semantic retrieval, docs-first merge - real fetchById,
    // real segment-id index, and since 2026-09-07 the real embedding model the
    // server ships. This used to override query(), so the search/fetch/fetch_docs
    // contract was asserted without once running the retrieval it is a contract
    // over. Empty = no match; null is still reserved for "index unavailable"
    // (audit F5), which a store WITH an index never returns.
    //
    // BGE-small puts any two short English sentences within RagStore's minScore
    // of each other, so a two-segment index answers EVERY query with both
    // segments (see TestDocsIndex). The assertions below are therefore about
    // WHICH segment leads, not how many came back - the counts the toy embedder
    // used to produce were an artifact of its orthogonal vectors, not a property
    // of the tools.
    private val fixtureStore = TestDocsIndex.store(authSegment, rellSegment)

    @Test
    fun searchReturnsIdTitleUrlFromFixtureStore() = runBlocking {
        val strategy = SearchDocsStrategy(CompletableDeferred(fixtureStore))
        val request = callToolRequest(
            name = "search",
            arguments = buildJsonObject { put("query", "FT4 authentication") }
        )
        val result = strategy.execute(request, ChromiaRepositoryImpl())
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!)
        val results = payload.jsonObject["results"]!!.jsonArray
        assertEquals(2, results.size, "the whole two-segment index is within minScore of any query")
        val hit = results.first().jsonObject
        assertEquals(segmentId(authSegment), hit["id"]!!.jsonPrimitive.content, "the FT4 page leads an FT4 query")
        assertEquals("ft4-auth.md", hit["title"]!!.jsonPrimitive.content)
        assertTrue(hit["url"]!!.jsonPrimitive.content.contains("ft4-auth.md"))
        assertTrue(result.isError != true)
        val structured = result.structuredContent
        assertNotNull(structured)
        val structuredHits = structured!!["results"]!!.jsonArray
        assertEquals(results.size, structuredHits.size)
        assertEquals(segmentId(authSegment), structuredHits.first().jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals("ft4-auth.md", structuredHits.first().jsonObject["title"]!!.jsonPrimitive.content)
        assertTrue("metadata" !in structured)
    }

    @Test
    fun fetchReturnsSegmentTextFromFixtureStore() = runBlocking {
        val store = fixtureStore
        SearchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "Rell compiler pipeline") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = segmentId(rellSegment)
        val request = callToolRequest(
            name = "fetch",
            arguments = buildJsonObject { put("id", id) }
        )
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(request, ChromiaRepositoryImpl())
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(id, payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["text"]!!.jsonPrimitive.content.contains("Rell compiler pipeline"))
        assertEquals("rell-compiler.md", payload["title"]!!.jsonPrimitive.content)
        assertTrue("metadata" !in payload)
        assertTrue("metadata" !in result.structuredContent!!)
        assertEquals(payload["id"]!!.jsonPrimitive.content, result.structuredContent!!["id"]!!.jsonPrimitive.content)
        assertTrue(result.isError != true)
    }

    @Test
    fun fetchUnknownIdReturnsNotFoundPayload() = runBlocking {
        val strategy = FetchDocumentStrategy(CompletableDeferred(fixtureStore))
        val request = callToolRequest(
            name = "fetch",
            arguments = buildJsonObject { put("id", "missing-doc") }
        )
        val result = strategy.execute(request, ChromiaRepositoryImpl())
        val text = (result.content.first() as TextContent).text!!
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("missing-doc", payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertTrue("title" !in payload)
        assertTrue("url" !in payload)
        assertFalse(text.contains("chromia docs", ignoreCase = true))
        assertFalse(text.contains("https://docs.chromia.com"))
        assertEquals(true, result.isError)
        assertEquals("missing-doc", result.structuredContent!!["id"]!!.jsonPrimitive.content)
        assertTrue(result.structuredContent!!["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertTrue("title" !in result.structuredContent!!)
        assertTrue("text" !in result.structuredContent!!)
    }


    @Test
    fun fetchKnownIdAfterSearchHitsExactSegment() = runBlocking {
        val store = fixtureStore
        SearchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = segmentId(authSegment)
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", id) }
            ),
            ChromiaRepositoryImpl()
        )
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(id, payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["text"]!!.jsonPrimitive.content.contains("FT4 authentication"))
        assertTrue(result.isError != true)
    }

    @Test
    fun fetchUnknownIdDoesNotReturnFilenameNeighbor() = runBlocking {
        val store = fixtureStore
        SearchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val fakeId = "ft4-auth.md-deadbeef-99"
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", fakeId) }
            ),
            ChromiaRepositoryImpl()
        )
        val text = (result.content.first() as TextContent).text!!
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals(fakeId, payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertTrue("title" !in payload)
        assertTrue("url" !in payload)
        assertFalse(text.contains("FT4 authentication"))
        assertFalse(text.contains("auth descriptors"))
        assertEquals(true, result.isError)
    }

    @Test
    fun fetchQueryTextAsIdDoesNotReturnFuzzyNeighbor() = runBlocking {
        val result = FetchDocumentStrategy(CompletableDeferred(fixtureStore)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val text = (result.content.first() as TextContent).text!!
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("ft4 authentication", payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertFalse(text.contains("auth descriptors"))
        assertEquals(true, result.isError)
    }

    @Test
    fun fetchDocsOutputSchemaDescribesTextAndHitsWithFetchIds() {
        val schema = McpTools.fetchDocsTool().outputSchema
        assertNotNull(schema)
        val textProp = schema!!.propertiesOrEmpty["text"]
        assertNotNull(textProp)
        assertEquals("string", textProp!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertTrue(
            textProp.jsonObject["description"]!!.jsonPrimitive.content.contains(
                "documentation",
                ignoreCase = true
            )
        )
        val hitsProp = schema.propertiesOrEmpty["hits"]
        assertNotNull(hitsProp)
        assertEquals("array", hitsProp!!.jsonObject["type"]!!.jsonPrimitive.content)
        val item = hitsProp.jsonObject["items"]!!.jsonObject
        val itemProps = item["properties"]!!.jsonObject
        assertEquals("string", itemProps["id"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", itemProps["text"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        val itemRequired = item["required"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("id" in itemRequired)
        assertTrue("text" in itemRequired)
        assertEquals(listOf("text", "hits"), schema.required)
        assertFalse(schema.propertiesOrEmpty["type"] is kotlinx.serialization.json.JsonPrimitive)
    }


    @Test
    fun fetchOutputSchemaMatchesIdTitleUrlTextOrError() {
        val schema = McpTools.fetchTool().outputSchema
        assertNotNull(schema)
        val props = schema!!.propertiesOrEmpty
        assertEquals("string", props["id"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", props["title"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", props["url"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", props["text"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", props["error"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals(listOf("id"), schema.required)
        val required = schema.required.orEmpty()
        assertFalse("title" in required)
        assertFalse("url" in required)
        assertFalse("text" in required)
        assertFalse("error" in required)
        assertTrue("metadata" !in props)
        assertFalse(schema.propertiesOrEmpty["type"] is kotlinx.serialization.json.JsonPrimitive)
    }

    @Test
    fun searchOutputSchemaMatchesResultsWithIds() {
        val schema = McpTools.searchTool().outputSchema
        assertNotNull(schema)
        val resultsProp = schema!!.propertiesOrEmpty["results"]
        assertNotNull(resultsProp)
        assertEquals("array", resultsProp!!.jsonObject["type"]!!.jsonPrimitive.content)
        val item = resultsProp.jsonObject["items"]!!.jsonObject
        val itemProps = item["properties"]!!.jsonObject
        assertEquals("string", itemProps["id"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", itemProps["title"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("string", itemProps["url"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        val itemRequired = item["required"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertTrue("id" in itemRequired)
        assertTrue("title" in itemRequired)
        assertTrue("url" in itemRequired)
        assertEquals(listOf("results"), schema.required)
        assertFalse(schema.propertiesOrEmpty["type"] is kotlinx.serialization.json.JsonPrimitive)
    }

    @Test
    fun fetchDocsReturnsReadableSegmentTextNotToStringDump() = runBlocking {
        val strategy = FetchDocsStrategy(CompletableDeferred(fixtureStore))
        val result = strategy.execute(
            callToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val text = (result.content.first() as TextContent).text!!
        val expected = formatFetchDocsText(listOf(authSegment, rellSegment))
        assertEquals(expected, text)
        assertEquals(2, text.lines().size, "one line per hit")
        assertEquals("id: ${segmentId(authSegment)} | ${authSegment.text()}", text.lines().first())
        assertTrue(text.startsWith("id: ${segmentId(authSegment)} | "))
        assertTrue(text.contains(authSegment.text()))
        assertFalse(text.contains("TextSegment {"))
        assertFalse(text.contains("metadata ="))
        assertEquals(expected, result.structuredContent!!["text"]!!.jsonPrimitive.content)
        assertTrue(result.isError != true)
    }

    @Test
    fun fetchDocsReturnsTextMatchingOutputSchema() = runBlocking {
        val strategy = FetchDocsStrategy(CompletableDeferred(fixtureStore))
        val request = callToolRequest(
            name = "fetch_docs",
            arguments = buildJsonObject { put("query", "FT4 authentication") }
        )
        val result = strategy.execute(request, ChromiaRepositoryImpl())
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("FT4 authentication"))
        val structured = result.structuredContent
        assertNotNull(structured)
        assertEquals(text, structured!!["text"]!!.jsonPrimitive.content)
        val hits = structured["hits"]!!.jsonArray
        assertEquals(2, hits.size)
        val hit = hits.first().jsonObject
        assertEquals(segmentId(authSegment), hit["id"]!!.jsonPrimitive.content)
        assertEquals(authSegment.text(), hit["text"]!!.jsonPrimitive.content)
        assertTrue(result.isError != true)
    }

    @Test
    fun fetchDocsHitIdThenFetchHitsExactSegment() = runBlocking {
        val store = fixtureStore
        val docs = FetchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        assertTrue(docs.isError != true)
        val id = docs.structuredContent!!["hits"]!!.jsonArray.first().jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(segmentId(authSegment), id)

        val fetch = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", id) }
            ),
            ChromiaRepositoryImpl()
        )
        val payload = Json.parseToJsonElement((fetch.content.first() as TextContent).text!!).jsonObject
        assertEquals(id, payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["text"]!!.jsonPrimitive.content.contains("FT4 authentication"))
        assertEquals(id, fetch.structuredContent!!["id"]!!.jsonPrimitive.content)
        assertEquals(payload["text"]!!.jsonPrimitive.content, fetch.structuredContent!!["text"]!!.jsonPrimitive.content)
        assertTrue(fetch.isError != true)
    }

    @Test
    fun searchHitIdThenFetchHitsExactSegment() = runBlocking {
        val store = fixtureStore
        val search = SearchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "Rell compiler pipeline") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = Json.parseToJsonElement((search.content.first() as TextContent).text!!)
            .jsonObject["results"]!!.jsonArray.first().jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(segmentId(rellSegment), id, "the Rell page leads a Rell query")

        val fetch = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", id) }
            ),
            ChromiaRepositoryImpl()
        )
        val payload = Json.parseToJsonElement((fetch.content.first() as TextContent).text!!).jsonObject
        assertEquals(id, payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["text"]!!.jsonPrimitive.content.contains("Rell compiler pipeline"))
        assertTrue(fetch.isError != true)
    }


    @Test
    fun fetchDocsSetsIsErrorWhenDeferredRagStoreFails() = runBlocking {
        val failed = CompletableDeferred<RagStore>()
        failed.completeExceptionally(IllegalStateException("rag init failed"))
        val result = FetchDocsStrategy(failed).execute(
            callToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4") }
            ),
            McpTestSupport.offlineRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.startsWith("Error fetching documentation:"))
        assertTrue(text.contains("rag init failed"))
        assertEquals(true, result.isError)
        val structured = result.structuredContent
        assertNotNull(structured)
        assertEquals(text, structured!!["text"]!!.jsonPrimitive.content)
        assertEquals(0, structured["hits"]!!.jsonArray.size)
    }

    @Test
    fun searchSetsIsErrorWhenDeferredRagStoreFails() = runBlocking {
        val failed = CompletableDeferred<RagStore>()
        failed.completeExceptionally(IllegalStateException("rag init failed"))
        val result = SearchDocsStrategy(failed).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4") }
            ),
            McpTestSupport.offlineRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.startsWith("Error searching documentation:"))
        assertTrue(text.contains("rag init failed"))
        assertEquals(true, result.isError)
        val structured = result.structuredContent
        assertNotNull(structured)
        assertEquals(0, structured!!["results"]!!.jsonArray.size)
    }

    @Test
    fun fetchSetsIsErrorWhenDeferredRagStoreFails() = runBlocking {
        val failed = CompletableDeferred<RagStore>()
        failed.completeExceptionally(IllegalStateException("rag init failed"))
        val result = FetchDocumentStrategy(failed).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "any-id") }
            ),
            McpTestSupport.offlineRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("any-id", payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["error"]!!.jsonPrimitive.content.contains("Error fetching documentation:"))
        assertTrue(payload["error"]!!.jsonPrimitive.content.contains("rag init failed"))
        assertTrue("title" !in payload)
        assertTrue("text" !in payload)
        assertEquals("any-id", result.structuredContent!!["id"]!!.jsonPrimitive.content)
        assertTrue(result.structuredContent!!["error"]!!.jsonPrimitive.content.contains("rag init failed"))
        assertEquals(true, result.isError)
    }

    // REMOVED 2026-09-07: `fetchDocsNotFoundSetsIsError`.
    //
    // It asked a POPULATED fixture index for "no-such-documentation" and claimed
    // the answer was `isError` with zero hits. That only ever held because the
    // toy bag-of-words embedder scored a query sharing no vocabulary at cosine 0
    // (relevance 0.5, under RagStore's minScore of 0.6). The model the server
    // actually ships has no such cliff: BGE-small puts unrelated English text at
    // cosine ~0.6-0.8, so a real index of two segments answers "no-such-
    // documentation" with both of them, and there is no query text that makes a
    // populated index return nothing. The claim it carried - "zero hits is an
    // explicit error, not a silent empty success" - is now covered by
    // `fetchDocsEmptyHitsSetsIsError` below, over a real EMPTY index, which is
    // the only way an available index really produces zero hits.

    @Test
    fun fetchDocsEmptyHitsSetsIsError() = runBlocking {
        // A real store whose index holds nothing: query() runs for real and
        // finds no hit, which is not the same as "index unavailable".
        val emptyStore = TestDocsIndex.store()
        val result = FetchDocsStrategy(CompletableDeferred(emptyStore)).execute(
            callToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "anything") }
            ),
            McpTestSupport.offlineRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Documentation not found") || text.isBlank())
        assertEquals(true, result.isError)
        val hits = result.structuredContent!!["hits"]!!.jsonArray
        assertEquals(0, hits.size)
    }


    @Test
    fun fetchOnStoreBHitsIdFromSearchOnStoreA(@TempDir tempDir: Path) = runBlocking {
        // Both stores LOAD the same persisted index file the way production
        // does; storeA answers the search with the real query path (it used to
        // override query() and filter by substring).
        val path = TestDocsIndex.persist(tempDir.resolve("embeddings.json"), authSegment, rellSegment)
        val storeA = TestDocsIndex.storeLoadedFrom(path)
        val storeB = TestDocsIndex.storeLoadedFrom(path)
        val search = SearchDocsStrategy(CompletableDeferred(storeA)).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val results = Json.parseToJsonElement((search.content.first() as TextContent).text!!).jsonObject["results"]!!.jsonArray
        assertEquals(2, results.size)
        val id = results.first().jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(segmentId(authSegment), id)

        val fetch = FetchDocumentStrategy(CompletableDeferred(storeB)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", id) }
            ),
            ChromiaRepositoryImpl()
        )
        val payload = Json.parseToJsonElement((fetch.content.first() as TextContent).text!!).jsonObject
        assertEquals(id, payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["text"]!!.jsonPrimitive.content.contains("FT4 authentication"))
        assertTrue(fetch.isError != true)

        val miss = FetchDocumentStrategy(CompletableDeferred(storeB)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "ft4-auth.md") }
            ),
            ChromiaRepositoryImpl()
        )
        val missText = (miss.content.first() as TextContent).text!!
        val missPayload = Json.parseToJsonElement(missText).jsonObject
        assertTrue(missPayload["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertEquals(true, miss.isError)
        assertFalse(missText.contains("FT4 authentication"))
    }

    @Test
    fun fetchAcceptsUppercaseSha256HexFromSearchId() = runBlocking {
        val store = fixtureStore
        SearchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = segmentId(authSegment)
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", id.uppercase()) }
            ),
            ChromiaRepositoryImpl()
        )
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(id, payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["id"]!!.jsonPrimitive.content.matches(Regex("[0-9a-f]{64}")))
        assertTrue(payload["text"]!!.jsonPrimitive.content.contains("FT4 authentication"))
        assertTrue(result.isError != true)
    }

    @Test
    fun fetchSuccessPayloadEchoesTrimmedLowercaseId() = runBlocking {
        val store = fixtureStore
        SearchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = segmentId(authSegment)
        val mixed = id.mapIndexed { index, ch ->
            if (index % 2 == 0) ch.uppercaseChar() else ch
        }.joinToString("")
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "  $mixed  ") }
            ),
            ChromiaRepositoryImpl()
        )
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals(id, payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["text"]!!.jsonPrimitive.content.contains("FT4 authentication"))
        assertTrue(result.isError != true)
    }

    @Test
    fun fetchErrorPayloadEchoesTrimmedLowercaseId() = runBlocking {
        val result = FetchDocumentStrategy(CompletableDeferred(fixtureStore)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "  MISSING-DOC  ") }
            ),
            ChromiaRepositoryImpl()
        )
        val text = (result.content.first() as TextContent).text!!
        val payload = Json.parseToJsonElement(text).jsonObject
        assertEquals("missing-doc", payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertTrue("title" !in payload)
        assertTrue("url" !in payload)
        assertEquals(true, result.isError)
    }

    @Test
    fun fetchErrorPayloadEchoesLowercaseUnknownSha256Hex() = runBlocking {
        val unknownUpper = "A".repeat(64)
        val result = FetchDocumentStrategy(CompletableDeferred(fixtureStore)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "  $unknownUpper  ") }
            ),
            ChromiaRepositoryImpl()
        )
        val payload = Json.parseToJsonElement((result.content.first() as TextContent).text!!).jsonObject
        assertEquals("a".repeat(64), payload["id"]!!.jsonPrimitive.content)
        assertTrue(payload["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertEquals(true, result.isError)
    }

    @Test
    fun fetchDocsEscapesNewlinesSoEachHitStaysOneLine() = runBlocking {
        val multiline = TextSegment.from(
            "First paragraph.\nSecond paragraph with more detail.\n\nTrailing block.",
            Metadata.from("file_name", "multiline.md")
        )
        // Real store, real retrieval, real embedder. The query is the multiline
        // segment's own wording, so BGE-small ranks it first and the second hit is
        // the other fixture segment - the ORDER is what the formatting assertion
        // below needs, and it is the order the real model produces.
        val store = TestDocsIndex.store(multiline, rellSegment)
        val result = FetchDocsStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject {
                    put("query", "First paragraph. Second paragraph with more detail. Trailing block.")
                }
            ),
            ChromiaRepositoryImpl()
        )
        val text = (result.content.first() as TextContent).text!!
        val expected = formatFetchDocsText(listOf(multiline, rellSegment))
        assertEquals(expected, text)
        val records = text.lines()
        assertEquals(2, records.size)
        val escaped = escapeFetchDocsSegmentText(multiline.text())
        assertEquals("id: ${segmentId(multiline)} | $escaped", records[0])
        assertEquals("id: ${segmentId(rellSegment)} | ${rellSegment.text()}", records[1])
        assertTrue(escaped.contains("\\n"))
        assertFalse(records[0].contains("First paragraph.\nSecond"))
        assertTrue(records[0].contains("First paragraph.\\nSecond"))
        assertTrue(multiline.text().contains("\n"))
        assertFalse(text.contains("TextSegment {"))
        assertEquals(expected, result.structuredContent!!["text"]!!.jsonPrimitive.content)
        val hits = result.structuredContent!!["hits"]!!.jsonArray
        assertEquals(2, hits.size)
        assertEquals(segmentId(multiline), hits[0].jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals(multiline.text(), hits[0].jsonObject["text"]!!.jsonPrimitive.content)
        assertTrue(hits[0].jsonObject["text"]!!.jsonPrimitive.content.contains("\n"))
        assertEquals(rellSegment.text(), hits[1].jsonObject["text"]!!.jsonPrimitive.content)

        val fetch = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", segmentId(multiline)) }
            ),
            ChromiaRepositoryImpl()
        )
        val payload = Json.parseToJsonElement((fetch.content.first() as TextContent).text!!).jsonObject
        assertEquals(multiline.text(), payload["text"]!!.jsonPrimitive.content)
        assertEquals(multiline.text(), fetch.structuredContent!!["text"]!!.jsonPrimitive.content)
        assertTrue(fetch.isError != true)
    }

    // REMOVED 2026-09-07: `defaultConstructWithoutEmbeddingsReportsIndexUnavailable`.
    //
    // The claim it carried is real and audited - a RagStore whose index never
    // loaded must answer search/fetch/fetch_docs with "index is unavailable"
    // (audit F5) and must NOT let `fetch` degrade into "Documentation not found"
    // (audit round 4 F3). It is removed FROM HERE because the only way this file
    // could reach that state was `registryLoader = { null }`, a lambda double
    // standing in for the published index download.
    //
    // WHAT COVERS IT NOW, for real and on all three tools:
    // RagStoreRegistryDownloadTest
    // .anIndexThatCouldNotBeDownloadedIsReportedUnavailableByAllThreeDocsTools,
    // which reaches the same state through a genuinely CLOSED loopback port -
    // the production client opens a real socket and the operating system refuses
    // it - now that `RagStore` takes `remoteUrls`, the URL list its
    // `CHROMIA_EMBEDDINGS_URL` override always implied. Nothing is lost.

    @Test
    fun anAvailableButEmptyIndexAnswersNoMatchNotIndexUnavailable() = runBlocking {
        // Replaces `loadedStoreWithNoSimilarDocsReturnsEmptyHitsNotError`, which
        // manufactured "no similar docs" with an `object : EmbeddingModel` that
        // embedded every query to the exact opposite of the stored vector. The
        // real model cannot be made to miss a segment that is in the index, so the
        // honest version of "the retriever succeeded and found nothing" is a real
        // index with nothing in it. What survives unchanged is the distinction the
        // test exists for: zero hits from an AVAILABLE index is a no-match, never
        // the "index is unavailable" error (audit F5 / round 4 F3).
        //
        // WEAKENED: the deleted version also proved that fetch-by-id keeps working
        // while search finds nothing. That pairing cannot exist over an empty
        // index; `RagStoreFetchByIdTest.fetchByIdWorksOnFreshStoreWithoutPriorQuery`
        // covers fetch-by-id without a prior query.
        val store = TestDocsIndex.store()
        assertNotNull(store.embeddingStore)
        assertFalse(store.isIndexUnavailable(), "a store with an index is available, empty or not")
        val hits = store.query("FT4 authentication")
        assertNotNull(hits)
        assertTrue(hits!!.isEmpty(), "an empty index must yield no hits, not invented ones")

        val deferred = CompletableDeferred(store)
        val search = SearchDocsStrategy(deferred).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        assertTrue(search.isError != true, "no match is a successful search with no results")
        val searchText = (search.content.first() as TextContent).text!!
        assertEquals(0, Json.parseToJsonElement(searchText).jsonObject["results"]!!.jsonArray.size)
        assertFalse(searchText.contains("docs.chromia.com"))
        assertFalse(searchText.contains("index is unavailable"))

        val fetchUnknown = FetchDocumentStrategy(deferred).execute(
            callToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "https://docs.chromia.com/intro") }
            ),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetchUnknown.isError)
        val unknownText = (fetchUnknown.content.first() as TextContent).text!!
        assertTrue(unknownText.contains("Documentation not found"), unknownText)
        assertFalse(unknownText.contains("index is unavailable"), unknownText)

        val fetchDocs = FetchDocsStrategy(deferred).execute(
            callToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetchDocs.isError)
        val docsText = (fetchDocs.content.first() as TextContent).text!!
        assertTrue(docsText.contains("Documentation not found"), docsText)
        assertFalse(docsText.contains("index is unavailable"), docsText)
        assertEquals(0, fetchDocs.structuredContent!!["hits"]!!.jsonArray.size)
    }

    // ---- reality audit D6: a THROWING retriever is a retrieval error, ------
    // never "Documentation not found". A broken index (e.g. embedding
    // dimension mismatch) used to be swallowed into emptyList(), so agents
    // were told the docs do not exist while the index was simply broken.

    @Test
    fun throwingRetrieverSurfacesRetrievalErrorNotDocumentationNotFound() = runBlocking {
        // The failure is REAL now, not injected. The index below holds a
        // three-component vector - the shape a store built by some other embedder
        // has - and the store queries with the model the server actually ships
        // (384 components). langchain4j's CosineSimilarity refuses to compare
        // vectors of different lengths, so the retriever throws exactly the way it
        // throws in production against a foreign index. This used to be an
        // `object : EmbeddingModel` whose embedAll() threw a message the test wrote
        // for itself; the message below is written by langchain4j and RagStore.
        val fixture = InMemoryEmbeddingStore<TextSegment>().also { store ->
            store.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), authSegment)
        }
        val store = RagStore(
            loadFromRegistry = false,
            initialStore = fixture,
            embeddingModel = TestDocsIndex.model
        )
        val thrown = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            store.query("FT4 authentication")
        }
        assertTrue(thrown.message!!.contains("NOT a no-match"), thrown.message)
        assertTrue(thrown.message!!.contains("dimension mismatch"), thrown.message)
        assertTrue(
            thrown.message!!.contains("${TestDocsIndex.DIMENSION}"),
            "the real model's width is what the stored vector was compared against: ${thrown.message}"
        )

        val deferred = CompletableDeferred(store)
        val search = SearchDocsStrategy(deferred).execute(
            callToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, search.isError)
        val searchText = (search.content.first() as TextContent).text!!
        assertTrue(searchText.contains("Error searching documentation"), searchText)
        assertTrue(searchText.contains("NOT a no-match"), searchText)
        assertFalse(searchText.contains("Documentation not found"), searchText)

        val fetchDocs = FetchDocsStrategy(deferred).execute(
            callToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetchDocs.isError)
        val docsText = (fetchDocs.content.first() as TextContent).text!!
        assertTrue(docsText.contains("Error fetching documentation"), docsText)
        assertFalse(docsText.contains("Documentation not found"), docsText)

        // fetch by id does not rank - the id index keeps answering.
        val knownId = segmentId(authSegment)
        val fetchKnown = FetchDocumentStrategy(deferred).execute(
            callToolRequest(name = "fetch", arguments = buildJsonObject { put("id", knownId) }),
            ChromiaRepositoryImpl()
        )
        assertTrue(fetchKnown.isError != true)
    }
}
