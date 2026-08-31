package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppAuthTokenTest {

    @Test
    fun bearerTokenGuardsEverythingExceptHealth() = runBlocking {
        val app = App()
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false, authToken = "sekrit")
        try {
            val port = server.engine.resolvedConnectors().first().port
            val http = HttpClient(CIO)
            http.use {
                assertEquals(HttpStatusCode.OK, it.get("http://127.0.0.1:$port/health").status, "/health must stay open")
                assertEquals(HttpStatusCode.Unauthorized, it.get("http://127.0.0.1:$port/").status, "no token must be rejected")
                assertEquals(
                    HttpStatusCode.Unauthorized,
                    it.get("http://127.0.0.1:$port/") { header(HttpHeaders.Authorization, "Bearer wrong") }.status,
                    "wrong token must be rejected"
                )
            }
        } finally {
            server.stop(100, 500)
        }
    }

    @Test
    fun noTokenConfiguredMeansOpenServer() = runBlocking {
        val app = App()
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false, authToken = null)
        try {
            val port = server.engine.resolvedConnectors().first().port
            HttpClient(CIO).use {
                assertEquals(HttpStatusCode.OK, it.get("http://127.0.0.1:$port/health").status)
            }
        } finally {
            server.stop(100, 500)
        }
    }
}
