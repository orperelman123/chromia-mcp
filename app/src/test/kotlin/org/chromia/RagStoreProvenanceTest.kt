package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.tools.FetchDocsStrategy
import org.chromia.tools.RagStore
import org.chromia.tools.persistLocalEmbeddings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * Round 10 (2026-09-04): the published registry package was generated
 * 2025-10-21 and nothing said so - production answered from a year-old store
 * and `fetch_docs` could not find FT4's `require_mandatory_flags`. The index
 * now carries its origin and age, logs them, and tells the agent on every
 * fetch_docs answer when it is past the freshness limit.
 *
 * Zero doubles (2026-09-07): the two loads below used to be driven by a
 * `registryLoader = { ... }` constructor lambda - one that threw to mean "the
 * network must not be touched", one that returned a store to mean "the registry
 * served this". Neither ran a byte of the download path. They are now a REAL
 * Ktor server on a real loopback socket serving a REAL index file, and the
 * "must not be touched" case is that same server counting the requests it
 * received and finding none.
 */
class RagStoreProvenanceTest {

    @TempDir
    lateinit var tempDir: Path

    private val now = Instant.parse("2026-09-04T12:00:00Z")

    /** A real index over [markers], embedded with the model the server ships. */
    private fun fixtureStore(vararg markers: String): InMemoryEmbeddingStore<TextSegment> =
        TestDocsIndex.index(markers.map { TextSegment.from(it, Metadata.from("file_name", "$it.md")) })

    /**
     * Serves a REAL index file over a REAL socket and counts the requests that
     * actually arrived. No `Last-Modified` is sent, which is itself a real case:
     * a plain static host that dates nothing.
     */
    private fun <T> servingIndex(vararg markers: String, block: (url: String, hits: AtomicInteger) -> T): T {
        val file = tempDir.resolve("served-${markers.joinToString("-")}.json")
        persistLocalEmbeddings(fixtureStore(*markers), file)
        val body = Files.readAllBytes(file)
        val hits = AtomicInteger()
        val server = embeddedServer(ServerCIO, host = "127.0.0.1", port = 0) {
            routing {
                get("/embeddings.json") {
                    hits.incrementAndGet()
                    call.respondBytes(body)
                }
            }
        }.start(wait = false)
        return try {
            val port = runBlocking { server.engine.resolvedConnectors().first().port }
            block("http://127.0.0.1:$port/embeddings.json", hits)
        } finally {
            server.stop(0, 500)
        }
    }

    /** The download cache, always inside the @TempDir - never the developer's real `~/.chromia-mcp`. */
    private fun cachePath(name: String): Path = tempDir.resolve("cache-$name").resolve(RagStore.FILE_NAME)

    @Test
    fun aFreshIndexHasNoWarningAndAnOldOneNamesDateAgeAndFix() {
        val fresh = RagStore.Provenance("local file x", now.minus(Duration.ofDays(30)), 5)
        assertNull(fresh.staleWarning(now))
        assertEquals("documentation index: 5 segments from local file x, generated 2026-08-05 (30 days old)", fresh.describe(now))

        val old = RagStore.Provenance("GitLab registry package p", Instant.parse("2025-10-21T09:13:14Z"), 3084)
        val warning = old.staleWarning(now)
        assertNotNull(warning)
        assertTrue(warning!!.contains("STALE"), warning)
        assertTrue(warning.contains("generated 2025-10-21"), warning)
        assertTrue(warning.contains("318 days ago"), warning)
        assertTrue(warning.contains("limit 120"), warning)
        assertTrue(warning.contains("GitLab tags"), "the agent must be told how to verify: $warning")
        // The fix is the workflow anyone with repo access can run, not a GitLab token only ChromaWay holds.
        assertTrue(warning.contains("Embeddings refresh") && warning.contains(RagStore.GITHUB_RELEASE_URL), "the operator must be told how to fix: $warning")
        assertTrue(warning.contains(RagStore.EMBEDDINGS_PATH_ENV), warning)

        // Exactly at the limit is still fresh; one day past is not.
        assertNull(RagStore.Provenance("x", now.minus(Duration.ofDays(120)), 1).staleWarning(now))
        assertNotNull(RagStore.Provenance("x", now.minus(Duration.ofDays(121)), 1).staleWarning(now))
    }

