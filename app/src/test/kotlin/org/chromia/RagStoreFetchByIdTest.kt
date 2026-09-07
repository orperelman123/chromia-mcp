package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import org.chromia.tools.RagStore
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * The id round-trip: what `search`/`fetch_docs` hands out must be what `fetch`
 * takes back, and nothing else must.
 *
 * Every store here is a real [RagStore] over a real index built with the model
 * the server ships ([TestDocsIndex]) - real `query()`, real `lexicalHits`/
 * `mergeHits`, real `fetchById`, real segment-id index.
 *
 * Two things changed on 2026-09-07. The stores that meant "load this file, do
 * not go to the registry" passed `registryLoader = { null }`, a constructor
 * lambda no production caller ever used that replaced the whole download path;
 * they now pass `remoteUrls = emptyList()`, a real deployment (an air-gapped
 * install with no remote configured) that takes the same code path. And the
 * hit-COUNT assertions are gone: with the real BGE-small encoder two unrelated
 * English sentences still sit well above `minScore = 0.6`, so in a fixture index
 * of two or three segments practically everything is retrieved for practically
 * every query - exactly as in production, where the counts were never the point.
 * What a retrieval test can honestly assert is which segment LEADS.
 */
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

    // Real stores, real query()/fetchById(). Both of these used to be
    // `object : RagStore { override fun query(...) }` - a substring filter
    // standing in for our own retrieval, which meant the id round-trip these
    // tests exist to prove was never driven by the code that produces the ids.
    private fun store(): RagStore = TestDocsIndex.store(authSegment, rellSegment)

    private fun writeFixture(@TempDir tempDir: Path): Path =
        TestDocsIndex.persist(tempDir.resolve("embeddings.json"), authSegment, rellSegment, postchainSegment)

    /**
     * The production loader over [path], with no remote configured at all - the
     * air-gapped shape, which is how this reaches `fetchById` on a store the test
     * never queried.
     */
    private fun loadedStore(path: Path): RagStore = RagStore(
        loadFromRegistry = true,
        localEmbeddingsPath = path,
        remoteUrls = emptyList(),
        cacheEmbeddingsPath = path.resolveSibling("cache").resolve(RagStore.FILE_NAME)
    )

    private fun queryingStore(path: Path): RagStore = TestDocsIndex.storeLoadedFrom(path)

    /** The segment a query led with, or null when nothing came back. */
    private fun leader(hits: List<TextSegment>?): TextSegment? = hits?.firstOrNull()

    @Test
    fun knownIdAfterQueryHitsExactSegment() {
        val rag = store()
        val hits = rag.query("FT4 authentication")
        assertEquals(authSegment.text(), leader(hits)?.text(), "the asked-about segment leads: $hits")
        val knownId = segmentId(authSegment)
        assertEquals(authSegment.text(), rag.fetchById(knownId)?.text())
    }

    @Test
    fun unknownIdDoesNotReturnFilenameNeighbor() {
        val rag = store()
        rag.query("FT4 authentication")
        val knownId = segmentId(authSegment)
        val unknownButFilenameShaped = "ft4-auth.md-deadbeef-99"
        assertNull(rag.fetchById(unknownButFilenameShaped), "filename-similar unknown id must not return a neighbor")
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
        assertTrue(hits!!.isNotEmpty())
        assertTrue(hits!![0].text().contains("FT4 authentication"), "wording, not the exact phrase, still leads: $hits")
        val rellHits = rag.query("compiler pipeline")
        assertTrue(rellHits!!.isNotEmpty())
        assertTrue(rellHits!![0].text().contains("Rell compiler pipeline"), "and the other query leads with the other segment: $rellHits")
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
        val hit = leader(hits)
        assertEquals(authSegment.text(), hit?.text(), "the asked-about segment leads: $hits")
        val id = segmentId(hit!!)
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
        // The index handed to the constructor is built by the real embedder, at
        // the real width; a hand-written three-float vector used to stand here,
        // and a fixture at a width the shipped model never produces is the kind
        // that passes while production would not.
        val rag = RagStore(
            loadFromRegistry = false,
            initialStore = TestDocsIndex.index(listOf(authSegment, rellSegment))
        )
        assertEquals(authSegment.text(), rag.fetchById(segmentId(authSegment))?.text())
        assertEquals(authSegment.text(), rag.fetchById(segmentId(authSegment).uppercase())?.text())
        assertEquals(rellSegment.text(), rag.fetchById(segmentId(rellSegment).uppercase())?.text())
        assertNull(rag.fetchById("ft4-auth.md"))
    }
}
