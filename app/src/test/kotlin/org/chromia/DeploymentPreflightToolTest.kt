package org.chromia

import org.chromia.tools.propertiesOrEmpty

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.domain.NetworkResult
import org.chromia.tools.McpTools
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.WriteDeploymentConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * deployment_preflight: catch every deployment problem BEFORE a human burns a
 * lease step or signs anything. Unit-level only: the RecordingRepository height
 * seam replaces all network I/O, and the compile/security gates run the real
 * in-process tools on tiny sources.
 */
class DeploymentPreflightToolTest {

    private val testnetBrid = WriteDeploymentConfig.TESTNET_DIRECTORY_BRID
    private val mainnetBrid = WriteDeploymentConfig.MAINNET_DIRECTORY_BRID

    private val cleanRell = "module;\n\nquery hello_world() = \"hello\";\n"

    // Transitive unauthenticated mutation - a known HIGH security finding.
    private val insecureRell = """
        module;
        entity vault { key owner: text; mutable amount: integer; }
        operation transfer(owner: text, amount: integer) {
            update vault @ { .owner == owner } ( .amount -= amount );
        }
    """.trimIndent()

    private fun yamlFor(
        target: String,
        brid: String,
        url: String = "https://node0.testnet.chromia.com:7740",
        container: String = "abc123containerlease",
        chainLine: String = "      my_chain:",
        pins: Boolean = true
    ): String = buildString {
        appendLine("blockchains:")
        appendLine("  my_chain:")
        appendLine("    module: main")
        if (pins) {
            appendLine("    config:")
            appendLine("      features:")
            appendLine("        merkle_hash_version: 2")
            appendLine("compile:")
            appendLine("  rellVersion: 0.16.1")
        }
        appendLine("deployments:")
        appendLine("  $target:")
        appendLine("    url:")
        appendLine("      - $url")
        appendLine("    brid: x\"$brid\"")
        appendLine("    container: $container")
        appendLine("    chains:")
        appendLine(chainLine)
    }

    private val testnetYaml = yamlFor("testnet", testnetBrid)
    private val mainnetYaml = yamlFor("mainnet", mainnetBrid, url = "https://system.chromaway.com")

    private val repo = RecordingRepository()

    private fun call(args: JsonObject) = runBlocking {
        ToolExecutor(repo, PromptManager())
            .executeTool(callToolRequest(name = "deployment_preflight", arguments = args))
    }

    private fun findings(s: JsonObject): List<JsonObject> =
        s["findings"]!!.jsonArray.map { it.jsonObject }

    private fun blockers(s: JsonObject): JsonArray = s["blockers"]!!.jsonArray

    // ---- ready path ----------------------------------------------------------

