package org.chromia

import org.chromia.tools.callToolRequest
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.chromia.tools.McpTools
import org.chromia.tools.OnboardingNextStep
import org.chromia.tools.PromptManager
import org.chromia.tools.ToolExecutor
import org.chromia.tools.VaultLeaseHelp
import org.chromia.tools.WriteDeploymentConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * AUDIT F3 (2026-09-06) - the server told an agent a HUMAN blocker existed for
 * work this very server automates.
 *
 * onboarding_next_step{} (the first call any agent makes, 30-82 ms) answered:
 *
 *   "blockers":[
 *     "deploy_key: a human must run `chr keygen` - this server never handles key material.",
 *     "testnet_container: the tCHR faucet web UI (https://faucet.testnet.chromia.com/) requires
 *      a captcha and the container lease is a Vault web step
 *      (https://vault.testnet.chromia.com/en/containers/) - this server automates neither, and
 *      the pmc alternative for tCHR needs a configured provider account."]
 *
 * and deployment_preflight's container BLOCKER answered:
 *
 *   {"severity":"BLOCKER","check":"container",
 *    "message":"deployments.testnet.container \"<containerIID>\" looks like a placeholder...",
 *    "fix":"Lease a container (testnet: https://vault.testnet.chromia.com/en/containers/ ...)
 *           and put the real Container ID here - a human Vault web step this tool cannot do."}
 *
 * The same jar ships `provision_testnet_container` (which answered
 * {"cluster":"blue","scu":1,"durationWeeks":2,"costTchr":"70","status":"dry_run"} in 2,882 ms)
 * and `claim_testnet_tchr`, whose own README paragraph says "1000 tCHR per account per 7 days,
 * FT4-authenticated, no captcha and no website". The state machine and the blocker were
 * written before those tools existed and were never updated - the single largest reason an
 * agent abandons the deploy path.
 *
 * The pin: no blocker text may name a human/browser step for work a REGISTERED tool performs.
 */
class NoHumanBlockerForAutomatedStepTest {

    private val registered = McpTools.ALL_TOOL_NAMES

    /**
     * Steps a shipped tool performs, and the browser artefacts whose appearance
     * in a BLOCKER means the server is sending the agent to a human it does not
     * need. `chr keygen` is deliberately absent: no tool on this server makes
     * key material, and saying so is honest.
     */
    private val automatedStepMarkers = listOf(
        VaultLeaseHelp.TESTNET_FAUCET,
        VaultLeaseHelp.TESTNET_VAULT_CONTAINERS,
        "captcha",
        "web UI",
        "Vault web step"
    )

    private fun everyState(): List<OnboardingNextStep.State> = buildList {
        for (goal in OnboardingNextStep.GOALS) {
            for (deployedTo in OnboardingNextStep.DEPLOYED_TO) {
                for (key in listOf(false, true)) {
                    for (container in listOf(false, true)) {
                        add(
                            OnboardingNextStep.State(
                                hasProject = true, compiles = true, securityClean = true, testsPass = true,
                                hasTestnetKey = key, hasTestnetContainer = container,
                                hasDeploymentConfig = false, deployedTo = deployedTo, goal = goal
                            )
                        )
                    }
                }
            }
        }
    }

    @Test
    fun noBlockerNamesAHumanStepAToolOnThisServerPerforms() {
        everyState().forEach { state ->
            val result = OnboardingNextStep.plan(state, registered)
            result.blockers.forEach { blocker ->
                // The mainnet lease genuinely has no tool - provision_testnet_container
                // is testnet only - so mainnet_container may keep its Vault text.
                if (blocker.startsWith("mainnet_container")) return@forEach
                automatedStepMarkers.forEach { marker ->
                    assertFalse(
                        blocker.contains(marker),
                        "state=$state blocker names \"$marker\" for a step " +
                            "${OnboardingNextStep.TESTNET_CONTAINER_TOOLS} performs: $blocker"
                    )
                }
            }
        }
    }

    @Test
    fun theTestnetContainerStepIsAnAgentStepThatNamesBothToolsAndTheOneConditionItCannot() {
        val state = OnboardingNextStep.State(
            hasProject = true, compiles = true, securityClean = true, testsPass = true,
            hasTestnetKey = true, goal = "testnet"
        )
        val result = OnboardingNextStep.plan(state, registered)
        assertEquals("testnet_container", result.stage)
        assertEquals("agent", result.nextAction.who, "the server leases this itself: ${result.nextAction}")
        val how = result.nextAction.how
        assertTrue(how.contains("claim_testnet_tchr"), how)
        assertTrue(how.contains("provision_testnet_container"), how)
        assertTrue(how.contains("no captcha and no website"), how)
        // The exact condition under which it cannot, named.
        assertTrue(how.contains("CHROMIA_TESTNET_FUNDING_PRIVKEY"), how)
        assertTrue(how.contains("dry run") || how.contains("dryRun"), how)
        assertTrue(
            result.blockers.none { it.startsWith("testnet_container") },
            "an agent step is never a blocker: ${result.blockers}"
        )
        assertTrue(
            result.blockers.any { it.startsWith("deploy_key") },
            "chr keygen really is a human step and stays one: ${result.blockers}"
        )
        assertTrue(result.notes.contains("claim_testnet_tchr"), result.notes)
    }

    @Test
    fun aServerThatDoesNotRegisterThoseToolsStillTellsTheHonestBrowserTruth() {
        val withoutProvisioning = registered - OnboardingNextStep.TESTNET_CONTAINER_TOOLS
        val state = OnboardingNextStep.State(
            hasProject = true, compiles = true, securityClean = true, testsPass = true,
            hasTestnetKey = true, goal = "testnet"
        )
        val result = OnboardingNextStep.plan(state, withoutProvisioning)
        assertEquals("human", result.nextAction.who)
        assertTrue(result.nextAction.how.contains(VaultLeaseHelp.TESTNET_FAUCET), result.nextAction.how)
        assertTrue(
            result.blockers.any { it.startsWith("testnet_container") },
            "with no tool to do it, it IS a human blocker: ${result.blockers}"
        )
    }

    // ---- deployment_preflight ------------------------------------------------

    private val scaffoldYamlWithPlaceholderContainer = buildString {
        appendLine("blockchains:")
        appendLine("  fee_token:")
        appendLine("    module: main")
        appendLine("    config:")
        appendLine("      features:")
        appendLine("        merkle_hash_version: 2")
        appendLine("compile:")
        appendLine("  rellVersion: 0.16.1")
        appendLine("deployments:")
        appendLine("  testnet:")
        appendLine("    url:")
        appendLine("      - https://node0.testnet.chromia.com:7740")
        appendLine("    brid: x\"${WriteDeploymentConfig.TESTNET_DIRECTORY_BRID}\"")
        appendLine("    container: <containerIID>")
    }

    private fun preflight(yaml: String, target: String) = runBlocking {
        ToolExecutor(RecordingRepository(), PromptManager()).executeTool(
            callToolRequest(
                name = "deployment_preflight",
                arguments = buildJsonObject {
                    put("yaml", yaml)
                    put("target", target)
                    put("rell", buildJsonObject { put("main.rell", "module;\n\nquery hello_world() = \"hello\";\n") })
                }
            )
        )
    }

    @Test
    fun thePlaceholderContainerBlockerNamesTheToolThatLeasesItNotABrowser() {
        val result = preflight(scaffoldYamlWithPlaceholderContainer, "testnet")
        val container = result.structuredContent!!["findings"]!!.jsonArray
            .map { it.jsonObject }
            .firstOrNull { it["check"]!!.jsonPrimitive.content == "container" }
        assertNotNull(container, "the audit's exact call still reports the placeholder: ${result.structuredContent}")
        val fix = container!!["fix"]!!.jsonPrimitive.content
        assertTrue(fix.contains("provision_testnet_container"), fix)
        assertTrue(fix.contains("claim_testnet_tchr"), fix)
        assertFalse(
            fix.contains("a human Vault web step this tool cannot do"),
            "the tool CAN do it: $fix"
        )
        assertTrue(fix.contains("CHROMIA_TESTNET_FUNDING_PRIVKEY"), "name the one condition it cannot: $fix")
    }

    @Test
    fun theMainnetContainerBlockerStaysHonestlyHumanBecauseNoToolLeasesThere() {
        val mainnetYaml = scaffoldYamlWithPlaceholderContainer
            .replace("  testnet:", "  mainnet:")
            .replace(WriteDeploymentConfig.TESTNET_DIRECTORY_BRID, WriteDeploymentConfig.MAINNET_DIRECTORY_BRID)
            .replace("https://node0.testnet.chromia.com:7740", "https://system.chromaway.com")
        val result = preflight(mainnetYaml, "mainnet")
        val container = result.structuredContent!!["findings"]!!.jsonArray
            .map { it.jsonObject }
            .firstOrNull { it["check"]!!.jsonPrimitive.content == "container" }
        assertNotNull(container, result.structuredContent.toString())
        val fix = container!!["fix"]!!.jsonPrimitive.content
        assertTrue(fix.contains(VaultLeaseHelp.MAINNET_VAULT_CONTAINERS), fix)
        assertTrue(fix.contains("testnet only"), "and it says WHY it is human here: $fix")
    }
}
