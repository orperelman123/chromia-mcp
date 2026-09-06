package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.chromia.tools.RagStore
import org.chromia.tools.docs.fetcher.IngestPathFilter
import org.chromia.tools.isDocsSegment
import org.chromia.tools.segmentTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

/**
 * Audit F15/F10-retrieval. Ten realistic Rell questions, graded on the refreshed
 * 25,823-segment index (2026-09-06): 3 correct / 5 partly / 2 wrong, and TWO of
 * the answers came out of the Rell compiler's own Kotlin unit tests
 * (`ReplDefinitionTest.kt`, `LibRellTestBlockClockTest.kt`) because the corpus
 * indexes `*.kt` test sources and dense retrieval has no idea an agent asking
 * "how do I declare module_args?" wants the page, not the test.
 *
 * Two fixes, pinned here:
 *  - the corpus drops host-language TEST sources ([IngestPathFilter]);
 *  - retrieval ranks a hit by the kind of file it came from ([segmentTier]): it
 *    breaks the score tie among exact-name hits, and re-orders the semantic tail
 *    docs-first. A DEFINITION site still comes first - that is round 10.
 *
 * The ten questions themselves are re-asked out of band against the real index
 * by `scripts/rag-audit-ten.mjs`, whose result is recorded in
 * `rag-audit-ten.json` - the store is 150 MB and the test JVM has 1,280 MB, so
 * the graded outcome lives here as data rather than as a load the suite cannot
 * carry.
 */
class DocsBeforeSourceRetrievalTest {

    @Serializable
    private data class GradedHit(val question: String, val title: String? = null, val url: String? = null, val id: String? = null)

    private val docsExtensions = setOf("md", "mdx", "rst", "adoc")
    private val sourceExtensions = setOf("kt", "kts", "java", "ts", "js", "py")

    // ---- corpus ------------------------------------------------------------

    @Test
    fun kotlinTestSourcesAreNotIndexed() {
        assertFalse(IngestPathFilter.accept(Path("rell-base/src/test/kotlin/net/postchain/rell/ReplDefinitionTest.kt")))
        assertFalse(IngestPathFilter.accept(Path("rell-base/src/test/kotlin/LibRellTestBlockClockTest.kt")))
        assertFalse(IngestPathFilter.accept(Path("postchain-base/src/test/kotlin/Anything.kt")))
        assertFalse(IngestPathFilter.accept(Path("client/src/testFixtures/kotlin/Fixtures.kt")))
        assertFalse(IngestPathFilter.accept(Path("client/tests/transaction.spec.ts")))
        assertFalse(IngestPathFilter.accept(Path("client/src/AccountTests.ts")))
    }

    @Test
    fun implementationSourcesAndAllDocsStayIndexed() {
        // rt_primitive_types.kt documents big_integer BY BEING the implementation.
        assertTrue(IngestPathFilter.accept(Path("rell-base/src/main/kotlin/rt_primitive_types.kt")))
        assertTrue(IngestPathFilter.accept(Path("ft4-lib/rell/src/lib/ft4/accounts/module.rell")))
        // A docs page about testing is a docs page, wherever it lives.
        assertTrue(IngestPathFilter.accept(Path("doc/tests/index.mdx")))
        assertTrue(IngestPathFilter.accept(Path("doc/testing.md")))
        assertTrue(IngestPathFilter.accept(Path("src/test/resources/notes.md")))
        assertTrue(IngestPathFilter.accept(Path("doc/releases/0.10.4.txt")))
    }

    // ---- ranking -----------------------------------------------------------

    private fun segment(fileName: String, text: String = "module_args is declared as a struct and read with chain_context.args") =
        TextSegment.from(text, Metadata.from("file_name", fileName))

    @Test
    fun documentationOutranksEveryKindOfSource() {
        assertEquals(0, segmentTier(segment("chain_context.md")))
        assertEquals(0, segmentTier(segment("auth-descriptors.mdx")))
        assertEquals(0, segmentTier(segment("modules.rst")))
        assertEquals(1, segmentTier(segment("0.10.4.txt")))
        assertEquals(2, segmentTier(segment("module.rell")))
        assertEquals(3, segmentTier(segment("chromia.yml")))
        assertEquals(4, segmentTier(segment("rt_primitive_types.kt")))
        assertEquals(4, segmentTier(segment("transaction-builder.ts")))
        assertTrue(isDocsSegment(segment("chain_context.md")))
        assertFalse(isDocsSegment(segment("rt_primitive_types.kt")))
    }

