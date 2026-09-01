package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.client.mcpSse
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Client disconnects must not leak SSE sessions. The SDK's `mcp {}` plugin
 * removes a transport from its session map only via `server.onClose`, which
 * fires only on an explicit `server.close()` - never on client disconnect -
 * so every reconnecting client leaked one transport (holding a full Server
 * closure) for the life of the process. The in-app wiring (installMcpSse)
 * removes the entry in a `finally` when ktor cancels the SSE handler
 * (e2e transport probe 2026-09-01).
 */
class SseSessionCleanupTest {

    @Test
    fun disconnectRemovesTransportAndDeadSessionPost404s(): Unit = runBlocking {
        val app = McpTestSupport.testApp()
        val transports = ConcurrentHashMap<String, SseServerTransport>()
        val server = embeddedServer(io.ktor.server.cio.CIO, host = "127.0.0.1", port = 0) {
            with(app) { installMcpSse(compact = false, disabled = emptySet(), transports = transports, heartbeatMillis = 500) }
        }.start(wait = false)
        val http = HttpClient(CIO) { install(SSE) }
        try {
            val port = server.engine.resolvedConnectors().first().port
            val base = "http://127.0.0.1:$port"

            val client = withTimeout(15_000) { http.mcpSse(base) }
            assertEquals(1, transports.size, "connected session must be tracked")
            val sessionId = transports.keys.single()

            runCatching { client.close() }

            val deadline = System.currentTimeMillis() + 10_000
            while (transports.isNotEmpty() && System.currentTimeMillis() < deadline) delay(100)
            assertTrue(
                transports.isEmpty(),
                "transport for disconnected session still tracked after 10s (session leak)"
            )

            // A POST to the dead session must 404 like any unknown session.
            HttpClient(CIO).use { plain ->
                val resp = plain.post("$base/?sessionId=$sessionId") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}""")
                }
                assertEquals(HttpStatusCode.NotFound, resp.status)
            }

            // And the server must stay fully usable for the next client.
            val client2 = withTimeout(15_000) { http.mcpSse(base) }
            assertEquals(1, transports.size, "fresh session must be tracked after a disconnect")
            val listed = withTimeout(10_000) { client2.listTools() }
            assertTrue((listed?.tools ?: emptyList()).isNotEmpty(), "reconnected session must list tools")
            runCatching { client2.close() }
        } finally {
            http.close()
            server.stop(500, 1000)
        }
    }
}
