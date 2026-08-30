package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.cio.CIO as ServerCIO
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppHealthRouteTest {

    @Test
    fun getHealthReturnsHealthJsonWithoutNetwork() = runBlocking {
        val server = embeddedServer(ServerCIO, host = "127.0.0.1", port = 0) {
            installHealthEndpoint()
        }.start(wait = false)
        val client = HttpClient(CIO)
        try {
            val port = server.engine.resolvedConnectors().first().port
            val response = client.get("http://127.0.0.1:$port/health")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
            val body = response.bodyAsText()
            assertEquals(App.healthJson(), body)
            assertTrue(body.contains("\"status\": \"healthy\"") || body.contains("\"status\":\"healthy\""))
        } finally {
            client.close()
            server.stop(500, 1000)
        }
    }
}
