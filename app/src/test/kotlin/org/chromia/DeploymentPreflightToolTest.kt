package org.chromia

import org.chromia.tools.propertiesOrEmpty

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
import org.chromia.domain.ChromiaRepository
import org.chromia.tools.DeploymentPreflight
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
 * lease step or signs anything.
 *
 * ZERO DOUBLES (2026-09-07). The reachability probe used to be answered by
 * `RecordingRepository` - a double of our own `ChromiaRepository` that the test
 * told what height to report, and in what order. It is gone, and every test here
 * now names which REAL thing answers it:
 *
 *  - **the live testnet** ([LiveChromia.repository]) against the official
 *    testnet node URL. The three tests that are ABOUT reachability live there,
 *    because only a real node can say whether it serves a BRID;
 *  - **a real closed port** ([McpTestSupport.offlineRepository], with
 *    127.0.0.1:1 in the yaml) where the claim is that an unreachable URL is a
 *    blocker, or where the tool layer (aliases, validation, a missing target)
 *    is the subject and the probe is incidental;
 *  - **no probe at all** for the checks that never had a network in them - yaml
 *    validity, pins, the compile/security source gate, network classification.
 *    `DeploymentPreflight.run(..., probe = null)` is production behaviour, not a
 *    stand-in: it is the documented mode for "no probe available", it records
 *    the skip in notes, and it never claims reachability was checked.
 *
 * The compile and security gates run the real in-process tools on real sources,
 * as they always did.
 */
class DeploymentPreflightToolTest {

    private val testnetBrid = WriteDeploymentConfig.TESTNET_DIRECTORY_BRID
    private val mainnetBrid = WriteDeploymentConfig.MAINNET_DIRECTORY_BRID

    /**
     * A real address nothing listens on - the same closed loopback port
     * [McpTestSupport.offlineRepository] points at. A probe against it fails
     * with the operating system's refusal, not with a message a test wrote.
     */
    private val closedPortUrl = "http://127.0.0.1:1"

    /** The official testnet node, which really does serve the testnet Directory chain. */
    private val liveTestnetUrl = WriteDeploymentConfig.TESTNET_URL

    private val cleanRell = "module;\n\nquery hello_world() = \"hello\";\n"

    // Transitive unauthenticated mutation - a known HIGH security finding.
    private val insecureRell = """
        module;
        entity vault { key owner: text; mutable amount: integer; }
        operation transfer(owner: text, amount: integer) {
            update vault @ { .owner == owner } ( .amount -= amount );
        }
    """.trimIndent()

    private companion object {
        /** ONE production repository for the whole class; the live tests share it. */
        val liveRepository by lazy { LiveChromia.repository() }
    }

    private fun yamlFor(
        target: String,
        brid: String,
        urls: List<String> = listOf(closedPortUrl),
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
        urls.forEach { appendLine("      - $it") }
        appendLine("    brid: x\"$brid\"")
        appendLine("    container: $container")
        appendLine("    chains:")
        appendLine(chainLine)
    }

    private val testnetYaml = yamlFor("testnet", testnetBrid)
    private val mainnetYaml =
        yamlFor("mainnet", mainnetBrid, urls = listOf("https://system.chromaway.com"))

    /** The tool, driven by whichever REAL repository the test names. */
    private fun call(repository: ChromiaRepository, args: JsonObject) = runBlocking {
        ToolExecutor(repository, PromptManager())
            .executeTool(callToolRequest(name = "deployment_preflight", arguments = args))
    }

    /**
     * The production logic with NO probe - the documented "no probe available"
     * mode. Used by every check that has no network in it; reachability itself
     * is proved live further down.
     */
    private fun preflightWithoutProbe(
        yaml: String,
        target: String,
        rell: Map<String, String>? = null,
        strict: Boolean? = null
    ): DeploymentPreflight.Result = runBlocking {
        DeploymentPreflight.run(yaml, target, rell, strict, null)
    }

    private fun findings(s: JsonObject): List<JsonObject> =
        s["findings"]!!.jsonArray.map { it.jsonObject }

    private fun blockers(s: JsonObject): JsonArray = s["blockers"]!!.jsonArray

