package org.chromia

import dev.langchain4j.data.document.Document
import org.chromia.tools.RAG_MAX_SEGMENT_CHARS
import org.chromia.tools.ragDocumentSplitter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RagChunkSplitterTest {

    @Test
    fun longDocumentSplitsIntoMultipleSegments() {
        val text = "word ".repeat(500)
        val parts = ragDocumentSplitter().split(Document.from(text))
        assertTrue(parts.size > 1, "expected multiple segments, got ${parts.size}")
        assertTrue(parts.all { it.text().length <= RAG_MAX_SEGMENT_CHARS + 50 })
    }

    @Test
    fun headingMarkdownSplitsOnStructure() {
        val text = (1..6).joinToString("\n\n") { n ->
            "# Heading $n\n\n" + ("paragraph $n words ".repeat(40))
        }
        val parts = ragDocumentSplitter().split(Document.from(text))
        assertTrue(parts.size > 1, "expected heading-aware split, got ${parts.size}")
        assertTrue(parts.any { it.text().contains("# Heading") })
        assertTrue(parts.all { it.text().length <= RAG_MAX_SEGMENT_CHARS + 50 })
    }
}
