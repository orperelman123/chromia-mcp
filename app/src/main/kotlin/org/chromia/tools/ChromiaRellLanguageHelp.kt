package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Rell definition syntax (query / operation / entity / object / struct / enum / function / module
 * plus module pages: namespace / mount / abstract / size-constraint-annotations
 * plus /rell/rell-doc (RellDoc) and /rell/language-features/identifiers-syntax).
 * Quotes official docs.chromia.com/rell pages only. Does not invent language features.
 * Modules/imports/layouts live on chromia_project_structure_help.
 * REAL bug fixed: size-constraint is not parameters-only; official page also covers struct / entity / object
 * attributes. Official slug is /modules/size-constraint-annotations (200); /modules/size-constraint is 404.
 * Source wins: Rell 0.16.7 (docs may still list 0.16.4).
 */
object ChromiaRellLanguageHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val RELL_VERSION = DappScaffold.RELL_SOURCE_TAG
    const val TOOL_NAME = "chromia_rell_language_help"
    const val INTRO_URL = "https://docs.chromia.com/rell/rell-intro"
    const val CORE_CONCEPTS_URL = "https://docs.chromia.com/rell/core-concepts"
    const val QUERY_URL = "https://docs.chromia.com/rell/language-features/modules/query"
    const val OPERATION_URL = "https://docs.chromia.com/rell/language-features/modules/operation"
    const val ENTITY_URL = "https://docs.chromia.com/rell/language-features/modules/entity"
    const val OBJECT_URL = "https://docs.chromia.com/rell/language-features/modules/object"
    const val STRUCT_URL = "https://docs.chromia.com/rell/language-features/modules/struct"
    const val ENUM_URL = "https://docs.chromia.com/rell/language-features/modules/enum"
    const val FUNCTION_URL = "https://docs.chromia.com/rell/language-features/modules/function"
    const val MODULES_URL = "https://docs.chromia.com/rell/modules"
    const val MODULES_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/"
    const val NAMESPACE_URL = "https://docs.chromia.com/rell/language-features/modules/namespace"
    const val MOUNT_URL = "https://docs.chromia.com/rell/language-features/modules/mount"
    const val ABSTRACT_URL = "https://docs.chromia.com/rell/language-features/modules/abstract"
    const val SIZE_CONSTRAINT_URL = "https://docs.chromia.com/rell/language-features/modules/size-constraint-annotations"
    const val SIZE_CONSTRAINT_404_URL = "https://docs.chromia.com/rell/language-features/modules/size-constraint"
    const val EXTERNAL_404_URL = "https://docs.chromia.com/rell/language-features/modules/external"
    const val SPECIAL_OPS_URL = "https://docs.chromia.com/rell/special-operations"
    const val RELLDOC_URL = "https://docs.chromia.com/rell/rell-doc"
    const val IDENTIFIERS_URL = "https://docs.chromia.com/rell/language-features/identifiers-syntax"
    const val RELL_LANGUAGE_FEATURES_INDEX_URL = "https://docs.chromia.com/rell/language-features"
    const val RELL_LANGUAGE_FEATURES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/"
    const val RELL_LANGUAGE_FEATURES_INDEX_TITLE = "Rell language reference"  // official H1
    const val WHAT_IS_RELL_INDEX_URL = "https://docs.chromia.com/get-started/about/what-is-rell"
    const val WHAT_IS_RELL_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/about/what-is-rell/"
    const val WHAT_IS_RELL_INDEX_TITLE = "Rell"  // official H1
    const val GET_STARTED_ICMF_USE_CASE_INDEX_URL = "https://docs.chromia.com/get-started/use-cases/cross-chain/icmf"
    const val GET_STARTED_ICMF_USE_CASE_INDEX_URL_SLASH = "https://docs.chromia.com/get-started/use-cases/cross-chain/icmf/"
    const val GET_STARTED_ICMF_USE_CASE_INDEX_TITLE = "Cross-chain communication applications"  // official H1
    const val ECOSYSTEM_PROVIDER_REWARDS_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/rewards"
    const val ECOSYSTEM_PROVIDER_REWARDS_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/rewards/"
    const val ECOSYSTEM_PROVIDER_REWARDS_INDEX_TITLE = "Provider rewards"  // official H1
    const val ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/interact-with-frontend"
    const val ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/interact-with-frontend/"
    const val ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_TITLE = "Interact with the frontend"  // official H1
    const val ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/automated-network-setup"
    const val ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/automated-network-setup/"
    const val ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_TITLE = "Automated network setup"  // official H1
    const val ECOSYSTEM_PMC_ECONOMY_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/economy"
    const val ECOSYSTEM_PMC_ECONOMY_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/economy/"
    const val ECOSYSTEM_PMC_ECONOMY_INDEX_TITLE = "economy"  // official H1
    const val REFERENCE_TERMINOLOGY_INDEX_URL = "https://docs.chromia.com/reference/terminology"
    const val REFERENCE_TERMINOLOGY_INDEX_URL_SLASH = "https://docs.chromia.com/reference/terminology/"
    const val REFERENCE_TERMINOLOGY_INDEX_TITLE = "Terminology"  // official H1
    const val RELL_DATABASE_CREATE_INDEX_URL = "https://docs.chromia.com/rell/language-features/database/create"
    const val RELL_DATABASE_CREATE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/database/create/"
    const val RELL_DATABASE_CREATE_INDEX_TITLE = "Create statement"  // official H1
    const val LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-entity"
    const val LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-entity/"
    const val LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_TITLE = "Lesson 1 - Create your first entity"  // official H1
    const val LEARN_FT4_ASSET_INTRO_INDEX_URL = "https://learn.chromia.com/courses/ft4-asset/introduction"
    const val LEARN_FT4_ASSET_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-asset/introduction/"
    const val LEARN_FT4_ASSET_INTRO_INDEX_TITLE = "Asset management"  // official H1
    const val LEARN_EVM_SUMMARY_INDEX_URL = "https://learn.chromia.com/courses/chromia-for-evm-developers/summary"
    const val LEARN_EVM_SUMMARY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-for-evm-developers/summary/"
    const val LEARN_EVM_SUMMARY_INDEX_TITLE = "Summary"  // official H1
    const val LEARN_FT4_DEMO_FRONTEND_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application"
    const val LEARN_FT4_DEMO_FRONTEND_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/"
    const val LEARN_FT4_DEMO_FRONTEND_INDEX_TITLE = "Module 3 - Frontend application"  // official H1
    const val LEARN_NEWS_MODEL_RELL_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling/model"
    const val LEARN_NEWS_MODEL_RELL_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling/model/"
    const val LEARN_NEWS_MODEL_RELL_INDEX_TITLE = "Implement the model in Rell"  // official H1
    const val LEARN_TTT_INTRO_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/introduction"
    const val LEARN_TTT_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/introduction/"
    const val LEARN_TTT_INTRO_INDEX_TITLE = "Create Tic Tac Toe game on Chromia using Rell and Unity"  // official H1
    const val LEARN_VECTOR_DB_DEEP_DIVE_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive"
    const val LEARN_VECTOR_DB_DEEP_DIVE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive/"
    const val LEARN_VECTOR_DB_DEEP_DIVE_INDEX_TITLE = "Module 4 – Code deep dive"  // official H1
    const val LEARN_ZK_DAPP_ENTITIES_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-entities"
    const val LEARN_ZK_DAPP_ENTITIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-entities/"
    const val LEARN_ZK_DAPP_ENTITIES_INDEX_TITLE = "Dapp entities"  // official H1
    const val LEARN_CHAT_AGENT_TEST_SETUP_INDEX_URL = "https://learn.chromia.com/courses/chat-agent-course/test-your-setup"
    const val LEARN_CHAT_AGENT_TEST_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chat-agent-course/test-your-setup/"
    const val LEARN_CHAT_AGENT_TEST_SETUP_INDEX_TITLE = "Test your setup"  // official H1
    const val LEARN_LKT_INTRO_INDEX_URL = "https://learn.chromia.com/courses/latest-known-time/introduction"
    const val LEARN_LKT_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/latest-known-time/introduction/"
    const val LEARN_LKT_INTRO_INDEX_TITLE = "How to work with timestamps on Chromia using Rell"  // official H1
    const val LEARN_COMPARISONS_SOLANA_INDEX_URL = "https://learn.chromia.com/courses/chromia-comparisons/solana"
    const val LEARN_COMPARISONS_SOLANA_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-comparisons/solana/"
    const val LEARN_COMPARISONS_SOLANA_INDEX_TITLE = "Solana"  // official H1
    const val RELL_MODULE_ENTITY_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/entity"
    const val RELL_MODULE_ENTITY_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/entity/"
    const val RELL_MODULE_ENTITY_INDEX_TITLE = "Entity"  // official H1
    const val RELL_MODULE_QUERY_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/query"
    const val RELL_MODULE_QUERY_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/query/"
    const val RELL_MODULE_QUERY_INDEX_TITLE = "Query"  // official H1
    const val RELL_MODULE_ABSTRACT_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/abstract"
    const val RELL_MODULE_ABSTRACT_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/abstract/"
    const val RELL_MODULE_ABSTRACT_INDEX_TITLE = "Abstract module"  // official H1
    const val RELL_MODULE_OBJECT_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/object"
    const val RELL_MODULE_OBJECT_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/object/"
    const val RELL_MODULE_OBJECT_INDEX_TITLE = "Object"  // official H1
    const val RELL_LANGUAGE_MODULES_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules"
    const val RELL_LANGUAGE_MODULES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/"
    const val RELL_LANGUAGE_MODULES_INDEX_TITLE = "Definitions"  // official H1
    const val LEARN_TAGS_RELL_INDEX_URL = "https://learn.chromia.com/tags/Rell"
    const val LEARN_TAGS_RELL_INDEX_URL_SLASH = "https://learn.chromia.com/tags/Rell/"
    const val LEARN_TAGS_RELL_INDEX_TITLE = "Courses tagged with: Rell"  // official H1
    const val RELEASES_URL = "https://docs.chromia.com/rell/releases"
    const val HELLO_WORLD_URL =
        "https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart"

    fun queryShort(): String = """
        query q(x: integer): integer = x * x;
    """.trimIndent() + "\n"

    fun queryFull(): String = """
        query q(x: integer): integer {
            return x * x;
        }
    """.trimIndent() + "\n"

    fun queryDefaults(): String = """
        query get_users(min_age: integer = 0, max_age: integer = 100): list<user> {
            return user @* { .age >= min_age, .age <= max_age };
        }
    """.trimIndent() + "\n"

    fun operationExample(): String = """
        operation create_user(name: text) {
            create user(name = name);
        }
    """.trimIndent() + "\n"

    fun entityExample(): String = """
        entity company {
            name: text;
            address: text;
        }
        entity user {
            first_name: text;
            last_name: text;
            year_of_birth: integer;
            mutable salary: integer;
        }
    """.trimIndent() + "\n"

    fun objectExample(): String = """
        object event_stats {
            mutable event_count: integer = 0;
            mutable last_event: text = "n/a";
        }
        query get_event_count() = event_stats.event_count;
    """.trimIndent() + "\n"

    fun structExample(): String = """
        struct user {
            name: text;
            address: text;
            mutable balance: integer = 0;
        }
        val u = user(name = 'Bob', address = 'New York');
    """.trimIndent() + "\n"

    fun enumExample(): String = """
        enum currency {
            USD,
            EUR,
            GBP
        }
        print(currency.values()); // [USD, EUR, GBP]
    """.trimIndent() + "\n"

    fun functionShort(): String = """
        function f(x: integer): integer = x * x;
    """.trimIndent() + "\n"

    fun functionFull(): String = """
        function f(x: integer): integer {
            return x * x;
        }
    """.trimIndent() + "\n"

    fun moduleHeader(): String = """
        module;
    """.trimIndent() + "\n"

    fun helloWorldQuery(): String = """
        object my_name {
          mutable name = "World";
        }
        query hello_world() = "Hello %s!".format(my_name.name);
    """.trimIndent() + "\n"

    fun namespaceExample(): String = """
        namespace foo {
            entity user {
                name;
                country;
            }
            struct point {
                x: integer;
                y: integer;
            }
            enum country {
                USA,
                DE,
                FR
            }
        }
        query get_users_by_country(c: foo.country) = foo.user @* { .country == c };
        namespace x.y.z {
            function f() = 123;
        }
        @mount('foo.bar')
        namespace {
            entity user {}
            entity company {}
        }
        import _: foo;
    """.trimIndent() + "\n"

    fun mountExample(): String = """
        @mount('foo.bar.user')
        entity user {}
        @mount('foo.bar')
        namespace ns {
            entity user {}
        }
        @mount('foo.bar')
        module;
        entity user {}
        entity user {
            @mount('fname') first_name: text;
            age: integer;
        }
    """.trimIndent() + "\n"

    fun abstractExample(): String = """
        abstract module;
        abstract function customize(x: integer): text;
        abstract function customize(x: integer): text = "Default";
        override function lib.customize(x: integer): text {
        }
    """.trimIndent() + "\n"

    fun specialOpsExample(): String = """
        operation __begin_block(height: integer) {
            print("Block %s started".format(height));
        }
        operation __end_block(height: integer) {
            print("Block %s completed".format(height));
        }
        @mount('icmf.message')
        operation __icmf_message() {}
        @mount('stork.oracle.prices')
        operation __stork_oracle_prices() {}
    """.trimIndent() + "\n"

    val specialOps = listOf(
        "__begin_block(height: integer)  # on-chain; before regular transactions",
        "__end_block(height: integer)  # on-chain; after regular transactions",
        "__icmf_message  # cross-chain ICMF; parameter list not on the official page",
        "__evm_block  # cross-chain EIF; parameter list not on the official page",
        "__timeb  # on-chain time window; timestamps UTC; parameter list not on the official page"
    )

    fun rellDocExample(): String = """
        /**
         * Authenticates a user in the system.
         *
         * @param username The username to authenticate
         * @return Boolean indicating whether authentication was successful
         * @throws UserNotFound When the username doesn't exist
         * @since 1.10.0
         * @see user_validation For additional validation rules
         * @author Jane Doe
         */
        function authenticate_user(username: text): boolean {
            require(username != "not_found", "UserNotFound: Username not found");
            return true;
        }
    """.trimIndent() + "\n"

    val rellDocTags = listOf(
        "@param parameterName description  # function / operation / query parameter",
        "@return description  # function or query return value",
        "@throws exception description",
        "@see reference description",
        "@since version",
        "@author name"
    )

    val leftoverReleases = listOf(
        "docs-site latest listed 0.16.4 (2026-08-02); language source tag 0.16.7",
        "0.16.7  source notes 2026-08-14 (not on docs-site): smaller dependency footprint; LSP honours compile.rellVersion; rell/setSettingsFiles; more constructs version-restricted",
        "0.16.6  source notes 2026-08-07 (not on docs-site): convert expression/block body; lambdas/value-blocks/jumps now require language version 0.16.1",
        "0.16.5  source notes 2026-08-04 (not on docs-site): linter+formatter edit order; formatter argument-list/chain breaks; rule_simplify_nullable_if",
        "0.16.4  linter rule_prefer_null_check_operator (??) and rule_prefer_at_projection; if-to-when compact arms; formatter keeps doc/EOL/call-chain comments; plain-language errors; diagnostic ranges",
        "0.16.3  linter rule_replace_if_with_when; named quick fixes; disable inspection in .rell_lint; LSP chromiaConfigFiles; formatter keeps binary-operator line breaks",
        "0.16.2  linter inspections on by default; no sun.misc.Unsafe on JDK 25+; SQL generation locale-independent",
        "0.16.1  lambdas; value-block if/when arms; return/break/continue as expressions",
        "0.16.0  resolved runtime model RR_*; compileApp() returns it; SQL on jOOQ; ANTLR mainline parser; GraalVM Truffle peer; BNF facility removed",
        "0.15.4  global system functions moved out of per-chain initializeDB",
        "0.15.3  gtv_type enum and gtv.type; CLI library Picocli to Clikt",
        "0.15.2  snapshots support; dropped attributes compatibility mode for mixed-version nodes",
        "0.15.1  size annotations on entity/object attributes; @mount on attributes; @disabled tests; dropped columns for removed attrs",
        "0.15.0  guard block runs during operation validation; GTV big_integer JSON serialization change",
        "0.14.16  JSON [] / getters; text.regex_replace; try_call_catch and try_call_result; struct.copy(); collection + - &",
        "0.14.15  rell.error()",
        "0.14.14  rell.time; size-constraint on function/query/operation parameters",
        "0.14.13  @singular / @compound; function-level @test; size-constraint on struct attributes",
        "0.14.12  optional struct attributes omittable in op/query calls and from_gtv*",
        "0.14.9  multi-row insert allocates row IDs in bulk",
        "0.14.8  compiler/interpreter backward compatibility for language version 0.10.9",
        "0.14.5  T.hash() uses V1 or V2 from chain merkle_hash_version (official default V1; production pin 2); gtv.legacy_hash(value: T, version: integer)",
        "0.14.3  default params on operations and queries; new params must be at the end",
        "0.14.2  try_call() logs caught exceptions including stack traces",
        "0.14.0  @author / @return (replaces @returns) / @throws",
        "0.13.14  RellDoc /** */; @returns later renamed @return in 0.14.0",
        "0.13.13  rell.get_mount_names / rell.get_module_args",
        "0.13.12  anonymous import _; trailing commas; more reserved names",
        "0.13.11  mount names max 58 characters  # crypto.get_signature skipped (signing)",
        "0.13.10  inner/outer join; nullable comparisons; try_call()",
        "0.13.9  add/remove keys/indexes at DB init; @list @set @map; strict GTV",
        "0.13.5  rell.meta; module_args defaults; bulk create",
        "0.9.0  integer overflow throws; ??; rowid; GTXValue renamed gtv",
        "0.7.0  object; enum; if expression; @log; table names prefixed by blockchain id"
    )

    fun sizeConstraintExample(): String = """
        query get_info(@size(32) pubkey: byte_array) { }
        function print_status(@min_size(1) status: text) { }
        operation register_user(@max_size(50) name) { }
        query q(@size(8, 16) data: byte_array) { }
        entity user {
            id: integer;
            @max_size(50) userName: text;
            @size(32) publicKey: byte_array;
            @min_size(1) status: text;
        }
    """.trimIndent() + "\n"

    fun notes(): String = """
        Official Rell definition syntax for CLI $CLI_SERIES. Rell language source tag $RELL_VERSION (docs may still list 0.16.4 — source wins); the chromia.yml compile.rellVersion pin is ${DappScaffold.RELL_VERSION}.
        Intro: $INTRO_URL
        Core concepts (cardinality @ / @? / @+ / @*): $CORE_CONCEPTS_URL
        Query: $QUERY_URL — cannot modify the database; must return a value; parameter and return types must be GTV-compatible.
        Short form and full form with return are official. Default parameter values are optional for external clients.
        Operation: $OPERATION_URL — can modify the database; does not return a value; parameter types must be GTV-compatible.
        Official operation example is syntax only. This tool does not document chr tx or key generation.
        New operation parameters with defaults must be added at the end of the parameter list.
        Entity: $ENTITY_URL — persistent table. A variable holds the rowid, not attribute values. Implicit rowid primary key.
        key = unique + index. index = non-unique index. @log adds a transaction attribute and makes the entity immutable and non-deletable.
        Entity attributes cannot be nullable (complex-types page). Compatible schema changes: add attributes with defaults; add attributes to empty tables; remove attributes; change mutability when not @log. Incompatible: change attribute type; add or remove @log.
        Object: $OBJECT_URL — singleton; auto-initialized; cannot be created or deleted from code; every attribute requires a default value.
        Struct: $STRUCT_URL — in-memory only. Attributes immutable unless mutable. Construct by calling the struct name.
        Enum: $ENUM_URL — .name: text and .value: integer (declaration index). T.values(), T.value(text), T.value(integer).
        Function: $FUNCTION_URL — short form and full form. No explicit return type means unit.
        Module header: $MODULES_URL — a single-file module starts with module;. Layouts and official import forms: chromia_project_structure_help.
        Definitions index: $MODULES_INDEX_URL
        Namespace: $NAMESPACE_URL — simplified referencing inside the namespace; entity/object tables include the namespace path (foo.user -> c0.foo.user).
        Same namespace name may be defined multiple times. Nested short form: namespace x.y.z { }. Anonymous namespace can apply an annotation to a group.
        Anonymous import import _: foo; activates the module (operations, queries, extensions, overrides) without adding names.
        Mount: $MOUNT_URL — entities/objects (SQL table), operations/queries (external invoke). Default is the fully qualified definition name.
        @mount('desired_mount_name') on entity, object, operation, query. Since 0.15.1, @mount on an entity attribute names the SQL column.
        Namespace/module @mount prefixes inner definitions. Relative shortcuts: . appends; ^ pops the last context part.
        Mount names are set at definition, not at import. Special operations require the __ prefix in the function name
        and a dotted @mount (official examples: @mount('icmf.message') operation __icmf_message(); @mount('stork.oracle.prices') operation __stork_oracle_prices()).
        Do not invent special-operation parameter lists.
        Abstract: $ABSTRACT_URL — abstract module; abstract function with or without a default body; override function in the client module.
        Importing an abstract module requires overriding every abstract function that lacks a body.
        Size-constraint annotations: $SIZE_CONSTRAINT_URL — @size(n) / @size(min, max) / @min_size(n) / @max_size(n)
        on text and byte_array. Official targets: function / query / operation parameters, struct attributes,
        and (since 0.15.1) entity and object attributes. Parameter annotations are checked at the call; defaults at compile time where possible.
        On entity/object attributes they compile to PostgreSQL CHECK and are enforced on create, update, and attribute = / +=.
        Cannot tighten size constraints on a non-empty entity; can relax (increase max or decrease min).
        Official $SIZE_CONSTRAINT_404_URL is 404 — slug is size-constraint-annotations, not size-constraint.
        Official $EXTERNAL_404_URL is 404 — modules index mentions external modules; there is no /modules/external page.
        Special operations: $SPECIAL_OPS_URL — automatically executed at blockchain lifecycle points.
        Official names: __begin_block(height: integer), __end_block(height: integer), __icmf_message, __evm_block, __timeb.
        Parameter lists besides begin/end_block are not on the official page — do not invent them.
        Always @mount with dotted extension names (__ prefix stays on the operation name). Official examples:
        @mount('icmf.message') operation __icmf_message(); @mount('stork.oracle.prices') operation __stork_oracle_prices();
        @mount('bridge.deposit') operation __bridge_deposit();. __timeb requires UTC and a reasonably synced client clock.
        Official Hello World query ($HELLO_WORLD_URL / run-dapp-cli): object my_name { mutable name = "World"; } and
        query hello_world() = "Hello %s!".format(my_name.name); First chr query hello_world returns "Hello World!".
        Identifiers: $IDENTIFIERS_URL — start with _ / A-Z / a-z; then _, letters, digits. Case-sensitive.
        Keywords (function, entity, if, while, …) cannot be identifiers. Names must be unique in a module scope.
        Comments: // rest-of-line and /* block */ (core-concepts). RellDoc: $RELLDOC_URL — /** … */ with leading *.
        Official RellDoc tags: @param, @return, @throws, @see, @since, @author.
        RellDoc comments are processed by chr generate docs-site (chromia_docs_yml_help).
        Official releases: $RELEASES_URL — docs-site latest listed is 0.16.4 (2026-08-02); the language source tag is $RELL_VERSION (source wins). The chromia.yml compile.rellVersion pin stays ${DappScaffold.RELL_VERSION}.
        Source notes (not on the docs-site table): 0.16.5 / 0.16.6 / 0.16.7. Do not invent changelog items.
        0.16.3: rule_replace_if_with_when and LSP chromiaConfigFiles. 0.16.0: RR_* + jOOQ SQL + ANTLR.
        0.14.0 replaced @returns with @return. 0.14.3: new default params at the end (ops and queries).
        0.14.5: T.hash() follows merkle_hash_version (official default V1; production pin must stay 2).
        0.13.11: mount names max 58 chars.
        0.15.1: size annotations + @mount on entity/object attributes; @disabled tests; dropped columns.
        Official signing helper crypto.get_signature (0.13.11) is skipped.
        Do not invent language features, mount names, or GTX class names.
        Official rell/language-features INDEX ($RELL_LANGUAGE_FEATURES_INDEX_URL 307 $RELL_LANGUAGE_FEATURES_INDEX_URL_SLASH 200 $RELL_LANGUAGE_FEATURES_INDEX_TITLE): HELP ONLY Query-only Origin parked WRITE SKIP skip signed txs no invented 64-hex BRIDs no sign recipe.
        Official get-started/about/what-is-rell INDEX ($WHAT_IS_RELL_INDEX_URL 307 $WHAT_IS_RELL_INDEX_URL_SLASH 200 $WHAT_IS_RELL_INDEX_TITLE): HELP ONLY slash title Query-only Origin parked WRITE SKIP skip signed txs no invented 64-hex BRIDs no sign recipe.
        Official get-started/use-cases/cross-chain/icmf INDEX ($GET_STARTED_ICMF_USE_CASE_INDEX_URL 307 $GET_STARTED_ICMF_USE_CASE_INDEX_URL_SLASH 200 $GET_STARTED_ICMF_USE_CASE_INDEX_TITLE): slash title WRITE SKIP HELP ONLY Origin parked Query-only skip signed txs no sample keys no invented 64-hex BRIDs no sign recipe.
        Official ECOSYSTEM ecosystem/providers/rewards INDEX ($ECOSYSTEM_PROVIDER_REWARDS_INDEX_URL 307 $ECOSYSTEM_PROVIDER_REWARDS_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PROVIDER_REWARDS_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/bridge/deploy-bridge/interact-with-frontend INDEX ($ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_URL 307 $ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/nodes/automated-network-setup INDEX ($ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_URL 307 $ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/economy INDEX ($ECOSYSTEM_PMC_ECONOMY_INDEX_URL 307 $ECOSYSTEM_PMC_ECONOMY_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_ECONOMY_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official REFERENCE reference/terminology INDEX ($REFERENCE_TERMINOLOGY_INDEX_URL 307 $REFERENCE_TERMINOLOGY_INDEX_URL_SLASH 200 H1 $REFERENCE_TERMINOLOGY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP. Glossary INDEX terms: Account ID, Auth Descriptor, BRID, Query vs Operation, Rell, FT, ICMF, PMC, Vault.
        Official RELL rell/language-features/database/create INDEX ($RELL_DATABASE_CREATE_INDEX_URL 307 $RELL_DATABASE_CREATE_INDEX_URL_SLASH 200 H1 $RELL_DATABASE_CREATE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/book-entity INDEX ($LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_URL 301 $LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/ft4-asset/introduction INDEX ($LEARN_FT4_ASSET_INTRO_INDEX_URL 301 $LEARN_FT4_ASSET_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_FT4_ASSET_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/chromia-for-evm-developers/summary INDEX ($LEARN_EVM_SUMMARY_INDEX_URL 301 $LEARN_EVM_SUMMARY_INDEX_URL_SLASH 200 H1 $LEARN_EVM_SUMMARY_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/ft4-demo-app/module-frontend-application INDEX ($LEARN_FT4_DEMO_FRONTEND_INDEX_URL 301 $LEARN_FT4_DEMO_FRONTEND_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_FRONTEND_INDEX_TITLE HELP ONLY WRITE SKIP)
        Official LEARN courses/my-news-feed/module-one/data-modeling/model INDEX ($LEARN_NEWS_MODEL_RELL_INDEX_URL 301 $LEARN_NEWS_MODEL_RELL_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_MODEL_RELL_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/modules/entity INDEX ($RELL_MODULE_ENTITY_INDEX_URL 307 $RELL_MODULE_ENTITY_INDEX_URL_SLASH 200 H1 $RELL_MODULE_ENTITY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/tic-tac-toe/introduction INDEX ($LEARN_TTT_INTRO_INDEX_URL 301 $LEARN_TTT_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_TTT_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/vector-db-movie-demo/code-deep-dive INDEX ($LEARN_VECTOR_DB_DEEP_DIVE_INDEX_URL 301 $LEARN_VECTOR_DB_DEEP_DIVE_INDEX_URL_SLASH 200 H1 $LEARN_VECTOR_DB_DEEP_DIVE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/modules/query INDEX ($RELL_MODULE_QUERY_INDEX_URL 307 $RELL_MODULE_QUERY_INDEX_URL_SLASH 200 H1 $RELL_MODULE_QUERY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof dapp-entities ($LEARN_ZK_DAPP_ENTITIES_INDEX_URL 301 $LEARN_ZK_DAPP_ENTITIES_INDEX_URL_SLASH 200 H1 $LEARN_ZK_DAPP_ENTITIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX chat-agent-course test-your-setup ($LEARN_CHAT_AGENT_TEST_SETUP_INDEX_URL 301 $LEARN_CHAT_AGENT_TEST_SETUP_INDEX_URL_SLASH 200 H1 $LEARN_CHAT_AGENT_TEST_SETUP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX latest-known-time introduction ($LEARN_LKT_INTRO_INDEX_URL 301 $LEARN_LKT_INTRO_INDEX_URL_SLASH 200 H1 $LEARN_LKT_INTRO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX chromia-comparisons solana ($LEARN_COMPARISONS_SOLANA_INDEX_URL 301 $LEARN_COMPARISONS_SOLANA_INDEX_URL_SLASH 200 H1 $LEARN_COMPARISONS_SOLANA_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/modules/abstract INDEX ($RELL_MODULE_ABSTRACT_INDEX_URL 307 $RELL_MODULE_ABSTRACT_INDEX_URL_SLASH 200 H1 $RELL_MODULE_ABSTRACT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/modules/object INDEX ($RELL_MODULE_OBJECT_INDEX_URL 307 $RELL_MODULE_OBJECT_INDEX_URL_SLASH 200 H1 $RELL_MODULE_OBJECT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/modules INDEX ($RELL_LANGUAGE_MODULES_INDEX_URL 307 $RELL_LANGUAGE_MODULES_INDEX_URL_SLASH 200 H1 $RELL_LANGUAGE_MODULES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN tags/Rell INDEX ($LEARN_TAGS_RELL_INDEX_URL 301 $LEARN_TAGS_RELL_INDEX_URL_SLASH 200 H1 $LEARN_TAGS_RELL_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        This tool does not run chr, does not generate a key, and does not send signed transactions.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("rell", RELL_VERSION)
        put("rellSourceTag", RELL_VERSION)
        put("rellVersionPin", DappScaffold.RELL_VERSION)
        put("tool", TOOL_NAME)
        put("docs", INTRO_URL)
        put("query_docs", QUERY_URL)
        put("operation_docs", OPERATION_URL)
        put("entity_docs", ENTITY_URL)
        put("object_docs", OBJECT_URL)
        put("struct_docs", STRUCT_URL)
        put("enum_docs", ENUM_URL)
        put("function_docs", FUNCTION_URL)
        put("modules_docs", MODULES_URL)
        put("namespace_docs", NAMESPACE_URL)
        put("mount_docs", MOUNT_URL)
        put("abstract_docs", ABSTRACT_URL)
        put("size_constraint_docs", SIZE_CONSTRAINT_URL)
        put("special_ops_docs", SPECIAL_OPS_URL)
        put("relldoc_docs", RELLDOC_URL)
        put("identifiers_docs", IDENTIFIERS_URL)
        put("rell_language_features_index_docs", RELL_LANGUAGE_FEATURES_INDEX_URL)
        put("rell_language_features_index_url_slash", RELL_LANGUAGE_FEATURES_INDEX_URL_SLASH)
        put("rell_language_features_index_title", RELL_LANGUAGE_FEATURES_INDEX_TITLE)
        put("what_is_rell_index_docs", WHAT_IS_RELL_INDEX_URL)
        put("what_is_rell_index_url_slash", WHAT_IS_RELL_INDEX_URL_SLASH)
        put("what_is_rell_index_title", WHAT_IS_RELL_INDEX_TITLE)
        put("get_started_icmf_use_case_index_docs", GET_STARTED_ICMF_USE_CASE_INDEX_URL)
        put("get_started_icmf_use_case_index_url_slash", GET_STARTED_ICMF_USE_CASE_INDEX_URL_SLASH)
        put("get_started_icmf_use_case_index_title", GET_STARTED_ICMF_USE_CASE_INDEX_TITLE)
        put("ecosystem_provider_rewards_index_url_slash", ECOSYSTEM_PROVIDER_REWARDS_INDEX_URL_SLASH)
        put("ecosystem_provider_rewards_index_title", ECOSYSTEM_PROVIDER_REWARDS_INDEX_TITLE)
        put("ecosystem_interact_with_frontend_index_url_slash", ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_URL_SLASH)
        put("ecosystem_interact_with_frontend_index_title", ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_TITLE)
        put("ecosystem_automated_network_setup_index_url_slash", ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_URL_SLASH)
        put("ecosystem_automated_network_setup_index_title", ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_TITLE)
        put("ecosystem_pmc_economy_index_url_slash", ECOSYSTEM_PMC_ECONOMY_INDEX_URL_SLASH)
        put("ecosystem_pmc_economy_index_title", ECOSYSTEM_PMC_ECONOMY_INDEX_TITLE)
        put("reference_terminology_index_url_slash", REFERENCE_TERMINOLOGY_INDEX_URL_SLASH)
        put("reference_terminology_index_title", REFERENCE_TERMINOLOGY_INDEX_TITLE)
        put("rell_database_create_index_url_slash", RELL_DATABASE_CREATE_INDEX_URL_SLASH)
        put("rell_database_create_index_title", RELL_DATABASE_CREATE_INDEX_TITLE)
        put("learn_book_review_book_entity_index_url_slash", LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_URL_SLASH)
        put("learn_book_review_book_entity_index_title", LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_TITLE)
        put("learn_ft4_asset_intro_index_url_slash", LEARN_FT4_ASSET_INTRO_INDEX_URL_SLASH)
        put("learn_ft4_asset_intro_index_title", LEARN_FT4_ASSET_INTRO_INDEX_TITLE)
        put("learn_evm_summary_index_url_slash", LEARN_EVM_SUMMARY_INDEX_URL_SLASH)
        put("learn_evm_summary_index_title", LEARN_EVM_SUMMARY_INDEX_TITLE)
        put("learn_ft4_demo_frontend_index_url_slash", LEARN_FT4_DEMO_FRONTEND_INDEX_URL_SLASH)
        put("learn_ft4_demo_frontend_index_title", LEARN_FT4_DEMO_FRONTEND_INDEX_TITLE)
        put("releases_docs", RELEASES_URL)
        put("project_structure_help", ChromiaProjectStructureHelp.TOOL_NAME)
        put("query_short", queryShort())
        put("query_full", queryFull())
        put("query_defaults", queryDefaults())
        put("operation_example", operationExample())
        put("entity_example", entityExample())
        put("object_example", objectExample())
        put("struct_example", structExample())
        put("enum_example", enumExample())
        put("function_short", functionShort())
        put("function_full", functionFull())
        put("module_header", moduleHeader())
        put("hello_world_query", helloWorldQuery())
        put("hello_world_result", "Hello World!")
        put("namespace_example", namespaceExample())
        put("mount_example", mountExample())
        put("abstract_example", abstractExample())
        put("size_constraint_example", sizeConstraintExample())
        put("special_ops", buildJsonArray { specialOps.forEach { add(JsonPrimitive(it)) } })
        put("special_ops_example", specialOpsExample())
        put("rell_doc_example", rellDocExample())
        put("rell_doc_tags", buildJsonArray { rellDocTags.forEach { add(JsonPrimitive(it)) } })
        put("leftover_releases", buildJsonArray { leftoverReleases.forEach { add(JsonPrimitive(it)) } })
        put(
            "skipped_404",
            buildJsonArray {
                add(JsonPrimitive("$SIZE_CONSTRAINT_404_URL (404; official slug is $SIZE_CONSTRAINT_URL)"))
                add(JsonPrimitive("$EXTERNAL_404_URL (404; no official /modules/external page)"))
            }
        )
        put("learn_news_model_rell_index_url_slash", LEARN_NEWS_MODEL_RELL_INDEX_URL_SLASH)
        put("learn_news_model_rell_index_title", LEARN_NEWS_MODEL_RELL_INDEX_TITLE)
        put("rell_module_entity_index_url_slash", RELL_MODULE_ENTITY_INDEX_URL_SLASH)
        put("rell_module_entity_index_title", RELL_MODULE_ENTITY_INDEX_TITLE)
        put("learn_ttt_intro_index_url_slash", LEARN_TTT_INTRO_INDEX_URL_SLASH)
        put("learn_ttt_intro_index_title", LEARN_TTT_INTRO_INDEX_TITLE)
        put("learn_vector_db_deep_dive_index_url_slash", LEARN_VECTOR_DB_DEEP_DIVE_INDEX_URL_SLASH)
        put("learn_vector_db_deep_dive_index_title", LEARN_VECTOR_DB_DEEP_DIVE_INDEX_TITLE)
        put("rell_module_query_index_url_slash", RELL_MODULE_QUERY_INDEX_URL_SLASH)
        put("rell_module_query_index_title", RELL_MODULE_QUERY_INDEX_TITLE)
        put("learn_zk_dapp_entities_index_url_slash", LEARN_ZK_DAPP_ENTITIES_INDEX_URL_SLASH)
        put("learn_zk_dapp_entities_index_title", LEARN_ZK_DAPP_ENTITIES_INDEX_TITLE)
        put("learn_chat_agent_test_setup_index_url_slash", LEARN_CHAT_AGENT_TEST_SETUP_INDEX_URL_SLASH)
        put("learn_chat_agent_test_setup_index_title", LEARN_CHAT_AGENT_TEST_SETUP_INDEX_TITLE)
        put("learn_lkt_intro_index_url_slash", LEARN_LKT_INTRO_INDEX_URL_SLASH)
        put("learn_lkt_intro_index_title", LEARN_LKT_INTRO_INDEX_TITLE)
        put("learn_comparisons_solana_index_url_slash", LEARN_COMPARISONS_SOLANA_INDEX_URL_SLASH)
        put("learn_comparisons_solana_index_title", LEARN_COMPARISONS_SOLANA_INDEX_TITLE)
        put("rell_module_abstract_index_url_slash", RELL_MODULE_ABSTRACT_INDEX_URL_SLASH)
        put("rell_module_abstract_index_title", RELL_MODULE_ABSTRACT_INDEX_TITLE)
        put("rell_module_object_index_url_slash", RELL_MODULE_OBJECT_INDEX_URL_SLASH)
        put("rell_module_object_index_title", RELL_MODULE_OBJECT_INDEX_TITLE)
        put("rell_language_modules_index_url_slash", RELL_LANGUAGE_MODULES_INDEX_URL_SLASH)
        put("rell_language_modules_index_title", RELL_LANGUAGE_MODULES_INDEX_TITLE)
        put("learn_tags_rell_index_url_slash", LEARN_TAGS_RELL_INDEX_URL_SLASH)
        put("learn_tags_rell_index_title", LEARN_TAGS_RELL_INDEX_TITLE)
        put("notes", notes())
    }
}
// Official rell/language-features INDEX leftovers encoded as RELL_LANGUAGE_FEATURES_INDEX_* (query-only).
// Official get-started/about/what-is-rell INDEX leftovers encoded as WHAT_IS_RELL_INDEX_* (query-only).
// Official get-started/use-cases/cross-chain/icmf INDEX leftovers encoded as GET_STARTED_ICMF_USE_CASE_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/rewards INDEX leftovers encoded as ECOSYSTEM_PROVIDER_REWARDS_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/bridge/deploy-bridge/interact-with-frontend INDEX leftovers encoded as ECOSYSTEM_INTERACT_WITH_FRONTEND_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/nodes/automated-network-setup INDEX leftovers encoded as ECOSYSTEM_AUTOMATED_NETWORK_SETUP_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/economy INDEX leftovers encoded as ECOSYSTEM_PMC_ECONOMY_INDEX_* (query-only HELP ONLY).
// Official REFERENCE reference/terminology INDEX leftovers encoded as REFERENCE_TERMINOLOGY_INDEX_* (query-only HELP ONLY).
// Official RELL rell/language-features/database/create INDEX leftovers encoded as RELL_DATABASE_CREATE_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/book-review/book-entity INDEX leftovers encoded as LEARN_BOOK_REVIEW_BOOK_ENTITY_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/ft4-asset/introduction INDEX leftovers encoded as LEARN_FT4_ASSET_INTRO_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/chromia-for-evm-developers/summary INDEX leftovers encoded as LEARN_EVM_SUMMARY_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/ft4-demo-app/module-frontend-application INDEX leftovers encoded as LEARN_FT4_DEMO_FRONTEND_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/my-news-feed/module-one/data-modeling/model INDEX leftovers encoded as LEARN_NEWS_MODEL_RELL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/modules/entity INDEX leftovers encoded as RELL_MODULE_ENTITY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/introduction INDEX leftovers encoded as LEARN_TTT_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/vector-db-movie-demo/code-deep-dive INDEX leftovers encoded as LEARN_VECTOR_DB_DEEP_DIVE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/modules/query INDEX leftovers encoded as RELL_MODULE_QUERY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof dapp-entities leftovers encoded as LEARN_ZK_DAPP_ENTITIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX chat-agent-course test-your-setup leftovers encoded as LEARN_CHAT_AGENT_TEST_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX latest-known-time introduction leftovers encoded as LEARN_LKT_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX chromia-comparisons solana leftovers encoded as LEARN_COMPARISONS_SOLANA_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/modules/abstract INDEX leftovers encoded as RELL_MODULE_ABSTRACT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/modules/object INDEX leftovers encoded as RELL_MODULE_OBJECT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/modules INDEX leftovers encoded as RELL_LANGUAGE_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN tags/Rell INDEX leftovers encoded as LEARN_TAGS_RELL_INDEX_* (query-only HELP ONLY WRITE SKIP).
