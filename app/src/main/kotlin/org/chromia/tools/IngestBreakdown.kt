package org.chromia.tools

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.chromia.tools.docs.fetcher.IngestPathFilter
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.io.path.writeText

/**
 * WHAT THE INGEST DID, PER SOURCE - the numbers the refresh gate compares.
 *
 * The `Embeddings refresh` workflow used to decide whether a fresh store may
 * replace the published one by comparing the two files' SIZES: under 80% of the
 * published bytes and the run is refused as a half-failed ingest. That gate went
 * red on 2026-09-07 (run 34103273206) and again on 2026-09-09 (run 34344751517)
 * - 109,958,949 bytes against 147,681,194, 74.5% - and it was wrong both times.
 * Nothing had failed to clone: commit f0ee597 (audit F15) had taught
 * [IngestPathFilter.isTestSource] to leave the host-language TEST sources out of
 * the corpus, because `ReplDefinitionTest.kt` was outranking the `.md` pages on
 * ten basic Rell questions. The store is smaller ON PURPOSE, and a byte count
 * cannot tell a deliberate exclusion from a repository that half-arrived - which
 * is the one thing the gate exists to detect.
 *
 * So the generator now writes down what it saw, per source: the documents and
 * segments it INDEXED, and separately the documents and segments the ingest
 * rules REFUSED. Their sum is what the source offered, which is the quantity
 * that is comparable across an ingest-rule change. `scripts/embeddings-gate.mjs`
 * compares that, per source, against the published sidecar.
 */
data class SourceIngest(
    /** Top-level directory under the fetch root: a repository name, or the sitemap mirror. */
    val source: String,
    val documents: Int,
    val segments: Int,
    val excludedDocuments: Int,
    val excludedSegments: Int
) {
    /** Every segment the source OFFERED - indexed plus deliberately excluded. */
    val availableSegments: Int get() = segments + excludedSegments
}

/** The per-source rows plus the totals derived from them (never counted twice). */
data class IngestBreakdown(val sources: List<SourceIngest>) {
    val documents: Int get() = sources.sumOf { it.documents }
    val segments: Int get() = sources.sumOf { it.segments }
    val excludedDocuments: Int get() = sources.sumOf { it.excludedDocuments }
    val excludedSegments: Int get() = sources.sumOf { it.excludedSegments }
    val availableSegments: Int get() = segments + excludedSegments

    /** The same table the sidecar carries, for the generator's log. */
    fun table(): String {
        val rows = StringBuilder()
        rows.append(ROW_FORMAT.format("source", "documents", "segments", "ex-docs", "ex-segs", "available"))
        sources.forEach { s ->
            rows.append('\n').append(
                ROW_FORMAT.format(
                    s.source,
                    s.documents.toString(),
                    s.segments.toString(),
                    s.excludedDocuments.toString(),
                    s.excludedSegments.toString(),
                    s.availableSegments.toString()
                )
            )
        }
        rows.append('\n').append(
            ROW_FORMAT.format(
                "TOTAL",
                documents.toString(),
                segments.toString(),
                excludedDocuments.toString(),
                excludedSegments.toString(),
                availableSegments.toString()
            )
        )
        return rows.toString()
    }

    companion object {
        private const val ROW_FORMAT = "%-24s %10s %10s %10s %10s %10s"
    }
}

/** The bucket for a document the fetch root cannot attribute to a source. Never expected; never silent. */
internal const val UNATTRIBUTED_SOURCE = "(unattributed)"

/** Names the rule whose refusals the `excluded_*` numbers count, so the sidecar says what it means. */
internal const val INGEST_EXCLUSION_RULE = "IngestPathFilter.isTestSource"

/**
 * The breakdown for one ingest.
 *
 * [documents] is the list that was actually embedded, so the `documents` and
 * `segments` columns are the ingest's own numbers rather than a second walk's
 * opinion of them. The excluded columns come from walking [root] with the same
 * extension matcher MINUS the exclusion and splitting what it finds with the
 * same [splitter] - no embedding, so it costs a read and a split.
 *
 * Every name in [configuredSources] gets a row even when it produced nothing: a
 * repository that failed to clone must appear as zeros, not disappear.
 */
internal fun ingestBreakdown(
    root: Path,
    documents: List<Document>,
    splitter: DocumentSplitter,
    configuredSources: List<String>
): IngestBreakdown {
    val indexed = HashMap<String, IntArray>()
    documents.forEach { document ->
        val row = indexed.getOrPut(documentSource(root, document)) { IntArray(2) }
        row[0]++
        row[1] += runCatching { splitter.split(document).size }.getOrDefault(0)
    }
    val excluded = excludedByIngestRules(root, splitter)
    val names = (configuredSources + indexed.keys + excluded.keys).distinct().sorted()
    return IngestBreakdown(
        names.map { name ->
            val i = indexed[name] ?: EMPTY_ROW
            val e = excluded[name] ?: EMPTY_ROW
            SourceIngest(name, i[0], i[1], e[0], e[1])
        }
    )
}

