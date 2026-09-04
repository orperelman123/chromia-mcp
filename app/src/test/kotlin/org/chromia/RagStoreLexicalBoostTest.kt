package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.RagStore
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Round 10 (2026-09-04): `require_mandatory_flags` was in the store (three
 * segments of ft4-lib's accounts/module.rell) and `fetch_docs` missed it under
 * every phrasing, the bare identifier included - dense retrieval ranks prose
 * about auth descriptors above the code that defines the name. Names are what
 * agents ask about, so the query is hybrid: exact-identifier hits from the
 * in-memory segment index come first, then the semantic hits.
 */
class RagStoreLexicalBoostTest {

    private val near = floatArrayOf(0.1f, 0.2f, 0.3f)
    private val far = floatArrayOf(-0.1f, -0.2f, -0.3f)

    private val prose = TextSegment.from(
        "Auth descriptors carry flags such as A (account) and T (transfer). Rules restrict how they may be used.",
        Metadata.from("file_name", "auth-descriptors.md")
    )
    private val definition = TextSegment.from(
        "/** Throws MISSING MANDATORY FLAGS if some are missing. */\nfunction require_mandatory_flags(auth_descriptor) {\n    val flags = get_flags(auth_descriptor);\n}",
        Metadata.from("file_name", "module.rell")
    )
    private val mention = TextSegment.from(
        "require(auth_descriptor.rules == GTV_NULL, \"RESTRICTED MAIN AUTH\");\nrequire_mandatory_flags(auth_descriptor);\ndelete_main_auth_descriptor(account);",
        Metadata.from("file_name", "module.rell")
    )
    private val unrelated = TextSegment.from(
        "ICMF topics are strings; a receiver module handles messages per topic.",
        Metadata.from("file_name", "icmf.md")
    )

    /** Every query embeds to [near]: `prose` and `unrelated` are similar (cosine 1), the code segments are not (cosine -1). */
    private fun fixedModel(): EmbeddingModel = object : EmbeddingModel {
        override fun embedAll(segments: List<TextSegment>): Response<List<Embedding>> =
            Response.from(segments.map { Embedding.from(near) })
    }

    private fun store(): RagStore {
        val fixture = InMemoryEmbeddingStore<TextSegment>().also {
            it.add(Embedding.from(near), prose)
            it.add(Embedding.from(far), definition)
            it.add(Embedding.from(far), mention)
            it.add(Embedding.from(near), unrelated)
        }
        return RagStore(loadFromRegistry = false, initialStore = fixture, embeddingModel = fixedModel())
    }

    @Test
    fun identifierTokensAreNamesNotWordsAcronymsOrFileNames() {
        assertEquals(listOf("require_mandatory_flags"), RagStore.identifierTokens("Where should require_mandatory_flags be set in an FT4 auth descriptor?"))
        assertEquals(listOf("auth.authenticate"), RagStore.identifierTokens("How do I authenticate an operation with ft4 auth.authenticate?"))
        assertEquals(listOf("module_args"), RagStore.identifierTokens("How do I add a module_args struct and pass values in chromia.yml?"))
        assertEquals(listOf("run_must_fail"), RagStore.identifierTokens("How do I write a Rell unit test with @test module and run_must_fail?"))
        assertEquals(listOf("merkle_hash_version"), RagStore.identifierTokens("What merkle_hash_version should a new blockchain config use?"))
        assertEquals(listOf("getAssetBalance"), RagStore.identifierTokens("what does getAssetBalance return"))
        assertEquals(emptyList<String>(), RagStore.identifierTokens("FT4 authentication"))
        assertEquals(emptyList<String>(), RagStore.identifierTokens("How do I query a dapp with the postchain client from TypeScript?"))
        assertEquals(emptyList<String>(), RagStore.identifierTokens("edit main.rell and docs.chromia.com pages"))
        assertEquals(listOf("rell.get_app_structure"), RagStore.identifierTokens("call rell.get_app_structure first"))
        // Repeated names count once; a trailing sentence dot is not part of the name.
        assertEquals(listOf("op_context.is_signer"), RagStore.identifierTokens("use op_context.is_signer. Then op_context.is_signer again."))
    }

    @Test
    fun theDefinitionOutranksAMentionAndMentionsOutrankSilence() {
        assertTrue(RagStore.lexicalScore(definition.text(), "require_mandatory_flags") > RagStore.lexicalScore(mention.text(), "require_mandatory_flags"))
        assertTrue(RagStore.lexicalScore(mention.text(), "require_mandatory_flags") > 0)
        assertEquals(0, RagStore.lexicalScore(prose.text(), "require_mandatory_flags"))
        assertTrue(RagStore.lexicalScore("x\ny require_mandatory_flags z require_mandatory_flags", "require_mandatory_flags") > RagStore.lexicalScore("require_mandatory_flags once", "require_mandatory_flags"))
    }

    @Test
    fun aQueryNamingAnIdentifierGetsItsDefinitionFirstThenTheSemanticHits() {
        val hits = store().query("Where should require_mandatory_flags be set in an FT4 auth descriptor?")
        assertNotNull(hits)
        val ids = hits!!.map { segmentId(it) }
        assertEquals(segmentId(definition), ids[0], "the segment that DEFINES the name leads: $ids")
        assertEquals(segmentId(mention), ids[1], "then the segment that mentions it")
        assertTrue(segmentId(prose) in ids && segmentId(unrelated) in ids, "semantic hits still follow: $ids")
        assertEquals(ids.size, ids.toSet().size, "no duplicates")
    }

    @Test
    fun aQueryWithoutAnIdentifierIsPurelySemantic() {
        val hits = store().query("FT4 authentication")
        assertNotNull(hits)
        val ids = hits!!.map { segmentId(it) }
        assertFalse(segmentId(definition) in ids, "no lexical pull without a name in the query: $ids")
        assertFalse(segmentId(mention) in ids)
        assertTrue(segmentId(prose) in ids)
    }

    /**
     * A pasted stack trace is a query too. Measured 2026-09-04 on the real
     * 25823-segment store: one identifier +300 ms, 40 identifiers 4.1 s - each
     * token scanned every segment with contains(ignoreCase) and compiled two
     * regexes per matching segment. The token list is capped so the scan is
     * bounded, first-mentioned names win (the agent leads with what it means).
     */
    @Test
    fun identifierTokensAreCappedAtTheFirstFew() {
        val flood = (1..30).joinToString(" ") { "name_$it" }
        val tokens = RagStore.identifierTokens(flood)
        assertEquals(RagStore.MAX_IDENTIFIER_TOKENS, tokens.size)
        assertEquals((1..RagStore.MAX_IDENTIFIER_TOKENS).map { "name_$it" }, tokens)
    }

    @Test
    fun lexicalMatchingIsCaseInsensitiveAndTheDefinitionStillLeads() {
        val upper = TextSegment.from("See REQUIRE_MANDATORY_FLAGS in the accounts module.", Metadata.from("file_name", "notes.md"))
        val fixture = InMemoryEmbeddingStore<TextSegment>().also { s ->
            s.add(Embedding.from(far), definition)
            s.add(Embedding.from(far), upper)
            s.add(Embedding.from(far), unrelated)
        }
        val store = RagStore(loadFromRegistry = false, initialStore = fixture, embeddingModel = fixedModel())
        val ids = store.lexicalHits("require_mandatory_flags").map { segmentId(it) }
        assertEquals(listOf(segmentId(definition), segmentId(upper)), ids)
    }

    @Test
    fun lexicalHitsAreCappedPerTokenAndTheMergeIsCappedAndDeduplicated() {
        val many = (1..10).map { i -> TextSegment.from("mention $i of require_mandatory_flags in passing", Metadata.from("file_name", "m$i.md")) }
        val fixture = InMemoryEmbeddingStore<TextSegment>().also { s ->
            s.add(Embedding.from(far), definition)
            many.forEach { s.add(Embedding.from(far), it) }
        }
        val store = RagStore(loadFromRegistry = false, initialStore = fixture, embeddingModel = fixedModel())
        val lexical = store.lexicalHits("require_mandatory_flags")
        assertEquals(RagStore.LEXICAL_HITS_PER_TOKEN, lexical.size)
        assertEquals(segmentId(definition), segmentId(lexical.first()))

        val merged = store.mergeHits(lexical, lexical + many)
        assertEquals(merged.map { segmentId(it) }.toSet().size, merged.size, "deduplicated")
        assertTrue(merged.size <= RagStore.MAX_HITS)
        assertEquals(segmentId(definition), segmentId(merged.first()))
    }
}
