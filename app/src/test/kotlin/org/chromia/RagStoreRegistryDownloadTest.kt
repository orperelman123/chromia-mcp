package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
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
import org.chromia.tools.embeddingStoreSegments
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.time.Instant
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
        // Unavailable index is an explicit, retryable error - not silent emptiness (audit F5).
        assertEquals(true, search.isError)
        val searchText = (search.content.first() as TextContent).text!!
        assertTrue(searchText.contains("index is unavailable"), searchText)
        assertEquals(0, search.structuredContent!!["results"]!!.jsonArray.size)
        assertFalse(searchText.contains("docs.chromia.com"))

        val fetch = FetchDocumentStrategy(deferred).execute(
            CallToolRequest(name = "fetch", arguments = buildJsonObject { put("id", "https://docs.chromia.com") }),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetch.isError)
        // An unloaded index must not masquerade as "not found" (audit round 4 F3).
        val fetchText = (fetch.content.first() as TextContent).text!!
        assertTrue(fetchText.contains("index is unavailable"), fetchText)
        assertFalse(fetchText.contains("Documentation not found"), fetchText)

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
                    RagStore.downloadRemoteEmbeddings(client),
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
            assertNull(RagStore.downloadRemoteEmbeddings(client), "corrupt registry body must not throw")
        } finally {
            client.close()
        }
    }

    private fun fixtureJson(vararg names: String): String {
        val store = InMemoryEmbeddingStore<TextSegment>()
        names.forEach { name ->
            store.add(Embedding.from(FloatArray(4) { 0.25f }), TextSegment.from("doc $name", Metadata.from("file_name", name)))
        }
        return store.serializeToJson()
    }

    @Test
    fun remoteOrderIsEnvOverrideThenGitHubReleaseThenGitLabPackage() {
        assertEquals(
            listOf(RagStore.GITHUB_RELEASE_URL, "${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}"),
            RagStore.remoteEmbeddingsUrls(emptyMap())
        )
        assertEquals(
            listOf("https://mirror.example/e.json", RagStore.GITHUB_RELEASE_URL, "${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}"),
            RagStore.remoteEmbeddingsUrls(mapOf(RagStore.EMBEDDINGS_URL_ENV to "https://mirror.example/e.json"))
        )
        // A blank override is no override; an override equal to a default is not tried twice.
        assertEquals(2, RagStore.remoteEmbeddingsUrls(mapOf(RagStore.EMBEDDINGS_URL_ENV to "  ")).size)
        assertEquals(2, RagStore.remoteEmbeddingsUrls(mapOf(RagStore.EMBEDDINGS_URL_ENV to RagStore.GITHUB_RELEASE_URL)).size)
    }

    @Test
    fun theGitHubReleaseAssetIsTakenFirstAndItsLastModifiedIsKept() {
        val requested = mutableListOf<String>()
        val engine = MockEngine { request ->
            requested += request.url.toString()
            respond(
                content = fixtureJson("fresh.md"),
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType to listOf("application/octet-stream"),
                    HttpHeaders.LastModified to listOf("Mon, 31 Aug 2026 11:20:00 GMT")
                )
            )
        }
        val client = createRegistryDownloadClient(engine)
        try {
            val remote = RagStore.downloadRemoteEmbeddings(client)!!
            assertEquals(listOf(RagStore.GITHUB_RELEASE_URL), requested, "GitLab must not be contacted when the release asset loads")
            assertEquals(RagStore.GITHUB_RELEASE_URL, remote.url)
            assertEquals(Instant.parse("2026-08-31T11:20:00Z"), remote.lastModified)
            assertEquals("fresh.md", embeddingStoreSegments(remote.store).single().metadata().getString("file_name"))
            assertTrue(RagStore.describeRemote(remote.url).startsWith("GitHub release asset"))
        } finally {
            client.close()
        }
    }

    @Test
    fun aMissingOrCorruptReleaseAssetFallsThroughToTheGitLabPackage() {
        listOf<(String) -> Pair<String, HttpStatusCode>>(
            { _ -> "Not Found" to HttpStatusCode.NotFound },
            { _ -> "<html>rate limited</html>" to HttpStatusCode.TooManyRequests },
            { _ -> "{\"entries\":[{\"id\":\"x\"}]}" to HttpStatusCode.OK } // corrupt: entry without vector
        ).forEach { githubAnswer ->
            val requested = mutableListOf<String>()
            val engine = MockEngine { request ->
                val url = request.url.toString()
                requested += url
                if (url == RagStore.GITHUB_RELEASE_URL) {
                    val (body, status) = githubAnswer(url)
                    respond(body, status)
                } else {
                    respond(fixtureJson("old.md"), HttpStatusCode.OK)
                }
            }
            val client = createRegistryDownloadClient(engine)
            try {
                val remote = RagStore.downloadRemoteEmbeddings(client)!!
                assertEquals(listOf(RagStore.GITHUB_RELEASE_URL, "${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}"), requested)
                assertEquals("${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}", remote.url)
                assertNull(remote.lastModified, "no Last-Modified header on the fallback response")
                assertTrue(RagStore.describeRemote(remote.url).startsWith("GitLab registry package"))
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun theDownloadClientBudgetFitsTheFullStoreNotJustTheOldPackage() {
        // 150 MB at 10 s was a 15 MB/s floor; the whole-request budget must be minutes, with a
        // socket timeout doing the stall detection instead.
        assertTrue(RagStore.REGISTRY_REQUEST_TIMEOUT_MS >= 300_000L, "request budget ${RagStore.REGISTRY_REQUEST_TIMEOUT_MS}")
        assertTrue(RagStore.REGISTRY_SOCKET_TIMEOUT_MS in 10_000L..60_000L, "socket timeout ${RagStore.REGISTRY_SOCKET_TIMEOUT_MS}")
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
