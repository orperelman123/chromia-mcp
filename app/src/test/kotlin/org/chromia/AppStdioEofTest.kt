package org.chromia

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * Regression for the zombie-JVM QA finding: when the MCP client goes away and
 * stdin reaches EOF, runStdioMcpServer must return (so main can exit) instead
 * of waiting forever on Server.onClose, which only fires on an explicit
 * Server.close().
 */
class AppStdioEofTest {

    @Test
    fun stdioServerReturnsWhenStdinReachesEofMidSession() = runBlocking {
        val clientToServer = PipedOutputStream()
        val serverIn = PipedInputStream(clientToServer, 64 * 1024)
        val serverOut = ByteArrayOutputStream()

        val app = McpTestSupport.testApp()
        val serverRun = async(Dispatchers.IO) {
            app.runStdioMcpServer(
                inputStream = serverIn.asSource().buffered(),
                outputStream = serverOut.asSink().buffered(),
            )
        }

        // Speak a bit of protocol first so EOF lands on a live session, the
        // way a real client disconnect does.
        val initialize =
            """{"jsonrpc":"2.0","id":1,"method":"initialize","params":""" +
                """{"protocolVersion":"2024-11-05","capabilities":{},""" +
                """"clientInfo":{"name":"eof-test","version":"0"}}}""" + "\n"
        clientToServer.write(initialize.toByteArray())
        clientToServer.flush()
        clientToServer.close() // client is gone -> stdin EOF

        withTimeout(15_000) { serverRun.await() }
    }

    @Test
    fun stdioServerReturnsOnImmediateEof() = runBlocking {
        val app = McpTestSupport.testApp()
        val serverRun = async(Dispatchers.IO) {
            app.runStdioMcpServer(
                inputStream = ByteArrayInputStream(ByteArray(0)).asSource().buffered(),
                outputStream = ByteArrayOutputStream().asSink().buffered(),
            )
        }
        withTimeout(15_000) { serverRun.await() }
    }
}
