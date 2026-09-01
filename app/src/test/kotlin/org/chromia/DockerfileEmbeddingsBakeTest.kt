package org.chromia

import org.chromia.tools.RagStore
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * The Dockerfile bakes embeddings.json into the image at build time from the
 * SAME GitLab registry URL the runtime fallback uses. The URL cannot be
 * shared as code between a Dockerfile and Kotlin, so this test is the single
 * source of truth: if RagStore.PACKAGE_URL/FILE_NAME ever changes, this
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
    fun dockerfileDownloadsFromTheRuntimeRegistryUrl() {
        val dockerfile = Files.readString(repoRoot().resolve("Dockerfile"))
        val runtimeUrl = "${RagStore.PACKAGE_URL}/${RagStore.FILE_NAME}"
        assertTrue(
            dockerfile.contains(runtimeUrl),
            "Dockerfile must bake embeddings from the runtime registry URL $runtimeUrl - " +
                "update the Dockerfile download step to match RagStore.PACKAGE_URL/FILE_NAME"
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
