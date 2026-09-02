package org.chromia.tools

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.splitter.DocumentSplitters
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.rag.query.Query
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.chromia.App.Companion.logger
import org.chromia.downloadFile
import org.chromia.tools.docs.fetcher.DocsFetcher
import org.chromia.tools.docs.fetcher.IngestPathFilter
import org.chromia.uploadFile
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isRegularFile

open class RagStore(
    loadFromRegistry: Boolean = true,
    initialStore: InMemoryEmbeddingStore<TextSegment>? = null,
    val localEmbeddingsPath: Path = resolveLocalEmbeddingsPath(),
    registryLoader: (() -> InMemoryEmbeddingStore<TextSegment>?)? = null,
    val embeddingModel: EmbeddingModel? = null
) {
    val ktorClient by lazy { HttpClient() }

    companion object {
        const val FILE_NAME = "embeddings.json"
        const val PACKAGE_URL = "https://gitlab.com/api/v4/projects/71940508/packages/generic/embeddings/v1"
        const val EMBEDDINGS_PATH_ENV = "CHROMIA_EMBEDDINGS_PATH"
        const val REGISTRY_REQUEST_TIMEOUT_MS = 10_000L
        const val REGISTRY_CONNECT_TIMEOUT_MS = 5_000L
        const val LOAD_RETRY_COOLDOWN_MS = 60_000L

        fun downloadFromRegistry(
            client: HttpClient? = null
        ): InMemoryEmbeddingStore<TextSegment>? {
            val owned = client == null
            val http = client ?: createRegistryDownloadClient()
            return try {
                runCatching {
                    runBlocking {
                        logger.info("Download embedding from Gitlab registry")
                        http.downloadFile("$PACKAGE_URL/$FILE_NAME")?.let { tempFile ->
                            val loaded = runCatching {
                                InMemoryEmbeddingStore.fromFile(tempFile)
                            }.onFailure { error ->
                                logger.warn("Failed to parse embeddings from registry: ${error.message}")
                            }.getOrNull()
                            tempFile.deleteIfExists()
                            loaded
                        }
                    }
                }.onFailure { error ->
                    logger.warn("GitLab registry embeddings download skipped: ${error.message}")
                }.getOrNull()
            } finally {
                if (owned) {
                    http.close()
                }
            }
        }
    }

    private val gitLabAccessToken = System.getenv("GITLAB_ACCESS_TOKEN")
    val docsFetcher by lazy { DocsFetcher() }
    private val segmentsById = ConcurrentHashMap<String, TextSegment>()

    var embeddingStore: InMemoryEmbeddingStore<TextSegment>? = null
        set(value) {
            field = value
            rebuildSegmentIndex(value)
        }

    // A transient local/registry load failure at startup used to leave
    // embeddingStore null until redeploy - every search/fetch_docs answered
    // "not found" forever (audit F5). Keep the load recipe and retry it on use,
    // at most once per cooldown window.
    private val storeLoader: (() -> InMemoryEmbeddingStore<TextSegment>?)? =
        if (loadFromRegistry && initialStore == null) {
            { loadLocalEmbeddings(localEmbeddingsPath) ?: (registryLoader ?: { downloadFromRegistry() })() }
        } else {
            null
        }
    internal var loadRetryCooldownMs: Long = LOAD_RETRY_COOLDOWN_MS
    internal var clock: () -> Long = { System.currentTimeMillis() }
    private var nextLoadRetryAtMs = 0L

    private fun tryLoadStore(): InMemoryEmbeddingStore<TextSegment>? {
        val load = storeLoader ?: return null
        nextLoadRetryAtMs = clock() + loadRetryCooldownMs
        return runCatching(load).onFailure { error ->
            logger.warn("GitLab registry embeddings load skipped: ${error.message}")
        }.getOrNull()
    }

    /**
     * The loaded store, or a cooldown-limited reload attempt when the initial
     * load failed. Null while the index stays unavailable.
     */
    @Synchronized
    private fun storeOrRetry(): InMemoryEmbeddingStore<TextSegment>? {
        embeddingStore?.let { return it }
        if (storeLoader == null || clock() < nextLoadRetryAtMs) return null
        val loaded = tryLoadStore()
        if (loaded != null) {
            logger.info("Embeddings index loaded on retry")
            embeddingStore = loaded
        }
        return loaded
    }

    init {
        embeddingStore = initialStore ?: tryLoadStore()
    }

    /**
     * Constructor-injected model wins (tests); otherwise the same SPI factory
     * the [EmbeddingStoreIngestor] uses at ingest time (easy-rag BGE-small
     * quantized, bundled in the jar) so runtime queries embed with the model
     * that produced the store. Without this fallback a default-constructed
     * RagStore answered every search with "not found".
     */
    /** Test seam for the SPI fallback below; production uses the ServiceLoader. */
    internal var embeddingModelSpiLoader: () -> EmbeddingModel? = {
        runCatching {
            java.util.ServiceLoader.load(
                dev.langchain4j.spi.model.embedding.EmbeddingModelFactory::class.java
            ).firstOrNull()?.create()
        }.onFailure { error ->
            logger.warn("SPI embedding model load failed: ${error.message}")
        }.getOrNull()
    }

    private val resolvedEmbeddingModel: EmbeddingModel? by lazy {
        embeddingModel ?: embeddingModelSpiLoader()
    }

    open fun query(query: String): List<TextSegment>? {
        val store = storeOrRetry() ?: return null
        val model = resolvedEmbeddingModel
        if (model == null) {
            // Null = "index unavailable", same as a missing store - an empty
            // success here showed "no results" for every search (audit round 4 F6).
            logger.warn("Embedding search for '$query' is unavailable; no embedding model could be loaded")
            return null
        }
        val retriever: ContentRetriever = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(store)
            .embeddingModel(model)
            .maxResults(15) // topK
            .minScore(0.6) // similarity score
            .build()
        return runCatching {
            retriever.retrieve(Query.from(query))?.mapNotNull { it.textSegment() }?.also { rememberQueryHits(it) }
        }.getOrElse { error ->
            // A throwing retriever (e.g. embedding-dimension mismatch against a
            // foreign store) used to degrade to emptyList(), so a BROKEN index
            // answered exactly like "no such docs" (reality audit D6). The
            // index-missing path (null) already reports honestly - mirror that
            // honesty here by surfacing a retrieval ERROR; the search/fetch_docs
            // strategies' error paths render it with isError=true.
            logger.warn("Embedding search failed for '$query': ${error.message}")
            throw IllegalStateException(
                "documentation retrieval failed (this is NOT a no-match answer): ${error.message}. " +
                    "The embeddings index may be corrupt or built with a different embedding model " +
                    "(dimension mismatch) - rebuild/redeploy the embeddings; repeating the same " +
                    "query will not help until the index is fixed.",
                error
            )
        }
    }

    internal fun rememberQueryHits(segments: List<TextSegment>) {
        segments.forEach { segment ->
            segmentsById[segmentId(segment)] = segment
        }
    }

    open fun fetchById(id: String): TextSegment? {
        storeOrRetry() // repopulates the id index if the initial load failed
        return segmentsById[normalizeSegmentId(id)]
    }

    /**
     * True when embeddings should have loaded (a loader is configured) but the
     * index is still missing. `fetch` must then answer "index unavailable" like
     * search/fetch_docs do - a miss here is not proof the id is unknown (audit
     * round 4 F3, residual of the round 1 F5 fix). Call after [fetchById] so a
     * successful cooldown retry is reflected. Fixture stores constructed with
     * loadFromRegistry=false have no loader and count as available.
     */
    open fun isIndexUnavailable(): Boolean = embeddingStore == null && storeLoader != null

    private fun rebuildSegmentIndex(store: InMemoryEmbeddingStore<TextSegment>?) {
        segmentsById.clear()
        if (store == null) return
        embeddingStoreSegments(store).forEach { segment ->
            segmentsById[segmentId(segment)] = segment
        }
    }

    fun createAndUploadEmbeddings(upload: Boolean = true): InMemoryEmbeddingStore<TextSegment> = runBlocking {
        logger.info("Creating embeddings...")
        val fetchedReposPath = docsFetcher.fetchRepositories()
        val documents = FileSystemDocumentLoader.loadDocumentsRecursively(
            fetchedReposPath,
            IngestPathFilter.pathMatcher()
        )
        logger.info("documents loaded successfully --> ${documents.size}")
        require(documents.isNotEmpty()) {
            "No documents loaded; check docs-repositories.json and fetch credentials"
        }

        val splitter = ragDocumentSplitter()
        val segmentCount = documents.sumOf { splitter.split(it).size }
        logger.info(
            "segments after DocumentSplitters.recursive($RAG_MAX_SEGMENT_CHARS, $RAG_MAX_OVERLAP_CHARS) --> $segmentCount"
        )

        InMemoryEmbeddingStore<TextSegment>().also { store ->
            EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .documentSplitter(splitter)
                .build()
                .ingest(documents)
            logger.info("embeddings ingest complete; planned segments --> $segmentCount")
            embeddingStore = store
            persistLocalEmbeddings(store, localEmbeddingsPath)
            if (upload) {
                uploadToRegistry(store, ktorClient)
            } else {
                logger.info("Skipping embeddings upload (no-upload mode); local file is $localEmbeddingsPath")
            }
            docsFetcher.cleanDocs()
        }
    }

    private suspend fun uploadToRegistry(
        store: InMemoryEmbeddingStore<TextSegment>,
        ktorClient: HttpClient
    ) {
        val jobToken = System.getenv("CI_JOB_TOKEN")
        val (token, header) = when {
            !gitLabAccessToken.isNullOrBlank() -> gitLabAccessToken to "PRIVATE-TOKEN"
            !jobToken.isNullOrBlank() -> jobToken to "JOB-TOKEN"
            else -> {
                logger.warn("GITLAB_ACCESS_TOKEN or CI_JOB_TOKEN missing; skipping embeddings upload after successful ingest")
                return
            }
        }

        val tempFile = createTempFile()
        try {
            store.serializeToFile(tempFile)
            ktorClient.uploadFile(
                url = "$PACKAGE_URL/$FILE_NAME",
                token = token,
                file = tempFile.toFile(),
                tokenHeader = header
            )
            logger.info("Uploaded embeddings to $PACKAGE_URL/$FILE_NAME")
        } finally {
            tempFile.deleteIfExists()
        }
    }
}

