package org.chromia.tools

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.rag.query.Query
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import org.chromia.App.Companion.logger
import org.chromia.downloadFile
import org.chromia.tools.docs.fetcher.DocsFetcher
import kotlin.io.path.deleteIfExists
// import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
// import org.chromia.uploadFile
// import kotlin.io.path.createTempFile

class RagStore {
    val ktorClient = HttpClient()

    companion object {
        const val FILE_NAME = "embeddings.json"
        const val PACKAGE_URL = "https://gitlab.com/api/v4/projects/71940508/packages/generic/embeddings/v1"
    }

    private val gitLabAccessToken = System.getenv("GITLAB_ACCESS_TOKEN")
    val docsFetcher = DocsFetcher()
    var embeddingStore = downloadFromRegistry() // ?: createAndUploadEmbeddings()

    private fun downloadFromRegistry() = runBlocking {
        logger.info("Download embedding from Gitlab registry")
        ktorClient.downloadFile("$PACKAGE_URL/$FILE_NAME")?.let { tempFile ->
            InMemoryEmbeddingStore.fromFile(tempFile).also {
                logger.info("Successfully loaded embeddings from registry")
                tempFile.deleteIfExists()
            }
        }
    }

    fun query(query: String): List<TextSegment>? {
        val retriever: ContentRetriever? = EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .maxResults(15) // topK
            .minScore(0.6) // similarity score
            .build()
        return retriever?.retrieve(Query.from(query))?.mapNotNull { it.textSegment() }
        // TODO: REMOVE ME
        // .also { logger.info(it?.joinToString()) }
    }

    /* TODO: should be removed once a pipeline that creates and updates embeddings for docs is created
    private fun createAndUploadEmbeddings(): InMemoryEmbeddingStore<TextSegment> = runBlocking {
        logger.info("Creating embeddings...")
        val fetchedReposPath = docsFetcher.fetchRepositories()
        val documents = FileSystemDocumentLoader.loadDocumentsRecursively(fetchedReposPath)
        logger.info("documents loaded successfully --> ${documents.size}")

        InMemoryEmbeddingStore<TextSegment>().also { store ->
            EmbeddingStoreIngestor.ingest(documents, store)
            uploadToRegistry(store, ktorClient)
            docsFetcher.cleanDocs()
        }
    }

    private suspend fun uploadToRegistry(
        store: InMemoryEmbeddingStore<TextSegment>,
        ktorClient: HttpClient
    ) = runCatching {
        val tempFile = createTempFile()
        store.serializeToFile(tempFile)

        ktorClient.uploadFile(
            url = "$PACKAGE_URL/$FILE_NAME",
            token = gitLabAccessToken,
            file = tempFile.toFile()
        )

        tempFile.deleteIfExists()
    }.onFailure { error ->
        logger.error("Upload failed", error)
    }
    */
}
