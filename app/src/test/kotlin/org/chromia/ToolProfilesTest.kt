package org.chromia

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.ToolProfiles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The `public` profile is the answer to one fact: a ChatGPT connector (and the
 * OpenAI tunnel) is commonly wired with no auth, so whoever has the URL can call
 * every advertised tool. Anything that acts on the operator's machine or signs
 * with their key must therefore be gone from that surface - and stay gone when
 * someone adds a tool six months from now.
 *
 * Two mechanisms enforce that, and this test pins both:
 *  - [org.chromia.tools.ToolStrategy.touchesLocalMachine] is abstract and has no
 *    default on BaseToolStrategy, so a new strategy does not COMPILE until its
 *    author classifies it.
 *  - [ToolProfiles.PUBLIC_DISABLED] - the list the server actually applies - must
 *    equal the set derived from those markers, asserted below. A tool marked as
 *    machine-touching but left out of the profile fails here.
 */
class ToolProfilesTest {

    private fun executor() = ToolExecutor(RecordingRepository(), PromptManager())

    @Test
    fun publicProfileDisablesExactlyTheMarkedStrategies() {
        assertEquals(
            executor().localMachineToolNames,
            ToolProfiles.PUBLIC_DISABLED.toSortedSet(),
            "ToolProfiles.PUBLIC_DISABLED must equal the set of strategies marked touchesLocalMachine. " +
                "A tool that acts on the local machine or uses a key belongs in the public profile's " +
                "disabled set; adding the marker without adding it here (or the reverse) is the drift " +
                "this assertion exists to catch."
        )
    }

    @Test
    fun publicProfileDisabledSetIsExactlyThisList() {
        assertEquals(
            setOf(
                "claim_testnet_tchr",
                "deploy_testnet_chain",
                "local_chain_up",
                "provision_testnet_container"
            ),
            ToolProfiles.PUBLIC_DISABLED,
            "the public surface changed - say why in the commit"
        )
    }

    @Test
    fun publicProfileKeepsTheCompilerLoopDocsExplorerAndPrompts() {
        val kept = McpTools.ALL_TOOL_NAMES - ToolProfiles.PUBLIC_DISABLED
        // The compiler loop is the product: an agent must still be able to write
        // Rell, compile it, scan it and run its tests through a public endpoint.
        listOf(
            "rell_check", "rell_security_check", "run_rell_tests", "verify_guards",
            "scaffold_dapp", "check_dapp_project", "check_ft4_imports",
            // Docs (including ChatGPT's search/fetch contract) and the help gateway.
            "search", "fetch", "fetch_docs", "chromia_help", "get_prompts",
            // Explorer queries are read-only network calls.
            "filter_blockchains", "get_all_assets", "get_network_stats", "chromia_dapp_query",
            // Deployment advice that neither writes a file nor signs anything.
            "write_deployment_config", "deployment_preflight", "verify_deployment",
            "translate_error", "onboarding_next_step", "validate_chromia_yml", "ft4_module_args"
        ).forEach { name ->
            assertTrue(name in kept, "public profile must keep $name")
        }
    }

    @Test
    fun everyDisabledToolIsARealToolOfThisServer() {
        ToolProfiles.PUBLIC_DISABLED.forEach { name ->
            assertTrue(name in McpTools.ALL_TOOL_NAMES, "$name is not a tool of this server")
        }
    }

    @Test
    fun profileResolvesFromFlagThenEnvAndRejectsUnknownNames() {
        assertEquals(ToolProfiles.FULL, ToolProfiles.resolve(flag = null, env = emptyMap()))
        assertEquals(
            ToolProfiles.PUBLIC,
            ToolProfiles.resolve(flag = null, env = mapOf(ToolProfiles.PROFILE_ENV to "public"))
        )
        assertEquals(
            ToolProfiles.PUBLIC,
            ToolProfiles.resolve(flag = "PUBLIC", env = emptyMap()),
            "profile names are case-insensitive"
        )
        // The flag wins over the environment.
        assertEquals(
            ToolProfiles.FULL,
            ToolProfiles.resolve(flag = "full", env = mapOf(ToolProfiles.PROFILE_ENV to "public"))
        )
        // A typo must never quietly serve the full toolset over a public URL.
        val e = assertThrows(IllegalArgumentException::class.java) {
            ToolProfiles.resolve(flag = "publik", env = emptyMap())
        }
        assertTrue(e.message!!.contains("Unknown profile"), e.message)
        assertThrows(IllegalArgumentException::class.java) {
            ToolProfiles.resolve(flag = null, env = mapOf(ToolProfiles.PROFILE_ENV to "wide-open"))
        }
    }

