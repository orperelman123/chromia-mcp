package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.chromia.tools.RagStore
import org.chromia.tools.embeddingStoreSegments
import org.chromia.tools.persistLocalEmbeddings
import org.chromia.tools.segmentId
import org.chromia.tools.segmentMetadataValue
import org.chromia.tools.textSegmentsFromStoreJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class EmbeddingStoreSegmentsTest {

    @Test
    fun listsSegmentsViaPublicSerializeToJsonAndPreservesIds() {
        val segment = TextSegment.from(
            "Rell compiles through S_ then thirteen C_ passes.",
            Metadata.from(
                mapOf(
                    "file_name" to "rell.md",
                    "url" to "https://docs.chromia.com/rell"
                )
            )
        )
        val store = InMemoryEmbeddingStore<TextSegment>().also {
            it.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), segment)
        }

        val serialized = store.serializeToJson()
        val root = Json.parseToJsonElement(serialized)
        val entries = when (root) {
            is JsonObject -> root["entries"]?.jsonArray
            is JsonArray -> root
            else -> null
        }
        requireNotNull(entries) {
            "InMemoryEmbeddingStore.serializeToJson() must expose an entries array; got ${root::class.simpleName}"
        }
        assertTrue(entries.isNotEmpty(), "serialized store must contain at least one entry")
        val embedded = entries.first().jsonObject["embedded"]?.jsonObject
            ?: error("public JSON entry must include embedded")
        assertTrue(embedded.containsKey("text"), "public JSON embedded must include text")
        val metadata = embedded["metadata"]?.jsonObject
            ?: error("public JSON embedded must include metadata")
        assertTrue(
            metadata.containsKey("file_name") ||
                (metadata["metadata"] as? JsonObject)?.containsKey("file_name") == true,
            "serializeToJson() metadata must expose file_name at metadata or metadata.metadata"
        )

        val segments = embeddingStoreSegments(store)
        assertEquals(1, segments.size)
        assertEquals(segment.text(), segments.single().text())
        assertEquals("rell.md", segmentMetadataValue(segments.single(), "file_name"))
        assertEquals(segmentId(segment), segmentId(segments.single()))
    }

    @Test
    fun missingEntriesFailsLoudWithSerializeToJsonInMessage() {
        val error = assertThrows(IllegalStateException::class.java) {
            textSegmentsFromStoreJson("""{"not_entries":[]}""")
        }
        assertTrue(error.message!!.contains("serializeToJson"))
    }

    @Test
    fun missingEmbeddedFailsLoudWithSerializeToJsonInMessage() {
        val error = assertThrows(IllegalStateException::class.java) {
            textSegmentsFromStoreJson("""{"entries":[{"id":"x"}]}""")
        }
        assertTrue(error.message!!.contains("serializeToJson"))
    }

    @Test
    fun missingTextFailsLoudWithSerializeToJsonInMessage() {
        val error = assertThrows(IllegalStateException::class.java) {
            textSegmentsFromStoreJson("""{"entries":[{"embedded":{"metadata":{}}}]}""")
        }
        assertTrue(error.message!!.contains("serializeToJson"))
    }

    @Test
    fun nonObjectMetadataFailsLoudWithSerializeToJsonInMessage() {
        val error = assertThrows(IllegalStateException::class.java) {
            textSegmentsFromStoreJson(
                """{"entries":[{"embedded":{"text":"hi","metadata":"nope"}}]}"""
            )
        }
        assertTrue(error.message!!.contains("serializeToJson"))
        assertTrue(error.message!!.contains("metadata"))
    }

    @Test
    fun invalidJsonFailsLoudWithSerializeToJsonInMessage() {
        val error = assertThrows(IllegalStateException::class.java) {
            textSegmentsFromStoreJson("not-json")
        }
        assertTrue(error.message!!.contains("serializeToJson"))
    }

    @Test
    fun ragStoreDoesNotWalkLangchainPrivateEntries() {
        val ragStore = File("src/main/kotlin/org/chromia/tools/RagStore.kt").takeIf { it.isFile }
            ?: File("app/src/main/kotlin/org/chromia/tools/RagStore.kt")
        val source = ragStore.readText()
        assertFalse(source.contains("getDeclaredField"), "must not reflect InMemoryEmbeddingStore fields")
        assertFalse(source.contains("isAccessible"), "must not open package-private entries")
        assertFalse(source.contains("InMemoryEmbeddingStore\$Entry"), "must not name the package-private Entry type")
        assertTrue(source.contains("serializeToJson"), "dimension-mismatch fallback must keep serializeToJson")
        assertTrue(source.contains("search"), "must list via public EmbeddingStore.search")
        assertTrue(source.contains("64") && source.contains("384") && source.contains("768"))
        assertFalse(source.contains("/app/build/embeddings.json"), "must not hardcode production embeddings.json")
        assertFalse(source.contains("getDeclaredField"))
    }

    @Test
    fun listsSegmentsViaPublicSearchWhenDimensionIs64() {
        val segment = TextSegment.from(
            "search lists every same-width entry",
            Metadata.from("file_name", "search.md")
        )
        val store = InMemoryEmbeddingStore<TextSegment>().also {
            it.add(Embedding.from(FloatArray(64) { 0.01f }), segment)
        }
        val segments = embeddingStoreSegments(store)
        assertEquals(1, segments.size)
        assertEquals(segment.text(), segments.single().text())
        assertEquals("search.md", segmentMetadataValue(segments.single(), "file_name"))
        assertEquals(segmentId(segment), segmentId(segments.single()))
    }

    @Test
    fun fixtureFileLoadIndexesWithoutProductionEmbeddings(@TempDir tempDir: Path) {
        val production = File("app/build/embeddings.json")
        val productionAlt = File("build/embeddings.json")
        assertTrue(
            !production.absolutePath.startsWith(tempDir.toAbsolutePath().toString()),
            "temp fixture must not be the production embeddings path"
        )
        val segment = TextSegment.from(
            "Directory Chain api_version is 110.",
            Metadata.from("file_name", "directory.md")
        )
        val path = tempDir.resolve("fixture-embeddings.json")
        InMemoryEmbeddingStore<TextSegment>().also { store ->
            store.add(Embedding.from(floatArrayOf(0.4f, 0.5f, 0.6f)), segment)
            persistLocalEmbeddings(store, path)
        }
        val rag = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = path,
            registryLoader = { null }
        )
        assertEquals(segment.text(), rag.fetchById(segmentId(segment))?.text())
        assertEquals(segment.text(), rag.fetchById(segmentId(segment).uppercase())?.text())
        assertNull(rag.fetchById("directory.md"))
        assertFalse(path.toAbsolutePath().toString().endsWith("app/build/embeddings.json"))
        assertFalse(path.toAbsolutePath() == production.absoluteFile.toPath())
        assertFalse(path.toAbsolutePath() == productionAlt.absoluteFile.toPath())
    }
}
