package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.RagStore
import org.chromia.tools.embeddingStoreSegments
import org.chromia.tools.persistLocalEmbeddings
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RagStoreFetchByIdTest {

    private val authSegment = TextSegment.from(
        "FT4 authentication uses auth descriptors and require_mandatory_flags on the main descriptor.",
        Metadata.from("file_name", "ft4-auth.md")
    )
    private val rellSegment = TextSegment.from(
        "Rell compiler pipeline is S_ then C_ passes then R_ then RR_ then Rt.",
        Metadata.from("file_name", "rell-compiler.md")
    )
    private val postchainSegment = TextSegment.from(
        "Postchain writes one JDBC transaction per block with SAVEPOINT per GTX.",
        Metadata.from("file_name", "postchain.md")
    )

    private fun store(): RagStore = object : RagStore(loadFromRegistry = false) {
        override fun query(query: String): List<TextSegment>? {
            val hits = listOf(authSegment, rellSegment).filter { segment ->
                segment.text().contains(query, ignoreCase = true) ||
                    (segment.metadata()?.getString("file_name")?.contains(query, ignoreCase = true) == true)
            }
            return hits.ifEmpty { null }?.also { rememberQueryHits(it) }
        }
    }

    private fun writeFixture(@TempDir tempDir: Path): Path {
        val path = tempDir.resolve("embeddings.json")
        val fixture = InMemoryEmbeddingStore<TextSegment>().also { store ->
            store.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), authSegment)
            store.add(Embedding.from(floatArrayOf(0.2f, 0.1f, 0.3f)), rellSegment)
            store.add(Embedding.from(floatArrayOf(0.3f, 0.3f, 0.1f)), postchainSegment)
        }
        persistLocalEmbeddings(fixture, path)
        return path
    }

    private fun loadedStore(path: Path): RagStore = RagStore(
        loadFromRegistry = true,
        localEmbeddingsPath = path,
        registryLoader = { null }
    )

    private fun queryingStore(path: Path): RagStore = object : RagStore(
        loadFromRegistry = true,
        localEmbeddingsPath = path,
        registryLoader = { null }
    ) {
        override fun query(query: String): List<TextSegment>? {
            val hits = embeddingStoreSegments(embeddingStore ?: return null).filter { segment ->
                segment.text().contains(query, ignoreCase = true)
            }
            return hits.ifEmpty { null }
        }
    }

    @Test
    fun knownIdAfterQueryHitsExactSegment() {
        val rag = store()
        val hits = rag.query("FT4 authentication")
        assertEquals(1, hits?.size)
        val knownId = segmentId(authSegment)
        assertEquals(authSegment.text(), rag.fetchById(knownId)?.text())
    }

    @Test
    fun unknownIdDoesNotReturnFilenameNeighbor() {
        val rag = store()
        rag.query("FT4 authentication")
        val knownId = segmentId(authSegment)
        val fakeId = "ft4-auth.md-deadbeef-99"
        assertNull(rag.fetchById(fakeId), "filename-similar unknown id must not return a neighbor")
        assertEquals(authSegment.text(), rag.fetchById(knownId)?.text())
    }

    @Test
    fun unknownIdDoesNotReturnQueryTextNeighbor() {
        val rag = store()
        assertNull(rag.fetchById("FT4 authentication"), "fetch-by-id must not fall back to fuzzy query")
        assertNull(rag.fetchById("rell-compiler.md"))
    }

    @Test
    fun queryRemainsFuzzy() {
        val rag = store()
        val hits = rag.query("auth descriptors")
        assertEquals(1, hits?.size)
        assertTrue(hits!![0].text().contains("FT4 authentication"))
        val rellHits = rag.query("compiler pipeline")
        assertEquals(1, rellHits?.size)
        assertTrue(rellHits!![0].text().contains("Rell compiler pipeline"))
    }

    @Test
    fun fetchByIdWorksOnFreshStoreWithoutPriorQuery(@TempDir tempDir: Path) {
        val path = writeFixture(tempDir)
        val rag = loadedStore(path)
        assertEquals(authSegment.text(), rag.fetchById(segmentId(authSegment))?.text())
        assertEquals(rellSegment.text(), rag.fetchById(segmentId(rellSegment))?.text())
    }

    @Test
    fun searchOnStoreAFetchOnStoreBHitsSameId(@TempDir tempDir: Path) {
        val path = writeFixture(tempDir)
        val storeA = queryingStore(path)
        val storeB = loadedStore(path)

        val hits = storeA.query("FT4 authentication")
        assertEquals(1, hits?.size)
        val id = segmentId(hits!![0])
        assertEquals(64, id.length)
        assertTrue(id.matches(Regex("[0-9a-f]{64}")))
        assertEquals(authSegment.text(), storeB.fetchById(id)?.text())
        assertNull(storeB.fetchById("ft4-auth.md-deadbeef-99"), "filename-similar unknown id must not hit")
        assertNull(storeB.fetchById("ft4-auth.md"), "filename must not hit")
        assertNull(storeB.fetchById("FT4 authentication"), "query text must not hit")
        assertNotEquals(id, segmentId(rellSegment))
        assertNull(storeB.fetchById(segmentId(rellSegment).dropLast(1) + "0"))
    }

    @Test
    fun fetchByIdAcceptsUppercaseSha256HexAfterQuery() {
        val rag = store()
        rag.query("FT4 authentication")
        val id = segmentId(authSegment)
        assertTrue(id.matches(Regex("[0-9a-f]{64}")))
        assertEquals(authSegment.text(), rag.fetchById(id.uppercase())?.text())
        assertEquals(authSegment.text(), rag.fetchById(id)?.text())
    }

    @Test
    fun fetchByIdAcceptsMixedCaseSha256HexOnFreshStore(@TempDir tempDir: Path) {
        val path = writeFixture(tempDir)
        val rag = loadedStore(path)
        val id = segmentId(authSegment)
        val mixed = id.mapIndexed { index, ch ->
            if (index % 2 == 0) ch.uppercaseChar() else ch
        }.joinToString("")
        assertTrue(mixed.any { it.isUpperCase() })
        assertTrue(mixed.any { it.isLowerCase() || it.isDigit() })
        assertEquals(authSegment.text(), rag.fetchById(mixed)?.text())
        assertEquals(authSegment.text(), rag.fetchById(id.uppercase())?.text())
        assertEquals(rellSegment.text(), rag.fetchById(segmentId(rellSegment).uppercase())?.text())
    }

    @Test
    fun fetchByIdNormalizesHexCaseAndStillMissesUnknownIds(@TempDir tempDir: Path) {
        val path = writeFixture(tempDir)
        val rag = loadedStore(path)
        val unknownUpper = "A".repeat(64)
        assertNull(rag.fetchById(unknownUpper), "uppercase unknown hex must not hit a neighbor")
        assertNull(rag.fetchById("FT4 AUTHENTICATION"))
        assertNull(rag.fetchById("ft4-auth.md"))
        assertEquals(authSegment.text(), rag.fetchById("  " + segmentId(authSegment).uppercase() + "  ")?.text())
    }

    @Test
    fun initialStoreIndexesForCaseInsensitiveFetchById() {
        val fixture = InMemoryEmbeddingStore<TextSegment>().also { store ->
            store.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), authSegment)
            store.add(Embedding.from(floatArrayOf(0.2f, 0.1f, 0.3f)), rellSegment)
        }
        val rag = RagStore(
            loadFromRegistry = false,
            initialStore = fixture
        )
        assertEquals(authSegment.text(), rag.fetchById(segmentId(authSegment))?.text())
        assertEquals(authSegment.text(), rag.fetchById(segmentId(authSegment).uppercase())?.text())
        assertEquals(rellSegment.text(), rag.fetchById(segmentId(rellSegment).uppercase())?.text())
        assertNull(rag.fetchById("ft4-auth.md"))
    }
}
