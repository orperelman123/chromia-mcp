package org.chromia

import org.chromia.tools.RealProcessRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path

/**
 * RealProcessRunner used to read stdout to EOF BEFORE waitFor(timeout), so:
 *  - a hung child (chr waiting on something) kept stdout open and run() never
 *    returned - the timeoutMs parameter was unenforceable dead code and the
 *    child was left orphaned;
 *  - a child flooding stderr past the OS pipe buffer deadlocked against the
 *    parent draining stdout first, even when the child would otherwise exit.
 * Real child JVMs, real pipes - no fakes.
 */
class RealProcessRunnerTest {

    private fun javaCmd(vararg progArgs: String): List<String> {
        val exe = Path.of(
            System.getProperty("java.home"), "bin",
            if (System.getProperty("os.name").lowercase().contains("win")) "java.exe" else "java"
        )
        val classpath = listOf(ProcHang::class.java, Unit::class.java)
            .map { Path.of(it.protectionDomain.codeSource.location.toURI()).toString() }
            .distinct()
            .joinToString(File.pathSeparator)
        return listOf(exe.toString(), "-cp", classpath, "org.chromia.ProcHang", *progArgs)
    }

    @Test
    fun timeoutKillsAHangingChildInsteadOfWaitingForItsOutput() {
        val started = System.nanoTime()
        val out = RealProcessRunner.run(javaCmd("sleep", "30000"), Path.of("."), emptyMap(), 2_000)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertEquals(-1, out.exitCode, "expected the timeout marker exit code; got $out")
        assertTrue(out.stderr.contains("timed out"), out.stderr)
        assertTrue(
            elapsedMs < 15_000,
            "run() took ${elapsedMs}ms - the 2000ms timeout was not enforced (stdout was drained " +
                "to EOF before waitFor, so a hung child blocked forever)"
        )
    }

    @Test
    fun largeStderrDoesNotDeadlockAgainstStdoutDraining() {
        val out = RealProcessRunner.run(javaCmd("flood"), Path.of("."), emptyMap(), 60_000)
        assertEquals(0, out.exitCode, "flood child should exit cleanly; stderr: ${out.stderr.take(200)}")
        assertTrue(out.stdout.contains("flood-done"), out.stdout.take(200))
        assertTrue(
            out.stderr.length >= 8192 * 64,
            "expected the full 512KiB stderr flood to be captured; got ${out.stderr.length} chars"
        )
    }
}
