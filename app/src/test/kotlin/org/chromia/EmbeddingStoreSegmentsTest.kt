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
    fun listsSegmentsFromTheRealModelsIndexAndPreservesIds() {
        val segment = TextSegment.from(
            "Rell compiles through S_ then thirteen C_ passes.",
            Metadata.from(
                mapOf(
                    "file_name" to "rell.md",
                    "url" to "https://docs.chromia.com/rell"
                )
            )
        )
        // Embedded by the model the server ships, at its real width - a
        // hand-written three-float vector used to stand here.
        val store = TestDocsIndex.index(listOf(segment))

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
    fun listsSegmentsOfAStoreBuiltByAForeignEmbedderAtEitherKindOfWidth() {
        // These vectors are NOT a substitute for the shipped embedder - they are
        // an index built by a DIFFERENT one, which is the entire reason
        // `embeddingStoreSegments` looks past the model's own 384: it tries the
        // fallback widths (64, 768) with `search`, and for a width it does not
        // know it parses `serializeToJson()` instead. Neither branch has an input
        // the real model can produce, so this is the one place in the file that
        // writes a vector by hand, and it covers both:
        //
        //   - 64 wide: a known fallback width, listed through public search;
        //   - 3 wide: an unknown width, where every search() throws on the
        //     dimension mismatch and the serializeToJson() parse takes over.
        val searched = TextSegment.from(
            "search lists every same-width entry",
            Metadata.from("file_name", "search.md")
        )
        val viaSearch = InMemoryEmbeddingStore<TextSegment>().also {
            it.add(Embedding.from(FloatArray(64) { 0.01f }), searched)
        }
        val searchSegments = embeddingStoreSegments(viaSearch)
        assertEquals(1, searchSegments.size)
        assertEquals(searched.text(), searchSegments.single().text())
        assertEquals("search.md", segmentMetadataValue(searchSegments.single(), "file_name"))
        assertEquals(segmentId(searched), segmentId(searchSegments.single()))

        val parsed = TextSegment.from(
            "an unknown width falls back to the public JSON",
            Metadata.from("file_name", "fallback.md")
        )
        val viaJson = InMemoryEmbeddingStore<TextSegment>().also {
            it.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), parsed)
        }
        val jsonSegments = embeddingStoreSegments(viaJson)
        assertEquals(1, jsonSegments.size)
        assertEquals(parsed.text(), jsonSegments.single().text())
        assertEquals("fallback.md", segmentMetadataValue(jsonSegments.single(), "file_name"))
        assertEquals(segmentId(parsed), segmentId(jsonSegments.single()))
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
        // Written by the production writer, embedded by the production model.
        val path = TestDocsIndex.persist(tempDir.resolve("fixture-embeddings.json"), segment)
        // `remoteUrls = emptyList()` is an air-gapped install with no remote
        // configured - a real deployment, and the same code path. It replaced
        // `registryLoader = { null }`, a constructor lambda no production caller
        // ever passed, which skipped the entire download path.
        val rag = RagStore(
            loadFromRegistry = true,
            localEmbeddingsPath = path,
            remoteUrls = emptyList(),
            cacheEmbeddingsPath = tempDir.resolve("cache").resolve(RagStore.FILE_NAME)
        )
        assertEquals(segment.text(), rag.fetchById(segmentId(segment))?.text())
        assertEquals(segment.text(), rag.fetchById(segmentId(segment).uppercase())?.text())
        assertNull(rag.fetchById("directory.md"))
        assertFalse(path.toAbsolutePath().toString().endsWith("app/build/embeddings.json"))
        assertFalse(path.toAbsolutePath() == production.absoluteFile.toPath())
        assertFalse(path.toAbsolutePath() == productionAlt.absoluteFile.toPath())
    }
}
