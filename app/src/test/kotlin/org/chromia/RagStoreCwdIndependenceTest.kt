package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.RagStore
import org.chromia.tools.RagStore.Companion.RemoteEmbeddings
import org.chromia.tools.resolveRuntimeEmbeddingsPath
import org.chromia.tools.runtimeEmbeddingsCandidates
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/**
 * AUDIT F1 (2026-09-06) - the documentation index the server answers from
 * depended on the process working directory, and the README's own registration
 * command sets none.
 *
 * The measured finding, same jar, same `--stdio`, same env, only `cwd` differs:
 *
 *   cwd = C:\Users\Orpe7\chromia-mcp-audit
 *     fetch_docs{query:"module_args"}.index =
 *       {"origin":"cached GitLab registry package ... (fetched 2026-09-05)",
 *        "generated_at":"2025-10-21T09:13:14Z","age_days":320,"segments":3208,"stale":true}
 *
 *   cwd = C:\Users\Orpe7\chromia-mcp
 *     fetch_docs{query:"module_args"}.index =
 *       {"origin":"local file app\build\embeddings.json",
 *        "generated_at":"2026-09-04T14:29:18Z","age_days":1,"segments":25823,"stale":false}
 *
 * 12.5% of the corpus, 320 days old, chosen by nothing but the directory the
 * client happened to launch the jar in.
 */
class RagStoreCwdIndependenceTest {

    @TempDir
    lateinit var tempDir: Path

    private val jarDir: Path get() = tempDir.resolve("app").resolve("build").resolve("libs")
    private val jarAdjacent: Path get() = jarDir.resolve("embeddings.json").toAbsolutePath().normalize()
    private val appBuild: Path get() = tempDir.resolve("app").resolve("build").resolve("embeddings.json").toAbsolutePath().normalize()

    /**
     * Two working directories, modelled exactly as the process sees them: a
     * relative candidate resolves against cwd, so `build/embeddings.json` and
     * `app/build/embeddings.json` exist in one and not the other. Anything
     * absolute answers the same in both.
     */
    private fun existsInRepoRootCwd(): (Path) -> Boolean = { path ->
        when {
            !path.isAbsolute -> path == Path.of("app", "build", "embeddings.json") ||
                path == Path.of("settings.gradle.kts")
            else -> path == appBuild
        }
    }

    private fun existsInSomeOtherCwd(): (Path) -> Boolean = { path ->
        when {
            !path.isAbsolute -> false
            else -> path == appBuild
        }
    }

    @Test
    fun theRuntimeResolvesTheSameStoreFromTwoDifferentWorkingDirectories() {
        val fromRepoRoot = resolveRuntimeEmbeddingsPath(
            env = emptyMap(),
            jarDir = jarDir,
            exists = existsInRepoRootCwd(),
            userHome = tempDir.toString()
        )
        val fromElsewhere = resolveRuntimeEmbeddingsPath(
            env = emptyMap(),
            jarDir = jarDir,
            exists = existsInSomeOtherCwd(),
            userHome = tempDir.toString()
        )
        assertEquals(fromRepoRoot, fromElsewhere, "the same jar must answer from the same store in any cwd")
        assertEquals(appBuild, fromRepoRoot, "the jar-adjacent store, resolved from the jar's own location")
    }

    @Test
    fun everyRuntimeCandidateIsAbsolute() {
        val candidates = runtimeEmbeddingsCandidates(jarDir)
        assertTrue(candidates.isNotEmpty())
        candidates.forEach { assertTrue(it.isAbsolute, "cwd-relative runtime candidate: $it") }
        listOf(
            resolveRuntimeEmbeddingsPath(emptyMap(), jarDir, { false }, tempDir.toString()),
            resolveRuntimeEmbeddingsPath(emptyMap(), null, { false }, tempDir.toString()),
            resolveRuntimeEmbeddingsPath(emptyMap(), null, { true }, tempDir.toString())
        ).forEach {
            assertTrue(it.isAbsolute, "cwd-relative runtime path: $it")
            assertFalse(
                it == Path.of("build", "embeddings.json") || it == Path.of("app", "build", "embeddings.json"),
                "the runtime must never read a cwd-relative path: $it"
            )
        }
    }

