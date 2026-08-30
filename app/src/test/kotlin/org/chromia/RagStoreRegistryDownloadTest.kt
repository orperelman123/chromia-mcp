package org.chromia

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
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
import org.chromia.tools.FetchDocumentStrategy
import org.chromia.tools.RagStore
import org.chromia.tools.SearchDocsStrategy
import org.chromia.tools.createRegistryDownloadClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class RagStoreRegistryDownloadTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun missingLocalAndThrowingRegistryDoesNotCrashOrInventDocs() = runBlocking {
        val registryCalls = AtomicInteger(0)
        val ragStore = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = tempDir.resolve("missing-embeddings.json"),
            registryLoader = {
                registryCalls.incrementAndGet()
                error("401 Unauthorized: missing GITLAB_ACCESS_TOKEN")
            }
        )
        assertEquals(1, registryCalls.get())
        assertNull(ragStore.embeddingStore)
        assertTrue(ragStore.query("FT4 tokens").isNullOrEmpty())

        val deferred = CompletableDeferred(ragStore)
        val search = SearchDocsStrategy(deferred).execute(
            CallToolRequest(name = "search", arguments = buildJsonObject { put("query", "FT4 tokens") }),
            ChromiaRepositoryImpl()
        )
        assertTrue(search.isError != true)
        val searchText = (search.content.first() as TextContent).text!!
        assertEquals(0, Json.parseToJsonElement(searchText).jsonObject["results"]!!.jsonArray.size)
        assertFalse(searchText.contains("docs.chromia.com"))

        val fetch = FetchDocumentStrategy(deferred).execute(
            CallToolRequest(name = "fetch", arguments = buildJsonObject { put("id", "https://docs.chromia.com") }),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetch.isError)
        assertTrue((fetch.content.first() as TextContent).text!!.contains("Documentation not found"))

        val fetchDocs = FetchDocsStrategy(deferred).execute(
            CallToolRequest(name = "fetch_docs", arguments = buildJsonObject { put("query", "FT4 tokens") }),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetchDocs.isError)
        assertEquals(0, fetchDocs.structuredContent!!["hits"]!!.jsonArray.size)
    }

    @Test
    fun downloadFromRegistryHttpFailuresAreSkippedWithoutLiveNetwork() {
        listOf(
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            HttpStatusCode.NotFound
        ).forEach { status ->
            val engine = MockEngine { request ->
                assertTrue(
                    request.url.toString().contains("embeddings.json"),
                    request.url.toString()
                )
                respond(
                    content = "denied",
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "text/plain")
                )
            }
            val client = createRegistryDownloadClient(engine)
            try {
                assertNull(
                    RagStore.downloadFromRegistry(client),
                    "HTTP $status must skip the registry store"
                )
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun downloadFromRegistryCorruptBodyIsSkippedWithoutLiveNetwork() {
        val engine = MockEngine {
            respond(
                content = "not-an-embedding-store",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = createRegistryDownloadClient(engine)
        try {
            assertNull(RagStore.downloadFromRegistry(client), "corrupt registry body must not throw")
        } finally {
            client.close()
        }
    }

    @Test
    fun registryDownloadClientHasTimeoutsAndDoesNotExpectSuccess() {
        val engine = MockEngine {
            respond("denied", HttpStatusCode.Unauthorized)
        }
        val client = createRegistryDownloadClient(engine)
        try {
            assertTrue(client.pluginOrNull(HttpTimeout) != null)
        } finally {
            client.close()
        }
    }
}
