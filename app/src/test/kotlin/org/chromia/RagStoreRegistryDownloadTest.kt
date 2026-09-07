package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.chromia.tools.RagStore
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
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.io.path.deleteIfExists

/**
 * The published embeddings index, downloaded over REAL HTTP.
 *
 * Until 2026-09-07 every test here drove a Ktor `MockEngine`: a substitute for
 * the transport itself, which meant the production download path was exercised
 * against a function that returned whatever the test said, and nothing here ever
 * opened a socket, parsed a real response, followed a real redirect or hit a
 * real status line. The engine below is the production CIO one, and the peer is
 * a real Ktor server on a loopback port serving a real index file written by the
 * production writer (`persistLocalEmbeddings`, via [TestDocsIndex.persist]).
 * A real server behaving badly - 404, 429, garbage bytes - is not a double; a
 * closed port (127.0.0.1:1) is the honest way to produce a refused connection.
 *
 * ## What could not be reproduced without github.com
 *
 * `RagStore.resolveDownload` special-cases exactly one URL - the literal
 * [RagStore.GITHUB_RELEASE_URL] - and looks its asset up on the literal
 * [RagStore.GITHUB_RELEASE_API_URL]. A loopback server cannot be either of them,
 * so the token-carrying releases-API branch is not covered here any more; see
 * the removal notes below for what went and what still covers it. The URL
 * ORDERING those tests also asserted is covered twice over: as a pure fact about
 * production's real list (`remoteEmbeddingsUrls`) and as real behaviour over two
 * real endpoints.
 */
class RagStoreRegistryDownloadTest {

    @TempDir
    lateinit var tempDir: Path

    /** One received request: what was asked for, and what credential came with it. */
    private data class Received(val uri: String, val authorization: String?)

    /**
     * Runs [routes] on a REAL Ktor CIO server on an ephemeral loopback port and
     * hands [block] its base URL. Nothing is simulated: the production client
     * opens a socket to it.
     */
    private fun <T> withServer(host: String = "127.0.0.1", routes: Routing.() -> Unit, block: (String) -> T): T {
        val server = embeddedServer(ServerCIO, host = host, port = 0) { routing(routes) }.start(wait = false)
        return try {
            val port = runBlocking { server.engine.resolvedConnectors().first().port }
            block("http://$host:$port")
        } finally {
            server.stop(0, 500)
        }
    }

    /**
     * A REAL index file, written by the production writer and read back as bytes,
     * so the body the server serves is byte-for-byte what the publish step
     * produces. [name] becomes the segment's `file_name`, which is what the
     * assertions below identify the served copy by.
     */
    private fun indexBytes(name: String): ByteArray {
        val path = TestDocsIndex.persist(
            tempDir.resolve("index-$name"),
            TextSegment.from("doc $name", Metadata.from("file_name", name))
        )
        return Files.readAllBytes(path)
    }

    /** A real address nothing listens on: the OS refuses the connection for real. */
    private val closedPortUrl = "http://127.0.0.1:1/embeddings.json"

    // REMOVED 2026-09-07: `missingLocalAndThrowingRegistryDoesNotCrashOrInventDocs`.
    //
    // Its claim is real and audited: a RagStore whose index never loaded must
    // answer search/fetch/fetch_docs with an explicit, retryable "index is
    // unavailable" (audit F5) and `fetch` must not degrade into "Documentation
    // not found" (audit round 4 F3). It is removed because the only way this
    // suite could put a RagStore into that state was `registryLoader = { ... }`,
    // a lambda double standing in for the published index download.
    //
    // No real configuration produces it today: with a missing local file
    // `loadFreshestStore()` calls `downloadRemoteEmbeddings()`, which reads its
    // URL list from `remoteEmbeddingsUrls()` INSIDE the call. A test cannot point
    // that at the closed port above, and letting it run would fetch the real
    // ~150 MB release asset on every build.
    //
    // TO RESTORE IT FOR REAL: give the RagStore constructor the URL list its env
    // override already implies (`remoteUrls: List<String> = remoteEmbeddingsUrls()`,
    // threaded into `downloadRemoteEmbeddings`). `remoteUrls = listOf(closedPortUrl)`
    // then produces a genuinely unloadable index, and `registryLoader` can be
    // deleted from production together with every `registryLoader = { ... }` still
    // in this suite (AuditConcurrencyRegressionTest, AuditRound4RegressionTest,
    // EmbeddingStoreSegmentsTest, RagStoreCwdIndependenceTest, RagStoreFetchByIdTest,
    // RagStoreLocalEmbeddingsTest, RagStoreProvenanceTest).

