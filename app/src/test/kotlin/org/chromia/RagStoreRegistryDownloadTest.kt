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
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
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
import java.nio.file.Files
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
            callToolRequest(name = "search", arguments = buildJsonObject { put("query", "FT4 tokens") }),
            ChromiaRepositoryImpl()
        )
        // Unavailable index is an explicit, retryable error - not silent emptiness (audit F5).
        assertEquals(true, search.isError)
        val searchText = (search.content.first() as TextContent).text!!
        assertTrue(searchText.contains("index is unavailable"), searchText)
        assertEquals(0, search.structuredContent!!["results"]!!.jsonArray.size)
        assertFalse(searchText.contains("docs.chromia.com"))

        val fetch = FetchDocumentStrategy(deferred).execute(
            callToolRequest(name = "fetch", arguments = buildJsonObject { put("id", "https://docs.chromia.com") }),
            ChromiaRepositoryImpl()
        )
        assertEquals(true, fetch.isError)
        // An unloaded index must not masquerade as "not found" (audit round 4 F3).
        val fetchText = (fetch.content.first() as TextContent).text!!
        assertTrue(fetchText.contains("index is unavailable"), fetchText)
        assertFalse(fetchText.contains("Documentation not found"), fetchText)

        val fetchDocs = FetchDocsStrategy(deferred).execute(
            callToolRequest(name = "fetch_docs", arguments = buildJsonObject { put("query", "FT4 tokens") }),
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
                    RagStore.downloadRemoteEmbeddings(client, token = null),
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
            assertNull(RagStore.downloadRemoteEmbeddings(client, token = null), "corrupt registry body must not throw")
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
            val remote = RagStore.downloadRemoteEmbeddings(client, token = null)!!
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
                val remote = RagStore.downloadRemoteEmbeddings(client, token = null)!!
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
    fun withATokenThePrivateReleaseAssetIsResolvedThroughTheApiAndTheCredentialStopsAtGitHub() {
        val seen = mutableListOf<Pair<String, Map<String, String>>>()
        val storage = "https://objects.githubusercontent.com/github-production-release-asset/abc?X-Amz-Signature=sig"
        val engine = MockEngine { request ->
            val url = request.url.toString()
            seen += url to request.headers.names().associateWith { request.headers[it]!! }
            when {
                url == RagStore.GITHUB_RELEASE_API_URL -> {
                    assertEquals("Bearer t0k3n", request.headers[HttpHeaders.Authorization])
                    respond(
                        """{"tag_name":"embeddings","assets":[
                             {"name":"embeddings.provenance.json","url":"https://api.github.com/repos/${RagStore.GITHUB_REPO}/releases/assets/1"},
                             {"name":"embeddings.json","url":"https://api.github.com/repos/${RagStore.GITHUB_REPO}/releases/assets/2"}]}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                url == "https://api.github.com/repos/${RagStore.GITHUB_REPO}/releases/assets/2" -> {
                    assertEquals("Bearer t0k3n", request.headers[HttpHeaders.Authorization])
                    assertEquals("application/octet-stream", request.headers[HttpHeaders.Accept])
                    respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, storage))
                }
                url == storage -> {
                    assertNull(request.headers[HttpHeaders.Authorization], "the bearer token must not reach object storage")
                    respond(fixtureJson("fresh.md"), HttpStatusCode.OK, headersOf(HttpHeaders.LastModified, "Fri, 04 Sep 2026 20:53:44 GMT"))
                }
                else -> error("unexpected request $url")
            }
        }
        val client = createRegistryDownloadClient(engine)
        try {
            val remote = RagStore.downloadRemoteEmbeddings(client, token = "t0k3n")!!
            assertEquals(RagStore.GITHUB_RELEASE_URL, remote.url, "provenance names the release, not the signed storage URL")
            assertEquals(Instant.parse("2026-09-04T20:53:44Z"), remote.lastModified)
            assertEquals("fresh.md", embeddingStoreSegments(remote.store).single().metadata().getString("file_name"))
            assertEquals(3, seen.size, seen.map { it.first }.toString())
            assertFalse(seen.any { it.first.startsWith(RagStore.PACKAGE_URL) }, "GitLab is not contacted when the asset loads")
        } finally {
            client.close()
        }
    }

    @Test
    fun withoutATokenThePublicUrlIsTriedAndAPrivateRepos404FallsThroughToGitLab() {
        val requested = mutableListOf<String>()
        val engine = MockEngine { request ->
            val url = request.url.toString()
            requested += url
            assertNull(request.headers[HttpHeaders.Authorization])
            when (url) {
                RagStore.GITHUB_RELEASE_URL -> respond("Not Found", HttpStatusCode.NotFound)
                "${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}" -> respond(fixtureJson("old.md"), HttpStatusCode.OK)
                else -> error("unexpected request $url")
            }
        }
        val client = createRegistryDownloadClient(engine)
        try {
            val remote = RagStore.downloadRemoteEmbeddings(client, token = null)!!
            assertEquals(listOf(RagStore.GITHUB_RELEASE_URL, "${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}"), requested)
            assertEquals("${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}", remote.url)
        } finally {
            client.close()
        }
    }

    @Test
    fun aTokenThatGitHubRejectsStillFallsBackToThePublicUrlThenGitLab() {
        val requested = mutableListOf<String>()
        val engine = MockEngine { request ->
            val url = request.url.toString()
            requested += url
            when (url) {
                RagStore.GITHUB_RELEASE_API_URL -> respond("""{"message":"Bad credentials"}""", HttpStatusCode.Unauthorized)
                RagStore.GITHUB_RELEASE_URL -> respond("Not Found", HttpStatusCode.NotFound)
                "${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}" -> respond(fixtureJson("old.md"), HttpStatusCode.OK)
                else -> error("unexpected request $url")
            }
        }
        val client = createRegistryDownloadClient(engine)
        try {
            val remote = RagStore.downloadRemoteEmbeddings(client, token = "expired")!!
            assertEquals(
                listOf(RagStore.GITHUB_RELEASE_API_URL, RagStore.GITHUB_RELEASE_URL, "${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}"),
                requested
            )
            assertEquals("${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}", remote.url)
        } finally {
            client.close()
        }
    }

    @Test
    fun tokenComesFromTheDedicatedVariableThenTheSecretFileThenGitHubToken() {
        val missing = tempDir.resolve("no-such-secret")
        assertNull(RagStore.embeddingsToken(emptyMap(), missing))
        assertNull(RagStore.embeddingsToken(mapOf(RagStore.EMBEDDINGS_TOKEN_ENV to " "), missing))
        assertEquals("gh", RagStore.embeddingsToken(mapOf(RagStore.GITHUB_TOKEN_ENV to "gh"), missing))
        assertEquals("mine", RagStore.embeddingsToken(mapOf(RagStore.EMBEDDINGS_TOKEN_ENV to "mine", RagStore.GITHUB_TOKEN_ENV to "gh"), missing))

        // Render Secret File: content with a trailing newline, read when the env var is unset.
        val secret = tempDir.resolve(RagStore.EMBEDDINGS_TOKEN_ENV).also { Files.writeString(it, "fr0m-f1le\n") }
        assertEquals("fr0m-f1le", RagStore.embeddingsToken(mapOf(RagStore.GITHUB_TOKEN_ENV to "gh"), secret), "the file outranks GITHUB_TOKEN")
        assertEquals("mine", RagStore.embeddingsToken(mapOf(RagStore.EMBEDDINGS_TOKEN_ENV to "mine"), secret), "the env var outranks the file")
        val empty = tempDir.resolve("empty").also { Files.writeString(it, "\n") }
        assertNull(RagStore.embeddingsToken(emptyMap(), empty), "a blank file is no token")
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
