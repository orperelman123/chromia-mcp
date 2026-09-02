package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.embeddingStoreSegments
import org.chromia.tools.persistLocalEmbeddings
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

    private val fixtureStore = object : RagStore(loadFromRegistry = false) {
        override fun query(query: String): List<TextSegment>? {
            val hits = listOf(authSegment, rellSegment).filter { segment ->
                segment.text().contains(query, ignoreCase = true) ||
                    (segment.metadata()?.getString("file_name")?.contains(query, ignoreCase = true) == true)
            }
            // Empty = no match; null is reserved for "index unavailable" (audit F5).
            return hits.also { rememberQueryHits(it) }
        }
    }

    @Test
    fun searchReturnsIdTitleUrlFromFixtureStore() = runBlocking {
        val strategy = SearchDocsStrategy(CompletableDeferred(fixtureStore))
        val request = CallToolRequest(
            name = "search",
            arguments = buildJsonObject { put("query", "FT4 authentication") }
        )
        val result = strategy.execute(request, ChromiaRepositoryImpl())
        val payload = Json.parseToJsonElement((result.content.first() as io.modelcontextprotocol.kotlin.sdk.TextContent).text!!)
        val results = payload.jsonObject["results"]!!.jsonArray
        assertEquals(1, results.size)
        val hit = results.first().jsonObject
        assertEquals(segmentId(authSegment), hit["id"]!!.jsonPrimitive.content)
        assertEquals("ft4-auth.md", hit["title"]!!.jsonPrimitive.content)
        assertTrue(hit["url"]!!.jsonPrimitive.content.contains("ft4-auth.md"))
        assertTrue(result.isError != true)
        val structured = result.structuredContent
        assertNotNull(structured)
        val structuredHits = structured!!["results"]!!.jsonArray
        assertEquals(1, structuredHits.size)
        assertEquals(segmentId(authSegment), structuredHits.first().jsonObject["id"]!!.jsonPrimitive.content)
        assertEquals("ft4-auth.md", structuredHits.first().jsonObject["title"]!!.jsonPrimitive.content)
        assertTrue("metadata" !in structured)
    }

    @Test
    fun fetchReturnsSegmentTextFromFixtureStore() = runBlocking {
        val store = fixtureStore
        SearchDocsStrategy(CompletableDeferred(store)).execute(
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "Rell compiler pipeline") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = segmentId(rellSegment)
        val request = CallToolRequest(
            name = "fetch",
            arguments = buildJsonObject { put("id", id) }
        )
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(request, ChromiaRepositoryImpl())
        val payload = Json.parseToJsonElement((result.content.first() as io.modelcontextprotocol.kotlin.sdk.TextContent).text!!).jsonObject
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
        val request = CallToolRequest(
            name = "fetch",
            arguments = buildJsonObject { put("id", "missing-doc") }
        )
        val result = strategy.execute(request, ChromiaRepositoryImpl())
        val text = (result.content.first() as io.modelcontextprotocol.kotlin.sdk.TextContent).text!!
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
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = segmentId(authSegment)
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            CallToolRequest(
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
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val fakeId = "ft4-auth.md-deadbeef-99"
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            CallToolRequest(
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
            CallToolRequest(
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
        val textProp = schema!!.properties["text"]
        assertNotNull(textProp)
        assertEquals("string", textProp!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertTrue(
            textProp.jsonObject["description"]!!.jsonPrimitive.content.contains(
                "documentation",
                ignoreCase = true
            )
        )
        val hitsProp = schema.properties["hits"]
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
        assertFalse(schema.properties["type"] is kotlinx.serialization.json.JsonPrimitive)
    }


    @Test
    fun fetchOutputSchemaMatchesIdTitleUrlTextOrError() {
        val schema = McpTools.fetchTool().outputSchema
        assertNotNull(schema)
        val props = schema!!.properties
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
        assertFalse(schema.properties["type"] is kotlinx.serialization.json.JsonPrimitive)
    }

    @Test
    fun searchOutputSchemaMatchesResultsWithIds() {
        val schema = McpTools.searchTool().outputSchema
        assertNotNull(schema)
        val resultsProp = schema!!.properties["results"]
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
        assertFalse(schema.properties["type"] is kotlinx.serialization.json.JsonPrimitive)
    }

    @Test
    fun fetchDocsReturnsReadableSegmentTextNotToStringDump() = runBlocking {
        val strategy = FetchDocsStrategy(CompletableDeferred(fixtureStore))
        val result = strategy.execute(
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val text = (result.content.first() as TextContent).text!!
        val expected = formatFetchDocsText(listOf(authSegment))
        assertEquals(expected, text)
        assertEquals("id: ${segmentId(authSegment)} | ${authSegment.text()}", text)
        assertEquals(1, text.lines().size)
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
        val request = CallToolRequest(
            name = "fetch_docs",
            arguments = buildJsonObject { put("query", "FT4 authentication") }
        )
        val result = strategy.execute(request, ChromiaRepositoryImpl())
        val text = (result.content.first() as io.modelcontextprotocol.kotlin.sdk.TextContent).text!!
        assertTrue(text.contains("FT4 authentication"))
        val structured = result.structuredContent
        assertNotNull(structured)
        assertEquals(text, structured!!["text"]!!.jsonPrimitive.content)
        val hits = structured["hits"]!!.jsonArray
        assertEquals(1, hits.size)
        val hit = hits.first().jsonObject
        assertEquals(segmentId(authSegment), hit["id"]!!.jsonPrimitive.content)
        assertEquals(authSegment.text(), hit["text"]!!.jsonPrimitive.content)
        assertTrue(result.isError != true)
    }

    @Test
    fun fetchDocsHitIdThenFetchHitsExactSegment() = runBlocking {
        val store = fixtureStore
        val docs = FetchDocsStrategy(CompletableDeferred(store)).execute(
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        assertTrue(docs.isError != true)
        val id = docs.structuredContent!!["hits"]!!.jsonArray.first().jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(segmentId(authSegment), id)

        val fetch = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            CallToolRequest(
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
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "Rell compiler pipeline") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = Json.parseToJsonElement((search.content.first() as TextContent).text!!)
            .jsonObject["results"]!!.jsonArray.first().jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(segmentId(rellSegment), id)

        val fetch = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            CallToolRequest(
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
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4") }
            ),
            RecordingRepository()
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
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4") }
            ),
            RecordingRepository()
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
            CallToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "any-id") }
            ),
            RecordingRepository()
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

    @Test
    fun fetchDocsNotFoundSetsIsError() = runBlocking {
        val strategy = FetchDocsStrategy(CompletableDeferred(fixtureStore))
        val result = strategy.execute(
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "no-such-documentation") }
            ),
            RecordingRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Documentation not found"))
        assertEquals(true, result.isError)
        val hits = result.structuredContent!!["hits"]!!.jsonArray
        assertEquals(0, hits.size)
    }


    @Test
    fun fetchDocsEmptyHitsSetsIsError() = runBlocking {
        val emptyStore = object : RagStore(loadFromRegistry = false) {
            override fun query(query: String) = emptyList<TextSegment>()
        }
        val result = FetchDocsStrategy(CompletableDeferred(emptyStore)).execute(
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "anything") }
            ),
            RecordingRepository()
        )
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("Documentation not found") || text.isBlank())
        assertEquals(true, result.isError)
        val hits = result.structuredContent!!["hits"]!!.jsonArray
        assertEquals(0, hits.size)
    }


    @Test
    fun fetchOnStoreBHitsIdFromSearchOnStoreA(@TempDir tempDir: Path) = runBlocking {
        val path = tempDir.resolve("embeddings.json")
        InMemoryEmbeddingStore<TextSegment>().also { store ->
            store.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), authSegment)
            store.add(Embedding.from(floatArrayOf(0.2f, 0.1f, 0.3f)), rellSegment)
            persistLocalEmbeddings(store, path)
        }
        val storeA = object : RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = path,
            registryLoader = { null }
        ) {
            override fun query(query: String): List<TextSegment>? {
                val hits = embeddingStoreSegments(embeddingStore ?: return null).filter { segment ->
                    segment.text().contains(query, ignoreCase = true)
                }
                return hits.ifEmpty { null }
            }
        }
        val storeB = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = path,
            registryLoader = { null }
        )
        val search = SearchDocsStrategy(CompletableDeferred(storeA)).execute(
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val results = Json.parseToJsonElement((search.content.first() as TextContent).text!!).jsonObject["results"]!!.jsonArray
        assertEquals(1, results.size)
        val id = results.first().jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(segmentId(authSegment), id)

        val fetch = FetchDocumentStrategy(CompletableDeferred(storeB)).execute(
            CallToolRequest(
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
            CallToolRequest(
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
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        val id = segmentId(authSegment)
        val result = FetchDocumentStrategy(CompletableDeferred(store)).execute(
            CallToolRequest(
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
            CallToolRequest(
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
            CallToolRequest(
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
            CallToolRequest(
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
            CallToolRequest(
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
        val store = object : RagStore(loadFromRegistry = false) {
            override fun query(query: String): List<TextSegment>? {
                return listOf(multiline, rellSegment).also { rememberQueryHits(it) }
            }
        }
        val result = FetchDocsStrategy(CompletableDeferred(store)).execute(
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "paragraph") }
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
            CallToolRequest(
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

    @Test
    fun defaultConstructWithoutEmbeddingsReportsIndexUnavailable(@TempDir tempDir: Path) = runBlocking {
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = tempDir.resolve("missing-embeddings.json"),
            registryLoader = { null }
        )
        assertNull(store.embeddingStore)
        assertNull(store.query("https://docs.chromia.com/intro"))

        val deferred = CompletableDeferred(store)

        val search = SearchDocsStrategy(deferred).execute(
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "https://docs.chromia.com/intro") }
            ),
            ChromiaRepositoryImpl()
        )
        // Index unavailable is now an explicit error, not silent emptiness (audit F5).
        assertEquals(true, search.isError)
        val searchText = (search.content.first() as TextContent).text!!
        assertTrue(searchText.contains("index is unavailable"), searchText)
        assertEquals(0, search.structuredContent!!["results"]!!.jsonArray.size)
        assertFalse(searchText.contains("docs.chromia.com"))
        assertFalse(searchText.contains("https://docs.chromia.com"))

        val fetch = FetchDocumentStrategy(deferred).execute(
            CallToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "https://docs.chromia.com/intro") }
            ),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetch.isError)
        val fetchText = (fetch.content.first() as TextContent).text!!
        val fetchPayload = Json.parseToJsonElement(fetchText).jsonObject
        assertEquals("https://docs.chromia.com/intro", fetchPayload["id"]!!.jsonPrimitive.content)
        // An unloaded index must not masquerade as "not found" (audit round 4 F3).
        assertTrue(fetchPayload["error"]!!.jsonPrimitive.content.contains("index is unavailable"))
        assertFalse(fetchPayload["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
        assertTrue("title" !in fetchPayload)
        assertTrue("text" !in fetchPayload)
        assertTrue("url" !in fetchPayload)
        assertFalse(fetchText.contains("chromia docs", ignoreCase = true))

        val fetchDocs = FetchDocsStrategy(deferred).execute(
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "https://docs.chromia.com/intro") }
            ),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetchDocs.isError)
        val docsText = (fetchDocs.content.first() as TextContent).text!!
        assertTrue(docsText.contains("index is unavailable"), docsText)
        assertEquals(0, fetchDocs.structuredContent!!["hits"]!!.jsonArray.size)
        assertFalse(docsText.contains("https://docs.chromia.com/intro"))
    }

    @Test
    fun loadedStoreWithNoSimilarDocsReturnsEmptyHitsNotError() = runBlocking {
        // Deterministic no-match: the injected model embeds every query to the
        // OPPOSITE of the stored vector (cosine -1, far below minScore), so the
        // retriever succeeds with zero hits. (The previous version of this test
        // left embeddingModel null and relied on the SPI model's dimension
        // mismatch being swallowed into emptyList() - reality audit D6 made
        // that swallow an explicit retrieval error, tested separately below.)
        val fixture = InMemoryEmbeddingStore<TextSegment>().also { store ->
            store.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), authSegment)
        }
        val store = RagStore(
            loadFromRegistry = false,
            initialStore = fixture,
            embeddingModel = fixedVectorModel(floatArrayOf(-0.1f, -0.2f, -0.3f))
        )
        val hits = store.query("FT4 authentication")
        assertNotNull(hits)
        assertTrue(hits!!.isEmpty(), "a dissimilar query must yield no hits, not invented ones")

        val deferred = CompletableDeferred(store)
        val search = SearchDocsStrategy(deferred).execute(
            CallToolRequest(
                name = "search",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        assertTrue(search.isError != true)
        val searchText = (search.content.first() as TextContent).text!!
        assertEquals(0, Json.parseToJsonElement(searchText).jsonObject["results"]!!.jsonArray.size)
        assertFalse(searchText.contains("docs.chromia.com"))

        val knownId = segmentId(authSegment)
        val fetchKnown = FetchDocumentStrategy(deferred).execute(
            CallToolRequest(name = "fetch", arguments = buildJsonObject { put("id", knownId) }),
            ChromiaRepositoryImpl()
        )
        assertTrue(fetchKnown.isError != true)
        val knownPayload = Json.parseToJsonElement((fetchKnown.content.first() as TextContent).text!!).jsonObject
        assertEquals(knownId, knownPayload["id"]!!.jsonPrimitive.content)
        assertTrue(knownPayload["text"]!!.jsonPrimitive.content.contains("FT4 authentication"))

        val fetchUnknown = FetchDocumentStrategy(deferred).execute(
            CallToolRequest(
                name = "fetch",
                arguments = buildJsonObject { put("id", "https://docs.chromia.com/intro") }
            ),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetchUnknown.isError)
        val unknownText = (fetchUnknown.content.first() as TextContent).text!!
        assertTrue(unknownText.contains("Documentation not found"))
        assertFalse(unknownText.contains("FT4 authentication"))

        val fetchDocs = FetchDocsStrategy(deferred).execute(
            CallToolRequest(
                name = "fetch_docs",
                arguments = buildJsonObject { put("query", "FT4 authentication") }
            ),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetchDocs.isError)
        assertTrue((fetchDocs.content.first() as TextContent).text!!.contains("Documentation not found"))
        assertEquals(0, fetchDocs.structuredContent!!["hits"]!!.jsonArray.size)
    }

    // ---- reality audit D6: a THROWING retriever is a retrieval error, ------
    // never "Documentation not found". A broken index (e.g. embedding
    // dimension mismatch) used to be swallowed into emptyList(), so agents
    // were told the docs do not exist while the index was simply broken.

    @Test
    fun throwingRetrieverSurfacesRetrievalErrorNotDocumentationNotFound() = runBlocking {
        val fixture = InMemoryEmbeddingStore<TextSegment>().also { store ->
            store.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), authSegment)
        }
        val store = RagStore(
            loadFromRegistry = false,
            initialStore = fixture,
            embeddingModel = throwingModel("embedding dimension mismatch: 384 vs 3")
        )
        val thrown = org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException::class.java) {
            store.query("FT4 authentication")
        }
        assertTrue(thrown.message!!.contains("NOT a no-match"), thrown.message)
        assertTrue(thrown.message!!.contains("dimension mismatch"), thrown.message)

        val deferred = CompletableDeferred(store)
        val search = SearchDocsStrategy(deferred).execute(
            CallToolRequest(
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
            CallToolRequest(
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
            CallToolRequest(name = "fetch", arguments = buildJsonObject { put("id", knownId) }),
            ChromiaRepositoryImpl()
        )
        assertTrue(fetchKnown.isError != true)
    }

    /** Embeds every text to the same [vector] - deterministic similarity. */
    private fun fixedVectorModel(vector: FloatArray): dev.langchain4j.model.embedding.EmbeddingModel =
        object : dev.langchain4j.model.embedding.EmbeddingModel {
            override fun embedAll(
                segments: List<TextSegment>
            ): dev.langchain4j.model.output.Response<List<Embedding>> =
                dev.langchain4j.model.output.Response.from(segments.map { Embedding.from(vector) })
        }

    /** Every embed attempt throws - deterministic retrieval failure. */
    private fun throwingModel(message: String): dev.langchain4j.model.embedding.EmbeddingModel =
        object : dev.langchain4j.model.embedding.EmbeddingModel {
            override fun embedAll(
                segments: List<TextSegment>
            ): dev.langchain4j.model.output.Response<List<Embedding>> =
                throw RuntimeException(message)
        }
}
