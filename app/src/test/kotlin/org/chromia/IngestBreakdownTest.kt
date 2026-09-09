package org.chromia

import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.chromia.tools.docs.fetcher.DocsConfigParser
import org.chromia.tools.docs.fetcher.DocsFetcher
import org.chromia.tools.docs.fetcher.IngestPathFilter
import org.chromia.tools.ingestBreakdown
import org.chromia.tools.provenanceSidecarPath
import org.chromia.tools.ragDocumentSplitter
import org.chromia.tools.writeIngestProvenance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * THE PROVENANCE SIDECAR'S SHAPE, PINNED AGAINST THE REAL WRITER.
 *
 * `scripts/embeddings-gate.mjs` is the step that decides whether a fresh index
 * may replace the published one, and every number it reads comes out of this
 * writer. A field renamed here and nowhere else does not fail a compile; it
 * fails a Monday-morning refresh with "the new sidecar has no per-source
 * breakdown", or - worse, if the gate were more forgiving - passes one that
 * should have been refused.
 *
 * Nothing here is stood in for. The tree is REAL files on disk, laid out the way
 * `DocsFetcher.fetchRepositories` lays one out (one directory per configured
 * repository plus the sitemap mirror); the documents come from the REAL
 * `FileSystemDocumentLoader` with the REAL `IngestPathFilter.pathMatcher()`; the
 * segments come from the REAL `ragDocumentSplitter()`; the source list comes
 * from the REAL `docs-repositories.json` that ships in the jar. The only thing
 * that does not happen is the network fetch and the embedding.
 */
class IngestBreakdownTest {

    @TempDir
    lateinit var root: Path

    /** The seven repositories the shipped config names, plus the sitemap mirror. */
    private val configuredSources: List<String> =
        DocsConfigParser().parseConfig().repositories.map { it.name } + DocsFetcher.SITEMAP_DIRECTORY

    /**
     * Two of the eight sources get real files. The other six get nothing, which
     * is the case that matters most: a repository that failed to clone has to
     * appear in the table as zeros. If it simply vanished, the gate would
     * compare the sources that survived and call a lost repository a pass.
     */
    private fun fetchedTree() {
        write("rell/doc/language-guide.md", "# Rell\n\n" + "An at-expression reads `@*` and `@?`. ".repeat(40))
        write("rell/rell-base/src/main/kotlin/ReplDefinition.kt", "package rell\n\n" + "class ReplDefinition { fun run() = 1 }\n".repeat(40))
        // Excluded by name (audit F15): a *Test.kt outside any test directory.
        write("rell/rell-base/src/main/kotlin/ReplDefinitionTest.kt", "package rell\n\n" + "class ReplDefinitionTest { fun t() = 1 }\n".repeat(40))
        // Excluded by directory: src/test/... - the same rule, the other half of it.
        write("rell/rell-base/src/test/kotlin/LibRellTestBlockClock.kt", "package rell\n\n" + "class LibRellTestBlockClock { fun t() = 2 }\n".repeat(40))
        // Wanted by extension, refused by neither rule: a docs page under docs/tests/.
        write("rell/doc/tests/writing-tests.md", "# Writing tests\n\n" + "Use `@test module` and `run_must_fail`. ".repeat(40))
        write("docs-chromia-com/ft4/accounts.md", "# https://docs.chromia.com/ft4/accounts\n\n" + "require_mandatory_flags on an auth descriptor. ".repeat(40))
        // Neither indexed nor excluded: the corpus never wanted these extensions,
        // and counting them as exclusions would inflate the gate's denominator.
        write("rell/doc/diagram.png", "not really a png")
        write("rell/.gitignore", "build/")
    }

    private fun write(relative: String, text: String) {
        val file = root.resolve(relative)
        file.parent.createDirectories()
        file.writeText(text)
    }

    private fun breakdown() = ingestBreakdown(
        root,
        FileSystemDocumentLoader.loadDocumentsRecursively(root, IngestPathFilter.pathMatcher()),
        ragDocumentSplitter(),
        configuredSources
    )

    @Test
    fun everyConfiguredSourceGetsARowEvenWhenItFetchedNothing() {
        fetchedTree()
        val rows = breakdown().sources.associateBy { it.source }

        assertEquals(
            configuredSources.sorted(), rows.keys.sorted(),
            "the table must have exactly one row per configured repository plus the sitemap mirror, " +
                "so a source that fetched nothing shows as zeros instead of disappearing from the comparison"
        )
        for (silent in configuredSources - setOf("rell", DocsFetcher.SITEMAP_DIRECTORY)) {
            assertTrue(silent in rows, "no row for the configured source $silent")
            assertEquals(0, rows.getValue(silent).documents, "$silent fetched nothing in this tree")
            assertEquals(0, rows.getValue(silent).availableSegments, "$silent offered no segments in this tree")
        }
    }

