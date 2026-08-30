package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.modelcontextprotocol.kotlin.sdk.ReadResourceRequest
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.mcpSse
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

class McpSseSessionTest {

    @Test
    fun initializeListsToolsAndCallsGetPromptsOverSseWithoutNetwork() = runBlocking {
        withSseSession { client ->
            val serverInfo = client.serverVersion
            assertNotNull(serverInfo)
            assertEquals("chromia-mcp-server", serverInfo!!.name)
            assertEquals(BuildInfo.VERSION, serverInfo.version)
            assertEquals(false, client.serverCapabilities?.tools?.listChanged)

            val listed = withTimeout(10_000) { client.listTools() }
            assertNotNull(listed)
            val names = listed!!.tools.map { it.name }
            assertTrue(names.contains("search"), "tools/list missing search: $names")
            assertTrue(names.contains("get_prompts"), "tools/list missing get_prompts: $names")
            assertTrue(names.contains("filter_blockchains"), "tools/list missing filter_blockchains: $names")
            assertTrue(names.contains("chromia_dapp_query"), "tools/list missing chromia_dapp_query: $names")

            val call = withTimeout(10_000) {
                client.callTool(name = "get_prompts", arguments = emptyMap())
            }
            assertNotNull(call)
            assertEquals(false, call!!.isError == true)
            val text = (call.content.first() as TextContent).text!!
            assertTrue(text.contains("prompts"), text)
            assertTrue(call.structuredContent!!.containsKey("prompts"))
            assertTrue(call.structuredContent!!.containsKey("statistics"))
        }
    }

    @Test
    fun initializeListsResourcesAndReadsHealthOverSseWithoutNetwork() = runBlocking {
        withSseSession { client ->
            assertEquals(false, client.serverCapabilities?.resources?.subscribe)
            assertEquals(false, client.serverCapabilities?.resources?.listChanged)

            val listed = withTimeout(10_000) { client.listResources() }
            assertNotNull(listed)
            val uris = listed!!.resources.map { it.uri }
            assertEquals(
                setOf(
                    McpResources.HEALTH_URI,
                    McpResources.DOCS_REPOSITORIES_URI,
                    McpResources.PROMPT_CATALOG_URI
                ),
                uris.toSet()
            )
            assertEquals(3, uris.size)

            val health = listed.resources.single { it.uri == McpResources.HEALTH_URI }
            assertEquals("server-health", health.name)
            assertEquals(McpResources.JSON_MIME, health.mimeType)
            assertEquals("chromia://server/health", health.uri)

            val read = withTimeout(10_000) {
                client.readResource(ReadResourceRequest(uri = McpResources.HEALTH_URI))
            }
            assertNotNull(read)
            val content = read!!.contents.single() as TextResourceContents
            assertEquals(McpResources.HEALTH_URI, content.uri)
            assertEquals(McpResources.JSON_MIME, content.mimeType)
            assertEquals(App.healthJson(), content.text)
            assertTrue(content.text.contains("\"status\": \"healthy\""))
            assertTrue(content.text.contains(App.SERVER_NAME))
            assertTrue(content.text.contains(App.SERVER_VERSION))
        }
    }

