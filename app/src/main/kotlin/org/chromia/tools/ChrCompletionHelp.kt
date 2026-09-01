package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x help / version / --generate-completion help.
 * Source: docs.chromia.com/build/cli/commands/ (index), /help, /version.
 * Does not run chr, generate a key, invent a BRID, or send signed transactions.
 *
 * Skipped (not official public BUILD/ops help):
 * - chr fetch-config: hidden + experimental in source; not on the official command index
 * - chr deployment lease-info: hidden + experimental; not on the live deployment page
 * - chr deployment remove-container: hidden; economy-chain FT4 removeContainerOperation (signed, no refund)
 * Official BUILD cli/commands index slash/title/child-card values live here (query-only).
 * Official BUILD cli/introduction index slash/title values live here (query-only).
 * Official BUILD cli/cli-release-notes index slash/title values live here (query-only).
 * Official BUILD cli/commands/help index slash/title values live here (query-only).
 * Official BUILD cli/commands/version index slash/title values live here (query-only).
 */
object ChrCompletionHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val COMMANDS_URL = "https://docs.chromia.com/build/cli/commands/"
    const val COMMANDS_INDEX_URL = "https://docs.chromia.com/build/cli/commands"
    const val COMMANDS_INDEX_URL_SLASH = COMMANDS_URL
    const val COMMANDS_INDEX_TITLE = "CLI command reference"
    const val COMMANDS_INDEX_CARD_HELP = "help"
    const val COMMANDS_INDEX_CARD_VERSION = "version"
    const val COMMANDS_INDEX_CARD_BUILD = "build"
    const val COMMANDS_INDEX_CARD_CREATE_RELL_DAPP = "create-rell-dapp"
    const val COMMANDS_INDEX_CARD_DEPLOYMENT = "deployment"
    const val COMMANDS_INDEX_CARD_EIF = "eif"
    const val COMMANDS_INDEX_CARD_GENERATE = "generate"
    const val COMMANDS_INDEX_CARD_LIBRARY = "library"
    const val COMMANDS_INDEX_CARD_KEYGEN = "keygen"
    const val COMMANDS_INDEX_CARD_NODE = "node"
    const val COMMANDS_INDEX_CARD_QUERY = "query"
    const val COMMANDS_INDEX_CARD_REPL = "repl"
    const val COMMANDS_INDEX_CARD_TEST = "test"
    const val COMMANDS_INDEX_CARD_TX = "tx"
    const val COMMANDS_INDEX_CARD_CODE = "code"
    const val COMMANDS_INDEX_CARD_MULTI_SIGNATURE = "multi-signature"
    const val COMMANDS_INDEX_CARD_TOOLS = "tools"
    const val COMMANDS_INDEX_CARD_SEEDER = "seeder"
    const val HELP_URL = "https://docs.chromia.com/build/cli/commands/help"
    const val HELP_INDEX_URL = HELP_URL
    const val HELP_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/help/"
    const val HELP_INDEX_TITLE = "help"  // official H1
    const val VERSION_URL = "https://docs.chromia.com/build/cli/commands/version"
    const val VERSION_INDEX_URL = VERSION_URL
    const val VERSION_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/version/"
    const val VERSION_INDEX_TITLE = "version"  // official H1
    const val INTRO_URL = "https://docs.chromia.com/build/cli/introduction"
    const val CLI_INTRO_INDEX_URL = INTRO_URL
    const val CLI_INTRO_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/introduction/"
    const val CLI_INTRO_INDEX_TITLE = "Introduction to Chromia CLI"
    const val RELEASE_NOTES_URL = "https://docs.chromia.com/build/cli/cli-release-notes"
    const val CLI_RELEASE_NOTES_INDEX_URL = RELEASE_NOTES_URL
    const val CLI_RELEASE_NOTES_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/cli-release-notes/"
    const val CLI_RELEASE_NOTES_INDEX_TITLE = "Release notes"
    const val DOCS_LATEST_CLI = "0.30.0"
    const val DOCS_LATEST_CLI_DATE = "2026-02-27"
    const val TOOL_NAME = "chr_completion_help"
    const val ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-bridge-chains"
    const val ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-bridge-chains/"
    const val ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_TITLE = "Deploy Chromia bridge chains"  // official H1
    const val LEARN_RELL_MASTERCLASS_INDEXES_INDEX_URL = "https://learn.chromia.com/courses/rell-masterclass/indexes"
    const val LEARN_RELL_MASTERCLASS_INDEXES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-masterclass/indexes/"
    const val LEARN_RELL_MASTERCLASS_INDEXES_INDEX_TITLE = "Keys and indexes"  // official H1
    const val LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_URL = "https://learn.chromia.com/courses/iccf-course/system-overview"
    const val LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_URL_SLASH = "https://learn.chromia.com/courses/iccf-course/system-overview/"
    const val LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_TITLE = "System overview"  // official H1

    val shells = listOf("bash", "zsh", "fish")

    fun notes(): String = """
        Chromia CLI $CLI_SERIES help / version / shell completion. Java 21+, Postgres 16+.
        Official command index: $COMMANDS_URL
        Official intro (200): $INTRO_URL
        Official BUILD cli/introduction ($CLI_INTRO_INDEX_URL 307 $CLI_INTRO_INDEX_URL_SLASH 200 $CLI_INTRO_INDEX_TITLE): intro Chromia CLI simplifies the development cycle and deployment of Rell dapps, where all the needed capability is available in one CLI It's a command-line tool that provides a way to interact with the Chromia blockchain using a set of commands The Chromia CLI tool is designed to be user-friendly and easy to use It provides a set of commands that can be used in a terminal or console window and supports a range of options and flags to customize the behavior of the commands For information about installing Chromia CLI, see Install Chromia CLI tip You can use Chromia CLI in Gitlab CI and Bitbucket Pipeline How the CLI Works The Chromia CLI is your main tool for building and deploying dapps Local commands `chr node start` Starts a local blockchain node on your machine `chr query` Queries a local or remote blockchain `chr test` Runs your test files `chr build` Builds your blockchain configuration Remote commands `chr deployment create` Deploys your dapp to a remote network `chr deployment update` Updates an existing deployment Common Development Workflow `chr create-rell-dapp` `chr node start` `chr query hello_world` `chr test` `chr deployment create` Database Requirement you need a running database when using CLI commands that interact with a local blockchain WRITE SKIP keygen Understanding Key Management skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex.
        Official release notes (200): $RELEASE_NOTES_URL
        Official BUILD cli/cli-release-notes ($CLI_RELEASE_NOTES_INDEX_URL 307 $CLI_RELEASE_NOTES_INDEX_URL_SLASH 200 $CLI_RELEASE_NOTES_INDEX_TITLE): notes The release notes lists all new features, resolved issues, and known issues of Chromia CLI in chronological order Chromia CLI 0.30.0 Released on February 27, 2026 Added chr deployment create writes chromia.yml chr build --skip-lib-check Enum Change Detection Schema comparison Version Bumps rell 0.15.2 postchain 3.49.2 postchain-chromia 3.39.3 eif 0.32.0 chromia-cli-tools 0.10.0 Fixed chr install --brid --url override chromia.yml duplicate progress docs latest $DOCS_LATEST_CLI $DOCS_LATEST_CLI_DATE source tags $CLI_SERIES skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex.
        Official help page (200): $HELP_URL
        Official BUILD cli/commands/help ($HELP_INDEX_URL 307 $HELP_INDEX_URL_SLASH 200 $HELP_INDEX_TITLE): intro Usage chr help [<options>] Show this message and exit Options -h, --help Show this message and exit The help command chr help shows general help information and lists all available commands Query-only WRITE SKIP skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no mnemonic keygen do not invent flags do not document chr tx signed send keygen samples.
        Official version page (200): $VERSION_URL
        Official BUILD cli/commands/version ($VERSION_INDEX_URL 307 $VERSION_INDEX_URL_SLASH 200 $VERSION_INDEX_TITLE): intro Usage chr version [<options>] Show the version and exit Options -h, --help Show this message and exit The version command chr version displays version information for the chr CLI tool and its components Query-only WRITE SKIP skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no mnemonic keygen do not invent flags do not document chr tx signed send keygen samples.
        Official BUILD cli/commands ($COMMANDS_INDEX_URL 307 $COMMANDS_INDEX_URL_SLASH 200 $COMMANDS_INDEX_TITLE): intro This section contains information about all commands and settings available in the Chromia CLI, including examples and flags note The default option of using Chromia CLI to interact with the Chromia blockchain is `--use-db`. If you want to use it without the database, then use `--no-db` child cards $COMMANDS_INDEX_CARD_HELP $HELP_URL Show command help and usage with `chr help` $COMMANDS_INDEX_CARD_VERSION $VERSION_URL Show the current CLI version with `chr version` $COMMANDS_INDEX_CARD_BUILD Run the `chr build` command to create a blockchain configuration for your dapp $COMMANDS_INDEX_CARD_CREATE_RELL_DAPP Use `chr create-rell-dapp` to generate a new Rell-based "Hello World" project $COMMANDS_INDEX_CARD_DEPLOYMENT Manage blockchain deployments with the `chr deployment` command $COMMANDS_INDEX_CARD_EIF The `chr eif` command provides access to Ethereum Integration Framework functionalities $COMMANDS_INDEX_CARD_GENERATE Generate client stubs and documentation for your Rell project using `chr generate` $COMMANDS_INDEX_CARD_LIBRARY Download and use third-party Rell libraries in your dapp with `chr library install` $COMMANDS_INDEX_CARD_KEYGEN WRITE SKIP keygen $COMMANDS_INDEX_CARD_NODE Start or update a node running your applications using `chr node` $COMMANDS_INDEX_CARD_QUERY Test and interact with local or deployed chains without a client using `chr query` $COMMANDS_INDEX_CARD_REPL Run Rell methods interactively in the shell with `chr repl`, ideal for troubleshooting $COMMANDS_INDEX_CARD_TEST Execute project-specific tests defined in `chromia.yml` with `chr test` $COMMANDS_INDEX_CARD_TX Sign and run transactions with `chr tx`, similar to the `query` command WRITE SKIP signed txs $COMMANDS_INDEX_CARD_CODE Manage code quality, including formatting and linting, using `chr code` $COMMANDS_INDEX_CARD_MULTI_SIGNATURE Handle multi-signer transactions using the `chr multi-signature` command WRITE SKIP signed txs $COMMANDS_INDEX_CARD_TOOLS Access various utilities and tools for Chromia development with `chr tools` $COMMANDS_INDEX_CARD_SEEDER Generate mock data for a local database with `chr seeder` skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex.
        Official docs-site latest listed CLI $DOCS_LATEST_CLI ($DOCS_LATEST_CLI_DATE); source tags $CLI_SERIES — state both. Do not invent flags from 0.30.x docs as if they were $CLI_SERIES-only.
        Official intro local commands: `chr node start`, `chr query`, `chr test`, `chr build`.
        Official intro remote commands: `chr deployment create`, `chr deployment update`.
        Official intro first-run: `chr create-rell-dapp` → `chr node start` → `chr query hello_world` (no extra options when a single local chain) → `chr test` → `chr deployment create`.
        Official intro: keys live in `~/.chromia/` (lookup only; this tool does not generate a key).
        Official intro: Postgres is required for local-blockchain CLI commands.
        Official 0.20.14: blockchain names cannot contain hyphens.
        Official 0.21.0: Java 21 required.
        Official 0.25.0: merkle hash calculator v2.
        Official 0.30.0: `chr build --skip-lib-check`; `chr deployment create` writes chromia.yml.
        Official usage: `chr help [<options>]` and `chr version [<options>]`.
        Official flag on both pages: `-h, --help` (Show this message and exit).
        `chr help` shows general help information and lists all available commands.
        `chr version` displays version information for the chr CLI tool and its components.
        CLI source VersionCommand prints CLI + rell + postchain + EIF + Java. Install docs also show `chr --version`.
        Official completion: `chr --generate-completion [bash|zsh|fish]`.
        Official examples: `chr --generate-completion bash >> ~/chr.sh`, `… zsh >> ~/chr.zsh`, `… fish >> ~/chr.fish`.
        Source those files from `.bashrc`, `.zshrc`, or `config.fish`.
        Official two-letter shortcut: first two letters of each command/subcommand (`chr de cr` = `chr deployment create`).
        Skipped (not official public help): `chr fetch-config` is hidden+experimental in CLI 0.33.x source and is not on the official command index.
        Skipped: `chr deployment lease-info` is hidden+experimental and is not on the live deployment page.
        Skipped: `chr deployment remove-container` is hidden and posts a signed economy-chain FT4 removeContainerOperation (no refund). Requires keys/tx — this tool does not document a procedure.
        Official ECOSYSTEM ecosystem/bridge/deploy-bridge/deploy-bridge-chains INDEX ($ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_URL 307 $ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/rell-masterclass/indexes INDEX ($LEARN_RELL_MASTERCLASS_INDEXES_INDEX_URL 301 $LEARN_RELL_MASTERCLASS_INDEXES_INDEX_URL_SLASH 200 H1 $LEARN_RELL_MASTERCLASS_INDEXES_INDEX_TITLE HELP ONLY WRITE SKIP). Indexes are crucial in improving database performance by allowing faster data retrieval In Rell, you have two options to mark an attribute for indexing Both improve query performance. Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/iccf-course/system-overview INDEX ($LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_URL 301 $LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_URL_SLASH 200 H1 $LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", COMMANDS_URL)
        put("commands_index_docs", COMMANDS_INDEX_URL)
        put("commands_index_url_slash", COMMANDS_INDEX_URL_SLASH)
        put("commands_index_title", COMMANDS_INDEX_TITLE)
        put("commands_index_card_help", COMMANDS_INDEX_CARD_HELP)
        put("commands_index_card_version", COMMANDS_INDEX_CARD_VERSION)
        put("commands_index_card_build", COMMANDS_INDEX_CARD_BUILD)
        put("commands_index_card_create_rell_dapp", COMMANDS_INDEX_CARD_CREATE_RELL_DAPP)
        put("commands_index_card_deployment", COMMANDS_INDEX_CARD_DEPLOYMENT)
        put("commands_index_card_eif", COMMANDS_INDEX_CARD_EIF)
        put("commands_index_card_generate", COMMANDS_INDEX_CARD_GENERATE)
        put("commands_index_card_library", COMMANDS_INDEX_CARD_LIBRARY)
        put("commands_index_card_keygen", COMMANDS_INDEX_CARD_KEYGEN)
        put("commands_index_card_node", COMMANDS_INDEX_CARD_NODE)
        put("commands_index_card_query", COMMANDS_INDEX_CARD_QUERY)
        put("commands_index_card_repl", COMMANDS_INDEX_CARD_REPL)
        put("commands_index_card_test", COMMANDS_INDEX_CARD_TEST)
        put("commands_index_card_tx", COMMANDS_INDEX_CARD_TX)
        put("commands_index_card_code", COMMANDS_INDEX_CARD_CODE)
        put("commands_index_card_multi_signature", COMMANDS_INDEX_CARD_MULTI_SIGNATURE)
        put("commands_index_card_tools", COMMANDS_INDEX_CARD_TOOLS)
        put("commands_index_card_seeder", COMMANDS_INDEX_CARD_SEEDER)
        put("help_docs", HELP_URL)
        put("help_index_docs", HELP_INDEX_URL)
        put("help_index_url_slash", HELP_INDEX_URL_SLASH)
        put("help_index_title", HELP_INDEX_TITLE)
        put("version_docs", VERSION_URL)
        put("version_index_docs", VERSION_INDEX_URL)
        put("version_index_url_slash", VERSION_INDEX_URL_SLASH)
        put("version_index_title", VERSION_INDEX_TITLE)
        put("intro_docs", INTRO_URL)
        put("cli_intro_index_docs", CLI_INTRO_INDEX_URL)
        put("cli_intro_index_url_slash", CLI_INTRO_INDEX_URL_SLASH)
        put("cli_intro_index_title", CLI_INTRO_INDEX_TITLE)
        put("release_notes_docs", RELEASE_NOTES_URL)
        put("cli_release_notes_index_docs", CLI_RELEASE_NOTES_INDEX_URL)
        put("cli_release_notes_index_url_slash", CLI_RELEASE_NOTES_INDEX_URL_SLASH)
        put("cli_release_notes_index_title", CLI_RELEASE_NOTES_INDEX_TITLE)
        put("docs_latest_cli", DOCS_LATEST_CLI)
        put("docs_latest_cli_date", DOCS_LATEST_CLI_DATE)
        put("source_cli", CLI_SERIES)
        put("tool", TOOL_NAME)
        put(
            "commands",
            buildJsonObject {
                put("help", "chr help")
                put("help_usage", "chr help [<options>]")
                put("version", "chr version")
                put("version_usage", "chr version [<options>]")
                put("version_short", "chr --version")
                put("completion_bash", "chr --generate-completion bash >> ~/chr.sh")
                put("completion_zsh", "chr --generate-completion zsh >> ~/chr.zsh")
                put("completion_fish", "chr --generate-completion fish >> ~/chr.fish")
                put("shortcut_example", "chr de cr")
                put("shortcut_equals", "chr deployment create")
                put("official_local_query", "chr query hello_world")
                put("official_local_build", "chr build")
                put("official_local_test", "chr test")
                put("official_local_node", "chr node start")
            }
        )
        put(
            "help_flags",
            buildJsonObject {
                put("help", "-h, --help  # official /build/cli/commands/help")
            }
        )
        put(
            "version_flags",
            buildJsonObject {
                put("help", "-h, --help  # official /build/cli/commands/version")
            }
        )
        put(
            "shells",
            buildJsonArray { shells.forEach { add(JsonPrimitive(it)) } }
        )
        put(
            "skipped",
            buildJsonObject {
                put(
                    "fetch_config",
                    "chr fetch-config — hidden + experimental in CLI 0.33.x source; not on the official command index"
                )
                put(
                    "deployment_lease_info",
                    "chr deployment lease-info — hidden + experimental; not on the live deployment page"
                )
                put(
                    "deployment_remove_container",
                    "chr deployment remove-container — hidden; signed economy-chain FT4 removeContainerOperation (no refund). Requires keys/tx — skipped"
                )
            }
        )
        put("ecosystem_deploy_bridge_chains_index_url_slash", ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_URL_SLASH)
        put("ecosystem_deploy_bridge_chains_index_title", ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_TITLE)
        put("learn_rell_masterclass_indexes_index_url_slash", LEARN_RELL_MASTERCLASS_INDEXES_INDEX_URL_SLASH)
        put("learn_rell_masterclass_indexes_index_title", LEARN_RELL_MASTERCLASS_INDEXES_INDEX_TITLE)
        put("learn_iccf_system_overview_index_url_slash", LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_URL_SLASH)
        put("learn_iccf_system_overview_index_title", LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_TITLE)

        put("notes", notes())
    }
}
// Official BUILD cli/commands/help encoded as HELP_INDEX_* (query-only).
// Official BUILD cli/commands/version encoded as VERSION_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/bridge/deploy-bridge/deploy-bridge-chains INDEX encoded as ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/rell-masterclass/indexes INDEX encoded as LEARN_RELL_MASTERCLASS_INDEXES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/iccf-course/system-overview INDEX encoded as LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
