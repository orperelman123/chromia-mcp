package org.chromia.tools

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official chromia.yml `definitions` / YAML anchors / `!include` help.
 * Examples from docs.chromia.com/build/configuration/project-config only.
 * Does not invent include semantics. Does not resolve aliases. Does not run chr.
 */
object ChromiaYmlDefinitionsHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting"
    const val ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/"
    const val ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_TITLE = "Troubleshoot the deployed bridge"  // official H1
    const val ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/provider-keypair"
    const val ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/provider-keypair/"
    const val ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_TITLE = "Configure provider key pair"  // official H1
    const val ECOSYSTEM_PMC_PROVIDER_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/provider"
    const val ECOSYSTEM_PMC_PROVIDER_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/provider/"
    const val ECOSYSTEM_PMC_PROVIDER_INDEX_TITLE = "provider"  // official H1
    const val LEARN_FT4_ASSET_OPERATIONS_INDEX_URL = "https://learn.chromia.com/courses/ft4-asset/asset-operations"
    const val LEARN_FT4_ASSET_OPERATIONS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-asset/asset-operations/"
    const val LEARN_FT4_ASSET_OPERATIONS_INDEX_TITLE = "Asset functions, operations & queries"  // official H1
    const val LEARN_ICMF_MANUAL_TESTING_INDEX_URL = "https://learn.chromia.com/courses/icmf-course/manual-testing"
    const val LEARN_ICMF_MANUAL_TESTING_INDEX_URL_SLASH = "https://learn.chromia.com/courses/icmf-course/manual-testing/"
    const val LEARN_ICMF_MANUAL_TESTING_INDEX_TITLE = "Test the dapp"  // official H1
    const val LEARN_NEWS_WHAT_NEXT_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/what-next"
    const val LEARN_NEWS_WHAT_NEXT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/what-next/"
    const val LEARN_NEWS_WHAT_NEXT_INDEX_TITLE = "What is next?"  // official H1
    const val LEARN_INSTALL_POSTGRES_INDEX_URL = "https://learn.chromia.com/docs/install/database-setup"
    const val LEARN_INSTALL_POSTGRES_INDEX_URL_SLASH = "https://learn.chromia.com/docs/install/database-setup/"
    const val LEARN_INSTALL_POSTGRES_INDEX_TITLE = "Set up PostgreSQL database"  // official H1
    const val LEARN_NEWS_AUTHENTICATION_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts/authentication"
    const val LEARN_NEWS_AUTHENTICATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts/authentication/"
    const val LEARN_NEWS_AUTHENTICATION_INDEX_TITLE = "Authentication with FT4 accounts"  // official H1
    const val LEARN_ZK_FRONTEND_TEST_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-test"
    const val LEARN_ZK_FRONTEND_TEST_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-test/"
    const val LEARN_ZK_FRONTEND_TEST_INDEX_TITLE = "Frontend: test"  // official H1
    const val LEARN_WEB3_INTRO_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/introduction"
    const val LEARN_WEB3_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/introduction/"
    const val LEARN_WEB3_INTRO_INDEX_TITLE = "Web3 for Web2 developers"  // official H1
    const val LEARN_COMPARISONS_COSMOS_INDEX_URL = "https://learn.chromia.com/courses/chromia-comparisons/cosmos"
    const val LEARN_COMPARISONS_COSMOS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-comparisons/cosmos/"
    const val LEARN_COMPARISONS_COSMOS_INDEX_TITLE = "Cosmos"  // official H1
    const val RELL_TESTS_INDEX_URL = "https://docs.chromia.com/rell/tests"
    const val RELL_TESTS_INDEX_URL_SLASH = "https://docs.chromia.com/rell/tests/"
    const val RELL_TESTS_INDEX_TITLE = "Test Rell code"  // official H1
    const val LEARN_TAGS_METAMASK_INDEX_URL = "https://learn.chromia.com/tags/MetaMask"
    const val LEARN_TAGS_METAMASK_INDEX_URL_SLASH = "https://learn.chromia.com/tags/MetaMask/"
    const val LEARN_TAGS_METAMASK_INDEX_TITLE = "Courses tagged with: MetaMask"  // official H1
    const val TOOL_NAME = "chromia_yml_definitions_help"

    fun anchorsYaml(): String = """
        definitions: #Used for YAML anchors in this file
          modules: &test
            - test.arithmetic_test
            - test.data_test

        blockchains:
          hello:
            module: main
        test:
          modules: *test
    """.trimIndent() + "\n"

    fun includeSourceYaml(): String = """
        #test.yml
        a: 13
        b: 15
    """.trimIndent() + "\n"

    fun includeWholeFileYaml(): String = """
        #chromia.yml
        blockchains:
          hello:
            module: main
        test:
          modules: !include test.yml
    """.trimIndent() + "\n"

    fun includeWholeFileResultYaml(): String = """
        #chromia.yml
        blockchains:
          hello:
            module: main
        test:
          modules:
            a: 13
            b: 15
    """.trimIndent() + "\n"

    fun includeTagYaml(): String = """
        #chromia.yml
        blockchains:
          hello:
            module: main
        test:
          modules: !include test.yml#a
    """.trimIndent() + "\n"

    fun includeTagResultYaml(): String = """
        #chromia.yml
        blockchains:
          hello:
            module: main
        test:
          modules: 13
    """.trimIndent() + "\n"

    fun notes(): String = """
        Official chromia.yml YAML anchors and !include for Chromia CLI $CLI_SERIES.
        Schema: $PROJECT_CONFIG_URL (source of truth). Official examples only.
        Any anchored value must live under the `definitions` key. Official example: `definitions.modules: &test` then `test.modules: *test`.
        `!include other.yml` inlines that file's YAML in place. Official example: `test.modules: !include test.yml` becomes the mapping a: 13 / b: 15.
        `!include other.yml#tag` includes a specific tag from that file. Official example: `!include test.yml#a` becomes 13 (the value of key a).
        Do not invent include semantics: no directory include, no glob, no recursive include, no YAML merge-key docs on this page.
        Official ICMF course also puts YAML anchors under `definitions:` as a sequence (`&sender`, `&receiver`, `&sender_receiver`) using official ICMF GTX modules from blockchain-properties. That course does not add include rules.
        validate_chromia_yml / SimpleYaml do not resolve aliases or !include — they parse a subset (maps, lists, scalars). Use chr / the official loader for resolved config.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover ECOSYSTEM ecosystem/bridge/bridge-troubleshooting INDEX (leftover official $ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/provider-keypair INDEX (leftover official $ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/provider INDEX (leftover official $ECOSYSTEM_PMC_PROVIDER_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_PROVIDER_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_PROVIDER_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/ft4-asset/asset-operations INDEX (leftover official $LEARN_FT4_ASSET_OPERATIONS_INDEX_URL leftover official 301 leftover official $LEARN_FT4_ASSET_OPERATIONS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_ASSET_OPERATIONS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/icmf-course/manual-testing INDEX (leftover official $LEARN_ICMF_MANUAL_TESTING_INDEX_URL leftover official 301 leftover official $LEARN_ICMF_MANUAL_TESTING_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICMF_MANUAL_TESTING_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/my-news-feed/what-next INDEX (leftover official $LEARN_NEWS_WHAT_NEXT_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_WHAT_NEXT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_WHAT_NEXT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN docs/install/database-setup INDEX (leftover official $LEARN_INSTALL_POSTGRES_INDEX_URL leftover official 301 leftover official $LEARN_INSTALL_POSTGRES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_INSTALL_POSTGRES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX news-feed authentication (leftover official $LEARN_NEWS_AUTHENTICATION_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_AUTHENTICATION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_AUTHENTICATION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX zero-knowledge-proof frontend-test (leftover official $LEARN_ZK_FRONTEND_TEST_INDEX_URL leftover official 301 leftover official $LEARN_ZK_FRONTEND_TEST_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ZK_FRONTEND_TEST_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX web3-for-web2-devs introduction (leftover official $LEARN_WEB3_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_WEB3_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_WEB3_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX chromia-comparisons cosmos (leftover official $LEARN_COMPARISONS_COSMOS_INDEX_URL leftover official 301 leftover official $LEARN_COMPARISONS_COSMOS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_COMPARISONS_COSMOS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/tests INDEX (leftover official $RELL_TESTS_INDEX_URL leftover official 307 leftover official $RELL_TESTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_TESTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/MetaMask INDEX (leftover official $LEARN_TAGS_METAMASK_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_METAMASK_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_METAMASK_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("project_config", PROJECT_CONFIG_URL)
        put("tool", TOOL_NAME)
        put("anchors_yaml", anchorsYaml())
        put("include_source_yaml", includeSourceYaml())
        put("include_whole_file_yaml", includeWholeFileYaml())
        put("include_whole_file_result_yaml", includeWholeFileResultYaml())
        put("include_tag_yaml", includeTagYaml())
        put("include_tag_result_yaml", includeTagResultYaml())
        put("include_whole", "!include test.yml")
        put("include_tag", "!include test.yml#a")
        put("ecosystem_bridge_troubleshooting_index_url_slash", ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_URL_SLASH)
        put("ecosystem_bridge_troubleshooting_index_title", ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_TITLE)
        put("ecosystem_provider_keypair_index_url_slash", ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_URL_SLASH)
        put("ecosystem_provider_keypair_index_title", ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_TITLE)
        put("ecosystem_pmc_provider_index_url_slash", ECOSYSTEM_PMC_PROVIDER_INDEX_URL_SLASH)
        put("ecosystem_pmc_provider_index_title", ECOSYSTEM_PMC_PROVIDER_INDEX_TITLE)
        put("learn_ft4_asset_operations_index_url_slash", LEARN_FT4_ASSET_OPERATIONS_INDEX_URL_SLASH)
        put("learn_ft4_asset_operations_index_title", LEARN_FT4_ASSET_OPERATIONS_INDEX_TITLE)
        put("learn_icmf_manual_testing_index_url_slash", LEARN_ICMF_MANUAL_TESTING_INDEX_URL_SLASH)
        put("learn_icmf_manual_testing_index_title", LEARN_ICMF_MANUAL_TESTING_INDEX_TITLE)
        put("learn_news_what_next_index_url_slash", LEARN_NEWS_WHAT_NEXT_INDEX_URL_SLASH)
        put("learn_news_what_next_index_title", LEARN_NEWS_WHAT_NEXT_INDEX_TITLE)
        put("learn_install_postgres_index_url_slash", LEARN_INSTALL_POSTGRES_INDEX_URL_SLASH)
        put("learn_install_postgres_index_title", LEARN_INSTALL_POSTGRES_INDEX_TITLE)
        put("learn_news_authentication_index_url_slash", LEARN_NEWS_AUTHENTICATION_INDEX_URL_SLASH)
        put("learn_news_authentication_index_title", LEARN_NEWS_AUTHENTICATION_INDEX_TITLE)
        put("learn_zk_frontend_test_index_url_slash", LEARN_ZK_FRONTEND_TEST_INDEX_URL_SLASH)
        put("learn_zk_frontend_test_index_title", LEARN_ZK_FRONTEND_TEST_INDEX_TITLE)
        put("learn_web3_intro_index_url_slash", LEARN_WEB3_INTRO_INDEX_URL_SLASH)
        put("learn_web3_intro_index_title", LEARN_WEB3_INTRO_INDEX_TITLE)
        put("learn_comparisons_cosmos_index_url_slash", LEARN_COMPARISONS_COSMOS_INDEX_URL_SLASH)
        put("learn_comparisons_cosmos_index_title", LEARN_COMPARISONS_COSMOS_INDEX_TITLE)
        put("rell_tests_index_url_slash", RELL_TESTS_INDEX_URL_SLASH)
        put("rell_tests_index_title", RELL_TESTS_INDEX_TITLE)
        put("learn_tags_metamask_index_url_slash", LEARN_TAGS_METAMASK_INDEX_URL_SLASH)
        put("learn_tags_metamask_index_title", LEARN_TAGS_METAMASK_INDEX_TITLE)
        put("notes", notes())
    }
}

// Leftover official leftover ECOSYSTEM ecosystem/bridge/bridge-troubleshooting INDEX leftovers encoded as ECOSYSTEM_BRIDGE_TROUBLESHOOTING_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/provider-keypair INDEX leftovers encoded as ECOSYSTEM_PROVIDER_KEYPAIR_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/provider INDEX leftovers encoded as ECOSYSTEM_PMC_PROVIDER_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/ft4-asset/asset-operations INDEX leftovers encoded as LEARN_FT4_ASSET_OPERATIONS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/icmf-course/manual-testing INDEX leftovers encoded as LEARN_ICMF_MANUAL_TESTING_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/my-news-feed/what-next INDEX leftovers encoded as LEARN_NEWS_WHAT_NEXT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN docs/install/database-setup INDEX leftovers encoded as LEARN_INSTALL_POSTGRES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX news-feed authentication leftovers encoded as LEARN_NEWS_AUTHENTICATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX zero-knowledge-proof frontend-test leftovers encoded as LEARN_ZK_FRONTEND_TEST_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX web3-for-web2-devs introduction leftovers encoded as LEARN_WEB3_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX chromia-comparisons cosmos leftovers encoded as LEARN_COMPARISONS_COSMOS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/tests INDEX leftovers encoded as RELL_TESTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/MetaMask INDEX leftovers encoded as LEARN_TAGS_METAMASK_INDEX_* (query-only HELP ONLY WRITE SKIP).