    @Test
    fun mergedHitsPutDocsFirstInTheSemanticTail() {
        val store = RagStore(loadFromRegistry = false)
        val kt = segment("rt_primitive_types.kt")
        val ts = segment("transaction-builder.ts")
        val md = segment("simple-types.md")
        val rell = segment("module.rell")

        // Semantic-only: the page wins even though the source came back first.
        assertEquals(
            listOf("simple-types.md", "module.rell", "rt_primitive_types.kt"),
            store.mergeHits(emptyList(), listOf(kt, rell, md)).map { it.metadata().getString("file_name") }
        )
        // Exact-name (lexical) hits still come first in their own order - round
        // 10 - and the docs preference applies to the semantic tail behind them.
        assertEquals(
            listOf("transaction-builder.ts", "module.rell", "simple-types.md"),
            store.mergeHits(listOf(ts, rell), listOf(md)).map { it.metadata().getString("file_name") }
        )
    }

    /**
     * `val BIG_INTEGER: Rt_ValueClass<*>` in the compiler's rt_primitive_types.kt
     * matched the case-insensitive definition regex for `big_integer` and beat
     * the page that explains Rell big_integer arithmetic (audit F15, question 5).
     * A host-language source cannot claim the definition boost.
     */
    @Test
    fun aCasingCoincidenceInKotlinIsNotADefinition() {
        val kotlinConstant = TextSegment.from(
            "    val BIG_INTEGER: Rt_ValueClass<*> = Rt_BigIntegerValue",
            Metadata.from("file_name", "rt_primitive_types.kt")
        )
        val page = TextSegment.from(
            "Rell big_integer is an arbitrary-precision integer; division truncates toward zero.",
            Metadata.from("file_name", "simple-types.md")
        )
        val fixture = dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<TextSegment>().also {
            it.add(dev.langchain4j.data.embedding.Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), kotlinConstant)
            it.add(dev.langchain4j.data.embedding.Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), page)
        }
        val store = RagStore(loadFromRegistry = false, initialStore = fixture)
        assertEquals(
            "simple-types.md",
            store.lexicalHits("big_integer arithmetic").first().metadata().getString("file_name")
        )
        // A Rell definition still wins - that is round 10, and it is not a host source.
        val rellDefinition = TextSegment.from(
            "function require_mandatory_flags(auth_descriptor) { val flags = get_flags(auth_descriptor); }",
            Metadata.from("file_name", "module.rell")
        )
        val prose = TextSegment.from(
            "Auth descriptors carry flags; require_mandatory_flags is mentioned here and here: require_mandatory_flags.",
            Metadata.from("file_name", "auth-descriptors.md")
        )
        val fixture2 = dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore<TextSegment>().also {
            it.add(dev.langchain4j.data.embedding.Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), prose)
            it.add(dev.langchain4j.data.embedding.Embedding.from(floatArrayOf(0.1f, 0.2f, 0.3f)), rellDefinition)
        }
        assertEquals(
            "module.rell",
            RagStore(loadFromRegistry = false, initialStore = fixture2)
                .lexicalHits("require_mandatory_flags").first().metadata().getString("file_name")
        )
    }

    // ---- the audit's ten questions -----------------------------------------

    @Test
    fun theTenAuditQuestionsNoLongerAnswerFromASourceFile() {
        val text = checkNotNull(javaClass.classLoader.getResourceAsStream("rag-audit-ten.json")) {
            "rag-audit-ten.json is missing: regenerate it with scripts/rag-audit-ten.mjs"
        }.bufferedReader().use { it.readText() }
        val graded = Json { ignoreUnknownKeys = true }.decodeFromString<List<GradedHit>>(text)

        assertEquals(10, graded.size, "the audit asked ten questions")
        val fromSource = graded.filter { it.title.orEmpty().substringAfterLast('.').lowercase() in sourceExtensions }
        assertTrue(
            fromSource.isEmpty(),
            "these questions still answer out of a source file: " +
                fromSource.joinToString { "${it.title} <- ${it.question}" }
        )
        val fromDocs = graded.count { it.title.orEmpty().substringAfterLast('.').lowercase() in docsExtensions }
        assertTrue(
            fromDocs >= 9,
            "only $fromDocs of 10 top hits are documentation pages (audit baseline: 4): " +
                graded.joinToString { it.title.orEmpty() }
        )
    }
}
