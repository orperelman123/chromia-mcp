package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.EmbeddingStoreJson
import org.chromia.tools.embeddingStoreSegments
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

/**
 * The streaming reader must accept exactly what `InMemoryEmbeddingStore.serializeToFile`
 * writes (the published packages and every local `embeddings.json` were written by it)
 * and give back the same ids, vectors, text and metadata. It replaced
 * `InMemoryEmbeddingStore.fromFile`, which could not read the 150 MB store under the
 * production JVM flags (see [EmbeddingStoreJson]).
 */
class EmbeddingStoreJsonTest {

    @TempDir
    lateinit var tempDir: Path

    /** Distinct per entry: a 1.0 spike at `i % 384` on a floor that grows with `i / 384`. */
    private fun sampleVector(i: Int) = FloatArray(384) { d -> if (d == i % 384) 1f else 0.1f * (i / 384 + 1) }

    private fun sampleStore(entries: Int): InMemoryEmbeddingStore<TextSegment> {
        val store = InMemoryEmbeddingStore<TextSegment>()
        repeat(entries) { i ->
            val vector = sampleVector(i)
            val metadata = Metadata.from(
                mapOf(
                    "file_name" to "doc-$i.md",
                    "index" to "$i",
                    "absolute_directory_path" to "/tmp/chromia_docs_repos1/ft4-lib/doc",
                    "weight" to i.toLong(),
                    "ratio" to i / 4.0
                )
            )
            store.add("id-$i", Embedding.from(vector), TextSegment.from("segment $i: require_mandatory_flags \"quoted\" \\ back\nslash", metadata))
        }
        return store
    }

    @Test
    fun roundTripsWhatSerializeToFileWrites() {
        val original = sampleStore(2503) // spans two full batches plus a partial one
        val path = tempDir.resolve("embeddings.json")
        original.serializeToFile(path)

        val loaded = EmbeddingStoreJson.read(path)

        val before = embeddingStoreSegments(original).associateBy { it.metadata().getString("file_name") }
        val after = embeddingStoreSegments(loaded).associateBy { it.metadata().getString("file_name") }
        assertEquals(before.keys, after.keys)
        assertEquals(2503, after.size)
        before.forEach { (name, segment) ->
            val got = after.getValue(name)
            assertEquals(segment.text(), got.text(), name)
            assertEquals(segment.metadata().getString("index"), got.metadata().getString("index"))
            assertEquals(segment.metadata().getString("absolute_directory_path"), got.metadata().getString("absolute_directory_path"))
            assertEquals(segment.metadata().getLong("weight"), got.metadata().getLong("weight"))
            assertEquals(segment.metadata().getDouble("ratio"), got.metadata().getDouble("ratio"))
        }

        // Vectors survive: the nearest neighbour of an original embedding is its own entry.
        val probe = Embedding.from(sampleVector(42))
        val match = loaded.search(EmbeddingSearchRequest.builder().queryEmbedding(probe).maxResults(1).build()).matches().single()
        assertEquals("doc-42.md", match.embedded().metadata().getString("file_name"))
        assertArrayEquals(probe.vector(), match.embedding().vector(), 1e-6f)
        assertTrue(match.score() > 0.999, "score ${match.score()}")
    }

    @Test
    fun unknownFieldsAreSkippedNotFatal() {
        val json = """
            {"version":"future","entries":[
              {"id":"a","extra":{"x":[1,2]},"embedding":{"vector":[0.5,0.25],"dims":2},
               "embedded":{"text":"hello","lang":"en","metadata":{"metadata":{"file_name":"a.md"},"other":1}}}
            ],"trailer":null}
        """.trimIndent()
        val loaded = EmbeddingStoreJson.read(json.byteInputStream())
        val segment = embeddingStoreSegments(loaded).single()
        assertEquals("hello", segment.text())
        assertEquals("a.md", segment.metadata().getString("file_name"))
    }

    @Test
    fun garbageAndMissingEntriesFail() {
        assertThrows(Exception::class.java) { EmbeddingStoreJson.read("not-an-embedding-store".byteInputStream()) }
        assertThrows(IllegalArgumentException::class.java) { EmbeddingStoreJson.read("""{"nothing":[]}""".byteInputStream()) }
        assertThrows(IllegalArgumentException::class.java) {
            EmbeddingStoreJson.read("""{"entries":[{"id":"a","embedded":{"text":"t"}}]}""".byteInputStream())
        }
    }

    @Test
    fun readsWithASmallDirectMemoryBudgetBecauseItNeverBuffersTheFile() {
        // fromFile's Files.readAllBytes needed a direct buffer the size of the file.
        // The streaming reader goes through an 8 KB buffered stream; this guards the
        // property indirectly: a multi-MB file reads fine through a plain InputStream.
        val path = tempDir.resolve("big.json")
        sampleStore(1200).serializeToFile(path)
        assertTrue(Files.size(path) > 2_000_000, "fixture should be a couple of MB, was ${Files.size(path)}")
        assertEquals(1200, embeddingStoreSegments(EmbeddingStoreJson.read(path)).size)
    }
}
