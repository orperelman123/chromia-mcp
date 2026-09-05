package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.RagStore
import org.chromia.tools.RagStore.Companion.RemoteEmbeddings
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
import java.util.concurrent.atomic.AtomicInteger

/**
 * Local-first (2026-09-05): a boot with no local embeddings.json used to download
 * the 147 MB release asset EVERY time (streamed, ~12 s, never kept). The
 * downloaded index is now kept in CHROMIA_MCP_HOME (the npm launcher's home) and
 * reused until it is a week old; the weekly release is the refresh.
 */
class RagStoreCacheTest {

    @TempDir
    lateinit var tempDir: Path

    private val now: Instant = Instant.parse("2026-09-05T12:00:00Z")
    private val published: Instant = Instant.parse("2026-09-04T20:53:44Z")

    private fun fixtureStore(marker: String) = InMemoryEmbeddingStore<TextSegment>().also {
        it.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), TextSegment.from(marker, Metadata.from("file_name", "$marker.md")))
    }

    /** A downloader the way [RagStore.downloadRemoteEmbeddings] behaves with keepFile: the body is left in a temp file. */
    private fun downloader(marker: String, calls: AtomicInteger, lastModified: Instant? = published): () -> RemoteEmbeddings? = {
        calls.incrementAndGet()
        val file = Files.createTempFile(tempDir, "dl", ".json")
        fixtureStore(marker).serializeToFile(file)
        RemoteEmbeddings(fixtureStore(marker), RagStore.GITHUB_RELEASE_URL, lastModified, file)
    }

    private fun markerOf(store: InMemoryEmbeddingStore<TextSegment>?) =
        store?.serializeToJson()?.let { Regex("\"text\":\"([A-Z0-9_]+)\"").find(it)?.groupValues?.get(1) }

    @Test
    fun theFirstBootDownloadsAndKeepsTheIndexTheSecondBootReadsItBack() {
        val cache = tempDir.resolve("home").resolve("embeddings.json")
        val calls = AtomicInteger()

        val first = RagStore.loadCachedOrRemote(cache, now, downloader("FIRST", calls))
        assertNotNull(first)
        assertEquals(1, calls.get())
        assertEquals("FIRST", markerOf(first!!.store))
        assertEquals("GitHub release asset ${RagStore.GITHUB_RELEASE_URL}", first.provenance.origin)
        assertEquals(published, first.provenance.generatedAt)
        assertTrue(Files.isRegularFile(RagStore.cacheBinaryPath(cache)), "the body is kept, re-encoded, at ${RagStore.cacheBinaryPath(cache)}")
        assertFalse(Files.exists(cache), "the downloaded JSON is not kept: parsing it is the boot's whole cost")
        assertTrue(Files.isRegularFile(RagStore.cacheMetaPath(cache)), "with a sidecar naming url, Last-Modified and fetch time")
        assertEquals(0, Files.list(tempDir).filter { it.fileName.toString().startsWith("dl") }.count(), "the temp body is gone")

        val second = RagStore.loadCachedOrRemote(cache, now.plus(Duration.ofDays(1)), downloader("SECOND", calls))
        assertEquals(1, calls.get(), "a day-old cache is served without touching the network")
        assertEquals("FIRST", markerOf(second!!.store))
        assertEquals("cached GitHub release asset ${RagStore.GITHUB_RELEASE_URL} (fetched 2026-09-05)", second.provenance.origin)
        assertEquals(published, second.provenance.generatedAt, "age is the asset's, not the download's")
    }

    @Test
    fun aWeekOldCacheIsRefreshedAndReplaced() {
        val cache = tempDir.resolve("embeddings.json")
        val calls = AtomicInteger()
        RagStore.loadCachedOrRemote(cache, now, downloader("OLD", calls))

        val later = now.plus(RagStore.CACHE_REFRESH_AFTER).plusSeconds(1)
        val refreshed = RagStore.loadCachedOrRemote(cache, later, downloader("NEW", calls, lastModified = later.minusSeconds(3600)))
        assertEquals(2, calls.get())
        assertEquals("NEW", markerOf(refreshed!!.store))
        assertEquals("GitHub release asset ${RagStore.GITHUB_RELEASE_URL}", refreshed.provenance.origin)

        val again = RagStore.loadCachedOrRemote(cache, later.plusSeconds(60), downloader("NEWER", calls))
        assertEquals(2, calls.get(), "the replaced cache is fresh again")
        assertEquals("NEW", markerOf(again!!.store))
    }

    @Test
    fun whenTheRefreshFailsTheOldCacheStillAnswers() {
        val cache = tempDir.resolve("embeddings.json")
        val calls = AtomicInteger()
        RagStore.loadCachedOrRemote(cache, now, downloader("OLD", calls))

        val later = now.plus(Duration.ofDays(30))
        val result = RagStore.loadCachedOrRemote(cache, later) { calls.incrementAndGet(); null }
        assertEquals(2, calls.get(), "the refresh was attempted")
        assertNotNull(result, "offline is not the same as no index")
        assertEquals("OLD", markerOf(result!!.store))
        assertTrue(result.provenance.origin.startsWith("cached GitHub release asset"), result.provenance.origin)
        assertEquals(published, result.provenance.generatedAt)
    }

    @Test
    fun aCorruptCacheIsReplacedNotServed() {
        val cache = tempDir.resolve("embeddings.json")
        Files.writeString(cache, "{ not a store")
        val calls = AtomicInteger()
        val result = RagStore.loadCachedOrRemote(cache, now, downloader("FRESH", calls))
        assertEquals(1, calls.get())
        assertEquals("FRESH", markerOf(result!!.store))
        assertEquals("FRESH", markerOf(RagStore.loadCachedOrRemote(cache, now, downloader("X", calls))!!.store))
        assertEquals(1, calls.get())
    }

    @Test
    fun aHandCopiedJsonBodyWithoutSidecarIsServedAndDatedByItsMtime() {
        val cache = tempDir.resolve("embeddings.json")
        fixtureStore("COPIED").serializeToFile(cache)
        val calls = AtomicInteger()
        val result = RagStore.loadCachedOrRemote(cache, Instant.now(), downloader("NET", calls))
        assertEquals(0, calls.get(), "a fresh JSON body in the home is a cache too")
        assertEquals("COPIED", markerOf(result!!.store))
        assertTrue(result.provenance.origin.startsWith("cached GitHub release asset"), result.provenance.origin)
        assertNotNull(result.provenance.generatedAt, "dated by the file, absent a sidecar")
    }

    @Test
    fun aCorruptBinaryBodyIsRefreshedNotServed() {
        val cache = tempDir.resolve("embeddings.json")
        val calls = AtomicInteger()
        RagStore.loadCachedOrRemote(cache, now, downloader("GOOD", calls))
        Files.write(RagStore.cacheBinaryPath(cache), ByteArray(4096) { 7 })
        val result = RagStore.loadCachedOrRemote(cache, now, downloader("REFRESHED", calls))
        assertEquals(2, calls.get())
        assertEquals("REFRESHED", markerOf(result!!.store))
        assertEquals("REFRESHED", markerOf(RagStore.loadCachedOrRemote(cache, now, downloader("X", calls))!!.store))
        assertEquals(2, calls.get())
    }

    @Test
    fun noCachePathMeansDownloadEveryTimeAndKeepNothing() {
        val calls = AtomicInteger()
        val r = RagStore.loadCachedOrRemote(null, now, downloader("A", calls))
        assertEquals("A", markerOf(r!!.store))
        assertNull(r.provenance.origin.takeIf { it.startsWith("cached") })
        assertEquals("B", markerOf(RagStore.loadCachedOrRemote(null, now, downloader("B", calls))!!.store))
        assertEquals(2, calls.get())
        assertEquals(0, Files.list(tempDir).filter { it.fileName.toString().startsWith("dl") }.count(), "temp bodies are removed when nothing keeps them")
    }

    @Test
    fun aFailedFirstDownloadWithNoCacheIsNull() {
        val cache = tempDir.resolve("embeddings.json")
        assertNull(RagStore.loadCachedOrRemote(cache, now) { null })
        assertFalse(Files.exists(cache))
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
