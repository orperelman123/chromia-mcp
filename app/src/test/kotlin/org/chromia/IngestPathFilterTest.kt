package org.chromia

import org.chromia.tools.docs.fetcher.IngestPathFilter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class IngestPathFilterTest {

    @Test
    fun acceptsDocsAndSourceText() {
        assertTrue(IngestPathFilter.accept(Path("doc/guide.md")))
        assertTrue(IngestPathFilter.accept(Path("src/main.rell")))
        assertTrue(IngestPathFilter.accept(Path("Foo.kt")))
        assertTrue(IngestPathFilter.accept(Path("chromia.yml")))
        assertTrue(IngestPathFilter.accept(Path("client.ts")))
    }

    @Test
    fun rejectsBinariesSecretsAndMetadata() {
        assertFalse(IngestPathFilter.accept(Path("diagram.png")))
        assertFalse(IngestPathFilter.accept(Path("truststore.jks")))
        assertFalse(IngestPathFilter.accept(Path("alice.keypair")))
        assertFalse(IngestPathFilter.accept(Path(".gitignore")))
        assertFalse(IngestPathFilter.accept(Path("flow.graphml")))
    }
}
