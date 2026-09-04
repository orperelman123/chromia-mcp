package org.chromia

import org.chromia.tools.RagStore
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * The Dockerfile bakes embeddings.json into the image at build time from the
 * SAME remotes, in the SAME order, as the runtime fallback: the GitHub release
 * asset first, the GitLab package second. The URLs cannot be shared as code
 * between a Dockerfile and Kotlin, so this test is the single source of truth:
 * if RagStore.GITHUB_RELEASE_URL or PACKAGE_URL/FILE_NAME ever changes, this
 * fails until the Dockerfile download step is updated to match.
 */
class DockerfileEmbeddingsBakeTest {

    private fun repoRoot(): Path {
        var dir: Path? = Path.of("").toAbsolutePath()
        while (dir != null) {
            if (Files.isRegularFile(dir.resolve("settings.gradle.kts")) &&
                Files.isRegularFile(dir.resolve("Dockerfile"))
            ) {
                return dir
            }
            dir = dir.parent
        }
        error("repo root with settings.gradle.kts + Dockerfile not found above ${Path.of("").toAbsolutePath()}")
    }

    @Test
    fun dockerfileDownloadsFromTheRuntimeRemotesInTheRuntimeOrder() {
        val dockerfile = Files.readString(repoRoot().resolve("Dockerfile"))
        val urls = RagStore.remoteEmbeddingsUrls(emptyMap())
        val positions = urls.map { url ->
            val at = dockerfile.indexOf(url)
            assertTrue(at >= 0, "Dockerfile must bake embeddings from the runtime remote $url - update the download step")
            at
        }
        assertTrue(positions == positions.sorted(), "Dockerfile must try the remotes in runtime order: $urls")
    }

    @Test
    fun dockerfilePointsRuntimeAtTheBakedFile() {
        val dockerfile = Files.readString(repoRoot().resolve("Dockerfile"))
        assertTrue(
            dockerfile.contains("ENV ${RagStore.EMBEDDINGS_PATH_ENV}="),
            "Dockerfile must set ${RagStore.EMBEDDINGS_PATH_ENV} so RagStore loads the baked file from disk"
        )
    }
}