    private fun DeploymentPreflight.Result.finding(check: String): DeploymentPreflight.Finding =
        findings.first { it.check == check }

    private val DeploymentPreflight.Result.noteText: String get() = notes.joinToString(" ")

    // ---- reachability, against real nodes ------------------------------------

    /**
     * THE READY PATH, END TO END, WITH A REAL NODE ANSWERING.
     *
     * Replaces validTestnetBlockWithSourcesIsReady, which asserted that the
     * recorder had been asked for a height with the block's URL and the
     * Directory BRID - a restatement of the call. Here the official testnet node
     * really answers for the real testnet Directory Chain, so the INFO finding
     * carries a height nobody in this test chose.
     */
    @Test
    fun liveValidTestnetBlockWithSourcesIsReady() {
        LiveChromia.requireLive("probes the official testnet node for the Directory Chain height")
        val result = call(
            liveRepository,
            buildJsonObject {
                put("yaml", yamlFor("testnet", testnetBrid, urls = listOf(liveTestnetUrl)))
                put("target", "testnet")
                put("rell", buildJsonObject { put("main.rell", cleanRell) })
            }
        )
        assertTrue(result.isError != true, (result.content.first() as TextContent).text)
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
        val reach = findings(s).first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertEquals("INFO", reach["severity"]!!.jsonPrimitive.content, s.toString())
        val message = reach["message"]!!.jsonPrimitive.content
        assertTrue(message.contains(liveTestnetUrl), message)
        assertTrue(message.contains("height"), message)
    }

