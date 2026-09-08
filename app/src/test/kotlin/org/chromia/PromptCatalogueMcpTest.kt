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
import org.junit.jupiter.api.Assertions.assertThrows
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
        // 90 until 2026-09-07, when get_network_stats, get_transactions_by_cluster,
        // get_blockchains_transactions and get_node_unavailability were retired: the
        // live explorer will not serve what they advertised, and eight prompts told
        // agents to call them. The `analytics` and `monitoring` categories held
        // nothing else and went with them.
        assertEquals(82, checked, "the catalogue changed size - update the pin deliberately")
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

    // ==================================================================
    // THE ARGUMENT BOUND (adversary round 18, section 7)
    // ==================================================================
    //
    // The round fetched all 82 prompts with an empty value, a path traversal, a
    // SQL-ish string, control characters (ESC, BEL, NUL), 5,000 `x` and template
    // markers. Nothing errored and nothing was rejected: for each of the 48
    // prompts that take an argument the 5,000-character value came back VERBATIM
    // inside the prompt text, the largest answer 15,316 B - roughly three times
    // the input, because several templates interpolate the same blank more than
    // once. An unbounded value the agent did not choose, amplified by a factor
    // the template picks, straight into the agent's context.

    /** Every prompt that takes at least one argument - the 48 the round measured. */
    private fun argumentTakingPrompts() =
        McpPrompts.catalogue(PromptManager()).filter { it.argumentNames.isNotEmpty() }

    /**
     * ALL 48, not a sample. Each one refuses the round's own 5,000-character
     * value on EVERY argument it declares, with an error that names the cap.
     */
    @Test
    fun everyArgumentTakingPromptRefusesAnOverLongValueAndNamesTheCap() {
        val prompts = argumentTakingPrompts()
        assertEquals(
            48, prompts.size,
            "the round measured 48 argument-taking prompts; the catalogue changed size - update the " +
                "pin deliberately"
        )
        val hostile = "x".repeat(5000)
        var checked = 0
        prompts.forEach { prompt ->
            prompt.argumentNames.forEach { argument ->
                val thrown = assertThrows(IllegalArgumentException::class.java) {
                    prompt.toResult(mapOf(argument to hostile))
                }
                val message = thrown.message.orEmpty()
                assertTrue(message.contains(prompt.name), "the error must name the prompt: $message")
                assertTrue(message.contains(argument), "the error must name the argument: $message")
                assertTrue(
                    message.contains("${McpPrompts.MAX_ARGUMENT_CHARS}"),
                    "the error must name the cap: $message"
                )
                assertTrue(message.contains("5000"), "the error must name what was sent: $message")
                checked++
            }
        }
        assertTrue(checked >= 48, "only $checked prompt arguments were exercised")
    }

    /** The cap is a boundary, not a vibe: exactly the cap renders, one over fails. */
    @Test
    fun theCapItselfRendersAndOneCharacterMoreDoesNot() {
        val flagship = McpPrompts.catalogue(PromptManager()).first { it.name == McpPrompts.FLAGSHIP }
        val atTheCap = "g".repeat(McpPrompts.MAX_ARGUMENT_CHARS)
        val rendered = flagship.toResult(mapOf("goal" to atTheCap))
        assertTrue(
            (rendered.messages.single().content as TextContent).text!!.contains(atTheCap),
            "a value exactly at the cap must still render"
        )
        assertThrows(IllegalArgumentException::class.java) {
            flagship.toResult(mapOf("goal" to "g".repeat(McpPrompts.MAX_ARGUMENT_CHARS + 1)))
        }
    }

    /**
     * The control characters round 18 sent - ESC, BEL, NUL - are refused on every
     * argument-taking prompt; tab, newline and carriage return are ordinary text
     * in a multi-line phrase and are not.
     */
    @Test
    fun everyArgumentTakingPromptRefusesTheControlCharactersThatBreakStructure() {
        val forbidden = listOf('\u001B', '\u0007', '\u0000', '\u007F', '\u0085')
        argumentTakingPrompts().forEach { prompt ->
            val argument = prompt.argumentNames.first()
            forbidden.forEach { c ->
                val thrown = assertThrows(IllegalArgumentException::class.java) {
                    prompt.toResult(mapOf(argument to "a lending${c}market"))
                }
                val message = thrown.message.orEmpty()
                assertTrue(
                    message.contains("U+%04X".format(c.code)),
                    "the error must name the control character it refused: $message"
                )
            }
            val multiline = "a lending market\n\twith\r\nliquidations"
            assertTrue(
                (prompt.toResult(mapOf(argument to multiline)).messages.single().content as TextContent)
                    .text!!.isNotBlank(),
                "${prompt.name}: tab, newline and carriage return are text, not structure"
            )
        }
    }

    /**
     * WHAT THE BOUND BUYS, measured rather than asserted in the abstract. The
     * round's largest answer was 15,316 B; with the cap in force the largest
     * possible answer over the whole catalogue is printed here and pinned under
     * 16 KB, and the round's own 5,000-character input can no longer be sent at
     * all.
     */
    @Test
    fun theWorstCaseRenderedPromptIsBounded() {
        val worst = argumentTakingPrompts().maxOf { prompt ->
            val value = "x".repeat(McpPrompts.MAX_ARGUMENT_CHARS)
            prompt.toResult(prompt.argumentNames.associateWith { value })
                .messages.single().let { (it.content as TextContent).text!!.toByteArray().size }
        }
        println("worst-case rendered prompt at the cap: $worst B (round 18 measured 15316 B uncapped)")
        assertTrue(
            worst < 16 * 1024,
            "the worst case at the cap is $worst B; the cap is supposed to bound the amplification"
        )
    }

    /** The catalogue's own description says the cap, so an agent reads it before it hits it. */
    @Test
    fun theCatalogueDescriptionSaysTheCap() {
        val description = McpTools.allTools(compact = false)
            .single { it.name == "get_prompts" }.description.orEmpty()
        assertTrue(
            description.contains("${McpPrompts.MAX_ARGUMENT_CHARS} characters"),
            "get_prompts must advertise the per-argument cap: $description"
        )
        assertTrue(
            description.contains("control characters"),
            "get_prompts must advertise the control-character rule: $description"
        )
    }

    /** And the bound is enforced on the PROTOCOL, not only in process. */
    @Test
    fun promptsGetOverTheProtocolRefusesAnOverLongArgument() = runBlocking {
        withStdioSession { client ->
            val refused = runCatching {
                withTimeout(10_000) {
                    client.getPrompt(
                        GetPromptRequest(
                            GetPromptRequestParams(
                                name = McpPrompts.FLAGSHIP,
                                arguments = mapOf("goal" to "x".repeat(5000))
                            )
                        )
                    )
                }
            }
            assertTrue(
                refused.isFailure,
                "prompts/get answered a 5,000-character argument instead of refusing it: " +
                    "${refused.getOrNull()}"
            )
            // The session survives the refusal - a bounded argument still works.
            val ok = withTimeout(10_000) {
                client.getPrompt(
                    GetPromptRequest(
                        GetPromptRequestParams(
                            name = McpPrompts.FLAGSHIP,
                            arguments = mapOf("goal" to "a lending market")
                        )
                    )
                )
            }
            assertTrue(
                (ok!!.messages.single().content as TextContent).text!!.contains("a lending market"),
                "the session must still answer a bounded argument after refusing an unbounded one"
            )
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
