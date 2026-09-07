package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import org.chromia.tools.RagStore
import org.chromia.tools.loadLocalEmbeddings
import org.chromia.tools.persistLocalEmbeddings
import org.chromia.tools.resolveLocalEmbeddingsPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

/**
 * Which index the runtime answers from, over the REAL loading path.
 *
 * Until 2026-09-07 every test below passed `registryLoader = { ... }`, a
 * constructor lambda no production caller ever used: it handed a store straight
 * over and so replaced the ENTIRE remote step - the HTTP client, the redirect
 * handling, the streaming parse, the cache sidecar and the fallback order - in
 * the tests that claimed to cover it. The lambda is gone from production. What
 * stands in its place is the operator-facing configuration point the constructor
 * now takes, `remoteUrls`, driven two ways:
 *
 *  - `remoteUrls = emptyList()` - a real deployment (an air-gapped install that
 *    ships its own embeddings.json and has no remote to fall back on), not a
 *    stub for one;
 *  - `remoteUrls = listOf(<loopback url>)` - a REAL Ktor server on a real socket
 *    serving a REAL index file written by the production writer. A server that
 *    counts the requests it received is how "the network was not touched" is
 *    proved here, and a real server serving real bytes is not a double.
 */
class RagStoreLocalEmbeddingsTest {

    @TempDir
    lateinit var tempDir: Path

    /**
     * A one-segment index built with the model the server actually ships
     * ([TestDocsIndex]). [marker] is the segment's text, which is how each
     * assertion below identifies the copy that was served.
     */
    private fun fixtureStore(marker: String): InMemoryEmbeddingStore<TextSegment> =
        TestDocsIndex.index(listOf(TextSegment.from(marker, Metadata.from("file_name", "$marker.md"))))

    private fun writeStore(marker: String, fileName: String = "embeddings.json"): Path {
        val path = tempDir.resolve(fileName)
        persistLocalEmbeddings(fixtureStore(marker), path)
        return path
    }

    /**
     * The download cache, always inside the @TempDir. Never the default
     * (`~/.chromia-mcp/embeddings.json`), which on a developer machine is a real
     * 150 MB index this suite would read and overwrite.
     */
    private fun cachePath(name: String): Path = tempDir.resolve("cache-$name").resolve(RagStore.FILE_NAME)

