package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Regression for the CORS QA finding: the CORS plugin was installed with no
 * allowed hosts, so EVERY browser request (preflight included) was rejected
 * with 403 and no browser-based MCP client could connect.
 */
class AppCorsTest {

    @Test
    fun preflightAndSimpleRequestsAllowedForAnyOriginByDefault() = runBlocking {
        val app = McpTestSupport.testApp()
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false, authToken = null, allowedOrigins = null)
        try {
            val port = server.engine.resolvedConnectors().first().port
            HttpClient(CIO).use { http ->
                val preflight = http.options("http://127.0.0.1:$port/sse") {
                    header(HttpHeaders.Origin, "https://inspector.example")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                }
                assertEquals(HttpStatusCode.OK, preflight.status, "preflight must succeed")
                assertEquals("*", preflight.headers[HttpHeaders.AccessControlAllowOrigin])
                assertNull(
                    preflight.headers[HttpHeaders.AccessControlAllowCredentials],
                    "wildcard origin must never be combined with credentials"
                )

                val get = http.get("http://127.0.0.1:$port/health") {
                    header(HttpHeaders.Origin, "https://inspector.example")
                }
                assertEquals(HttpStatusCode.OK, get.status)
                assertEquals("*", get.headers[HttpHeaders.AccessControlAllowOrigin])
            }
        } finally {
            server.stop(100, 500)
        }
    }

    @Test
    fun allowedOriginsListRestrictsBrowserOrigins() = runBlocking {
        val app = McpTestSupport.testApp()
        val server = app.runSseMcpServer(
            host = "127.0.0.1",
            port = 0,
            wait = false,
            authToken = null,
            allowedOrigins = "https://app.example.com, http://localhost:5173"
        )
        try {
            val port = server.engine.resolvedConnectors().first().port
            HttpClient(CIO).use { http ->
                val allowed = http.options("http://127.0.0.1:$port/sse") {
                    header(HttpHeaders.Origin, "https://app.example.com")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                }
                assertEquals(HttpStatusCode.OK, allowed.status)
                assertEquals("https://app.example.com", allowed.headers[HttpHeaders.AccessControlAllowOrigin])

                val localhost = http.options("http://127.0.0.1:$port/sse") {
                    header(HttpHeaders.Origin, "http://localhost:5173")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                }
                assertEquals(HttpStatusCode.OK, localhost.status)
                assertEquals("http://localhost:5173", localhost.headers[HttpHeaders.AccessControlAllowOrigin])

                val denied = http.options("http://127.0.0.1:$port/sse") {
                    header(HttpHeaders.Origin, "https://evil.example")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                }
                assertEquals(HttpStatusCode.Forbidden, denied.status, "unlisted origin must be rejected")
                assertNull(denied.headers[HttpHeaders.AccessControlAllowOrigin])
            }
        } finally {
            server.stop(100, 500)
        }
    }

    @Test
    fun preflightSucceedsEvenWhenBearerAuthIsEnabled() = runBlocking {
        val app = McpTestSupport.testApp()
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false, authToken = "sekrit", allowedOrigins = null)
        try {
            val port = server.engine.resolvedConnectors().first().port
            HttpClient(CIO).use { http ->
                // Browsers never attach Authorization to the preflight itself;
                // CORS must answer it before the bearer check runs.
                val preflight = http.options("http://127.0.0.1:$port/sse") {
                    header(HttpHeaders.Origin, "https://inspector.example")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                    header(HttpHeaders.AccessControlRequestHeaders, "Authorization")
                }
                assertEquals(HttpStatusCode.OK, preflight.status)
                assertEquals("*", preflight.headers[HttpHeaders.AccessControlAllowOrigin])
            }
        } finally {
            server.stop(100, 500)
        }
    }
}
