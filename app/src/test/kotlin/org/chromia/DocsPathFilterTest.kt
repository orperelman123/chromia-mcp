package org.chromia

import org.chromia.tools.docs.fetcher.DocsPathFilter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.writeText

class DocsPathFilterTest {

    @Test
    fun keepsNestedModuleDocAndDropsSiblings(@TempDir root: Path) {
        root.resolve("postchain-client/doc").createDirectories()
        root.resolve("postchain-client/src").createDirectories()
        root.resolve("postchain-client/doc/guide.md").createFile().writeText("# client")
        root.resolve("chromia-client").createDirectories()
        root.resolve("README.md").createFile().writeText("readme")

        DocsPathFilter.keepAllowedPaths(root.toFile(), listOf("postchain-client/doc"))

        assertTrue(root.resolve("postchain-client/doc/guide.md").exists())
        assertFalse(root.resolve("postchain-client/src").exists())
        assertFalse(root.resolve("chromia-client").exists())
        assertFalse(root.resolve("README.md").exists())
    }

    @Test
    fun keepsTopLevelModuleDirs(@TempDir root: Path) {
        root.resolve("doc").createDirectories()
        root.resolve("rell-base/src").createDirectories()
        root.resolve("rell-gtx/src").createDirectories()
        root.resolve("performance").createDirectories()

        DocsPathFilter.keepAllowedPaths(root.toFile(), listOf("doc", "rell-base", "rell-gtx"))

        assertTrue(root.resolve("doc").exists())
        assertTrue(root.resolve("rell-base/src").exists())
        assertTrue(root.resolve("rell-gtx/src").exists())
        assertFalse(root.resolve("performance").exists())
    }

    @Test
    fun deletesGitMetadata(@TempDir root: Path) {
        root.resolve("doc").createDirectories()
        root.resolve(".git/objects").createDirectories()
        DocsPathFilter.keepAllowedPaths(root.toFile(), listOf("doc"))
        assertTrue(root.resolve("doc").exists())
        assertFalse(root.resolve(".git").exists())
    }
}