    /**
     * Serves a REAL index file over a REAL socket: the bytes come from
     * [persistLocalEmbeddings], the production writer, and the production HTTP
     * client connects to an ephemeral loopback port to fetch them. [block] gets
     * the URL and a counter of the requests the server actually received.
     */
    private fun <T> servingIndex(marker: String, block: (url: String, hits: AtomicInteger) -> T): T {
        val file = tempDir.resolve("served-$marker.json")
        persistLocalEmbeddings(fixtureStore(marker), file)
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

    @Test
    fun localFileIsPreferredOverTheRemoteIndex() {
        val localPath = writeStore("LOCAL_MARKER")

        servingIndex("REGISTRY_MARKER") { url, hits ->
            val store = RagStore(
                loadFromRegistry = true,
                localEmbeddingsPath = localPath,
                remoteUrls = listOf(url),
                embeddingModel = TestDocsIndex.model,
                cacheEmbeddingsPath = cachePath("local-preferred")
            )

            assertEquals(0, hits.get(), "the remote index must not be contacted when a local file exists")
            assertNotNull(store.embeddingStore)
            assertEquals(
                InMemoryEmbeddingStore.fromFile(localPath).serializeToJson(),
                store.embeddingStore!!.serializeToJson()
            )
            assertTrue(store.embeddingStore!!.serializeToJson().contains("LOCAL_MARKER"))
            assertFalse(store.embeddingStore!!.serializeToJson().contains("REGISTRY_MARKER"))
        }
    }

    @Test
    fun missingLocalFileFallsBackToTheRemoteIndex() {
        val missing = tempDir.resolve("does-not-exist").resolve("embeddings.json")

        servingIndex("REGISTRY_MARKER") { url, hits ->
            val store = RagStore(
                loadFromRegistry = true,
                localEmbeddingsPath = missing,
                remoteUrls = listOf(url),
                embeddingModel = TestDocsIndex.model,
                cacheEmbeddingsPath = cachePath("missing-local")
            )

            assertEquals(1, hits.get(), "the remote must be fetched when the local file is missing")
            assertNotNull(store.embeddingStore)
            // Identity (`assertSame(registryStore, ...)`) was only ever assertable
            // because the removed lambda handed the very object back. A real
            // download parses new objects out of real bytes, so what the served
            // copy is identified by is its content.
            assertTrue(store.embeddingStore!!.serializeToJson().contains("REGISTRY_MARKER"))
        }
    }

    @Test
    fun corruptLocalFileFallsBackToTheRemoteIndex() {
        val corrupt = tempDir.resolve("embeddings.json")
        Files.writeString(corrupt, "{ this is not a valid embeddings store")

        servingIndex("REGISTRY_MARKER") { url, hits ->
            val store = RagStore(
                loadFromRegistry = true,
                localEmbeddingsPath = corrupt,
                remoteUrls = listOf(url),
                embeddingModel = TestDocsIndex.model,
                cacheEmbeddingsPath = cachePath("corrupt-local")
            )

            assertEquals(1, hits.get(), "an unparseable local file must reach the remote")
            assertNotNull(store.embeddingStore)
            assertTrue(store.embeddingStore!!.serializeToJson().contains("REGISTRY_MARKER"))
        }
    }

    @Test
    fun generatePathDoesNotLoadLocalFileEvenIfPresent() {
        val localPath = writeStore("LOCAL_MARKER")

        servingIndex("REGISTRY_MARKER") { url, hits ->
            val store = RagStore(
                loadFromRegistry = false,
                localEmbeddingsPath = localPath,
                remoteUrls = listOf(url),
                cacheEmbeddingsPath = cachePath("generate-path")
            )

            assertNull(store.embeddingStore)
            assertEquals(0, hits.get(), "the generate path loads nothing, locally or remotely")
        }
    }

    @Test
    fun persistWritesFileThatRuntimeLoadPrefers() {
        val path = tempDir.resolve("nested").resolve("embeddings.json")
        persistLocalEmbeddings(fixtureStore("PERSISTED"), path)
        assertTrue(Files.isRegularFile(path))

        val loaded = loadLocalEmbeddings(path)
        assertNotNull(loaded)
        assertTrue(loaded!!.serializeToJson().contains("PERSISTED"))

        servingIndex("REGISTRY_MARKER") { url, hits ->
            val store = RagStore(
                loadFromRegistry = true,
                localEmbeddingsPath = path,
                remoteUrls = listOf(url),
                embeddingModel = TestDocsIndex.model,
                cacheEmbeddingsPath = cachePath("persist-preferred")
            )
            assertEquals(0, hits.get(), "the file just persisted is fresh; nothing is downloaded")
            assertEquals(loaded.serializeToJson(), store.embeddingStore!!.serializeToJson())
        }
    }

    @Test
    fun resolvePathUsesEnvThenDefaultBuildFile() {
        val nothingExists: (Path) -> Boolean = { false }
        assertEquals(
            Path.of("build", "embeddings.json"),
            resolveLocalEmbeddingsPath(emptyMap(), nothingExists)
        )
        assertEquals(
            Path.of("/tmp/custom-embeddings.json"),
            resolveLocalEmbeddingsPath(mapOf(RagStore.EMBEDDINGS_PATH_ENV to "/tmp/custom-embeddings.json"))
        )
        assertEquals(
            Path.of("build", "embeddings.json"),
            resolveLocalEmbeddingsPath(mapOf(RagStore.EMBEDDINGS_PATH_ENV to "   "), nothingExists)
        )
    }

    @Test
    fun resolvePathPrefersAppBuildWhenCwdBuildMissing() {
        val exists: (Path) -> Boolean = { it == Path.of("app", "build", "embeddings.json") }
        assertEquals(
            Path.of("app", "build", "embeddings.json"),
            resolveLocalEmbeddingsPath(emptyMap(), exists)
        )
    }

    @Test
    fun resolvePathPrefersCwdBuildWhenBothExist() {
        val exists: (Path) -> Boolean = {
            it == Path.of("build", "embeddings.json") ||
                it == Path.of("app", "build", "embeddings.json")
        }
        assertEquals(
            Path.of("build", "embeddings.json"),
            resolveLocalEmbeddingsPath(emptyMap(), exists)
        )
    }

    @Test
    fun resolvePathEnvWinsOverExistingAppBuild() {
        val exists: (Path) -> Boolean = { it == Path.of("app", "build", "embeddings.json") }
        assertEquals(
            Path.of("/tmp/custom-embeddings.json"),
            resolveLocalEmbeddingsPath(
                mapOf(RagStore.EMBEDDINGS_PATH_ENV to "/tmp/custom-embeddings.json"),
                exists
            )
        )
    }

    @Test
    fun resolvePathFirstPersistPrefersAppBuildWhenRepoRoot() {
        val exists: (Path) -> Boolean = { it == Path.of("settings.gradle.kts") }
        val directoryExists: (Path) -> Boolean = { it == Path.of("app", "build") }
        assertEquals(
            Path.of("app", "build", "embeddings.json"),
            resolveLocalEmbeddingsPath(emptyMap(), exists, directoryExists)
        )
    }

    @Test
    fun resolvePathFirstPersistKeepsCwdBuildWhenNotRepoRoot() {
        val exists: (Path) -> Boolean = { false }
        val directoryExists: (Path) -> Boolean = { it == Path.of("app", "build") }
        assertEquals(
            Path.of("build", "embeddings.json"),
            resolveLocalEmbeddingsPath(emptyMap(), exists, directoryExists)
        )
    }

    @Test
    fun missingLocalAndNoRemoteConfiguredLeavesStoreEmpty() {
        // `remoteUrls = emptyList()` is not "the registry returned nothing" acted
        // out by a lambda - it is an air-gapped install, a real deployment that
        // ships its own embeddings.json and has no remote at all. The remote step
        // is skipped before the cache is even looked at, and the cache path below
        // is inside the @TempDir so that stays true if that order ever changes.
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = tempDir.resolve("missing.json"),
            remoteUrls = emptyList(),
            cacheEmbeddingsPath = cachePath("air-gapped")
        )
        assertNull(store.embeddingStore)
    }
}
