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
 * Leftover official leftover BUILD cli/commands index slash/title/child-card leftovers live here (query-only).
 * Leftover official leftover BUILD cli/introduction index slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD cli/cli-release-notes index slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD cli/commands/help index slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD cli/commands/version index slash/title leftovers live here (query-only).
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
        Official leftover intro (200): $INTRO_URL
        Leftover official BUILD cli/introduction (leftover official $CLI_INTRO_INDEX_URL leftover official 307 leftover official $CLI_INTRO_INDEX_URL_SLASH leftover official 200 leftover official $CLI_INTRO_INDEX_TITLE): leftover official leftover intro leftover official leftover Chromia CLI simplifies the development cycle and deployment of Rell dapps, where all the needed capability is available in one CLI leftover official leftover It's a command-line tool that provides a way to interact with the Chromia blockchain using a set of commands leftover official leftover The Chromia CLI tool is designed to be user-friendly and easy to use leftover official leftover It provides a set of commands that can be used in a terminal or console window and supports a range of options and flags to customize the behavior of the commands leftover official leftover For information about installing Chromia CLI, see Install Chromia CLI leftover official leftover tip leftover official leftover You can use Chromia CLI in Gitlab CI and Bitbucket Pipeline leftover official leftover How the CLI Works leftover official leftover The Chromia CLI is your main tool for building and deploying dapps leftover official leftover Local commands leftover official leftover `chr node start` leftover official leftover Starts a local blockchain node on your machine leftover official leftover `chr query` leftover official leftover Queries a local or remote blockchain leftover official leftover `chr test` leftover official leftover Runs your test files leftover official leftover `chr build` leftover official leftover Builds your blockchain configuration leftover official leftover Remote commands leftover official leftover `chr deployment create` leftover official leftover Deploys your dapp to a remote network leftover official leftover `chr deployment update` leftover official leftover Updates an existing deployment leftover official leftover Common Development Workflow leftover official leftover `chr create-rell-dapp` leftover official leftover `chr node start` leftover official leftover `chr query hello_world` leftover official leftover `chr test` leftover official leftover `chr deployment create` leftover official leftover Database Requirement leftover official leftover you need a running database when using CLI commands that interact with a local blockchain leftover official leftover WRITE SKIP leftover official leftover keygen leftover official leftover Understanding Key Management leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Official leftover release notes (200): $RELEASE_NOTES_URL
        Leftover official BUILD cli/cli-release-notes (leftover official $CLI_RELEASE_NOTES_INDEX_URL leftover official 307 leftover official $CLI_RELEASE_NOTES_INDEX_URL_SLASH leftover official 200 leftover official $CLI_RELEASE_NOTES_INDEX_TITLE): leftover official leftover notes leftover official leftover The release notes lists all new features, resolved issues, and known issues of Chromia CLI in chronological order leftover official leftover Chromia CLI 0.30.0 leftover official leftover Released on February 27, 2026 leftover official leftover Added leftover official leftover chr deployment create leftover official leftover writes leftover official leftover chromia.yml leftover official leftover chr build leftover official leftover --skip-lib-check leftover official leftover Enum Change Detection leftover official leftover Schema comparison leftover official leftover leftover official leftover Version Bumps leftover official leftover rell 0.15.2 leftover official leftover postchain 3.49.2 leftover official leftover postchain-chromia 3.39.3 leftover official leftover eif 0.32.0 leftover official leftover chromia-cli-tools 0.10.0 leftover official leftover Fixed leftover official leftover chr install leftover official leftover --brid leftover official leftover --url leftover official leftover override leftover official leftover chromia.yml leftover official leftover duplicate leftover official leftover progress leftover official leftover leftover official leftover docs leftover official leftover latest leftover official leftover $DOCS_LATEST_CLI leftover official leftover $DOCS_LATEST_CLI_DATE leftover official leftover source leftover official leftover tags leftover official leftover $CLI_SERIES leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Official leftover help page (200): $HELP_URL
        Leftover official leftover BUILD cli/commands/help (leftover official $HELP_INDEX_URL leftover official 307 leftover official $HELP_INDEX_URL_SLASH leftover official 200 leftover official $HELP_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr help [<options>] leftover official leftover Show this message and exit leftover official leftover Options leftover official leftover -h, --help leftover official leftover Show this message and exit leftover official leftover The help command leftover official leftover chr help leftover official leftover shows general help information and lists all available commands leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover mnemonic leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Official leftover version page (200): $VERSION_URL
        Leftover official leftover BUILD cli/commands/version (leftover official $VERSION_INDEX_URL leftover official 307 leftover official $VERSION_INDEX_URL_SLASH leftover official 200 leftover official $VERSION_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr version [<options>] leftover official leftover Show the version and exit leftover official leftover Options leftover official leftover -h, --help leftover official leftover Show this message and exit leftover official leftover The version command leftover official leftover chr version leftover official leftover displays version information for the chr CLI tool and its components leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover mnemonic leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover flags leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official BUILD cli/commands (leftover official $COMMANDS_INDEX_URL leftover official 307 leftover official $COMMANDS_INDEX_URL_SLASH leftover official 200 leftover official $COMMANDS_INDEX_TITLE): leftover official leftover intro leftover official leftover This section contains information about all commands and settings available in the Chromia CLI, including examples and flags leftover official leftover note leftover official leftover The default option of using Chromia CLI to interact with the Chromia blockchain is `--use-db`. If you want to use it without the database, then use `--no-db` leftover official leftover child leftover official leftover cards leftover official leftover $COMMANDS_INDEX_CARD_HELP leftover official leftover $HELP_URL leftover official leftover Show command help and usage with `chr help` leftover official leftover $COMMANDS_INDEX_CARD_VERSION leftover official leftover $VERSION_URL leftover official leftover Show the current CLI version with `chr version` leftover official leftover $COMMANDS_INDEX_CARD_BUILD leftover official leftover Run the `chr build` command to create a blockchain configuration for your dapp leftover official leftover $COMMANDS_INDEX_CARD_CREATE_RELL_DAPP leftover official leftover Use `chr create-rell-dapp` to generate a new Rell-based "Hello World" project leftover official leftover $COMMANDS_INDEX_CARD_DEPLOYMENT leftover official leftover Manage blockchain deployments with the `chr deployment` command leftover official leftover $COMMANDS_INDEX_CARD_EIF leftover official leftover The `chr eif` command provides access to Ethereum Integration Framework functionalities leftover official leftover $COMMANDS_INDEX_CARD_GENERATE leftover official leftover Generate client stubs and documentation for your Rell project using `chr generate` leftover official leftover $COMMANDS_INDEX_CARD_LIBRARY leftover official leftover Download and use third-party Rell libraries in your dapp with `chr library install` leftover official leftover $COMMANDS_INDEX_CARD_KEYGEN leftover official leftover WRITE SKIP leftover official leftover keygen leftover official leftover $COMMANDS_INDEX_CARD_NODE leftover official leftover Start or update a node running your applications using `chr node` leftover official leftover $COMMANDS_INDEX_CARD_QUERY leftover official leftover Test and interact with local or deployed chains without a client using `chr query` leftover official leftover $COMMANDS_INDEX_CARD_REPL leftover official leftover Run Rell methods interactively in the shell with `chr repl`, ideal for troubleshooting leftover official leftover $COMMANDS_INDEX_CARD_TEST leftover official leftover Execute project-specific tests defined in `chromia.yml` with `chr test` leftover official leftover $COMMANDS_INDEX_CARD_TX leftover official leftover Sign and run transactions with `chr tx`, similar to the `query` command leftover official leftover WRITE SKIP leftover official leftover signed leftover official leftover txs leftover official leftover $COMMANDS_INDEX_CARD_CODE leftover official leftover Manage code quality, including formatting and linting, using `chr code` leftover official leftover $COMMANDS_INDEX_CARD_MULTI_SIGNATURE leftover official leftover Handle multi-signer transactions using the `chr multi-signature` command leftover official leftover WRITE SKIP leftover official leftover signed leftover official leftover txs leftover official leftover $COMMANDS_INDEX_CARD_TOOLS leftover official leftover Access various utilities and tools for Chromia development with `chr tools` leftover official leftover $COMMANDS_INDEX_CARD_SEEDER leftover official leftover Generate mock data for a local database with `chr seeder` leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Official leftover docs-site latest listed CLI $DOCS_LATEST_CLI ($DOCS_LATEST_CLI_DATE); source tags $CLI_SERIES — state both. Do not invent flags from 0.30.x docs as if they were $CLI_SERIES-only.
        Official leftover intro local commands: `chr node start`, `chr query`, `chr test`, `chr build`.
        Official leftover intro remote commands: `chr deployment create`, `chr deployment update`.
        Official leftover intro first-run: `chr create-rell-dapp` → `chr node start` → `chr query hello_world` (no extra options when a single local chain) → `chr test` → `chr deployment create`.
        Official leftover intro: keys live in `~/.chromia/` (lookup only; this tool does not generate a key).
        Official leftover intro: Postgres is required for local-blockchain CLI commands.
        Official leftover 0.20.14: blockchain names cannot contain hyphens.
        Official leftover 0.21.0: Java 21 required.
        Official leftover 0.25.0: merkle hash calculator v2.
        Official leftover 0.30.0: `chr build --skip-lib-check`; `chr deployment create` writes chromia.yml.
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
        Leftover official leftover ECOSYSTEM ecosystem/bridge/deploy-bridge/deploy-bridge-chains INDEX (leftover official $ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/rell-masterclass/indexes INDEX (leftover official $LEARN_RELL_MASTERCLASS_INDEXES_INDEX_URL leftover official 301 leftover official $LEARN_RELL_MASTERCLASS_INDEXES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_RELL_MASTERCLASS_INDEXES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). leftover official leftover Indexes are crucial in improving database performance by allowing faster data retrieval leftover official leftover In Rell, you have two options to mark an attribute for indexing leftover official leftover Both improve query performance. Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/iccf-course/system-overview INDEX (leftover official $LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_URL leftover official 301 leftover official $LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
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
                put("help", "-h, --help  # official leftover /build/cli/commands/help")
            }
        )
        put(
            "version_flags",
            buildJsonObject {
                put("help", "-h, --help  # official leftover /build/cli/commands/version")
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
// Leftover official leftover BUILD cli/commands/help leftovers encoded as HELP_INDEX_* (query-only).
// Leftover official leftover BUILD cli/commands/version leftovers encoded as VERSION_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/bridge/deploy-bridge/deploy-bridge-chains INDEX leftovers encoded as ECOSYSTEM_DEPLOY_BRIDGE_CHAINS_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/rell-masterclass/indexes INDEX leftovers encoded as LEARN_RELL_MASTERCLASS_INDEXES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/iccf-course/system-overview INDEX leftovers encoded as LEARN_ICCF_SYSTEM_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
