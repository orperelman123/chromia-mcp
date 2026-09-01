package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Rell expression help. Quotes docs.chromia.com/rell expression pages only.
 * Does not invent operators. Rell language source tag 0.16.7 (docs may list 0.16.4).
 */
object ChromiaRellExpressionsHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val RELL_VERSION = DappScaffold.RELL_SOURCE_TAG
    const val TOOL_NAME = "chromia_rell_expressions_help"
    const val INDEX_URL = "https://docs.chromia.com/rell/language-features/expressions/"
    const val VALUES_URL = "https://docs.chromia.com/rell/language-features/expressions/values"
    const val OPERATORS_URL = "https://docs.chromia.com/rell/language-features/expressions/operators"
    const val CONDITIONAL_URL = "https://docs.chromia.com/rell/language-features/expressions/conditional-expressions"
    const val JUMP_URL = "https://docs.chromia.com/rell/language-features/expressions/jump-expressions"
    const val LAMBDA_URL = "https://docs.chromia.com/rell/language-features/expressions/lambda-expressions"
    const val ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_URL = "https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filechain"
    const val ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filechain/"
    const val ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_TITLE = "Deploy Filechain"  // official H1
    const val ECOSYSTEM_GOV_ICMF_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/extensions/icmf"
    const val ECOSYSTEM_GOV_ICMF_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/extensions/icmf/"
    const val ECOSYSTEM_GOV_ICMF_INDEX_TITLE = "Governance Tool ICMF extension"  // official H1
    const val ECOSYSTEM_GOV_STRUCTURE_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure"
    const val ECOSYSTEM_GOV_STRUCTURE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/"
    const val ECOSYSTEM_GOV_STRUCTURE_INDEX_TITLE = "Governance structure"  // official H1
    const val ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/governance-voting-process"
    const val ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/governance-voting-process/"
    const val ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_TITLE = "Voting process"  // official H1
    const val RELL_CORE_CONCEPTS_INDEX_URL = "https://docs.chromia.com/rell/core-concepts"
    const val RELL_CORE_CONCEPTS_INDEX_URL_SLASH = "https://docs.chromia.com/rell/core-concepts/"
    const val RELL_CORE_CONCEPTS_INDEX_TITLE = "Core concepts"  // official H1
    const val LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_URL = "https://learn.chromia.com/courses/book-review/blockchain-transactions"
    const val LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/blockchain-transactions/"
    const val LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_TITLE = "Lesson 5 - Understand blockchain state and transactions"  // official H1
    const val LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_URL = "https://learn.chromia.com/courses/book-review/build-client/sign-transaction"
    const val LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/build-client/sign-transaction/"
    const val LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_TITLE = "Connecting to the Chromia blockchain"  // official H1
    const val LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_URL = "https://learn.chromia.com/courses/rell-masterclass/sub-queries"
    const val LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-masterclass/sub-queries/"
    const val LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_TITLE = "Subqueries"  // official H1
    const val LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/burn"
    const val LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/burn/"
    const val LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_TITLE = "Lesson 5 - Burn Tokens"  // official H1
    const val LEARN_MARKETPLACE_LIST_CARD_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-assets/list-card"
    const val LEARN_MARKETPLACE_LIST_CARD_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-assets/list-card/"
    const val LEARN_MARKETPLACE_LIST_CARD_INDEX_TITLE = "List a card for sale on the marketplace"  // official H1
    const val LEARN_NEWS_INCORPORATE_MODULES_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/project-structure/incorporate-modules"
    const val LEARN_NEWS_INCORPORATE_MODULES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/project-structure/incorporate-modules/"
    const val LEARN_NEWS_INCORPORATE_MODULES_INDEX_TITLE = "Incorporate modules in the dapp"  // official H1
    const val LEARN_TTT_SCAFFOLD_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-two/scaffold"
    const val LEARN_TTT_SCAFFOLD_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-two/scaffold/"
    const val LEARN_TTT_SCAFFOLD_INDEX_TITLE = "Project scaffold"  // official H1
    const val LEARN_VECTOR_DB_USE_CASES_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/use-cases"
    const val LEARN_VECTOR_DB_USE_CASES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/use-cases/"
    const val LEARN_VECTOR_DB_USE_CASES_INDEX_TITLE = "Use cases and extensions"  // official H1
    const val LEARN_ZK_CIRCOM_COMPILE_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-compile"
    const val LEARN_ZK_CIRCOM_COMPILE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-compile/"
    const val LEARN_ZK_CIRCOM_COMPILE_INDEX_TITLE = "Circom circuits: compile"  // official H1
    const val LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_URL = "https://learn.chromia.com/courses/rell-integration-test/introduction"
    const val LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-integration-test/introduction/"
    const val LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_TITLE = "Run Integration tests with Rell and TypeScript"  // official H1
    const val LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_URL = "https://learn.chromia.com/courses/book-review/sign-transaction/sign-transaction"
    const val LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/sign-transaction/sign-transaction/"
    const val LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_TITLE = "Sign a transaction"  // official H1
    const val RELL_EXPRESSIONS_INDEX_URL = "https://docs.chromia.com/rell/language-features/expressions"
    const val RELL_EXPRESSIONS_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/expressions/"
    const val RELL_EXPRESSIONS_INDEX_TITLE = "Expressions"  // official H1
    const val RELL_EXPRESSIONS_CONDITIONAL_INDEX_URL = "https://docs.chromia.com/rell/language-features/expressions/conditional-expressions"
    const val RELL_EXPRESSIONS_CONDITIONAL_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/expressions/conditional-expressions/"
    const val RELL_EXPRESSIONS_CONDITIONAL_INDEX_TITLE = "Conditional expressions"  // official H1
    const val RELL_MODULE_MOUNT_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/mount"
    const val RELL_MODULE_MOUNT_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/mount/"
    const val RELL_MODULE_MOUNT_INDEX_TITLE = "Mount names"  // official H1
    const val RELL_SYSTEMLIB_OP_CONTEXT_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context"
    const val RELL_SYSTEMLIB_OP_CONTEXT_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context/"
    const val RELL_SYSTEMLIB_OP_CONTEXT_INDEX_TITLE = "op_context"  // official H1
    const val RELL_TYPES_SUB_INDEX_URL = "https://docs.chromia.com/rell/language-features/types/sub-types"
    const val RELL_TYPES_SUB_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/types/sub-types/"
    const val RELL_TYPES_SUB_INDEX_TITLE = "Subtypes"  // official H1

    val pages = listOf(INDEX_URL, VALUES_URL, OPERATORS_URL, CONDITIONAL_URL, JUMP_URL, LAMBDA_URL)

    val officialOperators = listOf(
        ".  # member access: user.name, s.sub(5, 10)",
        "()  # function call: print('Hello'), value.to_text()",
        "[]  # element access: values[i]",
        "+ - * / % ++ --  # arithmetic; + also concatenates text",
        "< > <= >= in not in  # relational",
        "== !=  # value equality (recursive for collections/tuples/structs; entity = object IDs)",
        "=== !==  # reference equality; official types: tuple, struct, list, set, map, GTV, range",
        "??  # unary null check; equivalent to != null",
        "and or not  # logical words, not C-style symbols",
        "= *= /= %= += -=  # assignment; on an entity attribute this becomes update",
        "@  # at-operator (database overview)",
        "if  # if expression always needs else"
    )

    val valueLiterals = listOf(
        "null  # own type null",
        "true / false",
        "integer  # 123, 0, -456",
        "text  # 'Hello' or \"World\"",
        "byte_array  # x'1234' or x\"ABCD\"",
        "big_integer  # suffix L",
        "decimal  # 123.456"
    )

    fun valuesExample(): String = """
        val my_list = [
            123,
            456,
            789,
        ];
        val my_map = [
            123: 'A',
            456: 'B',
        ];
    """.trimIndent() + "\n"

    fun equalityExample(): String = """
        val x = [1, 2, 3];
        val y = list(x);
        print(x == y);
        print(x === y);
    """.trimIndent() + "\n"

    fun nullCheckExample(): String = """
        val u = user @? { .name == 'Bob' };
        if (u??) {
        }
    """.trimIndent() + "\n"

    fun ifExpressionExample(): String = """
        function max_of(a: integer, b: integer) {
            val max = if (a >= b) a else b;
            return max;
        }
    """.trimIndent() + "\n"

    fun conditionalExample(): String = """
        val max = if (a >= b) a else b;
        val size = when (n) {
            0 -> 'zero';
            1, 2 -> 'small';
            else -> 'big';
        };
    """.trimIndent() + "\n"

    fun valueBlockExample(): String = """
        val x = if (cond) {
            val v = compute();
            v * 2
        } else fallback;
    """.trimIndent() + "\n"

    fun jumpExample(): String = """
        function f(x: integer?): integer {
            val y = x ?: return -1;
            return x + y;
        }
        val u = user @? { .name == n } ?: return 'no such user';
        val v = if (n < 0) return -n else n * 2;
    """.trimIndent() + "\n"

    fun lambdaExample(): String = """
        x -> x * 2
        (x, y) -> x + y
        () -> 42
        val f: (integer) -> integer = n -> {
            val doubled = n * 2;
            doubled + 1
        };
        val g = (x: integer) -> x * 2;
    """.trimIndent() + "\n"

    fun captureExample(): String = """
        var n = 7;
        val g: (integer) -> integer = x -> x + n;
        n = 100;
        g(1);
        val xs = [1, 2, 3];
        val h: () -> integer = () -> xs.size();
        xs.add(4);
        h();
    """.trimIndent() + "\n"

    fun notes(): String = """
        Official Rell expression pages for CLI $CLI_SERIES. Rell language source tag $RELL_VERSION (docs may still list 0.16.4 — source wins); the chromia.yml compile.rellVersion pin is ${DappScaffold.RELL_VERSION}.
        Index: $INDEX_URL
        Values: $VALUES_URL  Operators: $OPERATORS_URL
        Conditional: $CONDITIONAL_URL  Jump: $JUMP_URL  Lambda: $LAMBDA_URL
        Official values-page literals: null, boolean, integer, text ('Hello' / "World"),
        byte_array (x'1234' / x"ABCD"), big_integer suffix L, decimal.
        Text escapes on the values page: \r \n \t \b \" \' \\ and \u003A.
        Trailing commas are allowed in any comma-separated list (collections, function parameters, enums).
        Official operators only — do not invent C-style logical symbols or bitwise operators.
        Logical operators are the words and / or / not.
        == / != compare values. === / !== compare references for tuple, struct, list, set, map, GTV, range.
        ?? is a unary null check equivalent to != null.
        Assignment on an entity attribute is translated to an update statement (Rell language; see chromia_rell_database_help).
        if expression always needs else. when expression must be exhaustive (else makes it so).
        Only the chosen arm evaluates. In an at-expression, when becomes SQL CASE WHEN ... THEN.
        Value-block arms (since 0.16.1): statements then a trailing expression without a semicolon.
        return in a value-block arm returns from the enclosing function. break / continue bind to the enclosing loop.
        Illegal in a global constant. In at-expression bodies and default-value expressions, return/break/continue cannot escape the block.
        Jump expressions (since 0.16.1): return, break, continue as expressions. They never produce a value.
        return is greedy: x ?: return 1 + 2 returns 3. Illegal in lambda bodies. Illegal in a database at-expression's SQL.
        Lambdas (since 0.16.1): x -> x * 2, (x, y) -> x + y, () -> 42. Body is an expression or a value block.
        return is illegal inside a lambda. Annotate all parameters or none. Return type always inferred.
        Capture is by value at creation for the binding; later reassignment of the outer variable is not seen.
        A captured mutable object is captured by reference, so in-place mutation is seen.
        Types and literals also: chromia_rell_types_help. Statement forms: chromia_rell_statements_help.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Official ECOSYSTEM ecosystem/filehub/filehub-setup/deploy-filechain INDEX ($ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_URL 307 $ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/governance/getting-started/extensions/icmf INDEX ($ECOSYSTEM_GOV_ICMF_INDEX_URL 307 $ECOSYSTEM_GOV_ICMF_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_ICMF_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure INDEX ($ECOSYSTEM_GOV_STRUCTURE_INDEX_URL 307 $ECOSYSTEM_GOV_STRUCTURE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_STRUCTURE_INDEX_TITLE HELP ONLY WRITE SKIP).
        Official ECOSYSTEM ecosystem/governance/governance-voting-process INDEX ($ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_URL 307 $ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_TITLE HELP ONLY WRITE SKIP).
        Official RELL rell/core-concepts INDEX ($RELL_CORE_CONCEPTS_INDEX_URL 307 $RELL_CORE_CONCEPTS_INDEX_URL_SLASH 200 H1 $RELL_CORE_CONCEPTS_INDEX_TITLE HELP ONLY WRITE SKIP).
        Official LEARN courses/book-review/blockchain-transactions INDEX ($LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_URL 301 $LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_TITLE HELP ONLY WRITE SKIP).
        Official LEARN courses/book-review/build-client/sign-transaction INDEX ($LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_URL 301 $LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_TITLE HELP ONLY WRITE SKIP).
        Official LEARN courses/rell-masterclass/sub-queries INDEX ($LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_URL 301 $LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_URL_SLASH 200 H1 $LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP. nested expressions exists empty in operator subqueries refine search results efficient database queries.
        Official LEARN courses/ft4-demo-app/module-frontend-application/burn INDEX ($LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_URL 301 $LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/marketplace-course/module-assets/list-card INDEX ($LEARN_MARKETPLACE_LIST_CARD_INDEX_URL 301 $LEARN_MARKETPLACE_LIST_CARD_INDEX_URL_SLASH 200 H1 $LEARN_MARKETPLACE_LIST_CARD_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/project-structure/incorporate-modules INDEX ($LEARN_NEWS_INCORPORATE_MODULES_INDEX_URL 301 $LEARN_NEWS_INCORPORATE_MODULES_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_INCORPORATE_MODULES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/expressions INDEX ($RELL_EXPRESSIONS_INDEX_URL 307 $RELL_EXPRESSIONS_INDEX_URL_SLASH 200 H1 $RELL_EXPRESSIONS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/tic-tac-toe/module-two/scaffold INDEX ($LEARN_TTT_SCAFFOLD_INDEX_URL 301 $LEARN_TTT_SCAFFOLD_INDEX_URL_SLASH 200 H1 $LEARN_TTT_SCAFFOLD_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX vector-db use-cases ($LEARN_VECTOR_DB_USE_CASES_INDEX_URL 301 $LEARN_VECTOR_DB_USE_CASES_INDEX_URL_SLASH GET 200 H1 $LEARN_VECTOR_DB_USE_CASES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/expressions/conditional-expressions INDEX ($RELL_EXPRESSIONS_CONDITIONAL_INDEX_URL 307 $RELL_EXPRESSIONS_CONDITIONAL_INDEX_URL_SLASH 200 H1 $RELL_EXPRESSIONS_CONDITIONAL_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof circom-circuits-compile ($LEARN_ZK_CIRCOM_COMPILE_INDEX_URL 301 $LEARN_ZK_CIRCOM_COMPILE_INDEX_URL_SLASH GET 200 H1 $LEARN_ZK_CIRCOM_COMPILE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX rell-integration-test introduction ($LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_URL 301 $LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_URL_SLASH GET 200 H1 $LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX book-review sign-transaction child ($LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_URL 301 $LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_URL_SLASH GET 200 H1 $LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/modules/mount INDEX ($RELL_MODULE_MOUNT_INDEX_URL 307 $RELL_MODULE_MOUNT_INDEX_URL_SLASH 200 H1 $RELL_MODULE_MOUNT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/namespaces/op_context INDEX ($RELL_SYSTEMLIB_OP_CONTEXT_INDEX_URL 307 $RELL_SYSTEMLIB_OP_CONTEXT_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_OP_CONTEXT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/types/sub-types INDEX ($RELL_TYPES_SUB_INDEX_URL 307 $RELL_TYPES_SUB_INDEX_URL_SLASH 200 H1 $RELL_TYPES_SUB_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("rell", RELL_VERSION)
        put("rellSourceTag", RELL_VERSION)
        put("rellVersionPin", DappScaffold.RELL_VERSION)
        put("tool", TOOL_NAME)
        put("docs", INDEX_URL)
        put("values_docs", VALUES_URL)
        put("operators_docs", OPERATORS_URL)
        put("conditional_docs", CONDITIONAL_URL)
        put("jump_docs", JUMP_URL)
        put("lambda_docs", LAMBDA_URL)
        put("pages", buildJsonArray { pages.forEach { add(JsonPrimitive(it)) } })
        put("official_operators", buildJsonArray { officialOperators.forEach { add(JsonPrimitive(it)) } })
        put("value_literals", buildJsonArray { valueLiterals.forEach { add(JsonPrimitive(it)) } })
        put("values_example", valuesExample())
        put("equality_example", equalityExample())
        put("null_check_example", nullCheckExample())
        put("if_expression_example", ifExpressionExample())
        put("conditional_example", conditionalExample())
        put("value_block_example", valueBlockExample())
        put("jump_example", jumpExample())
        put("lambda_example", lambdaExample())
        put("capture_example", captureExample())
        put("types_help", ChromiaRellTypesHelp.TOOL_NAME)
        put("statements_help", ChromiaRellStatementsHelp.TOOL_NAME)
        put("ecosystem_filehub_deploy_filechain_index_url_slash", ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_URL_SLASH)
        put("ecosystem_filehub_deploy_filechain_index_title", ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_TITLE)
        put("ecosystem_gov_icmf_index_url_slash", ECOSYSTEM_GOV_ICMF_INDEX_URL_SLASH)
        put("ecosystem_gov_icmf_index_title", ECOSYSTEM_GOV_ICMF_INDEX_TITLE)
        put("ecosystem_gov_structure_index_url_slash", ECOSYSTEM_GOV_STRUCTURE_INDEX_URL_SLASH)
        put("ecosystem_gov_structure_index_title", ECOSYSTEM_GOV_STRUCTURE_INDEX_TITLE)
        put("ecosystem_gov_voting_process_index_url_slash", ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_URL_SLASH)
        put("ecosystem_gov_voting_process_index_title", ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_TITLE)
        put("rell_core_concepts_index_url_slash", RELL_CORE_CONCEPTS_INDEX_URL_SLASH)
        put("rell_core_concepts_index_title", RELL_CORE_CONCEPTS_INDEX_TITLE)
        put("learn_book_review_transactions_index_url_slash", LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_URL_SLASH)
        put("learn_book_review_transactions_index_title", LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_TITLE)
        put("learn_book_review_connect_chain_index_url_slash", LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_URL_SLASH)
        put("learn_book_review_connect_chain_index_title", LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_TITLE)
        put("learn_rell_masterclass_subqueries_index_url_slash", LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_URL_SLASH)
        put("learn_rell_masterclass_subqueries_index_title", LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_TITLE)
        put("learn_ft4_demo_frontend_burn_index_url_slash", LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_URL_SLASH)
        put("learn_ft4_demo_frontend_burn_index_title", LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_TITLE)
        put("learn_marketplace_list_card_index_url_slash", LEARN_MARKETPLACE_LIST_CARD_INDEX_URL_SLASH)
        put("learn_marketplace_list_card_index_title", LEARN_MARKETPLACE_LIST_CARD_INDEX_TITLE)
        put("learn_news_incorporate_modules_index_url_slash", LEARN_NEWS_INCORPORATE_MODULES_INDEX_URL_SLASH)
        put("learn_news_incorporate_modules_index_title", LEARN_NEWS_INCORPORATE_MODULES_INDEX_TITLE)
        put("rell_expressions_index_url_slash", RELL_EXPRESSIONS_INDEX_URL_SLASH)
        put("rell_expressions_index_title", RELL_EXPRESSIONS_INDEX_TITLE)
        put("learn_ttt_scaffold_index_url_slash", LEARN_TTT_SCAFFOLD_INDEX_URL_SLASH)
        put("learn_ttt_scaffold_index_title", LEARN_TTT_SCAFFOLD_INDEX_TITLE)
        put("learn_vector_db_use_cases_index_url_slash", LEARN_VECTOR_DB_USE_CASES_INDEX_URL_SLASH)
        put("learn_vector_db_use_cases_index_title", LEARN_VECTOR_DB_USE_CASES_INDEX_TITLE)
        put("rell_expressions_conditional_index_url_slash", RELL_EXPRESSIONS_CONDITIONAL_INDEX_URL_SLASH)
        put("rell_expressions_conditional_index_title", RELL_EXPRESSIONS_CONDITIONAL_INDEX_TITLE)
        put("learn_zk_circom_compile_index_url_slash", LEARN_ZK_CIRCOM_COMPILE_INDEX_URL_SLASH)
        put("learn_zk_circom_compile_index_title", LEARN_ZK_CIRCOM_COMPILE_INDEX_TITLE)
        put("learn_rell_integration_test_intro_index_url_slash", LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_URL_SLASH)
        put("learn_rell_integration_test_intro_index_title", LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_TITLE)
        put("learn_book_review_sign_tx_sign_index_url_slash", LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_URL_SLASH)
        put("learn_book_review_sign_tx_sign_index_title", LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_TITLE)
        put("rell_module_mount_index_url_slash", RELL_MODULE_MOUNT_INDEX_URL_SLASH)
        put("rell_module_mount_index_title", RELL_MODULE_MOUNT_INDEX_TITLE)
        put("rell_systemlib_op_context_index_url_slash", RELL_SYSTEMLIB_OP_CONTEXT_INDEX_URL_SLASH)
        put("rell_systemlib_op_context_index_title", RELL_SYSTEMLIB_OP_CONTEXT_INDEX_TITLE)
        put("rell_types_sub_index_url_slash", RELL_TYPES_SUB_INDEX_URL_SLASH)
        put("rell_types_sub_index_title", RELL_TYPES_SUB_INDEX_TITLE)
        put("notes", notes())
    }
}

// Official ECOSYSTEM ecosystem/filehub/filehub-setup/deploy-filechain INDEX leftovers encoded as ECOSYSTEM_FILEHUB_DEPLOY_FILECHAIN_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/governance/getting-started/extensions/icmf INDEX leftovers encoded as ECOSYSTEM_GOV_ICMF_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/getting-started/governance-structure INDEX leftovers encoded as ECOSYSTEM_GOV_STRUCTURE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official ECOSYSTEM ecosystem/governance/governance-voting-process INDEX leftovers encoded as ECOSYSTEM_GOV_VOTING_PROCESS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/core-concepts INDEX leftovers encoded as RELL_CORE_CONCEPTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/blockchain-transactions INDEX leftovers encoded as LEARN_BOOK_REVIEW_TRANSACTIONS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/build-client/sign-transaction INDEX leftovers encoded as LEARN_BOOK_REVIEW_CONNECT_CHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/rell-masterclass/sub-queries INDEX leftovers encoded as LEARN_RELL_MASTERCLASS_SUBQUERIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-frontend-application/burn INDEX leftovers encoded as LEARN_FT4_DEMO_FRONTEND_BURN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/marketplace-course/module-assets/list-card INDEX leftovers encoded as LEARN_MARKETPLACE_LIST_CARD_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/project-structure/incorporate-modules INDEX leftovers encoded as LEARN_NEWS_INCORPORATE_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/expressions INDEX leftovers encoded as RELL_EXPRESSIONS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-two/scaffold INDEX leftovers encoded as LEARN_TTT_SCAFFOLD_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX vector-db use-cases leftovers encoded as LEARN_VECTOR_DB_USE_CASES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/expressions/conditional-expressions INDEX leftovers encoded as RELL_EXPRESSIONS_CONDITIONAL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof circom-circuits-compile leftovers encoded as LEARN_ZK_CIRCOM_COMPILE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX rell-integration-test introduction leftovers encoded as LEARN_RELL_INTEGRATION_TEST_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX book-review sign-transaction child leftovers encoded as LEARN_BOOK_REVIEW_SIGN_TX_SIGN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/modules/mount INDEX leftovers encoded as RELL_MODULE_MOUNT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/namespaces/op_context INDEX leftovers encoded as RELL_SYSTEMLIB_OP_CONTEXT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/types/sub-types INDEX leftovers encoded as RELL_TYPES_SUB_INDEX_* (query-only HELP ONLY WRITE SKIP).