    @Test
    fun parseSseArgsAcceptsProfileAndRejectsUnknownOnes() {
        assertEquals(ToolProfiles.FULL, parseSseArgs(emptyList()).profile)
        assertEquals(ToolProfiles.PUBLIC, parseSseArgs(listOf("--profile", "public")).profile)
        val opts = parseSseArgs(listOf("--host", "0.0.0.0", "--port", "8080", "--profile", "public"))
        assertEquals("0.0.0.0", opts.host)
        assertEquals(8080, opts.port)
        assertEquals(ToolProfiles.PUBLIC, opts.profile)
        assertThrows(IllegalArgumentException::class.java) { parseSseArgs(listOf("--profile", "nope")) }
    }

    @Test
    fun profileOnlyEverAddsToTheOperatorsDisabledSet() {
        val operatorDisabled = mapOf("CHROMIA_MCP_DISABLE_TOOLS" to "search,fetch")
        val full = App.effectiveDisabledTools(operatorDisabled, ToolProfiles.FULL)
        assertEquals(setOf("search", "fetch"), full)
        val public = App.effectiveDisabledTools(operatorDisabled, ToolProfiles.PUBLIC)
        assertTrue(public.containsAll(full), "a profile must never re-enable a tool the operator disabled")
        assertEquals(full + ToolProfiles.PUBLIC_DISABLED, public)
    }

    @Test
    fun publicProfileToolsListOmitsTheDisabledToolsAndTheyRefuseWithGuidance() {
        val disabled = App.effectiveDisabledTools(emptyMap(), ToolProfiles.PUBLIC)
        val advertised = McpTools.allTools(compact = false, disabled = disabled).map { it.name }
        ToolProfiles.PUBLIC_DISABLED.forEach { name ->
            assertFalse(name in advertised, "$name must not be advertised on the public profile")
            val refusal = McpTools.disabledToolRefusal(name, disabled)
            assertTrue(refusal != null, "$name must refuse with guidance, not 'Tool not found'")
            assertTrue(
                refusal!!.contains("Run chromia-mcp locally for the full toolset"),
                refusal
            )
        }
        assertTrue("rell_check" in advertised)
        assertTrue("search" in advertised)
    }

    @Test
    fun healthAndServerInfoReportTheProfile() = runBlocking {
        assertTrue(App.healthJson(ToolProfiles.PUBLIC).contains("\"profile\": \"public\""))
        assertTrue(App.healthJson(ToolProfiles.FULL).contains("\"profile\": \"full\""))
        assertEquals(
            "Chromia MCP Server (profile: public)",
            App.serverImplementation(ToolProfiles.PUBLIC).title
        )
        assertEquals(App.SERVER_NAME, App.serverImplementation(ToolProfiles.PUBLIC).name)

        // ... and /health serves it, so a connector can see what it is talking to.
        val app = McpTestSupport.testApp()
        val server = app.runSseMcpServer(host = "127.0.0.1", port = 0, wait = false)
        HttpClient(CIO).use { http ->
            try {
                val port = server.engine.resolvedConnectors().first().port
                val response = http.get("http://127.0.0.1:$port/health")
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    App.profile,
                    Json.parseToJsonElement(response.bodyAsText()).jsonObject
                        .getValue("profile").jsonPrimitive.content
                )
            } finally {
                server.stop(500, 1000)
            }
        }
    }
}