internal fun segmentTitle(segment: TextSegment): String {
    return segmentMetadataValue(segment, "file_name")
        ?: segment.text().lineSequence().firstOrNull()?.take(80)?.ifBlank { null }
        ?: "chromia-docs"
}

internal fun segmentUrl(segment: TextSegment): String {
    val firstLine = segment.text().lineSequence().firstOrNull()?.trim().orEmpty()
    val markdownUrl = firstLine.removePrefix("#").trim()
    if (markdownUrl.startsWith("https://")) {
        return markdownUrl
    }
    val fileName = segmentMetadataValue(segment, "file_name")
    val absDir = segmentMetadataValue(segment, "absolute_directory_path")
    return githubBlobUrl(absDir, fileName)
        ?: when {
            !fileName.isNullOrBlank() -> "https://docs.chromia.com/$fileName"
            else -> "https://docs.chromia.com"
        }
}

internal val githubRepoBranches = listOf(
    "postchain-client" to "dev",
    "postchain-eif" to "dev",
    "directory-chain" to "dev",
    "chromia-cli" to "dev",
    "ft4-lib" to "development",
    "postchain" to "dev",
    "rell" to "dev"
)

internal fun githubBlobUrl(absDir: String?, fileName: String?): String? {
    if (absDir.isNullOrBlank() || fileName.isNullOrBlank()) return null
    val combined = "$absDir/$fileName".replace('\\', '/')
    for ((repo, branch) in githubRepoBranches) {
        val marker = "/$repo/"
        val idx = combined.indexOf(marker)
        if (idx >= 0) {
            val relative = combined.substring(idx + marker.length)
            if (relative.isNotBlank()) {
                return "https://github.com/ChromiaProject/$repo/blob/$branch/$relative"
            }
        }
    }
    return null
}

