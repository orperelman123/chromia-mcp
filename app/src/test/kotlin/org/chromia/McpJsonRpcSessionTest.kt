package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import org.chromia.tools.readResourceRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.chromia.data.config.ChromiaConfig
import org.chromia.tools.McpResources
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream

class McpJsonRpcSessionTest {

    private val validBrid = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

    @Test
    fun initializeListsToolsAndCallsGetPromptsWithoutNetwork() = runBlocking {
        withStdioSession { client ->
            val serverInfo = client.serverVersion
            assertNotNull(serverInfo)
            assertEquals("chromia-mcp-server", serverInfo!!.name)
            assertEquals(BuildInfo.VERSION, serverInfo.version)
            assertEquals(false, client.serverCapabilities?.tools?.listChanged)

            val listed = withTimeout(10_000) { client.listTools() }
            assertNotNull(listed)
            val names = listed!!.tools.map { it.name }
            assertTrue(names.contains("search"), "tools/list missing search: $names")
            assertTrue(names.contains("fetch"), "tools/list missing fetch: $names")
            assertTrue(names.contains("get_prompts"), "tools/list missing get_prompts: $names")
            assertTrue(
                names.contains("get_total_rewards_paid"),
                "tools/list missing get_total_rewards_paid: $names"
            )
            // Retired 2026-09-07 with the four tools the explorer will not serve:
            // a tools/list that still advertised them is the thing this branch removes.
            listOf(
                "get_network_stats", "get_transactions_by_cluster",
                "get_blockchains_transactions", "get_node_unavailability"
            ).forEach { retired ->
                assertTrue(!names.contains(retired), "tools/list still advertises $retired")
            }
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
            val stats = call.structuredContent!!["statistics"]
            assertNotNull(stats)
        }
    }

