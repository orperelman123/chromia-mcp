package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.RagStore
import org.chromia.tools.loadLocalEmbeddings
import org.chromia.tools.persistLocalEmbeddings
import org.chromia.tools.resolveLocalEmbeddingsPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

class RagStoreLocalEmbeddingsTest {

    @TempDir
    lateinit var tempDir: Path

    private fun fixtureStore(marker: String): InMemoryEmbeddingStore<TextSegment> {
        return InMemoryEmbeddingStore<TextSegment>().also { store ->
            store.add(
                Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)),
                TextSegment.from(marker, Metadata.from("file_name", "$marker.md"))
            )
        }
    }

    private fun writeStore(marker: String, fileName: String = "embeddings.json"): Path {
        val path = tempDir.resolve(fileName)
        fixtureStore(marker).serializeToFile(path)
        return path
    }

    @Test
    fun localFileIsPreferredOverRegistry() {
        val localPath = writeStore("LOCAL_MARKER")
        val registryCalled = AtomicBoolean(false)
        val registryStore = fixtureStore("REGISTRY_MARKER")

        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = localPath,
            registryLoader = {
                registryCalled.set(true)
                registryStore
            }
        )

        assertFalse(registryCalled.get(), "registry must not be contacted when a local file exists")
        assertNotNull(store.embeddingStore)
        assertEquals(
            InMemoryEmbeddingStore.fromFile(localPath).serializeToJson(),
            store.embeddingStore!!.serializeToJson()
        )
        assertTrue(store.embeddingStore!!.serializeToJson().contains("LOCAL_MARKER"))
        assertFalse(store.embeddingStore!!.serializeToJson().contains("REGISTRY_MARKER"))
    }

    @Test
    fun missingLocalFileFallsBackToRegistryStub() {
        val missing = tempDir.resolve("does-not-exist").resolve("embeddings.json")
        val registryCalled = AtomicBoolean(false)
        val registryStore = fixtureStore("REGISTRY_MARKER")

        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = missing,
            registryLoader = {
                registryCalled.set(true)
                registryStore
            }
        )

        assertTrue(registryCalled.get(), "registry stub must run when local file is missing")
        assertSame(registryStore, store.embeddingStore)
        assertTrue(store.embeddingStore!!.serializeToJson().contains("REGISTRY_MARKER"))
    }

    @Test
    fun corruptLocalFileFallsBackToRegistryStub() {
        val corrupt = tempDir.resolve("embeddings.json")
        Files.writeString(corrupt, "{ this is not a valid embeddings store")
        val registryStore = fixtureStore("REGISTRY_MARKER")

        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = corrupt,
            registryLoader = { registryStore }
        )

        assertSame(registryStore, store.embeddingStore)
    }

    @Test
    fun generatePathDoesNotLoadLocalFileEvenIfPresent() {
        val localPath = writeStore("LOCAL_MARKER")
        val registryCalled = AtomicBoolean(false)

        val store = RagStore(
            loadFromRegistry = false,
            localEmbeddingsPath = localPath,
            registryLoader = {
                registryCalled.set(true)
                fixtureStore("REGISTRY_MARKER")
            }
        )

        assertNull(store.embeddingStore)
        assertFalse(registryCalled.get())
    }

    @Test
    fun persistWritesFileThatRuntimeLoadPrefers() {
        val path = tempDir.resolve("nested").resolve("embeddings.json")
        persistLocalEmbeddings(fixtureStore("PERSISTED"), path)
        assertTrue(Files.isRegularFile(path))

        val loaded = loadLocalEmbeddings(path)
        assertNotNull(loaded)
        assertTrue(loaded!!.serializeToJson().contains("PERSISTED"))

        val registryCalled = AtomicBoolean(false)
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = path,
            registryLoader = {
                registryCalled.set(true)
                fixtureStore("REGISTRY_MARKER")
            }
        )
        assertFalse(registryCalled.get())
        assertEquals(loaded.serializeToJson(), store.embeddingStore!!.serializeToJson())
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
    fun missingLocalAndNullRegistryLeavesStoreEmpty() {
        val store = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = tempDir.resolve("missing.json"),
            registryLoader = { null }
        )
        assertNull(store.embeddingStore)
    }
}
