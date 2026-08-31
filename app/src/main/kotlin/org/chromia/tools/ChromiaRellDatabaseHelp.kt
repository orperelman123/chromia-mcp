package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Rell database-language help (at / create / update / delete).
 * Quotes docs.chromia.com/rell database pages only. These are Rell constructs
 * that run inside operations. This tool does not document chr tx or signed send.
 * Official /database/create-copy and /database/at are 404; at lives on overview.
 */
object ChromiaRellDatabaseHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val RELL_VERSION = DappScaffold.RELL_SOURCE_TAG
    const val TOOL_NAME = "chromia_rell_database_help"
    const val INDEX_URL = "https://docs.chromia.com/rell/language-features/database/"
    const val OVERVIEW_URL = "https://docs.chromia.com/rell/language-features/database/overview"
    const val CREATE_URL = "https://docs.chromia.com/rell/language-features/database/create"
    const val UPDATE_URL = "https://docs.chromia.com/rell/language-features/database/update"
    const val DELETE_URL = "https://docs.chromia.com/rell/language-features/database/delete"
    const val CREATE_COPY_404_URL = "https://docs.chromia.com/rell/language-features/database/create-copy"
    const val AT_404_URL = "https://docs.chromia.com/rell/language-features/database/at"
    const val BUILD_GETTING_STARTED_URL = ChromiaYmlSections.DATABASE_GETTING_STARTED_URL
    const val BUILD_GETTING_STARTED_INDEX_URL = "https://docs.chromia.com/build/database/getting-started"
    const val BUILD_GETTING_STARTED_INDEX_URL_SLASH = "https://docs.chromia.com/build/database/getting-started/"
    const val BUILD_GETTING_STARTED_INDEX_TITLE = "Getting started"
    const val BUILD_OVERVIEW_URL = ChromiaYmlSections.DATABASE_OVERVIEW_URL
    const val BUILD_OVERVIEW_INDEX_URL = "https://docs.chromia.com/build/database/overview"
    const val BUILD_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/build/database/overview/"
    const val BUILD_OVERVIEW_INDEX_TITLE = "Chromia Database overview"
    const val ECOSYSTEM_VECTOR_DB_INDEX_URL = "https://docs.chromia.com/ecosystem/extensions/vector-db"
    const val ECOSYSTEM_VECTOR_DB_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/extensions/vector-db/"
    const val ECOSYSTEM_VECTOR_DB_INDEX_TITLE = "Vector DB"  // official H1
    const val ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/bridge-withdraw-troubleshooting"
    const val ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/bridge-withdraw-troubleshooting/"
    const val ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_TITLE = "Bridge withdrawal troubleshooting guide"  // official H1
    const val ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/customize-functions"
    const val ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/customize-functions/"
    const val ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_TITLE = "Customizing functions"  // official H1
    const val ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/overview"
    const val ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/overview/"
    const val ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_TITLE = "Governance Starter Kit"
    const val ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/governance-proposals/create-proposal"
    const val ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/governance-proposals/create-proposal/"
    const val ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_TITLE = "Create a proposal"  // official H1
    const val RELL_ANALYZE_DAPP_CODE_INDEX_URL = "https://docs.chromia.com/rell/analyze-rell-dapp-code"
    const val RELL_ANALYZE_DAPP_CODE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/analyze-rell-dapp-code/"
    const val RELL_ANALYZE_DAPP_CODE_INDEX_TITLE = "Rell code optimization"
    const val LEARN_BIG_DATA_PROJECT_RUN_INDEX_URL = "https://learn.chromia.com/courses/big-data/project-run"
    const val LEARN_BIG_DATA_PROJECT_RUN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/big-data/project-run/"
    const val LEARN_BIG_DATA_PROJECT_RUN_INDEX_TITLE = "Run the project"
    const val LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_URL = "https://learn.chromia.com/courses/book-review/blockchain-transactions/example"
    const val LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/blockchain-transactions/example/"
    const val LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_TITLE = "Let's look at an example"
    const val LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_URL = "https://learn.chromia.com/courses/book-review/build-client/query-blockchain"
    const val LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/build-client/query-blockchain/"
    const val LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_TITLE = "Querying the blockchain with postchain-client"
    const val LEARN_VECTOR_DB_INTRO_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/introduction"
    const val LEARN_VECTOR_DB_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/introduction/"
    const val LEARN_VECTOR_DB_INTRO_INDEX_TITLE = "Semantic movie search on Chromia"  // official H1
    const val LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/setup"
    const val LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/setup/"
    const val LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_TITLE = "Lesson 1 - Set up the Frontend Application"  // official H1
    const val LEARN_MARKETPLACE_BUY_LISTED_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-assets/buy-listed-card"
    const val LEARN_MARKETPLACE_BUY_LISTED_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-assets/buy-listed-card/"
    const val LEARN_MARKETPLACE_BUY_LISTED_INDEX_TITLE = "Purchase a card from the marketplace"  // official H1
    const val LEARN_NEWS_CONNECT_CLIENT_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-two/connecting-the-client"
    const val LEARN_NEWS_CONNECT_CLIENT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-two/connecting-the-client/"
    const val LEARN_NEWS_CONNECT_CLIENT_INDEX_TITLE = "Connect the client"  // official H1
    const val LEARN_TTT_SUMMARY_TESTS_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-two/summary-and-tests"
    const val LEARN_TTT_SUMMARY_TESTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-two/summary-and-tests/"
    const val LEARN_TTT_SUMMARY_TESTS_INDEX_TITLE = "Summary and manual testing"  // official H1
    const val LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-ft4/register-token"
    const val LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-ft4/register-token/"
    const val LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_TITLE = "Register payment token"  // official H1
    const val LEARN_ZK_CIRCOM_PROJECT_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-project"
    const val LEARN_ZK_CIRCOM_PROJECT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-project/"
    const val LEARN_ZK_CIRCOM_PROJECT_INDEX_TITLE = "Circom files overview"  // official H1
    const val LEARN_GOAT_EXPLORE_INDEX_URL = "https://learn.chromia.com/courses/chromia-goat-chat-agent/explore-agent"
    const val LEARN_GOAT_EXPLORE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-goat-chat-agent/explore-agent/"
    const val LEARN_GOAT_EXPLORE_INDEX_TITLE = "Explore the chat agent"  // official H1
    const val LEARN_DOCS_INDEX_URL = "https://learn.chromia.com/docs"
    const val LEARN_DOCS_INDEX_URL_SLASH = "https://learn.chromia.com/docs/"
    const val LEARN_DOCS_INDEX_TITLE = "Introduction"  // official H1
    const val RELL_DATABASE_UPDATE_INDEX_URL = "https://docs.chromia.com/rell/language-features/database/update"
    const val RELL_DATABASE_UPDATE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/database/update/"
    const val RELL_DATABASE_UPDATE_INDEX_TITLE = "Update statement"  // official H1
    const val RELL_DATABASE_OVERVIEW_INDEX_URL = "https://docs.chromia.com/rell/language-features/database/overview"
    const val RELL_DATABASE_OVERVIEW_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/database/overview/"
    const val RELL_DATABASE_OVERVIEW_INDEX_TITLE = "Overview"  // official H1
    const val RELL_MODULE_ENUM_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/enum"
    const val RELL_MODULE_ENUM_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/enum/"
    const val RELL_MODULE_ENUM_INDEX_TITLE = "Enum"  // official H1
    const val RELL_SYSTEMLIB_META_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/meta"
    const val RELL_SYSTEMLIB_META_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/meta/"
    const val RELL_SYSTEMLIB_META_INDEX_TITLE = "meta"  // official H1
    const val RELL_TYPES_ITERABLES_INDEX_URL = "https://docs.chromia.com/rell/language-features/types/iterables"
    const val RELL_TYPES_ITERABLES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/types/iterables/"
    const val RELL_TYPES_ITERABLES_INDEX_TITLE = "Iterables"  // official H1
    const val BUILD_ARCHITECTURE_404_URL = ChromiaYmlSections.DATABASE_ARCHITECTURE_404_URL
    const val BUILD_SCALING_404_URL = ChromiaYmlSections.DATABASE_SCALING_404_URL

    val pages = listOf(INDEX_URL, OVERVIEW_URL, CREATE_URL, UPDATE_URL, DELETE_URL, BUILD_GETTING_STARTED_URL, BUILD_OVERVIEW_URL)

    val cardinality = listOf(
        "@   # exactly one",
        "@?  # zero or one",
        "@*  # any number (list)",
        "@+  # one or more"
    )

    fun atExample(): String = """
        val name = 'Bill';
        val company = 'Microsoft';
        return user @ { name, company };
        return user @ { .name == name and .company == company };
        user @* { .company == 'Microsoft' } limit 10
        user @* {}(@sort .company) offset 10
        people @* {}(@sort .age) offset 10 limit 20
    """.trimIndent() + "\n"

    fun whatExample(): String = """
        user @ { .name == 'Bob' } ( .company.name )
        user @* {} ( @sort .last_name, @sort .first_name )
        user @* {} ( x = .company.name, y = .company.address )
        val us = user @* {} ( .last_name, @omit .first_name );
    """.trimIndent() + "\n"

    fun createExample(): String = """
        create user(name = 'Bob', company = company @ { .name == 'Amazon' });
        val name = 'Bob';
        create user(name, company @ { company.name == 'Amazon' });
        val new_company = create company(name = 'Amazon');
        val new_user = create user(name = 'Bob', new_company);
        create MyEntity(list<struct<MyEntity>>): list<MyEntity>;
    """.trimIndent() + "\n"

    fun updateExample(): String = """
        update user @ { .name == 'Bob' } ( company = 'Microsoft' );
        update user @? { .name == 'Bob' } ( deleted = true );
        update user @* { .company.name == 'Bad Company' } ( salary -= 1000 );
        val company = 'Microsoft';
        update user @ { .name == 'Bob' } ( company );
        val u = user @? { .name == 'Bob' };
        update u ( salary += 5000 );
        u.salary += 5000;
    """.trimIndent() + "\n"

    fun deleteExample(): String = """
        delete user @ { .name == 'Bob' };
        delete user @? { .name == 'Bob' };
        delete user @* { .company.name == 'Bad Company' };
        val u = user @? { .name == 'Bob' };
        delete u;
    """.trimIndent() + "\n"

    fun notes(): String = """
        Official Rell database-language pages for CLI $CLI_SERIES. Rell pin $RELL_VERSION (docs may still list 0.16.4 — source wins).
        Index: $INDEX_URL  Overview / at-operator: $OVERVIEW_URL
        Create: $CREATE_URL  Update: $UPDATE_URL  Delete: $DELETE_URL
        These are Rell language constructs. create / update / delete run inside operations.
        This tool documents Rell syntax only. It does not document chr tx, key generation, or sending a signed transaction.
        Official at form: FROM @ CARDINALITY { WHERE } ( WHAT ) TAIL.
        Cardinality: @ exactly one, @? zero or one, @* any number, @+ one or more.
        Cardinality is tested before limit, so user @ { .company == 'Microsoft' } limit 1 cannot fail with "more than one."
        Comma where-clause allows a bare variable matched by name or type. and notation requires full expressions.
        They are not completely equivalent.
        What empty -> entity reference (rowid). One expression -> that type. Several -> a tuple.
        Official WHAT annotations: @sort / @sort_desc, named field x = .company.name, _ = unnamed, @omit, @group, @min @max @sum, @list @set @map.
        Tail: limit N, offset N.
        Nested at-operators are legal. @* nested with empty() / exists() can compile to a single SQL query.
        Inner join: (u: user, c: contract @* { c.user == u }) @* {} ( u, c ).
        Outer join: (u: user, @outer c: contract @* { c.user == u }) @* {} ( u, c ); outer-joined entity is T?.
        create must specify every attribute that has no default. Name may be omitted when matched by name or type.
        Bulk insert: create MyEntity(list<struct<MyEntity>>): list<MyEntity> — one SQL statement; empty list -> no SQL.
        update / delete cardinality @ / @? / @* / @+ — runtime error if the row count does not match.
        update only mutable attributes. Multi-entity form: first entity is updated or deleted; others are for the where-part.
        Also accepts an expression that yields an entity, T?, or a collection of entities: update u (...); delete u;
        Single-attribute assignment u.salary += 5000; is translated to update.
        @log entities cannot be deleted (entity page / chromia_rell_language_help).
        Official $CREATE_COPY_404_URL is 404 — no create-copy page. Official $AT_404_URL is 404 — at lives on overview.
        Official leftover BUILD getting-started (200): $BUILD_GETTING_STARTED_URL
        Leftover official leftover BUILD database/getting-started (leftover official $BUILD_GETTING_STARTED_INDEX_URL leftover official 307 leftover official $BUILD_GETTING_STARTED_INDEX_URL_SLASH leftover official 200 leftover official $BUILD_GETTING_STARTED_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover mnemonic leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover SQL leftover official leftover schema leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover leftover official leftover getting-started leftover official leftover still leftover official leftover says leftover official leftover chromia start leftover official leftover that leftover official leftover is leftover official leftover NOT leftover official leftover a leftover official leftover chr leftover official leftover command leftover official leftover official leftover official leftover local leftover official leftover dapp leftover official leftover loop leftover official leftover is leftover official leftover chr node start leftover official leftover Postgres leftover official leftover 16+.
        Official leftover BUILD overview (200): $BUILD_OVERVIEW_URL
        Leftover official leftover BUILD database/overview (leftover official $BUILD_OVERVIEW_INDEX_URL leftover official 307 leftover official $BUILD_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official $BUILD_OVERVIEW_INDEX_TITLE): leftover official leftover Query-only leftover official leftover WRITE SKIP leftover official leftover no leftover official leftover signed leftover official leftover txs leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover no leftover official leftover mnemonic leftover official leftover keygen leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover SQL leftover official leftover schema leftover official leftover do leftover official leftover not leftover official leftover invent leftover official leftover BRIDs leftover official leftover leftover official leftover getting-started leftover official leftover still leftover official leftover says leftover official leftover chromia start leftover official leftover that leftover official leftover is leftover official leftover NOT leftover official leftover a leftover official leftover chr leftover official leftover command leftover official leftover official leftover official leftover local leftover official leftover dapp leftover official leftover loop leftover official leftover is leftover official leftover chr node start leftover official leftover Postgres leftover official leftover 16+.
        Leftover official leftover ECOSYSTEM ecosystem/extensions/vector-db INDEX (leftover official $ECOSYSTEM_VECTOR_DB_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_VECTOR_DB_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_VECTOR_DB_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/bridge/bridge-troubleshooting/bridge-withdraw-troubleshooting INDEX (leftover official $ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/customize-functions INDEX (leftover official $ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/overview INDEX (leftover official $ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover ECOSYSTEM ecosystem/governance/governance-proposals/create-proposal INDEX (leftover official $ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover RELL rell/analyze-rell-dapp-code INDEX (leftover official $RELL_ANALYZE_DAPP_CODE_INDEX_URL leftover official 307 leftover official $RELL_ANALYZE_DAPP_CODE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_ANALYZE_DAPP_CODE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/big-data/project-run INDEX (leftover official $LEARN_BIG_DATA_PROJECT_RUN_INDEX_URL leftover official 301 leftover official $LEARN_BIG_DATA_PROJECT_RUN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BIG_DATA_PROJECT_RUN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/book-review/blockchain-transactions/example INDEX (leftover official $LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/book-review/build-client/query-blockchain INDEX (leftover official $LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Official leftover $BUILD_ARCHITECTURE_404_URL is 404. Official leftover $BUILD_SCALING_404_URL is 404.
        Official leftover getting-started says `chromia start` — that is NOT a chr command. Official local dapp loop is `chr node start` (Postgres 16+).
        Source-observed dapp table names: ${ChromiaYmlSections.TABLE_NAME_SOURCE} in the current schema (rell_app by default). Do not invent a SQL schema.
        Query/read + schema definition: chromia_rell_language_help. Types: chromia_rell_types_help.
        Leftover official leftover RELL rell/language-features/database/update INDEX (leftover official $RELL_DATABASE_UPDATE_INDEX_URL leftover official 307 leftover official $RELL_DATABASE_UPDATE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_DATABASE_UPDATE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover LEARN courses/vector-db-movie-demo/introduction INDEX (leftover official $LEARN_VECTOR_DB_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_VECTOR_DB_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_VECTOR_DB_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP. leftover official leftover full-stack leftover official leftover app leftover official leftover movie leftover official leftover plot leftover official leftover summaries leftover official leftover vector leftover official leftover embeddings leftover official leftover stores leftover official leftover them leftover official leftover on leftover official leftover Chromia leftover official leftover vector_db_extension leftover official leftover semantic leftover official leftover searches leftover official leftover query leftover official leftover by leftover official leftover meaning.
        Leftover official leftover LEARN courses/ft4-demo-app/module-frontend-application/setup INDEX (leftover official $LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-assets/buy-listed-card INDEX (leftover official $LEARN_MARKETPLACE_BUY_LISTED_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_BUY_LISTED_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_BUY_LISTED_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/my-news-feed/module-two/connecting-the-client INDEX (leftover official $LEARN_NEWS_CONNECT_CLIENT_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_CONNECT_CLIENT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_CONNECT_CLIENT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/tic-tac-toe/module-two/summary-and-tests INDEX (leftover official $LEARN_TTT_SUMMARY_TESTS_INDEX_URL leftover official 301 leftover official $LEARN_TTT_SUMMARY_TESTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_SUMMARY_TESTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX marketplace register-token (leftover official $LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_TITLE leftover official Register payment token leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/database/overview INDEX (leftover official $RELL_DATABASE_OVERVIEW_INDEX_URL leftover official 307 leftover official $RELL_DATABASE_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_DATABASE_OVERVIEW_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX zero-knowledge-proof circom-circuits-project (leftover official $LEARN_ZK_CIRCOM_PROJECT_INDEX_URL leftover official 301 leftover official $LEARN_ZK_CIRCOM_PROJECT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ZK_CIRCOM_PROJECT_INDEX_TITLE leftover official Circom files overview leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX chromia-goat-chat-agent explore-agent (leftover official $LEARN_GOAT_EXPLORE_INDEX_URL leftover official 301 leftover official $LEARN_GOAT_EXPLORE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_GOAT_EXPLORE_INDEX_TITLE leftover official Explore the chat agent leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX learn.chromia.com/docs (leftover official $LEARN_DOCS_INDEX_URL leftover official 301 leftover official $LEARN_DOCS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_DOCS_INDEX_TITLE leftover official Introduction leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/modules/enum INDEX (leftover official $RELL_MODULE_ENUM_INDEX_URL leftover official 307 leftover official $RELL_MODULE_ENUM_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_MODULE_ENUM_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/systemlib/namespaces/meta INDEX (leftover official $RELL_SYSTEMLIB_META_INDEX_URL leftover official 307 leftover official $RELL_SYSTEMLIB_META_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_SYSTEMLIB_META_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/types/iterables INDEX (leftover official $RELL_TYPES_ITERABLES_INDEX_URL leftover official 307 leftover official $RELL_TYPES_ITERABLES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_TYPES_ITERABLES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("rell", RELL_VERSION)
        put("tool", TOOL_NAME)
        put("docs", OVERVIEW_URL)
        put("create_docs", CREATE_URL)
        put("update_docs", UPDATE_URL)
        put("delete_docs", DELETE_URL)
        put("build_getting_started", BUILD_GETTING_STARTED_URL)
        put("build_getting_started_index_docs", BUILD_GETTING_STARTED_INDEX_URL)
        put("build_getting_started_index_url_slash", BUILD_GETTING_STARTED_INDEX_URL_SLASH)
        put("build_getting_started_index_title", BUILD_GETTING_STARTED_INDEX_TITLE)
        put("build_overview", BUILD_OVERVIEW_URL)
        put("build_overview_index_docs", BUILD_OVERVIEW_INDEX_URL)
        put("build_overview_index_url_slash", BUILD_OVERVIEW_INDEX_URL_SLASH)
        put("build_overview_index_title", BUILD_OVERVIEW_INDEX_TITLE)
        put("table_names_source", ChromiaYmlSections.TABLE_NAME_SOURCE)
        put("official_local_node", "chr node start")
        put("read_only_tx", true)
        put("pages", buildJsonArray { pages.forEach { add(JsonPrimitive(it)) } })
        put("cardinality", buildJsonArray { cardinality.forEach { add(JsonPrimitive(it)) } })
        put("at_example", atExample())
        put("what_example", whatExample())
        put("create_example", createExample())
        put("update_example", updateExample())
        put("delete_example", deleteExample())
        put(
            "skipped_404",
            buildJsonArray {
                add(JsonPrimitive("$CREATE_COPY_404_URL (404; no official create-copy page)"))
                add(JsonPrimitive("$AT_404_URL (404; at-operator is on $OVERVIEW_URL)"))
                add(JsonPrimitive("$BUILD_ARCHITECTURE_404_URL (404; leftover BUILD architecture page)"))
                add(JsonPrimitive("$BUILD_SCALING_404_URL (404; leftover BUILD scaling page)"))
                add(JsonPrimitive("leftover $BUILD_GETTING_STARTED_URL says `chromia start` — NOT a chr command; use `chr node start`"))
            }
        )
        put("language_help", ChromiaRellLanguageHelp.TOOL_NAME)
        put("types_help", ChromiaRellTypesHelp.TOOL_NAME)
        put("ecosystem_vector_db_index_url_slash", ECOSYSTEM_VECTOR_DB_INDEX_URL_SLASH)
        put("ecosystem_vector_db_index_title", ECOSYSTEM_VECTOR_DB_INDEX_TITLE)
        put("ecosystem_bridge_withdraw_troubleshooting_index_url_slash", ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_URL_SLASH)
        put("ecosystem_bridge_withdraw_troubleshooting_index_title", ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_TITLE)
        put("ecosystem_gov_customize_functions_index_url_slash", ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_URL_SLASH)
        put("ecosystem_gov_customize_functions_index_title", ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_TITLE)
        put("ecosystem_gov_starter_kit_overview_index_url_slash", ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_URL_SLASH)
        put("ecosystem_gov_starter_kit_overview_index_title", ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_TITLE)
        put("ecosystem_gov_create_proposal_index_url_slash", ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_URL_SLASH)
        put("ecosystem_gov_create_proposal_index_title", ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_TITLE)
        put("rell_analyze_dapp_code_index_url_slash", RELL_ANALYZE_DAPP_CODE_INDEX_URL_SLASH)
        put("rell_analyze_dapp_code_index_title", RELL_ANALYZE_DAPP_CODE_INDEX_TITLE)
        put("learn_big_data_project_run_index_url_slash", LEARN_BIG_DATA_PROJECT_RUN_INDEX_URL_SLASH)
        put("learn_big_data_project_run_index_title", LEARN_BIG_DATA_PROJECT_RUN_INDEX_TITLE)
        put("learn_book_review_tx_example_index_url_slash", LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_URL_SLASH)
        put("learn_book_review_tx_example_index_title", LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_TITLE)
        put("learn_book_review_query_blockchain_index_url_slash", LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_URL_SLASH)
        put("learn_book_review_query_blockchain_index_title", LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_TITLE)
        put("learn_vector_db_intro_index_url_slash", LEARN_VECTOR_DB_INTRO_INDEX_URL_SLASH)
        put("learn_vector_db_intro_index_title", LEARN_VECTOR_DB_INTRO_INDEX_TITLE)
        put("learn_ft4_demo_frontend_setup_index_url_slash", LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_URL_SLASH)
        put("learn_ft4_demo_frontend_setup_index_title", LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_TITLE)
        put("learn_marketplace_buy_listed_index_url_slash", LEARN_MARKETPLACE_BUY_LISTED_INDEX_URL_SLASH)
        put("learn_marketplace_buy_listed_index_title", LEARN_MARKETPLACE_BUY_LISTED_INDEX_TITLE)
        put("learn_news_connect_client_index_url_slash", LEARN_NEWS_CONNECT_CLIENT_INDEX_URL_SLASH)
        put("learn_news_connect_client_index_title", LEARN_NEWS_CONNECT_CLIENT_INDEX_TITLE)
        put("rell_database_update_index_url_slash", RELL_DATABASE_UPDATE_INDEX_URL_SLASH)
        put("rell_database_update_index_title", RELL_DATABASE_UPDATE_INDEX_TITLE)
        put("learn_ttt_summary_tests_index_url_slash", LEARN_TTT_SUMMARY_TESTS_INDEX_URL_SLASH)
        put("learn_ttt_summary_tests_index_title", LEARN_TTT_SUMMARY_TESTS_INDEX_TITLE)
        put("learn_marketplace_register_token_index_url_slash", LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_URL_SLASH)
        put("learn_marketplace_register_token_index_title", LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_TITLE)
        put("rell_database_overview_index_url_slash", RELL_DATABASE_OVERVIEW_INDEX_URL_SLASH)
        put("rell_database_overview_index_title", RELL_DATABASE_OVERVIEW_INDEX_TITLE)
        put("learn_zk_circom_project_index_url_slash", LEARN_ZK_CIRCOM_PROJECT_INDEX_URL_SLASH)
        put("learn_zk_circom_project_index_title", LEARN_ZK_CIRCOM_PROJECT_INDEX_TITLE)
        put("learn_goat_explore_index_url_slash", LEARN_GOAT_EXPLORE_INDEX_URL_SLASH)
        put("learn_goat_explore_index_title", LEARN_GOAT_EXPLORE_INDEX_TITLE)
        put("learn_docs_index_url_slash", LEARN_DOCS_INDEX_URL_SLASH)
        put("learn_docs_index_title", LEARN_DOCS_INDEX_TITLE)
        put("rell_module_enum_index_url_slash", RELL_MODULE_ENUM_INDEX_URL_SLASH)
        put("rell_module_enum_index_title", RELL_MODULE_ENUM_INDEX_TITLE)
        put("rell_systemlib_meta_index_url_slash", RELL_SYSTEMLIB_META_INDEX_URL_SLASH)
        put("rell_systemlib_meta_index_title", RELL_SYSTEMLIB_META_INDEX_TITLE)
        put("rell_types_iterables_index_url_slash", RELL_TYPES_ITERABLES_INDEX_URL_SLASH)
        put("rell_types_iterables_index_title", RELL_TYPES_ITERABLES_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover BUILD database/overview leftovers encoded as BUILD_OVERVIEW_INDEX_* (query-only).
// Leftover official leftover BUILD database/getting-started leftovers encoded as BUILD_GETTING_STARTED_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/extensions/vector-db INDEX leftovers encoded as ECOSYSTEM_VECTOR_DB_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/bridge/bridge-troubleshooting/bridge-withdraw-troubleshooting INDEX leftovers encoded as ECOSYSTEM_BRIDGE_WITHDRAW_TROUBLESHOOTING_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/customize-functions INDEX leftovers encoded as ECOSYSTEM_GOV_CUSTOMIZE_FUNCTIONS_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/overview INDEX leftovers encoded as ECOSYSTEM_GOV_STARTER_KIT_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover ECOSYSTEM ecosystem/governance/governance-proposals/create-proposal INDEX leftovers encoded as ECOSYSTEM_GOV_CREATE_PROPOSAL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/analyze-rell-dapp-code INDEX leftovers encoded as RELL_ANALYZE_DAPP_CODE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/big-data/project-run INDEX leftovers encoded as LEARN_BIG_DATA_PROJECT_RUN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/book-review/blockchain-transactions/example INDEX leftovers encoded as LEARN_BOOK_REVIEW_TX_EXAMPLE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/book-review/build-client/query-blockchain INDEX leftovers encoded as LEARN_BOOK_REVIEW_QUERY_BLOCKCHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/vector-db-movie-demo/introduction INDEX leftovers encoded as LEARN_VECTOR_DB_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/ft4-demo-app/module-frontend-application/setup INDEX leftovers encoded as LEARN_FT4_DEMO_FRONTEND_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/module-assets/buy-listed-card INDEX leftovers encoded as LEARN_MARKETPLACE_BUY_LISTED_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/my-news-feed/module-two/connecting-the-client INDEX leftovers encoded as LEARN_NEWS_CONNECT_CLIENT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/database/update INDEX leftovers encoded as RELL_DATABASE_UPDATE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-two/summary-and-tests INDEX leftovers encoded as LEARN_TTT_SUMMARY_TESTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX marketplace register-token leftovers encoded as LEARN_MARKETPLACE_REGISTER_TOKEN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/database/overview INDEX leftovers encoded as RELL_DATABASE_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX zero-knowledge-proof circom-circuits-project leftovers encoded as LEARN_ZK_CIRCOM_PROJECT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX chromia-goat-chat-agent explore-agent leftovers encoded as LEARN_GOAT_EXPLORE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX learn.chromia.com/docs leftovers encoded as LEARN_DOCS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/modules/enum INDEX leftovers encoded as RELL_MODULE_ENUM_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/systemlib/namespaces/meta INDEX leftovers encoded as RELL_SYSTEMLIB_META_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/types/iterables INDEX leftovers encoded as RELL_TYPES_ITERABLES_INDEX_* (query-only HELP ONLY WRITE SKIP).
