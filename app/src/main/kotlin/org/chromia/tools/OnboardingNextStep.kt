package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * State machine behind the `onboarding_next_step` tool: given what an agent
 * honestly reports about a dapp project, answer exactly ONE next action on the
 * journey from nothing to a deployed dapp, plus the remaining steps and any
 * human-only blockers.
 *
 * Every fact here is grounded in the live-verified research of 2026-09-01
 * (docs.chromia.com + the testnet economy chain; see the vault_lease_help and
 * chr_deploy_help tools for the underlying pages):
 * - The tCHR faucet web UI requires a captcha, so a human claims it (1000 tCHR
 *   per 7 days). `pmc economy claim-test-chr` exists (testnet only), but pmc is
 *   the provider CLI submitting signed Economy Chain transactions from a
 *   configured provider account - not a path for a keyless agent.
 * - Testnet container lease is a Vault web step, priced in tCHR at lease time
 *   (docs publish no fixed CHR-per-SCU price; observed ~35 tCHR/SCU-week on the
 *   testnet economy chain 2026-09-01), 1-12 weeks.
 * - `chr deployment create/update --settings chromia.yml --network testnet|mainnet
 *   --blockchain <name>` is fully headless, signed by the CONTAINER key
 *   (env POSTCHAIN_CLIENT_PUBKEY/POSTCHAIN_CLIENT_PRIVKEY is Chromia's own
 *   documented CI pattern). The deploy key holds no funds.
 * - Mainnet needs a Vault deposit (>= 10 CHR) plus a mainnet container lease first.
 *
 * Policy: this object NEVER emits key material or keygen recipes - key
 * creation is pointed at `chr keygen` as a human step, with no example keys.
 */
object OnboardingNextStep {

    val GOALS = listOf("local", "testnet", "mainnet")
    val DEPLOYED_TO = listOf("none", "local", "testnet", "mainnet")

    data class State(
        val hasProject: Boolean = false,
        val compiles: Boolean = false,
        val securityClean: Boolean = false,
        val testsPass: Boolean = false,
        val hasLocalChain: Boolean = false,
        val hasTestnetContainer: Boolean = false,
        val hasTestnetKey: Boolean = false,
        val hasDeploymentConfig: Boolean = false,
        val deployedTo: String = "none",
        val goal: String = "testnet"
    )

    data class NextAction(
        val what: String,
        val who: String, // "agent" | "human"
        val how: String,
        val verify: String
    )

    data class Result(
        val stage: String,
        val nextAction: NextAction,
        val remainingSteps: List<String>,
        val blockers: List<String>,
        val notes: String
    ) {
        fun toJson() = buildJsonObject {
            put("stage", stage)
            put(
                "nextAction",
                buildJsonObject {
                    put("what", nextAction.what)
                    put("who", nextAction.who)
                    put("how", nextAction.how)
                    put("verify", nextAction.verify)
                }
            )
            put("remainingSteps", buildJsonArray { remainingSteps.forEach { add(JsonPrimitive(it)) } })
            put("blockers", buildJsonArray { blockers.forEach { add(JsonPrimitive(it)) } })
            put("notes", notes)
        }
    }

    private class Step(
        val stage: String,
        val done: (State) -> Boolean,
        val action: (State, Set<String>) -> NextAction
    )

    private fun deployedRank(deployedTo: String): Int = DEPLOYED_TO.indexOf(deployedTo)

    /**
     * @param registeredTools the tool names this server registers at runtime -
     * checked dynamically so the local-chain step names `local_chain_up` only
     * once that tool actually exists in the registry.
     */
    fun plan(state: State, registeredTools: Set<String>): Result {
        require(state.goal in GOALS) {
            "goal must be one of ${GOALS.joinToString("|")} (got \"${state.goal}\")"
        }
        require(state.deployedTo in DEPLOYED_TO) {
            "deployedTo must be one of ${DEPLOYED_TO.joinToString("|")} (got \"${state.deployedTo}\")"
        }

        val steps = stepsFor(state.goal)
        val pending = steps.filterNot { it.done(state) }
        val notes = notesFor(state.goal, registeredTools)

        if (pending.isEmpty()) {
            return Result(
                stage = "done",
                nextAction = NextAction(
                    what = "Goal \"${state.goal}\" reached - confirm the deployment is live",
                    who = "agent",
                    how = doneVerifyHow(state.goal),
                    verify = "verify_deployment returns live:true with a block height" +
                        if (state.goal == "local") " (or the local node answers the query)" else ""
                ),
                remainingSteps = emptyList(),
                blockers = emptyList(),
                notes = notes
            )
        }

        val next = pending.first()
        val action = next.action(state, registeredTools)
        val blockers = pending.filter { it.action(state, registeredTools).who == "human" }
            .map { humanBlockerLine(it.stage) }
        return Result(
            stage = next.stage,
            nextAction = action,
            remainingSteps = pending.drop(1).map { it.stage },
            blockers = blockers,
            notes = notes
        )
    }

