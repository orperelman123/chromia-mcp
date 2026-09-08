package org.chromia

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

/**
 * Locates the repository from a running test, for the handful of tests that
 * assert over the repo's own sources (the mock ledger, the assumption ledger,
 * the documentation-consistency checks). Gradle's working directory for `test`
 * is `app/`, so nothing may assume a cwd - walk up to the marker files instead.
 *
 * It also answers WHERE THE TEST RUNTIME LOADS CLASSES FROM, and that answer is
 * Gradle's, not ours. Adversary round 19 (finding r19d5) walked past the
 * zero-doubles scan by pointing at a directory the scan had been told about in a
 * literal: `classesRoot` was `app/build/classes/kotlin`, so the java plugin's
 * conventional `app/src/test/java` output and `app/build/resources/test` - both
 * on the test runtime classpath - were outside it, and a double written in Java
 * or checked in as bytes was proven absent by nothing. A hard-coded list cannot
 * notice a source set that did not exist when it was written, so the `test` task
 * hands its own classpath and its own production output to the JVM as system
 * properties (the `chromia.test.*` block in `app/build.gradle.kts`) and
 * [testRuntimeClasspath] reads them back.
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

    /**
     * The test source set's source roots. `app/src/test/java` is the java
     * plugin's own convention and `app/build.gradle.kts` never redefines it, so
     * it is a source root whether or not anyone has yet put a file in it - which
     * is exactly why it is named here rather than discovered by looking.
     */
    val testSourceRoots: List<Path>
        get() = listOf(testSourceRoot, root.resolve("app/src/test/java"))

    /**
     * Every test source file, sorted by path for stable messages. `.java` is in
     * the filter because the test source set COMPILES it: a scan that reads only
     * `.kt` is blind to a double written in the other language of the same source
     * set, which is how adversary round 19 (r19d5) proposed to hide one.
     */
    fun testSources(): List<Path> =
        testSourceRoots.filter { Files.isDirectory(it) }
            .flatMap { sourceRoot ->
                Files.walk(sourceRoot).use { stream ->
                    stream.filter { file ->
                        Files.isRegularFile(file) &&
                            (file.toString().endsWith(".kt") || file.toString().endsWith(".java"))
                    }.toList()
                }
            }
            .sortedBy { it.toString() }

    fun text(relative: String): String = root.resolve(relative).readText()

    fun exists(relative: String): Boolean = Files.exists(root.resolve(relative))

    /** The simple class name a test source file declares (its file name). */
    fun className(file: Path): String =
        file.fileName.toString().removeSuffix(".kt").removeSuffix(".java")

    // ---- what the test JVM can load a class from ---------------------------

    /**
     * The `test` task's own runtime classpath, as Gradle resolved it, or null
     * when this JVM was not launched by that task. Never a list this file
     * maintains: a new source set has to appear here the day it is added.
     */
    val testRuntimeClasspath: List<Path>?
        get() = pathsOf("chromia.test.runtime.classpath")

    /**
     * The `main` source set's whole output - classes and resources. It is on the
     * test runtime classpath too, and it is PRODUCTION: a scan looking for
     * substitutes reads it as what a substitute stands in FOR, never as a tree to
     * search for one.
     */
    val productionOutput: List<Path>?
        get() = pathsOf("chromia.test.production.output")

    /** The `main` source set's compiled classes (Kotlin's and Java's). */
    val productionClasses: List<Path>?
        get() = pathsOf("chromia.test.production.classes")

    /**
     * The `doubleProbes` source set's compiled classes. Deliberately NOT on the
     * test runtime classpath - the probes are compiled and never run - so the
     * task passes the directory separately.
     */
    val doubleProbeClasses: List<Path>?
        get() = pathsOf("chromia.test.doubleprobes.classes")

    private fun pathsOf(property: String): List<Path>? =
        System.getProperty(property)
            ?.split(File.pathSeparator)
            ?.filter { it.isNotBlank() }
            ?.map { Path.of(it).toAbsolutePath().normalize() }

    /** A repo-relative, forward-slashed spelling of [path], for messages and evidence. */
    fun relative(path: Path): String =
        runCatching { root.relativize(path.toAbsolutePath().normalize()).toString() }
            .getOrDefault(path.toString())
            .replace('\\', '/')
}