/**
 * Clients sometimes send SHA-256 hex in uppercase or mixed case.
 * Store keys are lowercase hex from [segmentId]; normalize lookups.
 */
internal fun normalizeSegmentId(id: String): String = id.trim().lowercase()

/**
 * Stable store-wide segment id: lowercase SHA-256 hex (64 chars) of
 * `source\nchunkIndex\nchunk text`.
 * source = url else path else file_name else "unknown".
 * chunkIndex = splitter metadata `index` else 0.
 */
internal fun segmentId(segment: TextSegment): String {
    val source = segmentIdentitySource(segment)
    val chunkIndex = segmentChunkIndex(segment)
    val payload = "$source\n$chunkIndex\n${segment.text()}"
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    return digest.digest(payload.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

internal fun segmentIdentitySource(segment: TextSegment): String {
    val url = segmentDocumentUrl(segment)
    if (!url.isNullOrBlank()) return url
    val path = segmentDocumentPath(segment)
    if (!path.isNullOrBlank()) return path
    val fileName = segmentMetadataValue(segment, "file_name")
    if (!fileName.isNullOrBlank()) return fileName
    return "unknown"
}

internal fun segmentDocumentUrl(segment: TextSegment): String? {
    val firstLine = segment.text().lineSequence().firstOrNull()?.trim().orEmpty()
    val markdownUrl = firstLine.removePrefix("#").trim()
    if (markdownUrl.startsWith("https://")) return markdownUrl
    val metaUrl = segmentMetadataValue(segment, "url")
    if (!metaUrl.isNullOrBlank() && (metaUrl.startsWith("https://") || metaUrl.startsWith("http://"))) {
        return metaUrl
    }
    val fileName = segmentMetadataValue(segment, "file_name")
    val absDir = segmentMetadataValue(segment, "absolute_directory_path")
    return githubBlobUrl(absDir, fileName)
}

internal fun segmentDocumentPath(segment: TextSegment): String? {
    val fileName = segmentMetadataValue(segment, "file_name")
    val absDir = segmentMetadataValue(segment, "absolute_directory_path")
    return when {
        !absDir.isNullOrBlank() && !fileName.isNullOrBlank() ->
            "$absDir/$fileName".replace('\\', '/')
        !absDir.isNullOrBlank() -> absDir.replace('\\', '/')
        else -> null
    }
}

internal fun segmentChunkIndex(segment: TextSegment): Int {
    val raw = runCatching { segment.metadata()?.toMap()?.get("index") }.getOrNull()
    return when (raw) {
        is Number -> raw.toInt()
        is String -> raw.toIntOrNull() ?: 0
        else -> 0
    }
}

internal fun segmentMetadataValue(segment: TextSegment, key: String): String? = runCatching {
    segment.metadata()?.getString(key)?.takeIf { it.isNotBlank() }
}.getOrNull()

/**
 * easy-rag default (BGE-small-en-v1.5 quantized) embedding width.
 * Tried first when listing via public [InMemoryEmbeddingStore.search].
 */
internal const val RAG_MODEL_EMBEDDING_DIMENSION = 384

/**
 * Extra widths after the model dim. Production store is 384; tests often
 * use tiny fixtures (e.g. 3) that miss these and fall back to serializeToJson.
 */
internal val RAG_SEARCH_LIST_FALLBACK_DIMENSIONS = listOf(64, 384, 768)

internal fun searchListDimensions(modelDimension: Int = RAG_MODEL_EMBEDDING_DIMENSION): List<Int> {
    val dims = ArrayList<Int>()
    if (modelDimension > 0) dims.add(modelDimension)
    RAG_SEARCH_LIST_FALLBACK_DIMENSIONS.forEach { dim ->
        if (dim !in dims) dims.add(dim)
    }
    return dims
}

/**
 * Enumerates TextSegments already loaded in [InMemoryEmbeddingStore].
 * Prefer the public [InMemoryEmbeddingStore.search] API with a dummy vector
 * of the model dim, then 64 / 384 / 768, maxResults = Int.MAX_VALUE and
 * minScore = 0 so every same-width entry is returned. That avoids
 * serializeToJson() cloning the ~147MB store on startup.
 *
 * `entries` is package-private; do not reflect it. search() throws
 * IllegalArgumentException on width mismatch, so unknown widths (tiny test
 * fixtures) fall back to the public serializeToJson() parse. Fail loud if
 * that JSON shape loses `entries` / `embedded` / `text`.
 */
internal fun embeddingStoreSegments(store: InMemoryEmbeddingStore<TextSegment>): List<TextSegment> {
    for (dimension in searchListDimensions()) {
        searchAllSegments(store, dimension)?.let { return it }
    }
    return textSegmentsFromStoreJson(store.serializeToJson())
}

internal fun searchAllSegments(
    store: InMemoryEmbeddingStore<TextSegment>,
    dimension: Int
): List<TextSegment>? {
    return try {
        val probe = Embedding.from(FloatArray(dimension) { index -> if (index == 0) 1f else 0f })
        val request = EmbeddingSearchRequest.builder()
            .queryEmbedding(probe)
            .maxResults(Int.MAX_VALUE)
            .minScore(0.0)
            .build()
        store.search(request).matches().mapNotNull { it.embedded() }
    } catch (_: IllegalArgumentException) {
        null
    }
}

private val storeJsonFactory = JsonFactory()

/**
 * Parses store JSON and keeps only entry text + metadata. Embedding vectors
 * are skipped so a 147MB file does not become a second in-memory clone of
 * every float. Used only when search() cannot list (dimension mismatch).
 */
internal fun textSegmentsFromStoreJson(json: String): List<TextSegment> {
    return try {
        storeJsonFactory.createParser(json).use { parseStoreJsonSegments(it) }
    } catch (error: IllegalStateException) {
        throw error
    } catch (error: Exception) {
        throw IllegalStateException("serializeToJson() parse failed: ${error.message}", error)
    }
}

private fun parseStoreJsonSegments(parser: JsonParser): List<TextSegment> {
    val root = parser.nextToken()
        ?: error("InMemoryEmbeddingStore.serializeToJson() must be an object with entries or an array; got null")
    return when (root) {
        JsonToken.START_ARRAY -> parseEntryArray(parser)
        JsonToken.START_OBJECT -> parseEntriesObject(parser)
        else -> error(
            "InMemoryEmbeddingStore.serializeToJson() must be an object with entries or an array; got ${root.name}"
        )
    }
}

private fun parseEntriesObject(parser: JsonParser): List<TextSegment> {
    var entries: List<TextSegment>? = null
    while (parser.nextToken() != JsonToken.END_OBJECT) {
        if (parser.currentToken() != JsonToken.FIELD_NAME) continue
        val name = parser.currentName()
        val valueToken = parser.nextToken()
        if (name == "entries") {
            if (valueToken != JsonToken.START_ARRAY) {
                error("InMemoryEmbeddingStore.serializeToJson() must include an entries array")
            }
            entries = parseEntryArray(parser)
        } else {
            parser.skipChildren()
        }
    }
    return entries ?: error("InMemoryEmbeddingStore.serializeToJson() must include an entries array")
}

private fun parseEntryArray(parser: JsonParser): List<TextSegment> {
    val out = ArrayList<TextSegment>()
    while (parser.nextToken() != JsonToken.END_ARRAY) {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            error("serializeToJson() entry must include embedded")
        }
        out.add(parseEntry(parser))
    }
    return out
}

private fun parseEntry(parser: JsonParser): TextSegment {
    var segment: TextSegment? = null
    while (parser.nextToken() != JsonToken.END_OBJECT) {
        if (parser.currentToken() != JsonToken.FIELD_NAME) continue
        val name = parser.currentName()
        val valueToken = parser.nextToken()
        when (name) {
            "embedded" -> {
                if (valueToken != JsonToken.START_OBJECT) {
                    error("serializeToJson() entry must include embedded")
                }
                segment = parseEmbedded(parser)
            }
            else -> parser.skipChildren()
        }
    }
    return segment ?: error("serializeToJson() entry must include embedded")
}

private fun parseEmbedded(parser: JsonParser): TextSegment {
    var text: String? = null
    val metadataValues = linkedMapOf<String, Any>()
    while (parser.nextToken() != JsonToken.END_OBJECT) {
        if (parser.currentToken() != JsonToken.FIELD_NAME) continue
        val name = parser.currentName()
        val valueToken = parser.nextToken()
        when (name) {
            "text" -> {
                text = when (valueToken) {
                    JsonToken.VALUE_STRING -> parser.text
                    JsonToken.VALUE_NULL -> null
                    else -> error("serializeToJson() embedded must include text")
                }
            }
            "metadata" -> metadataValues.putAll(readMetadataMap(parser))
            else -> parser.skipChildren()
        }
    }
    val resolved = text ?: error("serializeToJson() embedded must include text")
    return if (metadataValues.isEmpty()) {
        TextSegment.from(resolved)
    } else {
        textSegmentWithMetadata(resolved, metadataValues)
    }
}

private fun readMetadataMap(parser: JsonParser): Map<String, Any> {
    if (parser.currentToken() != JsonToken.START_OBJECT) {
        val got = parser.currentToken()?.name ?: parser.currentToken()?.let { it::class.simpleName } ?: "null"
        error("serializeToJson() metadata must be a JSON object, got $got")
    }
    val raw = linkedMapOf<String, Any>()
    var nestedOnly: Map<String, Any>? = null
    var fieldCount = 0
    var onlyMetadataKey = true
    while (parser.nextToken() != JsonToken.END_OBJECT) {
        if (parser.currentToken() != JsonToken.FIELD_NAME) continue
        val key = parser.currentName()
        parser.nextToken()
        fieldCount++
        if (key == "metadata" && parser.currentToken() == JsonToken.START_OBJECT && nestedOnly == null && fieldCount == 1) {
            nestedOnly = readMetadataMap(parser)
        } else {
            onlyMetadataKey = false
            metadataScalar(parser)?.let { raw[key] = it }
        }
    }
    if (nestedOnly != null && onlyMetadataKey && fieldCount == 1) {
        return nestedOnly
    }
    if (nestedOnly != null && !onlyMetadataKey) {
        error("serializeToJson() metadata values must be scalars, got START_OBJECT")
    }
    return raw
}

private fun metadataScalar(parser: JsonParser): Any? = when (parser.currentToken()) {
    JsonToken.VALUE_NULL -> null
    JsonToken.VALUE_STRING -> parser.text
    JsonToken.VALUE_TRUE, JsonToken.VALUE_FALSE -> parser.booleanValue
    JsonToken.VALUE_NUMBER_INT -> {
        val n = parser.longValue
        if (n in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) n.toInt() else n
    }
    JsonToken.VALUE_NUMBER_FLOAT -> parser.doubleValue
    JsonToken.START_OBJECT, JsonToken.START_ARRAY ->
        error("serializeToJson() metadata values must be scalars, got ${parser.currentToken()?.name}")
    else -> error("serializeToJson() metadata values must be scalars, got ${parser.currentToken()?.name}")
}

internal fun textSegmentWithMetadata(text: String, values: Map<String, Any>): TextSegment {
    val metadata = Metadata()
    values.forEach { (key, value) ->
        when (value) {
            is String -> metadata.put(key, value)
            is Int -> metadata.put(key, value)
            is Long -> metadata.put(key, value)
            is Double -> metadata.put(key, value)
            is Float -> metadata.put(key, value)
            is Boolean -> metadata.put(key, value.toString())
            else -> metadata.put(key, value.toString())
        }
    }
    return TextSegment.from(text, metadata)
}


internal const val RAG_MAX_SEGMENT_CHARS = 1000
internal const val RAG_MAX_OVERLAP_CHARS = 150

internal fun ragDocumentSplitter() = DocumentSplitters.recursive(RAG_MAX_SEGMENT_CHARS, RAG_MAX_OVERLAP_CHARS)

/**
 * Candidate local embeddings paths relative to the process working directory.
 * `build/embeddings.json` is the Gradle-module cwd default; `app/build/embeddings.json`
 * is what `java -jar` from the repo root should find after `:app:generateEmbeddingsNoUpload`.
 */
fun defaultLocalEmbeddingsCandidates(): List<Path> = listOf(
    Path.of("build", RagStore.FILE_NAME),
    Path.of("app", "build", RagStore.FILE_NAME)
)

/**
 * Local embeddings path. `CHROMIA_EMBEDDINGS_PATH` wins. Otherwise the first existing
 * candidate from [defaultLocalEmbeddingsCandidates] (so `java -jar` from the repo root
 * finds `app/build/embeddings.json` if present). If neither file exists and cwd is
 * the repo root (`settings.gradle.kts`) with an `app/build/` tree, first persist
 * writes `app/build/embeddings.json` (same place as Gradle JavaExec). Otherwise
 * `build/embeddings.json`. Gradle JavaExec tasks set the env to `app/build/embeddings.json`.
 */
fun resolveLocalEmbeddingsPath(
    env: Map<String, String> = System.getenv(),
    exists: (Path) -> Boolean = { it.isRegularFile() },
    directoryExists: (Path) -> Boolean = { java.nio.file.Files.isDirectory(it) }
): Path {
    val configured = env[RagStore.EMBEDDINGS_PATH_ENV]?.takeIf { it.isNotBlank() }
    if (configured != null) return Path.of(configured)
    val candidates = defaultLocalEmbeddingsCandidates()
    candidates.firstOrNull(exists)?.let { return it }
    val repoRoot = exists(Path.of("settings.gradle.kts"))
    val appBuildTree = directoryExists(Path.of("app", "build"))
    if (repoRoot && appBuildTree) {
        return Path.of("app", "build", RagStore.FILE_NAME)
    }
    return candidates.first()
}

internal fun loadLocalEmbeddings(path: Path): InMemoryEmbeddingStore<TextSegment>? {
    if (!path.isRegularFile()) return null
    return runCatching {
        logger.info("Loading embeddings from local file $path")
        InMemoryEmbeddingStore.fromFile(path).also {
            logger.info("Successfully loaded embeddings from local file")
        }
    }.onFailure { error ->
        logger.warn("Failed to load local embeddings from $path: ${error.message}")
    }.getOrNull()
}

internal fun persistLocalEmbeddings(store: InMemoryEmbeddingStore<TextSegment>, path: Path) {
    path.parent?.createDirectories()
    store.serializeToFile(path)
    logger.info("Persisted embeddings to $path")
}

internal fun createRegistryDownloadClient(
    engine: HttpClientEngine = CIO.create()
): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = RagStore.REGISTRY_REQUEST_TIMEOUT_MS
        connectTimeoutMillis = RagStore.REGISTRY_CONNECT_TIMEOUT_MS
    }
}
