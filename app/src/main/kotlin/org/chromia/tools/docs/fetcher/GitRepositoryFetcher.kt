package org.chromia.tools.docs.fetcher

import org.chromia.App.Companion.logger
import org.chromia.ifNotEmpty
import org.chromia.safeDelete
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.div

class GitRepositoryFetcher(
    val docsSettings: DocumentationSettings
) {
    val tempDir by lazy { createTempDirectory(docsSettings.tempDirPrefix) }

    fun fetchDocs(repo: DocumentationRepository) = runCatching {
        val repoPath = fetchRepository(repo)
        repo.subdirectories.takeIf {
            it.isNotEmpty()
        }?.let { cleanUnnecessaryDirs(it, repoPath) }
    }.getOrElse { throwable -> logger.error(throwable.message) }

    private fun cleanUnnecessaryDirs(allowedSubDirs: List<String>, repoPath: Path) {
        allowedSubDirs.ifNotEmpty {
            repoPath.toFile().listFiles { file ->
                file.name !in allowedSubDirs
            }.forEach { it.safeDelete() }
        }
    }

    private fun fetchRepository(repo: DocumentationRepository): Path {
        val repoPath = (tempDir / repo.name)
        logger.info("Fetching repository ${repo.name} into ${repoPath.absolutePathString()}")
        val processBuilder = ProcessBuilder(
            "git",
            "clone",
            "--branch",
            repo.branch,
            "--depth",
            "1",
            repo.url,
            repoPath.absolutePathString()
        )

        val process = processBuilder.start()
        val exitCode = process.waitFor()
        require(exitCode == 0) {
            val error = process.errorStream.bufferedReader().readText()
            "Failed to clone repository: ${repo.url} @ ${repo.branch} -> $error"
        }
        return repoPath
    }
}