    // ---- step definitions ---------------------------------------------------

    private fun stepsFor(goal: String): List<Step> {
        val common = listOf(scaffoldStep(), compileStep(), securityStep(), testsStep())
        return when (goal) {
            "local" -> common + localChainStep()
            "testnet" -> common + listOf(
                deployKeyStep(),
                testnetContainerStep(),
                deploymentConfigStep(),
                deployStep("testnet")
            )
            else -> common + listOf(
                deployKeyStep(),
                mainnetContainerStep(),
                deploymentConfigStep(),
                deployStep("mainnet")
            )
        }
    }

    private fun scaffoldStep() = Step(
        stage = "scaffold_project",
        done = { it.hasProject }
    ) { _, _ ->
        NextAction(
            what = "Create a dapp project",
            who = "agent",
            how = "Call scaffold_dapp (e.g. {\"name\": \"my_dapp\", \"template\": \"ft4\"}) to generate " +
                "a compilable chromia.yml + Rell starter, or start from existing sources.",
            verify = "check_dapp_project on the generated files returns ok:true."
        )
    }

    private fun compileStep() = Step(
        stage = "compile",
        done = { it.compiles }
    ) { _, _ ->
        NextAction(
            what = "Make the project compile",
            who = "agent",
            how = "Call check_dapp_project with {\"yaml\": <chromia.yml>, \"rell\": {path -> source}} " +
                "and fix every reported error (translate_error explains cryptic ones).",
            verify = "check_dapp_project returns ok:true with an empty errors array."
        )
    }

    private fun securityStep() = Step(
        stage = "security",
        done = { it.securityClean }
    ) { _, tools ->
        // Same dynamic-registry rule as the tool RECOMMENDATIONS (7e718c0):
        // prose must not name-drop rell_security_check on deployments that
        // disable it (CHROMIA_MCP_DISABLE_TOOLS) - the parenthetical sent
        // agents into the disabled-tool refusal (live probe 2026-09-02, D2).
        val detail = if ("rell_security_check" in tools) {
            " (rell_security_check gives the per-rule detail)"
        } else {
            ""
        }
        NextAction(
            what = "Clear the security scan",
            who = "agent",
            how = "check_dapp_project already runs the security pass when the code compiles; " +
                "fix every CRITICAL/HIGH finding$detail.",
            verify = "check_dapp_project returns ok:true - no CRITICAL/HIGH security findings remain."
        )
    }

    private fun testsStep() = Step(
        stage = "tests",
        done = { it.testsPass }
    ) { _, _ ->
        NextAction(
            what = "Write and pass Rell tests",
            who = "agent",
            how = "Call run_rell_tests with {\"files\": {path -> source}} including a `@test module;` file " +
                "covering the operations and queries.",
            verify = "run_rell_tests reports every case passed."
        )
    }

    private fun localChainStep() = Step(
        stage = "local_chain",
        done = { it.hasLocalChain || deployedRank(it.deployedTo) >= deployedRank("local") }
    ) { _, tools ->
        if ("local_chain_up" in tools) {
            NextAction(
                what = "Start a local chain running the dapp",
                who = "agent",
                how = "Call local_chain_up to start a local node with the project deployed.",
                verify = "verify_deployment (or a chromia_dapp_query) against the local node answers, " +
                    "e.g. the scaffold's hello_world query."
            )
        } else {
            NextAction(
                what = "Start a local node running the dapp",
                who = "agent",
                how = "No MCP tool starts a local node on this server yet - run `chr node start` from the " +
                    "project directory (needs the Chromia CLI and a local PostgreSQL).",
                verify = "`chr query hello_world` (or your own query) answers against the local REST port."
            )
        }
    }

    private fun deployKeyStep() = Step(
        stage = "deploy_key",
        done = { it.hasTestnetKey }
    ) { _, _ ->
        NextAction(
            what = "Create a deployment keypair",
            who = "human",
            how = "Run `chr keygen` yourself on a trusted machine - this server never generates or " +
                "displays keys. Keep the private key out of agent-visible files and chat. " +
                "The deploy key only signs deployments and holds no funds.",
            verify = "A pubkey file exists locally (e.g. under ~/.chromia/); the public key is safe to " +
                "paste into the Vault lease form - the private key never is."
        )
    }

    /** The two tools that make the tCHR + lease step headless when this server ships them. */
    val TESTNET_CONTAINER_TOOLS = setOf("claim_testnet_tchr", "provision_testnet_container")

