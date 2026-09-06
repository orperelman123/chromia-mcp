package org.chromia

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * DNS-rebinding protection on both HTTP transports.
 *
 * kotlin-sdk 0.13 turned this on by default for its own HTTP routes, but its
 * allow-list is exact hostnames, which cannot express the random
 * `<words>.trycloudflare.com` a quick tunnel mints on every start - the exact
 * deployment serve-public.ps1 exists for. The server therefore runs its own gate
 * ([App.hostAllowed]) with suffix wildcards, on the SSE endpoints too, which had
 * no `Host` validation at all before.
 *
 * The attack this stops: a page on evil.com whose DNS is re-pointed at 127.0.0.1
 * can make the victim's browser POST to a local MCP server. It cannot forge the
 * `Host` header - the browser sends `evil.com` - so the request is refused before
 * any tool runs, whatever CORS is configured to allow.
 */
class AppHostAllowListTest {

    @Test
    fun defaultsCoverLoopbackAndACloudflareQuickTunnel() {
        val allowed = App.hostAllowList(configured = null, bindHost = "127.0.0.1")
        assertTrue(App.hostAllowed("localhost:3001", allowed))
        assertTrue(App.hostAllowed("127.0.0.1:3001", allowed))
        assertTrue(App.hostAllowed("127.0.0.1", allowed))
        assertTrue(App.hostAllowed("[::1]:3001", allowed))
        assertTrue(App.hostAllowed("LOCALHOST:3001", allowed), "Host matching is case-insensitive")
        // The whole point of the wildcard: the name is minted per tunnel start.
        assertTrue(App.hostAllowed("wide-fox-42.trycloudflare.com", allowed))
        assertTrue(App.hostAllowed("another-one.trycloudflare.com:443", allowed))

        assertFalse(App.hostAllowed("evil.com", allowed))
        assertFalse(App.hostAllowed("evil.com:3001", allowed))
        // Anchored at the END, so a suffix cannot be used as a prefix.
        assertFalse(App.hostAllowed("trycloudflare.com.evil.net", allowed))
        // ... and the bare apex is not one of the tunnel names.
        assertFalse(App.hostAllowed("trycloudflare.com", allowed))
        // A missing or unparseable Host is not a pass.
        assertFalse(App.hostAllowed(null, allowed))
        assertFalse(App.hostAllowed("", allowed))
        assertFalse(App.hostAllowed("evil.com/localhost", allowed))
    }

    @Test
    fun operatorCanNameACustomDomainAndTheBindAddress() {
        val allowed = App.hostAllowList(configured = "mcp.example.com, *.corp.internal", bindHost = "192.168.1.20")
        assertTrue(App.hostAllowed("mcp.example.com", allowed))
        assertTrue(App.hostAllowed("a.corp.internal:8080", allowed))
        assertTrue(App.hostAllowed("192.168.1.20:3001", allowed), "the bound interface is allowed without a second setting")
        assertTrue(App.hostAllowed("localhost", allowed), "the defaults are never dropped")
        assertFalse(App.hostAllowed("other.example.com", allowed))

        // A wildcard bind address is not itself a Host anyone sends.
        assertFalse("0.0.0.0" in App.hostAllowList(configured = null, bindHost = "0.0.0.0"))
    }

    @Test
    fun starDisablesTheCheck() {
        val allowed = App.hostAllowList(configured = "*", bindHost = "127.0.0.1")
        assertTrue(App.hostAllowed("anything.at.all", allowed))
        assertTrue(App.hostAllowed(null, allowed))
    }

    /**
     * Spoken over a raw socket rather than through the ktor client: the header
     * under test is the one an HTTP client library owns and rewrites, and a test
     * that lets the engine set `Host` would not be testing anything.
     */
    @Test
    fun aForeignHostIsRefusedOnBothTransportsButNotOnHealth() = runBlocking {
        val app = McpTestSupport.testApp()
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false)
        try {
            val port = server.engine.resolvedConnectors().first().port

            val initBody =
                """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26",""" +
                    """"capabilities":{},"clientInfo":{"name":"rebind","version":"1"}}}"""

            assertEquals(
                403,
                statusOf(
                    port, "POST", "/mcp", "evil.com", initBody,
                    listOf("content-type: application/json", "accept: application/json, text/event-stream")
                ),
                "POST /mcp with a foreign Host must be refused before any tool runs"
            )
            assertEquals(
                403,
                statusOf(
                    port, "POST", "/?sessionId=whatever", "evil.com",
                    """{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}""",
                    listOf("content-type: application/json")
                ),
                "the legacy SSE POST endpoint must be refused too"
            )
            assertEquals(
                403,
                statusOf(port, "GET", "/", "evil.com", null, listOf("accept: text/event-stream")),
                "opening the SSE stream must be refused too"
            )

            // /health is exempt: an IP-addressed load-balancer probe must keep
            // working, and it exposes no tools.
            assertEquals(200, statusOf(port, "GET", "/health", "evil.com", null, emptyList()))

            // A legitimate client is unaffected.
            assertEquals(200, statusOf(port, "GET", "/health", "127.0.0.1:$port", null, emptyList()))
            assertEquals(
                200,
                statusOf(
                    port, "POST", "/mcp", "127.0.0.1:$port", initBody,
                    listOf("content-type: application/json", "accept: application/json, text/event-stream")
                )
            )
        } finally {
            server.stop(500, 1000)
        }
    }

    /** Sends one hand-written HTTP/1.1 request and returns the status code. */
    private fun statusOf(
        port: Int,
        method: String,
        path: String,
        hostHeader: String,
        body: String?,
        headers: List<String>
    ): Int = java.net.Socket("127.0.0.1", port).use { socket ->
        socket.soTimeout = 15_000
        val bytes = body?.toByteArray()
        val request = buildString {
            append("$method $path HTTP/1.1\r\n")
            append("Host: $hostHeader\r\n")
            headers.forEach { append("$it\r\n") }
            append("Content-Length: ${bytes?.size ?: 0}\r\n")
            append("Connection: close\r\n")
            append("\r\n")
        }
        socket.getOutputStream().apply {
            write(request.toByteArray())
            if (bytes != null) write(bytes)
            flush()
        }
        val statusLine = socket.getInputStream().bufferedReader().readLine()
            ?: error("no response to $method $path (Host: $hostHeader)")
        statusLine.split(" ").getOrNull(1)?.toIntOrNull()
            ?: error("unparseable status line: $statusLine")
    }
}
