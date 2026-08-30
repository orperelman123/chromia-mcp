package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr seeder` help (init / generate).
 * Does not run chr, generate a key, invent a BRID, or send signed transactions.
 * Source: docs.chromia.com/build/cli/commands/seeder, docs.chromia.com/build/cli/Seeder,
 * and chromia-cli SeederCommand (0.33.x). Early-stage; may change.
 * Leftover official leftover BUILD cli/Seeder index slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD cli/Seeder/generator index slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD cli/Seeder/seeder-example index slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD cli/Seeder/configurable-generators index slash/title leftovers live here (query-only).
 * Leftover official leftover BUILD cli/commands/seeder index slash/title leftovers live here (query-only).
 */
object ChrSeederHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val DOCS_URL = "https://docs.chromia.com/build/cli/commands/seeder"
    const val COMMANDS_SEEDER_INDEX_URL = DOCS_URL
    const val COMMANDS_SEEDER_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/commands/seeder/"
    const val COMMANDS_SEEDER_INDEX_TITLE = "seeder"  // official H1
    const val GUIDE_URL = "https://docs.chromia.com/build/cli/Seeder"
    const val SEEDER_INDEX_URL = GUIDE_URL
    const val SEEDER_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/Seeder/"
    const val SEEDER_INDEX_TITLE = "Seeder"
    const val SEEDER_GENERATOR_INDEX_URL = "https://docs.chromia.com/build/cli/Seeder/generator"
    const val SEEDER_GENERATOR_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/Seeder/generator/"
    const val SEEDER_GENERATOR_INDEX_TITLE = "Available generators"
    const val SEEDER_EXAMPLE_INDEX_URL = "https://docs.chromia.com/build/cli/Seeder/seeder-example"
    const val SEEDER_EXAMPLE_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/Seeder/seeder-example/"
    const val SEEDER_EXAMPLE_INDEX_TITLE = "Using the seeder"
    const val SEEDER_CONFIGURABLE_INDEX_URL = "https://docs.chromia.com/build/cli/Seeder/configurable-generators"
    const val SEEDER_CONFIGURABLE_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/Seeder/configurable-generators/"
    const val SEEDER_CONFIGURABLE_INDEX_TITLE = "Configurable generators"
    const val ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/troubleshooting"
    const val ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/troubleshooting/"
    const val ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_TITLE = "Postchain troubleshooting"  // official H1
    const val ECOSYSTEM_NODES_API_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/api"
    const val ECOSYSTEM_NODES_API_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/api/"
    const val ECOSYSTEM_NODES_API_INDEX_TITLE = "Node management API"  // official H1
    const val ECOSYSTEM_PMC_HELP_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/help"
    const val ECOSYSTEM_PMC_HELP_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/help/"
    const val ECOSYSTEM_PMC_HELP_INDEX_TITLE = "help"  // official H1
    const val ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/subnode-jar-extension"
    const val ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/subnode-jar-extension/"
    const val ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_TITLE = "subnode-jar-extension"  // official H1
    const val LEARN_FT4_ASSET_BASICS_INDEX_URL = "https://learn.chromia.com/courses/ft4-asset/ft4-basics"
    const val LEARN_FT4_ASSET_BASICS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-asset/ft4-basics/"
    const val LEARN_FT4_ASSET_BASICS_INDEX_TITLE = "Asset basics"  // official H1
    const val LEARN_ICMF_SETUP_INDEX_URL = "https://learn.chromia.com/courses/icmf-course/setup"
    const val LEARN_ICMF_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/icmf-course/setup/"
    const val LEARN_ICMF_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_MARKETPLACE_TEST_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-assets/test-marketplace"
    const val LEARN_MARKETPLACE_TEST_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-assets/test-marketplace/"
    const val LEARN_MARKETPLACE_TEST_INDEX_TITLE = "Test our marketplace"  // official H1
    const val RELL_MODULE_OPERATION_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/operation"
    const val RELL_MODULE_OPERATION_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/operation/"
    const val RELL_MODULE_OPERATION_INDEX_TITLE = "Operation"  // official H1
    const val LEARN_TAGS_CONCEPTS_INDEX_URL = "https://learn.chromia.com/tags/Concepts"
    const val LEARN_TAGS_CONCEPTS_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Concepts/"
    const val LEARN_TAGS_CONCEPTS_INDEX_TITLE = "Courses tagged with: Concepts"  // official H1
    const val TOOL_NAME = "chr_seeder_help"
    const val DEFAULT_CONFIG_FOLDER = ".chromia/seeder"

    fun notes(): String = """
        Chromia CLI $CLI_SERIES `chr seeder` help. Java 21+, Postgres 16+.
        Official command page: $DOCS_URL
        Narrative page: $GUIDE_URL
        Official verbs: init, generate. Docs mark this as early-stage and subject to change.
        `chr seeder init` writes initial seeder configuration (defaults to all blockchains).
        `chr seeder generate` writes a Rell seeder module from that configuration.
        Official flags: -s/--settings, -bc/--blockchain (repeatable; defaults to all).
        generate also has --alternative-config-folder=<path>.
        Source default config folder is $DEFAULT_CONFIG_FOLDER (per blockchain: $DEFAULT_CONFIG_FOLDER/<chain>/seeder.yml).
        Source generate output is compile.source/seeder/seed_<blockchain>.rell.
        Official pages do not publish a seeder.yml key schema — do not invent keys.
        Blockchain must have a main module; library chains are supported in source.
        Leftover official BUILD cli/Seeder (leftover official $SEEDER_INDEX_URL leftover official 307 leftover official $SEEDER_INDEX_URL_SLASH leftover official 200 leftover official $SEEDER_INDEX_TITLE): leftover official leftover intro leftover official leftover The Seeder command is a powerful CLI tool that helps you manage and populate your Chromia blockchain with initial data leftover official leftover It's particularly useful during development and testing phases when you need to set up your blockchain with predefined data structures and content leftover official leftover The seeder allows you to leftover official leftover Create and manage seed data for your blockchain leftover official leftover Populate your blockchain with test data leftover official leftover h1 leftover official leftover Seeder command leftover official leftover Basic usage leftover official leftover chr seeder init leftover official leftover Initialize a new seeder configuration leftover official leftover chr seeder generate leftover official leftover Generate the Rell seeder modules leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Leftover official BUILD cli/Seeder/generator (leftover official $SEEDER_GENERATOR_INDEX_URL leftover official 307 leftover official $SEEDER_GENERATOR_INDEX_URL_SLASH leftover official 200 leftover official $SEEDER_GENERATOR_INDEX_TITLE): leftover official leftover intro leftover official leftover This topic provides a comprehensive catalog of all data generators available in the Rell Toolbox Seeder module leftover official leftover These generators create realistic mock data for development, testing, and database seeding purposes leftover official leftover Each generator produces leftover official leftover Using generators leftover official leftover To use a specific generator, simply reference its id leftover official leftover Query-only leftover official leftover WRITE leftover official leftover SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover invented leftover official leftover BRIDs leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover seeder.yml leftover official leftover keys.
        Leftover official BUILD cli/Seeder/seeder-example (leftover official $SEEDER_EXAMPLE_INDEX_URL leftover official 307 leftover official $SEEDER_EXAMPLE_INDEX_URL_SLASH leftover official 200 leftover official $SEEDER_EXAMPLE_INDEX_TITLE): leftover official leftover intro leftover official leftover This topic explains how to use the seeder commands to generate test data for your Rell projects leftover official leftover The seeder functionality helps you create realistic test data for your local database used by your blockchain during development and testing leftover official leftover The Chromia CLI provides two commands leftover official leftover chr seeder init leftover official leftover Generates the initial seeder configuration leftover official leftover chr seeder generate leftover official leftover Creates a Rell module leftover official leftover Query-only leftover official leftover WRITE leftover official leftover SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover invented leftover official leftover BRIDs leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover seeder.yml leftover official leftover keys.
        Leftover official BUILD cli/Seeder/configurable-generators (leftover official $SEEDER_CONFIGURABLE_INDEX_URL leftover official 307 leftover official $SEEDER_CONFIGURABLE_INDEX_URL_SLASH leftover official 200 leftover official $SEEDER_CONFIGURABLE_INDEX_TITLE): leftover official leftover intro leftover official leftover The following generators support additional configuration options leftover official leftover Query-only leftover official leftover WRITE leftover official leftover SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover invented leftover official leftover BRIDs leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover seeder.yml leftover official leftover keys.
        Leftover official leftover BUILD cli/commands/seeder (leftover official $COMMANDS_SEEDER_INDEX_URL leftover official 307 leftover official $COMMANDS_SEEDER_INDEX_URL_SLASH leftover official 200 leftover official $COMMANDS_SEEDER_INDEX_TITLE): leftover official leftover intro leftover official leftover Usage leftover official leftover chr seeder leftover official leftover init leftover official leftover generate leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover seeder.yml leftover official leftover keys leftover official leftover leftover official leftover flags leftover official leftover do leftover official leftover not leftover official leftover document leftover official leftover chr leftover official leftover tx leftover official leftover leftover official leftover signed leftover official leftover send leftover official leftover leftover official leftover keygen leftover official leftover samples.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/troubleshooting INDEX (leftover official $ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/api INDEX (leftover official $ECOSYSTEM_NODES_API_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_NODES_API_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_NODES_API_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/help INDEX (leftover official $ECOSYSTEM_PMC_HELP_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_HELP_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_HELP_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/subnode-jar-extension INDEX (leftover official $ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/ft4-asset/ft4-basics INDEX (leftover official $LEARN_FT4_ASSET_BASICS_INDEX_URL leftover official 301 leftover official $LEARN_FT4_ASSET_BASICS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_ASSET_BASICS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/icmf-course/setup INDEX (leftover official $LEARN_ICMF_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_ICMF_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICMF_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-assets/test-marketplace INDEX (leftover official $LEARN_MARKETPLACE_TEST_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_TEST_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_TEST_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover RELL rell/language-features/modules/operation INDEX (leftover official $RELL_MODULE_OPERATION_INDEX_URL leftover official 307 leftover official $RELL_MODULE_OPERATION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_MODULE_OPERATION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/Concepts INDEX (leftover official $LEARN_TAGS_CONCEPTS_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_CONCEPTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_CONCEPTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("docs", DOCS_URL)
        put("commands_seeder_index_docs", COMMANDS_SEEDER_INDEX_URL)
        put("commands_seeder_index_url_slash", COMMANDS_SEEDER_INDEX_URL_SLASH)
        put("commands_seeder_index_title", COMMANDS_SEEDER_INDEX_TITLE)
        put("guide", GUIDE_URL)
        put("seeder_index_docs", SEEDER_INDEX_URL)
        put("seeder_index_url_slash", SEEDER_INDEX_URL_SLASH)
        put("seeder_index_title", SEEDER_INDEX_TITLE)
        put("seeder_generator_index_docs", SEEDER_GENERATOR_INDEX_URL)
        put("seeder_generator_index_url_slash", SEEDER_GENERATOR_INDEX_URL_SLASH)
        put("seeder_generator_index_title", SEEDER_GENERATOR_INDEX_TITLE)
        put("seeder_example_index_docs", SEEDER_EXAMPLE_INDEX_URL)
        put("seeder_example_index_url_slash", SEEDER_EXAMPLE_INDEX_URL_SLASH)
        put("seeder_example_index_title", SEEDER_EXAMPLE_INDEX_TITLE)
        put("seeder_configurable_index_docs", SEEDER_CONFIGURABLE_INDEX_URL)
        put("seeder_configurable_index_url_slash", SEEDER_CONFIGURABLE_INDEX_URL_SLASH)
        put("seeder_configurable_index_title", SEEDER_CONFIGURABLE_INDEX_TITLE)
        put("tool", TOOL_NAME)
        put("early_stage", true)
        put("default_config_folder", DEFAULT_CONFIG_FOLDER)
        put(
            "commands",
            buildJsonObject {
                put("seeder", "chr seeder")
                put("init", "chr seeder init")
                put("init_blockchain", "chr seeder init --blockchain hello")
                put("generate", "chr seeder generate")
                put("generate_blockchain", "chr seeder generate --blockchain hello")
            }
        )
        put(
            "init_flags",
            buildJsonObject {
                put("settings", "-s, --settings=<settings>")
                put("blockchain", "-bc, --blockchain=<blockchain>  # repeatable; defaults to all")
            }
        )
        put(
            "generate_flags",
            buildJsonObject {
                put("settings", "-s, --settings=<settings>")
                put("alternative_config_folder", "--alternative-config-folder=<path>")
                put("blockchain", "-bc, --blockchain=<blockchain>  # repeatable; defaults to all")
            }
        )
        put("output_rell", "src/seeder/seed_<blockchain>.rell  # compile.source/seeder; source SeederGenerateCommand")
        put("ecosystem_pmc_troubleshooting_index_url_slash", ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_URL_SLASH)
        put("ecosystem_pmc_troubleshooting_index_title", ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_TITLE)
        put("ecosystem_nodes_api_index_url_slash", ECOSYSTEM_NODES_API_INDEX_URL_SLASH)
        put("ecosystem_nodes_api_index_title", ECOSYSTEM_NODES_API_INDEX_TITLE)
        put("ecosystem_pmc_help_index_url_slash", ECOSYSTEM_PMC_HELP_INDEX_URL_SLASH)
        put("ecosystem_pmc_help_index_title", ECOSYSTEM_PMC_HELP_INDEX_TITLE)
        put("ecosystem_pmc_subnode_jar_index_url_slash", ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_URL_SLASH)
        put("ecosystem_pmc_subnode_jar_index_title", ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_TITLE)
        put("learn_ft4_asset_basics_index_url_slash", LEARN_FT4_ASSET_BASICS_INDEX_URL_SLASH)
        put("learn_ft4_asset_basics_index_title", LEARN_FT4_ASSET_BASICS_INDEX_TITLE)
        put("learn_icmf_setup_index_url_slash", LEARN_ICMF_SETUP_INDEX_URL_SLASH)
        put("learn_icmf_setup_index_title", LEARN_ICMF_SETUP_INDEX_TITLE)
        put("learn_marketplace_test_index_url_slash", LEARN_MARKETPLACE_TEST_INDEX_URL_SLASH)
        put("learn_marketplace_test_index_title", LEARN_MARKETPLACE_TEST_INDEX_TITLE)

        put("rell_module_operation_index_url_slash", RELL_MODULE_OPERATION_INDEX_URL_SLASH)
        put("rell_module_operation_index_title", RELL_MODULE_OPERATION_INDEX_TITLE)
        put("learn_tags_concepts_index_url_slash", LEARN_TAGS_CONCEPTS_INDEX_URL_SLASH)
        put("learn_tags_concepts_index_title", LEARN_TAGS_CONCEPTS_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover BUILD cli/Seeder/configurable-generators leftovers encoded as SEEDER_CONFIGURABLE_INDEX_* (query-only).
// Leftover official leftover BUILD cli/commands/seeder leftovers encoded as COMMANDS_SEEDER_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/troubleshooting INDEX leftovers encoded as ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/api INDEX leftovers encoded as ECOSYSTEM_NODES_API_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/help INDEX leftovers encoded as ECOSYSTEM_PMC_HELP_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/subnode-jar-extension INDEX leftovers encoded as ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/ft4-asset/ft4-basics INDEX leftovers encoded as LEARN_FT4_ASSET_BASICS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/icmf-course/setup INDEX leftovers encoded as LEARN_ICMF_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/module-assets/test-marketplace INDEX leftovers encoded as LEARN_MARKETPLACE_TEST_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/modules/operation INDEX leftovers encoded as RELL_MODULE_OPERATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/Concepts INDEX leftovers encoded as LEARN_TAGS_CONCEPTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
