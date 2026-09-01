package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Rell BUILD practice pages: security + best-practices.
 * Quotes docs.chromia.com/rell/security and /rell/rell-best-practices only.
 * BUILD / read-only guidance. No exploit recipes, no signing, no key material.
 * Skips proposal vote/retract. Does not invent YAML keys or 64-hex.
 */
object ChromiaRellPracticesHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val RELL_VERSION = DappScaffold.RELL_SOURCE_TAG
    const val TOOL_NAME = "chromia_rell_practices_help"
    const val SECURITY_URL = "https://docs.chromia.com/rell/security"
    const val BEST_PRACTICES_URL = "https://docs.chromia.com/rell/rell-best-practices"
    const val ANALYZE_URL = "https://docs.chromia.com/rell/analyze-rell-dapp-code"
    const val RELLDOC_URL = "https://docs.chromia.com/rell/rell-doc"
    const val ECOSYSTEM_AI_INFERENCE_INDEX_URL = "https://docs.chromia.com/ecosystem/extensions/ai_inference"
    const val ECOSYSTEM_AI_INFERENCE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/extensions/ai_inference/"
    const val ECOSYSTEM_AI_INFERENCE_INDEX_TITLE = "AI Inference"  // official H1
    const val ECOSYSTEM_ADD_NODE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/add-node"
    const val ECOSYSTEM_ADD_NODE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/add-node/"
    const val ECOSYSTEM_ADD_NODE_INDEX_TITLE = "Add a node to the network"  // official H1
    const val ECOSYSTEM_PMC_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc"
    const val ECOSYSTEM_PMC_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/"
    const val ECOSYSTEM_PMC_INDEX_TITLE = "Postchain Management Console CLI"  // official H1
    const val ECOSYSTEM_PMC_SUBNODE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/subnode"
    const val ECOSYSTEM_PMC_SUBNODE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/subnode/"
    const val ECOSYSTEM_PMC_SUBNODE_INDEX_TITLE = "subnode-image"  // official H1
    const val RELL_DATABASE_DELETE_INDEX_URL = "https://docs.chromia.com/rell/language-features/database/delete"
    const val RELL_DATABASE_DELETE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/database/delete/"
    const val RELL_DATABASE_DELETE_INDEX_TITLE = "Delete statement"  // official H1
    const val LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-entity/tables"
    const val LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-entity/tables/"
    const val LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_TITLE = "Create your first entity"  // official H1
    const val LEARN_FT4_ASSET_TESTING_INDEX_URL = "https://learn.chromia.com/courses/ft4-asset/testing"
    const val LEARN_FT4_ASSET_TESTING_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-asset/testing/"
    const val LEARN_FT4_ASSET_TESTING_INDEX_TITLE = "Testing"  // official H1
    const val LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/tools"
    const val LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/tools/"
    const val LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_TITLE = "Lesson 2 - Chromia tools"  // official H1
    const val LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-assets/buy-mystery-card"
    const val LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-assets/buy-mystery-card/"
    const val LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_TITLE = "Add a fee for buying a mystery card"  // official H1
    const val LEARN_NEWS_SCAFFOLD_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-two/scaffold"
    const val LEARN_NEWS_SCAFFOLD_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-two/scaffold/"
    const val LEARN_NEWS_SCAFFOLD_INDEX_TITLE = "Project scaffold"  // official H1
    const val LEARN_TTT_SETUP_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/setup"
    const val LEARN_TTT_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/setup/"
    const val LEARN_TTT_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts"
    const val LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts/"
    const val LEARN_NEWS_CREATE_ACCOUNTS_INDEX_TITLE = "Lesson 2 - Create accounts"  // official H1
    const val LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-explore"
    const val LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-explore/"
    const val LEARN_ZK_FRONTEND_EXPLORE_INDEX_TITLE = "Frontend architecture"  // official H1
    const val LEARN_GOAT_CODEBASE_INDEX_URL = "https://learn.chromia.com/courses/chromia-goat-chat-agent/codebase-overview"
    const val LEARN_GOAT_CODEBASE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-goat-chat-agent/codebase-overview/"
    const val LEARN_GOAT_CODEBASE_INDEX_TITLE = "Code walkthrough"  // official H1
    const val RELL_BEST_PRACTICES_INDEX_URL = "https://docs.chromia.com/rell/rell-best-practices"
    const val RELL_BEST_PRACTICES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/rell-best-practices/"
    const val RELL_BEST_PRACTICES_INDEX_TITLE = "Rell best practices"  // official H1
    const val RELL_STATEMENTS_LOOP_INDEX_URL = "https://docs.chromia.com/rell/language-features/statements/loop-statements"
    const val RELL_STATEMENTS_LOOP_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/statements/loop-statements/"
    const val RELL_STATEMENTS_LOOP_INDEX_TITLE = "Loop statements"  // official H1
    const val RELL_SYSTEMLIB_TIME_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/time"
    const val RELL_SYSTEMLIB_TIME_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/time/"
    const val RELL_SYSTEMLIB_TIME_INDEX_TITLE = "rell.time"  // official H1
    const val RELL_SECURITY_INDEX_URL = "https://docs.chromia.com/rell/security"
    const val RELL_SECURITY_INDEX_URL_SLASH = "https://docs.chromia.com/rell/security/"
    const val RELL_SECURITY_INDEX_TITLE = "Security tips for Chromia dapps"  // official H1
    const val LEARN_TAGS_ZKP_INDEX_URL = "https://learn.chromia.com/tags/ZKP"
    const val LEARN_TAGS_ZKP_INDEX_URL_SLASH = "https://learn.chromia.com/tags/ZKP/"
    const val LEARN_TAGS_ZKP_INDEX_TITLE = "Courses tagged with: ZKP"  // official H1

    val pages = listOf(SECURITY_URL, BEST_PRACTICES_URL)

    fun configDelayYaml(): String = """
        blockchains:
          my_blockchain:
            config:
              directory_chain:
                config_delay: 86400000
    """.trimIndent() + "\n"

    fun requireExample(): String = """
        operation transfer(from: account, to: account, asset, amount: big_integer) {
            require (from != to, "Sender and receiver have to be different");
            require (amount > 0, "Transfer amount must be positive");
        }
    """.trimIndent() + "\n"

    fun compositeKeyExample(): String = """
        entity balance {
          key accounts.account, asset;
          mutable amount: big_integer;
        }
        entity account {
          key id: byte_array;
          index type: text;
        }
    """.trimIndent() + "\n"

    fun inputValidationExample(): String = """
        function validate_asset_registration(
            name: text,
            symbol: text,
            decimals: integer
        ): boolean {
            require(name.size() >= 1, "Name cannot be empty");
            require(name.size() <= 1024, "Name too long");
            require(symbol.matches("^[A-Z0-9_]+$"),
                "Symbol must contain only uppercase letters, numbers, and underscores");
            require(symbol.size() <= 10, "Symbol too long");
            require(decimals >= 0, "Decimals cannot be negative");
            require(decimals <= 18, "Too many decimal places");
            return true;
        }
    """.trimIndent() + "\n"

    fun missingBalanceExample(): String = """
        function safe_get_balance(
            account_id: byte_array,
            asset_id: byte_array
        ): big_integer {
            val balance_record = balance @? {
                .account.id == account_id,
                .asset.id == asset_id
            };
            return if (balance_record != null) balance_record.amount else 0;
        }
    """.trimIndent() + "\n"

    fun runMustFailExample(): String = """
        function test_transfer_validation_must_fail() {
            val failure = rell.test.tx()
                .op(transfer(recipient, asset_id, -1))
                .run_must_fail("Amount must be positive");
            assert_true(failure.message.contains("Amount must be positive"));
        }
    """.trimIndent() + "\n"

    val securityKeys = listOf(
        "config.directory_chain.config_delay  # milliseconds; official example 86400000 = 24 hours",
        "lib.ft4.core.accounts.rate_limit.active",
        "lib.ft4.core.accounts.rate_limit.max_points",
        "lib.ft4.core.accounts.rate_limit.recovery_time",
        "lib.ft4.core.accounts.rate_limit.points_at_account_creation",
        "lib.governance.proposals.proposal_configs.option_item_limit",
        "lib.governance.proposals.proposal_configs.max_duration",
        "lib.governance.proposals.proposal_configs.min_duration",
        "lib.governance.votes.veto_config.veto_period"
    )

    val skipped = listOf(
        "proposal vote / retract (hard skip; official YAML keys only)",
        "live signing / chr tx / key generation",
        "official printed sample keys and 64-hex all-zero examples",
        "rell.test keypair sign helper  # official best-practices test uses test-scope sign; skipped here",
        "exploit recipes / attack procedures"
    )

    fun notes(): String = """
        Official Rell BUILD practice pages for CLI $CLI_SERIES. Rell language source tag $RELL_VERSION (docs may still list 0.16.4 — source wins); the chromia.yml compile.rellVersion pin is ${DappScaffold.RELL_VERSION}.
        Security: $SECURITY_URL
        Best practices: $BEST_PRACTICES_URL
        SQL analysis: $ANALYZE_URL (see chr_repl_help). RellDoc comments: $RELLDOC_URL (see chromia_rell_language_help).
        BUILD / read-only only. No exploit recipes, no signing, no key material.
        Official chromia.yml key on the security page: blockchains.<name>.config.directory_chain.config_delay
        (milliseconds; official example 86400000 = 24 hours). That key is NOT on blockchain-properties — quote the security page.
        Official governance moduleArgs keys (configs only): lib.governance.proposals.proposal_configs
        (option_item_limit, max_duration, min_duration) and lib.governance.votes.veto_config.veto_period.
        Proposal vote / retract is skipped.
        Official FT4 rate_limit keys (active, max_points, recovery_time, points_at_account_creation) already live on ft4_module_args.
        Official require example validates from != to and amount > 0. require details: chromia_rell_systemlib_help.
        Best practices: composite keys for contextual uniqueness; index fields used in filters / @* / joins; do not over-index (write cost).
        Missing rows: use @? then if (record != null) value else 0. Validate text sizes and symbol.matches before writes.
        Official account/asset id examples are 32 bytes and not all-zero — do not invent a 64-hex.
        Negative tests: rell.test.tx().op(...).run_must_fail("message") then assert_true(failure.message.contains(...)).
        Official best-practices test also calls .sign on a rell.test keypair — skipped here (no signing / no key material).
        Pagination list queries: chromia_cookbook_help. Formatting: spaces around operators, indented blocks, multi-line params.
        Official analyze-page example chain name house-key-example has a hyphen; CLI 0.20.14+ forbids hyphens — do not ship it.
        Official ECOSYSTEM ecosystem/extensions/ai_inference INDEX ($ECOSYSTEM_AI_INFERENCE_INDEX_URL 307 $ECOSYSTEM_AI_INFERENCE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_AI_INFERENCE_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/nodes/add-node INDEX ($ECOSYSTEM_ADD_NODE_INDEX_URL 307 $ECOSYSTEM_ADD_NODE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_ADD_NODE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc INDEX ($ECOSYSTEM_PMC_INDEX_URL 307 $ECOSYSTEM_PMC_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/subnode INDEX ($ECOSYSTEM_PMC_SUBNODE_INDEX_URL 307 $ECOSYSTEM_PMC_SUBNODE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_SUBNODE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/database/delete INDEX ($RELL_DATABASE_DELETE_INDEX_URL 307 $RELL_DATABASE_DELETE_INDEX_URL_SLASH 200 H1 $RELL_DATABASE_DELETE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/book-review/book-entity/tables INDEX ($LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL 301 $LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/ft4-asset/testing INDEX ($LEARN_FT4_ASSET_TESTING_INDEX_URL 301 $LEARN_FT4_ASSET_TESTING_INDEX_URL_SLASH 200 H1 $LEARN_FT4_ASSET_TESTING_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/ft4-demo-app/module-frontend-application/tools INDEX ($LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL 301 $LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/marketplace-course/module-assets/buy-mystery-card INDEX ($LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL 301 $LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL_SLASH 200 H1 $LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-two/scaffold INDEX ($LEARN_NEWS_SCAFFOLD_INDEX_URL 301 $LEARN_NEWS_SCAFFOLD_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_SCAFFOLD_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/tic-tac-toe/setup INDEX ($LEARN_TTT_SETUP_INDEX_URL 301 $LEARN_TTT_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_TTT_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX news-feed create-accounts ($LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL 301 $LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_CREATE_ACCOUNTS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/rell-best-practices INDEX ($RELL_BEST_PRACTICES_INDEX_URL 307 $RELL_BEST_PRACTICES_INDEX_URL_SLASH 200 H1 $RELL_BEST_PRACTICES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof frontend-explore ($LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL 301 $LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL_SLASH 200 H1 $LEARN_ZK_FRONTEND_EXPLORE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX chromia-goat-chat-agent codebase-overview ($LEARN_GOAT_CODEBASE_INDEX_URL 301 $LEARN_GOAT_CODEBASE_INDEX_URL_SLASH 200 H1 $LEARN_GOAT_CODEBASE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/statements/loop-statements INDEX ($RELL_STATEMENTS_LOOP_INDEX_URL 307 $RELL_STATEMENTS_LOOP_INDEX_URL_SLASH 200 H1 $RELL_STATEMENTS_LOOP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/namespaces/time INDEX ($RELL_SYSTEMLIB_TIME_INDEX_URL 307 $RELL_SYSTEMLIB_TIME_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_TIME_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/security INDEX ($RELL_SECURITY_INDEX_URL 307 $RELL_SECURITY_INDEX_URL_SLASH 200 H1 $RELL_SECURITY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official LEARN tags/ZKP INDEX ($LEARN_TAGS_ZKP_INDEX_URL 301 $LEARN_TAGS_ZKP_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_ZKP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("rell", RELL_VERSION)
        put("rellSourceTag", RELL_VERSION)
        put("rellVersionPin", DappScaffold.RELL_VERSION)
        put("tool", TOOL_NAME)
        put("security_docs", SECURITY_URL)
        put("best_practices_docs", BEST_PRACTICES_URL)
        put("analyze_docs", ANALYZE_URL)
        put("relldoc_docs", RELLDOC_URL)
        put("read_only", "true")
        put("pages", buildJsonArray { pages.forEach { add(JsonPrimitive(it)) } })
        put("config_delay_yaml", configDelayYaml())
        put("config_delay_ms", 86400000)
        put("require_example", requireExample())
        put("composite_key_example", compositeKeyExample())
        put("input_validation_example", inputValidationExample())
        put("missing_balance_example", missingBalanceExample())
        put("run_must_fail_example", runMustFailExample())
        put("security_keys", buildJsonArray { securityKeys.forEach { add(JsonPrimitive(it)) } })
        put("skipped", buildJsonArray { skipped.forEach { add(JsonPrimitive(it)) } })
        put("rate_limit_help", "ft4_module_args")
        put("require_help", ChromiaRellSystemlibHelp.TOOL_NAME)
        put("language_help", ChromiaRellLanguageHelp.TOOL_NAME)
        put("repl_help", ChrReplHelp.TOOL_NAME)
        put("cookbook_help", "chromia_cookbook_help")
        put("ecosystem_ai_inference_index_url_slash", ECOSYSTEM_AI_INFERENCE_INDEX_URL_SLASH)
        put("ecosystem_ai_inference_index_title", ECOSYSTEM_AI_INFERENCE_INDEX_TITLE)
        put("ecosystem_add_node_index_url_slash", ECOSYSTEM_ADD_NODE_INDEX_URL_SLASH)
        put("ecosystem_add_node_index_title", ECOSYSTEM_ADD_NODE_INDEX_TITLE)
        put("ecosystem_pmc_index_url_slash", ECOSYSTEM_PMC_INDEX_URL_SLASH)
        put("ecosystem_pmc_index_title", ECOSYSTEM_PMC_INDEX_TITLE)
        put("ecosystem_pmc_subnode_index_url_slash", ECOSYSTEM_PMC_SUBNODE_INDEX_URL_SLASH)
        put("ecosystem_pmc_subnode_index_title", ECOSYSTEM_PMC_SUBNODE_INDEX_TITLE)
        put("rell_database_delete_index_url_slash", RELL_DATABASE_DELETE_INDEX_URL_SLASH)
        put("rell_database_delete_index_title", RELL_DATABASE_DELETE_INDEX_TITLE)
        put("learn_book_review_entity_tables_index_url_slash", LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_URL_SLASH)
        put("learn_book_review_entity_tables_index_title", LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_TITLE)
        put("learn_ft4_asset_testing_index_url_slash", LEARN_FT4_ASSET_TESTING_INDEX_URL_SLASH)
        put("learn_ft4_asset_testing_index_title", LEARN_FT4_ASSET_TESTING_INDEX_TITLE)
        put("learn_ft4_demo_frontend_tools_index_url_slash", LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_URL_SLASH)
        put("learn_ft4_demo_frontend_tools_index_title", LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_TITLE)
        put("learn_marketplace_buy_mystery_index_url_slash", LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_URL_SLASH)
        put("learn_marketplace_buy_mystery_index_title", LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_TITLE)
        put("learn_news_scaffold_index_url_slash", LEARN_NEWS_SCAFFOLD_INDEX_URL_SLASH)
        put("learn_news_scaffold_index_title", LEARN_NEWS_SCAFFOLD_INDEX_TITLE)
        put("learn_ttt_setup_index_url_slash", LEARN_TTT_SETUP_INDEX_URL_SLASH)
        put("learn_ttt_setup_index_title", LEARN_TTT_SETUP_INDEX_TITLE)
        put("learn_news_create_accounts_index_url_slash", LEARN_NEWS_CREATE_ACCOUNTS_INDEX_URL_SLASH)
        put("learn_news_create_accounts_index_title", LEARN_NEWS_CREATE_ACCOUNTS_INDEX_TITLE)
        put("rell_best_practices_index_url_slash", RELL_BEST_PRACTICES_INDEX_URL_SLASH)
        put("rell_best_practices_index_title", RELL_BEST_PRACTICES_INDEX_TITLE)
        put("learn_zk_frontend_explore_index_url_slash", LEARN_ZK_FRONTEND_EXPLORE_INDEX_URL_SLASH)
        put("learn_zk_frontend_explore_index_title", LEARN_ZK_FRONTEND_EXPLORE_INDEX_TITLE)
        put("learn_goat_codebase_index_url_slash", LEARN_GOAT_CODEBASE_INDEX_URL_SLASH)
        put("learn_goat_codebase_index_title", LEARN_GOAT_CODEBASE_INDEX_TITLE)
        put("rell_statements_loop_index_url_slash", RELL_STATEMENTS_LOOP_INDEX_URL_SLASH)
        put("rell_statements_loop_index_title", RELL_STATEMENTS_LOOP_INDEX_TITLE)
        put("rell_systemlib_time_index_url_slash", RELL_SYSTEMLIB_TIME_INDEX_URL_SLASH)
        put("rell_systemlib_time_index_title", RELL_SYSTEMLIB_TIME_INDEX_TITLE)
        put("rell_security_index_url_slash", RELL_SECURITY_INDEX_URL_SLASH)
        put("rell_security_index_title", RELL_SECURITY_INDEX_TITLE)
        put("learn_tags_zkp_index_url_slash", LEARN_TAGS_ZKP_INDEX_URL_SLASH)
        put("learn_tags_zkp_index_title", LEARN_TAGS_ZKP_INDEX_TITLE)
        put("notes", notes())
    }
}
// Official ECOSYSTEM ecosystem/extensions/ai_inference INDEX leftovers encoded as ECOSYSTEM_AI_INFERENCE_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/nodes/add-node INDEX leftovers encoded as ECOSYSTEM_ADD_NODE_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc INDEX leftovers encoded as ECOSYSTEM_PMC_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/subnode INDEX leftovers encoded as ECOSYSTEM_PMC_SUBNODE_INDEX_* (query-only HELP ONLY).
// Official RELL rell/language-features/database/delete INDEX leftovers encoded as RELL_DATABASE_DELETE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/book-entity/tables INDEX leftovers encoded as LEARN_BOOK_REVIEW_ENTITY_TABLES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-asset/testing INDEX leftovers encoded as LEARN_FT4_ASSET_TESTING_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-frontend-application/tools INDEX leftovers encoded as LEARN_FT4_DEMO_FRONTEND_TOOLS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/marketplace-course/module-assets/buy-mystery-card INDEX leftovers encoded as LEARN_MARKETPLACE_BUY_MYSTERY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-two/scaffold INDEX leftovers encoded as LEARN_NEWS_SCAFFOLD_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/setup INDEX leftovers encoded as LEARN_TTT_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX news-feed create-accounts leftovers encoded as LEARN_NEWS_CREATE_ACCOUNTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/rell-best-practices INDEX leftovers encoded as RELL_BEST_PRACTICES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof frontend-explore leftovers encoded as LEARN_ZK_FRONTEND_EXPLORE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX chromia-goat-chat-agent codebase-overview leftovers encoded as LEARN_GOAT_CODEBASE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/statements/loop-statements INDEX leftovers encoded as RELL_STATEMENTS_LOOP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/namespaces/time INDEX leftovers encoded as RELL_SYSTEMLIB_TIME_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/security INDEX leftovers encoded as RELL_SECURITY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/ZKP INDEX leftovers encoded as LEARN_TAGS_ZKP_INDEX_* (query-only HELP ONLY WRITE SKIP).
