package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Rell system library help (index + global functions + require +
 * system entities + system queries + official namespace pages).
 * Quotes docs.chromia.com/rell systemlib pages only.
 * Official rell.time slug is /namespaces/time (200); /namespaces/rell.time is 404.
 * Crypto: HASH and VERIFY only. Skips privkey / signing helpers and official printed sample keys.
 * Rell language source tag 0.16.7 (docs examples may show 0.10.1).
 */
object ChromiaRellSystemlibHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val RELL_VERSION = DappScaffold.RELL_SOURCE_TAG
    const val TOOL_NAME = "chromia_rell_systemlib_help"
    const val INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/"
    const val GLOBAL_URL = "https://docs.chromia.com/rell/language-features/systemlib/global-functions"
    const val REQUIRE_URL = "https://docs.chromia.com/rell/language-features/systemlib/require-function"
    const val ENTITIES_URL = "https://docs.chromia.com/rell/language-features/systemlib/system-entities"
    const val QUERIES_URL = "https://docs.chromia.com/rell/language-features/systemlib/system-queries"
    const val NAMESPACES_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/"
    const val CHAIN_CONTEXT_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context"
    const val OP_CONTEXT_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context"
    const val CRYPTO_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/crypto"
    const val META_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/meta"
    const val TIME_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/time"
    const val TIME_404_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/rell.time"
    const val PAGES_RELL_404_URL = "https://docs.chromia.com/pages/rell/"
    const val PAGES_RELL_SYSLIB_404_URL = "https://docs.chromia.com/pages/rell-syslib/"
    const val ECOSYSTEM_BRIDGE_LEASE_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-lease"
    const val ECOSYSTEM_BRIDGE_LEASE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/bridge-lease/"
    const val ECOSYSTEM_BRIDGE_LEASE_INDEX_TITLE = "Lease a bridge"  // official H1
    const val ECOSYSTEM_MASS_EXIT_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/mass-exit"
    const val ECOSYSTEM_MASS_EXIT_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/mass-exit/"
    const val ECOSYSTEM_MASS_EXIT_INDEX_TITLE = "Mass exit"  // official H1
    const val ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/economy-chain-config"
    const val ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/economy-chain-config/"
    const val ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_TITLE = "Configure Economy Chain"  // official H1
    const val ECOSYSTEM_PMC_LEASE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/lease"
    const val ECOSYSTEM_PMC_LEASE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/lease/"
    const val ECOSYSTEM_PMC_LEASE_INDEX_TITLE = "lease"  // official H1
    const val LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_URL = "https://learn.chromia.com/courses/rell-masterclass/entities"
    const val LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-masterclass/entities/"
    const val LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_TITLE = "SQL table vs Rell entity"  // official H1
    const val LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_URL = "https://learn.chromia.com/courses/iccf-course/digital-warehouse-chain"
    const val LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/iccf-course/digital-warehouse-chain/"
    const val LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_TITLE = "Digital warehouse chain"  // official H1
    const val LEARN_NEWS_MODULE_ONE_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one"
    const val LEARN_NEWS_MODULE_ONE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/"
    const val LEARN_NEWS_MODULE_ONE_INDEX_TITLE = "Module 1 - Create a Rell backend app with FT accounts"  // official H1
    const val LEARN_NEWS_RELL_MODULES_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/project-structure/modules"
    const val LEARN_NEWS_RELL_MODULES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/project-structure/modules/"
    const val LEARN_NEWS_RELL_MODULES_INDEX_TITLE = "Work with Rell modules"  // official H1
    const val LEARN_TTT_MODULE_TWO_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-two"
    const val LEARN_TTT_MODULE_TWO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-two/"
    const val LEARN_TTT_MODULE_TWO_INDEX_TITLE = "Module 2 - Unity project"  // official H1
    const val LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/finalize-python-env"
    const val LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/setup/finalize-python-env/"
    const val LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_TITLE = "Finalize your Python environment"  // official H1
    const val LEARN_ZK_DAPP_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp"
    const val LEARN_ZK_DAPP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/"
    const val LEARN_ZK_DAPP_INDEX_TITLE = "Module 2 – Dapp"  // official H1
    const val LEARN_ZK_CIRCOM_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits"
    const val LEARN_ZK_CIRCOM_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/"
    const val LEARN_ZK_CIRCOM_INDEX_TITLE = "Module 1 – Circom circuits"  // official H1
    const val LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_URL = "https://learn.chromia.com/courses/relationships-course/one-to-many"
    const val LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_URL_SLASH = "https://learn.chromia.com/courses/relationships-course/one-to-many/"
    const val LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_TITLE = "One-to-many relationships"  // official H1
    const val LEARN_WEB3_BENEFITS_INDEX_URL = "https://learn.chromia.com/courses/web3-for-web2-devs/web3-benefits"
    const val LEARN_WEB3_BENEFITS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/web3-for-web2-devs/web3-benefits/"
    const val LEARN_WEB3_BENEFITS_INDEX_TITLE = "Benefits and challenges of Web3"  // official H1
    const val RELL_SYSTEMLIB_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib"
    const val RELL_SYSTEMLIB_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/"
    const val RELL_SYSTEMLIB_INDEX_TITLE = "System library"  // official H1
    const val RELL_SYSTEMLIB_GLOBAL_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/global-functions"
    const val RELL_SYSTEMLIB_GLOBAL_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/global-functions/"
    const val RELL_SYSTEMLIB_GLOBAL_INDEX_TITLE = "Global functions"  // official H1
    const val RELL_SYSTEMLIB_NAMESPACES_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces"
    const val RELL_SYSTEMLIB_NAMESPACES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/"
    const val RELL_SYSTEMLIB_NAMESPACES_INDEX_TITLE = "Namespaces"  // official H1
    const val RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context"
    const val RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context/"
    const val RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_TITLE = "chain_context"  // official H1
    const val RELL_SYSTEMLIB_ENTITIES_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/system-entities"
    const val RELL_SYSTEMLIB_ENTITIES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/system-entities/"
    const val RELL_SYSTEMLIB_ENTITIES_INDEX_TITLE = "System entities"  // official H1

    val pages = listOf(
        INDEX_URL, GLOBAL_URL, REQUIRE_URL, ENTITIES_URL, QUERIES_URL,
        NAMESPACES_URL, CHAIN_CONTEXT_URL, OP_CONTEXT_URL, CRYPTO_URL, META_URL, TIME_URL
    )

    val globalFunctions = listOf(
        "abs / min / max  # integer, big_integer, decimal",
        "empty / exists  # nullable, list, set, map; nested @* is one SQL query",
        "print / log",
        "sha256 / keccak256",
        "verify_signature",
        "eth_ecrecover",
        "require / require_not_empty / rell.error / try_call / try_call_catch  # require-function page"
    )

    val systemQueries = listOf(
        "rell.get_rell_version(): text",
        "rell.get_postchain_version(): text",
        "rell.get_build(): text",
        "rell.get_build_details(): map<text, text>",
        "rell.get_app_structure(): map<text, gtv>",
        "rell.get_mount_names(kinds: list, modules: list): map<text, list>",
        "rell.get_module_args(modules: list): map<text, map<text, gtv>>"
    )

    val chainContextMembers = listOf(
        "chain_context.args  # this module's module_args struct from chromia.yml moduleArgs",
        "chain_context.blockchain_rid: byte_array",
        "chain_context.raw_config: gtv  # e.g. {\"gtx\":{\"rell\":{\"mainFile\":\"main.rell\"}}}"
    )

    val opContextMembers = listOf(
        "op_context.block_height  # height of the block being built",
        "op_context.last_block_time  # timestamp of the last committed block, milliseconds",
        "op_context.transaction  # the transaction being built",
        "op_context.exists  # running in an operation context?",
        "op_context.op_index  # index of this operation in the tx",
        "op_context.get_signers()  # all signing pubkeys",
        "op_context.is_signer  # did this key sign?",
        "op_context.get_all_operations()  # all ops in the current tx",
        "op_context.get_current_operation()  # this op",
        "op_context.emit_event  # emit to Postchain components"
    )

    val cryptoHashVerify = listOf(
        "crypto.sha256 / crypto.keccak256  # 32-byte hashes; global sha256 / keccak256 also exist",
        "crypto.verify_signature  # verify a signature against message and public key"
    )

    val metaMembers = listOf(
        "rell.meta(definition)  # constructor; definition is entity, object, operation, or query",
        "simple_name: text  # last segment (bar of lib:foo.bar)",
        "full_name: text  # <module>:[<ns>.]<simple>; root module is :foo",
        "module_name: text  # empty string for the root module",
        "mount_name: text  # mount name; runtime error if the definition has none"
    )

    val timeSpecifiers = listOf("y", "M", "w", "W", "D", "d", "E", "u", "a", "H", "h", "m", "s", "S")

    val timeMembers = listOf(
        "rell.time.format(pattern: text)  # constructor; available from Rell 0.14.14",
        ".ms_to_text(ms: integer): text",
        ".text_to_ms(text): integer  # throws on failure",
        ".text_to_ms_or_null(text): integer?",
        ".to_text(): text  # returns the format pattern",
        "rell.time.ms_to_text(pattern, ms): text",
        "rell.time.text_to_ms(pattern, text): integer",
        "rell.time.text_to_ms_or_null(pattern, text): integer?"
    )

    fun emptyExistsExample(): String = """
        user @* {
            empty(company @* { .city == user.city })
        }
        user @* {
            exists(company @* { .city == user.city })
        }
    """.trimIndent() + "\n"

    fun requireExample(): String = """
        val x: integer? = calculate();
        val y = require(x, "x is null");
        require_not_empty(p, "List is empty");
        rell.error('Not allowed.');
    """.trimIndent() + "\n"

    fun systemEntitiesExample(): String = """
        entity block {
            block_height: integer;
            block_rid: byte_array;
            timestamp;
        }
        entity transaction {
            tx_rid: byte_array;
            tx_hash: byte_array;
            tx_data: byte_array;
            block;
        }
        query get_transactions() {
            return transaction @* { } ( gtx_transaction.from_bytes(.tx_data) );
        }
    """.trimIndent() + "\n"

    fun chainContextExample(): String = """
        struct module_args {
            name: text;
            age: integer;
        }
        function f() {
            print(chain_context.args.name);
            print(chain_context.args.age);
        }
    """.trimIndent() + "\n"

    fun chainContextYamlExample(): String = """
        blockchains:
          module-args-example:
            module: example
            moduleArgs:
              example:
                name: Alice
                age: 46
    """.trimIndent() + "\n"

    fun opContextExample(): String = """
        operation log_operation() {
            print("Block height: %d".format(op_context.block_height));
            print("Operation index: %d".format(op_context.op_index));
            print("Number of signers: %d".format(op_context.get_signers().size()));
            print("Last block time: %d".format(op_context.last_block_time));
        }
        operation process_batch() {
            val all_ops = op_context.get_all_operations();
            for (op in all_ops) {
                print("Operation: %s".format(op.name));
            }
        }
    """.trimIndent() + "\n"

    fun cryptoVerifyExample(): String = """
        val ok = crypto.verify_signature(message, pubkey, signature);
    """.trimIndent() + "\n"

    fun metaExample(): String = """
        operation my_op() {}
        query get_op_name() = rell.meta(my_op).mount_name;
    """.trimIndent() + "\n"

    fun timeExample(): String = """
        rell.time.format('yyyy.MM.dd \'at\' HH:mm:ss').text_to_ms('2001.07.04 at 11:08:56');
        rell.time.format('h:mm a').text_to_ms('11:08 AM');
        val ms: integer = 994244936235;
        rell.time.format('hh:mm a').ms_to_text(ms);
        rell.time.format('yyyy-MM-dd\'T\'HH:mm:ss.SSS').ms_to_text(ms);
        rell.time.ms_to_text(pattern, ms);
        rell.time.text_to_ms(pattern, text);
        rell.time.text_to_ms_or_null(pattern, text);
    """.trimIndent() + "\n"

    fun notes(): String = """
        Official Rell systemlib pages for CLI $CLI_SERIES. Rell language source tag $RELL_VERSION (docs examples may show 0.10.1 — source wins); the chromia.yml compile.rellVersion pin is ${DappScaffold.RELL_VERSION}.
        Index: $INDEX_URL
        Global functions: $GLOBAL_URL
        require / error handling: $REQUIRE_URL
        System entities: $ENTITIES_URL
        System queries: $QUERIES_URL
        Namespaces index: $NAMESPACES_URL
        Official global functions (no namespace prefix): abs, min, max, empty, exists, print, log,
        sha256, keccak256, verify_signature, eth_ecrecover, plus require-page functions.
        empty() / exists() with nested @* compile to a single SQL query; @ / @? / @+ become separate queries.
        require(boolean, text) throws if false. require(T?, text): T throws if null.
        require_not_empty on T? / list / set / map throws if null or empty and otherwise returns the value.
        rell.error() / rell.error(text) (since 0.14.15) fails unconditionally and ends control flow like return.
        try_call catches all exceptions (optional fallback). try_call_catch (0.14.16) catches require exceptions only.
        Both restore database state on failure (writes rolled back).
        System entities block and transaction are immutable; cannot create / modify / delete them in code.
        Official fields: block.block_height, block.block_rid, block.timestamp; transaction.tx_rid, tx_hash, tx_data, block.
        tx_data is serialized GTX; official decode: gtx_transaction.from_bytes(.tx_data) (query example on the entities page).
        System queries are read-only inspection. get_mount_names kinds: "query", "operation", "entity", "object" (empty list = all).
        Return keys are pluralized: "queries", "operations", "entities", "objects". Invalid kind/module name throws; unknown module is ignored.
        Docs examples on the queries page may show Rell 0.10.1 — the current language source tag is $RELL_VERSION.
        chain_context ($CHAIN_CONTEXT_URL): args is this module's module_args struct from chromia.yml moduleArgs.
        Access to args is only possible if that module defines struct module_args. Defaults may be omitted in YAML.
        If every attribute has a default, the args section may be omitted. Every module can have its own module_args.
        blockchain_rid is a byte_array. raw_config is a GTV blockchain configuration.
        op_context ($OP_CONTEXT_URL): only in an operation or a function called from an operation — not in a query.
        Use op_context.exists to test. Do not read op_context.transaction.block except block_height — other block
        attributes are null during the building block and throw. Use block_height and last_block_time.
        crypto ($CRYPTO_URL): HASH and VERIFY only — crypto.sha256, crypto.keccak256, crypto.verify_signature.
        Functions that accept a public key accept compressed 33-byte, uncompressed 65-byte, and 64-byte (eth_ecrecover).
        rell.meta ($META_URL): rell.meta(definition) for entity, object, operation, or query. Fields: simple_name,
        full_name, module_name, mount_name.
        rell.time ($TIME_URL, since 0.14.14): UTC format/parse. Central type rell.time.format. Unix timestamps are milliseconds.
        Specifiers: y M w W D d E u a H h m s S. Quoted literals '...'; escape a quote as ''.
        Official $TIME_404_URL is 404 — canonical slug is /namespaces/time (title is rell.time).
        Skipped: privkey_to_pubkey / get_signature / eth_sign / eth_privkey_to_address (key/signing helpers).
        Skipped: official printed sample keys on the crypto and op_context pages.
        Generated $PAGES_RELL_404_URL and $PAGES_RELL_SYSLIB_404_URL are 404; use these narrative pages.
        Types: chromia_rell_types_help. Expressions: chromia_rell_expressions_help.
        Official ECOSYSTEM ecosystem/bridge/bridge-lease INDEX ($ECOSYSTEM_BRIDGE_LEASE_INDEX_URL 307 $ECOSYSTEM_BRIDGE_LEASE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_BRIDGE_LEASE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/bridge/mass-exit INDEX ($ECOSYSTEM_MASS_EXIT_INDEX_URL 307 $ECOSYSTEM_MASS_EXIT_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_MASS_EXIT_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/nodes/economy-chain-config INDEX ($ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_URL 307 $ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/lease INDEX ($ECOSYSTEM_PMC_LEASE_INDEX_URL 307 $ECOSYSTEM_PMC_LEASE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_LEASE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/rell-masterclass/entities INDEX ($LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_URL 301 $LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_URL_SLASH 200 H1 $LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_TITLE HELP ONLY WRITE SKIP). We start by looking at how to define an entity and what the corresponding SQL table will look like An entity is represented by a SQL table and is defined using the keyword entity Each attribute represents a column of the SQL table. Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/iccf-course/digital-warehouse-chain INDEX ($LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_URL 301 $LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_URL_SLASH 200 H1 $LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one INDEX ($LEARN_NEWS_MODULE_ONE_INDEX_URL 301 $LEARN_NEWS_MODULE_ONE_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_MODULE_ONE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/project-structure/modules INDEX ($LEARN_NEWS_RELL_MODULES_INDEX_URL 301 $LEARN_NEWS_RELL_MODULES_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_RELL_MODULES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib INDEX ($RELL_SYSTEMLIB_INDEX_URL 307 $RELL_SYSTEMLIB_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/tic-tac-toe/module-two INDEX ($LEARN_TTT_MODULE_TWO_INDEX_URL 301 $LEARN_TTT_MODULE_TWO_INDEX_URL_SLASH 200 H1 $LEARN_TTT_MODULE_TWO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX vector-db finalize-python-env ($LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_URL 301 $LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_URL_SLASH GET 200 H1 $LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof dapp ($LEARN_ZK_DAPP_INDEX_URL 301 $LEARN_ZK_DAPP_INDEX_URL_SLASH GET 200 H1 $LEARN_ZK_DAPP_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/global-functions INDEX ($RELL_SYSTEMLIB_GLOBAL_INDEX_URL 307 $RELL_SYSTEMLIB_GLOBAL_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_GLOBAL_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX zero-knowledge-proof circom-circuits ($LEARN_ZK_CIRCOM_INDEX_URL 301 $LEARN_ZK_CIRCOM_INDEX_URL_SLASH GET 200 H1 $LEARN_ZK_CIRCOM_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX relationships-course one-to-many ($LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_URL 301 $LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_URL_SLASH GET 200 H1 $LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN INDEX web3-for-web2-devs web3-benefits ($LEARN_WEB3_BENEFITS_INDEX_URL 301 $LEARN_WEB3_BENEFITS_INDEX_URL_SLASH GET 200 H1 $LEARN_WEB3_BENEFITS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/namespaces INDEX ($RELL_SYSTEMLIB_NAMESPACES_INDEX_URL 307 $RELL_SYSTEMLIB_NAMESPACES_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_NAMESPACES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/namespaces/chain_context INDEX ($RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_URL 307 $RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/system-entities INDEX ($RELL_SYSTEMLIB_ENTITIES_INDEX_URL 307 $RELL_SYSTEMLIB_ENTITIES_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_ENTITIES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
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
        put("docs", INDEX_URL)
        put("global_docs", GLOBAL_URL)
        put("require_docs", REQUIRE_URL)
        put("entities_docs", ENTITIES_URL)
        put("queries_docs", QUERIES_URL)
        put("namespaces_docs", NAMESPACES_URL)
        put("chain_context_docs", CHAIN_CONTEXT_URL)
        put("op_context_docs", OP_CONTEXT_URL)
        put("crypto_docs", CRYPTO_URL)
        put("meta_docs", META_URL)
        put("time_docs", TIME_URL)
        put("namespaces_expanded", true)
        put("pages", buildJsonArray { pages.forEach { add(JsonPrimitive(it)) } })
        put("global_functions", buildJsonArray { globalFunctions.forEach { add(JsonPrimitive(it)) } })
        put("system_queries", buildJsonArray { systemQueries.forEach { add(JsonPrimitive(it)) } })
        put("chain_context_members", buildJsonArray { chainContextMembers.forEach { add(JsonPrimitive(it)) } })
        put("op_context_members", buildJsonArray { opContextMembers.forEach { add(JsonPrimitive(it)) } })
        put("crypto_hash_verify", buildJsonArray { cryptoHashVerify.forEach { add(JsonPrimitive(it)) } })
        put("meta_members", buildJsonArray { metaMembers.forEach { add(JsonPrimitive(it)) } })
        put("time_specifiers", buildJsonArray { timeSpecifiers.forEach { add(JsonPrimitive(it)) } })
        put("time_members", buildJsonArray { timeMembers.forEach { add(JsonPrimitive(it)) } })
        put("empty_exists_example", emptyExistsExample())
        put("require_example", requireExample())
        put("system_entities_example", systemEntitiesExample())
        put("chain_context_example", chainContextExample())
        put("chain_context_yaml_example", chainContextYamlExample())
        put("op_context_example", opContextExample())
        put("crypto_verify_example", cryptoVerifyExample())
        put("meta_example", metaExample())
        put("time_example", timeExample())
        put(
            "skipped",
            buildJsonArray {
                add(JsonPrimitive("privkey_to_pubkey / get_signature / eth_sign / eth_privkey_to_address"))
                add(JsonPrimitive("official printed sample keys on crypto / op_context pages"))
                add(JsonPrimitive("$TIME_404_URL (404; official rell.time slug is $TIME_URL)"))
                add(JsonPrimitive("https://docs.chromia.com/rell/language-features/systemlib/namespaces/gtx (404)"))
                add(JsonPrimitive("https://docs.chromia.com/rell/language-features/systemlib/namespaces/gtv (404)"))
                add(JsonPrimitive("https://docs.chromia.com/rell/language-features/systemlib/namespaces/test (404)"))
                add(JsonPrimitive("$PAGES_RELL_404_URL and $PAGES_RELL_SYSLIB_404_URL (404)"))
            }
        )
        put("types_help", ChromiaRellTypesHelp.TOOL_NAME)
        put("ecosystem_bridge_lease_index_url_slash", ECOSYSTEM_BRIDGE_LEASE_INDEX_URL_SLASH)
        put("ecosystem_bridge_lease_index_title", ECOSYSTEM_BRIDGE_LEASE_INDEX_TITLE)
        put("ecosystem_mass_exit_index_url_slash", ECOSYSTEM_MASS_EXIT_INDEX_URL_SLASH)
        put("ecosystem_mass_exit_index_title", ECOSYSTEM_MASS_EXIT_INDEX_TITLE)
        put("ecosystem_economy_chain_config_index_url_slash", ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_URL_SLASH)
        put("ecosystem_economy_chain_config_index_title", ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_TITLE)
        put("ecosystem_pmc_lease_index_url_slash", ECOSYSTEM_PMC_LEASE_INDEX_URL_SLASH)
        put("ecosystem_pmc_lease_index_title", ECOSYSTEM_PMC_LEASE_INDEX_TITLE)
        put("learn_rell_masterclass_entities_index_url_slash", LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_URL_SLASH)
        put("learn_rell_masterclass_entities_index_title", LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_TITLE)
        put("learn_iccf_warehouse_chain_index_url_slash", LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_URL_SLASH)
        put("learn_iccf_warehouse_chain_index_title", LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_TITLE)
        put("learn_news_module_one_index_url_slash", LEARN_NEWS_MODULE_ONE_INDEX_URL_SLASH)
        put("learn_news_module_one_index_title", LEARN_NEWS_MODULE_ONE_INDEX_TITLE)
        put("learn_news_rell_modules_index_url_slash", LEARN_NEWS_RELL_MODULES_INDEX_URL_SLASH)
        put("learn_news_rell_modules_index_title", LEARN_NEWS_RELL_MODULES_INDEX_TITLE)
        put("rell_systemlib_index_url_slash", RELL_SYSTEMLIB_INDEX_URL_SLASH)
        put("rell_systemlib_index_title", RELL_SYSTEMLIB_INDEX_TITLE)
        put("learn_ttt_module_two_index_url_slash", LEARN_TTT_MODULE_TWO_INDEX_URL_SLASH)
        put("learn_ttt_module_two_index_title", LEARN_TTT_MODULE_TWO_INDEX_TITLE)
        put("learn_vector_db_finalize_python_index_url_slash", LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_URL_SLASH)
        put("learn_vector_db_finalize_python_index_title", LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_TITLE)
        put("learn_zk_dapp_index_url_slash", LEARN_ZK_DAPP_INDEX_URL_SLASH)
        put("learn_zk_dapp_index_title", LEARN_ZK_DAPP_INDEX_TITLE)
        put("rell_systemlib_global_index_url_slash", RELL_SYSTEMLIB_GLOBAL_INDEX_URL_SLASH)
        put("rell_systemlib_global_index_title", RELL_SYSTEMLIB_GLOBAL_INDEX_TITLE)
        put("learn_zk_circom_index_url_slash", LEARN_ZK_CIRCOM_INDEX_URL_SLASH)
        put("learn_zk_circom_index_title", LEARN_ZK_CIRCOM_INDEX_TITLE)
        put("learn_relationships_one_to_many_index_url_slash", LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_URL_SLASH)
        put("learn_relationships_one_to_many_index_title", LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_TITLE)
        put("learn_web3_benefits_index_url_slash", LEARN_WEB3_BENEFITS_INDEX_URL_SLASH)
        put("learn_web3_benefits_index_title", LEARN_WEB3_BENEFITS_INDEX_TITLE)
        put("rell_systemlib_namespaces_index_url_slash", RELL_SYSTEMLIB_NAMESPACES_INDEX_URL_SLASH)
        put("rell_systemlib_namespaces_index_title", RELL_SYSTEMLIB_NAMESPACES_INDEX_TITLE)
        put("rell_systemlib_chain_context_index_url_slash", RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_URL_SLASH)
        put("rell_systemlib_chain_context_index_title", RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_TITLE)
        put("rell_systemlib_entities_index_url_slash", RELL_SYSTEMLIB_ENTITIES_INDEX_URL_SLASH)
        put("rell_systemlib_entities_index_title", RELL_SYSTEMLIB_ENTITIES_INDEX_TITLE)
        put("notes", notes())
    }
}

// Official ECOSYSTEM ecosystem/bridge/bridge-lease INDEX leftovers encoded as ECOSYSTEM_BRIDGE_LEASE_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/bridge/mass-exit INDEX leftovers encoded as ECOSYSTEM_MASS_EXIT_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/nodes/economy-chain-config INDEX leftovers encoded as ECOSYSTEM_ECONOMY_CHAIN_CONFIG_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/lease INDEX leftovers encoded as ECOSYSTEM_PMC_LEASE_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/rell-masterclass/entities INDEX leftovers encoded as LEARN_RELL_MASTERCLASS_ENTITIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/iccf-course/digital-warehouse-chain INDEX leftovers encoded as LEARN_ICCF_WAREHOUSE_CHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one INDEX leftovers encoded as LEARN_NEWS_MODULE_ONE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/project-structure/modules INDEX leftovers encoded as LEARN_NEWS_RELL_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib INDEX leftovers encoded as RELL_SYSTEMLIB_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-two INDEX leftovers encoded as LEARN_TTT_MODULE_TWO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX vector-db finalize-python-env leftovers encoded as LEARN_VECTOR_DB_FINALIZE_PYTHON_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof dapp leftovers encoded as LEARN_ZK_DAPP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/global-functions INDEX leftovers encoded as RELL_SYSTEMLIB_GLOBAL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX zero-knowledge-proof circom-circuits leftovers encoded as LEARN_ZK_CIRCOM_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX relationships-course one-to-many leftovers encoded as LEARN_RELATIONSHIPS_ONE_TO_MANY_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN INDEX web3-for-web2-devs web3-benefits leftovers encoded as LEARN_WEB3_BENEFITS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/namespaces INDEX leftovers encoded as RELL_SYSTEMLIB_NAMESPACES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/namespaces/chain_context INDEX leftovers encoded as RELL_SYSTEMLIB_CHAIN_CONTEXT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/system-entities INDEX leftovers encoded as RELL_SYSTEMLIB_ENTITIES_INDEX_* (query-only HELP ONLY WRITE SKIP).
