package org.chromia

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.RagStore
import org.chromia.tools.persistLocalEmbeddings
import java.nio.file.Path
import kotlin.math.sqrt

/**
 * A REAL documentation index for tests.
 *
 * Every docs-tool test used to answer from `object : RagStore(...) { override
 * fun query(...) = ... }` - a double of OUR OWN retrieval. That made
 * `search`/`fetch`/`fetch_docs` green without ever running [RagStore.query],
 * which is where the two audited behaviours actually live: the exact-identifier
 * lexical boost (round 10) and the docs-first merge (audit F15). A rule that is
 * never executed by the tests that name it is a fake green, so those overrides
 * are gone and the stores below are ordinary [RagStore] instances: real
 * `query()`, real `lexicalHits`/`mergeHits`, real `fetchById`, real segment id
 * index.
 *
 * Two things a unit test cannot own are supplied instead of faked around:
 *
 *  - **the index content.** In production it is a 150 MB embeddings.json
 *    downloaded from the GitHub release asset. Here it is a handful of
 *    [TextSegment]s. That is fixture DATA, not a substitute implementation.
 *  - **the embedding model.** Production embeds with easy-rag's bundled
 *    quantized BGE-small ONNX model (~2.8 s to load, non-deterministic across
 *    versions). [BagOfWordsEmbeddingModel] below is a real - if deliberately
 *    simple - embedder: L2-normalized hashed bag-of-words, so cosine similarity
 *    is word overlap and a query only retrieves a segment it shares vocabulary
 *    with. It is listed in the mock ledger with its live counterparts
 *    (`scripts/rag-eval.mjs --production-shaped` in CI, and the sweep's
 *    `search (ChatGPT)` / `fetch (ChatGPT)` / `fetch_docs live+search`), which
 *    are the checks that exercise the real ONNX model against the real index.
 *
 * Scoring note, so the fixtures are readable: langchain4j maps cosine `c` to a
 * relevance score of `(c + 1) / 2`, and [RagStore] retrieves at `minScore =
 * 0.6`. With non-negative bag-of-words vectors that means "shares at least one
 * word" retrieves and "shares none" (cosine 0 -> score 0.5) does not.
 */
object TestDocsIndex {

    /** Wide enough that two different fixture words never collide in practice. */
    const val DIMENSION = 4096

    private val WORD = Regex("[A-Za-z0-9_]+")

    /**
     * L2-normalized hashed bag-of-words. Deterministic (String.hashCode is
     * specified), dimension-stable, and produces genuine cosine similarity -
     * this is a small embedding model, not a stub that returns canned hits.
     */
    object BagOfWordsEmbeddingModel : EmbeddingModel {
        override fun embedAll(segments: List<TextSegment>): Response<List<Embedding>> =
            Response.from(segments.map { Embedding.from(vector(it.text())) })

        fun vector(text: String): FloatArray {
            val v = FloatArray(DIMENSION)
            WORD.findAll(text.lowercase())
                .map { it.value }
                .distinct()
                .forEach { token -> v[Math.floorMod(token.hashCode(), DIMENSION)] = 1f }
            var sum = 0.0
            for (x in v) sum += (x * x).toDouble()
            val norm = sqrt(sum).toFloat()
            if (norm > 0f) for (i in v.indices) v[i] = v[i] / norm
            return v
        }
    }

    /** An in-memory index holding [segments], embedded with the model above. */
    fun index(segments: List<TextSegment>): InMemoryEmbeddingStore<TextSegment> =
        InMemoryEmbeddingStore<TextSegment>().also { store ->
            segments.forEach { segment ->
                store.add(Embedding.from(BagOfWordsEmbeddingModel.vector(segment.text())), segment)
            }
        }

    /** A real [RagStore] over [segments]; no download, no override. */
    fun store(vararg segments: TextSegment): RagStore = RagStore(
        loadFromRegistry = false,
        initialStore = index(segments.toList()),
        embeddingModel = BagOfWordsEmbeddingModel
    )

    /** Writes an index file the production loader can read back. */
    fun persist(path: Path, vararg segments: TextSegment): Path {
        persistLocalEmbeddings(index(segments.toList()), path)
        return path
    }

    /**
     * A real [RagStore] that LOADS [path] the way production loads the local
     * index file (registry disabled), then answers with the real query path.
     */
    fun storeLoadedFrom(path: Path): RagStore = RagStore(
        loadFromRegistry = true,
        localEmbeddingsPath = path,
        registryLoader = { null },
        embeddingModel = BagOfWordsEmbeddingModel
    )
}
