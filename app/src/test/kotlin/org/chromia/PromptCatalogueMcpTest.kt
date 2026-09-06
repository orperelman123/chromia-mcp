package org.chromia

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequest
import io.modelcontextprotocol.kotlin.sdk.types.GetPromptRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.chromia.tools.McpPrompts
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream

/**
 * Audit F8: `prompts/list` answered `{"code":-32601,"message":"Server does not
 * support prompts/list"}` and `initialize` advertised no prompts capability, so a
 * client that speaks only the prompts protocol saw none of the catalogue - and
 * every one of the 73 distinct `tool` fields in it named
 * `mcp_chromia-mcp_<name>`, a tool that does not exist.
 *
 * This drives a real in-process MCP client over stdio, the way the audit did.
 */
class PromptCatalogueMcpTest {

    @Test
    fun promptsListAndGetAnswerOverTheProtocol() = runBlocking {
        withStdioSession { client ->
            assertEquals(
                false, client.serverCapabilities?.prompts?.listChanged,
                "the prompts capability must be advertised: a client that only speaks prompts/list saw nothing"
            )

            val listed = withTimeout(10_000) { client.listPrompts() }
            assertNotNull(listed)
            val prompts = listed!!.prompts
            assertEquals(
                McpPrompts.catalogue(PromptManager()).size, prompts.size,
                "prompts/list must serve the whole catalogue"
            )
            assertEquals(
                McpPrompts.FLAGSHIP, prompts.first().name,
                "the Secure dapp workflow prompt goes first"
            )
            assertEquals(prompts.size, prompts.map { it.name }.toSet().size, "duplicate prompt names")

            val flagship = prompts.first()
            assertEquals("Secure dapp workflow", flagship.title)
            assertTrue(flagship.description!!.isNotBlank())
            assertEquals(listOf("goal"), flagship.arguments?.map { it.name })

            val got = withTimeout(10_000) {
                client.getPrompt(
                    GetPromptRequest(
                        GetPromptRequestParams(
                            name = McpPrompts.FLAGSHIP,
                            arguments = mapOf("goal" to "a lending market")
                        )
                    )
                )
            }
            assertNotNull(got)
            val message = got!!.messages.single()
            val text = (message.content as TextContent).text!!
            assertTrue(text.contains("a lending market"), "the argument was not substituted: $text")
            assertTrue(!text.contains("{goal}"), "the placeholder survived substitution")
            assertTrue(text.contains("verify_guards"), "the flagship prompt lost its guard-proving step")
            assertTrue(
                !text.contains("mcp_chromia-mcp_"),
                "a prompt still names tools that do not exist"
            )
        }
    }

    /** An unfilled placeholder stays visible rather than silently becoming "". */
    @Test
    fun missingArgumentsLeaveThePlaceholderInPlace() {
        val flagship = McpPrompts.catalogue(PromptManager()).first { it.name == McpPrompts.FLAGSHIP }
        assertTrue(flagship.render(null).contains("{goal}"))
        assertTrue(flagship.render(emptyMap()).contains("{goal}"))
    }

    /**
     * The pin the audit asked for: every tool name the catalogue mentions - the
     * `tool` field and any tool-shaped identifier in the prompt text - exists in
     * tools/list. Adding a prompt that names a tool this server does not implement
     * fails here instead of sending the agent into "Tool ... not found".
     */
    @Test
    fun everyToolNameMentionedInAnyPromptExists() {
        val advertised = McpTools.allTools(compact = false).map { it.name }.toSet()
        val referenced = McpPrompts.referencedToolNames(PromptManager())
        val unknown = referenced - advertised - McpPrompts.NON_TOOL_IDENTIFIERS
        assertTrue(
            unknown.isEmpty(),
            "prompts reference tools that are not in tools/list: ${unknown.sorted()}"
        )
        assertTrue("scaffold_dapp" in referenced)
        assertTrue("verify_guards" in referenced)
        assertTrue("filter_blockchains" in referenced)
    }

    /** Bare names in the catalogue, not the `mcp_chromia-mcp_` spelling (F8). */
    @Test
    fun theCatalogueStoresBareToolNames() {
        val manager = PromptManager()
        val advertised = McpTools.allTools(compact = false).map { it.name }.toSet()
        var checked = 0
        manager.getCategories().forEach { category ->
            manager.getPromptsForCategory(category).orEmpty().forEach { prompt ->
                val raw = manager.getToolForPrompt(prompt).orEmpty()
                assertTrue(
                    !raw.startsWith("mcp_") && !raw.startsWith("mcp__"),
                    "prompt in $category still stores a prefixed tool name: $raw"
                )
                assertTrue(raw in advertised, "prompt in $category names unknown tool $raw")
                checked++
            }
        }
        assertEquals(90, checked, "the catalogue changed size - update the pin deliberately")
    }

    /** Compact mode trims prompt descriptions but never the prompt itself. */
    @Test
    fun compactPromptDescriptionsAreTrimmedNotTruncatedPrompts() {
        McpPrompts.catalogue(PromptManager()).forEach { entry ->
            val trimmed = App.compactPromptDescription(entry.description)
            assertTrue(
                trimmed.toByteArray().size <= App.COMPACT_PROMPT_DESCRIPTION_BYTES + 4,
                "${entry.name} compact description is ${trimmed.toByteArray().size} B"
            )
            assertEquals(entry.template, entry.render(null), "compact mode must not touch the prompt text")
        }
    }

    private suspend fun CoroutineScope.withStdioSession(block: suspend (Client) -> Unit) {
        val clientToServer = PipedOutputStream()
        val serverIn = PipedInputStream(clientToServer, PIPE_BUFFER)
        val serverToClient = PipedOutputStream()
        val clientIn = PipedInputStream(serverToClient, PIPE_BUFFER)

        val app = McpTestSupport.testApp()
        val server = app.createMcpServer()
        val serverTransport = StdioServerTransport(
            input = serverIn.asSource().buffered(),
            output = serverToClient.asSink().buffered()
        )
        val clientTransport = StdioClientTransport(
            input = clientIn.asSource().buffered(),
            output = clientToServer.asSink().buffered()
        )
        val sessionJob = launch(Dispatchers.IO) { server.createSession(serverTransport) }
        val client = Client(Implementation(name = "chromia-mcp-test", version = "0"))
        try {
            withTimeout(10_000) { client.connect(clientTransport) }
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
