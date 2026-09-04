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
    fun dockerfileUsesTheSameReleaseApiAndSecretNameAsTheRuntimeAndNeverAnArgForTheToken() {
        val dockerfile = Files.readString(repoRoot().resolve("Dockerfile"))
        assertTrue(
            dockerfile.contains("https://api.github.com/repos/${RagStore.GITHUB_REPO}/releases/tags/${RagStore.GITHUB_RELEASE_TAG}"),
            "Dockerfile must resolve the private asset through RagStore.GITHUB_RELEASE_API_URL"
        )
        assertTrue(
            dockerfile.contains("--mount=type=secret,id=${RagStore.EMBEDDINGS_TOKEN_ENV}"),
            "Dockerfile must read the token as a BuildKit secret named ${RagStore.EMBEDDINGS_TOKEN_ENV} (the runtime reads ${RagStore.EMBEDDINGS_TOKEN_FILE})"
        )
        assertTrue(
            Regex("""(?m)^\s*ARG\s+${RagStore.EMBEDDINGS_TOKEN_ENV}""").containsMatchIn(dockerfile).not(),
            "the token must never be an ARG - build args linger in image metadata"
        )
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
