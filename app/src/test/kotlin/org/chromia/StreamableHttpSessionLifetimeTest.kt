package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A Streamable HTTP session is not pinned to an open socket - it survives
 * between requests, which is the whole point of it - and each one holds a full
 * `Server` with the ~70-tool registry. On the endpoint this feature exists for
 * (a public, often no-auth connector URL) that makes `initialize` an
 * unauthenticated allocation: without a bound, anyone who can reach the URL can
 * mint sessions until the JVM dies. The SSE endpoint never had this shape,
 * so the bound came in with the new transport, not after it.
 *
 * Two mechanisms, both exercised here: sessions idle past
 * `CHROMIA_MCP_HTTP_SESSION_IDLE_MS` are closed and dropped when the next one is
 * minted, and a mint that would exceed `CHROMIA_MCP_HTTP_MAX_SESSIONS` is
 * refused with 503 rather than served.
 */
class StreamableHttpSessionLifetimeTest {

    private val initBody =
        """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26",""" +
            """"capabilities":{},"clientInfo":{"name":"lifetime","version":"1"}}}"""

    private suspend fun HttpClient.initialize(url: String): io.ktor.client.statement.HttpResponse =
        post(url) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, "application/json, text/event-stream")
            setBody(initBody)
        }

    @Test
    fun idleSessionsAreReclaimedAndTheTableIsCapped() = runBlocking {
        val app = McpTestSupport.testApp()
        val sessions = ConcurrentHashMap<String, App.StreamableSession>()
        val server = embeddedServer(ServerCIO, host = "127.0.0.1", port = 0) {
            with(app) {
                installMcpJson()
                installMcpStreamableHttp(
                    compact = false,
                    disabled = emptySet(),
                    sessions = sessions,
                    idleMillis = 250,
                    maxSessions = 2
                )
            }
        }.start(wait = false)
        val http = HttpClient(CIO) {
            // No HttpTimeout means "wait forever", which in a 45-minute task budget
            // turns one stalled endpoint into a deleted-results suite timeout.
            install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        }
        try {
            val port = server.engine.resolvedConnectors().first().port
            val url = "http://127.0.0.1:$port/mcp"

            val first = http.initialize(url)
            assertEquals(HttpStatusCode.OK, first.status)
            val firstId = first.headers[App.MCP_SESSION_ID_HEADER]
            assertNotNull(firstId)
            assertEquals(1, sessions.size)

            val second = http.initialize(url)
            assertEquals(HttpStatusCode.OK, second.status)
            assertEquals(2, sessions.size)

            // The cap is reached and nothing is idle yet: refuse, do not serve.
            val third = http.initialize(url)
            assertEquals(HttpStatusCode.ServiceUnavailable, third.status)
            assertTrue(third.bodyAsText().contains("too many MCP sessions"), third.bodyAsText())
            assertEquals(2, sessions.size, "a refused mint must not grow the table")

            // Let both go idle; the next mint reclaims them first, then succeeds.
            kotlinx.coroutines.delay(400)
            val fourth = http.initialize(url)
            assertEquals(HttpStatusCode.OK, fourth.status)
            val fourthId = fourth.headers[App.MCP_SESSION_ID_HEADER]
            assertNotNull(fourthId)
            assertEquals(setOf(fourthId), sessions.keys.toSet(), "the two idle sessions must be gone")

            // A reclaimed id is no longer a session.
            val afterReap = http.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json, text/event-stream")
                header(App.MCP_SESSION_ID_HEADER, firstId!!)
                setBody("""{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""")
            }
            assertEquals(HttpStatusCode.NotFound, afterReap.status)
        } finally {
            http.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun aLiveSessionIsNotReclaimedWhileItIsBeingUsed() = runBlocking {
        val app = McpTestSupport.testApp()
        val sessions = ConcurrentHashMap<String, App.StreamableSession>()
        val server = embeddedServer(ServerCIO, host = "127.0.0.1", port = 0) {
            with(app) {
                installMcpJson()
                installMcpStreamableHttp(
                    compact = false,
                    disabled = emptySet(),
                    sessions = sessions,
                    idleMillis = 400,
                    maxSessions = 8
                )
            }
        }.start(wait = false)
        val http = HttpClient(CIO) {
            // No HttpTimeout means "wait forever", which in a 45-minute task budget
            // turns one stalled endpoint into a deleted-results suite timeout.
            install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        }
        try {
            val port = server.engine.resolvedConnectors().first().port
            val url = "http://127.0.0.1:$port/mcp"

            val init = http.initialize(url)
            val id = init.headers[App.MCP_SESSION_ID_HEADER]!!

            // Keep talking to it across more than one idle window.
            repeat(4) {
                kotlinx.coroutines.delay(150)
                val listed = http.post(url) {
                    contentType(ContentType.Application.Json)
                    header(HttpHeaders.Accept, "application/json, text/event-stream")
                    header(App.MCP_SESSION_ID_HEADER, id)
                    setBody("""{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""")
                }
                assertEquals(HttpStatusCode.OK, listed.status, "a session in use must stay alive")
            }
            // Minting another session must not sweep the one that is in use.
            http.initialize(url)
            assertTrue(id in sessions.keys, "an active session was reclaimed")

            // DELETE is still the explicit way out.
            assertEquals(HttpStatusCode.OK, http.delete(url) { header(App.MCP_SESSION_ID_HEADER, id) }.status)
            assertTrue(id !in sessions.keys)
        } finally {
            http.close()
            server.stop(500, 1000)
        }
    }

    @Test
    fun reaperClosesTheTransportAndReportsWhatItTook() = runBlocking {
        val app = McpTestSupport.testApp()
        val sessions = ConcurrentHashMap<String, App.StreamableSession>()
        val fresh = App.StreamableSession(
            StreamableHttpServerTransport(StreamableHttpServerTransport.Configuration(enableJsonResponse = true))
        )
        val stale = App.StreamableSession(
            StreamableHttpServerTransport(StreamableHttpServerTransport.Configuration(enableJsonResponse = true))
        )
        sessions["fresh"] = fresh
        sessions["stale"] = stale

        // Nothing is idle yet.
        assertEquals(0, app.reapIdleStreamableSessions(sessions, idleMillis = 60_000))
        assertEquals(2, sessions.size)

        // Both are touched, then judged against an explicit clock: at `now`
        // neither is idle, 60s later both are. Passing nowMs keeps this a test of
        // the rule rather than of how fast the machine runs.
        fresh.touch()
        stale.touch()
        val now = System.currentTimeMillis()
        assertEquals(0, app.reapIdleStreamableSessions(sessions, idleMillis = 60_000, nowMs = now))
        assertEquals(2, sessions.size)
        assertEquals(2, app.reapIdleStreamableSessions(sessions, idleMillis = 60_000, nowMs = now + 60_001))
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun boundsComeFromTheEnvironmentWithSaneDefaults() {
        assertEquals(App.DEFAULT_HTTP_SESSION_IDLE_MS, App.httpSessionIdleMillis(emptyMap()))
        assertEquals(App.DEFAULT_HTTP_MAX_SESSIONS, App.httpMaxSessions(emptyMap()))
        assertEquals(1234L, App.httpSessionIdleMillis(mapOf(App.HTTP_SESSION_IDLE_MS_ENV to "1234")))
        assertEquals(7, App.httpMaxSessions(mapOf(App.HTTP_MAX_SESSIONS_ENV to "7")))
        // Nonsense must not disable the bound.
        assertEquals(App.DEFAULT_HTTP_SESSION_IDLE_MS, App.httpSessionIdleMillis(mapOf(App.HTTP_SESSION_IDLE_MS_ENV to "0")))
        assertEquals(App.DEFAULT_HTTP_MAX_SESSIONS, App.httpMaxSessions(mapOf(App.HTTP_MAX_SESSIONS_ENV to "-1")))
        assertEquals(App.DEFAULT_HTTP_MAX_SESSIONS, App.httpMaxSessions(mapOf(App.HTTP_MAX_SESSIONS_ENV to "lots")))
    }
}
