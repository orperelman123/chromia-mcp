package org.chromia

import org.chromia.tools.docs.fetcher.awaitProcess
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AwaitProcessTest {

    @Test
    fun capturesSuccessfulOutput() {
        val process = ProcessBuilder("echo", "ok-from-git").redirectErrorStream(true).start()
        val output = awaitProcess(process, 10, "echo")
        assertTrue(output.contains("ok-from-git"))
    }

    @Test
    fun timesOutLongRunningProcess() {
        val process = ProcessBuilder("sleep", "20").redirectErrorStream(true).start()
        assertThrows(IllegalStateException::class.java) {
            awaitProcess(process, 1, "sleep")
        }
    }
}
