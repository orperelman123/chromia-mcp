package org.chromia

import io.modelcontextprotocol.kotlin.sdk.CallToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.ArgumentVocabulary
import org.chromia.tools.DappScaffold
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AUDIT F10 (2026-09-06) - three names for "the Rell source", two for "the
 * yaml", six for "the chain id", two for "the target", measured across all 74
 * schemas:
 *
 *   Rell source (single) `source` (rell_check, rell_security_check) vs `rell`
 *                        (check_dapp_project, check_ft4_imports,
 *                        deployment_preflight, deploy_testnet_chain)
 *   Rell sources (map)   `files` (8 tools) - and deployment_preflight accepted
 *                        `files` only as a DEPRECATING alias: "`files` was
 *                        accepted as an alias for the `rell` parameter - prefer
 *                        `rell` in future calls"
 *   chromia.yml          `yaml` (3 tools) vs `chromiaYml` (deploy_testnet_chain)
 *   chain identifier     `rid`, `brid`, `brids`, `blockchainRid`,
 *                        `blockchainIds`, `blockchain`
 *   deployment target    `target` (deployment_preflight) vs `network` (22 tools)
 *
 * "An agent guesses `source` for deployment_preflight and `rell` for
 * rell_check, one wasted call per tool, forever" - and deployment_preflight's
 * own note actively told the agent to STOP using the name it uses everywhere
 * else.
 *
 * The canonical name is the one the majority already used, so nothing an agent
 * has learned stops working, and every other spelling is still accepted with no
 * warning.
 */
class ArgumentVocabularyTest {

    private val executor = ToolExecutor(RecordingRepository(), PromptManager())

    private fun call(name: String, args: kotlinx.serialization.json.JsonObject) = runBlocking {
        executor.executeTool(CallToolRequest(name = name, arguments = args))
    }

    @Test
    fun everyToolSchemaNamesTheCanonicalArgumentFirst() {
        val violations = McpTools.allTools().mapNotNull { tool ->
            ArgumentVocabulary.firstViolation(tool.inputSchema.properties.keys.toList())
                ?.let { "${tool.name} $it" }
        }
        assertTrue(
            violations.isEmpty(),
            "one concept, one canonical name, declared first:\n" + violations.joinToString("\n")
        )
    }

    @Test
    fun noRequiredParameterIsAnAliasOfADeclaredCanonicalName() {
        val violations = McpTools.allTools().flatMap { tool ->
            val declared = tool.inputSchema.properties.keys
            tool.inputSchema.required.orEmpty().mapNotNull { required ->
                val concept = ArgumentVocabulary.conceptOf(required) ?: return@mapNotNull null
                if (required == concept.canonical || concept.canonical !in declared) null
                else "${tool.name} requires the alias `$required` while declaring `${concept.canonical}`"
            }
        }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    // ---- the aliases still work, with no warning -----------------------------

    private val scaffold = DappScaffold.files("vocab_demo")
    private val yaml = scaffold.getValue("chromia.yml")
    private val main = scaffold.getValue("src/main.rell")

    @Test
    fun everySpellingOfTheRellSourcesIsAcceptedWithNoNag() {
        listOf("files", "rell", "source").forEach { spelling ->
            val result = call(
                "check_dapp_project",
                buildJsonObject {
                    put("yaml", yaml)
                    put(spelling, buildJsonObject { put("src/main.rell", main) })
                }
            )
            assertTrue(result.isError != true, "$spelling: ${result.structuredContent}")
            val notes = result.structuredContent!!["notes"]!!.jsonPrimitive.content
            assertFalse(notes.contains("prefer `"), "$spelling was nagged about: $notes")
            assertFalse(notes.contains("accepted as an alias"), "$spelling was nagged about: $notes")
        }
    }

    @Test
    fun deploymentPreflightTakesNetworkAndStillTakesTarget() {
        val withDeployments = yaml.trimEnd() + "\n\n" + buildString {
            appendLine("deployments:")
            appendLine("  testnet:")
            appendLine("    url:")
            appendLine("      - https://node0.testnet.chromia.com:7740")
            appendLine("    brid: x\"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92\"")
            appendLine("    container: real_lease_9f2c")
        }
        val viaNetwork = call(
            "deployment_preflight",
            buildJsonObject { put("yaml", withDeployments); put("network", "testnet") }
        ).structuredContent!!
        val viaTarget = call(
            "deployment_preflight",
            buildJsonObject { put("yaml", withDeployments); put("target", "testnet") }
        ).structuredContent!!
        assertEquals("testnet", viaNetwork["target"]!!.jsonPrimitive.content)
        assertEquals(
            viaTarget["blockers"].toString(),
            viaNetwork["blockers"].toString(),
            "`network` and `target` are the same argument"
        )
    }

    @Test
    fun theChainIdIsBridEverywhereAndTheOldNamesStillResolve() {
        val details = McpTools.allTools().single { it.name == "get_blockchain_details" }
        assertNotNull(details.inputSchema.properties["brid"])
        assertNotNull(details.inputSchema.properties["rid"], "`rid` must keep working")
        assertEquals(listOf("brid"), details.inputSchema.required)

        val query = McpTools.allTools().single { it.name == "chromia_dapp_query" }
        assertNotNull(query.inputSchema.properties["brid"])
        assertNotNull(query.inputSchema.properties["blockchainRid"])
        assertEquals(listOf("brid"), query.inputSchema.required)

        // A missing chain id names the canonical spelling, not one of the six.
        val missing = call("get_blockchain_details", buildJsonObject { })
        assertTrue(missing.isError == true)
        assertTrue(
            missing.structuredContent!!["error"]!!.jsonPrimitive.content
                .contains("Missing required parameter: brid"),
            missing.structuredContent.toString()
        )
    }

    @Test
    fun theDeprecatingNoteThatPushedAgentsTheWrongWayIsGone() {
        // "`files` was accepted as an alias for the `rell` parameter - prefer
        // `rell` in future calls" - the one message in the server that told an
        // agent to stop using the name eight other tools take.
        val hits = McpTools.allTools().count { tool ->
            (tool.description.orEmpty() + tool.inputSchema.properties.toString()).contains("prefer `rell`")
        }
        assertEquals(0, hits, "no schema may still push an agent away from the canonical name")
    }

    @Test
    fun undeclaredArgumentsAreStillRefusedWithTheAcceptedNames() {
        // The vocabulary is wider, not looser: a name nothing reads is still a
        // refusal naming what IS read, because an undeclared argument is never
        // honoured.
        val result = call(
            "run_rell_tests",
            buildJsonObject {
                put("files", buildJsonObject { put("main.rell", main) })
                put("module_args", buildJsonObject { })
            }
        )
        assertTrue(result.isError == true)
        val text = result.structuredContent!!["error"]!!.jsonPrimitive.content
        assertTrue(text.contains("module_args"), text)
        assertTrue(text.contains("moduleArgs"), text)
    }
}
