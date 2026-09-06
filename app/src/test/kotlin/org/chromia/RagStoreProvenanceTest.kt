package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.tools.FetchDocsStrategy
import org.chromia.tools.RagStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Duration
import java.time.Instant

/**
 * Round 10 (2026-09-04): the published registry package was generated
 * 2025-10-21 and nothing said so - production answered from a year-old store
 * and `fetch_docs` could not find FT4's `require_mandatory_flags`. The index
 * now carries its origin and age, logs them, and tells the agent on every
 * fetch_docs answer when it is past the freshness limit.
 */
class RagStoreProvenanceTest {

    @TempDir
    lateinit var tempDir: Path

    private val now = Instant.parse("2026-09-04T12:00:00Z")

    private fun fixtureStore(vararg markers: String): InMemoryEmbeddingStore<TextSegment> =
        InMemoryEmbeddingStore<TextSegment>().also { store ->
            markers.forEach { marker ->
                store.add(Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), TextSegment.from(marker, Metadata.from("file_name", "$marker.md")))
            }
        }

    @Test
    fun aFreshIndexHasNoWarningAndAnOldOneNamesDateAgeAndFix() {
        val fresh = RagStore.Provenance("local file x", now.minus(Duration.ofDays(30)), 5)
        assertNull(fresh.staleWarning(now))
        assertEquals("documentation index: 5 segments from local file x, generated 2026-08-05 (30 days old)", fresh.describe(now))

        val old = RagStore.Provenance("GitLab registry package p", Instant.parse("2025-10-21T09:13:14Z"), 3084)
        val warning = old.staleWarning(now)
        assertNotNull(warning)
        assertTrue(warning!!.contains("STALE"), warning)
        assertTrue(warning.contains("generated 2025-10-21"), warning)
        assertTrue(warning.contains("318 days ago"), warning)
        assertTrue(warning.contains("limit 120"), warning)
        assertTrue(warning.contains("GitLab tags"), "the agent must be told how to verify: $warning")
        // The fix is the workflow anyone with repo access can run, not a GitLab token only ChromaWay holds.
        assertTrue(warning.contains("Embeddings refresh") && warning.contains(RagStore.GITHUB_RELEASE_URL), "the operator must be told how to fix: $warning")
        assertTrue(warning.contains(RagStore.EMBEDDINGS_PATH_ENV), warning)

        // Exactly at the limit is still fresh; one day past is not.
        assertNull(RagStore.Provenance("x", now.minus(Duration.ofDays(120)), 1).staleWarning(now))
        assertNotNull(RagStore.Provenance("x", now.minus(Duration.ofDays(121)), 1).staleWarning(now))
    }

    @Test
    fun anIndexOfUnknownAgeIsDescribedButNeverCalledStale() {
        val unknown = RagStore.Provenance("GitLab registry package p", null, 7)
        assertNull(unknown.staleWarning(now))
        assertEquals("documentation index: 7 segments from GitLab registry package p, generated unknown date", unknown.describe(now))
        assertNull(unknown.ageDays(now))
    }

    @Test
    fun lastModifiedHeaderParsesAsRfc1123AndGarbageIsNull() {
        assertEquals(Instant.parse("2025-10-21T09:13:14Z"), RagStore.parseLastModified("Tue, 21 Oct 2025 09:13:14 GMT"))
        assertEquals(Instant.parse("2025-10-21T09:13:14Z"), RagStore.parseLastModified("  Tue, 21 Oct 2025 09:13:14 GMT "))
        assertNull(RagStore.parseLastModified(null))
        assertNull(RagStore.parseLastModified(""))
        assertNull(RagStore.parseLastModified("2025-10-21"))
        assertNull(RagStore.parseLastModified("not a date"))
    }

    @Test
    fun aLocalLoadRecordsPathMtimeAndSegmentCount() {
        val path = tempDir.resolve("embeddings.json")
        fixtureStore("A", "B", "C").serializeToFile(path)
        val generated = Instant.parse("2026-08-26T16:54:00Z")
        Files.setLastModifiedTime(path, FileTime.from(generated))

        val store = RagStore(loadFromRegistry = true, localEmbeddingsPath = path, registryLoader = { error("registry must not be consulted") })

        val p = store.provenance
        assertNotNull(p, "a successful local load must record where the index came from")
        assertTrue(p!!.origin.contains(path.toString()), p.origin)
        assertEquals(generated, p.generatedAt)
        assertEquals(3, p.segments)
        assertNull(store.staleWarning(generated.plus(Duration.ofDays(10))))
        assertNotNull(store.staleWarning(generated.plus(Duration.ofDays(400))))
    }

    @Test
    fun aRegistryLoadRecordsTheRegistryOriginAndAnInjectedLoaderHasNoDate() {
        val missing = tempDir.resolve("none").resolve("embeddings.json")
        val store = RagStore(loadFromRegistry = true, localEmbeddingsPath = missing, registryLoader = { fixtureStore("R1", "R2") })

        val p = store.provenance
        assertNotNull(p)
        assertTrue(p!!.origin.contains("injected loader"), p.origin)
        assertNull(p.generatedAt, "an injected loader carries no Last-Modified")
        // Real remotes are named by kind so the log says which publish path served the index.
        assertEquals("GitHub release asset ${RagStore.GITHUB_RELEASE_URL}", RagStore.describeRemote(RagStore.GITHUB_RELEASE_URL))
        assertEquals(
            "GitLab registry package ${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}",
            RagStore.describeRemote("${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}")
        )
        assertEquals(2, p.segments)
        assertNull(store.staleWarning(now), "unknown age is never reported stale")
    }

    @Test
    fun aFailedLoadAndAFixtureStoreHaveNoProvenance() {
        val missing = tempDir.resolve("none").resolve("embeddings.json")
        assertNull(RagStore(loadFromRegistry = true, localEmbeddingsPath = missing, registryLoader = { null }).provenance)
        assertNull(RagStore(loadFromRegistry = false, initialStore = fixtureStore("F")).provenance)
    }

    private val segment = TextSegment.from(
        "chr deployment create writes deployments.<net>.chains into chromia.yml.",
        Metadata.from("file_name", "chr-deploy.md")
    )

    private fun answering(): RagStore = object : RagStore(loadFromRegistry = false) {
        override fun query(query: String): List<TextSegment>? = listOf(segment).also { rememberQueryHits(it) }
    }

    private suspend fun fetchDocs(store: RagStore) = FetchDocsStrategy(CompletableDeferred(store)).execute(
        callToolRequest(name = "fetch_docs", arguments = buildJsonObject { put("query", "deployments") }),
        ChromiaRepositoryImpl()
    )

    @Test
    fun fetchDocsAppendsTheStaleNoteOnlyWhenTheIndexIsPastTheLimit() = runBlocking {
        val stale = answering().also { it.provenance = RagStore.Provenance("GitLab registry package p", Instant.parse("2025-10-21T09:13:14Z"), 3084) }
        val result = fetchDocs(stale)
        assertFalse(result.isError == true)
        val text = (result.content.single() as TextContent).text!!
        assertTrue(text.contains("chr deployment create"), "the hits still come first: $text")
        assertTrue(text.contains("\n\nNOTE: documentation index is STALE: generated 2025-10-21"), text)
        val note = result.structuredContent!!.jsonObject["index_note"]?.jsonPrimitive?.content
        assertNotNull(note, "structured output carries the same note for clients that never read text")
        assertTrue(note!!.contains("GitLab tags"), note)

        val fresh = answering().also { it.provenance = RagStore.Provenance("local file x", Instant.now().minus(Duration.ofDays(1)), 3084) }
        val freshResult = fetchDocs(fresh)
        val freshText = (freshResult.content.single() as TextContent).text!!
        assertFalse(freshText.contains("NOTE: documentation index"), "a fresh index adds nothing: $freshText")
        assertNull(freshResult.structuredContent!!.jsonObject["index_note"])

        val unknown = answering() // fixture store, no provenance at all
        val unknownText = (fetchDocs(unknown).content.single() as TextContent).text!!
        assertFalse(unknownText.contains("NOTE: documentation index"), unknownText)
    }

    @Test
    fun fetchDocsAlwaysNamesTheIndexItAnsweredFromInStructuredOutput() = runBlocking {
        // The hosted server is a black box over the wire: /health has no store (it loads
        // lazily) and the text only speaks up when stale. A client verifying a deploy
        // needs the origin and age of the index on every answer, fresh or not.
        val generated = Instant.parse("2026-09-04T20:53:39Z")
        val store = answering().also {
            it.provenance = RagStore.Provenance("GitHub release asset ${RagStore.GITHUB_RELEASE_URL}", generated, 25823)
        }
        val index = fetchDocs(store).structuredContent!!.jsonObject["index"]!!.jsonObject
        assertEquals("GitHub release asset ${RagStore.GITHUB_RELEASE_URL}", index["origin"]?.jsonPrimitive?.content)
        assertEquals("2026-09-04T20:53:39Z", index["generated_at"]?.jsonPrimitive?.content)
        assertEquals(25823, index["segments"]?.jsonPrimitive?.content?.toInt())
        assertTrue((index["age_days"]?.jsonPrimitive?.content?.toLong() ?: -1) >= 0, index.toString())
        assertEquals("false", index["stale"]?.jsonPrimitive?.content)

        val text = (fetchDocs(store).content.single() as TextContent).text!!
        assertFalse(text.contains("GitHub release asset"), "text stays lean - provenance is structured only: $text")

        val unknownAge = answering().also { it.provenance = RagStore.Provenance("injected loader", null, 2) }
        val unknownIndex = fetchDocs(unknownAge).structuredContent!!.jsonObject["index"]!!.jsonObject
        assertTrue(unknownIndex["generated_at"] is JsonNull, unknownIndex.toString())
        assertTrue(unknownIndex["age_days"] is JsonNull, unknownIndex.toString())

        assertNull(fetchDocs(answering()).structuredContent!!.jsonObject["index"], "no provenance, no index object")
    }
}
