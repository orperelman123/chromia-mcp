package org.chromia

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Regression for the exit-code QA finding: startup failures used to fall out
 * of main() normally, so the process exited 0 and supervisors/CI thought the
 * server was fine. runMain must report non-zero for every startup failure.
 */
class AppStartupExitCodeTest {

    @Test
    fun invalidSsePortReturnsNonZero() {
        val code = runMain(arrayOf("--sse", "--port", "not-a-number")) { McpTestSupport.testApp() }
        assertNotEquals(0, code, "invalid --port must not exit 0")
    }

    @Test
    fun danglingSseArgReturnsNonZero() {
        val code = runMain(arrayOf("--sse", "--port")) { McpTestSupport.testApp() }
        assertNotEquals(0, code, "odd argument pairs must not exit 0")
    }

    @Test
    fun unknownCommandReturnsNonZero() {
        val code = runMain(arrayOf("--bogus")) { McpTestSupport.testApp() }
        assertNotEquals(0, code, "unknown command argument must not exit 0")
    }

    @Test
    fun unbindableHostReturnsNonZero() {
        // 192.0.2.1 is RFC 5737 TEST-NET-1: never assigned to a local
        // interface, so binding it fails deterministically without DNS.
        val code = runMain(arrayOf("--sse", "--host", "192.0.2.1", "--port", "3999")) {
            McpTestSupport.testApp()
        }
        assertEquals(1, code, "bind failure must exit 1")
    }
}
