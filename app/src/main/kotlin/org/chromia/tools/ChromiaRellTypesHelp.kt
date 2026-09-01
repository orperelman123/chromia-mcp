package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Rell type-system help. Quotes docs.chromia.com/rell type pages only.
 * Does not invent types or methods. Rell language source tag 0.16.7 (docs may list 0.16.4).
 * Definition syntax lives on chromia_rell_language_help.
 * Official /types/subtypes is 404; canonical slug is /types/sub-types.
 */
object ChromiaRellTypesHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val RELL_VERSION = DappScaffold.RELL_SOURCE_TAG
    const val TOOL_NAME = "chromia_rell_types_help"
    const val TYPES_URL = "https://docs.chromia.com/rell/language-features/types/"
    const val SIMPLE_URL = "https://docs.chromia.com/rell/language-features/types/simple-types"
    const val COLLECTION_URL = "https://docs.chromia.com/rell/language-features/types/collection-types"
    const val COMPLEX_URL = "https://docs.chromia.com/rell/language-features/types/complex-types"
    const val ITERABLES_URL = "https://docs.chromia.com/rell/language-features/types/iterables"
    const val SUBTYPES_URL = "https://docs.chromia.com/rell/language-features/types/sub-types"
    const val SUBTYPES_404_URL = "https://docs.chromia.com/rell/language-features/types/subtypes"
    const val VIRTUAL_URL = "https://docs.chromia.com/rell/language-features/types/virtual-types"
    const val VALUES_URL = "https://docs.chromia.com/rell/language-features/expressions/values"
    const val IDENTIFIERS_URL = "https://docs.chromia.com/rell/language-features/identifiers-syntax"
    const val ECOSYSTEM_ZKP_INDEX_URL = "https://docs.chromia.com/ecosystem/extensions/zkp"
    const val ECOSYSTEM_ZKP_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/extensions/zkp/"
    const val ECOSYSTEM_ZKP_INDEX_TITLE = "Zero-knowledge Proof"  // official H1
    const val ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/bridge-deposit-troubleshooting"
    const val ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/bridge-deposit-troubleshooting/"
    const val ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_TITLE = "Bridge deposit troubleshooting guide"  // official H1
    const val ECOSYSTEM_PREPARE_NODE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/prepare-node"
    const val ECOSYSTEM_PREPARE_NODE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/prepare-node/"
    const val ECOSYSTEM_PREPARE_NODE_INDEX_TITLE = "Prepare the node for deployment"  // official H1
    const val ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/blockchain"
    const val ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/blockchain/"
    const val ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_TITLE = "blockchain"  // official H1
    const val ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/alternative-sol"
    const val ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/alternative-sol/"
    const val ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_TITLE = "Alternative solutions"  // official H1
    const val ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/rell-language"
    const val ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/rell-language/"
    const val ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_TITLE = "Rell overview"  // official H1
    const val ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_URL = "https://docs.chromia.com/ecosystem/block-explorer/using-explorer"
    const val ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/block-explorer/using-explorer/"
    const val ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_TITLE = "Use the Block Explorer to test and validate your dapp deployment"  // official H1
    const val ECOSYSTEM_FILEHUB_WORK_INDEX_URL = "https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-work"
    const val ECOSYSTEM_FILEHUB_WORK_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-work/"
    const val ECOSYSTEM_FILEHUB_WORK_INDEX_TITLE = "Work with Filehub"  // official H1
    const val LEARN_BOOK_REVIEW_QUERY_TX_INDEX_URL = "https://learn.chromia.com/courses/book-review/blockchain-transactions/query-transaction"
    const val LEARN_BOOK_REVIEW_QUERY_TX_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/blockchain-transactions/query-transaction/"
    const val LEARN_BOOK_REVIEW_QUERY_TX_INDEX_TITLE = "Understanding blockchain state and transactions"  // official H1
    const val LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain"
    const val LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/"
    const val LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_TITLE = "Module 2 - Blockchain dapp"  // official H1
    const val LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/transfer"
    const val LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/transfer/"
    const val LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_TITLE = "Lesson 6 - Transfer asset"  // official H1
    const val LEARN_NEWS_DATA_TABLES_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling/tables"
    const val LEARN_NEWS_DATA_TABLES_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling/tables/"
    const val LEARN_NEWS_DATA_TABLES_INDEX_TITLE = "The data model"  // official H1
    const val LEARN_TTT_MODULE_ONE_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-one"
    const val LEARN_TTT_MODULE_ONE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-one/"
    const val LEARN_TTT_MODULE_ONE_INDEX_TITLE = "Module 1 - Create Rell backend app with FT4 accounts"  // official H1
    const val LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_URL = "https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive/rell-interface"
    const val LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive/rell-interface/"
    const val LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_TITLE = "Connecting your data to the Vector DB (Rell)"  // official H1
    const val LEARN_ZK_DAPP_OPERATIONS_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-operations"
    const val LEARN_ZK_DAPP_OPERATIONS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-operations/"
    const val LEARN_ZK_DAPP_OPERATIONS_INDEX_TITLE = "Dapp operations overview"  // official H1
    const val LEARN_BOOK_REVIEW_SIGN_TX_INDEX_URL = "https://learn.chromia.com/courses/book-review/sign-transaction"
    const val LEARN_BOOK_REVIEW_SIGN_TX_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/sign-transaction/"
    const val LEARN_BOOK_REVIEW_SIGN_TX_INDEX_TITLE = "Lesson 4 - Sign a transaction and filter queries"  // official H1
    const val LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL = "https://learn.chromia.com/courses/book-review/input-verification/input-verification"
    const val LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/input-verification/input-verification/"
    const val LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_TITLE = "Verify inputs"  // official H1
    const val LEARN_COMPARISONS_POLYGON_INDEX_URL = "https://learn.chromia.com/courses/chromia-comparisons/polygon"
    const val LEARN_COMPARISONS_POLYGON_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-comparisons/polygon/"
    const val LEARN_COMPARISONS_POLYGON_INDEX_TITLE = "Polygon"  // official H1
    const val RELL_TYPES_INDEX_URL = "https://docs.chromia.com/rell/language-features/types"
    const val RELL_TYPES_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/types/"
    const val RELL_TYPES_INDEX_TITLE = "Types"  // official H1
    const val RELL_TYPES_SIMPLE_INDEX_URL = "https://docs.chromia.com/rell/language-features/types/simple-types"
    const val RELL_TYPES_SIMPLE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/types/simple-types/"
    const val RELL_TYPES_SIMPLE_INDEX_TITLE = "Simple types"  // official H1
    const val RELL_TYPES_COLLECTION_INDEX_URL = "https://docs.chromia.com/rell/language-features/types/collection-types"
    const val RELL_TYPES_COLLECTION_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/types/collection-types/"
    const val RELL_TYPES_COLLECTION_INDEX_TITLE = "Collection types"  // official H1
    const val RELL_SYSTEMLIB_CRYPTO_INDEX_URL = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/crypto"
    const val RELL_SYSTEMLIB_CRYPTO_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/systemlib/namespaces/crypto/"
    const val RELL_SYSTEMLIB_CRYPTO_INDEX_TITLE = "crypto"  // official H1
    const val RELL_TYPES_COMPLEX_INDEX_URL = "https://docs.chromia.com/rell/language-features/types/complex-types"
    const val RELL_TYPES_COMPLEX_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/types/complex-types/"
    const val RELL_TYPES_COMPLEX_INDEX_TITLE = "Complex types"  // official H1

    val simpleTypes = listOf(
        "boolean  # true / false",
        "integer  # MIN_VALUE -2^63, MAX_VALUE 2^63-1",
        "big_integer  # suffix L; PRECISION 131072",
        "decimal  # not binary float; 20 decimal places; 1E-20 smallest nonzero",
        "text  # 'Hello' or \"World\" (values page)",
        "byte_array  # x'1234' or x\"ABCD\" (values page)",
        "rowid  # 64-bit PK; comparison only; rowid(integer) rejects negative",
        "json  # PostgreSQL JSON; json(text) fails if invalid",
        "unit  # no value; cannot write explicitly (simple-types)",
        "null  # type of null expression; cannot write as a type name"
    )

    val aliases = listOf(
        "pubkey = byte_array",
        "name = text",
        "timestamp = integer",
        "tuid = text"
    )

    val complexTypes = listOf(
        "T?  # nullable; entity attributes cannot be nullable",
        "tuple  # (16,) / (26, \"Bob\") / (x=32, y=26); access t[0] or named_tuple.x",
        "range  # range(start=0, end, step=1); start-inclusive, end-exclusive",
        "gtv  # remote op/query args and results; simple / array / string-keyed dict"
    )

    val iterableTypes = listOf(
        "range",
        "list<T>",
        "set<T>",
        "map<K,V>",
        "virtual<list<T>>",
        "virtual<set<T>>",
        "virtual<map<K,V>>"
    )

    val subtypeRelations = listOf(
        "T is a subtype of T?",
        "null is a subtype of T?",
        "(T, P) is a subtype of (T?, P?), (T?, P), and (T, P?)"
    )

    val virtualConstraints = listOf(
        "virtual<T> for T = list<*>, set<*>, map<text, *>",
        "internal elements of T must be GTV-compatible",
        "map keys must be text",
        "immutable after creation",
        "cannot convert to GTV; cannot be a query return type",
        ".to_full(): T throws if the value is not fully present"
    )

    fun booleanExample(): String = """
        val using_rell = true;
        if (using_rell) print("Awesome!");
    """.trimIndent() + "\n"

    fun integerExample(): String = """
        val user_age: integer = 26;
        print(user_age); // 26
    """.trimIndent() + "\n"

    fun bigIntegerExample(): String = """
        val bi: big_integer = 9223372036854775832L;
        print(bi); // 9223372036854775832
    """.trimIndent() + "\n"

    fun decimalExample(): String = """
        val approx_pi: decimal = 3.14159;
        val scientific_value: decimal = 55.77e-5;
        print(approx_pi); // 3.14159
        print(scientific_value); // 0.0005577
    """.trimIndent() + "\n"

    fun textExample(): String = """
        val placeholder = "Lorem ipsum donor sit amet";
        print(placeholder.size()); // 26
        print(placeholder.empty()); // false
    """.trimIndent() + "\n"

    fun byteArrayExample(): String = """
        val hex_bytes: byte_array = x"ABCD";
        val quoted_bytes: byte_array = x'1234';
    """.trimIndent() + "\n"

    fun jsonExample(): String = """
        val json_text = '{ "name": "Alice" }';
        val json_value: json = json(json_text);
    """.trimIndent() + "\n"

    fun collectionExample(): String = """
        val l1 = [1, 2, 3, 4, 5, 1];
        val l2 = list<integer>();
        val s = set<integer>();
        val m1 = ['Bob': 123, 'Alice': 456];
        val m2 = map<text, integer>();
    """.trimIndent() + "\n"

    fun combineExample(): String = """
        val c = a + b;
        // or
        val c = a.add_all_copy(b);
    """.trimIndent() + "\n"

    fun nullableExample(): String = """
        function f(): integer? {
            return null;
        }
        val x: integer? = f();
        val i = x!!;
        val j = require(x);
        val a = x ?: 456;
        val q = x?.to_hex();
    """.trimIndent() + "\n"

    fun tupleExample(): String = """
        val single_number: (integer,) = (16,);
        val user_tuple: (integer, text) = (26, "Bob");
        val named_tuple: (x: integer, y: integer) = (x=32, y=26);
        val number = user_tuple[0];
        val name = named_tuple.x;
        val (n, s) = (123, 'Hello');
        val (_, ignored) = (123, 'Hello');
    """.trimIndent() + "\n"

    fun rangeExample(): String = """
        val r1 = range(10);
        val r2 = range(5, 10);
        val r3 = range(5, 15, 4);
        val r4 = range(10, 5, -1);
        if (3 in r1) {
            print("3 is in the range");
        }
    """.trimIndent() + "\n"

    fun gtvExample(): String = """
        val g = [1, 2, 3].to_gtv();
        val l = list<integer>.from_gtv(g);
        print(l);
        print(g.hash());
        (123).to_gtv().type;
        'hello'.to_gtv().type;
        [1, 2, 3].to_gtv().type;
    """.trimIndent() + "\n"

    fun iterableExample(): String = """
        for (x in range(10)) {
            print(x);
        }
        val tuples = [(1,'A'), (2,'B'), (3,'C')];
        val m = map(tuples);
        val L = list(m);
        val unique_numbers = set([1, 2, 2, 3, 3, 3, 4]);
    """.trimIndent() + "\n"

    fun virtualExample(): String = """
        struct Record {
            t: text;
            s: integer;
        }
        operation processRecords(virtualRecords: virtual<list<Record>>) {
            for (virtualRecord in virtualRecords) {
                val fullRecord = virtualRecord.to_full();
                print(fullRecord.t);
            }
        }
    """.trimIndent() + "\n"

    fun notes(): String = """
        Official Rell type pages for CLI $CLI_SERIES. Rell language source tag $RELL_VERSION (docs may still list 0.16.4 — source wins); the chromia.yml compile.rellVersion pin is ${DappScaffold.RELL_VERSION}.
        Types index: $TYPES_URL
        Simple: $SIMPLE_URL  Collection: $COLLECTION_URL  Complex: $COMPLEX_URL
        Iterables: $ITERABLES_URL  Subtypes: $SUBTYPES_URL  Virtual: $VIRTUAL_URL
        Identifiers: $IDENTIFIERS_URL  Literals: $VALUES_URL
        Official simple types: boolean, integer, big_integer (suffix L), decimal, text, byte_array, rowid, json, unit, null.
        Official simple-type aliases: pubkey = byte_array, name = text, timestamp = integer, tuid = text.
        Official values-page literals: null, true/false, integer, text ('Hello' or "World"),
        byte_array (x'1234' or x"ABCD"), big_integer suffix L, decimal 123.456 / 55.77e-5.
        integer.MIN_VALUE = -2^63, integer.MAX_VALUE = 2^63-1.
        decimal is not C/Java float/double: 20 decimal places; decimal('1E-20') nonzero, decimal('1E-21') zero.
        decimal.SCALE = 20, decimal.INT_DIGITS = 131072. == / != compare numeric value (1.0E+2 == 10.0E+1).
        text operators: + concat, [] character access (returns single-character text).
        byte_array operators: + concat, [] element access. Constructors: byte_array(text) / from_hex / from_base64 / from_list (0-255).
        rowid: primary key of a database record; comparison only; rowid(integer) must not be negative; .to_integer().
        json: json(text); .to_text() / .str(); [] equivalent to get(); get_or_null; as_integer / as_text / as_boolean and *_or_null.
        unit and the type name null cannot be written explicitly.
        Collection types: list (ordered, duplicates), set (unordered, unique), map<K,V>.
        Collections are always mutable. Map keys and set elements must be non-mutable types.
        Since 0.14.16: + / - / & copy-combine lists, sets, and maps (add_all_copy / remove_all_copy / retain_all_copy;
        map + is put_all_copy, b wins on key conflict). Map literal last value wins if a key is repeated.
        Official list operators: [] index, in membership. Official set/map: in.
        Official list constructors: list(), list(list), list(set).
        Official set constructors: set(), set(set), set(list) (duplicates removed).
        Official map constructors: map<K,V>(), map<K,V>(iterable<(K,V)>).
        Complex: T? operators ?: / ?. / !!. require(y) asserts non-null. Assign T -> T? and null -> T?; not the reverse.
        list<T> is not assignable to list<T?>. Smart cast after if (x != null). Entity attributes cannot be nullable.
        Tuple compatibility is name-sensitive: (x:integer, y:integer) is not compatible with (a:integer, b:integer)
        or with (integer, integer). Unpack val (n, s) = t; ignore with _.
        range: start-inclusive, end-exclusive (Python-like). Negative step allowed. in considers step.
        gtv functions: from_json(text|json), from_bytes, from_bytes_or_null, .to_json(), .to_bytes(), .hash().
        gtv_type since 0.15.3: NULL, BYTEARRAY, STRING, INTEGER, DICT, ARRAY, BIGINTEGER (read .type).
        GTV-compatible types: T.from_gtv / from_gtv_pretty, .to_gtv() / .to_gtv_pretty(), .hash(), null.to_gtv().
        Some Rell types are not GTV-compatible (complex-types page does not list the exclusion set).
        iterable<T> is an internal compiler type and cannot be declared in user code.
        Official subtype page slug is sub-types (not subtypes). $SUBTYPES_404_URL is 404.
        virtual<T> cannot convert to GTV and cannot be a query return type. Typical use: operation parameter.
        Official virtual list ops: .empty, .get, .size, .to_full, .to_text, .join_to_text, [], in (index present).
        Official virtual set ops: .empty, .size, .to_full, .to_text, .join_to_text, in.
        Official virtual map ops: .contains, .empty, .get / .get_or_default / .get_or_null, .keys, .values, .size, .to_full, .to_text, .join_to_text, [], in.
        Do not invent types, methods, or YAML keys. Definition syntax: chromia_rell_language_help.
        Expressions / statements: chromia_rell_expressions_help, chromia_rell_statements_help.
        Database create/update/delete / at: chromia_rell_database_help (Rell syntax inside operations only).
        Official ECOSYSTEM ecosystem/extensions/zkp INDEX ($ECOSYSTEM_ZKP_INDEX_URL 307 $ECOSYSTEM_ZKP_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_ZKP_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/bridge/bridge-troubleshooting/bridge-deposit-troubleshooting INDEX ($ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_URL 307 $ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_TITLE). Query-only.
        Official ECOSYSTEM ecosystem/providers/nodes/prepare-node INDEX ($ECOSYSTEM_PREPARE_NODE_INDEX_URL 307 $ECOSYSTEM_PREPARE_NODE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PREPARE_NODE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/providers/pmc/commands/blockchain INDEX ($ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_URL 307 $ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/alternative-sol INDEX ($ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_URL 307 $ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/governance/getting-started/rell-language INDEX ($ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_URL 307 $ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/block-explorer/using-explorer INDEX ($ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_URL 307 $ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official ECOSYSTEM ecosystem/filehub/configure-filehub/filehub-work INDEX ($ECOSYSTEM_FILEHUB_WORK_INDEX_URL 307 $ECOSYSTEM_FILEHUB_WORK_INDEX_URL_SLASH 200 H1 $ECOSYSTEM_FILEHUB_WORK_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/blockchain-transactions/query-transaction INDEX ($LEARN_BOOK_REVIEW_QUERY_TX_INDEX_URL 301 $LEARN_BOOK_REVIEW_QUERY_TX_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_QUERY_TX_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/ft4-demo-app/module-blockchain INDEX ($LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_URL 301 $LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/ft4-demo-app/module-frontend-application/transfer INDEX ($LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_URL 301 $LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_URL_SLASH 200 H1 $LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/my-news-feed/module-one/data-modeling/tables INDEX ($LEARN_NEWS_DATA_TABLES_INDEX_URL 301 $LEARN_NEWS_DATA_TABLES_INDEX_URL_SLASH 200 H1 $LEARN_NEWS_DATA_TABLES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/types INDEX ($RELL_TYPES_INDEX_URL 307 $RELL_TYPES_INDEX_URL_SLASH 200 H1 $RELL_TYPES_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only.
        Official LEARN courses/tic-tac-toe/module-one INDEX ($LEARN_TTT_MODULE_ONE_INDEX_URL 301 $LEARN_TTT_MODULE_ONE_INDEX_URL_SLASH 200 H1 $LEARN_TTT_MODULE_ONE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/vector-db-movie-demo/code-deep-dive/rell-interface INDEX ($LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_URL 301 $LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_URL_SLASH 200 H1 $LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/types/simple-types INDEX ($RELL_TYPES_SIMPLE_INDEX_URL 307 $RELL_TYPES_SIMPLE_INDEX_URL_SLASH 200 H1 $RELL_TYPES_SIMPLE_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/zero-knowledge-proof/dapp/dapp-operations INDEX ($LEARN_ZK_DAPP_OPERATIONS_INDEX_URL 301 $LEARN_ZK_DAPP_OPERATIONS_INDEX_URL_SLASH 200 H1 $LEARN_ZK_DAPP_OPERATIONS_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/sign-transaction INDEX ($LEARN_BOOK_REVIEW_SIGN_TX_INDEX_URL 301 $LEARN_BOOK_REVIEW_SIGN_TX_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_SIGN_TX_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/book-review/input-verification/input-verification INDEX ($LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL 301 $LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL_SLASH 200 H1 $LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official LEARN courses/chromia-comparisons/polygon INDEX ($LEARN_COMPARISONS_POLYGON_INDEX_URL 301 $LEARN_COMPARISONS_POLYGON_INDEX_URL_SLASH 200 H1 $LEARN_COMPARISONS_POLYGON_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/types/collection-types INDEX ($RELL_TYPES_COLLECTION_INDEX_URL 307 $RELL_TYPES_COLLECTION_INDEX_URL_SLASH 200 H1 $RELL_TYPES_COLLECTION_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/systemlib/namespaces/crypto INDEX ($RELL_SYSTEMLIB_CRYPTO_INDEX_URL 307 $RELL_SYSTEMLIB_CRYPTO_INDEX_URL_SLASH 200 H1 $RELL_SYSTEMLIB_CRYPTO_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Official RELL rell/language-features/types/complex-types INDEX ($RELL_TYPES_COMPLEX_INDEX_URL 307 $RELL_TYPES_COMPLEX_INDEX_URL_SLASH 200 H1 $RELL_TYPES_COMPLEX_INDEX_TITLE HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
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
        put("docs", TYPES_URL)
        put("simple_docs", SIMPLE_URL)
        put("collection_docs", COLLECTION_URL)
        put("complex_docs", COMPLEX_URL)
        put("iterables_docs", ITERABLES_URL)
        put("subtypes_docs", SUBTYPES_URL)
        put("virtual_docs", VIRTUAL_URL)
        put("values_docs", VALUES_URL)
        put("simple_types", buildJsonArray { simpleTypes.forEach { add(JsonPrimitive(it)) } })
        put("aliases", buildJsonArray { aliases.forEach { add(JsonPrimitive(it)) } })
        put("complex_types", buildJsonArray { complexTypes.forEach { add(JsonPrimitive(it)) } })
        put("iterable_types", buildJsonArray { iterableTypes.forEach { add(JsonPrimitive(it)) } })
        put("subtype_relations", buildJsonArray { subtypeRelations.forEach { add(JsonPrimitive(it)) } })
        put("virtual_constraints", buildJsonArray { virtualConstraints.forEach { add(JsonPrimitive(it)) } })
        put("boolean_example", booleanExample())
        put("integer_example", integerExample())
        put("big_integer_example", bigIntegerExample())
        put("decimal_example", decimalExample())
        put("text_example", textExample())
        put("byte_array_example", byteArrayExample())
        put("json_example", jsonExample())
        put("collection_example", collectionExample())
        put("combine_example", combineExample())
        put("nullable_example", nullableExample())
        put("tuple_example", tupleExample())
        put("range_example", rangeExample())
        put("gtv_example", gtvExample())
        put("iterable_example", iterableExample())
        put("virtual_example", virtualExample())
        put(
            "skipped_404",
            buildJsonArray {
                add(JsonPrimitive("$SUBTYPES_404_URL (404; official slug is sub-types)"))
            }
        )
        put("language_help", ChromiaRellLanguageHelp.TOOL_NAME)
        put("expressions_help", ChromiaRellExpressionsHelp.TOOL_NAME)
        put("statements_help", ChromiaRellStatementsHelp.TOOL_NAME)
        put("database_help", ChromiaRellDatabaseHelp.TOOL_NAME)
        put("ecosystem_zkp_index_url_slash", ECOSYSTEM_ZKP_INDEX_URL_SLASH)
        put("ecosystem_zkp_index_title", ECOSYSTEM_ZKP_INDEX_TITLE)
        put("ecosystem_bridge_deposit_troubleshooting_index_url_slash", ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_URL_SLASH)
        put("ecosystem_bridge_deposit_troubleshooting_index_title", ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_TITLE)
        put("ecosystem_prepare_node_index_url_slash", ECOSYSTEM_PREPARE_NODE_INDEX_URL_SLASH)
        put("ecosystem_prepare_node_index_title", ECOSYSTEM_PREPARE_NODE_INDEX_TITLE)
        put("ecosystem_pmc_blockchain_index_url_slash", ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_URL_SLASH)
        put("ecosystem_pmc_blockchain_index_title", ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_TITLE)
        put("ecosystem_gov_alternative_sol_index_url_slash", ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_URL_SLASH)
        put("ecosystem_gov_alternative_sol_index_title", ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_TITLE)
        put("ecosystem_gov_rell_language_index_url_slash", ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_URL_SLASH)
        put("ecosystem_gov_rell_language_index_title", ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_TITLE)
        put("ecosystem_block_explorer_using_index_url_slash", ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_URL_SLASH)
        put("ecosystem_block_explorer_using_index_title", ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_TITLE)
        put("ecosystem_filehub_work_index_url_slash", ECOSYSTEM_FILEHUB_WORK_INDEX_URL_SLASH)
        put("ecosystem_filehub_work_index_title", ECOSYSTEM_FILEHUB_WORK_INDEX_TITLE)
        put("learn_book_review_query_tx_index_url_slash", LEARN_BOOK_REVIEW_QUERY_TX_INDEX_URL_SLASH)
        put("learn_book_review_query_tx_index_title", LEARN_BOOK_REVIEW_QUERY_TX_INDEX_TITLE)
        put("learn_ft4_demo_blockchain_index_url_slash", LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_URL_SLASH)
        put("learn_ft4_demo_blockchain_index_title", LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_TITLE)
        put("learn_ft4_demo_frontend_transfer_index_url_slash", LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_URL_SLASH)
        put("learn_ft4_demo_frontend_transfer_index_title", LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_TITLE)
        put("learn_news_data_tables_index_url_slash", LEARN_NEWS_DATA_TABLES_INDEX_URL_SLASH)
        put("learn_news_data_tables_index_title", LEARN_NEWS_DATA_TABLES_INDEX_TITLE)
        put("rell_types_index_url_slash", RELL_TYPES_INDEX_URL_SLASH)
        put("rell_types_index_title", RELL_TYPES_INDEX_TITLE)
        put("learn_ttt_module_one_index_url_slash", LEARN_TTT_MODULE_ONE_INDEX_URL_SLASH)
        put("learn_ttt_module_one_index_title", LEARN_TTT_MODULE_ONE_INDEX_TITLE)
        put("learn_vector_db_rell_interface_index_url_slash", LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_URL_SLASH)
        put("learn_vector_db_rell_interface_index_title", LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_TITLE)
        put("rell_types_simple_index_url_slash", RELL_TYPES_SIMPLE_INDEX_URL_SLASH)
        put("rell_types_simple_index_title", RELL_TYPES_SIMPLE_INDEX_TITLE)
        put("learn_zk_dapp_operations_index_url_slash", LEARN_ZK_DAPP_OPERATIONS_INDEX_URL_SLASH)
        put("learn_zk_dapp_operations_index_title", LEARN_ZK_DAPP_OPERATIONS_INDEX_TITLE)
        put("learn_book_review_sign_tx_index_url_slash", LEARN_BOOK_REVIEW_SIGN_TX_INDEX_URL_SLASH)
        put("learn_book_review_sign_tx_index_title", LEARN_BOOK_REVIEW_SIGN_TX_INDEX_TITLE)
        put("learn_book_review_input_verification_index_url_slash", LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_URL_SLASH)
        put("learn_book_review_input_verification_index_title", LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_TITLE)
        put("learn_comparisons_polygon_index_url_slash", LEARN_COMPARISONS_POLYGON_INDEX_URL_SLASH)
        put("learn_comparisons_polygon_index_title", LEARN_COMPARISONS_POLYGON_INDEX_TITLE)
        put("rell_types_collection_index_url_slash", RELL_TYPES_COLLECTION_INDEX_URL_SLASH)
        put("rell_types_collection_index_title", RELL_TYPES_COLLECTION_INDEX_TITLE)
        put("rell_systemlib_crypto_index_url_slash", RELL_SYSTEMLIB_CRYPTO_INDEX_URL_SLASH)
        put("rell_systemlib_crypto_index_title", RELL_SYSTEMLIB_CRYPTO_INDEX_TITLE)
        put("rell_types_complex_index_url_slash", RELL_TYPES_COMPLEX_INDEX_URL_SLASH)
        put("rell_types_complex_index_title", RELL_TYPES_COMPLEX_INDEX_TITLE)
        put("notes", notes())
    }
}

// Official ECOSYSTEM ecosystem/extensions/zkp INDEX leftovers encoded as ECOSYSTEM_ZKP_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/bridge/bridge-troubleshooting/bridge-deposit-troubleshooting INDEX leftovers encoded as ECOSYSTEM_BRIDGE_DEPOSIT_TROUBLESHOOTING_INDEX_* (query-only).
// Official ECOSYSTEM ecosystem/providers/nodes/prepare-node INDEX leftovers encoded as ECOSYSTEM_PREPARE_NODE_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/providers/pmc/commands/blockchain INDEX leftovers encoded as ECOSYSTEM_PMC_BLOCKCHAIN_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/alternative-sol INDEX leftovers encoded as ECOSYSTEM_GOV_ALTERNATIVE_SOL_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/governance/getting-started/rell-language INDEX leftovers encoded as ECOSYSTEM_GOV_RELL_LANGUAGE_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/block-explorer/using-explorer INDEX leftovers encoded as ECOSYSTEM_BLOCK_EXPLORER_USING_INDEX_* (query-only HELP ONLY).
// Official ECOSYSTEM ecosystem/filehub/configure-filehub/filehub-work INDEX leftovers encoded as ECOSYSTEM_FILEHUB_WORK_INDEX_* (query-only HELP ONLY).
// Official LEARN courses/book-review/blockchain-transactions/query-transaction INDEX leftovers encoded as LEARN_BOOK_REVIEW_QUERY_TX_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-blockchain INDEX leftovers encoded as LEARN_FT4_DEMO_BLOCKCHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/ft4-demo-app/module-frontend-application/transfer INDEX leftovers encoded as LEARN_FT4_DEMO_FRONTEND_TRANSFER_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/my-news-feed/module-one/data-modeling/tables INDEX leftovers encoded as LEARN_NEWS_DATA_TABLES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/types INDEX leftovers encoded as RELL_TYPES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/tic-tac-toe/module-one INDEX leftovers encoded as LEARN_TTT_MODULE_ONE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/vector-db-movie-demo/code-deep-dive/rell-interface INDEX leftovers encoded as LEARN_VECTOR_DB_RELL_INTERFACE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/types/simple-types INDEX leftovers encoded as RELL_TYPES_SIMPLE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/zero-knowledge-proof/dapp/dapp-operations INDEX leftovers encoded as LEARN_ZK_DAPP_OPERATIONS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/sign-transaction INDEX leftovers encoded as LEARN_BOOK_REVIEW_SIGN_TX_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/book-review/input-verification/input-verification INDEX leftovers encoded as LEARN_BOOK_REVIEW_INPUT_VERIFICATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official LEARN courses/chromia-comparisons/polygon INDEX leftovers encoded as LEARN_COMPARISONS_POLYGON_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/types/collection-types INDEX leftovers encoded as RELL_TYPES_COLLECTION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/systemlib/namespaces/crypto INDEX leftovers encoded as RELL_SYSTEMLIB_CRYPTO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Official RELL rell/language-features/types/complex-types INDEX leftovers encoded as RELL_TYPES_COMPLEX_INDEX_* (query-only HELP ONLY WRITE SKIP).
