package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Nothing that is TRACKED - and therefore published, the repository is public -
 * may point at the machine it was written on.
 *
 * The review of 2026-09-07 found nineteen tracked files carrying
 * `C:\Users\<this machine's user>\...` (README launcher examples, two docs, a
 * lane script, a KDoc and fourteen adversary harness scripts with a hard-coded
 * worktree root) plus four compiled Python caches whose bytecode embeds the
 * same absolute paths. This test scans exactly what `git ls-files` reports -
 * what a clone contains - so a path, the running user's name, a profile temp
 * directory or a compiled cache cannot be committed again unnoticed.
 *
 * Placeholders are fine and are what the docs use now: `C:\Users\<you>\`,
 * `<home>\`, `<path-to-your-clone>\`.
 */
class NoLocalMachinePathsTest {

    private val forbiddenContent = listOf(
        // A Windows home directory that is neither a placeholder (`<you>`) nor an
        // obviously fictional example user (`dev`, `alice`, ...).
        Regex("""[A-Za-z]:[\\/]+Users[\\/]+(?!<|(?:dev|you|user|alice|bob|example|username)[\\/])[A-Za-z0-9._-]+[\\/]"""),
        Regex("""/(?:c|mnt/c)/Users/(?!<|(?:dev|you|user|alice|bob|example|username)/)[A-Za-z0-9._-]+/"""),
        // A temp / app-data directory of a Windows profile (agent session
        // scratch directories live there too).
        Regex("""AppData[\\/]+(?:Local|Roaming)"""),
    ) + localUserName()

    /**
     * The user name of the machine running the test, read at runtime so the
     * name itself is never written into this public file. Whoever runs the
     * suite is thereby stopped from committing their own name.
     */
    private fun localUserName(): List<Regex> {
        val names = listOfNotNull(System.getProperty("user.name"), System.getenv("USERNAME"), System.getenv("USER"))
            .map { it.trim() }
            .filter { it.length >= 4 && it.lowercase() !in setOf("user", "root", "runner", "admin", "test", "build") }
            .toSet()
        return names.map { Regex("""\b${Regex.escape(it)}\b""") }
    }

    private val forbiddenPaths = listOf(
        Regex("""(^|/)__pycache__/"""),
        Regex("""\.pyc$"""),
        Regex("""(^|/)local-test-env\.properties$"""),
    )

    /** Files the scan reads as text; everything else is checked by path only. */
    private val textExtensions = setOf(
        "kt", "kts", "md", "mjs", "js", "ts", "json", "yml", "yaml", "ps1", "cmd", "sh", "py",
        "rell", "txt", "properties", "toml", "xml", "html", "css", "gradle", "patch", "cfg", "ini"
    )

    private fun repoRoot(): Path {
        var dir: Path? = Path.of("").toAbsolutePath()
        while (dir != null) {
            if (Files.isDirectory(dir.resolve(".git")) || Files.isRegularFile(dir.resolve(".git"))) return dir
            dir = dir.parent
        }
        error("not inside a git checkout")
    }

    private fun trackedFiles(root: Path): List<String> {
        val proc = ProcessBuilder("git", "ls-files", "-z")
            .directory(root.toFile())
            .redirectErrorStream(true)
            .start()
        val out = proc.inputStream.readBytes().toString(StandardCharsets.UTF_8)
        assertEquals(0, proc.waitFor(), "git ls-files failed: $out")
        return out.split('\u0000').filter { it.isNotEmpty() }
    }

    @Test
    fun noTrackedFileNamesOrContainsALocalMachinePath() {
        val root = repoRoot()
        val tracked = trackedFiles(root)
        assertTrue(tracked.size > 100, "expected a full checkout, saw ${tracked.size} tracked files")
        val offenders = mutableListOf<String>()
        for (rel in tracked) {
            forbiddenPaths.firstOrNull { it.containsMatchIn(rel) }?.let { offenders += "$rel  (path matches ${it.pattern})" }
            val ext = rel.substringAfterLast('.', "").lowercase()
            val file: File = root.resolve(rel).toFile()
            if (ext !in textExtensions || !file.isFile) continue
            val text = file.readText(StandardCharsets.UTF_8)
            text.lineSequence().forEachIndexed { i, line ->
                forbiddenContent.firstOrNull { it.containsMatchIn(line) }?.let {
                    offenders += "$rel:${i + 1}: ${line.trim().take(120)}"
                }
            }
        }
        assertEquals(
            emptyList<String>(), offenders,
            "${offenders.size} tracked file(s) point at a local machine; the repository is public:\n" +
                offenders.joinToString("\n")
        )
    }
}