    @Test
    fun anIndexOfUnknownAgeIsDescribedButNeverCalledStale() {
        val unknown = RagStore.Provenance("GitLab registry package p", null, 7)
        assertNull(unknown.staleWarning(now))
        assertEquals("documentation index: 7 segments from GitLab registry package p, generated unknown date", unknown.describe(now))
        assertNull(unknown.ageDays(now))
    }

    @Test
    fun lastModifiedHeaderParsesAsRfc1123AndGarbageIsNull() {
        assertEquals(Instant.parse("2025-10-21T09:13:14Z"), RagStore.parseLastModified("Tue, 21 Oct 2025 09:13:14 GMT"))
        assertEquals(Instant.parse("2025-10-21T09:13:14Z"), RagStore.parseLastModified("  Tue, 21 Oct 2025 09:13:14 GMT "))
        assertNull(RagStore.parseLastModified(null))
        assertNull(RagStore.parseLastModified(""))
        assertNull(RagStore.parseLastModified("2025-10-21"))
        assertNull(RagStore.parseLastModified("not a date"))
    }

    @Test
    fun aLocalLoadRecordsPathMtimeAndSegmentCount() {
        val path = tempDir.resolve("embeddings.json")
        persistLocalEmbeddings(fixtureStore("A", "B", "C"), path)
        val generated = Instant.parse("2026-08-26T16:54:00Z")
        Files.setLastModifiedTime(path, FileTime.from(generated))

        // The remote is configured and reachable; the point is that a usable
        // local file means it is never asked. `registryLoader = { error(...) }`
        // asserted that by throwing from a lambda that stood in for the whole
        // download; the real server below asserts it by receiving no request.
        servingIndex("REMOTE") { url, hits ->
            val store = RagStore(
                loadFromRegistry = true,
                localEmbeddingsPath = path,
                remoteUrls = listOf(url),
                embeddingModel = TestDocsIndex.model,
                cacheEmbeddingsPath = cachePath("local-load")
            )
            assertEquals(0, hits.get(), "the remote must not be consulted when the local file loads")

            val p = store.provenance
            assertNotNull(p, "a successful local load must record where the index came from")
            assertTrue(p!!.origin.contains(path.toString()), p.origin)
            assertEquals(generated, p.generatedAt)
            assertEquals(3, p.segments)
            assertNull(store.staleWarning(generated.plus(Duration.ofDays(10))))
            assertNotNull(store.staleWarning(generated.plus(Duration.ofDays(400))))
        }
    }

    @Test
    fun aRemoteLoadRecordsTheRemoteOriginAndHasNoDateWithoutLastModified() {
        val missing = tempDir.resolve("none").resolve("embeddings.json")
        servingIndex("R1", "R2") { url, hits ->
            val store = RagStore(
                loadFromRegistry = true,
                localEmbeddingsPath = missing,
                remoteUrls = listOf(url),
                embeddingModel = TestDocsIndex.model,
                cacheEmbeddingsPath = cachePath("remote-load")
            )
            assertEquals(1, hits.get(), "with no local file the remote is really fetched")

            val p = store.provenance
            assertNotNull(p)
            assertEquals(RagStore.describeRemote(url), p!!.origin)
            assertTrue(p.origin.contains(url), p.origin)
            assertNull(p.generatedAt, "a host that sends no Last-Modified dates the index with nothing")
            assertEquals(2, p.segments)
            assertNull(store.staleWarning(now), "unknown age is never reported stale")
        }

        // Real remotes are named by kind so the log says which publish path served the index.
        assertEquals("GitHub release asset ${RagStore.GITHUB_RELEASE_URL}", RagStore.describeRemote(RagStore.GITHUB_RELEASE_URL))
        assertEquals(
            "GitLab registry package ${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}",
            RagStore.describeRemote("${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}")
        )
    }

    @Test
    fun aFailedLoadAndAFixtureStoreHaveNoProvenance() {
        // No local file and no remote configured: an air-gapped install whose
        // shipped index is missing. A real deployment, not a lambda saying "null".
        val missing = tempDir.resolve("none").resolve("embeddings.json")
        assertNull(
            RagStore(
                loadFromRegistry = true,
                localEmbeddingsPath = missing,
                remoteUrls = emptyList(),
                cacheEmbeddingsPath = cachePath("no-remote")
            ).provenance
        )
        assertNull(RagStore(loadFromRegistry = false, initialStore = fixtureStore("F")).provenance)
    }

