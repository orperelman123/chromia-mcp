package org.chromia

import org.chromia.tools.propertiesOrEmpty

import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import org.chromia.tools.callToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * onboarding_next_step: one concrete next action per state on the journey from
 * nothing to a deployed dapp, grounded in the live-verified 2026-09-01 facts
 * (the faucet web UI requires a captcha - pmc's claim-test-chr needs a
 * provider account - the lease is a Vault web step priced at lease time, and
 * chr deployment create is headless and container-key-signed). Human-only
 * steps must say so with a URL; the tool must never emit key material or
 * keygen recipes.
 */
class OnboardingNextStepToolTest {

    private val tools = McpTools.ALL_TOOL_NAMES

    private fun plan(state: OnboardingNextStep.State, registered: Set<String> = tools) =
        OnboardingNextStep.plan(state, registered)

    // ---- sequencing: common build gate ---------------------------------------

    @Test
    fun emptyStateStartsAtScaffold() {
        val result = plan(OnboardingNextStep.State())
        assertEquals("scaffold_project", result.stage)
        assertEquals("agent", result.nextAction.who)
        assertTrue(result.nextAction.how.contains("scaffold_dapp"), result.nextAction.how)
        assertTrue(result.nextAction.verify.contains("check_dapp_project"), result.nextAction.verify)
        // Default goal is testnet: the deploy steps are all still ahead.
        assertEquals(
            listOf(
                "compile", "security", "tests", "deploy_key",
                "testnet_container", "deployment_config", "deploy_testnet"
            ),
            result.remainingSteps
        )
    }

    @Test
    fun buildGateProgressesCompileSecurityTests() {
        val compiled = plan(OnboardingNextStep.State(hasProject = true))
        assertEquals("compile", compiled.stage)
        assertTrue(compiled.nextAction.how.contains("check_dapp_project"))

        val secured = plan(OnboardingNextStep.State(hasProject = true, compiles = true))
        assertEquals("security", secured.stage)
        assertTrue(secured.nextAction.verify.contains("CRITICAL"), secured.nextAction.verify)

        val tested = plan(
            OnboardingNextStep.State(hasProject = true, compiles = true, securityClean = true)
        )
        assertEquals("tests", tested.stage)
        assertTrue(tested.nextAction.how.contains("run_rell_tests"))
    }

    @Test
    fun securityStageProseNamesRellSecurityCheckOnlyWhenEnabled() {
        // Live probe 2026-09-02 (D2): the security stage's `how` prose kept a
        // parenthetical name-drop of rell_security_check even on deployments
        // that disable the tool - 7e718c0 fixed the tool RECOMMENDATIONS but
        // not this prose. Both branches must respect the enabled set.
        val atSecurity = OnboardingNextStep.State(hasProject = true, compiles = true)

        val with = plan(atSecurity)
        assertEquals("security", with.stage)
        assertTrue("rell_security_check" in tools, "rell_security_check expected in shipped registry")
        assertTrue(with.nextAction.how.contains("rell_security_check"), with.nextAction.how)

        val without = plan(atSecurity, registered = tools - "rell_security_check")
        assertEquals("security", without.stage)
        assertFalse(without.nextAction.how.contains("rell_security_check"), without.nextAction.how)
        // The always-available path is still recommended.
        assertTrue(without.nextAction.how.contains("check_dapp_project"), without.nextAction.how)
        assertTrue(without.nextAction.how.contains("CRITICAL/HIGH"), without.nextAction.how)
    }

    // ---- testnet journey -----------------------------------------------------

    private val built = OnboardingNextStep.State(
        hasProject = true, compiles = true, securityClean = true, testsPass = true
    )