    @Test
    fun downloadFromRegistryHttpFailuresAreSkippedWithoutLiveNetwork() {
        // Real statuses from a real server, plus a real refused connection. Every
        // one of them must mean "skip this remote", never a thrown boot.
        listOf(
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            HttpStatusCode.NotFound
        ).forEach { status ->
            val received = CopyOnWriteArrayList<Received>()
            withServer(routes = {
                get("/release/embeddings.json") {
                    received += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                    call.respondBytes("denied".toByteArray(), status = status)
                }
            }) { base ->
                val client = createRegistryDownloadClient()
                try {
                    assertNull(
                        RagStore.downloadRemoteEmbeddings(
                            client = client,
                            urls = listOf("$base/release/embeddings.json"),
                            token = null
                        ),
                        "HTTP $status must skip the registry store"
                    )
                } finally {
                    client.close()
                }
            }
            assertEquals(1, received.size, "the remote was really contacted for $status")
            assertTrue(received.single().uri.contains("embeddings.json"), received.single().uri)
        }

        val client = createRegistryDownloadClient()
        try {
            assertNull(
                RagStore.downloadRemoteEmbeddings(client = client, urls = listOf(closedPortUrl), token = null),
                "a refused connection must skip the remote, not crash the boot"
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun downloadFromRegistryCorruptBodyIsSkippedWithoutLiveNetwork() {
        withServer(routes = {
            get("/release/embeddings.json") {
                call.respondBytes("not-an-embedding-store".toByteArray(), status = HttpStatusCode.OK)
            }
        }) { base ->
            val client = createRegistryDownloadClient()
            try {
                assertNull(
                    RagStore.downloadRemoteEmbeddings(
                        client = client,
                        urls = listOf("$base/release/embeddings.json"),
                        token = null
                    ),
                    "corrupt registry body must not throw"
                )
            } finally {
                client.close()
            }
        }
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
        // The origin each of those URLs is reported as, for the provenance line.
        assertTrue(RagStore.describeRemote(RagStore.GITHUB_RELEASE_URL).startsWith("GitHub release asset"))
        assertTrue(RagStore.describeRemote("${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}").startsWith("GitLab registry package"))
    }

    @Test
    fun theFirstRemoteThatAnswersWinsAndItsLastModifiedIsKept() {
        // Production's first entry is the GitHub release asset and its second is
        // the GitLab package (asserted as a fact of the list above); over real
        // HTTP what matters is that the SECOND endpoint is never contacted once
        // the first serves a parseable index, and that the server's real
        // Last-Modified is what ends up in the provenance.
        val received = CopyOnWriteArrayList<Received>()
        val body = indexBytes("fresh.md")
        withServer(routes = {
            get("/release/embeddings.json") {
                received += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                call.response.header(HttpHeaders.LastModified, "Mon, 31 Aug 2026 11:20:00 GMT")
                call.respondBytes(body, status = HttpStatusCode.OK)
            }
            get("/package/embeddings.json") {
                received += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                call.respondBytes(indexBytes("old.md"), status = HttpStatusCode.OK)
            }
        }) { base ->
            val client = createRegistryDownloadClient()
            try {
                val remote = RagStore.downloadRemoteEmbeddings(
                    client = client,
                    urls = listOf("$base/release/embeddings.json", "$base/package/embeddings.json"),
                    token = null
                )!!
                assertEquals(
                    listOf("/release/embeddings.json"),
                    received.map { it.uri },
                    "the fallback must not be contacted when the first remote loads"
                )
                assertEquals("$base/release/embeddings.json", remote.url)
                assertEquals(Instant.parse("2026-08-31T11:20:00Z"), remote.lastModified)
                assertEquals("fresh.md", embeddingStoreSegments(remote.store).single().metadata().getString("file_name"))
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun aMissingOrCorruptReleaseAssetFallsThroughToTheGitLabPackage() {
        // Each of these is a real answer from a real server: a 404, a rate-limit
        // page, and a 200 whose body is not an embedding store (an entry with no
        // vector). All three must fall through to the next remote.
        listOf<Pair<HttpStatusCode, ByteArray>>(
            HttpStatusCode.NotFound to "Not Found".toByteArray(),
            HttpStatusCode.TooManyRequests to "<html>rate limited</html>".toByteArray(),
            HttpStatusCode.OK to "{\"entries\":[{\"id\":\"x\"}]}".toByteArray()
        ).forEach { (status, firstBody) ->
            val received = CopyOnWriteArrayList<Received>()
            val fallbackBody = indexBytes("old.md")
            withServer(routes = {
                get("/release/embeddings.json") {
                    received += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                    call.respondBytes(firstBody, status = status)
                }
                get("/package/embeddings.json") {
                    received += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                    call.respondBytes(fallbackBody, status = HttpStatusCode.OK)
                }
            }) { base ->
                val client = createRegistryDownloadClient()
                try {
                    val remote = RagStore.downloadRemoteEmbeddings(
                        client = client,
                        urls = listOf("$base/release/embeddings.json", "$base/package/embeddings.json"),
                        token = null
                    )!!
                    assertEquals(
                        listOf("/release/embeddings.json", "/package/embeddings.json"),
                        received.map { it.uri }
                    )
                    assertEquals("$base/package/embeddings.json", remote.url)
                    assertNull(remote.lastModified, "no Last-Modified header on the fallback response")
                    assertEquals("old.md", embeddingStoreSegments(remote.store).single().metadata().getString("file_name"))
                } finally {
                    client.close()
                }
            }
        }
    }

    // REMOVED 2026-09-07: `withATokenThePrivateReleaseAssetIsResolvedThroughTheApiAndTheCredentialStopsAtGitHub`
    // and `aTokenThatGitHubRejectsStillFallsBackToThePublicUrlThenGitLab`.
    //
    // Both drove `MockEngine` and both asserted a branch that only exists for two
    // hard-coded github.com URLs: `RagStore.resolveDownload` looks the asset up on
    // GITHUB_RELEASE_API_URL only when the URL it was handed IS
    // GITHUB_RELEASE_URL. A loopback server can never be that URL, so the
    // releases-API lookup (asset id -> `Accept: application/octet-stream`) and the
    // "a rejected token falls back to the public URL" ordering cannot be produced
    // by any real server this test can start.
    //
    // WHAT NOW COVERS WHAT THEY CARRIED:
    //  - the credential rule ("the bearer token must not reach object storage") is
    //    covered for real below, over two real servers and a real 302, because it
    //    lives in `downloadFile` and is not GitHub-specific;
    //  - falling through to the next remote after a failure is covered for real by
    //    `aMissingOrCorruptReleaseAssetFallsThroughToTheGitLabPackage` and
    //    `withoutATokenNoCredentialIsSentAndA404FallsThroughToTheNextRemote`;
    //  - the token PRECEDENCE (env var, secret file, GITHUB_TOKEN) is still covered
    //    by `tokenComesFromTheDedicatedVariableThenTheSecretFileThenGitHubToken`.
    //
    // NOT COVERED any more: the releases-API asset lookup itself. Only CI's live
    // download steps (`scripts/rag-eval.mjs --production-shaped`,
    // `scripts/stdio-smoke.mjs --launcher-download`) touch the real release, and
    // they run WITHOUT a token, so the private-repo path is untested. Making it
    // testable needs the API URL to be a configuration point the way the download
    // URL list should be - the same change named in the removal note above.

    @Test
    fun theBearerCredentialIsNotForwardedAcrossAHostRedirect() {
        // The real rule from Utils.downloadFile: a credential is for the origin we
        // were given, not for wherever it sends us (GitHub answers an authenticated
        // release-asset request with a 302 to pre-signed object storage, which
        // rejects a request carrying a second credential). Two real servers on two
        // different host names, one real 302, one real socket each.
        val atOrigin = CopyOnWriteArrayList<Received>()
        val atStorage = CopyOnWriteArrayList<Received>()
        val body = indexBytes("fresh.md")
        withServer(host = "localhost", routes = {
            get("/storage/embeddings.json") {
                atStorage += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                call.respondBytes(body, status = HttpStatusCode.OK)
            }
        }) { storageBase ->
            withServer(routes = {
                get("/release/embeddings.json") {
                    atOrigin += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                    call.respondRedirect("$storageBase/storage/embeddings.json", permanent = false)
                }
            }) { originBase ->
                val client = createRegistryDownloadClient()
                val downloaded = try {
                    runBlocking {
                        client.downloadFile(
                            "$originBase/release/embeddings.json",
                            mapOf(HttpHeaders.Authorization to "Bearer t0k3n")
                        )
                    }
                } finally {
                    client.close()
                }
                try {
                    assertTrue(downloaded != null, "the redirect must be followed to the body")
                    assertTrue(Files.readAllBytes(downloaded!!).contentEquals(body), "the storage body is what was kept")
                } finally {
                    downloaded?.deleteIfExists()
                }
            }
        }
        assertEquals("Bearer t0k3n", atOrigin.single().authorization, "the origin gets the credential it was given")
        assertNull(atStorage.single().authorization, "the bearer token must not reach the redirect target")
    }

    @Test
    fun withoutATokenNoCredentialIsSentAndA404FallsThroughToTheNextRemote() {
        val received = CopyOnWriteArrayList<Received>()
        val fallbackBody = indexBytes("old.md")
        withServer(routes = {
            get("/release/embeddings.json") {
                received += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                call.respondBytes("Not Found".toByteArray(), status = HttpStatusCode.NotFound)
            }
            get("/package/embeddings.json") {
                received += Received(call.request.uri, call.request.headers[HttpHeaders.Authorization])
                call.respondBytes(fallbackBody, status = HttpStatusCode.OK)
            }
        }) { base ->
            val client = createRegistryDownloadClient()
            try {
                val remote = RagStore.downloadRemoteEmbeddings(
                    client = client,
                    urls = listOf("$base/release/embeddings.json", "$base/package/embeddings.json"),
                    token = null
                )!!
                assertEquals(
                    listOf("/release/embeddings.json", "/package/embeddings.json"),
                    received.map { it.uri }
                )
                assertEquals("$base/package/embeddings.json", remote.url)
            } finally {
                client.close()
            }
        }
        assertTrue(received.all { it.authorization == null }, "no token means no Authorization header on the wire")
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
        // `expectSuccess = false` used to be asserted by inspecting a MockEngine
        // client; here a real 401 comes back from a real server as a status, not
        // as a thrown exception, which is the behaviour that matters.
        withServer(routes = {
            get("/release/embeddings.json") {
                call.respondBytes("denied".toByteArray(), status = HttpStatusCode.Unauthorized)
            }
        }) { base ->
            val client = createRegistryDownloadClient()
            try {
                assertTrue(client.pluginOrNull(HttpTimeout) != null)
                val status = runBlocking { client.get("$base/release/embeddings.json").status }
                assertEquals(HttpStatusCode.Unauthorized, status)
                assertFalse(status.isSuccess())
            } finally {
                client.close()
            }
        }
    }
}
