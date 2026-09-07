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

/**
 * A REAL sparse clone of a third-party GitHub repository.
 *
 * There is no gate on this test and there is deliberately no escape hatch: the
 * network is a third party, and a third party being down is a RED with retry as
 * the remedy, not a green. This test used to end with
 *
 *     if (!doc.exists()) { System.err.println("... skipped ..."); return }
 *
 * which is the worst shape in a suite - a pass that did no work, invisible to
 * the skip counter because JUnit sees a method that completed normally. The
 * scan in [AssumptionLedgerTest.noTestAnnouncesThatItIsNotTestingAndThenPasses]
 * now refuses that shape anywhere in the tree.
 */
class GitRepositoryFetcherLiveTest {

    @Test
    fun sparseFetchesNestedPostchainClientDoc() {
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
            val present = runCatching {
                fetcher.tempDir.resolve("postchain-client").listDirectoryEntries().map { it.fileName.toString() }
            }.getOrDefault(emptyList())
            assertTrue(
                doc.exists() && doc.isDirectory(),
                "the sparse checkout produced no directory at $doc. Either the clone failed " +
                    "(third-party outage - retry) or upstream moved the path, which is precisely " +
                    "what this test exists to notice. Checkout contains: $present"
            )
            assertTrue(doc.listDirectoryEntries().isNotEmpty(), "sparse doc directory is empty: $doc")
            assertFalse(fetcher.tempDir.resolve("postchain-client/postchain-client/src").exists())
            assertFalse(fetcher.tempDir.resolve("postchain-client/.git").exists())
        } finally {
            fetcher.tempDir.toFile().deleteRecursively()
        }
    }
}