    @Test
    fun testnetJourneyKeyThenContainerThenConfigThenDeploy() {
        val key = plan(built)
        assertEquals("deploy_key", key.stage)
        assertEquals("human", key.nextAction.who)
        assertTrue(key.nextAction.how.contains("chr keygen"), key.nextAction.how)
        assertTrue(key.nextAction.how.contains("never generates"), key.nextAction.how)

        // AUDIT F3 (2026-09-06): this server ships claim_testnet_tchr and
        // provision_testnet_container, so with them registered the tCHR + lease
        // step is an AGENT step naming both tools - it used to be a "human"
        // step pointing at a captcha the server does not need, and the deploy
        // journey ended there. The browser facts survive as the fallback.
        val container = plan(built.copy(hasTestnetKey = true))
        assertEquals("testnet_container", container.stage)
        assertEquals("agent", container.nextAction.who)
        val how = container.nextAction.how
        assertTrue(how.contains("claim_testnet_tchr"), how)
        assertTrue(how.contains("provision_testnet_container"), how)
        assertTrue(how.contains("no captcha and no website"), how)
        // The one condition in which the tools cannot: no funding key on the
        // SERVER - the operator's configuration, not a human with a browser.
        assertTrue(how.contains("CHROMIA_TESTNET_FUNDING_PRIVKEY"), how)
        assertTrue(how.contains("dry run"), how)
        // The manual fallback is still stated, with the same dated facts.
        assertTrue(how.contains("https://faucet.testnet.chromia.com"), how)
        assertTrue(how.contains("captcha"), how)
        assertTrue(how.contains("https://vault.testnet.chromia.com/en/containers/"), how)
        // Docs publish no fixed CHR-per-SCU price - the lease is priced at lease
        // time; the ~35 figure is a dated live observation, never a quoted price.
        assertTrue(how.contains("priced in tCHR at lease time"), how)
        assertTrue(how.contains("~35 tCHR/SCU-week"), how)
        assertTrue(how.contains("2026-09-01"), how)
        assertTrue(how.contains("1-12 weeks"), how)
        assertFalse(how.contains("no API"), how)

        // With those tools NOT registered, the honest browser answer is restored
        // in full - including the pmc disclosure.
        val manual = plan(
            built.copy(hasTestnetKey = true),
            registered = tools - OnboardingNextStep.TESTNET_CONTAINER_TOOLS
        )
        assertEquals("human", manual.nextAction.who)
        assertTrue(manual.nextAction.how.contains("pmc economy claim-test-chr"), manual.nextAction.how)
        assertTrue(manual.nextAction.how.contains("provider account"), manual.nextAction.how)
        assertTrue(manual.nextAction.how.contains("1000 tCHR per 7 days"), manual.nextAction.how)

        val config = plan(built.copy(hasTestnetKey = true, hasTestnetContainer = true))
        assertEquals("deployment_config", config.stage)
        assertEquals("agent", config.nextAction.who)
        assertTrue(config.nextAction.how.contains("write_deployment_config"))

        val deploy = plan(
            built.copy(hasTestnetKey = true, hasTestnetContainer = true, hasDeploymentConfig = true)
        )
        assertEquals("deploy_testnet", deploy.stage)
        assertEquals("agent", deploy.nextAction.who)
        assertTrue(
            deploy.nextAction.how.contains(
                "chr deployment create --settings chromia.yml --network testnet"
            ),
            deploy.nextAction.how
        )
        assertTrue(deploy.nextAction.how.contains("POSTCHAIN_CLIENT_PUBKEY"), deploy.nextAction.how)
        assertTrue(deploy.nextAction.verify.contains("verify_deployment"), deploy.nextAction.verify)

        val done = plan(
            built.copy(
                hasTestnetKey = true, hasTestnetContainer = true,
                hasDeploymentConfig = true, deployedTo = "testnet"
            )
        )
        assertEquals("done", done.stage)
        assertTrue(done.remainingSteps.isEmpty())
        assertTrue(done.blockers.isEmpty())
        assertTrue(done.nextAction.how.contains("verify_deployment"), done.nextAction.how)
    }

    @Test
    fun humanOnlyStepsAreListedAsBlockersWithUrls() {
        val result = plan(built.copy(goal = "testnet"))
        // AUDIT F3: one blocker, not two. `chr keygen` really is a human step -
        // this server never handles key material. The tCHR + lease step is not,
        // because claim_testnet_tchr and provision_testnet_container ship here.
        assertEquals(1, result.blockers.size, result.blockers.toString())
        assertTrue(result.blockers.any { it.contains("chr keygen") }, result.blockers.toString())
        assertFalse(
            result.blockers.any { it.contains("https://faucet.testnet.chromia.com") },
            "the faucet is not a blocker on a server that claims tCHR itself: ${result.blockers}"
        )
        // Agent steps are never blockers.
        assertFalse(result.blockers.any { it.contains("write_deployment_config") })

        // Take the two provisioning tools away and the browser blocker is back,
        // saying WHY: this server does not register them.
        val manual = plan(built.copy(goal = "testnet"), registered = tools - OnboardingNextStep.TESTNET_CONTAINER_TOOLS)
        assertEquals(2, manual.blockers.size, manual.blockers.toString())
        assertTrue(
            manual.blockers.any { it.contains("https://faucet.testnet.chromia.com") },
            manual.blockers.toString()
        )
    }

