package org.chromia.tools

/**
 * Named deployment shapes for the tool catalog.
 *
 * A profile is nothing more than a preset for [McpTools.disabledTools] plus a
 * label the server reports, so a client can see what it is talking to. It
 * composes with `CHROMIA_MCP_DISABLE_TOOLS`: an operator can always disable
 * more, never less.
 *
 * ## Why `public` exists
 *
 * ChatGPT connectors (and the OpenAI tunnel) are commonly wired without auth:
 * whoever has the URL can call every advertised tool. On the [FULL] profile
 * that includes tools that start an embedded chain on the operator's machine,
 * shell out to `chr`, and sign transactions with the deploy keystore's private
 * key. The `public` profile removes exactly those, and nothing else - the
 * compiler loop, the docs tools, the explorer queries and the prompt catalog
 * all stay, because that is the product.
 *
 * Selected with `CHROMIA_MCP_PROFILE=public` or `--profile public`.
 */
object ToolProfiles {

    /** Every tool this build implements, gated only by CHROMIA_MCP_DISABLE_TOOLS. */
    const val FULL: String = "full"

    /** No tool that acts on the local machine or uses a key. See [PUBLIC_DISABLED]. */
    const val PUBLIC: String = "public"

    const val PROFILE_ENV: String = "CHROMIA_MCP_PROFILE"

    val NAMES: Set<String> = setOf(FULL, PUBLIC)

    /**
     * The exact tool set the `public` profile disables.
     *
     * Pinned here rather than derived at startup so the running server never
     * depends on constructing a [ToolExecutor] to know what it serves, and so a
     * reviewer can read the public surface in one place. It must equal
     * [ToolExecutor.localMachineToolNames] - the set derived from the
     * [ToolStrategy.touchesLocalMachine] markers - and ToolProfilesTest fails if
     * it ever does not, which is what makes a newly added machine-touching tool
     * a deliberate decision instead of an accident.
     *
     * Why each is here:
     *  - `local_chain_up` - boots an embedded Postchain node against the host's
     *    PostgreSQL and leaves it running.
     *  - `provision_testnet_container` - generates a key pair, writes it to the
     *    deploy keystore, and signs a funded transaction on testnet.
     *  - `claim_testnet_tchr` - signs a faucet claim with the funding key.
     *  - `deploy_testnet_chain` - reads a private key from the deploy keystore or
     *    the environment, materialises a work directory, and runs the `chr` CLI.
     *
     * Audited and deliberately NOT here (the brief listed the first two as
     * candidates; the code says otherwise):
     *  - `check_dapp_project` and `deployment_preflight` take Rell sources as
     *    arguments, never a path on disk - and check_dapp_project is the
     *    compile+security gateway the whole product is built around.
     *  - `write_deployment_config` renders a chromia.yml `deployments` block as
     *    text; it writes no file and signs nothing.
     *  - `rell_check` / `rell_security_check` / `run_rell_tests` / `verify_guards`
     *    materialise only the caller's own sources into a fresh temp directory
     *    (traversal rejected before any write) and delete it afterwards.
     *  - `verify_deployment` reads block heights over the network; no key.
     */
    val PUBLIC_DISABLED: Set<String> = setOf(
        "claim_testnet_tchr",
        "deploy_testnet_chain",
        "local_chain_up",
        "provision_testnet_container",
    )

    /**
     * The active profile name. `--profile <name>` wins over [PROFILE_ENV]; an
     * unknown name is rejected rather than silently downgraded to [FULL] - a
     * typo'd `--profile publik` that quietly served the full toolset over a
     * public tunnel is exactly the failure this feature exists to prevent.
     */
    fun resolve(
        flag: String? = null,
        env: Map<String, String> = System.getenv(),
    ): String {
        val raw = flag ?: env[PROFILE_ENV]
        val name = raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return FULL
        require(name in NAMES) {
            "Unknown profile: $raw. Valid profiles: ${NAMES.sorted().joinToString(", ")}"
        }
        return name
    }

    /** Tools the given profile disables on top of CHROMIA_MCP_DISABLE_TOOLS. */
    fun disabledTools(profile: String): Set<String> = when (profile) {
        PUBLIC -> PUBLIC_DISABLED
        else -> emptySet()
    }
}
