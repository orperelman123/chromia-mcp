package org.chromia.tools.docs.fetcher

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.chromia.App.Companion.logger
import org.chromia.safeDelete

class DocsFetcher {
    private val configParser: DocsConfigParser = DocsConfigParser()
    private val docsConfig by lazy { configParser.parseConfig() }
    private val docsSettings by lazy { docsConfig.settings }
    private val gitFetcher by lazy { GitRepositoryFetcher(docsSettings) }

    suspend fun fetchRepositories() = coroutineScope {
        logger.info("Fetching repositories --> ${docsConfig.repositories.map { it.name }}")
        docsConfig.repositories
            .chunked(docsSettings.concurrentFetches)
            .flatMap { chunk ->
                chunk.map {
                    async { gitFetcher.fetchDocs(it) }
                }
            }.awaitAll()

        gitFetcher.tempDir
    }

    fun cleanDocs() = runCatching {
        gitFetcher.tempDir.toFile().safeDelete()
    }.fold(
        onSuccess = { logger.info("temporary docs files deleted successfully") },
        onFailure = { logger.error("Unable to delete temporary files: ${it.message}") }
    )
}
