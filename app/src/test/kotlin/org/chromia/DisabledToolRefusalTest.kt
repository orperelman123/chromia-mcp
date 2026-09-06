package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.client.mcpSse
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.chromia.tools.McpTools
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Deployment-aware disabled-tool refusals (hosted probe 2026-09-01): a tool
 * disabled via CHROMIA_MCP_DISABLE_TOOLS used to answer with the SDK's bare
 * "Tool X not found" - indistinguishable from a tool that never existed, no
 * alternative offered. The SDK keeps ONE registry feeding both tools/list and
 * tools/call, so a registered stub would also be advertised; instead the
 * per-session tools/call handler is replaced (App.createGatedSession) so
 * tools/list stays exactly the advertised set while the CALL gets guidance.
 */
class DisabledToolRefusalTest {

    // ---- refusal message unit tests -------------------------------------

    @Test
    fun rellToolsPointAtCheckDappProject() {
        val disabled = setOf("rell_check", "rell_security_check", "run_rell_tests")
        disabled.forEach { name ->
            val message = McpTools.disabledToolRefusal(name, disabled)
            assertNotNull(message, name)
            assertTrue(message!!.contains("disabled on this deployment"), message)
            assertTrue(message.contains("CHROMIA_MCP_DISABLE_TOOLS"), message)
            assertTrue(message.contains("check_dapp_project"), message)
            assertTrue(message.contains("compilation and security scanning"), message)
            assertTrue(message.contains("Run chromia-mcp locally for the full toolset"), message)
        }
    }

    @Test
    fun dappQueryPointsAtExplorerTools() {
        val message = McpTools.disabledToolRefusal("chromia_dapp_query", setOf("chromia_dapp_query"))
        assertNotNull(message)
        assertTrue(message!!.contains("explorer analytics tools"), message)
        assertTrue(message.contains("filter_blockchains"), message)
        assertTrue(message.contains("Run chromia-mcp locally"), message)
    }

    @Test
    fun disabledAlternativeIsNeverItselfDisabled() {
        // check_dapp_project disabled too: the refusal must not point at it.
        val disabled = setOf("rell_check", "check_dapp_project")
        val message = McpTools.disabledToolRefusal("rell_check", disabled)
        assertNotNull(message)
        assertFalse(message!!.contains("check_dapp_project"), message)
        assertTrue(message.contains("Run chromia-mcp locally"), message)
    }

    @Test
    fun toolWithoutSpecificAlternativeStillGetsRefusal() {
        val message = McpTools.disabledToolRefusal("search", setOf("search"))
        assertNotNull(message)
        assertTrue(message!!.contains("disabled on this deployment"), message)
        assertTrue(message.contains("Run chromia-mcp locally"), message)
    }

    @Test
    fun noRefusalForEnabledOrNonexistentTools() {
        // Not disabled: null (normal routing answers).
        assertNull(McpTools.disabledToolRefusal("rell_check", emptySet()))
        // Disabled name that is not a real tool of this server: null (stays "not found").
        assertNull(McpTools.disabledToolRefusal("made_up_tool", setOf("made_up_tool")))
    }

    // ---- stdio session: list unchanged, call refused --------------------

    @Test
    fun stdioDisabledToolIsHiddenFromListButCallGetsGuidance() = runBlocking {
        val disabled = setOf("rell_check", "rell_security_check", "run_rell_tests")
        withStdioSession(disabled = disabled) { client ->
            val names = withTimeout(10_000) { client.listTools() }!!.tools.map { it.name }
            // tools/list stays exactly the advertised set - no stub entries.
            assertTrue(disabled.none { it in names }, "disabled tools leaked into tools/list: $names")
            assertTrue("check_dapp_project" in names, names.toString())

            val call = withTimeout(10_000) {
                client.callTool(name = "rell_check", arguments = mapOf("source" to "module;"))
            }
            assertNotNull(call)
            assertEquals(true, call!!.isError)
            val text = (call.content.first() as TextContent).text!!
            assertTrue(text.contains("disabled on this deployment"), text)
            assertTrue(text.contains("check_dapp_project"), text)
            assertTrue(text.contains("Run chromia-mcp locally"), text)

            // A name this server never implemented keeps the SDK-shaped answer.
            val unknown = withTimeout(10_000) {
                client.callTool(name = "definitely_not_a_tool", arguments = emptyMap())
            }
            assertEquals(true, unknown!!.isError)
            assertEquals(
                "Tool definitely_not_a_tool not found",
                (unknown.content.first() as TextContent).text
            )

            // Enabled tools still execute through the gated handler.
            val prompts = withTimeout(10_000) { client.callTool(name = "get_prompts", arguments = emptyMap()) }
            assertEquals(false, prompts!!.isError == true)
        }
    }

