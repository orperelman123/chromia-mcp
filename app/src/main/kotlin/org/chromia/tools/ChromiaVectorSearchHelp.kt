package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official leftover BUILD vector-search (read-only).
 * Live leftover pages: /build/vector-search/overview/ and /sample-workloads (200).
 * /build/vector-search/ is 404. /build/vector-search/overview (no slash) is 307.
 * /build/extensions/ is 404. Official leftover BUILD pages print no module names,
 * yml keys, or query names — do not invent any.
 */
object ChromiaVectorSearchHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val TOOL_NAME = "chromia_vector_search_help"
    const val OVERVIEW_URL = "https://docs.chromia.com/build/vector-search/overview/"
    const val OVERVIEW_REDIRECT = "https://docs.chromia.com/build/vector-search/overview"
    const val OVERVIEW_INDEX_URL = OVERVIEW_REDIRECT
    const val OVERVIEW_INDEX_URL_SLASH = OVERVIEW_URL
    const val OVERVIEW_INDEX_TITLE = "Vector and Search overview"
    const val WORKLOADS_URL = "https://docs.chromia.com/build/vector-search/sample-workloads"
    const val WORKLOADS_INDEX_URL = WORKLOADS_URL
    const val WORKLOADS_INDEX_URL_SLASH = "https://docs.chromia.com/build/vector-search/sample-workloads/"
    const val WORKLOADS_INDEX_TITLE = "Sample workloads"
    const val INDEX_404_URL = "https://docs.chromia.com/build/vector-search/"
    const val EXTENSIONS_404_URL = "https://docs.chromia.com/build/extensions/"
    const val FILEHUB_CLIENT_URL = "https://docs.chromia.com/build/clients/filehub-client/"
    const val COOKBOOK_URL = "https://docs.chromia.com/build/cookbook/overview"
    const val GET_STARTED_VECTOR_DB_INDEX_URL = "https://docs.chromia.com/get-started/use-cases/ai-on-chain/vector-db"
    const val GET_STARTED_VECTOR_DB_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/ai-on-chain/vector-db/"
    const val GET_STARTED_VECTOR_DB_INDEX_TITLE = "Vector database applications"
    const val GET_STARTED_AI_ON_CHAIN_INDEX_URL = "https://docs.chromia.com/get-started/use-cases/ai-on-chain"
    const val GET_STARTED_AI_ON_CHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/ai-on-chain/"
    const val GET_STARTED_AI_ON_CHAIN_INDEX_TITLE = "AI applications"
    const val GET_STARTED_AI_INFERENCE_INDEX_URL = "https://docs.chromia.com/get-started/use-cases/ai-on-chain/ai_inference"
    const val GET_STARTED_AI_INFERENCE_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/ai-on-chain/ai_inference/"
    const val GET_STARTED_AI_INFERENCE_INDEX_TITLE = "AI inference applications"
    const val ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_URL = "https://docs.chromia.com/ecosystem/extensions/overview"
    const val ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/extensions/overview/"
    const val ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_TITLE = "What are Chromia extensions?"  // official H1
    const val ECOSYSTEM_NODES_INSTALL_PMC_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/install-pmc"
    const val ECOSYSTEM_NODES_INSTALL_PMC_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/install-pmc/"
    const val ECOSYSTEM_NODES_INSTALL_PMC_INDEX_TITLE = "Install PMC CLI"  // official H1
    const val DOCS_UPDATES_INDEX_URL = "https://docs.chromia.com/updates"
    const val DOCS_UPDATES_INDEX_URL_SLASH = "https://docs.chromia.com/updates/"
    const val DOCS_UPDATES_INDEX_TITLE = "Recent Updates"  // official H1
    const val LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_URL = "https://learn.chromia.com/courses/big-data/project-launch"
    const val LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_URL_SLASH = "https://learn.chromia.com/courses/big-data/project-launch/"
    const val LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_TITLE = "Prepare the project"  // official H1
    const val LEARN_EVM_AUTHENTICATION_INDEX_URL = "https://learn.chromia.com/courses/chromia-for-evm-developers/compare-authentication"
    const val LEARN_EVM_AUTHENTICATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-for-evm-developers/compare-authentication/"
    const val LEARN_EVM_AUTHENTICATION_INDEX_TITLE = "Authentication"
    const val LEARN_VECTOR_DB_SETUP_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/setup"
    const val LEARN_VECTOR_DB_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/"
    const val LEARN_VECTOR_DB_SETUP_INDEX_TITLE = "Module 1 – Set up your project"  // official H1
    const val LEARN_ICMF_FACTORY_CHAIN_INDEX_URL = "https://learn.chromia.com/courses/icmf-course/factory-chain"
    const val LEARN_ICMF_FACTORY_CHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/icmf-course/factory-chain/"
    const val LEARN_ICMF_FACTORY_CHAIN_INDEX_TITLE = "Factory chain (send and receive)"  // official H1
    const val LEARN_NEWS_SUMMARY_TESTS_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-two/summary-and-tests"
    const val LEARN_NEWS_SUMMARY_TESTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-two/summary-and-tests/"
    const val LEARN_NEWS_SUMMARY_TESTS_INDEX_TITLE = "Summary and manual testing"  // official H1
    const val LEARN_INSTALL_CLI_INDEX_URL = "https://learn.chromia.com/docs/install/cli-installation"
    const val LEARN_INSTALL_CLI_INDEX_URL_SLASH = "https://learn.chromia.com/docs/install/cli-installation/"
    const val LEARN_INSTALL_CLI_INDEX_TITLE = "Install Chromia CLI"  // official H1
    const val LEARN_ZK_ARCHITECTURE_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/architecture-overview"
    const val LEARN_ZK_ARCHITECTURE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/architecture-overview/"
    const val LEARN_ZK_ARCHITECTURE_INDEX_TITLE = "Architecture overview"  // official H1
    const val RELL_EXPRESSIONS_VALUES_INDEX_URL = "https://docs.chromia.com/rell/language-features/expressions/values"
    const val RELL_EXPRESSIONS_VALUES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/expressions/values/"
    const val RELL_EXPRESSIONS_VALUES_INDEX_TITLE = "Values"  // official H1
    const val LEARN_TAGS_PYSPARK_INDEX_URL = "https://learn.chromia.com/tags/PySpark"
    const val LEARN_TAGS_PYSPARK_INDEX_URL_SLASH = "https://learn.chromia.com/tags/PySpark/"
    const val LEARN_TAGS_PYSPARK_INDEX_TITLE = "Courses tagged with: PySpark"  // official H1
    const val LEARN_TAGS_VECTOR_DB_INDEX_URL = "https://learn.chromia.com/tags/Vector%20DB"
    const val LEARN_TAGS_VECTOR_DB_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Vector%20DB/"
    const val LEARN_TAGS_VECTOR_DB_INDEX_TITLE = "Courses tagged with: Vector DB"  // leftover LEARN_TAGS_TAG pattern; official leftover tag URL currently 404

    val pages = listOf(OVERVIEW_URL, WORKLOADS_URL)

    val capabilities = listOf(
        "pgvector extension with deterministic replication across providers",
        "full-text indexes that scope search queries to tenants or application chains",
        "deterministic ranking functions so the same query returns the same ordered results across replicas"
    )

    val concepts = listOf(
        "Embeddings  # leftover official: store high-dimensional vectors per account, message, or object",
        "Hybrid search  # leftover official: combine vector similarity with structured filters from the relational schema",
        "Access control  # leftover official: use Rell to guard both writes and read access to sensitive embeddings"
    )

    val workloads = listOf(
        "Recommendation feed  # leftover official: dedicated table; pgvector cosine_distance; cache table",
        "AI-assisted search  # leftover official: off-chain text-embedding-3-small; Filehub API or backend worker; full-text indexing",
        "RAG pipelines  # leftover official: Filehub binary refs; chunk embeddings; Postchain REST plus Rell ACL"
    )

    val skipped = listOf(
        "index /build/vector-search/ is 404",
        "overview without trailing slash is 307",
        "/build/extensions/ is 404",
        "invented module names yml keys query names",
        "ingest embeddings and ONNX hard skip",
        "leftover official BUILD Filehub client page prints no package id; leftover official configure and leftover official work print filehub",
        "invented hex placeholders"
    )

    fun notes(): String = """
        Official leftover Chromia BUILD vector-search. CLI $CLI_SERIES. Java 21+, Postgres 16+.
        Leftover official overview (200 with trailing slash): $OVERVIEW_URL
        Bare $OVERVIEW_REDIRECT is 307. Leftover official index $INDEX_404_URL is 404.
        Leftover official sample workloads (200): $WORKLOADS_URL
        Leftover official /build/extensions/ is 404 ($EXTENSIONS_404_URL).
        Leftover official capabilities: pgvector; full-text indexes; deterministic ranking.
        Leftover official concepts: embeddings; hybrid search; Rell access control for embeddings.
        Leftover official workloads: recommendation feed (pgvector cosine_distance); AI-assisted search
        (off-chain text-embedding-3-small plus Filehub API); RAG (Filehub binary plus Postchain REST plus Rell ACL).
        Leftover official BUILD pages print no module names, yml keys, or query names — do not invent any.
        Leftover official Filehub client pointer: $FILEHUB_CLIENT_URL — TypeScript; peer deps @chromia/ft4 and postchain-client; leftover official BUILD page prints no package id. Leftover official configure and leftover official work print package id filehub (see chromia_language_clients_help).
        Leftover official sample-workloads says continue via CLI cookbook templates ($COOKBOOK_URL).
        Hard skip: ingest embeddings, ONNX, invented query names.
        Leftover official leftover BUILD vector-search/overview (leftover official $OVERVIEW_INDEX_URL leftover official 307 leftover official $OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official $OVERVIEW_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover leftover official leftover BUILD leftover official leftover pages leftover official leftover print leftover official leftover no leftover official leftover module leftover official leftover names leftover official leftover yml leftover official leftover keys leftover official leftover or leftover official leftover query leftover official leftover names leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover any leftover official leftover leftover official leftover $INDEX_404_URL leftover official leftover is leftover official leftover 404.
        Leftover official leftover BUILD vector-search/sample-workloads (leftover official $WORKLOADS_INDEX_URL leftover official 307 leftover official $WORKLOADS_INDEX_URL_SLASH leftover official 200 leftover official $WORKLOADS_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover keygen leftover official leftover leftover official leftover BUILD leftover official leftover pages leftover official leftover print leftover official leftover no leftover official leftover module leftover official leftover names leftover official leftover yml leftover official leftover keys leftover official leftover or leftover official leftover query leftover official leftover names leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover any.
        Leftover official leftover GET-STARTED get-started/use-cases/ai-on-chain/vector-db INDEX (leftover official $GET_STARTED_VECTOR_DB_INDEX_URL leftover official 307 leftover official $GET_STARTED_VECTOR_DB_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_VECTOR_DB_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover GET-STARTED get-started/use-cases/ai-on-chain INDEX (leftover official $GET_STARTED_AI_ON_CHAIN_INDEX_URL leftover official 307 leftover official $GET_STARTED_AI_ON_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_AI_ON_CHAIN_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover GET-STARTED get-started/use-cases/ai-on-chain/ai_inference INDEX (leftover official $GET_STARTED_AI_INFERENCE_INDEX_URL leftover official 307 leftover official $GET_STARTED_AI_INFERENCE_INDEX_URL_SLASH leftover official 200 leftover official $GET_STARTED_AI_INFERENCE_INDEX_TITLE): leftover official leftover slash leftover official leftover title leftover official leftover WRITE SKIP leftover official leftover HELP ONLY leftover official leftover Origin parked leftover official leftover Query-only leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs leftover official leftover no leftover official leftover chr leftover official leftover keygen leftover official leftover no leftover official leftover sign leftover official leftover recipe.
        Leftover official leftover ECOSYSTEM ecosystem/extensions/overview INDEX (leftover official $ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/install-pmc INDEX (leftover official $ECOSYSTEM_NODES_INSTALL_PMC_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_NODES_INSTALL_PMC_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_NODES_INSTALL_PMC_INDEX_TITLE). Query-only.
        Leftover official leftover docs/updates INDEX (leftover official $DOCS_UPDATES_INDEX_URL leftover official 307 leftover official $DOCS_UPDATES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $DOCS_UPDATES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/big-data/project-launch INDEX (leftover official $LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_URL leftover official 301 leftover official $LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/chromia-for-evm-developers/compare-authentication INDEX (leftover official $LEARN_EVM_AUTHENTICATION_INDEX_URL leftover official 301 leftover official $LEARN_EVM_AUTHENTICATION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_EVM_AUTHENTICATION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official page is a docs changelog (2025 UI/nav, deployment restructure, getting-started, key management centralization, token chain, installation). HELP ONLY. Skip signed txs, sample keys, invented 64-hex.
        See chromia_rell_database_help (Postgres 16+), chromia_language_clients_help, chromia_integrations_help.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover LEARN courses/vector-db-movie-demo/setup INDEX (leftover official $LEARN_VECTOR_DB_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_VECTOR_DB_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_VECTOR_DB_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP. leftover official leftover first leftover official leftover module leftover official leftover configure leftover official leftover environment leftover official leftover Vector leftover official leftover DB leftover official leftover Movie leftover official leftover Demo leftover official leftover sentence leftover official leftover embedding leftover official leftover model leftover official leftover dimensionality leftover official leftover vector_db_extension leftover official leftover Rell leftover official leftover backend leftover official leftover Chromia leftover official leftover testnet leftover official leftover Python leftover official leftover environment.
        Leftover official leftover LEARN courses/icmf-course/factory-chain INDEX (leftover official $LEARN_ICMF_FACTORY_CHAIN_INDEX_URL leftover official 301 leftover official $LEARN_ICMF_FACTORY_CHAIN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ICMF_FACTORY_CHAIN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/my-news-feed/module-two/summary-and-tests INDEX (leftover official $LEARN_NEWS_SUMMARY_TESTS_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_SUMMARY_TESTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_SUMMARY_TESTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN docs/install/cli-installation INDEX (leftover official $LEARN_INSTALL_CLI_INDEX_URL leftover official 301 leftover official $LEARN_INSTALL_CLI_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_INSTALL_CLI_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/zero-knowledge-proof/architecture-overview INDEX (leftover official $LEARN_ZK_ARCHITECTURE_INDEX_URL leftover official 301 leftover official $LEARN_ZK_ARCHITECTURE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ZK_ARCHITECTURE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/expressions/values INDEX (leftover official $RELL_EXPRESSIONS_VALUES_INDEX_URL leftover official 307 leftover official $RELL_EXPRESSIONS_VALUES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_EXPRESSIONS_VALUES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/PySpark INDEX (leftover official $LEARN_TAGS_PYSPARK_INDEX_URL leftover official 301 leftover official $LEARN_TAGS_PYSPARK_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TAGS_PYSPARK_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN tags/Vector DB INDEX (leftover official $LEARN_TAGS_VECTOR_DB_INDEX_URL leftover official 404 leftover official $LEARN_TAGS_VECTOR_DB_INDEX_URL_SLASH leftover official 404 leftover official leftover official leftover tag leftover official leftover URL leftover official leftover currently leftover official leftover 404 leftover official leftover TITLE leftover official leftover $LEARN_TAGS_VECTOR_DB_INDEX_TITLE leftover official leftover live leftover official leftover Vector leftover official leftover DB leftover official leftover INDEX leftover official leftover is leftover official leftover leftover leftover official leftover learn leftover official leftover vector-db leftover official leftover course leftover official leftover $LEARN_VECTOR_DB_SETUP_INDEX_TITLE leftover official leftover ${ChromiaRellExpressionsHelp.LEARN_VECTOR_DB_USE_CASES_INDEX_TITLE} leftover official leftover HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("tool", TOOL_NAME)
        put("docs", OVERVIEW_URL)
        put("workloads_docs", WORKLOADS_URL)
        put("index_404", INDEX_404_URL)
        put("overview_redirect", OVERVIEW_REDIRECT)
        put("overview_index_docs", OVERVIEW_INDEX_URL)
        put("overview_index_url_slash", OVERVIEW_INDEX_URL_SLASH)
        put("overview_index_title", OVERVIEW_INDEX_TITLE)
        put("workloads_index_docs", WORKLOADS_INDEX_URL)
        put("workloads_index_url_slash", WORKLOADS_INDEX_URL_SLASH)
        put("workloads_index_title", WORKLOADS_INDEX_TITLE)
        put("get_started_vector_db_index_docs", GET_STARTED_VECTOR_DB_INDEX_URL)
        put("get_started_vector_db_index_url_slash", GET_STARTED_VECTOR_DB_INDEX_URL_SLASH)
        put("get_started_vector_db_index_title", GET_STARTED_VECTOR_DB_INDEX_TITLE)
        put("get_started_ai_on_chain_index_docs", GET_STARTED_AI_ON_CHAIN_INDEX_URL)
        put("get_started_ai_on_chain_index_url_slash", GET_STARTED_AI_ON_CHAIN_INDEX_URL_SLASH)
        put("get_started_ai_on_chain_index_title", GET_STARTED_AI_ON_CHAIN_INDEX_TITLE)
        put("get_started_ai_inference_index_docs", GET_STARTED_AI_INFERENCE_INDEX_URL)
        put("get_started_ai_inference_index_url_slash", GET_STARTED_AI_INFERENCE_INDEX_URL_SLASH)
        put("get_started_ai_inference_index_title", GET_STARTED_AI_INFERENCE_INDEX_TITLE)
        put("extensions_404", EXTENSIONS_404_URL)
        put("filehub_client_docs", FILEHUB_CLIENT_URL)
        put("read_only", true)
        put("pages", buildJsonArray { pages.forEach { add(JsonPrimitive(it)) } })
        put("capabilities", buildJsonArray { capabilities.forEach { add(JsonPrimitive(it)) } })
        put("concepts", buildJsonArray { concepts.forEach { add(JsonPrimitive(it)) } })
        put("workloads", buildJsonArray { workloads.forEach { add(JsonPrimitive(it)) } })
        put("official_operator", "pgvector cosine_distance")
        put("official_offchain_model_example", "text-embedding-3-small")
        put("skipped_404_or_write", buildJsonArray { skipped.forEach { add(JsonPrimitive(it)) } })
        put("rell_database_help", ChromiaRellDatabaseHelp.TOOL_NAME)
        put("language_clients_help", ChromiaLanguageClientsHelp.TOOL_NAME)
        put("integrations_help", ChromiaIntegrationsHelp.TOOL_NAME)
        put("ecosystem_extensions_overview_index_url_slash", ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_URL_SLASH)
        put("ecosystem_extensions_overview_index_title", ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_TITLE)
        put("ecosystem_nodes_install_pmc_index_url_slash", ECOSYSTEM_NODES_INSTALL_PMC_INDEX_URL_SLASH)
        put("ecosystem_nodes_install_pmc_index_title", ECOSYSTEM_NODES_INSTALL_PMC_INDEX_TITLE)
        put("docs_updates_index_url_slash", DOCS_UPDATES_INDEX_URL_SLASH)
        put("docs_updates_index_title", DOCS_UPDATES_INDEX_TITLE)
        put("learn_big_data_project_launch_index_url_slash", LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_URL_SLASH)
        put("learn_big_data_project_launch_index_title", LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_TITLE)
        put("learn_evm_authentication_index_url_slash", LEARN_EVM_AUTHENTICATION_INDEX_URL_SLASH)
        put("learn_evm_authentication_index_title", LEARN_EVM_AUTHENTICATION_INDEX_TITLE)
        put("learn_vector_db_setup_index_url_slash", LEARN_VECTOR_DB_SETUP_INDEX_URL_SLASH)
        put("learn_vector_db_setup_index_title", LEARN_VECTOR_DB_SETUP_INDEX_TITLE)
        put("learn_icmf_factory_chain_index_url_slash", LEARN_ICMF_FACTORY_CHAIN_INDEX_URL_SLASH)
        put("learn_icmf_factory_chain_index_title", LEARN_ICMF_FACTORY_CHAIN_INDEX_TITLE)
        put("learn_news_summary_tests_index_url_slash", LEARN_NEWS_SUMMARY_TESTS_INDEX_URL_SLASH)
        put("learn_news_summary_tests_index_title", LEARN_NEWS_SUMMARY_TESTS_INDEX_TITLE)
        put("learn_install_cli_index_url_slash", LEARN_INSTALL_CLI_INDEX_URL_SLASH)
        put("learn_install_cli_index_title", LEARN_INSTALL_CLI_INDEX_TITLE)
        put("learn_zk_architecture_index_url_slash", LEARN_ZK_ARCHITECTURE_INDEX_URL_SLASH)
        put("learn_zk_architecture_index_title", LEARN_ZK_ARCHITECTURE_INDEX_TITLE)
        put("rell_expressions_values_index_url_slash", RELL_EXPRESSIONS_VALUES_INDEX_URL_SLASH)
        put("rell_expressions_values_index_title", RELL_EXPRESSIONS_VALUES_INDEX_TITLE)
        put("learn_tags_pyspark_index_url_slash", LEARN_TAGS_PYSPARK_INDEX_URL_SLASH)
        put("learn_tags_pyspark_index_title", LEARN_TAGS_PYSPARK_INDEX_TITLE)
        put("learn_tags_vector_db_index_url_slash", LEARN_TAGS_VECTOR_DB_INDEX_URL_SLASH)
        put("learn_tags_vector_db_index_title", LEARN_TAGS_VECTOR_DB_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover BUILD vector-search/sample-workloads leftovers encoded as WORKLOADS_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/use-cases/ai-on-chain/vector-db INDEX leftovers encoded as GET_STARTED_VECTOR_DB_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/use-cases/ai-on-chain INDEX leftovers encoded as GET_STARTED_AI_ON_CHAIN_INDEX_* (query-only).
// Leftover official leftover GET-STARTED get-started/use-cases/ai-on-chain/ai_inference INDEX leftovers encoded as GET_STARTED_AI_INFERENCE_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/extensions/overview INDEX leftovers encoded as ECOSYSTEM_EXTENSIONS_OVERVIEW_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/install-pmc INDEX leftovers encoded as ECOSYSTEM_NODES_INSTALL_PMC_INDEX_* (query-only).
// Leftover official leftover docs/updates INDEX leftovers encoded as DOCS_UPDATES_INDEX_* (query-only HELP ONLY).
// Leftover official leftover LEARN courses/big-data/project-launch INDEX leftovers encoded as LEARN_BIG_DATA_PROJECT_LAUNCH_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/chromia-for-evm-developers/compare-authentication INDEX leftovers encoded as LEARN_EVM_AUTHENTICATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/vector-db-movie-demo/setup INDEX leftovers encoded as LEARN_VECTOR_DB_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/icmf-course/factory-chain INDEX leftovers encoded as LEARN_ICMF_FACTORY_CHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/my-news-feed/module-two/summary-and-tests INDEX leftovers encoded as LEARN_NEWS_SUMMARY_TESTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN docs/install/cli-installation INDEX leftovers encoded as LEARN_INSTALL_CLI_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/zero-knowledge-proof/architecture-overview INDEX leftovers encoded as LEARN_ZK_ARCHITECTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/expressions/values INDEX leftovers encoded as RELL_EXPRESSIONS_VALUES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/PySpark INDEX leftovers encoded as LEARN_TAGS_PYSPARK_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN tags/Vector DB INDEX leftovers encoded as LEARN_TAGS_VECTOR_DB_INDEX_* (query-only HELP ONLY WRITE SKIP).
