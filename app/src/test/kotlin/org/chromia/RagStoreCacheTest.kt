package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.ktor.http.HttpHeaders
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.chromia.tools.RagStore
import org.chromia.tools.RagStore.Companion.RemoteEmbeddings
import org.chromia.tools.persistLocalEmbeddings
import org.chromia.tools.resolveCacheEmbeddingsPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Local-first (2026-09-05): a boot with no local embeddings.json used to download
 * the 147 MB release asset EVERY time (streamed, ~12 s, never kept). The
 * downloaded index is now kept in CHROMIA_MCP_HOME (the npm launcher's home) and
 * reused until it is a week old; the weekly release is the refresh.
 *
 * ## Zero doubles (2026-09-07)
 *
 * Every test below used to fabricate the download: a
 * `downloader(marker, calls, lastModified)` helper returning a
 * `() -> RemoteEmbeddings?` lambda that built a hand-made store, wrote it to a
 * temp file and handed back a [RemoteEmbeddings] with a Last-Modified of its own
 * choosing. That lambda WAS the whole remote step - the HTTP client, the
 * streamed body, the header parse and the JSON reader all replaced by four
 * constructor arguments - so "the cache is refreshed" only ever proved that the
 * cache code calls a function.
 *
 * It is gone, together with the hand-written `Embedding.from(floatArrayOf(0.1f,
 * 0.2f, 0.3f))` fixture vector: 3 floats where the model the server ships
 * produces 384, i.e. a store production could never have written.
 *
 * What runs now, exactly as in [RagStoreCwdIndependenceTest]: a REAL Ktor CIO
 * server on a real loopback socket ([servingIndex]) serving a REAL index file
 * written by the production writer ([persistLocalEmbeddings]) over a real
 * `Last-Modified` header, fetched by production's own
 * [RagStore.downloadRemoteEmbeddings]. "Without touching the network" is proved
 * by a counter incremented INSIDE the route, so a served cache means the server
 * really was never contacted. An offline boot is a real refused connection to a
 * real closed port. Only `now` is injected, and that is production's own clock
 * parameter, not a substitute for a collaborator.
 */
class RagStoreCacheTest {

    @TempDir
    lateinit var tempDir: Path

    private val now: Instant = Instant.parse("2026-09-05T12:00:00Z")
    private val published: Instant = Instant.parse("2026-09-04T20:53:44Z")

    /** A one-segment index built with the model the server ships; [marker] identifies the copy. */
    private fun fixtureStore(marker: String): InMemoryEmbeddingStore<TextSegment> =
        TestDocsIndex.index(listOf(TextSegment.from(marker, Metadata.from("file_name", "$marker.md"))))

    private fun markerOf(store: InMemoryEmbeddingStore<TextSegment>?) =
        store?.serializeToJson()?.let { Regex("\"text\":\"([A-Z0-9_]+)\"").find(it)?.groupValues?.get(1) }

    /** A real address nothing listens on: the OS refuses the connection for real. */
    private val closedPortUrl = "http://127.0.0.1:1/embeddings.json"

    /**
     * Serves a REAL index file over a REAL socket, with [lastModified] actually
     * written into the response header production parses. [block] gets the URL
     * and a counter of the requests the server really received.
     */
    private fun <T> servingIndex(
        marker: String,
        lastModified: Instant?,
        block: (url: String, hits: AtomicInteger) -> T
    ): T {
        val file = tempDir.resolve("served-$marker.json")
        persistLocalEmbeddings(fixtureStore(marker), file)
        val body = Files.readAllBytes(file)
        val hits = AtomicInteger()
        val server = embeddedServer(ServerCIO, host = "127.0.0.1", port = 0) {
            routing {
                get("/embeddings.json") {
                    hits.incrementAndGet()
                    lastModified?.let {
                        call.response.header(
                            HttpHeaders.LastModified,
                            DateTimeFormatter.RFC_1123_DATE_TIME.format(it.atOffset(ZoneOffset.UTC))
                        )
                    }
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

    /** The temp file production's last download streamed the body to; the cache owns and must remove it. */
    private val lastBody = AtomicReference<Path?>()

    /** Production's real download, pointed at [url]; [calls] counts the refresh attempts. */
    private fun download(url: String, calls: AtomicInteger): () -> RemoteEmbeddings? = {
        calls.incrementAndGet()
        RagStore.downloadRemoteEmbeddings(urls = listOf(url), keepFile = true).also { lastBody.set(it?.file) }
    }

    @Test
    fun theFirstBootDownloadsAndKeepsTheIndexTheSecondBootReadsItBack() {
        val cache = tempDir.resolve("home").resolve("embeddings.json")
        val calls = AtomicInteger()

        val firstUrl = servingIndex("FIRST", published) { url, hits ->
            val first = RagStore.loadCachedOrRemote(cache, now, download(url, calls))
            assertNotNull(first)
            assertEquals(1, calls.get())
            assertEquals(1, hits.get(), "the body really came over the socket")
            assertEquals("FIRST", markerOf(first!!.store))
            assertEquals(RagStore.describeRemote(url), first.provenance.origin)
            assertEquals(published, first.provenance.generatedAt, "the server's own Last-Modified, parsed by production")
            url
        }
        assertTrue(Files.isRegularFile(RagStore.cacheBinaryPath(cache)), "the body is kept, re-encoded, at ${RagStore.cacheBinaryPath(cache)}")
        assertFalse(Files.exists(cache), "the downloaded JSON is not kept: parsing it is the boot's whole cost")
        assertTrue(Files.isRegularFile(RagStore.cacheMetaPath(cache)), "with a sidecar naming url, Last-Modified and fetch time")
        assertFalse(Files.exists(lastBody.get()!!), "the temp file the download streamed to is gone")

        servingIndex("SECOND", published) { url, hits ->
            val second = RagStore.loadCachedOrRemote(cache, now.plus(Duration.ofDays(1)), download(url, calls))
            assertEquals(0, hits.get(), "a day-old cache is served without touching the network")
            assertEquals(1, calls.get(), "the refresh was not even attempted")
            assertEquals("FIRST", markerOf(second!!.store))
            assertEquals("cached ${RagStore.describeRemote(firstUrl)} (fetched 2026-09-05)", second.provenance.origin)
            assertEquals(published, second.provenance.generatedAt, "age is the asset's, not the download's")
        }
    }

    @Test
    fun aWeekOldCacheIsRefreshedAndReplaced() {
        val cache = tempDir.resolve("embeddings.json")
        val calls = AtomicInteger()
        servingIndex("OLD", published) { url, _ -> RagStore.loadCachedOrRemote(cache, now, download(url, calls)) }

        val later = now.plus(RagStore.CACHE_REFRESH_AFTER).plusSeconds(1)
        servingIndex("NEW", later.minusSeconds(3600)) { url, hits ->
            val refreshed = RagStore.loadCachedOrRemote(cache, later, download(url, calls))
            assertEquals(2, calls.get())
            assertEquals(1, hits.get(), "a week-old cache really goes back to the server")
            assertEquals("NEW", markerOf(refreshed!!.store))
            assertEquals(RagStore.describeRemote(url), refreshed.provenance.origin)
        }

        servingIndex("NEWER", published) { url, hits ->
            val again = RagStore.loadCachedOrRemote(cache, later.plusSeconds(60), download(url, calls))
            assertEquals(0, hits.get(), "the replaced cache is fresh again: not one request")
            assertEquals(2, calls.get())
            assertEquals("NEW", markerOf(again!!.store))
        }
    }

    @Test
    fun whenTheRefreshFailsTheOldCacheStillAnswers() {
        val cache = tempDir.resolve("embeddings.json")
        val calls = AtomicInteger()
        val oldUrl = servingIndex("OLD", published) { url, _ ->
            RagStore.loadCachedOrRemote(cache, now, download(url, calls))
            url
        }

        // The outage is real: nothing is listening on port 1, so the production
        // client gets a refused connection rather than a lambda returning null.
        val later = now.plus(Duration.ofDays(30))
        val result = RagStore.loadCachedOrRemote(cache, later, download(closedPortUrl, calls))
        assertEquals(2, calls.get(), "the refresh was attempted")
        assertNotNull(result, "offline is not the same as no index")
        assertEquals("OLD", markerOf(result!!.store))
        assertEquals("cached ${RagStore.describeRemote(oldUrl)} (fetched 2026-09-05)", result.provenance.origin)
        assertEquals(published, result.provenance.generatedAt)
    }

    @Test
    fun aCorruptCacheIsReplacedNotServed() {
        val cache = tempDir.resolve("embeddings.json")
        Files.writeString(cache, "{ not a store")
        val calls = AtomicInteger()
        servingIndex("FRESH", published) { url, hits ->
            val result = RagStore.loadCachedOrRemote(cache, now, download(url, calls))
            assertEquals(1, calls.get())
            assertEquals(1, hits.get(), "a body that does not parse is refreshed over the wire, not served")
            assertEquals("FRESH", markerOf(result!!.store))
        }
        servingIndex("SECOND", published) { url, hits ->
            assertEquals("FRESH", markerOf(RagStore.loadCachedOrRemote(cache, now, download(url, calls))!!.store))
            assertEquals(0, hits.get(), "and the replacement is a real cache: the second boot never asks")
            assertEquals(1, calls.get())
        }
    }

    @Test
    fun aHandCopiedJsonBodyWithoutSidecarIsServedAndDatedByItsMtime() {
        val cache = tempDir.resolve("embeddings.json")
        persistLocalEmbeddings(fixtureStore("COPIED"), cache)
        val calls = AtomicInteger()
        servingIndex("NET", published) { url, hits ->
            val result = RagStore.loadCachedOrRemote(cache, Instant.now(), download(url, calls))
            assertEquals(0, calls.get(), "a fresh JSON body in the home is a cache too")
            assertEquals(0, hits.get(), "and the server really was not contacted")
            assertEquals("COPIED", markerOf(result!!.store))
            assertTrue(result.provenance.origin.startsWith("cached GitHub release asset"), result.provenance.origin)
            assertNotNull(result.provenance.generatedAt, "dated by the file, absent a sidecar")
        }
    }

    @Test
    fun aCorruptBinaryBodyIsRefreshedNotServed() {
        val cache = tempDir.resolve("embeddings.json")
        val calls = AtomicInteger()
        servingIndex("GOOD", published) { url, _ -> RagStore.loadCachedOrRemote(cache, now, download(url, calls)) }
        Files.write(RagStore.cacheBinaryPath(cache), ByteArray(4096) { 7 })

        servingIndex("REFRESHED", published.plusSeconds(3600)) { url, hits ->
            val result = RagStore.loadCachedOrRemote(cache, now, download(url, calls))
            assertEquals(2, calls.get())
            assertEquals(1, hits.get(), "the unreadable binary sent the boot back to the network")
            assertEquals("REFRESHED", markerOf(result!!.store))
        }
        servingIndex("AGAIN", published) { url, hits ->
            assertEquals("REFRESHED", markerOf(RagStore.loadCachedOrRemote(cache, now, download(url, calls))!!.store))
            assertEquals(0, hits.get(), "the repaired cache is served without a request")
            assertEquals(2, calls.get())
        }
    }

    @Test
    fun noCachePathMeansDownloadEveryTimeAndKeepNothing() {
        val calls = AtomicInteger()
        servingIndex("A", published) { url, hits ->
            val r = RagStore.loadCachedOrRemote(null, now, download(url, calls))
            assertEquals("A", markerOf(r!!.store))
            assertEquals(1, hits.get())
            assertNull(r.provenance.origin.takeIf { it.startsWith("cached") })
            assertFalse(Files.exists(lastBody.get()!!), "temp bodies are removed when nothing keeps them")
        }
        servingIndex("B", published) { url, hits ->
            assertEquals("B", markerOf(RagStore.loadCachedOrRemote(null, now, download(url, calls))!!.store))
            assertEquals(1, hits.get(), "with no cache path every boot really downloads again")
            assertFalse(Files.exists(lastBody.get()!!), "and keeps nothing behind")
        }
        assertEquals(2, calls.get())
    }

    @Test
    fun aFailedFirstDownloadWithNoCacheIsNull() {
        val cache = tempDir.resolve("embeddings.json")
        val calls = AtomicInteger()
        assertNull(RagStore.loadCachedOrRemote(cache, now, download(closedPortUrl, calls)))
        assertEquals(1, calls.get(), "the download was really attempted, against a really closed port")
        assertFalse(Files.exists(cache))
        assertFalse(Files.exists(RagStore.cacheBinaryPath(cache)), "a failed download leaves no cache behind")
    }

    @Test
    fun theCacheLivesInTheLauncherHome() {
        assertEquals(
            Path.of("/tmp/x", "embeddings.json"),
            resolveCacheEmbeddingsPath(env = mapOf(RagStore.HOME_ENV to "/tmp/x"), userHome = "/home/u")
        )
        assertEquals(
            Path.of("/home/u", ".chromia-mcp", "embeddings.json"),
            resolveCacheEmbeddingsPath(env = emptyMap(), userHome = "/home/u")
        )
        assertEquals(
            Path.of("/home/u", ".chromia-mcp", "embeddings.json"),
            resolveCacheEmbeddingsPath(env = mapOf(RagStore.HOME_ENV to "  "), userHome = "/home/u"),
            "blank is unset"
        )
        assertNull(resolveCacheEmbeddingsPath(env = mapOf(RagStore.CACHE_ENV to "off"), userHome = "/home/u"), "CHROMIA_EMBEDDINGS_CACHE=off disables it")
    }
}