    /**
     * AUDIT F3 (2026-09-06): this step told the agent "the faucet web UI
     * requires a captcha and the lease is a Vault web step - this server
     * automates neither" while `claim_testnet_tchr` and
     * `provision_testnet_container` shipped in the same jar, and
     * `claim_testnet_tchr` is FT4-authenticated with no captcha and no website.
     * It was the single sentence that stopped an agent finishing the deploy.
     *
     * Same dynamic-registry rule the local-chain and deploy steps already use:
     * name the tools only when this server actually registers them. When it
     * does, this is an AGENT step and therefore never a human blocker - what
     * can be missing is a FUNDING KEY, which is the server operator's
     * configuration, not a browser.
     */
    private fun testnetContainerStep() = Step(
        stage = "testnet_container",
        done = { it.hasTestnetContainer }
    ) { _, tools ->
        if (TESTNET_CONTAINER_TOOLS.all { it in tools }) {
            NextAction(
                what = "Get tCHR and lease a testnet container",
                who = "agent",
                how = "1) Call claim_testnet_tchr - FT4-authenticated on the testnet economy chain, " +
                    "1000 tCHR per account per 7 days, no captcha and no website. " +
                    "2) Call provision_testnet_container (cluster, scu, durationWeeks) - it leases " +
                    "headlessly, priced in tCHR at lease time (observed ~35 tCHR/SCU-week on the " +
                    "testnet economy chain 2026-09-01), 1-12 weeks; call it with dryRun first to see " +
                    "the cost. Neither is a human step. THE ONE CONDITION IN WHICH THEY CANNOT: with " +
                    "no funding key registered on the server (env " +
                    "${TestnetProvisioning.FUNDING_KEY_ENV} or ${TestnetProvisioning.FUNDING_KEY_ID_ENV}) " +
                    "they answer a dry run / blocked_setup and sign nothing - that is the SERVER " +
                    "OPERATOR configuring a key, not a human opening a browser. The manual fallback, " +
                    "if the operator will not: the faucet at ${VaultLeaseHelp.TESTNET_FAUCET} (captcha, " +
                    "1000 tCHR per 7 days) and a lease at ${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS}. " +
                    "3) Use the resulting Container ID in chromia.yml.",
                verify = "provision_testnet_container reports status \"provisioned\" with a Container ID " +
                    "(status \"dry_run\" means nothing was leased; status \"blocked_human_step\" names " +
                    "what the operator must configure)."
            )
        } else {
            NextAction(
                what = "Get tCHR and lease a testnet container",
                who = "human",
                how = "This server does not register claim_testnet_tchr / provision_testnet_container, " +
                    "so the headless path is unavailable here. 1) Claim tCHR at " +
                    "${VaultLeaseHelp.TESTNET_FAUCET} - the web UI requires a captcha " +
                    "(1000 tCHR per 7 days), so a human must do it. (`pmc economy claim-test-chr` exists " +
                    "for testnet, but pmc needs a configured provider account - not a keyless-agent path.) " +
                    "2) Lease a container at ${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS} using your pubkey - " +
                    "priced in tCHR at lease time (observed ~35 tCHR/SCU-week on the testnet economy chain " +
                    "2026-09-01), 1-12 weeks. " +
                    "3) Give the agent the resulting Container ID.",
                verify = "The Vault containers page shows the lease; you hold a real Container ID for chromia.yml."
            )
        }
    }

    private fun mainnetContainerStep() = Step(
        stage = "mainnet_container",
        done = { it.hasTestnetContainer }
    ) { _, _ ->
        NextAction(
            what = "Deposit CHR and lease a mainnet container",
            who = "human",
            how = "1) Deposit at least 10 CHR at ${VaultLeaseHelp.MAINNET_VAULT_DEPOSIT}. " +
                "2) Lease a container at ${VaultLeaseHelp.MAINNET_VAULT_CONTAINERS} using your pubkey " +
                "(weekly lease paid in CHR, 1-12 weeks). " +
                "3) Give the agent the resulting Container ID.",
            verify = "The Vault containers page shows the lease; you hold a real Container ID for chromia.yml."
        )
    }

    private fun deploymentConfigStep() = Step(
        stage = "deployment_config",
        done = { it.hasDeploymentConfig }
    ) { _, _ ->
        NextAction(
            what = "Write the deployment section of chromia.yml",
            who = "agent",
            how = "Call write_deployment_config with the leased Container ID and target network so " +
                "chromia.yml gains deployments.<network> (url, brid, container).",
            verify = "validate_chromia_yml (or check_dapp_project) accepts the updated chromia.yml."
        )
    }

