package org.chromia

import org.chromia.tools.docs.fetcher.awaitProcess
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AwaitProcessTest {

    private val isWindows = System.getProperty("os.name").lowercase().contains("win")

    private fun echoCommand(text: String): List<String> =
        if (isWindows) listOf("cmd", "/c", "echo", text) else listOf("echo", text)

    // ping -n waits ~1s per echo request, giving a portable long-running process.
    private fun longRunningCommand(): List<String> =
        if (isWindows) listOf("cmd", "/c", "ping", "-n", "30", "127.0.0.1") else listOf("sleep", "20")

    @Test
    fun capturesSuccessfulOutput() {
        val process = ProcessBuilder(echoCommand("ok-from-git")).redirectErrorStream(true).start()
        val output = awaitProcess(process, 10, "echo")
        assertTrue(output.contains("ok-from-git"))
    }

    @Test
    fun timesOutLongRunningProcess() {
        val process = ProcessBuilder(longRunningCommand()).redirectErrorStream(true).start()
        assertThrows(IllegalStateException::class.java) {
            awaitProcess(process, 1, "long-running")
        }
    }
}
