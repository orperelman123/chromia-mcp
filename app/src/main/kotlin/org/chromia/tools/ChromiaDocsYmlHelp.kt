package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official chromia.yml `docs:` section for `chr generate docs-site`.
 * Keys from docs.chromia.com/build/configuration/project-config only.
 * Does not invent theme / nav / logo. Does not run chr.
 * Leftover official leftover BUILD cli/generating-doc-site index slash/title leftovers live here (query-only).
 */
object ChromiaDocsYmlHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val PROJECT_CONFIG_URL = WriteDeploymentConfig.PROJECT_CONFIG_URL
    const val GENERATE_URL = "https://docs.chromia.com/build/cli/commands/generate"
    const val DOCS_SITE_URL = "https://docs.chromia.com/build/cli/generating-doc-site"
    const val DOCS_SITE_INDEX_URL = DOCS_SITE_URL
    const val DOCS_SITE_INDEX_URL_SLASH = "https://docs.chromia.com/build/cli/generating-doc-site/"
    const val DOCS_SITE_INDEX_TITLE = "Generate documentation"
    const val RELLDOC_URL = "https://docs.chromia.com/rell/rell-doc"
    const val ECOSYSTEM_BRIDGE_CLIENT_EXAMPLE_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-client/example"
    const val ECOSYSTEM_BRIDGE_CLIENT_EXAMPLE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/bridge-client/example/"
    const val ECOSYSTEM_BRIDGE_CLIENT_EXAMPLE_INDEX_TITLE = "Example usage: Bridge from EVM to Chromia and vice versa"  // official H1
    const val ECOSYSTEM_MANAGE_PROVIDER_KEYS_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/manage-provider-keys"
    const val ECOSYSTEM_MANAGE_PROVIDER_KEYS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/manage-provider-keys/"
    const val ECOSYSTEM_MANAGE_PROVIDER_KEYS_INDEX_TITLE = "Provider key management"  // official H1
    const val ECOSYSTEM_PMC_PROPOSAL_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/proposal"
    const val ECOSYSTEM_PMC_PROPOSAL_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/proposal/"
    const val ECOSYSTEM_PMC_PROPOSAL_INDEX_TITLE = "proposal"  // official H1
    const val ECOSYSTEM_GOV_STARTER_KIT_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit"
    const val ECOSYSTEM_GOV_STARTER_KIT_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/"
    const val ECOSYSTEM_GOV_STARTER_KIT_INDEX_TITLE = "The Governance Tool Starter Kit"  // official H1
    const val LEARN_FT4_DEMO_MODULE_INIT_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-init"
    const val LEARN_FT4_DEMO_MODULE_INIT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-init/"
    const val LEARN_FT4_DEMO_MODULE_INIT_INDEX_TITLE = "Module 1 - Init Fullstack application"  // official H1
    const val LEARN_ICCF_MANUAL_TESTING_INDEX_URL = "https://learn.chromia.com/courses/iccf-course/manual-testing"
    const val LEARN_ICCF_MANUAL_TESTING_INDEX_URL_SLASH = "https://learn.chromia.com/courses/iccf-course/manual-testing/"
    const val LEARN_ICCF_MANUAL_TESTING_INDEX_TITLE = "Testing the dapp"  // official H1
    const val LEARN_NEWS_BASIC_QUERIES_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries/write-queries"
    const val LEARN_NEWS_BASIC_QUERIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries/write-queries/"
    const val LEARN_NEWS_BASIC_QUERIES_INDEX_TITLE = "Basic queries"  // official H1
    const val LEARN_TTT_INCORPORATE_MODULES_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one/project-structure/incorporate-modules"
    const val LEARN_TTT_INCORPORATE_MODULES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/project-structure/incorporate-modules/"
    const val LEARN_TTT_INCORPORATE_MODULES_INDEX_TITLE = "Incorporate modules in the dapp"  // official H1
    const val LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/deploy-rell-module"
    const val LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/deploy-rell-module/"
    const val LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_TITLE = "Deploy your Rell module"  // official H1
    const val LEARN_MONETIZE_OPEN_INDEX_URL = "https://learn.chromia.com/courses/monetize-dapp/open"
    const val LEARN_MONETIZE_OPEN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/monetize-dapp/open/"
    const val LEARN_MONETIZE_OPEN_INDEX_TITLE = "Open strategy"  // official H1
    const val LEARN_ZK_SETUP_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/setup"
    const val LEARN_ZK_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/setup/"
    const val LEARN_ZK_SETUP_INDEX_TITLE = "Project setup"  // official H1
    const val LEARN_RELATIONSHIPS_SETUP_INDEX_URL = "https://learn.chromia.com/courses/relationships-course/setup"
    const val LEARN_RELATIONSHIPS_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/relationships-course/setup/"
    const val LEARN_RELATIONSHIPS_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_WEB3_REVENUES_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/revenues-and-op-costs"
    const val LEARN_WEB3_REVENUES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/revenues-and-op-costs/"
    const val LEARN_WEB3_REVENUES_INDEX_TITLE = "Revenue models and operational costs"  // official H1
    const val LEARN_TAGS_MULTICHAIN_INDEX_URL = "https://learn.chromia.com/tags/Multichain"
    const val LEARN_TAGS_MULTICHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Multichain/"
    const val LEARN_TAGS_MULTICHAIN_INDEX_TITLE = "Courses tagged with: Multichain"  // official H1
    const val TOOL_NAME = "chromia_docs_yml_help"

    val keys = listOf(
        "title",
        "footerMessage",
        "customStyleSheets",
        "customAssets",
        "additionalContent",
        "sourceLink.remoteUrl",
        "sourceLink.remoteLineSuffix"
    )

    val notOfficialKeys = listOf("theme", "nav", "logo")
    val leftoverOfficialDocsKeysNotInProjectConfig = listOf("additionalModules")

    val lineSuffixes = linkedMapOf(
        "github" to "#L",
        "gitlab" to "#L",
        "bitbucket" to "#lines-"
    )

    fun docsYaml(): String = """
        docs:
          title: My Dapp
          footerMessage: Copyright example
          customStyleSheets:
            - styles/custom.css
          customAssets:
            - images/logo.png
          additionalContent:
            - my-doc.md
          sourceLink:
            remoteUrl: https://github.com/my-repo/blob/main/src/
            remoteLineSuffix: "#L"
    """.trimIndent() + "\n"

    fun additionalContentNote(): String = """
        additionalContent markdown: `# Dapp <title>` must match docs.title.
        `# Module <module-name>` sections populate that Rell module page. Sub-titles are allowed.
        customAssets files are copied into the generated site images/ folder.
    """.trimIndent()

    fun notes(): String = """
        Official chromia.yml `docs:` section for Chromia CLI $CLI_SERIES `chr generate docs-site`.
        Schema: $PROJECT_CONFIG_URL (source of truth for keys).
        Generate command: $GENERATE_URL. Narrative page: $DOCS_SITE_URL.
        Official keys only: ${keys.joinToString(", ")}.
        GitHub / GitLab `sourceLink.remoteLineSuffix` is "#L"; Bitbucket is "#lines-".
        ${additionalContentNote()}
        Do not invent theme, nav, or logo keys — they are not on the official schema.
        Leftover official generating-doc-site (200) prints additionalModules; leftover official project-config (200) does not list it. Record the discrepancy. Do not emit additionalModules in the pasteable docs yaml. Do not invent theme, nav, or logo.
        `chr generate docs-site -i, --include=<text>` is the official CLI flag for including a lib identifier (e.g. lib.foo) in navigation.
        `--hide-lib-warnings` is official on `chr generate docs-site`.
        Use $TOOL_NAME for the pasteable `docs:` block. chr_generate_client_help points here.
        Official leftover /rell/rell-doc ($RELLDOC_URL): RellDoc comments (`/** … */` with @param / @return /
        @throws / @see / @since / @author) are processed by documentation generation. Syntax: chromia_rell_language_help.
        Leftover official BUILD cli/generating-doc-site (leftover official $DOCS_SITE_INDEX_URL leftover official 307 leftover official $DOCS_SITE_INDEX_URL_SLASH leftover official 200 leftover official $DOCS_SITE_INDEX_TITLE): leftover official leftover intro leftover official leftover This topic explains how to generate a documentation site from your Rell source code and how to customize it to match your project's branding and requirements leftover official leftover Overview leftover official leftover The Chromia CLI provides a built-in command to generate a documentation site from your Rell source code leftover official leftover API documentation automatically generated leftover official leftover RellDoc leftover official leftover Custom content pages leftover official leftover Customizable styling and branding leftover official leftover Generate a documentation site leftover official leftover chr generate docs-site leftover official leftover build/docs-site/index.html leftover official leftover navigation bar leftover official leftover web server leftover official leftover Configure document generation leftover official leftover docs leftover official leftover chromia.yml leftover official leftover title leftover official leftover footerMessage leftover official leftover customAssets leftover official leftover customStyleSheets leftover official leftover additionalContent leftover official leftover additionalModules leftover official leftover leftover official leftover generating-doc-site leftover official leftover prints leftover official leftover additionalModules leftover official leftover leftover official leftover project-config leftover official leftover does leftover official leftover not leftover official leftover list leftover official leftover it leftover official leftover Add additional content leftover official leftover Markdown leftover official leftover # Dapp <title> leftover official leftover # Module leftover official leftover Sub-titles leftover official leftover Customize styling leftover official leftover customStyleSheets leftover official leftover Add custom assets leftover official leftover customAssets leftover official leftover Add additional modules leftover official leftover test.doc_module leftover official leftover Troubleshooting leftover official leftover Navigation bar not showing leftover official leftover Docker leftover official leftover httpd leftover official leftover Node.js leftover official leftover http-server leftover official leftover Custom styling not applied leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover sample leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover pubkey leftover official leftover keygen leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex.
        Leftover official leftover ECOSYSTEM ecosystem/bridge/bridge-client/example INDEX (leftover official $ECOSYSTEM_BRIDGE_CLIENT_EXAMPLE_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_BRIDGE_CLIENT_EXAMPLE_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_BRIDGE_CLIENT_EXAMPLE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/manage-provider-keys INDEX (leftover official $ECOSYSTEM_MANAGE_PROVIDER_KEYS_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_MANAGE_PROVIDER_KEYS_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_MANAGE_PROVIDER_KEYS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/proposal INDEX (leftover official $ECOSYSTEM_PMC_PROPOSAL_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_PROPOSAL_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_PROPOSAL_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit INDEX (leftover official $ECOSYSTEM_GOV_STARTER_KIT_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_STARTER_KIT_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_GOV_STARTER_KIT_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/ft4-demo-app/module-init INDEX (leftover official $LEARN_FT4_DEMO_MODULE_INIT_INDEX_URL leftover official 301 leftover official $LEARN_FT4_DEMO_MODULE_INIT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_DEMO_MODULE_INIT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). leftover official leftover This course will guide you to setup blockchain environment and create a rell dapp template leftover official leftover Create a project based on the asset-managent template. Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/iccf-course/manual-testing INDEX (leftover official $LEARN_ICCF_MANUAL_TESTING_INDEX_URL leftover official 301 leftover official $LEARN_ICCF_MANUAL_TESTING_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICCF_MANUAL_TESTING_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/my-news-feed/module-one/operations-queries/write-queries INDEX (leftover official $LEARN_NEWS_BASIC_QUERIES_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_BASIC_QUERIES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_BASIC_QUERIES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/tic-tac-toe/module-one/project-structure/incorporate-modules INDEX (leftover official $LEARN_TTT_INCORPORATE_MODULES_INDEX_URL leftover official 301 leftover official $LEARN_TTT_INCORPORATE_MODULES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_INCORPORATE_MODULES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/vector-db-movie-demo/setup/deploy-rell-module INDEX (leftover official $LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_URL leftover official 301 leftover official $LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/monetize-dapp/open INDEX (leftover official $LEARN_MONETIZE_OPEN_INDEX_URL leftover official 301 leftover official $LEARN_MONETIZE_OPEN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MONETIZE_OPEN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/zero-knowledge-proof/setup INDEX (leftover official $LEARN_ZK_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_ZK_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ZK_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX relationships-course setup (leftover official $LEARN_RELATIONSHIPS_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_RELATIONSHIPS_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_RELATIONSHIPS_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX web3-for-web2-devs revenues-and-op-costs (leftover official $LEARN_WEB3_REVENUES_INDEX_URL leftover official 301 leftover official $LEARN_WEB3_REVENUES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_WEB3_REVENUES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover LEARN tags/Multichain INDEX (leftover official $LEARN_TAGS_MULTICHAIN_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_MULTICHAIN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_MULTICHAIN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("project_config", PROJECT_CONFIG_URL)
        put("docs", GENERATE_URL)
        put("docs_site", DOCS_SITE_URL)
        put("docs_site_index_docs", DOCS_SITE_INDEX_URL)
        put("docs_site_index_url_slash", DOCS_SITE_INDEX_URL_SLASH)
        put("docs_site_index_title", DOCS_SITE_INDEX_TITLE)
        put("relldoc_docs", RELLDOC_URL)
        put("command", "chr generate docs-site")
        put("tool", TOOL_NAME)
        put(
            "keys",
            buildJsonArray { keys.forEach { add(JsonPrimitive(it)) } }
        )
        put(
            "not_official_keys",
            buildJsonArray { (notOfficialKeys + leftoverOfficialDocsKeysNotInProjectConfig).forEach { add(JsonPrimitive(it)) } }
        )
        put(
            "leftover_official_docs_keys_not_in_project_config",
            buildJsonArray { leftoverOfficialDocsKeysNotInProjectConfig.forEach { add(JsonPrimitive(it)) } }
        )
        put("additional_modules_discrepancy", "leftover official generating-doc-site (200) prints additionalModules; leftover official project-config (200) does not list it")
        put(
            "line_suffixes",
            buildJsonObject {
                lineSuffixes.forEach { (host, suffix) -> put(host, suffix) }
            }
        )
        put("docs_yaml", docsYaml())
        put("additional_content", additionalContentNote())
        put("ecosystem_bridge_client_example_index_url_slash", ECOSYSTEM_BRIDGE_CLIENT_EXAMPLE_INDEX_URL_SLASH)
        put("ecosystem_bridge_client_example_index_title", ECOSYSTEM_BRIDGE_CLIENT_EXAMPLE_INDEX_TITLE)
        put("ecosystem_manage_provider_keys_index_url_slash", ECOSYSTEM_MANAGE_PROVIDER_KEYS_INDEX_URL_SLASH)
        put("ecosystem_manage_provider_keys_index_title", ECOSYSTEM_MANAGE_PROVIDER_KEYS_INDEX_TITLE)
        put("ecosystem_pmc_proposal_index_url_slash", ECOSYSTEM_PMC_PROPOSAL_INDEX_URL_SLASH)
        put("ecosystem_pmc_proposal_index_title", ECOSYSTEM_PMC_PROPOSAL_INDEX_TITLE)
        put("ecosystem_gov_starter_kit_index_url_slash", ECOSYSTEM_GOV_STARTER_KIT_INDEX_URL_SLASH)
        put("ecosystem_gov_starter_kit_index_title", ECOSYSTEM_GOV_STARTER_KIT_INDEX_TITLE)
        put("learn_ft4_demo_module_init_index_url_slash", LEARN_FT4_DEMO_MODULE_INIT_INDEX_URL_SLASH)
        put("learn_ft4_demo_module_init_index_title", LEARN_FT4_DEMO_MODULE_INIT_INDEX_TITLE)
        put("learn_iccf_manual_testing_index_url_slash", LEARN_ICCF_MANUAL_TESTING_INDEX_URL_SLASH)
        put("learn_iccf_manual_testing_index_title", LEARN_ICCF_MANUAL_TESTING_INDEX_TITLE)
        put("learn_news_basic_queries_index_url_slash", LEARN_NEWS_BASIC_QUERIES_INDEX_URL_SLASH)
        put("learn_news_basic_queries_index_title", LEARN_NEWS_BASIC_QUERIES_INDEX_TITLE)
        put("learn_ttt_incorporate_modules_index_url_slash", LEARN_TTT_INCORPORATE_MODULES_INDEX_URL_SLASH)
        put("learn_ttt_incorporate_modules_index_title", LEARN_TTT_INCORPORATE_MODULES_INDEX_TITLE)
        put("learn_vector_db_deploy_rell_index_url_slash", LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_URL_SLASH)
        put("learn_vector_db_deploy_rell_index_title", LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_TITLE)
        put("learn_monetize_open_index_url_slash", LEARN_MONETIZE_OPEN_INDEX_URL_SLASH)
        put("learn_monetize_open_index_title", LEARN_MONETIZE_OPEN_INDEX_TITLE)
        put("learn_zk_setup_index_url_slash", LEARN_ZK_SETUP_INDEX_URL_SLASH)
        put("learn_zk_setup_index_title", LEARN_ZK_SETUP_INDEX_TITLE)
        put("learn_relationships_setup_index_url_slash", LEARN_RELATIONSHIPS_SETUP_INDEX_URL_SLASH)
        put("learn_relationships_setup_index_title", LEARN_RELATIONSHIPS_SETUP_INDEX_TITLE)
        put("learn_web3_revenues_index_url_slash", LEARN_WEB3_REVENUES_INDEX_URL_SLASH)
        put("learn_web3_revenues_index_title", LEARN_WEB3_REVENUES_INDEX_TITLE)
        put("learn_tags_multichain_index_url_slash", LEARN_TAGS_MULTICHAIN_INDEX_URL_SLASH)
        put("learn_tags_multichain_index_title", LEARN_TAGS_MULTICHAIN_INDEX_TITLE)
        put("notes", notes())
    }
}

// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/proposal INDEX leftovers encoded as ECOSYSTEM_PMC_PROPOSAL_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit INDEX leftovers encoded as ECOSYSTEM_GOV_STARTER_KIT_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/ft4-demo-app/module-init INDEX leftovers encoded as LEARN_FT4_DEMO_MODULE_INIT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/iccf-course/manual-testing INDEX leftovers encoded as LEARN_ICCF_MANUAL_TESTING_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/my-news-feed/module-one/operations-queries/write-queries INDEX leftovers encoded as LEARN_NEWS_BASIC_QUERIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-one/project-structure/incorporate-modules INDEX leftovers encoded as LEARN_TTT_INCORPORATE_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/vector-db-movie-demo/setup/deploy-rell-module INDEX leftovers encoded as LEARN_VECTOR_DB_DEPLOY_RELL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/monetize-dapp/open INDEX leftovers encoded as LEARN_MONETIZE_OPEN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/zero-knowledge-proof/setup INDEX leftovers encoded as LEARN_ZK_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX relationships-course setup leftovers encoded as LEARN_RELATIONSHIPS_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX web3-for-web2-devs revenues-and-op-costs leftovers encoded as LEARN_WEB3_REVENUES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/Multichain INDEX leftovers encoded as LEARN_TAGS_MULTICHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
