package org.chromia

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Locates the repository from a running test, for the handful of tests that
 * assert over the repo's own sources (the mock ledger, the assumption ledger,
 * the documentation-consistency checks). Gradle's working directory for `test`
 * is `app/`, so nothing may assume a cwd - walk up to the marker files instead.
 */
object RepoFiles {

    val root: Path by lazy {
        var dir: Path? = Path.of("").toAbsolutePath()
        while (dir != null) {
            if (Files.isRegularFile(dir.resolve("settings.gradle.kts")) &&
                Files.isDirectory(dir.resolve("scripts"))
            ) return@lazy dir
            dir = dir.parent
        }
        error("repo root (settings.gradle.kts + scripts/) not found above ${Path.of("").toAbsolutePath()}")
    }

    val testSourceRoot: Path get() = root.resolve("app/src/test/kotlin")

    /** Every .kt file under app/src/test/kotlin, sorted by name for stable messages. */
    fun testSources(): List<Path> =
        Files.walk(testSourceRoot).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .sorted()
                .toList()
        }

    fun text(relative: String): String = root.resolve(relative).readText()

    fun exists(relative: String): Boolean = Files.exists(root.resolve(relative))

    /** The simple class name a test source file declares (its file name). */
    fun className(file: Path): String = file.fileName.toString().removeSuffix(".kt")
}
