package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Chromia CLI 0.33.x `chr seeder` help (init / generate).
 * Does not run chr, generate a key, invent a BRID, or send signed transactions.
 * Source: docs.chromia.com/build/cli/commands/seeder, docs.chromia.com/build/cli/Seeder,
 * and chromia-cli SeederCommand (0.33.x). Early-stage; may change.
 * Official BUILD cli/Seeder index slash/title values live here (query-only).
 * Official BUILD cli/Seeder/generator index slash/title values live here (query-only).
 * Official BUILD cli/Seeder/seeder-example index slash/title values live here (query-only).
 * Official BUILD cli/Seeder/configurable-generators index slash/title values live here (query-only).
 * Official BUILD cli/commands/seeder index slash/title values live here (query-only).
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
        Official BUILD cli/Seeder ($SEEDER_INDEX_URL 307 $SEEDER_INDEX_URL_SLASH 200 $SEEDER_INDEX_TITLE): intro The Seeder command is a powerful CLI tool that helps you manage and populate your Chromia blockchain with initial data It's particularly useful during development and testing phases when you need to set up your blockchain with predefined data structures and content The seeder allows you to Create and manage seed data for your blockchain Populate your blockchain with test data h1 Seeder command Basic usage chr seeder init Initialize a new seeder configuration chr seeder generate Generate the Rell seeder modules skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex.
        Official BUILD cli/Seeder/generator ($SEEDER_GENERATOR_INDEX_URL 307 $SEEDER_GENERATOR_INDEX_URL_SLASH 200 $SEEDER_GENERATOR_INDEX_TITLE): intro This topic provides a comprehensive catalog of all data generators available in the Rell Toolbox Seeder module These generators create realistic mock data for development, testing, and database seeding purposes Each generator produces Using generators To use a specific generator, simply reference its id Query-only WRITE SKIP skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no invented BRIDs do not invent seeder.yml keys.
        Official BUILD cli/Seeder/seeder-example ($SEEDER_EXAMPLE_INDEX_URL 307 $SEEDER_EXAMPLE_INDEX_URL_SLASH 200 $SEEDER_EXAMPLE_INDEX_TITLE): intro This topic explains how to use the seeder commands to generate test data for your Rell projects The seeder functionality helps you create realistic test data for your local database used by your blockchain during development and testing The Chromia CLI provides two commands chr seeder init Generates the initial seeder configuration chr seeder generate Creates a Rell module Query-only WRITE SKIP skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no invented BRIDs do not invent seeder.yml keys.
        Official BUILD cli/Seeder/configurable-generators ($SEEDER_CONFIGURABLE_INDEX_URL 307 $SEEDER_CONFIGURABLE_INDEX_URL_SLASH 200 $SEEDER_CONFIGURABLE_INDEX_TITLE): intro The following generators support additional configuration options Query-only WRITE SKIP skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no invented BRIDs do not invent seeder.yml keys.
        Official BUILD cli/commands/seeder ($COMMANDS_SEEDER_INDEX_URL 307 $COMMANDS_SEEDER_INDEX_URL_SLASH 200 $COMMANDS_SEEDER_INDEX_TITLE): intro Usage chr seeder init generate Query-only WRITE SKIP skip signed txs sample admin pubkey keygen no sample keys no invented 64-hex no keygen do not invent seeder.yml keys flags do not document chr tx signed send keygen samples.
        Official ECOSYSTEM ecosystem/providers/pmc/troubleshooting INDEX ($ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_URL 307 $ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/nodes/api INDEX ($ECOSYSTEM_NODES_API_INDEX_URL 307 $ECOSYSTEM_NODES_API_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_NODES_API_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/help INDEX ($ECOSYSTEM_PMC_HELP_INDEX_URL 307 $ECOSYSTEM_PMC_HELP_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_HELP_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/subnode-jar-extension INDEX ($ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_URL 307 $ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/ft4-asset/ft4-basics INDEX ($LEARN_FT4_ASSET_BASICS_INDEX_URL 301 $LEARN_FT4_ASSET_BASICS_INDEX_URL_SLASH 200 H1 $LEARN_FT4_ASSET_BASICS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/icmf-course/setup INDEX ($LEARN_ICMF_SETUP_INDEX_URL 301 $LEARN_ICMF_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_ICMF_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/marketplace-course/module-assets/test-marketplace INDEX ($LEARN_MARKETPLACE_TEST_INDEX_URL 301 $LEARN_MARKETPLACE_TEST_INDEX_URL_SLASH 200 H1 $LEARN_MARKETPLACE_TEST_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official RELL rell/language-features/modules/operation INDEX ($RELL_MODULE_OPERATION_INDEX_URL 307 $RELL_MODULE_OPERATION_INDEX_URL_SLASH 200 H1 $RELL_MODULE_OPERATION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/Concepts INDEX ($LEARN_TAGS_CONCEPTS_INDEX_URL 301 $LEARN_TAGS_CONCEPTS_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_CONCEPTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
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
// Official BUILD cli/Seeder/configurable-generators encoded as SEEDER_CONFIGURABLE_INDEX_* (query-only).
// Official BUILD cli/commands/seeder encoded as COMMANDS_SEEDER_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/pmc/troubleshooting INDEX encoded as ECOSYSTEM_PMC_TROUBLESHOOTING_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/nodes/api INDEX encoded as ECOSYSTEM_NODES_API_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/help INDEX encoded as ECOSYSTEM_PMC_HELP_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/subnode-jar-extension INDEX encoded as ECOSYSTEM_PMC_SUBNODE_JAR_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/ft4-asset/ft4-basics INDEX encoded as LEARN_FT4_ASSET_BASICS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/icmf-course/setup INDEX encoded as LEARN_ICMF_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/marketplace-course/module-assets/test-marketplace INDEX encoded as LEARN_MARKETPLACE_TEST_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/modules/operation INDEX encoded as RELL_MODULE_OPERATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/Concepts INDEX encoded as LEARN_TAGS_CONCEPTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