    @Test
    fun validTestnetBlockWithSourcesIsReady() {
        repo.nextHeight = NetworkResult.Success(42L)
        val result = call(
            buildJsonObject {
                put("yaml", testnetYaml)
                put("target", "testnet")
                put("rell", buildJsonObject { put("main.rell", cleanRell) })
            }
        )
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        assertTrue(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        assertEquals("testnet", s["target"]!!.jsonPrimitive.content)
        assertEquals("testnet", s["network"]!!.jsonPrimitive.content)
        assertTrue(blockers(s).isEmpty(), s.toString())
        // The exact deploy command, verbatim - empty chains value means CREATE.
        val next = s["nextAction"]!!.jsonPrimitive.content
        assertTrue(
            next.contains(
                "chr deployment create --settings chromia.yml --network testnet --blockchain my_chain"
            ),
            next
        )
        // The probe hit the block's own URL with the Directory Chain BRID.
        assertEquals("https://node0.testnet.chromia.com:7740", repo.lastHeightNetwork)
        assertEquals(testnetBrid, repo.lastHeightBrid.orEmpty().uppercase())
        assertTrue(
            findings(s).any {
                it["check"]!!.jsonPrimitive.content == "reachability" &&
                    it["severity"]!!.jsonPrimitive.content == "INFO"
            },
            s.toString()
        )
    }

    @Test
    fun filledChainRidEmitsUpdateCommand() {
        val dappRid = "00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA"
        val yaml = yamlFor("testnet", testnetBrid, chainLine = "      my_chain: x\"$dappRid\"")
        val result = call(
            buildJsonObject {
                put("yaml", yaml)
                put("target", "testnet")
                put("rell", cleanRell)
            }
        )
        val s = result.structuredContent!!
        assertTrue(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        assertTrue(
            s["nextAction"]!!.jsonPrimitive.content.contains(
                "chr deployment update --settings chromia.yml --network testnet --blockchain my_chain"
            ),
            s["nextAction"]!!.jsonPrimitive.content
        )
    }

    // ---- network sanity ------------------------------------------------------

    @Test
    fun wrongNetworkBridIsHighBlocker() {
        val yaml = yamlFor("testnet", mainnetBrid)
        val result = call(
            buildJsonObject {
                put("yaml", yaml)
                put("target", "testnet")
                put("rell", cleanRell)
            }
        )
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val network = findings(s).first { it["check"]!!.jsonPrimitive.content == "network" }
        assertEquals("HIGH", network["severity"]!!.jsonPrimitive.content)
        assertTrue(network["message"]!!.jsonPrimitive.content.contains("unrecoverable"))
        assertTrue(network["message"]!!.jsonPrimitive.content.contains("MAINNET Directory Chain RID"))
        assertTrue(blockers(s).any { it.jsonPrimitive.content.contains("[network]") }, s.toString())
    }

    @Test
    fun wrongNetworkUrlIsHighBlocker() {
        // Correct testnet brid, but the url is a known MAINNET node.
        val yaml = yamlFor("testnet", testnetBrid, url = "https://system.chromaway.com")
        val result = call(buildJsonObject { put("yaml", yaml); put("target", "testnet") })
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val network = findings(s).first { it["check"]!!.jsonPrimitive.content == "network" }
        assertEquals("HIGH", network["severity"]!!.jsonPrimitive.content)
        assertTrue(network["message"]!!.jsonPrimitive.content.contains("mainnet node"))
    }

    // ---- reachability --------------------------------------------------------

    @Test
    fun unreachableUrlIsBlockerWithClassifiedHint() {
        repo.nextHeight = NetworkResult.Error("Connection refused: node0.testnet.chromia.com")
        val result = call(buildJsonObject { put("yaml", testnetYaml); put("target", "testnet") })
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val reach = findings(s).first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertEquals("BLOCKER", reach["severity"]!!.jsonPrimitive.content)
        assertTrue(reach["message"]!!.jsonPrimitive.content.contains("could not be reached"))
    }

    @Test
    fun unknownDirectoryBridAnswerIsClassifiedAsWrongNetworkHint() {
        repo.nextHeight = NetworkResult.Error("Can't find blockchain with blockchainRID: $testnetBrid")
        val result = call(buildJsonObject { put("yaml", testnetYaml); put("target", "testnet") })
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val reach = findings(s).first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertTrue(reach["message"]!!.jsonPrimitive.content.contains("do not serve this BRID"))
    }

    @Test
    fun secondUrlAnswersWhenFirstIsDown() {
        val yaml = buildString {
            append(testnetYaml.substringBefore("    url:"))
            appendLine("    url:")
            appendLine("      - https://node0.testnet.chromia.com:7740")
            appendLine("      - https://node1.testnet.chromia.com:7740")
            append(testnetYaml.substringAfter("- https://node0.testnet.chromia.com:7740\n"))
        }
        repo.heightQueue.addAll(
            listOf(NetworkResult.Error("connect timed out"), NetworkResult.Success(7L))
        )
        val result = call(buildJsonObject { put("yaml", yaml); put("target", "testnet") })
        val s = result.structuredContent!!
        assertTrue(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        assertEquals(2, repo.heightCalls)
        val reach = findings(s).first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertTrue(reach["message"]!!.jsonPrimitive.content.contains("node1.testnet.chromia.com"))
    }

    // ---- source gate ---------------------------------------------------------

    @Test
    fun mainnetHighSecurityFindingBlocks() {
        val result = call(
            buildJsonObject {
                put("yaml", mainnetYaml)
                put("target", "mainnet")
                put("rell", buildJsonObject { put("main.rell", insecureRell) })
            }
        )
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val security = findings(s).first {
            it["check"]!!.jsonPrimitive.content == "security" &&
                it["message"]!!.jsonPrimitive.content.contains("unauthenticated-mutation")
        }
        assertEquals("BLOCKER", security["severity"]!!.jsonPrimitive.content)
        assertTrue(blockers(s).any { it.jsonPrimitive.content.contains("[security]") }, s.toString())
    }

    @Test
    fun sameSecurityFindingIsWarningForTestnet() {
        val result = call(
            buildJsonObject {
                put("yaml", testnetYaml)
                put("target", "testnet")
                put("rell", buildJsonObject { put("main.rell", insecureRell) })
            }
        )
        val s = result.structuredContent!!
        assertTrue(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val security = findings(s).first {
            it["check"]!!.jsonPrimitive.content == "security" &&
                it["message"]!!.jsonPrimitive.content.contains("unauthenticated-mutation")
        }
        assertEquals("WARNING", security["severity"]!!.jsonPrimitive.content)
        assertTrue(
            s["notes"]!!.jsonPrimitive.content.contains("would BLOCK a mainnet"),
            s["notes"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun compileErrorBlocksAnyTarget() {
        val result = call(
            buildJsonObject {
                put("yaml", testnetYaml)
                put("target", "testnet")
                put("rell", "module;\n\nquery broken() = unknown_thing;\n")
            }
        )
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        assertTrue(
            findings(s).any {
                it["check"]!!.jsonPrimitive.content == "source" &&
                    it["severity"]!!.jsonPrimitive.content == "BLOCKER"
            },
            s.toString()
        )
    }

    @Test
    fun mainnetWithoutRellIsBlockedOnSourceGate() {
        val result = call(buildJsonObject { put("yaml", mainnetYaml); put("target", "mainnet") })
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val gate = findings(s).first { it["check"]!!.jsonPrimitive.content == "source_gate" }
        assertEquals("BLOCKER", gate["severity"]!!.jsonPrimitive.content)
        assertTrue(gate["message"]!!.jsonPrimitive.content.contains("MAINNET"))
    }

    @Test
    fun testnetWithoutRellIsReadyWithHonestSkipNote() {
        val result = call(buildJsonObject { put("yaml", testnetYaml); put("target", "testnet") })
        val s = result.structuredContent!!
        assertTrue(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        assertTrue(
            s["notes"]!!.jsonPrimitive.content.contains("Source gate SKIPPED"),
            s["notes"]!!.jsonPrimitive.content
        )
    }

    // ---- pins ----------------------------------------------------------------

    @Test
    fun tooNewRellVersionPinBlocks() {
        val yaml = testnetYaml.replace("rellVersion: 0.16.1", "rellVersion: 0.99.0")
        val result = call(buildJsonObject { put("yaml", yaml); put("target", "testnet") })
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val pin = findings(s).first {
            it["check"]!!.jsonPrimitive.content == "chromia_yml" &&
                it["message"]!!.jsonPrimitive.content.contains("rellVersion")
        }
        assertEquals("BLOCKER", pin["severity"]!!.jsonPrimitive.content)
        assertTrue(pin["message"]!!.jsonPrimitive.content.contains("newer"))
    }

    @Test
    fun mainnetMissingPinsBlockByDefaultButNotWithStrictFalse() {
        val yaml = yamlFor("mainnet", mainnetBrid, url = "https://system.chromaway.com", pins = false)
        val args = buildJsonObject {
            put("yaml", yaml)
            put("target", "mainnet")
            put("rell", cleanRell)
        }
        val strictDefault = call(args).structuredContent!!
        assertFalse(strictDefault["ready"]!!.jsonPrimitive.boolean, strictDefault.toString())
        val pinBlockers = blockers(strictDefault).map { it.jsonPrimitive.content }
        assertTrue(pinBlockers.any { it.contains("rellVersion") }, pinBlockers.toString())
        assertTrue(pinBlockers.any { it.contains("merkle_hash_version") }, pinBlockers.toString())

        val relaxed = call(
            buildJsonObject {
                put("yaml", yaml)
                put("target", "mainnet")
                put("rell", cleanRell)
                put("strict", false)
            }
        ).structuredContent!!
        assertTrue(relaxed["ready"]!!.jsonPrimitive.boolean, relaxed.toString())
    }

    // ---- deployment block validity -------------------------------------------

    @Test
    fun missingTargetIsBlockerNamingAvailableTargets() {
        val result = call(buildJsonObject { put("yaml", testnetYaml); put("target", "prod") })
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val target = findings(s).first { it["check"]!!.jsonPrimitive.content == "target" }
        assertEquals("BLOCKER", target["severity"]!!.jsonPrimitive.content)
        assertTrue(target["message"]!!.jsonPrimitive.content.contains("available: testnet"))
        // No probe without a target block.
        assertEquals(0, repo.heightCalls)
    }

    @Test
    fun placeholderContainerBlocks() {
        val yaml = yamlFor("testnet", testnetBrid, container = "<containerIID>")
        val result = call(buildJsonObject { put("yaml", yaml); put("target", "testnet") })
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val container = findings(s).first { it["check"]!!.jsonPrimitive.content == "container" }
        assertEquals("BLOCKER", container["severity"]!!.jsonPrimitive.content)
        assertTrue(container["message"]!!.jsonPrimitive.content.contains("placeholder"))
        assertTrue(container["fix"]!!.jsonPrimitive.content.contains("vault"), container.toString())
    }

    @Test
    fun chainNotInBlockchainsBlocks() {
        val yaml = yamlFor("testnet", testnetBrid, chainLine = "      ghost_chain:")
        val result = call(buildJsonObject { put("yaml", yaml); put("target", "testnet") })
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val chains = findings(s).first { it["check"]!!.jsonPrimitive.content == "chains" }
        assertTrue(chains["message"]!!.jsonPrimitive.content.contains("ghost_chain does not match"))
    }

    @Test
    fun unparsableYamlIsSingleBlockerNotACrash() {
        val result = call(
            buildJsonObject { put("yaml", "deployments:\n      bad indent: here\n  x: y"); put("target", "testnet") }
        )
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        assertEquals("unknown", s["network"]!!.jsonPrimitive.content)
        assertTrue(
            findings(s).any { it["check"]!!.jsonPrimitive.content == "yaml" },
            s.toString()
        )
    }

    // ---- validation ----------------------------------------------------------

    @Test
    fun missingYamlAndTargetAreValidationErrors() {
        val noYaml = call(buildJsonObject { put("target", "testnet") })
        assertEquals(true, noYaml.isError)
        assertTrue((noYaml.content.first() as TextContent).text!!.contains("yaml"))

        val noTarget = call(buildJsonObject { put("yaml", testnetYaml) })
        assertEquals(true, noTarget.isError)
        assertTrue((noTarget.content.first() as TextContent).text!!.contains("target"))
    }

    // ---- schema + compact mode ----------------------------------------------

    @Test
    fun advertisedInFullAndCompactMode() {
        val full = McpTools.allTools(compact = false).map { it.name }
        val compact = McpTools.allTools(compact = true).map { it.name }
        assertTrue("deployment_preflight" in full)
        assertTrue(
            "deployment_preflight" in compact,
            "deployment_preflight is cheap+high-value: compact mode must keep it"
        )
    }

    @Test
    fun toolSchemaDeclaresRequiredInputsAndOutputShape() {
        val tool = McpTools.deploymentPreflightTool()
        assertEquals("deployment_preflight", tool.name)
        assertEquals(listOf("yaml", "target"), tool.inputSchema.required)
        listOf("yaml", "target", "rell", "files", "strict")
            .forEach { assertNotNull(tool.inputSchema.propertiesOrEmpty[it], "inputSchema missing $it") }
        val out = tool.outputSchema!!
        listOf("ready", "target", "network", "findings", "blockers", "nextAction", "notes")
            .forEach { assertNotNull(out.propertiesOrEmpty[it], "outputSchema missing $it") }
        // The policy is part of the contract: read-only, never signs.
        assertTrue(tool.description!!.contains("no signing"))
        // Honest ready semantics: only a MAINNET target is blocked on a missing
        // source gate; other targets can be ready with the skip called out in notes.
        assertTrue(
            tool.description!!.contains("MAINNET target without `rell` stays blocked"),
            tool.description
        )
        assertTrue(tool.description!!.contains("called out in notes"), tool.description)
    }

    // ---- `files` alias for `rell` -------------------------------------------

    @Test
    fun filesIsAcceptedAsAnAliasForRell() {
        // An agent porting a rell_check/run_rell_tests call sends `files`; a
        // silently ignored `files` would skip the source gate and still say
        // ready:true on testnet. The alias must run the gate and be noted.
        repo.nextHeight = NetworkResult.Success(42L)
        val result = call(
            buildJsonObject {
                put("yaml", testnetYaml)
                put("target", "testnet")
                put("files", buildJsonObject { put("main.rell", insecureRell) })
            }
        )
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        // The source gate ran on the aliased sources: the HIGH security finding shows up.
        assertTrue(
            findings(s).any { it["check"]!!.jsonPrimitive.content == "security" },
            s.toString()
        )
        val notes = s["notes"]!!.jsonPrimitive.content
        assertTrue(notes.contains("`files` was accepted as an alias"), notes)
        assertFalse(notes.contains("Source gate SKIPPED"), notes)
    }

    @Test
    fun rellWinsWhenBothRellAndFilesArePresent() {
        repo.nextHeight = NetworkResult.Success(42L)
        val result = call(
            buildJsonObject {
                put("yaml", testnetYaml)
                put("target", "testnet")
                put("rell", buildJsonObject { put("main.rell", cleanRell) })
                // The alias carries uncompilable code - it must be ignored.
                put("files", buildJsonObject { put("main.rell", "module; query broken(") })
            }
        )
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        assertTrue(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        assertTrue(blockers(s).isEmpty(), s.toString())
        // No alias note: `rell` was used, `files` ignored.
        val notes = s["notes"]!!.jsonPrimitive.content
        assertFalse(notes.contains("alias"), notes)
    }

    // ---- reality audit D4: unresolved !include must not pass the mainnet gate

    @Test
    fun mainnetLibsIncludeBlocksBecauseTheIncludedFileWasNotValidated() {
        repo.nextHeight = NetworkResult.Success(42L)
        val result = call(
            buildJsonObject {
                put("yaml", mainnetYaml + "libs: !include libs.yml\n")
                put("target", "mainnet")
                put("rell", buildJsonObject { put("main.rell", cleanRell) })
            }
        )
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val finding = findings(s).first {
            it["check"]!!.jsonPrimitive.content == "chromia_yml" &&
                it["message"]!!.jsonPrimitive.content.contains("!include")
        }
        assertEquals("BLOCKER", finding["severity"]!!.jsonPrimitive.content)
        assertTrue(
            finding["message"]!!.jsonPrimitive.content.contains("libs.yml"),
            finding.toString()
        )
    }

    @Test
    fun testnetLibsIncludeWarnsButDoesNotBlock() {
        repo.nextHeight = NetworkResult.Success(42L)
        val result = call(
            buildJsonObject {
                put("yaml", testnetYaml + "libs: !include libs.yml\n")
                put("target", "testnet")
                put("rell", buildJsonObject { put("main.rell", cleanRell) })
            }
        )
        val s = result.structuredContent!!
        assertTrue(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val finding = findings(s).first {
            it["check"]!!.jsonPrimitive.content == "chromia_yml" &&
                it["message"]!!.jsonPrimitive.content.contains("!include")
        }
        assertEquals("WARNING", finding["severity"]!!.jsonPrimitive.content)
    }
}
