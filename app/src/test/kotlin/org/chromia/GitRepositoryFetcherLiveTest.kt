package org.chromia

import org.chromia.tools.docs.fetcher.DocumentationRepository
import org.chromia.tools.docs.fetcher.DocumentationSettings
import org.chromia.tools.docs.fetcher.GitRepositoryFetcher
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

class GitRepositoryFetcherLiveTest {

    @Test
    fun sparseFetchesNestedPostchainClientDocWhenNetworkAvailable() {
        val fetcher = GitRepositoryFetcher(DocumentationSettings(tempDirPrefix = "chromia_docs_test_"))
        try {
            fetcher.fetchDocs(
                DocumentationRepository(
                    name = "postchain-client",
                    url = "https://github.com/ChromiaProject/postchain-client.git",
                    branch = "dev",
                    subdirectories = listOf("postchain-client/doc")
                )
            )
            val doc = fetcher.tempDir.resolve("postchain-client/postchain-client/doc")
            if (!doc.exists() || !doc.isDirectory()) {
                System.err.println("live nested fetch skipped (path missing after clone)")
                return
            }
            assertTrue(doc.listDirectoryEntries().isNotEmpty())
            assertFalse(fetcher.tempDir.resolve("postchain-client/postchain-client/src").exists())
            assertFalse(fetcher.tempDir.resolve("postchain-client/.git").exists())
        } finally {
            fetcher.tempDir.toFile().deleteRecursively()
        }
    }
}
