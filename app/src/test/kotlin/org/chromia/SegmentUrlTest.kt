package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import org.chromia.tools.githubBlobUrl
import org.chromia.tools.segmentUrl
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SegmentUrlTest {

    @Test
    fun prefersDocsUrlFromFirstMarkdownLine() {
        val segment = TextSegment.from("# https://docs.chromia.com/build/cli/library/\n\nLibrary commands")
        assertEquals("https://docs.chromia.com/build/cli/library/", segmentUrl(segment))
    }

    @Test
    fun mapsNestedGitPathToPublicGitHubBlob() {
        val url = githubBlobUrl(
            "/tmp/chromia_docs_repos1/postchain-client/postchain-client/doc",
            "README.md"
        )
        assertEquals(
            "https://github.com/ChromiaProject/postchain-client/blob/dev/postchain-client/doc/README.md",
            url
        )
    }

    @Test
    fun prefersLongerRepoNameOverPostchainPrefix() {
        val url = githubBlobUrl("/tmp/x/postchain-eif/doc", "intro.md")
        assertEquals("https://github.com/ChromiaProject/postchain-eif/blob/dev/doc/intro.md", url)
    }

    @Test
    fun usesFileNameFallbackWhenNoRepoHint() {
        val segment = TextSegment.from(
            "plain text",
            Metadata.from("file_name", "rell.md")
        )
        assertEquals("https://docs.chromia.com/rell.md", segmentUrl(segment))
    }
}
