package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import org.chromia.tools.RagStore
import org.chromia.tools.segmentId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
 *
 * 2026-09-07: this file used to build its fixture out of hand-picked vectors
 * (`near` = every query, `far` = the code segments) plus an `object :
 * EmbeddingModel` that answered every text with `near`. Cosine 1 against cosine
 * -1 made the semantic half of the merge a switch the test flipped itself, so
 * "the lexical hits jump the queue" was asserted against a queue nobody else
 * would ever see. The fixture is now embedded with the model the server ships
 * ([TestDocsIndex.model]) and the store is a plain [RagStore] over it: the
 * semantic tail is whatever BGE-small really thinks, and the lexical boost has
 * to beat it for real.
 */
class RagStoreLexicalBoostTest {

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

    private fun store(): RagStore = TestDocsIndex.store(prose, definition, mention, unrelated)

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
    fun cliFlagsAreIdentifiersToo() {
        // 2026-09-05: "How do I run only some Rell tests with chr test --tests?" ranked repl.md
        // first although the docs page literally says `chr test --tests my_filter`. A long
        // flag is an exact name an agent copies from a terminal, same as a snake_case one.
        assertEquals(listOf("--tests"), RagStore.identifierTokens("How do I run only some Rell tests with chr test --tests?"))
        assertEquals(listOf("--hide-lib-warnings", "--settings"), RagStore.identifierTokens("chr build --hide-lib-warnings --settings chromia-it.yml"))
        assertEquals(listOf("--wipe"), RagStore.identifierTokens("start the node with --wipe, then -t and -- alone mean nothing"))
        // Two dashes inside prose (an em-dash stand-in) and short flags are not names.
        assertEquals(emptyList<String>(), RagStore.identifierTokens("deploy -- then test -v -t quickly"))
        // The flag stops at `=`; the value is not part of the name.
        assertEquals(listOf("--tests"), RagStore.identifierTokens("--tests=abc test method pattern"))
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
        // WEAKENED 2026-09-07 with the toy embedder's removal, and honest about why.
        // With hand-picked orthogonal vectors the code segments scored below
        // minScore and this test could say "the definition is not in the results at
        // all". The real model puts every short English sentence within retrieval
        // range of every other one (cosine ~0.6-0.8 -> score ~0.8-0.9, well over
        // RagStore's 0.6), so a four-segment index returns four hits for anything -
        // in production the store has 25k segments and the cap does the work. What
        // this test actually names is that a query WITHOUT an identifier gets no
        // LEXICAL pull, so that is what it asserts: no lexical hits, and the merge
        // is the plain docs-first semantic order, with the .rell definition behind
        // the two .md pages rather than jumped to the front.
        val store = store()
        val query = "FT4 authentication"
        assertEquals(emptyList<String>(), RagStore.identifierTokens(query))
        assertTrue(store.lexicalHits(query).isEmpty(), "no name in the query means no lexical block")

        val hits = store.query(query)
        assertNotNull(hits)
        val ids = hits!!.map { segmentId(it) }
        assertNotEquals(segmentId(definition), ids[0], "nothing pulls the definition to the front: $ids")
        assertEquals(
            setOf(segmentId(prose), segmentId(unrelated)),
            setOf(ids[0], ids[1]),
            "docs pages lead the semantic tail (segmentTier), the .rell segments follow: $ids"
        )
        assertEquals(ids.size, ids.toSet().size, "no duplicates")
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
        val store = TestDocsIndex.store(definition, upper, unrelated)
        val ids = store.lexicalHits("require_mandatory_flags").map { segmentId(it) }
        assertEquals(listOf(segmentId(definition), segmentId(upper)), ids)
    }

    @Test
    fun lexicalHitsAreCappedPerTokenAndTheMergeIsCappedAndDeduplicated() {
        val many = (1..10).map { i -> TextSegment.from("mention $i of require_mandatory_flags in passing", Metadata.from("file_name", "m$i.md")) }
        val store = TestDocsIndex.store(definition, *many.toTypedArray())
        val lexical = store.lexicalHits("require_mandatory_flags")
        assertEquals(RagStore.LEXICAL_HITS_PER_TOKEN, lexical.size)
        assertEquals(segmentId(definition), segmentId(lexical.first()))

        val merged = store.mergeHits(lexical, lexical + many)
        assertEquals(merged.map { segmentId(it) }.toSet().size, merged.size, "deduplicated")
        assertTrue(merged.size <= RagStore.MAX_HITS)
        assertEquals(segmentId(definition), segmentId(merged.first()))
    }
}
