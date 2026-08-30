package org.chromia.tools.docs.fetcher

import org.chromia.App.Companion.logger
import org.chromia.safeDelete
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class GitRepositoryFetcher(
    val docsSettings: DocumentationSettings
) {
    val tempDir by lazy { createTempDirectory(docsSettings.tempDirPrefix) }

    fun fetchDocs(repo: DocumentationRepository) = runCatching {
        val repoPath = fetchRepository(repo)
        verifyConfiguredPaths(repo, repoPath)
    }.getOrElse { throwable -> logger.error(throwable.message) }

    private fun fetchRepository(repo: DocumentationRepository): Path {
        val repoPath = (tempDir / repo.name)
        logger.info("Fetching repository ${repo.name} into ${repoPath.absolutePathString()}")
        return runCatching {
            sparseClone(repo, repoPath)
            pruneToConfiguredPaths(repo, repoPath)
            repoPath
        }.recoverCatching { error ->
            logger.warn("Sparse checkout failed for ${repo.name}, falling back to full clone: ${error.message}")
            if (repoPath.toFile().exists()) {
                repoPath.toFile().safeDelete()
            }
            fullClone(repo, repoPath)
            pruneToConfiguredPaths(repo, repoPath)
            repoPath
        }.getOrThrow()
    }

    private fun sparseClone(repo: DocumentationRepository, repoPath: Path): Path {
        runGit(
            listOf(
                "git", "clone",
                "--filter=blob:none",
                "--sparse",
                "--branch", repo.branch,
                "--depth", "1",
                repo.url,
                repoPath.absolutePathString()
            ),
            "sparse clone ${repo.name}"
        )
        if (repo.subdirectories.isNotEmpty()) {
            runGit(
                listOf(
                    "git", "-C", repoPath.absolutePathString(),
                    "sparse-checkout", "set", "--cone"
                ) + repo.subdirectories,
                "sparse-checkout ${repo.name}"
            )
        }
        return repoPath
    }

    private fun fullClone(repo: DocumentationRepository, repoPath: Path): Path {
        runGit(
            listOf(
                "git", "clone",
                "--branch", repo.branch,
                "--depth", "1",
                repo.url,
                repoPath.absolutePathString()
            ),
            "clone ${repo.name}"
        )
        return repoPath
    }

    private fun pruneToConfiguredPaths(repo: DocumentationRepository, repoPath: Path) {
        if (repo.subdirectories.isNotEmpty()) {
            DocsPathFilter.keepAllowedPaths(repoPath.toFile(), repo.subdirectories)
        }
    }

    private fun verifyConfiguredPaths(repo: DocumentationRepository, repoPath: Path) {
        repo.subdirectories.forEach { relative ->
            val path = repoPath.resolve(relative)
            if (!path.exists() || !path.isDirectory()) {
                logger.warn("Configured path missing after fetch: ${repo.name}/$relative")
            }
        }
    }

    private fun runGit(command: List<String>, label: String) {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val output = awaitProcess(process, docsSettings.timeoutSeconds, label)
        val exitCode = process.exitValue()
        require(exitCode == 0) {
            "Failed $label (exit $exitCode): $output"
        }
    }
}

internal fun awaitProcess(process: Process, timeoutSeconds: Int, label: String): String {
    val output = StringBuilder()
    val reader = Thread {
        process.inputStream.bufferedReader().use { output.append(it.readText()) }
    }.apply {
        isDaemon = true
        start()
    }
    val finished = process.waitFor(timeoutSeconds.toLong().coerceAtLeast(1), TimeUnit.SECONDS)
    if (!finished) {
        process.destroyForcibly()
        reader.join(1_000)
        throw IllegalStateException("Failed $label (timeout ${timeoutSeconds}s)")
    }
    reader.join()
    return output.toString()
}

internal object IngestPathFilter {
    val allowedExtensions = setOf(
        "md", "mdx", "rst", "txt", "adoc",
        "rell", "kt", "kts", "java",
        "ts", "js", "py",
        "yml", "yaml", "json",
        "html", "xml", "properties",
        "api", "fbs", "g4"
    )

    fun accept(path: Path): Boolean {
        val name = path.fileName?.toString() ?: return false
        if (name.startsWith(".")) return false
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in allowedExtensions
    }

    fun pathMatcher(): java.nio.file.PathMatcher = java.nio.file.PathMatcher { path -> accept(path) }
}

internal object DocsPathFilter {
    fun keepAllowedPaths(repoRoot: File, allowed: List<String>) {
        val normalized = allowed.map { it.trim().trim('/').replace('\\', '/') }.filter { it.isNotEmpty() }
        if (normalized.isEmpty() || !repoRoot.isDirectory) return
        prune(repoRoot, normalized, prefix = "")
    }

    private fun prune(dir: File, allowed: List<String>, prefix: String) {
        dir.listFiles()?.forEach { child ->
            if (child.name == ".git") {
                child.safeDelete()
                return@forEach
            }
            val rel = if (prefix.isEmpty()) child.name else "$prefix/${child.name}"
            val isAllowedOrDescendant = allowed.any { it == rel || rel.startsWith("$it/") }
            val isAncestor = allowed.any { it.startsWith("$rel/") }
            when {
                isAllowedOrDescendant -> Unit
                isAncestor && child.isDirectory -> prune(child, allowed, rel)
                else -> child.safeDelete()
            }
        }
    }
}
