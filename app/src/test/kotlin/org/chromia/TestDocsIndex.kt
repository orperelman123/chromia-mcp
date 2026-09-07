package org.chromia

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.spi.model.embedding.EmbeddingModelFactory
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import org.chromia.tools.RagStore
import org.chromia.tools.persistLocalEmbeddings
import java.nio.file.Path
import java.util.ServiceLoader

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
 * The last substitute in this file is gone too. Until 2026-09-07 the embedding
 * model here was `BagOfWordsEmbeddingModel`, a hand-written L2-normalized hashed
 * bag-of-words embedder over 4096 dimensions, written so that "cosine == word
 * overlap" and the fixture assertions could be read off the segment text. It was
 * a double of a third party, and it made the suite's retrieval behave in a way
 * production's retrieval never does. [model] below is now the model the server
 * actually ships.
 *
 * What is left un-owned is the index CONTENT: in production it is a ~150 MB
 * embeddings.json downloaded from the GitHub release asset, here it is a handful
 * of [TextSegment]s. That is fixture DATA, not a substitute implementation.
 *
 * ## What the real model changed
 *
 * BGE-small-en-v1.5 is a general-purpose sentence encoder: two unrelated English
 * sentences still sit at a cosine of roughly 0.6-0.8. langchain4j maps cosine
 * `c` to a relevance score of `(c + 1) / 2` and [RagStore] retrieves at
 * `minScore = 0.6`, i.e. cosine >= 0.2 - so in a fixture index of two or three
 * segments EVERY segment is retrieved for practically every query, and the
 * meaningful thing a test can assert is the ORDER and identity of the hits, not
 * how many came back. Tests that used to read "one hit, and it is the right one"
 * now read "the right one leads". That is what production does too; the old
 * counts were an artifact of the toy embedder's orthogonal vectors.
 */
object TestDocsIndex {

    /**
     * The production embedder, resolved exactly the way [RagStore] resolves it
     * when no model is injected: langchain4j's `EmbeddingModelFactory` SPI, which
     * the `langchain4j-easy-rag` dependency answers with the quantized BGE-small
     * ONNX model bundled in the jar (384 dimensions).
     *
     * Loading it costs ~3 s and ~34 MB of ONNX weights, so ONE instance is shared
     * by every fixture store in the test JVM: `by lazy` here means the suite pays
     * that cost once in total, not once per [RagStore]. Handing the same instance
     * to the RagStore constructor is not an injected substitute - it is the very
     * object the SPI fallback would have built, built once.
     */
    val model: EmbeddingModel by lazy {
        ServiceLoader.load(EmbeddingModelFactory::class.java).firstOrNull()?.create()
            ?: error(
                "no EmbeddingModelFactory on the test classpath - the tests embed with the model the " +
                    "server ships (dev.langchain4j:langchain4j-easy-rag), and there is no substitute for it"
            )
    }

    /** The real model's width (384 for BGE-small); no longer a number this file chooses. */
    val DIMENSION: Int by lazy { model.dimension() }

    /** An in-memory index holding [segments], embedded with the real [model]. */
    fun index(segments: List<TextSegment>): InMemoryEmbeddingStore<TextSegment> =
        InMemoryEmbeddingStore<TextSegment>().also { store ->
            if (segments.isNotEmpty()) {
                store.addAll(model.embedAll(segments).content(), segments)
            }
        }

    /** A real [RagStore] over [segments]; no download, no override, the real embedder. */
    fun store(vararg segments: TextSegment): RagStore = RagStore(
        loadFromRegistry = false,
        initialStore = index(segments.toList()),
        embeddingModel = model
    )

    /** Writes an index file the production loader can read back. */
    fun persist(path: Path, vararg segments: TextSegment): Path {
        persistLocalEmbeddings(index(segments.toList()), path)
        return path
    }

    /**
     * A real [RagStore] that LOADS [path] through production's own loader.
     *
     * This used to pass `registryLoader = { null }` - a lambda double standing in
     * for the published index - to mean "do not go to the registry". It does not
     * need one: `loadFreshestStore()` tries the local file FIRST and returns as
     * soon as it parses, and [path] was written moments ago by [persist], so it is
     * inside the staleness window and the remote step is never reached. The
     * registry is not stubbed out, it is simply not needed - which is exactly what
     * production does when a fresh local index is present.
     */
    fun storeLoadedFrom(path: Path): RagStore = RagStore(
        loadFromRegistry = true,
        localEmbeddingsPath = path,
        // No remote is CONFIGURED, which is a real deployment (an air-gapped
        // install shipping its own embeddings.json) - and the reason it matters
        // here is that the default list is the real GitHub release asset: a
        // fixture file that ever failed to parse would otherwise pull ~150 MB
        // over the network on somebody's build.
        remoteUrls = emptyList(),
        cacheEmbeddingsPath = null,
        embeddingModel = model
    )
}