    @Test
    fun anExplicitPathWinsAndIsAbsolutised() {
        val explicit = tempDir.resolve("pinned.json")
        val resolved = resolveRuntimeEmbeddingsPath(
            env = mapOf(RagStore.EMBEDDINGS_PATH_ENV to explicit.toString()),
            jarDir = jarDir,
            exists = { true },
            userHome = tempDir.toString()
        )
        assertEquals(explicit.toAbsolutePath().normalize(), resolved)
    }

    @Test
    fun withNoJarTheRuntimeFallsBackToTheLauncherHomeNeverToCwd() {
        val resolved = resolveRuntimeEmbeddingsPath(
            env = mapOf(RagStore.HOME_ENV to tempDir.resolve("home").toString()),
            jarDir = null,
            exists = { false },
            userHome = tempDir.toString()
        )
        assertEquals(tempDir.resolve("home").resolve("embeddings.json").toAbsolutePath().normalize(), resolved)
    }

    @Test
    fun theJarAdjacentStoreIsPreferredOverTheOneLevelUpCopy() {
        Files.createDirectories(jarDir)
        Files.writeString(jarAdjacent, "{}")
        val resolved = resolveRuntimeEmbeddingsPath(
            env = emptyMap(),
            jarDir = jarDir,
            userHome = tempDir.toString()
        )
        assertEquals(jarAdjacent, resolved)
    }

    // ---- the second half of F1: a stale store is never sticky ---------------