    @Test
    fun sseFilterBlockchainsUsesMockEngine200ThroughMcpSession() = runBlocking {
        val fixture = """{"data":{"allBlockchains":[{"rid":"abc","name":"directory_chain","system":true}]}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        withSseSession(engine = engine) { client ->
            val call = withTimeout(10_000) {
                client.callTool(
                    name = "filter_blockchains",
                    arguments = mapOf(
                        "network" to "testnet",
                        "name" to "directory",
                        "limit" to 5,
                        "system" to true
                    )
                )
            }
            assertNotNull(call)
            assertEquals(false, call!!.isError == true)
            val structured = call.structuredContent!!
            assertEquals(
                "directory_chain",
                structured
                    .getValue("data")
                    .jsonObject
                    .getValue("allBlockchains")
                    .jsonArray[0]
                    .jsonObject
                    .getValue("name")
                    .jsonPrimitive
                    .content
            )
            val text = (call.content.first() as TextContent).text!!
            assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
            assertEquals(1, engine.requestHistory.size)
            assertEquals("testnet", engine.requestHistory.first().url.parameters["network"])
            assertEquals(1, capturedBodies.size)
            val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
            assertTrue(
                posted["query"]!!.jsonPrimitive.content.contains("allBlockchains"),
                posted["query"]!!.jsonPrimitive.content
            )
            val variables = posted.getValue("variables").jsonObject
            assertEquals("directory", variables["name"]!!.jsonPrimitive.content)
            assertEquals("5", variables["limit"]!!.jsonPrimitive.content)
            assertEquals("true", variables["system"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun sseFilterAssetsUsesMockEngine200ThroughMcpSession() = runBlocking {
        val fixture = """{"data":{"filterAssets":{"assets":[{"name":"Chromia","symbol":"CHR","type":"FT"}],"totalCount":1}}}"""
        val capturedBodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            capturedBodies.add(request.body.toByteArray().decodeToString())
            respond(
                content = fixture,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        withSseSession(engine = engine) { client ->
            val call = withTimeout(10_000) {
                client.callTool(
                    name = "filter_assets",
                    arguments = mapOf(
                        "network" to "mainnet",
                        "searchQuery" to "CHR",
                        "type" to "FT",
                        "limit" to 10
                    )
                )
            }
            assertNotNull(call)
            assertEquals(false, call!!.isError == true)
            val structured = call.structuredContent!!
            assertEquals(
                "CHR",
                structured
                    .getValue("data")
                    .jsonObject
                    .getValue("filterAssets")
                    .jsonObject
                    .getValue("assets")
                    .jsonArray[0]
                    .jsonObject
                    .getValue("symbol")
                    .jsonPrimitive
                    .content
            )
            val text = (call.content.first() as TextContent).text!!
            assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
            assertEquals(1, engine.requestHistory.size)
            assertEquals("mainnet", engine.requestHistory.first().url.parameters["network"])
            assertEquals(1, capturedBodies.size)
            val posted = Json.parseToJsonElement(capturedBodies.first()).jsonObject
            assertTrue(
                posted["query"]!!.jsonPrimitive.content.contains("filterAssets"),
                posted["query"]!!.jsonPrimitive.content
            )
            val variables = posted.getValue("variables").jsonObject
            assertEquals("CHR", variables["searchQuery"]!!.jsonPrimitive.content)
            assertEquals("FT", variables["type"]!!.jsonPrimitive.content)
            assertEquals("10", variables["limit"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun sseReadsDocsRepositoriesResourceWithoutNetwork() = runBlocking {
        withSseSession { client ->
            val read = withTimeout(10_000) {
                client.readResource(ReadResourceRequest(uri = McpResources.DOCS_REPOSITORIES_URI))
            }
            assertNotNull(read)
            val content = read!!.contents.single() as TextResourceContents
            assertEquals(McpResources.DOCS_REPOSITORIES_URI, content.uri)
            assertEquals(McpResources.JSON_MIME, content.mimeType)
            assertEquals(McpResources.classpathText("docs-repositories.json"), content.text)
            assertTrue(content.text.contains("\"name\": \"rell\""))
            assertTrue(content.text.contains("https://github.com/ChromiaProject/rell.git"))
        }
    }

    @Test
    fun sseReadsPromptCatalogResourceWithoutNetwork() = runBlocking {
        withSseSession { client ->
            val listed = withTimeout(10_000) { client.listResources() }
            assertNotNull(listed)
            val resource = listed!!.resources.single { it.uri == McpResources.PROMPT_CATALOG_URI }
            assertEquals("prompt-catalog", resource.name)
            assertEquals(McpResources.JSON_MIME, resource.mimeType)

            val read = withTimeout(10_000) {
                client.readResource(ReadResourceRequest(uri = McpResources.PROMPT_CATALOG_URI))
            }
            assertNotNull(read)
            val content = read!!.contents.single() as TextResourceContents
            assertEquals(McpResources.PROMPT_CATALOG_URI, content.uri)
            assertEquals(McpResources.JSON_MIME, content.mimeType)
            assertEquals(McpResources.classpathText("prompt_templates.json"), content.text)
            assertTrue(content.text.contains("chromia_stack"))
            assertTrue(content.text.contains("Chromia stack expert"))
            assertTrue(content.text.contains("dapp_query"))
            assertTrue(content.text.contains("chromia_dapp_query"))
        }
    }

    @Test
    fun sseSearchThenFetchUsesInMemoryRagStore() = runBlocking {
        withSseSession { client ->
            val search = withTimeout(10_000) {
                client.callTool(name = "search", arguments = mapOf("query" to "FT4 authentication"))
            }
            assertNotNull(search)
            assertEquals(false, search!!.isError == true)
            val searchHits = search.structuredContent!!["results"]!!.jsonArray
            assertEquals(1, searchHits.size)
            val id = searchHits.first().jsonObject["id"]!!.jsonPrimitive.content
            assertEquals(org.chromia.tools.segmentId(McpTestSupport.AUTH_SEGMENT), id)
            assertEquals("ft4-auth.md", searchHits.first().jsonObject["title"]!!.jsonPrimitive.content)
            assertTrue(searchHits.first().jsonObject["url"]!!.jsonPrimitive.content.contains("ft4-auth.md"))
            val searchText = (search.content.first() as TextContent).text!!
            assertEquals(search.structuredContent, Json.parseToJsonElement(searchText).jsonObject)

            val fetch = withTimeout(10_000) {
                client.callTool(name = "fetch", arguments = mapOf("id" to id))
            }
            assertNotNull(fetch)
            assertEquals(false, fetch!!.isError == true)
            assertEquals(id, fetch.structuredContent!!["id"]!!.jsonPrimitive.content)
            assertEquals("ft4-auth.md", fetch.structuredContent!!["title"]!!.jsonPrimitive.content)
            assertTrue(fetch.structuredContent!!["text"]!!.jsonPrimitive.content.contains("FT4 authentication"))
            assertTrue("error" !in fetch.structuredContent!!)
            assertTrue("metadata" !in fetch.structuredContent!!)

            val docs = withTimeout(10_000) {
                client.callTool(name = "fetch_docs", arguments = mapOf("query" to "Rell compiler pipeline"))
            }
            assertNotNull(docs)
            assertEquals(false, docs!!.isError == true)
            val docsHits = docs.structuredContent!!["hits"]!!.jsonArray
            assertEquals(1, docsHits.size)
            val rellId = docsHits.first().jsonObject["id"]!!.jsonPrimitive.content
            assertEquals(org.chromia.tools.segmentId(McpTestSupport.RELL_SEGMENT), rellId)
            assertEquals(McpTestSupport.RELL_SEGMENT.text(), docsHits.first().jsonObject["text"]!!.jsonPrimitive.content)
            assertTrue(docs.structuredContent!!["text"]!!.jsonPrimitive.content.contains(rellId))

            val unknown = withTimeout(10_000) {
                client.callTool(name = "fetch", arguments = mapOf("id" to "missing-doc"))
            }
            assertNotNull(unknown)
            assertEquals(true, unknown!!.isError)
            assertEquals("missing-doc", unknown.structuredContent!!["id"]!!.jsonPrimitive.content)
            assertTrue(unknown.structuredContent!!["error"]!!.jsonPrimitive.content.contains("Documentation not found"))
            assertTrue("title" !in unknown.structuredContent!!)
            assertTrue("text" !in unknown.structuredContent!!)
        }
    }

    private suspend fun withSseSession(
        engine: MockEngine = McpTestSupport.errorEngine(),
        block: suspend (Client) -> Unit
    ) {
        val app = McpTestSupport.testApp(engine = engine)
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false)
        val http = HttpClient(CIO) { install(SSE) }
        try {
            val port = server.engine.resolvedConnectors().first().port
            val client = withTimeout(15_000) {
                http.mcpSse("http://127.0.0.1:$port")
            }
            try {
                block(client)
            } finally {
                runCatching { client.close() }
            }
        } finally {
            http.close()
            server.stop(500, 1000)
        }
    }
}