    private fun deployStep(network: String) = Step(
        stage = "deploy_$network",
        done = { deployedRank(it.deployedTo) >= deployedRank(network) }
    ) { _, tools ->
        // Dynamic registry check, like the local-chain step: name the preflight
        // tool only once it actually exists in this server's registry.
        val preflight = if ("deployment_preflight" in tools) {
            "First call deployment_preflight with {\"yaml\": <chromia.yml>, \"target\": \"$network\", " +
                "\"rell\": {path -> source}} and clear every blocker - it catches wrong-network BRIDs, " +
                "placeholder container ids, bad pins, and unreachable nodes BEFORE anything is signed. Then: "
        } else {
            ""
        }
        NextAction(
            what = "Deploy the dapp to $network",
            who = "agent",
            how = preflight + "Run `chr deployment create --settings chromia.yml --network $network " +
                "--blockchain <name>` - fully headless, signed by the CONTAINER key; supply it via the " +
                "POSTCHAIN_CLIENT_PUBKEY/POSTCHAIN_CLIENT_PRIVKEY environment variables (Chromia's " +
                "documented CI pattern). The command writes the resulting BRID into chromia.yml. " +
                "Use `chr deployment update` for later config changes.",
            verify = "verify_deployment with the written BRID and network \"$network\" returns live:true " +
                "with a block height."
        )
    }

    // ---- supporting text ----------------------------------------------------

    private fun humanBlockerLine(stage: String): String = when (stage) {
        "deploy_key" -> "deploy_key: a human must run `chr keygen` - this server never handles key material."
        // Only reachable when this server does NOT register the two provisioning
        // tools; when it does, the step is an agent step and never a blocker.
        "testnet_container" -> "testnet_container: this server does not register " +
            "${TESTNET_CONTAINER_TOOLS.sorted().joinToString(" / ")}, so the tCHR faucet web UI " +
            "(${VaultLeaseHelp.TESTNET_FAUCET}, captcha) and the Vault lease " +
            "(${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS}) are the only path here, and the " +
            "pmc alternative for tCHR needs a configured provider account."
        "mainnet_container" -> "mainnet_container: the Vault deposit (>= 10 CHR, " +
            "${VaultLeaseHelp.MAINNET_VAULT_DEPOSIT}) and container lease " +
            "(${VaultLeaseHelp.MAINNET_VAULT_CONTAINERS}) are Vault web steps a human must do."
        else -> "$stage: requires a human step."
    }

    private fun doneVerifyHow(goal: String): String = when (goal) {
        "local" -> "Query the local node (chromia_dapp_query against the local REST port, or " +
            "`chr query`) to confirm the chain answers."
        else -> "Call verify_deployment with {\"brid\": <deployed BRID>, \"network\": \"$goal\"} to " +
            "confirm the chain is live and producing blocks."
    }

    private fun notesFor(goal: String, registeredTools: Set<String> = emptySet()): String {
        // AUDIT F3: these notes described the browser path as the only path while
        // the server shipped the tools that replace it. Say which is true HERE.
        val headless = if (TESTNET_CONTAINER_TOOLS.all { it in registeredTools }) {
            "This server automates tCHR and the testnet lease: claim_testnet_tchr is " +
                "FT4-authenticated (1000 tCHR per account per 7 days, no captcha and no website) and " +
                "provision_testnet_container leases headlessly. They need a funding key configured on " +
                "the SERVER (${TestnetProvisioning.FUNDING_KEY_ENV} / " +
                "${TestnetProvisioning.FUNDING_KEY_ID_ENV}); without one they answer a dry run and " +
                "sign nothing. The browser path below is the fallback, not the default. "
        } else {
            ""
        }
        val base = headless + "Facts (live-verified 2026-09-01): the tCHR faucet web UI requires a captcha " +
            "(1000 tCHR / 7 days, ${VaultLeaseHelp.TESTNET_FAUCET}; `pmc economy claim-test-chr` " +
            "exists for testnet but needs a configured provider account); a testnet container lease " +
            "is priced in tCHR at lease time - observed ~35 tCHR/SCU-week on the testnet economy " +
            "chain 2026-09-01 - for 1-12 weeks (${VaultLeaseHelp.TESTNET_VAULT_CONTAINERS}); " +
            "mainnet needs a Vault deposit of at least 10 CHR plus a lease before deploying; " +
            "`chr deployment create/update` is headless and signed by the container key, which holds " +
            "no funds. This server never generates keys or emits key material."
        val goalNote = when (goal) {
            "local" -> " Goal \"local\": the journey ends at a running local chain - no keys, tokens, " +
                "container, or Vault steps are needed."
            "mainnet" -> " Goal \"mainnet\": hasTestnetContainer is read as \"holds a container lease on " +
                "the goal network\" - report it true once the MAINNET lease exists. Consider deploying " +
                "to testnet first as a rehearsal."
            else -> ""
        }
        return base + goalNote
    }
}