    private fun fixtureStore(marker: String) = InMemoryEmbeddingStore<TextSegment>().also {
        it.add(
            Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)),
            TextSegment.from(marker, Metadata.from("file_name", "$marker.md"))
        )
    }

    private fun markerOf(store: InMemoryEmbeddingStore<TextSegment>?) =
        store?.serializeToJson()?.let { Regex("\"text\":\"([A-Z0-9_]+)\"").find(it)?.groupValues?.get(1) }

    private fun downloader(marker: String, calls: AtomicInteger, lastModified: Instant?): () -> RemoteEmbeddings? = {
        calls.incrementAndGet()
        val file = Files.createTempFile(tempDir, "dl", ".json")
        fixtureStore(marker).serializeToFile(file)
        RemoteEmbeddings(fixtureStore(marker), RagStore.GITHUB_RELEASE_URL, lastModified, file)
    }

    @Test
    fun aStaleCacheIsRefreshedInsideTheSevenDayWindowInsteadOfBeingServed() {
        // The exact disk state the audit found: a body downloaded yesterday whose
        // own generated_at is 2025-10-21 - inside the 7-day cache window, and 320
        // days old. It used to be served for the rest of the week.
        val cache = tempDir.resolve("home").resolve("embeddings.json")
        val calls = AtomicInteger()
        val gitlabAge = Instant.parse("2025-10-21T09:13:14Z")
        val yesterday = Instant.parse("2026-09-05T18:58:17Z")
        RagStore.loadCachedOrRemote(cache, yesterday, downloader("STALE_GITLAB", calls, gitlabAge))
        assertEquals(1, calls.get())

        val today = Instant.parse("2026-09-06T09:00:00Z")
        val fresh = Instant.parse("2026-09-04T20:53:38Z")
        val result = RagStore.loadCachedOrRemote(cache, today, downloader("FRESH_RELEASE", calls, fresh))
        assertEquals(2, calls.get(), "a 320-day-old cached store must not be served without trying the release asset")
        assertEquals("FRESH_RELEASE", markerOf(result!!.store))
        assertFalse(result.provenance.staleWarning(today) != null, "the served store is the fresh one")
    }

    @Test
    fun aStaleCacheIsStillServedWhenTheRefreshFailsAndIsReportedAsStale() {
        val cache = tempDir.resolve("home").resolve("embeddings.json")
        val calls = AtomicInteger()
        val gitlabAge = Instant.parse("2025-10-21T09:13:14Z")
        RagStore.loadCachedOrRemote(cache, Instant.parse("2026-09-05T18:58:17Z"), downloader("STALE_GITLAB", calls, gitlabAge))

        val today = Instant.parse("2026-09-06T09:00:00Z")
        val result = RagStore.loadCachedOrRemote(cache, today) { calls.incrementAndGet(); null }
        assertEquals(2, calls.get(), "the refresh was attempted")
        assertNotNull(result, "an old index still beats no index")
        assertEquals("STALE_GITLAB", markerOf(result!!.store))
        assertNotNull(result.provenance.staleWarning(today), "and it is REPORTED as stale")
        assertEquals(gitlabAge, result.provenance.generatedAt)
    }

    @Test
    fun aFallbackDownloadOlderThanTheCacheNeverReplacesIt() {
        val cache = tempDir.resolve("home").resolve("embeddings.json")
        val calls = AtomicInteger()
        val fresh = Instant.parse("2026-09-04T20:53:38Z")
        val now = Instant.parse("2026-09-05T12:00:00Z")
        RagStore.loadCachedOrRemote(cache, now, downloader("FRESH_RELEASE", calls, fresh))

        // A week later the GitHub asset is momentarily 404 and the GitLab package
        // answers with a body generated 2025-10-21. It must not become the cache.
        val later = now.plus(RagStore.CACHE_REFRESH_AFTER).plusSeconds(1)
        val gitlabAge = Instant.parse("2025-10-21T09:13:14Z")
        RagStore.loadCachedOrRemote(cache, later, downloader("STALE_GITLAB", calls, gitlabAge))
        assertEquals(2, calls.get())

        val afterwards = RagStore.loadCachedOrRemote(cache, later.plusSeconds(60)) { calls.incrementAndGet(); null }
        assertEquals(3, calls.get())
        assertEquals("FRESH_RELEASE", markerOf(afterwards!!.store), "the newer cached body survived the older download")
        assertEquals(fresh, afterwards.provenance.generatedAt)
    }

    @Test
    fun theRuntimeStorePrefersTheFreshRemoteOverAStaleLocalFileAndReportsIt() {
        val local = tempDir.resolve("embeddings.json")
        fixtureStore("STALE_LOCAL").serializeToFile(local)
        Files.setLastModifiedTime(
            local,
            java.nio.file.attribute.FileTime.from(Instant.now().minus(Duration.ofDays(320)))
        )
        val registryCalled = AtomicInteger()
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = local,
            registryLoader = { registryCalled.incrementAndGet(); fixtureStore("FRESH_REMOTE") }
        )
        assertEquals(1, registryCalled.get(), "a 320-day-old local file is not the freshest usable store")
        assertEquals("FRESH_REMOTE", markerOf(store.embeddingStore))
    }

    @Test
    fun theStaleLocalFileIsStillUsedWhenNothingFresherLoadsAndIsReported() {
        val local = tempDir.resolve("embeddings.json")
        fixtureStore("STALE_LOCAL").serializeToFile(local)
        val age = Instant.now().minus(Duration.ofDays(320))
        Files.setLastModifiedTime(local, java.nio.file.attribute.FileTime.from(age))
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = local,
            registryLoader = { null }
        )
        assertEquals("STALE_LOCAL", markerOf(store.embeddingStore))
        assertNotNull(store.provenance, "a served store always carries its provenance")
        assertNotNull(store.staleWarning(), "and a stale fallback is REPORTED, never silent")
    }

    @Test
    fun aFreshLocalFileIsServedWithoutTouchingTheNetwork() {
        val local = tempDir.resolve("embeddings.json")
        fixtureStore("FRESH_LOCAL").serializeToFile(local)
        val registryCalled = AtomicInteger()
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = local,
            registryLoader = { registryCalled.incrementAndGet(); fixtureStore("REMOTE") }
        )
        assertEquals(0, registryCalled.get())
        assertEquals("FRESH_LOCAL", markerOf(store.embeddingStore))
    }
}
