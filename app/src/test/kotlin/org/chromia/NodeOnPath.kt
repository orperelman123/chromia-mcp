package org.chromia

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * A node that actually RUNS, found by trying every one on `PATH` until one
 * answers `--version`.
 *
 * Not paranoia, and not a convenience: a bare `ProcessBuilder("node")` in the
 * Gradle test JVM once started something that printed nothing on either stream
 * and exited without an exception, so the test reported *"the tally printed no
 * JSON"* for a tally that was never executed. Windows keeps an
 * App-Execution-Alias stub called `node.exe` in `WindowsApps` for machines with
 * no Node installed, and it behaves exactly like that. Proving the interpreter
 * before blaming the script is the difference between a diagnosis and a guess.
 *
 * [UpstreamWarningGateTest] discovered that and carries the original; the
 * round-20 gate probes need the same answer for the same reason, and a third
 * hand-rolled copy would be a third thing to get subtly wrong. Nothing here
 * stands in for node - it LOCATES the real one and refuses to continue without
 * it, because the merge gate's classifier runs on node and a box that cannot
 * run it cannot verify the decision that gates its own pushes.
 */
object NodeOnPath {

    /** The absolute path of a node that answered `--version`. */
    val executable: String by lazy {
        val tried = mutableListOf<String>()
        val candidates = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .flatMap { dir -> listOf("node.exe", "node").map { File(dir, it) } }
            .filter { it.isFile }
            .map { it.absolutePath }
            .distinct()
        for (candidate in candidates) {
            val probe = ProcessBuilder(candidate, "--version").redirectErrorStream(true).start()
            val said = probe.inputStream.bufferedReader().readText().trim()
            probe.waitFor(60, TimeUnit.SECONDS)
            if (probe.exitValue() == 0 && said.startsWith("v")) return@lazy candidate
            tried += "$candidate -> exit ${probe.exitValue()}, said ${said.ifBlank { "<nothing>" }}"
        }
        throw AssertionError(
            "no working node on PATH, and the merge gate's tally runs on node - so this box cannot " +
                "verify the classification that decides its own pushes. Tried ${candidates.size} " +
                "candidate(s):\n  " + tried.joinToString("\n  ")
        )
    }

    /** What one run of a script said: its exit code and both streams. */
    data class Ran(val exit: Int, val stdout: String, val stderr: String) {
        /** Everything the run printed, for an assertion message that diagnoses. */
        fun transcript(): String =
            "exit $exit\n--- stdout ---\n${stdout.ifBlank { "<nothing>" }}\n" +
                "--- stderr ---\n${stderr.ifBlank { "<nothing>" }}"
    }

    /**
     * Runs `node <script> <args...>` from [workingDir] and waits for it. The
     * gate cannot hang - it is the thing that decides whether a push happens -
     * so a run that has not finished in two minutes is an assertion failure
     * rather than a test that never returns.
     */
    fun exec(script: File, args: List<String>, workingDir: File): Ran {
        val command = listOf(executable, script.absolutePath) + args
        val process = ProcessBuilder(command).directory(workingDir).start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        if (!process.waitFor(120, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw AssertionError(
                "${script.name} did not finish in 120 s. The gate cannot hang: it is the step that " +
                    "decides a merge.\n  command: ${command.joinToString(" ")}\n  cwd: $workingDir"
            )
        }
        return Ran(process.exitValue(), stdout, stderr)
    }
}
