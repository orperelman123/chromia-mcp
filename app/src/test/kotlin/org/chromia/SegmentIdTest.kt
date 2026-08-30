package org.chromia

import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import org.chromia.tools.segmentChunkIndex
import org.chromia.tools.segmentId
import org.chromia.tools.segmentIdentitySource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SegmentIdTest {

    @Test
    fun isLowercaseSha256OfSourceChunkIndexAndText() {
        val segment = TextSegment.from("hello chunk", Metadata.from("file_name", "ft4-auth.md"))
        assertEquals("ft4-auth.md", segmentIdentitySource(segment))
        assertEquals(0, segmentChunkIndex(segment))
        val id = segmentId(segment)
        assertEquals(64, id.length)
        assertTrue(id.matches(Regex("[0-9a-f]{64}")))
        assertEquals("9d0337f1e95621082a4abd3a577995e688f8d1eed82cf23319cc68f97c509508", id)
    }

    @Test
    fun usesSplitterIndexWhenPresent() {
        val meta = Metadata.from("file_name", "rell.md")
        meta.put("index", 3)
        val segment = TextSegment.from("chunk text", meta)
        assertEquals(3, segmentChunkIndex(segment))
        val withoutIndex = TextSegment.from("chunk text", Metadata.from("file_name", "rell.md"))
        assertNotEquals(segmentId(segment), segmentId(withoutIndex))
    }

    @Test
    fun prefersUrlOverPathAndFileName() {
        val meta = Metadata()
        meta.put("file_name", "library.md")
        meta.put("absolute_directory_path", "/tmp/docs/build/cli")
        meta.put("url", "https://docs.chromia.com/build/cli/library/")
        val segment = TextSegment.from("Library commands", meta)
        assertEquals("https://docs.chromia.com/build/cli/library/", segmentIdentitySource(segment))
    }

    @Test
    fun prefersGithubUrlFromPathOverRawTempPath() {
        val meta = Metadata()
        meta.put("file_name", "README.md")
        meta.put("absolute_directory_path", "/tmp/chromia_docs_repos1/postchain-client/postchain-client/doc")
        val segment = TextSegment.from("client docs", meta)
        assertEquals(
            "https://github.com/ChromiaProject/postchain-client/blob/dev/postchain-client/doc/README.md",
            segmentIdentitySource(segment)
        )
    }

    @Test
    fun fallsBackToPathThenFileNameThenUnknown() {
        val withPath = TextSegment.from(
            "plain",
            Metadata.from(
                mapOf(
                    "file_name" to "local.md",
                    "absolute_directory_path" to "/tmp/other/docs"
                )
            )
        )
        assertEquals("/tmp/other/docs/local.md", segmentIdentitySource(withPath))

        val fileOnly = TextSegment.from("plain", Metadata.from("file_name", "local.md"))
        assertEquals("local.md", segmentIdentitySource(fileOnly))

        val empty = TextSegment.from("plain")
        assertEquals("unknown", segmentIdentitySource(empty))
    }

    @Test
    fun doesNotDependOnQueryOrder() {
        val a = TextSegment.from("alpha", Metadata.from("file_name", "a.md"))
        val b = TextSegment.from("beta", Metadata.from("file_name", "b.md"))
        assertEquals(segmentId(a), segmentId(a))
        assertNotEquals(segmentId(a), segmentId(b))
    }
}
