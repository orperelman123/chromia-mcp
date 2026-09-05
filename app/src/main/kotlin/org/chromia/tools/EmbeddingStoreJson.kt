package org.chromia.tools

import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

/**
 * Streaming reader for the `InMemoryEmbeddingStore` JSON that
 * `serializeToFile` writes: `{"entries":[{"id","embedding":{"vector":[..]},"embedded":{"text","metadata":{"metadata":{..}}}}]}`.
 *
 * `InMemoryEmbeddingStore.fromFile` does `Files.readAllBytes` + one Gson tree
 * over the whole document. Measured 2026-09-04 against the fresh 150 MB /
 * 25823-segment store under the production JVM flags (512 MB box:
 * `-XX:MaxRAMPercentage=35` = 179 MB heap, `-XX:MaxDirectMemorySize=64m`):
 * the read itself failed with "Cannot reserve 150059434 bytes of direct
 * buffer memory" and the server silently fell back to the 318-day-old
 * registry package. Even without that cap the 150 MB string plus the parsed
 * tree would not fit the heap. Reading entry by entry keeps the transient at
 * one entry (~6 KB) and the live footprint at the store itself (~100 MB).
 */
internal object EmbeddingStoreJson {

    /** Entries handed to the store per `addAll`, bounding the id/embedding/segment staging lists. */
    private const val BATCH_SIZE = 1000

    fun read(path: Path): InMemoryEmbeddingStore<TextSegment> =
        Files.newInputStream(path).buffered().use { read(it) }

    fun read(input: InputStream): InMemoryEmbeddingStore<TextSegment> {
        val batch = StoreBatcher()
        readEntries(input) { id, embedding, segment -> batch.add(id, embedding, segment) }
        return batch.finish()
    }

    /** Streams every entry of the store JSON on [input] to [sink] without building a store. */
    fun readEntries(input: InputStream, sink: (id: String, embedding: Embedding, segment: TextSegment) -> Unit) {
        val intern = Interner()
        JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
            reader.beginObject()
            var sawEntries = false
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "entries" -> {
                        sawEntries = true
                        reader.beginArray()
                        while (reader.hasNext()) {
                            val (id, embedding, segment) = readEntry(reader, intern)
                            sink(id, embedding, segment)
                        }
                        reader.endArray()
                    }
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            require(sawEntries) { "embedding store JSON has no \"entries\" array" }
        }
    }

    /** Hands entries to an [InMemoryEmbeddingStore] in batches of [BATCH_SIZE], bounding the staging lists. */
    class StoreBatcher {
        private val store = InMemoryEmbeddingStore<TextSegment>()
        private val ids = ArrayList<String>(BATCH_SIZE)
        private val embeddings = ArrayList<Embedding>(BATCH_SIZE)
        private val segments = ArrayList<TextSegment>(BATCH_SIZE)

        fun add(id: String, embedding: Embedding, segment: TextSegment) {
            ids += id; embeddings += embedding; segments += segment
            if (ids.size >= BATCH_SIZE) flush()
        }

        private fun flush() {
            if (ids.isEmpty()) return
            store.addAll(ArrayList(ids), ArrayList(embeddings), ArrayList(segments))
            ids.clear(); embeddings.clear(); segments.clear()
        }

        fun finish(): InMemoryEmbeddingStore<TextSegment> {
            flush()
            return store
        }
    }

    private data class Entry(val id: String, val embedding: Embedding, val segment: TextSegment)

    /**
     * Metadata values repeat per source file (`file_name`, `absolute_directory_path`,
     * small `index` numbers): one String per distinct value instead of one per
     * segment. Bounded by the number of distinct values, a few thousand.
     */
    private class Interner {
        private val pool = HashMap<String, String>()
        fun of(s: String): String = pool.getOrPut(s) { s }
    }

    private fun readEntry(reader: JsonReader, intern: Interner): Entry {
        var id: String? = null
        var vector: FloatArray? = null
        var segment: TextSegment? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "embedding" -> vector = readEmbedding(reader)
                "embedded" -> segment = readSegment(reader, intern)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Entry(
            requireNotNull(id) { "entry without id" },
            Embedding.from(requireNotNull(vector) { "entry $id without embedding.vector" }),
            requireNotNull(segment) { "entry $id without embedded text" }
        )
    }

    private fun readEmbedding(reader: JsonReader): FloatArray {
        var vector: FloatArray? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "vector" -> {
                    // Vectors are fixed-width (384 for bge-small); a growable buffer avoids assuming it.
                    var buf = FloatArray(384)
                    var n = 0
                    reader.beginArray()
                    while (reader.hasNext()) {
                        if (n == buf.size) buf = buf.copyOf(buf.size * 2)
                        buf[n++] = reader.nextDouble().toFloat()
                    }
                    reader.endArray()
                    vector = if (n == buf.size) buf else buf.copyOf(n)
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return requireNotNull(vector) { "embedding without vector" }
    }

    private fun readSegment(reader: JsonReader, intern: Interner): TextSegment {
        var text: String? = null
        var metadata = Metadata()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "text" -> text = reader.nextString()
                "metadata" -> metadata = readMetadataWrapper(reader, intern)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return TextSegment.from(requireNotNull(text) { "embedded segment without text" }, metadata)
    }

    /** `"metadata":{"metadata":{k:v}}` - langchain4j wraps the map in the Metadata object. */
    private fun readMetadataWrapper(reader: JsonReader, intern: Interner): Metadata {
        var metadata = Metadata()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "metadata" -> metadata = readMetadataMap(reader, intern)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return metadata
    }

    private fun readMetadataMap(reader: JsonReader, intern: Interner): Metadata {
        val map = LinkedHashMap<String, Any>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = intern.of(reader.nextName())
            when (reader.peek()) {
                JsonToken.STRING -> map[key] = intern.of(reader.nextString())
                JsonToken.NUMBER -> {
                    val literal = reader.nextString()
                    map[key] = literal.toLongOrNull() ?: literal.toDouble()
                }
                JsonToken.BOOLEAN -> map[key] = reader.nextBoolean().toString()
                JsonToken.NULL -> reader.nextNull()
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Metadata.from(map)
    }
}
