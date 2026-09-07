package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import org.chromia.data.config.ChromiaConfig
import org.chromia.tools.readResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
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
                client.readResource(readResourceRequest(uri = McpResources.HEALTH_URI))
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

    /**
     * THE LIVE EXPLORER, THROUGH THE IN-PROCESS SSE SESSION.
     *
     * A MockEngine used to answer with a recorded `allBlockchains` envelope while the
     * test asserted over the request the engine had captured. That proved the
     * GraphQL document and the variables the repository builds - and nothing at
     * all about the explorer, which never saw either.
     *
     * Live, the answer proves both: the explorer returns the directory chain for name="directory", system=true only if the
     * document was well formed and the variables bound, and what comes back is
     * what the explorer says today rather than what it said when the fixture was
     * written. The `network` parameter is mainnet because the public explorer
     * answers 400 for every other one (docs/UPSTREAM.md #9) - a fixture was free
     * to pretend otherwise, and did.
     */
    @Test
    fun sseFilterBlockchainsReachesTheLiveExplorerThroughMcpSession() = runBlocking {
        LiveChromia.requireLive("calls filter_blockchains against the live explorer over an in-process SSE session")
        withSseSession(config = LiveChromia.config()) { client ->
            val call = withTimeout(60_000) {
                client.callTool(
                    name = "filter_blockchains",
                    arguments = mapOf(
                        "network" to LiveChromia.EXPLORER_NETWORK,
                        "name" to "directory",
                        "limit" to 5,
                        "system" to true
                    )
                )
            }
            assertNotNull(call)
            assertEquals(false, call!!.isError == true, (call.content.first() as TextContent).text)
            val structured = call.structuredContent!!
            val chains = structured.getValue("data").jsonObject.getValue("allBlockchains").jsonArray
            val names = chains.map { it.jsonObject.getValue("name").jsonPrimitive.content }
            assertTrue(
                names.contains("directory_chain"),
                "name/system did not bind - the live explorer returned $names"
            )
            val text = (call.content.first() as TextContent).text!!
            assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        }
    }

    /**
     * THE LIVE EXPLORER, THROUGH THE IN-PROCESS SSE SESSION.
     *
     * A MockEngine used to answer with a recorded `filterAssets` envelope while the
     * test asserted over the request the engine had captured. That proved the
     * GraphQL document and the variables the repository builds - and nothing at
     * all about the explorer, which never saw either.
     *
     * Live, the answer proves both: the explorer returns an asset whose symbol mentions CHR for searchQuery="CHR" only if the
     * document was well formed and the variables bound, and what comes back is
     * what the explorer says today rather than what it said when the fixture was
     * written. The `network` parameter is mainnet because the public explorer
     * answers 400 for every other one (docs/UPSTREAM.md #9) - a fixture was free
     * to pretend otherwise, and did.
     */
    @Test
    fun sseFilterAssetsReachesTheLiveExplorerThroughMcpSession() = runBlocking {
        LiveChromia.requireLive("calls filter_assets against the live explorer over an in-process SSE session")
        withSseSession(config = LiveChromia.config()) { client ->
            val call = withTimeout(60_000) {
                client.callTool(
                    name = "filter_assets",
                    arguments = mapOf(
                        "network" to LiveChromia.EXPLORER_NETWORK,
                        "searchQuery" to "CHR",
                        "limit" to 10
                    )
                )
            }
            assertNotNull(call)
            assertEquals(false, call!!.isError == true, (call.content.first() as TextContent).text)
            val structured = call.structuredContent!!
            val assets = structured.getValue("data").jsonObject
                .getValue("filterAssets").jsonObject
                .getValue("assets").jsonArray
            assertTrue(assets.isNotEmpty(), "searchQuery=CHR matched nothing on mainnet: $structured")
            val symbols = assets.map { it.jsonObject.getValue("symbol").jsonPrimitive.content }
            assertTrue(symbols.any { it.contains("CHR") }, "searchQuery did not bind: $symbols")
            val text = (call.content.first() as TextContent).text!!
            assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        }
    }

    @Test
    fun sseReadsDocsRepositoriesResourceWithoutNetwork() = runBlocking {
        withSseSession { client ->
            val read = withTimeout(10_000) {
                client.readResource(readResourceRequest(uri = McpResources.DOCS_REPOSITORIES_URI))
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
                client.readResource(readResourceRequest(uri = McpResources.PROMPT_CATALOG_URI))
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
            // Two segments, and the real BGE-small embedder scores any two short English
            // sentences above the store's 0.6 retrieval floor, so a two-segment fixture
            // index returns both. The claim this test makes is which one LEADS.
            assertEquals(2, searchHits.size)
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
            // Two segments, and the real BGE-small embedder scores any two short English
            // sentences above the store's 0.6 retrieval floor, so a two-segment fixture
            // index returns both. The claim this test makes is which one LEADS.
            assertEquals(2, docsHits.size)
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
        config: ChromiaConfig = McpTestSupport.offlineConfig(),
        block: suspend (Client) -> Unit
    ) {
        val app = McpTestSupport.testApp(config = config)
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
