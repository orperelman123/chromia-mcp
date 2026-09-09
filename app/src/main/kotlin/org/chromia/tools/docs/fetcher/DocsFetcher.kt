package org.chromia.tools.docs.fetcher

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.chromia.App.Companion.logger
import org.chromia.safeDelete
import kotlin.io.path.div

class DocsFetcher {
    private val configParser: DocsConfigParser = DocsConfigParser()
    private val docsConfig by lazy { configParser.parseConfig() }
    private val docsSettings by lazy { docsConfig.settings }
    private val gitFetcher by lazy { GitRepositoryFetcher(docsSettings) }

    /**
     * The top-level directory names [fetchRepositories] creates under its temp
     * dir - one per configured repository, plus the sitemap mirror. This is the
     * row list the ingest breakdown must account for: a repository that failed
     * to clone has to show up as zeros rather than vanish from the table.
     */
    val sourceNames: List<String>
        get() = docsConfig.repositories.map { it.name } + SITEMAP_DIRECTORY

    suspend fun fetchRepositories() = coroutineScope {
        logger.info("Fetching repositories --> ${docsConfig.repositories.map { it.name }}")
        val limiter = Semaphore(docsSettings.concurrentFetches.coerceAtLeast(1))
        docsConfig.repositories.map { repo ->
            async(Dispatchers.IO) {
                limiter.withPermit { gitFetcher.fetchDocs(repo) }
            }
        }.awaitAll()

        HttpClient(CIO) { installDocsHttpTimeout(this) }.use { sitemapClient ->
            safeSitemapIngest {
                SitemapDocsFetcher(
                    sitemapUrl = docsSettings.sitemapUrl,
                    concurrentFetches = docsSettings.concurrentFetches,
                    client = sitemapClient
                ).fetchInto(gitFetcher.tempDir / SITEMAP_DIRECTORY)
            }
        }

        gitFetcher.tempDir
    }

    internal suspend fun safeSitemapIngest(ingest: suspend () -> Int) {
        try {
            val written = ingest()
            if (written == 0) {
                logger.warn("Sitemap ingest produced no pages; continuing with git remotes only")
            }
        } catch (e: CancellationException) {
            // Never swallow coroutine cancellation.
            throw e
        } catch (e: Exception) {
            logger.warn("Sitemap ingest skipped: ${e::class.simpleName}: ${e.message}")
        }
    }

    companion object {
        /** Where [SitemapDocsFetcher] writes the docs.chromia.com pages, and the source name they carry. */
        const val SITEMAP_DIRECTORY = "docs-chromia-com"
    }

    fun cleanDocs() {
        if (!docsSettings.cleanupOnExit) {
            logger.info("Skipping temporary docs cleanup (cleanup_on_exit=false)")
            return
        }
        runCatching {
            gitFetcher.tempDir.toFile().safeDelete()
        }.fold(
            onSuccess = { logger.info("temporary docs files deleted successfully") },
            onFailure = { logger.error("Unable to delete temporary files: ${it.message}") }
        )
    }
}