    // ---- mainnet journey -----------------------------------------------------

    @Test
    fun mainnetJourneyRequiresVaultDepositAndMainnetLease() {
        val lease = plan(built.copy(goal = "mainnet", hasTestnetKey = true))
        assertEquals("mainnet_container", lease.stage)
        assertEquals("human", lease.nextAction.who)
        assertTrue(lease.nextAction.how.contains("at least 10 CHR"), lease.nextAction.how)
        assertTrue(lease.nextAction.how.contains("https://vault.chromia.com/en/deposit"), lease.nextAction.how)
        assertTrue(
            lease.nextAction.how.contains("https://vault.chromia.com/en/containers/"),
            lease.nextAction.how
        )

        val deploy = plan(
            built.copy(
                goal = "mainnet", hasTestnetKey = true,
                hasTestnetContainer = true, hasDeploymentConfig = true
            )
        )
        assertEquals("deploy_mainnet", deploy.stage)
        assertTrue(deploy.nextAction.how.contains("--network mainnet"), deploy.nextAction.how)

        // A testnet deployment does NOT satisfy the mainnet goal.
        val stillPending = plan(
            built.copy(
                goal = "mainnet", hasTestnetKey = true, hasTestnetContainer = true,
                hasDeploymentConfig = true, deployedTo = "testnet"
            )
        )
        assertEquals("deploy_mainnet", stillPending.stage)

        val done = plan(built.copy(goal = "mainnet", deployedTo = "mainnet"))
        // Deployed to mainnet implies the lease/config steps completed.
        assertTrue(done.remainingSteps.none { it.startsWith("deploy_") }, done.remainingSteps.toString())
    }

    // ---- local goal ----------------------------------------------------------

    @Test
    fun localGoalEndsAtARunningChainWithNoKeyOrVaultSteps() {
        val result = plan(built.copy(goal = "local"))
        assertEquals("local_chain", result.stage)
        assertTrue(result.remainingSteps.isEmpty(), result.remainingSteps.toString())
        assertTrue(result.blockers.isEmpty(), result.blockers.toString())
        assertFalse(result.nextAction.how.contains("faucet"))
        assertFalse(result.nextAction.how.contains("keygen"))

        val done = plan(built.copy(goal = "local", hasLocalChain = true))
        assertEquals("done", done.stage)
        val notes = done.notes
        assertTrue(notes.contains("no keys, tokens, container, or Vault steps"), notes)
    }

    @Test
    fun localChainStepNamesLocalChainUpOnlyWhenRegistered() {
        // Registry check is dynamic. local_chain_up now ships (it landed from a
        // parallel lane), but the fallback still has to work for deployments
        // that disable it - so drive both branches explicitly rather than
        // depending on what the live registry happens to contain.
        val without = plan(built.copy(goal = "local"), registered = tools - "local_chain_up")
        assertFalse(without.nextAction.how.contains("local_chain_up"), without.nextAction.how)
        assertTrue(without.nextAction.how.contains("chr node start"), without.nextAction.how)

        assertTrue("local_chain_up" in tools, "local_chain_up is expected in the shipped registry")

        val with = plan(built.copy(goal = "local"), registered = tools + "local_chain_up")
        assertTrue(with.nextAction.how.contains("local_chain_up"), with.nextAction.how)
    }

    @Test
    fun deploymentDisabledToolsAreExcludedFromTheRegistrySeenByThePlan() {
        // The strategy consults the ACTUALLY-ENABLED set (compiled-in minus
        // CHROMIA_MCP_DISABLE_TOOLS), not the compiled-in set: a deployment
        // that disables local_chain_up must get the `chr node start` fallback,
        // never a recommendation for a tool whose call would be refused.
        val disabledEnv = McpTools.disabledTools(
            mapOf("CHROMIA_MCP_DISABLE_TOOLS" to "local_chain_up")
        )
        assertEquals(setOf("local_chain_up"), disabledEnv)
        val enabled = McpTools.enabledToolNames(disabledEnv)
        assertFalse("local_chain_up" in enabled)
        assertTrue("deployment_preflight" in enabled)

        val withoutTool = plan(built.copy(goal = "local"), registered = enabled)
        assertFalse(withoutTool.nextAction.how.contains("local_chain_up"), withoutTool.nextAction.how)
        assertTrue(withoutTool.nextAction.how.contains("chr node start"), withoutTool.nextAction.how)

        // Nothing disabled: the enabled set is the full registry and the tool is named.
        val allEnabled = McpTools.enabledToolNames(emptySet())
        assertEquals(McpTools.ALL_TOOL_NAMES, allEnabled)
        val withTool = plan(built.copy(goal = "local"), registered = allEnabled)
        assertTrue(withTool.nextAction.how.contains("local_chain_up"), withTool.nextAction.how)
    }