    @Test
    fun stdioCompactHiddenHelpToolsGetNoStubOrRefusal() = runBlocking {
        // Compact mode hides help tools for schema savings, it does not disable
        // them as a deployment policy - chromia_help covers their content, so no
        // stub in tools/list and no disabled-refusal on call.
        withStdioSession(compact = true) { client ->
            val names = withTimeout(10_000) { client.listTools() }!!.tools.map { it.name }
            assertTrue("chr_build_help" !in names, names.toString())
            assertTrue("chromia_help" in names, names.toString())

            val call = withTimeout(10_000) { client.callTool(name = "chr_build_help", arguments = emptyMap()) }
            assertEquals(true, call!!.isError)
            val text = (call.content.first() as TextContent).text!!
            assertEquals("Tool chr_build_help not found", text)

            val gateway = withTimeout(10_000) {
                client.callTool(name = "chromia_help", arguments = mapOf("topic" to "chr_build"))
            }
            assertEquals(false, gateway!!.isError == true)
        }
    }

    // ---- SSE session: same behavior over the hosted transport -----------

    @Test
    fun sseDisabledToolIsHiddenFromListButCallGetsGuidance() = runBlocking {
        val disabled = setOf("rell_check")
        val app = McpTestSupport.testApp()
        val server = app.runSseMcpServer(
            host = "127.0.0.1",
            port = 0,
            wait = false,
            authToken = null,
            allowedOrigins = null,
            compact = false,
            disabled = disabled
        )
        val http = HttpClient(CIO) { install(SSE) }
        try {
            val port = server.engine.resolvedConnectors().first().port
            val client = withTimeout(15_000) { http.mcpSse("http://127.0.0.1:$port") }
            try {
                val names = withTimeout(10_000) { client.listTools() }!!.tools.map { it.name }
                assertTrue("rell_check" !in names, names.toString())
                assertTrue("check_dapp_project" in names, names.toString())

                val call = withTimeout(10_000) {
                    client.callTool(name = "rell_check", arguments = mapOf("source" to "module;"))
                }
                assertEquals(true, call!!.isError)
                val text = (call.content.first() as TextContent).text!!
                assertTrue(text.contains("disabled on this deployment"), text)
                assertTrue(text.contains("check_dapp_project"), text)

                // Regular tools keep working over the rewired SSE endpoints.
                val prompts = withTimeout(10_000) { client.callTool(name = "get_prompts", arguments = emptyMap()) }
                assertEquals(false, prompts!!.isError == true)
            } finally {
                runCatching { client.close() }
            }
        } finally {
            http.close()
            server.stop(500, 1000)
        }
    }

    private suspend fun CoroutineScope.withStdioSession(
        compact: Boolean = false,
        disabled: Set<String> = emptySet(),
        block: suspend (Client) -> Unit
    ) {
        val clientToServer = PipedOutputStream()
        val serverIn = PipedInputStream(clientToServer, PIPE_BUFFER)
        val serverToClient = PipedOutputStream()
        val clientIn = PipedInputStream(serverToClient, PIPE_BUFFER)

        val app = McpTestSupport.testApp()
        val server = app.createMcpServer(compact = compact, disabled = disabled)
        val serverTransport = StdioServerTransport(
            input = serverIn.asSource().buffered(),
            output = serverToClient.asSink().buffered()
        )
        val clientTransport = StdioClientTransport(
            input = clientIn.asSource().buffered(),
            output = clientToServer.asSink().buffered()
        )
        val sessionJob = launch(Dispatchers.IO) {
            app.createGatedSession(server, serverTransport, disabled)
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