    private val segment = TextSegment.from(
        "chr deployment create writes deployments.<net>.chains into chromia.yml.",
        Metadata.from("file_name", "chr-deploy.md")
    )

    // A real store that really retrieves [segment] for the "deployments" query
    // below (it used to override query() and hand the hit back unconditionally).
    private fun answering(): RagStore = TestDocsIndex.store(segment)

    private suspend fun fetchDocs(store: RagStore) = FetchDocsStrategy(CompletableDeferred(store)).execute(
        callToolRequest(name = "fetch_docs", arguments = buildJsonObject { put("query", "deployments") }),
        ChromiaRepositoryImpl()
    )

    @Test
    fun fetchDocsAppendsTheStaleNoteOnlyWhenTheIndexIsPastTheLimit() = runBlocking {
        val stale = answering().also { it.provenance = RagStore.Provenance("GitLab registry package p", Instant.parse("2025-10-21T09:13:14Z"), 3084) }
        val result = fetchDocs(stale)
        assertFalse(result.isError == true)
        val text = (result.content.single() as TextContent).text!!
        assertTrue(text.contains("chr deployment create"), "the hits still come first: $text")
        assertTrue(text.contains("\n\nNOTE: documentation index is STALE: generated 2025-10-21"), text)
        val note = result.structuredContent!!.jsonObject["index_note"]?.jsonPrimitive?.content
        assertNotNull(note, "structured output carries the same note for clients that never read text")
        assertTrue(note!!.contains("GitLab tags"), note)

        val fresh = answering().also { it.provenance = RagStore.Provenance("local file x", Instant.now().minus(Duration.ofDays(1)), 3084) }
        val freshResult = fetchDocs(fresh)
        val freshText = (freshResult.content.single() as TextContent).text!!
        assertFalse(freshText.contains("NOTE: documentation index"), "a fresh index adds nothing: $freshText")
        assertNull(freshResult.structuredContent!!.jsonObject["index_note"])

        val unknown = answering() // fixture store, no provenance at all
        val unknownText = (fetchDocs(unknown).content.single() as TextContent).text!!
        assertFalse(unknownText.contains("NOTE: documentation index"), unknownText)
    }

    @Test
    fun fetchDocsAlwaysNamesTheIndexItAnsweredFromInStructuredOutput() = runBlocking {
        // The hosted server is a black box over the wire: /health has no store (it loads
        // lazily) and the text only speaks up when stale. A client verifying a deploy
        // needs the origin and age of the index on every answer, fresh or not.
        val generated = Instant.parse("2026-09-04T20:53:39Z")
        val store = answering().also {
            it.provenance = RagStore.Provenance("GitHub release asset ${RagStore.GITHUB_RELEASE_URL}", generated, 25823)
        }
        val index = fetchDocs(store).structuredContent!!.jsonObject["index"]!!.jsonObject
        assertEquals("GitHub release asset ${RagStore.GITHUB_RELEASE_URL}", index["origin"]?.jsonPrimitive?.content)
        assertEquals("2026-09-04T20:53:39Z", index["generated_at"]?.jsonPrimitive?.content)
        assertEquals(25823, index["segments"]?.jsonPrimitive?.content?.toInt())
        assertTrue((index["age_days"]?.jsonPrimitive?.content?.toLong() ?: -1) >= 0, index.toString())
        assertEquals("false", index["stale"]?.jsonPrimitive?.content)

        val text = (fetchDocs(store).content.single() as TextContent).text!!
        assertFalse(text.contains("GitHub release asset"), "text stays lean - provenance is structured only: $text")

        // An index whose host sent no Last-Modified: exactly the provenance the
        // real loopback download above produced, dated by nothing.
        val unknownAge = answering().also {
            it.provenance = RagStore.Provenance(RagStore.describeRemote(RagStore.GITHUB_RELEASE_URL), null, 2)
        }
        val unknownIndex = fetchDocs(unknownAge).structuredContent!!.jsonObject["index"]!!.jsonObject
        assertTrue(unknownIndex["generated_at"] is JsonNull, unknownIndex.toString())
        assertTrue(unknownIndex["age_days"] is JsonNull, unknownIndex.toString())

        assertNull(fetchDocs(answering()).structuredContent!!.jsonObject["index"], "no provenance, no index object")
    }
}