    @Test
    fun strategySeamComputesEnabledSetFromDisabledTools() = runBlocking {
        // Same behavior through the MCP strategy itself, with the registry
        // provider injected the way the production default computes it.
        val strategy = org.chromia.tools.OnboardingNextStepStrategy(
            registeredTools = {
                McpTools.enabledToolNames(
                    McpTools.disabledTools(mapOf("CHROMIA_MCP_DISABLE_TOOLS" to "local_chain_up"))
                )
            }
        )
        val result = strategy.execute(
            callToolRequest(
                name = "onboarding_next_step",
                arguments = buildJsonObject {
                    put("hasProject", true)
                    put("compiles", true)
                    put("securityClean", true)
                    put("testsPass", true)
                    put("goal", "local")
                }
            ),
            RecordingRepository()
        )
        assertTrue(result.isError != true)
        val how = result.structuredContent!!["nextAction"]!!
            .jsonObject["how"]!!.jsonPrimitive.content
        assertFalse(how.contains("local_chain_up"), how)
        assertTrue(how.contains("chr node start"), how)
    }

    @Test
    fun deployStepReferencesDeploymentPreflightOnlyWhenRegistered() {
        // Same dynamic-registry pattern as the local-chain step: the deploy step
        // names deployment_preflight only when the tool is actually registered.
        val readyToDeploy = built.copy(
            hasTestnetKey = true, hasTestnetContainer = true, hasDeploymentConfig = true
        )
        val without = plan(readyToDeploy, registered = tools - "deployment_preflight")
        assertFalse(without.nextAction.how.contains("deployment_preflight"), without.nextAction.how)
        assertTrue(without.nextAction.how.contains("chr deployment create"), without.nextAction.how)

        assertTrue(
            "deployment_preflight" in tools,
            "deployment_preflight is expected in the shipped registry"
        )
        val with = plan(readyToDeploy)
        assertTrue(with.nextAction.how.contains("deployment_preflight"), with.nextAction.how)
        // The preflight comes FIRST and the deploy command is still spelled out.
        assertTrue(
            with.nextAction.how.indexOf("deployment_preflight") <
                with.nextAction.how.indexOf("chr deployment create"),
            with.nextAction.how
        )
        assertTrue(
            with.nextAction.how.contains(
                "chr deployment create --settings chromia.yml --network testnet"
            ),
            with.nextAction.how
        )

        val mainnet = plan(readyToDeploy.copy(goal = "mainnet"))
        assertTrue(mainnet.nextAction.how.contains("\"target\": \"mainnet\""), mainnet.nextAction.how)
    }

    // ---- validation ----------------------------------------------------------

    @Test
    fun unknownGoalAndDeployedToAreValidationErrors() {
        val goalError = assertThrows(IllegalArgumentException::class.java) {
            plan(OnboardingNextStep.State(goal = "moon"))
        }
        assertTrue(goalError.message!!.contains("local|testnet|mainnet"), goalError.message)

        val deployedError = assertThrows(IllegalArgumentException::class.java) {
            plan(OnboardingNextStep.State(deployedTo = "prod"))
        }
        assertTrue(deployedError.message!!.contains("none|local|testnet|mainnet"), deployedError.message)
    }

    // ---- key-material policy -------------------------------------------------

    @Test
    fun noStateEverEmitsKeyMaterialOrInventedHex() {
        val invented = Regex("""(?i)(?<![0-9a-f])[0-9a-f]{64}(?![0-9a-f])""")
        val states = buildList {
            for (goal in OnboardingNextStep.GOALS) {
                add(OnboardingNextStep.State(goal = goal))
                add(built.copy(goal = goal))
                add(built.copy(goal = goal, hasTestnetKey = true))
                add(built.copy(goal = goal, hasTestnetKey = true, hasTestnetContainer = true))
                add(
                    built.copy(
                        goal = goal, hasTestnetKey = true, hasTestnetContainer = true,
                        hasDeploymentConfig = true
                    )
                )
                add(
                    built.copy(
                        goal = goal, hasTestnetKey = true, hasTestnetContainer = true,
                        hasDeploymentConfig = true, hasLocalChain = true, deployedTo = goal
                    )
                )
            }
        }
        states.forEach { state ->
            listOf(tools, tools + "local_chain_up").forEach { registered ->
                val text = plan(state, registered).toJson().toString()
                // POSTCHAIN_CLIENT_PRIVKEY is the documented CI env var NAME for
                // chr deployment - naming it is allowed; any other privkey talk is not.
                val scrubbed = text.replace("POSTCHAIN_CLIENT_PRIVKEY", "")
                assertFalse(scrubbed.contains("privkey", ignoreCase = true), text)
                assertFalse(scrubbed.contains("mnemonic", ignoreCase = true), text)
                assertFalse(scrubbed.contains("BEGIN PRIVATE"), text)
                assertFalse(scrubbed.contains("--secret"), text)
                assertTrue(invented.findAll(text).none(), "invented 64-hex in: $text")
            }
        }
    }