    @Test
    fun theExcludedColumnsCountTheFilesTheIngestRulesRefusedAndNothingElse() {
        fetchedTree()
        val rows = breakdown().sources.associateBy { it.source }
        val rell = rows.getValue("rell")

        // Indexed: the language guide, the docs page under doc/tests/, and the
        // non-test Kotlin source. Excluded: the two test sources. The .png and
        // the dotfile are in neither column.
        assertEquals(3, rell.documents, "indexed documents for rell")
        assertEquals(2, rell.excludedDocuments, "the two host-language test sources, and only those")
        assertTrue(rell.segments >= 3, "each indexed document contributes at least one segment; got ${rell.segments}")
        assertTrue(
            rell.excludedSegments >= 2,
            "the excluded documents are split with the same splitter, so they contribute segments too; " +
                "got ${rell.excludedSegments}"
        )
        assertEquals(
            rell.segments + rell.excludedSegments, rell.availableSegments,
            "`available` is what the source OFFERED - it is the sum, and it is what the gate compares"
        )

        val sitemap = rows.getValue(DocsFetcher.SITEMAP_DIRECTORY)
        assertEquals(1, sitemap.documents, "the one sitemap page")
        assertEquals(0, sitemap.excludedDocuments, "the sitemap mirror is markdown; nothing there is a test source")
    }

    @Test
    fun theTotalsAreTheRowsAndTheLoaderAgreesWithThem() {
        fetchedTree()
        val loaded = FileSystemDocumentLoader.loadDocumentsRecursively(root, IngestPathFilter.pathMatcher())
        val breakdown = ingestBreakdown(root, loaded, ragDocumentSplitter(), configuredSources)

        assertEquals(
            loaded.size, breakdown.documents,
            "every document the ingest embedded must be attributed to a source; RagStore requires this " +
                "before it publishes anything"
        )
        assertEquals(breakdown.sources.sumOf { it.segments }, breakdown.segments, "totals are the rows")
        assertEquals(
            breakdown.segments + breakdown.excludedSegments, breakdown.availableSegments,
            "the gate's quantity: segments offered = indexed + deliberately excluded"
        )
        assertTrue(
            breakdown.table().contains("TOTAL"),
            "the generator logs this table; it must carry the totals row an operator reads first"
        )
    }

    @Test
    fun theSidecarCarriesEveryFieldTheGateReadsBesideTheStore() {
        fetchedTree()
        val breakdown = breakdown()
        val store = root.resolve("embeddings.json")
        store.writeText("{}")

        val sidecar = writeIngestProvenance(store, breakdown, Instant.parse("2026-09-09T11:22:33.456Z"))
        assertEquals(
            root.resolve("embeddings.provenance.json"), sidecar,
            "the sidecar is named after the store and sits beside it - that is the pair the release " +
                "publishes and the pair `gh release download` fetches back"
        )
        assertTrue(Files.isRegularFile(sidecar), "the sidecar was not written")

        val json = Json.parseToJsonElement(sidecar.readText()).jsonObject
        for (field in listOf(
            "generated_at", "segments", "size_bytes", "documents",
            "excluded_documents", "excluded_segments", "excluded_by", "sources"
        )) {
            assertTrue(field in json, "scripts/embeddings-gate.mjs reads `$field`; the sidecar does not carry it")
        }
        assertEquals(
            "2026-09-09T11:22:33Z", json.getValue("generated_at").jsonPrimitive.content,
            "the timestamp is truncated to seconds, the way the published sidecar of 2026-09-04 spells it"
        )
        assertEquals(
            "IngestPathFilter.isTestSource", json.getValue("excluded_by").jsonPrimitive.content,
            "the sidecar has to say WHICH rule the excluded counts belong to, or a future rule change " +
                "silently changes what the gate is comparing"
        )
        assertEquals(
            2L, json.getValue("size_bytes").jsonPrimitive.content.toLong(),
            "size_bytes is measured off the store on disk, not carried over from a log line"
        )
        assertEquals(
            breakdown.segments, json.getValue("segments").jsonPrimitive.int,
            "the published `segments` is the generator's own count"
        )

        val rows = json.getValue("sources").jsonArray.map { it.jsonObject }
        assertEquals(
            configuredSources.sorted(),
            rows.map { it.getValue("source").jsonPrimitive.content }.sorted(),
            "the sidecar's rows are the configured sources plus the sitemap"
        )
        for (row in rows) {
            for (field in listOf("source", "documents", "segments", "excluded_documents", "excluded_segments")) {
                assertTrue(field in row, "a source row is missing `$field`: $row")
            }
        }
        assertEquals(
            breakdown.excludedSegments,
            rows.sumOf { it.getValue("excluded_segments").jsonPrimitive.int },
            "the total the gate falls back to must be the sum of the rows it would otherwise compare"
        )
    }

    @Test
    fun aStoreWithNoFetchedTreeStillProducesTheFullTable() {
        // The empty case is not hypothetical: it is what a fetch that failed
        // outright looks like, and the gate has to see eight zero rows rather
        // than an empty table it would have nothing to say about.
        val breakdown = ingestBreakdown(root, emptyList(), ragDocumentSplitter(), configuredSources)
        assertEquals(configuredSources.size, breakdown.sources.size)
        assertEquals(0, breakdown.availableSegments)
        assertEquals(provenanceSidecarPath(root.resolve("embeddings.json")), root.resolve("embeddings.provenance.json"))
    }
}
