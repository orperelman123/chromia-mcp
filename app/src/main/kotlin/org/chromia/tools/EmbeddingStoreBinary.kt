package org.chromia.tools

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists

/**
 * The on-disk cache format for a downloaded index. The published asset is
 * langchain4j's JSON (`serializeToFile`), and parsing it is the whole boot:
 * measured 2026-09-05 on the 143 MB / 25 823-segment store, `EmbeddingStoreJson`
 * takes 11.0 s of a 17 s boot-to-first-answer - 10 million floats read as
 * decimal text. Every other phase is small (embedding model 2.8 s, listing the
 * store 0.2 s, one search 0.13 s). So the cache keeps the store in this flat
 * little-endian layout instead, where a vector is one `readFully` into a
 * `FloatBuffer`:
 *
 * ```
 * magic "CHROMIA-MCP-EMBEDDINGS" | u32 version=1 | u32 count
 * count x [ str id | u32 n | n x f32 | str text | u32 m | m x (str key | u8 type | value) ]
 * str = u32 byte length + UTF-8 bytes; value by type: 1 str, 2 i64, 3 f64, 4 i32, 5 f32
 * ```
 *
 * Nothing but this server reads it; a magic/version mismatch is "corrupt",
 * which the cache loader answers by refreshing from the asset.
 */
internal object EmbeddingStoreBinary {
    private val MAGIC = "CHROMIA-MCP-EMBEDDINGS".toByteArray(StandardCharsets.US_ASCII)
    private const val VERSION = 1
    private const val BUFFER = 1 shl 16

    private const val T_STRING: Int = 1
    private const val T_LONG: Int = 2
    private const val T_DOUBLE: Int = 3
    private const val T_INT: Int = 4
    private const val T_FLOAT: Int = 5

    /** Every entry of [store] as (id, embedding, segment); null when the store cannot be listed. */
    fun entriesOf(store: InMemoryEmbeddingStore<TextSegment>): List<Triple<String, Embedding, TextSegment>>? {
        for (dimension in searchListDimensions()) {
            val listed = try {
                val probe = Embedding.from(FloatArray(dimension) { index -> if (index == 0) 1f else 0f })
                store.search(
                    EmbeddingSearchRequest.builder().queryEmbedding(probe).maxResults(Int.MAX_VALUE).minScore(0.0).build()
                ).matches()
            } catch (_: IllegalArgumentException) {
                null
            }
            if (listed != null) return listed.map { Triple(it.embeddingId(), it.embedding(), it.embedded()) }
        }
        // Tiny fixture stores have a width none of the probes match: round-trip
        // through the JSON they came from (never the production path).
        val entries = ArrayList<Triple<String, Embedding, TextSegment>>()
        EmbeddingStoreJson.readEntries(store.serializeToJson().byteInputStream()) { id, embedding, segment ->
            entries += Triple(id, embedding, segment)
        }
        return entries
    }

    /** Writes [store] to [path] atomically (temp file in the same directory, then move). */
    fun write(store: InMemoryEmbeddingStore<TextSegment>, path: Path) {
        val entries = entriesOf(store) ?: throw IllegalStateException("embedding store cannot be listed")
        path.parent?.createDirectories()
        val dir = path.toAbsolutePath().parent
        val tmp = Files.createTempFile(dir, "embeddings", ".bin.tmp")
        try {
            Files.newOutputStream(tmp).buffered(BUFFER).use { write(entries, it) }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            tmp.deleteIfExists()
        }
    }

    fun write(entries: List<Triple<String, Embedding, TextSegment>>, output: OutputStream) {
        val out = DataOutputStream(output)
        out.write(MAGIC)
        out.writeInt(VERSION)
        out.writeInt(entries.size)
        var vectorBytes = ByteArray(0)
        for ((id, embedding, segment) in entries) {
            writeString(out, id)
            val vector = embedding.vector()
            out.writeInt(vector.size)
            if (vectorBytes.size < vector.size * 4) vectorBytes = ByteArray(vector.size * 4)
            ByteBuffer.wrap(vectorBytes, 0, vector.size * 4).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().put(vector)
            out.write(vectorBytes, 0, vector.size * 4)
            writeString(out, segment.text())
            val metadata = segment.metadata().toMap()
            out.writeInt(metadata.size)
            for ((key, value) in metadata) {
                writeString(out, key)
                when (value) {
                    is Long -> { out.writeByte(T_LONG); out.writeLong(value) }
                    is Int -> { out.writeByte(T_INT); out.writeInt(value) }
                    is Double -> { out.writeByte(T_DOUBLE); out.writeDouble(value) }
                    is Float -> { out.writeByte(T_FLOAT); out.writeFloat(value) }
                    else -> { out.writeByte(T_STRING); writeString(out, value.toString()) }
                }
            }
        }
        out.flush()
    }

    fun read(path: Path): InMemoryEmbeddingStore<TextSegment> =
        Files.newInputStream(path).buffered(BUFFER).use { read(it) }

    fun read(input: InputStream): InMemoryEmbeddingStore<TextSegment> {
        val batch = EmbeddingStoreJson.StoreBatcher()
        readEntries(input) { id, embedding, segment -> batch.add(id, embedding, segment) }
        return batch.finish()
    }

    fun readEntries(input: InputStream, sink: (id: String, embedding: Embedding, segment: TextSegment) -> Unit) {
        val inp = DataInputStream(input)
        val magic = ByteArray(MAGIC.size)
        try {
            inp.readFully(magic)
        } catch (_: EOFException) {
            throw IllegalArgumentException("not a Chromia MCP embeddings file (too short)")
        }
        require(magic.contentEquals(MAGIC)) { "not a Chromia MCP embeddings file (bad magic)" }
        val version = inp.readInt()
        require(version == VERSION) { "embeddings file version $version, expected $VERSION" }
        val count = inp.readInt()
        require(count >= 0) { "negative entry count $count" }
        val intern = HashMap<String, String>()
        var vectorBytes = ByteArray(0)
        repeat(count) {
            val id = readString(inp)
            val n = inp.readInt()
            require(n in 1..65536) { "entry $id: implausible vector width $n" }
            if (vectorBytes.size < n * 4) vectorBytes = ByteArray(n * 4)
            inp.readFully(vectorBytes, 0, n * 4)
            val vector = FloatArray(n)
            ByteBuffer.wrap(vectorBytes, 0, n * 4).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(vector)
            val text = readString(inp)
            val m = inp.readInt()
            require(m in 0..4096) { "entry $id: implausible metadata size $m" }
            val metadata = LinkedHashMap<String, Any>(m * 2)
            repeat(m) {
                val key = readString(inp).let { k -> intern.getOrPut(k) { k } }
                metadata[key] = when (val type = inp.readUnsignedByte()) {
                    T_STRING -> readString(inp).let { s -> intern.getOrPut(s) { s } }
                    T_LONG -> inp.readLong()
                    T_DOUBLE -> inp.readDouble()
                    T_INT -> inp.readInt()
                    T_FLOAT -> inp.readFloat()
                    else -> throw IllegalArgumentException("entry $id: unknown metadata type $type")
                }
            }
            sink(id, Embedding.from(vector), TextSegment.from(text, Metadata.from(metadata)))
        }
    }

    private fun writeString(out: DataOutputStream, s: String) {
        val bytes = s.toByteArray(StandardCharsets.UTF_8)
        out.writeInt(bytes.size)
        out.write(bytes)
    }

    private fun readString(inp: DataInputStream): String {
        val len = inp.readInt()
        require(len in 0..(64 shl 20)) { "implausible string length $len" }
        val bytes = ByteArray(len)
        inp.readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }
}