    @Test
    fun initializeListsResourcesAndReadsHealthWithoutNetwork() = runBlocking {
        withStdioSession { client ->
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

    @Test
    fun stdioReadsDocsRepositoriesResourceWithoutNetwork() = runBlocking {
        withStdioSession { client ->
            val listed = withTimeout(10_000) { client.listResources() }
            assertNotNull(listed)
            val resource = listed!!.resources.single { it.uri == McpResources.DOCS_REPOSITORIES_URI }
            assertEquals("docs-repositories", resource.name)
            assertEquals(McpResources.JSON_MIME, resource.mimeType)

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
    fun stdioReadsPromptCatalogResourceWithoutNetwork() = runBlocking {
        withStdioSession { client ->
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

    /**
     * THE LIVE EXPLORER, THROUGH THE IN-PROCESS stdio SESSION.
     *
     * This used to be a MockEngine handing back a recorded `allBlockchains`
     * envelope, plus assertions over the request the engine had captured. The
     * captured-request half proved the GraphQL document and variables the
     * repository builds; the response half proved nothing about the explorer,
     * because the explorer never saw it.
     *
     * Live, the same properties are proved by the answer instead of by the
     * request: the explorer only returns the directory chain for name="directory", system=true if the document it was
     * sent was well formed and the variables bound, and the value that comes
     * back is what the explorer says today rather than what it said when the
     * fixture was written.
     */
    @Test
    fun stdioFilterBlockchainsReachesTheLiveExplorerThroughMcpSession() = runBlocking {
        LiveChromia.requireLive("calls filter_blockchains against the live explorer over an in-process stdio session")
        withStdioSession(config = LiveChromia.config()) { client ->
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
            val chains = call.structuredContent!!
                .getValue("data").jsonObject
                .getValue("allBlockchains").jsonArray
            assertTrue(chains.isNotEmpty(), "the live explorer knows the directory chain: $chains")
            val names = chains.map { it.jsonObject.getValue("name").jsonPrimitive.content }
            assertTrue(
                names.contains("directory_chain"),
                "name=\"directory\" + system=true must reach the directory chain; the variables did not " +
                    "bind if it did not. Got: $names"
            )
            val directory = chains.first { it.jsonObject.getValue("name").jsonPrimitive.content == "directory_chain" }
            assertEquals(true, directory.jsonObject.getValue("system").jsonPrimitive.content.toBoolean())
            assertEquals(64, directory.jsonObject.getValue("rid").jsonPrimitive.content.length)
        }
    }

    /**
     * chromia_dapp_query AGAINST THE LIVE ECONOMY CHAIN.
     *
     * The double here was a trailing-lambda `BlockchainQueryClient` that
     * asserted the arguments it had been handed and then produced the answer
     * itself - so `{"modules":"ok","name":"CHR"}` was proof that the test could
     * write a dictionary. Live, the Economy Chain binds the arguments (a wrong
     * conversion cannot bind) and the response is real GTV that has been through
     * the strict gson, so the tool's whole path is exercised end to end over the
     * in-process stdio session.
     */
    @Test
    fun stdioChromiaDappQueryReachesTheLiveEconomyChain() = runBlocking {
        LiveChromia.requireLive("calls chromia_dapp_query against the live Economy Chain over stdio")
        withStdioSession(config = LiveChromia.config()) { client ->
            val call = withTimeout(60_000) {
                client.callTool(
                    name = "chromia_dapp_query",
                    arguments = mapOf(
                        "network" to LiveChromia.NETWORK,
                        "blockchainRid" to LiveChromia.ECONOMY_CHAIN_BRID_HEX,
                        "query" to "get_chr_asset",
                        "arguments" to emptyMap<String, String>()
                    )
                )
            }
            assertNotNull(call)
            assertEquals(false, call!!.isError == true, (call.content.first() as TextContent).text)
            val structured = call.structuredContent!!
            assertEquals("tCHR", structured.getValue("symbol").jsonPrimitive.content)
            // A real big_integer supply, serialized as a JSON string by the
            // strict gson - the audit F1 path, over a real MCP session.
            assertTrue(structured.getValue("supply").jsonPrimitive.isString, structured.toString())
            val text = (call.content.first() as TextContent).text!!
            assertTrue(text.contains("tCHR"), text)
        }
    }

    /**
     * THE LIVE EXPLORER, THROUGH THE IN-PROCESS stdio SESSION.
     *
     * This used to be a MockEngine handing back a recorded `filterAssets`
     * envelope, plus assertions over the request the engine had captured. The
     * captured-request half proved the GraphQL document and variables the
     * repository builds; the response half proved nothing about the explorer,
     * because the explorer never saw it.
     *
     * Live, the same properties are proved by the answer instead of by the
     * request: the explorer only returns an asset whose symbol is CHR for searchQuery="CHR" if the document it was
     * sent was well formed and the variables bound, and the value that comes
     * back is what the explorer says today rather than what it said when the
     * fixture was written.
     */
    @Test
    fun stdioFilterAssetsReachesTheLiveExplorerThroughMcpSession() = runBlocking {
        LiveChromia.requireLive("calls filter_assets against the live explorer over an in-process stdio session")
        withStdioSession(config = LiveChromia.config()) { client ->
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
            val page = structured.getValue("data").jsonObject.getValue("filterAssets").jsonObject
            val assets = page.getValue("assets").jsonArray
            assertTrue(assets.isNotEmpty(), "searchQuery=CHR must match something on mainnet: $page")
            val symbols = assets.map { it.jsonObject.getValue("symbol").jsonPrimitive.content }
            assertTrue(
                symbols.any { it.contains("CHR") },
                "searchQuery did not bind - none of the returned symbols mention CHR: $symbols"
            )
            val text = (call.content.first() as TextContent).text!!
            assertEquals(structured, Json.parseToJsonElement(text).jsonObject)
        }
    }

    @Test
    fun stdioSearchThenFetchUsesInMemoryRagStore() = runBlocking {
        withStdioSession { client ->
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

    private suspend fun CoroutineScope.withStdioSession(
        config: ChromiaConfig = McpTestSupport.offlineConfig(),
        block: suspend (Client) -> Unit
    ) {
        val clientToServer = PipedOutputStream()
        val serverIn = PipedInputStream(clientToServer, PIPE_BUFFER)
        val serverToClient = PipedOutputStream()
        val clientIn = PipedInputStream(serverToClient, PIPE_BUFFER)

        val app = McpTestSupport.testApp(config = config)
        val server = app.createMcpServer()
        val serverTransport = StdioServerTransport(
            input = serverIn.asSource().buffered(),
            output = serverToClient.asSink().buffered()
        )
        val clientTransport = StdioClientTransport(
            input = clientIn.asSource().buffered(),
            output = clientToServer.asSink().buffered()
        )
        val sessionJob = launch(Dispatchers.IO) {
            server.createSession(serverTransport)
        }
        val client = Client(Implementation(name = "chromia-mcp-test", version = "0"))
        try {
            withTimeout(10_000) {
                client.connect(clientTransport)
            }
            block(client)
        } finally {
            runCatching { client.close() }
            runCatching { serverTransport.close() }
            sessionJob.cancel()
        }
    }

    private companion object {
        const val PIPE_BUFFER = 2 * 1024 * 1024
    }
}