    @Test
    fun keygenIsPointedAtAsAHumanStepWithoutARecipe() {
        val result = plan(built)
        // Pointing at `chr keygen` is required; printing its output shape is not.
        assertTrue(result.nextAction.how.contains("chr keygen"))
        assertFalse(result.nextAction.how.contains("pubkey:"), result.nextAction.how)
        assertFalse(result.nextAction.how.contains("privkey"), result.nextAction.how)
    }

    // ---- MCP wiring ----------------------------------------------------------

    private fun callViaExecutor(args: kotlinx.serialization.json.JsonObject) = runBlocking {
        ToolExecutor(RecordingRepository(), PromptManager())
            .executeTool(callToolRequest(name = "onboarding_next_step", arguments = args))
    }

    @Test
    fun executeToolReturnsStructuredPlan() {
        val result = callViaExecutor(
            buildJsonObject {
                put("hasProject", true)
                put("compiles", true)
                put("securityClean", true)
                put("testsPass", true)
                put("goal", "testnet")
            }
        )
        assertTrue(result.isError != true)
        val structured = result.structuredContent!!
        assertEquals("deploy_key", structured["stage"]!!.jsonPrimitive.content)
        val action = structured["nextAction"]!!.jsonObject
        assertEquals("human", action["who"]!!.jsonPrimitive.content)
        assertTrue(action["how"]!!.jsonPrimitive.content.contains("chr keygen"))
        assertNotNull(structured["remainingSteps"]!!.jsonArray)
        assertNotNull(structured["blockers"]!!.jsonArray)
        val text = (result.content.first() as TextContent).text!!
        assertEquals(structured, kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject)
    }

    @Test
    fun executeToolInvalidGoalIsError() {
        val result = callViaExecutor(buildJsonObject { put("goal", "moon") })
        assertEquals(true, result.isError)
        val text = (result.content.first() as TextContent).text!!
        assertTrue(text.contains("local|testnet|mainnet"), text)
    }

    @Test
    fun executeToolAbsentFieldsMeanNotDone() {
        val result = callViaExecutor(buildJsonObject { })
        assertTrue(result.isError != true)
        assertEquals(
            "scaffold_project",
            result.structuredContent!!["stage"]!!.jsonPrimitive.content
        )
    }

    // ---- schema + compact mode ----------------------------------------------

    @Test
    fun advertisedInFullAndCompactMode() {
        val full = McpTools.allTools(compact = false).map { it.name }
        val compact = McpTools.allTools(compact = true).map { it.name }
        assertTrue("onboarding_next_step" in full)
        assertTrue(
            "onboarding_next_step" in compact,
            "onboarding_next_step is cheap+high-value: compact mode must keep it"
        )
    }

    @Test
    fun toolSchemaDeclaresStateFieldsAndOutputShape() {
        val tool = McpTools.onboardingNextStepTool()
        assertEquals("onboarding_next_step", tool.name)
        assertTrue(tool.inputSchema.required!!.isEmpty())
        listOf(
            "hasProject", "compiles", "securityClean", "testsPass", "hasLocalChain",
            "hasTestnetContainer", "hasTestnetKey", "hasDeploymentConfig", "deployedTo", "goal"
        ).forEach { assertNotNull(tool.inputSchema.propertiesOrEmpty[it], "inputSchema missing $it") }
        val out = tool.outputSchema!!
        listOf("stage", "nextAction", "remainingSteps", "blockers", "notes")
            .forEach { assertNotNull(out.propertiesOrEmpty[it], "outputSchema missing $it") }
        // Grounding facts live in the description, not invented at call time.
        assertTrue(tool.description!!.contains("captcha"))
        assertTrue(tool.description!!.contains("never"))
    }
}