private val EMPTY_ROW = IntArray(2)

/**
 * Documents and segments the ingest rules refused under [root], by source.
 *
 * "Refused" means exactly: the file's extension is in
 * [IngestPathFilter.allowedExtensions] and it is not a dotfile - so the corpus
 * would have taken it - and [IngestPathFilter.isTestSource] said no. A file with
 * an extension the corpus never wanted (a `.png`, a keystore) is not an
 * exclusion and is not counted here.
 */
internal fun excludedByIngestRules(root: Path, splitter: DocumentSplitter): Map<String, IntArray> {
    val out = HashMap<String, IntArray>()
    if (!Files.isDirectory(root)) return out
    Files.walk(root).use { stream ->
        stream.filter { Files.isRegularFile(it) && isExcludedByIngestRules(it) }.forEach { file ->
            // Loaded and split exactly as the ingest would have, so the count is
            // the segments this file WOULD have contributed - and skipped on the
            // same failures the loader skips on (a blank file is not a document).
            val document = runCatching { FileSystemDocumentLoader.loadDocument(file) }.getOrNull()
                ?: return@forEach
            val row = out.getOrPut(pathSource(root, file.parent)) { IntArray(2) }
            row[0]++
            row[1] += runCatching { splitter.split(document).size }.getOrDefault(0)
        }
    }
    return out
}

/** True for a file the corpus wanted by extension and [IngestPathFilter.isTestSource] refused. */
internal fun isExcludedByIngestRules(path: Path): Boolean {
    val name = path.fileName?.toString() ?: return false
    if (name.startsWith(".")) return false
    val extension = name.substringAfterLast('.', "").lowercase()
    if (extension !in IngestPathFilter.allowedExtensions) return false
    return IngestPathFilter.isTestSource(path)
}

/** The source a loaded document came from, read from the loader's own metadata. */
internal fun documentSource(root: Path, document: Document): String {
    val directory = runCatching { document.metadata().getString("absolute_directory_path") }.getOrNull()
    if (directory.isNullOrBlank()) return UNATTRIBUTED_SOURCE
    return pathSource(root, runCatching { Path.of(directory) }.getOrNull())
}

/** First path element of [path] under [root] - the repository directory the fetcher created. */
internal fun pathSource(root: Path, path: Path?): String {
    if (path == null) return UNATTRIBUTED_SOURCE
    val relative = runCatching {
        root.toAbsolutePath().normalize().relativize(path.toAbsolutePath().normalize())
    }.getOrNull() ?: return UNATTRIBUTED_SOURCE
    val first = relative.firstOrNull()?.toString().orEmpty()
    return if (first.isBlank() || first == "..") UNATTRIBUTED_SOURCE else first
}

/** `embeddings.json` -> `embeddings.provenance.json`, beside the store. */
fun provenanceSidecarPath(store: Path): Path =
    store.resolveSibling(store.fileName.toString().substringBeforeLast('.') + ".provenance.json")

/**
 * The sidecar's ingest half. The workflow adds `commit`, `run` and `probes` -
 * facts only the run knows - and publishes the result beside the store.
 */
internal fun ingestProvenanceJson(
    breakdown: IngestBreakdown,
    sizeBytes: Long,
    generatedAt: Instant
): JsonObject = buildJsonObject {
    put("generated_at", generatedAt.truncatedTo(ChronoUnit.SECONDS).toString())
    put("segments", breakdown.segments)
    put("size_bytes", sizeBytes)
    put("documents", breakdown.documents)
    put("excluded_documents", breakdown.excludedDocuments)
    put("excluded_segments", breakdown.excludedSegments)
    put("excluded_by", INGEST_EXCLUSION_RULE)
    putJsonArray("sources") {
        breakdown.sources.forEach { source ->
            add(
                buildJsonObject {
                    put("source", source.source)
                    put("documents", source.documents)
                    put("segments", source.segments)
                    put("excluded_documents", source.excludedDocuments)
                    put("excluded_segments", source.excludedSegments)
                }
            )
        }
    }
}

/** Writes the sidecar beside [store] and returns its path. */
fun writeIngestProvenance(
    store: Path,
    breakdown: IngestBreakdown,
    generatedAt: Instant = Instant.now()
): Path {
    val sizeBytes = runCatching { Files.size(store) }.getOrDefault(0L)
    val sidecar = provenanceSidecarPath(store)
    sidecar.writeText(
        Json.encodeToString(JsonObject.serializer(), ingestProvenanceJson(breakdown, sizeBytes, generatedAt)) + "\n"
    )
    return sidecar
}
