package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.EmbeddingStoreBinary
import org.chromia.tools.EmbeddingStoreJson
import org.chromia.tools.RagStore
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random

/**
 * The binary cache body (2026-09-05): parsing the published JSON was 11 s of a
 * 17 s boot-to-first-answer. The cache re-encodes the parsed store into a flat
 * layout that reads back in a fraction of that. These tests pin the round trip;
 * the speed claim is measured on the real store in the round-17 board entry.
 */
class EmbeddingStoreBinaryTest {

    @TempDir
    lateinit var tempDir: Path

    private fun entries(count: Int, dims: Int, seed: Int = 1): List<Triple<String, Embedding, TextSegment>> {
        val rnd = Random(seed)
        return (0 until count).map { i ->
            Triple(
                "id-$i-${rnd.nextInt()}",
                Embedding.from(FloatArray(dims) { rnd.nextFloat() * 2 - 1 }),
                TextSegment.from(
                    "segment $i: ünïcödé ✓ and `chr test --tests` ${"x".repeat(rnd.nextInt(0, 900))}",
                    Metadata.from(
                        mapOf(
                            "file_name" to "doc-${i % 7}.md",
                            "index" to i.toLong(),
                            "small" to i,
                            "ratio" to i / 3.0,
                            "f" to 1.5f,
                            "absolute_directory_path" to "/docs/${i % 3}"
                        )
                    )
                )
            )
        }
    }

    private fun roundTrip(entries: List<Triple<String, Embedding, TextSegment>>): List<Triple<String, Embedding, TextSegment>> {
        val bytes = ByteArrayOutputStream().also { EmbeddingStoreBinary.write(entries, it) }.toByteArray()
        val back = ArrayList<Triple<String, Embedding, TextSegment>>()
        EmbeddingStoreBinary.readEntries(ByteArrayInputStream(bytes)) { id, e, s -> back += Triple(id, e, s) }
        return back
    }

    @Test
    fun everyEntryFieldSurvivesTheRoundTripBitForBit() {
        val original = entries(count = 50, dims = 384)
        val back = roundTrip(original)
        assertEquals(original.size, back.size)
        original.zip(back).forEach { (a, b) ->
            assertEquals(a.first, b.first)
            assertArrayEquals(a.second.vector(), b.second.vector(), 0f, "vectors are stored as raw float32, not decimal text")
            assertEquals(a.third.text(), b.third.text())
            assertEquals(a.third.metadata().toMap(), b.third.metadata().toMap(), "metadata keeps its value types")
        }
    }

    @Test
    fun emptyAndVariableWidthStoresRoundTrip() {
        assertEquals(0, roundTrip(emptyList()).size)
        val mixed = entries(3, 3) + entries(2, 384, seed = 9)
        assertEquals(listOf(3, 3, 3, 384, 384), roundTrip(mixed).map { it.second.vector().size })
    }

    @Test
    fun theBinaryFileReadsBackAsAStoreThatAnswersLikeTheJsonOne() {
        val json = tempDir.resolve("embeddings.json")
        val original = InMemoryEmbeddingStore<TextSegment>()
        entries(40, 384).forEach { (id, e, s) -> original.add(id, e, s) }
        original.serializeToFile(json)
        val fromJson = EmbeddingStoreJson.read(json)

        val bin = tempDir.resolve("embeddings.bin")
        EmbeddingStoreBinary.write(fromJson, bin)
        val fromBin = EmbeddingStoreBinary.read(bin)

        val probe = entries(40, 384)[17]
        fun top(store: InMemoryEmbeddingStore<TextSegment>) = store.search(
            dev.langchain4j.store.embedding.EmbeddingSearchRequest.builder().queryEmbedding(probe.second).maxResults(3).build()
        ).matches().map { it.embeddingId() to it.score() }
        assertEquals(top(fromJson), top(fromBin))
        assertEquals(probe.first, top(fromBin).first().first)
        assertTrue(Files.size(bin) < Files.size(json), "binary ${Files.size(bin)} B vs JSON ${Files.size(json)} B")
        assertEquals(0, Files.list(tempDir).filter { it.fileName.toString().endsWith(".bin.tmp") }.count(), "the temp file is gone")
    }

    @Test
    fun tinyFixtureStoresAreWrittenThroughTheJsonFallbackListing() {
        // Width 3 matches none of the search probes; entriesOf must still list it.
        val store = InMemoryEmbeddingStore<TextSegment>()
        store.add("a", Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), TextSegment.from("A", Metadata.from("file_name", "a.md")))
        val listed = EmbeddingStoreBinary.entriesOf(store)!!
        assertEquals(listOf("a"), listed.map { it.first })
        val bin = tempDir.resolve("tiny.bin")
        EmbeddingStoreBinary.write(store, bin)
        assertEquals("A", EmbeddingStoreBinary.read(bin).let { s -> EmbeddingStoreBinary.entriesOf(s)!!.single().third.text() })
    }

    @Test
    fun garbageAndForeignFilesAreRejectedNotMisread() {
        assertThrows(IllegalArgumentException::class.java) { EmbeddingStoreBinary.read(ByteArrayInputStream(ByteArray(0))) }
        assertThrows(IllegalArgumentException::class.java) { EmbeddingStoreBinary.read(ByteArrayInputStream("{\"entries\":[]}".toByteArray())) }
        assertThrows(IllegalArgumentException::class.java) { EmbeddingStoreBinary.read(ByteArrayInputStream(ByteArray(4096) { 7 })) }
        // Right magic, wrong version.
        val good = ByteArrayOutputStream().also { EmbeddingStoreBinary.write(entries(1, 4), it) }.toByteArray()
        val bumped = good.copyOf().also { it[good.indexOfVersion() + 3] = 9 }
        val error = assertThrows(IllegalArgumentException::class.java) { EmbeddingStoreBinary.read(ByteArrayInputStream(bumped)) }
        assertTrue(error.message!!.contains("version"), error.message)
        // Truncated body: an EOF, not a half store.
        assertThrows(java.io.EOFException::class.java) { EmbeddingStoreBinary.read(ByteArrayInputStream(good.copyOf(good.size - 10))) }
    }

    private fun ByteArray.indexOfVersion(): Int = "CHROMIA-MCP-EMBEDDINGS".length

    @Test
    fun theCachePathsSitBesideTheJsonName() {
        val cache = Path.of("/home/u/.chromia-mcp/embeddings.json")
        assertEquals(Path.of("/home/u/.chromia-mcp/embeddings.bin"), RagStore.cacheBinaryPath(cache))
        assertEquals(Path.of("/home/u/.chromia-mcp/embeddings.cache.json"), RagStore.cacheMetaPath(cache))
        assertFalse(RagStore.cacheBinaryPath(cache) == RagStore.cacheMetaPath(cache))
    }
}