    /**
     * FAILOVER, WITH A REAL DEAD URL FIRST.
     *
     * Replaces secondUrlAnswersWhenFirstIsDown, which queued an error then a
     * success on the recorder. The first URL here is a real closed port (the
     * operating system refuses it) and the second is the real testnet node, so
     * the failover is the tool's, over two genuinely different outcomes.
     */
    @Test
    fun liveSecondUrlAnswersWhenTheFirstIsARealClosedPort() {
        LiveChromia.requireLive("falls over from a closed port to the official testnet node")
        val result = call(
            liveRepository,
            buildJsonObject {
                put("yaml", yamlFor("testnet", testnetBrid, urls = listOf(closedPortUrl, liveTestnetUrl)))
                put("target", "testnet")
            }
        )
        val s = result.structuredContent!!
        assertTrue(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val reach = findings(s).first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertEquals("INFO", reach["severity"]!!.jsonPrimitive.content, s.toString())
        assertTrue(reach["message"]!!.jsonPrimitive.content.contains(liveTestnetUrl), s.toString())
    }

    /**
     * A WRONG-NETWORK BRID, AND THE NODE'S OWN VERDICT ON IT.
     *
     * Merges two recorder tests: wrongNetworkBridIsHighBlocker (the local
     * classification) and unknownDirectoryBridAnswerIsClassifiedAsWrongNetworkHint
     * (which typed out "Can't find blockchain with blockchainRID: ..." and then
     * asserted our classifier's reading of that sentence). Both are one real
     * question here: point a testnet target at the MAINNET Directory BRID and
     * ask an official testnet node about it. The node answers - 2026-09-07,
     * "Unknown blockchain 0x7e5b..." - and the tool has to turn THAT into the
     * do-not-serve hint.
     */
    @Test
    fun liveWrongNetworkBridIsHighBlockerAndTheNodeSaysItDoesNotServeIt() {
        LiveChromia.requireLive("asks an official testnet node about the MAINNET Directory Chain BRID")
        val result = call(
            liveRepository,
            buildJsonObject {
                put("yaml", yamlFor("testnet", mainnetBrid, urls = listOf(liveTestnetUrl)))
                put("target", "testnet")
                put("rell", buildJsonObject { put("main.rell", cleanRell) })
            }
        )
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val network = findings(s).first { it["check"]!!.jsonPrimitive.content == "network" }
        assertEquals("HIGH", network["severity"]!!.jsonPrimitive.content)
        assertTrue(network["message"]!!.jsonPrimitive.content.contains("unrecoverable"))
        assertTrue(network["message"]!!.jsonPrimitive.content.contains("MAINNET Directory Chain RID"))
        assertTrue(blockers(s).any { it.jsonPrimitive.content.contains("[network]") }, s.toString())
        // And the node's own answer, classified.
        val reach = findings(s).first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertEquals("BLOCKER", reach["severity"]!!.jsonPrimitive.content, s.toString())
        assertTrue(
            reach["message"]!!.jsonPrimitive.content.contains("do not serve this BRID"),
            reach.toString()
        )
    }

    /**
     * A URL THAT REALLY IS NOT THERE. The refusal comes from the operating
     * system ("Connection Refused ... getsockopt"), and failureHint has to
     * classify that wording rather than one this test invented.
     */
    @Test
    fun offlineUrlIsBlockerWithClassifiedHint() {
        val result = call(
            McpTestSupport.offlineRepository(),
            buildJsonObject { put("yaml", testnetYaml); put("target", "testnet") }
        )
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val reach = findings(s).first { it["check"]!!.jsonPrimitive.content == "reachability" }
        assertEquals("BLOCKER", reach["severity"]!!.jsonPrimitive.content)
        assertTrue(reach["message"]!!.jsonPrimitive.content.contains("could not be reached"))
    }

    // ---- deployment block validity (no network in the claim) -----------------

    @Test
    fun filledChainRidEmitsUpdateCommand() {
        val dappRid = "00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA00AA"
        val yaml = yamlFor("testnet", testnetBrid, chainLine = "      my_chain: x\"$dappRid\"")
        val result = preflightWithoutProbe(yaml, "testnet", mapOf("main.rell" to cleanRell))
        assertTrue(result.ready, result.findings.toString())
        assertTrue(
            result.nextAction.contains(
                "chr deployment update --settings chromia.yml --network testnet --blockchain my_chain"
            ),
            result.nextAction
        )
    }

    @Test
    fun wrongNetworkUrlIsHighBlocker() {
        // Correct testnet brid, but the url is a known MAINNET node.
        val yaml = yamlFor("testnet", testnetBrid, urls = listOf("https://system.chromaway.com"))
        val result = preflightWithoutProbe(yaml, "testnet")
        assertFalse(result.ready, result.findings.toString())
        val network = result.finding("network")
        assertEquals("HIGH", network.severity)
        assertTrue(network.message.contains("mainnet node"), network.message)
    }

    @Test
    fun missingTargetIsBlockerNamingAvailableTargets() {
        // Through the tool with the closed port: a target block that is not
        // there must not be probed at all, and the ABSENCE of any reachability
        // finding is how that shows - the recorder's call count was a proxy for
        // exactly this.
        val result = call(
            McpTestSupport.offlineRepository(),
            buildJsonObject { put("yaml", testnetYaml); put("target", "prod") }
        )
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        val target = findings(s).first { it["check"]!!.jsonPrimitive.content == "target" }
        assertEquals("BLOCKER", target["severity"]!!.jsonPrimitive.content)
        assertTrue(target["message"]!!.jsonPrimitive.content.contains("available: testnet"))
        assertTrue(
            findings(s).none { it["check"]!!.jsonPrimitive.content == "reachability" },
            "no target block means nothing to probe: $s"
        )
    }

    @Test
    fun placeholderContainerBlocks() {
        val result = preflightWithoutProbe(
            yamlFor("testnet", testnetBrid, container = "<containerIID>"),
            "testnet"
        )
        assertFalse(result.ready, result.findings.toString())
        val container = result.finding("container")
        assertEquals("BLOCKER", container.severity)
        assertTrue(container.message.contains("placeholder"), container.message)
        assertTrue(container.fix.contains("vault"), container.fix)
    }

    @Test
    fun chainNotInBlockchainsBlocks() {
        val result = preflightWithoutProbe(
            yamlFor("testnet", testnetBrid, chainLine = "      ghost_chain:"),
            "testnet"
        )
        assertFalse(result.ready, result.findings.toString())
        assertTrue(
            result.finding("chains").message.contains("ghost_chain does not match"),
            result.finding("chains").message
        )
    }

    @Test
    fun unparsableYamlIsSingleBlockerNotACrash() {
        val result = call(
            McpTestSupport.offlineRepository(),
            buildJsonObject {
                put("yaml", "deployments:\n      bad indent: here\n  x: y")
                put("target", "testnet")
            }
        )
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        assertFalse(s["ready"]!!.jsonPrimitive.boolean, s.toString())
        assertEquals("unknown", s["network"]!!.jsonPrimitive.content)
        assertTrue(findings(s).any { it["check"]!!.jsonPrimitive.content == "yaml" }, s.toString())
    }

    // ---- source gate ---------------------------------------------------------

    @Test
    fun mainnetHighSecurityFindingBlocks() {
        val result = preflightWithoutProbe(mainnetYaml, "mainnet", mapOf("main.rell" to insecureRell))
        assertFalse(result.ready, result.findings.toString())
        val security = result.findings.first {
            it.check == "security" && it.message.contains("unauthenticated-mutation")
        }
        assertEquals("BLOCKER", security.severity)
        assertTrue(result.blockers.any { it.contains("[security]") }, result.blockers.toString())
    }

    @Test
    fun sameSecurityFindingIsWarningForTestnet() {
        val result = preflightWithoutProbe(testnetYaml, "testnet", mapOf("main.rell" to insecureRell))
        assertTrue(result.ready, result.findings.toString())
        val security = result.findings.first {
            it.check == "security" && it.message.contains("unauthenticated-mutation")
        }
        assertEquals("WARNING", security.severity)
        assertTrue(result.noteText.contains("would BLOCK a mainnet"), result.noteText)
    }

    @Test
    fun compileErrorBlocksAnyTarget() {
        val result = preflightWithoutProbe(
            testnetYaml,
            "testnet",
            mapOf("main.rell" to "module;\n\nquery broken() = unknown_thing;\n")
        )
        assertFalse(result.ready, result.findings.toString())
        assertTrue(
            result.findings.any { it.check == "source" && it.severity == "BLOCKER" },
            result.findings.toString()
        )
    }

    @Test
    fun mainnetWithoutRellIsBlockedOnSourceGate() {
        val result = preflightWithoutProbe(mainnetYaml, "mainnet")
        assertFalse(result.ready, result.findings.toString())
        val gate = result.finding("source_gate")
        assertEquals("BLOCKER", gate.severity)
        assertTrue(gate.message.contains("MAINNET"), gate.message)
    }

    @Test
    fun testnetWithoutRellIsReadyWithHonestSkipNote() {
        val result = preflightWithoutProbe(testnetYaml, "testnet")
        assertTrue(result.ready, result.findings.toString())
        assertTrue(result.noteText.contains("Source gate SKIPPED"), result.noteText)
    }

    // ---- pins ----------------------------------------------------------------

    @Test
    fun tooNewRellVersionPinBlocks() {
        val yaml = testnetYaml.replace("rellVersion: 0.16.1", "rellVersion: 0.99.0")
        val result = preflightWithoutProbe(yaml, "testnet")
        assertFalse(result.ready, result.findings.toString())
        val pin = result.findings.first {
            it.check == "chromia_yml" && it.message.contains("rellVersion")
        }
        assertEquals("BLOCKER", pin.severity)
        assertTrue(pin.message.contains("newer"), pin.message)
    }

    @Test
    fun mainnetMissingPinsBlockByDefaultButNotWithStrictFalse() {
        val yaml = yamlFor(
            "mainnet", mainnetBrid, urls = listOf("https://system.chromaway.com"), pins = false
        )
        val strictDefault = preflightWithoutProbe(yaml, "mainnet", mapOf("main.rell" to cleanRell))
        assertFalse(strictDefault.ready, strictDefault.findings.toString())
        assertTrue(
            strictDefault.blockers.any { it.contains("rellVersion") },
            strictDefault.blockers.toString()
        )
        assertTrue(
            strictDefault.blockers.any { it.contains("merkle_hash_version") },
            strictDefault.blockers.toString()
        )

        val relaxed = preflightWithoutProbe(
            yaml, "mainnet", mapOf("main.rell" to cleanRell), strict = false
        )
        assertTrue(relaxed.ready, relaxed.findings.toString())
    }

    // ---- reality audit D4: unresolved !include must not pass the mainnet gate

    @Test
    fun mainnetLibsIncludeBlocksBecauseTheIncludedFileWasNotValidated() {
        val result = preflightWithoutProbe(
            mainnetYaml + "libs: !include libs.yml\n", "mainnet", mapOf("main.rell" to cleanRell)
        )
        assertFalse(result.ready, result.findings.toString())
        val finding = result.findings.first {
            it.check == "chromia_yml" && it.message.contains("!include")
        }
        assertEquals("BLOCKER", finding.severity)
        assertTrue(finding.message.contains("libs.yml"), finding.message)
    }

    @Test
    fun testnetLibsIncludeWarnsButDoesNotBlock() {
        val result = preflightWithoutProbe(
            testnetYaml + "libs: !include libs.yml\n", "testnet", mapOf("main.rell" to cleanRell)
        )
        assertTrue(result.ready, result.findings.toString())
        val finding = result.findings.first {
            it.check == "chromia_yml" && it.message.contains("!include")
        }
        assertEquals("WARNING", finding.severity)
    }

    // ---- validation ----------------------------------------------------------

    @Test
    fun missingYamlAndTargetAreValidationErrors() {
        val offline = McpTestSupport.offlineRepository()
        val noYaml = call(offline, buildJsonObject { put("target", "testnet") })
        assertEquals(true, noYaml.isError)
        assertTrue((noYaml.content.first() as TextContent).text!!.contains("yaml"))

        val noTarget = call(offline, buildJsonObject { put("yaml", testnetYaml) })
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
        // AUDIT F10: "deployment target `target` (deployment_preflight) vs
        // `network` (22 tools)" - the canonical name is the one the majority
        // already used, so the REQUIRED input is `network` and `target` stays a
        // declared alias (no required parameter may be an alias of a canonical
        // name). The alias is still asserted present below.
        assertEquals(listOf("yaml", "network"), tool.inputSchema.required)
        listOf("yaml", "network", "target", "rell", "files", "strict")
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
        // The probe here hits the closed port - irrelevant to the claim, and
        // honest about it: the reachability blocker is the only one allowed.
        val result = call(
            McpTestSupport.offlineRepository(),
            buildJsonObject {
                put("yaml", testnetYaml)
                put("target", "testnet")
                put("files", buildJsonObject { put("main.rell", insecureRell) })
            }
        )
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        // The source gate ran on the aliased sources: the HIGH security finding shows up.
        assertTrue(findings(s).any { it["check"]!!.jsonPrimitive.content == "security" }, s.toString())
        val notes = s["notes"]!!.jsonPrimitive.content
        // AUDIT F10: `files` is the canonical name here as everywhere else, so
        // there is nothing to note and nothing to prefer. The deprecating line
        // this used to carry - "prefer `rell` in future calls" - pushed agents
        // away from the spelling eight other tools use.
        assertFalse(notes.contains("prefer `rell`"), notes)
        assertFalse(notes.contains("accepted as an alias"), notes)
        assertFalse(notes.contains("Source gate SKIPPED"), notes)
    }

    @Test
    fun theCanonicalFilesWinsWhenBothRellAndFilesArePresent() {
        val result = call(
            McpTestSupport.offlineRepository(),
            buildJsonObject {
                put("yaml", testnetYaml)
                put("target", "testnet")
                // AUDIT F10: "`files` for Rell sources" is the canonical name -
                // the one eight other code-taking tools already use - and `rell`
                // is the alias. So the canonical map is the one that is compiled;
                // the ALIAS is what carries uncompilable code and must be ignored.
                put("files", buildJsonObject { put("main.rell", cleanRell) })
                put("rell", buildJsonObject { put("main.rell", "module; query broken(") })
            }
        )
        assertTrue(result.isError != true)
        val s = result.structuredContent!!
        // Had the alias won, the unparsable source would be a [source] blocker.
        assertTrue(
            findings(s).none { it["check"]!!.jsonPrimitive.content == "source" },
            "`files` was compiled and `rell` ignored, so nothing may fail the source gate: $s"
        )
        assertTrue(
            blockers(s).all { it.jsonPrimitive.content.startsWith("[reachability]") },
            "the closed port is the only thing allowed to block here: $s"
        )
        // No alias note: `files` was used, `rell` ignored.
        assertFalse(s["notes"]!!.jsonPrimitive.content.contains("alias"), s.toString())
    }
}
