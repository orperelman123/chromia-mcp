package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.mcpStreamableHttp
import org.chromia.tools.readResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.chromia.tools.McpResources
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Streamable HTTP (`POST/GET/DELETE /mcp`) end to end against the same
 * `--sse` server that serves the root SSE endpoints.
 *
 * ChatGPT's own connector docs still show `/sse/`, but every other current
 * client - and the MCP spec since 2025-03-26 - is on Streamable HTTP, and the
 * SSE transport is the deprecated one. Both must work from one process, on one
 * port, behind the same auth and CORS, or "connect it to ChatGPT" and "connect
 * it to anything else" become two different deployments.
 */
class McpStreamableHttpSessionTest {

    @Test
    fun initializeListsToolsAndCallsGetPromptsOverStreamableHttp() = runBlocking {
        withStreamableHttpSession { client, _ ->
            val serverInfo = client.serverVersion
            assertNotNull(serverInfo)
            assertEquals("chromia-mcp-server", serverInfo!!.name)
            assertEquals(BuildInfo.VERSION, serverInfo.version)
            // serverInfo carries the active profile, so a connector can see what
            // it is talking to without calling a tool.
            assertEquals("Chromia MCP Server (profile: ${App.profile})", serverInfo.title)
            assertEquals(false, client.serverCapabilities?.tools?.listChanged)

            val listed = withTimeout(10_000) { client.listTools() }
            val names = listed.tools.map { it.name }
            assertTrue(names.contains("search"), "tools/list missing search: $names")
            assertTrue(names.contains("fetch"), "tools/list missing fetch: $names")
            assertTrue(names.contains("get_prompts"), "tools/list missing get_prompts: $names")
            assertTrue(names.contains("chromia_help"), "tools/list missing chromia_help: $names")

            val call = withTimeout(10_000) {
                client.callTool(name = "get_prompts", arguments = emptyMap())
            }
            assertEquals(false, call.isError == true)
            val text = (call.content.first() as TextContent).text
            assertTrue(text.contains("prompts"), text)
            assertTrue(call.structuredContent!!.containsKey("prompts"))
            assertTrue(call.structuredContent!!.containsKey("statistics"))
        }
    }

    @Test
    fun readsResourcesOverStreamableHttp() = runBlocking {
        withStreamableHttpSession { client, _ ->
            val listed = withTimeout(10_000) { client.listResources() }
            assertEquals(
                setOf(
                    McpResources.HEALTH_URI,
                    McpResources.DOCS_REPOSITORIES_URI,
                    McpResources.PROMPT_CATALOG_URI
                ),
                listed.resources.map { it.uri }.toSet()
            )
            val read = withTimeout(10_000) {
                client.readResource(readResourceRequest(uri = McpResources.HEALTH_URI))
            }
            val content = read.contents.single() as TextResourceContents
            assertEquals(App.healthJson(), content.text)
        }
    }

    @Test
    fun toolCallReachesTheSameStrategiesAsSse() = runBlocking {
        val fixture = """{"data":{"allBlockchains":[{"rid":"abc","name":"directory_chain","system":true}]}}"""
        val captured = mutableListOf<String>()
        val engine = MockEngine { request ->
            captured.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        withStreamableHttpSession(engine = engine) { client, _ ->
            val call = withTimeout(10_000) {
                client.callTool(
                    name = "filter_blockchains",
                    arguments = mapOf("network" to "testnet", "name" to "directory", "limit" to 5)
                )
            }
            assertEquals(false, call.isError == true)
            val structured = call.structuredContent!!
            assertEquals(
                "directory_chain",
                structured.getValue("data").jsonObject
                    .getValue("allBlockchains").jsonArray[0].jsonObject
                    .getValue("name").jsonPrimitive.content
            )
            val text = (call.content.first() as TextContent).text
            assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
            assertEquals(1, engine.requestHistory.size)
            assertEquals("testnet", engine.requestHistory.first().url.parameters["network"])
            assertEquals(1, captured.size)
        }
    }

    /**
     * The session-id round trip, spoken as raw HTTP rather than through the SDK
     * client: `initialize` answers with `Mcp-Session-Id`, a follow-up POST that
     * echoes it lands on the same session, a POST that does not is a fresh
     * (uninitialized) one, an unknown id 404s, and DELETE ends it.
     */
    @Test
    fun sessionIdIsMintedEchoedAndDeletable() = runBlocking {
        val app = McpTestSupport.testApp()
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false)
        val http = HttpClient(CIO) {
            // No HttpTimeout means "wait forever", which in a 45-minute task budget
            // turns one stalled endpoint into a deleted-results suite timeout.
            install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        }
        try {
            val port = server.engine.resolvedConnectors().first().port
            val url = "http://127.0.0.1:$port/mcp"

            val init = http.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json, text/event-stream")
                setBody(
                    """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26",""" +
                        """"capabilities":{},"clientInfo":{"name":"raw-test","version":"1"}}}"""
                )
            }
            assertEquals(HttpStatusCode.OK, init.status)
            val sessionId = init.headers[App.MCP_SESSION_ID_HEADER]
            assertNotNull(sessionId, "initialize must mint an ${App.MCP_SESSION_ID_HEADER}")
            val initBody = Json.parseToJsonElement(init.bodyAsText()).jsonObject
            assertEquals(
                "chromia-mcp-server",
                initBody.getValue("result").jsonObject
                    .getValue("serverInfo").jsonObject
                    .getValue("name").jsonPrimitive.content
            )

            // The same session answers tools/list.
            val listed = http.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json, text/event-stream")
                header(App.MCP_SESSION_ID_HEADER, sessionId!!)
                setBody("""{"jsonrpc":"2.0","id":2,"method":"tools/list","params":{}}""")
            }
            assertEquals(HttpStatusCode.OK, listed.status)
            val tools = Json.parseToJsonElement(listed.bodyAsText()).jsonObject
                .getValue("result").jsonObject.getValue("tools").jsonArray
            assertTrue(tools.size >= 25, "only ${tools.size} tools over /mcp")

