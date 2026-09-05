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
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.chromia.App.Companion.logger
import org.chromia.downloadFile
import org.chromia.tools.docs.fetcher.DocsFetcher
import org.chromia.tools.docs.fetcher.IngestPathFilter
import org.chromia.uploadFile
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

open class RagStore(
    loadFromRegistry: Boolean = true,
    initialStore: InMemoryEmbeddingStore<TextSegment>? = null,
    val localEmbeddingsPath: Path = resolveLocalEmbeddingsPath(),
    registryLoader: (() -> InMemoryEmbeddingStore<TextSegment>?)? = null,
    val embeddingModel: EmbeddingModel? = null,
    /** Where a downloaded index is kept between runs; null = download every boot, keep nothing. Unused with [registryLoader]. */
    val cacheEmbeddingsPath: Path? = resolveCacheEmbeddingsPath()
) {
    val ktorClient by lazy { HttpClient() }

    companion object {
        const val FILE_NAME = "embeddings.json"
        /** GitLab generic package - the original publish target; only writable with a ChromaWay token. */
        const val PACKAGE_URL = "https://gitlab.com/api/v4/projects/71940508/packages/generic/embeddings/v1"
        /**
         * Release asset the `Embeddings refresh` workflow publishes (tag `embeddings`,
         * asset clobbered in place so the URL is stable). Tried before [PACKAGE_URL]:
         * the GitLab package was last uploaded 2025-10-21 and nobody outside ChromaWay
         * can refresh it, while this asset is rebuilt from the live sources by CI.
         */
        const val GITHUB_REPO = "orperelman123/chromia-mcp"
        const val GITHUB_RELEASE_TAG = "embeddings"
        const val GITHUB_RELEASE_URL = "https://github.com/$GITHUB_REPO/releases/download/$GITHUB_RELEASE_TAG/$FILE_NAME"
        /** Release metadata endpoint; its `assets[].url` is the only download path a private repo allows (unused while the repo is public). */
        const val GITHUB_RELEASE_API_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/tags/$GITHUB_RELEASE_TAG"
        const val EMBEDDINGS_PATH_ENV = "CHROMIA_EMBEDDINGS_PATH"
        /** Optional first remote to try, ahead of the GitHub release and the GitLab package. */
        const val EMBEDDINGS_URL_ENV = "CHROMIA_EMBEDDINGS_URL"
        /**
         * The launcher's home (`packages/npm/bin/chromia-mcp.mjs` keeps the jar there);
         * the downloaded index is kept beside it as `embeddings.json` + `embeddings.cache.json`
         * and reused until it is [CACHE_REFRESH_AFTER] old. Default `~/.chromia-mcp`.
         */
        const val HOME_ENV = "CHROMIA_MCP_HOME"
        /** `CHROMIA_EMBEDDINGS_CACHE=off` downloads every boot and keeps nothing (what CI's production-shaped boot wants). */
        const val CACHE_ENV = "CHROMIA_EMBEDDINGS_CACHE"
        /** The `Embeddings refresh` workflow publishes weekly; a cache older than this is refreshed on boot, and still served if the refresh fails. */
        val CACHE_REFRESH_AFTER: Duration = Duration.ofDays(7)

        /** Sidecar recording which URL the cached body came from, its Last-Modified and when it was fetched. */
        fun cacheMetaPath(cache: Path): Path =
            cache.resolveSibling(cache.fileName.toString().removeSuffix(".json") + ".cache.json")
        /**
         * Optional token for the GitHub release download. The repository has been public
         * since 2026-09-05, so the plain `releases/download/...` URL is 200 with no
         * credential and this is normally unset. It exists for the private case (the repo
         * WAS private 2026-09-04 and the asset was 404 to production, which fell through
         * to the year-old GitLab package without a word): with a token the asset is
         * fetched through the releases API instead (`Accept: application/octet-stream`
         * on `assets[].url`). A fine-grained token with read access to Contents on the
         * repo is enough. `GITHUB_TOKEN` is honoured as a fallback so a GitHub Actions
         * job needs nothing extra.
         */
        const val EMBEDDINGS_TOKEN_ENV = "CHROMIA_EMBEDDINGS_TOKEN"
        const val GITHUB_TOKEN_ENV = "GITHUB_TOKEN"
        /** Render mounts Secret Files here at runtime; the Dockerfile reads the same file as a BuildKit secret. */
        val EMBEDDINGS_TOKEN_FILE: Path = Path.of("/etc/secrets", EMBEDDINGS_TOKEN_ENV)

        /** `CHROMIA_EMBEDDINGS_TOKEN`, else the secret file, else `GITHUB_TOKEN`; blank counts as unset. */
        fun embeddingsToken(env: Map<String, String> = System.getenv(), tokenFile: Path = EMBEDDINGS_TOKEN_FILE): String? =
            env[EMBEDDINGS_TOKEN_ENV]?.takeIf { it.isNotBlank() }
                ?: runCatching { if (tokenFile.isRegularFile()) tokenFile.readText().trim().takeIf { it.isNotEmpty() } else null }.getOrNull()
                ?: env[GITHUB_TOKEN_ENV]?.takeIf { it.isNotBlank() }
        /**
         * Whole-request budget for one remote download. Was 10 s, sized for the
         * 18.8 MB GitLab package; the fresh store is 150 MB, which 10 s would
         * abort on anything slower than 15 MB/s. Stalls are caught by the socket
         * timeout instead, so a dead peer does not hold the loader for the full budget.
         */
        const val REGISTRY_REQUEST_TIMEOUT_MS = 600_000L
        const val REGISTRY_SOCKET_TIMEOUT_MS = 30_000L
        const val REGISTRY_CONNECT_TIMEOUT_MS = 5_000L
        const val LOAD_RETRY_COOLDOWN_MS = 60_000L

        /** Remote candidates in order: env override, GitHub release asset, GitLab package. */
        fun remoteEmbeddingsUrls(env: Map<String, String> = System.getenv()): List<String> =
            listOfNotNull(env[EMBEDDINGS_URL_ENV]?.takeIf { it.isNotBlank() }, GITHUB_RELEASE_URL, "$PACKAGE_URL/$FILE_NAME").distinct()

        /** Human-readable origin for a remote URL, used in [Provenance.origin]. */
        internal fun describeRemote(url: String): String = when {
            url.startsWith(PACKAGE_URL) -> "GitLab registry package $url"
            url == GITHUB_RELEASE_URL -> "GitHub release asset $url"
            else -> "remote embeddings $url"
        }

        /**
         * An index older than this is announced as stale. The store is a snapshot
         * of moving sources (chr releases monthly, FT4 and the docs site move
         * with it); at this age an agent asking "how do I..." is being answered
         * from a different release than the one it deploys with.
         */
        val STALE_AFTER: Duration = Duration.ofDays(120)

        /** Hits per query, lexical and semantic together. */
        const val MAX_HITS = 15
        /** Exact-identifier hits kept per identifier token in the query. */
        const val LEXICAL_HITS_PER_TOKEN = 3
        /**
         * Identifier tokens scanned per query, first-mentioned first. Each token
         * is one pass over every segment; a pasted stack trace carried 40+ names
         * and took 4.1 s against the 25823-segment store (2026-09-04).
         */
        const val MAX_IDENTIFIER_TOKENS = 8
        private val DEFINITION_KEYWORDS = "function|operation|query|struct|entity|object|val|def|fun|class|enum|namespace"

        /**
         * Identifier-shaped tokens: snake_case, dotted.names, camelCase - at
         * least 5 chars. Plain words and acronyms (`FT4`, `Rell`) are not
         * identifiers; neither is a file name (`chromia.yml`, `main.rell`),
         * which would pull in every segment that mentions the file.
         */
        private val IDENTIFIER_TOKEN = Regex("""\b(?:[A-Za-z][A-Za-z0-9]*(?:[_.][A-Za-z0-9]+)+|[a-z][a-z0-9]+[A-Z][A-Za-z0-9]*)\b""")
        /**
         * Long CLI flags (`--tests`, `--hide-lib-warnings`) are exact names an agent copies
         * from a terminal, and the docs spell them the same way; the embedding model does
         * not (2026-09-05: `chr test --tests` ranked repl.md first). `--` alone and short
         * flags (`-t`) are not names. Stops at `=`, so the value is not part of the flag.
         */
        private val CLI_FLAG_TOKEN = Regex("""(?<![\w-])--[a-z][a-z0-9]*(?:-[a-z0-9]+)*(?![\w-])""")
        private val FILE_EXTENSIONS = setOf("yml", "yaml", "rell", "md", "json", "kt", "kts", "ts", "js", "py", "xml", "html", "txt", "jar", "com", "org", "io", "dev")

        internal fun identifierTokens(query: String): List<String> =
            (IDENTIFIER_TOKEN.findAll(query) + CLI_FLAG_TOKEN.findAll(query))
                .sortedBy { it.range.first } // first-mentioned first, across both shapes
                .map { it.value.trim('.', '_') }
                .filter { it.length >= 5 }
                .filterNot { token -> token.contains('.') && token.substringAfterLast('.').lowercase() in FILE_EXTENSIONS }
                .distinct()
                .take(MAX_IDENTIFIER_TOKENS)
                .toList()

        /** One token's compiled matchers - built once per query token, not once per segment. */
        internal class TokenMatcher(token: String) {
            val lower: String = token.lowercase()
            private val definition = Regex("""(?im)^\s*(?:$DEFINITION_KEYWORDS)\s+${Regex.escape(token)}\b""")
            private val mention = Regex(Regex.escape(token), RegexOption.IGNORE_CASE)

            /** Definition sites outrank mentions; more mentions outrank fewer. */
            fun score(text: String): Int =
                (if (definition.containsMatchIn(text)) 1000 else 0) + mention.findAll(text).count()
        }

        internal fun lexicalScore(text: String, token: String): Int = TokenMatcher(token).score(text)

        internal fun parseLastModified(header: String?): Instant? =
            header?.let { runCatching { Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(it.trim())) }.getOrNull() }

        /**
         * A remotely fetched store with where it came from, the server's Last-Modified
         * (null when absent) and, when the caller asked to keep it, the temp file the
         * body was streamed to (the caller owns and must move or delete it).
         */
        data class RemoteEmbeddings(
            val store: InMemoryEmbeddingStore<TextSegment>,
            val url: String,
            val lastModified: Instant?,
            val file: Path? = null
        )

        /** A store the runtime will answer from, with the provenance to record for it. */
        data class LoadedEmbeddings(val store: InMemoryEmbeddingStore<TextSegment>, val provenance: Provenance)

        /**
         * First remote in [urls] that answers 200 with a parseable store. A 404,
         * an HTTP error, a timeout or a corrupt body moves on to the next URL,
         * so a missing GitHub asset still reaches the GitLab package and vice
         * versa. The body is streamed to a temp file and parsed entry by entry
         * ([EmbeddingStoreJson]) - never held in memory whole. With [keepFile]
         * the temp file is handed back in [RemoteEmbeddings.file] instead of deleted.
         */
        fun downloadRemoteEmbeddings(
            client: HttpClient? = null,
            urls: List<String> = remoteEmbeddingsUrls(),
            token: String? = embeddingsToken(),
            keepFile: Boolean = false
        ): RemoteEmbeddings? {
            val owned = client == null
            val http = client ?: createRegistryDownloadClient()
            try {
                for (url in urls) {
                    var lastModified: Instant? = null
                    val loaded = runCatching {
                        runBlocking {
                            val (downloadUrl, headers) = resolveDownload(http, url, token)
                            logger.info("Downloading embeddings from $downloadUrl")
                            http.downloadFile(downloadUrl, headers) { response ->
                                lastModified = parseLastModified(response.headers[HttpHeaders.LastModified])
                            }?.let { tempFile ->
                                var keep = false
                                try {
                                    val store = EmbeddingStoreJson.read(tempFile)
                                    keep = keepFile
                                    store to tempFile.takeIf { keepFile }
                                } finally {
                                    if (!keep) tempFile.deleteIfExists()
                                }
                            }
                        }
                    }.onFailure { error ->
                        logger.warn("Embeddings download from $url skipped: ${error.message}")
                    }.getOrNull()
                    if (loaded != null) return RemoteEmbeddings(loaded.first, url, lastModified, loaded.second)
                }
                return null
            } finally {
                if (owned) {
                    http.close()
                }
            }
        }

        /** What the sidecar next to a cached index records. */
        private data class CacheMeta(val url: String, val lastModified: Instant?, val downloadedAt: Instant) {
            fun provenance(segments: Int): Provenance = Provenance(
                "cached ${describeRemote(url)} (fetched ${DateTimeFormatter.ISO_LOCAL_DATE.format(downloadedAt.atOffset(ZoneOffset.UTC))})",
                lastModified ?: downloadedAt,
                segments
            )
        }

        private fun readCacheMeta(cache: Path): CacheMeta? {
            if (!cache.isRegularFile()) return null
            val meta = runCatching {
                val json = Json.parseToJsonElement(cacheMetaPath(cache).readText()).jsonObject
                CacheMeta(
                    url = json["url"]!!.jsonPrimitive.content,
                    lastModified = json["last_modified"]?.jsonPrimitive?.takeIf { it.isString }?.content?.let(Instant::parse),
                    downloadedAt = Instant.parse(json["downloaded_at"]!!.jsonPrimitive.content)
                )
            }.getOrNull()
            // A body without a readable sidecar (hand-copied, or an older layout) is dated by its mtime.
            return meta ?: CacheMeta(GITHUB_RELEASE_URL, null, cache.getLastModifiedTime().toInstant())
        }

        private fun keepInCache(remote: RemoteEmbeddings, cache: Path, now: Instant) {
            val file = remote.file ?: return
            runCatching {
                cache.parent?.createDirectories()
                try {
                    Files.move(file, cache, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: AtomicMoveNotSupportedException) {
                    Files.move(file, cache, StandardCopyOption.REPLACE_EXISTING)
                }
                cacheMetaPath(cache).writeText(
                    buildJsonObject {
                        put("url", remote.url)
                        put("last_modified", remote.lastModified?.toString())
                        put("downloaded_at", now.toString())
                    }.toString()
                )
                logger.info("Kept the downloaded embeddings at $cache; boots for the next ${CACHE_REFRESH_AFTER.toDays()} days read it back instead of downloading")
            }.onFailure { error ->
                logger.warn("Could not keep the downloaded embeddings at $cache: ${error.message}")
                file.deleteIfExists()
            }
        }

        /**
         * The runtime's remote step, local-first: a cached body younger than
         * [CACHE_REFRESH_AFTER] at [cachePath] is read back without touching the
         * network; otherwise [download] runs and its body replaces the cache. A
         * failed refresh still serves the old cache (offline is not "no index"),
         * and a body that no longer parses is refreshed rather than served.
         * [cachePath] null = download every boot, keep nothing.
         */
        fun loadCachedOrRemote(
            cachePath: Path?,
            now: Instant = Instant.now(),
            download: () -> RemoteEmbeddings?
        ): LoadedEmbeddings? {
            val cached = cachePath?.let(::readCacheMeta)
            fun serveCache(why: String): LoadedEmbeddings? {
                if (cached == null) return null
                val store = loadLocalEmbeddings(cachePath!!) ?: return null
                logger.info("Serving the cached embeddings from $cachePath ($why)")
                return LoadedEmbeddings(store, cached.provenance(embeddingStoreSegments(store).size))
            }
            if (cached != null && Duration.between(cached.downloadedAt, now) <= CACHE_REFRESH_AFTER) {
                serveCache("fetched ${cached.downloadedAt}, refresh after ${CACHE_REFRESH_AFTER.toDays()} days")?.let { return it }
                logger.warn("Cached embeddings at $cachePath could not be read; downloading a fresh copy")
            }
            val remote = runCatching(download).onFailure { error ->
                logger.warn("Embeddings download failed: ${error.message}")
            }.getOrNull()
            if (remote != null) {
                if (cachePath != null) keepInCache(remote, cachePath, now) else remote.file?.deleteIfExists()
                return LoadedEmbeddings(
                    remote.store,
                    Provenance(describeRemote(remote.url), remote.lastModified, embeddingStoreSegments(remote.store).size)
                )
            }
            return serveCache("the refresh failed; this copy is from ${cached?.downloadedAt}")
        }

        /**
         * The URL and headers to actually GET for [url]. Only the GitHub release with a
         * token is special: the public download URL is 404 on a private repo, so the
         * asset id is looked up on the releases API and fetched as an octet-stream
         * with the token. Everything else is fetched as given.
         */
        internal suspend fun resolveDownload(http: HttpClient, url: String, token: String?): Pair<String, Map<String, String>> {
            if (url != GITHUB_RELEASE_URL || token.isNullOrBlank()) return url to emptyMap()
            val auth = mapOf(HttpHeaders.Authorization to "Bearer $token", "X-GitHub-Api-Version" to "2022-11-28")
            val release = http.get(GITHUB_RELEASE_API_URL) {
                auth.forEach { (k, v) -> header(k, v) }
                header(HttpHeaders.Accept, "application/vnd.github+json")
            }
            if (release.status != HttpStatusCode.OK) {
                logger.info("GitHub release lookup answered HTTP ${release.status}; trying the public asset URL")
                return url to emptyMap()
            }
            val asset = runCatching {
                Json.parseToJsonElement(release.bodyAsText()).jsonObject["assets"]?.jsonArray
                    ?.map { it.jsonObject }
                    ?.firstOrNull { it["name"]?.jsonPrimitive?.content == FILE_NAME }
                    ?.get("url")?.jsonPrimitive?.content
            }.getOrNull()
            if (asset == null) {
                logger.info("GitHub release $GITHUB_RELEASE_TAG has no $FILE_NAME asset; trying the public asset URL")
                return url to emptyMap()
            }
            return asset to auth + (HttpHeaders.Accept to "application/octet-stream")
        }
    }

    /**
     * Where the loaded index came from and how old it is. Round 10 (2026-09-04)
     * found the published registry package was generated 2025-10-21 - 18.8 MB
     * against the 140 MB / 25555-segment local ingest of 2026-08-26 that was
     * never uploaded - so production, the Docker image and every clone without
     * a local file answered from a year-old store, and `fetch_docs` could not
     * find FT4's `require_mandatory_flags` (added to the sources since). Nothing
     * said so: the store is a black box unless its age is written down.
     */
    data class Provenance(
        /** Human-readable origin: the local path or the registry package URL. */
        val origin: String,
        /** When the index was generated (local mtime / registry Last-Modified); null = unknown. */
        val generatedAt: Instant?,
        val segments: Int
    ) {
        fun ageDays(now: Instant): Long? = generatedAt?.let { Duration.between(it, now).toDays() }

        fun describe(now: Instant = Instant.now()): String {
            val date = generatedAt?.let { DateTimeFormatter.ISO_LOCAL_DATE.format(it.atZone(ZoneOffset.UTC)) } ?: "unknown date"
            val age = ageDays(now)?.let { " ($it days old)" } ?: ""
            return "documentation index: $segments segments from $origin, generated $date$age"
        }

        /**
         * Structured form for tool output. The hosted server loads the store lazily, so
         * `/health` cannot say what index it runs on; this is how a client verifies a
         * deploy actually picked up a published asset (2026-09-04: it had not, twice).
         */
        fun toJson(now: Instant = Instant.now()): JsonObject = buildJsonObject {
            put("origin", origin)
            put("generated_at", generatedAt?.toString())
            put("age_days", ageDays(now))
            put("segments", segments)
            put("stale", staleWarning(now) != null)
        }

        /** A warning naming the fix when the index is older than [staleAfter]; null when fresh or of unknown age. */
        fun staleWarning(now: Instant = Instant.now(), staleAfter: Duration = STALE_AFTER): String? {
            val days = ageDays(now) ?: return null
            if (days <= staleAfter.toDays()) return null
            return "documentation index is STALE: generated ${DateTimeFormatter.ISO_LOCAL_DATE.format(generatedAt!!.atZone(ZoneOffset.UTC))}, " +
                "$days days ago (limit ${staleAfter.toDays()}). Anything released since - chr, FT4, Rell, the docs site - is missing " +
                "or outdated here; confirm versions against GitLab tags. Fix: run the `Embeddings refresh` GitHub workflow " +
                "(publishes $GITHUB_RELEASE_URL), or ship a fresh embeddings.json via $EMBEDDINGS_PATH_ENV."
        }
    }

    private val gitLabAccessToken = System.getenv("GITLAB_ACCESS_TOKEN")
    val docsFetcher by lazy { DocsFetcher() }
    private val segmentsById = ConcurrentHashMap<String, TextSegment>()

    /** A segment with its text lowercased once, so the lexical scan is a plain indexOf per token. */
    private class LexicalEntry(val segment: TextSegment, val lowerText: String)

    /** Rebuilt with [segmentsById]; ~1 KB per segment (25 MB for the production store). */
    @Volatile
    private var lexicalIndex: List<LexicalEntry> = emptyList()

    var embeddingStore: InMemoryEmbeddingStore<TextSegment>? = null
        set(value) {
            field = value
            rebuildSegmentIndex(value)
        }

    // A transient local/registry load failure at startup used to leave
    // embeddingStore null until redeploy - every search/fetch_docs answered
    // "not found" forever (audit F5). Keep the load recipe and retry it on use,
    // at most once per cooldown window.
    /**
     * Origin and age of the loaded index; null until a load succeeds, and for
     * fixture stores handed in directly. Read by the docs tools to warn on a
     * stale store (see [Provenance]).
     */
    @Volatile
    var provenance: Provenance? = null
        internal set

    private val storeLoader: (() -> InMemoryEmbeddingStore<TextSegment>?)? =
        if (loadFromRegistry && initialStore == null) {
            {
                loadLocalEmbeddings(localEmbeddingsPath)?.also { store ->
                    val mtime = runCatching { localEmbeddingsPath.getLastModifiedTime().toInstant() }.getOrNull()
                    recordProvenance(Provenance("local file $localEmbeddingsPath", mtime, embeddingStoreSegments(store).size))
                } ?: run {
                    val remote = if (registryLoader != null) {
                        registryLoader()?.let { LoadedEmbeddings(it, Provenance(describeRemote("injected loader"), null, embeddingStoreSegments(it).size)) }
                    } else {
                        loadCachedOrRemote(cacheEmbeddingsPath) { downloadRemoteEmbeddings(keepFile = cacheEmbeddingsPath != null) }
                    }
                    remote?.also { recordProvenance(it.provenance) }?.store
                }
            }
        } else {
            null
        }

    private fun recordProvenance(p: Provenance) {
        provenance = p
        logger.info(p.describe())
        p.staleWarning()?.let { logger.warn(it) }
    }

    /** The stale-index warning for the loaded store, or null when fresh, unknown, or not loaded. */
    fun staleWarning(now: Instant = Instant.now()): String? = provenance?.staleWarning(now)
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
            .maxResults(MAX_HITS) // topK
            .minScore(0.6) // similarity score
            .build()
        return runCatching {
            val semantic = retriever.retrieve(Query.from(query))?.mapNotNull { it.textSegment() } ?: emptyList()
            mergeHits(lexicalHits(query), semantic).also { rememberQueryHits(it) }
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

    /**
     * Exact-identifier hits for the identifier-shaped tokens in [query], best
     * first. Round 10 (2026-09-04): `require_mandatory_flags` was IN the store
     * (three segments of ft4-lib's accounts/module.rell) and still missed under
     * every phrasing, the bare identifier included - BGE-small ranks prose about
     * auth descriptors above the code that defines the name, and an agent asks
     * about names. Dense retrieval alone is the wrong tool for a name; this is
     * the lexical half of a hybrid: scan the in-memory segment index for the
     * token, prefer the segment that DEFINES it, cap per token.
     */
    internal fun lexicalHits(query: String): List<TextSegment> {
        val tokens = identifierTokens(query)
        if (tokens.isEmpty()) return emptyList()
        val index = lexicalIndex
        return tokens.flatMap { token ->
            val matcher = TokenMatcher(token)
            index.asSequence()
                .filter { it.lowerText.contains(matcher.lower) }
                .map { it.segment to matcher.score(it.segment.text()) }
                .sortedByDescending { it.second }
                .take(LEXICAL_HITS_PER_TOKEN)
                .map { it.first }
                .toList()
        }
    }

    /** Lexical hits first (the name the agent asked about), then semantic, deduplicated, capped at [MAX_HITS]. */
    internal fun mergeHits(lexical: List<TextSegment>, semantic: List<TextSegment>): List<TextSegment> {
        val seen = HashSet<String>()
        return (lexical + semantic).filter { seen.add(segmentId(it)) }.take(MAX_HITS)
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
        lexicalIndex = emptyList()
        if (store == null) return
        val segments = embeddingStoreSegments(store)
        segments.forEach { segment ->
            segmentsById[segmentId(segment)] = segment
        }
        lexicalIndex = segments.map { LexicalEntry(it, it.text().lowercase()) }
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

/**
 * Where a downloaded index is kept between runs: `$CHROMIA_MCP_HOME/embeddings.json`,
 * default `~/.chromia-mcp/embeddings.json` - the same home the npm launcher keeps the
 * jar in, so one directory holds everything a local install downloads. Null when
 * `CHROMIA_EMBEDDINGS_CACHE=off`.
 */
fun resolveCacheEmbeddingsPath(
    env: Map<String, String> = System.getenv(),
    userHome: String = System.getProperty("user.home")
): Path? {
    if (env[RagStore.CACHE_ENV]?.trim()?.equals("off", ignoreCase = true) == true) return null
    val home = env[RagStore.HOME_ENV]?.takeIf { it.isNotBlank() }?.let { Path.of(it) }
        ?: Path.of(userHome, ".chromia-mcp")
    return home.resolve(RagStore.FILE_NAME)
}

internal fun loadLocalEmbeddings(path: Path): InMemoryEmbeddingStore<TextSegment>? {
    if (!path.isRegularFile()) return null
    return runCatching {
        logger.info("Loading embeddings from local file $path")
        EmbeddingStoreJson.read(path).also {
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
    // downloadFile follows redirects itself so an Authorization header never
    // travels to the object-storage host GitHub redirects release assets to.
    followRedirects = false
    install(HttpTimeout) {
        requestTimeoutMillis = RagStore.REGISTRY_REQUEST_TIMEOUT_MS
        socketTimeoutMillis = RagStore.REGISTRY_SOCKET_TIMEOUT_MS
        connectTimeoutMillis = RagStore.REGISTRY_CONNECT_TIMEOUT_MS
    }
}