            // A successful tools/call still carries "isError": false on the wire.
            // kotlin-sdk 0.7.7 defaulted CallToolResult.isError to false; 0.15
            // defaults it to null, and the field is serialized as set - so a
            // success that leaves it unset drops the field entirely. That is a
            // wire change for any client reading the field rather than its
            // absence, which is why toolSuccessResult states it.
            val called = http.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json, text/event-stream")
                header(App.MCP_SESSION_ID_HEADER, sessionId)
                setBody("""{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"get_prompts","arguments":{}}}""")
            }
            assertEquals(HttpStatusCode.OK, called.status)
            val callResult = Json.parseToJsonElement(called.bodyAsText()).jsonObject.getValue("result").jsonObject
            assertEquals(
                false,
                callResult.getValue("isError").jsonPrimitive.content.toBoolean(),
                "a successful tools/call must still send isError:false"
            )

            // An id this process never minted is not a session.
            val bogus = http.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json, text/event-stream")
                header(App.MCP_SESSION_ID_HEADER, "00000000-0000-0000-0000-000000000000")
                setBody("""{"jsonrpc":"2.0","id":3,"method":"tools/list","params":{}}""")
            }
            assertEquals(HttpStatusCode.NotFound, bogus.status)

            // DELETE ends it; the id stops working.
            val deleted = http.delete(url) {
                header(App.MCP_SESSION_ID_HEADER, sessionId)
            }
            assertEquals(HttpStatusCode.OK, deleted.status)
            val afterDelete = http.post(url) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json, text/event-stream")
                header(App.MCP_SESSION_ID_HEADER, sessionId)
                setBody("""{"jsonrpc":"2.0","id":4,"method":"tools/list","params":{}}""")
            }
            assertEquals(HttpStatusCode.NotFound, afterDelete.status)
        } finally {
            http.close()
            server.stop(500, 1000)
        }
    }

    /** /health is untouched by the new endpoint, needs no session, and names the profile. */
    @Test
    fun healthStaysOpenBesideBothTransports() = runBlocking {
        val app = McpTestSupport.testApp()
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false)
        val http = HttpClient(CIO) {
            // No HttpTimeout means "wait forever", which in a 45-minute task budget
            // turns one stalled endpoint into a deleted-results suite timeout.
            install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        }
        try {
            val port = server.engine.resolvedConnectors().first().port

            val health = http.get("http://127.0.0.1:$port/health")
            assertEquals(HttpStatusCode.OK, health.status)
            val body = Json.parseToJsonElement(health.bodyAsText()).jsonObject
            assertEquals("healthy", body.getValue("status").jsonPrimitive.content)
            assertEquals(App.SERVER_NAME, body.getValue("server").jsonPrimitive.content)
            assertEquals(App.profile, body.getValue("profile").jsonPrimitive.content)

            // ... and /mcp answers on the same port at the same time.
            val init = http.post("http://127.0.0.1:$port/mcp") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, "application/json, text/event-stream")
                setBody(
                    """{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26",""" +
                        """"capabilities":{},"clientInfo":{"name":"t","version":"1"}}}"""
                )
            }
            assertEquals(HttpStatusCode.OK, init.status)
            assertNotNull(init.headers[App.MCP_SESSION_ID_HEADER])
        } finally {
            http.close()
            server.stop(500, 1000)
        }
    }

    private suspend fun withStreamableHttpSession(
        engine: MockEngine = McpTestSupport.errorEngine(),
        block: suspend (Client, Int) -> Unit
    ) {
        val app = McpTestSupport.testApp(engine = engine)
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false)
        val http = HttpClient(CIO) {
            install(SSE)
            install(HttpTimeout) { requestTimeoutMillis = 20_000 }
        }
        try {
            val port = server.engine.resolvedConnectors().first().port
            val client = withTimeout(20_000) { http.mcpStreamableHttp("http://127.0.0.1:$port/mcp") }
            try {
                block(client, port)
            } finally {
                runCatching { client.close() }
            }
        } finally {
            http.close()
            server.stop(500, 1000)
        }
    }
}
